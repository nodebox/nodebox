import React, { useState, useCallback, useEffect, useRef, useMemo } from 'react';
import { useStore } from '../state/store';
import { NODE_TEMPLATES, createNodeFromTemplate, getTemplatesByCategory } from '../node-templates';
import {
  DIALOG_BACKGROUND, DIALOG_BORDER, SELECTION_BG,
  TEXT_STRONG, TEXT_DEFAULT, TEXT_DISABLED, ZINC_600,
  FONT_SIZE_SMALL, FONT_SIZE_BASE,
} from '../theme/tokens';

const CATEGORIES = getTemplatesByCategory();

export function NodeSelectionDialog() {
  const visible = useStore((s) => s.nodeSelectionDialogOpen);
  const setVisible = useStore((s) => s.setNodeSelectionDialogOpen);
  const nodeDialogPosition = useStore((s) => s.nodeDialogPosition);
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

  const filteredTemplates = useMemo(() => {
    let all = NODE_TEMPLATES;
    if (activeCategory) all = all.filter((t) => t.category === activeCategory);
    if (search) {
      const lower = search.toLowerCase();
      all = all.filter((t) => t.name.toLowerCase().includes(lower));
    }
    return all;
  }, [search, activeCategory]);

  const addNode = useCallback((template: typeof NODE_TEMPLATES[0]) => {
    pushHistory();
    const library = useStore.getState().library;
    if (!library) return;

    const parts = currentNetworkPath.split('/').filter(Boolean);
    let network = library.root;
    const startIdx = parts[0] === network.name ? 1 : 0;
    for (let i = startIdx; i < parts.length; i++) {
      const child = network.children.find((c: any) => c.name === parts[i]);
      if (!child) break;
      network = child;
    }

    const existing = new Set(network.children.map((c: any) => c.name));
    let name = template.name + '1';
    for (let n = 2; existing.has(name); n++) { name = template.name + n; }

    const pos = nodeDialogPosition ?? { x: 5, y: 5 };
    const node = createNodeFromTemplate(template, name, pos);
    addNodeToNetwork(currentNetworkPath, node);
    setVisible(false);
  }, [addNodeToNetwork, currentNetworkPath, pushHistory, setVisible, nodeDialogPosition]);

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') { e.preventDefault(); setSelectedIndex((i) => Math.min(i + 1, filteredTemplates.length - 1)); }
    if (e.key === 'ArrowUp') { e.preventDefault(); setSelectedIndex((i) => Math.max(0, i - 1)); }
    if (e.key === 'Enter' && filteredTemplates[selectedIndex]) {
      e.preventDefault();
      addNode(filteredTemplates[selectedIndex]);
    }
    if (e.key === 'Escape') { setVisible(false); }
  }, [filteredTemplates, selectedIndex, setVisible, addNode]);

  if (!visible) return null;

  return (
    <div
      onClick={(e) => { if (e.target === e.currentTarget) setVisible(false); }}
      style={{ position: 'fixed', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 100, background: 'rgba(0,0,0,0.4)' }}
    >
      <div style={{ width: 400, maxHeight: 500, background: DIALOG_BACKGROUND, border: `1px solid ${DIALOG_BORDER}`, borderRadius: 6, overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: 8 }}>
          <input
            ref={inputRef}
            type="text"
            value={search}
            onChange={(e) => { setSearch(e.target.value); setSelectedIndex(0); }}
            onKeyDown={handleKeyDown}
            placeholder="Search nodes..."
            style={{ width: '100%', background: ZINC_600, color: TEXT_STRONG, border: 'none', padding: '6px 10px', fontSize: FONT_SIZE_BASE, borderRadius: 4, outline: 'none', fontFamily: 'inherit', boxSizing: 'border-box' }}
          />
        </div>

        <div style={{ display: 'flex', gap: 4, padding: '0 8px 8px', flexWrap: 'wrap' }}>
          <CategoryPill label="All" active={activeCategory === null} onClick={() => { setActiveCategory(null); setSelectedIndex(0); }} />
          {CATEGORIES.map((cat) => (
            <CategoryPill key={cat.category} label={cat.category} active={activeCategory === cat.category} onClick={() => { setActiveCategory(cat.category); setSelectedIndex(0); }} />
          ))}
        </div>

        <div style={{ flex: 1, overflow: 'auto', padding: '0 4px 4px' }}>
          {filteredTemplates.map((tmpl, i) => (
            <div
              key={tmpl.name + tmpl.function}
              onClick={() => addNode(tmpl)}
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
              <span>{tmpl.name}</span>
              <span style={{ color: TEXT_DISABLED, fontSize: 10 }}>{tmpl.category}</span>
            </div>
          ))}
          {filteredTemplates.length === 0 && (
            <div style={{ padding: 16, color: TEXT_DISABLED, textAlign: 'center', fontSize: FONT_SIZE_SMALL }}>No nodes match</div>
          )}
        </div>
      </div>
    </div>
  );
}

function CategoryPill({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button onClick={onClick} style={{
      background: active ? ZINC_600 : 'transparent',
      color: active ? TEXT_STRONG : TEXT_DISABLED,
      border: `1px solid ${ZINC_600}`,
      borderRadius: 12, padding: '1px 8px', fontSize: 10, cursor: 'pointer', fontFamily: 'inherit',
    }}>{label}</button>
  );
}
