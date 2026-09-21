/**
 * Sort table data in ascending or descending order.
 *
 * - Natural mode:
 * - Numerical mode:
 * - Alphabethical mode:
 *
 * @category Table
 */

import { sort, group, ascending, descending } from "https://esm.sh/d3-array@3.2.4";

export default function (node) {
  const dataIn = node.tableIn({ name: "dataIn", label: "Data" });
  const sortByIn = node.stringIn({ name: "sortBy", label: "Sort by attributename(s)" });
  const groupByIn = node.stringIn({ name: "groupBy", label: "Group by attributename(s)" });
  const orderIn = node.stringIn({
    name: "order",
    label: "Order",
    value: "ascending",
    choices: ["ascending", "descending"],
  });
  const modeIn = node.stringIn({
    name: "mode",
    label: "Mode",
    value: "natural",
    choices: ["natural", "numerical", "alphabetical"],
  });
  const dataOut = node.tableOut({ name: "dataOut", label: "Data" });

  function sortData(data, sortBy = [], groupBy = [], order = "ascending", mode = "natural") {
    // Determine the sorting order
    const isAscending = order === "ascending";

    // Define the comparison function based on the mode
    const getComparisonFunction = (mode) => {
      switch (mode) {
        case "numerical":
          return (a, b) => ascending(+a, +b);
        case "alphabetical":
          return (a, b) => ascending(String(a), String(b));
        case "natural":
        default:
          return (a, b) => ascending(a, b);
      }
    };

    const compare = getComparisonFunction(mode);
    const compareFunction = isAscending ? compare : (a, b) => -compare(a, b);

    // Group data by specified attributes
    const groupedData = group(data, (d) => groupBy.map((attr) => d[attr]).join("|"));
    // Sort each group
    const sortedData = [];
    for (const group of groupedData.values()) {
      group.sort((a, b) => {
        for (const attr of sortBy) {
          const comp = compareFunction(a[attr], b[attr]);
          if (comp !== 0) return comp;
        }
        return 0;
      });

      sortedData.push(...group);
    }

    return sortedData;
  }

  node.onRender = () => {
    const data = dataIn.value ? structuredClone(dataIn.value) : [];
    const sortBy = sortByIn.value ? sortByIn.value.split(",").map((d) => d.trim()) : undefined;
    const groupBy = groupByIn.value ? groupByIn.value.split(",").map((d) => d.trim()) : undefined;
    const newData = sortData(data, sortBy, groupBy, orderIn.value, modeIn.value);

    dataOut.set(newData);
  };
}
