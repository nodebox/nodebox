/**
 * Define the legend of the plot.
 *
 * @category Plot
 */

import { emptyPlot, validateVegaSpec, parse2vegaRef, nbColorToCSS, addLegend } from "project:Utilities";

export default function (node) {
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });

  node.pushSection({ name: "General" });
  const scaleIn = node.stringIn({
    name: "scale",
    label: "Scale",
    value: "all",
    choices: [
      "all",
      "fillColorScale",
      "strokeColorScale",
      "sizeScale",
      "strokeWidthScale",
      "shapeScale",
      "opacityScale",
      "fillOpacityScale",
      "strokeOpacityScale",
      "strokeDashScale",
      "<custom>",
    ],
  });
  const markNameIn = node.stringIn({ name: "markName", label: "Mark name" });
  const titleIn = node.stringIn({ name: "title", label: "Title", value: "legend" });
  const typeIn = node.stringIn({ name: "type", label: "Type", value: "symbol", choices: ["symbol", "gradient"] });
  const shapeIn = node.stringIn({
    name: "shape",
    label: "Symbol shape",
    value: "<auto>",
    choices: [
      "<auto>",
      "circle",
      "square",
      "cross",
      "diamond",
      "triangle-up",
      "triangle-down",
      "triangle-right",
      "triangle-left",
      "stroke",
      "arrow",
      "wedge",
      "triangle",
    ],
  });
  const valuesIn = node.stringIn({ name: "values", label: "Visible values" });
  node.popSection();
  node.pushSection({ name: "Custom scales", collapsed: true });
  const fillScaleIn = node.stringIn({ name: "fillColorScale", label: "Fill color scale" });
  const opacityScaleIn = node.stringIn({ name: "opacityScale", label: "Opacity scale" });
  const shapeScaleIn = node.stringIn({ name: "shapeScale", label: "Shape scale" });
  const sizeScaleIn = node.stringIn({ name: "sizeScale", label: "Size scale" });
  const strokeScaleIn = node.stringIn({ name: "strokeColorScale", label: "Stroke color scale" });
  const strokeDashScaleIn = node.stringIn({ name: "strokeDashScale", label: "Stroke dash scale" });
  const strokeWidthScaleIn = node.stringIn({ name: "strokeWidthScale", label: "Stroke width scale" });
  node.popSection();
  node.pushSection({ name: "Layout", collapsed: true });
  const orientIn = node.stringIn({
    name: "orient",
    label: "Orientation",
    value: "top-right",
    choices: [
      ["left", "Left"],
      ["right", "Right"],
      ["top", "Top"],
      ["bottom", "Bottom"],
      ["top-left", "Top left"],
      ["top-right", "Top right"],
      ["bottom-left", "Bottom left"],
      ["bottom-right", "Bottom right"],
      ["none", "XY position"],
    ],
  });
  const directionIn = node.stringIn({
    name: "direction",
    label: "Direction",
    value: "vertical",
    choices: ["vertical", "horizontal"],
  });
  const columnsIn = node.numberIn({ name: "columns", label: "Columns", value: 1 });
  const offsetIn = node.numberIn({ name: "offset", label: "Offset", value: 10 });
  const paddingIn = node.numberIn({ name: "padding", label: "Padding", value: 10 });
  const rowPaddingIn = node.numberIn({ name: "rowPadding", label: "Row padding", value: 2 });
  const columnPaddingIn = node.numberIn({ name: "columnPadding", label: "Column padding", value: 2 });
  const cornerRadiusIn = node.numberIn({ name: "cornerRadius", label: "Corner radius", value: 3 });
  const legendXIn = node.numberIn({ name: "legendX", label: "X position", value: 200 });
  const legendYIn = node.numberIn({ name: "legendY", label: "Y position", value: 16 });
  const gridAlignIn = node.stringIn({
    name: "gridAlign",
    label: "Grid alignment",
    value: "each",
    choices: ["all", "each", "none"],
  });
  const fillColorIn = node.colorIn({ name: "fillColor", label: "Background color", value: { r: 1, g: 1, b: 1, a: 0 } });
  const strokeColorIn = node.colorIn({ name: "strokeColor", label: "Stroke color", value: { r: 1, g: 1, b: 1, a: 0 } });
  node.popSection();

  node.pushSection({ name: "Title", collapsed: true });
  const titleFontIn = node.stringIn({ name: "titleFont", label: "Title font" });
  const titleFontSizeIn = node.numberIn({ name: "titleFontSize", label: "Title font size", value: 12 });
  const titleFontWeightIn = node.stringIn({
    name: "titleFontWeight",
    label: "Title font weight",
    value: "bold",
    choices: ["normal", "bold", "lighter", "bolder"],
  });
  const titleFontStyleIn = node.stringIn({
    name: "titleFontStyle",
    label: "Title font style",
    value: "normal",
    choices: ["normal", "italic"],
  });
  const titleColorIn = node.colorIn({ name: "titleColor", label: "Title color" });
  const titleOpacityIn = node.numberIn({ name: "titleOpacity", label: "Title opacity", value: 1.0 });
  const titlePaddingIn = node.numberIn({ name: "titlePadding", label: "Title padding", value: 10 });
  const titleLineHeightIn = node.numberIn({ name: "titleLineHeight", label: "Title line height", value: 16 });
  const titleAlignIn = node.stringIn({
    name: "titleAlign",
    label: "Title alignment",
    value: "left",
    choices: ["left", "center", "right"],
  });
  const titleOrientIn = node.stringIn({
    name: "titleOrient",
    label: "Title orientation",
    value: "top",
    choices: ["top", "left", "right", "bottom"],
  });
  const titleAnchorIn = node.stringIn({
    name: "titleAnchor",
    label: "Title anchor",
    value: "start",
    choices: ["start", "middle", "end"],
  });
  const titleBaselineIn = node.stringIn({
    name: "titleBaseline",
    label: "Title baseline",
    value: "middle",
    choices: ["top", "middle", "bottom"],
  });
  const titleLimitIn = node.numberIn({ name: "titleLimit", label: "Title limit", value: 100 });
  node.popSection();
  node.pushSection({ name: "Labels", collapsed: true });
  const labelFontIn = node.stringIn({ name: "labelFont", label: "Label font" });
  const labelFontSizeIn = node.numberIn({ name: "labelFontSize", label: "Label font size", value: 12 });
  const labelFontWeightIn = node.stringIn({
    name: "labelFontWeight",
    label: "Label font weight",
    value: "normal",
    choices: ["normal", "bold", "lighter", "bolder"],
  });
  const labelFontStyleIn = node.stringIn({
    name: "labelFontStyle",
    label: "Label font style",
    value: "normal",
    choices: ["normal", "italic"],
  });
  const labelColorIn = node.colorIn({ name: "labelColor", label: "Label color" });
  const labelOpacityIn = node.numberIn({ name: "labelOpacity", label: "Label opacity", value: 1.0 });
  const labelAlignIn = node.stringIn({
    name: "labelAlign",
    label: "Label alignment",
    value: "left",
    choices: ["left", "center", "right"],
  });
  const labelBaselineIn = node.stringIn({
    name: "labelBaseline",
    label: "Label baseline",
    value: "middle",
    choices: ["top", "middle", "bottom"],
  });
  const labelLimitIn = node.numberIn({ name: "labelLimit", label: "Label limit", value: 100 });
  const labelOverlapIn = node.stringIn({
    name: "labelOverlap",
    label: "Label overlap",
    value: "false",
    choices: ["false", "parity", "greedy"],
  });
  const labelSeparationIn = node.numberIn({ name: "labelSeparation", label: "Label separation" });
  const labelFormatIn = node.stringIn({ name: "labelFormat", label: "Label format" });

  node.popSection();
  node.pushSection({ name: "Symbols", collapsed: true });
  const symbolSizeIn = node.numberIn({ name: "symbolSize", label: "Symbol size", value: 100 });
  const symbolFillColorIn = node.colorIn({
    name: "symbolFillColor",
    label: "Symbol fill color",
    value: { r: 0.2980392156862745, g: 0.47058823529411764, b: 0.6588235294117647, a: 1 },
  });
  const symbolStrokeColorIn = node.colorIn({
    name: "symbolStrokeColor",
    label: "Symbol stroke color",
    value: { r: 1, g: 1, b: 1, a: 0 },
  });
  const symbolOpacityIn = node.numberIn({ name: "symbolOpacity", label: "Symbol opacity", value: 1.0 });
  const symbolTypeIn = node.stringIn({
    name: "symbolType",
    label: "Symbol type",
    value: "filled",
    choices: ["filled", "stroked"],
  });
  const symbolDashIn = node.stringIn({ name: "symbolDash", label: "Symbol dash" });
  const symbolDashOffsetIn = node.numberIn({ name: "symbolDashOffset", label: "Symbol dash offset" });
  const symbolLimitIn = node.numberIn({ name: "symbolLimit", label: "Symbol limit", value: 10 });
  const symbolOffsetIn = node.numberIn({ name: "symbolOffset", label: "Symbol offset", value: 5 });
  node.popSection();
  node.pushSection({ name: "Gradient", collapsed: true });
  const gradientLengthIn = node.stringIn({ name: "gradientLength", label: "Gradient length", value: 200 });
  const gradientThicknessIn = node.stringIn({ name: "gradientThickness", label: "Gradient thickness", value: 16 });
  const gradientLabelOffsetIn = node.numberIn({ name: "gradientLabelOffset", label: "Label offset", value: 2 });
  const gradientOpacityIn = node.numberIn({ name: "gradientOpacity", label: "Gradient opacity", value: 1.0 });
  const gradientStrokeColorIn = node.colorIn({
    name: "gradientStrokeColor",
    label: "Gradient stroke color",
    value: { r: 1, g: 1, b: 1, a: 0 },
  });
  const gradientStrokeWidthIn = node.stringIn({ name: "gradientStrokeWidth", label: "Gradient stroke width" });
  const tickCountIn = node.numberIn({ name: "tickCount", label: "Tick count", value: 4 });
  const tickMinStepIn = node.numberIn({ name: "tickMinStep", label: "Tick min step" });
  const tickOffsetIn = node.numberIn({ name: "tickOffset", label: "Tick offset" });
  const tickSizeIn = node.numberIn({ name: "tickSize", label: "Tick size", value: 5 });
  node.popSection();

  node.pushSection({ name: "Other", collapsed: true });
  const clipHeightIn = node.numberIn({ name: "clipHeight", label: "Clip height" });
  const zindexIn = node.numberIn({ name: "zindex", label: "Z-index", value: 1 });
  const interactiveIn = node.booleanIn({ name: "interactive", label: "Interactive", value: false });
  const encodeIn = node.booleanIn({ name: "encode", label: "encode", widget: "TEXT", value: {} });
  node.popSection();

  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot spec out" });
  const shapeOut = node.shapeOut({ name: "plotShape", label: "Plot shape" });

  node.onRender = () => {
    let specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : emptyPlot);
    validateVegaSpec(specOut);

    addLegend({
      spec: specOut,
      scaleNames: scaleIn.value,
      markName: markNameIn.value,
      title: titleIn.value,
      shape: shapeIn.value,
      values: parse2vegaRef(specOut, undefined, undefined, valuesIn),
      type: typeIn.value,
      orient: orientIn.value,
      direction: directionIn.value,
      columns: columnsIn.value,
      offset: offsetIn.value,
      padding: paddingIn.value,
      titlePadding: titlePaddingIn.value,
      rowPadding: rowPaddingIn.value,
      columnPadding: columnPaddingIn.value,
      cornerRadius: cornerRadiusIn.value,
      gradientLength: parse2vegaRef(specOut, undefined, undefined, gradientLengthIn),
      gradientThickness: parse2vegaRef(specOut, undefined, undefined, gradientThicknessIn),
      legendX: legendXIn.value,
      legendY: legendYIn.value,
      titleFont: titleFontIn.value,
      titleFontSize: titleFontSizeIn.value,
      titleFontWeight: titleFontWeightIn.value,
      titleColor: nbColorToCSS(titleColorIn.value),
      titleAlign: titleAlignIn.value,
      titleAnchor: titleAnchorIn.value,
      titleBaseline: titleBaselineIn.value,
      labelColor: nbColorToCSS(labelColorIn.value),
      labelFont: labelFontIn.value,
      labelFontSize: labelFontSizeIn.value,
      labelFontWeight: labelFontWeightIn.value,
      labelLimit: labelLimitIn.value,
      labelFormat: labelFormatIn.value,
      labelAlign: labelAlignIn.value,
      labelBaseline: labelBaselineIn.value,
      symbolSize: symbolSizeIn.value,
      symbolFillColor: nbColorToCSS(symbolFillColorIn.value),
      symbolStrokeColor: nbColorToCSS(symbolStrokeColorIn.value),
      symbolOpacity: symbolOpacityIn.value,
      symbolType: symbolTypeIn.value,
      gradientLabelOffset: gradientLabelOffsetIn.value,
      tickCount: tickCountIn.value,
      tickMinStep: tickMinStepIn.value,
      tickOffset: tickOffsetIn.value,
      tickSize: tickSizeIn.value,
      clipHeight: clipHeightIn.value,
      zindex: zindexIn.value,
      interactive: interactiveIn.value,
      fillScale: fillScaleIn.value,
      opacityScale: opacityScaleIn.value,
      shapeScale: shapeScaleIn.value,
      sizeScale: sizeScaleIn.value,
      strokeScale: strokeScaleIn.value,
      strokeDashScale: strokeDashScaleIn.value,
      strokeWidthScale: strokeWidthScaleIn.value,
      fillColor: nbColorToCSS(fillColorIn.value),
      strokeColor: nbColorToCSS(strokeColorIn.value),
      gradientOpacity: gradientOpacityIn.value,
      gradientStrokeColor: nbColorToCSS(gradientStrokeColorIn.value),
      gradientStrokeWidth: parse2vegaRef(specOut, undefined, undefined, gradientStrokeWidthIn),
      labelFontStyle: labelFontStyleIn.value,
      labelOpacity: labelOpacityIn.value,
      labelOverlap: labelOverlapIn.value,
      labelSeparation: labelSeparationIn.value,
      symbolDash: symbolDashIn.value,
      symbolDashOffset: symbolDashOffsetIn.value,
      symbolLimit: symbolLimitIn.value,
      symbolOffset: symbolOffsetIn.value,
      titleFontStyle: titleFontStyleIn.value,
      titleLimit: titleLimitIn.value,
      titleLineHeight: titleLineHeightIn.value,
      titleOpacity: titleOpacityIn.value,
      titleOrient: titleOrientIn.value,
      gridAlign: gridAlignIn.value,
      encode: encodeIn.value,
    });
    plotSpecOut.set(specOut);
  };
}
