import React, { useRef, useEffect, useCallback } from 'react';
import { useStore } from '../state/store.js';
import { renderPaths } from '../viewer/canvas-renderer.js';

export function ViewerCanvas() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const paths = useStore((s) => s.paths);
  const showPoints = useStore((s) => s.showPoints);
  const showOrigin = useStore((s) => s.showOrigin);

  const draw = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    ctx.scale(dpr, dpr);

    // Clear
    ctx.fillStyle = '#fafafa';
    ctx.fillRect(0, 0, rect.width, rect.height);

    // Center transform
    ctx.save();
    ctx.translate(rect.width / 2, rect.height / 2);

    // Origin crosshair
    if (showOrigin) {
      ctx.strokeStyle = '#ccc';
      ctx.lineWidth = 0.5;
      ctx.beginPath();
      ctx.moveTo(-20, 0);
      ctx.lineTo(20, 0);
      ctx.moveTo(0, -20);
      ctx.lineTo(0, 20);
      ctx.stroke();
    }

    // Render paths
    renderPaths(ctx, paths, showPoints);

    ctx.restore();
  }, [paths, showPoints, showOrigin]);

  useEffect(() => {
    draw();
    const handleResize = () => draw();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [draw]);

  return (
    <canvas
      ref={canvasRef}
      style={{ width: '100%', height: '100%', display: 'block' }}
    />
  );
}
