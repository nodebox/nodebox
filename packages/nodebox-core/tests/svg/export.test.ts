import { describe, it, expect } from 'vitest';
import { exportSvg } from '../../src/svg/export.js';
import { rect, ellipse, line } from '../../src/ops/generators.js';

describe('SVG Export', () => {
  it('exports empty paths', () => {
    const svg = exportSvg([], [], { width: 100, height: 100 });
    expect(svg).toContain('<svg');
    expect(svg).toContain('</svg>');
    expect(svg).toContain('width="100"');
  });

  it('exports a rect', () => {
    const r = rect({ x: 50, y: 50 }, 100, 100, { x: 0, y: 0 });
    const svg = exportSvg([r], [], { width: 200, height: 200 });
    expect(svg).toContain('<path');
    expect(svg).toContain('d="M');
    expect(svg).toContain('Z');
  });

  it('exports fill and stroke', () => {
    const r = rect({ x: 50, y: 50 }, 100, 100, { x: 0, y: 0 });
    r.fill = { r: 1, g: 0, b: 0, a: 1 };
    r.stroke = { r: 0, g: 0, b: 1, a: 1 };
    r.strokeWidth = 2;
    const svg = exportSvg([r], [], { width: 200, height: 200 });
    expect(svg).toContain('fill="#ff0000"');
    expect(svg).toContain('stroke="#0000ff"');
    expect(svg).toContain('stroke-width="2"');
  });

  it('exports no fill', () => {
    const l = line({ x: 0, y: 0 }, { x: 100, y: 100 }, 2);
    const svg = exportSvg([l], [], { width: 200, height: 200 });
    expect(svg).toContain('fill="none"');
  });

  it('exports with background', () => {
    const svg = exportSvg([], [], {
      width: 100, height: 100,
      background: { r: 1, g: 1, b: 1, a: 1 },
    });
    expect(svg).toContain('<rect');
    expect(svg).toContain('fill="#ffffff"');
  });

  it('exports text', () => {
    const svg = exportSvg([], [{
      text: 'Hello',
      position: { x: 50, y: 50 },
      fontFamily: 'Inter',
      fontSize: 24,
      align: 'center',
      fill: { r: 0, g: 0, b: 0, a: 1 },
    }], { width: 200, height: 200 });
    expect(svg).toContain('<text');
    expect(svg).toContain('Hello');
    expect(svg).toContain('text-anchor="middle"');
  });

  it('escapes XML in text', () => {
    const svg = exportSvg([], [{
      text: '<script>alert("xss")</script>',
      position: { x: 0, y: 0 },
      fontFamily: 'Inter',
      fontSize: 12,
      align: 'left',
      fill: null,
    }], { width: 100, height: 100 });
    expect(svg).not.toContain('<script>');
    expect(svg).toContain('&lt;script&gt;');
  });
});
