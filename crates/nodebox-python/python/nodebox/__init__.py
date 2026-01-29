"""
NodeBox Python compatibility layer.

This module provides a Python-compatible API for NodeBox's Rust implementation.
It mirrors the original NodeBox Python API to allow existing scripts to work
with minimal modification.

Example usage:
    from nodebox.graphics import Point, Color, Path

    # Create a point
    p = Point(100, 200)

    # Create a color
    c = Color.rgb(1.0, 0.0, 0.0)

    # Create a path
    path = Path()
    path = path.moveto(0, 0)
    path = path.lineto(100, 100)
"""

# Try to import from the Rust module first
try:
    from nodebox_python import (
        Point,
        Color,
        Rect,
        Path,
        ellipse,
        rect,
        line,
        polygon,
        star,
        arc,
        grid,
        translate,
        rotate,
        scale,
        colorize,
    )
except ImportError:
    # Fall back to pure Python implementations if Rust module not available
    from .graphics import Point, Color, Rect, Path
    from .functions import ellipse, rect, line, polygon, star, arc, grid
    from .functions import translate, rotate, scale, colorize

__version__ = "0.1.0"
__all__ = [
    "Point",
    "Color",
    "Rect",
    "Path",
    "ellipse",
    "rect",
    "line",
    "polygon",
    "star",
    "arc",
    "grid",
    "translate",
    "rotate",
    "scale",
    "colorize",
]
