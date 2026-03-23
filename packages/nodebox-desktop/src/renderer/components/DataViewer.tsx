import React from 'react';
import { useStore } from '../state/store';
import type { Value } from 'nodebox-core';
import {
  TABLE_ROW_EVEN, TABLE_ROW_ODD, TABLE_HEADER_BG,
  TEXT_DEFAULT, TEXT_SUBDUED, FONT_SIZE_SMALL, ZINC_200, ZINC_300,
} from '../theme/tokens';

export function DataViewer() {
  const output = useStore((s) => s.output);

  if (output.length === 0) {
    return <div style={{ padding: 16, color: TEXT_SUBDUED, fontSize: FONT_SIZE_SMALL }}>No data to display</div>;
  }

  // Determine columns from data
  const firstData = output.find((v) => v.type === 'data');
  if (firstData && firstData.type === 'data') {
    const columns = Object.keys(firstData.value);
    return (
      <div style={{ overflow: 'auto', height: '100%', fontSize: FONT_SIZE_SMALL }}>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <thead>
            <tr style={{ background: TABLE_HEADER_BG }}>
              <th style={thStyle}>Index</th>
              {columns.map((col) => (
                <th key={col} style={thStyle}>{col}</th>
              ))}
            </tr>
          </thead>
          <tbody>
            {output.map((row, i) => (
              <tr key={i} style={{ background: i % 2 === 0 ? TABLE_ROW_EVEN : TABLE_ROW_ODD }}>
                <td style={{ ...tdStyle, color: TEXT_SUBDUED }}>{i}</td>
                {columns.map((col) => (
                  <td key={col} style={tdStyle}>
                    {row.type === 'data' ? formatCell((row.value as Record<string, unknown>)[col]) : ''}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    );
  }

  // Simple list of values
  return (
    <div style={{ overflow: 'auto', height: '100%', fontSize: FONT_SIZE_SMALL }}>
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ background: TABLE_HEADER_BG }}>
            <th style={thStyle}>Index</th>
            <th style={thStyle}>Value</th>
          </tr>
        </thead>
        <tbody>
          {output.map((v, i) => (
            <tr key={i} style={{ background: i % 2 === 0 ? TABLE_ROW_EVEN : TABLE_ROW_ODD }}>
              <td style={{ ...tdStyle, color: TEXT_SUBDUED }}>{i}</td>
              <td style={tdStyle}>{formatValue(v)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

const thStyle: React.CSSProperties = {
  textAlign: 'left',
  padding: '4px 8px',
  color: ZINC_200,
  fontWeight: 'normal',
  borderBottom: `1px solid ${ZINC_300}20`,
};

const tdStyle: React.CSSProperties = {
  padding: '4px 8px',
  color: TEXT_DEFAULT,
};

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
    case 'color': {
      const { r, g, b, a } = v.value;
      const hex = (n: number) => Math.round(n * 255).toString(16).padStart(2, '0').toUpperCase();
      return `#${hex(r)}${hex(g)}${hex(b)}${hex(a)}`;
    }
    case 'geometry': return `[${v.value.length} paths]`;
    case 'list': return `[${v.value.length} items]`;
    case 'data': return JSON.stringify(v.value);
    case 'null': return '';
  }
}
