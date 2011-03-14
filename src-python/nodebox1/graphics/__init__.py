from java.lang import Boolean
from nodebox.graphics import CanvasContext, Point, Path, Text, NodeBoxError
from random import choice

class Context(CanvasContext):
    def __init__(self, canvas=None, ns=None):
        args = canvas is not None and [canvas] or []
        CanvasContext.__init__(self, *args)
        if ns is None:
            ns = {}
        self._ns = ns
        
        # todo: better way to handle mouse events
        self._ns["MOUSEX"] = 0
        self._ns["MOUSEY"] = 0
        self._ns["mousedown"] = False

    def ximport(self, libName):
        lib = __import__(libName)
        self._ns[libName] = lib
        lib._ctx = self
        return lib
        
    def size(self, width, height):
        CanvasContext.size(self, width, height)
        # To keep the WIDTH and HEIGHT properties up to date in the executing script,
        # the Context object must have access to its namespace. Normally, we passed this
        # during construction time.
        self._ns["WIDTH"] = width
        self._ns["HEIGHT"] = height

    def background(self, *args):
        if len(args) == 1 and isinstance(args[0], tuple):
            args = args[0]
        return CanvasContext.background(self, *args)

    #### Primitives ####
    
    def rect(self, x, y, width, height, roundness=0.0, draw=True, **kwargs):
        if roundness == 0:
            p = CanvasContext.rect(self, x, y, width, height, Boolean(draw))
        else:
            rw = roundness * width * 0.5
            rh = roundness * height * 0.5
            p = CanvasContext.rect(self, x, y, width, height, rw, rh, Boolean(draw))
        self._setAttributesFromKwargs(p, **kwargs)
        return p

    def ellipse(self, x, y, width, height, draw=True, **kwargs):
        p = CanvasContext.ellipse(self, x, y, width, height, Boolean(draw))
        self._setAttributesFromKwargs(p, **kwargs)
        return p
    oval = ellipse
    
    def line(self, x1, y1, x2, y2, draw=True, **kwargs):
        p = CanvasContext.line(self, x1, y1, x2, y2, Boolean(draw))
        self._setAttributesFromKwargs(p, **kwargs)
        return p
    
    def star(self, startx, starty, points=20, outer=100, inner=50, draw=True, **kwargs):
        p = CanvasContext.star(self, startx, starty, points, outer, inner, Boolean(draw))
        self._setAttributesFromKwargs(p, **kwargs)
        return p
    
    def arrow(self, x, y, width=100, type=CanvasContext.NORMAL, draw=True, **kwargs):
        p = CanvasContext.arrow(self, x, y, width, type, Boolean(draw))
        self._setAttributesFromKwargs(p, **kwargs)
        return p
        
    ### Path Commands ###

    def beginpath(self, x=None, y=None):
        if x != None and y != None:
            CanvasContext.beginpath(self, x, y)
        else:
            CanvasContext.beginpath(self)
    
    def endpath(self, draw=True):
        return CanvasContext.endpath(self, Boolean(draw))
    
    def drawpath(self, path, **kwargs):
        CanvasContext.drawpath(self, path)
        self._setAttributesFromKwargs(path, **kwargs)
    
    def autoclosepath(self, close=None):
        if close is None:
            return CanvasContext.autoclosepath(self)
        else:
            return CanvasContext.autoclosepath(self, Boolean(close))
    
    def findpath(self, points, curvature=1.0):
        # The list of points consists of Point objects,
        # but it shouldn't crash on something straightforward
        # as someone supplying a list of (x,y)-tuples.

        from types import TupleType
        for i, pt in enumerate(points):
            if type(pt) == TupleType:
                points[i] = Point(pt[0], pt[1])
        return CanvasContext.findpath(self, points, curvature)
    
    ### Transformation commands ###
    
    def transform(self, mode=None):
        if mode is None:
            return CanvasContext.transform(self)
        else:
            return CanvasContext.transform(self, mode)
    
    def translate(self, tx=0, ty=0):
        CanvasContext.translate(self, tx, ty)
    
    def rotate(self, degrees=0, radians=0):
        # todo: radians
        CanvasContext.rotate(self, degrees)
    
    def scale(self, sx=1, sy=None):
        if sy is None:
            CanvasContext.scale(self, sx)
        else:
            CanvasContext.scale(self, sx, sy)
    
    def skew(self, kx=0, ky=None):
        if ky is None:
            CanvasContext.skew(self, kx)
        else:
            CanvasContext.skew(self, kx, ky)
    
    ### Color Commands ###

    def color(self, *args):
        if len(args) == 1 and isinstance(args[0], tuple):
            args = args[0]
        return CanvasContext.color(self, *args)

    Color = color
    
    def fill(self, *args):
        if len(args) == 1 and isinstance(args[0], tuple):
            args = args[0]
        return CanvasContext.fill(self, *args)

    def stroke(self, *args):
        if len(args) == 1 and isinstance(args[0], tuple):
            args = args[0]
        return CanvasContext.stroke(self, *args)

    def colormode(self, mode=None, range=None):
        if mode is None:
            if range is not None:
                CanvasContext.colorrange(self, range)
            return CanvasContext.colormode(self)
        return CanvasContext.colormode(self, mode, range)
    
    def colorrange(self, range=None):
        if range is not None:
            return CanvasContext.colorrange(self, range)
        return CanvasContext.colorrange(self)

    def strokewidth(self, width=None):
        if width is not None:
            return CanvasContext.strokewidth(self, width)
        return CanvasContext.strokewidth(self)
    
    ### Font Commands ###
    
    def font(self, fontname=None, fontsize=None):
        if fontname is not None and fontsize is not None:
            return CanvasContext.font(self, fontname, fontsize)
        elif fontname is not None:
            return CanvasContext.font(self, fontname)
        elif fontsize is not None:
            CanvasContext.fontsize(self, fontsize)
        return CanvasContext.font(self)
    
    def fontsize(self, fontsize=None):
        if fontsize is not None:
            return CanvasContext.fontsize(self, fontsize)
        return CanvasContext.fontsize(self)
            
    def lineheight(self, lineheight=None):
        if lineheight is not None:
            return CanvasContext.lineheight(self, lineheight)
        return CanvasContext.lineheight(self)

    def align(self, align=None):
        if align is not None:
            return CanvasContext.align(self, align)
        return CanvasContext.align(self)
    
    def text(self, txt, x, y, width=0, height=0, outline=False, draw=True, **kwargs):
        if outline:
            t = CanvasContext.text(self, unicode(txt), x, y, width, height, Boolean(False))
            p = t.path
            self._setAttributesFromKwargs(p, **kwargs)
            if draw:
                self.addPath(p)
            return p
        else:
            t = CanvasContext.text(self, unicode(txt), x, y, width, height, Boolean(draw))
            self._setAttributesFromKwargs(t, **kwargs)
            return t
            
    def textpath(self, txt, x, y, width=None, height=None, **kwargs):
        if width is None: width = 0
        if height is None: height = 0
        p = CanvasContext.textpath(self, unicode(txt), x, y, width, height)
        self._setAttributesFromKwargs(p, **kwargs)
        return p

    def textmetrics(self, txt, width=None, height=None, **kwargs):
        if width is None: width = 0
        if height is None: height = 0
        r = CanvasContext.textmetrics(self, unicode(txt), width, height)
        # todo: handle kwargs?
        return r

    def textwidth(self, txt, width=None, **kwargs):
        if width is None: width = 0
        w = CanvasContext.textwidth(self, unicode(txt), width)
        # todo: handle kwargs?
        return w

    def textheight(self, txt, height=None, **kwargs):
        if height is None: height = 0
        h = CanvasContext.textheight(self, unicode(txt), height)
        # todo: handle kwargs?
        return h
    
    ### Image commands ###

    def image(self, path, x, y, width=None, height=None, alpha=1.0, data=None, draw=True, **kwargs):
        if data is not None:
            from nodebox.graphics import Image
            arg = Image.fromData(data).awtImage
        else:
            arg = path
        img = CanvasContext.image(self, arg, x, y, width, height, alpha, Boolean(draw))
        # todo: handle data and kwargs
        return img

    def imagesize(self, path, data=None):
        if data is not None:
            from nodebox.graphics import Image
            arg = Image.fromData(data).awtImage
        else:
            arg = path
        return CanvasContext.imagesize(self, arg)

    def _setAttributesFromKwargs(self, item, **kwargs):
        if isinstance(item, Path):
            kwargs_attrs = (('fill', 'fillColor'), ('stroke', 'strokeColor'), ('strokewidth', 'strokeWidth'))
        elif isinstance(item, Text):
            kwargs_attrs = (('fill', 'fillColor'), ('font', 'fontName'), ('fontsize', 'fontSize'), ('align', 'align'), ('lineheight', 'lineHeight'))
        else:
            kwargs_attrs = ()
        keys = kwargs.keys()
        for kwarg, attr in kwargs_attrs:
            if kwarg in keys:
                v = kwargs.pop(kwarg)
                if kwarg in ('fill', 'stroke'):
                    if isinstance(v, (int, float)):
                        v = self.color(v)
                    elif isinstance(v, tuple):
                        v = self.color(*v)
                setattr(item, attr, v)
        remaining = kwargs.keys()
        if remaining:
            raise NodeBoxError, "Unknown argument(s) '%s'" % ", ".join(remaining)

    #### util ####

    def random(self, v1=None, v2=None):
        """Returns a random value.

        This function does a lot of things depending on the parameters:
        - If one or more floats is given, the random value will be a float.
        - If all values are ints, the random value will be an integer.

        - If one value is given, random returns a value from 0 to the given value.
          This value is not inclusive.
        - If two values are given, random returns a value between the two; if two
          integers are given, the two boundaries are inclusive.
        """
        import random
        if v1 != None and v2 == None: # One value means 0 -> v1
            if isinstance(v1, float):
                return random.random() * v1
            else:
                return int(random.random() * v1)
        elif v1 != None and v2 != None: # v1 -> v2
            if isinstance(v1, float) or isinstance(v2, float):
                start = min(v1, v2)
                end = max(v1, v2)
                return start + random.random() * (end-start)
            else:
                start = min(v1, v2)
                end = max(v1, v2) + 1
                return int(start + random.random() * (end-start))
        else: # No values means 0.0 -> 1.0
            return random.random()

    def choice(self, *args):
        return choice(*args)
    
    def var(self, *args):
        CanvasContext.var(self, *args)
        v = self.findVar(args[0])
        if v is not None:
            self._ns[v.name] = v.value
