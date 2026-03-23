import React from 'react';
import { useStore } from '../state/store';
import { DragValue } from './DragValue';
import type { Port, Value } from 'nodebox-core';

export function ParameterPanel() {
  const library = useStore((s) => s.library);
  const activeNode = useStore((s) => s.activeNode);
  const setPortValueAction = useStore((s) => s.setPortValueAction);
  const setCanvasProperty = useStore((s) => s.setCanvasProperty);
  const pushHistory = useStore((s) => s.pushHistory);
  const currentNetworkPath = useStore((s) => s.currentNetworkPath);

  if (!library) return null;

  const parts = currentNetworkPath.split('/').filter(Boolean);
  let network = library.root;
  const startIdx = parts[0] === network.name ? 1 : 0;
  for (let i = startIdx; i < parts.length; i++) {
    const child = network.children.find((c: any) => c.name === parts[i]);
    if (!child) break;
    network = child;
  }

  const node = activeNode ? network.children.find((c: any) => c.name === activeNode) : null;

  // Document properties (no node selected)
  if (!node) {
    const w = parseFloat(library.properties.canvasWidth ?? '1000');
    const h = parseFloat(library.properties.canvasHeight ?? '1000');
    const bg = library.properties.canvasBackground ?? '#e4e4e7';
    return (
      <div className="pt-2">
        <DocRow label="width">
          <DragValue value={w} onChange={(v) => setCanvasProperty('canvasWidth', String(Math.round(v)))} onCommit={pushHistory} step={1} precision={0} min={1} />
        </DocRow>
        <DocRow label="height">
          <DragValue value={h} onChange={(v) => setCanvasProperty('canvasHeight', String(Math.round(v)))} onCommit={pushHistory} step={1} precision={0} min={1} />
        </DocRow>
        <DocRow label="background">
          <input
            type="color"
            value={bg.startsWith('#') ? bg.slice(0, 7) : '#e4e4e7'}
            onChange={(e) => setCanvasProperty('canvasBackground', e.target.value)}
            onBlur={() => pushHistory()}
            className="w-6 h-5 border-none p-0 cursor-pointer"
          />
        </DocRow>
      </div>
    );
  }

  const nodePath = `${currentNetworkPath}/${node.name}`;
  const connectedPorts = new Set<string>();
  for (const conn of network.connections) {
    if (conn.inputNode === node.name) connectedPorts.add(conn.inputPort);
  }

  return (
    <div className="pt-2">
      {node.inputs
        .filter((p: Port) => p.type !== 'context')
        .map((port: Port) => (
          <PortRow
            key={port.name}
            port={port}
            isConnected={connectedPorts.has(port.name)}
            onChange={(value) => setPortValueAction(nodePath, port.name, value)}
            onCommit={() => pushHistory()}
          />
        ))}
    </div>
  );
}

// ─── Document property row ───────────────────────────────

function DocRow({ label, children }: { label: string; children: React.ReactNode }) {
  const [hovered, setHovered] = React.useState(false);
  return (
    <div className="flex items-center h-9" onMouseEnter={() => setHovered(true)} onMouseLeave={() => setHovered(false)}>
      <div className={`w-28 shrink-0 text-[11px] text-zinc-300 text-right pr-2 h-full flex items-center justify-end ${hovered ? 'bg-field-hover' : 'bg-zinc-800'}`}>
        {label}
      </div>
      <div className={`flex-1 h-full flex items-center ${hovered ? 'bg-field-hover' : 'bg-zinc-700'}`}>
        {children}
      </div>
    </div>
  );
}

// ─── Port row ────────────────────────────────────────────

function PortRow({ port, isConnected, onChange, onCommit }: { port: Port; isConnected: boolean; onChange: (v: Value) => void; onCommit: () => void }) {
  const [hovered, setHovered] = React.useState(false);
  const dragging = React.useRef(false);
  const startX = React.useRef(0);
  const startValues = React.useRef<any>(0);
  const isNumeric = !isConnected && (port.type === 'float' || port.type === 'int' || port.type === 'point');

  const handleLabelPointerDown = React.useCallback((e: React.PointerEvent) => {
    if (!isNumeric) return;
    dragging.current = true;
    startX.current = e.clientX;
    startValues.current = port.value.type === 'point' ? { ...(port.value as any).value } : (port.value as any).value;
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
    e.preventDefault();
  }, [port, isNumeric]);

  const handleLabelPointerMove = React.useCallback((e: React.PointerEvent) => {
    if (!dragging.current) return;
    const dx = e.clientX - startX.current;
    const v = port.value;
    if (v.type === 'float' || v.type === 'int') {
      const step = v.type === 'int' ? 1 : 0.1;
      const newVal = (startValues.current as number) + dx * step;
      onChange({ type: v.type, value: v.type === 'int' ? Math.round(newVal) : parseFloat(newVal.toFixed(2)) } as Value);
    } else if (v.type === 'point') {
      const sv = startValues.current as { x: number; y: number };
      onChange({ type: 'point', value: { x: parseFloat((sv.x + dx * 0.1).toFixed(2)), y: parseFloat((sv.y + dx * 0.1).toFixed(2)) } });
    }
  }, [port, onChange]);

  const handleLabelPointerUp = React.useCallback(() => {
    if (dragging.current) { dragging.current = false; onCommit(); }
  }, [onCommit]);

  return (
    <div className="flex items-center h-9" onMouseEnter={() => setHovered(true)} onMouseLeave={() => setHovered(false)}>
      {/* Label: fixed width, right-aligned, darker bg */}
      <div
        className={`w-28 shrink-0 h-full flex items-center justify-end pr-2 text-[11px] text-zinc-300 overflow-hidden whitespace-nowrap select-none ${isNumeric ? 'cursor-ew-resize' : ''} ${hovered ? 'bg-field-hover' : 'bg-zinc-800'}`}
        onPointerDown={handleLabelPointerDown}
        onPointerMove={handleLabelPointerMove}
        onPointerUp={handleLabelPointerUp}
      >
        {port.label || port.name}
      </div>
      {/* Value: fill remaining, lighter bg */}
      <div className={`flex-1 h-full flex items-center ${hovered ? 'bg-field-hover' : 'bg-zinc-700'}`}>
        {isConnected ? (
          <span className="text-[11px] text-zinc-400 italic px-2">connected</span>
        ) : (
          <PortWidget port={port} onChange={onChange} onCommit={onCommit} />
        )}
      </div>
    </div>
  );
}

// ─── Port value widgets ──────────────────────────────────

function PortWidget({ port, onChange, onCommit }: { port: Port; onChange: (v: Value) => void; onCommit: () => void }) {
  const value = port.value;

  if (port.widget === 'menu' && port.menuItems.length > 0) {
    return (
      <select
        value={value.type === 'string' ? value.value : ''}
        onChange={(e) => { onChange({ type: 'string', value: e.target.value }); onCommit(); }}
        className="w-full bg-transparent text-zinc-100 border-none text-[13px] px-2 py-0.5 font-[inherit] outline-none"
      >
        {port.menuItems.map((item) => (<option key={item.key} value={item.key}>{item.label}</option>))}
      </select>
    );
  }

  if (port.widget === 'toggle' || port.type === 'boolean') {
    const bval = value.type === 'boolean' ? value.value : false;
    return (
      <span
        onClick={() => { onChange({ type: 'boolean', value: !bval }); onCommit(); }}
        className="text-[13px] text-zinc-100 px-2 cursor-pointer select-none hover:text-zinc-50"
      >
        {bval ? 'true' : 'false'}
      </span>
    );
  }

  if (port.type === 'color') {
    const c = value.type === 'color' ? value.value : { r: 0, g: 0, b: 0, a: 1 };
    const hex = `#${Math.round(c.r * 255).toString(16).padStart(2, '0')}${Math.round(c.g * 255).toString(16).padStart(2, '0')}${Math.round(c.b * 255).toString(16).padStart(2, '0')}`;
    return (
      <div className="flex items-center px-2">
        <input
          type="color" value={hex}
          onChange={(e) => { const h = e.target.value.slice(1); onChange({ type: 'color', value: { r: parseInt(h.slice(0, 2), 16) / 255, g: parseInt(h.slice(2, 4), 16) / 255, b: parseInt(h.slice(4, 6), 16) / 255, a: c.a } }); }}
          onBlur={onCommit}
          className="w-6 h-5 border-none p-0 cursor-pointer"
        />
      </div>
    );
  }

  if (port.type === 'point') {
    const p = value.type === 'point' ? value.value : { x: 0, y: 0 };
    return (
      <div className="flex gap-4 w-full">
        <DragValue value={p.x} onChange={(x) => onChange({ type: 'point', value: { x, y: p.y } })} onCommit={onCommit} step={0.1} />
        <DragValue value={p.y} onChange={(y) => onChange({ type: 'point', value: { x: p.x, y } })} onCommit={onCommit} step={0.1} />
      </div>
    );
  }

  if (port.type === 'float' || port.type === 'int') {
    const num = value.type === 'float' || value.type === 'int' ? value.value : 0;
    return <DragValue value={num} onChange={(v) => onChange({ type: port.type as 'float' | 'int', value: port.type === 'int' ? Math.round(v) : v })} onCommit={onCommit} step={port.type === 'int' ? 1 : 0.1} min={port.minimumValue} max={port.maximumValue} precision={port.type === 'int' ? 0 : 2} />;
  }

  if (port.type === 'string') {
    return (
      <input
        type="text"
        value={value.type === 'string' ? value.value : ''}
        onChange={(e) => onChange({ type: 'string', value: e.target.value })}
        onBlur={onCommit}
        className="w-full bg-transparent hover:bg-field-hover text-zinc-100 border-none text-[13px] px-2 py-0.5 font-[inherit] outline-none"
      />
    );
  }

  return <span className="text-[11px] text-zinc-400 px-2">{port.type}</span>;
}
