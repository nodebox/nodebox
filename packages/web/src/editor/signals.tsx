import { pushHistory } from "./history";
import { computed, signal } from "@preact/signals-react";
import { produce } from "immer";
import {
  Color,
  Context,
  Project,
  Network,
  Node,
  Inlet,
  Outlet,
  Port,
  PortValue,
  Item,
  loadMainProject,
  loadAsset,
  Point,
  findNodeById,
  evaluateItem,
  sendChangeEvent,
  NetworkItem,
  NodeToNodeConnection,
  ParameterValue,
  Gallery,
  ProjectTitle,
  LiteralValue,
  PortType,
  Sticky,
  ParameterType,
  WidgetType,
  defaultValueForType,
  renderItemToSvgString,
} from "@ndbx/runtime";
import * as mutation from "@ndbx/runtime/src/mutation";
import { debounce } from "../util";
import { checkFunctionId } from "@ndbx/runtime/src/identifiers";
import Markdown from "react-markdown";

export async function apiRequest(
  url: string,
  method = "GET",
  body: undefined | Gallery | Project | ProjectTitle | { scope: string },
) {
  const token = localStorage.getItem("token");
  const response = await fetch(url, {
    method,
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: (body !== undefined && JSON.stringify(body)) || undefined,
  });

  if (!response.ok) {
    error.value = `Failed to fetch: ${response.statusText}`; // Set error signal value
  }

  return response;
}

export enum SaveState {
  Saving = "SAVING",
  Saved = "SAVED",
  Error = "ERROR",
  Clear = "CLEAR",
}

export enum PlayState {
  Playing = "PLAYING",
  Paused = "PAUSED",
}

export enum OutlinerMode {
  Project = "PROJECT",
  Dependencies = "DEPENDENCIES",
}

export const userId = signal<string | null>(null);
export const projectId = signal<string | null>(null);
export const version = signal<string>("dev");
export const cx = signal<Context | null>(null);
export const project = signal<Project | null>(null);
export const projectNewState = signal<Project | null>(null);
export const error = signal<string | null>(null);
export const nodeError = signal<Error | null>(null);
export const result = signal<PortValue | null>(null);
export const resultVersion = signal<number>(0);
export const playState = signal<PlayState>(PlayState.Paused);
export const scopePrivateNoPlan = signal<boolean>(false);
let _renderRequested = false;
export const createParameterModalVisible = signal<boolean>(false);
export const createParameterModalDefaultName = signal<string>("");
export const createParameterModalDefaultType = signal<ParameterType>(ParameterType.Number);
export const createParameterModalDefaultValue = signal<LiteralValue>(0);
export const createParameterModalCallback = signal<(expression: any) => void>(() => {});
export const saveState = signal<SaveState>(SaveState.Saved);
export const shareModalVisible = signal<boolean>(false);

// Fully-qualified item id (e.g. self/self/0:2 or core/g/12:2)
export const activeItemId = signal<string | null>(null);
export const pinnedItemId = signal<string | null>(null);
export const outlinerMode = signal<OutlinerMode>(OutlinerMode.Project);

// Properties panel
export const collapsedSections = signal<Record<string, boolean>>({});

export const createItemModalVisible = signal<boolean>(false);
export const createNodeModalVisible = signal<boolean>(false);
export const createNodeModalVisibleMode = signal<string>("CREATE");
export const createOutletModalVisible = signal<boolean>(false);
export const createNetworkItemPosition = signal<Point>({ x: 0, y: 0 });
export const addDependencyModalVisible = signal<boolean>(false);
export const assetsModalVisible = signal<boolean>(false);
export const assetParameterPath = signal<string>("");
export const projectModalVisible = signal<boolean>(false);
export const parameterMetaPanelVisible = signal<boolean>(false);
export const parameterMetaPanelPosition = signal<Point>({ x: 0, y: 0 });
export const parameterMetaPanelParameterName = signal<string | undefined>(undefined);

export const activeNetworkItemIdMap = signal<Map<string, string>>(new Map());
export const stickyCanvasCacheMap: Map<string, HTMLCanvasElement> = new Map();

const lastChangeTime = signal<number>(0);
const lastChangeType = signal<string>("");
const lastChangeKey = signal<string | undefined>(undefined);

export const activeItem = computed(() => {
  if (!cx.value || !project.value) return null;
  if (!activeItemId.value) return null;
  if (activeItemId.value.startsWith("self/self")) {
    const itemId = activeItemId.value.split("/")[2];
    return project.value.items.find((item: Item) => item.id === itemId) || null;
  }
  return cx.value.lookupItemById(activeItemId.value);
});

export const currentItem = computed(() => {
  if (!cx.value || !project.value) return null;
  const fqId = pinnedItemId.value || activeItemId.value;
  if (fqId === null) return null;
  if (fqId.startsWith("self/self")) {
    const itemId = fqId.split("/")[2];
    return project.value.items.find((item: Item) => item.id === itemId) || null;
  }
  return cx.value.lookupItemById(fqId);
});

export const setActiveItemId = (id: string) => {
  if (!checkFunctionId(id)) {
    throw new Error(`Invalid item id: ${id}`);
  }
  activeItemId.value = id;
  requestRender();
};

export const togglePinnedItemId = (id: string) => {
  if (!checkFunctionId(id)) {
    throw new Error(`Invalid item id: ${id}`);
  }
  if (pinnedItemId.value === id) {
    pinnedItemId.value = null;
  } else {
    pinnedItemId.value = id;
  }
  requestRender();
};

export const activeNetworkItemId = computed(() => {
  if (!activeItemId.value) return null;
  const id = activeItemId.value.split("/")[2];
  return activeNetworkItemIdMap.value.get(id);
});

export const settingsVisible = signal<boolean>(false);
export const functionModalVisible = signal<boolean>(false);

export async function loadProject(_userId: string, _projectId: string, _version: string) {
  userId.value = _userId;
  projectId.value = _projectId;
  version.value = _version;
  cx.value = await loadMainProject(_userId, _projectId, _version);
  project.value = cx.value!.project;
  const hash = document.location.hash.replace("#", "");
  const itemIndex = project.value!.items.findIndex((i) => i.id === hash);
  const firstItem = project.value!.items[itemIndex] || project.value!.items[0];
  if (firstItem) {
    activeItemId.value = `self/self/${firstItem.id}`;
  }
  //   _pushUndo();
  requestRender();
}

/**
 * This is called immediately after a change is made to the project.
 * The change type indicates the type of change that is made, e.g. delete-nodes, set-parameter-value, etc.
 * The change key indicates on what the change was made, e.g. the name of the node, the name of the parameter, etc.
 * Edits can be grouped when:
 * - The changeType matches (e.g. "set-node-position")
 * - The changeKey matches (e.g. "rect1")
 * - The change was made within one second
 * If any of these conditions is not met, or if the changeKey is undefined, the edits are not grouped.
 * @param changeType The type of change that was made
 * @param changeKey The key of the change that was made
 */
function onChangeSignal(changeType: string, changeKey?: string) {
  if (project.value) {
    pushHistory(project.value, changeType, changeKey);
  }

  if (saveState.value === SaveState.Saved) {
    saveState.value = SaveState.Saving;
  }
  lastChangeTime.value = Date.now();
  lastChangeType.value = changeType;
  lastChangeKey.value = changeKey;
  saveProjectDebounced();
}

export function updateContext(newProjectState?: Project) {
  if (newProjectState) {
    project.value = structuredClone(newProjectState);
    cx.value!.project = project.value;
    mutation.markProjectDirty(cx.value!, project.value!);
  } else {
    cx.value!.project = project.value!;
  }
  requestRender();
}

export async function saveProject() {
  const currentUserId = localStorage.getItem("userId");
  if (!currentUserId || currentUserId !== userId.value) return;
  if (version.value !== "dev") return;
  if (scopePrivateNoPlan.value) return;

  const projectUrl = `/api/projects/${userId.value}/${projectId.value}`;

  try {
    const res = await apiRequest(projectUrl, "POST", project.value!);
    if (!res.ok) {
      error.value = "Failed to save project.";
      saveState.value = SaveState.Error;
      return;
    }
    saveState.value = SaveState.Saved;
    error.value = null;
  } catch (err) {
    if (err instanceof Error) {
      error.value = `Error: ${err.message}`;
    } else {
      error.value = "An unknown error occurred.";
    }
    saveState.value = SaveState.Error;
  }
}
export async function updateProjectTitleInProfile(title: string) {
  const currentUserId = localStorage.getItem("userId");
  if (!currentUserId || currentUserId !== userId.value) return;

  const projectUrl = `/api/set-title/${userId.value}/${projectId.value}`;

  try {
    const res = await apiRequest(projectUrl, "POST", { title: title });
    if (!res.ok) {
      throw new Error("Failed to update title in profile.");
    }
    // No errors, API call succeeded
    saveState.value = SaveState.Saved;
    error.value = null; // Clear any previous error
  } catch (err) {
    if (err instanceof Error) {
      error.value = `Error: ${err.message}`;
    } else {
      error.value = "An unknown error occurred.";
    }
    saveState.value = SaveState.Error;
    throw err;
  }
}

export async function updateProjectScopeInProfile(scope: string) {
  const currentUserId = localStorage.getItem("userId");
  if (!currentUserId || currentUserId !== userId.value) return;

  const projectUrl = `/api/set-scope/${userId.value}/${projectId.value}`;

  try {
    const res = await apiRequest(projectUrl, "POST", { scope: scope });
    if (!res.ok) {
      throw new Error("Failed to update scope in profile.");
    }
    // No errors, API call succeeded
    saveState.value = SaveState.Saved;
    error.value = null; // Clear any previous error
  } catch (err) {
    if (err instanceof Error) {
      error.value = `Error: ${err.message}`;
    } else {
      error.value = "An unknown error occurred.";
    }
    saveState.value = SaveState.Error;
    throw err;
  }
}

export async function publishProject() {
  const currentUserId = localStorage.getItem("userId");
  if (!currentUserId || currentUserId !== userId.value) return;
  // don't add to undo:redo stack
  // onChangeSignal("publish-project");

  project.value = produce(project.value!, (draft) => {
    draft.isPublished = true;
    draft.publishDate = new Date().toISOString();
  });

  await saveProject();

  const projectUrl = `/api/projects/${userId.value}/${projectId.value}/publish`;
  const res = await apiRequest(projectUrl, "POST", project.value!);
  if (!res.ok) {
    error.value = "Failed to publish project";
    saveState.value = SaveState.Error;
    return;
  }
  saveState.value = SaveState.Saved;
}

const saveProjectDebounced = debounce(saveProject, 1000);

export function requestRender() {
  if (_renderRequested) return;
  _renderRequested = true;
  window.requestAnimationFrame(_render);
}

export const requestUpdateContextDebounced = debounce(updateContext, 200);

async function _render() {
  if (!cx.value || !project.value) return null;
  nodeError.value = null;
  const fqId = pinnedItemId.value || activeItemId.value;
  if (fqId === null) return null;
  const [userId, projectId] = fqId.split("/");
  const item = cx.value.lookupItemById(fqId);
  const fqName = `${userId}/${projectId}/${item.name}`;
  _renderItem(fqName, item);
}

async function _renderItem(fqName: string, item: Item) {
  let runtimeNode;
  try {
    runtimeNode = await evaluateItem(cx.value!, fqName, item);
  } catch (e) {
    console.error(`Error evaluating ${item.name} [${item.id}]: ${e}`, (e as Error).stack);
    nodeError.value = e as Error;
  }
  _renderRequested = false;
  // FIXME can have multiple output ports. Now we just take the first one
  if (runtimeNode) {
    if (runtimeNode.outputPorts.length > 0) {
      result.value = runtimeNode.outputPorts[0].value;
    } else if (runtimeNode.message) {
      result.value = (
        <Markdown className="markdown py-8 px-2 max-w-lg font-mono text-zinc-400 text-sm">
          {runtimeNode.message}
        </Markdown>
      );
    }
    resultVersion.value = resultVersion.value + 1;
    if (runtimeNode.timeDependent && playState.value === PlayState.Playing) {
      requestRender();
    }
  } else {
    result.value = null;
  }
}

export async function saveSvgToServer(projectId: string, item: string, version: string, svgString: string) {
  const token = localStorage.getItem("token");
  const userId = localStorage.getItem("userId");
  const url = `/api/save-svg`;
  const body = JSON.stringify({ projectId, item, version, userId: userId, svgContent: svgString });

  const response = await fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: body,
  });

  if (!response.ok) {
    throw new Error(`Failed to save SVG: ${response.statusText}`);
  }

  return response.json();
}

export async function exportAllExampleNetworks() {
  const fqId = pinnedItemId.value || activeItemId.value;
  for (const item of project.value!.items) {
    await _renderItem(`self/self/${item.name}`, item);
    setActiveItemId(`self/self/${item.id}`);
    const svgString = await renderToSvg();
    if (svgString) {
      try {
        await saveSvgToServer(project.value!.id, item.id, "publish", svgString);
      } catch (e) {
        console.error(`Failed to save ${item.id}.svg: ${(e as Error).message}`);
      }
    }
  }
  if (fqId !== null) setActiveItemId(fqId);
}

export function renderToSvg(): string | undefined {
  const item = currentItem.value;
  if (!item) return;
  return renderItemToSvgString(item, result.value);
}

export function setPlayState(state: PlayState) {
  playState.value = state;
  requestRender();
}

export function togglePlay() {
  if (playState.value === PlayState.Playing) {
    setPlayState(PlayState.Paused);
  } else {
    setPlayState(PlayState.Playing);
  }
}

export async function setProjectTitle(title: string) {
  try {
    await updateProjectTitleInProfile(title);
    project.value = produce(project.value!, (draft) => {
      draft.title = title;
      onChangeSignal("set-project-title");
    });
  } catch (err) {
    if (err instanceof Error) {
      error.value = `Error: ${err.message}`;
      throw new Error(`Failed to set project title: ${err.message}`);
    } else {
      error.value = "An unknown error occurred.";
    }
  }
}

export function setProjectDescription(description: string) {
  project.value = produce(project.value!, (draft) => {
    draft.description = description;
    draft.color = project.value!.items[0].background;
    onChangeSignal("set-project-description");
  });
}

export async function setProjectScope(scope: string) {
  try {
    await updateProjectScopeInProfile(scope);
    project.value = produce(project.value!, (draft) => {
      draft.scope = scope;
    });
    onChangeSignal("set-project-scope");
  } catch (err) {
    if (err instanceof Error) {
      error.value = `Error: ${err.message}`;
      throw new Error(`Failed to set project scope: ${err.message}`);
    } else {
      error.value = "An unknown error occurred.";
    }
  }
}

export function setProjectItemName(networkID: string, newName: string) {
  const isNode = (item: NetworkItem): item is Node => item.type === "NODE";
  if (!project.value!.items.filter((i) => i.id != networkID).some((i) => i.name === newName)) {
    project.value = produce(project.value!, (draft) => {
      const originalName = draft.items.find((l) => l.id === networkID)!.name;
      draft.items
        .filter((item): item is Network => item.type === "NETWORK")
        .forEach((item) => {
          item.children &&
            item.children.forEach((child) => {
              if (isNode(child) && child.fn === `self/self/${originalName}`) {
                child.fn = `self/self/${newName.trim()}`;
                child.name = child.name.replace(originalName, newName.trim());
              }
            });
        });

      if (draft.items.find((l) => l.id === networkID))
        draft.items.find((l) => l.id === networkID)!.name = newName.trim();

      onChangeSignal("set-project-network-name");
    });
  } else {
    nodeError.value = `names must be unique` as unknown as Error;
  }
}
export function removeNetworkInProject(networkID: string) {
  const networkItem = project.value!.items.find((item) => item.id === networkID);
  if (networkItem === undefined) return;
  const networkName = networkItem.name;
  const isNode = (item: NetworkItem): item is Node => item.type === "NODE";
  const references = project
    .value!.items.filter((item): item is Network => item.type === "NETWORK")
    .filter((item: Network) => item.children.some((child) => isNode(child) && child.fn === `self/self/${networkName}`));

  if (references.length === 0) {
    project.value = produce(project.value!, (draft) => {
      if (draft.items.find((l) => l.id === networkID)) draft.items = draft.items.filter((l) => l.id != networkID);
      onChangeSignal("remove-network");
    });
  } else {
    nodeError.value = `cannot delete ${networkName}: used in ${references
      .map((ref) => `[${ref.name}]`)
      .join(",")}` as unknown as Error;
  }
}

export async function addDependency(userId: string, projectId: string) {
  project.value = produce(project.value!, (draft) => {
    draft.dependencies[`${userId}/${projectId}`] = "dev";
    onChangeSignal("add-dependency");
  });
}
export function createItem(name: string, type: string) {
  let newItem: Item | undefined;
  onChangeSignal("create-item");
  project.value = produce(project.value!, (draft) => {
    if (type === "NETWORK") {
      newItem = mutation.createNetwork(cx.value!, draft, name);
    } else if (type === "FUNCTION") {
      newItem = mutation.createFunction(cx.value!, draft, name);
    } else {
      throw new Error(`Invalid item type ${type}`);
    }
  });
  updateContext();
  if (newItem) {
    setActiveItemId(`self/self/${newItem.id}`);
  }
}

export function setItemMeta(item: Item, key: string, value: unknown) {
  project.value = produce(project.value!, (draft) => {
    const draftItem = draft.items.find((i: Item) => i.id === item.id);
    if (draftItem) {
      (draftItem as Record<string, unknown>)[key] = value;
    }
    onChangeSignal("set-item-meta", key);
  });
}
type ValueWithValueProperty = {
  value: unknown;
};
function isValueWithValueProperty(obj: unknown): obj is ValueWithValueProperty {
  return typeof obj === "object" && obj !== null && "value" in obj;
}
export function setItemMetaGallery(item: Item, key: string, value: unknown) {
  project.value = produce(project.value!, (draft) => {
    const draftItem = draft.items.find((i: Item) => i.id === item.id);
    if (draftItem) {
      // We need to cast to Record<string, unknown> because TS can't check all possible keys.
      if (!("__gallery" in draftItem)) {
        (draftItem as Record<string, unknown>).__gallery = {};
      }
      const gallery = (draftItem as { __gallery: Record<string, unknown> }).__gallery;
      if (isValueWithValueProperty(value)) gallery[key] = value.value;
    }
    onChangeSignal("set-item-meta-gallery", key);
  });
}

export function addItemParameter(item: Item, parameterName: string, type: ParameterType, defaultValue: LiteralValue) {
  project.value = produce(project.value!, (draft) => {
    const draftItem = draft.items.find((i: Item) => i.id === item.id);
    if (draftItem) {
      if (!draftItem.parameters) {
        draftItem.parameters = [];
      }
      draftItem.parameters.push({
        name: parameterName,
        label: parameterName,
        type,
        widget: type as unknown as WidgetType,
        defaultValue,
        min: -Infinity,
        max: Infinity,
        step: 1,
      });
    }
    onChangeSignal("add-item-parameter");
  });
}

export function removeItemParameter(item: Item, parameterName: string) {
  project.value = produce(project.value!, (draft) => {
    const draftItem = draft.items.find((i: Item) => i.id === item.id);
    if (draftItem) {
      draftItem.parameters = draftItem.parameters.filter((p) => p.name !== parameterName);
    }
    onChangeSignal("remove-item-parameter");
  });
  const newItem = project.value!.items.find((i: Item) => i.id === item.id)!;
  mutation.markItemParameterDirty(cx.value!, newItem, parameterName);
  updateContext();
}

export function setItemParameterProperty(item: Item, parameterName: string, key: string, value: unknown) {
  project.value = produce(project.value!, (draft) => {
    const draftItem = draft.items.find((i: Item) => i.id === item.id);
    if (draftItem) {
      const parameter = draftItem.parameters.find((p) => p.name === parameterName);
      if (parameter) {
        (parameter as unknown as Record<string, unknown>)[key] = value;
        if (key === "type") {
          // Make sure the widget matches the type.
          parameter.widget = value as unknown as WidgetType;
          //   const stringValue = value.toString();
          parameter.defaultValue = defaultValueForType(value as ParameterType);
        }
      }
    }
    onChangeSignal("set-item-parameter-property", `${parameterName}/${key}`);
  });
}

export function setItemParameterValue(item: Item, parameterName: string, value: LiteralValue) {
  project.value = produce(project.value!, (draft) => {
    const draftItem = draft.items.find((i: Item) => i.id === item.id);
    if (draftItem) {
      const parameter = draftItem.parameters.find((p) => p.name === parameterName);
      if (parameter) {
        parameter.defaultValue = value;
      }
    }
    onChangeSignal("set-item-parameter-value", parameterName);
  });
  const newItem = project.value!.items.find((i: Item) => i.id === item.id)!;
  mutation.markItemParameterDirty(cx.value!, newItem, parameterName);
  updateContext();
}

export function setFunctionSource(id: string, source: string) {
  onChangeSignal("set-function-source", id);
  project.value = produce(project.value!, (draft) => {
    const item = draft.items.find((item: Item) => item.id === id);
    if (item && item.type === "FUNCTION") {
      item.source = source;
    }
  });
  const item = project.value!.items.find((item: Item) => item.id === id)!;
  const fqId = `self/self/${item!.name}`;
  mutation.markFunctionDirty(cx.value!, project.value!, fqId);
  requestUpdateContextDebounced();
}

export function createNode(network: Network, fn: string, position?: Point): Node {
  let newnode;
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    newnode = mutation.createNode(cx.value as Context, draftNetwork, fn, position);
    onChangeSignal("create-node");
  });

  activeNetworkItemIdMap.value = new Map(activeNetworkItemIdMap.value);
  activeNetworkItemIdMap.value.set(network.id, newnode!.id);

  return newnode!;
}

export function replaceNode(network: Network, fn: string, nodeId: string) {
  project.value = produce(project.value!, (draft) => {
    const draftNetwork: Network = draft.items.find((item: Item) => item.id === network.id) as Network;
    if (!draftNetwork) return;
    const node = draftNetwork.children.find(
      (child: NetworkItem) => child.id === nodeId && child.type === "NODE",
    ) as Node;
    mutation.replaceNode(cx.value as Context, draftNetwork, node, fn);
    onChangeSignal("update-node");
  });
  updateContext();
}

export function duplicateItems(network: Network, selectionIds: string[]): [Network, string[]] {
  let newIds: string[] = [];
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    newIds = mutation.duplicateItems(cx.value as Context, draftNetwork, selectionIds);
    onChangeSignal("duplicate-items");
  });
  updateContext();
  const newNetwork = project.value!.items.find((item: Item) => item.id === network.id) as Network;
  // Because we want to be able to select the new items, we want to also return the new network.
  return [newNetwork, newIds];
}

export function pasteItemsFromClipboard(
  network: Network,
  items: NetworkItem[],
  internalConnections: NodeToNodeConnection[],
): [string[]] {
  let newIds: string[] = [];
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    newIds = mutation.addItemsAndConnections(cx.value as Context, draftNetwork, items, internalConnections);
    onChangeSignal("paste-items-from-clipboard");
  });
  updateContext();
  // Because we want to be able to select the new items, we want to also return the new network.
  return [newIds];
}

export function updateNodeName(network: Network, node: Node, newName: string) {
  const name = newName === "" ? node.fn : newName;
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    const draftNode = draftNetwork.children.find((n: NetworkItem) => n.id === node.id) as Node;
    draftNode.name = name;
    onChangeSignal("update-node-name", `${node.id}/${newName}`);
  });
  updateContext();
}

export function setParameterValue(network: Network, node: Node, parameterName: string, value: ParameterValue) {
  onChangeSignal("set-parameter-value", `${node.id}/${parameterName}`);
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    const draftNode = draftNetwork.children.find((n: NetworkItem) => n.id === node.id) as Node;
    if (draftNode.values === undefined) {
      draftNode.values = {};
    }
    draftNode.values[parameterName] = value;
  });
  mutation.markDirty(cx.value!, network, node);
  sendChangeEvent(cx.value!, node.id, parameterName);
  updateContext();
}

export function setParameterEmpty(network: Network, node: Node, parameterName: string) {
  onChangeSignal("set-parameter-empty", `${node.id}/${parameterName}`);
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    const draftNode = draftNetwork.children.find((n: NetworkItem) => n.id === node.id) as Node;
    if (draftNode.values === undefined) {
      draftNode.values = {};
    }
    delete draftNode.values[parameterName];
  });
  mutation.markDirty(cx.value!, network, node);
  sendChangeEvent(cx.value!, node.id, parameterName);
  updateContext();
}

export function setStickyText(network: Network, sticky: Sticky, value: string) {
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    const draftNode = draftNetwork.children.find((n: NetworkItem) => n.id === sticky.id) as Sticky;
    draftNode.text = value;
    onChangeSignal("set-sticky-text", `${sticky.id}/text`);
  });
  updateContext();
}
export function createNewSticky(network: Network) {
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    const sticky = mutation.createSticky(cx.value as Context, draftNetwork);
    onChangeSignal("create-sticky", sticky.id);
  });
}

export function setStickyBackgroundColor(network: Network, sticky: Sticky, value: Color) {
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    const draftSticky = draftNetwork.children.find((n: NetworkItem) => n.id === sticky.id) as Sticky;
    draftSticky.backgroundColor = value;
    onChangeSignal("set-sticky-backgroundColor", sticky.id);
  });
}

export function setStickyFontSize(network: Network, sticky: Sticky, fontSize: string) {
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    const draftSticky = draftNetwork.children.find((n: NetworkItem) => n.id === sticky.id) as Sticky;
    draftSticky.fontSize = parseInt(fontSize.replace("px", ""));
    onChangeSignal("set-sticky-fontColor", sticky.id);
  });
}

export function setStickyFontColor(network: Network, sticky: Sticky, value: Color) {
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    const draftSticky = draftNetwork.children.find((n: NetworkItem) => n.id === sticky.id) as Sticky;
    draftSticky.fontColor = value;
    onChangeSignal("set-sticky-fontColor", sticky.id);
  });
}

export function createOutlet(network: Network, name: string, type: PortType) {
  const position = createNetworkItemPosition.value;
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    mutation.createOutlet(cx.value!, draftNetwork, name, type, position);
    onChangeSignal("create-outlet");
  });
}

export function groupIntoNetwork(network: Network, selectionIds: string[], newNetworkName: string) {
  project.value = produce(project.value!, (draft) => {
    mutation.groupIntoNetwork(cx.value as Context, draft, network, selectionIds, newNetworkName);
    onChangeSignal("group-into-network");
  });
  updateContext();
}

export function deleteItems(network: Network, itemIds: string[]) {
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    mutation.deleteItems(cx.value as Context, draftNetwork, itemIds);
    onChangeSignal("delete-items");
  });
  updateContext();
}

export function connectNodeToNode(network: Network, outNode: Node, outPort: Port, inNode: Node, inPort: Port) {
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    mutation.connectNodeToNode(cx.value as Context, draftNetwork, outNode, outPort, inNode, inPort);
    onChangeSignal("connect-node-to-node");
  });
  updateContext();
}

export function connectInletToNode(network: Network, inlet: Inlet, node: Node, port: Port) {
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    mutation.connectInletToNode(draftNetwork, inlet, node, port);
    onChangeSignal("connect-inlet-to-node");
  });
  updateContext();
}

export function connectNodeToOutlet(network: Network, node: Node, port: Port, outlet: Outlet) {
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    mutation.connectNodeToOutlet(draftNetwork, node, port, outlet);
    onChangeSignal("connect-node-to-outlet");
  });
  updateContext();
}

export function disconnect(network: Network, inputNodeId: string, inputPortName: string) {
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    mutation.disconnect(cx.value as Context, draftNetwork, inputNodeId, inputPortName);
    onChangeSignal("disconnect");
  });
  updateContext();
}

export function setRenderedNode(network: Network, nodeId: string | undefined) {
  if (nodeId)
    project.value = produce(project.value!, (draft) => {
      const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
      // Check that the node id exists on the given network
      if (findNodeById(draftNetwork, nodeId)) {
        draftNetwork.renderedNode = nodeId;
      }
      onChangeSignal("set-rendered-node");
    });
  updateContext();
}

export function setItemPositions(network: Network, positions: Map<string, Point>) {
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    for (const [id, position] of positions.entries()) {
      const item = draftNetwork.children.find((item) => item.id === id);
      if (item) {
        item.x = position.x;
        item.y = position.y;
      }
    }
    const changeKey = Array.from(positions.keys()).join(",");
    onChangeSignal("set-positions", changeKey);
  });
}

export async function addAsset(filename: string) {
  project.value = produce(project.value!, (draft) => {
    draft.assets[filename] = filename;
    onChangeSignal("add-asset", filename);
  });
  await loadAsset(cx.value!, userId.value!, projectId.value!, filename);
}

export async function deleteAsset(filename: string) {
  project.value = produce(project.value!, (draft) => {
    delete draft.assets[filename];
    onChangeSignal("delete-asset", filename);
  });
}

export function setStickySize(network: Network, sticky: Sticky, width: number, height: number) {
  project.value = produce(project.value!, (draft) => {
    const draftNetwork = draft.items.find((item: Item) => item.id === network.id) as Network;
    const draftSticky = draftNetwork.children.find((n: NetworkItem) => n.id === sticky.id) as Sticky;
    draftSticky.width = width;
    draftSticky.height = height;
    onChangeSignal("set-sticky-size", sticky.id);
  });
}

export function setGalleryMetadata(metadata?: string, value?: string | undefined) {
  project.value = produce(project.value!, (draft) => {
    const dflt: { [key: string]: string | undefined } = {
      keyword: project.value!.title,
      subKeyword: ">" + project.value!.title,
    };
    const draftItem = draft.items.find((i: Item) => i.id === activeItem!.value!.id);
    if (draftItem) {
      if (metadata && Object.keys(dflt).includes(metadata)) {
        const galsection = (draftItem as Record<string, unknown>)["__gallery"] as Record<string, unknown>;
        galsection[metadata] = value;
      } else (draftItem as Record<string, unknown>)["__gallery"] = dflt;
    }
    onChangeSignal("set-item-meta", "__gallery");
  });
}
export function removeGalleryMetadata() {
  project.value = produce(project.value!, (draft) => {
    const draftItem = draft.items.find((i: Item) => i.id === activeItem!.value!.id);
    if (draftItem) {
      (draftItem as Record<string, unknown>)["__gallery"] = undefined;
    }
    onChangeSignal("remove-item-meta", "__gallery");
  });
}

export const selectionIdsMap = signal<Map<string, Set<string>>>(new Map());
