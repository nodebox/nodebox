import { useCallback, useRef, useState } from 'react';
import { useStore } from '../state/store';
import { usePanZoom } from '../hooks/usePanZoom';
import { useCanvasRenderer } from '../hooks/useCanvasRenderer';
import type { Node, Connection, PortType } from '../types/node';
import {
  NETWORK_BACKGROUND,
  NETWORK_GRID,
  ZINC_50,
  ZINC_200,
  FONT_SIZE_SMALL,
  NODE_BODY_GEOMETRY,
  NODE_BODY_INT,
  NODE_BODY_FLOAT,
  NODE_BODY_STRING,
  NODE_BODY_BOOLEAN,
  NODE_BODY_POINT,
  NODE_BODY_COLOR,
  NODE_BODY_LIST,
  NODE_BODY_DATA,
  NODE_BODY_DEFAULT,
  PORT_COLOR_INT,
  PORT_COLOR_FLOAT,
  PORT_COLOR_STRING,
  PORT_COLOR_BOOLEAN,
  PORT_COLOR_POINT,
  PORT_COLOR_COLOR,
  PORT_COLOR_GEOMETRY,
  PORT_COLOR_LIST,
  PORT_COLOR_DATA,
} from '../theme/tokens';

// Layout constants
const GRID_SIZE = 48;
const NODE_MARGIN = 8;
const NODE_WIDTH = 128;
const NODE_HEIGHT = 32;
const PORT_WIDTH = 12;
const PORT_HEIGHT = 4;
const PORT_SPACING = 8;

function nodeBodyColor(outputType: PortType): string {
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

function portColor(portType: PortType): string {
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

function nodeScreenRect(
  node: Node,
  worldToScreen: (wx: number, wy: number) => { x: number; y: number },
  zoom: number,
) {
  const pos = worldToScreen(
    node.position.x * GRID_SIZE + NODE_MARGIN,
    node.position.y * GRID_SIZE + NODE_MARGIN,
  );
  return {
    x: pos.x,
    y: pos.y,
    width: NODE_WIDTH * zoom,
    height: NODE_HEIGHT * zoom,
  };
}

function drawGrid(
  ctx: CanvasRenderingContext2D,
  width: number,
  height: number,
  panX: number,
  panY: number,
  zoom: number,
) {
  const cellSize = GRID_SIZE * zoom;
  if (cellSize < 4) return;

  ctx.strokeStyle = NETWORK_GRID;
  ctx.lineWidth = 1;
  ctx.beginPath();

  const offsetX = panX % cellSize;
  const offsetY = panY % cellSize;

  for (let x = offsetX; x < width; x += cellSize) {
    ctx.moveTo(Math.round(x) + 0.5, 0);
    ctx.lineTo(Math.round(x) + 0.5, height);
  }
  for (let y = offsetY; y < height; y += cellSize) {
    ctx.moveTo(0, Math.round(y) + 0.5);
    ctx.lineTo(width, Math.round(y) + 0.5);
  }
  ctx.stroke();
}

function drawNode(
  ctx: CanvasRenderingContext2D,
  node: Node,
  isSelected: boolean,
  isRendered: boolean,
  worldToScreen: (wx: number, wy: number) => { x: number; y: number },
  zoom: number,
) {
  const rect = nodeScreenRect(node, worldToScreen, zoom);
  const z = zoom;

  // Selection: white background then body color inset by 2px
  if (isSelected) {
    ctx.fillStyle = ZINC_50;
    ctx.fillRect(rect.x, rect.y, rect.width, rect.height);
    ctx.fillStyle = nodeBodyColor(node.outputType);
    ctx.fillRect(rect.x + 2 * z, rect.y + 2 * z, rect.width - 4 * z, rect.height - 4 * z);
  } else {
    ctx.fillStyle = nodeBodyColor(node.outputType);
    ctx.fillRect(rect.x, rect.y, rect.width, rect.height);
  }

  // Rendered child indicator: white triangle at bottom-right
  if (isRendered) {
    ctx.fillStyle = ZINC_50;
    ctx.beginPath();
    ctx.moveTo(rect.x + rect.width - 2 * z, rect.y + rect.height - 20 * z);
    ctx.lineTo(rect.x + rect.width - 2 * z, rect.y + rect.height - 2 * z);
    ctx.lineTo(rect.x + rect.width - 20 * z, rect.y + rect.height - 2 * z);
    ctx.closePath();
    ctx.fill();
  }

  // Node name (left-aligned)
  const fontSize = Math.max(8, FONT_SIZE_SMALL * zoom);
  ctx.font = `${fontSize}px -apple-system, system-ui, sans-serif`;
  ctx.fillStyle = ZINC_50;
  ctx.textAlign = 'left';
  ctx.textBaseline = 'middle';
  ctx.fillText(
    node.name,
    rect.x + 32 * z,
    rect.y + rect.height / 2,
    rect.width - 36 * z,
  );

  // Input ports (left-aligned from node left edge)
  const inputCount = node.inputs.length;
  if (inputCount > 0) {
    for (let i = 0; i < inputCount; i++) {
      const px = rect.x + (PORT_WIDTH + PORT_SPACING) * z * i;
      const py = rect.y - PORT_HEIGHT * z;
      ctx.fillStyle = portColor(node.inputs[i].portType);
      ctx.fillRect(px, py, PORT_WIDTH * z, PORT_HEIGHT * z);
    }
  }

  // Output port (bottom-left)
  const opx = rect.x;
  const opy = rect.y + rect.height;
  ctx.fillStyle = portColor(node.outputType);
  ctx.fillRect(opx, opy, PORT_WIDTH * z, PORT_HEIGHT * z);
}

function drawConnection(
  ctx: CanvasRenderingContext2D,
  conn: Connection,
  nodes: Node[],
  worldToScreen: (wx: number, wy: number) => { x: number; y: number },
  zoom: number,
) {
  const outputNode = nodes.find((n) => n.name === conn.outputNode);
  const inputNode = nodes.find((n) => n.name === conn.inputNode);
  if (!outputNode || !inputNode) return;

  // Output port position (bottom-left of output node)
  const outRect = nodeScreenRect(outputNode, worldToScreen, zoom);
  const x1 = outRect.x + (PORT_WIDTH * zoom) / 2;
  const y1 = outRect.y + outRect.height + PORT_HEIGHT * zoom;

  // Input port position (top of input node, left-aligned)
  const inRect = nodeScreenRect(inputNode, worldToScreen, zoom);
  const portIdx = inputNode.inputs.findIndex((p) => p.name === conn.inputPort);
  const x2 = inRect.x + (PORT_WIDTH + PORT_SPACING) * zoom * portIdx + (PORT_WIDTH * zoom) / 2;
  const y2 = inRect.y - PORT_HEIGHT * zoom;

  // Bezier curve
  const cpOffset = Math.abs(y2 - y1) * 0.4;
  ctx.strokeStyle = ZINC_200;
  ctx.lineWidth = 1.5;
  ctx.beginPath();
  ctx.moveTo(x1, y1);
  ctx.bezierCurveTo(x1, y1 + cpOffset, x2, y2 - cpOffset, x2, y2);
  ctx.stroke();
}

export function NetworkCanvas() {
  const children = useStore((s) => s.library.root.children);
  const connections = useStore((s) => s.library.root.connections);
  const renderedChild = useStore((s) => s.library.root.renderedChild);
  const selectedNodes = useStore((s) => s.selectedNodes);
  const selectNode = useStore((s) => s.selectNode);
  const toggleNode = useStore((s) => s.toggleNode);
  const clearSelection = useStore((s) => s.clearSelection);
  const setNodeDialogVisible = useStore((s) => s.setNodeDialogVisible);

  const panZoom = usePanZoom({ x: 200, y: 100 });
  const { state: pz, handlers, worldToScreen, screenToWorld } = panZoom;

  const [dragging, setDragging] = useState<string | null>(null);
  const dragStartWorld = useRef({ x: 0, y: 0 });

  const draw = useCallback(
    (ctx: CanvasRenderingContext2D, width: number, height: number) => {
      ctx.fillStyle = NETWORK_BACKGROUND;
      ctx.fillRect(0, 0, width, height);

      drawGrid(ctx, width, height, pz.panX, pz.panY, pz.zoom);

      for (const conn of connections) {
        drawConnection(ctx, conn, children, worldToScreen, pz.zoom);
      }

      for (const node of children) {
        const isSelected = selectedNodes.has(node.name);
        const isRendered = renderedChild === node.name;
        drawNode(ctx, node, isSelected, isRendered, worldToScreen, pz.zoom);
      }
    },
    [pz, children, connections, selectedNodes, renderedChild, worldToScreen],
  );

  const { canvasRef, requestRender } = useCanvasRenderer(draw);

  const hitTestNode = useCallback(
    (sx: number, sy: number): Node | null => {
      for (let i = children.length - 1; i >= 0; i--) {
        const node = children[i];
        const rect = nodeScreenRect(node, worldToScreen, pz.zoom);
        if (
          sx >= rect.x &&
          sx <= rect.x + rect.width &&
          sy >= rect.y &&
          sy <= rect.y + rect.height
        ) {
          return node;
        }
      }
      return null;
    },
    [children, worldToScreen, pz.zoom],
  );

  const handlePointerDown = useCallback(
    (e: React.PointerEvent<HTMLCanvasElement>) => {
      if (e.button === 1 || (e.button === 0 && e.altKey)) {
        handlers.onPointerDown(e);
        return;
      }

      if (e.button !== 0) return;

      const rect = e.currentTarget.getBoundingClientRect();
      const sx = e.clientX - rect.left;
      const sy = e.clientY - rect.top;
      const node = hitTestNode(sx, sy);

      if (node) {
        if (e.shiftKey || e.metaKey) {
          toggleNode(node.name);
        } else if (!selectedNodes.has(node.name)) {
          selectNode(node.name);
        }
        const world = screenToWorld(sx, sy);
        dragStartWorld.current = world;
        setDragging(node.name);
        e.currentTarget.setPointerCapture(e.pointerId);
      } else {
        clearSelection();
      }
    },
    [handlers, hitTestNode, selectNode, toggleNode, clearSelection, selectedNodes, screenToWorld],
  );

  const handlePointerMove = useCallback(
    (e: React.PointerEvent<HTMLCanvasElement>) => {
      handlers.onPointerMove(e);
      if (dragging) {
        requestRender();
      }
    },
    [handlers, dragging, requestRender],
  );

  const handlePointerUp = useCallback(
    (e: React.PointerEvent<HTMLCanvasElement>) => {
      handlers.onPointerUp(e);
      setDragging(null);
    },
    [handlers],
  );

  const handleDoubleClick = useCallback(
    (e: React.MouseEvent<HTMLCanvasElement>) => {
      const rect = e.currentTarget.getBoundingClientRect();
      const sx = e.clientX - rect.left;
      const sy = e.clientY - rect.top;
      const node = hitTestNode(sx, sy);
      if (!node) {
        setNodeDialogVisible(true);
      }
    },
    [hitTestNode, setNodeDialogVisible],
  );

  return (
    <canvas
      ref={canvasRef}
      className="w-full h-full block"
      style={{ cursor: panZoom.isPanning ? 'grabbing' : 'default' }}
      onWheel={handlers.onWheel}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onDoubleClick={handleDoubleClick}
    />
  );
}
