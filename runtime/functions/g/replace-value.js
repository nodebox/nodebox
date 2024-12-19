/**
 * Replace specific data values in the dataset.
 *
 * This node replaces the specified values in the input table by a given new value.
 * If no attribute name is specified, values will be replaced in all attributes.
 *
 * Values to replace:
 * - null: Represents an explicitly assigned absence of a value.
 * - undefined: Represents a missing value. The variable has not been assigned.
 *              In the raw data, the property is missing in the row object.
 * - NaN: Not-a-number. Represents an invalid numeric value in an attribute with variable type number.
 * - Empty: Values that represent 'nothing' but are valid attribute values.
 *          This includes empty strings (""), empty arrays ([]), or empty objects ({})
 * - <regex>: Uses the specified regular expression to match text to be replaced.
 * - <all>: Replaces all the above.
 *
 * Match inside:
 * - disabled: matching and replacing is performed on the attribute value as a whole.
 * - enabled: the node will also match and replace (multiple) parts inside text values (only with regex).
 *
 * @category Data
 */

import { fn2 } from "project:Utilities";

export default function (node) {
  const dataIn = node.tableIn({ name: "data", label: "Input Data" });

  // Parameters
  const attrNameIn = node.stringIn({
    name: "attrName",
    label: "Attributes to replace",
  });

  const matchTypeIn = node.stringIn({
    name: "matchType",
    label: "Values to replace",
    value: "<all>",
    choices: ["null", "undefined", "NaN", "empty", "<regex>", "<all>"],
  });

  const regexIn = node.stringIn({
    name: "regex",
    label: "Regular Expression",
  });

  const replaceTypeIn = node.stringIn({
    name: "replaceType",
    label: "New value type",
    value: "<value>",
    choices: ["<value>", "null", "undefined", "NaN"],
  });

  const replaceValueIn = node.stringIn({
    name: "replaceValue",
    label: "New value",
  });

  const matchInsideIn = node.booleanIn({
    name: "matchInside",
    label: "Match and replace inside text values (only with regex)",
    value: false,
  });

  const dataOut = node.tableOut({ name: "dataOut", label: "Output Data" });

  // Helper function to check if a value matches the replacement type
  function matchValue(value, matchType, regex) {
    const isNothingType =
      value === null ||
      value === undefined ||
      (typeof value === "number" && isNaN(value)) ||
      value === "" ||
      (Array.isArray(value) && value.length === 0) ||
      (typeof value === "object" && !Object.keys(value).length);

    switch (matchType) {
      case "null":
        return value === null;
      case "undefined":
        return value === undefined;
      case "NaN":
        return typeof value === "number" && isNaN(value);
      case "empty":
        return value === "" || (Array.isArray(value) && value.length === 0) || (typeof value === "object" && !value);
      case "<regex>":
        return regex ? regex.test(value) : false;
      case "<all>":
        return isNothingType || (regex && typeof value === "string" && regex.test(value));
      default:
        return false;
    }
  }

  // Helper function to determine the replacement value based on the type
  function setNewValue(newValueType, newValue) {
    switch (newValueType) {
      case "null":
        return null;
      case "undefined":
        return undefined;
      case "NaN":
        return NaN;
      case "<value>":
      default:
        return newValue;
    }
  }

  // Function to replace data values
  function replaceData(data, attributes, matchType, regex, replaceType, replaceValue, matchInside) {
    const setValue = setNewValue(replaceType, replaceValue);
    return data.map((row) => {
      const newRow = { ...row };
      const newValue = typeof setValue === "function" ? setValue(row) : setValue; //apply expression to datarow
      attributes.forEach((attr) => {
        if (newRow.hasOwnProperty(attr)) {
          const value = newRow[attr];

          // Match and replace text inside strings only if regex is provided and matchInside is enabled
          if (matchType === "<regex>" && regex && typeof value === "string" && matchInside && regex) {
            // Replace inside text
            newRow[attr] = value.replace(regex, newValue);
          } else if (
            matchType === "<all>" &&
            regex &&
            typeof value === "string" &&
            value !== null &&
            value !== undefined &&
            value != "" &&
            matchInside &&
            regex
          ) {
            // Replace inside text
            newRow[attr] = value.replace(regex, newValue);
          }

          // Replace the entire value if it matches base types or regex
          else if (matchValue(value, matchType, regex)) {
            newRow[attr] = newValue;
          }
        }
      });
      return newRow;
    });
  }

  // Main processing
  node.onRender = () => {
    const data = structuredClone(dataIn.value ? dataIn.value : []);
    const attributes =
      attrNameIn.value === undefined || attrNameIn.value === ""
        ? Object.keys(data[0] || {})
        : attrNameIn.value.split(",").map((attr) => attr.trim());

    const matchType = matchTypeIn.value;
    const regex = regexIn.value ? new RegExp(regexIn.value, "g") : null;
    const replaceType = replaceTypeIn.value;
    const replaceValue = fn2(replaceValueIn);
    const matchInside = matchInsideIn.value;

    // Validate regex for <regex> or <all> when regex is expected
    if ((matchType === "<regex>" || (matchType === "<all>" && regexIn.value)) && !regex) {
      throw new Error("The provide regular expression is not valid.");
    }

    const updatedData = replaceData(data, attributes, matchType, regex, replaceType, replaceValue, matchInside);
    dataOut.set(updatedData);
  };
}
