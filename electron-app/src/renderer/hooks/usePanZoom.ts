import { useCallback, useRef, useState } from 'react';

export interface PanZoomState {
  panX: number;
  panY: number;
  zoom: number;
}

export interface PanZoomHandlers {
  onWheel: (e: React.WheelEvent<HTMLCanvasElement>) => void;
  onPointerDown: (e: React.PointerEvent<HTMLCanvasElement>) => void;
  onPointerMove: (e: React.PointerEvent<HTMLCanvasElement>) => void;
  onPointerUp: (e: React.PointerEvent<HTMLCanvasElement>) => void;
}

export interface UsePanZoomResult {
  state: PanZoomState;
  handlers: PanZoomHandlers;
  worldToScreen: (wx: number, wy: number) => { x: number; y: number };
  screenToWorld: (sx: number, sy: number) => { x: number; y: number };
  zoomIn: () => void;
  zoomOut: () => void;
  fit: (contentRect: { x: number; y: number; width: number; height: number }, canvasWidth: number, canvasHeight: number) => void;
  setPan: (x: number, y: number) => void;
  setZoom: (z: number) => void;
  isPanning: boolean;
}

const MIN_ZOOM = 0.1;
const MAX_ZOOM = 10.0;
const ZOOM_STEP = 1.1;

export function usePanZoom(
  initialPan = { x: 0, y: 0 },
  initialZoom = 1.0,
  options?: { scrollToZoom?: boolean },
): UsePanZoomResult {
  const scrollToZoom = options?.scrollToZoom ?? false;
  const [pzState, setPzState] = useState<PanZoomState>({
    panX: initialPan.x,
    panY: initialPan.y,
    zoom: initialZoom,
  });
  const [isPanning, setIsPanning] = useState(false);
  const lastPos = useRef({ x: 0, y: 0 });

  const { panX, panY, zoom } = pzState;

  const worldToScreen = useCallback(
    (wx: number, wy: number) => ({
      x: wx * zoom + panX,
      y: wy * zoom + panY,
    }),
    [panX, panY, zoom],
  );

  const screenToWorld = useCallback(
    (sx: number, sy: number) => ({
      x: (sx - panX) / zoom,
      y: (sy - panY) / zoom,
    }),
    [panX, panY, zoom],
  );

  const onWheel = useCallback(
    (e: React.WheelEvent<HTMLCanvasElement>) => {
      e.preventDefault();

      const shouldZoom = scrollToZoom || e.ctrlKey || e.metaKey;

      if (shouldZoom) {
        // Pinch-to-zoom, Ctrl+scroll, or scrollToZoom mode
        const rect = e.currentTarget.getBoundingClientRect();
        const mx = e.clientX - rect.left;
        const my = e.clientY - rect.top;

        const factor = Math.pow(2, -e.deltaY * 0.005);

        setPzState((prev) => {
          const newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, prev.zoom * factor));
          const scale = newZoom / prev.zoom;
          return {
            panX: mx - (mx - prev.panX) * scale,
            panY: my - (my - prev.panY) * scale,
            zoom: newZoom,
          };
        });
      } else {
        // Regular scroll = pan
        setPzState((prev) => ({
          ...prev,
          panX: prev.panX - e.deltaX,
          panY: prev.panY - e.deltaY,
        }));
      }
    },
    [scrollToZoom],
  );

  const onPointerDown = useCallback(
    (e: React.PointerEvent<HTMLCanvasElement>) => {
      if (e.button === 1 || (e.button === 0 && e.altKey)) {
        // Middle mouse or Alt+click = pan
        setIsPanning(true);
        lastPos.current = { x: e.clientX, y: e.clientY };
        e.currentTarget.setPointerCapture(e.pointerId);
        e.preventDefault();
      }
    },
    [],
  );

  const onPointerMove = useCallback(
    (e: React.PointerEvent<HTMLCanvasElement>) => {
      if (!isPanning) return;
      const dx = e.clientX - lastPos.current.x;
      const dy = e.clientY - lastPos.current.y;
      lastPos.current = { x: e.clientX, y: e.clientY };
      setPzState((prev) => ({
        ...prev,
        panX: prev.panX + dx,
        panY: prev.panY + dy,
      }));
    },
    [isPanning],
  );

  const onPointerUp = useCallback(() => {
    setIsPanning(false);
  }, []);

  const zoomIn = useCallback(() => {
    setPzState((prev) => ({
      ...prev,
      zoom: Math.min(MAX_ZOOM, prev.zoom * ZOOM_STEP),
    }));
  }, []);

  const zoomOut = useCallback(() => {
    setPzState((prev) => ({
      ...prev,
      zoom: Math.max(MIN_ZOOM, prev.zoom / ZOOM_STEP),
    }));
  }, []);

  const fit = useCallback(
    (
      contentRect: { x: number; y: number; width: number; height: number },
      canvasWidth: number,
      canvasHeight: number,
    ) => {
      if (contentRect.width <= 0 || contentRect.height <= 0) return;
      const zx = canvasWidth / contentRect.width;
      const zy = canvasHeight / contentRect.height;
      const newZoom = Math.min(zx, zy) * 0.9; // 90% to add padding
      const cx = contentRect.x + contentRect.width / 2;
      const cy = contentRect.y + contentRect.height / 2;
      setPzState({
        panX: canvasWidth / 2 - cx * newZoom,
        panY: canvasHeight / 2 - cy * newZoom,
        zoom: newZoom,
      });
    },
    [],
  );

  const setPan = useCallback((x: number, y: number) => {
    setPzState((prev) => ({ ...prev, panX: x, panY: y }));
  }, []);

  const setZoom = useCallback((z: number) => {
    setPzState((prev) => ({
      ...prev,
      zoom: Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z)),
    }));
  }, []);

  return {
    state: pzState,
    handlers: { onWheel, onPointerDown, onPointerMove, onPointerUp },
    worldToScreen,
    screenToWorld,
    zoomIn,
    zoomOut,
    fit,
    setPan,
    setZoom,
    isPanning,
  };
}
