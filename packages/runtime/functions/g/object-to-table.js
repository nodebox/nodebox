/**
 * Converts an object where each key represents an ID into a table format.
 * Each object value becomes a row in the table, with the key stored as an ID field.
 *
 * @category Data Transformation
 */

export default function (node) {
  // Input ports
  const objectIn = node.tableIn({ name: "object", label: "Input Object" });

  // Configuration parameters
  const idFieldNameIn = node.stringIn({
    name: "idFieldName",
    label: "ID field name",
    value: "id",
  });
  const nestingModeIn = node.stringIn({
    name: "nestingMode",
    label: "Nesting mode",
    value: "preserve",
    choices: [
      ["preserve", "Preserve nesting"],
      ["flatten", "Flatten"],
    ],
  });
  const fieldSeparatorIn = node.stringIn({
    name: "fieldSeparator",
    label: "Field separator",
    value: "_",
  });

  // Output port
  const tableOut = node.tableOut({ name: "out", label: "Table Output" });

  // Helper function to flatten nested objects if needed
  function flattenObject(obj, prefix = "", separator) {
    return Object.keys(obj).reduce((acc, key) => {
      const newKey = prefix ? `${prefix}${separator}${key}` : key;
      if (typeof obj[key] === "object" && obj[key] !== null) {
        return { ...acc, ...flattenObject(obj[key], newKey, separator) };
      }
      return { ...acc, [newKey]: obj[key] };
    }, {});
  }

  node.onRender = () => {
    const inputData = objectIn.value;
    if (!inputData) {
      tableOut.set([]);
      return;
    }

    const idFieldName = idFieldNameIn.value;
    const nestingMode = nestingModeIn.value;
    const separator = fieldSeparatorIn.value;

    // Transform the input object into an array of records
    const records = Object.entries(inputData).map(([key, value]) => {
      let record;

      if (nestingMode === "preserve") {
        // Keep nested structures as is
        record = { ...value };
      } else {
        // Flatten nested structures
        record = flattenObject(value, "", separator);
      }

      // Add the ID field
      record[idFieldName] = key;

      return record;
    });

    tableOut.set(records);
  };
}
