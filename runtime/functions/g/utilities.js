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
  // Create a lookup table from 'newCore' using their explicit `id` when present.
  // We **do not** include items whose `id` is `undefined` so we can fall back to
  // a positional match later. This prevents all "id-less" items from ending up
  // under the same key and overwriting each other.
  const newCore_lookup = new Map();
  newCore.forEach((item) => {
    if (item && item.id !== undefined && item.id !== null) {
      newCore_lookup.set(item.id, item);
    }
  });

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
        .map((f, idx) => {
          // Try lookup by explicit `id` first, then fall back to positional index.
          const lookupFeature = (f.id !== undefined && newCore_lookup.get(f.id)) || newCore[idx];
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
          .map((f, idx) => {
            // Try lookup by explicit `id` first, then fall back to positional index.
            const lookupFeature = (f.id !== undefined && newCore_lookup.get(f.id)) || newCore[idx];
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

// Function to detect or validate data format (json, geojson, topojson)
export function detectDataFormat(data) {
  if (!data) return undefined;

  let dataObj = data;
  // Handle cases where data might be an array containing a single relevant object
  if (Array.isArray(data)) {
    if (data.length === 1 && typeof data[0] === "object" && data[0] !== null) {
      dataObj = data[0];
    } else {
      // Multi-element arrays, empty arrays, or arrays with non-object single elements are plain JSON
      return "json";
    }
  }

  if (typeof dataObj !== "object" || dataObj === null || typeof dataObj.type !== "string") {
    // Not an object, null, or doesn't have a 'type' string: consider it plain JSON
    return "json";
  }

  const { type, features, objects, arcs, geometries } = dataObj;

  // Check for GeoJSON FeatureCollection
  if (type === "FeatureCollection" && Array.isArray(features)) {
    return "geojson featurecollection";
  }

  // Check for TopoJSON Topology
  if (type === "Topology" && typeof objects === "object" && Array.isArray(arcs)) {
    return "topojson topology";
  }

  // Check for GeometryCollection (often found within TopoJSON objects or as standalone GeoJSON)
  // This is important because `featureObj` in your other functions can be a GeometryCollection.
  if (type === "GeometryCollection" && Array.isArray(geometries)) {
    // If any internal geometry uses 'arcs', it's likely a TopoJSON-style GeometryCollection
    if (geometries.some((g) => g && typeof g === "object" && Array.isArray(g.arcs))) {
      return "topojson geometrycollection";
    }
    // Otherwise, assume it's a GeoJSON-style GeometryCollection
    return "geojson geometrycollection";
  }

  // Default to JSON if no specific collection format is confidently detected
  return "json";
}
