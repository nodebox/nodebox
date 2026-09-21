import JSON5 from "json5";
import {
  Project,
  Item,
  Parameter,
  Port,
  FunctionItem,
  Choice,
  ParameterType,
  PortType,
  WidgetType,
  Section,
} from "./types";
import Context from "./context";
import RuntimeNode, { defaultValueForType, defaultWidgetForType } from "./runtime-node";
import { updateFormatVersion } from "./upgrades";
import { evalTemplate, startCase } from "./string-utils";
import { findNodeStatements } from "./lexer";
import { isNativeItem, nativeProject } from "./core-engine";
import { classicLibraryProject, classicProjectToLiveProject, isClassicProject } from "@ndbx/core";

interface LoadResult {
  status: "ok" | "error";
  message?: string;
  assetsUrlTemplate?: string;
  project?: Project;
}

export async function loadProjectThroughApi(url: string): Promise<LoadResult> {
  const token = localStorage.getItem("token");
  const response = await fetch(url, {
    method: "GET",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token || ""}`,
    },
  });

  if (!response.ok) {
    try {
      const result = await response.json();
      return result;
    } catch (e) {
      return { status: "error", message: response.statusText };
    }
  }

  try {
    const result = await response.json();
    return result;
  } catch (e) {
    return { status: "error", message: "Failed to parse response" };
  }
}

export async function loadProjectDirectly(url: string): Promise<LoadResult> {
  try {
    const response = await fetch(url);
    if (!response.ok) {
      try {
        const errBody = await response.json();
        return {
          status: "error",
          message: errBody?.message || response.statusText,
        };
      } catch {
        return { status: "error", message: response.statusText };
      }
    }
    try {
      const data = await response.json();
      return { status: "ok", project: data };
    } catch (e: any) {
      return {
        status: "error",
        message: "Failed to parse JSON: " + String(e),
      };
    }
  } catch (e: any) {
    return { status: "error", message: String(e) };
  }
}

export const CURRENT_FORMAT_VERSION = 1;

const envType = typeof window === "undefined" ? "node" : "browser";

export const config = {
  apiRoot: "https://new.nodebox.live",
  publishedUrlTemplate: "https://new.nodebox.live/api/published/{{ userId }}/{{ projectId }}",
  // Template for asset URLs
  assetsUrlTemplate: "https://new.nodebox.live/api/assets/{{ userId }}/{{ projectId }}/blobs/{{ hash }}",
  // Template for library URLs
  libUrlTemplate: "https://new.nodebox.live/api/fn/{{ userId }}/{{ projectId }}/{{ file }}.js",
  // Used to replace @ndbx/g with https://esm.sh/@ndbx/g
  bareImportReplacer: (name: string) => `https://esm.sh/${name}`,
};

interface ProjectLoader {
  assetMap: Map<string, any>;
  projectMap: Map<string, Project>;
}

export async function loadMainProject(userId: string, projectId: string, version: string): Promise<Context> {
  const projectKey = `${userId}/${projectId}`;
  const loader: ProjectLoader = { assetMap: new Map(), projectMap: new Map() };
  const project = await loadProject(userId, projectId, version, loader);

  const dependencies = structuredClone(loader.projectMap);
  dependencies.delete(projectKey);
  const cx = new Context(project, loader.assetMap, dependencies);
  //   await Promise.all(project.items.map((item: Item) => loadItem(cx, projectKey, item)));
  return cx;
}

async function loadProject(
  userId: string,
  projectId: string,
  version: string,
  loader: ProjectLoader,
): Promise<Project> {
  const projectKey = `${userId}/${projectId}`;
  if (loader.projectMap.has(projectKey)) {
    return loader.projectMap.get(projectKey)!;
  }
  let result: LoadResult;
  if (userId === "nodebox") {
    // The NodeBox 3 built-in libraries come from @ndbx/core, not from the server.
    const project = nativeProject(projectKey);
    result = project
      ? { status: "ok", project: project as unknown as Project }
      : { status: "error", message: `Unknown built-in library ${projectKey}` };
  } else if (version !== "published") {
    // If it's not a published project, we're going through the API
    const projectUrl = `${config.apiRoot}/api/projects/${userId}/${projectId}/${version}`;
    result = await loadProjectThroughApi(projectUrl);
  } else {
    // If it is a published project, we'll use the publishedRoot prefix.
    // Note that this is not an API call! We'll just receive the project.json, so we can't check `result.status`.
    const projectUrl = evalTemplate(config.publishedUrlTemplate, { userId, projectId });
    result = await loadProjectDirectly(projectUrl);
  }
  if (result.status !== "ok") {
    throw new Error(`Error loading project '${userId}/${projectId}': ${result.message}`);
  }
  if (result.assetsUrlTemplate) {
    config.assetsUrlTemplate = result.assetsUrlTemplate;
  }
  const project: Project = result.project!;
  if (project === undefined) {
    throw new Error(`Failed to load project ${userId}/${projectId}@${version}: ${result.message}`);
  }
  if (isClassicProject(project)) {
    return loadClassicProject(projectKey, project, loader);
  }
  const loadedProject = updateFormatVersion(project);
  setMetaDataForItems(project);
  analyzeFunctions(loadedProject);
  loader.projectMap.set(projectKey, loadedProject);
  await loadAssets(userId, projectId, loadedProject, loader);
  await loadDependencies(loadedProject, loader);
  return loadedProject;
}

/**
 * A project in the first NodeBox Live format: a list of functions rather than items, with no
 * format version and no ES modules. It is converted for the editor and kept as it was for the
 * core engine, which renders it with the classic list matching (see docs/classic-nodebox-live.md).
 */
async function loadClassicProject(projectKey: string, classic: any, loader: ProjectLoader): Promise<Project> {
  const [userId, projectId] = projectKey.split("/");
  const project = classicProjectToLiveProject(classic, { key: projectKey }) as unknown as Project;
  project.__classicSource = { key: projectKey, project: classic };
  setMetaDataForItems(project);
  loader.projectMap.set(projectKey, project);
  await loadAssets(userId, projectId, project, loader);
  await loadClassicDependencies(classic, loader);
  return project;
}

/**
 * A classic project's dependencies must be classic too. "core/g" was rewritten in a later format
 * and no longer matches the nodes that call it, so the classic library built into @ndbx/core is
 * used instead of the one the server stores.
 */
async function loadClassicDependencies(classic: any, loader: ProjectLoader) {
  for (const key of Object.keys(classic.dependencies ?? {})) {
    if (loader.projectMap.has(key)) continue;
    const builtin = classicLibraryProject(key);
    if (builtin) {
      await loadClassicProject(key, builtin, loader);
      continue;
    }
    const [userId, projectId] = key.split("/");
    await loadProject(userId, projectId, "published", loader);
  }
}

function setMetaDataForItems(project: Project) {
  for (const item of project.items) {
    item.width = item.width || 1000;
    item.height = item.height || 1000;
  }
}

function analyzeFunctions(project: Project) {
  if (project === undefined) return;
  for (const item of project.items) {
    // Native items carry their ports and parameters explicitly; there is no source to analyze.
    if (isNativeItem(item)) continue;
    if (item.type === "FUNCTION") {
      const nodeStatements = findNodeStatements(item.source);
      const { parameters, sections, inputPorts, outputPorts } = parseNodeStatements(nodeStatements);

      item.parameters = parameters;
      item.sections = sections;
      item.inputPorts = inputPorts;
      item.outputPorts = outputPorts;

      const itemDoc = extractJSDoc(item.source);
      item.description = itemDoc.description;
      item.category = itemDoc.category;
    }
  }
}

export function parseNodeStatements(statements: string[]): {
  parameters: Parameter[];
  sections: Section[];
  inputPorts: Port[];
  outputPorts: Port[];
} {
  let currentSection = undefined;
  const parameters: Parameter[] = [];
  const sections: Section[] = [];
  const inputPorts: Port[] = [];
  const outputPorts: Port[] = [];

  const PARAMETER_TYPE_MAP = new Map<string, ParameterType>([
    ["numberIn", ParameterType.Number],
    ["stringIn", ParameterType.String],
    ["booleanIn", ParameterType.Boolean],
    ["pointIn", ParameterType.Point],
    ["colorIn", ParameterType.Color],
    ["fileIn", ParameterType.File],
    ["choiceIn", ParameterType.Choice],
  ]);
  const INPUT_TYPE_MAP = new Map<string, PortType>([
    ["tableIn", PortType.Table],
    ["shapeIn", PortType.Shape],
    ["specIn", PortType.Spec],
  ]);
  const OUTPUT_TYPE_MAP = new Map<string, PortType>([
    ["tableOut", PortType.Table],
    ["shapeOut", PortType.Shape],
    ["specOut", PortType.Spec],
  ]);

  for (const statement of statements) {
    const match = statement.match(/node\.(\w+)\s*\(/);
    if (!match) continue;

    const [, methodName] = match;
    const argsString = statement.slice(statement.indexOf("(") + 1, statement.lastIndexOf(")"));
    const args = parseArgs(argsString);

    if (PARAMETER_TYPE_MAP.has(methodName)) {
      const type = PARAMETER_TYPE_MAP.get(methodName)!;
      parameters.push(createParameter(type, args, currentSection?.name));
    } else if (INPUT_TYPE_MAP.has(methodName)) {
      inputPorts.push(createPort(methodName, args, INPUT_TYPE_MAP));
    } else if (OUTPUT_TYPE_MAP.has(methodName)) {
      outputPorts.push(createPort(methodName, args, OUTPUT_TYPE_MAP));
    } else if (methodName === "pushSection") {
      currentSection = createSection(args);
      sections.push(currentSection);
    } else if (methodName === "popSection") {
      currentSection = undefined;
    } else if (methodName === "error") {
      // Ignore error methods:
      // C:\GitHub\nodeboxlive\packages\runtime\functions\g\explode-data.js
      // C:\GitHub\nodeboxlive\packages\runtime\functions\g\flatten-data.js
      // -> node.error("Invalid source type.");
    } else {
      console.warn(`Unknown method: ${methodName}`);
    }
  }

  return { parameters, sections, inputPorts, outputPorts };
}

function parseArgs(argsString: string): Record<string, any> {
  if (argsString === "") return {};
  try {
    return JSON5.parse(argsString) as Record<string, any>;
  } catch (error) {
    console.error(`Failed to parse arguments: \`${argsString}\``);
    throw error;
  }
}

export function parseChoices(choices: string[] | string[][]): Choice[] {
  if (choices.length === 0) return [];
  if (typeof choices[0] === "string") {
    return (choices as string[]).map((name) => ({ name, label: startCase(name) }));
  } else {
    return (choices as string[][]).map(([name, label]) => ({ name, label }));
  }
}

function createSection(args: Record<string, any>): Section {
  return {
    name: args.name,
    collapsed: args.collapsed || false,
  };
}

function createParameter(type: ParameterType, args: Record<string, any>, section?: string): Parameter {
  return {
    name: args.name,
    type: type,
    widget: (args.widget as WidgetType) || defaultWidgetForType(type),
    label: args.label || convertToLabel(args.name),
    section,
    defaultValue: args.value ?? defaultValueForType(type),
    choices: args.choices && parseChoices(args.choices),
    min: args.min ?? -Infinity,
    max: args.max ?? Infinity,
    step: args.step || 1,
  };
}

function createPort(methodName: string, args: Record<string, any>, typeMap: Map<string, PortType>): Port {
  return {
    name: args.name,
    type: typeMap.get(methodName)!,
  };
}

interface ItemDoc {
  description: string;
  category: string;
}

function extractJSDoc(sourceCode: string): ItemDoc {
  const commentRegex = /\/\*\*([\s\S]*?)\*\//;
  const match = sourceCode.match(commentRegex);
  if (!match) {
    return { description: "", category: "" };
  }
  const lines = match[1].split("\n").map((l) => l.trim().replace(/^\*\s*/, ""));
  const firstTagIndex = lines.findIndex((l) => l.startsWith("@"));
  const description = lines.slice(0, firstTagIndex).join("\n");
  const tagMap = new Map<string, string>();
  for (const line of lines.slice(firstTagIndex)) {
    if (!line.startsWith("@")) continue;
    const [tag, ...name] = line.split(" ");
    tagMap.set(tag.slice(1), name.join(" "));
  }
  return {
    description: description.trim(),
    category: tagMap.get("category") ?? "",
  };
}

function loadAssets(userId: string, projectId: string, project: Project, loader: ProjectLoader) {
  if (project === undefined) return;
  if (typeof project.assets !== "object") return;
  const loaders = Object.entries(project.assets).map(([filename, hash]) => {
    return _loadAsset(userId, projectId, filename, hash).then((asset) => {
      loader.assetMap.set(filename, asset);
    });
  });
  return Promise.all(loaders);
}

const TEXT_BASED_FILE_EXTENSIONS = new Set([
  ".csv",
  ".txt",
  ".json",
  ".html",
  ".svg",
  ".xml",
  ".css",
  ".geojson",
  ".log",
  ".mdf",
  ".yaml",
]);

function assetUrl(userId: string, projectId: string, hash: string): string {
  return evalTemplate(config.assetsUrlTemplate, { userId, projectId, hash });
}

export async function loadAsset(cx: Context, userId: string, projectId: string, filename: string): Promise<any> {
  const asset = await _loadAsset(userId, projectId, filename, filename);
  cx.assetMap.set(filename, asset);
  return asset;
}

async function _loadAsset(userId: string, projectId: string, filename: string, hash: string): Promise<any> {
  const url = assetUrl(userId, projectId, hash);
  const ext = fileExtension(filename).toLowerCase();
  if (ext === ".jpg" || ext === ".png") {
    return loadImage(url);
  } else if (ext === ".js") {
    return loadScript(url);
  } else if (TEXT_BASED_FILE_EXTENSIONS.has(ext)) {
    const res = await fetch(url);
    const text = await res.text();
    return text;
  } else {
    const res = await fetch(url);
    const buffer = await res.arrayBuffer();
    return buffer;
  }
}

export function fileExtension(filename: string): string {
  const parts = filename.split("/");
  const baseName = parts[parts.length - 1];
  const dot = baseName.lastIndexOf(".");
  if (dot === -1) return "";
  return filename.substring(dot);
}

async function loadImage(url: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image();
    img.crossOrigin = "Anonymous";
    img.onload = () => resolve(img);
    img.onerror = reject;
    img.src = url;
  });
}

async function loadScript(url: string): Promise<void> {
  return new Promise((resolve, reject) => {
    const script = document.createElement("script");
    script.onload = () => resolve();
    script.onerror = reject;
    script.src = url;
    document.body.appendChild(script);
  });
}

function loadDependencies(project: Project, loader: ProjectLoader) {
  if (project === undefined) return;
  const loaders = Object.keys(project.dependencies).map((key) => {
    const [userId, projectId] = key.split("/");
    // Dependencies always load the published version of the project
    return loadProject(userId, projectId, "published", loader);
  });
  return Promise.all(loaders);
}

export async function loadItem(cx: Context, item: Item, itemKey: string) {
  if (item.type === "FUNCTION") {
    await loadFunction(cx, item as FunctionItem, itemKey);
  }
}

export async function loadFunction(cx: Context, item: FunctionItem, itemKey: string) {
  if (cx.initializers.has(itemKey)) {
    return;
  }
  const itemName = itemKey.split("/").pop();
  if (envType === "browser") {
    const source = fixupSource(item.source, itemKey);
    const blob = new Blob([source], { type: "application/javascript" });
    const url = URL.createObjectURL(blob);
    let error;
    try {
      const module = await import(/* @vite-ignore */ url);
      cx.initializers.set(itemKey, module.default || makeEmptyModule(module, itemName!));
    } catch (e) {
      console.error(`Failed to load function ${itemKey}: ${e}`);
      cx.warnings.push(`Failed to load function ${itemKey}: ${e}`);
      error = e;
    } finally {
      URL.revokeObjectURL(url);
    }
    if (error) {
      throw error;
    }
  } else if (envType === "node") {
    try {
      const importSource = `data:text/javascript,${encodeURIComponent(item.source)}`;
      const module = await import(/* @vite-ignore */ importSource);
      cx.initializers.set(itemKey, module.default);
    } catch (error) {
      console.error(`Failed to load function ${itemKey}: ${error}`);
      cx.warnings.push(`Failed to load function ${itemKey}: ${error}`);
    }
  }
}
function makeEmptyModule(module: Record<string, any>, itemName: string) {
  return (node: RuntimeNode) => {
    node.onRender = () => {
      let md = "";
      md += "This module does not contain a default export and is treated as a utility module.\n";
      md += "The following exports are available:\n\n";
      md += "```text\n";
      for (const key in module) {
        md += `- ${key}\n`;
      }
      md += "```\n\n";
      md += "Import it into another function like this:\n";
      md += "```javascript\n";
      md += `import * as util from 'project:${itemName}';\n`;
      md += "```\n\n";

      if (Object.keys(module).length > 0) {
        md += "Or import a specific item like this:\n";
        const firstKey = Object.keys(module)[0];
        md += "```javascript\n";
        md += `import { ${firstKey} } from 'project:${itemName}';\n`;
        md += "```\n\n";
      }
      md += "If you expected this code to work as a node, make sure it looks like this:\n";
      md += "```javascript\n";
      md += "export default function(node) {\n";
      md += "  node.onRender = () => {\n";
      md += "    // your code here\n";
      md += "  }\n";
      md += "}\n";

      node.message = md;
    };
  };
}

function fixupSource(source: string, itemKey: string) {
  const graphicsImportRe = /import\s+(?:\{\s*[\w\s,]+\s*\}|\*\s+as\s+\w+)\s+from\s+"(@ndbx\/g)";/g;
  source = source.replace(graphicsImportRe, (match, graphicsLibrary) => {
    return match.replace(`"${graphicsLibrary}"`, `"${config.bareImportReplacer(graphicsLibrary)}"`);
  });

  if (!source.includes('from "project:')) {
    return source;
  }
  let [userId, projectId, _] = itemKey.split("/");
  if (userId === "self" && projectId === "self") {
    userId = document.location.pathname.split("/")[1];
    projectId = document.location.pathname.split("/")[2];
  }
  const regex = /from "project:(.*?)"/g;
  const libraryName = regex.exec(source)![1].toLowerCase().replaceAll(" ", "-");
  const libUrl = evalTemplate(config.libUrlTemplate, { userId, projectId, file: libraryName });
  source = source.replace(regex, `from "${libUrl}"`);
  return source;
}

function convertToLabel(parameterName: string): string {
  // Replace camelCase with space between words
  const spaced = parameterName.replace(/([a-z])([A-Z])/g, "$1 $2");
  // Capitalize the first letter and convert the rest to lowercase
  return spaced.charAt(0).toUpperCase() + spaced.slice(1).toLowerCase();
}
