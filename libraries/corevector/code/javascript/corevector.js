var corevector = {};

corevector.generator = function () {
    return g.rect(0, 0, 100, 100);
};

corevector.filter = function (shape) {
    var t = new g.Transform().rotate(45);
    return t.transformShape(shape);
};

corevector.doNothing = function (shape) {
    return shape;
};

corevector.makePoint = g.makePoint;

corevector.point = function (v) {
    if (v.x && v.y) {
        return v;
    }
    if (v.elements) {
        return _.map(_.filter(v.elements, function (el) { if (el.point) { return true; } return false; }), function (el) { return el.point; });
    }
};

corevector.rect = g.rect;
corevector.ellipse = g.ellipse;
corevector.line = g.line;
corevector.lineAngle = corevector.line_angle = g.lineAngle;
corevector.arc = g.arc;
corevector.quadCurve = corevector.quad_curve = g.quadCurve;
corevector.polygon = g.polygon;
corevector.star = g.star;
corevector.freehand = g.freehand;
corevector.grid = g.grid;

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

corevector.colorize = g.colorize;
corevector.translate = g.translate;
corevector.scale = g.scale;
corevector.rotate = g.rotate;
corevector.skew = g.skew;
corevector.copy = g.copy;
corevector.fit = g.fit;
corevector.fitTo = corevector.fit_to = g.fitTo;
corevector.reflect = g.reflect;
corevector.resample = g.resample;
corevector.wiggle = g.wiggle;
corevector.scatter = g.scatter;
corevector.connect = g.connect;
corevector.align = g.align;
corevector.snap = g.snap;
corevector["delete"] = g["delete"];
corevector.pointOnPath = g.pointOnPath;
corevector.shapeOnPath = corevector.shape_on_path = g.shapeOnPath;
corevector.sort = g.sort;
corevector.group = g.group;
corevector.ungroup = g.ungroup;
corevector.centroid = g.centroid;
corevector.link = g.link;
corevector.stack = g.stack;