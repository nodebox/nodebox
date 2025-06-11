/*
 * Utility functions and variables for g.
 */

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

/*
-------- HELPER FUNCTIONS FOR NESTED DATA --------
*/

// Helper function to get nested properties using a string path.
export function getNestedProperty(obj, path) {
  if (obj === null || typeof obj !== "object") {
    return undefined;
  }

  if (Object.prototype.hasOwnProperty.call(obj, path)) {
    return obj[path];
  }

  const keys = path.split(".");
  let current = obj;

  for (const key of keys) {
    if (current === null || typeof current !== "object") {
      return undefined;
    }
    current = current[key];
  }

  return current;
}

// Helper function to extract core data from nested data formats
export function extractCoreData(data, format, feature, properties) {
  const detectedFormat = detectDataFormat(data);
  const specifiedFormat = format === "" ? undefined : format;
  let jsonCore = structuredClone(data);
  if (!detectedFormat.startsWith(specifiedFormat)) {
    console.warn(`Detected format is ${detectedFormat}, not ${specifiedFormat}`);
    return jsonCore;
  }
  if (detectedFormat === "geojson featurecollection") {
    if (Array.isArray(jsonCore)) {
      if (jsonCore.length === 1) {
        jsonCore = jsonCore[0];
      } else {
        return jsonCore;
      }
    }
    if (properties) {
      // use properties as core
      jsonCore = jsonCore.features.map((d) => ({ ...d.properties, id: d.id }));
    } else {
      jsonCore = jsonCore.features.map((d) => ({ ...d, id: d.id }));
    }
  } else if (detectedFormat === "topojson topology") {
    if (Array.isArray(jsonCore)) {
      if (jsonCore.length === 1) {
        jsonCore = jsonCore[0];
      } else {
        return jsonCore;
      }
    }
    const featureKey = feature || Object.keys(jsonCore.objects)[0];
    const featureObj = jsonCore.objects[featureKey];
    const featureFormat = detectDataFormat(featureObj);
    if (["geojson geometrycollection", "topojson geometrycollection"].includes(featureFormat)) {
      if (properties) {
        // use properties as core
        jsonCore = featureObj.geometries.map((d) => ({ ...d.properties, id: d.id }));
      } else {
        jsonCore = featureObj.geometries.map((d) => ({ ...d, id: d.id }));
      }
    }
  }

  return jsonCore;
}

// Helper function to replace core data from nested data formats
export function replaceCoreData(data, format, feature, newCore, properties) {
  const detectedFormat = detectDataFormat(data);
  const specifiedFormat = format === "" ? undefined : format;
  let newData = structuredClone(data);
  if (!Array.isArray(newCore)) {
    console.warn("The new data is not an array. ");
    return newData;
  }
  if (!detectedFormat.startsWith(specifiedFormat)) {
    console.warn(`Detected format is ${detectedFormat}, not ${specifiedFormat}`);
    return newData;
  }
  // Create a lookup table from 'newCore'
  const newCore_lookup = new Map(newCore.map((item) => [item.id, item]));

  if (detectedFormat === "geojson featurecollection") {
    if (Array.isArray(newData)) {
      if (newData.length === 1) {
        newData = newData[0];
      } else {
        newData = structuredClone(newCore);
        return newData;
      }
    }
    if (properties) {
      // use properties as core
      newData.features = newData.features
        .map((f) => {
          const lookupFeature = newCore_lookup.get(f.id); // Find the corresponding value
          if (lookupFeature) {
            // Add the looked-up feature to `properties`
            return {
              ...f,
              properties: lookupFeature,
            };
          } else {
            return null; // Return null if no match found
          }
        })
        .filter(Boolean); // Remove features with no match (null values)
    } else {
      newData.features = newCore;
    }
    //delete newData.features.__id
  } else if (detectedFormat === "topojson topology") {
    if (Array.isArray(newData)) {
      if (newData.length === 1) {
        newData = newData[0];
      } else {
        newData = structuredClone(newCore);
        return newData;
      }
    }
    const featureKey = feature || Object.keys(newData.objects)[0];
    const featureObj = newData.objects[featureKey];
    const featureFormat = detectDataFormat(featureObj);
    if (["geojson geometrycollection", "topojson geometrycollection"].includes(featureFormat)) {
      if (properties) {
        // use properties as core
        featureObj.geometries = featureObj.geometries
          .map((f) => {
            const lookupFeature = newCore_lookup.get(f.id); // Find the corresponding value
            if (lookupFeature) {
              // Add the looked-up feature to `properties`
              return {
                ...f,
                properties: lookupFeature,
              };
            }
            return null; // Return null if no match found
          })
          .filter(Boolean); // Remove features with no match (null values)
      } else {
        featureObj.geometries = newCore;
      }
    }
  } else {
    newData = structuredClone(newCore);
  }

  return newData;
}

/*
-------- HELPER FUNCTIONS FOR DATA HANDLING --------
*/

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
    const type = dataObj.type;
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
