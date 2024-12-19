/**
 * Collapse unrelated fields into a new top-level property or group rows by a specified attribute.
 *
 * @category Data Manipulation
 */

export default function (node) {
  const dataIn = node.tableIn({ name: "dataIn", label: "Input Data" });

  // Collapse Settings
  node.pushSection({ name: "Collapse Options" });
  const collapseKeyIn = node.stringIn({
    name: "collapseKey",
    label: "Top-Level Property",
    value: "collapsed", // Default property name
  });
  const groupByIn = node.stringIn({
    name: "groupBy",
    label: "Group By",
  });
  node.popSection();

  const dataOut = node.tableOut({ name: "dataOut", label: "Output Data" });

  // Collapse Data Function
  function collapseData(data, topKey, groupBy) {
    // Group data by the specified attribute, or treat all as a single group
    const grouped = {};
    data.forEach((row) => {
      const groupKey = groupBy ? row[groupBy] || null : "all"; // Default group key to "all" if no groupBy is specified
      if (!grouped[groupKey]) grouped[groupKey] = [];
      grouped[groupKey].push(row);
    });

    // Format the grouped data
    return Object.entries(grouped).map(([groupKey, groupRows]) => ({
      group: groupKey === "all" ? null : groupKey,
      [topKey]: groupRows,
    }));
  }

  // Node Logic
  node.onRender = () => {
    const collapseKey = collapseKeyIn.value;
    const groupByAttr = groupByIn.value;
    const inputData = Array.isArray(dataIn.value) ? dataIn.value : [dataIn.value];

    if (!inputData || !Array.isArray(inputData)) {
      dataOut.set([]);
      return;
    }

    // Collapse data
    const outputData = collapseData(inputData, collapseKey, groupByAttr);

    // Output the processed data
    dataOut.set(outputData);
  };
}
