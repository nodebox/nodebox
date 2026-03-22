import React from 'react';
import { useStore } from '../state/store.js';
import type { Port, Value } from 'nodebox-core';

export function ParameterPanel() {
  const library = useStore((s) => s.library);
  const selectedNodes = useStore((s) => s.selectedNodes);
  const currentNetworkPath = useStore((s) => s.currentNetworkPath);
  const setPortValueAction = useStore((s) => s.setPortValueAction);

  if (!library || selectedNodes.length !== 1) {
    return <div style={{ padding: 16, color: '#888' }}>Select a node to edit parameters</div>;
  }

  const network = findNodeByPath(library.root, currentNetworkPath);
  if (!network) return null;
  const node = network.children?.find((c: any) => c.name === selectedNodes[0]);
  if (!node) return null;

  const nodePath = `${currentNetworkPath}/${node.name}`;

  return (
    <div style={{ padding: 8 }}>
      <h3 style={{ margin: '0 0 8px 0', fontSize: 14 }}>{node.name}</h3>
      {node.inputs.map((port: Port) => (
        <PortWidget
          key={port.name}
          port={port}
          onChange={(value) => setPortValueAction(nodePath, port.name, value)}
        />
      ))}
    </div>
  );
}

function PortWidget({ port, onChange }: { port: Port; onChange: (v: Value) => void }) {
  const label = port.label || port.name;

  return (
    <div style={{ marginBottom: 6 }}>
      <label style={{ fontSize: 11, color: '#999', display: 'block' }}>{label}</label>
      <PortInput port={port} onChange={onChange} />
    </div>
  );
}

function PortInput({ port, onChange }: { port: Port; onChange: (v: Value) => void }) {
  const value = port.value;

  if (port.widget === 'menu' && port.menuItems.length > 0) {
    return (
      <select
        value={value.type === 'string' ? value.value : ''}
        onChange={(e) => onChange({ type: 'string', value: e.target.value })}
        style={{ width: '100%' }}
      >
        {port.menuItems.map((item) => (
          <option key={item.key} value={item.key}>{item.label}</option>
        ))}
      </select>
    );
  }

  if (port.widget === 'toggle' || port.type === 'boolean') {
    return (
      <input
        type="checkbox"
        checked={value.type === 'boolean' ? value.value : false}
        onChange={(e) => onChange({ type: 'boolean', value: e.target.checked })}
      />
    );
  }

  if (port.type === 'color') {
    const hex = value.type === 'color'
      ? `#${Math.round(value.value.r * 255).toString(16).padStart(2, '0')}${Math.round(value.value.g * 255).toString(16).padStart(2, '0')}${Math.round(value.value.b * 255).toString(16).padStart(2, '0')}`
      : '#000000';
    return (
      <input
        type="color"
        value={hex}
        onChange={(e) => {
          const h = e.target.value.slice(1);
          onChange({
            type: 'color',
            value: {
              r: parseInt(h.slice(0, 2), 16) / 255,
              g: parseInt(h.slice(2, 4), 16) / 255,
              b: parseInt(h.slice(4, 6), 16) / 255,
              a: 1,
            },
          });
        }}
      />
    );
  }

  if (port.type === 'float' || port.type === 'int') {
    const num = value.type === 'float' || value.type === 'int' ? value.value : 0;
    return (
      <input
        type="number"
        value={num}
        step={port.type === 'int' ? 1 : 0.1}
        onChange={(e) => {
          const v = parseFloat(e.target.value);
          if (!isNaN(v)) {
            onChange({ type: port.type as 'float' | 'int', value: port.type === 'int' ? Math.round(v) : v });
          }
        }}
        style={{ width: '100%' }}
      />
    );
  }

  if (port.type === 'string') {
    return (
      <input
        type="text"
        value={value.type === 'string' ? value.value : ''}
        onChange={(e) => onChange({ type: 'string', value: e.target.value })}
        style={{ width: '100%' }}
      />
    );
  }

  return <span style={{ fontSize: 11, color: '#666' }}>{port.type}</span>;
}

function findNodeByPath(root: any, path: string): any {
  const parts = path.split('/').filter(Boolean);
  let current = root;
  const startIdx = parts[0] === current.name ? 1 : 0;
  for (let i = startIdx; i < parts.length; i++) {
    const child = current.children?.find((c: any) => c.name === parts[i]);
    if (!child) return null;
    current = child;
  }
  return current;
}
