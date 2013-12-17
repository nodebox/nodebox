var corevector = {};

corevector.generator = function () {
    return g.graphics.rect(0, 0, 100, 100);
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

corevector.rect = g.shapes.rect;
corevector.ellipse = g.shapes.ellipse;
corevector.line = g.shapes.line;
corevector.lineAngle = corevector.line_angle = g.shapes.lineAngle;
corevector.arc = g.shapes.arc;
corevector.quadCurve = corevector.quad_curve = g.shapes.quadCurve;
corevector.polygon = g.shapes.polygon;
corevector.star = g.shapes.star;
corevector.freehand = g.shapes.freehand;
corevector.grid = g.shapes.grid;

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

corevector.colorize = g.filters.colorize;
corevector.translate = g.filters.translate;
corevector.scale = g.filters.scale;
corevector.rotate = g.filters.rotate;
corevector.skew = g.filters.skew;
corevector.copy = g.filters.copy;
corevector.fit = g.filters.fit;
corevector.fitTo = corevector.fit_to = g.filters.fitTo;
corevector.reflect = g.filters.reflect;
corevector.resample = g.filters.resample;
corevector.wiggle = g.filters.wiggle;
corevector.scatter = g.filters.scatter;
corevector.connect = g.filters.connect;
corevector.align = g.filters.align;
corevector.snap = g.filters.snap;
corevector["delete"] = g.filters["delete"];
corevector.pointOnPath = g.filters.pointOnPath;
corevector.shapeOnPath = corevector.shape_on_path = g.filters.shapeOnPath;
corevector.sort = g.filters.sort;
corevector.group = g.makeGroup;
corevector.ungroup = g.filters.ungroup;
corevector.centroid = g.filters.centroid;
corevector.link = g.filters.link;
corevector.stack = g.filters.stack;









