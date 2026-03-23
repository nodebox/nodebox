import React, { useState, useCallback, useEffect, useRef, useMemo } from 'react';
import { useStore } from '../state/store';
import { NODE_TEMPLATES, createNodeFromTemplate, getTemplatesByCategory } from '../node-templates';
import { isCompatible } from 'nodebox-core';

const CATEGORIES = getTemplatesByCategory();

function iconName(tmpl: { name: string; function: string }): string {
  const fn = tmpl.function;
  const name = fn.substring(fn.lastIndexOf('/') + 1);
  const map: Record<string, string> = {
    lineAngle: 'line_angle', quadCurve: 'quad_curve', makePoint: 'make_point',
    pointOnPath: 'point_on_path', shapeOnPath: 'shape_on_path', fitTo: 'fit_to',
    sortShapes: 'sort', randomNumbers: 'generator', rgbColor: 'colorize',
    hsbColor: 'colorize', grayColor: 'colorize',
  };
  return map[name] ?? name;
}

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
  const listRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (visible) { setSearch(''); setSelectedIndex(0); setActiveCategory(null); setTimeout(() => inputRef.current?.focus(), 50); }
  }, [visible]);

  useEffect(() => {
    const item = listRef.current?.children[selectedIndex] as HTMLElement | undefined;
    item?.scrollIntoView({ block: 'nearest' });
  }, [selectedIndex]);

  const filteredTemplates = useMemo(() => {
    let all = NODE_TEMPLATES;
    if (activeCategory) all = all.filter((t) => t.category === activeCategory);
    if (search) {
      const lower = search.toLowerCase();
      all = all.filter((t) => t.name.toLowerCase().includes(lower) || t.description.toLowerCase().includes(lower));
    }
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
    const si = parts[0] === network.name ? 1 : 0;
    for (let i = si; i < parts.length; i++) { const c = network.children.find((c: any) => c.name === parts[i]); if (!c) break; network = c; }
    const existing = new Set(network.children.map((c: any) => c.name));
    let name = template.name + '1';
    for (let n = 2; existing.has(name); n++) { name = template.name + n; }
    addNodeToNetwork(currentNetworkPath, createNodeFromTemplate(template, name, nodeDialogPosition ?? { x: 5, y: 5 }));
    if (pendingConnection) {
      const fi = template.inputs.find((p) => p.type !== 'context');
      if (fi && isCompatible(pendingConnection.outputType, fi.type)) {
        addConnectionToNetwork(currentNetworkPath, { outputNode: pendingConnection.fromNode, inputNode: name, inputPort: fi.name });
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
      <div className="w-[560px] h-[500px] bg-zinc-700 border border-zinc-500 rounded-lg overflow-hidden flex flex-col shadow-2xl">
        {/* Search */}
        <div className="p-3">
          <input ref={inputRef} type="text" value={search}
            onChange={(e) => { setSearch(e.target.value); setSelectedIndex(0); }}
            onKeyDown={handleKeyDown} placeholder="Search nodes..."
            className="w-full bg-zinc-600 text-zinc-50 border-none px-3 py-2 text-[13px] rounded outline-none font-[inherit] placeholder:text-zinc-400"
          />
        </div>

        {/* Category pills */}
        <div className="flex gap-1.5 px-3 pb-3 flex-wrap">
          <CategoryPill label="All" active={activeCategory === null} onClick={() => { setActiveCategory(null); setSelectedIndex(0); }} />
          {CATEGORIES.map((cat) => (
            <CategoryPill key={cat.category} label={cat.category} active={activeCategory === cat.category} onClick={() => { setActiveCategory(cat.category); setSelectedIndex(0); }} />
          ))}
        </div>

        <div className="h-px bg-zinc-600 mx-3" />

        {/* Node list */}
        <div ref={listRef} className="flex-1 overflow-auto py-2 px-2">
          {filteredTemplates.map((tmpl, i) => (
            <div key={tmpl.name + tmpl.function} onClick={() => addNode(tmpl)} onMouseEnter={() => setSelectedIndex(i)}
              className={`flex items-center gap-3 px-3 py-2 cursor-pointer rounded ${i === selectedIndex ? 'bg-violet-800' : 'hover:bg-zinc-600'}`}
            >
              <img src={`/icons/corevector/${iconName(tmpl)}.svg`} alt="" className="w-5 h-5 shrink-0 invert opacity-70"
                onError={(e) => { (e.target as HTMLImageElement).style.display = 'none'; }} />
              <div className="flex-1 min-w-0">
                <div className="text-[13px] text-zinc-100 leading-tight">{tmpl.name}</div>
                <div className="text-[10px] text-zinc-400 leading-tight truncate">{tmpl.description}</div>
              </div>
              <span className="text-[10px] text-zinc-400 shrink-0">{tmpl.category}</span>
            </div>
          ))}
          {filteredTemplates.length === 0 && <div className="py-8 text-zinc-400 text-center text-[13px]">No nodes match your search.</div>}
        </div>
      </div>
    </div>
  );
}

function CategoryPill({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <button
      onClick={onClick}
      className={`border-b-2 px-2 py-1 text-[11px] cursor-pointer font-[inherit] ${active ? 'bg-zinc-600 text-zinc-50 border-b-violet-400' : 'bg-transparent text-zinc-400 border-b-transparent hover:text-zinc-300'}`}
    >{label}</button>
  );
}
