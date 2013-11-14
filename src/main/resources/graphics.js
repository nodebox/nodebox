/*---  Based on: canvas.js, https://github.com/clips/pattern/blob/master/pattern/canvas.js (BSD)
  ---  De Smedt T. & Daelemans W. (2012). Pattern for Python. Journal of Machine Learning Research. --*/

var g = {};

g.RGB = "RGB";
g.HSB = "HSB";
g.HEX = "HEX";

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

g.roundedRect = function (cx, cy, width, height, rx, ry) {
    var ONE_MINUS_QUARTER = 1.0 - 0.552,

        elements = [],

        halfWidth = width / 2,
        halfHeight = height / 2,
        dx = rx,
        dy = ry,

        left = cx - halfWidth,
        right = cx + halfWidth,
        top = cy - halfHeight,
        bottom = cy + halfHeight;

    // rx/ry cannot be greater than half of the width of the rectangle
    // (required by SVG spec)
    dx = Math.min(dx, width * 0.5);
    dy = Math.min(dy, height * 0.5);
    elements.push(g.moveto(left + dx, top));
    if (dx < width * 0.5) {
        elements.push(g.lineto(right - rx, top));
    }
    elements.push(g.curveto(right - dx * ONE_MINUS_QUARTER, top, right, top + dy * ONE_MINUS_QUARTER, right, top + dy));
    if (dy < height * 0.5) {
        elements.push(g.lineto(right, bottom - dy));
    }
    elements.push(g.curveto(right, bottom - dy * ONE_MINUS_QUARTER, right - dx * ONE_MINUS_QUARTER, bottom, right - dx, bottom));
    if (dx < width * 0.5) {
        elements.push(g.lineto(left + dx, bottom));
    }
    elements.push(g.curveto(left + dx * ONE_MINUS_QUARTER, bottom, left, bottom - dy * ONE_MINUS_QUARTER, left, bottom - dy));
    if (dy < height * 0.5) {
        elements.push(g.lineto(left, top + dy));
    }
    elements.push(g.curveto(left, top + dy * ONE_MINUS_QUARTER, left + dx * ONE_MINUS_QUARTER, top, left + dx, top));
    elements.push(g.closepath());
    return Object.freeze({ elements: elements });
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

g.arc = function (x, y, width, height, startAngle, degrees, arcType) {
    var w, h, angStRad, ext, arcSegs, increment, cv, lineSegs,
        index, elements, angle, relx, rely, coords;
    w = width / 2;
    h = height / 2;
    angStRad = g.math.radians(startAngle);
    ext = degrees;

    if (ext >= 360.0 || ext <= -360) {
        arcSegs = 4;
        increment = Math.PI / 2;
        cv = 0.5522847498307933;
        if (ext < 0) {
            increment = -increment;
            cv = -cv;
        }
    } else {
        arcSegs = Math.ceil(Math.abs(ext) / 90.0);
        increment = g.math.radians(ext / arcSegs);
        cv = 4.0 / 3.0 * Math.sin(increment / 2.0) / (1.0 + Math.cos(increment / 2.0));
        if (cv === 0) {
            arcSegs = 0;
        }
    }

    if (arcType === "open") {
        lineSegs = 0;
    } else if (arcType === "chord") {
        lineSegs = 1;
    } else if (arcType === "pie") {
        lineSegs = 2;
    }

    if (w < 0 || h < 0) {
        arcSegs = lineSegs = -1;
    }

    index = 0;
    elements = [];
    while (index <= arcSegs + lineSegs) {
        angle = angStRad;
        if (index === 0) {
            elements.push(
                g.moveTo(x + Math.cos(angle) * w,
                         y + Math.sin(angle) * h)
            );
        } else if (index > arcSegs) {
            if (index === arcSegs + lineSegs) {
                elements.push(g.closePath());
            } else {
                elements.push(g.lineTo(x, y));
            }
        } else {
            angle += increment * (index - 1);
            relx = Math.cos(angle);
            rely = Math.sin(angle);
            coords = [];
            coords.push(x + (relx - cv * rely) * w);
            coords.push(y + (rely + cv * relx) * h);
            angle += increment;
            relx = Math.cos(angle);
            rely = Math.sin(angle);
            coords.push(x + (relx + cv * rely) * w);
            coords.push(y + (rely - cv * relx) * h);
            coords.push(x + relx * w);
            coords.push(y + rely * h);
            elements.push(g.curveTo.apply(null, coords));
        }
        index += 1;
    }

    return Object.freeze({ elements: elements });
};

g.makePath = function (pe, fill, stroke, strokeWidth) {
    var elements = pe.elements || pe,
        d = { elements: elements };
    if (fill !== undefined) { d.fill = fill; }
    if (stroke !== undefined) { d.stroke = stroke; }
    if (strokeWidth !== undefined) { d.strokeWidth = strokeWidth; }
    return Object.freeze(d);
};

g.makeGroup = function (shapes) {
    var newShapes = [];
    if (shapes.shapes || shapes.elements) {
        newShapes = [shapes];
    } else if (shapes) {
        newShapes = shapes;
    }
    return Object.freeze({
        shapes: newShapes
    });
};

g.colorizeGroup = function (group, fill, stroke, strokeWidth) {
    var shapes = _.map(group.shapes, function (shape) {
        return g.colorizeShape(shape, fill, stroke, strokeWidth);
    });
    return g.makeGroup(shapes);
};

g.colorizeShape = function (shape, fill, stroke, strokeWidth) {
    var fn = (shape.shapes) ? g.colorizeGroup : g.makePath;
    return fn(shape, fill, stroke, strokeWidth);
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

g.pathBounds = function (path) {
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

g.groupBounds = function (group) {
    if (_.isEmpty(group.shapes)) { return g.makeRect(0, 0, 0, 0); }
    var i, r, shape,
        shapes = group.shapes;
    for (i = 0; i < shapes.length; i += 1) {
        shape = shapes[i];
        if (r === undefined) {
            r = g.bounds(shape);
        }
        if ((shape.shapes && !_.isEmpty(shape.shapes)) ||
           (shape.elements && !_.isEmpty(shape.elements))) {
            r = g.unitedRect(r, g.bounds(shape));
        }
    }
    return (r !== undefined) ? r : g.makeRect(0, 0, 0, 0);
};

g.bounds = function (shape) {
    if (shape.elements) { return g.pathBounds(shape); }
    if (shape.shapes) { return g.groupBounds(shape); }
    return g.makeRect(0, 0, 0, 0);
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

g.unitedRect = function (r1, r2) {
    r1 = g.normalizedRect(r1);
    r2 = g.normalizedRect(r2);

    var x = Math.min(r1.x, r2.x),
        y = Math.min(r1.y, r2.y),
        width = Math.max(r1.x + r1.width, r2.x + r2.width) - x,
        height = Math.max(r1.y + r1.height, r2.y + r2.height) - y;

    return g.makeRect(x, y, width, height);

};

g.getRectCentroid = function (rect) {
    return g.makePoint(rect.x + rect.width / 2,  rect.y + rect.height / 2);
};

g._namedColors = {
    "lightpink"            : [1.00, 0.71, 0.76],
    "pink"                 : [1.00, 0.75, 0.80],
    "crimson"              : [0.86, 0.08, 0.24],
    "lavenderblush"        : [1.00, 0.94, 0.96],
    "palevioletred"        : [0.86, 0.44, 0.58],
    "hotpink"              : [1.00, 0.41, 0.71],
    "deeppink"             : [1.00, 0.08, 0.58],
    "mediumvioletred"      : [0.78, 0.08, 0.52],
    "orchid"               : [0.85, 0.44, 0.84],
    "thistle"              : [0.85, 0.75, 0.85],
    "plum"                 : [0.87, 0.63, 0.87],
    "violet"               : [0.93, 0.51, 0.93],
    "fuchsia"              : [1.00, 0.00, 1.00],
    "darkmagenta"          : [0.55, 0.00, 0.55],
    "purple"               : [0.50, 0.00, 0.50],
    "mediumorchid"         : [0.73, 0.33, 0.83],
    "darkviolet"           : [0.58, 0.00, 0.83],
    "darkorchid"           : [0.60, 0.20, 0.80],
    "indigo"               : [0.29, 0.00, 0.51],
    "blueviolet"           : [0.54, 0.17, 0.89],
    "mediumpurple"         : [0.58, 0.44, 0.86],
    "mediumslateblue"      : [0.48, 0.41, 0.93],
    "slateblue"            : [0.42, 0.35, 0.80],
    "darkslateblue"        : [0.28, 0.24, 0.55],
    "ghostwhite"           : [0.97, 0.97, 1.00],
    "lavender"             : [0.90, 0.90, 0.98],
    "blue"                 : [0.00, 0.00, 1.00],
    "mediumblue"           : [0.00, 0.00, 0.80],
    "darkblue"             : [0.00, 0.00, 0.55],
    "navy"                 : [0.00, 0.00, 0.50],
    "midnightblue"         : [0.10, 0.10, 0.44],
    "royalblue"            : [0.25, 0.41, 0.88],
    "cornflowerblue"       : [0.39, 0.58, 0.93],
    "lightsteelblue"       : [0.69, 0.77, 0.87],
    "lightslategray"       : [0.47, 0.53, 0.60],
    "slategray"            : [0.44, 0.50, 0.56],
    "dodgerblue"           : [0.12, 0.56, 1.00],
    "aliceblue"            : [0.94, 0.97, 1.00],
    "steelblue"            : [0.27, 0.51, 0.71],
    "lightskyblue"         : [0.53, 0.81, 0.98],
    "skyblue"              : [0.53, 0.81, 0.92],
    "deepskyblue"          : [0.00, 0.75, 1.00],
    "lightblue"            : [0.68, 0.85, 0.90],
    "powderblue"           : [0.69, 0.88, 0.90],
    "cadetblue"            : [0.37, 0.62, 0.63],
    "darkturquoise"        : [0.00, 0.81, 0.82],
    "azure"                : [0.94, 1.00, 1.00],
    "lightcyan"            : [0.88, 1.00, 1.00],
    "paleturquoise"        : [0.69, 0.93, 0.93],
    "aqua"                 : [0.00, 1.00, 1.00],
    "darkcyan"             : [0.00, 0.55, 0.55],
    "teal"                 : [0.00, 0.50, 0.50],
    "darkslategray"        : [0.18, 0.31, 0.31],
    "mediumturquoise"      : [0.28, 0.82, 0.80],
    "lightseagreen"        : [0.13, 0.70, 0.67],
    "turquoise"            : [0.25, 0.88, 0.82],
    "aquamarine"           : [0.50, 1.00, 0.83],
    "mediumaquamarine"     : [0.40, 0.80, 0.67],
    "mediumspringgreen"    : [0.00, 0.98, 0.60],
    "mintcream"            : [0.96, 1.00, 0.98],
    "springgreen"          : [0.00, 1.00, 0.50],
    "mediumseagreen"       : [0.24, 0.70, 0.44],
    "seagreen"             : [0.18, 0.55, 0.34],
    "honeydew"             : [0.94, 1.00, 0.94],
    "darkseagreen"         : [0.56, 0.74, 0.56],
    "palegreen"            : [0.60, 0.98, 0.60],
    "lightgreen"           : [0.56, 0.93, 0.56],
    "limegreen"            : [0.20, 0.80, 0.20],
    "lime"                 : [0.00, 1.00, 0.00],
    "forestgreen"          : [0.13, 0.55, 0.13],
    "green"                : [0.00, 0.50, 0.00],
    "darkgreen"            : [0.00, 0.39, 0.00],
    "lawngreen"            : [0.49, 0.99, 0.00],
    "chartreuse"           : [0.50, 1.00, 0.00],
    "greenyellow"          : [0.68, 1.00, 0.18],
    "darkolivegreen"       : [0.33, 0.42, 0.18],
    "yellowgreen"          : [0.60, 0.80, 0.20],
    "olivedrab"            : [0.42, 0.56, 0.14],
    "ivory"                : [1.00, 1.00, 0.94],
    "beige"                : [0.96, 0.96, 0.86],
    "lightyellow"          : [1.00, 1.00, 0.88],
    "lightgoldenrodyellow" : [0.98, 0.98, 0.82],
    "yellow"               : [1.00, 1.00, 0.00],
    "olive"                : [0.50, 0.50, 0.00],
    "darkkhaki"            : [0.74, 0.72, 0.42],
    "palegoldenrod"        : [0.93, 0.91, 0.67],
    "lemonchiffon"         : [1.00, 0.98, 0.80],
    "khaki"                : [0.94, 0.90, 0.55],
    "gold"                 : [1.00, 0.84, 0.00],
    "cornsilk"             : [1.00, 0.97, 0.86],
    "goldenrod"            : [0.85, 0.65, 0.13],
    "darkgoldenrod"        : [0.72, 0.53, 0.04],
    "floralwhite"          : [1.00, 0.98, 0.94],
    "oldlace"              : [0.99, 0.96, 0.90],
    "wheat"                : [0.96, 0.87, 0.07],
    "orange"               : [1.00, 0.65, 0.00],
    "moccasin"             : [1.00, 0.89, 0.71],
    "papayawhip"           : [1.00, 0.94, 0.84],
    "blanchedalmond"       : [1.00, 0.92, 0.80],
    "navajowhite"          : [1.00, 0.87, 0.68],
    "antiquewhite"         : [0.98, 0.92, 0.84],
    "tan"                  : [0.82, 0.71, 0.55],
    "burlywood"            : [0.87, 0.72, 0.53],
    "darkorange"           : [1.00, 0.55, 0.00],
    "bisque"               : [1.00, 0.89, 0.77],
    "linen"                : [0.98, 0.94, 0.90],
    "peru"                 : [0.80, 0.52, 0.25],
    "peachpuff"            : [1.00, 0.85, 0.73],
    "sandybrown"           : [0.96, 0.64, 0.38],
    "chocolate"            : [0.82, 0.41, 0.12],
    "saddlebrown"          : [0.55, 0.27, 0.07],
    "seashell"             : [1.00, 0.96, 0.93],
    "sienna"               : [0.63, 0.32, 0.18],
    "lightsalmon"          : [1.00, 0.63, 0.48],
    "coral"                : [1.00, 0.50, 0.31],
    "orangered"            : [1.00, 0.27, 0.00],
    "darksalmon"           : [0.91, 0.59, 0.48],
    "tomato"               : [1.00, 0.39, 0.28],
    "salmon"               : [0.98, 0.50, 0.45],
    "mistyrose"            : [1.00, 0.89, 0.88],
    "lightcoral"           : [0.94, 0.50, 0.50],
    "snow"                 : [1.00, 0.98, 0.98],
    "rosybrown"            : [0.74, 0.56, 0.56],
    "indianred"            : [0.80, 0.36, 0.36],
    "red"                  : [1.00, 0.00, 0.00],
    "brown"                : [0.65, 0.16, 0.16],
    "firebrick"            : [0.70, 0.13, 0.13],
    "darkred"              : [0.55, 0.00, 0.00],
    "maroon"               : [0.50, 0.00, 0.00],
    "white"                : [1.00, 1.00, 1.00],
    "whitesmoke"           : [0.96, 0.96, 0.96],
    "gainsboro"            : [0.86, 0.86, 0.86],
    "lightgrey"            : [0.83, 0.83, 0.83],
    "silver"               : [0.75, 0.75, 0.75],
    "darkgray"             : [0.66, 0.66, 0.66],
    "gray"                 : [0.50, 0.50, 0.50],
    "grey"                 : [0.50, 0.50, 0.50],
    "dimgray"              : [0.41, 0.41, 0.41],
    "dimgrey"              : [0.41, 0.41, 0.41],
    "black"                : [0.00, 0.00, 0.00],
    "cyan"                 : [0.00, 0.68, 0.94],

    "transparent"          : [0.00, 0.00, 0.00, 0.00],
    "bark"                 : [0.25, 0.19, 0.13]
};

// Converts the given R,G,B values to a hexadecimal color string.
g._rgb2hex = function (r, g, b) {
    var parseHex = function (i) {
        return ((i === 0) ? "00" : (i.length < 2) ? "0" + i : i).toString(16).toUpperCase();
    };
    return "#"
        + parseHex(Math.round(r * 255))
        + parseHex(Math.round(g * 255))
        + parseHex(Math.round(b * 255));
};

// Converts the given hexadecimal color string to R,G,B (between 0.0-1.0).
g._hex2rgb = function (hex) {
    var arr, r, g, b;
    hex = hex.replace(/^#/, "");
    if (hex.length < 6) { // hex += hex[-1] * (6-hex.length);
        arr = [];
        arr.length = 6 - hex.length;
        hex += arr.join(hex.substr(hex.length - 1));
    }
    r = parseInt(hex.substr(0, 2), 16) / 255;
    g = parseInt(hex.substr(2, 2), 16) / 255;
    b = parseInt(hex.substr(4, 2), 16) / 255;
    return [r, g, b];
};

// Converts the given R,G,B values to H,S,B (between 0.0-1.0).
g._rgb2hsb = function (r, g, b) {
    var h = 0,
        s = 0,
        v = Math.max(r, g, b),
        d = v - Math.min(r, g, b);
    if (v !== 0) {
        s = d / v;
    }
    if (s !== 0) {
        if (r === v) {
            h = (g - b) / d;
        } else if (g === v) {
            h = 2 + (b - r) / d;
        } else {
            h = 4 + (r - g) / d;
        }
    }
    h = h / 6.0 % 1;
    return [h, s, v];
};

// Converts the given H,S,B color values to R,G,B (between 0.0-1.0).
g._hsb2rgb = function (h, s, v) {
    if (s === 0) {
        return [v, v, v];
    }
    h = h % 1 * 6.0;
    var i = Math.floor(h),
        f = h - i,
        x = v * (1 - s),
        y = v * (1 - s * f),
        z = v * (1 - s * (1 - f));
    if (i > 4) {
        return [v, x, y];
    }
    return [[v, z, x], [y, v, x], [x, v, z], [x, y, v], [z, x, v]][parseInt(i, 10)];
};

g.color = function (R, G, B, A, options) {
    var rgb;
    if (R.r && R.g && R.b && R.a) { return R; }
    if (R instanceof Array) {
        R = R[0];
        G = R[1];
        B = R[2];
        A = R[3] || 1;
    } else if (R === undefined || R === null) {
        R = G = B = A = 0;
    } else if (G === undefined || G === null) {
        G = B = R;
        A = 1;
    } else if (B === undefined || B === null) {
        A = G;
        G = B = R;
    } else if (A === undefined || A === null) {
        A = 1;
    }

    if (options) {
        // Transform to base 1:
        if (options.base !== undefined) {
            R /= options.base;
            G /= options.base;
            B /= options.base;
            A /= options.base;
        }
        // Transform to color space RGB:
        if (options.colorspace === g.HSB) {
            rgb = g._hsb2rgb(R, G, B);
            R = rgb[0];
            G = rgb[1];
            B = rgb[2];
        }
        // Transform to color space HEX:
        if (options.colorspace === g.HEX) {
            rgb = g._hex2rgb(R);
            R = rgb[0];
            G = rgb[1];
            B = rgb[2];
            A = 1;
        }
    }
    return Object.freeze({ r: R, g: G, b: B, a: A });
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

g.transformGroup = function (group, matrix) {
    var shapes = _.map(group.shapes, function (shape) {
        return g.transformShape(shape, matrix);
    });
    return g.makeGroup(shapes);
};

g.transformShape = function (shape, matrix) {
    var fn = (shape.shapes) ? g.transformGroup : g.transformPath;
    return fn(shape, matrix);
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

g._getColor = function (c) {
    if (c === null) { return "none"; }
    if (c === undefined) { return "black"; }
    var R = Math.round(c.r * 255),
        G = Math.round(c.g * 255),
        B = Math.round(c.b * 255);
    return "rgba(" + R + ", " + G + ", " + B + ", " + c.a + ")";
};

g.drawPoints = function (ctx, points) {
    var pt, i;
    ctx.fillStyle = 'blue';
    ctx.beginPath();
    for (i = 0; i < points.length; i += 1) {
        pt = points[i];
        ctx.moveTo(pt.x, pt.y);
        ctx.arc(pt.x, pt.y, 4, 0, Math.PI * 2, false);
    }
    ctx.fill();
};

g.draw = function (ctx, shape) {
    try {
        if (_.isArray(shape)) {
            if (shape[0].x !== undefined && shape[0].y !== undefined) {
                g.drawPoints(ctx, shape);
            } else {
                _.each(shape, _.partial(g.draw, ctx));
            }
        } else if (shape.shapes) {
            _.each(shape.shapes, _.partial(g.draw, ctx));
        } else if (shape.elements) {
            ctx.beginPath();
            _.each(shape.elements, function (command) {
                g.drawCommand(ctx, command);
            });
            if (shape.fill !== null) {
                ctx.fillStyle = g._getColor(shape.fill);
                ctx.fill();
            }
            if (shape.stroke && shape.strokeWidth && shape.strokeWidth > 0) {
                ctx.strokeStyle = g._getColor(shape.stroke);
                ctx.lineWidth = shape.strokeWidth;
                ctx.stroke();
            }
        } else if (shape.x !== undefined && shape.y !== undefined) {
            g.drawPoints(ctx, [shape]);
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

        var shapes = [];

        _.each(node.childNodes, function(n) {

            var tag, tagName, o;
            tag = n.nodeName;
            if (!tag) { return; }
            tagName = tag.replace(/svg\:/ig, '').toLowerCase();
            if (tagName in g.svg.read) {
                o = g.svg.read[tagName].call(this, n);
                shapes.push(o);
            }
        });

        return g.svg.applySvgAttributes(node, g.makeGroup(shapes));
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

    if (shape.elements) {
        fill = "black";
    }

    transforms = [];
    types = {};

    types.translate = function (s) {
        var a = g.svg.ToNumberArray(s),
            tx = a[0],
            ty = a[1] || 0;
        return g.translate(g.IDENTITY, tx, ty);
    };

    types.scale = function (s) {
        var a = g.svg.ToNumberArray(s),
            sx = a[0],
            sy = a[1] || sx;
        return g.scale(g.IDENTITY, sx, sy);
    };

    types.rotate = function (s) {
        var t,
            a = g.svg.ToNumberArray(s),
            r = a[0],
            tx = a[1] || 0,
            ty = a[2] || 0;
        t = g.translate(g.IDENTITY, tx, ty);
        t = g.rotate(t, r);
        t = g.translate(t, -tx, -ty);
        return t;
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

    if (fill !== undefined) {
        if (g._namedColors[fill]) {
            fill = g.color.apply(g, g._namedColors[fill]);
        } else if (fill.indexOf("#") === 0) {
            fill = g.color(fill, 0, 0, 0, { colorspace: g.HEX });
        } else if (fill === "none") {
            fill = null;
        }
    }

    if (stroke !== undefined) {
        if (g._namedColors[stroke]) {
            stroke = g.color.apply(g, g._namedColors[stroke]);
        } else if (stroke.indexOf("#") === 0) {
            stroke = g.color(stroke, 0, 0, 0, { colorspace: g.HEX });
        } else if (stroke === "none") {
            stroke = null;
        }
    }

    transform = g.IDENTITY;
    for (i = 0; i < transforms.length; i += 1) {
        transform = g.append(transform, transforms[i]);
    }

    function applyAttributes(shape) {
        if (shape.elements) {
            var elements = g.transformPath(shape, transform).elements,
                f = (fill === undefined) ? shape.fill : fill,
                s = (stroke === undefined) ? shape.stroke : stroke,
                sw = (strokeWidth === undefined) ? shape.strokeWidth : strokeWidth;
            if (sw !== undefined) {
                sw *= transform[0];
            }
            return g.makePath(elements, f, s, sw);
        } else if (shape.shapes) {
            return g.makeGroup(_.map(shape.shapes, applyAttributes));
        }
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