"""
Pure Python implementations of NodeBox graphics types.

These are used as fallbacks when the Rust module is not available,
and provide type annotations for IDE support.
"""

from __future__ import annotations
from dataclasses import dataclass, field
from typing import Optional, List, Tuple
import math


@dataclass
class Point:
    """A 2D point with x and y coordinates."""

    x: float = 0.0
    y: float = 0.0

    def __add__(self, other: Point) -> Point:
        return Point(self.x + other.x, self.y + other.y)

    def __sub__(self, other: Point) -> Point:
        return Point(self.x - other.x, self.y - other.y)

    def __mul__(self, scalar: float) -> Point:
        return Point(self.x * scalar, self.y * scalar)

    def __truediv__(self, scalar: float) -> Point:
        return Point(self.x / scalar, self.y / scalar)

    def __neg__(self) -> Point:
        return Point(-self.x, -self.y)

    def distance_to(self, other: Point) -> float:
        """Calculate the distance to another point."""
        dx = self.x - other.x
        dy = self.y - other.y
        return math.sqrt(dx * dx + dy * dy)

    def angle_to(self, other: Point) -> float:
        """Calculate the angle to another point in degrees."""
        return math.degrees(math.atan2(other.y - self.y, other.x - self.x))

    @classmethod
    def origin(cls) -> Point:
        """Return the origin point (0, 0)."""
        return cls(0.0, 0.0)

    @classmethod
    def polar(cls, angle: float, distance: float) -> Point:
        """Create a point from polar coordinates (angle in degrees)."""
        rad = math.radians(angle)
        return cls(math.cos(rad) * distance, math.sin(rad) * distance)


@dataclass
class Color:
    """An RGBA color with components in the range 0.0-1.0."""

    r: float = 0.0
    g: float = 0.0
    b: float = 0.0
    a: float = 1.0

    @classmethod
    def rgb(cls, r: float, g: float, b: float) -> Color:
        """Create a color from RGB components."""
        return cls(r, g, b, 1.0)

    @classmethod
    def rgba(cls, r: float, g: float, b: float, a: float) -> Color:
        """Create a color from RGBA components."""
        return cls(r, g, b, a)

    @classmethod
    def hsb(cls, h: float, s: float, b: float) -> Color:
        """Create a color from HSB (Hue, Saturation, Brightness) components."""
        if s == 0:
            return cls(b, b, b, 1.0)

        h = h % 1.0
        h *= 6.0
        i = int(h)
        f = h - i
        p = b * (1 - s)
        q = b * (1 - s * f)
        t = b * (1 - s * (1 - f))

        if i == 0:
            return cls(b, t, p, 1.0)
        elif i == 1:
            return cls(q, b, p, 1.0)
        elif i == 2:
            return cls(p, b, t, 1.0)
        elif i == 3:
            return cls(p, q, b, 1.0)
        elif i == 4:
            return cls(t, p, b, 1.0)
        else:
            return cls(b, p, q, 1.0)

    @classmethod
    def from_hex(cls, hex_str: str) -> Color:
        """Create a color from a hex string (e.g., '#FF0000' or 'FF0000')."""
        hex_str = hex_str.lstrip("#")
        if len(hex_str) == 6:
            r = int(hex_str[0:2], 16) / 255.0
            g = int(hex_str[2:4], 16) / 255.0
            b = int(hex_str[4:6], 16) / 255.0
            return cls(r, g, b, 1.0)
        elif len(hex_str) == 8:
            r = int(hex_str[0:2], 16) / 255.0
            g = int(hex_str[2:4], 16) / 255.0
            b = int(hex_str[4:6], 16) / 255.0
            a = int(hex_str[6:8], 16) / 255.0
            return cls(r, g, b, a)
        else:
            raise ValueError(f"Invalid hex color: {hex_str}")

    def to_hex(self) -> str:
        """Convert to hex string."""
        return "#{:02x}{:02x}{:02x}".format(
            int(self.r * 255), int(self.g * 255), int(self.b * 255)
        )

    @classmethod
    def black(cls) -> Color:
        """Return black color."""
        return cls(0.0, 0.0, 0.0, 1.0)

    @classmethod
    def white(cls) -> Color:
        """Return white color."""
        return cls(1.0, 1.0, 1.0, 1.0)

    @classmethod
    def red(cls) -> Color:
        """Return red color."""
        return cls(1.0, 0.0, 0.0, 1.0)

    @classmethod
    def green(cls) -> Color:
        """Return green color."""
        return cls(0.0, 1.0, 0.0, 1.0)

    @classmethod
    def blue(cls) -> Color:
        """Return blue color."""
        return cls(0.0, 0.0, 1.0, 1.0)


@dataclass
class Rect:
    """A rectangle defined by position and size."""

    x: float = 0.0
    y: float = 0.0
    width: float = 0.0
    height: float = 0.0

    @property
    def left(self) -> float:
        return self.x

    @property
    def right(self) -> float:
        return self.x + self.width

    @property
    def top(self) -> float:
        return self.y

    @property
    def bottom(self) -> float:
        return self.y + self.height

    def center(self) -> Point:
        """Return the center point of the rectangle."""
        return Point(self.x + self.width / 2, self.y + self.height / 2)

    def contains(self, point: Point) -> bool:
        """Check if a point is inside the rectangle."""
        return (
            self.x <= point.x <= self.x + self.width
            and self.y <= point.y <= self.y + self.height
        )


@dataclass
class PathPoint:
    """A point on a path with type information."""

    point: Point
    point_type: str = "lineto"  # "lineto", "curveto", "curvedata"


@dataclass
class Contour:
    """A contour (sub-path) within a path."""

    points: List[PathPoint] = field(default_factory=list)
    closed: bool = False


@dataclass
class Path:
    """A geometric path consisting of one or more contours."""

    contours: List[Contour] = field(default_factory=list)
    fill: Optional[Color] = None
    stroke: Optional[Color] = None
    stroke_width: float = 1.0

    def moveto(self, x: float, y: float) -> Path:
        """Move to a point, starting a new contour."""
        new_path = self.copy()
        new_contour = Contour(
            points=[PathPoint(Point(x, y), "lineto")], closed=False
        )
        new_path.contours.append(new_contour)
        return new_path

    def lineto(self, x: float, y: float) -> Path:
        """Draw a line to a point."""
        new_path = self.copy()
        if new_path.contours:
            new_path.contours[-1].points.append(PathPoint(Point(x, y), "lineto"))
        else:
            return new_path.moveto(x, y)
        return new_path

    def curveto(
        self, x1: float, y1: float, x2: float, y2: float, x3: float, y3: float
    ) -> Path:
        """Draw a cubic bezier curve."""
        new_path = self.copy()
        if new_path.contours:
            contour = new_path.contours[-1]
            contour.points.append(PathPoint(Point(x1, y1), "curveto"))
            contour.points.append(PathPoint(Point(x2, y2), "curvedata"))
            contour.points.append(PathPoint(Point(x3, y3), "curvedata"))
        return new_path

    def close(self) -> Path:
        """Close the current contour."""
        new_path = self.copy()
        if new_path.contours:
            new_path.contours[-1].closed = True
        return new_path

    def copy(self) -> Path:
        """Create a copy of this path."""
        import copy

        return copy.deepcopy(self)

    def bounds(self) -> Optional[Rect]:
        """Get the bounding rectangle of this path."""
        if not self.contours:
            return None

        min_x = float("inf")
        min_y = float("inf")
        max_x = float("-inf")
        max_y = float("-inf")

        for contour in self.contours:
            for pp in contour.points:
                min_x = min(min_x, pp.point.x)
                min_y = min(min_y, pp.point.y)
                max_x = max(max_x, pp.point.x)
                max_y = max(max_y, pp.point.y)

        if min_x == float("inf"):
            return None

        return Rect(min_x, min_y, max_x - min_x, max_y - min_y)

    def contour_count(self) -> int:
        """Get the number of contours."""
        return len(self.contours)

    def point_count(self) -> int:
        """Get the total number of points."""
        return sum(len(c.points) for c in self.contours)

    def points(self) -> List[Tuple[float, float]]:
        """Get all points as a list of (x, y) tuples."""
        result = []
        for contour in self.contours:
            for pp in contour.points:
                result.append((pp.point.x, pp.point.y))
        return result
