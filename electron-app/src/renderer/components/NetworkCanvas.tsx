import { useCallback, useRef, useState, useEffect } from 'react';
import { useStore } from '../state/store';
import { usePanZoom } from '../hooks/usePanZoom';
import { useCanvasRenderer } from '../hooks/useCanvasRenderer';
import type { Node, Connection, PortType } from '../types/node';
import {
  NETWORK_BACKGROUND,
  NETWORK_GRID,
  ZINC_50,
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
  TOOLTIP_BG,
  TOOLTIP_TEXT,
  PORT_HOVER,
  ERROR_RED,
} from '../theme/tokens';

// Layout constants
const GRID_SIZE = 48;
const NODE_MARGIN = 8;
const NODE_WIDTH = 128;
const NODE_HEIGHT = 32;
const PORT_WIDTH = 12;
const PORT_HEIGHT = 4;
const PORT_SPACING = 8;

const NODE_ICON_SIZE = 24;
const NODE_PADDING = 4;
const PORT_HEIGHT_EXPANDED = 6;
const PORT_HEIGHT_SHRUNK = 1;
// Hit-test tolerance for ports (in screen pixels)
const PORT_HIT_TOLERANCE = 6;
// Margin between ports for expanded hit-area calculations during connection drag
const PORT_MARGIN = 6;

/** Check if an output type is compatible with an input type (mirrors Rust PortType::is_compatible). */
function isPortCompatible(outputType: string, inputType: string): boolean {
  if (outputType === inputType) return true;
  if (inputType === 'List') return true;
  if (outputType === 'List') return true;
  if (outputType === 'Data' || inputType === 'Data') return true;
  if (inputType === 'String') return true;
  if (outputType === 'Int' && inputType === 'Float') return true;
  if (outputType === 'Float' && inputType === 'Int') return true;
  if ((outputType === 'Int' || outputType === 'Float') && inputType === 'Point') return true;
  return false;
}

function nodeBodyColor(outputType: PortType): string {
  switch (outputType) {
    case 'Geometry': return NODE_BODY_GEOMETRY;
    case 'Int': return NODE_BODY_INT;
    case 'Float': return NODE_BODY_FLOAT;
    case 'String': return NODE_BODY_STRING;
    case 'Boolean': return NODE_BODY_BOOLEAN;
    case 'Point': return NODE_BODY_POINT;
    case 'Color': return NODE_BODY_COLOR;
    case 'List': return NODE_BODY_LIST;
    case 'Data': return NODE_BODY_DATA;
    default: return NODE_BODY_DEFAULT;
  }
}

function portColor(portType: PortType): string {
  switch (portType) {
    case 'Int': return PORT_COLOR_INT;
    case 'Float': return PORT_COLOR_FLOAT;
    case 'String': return PORT_COLOR_STRING;
    case 'Boolean': return PORT_COLOR_BOOLEAN;
    case 'Point': return PORT_COLOR_POINT;
    case 'Color': return PORT_COLOR_COLOR;
    case 'Geometry': return PORT_COLOR_GEOMETRY;
    case 'List': return PORT_COLOR_LIST;
    case 'Data': return PORT_COLOR_DATA;
    default: return PORT_COLOR_GEOMETRY;
  }
}

/** Strip trailing digits to get the name prefix (e.g. "rect1" -> "rect"). */
function extractNamePrefix(name: string): string {
  const match = name.match(/^(.*\D)\d*$/);
  return match ? match[1] : name;
}

/** Generate a unique name with the given prefix, avoiding names in `existing`. */
function generateUniqueName(prefix: string, existing: Set<string>): string {
  for (let i = 1; i < 1000; i++) {
    const name = `${prefix}${i}`;
    if (!existing.has(name)) return name;
  }
  return `${prefix}1000`;
}

// Icon cache: loads SVG icons and pre-renders them as white-tinted OffscreenCanvas images
class NodeIconCache {
  private entries = new Map<string, OffscreenCanvas | null>();
  private listeners: (() => void)[] = [];

  onLoad(cb: () => void) {
    this.listeners.push(cb);
  }

  removeOnLoad(cb: () => void) {
    this.listeners = this.listeners.filter((c) => c !== cb);
  }

  ensure(name: string): void {
    if (this.entries.has(name)) return;
    this.entries.set(name, null);

    const img = new Image();
    img.onload = () => {
      const size = 32;
      const oc = new OffscreenCanvas(size, size);
      const octx = oc.getContext('2d')!;
      octx.drawImage(img, 0, 0, size, size);
      // Tint black paths to white using source-atop compositing
      octx.globalCompositeOperation = 'source-atop';
      octx.fillStyle = 'white';
      octx.fillRect(0, 0, size, size);
      this.entries.set(name, oc);
      for (const cb of this.listeners) cb();
    };
    img.src = `/icons/corevector/${name}.svg`;
  }

  draw(ctx: CanvasRenderingContext2D, name: string, cx: number, cy: number, size: number): void {
    const tinted = this.entries.get(name);
    if (!tinted) return;
    ctx.drawImage(tinted, cx - size / 2, cy - size / 2, size, size);
  }
}

const nodeIconCache = new NodeIconCache();

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
  hasError: boolean,
  dragOutputType: string | null,
) {
  const rect = nodeScreenRect(node, worldToScreen, zoom);
  const z = zoom;

  // Use error color if node has an error, otherwise use output type color
  const bodyColor = hasError ? ERROR_RED : nodeBodyColor(node.output_type);

  // Selection: white background then body color inset by 2px
  if (isSelected) {
    ctx.fillStyle = ZINC_50;
    ctx.fillRect(rect.x, rect.y, rect.width, rect.height);
    ctx.fillStyle = bodyColor;
    ctx.fillRect(rect.x + 2 * z, rect.y + 2 * z, rect.width - 4 * z, rect.height - 4 * z);
  } else {
    ctx.fillStyle = bodyColor;
    ctx.fillRect(rect.x, rect.y, rect.width, rect.height);
  }

  // Node icon (SVG, no category background)
  const iconSize = NODE_ICON_SIZE * z;
  const iconCx = rect.x + (NODE_PADDING + NODE_ICON_SIZE / 2) * z;
  const iconCy = rect.y + rect.height / 2;
  const protoName = node.prototype?.split('.').pop();
  if (protoName) {
    nodeIconCache.ensure(protoName);
    nodeIconCache.draw(ctx, protoName, iconCx, iconCy, iconSize);
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

  // Input ports (left-aligned from node left edge) with connection-drag feedback
  for (let i = 0; i < node.inputs.length; i++) {
    const port = node.inputs[i];
    const px = rect.x + (PORT_WIDTH + PORT_SPACING) * z * i;
    const isHovered = hoveredPort?.nodeName === node.name && hoveredPort?.portName === port.name;

    let portHeight: number;
    let color: string;
    if (dragOutputType) {
      const compatible = isPortCompatible(dragOutputType, port.port_type);
      if (isHovered && compatible) {
        portHeight = PORT_HEIGHT;
        color = PORT_HOVER;
      } else if (compatible) {
        portHeight = PORT_HEIGHT_EXPANDED;
        color = portColor(port.port_type);
      } else {
        portHeight = PORT_HEIGHT_SHRUNK;
        color = portColor(port.port_type);
      }
    } else if (isHovered) {
      portHeight = PORT_HEIGHT;
      color = PORT_HOVER;
    } else {
      portHeight = PORT_HEIGHT;
      color = portColor(port.port_type);
    }

    // Port bottom hugs node top
    const py = rect.y - portHeight * z;
    ctx.fillStyle = color;
    ctx.fillRect(px, py, PORT_WIDTH * z, portHeight * z);
  }

  // Output port (bottom-left) — no expand/shrink during drag
  const opx = rect.x;
  const opy = rect.y + rect.height;
  const isOutputHovered = hoveredPort?.nodeName === node.name && hoveredPort?.portName === 'output';
  ctx.fillStyle = (isOutputHovered && !dragOutputType) ? PORT_HOVER : portColor(node.output_type);
  ctx.fillRect(opx, opy, PORT_WIDTH * z, PORT_HEIGHT * z);
}

function drawConnection(
  ctx: CanvasRenderingContext2D,
  conn: Connection,
  nodes: Node[],
  worldToScreen: (wx: number, wy: number) => { x: number; y: number },
  zoom: number,
) {
  const outputNode = nodes.find((n) => n.name === conn.output_node);
  const inputNode = nodes.find((n) => n.name === conn.input_node);
  if (!outputNode || !inputNode) return;

  // Output port position (bottom-left of output node)
  const outRect = nodeScreenRect(outputNode, worldToScreen, zoom);
  const x1 = outRect.x + (PORT_WIDTH * zoom) / 2;
  const y1 = outRect.y + outRect.height + PORT_HEIGHT * zoom;

  // Input port position (top of input node, left-aligned)
  const inRect = nodeScreenRect(inputNode, worldToScreen, zoom);
  const portIdx = inputNode.inputs.findIndex((p) => p.name === conn.input_port);
  const x2 = inRect.x + (PORT_WIDTH + PORT_SPACING) * zoom * portIdx + (PORT_WIDTH * zoom) / 2;
  const y2 = inRect.y - PORT_HEIGHT * zoom;

  // Wire colored by input port type (matching egui behavior)
  const inputPort = inputNode.inputs[portIdx];
  ctx.strokeStyle = inputPort ? portColor(inputPort.port_type) : portColor(outputNode.output_type);
  ctx.lineWidth = 1.5;
  ctx.beginPath();
  ctx.moveTo(x1, y1);

  // Short connections render as straight lines (< 1 grid cell)
  const dy = Math.abs(y2 - y1);
  if (dy < GRID_SIZE * zoom) {
    ctx.lineTo(x2, y2);
  } else {
    const cpOffset = dy * 0.4;
    ctx.bezierCurveTo(x1, y1 + cpOffset, x2, y2 - cpOffset, x2, y2);
  }
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
  ctx.strokeStyle = portColor(fromNode.output_type);
  ctx.lineWidth = 1.5;
  ctx.beginPath();
  ctx.moveTo(x1, y1);
  ctx.bezierCurveTo(x1, y1 + cpOffset, mouseX, mouseY - cpOffset, mouseX, mouseY);
  ctx.stroke();
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
  mouseX: number;
  mouseY: number;
  isReroute: boolean;
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
  const renderedChild = useStore((s) => s.library.root.rendered_child);
  const selectedNodes = useStore((s) => s.selectedNodes);
  const selectNode = useStore((s) => s.selectNode);
  const selectNodes = useStore((s) => s.selectNodes);
  const toggleNode = useStore((s) => s.toggleNode);
  const clearSelection = useStore((s) => s.clearSelection);
  const setNodeDialogVisible = useStore((s) => s.setNodeDialogVisible);
  const setNodeDialogPosition = useStore((s) => s.setNodeDialogPosition);
  const setPendingConnection = useStore((s) => s.setPendingConnection);
  const setNodePosition = useStore((s) => s.setNodePosition);
  const setRenderedChild = useStore((s) => s.setRenderedChild);
  const addNode = useStore((s) => s.addNode);
  const addConnection = useStore((s) => s.addConnection);
  const removeConnection = useStore((s) => s.removeConnection);
  const pushSnapshot = useStore((s) => s.pushSnapshot);
  const library = useStore((s) => s.library);
  const renderErrors = useStore((s) => s.renderResult?.errors);

  const panZoom = usePanZoom({ x: 8, y: 8 });
  const { state: pz, handlers, worldToScreen, screenToWorld } = panZoom;

  const [isDragging, setIsDragging] = useState(false);
  const dragStartWorld = useRef({ x: 0, y: 0 });
  const dragStartPositions = useRef<Map<string, { x: number; y: number }>>(new Map());
  const isAltCopyDrag = useRef(false);

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

      const errorNodeNames = new Set(renderErrors?.map((e) => e.nodeName));

      // Determine output type of connection being dragged (for port feedback)
      const dragOutputType = creatingConnection
        ? children.find((n) => n.name === creatingConnection.fromNode)?.output_type ?? null
        : null;

      for (const node of children) {
        const isSelected = selectedNodes.has(node.name);
        const isRendered = renderedChild === node.name;
        drawNode(ctx, node, isSelected, isRendered, worldToScreen, pz.zoom, hoveredPort, errorNodeNames.has(node.name), dragOutputType);
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
    [pz, children, connections, selectedNodes, renderedChild, worldToScreen, creatingConnection, rubberBand, hoveredPort, renderErrors],
  );

  const { canvasRef, requestRender } = useCanvasRenderer(draw);

  // Re-render when SVG icons finish loading
  useEffect(() => {
    const handleLoad = () => requestRender();
    nodeIconCache.onLoad(handleLoad);
    return () => nodeIconCache.removeOnLoad(handleLoad);
  }, [requestRender]);

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
          return { node, portType: node.output_type };
        }
      }
      return null;
    },
    [children, worldToScreen, pz.zoom],
  );

  // Hit test for input ports (top of node)
  // When isConnecting=true, expand hit areas to span the entire node height
  const findInputPortAt = useCallback(
    (sx: number, sy: number, isConnecting = false): { node: Node; portName: string; portType: PortType } | null => {
      for (let i = children.length - 1; i >= 0; i--) {
        const node = children[i];
        const rect = nodeScreenRect(node, worldToScreen, pz.zoom);
        const z = pz.zoom;
        for (let j = 0; j < node.inputs.length; j++) {
          let px: number, py: number, pw: number, ph: number;
          if (isConnecting) {
            // Expanded hit area: width includes margin, height spans port + entire node body
            px = rect.x + (PORT_WIDTH + PORT_SPACING) * z * j - (PORT_MARGIN / 2) * z;
            py = rect.y - PORT_HEIGHT * z;
            pw = (PORT_WIDTH + PORT_MARGIN) * z;
            ph = (PORT_HEIGHT + NODE_HEIGHT) * z;
          } else {
            // Normal hit area with basic affordance
            px = rect.x + (PORT_WIDTH + PORT_SPACING) * z * j;
            py = rect.y - PORT_HEIGHT * z;
            pw = PORT_WIDTH * z;
            ph = PORT_HEIGHT * z;
          }
          if (
            sx >= px - PORT_HIT_TOLERANCE &&
            sx <= px + pw + PORT_HIT_TOLERANCE &&
            sy >= py - PORT_HIT_TOLERANCE &&
            sy <= py + ph + PORT_HIT_TOLERANCE
          ) {
            return { node, portName: node.inputs[j].name, portType: node.inputs[j].port_type };
          }
        }
      }
      return null;
    },
    [children, worldToScreen, pz.zoom],
  );

  const handlePointerDown = useCallback(
    (e: React.PointerEvent<HTMLCanvasElement>) => {
      if (e.button === 1 || (e.button === 0 && (e.altKey || panZoom.isSpaceDown))) {
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
          mouseX: sx,
          mouseY: sy,
          isReroute: false,
        });
        e.currentTarget.setPointerCapture(e.pointerId);
        return;
      }

      // Check input port hit for disconnect-and-reroute
      const inputPort = findInputPortAt(sx, sy);
      if (inputPort) {
        const conn = connections.find(
          (c) => c.input_node === inputPort.node.name && c.input_port === inputPort.portName,
        );
        if (conn) {
          pushSnapshot(library);
          removeConnection('root', conn.input_node, conn.input_port);
          setCreatingConnection({
            fromNode: conn.output_node,
            mouseX: sx,
            mouseY: sy,
            isReroute: true,
          });
          e.currentTarget.setPointerCapture(e.pointerId);
          return;
        }
      }

      const node = hitTestNode(sx, sy);

      if (node) {
        if (e.shiftKey || e.metaKey) {
          toggleNode(node.name);
        } else if (!selectedNodes.has(node.name)) {
          selectNode(node.name);
        }

        // Record start positions for all selected nodes (multi-drag)
        const world = screenToWorld(sx, sy);
        dragStartWorld.current = world;
        const positions = new Map<string, { x: number; y: number }>();
        // Use the current selection, but if the clicked node wasn't in it we just selected it
        const effectiveSelection = (e.shiftKey || e.metaKey)
          ? useStore.getState().selectedNodes
          : selectedNodes.has(node.name) ? selectedNodes : new Set([node.name]);
        for (const name of effectiveSelection) {
          const n = children.find((c) => c.name === name);
          if (n) {
            positions.set(name, { x: n.position.x, y: n.position.y });
          }
        }
        dragStartPositions.current = positions;

        pushSnapshot(library);

        // Alt+drag: clone selected nodes and drag the copies
        if (e.altKey) {
          const usedNames = new Set(children.map((c) => c.name));
          const nameMap = new Map<string, string>();

          // Phase 1: Build old->new name mapping
          for (const oldName of effectiveSelection) {
            const prefix = extractNamePrefix(oldName);
            const newName = generateUniqueName(prefix, usedNames);
            usedNames.add(newName);
            nameMap.set(oldName, newName);
          }

          // Phase 2: Clone nodes with new names
          for (const oldName of effectiveSelection) {
            const original = children.find((c) => c.name === oldName);
            if (original) {
              const cloned = JSON.parse(JSON.stringify(original));
              cloned.name = nameMap.get(oldName)!;
              addNode('root', cloned);
            }
          }

          // Phase 3: Duplicate connections (internal + incoming)
          for (const conn of connections) {
            const outInSelection = nameMap.has(conn.output_node);
            const inInSelection = nameMap.has(conn.input_node);
            if (outInSelection && inInSelection) {
              addConnection('root', {
                output_node: nameMap.get(conn.output_node)!,
                input_node: nameMap.get(conn.input_node)!,
                input_port: conn.input_port,
              });
            } else if (inInSelection) {
              addConnection('root', {
                output_node: conn.output_node,
                input_node: nameMap.get(conn.input_node)!,
                input_port: conn.input_port,
              });
            }
          }

          // Phase 4: Update selection and drag positions to clones
          selectNodes(Array.from(nameMap.values()));
          const clonePositions = new Map<string, { x: number; y: number }>();
          for (const [oldName, newName] of nameMap) {
            const origPos = positions.get(oldName);
            if (origPos) clonePositions.set(newName, { ...origPos });
          }
          dragStartPositions.current = clonePositions;
          isAltCopyDrag.current = true;
        } else {
          isAltCopyDrag.current = false;
        }

        setIsDragging(true);
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
    [handlers, hitTestNode, findOutputPortAt, findInputPortAt, selectNode, selectNodes, toggleNode, clearSelection, selectedNodes, screenToWorld, connections, pushSnapshot, library, removeConnection, children, panZoom.isSpaceDown, addNode, addConnection],
  );

  const handlePointerMove = useCallback(
    (e: React.PointerEvent<HTMLCanvasElement>) => {
      handlers.onPointerMove(e);

      const canvasRect = e.currentTarget.getBoundingClientRect();
      const sx = e.clientX - canvasRect.left;
      const sy = e.clientY - canvasRect.top;

      if (isDragging) {
        const world = screenToWorld(sx, sy);
        const deltaGridX = Math.round((world.x - dragStartWorld.current.x) / GRID_SIZE);
        const deltaGridY = Math.round((world.y - dragStartWorld.current.y) / GRID_SIZE);
        for (const [name, origPos] of dragStartPositions.current) {
          setNodePosition(name, {
            x: origPos.x + deltaGridX,
            y: origPos.y + deltaGridY,
          });
        }
        requestRender();
      } else if (creatingConnection) {
        setCreatingConnection((prev) =>
          prev ? { ...prev, mouseX: sx, mouseY: sy } : null,
        );

        // Port hover detection during connection drag (with expanded hit areas)
        const inputPort = findInputPortAt(sx, sy, true);
        if (inputPort) {
          setHoveredPort({
            nodeName: inputPort.node.name,
            portName: inputPort.portName,
            portType: inputPort.portType,
            screenX: sx,
            screenY: sy,
          });
        } else if (hoveredPort) {
          setHoveredPort(null);
        }

        requestRender();
      } else if (rubberBand) {
        setRubberBand((prev) =>
          prev ? { ...prev, currentX: sx, currentY: sy } : null,
        );
        requestRender();
      } else {
        // Port hover detection (idle)
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
    [handlers, isDragging, creatingConnection, rubberBand, screenToWorld, setNodePosition, requestRender, findInputPortAt, findOutputPortAt, hoveredPort],
  );

  const handlePointerUp = useCallback(
    (e: React.PointerEvent<HTMLCanvasElement>) => {
      handlers.onPointerUp(e);

      if (creatingConnection) {
        const rect = e.currentTarget.getBoundingClientRect();
        const sx = e.clientX - rect.left;
        const sy = e.clientY - rect.top;
        // Use expanded hit areas for dropping connections
        const inputPort = findInputPortAt(sx, sy, true);
        if (inputPort && inputPort.node.name !== creatingConnection.fromNode) {
          pushSnapshot(library);
          addConnection('root', {
            output_node: creatingConnection.fromNode,
            input_node: inputPort.node.name,
            input_port: inputPort.portName,
          });
        } else if (!inputPort && !creatingConnection.isReroute) {
          // Dropped on empty space — check if we're over any node
          const node = hitTestNode(sx, sy);
          if (!node) {
            // Open filtered node dialog at drop position
            const world = screenToWorld(sx, sy);
            const gridX = Math.round(world.x / GRID_SIZE);
            const gridY = Math.round(world.y / GRID_SIZE);
            setNodeDialogPosition({ x: gridX, y: gridY });

            const fromNode = children.find((n) => n.name === creatingConnection.fromNode);
            if (fromNode) {
              setPendingConnection({
                fromNode: creatingConnection.fromNode,
                outputType: fromNode.output_type,
              });
            }
            setNodeDialogVisible(true);
          }
        }
        setCreatingConnection(null);
        setHoveredPort(null);
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

      if (isDragging) {
        // Snap all dragged nodes to grid (positions are already grid-snapped during drag)
        setIsDragging(false);
      }
    },
    [handlers, creatingConnection, rubberBand, isDragging, findInputPortAt, addConnection, children, worldToScreen, pz.zoom, selectNodes, requestRender, pushSnapshot, library, hitTestNode, screenToWorld, setNodeDialogPosition, setPendingConnection, setNodeDialogVisible],
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
        const world = screenToWorld(sx, sy);
        const gridX = Math.round(world.x / GRID_SIZE);
        const gridY = Math.round(world.y / GRID_SIZE);
        setNodeDialogPosition({ x: gridX, y: gridY });
        setNodeDialogVisible(true);
      }
    },
    [hitTestNode, setNodeDialogVisible, setRenderedChild, screenToWorld, setNodeDialogPosition],
  );

  return (
    <div style={{ position: 'relative', width: '100%', height: '100%' }}>
      <canvas
        ref={canvasRef}
        className="w-full h-full block"
        style={{ cursor: panZoom.isPanning ? 'grabbing' : panZoom.isSpaceDown ? 'grab' : isDragging ? 'move' : creatingConnection ? 'crosshair' : 'default' }}
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
