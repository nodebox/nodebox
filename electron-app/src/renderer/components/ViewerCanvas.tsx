import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { useStore } from '../state/store';
import { usePanZoom } from '../hooks/usePanZoom';
import { useCanvasRenderer } from '../hooks/useCanvasRenderer';
import type { PathRenderData, TextRenderData } from '../types/eval-result';
import type { Contour, Point } from '../types/geometry';
import {
  ZINC_200,
  VIEWER_CROSSHAIR,
  ZINC_500,
  POINT_LINE_TO,
  POINT_CURVE_TO,
  POINT_CURVE_DATA,
  HANDLE_PRIMARY,
} from '../theme/tokens';
import {
  drawFourPointHandle,
  hitTest,
  applyDrag,
  type FourPointDragTarget,
} from '../viewer/four-point-handle';
import { resolveFourPointHandle } from '../viewer/handle-resolver';

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
      case 'quadData': {
        // One quadData followed by one quadTo
        const cp = pts[i];
        const end = pts[i + 1];
        if (end && end.pointType === 'quadTo') {
          path.quadraticCurveTo(cp.x, cp.y, end.x, end.y);
          i += 2;
        } else {
          i++;
        }
        break;
      }
      case 'quadTo':
        // Should be handled by quadData, but fallback
        path.lineTo(pt.x, pt.y);
        i++;
        break;
      default:
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
  const combined = new Path2D();
  for (const contour of pathData.contours) {
    combined.addPath(contourToPath2D(contour));
  }

  if (pathData.fill) {
    ctx.fillStyle = colorToCSS(pathData.fill);
    ctx.fill(combined);
  }
  if (pathData.stroke) {
    ctx.strokeStyle = colorToCSS(pathData.stroke);
    ctx.lineWidth = pathData.strokeWidth;
    ctx.stroke(combined);
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

function pointColor(pointType: string): string {
  switch (pointType) {
    case 'curveTo': return POINT_CURVE_TO;
    case 'curveData': return POINT_CURVE_DATA;
    default: return POINT_LINE_TO; // moveTo, lineTo
  }
}

// DigitCache: pre-renders digits 0-9 using a 5x7 bitmap font at 2x scale.
// Each digit is rendered with a white outline and blue fill on OffscreenCanvas.
const DIGIT_BITMAPS: string[][] = [
  ['01110','10001','10011','10101','11001','10001','01110'], // 0
  ['00100','01100','00100','00100','00100','00100','01110'], // 1
  ['01110','10001','00001','00010','00100','01000','11111'], // 2
  ['11111','00010','00100','00010','00001','10001','01110'], // 3
  ['00010','00110','01010','10010','11111','00010','00010'], // 4
  ['11111','10000','11110','00001','00001','10001','01110'], // 5
  ['00110','01000','10000','11110','10001','10001','01110'], // 6
  ['11111','10001','00010','00100','01000','01000','01000'], // 7
  ['01110','10001','10001','01110','10001','10001','01110'], // 8
  ['01110','10001','10001','01111','00001','00010','01100'], // 9
];

const DIGIT_W = 5;
const DIGIT_H = 7;
const DIGIT_SCALE = 1;
const GLYPH_W = DIGIT_W * DIGIT_SCALE;
const GLYPH_H = DIGIT_H * DIGIT_SCALE;
const FILL_COLOR = POINT_CURVE_DATA; // #6464c8
const OUTLINE_COLOR = '#ffffff';

class DigitCache {
  private glyphs: OffscreenCanvas[] = [];

  constructor() {
    for (let d = 0; d < 10; d++) {
      this.glyphs.push(this.renderGlyph(d));
    }
  }

  private renderGlyph(digit: number): OffscreenCanvas {
    // Extra padding for the outline (1 pixel shift in 8 directions)
    const pad = 2;
    const w = GLYPH_W + pad * 2;
    const h = GLYPH_H + pad * 2;
    const canvas = new OffscreenCanvas(w, h);
    const ctx = canvas.getContext('2d')!;

    const bitmap = DIGIT_BITMAPS[digit];
    // Draw white outline: stamp the glyph shifted in 8 directions
    ctx.fillStyle = OUTLINE_COLOR;
    const offsets = [[-1,-1],[-1,0],[-1,1],[0,-1],[0,1],[1,-1],[1,0],[1,1]];
    for (const [dx, dy] of offsets) {
      for (let row = 0; row < DIGIT_H; row++) {
        for (let col = 0; col < DIGIT_W; col++) {
          if (bitmap[row][col] === '1') {
            ctx.fillRect(
              pad + col * DIGIT_SCALE + dx,
              pad + row * DIGIT_SCALE + dy,
              DIGIT_SCALE,
              DIGIT_SCALE,
            );
          }
        }
      }
    }

    // Draw fill on top
    ctx.fillStyle = FILL_COLOR;
    for (let row = 0; row < DIGIT_H; row++) {
      for (let col = 0; col < DIGIT_W; col++) {
        if (bitmap[row][col] === '1') {
          ctx.fillRect(
            pad + col * DIGIT_SCALE,
            pad + row * DIGIT_SCALE,
            DIGIT_SCALE,
            DIGIT_SCALE,
          );
        }
      }
    }

    return canvas;
  }

  drawNumber(ctx: CanvasRenderingContext2D, num: number, x: number, y: number) {
    const digits = String(num);
    const spacing = GLYPH_W + 1;
    for (let i = 0; i < digits.length; i++) {
      const d = parseInt(digits[i], 10);
      ctx.drawImage(this.glyphs[d], x + i * spacing, y);
    }
  }
}

let digitCacheInstance: DigitCache | null = null;
function getDigitCache(): DigitCache {
  if (!digitCacheInstance) {
    digitCacheInstance = new DigitCache();
  }
  return digitCacheInstance;
}

export function ViewerCanvas() {
  const renderResult = useStore((s) => s.renderResult);
  const showOrigin = useStore((s) => s.showOrigin);
  const showCanvasBorder = useStore((s) => s.showCanvasBorder);
  const showHandles = useStore((s) => s.showHandles);
  const showPoints = useStore((s) => s.showPoints);
  const showPointNumbers = useStore((s) => s.showPointNumbers);
  const library = useStore((s) => s.library);
  const setViewerZoom = useStore((s) => s.setViewerZoom);
  const viewerZoomAction = useStore((s) => s.viewerZoomAction);
  const clearViewerZoomAction = useStore((s) => s.clearViewerZoomAction);
  const setPortValue = useStore((s) => s.setPortValue);
  const pushSnapshot = useStore((s) => s.pushSnapshot);

  const [handleDragTarget, setHandleDragTarget] = useState<FourPointDragTarget>('none');
  const handleDragStartWorld = useRef<Point>({ x: 0, y: 0 });
  const handleDragStartValues = useRef({ center: { x: 0, y: 0 }, width: 100, height: 100 });

  const fourPointHandle = useMemo(
    () => resolveFourPointHandle(library.root.renderedChild, library.root.children),
    [library],
  );

  const panZoom = usePanZoom(undefined, undefined, { scrollToZoom: true, centerOrigin: true });
  const { state: pz, handlers } = panZoom;
  const { zoomIn, zoomOut, setPan, setZoom } = panZoom;

  // Sync viewer zoom to store for header display
  useEffect(() => {
    setViewerZoom(pz.zoom);
  }, [pz.zoom, setViewerZoom]);

  // Handle zoom actions from header controls
  useEffect(() => {
    if (!viewerZoomAction) return;
    if (viewerZoomAction === 'in') zoomIn();
    else if (viewerZoomAction === 'out') zoomOut();
    else if (viewerZoomAction === 'reset') { setPan(0, 0); setZoom(1); }
    clearViewerZoomAction();
  }, [viewerZoomAction, clearViewerZoomAction, zoomIn, zoomOut, setPan, setZoom]);

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

      // Draw points
      if (showPoints) {
        ctx.save();
        ctx.translate(width / 2 + pz.panX, height / 2 + pz.panY);
        ctx.scale(pz.zoom, pz.zoom);

        if (renderResult) {
          for (const pathData of renderResult.paths) {
            for (const contour of pathData.contours) {
              const r = 3 / pz.zoom;
              for (const pt of contour.points) {
                ctx.fillStyle = pointColor(pt.pointType);
                ctx.beginPath();
                ctx.arc(pt.x, pt.y, r, 0, Math.PI * 2);
                ctx.fill();
              }
            }
          }
        }

        ctx.restore();
      }

      // Draw four-point handle (in screen space)
      if (showHandles && fourPointHandle) {
        const wts = (wx: number, wy: number) => ({
          x: width / 2 + pz.panX + wx * pz.zoom,
          y: height / 2 + pz.panY + wy * pz.zoom,
        });
        drawFourPointHandle(ctx, fourPointHandle, HANDLE_PRIMARY, wts);
      }

      // Draw point numbers (in screen space, after restoring from world transform)
      if (showPointNumbers && renderResult) {
        const cache = getDigitCache();
        const centerX = width / 2 + pz.panX;
        const centerY = height / 2 + pz.panY;
        let pointIndex = 0;

        for (const pathData of renderResult.paths) {
          for (const contour of pathData.contours) {
            for (const pt of contour.points) {
              const screenX = centerX + pt.x * pz.zoom;
              const screenY = centerY + pt.y * pz.zoom;
              cache.drawNumber(ctx, pointIndex, screenX + 6, screenY - 12);
              pointIndex++;
            }
          }
        }
      }

      // Origin crosshair (drawn last so it's visible above geometry)
      if (showOrigin) {
        drawOrigin(ctx, width, height, pz.panX, pz.panY);
      }
    },
    [pz, renderResult, showOrigin, showCanvasBorder, showHandles, showPoints, showPointNumbers, docWidth, docHeight, canvasBg, fourPointHandle],
  );

  const { canvasRef } = useCanvasRenderer(draw);

  // Convert pointer event to world coordinates (center-origin)
  const pointerToWorld = useCallback(
    (e: React.PointerEvent<HTMLCanvasElement>) => {
      const rect = e.currentTarget.getBoundingClientRect();
      const sx = e.clientX - rect.left;
      const sy = e.clientY - rect.top;
      return {
        sx,
        sy,
        canvasW: rect.width,
        canvasH: rect.height,
        worldX: (sx - rect.width / 2 - pz.panX) / pz.zoom,
        worldY: (sy - rect.height / 2 - pz.panY) / pz.zoom,
      };
    },
    [pz],
  );

  const handlePointerDown = useCallback(
    (e: React.PointerEvent<HTMLCanvasElement>) => {
      // Only intercept left click, no Alt (Alt is for pan)
      if (e.button === 0 && !e.altKey && showHandles && fourPointHandle) {
        const { sx, sy, canvasW, canvasH, worldX, worldY } = pointerToWorld(e);

        const wts = (wx: number, wy: number) => ({
          x: canvasW / 2 + pz.panX + wx * pz.zoom,
          y: canvasH / 2 + pz.panY + wy * pz.zoom,
        });
        const target = hitTest(fourPointHandle, sx, sy, wts);

        if (target !== 'none') {
          pushSnapshot(library);
          setHandleDragTarget(target);
          handleDragStartWorld.current = { x: worldX, y: worldY };
          handleDragStartValues.current = {
            center: { ...fourPointHandle.center },
            width: fourPointHandle.width,
            height: fourPointHandle.height,
          };
          e.currentTarget.setPointerCapture(e.pointerId);
          e.preventDefault();
          return;
        }
      }
      handlers.onPointerDown(e);
    },
    [showHandles, fourPointHandle, pz, handlers, pushSnapshot, library, pointerToWorld],
  );

  const handlePointerMove = useCallback(
    (e: React.PointerEvent<HTMLCanvasElement>) => {
      if (handleDragTarget !== 'none' && fourPointHandle) {
        const { worldX, worldY } = pointerToWorld(e);
        const dx = worldX - handleDragStartWorld.current.x;
        const dy = worldY - handleDragStartWorld.current.y;
        const result = applyDrag(handleDragTarget, handleDragStartValues.current, dx, dy);

        setPortValue(fourPointHandle.nodeName, 'position', {
          type: 'point',
          value: result.center,
        });
        setPortValue(fourPointHandle.nodeName, 'width', {
          type: 'float',
          value: result.width,
        });
        setPortValue(fourPointHandle.nodeName, 'height', {
          type: 'float',
          value: result.height,
        });
        return;
      }
      handlers.onPointerMove(e);
    },
    [handleDragTarget, fourPointHandle, handlers, setPortValue, pointerToWorld],
  );

  const handlePointerUp = useCallback(
    (e: React.PointerEvent<HTMLCanvasElement>) => {
      if (handleDragTarget !== 'none') {
        setHandleDragTarget('none');
        return;
      }
      handlers.onPointerUp(e);
    },
    [handleDragTarget, handlers],
  );

  const cursor = handleDragTarget !== 'none' || panZoom.isPanning ? 'grabbing' : 'default';

  return (
    <canvas
      ref={canvasRef}
      className="w-full h-full block"
      style={{ cursor }}
      onWheel={handlers.onWheel}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
    />
  );
}
