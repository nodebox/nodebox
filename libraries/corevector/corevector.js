var corevector = {};

corevector.rect = function (position, width, height) {
    return g.rect(position.x - width / 2, position.y - height / 2, width, height);
};

corevector.ellipse = function (position, width, height) {
    return g.ellipse(position.x - width / 2, position.y - height / 2, width, height);
};

corevector.line = function (point1, point2) {
    var line = g.line(point1.x, point1.y, point2.x, point2.y);
    return Object.freeze({
        elements: line.elements,
        stroke: {"r": 0, "g": 0, "b": 0, "a": 1},
        strokeWidth: 1
    });
};

corevector.lineAngle = function (point, angle, distance) {
    var point2 = g.geometry.coordinates(point.x, point.y, distance, angle);
    return corevector.line(point, point2);
};

corevector.line_angle = corevector.lineAngle;

corevector.quadCurve = function (pt1, pt2, t, distance) {
    t /= 100.0;
    var cx = pt1.x + t * (pt2.x - pt1.x);
    var cy = pt1.y + t * (pt2.y - pt1.y);
    var a = g.geometry.angle(pt1.x, pt1.y, pt2.x, pt2.y) + 90;
    var q = g.geometry.coordinates(cx, cy, distance, a);
    var qx = q.x;
    var qy = q.y;

    var c1x = pt1.x + 2/3.0 * (qx - pt1.x);
    var c1y = pt1.y + 2/3.0 * (qy - pt1.y);
    var c2x = pt2.x + 2/3.0 * (qx - pt2.x);
    var c2y = pt2.y + 2/3.0 * (qy - pt2.y);
    var elems = [
      g.moveto(pt1.x, pt1.y),
      g.curveto(c1x, c1y, c2x, c2y, pt2.x, pt2.y)];
  return g.makePath(elems, null, {'r': 0, 'g': 0, 'b': 0, 'a': 1}, 1.0);
};

corevector.quad_curve = corevector.quadCurve;

corevector.polygon = function (position, radius, sides, align) {
    var x = position.x;
    var y = position.y;
    var r = radius;
    var sides = Math.max(sides, 3);
    var a = 360.0 / sides;
    var da = 0;
    if (align === true) {
        var c0 = g.geometry.coordinates(x, y, r, 0);
        var c1 = g.geometry.coordinates(x, y, r, a);
        da = - g.geometry.angle(c1.x, c1.y, c0.x, c0.y);
    }
    var elems = [];
    for (var i=0; i<sides; i++) {
        var c = g.geometry.coordinates(x, y, r, (a*i) + da);
        elems.push(((i === 0)? g.moveto : g.lineto)(c.x, c.y));
    }
    elems.push(g.closepath());
    return Object.freeze({ elements: elems });
};

corevector.star = function (position, points, outer, inner) {
    var elems = [g.moveto(position.x, position.y + outer / 2)];
    // Calculate the points of the star.
    for (var i=1; i<points * 2; i++) {
        var angle = i * Math.PI / points;
      var radius = (i % 2 === 1) ? inner / 2 : outer / 2;
        var x = position.x + radius * Math.sin(angle);
        var y = position.y + radius * Math.cos(angle);
        elems.push(g.lineto(x, y));
    }
    elems.push(g.closePath());
    return Object.freeze({ elements: elems });
};

corevector.colorize = function (shape, fill, stroke, strokeWidth) {
    return g.makePath(shape, fill, stroke, strokeWidth);
};

corevector.translate = function (shape, position) {
    var t = g.translate(g.IDENTITY, position.x, position.y);
    return g.transformPath(shape, t);
};

corevector.scale = function (shape, scale) {
    var t = g.scale(g.IDENTITY, scale.x, scale.y);
    return g.transformPath(shape, t);
};

corevector.rotate = function (shape, angle) {
    var t = g.rotate(g.IDENTITY, angle);
    return g.transformPath(shape, t);
};

corevector.resample = function (shape, method, length, points, perContour) {
    var pts = _.map(g.points(shape, points + 1), function(pe)
                    { return pe.point; });
    var elems = [g.moveto(pts[0].x, pts[0].y)];
    for (var i=1; i<pts.length; i++)
        elems.push(g.lineto(pts[i].x, pts[i].y));
    elems.push(g.closePath());
    return g.makePath(elems, shape.fill, shape.stroke, shape.strokeWidth);
};

corevector.wiggle = function (shape, scope, offset, seed) {
    var rand = core.randomGenerator(seed);
    var elems = [];
    for (var i=0; i<shape.elements.length; i++) {
        var dx = (rand(0, 1) - 0.5) * offset.x * 2;
        var dy = (rand(0, 1) - 0.5) * offset.y * 2;
        var pe = shape.elements[i];
        if (pe.cmd === g.CLOSE) {
            elems.push(pe);
        } else if (pe.cmd === g.MOVETO) {
            elems.push(g.moveto(pe.point.x + dx, pe.point.y + dy));
        } else if (pe.cmd === g.LINETO) {
            elems.push(g.lineto(pe.point.x + dx, pe.point.y + dy));
        } else if (pe.cmd === g.CURVETO) {
            elems.push(g.curveto(pe.ctrl1.x, pe.ctrl1.y,
                             pe.ctrl2.x, pe.ctrl2.y,
                             pe.point.x + dx, pe.point.y + dy));
        }
    }
    return g.makePath(elems, shape.fill, shape.stroke, shape.strokeWidth);
};

corevector.grid = function (rows, columns, width, height, position) {
    // Create a grid of points.
    var column_size, left;
    var row_size, top;
    if (columns > 1) {
        column_size = width / (columns - 1);
        left = position.x - width / 2;
    } else {
        column_size = left = position.x;
    }
    if (rows > 1) {
        row_size = height / (rows - 1);
        top = position.y - height / 2;
    } else {
        row_size = top = position.y;
    };

    var points = [];
    _.each(_.range(rows), function(ri) {
        _.each(_.range(columns), function(ci) {
            var x = left + ci * column_size;
            var y = top + ri * row_size;
            points.push(g.makePoint(x, y));
        });
    });
    return Object.freeze(points);
};

corevector.makePoint = function(x, y) {
    return {x: x, y: y};
};
