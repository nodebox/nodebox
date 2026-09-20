/**
 * Pivot data to reshape it into a wider or longer format.
 *
 * This node helps to structure data for analysis or visualization.
 * - Longer: Converts columns into rows, making the data longer.
 * This is useful when you have multiple columns representing the same variable (e.g., time points)
 * and want them in a single column.
 * - Wider: Converts rows into columns, making the data wider.
 * This is useful when you want to spread a key-value pair into separate columns for comparison.
 *
 * @category Data
 */

export default function (node) {
  const dataIn = node.tableIn({ name: "dataIn", label: "Data" });

  // General section for direction choice
  node.pushSection({ name: "General" });
  const dirIn = node.stringIn({
    name: "direction",
    label: "Direction",
    value: "Longer",
    choices: ["Longer", "Wider"],
  });
  node.popSection();

  // Longer section for pivoting columns into rows
  node.pushSection({ name: "Longer", collapsed: true });
  const pivotAttrsIn = node.stringIn({ name: "pivotAttrs", label: "Pivot attributes" });
  const excludeAttrsIn = node.stringIn({ name: "excludeAttrs", label: "Exclude attribute(s)" });
  const namesToIn = node.stringIn({ name: "namesTo", label: "Names to" });
  const valuesToIn = node.stringIn({ name: "valuesTo", label: "Values to" });
  node.popSection();

  // Wider section for pivoting rows into columns
  node.pushSection({ name: "Wider", collapsed: true });
  const idAttrsIn = node.stringIn({ name: "idAttrs", label: "Identifier attribute(s)" });
  const namesFromIn = node.stringIn({ name: "namesFrom", label: "Names from" });
  const valuesFromIn = node.stringIn({ name: "valuesFrom", label: "Values from" });
  node.popSection();

  const dataOut = node.tableOut({ name: "dataOut", label: "Data" });

  // Helper function to handle string or array inputs
  function parseAttrs(attrInput) {
    if (!attrInput) return [];
    if (Array.isArray(attrInput)) {
      return attrInput;
    }
    return attrInput.split(",").map((attr) => attr.trim());
  }

  // Pivot longer function - Converts columns to rows
  function pivotLonger({ data, pivotAttrs, excludeAttrs, namesTo, valuesTo }) {
    if (pivotAttrs == "") pivotAttrs = undefined;
    if (excludeAttrs == "") excludeAttrs = undefined;

    pivotAttrs = parseAttrs(pivotAttrs); // Parse string or array input
    excludeAttrs = parseAttrs(excludeAttrs); // Parse string or array input
    pivotAttrs =
      pivotAttrs.length > 0
        ? pivotAttrs
        : excludeAttrs.length > 0
          ? Object.keys(data[0]).filter((d) => !excludeAttrs.includes(d))
          : undefined;
    if (pivotAttrs && excludeAttrs) pivotAttrs = pivotAttrs.filter((d) => !excludeAttrs.includes(d));

    let newData = [];
    data.forEach((row) => {
      pivotAttrs.forEach((attr) => {
        let newRow = { ...row };
        newRow[namesTo] = attr;
        newRow[valuesTo] = row[attr];
        pivotAttrs.forEach((attrToRemove) => delete newRow[attrToRemove]);
        newData.push(newRow);
      });
    });
    return newData;
  }

  // Pivot wider function - Converts rows to columns
  function pivotWider({ data, idAttrs, namesFrom, valuesFrom }) {
    // If idAttrs is undefined, automatically use all attributes except namesFrom and valuesFrom
    if (!idAttrs) {
      idAttrs = Object.keys(data[0]).filter((attr) => attr !== namesFrom && attr !== valuesFrom);
    } else {
      idAttrs = parseAttrs(idAttrs); // Parse string or array input
    }

    let widerData = {};
    data.forEach((row) => {
      let key = JSON.stringify(idAttrs.map((attr) => row[attr]));
      if (!widerData[key]) {
        widerData[key] = idAttrs.reduce((obj, attr) => {
          obj[attr] = row[attr];
          return obj;
        }, {});
      }
      widerData[key][row[namesFrom]] = row[valuesFrom];
    });
    return Object.values(widerData);
  }

  // onRender function
  node.onRender = () => {
    let data = dataIn.value ? structuredClone(dataIn.value) : [];
    const direction = dirIn.value;
    let newData = [];

    if (direction === "Wider") {
      // Wider transformation
      newData = pivotWider({
        data: data,
        idAttrs: idAttrsIn.value,
        namesFrom: namesFromIn.value,
        valuesFrom: valuesFromIn.value,
      });
    } else if (direction === "Longer") {
      // Longer transformation
      newData = pivotLonger({
        data: data,
        pivotAttrs: pivotAttrsIn.value,
        excludeAttrs: excludeAttrsIn.value,
        namesTo: namesToIn.value,
        valuesTo: valuesToIn.value,
      });
    }

    dataOut.set(newData);
  };
}
