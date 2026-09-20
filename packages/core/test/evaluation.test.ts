import { describe, expect, it, beforeEach } from "vitest";
import { Color, NodeContext, Point, connect, createNetworkNode, createPort, publish, setInputValue } from "../src";
import {
  addNode,
  expectResults,
  functions,
  instance,
  libraryWithRoot,
  makeNumbersNode,
  makeStringsNode,
  negateNode,
  networkNode,
  numberNode,
  port,
  renderChild,
  renderNode,
  rootNode,
  sideEffects,
} from "./helpers";

beforeEach(() => sideEffects.reset());

function createAddNetwork(name: string, v1: number, v2: number) {
  const number1 = instance(numberNode, "number1", { number: v1 });
  const number2 = instance(numberNode, "number2", { number: v2 });
  const add = instance(addNode, "add");
  const net = networkNode(name, number1, number2, add);
  net.renderedChild = "add";
  connect(net, "number1", "add", "v1");
  connect(net, "number2", "add", "v2");
  return net;
}

describe("NodeContext", () => {
  it("renders a single output", async () => {
    const node = rootNode("values_to_point", "corevector/makePoint", port("x", "float", 0), port("y", "float", 0));
    node.outputType = "point";
    expectResults(await renderNode(node), Point.ZERO);
  });

  it("passes a list-range port whole", async () => {
    // An unconnected list-range port contributes the empty list, not its literal value.
    const node = rootNode("avg", "math/average", port("values", "float", 42, "list"));
    expectResults(await renderNode(node), 0);
  });

  it("processes list-aware nodes", async () => {
    expectResults(await renderNode(instance(makeNumbersNode, "m", { string: "1 2 3 4" })), 1, 2, 3, 4);
  });

  it("cycles connected lists through value ports", async () => {
    const makeNumbers1 = instance(makeNumbersNode, "makeNumbers1", { string: "1 2 3 4" });
    const invert1 = instance(negateNode, "invert1");
    const net = networkNode("net", makeNumbers1, invert1);
    connect(net, "makeNumbers1", "invert1", "value");
    net.renderedChild = "invert1";
    expectResults(await renderChild(net, invert1), -1, -2, -3, -4);
  });

  it("produces nothing when a value input is empty", async () => {
    const noNumbers = instance(makeNumbersNode, "noNumbers", { string: "" });
    const add1 = instance(addNode, "add1");
    const net = networkNode("net", noNumbers, add1);
    connect(net, "noNumbers", "add1", "v1");
    expectResults(await renderChild(net, add1));
  });

  it("runs side-effect nodes once", async () => {
    sideEffects.theInput = 42;
    expectResults(await renderNode(rootNode("get", "side-effects/getNumber")), 42);
    await renderNode(rootNode("set", "side-effects/setNumber", port("number", "int", 42)));
    expect(sideEffects.theOutput).toBe(42);
  });

  it("invokes the function once per element", async () => {
    const makeNumbers1 = instance(makeNumbersNode, "makeNumbers1", { string: "1 2 3" });
    const inc = rootNode("inc", "side-effects/increaseAndCount", port("number", "float", 0));
    const net = networkNode("net", makeNumbers1, inc);
    connect(net, "makeNumbers1", "inc", "number");
    expectResults(await renderChild(net, inc), 2, 3, 4);
    expect(sideEffects.theCounter).toBe(3);
  });

  it("combines a list input with a port value", async () => {
    const makeNumbers1 = instance(makeNumbersNode, "makeNumbers1", { string: "1 2 3" });
    const add1 = instance(addNode, "add1", { v2: 100 });
    const net = networkNode("net", makeNumbers1, add1);
    connect(net, "makeNumbers1", "add1", "v1");
    expectResults(await renderChild(net, add1), 101, 102, 103);
  });

  it("uses the longest list and wraps the shorter ones", async () => {
    const three = instance(makeNumbersNode, "threeNumbers", { string: "1 2 3" });
    const five = instance(makeNumbersNode, "fiveNumbers", { string: "100 200 300 400 500" });
    const add = instance(addNode, "add");
    const net = networkNode("net", three, five, add);
    connect(net, "threeNumbers", "add", "v1");
    connect(net, "fiveNumbers", "add", "v2");
    expectResults(await renderChild(net, add), 101, 202, 303, 401, 502);
  });

  it("matches port ranges", async () => {
    const sum = rootNode("sum", "math/sum", port("numbers", "float", 0, "list"));
    const three = instance(makeNumbersNode, "threeNumbers", { string: "1 2 3" });
    const net = networkNode("net", sum, three);
    connect(net, "threeNumbers", "sum", "numbers");
    expectResults(await renderChild(net, sum), 6);
  });

  const identity: Record<string, string> = {
    int: "math/integer",
    float: "math/number",
    string: "string/string",
    boolean: "math/makeBoolean",
    color: "color/color",
    point: "corevector/point",
  };

  async function convert(sourceType: string, targetType: string, sourceValue: unknown): Promise<unknown> {
    const generate = rootNode("generate", identity[sourceType], port("value", sourceType, sourceValue));
    generate.outputType = sourceType;
    const converter = rootNode("convert", identity[targetType], port("value", targetType));
    converter.outputType = targetType;
    const net = networkNode("net", generate, converter);
    net.renderedChild = "convert";
    connect(net, "generate", "convert", "value");
    const results = await renderNode(net);
    expect(results).toHaveLength(1);
    return results[0];
  }

  it("converts types between connected ports", async () => {
    expect(await convert("int", "float", 42)).toBe(42);
    expect(await convert("int", "string", 42)).toBe("42");
    expect(await convert("int", "boolean", 42)).toBe(true);
    expect((await convert("int", "color", 255)) as Color).toEqual(Color.WHITE);
    expect(await convert("int", "point", 42)).toEqual(new Point(42, 42));
    expect(await convert("float", "int", 42.4)).toBe(42);
    expect(await convert("float", "string", 42)).toBe("42.0");
    expect(await convert("float", "boolean", 0)).toBe(false);
    expect(await convert("string", "int", "42")).toBe(42);
    expect(await convert("string", "float", "42")).toBe(42);
    expect(await convert("string", "boolean", "true")).toBe(true);
    expect(await convert("string", "boolean", "not-a-boolean")).toBe(false);
    expect((await convert("string", "color", "#ff0000ff")) as Color).toEqual(new Color(1, 0, 0));
    expect(await convert("string", "point", "4,2")).toEqual(new Point(4, 2));
    expect(await convert("boolean", "int", true)).toBe(1);
    expect(await convert("boolean", "float", true)).toBe(1);
    expect(await convert("boolean", "string", false)).toBe("false");
    expect((await convert("boolean", "color", true)) as Color).toEqual(Color.WHITE);
    expect(await convert("color", "string", new Color(0, 1, 0))).toBe("#00ff00ff");
    expect(await convert("point", "string", new Point(4, 2))).toBe("4.00,2.00");
  });

  it("converts geometry to points", async () => {
    const line = rootNode(
      "line",
      "corevector/line",
      port("point1", "point", new Point(10, 20)),
      port("point2", "point", new Point(30, 40)),
      port("points", "int", 2),
    );
    line.outputType = "geometry";
    const pt = rootNode("point", "corevector/point", port("value", "point", Point.ZERO));
    pt.outputType = "point";
    const net = networkNode("net", line, pt);
    net.renderedChild = "point";
    connect(net, "line", "point", "value");
    expectResults(await renderNode(net), new Point(10, 20), new Point(30, 40));
  });

  it("drops nulls from results", async () => {
    const three = instance(makeNumbersNode, "threeNumbers", { string: "1 2 3" });
    const makeNull = rootNode("makeNull", "test/makeNull", port("value", "float", 0));
    const net = networkNode("net", three, makeNull);
    connect(net, "threeNumbers", "makeNull", "value");
    expectResults(await renderChild(net, makeNull));
  });

  it("flattens nested list results", async () => {
    const makeStrings = instance(makeStringsNode, "makeStrings", { string: "1,2;3,4;5,6" });
    const makeNumbers = instance(makeNumbersNode, "makeNumbers", { separator: "," });
    const net = networkNode("net", makeStrings, makeNumbers);
    net.renderedChild = "makeNumbers";
    connect(net, "makeStrings", "makeNumbers", "string");
    expectResults(await renderNode(net), 1, 2, 3, 4, 5, 6);
  });

  it("renders subnetworks", async () => {
    const subnet = createAddNetwork("subnet1", 1, 2);
    const net = networkNode("net", subnet);
    net.renderedChild = "subnet1";
    expectResults(await renderChild(net, subnet), 3);
    expectResults(await renderNode(createNetworkNode()));
  });

  it("uses subnetwork results", async () => {
    const subnet1 = createAddNetwork("subnet1", 1, 2);
    const subnet2 = createAddNetwork("subnet2", 3, 4);
    const add1 = instance(addNode, "add1");
    const net = networkNode("net", subnet1, subnet2, add1);
    net.renderedChild = "add1";
    connect(net, "subnet1", "add1", "v1");
    connect(net, "subnet2", "add1", "v2");
    expectResults(await renderChild(net, add1), 10);
  });

  it("renders networks with published ports", async () => {
    const subNet = createAddNetwork("subnet1", 0, 0);
    publish(subNet, "number1", "number", "value1");
    publish(subNet, "number2", "number", "value2");
    setInputValue(subNet, "value1", 2);
    setInputValue(subNet, "value2", 3);
    const net = networkNode("net", subNet);
    net.renderedChild = "subnet1";
    expectResults(await renderChild(net, subNet), 5);
  });

  it("renders networks with connected published ports", async () => {
    const addNet = createAddNetwork("addNet", 0, 0);
    publish(addNet, "number1", "number", "value1");
    publish(addNet, "number2", "number", "value2");
    const number1 = instance(numberNode, "number1", { number: 5 });
    const number2 = instance(numberNode, "number2", { number: 3 });
    const net = networkNode("net", number1, number2, addNet);
    net.renderedChild = "addNet";
    connect(net, "number1", "addNet", "value1");
    connect(net, "number2", "addNet", "value2");
    expectResults(await renderChild(net, addNet), 8);
  });

  it("renders nested networks with connected published ports", async () => {
    const subnet1 = createAddNetwork("subnet1", 0, 0);
    publish(subnet1, "number1", "number", "n1");
    publish(subnet1, "number2", "number", "n2");
    const subnet2 = createAddNetwork("subnet2", 0, 0);
    publish(subnet2, "number1", "number", "n1");
    publish(subnet2, "number2", "number", "n2");
    const add1 = instance(addNode, "add1");
    const subnet = networkNode("subnet", subnet1, subnet2, add1);
    subnet.renderedChild = "add1";
    connect(subnet, "subnet1", "add1", "v1");
    connect(subnet, "subnet2", "add1", "v2");
    publish(subnet, "subnet1", "n1", "value1");
    publish(subnet, "subnet1", "n2", "value2");
    publish(subnet, "subnet2", "n1", "value3");
    publish(subnet, "subnet2", "n2", "value4");
    const numbers = [11, 22, 33, 44].map((v, i) => instance(numberNode, `number${i + 1}`, { number: v }));
    const net = networkNode("net", ...numbers, subnet);
    net.renderedChild = "subnet";
    for (let i = 1; i <= 4; i++) connect(net, `number${i}`, "subnet", `value${i}`);
    expectResults(await renderChild(net, subnet), 110);
  });

  it("reads the frame from the context", async () => {
    const frame = rootNode("frame", "core/frame", port("context", "context"));
    const net = networkNode("net", frame);
    net.renderedChild = "frame";
    expectResults(await renderNode(net, { frame: 42 }), 42);
  });

  it("applies the list output range", async () => {
    const slice = rootNode(
      "slice",
      "list/slice",
      port("list", "string", "", "list"),
      port("start", "int", 0),
      port("size", "int", 1000),
      port("invert", "boolean", false),
    );
    slice.outputRange = "list";
    const makeStrings = instance(makeStringsNode, "makeStrings", { string: "A;B;C" });
    const makeNumbers = instance(makeNumbersNode, "makeNumbers", { string: "0;1;2", separator: ";" });
    const net = networkNode("net", makeStrings, makeNumbers, slice);
    connect(net, "makeStrings", "slice", "list");
    connect(net, "makeNumbers", "slice", "start");
    expectResults(await renderChild(net, slice), "A", "B", "C", "B", "C", "C");
  });

  it("wraps list results of value-range networks", async () => {
    const makeStrings = instance(makeStringsNode, "makeStrings", { string: "A;B;C" });
    const repeat = rootNode(
      "repeat",
      "list/repeat",
      port("value", "list", null, "list"),
      port("amount", "int", 3),
      port("per_item", "boolean", false),
    );
    repeat.outputRange = "list";
    const repeatNet = networkNode("repeatNet", repeat);
    repeatNet.renderedChild = "repeat";
    publish(repeatNet, "repeat", "value", "strings");
    repeatNet.outputRange = "value";
    repeatNet.inputs.find((p) => p.name === "strings")!.range = "value";
    expectResults(await renderNode(repeatNet));
    const net = networkNode("net", makeStrings, repeatNet);
    net.renderedChild = "repeatNet";
    connect(net, "makeStrings", "repeatNet", "strings");
    expectResults(await renderNode(net), ["A", "A", "A"], ["B", "B", "B"], ["C", "C", "C"]);
  });

  it("runs a nested filter per element", async () => {
    const makeStrings = instance(makeStringsNode, "makeStrings", { string: "alpha;beta;gamma" });
    const caseNode = rootNode(
      "changeCase",
      "string/changeCase",
      port("value", "string", ""),
      port("method", "string", "uppercase"),
    );
    const caseNet = networkNode("caseNet", caseNode);
    caseNet.renderedChild = "changeCase";
    publish(caseNet, "changeCase", "value", "value");
    const net = networkNode("net", makeStrings, caseNet);
    connect(net, "makeStrings", "caseNet", "value");
    net.renderedChild = "caseNet";
    expectResults(await renderNode(net), "ALPHA", "BETA", "GAMMA");
  });

  it("clamps values", async () => {
    const clamped = instance(negateNode, "negate");
    clamped.inputs[0].max = 10;
    setInputValue(clamped, "value", 25);
    expectResults(await renderNode(clamped), -10);
    const number1 = instance(numberNode, "number", { number: 25 });
    const net = networkNode("net", clamped, number1);
    connect(net, "number", "negate", "value");
    expectResults(await renderChild(net, clamped), -10);
  });

  it("stores intermediate results", async () => {
    const increase = rootNode("increase", "side-effects/increaseAndCount", port("counter", "float", 42));
    const add = instance(addNode, "add");
    const net = networkNode("net", add, increase);
    connect(net, "increase", "add", "v1");
    connect(net, "increase", "add", "v2");
    expectResults(await renderChild(net, add), 86);
    expect(sideEffects.theCounter).toBe(1);
  });

  it("applies port overrides", async () => {
    const number3 = instance(numberNode, "number3", { number: 3 });
    const number5 = instance(numberNode, "number5", { number: 5 });
    const add = instance(addNode, "add");
    const net = networkNode("net", number3, number5, add);
    connect(net, "number3", "add", "v1");
    connect(net, "number5", "add", "v2");
    net.renderedChild = "add";
    expectResults(await renderChild(net, add), 8);
    expectResults(await renderChild(net, add, { portOverrides: { "number3.number": 10 } }), 15);
  });

  it("evaluates expressions on ports", async () => {
    const n = instance(numberNode, "number");
    n.inputs[0].expression = "frame * 2";
    expectResults(await renderNode(n, { frame: 21 }), 42);
  });

  it("reports errors with the node path", async () => {
    const bad = rootNode("bad", "math/nope");
    await expect(renderNode(bad)).rejects.toThrow(/\/: Function 'math\/nope' does not exist/);
  });

  it("supports named outputs and multiple outlets", async () => {
    const fns = functions();
    const split = rootNode("split", "test/split", port("value", "float", 5));
    split.outputs = [createPort("double", "float"), createPort("half", "float")];
    fns.getLibrary("test")!.getFunction; // exists
    const { Outputs, JavaScriptLibrary, FunctionRepository } = await import("../src");
    const lib = new JavaScriptLibrary("test", { split: (v: number) => new Outputs({ double: v * 2, half: v / 2 }) });
    const negate = instance(negateNode, "negate");
    const net = networkNode("net", split, negate);
    connect(net, "split", "negate", "value", "half");
    net.renderedChild = "negate";
    const ctx = new NodeContext(libraryWithRoot(net), fns.combine(FunctionRepository.of(lib)));
    expect(await ctx.render("/")).toEqual([-2.5]);
  });
});
