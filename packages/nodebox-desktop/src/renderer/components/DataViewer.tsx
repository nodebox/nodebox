import React from 'react';
import { useStore } from '../state/store';
import type { Value } from 'nodebox-core';

export function DataViewer() {
  const output = useStore((s) => s.output);

  if (output.length === 0) {
    return <div className="p-4 text-zinc-300 text-[11px]">No data to display</div>;
  }

  const firstData = output.find((v) => v.type === 'data');
  if (firstData && firstData.type === 'data') {
    const columns = Object.keys(firstData.value);
    return (
      <div className="overflow-auto h-full text-[11px]">
        <table className="w-full border-collapse">
          <thead>
            <tr className="bg-zinc-700">
              <th className="text-left px-2 py-1 text-zinc-200 font-normal border-b border-zinc-300/10">Index</th>
              {columns.map((col) => <th key={col} className="text-left px-2 py-1 text-zinc-200 font-normal border-b border-zinc-300/10">{col}</th>)}
            </tr>
          </thead>
          <tbody>
            {output.map((row, i) => (
              <tr key={i} className={i % 2 === 0 ? 'bg-zinc-800' : 'bg-[#333338]'}>
                <td className="px-2 py-1 text-zinc-300">{i}</td>
                {columns.map((col) => <td key={col} className="px-2 py-1 text-zinc-100">{row.type === 'data' ? formatCell((row.value as Record<string, unknown>)[col]) : ''}</td>)}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  return (
    <div className="overflow-auto h-full text-[11px]">
      <table className="w-full border-collapse">
        <thead>
          <tr className="bg-zinc-700">
            <th className="text-left px-2 py-1 text-zinc-200 font-normal border-b border-zinc-300/10">Index</th>
            <th className="text-left px-2 py-1 text-zinc-200 font-normal border-b border-zinc-300/10">Value</th>
          </tr>
        </thead>
        <tbody>
          {output.map((v, i) => (
            <tr key={i} className={i % 2 === 0 ? 'bg-zinc-800' : 'bg-[#333338]'}>
              <td className="px-2 py-1 text-zinc-300">{i}</td>
              <td className="px-2 py-1 text-zinc-100">{formatValue(v)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function formatCell(val: unknown): string {
  if (val === null || val === undefined) return '';
  if (typeof val === 'number') return val.toFixed(2);
  return String(val);
}

function formatValue(v: Value): string {
  switch (v.type) {
    case 'int': case 'float': return String(v.value);
    case 'string': return v.value;
    case 'boolean': return String(v.value);
    case 'point': return `${v.value.x.toFixed(2)}, ${v.value.y.toFixed(2)}`;
    case 'color': { const { r, g, b, a } = v.value; const hex = (n: number) => Math.round(n * 255).toString(16).padStart(2, '0').toUpperCase(); return `#${hex(r)}${hex(g)}${hex(b)}${hex(a)}`; }
    case 'geometry': return `[${v.value.length} paths]`;
    case 'list': return `[${v.value.length} items]`;
    case 'data': return JSON.stringify(v.value);
    case 'null': return '';
  }
}
