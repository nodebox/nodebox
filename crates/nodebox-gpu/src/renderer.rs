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
    bind_group: wgpu::BindGroup,
    instances: wgpu::Buffer,
    count: u32,
}

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
    pub fn render(
        &mut self,
        device: &wgpu::Device,
        queue: &wgpu::Queue,
        prepared: &Prepared,
        view: View,
        target: &wgpu::TextureView,
        background: wgpu::Color,
    ) {
        if self.uploaded.as_ref().map(|u| u.id) != Some(prepared.id) {
            self.uploaded = Some(self.upload(device, prepared));
        }
        let uniform = ViewUniform {
            viewport: [view.width as f32, view.height as f32],
            scale: view.scale as f32,
            pad: 0.0,
            t: [view.offset.0 as f32, view.offset.1 as f32],
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
            let uploaded = self.uploaded.as_ref().unwrap();
            if uploaded.count > 0 {
                pass.set_pipeline(&self.pipeline);
                pass.set_bind_group(0, &uploaded.bind_group, &[]);
                pass.set_vertex_buffer(0, uploaded.instances.slice(..));
                pass.draw(0..6, 0..uploaded.count);
            }
        }
        queue.submit([encoder.finish()]);
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
            wgpu::BufferUsages::VERTEX,
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
        Uploaded { id: prepared.id, bind_group, instances, count: prepared.instances.len() as u32 }
    }
}
