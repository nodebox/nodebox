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
      port('radius', 'float', f(50)),
      port('sides', 'int', i(6), { minimumValue: 3 }),
      port('align', 'boolean', b(true)),
    ],
  },
  {
    name: 'star', category: 'Geometry', description: 'Create a star shape.', function: 'corevector/star', outputType: 'geometry',
    inputs: [
      port('position', 'point', pt(0, 0)),
      port('points', 'int', i(5), { minimumValue: 2 }),
      port('outer', 'float', f(100)),
      port('inner', 'float', f(50)),
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
      port('point1', 'point', pt(0, 0)),
      port('point2', 'point', pt(100, 0)),
      port('points', 'int', i(2)),
    ],
  },
  {
    name: 'grid', category: 'Geometry', description: 'Create a grid of points.', function: 'corevector/grid', outputType: 'point', outputRange: 'list',
    inputs: [
      port('columns', 'int', i(5)),
      port('rows', 'int', i(5)),
      port('width', 'float', f(200)),
      port('height', 'float', f(200)),
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
    name: 'makePoint', category: 'Geometry', description: 'Create a single point.', function: 'corevector/makePoint', outputType: 'point',
    inputs: [
      port('x', 'float', f(0)),
      port('y', 'float', f(0)),
    ],
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
      port('angle', 'float', f(0), { widget: 'angle' }),
      port('origin', 'point', pt(0, 0)),
    ],
  },
  {
    name: 'scale', category: 'Transform', description: 'Scale a shape by percentage.', function: 'corevector/scale', outputType: 'geometry',
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
      port('order', 'string', s('tsr'), { widget: 'menu', menuItems: [{ key: 'tsr', label: 'Trans Scale Rot' }, { key: 'trs', label: 'Trans Rot Scale' }, { key: 'str', label: 'Scale Trans Rot' }, { key: 'srt', label: 'Scale Rot Trans' }, { key: 'rts', label: 'Rot Trans Scale' }, { key: 'rst', label: 'Rot Scale Trans' }] }),
      port('translate', 'point', pt(0, 0)),
      port('rotate', 'float', f(0)),
      port('scale', 'point', pt(100, 100)),
    ],
  },
  {
    name: 'align', category: 'Transform', description: 'Align a shape to a point.', function: 'corevector/align', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('position', 'point', pt(0, 0)),
      port('halign', 'string', s('center'), { label: 'H Align', widget: 'menu', menuItems: [{ key: 'none', label: 'None' }, { key: 'left', label: 'Left' }, { key: 'center', label: 'Center' }, { key: 'right', label: 'Right' }] }),
      port('valign', 'string', s('middle'), { label: 'V Align', widget: 'menu', menuItems: [{ key: 'none', label: 'None' }, { key: 'top', label: 'Top' }, { key: 'middle', label: 'Middle' }, { key: 'bottom', label: 'Bottom' }] }),
    ],
  },
  {
    name: 'fit', category: 'Transform', description: 'Fit a shape into a bounding box.', function: 'corevector/fit', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('position', 'point', pt(0, 0)),
      port('width', 'float', f(200)),
      port('height', 'float', f(200)),
      port('keepProportions', 'boolean', b(true), { label: 'Keep Proportions' }),
    ],
  },
  {
    name: 'reflect', category: 'Transform', description: 'Mirror a shape.', function: 'corevector/reflect', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('position', 'point', pt(0, 0)),
      port('angle', 'float', f(90)),
      port('keepOriginal', 'boolean', b(true), { label: 'Keep Original' }),
    ],
  },
  {
    name: 'resample', category: 'Style', description: 'Redistribute points along a path.', function: 'corevector/resample', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('method', 'string', s('amount'), { widget: 'menu', menuItems: [{ key: 'amount', label: 'By Amount' }, { key: 'length', label: 'By Length' }] }),
      port('length', 'float', f(10)),
      port('points', 'int', i(20)),
      port('perContour', 'boolean', b(false), { label: 'Per Contour' }),
    ],
  },
  {
    name: 'wiggle', category: 'Style', description: 'Randomly displace points.', function: 'corevector/wiggle', outputType: 'geometry', outputRange: 'list',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('scope', 'string', s('points'), { widget: 'menu', menuItems: [{ key: 'points', label: 'Points' }, { key: 'contours', label: 'Contours' }, { key: 'paths', label: 'Paths' }] }),
      port('offset', 'point', pt(10, 10)),
      port('seed', 'int', i(0), { widget: 'seed' }),
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
    name: 'ungroup', category: 'Style', description: 'Split geometry into separate paths.', function: 'corevector/ungroup', outputType: 'geometry', outputRange: 'list',
    inputs: [port('shape', 'geometry', { type: 'null' }, { widget: 'none' })],
  },
  {
    name: 'sort', category: 'Style', description: 'Sort shapes by position or size.', function: 'corevector/sort', outputType: 'geometry', outputRange: 'list',
    inputs: [
      port('shapes', 'geometry', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('orderBy', 'string', s('x'), { label: 'Order', widget: 'menu', menuItems: [{ key: 'x', label: 'X' }, { key: 'y', label: 'Y' }, { key: 'distance', label: 'Distance' }, { key: 'angle', label: 'Angle' }, { key: 'area', label: 'Area' }] }),
      port('position', 'point', pt(0, 0)),
    ],
  },
  {
    name: 'stack', category: 'Style', description: 'Stack shapes side by side.', function: 'corevector/stack', outputType: 'geometry', outputRange: 'list',
    inputs: [
      port('shapes', 'geometry', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('direction', 'string', s('right'), { widget: 'menu', menuItems: [{ key: 'right', label: 'Right' }, { key: 'down', label: 'Down' }] }),
      port('margin', 'float', f(0)),
    ],
  },
  {
    name: 'snap', category: 'Transform', description: 'Snap points to a grid.', function: 'corevector/snap', outputType: 'geometry',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('distance', 'float', f(10)),
      port('strength', 'float', f(100)),
      port('position', 'point', pt(0, 0)),
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
    name: 'group', category: 'Geometry', description: 'Combine shapes into one geometry.', function: 'corevector/group', outputType: 'geometry',
    inputs: [port('shapes', 'geometry', { type: 'null' }, { range: 'list', widget: 'none' })],
  },
  {
    name: 'scatter', category: 'Style', description: 'Generate random points in a shape.', function: 'corevector/scatter', outputType: 'point', outputRange: 'list',
    inputs: [
      port('shape', 'geometry', { type: 'null' }, { widget: 'none' }),
      port('amount', 'int', i(20)),
      port('seed', 'int', i(0), { widget: 'seed' }),
    ],
  },

  // ─── Math ──────────────────────────────────────
  {
    name: 'number', category: 'Math', description: 'A floating-point number.', function: 'math/number', outputType: 'float',
    inputs: [port('value', 'float', f(0))],
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
    inputs: [port('value1', 'float', f(0)), port('value2', 'float', f(0))],
  },
  {
    name: 'divide', category: 'Math', description: 'Divide two numbers.', function: 'math/divide', outputType: 'float',
    inputs: [port('value1', 'float', f(0)), port('value2', 'float', f(1))],
  },
  {
    name: 'range', category: 'Math', description: 'Generate a range of numbers.', function: 'math/range', outputType: 'float', outputRange: 'list',
    inputs: [port('start', 'float', f(0)), port('end', 'float', f(10)), port('step', 'float', f(1))],
  },
  {
    name: 'sample', category: 'Math', description: 'Generate evenly spaced numbers.', function: 'math/sample', outputType: 'float', outputRange: 'list',
    inputs: [port('amount', 'int', i(10)), port('start', 'float', f(0)), port('end', 'float', f(100))],
  },
  {
    name: 'random_numbers', category: 'Math', description: 'Generate random numbers.', function: 'math/randomNumbers', outputType: 'float', outputRange: 'list',
    inputs: [port('amount', 'int', i(10)), port('start', 'float', f(0)), port('end', 'float', f(100)), port('seed', 'int', i(0), { widget: 'seed' })],
  },
  {
    name: 'wave', category: 'Math', description: 'Generate a wave signal.', function: 'math/wave', outputType: 'float',
    inputs: [port('min', 'float', f(-1)), port('max', 'float', f(1)), port('period', 'float', f(60)), port('offset', 'float', f(0)), port('type', 'string', s('sine'), { widget: 'menu', menuItems: [{ key: 'sine', label: 'Sine' }, { key: 'square', label: 'Square' }, { key: 'triangle', label: 'Triangle' }, { key: 'sawtooth', label: 'Sawtooth' }] }), port('frame', 'float', f(1))],
  },
  {
    name: 'compare', category: 'Math', description: 'Compare two values.', function: 'math/compare', outputType: 'boolean',
    inputs: [port('value1', 'float', f(0)), port('value2', 'float', f(0)), port('comparator', 'string', s('equal'), { widget: 'menu', menuItems: [{ key: 'equal', label: '=' }, { key: 'not-equal', label: '!=' }, { key: 'less-than', label: '<' }, { key: 'greater-than', label: '>' }, { key: 'less-or-equal', label: '<=' }, { key: 'greater-or-equal', label: '>=' }] })],
  },

  // ─── List ──────────────────────────────────────
  {
    name: 'combine', category: 'List', description: 'Merge multiple lists.', function: 'list/combine', outputType: 'list',
    inputs: [
      port('list1', 'list', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('list2', 'list', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('list3', 'list', { type: 'null' }, { range: 'list', widget: 'none' }),
    ],
  },
  {
    name: 'count', category: 'List', description: 'Count items in a list.', function: 'list/count', outputType: 'int',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' })],
  },
  {
    name: 'slice', category: 'List', description: 'Take a portion of a list.', function: 'list/slice', outputType: 'list',
    inputs: [
      port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' }),
      port('start_index', 'int', i(0), { label: 'Start' }),
      port('size', 'int', i(10)),
      port('invert', 'boolean', b(false)),
    ],
  },
  {
    name: 'reverse', category: 'List', description: 'Reverse a list.', function: 'list/reverse', outputType: 'list',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' })],
  },
  {
    name: 'shuffle', category: 'List', description: 'Randomly reorder a list.', function: 'list/shuffle', outputType: 'list',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' }), port('seed', 'int', i(0), { widget: 'seed' })],
  },
  {
    name: 'repeat', category: 'List', description: 'Repeat a list.', function: 'list/repeat', outputType: 'list',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' }), port('amount', 'int', i(2)), port('perItem', 'boolean', b(false), { label: 'Per Item' })],
  },
  {
    name: 'cull', category: 'List', description: 'Filter a list by boolean mask.', function: 'list/cull', outputType: 'list',
    inputs: [port('list', 'list', { type: 'null' }, { range: 'list', widget: 'none' }), port('booleans', 'list', { type: 'null' }, { range: 'list', widget: 'none' })],
  },

  // ─── Color ─────────────────────────────────────
  {
    name: 'rgb_color', category: 'Color', description: 'Create a color from RGB values.', function: 'color/rgbColor', outputType: 'color',
    inputs: [port('red', 'float', f(0)), port('green', 'float', f(0)), port('blue', 'float', f(0)), port('alpha', 'float', f(255)), port('range', 'float', f(255))],
  },
  {
    name: 'hsb_color', category: 'Color', description: 'Create a color from HSB values.', function: 'color/hsbColor', outputType: 'color',
    inputs: [port('hue', 'float', f(0)), port('saturation', 'float', f(255)), port('brightness', 'float', f(255)), port('alpha', 'float', f(255)), port('range', 'float', f(255))],
  },
  {
    name: 'gray_color', category: 'Color', description: 'Create a grayscale color.', function: 'color/grayColor', outputType: 'color',
    inputs: [port('gray', 'float', f(128)), port('alpha', 'float', f(255)), port('range', 'float', f(255))],
  },

  // ─── String ────────────────────────────────────
  {
    name: 'string', category: 'String', description: 'A text string.', function: 'string/string', outputType: 'string',
    inputs: [port('value', 'string', s(''))],
  },
  {
    name: 'concatenate', category: 'String', description: 'Join strings together.', function: 'string/concatenate', outputType: 'string',
    inputs: [port('string1', 'string', s('')), port('string2', 'string', s('')), port('string3', 'string', s('')), port('string4', 'string', s('')), port('string5', 'string', s('')), port('string6', 'string', s('')), port('string7', 'string', s('')), port('separator', 'string', s(''))],
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
