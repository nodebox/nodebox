import { format } from "d3";
import { Context, FunctionItem, Project, PortType, ParameterType } from "../src";
import { createProject, createNetwork, createNode } from "../src/mutation";

export function createValueFn(): FunctionItem {
  const source = `

export default function(node) {
    const valueIn = node.numberIn({ name: "value" });
    const attributeIn = node.stringIn({ name: "attribute", value: "value" });
    const tableOut = node.tableOut({ name: "out" });

    node.onRender = () => {
        const table = [{ [attributeIn.value]: valueIn.value }];
        tableOut.set(table);
    };
}
`;

  return {
    type: "FUNCTION",
    id: "value",
    name: "value",
    category: "Math",
    description: "Generates a simple value",
    inputPorts: [],
    outputPorts: [{ type: PortType.Series, name: "out" }],
    parameters: [{ type: ParameterType.Number, name: "value", label: "Value", defaultValue: 0 }],
    source,
  };
}

export function createAddFn(): FunctionItem {
  const source = `
import { tableFromArrays, makeVector } from 'https://esm.sh/apache-arrow';

export default function(node) {
    const tableIn = node.tableIn({ name: "table" });
    const attribute1In = node.stringIn({ name: "attribute", defaultValue: "value" });
    const attribute2In = node.stringIn({ name: "attribute", defaultValue: "value" });
    const targetAttributeIn = node.stringIn({ name: "targetAttribute", defaultValue: "value" });
    const tableOut = node.tableOut({ name: "out" });

    node.onRender = () => {
        const [table, attribute1, attribute2, targetAttribute] = [tableIn.value, attribute1In.value, attribute2In.value, targetAttributeIn.value];
        const vector1 = table.getChild(attribute1);
        const vector2 = table.getChild(attribute2);
        const vectorOut = makeVector(new Float64Array(table.numRows));

        for (let i = 0; i < table.numRows; i++) {
            const v1 = i < vector1.length ? vector1.get(i) : null;
            const v2 = i < vector2.length ? vector2.get(i) : null;
            if (typeof v1 !== "number" || typeof v2 !== "number") {
                vectorOut.set(i, null);
            } else {
                vectorOut.set(i, v1 + v2);
            }
        }

        const tableMap = table.schema.fields.reduce((acc, field) => {
            acc[field.name] = table.getChild(field.name);
            return acc;
        }, {});
        tableMap[targetAttribute] = vectorOut;
        tableOut.set(makeTable(tableMap));
    };
}
`;

  return {
    type: "FUNCTION",
    id: "add",
    name: "add",
    category: "Math",
    description: "Adds two columns",
    inputPorts: [{ type: PortType.Table, name: "table" }],
    outputPorts: [{ type: PortType.Table, name: "out" }],
    parameters: [
      { type: ParameterType.String, name: "attribute1", label: "Attribute 1", defaultValue: "value" },
      { type: ParameterType.String, name: "attribute2", label: "Attribute 2", defaultValue: "value" },
      { type: ParameterType.String, name: "targetAttribute", label: "Target Attribute", defaultValue: "value" },
    ],
    source,
  };
}

export function createNegateFn(): FunctionItem {
  const source = `
export default function(node) {
    const tableIn = node.tableIn({ name: "table" });
    const attributeIn = node.stringIn({ name: "attribute", value: "value" });
    const targetAttributeIn = node.stringIn({ name: "targetAttribute", value: "value" });
    const tableOut = node.tableOut({ name: "out" });

    node.onRender = () => {
        const [table, attribute, targetAttribute] = [tableIn.value, attributeIn.value, targetAttributeIn.value];
        const newTable = [];

        for (let i = 0; i < table.length; i++) {
            const row = table[i];
            const v = row[attribute];
            if (typeof v === "number") {
                newTable.push({...row, [targetAttribute]: -v});
            } else {
                newTable.push({...row, [targetAttribute]: null});
            }
        }
        tableOut.set(newTable);
    };
}
`;

  return {
    type: "FUNCTION",
    id: "negate",
    name: "negate",
    category: "Math",
    description: "Negates a column",
    inputPorts: [{ type: PortType.Table, name: "table" }],
    outputPorts: [{ type: PortType.Table, name: "out" }],
    parameters: [
      { type: ParameterType.String, name: "attribute", label: "Attribute", defaultValue: "value" },
      { type: ParameterType.String, name: "targetAttribute", label: "Target Attribute", defaultValue: "value" },
    ],
    source,
  };
}

export function createMakeNumbersFn(): FunctionItem {
  const source = `
import { tableFromArrays, makeVector } from 'https://esm.sh/apache-arrow';

export default function(node) {
    const templateIn = node.stringIn({ name: "template", defaultValue: "11;22;33" });
    const targetAttributeIn = node.stringIn({ name: "targetAttribute", defaultValue: "value" });
    const tableOut = node.tableOut({ name: "out" });

    node.onRender = () => {
        const [template, targetAttribute] = [templateIn.value, targetAttributeIn.value];
        const array = template.split(";").map(x => parseFloat(x));
        const table = tableFromArrays({ [targetAttribute]: array });
        tableOut.set(table);
    };
}
`;

  return {
    type: "FUNCTION",
    id: "make-numbers",
    name: "make-numbers",
    category: "Math",
    description: "Parse numbers from a string",
    inputPorts: [],
    outputPorts: [{ type: PortType.Table, name: "out" }],
    parameters: [
      { type: ParameterType.String, name: "template", label: "Template", defaultValue: "11;22;33" },
      { type: ParameterType.String, name: "targetAttribute", label: "Target Attribute", defaultValue: "value" },
    ],
    source,
  };
}

export function createMathProject(): Project {
  const project = createProject("math");
  project.items.push(createAddFn());
  project.items.push(createNegateFn());
  project.items.push(createValueFn());
  project.items.push(createMakeNumbersFn());
  return project;
}

export function createTestContext() {
  const project = createProject("test");
  const cx = new Context(project, new Map(), new Map([["test/math", createMathProject()]]));
  const network = createNetwork(cx, project, "test");
  //   plan = createNetwork(plan, "test");
  //   plan = createNode(plan, "test", "math.add");
  //   for (const project of plan.dependencies) {
  //     project.functions.forEach((fn) => loadFunction(plan, project, fn));
  //   }
  return { cx, project, network };
}
