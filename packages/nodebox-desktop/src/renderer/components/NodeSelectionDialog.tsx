import React, { useState, useCallback, useEffect, useRef, useMemo } from 'react';
import { useStore } from '../state/store';
import {
  DIALOG_BACKGROUND, DIALOG_BORDER, SELECTION_BG, HOVERED_ITEM,
  TEXT_STRONG, TEXT_DEFAULT, TEXT_DISABLED, ZINC_600, ZINC_800,
  FONT_SIZE_SMALL, FONT_SIZE_BASE,
} from '../theme/tokens';

// Node categories with their available node types
const NODE_LIBRARY: { category: string; nodes: { name: string; prototype: string; outputType: string }[] }[] = [
  {
    category: 'Geometry',
    nodes: [
      { name: 'rect', prototype: 'corevector.rect', outputType: 'geometry' },
      { name: 'ellipse', prototype: 'corevector.ellipse', outputType: 'geometry' },
      { name: 'polygon', prototype: 'corevector.polygon', outputType: 'geometry' },
      { name: 'star', prototype: 'corevector.star', outputType: 'geometry' },
      { name: 'arc', prototype: 'corevector.arc', outputType: 'geometry' },
      { name: 'line', prototype: 'corevector.line', outputType: 'geometry' },
      { name: 'lineAngle', prototype: 'corevector.lineAngle', outputType: 'geometry' },
      { name: 'connect', prototype: 'corevector.connect', outputType: 'geometry' },
      { name: 'grid', prototype: 'corevector.grid', outputType: 'point' },
      { name: 'freehand', prototype: 'corevector.freehand', outputType: 'geometry' },
    ],
  },
  {
    category: 'Transform',
    nodes: [
      { name: 'translate', prototype: 'corevector.translate', outputType: 'geometry' },
      { name: 'rotate', prototype: 'corevector.rotate', outputType: 'geometry' },
      { name: 'scale', prototype: 'corevector.scale', outputType: 'geometry' },
      { name: 'copy', prototype: 'corevector.copy', outputType: 'geometry' },
      { name: 'align', prototype: 'corevector.align', outputType: 'geometry' },
      { name: 'fit', prototype: 'corevector.fit', outputType: 'geometry' },
      { name: 'reflect', prototype: 'corevector.reflect', outputType: 'geometry' },
      { name: 'snap', prototype: 'corevector.snap', outputType: 'geometry' },
      { name: 'skew', prototype: 'corevector.skew', outputType: 'geometry' },
    ],
  },
  {
    category: 'Style',
    nodes: [
      { name: 'colorize', prototype: 'corevector.colorize', outputType: 'geometry' },
      { name: 'compound', prototype: 'corevector.compound', outputType: 'geometry' },
      { name: 'resample', prototype: 'corevector.resample', outputType: 'geometry' },
      { name: 'wiggle', prototype: 'corevector.wiggle', outputType: 'geometry' },
      { name: 'ungroup', prototype: 'corevector.ungroup', outputType: 'geometry' },
      { name: 'sort', prototype: 'corevector.sort', outputType: 'geometry' },
      { name: 'stack', prototype: 'corevector.stack', outputType: 'geometry' },
      { name: 'delete', prototype: 'corevector.delete', outputType: 'geometry' },
    ],
  },
  {
    category: 'Math',
    nodes: [
      { name: 'add', prototype: 'math.add', outputType: 'float' },
      { name: 'subtract', prototype: 'math.subtract', outputType: 'float' },
      { name: 'multiply', prototype: 'math.multiply', outputType: 'float' },
      { name: 'divide', prototype: 'math.divide', outputType: 'float' },
      { name: 'number', prototype: 'math.number', outputType: 'float' },
      { name: 'range', prototype: 'math.range', outputType: 'float' },
      { name: 'sample', prototype: 'math.sample', outputType: 'float' },
      { name: 'random_numbers', prototype: 'math.randomNumbers', outputType: 'float' },
      { name: 'wave', prototype: 'math.wave', outputType: 'float' },
      { name: 'compare', prototype: 'math.compare', outputType: 'boolean' },
    ],
  },
  {
    category: 'List',
    nodes: [
      { name: 'combine', prototype: 'list.combine', outputType: 'list' },
      { name: 'count', prototype: 'list.count', outputType: 'int' },
      { name: 'slice', prototype: 'list.slice', outputType: 'list' },
      { name: 'sort', prototype: 'list.sort', outputType: 'list' },
      { name: 'reverse', prototype: 'list.reverse', outputType: 'list' },
      { name: 'shuffle', prototype: 'list.shuffle', outputType: 'list' },
      { name: 'repeat', prototype: 'list.repeat', outputType: 'list' },
      { name: 'cull', prototype: 'list.cull', outputType: 'list' },
    ],
  },
  {
    category: 'Color',
    nodes: [
      { name: 'rgb_color', prototype: 'color.rgbColor', outputType: 'color' },
      { name: 'hsb_color', prototype: 'color.hsbColor', outputType: 'color' },
      { name: 'gray_color', prototype: 'color.grayColor', outputType: 'color' },
    ],
  },
  {
    category: 'String',
    nodes: [
      { name: 'string', prototype: 'string.string', outputType: 'string' },
      { name: 'concatenate', prototype: 'string.concatenate', outputType: 'string' },
      { name: 'replace', prototype: 'string.replace', outputType: 'string' },
    ],
  },
];

export function NodeSelectionDialog() {
  const visible = useStore((s) => s.nodeSelectionDialogOpen);
  const setVisible = useStore((s) => s.setNodeSelectionDialogOpen);
  const addNodeToNetwork = useStore((s) => s.addNodeToNetwork);
  const currentNetworkPath = useStore((s) => s.currentNetworkPath);
  const pushHistory = useStore((s) => s.pushHistory);

  const [search, setSearch] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [activeCategory, setActiveCategory] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (visible) {
      setSearch('');
      setSelectedIndex(0);
      setActiveCategory(null);
      setTimeout(() => inputRef.current?.focus(), 50);
    }
  }, [visible]);

  const filteredNodes = useMemo(() => {
    const all = NODE_LIBRARY.flatMap((cat) =>
      (activeCategory === null || activeCategory === cat.category)
        ? cat.nodes.map((n) => ({ ...n, category: cat.category }))
        : [],
    );
    if (!search) return all;
    const lower = search.toLowerCase();
    return all.filter((n) => n.name.toLowerCase().includes(lower));
  }, [search, activeCategory]);

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') { e.preventDefault(); setSelectedIndex((i) => Math.min(i + 1, filteredNodes.length - 1)); }
    if (e.key === 'ArrowUp') { e.preventDefault(); setSelectedIndex((i) => Math.max(0, i - 1)); }
    if (e.key === 'Enter' && filteredNodes[selectedIndex]) {
      e.preventDefault();
      addNode(filteredNodes[selectedIndex]);
    }
    if (e.key === 'Escape') { setVisible(false); }
  }, [filteredNodes, selectedIndex, setVisible]);

  const addNode = useCallback((nodeInfo: typeof filteredNodes[0]) => {
    pushHistory();
    const library = useStore.getState().library;
    if (!library) return;

    // Find existing names to generate unique name
    const parts = currentNetworkPath.split('/').filter(Boolean);
    let network = library.root;
    const startIdx = parts[0] === network.name ? 1 : 0;
    for (let i = startIdx; i < parts.length; i++) {
      const child = network.children.find((c: any) => c.name === parts[i]);
      if (!child) break;
      network = child;
    }

    const existing = new Set(network.children.map((c: any) => c.name));
    let name = nodeInfo.name + '1';
    for (let i = 1; existing.has(name); i++) { name = nodeInfo.name + i; }

    addNodeToNetwork(currentNetworkPath, {
      name,
      prototype: nodeInfo.prototype,
      function: null,
      category: null,
      description: null,
      image: null,
      position: { x: 5, y: 5 },
      comment: null,
      inputs: [],
      outputType: nodeInfo.outputType,
      outputRange: 'value',
      children: [],
      connections: [],
      renderedChild: null,
      handle: null,
      alwaysRendered: false,
    });
    setVisible(false);
  }, [addNodeToNetwork, currentNetworkPath, pushHistory, setVisible]);

  if (!visible) return null;

  return (
    <div style={{ position: 'fixed', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, background: 'rgba(0,0,0,0.4)' }}>
      <div style={{ width: 400, maxHeight: 500, background: DIALOG_BACKGROUND, border: `1px solid ${DIALOG_BORDER}`, borderRadius: 6, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        {/* Search */}
        <div style={{ padding: 8 }}>
          <input
            ref={inputRef}
            type="text"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setSelectedIndex(0); }}
            onKeyDown={handleKeyDown}
            placeholder="Search nodes..."
            style={{ width: '100%', background: ZINC_600, color: TEXT_STRONG, border: 'none', padding: '6px 10px', fontSize: FONT_SIZE_BASE, borderRadius: 4, outline: 'none', fontFamily: 'inherit' }}
          />
        </div>

        {/* Category pills */}
        <div style={{ display: 'flex', gap: 4, padding: '0 8px 8px', flexWrap: 'wrap' }}>
          <CategoryPill label="All" active={activeCategory === null} onClick={() => setActiveCategory(null)} />
          {NODE_LIBRARY.map((cat) => (
            <CategoryPill key={cat.category} label={cat.category} active={activeCategory === cat.category} onClick={() => setActiveCategory(cat.category)} />
          ))}
        </div>

        {/* Node list */}
        <div style={{ flex: 1, overflow: 'auto', padding: '0 4px 4px' }}>
          {filteredNodes.map((node, i) => (
            <div
              key={node.name + node.prototype}
              onClick={() => addNode(node)}
              onMouseEnter={() => setSelectedIndex(i)}
              style={{
                padding: '6px 10px',
                fontSize: FONT_SIZE_SMALL,
                color: TEXT_DEFAULT,
                cursor: 'pointer',
                background: i === selectedIndex ? SELECTION_BG : 'transparent',
                borderRadius: 3,
                display: 'flex',
                justifyContent: 'space-between',
              }}
            >
              <span>{node.name}</span>
              <span style={{ color: TEXT_DISABLED, fontSize: 10 }}>{node.category}</span>
            </div>
          ))}
          {filteredNodes.length === 0 && (
            <div style={{ padding: 16, color: TEXT_DISABLED, textAlign: 'center', fontSize: FONT_SIZE_SMALL }}>No nodes match</div>
          )}
        </div>
      </div>
    </div>
  );
}

function CategoryPill({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      style={{
        background: active ? ZINC_600 : 'transparent',
        color: active ? TEXT_STRONG : TEXT_DISABLED,
        border: `1px solid ${active ? ZINC_600 : ZINC_600}`,
        borderRadius: 12,
        padding: '1px 8px',
        fontSize: 10,
        cursor: 'pointer',
        fontFamily: 'inherit',
      }}
    >
      {label}
    </button>
  );
}
