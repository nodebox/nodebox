/**
 * Slices the data array using JavaScript's slice() method, optionally grouping by attributes.
 *
 * This node slices the data based on the specified start and end indices.
 *
 * The optional select parameter allows to pass a JavaScript function
 * to define which attributes (or derivatives) to keep in the resulting sliced dataset.
 *
 * @category Data Manipulation
 */

import { group } from "https://esm.sh/d3-array@3.2.4";

export default function (node) {
  const dataIn = node.tableIn({ name: "dataIn", label: "Data" });
  const startIn = node.numberIn({ name: "start", label: "Start index" });
  const endIn = node.numberIn({ name: "end", label: "End index" }); // null means to slice till the end
  const groupByIn = node.stringIn({ name: "groupBy", label: "Group by attribute name(s)" });
  const selectIn = node.stringIn({ name: "select", label: "Select", widget: "TEXT", value: "return { ...d };" });
  const dataOut = node.tableOut({ name: "dataOut", label: "Data" });

  function sliceData(data, start, end, groupBy, selectFn) {
    let slicedData = [];

    if (groupBy && groupBy.length > 0) {
      const groupedData = group(data, (d) => groupBy.map((key) => d[key]).join("|"));
      groupedData.forEach((group) => {
        const groupValues = group.slice(start, end).map(selectFn);
        slicedData.push(...groupValues);
      });
    } else {
      slicedData = data.slice(start, end).map(selectFn);
    }

    return slicedData;
  }

  node.onRender = async () => {
    const data = dataIn.value ? structuredClone(dataIn.value) : [];
    const start = startIn.value ? startIn.value : 0;
    const end = endIn.value ? endIn.value : undefined; // undefined will slice till the end
    const groupBy = groupByIn.value ? groupByIn.value.split(",").map((attr) => attr.trim()) : [];
    const selectFn = new Function("d", selectIn.value);

    if (!data || data.length === 0) {
      dataOut.set([]);
      return;
    }

    const slicedData = sliceData(data, start, end, groupBy, selectFn);

    dataOut.set(slicedData);
  };
}
