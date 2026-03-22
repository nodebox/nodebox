import { describe, it, expect } from 'vitest';
import { textpath, textOnPath, loadFont, hasFontLoaded } from '../../src/ops/text.js';

describe('Text Operations', () => {
  // Note: These tests are limited without a real font loaded.
  // Full text tests require loading Inter.ttf.

  it('returns empty path when font not loaded', () => {
    const p = textpath('hello', 'NonExistentFont', 24, 'left', { x: 0, y: 0 }, 0);
    expect(p.contours.length).toBe(0);
  });

  it('textOnPath returns empty when font not loaded', () => {
    const path = { contours: [], fill: null, stroke: null, strokeWidth: 1 };
    const p = textOnPath('hello', path, 'NonExistentFont', 24, 'left', 0, 0);
    expect(p.contours.length).toBe(0);
  });

  it('hasFontLoaded returns false for unknown font', () => {
    expect(hasFontLoaded('Unknown')).toBe(false);
  });

  // Integration test with actual font would go here:
  // it('renders text with loaded font', async () => {
  //   const fontBytes = readFileSync('fonts/Inter.ttf');
  //   await loadFont(new Uint8Array(fontBytes), 'Inter');
  //   expect(hasFontLoaded('Inter')).toBe(true);
  //   const p = textpath('Hello', 'Inter', 24, 'left', { x: 0, y: 0 }, 0);
  //   expect(p.contours.length).toBeGreaterThan(0);
  // });
});
