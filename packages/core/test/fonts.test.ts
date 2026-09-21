import { describe, expect, it } from "vitest";
import fs from "node:fs";
import { OpenTypeFontProvider, Text, corevector, parseFontName, setFontProvider, Point } from "../src";

const FONT_FILE = "/mnt/skills/examples/canvas-design/canvas-fonts/CrimsonPro-Regular.ttf";
const hasFont = fs.existsSync(FONT_FILE);

describe("fonts", () => {
  it("parses Java font names", () => {
    expect(parseFontName("Verdana-Bold")).toEqual({ family: "Verdana", style: "Bold" });
    expect(parseFontName("Helvetica Bold Italic")).toEqual({ family: "Helvetica", style: "Bold Italic" });
    expect(parseFontName("Verdana")).toEqual({ family: "Verdana", style: "Regular" });
  });

  it.skipIf(!hasFont)("outlines text with opentype.js", () => {
    const provider = new OpenTypeFontProvider();
    const buffer = fs.readFileSync(FONT_FILE);
    provider.registerBuffer(
      buffer.buffer.slice(buffer.byteOffset, buffer.byteOffset + buffer.byteLength),
      "Crimson Pro",
      "Regular",
    );
    setFontProvider(provider);
    try {
      const text = new Text("Hello", 0, 0);
      text.fontName = "Crimson Pro";
      text.fontSize = 24;
      const path = text.getPath();
      expect(path.contours.length).toBeGreaterThan(4);
      expect(path.getBounds().width).toBeGreaterThan(20);
      // Unknown fonts fall back to the first registered one, as AWT falls back to a default font.
      const shape = corevector.textpath("NodeBox", "Verdana", 36, "CENTER", new Point(0, 0), 0);
      expect(shape.constructor.name).toBe("Path");
      expect(provider.width("Hello", "Verdana", 24)).toBeGreaterThan(0);
    } finally {
      setFontProvider(null);
    }
  });
});
