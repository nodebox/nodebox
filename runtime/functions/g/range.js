/**
 * Create a range of numbers from a start value to an end value with a specified step size.
 *
 * The output is a table with a single column containing the range of numbers. The name of the column
 * is "value" by default, but you can change it to something else.
 * @category Generators
 */

export default function (node) {
  const startIn = node.numberIn({ name: "start", value: 0 });
  const endIn = node.numberIn({ name: "end", value: 10 });
  const stepIn = node.numberIn({ name: "step", value: 1 });
  const attributeIn = node.stringIn({ name: "attribute", value: "value" });
  const tableOut = node.tableOut({ name: "out" });

  node.onRender = () => {
    const [start, end, step] = [startIn.value, endIn.value, stepIn.value];
    let numItems = 0;
    if (step !== 0) {
      numItems = Math.floor((end - start) / step) + 1;
      numItems = Math.max(numItems, 0);
    }
    const table = [];
    for (let i = 0; i < numItems; i++) {
      table.push({ [attributeIn.value]: start + i * step });
    }
    tableOut.set(table);
  };
}
