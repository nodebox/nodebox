import React, { useCallback, useEffect } from 'react';
import { useStore } from '../state/store';
import { usePanZoom } from '../hooks/usePanZoom';
import { useCanvasRenderer } from '../hooks/useCanvasRenderer';
import { renderPaths } from '../viewer/canvas-renderer';
import {
  ZINC_200,
  ZINC_500,
  VIEWER_CROSSHAIR,
  POINT_LINE_TO,
  POINT_CURVE_TO,
  POINT_CURVE_DATA,
} from '../theme/tokens';
import type { Path } from 'nodebox-core';

function pointColor(pointType: string): string {
  switch (pointType) {
    case 'curveTo': return POINT_CURVE_TO;
    case 'curveData': return POINT_CURVE_DATA;
    case 'quadTo': return POINT_CURVE_TO;
    case 'quadData': return POINT_CURVE_DATA;
    default: return POINT_LINE_TO;
  }
}

export function ViewerCanvas() {
  const paths = useStore((s) => s.paths);
  const showOrigin = useStore((s) => s.showOrigin);
  const showCanvasBorder = useStore((s) => s.showCanvasBorder);
  const showPoints = useStore((s) => s.showPoints);
  const showPointNumbers = useStore((s) => s.showPointNumbers);
  const setViewerZoom = useStore((s) => s.setViewerZoom);
  const viewerZoomAction = useStore((s) => s.viewerZoomAction);
  const clearViewerZoomAction = useStore((s) => s.clearViewerZoomAction);
  const library = useStore((s) => s.library);

  const panZoom = usePanZoom(undefined, undefined, { scrollToZoom: true, centerOrigin: true });
  const { state: pz, handlers } = panZoom;
  const { zoomIn, zoomOut, setPan, setZoom } = panZoom;

  useEffect(() => { setViewerZoom(pz.zoom); }, [pz.zoom, setViewerZoom]);

  useEffect(() => {
    if (!viewerZoomAction) return;
    if (viewerZoomAction === 'in') zoomIn();
    else if (viewerZoomAction === 'out') zoomOut();
    else if (viewerZoomAction === 'reset') { setPan(0, 0); setZoom(1); }
    clearViewerZoomAction();
  }, [viewerZoomAction, clearViewerZoomAction, zoomIn, zoomOut, setPan, setZoom]);

  const docWidth = library ? parseFloat(library.properties.canvasWidth ?? '1000') : 1000;
  const docHeight = library ? parseFloat(library.properties.canvasHeight ?? '1000') : 1000;

  const draw = useCallback(
    (ctx: CanvasRenderingContext2D, width: number, height: number) => {
      ctx.fillStyle = ZINC_200;
      ctx.fillRect(0, 0, width, height);

      // Canvas border
      if (showCanvasBorder) {
        const cx = width / 2 + pz.panX;
        const cy = height / 2 + pz.panY;
        const halfW = (docWidth / 2) * pz.zoom;
        const halfH = (docHeight / 2) * pz.zoom;
        ctx.strokeStyle = ZINC_500;
        ctx.lineWidth = 1;
        ctx.strokeRect(cx - halfW, cy - halfH, halfW * 2, halfH * 2);
      }

      // Render geometry
      if (paths.length > 0) {
        ctx.save();
        ctx.translate(width / 2 + pz.panX, height / 2 + pz.panY);
        ctx.scale(pz.zoom, pz.zoom);
        renderPaths(ctx, paths, false);
        ctx.restore();
      }

      // Draw points
      if (showPoints && paths.length > 0) {
        ctx.save();
        ctx.translate(width / 2 + pz.panX, height / 2 + pz.panY);
        ctx.scale(pz.zoom, pz.zoom);
        for (const path of paths) {
          for (const contour of path.contours) {
            const r = 3 / pz.zoom;
            for (const pt of contour.points) {
              ctx.fillStyle = pointColor(pt.type);
              ctx.beginPath();
              ctx.arc(pt.point.x, pt.point.y, r, 0, Math.PI * 2);
              ctx.fill();
            }
          }
        }
        ctx.restore();
      }

      // Draw point numbers
      if (showPointNumbers && paths.length > 0) {
        const centerX = width / 2 + pz.panX;
        const centerY = height / 2 + pz.panY;
        let idx = 0;
        ctx.font = '9px monospace';
        ctx.textAlign = 'left';
        ctx.textBaseline = 'bottom';
        for (const path of paths) {
          for (const contour of path.contours) {
            for (const pt of contour.points) {
              const sx = centerX + pt.point.x * pz.zoom;
              const sy = centerY + pt.point.y * pz.zoom;
              ctx.fillStyle = '#fff';
              ctx.strokeStyle = POINT_CURVE_DATA;
              ctx.lineWidth = 2;
              ctx.strokeText(String(idx), sx + 5, sy - 3);
              ctx.fillText(String(idx), sx + 5, sy - 3);
              idx++;
            }
          }
        }
      }

      // Origin crosshair
      if (showOrigin) {
        const cx = width / 2 + pz.panX;
        const cy = height / 2 + pz.panY;
        ctx.strokeStyle = VIEWER_CROSSHAIR;
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(cx - 20, cy + 0.5);
        ctx.lineTo(cx + 20, cy + 0.5);
        ctx.moveTo(cx + 0.5, cy - 20);
        ctx.lineTo(cx + 0.5, cy + 20);
        ctx.stroke();
      }
    },
    [pz, paths, showOrigin, showCanvasBorder, showPoints, showPointNumbers, docWidth, docHeight],
  );

  const { canvasRef } = useCanvasRenderer(draw);

  const cursor = panZoom.isPanning ? 'grabbing' : panZoom.isSpaceDown ? 'grab' : 'default';

  return (
    <canvas
      ref={canvasRef}
      className="w-full h-full block"
      style={{ cursor }}
      onWheel={handlers.onWheel}
      onPointerDown={handlers.onPointerDown}
      onPointerMove={handlers.onPointerMove}
      onPointerUp={handlers.onPointerUp}
    />
  );
}
