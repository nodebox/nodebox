import React, { FormEvent } from "react";
import Icon from "../components/icon";
import { createOutletModalVisible, createOutlet, activeItem } from "./signals";
import { Network, PortType } from "@ndbx/runtime";
import FullscreenModal from "../components/fullscreen-modal";
import { TextField, ChoiceField, Option, SubmitField } from "../components/fields";
import InlineMessage from "../components/inline-message";

export default function CreateOutletModal() {
  const [name, setName] = React.useState("out");
  const [type, setType] = React.useState(PortType.Shape);
  const [error, setError] = React.useState<string | null>(null);

  const handleClose = () => {
    createOutletModalVisible.value = false;
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    try {
      createOutlet(activeItem.value as Network, name.trim(), type);
      handleClose();
    } catch (err) {
      console.error(err);
      setError((err as Error)?.message);
    }
  };

  const validName = name.match(/^[a-zA-Z0-9_\-\s]{2,40}$/);
  const alreadyExists = (activeItem.value as Network).outputPorts?.some((p) => p.name === name);
  const nameError = alreadyExists ? "Name already exists" : !validName ? "Invalid name" : null;

  const PORT_TYPES: Option[] = [
    {
      name: PortType.Table as string,
      label: "Table",
    },
    { name: PortType.Shape as string, label: "Shape" },
  ];

  return (
    <FullscreenModal style={{ width: "min(90vw, 750px)" }} onClose={handleClose}>
      <main className="flex flex-row h-full w-full relative">
        <div className="absolute top-2 right-2 cursor-pointer">
          <Icon name="x" onClick={handleClose} size={24} />
        </div>
        <div className="modal-content flex-1 bg-zinc-900 px-8">
          <h1 className="mt-2 mb-6 font-bold text-sm">Create Outlet</h1>
          {error && <InlineMessage key={Date.now()}>{error}</InlineMessage>}
          <form onSubmit={handleSubmit}>
            <TextField
              name="name"
              label="Outlet Name"
              value={name}
              onChange={(e) => setName(e.target.value)}
              autoFocus={true}
              autoComplete="off"
              placeholder="out"
            />
            <span className="absolute -translate-y-[25px] text-xs text-red-400">{nameError}</span>
            <ChoiceField
              name="type"
              label="Type"
              options={PORT_TYPES}
              value={type}
              onChange={(e) => setType(e.target.value as PortType)}
            />
            <div className="flex justify-center mb-6">
              <SubmitField name="create" label="Create Outlet" disabled={!validName || alreadyExists} />
            </div>
          </form>
        </div>
      </main>
    </FullscreenModal>
  );
}
