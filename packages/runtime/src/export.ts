import React, { ReactElement } from "react";
import ReactDOMServer from "react-dom/server";
import { Bounds, Context as GraphicsContext } from "@ndbx/g";
import { Spec } from "vega";
import { Item, Network, Color } from "./types";
import { renderDefs, renderShape, renderVegaSpec } from "./render";

function isVegaSpec(value: unknown): value is Spec {
  return typeof value === "object" && value !== null && "$schema" in (value as Record<string, unknown>);
}

function colorToCss(color: Color): string {
  const r = (v: number) => Math.round(v * 255);
  return color.a === 1
    ? `rgb(${r(color.r)} ${r(color.g)} ${r(color.b)})`
    : `rgb(${r(color.r)} ${r(color.g)} ${r(color.b)} / ${color.a})`;
}

function colorIsTransparent(color?: Color): boolean {
  if (!color || typeof color.a !== "number") return true;
  return color.a === 0;
}

export interface RenderOptions {
  drawPoints?: boolean;
  includeBackground?: boolean; // default: true if background is non-transparent
}

export interface PngOptions extends RenderOptions {
  scale?: number; // default: 2
}

export function renderItemToSvgElement(
  item: Item,
  resultValue: unknown,
  options: RenderOptions = {},
): ReactElement | undefined {
  const includeBackground = options.includeBackground ?? true;

  // Normalize the result to a drawable shape or React element
  let shapeOrElement: any = resultValue;
  if (isVegaSpec(resultValue)) {
    shapeOrElement = renderVegaSpec(resultValue);
  }

  const graphicsContext = new GraphicsContext();

  let shapeElement: ReactElement | null = null;
  let defsElement: ReactElement | null = null;
  try {
    if (shapeOrElement && (shapeOrElement.type || React.isValidElement(shapeOrElement))) {
      if (React.isValidElement(shapeOrElement)) {
        shapeElement = shapeOrElement as ReactElement;
      } else {
        shapeElement = renderShape(shapeOrElement, graphicsContext, undefined, !!options.drawPoints) as any;
      }
      const defs = renderDefs(graphicsContext);
      defsElement = defs as any;
    }
  } catch (e) {
    console.error("renderItemToSvgElement: render error", e);
    return undefined;
  }

  if (!shapeElement) return undefined;

  // Determine SVG size and origin
  let left = 0,
    top = 0,
    width = (item.width ?? 1000) as number,
    height = (item.height ?? 1000) as number;
  const maybeNetwork = item as Network;
  if (typeof (shapeOrElement as any)?.getBounds === "function" && maybeNetwork.canvasSize === "auto") {
    const auto: Bounds = (shapeOrElement as any).getBounds();
    left = auto.left;
    top = auto.top;
    width = auto.right - auto.left;
    height = auto.bottom - auto.top;
  }

  const backgroundColor = item.background;

  const svg = React.createElement(
    "svg",
    { width, height, xmlns: "http://www.w3.org/2000/svg" },
    React.createElement(
      "defs",
      {},
      React.createElement("clipPath", { id: "frame" }, React.createElement("rect", { x: left, y: top, width, height })),
    ),
    React.createElement(
      "g",
      {
        clipPath: "url(#frame)",
        transform: `translate(${-left}, ${-top})`,
      },
      includeBackground && !colorIsTransparent(backgroundColor)
        ? React.createElement("rect", {
            x: left,
            y: top,
            width,
            height,
            fill: colorToCss(backgroundColor),
          })
        : null,
      shapeElement,
      defsElement,
    ),
  );

  return svg;
}

export function renderItemToSvgString(
  item: Item,
  resultValue: unknown,
  options: RenderOptions = {},
): string | undefined {
  const element = renderItemToSvgElement(item, resultValue, options);
  if (!element) return undefined;
  return ReactDOMServer.renderToStaticMarkup(element);
}

export async function svgStringToPngBlob(
  svgString: string,
  width: number,
  height: number,
  options: PngOptions = {},
): Promise<Blob> {
  const scale = options.scale ?? 2;
  return new Promise<Blob>((resolve, reject) => {
    try {
      const img = new Image();
      const svgBlob = new Blob([svgString], { type: "image/svg+xml" });
      const svgUrl = URL.createObjectURL(svgBlob);

      img.onload = () => {
        try {
          const canvas = document.createElement("canvas");
          canvas.width = Math.max(1, Math.round(width * scale));
          canvas.height = Math.max(1, Math.round(height * scale));
          const ctx = canvas.getContext("2d");
          if (!ctx) throw new Error("2D context not available");
          ctx.scale(scale, scale);
          ctx.drawImage(img, 0, 0);
          canvas.toBlob((blob) => {
            URL.revokeObjectURL(svgUrl);
            if (!blob) return reject(new Error("PNG blob creation failed"));
            resolve(blob);
          }, "image/png");
        } catch (err) {
          URL.revokeObjectURL(svgUrl);
          reject(err);
        }
      };
      img.onerror = (e) => {
        URL.revokeObjectURL(svgUrl);
        reject(new Error("Failed to load SVG into image"));
      };
      img.src = svgUrl;
    } catch (e) {
      reject(e as Error);
    }
  });
}

export async function renderItemToPngBlob(
  item: Item,
  resultValue: unknown,
  options: PngOptions = {},
): Promise<Blob | undefined> {
  // First, render to SVG and capture final dimensions
  const maybeNetwork = item as Network;
  let left = 0,
    top = 0,
    width = (item.width ?? 1000) as number,
    height = (item.height ?? 1000) as number;

  let shapeOrElement: any = resultValue;
  if (isVegaSpec(resultValue)) {
    shapeOrElement = renderVegaSpec(resultValue);
  }

  if (typeof (shapeOrElement as any)?.getBounds === "function" && maybeNetwork.canvasSize === "auto") {
    const auto: Bounds = (shapeOrElement as any).getBounds();
    left = auto.left;
    top = auto.top;
    width = auto.right - auto.left;
    height = auto.bottom - auto.top;
  }

  const svg = renderItemToSvgString(item, resultValue, options);
  if (!svg) return undefined;
  return svgStringToPngBlob(svg, width, height, options);
}
