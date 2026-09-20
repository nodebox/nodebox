import React, { FormEvent } from "react";
import Icon from "../components/icon";
import {
  addItemParameter,
  activeItem,
  createParameterModalDefaultName,
  createParameterModalDefaultType,
  createParameterModalCallback,
  createParameterModalDefaultValue,
  createParameterModalVisible,
} from "./signals";
import { LiteralValue, ParameterType } from "@ndbx/runtime";
import FullscreenModal from "../components/fullscreen-modal";
import { TextField, ChoiceField, Option, SubmitField } from "../components/fields";
import InlineMessage from "../components/inline-message";

const PARAMETER_TYPES: Option[] = [
  { name: ParameterType.Number, label: "Number" },
  { name: ParameterType.String, label: "String" },
  { name: ParameterType.Boolean, label: "Boolean" },
  { name: ParameterType.Color, label: "Color" },
  { name: ParameterType.Point, label: "Point" },
];

interface CreateParameterModalProps {
  defaultName?: string;
  defaultType?: ParameterType;
  handleSave: (expression?: any) => void;
}

export default function CreateParameterModal({ defaultName, defaultType, handleSave }: CreateParameterModalProps) {
  const [name, setName] = React.useState(defaultName || "");
  const [type, setType] = React.useState<ParameterType>(defaultType || ParameterType.Number);
  const [error, setError] = React.useState<string | null>(null);

  const handleClose = () => {
    setError(null);
    createParameterModalCallback.value = () => {};
    createParameterModalVisible.value = false;
  };

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    try {
      const newName = name.trim();
      const defaultFromSignal = createParameterModalDefaultValue.value;
      const defaultValue =
        defaultFromSignal ||
        (type === ParameterType.Number
          ? 0
          : type === ParameterType.String
            ? ""
            : type === ParameterType.Boolean
              ? false
              : type === ParameterType.Color
                ? "#000000"
                : [0, 0]);
      addItemParameter(activeItem.value!, newName, type, defaultValue as unknown as LiteralValue);
      handleSave();
    } catch (err) {
      setError((err as Error)?.message);
    }
  };

  const reservedNames = [
    "type",
    "id",
    "name",
    "background",
    "children",
    "connections",
    "renderedNode",
    "outputPorts",
    "canvasSize",
    "category",
    "width",
    "parameters",
    "description",
  ];
  const validName =
    name.match(/^[a-zA-Z0-9_]{1,40}$/) &&
    !name.match(/^[0-9]/) &&
    !name.match(/^[A-Z]/) &&
    reservedNames.indexOf(name) === -1;
  function getNameError(name: string, activeItem: any): string | null {
    if (!name) return null;

    if (activeItem.value?.parameters?.some((p: { name: string }) => p.name === name)) {
      return "Parameter name already exists";
    }

    if (reservedNames.includes(name)) {
      return `'${name}' is a reserved keyword`;
    }

    if (name.match(/^[0-9]/)) {
      return "Name cannot start with a number";
    }

    if (name.match(/^[A-Z]/)) {
      return "Name cannot start with an uppercase letter";
    }

    if (!name.match(/^[a-zA-Z0-9_]{1,40}$/)) {
      return "Name can only contain letters, numbers and underscore (max 40 chars)";
    }

    return null;
  }
  const nameError = getNameError(name, activeItem);
  return (
    <FullscreenModal style={{ width: "min(90vw, 750px)" }} onClose={handleClose}>
      <main className="flex flex-row h-full w-full relative">
        <div className="absolute top-2 right-2 cursor-pointer">
          <Icon name="x" onClick={handleClose} size={24} />
        </div>
        <div className="modal-content flex-1 bg-zinc-900 px-8">
          <h1 className="mt-2 mb-6 font-bold text-sm">Create Network Parameter</h1>
          {error && <InlineMessage key={Date.now()}>{error}</InlineMessage>}
          <form onSubmit={handleSubmit}>
            <div>
              <TextField
                name="name"
                label="Parameter Name"
                value={name}
                onChange={(e) => {
                  setName(e.target.value);
                  createParameterModalDefaultName.value = e.target.value;
                }}
                autoFocus={true}
                autoComplete="off"
                placeholder="myParameter"
              />
              <span className="absolute -translate-y-[22px] text-xs text-red-400">{nameError}</span>
            </div>
            <ChoiceField
              name="type"
              label="Type"
              options={PARAMETER_TYPES}
              value={type}
              onChange={(e) => {
                setType(e.target.value as ParameterType);
                createParameterModalDefaultType.value = e.target.value as ParameterType;
              }}
            />
            <div className="flex justify-center mb-6">
              <SubmitField name="create" label="Create Parameter" disabled={!validName || nameError !== null} />
            </div>
          </form>
        </div>
      </main>
    </FullscreenModal>
  );
}
