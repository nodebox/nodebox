/**
 * Set a theme to change the style of the plot.
 *
 * @category Plot
 */

import { emptyPlot, validateVegaSpec } from "project:Utilities";
import { theme } from "project:Themes";

export default function (node) {
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });
  const themeIn = node.stringIn({
    name: "theme",
    label: "Theme",
    value: "dark",
    choices: ["<default>", "ggplot2", "dark", "quartz", "vox", "fiveThirtyEight"],
  });
  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot spec out" });
  const shapeOut = node.shapeOut({ name: "plotShape", label: "Ploit shape" });

  node.onRender = () => {
    let specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : emptyPlot);
    validateVegaSpec(specOut);
    if (themeIn.value != "<default>") specOut.config = theme(themeIn.value);

    plotSpecOut.set(specOut);
  };
}
