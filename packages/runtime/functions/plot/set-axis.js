/**
 * Define the general properties of the plot.
 *
 * @category Plot
 */

import { emptyPlot, validateVegaSpec, parse2vegaRef, setAxis } from "project:Utilities";

export default function (node) {
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });

  node.pushSection({ name: "General" });
  const scaleIn = node.stringIn({
    name: "scale",
    label: "Scale",
    value: "xScale",
    choices: ["xScale", "yScale", "<custom>"],
  });
  const customScaleIn = node.stringIn({ name: "customScale", label: "Custom scale" });
  const titleIn = node.stringIn({ name: "title", label: "Title" });
  const orientIn = node.stringIn({
    name: "orient",
    label: "Orientation",
    value: "bottom",
    choices: ["left", "right", "top", "bottom"],
  });
  node.popSection();

  node.pushSection({ name: "Layout", collapsed: true });
  const offsetIn = node.numberIn({ name: "offset", label: "Offset", value: 5 });
  const positionIn = node.numberIn({ name: "position", label: "Axis position" });
  const minExtentIn = node.numberIn({ name: "minExtent", label: "Min extent" });
  const maxExtentIn = node.numberIn({ name: "maxExtent", label: "Max extent" });
  const zindexIn = node.numberIn({ name: "zindex", label: "Z index", value: 0 });
  node.popSection();

  node.pushSection({ name: "Title", collapsed: true });
  const titlePaddingIn = node.numberIn({ name: "titlePadding", label: "Title padding" });
  node.popSection();

  node.pushSection({ name: "Labels", collapsed: true });
  const labelsIn = node.booleanIn({ name: "labels", label: "Show labels", value: true });
  const valuesIn = node.stringIn({ name: "labelValues", label: "Label values" });
  const labelAngleIn = node.numberIn({ name: "labelAngle", label: "Label angle" });
  const labelAlignIn = node.stringIn({
    name: "labelAlign",
    label: "Label alignment",
    value: "center",
    choices: ["left", "center", "right"],
  });
  const labelPaddingIn = node.numberIn({ name: "labelPadding", label: "Label padding", value: 2 });
  const labelBaselineIn = node.stringIn({
    name: "labelBaseline",
    label: "Label baseline",
    value: "middle",
    choices: ["alphabetic", "top", "middle", "bottom", "line-top", "line-bottom"],
  });
  const formatIn = node.stringIn({ name: "format", label: "Format label" });
  node.popSection();

  node.pushSection({ name: "Ticks", collapsed: true });
  const ticksIn = node.booleanIn({ name: "ticks", label: "Show ticks", value: true });
  const tickSizeIn = node.numberIn({ name: "tickSize", label: "Tick size" });
  const tickCountIn = node.numberIn({ name: "tickCount", label: "Tick count" });
  const tickOffsetIn = node.numberIn({ name: "tickOffset", label: "Tick offset" });
  const tickMinStepIn = node.numberIn({ name: "tickMinStep", label: "Tick minimum step" });
  node.popSection();

  node.pushSection({ name: "Baseline", collapsed: true });
  const domainIn = node.booleanIn({ name: "domain", label: "Show baseline", value: true });
  const domainColorIn = node.stringIn({ name: "domainColor", label: "Baseline color" });
  const domainOpacityIn = node.numberIn({ name: "domainOpacity", label: "Baseline opacity" });
  const domainWidthIn = node.numberIn({ name: "domainWidth", label: "Baseline width" });
  const domainCapIn = node.stringIn({
    name: "domainCap",
    label: "Baseline cap",
    value: "butt",
    choices: ["butt", "round", "square"],
  });
  const domainDashIn = node.stringIn({ name: "domainDash", label: "Baseline dash" });
  const domainDashOffsetIn = node.numberIn({ name: "domainDashOffset", label: "Baseline dash offset" });
  node.popSection();

  node.pushSection({ name: "Grid", collapsed: true });
  const gridIn = node.booleanIn({ name: "grid", label: "Show grid", value: false });
  const gridColorIn = node.stringIn({ name: "gridColor", label: "Grid color" });
  const gridOpacityIn = node.numberIn({ name: "gridOpacity", label: "Grid opacity" });
  const gridWidthIn = node.numberIn({ name: "gridWidth", label: "Grid width" });
  const gridCapIn = node.stringIn({
    name: "gridCap",
    label: "Grid cap",
    value: "butt",
    choices: ["butt", "round", "square"],
  });
  const gridDashIn = node.stringIn({ name: "gridDash", label: "Grid dash" });
  const gridDashOffsetIn = node.numberIn({ name: "gridDashOffset", label: "Grid dash offset" });
  const gridScaleIn = node.stringIn({ name: "gridScale", label: "Grid scale" });
  node.popSection();

  node.pushSection({ name: "Other", collapsed: true });
  const removeIn = node.booleanIn({ name: "remove", label: "Remove axis", value: false });
  const encodeIn = node.stringIn({ name: "encode", label: "Encode", widget: "TEXT" });
  node.popSection();

  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot spec out" });

  node.onRender = () => {
    let specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : emptyPlot);
    validateVegaSpec(specOut);
    setAxis({
      spec: specOut,
      scale: scaleIn.value === "<custom>" ? customScaleIn.value : scaleIn.value,
      orient: orientIn.value,
      title: titleIn.value,
      offset: offsetIn.value,
      position: positionIn.value,
      titlePadding: titlePaddingIn.value,
      maxExtent: maxExtentIn.value,
      minExtent: minExtentIn.value,
      labels: labelsIn.value,
      labelValues: parse2vegaRef(specOut, undefined, undefined, valuesIn),
      labelAngle: labelAngleIn.value,
      labelAlign: labelAlignIn.value,
      labelPadding: labelPaddingIn.value,
      labelBaseline: labelBaselineIn.value,
      ticks: ticksIn.value,
      tickSize: tickSizeIn.value,
      tickCount: tickCountIn.value,
      tickOffset: tickOffsetIn.value,
      tickMinStep: tickMinStepIn.value,
      domain: domainIn.value,
      domainCap: domainCapIn.value,
      domainColor: domainColorIn.value,
      domainDash: domainDashIn.value ? parse2vegaRef(specOut, undefined, undefined, domainDashIn) : undefined,
      domainDashOffset: domainDashOffsetIn.value,
      domainOpacity: domainOpacityIn.value,
      domainWidth: domainWidthIn.value,
      grid: gridIn.value,
      gridCap: gridCapIn.value,
      gridColor: gridColorIn.value,
      gridDash: gridDashIn.value ? parse2vegaRef(specOut, undefined, undefined, gridDashIn) : undefined,
      gridDashOffset: gridDashOffsetIn.value,
      gridOpacity: gridOpacityIn.value,
      gridScale: gridScaleIn.value,
      gridWidth: gridWidthIn.value,
      zindex: zindexIn.value,
      format: formatIn.value,
      encode: encodeIn.value ? encodeIn.value : undefined,
      remove: removeIn.value,
    });

    plotSpecOut.set(specOut);
  };
}
