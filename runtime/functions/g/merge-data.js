/**
 * Combines two or more datasets into one merged dataset by appending or replacing record values.
 *
 * - append: All records of all datasets are appended to the merged dataset.
 * - first: All null or missing values in one dataset are replaced with the first non-null
 *          and non-missing value in the same location (attribute and row index) of another datasets.
 *          The merged dataset contains all the first non-null and non-missing values in
 *          all occuring locations in all the datasets.
 *
 * The optional select parameter allows to pass a JavaScript function
 * to define which attributes (or derivatives) to keep in the resulting merged dataset.
 *
 * @category Data Manipulation
 */

import { max } from "https://esm.sh/d3-array@3.2.4";

export default function (node) {
  const data1In = node.tableIn({ name: "data1", label: "Data 1" });
  const data2In = node.tableIn({ name: "data2", label: "Data 2" });
  const data3In = node.tableIn({ name: "data3", label: "Data 3" });
  const data4In = node.tableIn({ name: "data4", label: "Data 4" });
  const modeIn = node.stringIn({ name: "mode", label: "Mode", value: "append", choices: ["append", "first"] });
  const selectIn = node.stringIn({ name: "select", label: "Select", widget: "TEXT", value: "return { ...d };" });
  const dataOut = node.tableOut({ name: "dataOut", label: "Data" });

  function mergeAppend(datasets) {
    let merged = [];
    datasets.forEach((data) => {
      merged.push(...data);
    });
    return merged;
  }

  function mergeFirst(datasets) {
    let merged = [];
    const maxi = max(datasets.map((d) => d.length));
    for (let i = 0; i < maxi; i++) {
      datasets.forEach((data) => {
        if (i < merged.length && i < data.length) {
          Object.entries(data[i]).forEach(([key, value]) => (merged[i][key] = merged[i][key] ? merged[i][key] : value));
        } else if (i < data.length) {
          merged.push(data[i]);
        }
      });
    }
    return merged;
  }

  node.onRender = async () => {
    const datasets = [data1In.value, data2In.value, data3In.value, data4In.value].filter((d) => d);
    let merged;
    switch (modeIn.value) {
      case "append":
        merged = mergeAppend(datasets);
        break;
      case "first":
        merged = mergeFirst(datasets);
        break;
    }

    if (selectIn.value) {
      const fn = new Function("d", selectIn.value);
      merged = merged.map(fn);
    }

    dataOut.set(merged);
  };
}
