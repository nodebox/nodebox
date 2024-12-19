/**
 * Draw text.
 * @category Graphics
 */

import { Text, Group } from "@ndbx/g";

const ALIGN_MAP = {
  left: "start",
  middle: "middle",
  right: "end",
};

export default function (node) {
  const tableIn = node.tableIn({ name: "table" });
  const textIn = node.stringIn({ name: "text", value: "Hello" });
  const xIn = node.numberIn({ name: "x", value: 100 });
  const yIn = node.numberIn({ name: "y", value: 100 });
  const fontSizeIn = node.numberIn({ name: "font size", value: 12, min: 1 });
  const fontFamilyIn = node.stringIn({ name: "font family", value: "sans-serif" });
  const fontWeightIn = node.stringIn({ name: "font weight", value: "normal", choices: ["normal", "bold"] });
  const alignIn = node.stringIn({ name: "align", value: "left", choices: ["left", "middle", "right"] });
  const fillIn = node.colorIn({ name: "fill", value: "black" });
  const shapeOut = node.shapeOut({ name: "Out" });

  node.onRender = () => {
    if (!tableIn.value) {
      // Draw a single text element.
      const text = new Text(
        textIn.value,
        xIn.value,
        yIn.value,
        fontSizeIn.value,
        fontFamilyIn.value,
        fontWeightIn.value,
      );
      text.textAnchor = ALIGN_MAP[alignIn.value];
      text.fill = fillIn.value;
      shapeOut.set(text);
    } else {
      // Draw one text element per row.
      const [ftxt, fx, fy, ffs, fff, ffw, fa, ff] = [
        textIn.fn,
        xIn.fn,
        yIn.fn,
        fontSizeIn.fn,
        fontFamilyIn.fn,
        fontWeightIn.fn,
        alignIn.fn,
        fillIn.fn,
      ];
      const table = tableIn.value;
      const group = new Group();
      for (const d of table) {
        const text = new Text(ftxt(d), fx(d), fy(d), ffs(d), fff(d), ffw(d));
        text.textAnchor = ALIGN_MAP[fa(d)];
        text.fill = ff(d);
        group.add(text);
      }
      shapeOut.set(group);
    }
  };
}
