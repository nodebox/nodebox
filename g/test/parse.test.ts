import { expect, it } from "vitest";
import { Shape, ShapeType, parseSVG } from "../src";
import { JSDOM } from "jsdom";

it("can parse a simple SVG", () => {
  const svg = `<rect x="10" y="20" width="30" height="40"/>`;
  const shape = _parseSVG(svg);
  expect(shape.type === ShapeType.Rect);
});

it("can parse transforms", () => {
  function expectMatrix(s: string, values: number[]) {
    const svg = `<g transform="${s}"/>`;
    const shape = _parseSVG(svg);
    expect(shape.type === ShapeType.Group);
    expect(Array.from(shape.transform.matrix)).toEqual(values);
  }

  expectMatrix("translate(10,20)", [1, 0, 0, 1, 10, 20]);
  expectMatrix("translate(10 20)", [1, 0, 0, 1, 10, 20]);
  expectMatrix("translate(10)", [1, 0, 0, 1, 10, 0]);

  //   expectMatrix("rotate(90)", [1, 0, Math.cos(Math.PI), 1, 10, 0]);
});

function makeSvg(s: string, width?: number, height?: number): string {
  width = width !== undefined ? width : 1000;
  height = height !== undefined ? height : 1000;
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" width="${width}" height="${height}" viewBox="0 0 ${width} ${height}">${s}</svg>`;
  return svg;
}

function _parseSVG(svg: string): Shape {
  const dom = new JSDOM();
  const parser = new dom.window.DOMParser();
  const shape = parseSVG(makeSvg(svg), parser);
  return shape;
}
