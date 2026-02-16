import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useStore } from '../state/store';
import { createDefaultNode } from '../types/node';
import type { Port } from '../types/node';
import type { Value } from '../types/value';
import {
  SELECTED_ITEM,
  CATEGORY_GEOMETRY,
  CATEGORY_TRANSFORM,
  CATEGORY_COLOR,
  CATEGORY_MATH,
  CATEGORY_LIST,
  CATEGORY_STRING,
  CATEGORY_DATA,
  CATEGORY_DEFAULT,
} from '../theme/tokens';

interface NodePrototype {
  name: string;
  category: string;
  description: string;
  defaultInputs: Port[];
}

const mkPort = (
  name: string,
  port_type: Port['port_type'],
  widget: Port['widget'],
  value: Port['value'],
  label?: string,
): Port => ({
  name,
  port_type,
  label: label ?? name,
  description: null,
  widget,
  range: 'Value',
  value,
  min: null,
  max: null,
  menu_items: [],
});

const FLOAT_ZERO: Value = { Float: 0 };
const FLOAT_100: Value = { Float: 100 };
const POINT_ZERO: Value = { Point: { x: 0, y: 0 } };

const NODE_PROTOTYPES: NodePrototype[] = [
  {
    name: 'rect',
    category: 'geometry',
    description: 'Create a rectangle',
    defaultInputs: [
      mkPort('position', 'Point', 'Point', POINT_ZERO),
      mkPort('width', 'Float', 'Float', FLOAT_100),
      mkPort('height', 'Float', 'Float', FLOAT_100),
      mkPort('roundness', 'Point', 'Point', POINT_ZERO),
    ],
  },
  {
    name: 'ellipse',
    category: 'geometry',
    description: 'Create an ellipse',
    defaultInputs: [
      mkPort('position', 'Point', 'Point', POINT_ZERO),
      mkPort('width', 'Float', 'Float', FLOAT_100),
      mkPort('height', 'Float', 'Float', FLOAT_100),
    ],
  },
  {
    name: 'line',
    category: 'geometry',
    description: 'Create a line',
    defaultInputs: [
      mkPort('point1', 'Point', 'Point', POINT_ZERO),
      mkPort('point2', 'Point', 'Point', { Point: { x: 100, y: 0 } }),
      mkPort('points', 'Int', 'Int', { Int: 2 }),
    ],
  },
  {
    name: 'star',
    category: 'geometry',
    description: 'Create a star',
    defaultInputs: [
      mkPort('position', 'Point', 'Point', POINT_ZERO),
      mkPort('points', 'Int', 'Int', { Int: 5 }),
      mkPort('outer', 'Float', 'Float', FLOAT_100),
      mkPort('inner', 'Float', 'Float', { Float: 50 }),
    ],
  },
  {
    name: 'polygon',
    category: 'geometry',
    description: 'Create a polygon',
    defaultInputs: [
      mkPort('position', 'Point', 'Point', POINT_ZERO),
      mkPort('radius', 'Float', 'Float', FLOAT_100),
      mkPort('sides', 'Int', 'Int', { Int: 6 }),
    ],
  },
  {
    name: 'arc',
    category: 'geometry',
    description: 'Create an arc',
    defaultInputs: [
      mkPort('position', 'Point', 'Point', POINT_ZERO),
      mkPort('width', 'Float', 'Float', FLOAT_100),
      mkPort('height', 'Float', 'Float', FLOAT_100),
      mkPort('startAngle', 'Float', 'Angle', FLOAT_ZERO),
      mkPort('degrees', 'Float', 'Angle', { Float: 360 }),
    ],
  },
  {
    name: 'grid',
    category: 'geometry',
    description: 'Create a point grid',
    defaultInputs: [
      mkPort('columns', 'Int', 'Int', { Int: 10 }),
      mkPort('rows', 'Int', 'Int', { Int: 10 }),
      mkPort('width', 'Float', 'Float', { Float: 200 }),
      mkPort('height', 'Float', 'Float', { Float: 200 }),
    ],
  },
  {
    name: 'textpath',
    category: 'geometry',
    description: 'Create text path',
    defaultInputs: [
      mkPort('text', 'String', 'String', { String: 'Hello' }),
      mkPort('position', 'Point', 'Point', POINT_ZERO),
      mkPort('fontSize', 'Float', 'Float', { Float: 24 }),
    ],
  },
  {
    name: 'colorize',
    category: 'color',
    description: 'Set fill color',
    defaultInputs: [
      mkPort('shape', 'Geometry', 'None', 'Null'),
      mkPort('fill', 'Color', 'Color', { Color: { r: 1, g: 1, b: 1, a: 1 } }),
    ],
  },
  {
    name: 'stroke',
    category: 'color',
    description: 'Set stroke',
    defaultInputs: [
      mkPort('shape', 'Geometry', 'None', 'Null'),
      mkPort('color', 'Color', 'Color', { Color: { r: 0, g: 0, b: 0, a: 1 } }),
      mkPort('width', 'Float', 'Float', { Float: 1 }),
    ],
  },
  {
    name: 'translate',
    category: 'transform',
    description: 'Move geometry',
    defaultInputs: [
      mkPort('shape', 'Geometry', 'None', 'Null'),
      mkPort('translate', 'Point', 'Point', POINT_ZERO),
    ],
  },
  {
    name: 'rotate',
    category: 'transform',
    description: 'Rotate geometry',
    defaultInputs: [
      mkPort('shape', 'Geometry', 'None', 'Null'),
      mkPort('angle', 'Float', 'Angle', FLOAT_ZERO),
    ],
  },
  {
    name: 'scale',
    category: 'transform',
    description: 'Scale geometry',
    defaultInputs: [
      mkPort('shape', 'Geometry', 'None', 'Null'),
      mkPort('scale', 'Point', 'Point', { Point: { x: 1, y: 1 } }),
    ],
  },
  {
    name: 'copy',
    category: 'transform',
    description: 'Copy with transform',
    defaultInputs: [
      mkPort('shape', 'Geometry', 'None', 'Null'),
      mkPort('copies', 'Int', 'Int', { Int: 10 }),
      mkPort('translate', 'Point', 'Point', POINT_ZERO),
      mkPort('rotate', 'Float', 'Angle', FLOAT_ZERO),
      mkPort('scale', 'Point', 'Point', { Point: { x: 1, y: 1 } }),
    ],
  },
  {
    name: 'add',
    category: 'math',
    description: 'Add two numbers',
    defaultInputs: [
      mkPort('value1', 'Float', 'Float', FLOAT_ZERO),
      mkPort('value2', 'Float', 'Float', FLOAT_ZERO),
    ],
  },
  {
    name: 'subtract',
    category: 'math',
    description: 'Subtract two numbers',
    defaultInputs: [
      mkPort('value1', 'Float', 'Float', FLOAT_ZERO),
      mkPort('value2', 'Float', 'Float', FLOAT_ZERO),
    ],
  },
  {
    name: 'multiply',
    category: 'math',
    description: 'Multiply two numbers',
    defaultInputs: [
      mkPort('value1', 'Float', 'Float', FLOAT_ZERO),
      mkPort('value2', 'Float', 'Float', FLOAT_ZERO),
    ],
  },
  {
    name: 'divide',
    category: 'math',
    description: 'Divide two numbers',
    defaultInputs: [
      mkPort('value1', 'Float', 'Float', FLOAT_ZERO),
      mkPort('value2', 'Float', 'Float', { Float: 1 }),
    ],
  },
  {
    name: 'random',
    category: 'math',
    description: 'Random number',
    defaultInputs: [
      mkPort('min', 'Float', 'Float', FLOAT_ZERO),
      mkPort('max', 'Float', 'Float', FLOAT_100),
      mkPort('seed', 'Int', 'Seed', { Int: 0 }),
    ],
  },
  {
    name: 'merge',
    category: 'list',
    description: 'Merge paths',
    defaultInputs: [
      mkPort('shape1', 'Geometry', 'None', 'Null'),
      mkPort('shape2', 'Geometry', 'None', 'Null'),
    ],
  },
  {
    name: 'switch',
    category: 'list',
    description: 'Switch between inputs',
    defaultInputs: [
      mkPort('input1', 'Geometry', 'None', 'Null'),
      mkPort('input2', 'Geometry', 'None', 'Null'),
      mkPort('index', 'Int', 'Int', { Int: 0 }),
    ],
  },
];

function getCategoryColor(category: string): string {
  switch (category) {
    case 'geometry': return CATEGORY_GEOMETRY;
    case 'transform': return CATEGORY_TRANSFORM;
    case 'color': return CATEGORY_COLOR;
    case 'math': return CATEGORY_MATH;
    case 'list': return CATEGORY_LIST;
    case 'string': return CATEGORY_STRING;
    case 'data': return CATEGORY_DATA;
    default: return CATEGORY_DEFAULT;
  }
}

function NodeIcon({ name, category }: { name: string; category: string }) {
  const [hasImage, setHasImage] = useState(true);
  const catColor = getCategoryColor(category);

  if (!hasImage) {
    return (
      <div
        data-testid={`node-icon-${name}`}
        style={{
          width: 16,
          height: 16,
          background: catColor,
          borderRadius: 2,
          flexShrink: 0,
        }}
      />
    );
  }

  return (
    <img
      data-testid={`node-icon-${name}`}
      src={`/icons/corevector/${name}.svg`}
      width={16}
      height={16}
      style={{ flexShrink: 0, filter: 'brightness(0) invert(1)' }}
      onError={() => setHasImage(false)}
    />
  );
}

function generateUniqueName(baseName: string, existingNames: string[]): string {
  let counter = 1;
  let name = `${baseName}${counter}`;
  while (existingNames.includes(name)) {
    counter++;
    name = `${baseName}${counter}`;
  }
  return name;
}

export function NodeSelectionDialog() {
  const visible = useStore((s) => s.nodeDialogVisible);
  const setVisible = useStore((s) => s.setNodeDialogVisible);
  const addNode = useStore((s) => s.addNode);
  const selectNode = useStore((s) => s.selectNode);
  const setRenderedChild = useStore((s) => s.setRenderedChild);
  const children = useStore((s) => s.library.root.children);
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  const filtered = NODE_PROTOTYPES.filter(
    (n) =>
      n.name.includes(query.toLowerCase()) ||
      n.category.includes(query.toLowerCase()),
  );

  const groupedItems = useMemo(() => {
    const groups: { category: string; protos: { proto: NodePrototype; globalIndex: number }[] }[] = [];
    let globalIndex = 0;

    filtered.forEach((proto) => {
      let group = groups.find((g) => g.category === proto.category);
      if (!group) {
        group = { category: proto.category, protos: [] };
        groups.push(group);
      }
      group.protos.push({ proto, globalIndex: globalIndex++ });
    });

    return groups;
  }, [filtered]);

  const createNode = useCallback(
    (proto: NodePrototype) => {
      const existingNames = children.map((n) => n.name);
      const name = generateUniqueName(proto.name, existingNames);
      const node = {
        ...createDefaultNode(name),
        prototype: `corevector.${proto.name}`,
        category: proto.category,
        position: { x: 1, y: 1 + children.length * 2 },
        inputs: proto.defaultInputs.map((p) => ({ ...p })),
      };
      addNode('root', node);
      selectNode(name);
      setRenderedChild('root', name);
      setVisible(false);
    },
    [children, addNode, selectNode, setRenderedChild, setVisible],
  );

  useEffect(() => {
    if (visible) {
      setQuery('');
      setSelectedIndex(0);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [visible]);

  useEffect(() => {
    setSelectedIndex(0);
  }, [query]);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'ArrowDown') {
        e.preventDefault();
        setSelectedIndex((i) => Math.min(filtered.length - 1, i + 1));
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        setSelectedIndex((i) => Math.max(0, i - 1));
      } else if (e.key === 'Enter') {
        e.preventDefault();
        if (filtered[selectedIndex]) {
          createNode(filtered[selectedIndex]);
        }
      } else if (e.key === 'Escape') {
        setVisible(false);
      }
    },
    [filtered, selectedIndex, setVisible, createNode],
  );

  if (!visible) return null;

  return (
    <div
      className="fixed inset-0 flex items-start justify-center pt-24"
      style={{ zIndex: 100 }}
      onClick={() => setVisible(false)}
    >
      <div
        className="flex flex-col bg-zinc-700 border border-zinc-500"
        style={{ width: 320, maxHeight: 400 }}
        onClick={(e) => e.stopPropagation()}
      >
        {/* Search input */}
        <input
          ref={inputRef}
          type="text"
          placeholder="Search nodes..."
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          className="outline-none px-3 py-2 bg-zinc-800 text-zinc-100 text-[13px] border-none"
        />

        {/* Results list */}
        <div className="overflow-y-auto flex-1">
          {groupedItems.map((group) => (
            <div key={group.category}>
              <div
                data-testid={`category-header-${group.category}`}
                className="px-3 uppercase flex items-center text-zinc-300 bg-zinc-700 sticky top-0 z-1"
                style={{ height: 24, fontSize: 10, letterSpacing: '0.05em' }}
              >
                {group.category}
              </div>
              {group.protos.map(({ proto, globalIndex }) => (
                <div
                  key={proto.name}
                  className="flex items-center px-3 cursor-pointer gap-2 text-zinc-100 text-[11px]"
                  style={{
                    height: 32,
                    background: globalIndex === selectedIndex ? SELECTED_ITEM : 'transparent',
                  }}
                  onClick={() => createNode(proto)}
                  onMouseEnter={() => setSelectedIndex(globalIndex)}
                >
                  <NodeIcon name={proto.name} category={proto.category} />
                  <span className="flex-1">{proto.name}</span>
                  <span className="text-zinc-300 text-[11px]">
                    {proto.category}
                  </span>
                </div>
              ))}
            </div>
          ))}
          {filtered.length === 0 && (
            <div className="px-3 py-4 text-center text-zinc-300 text-[11px]">
              No nodes found
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
