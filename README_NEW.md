# NodeBox Rust

A generative design toolkit for creating 2D graphics through visual programming or code.

## Quick Start

### GUI (Visual Editor)

```bash
cargo run -p nodebox-gui
```

Opens the visual editor with:
- **Canvas viewer** - See your designs in real-time
- **Node graph** - Connect nodes to build generative systems
- **Parameter editor** - Tweak values with sliders and inputs
- **Timeline** - Animate parameters over time

### Command Line

```bash
# Generate demo SVGs
cargo run -p nodebox-cli -- demo shapes > shapes.svg
cargo run -p nodebox-cli -- demo spiral > spiral.svg

# Convert text to vector paths
cargo run -p nodebox-cli -- text "Hello" > hello.svg

# Interactive mode
cargo run -p nodebox-cli
```

## Using as a Library

Add to your `Cargo.toml`:

```toml
[dependencies]
nodebox-core = { path = "crates/nodebox-core" }
nodebox-ops = { path = "crates/nodebox-ops" }
nodebox-svg = { path = "crates/nodebox-svg" }
```

### Example: Generative Pattern

```rust
use nodebox_core::{Point, Color, Path};
use nodebox_ops::{polygon, star};
use nodebox_svg::render_to_svg;

fn main() {
    let mut paths = Vec::new();

    // Create a grid of rotating stars
    for row in 0..5 {
        for col in 0..5 {
            let x = 50.0 + col as f64 * 80.0;
            let y = 50.0 + row as f64 * 80.0;
            let rotation = (row + col) as f64 * 15.0;

            let mut s = star(Point::new(x, y), 6, 30.0, 15.0);
            s = nodebox_ops::rotate(&s, Point::new(x, y), rotation);
            s.fill = Some(Color::hsb(
                (row * 5 + col) as f64 / 25.0,  // hue
                0.7,                              // saturation
                0.9,                              // brightness
            ));
            paths.push(s);
        }
    }

    let svg = render_to_svg(&paths, 450.0, 450.0);
    std::fs::write("pattern.svg", svg).unwrap();
}
```

### Example: Animated Export

```rust
use nodebox_core::{Point, Color, Path};
use nodebox_ops::polygon;
use nodebox_svg::render_to_svg;
use std::f64::consts::PI;

fn main() {
    // Export 60 frames of animation
    for frame in 0..60 {
        let t = frame as f64 / 60.0;
        let mut paths = Vec::new();

        for i in 0..12 {
            let angle = t * 2.0 * PI + i as f64 * PI / 6.0;
            let x = 200.0 + angle.cos() * 80.0;
            let y = 200.0 + angle.sin() * 80.0;

            let mut hex = polygon(Point::new(x, y), 25.0, 6, true);
            hex.fill = Some(Color::hsb(i as f64 / 12.0 + t, 0.8, 0.9));
            paths.push(hex);
        }

        let svg = render_to_svg(&paths, 400.0, 400.0);
        std::fs::write(format!("frame_{:03}.svg", frame), svg).unwrap();
    }
}
```

## Available Operations

| Module | Examples |
|--------|----------|
| **Generators** | `ellipse`, `rect`, `line`, `polygon`, `star`, `arc`, `spiral`, `grid` |
| **Filters** | `translate`, `rotate`, `scale`, `colorize`, `copy`, `align`, `fit` |
| **Math** | `add`, `sin`, `cos`, `random`, `range`, `wave`, `sample` |
| **List** | `sort`, `shuffle`, `slice`, `repeat`, `combine`, `pick` |
| **String** | `concatenate`, `split`, `format_number`, `change_case` |

## Creative Ideas

- **Generative logos** - Combine shapes with randomness for unique variations
- **Data visualization** - Map data to size, color, and position
- **Pattern design** - Tile and transform shapes for textiles/wallpapers
- **Motion graphics** - Export frame sequences for video
- **Plotter art** - Generate SVGs for pen plotters

## Python Support

For Python scripting support (requires Python dev headers):

```bash
cargo build -p nodebox-python
```

See [docs/python-nodes.md](docs/python-nodes.md) for writing Python nodes.
