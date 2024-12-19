/**
 * Joins two datasets based on the index (row number) of each record.
 *
 * This node provides several options for handling datasets of unequal lengths:
 * - Shortest: The output is truncated to the length of the shorter dataset.
 * - Longest: The output extends to the length of the longer dataset, with empty values filling in for the shorter one.
 * - Cycle: The shorter dataset is repeated cyclically to match the length of the longer one.
 * - Extend last: The last row of the shorter dataset is repeated to match the length of the longer one.
 *
 * When columns with the same name exist in both datasets, you can choose to:
 * - Prefix: Add a prefix (e.g., 'left_' or 'right_') to the column names to avoid conflicts.
 * - Overwrite: Allow the right dataset's columns to overwrite those in the left dataset.
 * - Exclude: Choose which dataset's properties to keep in case of naming conflicts.
 *
 * @category Data Manipulation
 */
import { detectDataFormat, extractCoreData, replaceCoreData } from "project:Utilities";

export default function (node) {
  node.pushSection({ name: "General" });
  const leftIn = node.tableIn({ name: "dataLeft", label: "Left data" });
  const rightIn = node.tableIn({ name: "dataRight", label: "Right data" });
  const lengthModeIn = node.stringIn({
    name: "lengthMode",
    label: "Length mode",
    value: "shortest",
    choices: ["shortest", "longest", "cycle", "extend last"],
  });
  const conflictModeIn = node.stringIn({
    name: "conflictMode",
    label: "Conflict mode",
    value: "prefix",
    choices: ["prefix", "overwrite", "exclude"],
  });
  const leftPrefixIn = node.stringIn({
    name: "leftPrefix",
    label: "Left prefix",
    value: "left_",
  });
  const rightPrefixIn = node.stringIn({
    name: "rightPrefix",
    label: "Right prefix",
    value: "right_",
  });
  const excludeSourceIn = node.stringIn({
    name: "excludeSource",
    label: "Exclude source",
    value: "right",
    choices: ["left", "right"],
  });
  node.popSection();
  node.pushSection({ name: "Data formats", collapsed: true });
  const leftFormatIn = node.stringIn({
    name: "leftFormat",
    label: "Left format",
    value: "json",
    choices: [
      ["json", "<default>"],
      ["geojson", "GeoJSON"],
      ["topojson", "TopoJSON"],
    ],
  });
  const leftFeatureIn = node.stringIn({
    name: "leftFeature",
    label: "Left feature",
  });
  const leftPropertiesIn = node.booleanIn({
    name: "leftProperties",
    label: "Use properties (left)",
    value: true,
  });
  const rightFormatIn = node.stringIn({
    name: "rightFormat",
    label: "Right format",
    value: "json",
    choices: [
      ["json", "<default>"],
      ["geojson", "GeoJSON"],
      ["topojson", "TopoJSON"],
    ],
  });
  const rightFeatureIn = node.stringIn({
    name: "rightFeature",
    label: "Right feature",
  });
  const rightPropertiesIn = node.booleanIn({
    name: "rightProperties",
    label: "Use properties (right)",
    value: true,
  });
  node.popSection();

  const dataOut = node.tableOut({ name: "dataOut", label: "Data" });

  function joinByIndex(left, right, lengthMode, conflictMode, leftPrefix, rightPrefix, excludeSource) {
    const maxLength = Math.max(left.length, right.length);
    const minLength = Math.min(left.length, right.length);
    const output = [];

    for (let i = 0; i < maxLength; i++) {
      if (lengthMode === "shortest" && i >= minLength) {
        break;
      }

      let leftRow = left[i];
      let rightRow = right[i];

      if (i >= left.length) {
        if (lengthMode === "cycle") {
          leftRow = left[i % minLength];
        } else if (lengthMode === "extend last") {
          leftRow = left[left.length - 1];
        } else {
          leftRow = {};
        }
      }

      if (i >= right.length) {
        if (lengthMode === "cycle") {
          rightRow = right[i % minLength];
        } else if (lengthMode === "extend last") {
          rightRow = right[right.length - 1];
        } else {
          rightRow = {};
        }
      }

      let joinedRow = {};
      const allKeys = new Set([...Object.keys(leftRow), ...Object.keys(rightRow)]);

      for (const key of allKeys) {
        const leftHasKey = key in leftRow;
        const rightHasKey = key in rightRow;

        if (leftHasKey && rightHasKey) {
          if (conflictMode === "prefix") {
            joinedRow[`${leftPrefix}${key}`] = leftRow[key];
            joinedRow[`${rightPrefix}${key}`] = rightRow[key];
          } else if (conflictMode === "overwrite") {
            joinedRow[key] = rightRow[key];
          } else if (conflictMode === "exclude") {
            if (excludeSource === "left") {
              joinedRow[key] = rightRow[key];
            } else {
              joinedRow[key] = leftRow[key];
            }
          }
        } else if (leftHasKey) {
          joinedRow[key] = leftRow[key];
        } else if (rightHasKey) {
          joinedRow[key] = rightRow[key];
        }
      }
      output.push(joinedRow);
    }

    return output;
  }

  node.onRender = async () => {
    const leftData = structuredClone(leftIn.value);
    const rightData = structuredClone(rightIn.value);
    const leftFeature = leftFeatureIn.value === "" ? undefined : leftFeatureIn.value;
    const leftFormat = leftFormatIn.value === "" ? undefined : leftFormatIn.value;
    const rightFeature = rightFeatureIn.value === "" ? undefined : rightFeatureIn.value;
    const rightFormat = rightFormatIn.value === "" ? undefined : rightFormatIn.value;
    const left = extractCoreData(leftData, leftFormat, leftFeature, leftPropertiesIn.value);
    const right = extractCoreData(rightData, rightFormat, rightFeature, rightPropertiesIn.value);
    const lengthMode = lengthModeIn.value;
    const conflictMode = conflictModeIn.value;
    const leftPrefix = leftPrefixIn.value;
    const rightPrefix = rightPrefixIn.value;
    const excludeSource = excludeSourceIn.value;

    if (!left || !right) {
      dataOut.set([]);
      return;
    }

    const joined = joinByIndex(left, right, lengthMode, conflictMode, leftPrefix, rightPrefix, excludeSource);
    const newLeft = replaceCoreData(leftData, leftFormat, leftFeature, joined, leftPropertiesIn.value);
    dataOut.set(newLeft);
  };
}
