import type { Value } from '../node/value.js';

export type NodeFunction = (...args: Value[]) => Value | Value[];

export interface FunctionRegistry {
  get(name: string): NodeFunction | undefined;
  register(name: string, fn: NodeFunction): void;
  has(name: string): boolean;
  names(): string[];
}

export function createFunctionRegistry(): FunctionRegistry {
  const functions = new Map<string, NodeFunction>();
  return {
    get(name: string) { return functions.get(name); },
    register(name: string, fn: NodeFunction) { functions.set(name, fn); },
    has(name: string) { return functions.has(name); },
    names() { return [...functions.keys()]; },
  };
}
