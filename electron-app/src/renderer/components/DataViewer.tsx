import { useStore } from '../state/store';
import {
  TABLE_ROW_EVEN,
  TABLE_ROW_ODD,
  TABLE_HEADER_HEIGHT,
  ROW_HEIGHT,
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

const cellClass = 'px-2 text-[11px] whitespace-nowrap overflow-hidden text-ellipsis';

const headerCellStyle: React.CSSProperties = {
  height: TABLE_HEADER_HEIGHT,
  lineHeight: `${TABLE_HEADER_HEIGHT}px`,
};

export function DataViewer() {
  const renderResult = useStore((s) => s.renderResult);

  if (!renderResult || renderResult.paths.length === 0) {
    return (
      <div
        data-testid="data-viewer"
        className="w-full h-full bg-zinc-800 text-zinc-100 text-[11px] p-4"
      >
        No data to display.
      </div>
    );
  }

  return (
    <div
      data-testid="data-viewer"
      className="w-full h-full overflow-auto bg-zinc-800"
    >
      <table className="w-full border-collapse">
        <thead>
          <tr className="bg-zinc-700">
            <th className={`${cellClass} text-zinc-200 font-semibold text-right w-10`} style={headerCellStyle}>#</th>
            <th className={`${cellClass} text-zinc-200 font-semibold text-left`} style={headerCellStyle}>Fill</th>
            <th className={`${cellClass} text-zinc-200 font-semibold text-left`} style={headerCellStyle}>Stroke</th>
            <th className={`${cellClass} text-zinc-200 font-semibold text-right`} style={headerCellStyle}>Contours</th>
            <th className={`${cellClass} text-zinc-200 font-semibold text-right`} style={headerCellStyle}>Points</th>
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
              <td className={`${cellClass} text-zinc-300 text-right`}>
                {i}
              </td>
              <td className={`${cellClass} text-zinc-100 text-left`}>
                {path.fill ? (
                  <span className="inline-flex items-center gap-1">
                    <span
                      className="inline-block w-3 h-3"
                      style={{
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
              <td className={`${cellClass} text-zinc-100 text-left`}>
                {path.stroke ? (
                  <span className="inline-flex items-center gap-1">
                    <span
                      className="inline-block w-3 h-3"
                      style={{
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
              <td className={`${cellClass} text-zinc-100 text-right`}>
                {path.contours.length}
              </td>
              <td className={`${cellClass} text-zinc-100 text-right`}>
                {totalPoints(path)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
