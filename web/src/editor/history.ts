import { computed, signal } from "@preact/signals-react";
import { Project } from "@ndbx/runtime";
import { debounce } from "../util";
import { updateProjectTitleInProfile, selectionIdsMap } from "./signals";
interface HistoryEntry {
  project: Project;
  changeType: string;
  changeKey?: string;
  timestamp: number;
  selectionIds: Map<string, Set<string>>;
}

export const undoStack = signal<HistoryEntry[]>([]);
(document as any).undoStack = undoStack;
export const redoStack = signal<HistoryEntry[]>([]);
export const canUndo = computed(() => {
  return Boolean(undoStack.value?.length > 0);
});
export const canRedo = computed(() => redoStack.value.length > 0);

export const lastChangeType = signal<string>("");
export const lastChangeKey = signal<string>("");
export const lastChangeTime = signal<number>(0);

const UNDO_REDO_DELAY = 500;

function cloneSelectionMap(map: Map<string, Set<string>>) {
  const newMap = new Map<string, Set<string>>();
  for (const [key, value] of map.entries()) {
    newMap.set(key, new Set(value));
  }
  return newMap;
}

export function createHistoryEntry(project: Project, changeType: string, changeKey?: string): HistoryEntry {
  return {
    project: structuredClone(project),
    changeType,
    changeKey,
    timestamp: Date.now(),
    selectionIds: cloneSelectionMap(selectionIdsMap.value),
  };
}

export function applyHistoryEntry(entry: HistoryEntry) {
  selectionIdsMap.value = cloneSelectionMap(entry.selectionIds);
}

export const debouncedPushHistory = debounce((entry: HistoryEntry) => {
  undoStack.value = [...undoStack.value, entry];
  redoStack.value = [];
}, UNDO_REDO_DELAY);

export function pushHistory(project: Project, changeType: string, changeKey?: string) {
  const entry = createHistoryEntry(project, changeType, changeKey);
  const canCombine =
    changeType === lastChangeType.value &&
    changeKey === lastChangeKey.value &&
    Date.now() - lastChangeTime.value < UNDO_REDO_DELAY;

  if (!canCombine) {
    debouncedPushHistory(entry);
  }

  lastChangeType.value = changeType;
  lastChangeKey.value = changeKey || "";
  lastChangeTime.value = Date.now();
}

export function undo(currentProject: Project): Project | undefined {
  if (undoStack.value.length === 0) return undefined;

  redoStack.value = [
    ...redoStack.value,
    {
      project: structuredClone(currentProject),
      changeType: lastChangeType.value,
      changeKey: lastChangeKey.value,
      timestamp: Date.now(),
      selectionIds: cloneSelectionMap(selectionIdsMap.value),
    },
  ];

  const previousEntry = undoStack.value.pop()!;

  lastChangeType.value = previousEntry.changeType;
  lastChangeKey.value = previousEntry.changeKey || "";
  lastChangeTime.value = previousEntry.timestamp;

  switch (previousEntry.changeType) {
    case "set-project-title":
      updateProjectTitleInProfile(previousEntry.project.title);
      break;
    case "publish-project":
      break;
    default:
      break;
  }

  applyHistoryEntry(previousEntry);

  selectionIdsMap.value = new Map();
  selectionIdsMap.value = previousEntry.selectionIds;

  return previousEntry.project;
}

export function redo(currentProject: Project): Project | undefined {
  if (redoStack.value.length === 0) return undefined;

  const currentEntry = createHistoryEntry(currentProject, redoStack.value[redoStack.value.length - 1].changeType);
  undoStack.value = [...undoStack.value, currentEntry];

  const nextEntry = redoStack.value[redoStack.value.length - 1];
  redoStack.value = redoStack.value.slice(0, -1);

  applyHistoryEntry(nextEntry);

  selectionIdsMap.value = new Map();
  selectionIdsMap.value = nextEntry.selectionIds;

  return structuredClone(nextEntry.project);
}
