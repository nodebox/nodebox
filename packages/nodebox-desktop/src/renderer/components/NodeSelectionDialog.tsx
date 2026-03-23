import React, { useState, useCallback, useEffect, useRef, useMemo } from 'react';
import { useStore } from '../state/store';
import { NODE_TEMPLATES, createNodeFromTemplate, getTemplatesByCategory } from '../node-templates';
import { isCompatible } from 'nodebox-core';

const CATEGORIES = getTemplatesByCategory();

export function NodeSelectionDialog() {
  const visible = useStore((s) => s.nodeSelectionDialogOpen);
  const setVisible = useStore((s) => s.setNodeSelectionDialogOpen);
  const nodeDialogPosition = useStore((s) => s.nodeDialogPosition);
  const pendingConnection = useStore((s) => s.pendingConnection);
  const addNodeToNetwork = useStore((s) => s.addNodeToNetwork);
  const addConnectionToNetwork = useStore((s) => s.addConnectionToNetwork);
  const currentNetworkPath = useStore((s) => s.currentNetworkPath);
  const pushHistory = useStore((s) => s.pushHistory);

  const [search, setSearch] = useState('');
  const [selectedIndex, setSelectedIndex] = useState(0);
  const [activeCategory, setActiveCategory] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (visible) { setSearch(''); setSelectedIndex(0); setActiveCategory(null); setTimeout(() => inputRef.current?.focus(), 50); }
  }, [visible]);

  const filteredTemplates = useMemo(() => {
    let all = NODE_TEMPLATES;
    if (activeCategory) all = all.filter((t) => t.category === activeCategory);
    if (search) { const lower = search.toLowerCase(); all = all.filter((t) => t.name.toLowerCase().includes(lower)); }
    if (pendingConnection) {
      const compat = all.filter((t) => { const fi = t.inputs.find((p) => p.type !== 'context'); return fi && isCompatible(pendingConnection.outputType, fi.type); });
      const incompat = all.filter((t) => !compat.includes(t));
      return [...compat, ...incompat];
    }
    return all;
  }, [search, activeCategory, pendingConnection]);

  const addNode = useCallback((template: typeof NODE_TEMPLATES[0]) => {
    pushHistory();
    const library = useStore.getState().library;
    if (!library) return;
    const parts = currentNetworkPath.split('/').filter(Boolean);
    let network = library.root;
    const startIdx = parts[0] === network.name ? 1 : 0;
    for (let i = startIdx; i < parts.length; i++) { const child = network.children.find((c: any) => c.name === parts[i]); if (!child) break; network = child; }
    const existing = new Set(network.children.map((c: any) => c.name));
    let name = template.name + '1';
    for (let n = 2; existing.has(name); n++) { name = template.name + n; }
    const pos = nodeDialogPosition ?? { x: 5, y: 5 };
    const node = createNodeFromTemplate(template, name, pos);
    addNodeToNetwork(currentNetworkPath, node);
    if (pendingConnection) {
      const firstInput = template.inputs.find((p) => p.type !== 'context');
      if (firstInput && isCompatible(pendingConnection.outputType, firstInput.type)) {
        addConnectionToNetwork(currentNetworkPath, { outputNode: pendingConnection.fromNode, inputNode: name, inputPort: firstInput.name });
      }
    }
    setVisible(false);
  }, [addNodeToNetwork, addConnectionToNetwork, currentNetworkPath, pushHistory, setVisible, nodeDialogPosition, pendingConnection]);

  const handleKeyDown = useCallback((e: React.KeyboardEvent) => {
    if (e.key === 'ArrowDown') { e.preventDefault(); setSelectedIndex((i) => Math.min(i + 1, filteredTemplates.length - 1)); }
    if (e.key === 'ArrowUp') { e.preventDefault(); setSelectedIndex((i) => Math.max(0, i - 1)); }
    if (e.key === 'Enter' && filteredTemplates[selectedIndex]) { e.preventDefault(); addNode(filteredTemplates[selectedIndex]); }
    if (e.key === 'Escape') { setVisible(false); }
  }, [filteredTemplates, selectedIndex, setVisible, addNode]);

  if (!visible) return null;

  return (
    <div onClick={(e) => { if (e.target === e.currentTarget) setVisible(false); }} className="fixed inset-0 flex items-center justify-center z-[100] bg-black/40">
      <div className="w-[400px] max-h-[500px] bg-zinc-700 border border-zinc-500 rounded-md overflow-hidden flex flex-col">
        <div className="p-2">
          <input
            ref={inputRef} type="text" value={search}
            onChange={(e) => { setSearch(e.target.value); setSelectedIndex(0); }}
            onKeyDown={handleKeyDown}
            placeholder="Search nodes..."
            className="w-full bg-zinc-600 text-zinc-50 border-none px-2.5 py-1.5 text-[13px] rounded outline-none font-[inherit]"
          />
        </div>
        <div className="flex gap-1 px-2 pb-2 flex-wrap">
          <CategoryPill label="All" active={activeCategory === null} onClick={() => { setActiveCategory(null); setSelectedIndex(0); }} />
          {CATEGORIES.map((cat) => (
            <CategoryPill key={cat.category} label={cat.category} active={activeCategory === cat.category} onClick={() => { setActiveCategory(cat.category); setSelectedIndex(0); }} />
          ))}
        </div>
        <div className="flex-1 overflow-auto px-1 pb-1">
          {filteredTemplates.map((tmpl, i) => (
            <div
              key={tmpl.name + tmpl.function}
              onClick={() => addNode(tmpl)}
              onMouseEnter={() => setSelectedIndex(i)}
              className={`px-2.5 py-1.5 text-[11px] text-zinc-100 cursor-pointer rounded-sm flex justify-between ${i === selectedIndex ? 'bg-violet-800' : ''}`}
            >
              <span>{tmpl.name}</span>
              <span className="text-zinc-400 text-[10px]">{tmpl.category}</span>
            </div>
          ))}
          {filteredTemplates.length === 0 && <div className="p-4 text-zinc-400 text-center text-[11px]">No nodes match</div>}
        </div>
      </div>
    </div>
  );
}

function CategoryPill({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button onClick={onClick} className={`border border-zinc-600 rounded-xl px-2 py-px text-[10px] cursor-pointer font-[inherit] ${active ? 'bg-zinc-600 text-zinc-50' : 'bg-transparent text-zinc-400'}`}>
      {label}
    </button>
  );
}
