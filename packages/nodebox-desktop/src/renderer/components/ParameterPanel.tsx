import React from 'react';
import { useStore } from '../state/store';
import { DragValue } from './DragValue';
import type { Port, Value } from 'nodebox-core';

export function ParameterPanel() {
  const library = useStore((s) => s.library);
  const activeNode = useStore((s) => s.activeNode);
  const setPortValueAction = useStore((s) => s.setPortValueAction);
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

  if (!node) {
    return (
      <div>
        <ParamRow label="width" value={library.properties.canvasWidth ?? '1000'} />
        <ParamRow label="height" value={library.properties.canvasHeight ?? '1000'} />
        <ParamRow label="background" value={library.properties.canvasBackground ?? '#e4e4e7'} />
      </div>
    );
  }

  const nodePath = `${currentNetworkPath}/${node.name}`;
  const connectedPorts = new Set<string>();
  for (const conn of network.connections) {
    if (conn.inputNode === node.name) connectedPorts.add(conn.inputPort);
  }

  return (
    <div>
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

function ParamRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-center h-9 border-b border-zinc-800">
      <div className="w-28 px-3 text-[11px] text-zinc-300 shrink-0">{label}</div>
      <div className="flex-1 px-3 text-[13px] text-zinc-100 text-right">{value}</div>
    </div>
  );
}

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
    startValues.current = port.value.type === 'point' ? { ...port.value.value } : port.value.value;
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
    <div className="flex items-center h-9 border-b border-zinc-800" onMouseEnter={() => setHovered(true)} onMouseLeave={() => setHovered(false)}>
      <div
        className={`w-28 px-3 py-2 text-[11px] text-zinc-300 shrink-0 overflow-hidden text-ellipsis whitespace-nowrap select-none ${isNumeric ? 'cursor-ew-resize' : ''} ${hovered ? 'bg-field-hover' : 'bg-panel'}`}
        onPointerDown={handleLabelPointerDown}
        onPointerMove={handleLabelPointerMove}
        onPointerUp={handleLabelPointerUp}
      >
        {port.label || port.name}
      </div>
      <div className={`flex-1 px-3 py-1 ${hovered ? 'bg-field-hover' : 'bg-zinc-700'}`}>
        {isConnected ? (
          <span className="text-[11px] text-zinc-400 italic">connected</span>
        ) : (
          <PortWidget port={port} onChange={onChange} onCommit={onCommit} />
        )}
      </div>
    </div>
  );
}

function PortWidget({ port, onChange, onCommit }: { port: Port; onChange: (v: Value) => void; onCommit: () => void }) {
  const value = port.value;

  if (port.widget === 'menu' && port.menuItems.length > 0) {
    return (
      <select
        value={value.type === 'string' ? value.value : ''}
        onChange={(e) => { onChange({ type: 'string', value: e.target.value }); onCommit(); }}
        className="w-full bg-zinc-700 text-zinc-100 border-none text-[13px] px-1 py-0.5 font-[inherit]"
      >
        {port.menuItems.map((item) => (<option key={item.key} value={item.key}>{item.label}</option>))}
      </select>
    );
  }
  if (port.widget === 'toggle' || port.type === 'boolean') {
    return <input type="checkbox" checked={value.type === 'boolean' ? value.value : false} onChange={(e) => { onChange({ type: 'boolean', value: e.target.checked }); onCommit(); }} />;
  }
  if (port.type === 'color') {
    const c = value.type === 'color' ? value.value : { r: 0, g: 0, b: 0, a: 1 };
    const hex = `#${Math.round(c.r * 255).toString(16).padStart(2, '0')}${Math.round(c.g * 255).toString(16).padStart(2, '0')}${Math.round(c.b * 255).toString(16).padStart(2, '0')}`;
    return (
      <div className="flex items-center gap-1">
        <input type="color" value={hex} onChange={(e) => { const h = e.target.value.slice(1); onChange({ type: 'color', value: { r: parseInt(h.slice(0, 2), 16) / 255, g: parseInt(h.slice(2, 4), 16) / 255, b: parseInt(h.slice(4, 6), 16) / 255, a: c.a } }); }} onBlur={onCommit} className="w-6 h-5 border-none p-0 cursor-pointer" />
      </div>
    );
  }
  if (port.type === 'point') {
    const p = value.type === 'point' ? value.value : { x: 0, y: 0 };
    return (
      <div className="flex gap-1">
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
    return <input type="text" value={value.type === 'string' ? value.value : ''} onChange={(e) => onChange({ type: 'string', value: e.target.value })} onBlur={onCommit} className="w-full bg-zinc-700 text-zinc-100 border-none text-[13px] px-1.5 py-0.5 font-[inherit]" />;
  }
  return <span className="text-[11px] text-zinc-400">{port.type}</span>;
}
