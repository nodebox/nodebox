import React, { useCallback, useRef, useState } from 'react';
import { useStore } from '../state/store';
import { usePanZoom } from '../hooks/usePanZoom';
import { useCanvasRenderer } from '../hooks/useCanvasRenderer';
import type { Node } from 'nodebox-core';
import {
  NETWORK_BACKGROUND,
  GRID_LINE_COLOR,
  ZINC_50,
  FONT_SIZE_SMALL,
  NODE_BODY_GEOMETRY, NODE_BODY_INT, NODE_BODY_FLOAT, NODE_BODY_STRING,
  NODE_BODY_BOOLEAN, NODE_BODY_POINT, NODE_BODY_COLOR, NODE_BODY_LIST,
  NODE_BODY_DATA, NODE_BODY_DEFAULT,
  PORT_COLOR_INT, PORT_COLOR_FLOAT, PORT_COLOR_STRING, PORT_COLOR_BOOLEAN,
  PORT_COLOR_POINT, PORT_COLOR_COLOR, PORT_COLOR_GEOMETRY, PORT_COLOR_LIST,
  PORT_COLOR_DATA,
  VIOLET_400,
} from '../theme/tokens';

const GRID_SIZE = 48;
const NODE_WIDTH = 128;
const NODE_HEIGHT = 32;
const PORT_WIDTH = 12;
const PORT_HEIGHT = 4;
const PORT_SPACING = 8;

function nodeBodyColor(outputType: string): string {
  switch (outputType) {
    case 'geometry': return NODE_BODY_GEOMETRY;
    case 'int': return NODE_BODY_INT;
    case 'float': return NODE_BODY_FLOAT;
    case 'string': return NODE_BODY_STRING;
    case 'boolean': return NODE_BODY_BOOLEAN;
    case 'point': return NODE_BODY_POINT;
    case 'color': return NODE_BODY_COLOR;
    case 'list': return NODE_BODY_LIST;
    case 'data': return NODE_BODY_DATA;
    default: return NODE_BODY_DEFAULT;
  }
}

function portColor(portType: string): string {
  switch (portType) {
    case 'int': return PORT_COLOR_INT;
    case 'float': return PORT_COLOR_FLOAT;
    case 'string': return PORT_COLOR_STRING;
    case 'boolean': return PORT_COLOR_BOOLEAN;
    case 'point': return PORT_COLOR_POINT;
    case 'color': return PORT_COLOR_COLOR;
    case 'geometry': return PORT_COLOR_GEOMETRY;
    case 'list': return PORT_COLOR_LIST;
    case 'data': return PORT_COLOR_DATA;
    default: return PORT_COLOR_GEOMETRY;
  }
}

export function NetworkCanvas() {
  const library = useStore((s) => s.library);
  const currentNetworkPath = useStore((s) => s.currentNetworkPath);
  const selectedNodes = useStore((s) => s.selectedNodes);
  const selectNode = useStore((s) => s.selectNode);
  const toggleNode = useStore((s) => s.toggleNode);
  const deselectAll = useStore((s) => s.deselectAll);
  const setNodePositionAction = useStore((s) => s.setNodePositionAction);
  const setRenderedChildAction = useStore((s) => s.setRenderedChildAction);
  const setCurrentNetworkPath = useStore((s) => s.setCurrentNetworkPath);
  const setNodeSelectionDialogOpen = useStore((s) => s.setNodeSelectionDialogOpen);
  const setNodeDialogPosition = useStore((s) => s.setNodeDialogPosition);

  const panZoom = usePanZoom();
  const { state: pz, handlers } = panZoom;

  const [dragNode, setDragNode] = useState<string | null>(null);
  const dragStartWorld = useRef({ x: 0, y: 0 });
  const dragStartNodePos = useRef({ x: 0, y: 0 });

  const findNetwork = useCallback((): Node | null => {
    if (!library) return null;
    const parts = currentNetworkPath.split('/').filter(Boolean);
    let current: Node = library.root;
    const startIdx = parts[0] === current.name ? 1 : 0;
    for (let i = startIdx; i < parts.length; i++) {
      const child = current.children.find((c: Node) => c.name === parts[i]);
      if (!child) return null;
      current = child;
    }
    return current;
  }, [library, currentNetworkPath]);

  const worldToNode = useCallback((wx: number, wy: number, network: Node): Node | null => {
    for (let i = network.children.length - 1; i >= 0; i--) {
      const child = network.children[i];
      const nx = child.position.x * GRID_SIZE;
      const ny = child.position.y * GRID_SIZE;
      if (wx >= nx && wx <= nx + NODE_WIDTH && wy >= ny && wy <= ny + NODE_HEIGHT) {
        return child;
      }
    }
    return null;
  }, []);

  const draw = useCallback(
    (ctx: CanvasRenderingContext2D, width: number, height: number) => {
      ctx.fillStyle = NETWORK_BACKGROUND;
      ctx.fillRect(0, 0, width, height);

      const network = findNetwork();
      if (!network) return;

      ctx.save();
      ctx.translate(pz.panX, pz.panY);
      ctx.scale(pz.zoom, pz.zoom);

      // Grid
      const gridStart = panZoom.screenToWorld(0, 0);
      const gridEnd = panZoom.screenToWorld(width, height);
      ctx.strokeStyle = GRID_LINE_COLOR;
      ctx.lineWidth = 0.5 / pz.zoom;
      const startX = Math.floor(gridStart.x / GRID_SIZE) * GRID_SIZE;
      const startY = Math.floor(gridStart.y / GRID_SIZE) * GRID_SIZE;
      for (let x = startX; x <= gridEnd.x; x += GRID_SIZE) {
        ctx.beginPath(); ctx.moveTo(x, gridStart.y); ctx.lineTo(x, gridEnd.y); ctx.stroke();
      }
      for (let y = startY; y <= gridEnd.y; y += GRID_SIZE) {
        ctx.beginPath(); ctx.moveTo(gridStart.x, y); ctx.lineTo(gridEnd.x, y); ctx.stroke();
      }

      // Connections (bezier curves colored by port type)
      for (const conn of network.connections) {
        const fromNode = network.children.find((c: Node) => c.name === conn.outputNode);
        const toNode = network.children.find((c: Node) => c.name === conn.inputNode);
        if (!fromNode || !toNode) continue;

        const fx = fromNode.position.x * GRID_SIZE + NODE_WIDTH / 2;
        const fy = fromNode.position.y * GRID_SIZE + NODE_HEIGHT;
        const tx = toNode.position.x * GRID_SIZE + NODE_WIDTH / 2;
        const ty = toNode.position.y * GRID_SIZE;

        const toPort = toNode.inputs.find((p: any) => p.name === conn.inputPort);
        ctx.strokeStyle = toPort ? portColor(toPort.type) : PORT_COLOR_GEOMETRY;
        ctx.lineWidth = 2 / pz.zoom;
        ctx.beginPath();
        ctx.moveTo(fx, fy);
        const midY = (fy + ty) / 2;
        ctx.bezierCurveTo(fx, midY, tx, midY, tx, ty);
        ctx.stroke();
      }

      // Nodes
      for (const child of network.children) {
        const x = child.position.x * GRID_SIZE;
        const y = child.position.y * GRID_SIZE;
        const isSelected = selectedNodes.includes(child.name);
        const isRendered = network.renderedChild === child.name;

        // Body
        ctx.fillStyle = nodeBodyColor(child.outputType);
        ctx.beginPath();
        ctx.roundRect(x, y, NODE_WIDTH, NODE_HEIGHT, 3);
        ctx.fill();

        if (isSelected) {
          ctx.strokeStyle = VIOLET_400;
          ctx.lineWidth = 2 / pz.zoom;
          ctx.stroke();
        }

        // Rendered child triangle
        if (isRendered) {
          ctx.fillStyle = '#ffffff';
          ctx.beginPath();
          ctx.moveTo(x + NODE_WIDTH - 10, y);
          ctx.lineTo(x + NODE_WIDTH, y);
          ctx.lineTo(x + NODE_WIDTH, y + 10);
          ctx.fill();
        }

        // Name
        ctx.fillStyle = ZINC_50;
        ctx.font = `${FONT_SIZE_SMALL}px -apple-system, sans-serif`;
        ctx.textAlign = 'left';
        ctx.textBaseline = 'middle';
        ctx.fillText(child.name, x + 28, y + NODE_HEIGHT / 2);

        // Input ports
        const inputPorts = child.inputs.filter((p: any) => p.type !== 'context');
        const portStartX = x + (NODE_WIDTH - (inputPorts.length * (PORT_WIDTH + PORT_SPACING) - PORT_SPACING)) / 2;
        for (let pi = 0; pi < inputPorts.length; pi++) {
          const port = inputPorts[pi];
          ctx.fillStyle = portColor(port.type);
          ctx.fillRect(portStartX + pi * (PORT_WIDTH + PORT_SPACING), y - PORT_HEIGHT, PORT_WIDTH, PORT_HEIGHT);
        }

        // Output port
        ctx.fillStyle = portColor(child.outputType);
        ctx.fillRect(x + NODE_WIDTH / 2 - PORT_WIDTH / 2, y + NODE_HEIGHT, PORT_WIDTH, PORT_HEIGHT);
      }

      ctx.restore();
    },
    [pz, findNetwork, selectedNodes, panZoom],
  );

  const { canvasRef } = useCanvasRenderer(draw);

  const handlePointerDown = useCallback((e: React.PointerEvent<HTMLCanvasElement>) => {
    if (e.button === 1 || e.altKey || panZoom.isSpaceDown) {
      handlers.onPointerDown(e);
      return;
    }
    if (e.button !== 0) return;

    const network = findNetwork();
    if (!network) return;

    const rect = e.currentTarget.getBoundingClientRect();
    const world = panZoom.screenToWorld(e.clientX - rect.left, e.clientY - rect.top);
    const hit = worldToNode(world.x, world.y, network);

    if (hit) {
      if (e.shiftKey) toggleNode(hit.name);
      else if (!selectedNodes.includes(hit.name)) selectNode(hit.name);
      setDragNode(hit.name);
      dragStartWorld.current = { x: world.x, y: world.y };
      dragStartNodePos.current = { x: hit.position.x, y: hit.position.y };
      e.currentTarget.setPointerCapture(e.pointerId);
    } else {
      deselectAll();
    }
  }, [findNetwork, worldToNode, selectedNodes, selectNode, toggleNode, deselectAll, handlers, panZoom]);

  const handlePointerMove = useCallback((e: React.PointerEvent<HTMLCanvasElement>) => {
    if (panZoom.isPanning) { handlers.onPointerMove(e); return; }
    if (!dragNode) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const world = panZoom.screenToWorld(e.clientX - rect.left, e.clientY - rect.top);
    const dx = (world.x - dragStartWorld.current.x) / GRID_SIZE;
    const dy = (world.y - dragStartWorld.current.y) / GRID_SIZE;
    setNodePositionAction(currentNetworkPath, dragNode, {
      x: dragStartNodePos.current.x + dx,
      y: dragStartNodePos.current.y + dy,
    });
  }, [dragNode, panZoom, handlers, setNodePositionAction, currentNetworkPath]);

  const handlePointerUp = useCallback((e: React.PointerEvent<HTMLCanvasElement>) => {
    if (panZoom.isPanning) { handlers.onPointerUp(e); return; }
    setDragNode(null);
  }, [panZoom, handlers]);

  const handleDoubleClick = useCallback((e: React.MouseEvent<HTMLCanvasElement>) => {
    const network = findNetwork();
    if (!network) return;
    const rect = e.currentTarget.getBoundingClientRect();
    const world = panZoom.screenToWorld(e.clientX - rect.left, e.clientY - rect.top);
    const hit = worldToNode(world.x, world.y, network);
    if (hit) {
      if (hit.children.length > 0) {
        setCurrentNetworkPath(currentNetworkPath + '/' + hit.name);
      } else {
        setRenderedChildAction(currentNetworkPath, hit.name);
      }
    } else {
      // Double-click on empty space: open node dialog, placing node at clicked grid position
      const gridX = Math.round(world.x / GRID_SIZE);
      const gridY = Math.round(world.y / GRID_SIZE);
      setNodeDialogPosition({ x: gridX, y: gridY });
      setNodeSelectionDialogOpen(true);
    }
  }, [findNetwork, worldToNode, panZoom, setCurrentNetworkPath, setRenderedChildAction, currentNetworkPath, setNodeDialogPosition, setNodeSelectionDialogOpen]);

  return (
    <canvas
      ref={canvasRef}
      style={{ width: '100%', height: '100%', display: 'block', cursor: panZoom.isPanning ? 'grabbing' : panZoom.isSpaceDown ? 'grab' : dragNode ? 'move' : 'default' }}
      onWheel={handlers.onWheel}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onDoubleClick={handleDoubleClick}
    />
  );
}
