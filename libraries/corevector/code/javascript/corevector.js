var corevector = {};

corevector.generator = function () {
    return g.rect(0, 0, 100, 100);
};

corevector.filter = function (shape) {
    var t = g.rotate(g.IDENTITY, 45);
    return g.transformShape(shape, t);
};

corevector.doNothing = function (shape) {
    return shape;
};

corevector.rect = function (position, width, height, roundness) {
    if (roundness.x === 0 && roundness.y === 0) {
        return g.rect(position.x - width / 2, position.y - height / 2, width, height);
    } else {
        return g.roundedRect(position.x, position.y, width, height, roundness.x, roundness.y);
    }
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
    return g.colorizeShape(shape, fill, stroke, strokeWidth);
};

corevector.translate = function (shape, position) {
    var t = g.translate(g.IDENTITY, position.x, position.y);
    return g.transformShape(shape, t);
};

corevector.scale = function (shape, scale) {
    var t = g.scale(g.IDENTITY, scale.x / 100, scale.y / 100);
    return g.transformShape(shape, t);
};

corevector.rotate = function (shape, angle) {
    var t = g.rotate(g.IDENTITY, angle);
    return g.transformShape(shape, t);
};

corevector.skew = function (shape, skew, origin) {
    var t = g.translate(g.IDENTITY, origin.x, origin.y);
    t = g.skew(t, skew.x, skew.y);
    t = g.translate(t, -origin.x, -origin.y);
    return g.transformShape(shape, t);
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

    var wiggleShape = function (shape) {
        if (shape.elements) {
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
        } else if (shape.shapes) {
            return g.makeGroup(_.map(shape.shapes, wiggleShape));
        } else {
            return _.map(shape, wiggleShape);
        }
    }

    return wiggleShape(shape);
};

corevector.grid = function (columns, rows, width, height, position) {
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

corevector.reflect = function (shape, position, angle, keepOriginal) {
    if (shape == null) return null;

    function f(point) {
        var d = g.geometry.distance(point.x, point.y, position.x, position.y);
        var a = g.geometry.angle(point.x, point.y, position.x, position.y);
        var pt = g.geometry.coordinates(position.x, position.y, d * Math.cos(g.math.radians(a - angle)), 180 + angle);
        d = g.geometry.distance(point.x, point.y, pt.x, pt.y);
        a = g.geometry.angle(point.x, point.y, pt.x, pt.y);
        pt = g.geometry.coordinates(point.x, point.y, d * 2, a);
        return g.makePoint(pt.x, pt.y);
    }

    var elements = _.map(shape.elements, function(elem) {
        if (elem.cmd === g.CLOSE) return elem;
        else if (elem.cmd === g.MOVETO) {
            var pt = f(elem.point);
            return g.moveto(pt.x, pt.y);
        } else if (elem.cmd === g.LINETO) {
            var pt = f(elem.point);
            return g.lineto(pt.x, pt.y);
        } else if (elem.cmd === g.CURVETO) {
            var pt = f(elem.point);
            var ctrl1 = f(elem.ctrl1);
            var ctrl2 = f(elem.ctrl2);
            return g.curveto(ctrl1.x, ctrl1.y, ctrl2.x, ctrl2.y, pt.x, pt.y);
        }
    });
    var p = g.makePath(elements, shape.fill, shape.stroke, shape.strokeWidth);
    if (keepOriginal)
        return [shape, p];
    else
        return p;

};

corevector.copy = function (shape, copies, order, translate, rotate, scale) {
    var shapes = [];
    var tx = 0;
    var ty = 0;
    var r = 0;
    var sx = 1.0;
    var sy = 1.0;
    for (var i=0; i<copies; i++) {
        var t = g.IDENTITY;
        _.each(order, function(op) {
            if (op === 't')
                t = g.translate(t, tx, ty);
            else if (op === 'r')
                t = g.rotate(t, r);
            else if (op === 's')
                t = g.scale(t, sx, sy);
        });
        shapes.push(g.transformShape(shape, t));

        tx += translate.x;
        ty += translate.y;
        r += rotate;
        sx += scale.x;
        sy += scale.y;
    }
    return shapes;
};

corevector.connect = function (points, closed) {
    if (points == null) return null;
    var elements = [];
    for (var i=0; i<points.length; i++) {
        var pt = points[i];
        if (i === 0)
            elements.push(g.moveto(pt.x, pt.y));
        else
            elements.push(g.lineto(pt.x, pt.y));
    }

    if (closed)
        elements.push(g.closePath());

    return Object.freeze({
        elements: elements,
        fill: null,
        stroke: {"r": 0, "g": 0, "b": 0, "a": 1},
        strokeWidth: 1
    });
};

corevector.fit = function (shape, position, width, height, keepProportions) {
    if (shape == null) return null;
    var bounds = g.bounds(shape);
    var bx = bounds.x;
    var by = bounds.y;
    var bw = bounds.width;
    var bh = bounds.height;

    // Make sure bw and bh aren't infinitely small numbers.
    // This will lead to incorrect transformations with for examples lines.
    var bw = (bw > 0.000000000001) ? bw : 0;
    var bh = (bh > 0.000000000001) ? bh : 0;

    var t = g.IDENTITY;
    t = g.translate(t, position.x, position.y);
    var sx, sy;
    if (keepProportions) {
        // don't scale widths or heights that are equal to zero.
        sx = (bw > 0) ? (width / bw) : Number.MAX_VALUE;
        sy = (bh > 0) ? (height / bh) : Number.MAX_VALUE;
        sx = sy = Math.min(sx, sy);
    } else {
        sx = (bw > 0) ? (width / bw) : 1;
        sy = (bh > 0) ? (height / bh) : 1;
    }

    t = g.scale(t, sx, sy);
    t = g.translate(t, -bw / 2 - bx, -bh / 2 - by);

    return g.transformShape(shape, t)
};

corevector.fitTo = function (shape, bounding, keepProportions) {
    // Fit a shape to another shape.
    if (shape == null) return null;
    if (bounding == null) return shape;

    var bounds = g.bounds(bounding);

    var bx = bounds.x;
    var by = bounds.y;
    var bw = bounds.width;
    var bh = bounds.height;

    return corevector.fit(shape, {x: bx+bw/2, y: by+bh/2}, bw, bh, keepProportions);
};

corevector.fit_to = corevector.fitTo;

corevector.align = function (shape, position, hAlign, vAlign) {
    if (shape == null) return null;
    var x = position.x;
    var y = position.y;
    var bounds = g.bounds(shape);
    var dx, dy;
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

    var t = g.translate(g.IDENTITY, dx, dy);
    return g.transformShape(shape, t);
};

corevector.scatter = function (shape, amount, seed) {
    // Generate points within the boundaries of a shape.
    if (shape == null) return null;
    var rand = core.randomGenerator(seed);
    var bounds = g.bounds(shape);
    var bx = bounds.x;
    var by = bounds.y;
    var bw = bounds.width;
    var bh = bounds.height;
    var points = [];

    for (var i=0; i<amount; i++) {
        var tries = 100;
        while (tries > 0) {
            var x = bx + rand(0, 1) * bw;
            var y = by + rand(0, 1) * bh;
            if (g.pathContains(shape, x, y)) {
                points.push(g.makePoint(x, y));
                break;
            }
            tries -= 1;
        }
    }
    return points;
};

corevector.snap = function (shape, distance, strength, position) {
    // Snap geometry to a grid.

    function _snap(v, offset, distance, strength) {
        if (offset == null) offset = 0.0;
        if (distance == null) distance = 10.0;
        if (strength == null) strength = 1.0;
        return (v * (1.0-strength)) + (strength * Math.round(v / distance) * distance);
    }

    if (shape == null) return null;
    if (position == null) position = g.ZERO;
    strength /= 100.0;
    var elements = _.map(shape.elements, function(pe) {
        if (pe.cmd === g.CLOSE) return pe;
        else {
            var x = _snap(pe.point.x + position.x, position.x, distance, strength) - position.x;
            var y = _snap(pe.point.y + position.y, position.y, distance, strength) - position.y;
            if (pe.cmd === g.MOVETO)
                return g.moveto(x, y);
            else if (pe.cmd === g.LINETO)
                return g.lineto(x, y);
            else if (pe.cmd === g.CURVETO) {
                var ctrl1x = _snap(pe.ctrl1.x + position.x, position.x, distance, strength) - position.x;
                var ctrl1y = _snap(pe.ctrl1.y + position.y, position.y, distance, strength) - position.y;
                var ctrl2x = _snap(pe.ctrl2.x + position.x, position.x, distance, strength) - position.x;
                var ctrl2y = _snap(pe.ctrl2.y + position.y, position.y, distance, strength) - position.y;
                return g.curveto(ctrl1x, ctrl1y, ctrl2x, ctrl2y, x, y);
            }
        }
    });
    return g.makePath(elements, shape.fill, shape.stroke, shape.strokeWidth);
};

corevector.link = function (shape1, shape2, orientation) {
    if (shape1 == null || shape2 == null) return null;
    var a = g.bounds(shape1);
    var b = g.bounds(shape2);

    var elements;

    if (orientation === "horizontal") {
        var hw = (b.x - (a.x + a.width)) / 2;
        elements = [
            g.moveto(a.x + a.width, a.y),
            g.curveto(a.x + a.width + hw, a.y, b.x - hw, b.y, b.x, b.y),
            g.lineto(b.x, b.y + b.height),
            g.curveto(b.x - hw, b.y + b.height, a.x + a.width + hw, a.y + a.height, a.x + a.width, a.y + a.height)
        ];
    } else {
        var hh = (b.y - (a.y + a.height)) / 2;
        elements = [
            g.moveto(a.x, a.y + a.height),
            g.curveto(a.x, a.y + a.height + hh, b.x, b.y - hh, b.x, b.y),
            g.lineto(b.x + b.width, b.y),
            g.curveto(b.x + b.width, b.y - hh, a.x + a.width, a.y + a.height + hh, a.x + a.width, a.y + a.height)
        ];
    }
    return g.makePath(elements);
};

corevector.stack = function (shapes, direction, margin) {
    if (shapes == null) { return []; }
    if (shapes.length <= 1) {
        return shapes;
    }
    var first_bounds = g.bounds(shapes[0]);
    var new_shapes = [];
    if (direction === 'e') {
        var tx = -(first_bounds.width / 2);
        _.each(shapes, function(shape) {
            var t = g.IDENTITY;
            var bounds = g.bounds(shape);
            t = g.translate(t, tx - bounds.x, 0);
            new_shapes.push(g.transformShape(shape, t));
            tx += bounds.width + margin;
        });
    } else if (direction === 'w') {
        var tx = first_bounds.width / 2;
        _.each(shapes, function(shape) {
            var t = g.IDENTITY;
            var bounds = g.bounds(shape);
            t = g.translate(t, tx + bounds.x, 0);
            new_shapes.push(g.transformShape(shape, t));
            tx -= bounds.width + margin;
        });
    } else if (direction === 'n') {
        var ty = first_bounds.height / 2;
        _.each(shapes, function(shape) {
            var t = g.IDENTITY;
            var bounds = g.bounds(shape);
            t = g.translate(t, 0, ty + bounds.y);
            new_shapes.push(g.transformShape(shape, t));
            ty -= bounds.height + margin;
        });
    } else if (direction === 's') {
        var ty = -(first_bounds.height / 2);
        _.each(shapes, function(shape) {
            var t = g.IDENTITY;
            var bounds = g.bounds(shape);
            t = g.translate(t, 0, ty - bounds.y);
            new_shapes.push(g.transformShape(shape, t));
            ty += bounds.height + margin;
        });
    }
    return new_shapes;
};

corevector.pointOnPath = function (shape, t) {
    if (shape == null) return null;
    t = Math.abs(t % 100);
    return g.point(shape, t / 100).point;
};

corevector.shapeOnPath = function (shapes, path, amount, alignment, spacing, margin, baseline_offset) {
    if (! shapes) return [];
    if (path == null) return [];

    if (alignment === "trailing") {
        shapes = shapes.slice();
        shapes.reverse();
    }

    var length = g.length(path) - margin;
    var m = margin / g.length(path);
    var c = 0;

    var new_shapes = [];
    for (var i=0; i<amount; i++) {
        _.each(shapes, function(shape) {
            var pos;
            if (alignment === "distributed") {
                var p = length / ((amount * shapes.length) - 1);
                pos = c * p / length;
                pos = m + (pos * (1 - 2 * m));
            } else {
                pos = ((c * spacing) % length) / length;
                pos = m + (pos * (1 - m));

                if (alignment === "trailing")
                    pos = 1 - pos;
            }

            var p1 = g.point(path, pos).point;
            var p2 = g.point(path, pos + 0.0000001).point;
            var a = g.geometry.angle(p1.x, p1.y, p2.x, p2.y);
            if (baseline_offset)
                p1 = g.geometry.coordinates(p1.x, p1.y, baseline_offset, a - 90);
            var t = g.IDENTITY;
            t = g.translate(t, p1.x, p1.y);
            t = g.rotate(t, a);
            new_shapes.push(g.transformShape(shape, t));
            c += 1;
        });
    }
    return new_shapes;
};

corevector.shape_on_path = corevector.shapeOnPath;

corevector.importSVG = corevector.import_svg = function (file) {
    var parser, xmlDoc,
        svgString = ndbx.assets['images/' + file];

    if (window.DOMParser) {
        parser = new DOMParser();
        xmlDoc = parser.parseFromString(svgString, "image/svg+xml");
    } else { // Internet Explorer
        xmlDoc = new ActiveXObject("Microsoft.XMLDOM");
        xmlDoc.async = false;
        xmlDoc.loadXML(svgString);
    }
    return g.svg.interpret(xmlDoc.documentElement);
};

corevector.makePoint = function (x, y) {
    return {x: x, y: y};
};

corevector.point = function (v) {
    return v;
};

corevector.centroid = function (shape) {
    if (shape == null) return g.ZERO;
    var elements = shape.elements;
    var firstPoint = elements[0].point;
    var xs = firstPoint.x;
    var ys = firstPoint.y;
    var count = 1;
    for (var i = 1; i < elements.length; i++) {
        if (elements[i].point) {
            var pt = elements[i].point;
            xs += pt.x;
            ys += pt.y;
            count++;
        }
    }
    return g.makePoint(xs/count, ys/count);
};

corevector.group = function (shapes) {
    return g.makeGroup(shapes);
};

corevector.deletePoints = function (shape, bounding, deleteSelected) {
    var deletePoints = function (shape) {
        if (shape.elements) {
            var elems = [];
            for (var i=0; i<shape.elements.length; i++) {
                var elem = shape.elements[i];
                if (! elem.point ||
                    (deleteSelected && g.pathContains(bounding, elem.point.x, elem.point.y)) ||
                    (!deleteSelected && !g.pathContains(bounding, elem.point.x, elem.point.y)))  {
                    elems.push(elem);
                }
            }
            return g.makePath(elems, shape.fill, shape.stroke, shape.strokeWidth);
        } else if (shape.shapes) {
            return g.makeGroup(_.map(shape.shapes, deletePoints));
        } else {
            return _.map(shape, deletePoints);
        }
    }

    return deletePoints(shape);
};

corevector.deletePaths = function (shape, bounding, deleteSelected) {
    var deletePaths = function (shape) {
        if (shape.elements) {
            return null;
        } else if (shape.shapes) {
            var newShapes = [];
            for (var i = 0; i < shape.shapes.length; i++) {
                var s = shape.shapes[i];
                if (s.elements) {
                    var selected = false;
                    for (var j=0; j<s.elements.length; j++) {
                        var elem = s.elements[j];
                        if (elem.point && g.pathContains(bounding, elem.point.x, elem.point.y)) {
                            selected = true;
                            break;
                        }
                    }
                    if (selected !== deleteSelected) {
                        newShapes.push(s);
                    }
                } else if (s.shapes) {
                    var subshapes = deletePaths(s);
                    if (subshapes.length !== 0)
                        newShapes.push(subshapes);
                }
            }
            return g.makeGroup(newShapes);
        } else {
            return _.map(shape, deletePaths);
        }
    }

    return deletePaths(shape);
};

corevector["delete"] = function (shape, bounding, scope, operation) {
    if (shape == null || bounding == null) { return null; }
    var deleteSelected = operation === "selected";
    if (scope === "points") { return corevector.deletePoints(shape, bounding, deleteSelected); }
    if (scope === "paths") { return corevector.deletePaths(shape, bounding, deleteSelected); }
};
