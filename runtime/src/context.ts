import { Project, Item, PortValue } from "./types";
import RuntimeNode from "./runtime-node";
import { checkFunctionId, generateRandomId } from "./identifiers";

export default class Context {
  project: Project;
  assetMap: Map<string, any>;
  dependencies: Map<string, Project>;
  warnings: string[];
  // A map of runtime nodes keyed on the node id.
  runtimeNodes: Map<string, RuntimeNode>;
  // A mpa of initializer functions keyed on the fully qualified function id.
  initializers: Map<string, any>;
  portValues: Map<string, PortValue>;
  clientId: string;
  instanceId: number;

  constructor(project: Project, assetMap: Map<string, any>, dependencies: Map<string, Project>) {
    this.project = project;
    this.assetMap = assetMap;
    this.dependencies = dependencies;
    this.warnings = [];
    this.runtimeNodes = new Map();
    this.initializers = new Map();
    this.portValues = new Map();
    this.clientId = generateRandomId();
    this.instanceId = 0;
  }

  generateId(): string {
    return `${this.clientId}:${this.instanceId++}`;
  }

  lookupItemByName(fqId: string): Item | undefined {
    if (!checkFunctionId(fqId)) {
      throw new Error(`Invalid function id: ${fqId}`);
    }
    const [userId, projectId, itemName] = fqId.split("/");
    let project: Project | undefined;
    if (userId === "self" && projectId === "self") {
      project = this.project;
    } else {
      project = this.dependencies.get(`${userId}/${projectId}`);
    }
    if (!project) {
      throw new Error(`Dependency not found: ${fqId}`);
    }
    const item = project.items.find((item) => item.name === itemName);
    if (!item) {
      // throw new Error(`Item not found: ${fqId}`);
    }
    return item;
  }

  lookupItemById(fqId: string): Item {
    if (!checkFunctionId(fqId)) {
      throw new Error(`Invalid function id: ${fqId}`);
    }
    const [userId, projectId, itemId] = fqId.split("/");
    let project: Project | undefined;
    if (userId === "self" && projectId === "self") {
      project = this.project;
    } else {
      project = this.dependencies.get(`${userId}/${projectId}`);
    }
    if (!project) {
      throw new Error(`Dependency not found: ${fqId}`);
    }
    const item = project.items.find((item) => item.id === itemId);
    if (!item) {
      throw new Error(`Item not found: ${fqId}`);
    }
    return item;
  }
}
