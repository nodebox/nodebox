/**
 * Explode nested arrays in data by expanding each array element into a separate record,
 * duplicating non-array fields.
 *
 * @category Data Manipulation
 */
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

  // Section: Explode options
  node.pushSection({ name: "Explode options" });
  const attrIn = node.stringIn({
    name: "attribute",
    label: "Attribute to explode",
    value: "",
  });
  node.popSection();

  // Section: Source settings
  node.pushSection({ name: "Source" });
  const sourceIn = node.stringIn({
    name: "sourceString",
    label: "Source string",
    widget: "TEXT",
    value: "",
  });
  node.popSection();

  // Function to explode data based on the array in the specified attribute
  function explodeData(data, attribute = "") {
    let result = [];

    // If no attribute is provided, explode the entire input object
    if (!attribute) {
      data.forEach((item) => {
        // Check for any arrays in the object and explode them
        const exploded = Object.keys(item).reduce((acc, key) => {
          const value = item[key];
          if (Array.isArray(value)) {
            // Duplicate the object for each array element
            value.forEach((arrayItem) => {
              const newItem = { ...item, [key]: arrayItem };
              acc.push(newItem);
            });
          } else {
            // No array, just keep the current value
            acc.push(item);
          }
          return acc;
        }, []);
        result.push(...exploded);
      });
    } else {
      // Explode based on the provided attribute
      data.forEach((item) => {
        const value = item[attribute];
        if (Array.isArray(value)) {
          // For each element in the array, create a new object
          value.forEach((arrayItem) => {
            const newItem = { ...item, [attribute]: arrayItem };
            result.push(newItem);
          });
        } else {
          // If the attribute is not an array, just keep the original object
          result.push(item);
        }
      });
    }

    return result;
  }

  const dataOut = node.tableOut({ name: "dataOut", label: "Exploded Data" });

  node.onRender = () => {
    const sourceType = sourceTypeIn.value;
    const attribute = attrIn.value;

    let dataToExplode;

    // Determine the source data based on sourceType
    switch (sourceType) {
      case "input":
        // Use the input port value as the source data
        dataToExplode = dataIn.value;
        break;

      case "attribute":
        // Use the specified attribute of the input data
        if (dataIn.value && Array.isArray(dataIn.value)) {
          dataToExplode = dataIn.value.flatMap((row) => row[attribute]);
        }
        break;

      case "string":
        // Use the provided source string as the data
        const sourceString = sourceIn.value;
        try {
          dataToExplode = [JSON.parse(sourceString)]; // Parse string as JSON
        } catch (e) {
          // If parsing fails, log an error and return
          dataOut.set([]);
          return;
        }
        break;

      default:
        node.error("Invalid source type.");
        dataOut.set([]);
        return;
    }

    // Ensure valid data to explode
    if (!dataToExplode || !Array.isArray(dataToExplode)) {
      dataOut.set([]); // Set an empty array if no valid data
      return;
    }

    // Call explodeData to explode the determined source data
    const explodedData = explodeData(dataToExplode, attribute);

    // Output the exploded data
    dataOut.set(explodedData);
  };
}
