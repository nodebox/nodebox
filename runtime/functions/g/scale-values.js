/**
 * Scales, normalizes or clips values in a specified attribute of a table.
 *
 * This node allows different types of scaling operations including normalization, standardization, and clipping.
 * - normalize: Scales the values to a specified range.
 * - standardize: Standardizes the values to have a mean of zero and unit variance.
 * - clip: Clips the values to the specified range.
 *
 * @category Data Transformation
 */

export default function (node) {
  const tableIn = node.tableIn({ name: "table" });
  const attributeIn = node.stringIn({ name: "attribute", value: "value" });
  const operationIn = node.stringIn({
    name: "operation",
    value: "scale",
    choices: ["scale", "standardize", "clip"],
  });
  const minIn = node.numberIn({ name: "min", value: 0 });
  const maxIn = node.numberIn({ name: "max", value: 1 });
  const tableOut = node.tableOut({ name: "out" });

  node.onRender = () => {
    const table = tableIn.value;
    const attribute = attributeIn.value;
    const operation = operationIn.value;
    const min = minIn.value;
    const max = maxIn.value;

    if (!table || table.length === 0) {
      tableOut.set([]);
      return;
    }

    if (!table.some((row) => row.hasOwnProperty(attribute))) {
      tableOut.set(table);
      return;
    }

    const values = table.map((row) => row[attribute]);

    switch (operation) {
      case "scale":
        scale(table, values, attribute, min, max);
        break;
      case "standardize":
        standardize(table, values, attribute);
        break;
      case "clip":
        clip(table, values, attribute, min, max);
        break;
      default:
        tableOut.set(table);
        break;
    }
  };

  function scale(table, values, attribute, min, max) {
    const minValue = Math.min(...values);
    const maxValue = Math.max(...values);
    const range = maxValue - minValue;
    const scaleRange = max - min;

    const scaledValues = values.map((value) => ((value - minValue) / range) * scaleRange + min);

    const newTable = table.map((row, index) => ({
      ...row,
      [attribute]: scaledValues[index],
    }));

    tableOut.set(newTable);
  }

  function standardize(table, values, attribute) {
    const mean = values.reduce((acc, value) => acc + value, 0) / values.length;
    const stdDev = Math.sqrt(
      values.map((value) => Math.pow(value - mean, 2)).reduce((acc, value) => acc + value, 0) / values.length,
    );

    const standardizedValues = values.map((value) => (value - mean) / stdDev);

    const newTable = table.map((row, index) => ({
      ...row,
      [attribute]: standardizedValues[index],
    }));

    tableOut.set(newTable);
  }

  function clip(table, values, attribute, clipMin, clipMax) {
    const clippedValues = values.map((value) => Math.min(Math.max(value, clipMin), clipMax));

    const newTable = table.map((row, index) => ({
      ...row,
      [attribute]: clippedValues[index],
    }));

    tableOut.set(newTable);
  }
}
