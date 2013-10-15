/*---  Based on: canvas.js, https://github.com/clips/pattern/blob/master/pattern/canvas.js (BSD)
  ---  De Smedt T. & Daelemans W. (2012). Pattern for Python. Journal of Machine Learning Research. --*/

var g = {};

/*--- GEOMETRY -------------------------------------------------------------------------------------*/

g.math = {};

g.math.round = function (x, decimals) {
    return (!decimals) ?
            Math.round(x) :
            Math.round(x * Math.pow(10, decimals)) / Math.pow(10, decimals);
};

g.math.sign = function (x) {
    if (x < 0) { return -1; }
    if (x > 0) { return +1; }
    return 0;
};

g.math.degrees = function (radians) {
    return radians * 180 / Math.PI;
};

g.math.radians = function (degrees) {
    return degrees / 180 * Math.PI;
};

g.math.clamp = function (value, min, max) {
    return (max > min) ?
            Math.max(min, Math.min(value, max)) :
            Math.max(max, Math.min(value, min));
};

g.math.dot = function (a, b) {
    var m = Math.min(a.length, b.length),
        n = 0,
        i;
    for (i = 0; i < m; i += 1) {
        n += a[i] * b[i];
    }
    return n;
};

g.math.mix = function (a, b, t) {
    if (t < 0.0) { return a; }
    if (t > 1.0) { return b; }
    return a + (b - a) * t;
};

g.geometry = {};

g.geometry.angle = function (x0, y0, x1, y1) {
    /* Returns the angle between two points.
     */
    return g.math.degrees(Math.atan2(y1 - y0, x1 - x0));
};

g.geometry.distance = function (x0, y0, x1, y1) {
    /* Returns the distance between two points.
     */
    return Math.sqrt(Math.pow(x1 - x0, 2) + Math.pow(y1 - y0, 2));
};

g.geometry.coordinates = function (x0, y0, distance, angle) {
    /* Returns the location of a point by rotating around origin (x0,y0).
     */
    var x1 = x0 + Math.cos(g.math.radians(angle)) * distance,
        y1 = y0 + Math.sin(g.math.radians(angle)) * distance;
    return Object.freeze({ x: x1, y: y1 });
};

g.geometry.pointInPolygon = function (points, x, y) {
    /* Ray casting algorithm.
     * Determines how many times a horizontal ray starting from the point
     * intersects with the sides of the polygon.
     * If it is an even number of times, the point is outside, if odd, inside.
     * The algorithm does not always report correctly when the point is very close to the boundary.
     * The polygon is passed as an array of Points.
     */
    // Based on: W. Randolph Franklin, 1970, http://www.ecse.rpi.edu/Homepages/wrf/Research/Short_Notes/pnpoly.html
    var i, j, x0, y0, x1, y1,
        odd = false,
        n = points.length;

    for (i = 0; i < n; i += 1) {
        j = (i < n - 1) ? i + 1 : 0;
        x0 = points[i].x;
        y0 = points[i].y;
        x1 = points[j].x;
        y1 = points[j].y;
        if ((y0 < y && y1 >= y) || (y1 < y && y0 >= y)) {
            if (x0 + (y - y0) / (y1 - y0) * (x1 - x0) < x) {
                odd = !odd;
            }
        }
    }
    return odd;
};


/*--- BEZIER MATH ----------------------------------------------------------------------------------*/
// Thanks to Prof. F. De Smedt at the Vrije Universiteit Brussel, 2006.

g.bezier = {};

// BEZIER MATH:

g.bezier.sum = function (values) {
    return _.reduce(values, function (a, b) { return a + b; }); // sum
};

g.bezier.linePoint = function (t, x0, y0, x1, y1) {
    /* Returns coordinates for the point at t (0.0-1.0) on the line.
     */
    var x = x0 + t * (x1 - x0),
        y = y0 + t * (y1 - y0);
    return g.lineTo(x, y);
};

g.bezier.lineLength = function (x0, y0, x1, y1) {
    /* Returns the length of the line.
     */
    var a = Math.pow(Math.abs(x0 - x1), 2),
        b = Math.pow(Math.abs(y0 - y1), 2);
    return Math.sqrt(a + b);
};

g.bezier.curvePoint = function (t, x0, y0, x1, y1, x2, y2, x3, y3, handles) {
    /* Returns coordinates for the point at t (0.0-1.0) on the curve
     * (de Casteljau interpolation algorithm).
     */
    var dt = 1 - t,
        x01 = x0 * dt + x1 * t,
        y01 = y0 * dt + y1 * t,
        x12 = x1 * dt + x2 * t,
        y12 = y1 * dt + y2 * t,
        x23 = x2 * dt + x3 * t,
        y23 = y2 * dt + y3 * t,

        h1x = x01 * dt + x12 * t,
        h1y = y01 * dt + y12 * t,
        h2x = x12 * dt + x23 * t,
        h2y = y12 * dt + y23 * t,
        x = h1x * dt + h2x * t,
        y = h1y * dt + h2y * t;

    return g.curveto(h1x, h1y, h2x, h2y, x, y);
/*    if (!handles) {
        return {x, y, h1x, h1y, h2x, h2y];
    } else {
        // Include the new handles of pt0 and pt3 (see g.bezier.insert_point()).
        return [x, y, h1x, h1y, h2x, h2y, x01, y01, x23, y23];
    }*/
};

g.bezier.curveLength = function (x0, y0, x1, y1, x2, y2, x3, y3, n) {
    /* Returns the length of the curve.
     * Integrates the estimated length of the cubic bezier spline defined by x0, y0, ... x3, y3,
     * by adding up the length of n linear lines along the curve.
     */
    if (n === undefined) { n = 20; }
    var i, t, pe,
        length = 0,
        xi = x0,
        yi = y0;
    for (i = 0; i < n; i += 1) {
        t = (i + 1) / n;
        pe = g.bezier.curvePoint(t, x0, y0, x1, y1, x2, y2, x3, y3);
        length += Math.sqrt(
            Math.pow(Math.abs(xi - pe.point.x), 2) +
                Math.pow(Math.abs(yi - pe.point.y), 2)
        );
        xi = pe.point.x;
        yi = pe.point.y;
    }
    return length;
};

// BEZIER PATH LENGTH:

g.bezier.segmentLengths = function (pathElements, relative, n) {
    /* Returns an array with the length of each segment in the path.
     * With relative=true, the total length of all segments is 1.0.
     */
    if (n === undefined) { n = 20; }
    var i, el, cmd, pt, close_x, close_y, x0, y0, s, lengths;
    lengths = [];
    for (i = 0; i < pathElements.length; i += 1) {
        el = pathElements[i];
        cmd = el.cmd;
        pt = el.point;

        if (i === 0) {
            close_x = pt.x;
            close_y = pt.y;
        } else if (cmd === g.MOVETO) {
            close_x = pt.x;
            close_y = pt.y;
            lengths.push(0.0);
        } else if (cmd === g.CLOSE) {
            lengths.push(g.bezier.lineLength(x0, y0, close_x, close_y));
        } else if (cmd === g.LINETO) {
            lengths.push(g.bezier.lineLength(x0, y0, pt.x, pt.y));
        } else if (cmd === g.CURVETO) {
            lengths.push(g.bezier.curveLength(x0, y0, el.ctrl1.x, el.ctrl1.y, el.ctrl2.x, el.ctrl2.y, pt.x, pt.y, n));
        }
        if (cmd !== g.CLOSE) {
            x0 = pt.x;
            y0 = pt.y;
        }
    }
    if (relative === true) {
        s = g.bezier.sum(lengths); // sum
        return (s > 0) ?
                _.map(lengths, function (v) { return v / s; }) :
                _.map(lengths, function () { return 0.0; });
    }
    return lengths;
};


g.bezier.length = function (path, segmented, n) {
    /* Returns the approximate length of the path.
     * Calculates the length of each curve in the path using n linear samples.
     * With segmented=true, returns an array with the relative length of each segment (sum=1.0).
     */
    if (n === undefined) { n = 20; }
    return (!segmented) ?
            g.bezier.sum(g.bezier.segmentLengths(path.elements, false, n)) :
            g.bezier.segmentLengths(path.elements, true, n);
};


// BEZIER PATH POINT:

g.bezier._locate = function (path, t, segments) {
    /* For a given relative t on the path (0.0-1.0), returns an array [index, t, PathElement],
     * with the index of the PathElement before t,
     * the absolute time on this segment,
     * the last MOVETO or any subsequent CLOSETO after i.
    */
    // Note: during iteration, supplying segmentLengths() yourself is 30x faster.
    var i, el, closeto;
    if (segments === undefined) {
        segments = g.bezier.segmentLengths(path.elements, true);
    }
    for (i = 0; i < path.elements.length; i += 1) {
        el = path.elements[i];
        if (i === 0 || el.cmd === g.MOVETO) {
            closeto = g.makePoint(el.point.x, el.point.y);
        }
        if (t <= segments[i] || i === segments.length - 1) {
            break;
        }
        t -= segments[i];
    }
    if (segments[i] !== 0) { t /= segments[i]; }
    if (i === segments.length - 1 && segments[i] === 0) { i -= 1; }
    return [i, t, closeto];
};

g.bezier.point = function (path, t, segments) {
    /* Returns the DynamicPathElement at time t on the path.
     * Note: in PathElement, ctrl1 is how the curve started, and ctrl2 how it arrives in this point.
     * Here, ctrl1 is how the curve arrives, and ctrl2 how it continues to the next point.
     */
    var loc, i, closeto, x0, y0, pe;
    loc = g.bezier._locate(path, t, segments);
    i = loc[0];
    t = loc[1];
    closeto = loc[2];
    x0 = path.elements[i].point.x;
    y0 = path.elements[i].point.y;
    pe = path.elements[i + 1];
    if (pe.cmd === g.LINETO || pe.cmd === g.CLOSE) {
        pe = (pe.cmd === g.CLOSE) ?
                 g.bezier.linePoint(t, x0, y0, closeto.x, closeto.y) :
                 g.bezier.linePoint(t, x0, y0, pe.point.x, pe.point.y);
    } else if (pe.cmd === g.CURVETO) {
        pe = g.bezier.curvePoint(t, x0, y0, pe.ctrl1.x, pe.ctrl1.y, pe.ctrl2.x, pe.ctrl2.y, pe.point.x, pe.point.y);
    }
    return pe;
};

g.bezier.extrema = function (p1, p2, p3, p4) {
    var minx, maxx, miny, maxy,
        ax, bx, cx, ay, by, cy,
        temp, rcp, tx, ty,

        x1 = p1.x,
        y1 = p1.y,
        x2 = p2.x,
        y2 = p2.y,
        x3 = p3.x,
        y3 = p3.y,
        x4 = p4.x,
        y4 = p4.y;

    function fuzzyCompare(p1, p2) {
        return Math.abs(p1 - p2) <= (0.000000000001 * Math.min(Math.abs(p1), Math.abs(p2)));
    }

    function coefficients(t) {
        var m_t, a, b, c, d;
        m_t = 1 - t;
        b = m_t * m_t;
        c = t * t;
        d = c * t;
        a = b * m_t;
        b *= (3.0 * t);
        c *= (3.0 * m_t);
        return [a, b, c, d];
    }

    function pointAt(t) {
        var a, b, c, d, coeff;
        coeff = coefficients(t);
        a = coeff[0];
        b = coeff[1];
        c = coeff[2];
        d = coeff[3];
        return {x: a * x1 + b * x2 + c * x3 + d * x4,
                y: a * y1 + b * y2 + c * y3 + d * y4};
    }

    function bezierCheck(t) {
        if (t >= 0 && t <= 1) {
            var p = pointAt(t);
            if (p.x < minx) {
                minx = p.x;
            } else if (p.x > maxx) {
                maxx = p.x;
            }
            if (p.y < miny) {
                miny = p.y;
            } else if (p.y > maxy) {
                maxy = p.y;
            }
        }
    }

    if (x1 < x4) {
        minx = x1;
        maxx = x4;
    } else {
        minx = x4;
        maxx = x1;
    }
    if (y1 < y4) {
        miny = y1;
        maxy = y4;
    } else {
        miny = y4;
        maxy = y1;
    }

    ax = 3 * (-x1 + 3 * x2 - 3 * x3 + x4);
    bx = 6 * (x1 - 2 * x2 + x3);
    cx = 3 * (-x1 + x2);

    if (fuzzyCompare(ax + 1, 1)) {
        if (!fuzzyCompare(bx + 1, 1)) {
            bezierCheck(-cx / bx);
        }
    } else {
        tx = bx * bx - 4 * ax * cx;
        if (tx >= 0) {
            temp = Math.sqrt(tx);
            rcp = 1 / (2 * ax);
            bezierCheck((-bx + temp) * rcp);
            bezierCheck((-bx - temp) * rcp);
        }
    }

    ay = 3 * (-y1 + 3 * y2 - 3 * y3 + y4);
    by = 6 * (y1 - 2 * y2 + y3);
    cy = 3 * (-y1 + y2);

    if (fuzzyCompare(ay + 1, 1)) {
        if (!fuzzyCompare(by + 1, 1)) {
            bezierCheck(-cy / by);
        }
    } else {
        ty = by * by - 4 * ay * cy;
        if (ty > 0) {
            temp = Math.sqrt(ty);
            rcp = 1 / (2 * ay);
            bezierCheck((-by + temp) * rcp);
            bezierCheck((-by - temp) * rcp);
        }
    }

    return g.makeRect(minx, miny, maxx - minx, maxy - miny);
};


g.makePoint = function (x, y) {
    return Object.freeze({ x: x, y: y });
};

g.MOVETO  = "M";
g.LINETO  = "L";
g.CURVETO = "C";
g.CLOSE   = "z";

g.ZERO = g.makePoint(0, 0);
g.CLOSE_ELEMENT = { cmd: g.CLOSE };

g.moveTo = g.moveto = function (x, y) {
    return Object.freeze({ cmd:   g.MOVETO,
                         point: g.makePoint(x, y) });
};

g.lineTo = g.lineto = function (x, y) {
    return Object.freeze({ cmd:   g.LINETO,
                         point: g.makePoint(x, y) });
};

g.curveTo = g.curveto = function (c1x, c1y, c2x, c2y, x, y) {
    return Object.freeze({ cmd:   g.CURVETO,
                         point: g.makePoint(x, y),
                         ctrl1: g.makePoint(c1x, c1y),
                         ctrl2: g.makePoint(c2x, c2y) });
};

g.closePath = g.closepath = function () {
    return g.CLOSE_ELEMENT;
};

g.rect = function (x, y, width, height) {
    return Object.freeze({ elements: [
        g.moveto(x, y),
        g.lineto(x + width, y),
        g.lineto(x + width, y + height),
        g.lineto(x, y + height),
        g.closepath()
    ] });
};

g.ellipse = function (x, y, width, height) {
    var k = 0.55, // kappa = (-1 + sqrt(2)) / 3 * 4
        dx = k * 0.5 * width,
        dy = k * 0.5 * height,
        x0 = x + 0.5 * width,
        y0 = y + 0.5 * height,
        x1 = x + width,
        y1 = y + height;
    return Object.freeze({ elements: [
        g.moveto(x, y0),
        g.curveto(x, y0 - dy, x0 - dx, y, x0, y),
        g.curveto(x0 + dx, y, x1, y0 - dy, x1, y0),
        g.curveto(x1, y0 + dy, x0 + dx, y1, x0, y1),
        g.curveto(x0 - dx, y1, x, y0 + dy, x, y0),
        g.closepath()
    ] });
};

g.line = function (x0, y0, x1, y1) {
    return Object.freeze({ elements: [
        g.moveto(x0, y0),
        g.lineto(x1, y1)
    ] });
};

g.quad = function (x1, y1, x2, y2, x3, y3, x4, y4) {
    return Object.freeze({ elements: [
        g.moveto(x1, y1),
        g.lineto(x2, y2),
        g.lineto(x3, y3),
        g.lineto(x4, y4),
        g.closepath()
    ] });
};

g.makePath = function (pe, fill, stroke, strokeWidth) {
    var elements = pe.elements || pe;
    return Object.freeze({
        elements: elements,
        fill: fill,
        stroke: stroke,
        strokeWidth: strokeWidth
    });
};

g.getContours = function (path) {
    var contours = [],
        currentContour = [];
    _.each(path.elements, function (el) {
        if (el.cmd === g.MOVETO) {
            if (!_.isEmpty(currentContour)) {
                contours.push(currentContour);
            }
            currentContour = [el];
        } else {
            currentContour.push(el);
        }
    });

    if (!_.isEmpty(currentContour)) {
        contours.push(currentContour);
    }

    return Object.freeze(contours);
};

g.point = function (path, t, segments) {
    /* Returns the DynamicPathElement at time t (0.0-1.0) on the path.
     */
    if (segments === undefined) {
        // Cache the segment lengths for performace.
        segments = g.bezier.length(path, true, 10);
    }
    return g.bezier.point(path, t, segments);
};

g.points = function (path, amount, options) {
    /* Returns an array of DynamicPathElements along the path.
     * To omit the last point on closed paths: {end: 1-1.0/amount}
     */
    var d, a, i, segments,
        start = (options && options.start !== undefined) ? options.start : 0.0,
        end = (options && options.end !== undefined) ? options.end : 1.0;
    if (path.elements.length === 0) {
        // Otherwise g.bezier.point() will raise an error for empty paths.
        return [];
    }
    amount = Math.round(amount);
    // The delta value is divided by amount-1, because we also want the last point (t=1.0)
    // If we don't use amount-1, we fall one point short of the end.
    // If amount=4, we want the point at t 0.0, 0.33, 0.66 and 1.0.
    // If amount=2, we want the point at t 0.0 and 1.0.
    d = (amount > 1) ? (end - start) / (amount - 1) : (end - start);
    a = [];
    segments = g.bezier.length(path, true, 10);

    for (i = 0; i < amount; i += 1) {
        a.push(g.point(path, start + d * i, segments));
    }
    return a;
};

g.length = function (path, precision) {
    /* Returns an approximation of the total length of the path.
     */
    if (precision === undefined) { precision = 10; }
    return g.bezier.length(path, false, precision);
};

g.pathContains = function (path, x, y, precision) {
    /* Returns true when point (x,y) falls within the contours of the path.
     */
    if (precision === undefined) { precision = 100; }
    var i, polygon = g.points(path, precision),
        points = [];
    for (i = 0; i < polygon.length; i += 1) {
        if (polygon[i].cmd !== g.CLOSE) {
            points.push(polygon[i].point);
        }
    }
//    if (this._polygon == null ||
//        this._polygon[1] != precision) {
//        this._polygon = [this.points(precision), precision];
//    }
    return g.geometry.pointInPolygon(points, x, y);
};

g.bounds = function (path) {
    if (_.isEmpty(path.elements)) { return g.makeRect(0, 0, 0, 0); }

    var px, py, prev, right, bottom,
        minX = Number.MAX_VALUE,
        minY = Number.MAX_VALUE,
        maxX = -(Number.MAX_VALUE),
        maxY = -(Number.MAX_VALUE);

    _.each(path.elements, function (el) {
        if (el.cmd === g.MOVETO || el.cmd === g.LINETO) {
            px = el.point.x;
            py = el.point.y;
            if (px < minX) { minX = px; }
            if (py < minY) { minY = py; }
            if (px > maxX) { maxX = px; }
            if (py > maxY) { maxY = py; }
            prev = el;
        } else if (el.cmd === g.CURVETO) {
            var r = g.bezier.extrema(prev.point, el.ctrl1, el.ctrl2, el.point);
            right = r.x + r.width;
            bottom = r.y + r.height;
            if (r.x < minX) { minX = r.x; }
            if (right > maxX) { maxX = right; }
            if (r.y < minY) { minY = r.y; }
            if (bottom > maxY) { maxY = bottom; }
            prev = el;
        }
    });

    return g.makeRect(minX, minY, maxX - minX, maxY - minY);
};

g.makeRect = function (x, y, width, height) {
    return Object.freeze({ x: x, y: y, width: width, height: height });
};

g.rectContains = function (rect, x, y) {
    return (x >= rect.x && x <= rect.x + rect.width && y >= rect.y && y <= rect.y + rect.height);
};

g.makeCenteredRect = function (cx, cy, width, height) {
    var x = cx - width / 2,
        y = cy - height / 2;
    return g.makeRect(x, y, width, height);
};

g.normalizedRect = function (rect) {
    var x = rect.x,
        y = rect.y,
        width = rect.width,
        height = rect.height;

    if (width < 0) {
        x += width;
        width = -width;
    }

    if (height < 0) {
        y += height;
        height = -height;
    }
    return g.makeRect(x, y, width, height);
};

g.getRectCentroid = function (rect) {
    return g.makePoint(rect.x + rect.width / 2,  rect.y + rect.height / 2);
};

g.IDENTITY = [1, 0, 0, 0, 1, 0, 0, 0, 1];

g._mmult = function (a, b) {
    /* Returns the 3x3 matrix multiplication of A and B.
     * Note that scale(), translate(), rotate() work with premultiplication,
     * e.g. the matrix A followed by B = BA and not AB.
     */
    return Object.freeze([
        a[0] * b[0] + a[1] * b[3],
        a[0] * b[1] + a[1] * b[4], 0,
        a[3] * b[0] + a[4] * b[3],
        a[3] * b[1] + a[4] * b[4], 0,
        a[6] * b[0] + a[7] * b[3] + b[6],
        a[6] * b[1] + a[7] * b[4] + b[7], 1
    ]);
};

g.prepend = function (matrix1, matrix2) {
    return g._mmult(matrix1, matrix2);
};

g.append = function (matrix1, matrix2) {
    return g._mmult(matrix2, matrix1);
};

g.inverse = function (m) {
    var d = m[0] * m[4] - m[1] * m[3];
    return Object.freeze([
        m[4] / d,
        -m[1] / d, 0,
        -m[3] / d,
        m[0] / d, 0,
        (m[3] * m[7] - m[4] * m[6]) / d,
        -(m[0] * m[7] - m[1] * m[6]) / d, 1
    ]);
};

g.scale = function (matrix, x, y) {
    if (y === undefined) { y = x; }
    return g._mmult([x, 0, 0, 0, y, 0, 0, 0, 1], matrix);
};

g.translate = function (matrix, x, y) {
    return g._mmult([1, 0, 0, 0, 1, 0, x, y, 1], matrix);
};

g.rotate = function (matrix, angle) {
    var c = Math.cos(g.math.radians(angle)),
        s = Math.sin(g.math.radians(angle));
    return g._mmult([c, s, 0, -s, c, 0, 0, 0, 1], matrix);
};

g.skew = function (matrix, x, y) {
    var kx = Math.PI * x / 180.0,
        ky = Math.PI * y / 180.0;
    return g._mmult([1, Math.tan(ky), 0, -Math.tan(kx), 1, 0, 0, 0, 1], matrix);
};

g.transformPoint = function (point, matrix) {
    /* Returns the new coordinates of the given point (x,y) after transformation.
     */
    var x = point.x,
        y = point.y;
    return g.makePoint(
        x * matrix[0] + y * matrix[3] + matrix[6],
        x * matrix[1] + y * matrix[4] + matrix[7]
    );
};

g.transformPath = function (path, matrix) {
    var elements = _.map(path.elements, function (pe) {
        if (pe.cmd === g.CLOSE) { return pe; }
        if (pe.cmd === g.MOVETO) {
            return Object.freeze({ cmd: g.MOVETO,
                                 point: g.transformPoint(pe.point, matrix) });
        }
        if (pe.cmd === g.LINETO) {
            return Object.freeze({ cmd: g.LINETO,
                                 point: g.transformPoint(pe.point, matrix) });
        }
        if (pe.cmd === g.CURVETO) {
            return Object.freeze({ cmd: g.CURVETO,
                                 point: g.transformPoint(pe.point, matrix),
                                 ctrl1: g.transformPoint(pe.ctrl1, matrix),
                                 ctrl2: g.transformPoint(pe.ctrl2, matrix) });
        }
    });
    return g.makePath(elements, path.fill, path.stroke, path.strokeWidth);
};

g.drawCommand = function (ctx, command) {
    var cmd = command.cmd;
    if (cmd === 'M') {
        ctx.moveTo(command.point.x, command.point.y);
    } else if (cmd === 'L') {
        ctx.lineTo(command.point.x, command.point.y);
    } else if (cmd === 'C') {
        ctx.bezierCurveTo(command.ctrl1.x, command.ctrl1.y, command.ctrl2.x, command.ctrl2.y, command.point.x, command.point.y);
    } else if (cmd === 'z') {
        ctx.closePath();
    } else {
        console.log('Unknown command ', command);
    }
};

g.getColor = function (c) {
    var R, G, B, A;
    if (c !== null) {
        if (_.isString(c)) {
            return c;
        }
        R = g.math.round(c.r * 255);
        G = g.math.round(c.g * 255);
        B = g.math.round(c.b * 255);
        A = c.a;
    } else {
        R = G = B = 0;
        A = 1;
    }
    return "rgba(" + R + ", " + G + ", " + B + ", " + A + ")";
};

g.draw = function (ctx, shape) {
    try {
        if (_.isArray(shape)) {
            _.each(shape, _.partial(g.draw, ctx));
        } else {
            ctx.beginPath();
            _.each(shape.elements, function (command) {
                g.drawCommand(ctx, command);
            });
            if (shape.fill !== null) {
                ctx.fillStyle = g.getColor(shape.fill);
                ctx.fill();
            }
            if (shape.stroke && shape.strokeWidth && shape.strokeWidth > 0) {
                ctx.strokeStyle = g.getColor(shape.stroke);
                ctx.lineWidth = shape.strokeWidth;
                ctx.stroke();
            }
        }
    } catch (err) {
        console.log("Error while drawing:", err);
    }
};

// The SVG engine uses code from the following libraries:
// - for parsing the main svg tree: two.js - http://jonobr1.github.io/two.js/
// - for constructing individual paths: canvg - https://code.google.com/p/canvg/
// - for constructing arcs: fabric.js - http://fabricjs.com

g.svg = {};

g.svg.interpret = function (svgNode) {
    var node,
        tag = svgNode.tagName.toLowerCase();
    if (!(tag in g.svg.read)) {
        return null;
    }

    node = g.svg.read[tag].call(this, svgNode);
    return node;
};

g.svg.getReflection = function (a, b, relative) {
    var theta,
        d = g.geometry.distance(a.x, a.y, b.x, b.y);

    if (d <= 0.0001) {
        return relative ? g.ZERO : a;
    }
    theta = g.geometry.angle(a.x, a.y, b.x, b.y);
    return g.makePoint(
        d * Math.cos(theta) + (relative ? 0 : a.x),
        d * Math.sin(theta) + (relative ? 0 : a.y)
    );
};

g.svg.trim = function (s) {
    return s.replace(/^\s+|\s+$/g, '');
};

g.svg.compressSpaces = function (s) {
    return s.replace(/[\s\r\t\n]+/gm, ' ');
};

g.svg.ToNumberArray = function (s) {
    var i,
        a = g.svg.trim(g.svg.compressSpaces((s || '').replace(/,/g, ' '))).split(' ');
    for (i = 0; i < a.length; i += 1) {
        a[i] = parseFloat(a[i]);
    }
    return a;
};

g.svg.read = {

    svg: function () {
        return g.svg.read.g.apply(this, arguments);
    },

    g: function (node) {

        var group = [];

        _.each(node.childNodes, function(n) {

            var tag, tagName, o;
            tag = n.nodeName;
            if (!tag) { return; }
            tagName = tag.replace(/svg\:/ig, '').toLowerCase();
            if (tagName in g.svg.read) {
                o = g.svg.read[tagName].call(this, n);
                group.push(o);
            }
        });

        return g.svg.applySvgAttributes(node, group);
    },

    polygon: function (node, open) {
        var points = node.getAttribute('points'),
            elements = [],
            poly;
        points.replace(/([\d\.?]+),([\d\.?]+)/g, function (match, p1, p2) {
            elements.push((elements.length === 0 ? g.moveto : g.lineto)(parseFloat(p1), parseFloat(p2)));
        });
        if (!open) {
            elements.push(g.closePath());
        }

        poly = Object.freeze({ elements: elements });
        return g.svg.applySvgAttributes(node, poly);
    },

    polyline: function (node) {
        return g.svg.read.polygon(node, true);
    },

    rect: function (node) {
        var x, y, width, height;
        x = parseFloat(node.getAttribute('x'));
        y = parseFloat(node.getAttribute('y'));
        width = parseFloat(node.getAttribute('width'));
        height = parseFloat(node.getAttribute('height'));
        return g.svg.applySvgAttributes(node, g.rect(x, y, width, height));
    },

    ellipse: function (node) {
        var cx, cy, rx, ry, x, y, width, height;
        cx = parseFloat(node.getAttribute('cx'));
        cy = parseFloat(node.getAttribute('cy'));
        rx = parseFloat(node.getAttribute('rx'));
        ry = parseFloat(node.getAttribute('ry'));
        x = cx - rx;
        y = cy - ry;
        width = rx * 2;
        height = ry * 2;
        return g.svg.applySvgAttributes(node, g.ellipse(x, y, width, height));
    },

    circle: function (node) {
        var cx, cy, r, x, y, width, height;
        cx = parseFloat(node.getAttribute('cx'));
        cy = parseFloat(node.getAttribute('cy'));
        r = parseFloat(node.getAttribute('r'));
        x = cx - r;
        y = cy - r;
        width = height = r * 2;
        return g.svg.applySvgAttributes(node, g.ellipse(x, y, width, height));
    },

    line: function (node) {
        var x1, y1, x2, y2;
        x1 = parseFloat(node.getAttribute('x1'));
        y1 = parseFloat(node.getAttribute('y1'));
        x2 = parseFloat(node.getAttribute('x2'));
        y2 = parseFloat(node.getAttribute('y2'));
        return g.svg.applySvgAttributes(node, g.line(x1, y1, x2, y2));
    },

    path: function (node) {
        var d, PathParser, elements, pp,
            p, newP, curr, p1, cntrl, cp, cp1x, cp1y, cp2x, cp2y,
            rx, ry, rot, large, sweep, ex, ey, segs, i, bez;
        // TODO: convert to real lexer based on http://www.w3.org/TR/SVG11/paths.html#PathDataBNF
        d = node.getAttribute('d');
        d = d.replace(/,/gm, ' '); // get rid of all commas
        d = d.replace(/([MmZzLlHhVvCcSsQqTtAa])([MmZzLlHhVvCcSsQqTtAa])/gm, '$1 $2'); // separate commands from commands
        d = d.replace(/([MmZzLlHhVvCcSsQqTtAa])([MmZzLlHhVvCcSsQqTtAa])/gm, '$1 $2'); // separate commands from commands
        d = d.replace(/([MmZzLlHhVvCcSsQqTtAa])([^\s])/gm, '$1 $2'); // separate commands from points
        d = d.replace(/([^\s])([MmZzLlHhVvCcSsQqTtAa])/gm, '$1 $2'); // separate commands from points
        d = d.replace(/([0-9])([+\-])/gm, '$1 $2'); // separate digits when no comma
        d = d.replace(/(\.[0-9]*)(\.)/gm, '$1 $2'); // separate digits when no comma
        d = d.replace(/([Aa](\s+[0-9]+){3})\s+([01])\s*([01])/gm, '$1 $3 $4 '); // shorthand elliptical arc path syntax
        d = g.svg.compressSpaces(d); // compress multiple spaces
        d = g.svg.trim(d);

        PathParser = function (d) {
            this.tokens = d.split(' ');

            this.reset = function () {
                this.i = -1;
                this.command = '';
                this.previousCommand = '';
                this.start = g.makePoint(0, 0);
                this.control = g.makePoint(0, 0);
                this.current = g.makePoint(0, 0);
                this.points = [];
                this.angles = [];
            };

            this.isEnd = function () {
                return this.i >= this.tokens.length - 1;
            };

            this.isCommandOrEnd = function () {
                if (this.isEnd()) { return true; }
                return this.tokens[this.i + 1].match(/^[A-Za-z]$/) !== null;
            };

            this.isRelativeCommand = function () {
                switch (this.command) {
                case 'm':
                case 'l':
                case 'h':
                case 'v':
                case 'c':
                case 's':
                case 'q':
                case 't':
                case 'a':
                case 'z':
                    return true;
                }
                return false;
            };

            this.getToken = function () {
                this.i += 1;
                return this.tokens[this.i];
            };

            this.getScalar = function () {
                return parseFloat(this.getToken());
            };

            this.nextCommand = function () {
                this.previousCommand = this.command;
                this.command = this.getToken();
            };

            this.getPoint = function () {
                var pt = g.makePoint(this.getScalar(), this.getScalar());
                return this.makeAbsolute(pt);
            };

            this.getAsControlPoint = function () {
                var pt = this.getPoint();
                this.control = pt;
                return pt;
            };

            this.getAsCurrentPoint = function () {
                var pt = this.getPoint();
                this.current = pt;
                return pt;
            };

            this.getReflectedControlPoint = function () {
                if (this.previousCommand.toLowerCase() !== 'c' &&
                        this.previousCommand.toLowerCase() !== 's' &&
                        this.previousCommand.toLowerCase() !== 'q' &&
                        this.previousCommand.toLowerCase() !== 't') {
                    return this.current;
                }

                // reflect point
                var pt = g.makePoint(2 * this.current.x - this.control.x, 2 * this.current.y - this.control.y);
                return pt;
            };

            this.makeAbsolute = function (p) {
                if (this.isRelativeCommand()) {
                    return g.makePoint(p.x + this.current.x, p.y + this.current.y);
                }
                return p;
            };
        };

        elements = [];

        pp = new PathParser(d);
        pp.reset();

        while (!pp.isEnd()) {
            pp.nextCommand();
            switch (pp.command) {
            case 'M':
            case 'm':
                p = pp.getAsCurrentPoint();
                elements.push(g.moveTo(p.x, p.y));
                pp.start = pp.current;
                while (!pp.isCommandOrEnd()) {
                    p = pp.getAsCurrentPoint();
                    elements.push(g.lineTo(p.x, p.y));
                }
                break;
            case 'L':
            case 'l':
                while (!pp.isCommandOrEnd()) {
                    p = pp.getAsCurrentPoint();
                    elements.push(g.lineTo(p.x, p.y));
                }
                break;
            case 'H':
            case 'h':
                while (!pp.isCommandOrEnd()) {
                    newP = g.makePoint((pp.isRelativeCommand() ? pp.current.x : 0) + pp.getScalar(), pp.current.y);
                    pp.current = newP;
                    elements.push(g.lineTo(pp.current.x, pp.current.y));
                }
                break;
            case 'V':
            case 'v':
                while (!pp.isCommandOrEnd()) {
                    newP = g.makePoint(pp.current.x, (pp.isRelativeCommand() ? pp.current.y : 0) + pp.getScalar());
                    pp.current = newP;
                    elements.push(g.lineTo(pp.current.x, pp.current.y));
                }
                break;
            case 'C':
            case 'c':
                while (!pp.isCommandOrEnd()) {
                    curr = pp.current;
                    p1 = pp.getPoint();
                    cntrl = pp.getAsControlPoint();
                    cp = pp.getAsCurrentPoint();
                    elements.push(g.curveTo(p1.x, p1.y, cntrl.x, cntrl.y, cp.x, cp.y));
                }
                break;
            case 'S':
            case 's':
                while (!pp.isCommandOrEnd()) {
                    curr = pp.current;
                    p1 = pp.getReflectedControlPoint();
                    cntrl = pp.getAsControlPoint();
                    cp = pp.getAsCurrentPoint();
                    elements.push(g.curveTo(p1.x, p1.y, cntrl.x, cntrl.y, cp.x, cp.y));
                }
                break;
            case 'Q':
            case 'q':
                while (!pp.isCommandOrEnd()) {
                    curr = pp.current;
                    cntrl = pp.getAsControlPoint();
                    cp = pp.getAsCurrentPoint();
                    cp1x = curr.x + 2 / 3 * (cntrl.x - curr.x); // CP1 = QP0 + 2 / 3 *(QP1-QP0)
                    cp1y = curr.y + 2 / 3 * (cntrl.y - curr.y); // CP1 = QP0 + 2 / 3 *(QP1-QP0)
                    cp2x = cp1x + 1 / 3 * (cp.x - curr.x); // CP2 = CP1 + 1 / 3 *(QP2-QP0)
                    cp2y = cp1y + 1 / 3 * (cp.y - curr.y); // CP2 = CP1 + 1 / 3 *(QP2-QP0)
                    elements.push(g.curveTo(cp1x, cp1y, cp2x, cp2y, cp.x, cp.y));
                }
                break;
            case 'T':
            case 't':
                while (!pp.isCommandOrEnd()) {
                    curr = pp.current;
                    cntrl = pp.getReflectedControlPoint();
                    pp.control = cntrl;
                    cp = pp.getAsCurrentPoint();
                    cp1x = curr.x + 2 / 3 * (cntrl.x - curr.x); // CP1 = QP0 + 2 / 3 *(QP1-QP0)
                    cp1y = curr.y + 2 / 3 * (cntrl.y - curr.y); // CP1 = QP0 + 2 / 3 *(QP1-QP0)
                    cp2x = cp1x + 1 / 3 * (cp.x - curr.x); // CP2 = CP1 + 1 / 3 *(QP2-QP0)
                    cp2y = cp1y + 1 / 3 * (cp.y - curr.y); // CP2 = CP1 + 1 / 3 *(QP2-QP0)
                    elements.push(g.curveTo(cp1x, cp1y, cp2x, cp2y, cp.x, cp.y));
                }
                break;
            case 'A':
            case 'a':
                while (!pp.isCommandOrEnd()) {
                    curr = pp.current;
                    rx = pp.getScalar();
                    ry = pp.getScalar();
                    rot = pp.getScalar();// * (Math.PI / 180.0);
                    large = pp.getScalar();
                    sweep = pp.getScalar();
                    cp = pp.getAsCurrentPoint();
                    ex = cp.x;
                    ey = cp.y;
                    segs = g.svg.arcToSegments(ex, ey, rx, ry, large, sweep, rot, curr.x, curr.y);
                    for (i = 0; i < segs.length; i += 1) {
                        bez = g.svg.segmentToBezier.apply(this, segs[i]);
                        elements.push(g.curveTo.apply(this, bez));
                    }
                }
                break;
            case 'Z':
            case 'z':
                elements.push(g.closePath());
                pp.current = pp.start;
                break;
            }
        }
        return g.svg.applySvgAttributes(node, g.makePath(elements));
    }
};

g.svg.applySvgAttributes = function (node, shape) {
    var fill, stroke, strokeWidth, transforms, types, transform, i;

    transforms = [];
    types = {};

    types.translate = function (s) {
        var p = g.makePoint.apply(null, g.svg.ToNumberArray(s));
        return g.translate(g.IDENTITY, p.x || 0.0, p.y || 0.0);
    };

    types.matrix = function (s) {
        var m = g.svg.ToNumberArray(s);
        return [m[0], m[1], 0, m[2], m[3], 0, m[4], m[5], 1];
    };

    _.each(node.attributes, function (v) {
        var property, data, type, s, d;
        property = v.nodeName;

        switch (property) {
        case 'transform':
            data = g.svg.trim(g.svg.compressSpaces(v.nodeValue)).replace(/\)(\s?,\s?)/g, ') ').split(/\s(?=[a-z])/);
            for (i = 0; i < data.length; i += 1) {
                type = g.svg.trim(data[i].split('(')[0]);
                s = data[i].split('(')[1].replace(')', '');
                transform = types[type](s);
                transforms.push(transform);
            }
            break;
        case 'visibility':
//          elem.visible = !!v.nodeValue;
            break;
        case 'stroke-linecap':
//          elem.cap = v.nodeValue;
            break;
        case 'stroke-linejoin':
//          elem.join = v.nodeValue;
            break;
        case 'stroke-miterlimit':
//          elem.miter = v.nodeValue;
            break;
        case 'stroke-width':
//          elem.linewidth = parseFloat(v.nodeValue);
            strokeWidth = parseFloat(v.nodeValue);
            break;
        case 'stroke-opacity':
        case 'fill-opacity':
//          elem.opacity = v.nodeValue;
            break;
        case 'fill':
            fill = v.nodeValue;
            break;
        case 'stroke':
            stroke = v.nodeValue;
            break;
        case 'style':
            d = {};
            _.each(v.nodeValue.split(';'), function (s) {
                var el = s.split(':');
                d[el[0].trim()] = el[1];
            });
            if (d.fill) {
                fill = d.fill;
            }
            if (d.stroke) {
                stroke = d.stroke;
            }
            if (d["stroke-width"]) {
                strokeWidth = parseFloat(d["stroke-width"]);
            }
            break;
        }
    });

    transform = g.IDENTITY;
    for (i = 0; i < transforms.length; i += 1) {
        transform = g.append(transform, transforms[i]);
    }

    function applyAttributes(shape) {
        if (shape.elements) {
            return Object.freeze({
                elements: g.transformPath(shape, transform).elements,
                fill: (fill === undefined) ? shape.fill : fill,
                stroke: (stroke === undefined) ? shape.stroke : stroke,
                strokeWidth: transform[0] * ((strokeWidth === undefined) ? shape.strokeWidth : strokeWidth)
            });
        }
        return _.map(shape, applyAttributes);
    }

    return applyAttributes(shape);
};

g.svg.arcToSegments = function (x, y, rx, ry, large, sweep, rotateX, ox, oy) {
/*    argsString = _join.call(arguments);
    if (arcToSegmentsCache[argsString]) {
      return arcToSegmentsCache[argsString];
    } */
    var th, sin_th, cos_th, px, py, pl,
        a00, a01, a10, a11, x0, y0, x1, y1,
        d, sfactor_sq, sfactor, xc, yc,
        th0, th1, th_arc,
        segments, result, th2, th3, i;

    th = rotateX * (Math.PI / 180);
    sin_th = Math.sin(th);
    cos_th = Math.cos(th);
    rx = Math.abs(rx);
    ry = Math.abs(ry);
    px = cos_th * (ox - x) * 0.5 + sin_th * (oy - y) * 0.5;
    py = cos_th * (oy - y) * 0.5 - sin_th * (ox - x) * 0.5;
    pl = (px * px) / (rx * rx) + (py * py) / (ry * ry);
    if (pl > 1) {
        pl = Math.sqrt(pl);
        rx *= pl;
        ry *= pl;
    }

    a00 = cos_th / rx;
    a01 = sin_th / rx;
    a10 = (-sin_th) / ry;
    a11 = cos_th / ry;
    x0 = a00 * ox + a01 * oy;
    y0 = a10 * ox + a11 * oy;
    x1 = a00 * x + a01 * y;
    y1 = a10 * x + a11 * y;

    d = (x1 - x0) * (x1 - x0) + (y1 - y0) * (y1 - y0);
    sfactor_sq = 1 / d - 0.25;
    if (sfactor_sq < 0) { sfactor_sq = 0; }
    sfactor = Math.sqrt(sfactor_sq);
    if (sweep === large) { sfactor = -sfactor; }
    xc = 0.5 * (x0 + x1) - sfactor * (y1 - y0);
    yc = 0.5 * (y0 + y1) + sfactor * (x1 - x0);

    th0 = Math.atan2(y0 - yc, x0 - xc);
    th1 = Math.atan2(y1 - yc, x1 - xc);

    th_arc = th1 - th0;
    if (th_arc < 0 && sweep === 1) {
        th_arc += 2 * Math.PI;
    } else if (th_arc > 0 && sweep === 0) {
        th_arc -= 2 * Math.PI;
    }

    segments = Math.ceil(Math.abs(th_arc / (Math.PI * 0.5 + 0.001)));
    result = [];
    for (i = 0; i < segments; i += 1) {
        th2 = th0 + i * th_arc / segments;
        th3 = th0 + (i + 1) * th_arc / segments;
        result[i] = [xc, yc, th2, th3, rx, ry, sin_th, cos_th];
    }

//    arcToSegmentsCache[argsString] = result;
    return result;
};

g.svg.segmentToBezier = function (cx, cy, th0, th1, rx, ry, sin_th, cos_th) {
//    argsString = _join.call(arguments);
//    if (segmentToBezierCache[argsString]) {
//      return segmentToBezierCache[argsString];
//    }

    var a00 = cos_th * rx,
        a01 = -sin_th * ry,
        a10 = sin_th * rx,
        a11 = cos_th * ry,

        th_half = 0.5 * (th1 - th0),
        t = (8 / 3) * Math.sin(th_half * 0.5) * Math.sin(th_half * 0.5) / Math.sin(th_half),
        x1 = cx + Math.cos(th0) - t * Math.sin(th0),
        y1 = cy + Math.sin(th0) + t * Math.cos(th0),
        x3 = cx + Math.cos(th1),
        y3 = cy + Math.sin(th1),
        x2 = x3 + t * Math.sin(th1),
        y2 = y3 - t * Math.cos(th1);

//    segmentToBezierCache[argsString] = [
    return [
        a00 * x1 + a01 * y1, a10 * x1 + a11 * y1,
        a00 * x2 + a01 * y2,      a10 * x2 + a11 * y2,
        a00 * x3 + a01 * y3,      a10 * x3 + a11 * y3
    ];

//    return segmentToBezierCache[argsString];
};
