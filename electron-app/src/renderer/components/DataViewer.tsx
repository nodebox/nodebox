import { useStore } from '../state/store';
import {
  TABLE_ROW_EVEN,
  TABLE_ROW_ODD,
  TABLE_HEADER_BG,
  TABLE_HEADER_TEXT,
  TABLE_CELL_TEXT,
  TABLE_INDEX_TEXT,
  TABLE_HEADER_HEIGHT,
  ROW_HEIGHT,
  FONT_SIZE_SMALL,
  PANEL_BG,
} from '../theme/tokens';
import type { PathRenderData } from '../types/eval-result';

function colorToHex(c: { r: number; g: number; b: number; a: number }): string {
  const r = Math.round(c.r * 255);
  const g = Math.round(c.g * 255);
  const b = Math.round(c.b * 255);
  return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
}

function totalPoints(path: PathRenderData): number {
  let count = 0;
  for (const contour of path.contours) {
    count += contour.points.length;
  }
  return count;
}

const cellStyle: React.CSSProperties = {
  padding: '0 8px',
  fontSize: FONT_SIZE_SMALL,
  whiteSpace: 'nowrap',
  overflow: 'hidden',
  textOverflow: 'ellipsis',
};

const headerCellStyle: React.CSSProperties = {
  ...cellStyle,
  color: TABLE_HEADER_TEXT,
  fontWeight: 600,
  height: TABLE_HEADER_HEIGHT,
  lineHeight: `${TABLE_HEADER_HEIGHT}px`,
};

export function DataViewer() {
  const renderResult = useStore((s) => s.renderResult);

  if (!renderResult || renderResult.paths.length === 0) {
    return (
      <div
        data-testid="data-viewer"
        style={{
          width: '100%',
          height: '100%',
          background: PANEL_BG,
          color: TABLE_CELL_TEXT,
          fontSize: FONT_SIZE_SMALL,
          padding: 16,
        }}
      >
        No data to display.
      </div>
    );
  }

  return (
    <div
      data-testid="data-viewer"
      style={{
        width: '100%',
        height: '100%',
        overflow: 'auto',
        background: PANEL_BG,
      }}
    >
      <table style={{ width: '100%', borderCollapse: 'collapse' }}>
        <thead>
          <tr style={{ background: TABLE_HEADER_BG }}>
            <th style={{ ...headerCellStyle, width: 40, textAlign: 'right' }}>#</th>
            <th style={{ ...headerCellStyle, textAlign: 'left' }}>Fill</th>
            <th style={{ ...headerCellStyle, textAlign: 'left' }}>Stroke</th>
            <th style={{ ...headerCellStyle, textAlign: 'right' }}>Contours</th>
            <th style={{ ...headerCellStyle, textAlign: 'right' }}>Points</th>
          </tr>
        </thead>
        <tbody>
          {renderResult.paths.map((path, i) => (
            <tr
              key={i}
              data-testid="data-row"
              style={{
                background: i % 2 === 0 ? TABLE_ROW_EVEN : TABLE_ROW_ODD,
                height: ROW_HEIGHT,
              }}
            >
              <td style={{ ...cellStyle, color: TABLE_INDEX_TEXT, textAlign: 'right' }}>
                {i}
              </td>
              <td style={{ ...cellStyle, color: TABLE_CELL_TEXT, textAlign: 'left' }}>
                {path.fill ? (
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                    <span
                      style={{
                        display: 'inline-block',
                        width: 12,
                        height: 12,
                        background: colorToHex(path.fill),
                        border: '1px solid rgba(255,255,255,0.2)',
                      }}
                    />
                    {colorToHex(path.fill)}
                  </span>
                ) : (
                  'none'
                )}
              </td>
              <td style={{ ...cellStyle, color: TABLE_CELL_TEXT, textAlign: 'left' }}>
                {path.stroke ? (
                  <span style={{ display: 'inline-flex', alignItems: 'center', gap: 4 }}>
                    <span
                      style={{
                        display: 'inline-block',
                        width: 12,
                        height: 12,
                        background: colorToHex(path.stroke),
                        border: '1px solid rgba(255,255,255,0.2)',
                      }}
                    />
                    {colorToHex(path.stroke)}
                  </span>
                ) : (
                  'none'
                )}
              </td>
              <td style={{ ...cellStyle, color: TABLE_CELL_TEXT, textAlign: 'right' }}>
                {path.contours.length}
              </td>
              <td style={{ ...cellStyle, color: TABLE_CELL_TEXT, textAlign: 'right' }}>
                {totalPoints(path)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
