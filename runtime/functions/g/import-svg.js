/**
 * Load an uploaded CSV or JSON file and outputs a table.
 *
 * For a CSV file, the delimiter is a character that separates the values in the file. The most common
 * delimiters are commas (`,`), tabs (`\t`), and semicolons (`;`).
 *
 * @category Input/Output
 */

import { parseSVG } from "@ndbx/g";

export default function (node) {
  const fileIn = node.fileIn({ name: "file" });
  const shapeOut = node.shapeOut({ name: "out" });

  node.onRender = (cx) => {
    const [file] = [fileIn.value];

    if (file === "") {
      shapeOut.set([]);
      return;
    }

    const buffer = cx.assetMap.get(file);
    if (!buffer) {
      throw new Error(`File ${file} is not loaded.`);
    }
    let data;
    if (file.endsWith(".svg")) {
      data = parseSVG(buffer);
    } else {
      throw new Error("Unsupported file format:", file);
    }
    shapeOut.set(data);
  };
}
