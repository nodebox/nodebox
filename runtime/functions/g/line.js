/**
 * Draw a line.
 * @category Graphics
 */

import { Line, Group } from "@ndbx/g";

export default function (node) {
  const tableIn = node.tableIn({ name: "table" });
  const x1In = node.numberIn({ name: "x1", value: 0 });
  const y1In = node.numberIn({ name: "y1", value: 0 });
  const x2In = node.numberIn({ name: "x2", value: 100 });
  const y2In = node.numberIn({ name: "y2", value: 100 });
  const strokeIn = node.colorIn({ name: "stroke", value: "black" });
  const strokeWidthIn = node.numberIn({ name: "strokeWidth", value: 1, min: 0 });
  const shapeOut = node.shapeOut({ name: "Out" });

  node.onRender = () => {
    if (!tableIn.value) {
      // Draw a single line.
      const line = new Line(x1In.value, y1In.value, x2In.value, y2In.value);
      line.stroke = strokeIn.value;
      line.strokeWidth = strokeWidthIn.value;
      shapeOut.set(line);
    } else {
      // Draw one line per row.
      const [fx1, fy1, fx2, fy2, fs, fsw] = [x1In.fn, y1In.fn, x2In.fn, y2In.fn, strokeIn.fn, strokeWidthIn.fn];
      const table = tableIn.value;
      const group = new Group();
      for (const d of table) {
        const line = new Line(fx1(d), fy1(d), fx2(d), fy2(d));
        line.stroke = fs(d);
        line.strokeWidth = fsw(d);
        group.add(line);
      }
      shapeOut.set(group);
    }
  };
}
