/**
 * Set the title of the Vega plot specification.
 *
 * @category Plot
 */

import { emptyPlot, validateVegaSpec } from "project:Utilities";

export default function (node) {
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });

  // Title parameters
  const titleTextIn = node.stringIn({ name: "titleText", label: "Title", value: "Chart Title" });
  const subtitleIn = node.stringIn({ name: "subtitle", label: "Subtitle", value: "" });
  const subtitlePaddingIn = node.numberIn({ name: "subtitlePadding", label: "Subtitle padding", value: 10 });
  const anchorIn = node.stringIn({
    name: "anchor",
    label: "Anchor",
    value: "start",
    choices: ["start", "middle", "end"],
  });
  const alignIn = node.stringIn({ name: "align", label: "Align", value: "left", choices: ["left", "center", "right"] });
  const baselineIn = node.stringIn({
    name: "baseline",
    label: "Baseline",
    value: "top",
    choices: ["top", "middle", "bottom"],
  });
  const dxIn = node.numberIn({ name: "dx", label: "Horizontal offset", value: 0 });
  const dyIn = node.numberIn({ name: "dy", label: "Vertical offset", value: 0 });
  const frameIn = node.stringIn({ name: "frame", label: "Frame", value: "group", choices: ["group", "bounds"] });
  const limitIn = node.numberIn({ name: "limit", label: "Limit", value: 0 });
  const lineHeightIn = node.numberIn({ name: "lineHeight", label: "Line height", value: 0 });
  const offsetIn = node.numberIn({ name: "offset", label: "Orthogonal offset", value: 10 });
  const orientIn = node.stringIn({
    name: "orient",
    label: "Orientation",
    value: "top",
    choices: ["top", "bottom", "left", "right"],
  });

  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot spec out" });

  node.onRender = () => {
    let specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : emptyPlot);
    validateVegaSpec(specOut);

    let title = {
      text: titleTextIn.value,
      subtitle: subtitleIn.value,
      subtitlePadding: subtitlePaddingIn.value,
      anchor: anchorIn.value,
      align: alignIn.value,
      baseline: baselineIn.value,
      dx: dxIn.value,
      dy: dyIn.value,
      frame: frameIn.value,
      limit: limitIn.value,
      lineHeight: lineHeightIn.value,
      offset: offsetIn.value,
      orient: orientIn.value,
    };

    specOut.title = title;

    plotSpecOut.set(specOut);
  };
}
