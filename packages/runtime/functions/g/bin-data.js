/**
 * Bins quantitative values into consecutive, non-overlapping intervals.
 *
 * This node bins values from a specified attribute of a table into a given number of bins. If the bin count is non-zero,
 * the algorithm will use approximately that many bins. If the count is zero,
 * the number of bins is determined automatically using the Freedman-Diaconis rule.
 *
 * If "flatten" is toggled, the output table will contain all the original rows with an additional column named "binIndex".
 * Otherwise, the node will return a nested table where each bin has a binIndex, min, max and a nested values table.
 *
 * @category Data Transformation
 */

import { bin } from "https://esm.sh/d3-array@3.2.4";

export default function (node) {
  const tableIn = node.tableIn({ name: "table" });
  const attributeIn = node.stringIn({ name: "attribute", value: "value" });
  const binCountIn = node.numberIn({ name: "bins", value: 0 });
  const flattenIn = node.booleanIn({ name: "flatten", value: false });
  const tableOut = node.tableOut({ name: "out" });

  node.onRender = () => {
    const table = tableIn.value;
    const attribute = attributeIn.value;
    const binCount = binCountIn.value;

    if (!table || table.length === 0) {
      tableOut.set([]);
      return;
    }

    let binGenerator = bin().value((d) => d[attribute]);
    if (binCount > 0) {
      binGenerator = binGenerator.thresholds(binCount);
    }
    const bins = binGenerator(table);
    if (!flattenIn.value) {
      let newTable = bins.map((bin, index) => {
        return { min: bin.x0, max: bin.x1, values: bin, count: bin.length };
      });
      tableOut.set(newTable);
    } else {
      let newTable = [];
      bins.forEach((bin, index) => {
        bin.forEach((row) => {
          newTable.push({ ...row, binIndex: index });
        });
      });
      tableOut.set(newTable);
    }
  };
}
