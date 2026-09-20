/**
 * Add a data label to plot.
 *
 * @category Plot
 */

import { emptyPlot, validateVegaSpec, parse2vegaRef, addParamAttr, setMark } from "project:Utilities";

export default function (node) {
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });

  node.pushSection({ name: "General" });
  const modeIn = node.stringIn({ name: "mode", label: "Mode", value: "replace", choices: ["replace", "append"] });
  const dataNameIn = node.stringIn({ name: "dataName", label: "Data name", value: "table" });
  const markNameIn = node.stringIn({ name: "markName", label: "Mark name", value: "label" });
  const labelTextIn = node.stringIn({ name: "labelText", label: "Label text" });
  const xIn = node.stringIn({ name: "X", label: "X position", value: '{"scale": "xScale","field": "__x"}' });
  const yIn = node.stringIn({ name: "Y", label: "Y position", value: '{"scale": "yScale","field": "__y"}' });
  const limitIn = node.numberIn({ name: "limit", label: "Limit", value: 0 });
  const ellipsisIn = node.stringIn({ name: "ellipsis", label: "Ellipsis", value: "..." });
  node.popSection();

  node.pushSection({ name: "Font", collapsed: true });
  const fontIn = node.stringIn({ name: "font", label: "Font" });
  const fontSizeIn = node.numberIn({ name: "fontSize", label: "Font size" });
  const fontWeightIn = node.stringIn({ name: "fontWeight", label: "Font weight" });
  const fontStyleIn = node.stringIn({ name: "fontStyle", label: "Font style" });
  const fontSizeStepIn = node.numberIn({ name: "fontSizeStep", label: "Font size step" });
  const lineHeightIn = node.numberIn({ name: "lineHeight", label: "Line height" });
  node.popSection();

  node.pushSection({ name: "Style", collapsed: true });
  const opacityIn = node.numberIn({ name: "opacity", label: "Opacity" });
  const fillIn = node.stringIn({ name: "fill", label: "Fill color" });
  const fillOpacityIn = node.numberIn({ name: "fillOpacity", label: "Fill opacity" });
  const strokeIn = node.stringIn({ name: "stroke", label: "Stroke color" });
  const strokeWidthIn = node.numberIn({ name: "strokeWidth", label: "Stroke width" });
  const strokeOpacityIn = node.numberIn({ name: "strokeOpacity", label: "Stroke opacity" });
  const strokeDashIn = node.stringIn({ name: "strokeDash", label: "Stroke dash" });
  const strokeDashOffsetIn = node.numberIn({ name: "strokeDashOffset", label: "Stroke dash offset" });
  node.popSection();

  node.pushSection({ name: "Layout", collapsed: true });
  const alignIn = node.stringIn({ name: "align", label: "Align", value: "left", choices: ["left", "center", "right"] });
  const angleIn = node.numberIn({ name: "angle", label: "Angle", value: 0 });
  const hOffsetIn = node.numberIn({ name: "hOffset", label: "Horizontal offset", value: 0 });
  const vOffsetIn = node.numberIn({ name: "vOffset", label: "Vertical offset", value: 0 });
  const baselineIn = node.stringIn({
    name: "baseline",
    label: "Baseline",
    value: "top",
    choices: ["top", "middle", "bottom"],
  });
  const thetaIn = node.numberIn({ name: "theta", label: "Theta" });
  const radiusIn = node.numberIn({ name: "radius", label: "Radius" });
  const zindexIn = node.numberIn({ name: "zindex", label: "Z-index" });
  node.popSection();

  node.pushSection({ name: "Other", collapsed: true });
  const filterIn = node.stringIn({ name: "filter", label: "Filter", widget: "TEXT", value: "true;" });
  const encodeIn = node.stringIn({ name: "encode", label: "Encode", widget: "TEXT" });
  node.popSection();

  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot spec out" });

  node.onRender = () => {
    let specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : emptyPlot);
    const dataName = dataNameIn.value || "table";
    addParamAttr(specOut, dataName, "__label", labelTextIn);

    setMark({
      mode: modeIn.value,
      spec: specOut,
      dataName: dataName,
      markName: markNameIn.value || "label",
      markType: "text",
      text: parse2vegaRef(specOut, dataName, "__label", labelTextIn),
      x: parse2vegaRef(specOut, dataName, "__x", xIn),
      y: parse2vegaRef(specOut, dataName, "__y", yIn),
      dx: parse2vegaRef(specOut, dataName, undefined, hOffsetIn),
      dy: parse2vegaRef(specOut, dataName, undefined, vOffsetIn),
      angle: parse2vegaRef(specOut, dataName, undefined, angleIn),
      align: parse2vegaRef(specOut, dataName, undefined, alignIn),
      baseline: parse2vegaRef(specOut, dataName, undefined, baselineIn),
      limit: parse2vegaRef(specOut, dataName, undefined, limitIn),
      ellipsis: parse2vegaRef(specOut, dataName, undefined, ellipsisIn),
      font: parse2vegaRef(specOut, dataName, undefined, fontIn),
      fontSize: parse2vegaRef(specOut, dataName, undefined, fontSizeIn),
      fontWeight: parse2vegaRef(specOut, dataName, undefined, fontWeightIn),
      fontStyle: parse2vegaRef(specOut, dataName, undefined, fontStyleIn),
      fontSizeStep: parse2vegaRef(specOut, dataName, undefined, fontSizeStepIn),
      lineHeight: parse2vegaRef(specOut, dataName, undefined, lineHeightIn),
      opacity: parse2vegaRef(specOut, dataName, undefined, opacityIn),
      fill: parse2vegaRef(specOut, dataName, undefined, fillIn),
      fillOpacity: parse2vegaRef(specOut, dataName, undefined, fillOpacityIn),
      stroke: parse2vegaRef(specOut, dataName, undefined, strokeIn),
      strokeWidth: parse2vegaRef(specOut, dataName, undefined, strokeWidthIn),
      strokeOpacity: parse2vegaRef(specOut, dataName, undefined, strokeOpacityIn),
      strokeDash: parse2vegaRef(specOut, dataName, undefined, strokeDashIn),
      strokeDashOffset: parse2vegaRef(specOut, dataName, undefined, strokeDashOffsetIn),
      theta: parse2vegaRef(specOut, dataName, undefined, thetaIn),
      radius: parse2vegaRef(specOut, dataName, undefined, radiusIn),
      zindex: parse2vegaRef(specOut, dataName, undefined, zindexIn),
      filter: filterIn.value,
      encode: encodeIn.value,
    });

    plotSpecOut.set(specOut);
  };
}
