import { useCallback, useEffect } from 'react';
import { useStore } from '../state/store';
import { usePanZoom } from '../hooks/usePanZoom';
import { useCanvasRenderer } from '../hooks/useCanvasRenderer';
import type { PathRenderData, TextRenderData } from '../types/eval-result';
import type { Contour } from '../types/geometry';
import {
  ZINC_200,
  VIEWER_CROSSHAIR,
  ZINC_500,
} from '../theme/tokens';

function colorToCSS(c: { r: number; g: number; b: number; a: number }): string {
  return `rgba(${Math.round(c.r * 255)}, ${Math.round(c.g * 255)}, ${Math.round(c.b * 255)}, ${c.a})`;
}

function contourToPath2D(contour: Contour): Path2D {
  const path = new Path2D();
  const pts = contour.points;
  let i = 0;
  while (i < pts.length) {
    const pt = pts[i];
    switch (pt.pointType) {
      case 'moveTo':
        path.moveTo(pt.x, pt.y);
        i++;
        break;
      case 'lineTo':
        path.lineTo(pt.x, pt.y);
        i++;
        break;
      case 'curveData': {
        // Expect two curveData followed by one curveTo
        const cp1 = pts[i];
        const cp2 = pts[i + 1];
        const end = pts[i + 2];
        if (cp2 && end && end.pointType === 'curveTo') {
          path.bezierCurveTo(cp1.x, cp1.y, cp2.x, cp2.y, end.x, end.y);
          i += 3;
        } else {
          i++;
        }
        break;
      }
      case 'curveTo':
        // Should be handled by curveData, but fallback
        path.lineTo(pt.x, pt.y);
        i++;
        break;
    }
  }
  if (contour.closed) {
    path.closePath();
  }
  return path;
}

function drawPathData(
  ctx: CanvasRenderingContext2D,
  pathData: PathRenderData,
) {
  for (const contour of pathData.contours) {
    const path2d = contourToPath2D(contour);

    if (pathData.fill) {
      ctx.fillStyle = colorToCSS(pathData.fill);
      ctx.fill(path2d);
    }
    if (pathData.stroke) {
      ctx.strokeStyle = colorToCSS(pathData.stroke);
      ctx.lineWidth = pathData.strokeWidth;
      ctx.stroke(path2d);
    }
  }
}

function drawTextData(
  ctx: CanvasRenderingContext2D,
  textData: TextRenderData,
) {
  ctx.font = `${textData.fontSize}px "${textData.fontFamily}", sans-serif`;
  ctx.textAlign = textData.align;
  ctx.textBaseline = 'alphabetic';
  if (textData.fill) {
    ctx.fillStyle = colorToCSS(textData.fill);
  } else {
    ctx.fillStyle = '#000000';
  }
  ctx.fillText(textData.text, textData.position.x, textData.position.y);
}

function drawOrigin(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  panX: number,
  panY: number,
) {
  const cx = width / 2 + panX;
  const cy = height / 2 + panY;
  const arm = 20;

  ctx.strokeStyle = VIEWER_CROSSHAIR;
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.moveTo(cx - arm, cy + 0.5);
  ctx.lineTo(cx + arm, cy + 0.5);
  ctx.moveTo(cx + 0.5, cy - arm);
  ctx.lineTo(cx + 0.5, cy + arm);
  ctx.stroke();
}

function drawCanvasBorder(
  ctx: CanvasRenderingContext2D,
  canvasWidth: number,
  canvasHeight: number,
  docWidth: number,
  docHeight: number,
  panX: number,
  panY: number,
  zoom: number,
) {
  const cx = canvasWidth / 2 + panX;
  const cy = canvasHeight / 2 + panY;
  const halfW = (docWidth / 2) * zoom;
  const halfH = (docHeight / 2) * zoom;

  ctx.strokeStyle = ZINC_500;
  ctx.lineWidth = 1;
  ctx.strokeRect(cx - halfW, cy - halfH, halfW * 2, halfH * 2);
}

export function ViewerCanvas() {
  const renderResult = useStore((s) => s.renderResult);
  const showOrigin = useStore((s) => s.showOrigin);
  const showCanvasBorder = useStore((s) => s.showCanvasBorder);
  const showHandles = useStore((s) => s.showHandles);
  const showPoints = useStore((s) => s.showPoints);
  const library = useStore((s) => s.library);
  const setViewerZoom = useStore((s) => s.setViewerZoom);

  const panZoom = usePanZoom(undefined, undefined, { scrollToZoom: true });
  const { state: pz, handlers } = panZoom;

  // Sync viewer zoom to store for header display
  useEffect(() => {
    setViewerZoom(pz.zoom);
  }, [pz.zoom, setViewerZoom]);

  const docWidth = parseFloat(library.properties.canvasWidth ?? '1000');
  const docHeight = parseFloat(library.properties.canvasHeight ?? '1000');
  const canvasBg = library.properties.canvasBackground ?? ZINC_200;

  const draw = useCallback(
    (ctx: CanvasRenderingContext2D, width: number, height: number) => {
      // Clear with light document background
      ctx.fillStyle = canvasBg;
      ctx.fillRect(0, 0, width, height);

      // Canvas border
      if (showCanvasBorder) {
        drawCanvasBorder(ctx, width, height, docWidth, docHeight, pz.panX, pz.panY, pz.zoom);
      }

      // Origin crosshair
      if (showOrigin) {
        drawOrigin(ctx, width, height, pz.panX, pz.panY);
      }

      // Render paths and texts
      if (renderResult) {
        ctx.save();
        ctx.translate(width / 2 + pz.panX, height / 2 + pz.panY);
        ctx.scale(pz.zoom, pz.zoom);

        for (const pathData of renderResult.paths) {
          drawPathData(ctx, pathData);
        }
        for (const textData of renderResult.texts) {
          drawTextData(ctx, textData);
        }

        ctx.restore();
      }

      // Draw handles and points
      if (showHandles || showPoints) {
        ctx.save();
        ctx.translate(width / 2 + pz.panX, height / 2 + pz.panY);
        ctx.scale(pz.zoom, pz.zoom);

        if (renderResult) {
          for (const pathData of renderResult.paths) {
            for (const contour of pathData.contours) {
              const pts = contour.points;

              if (showHandles) {
                // Draw handle lines from curveData to their curveTo
                ctx.strokeStyle = VIEWER_CROSSHAIR;
                ctx.lineWidth = 1 / pz.zoom;
                let j = 0;
                while (j < pts.length) {
                  if (pts[j].pointType === 'curveData' && j + 2 < pts.length && pts[j + 2].pointType === 'curveTo') {
                    const cp1 = pts[j];
                    const cp2 = pts[j + 1];
                    const end = pts[j + 2];
                    // Find the previous point for cp1 line
                    if (j > 0) {
                      const prev = pts[j - 1];
                      ctx.beginPath();
                      ctx.moveTo(prev.x, prev.y);
                      ctx.lineTo(cp1.x, cp1.y);
                      ctx.stroke();
                    }
                    // Line from cp2 to curveTo
                    ctx.beginPath();
                    ctx.moveTo(cp2.x, cp2.y);
                    ctx.lineTo(end.x, end.y);
                    ctx.stroke();
                    // Small circles at control points
                    const r = 3 / pz.zoom;
                    ctx.beginPath();
                    ctx.arc(cp1.x, cp1.y, r, 0, Math.PI * 2);
                    ctx.stroke();
                    ctx.beginPath();
                    ctx.arc(cp2.x, cp2.y, r, 0, Math.PI * 2);
                    ctx.stroke();
                    j += 3;
                  } else {
                    j++;
                  }
                }
              }

              if (showPoints) {
                const sz = 4 / pz.zoom;
                const half = sz / 2;
                for (const pt of pts) {
                  if (pt.pointType === 'curveData') continue;
                  ctx.strokeStyle = VIEWER_CROSSHAIR;
                  ctx.lineWidth = 1 / pz.zoom;
                  ctx.strokeRect(pt.x - half, pt.y - half, sz, sz);
                }
              }
            }
          }
        }

        ctx.restore();
      }
    },
    [pz, renderResult, showOrigin, showCanvasBorder, showHandles, showPoints, docWidth, docHeight, canvasBg],
  );

  const { canvasRef } = useCanvasRenderer(draw);

  return (
    <canvas
      ref={canvasRef}
      className="w-full h-full block"
      style={{ cursor: panZoom.isPanning ? 'grabbing' : 'default' }}
      onWheel={handlers.onWheel}
      onPointerDown={handlers.onPointerDown}
      onPointerMove={handlers.onPointerMove}
      onPointerUp={handlers.onPointerUp}
    />
  );
}
