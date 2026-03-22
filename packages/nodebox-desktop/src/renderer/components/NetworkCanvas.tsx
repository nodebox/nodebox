import React, { useRef, useEffect, useCallback } from 'react';
import { useStore } from '../state/store.js';

const NODE_WIDTH = 120;
const NODE_HEIGHT = 30;
const GRID_SIZE = 10;

export function NetworkCanvas() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const library = useStore((s) => s.library);
  const currentNetworkPath = useStore((s) => s.currentNetworkPath);
  const selectedNodes = useStore((s) => s.selectedNodes);

  const draw = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas || !library) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const dpr = window.devicePixelRatio || 1;
    const rect = canvas.getBoundingClientRect();
    canvas.width = rect.width * dpr;
    canvas.height = rect.height * dpr;
    ctx.scale(dpr, dpr);

    ctx.fillStyle = '#1e1e1e';
    ctx.fillRect(0, 0, rect.width, rect.height);

    // Find current network
    const network = findNodeByPath(library.root, currentNetworkPath);
    if (!network) return;

    ctx.save();
    ctx.translate(rect.width / 2, rect.height / 2);

    // Draw connections
    ctx.strokeStyle = '#555';
    ctx.lineWidth = 1;
    for (const conn of network.connections) {
      const from = network.children.find(c => c.name === conn.outputNode);
      const to = network.children.find(c => c.name === conn.inputNode);
      if (from && to) {
        const fx = from.position.x * GRID_SIZE * 10 + NODE_WIDTH / 2;
        const fy = from.position.y * GRID_SIZE * 10 + NODE_HEIGHT;
        const tx = to.position.x * GRID_SIZE * 10 + NODE_WIDTH / 2;
        const ty = to.position.y * GRID_SIZE * 10;
        ctx.beginPath();
        ctx.moveTo(fx, fy);
        ctx.lineTo(tx, ty);
        ctx.stroke();
      }
    }

    // Draw nodes
    for (const child of network.children) {
      const x = child.position.x * GRID_SIZE * 10;
      const y = child.position.y * GRID_SIZE * 10;
      const isSelected = selectedNodes.includes(child.name);
      const isRendered = network.renderedChild === child.name;

      ctx.fillStyle = isSelected ? '#4a90d9' : isRendered ? '#2a6030' : '#333';
      ctx.fillRect(x, y, NODE_WIDTH, NODE_HEIGHT);

      ctx.strokeStyle = isSelected ? '#6bb0ff' : '#555';
      ctx.lineWidth = 1;
      ctx.strokeRect(x, y, NODE_WIDTH, NODE_HEIGHT);

      ctx.fillStyle = '#eee';
      ctx.font = '11px sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(child.name, x + NODE_WIDTH / 2, y + NODE_HEIGHT / 2);
    }

    ctx.restore();
  }, [library, currentNetworkPath, selectedNodes]);

  useEffect(() => {
    draw();
    const handleResize = () => draw();
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [draw]);

  return (
    <canvas
      ref={canvasRef}
      style={{ width: '100%', height: '100%', display: 'block', background: '#1e1e1e' }}
    />
  );
}

function findNodeByPath(root: any, path: string): any {
  const parts = path.split('/').filter(Boolean);
  let current = root;
  const startIdx = parts[0] === current.name ? 1 : 0;
  for (let i = startIdx; i < parts.length; i++) {
    const child = current.children?.find((c: any) => c.name === parts[i]);
    if (!child) return null;
    current = child;
  }
  return current;
}
