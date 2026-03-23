import React from 'react';
import { useStore } from '../state/store';
import { DragValue } from './DragValue';
import type { Port, Value } from 'nodebox-core';
import {
  PANEL_BG, ZINC_700, ZINC_800, TEXT_DEFAULT, TEXT_DISABLED, TEXT_SUBDUED,
  FONT_SIZE_SMALL, FONT_SIZE_BASE, LABEL_WIDTH, PARAMETER_ROW_HEIGHT,
} from '../theme/tokens';

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
        <ParameterRow label="width" value={library.properties.canvasWidth ?? '1000'} />
        <ParameterRow label="height" value={library.properties.canvasHeight ?? '1000'} />
        <ParameterRow label="background" value={library.properties.canvasBackground ?? '#e4e4e7'} />
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

function ParameterRow({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', height: PARAMETER_ROW_HEIGHT, borderBottom: `1px solid ${ZINC_800}` }}>
      <div style={{ width: LABEL_WIDTH, paddingLeft: 8, fontSize: FONT_SIZE_SMALL, color: TEXT_SUBDUED, flexShrink: 0 }}>{label}</div>
      <div style={{ flex: 1, paddingRight: 8, fontSize: FONT_SIZE_BASE, color: TEXT_DEFAULT, textAlign: 'right' }}>{value}</div>
    </div>
  );
}

function PortRow({ port, isConnected, onChange, onCommit }: { port: Port; isConnected: boolean; onChange: (v: Value) => void; onCommit: () => void }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', height: PARAMETER_ROW_HEIGHT, borderBottom: `1px solid ${ZINC_800}`, background: PANEL_BG }}>
      <div style={{ width: LABEL_WIDTH, paddingLeft: 8, fontSize: FONT_SIZE_SMALL, color: TEXT_SUBDUED, flexShrink: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
        {port.label || port.name}
      </div>
      <div style={{ flex: 1, paddingRight: 8 }}>
        {isConnected ? (
          <span style={{ fontSize: FONT_SIZE_SMALL, color: TEXT_DISABLED, fontStyle: 'italic' }}>connected</span>
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
        style={{ width: '100%', background: ZINC_700, color: TEXT_DEFAULT, border: 'none', fontSize: FONT_SIZE_BASE, padding: '2px 4px', fontFamily: 'inherit' }}
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
      <div style={{ display: 'flex', alignItems: 'center', gap: 4 }}>
        <input type="color" value={hex} onChange={(e) => { const h = e.target.value.slice(1); onChange({ type: 'color', value: { r: parseInt(h.slice(0, 2), 16) / 255, g: parseInt(h.slice(2, 4), 16) / 255, b: parseInt(h.slice(4, 6), 16) / 255, a: c.a } }); }} onBlur={onCommit} style={{ width: 24, height: 20, border: 'none', padding: 0, cursor: 'pointer' }} />
      </div>
    );
  }
  if (port.type === 'point') {
    const p = value.type === 'point' ? value.value : { x: 0, y: 0 };
    return (
      <div style={{ display: 'flex', gap: 4 }}>
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
    return <input type="text" value={value.type === 'string' ? value.value : ''} onChange={(e) => onChange({ type: 'string', value: e.target.value })} onBlur={onCommit} style={{ width: '100%', background: ZINC_700, color: TEXT_DEFAULT, border: 'none', fontSize: FONT_SIZE_BASE, padding: '2px 6px', fontFamily: 'inherit' }} />;
  }
  return <span style={{ fontSize: FONT_SIZE_SMALL, color: TEXT_DISABLED }}>{port.type}</span>;
}
