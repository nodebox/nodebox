import { Paint, SolidPaint } from "../src";
import { describe, it, expect } from "vitest";

describe("Paint Class", () => {
  it("should create a SolidPaint instance with default alpha", () => {
    const paint = Paint.solid(1, 0.5, 0);
    expect(paint.r).toBe(1);
    expect(paint.g).toBe(0.5);
    expect(paint.b).toBe(0);
    expect(paint.a).toBe(1);
  });

  it("should create a SolidPaint instance with custom alpha", () => {
    const paint = Paint.solid(1, 0.5, 0, 0.75);
    expect(paint.r).toBe(1);
    expect(paint.g).toBe(0.5);
    expect(paint.b).toBe(0);
    expect(paint.a).toBe(0.75);
  });

  it("should clone a Paint instance", () => {
    const paint = Paint.solid(1, 0.5, 0, 0.75);
    const clone = paint.clone();
    expect(clone).not.toBe(paint);
    expect(clone.r).toBe(paint.r);
    expect(clone.g).toBe(paint.g);
    expect(clone.b).toBe(paint.b);
    expect(clone.a).toBe(paint.a);
  });

  it("should parse an RGB color string", () => {
    const paint = Paint.parse("rgb(255, 127, 0)");
    expect(paint.type).toBe("solid");
    const solid = paint as SolidPaint;
    expect(solid.r).toBeCloseTo(1);
    expect(solid.g).toBeCloseTo(0.498);
    expect(solid.b).toBeCloseTo(0);
    expect(solid.a).toBe(1);
  });

  it("should parse an HSL color string", () => {
    const paint = Paint.parse("hsl(30, 100%, 50%)");
    expect(paint.type).toBe("solid");
    const solid = paint as SolidPaint;
    expect(solid.r).toBeCloseTo(1);
    expect(solid.g).toBeCloseTo(0.5);
    expect(solid.b).toBeCloseTo(0);
    expect(solid.a).toBe(1);
  });

  it('should parse "none" string to a NoneColor', () => {
    const paint = Paint.parse("none");
    expect(paint.type).toBe("none");
    expect(paint.isTransparent()).toBe(true);
  });

  it('should parse "transparent" string to a transparent black', () => {
    const paint = Paint.parse("transparent");
    expect(paint.type).toBe("solid");
    const solid = paint as SolidPaint;
    expect(solid.r).toBe(0);
    expect(solid.g).toBe(0);
    expect(solid.b).toBe(0);
    expect(solid.a).toBe(0);
  });

  it("should create a Paint instance from a hex string - #ff7f00", () => {
    const paint = Paint.parse("#ff7f00");
    expect(paint.type).toBe("solid");
    const solid = paint as SolidPaint;
    expect(solid.r).toBeCloseTo(1);
    expect(solid.g).toBeCloseTo(0.498);
    expect(solid.b).toBeCloseTo(0);
    expect(solid.a).toBe(1);
  });

  it("should create a Paint instance from a hex string - #ff7f009b", () => {
    const paint = Paint.parse("#ff7f009b"); // rgba(255, 127, 0, 0.61)
    expect(paint.type).toBe("solid");
    const solid = paint as SolidPaint;
    expect(solid.r).toBeCloseTo(1);
    expect(solid.g).toBeCloseTo(0.498);
    expect(solid.b).toBeCloseTo(0);
    expect(solid.a).toBeCloseTo(0.61);
  });

  it("should convert a Paint instance to a hex string", () => {
    const paint = Paint.solid(1, 0.498, 0);
    const hex = paint.toString();
    expect(hex).toBe("#ff7f00");
  });

  it("should convert a Paint instance to a hex string", () => {
    const paint = Paint.solid(1, 0.498, 0, 0.2);
    const hex1 = paint.toHex();
    expect(hex1).toBe("#ff7f00");

    const hex2 = paint.toHex(true);
    expect(hex2).toBe("#ff7f0033");
  });

  it("should create a black Paint instance", () => {
    const black = Paint.black();
    expect(black.r).toBe(0);
    expect(black.g).toBe(0);
    expect(black.b).toBe(0);
    expect(black.a).toBe(1);
  });

  it("should create a white Paint instance", () => {
    const white = Paint.white();
    expect(white.r).toBe(1);
    expect(white.g).toBe(1);
    expect(white.b).toBe(1);
    expect(white.a).toBe(1);
  });

  it("should create a none Paint instance", () => {
    const none = Paint.none();
    expect(none.type).toBe("none");
    expect(none.isTransparent()).toBe(true);
  });

  it("should create a transparent Paint instance", () => {
    const none = Paint.transparent();
    expect(none.r).toBe(0);
    expect(none.g).toBe(0);
    expect(none.b).toBe(0);
    expect(none.a).toBe(0);
    expect(none.isTransparent()).toBe(true);
  });

  it("should convert a Paint instance to a string", () => {
    const paint = Paint.solid(1, 0.498, 0);
    const paintString = paint.toString();
    expect(paintString).toBe("#ff7f00");
  });

  it("should convert a Paint instance with alpha to an rgba string", () => {
    const paint = Paint.solid(1, 0.498, 0, 0.5);
    const paintString = paint.toString();
    expect(paintString).toBe("rgba(255, 127, 0, 0.5)");
  });

  it("should convert a Paint instance with zero values to 'transparent'", () => {
    const paint1 = Paint.solid(1, 0.498, 0, 0.0);
    const paintString1 = paint1.toString();
    expect(paintString1).toBe("rgba(255, 127, 0, 0)");

    const paint2 = Paint.solid(0, 0, 0, 0);
    const paintString2 = paint2.toString();
    expect(paintString2).toBe("transparent");
  });

  it("should create a Paint instance from an object", () => {
    const paint = Paint.parse({ r: 1, g: 1, b: 0.588, a: 0.8 }) as SolidPaint;
    expect(paint.r).toBe(1);
    expect(paint.g).toBe(1);
    expect(paint.b).toBeCloseTo(0.588);
    expect(paint.a).toBeCloseTo(0.8);
  });

  it("should create a Paint instance from an object without alpha", () => {
    const paint = Paint.parse({ r: 1, g: 1, b: 0.588 }) as SolidPaint;
    expect(paint.r).toBe(1);
    expect(paint.g).toBe(1);
    expect(paint.b).toBeCloseTo(0.588);
    expect(paint.a).toBe(1);
  });

  it("should convert a Paint instance to an object", () => {
    const paint = Paint.solid(1, 0.5, 0, 0.75);
    const paintObj = paint.toObject();
    expect(paintObj.r).toBe(1);
    expect(paintObj.g).toBe(0.5);
    expect(paintObj.b).toBe(0);
    expect(paintObj.a).toBe(0.75);
  });

  it("should convert a Paint instance to an object without alpha", () => {
    const paint = Paint.solid(1, 0.5, 0);
    expect(paint.a).toBe(1);
    const paintObj = paint.toObject();
    expect(paintObj.r).toBe(1);
    expect(paintObj.g).toBe(0.5);
    expect(paintObj.b).toBe(0);
    expect(paintObj.a).toBe(1);
  });
});
