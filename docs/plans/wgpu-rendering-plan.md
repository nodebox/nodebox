# Plan: wgpu Vector Rendering Integration

## Executive Summary

NodeBox currently uses CPU-based vector rendering via egui's painter API with tiny-skia for rasterization. This plan outlines how to integrate GPU-accelerated vector rendering using **Vello**, the most mature wgpu-based 2D vector renderer in the Rust ecosystem.

## Current State

### Rendering Pipeline
```
Node Evaluation → Path/Contour Geometry → egui Painter → tiny-skia (CPU) → Screen
```

### Current Dependencies
- **egui 0.33** - Immediate-mode UI framework
- **eframe 0.33** - Native window integration (uses wgpu 27)
- **tiny-skia 0.11** - CPU-based vector rasterizer
- **pathfinder_geometry** - Geometry primitives (used in nodebox-core)

### Rendering Location
- `crates/nodebox-desktop/src/viewer_pane.rs:714-803` - Manual path rendering
- Uses `egui::Painter` with `egui::Shape::line()` and `egui::Shape::convex_polygon()`
- Cubic bezier curves manually sampled to line segments (10 samples per curve)

### Current Limitations
- All rendering happens on CPU (single-threaded bottleneck)
- Complex scenes with many paths become sluggish
- Bezier curves approximated with line segments (quality loss at high zoom)
- No hardware acceleration for fills, strokes, or anti-aliasing

## Proposed Solution: Vello

### Why Vello?

| Feature | tiny-skia (current) | Vello |
|---------|---------------------|-------|
| Execution | CPU only | GPU compute shaders |
| Performance | Good for simple scenes | Up to 100x faster for complex scenes |
| Bezier rendering | Line approximation | Native GPU curves |
| Anti-aliasing | CPU MSAA | GPU-computed analytical AA |
| Memory | CPU RAM | GPU VRAM |
| Parallel processing | Limited (single-thread rasterization) | Massive GPU parallelism |
| Browser support | N/A | WebGPU (Chrome, experimental in Firefox/Safari) |

### Vello Benchmarks (from Linebender)
- **177 FPS** for paris-30k test scene (30,000 paths) on M1 Max at 1600x1600
- Vello CPU is 2nd fastest CPU renderer in Rust ecosystem (behind Blend2D)
- GPU mode significantly outperforms CPU mode on capable hardware

### Vello Architecture

Vello offers three rendering backends:
1. **Vello GPU** - Full compute shader pipeline (best performance)
2. **Vello CPU** - Pure CPU fallback (portable, good performance)
3. **Vello Hybrid** - GPU with CPU fallback for unsupported features

## Integration Architecture

### Target Pipeline
```
Node Evaluation → Path Geometry → kurbo BezPath → Vello Scene → wgpu Texture → egui Image
```

### Component Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                     nodebox-desktop                                 │
├─────────────────────────────────────────────────────────────────┤
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐  │
│  │   egui UI    │    │ VelloViewer  │    │  egui_wgpu       │  │
│  │  (panels,    │    │ (new widget) │    │  (shared wgpu    │  │
│  │   menus)     │    │              │    │   context)       │  │
│  └──────────────┘    └──────────────┘    └──────────────────┘  │
│                              │                    │             │
│                              ▼                    ▼             │
│                      ┌─────────────────────────────────┐       │
│                      │     Shared wgpu::Device         │       │
│                      └─────────────────────────────────┘       │
│                              │                                  │
│                              ▼                                  │
│                      ┌─────────────────────────────────┐       │
│                      │     Vello Renderer              │       │
│                      │  (renders to wgpu texture)      │       │
│                      └─────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

### Geometry Conversion Layer

NodeBox uses custom geometry types that need conversion to kurbo (Vello's geometry library):

```rust
// nodebox-core/src/geometry/path.rs → kurbo::BezPath
impl From<&nodebox_core::Path> for kurbo::BezPath {
    fn from(path: &nodebox_core::Path) -> Self {
        let mut bez = kurbo::BezPath::new();
        for contour in &path.contours {
            // Convert points to kurbo path elements
            // LineTo → bez.line_to()
            // CurveTo → bez.curve_to() (cubic bezier)
        }
        bez
    }
}

// nodebox-core Color → vello peniko::Color
impl From<nodebox_core::Color> for peniko::Color {
    fn from(c: nodebox_core::Color) -> Self {
        peniko::Color::rgba(c.r, c.g, c.b, c.a)
    }
}
```

## Implementation Phases

### Phase 1: Foundation (Week 1-2)

**Goal:** Set up wgpu infrastructure alongside existing rendering

#### Tasks:
1. **Add dependencies to nodebox-desktop/Cargo.toml:**
   ```toml
   # GPU rendering
   vello = "0.4"
   wgpu = "27.0"

   # Update eframe to use wgpu backend
   eframe = { version = "0.30", features = ["wgpu"] }
   ```

2. **Create geometry conversion module:**
   - `crates/nodebox-desktop/src/vello_convert.rs`
   - Implement `From<&Path>` for `kurbo::BezPath`
   - Implement color conversion

3. **Create Vello renderer wrapper:**
   - `crates/nodebox-desktop/src/vello_renderer.rs`
   - Initialize Vello with shared wgpu device from egui
   - Render to texture method

4. **Add feature flag for gradual rollout:**
   ```toml
   [features]
   default = []
   gpu-rendering = ["vello", "wgpu"]
   ```

### Phase 2: Vello Viewer Widget (Week 3-4)

**Goal:** Create a Vello-powered canvas widget

#### Tasks:
1. **Create VelloViewer widget:**
   - `crates/nodebox-desktop/src/vello_viewer.rs`
   - Manage wgpu texture lifecycle
   - Handle resize events
   - Display rendered texture in egui

2. **Implement scene building:**
   - Convert `Vec<Path>` to Vello `Scene`
   - Support fill and stroke styles
   - Implement transform handling

3. **Pan/zoom integration:**
   - Apply view transform to Vello scene
   - Share pan/zoom state with existing `PanZoom` struct

### Phase 3: Feature Parity (Week 5-6)

**Goal:** Match existing viewer functionality

#### Tasks:
1. **Grid rendering** - Draw grid using Vello
2. **Origin crosshair** - Render in Vello scene
3. **Canvas border** - Document bounds visualization
4. **Point markers** - Show path points when enabled
5. **Point numbers** - Render digit textures or Vello text
6. **Handles overlay** - Keep in egui layer (interaction needs egui)

### Phase 4: Optimization & Polish (Week 7-8)

**Goal:** Performance optimization and fallback handling

#### Tasks:
1. **Scene caching:**
   - Cache Vello scene when geometry unchanged
   - Only rebuild on node evaluation

2. **Incremental rendering:**
   - Track dirty regions
   - Re-render only changed areas

3. **CPU fallback:**
   - Detect GPU capabilities
   - Fall back to Vello CPU or tiny-skia on unsupported hardware

4. **Memory management:**
   - Texture pooling
   - VRAM budget tracking

### Phase 5: Advanced Features (Future)

**Goal:** Leverage GPU capabilities for advanced rendering

#### Potential Features:
- **Gradients** - Linear, radial, conic gradients
- **Blur effects** - GPU-accelerated blur (when Vello supports it)
- **Image fills** - Texture mapping in paths
- **Text rendering** - Direct GPU text (currently text→path)
- **Clipping** - Complex clip paths
- **Blend modes** - Porter-Duff compositing

## Code Structure

### New Files
```
crates/nodebox-desktop/src/
├── vello_convert.rs      # Geometry conversion (Path → kurbo)
├── vello_renderer.rs     # Vello wrapper and texture management
├── vello_viewer.rs       # Vello-powered viewer widget
└── gpu_context.rs        # Shared wgpu device management
```

### Modified Files
```
crates/nodebox-desktop/
├── Cargo.toml           # Add vello, wgpu dependencies
├── src/app.rs           # Initialize GPU context
├── src/viewer_pane.rs   # Switch between CPU/GPU rendering
└── src/lib.rs           # Export new modules
```

## Risk Assessment

### Technical Risks

| Risk | Likelihood | Impact | Mitigation |
|------|------------|--------|------------|
| wgpu device sharing with egui | Medium | High | Use egui_wgpu's device, or render to texture |
| WebGPU browser support | Medium | Medium | Keep tiny-skia for web builds |
| Vello alpha stability | Medium | Medium | Pin specific version, test thoroughly |
| Memory pressure on GPU | Low | Medium | Implement VRAM budgeting |

### Compatibility Concerns

1. **macOS Metal:** Excellent support (Apple Silicon is Vello's sweet spot)
2. **Windows DirectX 12:** Good support via wgpu
3. **Linux Vulkan:** Good support, depends on driver
4. **Web:** Experimental (Chrome WebGPU only)
5. **Older GPUs:** May need CPU fallback (no compute shaders)

## Dependencies

### Direct Dependencies
```toml
vello = "0.4"                    # GPU vector renderer
kurbo = "0.11"                   # Geometry primitives (already in workspace)
peniko = "0.3"                   # Color/brush types (Vello's styling)
wgpu = "27.0"                    # GPU abstraction
```

### Transitive (via Vello)
- `bytemuck` - GPU buffer casting
- `skrifa` - Font parsing (if using Vello text)

## Success Metrics

1. **Performance:** 10x+ improvement for scenes with 1000+ paths
2. **Quality:** Native bezier rendering (no line approximation)
3. **Compatibility:** Works on 90%+ of target hardware
4. **Fallback:** Graceful degradation to CPU rendering

## References

- [Vello GitHub](https://github.com/linebender/vello)
- [Vello Documentation](https://docs.rs/vello)
- [kurbo Geometry Library](https://docs.rs/kurbo)
- [egui wgpu Backend](https://docs.rs/egui-wgpu)
- [wgpu Documentation](https://docs.rs/wgpu)
- [Linebender Blog - Vello Updates](https://linebender.org/blog/)

## Decision Points

Before proceeding, confirm:

1. **Target hardware requirements** - Minimum GPU spec to support?
2. **Web deployment priority** - Is WebGPU support critical?
3. **Timeline constraints** - Phased rollout vs. full replacement?
4. **Feature scope** - Basic fills/strokes only, or gradients/effects?

---

*Plan created: 2026-02-01*
*Status: Unified wgpu Phase Complete (Phase 1-3)*

## Implementation Progress

### Phase 1: Foundation ✅ Complete
- Added vello 0.7 workspace dependency (uses wgpu 27)
- Created `vello_convert.rs` - geometry conversion (Path → kurbo BezPath)
- Created `vello_renderer.rs` - Vello wrapper and ViewTransform
- Added `gpu-rendering` feature flag (off by default)

### Phase 2: Vello Viewer Widget ✅ Complete
- Created `vello_viewer.rs` with VelloViewer widget
- Initial implementation with texture-copy approach (separate wgpu context)

### Phase 2.5: Unified wgpu ✅ Complete (2026-02-01)
**Major Performance Improvement**

Previously, egui-wgpu 0.30 used wgpu 23, while vello 0.7 used wgpu 27, requiring:
- Separate wgpu device for Vello
- GPU → CPU → GPU texture copying (slow!)

**Solution: Upgraded to egui 0.33 (egui-wgpu now uses wgpu 27)**

Changes:
- Updated egui/eframe/egui-wgpu from 0.30 to 0.33
- Fixed breaking API changes (Rounding→CornerRadius, Margin::same takes i8, etc.)
- Rewrote VelloViewer to use `egui_wgpu::RenderState`
- Vello now renders directly to egui-registered textures
- **Zero-copy texture sharing** - massive performance improvement

Architecture:
```
VelloViewer ──┬─> Vello Renderer ──> Shared wgpu Texture
              │
              └─> egui (via RenderState.device/queue)
```

### Phase 3: Feature Parity (In Progress)
- [x] Basic geometry rendering
- [x] Fill and stroke support
- [x] Pan/zoom transform
- [x] Cache invalidation via geometry hash
- [ ] Grid rendering in Vello
- [ ] Origin crosshair in Vello
- [ ] Canvas border in Vello
- [ ] Point markers in Vello
- Handles remain in egui (need interaction layer)

### Remaining Phases
- Phase 4: Optimization (scene caching, incremental rendering)
- Phase 5: Advanced Features (gradients, blur, blend modes)
