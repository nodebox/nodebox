import {
  Context,
  Shape,
  ShapeType,
  Circle,
  Rect,
  Line,
  Path,
  Group,
  Ellipse,
  Text,
  Bounds,
  LinearGradientPaint,
  RadialGradientPaint,
} from "@ndbx/g";
import { ReactSVGElement, createElement } from "react";
import { Spec } from "vega";
import { vegaToShape } from "./vega-to-shape";
import { Transform } from "@ndbx/g";

// Figma blue: #0D99FF
const cornerSquareSize = 1;
const colorBase: ColorAttributes = { fill: "none", stroke: "#1976D2", strokeWidth: 0.5 };
const colorCenter: ColorAttributes = { fill: "none", stroke: "#1976D2", strokeWidth: 0.5 };
const colorLines: ColorAttributes = { stroke: "#1976D2", strokeWidth: 0.5, fill: "none" };
const colorPoints: ColorAttributes = { fill: "none", stroke: "#1976D2", strokeWidth: 0.5 };

interface ColorAttributes {
  fill: string;
  stroke: string;
  strokeWidth: number;
}

interface PivotShape extends Shape {
  "data-pivot-mode"?: string;
}

function applyMatrixTransform(x: number, y: number, matrix: Float32Array): { x: number; y: number } {
  const [a, b, c, d, e, f] = matrix;

  return {
    x: a * x + c * y + e,
    y: b * x + d * y + f,
  };
}

function getPivotPoints(
  shape: Shape,
  bounds: Bounds,
  customPoints?: Array<{ x: number; y: number }>,
  key?: string | number,
): ReactSVGElement | null {
  const matrix = new Transform().matrix; //shape.transform.matrix;

  const defaultPoints = [
    { x: bounds.left, y: bounds.bottom },
    { x: bounds.left, y: bounds.top },
    { x: bounds.right, y: bounds.top },
    { x: bounds.right, y: bounds.bottom },
  ];

  const points = customPoints || defaultPoints;

  const center = [{ x: bounds.centerX, y: bounds.centerY }];
  const transformedPoints = points.map((point) => ({
    ...applyMatrixTransform(point.x, point.y, matrix),
  }));

  const transformedCenter = center.map((point) => ({
    ...applyMatrixTransform(point.x, point.y, matrix),
  }));
  const rectWidthHeight = cornerSquareSize + colorPoints.strokeWidth + colorLines.strokeWidth;

  const pointElements = transformedPoints.map((point, i) =>
    createElement("rect", {
      key: `point-${i}`,
      x: point.x - rectWidthHeight / 2,
      y: point.y - rectWidthHeight / 2,
      width: rectWidthHeight,
      height: rectWidthHeight,
      ...colorPoints,
      "data-tag": "point-corner",
    }),
  );
  const centerElements = transformedCenter.map((point, i) =>
    createElement("circle", {
      key: `cpoint-${i}`,
      cx: point.x,
      cy: point.y,
      r: 1,
      ...colorCenter,
      "data-tag": "point-center",
    }),
  );

  const lineElements = transformedPoints
    .map((point, i) => {
      const nextPoint = transformedPoints[(i + 1) % transformedPoints.length];
      const dx = nextPoint.x - point.x;
      const dy = nextPoint.y - point.y;

      const length = Math.hypot(dx, dy);

      if (length === 0) {
        return null;
      }

      const ux = dx / length;
      const uy = dy / length;

      const offsetDistance = rectWidthHeight / 2;

      const startX = point.x + ux * offsetDistance;
      const startY = point.y + uy * offsetDistance;
      const endX = nextPoint.x - ux * offsetDistance;
      const endY = nextPoint.y - uy * offsetDistance;

      return createElement("line", {
        key: `line-${i}`,
        x1: startX,
        y1: startY,
        x2: endX,
        y2: endY,
        ...colorLines,
        "data-tag": "line-corner",
      });
    })
    .filter((element) => element !== null);

  return createElement("g", { key }, ...lineElements, ...pointElements, ...centerElements);
}

function wrapShape(shape: Shape, element: ReactSVGElement, key?: string | number): ReactSVGElement {
  const bounds = shape.getBounds();
  return createElement("g", { key }, element, getPivotPoints(shape, bounds));
}

export function renderShape(
  shape: Shape,
  context?: Context,
  key?: string | number,
  showPoints: boolean = false,
  parentTransform: Transform = new Transform(),
): ReactSVGElement {
  if (shape.clipPath) {
    context?.clipPaths.push(shape.clipPath);
  }
  switch (shape.type) {
    case ShapeType.Circle: {
      const circle = shape as Circle;
      const element = createElement("circle", {
        key,
        id: circle.id,
        cx: circle.cx,
        cy: circle.cy,
        r: circle.radius,
        ...circle._getAttributes(context),
      });
      return showPoints ? wrapShape(circle, element, key) : element;
    }
    case ShapeType.Ellipse: {
      const ellipse = shape as Ellipse;
      const element = createElement("ellipse", {
        key,
        id: ellipse.id,
        cx: ellipse.cx,
        cy: ellipse.cy,
        rx: ellipse.rx,
        ry: ellipse.ry,
        ...ellipse._getAttributes(context),
      });
      return showPoints ? wrapShape(ellipse, element, key) : element;
    }
    case ShapeType.Group: {
      const groupShape = shape as Group;
      const currentTransform = parentTransform.combine(new Transform(groupShape.transform.matrix));
      const element = createElement(
        "g",
        { key, id: groupShape.id, ...groupShape._getAttributes() },
        groupShape.children.map((child, i) => renderShape(child, context, i, showPoints)),
      );
      if (!showPoints) return element;
      const bounds = groupShape.getBounds();
      let commonPoints: ReactSVGElement[] = [];
      if ((shape as PivotShape)["data-pivot-mode"]) {
        if ((shape as PivotShape)["data-pivot-mode"] === "on group") {
          const temp = getPivotPoints(groupShape, bounds);
          if (temp) commonPoints = [temp];
          showPoints = false;
        }
        if ((shape as PivotShape)["data-pivot-mode"] === "canvas") {
          showPoints = false;
        }
      }
      return createElement(
        "g",
        { key },
        createElement(
          "g",
          { ...groupShape._getAttributes(context) },
          groupShape.children
            .filter((c) => c?.type)
            .map((child, i) => renderShape(child as PivotShape, context, i, showPoints)),
        ),
        ...commonPoints,
      );
    }
    case ShapeType.Line: {
      const line = shape as Line;
      const element = createElement("line", {
        key,
        id: line.id,
        x1: line.x1,
        y1: line.y1,
        x2: line.x2,
        y2: line.y2,
        ...line._getAttributes(context),
      });
      return showPoints ? wrapShape(line, element, key) : element;
    }
    case ShapeType.Path: {
      const path = shape as Path;
      const element = createElement("path", {
        key,
        id: path.id,
        d: path.toPathData(),
        ...path._getAttributes(context),
      });
      return showPoints ? wrapShape(path, element, key) : element;
    }
    case ShapeType.Rect: {
      const rect = shape as Rect;
      const element = createElement("rect", {
        key,
        id: rect.id,
        x: rect.x,
        y: rect.y,
        width: rect.width,
        height: rect.height,
        ...rect._getAttributes(context),
      });
      return showPoints ? wrapShape(rect, element, key) : element;
    }
    case ShapeType.Text: {
      const text = shape as Text;
      const element = createElement(
        "text",
        {
          key,
          id: text.id,
          x: text.x,
          y: text.y,
          fontFamily: text.fontFamily,
          fontWeight: text.fontWeight,
          fontSize: text.fontSize,
          textAnchor: text.textAnchor,
          ...text._getAttributes(context),
        },
        text.text,
      );
      if (!showPoints) return element;
      const bounds = text.getBounds();

      // Define custom points including base points
      const customPoints = [
        { x: bounds.left, y: bounds.base },
        { x: bounds.left, y: bounds.top },
        { x: bounds.right, y: bounds.top },
        { x: bounds.right, y: bounds.base },
      ];

      return createElement("g", { key }, element, getPivotPoints(text, bounds));
    }
    default:
      throw new Error(`Unknown shape type: ${shape.type}`);
  }
}
export function dimensionForRenderShape(
  shape: PivotShape,
  useMatrix: Boolean = true,
  cumulBounds: Bounds = { left: Infinity, right: -Infinity, top: Infinity, bottom: -Infinity, centerX: 0, centerY: 0 },
): Bounds {
  switch (shape.type) {
    case ShapeType.Circle: {
      const circle = shape as Circle;
      const bounds = circle.getBounds();
      return bounds;
    }
    case ShapeType.Ellipse: {
      const ellipse = shape as Ellipse;
      const bounds = ellipse.getBounds();
      return bounds;
    }
    case ShapeType.Group: {
      const groupShape = shape as Group;
      if (groupShape.height && groupShape.width) {
        // height and width determined with parseSVG.
        const bounds = {
          left: Math.min(cumulBounds.left, 0),
          right: Math.max(cumulBounds.right, groupShape.width),
          top: Math.min(cumulBounds.top, 0),
          bottom: Math.max(cumulBounds.bottom, groupShape.height),
          centerX: (Math.min(cumulBounds.left, 0) + Math.max(cumulBounds.right, groupShape.width)) / 2,
          centerY: (Math.min(cumulBounds.top, 0) + Math.max(cumulBounds.bottom, groupShape.height)) / 2,
        };
        return bounds;
      } else if (
        (groupShape as PivotShape)["data-pivot-mode"] === undefined ||
        ((groupShape as PivotShape)["data-pivot-mode"] &&
          (groupShape as PivotShape)["data-pivot-mode"] === "per element")
      ) {
        const groupBounds = groupShape.children.reduce((accBounds: Bounds, child) => {
          const childBounds = dimensionForRenderShape(child as PivotShape);
          return {
            left: Math.min(accBounds.left, childBounds.left),
            right: Math.max(accBounds.right, childBounds.right),
            top: Math.min(accBounds.top, childBounds.top),
            bottom: Math.max(accBounds.bottom, childBounds.bottom),
            centerX: (Math.min(accBounds.left, childBounds.left) + Math.max(accBounds.right, childBounds.right)) / 2,
            centerY: (Math.min(accBounds.top, childBounds.top) + Math.max(accBounds.bottom, childBounds.bottom)) / 2,
          };
        }, cumulBounds);

        cumulBounds = {
          left: Math.min(cumulBounds.left, groupBounds.left),
          right: Math.max(cumulBounds.right, groupBounds.right),
          top: Math.min(cumulBounds.top, groupBounds.top),
          bottom: Math.max(cumulBounds.bottom, groupBounds.bottom),
          centerX: (Math.min(cumulBounds.left, groupBounds.left) + Math.max(cumulBounds.right, groupBounds.right)) / 2,
          centerY: (Math.min(cumulBounds.top, groupBounds.top) + Math.max(cumulBounds.bottom, groupBounds.bottom)) / 2,
        };

        return cumulBounds;
      } else if ((groupShape as PivotShape)["data-pivot-mode"] === "on group") {
        // get the regular bounds of each element, apply group transformation
        const groupBounds = groupShape.children.reduce((accBounds: Bounds, child) => {
          const childBounds = dimensionForRenderShape(child as PivotShape, false);
          return {
            left: Math.min(accBounds.left, childBounds.left),
            right: Math.max(accBounds.right, childBounds.right),
            top: Math.min(accBounds.top, childBounds.top),
            bottom: Math.max(accBounds.bottom, childBounds.bottom),
            centerX: (Math.min(accBounds.left, childBounds.left) + Math.max(accBounds.right, childBounds.right)) / 2,
            centerY: (Math.min(accBounds.top, childBounds.top) + Math.max(accBounds.bottom, childBounds.bottom)) / 2,
          };
        }, cumulBounds);

        const matrix = groupShape.transform.matrix;

        const topLeft = Transform.applyMatrixTransform(groupBounds.left, groupBounds.top, matrix);
        const topRight = Transform.applyMatrixTransform(groupBounds.right, groupBounds.top, matrix);
        const bottomLeft = Transform.applyMatrixTransform(groupBounds.left, groupBounds.bottom, matrix);
        const bottomRight = Transform.applyMatrixTransform(groupBounds.right, groupBounds.bottom, matrix);

        // Find the min and max of the transformed corners for bounding box
        const left = Math.min(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x);
        const right = Math.max(topLeft.x, topRight.x, bottomLeft.x, bottomRight.x);
        const top = Math.min(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y);
        const bottom = Math.max(topLeft.y, topRight.y, bottomLeft.y, bottomRight.y);

        // Center points can be calculated based on the new bounding box
        const centerX = (left + right) / 2;
        const centerY = (top + bottom) / 2;

        cumulBounds = {
          left: Math.min(cumulBounds.left, left),
          right: Math.max(cumulBounds.right, right),
          top: Math.min(cumulBounds.top, top),
          bottom: Math.max(cumulBounds.bottom, bottom),
          centerX: (Math.min(cumulBounds.left, left) + Math.max(cumulBounds.right, right)) / 2,
          centerY: (Math.min(cumulBounds.top, top) + Math.max(cumulBounds.bottom, bottom)) / 2,
        };

        return cumulBounds;
      }
    }
    case ShapeType.Line: {
      const line = shape as Line;
      const bounds = line.getBounds();
      return bounds;
    }
    case ShapeType.Path: {
      const path = shape as Path;
      const bounds = path.getBounds();
      return bounds;
    }
    case ShapeType.Rect: {
      const rect = shape as Rect;
      const bounds = rect.getBounds();
      return bounds;
    }
    case ShapeType.Text: {
      const text = shape as Text;
      const bounds = text.getTransformedBounds(); // : text.getBounds();
      return bounds;
    }
    default:
      // throw new Error(`Unknown shape type: ${shape.type}`);
      return { left: 0, right: 0, top: 0, bottom: 0, centerX: 0, centerY: 0 };
  }
}

export function renderVegaSpec(spec: Spec): Shape {
  return vegaToShape(spec);
}

export function renderDefs(context: Context): ReactSVGElement | null {
  if (context.clipPaths.length === 0 && context.gradients.length === 0) return null;

  const clipPaths = context.clipPaths.map((clipPath) =>
    createElement(
      "clipPath",
      {
        id: clipPath.id,
      },
      renderShape(clipPath.shape),
    ),
  );

  const gradients = context.gradients.map((gradient) => {
    const stops = gradient.stops.map((stop, index) =>
      createElement("stop", {
        key: index,
        offset: `${stop.offset * 100}%`,
        stopColor: stop.color,
      }),
    );
    const attrs: Record<string, string> = {};
    if (gradient.type === "linearGradient") {
      const linear = gradient as LinearGradientPaint;
      attrs.x1 = `${linear.x1 * 100}%`;
      attrs.y1 = `${linear.y1 * 100}%`;
      attrs.x2 = `${linear.x2 * 100}%`;
      attrs.y2 = `${linear.y2 * 100}%`;
    } else if (gradient.type === "radialGradient") {
      const radial = gradient as RadialGradientPaint;
      attrs.cx = `${radial.cx * 100}%`;
      attrs.cy = `${radial.cy * 100}%`;
      attrs.r = `${radial.r * 100}%`;
    }
    return createElement(
      gradient.type,
      {
        key: gradient.id,
        id: gradient.id,
        ...attrs,
      },
      ...stops,
    );
  });

  return createElement("defs", {}, ...clipPaths, ...gradients);
}
