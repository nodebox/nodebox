import type { Point } from '../types/geometry';

export const HANDLE_SIZE = 6;
export const HANDLE_HIT_RADIUS = 12;

export type FourPointDragTarget =
  | 'none'
  | 'topLeft'
  | 'topRight'
  | 'bottomRight'
  | 'bottomLeft'
  | 'center';

export interface FourPointHandleState {
  nodeName: string;
  center: Point;
  width: number;
  height: number;
}

export function corners(h: FourPointHandleState): [Point, Point, Point, Point] {
  const hw = h.width / 2;
  const hh = h.height / 2;
  return [
    { x: h.center.x - hw, y: h.center.y - hh }, // TL
    { x: h.center.x + hw, y: h.center.y - hh }, // TR
    { x: h.center.x + hw, y: h.center.y + hh }, // BR
    { x: h.center.x - hw, y: h.center.y + hh }, // BL
  ];
}

export function hitTest(
  h: FourPointHandleState,
  screenX: number,
  screenY: number,
  worldToScreen: (wx: number, wy: number) => { x: number; y: number },
): FourPointDragTarget {
  const [tl, tr, br, bl] = corners(h);
  const targets: { target: FourPointDragTarget; pt: Point }[] = [
    { target: 'topLeft', pt: tl },
    { target: 'topRight', pt: tr },
    { target: 'bottomRight', pt: br },
    { target: 'bottomLeft', pt: bl },
  ];

  // Check corners first
  for (const { target, pt } of targets) {
    const s = worldToScreen(pt.x, pt.y);
    const dx = screenX - s.x;
    const dy = screenY - s.y;
    if (dx * dx + dy * dy <= HANDLE_HIT_RADIUS * HANDLE_HIT_RADIUS) {
      return target;
    }
  }

  // Check interior (bounding box in screen space)
  const sTL = worldToScreen(tl.x, tl.y);
  const sBR = worldToScreen(br.x, br.y);
  const minX = Math.min(sTL.x, sBR.x);
  const maxX = Math.max(sTL.x, sBR.x);
  const minY = Math.min(sTL.y, sBR.y);
  const maxY = Math.max(sTL.y, sBR.y);
  if (screenX >= minX && screenX <= maxX && screenY >= minY && screenY <= maxY) {
    return 'center';
  }

  return 'none';
}

export function drawFourPointHandle(
  ctx: CanvasRenderingContext2D,
  h: FourPointHandleState,
  color: string,
  worldToScreen: (wx: number, wy: number) => { x: number; y: number },
) {
  const [tl, tr, br, bl] = corners(h);
  const sTL = worldToScreen(tl.x, tl.y);
  const sTR = worldToScreen(tr.x, tr.y);
  const sBR = worldToScreen(br.x, br.y);
  const sBL = worldToScreen(bl.x, bl.y);

  // Draw bounding box lines (1px)
  ctx.strokeStyle = color;
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.moveTo(sTL.x, sTL.y);
  ctx.lineTo(sTR.x, sTR.y);
  ctx.lineTo(sBR.x, sBR.y);
  ctx.lineTo(sBL.x, sBL.y);
  ctx.closePath();
  ctx.stroke();

  // Draw corner and center squares (filled)
  const half = HANDLE_SIZE / 2;
  const sCenter = worldToScreen(h.center.x, h.center.y);
  ctx.fillStyle = color;
  for (const s of [sTL, sTR, sBR, sBL, sCenter]) {
    ctx.fillRect(s.x - half, s.y - half, HANDLE_SIZE, HANDLE_SIZE);
  }
}

export function applyDrag(
  target: FourPointDragTarget,
  startValues: { center: Point; width: number; height: number },
  worldDx: number,
  worldDy: number,
) {
  if (target === 'center') {
    return {
      center: {
        x: startValues.center.x + worldDx,
        y: startValues.center.y + worldDy,
      },
      width: startValues.width,
      height: startValues.height,
    };
  }

  // Corner drags: symmetric around center
  let dw = 0;
  let dh = 0;
  switch (target) {
    case 'topLeft':
      dw = -worldDx * 2;
      dh = -worldDy * 2;
      break;
    case 'topRight':
      dw = worldDx * 2;
      dh = -worldDy * 2;
      break;
    case 'bottomRight':
      dw = worldDx * 2;
      dh = worldDy * 2;
      break;
    case 'bottomLeft':
      dw = -worldDx * 2;
      dh = worldDy * 2;
      break;
  }

  return {
    center: startValues.center,
    width: Math.max(1, startValues.width + dw),
    height: Math.max(1, startValues.height + dh),
  };
}
