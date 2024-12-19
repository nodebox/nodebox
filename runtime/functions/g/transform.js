/**
 * Translate, rotate and/or scale a set of shapes.
 *
 * This node takes shapes and applies a transformation to each shape.
 * The transformation is defined by the translation, rotation and scale parameters.
 */

import { Transform } from "@ndbx/g";

export default function (node) {
  const shapeIn = node.shapeIn({ name: "shapes" });

  const txIn = node.numberIn({ label: "translate x", name: "translate x", value: 0 });
  const tyIn = node.numberIn({ label: "y", name: "translate y", value: 0 });
  const sxIn = node.numberIn({ label: "scale x", name: "scale x", value: 1, step: 0.01 });
  const syIn = node.numberIn({ label: "y", name: "scale y", value: 1, step: 0.01 });

  const rotateIn = node.numberIn({ name: "rotate", value: 0, min: -360, max: 360 });
  node.pushSection("Pivot");
  const transformModeIn = node.choiceIn({
    name: "mode",
    value: "per element",
    choices: ["per element", "on group", "canvas"],
  });
  const pivotPointIn = node.choiceIn({
    name: "pivot point",
    value: "center",
    choices: [
      "bottom-left",
      "bottom-center",
      "bottom-right",
      "center-left",
      "center",
      "center-right",
      "top-left",
      "top-center",
      "top-right",
    ],
  });
  node.popSection();

  const shapeOut = node.shapeOut({ name: "out" });

  const getRotationPivot = (bounds, pivot) => {
    const { left, right, top, bottom, centerX, centerY } = bounds;
    switch (pivot) {
      case "bottom-left":
        return { x: left, y: bottom };
      case "bottom-center":
        return { x: centerX, y: bottom };
      case "bottom-right":
        return { x: right, y: bottom };
      case "top-left":
        return { x: left, y: top };
      case "top-center":
        return { x: centerX, y: top };
      case "top-right":
        return { x: right, y: top };
      case "center-left":
        return { x: left, y: centerY };
      case "center":
        return { x: centerX, y: centerY };
      case "center-right":
        return { x: right, y: centerY };
      default:
        return { x: centerX, y: centerY };
    }
  };

  node.onRender = () => {
    const [tx, ty, r, sx, sy, rg, pp] = [
      txIn.value,
      tyIn.value,
      rotateIn.value,
      sxIn.value,
      syIn.value,
      transformModeIn.value,
      pivotPointIn.value,
    ];
    const shape = shapeIn.value;
    if (!shape) {
      shapeOut.set([]);
      return;
    }

    const transform = new Transform();
    const newShape = shape.clone();
    transform["pivot-mode"] = rg;
    transform["pivot-point"] = pp;

    if (rg === "on group" || !(newShape.children && newShape.children.length > 0)) {
      const bounds = newShape.getBounds();
      const pivot = getRotationPivot(bounds, pp);
      transform
        .rotate((r * Math.PI) / 180, pivot)
        .scale(sx, sy, pivot)
        .translate(tx, ty);
      newShape.applyTransform(transform);
    } else if (rg === "per element") {
      if (newShape.children && newShape.children.length > 0) {
        newShape.children.forEach((child) => {
          const bounds = child.getBounds();
          const pivot = getRotationPivot(bounds, pp);
          const rotation = new Transform();

          rotation
            .rotate((r * Math.PI) / 180, pivot)
            .scale(sx, sy, pivot)
            .translate(tx, ty);

          child.applyTransform(rotation);
        });
      }
    } else if (rg === "canvas") {
      const nodeId = node.nodeId;
      const items = node.cx.project.items;
      const network = items.find((item) => item.children.some((child) => child.id === nodeId));
      const bounds = {
        left: 0,
        right: network.width,
        top: 0,
        bottom: network.height,
        centerX: network.width / 2,
        centerY: network.height / 2,
      };
      const pivot = getRotationPivot(bounds, pp);
      transform
        .rotate((r * Math.PI) / 180, pivot)
        .scale(sx, sy, pivot)
        .translate(tx, ty);
      newShape.applyTransform(transform);
    } else {
      throw new Error("invalid option");
    }
    shapeOut.set(newShape);
  };
}
