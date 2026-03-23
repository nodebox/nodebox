import { useCallback, useEffect, useRef, useState } from 'react';

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

const MIN_ZOOM = 0.1;
const MAX_ZOOM = 10.0;
const ZOOM_STEP = 1.1;

export function usePanZoom(
  initialPan = { x: 0, y: 0 },
  initialZoom = 1.0,
  options?: { scrollToZoom?: boolean; centerOrigin?: boolean },
) {
  const scrollToZoom = options?.scrollToZoom ?? false;
  const centerOrigin = options?.centerOrigin ?? false;

  const [pzState, setPzState] = useState<PanZoomState>({
    panX: initialPan.x,
    panY: initialPan.y,
    zoom: initialZoom,
  });
  const [isPanning, setIsPanning] = useState(false);
  const [isSpaceDown, setIsSpaceDown] = useState(false);
  const lastPos = useRef({ x: 0, y: 0 });

  useEffect(() => {
    const onKeyDown = (e: KeyboardEvent) => {
      if (e.code === 'Space' && !e.repeat) {
        const tag = (e.target as HTMLElement)?.tagName;
        if (tag === 'INPUT' || tag === 'TEXTAREA') return;
        e.preventDefault();
        setIsSpaceDown(true);
      }
    };
    const onKeyUp = (e: KeyboardEvent) => {
      if (e.code === 'Space') setIsSpaceDown(false);
    };
    window.addEventListener('keydown', onKeyDown);
    window.addEventListener('keyup', onKeyUp);
    return () => {
      window.removeEventListener('keydown', onKeyDown);
      window.removeEventListener('keyup', onKeyUp);
    };
  }, []);

  const { panX, panY, zoom } = pzState;

  const worldToScreen = useCallback(
    (wx: number, wy: number) => ({ x: wx * zoom + panX, y: wy * zoom + panY }),
    [panX, panY, zoom],
  );

  const screenToWorld = useCallback(
    (sx: number, sy: number) => ({ x: (sx - panX) / zoom, y: (sy - panY) / zoom }),
    [panX, panY, zoom],
  );

  const onWheel = useCallback(
    (e: React.WheelEvent<HTMLCanvasElement>) => {
      e.preventDefault();
      const shouldZoom = scrollToZoom || e.ctrlKey || e.metaKey;
      if (shouldZoom) {
        const rect = e.currentTarget.getBoundingClientRect();
        const mx = e.clientX - rect.left;
        const my = e.clientY - rect.top;
        const factor = Math.pow(2, -e.deltaY * 0.005);
        const ax = centerOrigin ? mx - rect.width / 2 : mx;
        const ay = centerOrigin ? my - rect.height / 2 : my;
        setPzState((prev) => {
          const newZoom = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, prev.zoom * factor));
          const scale = newZoom / prev.zoom;
          return { panX: ax - (ax - prev.panX) * scale, panY: ay - (ay - prev.panY) * scale, zoom: newZoom };
        });
      } else {
        setPzState((prev) => ({ ...prev, panX: prev.panX - e.deltaX, panY: prev.panY - e.deltaY }));
      }
    },
    [scrollToZoom, centerOrigin],
  );

  const spaceRef = useRef(isSpaceDown);
  spaceRef.current = isSpaceDown;

  const onPointerDown = useCallback((e: React.PointerEvent<HTMLCanvasElement>) => {
    if (e.button === 1 || (e.button === 0 && (e.altKey || spaceRef.current))) {
      setIsPanning(true);
      lastPos.current = { x: e.clientX, y: e.clientY };
      e.currentTarget.setPointerCapture(e.pointerId);
      e.preventDefault();
    }
  }, []);

  const onPointerMove = useCallback((e: React.PointerEvent<HTMLCanvasElement>) => {
    if (!isPanning) return;
    const dx = e.clientX - lastPos.current.x;
    const dy = e.clientY - lastPos.current.y;
    lastPos.current = { x: e.clientX, y: e.clientY };
    setPzState((prev) => ({ ...prev, panX: prev.panX + dx, panY: prev.panY + dy }));
  }, [isPanning]);

  const onPointerUp = useCallback(() => setIsPanning(false), []);

  const zoomIn = useCallback(() => setPzState((p) => ({ ...p, zoom: Math.min(MAX_ZOOM, p.zoom * ZOOM_STEP) })), []);
  const zoomOut = useCallback(() => setPzState((p) => ({ ...p, zoom: Math.max(MIN_ZOOM, p.zoom / ZOOM_STEP) })), []);
  const setPan = useCallback((x: number, y: number) => setPzState((p) => ({ ...p, panX: x, panY: y })), []);
  const setZoom = useCallback((z: number) => setPzState((p) => ({ ...p, zoom: Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, z)) })), []);

  return {
    state: pzState,
    handlers: { onWheel, onPointerDown, onPointerMove, onPointerUp } as PanZoomHandlers,
    worldToScreen, screenToWorld, zoomIn, zoomOut, setPan, setZoom, isPanning, isSpaceDown,
  };
}
