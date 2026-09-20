// Format upgrades for .ndbx documents, a port of nodebox.node.NodeLibraryUpgrades. Every step rewrites
// the XML tree of an older document into the next format version; loading a document runs the steps
// in order until the current version is reached.

import { Point } from "../graphics/point";
import { CURRENT_NDBX_FORMAT_VERSION } from "../model/library";
import { XmlElement, parseXml, serializeXml } from "./xml";

export interface UpgradeResult {
  xml: string;
  warnings: string[];
  fromVersion: string;
  toVersion: string;
}

export class UpgradeError extends Error {}

const FORMAT_VERSION_PATTERN = /formatVersion=['"]([\d.]+)['"]/;

export function parseFormatVersion(xml: string): string {
  const m = FORMAT_VERSION_PATTERN.exec(xml);
  if (!m) throw new UpgradeError("Invalid NodeBox file: no formatVersion attribute.");
  return m[1];
}

/** Whether the document needs upgrading (a missing formatVersion means a built-in library: current). */
export function needsUpgrade(xml: string): boolean {
  const m = FORMAT_VERSION_PATTERN.exec(xml);
  return m !== null && m[1] !== CURRENT_NDBX_FORMAT_VERSION;
}

export function upgradeXml(xml: string, targetVersion = CURRENT_NDBX_FORMAT_VERSION): UpgradeResult {
  const fromVersion = parseFormatVersion(xml);
  if (fromVersion === "0.9") throw new UpgradeError("This is a NodeBox 2 file and is no longer supported.");
  let currentXml = xml;
  let currentVersion = fromVersion;
  const warnings: string[] = [];
  let tries = 0;
  while (currentVersion !== targetVersion && tries < 100) {
    const step = UPGRADES[currentVersion];
    if (!step) {
      throw new UpgradeError(
        `Unsupported version ${currentVersion}: this file is too new. Try downloading a new version of NodeBox from https://nodebox.net/download/`,
      );
    }
    const result = step(currentXml);
    warnings.push(...result.warnings);
    currentXml = result.xml;
    currentVersion = parseFormatVersion(currentXml);
    tries++;
  }
  if (tries >= 100) throw new UpgradeError(`Got stuck in an infinite loop when trying to upgrade from ${currentVersion}`);
  return { xml: currentXml, warnings, fromVersion, toVersion: currentVersion };
}

interface StepResult {
  xml: string;
  warnings: string[];
}

interface UpgradeOp {
  start?(root: XmlElement): void;
  apply(e: XmlElement): void;
  end?(root: XmlElement): void;
  warnings: string[];
}

function makeOp(apply: (e: XmlElement, op: UpgradeOp) => void, hooks: Partial<UpgradeOp> = {}): UpgradeOp {
  const op: UpgradeOp = { warnings: [], apply: (e) => apply(e, op), ...hooks };
  return op;
}

function transformXml(xml: string, newFormatVersion: string, ...ops: UpgradeOp[]): StepResult {
  const doc = parseXml(xml);
  const root = doc.root;
  if (root.tagName !== "ndbx") throw new UpgradeError("This is not a valid NodeBox document.");
  root.setAttribute("formatVersion", newFormatVersion);
  const warnings: string[] = [];
  for (const op of ops) {
    op.start?.(root);
    // Collect first: ops may remove or replace elements while walking.
    for (const e of Array.from(root.descendants())) op.apply(e);
    op.end?.(root);
    warnings.push(...op.warnings);
  }
  return { xml: serializeXml(doc), warnings };
}

//// Helpers over the XML tree ////

function isNodeWithPrototype(e: XmlElement, prototype: string): boolean {
  return e.tagName === "node" && e.getAttribute("prototype") === prototype;
}

function portWithName(node: XmlElement, portName: string): XmlElement | undefined {
  return node.childElementsWithName("port").find((p) => p.getAttribute("name") === portName);
}

function childNodeNames(parent: XmlElement): Set<string> {
  return new Set(parent.childElementsWithName("node").map((e) => e.getAttribute("name") ?? ""));
}

function uniqueName(prefix: string, existing: Set<string>): string {
  let counter = 1;
  for (;;) {
    const suggested = `${prefix}${counter}`;
    if (!existing.has(suggested)) return suggested;
    counter++;
  }
}

function splitReference(ref: string): [string, string] {
  const i = ref.indexOf(".");
  return i < 0 ? [ref, ""] : [ref.slice(0, i), ref.slice(i + 1)];
}

function renameRenderedChildReference(element: XmlElement, oldName: string, newName: string | null): void {
  if (element.getAttribute("renderedChild") !== oldName) return;
  if (!newName) element.removeAttribute("renderedChild");
  else element.setAttribute("renderedChild", newName);
}

function renamePortReference(elements: XmlElement[], attribute: string, oldNodeName: string, newNodeName: string): void {
  for (const c of elements) {
    const ref = c.getAttribute(attribute);
    if (ref === undefined) continue;
    const [nodeName, portName] = splitReference(ref);
    if (nodeName === oldNodeName) c.setAttribute(attribute, `${newNodeName}.${portName}`);
  }
}

function renamePortInNodeList(elements: XmlElement[], attribute: string, nodeName: string, oldPort: string, newPort: string): void {
  for (const c of elements) {
    const ref = c.getAttribute(attribute);
    if (ref === undefined) continue;
    const [nodeRef, portRef] = splitReference(ref);
    if (nodeRef === nodeName && portRef === oldPort) c.setAttribute(attribute, `${nodeName}.${newPort}`);
  }
}

function renameNodeReference(elements: XmlElement[], attribute: string, oldNodeName: string, newNodeName: string): void {
  for (const c of elements) if (c.getAttribute(attribute) === oldNodeName) c.setAttribute(attribute, newNodeName);
}

function renameNodeInParent(e: XmlElement, oldNodeName: string, newNodeName: string): void {
  const parent = e.parent;
  if (!parent) return;
  renameRenderedChildReference(parent, oldNodeName, newNodeName);
  const connections = parent.childElementsWithName("conn");
  renamePortReference(connections, "input", oldNodeName, newNodeName);
  renameNodeReference(connections, "output", oldNodeName, newNodeName);
  renamePortReference(parent.childElementsWithName("port"), "childReference", oldNodeName, newNodeName);
}

function removeConnection(parent: XmlElement, child: string, input: string): void {
  for (const conn of parent.childElementsWithName("conn")) {
    if (conn.getAttribute("input") === `${child}.${input}`) parent.removeChild(conn);
  }
}

function removeConnections(parent: XmlElement, child: string): void {
  for (const conn of parent.childElementsWithName("conn")) {
    const inputNode = splitReference(conn.getAttribute("input") ?? "")[0];
    if (inputNode === child || conn.getAttribute("output") === child) parent.removeChild(conn);
  }
}

function parentPublishedInput(parent: XmlElement, child: string, input: string): string | undefined {
  for (const port of parent.childElementsWithName("port")) {
    if (port.getAttribute("childReference") === `${child}.${input}`) return port.getAttribute("name");
  }
  return undefined;
}

function parentPublishedInputs(parent: XmlElement, child: string): string[] {
  const result: string[] = [];
  for (const port of parent.childElementsWithName("port")) {
    const ref = port.getAttribute("childReference");
    if (ref !== undefined && splitReference(ref)[0] === child) result.push(port.getAttribute("name") ?? "");
  }
  return result;
}

function removeNodeInput(node: XmlElement, input: string): void {
  for (const port of node.childElementsWithName("port")) {
    if (port.getAttribute("name") === input) node.removeChild(port);
  }
  const name = node.getAttribute("name");
  const parent = node.parent;
  if (name !== undefined && parent) {
    removeConnection(parent, name, input);
    const published = parentPublishedInput(parent, name, input);
    if (published !== undefined) removeNodeInput(parent, published);
  }
}

//// Reusable operations ////

function changePrototypeOp(oldPrototype: string, newPrototype: string): UpgradeOp {
  return makeOp((e) => {
    if (isNodeWithPrototype(e, oldPrototype)) e.setAttribute("prototype", newPrototype);
  });
}

function renameNodeOp(oldPrefix: string, newPrefix: string): UpgradeOp {
  return makeOp((e) => {
    if (e.tagName !== "node") return;
    const name = e.getAttribute("name");
    if (name === undefined || !name.startsWith(oldPrefix) || !e.parent) return;
    const newName = uniqueName(newPrefix, childNodeNames(e.parent));
    e.setAttribute("name", newName);
    renameNodeInParent(e, name, newName);
  });
}

function exactRenameNodeOp(oldName: string, newPrefix: string, skipRoot = false): UpgradeOp {
  return makeOp((e) => {
    if (e.tagName !== "node") return;
    if (skipRoot && (!e.parent || e.parent.tagName !== "node")) return;
    if (e.getAttribute("name") !== oldName || !e.parent) return;
    const newName = uniqueName(newPrefix, childNodeNames(e.parent));
    e.setAttribute("name", newName);
    renameNodeInParent(e, oldName, newName);
  });
}

function renamePortOp(prototype: string, oldPort: string, newPort: string): UpgradeOp {
  return makeOp((e) => {
    if (!isNodeWithPrototype(e, prototype)) return;
    const nodeName = e.getAttribute("name") ?? "";
    const port = portWithName(e, oldPort);
    if (port) port.setAttribute("name", newPort);
    const parent = e.parent;
    if (!parent) return;
    renamePortInNodeList(parent.childElementsWithName("conn"), "input", nodeName, oldPort, newPort);
    renamePortInNodeList(parent.childElementsWithName("port"), "childReference", nodeName, oldPort, newPort);
  });
}

function changePortTypeOp(prototype: string, portName: string, newType: string, mappings: Record<string, string>): UpgradeOp {
  return makeOp((e) => {
    if (!isNodeWithPrototype(e, prototype)) return;
    const port = portWithName(e, portName);
    if (!port) return;
    port.setAttribute("type", newType);
    const value = port.getAttribute("value");
    if (value !== undefined) {
      const newValue = mappings[value];
      if (newValue === undefined)
        throw new UpgradeError(`Change port type (${prototype}.${portName} -> ${newType}): value ${value} not found in value mappings.`);
      port.setAttribute("value", newValue);
    }
  });
}

function addInputOp(prototype: string, name: string, type: string, value: string): UpgradeOp {
  return makeOp((e) => {
    if (!isNodeWithPrototype(e, prototype)) return;
    const port = new XmlElement("port", [
      ["name", name],
      ["type", type],
      ["value", value],
    ]);
    e.appendChild(port);
  });
}

function addAttributeOp(prototype: string, attribute: string, value: string): UpgradeOp {
  return makeOp((e) => {
    if (isNodeWithPrototype(e, prototype)) e.setAttribute(attribute, value);
  });
}

function removeInputOp(prototype: string, input: string): UpgradeOp {
  return makeOp((e) => {
    if (isNodeWithPrototype(e, prototype)) removeNodeInput(e, input);
  });
}

function removeNodeOp(prototype: string): UpgradeOp {
  const removed: string[] = [];
  const op = makeOp((e) => {
    if (!isNodeWithPrototype(e, prototype) || !e.parent) return;
    const parent = e.parent;
    const child = e.getAttribute("name") ?? "";
    removed.push(child);
    for (const published of parentPublishedInputs(parent, child)) removeNodeInput(parent, published);
    removeConnections(parent, child);
    renameRenderedChildReference(parent, child, null);
    parent.removeChild(e);
  });
  op.end = () => {
    if (removed.length > 0)
      op.warnings.push(`The '${prototype}' node became obsolete, the following nodes in your network got removed: [${removed.join(", ")}]`);
  };
  return op;
}

function setOldDefaultAudioDeviceNameOp(prototype: string, portName: string, deviceName: string): UpgradeOp {
  return makeOp((e) => {
    if (!isNodeWithPrototype(e, prototype)) return;
    if (portWithName(e, portName)) return;
    e.appendChild(
      new XmlElement("port", [
        ["name", portName],
        ["type", "string"],
        ["value", deviceName],
      ]),
    );
  });
}

function convertOscPropertyFormatOp(): UpgradeOp {
  return makeOp((e) => {
    if (e.tagName !== "property") return;
    const parent = e.parent;
    if (!parent || parent.tagName !== "ndbx") return;
    if (e.getAttribute("name") !== "oscPort") return;
    const value = e.getAttribute("value");
    if (value === undefined) {
      parent.removeChild(e);
      return;
    }
    const device = new XmlElement("device", [
      ["name", "osc1"],
      ["type", "osc"],
    ]);
    device.appendChild(
      new XmlElement("property", [
        ["name", "port"],
        ["value", value],
      ]),
    );
    device.appendChild(
      new XmlElement("property", [
        ["name", "autostart"],
        ["value", "true"],
      ]),
    );
    parent.replaceChild(device, e);
  });
}

function convertDevicePropertyNameOp(deviceType: string, oldName: string, newName: string): UpgradeOp {
  return makeOp((e) => {
    if (e.tagName !== "property") return;
    const parent = e.parent;
    if (!parent || parent.tagName !== "device" || parent.getAttribute("type") !== deviceType) return;
    if (e.getAttribute("name") === oldName) e.setAttribute("name", newName);
  });
}

//// The steps ////

type UpgradeStep = (xml: string) => StepResult;

export const UPGRADES: Record<string, UpgradeStep> = {
  "1.0": upgrade1to2,
  "2": upgrade2to3,
  "3": upgrade3to4,
  "4": upgrade4to5,
  "5": upgrade5to6,
  "6": upgrade6to7,
  "7": upgrade7to8,
  "8": upgrade8to9,
  "9": upgrade9to10,
  "10": upgrade10to11,
  "11": upgrade11to12,
  "12": upgrade12to13,
  "13": upgrade13to14,
  "14": upgrade14to15,
  "15": upgrade15to16,
  "16": upgrade16to17,
  "17": upgrade17to18,
  "18": upgrade18to19,
  "19": upgrade19to20,
  "20": upgrade20to21,
  "21": upgrade21to22,
};

export function upgrade1to2(xml: string): StepResult {
  // Version 2: vertical node networks. Swap x and y, then snap pixel positions to grid units.
  const GRID_CELL_SIZE = 48;
  const op = makeOp((e) => {
    if (e.tagName !== "node") return;
    const position = e.getAttribute("position");
    if (position === undefined) return;
    const pt = Point.parse(position);
    const reversed = new Point(pt.y, pt.x);
    const grid = new Point(Math.round(reversed.x / GRID_CELL_SIZE) * 3, Math.round(reversed.y / GRID_CELL_SIZE));
    e.setAttribute("position", grid.toString());
  });
  op.end = () => op.warnings.push("Nodes have been rotated. Your network will look different.");
  return transformXml(xml, "2", op);
}

export function upgrade2to3(xml: string): StepResult {
  return transformXml(xml, "3", changePrototypeOp("math.to_integer", "math.round"), renameNodeOp("to_integer", "round"));
}

export function upgrade3to4(xml: string): StepResult {
  return transformXml(
    xml,
    "4",
    renamePortOp("corevector.to_points", "shape", "value"),
    changePrototypeOp("corevector.to_points", "corevector.point"),
    renameNodeOp("to_points", "point"),
  );
}

export function upgrade4to5(xml: string): StepResult {
  return transformXml(xml, "5", removeInputOp("corevector.textpath", "height"));
}

export function upgrade5to6(xml: string): StepResult {
  return transformXml(
    xml,
    "6",
    renamePortOp("corevector.delete", "delete_selected", "operation"),
    changePortTypeOp("corevector.delete", "operation", "string", { true: "selected", false: "non-selected" }),
  );
}

export function upgrade6to7(xml: string): StepResult {
  return transformXml(xml, "7", changePrototypeOp("list.filter", "list.cull"), renameNodeOp("filter", "cull"));
}

export function upgrade7to8(xml: string): StepResult {
  return transformXml(xml, "8", removeInputOp("corevector.point_on_path", "range"));
}

export function upgrade8to9(xml: string): StepResult {
  return transformXml(
    xml,
    "9",
    addInputOp("corevector.resample_by_amount", "method", "string", "amount"),
    changePrototypeOp("corevector.resample_by_amount", "corevector.resample"),
    renameNodeOp("resample_by_amount", "resample"),
    addInputOp("corevector.resample_by_length", "method", "string", "length"),
    changePrototypeOp("corevector.resample_by_length", "corevector.resample"),
    renameNodeOp("resample_by_length", "resample"),
  );
}

export function upgrade9to10(xml: string): StepResult {
  return transformXml(
    xml,
    "10",
    addInputOp("corevector.wiggle_contours", "scope", "string", "contours"),
    changePrototypeOp("corevector.wiggle_contours", "corevector.wiggle"),
    renameNodeOp("wiggle_contours", "wiggle"),
    addInputOp("corevector.wiggle_paths", "scope", "string", "paths"),
    changePrototypeOp("corevector.wiggle_paths", "corevector.wiggle"),
    renameNodeOp("wiggle_paths", "wiggle"),
    addInputOp("corevector.wiggle_points", "scope", "string", "points"),
    changePrototypeOp("corevector.wiggle_points", "corevector.wiggle"),
    renameNodeOp("wiggle_points", "wiggle"),
  );
}

export function upgrade10to11(xml: string): StepResult {
  return transformXml(xml, "11", removeNodeOp("corevector.draw_path"));
}

export function upgrade11to12(xml: string): StepResult {
  return transformXml(
    xml,
    "12",
    renamePortOp("corevector.shape_on_path", "template", "path"),
    renamePortOp("corevector.shape_on_path", "dist", "spacing"),
    renamePortOp("corevector.shape_on_path", "start", "margin"),
  );
}

export function upgrade12to13(xml: string): StepResult {
  return transformXml(
    xml,
    "13",
    renamePortOp("corevector.text_on_path", "shape", "path"),
    renamePortOp("corevector.text_on_path", "position", "margin"),
    renamePortOp("corevector.text_on_path", "offset", "baseline_offset"),
    removeInputOp("corevector.text_on_path", "keep_geometry"),
  );
}

export function upgrade13to14(xml: string): StepResult {
  return transformXml(xml, "14", renamePortOp("math.wave", "speed", "period"), renamePortOp("math.wave", "frame", "offset"));
}

export function upgrade14to15(xml: string): StepResult {
  return transformXml(xml, "15", renameNodeOp("make_strings", "split"));
}

export function upgrade15to16(xml: string): StepResult {
  return transformXml(
    xml,
    "16",
    exactRenameNodeOp("network", "network"),
    exactRenameNodeOp("node", "node"),
    exactRenameNodeOp("root", "node", true),
    addAttributeOp("corevector.geonet", "outputType", "geometry"),
    changePrototypeOp("corevector.geonet", "core.network"),
  );
}

export function upgrade16to17(xml: string): StepResult {
  return transformXml(xml, "17", convertOscPropertyFormatOp());
}

export function upgrade17to18(xml: string): StepResult {
  // Version 18: switch and combine gained ports. Nothing changes in the file.
  return transformXml(xml, "18");
}

export function upgrade18to19(xml: string): StepResult {
  return transformXml(
    xml,
    "19",
    setOldDefaultAudioDeviceNameOp("device.audio_analysis", "device_name", "audioplayer1"),
    setOldDefaultAudioDeviceNameOp("device.audio_wave", "device_name", "audioplayer1"),
    setOldDefaultAudioDeviceNameOp("device.beat_detect", "device_name", "audioplayer1"),
  );
}

export function upgrade19to20(xml: string): StepResult {
  return transformXml(
    xml,
    "20",
    convertDevicePropertyNameOp("osc", "autostart", "sync_with_timeline"),
    convertDevicePropertyNameOp("audioplayer", "autostart", "sync_with_timeline"),
    convertDevicePropertyNameOp("audioinput", "autostart", "sync_with_timeline"),
  );
}

export function upgrade20to21(xml: string): StepResult {
  // Version 21: the copy node's scale is a percentage, like the scale node.
  const op = makeOp((e) => {
    if (!isNodeWithPrototype(e, "corevector.copy")) return;
    const scalePort = portWithName(e, "scale");
    const value = scalePort?.getAttribute("value");
    if (!scalePort || value === undefined) return;
    const pt = Point.parse(value);
    scalePort.setAttribute("value", new Point((pt.x + 1) * 100, (pt.y + 1) * 100).toString());
  });
  return transformXml(xml, "21", op);
}

export function upgrade21to22(xml: string): StepResult {
  // Version 22: feedback ("state") ports left the engine, which retired device.buffer_points.
  return transformXml(xml, "22", removeNodeOp("device.buffer_points"));
}
