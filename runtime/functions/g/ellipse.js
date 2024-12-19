/**
 * Draw an ellipse or circle.
 * @category Graphics
 */

import { Ellipse, Group } from "@ndbx/g";

export default function (node) {
  const tableIn = node.tableIn({ name: "table" });
  const cxIn = node.numberIn({ name: "cx", value: 100 });
  const cyIn = node.numberIn({ name: "cy", value: 100 });
  const rxIn = node.numberIn({ name: "rx", value: 75 });
  const ryIn = node.numberIn({ name: "ry", value: 75 });
  const fillIn = node.colorIn({ name: "fill", value: "black" });
  const strokeIn = node.colorIn({ name: "stroke", value: "transparent" });
  const strokeWidthIn = node.numberIn({ name: "strokeWidth", value: 1, min: 0 });
  const shapeOut = node.shapeOut({ name: "Out" });

  node.onRender = () => {
    if (!tableIn.value) {
      // Draw a single ellipse.
      const ellipse = new Ellipse(cxIn.value, cyIn.value, rxIn.value, ryIn.value);
      ellipse.fill = fillIn.value;
      ellipse.stroke = strokeIn.value;
      ellipse.strokeWidth = strokeWidthIn.value;
      shapeOut.set(ellipse);
    } else {
      // Draw one ellipse per row.
      const [fcx, fcy, frx, fry, ff, fs, fsw] = [
        cxIn.fn,
        cyIn.fn,
        rxIn.fn,
        ryIn.fn,
        fillIn.fn,
        strokeIn.fn,
        strokeWidthIn.fn,
      ];
      const table = tableIn.value;
      const group = new Group();
      for (const d of table) {
        const ellipse = new Ellipse(fcx(d), fcy(d), frx(d), fry(d));
        ellipse.fill = ff(d);
        ellipse.stroke = fs(d);
        ellipse.strokeWidth = fsw(d);
        group.add(ellipse);
      }
      shapeOut.set(group);
    }
  };
}
