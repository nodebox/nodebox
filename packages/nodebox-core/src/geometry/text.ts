import type { Point } from './point.js';
import type { Color } from './color.js';

export type TextAlign = 'left' | 'center' | 'right';

export interface Text {
  text: string;
  position: Point;
  fontFamily: string;
  fontSize: number;
  align: TextAlign;
  fill: Color | null;
}
