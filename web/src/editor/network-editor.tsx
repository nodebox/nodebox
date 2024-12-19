import { useRef, useState, useEffect } from "react";
import { signal } from "@preact/signals-react";
import {
  ConnectionType,
  Context,
  Dimension,
  Inlet,
  InletToNodeConnection,
  Item,
  Network,
  NetworkItem,
  Node,
  NodeToNodeConnection,
  NodeToOutletConnection,
  Outlet,
  Point,
  Port,
  PortType,
  Sticky,
} from "@ndbx/runtime";
import {
  cx,
  project,
  activeItem,
  activeItemId,
  groupIntoNetwork,
  deleteItems,
  connectNodeToNode,
  connectInletToNode,
  connectNodeToOutlet,
  disconnect,
  setRenderedNode,
  createNodeModalVisible,
  createOutletModalVisible,
  createNetworkItemPosition,
  setItemPositions,
  setStickyText,
  activeNetworkItemIdMap,
  duplicateItems,
  pasteItemsFromClipboard,
  setStickySize,
  createNewSticky,
  stickyCanvasCacheMap,
  selectionIdsMap,
} from "./signals";
import Rect from "../lib/rect";
import Icon from "../components/icon";
import { Menu, MenuItem, MenuSeparator } from "../components/menu";
import { MenuItemClipboardAvailable } from "../components/menu-clipboard";
import { colorToHex } from "../lib/color-utils";

const GRID_STROKE = "#222";
const GRID_STROKE_ORIGIN = "#555";
const GRID_SIZE = 40;
const GRID_SNAP_SIZE = 10;
const GRID_SNAP_OFFSET_X = 0;
const GRID_SNAP_OFFSET_Y = 7; //GRID_SNAP_SIZE-PORT_HEIGHT
const NODE_FILL = "#3E4C80";
const NODE_FILL_CORRUPT = "#AA0000";
const NODE_TEXT = "#eee";
const HIGHLIGHT_STROKE = "#E8AA00";
const RENDERED_NODE_COLOR = "#EEF1FC";
const DRAG_SELECTION_FILL = "rgba(0, 0, 0, .2)";
const DRAG_SELECTION_STROKE = "#668";

const TOOLTIP_FONT = "12px Inter, sans-serif";

const PORT_WIDTH_NET = 10;
const PORT_PADDING_LR = 2;
const PORT_WIDTH = PORT_WIDTH_NET + 2 * PORT_PADDING_LR;
const PORT_HEIGHT = 3;
const PORT_HEIGHT_AFFORDANCE = PORT_HEIGHT * 2;
const PORT_MARGIN = 6;
const PORT_HIGHLIGHT_COLOR = "#E8AA00";
const NODE_WIDTH = 120;
const NODE_HEIGHT = 36;
const NODE_HEIGHT_WITHOUT_PORTS = NODE_HEIGHT - PORT_HEIGHT * 2;
const NODE_PADDING = 5;

const INLET_FILL = "#444";
const INLET_TEXT_FILL = "#888";
const INLET_WIDTH = NODE_WIDTH;
const INLET_HEIGHT = 32;
const INLET_HEIGHT_WITHOUT_PORT = INLET_HEIGHT - PORT_HEIGHT;
const OUTLET_FILL = "#444";
const OUTLET_TEXT_FILL = "#888";
const OUTLET_WIDTH = NODE_WIDTH;
const OUTLET_HEIGHT = NODE_HEIGHT;

const STICKY_RESIZER_NUB_SIZE = 15;
const STICKY_SNAP_OFFSET_X = 0;
const STICKY_SNAP_OFFSET_Y = 0;
const STICKY_MIN_WIDTH = NODE_WIDTH + 2 * (GRID_SNAP_OFFSET_X - STICKY_SNAP_OFFSET_X);
const STICKY_MIN_HEIGHT = NODE_HEIGHT_WITHOUT_PORTS;

const MIN_VIEW_SCALE = 0.15;
const MAX_VIEW_SCALE = 5;
const DEFAULT_VIEW_SCALE = 1;

interface Viewport {
  zoom: number;
  x: number;
  y: number;
}

const _viewportMap = signal<Map<string, Viewport>>(new Map());

interface NodeAndPort {
  node: Node;
  port: Port;
  portIndex: number;
}

enum DragMode {
  None = "NONE",
  Item = "ITEM",
  Selection = "SELECTION",
  NodeConnection = "NODE_CONNECTION",
  InletConnection = "INLET_CONNECTION",
  SelectRect = "SELECT_RECT",
  Pan = "PAN",
  Resize = "RESIZE",
}

type DragState =
  | {
      mode: DragMode.None;
    }
  | {
      mode: DragMode.Item;
      item: NetworkItem;
      position: Point;
    }
  | {
      mode: DragMode.Selection;
      items: NetworkItem[];
    }
  | {
      mode: DragMode.NodeConnection;
      node: Node;
      port: Port | undefined;
      portIndex: number;
    }
  | {
      mode: DragMode.InletConnection;
      inlet: Inlet;
    }
  | {
      mode: DragMode.SelectRect;
      startPoint: Point;
      rect: Rect;
      ids: string[];
    }
  | {
      mode: DragMode.Pan;
      start: Viewport;
    }
  | {
      mode: DragMode.Resize;
      item: Sticky;
      dimension: Dimension;
    };

// Snap a value "v" to increments of "d". Add an optional offset of o.
function snap(value: number, delta: number, offset: number): number {
  if (typeof offset !== "number") {
    offset = 0;
  }
  return offset + Math.floor(value / delta) * delta;
}

function* networkNodes(network: Network): Generator<Node> {
  for (const item of network.children) {
    if (item.type === "NODE") {
      yield item as Node;
    }
  }
}

function* networkStickies(network: Network): Generator<Sticky> {
  for (const item of network.children) {
    if (item.type === "STICKY") {
      yield item as Sticky;
    }
  }
}

function* networkInlets(network: Network): Generator<Inlet> {
  for (const item of network.children) {
    if (item.type === "INLET") {
      yield item as Inlet;
    }
  }
}

function* networkOutlets(network: Network): Generator<Outlet> {
  for (const item of network.children) {
    if (item.type === "OUTLET") {
      yield item as Outlet;
    }
  }
}

function nodeRect(node: Node): Rect {
  return new Rect(node.x, node.y, NODE_WIDTH, NODE_HEIGHT);
}

function inletRect(inlet: Inlet): Rect {
  return new Rect(inlet.x, inlet.y, INLET_WIDTH, INLET_HEIGHT);
}

function itemRect(item: NetworkItem): Rect {
  switch (item.type) {
    case "NODE":
      return new Rect(item.x, item.y, NODE_WIDTH, NODE_HEIGHT);
    case "INLET":
      return new Rect(item.x, item.y, INLET_WIDTH, INLET_HEIGHT);
    case "OUTLET":
      return new Rect(item.x, item.y, OUTLET_WIDTH, OUTLET_HEIGHT);
    case "STICKY": {
      const sticky = item as Sticky;
      const backgroundColor = sticky.backgroundColor;
      return new Rect(sticky.x, sticky.y, sticky.width, sticky.height, backgroundColor);
    }
    default:
      return new Rect(item.x, item.y, NODE_WIDTH, NODE_HEIGHT);
  }
}

function portX(portIndex: number): number {
  return (PORT_WIDTH + PORT_MARGIN) * portIndex;
}

function inputRect(node: Node, portIndex: number, isConnecting: boolean = false): Rect {
  const pX = portX(portIndex);
  const portWidth = !isConnecting ? PORT_WIDTH : PORT_WIDTH + PORT_MARGIN;
  const portHeight = !isConnecting ? PORT_HEIGHT : PORT_HEIGHT + NODE_HEIGHT;
  return new Rect(node.x + PORT_PADDING_LR + pX, node.y, portWidth - 2 * PORT_PADDING_LR, portHeight);
}

function outputRect(node: Node, portIndex: number, isConnecting: boolean = false): Rect {
  const pX = portX(portIndex);
  const pY = !isConnecting ? NODE_HEIGHT - PORT_HEIGHT : NODE_HEIGHT / 2;
  const portWidth = !isConnecting ? PORT_WIDTH : PORT_WIDTH + PORT_MARGIN;
  const portHeight = !isConnecting ? PORT_HEIGHT : PORT_HEIGHT + NODE_HEIGHT / 2;
  return new Rect(node.x + PORT_PADDING_LR + pX, node.y + pY, portWidth - 2 * PORT_PADDING_LR, portHeight);
}

function inletPortRect(inlet: Inlet): Rect {
  // FIXME: Make this bigger
  return new Rect(inlet.x, inlet.y + INLET_HEIGHT_WITHOUT_PORT, INLET_WIDTH, PORT_HEIGHT);
}

function outletPortRect(outlet: Outlet, isConnecting?: boolean): Rect {
  return new Rect(outlet.x, outlet.y, OUTLET_WIDTH, isConnecting ? OUTLET_HEIGHT : PORT_HEIGHT);
}

function stickyRect(sticky: Sticky): Rect {
  const bgColor = sticky.backgroundColor;
  return new Rect(sticky.x, sticky.y, sticky.width, sticky.height, bgColor);
}

function isSticky(item: NetworkItem): item is Sticky {
  return item.type === "STICKY";
}

function findItemByPosition(network: Network, x: number, y: number): NetworkItem | Node | Sticky | undefined {
  // This returns the *last* item. This is because items are drawn in order,
  // so the last item is the one on top.
  const allItems = network.children.filter(
    (item) =>
      itemRect(item).containsPoint(x + PORT_MARGIN / 2, y + PORT_MARGIN / 2) ||
      itemRect(item).containsPoint(x + PORT_MARGIN / 2, y - PORT_MARGIN / 2) ||
      itemRect(item).containsPoint(x - PORT_MARGIN / 2, y + PORT_MARGIN / 2) ||
      itemRect(item).containsPoint(x - PORT_MARGIN / 2, y - PORT_MARGIN / 2),
  );
  let items;
  // nodes prevail on stickies #387
  if (allItems.length == allItems.filter((item) => item.type === "STICKY").length) {
    items = allItems;
  } else {
    items = allItems.filter((item) => item.type != "STICKY");
  }

  const item = items[items.length - 1];
  return item;
}

// function findItemsInRange(
//   network: Network,
//   x1: number,
//   x2: number,
//   y1: number,
//   y2: number
// ): (NetworkItem | Node | Sticky)[] {
//   // Create a rectangular range with the given coordinates
//   const rect = {
//     left: Math.min(x1, x2),
//     right: Math.max(x1, x2),
//     top: Math.min(y1, y2),
//     bottom: Math.max(y1, y2),
//     containsPoint: function (x: number, y: number) {
//       return x >= this.left && x <= this.right && y >= this.top && y <= this.bottom;
//     },
//   };

//   // Filter the items that fall within the rectangular range
//   const allItems = network.children
//     .filter((item) => item.type != "STICKY")
//     .filter((item) => {
//       const itemBounds = itemRect(item);
//       return (
//         rect.containsPoint(itemBounds.x, itemBounds.y) || // top-left corner
//         rect.containsPoint(itemBounds.x + itemBounds.width, itemBounds.y) || // top-right corner
//         rect.containsPoint(itemBounds.x, itemBounds.y + itemBounds.height) || //  bottom-left corner
//         rect.containsPoint(itemBounds.x + itemBounds.width, itemBounds.y + itemBounds.height) // bottom-right corner
//       );
//     });

//   return allItems;
// }

function findStickyResizerByPosition(network: Network, x: number, y: number): NetworkItem | Node | Sticky | undefined {
  // This returns the *last* item. This is because items are drawn in order,
  // so the last item is the one on top.
  function isPointInTriangle(px: number, py: number, sticky: Sticky, delta: number) {
    const p0 = {
      x: sticky.x + sticky.width - delta,
      y: sticky.y + sticky.height,
    };
    const p1 = {
      x: sticky.x + sticky.width,
      y: sticky.y + sticky.height - delta,
    };

    const isBelowOrOnLine = (p1.y - p0.y) * (px - p0.x) <= (p1.x - p0.x) * (py - p0.y);
    const withinBoundingBox = py <= sticky.y + sticky.height && px <= sticky.x + sticky.width;

    return isBelowOrOnLine && withinBoundingBox;
  }

  const items = network.children.filter(
    (item) =>
      itemRect(item).containsPoint(x + PORT_MARGIN / 2, y + PORT_MARGIN / 2) ||
      itemRect(item).containsPoint(x + PORT_MARGIN / 2, y - PORT_MARGIN / 2) ||
      itemRect(item).containsPoint(x - PORT_MARGIN / 2, y + PORT_MARGIN / 2) ||
      itemRect(item).containsPoint(x - PORT_MARGIN / 2, y - PORT_MARGIN / 2),
  );
  const item = items[items.length - 1];
  if (item && isSticky(item)) {
    const sticky = item as Sticky;

    if (isPointInTriangle(x, y, sticky, STICKY_RESIZER_NUB_SIZE)) return item;
    else return undefined;
  }
  return undefined;
}

function findNodeByPosition(network: Network, x: number, y: number): Node | undefined {
  // This returns the *last* node. This is because nodes are drawn in order,
  // so the last node is the one on top.
  const nodes = network.children.filter((n) => {
    if (n.type !== "NODE") return false;
    const rect = nodeRect(n as Node);
    return (
      rect.containsPoint(x + PORT_MARGIN / 2, y + PORT_MARGIN / 2) ||
      rect.containsPoint(x + PORT_MARGIN / 2, y - PORT_MARGIN / 2) ||
      rect.containsPoint(x - PORT_MARGIN / 2, y + PORT_MARGIN / 2) ||
      rect.containsPoint(x - PORT_MARGIN / 2, y - PORT_MARGIN / 2)
    );
  });
  return nodes[nodes.length - 1] as Node;
}

/**
 * Returns an object that contains the selected node and port.
 * @param {Context} cx The context object.
 * @param {Network} network The network object.
 * @param {number} x The X position.
 * @param {number} y The Y position.
 * @param {boolean} isConnecting Whether or not we are connecting a node. When connecting the inputs becomes "larger".
 * @returns {NodeAndPort|null} An object containing the node and port, or null.
 */
function findInputByPosition(
  cx: Context,
  network: Network,
  x: number,
  y: number,
  isConnecting: boolean = false,
): NodeAndPort | null {
  for (const node of networkNodes(network)) {
    // Check if the X/Y position is in the general area of the node. If it is not, we can skip it.
    const r = new Rect(
      node.x - PORT_HEIGHT_AFFORDANCE / 2,
      node.y - PORT_HEIGHT_AFFORDANCE / 2,
      NODE_WIDTH + PORT_HEIGHT_AFFORDANCE,
      isConnecting ? NODE_HEIGHT : PORT_HEIGHT_AFFORDANCE,
    );
    if (
      !(
        r.containsPoint(x - PORT_MARGIN / 2, y - PORT_MARGIN / 2) ||
        r.containsPoint(x - PORT_MARGIN / 2, y + PORT_MARGIN / 2) ||
        r.containsPoint(x + PORT_MARGIN / 2, y - PORT_MARGIN / 2) ||
        r.containsPoint(x + PORT_MARGIN / 2, y + PORT_MARGIN / 2)
      )
    )
      continue;
    // In order to know which ports the node has, we need to get the node function.
    const fn = cx.lookupItemByName(node.fn);
    // Calculating the parameter index based on the relative X position.
    const portIndex = Math.floor((x - node.x + PORT_MARGIN / 2) / (PORT_WIDTH + PORT_MARGIN));
    const port = fn?.inputPorts[portIndex];
    if (!port) continue;
    return { node, port, portIndex };
  }
  return null;
}

/**
 * Returns an object that contains the selected node and port.
 * @param {Context} cx The context object.
 * @param {Network} network The network object.
 * @param {number} x The X position.
 * @param {number} y The Y position.
 * @returns {NodeAndPort|null} An object containing the node and port, or null.
 */
function findOutputByPosition(cx: Context, network: Network, x: number, y: number): NodeAndPort | null {
  for (const node of networkNodes(network)) {
    const r = new Rect(node.x, node.y + NODE_HEIGHT - PORT_HEIGHT_AFFORDANCE, NODE_WIDTH, PORT_HEIGHT_AFFORDANCE);
    if (
      !(
        r.containsPoint(x - PORT_MARGIN / 2, y - PORT_MARGIN / 2) ||
        r.containsPoint(x - PORT_MARGIN / 2, y + PORT_MARGIN / 2) ||
        r.containsPoint(x + PORT_MARGIN / 2, y - PORT_MARGIN / 2) ||
        r.containsPoint(x + PORT_MARGIN / 2, y + PORT_MARGIN / 2)
      )
    )
      continue;
    const fn = cx.lookupItemByName(node.fn);
    const portIndex = Math.floor((x - node.x + PORT_MARGIN / 2) / (PORT_WIDTH + PORT_MARGIN));
    const port = fn?.outputPorts[portIndex];
    if (!port) continue;
    return { node, port, portIndex };
  }
  return null;
}

function findInletPortByPosition(network: Network, x: number, y: number): Inlet | undefined {
  for (const inlet of networkInlets(network)) {
    const r = inletPortRect(inlet);
    if (
      !(
        r.containsPoint(x - PORT_MARGIN / 2, y - PORT_MARGIN / 2) ||
        r.containsPoint(x - PORT_MARGIN / 2, y + PORT_MARGIN / 2) ||
        r.containsPoint(x + PORT_MARGIN / 2, y - PORT_MARGIN / 2) ||
        r.containsPoint(x + PORT_MARGIN / 2, y + PORT_MARGIN / 2)
      )
    )
      continue;
    return inlet;
  }
}

function findOutletByPosition(network: Network, x: number, y: number, isConnecting?: boolean): Outlet | undefined {
  for (const outlet of networkOutlets(network)) {
    const r = outletPortRect(outlet, isConnecting);
    if (
      !(
        r.containsPoint(x - PORT_MARGIN / 2, y - PORT_MARGIN / 2) ||
        r.containsPoint(x - PORT_MARGIN / 2, y + PORT_MARGIN / 2) ||
        r.containsPoint(x + PORT_MARGIN / 2, y - PORT_MARGIN / 2) ||
        r.containsPoint(x + PORT_MARGIN / 2, y + PORT_MARGIN / 2)
      )
    )
      continue;
    return outlet;
  }
}

function colorForPortType(portType: PortType) {
  switch (portType) {
    case PortType.Invalid:
      return "#aa0000";
    case PortType.Shape:
      return "#edf8b1";
    case PortType.Table:
      return "#7fcdbb";
    case PortType.Spec:
      return "#2c7fb8";
    default:
      return "red";
  }
}

function fillTextWrapped(
  ctx: CanvasRenderingContext2D,
  text: string,
  x: number,
  y: number,
  maxWidth: number,
  lineHeight: number,
): void {
  const words = text.split(/\s/g);
  let line = "";
  for (let i = 0; i < words.length; i += 1) {
    const testLine = line + words[i] + " ";
    const metrics = ctx.measureText(testLine);
    const testWidth = metrics.width;
    if (testWidth > maxWidth && i > 0) {
      ctx.fillText(line, x, y);
      line = words[i] + " ";
      y += lineHeight;
    } else {
      line = testLine;
    }
  }
  ctx.fillText(line, x, y);
}

//// Viewport ///////////////////////////////////////////////////////////////

function rectToView(r: Rect, viewport: Viewport): Rect {
  return new Rect(
    r.x * viewport.zoom + viewport.x,
    r.y * viewport.zoom + viewport.y,
    r.width * viewport.zoom,
    r.height * viewport.zoom,
  );
}

//// Drawing Functions //////////////////////////////////////////////////////

function zoomedFont(viewport: Viewport) {
  return `${12 * viewport.zoom}px Inter, sans-serif`;
}

function drawGrid(ctx: CanvasRenderingContext2D, viewport: Viewport) {
  const { width, height } = ctx.canvas;
  const gridSpacing = GRID_SIZE * viewport.zoom;
  ctx.save();
  if (gridSpacing >= 10) {
    ctx.strokeStyle = GRID_STROKE;
    ctx.lineWidth = 1;
    ctx.beginPath();

    for (let x = viewport.x % gridSpacing; x < width; x += gridSpacing) {
      ctx.moveTo(x, 0);
      ctx.lineTo(x, height);
    }
    for (let y = viewport.y % gridSpacing; y < height; y += gridSpacing) {
      ctx.moveTo(0, y);
      ctx.lineTo(width, y);
    }
    ctx.stroke();
  }

  // Draw a corner at the origin to know where 0,0 is.
  const originX = viewport.x;
  const originY = viewport.y;
  ctx.lineWidth = Math.min(1, Math.max(2, viewport.zoom));
  const originCornerSize = (GRID_SIZE / 3) * viewport.zoom;
  ctx.strokeStyle = GRID_STROKE_ORIGIN;
  ctx.beginPath();
  ctx.moveTo(originX, originCornerSize + originY);
  ctx.lineTo(originX, originY);
  ctx.lineTo(originCornerSize + originX, originY);
  ctx.stroke();

  ctx.restore();
}

function drawNodeToNodeConnection(
  ctx: CanvasRenderingContext2D,
  viewport: Viewport,
  outputNode: Node,
  outputPortIndex: number,
  inputNode: Node,
  inputPortIndex: number,
  portType: PortType,
  activeItemId: string,
) {
  const x1 = outputNode.x + portX(outputPortIndex) + PORT_WIDTH / 2;
  const y1 = outputNode.y + NODE_HEIGHT - 1;
  const x2 = inputNode.x + portX(inputPortIndex) + PORT_WIDTH / 2;
  const y2 = inputNode.y + 1;
  ctx.save();
  ctx.strokeStyle = colorForPortType(portType);
  ctx.lineWidth = 2;
  if (activeItemId === outputNode.id || activeItemId === inputNode.id) {
    ctx.lineWidth = 4;
    ctx.shadowColor = ctx.strokeStyle;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 0;
    ctx.shadowBlur = 4;
  }
  drawConnectionLine(
    ctx,
    x1 * viewport.zoom + viewport.x,
    y1 * viewport.zoom + viewport.y,
    x2 * viewport.zoom + viewport.x,
    y2 * viewport.zoom + viewport.y,
  );
  ctx.restore();
}

function drawInletToNodeConnection(
  ctx: CanvasRenderingContext2D,
  viewport: Viewport,
  inlet: Inlet,
  inputNode: Node,
  inputPortIndex: number,
  portType: PortType,
  activeItemId: string,
) {
  const x1 = inlet.x + PORT_WIDTH / 2;
  const y1 = inlet.y + PORT_HEIGHT;
  const x2 = inputNode.x + portX(inputPortIndex) + PORT_WIDTH / 2;
  const y2 = inputNode.y;
  ctx.save();
  ctx.strokeStyle = colorForPortType(portType);
  ctx.lineWidth = 2;
  if (activeItemId === inlet.id || activeItemId === inputNode.id) {
    ctx.lineWidth = 4;
    ctx.shadowColor = ctx.strokeStyle;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 0;
    ctx.shadowBlur = 4;
  }
  drawConnectionLine(
    ctx,
    x1 * viewport.zoom + viewport.x,
    y1 * viewport.zoom + viewport.y,
    x2 * viewport.zoom + viewport.x,
    y2 * viewport.zoom + viewport.y,
  );
  ctx.restore();
}

function drawNodeToOutletConnection(
  ctx: CanvasRenderingContext2D,
  viewport: Viewport,
  outputNode: Node,
  outputPortIndex: number,
  outlet: Outlet,
  portType: PortType,
  activeItemId: string,
) {
  const x1 = outputNode.x + portX(outputPortIndex) + PORT_WIDTH / 2;
  const y1 = outputNode.y + NODE_HEIGHT;
  const x2 = outlet.x + NODE_WIDTH / 2;
  const y2 = outlet.y;
  ctx.save();
  ctx.strokeStyle = colorForPortType(portType);
  ctx.lineWidth = 2;
  if (activeItemId === outputNode.id || activeItemId === outlet.id) {
    ctx.lineWidth = 4;
    ctx.shadowColor = ctx.strokeStyle;
    ctx.shadowOffsetX = 0;
    ctx.shadowOffsetY = 0;
    ctx.shadowBlur = 4;
  }
  drawConnectionLine(
    ctx,
    x1 * viewport.zoom + viewport.x,
    y1 * viewport.zoom + viewport.y,
    x2 * viewport.zoom + viewport.x,
    y2 * viewport.zoom + viewport.y,
  );
  ctx.restore();
}

function drawSticky(ctx: CanvasRenderingContext2D, viewport: Viewport, sticky: Sticky, isSelected: boolean) {
  const scaledRect = rectToView(stickyRect(sticky), viewport);
  ctx.save();
  ctx.fillStyle = colorToHex(sticky.backgroundColor);
  ctx.fillRect(scaledRect.x, scaledRect.y, scaledRect.width, scaledRect.height);

  // Resize nub
  if (viewport.zoom > 0.25) {
    const sz = STICKY_RESIZER_NUB_SIZE * viewport.zoom;
    ctx.fillStyle = colorToHex(sticky.backgroundColor);
    ctx.beginPath();
    ctx.moveTo(scaledRect.x + scaledRect.width - sz, scaledRect.y + scaledRect.height);
    ctx.lineTo(scaledRect.x + scaledRect.width, scaledRect.y + scaledRect.height - sz);
    ctx.lineTo(scaledRect.x + scaledRect.width, scaledRect.y + scaledRect.height);
    ctx.fill();
  }

  // Border
  ctx.strokeStyle = colorToHex(sticky.backgroundColor);
  ctx.lineWidth = 3; //isSelected ? 4 : 2; // #387
  ctx.strokeRect(scaledRect.x, scaledRect.y, scaledRect.width, scaledRect.height);

  if (isSelected) {
    ctx.strokeStyle = colorToHex(sticky.fontColor);
    ctx.lineWidth = 1; // #387
    ctx.strokeRect(scaledRect.x, scaledRect.y, scaledRect.width, scaledRect.height);
  }

  // Text
  if (stickyCanvasCacheMap.has(sticky.id)) {
    ctx.drawImage(
      stickyCanvasCacheMap.get(sticky.id)!,
      scaledRect.x,
      scaledRect.y,
      scaledRect.width,
      scaledRect.height,
    );
  } else {
    const render = () => {
      const canvas = document.createElement("canvas");
      const dpi = Math.min(window.devicePixelRatio, 2);
      canvas.width = (sticky.width || 1) * dpi;
      canvas.height = (sticky.height || 1) * dpi;
      const stickyCtx = canvas.getContext("2d")!;
      stickyCtx.scale(dpi, dpi);
      stickyCtx.fillStyle = colorToHex(sticky.fontColor);
      stickyCtx.font = sticky.fontSize + "px Inter, sans-serif";
      fillTextWrapped(stickyCtx, sticky.text, 5, sticky.fontSize + 2, sticky.width - 10, sticky.fontSize);
      stickyCanvasCacheMap.set(sticky.id, canvas);
      ctx.drawImage(canvas, scaledRect.x, scaledRect.y, scaledRect.width, scaledRect.height);
    };
    render();
  }

  ctx.restore();
}

function drawConnectionLine(ctx: CanvasRenderingContext2D, x1: number, y1: number, x2: number, y2: number): void {
  const halfDy = Math.abs(y2 - y1) / 2.0;
  ctx.beginPath();
  ctx.moveTo(x1, y1);
  ctx.bezierCurveTo(x1, y1 + halfDy, x2, y2 - halfDy, x2, y2);
  ctx.stroke();
}

function drawNode(
  ctx: CanvasRenderingContext2D,
  viewport: Viewport,
  network: Network,
  node: Node,
  fn: Item | undefined,
  isSelected: boolean,
  mousePositionInView: Point = { x: 0, y: 0 },
  // mode: DragMode,
  dragStateRef: React.MutableRefObject<DragState>,
) {
  const mode = dragStateRef.current.mode;
  const nodeRect = rectToView(new Rect(node.x, node.y + PORT_HEIGHT, NODE_WIDTH, NODE_HEIGHT_WITHOUT_PORTS), viewport);
  // Draw background
  ctx.fillStyle = fn ? NODE_FILL : NODE_FILL_CORRUPT;
  ctx.fillRect(nodeRect.x, nodeRect.y, nodeRect.width, nodeRect.height);

  // Draw stroke
  if (isSelected) {
    ctx.strokeStyle = HIGHLIGHT_STROKE;
    ctx.lineWidth = 2;
    ctx.strokeRect(nodeRect.x + 1, nodeRect.y + 1, nodeRect.width - 2, nodeRect.height - 2);
  }

  // Draw rendered flag
  if (network.renderedNode === node.id) {
    const nw20 = (NODE_WIDTH - 20) * viewport.zoom;
    const nh20 = (NODE_HEIGHT - 26) * viewport.zoom;
    const nw2 = (NODE_WIDTH - 2) * viewport.zoom;
    const nh2 = (NODE_HEIGHT - 6 - 2) * viewport.zoom;
    ctx.fillStyle = RENDERED_NODE_COLOR;
    ctx.beginPath();
    ctx.moveTo(nodeRect.x + nw2, nodeRect.y + nh20);
    ctx.lineTo(nodeRect.x + nw2, nodeRect.y + nh2);
    ctx.lineTo(nodeRect.x + nw20, nodeRect.y + nh2);
    ctx.fill();
  }

  // Draw input ports
  fn &&
    fn.inputPorts.forEach((p: Port, index: number) => {
      const inputIsConnected = network.connections.find(
        (conn) =>
          (conn.type === ConnectionType.NodeToNode || conn.type === ConnectionType.InletToNode) &&
          conn.inPort === p.name &&
          conn.inNode === node.id,
      );
      const inRect = rectToView(inputRect(node, index), viewport);
      //border on mouse over (+-) XXX
      const inPort = inputRect(node, index);
      inPort.x -= PORT_MARGIN / 2;
      inPort.y -= PORT_MARGIN / 2;
      inPort.width += PORT_MARGIN;
      inPort.height += PORT_MARGIN;
      const inRectOver = rectToView(inPort, viewport);
      const samePortType =
        dragStateRef.current.mode === DragMode.NodeConnection && dragStateRef.current.port?.type === p.type;
      if (
        samePortType &&
        (mode === DragMode.NodeConnection || inputIsConnected) &&
        mousePositionInView.x > inRectOver.x &&
        mousePositionInView.x < inRectOver.x + inRectOver.width &&
        mousePositionInView.y > inRectOver.y &&
        mousePositionInView.y < inRectOver.y + inRectOver.height + (inputIsConnected ? 0 : nodeRect.height)
      ) {
        ctx.fillStyle = PORT_HIGHLIGHT_COLOR;
      } else {
        ctx.fillStyle = colorForPortType(p.type);
      }
      ctx.fillRect(inRect.x, inRect.y, inRect.width, inRect.height);
    });

  // Draw output ports
  fn &&
    fn.outputPorts.forEach((p: Port, index: number) => {
      const outRect = rectToView(outputRect(node, index), viewport);
      //border on mouse over (+-)
      const outPort = outputRect(node, index);
      outPort.x -= PORT_MARGIN / 2;
      outPort.y -= PORT_MARGIN / 2;
      outPort.width += PORT_MARGIN;
      outPort.height += PORT_MARGIN;
      const outRectOver = rectToView(outPort, viewport);
      if (
        mode === DragMode.None &&
        mousePositionInView.x > outRectOver.x &&
        mousePositionInView.x < outRectOver.x + outRectOver.width &&
        mousePositionInView.y > outRectOver.y &&
        mousePositionInView.y < outRectOver.y + outRectOver.height
      ) {
        ctx.fillStyle = PORT_HIGHLIGHT_COLOR;
      } else {
        ctx.fillStyle = colorForPortType(p.type);
      }
      ctx.fillRect(outRect.x, outRect.y, outRect.width, outRect.height);
    });

  // Draw label
  ctx.fillStyle = NODE_TEXT;
  if (viewport.zoom > 0.25) {
    ctx.font = zoomedFont(viewport);
    ctx.fillText(node.name, nodeRect.x + NODE_PADDING, nodeRect.y + (NODE_HEIGHT - 16) * viewport.zoom);
  }
}

function drawInlet(ctx: CanvasRenderingContext2D, viewport: Viewport, inlet: Inlet, isSelected: boolean) {
  const r = rectToView(new Rect(inlet.x, inlet.y, INLET_WIDTH, INLET_HEIGHT), viewport);

  // Draw background
  ctx.fillStyle = INLET_FILL;
  ctx.fillRect(r.x, r.y, r.width, r.height);

  // Draw stroke
  if (isSelected) {
    ctx.strokeStyle = HIGHLIGHT_STROKE;
    ctx.lineWidth = 2;
    ctx.strokeRect(r.x + 1, r.y + 1, r.width - 2, r.height - 2);
  }

  // Draw label
  ctx.fillStyle = INLET_TEXT_FILL;
  if (viewport.zoom > 0.25) {
    ctx.font = zoomedFont(viewport);
    ctx.fillText(inlet.portName, r.x + NODE_PADDING, r.y + (NODE_HEIGHT - 10) * viewport.zoom);
  }
}

function drawOutlet(ctx: CanvasRenderingContext2D, viewport: Viewport, outlet: Outlet, isSelected: boolean) {
  const r = rectToView(new Rect(outlet.x, outlet.y, OUTLET_WIDTH, OUTLET_HEIGHT), viewport);
  const bump = 8 * viewport.zoom;

  // Draw background
  ctx.fillStyle = OUTLET_FILL;
  ctx.beginPath();
  ctx.moveTo(r.x, r.y + bump);
  ctx.lineTo(r.x + r.width / 2, r.y);
  ctx.lineTo(r.x + r.width, r.y + bump);
  ctx.lineTo(r.x + r.width, r.y + r.height);
  ctx.lineTo(r.x, r.y + r.height);
  ctx.closePath();
  ctx.fill();

  // Draw stroke
  if (isSelected) {
    ctx.strokeStyle = HIGHLIGHT_STROKE;
    ctx.lineWidth = 2;
    ctx.stroke();
  }

  // Draw label
  ctx.fillStyle = OUTLET_TEXT_FILL;
  if (viewport.zoom > 0.25) {
    ctx.font = zoomedFont(viewport);
    ctx.fillText(outlet.portName, r.x + NODE_PADDING, r.y + (NODE_HEIGHT - 10) * viewport.zoom);
  }
}

function drawTransformedTooltip(ctx: CanvasRenderingContext2D, viewport: Viewport, x: number, y: number, text: string) {
  drawTooltip(ctx, x * viewport.zoom + viewport.x, y * viewport.zoom + viewport.y, text);
}

function drawTooltip(ctx: CanvasRenderingContext2D, x: number, y: number, text: string) {
  ctx.font = TOOLTIP_FONT;
  const textWidth = ctx.measureText(text).width;
  const r = new Rect(x + 5, y + 22, textWidth + 10, 20);
  ctx.shadowBlur = 10;
  ctx.shadowColor = "rgba(0, 0, 0, 0.2)";
  ctx.fillStyle = "#fcffa6";
  ctx.fillRect(r.x, r.y, r.width, r.height);
  ctx.shadowBlur = 0;
  ctx.shadowColor = "transparent";
  ctx.strokeStyle = "#ccc";
  ctx.lineWidth = 1;
  ctx.strokeRect(r.x, r.y, r.width, r.height);
  ctx.fillStyle = "#333";
  ctx.fillText(text, r.x + 5, r.y + 15);
}

export default function NetworkEditor() {
  const [version, setVersion] = useState(0);
  const mousePositionRef = useRef({ x: 0, y: 0 });
  const scaledMousePositionRef = useRef<Point>({ x: 0, y: 0 });
  const dragStateRef = useRef<DragState>({ mode: DragMode.None });
  const dragStartRef = useRef<Point>({ x: 0, y: 0 });
  const startedDraggingRef = useRef(false);
  const dragSelectionRef = useRef<NetworkItem[]>([]);
  const [contextMenu, setContextMenu] = useState<Point | null>(null);
  const currentSelection = getSelection(activeItem.value as Network);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    window.addEventListener("resize", handleResize);
    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);
    document.body.style.overflow = "hidden";
    const resizeObserver = new ResizeObserver(handleResize);
    let observedCanvas: HTMLCanvasElement | null = null;
    if (canvasRef.current) {
      observedCanvas = canvasRef.current;
      resizeObserver.observe(observedCanvas);
    }
    handleResize();
    return () => {
      window.removeEventListener("resize", handleResize);
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keyup", handleKeyUp);
      document.body.style.overflow = "auto";
      if (observedCanvas) {
        resizeObserver.unobserve(observedCanvas);
      }
    };
  }, []);

  useEffect(() => {
    draw();
  }, [version, project.value, activeItemId.value]);

  function forceUpdate() {
    setVersion((v) => v + 1);
  }

  function getSelection(network: Network): Set<string> {
    return selectionIdsMap.value.get(network.id) || new Set();
  }

  function setSelection(ids: string[]) {
    const network = activeItem.value as Network;
    const newSelectionMap = new Map(selectionIdsMap.value);
    newSelectionMap.set(network.id, new Set(ids));
    selectionIdsMap.value = newSelectionMap;
  }

  // function getActiveItem(): NetworkItem | undefined {
  //   const id = _activeItemIdMap.value.get(network.id);
  //   if (!id) return;
  //   return findNetworkItemById(network, id);
  // }

  function setActiveItem(item: NetworkItem) {
    if (!item) return;
    const network = activeItem.value as Network;
    const newMap = new Map(activeNetworkItemIdMap.value);
    newMap.set(network.id, item.id);
    activeNetworkItemIdMap.value = newMap;
  }

  function clearActiveItem() {
    const network = activeItem.value as Network;
    const newMap = new Map(activeNetworkItemIdMap.value);
    newMap.delete(network.id);
    activeNetworkItemIdMap.value = newMap;
  }

  function mouseOffset(e: React.MouseEvent | MouseEvent): Point {
    const rect = canvasRef.current!.getBoundingClientRect();
    return {
      x: e.clientX - rect.left,
      y: e.clientY - rect.top,
    };
  }

  function getViewport(): Viewport {
    const network = activeItem.value as Network;
    return _viewportMap.value.get(network.id) || { zoom: 1, x: 0, y: 0 };
  }

  function setViewport(zoom: number, x: number, y: number) {
    const network = activeItem.value as Network;
    const newViewportMap = new Map(_viewportMap.value);
    newViewportMap.set(network.id, { zoom, x, y });
    _viewportMap.value = newViewportMap;
  }

  function mousePositionInView(e: React.MouseEvent | MouseEvent): Point {
    const offset = mouseOffset(e);
    return canvasToView(offset);
  }

  function canvasToView(pt: Point): Point {
    const viewport = getViewport();
    return {
      x: (pt.x - viewport.x) / viewport.zoom,
      y: (pt.y - viewport.y) / viewport.zoom,
    };
  }

  // function viewToCanvas(pt: Point): Point {
  //   const viewport = getViewport();
  //   return {
  //     x: pt.x * viewport.zoom + viewport.x,
  //     y: pt.y * viewport.zoom + viewport.y,
  //   };
  // }

  function isSelected(item: NetworkItem): boolean {
    const network = activeItem.value as Network;
    const selection = getSelection(network);
    const activeItemId = activeNetworkItemIdMap.value.get(network.id) || "";
    const inSelection = selection.has(item.id);
    const inDragSelection = !!dragSelectionRef.current.find((item: NetworkItem) => item.id === item.id);
    const isActive = activeItemId === item.id;
    return inSelection || inDragSelection || isActive;
  }

  function draw(options: { hideGrid?: boolean } = {}) {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext("2d")!;
    ctx.clearRect(0, 0, canvas.width, canvas.height);
    const dpi = Math.min(window.devicePixelRatio, 2);
    const viewport = getViewport();

    const network = activeItem.value as Network;

    ctx.save();
    ctx.scale(dpi, dpi);

    if (!(options && options.hideGrid)) {
      drawGrid(ctx, viewport);
    }
    drawNetwork(ctx, viewport, network, mousePositionRef.current);
    drawCurrentConnection(ctx, viewport);
    drawSelectRect(ctx, viewport);
    drawPortTooltip(ctx, viewport, network);
    ctx.restore();
  }

  function drawNetwork(
    ctx: CanvasRenderingContext2D,
    viewport: Viewport,
    network: Network,
    mousePositionInView: Point,
  ) {
    const activeItemId = activeNetworkItemIdMap.value.get(network.id) || "";

    // Draw connections
    for (const connection of network.connections) {
      if (connection.type === ConnectionType.NodeToNode) {
        const conn = connection as NodeToNodeConnection;
        const outputNode = network.children.find((n) => n.id === conn.outNode);
        const inputNode = network.children.find((n) => n.id === conn.inNode);
        if (!(outputNode && outputNode.type === "NODE" && inputNode && inputNode.type === "NODE")) continue;
        const outputFn = cx.value!.lookupItemByName((outputNode as Node).fn);
        const inputFn = cx.value!.lookupItemByName((inputNode as Node).fn);
        const inputPortIndex = inputFn ? inputFn.inputPorts.findIndex((p) => p.name === conn.inPort) : 0;
        const outputPort = outputFn ? outputFn.outputPorts.find((p) => p.name === conn.outPort) : undefined;
        const outputPortIndex = outputFn ? outputFn.outputPorts.findIndex((p) => p.name === conn.outPort) : 0;
        if (inputPortIndex < 0 || outputPortIndex < 0) continue;
        if (outputNode)
          drawNodeToNodeConnection(
            ctx,
            viewport,
            outputNode as Node,
            outputPortIndex,
            inputNode as Node,
            inputPortIndex,
            inputFn && outputFn ? outputPort!.type : PortType.Invalid,
            activeItemId,
          );
      } else if (connection.type === ConnectionType.InletToNode) {
        const conn = connection as InletToNodeConnection;
        const inlet = network.children.find((n) => n.id === conn.inlet);
        const inputNode = network.children.find((n) => n.id === conn.inNode);
        if (!(inlet && inlet.type === "INLET" && inputNode && inputNode.type === "NODE")) continue;
        const networkPort = network.inputPorts.find((p) => p.name === (inlet as Inlet).portName)!;
        const inputFn = cx.value!.lookupItemByName((inputNode as Node).fn);
        const inputPortIndex = inputFn ? inputFn.inputPorts.findIndex((p) => p.name === conn.inPort) : 0;
        drawInletToNodeConnection(
          ctx,
          viewport,
          inlet as Inlet,
          inputNode as Node,
          inputPortIndex,
          networkPort.type,
          activeItemId,
        );
      } else if (connection.type === ConnectionType.NodeToOutlet) {
        const conn = connection as NodeToOutletConnection;
        const outputNode = network.children.find((n) => n.id === conn.outNode);
        const outlet = network.children.find((n) => n.type === "OUTLET" && (n as Outlet).portName === conn.outlet);
        if (!(outputNode && outputNode.type === "NODE" && outlet && outlet.type === "OUTLET")) continue;
        const outputFn = cx.value!.lookupItemByName((outputNode as Node).fn);
        const outputPort = outputFn ? outputFn.outputPorts.find((p) => p.name === conn.outPort)! : undefined;
        const outputPortIndex = outputFn ? outputFn.outputPorts.findIndex((p) => p.name === conn.outPort) : 0;
        if (outputPort)
          drawNodeToOutletConnection(
            ctx,
            viewport,
            outputNode as Node,
            outputPortIndex,
            outlet as Outlet,
            outputFn ? outputPort!.type : PortType.Invalid,
            activeItemId,
          );
      }
    }

    for (const sticky of networkStickies(network)) {
      drawSticky(ctx, viewport, sticky, isSelected(sticky));
    }

    for (const node of networkNodes(network)) {
      const fn = cx.value!.lookupItemByName(node.fn);
      drawNode(ctx, viewport, network, node, fn, isSelected(node), mousePositionInView, dragStateRef);
    }

    for (const inlet of networkInlets(network)) {
      drawInlet(ctx, viewport, inlet, isSelected(inlet));
    }

    for (const outlet of networkOutlets(network)) {
      drawOutlet(ctx, viewport, outlet, isSelected(outlet));
    }
  }

  function drawSelectRect(ctx: CanvasRenderingContext2D, viewport: Viewport) {
    if (dragStateRef.current.mode !== DragMode.SelectRect) return;
    ctx.fillStyle = DRAG_SELECTION_FILL;
    ctx.strokeStyle = DRAG_SELECTION_STROKE;
    ctx.lineWidth = 1;
    const dragRect = dragStateRef.current.rect;
    const rect = rectToView(dragRect, viewport);
    ctx.fillRect(rect.x, rect.y, rect.width, rect.height);
    ctx.strokeRect(rect.x, rect.y, rect.width, rect.height);
  }

  function drawCurrentConnection(ctx: CanvasRenderingContext2D, viewport: Viewport) {
    const dragState = dragStateRef.current;
    if (dragState.mode === DragMode.InletConnection) {
      const inlet = dragState.inlet;
      const r = inletRect(inlet);
      ctx.strokeStyle = HIGHLIGHT_STROKE;
      ctx.lineWidth = 2;
      const x1 = r.x + PORT_WIDTH / 2;
      const y1 = r.y + r.height;
      const { x, y } = mousePositionRef.current;
      drawConnectionLine(ctx, x1, y1, x, y);
    } else if (dragState.mode === DragMode.NodeConnection) {
      const node = dragState.node;
      const portIndex = dragState.portIndex;
      ctx.strokeStyle = HIGHLIGHT_STROKE;
      ctx.lineWidth = 2;
      const { x, y } = scaledMousePositionRef.current;
      drawConnectionLine(
        ctx,
        (node.x + portIndex * (PORT_WIDTH + PORT_MARGIN) + PORT_WIDTH / 2) * viewport.zoom + viewport.x,
        (node.y + NODE_HEIGHT) * viewport.zoom + viewport.y,
        x * viewport.zoom + viewport.x,
        y * viewport.zoom + viewport.y,
      );
    }
  }

  function drawPortTooltip(ctx: CanvasRenderingContext2D, viewport: Viewport, network: Network) {
    // There are two kind of port tooltips:
    // - Tooltips on an input port (for a Node or Outlet)
    // - Tooltips on an output port (for a Node or Inlet)

    // We don't want to show the tooltips when:
    // - We're dragging an item
    // - We're dragging a selection rectangle
    // - We're panning the view

    const dragMode = dragStateRef.current.mode;
    // const sourcePort = dragStateRef.current.port;
    if (dragMode === DragMode.Item || dragMode === DragMode.SelectRect || dragMode === DragMode.Pan) return;

    const { x, y } = scaledMousePositionRef.current;
    const item = findItemByPosition(network, x, y);
    if (!item) return;
    if (item.type === "NODE") {
      const node = item as Node;
      const fn = cx.value!.lookupItemByName(node.fn);
      const dx = x - node.x + PORT_MARGIN / 2;
      const dy = y - node.y;
      if (dragMode === DragMode.InletConnection || dragMode === DragMode.NodeConnection) {
        // Don't show tooltips of input ports when dragging over the output node.
        if (dragMode === DragMode.NodeConnection && dragStateRef.current.node === node) {
          const port = dragStateRef.current.port;
          port && drawTransformedTooltip(ctx, viewport, x, y, `${port.name} (${port.type})`);
          return;
        }
        const portIndex = Math.floor(dx / (PORT_WIDTH + PORT_MARGIN));
        const p = fn ? fn.inputPorts[portIndex] : undefined;
        if (!p) return;
        if (
          dragMode === DragMode.NodeConnection &&
          dragStateRef.current.port &&
          p.type !== dragStateRef.current.port.type
        ) {
          return;
        }
        drawTransformedTooltip(ctx, viewport, x, y, `${p.name} (${p.type})`);
      } else if (dy <= PORT_HEIGHT_AFFORDANCE) {
        // We're over an input port.
        const portIndex = Math.floor(dx / (PORT_WIDTH + PORT_MARGIN));
        const p = fn ? fn.inputPorts[portIndex] : undefined;
        if (!p) return;
        drawTransformedTooltip(ctx, viewport, x, y, `${p.name} (${p.type})`);
      } else if (dy >= NODE_HEIGHT - PORT_HEIGHT - PORT_MARGIN / 2 && dy <= NODE_HEIGHT + PORT_MARGIN / 2) {
        // We're over an output port.

        const portIndex = Math.floor(dx / (PORT_WIDTH + PORT_MARGIN));
        const p = fn ? fn.outputPorts[portIndex] : undefined;
        if (!p) return;
        drawTransformedTooltip(ctx, viewport, x, y, `${p.name} (${p.type})`);
      }
    } else if (item.type === "INLET") {
      const inlet = item as Inlet;
      const networkPort = network.inputPorts.find((p) => p.name === inlet.portName)!;
      drawTransformedTooltip(ctx, viewport, x, y, `${inlet.portName} (${networkPort.type})`);
    } else if (item.type === "OUTLET") {
      const outlet = item as Outlet;
      const networkPort = network.outputPorts.find((p) => p.name === outlet.portName)!;
      drawTransformedTooltip(ctx, viewport, x, y, `${outlet.portName} (${networkPort.type})`);
    }
  }

  function handleResize() {
    const dpi = Math.min(window.devicePixelRatio, 2);
    const canvas = canvasRef.current;
    // This can happen when reloading the page.
    if (!canvas) return;
    const bounds = canvas.getBoundingClientRect();
    canvas.width = bounds.width * dpi;
    canvas.height = bounds.height * dpi;
    forceUpdate();
    draw();
  }

  function handleKeyDown(e: KeyboardEvent) {
    const network = activeItem.value as Network;

    const commandKeyPressed = e.metaKey || e.ctrlKey;
    if ((e.target as HTMLElement).tagName === "INPUT" || (e.target as HTMLElement).tagName === "TEXTAREA") return;
    if (e.key === "g") {
      e.preventDefault();
      const selectionIds: string[] = Array.from(selectionIdsMap.value.get(network.id) || new Set());
      const newNetworkName = prompt("Enter a name for the new network");
      if (newNetworkName) {
        groupIntoNetwork(network, selectionIds, newNetworkName);
        forceUpdate();
      }
    } else if (e.key === "Delete" || e.key === "Backspace") {
      e.preventDefault();
      const selectionIds: string[] = Array.from(selectionIdsMap.value.get(network.id) || new Set());
      deleteItems(network, selectionIds);
      forceUpdate();
    } else if (commandKeyPressed && e.key === "a") {
      e.preventDefault();
      setSelection(network.children.map((c) => c.id));
      forceUpdate();
    } else if (commandKeyPressed && e.key === "c") {
      e.preventDefault();
      handleCopyToClipboard();
      forceUpdate();
    } else if (commandKeyPressed && e.key === "v") {
      e.preventDefault();
      handlePasteFromClipboard();
      forceUpdate();
    }
  }

  function handleKeyUp() {
    // Ignore key up events for now.
  }

  function handleMouseDown(e: React.MouseEvent | MouseEvent) {
    setContextMenu(null);
    e.preventDefault();
    const network = activeItem.value as Network;
    const selection = getSelection(network);
    // Steal focus from current element. This is to prevent the title input in the header to keep focus.
    (e.currentTarget as HTMLInputElement).focus();
    dragStartRef.current = mouseOffset(e);
    startedDraggingRef.current = false;
    if (e.button === 0) {
      // Left mouse button drags, single-selects, or starts a connection.
      // To find out which, check if we're over a node, inlet or outlet.
      const offset = mouseOffset(e);
      const scaledPos = canvasToView(offset);
      const overItem = findItemByPosition(network, scaledPos.x, scaledPos.y);
      const overStickyResize = findStickyResizerByPosition(network, scaledPos.x, scaledPos.y);
      const overInput = findInputByPosition(cx.value!, network, scaledPos.x, scaledPos.y);
      const overOutput = findOutputByPosition(cx.value!, network, scaledPos.x, scaledPos.y);
      const overInletPort = findInletPortByPosition(network, scaledPos.x, scaledPos.y);
      const selectionIncludesOverItem = overItem && !!selection.has(overItem.id);
      // The order here matters:
      // - When shift key pressed, modify selection - append / remove from selection
      // - If we're over an output, we want to start a connection.
      // - If we're over an input, we want to remove a connection.
      // - If we're over a sticky resizer,
      // - If we have a selection, and the current node is included in the selection, we want to drag it.
      // - If we're over a node (but not in the selection),
      //        either we want to drag that node,
      //        or with control key pressed we duplicate that node
      if (e.shiftKey && overItem && getSelection(network).size > 0) {
        //extend selection list and change mouse cursor
        if (selection.has(overItem.id)) {
          setSelection(Array.from(getSelection(network)).filter((sel) => sel != overItem.id));
        } else {
          setSelection([...getSelection(network), overItem.id]);
        }
      } else if (overOutput) {
        dragStateRef.current = { mode: DragMode.NodeConnection, ...overOutput };
      } else if (overInletPort) {
        dragStateRef.current = { mode: DragMode.InletConnection, inlet: overInletPort };
      } else if (overInput) {
        // Check if there is a connection here.
        const conn = network.connections.find(
          (c) =>
            (c.type === ConnectionType.InletToNode || c.type === ConnectionType.NodeToNode) &&
            c.inNode === overInput.node.id &&
            c.inPort === overInput.port.name,
        );
        if (conn) {
          if (conn.type === ConnectionType.NodeToNode) {
            const outputNode = network.children.find((n) => n.id === conn.outNode) as Node;
            const fn = cx.value!.lookupItemByName(outputNode.fn);
            const portIndex = fn ? fn.outputPorts.findIndex((p) => p.name === conn.outPort) : 0;
            const outputPort = fn && fn.outputPorts[portIndex];
            dragStateRef.current = { mode: DragMode.NodeConnection, node: outputNode, port: outputPort, portIndex };
            disconnect(network, conn.inNode, conn.inPort);
          } else if (conn.type === ConnectionType.InletToNode) {
            const inlet = network.children.find((n) => n.id === conn.inNode) as Inlet;
            dragStateRef.current = { mode: DragMode.InletConnection, inlet: inlet };
            disconnect(network, conn.inNode, conn.inPort);
          }
        }
      } else if (overStickyResize) {
        const sticky = overStickyResize as Sticky;
        dragStateRef.current = {
          mode: DragMode.Resize,
          item: sticky,
          dimension: { width: sticky.width, height: sticky.height },
        };
      } else if (overItem && selectionIncludesOverItem) {
        const ids = Array.from(selection);
        const items = network.children.filter((item) => ids.includes(item.id));
        if (e.altKey) {
          // Note that duplicateItems, in addition to returning the new ids, also returns a new network.
          // That's because our activeItem and dragStateRef will refer to the new items, which don't exist yet
          // on the current (i.e. old) network.
          const [newNetwork, newIds] = duplicateItems(network, ids);
          setSelection(newIds);
          const newItems = newNetwork.children.filter((item) => newIds.includes(item.id));
          const firstItem = newItems[0];
          setActiveItem(firstItem!);
          dragStateRef.current = { mode: DragMode.Selection, items: newItems };
        } else {
          dragStateRef.current = { mode: DragMode.Selection, items };
        }
      } else if (overItem) {
        if (e.altKey) {
          // Note that duplicateItems, in addition to returning the new ids, also returns a new network.
          // That's because our activeItem and dragStateRef will refer to the new items, which don't exist yet
          // on the current (i.e. old) network.
          const [newNetwork, newIds] = duplicateItems(network, [overItem.id]);
          setSelection(newIds);
          const newItems = newNetwork.children.filter((item) => newIds.includes(item.id));
          const firstItem = newItems[0];
          setActiveItem(firstItem!);
          dragStateRef.current = { mode: DragMode.Selection, items: newItems };
        } else {
          setSelection([overItem.id]);
          setActiveItem(overItem);
          dragStateRef.current = { mode: DragMode.Item, item: overItem, position: { x: overItem.x, y: overItem.y } };
        }
      } else if (e.shiftKey) {
        // The rectangle is in view (network) coordinates.
        dragStateRef.current = {
          mode: DragMode.SelectRect,
          startPoint: scaledPos,
          rect: new Rect(scaledPos.x, scaledPos.y, 0, 0),
          ids: [],
        };
      } else {
        dragStateRef.current = { mode: DragMode.Pan, start: structuredClone(getViewport()) };
      }
    }
    window.addEventListener("mousemove", handleMouseDrag);
    window.addEventListener("mouseup", handleMouseUp);
    forceUpdate();
  }

  function handleMouseMove(e: React.MouseEvent) {
    const offset = mouseOffset(e);
    mousePositionRef.current = offset;
    scaledMousePositionRef.current = canvasToView(offset);
    forceUpdate();
  }

  function handleMouseDrag(e: MouseEvent) {
    e.preventDefault();
    const network = activeItem.value as Network;
    const scaledPos = mousePositionInView(e);
    const dragStart = canvasToView(dragStartRef.current!);
    const dragOffset = {
      x: dragStart.x - scaledPos.x,
      y: dragStart.y - scaledPos.y,
    };
    const dragState = dragStateRef.current;
    if (dragState.mode === DragMode.Resize) {
      const sticky = dragState.item as Sticky;
      const newWidth = snap(
        Math.max(STICKY_MIN_WIDTH, dragState.dimension.width - dragOffset.x),
        GRID_SNAP_SIZE,
        STICKY_SNAP_OFFSET_X,
      );
      const newHeight = snap(
        Math.max(STICKY_MIN_HEIGHT, dragState.dimension.height - dragOffset.y),
        GRID_SNAP_SIZE,
        STICKY_SNAP_OFFSET_Y,
      );
      setStickySize(network, dragState.item, newWidth, newHeight);
      stickyCanvasCacheMap.delete(sticky.id);
    } else if (dragState.mode === DragMode.SelectRect) {
      const dragRect = Rect.fromPoints(dragState.startPoint, scaledPos);
      dragState.rect = dragRect;
      dragState.ids = network.children.filter((item) => dragRect.intersects(itemRect(item))).map((item) => item.id);
    } else if (dragState.mode === DragMode.Selection) {
      const positions = new Map<string, Point>();
      let newX = 0,
        newY = 0;
      for (const item of dragState.items) {
        const startX = snap(item.x, GRID_SNAP_SIZE, GRID_SNAP_OFFSET_X);
        const startY = snap(item.y, GRID_SNAP_SIZE, GRID_SNAP_OFFSET_Y);
        if (item.type === "STICKY") {
          newX = snap(startX - dragOffset.x, GRID_SNAP_SIZE, STICKY_SNAP_OFFSET_X);
          newY = snap(startY - dragOffset.y, GRID_SNAP_SIZE, STICKY_SNAP_OFFSET_Y);
        } else {
          newX = snap(startX - dragOffset.x, GRID_SNAP_SIZE, GRID_SNAP_OFFSET_X);
          newY = snap(startY - dragOffset.y, GRID_SNAP_SIZE, GRID_SNAP_OFFSET_Y);
        }
        positions.set(item.id, { x: newX, y: newY });
      }
      setItemPositions(network, positions);
    } else if (dragState.mode === DragMode.Item) {
      let newX = dragState.position.x - dragOffset.x;
      let newY = dragState.position.y - dragOffset.y;
      if (dragState.item.type === "STICKY") {
        newX = snap(newX, GRID_SNAP_SIZE, STICKY_SNAP_OFFSET_X);
        newY = snap(newY, GRID_SNAP_SIZE, STICKY_SNAP_OFFSET_Y);
      } else {
        newX = snap(newX, GRID_SNAP_SIZE, GRID_SNAP_OFFSET_X);
        newY = snap(newY, GRID_SNAP_SIZE, GRID_SNAP_OFFSET_Y);
      }
      setItemPositions(network, new Map([[dragState.item.id, { x: newX, y: newY }]]));
    } else if (dragState.mode === DragMode.InletConnection || dragState.mode === DragMode.NodeConnection) {
      // Do nothing
    } else if (dragState.mode === DragMode.Pan) {
      const viewport = getViewport();
      const pos = mouseOffset(e);
      const dx = pos.x - dragStartRef.current.x;
      const dy = pos.y - dragStartRef.current.y;
      const vx = dragState.start.x + dx;
      const vy = dragState.start.y + dy;
      setViewport(viewport.zoom, vx, vy);
    }
    forceUpdate();
  }

  function handleMouseUp(e: MouseEvent) {
    e.preventDefault();
    const network = activeItem.value as Network;
    const scaledPos = mousePositionInView(e);
    // const dragDistanceSquared =
    //   Math.pow(dragStartRef.current.x - e.clientX, 2) + Math.pow(dragStartRef.current.y - e.clientY, 2);

    const dragState = dragStateRef.current;
    if (dragState.mode === DragMode.SelectRect) {
      if (e.shiftKey) {
        const currentSelection = Array.from(getSelection(network));
        const newSelection = [...dragState.ids].reduce((selection, id) => {
          if (selection.includes(id)) {
            return selection.filter((item) => item !== id); // Remove if already selected
          } else {
            return [...selection, id]; // Add if not selected
          }
        }, currentSelection);

        setSelection(newSelection);
      } else {
        setSelection(dragState.ids);
      }
      clearActiveItem();
    } else if (dragState.mode === DragMode.Item) {
      if (e.shiftKey) {
        setSelection([...Array.from(getSelection(network)), dragState.item.id]);
      } else {
        setSelection([dragState.item.id]);
      }
      setActiveItem(dragState.item);
    } else if (dragState.mode === DragMode.InletConnection) {
      const overInput = findInputByPosition(cx.value!, network, scaledPos.x, scaledPos.y);
      if (overInput) {
        connectInletToNode(network, dragState.inlet, overInput.node, overInput.port);
      }
    } else if (dragState.mode === DragMode.NodeConnection) {
      const overInput = findInputByPosition(cx.value!, network, scaledPos.x, scaledPos.y, true);
      const overOutlet = findOutletByPosition(network, scaledPos.x, scaledPos.y, true);
      if (overInput) {
        dragState.port &&
          dragState.port.type === overInput.port.type &&
          connectNodeToNode(network, dragState.node, dragState.port, overInput.node, overInput.port);
      } else if (overOutlet) {
        dragState.port && connectNodeToOutlet(network, dragState.node, dragState.port, overOutlet);
      }
    } else if (dragState.mode === DragMode.Pan) {
      setSelection([]);
      clearActiveItem();
    }

    dragState.mode = DragMode.None;
    window.removeEventListener("mousemove", handleMouseDrag);
    window.removeEventListener("mouseup", handleMouseUp);
    forceUpdate();
  }

  function handleDoubleClick(e: React.MouseEvent) {
    const network = activeItem.value as Network;
    let textArea: HTMLTextAreaElement | undefined, sticky: Sticky | undefined;

    function handleClickOutside(event: MouseEvent) {
      const target = event.target! as globalThis.Node;
      if (!textArea!.contains(target)) {
        commitStickyText(sticky!, textArea!.value);
        textArea!.remove();
        document.removeEventListener("mousedown", handleClickOutside);
        document.removeEventListener("wheel", handleWheel);
      }
    }

    function handleWheel(event: WheelEvent) {
      const target = event.target! as globalThis.Node;
      if (textArea!.contains(target)) {
        event.preventDefault();
        commitStickyText(sticky!, textArea!.value);
      } else {
        textArea!.remove();
        document.removeEventListener("mousedown", handleClickOutside);
        document.removeEventListener("wheel", handleWheel);
      }
    }

    function commitStickyText(sticky: Sticky, text: string | undefined) {
      setStickyText(network, sticky, text || "empty");
      stickyCanvasCacheMap.delete(sticky.id);
    }

    e.preventDefault();
    const scaledPos = mousePositionInView(e);
    const overNode = findNodeByPosition(network, scaledPos.x, scaledPos.y);
    const overItem = findItemByPosition(network, scaledPos.x, scaledPos.y);
    if (overItem && isSticky(overItem)) {
      sticky = overItem as Sticky;
      const boundingRect = canvasRef.current!.getBoundingClientRect();
      const viewport = getViewport();
      textArea = document.createElement("textarea");
      textArea.style.top = `${(sticky.y + 0.3175) * viewport.zoom + viewport.y + boundingRect.top + 5 * 0.3175}px`;
      textArea.style.left = `${(1 + sticky.x) * viewport.zoom + viewport.x + boundingRect.left}px`;
      textArea.style.width = `${(sticky.width - 12 * 0.3175) * viewport.zoom}px`;
      textArea.style.height = `${sticky.height * viewport.zoom}px`;
      textArea.style.background = colorToHex(sticky.backgroundColor);
      textArea.style.paddingTop = `${3.175 * viewport.zoom}px`;
      textArea.style.paddingLeft = `${4 * viewport.zoom - 0.3175}px`;
      textArea.style.paddingRight = `${4 * viewport.zoom - 0.3175}px`;
      textArea.style.overflow = "hidden";
      textArea.style.verticalAlign = "top";
      textArea.style.lineHeight = `${sticky.fontSize * viewport.zoom}px`;

      textArea.value = sticky.text;
      textArea.className = `absolute border-none resize-none text-zinc-900 `;
      textArea.style.color = colorToHex(sticky.fontColor);
      textArea.style.fontSize = `${sticky.fontSize * viewport.zoom}px`;
      textArea.style.fontFamily = "Inter, sans-serif";
      textArea.style.outline = "none";

      // Replace the mouseleave event with a keydown event to listen for Control+Enter, Shift+Enter, Escape, or Tab
      textArea.addEventListener("keydown", (event) => {
        if (
          (event.ctrlKey && event.key === "Enter") ||
          (event.shiftKey && event.key === "Enter") ||
          event.key === "Escape" ||
          event.key === "Tab"
        ) {
          if (event.key !== "Escape") {
            commitStickyText(sticky!, textArea!.value);
          }
          textArea!.remove();
          document.removeEventListener("mousedown", handleClickOutside);
          document.removeEventListener("wheel", handleWheel);
        }
      });

      document.addEventListener("mousedown", handleClickOutside);
      document.addEventListener("wheel", handleWheel, { passive: false });

      document.body.appendChild(textArea);

      textArea.focus();
      textArea.select();
    } else if (overNode) {
      // If double-clicking on a node, set that node as the rendered node.
      const fn = cx.value!.lookupItemByName(overNode.fn);
      if (!fn) {
        return;
        // Invalid node
        // createNodeModalVisible.value = true;
        // createNodeModalVisibleMode.value = overNode.id;
      } else setRenderedNode(network, overNode.id);
    } else if (overItem && (overItem.type === "OUTLET" || overItem.type === "INLET")) {
      // Do nothing
    } else {
      // If double-clicking on an empty space, show the node creation modal.
      // Put newly created node at the tip of the cursor.
      const canvas = canvasRef.current!;
      const bounds = canvas.getBoundingClientRect();
      const viewport = getViewport();
      const minX = -viewport.x / viewport.zoom;
      const minY = -viewport.y / viewport.zoom;
      const maxX = bounds.width / viewport.zoom - NODE_WIDTH + minX;
      const maxY = bounds.height / viewport.zoom - NODE_HEIGHT + minY;

      const pos = mousePositionInView(e);
      pos.x = snap(Math.max(minX, Math.min(pos.x, maxX)), GRID_SNAP_SIZE, GRID_SNAP_OFFSET_X);
      pos.y = snap(Math.max(minY, Math.min(pos.y, maxY)), GRID_SNAP_SIZE, GRID_SNAP_OFFSET_Y);
      createNodeModalVisible.value = true;
      createNetworkItemPosition.value = pos;
    }
    forceUpdate();
  }

  function handleWheel(e: React.WheelEvent<HTMLCanvasElement>) {
    if (dragStateRef.current.mode !== DragMode.None) return;

    const { deltaY, clientX, clientY } = e;
    const canvasRect = (e.currentTarget as HTMLCanvasElement).getBoundingClientRect();

    const cursorX = clientX - canvasRect.left;
    const cursorY = clientY - canvasRect.top;

    const viewport = getViewport();
    const newZoom = Math.max(MIN_VIEW_SCALE, Math.min(viewport.zoom * (1 - deltaY * 0.002), MAX_VIEW_SCALE));
    const zoomFactor = newZoom / viewport.zoom;

    const newOriginX = cursorX - zoomFactor * (cursorX - viewport.x);
    const newOriginY = cursorY - zoomFactor * (cursorY - viewport.y);

    setViewport(newZoom, newOriginX, newOriginY);
    forceUpdate();
  }

  function handleCreateSticky() {
    const network = activeItem.value as Network;
    createNewSticky(network);
  }

  function _randomPositionInView(): Point {
    const canvas = canvasRef.current!;
    const bounds = canvas.getBoundingClientRect();
    const viewport = getViewport();
    const minX = -viewport.x / viewport.zoom;
    const minY = -viewport.y / viewport.zoom;
    const maxX = bounds.width / viewport.zoom - NODE_WIDTH + minX;
    const maxY = bounds.height / viewport.zoom - NODE_HEIGHT + minY;

    let randomX = minX + Math.random() * (maxX - minX);
    let randomY = minY + Math.random() * (maxY - minY);
    randomX = snap(randomX, GRID_SNAP_SIZE, GRID_SNAP_OFFSET_X);
    randomY = snap(randomY, GRID_SNAP_SIZE, GRID_SNAP_OFFSET_Y);
    return { x: randomX, y: randomY };
  }

  function handleCreateNode() {
    createNetworkItemPosition.value = _randomPositionInView();
    createNodeModalVisible.value = true;
  }

  function handleCreateOutlet() {
    createNetworkItemPosition.value = _randomPositionInView();
    createOutletModalVisible.value = true;
  }

  function handleContextMenu(e: React.MouseEvent) {
    e.preventDefault();
    setContextMenu({ x: e.clientX + 2, y: e.clientY - 6 });
  }

  function handleResetViewport(origin: boolean = true) {
    const network = activeItem.value as Network;

    setContextMenu(null);

    if (origin || network.children.length === 0) {
      setViewport(1, 0, 0);
      forceUpdate();
      return;
    }

    const minX = Math.min(...network.children.map((node) => node.x));
    const maxX = Math.max(...network.children.map((node) => node.x + itemRect(node).width));
    const minY = Math.min(...network.children.map((node) => node.y));
    const maxY = Math.max(...network.children.map((node) => node.y + itemRect(node).height));
    const width = maxX - minX;
    const height = maxY - minY;
    const canvas = canvasRef.current!;
    const bounds = canvas.getBoundingClientRect();
    const zoom = Math.min(DEFAULT_VIEW_SCALE, Math.min(bounds.width / width, bounds.height / height) / 1.02);

    setViewport(
      Math.max(MIN_VIEW_SCALE, Math.min(MAX_VIEW_SCALE, zoom)),
      -minX * zoom + (bounds.width - width * zoom) / 2,
      -minY * zoom + (bounds.height - height * zoom) / 2,
    );
    forceUpdate();
  }

  function handleCopyAsImage(event: React.MouseEvent) {
    const canvas = document.getElementsByTagName("canvas")[0];

    // Disable grid drawing for screenshot
    draw({ hideGrid: true });

    const image = canvas.toDataURL("image/png").replace("image/png", "image/octet-stream");

    if (event.shiftKey) {
      // Download the file
      const link = document.createElement("a");
      link.href = image;
      link.download = "canvas-image.png";
      link.click();
    } else {
      // Copy to clipboard
      canvas.toBlob(function (blob) {
        if (blob) {
          const item = new ClipboardItem({ "image/png": blob });
          navigator.clipboard
            .write([item])
            .then(() => {
              console.log("Image copied to clipboard");
            })
            .catch((error) => {
              console.error("Failed to copy image: ", error);
            });
        } else {
          console.error("Failed to generate blob from canvas");
        }
      });
    }

    draw();
  }

  function handleGroupIntoNetwork() {
    const network = activeItem.value as Network;
    setContextMenu(null);
    const newNetworkName = prompt("Enter a name for the new network");
    if (!newNetworkName) return;
    const selection = Array.from(getSelection(network));
    groupIntoNetwork(network, selection, newNetworkName);
    forceUpdate();
  }

  function handleSetRendered() {
    const network = activeItem.value as Network;
    setContextMenu(null);
    const itemIds = getSelection(network);
    if (itemIds.size === 0) return;
    const id: string | undefined = itemIds.values().next().value;
    const item = network.children.find((it) => it.id === id);
    if (item?.type !== "NODE") return;
    id && setRenderedNode(network, id);
    forceUpdate();
  }

  function handleCopyToClipboard() {
    const network = activeItem.value as Network;
    setContextMenu(null);
    const itemIds = getSelection(network);
    const ids = Array.from(itemIds);
    if (ids.length === 0) return [];
    const items = network.children.filter((child) => ids.includes(child.id));
    // Internal connections are connections between selected nodes.
    // Both inNode and outNode should be replaced by the newly duplicated nodes.
    const internalConnections = network.connections.filter((c) => {
      return c.type === ConnectionType.NodeToNode && ids.includes(c.outNode) && ids.includes(c.inNode);
    }) as NodeToNodeConnection[];
    const renderedNodeIndex = ids.findIndex((i) => i === network.renderedNode);
    navigator.clipboard.writeText(
      `{"items":${JSON.stringify(
        items,
      )},"renderedNodeIndex":"${renderedNodeIndex}" ,"internalConnections":${JSON.stringify(internalConnections)}}`,
    );
  }

  function handleDuplicateItems() {
    setContextMenu(null);
    const network = activeItem.value as Network;
    const itemIds = getSelection(network);
    const [newNetwork, newIds] = duplicateItems(network, [...itemIds]);
    setSelection(newIds);
    const newItems = newNetwork.children.filter((item) => newIds.includes(item.id));
    const firstItem = newItems[0];
    setActiveItem(firstItem!);
    dragStateRef.current = { mode: DragMode.Selection, items: newItems };
  }

  function handleDeleteSelectedNodes() {
    setContextMenu(null);
    const network = activeItem.value as Network;
    const itemIds = getSelection(network);
    deleteItems(network, Array.from(itemIds));
    forceUpdate();
  }

  function handlePasteFromClipboard() {
    setContextMenu(null);
    setTimeout(async () => {
      const network = activeItem.value as Network;
      const pasteData = await navigator.clipboard.readText();

      console.log("Paste", pasteData);
      try {
        const json = JSON.parse(pasteData);
        const [newIds] = pasteItemsFromClipboard(network, json.items!, json.internalConnections!);
        setSelection(newIds);
        setRenderedNode(network, newIds[json.renderedNodeIndex]);
      } catch (err) {
        // The paste data was probably invalid.
      }
    }, 100);
  }

  return (
    <div className="flex-1 h-full overflow-hidden flex flex-col">
      <header className="flex items-center h-10 px-2 border-b border-b-zinc-700 text-xs overflow-hidden">
        {/* removed justify-between */}
        <button onClick={handleCreateNode} className="flex gap-1 text-xs hover:bg-zinc-800 px-1 py-1 rounded">
          <Icon name="plus" /> Create Node
        </button>
        <button onClick={handleCreateSticky} className="flex gap-1 text-xs hover:bg-zinc-800 px-1 py-1 rounded">
          <Icon name="plus" /> Create Sticky
        </button>
        <button onClick={handleCreateOutlet} className="flex gap-1 text-xs hover:bg-zinc-800 px-1 py-1 rounded">
          <Icon name="plus" /> Create Outlet
        </button>
      </header>
      <canvas
        ref={canvasRef}
        className="flex-1 overflow-hidden outline-none"
        style={{ height: "calc(100vh - 88px)" }}
        onMouseDown={handleMouseDown}
        onMouseMove={handleMouseMove}
        onDoubleClick={handleDoubleClick}
        onWheel={handleWheel}
        onContextMenu={handleContextMenu}
        tabIndex={-1}
      />
      <Menu
        open={!!contextMenu}
        onClose={() => setContextMenu(null)}
        onContextMenu={(e) => handleContextMenu(e)}
        anchorPosition={contextMenu ? contextMenu : { x: 0, y: 0 }}
      >
        <MenuItem disabled={currentSelection.size === 0} onClick={handleCopyToClipboard} shortcutKey="ctrl+c">
          Copy
        </MenuItem>
        <MenuItemClipboardAvailable onClick={handlePasteFromClipboard} shortcutKey="ctrl+v" />
        <MenuSeparator />
        <MenuItem disabled={currentSelection.size === 0} onClick={handleDuplicateItems}>
          Duplicate
        </MenuItem>
        <MenuItem disabled={currentSelection.size === 0} onClick={handleDeleteSelectedNodes}>
          Delete
        </MenuItem>
        <MenuItem disabled={currentSelection.size != 1} onClick={handleSetRendered}>
          Set Rendered
        </MenuItem>
        <MenuItem disabled={currentSelection.size === 0} onClick={handleGroupIntoNetwork}>
          Group Into Network
        </MenuItem>
        <MenuSeparator />
        <MenuItem onClick={() => handleResetViewport(true)}>Reset View</MenuItem>
        <MenuItem onClick={() => handleResetViewport(false)}>Fit View</MenuItem>
        <MenuSeparator />
        <MenuItem onClick={(event) => handleCopyAsImage(event)}>Copy as Image</MenuItem>
      </Menu>
    </div>
  );
}
