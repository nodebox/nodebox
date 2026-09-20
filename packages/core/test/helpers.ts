import { expect } from "vitest";
import {
  FunctionRepository,
  JavaScriptLibrary,
  Library,
  Node,
  NodeContext,
  Port,
  PortRange,
  PortType,
  builtinFunctionRepository,
  connect,
  createLibrary,
  createNetworkNode,
  createPort,
  createRootNode,
  extendNode,
  publish,
  setInputValue,
} from "../src";

export function rootNode(name: string, fn: string, ...inputs: Port[]): Node {
  const node = createRootNode();
  node.name = name;
  node.function = fn;
  node.inputs = inputs;
  return node;
}

export function networkNode(name: string, ...children: Node[]): Node {
  const net = createNetworkNode();
  net.name = name;
  for (const child of children) net.children.push(child);
  return net;
}

export function port(name: string, type: PortType, value?: unknown, range: PortRange = "value"): Port {
  const p = createPort(name, type, { range });
  if (value !== undefined) p.value = value as Port["value"];
  return p;
}

export function instance(prototype: Node, name: string, values: Record<string, unknown> = {}): Node {
  const node = extendNode(prototype, null, name);
  for (const [k, v] of Object.entries(values)) setInputValue(node, k, v);
  return node;
}

export const numberNode = rootNode("number", "math/number", port("number", "float", 0));
export const addNode = rootNode("add", "math/add", port("v1", "float", 0), port("v2", "float", 0));
export const negateNode = rootNode("negate", "math/negate", port("value", "float", 0));
export const makeNumbersNode = (() => {
  const n = rootNode("makeNumbers", "math/makeNumbers", port("string", "string", ""), port("separator", "string", " "));
  n.outputRange = "list";
  return n;
})();
export const makeStringsNode = (() => {
  const n = rootNode("makeStrings", "string/makeStrings", port("string", "string", "Alpha;Beta;Gamma"), port("separator", "string", ";"));
  n.outputRange = "list";
  return n;
})();

export const sideEffects = { theInput: 0, theOutput: 0, theCounter: 0, reset() { this.theInput = 0; this.theOutput = 0; this.theCounter = 0; } };

export const testLibraries = new JavaScriptLibrary("side-effects", {
  getNumber: () => sideEffects.theInput,
  setNumber: (n: number) => {
    sideEffects.theOutput = n;
  },
  increaseAndCount: (n: number) => {
    sideEffects.theCounter++;
    return n + 1;
  },
});
export const testFunctions = new JavaScriptLibrary("test", {
  makeNull: () => null,
  makeNestedWords: () => [
    ["alpha", "beta-gamma-1", "delta-epsi"],
    ["zeta-1", "etaa", "thet"],
    ["iotaa", "kappa-la", "mu-nu-"],
  ],
});

export function functions(): FunctionRepository {
  return builtinFunctionRepository().combine(FunctionRepository.of(testLibraries, testFunctions));
}

export function libraryWithRoot(root: Node): Library {
  return createLibrary("test", root);
}

export async function renderNode(node: Node, data?: Record<string, unknown>): Promise<unknown[]> {
  const ctx = new NodeContext(libraryWithRoot(node), functions(), { data });
  return ctx.render("/");
}

export async function renderChild(network: Node, child: Node, options: { portOverrides?: Record<string, unknown> } = {}): Promise<unknown[]> {
  const ctx = new NodeContext(libraryWithRoot(network), functions(), options);
  const results = await ctx.renderChild("/", child);
  return results.get("output") ?? [];
}

export function expectResults(results: unknown[], ...expected: unknown[]): void {
  expect(results).toEqual(expected);
}

export { connect, publish, setInputValue };
