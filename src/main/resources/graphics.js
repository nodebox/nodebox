/*jslint sloppy:true, nomen:true, bitwise:true, regexp:true */
/*global _,Uint8Array,Float32Array,console  */

/*---  Based on: canvas.js, https://github.com/clips/pattern/blob/master/pattern/canvas.js (BSD)
  ---  De Smedt T. & Daelemans W. (2012). Pattern for Python. Journal of Machine Learning Research. --*/

// The Java rhino engine doesn't know the following variables, so to not break compatibility with the Java version of NodeBox,
// we provide a fallback mechanism.
if (typeof Uint8Array === 'undefined') {
    var Uint8Array = Array;
}

if (typeof Float32Array === 'undefined') {
    var Float32Array = Array;
}

if (Object.freeze === undefined) {
    Object.freeze = function (o) {
        return o;
    };
}

if (Object.isFrozen === undefined) {
    Object.isFrozen = function (o) {
        return false;
    };
}

var g = {};

// deepFreeze code from https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Object/freeze
g.deepFreeze = function (o) {
    var prop, propKey;
    Object.freeze(o); // First freeze the object.
    for (propKey in o) {
        if (o.hasOwnProperty(propKey)) {
            prop = o[propKey];
            if (prop instanceof Object && !Object.isFrozen(prop)) {
                // If the object is on the prototype, not an object, or is already frozen,
                // skip it. Note that this might leave an unfrozen reference somewhere in the
                // object if there is an already frozen object containing an unfrozen object.
                g.deepFreeze(prop); // Recursively call deepFreeze.
            }
        }
    }
    return o;
};

g.frozen = function (fn) {
    return function () {
        var res = fn.apply(null, arguments);
        return g.deepFreeze(res);
    };
};

// Generate a random function that is seeded with the given value.
g.randomGenerator = function (seed) {
    // Based on random number generator from
    // http://indiegamr.com/generate-repeatable-random-numbers-in-js/
    return function (min, max) {
        min = min || 0;
        max = max || 1;
        seed = (seed * 9301 + 49297) % 233280;
        var v = seed / 233280;
        return min + v * (max - min);
    };
};

/*--- MATH UTILITIES ---------------------------------------------------------------------------------*/

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

// Compute fade curve for point t.
g.math._fade = function (t) {
    return t * t * t * (t * (t * 6 - 15) + 10);
};


// Linearly interpolate between a and b.
g.math._lerp = function (t, a, b) {
    return a + t * (b - a);
};

// Convert low 4 bits of hash code into 12 gradient directions.
g.math._grad = function (hash, x, y, z) {
    var h, u, v;
    h = hash & 15;
    u = h < 8 ? x : y;
    v = h < 4 ? y : h === 12 || h === 14 ? x : z;
    return ((h & 1) === 0 ? u : -u) + ((h & 2) === 0 ? v : -v);
};

g.math._scale = function (n) {
    return (1 + n) / 2;
};

g._permutation = (function () {
    var permutation, p, i;
    permutation = [ 151, 160, 137, 91, 90, 15,
        131, 13, 201, 95, 96, 53, 194, 233, 7, 225, 140, 36, 103, 30, 69, 142, 8, 99, 37, 240, 21, 10, 23,
        190, 6, 148, 247, 120, 234, 75, 0, 26, 197, 62, 94, 252, 219, 203, 117, 35, 11, 32, 57, 177, 33,
        88, 237, 149, 56, 87, 174, 20, 125, 136, 171, 168, 68, 175, 74, 165, 71, 134, 139, 48, 27, 166,
        77, 146, 158, 231, 83, 111, 229, 122, 60, 211, 133, 230, 220, 105, 92, 41, 55, 46, 245, 40, 244,
        102, 143, 54, 65, 25, 63, 161, 1, 216, 80, 73, 209, 76, 132, 187, 208, 89, 18, 169, 200, 196,
        135, 130, 116, 188, 159, 86, 164, 100, 109, 198, 173, 186, 3, 64, 52, 217, 226, 250, 124, 123,
        5, 202, 38, 147, 118, 126, 255, 82, 85, 212, 207, 206, 59, 227, 47, 16, 58, 17, 182, 189, 28, 42,
        223, 183, 170, 213, 119, 248, 152, 2, 44, 154, 163, 70, 221, 153, 101, 155, 167, 43, 172, 9,
        129, 22, 39, 253, 19, 98, 108, 110, 79, 113, 224, 232, 178, 185, 112, 104, 218, 246, 97, 228,
        251, 34, 242, 193, 238, 210, 144, 12, 191, 179, 162, 241, 81, 51, 145, 235, 249, 14, 239, 107,
        49, 192, 214, 31, 181, 199, 106, 157, 184, 84, 204, 176, 115, 121, 50, 45, 127, 4, 150, 254,
        138, 236, 205, 93, 222, 114, 67, 29, 24, 72, 243, 141, 128, 195, 78, 66, 215, 61, 156, 180];

    p = new Uint8Array(512);
    for (i = 0; i < 256; i += 1) {
        p[256 + i] = p[i] = permutation[i];
    }
    return p;
}());

// Calculate Perlin noise
g.noise = function (x, y, z) {
    var fade, lerp, grad, scale, p, X, Y, Z, u, v, w, A, AA, AB, B, BA, BB;
    fade = g.math._fade;
    lerp = g.math._lerp;
    grad = g.math._grad;
    scale = g.math._scale;
    p = g._permutation;

    // Find unit cube that contains the point.
    X = Math.floor(x) & 255;
    Y = Math.floor(y) & 255;
    Z = Math.floor(z) & 255;
    // Find relative x, y, z point in the cube.
    x -= Math.floor(x);
    y -= Math.floor(y);
    z -= Math.floor(z);
    // Compute fade curves for each x, y, z.
    u = fade(x);
    v = fade(y);
    w = fade(z);

    // Hash coordinates of the 8 cube corners.
    A = p[X] + Y;
    AA = p[A] + Z;
    AB = p[A + 1] + Z;
    B = p[X + 1] + Y;
    BA = p[B] + Z;
    BB = p[B + 1] + Z;

    // Add blended results from 8 corners of the cube.
    return scale(lerp(w, lerp(v, lerp(u, grad(p[AA], x, y, z),
        grad(p[BA], x - 1, y, z)),
        lerp(u, grad(p[AB], x, y - 1, z),
            grad(p[BB], x - 1, y - 1, z))),
        lerp(v, lerp(u, grad(p[AA + 1], x, y, z - 1),
            grad(p[BA + 1], x - 1, y, z - 1)),
            lerp(u, grad(p[AB + 1], x, y - 1, z - 1),
                grad(p[BB + 1], x - 1, y - 1, z - 1)))));
};


/*--- GEOMETRY -------------------------------------------------------------------------------------*/

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
    var x = x0 + Math.cos(g.math.radians(angle)) * distance,
        y = y0 + Math.sin(g.math.radians(angle)) * distance;
    return new g.Point(x, y);
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
    return g.lineto(x, y);
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

g.bezier.segmentLengths = g.frozen(g.bezier.segmentLengths);

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

g.bezier._locate = g.frozen(g.bezier._locate);

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


/*--- GRAPHICS -------------------------------------------------------------------------------------*/

g.Point = g.Vec2 = function (x, y) {
    this.x = x;
    this.y = y;
    Object.freeze(this);
};

g.Point.ZERO = new g.Point(0, 0);

g.Point.prototype.add = function (v) {
    return new g.Point(this.x + v.x, this.y + v.y);
};

g.Point.prototype.subtract = g.Point.prototype.sub = function (v) {
    return new g.Point(this.x - v.x, this.y - v.y);
};

g.Point.prototype.divide = function (n) {
    return new g.Point(this.x / n, this.y / n);
};

g.Point.prototype.multiply = function (n) {
    return new g.Point(this.x * n, this.y * n);
};

g.Point.prototype.magnitude = function () {
    return Math.sqrt(this.x * this.x + this.y * this.y);
};

g.Point.prototype.magnitudeSquared = function () {
    return this.x * this.x + this.y * this.y;
};

g.Point.prototype.heading = function () {
    return Math.atan2(this.y, this.x);
};

g.Vec2.prototype.distanceTo = function (v) {
    var dx = this.x - v.x,
        dy = this.y - v.y;
    return Math.sqrt(dx * dx + dy * dy);
};

g.Point.prototype.normalize = function () {
    var m = this.magnitude();
    if (m !== 0) {
        return this.divide(m);
    } else {
        return g.Point.ZERO;
    }
};

g.Point.prototype.limit = function (speed) {
    if (this.magnitudeSquared() > speed * speed) {
        return this.normalize().multiply(speed);
    }
    return this;
};

g.Point.prototype.translate = function (tx, ty) {
    return new g.Point(this.x + tx, this.y + ty);
};

g.Point.prototype.scale = function (sx, sy) {
    sy = sy !== undefined ? sy : sx;
    return new g.Point(this.x * sx, this.y * sy);
};

g.Point.prototype.toString = function () {
    return '[' + this.x + ', ' + this.y + ']';
};

g.makePoint = function (x, y) {
    return new g.Point(x, y);
};

g.Particle = function (position, velocity, acceleration, mass, lifespan) {
    this.position = position !== undefined ? position : g.Point.ZERO;
    this.velocity = velocity !== undefined ? velocity : g.Point.ZERO;
    this.acceleration = acceleration !== undefined ? acceleration : g.Point.ZERO;
    this.mass = mass !== undefined ? mass : 1;
    this.lifespan = lifespan !== undefined ? lifespan : Number.POSITIVE_INFINITY;
    g.deepFreeze(this);
};

g.MOVETO  = "M";
g.LINETO  = "L";
g.CURVETO = "C";
g.CLOSE   = "z";

g.CLOSE_ELEMENT = Object.freeze({ cmd: g.CLOSE });

g.moveto = function (x, y) {
    return { cmd:   g.MOVETO,
           point: g.makePoint(x, y) };
};

g.moveTo = g.moveto = g.frozen(g.moveto);

g.lineto = function (x, y) {
    return { cmd:   g.LINETO,
           point: g.makePoint(x, y) };
};

g.lineTo = g.lineto = g.frozen(g.lineto);

g.curveTo = g.curveto = function (c1x, c1y, c2x, c2y, x, y) {
    return { cmd:   g.CURVETO,
           point: g.makePoint(x, y),
           ctrl1: g.makePoint(c1x, c1y),
           ctrl2: g.makePoint(c2x, c2y) };
};

g.curveTo = g.curveto = g.frozen(g.curveto);

g.closePath = g.closepath = g.close = function () {
    return g.CLOSE_ELEMENT;
};

g._rect = function (x, y, width, height) {
    var elements = [
        g.moveto(x, y),
        g.lineto(x + width, y),
        g.lineto(x + width, y + height),
        g.lineto(x, y + height),
        g.close()
    ];
    return new g.Path(elements);
};

g.roundedRect = function (cx, cy, width, height, rx, ry) {
    var ONE_MINUS_QUARTER = 1.0 - 0.552,

        elements = [],

        dx = rx,
        dy = ry,

        left = cx,
        right = cx + width,
        top = cy,
        bottom = cy + height;

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
    elements.push(g.close());
    return new g.Path(elements);
};

g._ellipse = function (x, y, width, height) {
    var k = 0.55, // kappa = (-1 + sqrt(2)) / 3 * 4
        dx = k * 0.5 * width,
        dy = k * 0.5 * height,
        x0 = x + 0.5 * width,
        y0 = y + 0.5 * height,
        x1 = x + width,
        y1 = y + height,
        elements = [
            g.moveto(x, y0),
            g.curveto(x, y0 - dy, x0 - dx, y, x0, y),
            g.curveto(x0 + dx, y, x1, y0 - dy, x1, y0),
            g.curveto(x1, y0 + dy, x0 + dx, y1, x0, y1),
            g.curveto(x0 - dx, y1, x, y0 + dy, x, y0),
            g.close()
        ];
    return new g.Path(elements);
};

g._line = function (x1, y1, x2, y2) {
    var elements = [
        g.moveto(x1, y1),
        g.lineto(x2, y2)
    ];
    return new g.Path(elements);
};

g.quad = function (x1, y1, x2, y2, x3, y3, x4, y4) {
    var elements = [
        g.moveto(x1, y1),
        g.lineto(x2, y2),
        g.lineto(x3, y3),
        g.lineto(x4, y4),
        g.close()
    ];
    return new g.Path(elements);
};

g._arc = function (x, y, width, height, startAngle, degrees, arcType) {
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
                g.moveto(x + Math.cos(angle) * w,
                         y + Math.sin(angle) * h)
            );
        } else if (index > arcSegs) {
            if (index === arcSegs + lineSegs) {
                elements.push(g.close());
            } else {
                elements.push(g.lineto(x, y));
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
            elements.push(g.curveto.apply(null, coords));
        }
        index += 1;
    }

    return new g.Path(elements);
};

g.Path = function (p, attrs) {
    if (p === undefined) {
        this.elements = [];
    } else {
        this.elements = p.elements || p;
    }
    var key;
    if (attrs) {
        for (key in attrs) {
            if (attrs[key] !== undefined) {
                this[key] = attrs[key];
            }
        }
    }
    g.deepFreeze(this);
};

g.Path.prototype.extend = function (p) {
    var addElements = p.elements || p,
        elements = this.elements.concat(addElements),
        attrs = {
            fill: this.fill,
            stroke: this.stroke,
            strokeWidth: this.strokeWidth,
            _bounds: this._bounds,
            _length: this._length
        };
    return new g.Path(elements, attrs);
};

g.Path.prototype.moveTo = function (x, y) {
    return this.extend(g.moveto(x, y));
};

g.Path.prototype.lineTo = function (x, y) {
    return this.extend(g.lineto(x, y));
};

g.Path.prototype.curveTo = function (c1x, c1y, c2x, c2y, x, y) {
    return this.extend(g.curveto(c1x, c1y, c2x, c2y, x, y));
};

g.Path.prototype.closePath = function () {
    return this.extend(g.closePath());
};

g.Path.prototype.isClosed = function () {
    if (_.isEmpty(this.elements)) { return false; }
    return this.elements[this.elements.length - 1].cmd === g.CLOSE;
};

g.Path.prototype.rect = function (x, y, width, height) {
    return this.extend(g._rect(x, y, width, height));
};

g.Path.prototype.roundedRect = function (cx, cy, width, height, rx, ry) {
    return this.extend(g.roundedRect(cx, cy, width, height, rx, ry));
};

g.Path.prototype.ellipse = function (x, y, width, height) {
    return this.extend(g._ellipse(x, y, width, height));
};

g.Path.prototype.line = function (x1, y1, x2, y2) {
    return this.extend(g._line(x1, y1, x2, y2));
};

g.Path.prototype.colorize = function (fill, stroke, strokeWidth) {
    var attrs = {
        fill: fill,
        stroke: stroke,
        strokeWidth: strokeWidth,
        _bounds: this._bounds,
        _length: this._length
    };
    return new g.Path(this.elements, attrs);
};

g.Path.prototype.contours = function () {
    var contours = [],
        currentContour = [];
    _.each(this.elements, function (el) {
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

    return contours;
};

g.Path.prototype.bounds = function () {
    if (this._bounds) { return this._bounds; }
    if (_.isEmpty(this.elements)) { return g.makeRect(0, 0, 0, 0); }

    var px, py, prev, right, bottom,
        minX = Number.MAX_VALUE,
        minY = Number.MAX_VALUE,
        maxX = -(Number.MAX_VALUE),
        maxY = -(Number.MAX_VALUE);

    _.each(this.elements, function (el) {
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

g.Path.prototype.point = function (t, segments) {
    /* Returns the DynamicPathElement at time t (0.0-1.0) on the path.
     */
    if (segments === undefined) {
        // Cache the segment lengths for performace.
        segments = g.bezier.length(this, true, 10);
    }
    return g.bezier.point(this, t, segments);
};

g.Path.prototype.points = function (amount, options) {
    /* Returns an array of DynamicPathElements along the path.
     * To omit the last point on closed paths: {end: 1-1.0/amount}
     */
    var d, a, i, segments,
        start = (options && options.start !== undefined) ? options.start : 0.0,
        end = (options && options.end !== undefined) ? options.end : 1.0;
    if (this.elements.length === 0) {
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
    segments = g.bezier.length(this, true, 10);

    for (i = 0; i < amount; i += 1) {
        a.push(this.point(start + d * i, segments));
    }
    return Object.freeze(a);
};

g.Path.prototype.length = function (precision) {
    /* Returns an approximation of the total length of the path.
     */
    if (precision === undefined) { precision = 10; }
    return g.bezier.length(this, false, precision);
};

g.Path.prototype.contains = function (x, y, precision) {
    /* Returns true when point (x,y) falls within the contours of the path.
     */
    if (precision === undefined) { precision = 100; }
    var i, polygon = this.points(precision),
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

g.Path.prototype.resampleByAmount = function (points, perContour) {
    var i, j, subPath, pts, elem,
        subPaths = perContour ? this.contours() : [this.elements],
        elems = [];

    function getPoint(pe) {
        return pe.point;
    }

    for (j = 0; j < subPaths.length; j += 1) {
        subPath = g.makePath(subPaths[j]);
        pts = _.map(subPath.points(points + 1), getPoint);
        for (i = 0; i < pts.length - 1; i += 1) {
            elem = { cmd:   (i === 0) ? g.MOVETO : g.LINETO,
                   point: pts[i] };
            elems.push(elem);
        }
        elems.push(g.closePath());
    }
    return g.makePath(elems, this.fill, this.stroke, this.strokeWidth);
};

g.Path.prototype.resampleByLength = function (segmentLength) {
    var i, subPath, contourLength, amount,
        subPaths = this.contours(),
        elems = [];
    for (i = 0; i < subPaths.length; i += 1) {
        subPath = g.makePath(subPaths[i]);
        contourLength = subPath.length();
        amount = Math.ceil(contourLength / segmentLength);
        if (!subPath.isClosed()) { amount += 1; }
        elems = elems.concat(subPath.resampleByAmount(amount, false).elements);
    }
    return g.makePath(elems, this.fill, this.stroke, this.strokeWidth);
};

g.Path.prototype.toPathData = function () {
    var i, d, pe, x, y, x1, y1, x2, y2;
    d = '';
    for (i = 0; i < this.elements.length; i += 1) {
        pe = this.elements[i];
        if (pe.point) {
            x = g.clamp(pe.point.x, -9999, 9999);
            y = g.clamp(pe.point.y, -9999, 9999);
        }
        if (pe.ctrl1) {
            x1 = g.clamp(pe.ctrl1.x, -9999, 9999);
            y1 = g.clamp(pe.ctrl1.y, -9999, 9999);
        }
        if (pe.ctrl2) {
            x2 = g.clamp(pe.ctrl2.x, -9999, 9999);
            y2 = g.clamp(pe.ctrl2.y, -9999, 9999);
        }
        if (pe.cmd === g.MOVETO) {
            if (!isNaN(x) && !isNaN(y)) {
                d += 'M' + x + ' ' + y;
            }
        } else if (pe.cmd === g.LINETO) {
            if (!isNaN(x) && !isNaN(y)) {
                d += 'L' + x + ' ' + y;
            }
        } else if (pe.cmd === g.CURVETO) {
            if (!isNaN(x) && !isNaN(y) && !isNaN(x1) && !isNaN(y1) && !isNaN(x2) && !isNaN(y2)) {
                d += 'C' + x1 + ' ' + y1 + ' ' + x2 + ' ' + y2 + ' ' + x + ' ' + y;
            }
        } else if (pe.cmd === g.CLOSE) {
            d += 'Z';
        }
    }
    return d;
};

// Output the path as an SVG string.
g.Path.prototype.toSVG = function () {
    var svg = '<path d="';
    svg += this.toPathData();
    svg += '"';
/*    if (this.fill !== 'black') {
        if (this.fill === null) {
            svg += ' fill="none"';
        } else {
            svg += ' fill="' + this.fill + '"';
        }
    }
    if (this.stroke) {
        svg += ' stroke="' + this.stroke + '" stroke-width="' + this.strokeWidth + '"';
    } */
    svg += '/>';
    return svg;
};

// Draw the path to a 2D context.
g.Path.prototype.draw = function (ctx) {
    var nElements, i, pe;
    ctx.beginPath();
    nElements = this.elements.length;
    for (i = 0; i < nElements; i += 1) {
        pe = this.elements[i];
        if (pe.cmd === g.MOVETO) {
            ctx.moveTo(pe.point.x, pe.point.y);
        } else if (pe.cmd === g.LINETO) {
            ctx.lineTo(pe.point.x, pe.point.y);
        } else if (pe.cmd === g.CURVETO) {
            ctx.bezierCurveTo(pe.ctrl1.x, pe.ctrl1.y, pe.ctrl2.x, pe.ctrl2.y, pe.point.x, pe.point.y);
        } else if (pe.cmd === g.CLOSE) {
            ctx.closePath();
        }
    }
    if (this.fill !== null) {
        ctx.fillStyle = g._getColor(this.fill);
        ctx.fill();
    }
    if (this.stroke !== null && this.strokeWidth !== null && this.strokeWidth > 0) {
        ctx.strokeStyle = g._getColor(this.stroke);
        ctx.lineWidth = this.strokeWidth;
        ctx.stroke();
    }
};

g.makePath = function (pe, fill, stroke, strokeWidth) {
    var attrs = {
        fill: fill,
        stroke: stroke,
        strokeWidth: strokeWidth
    };
    return new g.Path(pe, attrs);
};

g.Group = function (shapes) {
    if (!shapes) {
        this.shapes = [];
    } else if (shapes.shapes || shapes.elements) {
        this.shapes = [shapes];
    } else if (shapes) {
        this.shapes = shapes;
    }
    g.deepFreeze(this);
};

g.Group.prototype.colorize = function (fill, stroke, strokeWidth) {
    var shapes = _.map(this.shapes, function (shape) {
        return shape.colorize(fill, stroke, strokeWidth);
    });
    return g.makeGroup(shapes);
};

g.Group.prototype.bounds = function () {
    if (_.isEmpty(this.shapes)) { return g.makeRect(0, 0, 0, 0); }
    var i, r, shape,
        shapes = this.shapes;
    for (i = 0; i < shapes.length; i += 1) {
        shape = shapes[i];
        if (r === undefined) {
            r = shape.bounds();
        }
        if ((shape.shapes && !_.isEmpty(shape.shapes)) ||
                (shape.elements && !_.isEmpty(shape.elements))) {
            r = r.unite(shape.bounds());
        }
    }
    return (r !== undefined) ? r : g.makeRect(0, 0, 0, 0);
};

g.Group.prototype.contains = function (x, y, precision) {
    /* Returns true when point (x,y) falls within the contours of the group.
     */
    if (precision === undefined) { precision = 100; }
    var i, shapes = this.shapes;
    for (i = 0; i < shapes.length; i += 1) {
        if (shapes[i].contains(x, y, precision)) {
            return true;
        }
    }
    return false;
};

g.Group.prototype.resampleByAmount = function (points, perContour) {
    var path, shapes;
    if (!perContour) {
        path = g.makePath(g.combinePaths(this));
        return path.resampleByAmount(points, perContour);
    }

    shapes = _.map(this.shapes, function (shape) {
        return shape.resampleByAmount(points, perContour);
    });
    return g.makeGroup(shapes);
};

g.Group.prototype.resampleByLength = function (length) {
    var shapes = _.map(this.shapes, function (shape) {
        return shape.resampleByLength(length);
    });
    return g.makeGroup(shapes);
};

g.Group.prototype.toSVG = function () {
    var l;
    l = _.map(this.shapes, function (shape) {
        return shape.toSVG();
    });
    return '<g>' + l.join('') + '</g>';
};

// Draw the group to a 2D context.
g.Group.prototype.draw = function (ctx) {
    var i, shapes = this.shapes, nShapes = shapes.length;
    for (i = 0; i < nShapes; i += 1) {
        shapes[i].draw(ctx);
    }
};

g.makeGroup = g.group = function (shapes) {
    return new g.Group(shapes);
};

// Combine all given shape arguments into a new group.
// This function works like makeGroup, except that this can take any number
// of arguments.
g.merge = function () {
    return g.makeGroup(arguments);
};

g.combinePaths = function (shape) {
    if (shape.elements) { return shape.elements; }
    var i, elements = [];
    for (i = 0; i < shape.shapes.length; i += 1) {
        elements = elements.concat(g.combinePaths(shape.shapes[i]));
    }
    return elements;
};

g.shapePoints = function (shape) {
    if (shape.elements) {
        return _.map(_.filter(shape.elements, function (el) { if (el.point) { return true; } return false; }), function (el) { return el.point; });
    }
    var i, points = [];
    for (i = 0; i < shape.shapes.length; i += 1) {
        points = points.concat(g.shapePoints(shape.shapes[i]));
    }
    return points;
};

g.combinePaths = g.frozen(g.combinePaths);

g.Rect = function (x, y, width, height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
    g.deepFreeze(this);
};

g.Rect.prototype.normalize = function () {
    var x = this.x,
        y = this.y,
        width = this.width,
        height = this.height;

    if (width < 0) {
        x += width;
        width = -width;
    }

    if (height < 0) {
        y += height;
        height = -height;
    }
    return new g.Rect(x, y, width, height);
};

g.Rect.prototype.containsPoint = function (x, y) {
    return (x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.height);
};

g.Rect.prototype.containsRect = function (r) {
    return r.x >= this.x && r.x + r.width <= this.x + this.width &&
        r.y >= this.y && r.y + r.height <= this.y + this.height;
};

g.Rect.prototype.grow = function (dx, dy) {
    var x = this.x - dx,
        y = this.y - dy,
        width = this.width + dx * 2,
        height = this.height + dy * 2;
    return new g.Rect(x, y, width, height);
};

g.Rect.prototype.unite = function (r) {
    var x = Math.min(this.x, r.x),
        y = Math.min(this.y, r.y),
        width = Math.max(this.x + this.width, r.x + r.width) - x,
        height = Math.max(this.y + this.height, r.y + r.height) - y;
    return new g.Rect(x, y, width, height);
};

g.Rect.prototype.addPoint = function (x, y) {
    var dx, dy,
        _x = this.x,
        _y = this.y,
        width = this.width,
        height = this.height;

    if (x < this.x) {
        dx = this.x - x;
        _x = x;
        width += dx;
    } else if (x > this.x + this.width) {
        dx = x - (this.x + this.width);
        width += dx;
    }
    if (y < this.y) {
        dy = this.y - y;
        _y = y;
        height += dy;
    } else if (y > this.y + this.height) {
        dy = y - (this.y + this.height);
        height += dy;
    }
    return new g.Rect(_x, _y, width, height);
};

g.Rect.prototype.centroid = function () {
    return new g.Point(this.x + this.width / 2, this.y + this.height / 2);
};

g.makeRect = function (x, y, width, height) {
    return new g.Rect(x, y, width, height);
};

g.makeCenteredRect = function (cx, cy, width, height) {
    var x = cx - width / 2,
        y = cy - height / 2;
    return new g.Rect(x, y, width, height);
};

g.RGB = "RGB";
g.HSB = "HSB";
g.HEX = "HEX";

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

Object.freeze(g._namedColors);

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

g.Color = function (R, G, B, A, options) {
    var rgb;
    if (R instanceof g.Color) {
        return R;
    } else if (R.r !== undefined && R.g !== undefined && R.b !== undefined && R.a !== undefined) {
        G = R.g;
        B = R.b;
        A = R.a;
        R = R.r;
    } else if (R instanceof Array) {
        G = R[1];
        B = R[2];
        A = R[3] || 1;
        R = R[0];
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
    this.r = R;
    this.g = G;
    this.b = B;
    this.a = A;
    g.deepFreeze(this);
};

g.Color.BLACK = new g.Color(0);
g.Color.WHITE = new g.Color(1);

g.Color.prototype.rgb = function () {
    return [this.r, this.g, this.b];
};

g.Color.prototype.rgba = function () {
    return [this.r, this.g, this.b, this.a];
};

g.Color.prototype._get = function () {
    var R = Math.round(this.r * 255),
        G = Math.round(this.g * 255),
        B = Math.round(this.b * 255);
    return "rgba(" + R + ", " + G + ", " + B + ", " + this.a + ")";
};

g.makeColor = function (R, G, B, A, options) {
    return new g.Color(R, G, B, A, options);
};


//// Three-dimensional vectors //////////////////////////////////////////////

g.Vec3 = function (x, y, z) {
    this.x = x === undefined ? 0 : x;
    this.y = y === undefined ? 0 : y;
    this.z = z === undefined ? 0 : z;
    g.deepFreeze(this);
};

// Generate the zero vector.
g.Vec3.ZERO = new g.Vec3(0, 0, 0);

g.Vec3.up = function () {
    return new g.Vec3(0, 1.0, 0);
};

// Generate the dot product of two vectors.
g.Vec3.dot = function (a, b) {
    return (a.x * b.x + a.y * b.y + a.z * b.z);
};

// Generate the cross product of two vectors.
g.Vec3.cross = function (a, b) {
    return new g.Vec3(
        a.y * b.z - a.z * b.y,
        a.z * b.x - a.x * b.z,
        a.x * b.y - a.y * b.x
    );
};

// Convert this vector to a string representation.
g.Vec3.prototype.toString = function () {
    return '[' + this.x + ', ' + this.y + ', ' + this.z + ']';
};

// Convert this vector to an array.
g.Vec3.prototype.toArray = function () {
    var array = [];
    array.push(this.x);
    array.push(this.y);
    array.push(this.z);
    return array;
};

// Calculate the length of this vector.
g.Vec3.prototype.getLength = function () {
    return Math.sqrt(this.x * this.x + this.y * this.y + this.z * this.z);
};

// Create a new vector that is this vector, normalized.
g.Vec3.prototype.normalize = function () {
    var len, c;
    len = this.getLength();
    if (len === 0) {
        return this;
    }
    c = 1.0 / len;
    return new g.Vec3(this.x * c, this.y * c, this.z * c);
};

// Create a new vector that is the addition of this vector and the given vector.
g.Vec3.prototype.add = function (o) {
    return new g.Vec3(this.x + o.x, this.y + o.y, this.z + o.z);
};

// Create a new vector that is the subtraction of this vector and the given vector.
g.Vec3.prototype.subtract = function (o) {
    return new g.Vec3(this.x - o.x, this.y - o.y, this.z - o.z);
};

// Transform the vector according to the matrix and return the result.
// A new vector is created, nothing is modified.
g.Vec3.prototype.transform = function (matrix4) {
    var x, y, z, w, matrix;

    matrix = matrix4;
    x = (this.x * matrix.m[0]) + (this.y * matrix.m[4]) + (this.z * matrix.m[8]) + matrix.m[12];
    y = (this.x * matrix.m[1]) + (this.y * matrix.m[5]) + (this.z * matrix.m[9]) + matrix.m[13];
    z = (this.x * matrix.m[2]) + (this.y * matrix.m[6]) + (this.z * matrix.m[10]) + matrix.m[14];
    w = (this.x * matrix.m[3]) + (this.y * matrix.m[7]) + (this.z * matrix.m[11]) + matrix.m[15];

    return new g.Vec3(x / w, y / w, z / w);
};


g.Matrix3 = g.Transform = function (m) {
    /* A geometric transformation in Euclidean space (i.e. 2D)
     * that preserves collinearity and ratio of distance between points.
     * Linear transformations include rotation, translation, scaling, shear.
     */
    if (m !== undefined) {
        this.m = m;
    } else {
        this.m = [1, 0, 0, 0, 1, 0, 0, 0, 1]; // Identity matrix.
    }
    g.deepFreeze(this);
};

g.Matrix3.IDENTITY = new g.Matrix3();

g.Matrix3._mmult = function (a, b) {
    /* Returns the 3x3 matrix multiplication of A and B.
     * Note that scale(), translate(), rotate() work with premultiplication,
     * e.g. the matrix A followed by B = BA and not AB.
     */
    if (a.m !== undefined) { a = a.m; }
    if (b.m !== undefined) { b = b.m; }

    return new g.Matrix3([
        a[0] * b[0] + a[1] * b[3],
        a[0] * b[1] + a[1] * b[4], 0,
        a[3] * b[0] + a[4] * b[3],
        a[3] * b[1] + a[4] * b[4], 0,
        a[6] * b[0] + a[7] * b[3] + b[6],
        a[6] * b[1] + a[7] * b[4] + b[7], 1
    ]);
};

g.Matrix3.prototype.prepend = function (matrix) {
    return g.Matrix3._mmult(this.m, matrix.m);
};

g.Matrix3.prototype.append = function (matrix) {
    return g.Matrix3._mmult(matrix.m, this.m);
};

g.Matrix3.prototype.inverse = function () {
    var m = this.m,
        d = m[0] * m[4] - m[1] * m[3];
    return new g.Matrix3([
        m[4] / d,
        -m[1] / d, 0,
        -m[3] / d,
        m[0] / d, 0,
        (m[3] * m[7] - m[4] * m[6]) / d,
        -(m[0] * m[7] - m[1] * m[6]) / d, 1
    ]);
};

g.Matrix3.prototype.scale = function (x, y) {
    if (y === undefined) { y = x; }
    return g.Matrix3._mmult([x, 0, 0, 0, y, 0, 0, 0, 1], this.m);
};

g.Matrix3.prototype.translate = function (x, y) {
    return g.Matrix3._mmult([1, 0, 0, 0, 1, 0, x, y, 1], this.m);
};

g.Matrix3.prototype.rotate = function (angle) {
    var c = Math.cos(g.math.radians(angle)),
        s = Math.sin(g.math.radians(angle));
    return g.Matrix3._mmult([c, s, 0, -s, c, 0, 0, 0, 1], this.m);
};

g.Matrix3.prototype.skew = function (x, y) {
    var kx = Math.PI * x / 180.0,
        ky = Math.PI * y / 180.0;
    return g.Matrix3._mmult([1, Math.tan(ky), 0, -Math.tan(kx), 1, 0, 0, 0, 1], this.m);
};

g.Matrix3.prototype.transformPoint = function (point) {
    /* Returns the new coordinates of the given point (x,y) after transformation.
     */
    var x = point.x,
        y = point.y,
        m = this.m;
    return new g.Point(
        x * m[0] + y * m[3] + m[6],
        x * m[1] + y * m[4] + m[7]
    );
};

g.Matrix3.prototype.transformPath = function (path) {
    var _this = this,
        elements = _.map(path.elements, function (pe) {
            if (pe.cmd === g.CLOSE) { return pe; }
            if (pe.cmd === g.MOVETO) {
                return { cmd: g.MOVETO,
                    point: _this.transformPoint(pe.point) };
            }
            if (pe.cmd === g.LINETO) {
                return { cmd: g.LINETO,
                    point: _this.transformPoint(pe.point) };
            }
            if (pe.cmd === g.CURVETO) {
                return { cmd: g.CURVETO,
                    point: _this.transformPoint(pe.point),
                    ctrl1: _this.transformPoint(pe.ctrl1),
                    ctrl2: _this.transformPoint(pe.ctrl2) };
            }
        });
    return g.makePath(elements, path.fill, path.stroke, path.strokeWidth);
};

g.Matrix3.prototype.transformGroup = function (group) {
    var _this = this,
        shapes = _.map(group.shapes, function (shape) {
            return _this.transformShape(shape);
        });
    return g.makeGroup(shapes);
};

g.Matrix3.prototype.transformShape = function (shape) {
    var fn = (shape.shapes) ? this.transformGroup : this.transformPath;
    return fn.call(this, shape);
};

// Construct a 4x4 matrix.
g.Matrix4 = function (m) {
    if (m !== undefined) {
       // todo: check for type and length
        this.m = m;
    } else {
        m = new Float32Array(16);
        m[0] = 1.0;
        m[1] = 0.0;
        m[2] = 0.0;
        m[3] = 0.0;
        m[4] = 0.0;
        m[5] = 1.0;
        m[6] = 0.0;
        m[7] = 0.0;
        m[8] = 0.0;
        m[9] = 0.0;
        m[10] = 1.0;
        m[11] = 0.0;
        m[12] = 0.0;
        m[13] = 0.0;
        m[14] = 0.0;
        m[15] = 1.0;
        this.m = m;
    }
    // A Float32Array object cannot actually be frozen.
    Object.freeze(this);
};

g.Matrix4.IDENTITY = new g.Matrix4();

// Create a perspective matrix transformation.
g.Matrix4.perspective = function (fov, aspect, zNear, zFar) {
    var m = new Float32Array(g.Matrix4.IDENTITY.m),
        tan = 1.0 / (Math.tan(fov * 0.5));

    m[0] = tan / aspect;
    m[1] = m[2] = m[3] = 0.0;
    m[5] = tan;
    m[4] = m[6] = m[7] = 0.0;
    m[8] = m[9] = 0.0;
    m[10] = -zFar / (zNear - zFar);
    m[11] = 1.0;
    m[12] = m[13] = m[15] = 0.0;
    m[14] = (zNear * zFar) / (zNear - zFar);

    return new g.Matrix4(m);
};

g.Matrix4.lookAt = function (eye, target, up) {
    var m, zAxis, xAxis, yAxis, ex, ey, ez;
    m = new Float32Array(16);
    zAxis = target.subtract(eye).normalize();
    xAxis = g.Vec3.cross(up, zAxis).normalize();
    yAxis = g.Vec3.cross(zAxis, xAxis).normalize();

    ex = -g.Vec3.dot(xAxis, eye);
    ey = -g.Vec3.dot(yAxis, eye);
    ez = -g.Vec3.dot(zAxis, eye);

    m[0] = xAxis.x;
    m[1] = yAxis.x;
    m[2] = zAxis.x;
    m[3] = 0;
    m[4] = xAxis.y;
    m[5] = yAxis.y;
    m[6] = zAxis.y;
    m[7] = 0;
    m[8] = xAxis.z;
    m[9] = yAxis.z;
    m[10] = zAxis.z;
    m[11] = 0;
    m[12] = ex;
    m[13] = ey;
    m[14] = ez;
    m[15] = 1;

    return new g.Matrix4(m);
};

// Return a new matrix with the inversion of this matrix.
g.Matrix4.prototype.invert = function () {
    var l1, l2, l3, l4, l5, l6, l7, l8, l9, l10, l11, l12, l13, l14, l15, l16, l17, l18, l19, l20, l21, l22, l23, l24, l25, l26, l27, l28,
        l29, l30, l31, l32, l33, l34, l35, l36, l37, l38, l39, m;
    l1 = this.m[0];
    l2 = this.m[1];
    l3 = this.m[2];
    l4 = this.m[3];
    l5 = this.m[4];
    l6 = this.m[5];
    l7 = this.m[6];
    l8 = this.m[7];
    l9 = this.m[8];
    l10 = this.m[9];
    l11 = this.m[10];
    l12 = this.m[11];
    l13 = this.m[12];
    l14 = this.m[13];
    l15 = this.m[14];
    l16 = this.m[15];
    l17 = (l11 * l16) - (l12 * l15);
    l18 = (l10 * l16) - (l12 * l14);
    l19 = (l10 * l15) - (l11 * l14);
    l20 = (l9 * l16) - (l12 * l13);
    l21 = (l9 * l15) - (l11 * l13);
    l22 = (l9 * l14) - (l10 * l13);
    l23 = ((l6 * l17) - (l7 * l18)) + (l8 * l19);
    l24 = -(((l5 * l17) - (l7 * l20)) + (l8 * l21));
    l25 = ((l5 * l18) - (l6 * l20)) + (l8 * l22);
    l26 = -(((l5 * l19) - (l6 * l21)) + (l7 * l22));
    l27 = 1.0 / ((((l1 * l23) + (l2 * l24)) + (l3 * l25)) + (l4 * l26));
    l28 = (l7 * l16) - (l8 * l15);
    l29 = (l6 * l16) - (l8 * l14);
    l30 = (l6 * l15) - (l7 * l14);
    l31 = (l5 * l16) - (l8 * l13);
    l32 = (l5 * l15) - (l7 * l13);
    l33 = (l5 * l14) - (l6 * l13);
    l34 = (l7 * l12) - (l8 * l11);
    l35 = (l6 * l12) - (l8 * l10);
    l36 = (l6 * l11) - (l7 * l10);
    l37 = (l5 * l12) - (l8 * l9);
    l38 = (l5 * l11) - (l7 * l9);
    l39 = (l5 * l10) - (l6 * l9);

    m = new Float32Array(16);
    m[0] = l23 * l27;
    m[4] = l24 * l27;
    m[8] = l25 * l27;
    m[12] = l26 * l27;
    m[1] = -(((l2 * l17) - (l3 * l18)) + (l4 * l19)) * l27;
    m[5] = (((l1 * l17) - (l3 * l20)) + (l4 * l21)) * l27;
    m[9] = -(((l1 * l18) - (l2 * l20)) + (l4 * l22)) * l27;
    m[13] = (((l1 * l19) - (l2 * l21)) + (l3 * l22)) * l27;
    m[2] = (((l2 * l28) - (l3 * l29)) + (l4 * l30)) * l27;
    m[6] = -(((l1 * l28) - (l3 * l31)) + (l4 * l32)) * l27;
    m[10] = (((l1 * l29) - (l2 * l31)) + (l4 * l33)) * l27;
    m[14] = -(((l1 * l30) - (l2 * l32)) + (l3 * l33)) * l27;
    m[3] = -(((l2 * l34) - (l3 * l35)) + (l4 * l36)) * l27;
    m[7] = (((l1 * l34) - (l3 * l37)) + (l4 * l38)) * l27;
    m[11] = -(((l1 * l35) - (l2 * l37)) + (l4 * l39)) * l27;
    m[15] = (((l1 * l36) - (l2 * l38)) + (l3 * l39)) * l27;
    return new g.Matrix4(m);
};

g.Matrix4.prototype.multiply = function (other) {
    var m = new Float32Array(16);

    m[0] = this.m[0] * other.m[0] + this.m[1] * other.m[4] + this.m[2] * other.m[8] + this.m[3] * other.m[12];
    m[1] = this.m[0] * other.m[1] + this.m[1] * other.m[5] + this.m[2] * other.m[9] + this.m[3] * other.m[13];
    m[2] = this.m[0] * other.m[2] + this.m[1] * other.m[6] + this.m[2] * other.m[10] + this.m[3] * other.m[14];
    m[3] = this.m[0] * other.m[3] + this.m[1] * other.m[7] + this.m[2] * other.m[11] + this.m[3] * other.m[15];

    m[4] = this.m[4] * other.m[0] + this.m[5] * other.m[4] + this.m[6] * other.m[8] + this.m[7] * other.m[12];
    m[5] = this.m[4] * other.m[1] + this.m[5] * other.m[5] + this.m[6] * other.m[9] + this.m[7] * other.m[13];
    m[6] = this.m[4] * other.m[2] + this.m[5] * other.m[6] + this.m[6] * other.m[10] + this.m[7] * other.m[14];
    m[7] = this.m[4] * other.m[3] + this.m[5] * other.m[7] + this.m[6] * other.m[11] + this.m[7] * other.m[15];

    m[8] = this.m[8] * other.m[0] + this.m[9] * other.m[4] + this.m[10] * other.m[8] + this.m[11] * other.m[12];
    m[9] = this.m[8] * other.m[1] + this.m[9] * other.m[5] + this.m[10] * other.m[9] + this.m[11] * other.m[13];
    m[10] = this.m[8] * other.m[2] + this.m[9] * other.m[6] + this.m[10] * other.m[10] + this.m[11] * other.m[14];
    m[11] = this.m[8] * other.m[3] + this.m[9] * other.m[7] + this.m[10] * other.m[11] + this.m[11] * other.m[15];

    m[12] = this.m[12] * other.m[0] + this.m[13] * other.m[4] + this.m[14] * other.m[8] + this.m[15] * other.m[12];
    m[13] = this.m[12] * other.m[1] + this.m[13] * other.m[5] + this.m[14] * other.m[9] + this.m[15] * other.m[13];
    m[14] = this.m[12] * other.m[2] + this.m[13] * other.m[6] + this.m[14] * other.m[10] + this.m[15] * other.m[14];
    m[15] = this.m[12] * other.m[3] + this.m[13] * other.m[7] + this.m[14] * other.m[11] + this.m[15] * other.m[15];

    return new g.Matrix4(m);
};

g.Matrix4.prototype.translate = function (tx, ty, tz) {
    var m = new Float32Array(this.m);
    m[12] += tx;
    m[13] += ty;
    m[14] += tz;
    return new g.Matrix4(m);
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
    if (typeof c === 'string') { return c; }
    if (c instanceof g.Color) { return c._get(); }
    return new g.Color(c)._get();
};

g.drawPoints = function (ctx, points) {
    var pt, i;
    ctx.fillStyle = 'blue';
    ctx.beginPath();
    for (i = 0; i < points.length; i += 1) {
        pt = points[i];
        ctx.moveTo(pt.x, pt.y);
        ctx.arc(pt.x, pt.y, 2, 0, Math.PI * 2, false);
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
        } else if (shape.shapes || shape.elements) {
            shape.draw(ctx);
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
    if (g.svg.read[tag] === undefined) {
        return null;
    }

    node = g.svg.read[tag].call(this, svgNode);
    return node;
};

g.svg.getReflection = function (a, b, relative) {
    var theta,
        d = g.geometry.distance(a.x, a.y, b.x, b.y);

    if (d <= 0.0001) {
        return relative ? g.Point.ZERO : a;
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

        _.each(node.childNodes, function (n) {

            var tag, tagName, o;
            tag = n.nodeName;
            if (!tag) { return; }
            tagName = tag.replace(/svg\:/ig, '').toLowerCase();
            if (g.svg.read[tagName] !== undefined) {
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
        return g.svg.applySvgAttributes(node, g._rect(x, y, width, height));
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
        return g.svg.applySvgAttributes(node, g._ellipse(x, y, width, height));
    },

    circle: function (node) {
        var cx, cy, r, x, y, width, height;
        cx = parseFloat(node.getAttribute('cx'));
        cy = parseFloat(node.getAttribute('cy'));
        r = parseFloat(node.getAttribute('r'));
        x = cx - r;
        y = cy - r;
        width = height = r * 2;
        return g.svg.applySvgAttributes(node, g._ellipse(x, y, width, height));
    },

    line: function (node) {
        var x1, y1, x2, y2;
        x1 = parseFloat(node.getAttribute('x1'));
        y1 = parseFloat(node.getAttribute('y1'));
        x2 = parseFloat(node.getAttribute('x2'));
        y2 = parseFloat(node.getAttribute('y2'));
        return g.svg.applySvgAttributes(node, g._line(x1, y1, x2, y2));
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
                elements.push(g.moveto(p.x, p.y));
                pp.start = pp.current;
                while (!pp.isCommandOrEnd()) {
                    p = pp.getAsCurrentPoint();
                    elements.push(g.lineto(p.x, p.y));
                }
                break;
            case 'L':
            case 'l':
                while (!pp.isCommandOrEnd()) {
                    p = pp.getAsCurrentPoint();
                    elements.push(g.lineto(p.x, p.y));
                }
                break;
            case 'H':
            case 'h':
                while (!pp.isCommandOrEnd()) {
                    newP = g.makePoint((pp.isRelativeCommand() ? pp.current.x : 0) + pp.getScalar(), pp.current.y);
                    pp.current = newP;
                    elements.push(g.lineto(pp.current.x, pp.current.y));
                }
                break;
            case 'V':
            case 'v':
                while (!pp.isCommandOrEnd()) {
                    newP = g.makePoint(pp.current.x, (pp.isRelativeCommand() ? pp.current.y : 0) + pp.getScalar());
                    pp.current = newP;
                    elements.push(g.lineto(pp.current.x, pp.current.y));
                }
                break;
            case 'C':
            case 'c':
                while (!pp.isCommandOrEnd()) {
                    curr = pp.current;
                    p1 = pp.getPoint();
                    cntrl = pp.getAsControlPoint();
                    cp = pp.getAsCurrentPoint();
                    elements.push(g.curveto(p1.x, p1.y, cntrl.x, cntrl.y, cp.x, cp.y));
                }
                break;
            case 'S':
            case 's':
                while (!pp.isCommandOrEnd()) {
                    curr = pp.current;
                    p1 = pp.getReflectedControlPoint();
                    cntrl = pp.getAsControlPoint();
                    cp = pp.getAsCurrentPoint();
                    elements.push(g.curveto(p1.x, p1.y, cntrl.x, cntrl.y, cp.x, cp.y));
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
                    elements.push(g.curveto(cp1x, cp1y, cp2x, cp2y, cp.x, cp.y));
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
                    elements.push(g.curveto(cp1x, cp1y, cp2x, cp2y, cp.x, cp.y));
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
                        elements.push(g.curveto.apply(this, bez));
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
        return new g.Transform().translate(tx, ty);
    };

    types.scale = function (s) {
        var a = g.svg.ToNumberArray(s),
            sx = a[0],
            sy = a[1] || sx;
        return new g.Transform().scale(sx, sy);
    };

    types.rotate = function (s) {
        var t,
            a = g.svg.ToNumberArray(s),
            r = a[0],
            tx = a[1] || 0,
            ty = a[2] || 0;
        t = new g.Transform();
        t = t.translate(tx, ty);
        t = t.rotate(r);
        t = t.translate(-tx, -ty);
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
            fill = g.makeColor.apply(g, g._namedColors[fill]);
        } else if (fill.indexOf("#") === 0) {
            fill = g.makeColor(fill, 0, 0, 0, { colorspace: g.HEX });
        } else if (fill === "none") {
            fill = null;
        }
    }

    if (stroke !== undefined) {
        if (g._namedColors[stroke]) {
            stroke = g.makeColor.apply(g, g._namedColors[stroke]);
        } else if (stroke.indexOf("#") === 0) {
            stroke = g.makeColor(stroke, 0, 0, 0, { colorspace: g.HEX });
        } else if (stroke === "none") {
            stroke = null;
        }
    }

    transform = new g.Transform();
    for (i = 0; i < transforms.length; i += 1) {
        transform = g.append(transform, transforms[i]);
    }

    function applyAttributes(shape) {
        if (shape.elements) {
            var elements = transform.transformShape(shape).elements,
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

////////// Shapes //////////

g.rect = function (position, width, height, roundness) {
    if (!roundness || (roundness.x === 0 && roundness.y === 0)) {
        return g._rect(position.x - width / 2, position.y - height / 2, width, height);
    } else {
        return g.roundedRect(position.x - width / 2, position.y - height / 2, width, height, roundness.x, roundness.y);
    }
};

g.ellipse = function (position, width, height) {
    return g._ellipse(position.x - width / 2, position.y - height / 2, width, height);
};

g.line = function (point1, point2, points) {
    var line = g.makePath(g._line(point1.x, point1.y, point2.x, point2.y),
                      null, {'r': 0, 'g': 0, 'b': 0, 'a': 1}, 1.0);
    if (points !== null && points > 2) {
        line = line.resampleByAmount(10, false);
    }
    return line;
};

g.lineAngle = function (point, angle, distance) {
    var point2 = g.geometry.coordinates(point.x, point.y, distance, angle);
    return g.line(point, point2);
};

g.arc = function (position, width, height, startAngle, degrees, arcType) {
    return g._arc(position.x, position.y, width, height, startAngle, degrees, arcType);
};

g.quadCurve = function (pt1, pt2, t, distance) {
    t /= 100.0;
    var cx = pt1.x + t * (pt2.x - pt1.x),
        cy = pt1.y + t * (pt2.y - pt1.y),
        a = g.geometry.angle(pt1.x, pt1.y, pt2.x, pt2.y) + 90,
        q = g.geometry.coordinates(cx, cy, distance, a),
        qx = q.x,
        qy = q.y,

        c1x = pt1.x + 2 / 3.0 * (qx - pt1.x),
        c1y = pt1.y + 2 / 3.0 * (qy - pt1.y),
        c2x = pt2.x + 2 / 3.0 * (qx - pt2.x),
        c2y = pt2.y + 2 / 3.0 * (qy - pt2.y),
        elements = [
            g.moveto(pt1.x, pt1.y),
            g.curveto(c1x, c1y, c2x, c2y, pt2.x, pt2.y)
        ];
    return g.makePath(elements, null, {'r': 0, 'g': 0, 'b': 0, 'a': 1}, 1.0);
};

g.polygon = function (position, radius, sides, align) {
    sides = Math.max(sides, 3);
    var c0, c1, i, c,
        x = position.x,
        y = position.y,
        r = radius,
        a = 360.0 / sides,
        da = 0,
        elements = [];
    if (align === true) {
        c0 = g.geometry.coordinates(x, y, r, 0);
        c1 = g.geometry.coordinates(x, y, r, a);
        da = -g.geometry.angle(c1.x, c1.y, c0.x, c0.y);
    }
    for (i = 0; i < sides; i += 1) {
        c = g.geometry.coordinates(x, y, r, (a * i) + da);
        elements.push(((i === 0) ? g.moveto : g.lineto)(c.x, c.y));
    }
    elements.push(g.close());
    return new g.Path(elements);
};

g.star = function (position, points, outer, inner) {
    var i, angle, radius, x, y,
        elements = [g.moveto(position.x, position.y + outer / 2)];
    // Calculate the points of the star.
    for (i = 1; i < points * 2; i += 1) {
        angle = i * Math.PI / points;
        radius = (i % 2 === 1) ? inner / 2 : outer / 2;
        x = position.x + radius * Math.sin(angle);
        y = position.y + radius * Math.cos(angle);
        elements.push(g.lineto(x, y));
    }
    elements.push(g.close());
    return new g.Path(elements);
};

g.freehand = function (pathString) {
    var i, j, values, cmd,
        elements = [],
        nonEmpty = function (s) { return s !== ""; },
        contours = _.filter(pathString.split("M"), nonEmpty);

    contours = _.map(contours, function (c) { return c.replace(/,/g, " "); });

    for (j = 0; j < contours.length; j += 1) {
        values = _.filter(contours[j].split(" "), nonEmpty);
        for (i = 0; i < values.length; i += 2) {
            if (values[i + 1] !== undefined) {
                cmd = (i === 0) ? g.moveto : g.lineto;
                elements.push(cmd(parseFloat(values[i]), parseFloat(values[i + 1])));
            }
        }
    }

    return g.makePath(elements, null, {"r": 0, "g": 0, "b": 0, "a": 1}, 1);
};

// Create a grid of points.
g.grid = function (columns, rows, width, height, position) {
    var columnSize, left, rowSize, top, rowIndex, colIndex, x, y,
        points = [];
    position = position !== undefined ? position : g.Point.ZERO;
    if (columns > 1) {
        columnSize = width / (columns - 1);
        left = position.x - width / 2;
    } else {
        columnSize = left = position.x;
    }
    if (rows > 1) {
        rowSize = height / (rows - 1);
        top = position.y - height / 2;
    } else {
        rowSize = top = position.y;
    }

    for (rowIndex = 0; rowIndex < rows; rowIndex += 1) {
        for (colIndex = 0; colIndex < columns; colIndex += 1) {
            x = left + colIndex * columnSize;
            y = top + rowIndex * rowSize;
            points.push(g.makePoint(x, y));
        }
    }
    return Object.freeze(points);
};

g.demoRect = function () {
    return new g.rect({x: 0, y: 0}, 100, 100, {x: 0, y: 0});
};

g.demoEllipse = function () {
    return new g.ellipse({x: 0, y: 0}, 100, 100);
};

////////// Filters //////////

g.colorize = function (shape, fill, stroke, strokeWidth) {
    return shape.colorize(fill, stroke, strokeWidth);
};

g.translate = function (shape, position) {
    var t = new g.Transform().translate(position.x, position.y);
    return t.transformShape(shape);
};

g.scale = function (shape, scale) {
    var t = new g.Transform().scale(scale.x / 100, scale.y / 100);
    return t.transformShape(shape);
};

g.rotate = function (shape, angle) {
    var t = new g.Transform().rotate(angle);
    return t.transformShape(shape);
};

g.skew = function (shape, skew, origin) {
    var t = new g.Transform();
    t = t.translate(origin.x, origin.y);
    t = t.skew(skew.x, skew.y);
    t = t.translate(-origin.x, -origin.y);
    return t.transformShape(shape);
};

g.copy = function (shape, copies, order, translate, rotate, scale) {
    var i, t, j, op,
        shapes = [],
        tx = 0,
        ty = 0,
        r = 0,
        sx = 1.0,
        sy = 1.0;
    for (i = 0; i < copies; i += 1) {
        t = new g.Transform();
        for (j = 0; j < order.length; j += 1) {
            op = order[j];
            if (op === 't') {
                t = t.translate(tx, ty);
            } else if (op === 'r') {
                t = t.rotate(r);
            } else if (op === 's') {
                t = t.scale(sx, sy);
            }
        }
        shapes.push(t.transformShape(shape));

        tx += translate.x;
        ty += translate.y;
        r += rotate;
        sx += scale.x;
        sy += scale.y;
    }
    return shapes;
};

g.fit = function (shape, position, width, height, keepProportions) {
    if (shape === null) { return null; }
    var t, sx, sy,
        bounds = shape.bounds(),
        bx = bounds.x,
        by = bounds.y,
        bw = bounds.width,
        bh = bounds.height;

    // Make sure bw and bh aren't infinitely small numbers.
    // This will lead to incorrect transformations with for examples lines.
    bw = (bw > 0.000000000001) ? bw : 0;
    bh = (bh > 0.000000000001) ? bh : 0;

    t = new g.Transform();
    t = t.translate(position.x, position.y);

    if (keepProportions) {
        // don't scale widths or heights that are equal to zero.
        sx = (bw > 0) ? (width / bw) : Number.MAX_VALUE;
        sy = (bh > 0) ? (height / bh) : Number.MAX_VALUE;
        sx = sy = Math.min(sx, sy);
    } else {
        sx = (bw > 0) ? (width / bw) : 1;
        sy = (bh > 0) ? (height / bh) : 1;
    }

    t = t.scale(sx, sy);
    t = t.translate(-bw / 2 - bx, -bh / 2 - by);

    return t.transformShape(shape);
};

g.fitTo = function (shape, bounding, keepProportions) {
    // Fit a shape to another shape.
    if (shape === null) { return null; }
    if (bounding === null) { return shape; }

    var bounds = bounding.bounds(),
        bx = bounds.x,
        by = bounds.y,
        bw = bounds.width,
        bh = bounds.height;

    return g.fit(shape, {x: bx + bw / 2, y: by + bh / 2}, bw, bh, keepProportions);
};

g.reflect = function (shape, position, angle, keepOriginal) {
    if (shape === null) { return null; }

    var f, reflectPath, reflectGroup, reflect, newShape;

    f = function (point) {
        var d = g.geometry.distance(point.x, point.y, position.x, position.y),
            a = g.geometry.angle(point.x, point.y, position.x, position.y),
            pt = g.geometry.coordinates(position.x, position.y, d * Math.cos(g.math.radians(a - angle)), 180 + angle);
        d = g.geometry.distance(point.x, point.y, pt.x, pt.y);
        a = g.geometry.angle(point.x, point.y, pt.x, pt.y);
        pt = g.geometry.coordinates(point.x, point.y, d * 2, a);
        return g.makePoint(pt.x, pt.y);
    };

    reflectPath = function (path) {
        var elements = _.map(path.elements, function (elem) {
            var pt, ctrl1, ctrl2;
            if (elem.cmd === g.CLOSE) {
                return elem;
            } else if (elem.cmd === g.MOVETO) {
                pt = f(elem.point);
                return g.moveto(pt.x, pt.y);
            } else if (elem.cmd === g.LINETO) {
                pt = f(elem.point);
                return g.lineto(pt.x, pt.y);
            } else if (elem.cmd === g.CURVETO) {
                pt = f(elem.point);
                ctrl1 = f(elem.ctrl1);
                ctrl2 = f(elem.ctrl2);
                return g.curveto(ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, pt.x, pt.y);
            }
        });
        return g.makePath(elements, path.fill, path.stroke, path.strokeWidth);
    };

    reflectGroup = function (group) {
        var shapes = _.map(group.shapes, function (shape) {
            return reflect(shape);
        });
        return g.makeGroup(shapes);
    };

    reflect = function (shape) {
        var fn = (shape.shapes) ? reflectGroup : reflectPath;
        return fn(shape);
    };

    newShape = reflect(shape);

    if (keepOriginal) {
        return g.makeGroup([shape, newShape]);
    } else {
        return newShape;
    }
};

g.resample = function (shape, method, length, points, perContour) {
    if (method === "length") {
        return shape.resampleByLength(length);
    } else {
        return shape.resampleByAmount(points, perContour);
    }
};

g.wiggle = function (shape, scope, offset, seed) {
    var rand, wigglePoints, wigglePaths, wiggleContours;
    rand = g.randomGenerator(seed);

    wigglePoints = function (shape) {
        if (shape.elements) {
            var i, dx, dy, pe, elements = [];
            for (i = 0; i < shape.elements.length; i += 1) {
                dx = (rand(0, 1) - 0.5) * offset.x * 2;
                dy = (rand(0, 1) - 0.5) * offset.y * 2;
                pe = shape.elements[i];
                if (pe.cmd === g.CLOSE) {
                    elements.push(pe);
                } else if (pe.cmd === g.MOVETO) {
                    elements.push(g.moveto(pe.point.x + dx, pe.point.y + dy));
                } else if (pe.cmd === g.LINETO) {
                    elements.push(g.lineto(pe.point.x + dx, pe.point.y + dy));
                } else if (pe.cmd === g.CURVETO) {
                    elements.push(g.curveto(pe.ctrl1.x, pe.ctrl1.y,
                                     pe.ctrl2.x, pe.ctrl2.y,
                                     pe.point.x + dx, pe.point.y + dy));
                }
            }
            return g.makePath(elements, shape.fill, shape.stroke, shape.strokeWidth);
        } else if (shape.shapes) {
            return g.makeGroup(_.map(shape.shapes, wigglePoints));
        } else {
            return _.map(shape, wigglePoints);
        }
    };

    wigglePaths = function (shape) {
        if (shape.elements) {
            return shape;
        } else if (shape.shapes) {
            var i, subShape, dx, dy, t, newShapes = [];
            for (i = 0; i < shape.shapes.length; i += 1) {
                subShape = shape.shapes[i];
                if (subShape.elements) {
                    dx = (rand(0, 1) - 0.5) * offset.x * 2;
                    dy = (rand(0, 1) - 0.5) * offset.y * 2;
                    t = new g.Transform().translate(dx, dy);
                    newShapes.push(t.transformShape(subShape));
                } else if (subShape.shapes) {
                    newShapes.push(wigglePaths(subShape));
                }
            }
            return new g.Group(newShapes);
        } else {
            return _.map(shape, wigglePaths);
        }
    };

    wiggleContours = function (shape) {
        if (shape.elements) {
            var i, dx, dy, t,
                subPaths = g.getContours(shape),
                elements = [];
            for (i = 0; i < subPaths.length; i += 1) {
                dx = (rand(0, 1) - 0.5) * offset.x * 2;
                dy = (rand(0, 1) - 0.5) * offset.y * 2;
                t = new g.Transform().translate(dx, dy);
                elements = elements.concat(t.transformShape(g.makePath(subPaths[i])).elements);
            }
            return g.makePath(elements, shape.fill, shape.stroke, shape.strokeWidth);
        } else if (shape.shapes) {
            return g.makeGroup(_.map(shape.shapes, wiggleContours));
        } else {
            return _.map(shape, wiggleContours);
        }
    };

    if (scope === "points") {
        return wigglePoints(shape);
    }
    if (scope === "paths") {
        return wigglePaths(shape);
    }
    if (scope === "contours") {
        return wiggleContours(shape);
    }
};

g.scatter = function (shape, amount, seed) {
    // Generate points within the boundaries of a shape.
    if (shape === null) { return null; }
    var i, tries, x, y,
        rand = g.randomGenerator(seed),
        bounds = shape.bounds(),
        bx = bounds.x,
        by = bounds.y,
        bw = bounds.width,
        bh = bounds.height,
        points = [];

    for (i = 0; i < amount; i += 1) {
        tries = 100;
        while (tries > 0) {
            x = bx + rand(0, 1) * bw;
            y = by + rand(0, 1) * bh;
            if (shape.contains(x, y)) {
                points.push(new g.Point(x, y));
                break;
            }
            tries -= 1;
        }
    }
    return points;
};

g.connect = function (points, closed) {
    if (points === null) { return null; }
    var i, pt, elements = [];
    for (i = 0; i < points.length; i += 1) {
        pt = points[i];
        elements.push((i === 0 ? g.moveto : g.lineto)(pt.x, pt.y));
    }
    if (closed) {
        elements.push(g.closePath());
    }
    return g.makePath(elements, null, {"r": 0, "g": 0, "b": 0, "a": 1}, 1);
};

g.align = function (shape, position, hAlign, vAlign) {
    if (shape === null) { return null; }
    var dx, dy, t,
        x = position.x,
        y = position.y,
        bounds = shape.bounds();
    if (hAlign === "left") {
        dx = x - bounds.x;
    } else if (hAlign === "right") {
        dx = x - bounds.x - bounds.width;
    } else if (hAlign === "center") {
        dx = x - bounds.x - bounds.width / 2;
    } else {
        dx = 0;
    }
    if (vAlign === "top") {
        dy = y - bounds.y;
    } else if (vAlign === "bottom") {
        dy = y - bounds.y - bounds.height;
    } else if (vAlign === "middle") {
        dy = y - bounds.y - bounds.height / 2;
    } else {
        dy = 0;
    }

    t = new g.Transform().translate(dx, dy);
    return t.transformShape(shape);
};

g.snap = function (shape, distance, strength, position) {
    // Snap geometry to a grid.

    function _snap(v, offset, distance, strength) {
        if (offset === null) { offset = 0.0; }
        if (distance === null) { distance = 10.0; }
        if (strength === null) { strength = 1.0; }
        return (v * (1.0 - strength)) + (strength * Math.round(v / distance) * distance);
    }

    if (shape === null) { return null; }
    if (position === null) { position = g.Point.ZERO; }
    strength /= 100.0;

    var snapShape = function (shape) {
        if (shape.elements) {
            var elements = _.map(shape.elements, function (pe) {
                if (pe.cmd === g.CLOSE) { return pe; }
                var x, y, ctrl1x, ctrl1y, ctrl2x, ctrl2y;
                x = _snap(pe.point.x + position.x, position.x, distance, strength) - position.x;
                y = _snap(pe.point.y + position.y, position.y, distance, strength) - position.y;
                if (pe.cmd === g.MOVETO) {
                    return g.moveto(x, y);
                } else if (pe.cmd === g.LINETO) {
                    return g.lineto(x, y);
                } else if (pe.cmd === g.CURVETO) {
                    ctrl1x = _snap(pe.ctrl1.x + position.x, position.x, distance, strength) - position.x;
                    ctrl1y = _snap(pe.ctrl1.y + position.y, position.y, distance, strength) - position.y;
                    ctrl2x = _snap(pe.ctrl2.x + position.x, position.x, distance, strength) - position.x;
                    ctrl2y = _snap(pe.ctrl2.y + position.y, position.y, distance, strength) - position.y;
                    return g.curveto(ctrl1x, ctrl1y, ctrl2x, ctrl2y, x, y);
                }
            });
            return g.makePath(elements, shape.fill, shape.stroke, shape.strokeWidth);
        } else if (shape.shapes) {
            return g.makeGroup(_.map(shape.shapes, snapShape));
        } else {
            return _.map(shape, snapShape);
        }
    };

    return snapShape(shape);
};

g.deletePoints = function (shape, bounding, deleteSelected) {
    var deletePoints = function (shape) {
        if (shape.elements) {
            var i, elem, elems = [];
            for (i = 0; i < shape.elements.length; i += 1) {
                elem = shape.elements[i];
                if (elem.point === undefined ||
                        (deleteSelected && bounding.contains(elem.point.x, elem.point.y)) ||
                        (!deleteSelected && !bounding.contains(elem.point.x, elem.point.y))) {
                    elems.push(elem);
                }
            }
            return g.makePath(elems, shape.fill, shape.stroke, shape.strokeWidth);
        } else if (shape.shapes) {
            return g.makeGroup(_.map(shape.shapes, deletePoints));
        } else {
            return _.map(shape, deletePoints);
        }
    };

    return deletePoints(shape);
};

g.deletePaths = function (shape, bounding, deleteSelected) {
    var deletePaths = function (shape) {
        if (shape.elements) {
            return null;
        } else if (shape.shapes) {
            var i, j, s, selected, elem, subShapes, newShapes = [];
            for (i = 0; i < shape.shapes.length; i += 1) {
                s = shape.shapes[i];
                if (s.elements) {
                    selected = false;
                    for (j = 0; j < s.elements.length; j += 1) {
                        elem = s.elements[j];
                        if (elem.point && bounding.contains(elem.point.x, elem.point.y)) {
                            selected = true;
                            break;
                        }
                    }
                    if (selected !== deleteSelected) {
                        newShapes.push(s);
                    }
                } else if (s.shapes) {
                    subShapes = deletePaths(s);
                    if (subShapes.length !== 0) {
                        newShapes.push(subShapes);
                    }
                }
            }
            return g.makeGroup(newShapes);
        } else {
            return _.map(shape, deletePaths);
        }
    };

    return deletePaths(shape);
};

g["delete"] = function (shape, bounding, scope, operation) {
    if (shape === null || bounding === null) { return null; }
    var deleteSelected = operation === "selected";
    if (scope === "points") { return g.deletePoints(shape, bounding, deleteSelected); }
    if (scope === "paths") { return g.deletePaths(shape, bounding, deleteSelected); }
};

g.pointOnPath = function (shape, t) {
    if (shape === null) { return null; }
    if (shape.shapes) {
        shape = g.makePath(g.combinePaths(shape));
    }
    t = Math.abs(t % 100);
    return shape.point(t / 100).point;
};

g.shapeOnPath = function (shapes, path, amount, alignment, spacing, margin, baseline_offset) {
    if (!shapes) { return []; }
    if (path === null) { return []; }

    if (alignment === "trailing") {
        shapes = shapes.slice();
        shapes.reverse();
    }

    var i, pos, p1, p2, a, t,
        length = path.length() - margin,
        m = margin / path.length(),
        c = 0,
        newShapes = [];

    function putOnPath(shape) {
        if (alignment === "distributed") {
            var p = length / ((amount * shapes.length) - 1);
            pos = c * p / length;
            pos = m + (pos * (1 - 2 * m));
        } else {
            pos = ((c * spacing) % length) / length;
            pos = m + (pos * (1 - m));

            if (alignment === "trailing") {
                pos = 1 - pos;
            }
        }

        p1 = path.point(pos).point;
        p2 = path.point(pos + 0.0000001).point;
        a = g.geometry.angle(p1.x, p1.y, p2.x, p2.y);
        if (baseline_offset) {
            p1 = g.geometry.coordinates(p1.x, p1.y, baseline_offset, a - 90);
        }
        t = new g.Transform();
        t = t.translate(p1.x, p1.y);
        t = t.rotate(a);
        newShapes.push(t.transformShape(shape));
        c += 1;
    }

    for (i = 0; i < amount; i += 1) {
        _.each(shapes, putOnPath);
    }
    return newShapes;
};

g._x = function (shape) {
    if (shape.x) {
        return shape.x;
    } else {
        return shape.bounds().x;
    }
};

g._y = function (shape) {
    if (shape.y) {
        return shape.y;
    } else {
        return shape.bounds().y;
    }
};

g._angleToPoint = function (point) {
    return function (shape) {
        if (shape.x && shape.y) {
            return g.geometry.angle(shape.x, shape.y, point.x, point.y);
        } else {
            var centroid = shape.bounds().centroid();
            return g.geometry.angle(centroid.x, centroid.y, point.x, point.y);
        }
    };
};

g._distanceToPoint = function (point) {
    return function (shape) {
        if (shape.x && shape.y) {
            return g.geometry.distance(shape.x, shape.y, point.x, point.y);
        } else {
            var centroid = shape.bounds().centroid();
            return g.geometry.distance(centroid.x, centroid.y, point.x, point.y);
        }
    };
};

g.sort = function (shapes, orderBy, point) {
    if (shapes === null) { return null; }
    var methods, sortMethod, newShapes;
    methods = {
        x: g._x,
        y: g._y,
        angle: g._angleToPoint(point),
        distance: g._distanceToPoint(point)
    };
    sortMethod = methods[orderBy];
    if (sortMethod === undefined) { return shapes; }
    newShapes = shapes.slice(0);
    newShapes.sort(function (a, b) {
        var _a = sortMethod(a),
            _b = sortMethod(b);
        if (_a < _b) { return -1; }
        if (_a > _b) { return 1; }
        return 0;
    });
    return newShapes;
};

g.ungroup = function (shape) {
    if (shape.shapes) {
        var i, s, shapes = [];
        for (i = 0; i < shape.shapes.length; i += 1) {
            s = shape.shapes[i];
            if (s.elements) {
                shapes.push(s);
            } else if (s.shapes) {
                shapes = shapes.concat(g.ungroup(s));
            }
        }
        return shapes;
    } else if (shape.elements) {
        return [shape];
    }
};

g.centroid = function (shape) {
    if (shape === null) { return g.Point.ZERO; }
    var i, pt,
        elements = g.combinePaths(shape),
        firstPoint = elements[0].point,
        xs = firstPoint.x,
        ys = firstPoint.y,
        count = 1;
    for (i = 1; i < elements.length; i += 1) {
        if (elements[i].point) {
            pt = elements[i].point;
            xs += pt.x;
            ys += pt.y;
            count += 1;
        }
    }
    return new g.Point(xs / count, ys / count);
};

g.link = function (shape1, shape2, orientation) {
    if (shape1 === null || shape2 === null) { return null; }
    var elements, hw, hh,
        a = shape1.bounds(),
        b = shape2.bounds();

    if (orientation === "horizontal") {
        hw = (b.x - (a.x + a.width)) / 2;
        elements = [
            g.moveto(a.x + a.width, a.y),
            g.curveto(a.x + a.width + hw, a.y, b.x - hw, b.y, b.x, b.y),
            g.lineto(b.x, b.y + b.height),
            g.curveto(b.x - hw, b.y + b.height, a.x + a.width + hw, a.y + a.height, a.x + a.width, a.y + a.height)
        ];
    } else {
        hh = (b.y - (a.y + a.height)) / 2;
        elements = [
            g.moveto(a.x, a.y + a.height),
            g.curveto(a.x, a.y + a.height + hh, b.x, b.y - hh, b.x, b.y),
            g.lineto(b.x + b.width, b.y),
            g.curveto(b.x + b.width, b.y - hh, a.x + a.width, a.y + a.height + hh, a.x + a.width, a.y + a.height)
        ];
    }
    return new g.Path(elements);
};

g.stack = function (shapes, direction, margin) {
    if (shapes === null) { return []; }
    if (shapes.length <= 1) {
        return shapes;
    }
    var tx, ty, t, bounds,
        first_bounds = shapes[0].bounds(),
        new_shapes = [];
    if (direction === 'e') {
        tx = -(first_bounds.width / 2);
        _.each(shapes, function (shape) {
            bounds = shape.bounds();
            t = new g.Transform().translate(tx - bounds.x, 0);
            new_shapes.push(t.transformShape(shape));
            tx += bounds.width + margin;
        });
    } else if (direction === 'w') {
        tx = first_bounds.width / 2;
        _.each(shapes, function (shape) {
            bounds = shape.bounds();
            t = new g.Transform().translate(tx + bounds.x, 0);
            new_shapes.push(t.transformShape(shape));
            tx -= bounds.width + margin;
        });
    } else if (direction === 'n') {
        ty = first_bounds.height / 2;
        _.each(shapes, function (shape) {
            bounds = shape.bounds();
            t = new g.Transform().translate(0, ty + bounds.y);
            new_shapes.push(t.transformShape(shape));
            ty -= bounds.height + margin;
        });
    } else if (direction === 's') {
        ty = -(first_bounds.height / 2);
        _.each(shapes, function (shape) {
            bounds = shape.bounds();
            t = new g.Transform().translate(0, ty - bounds.y);
            new_shapes.push(t.transformShape(shape));
            ty += bounds.height + margin;
        });
    }
    return new_shapes;
};

////////// Colors //////////

g.gray = function (gray, alpha, range) {
    range = Math.max(range, 1);
    return g.makeColor(gray / range, gray / range, gray / range, alpha / range);
};

g.rgb = function (red, green, blue, alpha, range) {
    range = Math.max(range, 1);
    return g.makeColor(red / range, green / range, blue / range, alpha / range);
};

g.hsb = function (hue, saturation, brightness, alpha, range) {
    range = Math.max(range, 1);
    return g.makeColor(hue / range, saturation / range, brightness / range, alpha / range, { colorspace: g.HSB });
};

