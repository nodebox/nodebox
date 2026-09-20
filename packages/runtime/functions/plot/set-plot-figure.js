/**
 * Define the general properties of the plot.
 *
 * @category Plot
 */

import { emptyPlot, validateVegaSpec, setThemeProperties } from "project:Utilities";

export default function (node) {
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });
  const widthIn = node.numberIn({ name: "width", label: "Width", value: 400 });
  const heightIn = node.numberIn({ name: "height", label: "Height", value: 400 });
  const paddingIn = node.numberIn({ name: "padding", label: "Padding", value: 10 });
  const padTopIn = node.numberIn({ name: "padTop", label: "Top padding" });
  const padBottomIn = node.numberIn({ name: "padBottom", label: "Bottom padding" });
  const padLeftIn = node.numberIn({ name: "padLeft", label: "Left padding" });
  const padRightIn = node.numberIn({ name: "padRight", label: "Right padding" });
  const autosizeIn = node.stringIn({
    name: "autosize",
    label: "Autosize",
    value: "pad",
    choices: ["pad", "fit", "fit-x", "fit-y", "none"],
  });
  const axesIn = node.booleanIn({ name: "defaultAxes", label: "Default axes", value: true });
  const bgColorIn = node.colorIn({ name: "bgColor", label: "Background color", value: "#f2f2f2" });

  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot spec out" });
  const shapeOut = node.shapeOut({ name: "plotShape" });

  node.onRender = () => {
    if (!axesIn.value) {
      emptyPlot.axes = [];
      emptyPlot.scales = [];
      emptyPlot.marks = [];
    }
    let specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : emptyPlot);
    validateVegaSpec(specOut);
    specOut.width = widthIn.value;
    specOut.height = heightIn.value;
    specOut.autosize = autosizeIn.value;
    if (padTopIn.value || padBottomIn.value || padLeftIn.value || padRightIn.value) {
      specOut.padding = {
        top: padTopIn.value || paddingIn.value || null,
        bottom: padBottomIn.value || paddingIn.value || null,
        left: padLeftIn.value || paddingIn.value || null,
        right: padRightIn.value || paddingIn.value || null,
      };
    } else {
      specOut.padding = paddingIn.value;
    }

    // set background color
    if (bgColorIn.value) {
      setThemeProperties(specOut, "background", bgColorIn.value.toString());
    }

    //shapeOut.set(rect);
    plotSpecOut.set(specOut);
  };
}
