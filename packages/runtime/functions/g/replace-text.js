/**
 * Transform attribute values using regular expression or text replacement.
 *
 * This node transforms the specified attribute (column) in the input table using a
 * regular expression or text-based replacement.
 *
 * @category Transformations
 */

export default function (node) {
  const tableIn = node.tableIn({ name: "table" });
  const attributeIn = node.stringIn({ name: "attribute", value: "value" });
  const expressionIn = node.stringIn({ name: "expression", value: "" });
  const replacementIn = node.stringIn({ name: "replacement", value: "" });
  const useRegexIn = node.booleanIn({ name: "useRegex", value: true });
  const caseSensitiveIn = node.booleanIn({ name: "caseSensitive", value: false });
  const tableOut = node.tableOut({ name: "out" });

  node.onRender = () => {
    const table = tableIn.value;
    if (!table || expressionIn.value === "") {
      tableOut.set(table || []);
      return;
    }

    const attr = attributeIn.value;
    const expr = expressionIn.value;
    const replacement = replacementIn.value;
    const useRegex = useRegexIn.value;
    const caseSensitive = caseSensitiveIn.value;
    const flags = caseSensitive ? "g" : "gi";

    const hasAttribute = table.some((row) => row[attr] !== undefined);
    if (!hasAttribute) {
      tableOut.set(table);
      return;
    }

    const transformedTable = table.map((row) => {
      if (row[attr] !== undefined) {
        let newValue;
        if (useRegex) {
          const regex = new RegExp(expr, flags);
          newValue = row[attr].toString().replace(regex, replacement);
        } else {
          const searchExpr = caseSensitive ? expr : expr.toLowerCase();
          const rowValue = caseSensitive ? row[attr].toString() : row[attr].toString().toLowerCase();
          newValue = rowValue.split(searchExpr).join(replacement);
        }
        return { ...row, [attr]: newValue };
      }
      return row;
    });

    tableOut.set(transformedTable);
  };
}
