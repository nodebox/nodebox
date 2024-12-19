/**
 * Merge shapes together.
 *
 * @category Graphics
 */

import { Group } from "@ndbx/g";

export default function (node) {
  const shape1In = node.shapeIn({ name: "shape 1" });
  const shape2In = node.shapeIn({ name: "shape 2" });
  const shape3In = node.shapeIn({ name: "shape 3" });
  const shape4In = node.shapeIn({ name: "shape 4" });
  const shape5In = node.shapeIn({ name: "shape 5" });
  const shapeOut = node.shapeOut({ name: "out" });

  node.onRender = () => {
    const shapes = [shape1In, shape2In, shape3In, shape4In, shape5In];
    const g = new Group();
    g.tags = ["_merged"];
    for (let shape of shapes) {
      if (shape.value) {
        g.children.push(shape.value);
      }
    }
    shapeOut.set(g);
  };
}
