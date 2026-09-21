/**
 * Unflatten hierarchical data into nested objects based on a key separator.
 *
 * @category Data Manipulation
 */

export default function (node) {
  const dataIn = node.tableIn({ name: "dataIn", label: "Input Data" });

  // Unflatten Settings
  node.pushSection({ name: "Unflatten Options" });
  const sepIn = node.stringIn({
    name: "sep",
    label: "Key separator",
    value: ".", // Default separator
  });
  const levelsIn = node.numberIn({
    name: "levels",
    label: "Unflatten Levels",
    value: 1, // Default unflatten 1 level
  });
  node.popSection();

  const dataOut = node.tableOut({ name: "dataOut", label: "Output Data" });

  // Unflatten Function
  function unflattenData(flattened, sep = ".", levels = 1) {
    const nested = {};

    Object.keys(flattened).forEach((key) => {
      const keys = key.split(sep);
      if (keys.length <= levels) {
        let current = nested;

        keys.forEach((k, index) => {
          if (index === keys.length - 1) {
            current[k] = flattened[key];
          } else {
            current[k] = current[k] || {};
            current = current[k];
          }
        });
      } else {
        // If levels exceeded, keep it flat
        nested[key] = flattened[key];
      }
    });

    return nested;
  }

  // Node Logic
  node.onRender = () => {
    const separator = sepIn.value;
    const levels = levelsIn.value;
    const inputData = Array.isArray(dataIn.value) ? dataIn.value : [dataIn.value];

    if (!inputData || !Array.isArray(inputData)) {
      dataOut.set([]);
      return;
    }

    const outputData = inputData.map((record) => unflattenData(record, separator, levels));
    dataOut.set(outputData);
  };
}
