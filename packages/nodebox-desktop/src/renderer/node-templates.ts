import type { Node, Port, Value } from 'nodebox-core';

// Full node templates with ports, function names, and defaults.
// These mirror the definitions from the library .ndbx files.

interface NodeTemplate {
  name: string;
  category: string;
  description: string;
  function: string;
  outputType: string;
  outputRange?: 'value' | 'list';
  inputs: Port[];
}

function port(name: string, type: Port['type'], value: Value, opts?: Partial<Port>): Port {
  return {
    name,
    type,
    label: opts?.label ?? null,
    description: opts?.description ?? null,
    widget: opts?.widget ?? defaultWidget(type),
    range: opts?.range ?? 'value',
    value,
    minimumValue: opts?.minimumValue ?? null,
    maximumValue: opts?.maximumValue ?? null,
    menuItems: opts?.menuItems ?? [],
    childReference: null,
  };
}

function defaultWidget(type: Port['type']): Port['widget'] {
  switch (type) {
    case 'int': return 'int';
    case 'float': return 'float';
    case 'string': return 'string';
    case 'boolean': return 'toggle';
    case 'point': return 'point';
    case 'color': return 'color';
    default: return 'none';
  }
}

const f = (v: number): Value => ({ type: 'float', value: v });
const i = (v: number): Value => ({ type: 'int', value: v });
const s = (v: string): Value => ({ type: 'string', value: v });
const b = (v: boolean): Value => ({ type: 'boolean', value: v });
const pt = (x: number, y: number): Value => ({ type: 'point', value: { x, y } });
const col = (r: number, g: number, b: number, a: number): Value => ({ type: 'color', value: { r, g, b, a } });

export const NODE_TEMPLATES: NodeTemplate[] = [
  // ─── Core ───────────────────────────────────────
  {
    name: 'frame', category: 'Core', description: 'Output the current frame number for animation.', function: 'core/frame', outputType: 'float',
    inputs: [port('context', 'context', { type: 'null' }, { widget: 'none' })],
  },

  // ─── Generators ────────────────────────────────
  {
    name: 'rect', category: 'Geometry', description: 'Create a rectangle or rounded rectangle.', function: 'corevector/rect', outputType: 'geometry',
    inputs: [
      port('position', 'point', pt(0, 0)),
      port('width', 'float', f(100)),
      port('height', 'float', f(100)),
      port('roundness', 'point', pt(0, 0)),
    ],
  },
  {
    name: 'ellipse', category: 'Geometry', description: 'Create an ellipse or circle.', function: 'corevector/ellipse', outputType: 'geometry',
    inputs: [
      port('position', 'point', pt(0, 0)),
      port('width', 'float', f(100)),
      port('height', 'float', f(100)),
    ],
  },
  {
    name: 'polygon', category: 'Geometry', description: 'Create a regular polygon.', function: 'corevector/polygon', outputType: 'geometry',
    inputs: [
      port('position', 'point', pt(0, 0)),
      port('radius', 'float', f(100), { minimumValue: 0 }),
      port('sides', 'int', i(3), { minimumValue: 3 }),
      port('align', 'boolean', b(false)),
    ],
  },
  {
    name: 'star', category: 'Geometry', description: 'Create a star shape.', function: 'corevector/star', outputType: 'geometry',
    inputs: [
      port('position', 'point', pt(0, 0)),
      port('points', 'int', i(20), { minimumValue: 1 }),
      port('outer', 'float', f(200), { label: 'Outer Diameter' }),
      port('inner', 'float', f(100), { label: 'Inner Diameter' }),
    ],
  },
  {
    name: 'arc', category: 'Geometry', description: 'Create an arc, pie, or chord.', function: 'corevector/arc', outputType: 'geometry',
    inputs: [
      port('position', 'point', pt(0, 0)),
      port('width', 'float', f(100)),
      port('height', 'float', f(100)),
      port('start_angle', 'float', f(0), { label: 'Start Angle' }),
      port('degrees', 'float', f(45)),
      port('type', 'string', s('pie'), { widget: 'menu', menuItems: [{ key: 'pie', label: 'Pie' }, { key: 'chord', label: 'Chord' }, { key: 'open', label: 'Open' }] }),
    ],
  },
  {
    name: 'line', category: 'Geometry', description: 'Create a line between two points.', function: 'corevector/line', outputType: 'geometry',
    inputs: [
      port('point1', 'point', pt(0, 0), { label: 'Point 1' }),
      port('point2', 'point', pt(100, 100), { label: 'Point 2' }),
      port('points', 'int', i(2), { minimumValue: 2 }),
    ],
  },
  {
    name: 'lineAngle', category: 'Geometry', description: 'Create a line from a point, angle and distance.', function: 'corevector/lineAngle', outputType: 'geometry',
    inputs: [
      port('position', 'point', pt(0, 0)),
      port('angle', 'float', f(0)),
      port('distance', 'float', f(100)),
      port('points', 'int', i(2), { minimumValue: 2 }),
    ],
  },
  {
    name: 'grid', category: 'Geometry', description: 'Create a grid of points.', function: 'corevector/grid', outputType: 'point', outputRange: 'list',
    inputs: [
      port('columns', 'int', i(10), { minimumValue: 1 }),
      port('rows', 'int', i(10), { minimumValue: 1 }),
      port('width', 'float', f(300)),
      port('height', 'float', f(300)),
      port('position', 'point', pt(0, 0)),
    ],
  },
  {
    name: 'connect', category: 'Geometry', description: 'Connect points into a path.', function: 'corevector/connect', outputType: 'geometry',
    inputs: [
      port('points', 'list', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('closed', 'boolean', b(true)),
    ],
  },
  {
    name: 'makePoint', category: 'Geometry', description: 'Create a point from X/Y coordinates.', function: 'corevector/makePoint', outputType: 'point',
    inputs: [
      port('x', 'float', f(0)),
      port('y', 'float', f(0)),
    ],
  },
  {
    name: 'point', category: 'Geometry', description: 'A point value variable.', function: 'corevector/point', outputType: 'point',
    inputs: [port('value', 'point', pt(0, 0))],
  },
  {
    name: 'quadCurve', category: 'Geometry', description: 'Create a quadratic curve with one off-curve point.', function: 'corevector/quadCurve', outputType: 'geometry',
    inputs: [
      port('point1', 'point', pt(0, 0), { label: 'Point 1' }),
      port('point2', 'point', pt(100, 0), { label: 'Point 2' }),
      port('t', 'float', f(50)),
      port('distance', 'float', f(50)),
    ],
  },
  {
    name: 'link', category: 'Geometry', description: 'Generate a visual link between two shapes.', function: 'corevector/link', outputType: 'geometry',
    inputs: [
      port('shape1', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('shape2', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('orientation', 'string', s('horizontal'), { widget: 'menu', menuItems: [{ key: 'horizontal', label: 'Horizontal' }, { key: 'vertical', label: 'Vertical' }] }),
    ],
  },
  {
    name: 'freehand', category: 'Geometry', description: 'Draw directly on the canvas using the mouse.', function: 'corevector/freehand', outputType: 'geometry',
    inputs: [port('path', 'string', s(''), { widget: 'data' })],
  },
  {
    name: 'group', category: 'Geometry', description: 'Combine multiple shapes into one geometry.', function: 'corevector/group', outputType: 'geometry',
    inputs: [port('shapes', 'geometry', { type: 'null' }, { range: 'list', widget: 'none' })],
  },

  // ─── Filters ───────────────────────────────────
  {
    name: 'colorize', category: 'Style', description: 'Change fill and stroke color.', function: 'corevector/colorize', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('fill', 'color', col(0, 0, 0, 1)),
      port('stroke', 'color', col(0, 0, 0, 1)),
      port('strokeWidth', 'float', f(0), { label: 'Stroke Width', minimumValue: 0 }),
    ],
  },
  {
    name: 'translate', category: 'Transform', description: 'Move a shape.', function: 'corevector/translate', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('translate', 'point', pt(0, 0)),
    ],
  },
  {
    name: 'rotate', category: 'Transform', description: 'Rotate a shape.', function: 'corevector/rotate', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('angle', 'float', f(0)),
      port('origin', 'point', pt(0, 0)),
    ],
  },
  {
    name: 'scale', category: 'Transform', description: 'Resize a shape by scaling it.', function: 'corevector/scale', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('scale', 'point', pt(100, 100)),
      port('origin', 'point', pt(0, 0)),
    ],
  },
  {
    name: 'copy', category: 'Transform', description: 'Create multiple transformed copies.', function: 'corevector/copy', outputType: 'geometry', outputRange: 'list',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('copies', 'int', i(1), { minimumValue: 1 }),
      port('order', 'string', s('tsr'), { widget: 'menu', menuItems: [{ key: 'srt', label: 'Scale Rot Trans' }, { key: 'str', label: 'Scale Trans Rot' }, { key: 'rst', label: 'Rot Scale Trans' }, { key: 'rts', label: 'Rot Trans Scale' }, { key: 'tsr', label: 'Trans Scale Rot' }, { key: 'trs', label: 'Trans Rot Scale' }] }),
      port('translate', 'point', pt(0, 0)),
      port('rotate', 'float', f(0)),
      port('scale', 'point', pt(100, 100)),
    ],
  },
  {
    name: 'align', category: 'Transform', description: 'Align a shape in relation to the origin.', function: 'corevector/align', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('position', 'point', pt(0, 0)),
      port('halign', 'string', s('center'), { label: 'Horizontal Align', widget: 'menu', menuItems: [{ key: 'none', label: 'No Change' }, { key: 'left', label: 'Left' }, { key: 'center', label: 'Center' }, { key: 'right', label: 'Right' }] }),
      port('valign', 'string', s('middle'), { label: 'Vertical Align', widget: 'menu', menuItems: [{ key: 'none', label: 'No Change' }, { key: 'top', label: 'Top' }, { key: 'middle', label: 'Middle' }, { key: 'bottom', label: 'Bottom' }] }),
    ],
  },
  {
    name: 'fit', category: 'Transform', description: 'Fit a shape within bounds.', function: 'corevector/fit', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('position', 'point', pt(0, 0)),
      port('width', 'float', f(300)),
      port('height', 'float', f(300)),
      port('keep_proportions', 'boolean', b(true), { label: 'Keep Proportions' }),
    ],
  },
  {
    name: 'fitTo', category: 'Transform', description: 'Fit a shape to another shape.', function: 'corevector/fitTo', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('bounding', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('keep_proportions', 'boolean', b(true), { label: 'Keep Proportions' }),
    ],
  },
  {
    name: 'reflect', category: 'Transform', description: 'Mirror a shape around an axis.', function: 'corevector/reflect', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('position', 'point', pt(0, 0)),
      port('angle', 'float', f(120)),
      port('keep_original', 'boolean', b(true), { label: 'Keep Original' }),
    ],
  },
  {
    name: 'skew', category: 'Transform', description: 'Skew a shape.', function: 'corevector/skew', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('skew', 'point', pt(0, 0)),
      port('origin', 'point', pt(0, 0)),
    ],
  },
  {
    name: 'snap', category: 'Transform', description: 'Snap points to a grid.', function: 'corevector/snap', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('distance', 'float', f(10), { minimumValue: 1 }),
      port('strength', 'float', f(100), { minimumValue: 0, maximumValue: 100 }),
      port('position', 'point', pt(0, 0)),
    ],
  },
  {
    name: 'resample', category: 'Style', description: 'Redistribute points along a path.', function: 'corevector/resample', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('method', 'string', s('length'), { widget: 'menu', menuItems: [{ key: 'length', label: 'By Length' }, { key: 'amount', label: 'By Amount' }] }),
      port('length', 'float', f(10), { minimumValue: 1 }),
      port('points', 'int', i(10), { minimumValue: 1 }),
      port('per_contour', 'boolean', b(false), { label: 'Per Contour' }),
    ],
  },
  {
    name: 'wiggle', category: 'Style', description: 'Randomly displace points.', function: 'corevector/wiggle', outputType: 'geometry', outputRange: 'list',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('scope', 'string', s('points'), { widget: 'menu', menuItems: [{ key: 'points', label: 'Points' }, { key: 'contours', label: 'Contours' }, { key: 'paths', label: 'Paths' }] }),
      port('offset', 'point', pt(10, 10)),
      port('seed', 'int', i(0)),
    ],
  },
  {
    name: 'compound', category: 'Style', description: 'Boolean operations on shapes.', function: 'corevector/compound', outputType: 'geometry',
    inputs: [
      port('shape1', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('shape2', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('function', 'string', s('united'), { widget: 'menu', menuItems: [{ key: 'united', label: 'Union' }, { key: 'subtracted', label: 'Difference' }, { key: 'intersected', label: 'Intersection' }] }),
      port('invert_difference', 'boolean', b(false), { label: 'Invert' }),
    ],
  },
  {
    name: 'ungroup', category: 'Style', description: 'Decompose geometry into separate paths.', function: 'corevector/ungroup', outputType: 'geometry', outputRange: 'list',
    inputs: [port('shape', 'geometry', { type: 'null' }, { widget: 'none' })],
  },
  {
    name: 'sort', category: 'Style', description: 'Sort shapes by position or angle.', function: 'corevector/sort', outputType: 'geometry', outputRange: 'list',
    inputs: [
      port('shapes', 'geometry', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('order_by', 'string', s('none'), { label: 'Order By', widget: 'menu', menuItems: [{ key: 'none', label: 'No Change' }, { key: 'x', label: 'X' }, { key: 'y', label: 'Y' }, { key: 'angle', label: 'Angle to Point' }, { key: 'distance', label: 'Distance to Point' }] }),
      port('position', 'point', pt(0, 0)),
    ],
  },
  {
    name: 'stack', category: 'Style', description: 'Arrange shapes in a layout.', function: 'corevector/stack', outputType: 'geometry', outputRange: 'list',
    inputs: [
      port('shapes', 'geometry', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('direction', 'string', s('e'), { widget: 'menu', menuItems: [{ key: 'n', label: 'North' }, { key: 'e', label: 'East' }, { key: 's', label: 'South' }, { key: 'w', label: 'West' }] }),
      port('margin', 'float', f(5)),
    ],
  },
  {
    name: 'scatter', category: 'Style', description: 'Generate random points in a shape.', function: 'corevector/scatter', outputType: 'point', outputRange: 'list',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('amount', 'int', i(20), { minimumValue: 0 }),
      port('seed', 'int', i(0)),
    ],
  },
  {
    name: 'centroid', category: 'Style', description: 'Calculate the geometric center of a shape.', function: 'corevector/centroid', outputType: 'point',
    inputs: [port('shape', 'geometry', { type: 'null' }, { widget: 'none' })],
  },
  {
    name: 'pointOnPath', category: 'Style', description: 'Calculate a point on a path.', function: 'corevector/pointOnPath', outputType: 'point',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('t', 'float', f(0)),
    ],
  },
  {
    name: 'distribute', category: 'Style', description: 'Distribute shapes on an axis.', function: 'corevector/distribute', outputType: 'geometry', outputRange: 'list',
    inputs: [
      port('shapes', 'geometry', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('horizontal', 'string', s('none'), { widget: 'menu', menuItems: [{ key: 'none', label: 'No Change' }, { key: 'left', label: 'Left' }, { key: 'center', label: 'Center' }, { key: 'right', label: 'Right' }] }),
      port('vertical', 'string', s('none'), { widget: 'menu', menuItems: [{ key: 'none', label: 'No Change' }, { key: 'top', label: 'Top' }, { key: 'middle', label: 'Middle' }, { key: 'bottom', label: 'Bottom' }] }),
    ],
  },
  {
    name: 'delete', category: 'Style', description: 'Delete points or paths within a bounding shape.', function: 'corevector/delete', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('bounding', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('scope', 'string', s('points'), { widget: 'menu', menuItems: [{ key: 'points', label: 'Points' }, { key: 'paths', label: 'Paths' }] }),
      port('operation', 'string', s('selected'), { widget: 'menu', menuItems: [{ key: 'selected', label: 'Delete Selected' }, { key: 'non-selected', label: 'Delete Non-selected' }] }),
    ],
  },
  {
    name: 'roundSegments', category: 'Style', description: 'Round the corners of a shape.', function: 'corevector/roundSegments', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('d', 'float', f(10)),
    ],
  },
  {
    name: 'shapeOnPath', category: 'Style', description: 'Copy shapes along a path.', function: 'corevector/shapeOnPath', outputType: 'geometry', outputRange: 'list',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('path', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('amount', 'int', i(1), { minimumValue: 0 }),
      port('alignment', 'string', s('leading'), { widget: 'menu', menuItems: [{ key: 'leading', label: 'Leading' }, { key: 'trailing', label: 'Trailing' }, { key: 'distributed', label: 'Distributed' }] }),
      port('spacing', 'float', f(20), { minimumValue: 0 }),
      port('margin', 'float', f(0), { minimumValue: 0 }),
      port('baseline_offset', 'float', f(0), { label: 'Baseline Offset' }),
    ],
  },
  {
    name: 'doNothing', category: 'Style', description: 'Pass through without changes.', function: 'corevector/doNothing', outputType: 'geometry',
    inputs: [port('shape', 'geometry', { type: 'null' }, { widget: 'none' })],
  },

  // ─── Math ──────────────────────────────────────
  {
    name: 'number', category: 'Math', description: 'A floating-point number variable.', function: 'math/number', outputType: 'float',
    inputs: [port('value', 'float', f(0))],
  },
  {
    name: 'integer', category: 'Math', description: 'An integer number variable.', function: 'math/integer', outputType: 'int',
    inputs: [port('value', 'int', i(0))],
  },
  {
    name: 'boolean', category: 'Math', description: 'A boolean variable.', function: 'math/boolean', outputType: 'boolean',
    inputs: [port('value', 'boolean', b(false))],
  },
  {
    name: 'add', category: 'Math', description: 'Add two numbers.', function: 'math/add', outputType: 'float',
    inputs: [port('value1', 'float', f(0)), port('value2', 'float', f(0))],
  },
  {
    name: 'subtract', category: 'Math', description: 'Subtract two numbers.', function: 'math/subtract', outputType: 'float',
    inputs: [port('value1', 'float', f(0)), port('value2', 'float', f(0))],
  },
  {
    name: 'multiply', category: 'Math', description: 'Multiply two numbers.', function: 'math/multiply', outputType: 'float',
    inputs: [port('value1', 'float', f(0)), port('value2', 'float', f(1))],
  },
  {
    name: 'divide', category: 'Math', description: 'Divide two numbers.', function: 'math/divide', outputType: 'float',
    inputs: [port('value1', 'float', f(0)), port('value2', 'float', f(1))],
  },
  {
    name: 'mod', category: 'Math', description: 'Calculate the remainder of a division.', function: 'math/mod', outputType: 'float',
    inputs: [port('value1', 'float', f(0)), port('value2', 'float', f(1))],
  },
  {
    name: 'negate', category: 'Math', description: 'Change the sign of a number.', function: 'math/negate', outputType: 'float',
    inputs: [port('value', 'float', f(0))],
  },
  {
    name: 'abs', category: 'Math', description: 'Convert to a positive number.', function: 'math/abs', outputType: 'float',
    inputs: [port('value', 'float', f(0))],
  },
  {
    name: 'round', category: 'Math', description: 'Round a number to the nearest integer.', function: 'math/round', outputType: 'int',
    inputs: [port('value', 'float', f(0))],
  },
  {
    name: 'ceil', category: 'Math', description: 'Round up to the nearest integer.', function: 'math/ceil', outputType: 'int',
    inputs: [port('value', 'float', f(0))],
  },
  {
    name: 'floor', category: 'Math', description: 'Round down to the nearest integer.', function: 'math/floor', outputType: 'int',
    inputs: [port('value', 'float', f(0))],
  },
  {
    name: 'pow', category: 'Math', description: 'Calculate the power of a number.', function: 'math/pow', outputType: 'float',
    inputs: [port('value1', 'float', f(0)), port('value2', 'float', f(0))],
  },
  {
    name: 'sqrt', category: 'Math', description: 'Calculate the square root.', function: 'math/sqrt', outputType: 'float',
    inputs: [port('value', 'float', f(0))],
  },
  {
    name: 'log', category: 'Math', description: 'Calculate the natural logarithm.', function: 'math/log', outputType: 'float',
    inputs: [port('value', 'float', f(1), { minimumValue: 0.01 })],
  },
  {
    name: 'sin', category: 'Math', description: 'Calculate the sine of an angle.', function: 'math/sin', outputType: 'float',
    inputs: [port('value', 'float', f(0))],
  },
  {
    name: 'cos', category: 'Math', description: 'Calculate the cosine of an angle.', function: 'math/cos', outputType: 'float',
    inputs: [port('value', 'float', f(0))],
  },
  {
    name: 'radians', category: 'Math', description: 'Convert degrees to radians.', function: 'math/radians', outputType: 'float',
    inputs: [port('degrees', 'float', f(0))],
  },
  {
    name: 'degrees', category: 'Math', description: 'Convert radians to degrees.', function: 'math/degrees', outputType: 'float',
    inputs: [port('radians', 'float', f(0))],
  },
  {
    name: 'pi', category: 'Math', description: 'The constant pi (3.14159...).', function: 'math/pi', outputType: 'float',
    inputs: [],
  },
  {
    name: 'e', category: 'Math', description: 'The constant e (2.71828...).', function: 'math/e', outputType: 'float',
    inputs: [],
  },
  {
    name: 'even', category: 'Math', description: 'Determine if a number is even.', function: 'math/even', outputType: 'boolean',
    inputs: [port('value', 'int', i(0))],
  },
  {
    name: 'odd', category: 'Math', description: 'Determine if a number is odd.', function: 'math/odd', outputType: 'boolean',
    inputs: [port('value', 'int', i(0))],
  },
  {
    name: 'min', category: 'Math', description: 'Select the smallest value from a list.', function: 'math/min', outputType: 'float',
    inputs: [port('values', 'float', f(0), { range: 'list' })],
  },
  {
    name: 'max', category: 'Math', description: 'Select the largest value from a list.', function: 'math/max', outputType: 'float',
    inputs: [port('values', 'float', f(0), { range: 'list' })],
  },
  {
    name: 'average', category: 'Math', description: 'Calculate the average of a list.', function: 'math/average', outputType: 'float',
    inputs: [port('values', 'float', f(0), { range: 'list' })],
  },
  {
    name: 'sum', category: 'Math', description: 'Add up all numbers in a list.', function: 'math/sum', outputType: 'float',
    inputs: [port('values', 'float', f(0), { range: 'list' })],
  },
  {
    name: 'compare', category: 'Math', description: 'Compare two values.', function: 'math/compare', outputType: 'boolean',
    inputs: [
      port('value1', 'float', f(0)),
      port('value2', 'float', f(0)),
      port('comparator', 'string', s('<'), { widget: 'menu', menuItems: [{ key: '<', label: '< less than' }, { key: '>', label: '> greater than' }, { key: '<=', label: '<= less or equal' }, { key: '>=', label: '>= greater or equal' }, { key: '==', label: '== equal' }, { key: '!=', label: '!= not equal' }] }),
    ],
  },
  {
    name: 'logicOperator', category: 'Math', description: 'Logical OR, AND, XOR.', function: 'math/logicOperator', outputType: 'boolean',
    inputs: [
      port('boolean1', 'boolean', b(false)),
      port('boolean2', 'boolean', b(false)),
      port('comparator', 'string', s('or'), { widget: 'menu', menuItems: [{ key: 'or', label: 'OR' }, { key: 'and', label: 'AND' }, { key: 'xor', label: 'XOR' }] }),
    ],
  },
  {
    name: 'convertRange', category: 'Math', description: 'Convert a value from one range to another.', function: 'math/convertRange', outputType: 'float',
    inputs: [
      port('value', 'float', f(50)),
      port('source_start', 'float', f(0), { label: 'Source Start' }),
      port('source_end', 'float', f(100), { label: 'Source End' }),
      port('target_start', 'float', f(0), { label: 'Target Start' }),
      port('target_end', 'float', f(1), { label: 'Target End' }),
      port('method', 'string', s('clamp'), { widget: 'menu', menuItems: [{ key: 'clamp', label: 'Clamp' }, { key: 'mirror', label: 'Mirror' }, { key: 'wrap', label: 'Wrap' }, { key: 'ignore', label: 'Ignore' }] }),
    ],
  },
  {
    name: 'coordinates', category: 'Math', description: 'Calculate a point from angle and distance.', function: 'math/coordinates', outputType: 'point',
    inputs: [
      port('position', 'point', pt(0, 0)),
      port('angle', 'float', f(0)),
      port('distance', 'float', f(100)),
    ],
  },
  {
    name: 'angle', category: 'Math', description: 'Calculate the angle between two points.', function: 'math/angle', outputType: 'float',
    inputs: [port('point1', 'point', pt(0, 0)), port('point2', 'point', pt(100, 100))],
  },
  {
    name: 'distance', category: 'Math', description: 'Calculate the distance between two points.', function: 'math/distance', outputType: 'float',
    inputs: [port('point1', 'point', pt(0, 0)), port('point2', 'point', pt(100, 100))],
  },
  {
    name: 'reflectPoint', category: 'Math', description: 'Reflect a point around another.', function: 'math/reflect', outputType: 'point',
    inputs: [
      port('point1', 'point', pt(0, 0)),
      port('point2', 'point', pt(100, 100)),
      port('angle', 'float', f(0)),
      port('distance', 'float', f(1)),
    ],
  },
  {
    name: 'range', category: 'Math', description: 'Generate a range of numbers.', function: 'math/range', outputType: 'float', outputRange: 'list',
    inputs: [port('start', 'float', f(0)), port('end', 'float', f(10)), port('step', 'float', f(1))],
  },
  {
    name: 'sample', category: 'Math', description: 'Generate evenly spaced numbers.', function: 'math/sample', outputType: 'float', outputRange: 'list',
    inputs: [port('amount', 'int', i(10), { minimumValue: 0 }), port('start', 'float', f(0)), port('end', 'float', f(100))],
  },
  {
    name: 'randomNumbers', category: 'Math', description: 'Generate random numbers.', function: 'math/randomNumbers', outputType: 'float', outputRange: 'list',
    inputs: [port('amount', 'int', i(10), { minimumValue: 0 }), port('start', 'float', f(0)), port('end', 'float', f(100)), port('seed', 'int', i(0))],
  },
  {
    name: 'makeNumbers', category: 'Math', description: 'Parse a string into a list of numbers.', function: 'math/makeNumbers', outputType: 'float', outputRange: 'list',
    inputs: [port('string', 'string', s('11;22;33')), port('separator', 'string', s(';'))],
  },
  {
    name: 'runningTotal', category: 'Math', description: 'Generate running totals of a list.', function: 'math/runningTotal', outputType: 'float', outputRange: 'list',
    inputs: [port('values', 'float', f(0), { range: 'list' })],
  },
  {
    name: 'wave', category: 'Math', description: 'Calculate a value based on wave equations.', function: 'math/wave', outputType: 'float',
    inputs: [
      port('min', 'float', f(0)),
      port('max', 'float', f(100)),
      port('period', 'float', f(60)),
      port('offset', 'float', f(0)),
      port('type', 'string', s('sine'), { widget: 'menu', menuItems: [{ key: 'sine', label: 'Sine' }, { key: 'square', label: 'Square' }, { key: 'triangle', label: 'Triangle' }, { key: 'sawtooth', label: 'Sawtooth' }] }),
    ],
  },

  // ─── List ──────────────────────────────────────
  {
    name: 'combine', category: 'List', description: 'Merge multiple lists into one.', function: 'list/combine', outputType: 'list', outputRange: 'list',
    inputs: [
      port('list1', 'list', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('list2', 'list', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('list3', 'list', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('list4', 'list', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('list5', 'list', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('list6', 'list', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('list7', 'list', { type: 'null' }, { range: 'list', widget: 'none' }),
    ],
  },
  {
    name: 'count', category: 'List', description: 'Count items in a list.', function: 'list/count', outputType: 'int',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' })],
  },
  {
    name: 'first', category: 'List', description: 'Take the first item of a list.', function: 'list/first', outputType: 'data',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' })],
  },
  {
    name: 'second', category: 'List', description: 'Take the second item of a list.', function: 'list/second', outputType: 'data',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' })],
  },
  {
    name: 'last', category: 'List', description: 'Take the last item of a list.', function: 'list/last', outputType: 'data',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' })],
  },
  {
    name: 'rest', category: 'List', description: 'All but the first item of a list.', function: 'list/rest', outputType: 'list', outputRange: 'list',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' })],
  },
  {
    name: 'slice', category: 'List', description: 'Take a portion of a list.', function: 'list/slice', outputType: 'list', outputRange: 'list',
    inputs: [
      port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('start_index', 'int', i(0), { label: 'Start', minimumValue: 0 }),
      port('size', 'int', i(10), { minimumValue: 0 }),
      port('invert', 'boolean', b(false)),
    ],
  },
  {
    name: 'reverse', category: 'List', description: 'Reverse a list.', function: 'list/reverse', outputType: 'list', outputRange: 'list',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' })],
  },
  {
    name: 'shuffle', category: 'List', description: 'Randomly reorder a list.', function: 'list/shuffle', outputType: 'list', outputRange: 'list',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' }), port('seed', 'int', i(0))],
  },
  {
    name: 'shift', category: 'List', description: 'Move items from the start to the end.', function: 'list/shift', outputType: 'list', outputRange: 'list',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' }), port('amount', 'int', i(1))],
  },
  {
    name: 'repeat', category: 'List', description: 'Repeat a list a number of times.', function: 'list/repeat', outputType: 'list', outputRange: 'list',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' }), port('amount', 'int', i(1)), port('per_item', 'boolean', b(false), { label: 'Per Item' })],
  },
  {
    name: 'sort_list', category: 'List', description: 'Sort items in a list.', function: 'list/sort', outputType: 'list', outputRange: 'list',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' }), port('key', 'string', s(''))],
  },
  {
    name: 'distinct', category: 'List', description: 'Remove duplicate items.', function: 'list/distinct', outputType: 'list', outputRange: 'list',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' }), port('key', 'string', s(''))],
  },
  {
    name: 'takeEvery', category: 'List', description: 'Take every nth element.', function: 'list/takeEvery', outputType: 'list', outputRange: 'list',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' }), port('n', 'int', i(1), { minimumValue: 1 })],
  },
  {
    name: 'cull', category: 'List', description: 'Filter a list by boolean mask.', function: 'list/cull', outputType: 'list', outputRange: 'list',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' }), port('booleans', 'list', { type: 'null' }, { range: 'list', widget: 'none' })],
  },
  {
    name: 'pick', category: 'List', description: 'Pick random items from a list.', function: 'list/pick', outputType: 'list', outputRange: 'list',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' }), port('amount', 'int', i(5)), port('seed', 'int', i(0))],
  },
  {
    name: 'keys', category: 'List', description: 'Get the keys of a map.', function: 'list/keys', outputType: 'data', outputRange: 'list',
    inputs: [port('maps', 'list', { type: 'null' }, { range: 'list', widget: 'none' })],
  },

  // ─── Color ─────────────────────────────────────
  {
    name: 'color', category: 'Color', description: 'A color variable.', function: 'color/color', outputType: 'color',
    inputs: [port('color', 'color', col(0, 0, 0, 1))],
  },
  {
    name: 'rgbColor', category: 'Color', description: 'Create a color from RGB values.', function: 'color/rgbColor', outputType: 'color',
    inputs: [port('red', 'float', f(0)), port('green', 'float', f(0)), port('blue', 'float', f(0)), port('alpha', 'float', f(255)), port('range', 'float', f(255))],
  },
  {
    name: 'hsbColor', category: 'Color', description: 'Create a color from HSB values.', function: 'color/hsbColor', outputType: 'color',
    inputs: [port('hue', 'float', f(0)), port('saturation', 'float', f(0)), port('brightness', 'float', f(0)), port('alpha', 'float', f(255)), port('range', 'float', f(255))],
  },
  {
    name: 'grayColor', category: 'Color', description: 'Create a grayscale color.', function: 'color/grayColor', outputType: 'color',
    inputs: [port('gray', 'float', f(0)), port('alpha', 'float', f(255)), port('range', 'float', f(255))],
  },

  // ─── String ────────────────────────────────────
  {
    name: 'string', category: 'String', description: 'A text string variable.', function: 'string/string', outputType: 'string',
    inputs: [port('value', 'string', s(''))],
  },
  {
    name: 'concatenate', category: 'String', description: 'Join strings together.', function: 'string/concatenate', outputType: 'string',
    inputs: [port('string1', 'string', s('')), port('string2', 'string', s('')), port('string3', 'string', s('')), port('string4', 'string', s(''))],
  },
  {
    name: 'makeStrings', category: 'String', description: 'Split a string into a list.', function: 'string/makeStrings', outputType: 'string', outputRange: 'list',
    inputs: [port('string', 'string', s('Alpha;Beta;Gamma')), port('separator', 'string', s(';'))],
  },
  {
    name: 'length', category: 'String', description: 'Count characters in a string.', function: 'string/length', outputType: 'int',
    inputs: [port('string', 'string', s(''))],
  },
  {
    name: 'wordCount', category: 'String', description: 'Count words in a string.', function: 'string/wordCount', outputType: 'int',
    inputs: [port('string', 'string', s(''))],
  },
  {
    name: 'formatNumber', category: 'String', description: 'Format a number as text.', function: 'string/formatNumber', outputType: 'string',
    inputs: [port('value', 'float', f(0)), port('format', 'string', s('%.2f'))],
  },
  {
    name: 'characters', category: 'String', description: 'Split a string into characters.', function: 'string/characters', outputType: 'string', outputRange: 'list',
    inputs: [port('string', 'string', s('default'))],
  },
  {
    name: 'characterAt', category: 'String', description: 'Get the character at an index.', function: 'string/characterAt', outputType: 'string',
    inputs: [port('string', 'string', s('default')), port('index', 'int', i(0))],
  },
  {
    name: 'contains', category: 'String', description: 'Check if a string contains another.', function: 'string/contains', outputType: 'boolean',
    inputs: [port('string', 'string', s('default')), port('contains', 'string', s('efa'))],
  },
  {
    name: 'startsWith', category: 'String', description: 'Check if a string starts with a prefix.', function: 'string/startsWith', outputType: 'boolean',
    inputs: [port('string', 'string', s('default')), port('starts_with', 'string', s('def'), { label: 'Starts With' })],
  },
  {
    name: 'endsWith', category: 'String', description: 'Check if a string ends with a suffix.', function: 'string/endsWith', outputType: 'boolean',
    inputs: [port('string', 'string', s('default')), port('ends_with', 'string', s('lt'), { label: 'Ends With' })],
  },
  {
    name: 'equals', category: 'String', description: 'Check if two strings are equal.', function: 'string/equals', outputType: 'boolean',
    inputs: [port('string', 'string', s('default')), port('equals', 'string', s('default')), port('case_sensitive', 'boolean', b(false), { label: 'Case Sensitive' })],
  },
  {
    name: 'replace', category: 'String', description: 'Replace part of a string.', function: 'string/replace', outputType: 'string',
    inputs: [port('string', 'string', s('defAULt')), port('old', 'string', s('AUL')), port('new', 'string', s('aul'))],
  },
  {
    name: 'subString', category: 'String', description: 'Take a portion of a string.', function: 'string/subString', outputType: 'string',
    inputs: [port('string', 'string', s('default')), port('start', 'int', i(0)), port('end', 'int', i(4)), port('end_offset', 'boolean', b(false), { label: 'End Offset' })],
  },
  {
    name: 'trim', category: 'String', description: 'Remove whitespace from start and end.', function: 'string/trim', outputType: 'string',
    inputs: [port('string', 'string', s('  default  '))],
  },
  {
    name: 'changeCase', category: 'String', description: 'Convert string to upper, lower, or title case.', function: 'string/changeCase', outputType: 'string',
    inputs: [
      port('string', 'string', s('default')),
      port('method', 'string', s('uppercase'), { widget: 'menu', menuItems: [{ key: 'lowercase', label: 'Lower Case' }, { key: 'uppercase', label: 'Upper Case' }, { key: 'titlecase', label: 'Title Case' }] }),
    ],
  },
  {
    name: 'randomCharacter', category: 'String', description: 'Generate random characters.', function: 'string/randomCharacter', outputType: 'string', outputRange: 'list',
    inputs: [
      port('characters', 'string', s('abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789')),
      port('amount', 'int', i(10), { minimumValue: 0 }),
      port('seed', 'int', i(0)),
    ],
  },
  {
    name: 'asBinaryString', category: 'String', description: 'Convert a string to binary representation.', function: 'string/asBinaryString', outputType: 'string',
    inputs: [port('string', 'string', s('default')), port('digit_separator', 'string', s(''), { label: 'Digit Sep' }), port('byte_separator', 'string', s(' '), { label: 'Byte Sep' })],
  },
  {
    name: 'asBinaryList', category: 'String', description: 'Convert a string to a list of binary values.', function: 'string/asBinaryList', outputType: 'int', outputRange: 'list',
    inputs: [port('string', 'string', s('default'))],
  },
  {
    name: 'asNumberList', category: 'String', description: 'Convert a string to a list of numbers.', function: 'string/asNumberList', outputType: 'float', outputRange: 'list',
    inputs: [port('string', 'string', s('default')), port('radix', 'int', i(10), { minimumValue: 2, maximumValue: 21 }), port('padding', 'boolean', b(true))],
  },

  // ─── Data ──────────────────────────────────────
  {
    name: 'lookup', category: 'Data', description: 'Look up a value by key.', function: 'data/lookup', outputType: 'data',
    inputs: [port('list', 'list', { type: 'null' }, { widget: 'none' }), port('key', 'string', s('x'))],
  },
  {
    name: 'filterData', category: 'Data', description: 'Filter data by key and value.', function: 'data/filterData', outputType: 'data', outputRange: 'list',
    inputs: [
      port('data', 'data', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('key', 'string', s('name')),
      port('op', 'string', s('='), { widget: 'menu', menuItems: [{ key: '=', label: '= Equal To' }, { key: '!=', label: '!= Not Equal To' }, { key: '>', label: '> Greater Than' }, { key: '>=', label: '>= Greater or Equal' }, { key: '<', label: '< Smaller Than' }, { key: '<=', label: '<= Smaller or Equal' }] }),
      port('value', 'string', s('')),
    ],
  },

  // ─── Network ───────────────────────────────────
  {
    name: 'encodeUrl', category: 'Network', description: 'Encode text for use in a URL.', function: 'network/encodeUrl', outputType: 'string',
    inputs: [port('value', 'string', s(''))],
  },
];

export function findTemplate(name: string): NodeTemplate | undefined {
  return NODE_TEMPLATES.find((t) => t.name === name);
}

export function createNodeFromTemplate(template: NodeTemplate, uniqueName: string, position: { x: number; y: number }): Node {
  return {
    name: uniqueName,
    prototype: null,
    function: template.function,
    category: template.category,
    description: null,
    image: null,
    position,
    comment: null,
    inputs: template.inputs.map((p) => ({ ...p })),
    outputType: template.outputType,
    outputRange: template.outputRange ?? 'value',
    children: [],
    connections: [],
    renderedChild: null,
    handle: null,
    alwaysRendered: false,
  };
}

// Group templates by category for the dialog
export function getTemplatesByCategory(): { category: string; templates: NodeTemplate[] }[] {
  const categories = new Map<string, NodeTemplate[]>();
  for (const t of NODE_TEMPLATES) {
    const list = categories.get(t.category) ?? [];
    list.push(t);
    categories.set(t.category, list);
  }
  return [...categories.entries()].map(([category, templates]) => ({ category, templates }));
}
