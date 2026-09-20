# Vector Graphics

NodeBox Live has great support for working with vector shapes such as paths, text, lines and points.

## Reference

### Basic Shapes

[rect](ref:g.rect) | [ellipse](ref:g.ellipse) | [polygon](ref:g.polygon) | [arc](ref:g.arc) | [star](ref:g.star)

[line](ref:g.line) — a line between two points

[lineAngle](ref:g.lineAngle) — a line with a starting point and an angle / distance

[curve](ref:g.curve) — smooth quadratic bézier curve

#### Text

[text](ref:g.text) — basic text

[textPath](ref:g.textPath) — text as a path object that can be manipulated

### Grids

[grid](ref:g.grid) — a rectangular grid of points

### Transforming Shapes

[align](ref:g.align) — align shapes in relation to the origin

[delete](ref:g.delete) — delete points or sub-paths in a shape

[fit](ref:g.fit) — resize shapes to fit given size

[mirror](ref:g.mirror) — mirror shapes

[translate](ref:g.translate) | [rotate](ref:g.rotate) | [scale](ref:g.scale) | [skew](ref:g.skew)

### Distorting Shapes

[resampleByLength](ref:g.resampleByLength) | [resampleByAmount](ref:g.resampleByAmount) — distribute points along the shape

[snap](ref:g.snap) — align points of the shape to an invisible grid

[wiggle](ref:g.wiggle) — randomly shift the points of a shape

### Combining Shapes

[merge](ref:g.merge) — combine multiple shapes into one

[compound](ref:g.compound) — add, subtract or intersect shapes

[copy](ref:g.copy) — make transformed copies

[fit](ref:g.fit) | [fitTo](ref:g.fitTo) — fit a shape within the bounds of another shape

[group](ref:g.group) | [ungroup](ref:g.ungroup) — combine shapes so they move together

[link](ref:g.link) — visually link two shapes together

[stack](ref:g.stack) — arrange shapes horizontally or vertically

[shapeSort](ref:g.shapeSort) — sort points/shapes by position or distance

### Points

[makePoint](ref:g.makePoint) — using X/Y coordinates

[toPoints](ref:g.toPoints) — extract points from the shape

[connectPoints](ref:g.connectPoints) — make a polygon from a list of points

[scatterPoints](ref:g.scatterPoints) — randomly place points within the shape

### Color

[colorize](ref:g.colorize) — set the color of the shape

[colorLookup](ref:g.colorLookup) — lookup a color component

[rgbColor](ref:g.rgbColor) | [hslColor](ref:g.hslColor) | [grayColor](ref:g.grayColor)

### Geometry

[bounds](ref:g.bounds) — calculate the bounding box

[centerPoint](ref:g.centerPoint) — get the center of the shape

[coordinates](ref:g.coordinates) — generate point based on distance / angle

[pointOnPath](ref:g.pointOnPath) — calculate a point on the path

[angle](ref:g.angle) | [distance](ref:g.distance)

### Importing Shapes

[import](ref:g.import) — import SVG files
