import React, { useEffect, useState, useRef } from "react";
import { loadMainProject, config } from "./loaders";
import Context from "./context";
import { Item, Network, Node, LiteralValue } from "./types";

import { renderShape, renderDefs } from "./render";
import { evaluateItem } from "./evaluation";
import { Context as GraphicsContext, Paint, Shape } from "@ndbx/g";
import { markItemParameterDirty } from "./mutation";
import { Spec } from "vega";
import { vegaToShape } from "./vega-to-shape";

function isVegaSpec(value: unknown): value is Spec {
  return typeof value === "object" && value !== null && "$schema" in (value as Record<string, unknown>);
}

interface PlayerProps {
  userId: string;
  projectId: string;
  version?: string;
  item?: string;
  values?: Record<string, LiteralValue>;
  apiRoot?: string;
  publishedUrlTemplate?: string;
  assetsUrlTemplate?: string;
  onProjectLoaded?: (context: Context) => void;
  onProjectError?: (message: string) => void;
}

const NodeBoxPlayer: React.FC<PlayerProps> = ({
  userId,
  projectId,
  version,
  item,
  values,
  apiRoot,
  publishedUrlTemplate,
  assetsUrlTemplate,
  onProjectLoaded,
  onProjectError,
}) => {
  const contextRef = useRef<Context | undefined>();
  const [activeItem, setActiveItem] = useState<Item | undefined>();
  const [result, setResult] = useState<Shape | undefined>();

  useEffect(() => {
    (async () => {
      // Apply custom configuration values if provided
      if (apiRoot !== undefined) config.apiRoot = apiRoot;
      if (publishedUrlTemplate !== undefined) config.publishedUrlTemplate = publishedUrlTemplate;
      if (assetsUrlTemplate !== undefined) config.assetsUrlTemplate = assetsUrlTemplate;

      let cx;
      try {
        cx = await loadMainProject(userId, projectId, version || "published");
        contextRef.current = cx;
      } catch (e) {
        if (onProjectError) {
          const errorMessage = e instanceof Error ? e.message : String(e);
          onProjectError(errorMessage);
          return;
        } else {
          throw e;
        }
      }

      let projectItem;
      if (!item) {
        projectItem = cx.project.items[0];
      } else {
        projectItem = cx.project.items.find((i) => i.name === item) as Item;
      }
      if (!projectItem) {
        if (onProjectError) {
          onProjectError(`Item not found: ${item}`);
          return;
        } else {
          throw new Error(`Item not found: ${item}`);
        }
      }
      setActiveItem(projectItem);
      if (onProjectLoaded) {
        onProjectLoaded(cx);
      }
    })();
  }, [
    userId,
    projectId,
    item,
    version,
    apiRoot,
    publishedUrlTemplate,
    assetsUrlTemplate,
    onProjectLoaded,
    onProjectError,
  ]);

  useEffect(() => {
    (async () => {
      const cx = contextRef.current;
      if (!cx || !activeItem) return;
      if (activeItem.type === "NETWORK") {
        const network = activeItem as Network;
        if (values) {
          for (const [key, value] of Object.entries(values)) {
            markItemParameterDirty(cx, network, key);
          }
        }
      }
      const fqId = `self/self/${item}`;
      await evaluateItem(cx, fqId, activeItem, values);

      if (activeItem.type === "NETWORK") {
        const network = activeItem as Network;
        if (!network.outputPorts || network.outputPorts.length === 0) {
          const renderedNode = network.children.find((node) => node.id === network.renderedNode)! as Node;
          if (!renderedNode) {
            const errorMessage = `No rendered node found in network ${network.name}.`;
            console.error(errorMessage);
            onProjectError && onProjectError(errorMessage);
            return;
          }
          const renderedNodeFn = cx.lookupItemByName(renderedNode.fn);
          if (renderedNodeFn) {
            const outputs = renderedNodeFn.outputPorts.map((port) =>
              cx.portValues.get(`${renderedNode.id}/${port.name}`),
            );
            if (outputs.length > 0) setResult(outputs[0] as Shape);
          }
        } else {
          const outputs = network.outputPorts.map((port) => cx.portValues.get(`${network.id}/${port.name}`));
          if (outputs.length > 0) setResult(outputs[0] as Shape);
        }
      }
    })();
  }, [activeItem, values, onProjectError]);

  let shape = null;
  let shapeElement = null;
  let defsElement = null;
  let graphicsContext = new GraphicsContext();
  if (result) {
    shape = result;
    if (isVegaSpec(result)) {
      shape = vegaToShape(result);
    } else {
      shape = result;
    }
    try {
      shapeElement = renderShape(shape, graphicsContext);
      defsElement = renderDefs(graphicsContext);
    } catch (e) {
      console.error(e);
    }
  }

  if (!activeItem) {
    return React.createElement("div", {}, "loading....");
  }

  const style: React.CSSProperties = {};
  if (activeItem.background) {
    style.backgroundColor = Paint.parse(activeItem.background).toString();
  }

  let width = activeItem.width ?? 1000;
  let height = activeItem.height ?? 1000;
  if ((activeItem as Network).canvasSize === "auto" && shape) {
    const bounds = shape.getBounds();
    width = bounds.right - bounds.left;
    height = bounds.bottom - bounds.top;
  }

  return React.createElement(
    "div",
    { className: "ndbx-wrapper" },
    React.createElement("svg", { width, height, style }, shapeElement, defsElement),
  );
};

export default NodeBoxPlayer;
