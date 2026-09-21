// A port of nodebox.function.CoreFunctions (namespace "core"), always available.

import { JavaScriptLibrary } from "../runtime/function-repository";
import type { NodeContext } from "../runtime/context";

export function zero(): number {
  return 0;
}

export function frame(context: NodeContext): number {
  return context.frame;
}

export const coreLibrary = new JavaScriptLibrary("core", { zero, frame });
