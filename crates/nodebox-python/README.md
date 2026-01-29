# nodebox-python

Python bindings for NodeBox's Rust implementation.

## Overview

This crate provides two-way integration between Python and NodeBox:

1. **Python Extension**: Export NodeBox types and functions to Python
2. **Python Runtime**: Load and execute Python modules from Rust

## Requirements

To build this crate, you need:
- Python 3.8+ with development headers installed
- On macOS: `brew install python` or Xcode command line tools
- On Ubuntu/Debian: `apt install python3-dev`
- On Windows: Python from python.org with development files

## Building

Since this crate requires Python development libraries, it's excluded from the default workspace build. To build it explicitly:

```bash
cargo build -p nodebox-python
```

## Writing Python Nodes

### Basic Structure

Create a Python file with functions that will be exposed as nodes:

```python
# my_nodes.py
from nodebox import Point, Path, Color

def spiral(center: Point, turns: float = 5, size: float = 100) -> Path:
    """Create a spiral path.

    Args:
        center: Center point of the spiral
        turns: Number of turns (default: 5)
        size: Maximum radius (default: 100)

    Returns:
        A Path representing the spiral.
    """
    import math

    path = Path()
    steps = int(turns * 36)  # 36 points per turn

    for i in range(steps):
        t = i / steps
        angle = t * turns * 2 * math.pi
        radius = t * size
        x = center.x + radius * math.cos(angle)
        y = center.y + radius * math.sin(angle)

        if i == 0:
            path = path.moveto(x, y)
        else:
            path = path.lineto(x, y)

    return path
```

### Type Annotations

Use type annotations to help NodeBox understand your function's signature:

- `Point` - A 2D point with x and y coordinates
- `Color` - An RGBA color
- `Path` - A geometric path with contours
- `Rect` - A rectangle with position and size
- `float`, `int`, `str`, `bool` - Standard Python types
- `List[Point]` - A list of points

### Available Types

```python
from nodebox import (
    Point,      # 2D point
    Color,      # RGBA color
    Rect,       # Rectangle
    Path,       # Geometric path
)
```

#### Point

```python
p = Point(100, 200)
p.x  # Get x coordinate
p.y  # Get y coordinate
p.distance_to(other)  # Distance to another point

Point.origin()  # Create point at (0, 0)
```

#### Color

```python
c = Color(1.0, 0.5, 0.0, 1.0)  # RGBA
c.r, c.g, c.b, c.a  # Get components

Color.rgb(1.0, 0.0, 0.0)      # Red
Color.rgba(1.0, 0.0, 0.0, 0.5)  # Red, 50% transparent
Color.hsb(0.0, 1.0, 1.0)      # Red (HSB)
Color.from_hex("#FF0000")      # From hex string
Color.black()                  # Black
Color.white()                  # White
```

#### Path

```python
path = Path()
path = path.moveto(0, 0)      # Start new contour
path = path.lineto(100, 100)  # Draw line
path = path.curveto(...)      # Draw bezier curve
path = path.close()           # Close contour

path.bounds()         # Bounding rectangle
path.points()         # List of (x, y) tuples
path.contour_count()  # Number of contours
path.point_count()    # Total number of points
```

### Available Functions

```python
from nodebox import (
    # Generators
    ellipse,    # Create ellipse
    rect,       # Create rectangle
    line,       # Create line
    polygon,    # Create regular polygon
    star,       # Create star shape
    arc,        # Create arc or pie
    grid,       # Create grid of points

    # Transforms
    translate,  # Move path
    rotate,     # Rotate path
    scale,      # Scale path
    colorize,   # Set fill/stroke colors
)
```

### Loading Python Modules from Rust

```rust
use nodebox_python::{PythonRuntime, PythonBridge};
use std::sync::{Arc, Mutex};
use std::path::Path;

// Create a runtime
let runtime = Arc::new(Mutex::new(PythonRuntime::new()?));

// Load a module
{
    let mut rt = runtime.lock().unwrap();
    rt.load_module(Path::new("my_nodes.py"), "my_nodes")?;
}

// Create a bridge for calling functions
let bridge = PythonBridge::new(runtime.clone());

// Call a function
let callable = bridge.make_callable("my_nodes", "spiral");
let result = callable(&[
    Value::Point(Point::new(0.0, 0.0)),
    Value::Float(10.0),
    Value::Float(200.0),
])?;
```

### Best Practices

1. **Use type annotations** - They help NodeBox create appropriate UI widgets
2. **Provide docstrings** - They appear as tooltips in the node editor
3. **Use default values** - Make nodes easy to use with sensible defaults
4. **Return immutable data** - Create new paths/colors instead of modifying inputs
5. **Handle None gracefully** - Check for None inputs when appropriate

### Example: Custom Filter

```python
def jitter(path: Path, amount: float = 10.0, seed: int = 0) -> Path:
    """Add random jitter to path points.

    Args:
        path: Input path to jitter
        amount: Maximum jitter distance
        seed: Random seed for reproducibility

    Returns:
        New path with jittered points.
    """
    import random
    random.seed(seed)

    new_path = Path()
    for x, y in path.points():
        dx = (random.random() - 0.5) * amount * 2
        dy = (random.random() - 0.5) * amount * 2
        new_path = new_path.lineto(x + dx, y + dy)

    return new_path
```

## Python Package Structure

The `python/nodebox/` directory contains a pure Python package that:
- Wraps the Rust extension when available
- Provides pure Python fallbacks for development/testing
- Ensures API compatibility with existing NodeBox Python code

```
python/
└── nodebox/
    ├── __init__.py     # Main module, imports from Rust or fallback
    ├── graphics.py     # Pure Python type implementations
    └── functions.py    # Pure Python function implementations
```

## License

GPL-3.0 - See LICENSE file in the repository root.
