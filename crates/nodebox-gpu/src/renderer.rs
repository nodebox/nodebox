//! The GPU side: uploads a [`Prepared`] and draws it.

use wgpu::util::DeviceExt;

use crate::prepare::{Instance, Prepared};

/// Where to draw: a target of `width` by `height` pixels, document
/// coordinates scaled by `scale` and the origin at `offset`.
#[derive(Clone, Copy, Debug)]
pub struct View {
    pub width: u32,
    pub height: u32,
    pub scale: f64,
    pub offset: (f64, f64),
}

#[repr(C)]
#[derive(Clone, Copy, bytemuck::Pod, bytemuck::Zeroable)]
struct ViewUniform {
    viewport: [f32; 2],
    scale: f32,
    pad: f32,
    t: [f32; 2],
    pad2: [f32; 2],
}

struct Uploaded {
    id: u64,
    /// The scale and offset the instances were last composed with.
    view: Option<(f64, f64, f64)>,
    bind_group: wgpu::BindGroup,
    instances: wgpu::Buffer,
    count: u32,
}

/// The prepared paths need a bigger buffer than the device allows.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub struct TooLarge {
    /// Bytes the largest buffer would need.
    pub needed: u64,
    /// Bytes the device allows.
    pub limit: u64,
}

impl std::fmt::Display for TooLarge {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        write!(f, "too much geometry to draw: {} MiB, the GPU allows {} MiB", self.needed >> 20, self.limit >> 20)
    }
}

impl std::error::Error for TooLarge {}

/// Draws prepared paths into a texture of one format.
pub struct Renderer {
    pipeline: wgpu::RenderPipeline,
    view: wgpu::Buffer,
    uploaded: Option<Uploaded>,
}

impl Renderer {
    /// A renderer that draws into textures of `format`, blending premultiplied colors.
    pub fn new(device: &wgpu::Device, format: wgpu::TextureFormat) -> Self {
        let module = device.create_shader_module(wgpu::ShaderModuleDescriptor {
            label: Some("nodebox-gpu path"),
            source: wgpu::ShaderSource::Wgsl(include_str!("path.wgsl").into()),
        });
        let attributes = wgpu::vertex_attr_array![0 => Uint32, 1 => Float32x4, 2 => Float32x2, 3 => Float32x4];
        let pipeline = device.create_render_pipeline(&wgpu::RenderPipelineDescriptor {
            label: Some("nodebox-gpu path"),
            layout: None,
            vertex: wgpu::VertexState {
                module: &module,
                entry_point: Some("vs"),
                compilation_options: Default::default(),
                buffers: &[wgpu::VertexBufferLayout {
                    array_stride: std::mem::size_of::<Instance>() as u64,
                    step_mode: wgpu::VertexStepMode::Instance,
                    attributes: &attributes,
                }],
            },
            fragment: Some(wgpu::FragmentState {
                module: &module,
                entry_point: Some("fs"),
                compilation_options: Default::default(),
                targets: &[Some(wgpu::ColorTargetState {
                    format,
                    blend: Some(wgpu::BlendState::PREMULTIPLIED_ALPHA_BLENDING),
                    write_mask: wgpu::ColorWrites::ALL,
                })],
            }),
            primitive: Default::default(),
            depth_stencil: None,
            multisample: Default::default(),
            multiview: None,
            cache: None,
        });
        let view = device.create_buffer(&wgpu::BufferDescriptor {
            label: Some("nodebox-gpu view"),
            size: std::mem::size_of::<ViewUniform>() as u64,
            usage: wgpu::BufferUsages::UNIFORM | wgpu::BufferUsages::COPY_DST,
            mapped_at_creation: false,
        });
        Renderer { pipeline, view, uploaded: None }
    }

    /// Clear `target` to `background` and draw `prepared` into it. Uploads the
    /// curves and instances only when `prepared` is not the one drawn last.
    /// When they do not fit in the device's buffers, it only clears.
    pub fn render(
        &mut self,
        device: &wgpu::Device,
        queue: &wgpu::Queue,
        prepared: &Prepared,
        view: View,
        target: &wgpu::TextureView,
        background: wgpu::Color,
    ) -> Result<(), TooLarge> {
        let fits = Self::fits(device, prepared);
        if fits.is_err() {
            self.uploaded = None;
        } else if self.uploaded.as_ref().map(|u| u.id) != Some(prepared.id) {
            self.uploaded = Some(self.upload(device, prepared));
        }
        // Each placement is composed with the view here, in f64, and the shader gets
        // pixels: in f32, a shape a million units out would land a few pixels off.
        let key = (view.scale, view.offset.0, view.offset.1);
        if let Some(uploaded) = self.uploaded.as_mut().filter(|u| u.view != Some(key) && u.count > 0) {
            let (s, (ox, oy)) = (view.scale, view.offset);
            let composed: Vec<Instance> = prepared
                .instances
                .iter()
                .zip(&prepared.placements)
                .map(|(i, [a, b, c, d, e, f])| Instance {
                    m: [(a * s) as f32, (b * s) as f32, (c * s) as f32, (d * s) as f32],
                    t: [(e * s + ox) as f32, (f * s + oy) as f32],
                    ..*i
                })
                .collect();
            queue.write_buffer(&uploaded.instances, 0, bytemuck::cast_slice(&composed));
            uploaded.view = Some(key);
        }
        let uniform = ViewUniform {
            viewport: [view.width as f32, view.height as f32],
            scale: 1.0,
            pad: 0.0,
            t: [0.0, 0.0],
            pad2: [0.0; 2],
        };
        queue.write_buffer(&self.view, 0, bytemuck::bytes_of(&uniform));

        let mut encoder = device.create_command_encoder(&wgpu::CommandEncoderDescriptor::default());
        {
            let mut pass = encoder.begin_render_pass(&wgpu::RenderPassDescriptor {
                label: Some("nodebox-gpu"),
                color_attachments: &[Some(wgpu::RenderPassColorAttachment {
                    view: target,
                    depth_slice: None,
                    resolve_target: None,
                    ops: wgpu::Operations { load: wgpu::LoadOp::Clear(background), store: wgpu::StoreOp::Store },
                })],
                ..Default::default()
            });
            if let Some(uploaded) = self.uploaded.as_ref().filter(|u| u.count > 0) {
                pass.set_pipeline(&self.pipeline);
                pass.set_bind_group(0, &uploaded.bind_group, &[]);
                pass.set_vertex_buffer(0, uploaded.instances.slice(..));
                pass.draw(0..6, 0..uploaded.count);
            }
        }
        queue.submit([encoder.finish()]);
        fits
    }

    fn fits(device: &wgpu::Device, prepared: &Prepared) -> Result<(), TooLarge> {
        let limits = device.limits();
        let storage = u64::from(limits.max_storage_buffer_binding_size).min(limits.max_buffer_size);
        let size = |n: usize, item: usize| (n * item) as u64;
        let largest_storage = size(prepared.curves.len(), 16)
            .max(size(prepared.entries.len(), std::mem::size_of::<crate::prepare::Entry>()))
            .max(size(prepared.bands.len(), 4));
        let instances = size(prepared.instances.len(), std::mem::size_of::<Instance>());
        if largest_storage > storage {
            return Err(TooLarge { needed: largest_storage, limit: storage });
        }
        if instances > limits.max_buffer_size {
            return Err(TooLarge { needed: instances, limit: limits.max_buffer_size });
        }
        Ok(())
    }

    fn upload(&self, device: &wgpu::Device, prepared: &Prepared) -> Uploaded {
        // wgpu rejects empty buffers, so an empty list uploads a few zero bytes.
        let init = |label: &str, data: &[u8], usage: wgpu::BufferUsages| {
            let contents = if data.is_empty() { &[0u8; 64][..] } else { data };
            device.create_buffer_init(&wgpu::util::BufferInitDescriptor { label: Some(label), contents, usage })
        };
        let storage = wgpu::BufferUsages::STORAGE;
        let curves = init("nodebox-gpu curves", bytemuck::cast_slice(&prepared.curves), storage);
        let entries = init("nodebox-gpu entries", bytemuck::cast_slice(&prepared.entries), storage);
        let bands = init("nodebox-gpu bands", bytemuck::cast_slice(&prepared.bands), storage);
        let instances = init(
            "nodebox-gpu instances",
            bytemuck::cast_slice(&prepared.instances),
            wgpu::BufferUsages::VERTEX | wgpu::BufferUsages::COPY_DST,
        );
        let bind_group = device.create_bind_group(&wgpu::BindGroupDescriptor {
            label: Some("nodebox-gpu"),
            layout: &self.pipeline.get_bind_group_layout(0),
            entries: &[
                wgpu::BindGroupEntry { binding: 0, resource: self.view.as_entire_binding() },
                wgpu::BindGroupEntry { binding: 1, resource: curves.as_entire_binding() },
                wgpu::BindGroupEntry { binding: 2, resource: entries.as_entire_binding() },
                wgpu::BindGroupEntry { binding: 3, resource: bands.as_entire_binding() },
            ],
        });
        Uploaded { id: prepared.id, view: None, bind_group, instances, count: prepared.instances.len() as u32 }
    }
}
