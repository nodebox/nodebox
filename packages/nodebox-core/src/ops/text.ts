import type { Point } from '../geometry/point.js';
import type { Path, PathPoint, Contour } from '../geometry/path.js';
import type { TextAlign } from '../geometry/text.js';
import type { Color } from '../geometry/color.js';
import { DEFAULT_FILL, pointOnPath, pathLength } from '../geometry/path.js';
import { translateTransform, rotateTransform, multiplyTransforms, transformPoint } from '../geometry/transform.js';

// opentype.js will be loaded dynamically
let opentypeModule: any = null;
const fontCache = new Map<string, any>();

function pp(x: number, y: number, type: PathPoint['type'] = 'lineTo'): PathPoint {
  return { point: { x, y }, type };
}

async function getOpentype(): Promise<any> {
  if (opentypeModule) return opentypeModule;
  try {
    opentypeModule = await import('opentype.js');
    return opentypeModule;
  } catch {
    throw new Error('opentype.js is required for text operations');
  }
}

export async function loadFont(fontBytes: Uint8Array, name: string): Promise<void> {
  const opentype = await getOpentype();
  const font = opentype.parse(fontBytes.buffer);
  fontCache.set(name, font);
}

export function hasFontLoaded(name: string): boolean {
  return fontCache.has(name);
}

// Convert opentype glyph paths to our Path format
function glyphPathToPath(otPath: any): Path {
  const contours: Contour[] = [];
  let currentPoints: PathPoint[] = [];

  for (const cmd of otPath.commands) {
    switch (cmd.type) {
      case 'M':
        if (currentPoints.length > 0) {
          contours.push({ points: currentPoints, closed: false });
        }
        currentPoints = [pp(cmd.x, cmd.y)];
        break;
      case 'L':
        currentPoints.push(pp(cmd.x, cmd.y));
        break;
      case 'C':
        currentPoints.push(pp(cmd.x1, cmd.y1, 'curveData'));
        currentPoints.push(pp(cmd.x2, cmd.y2, 'curveData'));
        currentPoints.push(pp(cmd.x, cmd.y, 'curveTo'));
        break;
      case 'Q':
        currentPoints.push(pp(cmd.x1, cmd.y1, 'quadData'));
        currentPoints.push(pp(cmd.x, cmd.y, 'quadTo'));
        break;
      case 'Z':
        if (currentPoints.length > 0) {
          contours.push({ points: currentPoints, closed: true });
          currentPoints = [];
        }
        break;
    }
  }
  if (currentPoints.length > 0) {
    contours.push({ points: currentPoints, closed: false });
  }

  return { contours, fill: DEFAULT_FILL, stroke: null, strokeWidth: 1 };
}

export function textpath(
  text: string, fontName: string, fontSize: number,
  align: TextAlign, position: Point, width: number,
): Path {
  const font = fontCache.get(fontName);
  if (!font) {
    // Return empty path if font not loaded
    return { contours: [], fill: DEFAULT_FILL, stroke: null, strokeWidth: 1 };
  }

  const otPath = font.getPath(text, 0, 0, fontSize);
  const path = glyphPathToPath(otPath);

  // Calculate alignment offset
  const bb = otPath.getBoundingBox();
  const textWidth = bb.x2 - bb.x1;
  let dx = position.x;
  if (align === 'center') dx -= textWidth / 2;
  else if (align === 'right') dx -= textWidth;

  const dy = position.y;

  // Translate all points
  return {
    ...path,
    contours: path.contours.map(c => ({
      ...c,
      points: c.points.map(p => ({
        ...p,
        point: { x: p.point.x + dx, y: p.point.y + dy },
      })),
    })),
  };
}

export function textOnPath(
  text: string, path: Path, fontName: string, fontSize: number,
  alignment: string, margin: number, baselineOffset: number,
): Path {
  const font = fontCache.get(fontName);
  if (!font) {
    return { contours: [], fill: DEFAULT_FILL, stroke: null, strokeWidth: 1 };
  }

  const totalLength = pathLength(path);
  if (totalLength === 0) return { contours: [], fill: DEFAULT_FILL, stroke: null, strokeWidth: 1 };

  const allContours: Contour[] = [];
  let currentOffset = margin;

  for (const char of text) {
    const glyph = font.charToGlyph(char);
    const advance = (glyph.advanceWidth / font.unitsPerEm) * fontSize;

    const t = currentOffset / totalLength;
    if (t > 1) break;

    const pt = pointOnPath(path, t);
    // Approximate tangent angle
    const dt = 0.001;
    const pt2 = pointOnPath(path, Math.min(1, t + dt));
    const angle = Math.atan2(pt2.y - pt.y, pt2.x - pt.x) * 180 / Math.PI;

    // Get character path
    const otPath = font.getPath(char, 0, 0, fontSize);
    const charPath = glyphPathToPath(otPath);

    // Transform: translate to path point, rotate to tangent
    const transform = multiplyTransforms(
      translateTransform(pt.x, pt.y + baselineOffset),
      rotateTransform(angle),
    );

    for (const contour of charPath.contours) {
      allContours.push({
        ...contour,
        points: contour.points.map(p => ({
          ...p,
          point: transformPoint(transform, p.point),
        })),
      });
    }

    currentOffset += advance;
  }

  return { contours: allContours, fill: DEFAULT_FILL, stroke: null, strokeWidth: 1 };
}
