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
  CATEGORY_GEOMETRY,
  CATEGORY_TRANSFORM,
  CATEGORY_COLOR,
  CATEGORY_MATH,
  CATEGORY_LIST,
  CATEGORY_STRING,
  CATEGORY_DATA,
  CATEGORY_DEFAULT,
  TOOLTIP_BG,
  TOOLTIP_TEXT,
  PORT_HOVER,
} from '../theme/tokens';

// Layout constants
const GRID_SIZE = 48;
const NODE_MARGIN = 8;
const NODE_WIDTH = 128;
const NODE_HEIGHT = 32;
const PORT_WIDTH = 12;
const PORT_HEIGHT = 4;
const PORT_SPACING = 8;

// Hit-test tolerance for ports (in screen pixels)
const PORT_HIT_TOLERANCE = 6;

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

function categoryColor(category: string): string {
  switch (category) {
    case 'geometry': return CATEGORY_GEOMETRY;
    case 'transform': return CATEGORY_TRANSFORM;
    case 'color': return CATEGORY_COLOR;
    case 'math': return CATEGORY_MATH;
    case 'list': return CATEGORY_LIST;
    case 'string': return CATEGORY_STRING;
    case 'data': return CATEGORY_DATA;
    default: return CATEGORY_DEFAULT;
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
  hoveredPort: { nodeName: string; portName: string; portType: string } | null,
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

  // Category indicator (small colored square on left side)
  const catColor = categoryColor(node.category);
  ctx.fillStyle = catColor;
  ctx.fillRect(rect.x + 4 * z, rect.y + 4 * z, 20 * z, rect.height - 8 * z);

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
      const isHovered = hoveredPort?.nodeName === node.name && hoveredPort?.portName === node.inputs[i].name;
      ctx.fillStyle = isHovered ? PORT_HOVER : portColor(node.inputs[i].portType);
      ctx.fillRect(px, py, PORT_WIDTH * z, PORT_HEIGHT * z);
    }
  }

  // Output port (bottom-left)
  const opx = rect.x;
  const opy = rect.y + rect.height;
  const isOutputHovered = hoveredPort?.nodeName === node.name && hoveredPort?.portName === 'output';
  ctx.fillStyle = isOutputHovered ? PORT_HOVER : portColor(node.outputType);
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

function drawPendingConnection(
  ctx: CanvasRenderingContext2D,
  fromNode: Node,
  mouseX: number,
  mouseY: number,
  worldToScreen: (wx: number, wy: number) => { x: number; y: number },
  zoom: number,
) {
  const outRect = nodeScreenRect(fromNode, worldToScreen, zoom);
  const x1 = outRect.x + (PORT_WIDTH * zoom) / 2;
  const y1 = outRect.y + outRect.height + PORT_HEIGHT * zoom;

  const cpOffset = Math.abs(mouseY - y1) * 0.4;
  ctx.strokeStyle = ZINC_200;
  ctx.lineWidth = 1.5;
  ctx.setLineDash([4, 4]);
  ctx.beginPath();
  ctx.moveTo(x1, y1);
  ctx.bezierCurveTo(x1, y1 + cpOffset, mouseX, mouseY - cpOffset, mouseX, mouseY);
  ctx.stroke();
  ctx.setLineDash([]);
}

function drawRubberBand(
  ctx: CanvasRenderingContext2D,
  x1: number,
  y1: number,
  x2: number,
  y2: number,
) {
  const left = Math.min(x1, x2);
  const top = Math.min(y1, y2);
  const width = Math.abs(x2 - x1);
  const height = Math.abs(y2 - y1);

  ctx.fillStyle = 'rgba(255, 255, 255, 0.05)';
  ctx.fillRect(left, top, width, height);
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.3)';
  ctx.lineWidth = 1;
  ctx.setLineDash([4, 4]);
  ctx.strokeRect(left + 0.5, top + 0.5, width, height);
  ctx.setLineDash([]);
}

interface CreatingConnection {
  fromNode: string;
  fromType: PortType;
  mouseX: number;
  mouseY: number;
}

interface RubberBand {
  startX: number;
  startY: number;
  currentX: number;
  currentY: number;
}

export function NetworkCanvas() {
  const children = useStore((s) => s.library.root.children);
  const connections = useStore((s) => s.library.root.connections);
  const renderedChild = useStore((s) => s.library.root.renderedChild);
  const selectedNodes = useStore((s) => s.selectedNodes);
  const selectNode = useStore((s) => s.selectNode);
  const selectNodes = useStore((s) => s.selectNodes);
  const toggleNode = useStore((s) => s.toggleNode);
  const clearSelection = useStore((s) => s.clearSelection);
  const setNodeDialogVisible = useStore((s) => s.setNodeDialogVisible);
  const setNodePosition = useStore((s) => s.setNodePosition);
  const setRenderedChild = useStore((s) => s.setRenderedChild);
  const addConnection = useStore((s) => s.addConnection);

  const panZoom = usePanZoom({ x: 200, y: 100 });
  const { state: pz, handlers, worldToScreen, screenToWorld } = panZoom;

  const [dragging, setDragging] = useState<string | null>(null);
  const dragStartWorld = useRef({ x: 0, y: 0 });
  const dragOrigPos = useRef({ x: 0, y: 0 });

  const [creatingConnection, setCreatingConnection] = useState<CreatingConnection | null>(null);
  const [rubberBand, setRubberBand] = useState<RubberBand | null>(null);
  const [hoveredPort, setHoveredPort] = useState<{
    nodeName: string;
    portName: string;
    portType: string;
    screenX: number;
    screenY: number;
  } | null>(null);

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
        drawNode(ctx, node, isSelected, isRendered, worldToScreen, pz.zoom, hoveredPort);
      }

      // Draw pending connection
      if (creatingConnection) {
        const fromNode = children.find((n) => n.name === creatingConnection.fromNode);
        if (fromNode) {
          drawPendingConnection(
            ctx,
            fromNode,
            creatingConnection.mouseX,
            creatingConnection.mouseY,
            worldToScreen,
            pz.zoom,
          );
        }
      }

      // Draw rubber band selection
      if (rubberBand) {
        drawRubberBand(
          ctx,
          rubberBand.startX,
          rubberBand.startY,
          rubberBand.currentX,
          rubberBand.currentY,
        );
      }
    },
    [pz, children, connections, selectedNodes, renderedChild, worldToScreen, creatingConnection, rubberBand, hoveredPort],
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

  // Hit test for output ports (bottom-left of node)
  const findOutputPortAt = useCallback(
    (sx: number, sy: number): { node: Node; portType: PortType } | null => {
      for (let i = children.length - 1; i >= 0; i--) {
        const node = children[i];
        const rect = nodeScreenRect(node, worldToScreen, pz.zoom);
        const z = pz.zoom;
        const opx = rect.x;
        const opy = rect.y + rect.height;
        const opw = PORT_WIDTH * z;
        const oph = PORT_HEIGHT * z;
        if (
          sx >= opx - PORT_HIT_TOLERANCE &&
          sx <= opx + opw + PORT_HIT_TOLERANCE &&
          sy >= opy - PORT_HIT_TOLERANCE &&
          sy <= opy + oph + PORT_HIT_TOLERANCE
        ) {
          return { node, portType: node.outputType };
        }
      }
      return null;
    },
    [children, worldToScreen, pz.zoom],
  );

  // Hit test for input ports (top of node)
  const findInputPortAt = useCallback(
    (sx: number, sy: number): { node: Node; portName: string; portType: PortType } | null => {
      for (let i = children.length - 1; i >= 0; i--) {
        const node = children[i];
        const rect = nodeScreenRect(node, worldToScreen, pz.zoom);
        const z = pz.zoom;
        for (let j = 0; j < node.inputs.length; j++) {
          const px = rect.x + (PORT_WIDTH + PORT_SPACING) * z * j;
          const py = rect.y - PORT_HEIGHT * z;
          const pw = PORT_WIDTH * z;
          const ph = PORT_HEIGHT * z;
          if (
            sx >= px - PORT_HIT_TOLERANCE &&
            sx <= px + pw + PORT_HIT_TOLERANCE &&
            sy >= py - PORT_HIT_TOLERANCE &&
            sy <= py + ph + PORT_HIT_TOLERANCE
          ) {
            return { node, portName: node.inputs[j].name, portType: node.inputs[j].portType };
          }
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

      // Check output port hit first (for connection creation)
      const outputPort = findOutputPortAt(sx, sy);
      if (outputPort) {
        setCreatingConnection({
          fromNode: outputPort.node.name,
          fromType: outputPort.portType,
          mouseX: sx,
          mouseY: sy,
        });
        e.currentTarget.setPointerCapture(e.pointerId);
        return;
      }

      const node = hitTestNode(sx, sy);

      if (node) {
        if (e.shiftKey || e.metaKey) {
          toggleNode(node.name);
        } else if (!selectedNodes.has(node.name)) {
          selectNode(node.name);
        }
        const world = screenToWorld(sx, sy);
        dragStartWorld.current = world;
        dragOrigPos.current = { x: node.position.x, y: node.position.y };
        setDragging(node.name);
        e.currentTarget.setPointerCapture(e.pointerId);
      } else {
        clearSelection();
        // Start rubber band selection
        setRubberBand({
          startX: sx,
          startY: sy,
          currentX: sx,
          currentY: sy,
        });
        e.currentTarget.setPointerCapture(e.pointerId);
      }
    },
    [handlers, hitTestNode, findOutputPortAt, selectNode, toggleNode, clearSelection, selectedNodes, screenToWorld],
  );

  const handlePointerMove = useCallback(
    (e: React.PointerEvent<HTMLCanvasElement>) => {
      handlers.onPointerMove(e);

      if (dragging) {
        const rect = e.currentTarget.getBoundingClientRect();
        const sx = e.clientX - rect.left;
        const sy = e.clientY - rect.top;
        const world = screenToWorld(sx, sy);
        const deltaWorldX = world.x - dragStartWorld.current.x;
        const deltaWorldY = world.y - dragStartWorld.current.y;
        // Convert world delta to grid cells
        const deltaGridX = Math.round(deltaWorldX / GRID_SIZE);
        const deltaGridY = Math.round(deltaWorldY / GRID_SIZE);
        const newX = dragOrigPos.current.x + deltaGridX;
        const newY = dragOrigPos.current.y + deltaGridY;
        setNodePosition(dragging, { x: newX, y: newY });
        requestRender();
      }

      if (creatingConnection) {
        const rect = e.currentTarget.getBoundingClientRect();
        const sx = e.clientX - rect.left;
        const sy = e.clientY - rect.top;
        setCreatingConnection((prev) =>
          prev ? { ...prev, mouseX: sx, mouseY: sy } : null,
        );
        requestRender();
      }

      if (rubberBand) {
        const rect = e.currentTarget.getBoundingClientRect();
        const sx = e.clientX - rect.left;
        const sy = e.clientY - rect.top;
        setRubberBand((prev) =>
          prev ? { ...prev, currentX: sx, currentY: sy } : null,
        );
        requestRender();
      }

      // Port hover detection
      if (!dragging && !creatingConnection && !rubberBand) {
        const rect = e.currentTarget.getBoundingClientRect();
        const sx = e.clientX - rect.left;
        const sy = e.clientY - rect.top;

        const inputPort = findInputPortAt(sx, sy);
        if (inputPort) {
          setHoveredPort({
            nodeName: inputPort.node.name,
            portName: inputPort.portName,
            portType: inputPort.portType,
            screenX: sx,
            screenY: sy,
          });
          requestRender();
        } else {
          const outputPort = findOutputPortAt(sx, sy);
          if (outputPort) {
            setHoveredPort({
              nodeName: outputPort.node.name,
              portName: 'output',
              portType: outputPort.portType,
              screenX: sx,
              screenY: sy,
            });
            requestRender();
          } else if (hoveredPort) {
            setHoveredPort(null);
            requestRender();
          }
        }
      }
    },
    [handlers, dragging, creatingConnection, rubberBand, screenToWorld, setNodePosition, requestRender, findInputPortAt, findOutputPortAt, hoveredPort],
  );

  const handlePointerUp = useCallback(
    (e: React.PointerEvent<HTMLCanvasElement>) => {
      handlers.onPointerUp(e);

      if (creatingConnection) {
        const rect = e.currentTarget.getBoundingClientRect();
        const sx = e.clientX - rect.left;
        const sy = e.clientY - rect.top;
        const inputPort = findInputPortAt(sx, sy);
        if (inputPort && inputPort.node.name !== creatingConnection.fromNode) {
          addConnection('root', {
            outputNode: creatingConnection.fromNode,
            inputNode: inputPort.node.name,
            inputPort: inputPort.portName,
          });
        }
        setCreatingConnection(null);
        requestRender();
      }

      if (rubberBand) {
        // Find all nodes intersecting the rubber band rectangle
        const left = Math.min(rubberBand.startX, rubberBand.currentX);
        const top = Math.min(rubberBand.startY, rubberBand.currentY);
        const right = Math.max(rubberBand.startX, rubberBand.currentX);
        const bottom = Math.max(rubberBand.startY, rubberBand.currentY);
        const width = right - left;
        const height = bottom - top;

        // Only select if the rubber band has meaningful size (not just a click)
        if (width > 4 || height > 4) {
          const names: string[] = [];
          for (const node of children) {
            const nodeRect = nodeScreenRect(node, worldToScreen, pz.zoom);
            // Check intersection
            if (
              nodeRect.x + nodeRect.width >= left &&
              nodeRect.x <= right &&
              nodeRect.y + nodeRect.height >= top &&
              nodeRect.y <= bottom
            ) {
              names.push(node.name);
            }
          }
          if (names.length > 0) {
            selectNodes(names);
          }
        }
        setRubberBand(null);
        requestRender();
      }

      setDragging(null);
    },
    [handlers, creatingConnection, rubberBand, findInputPortAt, addConnection, children, worldToScreen, pz.zoom, selectNodes, requestRender],
  );

  const handleDoubleClick = useCallback(
    (e: React.MouseEvent<HTMLCanvasElement>) => {
      const rect = e.currentTarget.getBoundingClientRect();
      const sx = e.clientX - rect.left;
      const sy = e.clientY - rect.top;
      const node = hitTestNode(sx, sy);
      if (node) {
        setRenderedChild('root', node.name);
      } else {
        setNodeDialogVisible(true);
      }
    },
    [hitTestNode, setNodeDialogVisible, setRenderedChild],
  );

  return (
    <div style={{ position: 'relative', width: '100%', height: '100%' }}>
      <canvas
        ref={canvasRef}
        className="w-full h-full block"
        style={{ cursor: panZoom.isPanning ? 'grabbing' : dragging ? 'move' : creatingConnection ? 'crosshair' : 'default' }}
        onWheel={handlers.onWheel}
        onPointerDown={handlePointerDown}
        onPointerMove={handlePointerMove}
        onPointerUp={handlePointerUp}
        onDoubleClick={handleDoubleClick}
      />
      {hoveredPort && (
        <div
          style={{
            position: 'absolute',
            left: hoveredPort.screenX + 12,
            top: hoveredPort.screenY - 8,
            background: TOOLTIP_BG,
            color: TOOLTIP_TEXT,
            fontSize: FONT_SIZE_SMALL,
            padding: '2px 6px',
            pointerEvents: 'none',
            whiteSpace: 'nowrap',
            zIndex: 10,
          }}
        >
          {hoveredPort.portName} ({hoveredPort.portType})
        </div>
      )}
    </div>
  );
}
