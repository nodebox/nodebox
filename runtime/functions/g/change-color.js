/**
 * Merge shapes together.
 *
 * @category Graphics
 */

import { Group, Paint } from "@ndbx/g";

export default function (node) {
  const shapeIn = node.shapeIn({ name: "shape" });

  const tagsIn = node.stringIn({ name: "tags", value: "" });

  const fillIn = node.colorIn({ name: "fill", value: "black" });
  const strokeIn = node.colorIn({ name: "stroke", value: "transparent" });
  const strokeWidthIn = node.numberIn({ name: "strokeWidth", value: 1, min: 0 });

  const shapeOut = node.shapeOut({ name: "out" });

  node.onRender = () => {
    let shape = shapeIn.value.clone();
    if (!shape) {
      shapeOut.set([]);
      return;
    }

    const colorizeCallback = (shape) => {
      shape.fill = fillIn.value;
      shape.stroke = strokeIn.value;
      shape.strokeWidth = strokeWidthIn.value;
    };

    try {
      if (shape.traverse) {
        shape.traverse(tagsIn.value.split(" "), (shape) => {
          colorizeCallback(shape);
        });
      } else if (shape.matchesTags(tagsIn.value)) {
        colorizeCallback(shape);
      }
    } catch (e) {
      console.error(e);
    }
    shapeOut.set(shape);
  };
}
