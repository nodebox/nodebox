from net.nodebox import graphics

_process_counter = 0

def number(value):
    """Simply returns the given number."""
    assert isinstance(value, int), "value should be an integer."
    return value

def negate(value):
    """Negates the given values, so 5 becomes -5."""
    assert isinstance(value, int), "value should be an integer."
    return -value

def add(v1, v2):
    """Adds two values, returning the sum."""
    assert isinstance(v1, int)
    assert isinstance(v2, int)
    return v1 + v2

def crash():
    """Method that always crashes when processing."""
    return 1/0

def counter(value):
    """Method that counts how many times it was processed.

    Note that this is something you should never do in real nodes, since
    processing a node should not have side effects."""
    global _process_counter
    _process_counter += 1
    return 42

def string(value):
    """Method that takes a string, and generates a string."""
    assert isinstance(value, basestring)
    return value

def custom(value):
    """Method that takes a custom value, and passes it on to the output."""
    return value

def bounding(*args):
    """Method that only tests the parsing of bounded parameters."""
    return 0

def ellipse(x, y, width, height, color):
    g = graphics.Group()
    p = graphics.BezierPath()
    p.fillColor = color
    p.ellipse(x, y, width, height)
    g.add(p)
    return g

def allcontrols(angle, color, custom, file, float, font, gradient, group, image, int, menu, point, seed, string, text,
                toggle):
    ctx = graphics.Context()
    ctx.font_size = 14

    ctx.background = bgcolor = graphics.Color(*color)
    if bgcolor.red + bgcolor.green + bgcolor.blue < 1.2:
        ctx.fill = graphics.Color(1)

    def _showvar(ctx, name, value, y):
        ctx.text("%s: %s" % (name, value), 20, y)
        return y + 20

    y = 20
    y = _showvar(ctx, "angle", angle, y)
    y = _showvar(ctx, "color", color, y)
    y = _showvar(ctx, "custom", custom, y)
    y = _showvar(ctx, "file", file, y)
    y = _showvar(ctx, "float", float, y)
    y = _showvar(ctx, "font", font, y)
    y = _showvar(ctx, "gradient", gradient, y)
    y = _showvar(ctx, "group", group, y)
    y = _showvar(ctx, "image", image, y)
    y = _showvar(ctx, "int", int, y)
    y = _showvar(ctx, "menu", menu, y)
    y = _showvar(ctx, "point", point, y)
    y = _showvar(ctx, "seed", seed, y)
    y = _showvar(ctx, "string", string, y)
    y = _showvar(ctx, "text", text, y)
    y = _showvar(ctx, "toggle", toggle, y)

    return ctx.canvas

