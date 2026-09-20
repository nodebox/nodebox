import { expect, it } from "vitest";
import { findNodeStatements } from "../src/lexer";

function _find(s: string): string[] {
  const src = `export default function (node) {\n${s}\n}`;
  return findNodeStatements(src);
}

it("can find regular node statements", () => {
  const stmts = _find(`const xIn = node.numberIn({ name: "x" });`);
  expect(stmts).toEqual(['node.numberIn({ name: "x" })']);
});

it("ignores node statements wrapped in a line comment", () => {
  const stmts = _find(`// const xIn = node.numberIn({ name: "x" });`);
  expect(stmts).toEqual([]);
});

it("ignores node statements wrapped in a block comment", () => {
  const stmts1 = _find(`/* const xIn = node.numberIn({ name: "x" }); */`);
  expect(stmts1).toEqual([]);
  const stmts2 = _find(`/* const xIn = node.numberIn({ name: "x" }); */\nconst yIn = node.numberIn({ name: "y" });`);
  expect(stmts2).toEqual(['node.numberIn({ name: "y" })']);
});

it("supports longer statements", () => {
  const source = `
/**
 * Translate, rotate and/or scale a set of shapes.
 *
 * This node takes shapes and applies a transformation to each shape.
 * The transformation is defined by the translation, rotation and scale parameters.
 */

import { Transform } from "@ndbx/g";

export default function (node) {
  const shapeIn = node.shapeIn({ name: "shapes" });
  const txIn = node.numberIn({ name: "translate x", value: 0 });
  const tyIn = node.numberIn({ name: "translate y", value: 0 });
  const rotateIn = node.numberIn({ name: "rotate", value: 0 });
  const sxIn = node.numberIn({ name: "scale x", value: 1, step: 0.01 });
  const syIn = node.numberIn({ name: "scale y", value: 1, step: 0.01 });
  const shapeOut = node.shapeOut({ name: "out" });

  node.onRender = () => {
    const [tx, ty, r, sx, sy] = [txIn.value, tyIn.value, rotateIn.value, sxIn.value, syIn.value];
    const shape = shapeIn.value;
    if (!shape) {
      shapeOut.set([]);
      return;
    }
    const transform = new Transform();
    transform
      .scale(sx, sy)
      .rotate((r * Math.PI) / 180)
      .translate(tx, ty);
    const newShape = shape.clone();
    newShape.applyTransform(transform);
    shapeOut.set(newShape);
  };
}
"`;
  const stmts = findNodeStatements(source);
  expect(stmts).toEqual([
    'node.shapeIn({ name: "shapes" })',
    'node.numberIn({ name: "translate x", value: 0 })',
    'node.numberIn({ name: "translate y", value: 0 })',
    'node.numberIn({ name: "rotate", value: 0 })',
    'node.numberIn({ name: "scale x", value: 1, step: 0.01 })',
    'node.numberIn({ name: "scale y", value: 1, step: 0.01 })',
    'node.shapeOut({ name: "out" })',
  ]);
});

it("supports sections", () => {
  const stmts1 = _find(`node.pushSection("Test");`);
  expect(stmts1).toEqual(['node.pushSection("Test")']);
  const stmts2 = _find(`node.popSection();`);
  expect(stmts2).toEqual(["node.popSection()"]);
});

it("handles nested objects and arrays", () => {
  const stmts = _find(
    `const complexIn = node.objectIn({ name: "complex", value: { arr: [1, 2, 3], obj: { a: 1, b: 2 } } });`,
  );
  expect(stmts).toEqual(['node.objectIn({ name: "complex", value: { arr: [1, 2, 3], obj: { a: 1, b: 2 } } })']);
});

it("handles multi-line statements", () => {
  const source = `
  const multiLineIn = node.numberIn({
    name: "multi-line",
    value: 0,
    min: -10,
    max: 10
  });`;
  const stmts = _find(source);
  expect(stmts).toEqual(['node.numberIn({\n    name: "multi-line",\n    value: 0,\n    min: -10,\n    max: 10\n  })']);
});

it("handles statements with no spaces", () => {
  const stmts = _find(`const noSpaceIn=node.booleanIn({name:"no-space"});`);
  expect(stmts).toEqual(['node.booleanIn({name:"no-space"})']);
});

it("handles multiple statements in succession", () => {
  const stmts = _find(`
      const a = node.numberIn({ name: "a" });const b = node.numberIn({ name: "b" });
      const c = node.numberIn({ name: "c" });
    `);
  expect(stmts).toEqual([
    'node.numberIn({ name: "a" })',
    'node.numberIn({ name: "b" })',
    'node.numberIn({ name: "c" })',
  ]);
});

it("ignores node.onRender statements", () => {
  const stmts = _find(`
      const x = node.numberIn({ name: "x" });
      node.onRender = () => {
        console.log("Rendering");
      };
      const y = node.numberIn({ name: "y" });
    `);
  expect(stmts).toEqual(['node.numberIn({ name: "x" })', 'node.numberIn({ name: "y" })']);
});
