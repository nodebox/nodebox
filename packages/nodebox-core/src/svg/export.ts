import type { Path, Contour, PathPoint } from '../geometry/path.js';
import type { Text } from '../geometry/text.js';
import type { Color } from '../geometry/color.js';

export interface SvgExportOptions {
  width: number;
  height: number;
  precision?: number;
  background?: Color | null;
}

export function exportSvg(paths: Path[], texts: Text[], options: SvgExportOptions): string {
  const { width, height, precision = 2, background = null } = options;
  const lines: string[] = [];

  lines.push(`<?xml version="1.0" encoding="UTF-8"?>`);
  lines.push(`<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">`);

  if (background) {
    lines.push(`  <rect width="${width}" height="${height}" fill="${colorToSvg(background)}"/>`);
  }

  for (const path of paths) {
    const d = pathToSvgData(path, precision);
    if (!d) continue;
    const attrs: string[] = [`d="${d}"`];
    if (path.fill) attrs.push(`fill="${colorToSvg(path.fill)}"`);
    else attrs.push(`fill="none"`);
    if (path.stroke) {
      attrs.push(`stroke="${colorToSvg(path.stroke)}"`);
      attrs.push(`stroke-width="${path.strokeWidth}"`);
    }
    lines.push(`  <path ${attrs.join(' ')}/>`);
  }

  for (const text of texts) {
    const attrs: string[] = [
      `x="${n(text.position.x, precision)}"`,
      `y="${n(text.position.y, precision)}"`,
      `font-family="${text.fontFamily}"`,
      `font-size="${text.fontSize}"`,
      `text-anchor="${textAnchor(text.align)}"`,
    ];
    if (text.fill) attrs.push(`fill="${colorToSvg(text.fill)}"`);
    lines.push(`  <text ${attrs.join(' ')}>${escapeXml(text.text)}</text>`);
  }

  lines.push(`</svg>`);
  return lines.join('\n');
}

function pathToSvgData(path: Path, precision: number): string {
  const parts: string[] = [];
  for (const contour of path.contours) {
    parts.push(contourToSvgData(contour, precision));
  }
  return parts.join(' ');
}

function contourToSvgData(contour: Contour, precision: number): string {
  const parts: string[] = [];
  let i = 0;
  for (const pt of contour.points) {
    if (i === 0) {
      parts.push(`M ${n(pt.point.x, precision)} ${n(pt.point.y, precision)}`);
    } else if (pt.type === 'curveTo') {
      // Already handled by curveData
    } else if (pt.type === 'curveData') {
      // Collect control points and endpoint
      if (i + 2 < contour.points.length) {
        const cp1 = pt;
        const cp2 = contour.points[i + 1];
        const ep = contour.points[i + 2];
        if (cp2.type === 'curveData' && ep.type === 'curveTo') {
          parts.push(`C ${n(cp1.point.x, precision)} ${n(cp1.point.y, precision)} ${n(cp2.point.x, precision)} ${n(cp2.point.y, precision)} ${n(ep.point.x, precision)} ${n(ep.point.y, precision)}`);
        }
      }
    } else if (pt.type === 'quadData') {
      if (i + 1 < contour.points.length) {
        const ep = contour.points[i + 1];
        if (ep.type === 'quadTo') {
          parts.push(`Q ${n(pt.point.x, precision)} ${n(pt.point.y, precision)} ${n(ep.point.x, precision)} ${n(ep.point.y, precision)}`);
        }
      }
    } else if (pt.type === 'quadTo') {
      // Already handled by quadData
    } else {
      parts.push(`L ${n(pt.point.x, precision)} ${n(pt.point.y, precision)}`);
    }
    i++;
  }
  if (contour.closed) parts.push('Z');
  return parts.join(' ');
}

function n(value: number, precision: number): string {
  return value.toFixed(precision);
}

function colorToSvg(color: Color): string {
  const r = Math.round(color.r * 255);
  const g = Math.round(color.g * 255);
  const b = Math.round(color.b * 255);
  if (color.a < 1) {
    return `rgba(${r},${g},${b},${color.a.toFixed(2)})`;
  }
  return `#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}`;
}

function textAnchor(align: string): string {
  if (align === 'center') return 'middle';
  if (align === 'right') return 'end';
  return 'start';
}

function escapeXml(str: string): string {
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}
