import { describe, it, expect } from "vitest";
import { updateFormatVersion, LATEST_FORMAT_VERSION } from "../src/upgrades";
import { findItemByName } from "../src/queries";
import { Project, Network, Node } from "../src/types";

// Define a sample project for testing.
const sampleProject: Project = {
  title: "Welcome - Getting Started",
  color: "teal",
  dependencies: {
    "core/g": "dev",
  },
  assets: {},
  items: [
    {
      type: "NETWORK",
      id: "0:1",
      name: "Main",
      children: [
        {
          id: "0:2",
          type: "NODE",
          x: 95,
          y: 213,
          name: "rectangle",
          fn: "core/g/rect",
          values: {
            x: {
              type: "EXPRESSION",
              expression: "value",
            },
            y: {
              type: "EXPRESSION",
              expression: "value",
            },
            width: {
              type: "VALUE",
              value: 10,
            },
            height: {
              type: "VALUE",
              value: 10,
            },
            fill: {
              type: "VALUE",
              value: {
                r: 0.9529411764705882,
                g: 0.023529411764705882,
                b: 0.023529411764705882,
                a: 1,
              },
            },
            stroke: {
              type: "VALUE",
              value: {
                r: 1,
                g: 1,
                b: 1,
                a: 1,
              },
            },
          },
        },
        {
          type: "NODE",
          id: "eM0brS62USj:0",
          x: 95,
          y: 63,
          name: "sample data",
          fn: "core/g/sample",
          values: {
            min: {
              type: "VALUE",
              value: 0,
            },
            max: {
              type: "VALUE",
              value: 100,
            },
            amount: {
              type: "VALUE",
              value: 11,
            },
          },
        },
        {
          type: "NODE",
          id: "eM0brS62USj:1",
          x: 225,
          y: 63,
          name: "load-csv 1",
          fn: "core/g/load-csv",
          values: {
            file: {
              type: "VALUE",
              value: "",
            },
          },
        },
        {
          type: "STICKY",
          id: "0:5:sticky",
          x: 275,
          y: 163,
          width: 100,
          height: 100,
          backgroundColor: {
            r: 0,
            g: 1,
            b: 1,
            a: 0,
          },
          text: "Hello world!",
          fontSize: 12,
        },
        {
          type: "NODE",
          id: "eNCKLaCwv9d:0",
          x: 85,
          y: 343,
          name: "sample 1",
          fn: "core/g/sample",
          values: {},
        },
      ],
      connections: [
        {
          type: "NODE_TO_NODE",
          outNode: "eM0brS62USj:0",
          outPort: "out",
          inNode: "0:2",
          inPort: "table",
        },
      ],
      stickies: [
        {
          x: 15,
          y: 95,
          width: 275,
          height: 35,
          text: "Click the live1 node and change the orderBy method from X to Y ( or to Angle / Distance).",
        },
      ],
      renderedNode: "eM0brS62USj:1",
    },
    {
      type: "FUNCTION",
      id: "eNCKLaCwv9d:1",
      name: "f01",
      category: "",
      description: "",
      inputPorts: [],
      outputPorts: [],
      parameters: [],
      source:
        '/**\n * Function Template\n *\n * Use this as a demonstration of a function.\n * @category Graphics\n */\n\nimport { Rect, Paint } from "@ndbx/g";\n\nexport default function (node) {\n  const shapeOut = node.shapeOut({ name: "Out" });\n\n  node.onRender = () => {\n    const rect = new Rect(0, 0, 100, 100);\n    rect.fill = Paint.solid(1, 0, 0);\n    shapeOut.set(rect);\n  };\n}\n',
    },
  ],
  id: "welcome",
  formatVersion: 0,
};

function _findNodesByFn(project: Project, fn: string): Node[] {
  const nodes: Node[] = [];
  for (const network of project.items) {
    if (network.type === "NETWORK") {
      for (const child of network.children) {
        if (child.type === "NODE" && (child as Node).fn === fn) {
          nodes.push(child as Node);
        }
      }
    }
  }
  return nodes;
}

describe("updateFormatVersion", () => {
  it("should not modify a project with the latest format version", () => {
    const project = { ...sampleProject, formatVersion: LATEST_FORMAT_VERSION };
    const updatedProject = updateFormatVersion(project);
    expect(updatedProject).toEqual(project);
  });

  it("should initialize format version to latest version if undefined", () => {
    const project: unknown = { ...sampleProject, formatVersion: undefined };
    const updatedProject = updateFormatVersion(project as Project);
    expect(updatedProject.formatVersion).toBe(LATEST_FORMAT_VERSION);
  });

  it("should throw an error if the project version is newer than supported", () => {
    const project: Project = { ...sampleProject, formatVersion: LATEST_FORMAT_VERSION + 1 };
    expect(() => updateFormatVersion(project)).toThrow(
      "Invalid project format version. You might want to retry later on.",
    );
  });

  it("should update format version and change name for fn: 'core/g/load-csv' for version 2", () => {
    const project: Project = { ...sampleProject };
    const updatedProject = updateFormatVersion(project, 2);
    expect(updatedProject.formatVersion).toBe(2);
    const mainNetwork = findItemByName(updatedProject, "Main") as Network;
    expect(mainNetwork).toBeDefined();
    const loadCsvNodes = _findNodesByFn(updatedProject, "core/g/load-csv");
    expect(loadCsvNodes.length).toBe(0);
    const importDataNodes = _findNodesByFn(updatedProject, "core/g/Import Data");
    expect(importDataNodes.length).toBe(1);
    expect(importDataNodes[0].name).toBe("Import Data 1");
  });
});
