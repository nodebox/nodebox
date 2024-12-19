/**
 * Creates a sequence of linearly interpolated values between a minimum and maximum value.
 *
 * The output is a table with a single column containing these interpolated values.
 * If the 'amount' is 1, the output will only include the 'min' value.
 * The column name can be specified or defaults to "value".
 * If a table is given as input, the amount of rows in the table will be used as the 'amount'.
 * @category Generators
 */

export default function (node) {
  const tableIn = node.tableIn({ name: "table" });
  const minIn = node.numberIn({ name: "min", value: 0 });
  const maxIn = node.numberIn({ name: "max", value: 90 });
  const amountIn = node.numberIn({ name: "amount", value: 10 });
  const attributeIn = node.stringIn({ name: "attribute", value: "value" });
  const tableOut = node.tableOut({ name: "out" });

  node.onRender = () => {
    let [min, max, amount] = [minIn.value, maxIn.value, amountIn.value];
    if (tableIn.value && tableIn.value.length > 0) {
      amount = tableIn.value.length;
    }
    const table = [];

    if (amount === 1) {
      // If amount is 1, output only the 'min' value.
      table.push({ [attributeIn.value]: min });
    } else if (amount > 1) {
      // Compute the step size based on the number of intervals (amount - 1)
      const step = (max - min) / (amount - 1);
      for (let i = 0; i < amount; i++) {
        // Linear interpolation formula: min + step * i
        table.push({ [attributeIn.value]: min + i * step });
      }
    }
    tableOut.set(table);
  };
}
