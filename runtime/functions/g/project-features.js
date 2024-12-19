/**
 * Converts the TopoJSON features to X/Y positions using a specified projection.
 *
 * @category Geo
 */
import { geoCentroid, geoDistance } from "https://esm.sh/d3-geo@3.1.1";

export default function (node) {
  const featuresIn = node.tableIn({ name: "features" });
  const projectionIn = node.tableIn({ name: "projection" });
  const tableOut = node.tableOut({ name: "out" });

  node.onRender = () => {
    if (!featuresIn.value) {
      tableOut.set([]);
      return;
    }
    const rows = [];
    const projection = projectionIn.value;
    let center = projection.rotate();
    center[0] *= -1;
    center[1] *= -1;
    for (const row of featuresIn.value) {
      const c = geoCentroid(row);
      const angle = geoDistance(center, c);
      if (angle > Math.PI / 2) continue;

      const [x, y] = projection(c);
      const newRow = { ...row, x, y };
      rows.push(newRow);
    }
    tableOut.set(rows);
  };
}
