import { useCallback } from 'react';
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
  for (const pt of contour.points) {
    switch (pt.pointType) {
      case 'moveTo':
        path.moveTo(pt.x, pt.y);
        break;
      case 'lineTo':
        path.lineTo(pt.x, pt.y);
        break;
      case 'curveTo':
        path.lineTo(pt.x, pt.y);
        break;
      case 'curveData':
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
  ctx.setLineDash([4, 4]);
  ctx.strokeRect(cx - halfW, cy - halfH, halfW * 2, halfH * 2);
  ctx.setLineDash([]);
}

export function ViewerCanvas() {
  const renderResult = useStore((s) => s.renderResult);
  const showOrigin = useStore((s) => s.showOrigin);
  const showCanvasBorder = useStore((s) => s.showCanvasBorder);
  const library = useStore((s) => s.library);

  const panZoom = usePanZoom();
  const { state: pz, handlers } = panZoom;

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
    },
    [pz, renderResult, showOrigin, showCanvasBorder, docWidth, docHeight, canvasBg],
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
