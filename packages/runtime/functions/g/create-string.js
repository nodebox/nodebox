/**
 * Create a string value.
 *
 * @category String
 */

export default function (node) {
  const stringIn = node.stringIn({ name: "stringIn", label: "string" });
  const tableOut = node.tableOut({ name: "stringOut", label: "string" });

  node.onRender = () => {
    tableOut.set([{ value: stringIn.value }]);
  };
}
