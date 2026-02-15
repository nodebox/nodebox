import { useCallback, useEffect, useRef } from 'react';

export function useCanvasRenderer(
  draw: (ctx: CanvasRenderingContext2D, width: number, height: number) => void,
) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const rafRef = useRef<number>(0);

  const render = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    const width = rect.width;
    const height = rect.height;

    // Resize canvas buffer to match display size at device pixel ratio
    if (canvas.width !== width * dpr || canvas.height !== height * dpr) {
      canvas.width = width * dpr;
      canvas.height = height * dpr;
    }

    ctx.save();
    ctx.scale(dpr, dpr);
    draw(ctx, width, height);
    ctx.restore();
  }, [draw]);

  const requestRender = useCallback(() => {
    cancelAnimationFrame(rafRef.current);
    rafRef.current = requestAnimationFrame(render);
  }, [render]);

  useEffect(() => {
    requestRender();
    return () => cancelAnimationFrame(rafRef.current);
  }, [requestRender]);

  return { canvasRef, requestRender };
}
