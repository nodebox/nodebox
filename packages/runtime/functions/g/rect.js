/**
 * Draw a rectangle.
 * @category Graphics
 */

import { Rect, Group } from "@ndbx/g";

export default function (node) {
  const tableIn = node.tableIn({ name: "table" });
  const xIn = node.numberIn({ name: "x", value: 0 });
  const yIn = node.numberIn({ name: "y", value: 0 });
  const widthIn = node.numberIn({ name: "width", value: 100, min: 0 });
  const heightIn = node.numberIn({ name: "height", value: 100, min: 0 });
  const fillIn = node.colorIn({ name: "fill", value: "black" });
  const strokeIn = node.colorIn({ name: "stroke", value: "transparent" });
  const strokeWidthIn = node.numberIn({ name: "strokeWidth", value: 1, min: 0 });
  const shapeOut = node.shapeOut({ name: "Out" });

  node.onRender = () => {
    if (!tableIn.value) {
      // Draw a single rect.
      const rect = new Rect(xIn.value, yIn.value, widthIn.value, heightIn.value);
      rect.fill = fillIn.value;
      rect.stroke = strokeIn.value;
      rect.strokeWidth = strokeWidthIn.value;
      shapeOut.set(rect);
    } else {
      const [fx, fy, fw, fh, ff, fs, fsw] = [
        xIn.fn,
        yIn.fn,
        widthIn.fn,
        heightIn.fn,
        fillIn.fn,
        strokeIn.fn,
        strokeWidthIn.fn,
      ];
      const table = tableIn.value;
      const group = new Group();
      for (const d of table) {
        const rect = new Rect(fx(d), fy(d), fw(d), fh(d));
        rect.fill = ff(d);
        rect.stroke = fs(d);
        rect.strokeWidth = fsw(d);
        group.add(rect);
      }
      shapeOut.set(group);
    }
  };
}
