/**
 * Add data to be visualized in the plot.
 *
 * @category Plot
 */

import { emptyPlot, validateVegaSpec, addPlotData } from "project:Utilities";
import { parse as parseVega } from "https://esm.sh/vega@5";

export default function (node) {
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });
  const dataIn = node.tableIn({ name: "data", label: "Data" });
  const nameIn = node.stringIn({ name: "name", label: "Data name", value: "table" });
  const formatIn = node.stringIn({ name: "format", label: "Data format" });
  const featureIn = node.stringIn({ name: "feature", label: "Feature" });
  const indexIn = node.numberIn({ name: "index", label: "Insert position", value: -1 });
  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot spec out" });

  node.onRender = () => {
    let specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : emptyPlot);
    validateVegaSpec(specOut);
    const dataName = nameIn.value;
    const format = formatIn.value;
    const feature = featureIn.value;
    const index = indexIn.value;

    if (dataIn.value) {
      addPlotData(specOut, structuredClone(dataIn.value), dataName, format, feature, index);
    }

    plotSpecOut.set(specOut);
  };
}
