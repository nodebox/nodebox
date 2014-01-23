### CREDITS ##########################################################################################

# Copyright (c) 2007 Tom De Smedt.
# See LICENSE.txt for details.
# See also the SVG DOM specification: http://www.w3.org/TR/SVG/

__author__    = "Tom De Smedt"
__version__   = "1.9.4.4"
__copyright__ = "Copyright (c) 2007 Tom De Smedt"
__license__   = "GPL"

import xml.dom.minidom as parser
import re
import md5
from math import sin, cos, pi, ceil, atan2, sqrt
from nodebox.graphics import Path, Color, Point, Transform, Geometry

#### CACHE ###########################################################################################

class cache(dict):
    
    """ Caches BezierPaths from parsed SVG data.
    """
    
    def id(self, svg):
        hash = md5.new()
        hash.update(svg)
        return hash.digest()
        
    def save(self, id, paths):
        self[id] = paths
        
    def load(self, id, copy=True):  
        if self.has_key(id):
            if copy: 
                return [path.clone() for path in self[id]]
            return self[id]
    
    def clear(self):
        for k in self.keys(): del self[k]
        
_cache = cache()

#### SVG PARSER ######################################################################################

def parse(svg, cached=False, _copy=True):
    
    """ Returns cached copies unless otherwise specified.
    """
    
    if not cached:
        dom = parser.parseString(svg)
        paths = parse_node(dom, [])
    else:
        id = _cache.id(svg)
        if not _cache.has_key(id):
            dom = parser.parseString(svg)
            _cache.save(id, parse_node(dom, []))
        paths = _cache.load(id, _copy)
   
    return paths
    
def parse_groups(svg):
    """ Returns groups dict
    """

    no_id_count = 0
    groups = {}
    dom = parser.parseString(svg)

    for group in dom.getElementsByTagName('g'):
        if group.hasAttribute('id'):
            groups[group.attributes['id'].value] = parse_node(group,[])
        else:
            groups['no_id_'+str(no_id_count)] = parse_node(group,[])
            no_id_count += 1

    return groups
    
def get_attribute(element, attribute, default=0):
    
    """ Returns XML element's attribute, or default if none.
    """ 
    
    a = element.getAttribute(attribute)
    if a == "": 
        return default
    return a

#--- XML NODE ----------------------------------------------------------------------------------------

def parse_node(node, paths=[], ignore=["pattern"]):
    
    """ Recurse the node tree and find drawable tags.
    
    Recures all the children in the node.
    If a child is something we can draw,
    a line, rect, oval or path,
    parse it to a PathElement drawable with drawpath()
    
    """
    
    # Ignore paths in Illustrator pattern swatches etc.
    if node.nodeType == node.ELEMENT_NODE and node.tagName in ignore: 
        return []
    
    if node.hasChildNodes():
        for child in node.childNodes:
            paths = parse_node(child, paths)
    
    if node.nodeType == node.ELEMENT_NODE:
        
        if node.tagName == "line":
            paths.append(parse_line(node))
        elif node.tagName == "rect":
            paths.append(parse_rect(node))
        elif node.tagName == "circle":
            paths.append(parse_circle(node))
        elif node.tagName == "ellipse":
            paths.append(parse_oval(node))
        elif node.tagName == "polygon":
            paths.append(parse_polygon(node))
        elif node.tagName == "polyline":
            paths.append(parse_polygon(node))
        elif node.tagName == "path":
            paths.append(parse_path(node))
            
        if node.tagName in ("line", "rect", "circle", "ellipse", "polygon", "polyline", "path"):
            paths[-1] = parse_transform(node, paths[-1])
            paths[-1] = add_color_info(node, paths[-1])
    
    return paths

#--- LINE --------------------------------------------------------------------------------------------

def parse_line(e):
    
    x1 = float(get_attribute(e, "x1"))
    y1 = float(get_attribute(e, "y1"))
    x2 = float(get_attribute(e, "x2"))
    y2 = float(get_attribute(e, "y2"))
    p = Path()
    p.line(x1, y1, x2, y2)
    return p

#--- RECT --------------------------------------------------------------------------------------------

def parse_rect(e):
    
    x = float(get_attribute(e, "x"))
    y = float(get_attribute(e, "y"))
    w = float(get_attribute(e, "width"))
    h = float(get_attribute(e, "height"))
    p = Path()
    p.rect(x+w/2, y+h/2, w, h)
    return p

#--- CIRCLE -----------------------------------------------------------------------------------------

def parse_circle(e):
    
    x = float(get_attribute(e, "cx"))
    y = float(get_attribute(e, "cy"))
    r = float(get_attribute(e, "r"))
    p = Path()
    p.ellipse(x, y, r*2, r*2)
    p.close()
    return p

#--- OVAL -------------------------------------------------------------------------------------------

def parse_oval(e):
    
    x = float(get_attribute(e, "cx"))
    y = float(get_attribute(e, "cy"))
    w = float(get_attribute(e, "rx"))*2
    h = float(get_attribute(e, "ry"))*2
    p = Path()
    p.ellipse(x, y, w, h)
    p.close()
    return p

#--- POLYGON -----------------------------------------------------------------------------------------

def parse_polygon(e):
    
    d = get_attribute(e, "points", default="")
    d = d.replace(" ", ",")
    d = d.replace("-", ",")
    d = d.split(",")
    points = []
    for x in d:
        if x != "": points.append(float(x))
    
    autoclosepath = True
    if (e.tagName == "polyline") :
        autoclosepath = False

    p = Path()
    p.moveto(points[0], points[1])
    for i in range(len(points)/2):
        p.lineto(points[i*2], points[i*2+1])
    if autoclosepath:
        p.close()
    return p

#--- PATH --------------------------------------------------------------------------------------------

class PathParser(object):
    def __init__(self, d):
        self.tokens = [el for el in d.split(" ") if el]

    def reset(self):
        self.i = -1
        self.command = ''
        self.previousCommand = ''
        self.start = Point.ZERO
        self.control = Point.ZERO
        self.current = Point.ZERO
        self.points = []
        self.angles = []

    def isEnd(self):
        return self.i >= len(self.tokens) - 1

    def isCommandOrEnd(self):
        if self.isEnd(): return True
        return re.match("^[A-Za-z]$", self.tokens[self.i + 1]) != None

    def isRelativeCommand(self):
        return self.command in ['m', 'l', 'h', 'v', 'c', 's', 'q', 't', 'a', 'z']

    def getToken(self):
        self.i += 1
        return self.tokens[self.i]

    def getScalar(self):
        t = self.getToken()
        return float(t)
#        return float(self.getToken())

    def nextCommand(self):
        self.previousCommand = self.command
        self.command = self.getToken()

    def getPoint(self):
        pt = Point(self.getScalar(), self.getScalar())
        return self.makeAbsolute(pt)

    def getAsControlPoint(self):
        pt = self.getPoint()
        self.control = pt
        return pt

    def getAsCurrentPoint(self):
        pt = self.getPoint()
        self.current = pt
        return pt

    def getReflectedControlPoint(self):
        if self.previousCommand.lower() != 'c' and\
                self.previousCommand.lower() != 's' and\
                self.previousCommand.lower() != 'q' and\
                self.previousCommand.lower() != 't':
            return self.current

        # reflect point
        pt = Point(2 * self.current.x - self.control.x, 2 * self.current.y - self.control.y)
        return pt

    def makeAbsolute(self, p):
        if self.isRelativeCommand():
            return Point(p.x + self.current.x, p.y + self.current.y)
        return p


# Arc construction, code ported from fabric.js: http://fabricjs.com
def arcToSegments(x, y, rx, ry, large, sweep, rotateX, ox, oy):
    th = rotateX * (pi / 180)
    sin_th = sin(th)
    cos_th = cos(th)
    rx = abs(rx)
    ry = abs(ry)
    px = cos_th * (ox - x) * 0.5 + sin_th * (oy - y) * 0.5
    py = cos_th * (oy - y) * 0.5 - sin_th * (ox - x) * 0.5
    pl = (px * px) / (rx * rx) + (py * py) / (ry * ry)
    if pl > 1:
        pl = sqrt(pl)
        rx *= pl
        ry *= pl

    a00 = cos_th / rx
    a01 = sin_th / rx
    a10 = (-sin_th) / ry
    a11 = cos_th / ry
    x0 = a00 * ox + a01 * oy
    y0 = a10 * ox + a11 * oy
    x1 = a00 * x + a01 * y
    y1 = a10 * x + a11 * y

    d = (x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0)
    sfactor_sq = 1 / d - 0.25
    if (sfactor_sq < 0): sfactor_sq = 0
    sfactor = sqrt(sfactor_sq)
    if (sweep == large): sfactor = -sfactor
    xc = 0.5 * (x0 + x1) - sfactor * (y1 - y0)
    yc = 0.5 * (y0 + y1) + sfactor * (x1 - x0)

    th0 = atan2(y0 - yc, x0 - xc)
    th1 = atan2(y1 - yc, x1 - xc)

    th_arc = th1 - th0
    if th_arc < 0 and sweep == 1:
        th_arc += 2 * pi
    elif th_arc > 0 and sweep == 0:
        th_arc -= 2 * pi

    segments = ceil(abs(th_arc / (pi * 0.5 + 0.001)))
    result = []
    for i in range(segments):
        th2 = th0 + i * th_arc / segments
        th3 = th0 + (i + 1) * th_arc / segments
        result.append([xc, yc, th2, th3, rx, ry, sin_th, cos_th])

    return result

def segmentToBezier(cx, cy, th0, th1, rx, ry, sin_th, cos_th):
    a00 = cos_th * rx
    a01 = -sin_th * ry
    a10 = sin_th * rx
    a11 = cos_th * ry

    th_half = 0.5 * (th1 - th0)
    t = (8 / 3) * sin(th_half * 0.5) * sin(th_half * 0.5) / sin(th_half)
    x1 = cx + cos(th0) - t * sin(th0)
    y1 = cy + sin(th0) + t * cos(th0)
    x3 = cx + cos(th1)
    y3 = cy + sin(th1)
    x2 = x3 + t * sin(th1)
    y2 = y3 - t * cos(th1)

    return [
        a00 * x1 + a01 * y1, a10 * x1 + a11 * y1,
        a00 * x2 + a01 * y2, a10 * x2 + a11 * y2,
        a00 * x3 + a01 * y3, a10 * x3 + a11 * y3
    ]

# Individual path construction ported from canvg library: https://code.google.com/p/canvg/
def parse_path(e):

    d = get_attribute(e, "d", default="")
    d = re.sub(r",", r" ", d) # get rid of all commas
    d = re.sub(r"([MmZzLlHhVvCcSsQqTtAa])([MmZzLlHhVvCcSsQqTtAa])", r"\1 \2", d) # separate commands from commands
    d = re.sub(r"([MmZzLlHhVvCcSsQqTtAa])([MmZzLlHhVvCcSsQqTtAa])", r"\1 \2", d) # separate commands from commands
    d = re.sub(r"([MmZzLlHhVvCcSsQqTtAa])([^\s])", r"\1 \2", d) # separate commands from points
    d = re.sub(r"([^\s])([MmZzLlHhVvCcSsQqTtAa])", r"\1 \2", d) # separate commands from points
    d = re.sub(r"([0-9])([+\-])", r"\1 \2", d) # separate digits when no comma
    d = re.sub(r"(\.[0-9]*)(\.)", r"\1 \2", d) # separate digits when no comma
    d = re.sub(r"([Aa](\s+[0-9]+){3})\s+([01])\s*([01])", r"\1 \3 \4 ", d) # shorthand elliptical arc path syntax
    d = re.sub(r"[\s\r\t\n]+", r" ", d)
    d = d.strip()

    path = Path()
    pp = PathParser(d)
    pp.reset()

    while not pp.isEnd():
        pp.nextCommand()
        command = pp.command.lower()
        if command == 'm':
            p = pp.getAsCurrentPoint()
            path.moveto(p.x, p.y)
            pp.start = pp.current
            while not pp.isCommandOrEnd():
                p = pp.getAsCurrentPoint()
                path.lineto(p.x, p.y)
        elif command == 'l':
            while not pp.isCommandOrEnd():
                p = pp.getAsCurrentPoint()
                path.lineto(p.x, p.y)
        elif command == 'h':
            while not pp.isCommandOrEnd():
                newP = Point((pp.isRelativeCommand() and pp.current.x or 0) + pp.getScalar(), pp.current.y)
                pp.current = newP
                path.lineto(pp.current.x, pp.current.y)
        elif command == 'v':
            while not pp.isCommandOrEnd():
                newP = Point(pp.current.x, (pp.isRelativeCommand() and pp.current.y or 0) + pp.getScalar())
                pp.current = newP
                path.lineto(pp.current.x, pp.current.y)
        elif command == 'c':
            while not pp.isCommandOrEnd():
                curr = pp.current
                p1 = pp.getPoint()
                cntrl = pp.getAsControlPoint()
                cp = pp.getAsCurrentPoint()
                path.curveto(p1.x, p1.y, cntrl.x, cntrl.y, cp.x, cp.y)
        elif command == 's':
            while not pp.isCommandOrEnd():
                curr = pp.current
                p1 = pp.getReflectedControlPoint()
                cntrl = pp.getAsControlPoint()
                cp = pp.getAsCurrentPoint()
                path.curveto(p1.x, p1.y, cntrl.x, cntrl.y, cp.x, cp.y)
        elif command == 'q':
            while not pp.isCommandOrEnd():
                curr = pp.current
                cntrl = pp.getAsControlPoint()
                cp = pp.getAsCurrentPoint()
                cp1x = curr.x + 2 / 3.0 * (cntrl.x - curr.x) # CP1 = QP0 + 2 / 3 *(QP1-QP0)
                cp1y = curr.y + 2 / 3.0 * (cntrl.y - curr.y) # CP1 = QP0 + 2 / 3 *(QP1-QP0)
                cp2x = cp1x + 1 / 3.0 * (cp.x - curr.x) # CP2 = CP1 + 1 / 3 *(QP2-QP0)
                cp2y = cp1y + 1 / 3.0 * (cp.y - curr.y) # CP2 = CP1 + 1 / 3 *(QP2-QP0)
                g.curveto(cp1x, cp1y, cp2x, cp2y, cp.x, cp.y)
        elif command == 't':
            while not pp.isCommandOrEnd():
                curr = pp.current
                cntrl = pp.getReflectedControlPoint()
                pp.control = cntrl
                cp = pp.getAsCurrentPoint()
                cp1x = curr.x + 2 / 3.0 * (cntrl.x - curr.x) # CP1 = QP0 + 2 / 3 *(QP1-QP0)
                cp1y = curr.y + 2 / 3.0 * (cntrl.y - curr.y) # CP1 = QP0 + 2 / 3 *(QP1-QP0)
                cp2x = cp1x + 1 / 3.0 * (cp.x - curr.x) # CP2 = CP1 + 1 / 3 *(QP2-QP0)
                cp2y = cp1y + 1 / 3.0 * (cp.y - curr.y) # CP2 = CP1 + 1 / 3 *(QP2-QP0)
                path.curveto(cp1x, cp1y, cp2x, cp2y, cp.x, cp.y)
        elif command == 'a':
            while not pp.isCommandOrEnd():
                curr = pp.current
                rx = pp.getScalar()
                ry = pp.getScalar()
                rot = pp.getScalar() # * (math.pi / 180.0)
                large = pp.getScalar()
                sweep = pp.getScalar()
                cp = pp.getAsCurrentPoint()
                ex = cp.x
                ey = cp.y
                segs = arcToSegments(ex, ey, rx, ry, large, sweep, rot, curr.x, curr.y)
                for seg in segs:
                    bez = segmentToBezier(*seg)
                    path.curveto(*bez)
        elif command == 'z':
            path.close()
            pp.current = pp.start
    return path

def path_from_string(path_string):
    s = '<?xml version="1.0"?><svg><g><path d="%s" /></g></svg>' % path_string
    path = parse(s)[0]
    return path

#--- PATH TRANSFORM ----------------------------------------------------------------------------------

def parse_transform(e, path):
    
    """ Transform the path according to a defined matrix.
    
    Attempts to extract a transform="matrix()|translate()" attribute.
    Transforms the path accordingly.
    
    """
    
    t = get_attribute(e, "transform", default="")
    
    for mode in ("matrix", "translate"):
        if t.startswith(mode):
            v = t.replace(mode, "").lstrip("(").rstrip(")")
            v = v.replace(", ", ",").replace(" ", ",")
            v = [float(x) for x in v.split(",")]
            from nodebox.graphics import Transform
            if mode == "matrix":
                t = Transform(*v)
            elif mode == "translate":
                t = Transform()            
                t.translate(*v)
            path = t.map(path)
            break

    # Transformations can also be defined as <g transform="matrix()"><path /><g>
    # instead of <g><path transform="matrix() /></g>.
    e = e.parentNode
    if e and e.tagName == "g":
        path = parse_transform(e, path)
        
    return path

#--- PATH COLOR INFORMATION --------------------------------------------------------------------------

def add_color_info(e, path):
    
    """ Expand the path with color information.
    
    Attempts to extract fill and stroke colors
    from the element and adds it to path attributes.
    
    """
    
    def _color(hex, alpha=1.0):
        if hex == "none": return None
        n = int(hex[1:],16)
        r = (n>>16)&0xff
        g = (n>>8)&0xff
        b = n&0xff
        return Color(r/255.0, g/255.0, b/255.0, alpha)

    path.fill = None
    path.stroke = None
    path.strokeWidth = 0

    # See if we can find an opacity attribute,
    # which is the color's alpha.
    alpha = get_attribute(e, "opacity", default="")
    if alpha == "":
        alpha = 1.0
    else:
        alpha = float(alpha)
    
    # Colors stored as fill="" or stroke="" attributes.
    try: path.fill = _color(get_attribute(e, "fill", default="#000000"), alpha)
    except: 
        pass
    try: path.stroke = _color(get_attribute(e, "stroke", default="none"), alpha)
    except: 
        pass
    try: path.strokeWidth = float(get_attribute(e, "stroke-width", default="1"))
    except: 
        pass
    
    # Colors stored as a CSS style attribute, for example:
    # style="fill:#ff6600;stroke:#ffe600;stroke-width:0.06742057"
    style = get_attribute(e, "style", default="").split(";")
    for s in style:
        try:
            if s.startswith("fill:"):
                path.fill = _color(s.replace("fill:", ""))
            elif s.startswith("stroke:"):
                path.stroke = _color(s.replace("stroke:", ""))
            elif s.startswith("stroke-width:"):
                path.strokeWidth = float(s.replace("stroke-width:", ""))
        except:
            pass
    
    # A path with beginning and ending coordinate
    # at the same location is considered closed.
    # Unless it contains a MOVETO somewhere in the middle.
    # path.closed = False
    # if path[0].x == path[len(path)-1].x and \
    #    path[0].y == path[len(path)-1].y: 
    #     path.closed = True
    # for i in range(1,len(path)-1):
    #     if path[i].cmd == MOVETO:
    #         path.closed = False

    return path

#-----------------------------------------------------------------------------------------------------

# 1.9.4.4
# Added absolute elliptical arc.
# transform attributes on a <g> node are processed.
# transform attributes on shapes other than path are processed.
# transform="translate()" is processed.

# 1.9.4.3
# Ignore Illustrator pattern swatches.

# 1.9.4.2
# get_attribute() returns a default value for missing XML element attributes instead of "".

# 1.9.4.1
# Added the missing parse_circle().