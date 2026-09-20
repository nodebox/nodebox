/**
 * Remove nested structures by collapsing hierarchical data into a single level with combined attribute names.
 * Flattening data does not change the number of records, it just changes the structure of attributes.
 *
 * @category Data Manipulation
 */

import { getNestedProperty } from "project:Utilities";

export default function (node) {
  const dataIn = node.tableIn({ name: "dataIn", label: "data" });

  node.pushSection({ name: "General" });
  const sourceTypeIn = node.stringIn({
    name: "sourceType",
    label: "Source type",
    value: "input",
    choices: [
      ["input", "Input port"],
      ["attribute", "Attribute"],
      ["string", "String"],
    ],
  });
  node.popSection();

  // Section: Flatten options
  node.pushSection({ name: "Flatten options" });
  const sepIn = node.stringIn({
    name: "sep",
    label: "Separator",
    value: "_", // Default separator for nested keys
  });
  const levelsIn = node.numberIn({
    name: "levels",
    label: "Flatten levels",
    value: 1, // Default to 1 level of flattening
    min: 1,
  });
  node.popSection();

  // Section: Source settings
  node.pushSection({ name: "Source" });
  const attrIn = node.stringIn({
    name: "attribute",
    label: "Attribute",
    value: "",
  });
  const sourceIn = node.stringIn({
    name: "sourceString",
    label: "Source string",
    widget: "TEXT",
    value: "",
  });
  node.popSection();

  // Function to flatten data
  function flattenData(data, parentKey = "", sep = "_", levels = 1, currentLevel = 0) {
    // Check type of data
    if (Array.isArray(data)) {
      // Recursively flatten each item in the array if it's an object
      return data.map((item) =>
        typeof item === "object" && item !== null ? flattenData(item, parentKey, sep, levels, currentLevel) : item,
      );
    } else if (typeof data === "object" && !Array.isArray(data)) {
      // Flatten object
      let result = {};
      for (let key in data) {
        if (data.hasOwnProperty(key)) {
          const fullKey = parentKey ? `${parentKey}${sep}${key}` : key;
          const value = data[key];

          // If the value is an object and currentLevel < levels, recursively flatten it
          if (value && typeof value === "object" && !Array.isArray(value) && currentLevel < levels) {
            const nested = flattenData(value, fullKey, sep, levels, currentLevel + 1);
            Object.assign(result, nested);
          }
          // If the value is an array, flatten the items inside if they are objects
          else if (Array.isArray(value)) {
            result[fullKey] = value.map((item) =>
              typeof item === "object" && item !== null ? flattenData(item, "", sep, levels, currentLevel + 1) : item,
            );
          }
          // Otherwise, handle primitive values
          else {
            result[fullKey] = value;
          }
        }
      }
      return result;
    } else {
      // If not an object/array, do not flatten
      return data;
    }
  }

  const dataOut = node.tableOut({ name: "dataOut", label: "data" });
  node.onRender = () => {
    const sourceType = sourceTypeIn.value;
    const sep = sepIn.value;
    const levels = levelsIn.value;
    const data = Array.isArray(dataIn.value) ? dataIn.value : [dataIn.value];

    let dataToFlatten;
    // Determine the source data based on sourceType
    switch (sourceType) {
      case "input":
        // Use the input port value as the source data
        dataToFlatten = data;
        break;

      case "attribute":
        // Flatten the specified attribute of the input data
        const attribute = attrIn.value;

        if (data && Array.isArray(data)) {
          dataToFlatten = data.flatMap((row) => getNestedProperty(row, attribute));
        }
        break;

      case "string":
        // Use the provided source string as the data to flatten
        const sourceString = sourceIn.value;
        try {
          dataToFlatten = [JSON.parse(sourceString)]; // Parse string as JSON
        } catch (e) {
          // If parsing fails, log an error and return
          //node.error(`Invalid JSON string: ${sourceString}`);
          dataOut.set([]);
        }
        break;

      default:
        node.error("Invalid source type.");
        dataOut.set([]);
        return;
    }

    // Ensure valid data to flatten
    if (!dataToFlatten || !Array.isArray(dataToFlatten)) {
      dataOut.set([]); // Set an empty array if no valid data
      return;
    }

    // Call flattenData to flatten the determined source data
    const flattenedData = flattenData(dataToFlatten, "", sep, levels);

    // Output the flattened data
    dataOut.set(flattenedData);
  };
}
