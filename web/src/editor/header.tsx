import { useState, useRef, useEffect } from "react";
import {
  SaveState,
  saveState,
  projectModalVisible,
  shareModalVisible,
  setProjectTitle,
  project,
  updateContext,
  saveProject,
  error,
  scopePrivateNoPlan,
} from "./signals";
import { canUndo, canRedo, undo, redo, undoStack, redoStack } from "./history";
import { drawAttentionProjectScope } from "./project-modal";
import { useAuth } from "../auth-context";
import NodeBoxLogo from "../components/nodebox-logo";
import Icon from "../components/icon";
import UserMenu from "../components/user-menu";
import { computed } from "@preact/signals-react";

interface IconButtonProps {
  name: string;
  label?: string;
  tooltip?: string;
  onClick: () => void;
  disabled?: boolean;
}

function IconButton({ name, label = undefined, tooltip = undefined, onClick, disabled = false }: IconButtonProps) {
  return (
    <button
      className="hover:bg-zinc-900 flex justify-center items-center gap-1 h-full w-12"
      onClick={onClick}
      disabled={disabled}
      title={tooltip}
    >
      <Icon name={name} size={24} />
      {label && <span className="text-xs text-zinc-100">{label}</span>}
    </button>
  );
}

interface HeaderProps {
  showReadOnlyWarning: boolean;
  userId: string;
  projectId: string;
  version: string;
}

export default function Header({ showReadOnlyWarning, userId, projectId, version }: HeaderProps) {
  const { userId: currentUserId, membership } = useAuth()!;
  scopePrivateNoPlan.value = project.value?.scope === "private" && membership.membership_type !== "plus";
  const readOnly = currentUserId !== userId || scopePrivateNoPlan.value || version !== "dev";
  const showScope = membership.membership_type === "plus";
  const [editingTitle, setEditingTitle] = useState<boolean>(false);
  const [temporaryTitle, setTemporaryTitle] = useState<string>(project.value?.title || "Untitled");
  const [originalTitle, setOriginalTitle] = useState<string>(project.value?.title || "Untitled");
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const titleInputRef = useRef<HTMLInputElement>(null);
  const isExampleUser = userId === "example" && currentUserId === "example";

  const lastUndoChangeType = computed(() =>
    undoStack.value.length > 0 ? "undo: " + undoStack.value[undoStack.value.length - 1].changeType : "",
  );

  const lastRedoChangeType = computed(() =>
    redoStack.value.length > 0 ? "redo: " + redoStack.value[redoStack.value.length - 1].changeType : "",
  );

  const performUndo = () => {
    if (canUndo.value && project.value) {
      const newProjectState = undo(project.value);
      if (newProjectState) {
        updateContext(newProjectState);
        saveProject();
      }
    }
  };

  const performRedo = () => {
    if (canRedo.value && project.value) {
      const newProjectState = redo(project.value);
      if (newProjectState) {
        updateContext(newProjectState);
        saveProject();
      }
    }
  };

  useEffect(() => {
    if (editingTitle) {
      titleInputRef.current?.select();
    }
  }, [editingTitle]);

  useEffect(() => {
    const handleKeyDown = (e: globalThis.KeyboardEvent) => {
      const commandKeyPressed = e.metaKey || e.ctrlKey;
      if (commandKeyPressed && e.key === "z") {
        e.preventDefault();
        performUndo();
      }
      if (commandKeyPressed && (e.key === "y" || (e.shiftKey && e.key === "z"))) {
        e.preventDefault();
        performRedo();
      }
    };

    const handleContextMenu = (e: MouseEvent) => {
      e.preventDefault(); // Prevent right-click menu
    };
    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("contextmenu", handleContextMenu);

    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("contextmenu", handleContextMenu);
    };
  }, []);

  const handleClickTitle = () => {
    if (!readOnly) {
      setEditingTitle(true);
      setTemporaryTitle(project.value!.title);
      setOriginalTitle(project.value!.title); // Save the original title to revert on failure
    }
  };

  const handleKeyDown = async (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      // Enter confirms the edit
      await saveTitle();
    } else if (e.key === "Escape") {
      // Escape cancels the edit
      setEditingTitle(false);
      setTemporaryTitle(originalTitle); // Revert to the original title if canceled
    }
  };

  const handleBlur = async () => {
    await saveTitle();
  };

  const saveTitle = async () => {
    setErrorMessage(null); // Clear previous error messages

    if (temporaryTitle !== originalTitle) {
      try {
        // Make the API call to update the title
        saveState.value = SaveState.Saving;
        await setProjectTitle(temporaryTitle)
          .then(() => {
            saveState.value = SaveState.Saved;
            setOriginalTitle(temporaryTitle);
          })
          .catch(() => {
            setErrorMessage(null);
          });
      } catch (error) {
        // If an error occurs, revert the title and show the error
        setErrorMessage("Failed to update title. Please try again.");
        setTemporaryTitle(originalTitle); // Revert back to the original title
        saveState.value = SaveState.Error;
      }
    }

    setEditingTitle(false);
  };

  let saveStateLabel;
  if (!readOnly) {
    if (saveState.value === SaveState.Saved) {
      saveStateLabel = "All changes saved";
    } else if (saveState.value === SaveState.Saving) {
      saveStateLabel = "Saving changes...";
    } else if (saveState.value === SaveState.Error) {
      saveStateLabel = "ERROR Could not save changes";
    }
    if (saveState.value === SaveState.Clear) {
      saveStateLabel = "...";
    }
  }

  let statusMessage;
  if (showReadOnlyWarning) {
    const loginUrl = `/login?next=${userId}/${projectId}`;
    statusMessage = (
      <p className="status error text-red-400">
        WARNING: This project is read-only. Changes will not be saved.
        <a href={loginUrl}>
          <u>Do you need to log in?</u>
        </a>
      </p>
    );
  } else if (scopePrivateNoPlan.value) {
    statusMessage = (
      <p className="status error text-red-400">
        Private project.{" "}
        <a href="/membership">
          <u>Upgrade your account</u>
        </a>{" "}
        to access this premium feature or
        <br />
        <a
          href="#"
          onClick={(e) => {
            e.preventDefault();
            projectModalVisible.value = true;
            setTimeout(() => drawAttentionProjectScope(), 1);
          }}
        >
          <u>make it public</u>{" "}
        </a>{" "}
        to allow edits.
      </p>
    );
  } else if (readOnly) {
    if (version !== "dev") {
      statusMessage = (
        <>
          <p className="status text-red-400">
            This is the published version and is read-only. Changes will not be saved.
            <br />
            The working version can be found{" "}
            <b>
              <u>
                <a href={`/${userId}/${projectId}`}>here</a>
              </u>
            </b>
            .
          </p>
        </>
      );
    } else {
      statusMessage = <p className="status text-red-400">This project is read-only. Changes will not be saved.</p>;
    }
  } else if (error.value) statusMessage = <p className="status text-red-400">{error.value}</p>;

  return (
    <div className="flex items-center justify-between bg-zinc-800 text-white border-b border-b-zinc-700 h-12">
      {/* Left Section */}
      <div className="flex gap-1 items-center h-full">
        <a className="h-full w-12 hover:bg-zinc-900 flex justify-center items-center" href={`/${userId}`}>
          <NodeBoxLogo size={20} />
        </a>
        <IconButton
          name="undo"
          onClick={performUndo}
          tooltip={lastUndoChangeType.value}
          disabled={undoStack.value?.length === 0}
        />
        <IconButton name="redo" onClick={performRedo} tooltip={lastRedoChangeType.value} disabled={!canRedo.value} />
      </div>
      {/* Middle Section */}
      <div className="flex">
        {showScope && (
          <div className="mr-2">
            <Icon name={(project.value?.scope?.toString() ?? "public") === "private" ? "lock" : "unlock"} size={16} />
          </div>
        )}
        <div className="project-title text-xs cursor-text" onClick={handleClickTitle}>
          {!editingTitle && project.value?.title}
          {editingTitle && (
            <input
              ref={titleInputRef}
              className="bg-transparent border-none text-inherit outline-none text-left"
              type="text"
              value={temporaryTitle}
              onChange={(e) => setTemporaryTitle(e.target.value)}
              onKeyDown={handleKeyDown}
              onBlur={handleBlur}
            />
          )}
        </div>
      </div>
      {/* Right Section */}
      <div className="flex gap-2 justify-end items-center h-full mr-4">
        <div className="w-100">
          <div className="status-wrap text-xs text-red-400">{errorMessage}</div>
          <div className="status-wrap text-xs text-zinc-500">{statusMessage}</div>
          <div className="save-state text-xs text-zinc-500 flex justify-end">{saveStateLabel}</div>
        </div>
        <button
          onClick={() => (shareModalVisible.value = true)}
          className="px-2 h-8 bg-blue-600 hover:bg-blue-700 text-white text-xs rounded-md flex items-center"
        >
          Share
        </button>
        {isExampleUser && <IconButton name="cog" onClick={() => (projectModalVisible.value = true)} />}
        <UserMenu />
      </div>
    </div>
  );
}
