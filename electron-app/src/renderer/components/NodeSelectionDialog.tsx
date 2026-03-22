import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useStore } from '../state/store';
import {
  getNodeTemplates,
  createNodeFromTemplate,
  isWasmReady,
} from '../eval/wasm';
import type { NodeTemplate } from '../eval/wasm';
import type { PortType } from '../types/node';
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

function isDirectlyCompatible(outputType: PortType, inputType: string): boolean {
  if (outputType === inputType) return true;
  // List input accepts any type
  if (inputType === 'List') return true;
  // List output connects to any input
  if (outputType === 'List') return true;
  // Int <-> Float
  if (outputType === 'Int' && inputType === 'Float') return true;
  if (outputType === 'Float' && inputType === 'Int') return true;
  return false;
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

export function NodeSelectionDialog() {
  const visible = useStore((s) => s.nodeDialogVisible);
  const setVisible = useStore((s) => s.setNodeDialogVisible);
  const nodeDialogPosition = useStore((s) => s.nodeDialogPosition);
  const pendingConnection = useStore((s) => s.pendingConnection);
  const setPendingConnection = useStore((s) => s.setPendingConnection);
  const addNode = useStore((s) => s.addNode);
  const addConnection = useStore((s) => s.addConnection);
  const pushSnapshot = useStore((s) => s.pushSnapshot);
  const selectNode = useStore((s) => s.selectNode);
  const setRenderedChild = useStore((s) => s.setRenderedChild);
  const library = useStore((s) => s.library);
  const children = library.root.children;
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [selectedCategory, setSelectedCategory] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const templates = useMemo(() => {
    if (!isWasmReady()) return [];
    return getNodeTemplates();
  }, [visible]);

  const filtered = useMemo(() => {
    let result = templates;

    // Filter by pending connection compatibility
    if (pendingConnection) {
      result = result.filter(
        (t) =>
          t.first_input_type !== null &&
          isDirectlyCompatible(pendingConnection.outputType, t.first_input_type),
      );
    }

    // Filter by selected category
    if (selectedCategory) {
      result = result.filter((t) => t.category === selectedCategory);
    }

    // Filter by search query
    if (query) {
      result = result.filter(
        (t) =>
          t.name.includes(query.toLowerCase()) ||
          t.category.includes(query.toLowerCase()),
      );
    }

    return result;
  }, [templates, query, pendingConnection, selectedCategory]);

  const groupedItems = useMemo(() => {
    const groups: { category: string; items: { template: NodeTemplate; globalIndex: number }[] }[] = [];
    let globalIndex = 0;

    filtered.forEach((template) => {
      let group = groups.find((g) => g.category === template.category);
      if (!group) {
        group = { category: template.category, items: [] };
        groups.push(group);
      }
      group.items.push({ template, globalIndex: globalIndex++ });
    });

    return groups;
  }, [filtered]);

  const createNode = useCallback(
    (template: NodeTemplate) => {
      const libraryJson = JSON.stringify(library);
      let x = nodeDialogPosition?.x ?? 1;
      let y = nodeDialogPosition?.y ?? (1 + children.length * 2);
      // Nudge down to avoid placing on top of an existing node
      while (children.some((c) => c.position.x === x && c.position.y === y)) {
        y += 2;
      }
      const node = createNodeFromTemplate(template.name, libraryJson, x, y);

      pushSnapshot(library);
      addNode('root', node);
      selectNode(node.name);
      setRenderedChild('root', node.name);

      // Auto-connect if we came from a drag-to-empty-space
      if (pendingConnection) {
        addConnection('root', {
          output_node: pendingConnection.fromNode,
          input_node: node.name,
          input_port: node.inputs[0]?.name ?? '',
        });
        setPendingConnection(null);
      }

      setVisible(false);
    },
    [library, children, nodeDialogPosition, pendingConnection, addNode, addConnection, pushSnapshot, selectNode, setRenderedChild, setVisible, setPendingConnection],
  );

  const categories = useMemo(() => {
    const cats = new Set(templates.map((t) => t.category));
    return Array.from(cats).sort();
  }, [templates]);

  useEffect(() => {
    if (visible) {
      setQuery('');
      setSelectedIndex(0);
      setSelectedCategory(null);
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
          placeholder={pendingConnection ? `Compatible with ${pendingConnection.outputType}...` : 'Search nodes...'}
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          className="outline-none px-3 py-2 bg-zinc-800 text-zinc-100 text-[13px] border-none"
        />

        {/* Category filter pills */}
        <div className="flex flex-wrap gap-1 px-2 py-1 bg-zinc-700 border-b border-zinc-600">
          <button
            className="px-2 py-0.5 text-[10px] uppercase tracking-wide cursor-pointer border-none"
            style={{
              background: selectedCategory === null ? '#52525b' : 'transparent',
              color: selectedCategory === null ? '#fafafa' : '#a1a1aa',
              borderRadius: 2,
            }}
            onClick={() => { setSelectedCategory(null); setSelectedIndex(0); }}
          >
            All
          </button>
          {categories.map((cat) => (
            <button
              key={cat}
              className="px-2 py-0.5 text-[10px] uppercase tracking-wide cursor-pointer border-none"
              style={{
                background: selectedCategory === cat ? getCategoryColor(cat) + '40' : 'transparent',
                color: selectedCategory === cat ? getCategoryColor(cat) : '#a1a1aa',
                borderRadius: 2,
              }}
              onClick={() => { setSelectedCategory(selectedCategory === cat ? null : cat); setSelectedIndex(0); }}
            >
              {cat}
            </button>
          ))}
        </div>

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
              {group.items.map(({ template, globalIndex }) => (
                <div
                  key={template.name}
                  className="flex items-center px-3 cursor-pointer gap-2 text-zinc-100 text-[11px]"
                  style={{
                    height: 32,
                    background: globalIndex === selectedIndex ? SELECTED_ITEM : 'transparent',
                  }}
                  onClick={() => createNode(template)}
                  onMouseEnter={() => setSelectedIndex(globalIndex)}
                >
                  <NodeIcon name={template.name} category={template.category} />
                  <span className="flex-1">{template.name}</span>
                  <span className="text-zinc-300 text-[11px]">
                    {template.category}
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
