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
import sys
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

def compress_spaces(s):
    return re.sub("[\s\r\t\n]+", " ", s, re.MULTILINE)

def to_number_array(s):
    a = compress_spaces((s or '').replace(",", " ")).lstrip().rstrip().split(" ")
    return [float(el) for el in a]

#### NAMED COLORS ####################################################################################

named_colors = {
    'lightpink'            : [1.00, 0.71, 0.76],
    'pink'                 : [1.00, 0.75, 0.80],
    'crimson'              : [0.86, 0.08, 0.24],
    'lavenderblush'        : [1.00, 0.94, 0.96],
    'palevioletred'        : [0.86, 0.44, 0.58],
    'hotpink'              : [1.00, 0.41, 0.71],
    'deeppink'             : [1.00, 0.08, 0.58],
    'mediumvioletred'      : [0.78, 0.08, 0.52],
    'orchid'               : [0.85, 0.44, 0.84],
    'thistle'              : [0.85, 0.75, 0.85],
    'plum'                 : [0.87, 0.63, 0.87],
    'violet'               : [0.93, 0.51, 0.93],
    'fuchsia'              : [1.00, 0.00, 1.00],
    'darkmagenta'          : [0.55, 0.00, 0.55],
    'purple'               : [0.50, 0.00, 0.50],
    'mediumorchid'         : [0.73, 0.33, 0.83],
    'darkviolet'           : [0.58, 0.00, 0.83],
    'darkorchid'           : [0.60, 0.20, 0.80],
    'indigo'               : [0.29, 0.00, 0.51],
    'blueviolet'           : [0.54, 0.17, 0.89],
    'mediumpurple'         : [0.58, 0.44, 0.86],
    'mediumslateblue'      : [0.48, 0.41, 0.93],
    'slateblue'            : [0.42, 0.35, 0.80],
    'darkslateblue'        : [0.28, 0.24, 0.55],
    'ghostwhite'           : [0.97, 0.97, 1.00],
    'lavender'             : [0.90, 0.90, 0.98],
    'blue'                 : [0.00, 0.00, 1.00],
    'mediumblue'           : [0.00, 0.00, 0.80],
    'darkblue'             : [0.00, 0.00, 0.55],
    'navy'                 : [0.00, 0.00, 0.50],
    'midnightblue'         : [0.10, 0.10, 0.44],
    'royalblue'            : [0.25, 0.41, 0.88],
    'cornflowerblue'       : [0.39, 0.58, 0.93],
    'lightsteelblue'       : [0.69, 0.77, 0.87],
    'lightslategray'       : [0.47, 0.53, 0.60],
    'slategray'            : [0.44, 0.50, 0.56],
    'dodgerblue'           : [0.12, 0.56, 1.00],
    'aliceblue'            : [0.94, 0.97, 1.00],
    'steelblue'            : [0.27, 0.51, 0.71],
    'lightskyblue'         : [0.53, 0.81, 0.98],
    'skyblue'              : [0.53, 0.81, 0.92],
    'deepskyblue'          : [0.00, 0.75, 1.00],
    'lightblue'            : [0.68, 0.85, 0.90],
    'powderblue'           : [0.69, 0.88, 0.90],
    'cadetblue'            : [0.37, 0.62, 0.63],
    'darkturquoise'        : [0.00, 0.81, 0.82],
    'azure'                : [0.94, 1.00, 1.00],
    'lightcyan'            : [0.88, 1.00, 1.00],
    'paleturquoise'        : [0.69, 0.93, 0.93],
    'aqua'                 : [0.00, 1.00, 1.00],
    'darkcyan'             : [0.00, 0.55, 0.55],
    'teal'                 : [0.00, 0.50, 0.50],
    'darkslategray'        : [0.18, 0.31, 0.31],
    'mediumturquoise'      : [0.28, 0.82, 0.80],
    'lightseagreen'        : [0.13, 0.70, 0.67],
    'turquoise'            : [0.25, 0.88, 0.82],
    'aquamarine'           : [0.50, 1.00, 0.83],
    'mediumaquamarine'     : [0.40, 0.80, 0.67],
    'mediumspringgreen'    : [0.00, 0.98, 0.60],
    'mintcream'            : [0.96, 1.00, 0.98],
    'springgreen'          : [0.00, 1.00, 0.50],
    'mediumseagreen'       : [0.24, 0.70, 0.44],
    'seagreen'             : [0.18, 0.55, 0.34],
    'honeydew'             : [0.94, 1.00, 0.94],
    'darkseagreen'         : [0.56, 0.74, 0.56],
    'palegreen'            : [0.60, 0.98, 0.60],
    'lightgreen'           : [0.56, 0.93, 0.56],
    'limegreen'            : [0.20, 0.80, 0.20],
    'lime'                 : [0.00, 1.00, 0.00],
    'forestgreen'          : [0.13, 0.55, 0.13],
    'green'                : [0.00, 0.50, 0.00],
    'darkgreen'            : [0.00, 0.39, 0.00],
    'lawngreen'            : [0.49, 0.99, 0.00],
    'chartreuse'           : [0.50, 1.00, 0.00],
    'greenyellow'          : [0.68, 1.00, 0.18],
    'darkolivegreen'       : [0.33, 0.42, 0.18],
    'yellowgreen'          : [0.60, 0.80, 0.20],
    'olivedrab'            : [0.42, 0.56, 0.14],
    'ivory'                : [1.00, 1.00, 0.94],
    'beige'                : [0.96, 0.96, 0.86],
    'lightyellow'          : [1.00, 1.00, 0.88],
    'lightgoldenrodyellow' : [0.98, 0.98, 0.82],
    'yellow'               : [1.00, 1.00, 0.00],
    'olive'                : [0.50, 0.50, 0.00],
    'darkkhaki'            : [0.74, 0.72, 0.42],
    'palegoldenrod'        : [0.93, 0.91, 0.67],
    'lemonchiffon'         : [1.00, 0.98, 0.80],
    'khaki'                : [0.94, 0.90, 0.55],
    'gold'                 : [1.00, 0.84, 0.00],
    'cornsilk'             : [1.00, 0.97, 0.86],
    'goldenrod'            : [0.85, 0.65, 0.13],
    'darkgoldenrod'        : [0.72, 0.53, 0.04],
    'floralwhite'          : [1.00, 0.98, 0.94],
    'oldlace'              : [0.99, 0.96, 0.90],
    'wheat'                : [0.96, 0.87, 0.07],
    'orange'               : [1.00, 0.65, 0.00],
    'moccasin'             : [1.00, 0.89, 0.71],
    'papayawhip'           : [1.00, 0.94, 0.84],
    'blanchedalmond'       : [1.00, 0.92, 0.80],
    'navajowhite'          : [1.00, 0.87, 0.68],
    'antiquewhite'         : [0.98, 0.92, 0.84],
    'tan'                  : [0.82, 0.71, 0.55],
    'burlywood'            : [0.87, 0.72, 0.53],
    'darkorange'           : [1.00, 0.55, 0.00],
    'bisque'               : [1.00, 0.89, 0.77],
    'linen'                : [0.98, 0.94, 0.90],
    'peru'                 : [0.80, 0.52, 0.25],
    'peachpuff'            : [1.00, 0.85, 0.73],
    'sandybrown'           : [0.96, 0.64, 0.38],
    'chocolate'            : [0.82, 0.41, 0.12],
    'saddlebrown'          : [0.55, 0.27, 0.07],
    'seashell'             : [1.00, 0.96, 0.93],
    'sienna'               : [0.63, 0.32, 0.18],
    'lightsalmon'          : [1.00, 0.63, 0.48],
    'coral'                : [1.00, 0.50, 0.31],
    'orangered'            : [1.00, 0.27, 0.00],
    'darksalmon'           : [0.91, 0.59, 0.48],
    'tomato'               : [1.00, 0.39, 0.28],
    'salmon'               : [0.98, 0.50, 0.45],
    'mistyrose'            : [1.00, 0.89, 0.88],
    'lightcoral'           : [0.94, 0.50, 0.50],
    'snow'                 : [1.00, 0.98, 0.98],
    'rosybrown'            : [0.74, 0.56, 0.56],
    'indianred'            : [0.80, 0.36, 0.36],
    'red'                  : [1.00, 0.00, 0.00],
    'brown'                : [0.65, 0.16, 0.16],
    'firebrick'            : [0.70, 0.13, 0.13],
    'darkred'              : [0.55, 0.00, 0.00],
    'maroon'               : [0.50, 0.00, 0.00],
    'white'                : [1.00, 1.00, 1.00],
    'whitesmoke'           : [0.96, 0.96, 0.96],
    'gainsboro'            : [0.86, 0.86, 0.86],
    'lightgrey'            : [0.83, 0.83, 0.83],
    'silver'               : [0.75, 0.75, 0.75],
    'darkgray'             : [0.66, 0.66, 0.66],
    'gray'                 : [0.50, 0.50, 0.50],
    'grey'                 : [0.50, 0.50, 0.50],
    'dimgray'              : [0.41, 0.41, 0.41],
    'dimgrey'              : [0.41, 0.41, 0.41],
    'black'                : [0.00, 0.00, 0.00],
    'cyan'                 : [0.00, 0.68, 0.94],

    'transparent'          : [0.00, 0.00, 0.00, 0.00],
    'bark'                 : [0.25, 0.19, 0.13]
}

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

def get_svg_attributes(node, parent_attributes={}):
    if parent_attributes:
        attributes = dict(parent_attributes)
    else:
        attributes = dict()

    transform = parse_transform(node)
    if transform and not transform.equals(Transform()):
        if attributes.has_key("transform"):
            t = Transform(attributes["transform"])
            t.append(transform)
            attributes["transform"] = t
        else:
            attributes["transform"] = transform

    color_attrs = parse_color_info(node)
    attributes.update(color_attrs)

    return attributes


def apply_svg_attributes(path, attributes):
    fill = attributes.get("fill")
    if fill is None and not path.isEmpty():
        fill = "#000000"
    fill_opacity = attributes.get("fill-opacity")
    stroke = attributes.get("stroke")
    stroke_opacity = attributes.get("stroke-opacity")
    opacity = attributes.get("opacity", 1.0)
    stroke_width = attributes.get("stroke-width")
    transform = attributes.get("transform")
    color = attributes.get("color")

    def _color(s, alpha=1.0):
        if s == "none": return None
        if s[0] == "#":
            n = int(s[1:],16)
            r = (n>>16)&0xff
            g = (n>>8)&0xff
            b = n&0xff
            return Color(r/255.0, g/255.0, b/255.0, alpha)
        elif named_colors.has_key(s):
            rgb = named_colors[s]
            if len(rgb) == 3:
                return Color(rgb[0], rgb[1], rgb[2], alpha)
            elif len(rgb) == 4:
                return Color(rgb[0], rgb[1], rgb[2], rgb[3] * alpha)
            else:
                return Color(0, 0, 0, 0)

    if transform is not None:
        path = transform.map(path)

    if fill == "currentColor":
        if color is None:
            fill = "#000000"
        else:
            fill = color
    if fill is not None:
        alpha = 1.0
        if fill_opacity is not None:
            alpha *= fill_opacity
        if opacity is not None:
            alpha *= opacity
        path.fill = _color(fill, alpha)
    if stroke is not None:
        alpha = 1.0
        if fill_opacity is not None:
            alpha *= fill_opacity
        if opacity is not None:
            alpha *= opacity
        path.stroke = _color(stroke, alpha)
    if stroke_width is not None:
        path.strokeWidth = stroke_width
    return path


#--- XML NODE ----------------------------------------------------------------------------------------

def parse_node(node, paths=[], ignore=["pattern"], parent_attributes={}):
    
    """ Recurse the node tree and find drawable tags.
    
    Recures all the children in the node.
    If a child is something we can draw,
    a line, rect, oval or path,
    parse it to a PathElement drawable with drawpath()
    
    """

    # Ignore paths in Illustrator pattern swatches etc.
    if node.nodeType == node.ELEMENT_NODE and node.tagName in ignore: 
        return []

    if node.nodeType == node.ELEMENT_NODE:
        attributes = get_svg_attributes(node, parent_attributes)
    else:
        attributes = {}

    if node.hasChildNodes():
        for child in node.childNodes:
            paths = parse_node(child, paths, parent_attributes=attributes)

    if node.nodeType == node.ELEMENT_NODE:

        if node.tagName == "line":
            paths.append(parse_line(node))
        elif node.tagName == "rect":
            paths.append(parse_rect(node))
        elif node.tagName == "circle":
            paths.append(parse_circle(node))
        elif node.tagName == "ellipse":
            paths.append(parse_ellipse(node))
        elif node.tagName == "polygon":
            paths.append(parse_polygon(node))
        elif node.tagName == "polyline":
            paths.append(parse_polygon(node))
        elif node.tagName == "path":
            paths.append(parse_path(node))

        if node.tagName in ("line", "rect", "circle", "ellipse", "polygon", "polyline", "path"):
            paths[-1] = apply_svg_attributes(paths[-1], attributes)

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
    if w < 0:
        print >> sys.stderr, "Error: invalid negative value for <rect> attribute width=\"%s\"" % w
        w = 0
    if h < 0:
        print >> sys.stderr, "Error: invalid negative value for <rect> attribute height=\"%s\"" % h
        h = 0
    rx = float(get_attribute(e, "rx"))
    ry = float(get_attribute(e, "ry"))
    if rx < 0:
        print >> sys.stderr, "Error: invalid negative value for <rect> attribute rx=\"%s\"" % rx
        rx = 0
    if ry < 0:
        print >> sys.stderr, "Error: invalid negative value for <rect> attribute ry=\"%s\"" % ry
        ry = 0
    if not rx or not ry:
        rx = ry = max(rx, ry)
    if rx > w / 2.0: rx = w / 2.0
    if ry > h / 2.0: ry = h / 2.0
    p = Path()
    p.rect(x + w / 2.0, y + h / 2.0, w, h, rx, ry)
    return p

#--- CIRCLE -----------------------------------------------------------------------------------------

def parse_circle(e):
    
    cx = float(get_attribute(e, "cx"))
    cy = float(get_attribute(e, "cy"))
    r = float(get_attribute(e, "r"))
    if r < 0:
        print >> sys.stderr, "Error: invalid negative value for <circle> attribute r=\"%s\"" % r
        r = 0
    p = Path()
    p.ellipse(cx, cy, r*2, r*2)
    p.close()
    return p

#--- ELLIPSE -----------------------------------------------------------------------------------------

def parse_ellipse(e):
    
    cx = float(get_attribute(e, "cx"))
    cy = float(get_attribute(e, "cy"))
    rx = float(get_attribute(e, "rx"))
    ry = float(get_attribute(e, "ry"))
    if rx < 0:
        print >> sys.stderr, "Error: invalid negative value for <ellipse> attribute rx=\"%s\"" % rx
        rx = 0
    if ry < 0:
        print >> sys.stderr, "Error: invalid negative value for <ellipse> attribute ry=\"%s\"" % ry
        ry = 0
    p = Path()
    p.ellipse(cx, cy, rx * 2, ry * 2)
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
    t = (8.0 / 3) * sin(th_half * 0.5) * sin(th_half * 0.5) / sin(th_half)
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

def parse_transform(e):
    
    """ Attempts to extract a transform="matrix()|translate()|scale()|rotate()" attribute.
    """

    from nodebox.graphics import Transform

    def translate(s):
        a = to_number_array(s)
        tx = a[0]
        ty = 0
        if len(a) > 1:
            ty = a[1]
        return Transform.translated(tx, ty)

    def scale(s):
        a = to_number_array(s)
        sx = a[0]
        sy = sx
        if len(a) > 1:
            sy = a[1]
        return Transform.scaled(sx, sy)

    def rotate(s):
        a = to_number_array(s)
        r = a[0]
        tx = 0
        ty = 0
        if len(a) > 1:
            tx = a[1]
        if len(a) > 2:
            ty = a[2]
        t = Transform()
        t.translate(tx, ty)
        t.rotate(r)
        t.translate(-tx, -ty)
        return t

    def matrix(s):
        m = to_number_array(s)
        return Transform(*m)

    types = {
        "translate": translate,
        "scale": scale,
        "rotate": rotate,
        "matrix": matrix
    }


    transforms = []
    t = get_attribute(e, "transform", default="")
    a = get_attribute(e, "id", None)
    if t:
        v = compress_spaces(t).lstrip().rstrip()
        v = re.split("\s(?=[a-z])", v)
        for el in v:
            type = el.split("(")[0].lstrip().rstrip()
            s = el.split("(")[1].replace(")", "")
            transform = types[type](s)
            transforms.append(transform)

    transform = Transform()
    if transforms:
        for t in transforms:
            transform.append(t)

    return transform


#--- PATH COLOR INFORMATION --------------------------------------------------------------------------

def parse_color_info(e):
    
    """ Attempts to extract fill and stroke colors
    """
    
    fill_opacity = get_attribute(e, "fill-opacity", None)
    if fill_opacity is not None:
        fill_opacity = float(fill_opacity)
    
    stroke_opacity = get_attribute(e, "stroke-opacity", None)
    if stroke_opacity is not None:
        stroke_opacity = float(stroke_opacity)

    opacity = get_attribute(e, "opacity", None)
    if opacity is not None:
        opacity = float(opacity)

    # Colors stored as fill="" or stroke="" attributes.
    fill = get_attribute(e, "fill", None)
    stroke = get_attribute(e, "stroke", None)
    color = get_attribute(e, "color", None)
    stroke_width = get_attribute(e, "stroke-width", None)
    if stroke_width is not None:
        stroke_width = float(stroke_width)

    # Colors stored as a CSS style attribute, for example:
    # style="fill:#ff6600;stroke:#ffe600;stroke-width:0.06742057"
    style = get_attribute(e, "style", default="").split(";")
    for s in style:
        try:
            el = s.split(":")
            if len(el) == 2:
                attr, value = el
                attr = attr.lstrip().rstrip()
                value = value.lstrip().rstrip()
                if attr == "fill":
                    fill = value
                elif attr == "stroke":
                    stroke = value
                elif attr == "color":
                    color = value
                elif attr == "stroke-width":
                    stroke_width = float(value)
                elif attr == "fill-opacity":
                    fill_opacity = float(value)
                elif attr == "stroke-opacity":
                    stroke_opacity = float(value)
                elif attr == "opacity":
                    opacity = float(value)
        except:
            pass

    attributes = dict()

    def _add_attr(attributes, attr, value):
        if value is not None:
            attributes[attr] = value

    _add_attr(attributes, "fill", fill)
    _add_attr(attributes, "stroke", stroke)
    _add_attr(attributes, "stroke-width", stroke_width)
    _add_attr(attributes, "fill-opacity", fill_opacity)
    _add_attr(attributes, "stroke-opacity", stroke_opacity)
    _add_attr(attributes, "opacity", opacity)
    _add_attr(attributes, "color", color)

    return attributes

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