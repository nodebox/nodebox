from random import uniform

from nodebox.graphics import Geometry, Path, Contour, Color, Transform, Text, Point, Rect
from nodebox.util.Geometry import coordinates, angle, distance

# Ported from Sean McCullough's Processing code:
# http://www.cricketschirping.com/processing/CirclePacking1/
# See also: http://en.wiki.mcneel.com/default.aspx/McNeel/2DCirclePacking

class PackObject:

    def __init__(self, path):
        self.path = path
        self.bounds = path.bounds
        self.x = self.bounds.x + self.bounds.width / 2
        self.y = self.bounds.y + self.bounds.height / 2
        self.radius = max(self.bounds.width, self.bounds.height) / 2

    def _offset(self):
        return distance(self.x, self.y, 0, 0)
    offset = property(_offset)

    def contains(self, x, y):
        return distance(self.x, self.y, x, y) <= self.radius

    def intersects(self, other):
        d = distance(self.x, self.y, other.x, other.y)
        return d < self.radius + other.radius

def _pack(circles, damping=0.1, padding=2, exclude=[]):

    circles.sort(lambda a, b: a.offset < b.offset)

    # Repulsive force: move away from intersecting circles.
    for i in range(len(circles)):
        circle1 = circles[i]
        for j in range(i+1, len(circles)):
            circle2 = circles[j]
            d = distance(circle1.x, circle1.y, circle2.x, circle2.y)
            if d == 0: d = 0.001
            r = circle1.radius + circle2.radius + padding
            if d**2 < r**2 - 0.01:
                dx = circle2.x - circle1.x
                dy = circle2.y - circle1.y
                vx = (dx / d) * (r-d) * 0.5
                vy = (dy / d) * (r-d) * 0.5
                if circle1 not in exclude:
                    circle1.x -= vx
                    circle1.y -= vy
                if circle2 not in exclude:
                    circle2.x += vx
                    circle2.y += vy

    # Attractive force: all circles move to center.
    for circle in circles:
        if circle not in exclude:
            vx = circle.x * damping
            vy = circle.y * damping
            circle.x -= vx
            circle.y -= vy


from random import seed as _seed

def pack(shapes, iterations, padding, seed):
    if shapes is None: return None
    _seed(seed)
    packed_objects = []
    for path in shapes:
        packed_objects.append(PackObject(path))
    for i in xrange(1, iterations):
        _pack(packed_objects, damping=0.1/i, padding=padding)


    geo = Geometry()
    for po in packed_objects:
        print po.x, po.y
        p = Transform.translated(po.x, po.y).map(po.path)
        geo.add(p)
    return geo

def divide_and_conquer(fn, vmin, vmax, limit=0):
    """Converge to a solution between minimum and maximum.

    fn - The query function. It takes one argument, and should
         return 1 if the value is too high, -1 if the value is
         too low, and 0 if the value is correct.
    vmin - The minimum value to try, inclusive.
    vmax - The maximum value to try, exclusive.
    limit - The delta limit. If this limit is reached, we bail out.
    """

    delta = vmax - vmin
    v = vmin + delta / 2
    if delta <= limit: return v
    r = fn(v)
    if r < 0: return divide_and_conquer(fn, v, vmax, limit)
    elif r > 0: return divide_and_conquer(fn, vmin, v, limit)
    else: return v

def try_angle(bounding_path, shape, angle, limit, maximum_radius, use_bounding_box=False):
    def shape_intersects(distance):
        tx, ty = coordinates(0, 0, distance, angle)
        t = Transform()
        t.translate(tx, ty)
        translated_shape = t.map(shape)
        if use_bounding_box:
            b = Path()
            b.cornerRect(translated_shape.bounds)
        else:
            b = translated_shape
        # If the shape intersects it is too close (the distance is too low).
        if bounding_path.intersects(b):
            return -1
        return 1

    return divide_and_conquer(shape_intersects, 0.0, maximum_radius, limit)


def angle_pack(shapes, seed, limit, maximum_radius, angle_tries=1, use_bounding_box=False):
    if shapes is None: return None
    _seed(seed)

    def center_and_translate(shape, tx=0, ty=0):
        bx, by, bw, bh = list(shape.bounds)
        t = Transform()
        t.translate(-bw / 2 - bx, -bh / 2 - by)
        return t.map(shape)

    geo = Geometry()
    bounding_path = Path()

    # Center first shape
    first_shape = center_and_translate(shapes[0])
    geo.add(first_shape)
    bounding_path.cornerRect(first_shape.bounds)

    for shape in shapes[1:]:
        centered_shape = center_and_translate(shape)

        angles = []
        for i in range(angle_tries):
            a = uniform(0, 360)
            if use_bounding_box:
                d = try_angle(bounding_path, centered_shape, a, limit, maximum_radius, use_bounding_box)
            else:
                d = try_angle(geo, centered_shape, a, limit, maximum_radius, use_bounding_box)
            angles.append([d, a])
        chosen_distance, chosen_angle = sorted(angles)[0]

        tx, ty = coordinates(0, 0, chosen_distance, chosen_angle)
        t = Transform()
        t.translate(tx, ty)
        translated_shape = t.map(centered_shape)
        bounding_path.cornerRect(translated_shape.bounds)
        geo.add(translated_shape)

    return geo



