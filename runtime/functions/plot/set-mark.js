/**
 * Update or add a mark to the plot.
 *
 * If a custom scale was defined using Set Plot Scale, the scale can be applied to an attribute
 * - explicitly by using the Vega scale syntax {scale:"scaleName", field: "attributeName"}
 * - implicitly by providing an existing scale name in a parameter expression. The scale will be
 *    applied to the domain of the scale.
 *
 * @category Plot
 */

import { emptyPlot, validateVegaSpec, parse2vegaRef, setMark } from "project:Utilities";

export default function (node) {
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });

  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot spec out" });
  const shapeOut = node.shapeOut({ name: "plotShape", label: "Plot shape" });

  node.pushSection({ name: "General" });
  const modeIn = node.stringIn({
    name: "mode",
    label: "Mode",
    value: "upsert",
    choices: [
      ["upsert", "Update or Insert"],
      ["replace", "Replace or Insert"],
      ["delete", "Delete"],
      ["update_all", "Update all"],
      ["truncate", "Replace all"],
      ["truncate_reuse", "Replace all (keep style)"],
    ],
  });
  const dataNameIn = node.stringIn({ name: "dataName", label: "Data name", value: "table" });
  const markNameIn = node.stringIn({ name: "markName", label: "Mark name", value: "exampleMark" });
  const markTypeIn = node.stringIn({
    name: "markType",
    label: "Mark type",
    value: "symbol",
    choices: ["arc", "area", "image", "line", "path", "rect", "rule", "shape", "symbol", "text", "trail"],
  });
  const insertIdxIn = node.numberIn({ name: "insertIdx", label: "Insert at index", value: -1 });

  const groupByIn = node.stringIn({ name: "groupBy", label: "Group by" });
  const groupNameIn = node.stringIn({ name: "groupName", label: "Group name" });
  node.popSection();

  node.pushSection({ name: "Visual properties" });
  const xIn = node.stringIn({ name: "x", label: "X" });
  const yIn = node.stringIn({ name: "y", label: "Y" });
  const x2In = node.stringIn({ name: "x2", label: "X2" });
  const y2In = node.stringIn({ name: "y2", label: "Y2" });
  const widthIn = node.numberIn({ name: "width", label: "Width" });
  const heightIn = node.numberIn({ name: "height", label: "Height" });
  const opacityIn = node.numberIn({ name: "opacity", label: "Opacity" });
  const fillIn = node.stringIn({ name: "fill", label: "Fill" });
  const strokeIn = node.stringIn({ name: "stroke", label: "Stroke" });
  const strokeWidthIn = node.numberIn({ name: "strokeWidth", label: "Stroke width" });
  const sizeIn = node.numberIn({ name: "size", label: "Size" });
  const shapeIn = node.stringIn({ name: "shape", label: "Shape" });
  const pathIn = node.stringIn({ name: "path", label: "Path" });
  node.popSection();

  node.pushSection({ name: "Other", collapsed: true });
  const orientIn = node.stringIn({ name: "orient", label: "Orient" });
  const tooltipIn = node.stringIn({ name: "tooltip", label: "Tooltip" });
  const orderIn = node.numberIn({ name: "order", label: "Order" });
  const urlIn = node.stringIn({ name: "url", label: "URL" });
  const alignIn = node.stringIn({ name: "align", label: "Align" });
  const baselineIn = node.stringIn({ name: "baseline", label: "Baseline" });
  const clipIn = node.booleanIn({ name: "clip", label: "Clip" });
  const dxIn = node.numberIn({ name: "dx", label: "dX" });
  const dyIn = node.numberIn({ name: "dy", label: "dY" });
  const endAngleIn = node.numberIn({ name: "endAngle", label: "End angle" });
  const innerRadiusIn = node.numberIn({ name: "innerRadius", label: "Inner radius" });
  const outerRadiusIn = node.numberIn({ name: "outerRadius", label: "Outer radius" });
  const startAngleIn = node.numberIn({ name: "startAngle", label: "Start angle" });
  const textIn = node.stringIn({ name: "text", label: "Text" });
  const limitIn = node.numberIn({ name: "limit", label: "Limit" });
  const thetaIn = node.numberIn({ name: "theta", label: "Theta" });
  const interpolateIn = node.stringIn({ name: "interpolate", label: "Interpolate" });
  const tensionIn = node.numberIn({ name: "tension", label: "Tension" });
  const definedIn = node.booleanIn({ name: "defined", label: "Defined" });
  const aspectIn = node.booleanIn({ name: "aspect", label: "Aspect" });
  const scaleIn = node.stringIn({ name: "scale", label: "Scale" });
  const zindexIn = node.numberIn({ name: "zindex", label: "Z-index" });
  const hrefIn = node.stringIn({ name: "href", label: "Href" });
  const roleIn = node.stringIn({ name: "role", label: "Role" });
  const descriptionIn = node.stringIn({ name: "description", label: "Description" });
  const styleIn = node.stringIn({ name: "style", label: "Style" });
  const sortIn = node.stringIn({ name: "sort", label: "Sort" });
  const keyIn = node.stringIn({ name: "key", label: "Key" });
  const onIn = node.stringIn({ name: "on", label: "On" });
  const encodeIn = node.stringIn({ name: "encode", label: "Encode" });
  const transformIn = node.stringIn({ name: "transform", label: "Transform" });
  node.popSection();

  node.pushSection({ name: "Filter", collapsed: true });
  const filterIn = node.stringIn({ name: "filter", label: "Filter", widget: "TEXT" });
  node.popSection();

  node.onRender = () => {
    let specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : emptyPlot);
    validateVegaSpec(specOut);
    const dataName = dataNameIn.value;
    const markName = markNameIn.value;

    setMark({
      mode: modeIn.value || undefined,
      insertIdx: insertIdxIn.value || -1,
      spec: specOut,
      dataName: dataName,
      markName: markName,
      markType: markTypeIn.value || undefined,
      groupBy: groupByIn.value || undefined,
      groupName: groupNameIn.value || undefined,
      x: parse2vegaRef(specOut, dataName, "__" + markName + "_x", xIn) || undefined,
      y: parse2vegaRef(specOut, dataName, "__" + markName + "_y", yIn) || undefined,
      x2: parse2vegaRef(specOut, dataName, "__" + markName + "_x2", x2In) || undefined,
      y2: parse2vegaRef(specOut, dataName, "__" + markName + "_y2", y2In) || undefined,
      width: parse2vegaRef(specOut, dataName, "__" + markName + "_width", widthIn) || undefined,
      height: parse2vegaRef(specOut, dataName, "__" + markName + "_height", heightIn) || undefined,
      opacity: parse2vegaRef(specOut, dataName, "__" + markName + "_opacity", opacityIn) || undefined,
      fill: parse2vegaRef(specOut, dataName, "__" + markName + "_fill", fillIn) || undefined,
      stroke: parse2vegaRef(specOut, dataName, "__" + markName + "_stroke", strokeIn) || undefined,
      strokeWidth: parse2vegaRef(specOut, dataName, "__" + markName + "_strokeWidth", strokeWidthIn) || undefined,
      size: parse2vegaRef(specOut, dataName, "__" + markName + "_size", sizeIn) || undefined,
      shape: parse2vegaRef(specOut, dataName, "__" + markName + "_shape", shapeIn) || undefined,
      path: pathIn.value || undefined,
      orient: orientIn.value || undefined,
      tooltip: tooltipIn.value || undefined,
      order: orderIn.value || undefined,
      url: urlIn.value || undefined,
      align: alignIn.value || undefined,
      baseline: baselineIn.value || undefined,
      clip: clipIn.value || undefined,
      dx: dxIn.value || undefined,
      dy: dyIn.value || undefined,
      endAngle: endAngleIn.value || undefined,
      innerRadius: innerRadiusIn.value || undefined,
      outerRadius: outerRadiusIn.value || undefined,
      startAngle: startAngleIn.value || undefined,
      text: textIn.value || undefined,
      limit: limitIn.value || undefined,
      theta: thetaIn.value || undefined,
      interpolate: interpolateIn.value || undefined,
      tension: tensionIn.value || undefined,
      defined: definedIn.value || undefined,
      aspect: aspectIn.value || undefined,
      scale: scaleIn.value || undefined,
      zindex: zindexIn.value || undefined,
      href: hrefIn.value || undefined,
      role: roleIn.value || undefined,
      description: descriptionIn.value || undefined,
      style: styleIn.value || undefined,
      sort: sortIn.value || undefined,
      key: keyIn.value || undefined,
      on: onIn.value || undefined,
      encode: encodeIn.value || undefined,
      transform: transformIn.value || undefined,
      filter: filterIn.value || undefined,
    });

    plotSpecOut.set(specOut);
  };
}
