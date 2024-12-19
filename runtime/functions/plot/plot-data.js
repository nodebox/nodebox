/**
 * Generate a default plot.
 *
 * @category Plot
 */

import {
  emptyPlot,
  validateVegaSpec,
  scaleDefaults,
  parse2vegaRef,
  addParamAttr,
  addPlotData,
  setScale,
  applyScale,
  setMark,
  setAxis,
  setPlotType,
} from "project:Utilities";
import { parse as parseVega } from "https://esm.sh/vega@5";
import { min, max, ascending, descending, rollup, sum, bin, range as d3range } from "https://esm.sh/d3-array@3.2.4";
import {
  scaleLinear,
  scaleTime,
  scaleUtc,
  scalePow,
  scaleSqrt,
  scaleLog,
  scaleSymlog,
  scaleOrdinal,
  scaleBand,
  scalePoint,
  scaleDiverging,
  scaleQuantile,
  scaleQuantize,
  scaleThreshold,
} from "https://esm.sh/d3-scale@4.0.2";

export default function (node) {
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });
  const dataIn = node.tableIn({ name: "data", label: "Data" });

  node.pushSection({ name: "General" });
  const plotTypeIn = node.stringIn({
    name: "plotType",
    label: "Plot type",
    value: "scatter",
    choices: ["scatter", "segment", "lollipop", "bar", "line", "area", "waffle"],
  });
  const groupByIn = node.stringIn({ name: "groupBy", label: "Group by" });
  node.popSection();
  node.pushSection({ name: "Mark properties" });
  const xIn = node.stringIn({ name: "X", label: "X" });
  const yIn = node.stringIn({ name: "Y", label: "Y" });
  const fillIn = node.stringIn({ name: "fillColor", label: "Fill color" });
  const strokeIn = node.stringIn({ name: "strokeColor", label: "Stroke color" });
  const sizeIn = node.numberIn({ name: "size", label: "Size" });
  const strokeWidthIn = node.numberIn({ name: "strokeWidth", label: "Stroke width" });
  const shapeIn = node.stringIn({ name: "shape", label: "Shape" });
  const opacityIn = node.numberIn({ name: "opacity", label: "Opacity" });
  const fillOpacityIn = node.numberIn({ name: "fillOpacity", label: "Fill opacity" });
  const strokeOpacityIn = node.numberIn({ name: "strokeOpacity", label: "Stroke opacity" });
  const strokeDashIn = node.stringIn({ name: "strokeDash", label: "Stroke dash" });
  node.popSection();
  node.pushSection({ name: "Other", collapsed: true });
  const dataNameIn = node.stringIn({
    name: "dataName",
    label: "Data name",
    value: "table",
  });
  const markNameIn = node.stringIn({
    name: "markName",
    label: "Mark name",
  });
  node.popSection();

  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot spec out" });

  node.onRender = () => {
    let dataName = dataNameIn.value;
    let markName = markNameIn.value === "" ? undefined : markNameIn.value;
    const plotType = plotTypeIn.value;
    let specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : emptyPlot);
    validateVegaSpec(specOut);
    //vega.validate(specOut);
    let specData = specOut.data.find((d) => d.name == dataName);
    let data = dataIn.value ? structuredClone(dataIn.value) : specData ? specData.values : []; // check input data first, then plotSpec data

    addPlotData(specOut, data, dataName);

    // SET SCALES

    const scaleNames = scaleDefaults.map((d) => d.scaleName);
    const scaleAttrs = scaleDefaults.map((d) => d.scaleAttr);
    const scaleProps = scaleDefaults.map((d) => d.property);
    const nodeParams = [
      xIn,
      yIn,
      fillIn,
      strokeIn,
      sizeIn,
      strokeWidthIn,
      shapeIn,
      opacityIn,
      fillOpacityIn,
      strokeOpacityIn,
      strokeDashIn,
    ];

    //addParamAttr (specOut, dataName, scaleAttrs, nodeParams)
    scaleNames.forEach((scaleName, i) => {
      let domain = undefined;
      const paramVal = nodeParams[i].value;

      if (paramVal) {
        const paramType = nodeParams[i].node.values[nodeParams[i].name]
          ? nodeParams[i].node.values[nodeParams[i].name].type
          : undefined;
        //if (["opacityScale", "fillOpacityScale", "strokeOpacityScale", "sizeScale", "strokeWidthScale"].includes(scaleName) &&
        //const paramVal = nodeParams[i].value

        // ADD SCALE PARAMS

        //const scaleName = (nameIn.value==="<custom>")?customNameIn.value : nameIn.value
        const scaleAttr = scaleAttrs[i];
        const nodeParam = nodeParams[i];

        // Add scaleAttr to data if expression, quoted string or single value (not list, array or object)
        const objRegex = new RegExp("^\\{.*\\}$");
        const arrRegex = new RegExp("^\\[.*\\]$");
        const litRegex = new RegExp('^".*"$');
        const commaRegex = new RegExp(",");
        //const paramType = nodeParam.node.values[nodeParam.name] ? nodeParam.node.values[nodeParam.name].type : "VALUE"
        //const paramVal=nodeParam.value
        if (paramType === "EXPRESSION") {
          addParamAttr(specOut, dataName, scaleAttr, nodeParam);
        } else if (paramType === "VALUE" && typeof paramVal === "number") {
          addParamAttr(specOut, dataName, scaleAttr, nodeParam);
        } else if (paramType === "VALUE" && paramVal.match(litRegex) && paramVal !== "") {
          addParamAttr(specOut, dataName, scaleAttr, nodeParam);
        } else if (
          paramType === "VALUE" &&
          !paramVal.match(objRegex) &&
          !paramVal.match(arrRegex) &&
          !paramVal.match(commaRegex) &&
          paramVal !== ""
        ) {
          addParamAttr(specOut, dataName, scaleAttr, nodeParam);
        }

        let result;
        if ((paramType === "VALUE") & (typeof paramVal !== "number")) {
          const regex = new RegExp('^".*"$');
          result = paramVal.split(",").map((d) => (!isNaN(Number(d)) ? Number(d) : d.match(regex) ? JSON.parse(d) : d)); // Convert comma-separated value to array
          if (!Array.isArray(result) || result.length === 1) result = result[0];
        } else {
          result = paramVal;
        }

        if (paramType === "VALUE" && !Array.isArray(result)) {
          // Set mark if parameter is value
          const scaleProp = scaleProps[i];
          setMark({
            mode: "update_all",
            spec: specOut,
            dataName: dataName,
            [scaleProp]: parse2vegaRef(specOut, dataName, scaleAttrs[i], nodeParams[i]),
          });
        } else {
          // Set and apply scale
          domain = parse2vegaRef(specOut, dataName, scaleAttrs[i], nodeParams[i]);
          setScale({
            spec: specOut,
            dataName: dataName,
            scaleName: scaleName,
            plotType: plotType,
            domain:
              typeof domain !== "object" ? domain : Object.keys(domain).includes("value") ? [domain.value] : domain,
          });
          applyScale({ spec: specOut, scaleName: scaleName, dataName: dataName, plotType: plotType });
        }

        // Add axes titles
        if (["xScale", "yScale"].includes(scaleName)) {
          setAxis({
            spec: specOut,
            scale: scaleName,
            orient: scaleName === "xScale" ? "bottom" : "left",
            title: nodeParams[i].value,
            offset: 5,
            remove: plotTypeIn.value === "waffle",
          });
        }
      }
    });

    let offset;
    // Waffle plot: set default inner padding and remove axis
    if (plotTypeIn.value === "waffle") {
      // Set default offset
      offset = "normalize";
      // Set default padding
      specOut.scales.forEach((d) => {
        if (["xScale", "yScale"].includes(d.name)) {
          d.paddingInner = 0.1;
        }
      });
      // Remove axes
      ["xScale", "yScale"].forEach((s) => {
        setAxis({ spec: specOut, scale: s, remove: true });
      });
    }

    setPlotType({
      spec: specOut,
      dataName: dataName,
      markName: markName,
      type: plotTypeIn.value,
      shape: shapeIn.value,
      groupBy: groupByIn.value,
      offset: offset,
    });

    plotSpecOut.set(specOut);
  };
}
