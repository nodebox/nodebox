"""
Pure Python implementations of NodeBox functions.

These are used as fallbacks when the Rust module is not available.
"""

from __future__ import annotations
import math
from typing import Optional, List

from .graphics import Point, Color, Path, Contour, PathPoint


def ellipse(
    position: Optional[Point] = None,
    width: Optional[float] = None,
    height: Optional[float] = None,
) -> Path:
    """Create an ellipse path.

    Args:
        position: Center position (default: origin)
        width: Width of the ellipse (default: 100)
        height: Height of the ellipse (default: same as width)

    Returns:
        A Path representing the ellipse.
    """
    pos = position or Point(0, 0)
    w = width or 100.0
    h = height or w

    # Approximate ellipse with bezier curves
    rx = w / 2
    ry = h / 2
    cx = pos.x
    cy = pos.y

    # Magic number for bezier approximation of circle
    k = 0.5522847498

    points = [
        PathPoint(Point(cx, cy - ry), "lineto"),  # Top
        PathPoint(Point(cx + rx * k, cy - ry), "curveto"),
        PathPoint(Point(cx + rx, cy - ry * k), "curvedata"),
        PathPoint(Point(cx + rx, cy), "curvedata"),  # Right
        PathPoint(Point(cx + rx, cy + ry * k), "curveto"),
        PathPoint(Point(cx + rx * k, cy + ry), "curvedata"),
        PathPoint(Point(cx, cy + ry), "curvedata"),  # Bottom
        PathPoint(Point(cx - rx * k, cy + ry), "curveto"),
        PathPoint(Point(cx - rx, cy + ry * k), "curvedata"),
        PathPoint(Point(cx - rx, cy), "curvedata"),  # Left
        PathPoint(Point(cx - rx, cy - ry * k), "curveto"),
        PathPoint(Point(cx - rx * k, cy - ry), "curvedata"),
        PathPoint(Point(cx, cy - ry), "curvedata"),  # Back to top
    ]

    contour = Contour(points=points, closed=True)
    return Path(contours=[contour])


def rect(
    position: Optional[Point] = None,
    width: Optional[float] = None,
    height: Optional[float] = None,
    roundness: Optional[float] = None,
) -> Path:
    """Create a rectangle path.

    Args:
        position: Top-left position (default: origin)
        width: Width of the rectangle (default: 100)
        height: Height of the rectangle (default: same as width)
        roundness: Corner roundness (default: 0)

    Returns:
        A Path representing the rectangle.
    """
    pos = position or Point(0, 0)
    w = width or 100.0
    h = height or w
    r = roundness or 0.0

    x = pos.x
    y = pos.y

    if r <= 0:
        # Simple rectangle
        points = [
            PathPoint(Point(x, y), "lineto"),
            PathPoint(Point(x + w, y), "lineto"),
            PathPoint(Point(x + w, y + h), "lineto"),
            PathPoint(Point(x, y + h), "lineto"),
        ]
    else:
        # Rounded rectangle
        r = min(r, w / 2, h / 2)
        k = 0.5522847498 * r

        points = [
            PathPoint(Point(x + r, y), "lineto"),
            PathPoint(Point(x + w - r, y), "lineto"),
            PathPoint(Point(x + w - r + k, y), "curveto"),
            PathPoint(Point(x + w, y + r - k), "curvedata"),
            PathPoint(Point(x + w, y + r), "curvedata"),
            PathPoint(Point(x + w, y + h - r), "lineto"),
            PathPoint(Point(x + w, y + h - r + k), "curveto"),
            PathPoint(Point(x + w - r + k, y + h), "curvedata"),
            PathPoint(Point(x + w - r, y + h), "curvedata"),
            PathPoint(Point(x + r, y + h), "lineto"),
            PathPoint(Point(x + r - k, y + h), "curveto"),
            PathPoint(Point(x, y + h - r + k), "curvedata"),
            PathPoint(Point(x, y + h - r), "curvedata"),
            PathPoint(Point(x, y + r), "lineto"),
            PathPoint(Point(x, y + r - k), "curveto"),
            PathPoint(Point(x + r - k, y), "curvedata"),
            PathPoint(Point(x + r, y), "curvedata"),
        ]

    contour = Contour(points=points, closed=True)
    return Path(contours=[contour])


def line(
    point1: Optional[Point] = None,
    point2: Optional[Point] = None,
    points: Optional[int] = None,
) -> Path:
    """Create a line path between two points.

    Args:
        point1: Start point (default: origin)
        point2: End point (default: (100, 100))
        points: Number of points on the line (default: 2)

    Returns:
        A Path representing the line.
    """
    p1 = point1 or Point(0, 0)
    p2 = point2 or Point(100, 100)
    n = points or 2

    if n <= 2:
        path_points = [
            PathPoint(Point(p1.x, p1.y), "lineto"),
            PathPoint(Point(p2.x, p2.y), "lineto"),
        ]
    else:
        path_points = []
        for i in range(n):
            t = i / (n - 1)
            x = p1.x + (p2.x - p1.x) * t
            y = p1.y + (p2.y - p1.y) * t
            path_points.append(PathPoint(Point(x, y), "lineto"))

    contour = Contour(points=path_points, closed=False)
    return Path(contours=[contour])


def polygon(
    position: Optional[Point] = None,
    radius: Optional[float] = None,
    sides: Optional[int] = None,
) -> Path:
    """Create a regular polygon path.

    Args:
        position: Center position (default: origin)
        radius: Radius of the polygon (default: 50)
        sides: Number of sides (default: 6)

    Returns:
        A Path representing the polygon.
    """
    pos = position or Point(0, 0)
    r = radius or 50.0
    n = sides or 6

    points = []
    for i in range(n):
        angle = -math.pi / 2 + (2 * math.pi * i / n)
        x = pos.x + r * math.cos(angle)
        y = pos.y + r * math.sin(angle)
        points.append(PathPoint(Point(x, y), "lineto"))

    contour = Contour(points=points, closed=True)
    return Path(contours=[contour])


def star(
    position: Optional[Point] = None,
    points: Optional[int] = None,
    outer_radius: Optional[float] = None,
    inner_radius: Optional[float] = None,
) -> Path:
    """Create a star path.

    Args:
        position: Center position (default: origin)
        points: Number of star points (default: 5)
        outer_radius: Outer radius (default: 50)
        inner_radius: Inner radius (default: 25)

    Returns:
        A Path representing the star.
    """
    pos = position or Point(0, 0)
    n = points or 5
    outer = outer_radius or 50.0
    inner = inner_radius or 25.0

    path_points = []
    for i in range(n * 2):
        radius = outer if i % 2 == 0 else inner
        angle = -math.pi / 2 + (math.pi * i / n)
        x = pos.x + radius * math.cos(angle)
        y = pos.y + radius * math.sin(angle)
        path_points.append(PathPoint(Point(x, y), "lineto"))

    contour = Contour(points=path_points, closed=True)
    return Path(contours=[contour])


def arc(
    position: Optional[Point] = None,
    width: Optional[float] = None,
    height: Optional[float] = None,
    start_angle: Optional[float] = None,
    degrees: Optional[float] = None,
    arc_type: Optional[str] = None,
) -> Path:
    """Create an arc or pie slice path.

    Args:
        position: Center position (default: origin)
        width: Width of the ellipse (default: 100)
        height: Height of the ellipse (default: same as width)
        start_angle: Starting angle in degrees (default: 0)
        degrees: Arc angle in degrees (default: 360)
        arc_type: "pie" or "chord" (default: "pie")

    Returns:
        A Path representing the arc.
    """
    pos = position or Point(0, 0)
    w = width or 100.0
    h = height or w
    start = start_angle or 0.0
    deg = degrees or 360.0
    typ = arc_type or "pie"

    rx = w / 2
    ry = h / 2

    # Number of segments based on arc length
    n = max(2, int(abs(deg) / 30))

    points = []
    for i in range(n + 1):
        angle = math.radians(start + deg * i / n)
        x = pos.x + rx * math.cos(angle)
        y = pos.y + ry * math.sin(angle)
        points.append(PathPoint(Point(x, y), "lineto"))

    if typ == "pie" and abs(deg) < 360:
        points.append(PathPoint(Point(pos.x, pos.y), "lineto"))

    contour = Contour(points=points, closed=True)
    return Path(contours=[contour])


def grid(
    rows: Optional[int] = None,
    columns: Optional[int] = None,
    width: Optional[float] = None,
    height: Optional[float] = None,
    position: Optional[Point] = None,
) -> List[Point]:
    """Create a grid of points.

    Args:
        rows: Number of rows (default: 5)
        columns: Number of columns (default: 5)
        width: Total width (default: 200)
        height: Total height (default: 200)
        position: Top-left position (default: origin)

    Returns:
        A list of Points forming a grid.
    """
    r = rows or 5
    c = columns or 5
    w = width or 200.0
    h = height or 200.0
    pos = position or Point(0, 0)

    points = []
    for row in range(r):
        for col in range(c):
            x = pos.x + (col + 0.5) * (w / c)
            y = pos.y + (row + 0.5) * (h / r)
            points.append(Point(x, y))

    return points


def translate(
    path: Path,
    tx: Optional[float] = None,
    ty: Optional[float] = None,
) -> Path:
    """Translate a path by an offset.

    Args:
        path: The path to translate
        tx: X offset (default: 0)
        ty: Y offset (default: 0)

    Returns:
        A new translated path.
    """
    offset_x = tx or 0.0
    offset_y = ty or 0.0

    new_path = path.copy()
    for contour in new_path.contours:
        for pp in contour.points:
            pp.point.x += offset_x
            pp.point.y += offset_y

    return new_path


def rotate(
    path: Path,
    angle: float,
    origin: Optional[Point] = None,
) -> Path:
    """Rotate a path around a point.

    Args:
        path: The path to rotate
        angle: Rotation angle in degrees
        origin: Center of rotation (default: origin)

    Returns:
        A new rotated path.
    """
    o = origin or Point(0, 0)
    rad = math.radians(angle)
    cos_a = math.cos(rad)
    sin_a = math.sin(rad)

    new_path = path.copy()
    for contour in new_path.contours:
        for pp in contour.points:
            x = pp.point.x - o.x
            y = pp.point.y - o.y
            pp.point.x = x * cos_a - y * sin_a + o.x
            pp.point.y = x * sin_a + y * cos_a + o.y

    return new_path


def scale(
    path: Path,
    sx: Optional[float] = None,
    sy: Optional[float] = None,
    origin: Optional[Point] = None,
) -> Path:
    """Scale a path.

    Args:
        path: The path to scale
        sx: X scale factor as percentage (default: 100)
        sy: Y scale factor as percentage (default: same as sx)
        origin: Center of scaling (default: origin)

    Returns:
        A new scaled path.
    """
    scale_x = (sx or 100.0) / 100.0
    scale_y = (sy or sx or 100.0) / 100.0
    o = origin or Point(0, 0)

    new_path = path.copy()
    for contour in new_path.contours:
        for pp in contour.points:
            pp.point.x = (pp.point.x - o.x) * scale_x + o.x
            pp.point.y = (pp.point.y - o.y) * scale_y + o.y

    return new_path


def colorize(
    path: Path,
    fill: Optional[Color] = None,
    stroke: Optional[Color] = None,
    stroke_width: Optional[float] = None,
) -> Path:
    """Set fill and stroke colors on a path.

    Args:
        path: The path to colorize
        fill: Fill color (default: black)
        stroke: Stroke color (default: black)
        stroke_width: Stroke width (default: 1)

    Returns:
        A new path with colors applied.
    """
    new_path = path.copy()
    new_path.fill = fill or Color.black()
    new_path.stroke = stroke or Color.black()
    new_path.stroke_width = stroke_width or 1.0
    return new_path
