import { Paint } from "./paint";
import { Point } from "./point";
import { Shape, ShapeType } from "./shape";
import { Transform } from "./transform";

export enum Verb {
  Move = "M",
  Line = "L",
  Quad = "Q",
  Cubic = "C",
  Close = "Z",
}

interface Segment {
  command: string;
  params: number[];
}

// prettier-ignore
const PARAM_COUNTS: { [key: string]: number } = {
  A: 7, a: 7,
  C: 6, c: 6,
  H: 1, h: 1,
  L: 2, l: 2,
  M: 2, m: 2,
  Q: 4, q: 4,
  S: 4, s: 4,
  T: 2, t: 2,
  V: 1, v: 1,
  Z: 0, z: 0,
};

export function pathDataToSegments(pathData: string): Segment[] {
  // Ensure spaces between letters and numbers
  // Otherwise 0-.227 would be parsed as [0, 0.227]
  pathData = pathData.replace(/([^\se])-/g, "$1 -");
  const tokens = pathData.match(/[a-zA-Z]|[-+]?(?:0|[1-9]\d*)(?:\.\d*)?(?:[eE][-+]?\d+)?|[-+]?\.\d+(?:[eE][-+]?\d+)?/g);

  if (!tokens) {
    return [];
  }

  let index = 0;
  let mode = "BOD";
  let token = tokens[index];
  const segments: Segment[] = [];
  while (token !== undefined) {
    let paramCount = 0;
    let params: number[] = [];
    if (mode === "BOD") {
      // Beginning of a path, only "M" and "m" commands are allowed
      if (token === "M" || token === "m") {
        index++;
        paramCount = PARAM_COUNTS[token];
        mode = token;
      } else {
        throw new Error("Invalid path data: path must begin with a moveto command");
      }
    } else if (/[a-zA-Z]/.test(token)) {
      // The token is a command, so we're switching modes
      index++;
      mode = token;
      paramCount = PARAM_COUNTS[token];
    } else {
      // The token is a number, so we're continuing with the current mode
      paramCount = PARAM_COUNTS[mode];
    }
    if (index + paramCount > tokens.length) {
      throw new Error(`Invalid path data: not enough parameters for command ${mode}`);
    }
    for (let i = index; i < index + paramCount; i++) {
      params.push(+tokens[i]);
    }
    const segment: Segment = { command: mode, params };
    segments.push(segment);
    index += paramCount;
    token = tokens[index];
    if (mode === "M") mode = "L";
    if (mode === "m") mode = "l";
  }
  return segments;
}

export function segmentsToPathData(segments: Segment[]): string {
  return segments.map((s) => s.command + s.params.join(" ")).join(" ");
}

function arcToBezier(
  x1: number,
  y1: number,
  x4: number,
  y4: number,
  rx: number,
  ry: number,
  xAxisRotation: number,
  largeArcFlag: boolean,
  sweepFlag: boolean,
): Point[] {
  // Ensure non-zero radii
  if (rx === 0 || ry === 0) {
    return []; // Return empty array to indicate straight line
  }

  // Convert rotation angle from degrees to radians
  const phi = (xAxisRotation * Math.PI) / 180;
  const sinPhi = Math.sin(phi);
  const cosPhi = Math.cos(phi);

  // Step 1: Compute (x1′, y1′)
  const x1p = (cosPhi * (x1 - x4)) / 2 + (sinPhi * (y1 - y4)) / 2;
  const y1p = (-sinPhi * (x1 - x4)) / 2 + (cosPhi * (y1 - y4)) / 2;

  // Step 2: Compute (cx′, cy′)
  let rxSq = rx * rx;
  let rySq = ry * ry;
  const x1pSq = x1p * x1p;
  const y1pSq = y1p * y1p;

  // Check if the radii are too small and scale them if necessary
  const radiiScale = x1pSq / rxSq + y1pSq / rySq;
  if (radiiScale > 1) {
    rx *= Math.sqrt(radiiScale);
    ry *= Math.sqrt(radiiScale);
    rxSq = rx * rx;
    rySq = ry * ry;
  }

  const sign = largeArcFlag === sweepFlag ? -1 : 1;
  const sq = Math.max((rxSq * rySq - rxSq * y1pSq - rySq * x1pSq) / (rxSq * y1pSq + rySq * x1pSq), 0);
  const coef = sign * Math.sqrt(sq);
  const cxp = coef * ((rx * y1p) / ry);
  const cyp = coef * (-(ry * x1p) / rx);

  // Step 3: Compute (cx, cy) from (cx′, cy′)
  const cx = cosPhi * cxp - sinPhi * cyp + (x1 + x4) / 2;
  const cy = sinPhi * cxp + cosPhi * cyp + (y1 + y4) / 2;

  // Step 4: Compute the angleStart and angleExtent
  const ux = (x1p - cxp) / rx;
  const uy = (y1p - cyp) / ry;
  const vx = (-x1p - cxp) / rx;
  const vy = (-y1p - cyp) / ry;

  const angleStart = Math.atan2(uy, ux);
  let angleExtent = Math.atan2(vy, vx) - angleStart;

  // Ensure angleExtent is in the correct direction
  if (!sweepFlag && angleExtent > 0) {
    angleExtent -= 2 * Math.PI;
  } else if (sweepFlag && angleExtent < 0) {
    angleExtent += 2 * Math.PI;
  }

  // The maximum angle for a single cubic Bézier curve to approximate an arc
  const MAX_ANGLE = Math.PI / 2;

  // Split arc into multiple segments if angle is too large
  const segments = Math.max(Math.ceil(Math.abs(angleExtent) / MAX_ANGLE), 1);
  const deltaAngle = angleExtent / segments;
  const curves: Point[] = [];

  for (let i = 0; i < segments; i++) {
    const angle = angleStart + i * deltaAngle;
    const nextAngle = angle + deltaAngle;

    // Length of the tangent for the cubic Bézier approximation
    // This magic number gives the best approximation for circular arcs
    const alpha = (Math.sin(deltaAngle) * (Math.sqrt(4 + 3 * Math.pow(Math.tan(deltaAngle / 2), 2)) - 1)) / 3;

    // Calculate points and control points
    const sinAngle = Math.sin(angle);
    const cosAngle = Math.cos(angle);
    const sinAngleNext = Math.sin(nextAngle);
    const cosAngleNext = Math.cos(nextAngle);

    // Start point
    const p1x = cx + (cosPhi * rx * cosAngle - sinPhi * ry * sinAngle);
    const p1y = cy + (sinPhi * rx * cosAngle + cosPhi * ry * sinAngle);

    // End point
    const p2x = cx + (cosPhi * rx * cosAngleNext - sinPhi * ry * sinAngleNext);
    const p2y = cy + (sinPhi * rx * cosAngleNext + cosPhi * ry * sinAngleNext);

    // Control points
    const c1x = p1x - alpha * (cosPhi * rx * sinAngle + sinPhi * ry * cosAngle);
    const c1y = p1y - alpha * (sinPhi * rx * sinAngle - cosPhi * ry * cosAngle);
    const c2x = p2x + alpha * (cosPhi * rx * sinAngleNext + sinPhi * ry * cosAngleNext);
    const c2y = p2y + alpha * (sinPhi * rx * sinAngleNext - cosPhi * ry * cosAngleNext);

    curves.push(new Point(c1x, c1y), new Point(c2x, c2y), new Point(p2x, p2y));
  }

  return curves;
}

export class Path extends Shape {
  verbs: Verb[];
  points: Point[];

  constructor() {
    super(ShapeType.Path);
    this.verbs = new Array();
    this.points = new Array();
    this.fill = Paint.black();
  }

  clone(): Shape {
    const path = new Path();
    path.verbs = this.verbs.slice();
    path.points = this.points.map((point) => point.clone());
    path._cloneAttributes(this);
    return path;
  }

  _updateAttributes(updates: { [key: string]: any }): void {
    Object.entries(updates).forEach(([key, value]) => {
      if (value !== undefined) {
        (this as any)[key] = value;
      }
    });
    return;
  }

  static rect(x: number, y: number, width: number, height: number): Path {
    const path = new Path();
    path.moveTo(x, y);
    path.lineTo(x + width, y);
    path.lineTo(x + width, y + height);
    path.lineTo(x, y + height);
    path.close();
    return path;
  }

  /**
   * Parse the given path data string and return a new Path object.
   * If the path data string is invalid, an empty path is returned.
   * @param pathData The SVG path data string
   * @returns A new Path object
   */
  static fromPathData(pathData: string): Path {
    const path = new Path();

    const tokens = pathData.match(/[a-zA-Z]|[\-+]?(?:0|[1-9]\d*)(?:\.\d*)?(?:[eE][\-+]?\d+)?|\.\d+(?:[eE][\-+]?\d+)?/g);

    if (!tokens) {
      return new Path();
    }
    let segments = pathDataToSegments(pathData);

    let currentX = 0;
    let currentY = 0;
    let startX = 0;
    let startY = 0;
    let prevControlX, prevControlY;
    for (const { command, params } of segments) {
      switch (command) {
        case "M":
          [currentX, currentY] = params;
          [startX, startY] = params;
          path.verbs.push(Verb.Move);
          path.points.push(new Point(currentX, currentY));
          break;
        case "m":
          currentX += params[0];
          currentY += params[1];
          [startX, startY] = [currentX, currentY];
          path.verbs.push(Verb.Move);
          path.points.push(new Point(currentX, currentY));
          break;
        case "L":
          [currentX, currentY] = params;
          path.verbs.push(Verb.Line);
          path.points.push(new Point(currentX, currentY));
          break;
        case "l":
          currentX += params[0];
          currentY += params[1];
          path.verbs.push(Verb.Line);
          path.points.push(new Point(currentX, currentY));
          break;
        case "H":
          currentX = params[0];
          path.verbs.push(Verb.Line);
          path.points.push(new Point(currentX, currentY));
          break;
        case "h":
          currentX += params[0];
          path.verbs.push(Verb.Line);
          path.points.push(new Point(currentX, currentY));
          break;
        case "V":
          currentY = params[0];
          path.verbs.push(Verb.Line);
          path.points.push(new Point(currentX, currentY));
          break;
        case "v":
          currentY += params[0];
          path.verbs.push(Verb.Line);
          path.points.push(new Point(currentX, currentY));
          break;
        case "Z":
        case "z":
          currentX = startX;
          currentY = startY;
          path.verbs.push(Verb.Close);
          break;
        case "Q":
          path.verbs.push(Verb.Quad);
          path.points.push(new Point(params[0], params[1]));
          path.points.push(new Point(params[2], params[3]));
          currentX = params[2];
          currentY = params[3];
          break;
        case "q":
          path.verbs.push(Verb.Quad);
          path.points.push(new Point(currentX + params[0], currentY + params[1]));
          path.points.push(new Point(currentX + params[2], currentY + params[3]));
          currentX += params[2];
          currentY += params[3];
          break;
        case "T":
          // Get previous control point
          prevControlX = currentX;
          prevControlY = currentY;
          if (path.verbs[path.verbs.length - 1] === Verb.Quad) {
            const lastPoint = path.points[path.points.length - 1];
            const lastControl = path.points[path.points.length - 2];
            prevControlX = 2 * lastPoint.x - lastControl.x;
            prevControlY = 2 * lastPoint.y - lastControl.y;
          }
          path.verbs.push(Verb.Quad);
          path.points.push(new Point(prevControlX, prevControlY));
          path.points.push(new Point(params[0], params[1]));
          currentX = params[0];
          currentY = params[1];
          break;
        case "t":
          // Get previous control point
          prevControlX = currentX;
          prevControlY = currentY;
          if (path.verbs[path.verbs.length - 1] === Verb.Quad) {
            const lastPoint = path.points[path.points.length - 1];
            const lastControl = path.points[path.points.length - 2];
            prevControlX = 2 * lastPoint.x - lastControl.x;
            prevControlY = 2 * lastPoint.y - lastControl.y;
          }
          path.verbs.push(Verb.Quad);
          path.points.push(new Point(prevControlX, prevControlY));
          path.points.push(new Point(currentX + params[0], currentY + params[1]));
          currentX += params[0];
          currentY += params[1];
          break;
        case "C":
          path.verbs.push(Verb.Cubic);
          path.points.push(new Point(params[0], params[1]));
          path.points.push(new Point(params[2], params[3]));
          path.points.push(new Point(params[4], params[5]));
          currentX = params[4];
          currentY = params[5];
          break;
        case "c":
          path.verbs.push(Verb.Cubic);
          path.points.push(new Point(currentX + params[0], currentY + params[1]));
          path.points.push(new Point(currentX + params[2], currentY + params[3]));
          path.points.push(new Point(currentX + params[4], currentY + params[5]));
          currentX += params[4];
          currentY += params[5];
          break;
        case "S":
          // Get previous control point
          prevControlX = currentX;
          prevControlY = currentY;
          if (path.verbs[path.verbs.length - 1] === Verb.Cubic) {
            const lastPoint = path.points[path.points.length - 1];
            const lastControl = path.points[path.points.length - 2];
            prevControlX = 2 * lastPoint.x - lastControl.x;
            prevControlY = 2 * lastPoint.y - lastControl.y;
          }
          path.verbs.push(Verb.Cubic);
          path.points.push(new Point(prevControlX, prevControlY));
          path.points.push(new Point(params[0], params[1]));
          path.points.push(new Point(params[2], params[3]));
          currentX = params[2];
          currentY = params[3];
          break;
        case "s":
          // Get previous control point
          prevControlX = currentX;
          prevControlY = currentY;
          if (path.verbs[path.verbs.length - 1] === Verb.Cubic) {
            const lastPoint = path.points[path.points.length - 1];
            const lastControl = path.points[path.points.length - 2];
            prevControlX = 2 * lastPoint.x - lastControl.x;
            prevControlY = 2 * lastPoint.y - lastControl.y;
          }
          path.verbs.push(Verb.Cubic);
          path.points.push(new Point(prevControlX, prevControlY));
          path.points.push(new Point(currentX + params[0], currentY + params[1]));
          path.points.push(new Point(currentX + params[2], currentY + params[3]));
          currentX += params[2];
          currentY += params[3];
          break;
        case "A":
          {
            const rx = params[0];
            const ry = params[1];
            const xAxisRotation = params[2];
            const largeArcFlag = params[3] === 1;
            const sweepFlag = params[4] === 1;
            const x = params[5];
            const y = params[6];
            const curves = arcToBezier(currentX, currentY, x, y, rx, ry, xAxisRotation, largeArcFlag, sweepFlag);
            for (let j = 0; j < curves.length; j += 3) {
              path.verbs.push(Verb.Cubic);
              path.points.push(curves[j]);
              path.points.push(curves[j + 1]);
              path.points.push(curves[j + 2]);
            }
            currentX = x;
            currentY = y;
          }
          break;
        case "a":
          {
            const rx = params[0];
            const ry = params[1];
            const xAxisRotation = params[2];
            const largeArcFlag = params[3] === 1;
            const sweepFlag = params[4] === 1;
            const x = currentX + params[5];
            const y = currentY + params[6];
            const curves = arcToBezier(currentX, currentY, x, y, rx, ry, xAxisRotation, largeArcFlag, sweepFlag);
            for (let j = 0; j < curves.length; j += 3) {
              path.verbs.push(Verb.Cubic);
              path.points.push(curves[j]);
              path.points.push(curves[j + 1]);
              path.points.push(curves[j + 2]);
            }
            currentX = x;
            currentY = y;
          }
          break;
      }
    }

    return path;
  }

  toPathData(applyTransform?: boolean, parentMatrix?: Float32Array): string {
    function _apply(x: number, y: number): { x: number; y: number } {
      if (!applyTransform || isIdentity) {
        return { x, y };
      } else {
        return Transform.applyMatrixTransform(x, y, matrix);
      }
    }

    const matrix = parentMatrix
      ? Transform.multiplyMatrices(parentMatrix, this.transform.matrix)
      : this.transform.matrix;
    const isIdentity = Transform.matrixIsIdentity(matrix);

    const parts = [];
    let pointIndex = 0;
    const len = this.verbs.length;
    for (let i = 0; i < len; i++) {
      const verb = this.verbs[i];
      switch (verb) {
        case "M":
        case "L":
          {
            const { x, y } = _apply(this.points[pointIndex].x, this.points[pointIndex].y);
            parts.push(verb, x, y);
            pointIndex++;
          }
          break;
        case "C":
          {
            const { x: x1, y: y1 } = _apply(this.points[pointIndex].x, this.points[pointIndex].y);
            const { x: x2, y: y2 } = _apply(this.points[pointIndex + 1].x, this.points[pointIndex + 1].y);
            const { x: x3, y: y3 } = _apply(this.points[pointIndex + 2].x, this.points[pointIndex + 2].y);
            parts.push(verb, x1, y1, x2, y2, x3, y3);
            pointIndex += 3;
          }
          break;
        case "Z":
          parts.push(verb);
          break;
        default:
          console.warn("Unsupported path command:", verb);
      }
    }

    return parts.join(" ");
  }

  moveTo(x: number, y: number): void {
    this.verbs.push(Verb.Move);
    this.points.push(new Point(x, y));
  }

  lineTo(x: number, y: number): void {
    this.verbs.push(Verb.Line);
    this.points.push(new Point(x, y));
  }

  quadTo(x1: number, y1: number, x: number, y: number): void {
    this.verbs.push(Verb.Quad);
    this.points.push(new Point(x1, y1));
    this.points.push(new Point(x, y));
  }

  cubicTo(x1: number, y1: number, x2: number, y2: number, x: number, y: number): void {
    this.verbs.push(Verb.Cubic);
    this.points.push(new Point(x1, y1));
    this.points.push(new Point(x2, y2));
    this.points.push(new Point(x, y));
  }

  curveTo(x1: number, y1: number, x2: number, y2: number, x: number, y: number): void {
    this.cubicTo(x1, y1, x2, y2, x, y);
  }

  bezierCurveTo(x1: number, y1: number, x2: number, y2: number, x: number, y: number): void {
    this.cubicTo(x1, y1, x2, y2, x, y);
  }

  close(): void {
    this.verbs.push(Verb.Close);
  }

  closePath(): void {
    this.verbs.push(Verb.Close);
  }

  rect(x: number, y: number, width: number, height: number): void {
    this.moveTo(x, y);
    this.lineTo(x + width, y);
    this.lineTo(x + width, y + height);
    this.lineTo(x, y + height);
    this.close();
  }

  circle(cx: number, cy: number, radius: number): void {
    // The magic number 0.552284749831 is calculated as 4/3 * tan(π/8)
    // This gives the best approximation of a circle using 4 cubic Bézier curves
    const offset = radius * 0.552284749831;

    // Start at rightmost point
    this.moveTo(cx + radius, cy);

    // Draw four cubic Bézier curves, one for each quarter of the circle

    // First quadrant (right to bottom)
    this.cubicTo(
      cx + radius,
      cy + offset, // First control point
      cx + offset,
      cy + radius, // Second control point
      cx,
      cy + radius, // End point
    );

    // Second quadrant (bottom to left)
    this.cubicTo(
      cx - offset,
      cy + radius, // First control point
      cx - radius,
      cy + offset, // Second control point
      cx - radius,
      cy, // End point
    );

    // Third quadrant (left to top)
    this.cubicTo(
      cx - radius,
      cy - offset, // First control point
      cx - offset,
      cy - radius, // Second control point
      cx,
      cy - radius, // End point
    );

    // Fourth quadrant (top to right)
    this.cubicTo(
      cx + offset,
      cy - radius, // First control point
      cx + radius,
      cy - offset, // Second control point
      cx + radius,
      cy, // End point
    );

    // Close the path
    this.close();
  }

  // Usage example:
  // const path = new Path();
  // circle(path, 100, 100, 50); // Draws a circle at (100,100) with radius 50
  //
  arc(x: number, y: number, radius: number, startAngle: number, endAngle: number): void {
    // Make sure the start is at the top
    startAngle -= Math.PI / 2;
    endAngle -= Math.PI / 2;

    // Normalize angles
    while (endAngle <= startAngle) {
      endAngle += 2 * Math.PI;
    }

    const isFullCircle = Math.abs(endAngle - startAngle - 2 * Math.PI) < 1e-6;

    if (isFullCircle) {
      this.moveTo(x + radius, y);
      this.drawFullCircle(x, y, radius, true);
    } else {
      const startSin = Math.sin(startAngle);
      const startCos = Math.cos(startAngle);
      const endSin = Math.sin(endAngle);
      const endCos = Math.cos(endAngle);

      const start = new Point(x + startCos * radius, y + startSin * radius);
      const end = new Point(x + endCos * radius, y + endSin * radius);

      this.moveTo(start.x, start.y);
      this.drawArcSegment(start.x, start.y, end.x, end.y, radius, endAngle - startAngle > Math.PI, true);
      this.lineTo(x, y);
    }

    this.close();
  }

  complexArc(
    x: number,
    y: number,
    innerRadius: number,
    outerRadius: number,
    startAngle: number,
    endAngle: number,
  ): void {
    startAngle -= Math.PI / 2;
    endAngle -= Math.PI / 2;
    // Step 1: Normalize angles
    while (endAngle <= startAngle) {
      endAngle += 2 * Math.PI;
    }

    // Step 2: Check if we're drawing a full circle
    const isFullCircle = Math.abs(endAngle - startAngle - 2 * Math.PI) < 1e-6;

    // Step 3: Calculate start/end points for the arc
    if (isFullCircle) {
      // Step 4a: Handle full circle case
      if (innerRadius > 0) {
        // Draw outer circle
        this.moveTo(x + outerRadius, y);
        this.drawFullCircle(x, y, outerRadius, true);
        // Draw inner circle
        this.moveTo(x + innerRadius, y);
        this.drawFullCircle(x, y, innerRadius, false);
      } else {
        // Draw simple full circle
        this.moveTo(x + outerRadius, y);
        this.drawFullCircle(x, y, outerRadius, true);
      }
    } else {
      // Step 4b: Handle partial arc case
      const startSin = Math.sin(startAngle);
      const startCos = Math.cos(startAngle);
      const endSin = Math.sin(endAngle);
      const endCos = Math.cos(endAngle);

      const outerStart = new Point(x + startCos * outerRadius, y + startSin * outerRadius);
      const outerEnd = new Point(x + endCos * outerRadius, y + endSin * outerRadius);

      this.moveTo(outerStart.x, outerStart.y);

      if (innerRadius > 0) {
        // Draw outer arc
        this.drawArcSegment(
          outerStart.x,
          outerStart.y,
          outerEnd.x,
          outerEnd.y,
          outerRadius,
          endAngle - startAngle > Math.PI,
          true,
        );

        // Draw inner arc
        const innerStart = new Point(x + endCos * innerRadius, y + endSin * innerRadius);
        const innerEnd = new Point(x + startCos * innerRadius, y + startSin * innerRadius);

        this.lineTo(innerStart.x, innerStart.y);
        this.drawArcSegment(
          innerStart.x,
          innerStart.y,
          innerEnd.x,
          innerEnd.y,
          innerRadius,
          endAngle - startAngle > Math.PI,
          false,
        );
      } else {
        // Draw simple arc
        this.drawArcSegment(
          outerStart.x,
          outerStart.y,
          outerEnd.x,
          outerEnd.y,
          outerRadius,
          endAngle - startAngle > Math.PI,
          true,
        );
        this.lineTo(x, y);
      }
    }

    this.close();
  }

  private drawFullCircle(x: number, y: number, radius: number, clockwise: boolean): void {
    // Split circle into two semicircles for better numerical stability
    const startPoint = new Point(x + radius, y); // Point at 0 degrees
    const midPoint = new Point(x - radius, y); // Point at 180 degrees

    this.drawArcSegment(startPoint.x, startPoint.y, midPoint.x, midPoint.y, radius, true, clockwise);
    this.drawArcSegment(midPoint.x, midPoint.y, startPoint.x, startPoint.y, radius, true, clockwise);
  }

  private drawArcSegment(
    startX: number,
    startY: number,
    endX: number,
    endY: number,
    radius: number,
    largeArc: boolean,
    clockwise: boolean,
  ): void {
    const curves = arcToBezier(startX, startY, endX, endY, radius, radius, 0, largeArc, clockwise);
    for (let i = 0; i < curves.length; i += 3) {
      this.cubicTo(curves[i].x, curves[i].y, curves[i + 1].x, curves[i + 1].y, curves[i + 2].x, curves[i + 2].y);
    }
  }

  getBounds() {
    if (this.points.length === 0) {
      return { left: 0, right: 0, top: 0, bottom: 0, centerX: 0, centerY: 0 };
    }

    const strokeWidth = this.strokeWidth || 0;

    // Transform all points in the path
    const matrix = this.transform.matrix;
    let transformedPoints = this.points.map((point) => Transform.applyMatrixTransform(point.x, point.y, matrix));

    // Compute the bounding box for transformed points
    let left = transformedPoints[0].x;
    let right = transformedPoints[0].x;
    let top = transformedPoints[0].y;
    let bottom = transformedPoints[0].y;

    for (const point of transformedPoints) {
      left = Math.min(left, point.x);
      right = Math.max(right, point.x);
      top = Math.min(top, point.y);
      bottom = Math.max(bottom, point.y);
    }

    // Apply transformation to the strokeWidth (assuming uniform scaling)
    const scaleFactor = Math.sqrt(matrix[0] * matrix[0] + matrix[1] * matrix[1]);
    const transformedStrokeWidth = strokeWidth * scaleFactor;

    left -= transformedStrokeWidth / 2;
    right += transformedStrokeWidth / 2;
    top -= transformedStrokeWidth / 2;
    bottom += transformedStrokeWidth / 2;

    const centerX = (left + right) / 2;
    const centerY = (top + bottom) / 2;

    return { left, right, top, bottom, centerX, centerY };
  }

  bakeTransform(parentMatrix?: Float32Array): void {
    const matrix = parentMatrix
      ? Transform.multiplyMatrices(parentMatrix, this.transform.matrix)
      : this.transform.matrix;
    const t = new Transform(matrix);
    this.points = this.points.map(t.transformPoint.bind(t));
    this.transform = new Transform();
  }
}
