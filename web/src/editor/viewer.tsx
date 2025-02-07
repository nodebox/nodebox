import React, { useEffect, useState, useRef, isValidElement, ReactElement, useCallback } from "react";
import clsx from "clsx";
import { Item, Network, renderShape, renderDefs, renderVegaSpec } from "@ndbx/runtime";
import { CanvasSize, Context as GraphicsContext, Shape, Bounds } from "@ndbx/g";
import {
  projectId,
  result,
  resultVersion,
  renderToSvg,
  nodeError,
  currentItem,
  togglePlay,
  playState,
  PlayState,
} from "./signals";
import TableViewer from "./table-viewer";
import DetailViewer from "./detail-viewer";
import Toggle from "../components/toggle";
import { Menu, MenuItem } from "../components/menu";
import Icon from "../components/icon";
import { ButtonMenu, MenuOption } from "../components/button-menu";
import { toast } from "react-toastify";
import * as vega from "vega";
import { colorIsTransparent, colorToCss } from "../lib/color-utils";

const MIN_VIEW_SCALE = 0.15;
const MAX_VIEW_SCALE = 5;

interface PopupState {
  content: string;
  x: number;
  y: number;
  visible: boolean;
}
function Popup({ content, x, y, visible }: PopupState) {
  if (!visible) return null;

  return (
    <div
      className="absolute z-50 bg-zinc-800 text-zinc-100 px-2 py-1 text-xs rounded shadow-lg pointer-events-none"
      style={{
        left: x + 10,
        top: y + 10,
      }}
      dangerouslySetInnerHTML={{ __html: content }}
    />
  );
}

interface DetailedError extends Error {
  lineNumber?: number;
}

function formatError(error: DetailedError): string {
  let s = error.toString();
  if (error.lineNumber) {
    s += ` at line ${error.lineNumber}`;
  }
  return s;
}

interface HtmlViewerProps {
  item: Item;
  result: ReactElement;
}

function HtmlViewer({ item, result }: HtmlViewerProps) {
  const wrapperRef = useRef(null);
  return (
    <div data-item-id={item.id} className="flex-1 overflow-auto" ref={wrapperRef}>
      {result}
    </div>
  );
}

interface Viewport {
  zoom: number;
  x: number;
  y: number;
}

interface MousePosition {
  x: number;
  y: number;
}

interface CanvasViewerProps {
  item: Item;
  result: Shape;
  showAttributes: boolean;
  drawPoints: boolean;
  drawBounds: boolean;
}

function isVegaSpec(value: unknown): value is vega.Spec {
  return typeof value === "object" && value !== null && "$schema" in (value as Record<string, unknown>);
}

function hasInlineValues(data: vega.BaseData): data is vega.BaseData & { values: unknown[] } {
  return "values" in data;
}

function CanvasViewer({ item, result, showAttributes, drawPoints, drawBounds }: CanvasViewerProps) {
  const [contextMenu, setContextMenu] = useState<MousePosition | null>(null);
  const [viewport, setViewport] = useState<Viewport>({ zoom: 1, x: 0, y: 0 });

  function handleMouseDown(e: React.MouseEvent) {
    e.preventDefault();
    // Steal focus from current element. This is to prevent the title input in the header to keep focus.
    (e.currentTarget as HTMLInputElement).focus();
    const startX = e.clientX;
    const startY = e.clientY;
    const startViewport = { ...viewport };

    const handleMouseMove = (e: MouseEvent) => {
      const dx = e.clientX - startX;
      const dy = e.clientY - startY;
      setViewport({
        zoom: startViewport.zoom,
        x: startViewport.x + dx,
        y: startViewport.y + dy,
      });
    };
    document.addEventListener("mousemove", handleMouseMove);
    document.addEventListener("mouseup", () => {
      document.removeEventListener("mousemove", handleMouseMove);
    });
  }

  function handleWheel(e: React.WheelEvent<SVGSVGElement>) {
    const { deltaY, clientX, clientY } = e;
    const svgRect = (e.currentTarget as SVGSVGElement).getBoundingClientRect();

    // Coordinates of the cursor relative to the SVG element
    const cursorX = clientX - svgRect.left;
    const cursorY = clientY - svgRect.top;

    setViewport((prev) => {
      const newZoom = Math.max(MIN_VIEW_SCALE, Math.min(prev.zoom * (1 - deltaY * 0.002), MAX_VIEW_SCALE));
      const zoomFactor = newZoom / prev.zoom;
      const newOriginX = cursorX - zoomFactor * (cursorX - prev.x);
      const newOriginY = cursorY - zoomFactor * (cursorY - prev.y);

      return { zoom: newZoom, x: newOriginX, y: newOriginY };
    });
  }

  function handleContextMenu(e: React.MouseEvent) {
    e.preventDefault();
    setContextMenu({ x: e.clientX + 2, y: e.clientY - 6 });
  }

  function handleResetViewport() {
    setContextMenu(null);
    setViewport({ zoom: 1, x: 0, y: 0 });
  }

  const getTransform = (dx: number = 0, dy: number = 0) => {
    const { zoom, x, y } = viewport;
    return `translate(${x + dx} ${y + dy}) scale(${zoom})`;
  };

  // This is here to force an update when the result changes
  resultVersion.value;

  const tx = (x: number) => x * viewport.zoom + viewport.x;
  const ty = (y: number) => y * viewport.zoom + viewport.y;

  let graphicsContext = new GraphicsContext();
  let shapeElement = null;
  let defsElement = null;
  if (result)
    try {
      if (result && result.type) {
        shapeElement = renderShape(result, graphicsContext, undefined, drawPoints);
        defsElement = renderDefs(graphicsContext);
      }
    } catch (e) {
      console.error(e);
    }

  let svgSize: CanvasSize = { left: 0, top: 0, width: item.width ?? 1000, height: item.height ?? 1000 };
  if (result && result.getBounds && (item as Network).canvasSize === "auto") {
    const autoSize: Bounds = result.getBounds();
    svgSize.left = autoSize.left;
    svgSize.top = autoSize.top;
    svgSize.width = autoSize.right - autoSize.left;
    svgSize.height = autoSize.bottom - autoSize.top;
  }
  const backgroundColor = item.background || "transparent";

  const [popup, setPopup] = useState<PopupState>({
    content: "",
    x: 0,
    y: 0,
    visible: false,
  });

  const handleElementMouseOver = useCallback((e: MouseEvent) => {
    const element = e.target as SVGElement;
    element.classList.add("__nodebox_svg_hover");

    const whichAttributes = ["class", "fill", "stroke", "stroke-width"];
    const attributes = Array.from(element.attributes)
      .filter((attr) => whichAttributes.includes(attr.name))
      .map(
        (attr) =>
          `<div style="display: flex; margin: 4px;"><span style="color: #52525b; min-width: 40px;">${attr.name}: </span><span>${attr.value.replace("__nodebox_svg_hover", "").trim()}</span></div>`,
      )
      .join("");

    setPopup({
      content: attributes,
      x: e.clientX,
      y: e.clientY,
      visible: true,
    });
  }, []);

  const handleElementMouseMove = useCallback((e: MouseEvent) => {
    setPopup((prev) => ({
      ...prev,
      x: e.clientX,
      y: e.clientY,
    }));
  }, []);

  const handleElementMouseOut = useCallback((e: MouseEvent) => {
    const element = e.target as SVGElement;
    element.classList.remove("__nodebox_svg_hover");
    setPopup({ content: "", x: 0, y: 0, visible: false });
  }, []);

  const svgRef = useRef<SVGSVGElement>(null);

  useEffect(() => {
    if (!svgRef.current || !shapeElement) return;
    if (!showAttributes) return;

    const elements = svgRef.current.querySelectorAll("circle, ellipse, line, path, polygon, polyline, rect, text");

    elements.forEach((element) => {
      element.addEventListener("mouseover", handleElementMouseOver as EventListener);
      element.addEventListener("mousemove", handleElementMouseMove as EventListener);
      element.addEventListener("mouseout", handleElementMouseOut as EventListener);
    });

    return () => {
      elements.forEach((element) => {
        element.removeEventListener("mouseover", handleElementMouseOver as EventListener);
        element.removeEventListener("mousemove", handleElementMouseMove as EventListener);
        element.removeEventListener("mouseout", handleElementMouseOut as EventListener);
      });
    };
  }, [shapeElement, handleElementMouseOver, handleElementMouseMove, handleElementMouseOut]);

  return (
    <div className="flex-1 overflow-hidden bg-zinc-900 flex">
      <svg
        ref={svgRef}
        className="flex-1 outline-none"
        onMouseDown={handleMouseDown}
        onWheel={handleWheel}
        onContextMenu={handleContextMenu}
        tabIndex={-1}
      >
        <defs>
          <clipPath id="frame">
            <rect x={svgSize.left} y={svgSize.top} width={svgSize.width} height={svgSize.height} />
          </clipPath>
        </defs>
        <g clipPath="url(#frame)" transform={getTransform(-svgSize.left, -svgSize.top)}>
          {colorIsTransparent(item.background) ? null : (
            <rect
              x={svgSize.left}
              y={svgSize.top}
              width={svgSize.width}
              height={svgSize.height}
              fill={colorToCss(backgroundColor)}
            />
          )}
          {shapeElement}
        </g>
        {drawBounds && (
          <rect
            x={tx(0)}
            y={ty(0)}
            width={item.width * viewport.zoom}
            height={item.height * viewport.zoom}
            fill="none"
            stroke="#555"
          />
        )}
        {defsElement}
      </svg>
      {showAttributes && <Popup {...popup} />}
      <Menu open={!!contextMenu} onClose={() => setContextMenu(null)} anchorPosition={contextMenu || { x: 0, y: 0 }}>
        <MenuItem onClick={handleResetViewport}>Reset View</MenuItem>
      </Menu>
    </div>
  );
}

enum ViewerMode {
  Viewer = "viewer",
  Table = "table",
  Detail = "detail",
}

const GRAPHIC_TYPES = ["CIRCLE", "ELLIPSE", "GROUP", "LINE", "PATH", "POINT", "RECT", "TEXT"];

export default function Viewer() {
  const [activeTab, setActiveTab] = useState<ViewerMode>(ViewerMode.Viewer);
  const [showAttributes, setShowAttributes] = useState(false);
  const [drawPoints, setDrawPoints] = useState(false);
  const [drawBounds, setDrawBounds] = useState(false);
  const [viewRaw, setViewRaw] = useState(false);
  const [resultShape, setResultShape] = useState<Shape | null>(null);
  const [loading, _setLoading] = useState(false);
  const [selectedTableName, setSelectedTableName] = useState("Show table");
  const [selectedTableIndex, setSelectedTableIndex] = useState(0);

  function handleExportSvg() {
    const svgString = renderToSvg();
    if (!svgString) return;
    const blob = new Blob([svgString], { type: "image/svg+xml" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${projectId.value}-${currentItem.value!.name.toLowerCase()}.svg`;
    a.click();
    URL.revokeObjectURL(url);
  }

  function handleExportPng() {
    const svgString = renderToSvg();
    if (!svgString) return;
    const item = currentItem.value!;
    const img = new Image();
    const blob = new Blob([svgString], { type: "image/svg+xml" });
    const url = URL.createObjectURL(blob);

    img.onload = () => {
      const canvas = document.createElement("canvas");
      const scale = 2; // Double resolution
      canvas.width = item.width * scale;
      canvas.height = item.height * scale;
      const ctx = canvas.getContext("2d")!;
      // Use transparent if background is undefined
      ctx.fillStyle = item.background ? colorToCss(item.background) : "transparent";
      ctx.fillRect(0, 0, canvas.width, canvas.height);
      ctx.scale(scale, scale);
      ctx.drawImage(img, 0, 0);

      canvas.toBlob((blob) => {
        if (!blob) return;
        const url = URL.createObjectURL(blob);
        const a = document.createElement("a");
        a.href = url;
        a.download = `${projectId.value}-${currentItem.value!.name.toLowerCase()}.png`;
        a.click();
        URL.revokeObjectURL(url);
      }, "image/png");
    };

    img.src = url;
  }

  async function handleExportCsv() {
    let data: unknown[] | JSON = [];

    if (isVegaSpec(result.value)) {
      const tables = result.value.data || [];
      const selectedTable = tables[selectedTableIndex];
      if (selectedTable && hasInlineValues(selectedTable)) {
        data = selectedTable.values;
      }
    } else {
      if (Array.isArray(result.value)) {
        data = result.value;
      } else {
        toast("Exporting CSV is not supported for this type of data", { type: "error" });
        throw new Error("Exporting CSV is not supported for this type of data");
      }
    }
    if (data.length === 0) return;

    const headers = Object.keys(data[0] as object);
    const csvRows = [
      headers.join(","),
      ...data.map((row) => headers.map((header) => JSON.stringify((row as any)[header] ?? "")).join(",")),
    ];
    const csvString = csvRows.join("\n");

    const blob = new Blob([csvString], { type: "text/csv;charset=utf-8;" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${projectId.value}-${currentItem.value!.name.toLowerCase()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }

  async function handleExportJson() {
    let data: unknown[] | JSON = [];

    if (isVegaSpec(result.value)) {
      const tables = result.value.data || [];
      const selectedTable = tables[selectedTableIndex];
      if (selectedTable && hasInlineValues(selectedTable)) {
        data = selectedTable.values;
      }
    } else {
      if (Array.isArray(result.value)) data = result.value as [];
      else data = result.value as unknown as JSON;
    }

    const jsonString = JSON.stringify(data, null, 2);
    const blob = new Blob([jsonString], { type: "application/json" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `${projectId.value}-${currentItem.value!.name.toLowerCase()}.json`;
    a.click();
    URL.revokeObjectURL(url);
  }

  const item = currentItem.value;
  let resultType;
  if (result.value) {
    if (typeof result.value === "object" && "type" in result.value) {
      resultType = result.value.type;
    } else if (Array.isArray(result.value)) {
      resultType = "TABLE";
    } else {
      resultType = "OBJECT";
    }
  } else {
    resultType = "OBJECT";
  }

  function getTablesFromVegaSpec(spec: vega.Spec): MenuOption[] {
    if (!(spec && spec.data)) return [];
    return spec.data.map((d, i) => ({
      label: d.name || `Table ${i + 1}`,
      action: () => {
        setSelectedTableIndex(i);
        setSelectedTableName(d.name || `Table ${i + 1}`);
      },
    }));
  }

  const resultIsDrawable =
    (typeof resultType === "string" && GRAPHIC_TYPES.includes(resultType)) ||
    isValidElement(result.value) ||
    isVegaSpec(result.value);
  let realActiveTab = activeTab;
  if (activeTab === ViewerMode.Viewer && !resultIsDrawable) {
    realActiveTab = ViewerMode.Table;
  }

  useEffect(() => {
    if (item !== null && result.value !== null && realActiveTab === ViewerMode.Viewer && isVegaSpec(result.value)) {
      let shape;
      try {
        shape = renderVegaSpec(result.value);
      } catch (e: any) {
        console.error(e);
        nodeError.value = e;
        shape = null;
      }
      setResultShape(shape);
    } else {
      setResultShape(result.value as Shape);
    }
  }, [item, result.value, realActiveTab]);

  useEffect(() => {
    if (result.value && isVegaSpec(result.value)) {
      const tables = result.value.data || [];
      if (selectedTableIndex >= tables.length) {
        setSelectedTableIndex(0);
        setSelectedTableName(tables.length > 0 ? tables[0].name : "No tables");
      } else if (tables.length > 0) {
        setSelectedTableName(tables[selectedTableIndex].name);
      }
    }
  }, [result.value, selectedTableIndex]);
  let viewer;
  if (item === null || result.value === null) {
    viewer = (
      <div className="flex-1 flex justify-center items-center text-xs font-mono text-zinc-500">
        <span>No result</span>
      </div>
    );
  } else if (realActiveTab === ViewerMode.Viewer) {
    if (isValidElement(result.value)) {
      viewer = <HtmlViewer item={item} result={result} />;
    } else {
      if (loading) {
        viewer = <div>Loading...</div>;
      } else {
        viewer = (
          <CanvasViewer
            item={item}
            result={resultShape as Shape}
            showAttributes={showAttributes}
            drawPoints={drawPoints}
            drawBounds={drawBounds}
          />
        );
      }
    }
  } else if (realActiveTab === ViewerMode.Table) {
    if (isVegaSpec(result.value)) {
      const tables = result.value.data || [];
      const selectedTable = tables[selectedTableIndex];

      let data: unknown[] = [];
      if (selectedTable && hasInlineValues(selectedTable)) {
        // Handle inline data (with `values` field)
        data = selectedTable.values || [];
      } else {
        // Handle case where there is no inline data (e.g., external URL, source)
        console.warn("Selected table doesn't have inline data");
      }
      viewer = <TableViewer data={data} className="overflow-auto" style={{ height: "calc(100vh - 88px)" }} />;
    } else {
      viewer = <TableViewer data={result.value} className="overflow-auto" style={{ height: "calc(100vh - 88px)" }} />;
    }
  } else if (realActiveTab === ViewerMode.Detail) {
    viewer = <DetailViewer data={result.value} viewRaw={viewRaw} style={{ height: "calc(100vh - 88px)" }} />;
  }
  return (
    <div className="flex-1 h-full overflow-hidden flex flex-col">
      <header className="flex justify-between items-center gap-2 h-10 border-b border-b-zinc-700 text-xs overflow-hidden">
        <div className="flex">
          <button
            onClick={() => setActiveTab(ViewerMode.Viewer)}
            className={clsx("p-2 h-10", {
              "bg-zinc-700": realActiveTab === ViewerMode.Viewer,
              "opacity-20": !resultIsDrawable,
              "cursor-not-allowed": !resultIsDrawable,
            })}
          >
            Viewer
          </button>
          <button
            onClick={() => setActiveTab(ViewerMode.Table)}
            className={clsx("p-2 h-10", { "bg-zinc-700": realActiveTab === ViewerMode.Table })}
          >
            Table
          </button>
          <button
            onClick={() => setActiveTab(ViewerMode.Detail)}
            className={clsx("p-2 h-10", { "bg-zinc-700": realActiveTab === ViewerMode.Detail })}
          >
            Detail
          </button>
          <div className="border-l border-r border-zinc-700 px-2 h-10 flex items-center">
            <Icon name={playState.value === PlayState.Playing ? "pause" : "play"} onClick={togglePlay} size={24} />
          </div>
        </div>
        <div className="flex gap-2 pr-2">
          {realActiveTab === ViewerMode.Table && (
            <>
              {isVegaSpec(result.value) && (
                <ButtonMenu label={selectedTableName} options={getTablesFromVegaSpec(result.value)} />
              )}
              <ButtonMenu
                label="Export"
                options={[
                  { label: "CSV", action: handleExportCsv },
                  { label: "JSON", action: handleExportJson },
                ]}
              />
            </>
          )}
          {realActiveTab === ViewerMode.Viewer && (
            <>
              <ButtonMenu
                label="Export"
                options={[
                  {
                    label: "SVG",
                    action: () => {
                      handleExportSvg();
                    },
                  },
                  {
                    label: "PNG",
                    action: () => {
                      handleExportPng();
                    },
                  },
                ]}
              />
              <Toggle label="Attributes" value={showAttributes} onChange={setShowAttributes} />
              <Toggle label="Points" value={drawPoints} onChange={setDrawPoints} />
              <Toggle label="Bounds" value={drawBounds} onChange={setDrawBounds} />
            </>
          )}
          {realActiveTab === ViewerMode.Detail && (
            <>
              <Toggle label="Raw" value={viewRaw} onChange={setViewRaw} />
            </>
          )}
        </div>
      </header>
      {viewer}
      {nodeError.value && (
        <footer className="px-2 py-1 text-xs font-mono bg-red-800 border-t border-red-500">
          {formatError(nodeError.value! as DetailedError)}
        </footer>
      )}
    </div>
  );
}
