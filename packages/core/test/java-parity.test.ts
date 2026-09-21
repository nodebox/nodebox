// Behaviours checked against the original Java engine (nodebox.graphics, nodebox.function and AWT
// text) on the same inputs. The expected values below were printed by that engine; see
// scripts/java-reference/README.md for how to run it.
import { afterAll, beforeAll, describe, expect, it } from "vitest";
import { Path, Point, Text, setFontProvider } from "../src";
import { corevector, list, math, string } from "../src";
import { installBundledFonts } from "../src/fonts/node";

function points(p: Path): string {
  return p.points.map((pt) => `${"?LCD"[pt.type]}${pt.x.toFixed(2)},${pt.y.toFixed(2)}`).join(" ");
}

describe("Path defaults and construction", () => {
  it("has no stroke width until a node sets one", () => {
    expect(new Path().strokeWidth).toBe(0);
    expect(corevector.connect([new Point(0, 0), new Point(10, 0)], false)).toMatchObject({ strokeWidth: 1 });
  });

  it("connect keeps the point types, so the points of a curve give the curve back", () => {
    const curve = new Path();
    curve.moveto(0, 0);
    curve.curveto(10, 0, 20, 10, 30, 10);
    const rebuilt = corevector.connect(curve.points, false) as Path;
    expect(points(rebuilt)).toBe("L0.00,0.00 D10.00,0.00 D20.00,10.00 C30.00,10.00");
  });

  it("connects a single point into a one-point path, as Java does", () => {
    expect((corevector.connect([new Point(3, 4)], false) as Path).pointCount).toBe(1);
  });

  it("builds rounded rectangles with the radius semantics of Path.roundedRect", () => {
    const p = new Path();
    p.roundedRect(200, 95, 77, 77, 19.25);
    expect(points(p).split(" ").slice(0, 5).join(" ")).toBe(
      "L180.75,56.50 L219.25,56.50 D229.88,56.50 D238.50,65.12 C238.50,75.75",
    );
  });
});

describe("Boolean operations behave like java.awt.geom.Area", () => {
  const ellipse = () => {
    const e = new Path();
    e.ellipse(0, 0, 20, 20);
    return e;
  };
  const rect = (x: number, y: number, w: number, h: number) => {
    const r = new Path();
    r.rect(x, y, w, h);
    return r;
  };

  it("keeps curves when the shapes do not cross, and starts outer rings top-left going down", () => {
    const u = ellipse().united(rect(0, 0, 4, 4));
    expect(u.pointCount).toBe(13);
    expect(points(u).split(" ").slice(0, 4).join(" ")).toBe("L0.00,-10.00 D-5.52,-10.00 D-10.00,-5.52 C-10.00,0.00");
    expect(u.strokeWidth).toBe(0);
  });

  it("subtracts a contained shape as a clockwise hole listed before the outer ring", () => {
    const d = ellipse().subtracted(rect(0, 0, 4, 4));
    expect(d.contours.length).toBe(2);
    expect(points(Path.fromContour(d.contours[0]))).toBe("L2.00,-2.00 L2.00,2.00 L-2.00,2.00 L-2.00,-2.00");
  });

  it("orders disjoint subpaths from the rightmost to the leftmost", () => {
    const far = rect(100, 0, 4, 4);
    const tri = new Path();
    tri.moveto(0, 0);
    tri.lineto(10, 0);
    tri.lineto(10, 10);
    tri.close();
    expect(points(tri.united(far))).toBe(
      "L98.00,-2.00 L98.00,2.00 L102.00,2.00 L102.00,-2.00 L0.00,0.00 L10.00,10.00 L10.00,0.00",
    );
    expect(ellipse().intersected(far).isEmpty()).toBe(true);
  });

  it("clips crossing shapes on their outlines with the same orientation rules", () => {
    const tri = new Path();
    tri.moveto(0, 0);
    tri.lineto(10, 0);
    tri.lineto(10, 10);
    tri.close();
    expect(points(rect(5, 5, 10, 10).united(tri))).toBe("L0.00,0.00 L0.00,10.00 L10.00,10.00 L10.00,0.00");
  });
});

describe("Function library corner cases", () => {
  it("compares doubles like Double.compare: NaN is above everything", () => {
    expect(math.compare(NaN, 0, "!=")).toBe(true);
    expect(math.compare(NaN, 0, ">")).toBe(true);
    expect(math.compare(NaN, NaN, "==")).toBe(true);
    expect(math.compare(-0, 0, "<")).toBe(true);
  });

  it("splits the empty string into one empty piece, as Guava's Splitter does", () => {
    expect(string.characters("")).toEqual([""]);
    expect(string.makeStrings("", "")).toEqual([""]);
    expect(string.makeStrings("", ",")).toEqual([""]);
  });

  it("lists Java bean properties for values that are not maps", () => {
    expect(list.keys(["s", 15, 60])).toEqual(["blank", "bytes", "class", "empty", "infinite", "naN"]);
    expect(list.keys([new Point(1, 2)])).toContain("curveTo");
    expect(list.keys([{ a: 1 }])).toEqual(["a"]);
  });
});

describe("Text layout matches AWT with the bundled DejaVu Sans", () => {
  beforeAll(() => installBundledFonts());
  afterAll(() => setFontProvider(null));

  function text(s: string, size: number, width = 0, align: Text["align"] = "CENTER"): Text {
    const t = new Text(s, 0, 0);
    t.fontName = "Verdana";
    t.fontSize = size;
    t.width = width;
    t.align = align;
    return t;
  }

  it("walks TrueType outlines like FreeType, on the 1/64 pixel grid", () => {
    const p = text("o", 16).getPath();
    expect(p.pointCount).toBe(50);
    expect(points(p).split(" ").slice(0, 4).join(" ")).toBe("L0.01,-7.75 D-0.76,-7.75 D-1.37,-7.45 C-1.82,-6.84");
    expect(text("Hello", 16).getPath().pointCount).toBe(127);
  });

  it("reports metrics as the union of the ink and the origin", () => {
    expect(String(text("a", 16, 0, "LEFT").getMetrics())).toBe("Rect(0, -8.96875, 8.359375, 9.203125)");
  });

  it("applies pair kerning and centers each line on its advance including trailing spaces", () => {
    const av = text("AV", 100, 0, "LEFT").getPath();
    expect(av.contours[av.contours.length - 1].points[0].x).toBeCloseTo(90.6211, 3);
    const b = text("one  two", 10, 40).getPath().getBounds();
    expect(b.x).toBeCloseTo(8.06396484375, 6);
    expect(b.width).toBeCloseTo(20.5048828125, 6);
  });

  it("wraps like LineBreakMeasurer: unkerned advances, character breaks inside long words", () => {
    expect(text("Asia Pacific", 5, 28.1).lines()).toEqual(["Asia ", "Pacific"]);
    expect(text("Asia Pacific", 5, 28.2).lines()).toEqual(["Asia Pacific"]);
    const b = text("20", 16, 14).getPath().getBounds();
    expect(b.height).toBeCloseTo(29.93125, 4);
  });

  it("truncates fractional sizes to 1/64 like FT_Set_Char_Size", () => {
    expect(text("Hgjq@", 13.7, 0, "LEFT").getMetrics().width).toBeCloseTo(44.206268, 5);
  });
});
