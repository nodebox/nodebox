# Generates nodebox3_random.txt: what NodeBox 3 (Jython 2.7.2) produces for
# scatter and wiggle. Run with the Jython that NodeBox 3 shipped:
#
#   java -jar jython-standalone-2.7.2.jar nodebox3_random.py > nodebox3_random.txt
#
# Mirrors pyvector.scatter / wiggle_* from NodeBox 3 on plain data, with
# java.awt.geom.GeneralPath for contains() and NodeBox's Bezier.extrema for bounds.
import random, struct, binascii, math, jarray
from org.python.core import Py
from java.lang import Long
from java.awt.geom import GeneralPath, Ellipse2D, AffineTransform, PathIterator

def h(x): return binascii.hexlify(struct.pack('>d', x))
def seed(s): random.seed(Py.java2py(Long.valueOf(s)))

# A contour: list of (x, y, type) with type 'L', 'D' (curve data), 'C' (curve to); closed flag.
STAR = ([(0.0, -50.0, 'L'), (11.8, -16.2, 'L'), (47.6, -15.5, 'L'), (19.1, 6.2, 'L'), (29.4, 40.5, 'L'),
         (0.0, 20.0, 'L'), (-29.4, 40.5, 'L'), (-19.1, 6.2, 'L'), (-47.6, -15.5, 'L'), (-11.8, -16.2, 'L')], True)
BLOB = ([(0.0, 0.0, 'L'), (40.0, -30.0, 'D'), (90.0, 10.0, 'D'), (80.0, 50.0, 'C'),
         (70.0, 90.0, 'D'), (10.0, 80.0, 'D'), (-10.0, 40.0, 'C')], True)
HOLE = ([(20.0, 20.0, 'L'), (50.0, 20.0, 'L'), (50.0, 40.0, 'L'), (20.0, 40.0, 'L')], True)

def ellipse(cx, cy, w, h):
    # Path.ellipse in NodeBox 3: the segments of java.awt.geom.Ellipse2D.
    it = Ellipse2D.Double(cx - w / 2, cy - h / 2, w, h).getPathIterator(AffineTransform())
    pts = []
    while not it.isDone():
        c = jarray.zeros(6, 'd')
        kind = it.currentSegment(c)
        if kind == PathIterator.SEG_MOVETO: pts.append((c[0], c[1], 'L'))
        elif kind == PathIterator.SEG_CUBICTO: pts.extend([(c[0], c[1], 'D'), (c[2], c[3], 'D'), (c[4], c[5], 'C')])
        it.next()
    return (pts, True)
ELLIPSE = ellipse(5.0, -3.0, 80.0, 50.0)

def general_path(contours):
    gp = GeneralPath(GeneralPath.WIND_NON_ZERO)
    for pts, closed in contours:
        i = 0
        gp.moveTo(pts[0][0], pts[0][1]); i = 1
        while i < len(pts):
            if pts[i][2] == 'D':
                gp.curveTo(pts[i][0], pts[i][1], pts[i+1][0], pts[i+1][1], pts[i+2][0], pts[i+2][1]); i += 3
            else:
                gp.lineTo(pts[i][0], pts[i][1]); i += 1
        if closed: gp.closePath()
    return gp

def fuzzy(p1, p2): return abs(p1 - p2) <= (0.000000000001 * min(abs(p1), abs(p2)))
def extrema(x1, y1, x2, y2, x3, y3, x4, y4):
    st = {}
    st['minx'], st['maxx'] = (x1, x4) if x1 < x4 else (x4, x1)
    st['miny'], st['maxy'] = (y1, y4) if y1 < y4 else (y4, y1)
    def point_at(t):
        m_t = 1 - t; b = m_t * m_t; c = t * t; d = c * t; a = b * m_t; b *= (3. * t); c *= (3. * m_t)
        return a * x1 + b * x2 + c * x3 + d * x4, a * y1 + b * y2 + c * y3 + d * y4
    def check(t):
        if t >= 0 and t <= 1:
            px, py = point_at(t)
            if px < st['minx']: st['minx'] = px
            elif px > st['maxx']: st['maxx'] = px
            if py < st['miny']: st['miny'] = py
            elif py > st['maxy']: st['maxy'] = py
    ax = 3 * (-x1 + 3 * x2 - 3 * x3 + x4); bx = 6 * (x1 - 2 * x2 + x3); cx = 3 * (-x1 + x2)
    if fuzzy(ax + 1, 1):
        if not fuzzy(bx + 1, 1): check(-cx / bx)
    else:
        tx = bx * bx - 4 * ax * cx
        if tx >= 0:
            temp = math.sqrt(tx); rcp = 1 / (2 * ax); check((-bx + temp) * rcp); check((-bx - temp) * rcp)
    ay = 3 * (-y1 + 3 * y2 - 3 * y3 + y4); by = 6 * (y1 - 2 * y2 + y3); cy = 3 * (-y1 + y2)
    if fuzzy(ay + 1, 1):
        if not fuzzy(by + 1, 1): check(-cy / by)
    else:
        ty = by * by - 4 * ay * cy
        if ty > 0:
            temp = math.sqrt(ty); rcp = 1 / (2 * ay); check((-by + temp) * rcp); check((-by - temp) * rcp)
    return st['minx'], st['miny'], st['maxx'], st['maxy']

def bounds(contours):
    minx = miny = 1.7976931348623157e308; maxx = maxy = -1.7976931348623157e308
    for pts, closed in contours:
        for i, (x, y, t) in enumerate(pts):
            if t == 'L': r = (x, y, x, y)
            elif t == 'C': r = extrema(pts[i-3][0], pts[i-3][1], pts[i-2][0], pts[i-2][1], pts[i-1][0], pts[i-1][1], x, y)
            else: continue
            minx = min(minx, r[0]); miny = min(miny, r[1]); maxx = max(maxx, r[2]); maxy = max(maxy, r[3])
    return minx, miny, maxx - minx, maxy - miny

def scatter(contours, amount, s):
    seed(s)
    gp = general_path(contours)
    bx, by, bw, bh = bounds(contours)
    points = []
    for i in xrange(amount):
        tries = 100
        while tries > 0:
            x = bx + random.uniform(0, 1) * bw; y = by + random.uniform(0, 1) * bh
            if gp.contains(x, y):
                points.append((x, y)); break
            tries -= 1
    return points

def wiggle(contours, scope, ox, oy, s):
    seed(s)
    out = []
    if scope == 'points':
        for pts, closed in contours:
            for (x, y, t) in pts:
                dx = (random.uniform(0, 1) - 0.5) * ox * 2; dy = (random.uniform(0, 1) - 0.5) * oy * 2
                out.append((x + dx, y + dy))
    elif scope == 'contours':
        for pts, closed in contours:
            dx = (random.uniform(0, 1) - 0.5) * ox * 2; dy = (random.uniform(0, 1) - 0.5) * oy * 2
            out.extend([(x + dx, y + dy) for (x, y, t) in pts])
    else:
        dx = (random.uniform(0, 1) - 0.5) * ox * 2; dy = (random.uniform(0, 1) - 0.5) * oy * 2
        for pts, closed in contours: out.extend([(x + dx, y + dy) for (x, y, t) in pts])
    return out

def dump(name, pts):
    print name, len(pts), ' '.join(h(x) + ',' + h(y) for x, y in pts)

print 'bounds blob', ' '.join(h(v) for v in bounds([BLOB]))
for s in (0, 42, -3):
    dump('scatter star %d' % s, scatter([STAR], 25, s))
    dump('scatter blob+hole %d' % s, scatter([BLOB, HOLE], 25, s))
    dump('scatter ellipse %d' % s, scatter([ELLIPSE], 25, s))
    for scope in ('points', 'contours', 'paths'):
        dump('wiggle %s %d' % (scope, s), wiggle([BLOB, HOLE], scope, 10.0, 5.0, s))
