# The NodeBox Graphics API

The graphics API is closely modelled on the SVG standard, with a few simplifications to make it more suitable for generative design. The API is divided into three main parts: shapes, colors, and transformations:

## Shapes

The `Shape` is the base class for all drawable objects in NodeBox. It is a container for paths, colors, and transformations. The Shape can not be instantiated by itself, but it has a number of common properties and methods that are available to all shape objects. These are:

- `shape.type`: The type of the shape. The type will always be in uppercase, like "RECTANGLE" or "ELLIPSE". These are the same as the SVG shape types.
- `shape.transform`: The transformation of the shape. This is an instance of the `Transform` class.
- `shape.fill`: The fill color of the shape.
- `shape.stroke`: The stroke color of the shape.
- `shape.strokeWidth`: The width of the stroke.
- `shape.opacity`: The opacity of the shape.

In addition there are some methods on the shape that are available for all shapes:

- `shape.clone()`: Returns a copy of the shape.
- `shape.applyTransform(transform)`: Applies a transformation to the shape.
- `shape.bakeTransform()`: "Bakes" the transformation into the shape, changing the coordinates of the shape to reflect the transformation. This will reset the transform to the identity matrix. (Note that for text, only the coordinates and font size are modified, rotation/skew is ignored.)
- `shape.getBounds()`: Returns the bounding box of the shape (width `left`, `right`, `top`, `bottom` and `centerX` and `centerY` attributes).
- `shape.toPathData(applyTransform)`: Returns the path data of the shape as a string. Use the optional `applyTransform` parameter to apply the current transform to the path data.
- `shape.getBounds()`: Returns the bounding box of the shape (width `left`, `right`, `top`, `bottom` and `centerX` and `centerY` attributes).

### Circle

The `Circle` class represents an [SVG circle](https://developer.mozilla.org/en-US/docs/Web/SVG/Element/circle). It has the following properties:

- `circle.cx`: The x-coordinate of the center of the circle.
- `circle.cy`: The y-coordinate of the center of the circle.
- `circle.radius`: The radius of the circle.

### Ellipse

The `Ellipse` class represents an [SVG ellipse](https://developer.mozilla.org/en-US/docs/Web/SVG/Element/ellipse). It has the following properties:

- `ellipse.cx`: The x-coordinate of the center of the ellipse.
- `ellipse.cy`: The y-coordinate of the center of the ellipse.
- `ellipse.rx`: The x-radius of the ellipse.
- `ellipse.ry`: The y-radius of the ellipse.

### Group

The `Group` class represents an [SVG group](https://developer.mozilla.org/en-US/docs/Web/SVG/Element/g). It is a container for other shapes. It has the following properties:

- `group.children`: An array of shapes that are children of the group.

Note that a group also has a `transform` attribute that applies to all its children.

### Line

The `Line` class represents an [SVG line](https://developer.mozilla.org/en-US/docs/Web/SVG/Element/line). It has the following properties:

- `line.x1`: The x-coordinate of the start of the line.
- `line.y1`: The y-coordinate of the start of the line.
- `line.x2`: The x-coordinate of the end of the line.
- `line.y2`: The y-coordinate of the end of the line.

A line has no fill color; use `stroke` and `strokeWidth` to set the line color and width.

### Path

The `Path` class represents an [SVG path](https://developer.mozilla.org/en-US/docs/Web/SVG/Element/path). It has the following properties:

- `path.verbs`: The list of "verbs" that make up the path. Each verb is a letter representing a command.
- `path.points`: The list of points that the verbs operate on. The points are always absolute coordinates.

To construct a path there a number of helper methods:

- `path.moveTo(x, y)`: Move the pen to the specified point.
- `path.lineTo(x, y)`: Draw a line to the specified point.
- `path.quadTo(x1, y1, x2, y2)`: Draw a quadratic bezier curve to the specified point.
- `path.cubicTo(x1, y1, x2, y2, x3, y3)`: Draw a cubic bezier curve to the specified point.
- `path.curveTo(x1, y1, x2, y2, x3, y3)`: Draw a cubic bezier curve to the specified point.
- `path.close()`: Close the path.

Another way to construct the path is to use path strings. A path string is a string that contains a series of commands and coordinates. Read the [MDN documentation on path commands](https://developer.mozilla.org/en-US/docs/Web/SVG/Attribute/d#path_commands) for more information.

To parse a path string, use `Path.fromPathData(pathData)`. This method returns a new path object. To get the path string from a path object, use `path.toPathData()`.

The path has a convenience method to draw simple shapes:

- `path.rect(x, y, width, height)`: Draw a rectangle at the specified position with the specified width and height.

### Rect

The `Rect` class represents an [SVG rectangle](https://developer.mozilla.org/en-US/docs/Web/SVG/Element/rect). It has the following properties:

- `rect.x`: The x-coordinate of the top-left corner of the rectangle.
- `rect.y`: The y-coordinate of the top-left corner of the rectangle.
- `rect.width`: The width of the rectangle.
- `rect.height`: The height of the rectangle.

### Text

The `Text` class represents an [SVG text element](https://developer.mozilla.org/en-US/docs/Web/SVG/Element/text). It has the following properties:

- `text.x`: The x-coordinate of the text.
- `text.y`: The y-coordinate of the text.
- `text.text`: The text to display.
- `text.fontSize`: The font size of the text.
- `text.fontFamily`: The font family of the text.
- `text.fontWeight`: The font weight of the text. ("normal", "bold" or a number between 100 and 900)
- `text.textAnchor`: The text anchor of the text. ("start", "middle" or "end")

## Transforms

The `Transform` class represents a 2D transformation matrix. It is used to translate, rotate or scale an object. It has a single property, `matrix`, that stores a 3x2 matrix. The matrix is an array of 6 numbers that represent the transformation:

```text
[ a, c, e,
  b, d, f ]
```

Normally, you don't have to think about the matrix directly. Instead, you can use the following methods to manipulate the transformation:

- `transform.translate(x, y)`: Translate the object by the specified x and y values.
- `transform.rotate(angle, around)`: Rotate the object by the specified angle in degrees. The optional `around` parameter specifies the point around which to rotate.
- `transform.scale(x, y, around)`: Scale the object by the specified x and y values. The optional `around` parameter specifies the point around which to scale.

To use the transformation on a shape, call `shape.applyTransform(transform)`. For a point use `transform.transformPoint(point)`.

If you want to apply the transform by changing the coordinates of the shape, you can _bake_ the transform. Call `shape.bakeTransform` to apply the _current_ transform. This will reset the transform to the identity matrix. For text shapes, the coordinates and font size are modified, but rotation/skew is ignored.

## Colors

A NodeBox shape can have different type of colors, which we call "paints". There are "solid" paints as well as gradient paints (linear and radial). There is also a special "none" paint, to indicate that the shape has no fill or stroke.

A solid paint can be created as follows: `Paint.solid(r, g, b, a)`, where `r`, `g`, `b` and `a` are numbers from 0 to 1. The `a` parameter is optional and defaults to 1.

You can also parse a color from a string using `Paint.parse`, e.g. `Paint.parse("#ff7f00")`.

### Solid Colors

A regular color is a "solid" paint, which has the following red, green, blue and alpha components, each values from 0 to 1. The color object has the following properties:

- `color.r`: The red component of the color.
- `color.g`: The green component of the color.
- `color.b`: The blue component of the color.
- `color.a`: The alpha component of the color.

A number of built-in colors are available:

- `Paint.black()`: Create a black color.
- `Paint.white()`: Create a white color.
- `Paint.lightGray()`: Create a light gray color.
- `Paint.darkGray()`: Create a dark gray color.
- `Paint.none()`: Create an undefined color (will show as `fill="none"`).
- `Paint.transparent()`: Create a transparent black color.

There are a number of conversion methods available. Imagine you've created a paint like this:

```js
const clr = Paint.solid(1, 0.498, 0, 0.5);
```

The following methods are available:

- `clr.isTransparent()`: Returns true if the color is transparent.
- `clr.toString()`: Returns a string representation of the color: either a hex string or a CSS color string.
- `clr.toObject()`: Convert a color to an object. To do the reverse use `Paint.parse`.

### Gradient Colors

To create gradients, you use the `Paint.linearGradient` and `Paint.radialGradient` methods. These methods take an array of stops, where each stop is an object with the following properties:

- `offset`: The offset of the stop, from 0 to 1.
- `color`: The color of the stop.

For example:

```js
const stops = [
  { offset: 0, color: Paint.solid(1, 0, 0, 1) },
  { offset: 0.5, color: Paint.solid(0, 1, 0, 1) },
  { offset: 1, color: Paint.solid(0, 0, 1, 1) },
];
const gradient = Paint.linearGradient(stops);
```
