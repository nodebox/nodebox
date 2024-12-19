/**
 * Fetch a CSV or JSON file and outputs a table.
 *
 * This node fetches a file from a URL and outputs a table. This file would typically be
 * hosted on a public URL.
 *
 * For a CSV file, the delimiter is a character that separates the values in the file. The most common
 * delimiters are commas (`,`), tabs (`\t`), and semicolons (`;`).
 *
 * @category Input/Output
 */
import { dsvFormat, autoType } from "https://esm.sh/d3-dsv@3.0.1";

export default function (node) {
  const urlIn = node.stringIn({ name: "url", value: "https://data.nodebox.live/iris.csv" });
  const delimiterIn = node.stringIn({ name: "delimiter", value: "," });
  const decimalSeparatorIn = node.stringIn({ name: "decimal separator", value: "." });
  const tableOut = node.tableOut({ name: "out" });

  node.onRender = async () => {
    if (urlIn.value === "") {
      tableOut.set([]);
      return;
    }

    const res = await fetch(urlIn.value);
    const contentType = res.headers.get("Content-Type");

    let data;
    if (contentType.includes("application/json")) {
      data = await res.json();
      // Ensure parsedData is an array of objects to fit table format
      if (Array.isArray(data) && data.every((item) => typeof item === "object")) {
        tableOut.set(data);
      } else if ([data].every((item) => typeof item === "object")) {
        tableOut.set([data]);
      } else {
        console.error("Invalid JSON format for table output");
        tableOut.set([]);
      }
    } else if (contentType.includes("text/csv") || contentType.includes("application/csv")) {
      const csv = await res.text();
      const parser = dsvFormat(delimiterIn.value);
      data = parser.parse(csv, (row) => {
        for (const key in row) {
          if (typeof row[key] === "string" && decimalSeparatorIn.value !== ".") {
            // Replace decimal separator with a dot (.)
            row[key] = row[key].replace(new RegExp(`\\${decimalSeparatorIn.value}`, "g"), ".");
          }
        }
        return autoType(row);
      });

      tableOut.set(data);
    } else {
      console.error("Unsupported content type:", contentType);
      tableOut.set([]);
    }
  };
}
