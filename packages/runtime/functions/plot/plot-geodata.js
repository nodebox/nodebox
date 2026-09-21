/**
 * Generate a default map.
 *
 * Parameters:
 * - Source CRS: This parameter provides the necessary information to interpret geographic coordinates.
 *   A CRS (Coordinate Reference System) is a system that defines how geographic data is mapped
 *   onto a flat surface. It includes three components:
 *   (1) The coordinate system defines how locations on Earth are described numerically.
 *      (e.g. geographical coordinates like latitude and longitude or
 *      projected Cartesian coordinates like X, Y coordinates)
 *   (2) The datum specifies the reference model of the Earth's shape (e.g., WGS84, NAD83)
 *       and its position relative to the Earth’s surface.
 *   (3) The projection is the method used to transform the Earth's curved surface into a flat,
 *       two-dimensional map.
 * - Target projection:
 * All source data will be reprojected to WGS84 (EPSG:4326) geographic coordinates.
 * The selected projection transforms these geographic coordinates (latitude and longitude)
 * into a new set of cartesian coordinates to plot on a flat 2D plane (X and Y coordinates).
 *
 * @category Geo
 */

import { debugPrint } from "project:Utilities";
import {
  validateVegaSpec,
  emptyMap,
  proj4Defs,
  scaleDefaults,
  addParamAttr,
  parse2vegaRef,
  setScale,
  applyScale,
  reprojectCoordinates,
  setMark,
  plotGeodata,
  setGraticule,
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
  const plotIn = node.specIn({ name: "plotSpec", label: "Plot spec" });
  const dataIn = node.tableIn({ name: "dataIn", label: "Geodata" });

  // Add parameters
  node.pushSection({ name: "General" });
  const dataNameIn = node.stringIn({
    name: "dataName",
    label: "Data name",
    value: "geodata",
  });
  const markNameIn = node.stringIn({
    name: "markName",
    label: "Mark name",
    value: "geoshape",
  });
  node.popSection();
  node.pushSection({ name: "Source data" });

  const coordSystemIn = node.stringIn({
    name: "crs",
    label: "Source CRS",
    value: "EPSG4326",
    choices: [
      ["EPSG4326", "WGS84 (EPSG:4326)"],
      ["EPSG3857", "Web Mercator (EPSG:3857)"],
      ["EPSG31370", "Belge Lambert 72 (EPSG:31370)"],
      ["EPSG4269", "NAD83 (EPSG:4269)"],
    ],
  });
  const formatIn = node.stringIn({
    name: "format",
    label: "Format",
    value: "json",
    choices: [
      ["json", "JSON"],
      ["geojson", "GeoJSON"],
      ["topojson", "TopoJSON"],
    ],
  });
  const featureIn = node.stringIn({
    name: "feature",
    label: "Feature",
  });
  node.popSection();
  node.pushSection({ name: "Mark properties" });
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

  const plotOut = node.specOut({ name: "plotSpecOut", label: "Plot spec" });

  node.onRender = () => {
    const dataName = dataNameIn.value;
    let specOut = structuredClone(plotIn.value ? plotIn.value : emptyMap);
    validateVegaSpec(specOut);
    let specData = specOut.data.find((d) => d.name == dataName);
    let geoData = dataIn.value ? structuredClone(dataIn.value) : specData ? specData.values : []; // check input data first, then plotSpec data
    const sourceCRS = coordSystemIn.value;

    // If geodata exists, apply the reprojection and plot it
    if (geoData) {
      // Reproject the coordinates based on the source CRS
      const reprojectedGeoData = reprojectCoordinates(geoData, sourceCRS);
      // Prepare parameters for plotGeodata
      const plotParams = {
        spec: specOut, // Current Vega spec
        geodata: reprojectedGeoData, // Reprojected geodata
        format: formatIn.value, // Data format,
        feature: featureIn.value,
        dataName: dataName,
        markName: markNameIn.value,
      };
      // Plot the reprojected geodata using Vega specifications
      specOut = plotGeodata(plotParams);
    }

    // SET SCALES
    const scaleNames = scaleDefaults.map((d) => d.scaleName);
    const scaleAttrs = scaleDefaults.map((d) => d.scaleAttr);
    const scaleProps = scaleDefaults.map((d) => d.property);
    const nodeParams = [
      undefined,
      undefined,
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

    scaleNames.forEach((scaleName, i) => {
      let domain = undefined;

      const paramVal = nodeParams[i] ? nodeParams[i].value : undefined;

      if (paramVal) {
        const paramType = nodeParams[i].node.values[nodeParams[i].name]
          ? nodeParams[i].node.values[nodeParams[i].name].type
          : undefined;

        // ADD SCALE PARAMS
        const scaleAttr = scaleAttrs[i];
        const nodeParam = nodeParams[i];

        // Add scaleAttr to data if expression, quoted string or single value (not list, array or object)
        const objRegex = new RegExp("^\\{.*\\}$");
        const arrRegex = new RegExp("^\\[.*\\]$");
        const litRegex = new RegExp('^".*"$');
        const commaRegex = new RegExp(",");
        if (paramType === "EXPRESSION") {
          addParamAttr(specOut, dataName, scaleAttr, nodeParam, true);
        } else if (paramType === "VALUE" && typeof paramVal === "number") {
          //addParamAttr(specOut, dataName, scaleAttr, nodeParam);
        } else if (paramType === "VALUE" && paramVal.match(litRegex) && paramVal !== "") {
          //addParamAttr(specOut, dataName, scaleAttr, nodeParam);
        } else if (
          paramType === "VALUE" &&
          !paramVal.match(objRegex) &&
          !paramVal.match(arrRegex) &&
          !paramVal.match(commaRegex) &&
          paramVal !== ""
        ) {
          //addParamAttr(specOut, dataName, scaleAttr, nodeParam, true);
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
          //domain = parse2vegaRef(specOut, dataName, scaleAttrs[i], nodeParams[i]);
          domain = parse2vegaRef(specOut, dataName, scaleAttrs[i], nodeParams[i]);
          setScale({
            spec: specOut,
            dataName: dataName,
            scaleName: scaleName,
            plotType: undefined,
            domain: undefined,
            //typeof domain !== "object" ? domain : Object.keys(domain).includes("value") ? [domain.value] : domain,
          });
          applyScale({ spec: specOut, scaleName: scaleName, plotType: undefined });
        }
      }
    });

    plotOut.set(specOut);
  };
}
