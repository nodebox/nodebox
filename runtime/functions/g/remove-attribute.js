/**
 * Delete one or more attributes from the data.
 *
 * @category Data
 */

export default function (node) {
  const dataIn = node.tableIn({ name: "data", label: "Data" });
  const attrIn = node.stringIn({ name: "attributeName", label: "Attribute name(s) to delete" });
  const dataOut = node.tableOut({ name: "dataOut", label: "Data" });

  node.onRender = () => {
    // Clone the input data
    let data = structuredClone(dataIn.value ? dataIn.value : []);

    // Parse the attribute name(s) input
    const attributeInput = attrIn.value;
    if (!attributeInput) {
      throw new Error("No attribute names specified for deletion.");
    }

    // Convert input to an array (supports comma-separated strings or arrays)
    const attributesToDelete = Array.isArray(attributeInput)
      ? attributeInput
      : attributeInput.split(",").map((attr) => attr.trim());

    if (attributesToDelete.length === 0) {
      throw new Error("No valid attributes specified to delete.");
    }

    // Function to delete the attributes from each object in the data
    let newData = data.map((item) => {
      let updatedItem = structuredClone(item);
      attributesToDelete.forEach((attr) => {
        delete updatedItem[attr];
      });
      return updatedItem;
    });

    // Set the updated data to the output
    dataOut.set(newData);
  };
}
