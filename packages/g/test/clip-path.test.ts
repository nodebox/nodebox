import { ClipPath, Rect, Ellipse } from "../src";
import { describe, it, expect } from "vitest";

describe("ClipPath Class", () => {
  it("should create a basic clip path", () => {
    const rect = new Rect(10, 10, 100, 100);
    const clipShape = new Ellipse(50, 50, 50);
    rect.clip(clipShape);
    expect(rect.clipPath).toBeDefined();
    expect(rect.clipPath).toBeInstanceOf(ClipPath);
    expect(rect.clipPath!.shape).toBe(clipShape);
    expect(rect.clipPath!.id).toMatch(/^clip-\d+$/);
  });
});
