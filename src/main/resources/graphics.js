var g = {};

g._freeze = function (o) {
    Object.freeze(o);
    return o;
};


/*--- GEOMETRY -------------------------------------------------------------------------------------*/

g.math = {};

g.math.PI = Math.PI;

g.math.round = function (x, decimals) {
    if (!decimals) {
        return Math.round(x);
    } else {
        return Math.round(x * Math.pow(10, decimals)) / Math.pow(10, decimals);
    }
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
    if (max > min) {
        return Math.max(min, Math.min(value, max));
    } else {
        return Math.max(max, Math.min(value, min));
    }
};

g.math.dot = function (a, b) {
    var m = Math.min(a.length, b.length);
    var n = 0;
    for (var i = 0; i < m; i++) n += a[i] * b[i];
    return n;
};

g.math.mix = function (a, b, t) {
    if (t < 0.0) return a;
    if (t > 1.0) return b;
    return a + (b-a)*t;
};


g.geometry = {};

g.geometry.angle = function (x0, y0, x1, y1) {
    /* Returns the angle between two points.
     */
    return g.math.degrees(Math.atan2(y1-y0, x1-x0));
};

g.geometry.distance = function (x0, y0, x1, y1) {
    /* Returns the distance between two points.
     */
    return Math.sqrt(Math.pow(x1-x0, 2) + Math.pow(y1-y0, 2));
};

g.geometry.coordinates = function (x0, y0, distance, angle) {
    /* Returns the location of a point by rotating around origin (x0,y0).
     */
    var x1 = x0 + Math.cos(g.math.radians(angle)) * distance;
    var y1 = y0 + Math.sin(g.math.radians(angle)) * distance;
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
    var odd = false;
    var n = points.length;
    for (var i=0; i < n; i++) {
        var j = (i<n-1)? i+1 : 0;
        var x0 = points[i].x;
        var y0 = points[i].y;
        var x1 = points[j].x;
        var y1 = points[j].y;
        if ((y0 < y && y1 >= y) || (y1 < y && y0 >= y)) {
            if (x0 + (y-y0) / (y1-y0) * (x1-x0) < x) {
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
    return _.reduce(values, function(a, b) { return a+b }); // sum
};

g.bezier.linePoint = function (t, x0, y0, x1, y1) {
    /* Returns coordinates for the point at t (0.0-1.0) on the line.
     */
    var x = x0 + t * (x1 - x0);
    var y = y0 + t * (y1 - y0);
    return g.lineTo(x, y);
};

g.bezier.lineLength = function (x0, y0, x1, y1) {
    /* Returns the length of the line.
     */
    var a = Math.pow(Math.abs(x0 - x1), 2);
    var b = Math.pow(Math.abs(y0 - y1), 2);
    return Math.sqrt(a + b);
};

g.bezier.curvePoint = function (t, x0, y0, x1, y1, x2, y2, x3, y3, handles) {
    /* Returns coordinates for the point at t (0.0-1.0) on the curve
     * (de Casteljau interpolation algorithm).
     */
    var dt = 1 - t;
    var x01 = x0*dt + x1*t;
    var y01 = y0*dt + y1*t;
    var x12 = x1*dt + x2*t;
    var y12 = y1*dt + y2*t;
    var x23 = x2*dt + x3*t;
    var y23 = y2*dt + y3*t;

    var h1x = x01*dt + x12*t;
    var h1y = y01*dt + y12*t;
    var h2x = x12*dt + x23*t;
    var h2y = y12*dt + y23*t;
    var x = h1x*dt + h2x*t;
    var y = h1y*dt + h2y*t;

    return g.curveto(h1x, h1y, h2x, h2y, x, y);
/*    if (!handles) {
        return {x, y, h1x, h1y, h2x, h2y];
    } else {
        // Include the new handles of pt0 and pt3 (see g.bezier.insert_point()).
        return [x, y, h1x, h1y, h2x, h2y, x01, y01, x23, y23];
    }*/
};

g.bezier.curveLength = function(x0, y0, x1, y1, x2, y2, x3, y3, n) {
    /* Returns the length of the curve.
     * Integrates the estimated length of the cubic bezier spline defined by x0, y0, ... x3, y3,
     * by adding up the length of n linear lines along the curve.
     */
    if (n == null) n = 20;
    var length = 0;
    var xi = x0;
    var yi = y0;
    for (var i=0; i < n; i++) {
        var t = (i+1) / n;
        var pe = g.bezier.curvePoint(t, x0, y0, x1, y1, x2, y2, x3, y3);
        length += Math.sqrt(
            Math.pow(Math.abs(xi-pe.point.x), 2) +
            Math.pow(Math.abs(yi-pe.point.y), 2)
        );
        xi = pe.point.x;
        yi = pe.point.y;
    }
    return length;
};

// BEZIER PATH LENGTH:

g.bezier.segmentLengths = function(pathElements, relative, n) {
    /* Returns an array with the length of each segment in the path.
     * With relative=true, the total length of all segments is 1.0.
     */
    if (n == null) n = 20;
    var lengths = [];
    for (var i=0; i < pathElements.length; i++) {
        var el = pathElements[i];
        var cmd = el.cmd;
        var pt = el.point;

        if (i === 0) {
            var close_x = pt.x;
            var close_y = pt.y;
        } else if (cmd === g.MOVETO) {
            var close_x = pt.x;
            var close_y = pt.y;
            lengths.push(0.0);
        } else if (cmd === g.CLOSE) {
            lengths.push(g.bezier.lineLength(x0, y0, close_x, close_y));
        } else if (cmd === g.LINETO) {
            lengths.push(g.bezier.lineLength(x0, y0, pt.x, pt.y));
        } else if (cmd === g.CURVETO) {
            lengths.push(g.bezier.curveLength(x0, y0, el.ctrl1.x, el.ctrl1.y, el.ctrl2.x, el.ctrl2.y, pt.x, pt.y, n));
        }
        if (cmd !== g.CLOSE) {
            var x0 = pt.x;
            var y0 = pt.y;
        }
    }
    if (relative === true) {
        var s = g.bezier.sum(lengths); // sum
        if (s > 0) {
            return _.map(lengths, function(v) { return v/s; });
        } else {
            return _.map(lengths, function(v) { return 0.0; });
        }
    }
    return lengths;
};

g.bezier.length = function(path, segmented, n) {
    /* Returns the approximate length of the path.
     * Calculates the length of each curve in the path using n linear samples.
     * With segmented=true, returns an array with the relative length of each segment (sum=1.0).
     */
    if (n == null) n = 20;
    if (!segmented) {
        return g.bezier.sum(g.bezier.segmentLengths(path.elements, false, n));
    } else {
        return g.bezier.segmentLengths(path.elements, true, n);
    }
};


// BEZIER PATH POINT:

g.bezier._locate = function(path, t, segments) {
    /* For a given relative t on the path (0.0-1.0), returns an array [index, t, PathElement],
     * with the index of the PathElement before t,
     * the absolute time on this segment,
     * the last MOVETO or any subsequent CLOSETO after i.
    */
    // Note: during iteration, supplying segmentLengths() yourself is 30x faster.
    if (segments == null) segments = g.bezier.segmentLengths(path.elements, true);
    for (var i=0; i < path.elements.length; i++) {
        var el = path.elements[i];
        if (i === 0 || el.cmd === g.MOVETO) {
            var closeto = g.makePoint(el.point.x, el.point.y);
        }
        if (t <= segments[i] || i == segments.length-1) {
            break;
        }
        t -= segments[i];
    }
    if (segments[i] !== 0) t /= segments[i];
    if (i === segments.length-1 && segments[i] === 0) i -= 1;
    return [i, t, closeto];
};


g.bezier.point = function(path, t, segments) {
    /* Returns the DynamicPathElement at time t on the path.
     * Note: in PathElement, ctrl1 is how the curve started, and ctrl2 how it arrives in this point.
     * Here, ctrl1 is how the curve arrives, and ctrl2 how it continues to the next point.
     */
    var _, i, closeto; _= g.bezier._locate(path, t, segments); i=_[0]; t=_[1]; closeto=_[2];
    var x0 = path.elements[i].point.x;
    var y0 = path.elements[i].point.y;
    var pe = path.elements[i+1];
    if (pe.cmd === g.LINETO || pe.cmd === g.CLOSE) {
        pe = (pe.cmd === g.CLOSE)?
             g.bezier.linePoint(t, x0, y0, closeto.x, closeto.y) :
             g.bezier.linePoint(t, x0, y0, pe.point.x, pe.point.y);
    } else if (pe.cmd === g.CURVETO) {
        pe = g.bezier.curvePoint(t, x0, y0, pe.ctrl1.x, pe.ctrl1.y, pe.ctrl2.x, pe.ctrl2.y, pe.point.x, pe.point.y);
    }
    return pe;
};



g.makePoint = function (x, y) {
    return Object.freeze({ x: x, y: y });
};

g.MOVETO  = "M";
g.LINETO  = "L";
g.CURVETO = "C";
g.CLOSE   = "z";

g.ZERO = new g.makePoint(0, 0);
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
    var k = 0.55; // kappa = (-1 + sqrt(2)) / 3 * 4
    var dx = k * 0.5 * width;
    var dy = k * 0.5 * height;
    var x0 = x + 0.5 * width;
    var y0 = y + 0.5 * height;
    var x1 = x + width;
    var y1 = y + height;
    return Object.freeze({ elements: [
        g.moveto(x, y0),
        g.curveto(x, y0-dy, x0-dx, y, x0, y),
        g.curveto(x0+dx, y, x1, y0-dy, x1, y0),
        g.curveto(x1, y0+dy, x0+dx, y1, x0, y1),
        g.curveto(x0-dx, y1, x, y0+dy, x, y0),
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
    var elements = (pe.elements) ? pe.elements : pe;
    return Object.freeze({
        elements: elements,
        fill: fill,
        stroke: stroke,
        strokeWidth: strokeWidth
    });
};

g.getContours = function (path) {
    var contours = [];
    var currentContour = [];
    _.each(path.elements, function(el) {
        if (el.cmd === g.MOVETO) {
            if (! _.isEmpty(currentContour))
                contours.push(currentContour);
            currentContour = [el];
        } else {
            currentContour.push(el);
        }
    });

    if (! _.isEmpty(currentContour))
        contours.push(currentContour);

    return Object.freeze(contours);
};

g.point = function(path, t, segments) {
    /* Returns the DynamicPathElement at time t (0.0-1.0) on the path.
     */
    if (segments == null) {
        // Cache the segment lengths for performace.
        segments = g.bezier.length(path, true, 10);
    }
    return g.bezier.point(path, t, segments);
};

g.points = function(path, amount, options) {
    /* Returns an array of DynamicPathElements along the path.
     * To omit the last point on closed paths: {end: 1-1.0/amount}
     */
    var start = (options && options.start !== undefined)? options.start : 0.0;
    var end = (options && options.end !== undefined)? options.end : 1.0;
    if (path.elements.length == 0) {
        // Otherwise g.bezier.point() will raise an error for empty paths.
        return [];
    }
    amount = Math.round(amount);
    // The delta value is divided by amount-1, because we also want the last point (t=1.0)
    // If we don't use amount-1, we fall one point short of the end.
    // If amount=4, we want the point at t 0.0, 0.33, 0.66 and 1.0.
    // If amount=2, we want the point at t 0.0 and 1.0.
    var d = (amount > 1)? (end-start) / (amount-1) : (end-start);
    var a = [];
    var segments = g.bezier.length(path, true, 10);

    for (var i=0; i < amount; i++) {
        a.push(g.point(path, start + d*i, segments));
    }
    return a;
};

g.length = function (path, precision) {
    /* Returns an approximation of the total length of the path.
     */
    if (precision === undefined) precision = 10;
    return g.bezier.length(path, false, precision);
};

g.pathContains = function (path, x, y, precision) {
    /* Returns true when point (x,y) falls within the contours of the path.
     */
    if (precision === undefined) precision = 100;
    var polygon = g.points(path, precision);
    var points = [];
    for (var i=0; i<polygon.length; i++) {
        if (polygon[i].cmd !== g.CLOSE)
            points.push(polygon[i].point);
    }
//    if (this._polygon == null ||
//        this._polygon[1] != precision) {
//        this._polygon = [this.points(precision), precision];
//    }
    return g.geometry.pointInPolygon(points, x, y);
};


g.makeRect = function (x, y, width, height) {
  return Object.freeze({ x: x, y: y, width: width, height: height });
};

g.rectContains = function (rect, x, y) {
    return (x >= rect.x && x <= rect.x + rect.width && y >= rect.y && y <= rect.y + rect.height);
};

g.makeCenteredRect = function (cx, cy, width, height) {
    var x = cx - width / 2;
    var y = cy - height / 2;
    return g.makeRect(x, y, width, height);
};

g.normalizedRect = function (rect) {
    var x = rect.x;
    var y = rect.y;
    var width = rect.width;
    var height = rect.height;

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
    if (y == null) y = x;
    return g._mmult([x, 0, 0, 0, y, 0, 0, 0, 1], matrix);
};

g.translate = function (matrix, x, y) {
    return g._mmult([1, 0, 0, 0, 1, 0, x, y, 1], matrix);
};

g.rotate = function (matrix, angle) {
    var c = Math.cos(g.math.radians(angle));
    var s = Math.sin(g.math.radians(angle));
    return g._mmult([c, s, 0, -s, c, 0, 0, 0, 1], matrix);
};

g.transformPoint = function (point, matrix) {
    /* Returns the new coordinates of the given point (x,y) after transformation.
     */
    var x = point.x;
    var y = point.y;
    return g.makePoint(
        x * matrix[0] + y * matrix[3] + matrix[6],
        x * matrix[1] + y * matrix[4] + matrix[7]
    );
};

g.transformPath = function (path, matrix) {
    var elements = _.map(path.elements, function(pe) {
        if (pe.cmd === g.CLOSE) return pe;
        else if (pe.cmd === g.MOVETO) {
            return Object.freeze({ cmd: g.MOVETO,
                                 point: g.transformPoint(pe.point, matrix) });
        } else if (pe.cmd === g.LINETO) {
            return Object.freeze({ cmd: g.LINETO,
                                 point: g.transformPoint(pe.point, matrix) });
        } else if (pe.cmd === g.CURVETO) {
            return Object.freeze({ cmd: g.CURVETO,
                                 point: g.transformPoint(pe.point, matrix),
                                 ctrl1: g.transformPoint(pe.ctrl1, matrix),
                                 ctrl2: g.transformPoint(pe.ctrl2, matrix) });
        };
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
    var R,G,B,A;
    if (c != null) {
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
            $.each(shape.elements, function (i, command) {
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
