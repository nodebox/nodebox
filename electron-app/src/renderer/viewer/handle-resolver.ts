import type { Node } from '../types/node';
import { getPoint, getFloat, isPoint, isFloat, isInt } from '../types/value';
import type { FourPointHandleState } from './four-point-handle';

const HANDLE_PROTOTYPES = new Set(['corevector.rect', 'corevector.ellipse']);

export function resolveFourPointHandle(
  nodeName: string | null,
  children: Node[],
): FourPointHandleState | null {
  if (!nodeName) return null;

  const node = children.find((n) => n.name === nodeName);
  if (!node || !node.prototype || !HANDLE_PROTOTYPES.has(node.prototype)) {
    return null;
  }

  const posPort = node.inputs.find((p) => p.name === 'position');
  const widthPort = node.inputs.find((p) => p.name === 'width');
  const heightPort = node.inputs.find((p) => p.name === 'height');

  const center =
    posPort && isPoint(posPort.value)
      ? getPoint(posPort.value)
      : { x: 0, y: 0 };
  const width =
    widthPort && (isFloat(widthPort.value) || isInt(widthPort.value))
      ? getFloat(widthPort.value)
      : 100;
  const height =
    heightPort && (isFloat(heightPort.value) || isInt(heightPort.value))
      ? getFloat(heightPort.value)
      : 100;

  return {
    nodeName: node.name,
    center,
    width,
    height,
  };
}
