/**
 * Load an uploaded CSV or JSON file and outputs a table.
 *
 * For a CSV file, the delimiter is a character that separates the values in the file. The most common
 * delimiters are commas (`,`), tabs (`\t`), and semicolons (`;`).
 *
 * @category Input/Output
 */
import { dsvFormat, autoType } from "https://esm.sh/d3-dsv@3.0.1";

export default function (node) {
  const fileIn = node.fileIn({ name: "file" });
  const delimiterIn = node.stringIn({ name: "delimiter", value: "," });
  const decimalSeparatorIn = node.stringIn({ name: "decimal separator", value: "." });
  const tableOut = node.tableOut({ name: "out" });

  node.onRender = (cx) => {
    const [file, delimiter, decimalSeparator] = [fileIn.value, delimiterIn.value, decimalSeparatorIn.value];

    if (file === "") {
      tableOut.set([]);
      return;
    }

    const buffer = cx.assetMap.get(file);
    if (!buffer) {
      throw new Error(`File ${file} is not loaded.`);
    }
    let data;
    if (file.endsWith(".csv")) {
      const parser = dsvFormat(delimiter);
      data = parser.parse(buffer, (row) => {
        for (const key in row) {
          if (typeof row[key] === "string" && decimalSeparator !== ".") {
            // Convert the decimal separator to a dot (.) for numeric fields
            row[key] = row[key].replace(new RegExp(`\\${decimalSeparator}`, "g"), ".");
          }
        }
        return autoType(row);
      });
    } else if (file.endsWith(".json") || file.endsWith(".geojson") || file.endsWith(".topojson")) {
      data = JSON.parse(buffer);
    } else {
      throw new Error("Unsupported file format:", file);
    }
    tableOut.set(data);
  };
}
