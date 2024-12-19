import React from "react";
import clsx from "clsx";
import Fuse from "fuse.js";
import Markdown from "react-markdown";

import Icon from "../components/icon";
import FullscreenModal from "../components/fullscreen-modal";
import { Project, Item, Network } from "@ndbx/runtime";
import {
  cx,
  project as _project,
  activeItem,
  createNodeModalVisible,
  createNodeModalVisibleMode,
  createNetworkItemPosition,
  createNode,
  replaceNode,
  selectionIdsMap,
} from "./signals";

interface ItemDetail {
  userId: string;
  projectId: string;
  item: Item;
}

function ItemName({
  detail,
  isSelected,
  onClick,
  onDoubleClick,
}: {
  detail: ItemDetail;
  isSelected: boolean;
  onClick: () => void;
  onDoubleClick: () => void;
}) {
  return (
    <div
      className={clsx(
        "flex flex-row items-center gap-2 p-2 rounded cursor-pointer select-none w-full hover:bg-zinc-800",
        {
          "bg-blue-800": isSelected,
        },
      )}
      onClick={onClick}
      onDoubleClick={onDoubleClick}
    >
      <img
        src={`https://api.dicebear.com/8.x/shapes/svg?seed=${detail.item.name}`}
        alt={detail.item.name}
        width={30}
        height={30}
      />
      <span className="text-sm text-zinc-100">{detail.item.name}</span>
    </div>
  );
}

function ItemDetail({ detail }: { detail: ItemDetail | undefined }) {
  if (!detail) {
    return null;
  }
  return (
    <div className="py-2 px-3">
      <div className="flex justify-between items-start">
        <div>
          <h1 className="text-lg font-bold">{detail.item.name}</h1>
          <div className="text-sm text-zinc-500" style={{ marginTop: -2 }}>
            {detail.item.category}
          </div>
        </div>
        <div className="text-xs text-zinc-500 pt-1">
          From{" "}
          {(detail.userId === "self" && detail.projectId === "self" && "this project") ||
            `${detail.userId}/${detail.projectId}`}
        </div>
      </div>
      <div className="mt-3 text-sm text-zinc-300 markdown">
        <Markdown>{detail.item.description}</Markdown>
      </div>
    </div>
  );
}

export default function CreateNodeModal() {
  const [searchText, setSearchText] = React.useState("");
  const [selectedItemIndex, setSelectedItemIndex] = React.useState(0);
  const searchFieldRef = React.useRef(null);
  const itemNamesRef = React.useRef<HTMLDivElement | null>(null);

  function handleCreateNode(detail: ItemDetail) {
    const { userId, projectId, item } = detail;
    const fqId = `${userId}/${projectId}/${item.name}`;
    const newNode = createNode(activeItem.value as Network, fqId, createNetworkItemPosition.value);
    if (newNode) {
      const newSelectionMap = new Map(selectionIdsMap.value);
      newSelectionMap.set((activeItem.value as Network).id, new Set([newNode.id]));
      selectionIdsMap.value = newSelectionMap;
    }
    handleClose();
  }

  function handleReplaceNode(detail: ItemDetail) {
    const { userId, projectId, item } = detail;
    const fqId = `${userId}/${projectId}/${item.name}`;
    replaceNode(activeItem.value as Network, fqId, createNodeModalVisibleMode.value);
    handleClose();
  }

  function handleCreateSelectedNode() {
    const details = getFilteredItemDetails();
    const detail = details[selectedItemIndex];
    if (detail) {
      handleCreateNode(detail);
    }
  }

  function handleDoubleClick(detail: ItemDetail) {
    const existingNode = createNodeModalVisibleMode.value !== "CREATE";
    if (existingNode) {
      handleReplaceNode(detail);
    } else {
      handleCreateNode(detail);
    }
  }

  function handleClose() {
    createNodeModalVisible.value = false;
    createNodeModalVisibleMode.value = "CREATE";
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Enter") {
      e.preventDefault();
      handleCreateSelectedNode();
    } else if (e.key === "Escape") {
      e.preventDefault();
      handleClose();
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      handleNodeIndexChange(-1);
    } else if (e.key === "ArrowDown") {
      e.preventDefault();
      handleNodeIndexChange(1);
    }
  }

  function handleNodeIndexChange(delta: number) {
    const details = getFilteredItemDetails();
    const newIndex = (selectedItemIndex + delta + details.length) % details.length;
    setSelectedItemIndex(newIndex);
    itemNamesRef.current!.children[newIndex].scrollIntoView({ block: "nearest" });
  }

  function getAllItemDetails(): ItemDetail[] {
    const project = _project.value as Project;
    const functionDetails: ItemDetail[] = [];
    for (const item of project.items) {
      if (item.id === activeItem.value?.id) continue;
      if (item.type === "FUNCTION" && !item.source.match(/export\s+default/)) continue;
      const itemDetail = { userId: "self", projectId: "self", item };
      functionDetails.push(itemDetail);
    }
    for (const dependencyId of Object.keys(project.dependencies)) {
      const [userId, projectId] = dependencyId.split("/");
      const dependency = cx.value!.dependencies.get(dependencyId)!;
      for (const item of dependency.items) {
        if (item.type === "FUNCTION" && !item.source.match(/export\s+default/)) continue;
        const itemDetail = { userId, projectId, item };
        functionDetails.push(itemDetail);
      }
    }
    return functionDetails;
  }

  function getFilteredItemDetails(): ItemDetail[] {
    const details = getAllItemDetails();
    const threshold = 0.3;
    if (searchText.trim().length === 0) {
      return details;
    } else {
      const fuse = new Fuse(details, {
        keys: ["item.name", "item.description"],
        includeScore: true,
        threshold: threshold,
        ignoreLocation: true,
        minMatchCharLength: 1,
      });
      const result = fuse.search(searchText);
      return result.filter((r) => r.score !== undefined && r.score <= threshold).map((r) => r.item);
    }
  }

  const details = getFilteredItemDetails();
  const selectedItem = details[selectedItemIndex];

  return (
    <FullscreenModal style={{ width: "min(90vw, 750px)" }} onClose={handleClose}>
      <main className="flex flex-row h-full w-full relative" style={{ height: "min(90vh, 600px)" }}>
        <div className="absolute top-2 right-2 cursor-pointer">
          <Icon name="x" onClick={handleClose} size={24} />
        </div>
        <div className="modal-content flex-1 flex flex-col bg-zinc-900 w-full">
          <h1 className="mt-2 mb-3 px-2 font-bold text-sm">
            {createNodeModalVisibleMode.value === "CREATE" ? "Create" : "Replace"} Node
          </h1>
          <input
            className="w-full h-8 px-2 py-2 bg-transparent border-b border-b-zinc-800 text-zinc-200 outline-none"
            type="text"
            ref={searchFieldRef}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
            tabIndex={10}
            onKeyDown={handleKeyDown}
            placeholder="Search..."
            autoFocus
          />
          <div className="flex-1 flex flex-row overflow-hidden">
            <div
              className="w-64 flex flex-col px-1 py-1 border-r border-r-zinc-800 h-full overflow-x-hidden overflow-y-auto"
              ref={itemNamesRef}
            >
              {details.map((detail, index) => (
                <ItemName
                  key={`${detail.userId}/${detail.projectId}/${detail.item.name}`}
                  detail={detail}
                  isSelected={index === selectedItemIndex}
                  onClick={() => setSelectedItemIndex(index)}
                  onDoubleClick={() => handleDoubleClick(detail)}
                />
              ))}
              {details.length === 0 && (
                <div className="w-full h-full flex items-center justify-center">
                  <span className="text-zinc-600">No items found</span>
                </div>
              )}
            </div>
            <div className="flex-1 overflow-y-auto">
              <ItemDetail detail={selectedItem} />
            </div>
          </div>
        </div>
      </main>
    </FullscreenModal>
  );
}
