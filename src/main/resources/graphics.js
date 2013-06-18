var g = {};

g.Rect = function (x, y, width, height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
};

g.Rect.prototype.containsPoint = function (x, y) {
    return (x >= this.x && x <= this.x + this.width && y >= this.y && y <= this.y + this.height);
};

// Enlarge the rectangle in all directions.
// The size of the rectangle will be width + dx * 2, height + dy * 2.
// This method changes the rectangle in place.
g.Rect.prototype.grow = function (dx, dy) {
    this.x -= dx;
    this.y -= dy;
    this.width += dx * 2;
    this.height += dy * 2;
};

g.moveTo = function (x, y) {
    return { cmd: 'M', x: x, y: y };
};

g.lineTo = function (x, y) {
    return { cmd: 'L', x: x, y: y };
};

g.curveTo = function (x1, y1, x2, y2, x, y) {
    return { cmd: 'C', x1: x1, y1: y1, x2: x2, y2: y2, x: x, y: y};
};

g.closePath = function () {
    return { cmd: 'z' };
};

g.drawCommand = function (ctx, command) {
    var cmd = command.cmd;
    if (cmd === 'M') {
        ctx.moveTo(command.x, command.y);
    } else if (cmd === 'L') {
        ctx.lineTo(command.x, command.y);
    } else if (cmd === 'C') {
        ctx.bezierCurveTo(command.x1, command.y1, command.x2, command.y2, command.x, command.y);
    } else if (cmd === 'z') {
        ctx.closePath();
    } else {
        console.log('Unknown command ', command);
    }
};

g.draw = function (ctx, shape) {
    try {
        if (_.isArray(shape)) {
            _.each(shape, _.partial(g.draw, ctx));
        } else {
            if (shape.fillStyle) {
                ctx.fillStyle = shape.fillStyle;
                ctx.beginPath();
                $.each(shape.commands, function (i, command) {
                    g.drawCommand(ctx, command);
                });
                ctx.fill();
            }
        }
    } catch (err) {
        console.log("Error while drawing:", err);
    }
};
