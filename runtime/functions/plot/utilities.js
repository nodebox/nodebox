/*
 * Utility functions and variables for NodeBox Plot.
 */

import { parse as parseVega, parseExpression, expressionFunction } from "https://esm.sh/vega@5";
import {
  min,
  max,
  ascending,
  descending,
  rollup,
  sum,
  bin,
  range as d3range,
  ticks,
} from "https://esm.sh/d3-array@3.2.4";
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
import {
  geoAlbers,
  geoAlbersUsa,
  geoAzimuthalEqualArea,
  geoAzimuthalEquidistant,
  geoConicConformal,
  geoConicEqualArea,
  geoConicEquidistant,
  geoEquirectangular,
  geoGnomonic,
  geoMercator,
  geoOrthographic,
  geoStereographic,
  geoTransverseMercator,
  geoNaturalEarth1,
} from "https://esm.sh/d3-geo@3.1.1";
import proj4 from "https://esm.sh/proj4@2";

/*
-------- GENERAL FUNCTIONS --------
*/
export function debugPrint(value) {
  return console.log("DEBUG PRINT", value);
}

export function validateVegaSpec(spec) {
  try {
    parseVega(spec); // Parses the spec, throws an error if invalid
    //console.log('Vega specification is valid.');
    return true;
  } catch (err) {
    throw new Error("Invalid Vega specification.", err.message);
  }
}

// Function to convert nodeBox color format to CSS color string
export function nbColorToCSS(color) {
  // Check if the input is a string (CSS color)
  if (color === undefined) {
    return color;
  } else if (typeof color === "string") {
    return color; // Return CSS color as is
  } else {
    // If the input is an object, process the RGBA values
    const { r, g, b, a } = color;
    const red = Math.round(r * 255);
    const green = Math.round(g * 255);
    const blue = Math.round(b * 255);
    return `rgba(${red}, ${green}, ${blue}, ${a})`;
  }
}
// Helper to check arrays for NaN recursively
function containsNaN(arr) {
  if (arr !== undefined && Array.isArray(arr)) {
    return arr.some((item) => {
      if (Array.isArray(item)) {
        // Recursively check nested arrays
        return containsNaN(item);
      }
      // Check if the item is NaN
      return typeof item === "number" && isNaN(item);
    });
  } else {
    return false;
  }
}

/*
-------- DEFAULTS VARIABLES --------
*/

export const emptyPlot = {
  $schema: "https://vega.github.io/schema/vega/v5.json",
  description: "A default empty plot specification.",
  width: 400,
  height: 400,
  padding: 10,
  autosize: "pad",
  config: {
    background: "#fff",
  },
  signals: [],
  data: [
    {
      name: "table",
      values: [],
    },
  ],
  scales: [
    {
      name: "xScale",
      type: "linear",
      range: "width",
      domain: [0, 1],
    },
    {
      name: "yScale",
      type: "linear",
      range: "height",
      domain: [0, 1],
    },
  ],
  projections: [],
  axes: [
    {
      orient: "bottom",
      scale: "xScale",
      offset: 5,
    },
    {
      orient: "left",
      scale: "yScale",
      offset: 5,
    },
  ],
  legends: [],
  marks: [
    {
      type: "symbol",
      from: { data: "table" },
      encode: {
        enter: {
          size: { value: 50 },
        },
        update: {
          x: { scale: "xScale", field: "__x" },
          y: { scale: "yScale", field: "__y" },
        },
      },
    },
  ],
};

export const emptyMap = {
  $schema: "https://vega.github.io/schema/vega/v5.json",
  description: "A default empty geodata plot specification.",
  width: 400,
  height: 400,
  padding: 10,
  autosize: "none",
  config: {
    background: "#fff",
  },
  projections: [],
  signals: [],
  data: [],
  scales: [],
  marks: [],
};

// Set of default scales
const [starSVG] = ["M0,.5L.6,.8L.5,.1L1,-.3L.3,-.4L0,-1L-.3,-.4L-1,-.3L-.5,.1L-.6,.8L0,.5Z"];
export const symbols = [
  "circle",
  "square",
  "triangle",
  "cross",
  "diamond",
  starSVG,
  "triangle-down",
  "triangle-right",
  "triangle-left",
  "stroke",
  "triangle-up",
  "wedge",
  "arrow",
];

export const scaleDefaults = [
  { scaleName: "xScale", range: "width", scaleAttr: "__x", property: "x" },
  { scaleName: "yScale", range: "height", scaleAttr: "__y", property: "y" },
  { scaleName: "fillColorScale", range: "category", scaleAttr: "__fillColor", property: "fill" },
  { scaleName: "strokeColorScale", range: "category", scaleAttr: "__strokeColor", property: "stroke" },
  { scaleName: "sizeScale", range: [4, 200], scaleAttr: "__size", property: "size" },
  { scaleName: "strokeWidthScale", range: [0, 10], scaleAttr: "__strokeWidth", property: "strokeWidth" },
  { scaleName: "shapeScale", range: symbols, scaleAttr: "__shape", property: "shape" },
  { scaleName: "opacityScale", range: [0, 1], scaleAttr: "__opacity", property: "opacity" },
  { scaleName: "fillOpacityScale", range: [0, 1], scaleAttr: "__fillOpacity", property: "fillOpacity" },
  { scaleName: "strokeOpacityScale", range: [0, 1], scaleAttr: "__strokeOpacity", property: "strokeOpacity" },
  {
    scaleName: "strokeDashScale",
    range: [
      [10, 5],
      [2, 2, 5, 2],
      [8, 4],
      [4, 4],
      [12, 3],
      [6, 2],
      [3, 6],
      [9, 2],
      [7, 5],
      [5, 8],
    ],
    scaleAttr: "__strokeDash",
    property: "strokeDash",
  },
];

// Define an array of objects containing the mapping logic
export const scaleTypeMapping = [
  { domainType: "numerical", rangeType: "numerical", plotType: "scatter", scaleType: "linear" },
  { domainType: "categorical", rangeType: "numerical", plotType: "default", scaleType: "point" },
  { domainType: "categorical", rangeType: "categorical", plotType: "default", scaleType: "ordinal" },
  { domainType: "numerical", rangeType: "categorical", plotType: "default", scaleType: "quantize" },
  { domainType: "categorical", rangeType: "numerical", plotType: "bar", scaleType: "band" },
  { domainType: "numerical", rangeType: "numerical", plotType: "waffle", scaleType: "band" },
  { domainType: "numerical", rangeType: "categorical", plotType: "waffle", scaleType: "quantize" },
  { domainType: "numerical", rangeType: "color", plotType: "heatmap", scaleType: "sequential" },
  { domainType: "numerical", rangeType: "color", plotType: "default", scaleType: "linear" },
  { domainType: "numerical", rangeType: "numerical", plotType: "histogram", scaleType: "quantize" },
  { domainType: "numerical", rangeType: "numerical", plotType: "default", scaleType: "linear" },
  { domainType: "temporal", rangeType: "numerical", plotType: "default", scaleType: "time" },
  { domainType: "categorical", rangeType: "numerical", plotType: "bar", scaleType: "band" },
  { domainType: "categorical", rangeType: "numerical", plotType: "point", scaleType: "band" },
  { domainType: "categorical", rangeType: "numerical", plotType: "default", scaleType: "ordinal" },
];

// Proj4 definitions of EPSG coordinate systems for use in reprojections.
export const proj4Defs = {
  NAD83: "+proj=longlat +datum=NAD83 +no_defs",
  EPSG4326: "+proj=longlat +datum=WGS84 +no_defs",
  EPSG3857: "+proj=merc +lon_0=0 +k=1 +x_0=0 +y_0=0 +datum=WGS84 +units=m +no_defs",
};

/*
-------- HELPER FUNCTIONS FOR PARAMETER AND EXPRESSION HANDLING --------
*/

// Function to parse NodeBox parameter (=update for .fn method)
// TODO:: create proper method to check value or expression reference or expression result of input
export function fn2(nodeParamIn) {
  let result;
  const objRegex = new RegExp("^\\{.*\\}$");
  const arrRegex = new RegExp("^\\[.*\\]$");
  const litRegex = new RegExp('^".*"$');
  const paramVal = nodeParamIn.value;
  const paramType = nodeParamIn.node.values[nodeParamIn.name]
    ? nodeParamIn.node.values[nodeParamIn.name].type
    : undefined;

  return (data) => {
    if (!paramVal) {
      result = paramVal; // undefined
    } else if (paramType === "VALUE" && typeof paramVal === "number") {
      // number value
      result = paramVal;
    } else if (paramType === "VALUE" && paramVal.match(objRegex)) {
      // object JSON value
      result = JSON.parse(paramVal);
    } else if (paramType === "VALUE" && paramVal.match(arrRegex)) {
      // array JSON value
      result = JSON.parse(paramVal);
    } else if (paramType === "VALUE" && paramVal.match(litRegex)) {
      // lit string value
      result = JSON.parse(paramVal);
    } else if (paramType === "VALUE" && typeof paramVal === "string") {
      // string or comma seperated list
      result = paramVal.split(",").map((d) => {
        // Convert comma-separated value to array
        if (!isNaN(Number(d))) {
          return Number(d); // number values
        } else if (d === "null") {
          return null; // null value
        } else if (d.match(litRegex)) {
          return JSON.parse(d); // quoted string values
        } else {
          return d;
        } // other
      });
      if (!Array.isArray(result) || result.length === 1) {
        result = result[0]; // single value
      } else {
        result = result;
      } // array
    } else if (paramType === "EXPRESSION") {
      result = nodeParamIn.fn(data);
    }
    return result;
  };
}

// Function to parse Vega references to a value
export function parseVegaRef(specIn, refIn) {
  let spec = structuredClone(specIn);
  let ref = structuredClone(refIn);
  let value;
  if (ref === "width") value = [0, spec.width];
  else if (ref === "height") value = [0, spec.height];
  else if (ref === "padding") value = spec.padding;
  else if (ref.data) value = spec.data.find((d) => d.name === ref.data).values.map((d) => d[ref.field]);
  else if (ref.value) value = ref.value;
  else value = ref;
  return value;
}

// Function to parse NodeBox parameter to Vega references.
// TODO:: create proper method to check value or expression reference or expression result of input
export function parse2vegaRef(spec, dataName, attrName, nodeParamIn) {
  // attrName is only used if nodeParamIn is a combined expression, not a single attribute or scale reference.
  // Cases: undefined / value number string
  const data = dataName ? spec.data.find((d) => d.name == dataName).values : undefined;
  const dataTransformAs = dataName ? spec.data.find((d) => d.name == dataName)?.transform?.map((d) => d.as) || [] : [];
  const dataKeys = dataName ? (data.length > 0 ? Object.keys(data[0]) : []) : [];
  const scaleNames = spec.scales?.map((d) => d.name);
  const paramVal = nodeParamIn.value;
  const attr = attrName
    ? attrName
    : scaleDefaults.find((d) => d.scaleName === paramVal)?.scaleAttr || "__" + paramVal + "_attr"; // default scaleAttribute added to data
  let result;
  const objRegex = new RegExp("^\\{.*\\}$");
  const arrRegex = new RegExp("^\\[.*\\]$");
  const litRegex = new RegExp('^".*"$');
  const paramType = nodeParamIn.node.values[nodeParamIn.name]
    ? nodeParamIn.node.values[nodeParamIn.name].type
    : undefined;

  if (!paramVal) {
    // undefined
    result = undefined;
  } else if (paramType === "VALUE" && typeof paramVal === "number") {
    // number value
    result = { value: paramVal };
  } else if (paramType === "VALUE" && paramVal.match(objRegex)) {
    // object JSON value
    result = JSON.parse(paramVal);
  } else if (paramType === "VALUE" && paramVal.match(arrRegex)) {
    // array JSON value
    result = JSON.parse(paramVal);
  } else if (paramType === "VALUE" && paramVal.match(litRegex)) {
    // lit string value
    result = { value: JSON.parse(paramVal) };
  } else if (paramType === "VALUE") {
    // number, string, litteral value
    const regex = new RegExp('^".*"$');
    result = paramVal.split(",").map((d) => (!isNaN(Number(d)) ? Number(d) : d.match(regex) ? JSON.parse(d) : d)); // Convert comma-separated value to array
    if (!Array.isArray(result) || result.length === 1) result = { value: result[0] };
  } else if (paramType === "EXPRESSION" && paramVal.match(/"/)) {
    let regex = /(?<!["'])\b[a-zA-Z_][^\s"']*\b(?!["'])/g;
    // Replace matched parts with prefixed "datum."
    result = paramVal.replace(regex, (match) => "datum." + match);
    result = { signal: result };
  } else if (paramType === "EXPRESSION" && (dataKeys.includes(attr) || dataTransformAs.includes(attr))) {
    // Expression existing attribute name
    result = { data: dataName, field: attr };
  } else if (paramType === "EXPRESSION" && scaleNames.includes(attr)) {
    // Expression existing scale name
    result = { scale: paramVal, field: attr };
  } else if (paramType === "EXPRESSION" && !dataKeys.includes(attr) && !dataTransformAs.includes(attr)) {
    // Expression other than existing attribute name
    addParamAttr(spec, dataName, attr, nodeParamIn);
    result = { data: dataName, field: attr }; // New attribute name
  }
  return result;
}

// Function to add node input parameter values to an attribute of the spec data.
export function addParamAttr(spec, dataName, attrName, paramIn, asTransform = false) {
  if (paramIn === undefined) return;

  const attrList = Array.isArray(attrName) ? attrName : [attrName];
  const paramList = Array.isArray(paramIn) ? paramIn : [paramIn];

  if (asTransform) {
    // Case: add attribute as a transform at the beginning
    paramList.forEach((p, j) => {
      const attr = attrList[j];
      let transformExpr;
      // Check if p is a literal value or node parameter
      if (typeof p === "object" && "fn" in p) {
        /*// If it's a node parameter, use the fn2 function to process it
        const fn = fn2(p);*/
        // Regular expression to match parts starting with a letter, followed by alphanumeric characters
        //let regex = /(?<!["'])\b[a-zA-Z_][a-zA-Z0-9_.]*\b(?!["'])/g;
        let regex = /(?<!["'])\b[a-zA-Z_][^\s"']*\b(?!["'])/g;
        // Replace matched parts with prefixed "datum."
        transformExpr = p.value.replace(regex, (match) => "datum." + match);
        transformExpr = transformExpr.toString(); // Convert the expression to a string expression
      } else {
        // If it's a literal value, create a direct expression
        transformExpr = JSON.stringify(p); // Convert literal to a JSON string for use in a transform
      }

      // Create the transform object
      setPlotDataTransform({
        spec,
        dataName,
        transformType: "formula",
        params: { expr: transformExpr, as: attr },
        index: -1,
      });
      /*const transform = {
        type: "formula",
        expr: transformExpr,
        as: attr,
      };

      // Use addPlotDataTransform to add the transform at the beginning (index = 0)
      addPlotDataTransform(spec, dataName, transform, 0);*/
    });
  } else {
    // Case: modify the data directly
    let data = spec.data.find((d) => d.name === dataName).values;
    data.forEach((d, i) => {
      paramList.forEach((p, j) => {
        const attr = attrList[j];

        // Check if p is a literal value or node parameter
        let value;
        if (typeof p === "object" && "fn" in p) {
          // If it's a node parameter, use the fn2 function to process it
          const fn = fn2(p);
          value = fn(d);
        } else {
          // If it's a literal value, use it directly
          value = p;
        }

        // Handle array values or single values for the attribute
        if (Array.isArray(value)) {
          d[attr] = i < value.length ? value[i] : null; // Use value cycling or null if out of bounds
        } else {
          d[attr] = value;
        }
      });
    });
    addPlotData(spec, data, dataName);
  }
}

/*
-------- HELPER FUNCTIONS FOR VEGA SPECS --------
*/

export function applyVegaTransform({ specData, dataName, transform }) {
  if (!Array.isArray(specData) || !dataName) {
    throw new Error("Invalid arguments. Provide specData (array) and dataName (string).");
  }

  // Find the target dataset by name
  const targetDataset = specData.find((d) => d.name === dataName);
  if (!targetDataset || !targetDataset.values) {
    throw new Error(`Dataset with name '${dataName}' not found or has no values.`);
  }

  // Extract relevant properties
  const values = targetDataset.values;
  const format = targetDataset.format;

  // Prepare the data to transform based on the format
  let targetData;
  if (!format || format.type === "json") {
    targetData = Array.isArray(values) ? values : [];
  } else if (format.type === "geojson") {
    if (!values.features || !Array.isArray(values.features)) {
      throw new Error("Invalid GeoJSON format. 'features' array is missing.");
    }
    targetData = values.features.map((f) => f.properties);
  } else if (format.type === "topojson") {
    if (!values.objects || !values.objects[format.feature] || !values.objects[format.feature].geometries) {
      throw new Error(`Feature '${format.feature}' not found in TopoJSON data.`);
    }
    targetData = values.objects[format.feature].geometries; //.map((g) => g.properties);
  } else {
    throw new Error(`Unsupported format: ${format.type}`);
  }

  // If no specific transform is provided, process all transforms
  if (!transform) {
    if (!targetDataset.transform || !Array.isArray(targetDataset.transform)) {
      //throw new Error(`No transforms found for dataset '${dataName}'.`);
      return specData;
    }

    while (targetDataset.transform.length > 0) {
      const currentTransform = targetDataset.transform.shift();
      targetData = applySingleTransform(specData, targetData, currentTransform);
    }
  } else {
    // Apply a single transform
    targetData = applySingleTransform(specData, targetData, transform);
  }

  // Update the dataset with transformed data
  if (format?.type === "geojson") {
    values.features.forEach((f, i) => {
      f.properties = targetData[i];
    });
  } else if (format?.type === "topojson") {
    values.objects[format.feature].geometries = targetData;
    /*values.objects[format.feature].geometries.forEach((g, i) => {
      g = targetData[i]; //g.properties = targetData[i]
    });*/
  } else {
    targetDataset.values = targetData;
  }

  return specData;
}

export function applySingleTransform(specData, data, transform) {
  switch (transform.type) {
    case "formula":
      return applyFormulaTransform(data, transform);

    case "lookup":
      return applyLookupTransform(specData, data, transform);

    case "collect":
      return applyCollectTransform(data, transform);

    case "filter":
      return applyFilterTransform(data, transform);

    case "geopoint":
      return data;

    default:
      throw new Error(`Unsupported transform type: ${transform.type}`);
  }
}

export function applyFormulaTransform_old(data, transform) {
  if (!transform.as || !transform.expr) {
    throw new Error("'formula' transform requires 'as' and 'expr' properties.");
  }

  return data.map((d) => {
    const newItem = { ...d };
    try {
      newItem[transform.as] = eval(transform.expr);
    } catch (err) {
      console.error(`Error evaluating formula: ${err.message}`);
      newItem[transform.as] = null;
    }
    return newItem;
  });
}

export function applyFormulaTransform_old2(data, transform) {
  if (!transform.as || !transform.expr) {
    throw new Error("'formula' transform requires 'as' and 'expr' properties.");
  }
  // Parse the Vega expression into an executable function
  const parsedExpression = parseExpression(transform.expr).code;
  const evaluateExpression = new Function("datum", `"use strict";\nreturn (${parsedExpression});`);

  return data.map((d) => {
    const newItem = { ...d };
    try {
      // Evaluate the Vega expression with the current datum
      newItem[transform.as] = evaluateExpression(d);
    } catch (err) {
      console.error(`Error evaluating formula: ${err.message}`);
      newItem[transform.as] = null;
    }
    return newItem;
  });
}
export function applyFormulaTransform(data, transform) {
  if (!transform.as || !transform.expr) {
    throw new Error("'formula' transform requires 'as' and 'expr' properties.");
  }
  // Create a function to evaluate the expression
  const evaluateExpression = new Function("datum", `"use strict";\nreturn (${transform.expr});`);

  return data.map((d) => {
    const newItem = { ...d };
    try {
      // Evaluate the Vega expression with the current datum
      newItem[transform.as] = evaluateExpression(d);
    } catch (err) {
      console.error(`Error evaluating formula: ${err.message}`);
      newItem[transform.as] = null;
    }
    return newItem;
  });
}

export function applyLookupTransform(specData, data, transform) {
  if (!transform.from || !transform.key || !transform.fields || !transform.values) {
    throw new Error("'lookup' transform requires 'from', 'key', 'fields', and 'values' properties.");
  }
  // Find the source dataset
  const sourceDataset = specData.find((d) => d.name === transform.from);
  if (!sourceDataset || !sourceDataset.values) {
    throw new Error(`Source dataset '${transform.from}' not found or has no values.`);
  }
  const sourceData = sourceDataset.values;

  // Build a lookup map
  const lookupMap = {};
  sourceData.forEach((item) => {
    lookupMap[item[transform.key]] = item;
  });
  return data.map((d) => {
    const newItem = { ...d };
    transform.fields.forEach((field, index) => {
      const lookupValue = lookupMap[getNestedProperty(newItem, field)];
      if (lookupValue) {
        transform.values.forEach((valueField, i) => {
          const newAttr = transform.as ? transform.as[i] : valueField;
          newItem[newAttr] = lookupValue[valueField];
        });
      }
    });
    return newItem;
  });
}

export function applyCollectTransform(data, transform) {
  if (!transform.sort || !Array.isArray(transform.sort.field)) {
    throw new Error("'collect' transform requires a 'sort' property with a 'field' array.");
  }

  const { field, order = "ascending" } = transform.sort;

  return [...data].sort((a, b) => {
    for (let i = 0; i < field.length; i++) {
      const key = field[i];
      const dir = Array.isArray(order) ? order[i] : order;
      const aValue = a[key];
      const bValue = b[key];

      if (aValue !== bValue) {
        if (dir === "ascending") {
          return aValue > bValue ? 1 : -1;
        } else {
          return aValue < bValue ? 1 : -1;
        }
      }
    }
    return 0;
  });
}

export function applyFilterTransform_old(data, transform) {
  if (!transform.expr) {
    throw new Error("'filter' transform requires an 'expr' property.");
  }
  const parsedExpression = parseExpression(transform.expr).code;
  const evaluateExpression = new Function("datum", `"use strict";\nreturn (${parsedExpression});`);

  return data.filter((d) => {
    try {
      return evaluateExpression(d);
    } catch (err) {
      console.error(`Error evaluating filter expression: ${err.message}`);
      return false;
    }
  });
}
export function applyFilterTransform(data, transform) {
  if (!transform.expr) {
    throw new Error("'filter' transform requires an 'expr' property.");
  }

  // Create a function to evaluate the expression
  const evaluateExpression = new Function("datum", `"use strict";\nreturn (${transform.expr});`);
  // Filter the data
  return data.filter((d) => {
    try {
      const result = evaluateExpression(d);
      return result; // Include only rows where the expression evaluates to true
    } catch (err) {
      console.error(`Error evaluating filter expression: ${err.message}`);
      return false; // Exclude rows with evaluation errors
    }
  });
}

/*
-------- HELPER FUNCTIONS FOR NESTED DATA --------
*/

// Function to filter out undefined object properties
export function deleteUndefined(object) {
  if (Array.isArray(object)) object.forEach(deleteUndefined);
  if (object)
    Object.keys(object).forEach((key) => {
      if (object[key] === undefined) delete object[key];
      else if (typeof object[key] === "object") deleteUndefined(object[key]);
    });
}

// Function to filter out undefined object properties
export function deleteNull(object) {
  if (Array.isArray(object)) object.forEach(deleteUndefined);
  if (object)
    Object.keys(object).forEach((key) => {
      if (object[key] === null) delete object[key];
      else if (typeof object[key] === "object") deleteNull(object[key]);
    });
}

// Function to merge nested objects
export function deepAssign(target, ...sources) {
  sources.forEach((source) => {
    if (source && typeof source === "object") {
      Object.keys(source).forEach((key) => {
        if (source[key] && typeof source[key] === "object") {
          if (!target[key] || typeof target[key] !== "object") {
            target[key] = Array.isArray(source[key]) ? [] : {};
          }
          deepAssign(target[key], source[key]);
        } else {
          target[key] = source[key];
        }
      });
    }
  });
  return target;
}

// Function to clean nested vega objects
export function deepCleanVega(spec) {
  //spec.forEach((obj) => {
  let obj = spec;
  if (obj && typeof obj === "object") {
    const keys = Object.keys(obj);
    if (keys[0] === "value" && keys.includes("field")) {
      delete obj.value;
    } else if (keys[keys.length - 1] === "value" && keys.includes("field")) {
      delete obj.field;
      delete obj.scale;
    }
    if (keys.includes("scale") && keys.includes("data")) {
      delete obj.data;
    }
    keys.forEach((key) => {
      if (obj[key] && typeof obj[key] === "object") {
        deepCleanVega(obj[key]);
      }
    });
  }
  //});
  return spec;
}

// Helper function to get nested properties using a string path.
export function getNestedProperty(obj, path) {
  if (!obj || typeof obj !== "object") return undefined;

  if (obj.hasOwnProperty(path)) {
    return obj[path];
  }

  // Split the path into keys
  const keys = path.split(".");

  // Traverse the object step by step
  let current = obj;
  for (let key of keys) {
    if (current[key] === undefined) {
      return undefined; // Return undefined if any part of the path is missing
    }
    current = current[key];
  }

  return current;
}

// Helper function to get properties from nested file formats
export function getAttributeValues({ data, format = "json", feature }) {
  if (!data) {
    throw new Error("Data must be provided.");
  }
  let dataVals = structuredClone(data);

  if (format === "json") {
    // Return the input array of objects as is for JSON format
    if (Array.isArray(dataVals)) {
      return dataVals;
    } else {
      throw new Error("For JSON format, data must be an array of objects.");
    }
  } else if (format === "geojson") {
    // Extract and return properties from GeoJSON features
    if (dataVals.features && Array.isArray(dataVals.features)) {
      return dataVals.features.map((f) => f.properties);
    } else {
      throw new Error("Invalid GeoJSON data. 'features' key is missing or invalid.");
    }
  } else if (format === "topojson") {
    // Extract and return properties from TopoJSON
    if (!feature) {
      throw new Error("For TopoJSON format, a feature name must be specified.");
    }
    if (dataVals.objects && dataVals.objects[feature] && dataVals.objects[feature].geometries) {
      return dataVals.objects[feature].geometries; //.map((g) => g.properties || {});
    } else {
      throw new Error(`Feature '${feature}' not found in TopoJSON data.`);
    }
  } else {
    throw new Error(`Unsupported format: ${format}`);
  }
}

/*
-------- HELPER FUNCTIONS FOR AUTO SCALES --------
*/

// Helper function to determine the type of the domain
export function determineDomainType(values) {
  // Filter out undefined and null values
  const filteredVals = Array.isArray(values)
    ? values.filter((val) => val !== undefined && val !== null)
    : values !== undefined && values !== null
      ? [values]
      : [];

  if (filteredVals.every((val) => typeof val === "number")) {
    return "numerical";
  } else if (filteredVals.every((val) => Object.prototype.toString.call(val) === "[object Date]")) {
    return "temporal";
  } else if (filteredVals.every((val) => typeof val === "string")) {
    return "categorical";
  } else {
    return "numerical"; // Default to numerical if mixed types
  }
}

// Helper function to determine the type of the range
export function determineRangeType(property) {
  const propertyTypes = {
    numerical: ["x", "y", "size", "strokeWidth", "opacity", "fillOpacity", "strokeOpacity"],
    categorical: ["shape", "fill", "stroke", "strokeDash", "gridUnits"],
  };
  if (propertyTypes.numerical.includes(property)) {
    return "numerical";
  } else if (propertyTypes.categorical.includes(property)) {
    return "categorical";
    /*} else if (values.every(val => typeof val === 'string' && /^#[0-9A-F]{6}$/i.test(val))) {
          return "color";*/
  } else {
    throw new Error("Unable to determine range type");
  }
}

// Function to find the best scale type from the mapping
export function findScaleType(domainType, rangeType, plotType) {
  const match = scaleTypeMapping.find(
    (mapping) =>
      mapping.domainType === domainType &&
      mapping.rangeType === rangeType &&
      (mapping.plotType === plotType || mapping.plotType === "default"),
  );
  if (match) {
    return match.scaleType;
  } else {
    return "linear";
    //throw new Error("No matching scale type found");
  }
}

// Function to determine the optimal vega scale type.
export function selectVegaScaleType(domainValues, property, plotType) {
  // Determine domain and range types
  const domainType = determineDomainType(domainValues);
  const rangeType = determineRangeType(property);
  // Determine the best scale type based on input parameters
  return findScaleType(domainType, rangeType, plotType);
}

/*
-------- HELPER FUNCTIONS FOR D3 SCALES --------
*/

// Custom quantize function to fit range.
export function scaleQuantizeFit(domain, range) {
  let [d0, d1] = domain.map(Number); // Coerce domain elements to numbers
  const rangeSize = range.length; // Length of the range (e.g., 60)
  const binWidth = (d1 - d0) / rangeSize;
  const thresholds = d3range(d0, d1, binWidth).slice(1, -1);

  // Function to create a bin generator with current thresholds
  function createBinGenerator() {
    return bin().domain([d0, d1]).thresholds(thresholds);
  }

  // Internal bin generator using d3.bin()
  let binGenerator = createBinGenerator();

  // Internal quantize function
  function quantizeFit(value) {
    value = +value; // Coerce input value to a number
    // Clamp value to range
    if (value <= d0) return range[0];
    if (value >= d1) return range[rangeSize - 1];
    // Generate bins for the input value and use the bin index
    const bins = binGenerator([value]);
    // `bins` will contain an array of bin thresholds, we find the correct bin
    const binIndex = bins.findIndex((bin) => bin.x0 <= value && value < bin.x1);

    // Return the corresponding range value
    return range[binIndex];
  }

  // Function to normalize values, bin them, and adjust the bins
  function fitValues(values) {
    // bin fit values
    const bins = binGenerator(values);

    // Get the bin counts
    let binCounts = bins.map((bin, i) => (bin.length ? i + 1 : null)).filter((d) => d != null);

    // Calculate total bin count
    let flooredSum = binCounts.reduce((acc, val) => acc + val, 0);
    let totalAdjustment = rangeSize - flooredSum; // Difference to adjust to match rangeSize
    // Adjust thresholds to balance the bin counts
    let fractionalParts = values.map((v) => v % binWidth);

    let fractionOrder = fractionalParts
      .map((part, index) => index)
      .sort((a, b) => fractionalParts[b] - fractionalParts[a]);

    let threshAdjust, fractionIndex, binIndex;

    for (let i = 0; i < Math.abs(totalAdjustment); i++) {
      if (totalAdjustment < 0) {
        // Add smallest fractional parts to resp. threshold (to fit in previous bin)
        fractionIndex = fractionOrder[fractionalParts.length - 1 - i];
        binIndex = Math.floor(values[fractionIndex] / binWidth);
        threshAdjust = fractionalParts[fractionIndex];
        thresholds[binIndex - 1] += threshAdjust + 1;
      } else if (totalAdjustment > 0) {
        // Subtract largest fractional parts from resp. threshold (to fit in next bin)
        fractionIndex = fractionOrder[i];
        binIndex = Math.floor(values[fractionIndex] / binWidth);
        threshAdjust = fractionalParts[fractionIndex];
        thresholds[binIndex] -= binWidth - threshAdjust;
      }
      binGenerator = createBinGenerator();
    }
  }

  // Method to get/set domain
  quantizeFit.domain = function (newDomain) {
    if (!arguments.length) return domain;
    domain = newDomain.map(Number); // Coerce domain elements to numbers
    [d0, d1] = domain;
    return quantizeFit;
  };

  // Method to get/set range
  quantizeFit.range = function (newRange) {
    if (!arguments.length) return range;
    range = newRange; // Allow range to accept any values
    return quantizeFit;
  };

  // Method to fit the values and adjust bins
  quantizeFit.fit = function (values) {
    fitValues(values);
    return quantizeFit;
  };

  // Method to invert extent
  quantizeFit.invertExtent = function (y) {
    const i = range.indexOf(y);
    return i === -1 ? [NaN, NaN] : [d0 + (i / rangeSize) * (d1 - d0), d0 + ((i + 1) / rangeSize) * (d1 - d0)];
  };

  // Method to copy the scale
  quantizeFit.copy = function () {
    return scaleQuantizeFit(domain.slice(), range.slice());
  };

  return quantizeFit;
}

// Function to create D3 scale using Vega parameters
export function parseVegaScale(
  specIn,
  {
    name,
    type,
    domain,
    domainMax,
    domainMin,
    domainMid,
    domainRaw,
    interpolate,
    range,
    reverse,
    round,
    bins,
    clamp,
    padding,
    nice,
    zero,
    base,
    exponent,
    constant,
    align,
    domainImplicit,
    paddingInner,
    paddingOuter,
  },
) {
  const spec = structuredClone(specIn);
  let scaleFncs = {
    linear: scaleLinear,
    log: scaleLog,
    pow: scalePow,
    sqrt: scaleSqrt,
    symlog: scaleSymlog,
    time: scaleTime,
    utc: scaleUtc,
    ordinal: scaleOrdinal,
    band: scaleBand,
    point: scalePoint,
    diverging: scaleDiverging,
    quantile: scaleQuantile,
    quantize: scaleQuantize,
    threshold: scaleThreshold,
    quantizeFit: scaleQuantizeFit,
  };
  let scale = scaleFncs[type](parseVegaRef(spec, domain), parseVegaRef(spec, range));
  if (domainMin && domainMax) scale.domain([parseVegaRef(spec, domainMin), parseVegaRef(spec, domainMax)]);
  if (domainRaw) scale.domain(parseVegaRef(spec, domainRaw));
  if (interpolate) scale.interpolate(parseVegaRef(spec, interpolate));
  if (reverse) scale.range(parseVegaRef(spec, range).reverse());
  if (round) scale.round(parseVegaRef(spec, round));
  if (["linear", "log", "pow", "sqrt", "symlog", "time", "utc"].includes(type)) {
    if (clamp) scale.clamp(parseVegaRef(spec, clamp));
    if (nice) scale.nice();
  }
  if (bins) scale.bins(bins);
  if (base) scale.base(base);
  if (exponent) scale.exponent(exponent);
  if (constant) scale.constant(constant);

  if (align && ["band", "point"].includes(type)) scale.align(align);
  if (padding && ["band", "point"].includes(type)) scale.padding(padding);
  if (paddingInner && type === "band") scale.paddingInner(paddingInner);
  if (paddingOuter && type === "band") scale.paddingOuter(paddingOuter);

  return scale;
}

/*
-------- DATA HANDLING FUNCTIONS --------
*/
// Enhanced function to handle single or multiple attribute grouping
export function sumRollup(data, groupBy, sumAttr, keep) {
  // Determine the key function based on whether groupBy is a single string or an array of strings
  const keyFunction = Array.isArray(groupBy)
    ? (d) => groupBy.map((attr) => d[attr]).join("|") // Join multiple attributes with a separator to form a composite key
    : (d) => d[groupBy]; // Single attribute grouping
  // Perform the rollup using d3.rollup with the dynamic key function
  let rolledUpData = structuredClone(data);
  rolledUpData = rollup(
    rolledUpData,
    (v) => {
      const result = {};
      // Keep attribute(s) in the result
      if (Array.isArray(keep)) {
        keep.forEach((attr) => (result[attr] = v[0][attr]));
      } else {
        result[keep] = v[0][keep];
      }
      // Keep only the groupBy attribute(s) in the result
      if (Array.isArray(groupBy)) {
        groupBy.forEach((attr) => (result[attr] = v[0][attr]));
      } else {
        result[groupBy] = v[0][groupBy];
      }
      result[sumAttr] = sum(v, (d) => d[sumAttr]); // Aggregate sum of the specified attribute
      return result;
    },
    keyFunction,
  );

  // Convert the Map result to an array of objects
  const resultArray = Array.from(rolledUpData, ([key, value]) => ({
    ...value,
  }));

  return resultArray;
}

/*
-------- HELPER FUNCTIONS FOR GRID PLOTS --------
*/

// Function to calculate grid size of waffle plot
export function calculateGridSize({ data, nCols, nRows, groupBy, offset, direction, gridProp }) {
  //let nDataRecords = data.length;
  let nUnits = data.map((d) => d[gridProp]).reduce((acc, curr) => acc + curr, 0);
  let nCols_calc = structuredClone(nCols);
  let nRows_calc = structuredClone(nRows);
  // Calculate the grid size based on direction and provided dimensions
  if (!nCols && !nRows) {
    nCols_calc = Math.ceil(Math.sqrt(nUnits));
    nRows_calc = Math.ceil(nUnits / nCols_calc);
  } else if (nCols && !nRows) {
    nRows_calc = Math.ceil(nUnits / nCol_calc);
  } else if (nRows && !nCols) {
    nCols_calc = Math.ceil(nUnits / nRows_calc);
  } /*else {
    if (direction === 'X' && nCols * nRows < nDataRecords) {
      nRows = Math.ceil(nDataRecords / nCols);
    } else if (direction === 'Y' && nCols * nRows < nDataRecords) {
      nCols = Math.ceil(nDataRecords / nRows);
    }
  }*/

  return [nCols_calc, nRows_calc, nUnits];
}

// Function to add grid position coördinates
// offset normalize converts absolute values into relative values
export function addGridData({
  spec,
  dataName,
  nCols,
  nRows,
  gridScaleName,
  groupBy,
  unitRatio = 1,
  offset,
  direction,
  sortAttr,
  order,
}) {
  // Get spec data
  let data = spec.data.find((d) => d.name === dataName);
  if (!data || !data.values) return; // Return if no data found
  const gridDataName = "gridTable";
  const gridProp = "__gridUnits";

  // Add gridUnit attribute to data
  let fn;
  if (typeof unitRatio === "object" && "fn" in unitRatio) {
    // If it's a node parameter, use the fn2 function to process it
    fn = fn2(unitRatio);
  } else {
    // If it's a literal value, use it directly
    fn = (d) => {
      return unitRatio;
    };
  }
  data.values.forEach((d, i) => {
    d[gridProp] = fn(d);
  });
  addPlotData(spec, data.values, dataName);

  let dataValues = structuredClone(data.values);
  //let nDataRecords = dataValues.length;
  let nCols_calc, nRows_calc, nUnits;

  // Sort data values if sorting is specified
  if (sortAttr) {
    let sortFn;
    if (order === "ascending" || order === undefined) {
      sortFn = (a, b) => ascending(a[sortAttr], b[sortAttr]);
    } else {
      sortFn = (a, b) => descending(a[sortAttr], b[sortAttr]);
    }
    dataValues.sort(sortFn);
  }
  // Calculate grid size
  [nCols_calc, nRows_calc, nUnits] = calculateGridSize({
    data: dataValues,
    nCols: nCols,
    nRows: nRows,
    groupBy: groupBy,
    offset: offset,
    direction: direction,
    gridProp: gridProp,
  });
  const totalCells = nCols_calc * nRows_calc;

  // Calculate grid domain
  const propKeys = Object.keys(dataValues[0]).filter((key) => key.startsWith("__"));
  //const rollupKeys = Array.isArray(groupBy) ? [...groupBy, ...propKeys] : [groupBy, ...propKeys];
  const groupedData = groupBy ? sumRollup(dataValues, groupBy, gridProp, propKeys) : dataValues;
  const domainTotal = sum(groupedData.map((d) => d[gridProp]));
  // Adjust domain based on user-defined unitRatio
  const gridDomainMin = 0;
  const gridDomainMax = domainTotal; // Scale the domain based on the unitRatio
  const gridDomain = [gridDomainMin, gridDomainMax];

  // Calculate range
  let gridRange = d3range(1, Math.ceil(gridDomainMax) + 1, 1);
  if (offset === "normalize") {
    gridRange = d3range(1, totalCells + 1, 1);
  }
  // Create and parse gridScale to calculate unit number.
  let gridScale = {
    name: "gridUnitScale",
    type: "quantizeFit",
    domain: gridDomain,
    range: gridRange,
  };

  let gridD3Scale = parseVegaScale(spec, gridScale);
  const fitData = groupedData.map((d) => d[gridProp]);

  // fit scale and count total
  let totalScaled;
  if (offset === "normalize") {
    gridD3Scale.fit(fitData);
    totalScaled = fitData.map((d) => gridD3Scale(d)).reduce((acc, curr) => acc + curr);
  } else if (typeof unitRatio === "object" && "fn" in unitRatio) {
    const paramType = unitRatio.node.values[unitRatio.name] ? unitRatio.node.values[unitRatio.name].type : undefined;
    if (paramType === "EXPRESSION") {
      gridD3Scale.fit(fitData);
      totalScaled = fitData.map((d) => gridD3Scale(d)).reduce((acc, curr) => acc + curr);
    } else {
      totalScaled = fitData.reduce((acc, curr) => acc + curr);
    }
  } else {
    totalScaled = fitData.reduce((acc, curr) => acc + curr);
  }

  // Create grid data
  let gridData = [];
  let cellIdx = 0;
  groupedData.forEach((elem) => {
    // for each record
    // set unitCount
    let unitCnt;
    if (offset === "normalize") {
      unitCnt = gridD3Scale(elem[gridProp]);
    } else if (typeof unitRatio === "object" && "fn" in unitRatio) {
      const paramType = unitRatio.node.values[unitRatio.name] ? unitRatio.node.values[unitRatio.name].type : undefined;
      if (paramType === "EXPRESSION") {
        unitCnt = gridD3Scale(elem[gridProp]);
      } else {
        unitCnt = elem[gridProp];
      }
    } else {
      unitCnt = elem[gridProp];
    }

    for (let i = 0; i < unitCnt && cellIdx < totalCells; i++) {
      // for each available unit
      if (cellIdx === totalCells) {
        break;
      } else {
        // Calculate __x and __y based on the direction
        const newElem = structuredClone(elem);
        newElem.__x = direction === "X" ? Math.floor(cellIdx / nRows) : cellIdx % nCols;
        newElem.__y = direction === "X" ? cellIdx % nRows : Math.floor(cellIdx / nCols);
        newElem.__plot = true;

        // Apply center offset adjustments if needed
        if (offset === "center") {
          if (direction === "Y" && newElem.__y === Math.ceil(totalScaled / nCols_calc) - 1) {
            if (nCols_calc - (totalScaled % nCols_calc) !== nCols_calc) {
              newElem.__x += Math.floor((nCols_calc - (totalScaled % nCols_calc)) / 2);
            }
          } else if (direction === "X" && newElem.__x === Math.ceil(totalScaled / nRows_calc) - 1) {
            if (nRows_calc - (totalScaled % nRows_calc) !== nRows_calc) {
              newElem.__y += Math.floor((nRows_calc - (totalScaled % nRows_calc)) / 2);
            }
          }
        }
        gridData.push(newElem);
        cellIdx++;
      }
    }
  });

  addPlotData(spec, gridData, gridDataName);

  // Copy transforms from the original data spec to the gridTable spec
  let originalTransforms = data.transform ? structuredClone(data.transform) : [];
  spec.data.find((d) => d.name === gridDataName).transform = originalTransforms;
}

/*
-------- HELPER FUNCTIONS FOR GEOGRAPHIC PLOTS --------
*/

// Reprojection functions
export function reprojectCoordinates(geoData, chosenCRS) {
  const targetProj = proj4Defs.EPSG4326;

  if (chosenCRS === "EPSG4326") {
    return geoData;
  }

  const sourceProj = proj4Defs[chosenCRS.replace(":", "")];

  if (!sourceProj) {
    throw new Error(`Unsupported CRS: ${chosenCRS}`);
    return geoData;
  }

  geoData.forEach((feature) => {
    if (feature.geometry && feature.geometry.coordinates) {
      feature.geometry.coordinates = transformCoordinates(feature.geometry.coordinates, sourceProj, targetProj);
    }
  });

  return geoData;
}

// Function to transform coordinates used in reprojection function
export function transformCoordinates(coordinates, sourceProj, targetProj) {
  if (Array.isArray(coordinates[0])) {
    return coordinates.map((coord) => transformCoordinates(coord, sourceProj, targetProj));
  } else {
    return proj4(sourceProj, targetProj, coordinates);
  }
}

// Object to lookup d3 projections
export const d3geo = {
  geoAlbers: geoAlbers,
  geoAlbersUsa: geoAlbersUsa,
  geoAzimuthalEqualArea: geoAzimuthalEqualArea,
  geoAzimuthalEquidistant: geoAzimuthalEquidistant,
  geoConicConformal: geoConicConformal,
  geoConicEqualArea: geoConicEqualArea,
  geoConicEquidistant: geoConicEquidistant,
  geoEquirectangular: geoEquirectangular,
  geoGnomonic: geoGnomonic,
  geoMercator: geoMercator,
  geoOrthographic: geoOrthographic,
  geoStereographic: geoStereographic,
  geoTransverseMercator: geoTransverseMercator,
  geoNaturalEarth1: geoNaturalEarth1,
};

// Helper function to apply the projection to coordinates or its inverse
export function applyD3Projection(longitude, latitude, projection, invert = false) {
  // Helper function to capitalize projection type for D3 compatibility
  function capitalize(string) {
    return string.charAt(0).toUpperCase() + string.slice(1);
  }

  // Use the D3 projection engine
  const d3Projection = d3geo[`geo${capitalize(projection.type)}`]();
  if (projection.center && d3Projection.center) d3Projection.center(projection.center);
  if (projection.scale && d3Projection.scale) d3Projection.scale(projection.scale);
  if (projection.translate && d3Projection.translate) d3Projection.translate(projection.translate);
  if (projection.rotate && d3Projection.rotate) d3Projection.rotate(projection.rotate);

  // Apply the projection or its inverse
  if (invert) {
    return d3Projection.invert([longitude, latitude]);
  } else {
    return d3Projection([longitude, latitude]);
  }
}

// Function to calculate the geographic extent of the map
export function calculateMapExtent(projection, width, height) {
  // Plot figure corners
  const topLeft = [0, 0]; // Top-left corner of the map
  const bottomRight = [width, height]; // Bottom-right corner of the map

  // Get geographic coordinates for the corners
  const topLeftGeo = applyD3Projection(topLeft[0], topLeft[1], projection, true);
  const bottomRightGeo = applyD3Projection(bottomRight[0], bottomRight[1], projection, true);

  // Return the geographic extent
  return {
    minLongitude: topLeftGeo[0],
    maxLongitude: bottomRightGeo[0],
    minLatitude: bottomRightGeo[1],
    maxLatitude: topLeftGeo[1],
  };
}

// Function to detect or validate data format (json, geojson, topojson)
export function detectDataFormat(data) {
  if (!data) return undefined;

  // Extract single dataObject for geojson and json
  let dataObj;
  if (Array.isArray(data)) {
    if (data.length === 1) {
      dataObj = data[0];
    } else {
      return "json";
    }
  } else {
    dataObj = data;
  }

  // GeoJSON Member types
  const geojsonTypes = [
    "Point",
    "MultiPoint",
    "LineString",
    "MultiLineString",
    "Polygon",
    "MultiPolygon",
    "GeometryCollection",
    "Feature",
    "FeatureCollection",
  ];

  // TopoJSON Member types
  const topojsonGeometryTypes = [
    "Point",
    "MultiPoint",
    "LineString",
    "MultiLineString",
    "Polygon",
    "MultiPolygon",
    "GeometryCollection",
  ];

  // Check data format
  if (dataObj && "type" in dataObj) {
    if (geojsonTypes.includes(type)) {
      // validate geojson
      switch (type) {
        case "Point":
        case "MultiPoint":
          if ("coordinates" in dataObj && Array.isArray(dataObj.coordinates)) {
            return `geojson ${type.toLowerCase()}`;
          }
          break;
        case "LineString":
        case "MultiLineString":
        case "Polygon":
        case "MultiPolygon":
          if ("coordinates" in dataObj && Array.isArray(dataObj.coordinates)) {
            return `geojson ${type.toLowerCase()}`;
          } else if ("arcs" in dataObj && Array.isArray(dataObj.arcs)) {
            return `topojson ${type.toLowerCase()}`;
          }
          break;

        case "GeometryCollection":
          if ("geometries" in dataObj && Array.isArray(dataObj.geometries)) {
            if (dataObj.geometries.every((geometry) => detectDataFormat(geometry).startsWith("geojson"))) {
              return "geojson geometrycollection";
            } else if (dataObj.geometries.some((geometry) => detectDataFormat(geometry).startsWith("topojson"))) {
              return "topojson geometrycollection";
            }
          }
          break;

        case "Feature":
          if (
            "geometry" in dataObj &&
            "properties" in dataObj &&
            (dataObj.geometry === null || detectDataFormat(dataObj.geometry).startsWith("geojson"))
          ) {
            return "geojson feature";
          }
          break;

        case "FeatureCollection":
          if (
            "features" in dataObj &&
            Array.isArray(dataObj.features) &&
            dataObj.features.every((feature) => detectDataFormat(feature) === "geojson feature")
          ) {
            return "geojson featurecollection";
          }
          break;

        default:
          break;
      }
    } else if (type === "Topology") {
      // validate topojson
      if ("objects" in dataObj && "arcs" in dataObj) {
        if (typeof dataObj.objects === "object" && Array.isArray(dataObj.arcs)) {
          // Validate objects in the topology
          for (const key in dataObj.objects) {
            const geometryObj = dataObj.objects[key];
            if (topojsonGeometryTypes.includes(geometryObj.type)) {
              const format = detectDataFormat(geometryObj);
              if (!format.startsWith("geojson") && !format.startsWith("topojson")) {
                console.warn(`Invalid GeoJSON member in TopoJSON object: ${key}`);
              } else {
                return "topojson topology";
              }
            }
          }
        } else {
          console.warn("Invalid TopoJSON topology.");
        }
      }

      /*&&
        (!("transform" in dataObj) ||
          (
            "scale" in dataObj.transform &&
            Array.isArray(dataObj.transform.scale) &&
            dataObj.transform.scale.length === 2 &&
            "translate" in dataObj.transform &&
            Array.isArray(dataObj.transform.translate) &&
            dataObj.transform.translate.length === 2
          ))*/
    }
  }
  return "json"; // Default to JSON if no valid format is detected
}

// Function to get coordinate values
export function extractCoordinates(geometry) {
  if (!geometry || !geometry.type || !geometry.coordinates) {
    console.warn("Invalid geometry data. Returning empty array.");
    return [];
  }

  switch (geometry.type) {
    case "Point":
      return [geometry.coordinates]; // Single [x, y]
    case "MultiPoint":
    case "LineString":
      return geometry.coordinates; // Array of [x, y]
    case "MultiLineString":
    case "Polygon":
      return geometry.coordinates.flat(); // Flatten nested arrays
    case "MultiPolygon":
      return geometry.coordinates.flat(2); // Deep flatten for MultiPolygon
    case "GeometryCollection":
      return geometry.geometries.flatMap((g) => extractCoordinates(g)); // Recursively process geometries
    default:
      console.warn(`Unsupported geometry type: ${geometry.type}`);
      return [];
  }
}

// Function to detect if geometry is (Multi)point, (Multi)LineString or (Multi)Polygon
export function detectGeometryTypes(data, feature = undefined, attribute = "geometry") {
  const geometryTypes = new Set();

  // Check if the dataset is GeoJSON
  if (data.type === "FeatureCollection") {
    data.features.forEach((feature) => {
      if (feature[attribute] && feature[attribute].type) {
        geometryTypes.add(feature[attribute].type);
      }
    });
  }
  // Check if the dataset is TopoJSON
  else if (data.type === "Topology") {
    // Use the specified feature or the first object in TopoJSON
    const selectedFeature = feature ? data.objects[feature] : Object.values(data.objects)[0];

    if (!selectedFeature) {
      console.error("Feature not found in TopoJSON data.");
      return [];
    }

    if (selectedFeature.type === "GeometryCollection") {
      selectedFeature.geometries.forEach((geometry) => {
        geometryTypes.add(geometry.type);
      });
    } else if (selectedFeature.type) {
      geometryTypes.add(selectedFeature.type);
    }
  }
  // Check if the dataset is a custom format
  else if (Array.isArray(data)) {
    data.forEach((item) => {
      if (item[attribute] && item[attribute].type) {
        geometryTypes.add(item[attribute].type);
      }
    });
  } else {
    console.error("Unsupported data format: Must be GeoJSON, TopoJSON, or a custom format.");
  }
  return Array.from(geometryTypes);
}

/*
-------- MARKS HELPER FUNCTIONS --------
*/

// A function to find the index or indexes of the mark(s) with the specified name.
export function findMark(spec, markName) {
  if (markName === undefined) {
    return -1;
  }
  function searchMarks(marks, path = []) {
    for (let index = 0; index < marks.length; index++) {
      const mark = marks[index];
      if (mark.name === markName) {
        return path.length ? [...path, index] : index;
      }
      if (mark.type === "group" && mark.marks) {
        const result = searchMarks(mark.marks, [...path, index]);
        if (result !== -1) {
          return result;
        }
      }
    }
    return -1;
  }
  return searchMarks(spec.marks);
}

// Helper function to create grouped marks
export function groupMark({ spec, dataName, groupName, groupBy, markName }) {
  // SET VARIABLES
  let group, groupAttr;
  let groupIdx = spec.marks.findIndex((d) => d.type === "group" && d?.from?.facet?.name === groupName);
  if (groupIdx !== -1) {
    // existing groupname
    group = groupName;
    groupAttr = spec.marks[groupIdx].from.facet.groupby;
  } else if (groupBy) {
    // groupBy attribute
    group = groupName || "series";
    groupAttr = "__groupBy" + "_" + group;
  } else {
    return;
  }

  // UPDATE GROUPBY ATTRIBUTE IN SPEC DATA
  // add groupAttr to data
  if (groupBy) {
    const dataIdx = spec.data.findIndex((d) => d.name === dataName);
    if (dataIdx >= 0) spec.data[dataIdx].values.forEach((d) => (d[groupAttr] = d[groupBy]));
  }
  // GROUP MARKS
  // inititate groupMarks
  let groupMarks;
  if (groupIdx !== -1) {
    // existing group
    groupMarks = spec.marks[groupIdx].marks;
  } else {
    // new group
    groupMarks = [];
  }

  // add new marks to group
  let marksToGroup, marksToKeep;
  if (markName) {
    // group only selected markNames
    markName = Array.isArray(markName) ? markName : [markName];
    marksToGroup = spec.marks.filter((d) => markName.includes(d.name));
    marksToKeep = spec.marks.filter((d) => !markName.includes(d.name));
  } else {
    // group all existing non group marks
    marksToGroup = spec.marks.filter((d) => d.type !== "group");
    marksToKeep = spec.marks.filter((d) => d.type === "group");
  }

  groupMarks.push(...marksToGroup);
  groupMarks.forEach((d) => (d.from.data = groupName));
  const marksGroup = {
    type: "group",
    from: {
      facet: {
        name: group,
        data: dataName,
        groupby: groupAttr,
      },
    },
    marks: groupMarks,
  };

  // create or update group
  groupIdx = marksToKeep.findIndex((d) => d.type === "group" && d?.from?.facet?.name === groupName);
  if (groupIdx !== -1) {
    // existing groupname
    Object.assign(marksToKeep[groupIdx], marksGroup);
  } else {
    marksToKeep.push(marksGroup);
  }
  spec.marks = marksToKeep;
}

// A helper function to list all marks
export function gatherMarks(marks) {
  let gatheredMarks = [];

  function traverseMarks(markArray) {
    markArray.forEach((mark) => {
      if (mark.type === "group" && mark.marks) {
        traverseMarks(mark.marks);
      } else {
        gatheredMarks.push(mark);
      }
    });
  }

  traverseMarks(marks);
  return gatheredMarks;
}

// Function to stack marks
export function stack(spec, dataName, markName, direction, offset, sortAttr, order) {
  const dirScaleName = direction === "Y" ? "yScale" : "xScale";
  const baseAttr = direction === "Y" ? "__x" : "__y";
  const dirAttr = direction === "Y" ? "__y" : "__x";

  const idx = findMark(spec, markName);
  let mark;
  // initiate mark
  if (idx !== -1) {
    // markName exists
    if (Array.isArray(idx)) {
      mark = structuredClone(spec.marks[idx[0]].marks[idx[1]]);
    } else {
      mark = structuredClone(spec.marks[idx]);
    }
  } else {
    mark = {};
  }

  let transforms = spec.data.find((d) => d.name == dataName).transform;
  transforms = transforms ? transforms : [];
  let stacktrans = {
    type: "stack",
    groupby: [baseAttr],
    field: dirAttr,
    as: ["__base0", "__base1"],
  };
  if (offset) stacktrans.offset = offset;
  if (sortAttr) {
    stacktrans.sort = { field: sortAttr, order: "ascending" };
    if (order) stacktrans.sort.order = order;
  }

  transforms.push(stacktrans);
  spec.data.find((d) => d.name == dataName).transform = transforms;
  mark.encode.update[direction === "Y" ? "y" : "x"] = { scale: dirScaleName, field: "__base0" };
  mark.encode.update[direction === "Y" ? "y2" : "x2"] = { scale: dirScaleName, field: "__base1" };

  // replace mark
  if (idx !== -1) {
    // markName exists
    if (Array.isArray(idx)) {
      spec.marks[idx[0]].marks[idx[1]] = mark;
    } else {
      spec.marks[idx] = mark;
    }
  } else {
    spec.marks.push(mark);
  }

  spec.scales.find((d) => d.name == dirScaleName).domain.field = "__base1";
  delete spec.scales.find((d) => d.name == dirScaleName).domainMin;
  delete spec.scales.find((d) => d.name == dirScaleName).domainMax;
}

// A helper function to determine Legend Shape
export function determineLegendShape(marks, scales) {
  const gatheredMarks = gatherMarks(marks);
  // Check for marks shapescale
  let shapeScale = null;
  gatheredMarks.forEach((mark) => {
    if (mark.encode.update.shape && mark.encode.update.shape.scale) {
      const shapeScaleName = mark.encode.update.shape.scale;
      if (scales.includes(shapeScaleName)) {
        shapeScale = shapeScaleName;
      }
    }
  });

  if (shapeScale) {
    return shapeScale;
  }

  // Check for dominant mark type
  const markTypeCounts = gatheredMarks.reduce((acc, mark) => {
    acc[mark.type] = (acc[mark.type] || 0) + 1;
    return acc;
  }, {});

  const dominantMarkType = Object.keys(markTypeCounts).reduce((a, b) =>
    markTypeCounts[a] > markTypeCounts[b] ? a : b,
  );

  switch (dominantMarkType) {
    case "rect":
      return "square";
    case "line":
    case "rule":
      return "stroke";
    case "symbol":
      const symbolMarks = gatheredMarks.filter((mark) => mark.type === "symbol");
      if (symbolMarks.length > 0 && symbolMarks[0].encode.update.shape) {
        return symbolMarks[0].encode.update.shape.value;
      }
      return "circle";
    default:
      return "circle";
  }
}

// Helper function to add a data transform to the vega plot specification
export function addPlotDataTransform(spec, dataName, transform, index = -1) {
  const dataIdx = spec.data.findIndex((d) => d.name === dataName); // check if dataName exists

  if (dataIdx !== -1) {
    // If data already exists, insert the transform at the specified index
    const transforms = spec.data[dataIdx].transform || [];
    const transformLength = transforms.length;

    // Handle the insertion based on index
    if (index === 0 || index < -transformLength) {
      // Insert at the beginning if index is 0 or negative and out of bounds
      transforms.unshift(transform);
    } else if (index === -1 || index >= transformLength) {
      // Add to the end if index is -1 or positive out of bounds
      transforms.push(transform);
    } else if (index < 0) {
      // Add at the position from the end (negative indexing)
      transforms.splice(transformLength + index + 1, 0, transform);
    } else {
      // Insert at the specified positive index within bounds
      transforms.splice(index, 0, transform);
    }

    // Update the data's transform array
    spec.data[dataIdx].transform = transforms;
  } else {
    // If data doesn't exist, create a new entry with the transform
    spec.data.push({
      name: dataName,
      transform: [transform],
    });
  }
}

// Function to add plot scale to spec
export function addPlotScale({ spec, scale }) {
  // Add or update grid scales in the spec
  const scaleName = scale.name;
  const existingScaleIndex = spec.scales.findIndex((s) => s.name === scaleName);
  if (existingScaleIndex !== -1) {
    spec.scales[existingScaleIndex] = scale;
  } else {
    spec.scales.push(scale);
  }
}

/*
-------- PLOT FUNCTIONS --------
*/

// Function to add plot data
export function addPlotData(spec, data, dataName, format, feature, index = -1) {
  const dataIdx = spec.data.findIndex((d) => d.name === dataName); // check if dataName exists
  const dataCnt = spec.data.length;
  let dataEntry;

  if (dataIdx >= 0) {
    // If the data already exists, update its values
    dataEntry = structuredClone(spec.data[dataIdx]);
    dataEntry.values = data;
  } else {
    // Create a new data entry with values
    dataEntry = { name: dataName, values: data };
  }

  // Add format and feature if specified
  if (format) {
    dataEntry.format = { type: format };
    if (feature) {
      dataEntry.format.feature = feature;
    }
  }

  // Handle existing data.
  if (dataIdx >= 0 && dataIdx !== index && index !== undefined) {
    // Remove data from original position if different index.
    spec.data.splice(dataIdx, 1); //Remove from original
  }

  // Handle the insertion based on index
  if (dataIdx >= 0 && (dataIdx === index || index === undefined)) {
    // Replace data at original position.
    spec.data[dataIdx] = dataEntry;
  } else if (index === 0 || index < -dataCnt) {
    // Insert at the beginning if index is 0 or negative and out of bounds
    spec.data.unshift(dataEntry);
  } else if (index === -1 || index >= dataCnt) {
    // Add to the end if index is -1 or positive out of bounds
    spec.data.push(dataEntry);
  } else if (index < 0) {
    // Add at the position from the end (negative indexing)
    spec.data.splice(dataCnt + index + 1, 0, dataEntry);
  } else {
    // Insert at the specified positive index within bounds
    spec.data.splice(index, 0, dataEntry);
  }
}

// Function to sort plot data
export function sortSpecData(spec, dataName, sortAttr, order) {
  const sortTrans = {
    type: "collect",
    sort: {
      field: sortAttr,
      order: order,
    },
  };
  const dataIdx = spec.data.findIndex((d) => d.name === dataName);
  if (dataIdx >= 0 && spec.data[dataIdx].transform) {
    spec.data[dataIdx].transform.push(sortTrans);
  } else if (dataIdx >= 0) {
    spec.data[dataIdx].transform = [sortTrans];
  }
}

// Function to add a data transform to the Vega plot specification
export function setPlotDataTransform({ spec, dataName, transformType, params, index = -1 }) {
  const transform = {};

  // Construct the transform based on type and parameters
  switch (transformType) {
    case "aggregate":
      transform.type = "aggregate";
      transform.ops = params.operations?.split(",").map((d) => d.trim());
      transform.fields =
        params.fields === "" || params.fields === undefined ? [] : params.fields?.split(",").map((d) => d.trim());
      transform.groupby = params.groupby?.split(",").map((d) => d.trim());
      break;

    case "filter":
      transform.type = "filter";
      transform.expr = params.expr;
      break;

    case "sort":
      transform.type = "collect";
      transform.sort = { field: params.field, order: params.order };
      break;

    case "bin":
      transform.type = "bin";
      transform.field = params.field;
      transform.as =
        params.as === "" || params.as === undefined ? undefined : params.as?.split(",").map((d) => d.trim());
      transform.maxbins = params.maxBins;
      transform.base = params.base;
      transform.step = params.step;
      transform.steps = params.steps?.split(",").map((s) => parseFloat(s));
      transform.extent = params.extent?.split(",").map((e) => parseFloat(e));
      break;

    case "collect":
      transform.type = "collect";
      transform.sort = { field: params.field, order: params.order };
      break;

    case "lookup":
      transform.type = "lookup";
      transform.from = params.from;
      transform.key = params.key;
      transform.fields =
        params.fields === "" || params.fields === undefined ? [] : params.fields?.split(",").map((d) => d.trim());
      transform.values =
        params.values === "" || params.values === undefined ? [] : params.values?.split(",").map((d) => d.trim());
      transform.as =
        params.as === "" || params.as === undefined ? undefined : params.as?.split(",").map((d) => d.trim());
      break;

    case "project":
      transform.type = "project";
      transform.fields =
        params.fields === "" || params.fields === undefined ? [] : params.fields?.split(",").map((d) => d.trim());
      break;

    case "formula":
      transform.type = "formula";
      transform.expr = params.expr;
      transform.as = params.as;
      break;

    case "graticule":
      transform.type = "graticule";
      transform.step = params.step?.split(",").map((s) => parseFloat(s));
      transform.extent = params.extent
        ?.split("|")
        .map((pair) => pair.split(",").map((value) => parseFloat(value.trim())));
      transform.precision = params.precision;
      break;

    case "geopoint":
      transform.type = "geopoint";
      transform.projection = params.projection; // Name of the projection to use
      transform.fields = params.fields; // Input geographic coordinates [longitude, latitude]
      transform.as = typeof params.as === "string" ? params.as.split(",").map((d) => d.trim()) : params.as; // Output x and y fields
      if (!transform.fields || transform.fields.length !== 2) {
        throw new Error(`geopoint requires exactly two input fields for longitude and latitude.`);
      }
      if (!transform.as || transform.as.length !== 2) {
        throw new Error(`geopoint requires exactly two output fields for x and y.`);
      }
      break;

    default:
      throw new Error(`Unsupported transform type: ${transformType}`);
  }

  // Filter out undefined properties
  Object.keys(transform).forEach((key) => transform[key] === undefined && delete transform[key]);

  // Add the transform to the Vega spec
  addPlotDataTransform(spec, dataName, transform, index);
}

// Function to set a mark in a plot specification.
export function setMark({
  mode,
  insertIdx,
  spec,
  dataName,
  markName,
  templateName,
  markType,
  groupBy,
  groupName,
  x,
  y,
  x2,
  y2,
  width,
  height,
  fill,
  stroke,
  strokeWidth,
  size,
  shape,
  opacity,
  fillOpacity,
  strokeOpacity,
  path,
  orient,
  tooltip,
  order,
  url,
  angle,
  align,
  baseline,
  clip,
  dx,
  dy,
  endAngle,
  innerRadius,
  outerRadius,
  startAngle,
  text,
  limit,
  ellipsis,
  font,
  fontSize,
  fontWeight,
  fontStyle,
  fontSizeStep,
  lineHeight,
  strokeDash,
  strokeDashOffset,
  theta,
  radius,
  interpolate,
  tension,
  defined,
  aspect,
  scale,
  zindex,
  href,
  role,
  description,
  style,
  sort,
  key,
  on,
  encode,
  transform,
  filter,
}) {
  if (mode === "update_all") {
    spec.marks
      .filter((d) => d.from?.data === dataName) // Only update marks with matching dataName
      .forEach((d) => {
        setMark({
          mode: "upsert",
          insertIdx,
          spec,
          dataName,
          markName: d.name,
          templateName,
          markType,
          groupBy,
          groupName,
          x,
          y,
          x2,
          y2,
          width,
          height,
          fill,
          stroke,
          strokeWidth,
          size,
          shape,
          opacity,
          fillOpacity,
          strokeOpacity,
          path,
          orient,
          tooltip,
          order,
          url,
          angle,
          align,
          baseline,
          clip,
          dx,
          dy,
          endAngle,
          innerRadius,
          outerRadius,
          startAngle,
          text,
          limit,
          ellipsis,
          font,
          fontSize,
          fontWeight,
          fontStyle,
          fontSizeStep,
          lineHeight,
          strokeDash,
          strokeDashOffset,
          theta,
          radius,
          interpolate,
          tension,
          defined,
          aspect,
          scale,
          zindex,
          href,
          role,
          description,
          style,
          sort,
          key,
          on,
          encode,
          transform,
          filter,
        });
      });
    return;
  }

  if (dataName === undefined) dataName = "table";
  const idx = findMark(spec, markName);
  const templateIdx = findMark(spec, templateName);
  let mark, newProps;

  if (mode === "delete" && idx !== -1) {
    if (Array.isArray(idx)) {
      spec.marks[idx[0]].marks.splice(idx[1], 1); // for nested (group) marks
    } else {
      spec.marks.splice(idx, 1);
    }
    return;
  }

  // Initiate new mark
  const initIdx = templateIdx === -1 && idx !== -1 ? idx : templateIdx;
  if (mode === "upsert" || mode === "truncate_reuse") {
    if (initIdx !== -1) {
      // markName exists
      if (Array.isArray(initIdx)) {
        mark = structuredClone(spec.marks[initIdx[0]].marks[initIdx[1]]);
      } else {
        mark = structuredClone(spec.marks[initIdx]);
      }
      dataName = dataName || mark.from.data; // Reuse the existing dataName
    } else if (spec.marks.length > 0) {
      // marks exist
      if (spec.marks[0].type === "group" && spec.marks[0].marks && spec.marks[0].marks.length > 0) {
        mark = structuredClone(spec.marks[0].marks[0]);
      } else {
        mark = structuredClone(spec.marks[0]);
      }
      dataName = dataName || mark.from.data; // Reuse the existing dataName
    } else {
      mark = {};
      dataName = dataName; // Empty mark with default table name
    }
  } else {
    mark = {};
    dataName = dataName; // Empty mark with default table name
  }

  // If template was geoshape, projection might be copied and needs to be removed explicitly.
  const geoTransformIdx = mark.transform?.findIndex((d) => d.type === "geoshape");
  if (geoTransformIdx !== -1 && mark.transform !== undefined && (markType === "symbol" || mark.type === "symbol")) {
    mark.transform.splice(geoTransformIdx, 1);
    if (mark.transform.length === 0) {
      delete mark.transform;
    }
  }

  newProps = {
    name: markName,
    type: markType,
    from: { data: dataName },
    encode: {
      update: {
        x,
        y,
        x2,
        y2,
        width,
        height,
        fill,
        stroke,
        strokeWidth,
        size,
        shape,
        opacity,
        fillOpacity,
        strokeOpacity,
        path,
        orient,
        tooltip,
        order,
        url,
        angle,
        align,
        baseline,
        clip,
        dx,
        dy,
        endAngle,
        innerRadius,
        outerRadius,
        startAngle,
        text,
        limit,
        ellipsis,
        font,
        fontSize,
        fontWeight,
        fontStyle,
        fontSizeStep,
        lineHeight,
        strokeDash,
        strokeDashOffset,
        theta,
        radius,
        interpolate,
        tension,
        defined,
        aspect,
        scale,
        zindex,
        href,
        role,
        description,
        style,
        sort,
        key,
      },
    },
    on,
    transform,
  };

  // Add filter
  if (filter) {
    const regex1 = new RegExp("^return ", "g");
    const regex2 = new RegExp(";$", "g");
    const filterExpr = filter.replaceAll("d.", "datum.").replaceAll(regex1, "").replaceAll(regex2, "");
    const transform = [
      {
        type: "filter",
        expr: filterExpr,
      },
    ];

    const filteredData = {
      name: dataName + "_filtered",
      source: dataName,
      transform: [
        {
          type: "filter",
          expr: filterExpr,
        },
      ],
    };
    spec.data.push(filteredData);
    newProps.from.data = dataName + "_filtered";
  }

  deleteUndefined(newProps);
  mark = deepAssign(mark, newProps);
  mark = deepCleanVega(mark);
  deleteNull(mark);
  // Update, replace or insert mark
  if (mode === "truncate_reuse" && spec.marks[0]?.type === "group") {
    // keep group
    spec.marks[0].marks = [];
    spec.marks[0].marks.push(mark);
  } else if (mode === "truncate" || mode === "truncate_reuse") {
    spec.marks = [];
    spec.marks.push(mark);
  } else if (Array.isArray(idx) && idx.length > 1 && idx[1] !== -1) {
    spec.marks[idx[0]].marks[idx[1]] = mark;
  } else if (Array.isArray(idx) && idx.length > 1 && idx[1] == -1) {
    spec.marks[idx[0]].marks.push(mark);
  } else if (idx !== -1) {
    spec.marks[idx] = mark;
  } else {
    if (insertIdx < 0) {
      spec.marks.push(mark);
    } else if (insertIdx >= 0 && insertIdx <= spec.marks.length) {
      spec.marks.splice(insertIdx, 0, mark);
    } else if (mode === "upsert" && spec.marks[0]?.type === "group") {
      // keep group
      spec.marks[0].marks.push(mark);
    } else {
      spec.marks.push(mark);
    }
  }

  if (groupBy) {
    groupMark({
      spec: spec,
      dataName: dataName,
      markName: markName,
      groupName: groupName,
      groupBy: groupBy,
    });
  }
}

// Function to set the vega scale
export function setScale({
  spec,
  dataName,
  scaleName,
  plotType,
  type,
  domain,
  sortAttr,
  order = "<none>",
  range,
  reverse,
  round,
  nice,
  zero,
  padding,
  paddingInner,
  paddingOuter,
  align,
  bins,
  clamp,
  interpolate,
  exponent,
  scheme,
  base,
  constant,
  domainMin,
  domainMax,
  domainMid,
  domainRaw,
  domainImplicit,
  timeInterval,
  timeStep,
}) {
  // if domain and range are not arrays, the value is used as attribute name.
  const dfltPlotType = plotType || "scatter"; // TODO extract plottype from spec?
  const dfltScale = scaleDefaults.find((d) => d.scaleName === scaleName);
  const dfltAttr = dfltScale ? dfltScale.scaleAttr : undefined;
  const dfltProp = dfltScale ? dfltScale.property : undefined;
  const dfltRange = dfltScale ? dfltScale.range : undefined;
  const specData = applyVegaTransform({ specData: structuredClone(spec.data), dataName: dataName });
  const data = specData.find((d) => d.name === dataName);
  const dataAttrs = getAttributeValues({
    data: data.values,
    format: data.format?.type,
    feature: data.format?.feature,
  });
  let domainData;
  if (!domain) {
    domainData = dataAttrs.map((d) => d[dfltAttr]) || []; // default attr or undefined
  } else if (Array.isArray(domain)) {
    domainData = domain; // array
  } else if (typeof domain == "object") {
    // vega ref
    if (Object.keys(domain).includes("field")) {
      domainData = dataAttrs.map((d) => getNestedProperty(d, domain.field));
    } else if (Object.keys(domain).includes("value")) {
      domainData = [domain.value];
    }
  } else {
    domainData = dataAttrs.map((d) => getNestedProperty(d, domain));
  } // domain attribute
  /*(!domain)? spec.data.find(d=>d.name === dataName).values.map(d=>d[])||[]:
          (Array.isArray(domain)) ? domain :
          (typeof domain=="object")? spec.data.find(d=>d.name === domain.data).values.map(d=>d[domain.field]):
          spec.data.find(d=>d.name === dataName).values.map(d=>d[domain])*/
  const dfltType = dfltScale ? selectVegaScaleType(domainData, dfltProp, dfltPlotType) : undefined;
  const scaleType = type ? type : dfltType;

  // Set nice value
  let niceVal = nice;
  if ([type, dfltType].includes("time") || [type, dfltType].includes("utc")) {
    if (timeInterval === "<none>") {
      niceVal = nice;
    } else if (!timeStep > 0) {
      niceVal = timeInterval;
    } else {
      niceVal = { interval: timeInterval, step: timeStep };
    }
  } else {
    niceVal = nice ? nice : scaleType === "linear" ? true : undefined;
  }

  // Set domain min and max value
  let domainMinVal = domainMin ? domainMin : undefined;
  let domainMaxVal = domainMin ? domainMin : undefined;
  if (["linear"].includes(scaleType)) {
    domainMinVal = domainMin ? domainMin : typeof domainData[0] === "number" ? min(domainData) : undefined;
    domainMaxVal = domainMax ? domainMax : typeof domainData[0] === "number" ? max(domainData) : undefined;
  }

  const scale = {
    name: scaleName,
    type: scaleType,
    domain: !domain ? (dfltAttr ? { data: dataName, field: dfltAttr } : undefined) : domain,
    range: !range ? dfltRange || undefined : range,
    ...(reverse && { reverse }),
    ...(round && { round }),
    nice: niceVal,
    zero: ["linear", "pow", "sqrt", "log", "symbol"].includes(scaleType)
      ? zero
        ? zero
        : scaleType === "linear"
          ? true
          : undefined
      : undefined,
    ...(padding && { padding }),
    ...(paddingInner && { paddingInner }),
    ...(paddingOuter && { paddingOuter }),
    align: ["band", "point"].includes(scaleType) ? (align ? align : undefined) : undefined,
    ...(bins && { bins: JSON.parse(bins) }),
    ...(clamp && { clamp }),
    ...(interpolate && { interpolate }),
    ...(exponent && { exponent }),
    ...(scheme && { scheme }),
    ...(base && { base }),
    ...(constant && { constant }),
    domainMin: domainMinVal,
    domainMax: domainMaxVal,
    ...(domainImplicit && { domainImplicit }),
  };

  // Domain sort if sorting is specified
  if (order !== "<none>") {
    if (typeof scale.domain === "object" && !Array.isArray(scale.domain)) {
      // vega ref object
      let sort = {};
      sort.op = "min";
      sort.field = sortAttr;
      sort.order = order != "<none>" ? order : undefined;
      scale.domain.sort = sort;
    } else if (Array.isArray(scale.domain)) {
      // explicit array
      let sortFn;
      if (order === "ascending") {
        sortFn = (a, b) => ascending(a, b);
      } else if (order === "descending") {
        sortFn = (a, b) => descending(a, b);
      }
      scale.domain.sort(sortFn);
    }

    // sort domainMin and domainMax if band scale
    if (["band"].includes(scale.type) && (scale.domainMin || scale.domainMax)) {
      const limits = structuredClone([scale.domainMin, scale.domainMax]);
      if (order === "descending") {
        scale.domainMin = max(limits);
        scale.domainMax = min(limits);
      } else if (order === "ascending") {
        scale.domainMin = min(limits);
        scale.domainMax = max(limits);
      }
    }
  }

  // Filter if domain min/max
  if (["xScale", "yScale"].includes(scaleName)) {
    spec.data
      .find((d) => d.name === dataName)
      .values.forEach((d) => {
        if (scale.domainMin || scale.domainMax) d["__plot"] = d["__plot"] === undefined ? true : d["__plot"];
        if (scale.domainMin && d[dfltAttr])
          d["__plot"] = d[dfltAttr] >= min([scale.domainMin, scale.domainMax]) && !!d["__plot"];
        if (scale.domainMax && d[dfltAttr])
          d["__plot"] = d[dfltAttr] <= max([scale.domainMin, scale.domainMax]) && !!d["__plot"];
        return d;
      });

    // Add filter transform
    if (spec.data.find((d) => d.name === dataName).values.length > 0) {
      if (Object.keys(spec.data.find((d) => d.name === dataName).values[0]).includes("__plot")) {
        let transforms = spec.data.find((d) => d.name === dataName).transform;
        transforms = transforms ? transforms : [];
        if (transforms.findIndex((d) => d.type === "filter" && d.expr === `datum.__plot`) === -1) {
          transforms.push({
            type: "filter",
            expr: `datum.__plot`,
          });
        }
        spec.data.find((d) => d.name === dataName).transform = transforms;
      }
    }
  }

  // Filter out undefined properties
  Object.keys(scale).forEach((key) => scale[key] === undefined && delete scale[key]);

  // Add or update scale in the spec
  const existingScaleIndex = spec.scales.findIndex((s) => s.name === scaleName);
  if (existingScaleIndex !== -1) {
    spec.scales[existingScaleIndex] = scale;
  } else {
    spec.scales.push(scale);
  }
}

// Function to apply scale in marks
export function applyScale({ spec, scaleName, plotType, attr, prop, markName, band }) {
  const property = prop || scaleDefaults.find((d) => d.scaleName === scaleName).property;
  const scaleAttr = attr || scaleDefaults.find((d) => d.scaleName === scaleName).scaleAttr;
  const newUpdate = {
    scale: scaleName,
    field: scaleAttr,
    band: band,
  };
  if (markName) {
    spec.marks.find((d) => (d.name = markName)).encode.update[property] = newUpdate;
  } else if (plotType) {
    spec.marks.find((d) => (d.name = plotType)).encode.update[property] = newUpdate;
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

// Function to apply a specific type of plot
export function setPlotType({
  spec,
  dataName,
  markName,
  type,
  shape,
  groupBy,
  stacked,
  offset,
  sortAttr,
  order,
  direction,
  baseline = 999,
  unitRatio,
}) {
  const [SCATTER, SEGMENT, LOLLIPOP, BAR, LINE, AREA, WAFFLE] = [
    "scatter",
    "segment",
    "lollipop",
    "bar",
    "line",
    "area",
    "waffle",
  ];
  let mark, marks;
  const baseScaleName = direction === "X" ? "yScale" : "xScale";
  const dirScaleName = direction === "X" ? "xScale" : "yScale";
  const baseAttr = direction === "X" ? "__y" : "__x";
  const dirAttr = direction === "X" ? "__x" : "__y";
  const baseScale = spec.scales.find((d) => d.name == baseScaleName);
  const dirScale = spec.scales.find((d) => d.name == dirScaleName);
  let baseScale_grid;
  let dirScale_grid;
  if (type === WAFFLE) {
    const gridScaleName = "gridUnitScale";
    /*let baseScaleDomain = baseScale.domain;
    let dirScaleDomain = dirScale.domain;
    console.log(dirScale.domain);*/
    /*if (!Array.isArray(baseScale.domain)) {
      baseScaleDomain = parseVegaRef(spec, baseScaleDomain);
    }
    if (!Array.isArray(dirScale.domain)) {
      dirScaleDomain = parseVegaRef(spec, dirScaleDomain);
    }*/

    const nRows =
      direction === "X"
        ? max(baseScale.domain) - min(baseScale.domain) + 1
        : max(dirScale.domain) - min(dirScale.domain) + 1;
    const nCols =
      direction === "X"
        ? max(dirScale.domain) - min(dirScale.domain) + 1
        : max(baseScale.domain) - min(baseScale.domain) + 1;
    addGridData({
      spec: spec,
      dataName: dataName,
      nCols: nCols,
      nRows: nRows,
      gridScaleName: gridScaleName,
      groupBy: groupBy,
      unitRatio: unitRatio,
      offset: offset,
      direction: direction,
      sortAttr: sortAttr,
      order: order,
    });
    dataName = "gridTable"; // use gridTable for waffle plot
    baseScale_grid = structuredClone(baseScale);
    dirScale_grid = structuredClone(dirScale);
    baseScale_grid.name = baseScale_grid.name + "_grid";
    dirScale_grid.name = dirScale_grid.name + "_grid";
    baseScale_grid.domain = { data: dataName, field: direction === "X" ? "__y" : "__x" };
    // "sort": {"order": (direction === "X")? "descending": "ascending"}}
    dirScale_grid.domain = { data: dataName, field: direction === "X" ? "__x" : "__y" };
    //"sort": {"order": (direction === "X")? "ascending": "descending"}}
  }

  let domainData = spec.data.find((d) => d.name == dataName).values || [];
  if (domainData.length > 0) {
    if (Object.keys(domainData[0]).includes("__plot")) {
      domainData = domainData.filter((d) => d["__plot"]);
    }
  }

  const dirDomainMin = min(domainData.map((d) => d[direction === "X" ? "__x" : "__y"]));
  const dirDomainMax = max(domainData.map((d) => d[direction === "X" ? "__x" : "__y"]));
  dirScale.domainMin = [BAR, SEGMENT, LOLLIPOP].includes(type) ? dirDomainMin - 0.05 * dirDomainMin : dirDomainMin; // Assure minimium length of bar/segment
  dirScale.domainMax = dirDomainMax;
  if (type === WAFFLE) {
    const baseDomainMin = min(domainData.map((d) => d[direction === "X" ? "__y" : "__x"]));
    const baseDomainMax = max(domainData.map((d) => d[direction === "X" ? "__y" : "__x"]));
    baseScale_grid.domainMin = baseDomainMin;
    baseScale_grid.domainMax = baseDomainMax;
  }

  let dirD3Scale = parseVegaScale(spec, dirScale);
  let dirTicks = dirD3Scale.ticks ? dirD3Scale.ticks() : dirD3Scale.domain();
  let baseValue = baseline != 999 ? baseline : min(dirTicks);

  let baseD3Scale = parseVegaScale(spec, baseScale);

  switch (type) {
    case SCATTER:
      markName = markName || "scatter";
      setMark({
        mode: "truncate_reuse",
        spec,
        dataName,
        markName: markName,
        markType: "symbol",
        x: { scale: "xScale", field: "__x" },
        y: { scale: "yScale", field: "__y" },
        shape: { value: Object.keys(symbols).includes(shape) ? symbols[shape] : undefined },
      });
      break;
    case BAR:
      markName = markName || "bar";
      if (baseScale.type != "band") {
        baseScale.type = "band";
        baseScale.padding = 0.05;
      }

      setMark({
        mode: "truncate_reuse",
        spec,
        dataName,
        markName: markName,
        markType: "rect",
        x: { scale: "xScale", field: "__x" },
        y: { scale: "yScale", field: "__y" },
        [direction === "X" ? "height" : "width"]: { scale: baseScaleName, band: 1 },
        [direction === "X" ? "x2" : "y2"]: { scale: dirScaleName, value: baseValue },
      });
      if (stacked) {
        stack(spec, dataName, markName, direction, offset, sortAttr, order);
      }
      break;
    case LINE:
      markName = markName || "line";
      sortSpecData(spec, dataName, baseAttr, "ascending");
      setMark({
        mode: "truncate_reuse",
        spec,
        dataName,
        markName: markName,
        markType: "line",
        x: { scale: "xScale", field: "__x" },
        y: { scale: "yScale", field: "__y" },
        fillOpacity: { value: 0, scale: null, field: null },
        fill: null,
      });
      if (stacked) {
        stack(spec, dataName, markName, direction, offset, sortAttr, order);
      }
      break;
    case SEGMENT:
      markName = markName || "segment";
      setMark({
        mode: "truncate_reuse",
        spec,
        dataName,
        markName: markName,
        markType: "rule",
        x: { scale: "xScale", field: "__x" },
        y: { scale: "yScale", field: "__y" },
        [direction === "X" ? "x2" : "y2"]: { scale: dirScaleName, value: baseValue },
        fillOpacity: { value: 0, scale: null, field: null },
        fill: null,
      });
      if (stacked) {
        stack(spec, dataName, markName, direction, offset, sortAttr, order);
      }
      break;
    case LOLLIPOP:
      markName = markName || "lollipop_symbol";
      setMark({
        mode: "truncate_reuse",
        spec,
        dataName,
        markName: "lollipop_symbol",
        markType: "symbol",
        x: { scale: "xScale", field: "__x" },
        y: { scale: "yScale", field: "__y" },
        shape: { value: Object.keys(symbols).includes(shape) ? symbols[shape] : undefined },
        size: { value: 100 },
      });
      if (stacked) {
        setMark({
          mode: "upsert",
          spec,
          dataName,
          markName: "lollipop_symbol",
          [direction === "X" ? "x" : "y"]: { scale: dirScaleName, field: "__base1" },
        });
      }
      markName = "lollipop_segment";

      setMark({
        mode: "upsert",
        spec,
        dataName,
        markName: markName || "lollipop_segment",
        templateName: "lollipop_symbol",
        markType: "rule",
        x: { scale: "xScale", field: "__x" },
        y: { scale: "yScale", field: "__y" },
        [direction === "X" ? "x2" : "y2"]: { scale: dirScaleName, value: baseValue },
        fill: null,
        insertIdx: 0,
      });
      if (stacked) {
        stack(spec, dataName, markName, direction, offset, sortAttr, order);
      }

      break;
    case AREA:
      markName = markName || "area";
      sortSpecData(spec, dataName, baseAttr, "ascending");
      setMark({
        mode: "truncate_reuse",
        spec,
        dataName,
        markName: markName,
        markType: "area",
        x: { scale: "xScale", field: "__x" },
        y: { scale: "yScale", field: "__y" },
        [direction === "X" ? "x2" : "y2"]: { scale: dirScaleName, value: baseValue },
        orient: { value: direction === "X" ? "horizontal" : "vertical" },
      });
      if (stacked) {
        stack(spec, dataName, markName, direction, offset, sortAttr, order);
      }
      break;
    case WAFFLE:
      markName = markName || "waffle";
      baseD3Scale = parseVegaScale(spec, baseScale_grid);
      dirD3Scale = parseVegaScale(spec, dirScale_grid);
      dirTicks = dirD3Scale.ticks ? dirD3Scale.ticks() : dirD3Scale.domain();

      if (baseScale_grid.type != "band") {
        baseScale_grid.type = "band";
        //baseScale.paddingInner = 0.1;
        baseScale_grid.zero = null;
        baseScale_grid.nice = null;
      }
      if (dirScale_grid.type != "band") {
        dirScale_grid.type = "band";
        //dirScale.paddingInner = 0.1;
        dirScale_grid.zero = null;
        dirScale_grid.nice = null;
      }

      // Add or update grid scales in the spec
      addPlotScale({ spec: spec, scale: baseScale_grid });
      addPlotScale({ spec: spec, scale: dirScale_grid });

      dirD3Scale = parseVegaScale(spec, dirScale_grid);
      baseD3Scale = parseVegaScale(spec, baseScale_grid);
      const shapeSize = min([dirD3Scale.bandwidth(), baseD3Scale.bandwidth()]) ** 2;

      // Set symbol mark (reuse properties, incl. shape)
      setMark({
        mode: "truncate_reuse",
        spec,
        dataName: dataName,
        markName: markName,
        markType: "symbol",
        x: { scale: "xScale_grid", field: "__x", band: 0.5 },
        y: { scale: "yScale_grid", field: "__y", band: 0.5 },
        size: { value: shapeSize },
      });

      if (Object.keys(symbols).includes(shape)) {
        // Set symbol mark shape if provided
        spec.marks.find((d) => d.name === markName).encode.update.shape = { value: symbols[shape] };
      } else if (!spec.marks.find((d) => d.name === markName).encode.update.shape) {
        // Set default shape
        setMark({
          mode: "truncate_reuse",
          spec,
          dataName: dataName,
          markName: markName,
          markType: "rect",
          x: { scale: "xScale_grid", field: "__x", band: 0 },
          y: { scale: "yScale_grid", field: "__y", band: 0 },
          width: { scale: "xScale_grid", band: 1 },
          height: { scale: "yScale_grid", band: 1 },
        });
      }

      // Change dataName in scales
      const waffleMark = spec.marks.find((d) => d.name === markName).encode.update;
      Object.values(waffleMark).forEach((val) => {
        if (val.scale) {
          const specScale = spec.scales.find((d) => d.name === val.scale);
          if (typeof specScale.domain === "object") {
            if (Object.keys(specScale.domain).includes("data")) {
              specScale.domain.data = dataName;
            }
          }
        }
      });

      break;
  }

  if (groupBy && !["waffle"].includes(type)) {
    groupMark({
      spec: spec,
      dataName: dataName,
      markName: markName,
      groupName: "series",
      groupBy: groupBy,
    });
  }
}

// Function to set axis of plot
export function setAxis({
  spec,
  scale,
  orient,
  title,
  offset,
  position,
  titlePadding,
  maxExtent,
  minExtent,
  labels,
  labelValues,
  labelAngle,
  labelAlign,
  labelPadding,
  labelBaseline,
  ticks,
  tickSize,
  tickCount,
  tickOffset,
  tickMinStep,
  domain,
  domainCap,
  domainColor,
  domainDash,
  domainDashOffset,
  domainOpacity,
  domainWidth,
  grid,
  gridCap,
  gridColor,
  gridDash,
  gridDashOffset,
  gridOpacity,
  gridScale,
  gridWidth,
  zindex,
  format,
  remove,
  encode,
}) {
  // Find existing axis for the scale
  const existingAxisIndex = spec.axes.findIndex((a) => a.scale === scale);
  if (remove) {
    // remove if axis exists, else do nothing
    if (existingAxisIndex !== -1) spec.axes.splice(existingAxisIndex, 1);
    return;
  }

  let encodeObj = encode;
  if (typeof encode === "string") {
    encodeObj = JSON.parse(encode.replaceAll("d.", "datum."));
  }

  const axis = {
    scale: scale,
    orient: orient || undefined,
    title: title || undefined,
    offset: offset || undefined,
    position: position || undefined,
    titlePadding: titlePadding || undefined,
    maxExtent: maxExtent || undefined,
    minExtent: minExtent || undefined,
    labels: labels !== undefined ? labels : undefined,
    values: labelValues,
    labelAngle: labelAngle,
    labelAlign: labelAlign,
    labelPadding: labelPadding,
    labelBaseline: labelBaseline,
    ticks: ticks !== undefined ? ticks : undefined,
    tickSize: tickSize || undefined,
    tickCount: tickCount || undefined,
    tickOffset: tickOffset || undefined,
    tickMinStep: tickMinStep || undefined,
    domain: domain !== undefined ? domain : undefined,
    domainCap: domainCap || "butt",
    domainColor: domainColor || undefined,
    domainDash: domainDash || undefined,
    domainDashOffset: domainDashOffset || undefined,
    domainOpacity: domainOpacity || undefined,
    domainWidth: domainWidth || undefined,
    grid: grid !== undefined ? grid : undefined,
    gridCap: gridCap || undefined,
    gridColor: gridColor || undefined,
    gridDash: gridDash || undefined,
    gridDashOffset: gridDashOffset || undefined,
    gridOpacity: gridOpacity || undefined,
    gridScale: gridScale || undefined,
    gridWidth: gridWidth || undefined,
    format: format || undefined,
    encode: encodeObj || undefined,
    zindex: zindex || undefined,
  };

  // Filter out undefined properties
  Object.keys(axis).forEach((key) => axis[key] === undefined && delete axis[key]);

  // Update or insert new axis
  if (existingAxisIndex !== -1) {
    spec.axes[existingAxisIndex] = axis;
  } else {
    spec.axes.push(axis);
  }
}

// A function to set the plot legend
export function addLegend({
  spec,
  scaleNames,
  markName,
  title,
  shape,
  values,
  type,
  orient,
  direction,
  columns,
  offset,
  padding,
  titlePadding,
  rowPadding,
  columnPadding,
  cornerRadius,
  gradientLength,
  gradientThickness,
  legendX,
  legendY,
  titleFont,
  titleFontSize,
  titleFontWeight,
  titleColor,
  titleAlign,
  titleAnchor,
  titleBaseline,
  labelColor,
  labelFont,
  labelFontSize,
  labelFontWeight,
  labelLimit,
  labelFormat,
  labelAlign,
  labelBaseline,
  symbolSize,
  symbolFillColor,
  symbolStrokeColor,
  symbolOpacity,
  symbolType,
  gradientLabelOffset,
  tickCount,
  tickMinStep,
  tickOffset,
  tickSize,
  clipHeight,
  zindex,
  interactive,
  fillScale,
  opacityScale,
  shapeScale,
  sizeScale,
  strokeScale,
  strokeDashScale,
  strokeWidthScale,
  encode,
  fillColor,
  strokeColor,
  gradientOpacity,
  gradientStrokeColor,
  gradientStrokeWidth,
  labelFontStyle,
  labelOpacity,
  labelOverlap,
  labelSeparation,
  symbolDash,
  symbolDashOffset,
  symbolLimit,
  symbolOffset,
  titleFontStyle,
  titleLimit,
  titleLineHeight,
  titleOpacity,
  titleOrient,
  gridAlign,
}) {
  //const allScales = scaleDefaults.filter((d)=>!["xScale", "yScale"].includes(d.scaleName)).map((d)=>d.scaleName)
  //let selectedScales = (scaleNames === "all") ? allScales : scaleNames.split(",");
  let selectedScales = scaleNames === "all" ? spec.scales.map((d) => d.name) : scaleNames.split(",");
  selectedScales = selectedScales.filter((d) => !["xScale", "yScale"].includes(d));
  let legendProps = {};
  //let commonDomain = null;
  //const specScales = structuredClone(spec.scales)

  // Apply custom scale names if provided
  if (fillScale) legendProps.fill = fillScale;
  if (opacityScale) legendProps.opacity = opacityScale;
  if (shapeScale) legendProps.shape = shapeScale;
  if (sizeScale) legendProps.size = sizeScale;
  if (strokeScale) legendProps.stroke = strokeScale;
  if (strokeDashScale) legendProps.strokeDash = strokeDashScale;
  if (strokeWidthScale) legendProps.strokeWidth = strokeWidthScale;

  let markIdx = findMark(spec, markName);
  if (markIdx === -1) markIdx = findMark(spec, "symbol", "type");
  if (markIdx === -1) markIdx = findMark(spec, "rect", "type");
  if (markIdx === -1) markIdx = findMark(spec, "area", "type");

  let mark;
  if (markIdx === -1 && spec.marks[0].type === "group") {
    mark = spec.marks[0].marks[0];
  } else if (markIdx === -1 && spec.marks[0].type !== "group") {
    mark = spec.marks[0];
  } else if (Array.isArray(markIdx)) {
    mark = spec.marks[markIdx[0]].marks[markIdx[1]];
  } else {
    mark = spec.marks[markIdx];
  }

  // Get legendprops from mark
  const stages = ["enter", "update"];
  let scaleCnt = 0;
  let valueProps = [];
  stages.forEach((stage) => {
    if (mark.encode[stage]) {
      Object.keys(mark.encode[stage]).forEach((key) => {
        const prop = mark.encode[stage][key];
        if (prop?.scale && selectedScales.includes(prop?.scale)) {
          scaleCnt++;
          legendProps[key] = prop.scale;
        } else if (prop?.value) {
          valueProps.push(key);
          legendProps.encode = legendProps.encode || {};
          legendProps.encode.symbols = legendProps.encode.symbols || {};
          legendProps.encode.symbols.enter = legendProps.encode.symbols.enter || {};
          legendProps.encode.symbols.enter[key] = prop;
        }
      });
    }
  });

  // Ensure there is at least 1 legend scale
  if (scaleCnt === 0) {
    const valueProp_value = legendProps.encode.symbols.enter[valueProps[0]].value;
    const valueProp_scale = "__" + valueProps[0] + "_legendScale";
    setScale({ spec: spec, scaleName: valueProp_scale, type: "linear", domain: [valueProp_value] });
    legendProps[valueProps[0]] = valueProp_scale;
    delete legendProps.encode.symbols.enter[valueProps[0]];
  }

  let legendShape = shape;
  if (shape === "<auto>") {
    legendShape = determineLegendShape(spec.marks, selectedScales);
  }

  // Create legend object with all provided properties
  let legend = {
    title: title,
    titleFont: titleFont,
    titleFontSize: titleFontSize,
    titleFontWeight: titleFontWeight,
    titleColor: titleColor,
    titleAlign: titleAlign,
    titleAnchor: titleAnchor,
    titleBaseline: titleBaseline,
    titleFontStyle: titleFontStyle,
    titleLimit: titleLimit,
    titleLineHeight: titleLineHeight,
    titleOpacity: titleOpacity,
    titleOrient: titleOrient,
    format: labelFormat ? labelFormat : undefined,
    encode: {
      symbols: {
        enter: {
          shape: { value: legendShape },
          size: symbolSize ? { value: symbolSize } : undefined,
          fill: symbolFillColor ? { value: symbolFillColor } : undefined,
          stroke: symbolStrokeColor ? { value: symbolStrokeColor } : undefined,
          opacity: symbolOpacity ? { value: symbolOpacity } : undefined,
          strokeDash: symbolDash ? { value: symbolDash } : undefined,
          strokeDashOffset: symbolDashOffset ? { value: symbolDashOffset } : undefined,
        },
      },
      labels: {
        enter: {
          fill: labelColor ? { value: labelColor } : undefined,
          font: labelFont ? { value: labelFont } : undefined,
          fontSize: labelFontSize ? { value: labelFontSize } : undefined,
          fontWeight: labelFontWeight ? { value: labelFontWeight } : undefined,
          limit: labelLimit ? { value: labelLimit } : undefined,
          align: labelAlign ? { value: labelAlign } : undefined,
          baseline: labelBaseline ? { value: labelBaseline } : undefined,
          fontStyle: labelFontStyle ? { value: labelFontStyle } : undefined,
          opacity: labelOpacity ? { value: labelOpacity } : undefined,
        },
      },
    },
    fillColor: fillColor ? { value: fillColor } : undefined,
    strokeColor: strokeColor ? { value: strokeColor } : undefined,
    gradientOpacity: gradientOpacity ? { value: gradientOpacity } : undefined,
    gradientStrokeColor: gradientStrokeColor ? { value: gradientStrokeColor } : undefined,
    gradientStrokeWidth: gradientStrokeWidth ? { value: gradientStrokeWidth } : undefined,
    values: values,
    type: type,
    orient: orient,
    direction: direction,
    columns: direction === "horizontal" ? 0 : columns,
    offset: offset,
    padding: padding,
    rowPadding: rowPadding,
    columnPadding: columnPadding,
    cornerRadius: cornerRadius,
    gradientLength: gradientLength,
    gradientThickness: gradientThickness,
    gradientLabelOffset: gradientLabelOffset,
    tickCount: tickCount,
    tickMinStep: tickMinStep,
    tickOffset: tickOffset,
    tickSize: tickSize,
    clipHeight: clipHeight,
    zindex: zindex,
    interactive: interactive,
    legendX: legendX || 0,
    legendY: legendY || 0,
    symbolLimit: symbolLimit,
    symbolOffset: symbolOffset,
    labelOverlap: labelOverlap,
    labelSeparation: labelSeparation,
    gridAlign: gridAlign,
  };

  // Set default symbol color
  if (!legend.encode.symbols.enter.fill && !legend.encode.symbols.enter.stroke && !legend.fill && !legend.stroke) {
    legend.encode.symboles.enter.shape = legend.encode.symboles.enter.shape || "square";
    legend.encode.symbols.enter.fill = { value: "#4c78a8" };
    legend.encode.symbols.enter.stroke = { value: null };
  } else if (legend.fill) {
    delete legend.encode.symbols.enter.fill;
  } else if (legend.stroke) {
    delete legend.encode.symbols.enter.stroke;
  }

  deepAssign(legend, legendProps);
  deepAssign(legend, { encode: encode });

  if (!spec.legends) {
    spec.legends = [];
  }

  spec.legends.push(legend);
}

// Function to apply colors
export function applyColorTheme(spec, bgColor, primColor, secColor, accentColor, colorScheme) {
  let theme = spec.config || {};
  const newTheme = {
    background: bgColor,
    view: {
      stroke: primColor,
    },
    title: {
      color: primColor,
      subtitleColor: primColor,
    },
    axis: {
      domainColor: primColor,
      tickColor: primColor,
      labelColor: primColor,
      titleColor: primColor,
    },
    axisY: {
      gridColor: secColor,
    },
    axisX: {
      gridColor: secColor,
    },
    legend: {
      labelColor: primColor,
      titleColor: primColor,
    },

    arc: { fill: accentColor },
    area: { fill: accentColor },
    line: { stroke: accentColor },
    path: { stroke: accentColor },
    rect: { fill: accentColor },
    shape: { stroke: accentColor },
    symbol: { fill: accentColor },

    text: { fill: primColor },

    style: {
      "guide-label": {
        fill: primColor, // Primary color for guide labels
      },
      "guide-title": {
        fill: primColor, // Primary color for guide titles
      },
    },
    range: {
      category: colorScheme,
    },
  };
  // Filter out undefined properties
  Object.keys(newTheme).forEach((key) => newTheme[key] === undefined && delete newTheme[key]);

  // Replace config
  spec.config = deepAssign(theme, newTheme);
}

/*
-------- GEOGRAPHIC PLOT FUNCTIONS --------
*/

// Function to plot geodata with reuseMap support
export function plotGeodata_old({
  spec,
  geodata,
  format,
  feature,
  dataName,
  markName,
  targetProject,
  center,
  scale = 1000,
  rotate,
  reuseMap = false, // New parameter to reuse existing map properties
}) {
  // Handle topojson
  if (format === "topojson" && Array.isArray(geodata)) {
    geodata = geodata[0];
  }
  // Use addPlotData to add or update geodata in the Vega spec
  if (geodata) {
    addPlotData(spec, geodata, dataName, format, feature); // Add data
  }

  let newProjection = {
    name: "projection",
    type: targetProject, // Use the selected projection type from user input
    center: center, // Apply the center parameter from user input
    translate: [{ signal: "width/2" }, { signal: "height/2" }], // Center on the visualization figure
    scale: scale, // Apply the scale parameter
    rotate: rotate, // Apply the rotate parameter
  };

  // Handle projection: reuse existing properties if reuseMap is enabled
  const existingProjectionIndex = spec.projections.findIndex((p) => p.name === "projection");
  if (existingProjectionIndex !== -1) {
    const existingProjection = spec.projections[existingProjectionIndex];

    if (reuseMap) {
      // Try existing projection properties if reuseMap is enabled
      newProjection.type = existingProjection.type || newProjection.type;
      newProjection.center = existingProjection.center || newProjection.center;
      newProjection.scale = existingProjection.scale || newProjection.scale;
      newProjection.rotate = existingProjection.rotate || newProjection.rotate;
    }

    // Merge with existing projection properties if they are undefined in the new one
    newProjection = deepAssign(existingProjection, newProjection); // Use deepAssign to merge projections
    spec.projections[existingProjectionIndex] = newProjection;
  } else {
    // Add new projection if none exists
    spec.projections.push(newProjection);
  }

  // Use setMark function to define or update the marks for the geoshape
  setMark({
    mode: "upsert", // Insert or update the mark
    spec: spec, // The current Vega spec
    dataName: dataName, // Data source name
    markName: markName, // Mark name for geoshape
    markType: "shape", // Mark type (shape)
    transform: [{ type: "geoshape", projection: "projection" }], // Transform using the specified projection
  });

  return spec;
}

// Function to plot geodata
export function plotGeodata({ spec, geodata, format, feature, dataName, markName }) {
  // Handle data formats
  if (format === "topojson" && Array.isArray(geodata)) {
    geodata = geodata[0];
  }
  if (format === "geojson") {
    if (Array.isArray(geodata)) {
      geodata = geodata[0];
    }
    geodata = geodata.features;
    format = "json";
  }
  if (!geodata) {
    throw new Error(`Data is empty or has invalid '${dataName}' format.`);
  }

  // Use addPlotData to add or update geodata in the Vega spec
  if (geodata) {
    addPlotData(spec, geodata, dataName, format, feature); // Add data
  }

  // Set default map projection if no existing
  if (!("projections" in spec) || spec.projections?.length === 0) {
    setGeoMap({ spec, projectionType: "mercator" });
  }
  //setGeoMap({ spec, projectionType: targetProject, center, scale, rotate, translate, reuseMap });

  // Use setMark function to define or update the marks for the geoshape
  setGeoMark({
    mode: "upsert", // Insert or update the mark
    spec: spec, // The current Vega spec
    dataName: dataName, // Data source name
    markName: markName, // Mark name for geoshape
    markType: "shape", // Mark type (shape)
    transform: [{ type: "geoshape", projection: "projection" }], // Transform using the specified projection
  });

  return spec;
}

// Function to define the geographic map (Vega projection properties)
export function setGeoMap({ spec, projectionType, center, scale, rotate, translate, reuseExisting }) {
  const arrRegex = new RegExp("^\\[.*\\]$");
  let translateArray;
  //const translateDflt = [{ signal: "width/2" }, { signal: "height/2" }];
  const translateDflt = [spec.width / 2, spec.height / 2];

  // Convert translate to array or default
  if (Array.isArray(translate)) {
    if (translate.length === 0) {
      translateArray = translateDflt;
    } else {
      translateArray = translate.map((d, i) => d + translateDflt[i]);
    }
  } else if (typeof translate === "string") {
    if (translate.match(arrRegex)) {
      translateArray = JSON.parse(translate);
    } else {
      // string or comma seperated list
      translateArray = translate.split(",").map((d) => {
        // Convert comma-separated value to array
        if (!isNaN(Number(d))) {
          return Number(d); // number values
        } else if (d === "null") {
          return null; // null value
        } else if (d.match(litRegex)) {
          return JSON.parse(d); // quoted string values
        } else {
          return d;
        } // other
      });
    }
  } else {
    translateArray = translateDflt;
  }

  let newProjection = {
    name: "projection",
    type: projectionType,
    center,
    translate: translateArray,
    scale,
    rotate,
  };

  // Handle reuseMap logic
  const existingProjectionIndex = spec.projections.findIndex((p) => p.name === "projection");
  if (existingProjectionIndex !== -1) {
    const existingProjection = spec.projections[existingProjectionIndex];

    if (reuseExisting) {
      // Reuse properties from the existing projection
      newProjection.type = existingProjection.type || newProjection.type;
      newProjection.center = existingProjection.center || newProjection.center;
      newProjection.scale = existingProjection.scale || newProjection.scale;
      newProjection.rotate = existingProjection.rotate || newProjection.rotate;
    }

    // Merge with existing projection properties
    newProjection = deepAssign(existingProjection, newProjection); // Deep merge existing and new projection
    spec.projections[existingProjectionIndex] = newProjection;
  } else {
    // Add new projection if none exists
    spec.projections.push(newProjection);
  }
}

// Helper function to set geographic marks
export function setGeoMark_old({
  mode,
  insertIdx,
  spec,
  dataName,
  markName,
  templateName,
  markType,
  groupBy,
  groupName,
  x,
  y,
  x2,
  y2,
  width,
  height,
  fill,
  stroke,
  strokeWidth,
  size,
  shape,
  opacity,
  fillOpacity,
  strokeOpacity,
  path,
  orient,
  tooltip,
  order,
  url,
  angle,
  align,
  baseline,
  clip,
  dx,
  dy,
  endAngle,
  innerRadius,
  outerRadius,
  startAngle,
  text,
  limit,
  ellipsis,
  font,
  fontSize,
  fontWeight,
  fontStyle,
  fontSizeStep,
  lineHeight,
  strokeDash,
  strokeDashOffset,
  theta,
  radius,
  interpolate,
  tension,
  defined,
  aspect,
  scale,
  zindex,
  href,
  role,
  description,
  style,
  sort,
  key,
  on,
  encode,
  transform,
  filter,
}) {
  // For now, just call setMark with the same arguments
  setMark({
    mode,
    insertIdx,
    spec,
    dataName,
    markName,
    templateName,
    markType,
    groupBy,
    groupName,
    x,
    y,
    x2,
    y2,
    width,
    height,
    fill,
    stroke,
    strokeWidth,
    size,
    shape,
    opacity,
    fillOpacity,
    strokeOpacity,
    path,
    orient,
    tooltip,
    order,
    url,
    angle,
    align,
    baseline,
    clip,
    dx,
    dy,
    endAngle,
    innerRadius,
    outerRadius,
    startAngle,
    text,
    limit,
    ellipsis,
    font,
    fontSize,
    fontWeight,
    fontStyle,
    fontSizeStep,
    lineHeight,
    strokeDash,
    strokeDashOffset,
    theta,
    radius,
    interpolate,
    tension,
    defined,
    aspect,
    scale,
    zindex,
    href,
    role,
    description,
    style,
    sort,
    key,
    on,
    encode,
    transform,
    filter,
  });
}
// Helper function to set geographic marks
export function setGeoMark({
  mode,
  insertIdx,
  spec,
  dataName,
  geometry = "geometry", // Optional: geometry attribute
  feature, // Optional: Feature property for TopoJSON
  projectionName = "projection",
  markName,
  templateName,
  markType = "shape", // Default mark type
  groupBy,
  groupName,
  fill,
  stroke,
  strokeWidth,
  size,
  shape,
  opacity,
  fillOpacity,
  strokeOpacity,
  path,
  orient,
  tooltip,
  order,
  url,
  angle,
  align,
  baseline,
  clip,
  dx,
  dy,
  transform = [],
  filter,
}) {
  // Get geodata
  const dataName_clone = dataName || "geodata";
  const geodata = spec.data.find((d) => d.name === dataName_clone)?.values;

  // Detect geometry types using the geodata and geometry property
  const geometryTypes = detectGeometryTypes(
    geodata || spec.data.find((d) => d.name === dataName_clone),
    feature,
    geometry,
  );

  // Determine the appropriate mark type based on the decision tree
  let determinedMarkType;
  if (geometryTypes.includes("Point") || geometryTypes.includes("MultiPoint")) {
    determinedMarkType = "symbol"; // Use symbol for points
  } else if (
    geometryTypes.includes("LineString") ||
    geometryTypes.includes("MultiLineString") ||
    geometryTypes.includes("Polygon") ||
    geometryTypes.includes("MultiPolygon") ||
    geometryTypes.includes("GeometryCollection")
  ) {
    determinedMarkType = "shape"; // Use geoshape for other geometry types
  } else {
    console.warn("Unsupported geometry type detected. Defaulting to geoshape.");
    determinedMarkType = "shape"; // Default to geoshape
  }

  // For all other geometry types, default to geoshape
  const geoTransformIdx = transform?.findIndex((d) => d.type === "geoshape");
  let transforms;
  if (determinedMarkType === "symbol") {
    if (geoTransformIdx !== -1) {
      transforms = [...transform];
      transforms.splice(geoTransformIdx, 1);
    }
    setPlotDataTransform({
      spec: spec,
      dataName: dataName_clone,
      transformType: "geopoint",
      params: {
        projection: projectionName, // The name of the projection in the Vega spec
        fields: [`${geometry}.coordinates[0]`, `${geometry}.coordinates[1]`], // Input longitude and latitude fields
        as: ["__x", "__y"], // Output x and y fields
      },
    });
    // Inline projection for x and y using geometry attribute
    setMark({
      mode,
      insertIdx,
      spec,
      dataName: dataName_clone,
      markName: markName,
      templateName,
      markType: "symbol",
      transform: transforms,
      x: {
        field: "__x",
      },
      y: {
        field: "__y",
      },
      fill,
      stroke,
      strokeWidth,
      size,
      shape,
      opacity,
      fillOpacity,
      strokeOpacity,
      path,
      orient,
      tooltip,
      order,
      url,
      angle,
      align,
      baseline,
      clip,
      dx,
      dy,
      filter,
    });
  } else {
    transforms =
      geoTransformIdx !== -1 ? [...transform] : [...transform, { type: "geoshape", projection: "projection" }];
    setMark({
      mode,
      insertIdx,
      spec,
      dataName: dataName_clone,
      markName: markName,
      templateName,
      markType: "shape",
      transform: transforms,
      fill,
      stroke,
      strokeWidth,
      size,
      shape,
      opacity,
      fillOpacity,
      strokeOpacity,
      path,
      orient,
      tooltip,
      order,
      url,
      angle,
      align,
      baseline,
      clip,
      dx,
      dy,
      filter,
    });
  }
}

// Function to add a Vega graticule to the plot
export function setGraticule({
  spec,
  graticuleName = "graticule", // Name of the graticule data set
  projection = "projection", // Default projection transform
  step, // Step for the graticule lines [longitude, latitude]
  extent, // Extent of the graticule, e.g., [[-180, -90], [180, 90]]
  precision = 2.5, // Graticule precision (smaller value = more points)
  stroke = "lightgray", // Graticule line color
  strokeWidth = 0.25, // Graticule line width
  strokeOpacity = 0.75, // Graticule line opacity
  customTransformName, // Custom projection transform name (null means default)
  removeGraticule = true, // Boolean to remove graticule-related components
}) {
  // If removeGraticule is set to false, remove all graticule components from spec
  if (removeGraticule) {
    // Remove any existing graticule-related data
    const existingDataIndex = spec.data.findIndex((d) => d.name === graticuleName);
    if (existingDataIndex !== -1) {
      spec.data.splice(existingDataIndex, 1); // Remove graticule data
    }

    // Remove any existing graticule-related mark
    const existingMarkIndex = spec.marks.findIndex((m) => m.name === `${graticuleName}_mark`);
    if (existingMarkIndex !== -1) {
      spec.marks.splice(existingMarkIndex, 1); // Remove graticule mark
    }
    return spec; // Exit the function after removing components
  } else {
    // Calculate default extent if not provided
    if (extent === undefined || containsNaN(extent)) {
      const projectionSpec = structuredClone(spec.projections?.find((p) => p.name === projection));
      const width = spec.width;
      const height = spec.height;
      // if projection contains signals
      if (Array.isArray(projectionSpec?.translate)) {
        if (projectionSpec.translate.find((d) => d.hasOwnProperty("signal"))) {
          projectionSpec.translate = [width / 2, height / 2];
        }
      }
      console.warn("The projection contains signals. Signals are replaced with parameters. ");
      if (projectionSpec) {
        extent = (() => {
          const calculatedExtent = calculateMapExtent(projectionSpec, width, height);
          return [
            [calculatedExtent.minLongitude, calculatedExtent.minLatitude],
            [calculatedExtent.maxLongitude, calculatedExtent.maxLatitude],
          ];
        })();
      }
    } // else extent is undefined (Vega default is used)

    // Calculate default step if not provided
    if (step === undefined || containsNaN(step)) {
      /*const longitudeRange = extent[1][0] - extent[0][0];
      const latitudeRange = extent[1][1] - extent[0][1];
      step = [
        Math.ceil(longitudeRange / 10), // Approximate 10 steps
        Math.ceil(latitudeRange / 10),
      ];*/

      const longitudeRange = extent[1][0] - extent[0][0];
      const latitudeRange = extent[1][1] - extent[0][1];

      // Use d3.ticks to calculate "nice" step values for longitude and latitude
      const niceLongitudeTicks = ticks(extent[0][0], extent[1][0], 10); // Approx. 10 steps
      const niceLatitudeTicks = ticks(extent[0][1], extent[1][1], 10);

      step = [
        niceLongitudeTicks[1] - niceLongitudeTicks[0], // Step for longitude
        niceLatitudeTicks[1] - niceLatitudeTicks[0], // Step for latitude
      ];
    } // else step is undefined (Vega default is used) [10, 10]

    // Add graticule data to the spec
    const graticuleData = {
      name: graticuleName,
      transform: [
        {
          type: "graticule",
          step: step, // Longitude and latitude step
          extent: extent, // Optional extent of the graticule (e.g., [[-180, -90], [180, 90]])
          precision: precision, // Precision of the graticule
        },
      ],
    };

    // Check if graticule data already exists, replace it if found
    const existingDataIndex = spec.data.findIndex((d) => d.name === graticuleName);
    if (existingDataIndex !== -1) {
      spec.data[existingDataIndex] = graticuleData; // Update existing graticule data
    } else {
      spec.data.push(graticuleData); // Add new graticule data
    }

    // Add a graticule mark to the spec
    const graticuleMark = {
      name: `${graticuleName}_mark`,
      type: "shape",
      from: { data: graticuleName },
      encode: {
        update: {
          stroke: { value: stroke }, // Graticule line color
          strokeWidth: { value: strokeWidth }, // Graticule line width
          strokeOpacity: { value: strokeOpacity }, // Graticule line opacity
        },
      },
      transform: [
        {
          type: "geoshape",
          projection: customTransformName || projection, // Use custom or default projection
        },
      ],
    };

    // Check if graticule mark already exists, replace it if found
    const existingMarkIndex = spec.marks.findIndex((m) => m.name === `${graticuleName}_mark`);
    if (existingMarkIndex !== -1) {
      spec.marks[existingMarkIndex] = graticuleMark; // Update existing graticule mark
    } else {
      spec.marks.push(graticuleMark); // Add new graticule mark
    }
  }

  return spec;
}
