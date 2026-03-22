import type { Path, PathPoint, Contour } from '../geometry/path.js';
import type { Color } from '../geometry/color.js';
import { colorFromHex } from '../geometry/color.js';
import { DEFAULT_FILL } from '../geometry/path.js';

function pp(x: number, y: number, type: PathPoint['type'] = 'lineTo'): PathPoint {
  return { point: { x, y }, type };
}

export function importSvg(svgString: string): Path[] {
  // Parse using DOMParser or @xmldom/xmldom
  let doc: Document;
  if (typeof DOMParser !== 'undefined') {
    doc = new DOMParser().parseFromString(svgString, 'image/svg+xml');
  } else {
    // Node.js / Web Worker — use @xmldom/xmldom (imported dynamically)
    throw new Error('SVG import requires DOMParser. Use @xmldom/xmldom in Node.js.');
  }
  return parseSvgDocument(doc);
}

// For Node.js environments, accept a pre-parsed document
export function importSvgFromDocument(doc: Document): Path[] {
  return parseSvgDocument(doc);
}

function parseSvgDocument(doc: Document): Path[] {
  const paths: Path[] = [];
  const svgEl = doc.documentElement;
  parseElement(svgEl, paths, null, null, 1);
  return paths;
}

function parseElement(
  el: Element, paths: Path[],
  parentFill: Color | null, parentStroke: Color | null,
  parentStrokeWidth: number,
): void {
  const fill = parseColorAttr(el, 'fill') ?? parentFill;
  const stroke = parseColorAttr(el, 'stroke') ?? parentStroke;
  const sw = parseFloat(el.getAttribute('stroke-width') ?? '') || parentStrokeWidth;

  const tag = el.tagName.toLowerCase();
  if (tag === 'path') {
    const d = el.getAttribute('d');
    if (d) {
      const parsed = parseSvgPathData(d);
      for (const contour of parsed) {
        paths.push({
          contours: [contour],
          fill: fill ?? DEFAULT_FILL,
          stroke: stroke,
          strokeWidth: sw,
        });
      }
    }
  } else if (tag === 'rect') {
    const x = parseFloat(el.getAttribute('x') ?? '0');
    const y = parseFloat(el.getAttribute('y') ?? '0');
    const w = parseFloat(el.getAttribute('width') ?? '0');
    const h = parseFloat(el.getAttribute('height') ?? '0');
    paths.push({
      contours: [{
        points: [pp(x, y), pp(x + w, y), pp(x + w, y + h), pp(x, y + h)],
        closed: true,
      }],
      fill: fill ?? DEFAULT_FILL,
      stroke, strokeWidth: sw,
    });
  } else if (tag === 'circle') {
    const cx = parseFloat(el.getAttribute('cx') ?? '0');
    const cy = parseFloat(el.getAttribute('cy') ?? '0');
    const r = parseFloat(el.getAttribute('r') ?? '0');
    paths.push(makeEllipsePath(cx, cy, r, r, fill ?? DEFAULT_FILL, stroke, sw));
  } else if (tag === 'ellipse') {
    const cx = parseFloat(el.getAttribute('cx') ?? '0');
    const cy = parseFloat(el.getAttribute('cy') ?? '0');
    const rx = parseFloat(el.getAttribute('rx') ?? '0');
    const ry = parseFloat(el.getAttribute('ry') ?? '0');
    paths.push(makeEllipsePath(cx, cy, rx, ry, fill ?? DEFAULT_FILL, stroke, sw));
  } else if (tag === 'line') {
    const x1 = parseFloat(el.getAttribute('x1') ?? '0');
    const y1 = parseFloat(el.getAttribute('y1') ?? '0');
    const x2 = parseFloat(el.getAttribute('x2') ?? '0');
    const y2 = parseFloat(el.getAttribute('y2') ?? '0');
    paths.push({
      contours: [{ points: [pp(x1, y1), pp(x2, y2)], closed: false }],
      fill: null, stroke: stroke ?? { r: 0, g: 0, b: 0, a: 1 }, strokeWidth: sw,
    });
  } else if (tag === 'polyline' || tag === 'polygon') {
    const pts = el.getAttribute('points') ?? '';
    const points = parsePointsAttr(pts);
    if (points.length > 0) {
      paths.push({
        contours: [{ points, closed: tag === 'polygon' }],
        fill: tag === 'polygon' ? (fill ?? DEFAULT_FILL) : null,
        stroke, strokeWidth: sw,
      });
    }
  }

  // Recurse into children
  for (let i = 0; i < el.children.length; i++) {
    parseElement(el.children[i], paths, fill, stroke, sw);
  }
}

function parseColorAttr(el: Element, attr: string): Color | null {
  const val = el.getAttribute(attr);
  if (!val || val === 'none') return null;
  try {
    return colorFromHex(val);
  } catch {
    // Handle named colors or rgb() — simplified
    if (val === 'black') return { r: 0, g: 0, b: 0, a: 1 };
    if (val === 'white') return { r: 1, g: 1, b: 1, a: 1 };
    if (val === 'red') return { r: 1, g: 0, b: 0, a: 1 };
    if (val === 'green') return { r: 0, g: 0.502, b: 0, a: 1 };
    if (val === 'blue') return { r: 0, g: 0, b: 1, a: 1 };
    return null;
  }
}

function parsePointsAttr(str: string): PathPoint[] {
  const nums = str.trim().split(/[\s,]+/).map(Number).filter(n => !isNaN(n));
  const points: PathPoint[] = [];
  for (let i = 0; i + 1 < nums.length; i += 2) {
    points.push(pp(nums[i], nums[i + 1]));
  }
  return points;
}

function makeEllipsePath(
  cx: number, cy: number, rx: number, ry: number,
  fill: Color | null, stroke: Color | null, strokeWidth: number,
): Path {
  const k = 0.5522847498;
  const kx = rx * k;
  const ky = ry * k;
  const points: PathPoint[] = [
    pp(cx, cy - ry),
    pp(cx + kx, cy - ry, 'curveData'),
    pp(cx + rx, cy - ky, 'curveData'),
    pp(cx + rx, cy, 'curveTo'),
    pp(cx + rx, cy + ky, 'curveData'),
    pp(cx + kx, cy + ry, 'curveData'),
    pp(cx, cy + ry, 'curveTo'),
    pp(cx - kx, cy + ry, 'curveData'),
    pp(cx - rx, cy + ky, 'curveData'),
    pp(cx - rx, cy, 'curveTo'),
    pp(cx - rx, cy - ky, 'curveData'),
    pp(cx - kx, cy - ry, 'curveData'),
    pp(cx, cy - ry, 'curveTo'),
  ];
  return { contours: [{ points, closed: true }], fill, stroke, strokeWidth };
}

// ─── SVG Path Data Parser ──────────────────────────────
export function parseSvgPathData(d: string): Contour[] {
  const contours: Contour[] = [];
  let currentPoints: PathPoint[] = [];
  let cx = 0, cy = 0; // current point
  let startX = 0, startY = 0; // start of current subpath
  let lastCx = 0, lastCy = 0; // last control point for smooth curves

  const tokens = tokenize(d);
  let i = 0;

  function nextNum(): number {
    return i < tokens.length ? parseFloat(tokens[i++]) : 0;
  }

  while (i < tokens.length) {
    const cmd = tokens[i++];

    switch (cmd) {
      case 'M': {
        if (currentPoints.length > 0) contours.push({ points: currentPoints, closed: false });
        cx = nextNum(); cy = nextNum();
        startX = cx; startY = cy;
        currentPoints = [pp(cx, cy)];
        // Subsequent coordinate pairs are implicit L commands
        while (i < tokens.length && isNumber(tokens[i])) {
          cx = nextNum(); cy = nextNum();
          currentPoints.push(pp(cx, cy));
        }
        break;
      }
      case 'm': {
        if (currentPoints.length > 0) contours.push({ points: currentPoints, closed: false });
        cx += nextNum(); cy += nextNum();
        startX = cx; startY = cy;
        currentPoints = [pp(cx, cy)];
        while (i < tokens.length && isNumber(tokens[i])) {
          cx += nextNum(); cy += nextNum();
          currentPoints.push(pp(cx, cy));
        }
        break;
      }
      case 'L':
        while (i < tokens.length && isNumber(tokens[i])) {
          cx = nextNum(); cy = nextNum();
          currentPoints.push(pp(cx, cy));
        }
        break;
      case 'l':
        while (i < tokens.length && isNumber(tokens[i])) {
          cx += nextNum(); cy += nextNum();
          currentPoints.push(pp(cx, cy));
        }
        break;
      case 'H':
        while (i < tokens.length && isNumber(tokens[i])) {
          cx = nextNum();
          currentPoints.push(pp(cx, cy));
        }
        break;
      case 'h':
        while (i < tokens.length && isNumber(tokens[i])) {
          cx += nextNum();
          currentPoints.push(pp(cx, cy));
        }
        break;
      case 'V':
        while (i < tokens.length && isNumber(tokens[i])) {
          cy = nextNum();
          currentPoints.push(pp(cx, cy));
        }
        break;
      case 'v':
        while (i < tokens.length && isNumber(tokens[i])) {
          cy += nextNum();
          currentPoints.push(pp(cx, cy));
        }
        break;
      case 'C':
        while (i < tokens.length && isNumber(tokens[i])) {
          const c1x = nextNum(), c1y = nextNum();
          const c2x = nextNum(), c2y = nextNum();
          cx = nextNum(); cy = nextNum();
          currentPoints.push(pp(c1x, c1y, 'curveData'));
          currentPoints.push(pp(c2x, c2y, 'curveData'));
          currentPoints.push(pp(cx, cy, 'curveTo'));
          lastCx = c2x; lastCy = c2y;
        }
        break;
      case 'c':
        while (i < tokens.length && isNumber(tokens[i])) {
          const c1x = cx + nextNum(), c1y = cy + nextNum();
          const c2x = cx + nextNum(), c2y = cy + nextNum();
          cx += nextNum(); cy += nextNum();
          currentPoints.push(pp(c1x, c1y, 'curveData'));
          currentPoints.push(pp(c2x, c2y, 'curveData'));
          currentPoints.push(pp(cx, cy, 'curveTo'));
          lastCx = c2x; lastCy = c2y;
        }
        break;
      case 'S':
        while (i < tokens.length && isNumber(tokens[i])) {
          const c1x = 2 * cx - lastCx, c1y = 2 * cy - lastCy;
          const c2x = nextNum(), c2y = nextNum();
          cx = nextNum(); cy = nextNum();
          currentPoints.push(pp(c1x, c1y, 'curveData'));
          currentPoints.push(pp(c2x, c2y, 'curveData'));
          currentPoints.push(pp(cx, cy, 'curveTo'));
          lastCx = c2x; lastCy = c2y;
        }
        break;
      case 's':
        while (i < tokens.length && isNumber(tokens[i])) {
          const c1x = 2 * cx - lastCx, c1y = 2 * cy - lastCy;
          const c2x = cx + nextNum(), c2y = cy + nextNum();
          cx += nextNum(); cy += nextNum();
          currentPoints.push(pp(c1x, c1y, 'curveData'));
          currentPoints.push(pp(c2x, c2y, 'curveData'));
          currentPoints.push(pp(cx, cy, 'curveTo'));
          lastCx = c2x; lastCy = c2y;
        }
        break;
      case 'Q':
        while (i < tokens.length && isNumber(tokens[i])) {
          const qx = nextNum(), qy = nextNum();
          cx = nextNum(); cy = nextNum();
          currentPoints.push(pp(qx, qy, 'quadData'));
          currentPoints.push(pp(cx, cy, 'quadTo'));
          lastCx = qx; lastCy = qy;
        }
        break;
      case 'q':
        while (i < tokens.length && isNumber(tokens[i])) {
          const qx = cx + nextNum(), qy = cy + nextNum();
          cx += nextNum(); cy += nextNum();
          currentPoints.push(pp(qx, qy, 'quadData'));
          currentPoints.push(pp(cx, cy, 'quadTo'));
          lastCx = qx; lastCy = qy;
        }
        break;
      case 'T':
        while (i < tokens.length && isNumber(tokens[i])) {
          const qx = 2 * cx - lastCx, qy = 2 * cy - lastCy;
          cx = nextNum(); cy = nextNum();
          currentPoints.push(pp(qx, qy, 'quadData'));
          currentPoints.push(pp(cx, cy, 'quadTo'));
          lastCx = qx; lastCy = qy;
        }
        break;
      case 't':
        while (i < tokens.length && isNumber(tokens[i])) {
          const qx = 2 * cx - lastCx, qy = 2 * cy - lastCy;
          cx += nextNum(); cy += nextNum();
          currentPoints.push(pp(qx, qy, 'quadData'));
          currentPoints.push(pp(cx, cy, 'quadTo'));
          lastCx = qx; lastCy = qy;
        }
        break;
      case 'A':
      case 'a': {
        const isRel = cmd === 'a';
        while (i < tokens.length && isNumber(tokens[i])) {
          const arcRx = nextNum(), arcRy = nextNum();
          const xRotation = nextNum();
          const largeArc = nextNum();
          const sweep = nextNum();
          const ex = isRel ? cx + nextNum() : nextNum();
          const ey = isRel ? cy + nextNum() : nextNum();
          // Convert arc to cubic bezier curves
          const curves = arcToCubicBeziers(cx, cy, arcRx, arcRy, xRotation, !!largeArc, !!sweep, ex, ey);
          for (const curve of curves) {
            currentPoints.push(pp(curve[0], curve[1], 'curveData'));
            currentPoints.push(pp(curve[2], curve[3], 'curveData'));
            currentPoints.push(pp(curve[4], curve[5], 'curveTo'));
          }
          cx = ex; cy = ey;
        }
        break;
      }
      case 'Z':
      case 'z':
        cx = startX; cy = startY;
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

  return contours;
}

function isNumber(token: string): boolean {
  return /^[+-]?(\d+\.?\d*|\.\d+)([eE][+-]?\d+)?$/.test(token);
}

function tokenize(d: string): string[] {
  const tokens: string[] = [];
  const re = /([a-zA-Z])|([+-]?(?:\d+\.?\d*|\.\d+)(?:[eE][+-]?\d+)?)/g;
  let match: RegExpExecArray | null;
  while ((match = re.exec(d)) !== null) {
    tokens.push(match[0]);
  }
  return tokens;
}

// ─── Arc to Cubic Bezier Conversion ────────────────────
// SVG spec endpoint-to-center parameterization
function arcToCubicBeziers(
  x1: number, y1: number,
  rx: number, ry: number,
  angle: number,
  largeArcFlag: boolean, sweepFlag: boolean,
  x2: number, y2: number,
): number[][] {
  if (rx === 0 || ry === 0) return [];

  const phi = angle * Math.PI / 180;
  const cosPhi = Math.cos(phi);
  const sinPhi = Math.sin(phi);

  const dx = (x1 - x2) / 2;
  const dy = (y1 - y2) / 2;
  const x1p = cosPhi * dx + sinPhi * dy;
  const y1p = -sinPhi * dx + cosPhi * dy;

  rx = Math.abs(rx);
  ry = Math.abs(ry);

  // Ensure radii are large enough
  const lambda = (x1p * x1p) / (rx * rx) + (y1p * y1p) / (ry * ry);
  if (lambda > 1) {
    const sqrtLambda = Math.sqrt(lambda);
    rx *= sqrtLambda;
    ry *= sqrtLambda;
  }

  const rxSq = rx * rx;
  const rySq = ry * ry;
  const x1pSq = x1p * x1p;
  const y1pSq = y1p * y1p;

  let sq = Math.max(0, (rxSq * rySq - rxSq * y1pSq - rySq * x1pSq) / (rxSq * y1pSq + rySq * x1pSq));
  sq = Math.sqrt(sq);
  if (largeArcFlag === sweepFlag) sq = -sq;

  const cxp = sq * rx * y1p / ry;
  const cyp = -sq * ry * x1p / rx;

  const cx = cosPhi * cxp - sinPhi * cyp + (x1 + x2) / 2;
  const cy = sinPhi * cxp + cosPhi * cyp + (y1 + y2) / 2;

  function vectorAngle(ux: number, uy: number, vx: number, vy: number): number {
    const sign = ux * vy - uy * vx < 0 ? -1 : 1;
    const dot = ux * vx + uy * vy;
    const mag = Math.sqrt((ux * ux + uy * uy) * (vx * vx + vy * vy));
    return sign * Math.acos(Math.max(-1, Math.min(1, dot / mag)));
  }

  const theta1 = vectorAngle(1, 0, (x1p - cxp) / rx, (y1p - cyp) / ry);
  let dTheta = vectorAngle(
    (x1p - cxp) / rx, (y1p - cyp) / ry,
    (-x1p - cxp) / rx, (-y1p - cyp) / ry,
  );

  if (!sweepFlag && dTheta > 0) dTheta -= 2 * Math.PI;
  if (sweepFlag && dTheta < 0) dTheta += 2 * Math.PI;

  // Split arc into segments of max 90 degrees
  const segments = Math.ceil(Math.abs(dTheta) / (Math.PI / 2));
  const delta = dTheta / segments;
  const curves: number[][] = [];

  for (let i = 0; i < segments; i++) {
    const t1 = theta1 + i * delta;
    const t2 = theta1 + (i + 1) * delta;
    const alpha = 4 / 3 * Math.tan((t2 - t1) / 4);

    const cos1 = Math.cos(t1), sin1 = Math.sin(t1);
    const cos2 = Math.cos(t2), sin2 = Math.sin(t2);

    const ep1x = rx * cos1;
    const ep1y = ry * sin1;
    const ep2x = rx * cos2;
    const ep2y = ry * sin2;

    const q1x = ep1x - alpha * rx * sin1;
    const q1y = ep1y + alpha * ry * cos1;
    const q2x = ep2x + alpha * rx * sin2;
    const q2y = ep2y - alpha * ry * cos2;

    // Transform back from center parameterization
    curves.push([
      cosPhi * q1x - sinPhi * q1y + cx,
      sinPhi * q1x + cosPhi * q1y + cy,
      cosPhi * q2x - sinPhi * q2y + cx,
      sinPhi * q2x + cosPhi * q2y + cy,
      cosPhi * ep2x - sinPhi * ep2y + cx,
      sinPhi * ep2x + cosPhi * ep2y + cy,
    ]);
  }

  return curves;
}
