import type { Path, Contour, PathPoint } from 'nodebox-core';
import type { Text } from 'nodebox-core';

export function renderPaths(
  ctx: CanvasRenderingContext2D,
  paths: Path[],
  showPoints = false,
): void {
  for (const path of paths) {
    const path2d = pathToPath2D(path);

    if (path.fill) {
      ctx.fillStyle = colorToCSS(path.fill);
      ctx.fill(path2d);
    }

    if (path.stroke) {
      ctx.strokeStyle = colorToCSS(path.stroke);
      ctx.lineWidth = path.strokeWidth;
      ctx.stroke(path2d);
    }

    if (showPoints) {
      drawPoints(ctx, path);
    }
  }
}

export function renderTexts(ctx: CanvasRenderingContext2D, texts: Text[]): void {
  for (const text of texts) {
    ctx.font = `${text.fontSize}px ${text.fontFamily}`;
    ctx.textAlign = text.align;
    if (text.fill) {
      ctx.fillStyle = colorToCSS(text.fill);
      ctx.fillText(text.text, text.position.x, text.position.y);
    }
  }
}

function pathToPath2D(path: Path): Path2D {
  const p = new Path2D();
  for (const contour of path.contours) {
    contourToPath2D(p, contour);
  }
  return p;
}

export function contourToPath2D(p: Path2D, contour: Contour): void {
  const pts = contour.points;
  if (pts.length === 0) return;

  p.moveTo(pts[0].point.x, pts[0].point.y);
  let i = 1;
  while (i < pts.length) {
    const pt = pts[i];
    if (pt.type === 'curveData' && i + 2 < pts.length) {
      const cp1 = pts[i];
      const cp2 = pts[i + 1];
      const ep = pts[i + 2];
      p.bezierCurveTo(
        cp1.point.x, cp1.point.y,
        cp2.point.x, cp2.point.y,
        ep.point.x, ep.point.y,
      );
      i += 3;
    } else if (pt.type === 'quadData' && i + 1 < pts.length) {
      const ep = pts[i + 1];
      p.quadraticCurveTo(pt.point.x, pt.point.y, ep.point.x, ep.point.y);
      i += 2;
    } else if (pt.type === 'lineTo') {
      p.lineTo(pt.point.x, pt.point.y);
      i++;
    } else {
      i++;
    }
  }
  if (contour.closed) p.closePath();
}

function drawPoints(ctx: CanvasRenderingContext2D, path: Path): void {
  ctx.fillStyle = '#4a90d9';
  for (const contour of path.contours) {
    for (const pt of contour.points) {
      if (pt.type === 'curveData' || pt.type === 'quadData') continue;
      ctx.beginPath();
      ctx.arc(pt.point.x, pt.point.y, 3, 0, Math.PI * 2);
      ctx.fill();
    }
  }
}

function colorToCSS(color: { r: number; g: number; b: number; a: number }): string {
  const r = Math.round(color.r * 255);
  const g = Math.round(color.g * 255);
  const b = Math.round(color.b * 255);
  if (color.a < 1) {
    return `rgba(${r},${g},${b},${color.a})`;
  }
  return `rgb(${r},${g},${b})`;
}
