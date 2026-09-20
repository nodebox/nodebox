import { describe, expect, it } from "vitest";
import { Color, JavaRandom, Point, color, corevector, data, list, math, string } from "../src";

describe("java random", () => {
  it("matches java.util.Random", () => {
    const r = new JavaRandom(42);
    expect(r.nextInt()).toBe(-1170105035);
    expect(new JavaRandom(42).nextDouble()).toBeCloseTo(0.7275636800328681, 12);
  });
});

describe("math", () => {
  it("random numbers match NodeBox 3", () => {
    const numbers = math.randomNumbers(3, -10, 10, 42);
    expect(numbers[0]).toBeCloseTo(3.89263, 3);
    expect(numbers[1]).toBeCloseTo(4.95359, 3);
    expect(numbers[2]).toBeCloseTo(4.66839, 3);
  });
  it("samples inclusively", () => {
    expect(math.sample(0, 1, 2)).toEqual([]);
    expect(math.sample(1, 100, 200)).toEqual([150]);
    expect(math.sample(3, 100, 200)).toEqual([100, 150, 200]);
    expect(math.sample(3, 200, 100)).toEqual([200, 150, 100]);
    const values = math.sample(1000, 0, 100);
    expect(values[values.length - 1]).toBe(100);
  });
  it("converts ranges", () => {
    expect(math.convertRange(50, 0, 100, 0, 1, "ignore")).toBeCloseTo(0.5);
    expect(math.convertRange(75, 0, 100, 1, 0, "ignore")).toBeCloseTo(0.25);
  });
  it("rounds like Java", () => {
    expect(math.round(5.5)).toBe(6);
    expect(math.round(5.4)).toBe(5);
    expect(math.mod(10, 3)).toBe(1);
    expect(() => math.mod(10, 0)).toThrow();
  });
  it("ranges", () => {
    expect(math.range(0, 10, 2.5)).toEqual([0, 2.5, 5, 7.5]);
    expect(math.range(10, 0, -5)).toEqual([10, 5]);
    expect(math.range(0, 10, 0)).toEqual([]);
    expect(math.runningTotal([1, 2, 3])).toEqual([0, 1, 3]);
    expect(math.runningTotal([])).toEqual([0]);
  });
  it("waves", () => {
    expect(math.wave(0, 100, 100, 0, "sine")).toBeCloseTo(50, 3);
    expect(math.wave(0, 100, 100, 25, "sine")).toBeCloseTo(0, 3);
    expect(math.wave(0, 100, 100, 75, "sine")).toBeCloseTo(100, 3);
    expect(math.wave(0, 100, 100, 0, "square")).toBe(0);
    expect(math.wave(0, 100, 100, 0, "triangle")).toBeCloseTo(50, 3);
  });
});

describe("list", () => {
  it("shuffles like Collections.shuffle", () => {
    expect(list.shuffle([1, 2, 3, 4, 5], 42)).toEqual([1, 3, 2, 4, 5]);
    expect(list.shuffle([1, 2, 3, 4, 5], 33)).toEqual([2, 1, 3, 4, 5]);
  });
  it("slices, shifts and repeats", () => {
    expect(list.slice([1, 2, 3, 4, 5], 1, 2, false)).toEqual([2, 3]);
    expect(list.slice([1, 2, 3, 4, 5], 1, 2, true)).toEqual([1, 4, 5]);
    expect(list.shift([1, 2, 3], 1)).toEqual([2, 3, 1]);
    expect(list.shift([1, 2, 3], -1)).toEqual([3, 1, 2]);
    expect(list.repeat([1, 2], 2, false)).toEqual([1, 2, 1, 2]);
    expect(list.repeat([1, 2], 2, true)).toEqual([1, 1, 2, 2]);
    expect(list.cull([1, 2, 3, 4], [true, false])).toEqual([1, 3]);
    expect(list.takeEvery([1, 2, 3, 4, 5], 2)).toEqual([1, 3, 5]);
    expect(list.doSwitch([1], [2], [3], [4], [5], [6], 7)).toEqual([2]);
  });
  it("sorts and dedups", () => {
    expect(list.sort([3, 1, 2], "")).toEqual([1, 2, 3]);
    expect(list.sort([{ a: 2 }, { a: 1 }], "a")).toEqual([{ a: 1 }, { a: 2 }]);
    expect(list.distinct([1, 2, 1, "1"], "")).toEqual([1, 2, "1"]);
    expect(list.keys([{ a: 1 }, { b: 2 }])).toEqual(["a", "b"]);
    expect(list.zipMap(["a", "b"], [1])).toEqual({ a: 1 });
  });
});

describe("string", () => {
  it("formats numbers with Java formats", () => {
    expect(string.formatNumber(3.14159, "%.2f")).toBe("3.14");
    expect(string.formatNumber(42, "%05d")).toBe("00042");
    expect(string.formatNumber(1234567.891, "%,.1f")).toBe("1,234,567.9");
    expect(string.formatNumber(5, "Value: %d units")).toBe("Value: 5 units");
  });
  it("handles substrings and cases", () => {
    expect(string.subString("hello", 1, 3, false)).toBe("el");
    expect(string.subString("hello", 1, 3, true)).toBe("ell");
    expect(string.subString("hello", -3, -1, false)).toBe("ll");
    expect(string.characterAt("hello", -1)).toBe("o");
    expect(string.changeCase("hello world", "titlecase")).toBe("Hello World");
    expect(string.wordCount("one two  three")).toBe(3);
    expect(string.makeStrings("abc", "")).toEqual(["a", "b", "c"]);
    expect(string.asBinaryList("A")).toEqual(["0", "1", "0", "0", "0", "0", "0", "1"]);
    expect(string.asNumberList("A", 16, true)).toEqual(["41"]);
    expect(string.randomCharacter("abc", 3, 1)).toHaveLength(3);
  });
});

describe("color", () => {
  it("builds colors", () => {
    expect(color.rgb(255, 0, 0, 255, 255)).toEqual(new Color(1, 0, 0, 1));
    expect(color.hsb(0, 1, 1, 1, 1)).toEqual(new Color(1, 0, 0, 1));
    expect(color.gray(0.5, 1, 1).r).toBeCloseTo(0.5);
    expect(Color.parse("#ff8000").toString()).toBe("#ff8000ff");
    expect(Color.fromHSB(0.5, 1, 1).toString()).toBe("#00ffffff");
  });
});

describe("data", () => {
  it("parses csv with numeric columns", () => {
    const rows = data.parseCsvTable('name,value\n"Smith, J",1.5\nDoe,2\n');
    expect(rows).toEqual([
      { name: "Smith, J", value: 1.5 },
      { name: "Doe", value: 2 },
    ]);
    expect(data.filterData(rows, "value", ">", 1.7)).toEqual([{ name: "Doe", value: 2 }]);
    expect(data.filterData(rows, "name", "=", "Doe")).toEqual([{ name: "Doe", value: 2 }]);
  });
  it("makes tables", () => {
    expect(data.makeTable("a;b", [1, 2], ["x"], null, null, null, null)).toEqual([
      { a: 1, b: "x" },
      { a: 2, b: "" },
    ]);
    expect(data.lookup({ a: { b: 3 } }, "a.b")).toBe(3);
  });
});

describe("corevector", () => {
  it("creates primitives with the right bounds", () => {
    const r = corevector.rect(new Point(0, 0), 100, 50, Point.ZERO);
    expect(r.getBounds()).toEqual({ x: -50, y: -25, width: 100, height: 50 });
    const e = corevector.ellipse(new Point(10, 10), 20, 20);
    const b = e.getBounds();
    expect(b.x).toBeCloseTo(0);
    expect(b.width).toBeCloseTo(20);
    expect(e.getLength()).toBeCloseTo(Math.PI * 20, 1);
    expect(e.contains(new Point(10, 10))).toBe(true);
    expect(e.contains(new Point(25, 25))).toBe(false);
  });
  it("copies with transforms", () => {
    const r = corevector.rect(new Point(0, 0), 10, 10, Point.ZERO);
    const copies = corevector.copy(r, 3, "tsr", new Point(20, 0), 0, new Point(100, 100));
    expect(copies).toHaveLength(3);
    expect(corevector.centroid(copies[2])).toEqual(new Point(40, 0));
  });
  it("grids and stars and polygons", () => {
    expect(corevector.grid(2, 2, 100, 100, Point.ZERO)).toEqual([new Point(-50, -50), new Point(50, -50), new Point(-50, 50), new Point(50, 50)]);
    expect(corevector.star(Point.ZERO, 5, 100, 50).pointCount).toBe(10);
    expect(corevector.polygon(Point.ZERO, 50, 6, false).pointCount).toBe(6);
  });
  it("resamples and measures", () => {
    const line = corevector.line(new Point(0, 0), new Point(100, 0), 5);
    expect(line.points.map((p) => p.x)).toEqual([0, 25, 50, 75, 100]);
    expect(corevector.pointOnPath(line, 50)).toEqual(new Point(50, 0));
    const resampled = corevector.resample(line, "length", 10, 0) as import("../src").Path;
    expect(resampled.pointCount).toBe(11);
  });
  it("does boolean operations", () => {
    const a = corevector.rect(new Point(0, 0), 100, 100, Point.ZERO);
    const b = corevector.rect(new Point(50, 0), 100, 100, Point.ZERO);
    const united = corevector.compound(a, b, "united", false)!;
    expect(united.getBounds().width).toBeCloseTo(150);
    const intersected = corevector.compound(a, b, "intersected", false)!;
    expect(intersected.getBounds().width).toBeCloseTo(50);
    const subtracted = corevector.compound(a, b, "subtracted", false)!;
    expect(subtracted.getBounds().width).toBeCloseTo(50);
  });
  it("scatters deterministically inside the shape", () => {
    const e = corevector.ellipse(Point.ZERO, 100, 100);
    const pts = corevector.scatter(e, 20, 7)!;
    expect(pts).toHaveLength(20);
    for (const p of pts) expect(e.contains(p)).toBe(true);
    expect(corevector.scatter(e, 20, 7)).toEqual(pts);
  });
});
