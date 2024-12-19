import { useState, useEffect, useRef } from "react";
import { outlinerMode, OutlinerMode, renderToSvg, setProjectItemName, removeNetworkInProject } from "./signals";
import { Menu, MenuItem } from "../components/menu";
import clsx from "clsx";
import {
  cx,
  project,
  activeItemId,
  pinnedItemId,
  createItemModalVisible,
  addDependencyModalVisible,
  requestRender,
  setActiveItemId,
  togglePinnedItemId,
} from "./signals";
import Icon from "../components/icon";
import { Item, Project, Network, NetworkItem, Node } from "@ndbx/runtime";
import FullscreenModal from "../components/fullscreen-modal";

interface MousePosition {
  x: number;
  y: number;
}

interface ItemItemProps {
  fqId: string;
  item: Item;
  className?: string;
  readOnly?: boolean;
}

function ItemItem({ fqId, item, className, readOnly }: ItemItemProps) {
  const [cursorStyle, setCursorStyle] = useState("pointer");
  const [contextMenu, setContextMenu] = useState<MousePosition | null>(null);
  const [editingProjectItemName, setEditingProjectItemName] = useState<boolean>(false);
  const [temporaryProjectItemName, setTemporaryProjectItemName] = useState<string>(item.name || "Untitled");
  const [isDeleteModalOpen, setDeleteModalOpen] = useState(false);
  const networkProjectItemInputRef = useRef<HTMLInputElement>(null);
  const itemNameForDeletionInputRef = useRef<HTMLInputElement>(null);
  const [isValidName, setIsValidName] = useState(true);
  const validateName = (name: string) => /^[a-zA-Z0-9_\-\s]{2,32}$/.test(name);
  function networkIsUsedInProject(networkID: string) {
    const networkItem = project.value!.items.find((item) => item.id === networkID);
    if (networkItem === undefined) return true;
    const networkItemName = networkItem.name;
    const isNode = (item: NetworkItem): item is Node => item.type === "NODE";
    const references = project
      .value!.items.filter((item): item is Network => item.type === "NETWORK")
      .filter((item: Network) =>
        item.children.some((child) => isNode(child) && child.fn === `self/self/${networkItemName}`),
      );

    return references.length > 0;
  }

  function handleTogglePinned(e: React.MouseEvent) {
    e.stopPropagation();
    togglePinnedItemId(fqId);
    requestRender();
  }

  function handleContextMenu(e: React.MouseEvent) {
    e.preventDefault();
    setContextMenu({ x: e.clientX + 2, y: e.clientY - 6 });
  }

  function handleDeleteNetwork() {
    setContextMenu(null);
    setEnteredItemName("");
    setDeleteModalOpen(true); // Open the confirmation modal
  }

  function confirmDeletion(e: React.MouseEvent<HTMLButtonElement, MouseEvent>) {
    e.stopPropagation();
    activeItemId.value = null;
    removeNetworkInProject(item.id); // Call the actual deletion function
    setDeleteModalOpen(false); // Close the modal after confirmation
  }

  useEffect(() => {
    if (editingProjectItemName) {
      networkProjectItemInputRef.current?.select();
    }
  }, [editingProjectItemName]);
  useEffect(() => {
    if (isDeleteModalOpen && itemNameForDeletionInputRef.current) {
      itemNameForDeletionInputRef.current.focus(); // Focus the input when the modal is open
    }
  }, [isDeleteModalOpen]); // Run this effect when `isDeleteModalOpen` changes
  function handleClickProjectItemName() {
    if (!readOnly) {
      setCursorStyle("text");
      setTimeout(() => {
        setCursorStyle("pointer");
      }, 1000);
    }
  }

  function handleDoubleClickProjectItemName() {
    if (!readOnly && outlinerMode.value === OutlinerMode.Project) {
      setEditingProjectItemName(true);
      setTemporaryProjectItemName(item.name);
    }
  }

  const handleKeyDownProjectItemName = (e: React.KeyboardEvent<HTMLInputElement>) => {
    const key = e.key;
    const allowedPattern = /^[a-zA-Z0-9_\-\s]$/;
    const specialKeys = ["Backspace", "Delete", "ArrowLeft", "ArrowRight", "Tab", "Enter"];
    const currentLength = temporaryProjectItemName.length;

    if (!allowedPattern.test(key) && !specialKeys.includes(key)) {
      e.preventDefault();
    }
    if (currentLength >= 32 && !specialKeys.includes(key)) {
      e.preventDefault();
    }
    if (e.key === "Enter") {
      if (isValidName) {
        setEditingProjectItemName(false);
        setProjectItemName(item.id, temporaryProjectItemName);
      } else {
        e.preventDefault();
        setEditingProjectItemName(false);
      }
    } else if (e.key === "Escape") {
      setEditingProjectItemName(false);
    }
  };

  const handleBlurProjectItemName = () => {
    if (isValidName) {
      setProjectItemName(item.id, temporaryProjectItemName);
    }
    setEditingProjectItemName(false);
    setCursorStyle("pointer");
  };
  const handleInputChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setTemporaryProjectItemName(value);
    setIsValidName(validateName(value));
  };

  async function handleCopyAsSvg() {
    setContextMenu(null);
    const svgString = await renderToSvg();
    if (!svgString) return;
    navigator.clipboard.writeText(svgString);
  }

  const isActive = activeItemId.value === fqId;
  const isPinned = pinnedItemId.value === fqId;

  const [enteredItemName, setEnteredItemName] = useState("");

  return (
    <div
      className={clsx("flex cursor-pointer text-zinc-300", className, {
        "bg-blue-700": isActive,
        "hover:bg-blue-800": isActive,
        "hover:bg-zinc-900": activeItemId.value !== fqId,
      })}
      onClick={() => setActiveItemId(fqId)}
      onContextMenu={handleContextMenu}
    >
      <span
        className={`text-xs w-full p-2 cursor-${cursorStyle}`}
        onClick={handleClickProjectItemName}
        onDoubleClick={handleDoubleClickProjectItemName}
      >
        {!editingProjectItemName && item.name}
        {editingProjectItemName && (
          <input
            ref={networkProjectItemInputRef}
            className={`${isValidName ? "bg-zinc-800" : "bg-red-600"} border-none text-inherit outline-none text-left`}
            type="text"
            value={temporaryProjectItemName}
            onChange={handleInputChange}
            onKeyDown={handleKeyDownProjectItemName}
            onBlur={handleBlurProjectItemName}
          />
        )}
      </span>
      <span
        className={clsx("opacity-0 hover:opacity-100 flex justify-center items-center p-2", {
          "bg-blue-500": isPinned,
          "opacity-100": isPinned,
        })}
        onClick={(e) => handleTogglePinned(e)}
      >
        <Icon name="pin" size={16} />
      </span>
      <Menu open={!!contextMenu} onClose={() => setContextMenu(null)} anchorPosition={contextMenu || { x: 0, y: 0 }}>
        <MenuItem onClick={handleCopyAsSvg}>Copy as SVG</MenuItem>
        <MenuItem onClick={handleDeleteNetwork} disabled={networkIsUsedInProject(item.id)}>
          Delete {item.type.replace(/\w\S*/g, (text) => text.charAt(0).toUpperCase() + text.substring(1).toLowerCase())}
        </MenuItem>
      </Menu>
      {isDeleteModalOpen && (
        <FullscreenModal style={{ width: "min(90vw, 750px)" }} onClose={() => setDeleteModalOpen(false)}>
          <main className="flex flex-row h-full w-full relative">
            <div className="absolute top-2 right-2 cursor-pointer">
              <Icon name="x" onClick={() => setDeleteModalOpen(false)} size={24} />
            </div>
            <div className="modal-content flex-1 bg-zinc-900 px-8">
              <h1 className="mt-2 mb-6 font-bold text-sm">Delete Item "{item.name}"</h1>
              <div className="p-4">
                <p className="text-zinc-300 mt-2">
                  Please type the name of the item "<strong>{item.name}</strong>" to confirm deletion.
                </p>
                <input
                  ref={itemNameForDeletionInputRef} // Attach the ref to the input
                  className="w-full border border-zinc-600 p-2 rounded bg-zinc-800 text-white mt-4"
                  type="text"
                  value={enteredItemName}
                  onChange={(e) => setEnteredItemName(e.target.value)}
                  placeholder={item.name}
                />
                <div className="mt-4 flex justify-end space-x-2">
                  <button
                    onClick={(e) => {
                      confirmDeletion(e);
                    }}
                    className={`border bg-blue-700 hover:bg-zinc-900 border-zinc-700 rounded p-4 text-zinc-100 w-full cursor-pointer ${
                      enteredItemName === item.name ? "" : "opacity-50 cursor-not-allowed"
                    }`}
                    disabled={enteredItemName !== item.name}
                  >
                    Delete
                  </button>
                </div>
              </div>
            </div>
          </main>
        </FullscreenModal>
      )}
    </div>
  );
}

interface ProjectItemProps {
  projectKey: string;
  project: Project;
}

function ProjectItem({ projectKey, project }: ProjectItemProps) {
  return (
    <div className={clsx("flex flex-col text-zinc-300")}>
      <span className="text-xs w-full p-2 font-bold text-zinc-100">{project.title}</span>
      <div className="flex flex-col w-full">
        {project.items.map((item: Item) => (
          <ItemItem key={item.id} fqId={`${projectKey}/${item.id}`} className="pl-2" item={item} />
        ))}
      </div>
    </div>
  );
}

export default function Outliner() {
  useEffect(() => {
    const hash = window.location.hash.substring(1); // Remove the leading #
    if (hash) {
      const projectItems = project.value?.items || [];
      for (const item of projectItems) {
        if (item.id === hash) {
          setActiveItemId(`self/self/${item.id}`);
          break;
        }
      }
    }
  }, []);

  const handlePlus = () => {
    if (outlinerMode.value === OutlinerMode.Project) {
      createItemModalVisible.value = true;
    } else {
      addDependencyModalVisible.value = true;
    }
  };

  function handleDisableContextMenu(e: React.MouseEvent) {
    e.preventDefault();
  }

  if (!cx.value || !project.value) {
    return <div className="h-full w-48 border-r border-zinc-700">Loading...</div>;
  }

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      <div className="flex justify-between items-center px-2 h-10 border-b border-b-zinc-700">
        <div className="flex gap-4">
          <span
            className={clsx("text-xs text-zinc-200 cursor-pointer", {
              "font-bold": outlinerMode.value === OutlinerMode.Project,
            })}
            onClick={() => (outlinerMode.value = OutlinerMode.Project)}
          >
            Project
          </span>
          <span
            className={clsx("text-xs text-zinc-200 cursor-pointer", {
              "font-bold": outlinerMode.value === OutlinerMode.Dependencies,
            })}
            onClick={() => (outlinerMode.value = OutlinerMode.Dependencies)}
          >
            Dependencies
          </span>
        </div>
        <Icon name="plus" size={16} onClick={handlePlus} className="cursor-pointer" />
      </div>
      <div
        className="items flex-1 flex flex-col overflow-y-auto overflow-x-hidden"
        onContextMenu={handleDisableContextMenu}
      >
        {outlinerMode.value === OutlinerMode.Project &&
          project.value!.items.map((item: Item) => (
            <ItemItem fqId={`self/self/${item.id}`} key={item.id} item={item} />
          ))}
        {outlinerMode.value === OutlinerMode.Dependencies &&
          Array.from(cx.value.dependencies).map(([key, project]: [string, Project]) => (
            <ProjectItem key={key} projectKey={key} project={project} />
          ))}
      </div>
    </div>
  );
}
