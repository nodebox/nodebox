import type { Node } from '../types/node';
import type { FourPointHandleState } from './four-point-handle';

const HANDLE_PROTOTYPES = new Set(['corevector.rect', 'corevector.ellipse']);

export function resolveFourPointHandle(
  renderedChild: string | null,
  children: Node[],
): FourPointHandleState | null {
  if (!renderedChild) return null;

  const node = children.find((n) => n.name === renderedChild);
  if (!node || !node.prototype || !HANDLE_PROTOTYPES.has(node.prototype)) {
    return null;
  }

  const posPort = node.inputs.find((p) => p.name === 'position');
  const widthPort = node.inputs.find((p) => p.name === 'width');
  const heightPort = node.inputs.find((p) => p.name === 'height');

  const center =
    posPort?.value.type === 'point'
      ? posPort.value.value
      : { x: 0, y: 0 };
  const width =
    widthPort?.value.type === 'float' || widthPort?.value.type === 'int'
      ? widthPort.value.value
      : 100;
  const height =
    heightPort?.value.type === 'float' || heightPort?.value.type === 'int'
      ? heightPort.value.value
      : 100;

  return {
    nodeName: node.name,
    center,
    width,
    height,
  };
}
