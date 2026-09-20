/**
 * Scales the values of a column from one range to another.
 *
 * This node scales the values of a column from one range to another. The input column must contain
 * numerical values. The output column will contain the scaled values. The scaling is done using a
 * linear scale, which is a common way to scale values. The linear scale is defined by a domain and a
 * range. The domain is the input range of values, and the range is the output range of values.
 *
 * The domain can be set manually or automatically. If set automatically, the extent of the domain
 * is based on the minimum and maximum values of the input column.
 *
 * By default, the input and output columns are named "value", meaning that the values of the input
 * column will be overwritten by the scaled values. You can change the names of the input and output
 * columns to something else.
 * @category Scales
 */

import { scaleLinear } from "https://esm.sh/d3-scale@4.0.2";
import { extent } from "https://esm.sh/d3-array@3.2.4";

export default function (node) {
  const tableIn = node.tableIn({ name: "table" });
  const inAttributeIn = node.stringIn({ name: "inAttribute", value: "value" });
  const outAttributeIn = node.stringIn({ name: "outAttribute", value: "value" });
  const domainTypeIn = node.stringIn({
    name: "domainType",
    value: "auto",
    choices: [
      ["auto", "Automatic"],
      ["manual", "Manual"],
    ],
  });
  const domainMinIn = node.numberIn({ name: "domainMin", value: 0 });
  const domainMaxIn = node.numberIn({ name: "domainMax", value: 1 });
  const rangeMinIn = node.numberIn({ name: "rangeMin", value: 0 });
  const rangeMaxIn = node.numberIn({ name: "rangeMax", value: 100 });
  const tableOut = node.tableOut({ name: "out" });

  node.onRender = () => {
    // Find the table and column
    const table = tableIn.value;
    if (!table) {
      tableOut.set([]);
      return;
    }
    const attr = inAttributeIn.value;
    // Check that at least some rows have the given attribute
    const hasAttribute = table.some((row) => row[attr] !== undefined);
    if (!hasAttribute) {
      tableOut.set(table);
      return;
    }

    // Scale the input values
    const inValues = table.map((row) => row[attr]);
    let min, max;
    if (domainTypeIn.value === "auto") {
      [min, max] = extent(inValues);
    } else {
      [min, max] = [domainMinIn.value, domainMaxIn.value];
    }
    const scale = scaleLinear().domain([min, max]).range([rangeMinIn.value, rangeMaxIn.value]);
    const newTable = table.map((row) => ({ ...row, [outAttributeIn.value]: scale(row[attr]) }));
    tableOut.set(newTable);
  };
}
