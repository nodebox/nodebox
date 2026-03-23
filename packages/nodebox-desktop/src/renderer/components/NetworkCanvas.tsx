import React, { useCallback, useEffect, useRef, useState } from 'react';
import { useStore } from '../state/store';
import { usePanZoom } from '../hooks/usePanZoom';
import { useCanvasRenderer } from '../hooks/useCanvasRenderer';
import { isCompatible } from 'nodebox-core';
import type { Node, Port } from 'nodebox-core';
import {
  NETWORK_BACKGROUND, GRID_LINE_COLOR, ZINC_50, FONT_SIZE_SMALL,
  NODE_BODY_GEOMETRY, NODE_BODY_INT, NODE_BODY_FLOAT, NODE_BODY_STRING,
  NODE_BODY_BOOLEAN, NODE_BODY_POINT, NODE_BODY_COLOR, NODE_BODY_LIST,
  NODE_BODY_DATA, NODE_BODY_DEFAULT,
  PORT_COLOR_INT, PORT_COLOR_FLOAT, PORT_COLOR_STRING, PORT_COLOR_BOOLEAN,
  PORT_COLOR_POINT, PORT_COLOR_COLOR, PORT_COLOR_GEOMETRY, PORT_COLOR_LIST,
  PORT_COLOR_DATA,
  VIOLET_400, ERROR_RED,
} from '../theme/tokens';

const GRID_SIZE = 48;
const NODE_WIDTH = 128;
const NODE_HEIGHT = 32;
const NODE_PADDING = 4;
const NODE_ICON_SIZE = 24;
const PORT_WIDTH = 12;
const PORT_HEIGHT = 4;
const PORT_SPACING = 8;

// Icon cache: loads SVG icons and pre-renders them as white-tinted images
const iconCache = new Map<string, OffscreenCanvas | null>();
const iconListeners: (() => void)[] = [];

function ensureIcon(name: string): void {
  if (iconCache.has(name)) return;
  iconCache.set(name, null); // mark loading
  const img = new Image();
  img.onload = () => {
    const size = 32;
    const oc = new OffscreenCanvas(size, size);
    const octx = oc.getContext('2d')!;
    octx.drawImage(img, 0, 0, size, size);
    // Tint to white using source-atop compositing
    octx.globalCompositeOperation = 'source-atop';
    octx.fillStyle = 'white';
    octx.fillRect(0, 0, size, size);
    iconCache.set(name, oc);
    for (const cb of iconListeners) cb();
  };
  img.onerror = () => { iconCache.set(name, null); };
  img.src = `/icons/corevector/${name}.svg`;
}

function drawIcon(ctx: CanvasRenderingContext2D, name: string, x: number, y: number, size: number): void {
  const oc = iconCache.get(name);
  if (oc) ctx.drawImage(oc, x, y, size, size);
}

// Map function name to icon name
function iconNameForNode(node: Node): string {
  const fn = node.function ?? '';
  // "corevector/rect" → "rect", "math/add" → "add"
  const slash = fn.lastIndexOf('/');
  const name = slash >= 0 ? fn.substring(slash + 1) : fn;
  // Some mappings
  switch (name) {
    case 'lineAngle': return 'line_angle';
    case 'quadCurve': return 'quad_curve';
    case 'makePoint': return 'make_point';
    case 'pointOnPath': return 'point_on_path';
    case 'shapeOnPath': return 'shape_on_path';
    case 'textOnPath': return 'text_on_path';
    case 'fitTo': return 'fit_to';
    case 'roundSegments': return 'edit';
    case 'deletePaths': return 'delete';
    case 'sortShapes': return 'sort';
    case 'doNothing': return 'null';
    default: return name;
  }
}

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

function visiblePorts(node: Node): Port[] {
  return node.inputs.filter((p: Port) => p.type !== 'context');
}

interface ConnectionDrag {
  fromNode: string;
  outputType: string;
  startX: number;
  startY: number;
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
  const addConnectionToNetwork = useStore((s) => s.addConnectionToNetwork);
  const setCurrentNetworkPath = useStore((s) => s.setCurrentNetworkPath);
  const setNodeSelectionDialogOpen = useStore((s) => s.setNodeSelectionDialogOpen);
  const setNodeDialogPosition = useStore((s) => s.setNodeDialogPosition);
  const setPendingConnection = useStore((s) => s.setPendingConnection);
  const pushHistory = useStore((s) => s.pushHistory);

  // Scroll-to-zoom like viewer
  const panZoom = usePanZoom(undefined, undefined, { scrollToZoom: true });
  const { state: pz, handlers } = panZoom;

  const [dragNode, setDragNode] = useState<string | null>(null);
  const [connDrag, setConnDrag] = useState<ConnectionDrag | null>(null);
  const [mouseWorld, setMouseWorld] = useState<{ x: number; y: number } | null>(null);
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

  // Hit test: which node is at world position?
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

  // Hit test: is world position on a node's output port?
  const hitOutputPort = useCallback((wx: number, wy: number, network: Node): Node | null => {
    for (const child of network.children) {
      const nx = child.position.x * GRID_SIZE;
      const ny = child.position.y * GRID_SIZE + NODE_HEIGHT;
      // Output port is at left side, below node
      if (wx >= nx && wx <= nx + PORT_WIDTH && wy >= ny && wy <= ny + PORT_HEIGHT + 4) {
        return child;
      }
    }
    return null;
  }, []);

  // Hit test: is world position on a node's input port? Returns [node, port] or null
  const hitInputPort = useCallback((wx: number, wy: number, network: Node): [Node, Port] | null => {
    for (const child of network.children) {
      const ports = visiblePorts(child);
      const nx = child.position.x * GRID_SIZE;
      const ny = child.position.y * GRID_SIZE - PORT_HEIGHT;
      for (let pi = 0; pi < ports.length; pi++) {
        const px = nx + pi * (PORT_WIDTH + PORT_SPACING);
        // Enlarged hit area during connection drag
        const hitH = connDrag ? NODE_HEIGHT + PORT_HEIGHT : PORT_HEIGHT + 4;
        const hitY = connDrag ? ny : ny;
        if (wx >= px - 2 && wx <= px + PORT_WIDTH + 2 && wy >= hitY && wy <= hitY + hitH) {
          return [child, ports[pi]];
        }
      }
    }
    return null;
  }, [connDrag]);

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
        // Output from left side bottom
        const fx = fromNode.position.x * GRID_SIZE + PORT_WIDTH / 2;
        const fy = fromNode.position.y * GRID_SIZE + NODE_HEIGHT + PORT_HEIGHT / 2;
        // Input port position (left-aligned)
        const toPortIdx = visiblePorts(toNode).findIndex((p: Port) => p.name === conn.inputPort);
        const tx = toNode.position.x * GRID_SIZE + (toPortIdx >= 0 ? toPortIdx * (PORT_WIDTH + PORT_SPACING) : 0) + PORT_WIDTH / 2;
        const ty = toNode.position.y * GRID_SIZE - PORT_HEIGHT / 2;

        const toPort = toNode.inputs.find((p: Port) => p.name === conn.inputPort);
        ctx.strokeStyle = toPort ? portColor(toPort.type) : PORT_COLOR_GEOMETRY;
        ctx.lineWidth = 2 / pz.zoom;
        ctx.beginPath();
        ctx.moveTo(fx, fy);
        const dy = Math.abs(ty - fy);
        const cp = Math.max(30, dy * 0.4);
        ctx.bezierCurveTo(fx, fy + cp, tx, ty - cp, tx, ty);
        ctx.stroke();
      }

      // Connection drag wire
      if (connDrag && mouseWorld) {
        const fromNode = network.children.find((c: Node) => c.name === connDrag.fromNode);
        if (fromNode) {
          const fx = fromNode.position.x * GRID_SIZE + PORT_WIDTH / 2;
          const fy = fromNode.position.y * GRID_SIZE + NODE_HEIGHT + PORT_HEIGHT / 2;
          ctx.strokeStyle = portColor(connDrag.outputType);
          ctx.lineWidth = 2 / pz.zoom;
          ctx.setLineDash([4 / pz.zoom, 4 / pz.zoom]);
          ctx.beginPath();
          ctx.moveTo(fx, fy);
          const dy = Math.abs(mouseWorld.y - fy);
          const cp = Math.max(30, dy * 0.4);
          ctx.bezierCurveTo(fx, fy + cp, mouseWorld.x, mouseWorld.y - cp, mouseWorld.x, mouseWorld.y);
          ctx.stroke();
          ctx.setLineDash([]);
        }
      }

      // Nodes
      for (const child of network.children) {
        const x = child.position.x * GRID_SIZE;
        const y = child.position.y * GRID_SIZE;
        const isSelected = selectedNodes.includes(child.name);
        const isRendered = network.renderedChild === child.name;

        // Selection ring (white border, 2px inset body)
        const bodyColor = nodeBodyColor(child.outputType);
        if (isSelected) {
          ctx.fillStyle = '#ffffff';
          ctx.fillRect(x, y, NODE_WIDTH, NODE_HEIGHT);
          ctx.fillStyle = bodyColor;
          ctx.fillRect(x + 2, y + 2, NODE_WIDTH - 4, NODE_HEIGHT - 4);
        } else {
          ctx.fillStyle = bodyColor;
          ctx.fillRect(x, y, NODE_WIDTH, NODE_HEIGHT);
        }

        // Rendered indicator: white triangle bottom-right
        if (isRendered) {
          ctx.fillStyle = '#ffffff';
          ctx.beginPath();
          ctx.moveTo(x + NODE_WIDTH - 2, y + NODE_HEIGHT - 20);
          ctx.lineTo(x + NODE_WIDTH - 2, y + NODE_HEIGHT - 2);
          ctx.lineTo(x + NODE_WIDTH - 20, y + NODE_HEIGHT - 2);
          ctx.fill();
        }

        // Icon
        const icoName = iconNameForNode(child);
        ensureIcon(icoName);
        drawIcon(ctx, icoName, x + NODE_PADDING, y + NODE_PADDING, NODE_ICON_SIZE);

        // Name (after icon)
        ctx.fillStyle = ZINC_50;
        ctx.font = `${FONT_SIZE_SMALL}px -apple-system, sans-serif`;
        ctx.textAlign = 'left';
        ctx.textBaseline = 'middle';
        ctx.fillText(child.name, x + NODE_ICON_SIZE + NODE_PADDING * 2, y + NODE_HEIGHT / 2);

        // Input ports: LEFT-ALIGNED, with type compatibility feedback during drag
        const ports = visiblePorts(child);
        for (let pi = 0; pi < ports.length; pi++) {
          const port = ports[pi];
          const px = x + pi * (PORT_WIDTH + PORT_SPACING);
          let ph = PORT_HEIGHT;
          let color = portColor(port.type);

          if (connDrag) {
            const compatible = isCompatible(connDrag.outputType, port.type);
            if (!compatible) {
              ph = 1; // incompatible: tiny
            } else {
              ph = PORT_HEIGHT + 2; // compatible: taller
              // Check hover
              if (mouseWorld && mouseWorld.x >= px && mouseWorld.x <= px + PORT_WIDTH
                && mouseWorld.y >= y - ph - 2 && mouseWorld.y <= y + 4) {
                color = VIOLET_400; // hovered + compatible: accent
              }
            }
          }

          ctx.fillStyle = color;
          ctx.fillRect(px, y - ph, PORT_WIDTH, ph);
        }

        // Output port: LEFT side, below node
        ctx.fillStyle = portColor(child.outputType);
        ctx.fillRect(x, y + NODE_HEIGHT, PORT_WIDTH, PORT_HEIGHT);
      }

      ctx.restore();
    },
    [pz, findNetwork, selectedNodes, panZoom, connDrag, mouseWorld],
  );

  const { canvasRef, requestRender } = useCanvasRenderer(draw);

  // Re-render when icons finish loading
  useEffect(() => {
    const cb = () => requestRender();
    iconListeners.push(cb);
    return () => { const idx = iconListeners.indexOf(cb); if (idx >= 0) iconListeners.splice(idx, 1); };
  }, [requestRender]);

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

    // Check output port hit first (start connection drag)
    const outputHit = hitOutputPort(world.x, world.y, network);
    if (outputHit) {
      setConnDrag({
        fromNode: outputHit.name,
        outputType: outputHit.outputType,
        startX: world.x,
        startY: world.y,
      });
      setMouseWorld(world);
      e.currentTarget.setPointerCapture(e.pointerId);
      return;
    }

    // Check node body hit (select / start drag)
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
  }, [findNetwork, worldToNode, hitOutputPort, selectedNodes, selectNode, toggleNode, deselectAll, handlers, panZoom]);

  const handlePointerMove = useCallback((e: React.PointerEvent<HTMLCanvasElement>) => {
    if (panZoom.isPanning) { handlers.onPointerMove(e); return; }

    const rect = e.currentTarget.getBoundingClientRect();
    const world = panZoom.screenToWorld(e.clientX - rect.left, e.clientY - rect.top);

    // Connection drag: update mouse position for wire drawing
    if (connDrag) {
      setMouseWorld(world);
      return;
    }

    // Node drag
    if (dragNode) {
      const dx = (world.x - dragStartWorld.current.x) / GRID_SIZE;
      const dy = (world.y - dragStartWorld.current.y) / GRID_SIZE;
      setNodePositionAction(currentNetworkPath, dragNode, {
        x: dragStartNodePos.current.x + dx,
        y: dragStartNodePos.current.y + dy,
      });
      return;
    }

    // Track mouse for hover effects
    setMouseWorld(world);
  }, [dragNode, connDrag, panZoom, handlers, setNodePositionAction, currentNetworkPath]);

  const handlePointerUp = useCallback((e: React.PointerEvent<HTMLCanvasElement>) => {
    if (panZoom.isPanning) { handlers.onPointerUp(e); return; }

    // Connection drag release
    if (connDrag) {
      const network = findNetwork();
      if (network) {
        const rect = e.currentTarget.getBoundingClientRect();
        const world = panZoom.screenToWorld(e.clientX - rect.left, e.clientY - rect.top);
        const portHit = hitInputPort(world.x, world.y, network);

        if (portHit) {
          const [targetNode, targetPort] = portHit;
          if (isCompatible(connDrag.outputType, targetPort.type)) {
            pushHistory();
            addConnectionToNetwork(currentNetworkPath, {
              outputNode: connDrag.fromNode,
              inputNode: targetNode.name,
              inputPort: targetPort.name,
            });
          }
        } else {
          // Released on empty space → open dialog for compatible nodes
          const overNode = worldToNode(world.x, world.y, network);
          if (!overNode) {
            const gridX = Math.round(world.x / GRID_SIZE);
            const gridY = Math.round(world.y / GRID_SIZE);
            setNodeDialogPosition({ x: gridX, y: gridY });
            setPendingConnection({ fromNode: connDrag.fromNode, outputType: connDrag.outputType });
            setNodeSelectionDialogOpen(true);
          }
        }
      }
      setConnDrag(null);
      setMouseWorld(null);
      return;
    }

    setDragNode(null);
  }, [connDrag, panZoom, handlers, findNetwork, hitInputPort, worldToNode, addConnectionToNetwork, currentNetworkPath, pushHistory, setNodeDialogPosition, setPendingConnection, setNodeSelectionDialogOpen]);

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
      const gridX = Math.round(world.x / GRID_SIZE);
      const gridY = Math.round(world.y / GRID_SIZE);
      setNodeDialogPosition({ x: gridX, y: gridY });
      setNodeSelectionDialogOpen(true);
    }
  }, [findNetwork, worldToNode, panZoom, setCurrentNetworkPath, setRenderedChildAction, currentNetworkPath, setNodeDialogPosition, setNodeSelectionDialogOpen]);

  return (
    <canvas
      ref={canvasRef}
      style={{ width: '100%', height: '100%', display: 'block', cursor: panZoom.isPanning ? 'grabbing' : panZoom.isSpaceDown ? 'grab' : connDrag ? 'crosshair' : dragNode ? 'move' : 'default' }}
      onWheel={handlers.onWheel}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onDoubleClick={handleDoubleClick}
    />
  );
}
