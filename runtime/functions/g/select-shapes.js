/**
 * Pick tags from a shape.
 *
 * @category Graphics
 */

export default function (node) {
  const shapeIn = node.shapeIn({ name: "shape" });
  const operationIn = node.stringIn({
    name: "operation",
    choices: ["keep selected", "delete selected"],
    defaultValue: "union",
  });
  const tagsIn = node.stringIn({ name: "tags" });
  const shapeOut = node.shapeOut({ name: "matched" });
  const shapeOut2 = node.shapeOut({ name: "unmatched" });

  node.onRender = () => {
    if (!shapeIn.value) {
      shapeOut.set([]);
      return;
    }

    let shape = shapeIn.value.clone();
    let shape2 = shapeIn.value.clone();
    const operation = operationIn.value;
    let tags = tagsIn.value.split(" ");

    if (operation === "delete selected") {
      tags = tags.map((tag) => (tag.startsWith("!") ? tag.slice(1) : `!${tag}`));
    }

    if (shape && shape.selectShapes) {
      shape = shape.selectShapes(tags);

      tags = tags.map((tag) => (tag.startsWith("!") ? tag.slice(1) : `!${tag}`));
      shape2 = shape2.selectShapes(tags);

      shapeOut.set(shape);
      shapeOut2.set(shape2);
    } else {
      shapeOut.set([]);
      shapeOut2.set([]);
    }
  };
}
