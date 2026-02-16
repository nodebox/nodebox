import { useState, useEffect, useRef, useCallback, useMemo } from 'react';
import { useStore } from '../state/store';
import {
  getNodeTemplates,
  createNodeFromTemplate,
  isWasmReady,
} from '../eval/wasm';
import type { NodeTemplate } from '../eval/wasm';
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
  const addNode = useStore((s) => s.addNode);
  const selectNode = useStore((s) => s.selectNode);
  const setRenderedChild = useStore((s) => s.setRenderedChild);
  const library = useStore((s) => s.library);
  const children = library.root.children;
  const [query, setQuery] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const inputRef = useRef<HTMLInputElement>(null);

  const templates = useMemo(() => {
    if (!isWasmReady()) return [];
    return getNodeTemplates();
  }, [visible]);

  const filtered = useMemo(
    () =>
      templates.filter(
        (t) =>
          t.name.includes(query.toLowerCase()) ||
          t.category.includes(query.toLowerCase()),
      ),
    [templates, query],
  );

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
      const y = 1 + children.length * 2;
      const node = createNodeFromTemplate(template.name, libraryJson, 1, y);
      addNode('root', node);
      selectNode(node.name);
      setRenderedChild('root', node.name);
      setVisible(false);
    },
    [library, children, addNode, selectNode, setRenderedChild, setVisible],
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
