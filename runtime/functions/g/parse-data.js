/**
 * Parse data and outputs a table.
 *
 * This node provides a text field where you can input data in either CSV or JSON format. The data will be parsed and converted into a table.
 *
 * @category Input/Output
 */

import { dsvFormat, autoType } from "https://esm.sh/d3-dsv@3.0.1";

export default function (node) {
  const tableIn = node.tableIn({ name: "table" });
  const formatIn = node.stringIn({
    name: "format",
    value: "csv",
    choices: [
      ["csv", "CSV"],
      ["json", "JSON"],
    ],
  });
  const delimiterIn = node.stringIn({ name: "delimiter", value: "," });
  const decimalSeparatorIn = node.stringIn({ name: "decimal separator", value: "." });
  const sourceIn = node.stringIn({ name: "source", widget: "TEXT" });
  const tableOut = node.tableOut({ name: "out" });

  function parseData(data) {
    const format = formatIn.value;
    const delimiter = delimiterIn.value;
    const decimalSeparator = decimalSeparatorIn.value;

    if (data.trim() === "") {
      return [];
    }

    if (format === "csv") {
      const parser = dsvFormat(delimiter);
      return parser.parse(data, (row) => {
        for (const key in row) {
          if (typeof row[key] === "string" && decimalSeparator !== ".") {
            row[key] = row[key].replace(new RegExp(`\\${decimalSeparator}`, "g"), ".");
          }
        }
        return autoType(row);
      });
    } else if (format === "json") {
      try {
        const parsedData = JSON.parse(data);
        if (Array.isArray(parsedData) && parsedData.every((item) => typeof item === "object")) {
          return parsedData;
        }
        throw new Error("Invalid JSON format for table output");
      } catch (error) {
        console.error("Error parsing JSON data:", error);
        return [];
      }
    }
    console.error("Unsupported format:", format);
    return [];
  }

  node.onRender = async () => {
    if (tableIn.value) {
      if (Array.isArray(tableIn.value)) {
        tableOut.set(tableIn.value);
        return;
      } else if (typeof tableIn.value === "string") {
        tableOut.set(parseData(tableIn.value));
        return;
      }
    }

    tableOut.set(parseData(sourceIn.value));
  };
}
