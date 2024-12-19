/**
 * Groups the rows of a table based on a specified attribute.
 *
 * This node takes in a table and an attribute, then groups the rows of the table
 * based on the values of the specified attribute. The output is an array of groups,
 * where each group is an object containing the attribute value and the rows that
 * belong to that group.
 *
 * @category Data Manipulation
 */

import { group } from "https://esm.sh/d3-array@3.2.4";

export default function (node) {
  const tableIn = node.tableIn({ name: "table" });
  const attributeIn = node.stringIn({ name: "attribute", value: "category" });
  const groupsOut = node.tableOut({ name: "groups" });

  node.onRender = () => {
    // Get the input table and attribute
    const table = tableIn.value;
    const attribute = attributeIn.value;

    // Check if the table and attribute are valid
    if (!table || !attribute) {
      groupsOut.set([]);
      return;
    }

    // Grouping logic using d3-array's group function
    const groupedMap = group(table, (d) => d[attribute]);

    // Convert the Map to an array of group objects
    const groupedData = Array.from(groupedMap, ([key, value]) => ({
      [attribute]: key,
      rows: value,
    }));

    groupsOut.set(groupedData);
  };
}
