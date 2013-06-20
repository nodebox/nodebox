var corevector = {};

corevector.rect = function (position, width, height) {
    var x0 = position.x - width / 2;
    var y0 = position.y - height / 2;
    var x1 = position.x + width / 2;
    var y1 = position.y + height / 2;
    return {
        fillStyle: 'black',
        commands: [
            g.moveTo(x0, y0),
            g.lineTo(x1, y0),
            g.lineTo(x1, y1),
            g.lineTo(x0, y1),
            g.closePath()
        ]
    };
};

corevector.ellipse = function (position, width, height) {
    var cx = position.x;
    var cy = position.y;

    var kappa = 0.5522848;
    var ox = (width / 2) * kappa; // control point offset horizontal
    var oy = (height / 2) * kappa; // control point offset vertical
    var left = cx - width / 2;
    var top = cy - height / 2;
    var right = cx + width / 2;
    var bottom = cy + height / 2;
    return {
        fillStyle: 'black',
        commands: [
            g.moveTo(left, cy),
            g.curveTo(left, cy - oy, cx - ox, top, cx, top),
            g.curveTo(cx + ox, top, right, cy - oy, right, cy),
            g.curveTo(right, cy + oy, cx + ox, bottom, cx, bottom),
            g.curveTo(cx - ox, bottom, left, cy + oy, left, cy),
            g.closePath()
        ]
    };
};

corevector.makePoint = function(x, y) {
    return {x: x, y: y};
};