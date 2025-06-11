/**
 * Run an expression on each item in an array and return the results.
 */

export default function (node) {
  const tableIn = node.tableIn({ name: "table" });
  const sourceIn = node.stringIn({ name: "source", widget: "TEXT", value: "return { ...d };" });
  const tableOut = node.tableOut({ name: "out" });

  node.onRender = async () => {
    const data = tableIn.value;
    const fn = new Function("d", sourceIn.value);

    if (Array.isArray(data)) {
      const rows = data.map(fn);
      tableOut.set(rows);
    } else {
      const result = fn(data);
      tableOut.set(result);
      return;
    }
  };
}
