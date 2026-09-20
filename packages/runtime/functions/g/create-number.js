/**
 * Create a number value.
 *
 * @category Number
 */

export default function (node) {
  const numberIn = node.numberIn({ name: "numberIn", label: "number" });
  const tableOut = node.tableOut({ name: "numberOut", label: "number" });

  node.onRender = () => {
    tableOut.set([{ value: numberIn.value }]);
  };
}
