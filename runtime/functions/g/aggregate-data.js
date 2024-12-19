/**
 * Aggregates data based on a specified operation, optionally grouping by one or more attributes.
 *
 * This node aggregates data based on a specified operation, such as sum, min, max, mean, count,
 * median, variance, deviation, extent, or mode.
 *
 * - sum: Computes the sum of the values.
 * - count: Computes the number of values.
 * - min: Computes the minimum value.
 * - max: Computes the maximum value.
 * - extent: Computes both the minimum and maximum values (columns will be called `column_min` and `column_max`).
 * - mean: Computes the mean (average) value.
 * - median: Computes the median value.
 * - variance: Computes the variance.
 * - deviation: Computes the standard deviation.
 * - mode: Computes the mode, i.e. the most frequent value.
 *
 * If one or more groupBy attributes are specified, the data is grouped by these attributes before aggregation.
 *
 * @category Data Manipulation
 */

import { group, sum, min, max, mean, median, variance, deviation, extent, mode } from "https://esm.sh/d3-array@3.2.4";

export default function (node) {
  const dataIn = node.tableIn({ name: "data", label: "data" });

  node.pushSection({ name: "General" });
  const groupByIn = node.stringIn({ name: "groupBy", label: "Group by", value: "" });
  const operationIn = node.stringIn({
    name: "operation",
    label: "Operation",
    value: "sum",
    choices: [
      "sum",
      "count",
      "countunique",
      "min",
      "max",
      "extent",
      "mean",
      "median",
      "variance",
      "deviation",
      "mode",
      "first",
      "last",
      "<multiple>",
    ],
  });
  node.popSection();
  node.pushSection({ name: "Multiple operations" });
  const opListIn = node.stringIn({ name: "operations", label: "Operation(s)", value: "" });
  const attrListIn = node.stringIn({ name: "attributes", label: "Attribute(s)", value: "" });
  const dataOut = node.tableOut({ name: "dataOut", label: "data" });
  node.popSection();

  const countDistinct = (data, attr) => {
    const iterable = data.map((d) => d[attr]);
    return new Set(iterable).size;
  };

  const aggregateValues = (values, operations, attributes) => {
    const aggregatedRow = {};
    const cnt = values.length;
    // Helper function to filter valid values
    const filterValidValues = (values, attr) =>
      values.map((d) => d[attr]).filter((v) => v !== null && v !== undefined && !(typeof v === "number" && isNaN(v)));

    attributes.forEach((attr, i) => {
      i = operations.length == 1 ? 0 : i;
      const validValues = filterValidValues(values, attr); // Filter valid values for first and last
      if (typeof values[0][attr] === "number") {
        switch (operations[i]) {
          case "count":
            aggregatedRow[`${attr}_count`] = cnt;
            break;
          case "countunique":
            aggregatedRow[`${attr}_countunique`] = cnt;
            break;
          case "sum":
            aggregatedRow[`${attr}_sum`] = sum(values, (d) => d[attr]);
            break;
          case "min":
            aggregatedRow[`${attr}_min`] = min(values, (d) => d[attr]);
            break;
          case "max":
            aggregatedRow[`${attr}_max`] = max(values, (d) => d[attr]);
            break;
          case "mean":
            aggregatedRow[`${attr}_mean`] = mean(values, (d) => d[attr]);
            break;
          case "median":
            aggregatedRow[`${attr}_median`] = median(values, (d) => d[attr]);
            break;
          case "variance":
            aggregatedRow[`${attr}_variance`] = variance(values, (d) => d[attr]);
            break;
          case "deviation":
            aggregatedRow[`${attr}_deviation`] = deviation(values, (d) => d[attr]);
            break;
          case "extent":
            const [minValue, maxValue] = extent(values, (d) => d[attr]);
            aggregatedRow[`${attr}_min`] = minValue;
            aggregatedRow[`${attr}_max`] = maxValue;
            break;
          case "mode":
            aggregatedRow[`${attr}_mode`] = mode(values, (d) => d[attr]);
            break;
          case "first":
            aggregatedRow[`${attr}_first`] = validValues.length > 0 ? validValues[0] : null;
            break;
          case "last":
            aggregatedRow[`${attr}_last`] = validValues.length > 0 ? validValues[validValues.length - 1] : null;
            break;
          default:
            aggregatedRow[`${attr}_count`] = cnt;
        }
      } else {
        switch (operations[i]) {
          case "count":
            aggregatedRow[`${attr}_count`] = cnt;
            break;
          case "countunique":
            aggregatedRow[`${attr}_countunique`] = cnt;
            break;
          case "first":
            aggregatedRow[`${attr}_first`] = validValues.length > 0 ? validValues[0] : null;
            break;
          case "last":
            aggregatedRow[`${attr}_last`] = validValues.length > 0 ? validValues[validValues.length - 1] : null;
            break;
          default:
            aggregatedRow[`${attr}_first`] = values[0][attr];
        }
      }
    });
    return aggregatedRow;
  };

  node.onRender = () => {
    if (dataIn.value) {
      const data = dataIn.value;
    } else {
      dataOut.set([]);
      return;
    }
    const data = dataIn.value ? dataIn.value : [];
    const operation = operationIn.value;
    let groupByKeys = groupByIn.value ? groupByIn.value.split(",").map((info) => info.trim()) : [];
    const opList =
      operation === "<multiple>" ? opListIn.value.split(",").map((info) => info.trim()) : [operationIn.value];
    const attrList =
      operation === "<multiple>"
        ? attrListIn.value.split(",").map((info) => info.trim())
        : Object.keys(data[0]).filter((key) => !groupByKeys.includes(key));

    if (!data || data.length === 0) {
      dataOut.set([]);
      return;
    }

    //const attributes = Object.keys(data[0])//.filter((key) => typeof data[0][key] === "number");

    if (attrList.length === 0 && operation !== "count") {
      dataOut.set([]);
      return;
    }

    let aggregatedData;

    if (groupByKeys.length > 0) {
      const grouped = group(data, (d) => groupByKeys.map((key) => d[key]).join("|"));
      aggregatedData = Array.from(grouped, ([key, values]) => {
        const keyValues = key.split("|");
        const aggregatedRow = {};
        groupByKeys.forEach((key, index) => {
          aggregatedRow[key] = keyValues[index];
        });
        Object.assign(aggregatedRow, aggregateValues(values, opList, attrList));
        return aggregatedRow;
      });
    } else {
      aggregatedData = [aggregateValues(data, opList, attrList)];
    }

    dataOut.set(aggregatedData);
  };
}
