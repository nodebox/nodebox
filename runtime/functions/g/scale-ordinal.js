/**
 * Maps the values of a column using an ordinal (categorical) scale to color.
 *
 * This node scales the values of a column using an ordinal (categorical) scale. The input column must
 * contain categorical values. The output column will contain the scaled values. The scaling is done using
 * an ordinal scale, which maps each unique input value to a unique output value. The output values are
 * evenly spaced along the output range.
 *
 * @category Scales
 */

import { scaleOrdinal } from "https://esm.sh/d3-scale@4.0.2";
import * as schemes from "https://esm.sh/d3-scale-chromatic@3.1.0";

const COLOR_SCHEMES = {
  Category10: schemes.schemeCategory10,
  Accent: schemes.schemeAccent,
  Dark2: schemes.schemeDark2,
  Observable10: schemes.schemeObservable10,
  Paired: schemes.schemePaired,
  Pastel1: schemes.schemePastel1,
  Pastel2: schemes.schemePastel2,
  Set1: schemes.schemeSet1,
  Set2: schemes.schemeSet2,
  Set3: schemes.schemeSet3,
  Tableau10: schemes.schemeTableau10,
};

export default function (node) {
  const tableIn = node.tableIn({ name: "table" });
  const inAttributeIn = node.stringIn({ name: "inAttribute", value: "value" });
  const outAttributeIn = node.stringIn({ name: "outAttribute", value: "value" });
  const domainTypeIn = node.stringIn({ name: "domainType", value: "auto" });
  const domainIn = node.stringIn({ name: "domain", value: "" });
  const colorSchemeIn = node.stringIn({ name: "colorScheme", value: "Category10" });
  const tableOut = node.tableOut({ name: "out" });

  node.onRender = () => {
    // Find the table and column
    const table = tableIn.value;
    if (!table) {
      tableOut.set([]);
      return;
    }
    const attr = inAttributeIn.value;
    // Check that at least some rows have the given attribute
    const hasAttribute = table.some((row) => row[attr] !== undefined);
    if (!hasAttribute) {
      tableOut.set(table);
      return;
    }
    const range = COLOR_SCHEMES[colorSchemeIn.value];

    // Scale the input values
    const inValues = table.map((row) => row[attr]);
    const domain = domainTypeIn.value === "auto" ? Array.from(new Set(inValues)) : domainIn.value.split(",");
    const scale = scaleOrdinal(domain, range).unknown("#f0f");
    const newTable = table.map((row) => ({ ...row, [outAttributeIn.value]: scale(row[attr]) }));
    tableOut.set(newTable);
  };
}
