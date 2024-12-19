import React, { FormEvent } from "react";
import clsx from "clsx";

import Icon from "../components/icon";
import { createItemModalVisible, createItem, project } from "./signals";
//import { useEditorStore } from "./editor-store";
import FullscreenModal from "../components/fullscreen-modal";
import { SubmitField, TextField } from "../components/fields";
import InlineMessage from "../components/inline-message";

export default function CreateItemModal() {
  //   const createFunction = useEditorStore((state) => state.createFunction);
  //   const setActiveFunctionName = useEditorStore((state) => state.setActiveFunctionName);
  const [itemName, setItemName] = React.useState("");
  const [itemType, setItemType] = React.useState("NETWORK");
  const [error, setError] = React.useState<string | null>(null);

  const handleClose = () => {
    createItemModalVisible.value = false;
  };

  const handleItemNameChange = (name: string) => {
    setItemName(name);
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    try {
      createItem(itemName.trim(), itemType);
      handleClose();
    } catch (err) {
      console.error(err);
      setError((err as Error)?.message);
    }
  };

  const validItemName = itemName.match(/^[a-zA-Z0-9_\-\s]{2,32}$/);
  const alreadyExists = project.value!.items.some((item) => item.name === itemName);

  return (
    <FullscreenModal style={{ width: "min(90vw, 750px)" }} onClose={handleClose}>
      <main className="flex flex-row h-full w-full relative">
        <div className="absolute top-2 right-2 cursor-pointer">
          <Icon name="x" onClick={handleClose} size={24} />
        </div>
        <div className="modal-content flex-1 bg-zinc-900 px-8">
          <h1 className="mt-2 mb-6 font-bold text-sm">Create Item</h1>
          {error && <InlineMessage key={Date.now()}>{error}</InlineMessage>}
          <form onSubmit={handleSubmit}>
            <TextField
              name="name"
              label="Item Name"
              value={itemName}
              onChange={(e) => handleItemNameChange(e.target.value)}
              autoFocus={true}
              autoComplete="off"
              placeholder="My Item"
            />
            <div className="flex justify-center items-center gap-5 mb-10">
              <div
                className={clsx(
                  "rounded-lg px-8 py-2 w-32 border border-zinc-600 cursor-pointer flex flex-col items-center text-zinc-200 gap-1",
                  { "bg-blue-500": itemType === "NETWORK" },
                )}
                onClick={() => setItemType("NETWORK")}
              >
                <Icon name="network" size={24} />
                <span className="text-xs">Network</span>
              </div>

              <div
                className={clsx(
                  "rounded-lg px-8 py-2 w-32 border border-zinc-600 cursor-pointer flex flex-col items-center text-zinc-200 gap-1",
                  { "bg-blue-500": itemType === "FUNCTION" },
                )}
                onClick={() => setItemType("FUNCTION")}
              >
                <Icon name="code" size={24} />
                <span className="text-xs">Function</span>
              </div>
            </div>
            <div className="flex justify-center mb-6">
              <SubmitField name="create" label="Create Item" disabled={!validItemName || alreadyExists} />
            </div>
          </form>
        </div>
      </main>
    </FullscreenModal>
  );
}
