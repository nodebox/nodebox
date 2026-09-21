/**
 * Set the type of the plot.
 *
 *
 * - Grid unit ratio: this allows you to define the ratio between a data unit (in the Group by attribute)
 * and a grid unit. Only enabled for offset zero and center.
 *
 * @category Plot
 */

import { emptyPlot, validateVegaSpec, setPlotType } from "project:Utilities";

export default function (node) {
  node.pushSection({ name: "General" });
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });
  const dataNameIn = node.stringIn({ name: "dataName", label: "Data name", value: "table" });
  const typeIn = node.stringIn({
    name: "plotType",
    label: "Plot type",
    value: "scatter",
    choices: ["scatter", "segment", "lollipop", "bar", "line", "area", "waffle"],
  });
  const shapeIn = node.stringIn({
    name: "shape",
    label: "Shape",
    value: "<default>",
    choices: [
      "<default>",
      "circle",
      "square",
      "triangle",
      "cross",
      "diamond",
      "star",
      "triangle-up",
      "triangle-down",
      "triangle-right",
      "triangle-left",
      "stroke",
      "arrow",
      "wedge",
    ],
  });
  const groupByIn = node.stringIn({ name: "groupBy", label: "Group by" });
  const stackIn = node.booleanIn({ name: "stacked", label: "Stack", value: false });
  const offsetIn = node.stringIn({
    name: "offset",
    label: "Offset",
    value: "zero",
    choices: ["zero", "center", "normalize"],
  });
  const sortAttrIn = node.stringIn({ name: "sortAttribute", label: "Sort attribute" });
  const orderIn = node.stringIn({
    name: "order",
    label: "Order",
    value: "ascending",
    choices: ["ascending", "descending"],
  });
  const dirIn = node.stringIn({
    name: "direction",
    label: "Direction",
    value: "Y",
    choices: ["Y", "X"],
  });
  const baseIn = node.numberIn({ name: "baseline", label: "Baseline", value: 999 });
  node.popSection();

  node.pushSection({ name: "Waffle", collapsed: true });
  const unitRatioIn = node.numberIn({ name: "unitRatio", label: "Grid unit ratio", value: 1 });
  node.popSection();

  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot spec out" });
  const shapeOut = node.shapeOut({ name: "plotShape", label: "Plot shape" });

  node.onRender = () => {
    let specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : emptyPlot);
    validateVegaSpec(specOut);
    const dataName = dataNameIn.value;

    setPlotType({
      spec: specOut,
      dataName: dataName,
      type: typeIn.value,
      shape: shapeIn.value == "<default>" ? undefined : shapeIn.value,
      groupBy: groupByIn.value,
      stacked: stackIn.value,
      offset: offsetIn.value,
      sortAttr: sortAttrIn.value,
      order: orderIn.value,
      direction: dirIn.value,
      baseline: baseIn.value,
      unitRatio: unitRatioIn,
    });

    plotSpecOut.set(specOut);
  };
}
