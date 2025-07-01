/**
 * Combines the records of two datasets based on common attributes.
 *
 * The join parameter defines the type of combination of the two datasets.
 *
 * - inner: Returns only records that have matching values in both datasets.
 * - left outer: Returns all records from the left dataset, and the matched records from the right dataset.
 * - right outer: Returns all records from the right dataset, and the matched records from the left dataset.
 * - full outer: Returns all matched and not-matched and unmatched records of both left and right datasets.
 * - cross: Matches each record of the right dataset with each record of the left dataset, regardless of the matching keys.
 *
 * The optional select parameter allows to pass a JavaScript function
 * to define which attributes (or derivatives) to keep in the resulting joined dataset.
 *
 * @category Data Manipulation
 */

import { detectDataFormat, getNestedProperty, extractCoreData, replaceCoreData } from "project:Utilities";

export default function (node) {
  node.pushSection({ name: "General" });
  const leftIn = node.tableIn({ name: "dataLeft", label: "Left data" });
  const rightIn = node.tableIn({ name: "dataRight", label: "Right data" });
  const modeIn = node.stringIn({
    name: "mode",
    label: "Mode",
    value: "inner",
    choices: ["inner", "left outer", "right outer", "full outer", "cross"],
  });
  const lkeyIn = node.stringIn({ name: "leftKey", label: "Left key(s)" });
  const rkeyIn = node.stringIn({ name: "rightKey", label: "Right key(s)" });
  const selectIn = node.stringIn({ name: "select", label: "Select", widget: "TEXT", value: "return { ...d };" });
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

  function joinData(left, right, lkey, rkey, mode, selectFn) {
    const output = [];
    if (!Array.isArray(left)) {
      throw new Error(`Left data is not an array. Detected format is ${detectDataFormat(left)}.`);
    }
    if (!Array.isArray(right)) {
      throw new Error(`Right data is not an array. Detected format is ${detectDataFormat(right)}.`);
    }

    if (mode === "cross") {
      left.forEach((leftRow) => {
        right.forEach((rightRow) => {
          const joinedRow = { ...leftRow, ...rightRow };
          output.push(selectFn(joinedRow));
        });
      });
    } else {
      const leftKeyArray = lkey.split(",").map((d) => d.trim());
      const rightKeyArray = rkey.split(",").map((d) => d.trim());

      // Helper to generate a composite key from a row. If any key is missing (undefined or null),
      // we return null so that this row is treated as "unmatched" rather than producing the string
      // "undefined" and accidentally colliding with other rows that miss the same field.
      const keyFn = (row, keys) => {
        const values = keys.map((k) => getNestedProperty(row, k));
        return values.some((v) => v === undefined || v === null) ? null : values.join("|");
      };

      // Build lookup table for the *right* dataset. Rows that have an invalid (null) key are not
      // placed in the lookup to avoid unintended matches, but they can still be emitted later for
      // right/full outer joins.
      const lookup = new Map();
      const unmatchedRight = [];
      right.forEach((row) => {
        const key = keyFn(row, rightKeyArray);
        if (key === null) {
          unmatchedRight.push(row);
          return;
        }
        if (!lookup.has(key)) {
          lookup.set(key, []);
        }
        lookup.get(key).push(row);
      });

      // Process the *left* dataset and join where possible.
      left.forEach((leftRow) => {
        const key = keyFn(leftRow, leftKeyArray);
        const rightRows = key !== null ? lookup.get(key) || [] : [];

        if (rightRows.length > 0) {
          rightRows.forEach((rightRow) => {
            const joinedRow = { ...leftRow, ...rightRow };
            output.push(selectFn(joinedRow));
          });
        } else if (mode === "left outer" || mode === "full outer") {
          // No match found – keep the left row as-is (outer-join behaviour)
          output.push(selectFn({ ...leftRow }));
        }
      });

      // Add rows from the right side that never matched a left row (right/full outer).
      if (mode === "right outer" || mode === "full outer") {
        // Rows that lacked a valid key were already collected in `unmatchedRight`.
        const rightCandidates = [...unmatchedRight];

        // Also include rows with a key that is not present in any left row.
        lookup.forEach((rows, key) => {
          const hasMatchInLeft = left.some((leftRow) => keyFn(leftRow, leftKeyArray) === key);
          if (!hasMatchInLeft) {
            rightCandidates.push(...rows);
          }
        });

        rightCandidates.forEach((row) => output.push(selectFn({ ...row })));
      }
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
    const mode = modeIn.value;
    const lkey = lkeyIn.value;
    const rkey = rkeyIn.value;

    if (!left || !right) {
      dataOut.set([]);
      return;
    } else if (mode != "cross" && (!lkey || !rkey)) {
      dataOut.set([]);
      return;
    }

    const selectFn = new Function("d", selectIn.value);
    const joined = joinData(left, right, lkey, rkey, mode, selectFn);
    const newLeft = replaceCoreData(leftData, leftFormat, leftFeature, joined, leftPropertiesIn.value);

    dataOut.set(newLeft);
  };
}
