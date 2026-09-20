/**
 * Define a scale to map a data attribute to a visual property of the plot.
 *
 * Predefined scales are automaticaly applied to the corresponding visual property unless
 * attribute and property parameter given.
 *
 * To define a scale, use Set Plot Scale.
 *
 * - Band position: Relative position on a band of a stacked, binned, time unit, or band scale.
 * For example, the marks will be positioned at the beginning of the band if set to 0, and at
 * the middle of the band if set to 0.5.
 *
 * @category Plot
 */

import { emptyPlot, validateVegaSpec, applyScale } from "project:Utilities";

export default function (node) {
  const plotSpecIn = node.specIn({ name: "plotSpecIn", label: "Plot spec" });

  node.pushSection("General");
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
  const customNameIn = node.stringIn({ name: "customName", label: "Custom scale name" });
  const attrNameIn = node.stringIn({ name: "attrName", label: "Attribute name" });
  const propNameIn = node.stringIn({ name: "propName", label: "Property name" });
  const bandIn = node.numberIn({ name: "band", label: "Band position" });
  node.popSection();

  const plotSpecOut = node.specOut({ name: "plotSpecOut", label: "Plot spec out" });

  // Function to apply scale in marks
  function applyScale_old({ spec, scaleName, plotType, attr, prop, markName, band }) {
    const property = prop || scaleDefaults.find((d) => d.scaleName === scaleName).property;
    const scaleAttr = attr || scaleDefaults.find((d) => d.scaleName === scaleName).scaleAttr;
    let newUpdate = {
      scale: scaleName,
      field: scaleAttr,
    };
    const scaleIdx = spec.scales.findIndex((d) => d.name === scaleName);
    if (scaleIdx >= 0) {
      if (spec.scales[scaleIdx].type === "band") {
        newUpdate.band = band;
      }
    }
    if (markName) {
      spec.marks.find((d) => d.name === markName).encode.update[property] = newUpdate;
    } else if (plotType) {
      spec.marks.find((d) => d.name === plotType).encode.update[property] = newUpdate;
    } else {
      spec.marks.forEach((mark) => {
        if (mark.type === "group") {
          mark.marks.forEach((submark) => {
            submark.encode.update[property] = newUpdate;
          });
        } else {
          mark.encode.update[property] = newUpdate;
        }
      });
    }
  }

  node.onRender = () => {
    let specOut = structuredClone(plotSpecIn.value ? plotSpecIn.value : emptyPlot);
    validateVegaSpec(specOut);

    // APPLY SCALES
    const scaleName = nameIn.value === "<custom>" ? customNameIn.value : nameIn.value;
    const scaleAttr = attrNameIn.value
      ? attrNameIn.value
      : nameIn.value === "<custom>"
        ? "__" + customNameIn.value + "_attr"
        : scaleDefaults.find((d) => d.scaleName === scaleName).scaleAttr;

    applyScale({ spec: specOut, scaleName: scaleName, prop: propNameIn.value, attr: scaleAttr, band: bandIn.value });

    plotSpecOut.set(specOut);
  };
}
