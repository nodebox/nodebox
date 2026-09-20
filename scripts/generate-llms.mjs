#!/usr/bin/env node
// This script combines all .JS files under packages/runtime/functions in order to feed them to LLMs.
//
import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const nodesRoot = path.resolve(__dirname, "../packages/runtime/functions");
const guidesRoot = path.resolve(__dirname, "../doc/guide");
const outputFile = path.resolve(__dirname, "../llms.txt");

function readGuides() {
  let combined = ``;
  const files = fs.readdirSync(guidesRoot);
  for (const file of files) {
    const fileName = path.join(guidesRoot, file);
    if (fs.statSync(fileName).isFile() && fileName.endsWith(".md")) {
      console.log(fileName);
      const source = fs.readFileSync(fileName, "utf8");
      combined += source;
    }
  }
  return combined;
}

function readPackage(name) {
  let combined = ``;
  const dirName = path.join(nodesRoot, name);
  const files = fs.readdirSync(dirName);
  for (const file of files) {
    const fileName = path.join(dirName, file);
    if (fs.statSync(fileName).isFile() && fileName.endsWith(".js")) {
      const source = fs.readFileSync(fileName, "utf8");
      const basename = path.parse(path.basename(fileName)).name;
      const nodeName = basename.replace(/-/g, " ").replace(/\b\w/g, (char) => char.toUpperCase());

      combined += `### Node: ${nodeName}\n\n`;
      combined += `\`\`\`js\n`;
      combined += source.replace(/`/g, "\\`");
      combined += `\`\`\`\n`;
      combined += "\n\n";
    }
  }
  return combined;
}

let allSource = `# NodeBox Live

This file describes the functions available in NodeBox Live. NodeBox Live is a node-based visual editor for data visualisation written in
TypeScript, React and Tailwind. We use D3 and Vega for the rendering.

Nodes have input and output ports of the following types:
- shape: a geometric shape, like a line, ellipse, text, path
- table: a table of data (an array of objects)
- spec: a Vega spec Most data visualization nodes will take in and return a spec.

Ports and parameters are created using in/out functions
- numberIn({ name: string; value: LiteralValue | undefined; min?: number; max?: number; step?: number; }): a number
- stringIn{ name: string; value: LiteralValue | undefined; widget?: WidgetType; choices?: ParameterChoices; }
- booleanIn({ name: string; value: LiteralValue | undefined; }): boolean parameter
- colorIn({ name: string; value: LiteralValue | undefined; }): a color parameter
- fileIn({ name: string; value: LiteralValue | undefined; }): a file parameter, represented internally as a string of the filename
- tableIn({ name: string; value: LiteralValue | undefined; }): a table port, displayed as a input port on the node
- shapeIn({ name: string; value: LiteralValue | undefined; }): a shape port, displayed as a input port on the node
- specIn({ name: string; value: LiteralValue | undefined; }): a Vega spec port, displayed as a input port on the node
- tableOut({ name: string }): a table port, displayed as an output port on the node
- shapeOut({ name: string }): a shape port, displayed as an output port on the node
- specOut({ name: string }): a Vega spec port, displayed as an output port on the node

Choices can be specified as a list of strings, which are then used as key and labels:

\`\`\`js
["red", "green", "blue"]
\`\`\`

or as a list of lists, where the first element is the key and the second element is the label:

\`\`\`js
[["red", "Red"], ["green", "Green"], ["blue", "Blue"]]
\`\`\`

## Guide

${readGuides()}

## Nodes

${readPackage("g")}
${readPackage("plot")}
`;
fs.writeFileSync(outputFile, allSource);
