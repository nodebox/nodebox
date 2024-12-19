/**
 * Define a scale to map data to a visual property of the plot.
 *
 * A scale defines the mapping of a data attribute value to a visual property.
 * Predefined scales are automaticaly applied to set the corresponding visual property.
 *
 * To define a custom scale, set the Scale name parameter to <custom> and provide a Custom scale name.
 * A custom scale will be available in the plot specification but must be applied in the Set Marks node.
 * - explicitly by using the Vega scale syntax {scale:"scaleName", field: "attributeName"} in the mark properties.
 * - explicitly by using Apply Plot Scale.
 * - implicitly by providing an existing scale name in a parameter expression. The scale will be
 *    applied to the domain of the scale.
 *
 * @category Plot
 */

import {
  emptyPlot,
  validateVegaSpec,
  scaleDefaults,
  addParamAttr,
  parse2vegaRef,
  setScale,
  applyScale,
} from "project:Utilities";

export default function (node) {
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });
  const dataIn = node.tableIn({ name: "data", label: "Data" });
  const shapeIn = node.shapeIn({ name: "shapeRange", label: "Shape range" });

  node.pushSection({ name: "General" });
  const nameIn = node.stringIn({
    name: "scaleName",
    label: "Scale name",
    value: "xScale",
    choices: [
      "xScale",
      "yScale",
      "fillColorScale",
      "strokeColorScale",
      "sizeScale",
      "strokeWidthScale",
      "shapeScale",
      "opacityScale",
      "fillOpacityScale",
      "strokeOpacityScale",
      "strokeDashScale",
      "gridUnitScale",
      "<custom>",
    ],
  });
  const customNameIn = node.stringIn({ name: "customName", label: "Custom name" });
  const scaleTypeIn = node.stringIn({
    name: "scaleType",
    label: "Scale type",
    value: "<auto>",
    choices: ["<auto>", "linear", "ordinal", "band", "point", "quantize", "time", "utc", "pow", "sqrt"],
  });
  const dataNameIn = node.stringIn({ name: "dataName", label: "Data name", value: "table" });
  const domainIn = node.stringIn({ name: "domain", label: "Domain" });
  const sortAttrIn = node.stringIn({ name: "sortAttribute", label: "Sort attribute" });
  const orderIn = node.stringIn({
    name: "order",
    label: "Order",
    value: "<none>",
    choices: ["<none>", "ascending", "descending"],
  });
  const rangeIn = node.stringIn({ name: "range", label: "Range" });
  const niceIn = node.booleanIn({ name: "nice", label: "Nice", value: true });
  const zeroIn = node.booleanIn({ name: "zero", label: "Zero" });
  const domainMinIn = node.numberIn({ name: "domainMin", label: "Domain min" });
  const domainMaxIn = node.numberIn({ name: "domainMax", label: "Domain max" });
  node.popSection();

  node.pushSection({ name: "Band scale", collapsed: true });
  const alignIn = node.numberIn({ name: "align", label: "Align", value: 0.5 });
  const domainImplIn = node.booleanIn({ name: "domainImpl", label: "Domain implicit" });
  const paddingIn = node.numberIn({ name: "padding", label: "Padding" });
  const padInnerIn = node.numberIn({ name: "padInner", label: "Padding inner" });
  const padOuterIn = node.numberIn({ name: "padOuter", label: "Padding outer" });
  node.popSection();

  node.pushSection({ name: "Time scale", collapsed: true });
  const timeIntervalIn = node.stringIn({
    name: "timeInterval",
    label: "Time interval",
    value: "<none>",
    choices: ["<none>", "millisecond", "second", "minute", "hour", "day", "week", "month", "year"],
  });
  const timeStepIn = node.numberIn({ name: "timeStep", label: "Time step" });
  node.popSection;

  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot spec out" });

  node.onRender = () => {
    const dataName = dataNameIn.value;
    let specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : emptyPlot);
    validateVegaSpec(specOut);

    // ADD SCALE PARAMS

    const scaleName = nameIn.value === "<custom>" ? customNameIn.value : nameIn.value;
    const scaleAttr =
      nameIn.value === "<custom>"
        ? "__" + customNameIn.value + "_attr"
        : scaleDefaults.find((d) => d.scaleName === scaleName).scaleAttr;
    const nodeParam = domainIn;

    // Add scaleAttr to data if expression, quoted string or single value (not list, array or object)
    const objRegex = new RegExp("^\\{.*\\}$");
    const arrRegex = new RegExp("^\\[.*\\]$");
    const litRegex = new RegExp('^".*"$');
    const commaRegex = new RegExp(",");
    const paramType = nodeParam.node.values[nodeParam.name] ? nodeParam.node.values[nodeParam.name].type : "VALUE";
    const paramVal = nodeParam.value;
    if (paramType === "EXPRESSION") {
      addParamAttr(specOut, dataName, scaleAttr, nodeParam, true);
    } else if (paramType === "VALUE" && typeof paramVal === "number") {
      addParamAttr(specOut, dataName, scaleAttr, nodeParam, true);
    } else if (paramType === "VALUE" && paramVal.match(litRegex) && paramVal !== "") {
      addParamAttr(specOut, dataName, scaleAttr, nodeParam, true);
    } else if (
      paramType === "VALUE" &&
      !paramVal.match(objRegex) &&
      !paramVal.match(arrRegex) &&
      !paramVal.match(commaRegex) &&
      paramVal !== ""
    ) {
      addParamAttr(specOut, dataName, scaleAttr, nodeParam, true);
    }

    // Format domain and range
    const domain = parse2vegaRef(specOut, dataName, scaleAttr, domainIn);
    let range;
    let shapeRange = shapeIn.value;

    if (shapeRange !== undefined && shapeRange !== null) {
      // Take children if merged group shape
      if (shapeRange.tags.includes("_merged")) {
        shapeRange = shapeRange.children;
      } else {
        shapeRange = [shapeRange];
      }
      range = shapeRange.map((s) => s.toPathData({ applyTransform: true }));
    } else {
      range = parse2vegaRef(specOut, dataName, scaleAttr + "_range", rangeIn);
    }

    if (typeof range === "object" && !Array.isArray(range)) {
      if (Object.keys(range).includes("value")) {
        range = [range.value];
      }
    }

    setScale({
      spec: specOut,
      dataName: dataName,
      scaleName: scaleName,
      type: scaleTypeIn.value == "<auto>" ? undefined : scaleTypeIn.value,
      domain: typeof domain !== "object" ? domain : Object.keys(domain).includes("value") ? [domain.value] : domain,
      sortAttr: sortAttrIn.value || undefined,
      order: orderIn.value || undefined,
      range: range,
      nice: niceIn.value,
      zero: zeroIn.value,
      domainMin: domainMinIn.value,
      domainMax: domainMaxIn.value,
      align: alignIn.value,
      domainImplicit: domainImplIn.value,
      padding: paddingIn.value,
      paddingInner: padInnerIn.value,
      paddingOuter: padOuterIn.value,
      timeInterval: timeIntervalIn.value,
      timeStep: timeStepIn.value,
    });

    if (!["<custom>", "gridUnitScale"].includes(nameIn.value)) applyScale({ spec: specOut, scaleName: scaleName });

    plotSpecOut.set(specOut);
  };
}
