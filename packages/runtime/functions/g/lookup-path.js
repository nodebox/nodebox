/**
 * Lookup a specific path in a dataset based on a key.
 *
 * This node takes a table and a key (in dot-notation)
 * and outputs the value at that key. The key can be used to navigate deeper
 * into nested objects.
 *
 * @category Data Manipulation
 */

export default function (node) {
  const tableIn = node.tableIn({ name: "table" });
  const keyIn = node.stringIn({ name: "key", value: "features" });
  const tableOut = node.tableOut({ name: "out" });

  node.onRender = () => {
    const data = tableIn.value;
    const key = keyIn.value;

    if (typeof data !== "object" || data === null) {
      console.error("Invalid data input. Expected an object.");
      tableOut.set([]);
      return;
    }

    const keys = key.split(".");
    let result = data;

    try {
      for (const k of keys) {
        if (result && k in result) {
          result = result[k];
        } else {
          throw new Error(`Key not found: ${k}`);
        }
      }
      tableOut.set(result);
    } catch (error) {
      console.error(error.message);
      tableOut.set(null);
    }
  };
}
