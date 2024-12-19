import React, { useEffect, useState, useRef } from "react";

import {
  Context,
  Network,
  Node,
  Parameter,
  getParameterValue,
  ParameterValue,
  defaultValueForType,
  WidgetType,
  Sticky,
  StaticValue,
  LiteralValue,
} from "@ndbx/runtime";
import BooleanWidget from "../widgets/boolean-widget";
import ColorWidget from "../widgets/color-widget";
import ExpressionWidget from "../widgets/expression-widget";
import FileWidget from "../widgets/file-widget";
import MenuWidget from "../widgets/menu-widget";
import NumberWidget from "../widgets/number-widget";
import StringWidget from "../widgets/string-widget";
import TextWidget from "../widgets/text-widget";
import ChoiceWidget from "../widgets/choice-widget";
import { WidgetProps, WidgetMetaButton, WidgetRemoveButton } from "../widgets/widget-utils";
import SegmentedButton from "../widgets/segmented-button";
import Icon from "../components/icon";
import {
  cx,
  activeItem,
  activeNetworkItemIdMap,
  collapsedSections,
  setItemMeta,
  setItemMetaGallery,
  setGalleryMetadata,
  removeGalleryMetadata,
  setItemParameterValue,
  setParameterValue,
  setStickyBackgroundColor,
  setStickyFontColor,
  setStickyFontSize,
  stickyCanvasCacheMap,
  removeItemParameter,
  parameterMetaPanelVisible,
  parameterMetaPanelPosition,
  parameterMetaPanelParameterName,
  createNodeModalVisible,
  createNodeModalVisibleMode,
  updateNodeName,
  createParameterModalVisible,
  createParameterModalDefaultName,
  createParameterModalDefaultType,
  createParameterModalDefaultValue,
  createParameterModalCallback,
} from "./signals";
import { colorToHex, hexToColor } from "../lib/color-utils";
import CreateParameterModal from "./create-parameter-modal";

function toChoices(...list: string[]) {
  return list.map((value) => ({ name: value, label: value }));
}

interface WidgetRowProps {
  label: string;
  children: React.ReactNode;
  onRemove?: () => void;
  onMeta?: (e: React.MouseEvent) => void;
}

function WidgetRow({ label, children, onRemove, onMeta }: WidgetRowProps) {
  return (
    <div className="flex items-stretch px-1 h-8 hover:bg-zinc-700 group relative">
      <WidgetMetaButton onMeta={onMeta} />
      <div className="w-24 px-1 text-xs leading-8 text-zinc-500 text-right select-none">{label}</div>
      <div className="flex-1">{children}</div>
      <WidgetRemoveButton onRemove={onRemove} />
    </div>
  );
}

// function ConnectedWidget({ label }) {
//   return (
//     <WidgetRow label={label}>
//       <span className="text-zinc-600 text-xs leading-8 px-2">&lt;connected&gt;</span>
//     </WidgetRow>
//   );
// }

type widgetClass = (props: WidgetProps) => JSX.Element;

const WIDGET_MAP: Record<WidgetType, widgetClass> = {
  NUMBER: NumberWidget,
  STRING: StringWidget,
  TEXT: TextWidget,
  BOOLEAN: BooleanWidget,
  POINT: StringWidget,
  COLOR: ColorWidget as widgetClass,
  FILE: FileWidget,
  CHOICE: ChoiceWidget,
};

interface ParameterRowProps {
  value: ParameterValue;
  parameter: Parameter;
  onChange: (value: ParameterValue) => void;
  onToggleExpression?: () => void;
  onPublishParameter?: () => void;
  onRemove?: () => void;
  onMeta?: (e: React.MouseEvent) => void;
}
export function ParameterRow({
  value,
  parameter,
  onChange,
  onToggleExpression,
  onPublishParameter,
  onRemove,
  onMeta,
}: ParameterRowProps) {
  const label = parameter.label || parameter.name;
  const editValue = value.type === "EXPRESSION" ? value.expression : value.value;
  let widgetClass = WIDGET_MAP[parameter.widget];
  if (parameter.choices) {
    return (
      <MenuWidget
        label={label}
        value={editValue}
        min={parameter.min}
        max={parameter.max}
        step={parameter.step}
        choices={parameter.choices}
        onChange={onChange}
        onToggleExpression={onToggleExpression}
        onPublishParameter={onPublishParameter}
        onRemove={onRemove}
        onMeta={onMeta}
      />
    );
  }
  if (value.type === "EXPRESSION") {
    widgetClass = ExpressionWidget;
  }
  if (!widgetClass) {
    widgetClass = () => (
      <WidgetRow label={parameter.name} onRemove={onRemove} onMeta={onMeta}>
        <span className="text-zinc-600 text-xs leading-8 px-2">&lt;{parameter.type}&gt;</span>
      </WidgetRow>
    );
  }

  return React.createElement(widgetClass, {
    name: parameter.name,
    label,
    value: editValue,
    min: parameter.min || -Infinity,
    max: parameter.max || Infinity,
    step: parameter.step || 1,
    onChange,
    onToggleExpression,
    onPublishParameter,
    onRemove,
    onMeta,
  });
}

interface NodeParameterRowProps {
  cx: Context;
  node: Node;
  parameter: Parameter;
  onChange: (value: ParameterValue) => void;
  onToggleExpression?: () => void;
  onPublishParameter?: () => void;
  onRemove?: () => void;
  onMeta?: () => void;
}

export function NodeParameterRow({
  cx,
  node,
  parameter,
  onChange,
  onToggleExpression,
  onPublishParameter,
  onRemove,
  onMeta,
}: NodeParameterRowProps) {
  const value = getParameterValue(cx, node, parameter.name);
  return (
    value && (
      <ParameterRow
        value={value}
        parameter={parameter}
        onChange={onChange}
        onToggleExpression={onToggleExpression}
        onPublishParameter={onPublishParameter}
        onRemove={onRemove}
        onMeta={onMeta}
      />
    )
  );
}

interface ItemParameterRowProps {
  parameter: Parameter;
  onChange: (value: ParameterValue) => void;
  onToggleExpression?: () => void;
  onPublishParameter?: () => void;
  onRemove?: () => void;
  onMeta?: (e: React.MouseEvent) => void;
}

export function ItemParameterRow({
  parameter,
  onChange,
  onToggleExpression,
  onPublishParameter,
  onRemove,
  onMeta,
}: ItemParameterRowProps) {
  const value = parameter.defaultValue;
  return (
    <ParameterRow
      value={{ type: "VALUE", value }}
      parameter={parameter}
      onChange={onChange}
      onToggleExpression={onToggleExpression}
      onPublishParameter={onPublishParameter}
      onRemove={onRemove}
      onMeta={onMeta}
    />
  );
}

interface ParameterSectionProps {
  cx: Context;
  node: Node;
  name: string;
  parameters: Parameter[];
  onChange: (parameter: Parameter, value: ParameterValue) => void;
  onToggleExpression: (parameter: Parameter) => void;
  onPublishParameter: (parameter: Parameter) => void;
}

function ParameterSection({
  cx,
  node,
  name,
  parameters,
  onChange,
  onToggleExpression,
  onPublishParameter,
}: ParameterSectionProps) {
  const fn = cx.lookupItemByName(node.fn);
  const section = fn ? fn.sections.find((section) => section.name === name) : undefined;
  const isCollapsed = collapsedSections.value[name] ?? section?.collapsed ?? false;
  const toggleCollapse = () => {
    collapsedSections.value = {
      ...collapsedSections.value,
      [name]: !isCollapsed,
    };
  };
  return (
    <div className="border-b border-zinc-700 last:border-none">
      <div className="flex items-center px-2 py-2 cursor-pointer text-zinc-400 font-bold" onClick={toggleCollapse}>
        <Icon name={isCollapsed ? "chevron-right" : "chevron-down"} />
        <span className="text-xs">{name}</span>
      </div>
      {!isCollapsed && (
        <>
          {parameters.map((parameter) => (
            <NodeParameterRow
              key={parameter.name}
              cx={cx}
              node={node}
              parameter={parameter}
              onToggleExpression={() => onToggleExpression(parameter)}
              onPublishParameter={() => onPublishParameter(parameter)}
              onChange={(value) => onChange(parameter, value)}
            />
          ))}
        </>
      )}
    </div>
  );
}

function ParameterModal() {
  return (
    <>
      {createParameterModalVisible.value && (
        <CreateParameterModal
          defaultName={createParameterModalDefaultName.value.split(".").pop()}
          defaultType={createParameterModalDefaultType.value}
          handleSave={(expression) => {
            createParameterModalVisible.value = false;
            createParameterModalCallback.value(expression);
          }}
        />
      )}
    </>
  );
}

export default function Properties() {
  const [isRenaming, setIsRenaming] = useState(false);
  const [nodeNewName, setNodeNewName] = useState("");
  const [originalName, setOriginalName] = useState("");
  const nodeNameInputRef = useRef<HTMLInputElement>(null);

  const item = activeItem.value;

  const network = item?.type === "NETWORK" ? (item as Network) : null;
  const networkItemId = network ? activeNetworkItemIdMap.value.get(item?.id || "")! : null;
  const networkItem = network && networkItemId ? network.children.find((item) => item.id === networkItemId)! : null;
  const node = networkItem && networkItem.type === "NODE" ? (networkItem as Node) : null;
  const sticky = networkItem && networkItem.type === "STICKY" ? (networkItem as Sticky) : null;

  useEffect(() => {
    if (isRenaming) {
      nodeNameInputRef.current?.select();
    }
  }, [isRenaming]);

  useEffect(() => {
    const handleKeyDownNodeRename = (e: KeyboardEvent) => {
      if (e.key === "F2" && node) {
        setIsRenaming(true);
        setNodeNewName(node.name);
        setOriginalName(node.name);
      }
    };

    window.addEventListener("keydown", handleKeyDownNodeRename);

    return () => {
      window.removeEventListener("keydown", handleKeyDownNodeRename);
    };
  }, [node]);

  if (!item) return null;

  const handleClickNodeRename = () => {
    if (!isRenaming && node) {
      setIsRenaming(true);
      setNodeNewName(node.name);
      setOriginalName(node.name);
    }
  };

  const handleKeyDownNodeRename = async (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      await saveName();
    } else if (e.key === "Escape") {
      setIsRenaming(false);
      setNodeNewName(originalName);
    }
  };

  const handleBlurNodeRename = async () => {
    await saveName();
  };

  const saveName = async () => {
    if (nodeNewName !== originalName) {
      if (node) updateNodeName(item as Network, node, nodeNewName.trim());
      setOriginalName(nodeNewName.trim());
    }

    setIsRenaming(false);
  };

  function handleAddGalleryMetadata() {
    setGalleryMetadata();
  }
  function handleRemoveGalleryMetadata() {
    removeGalleryMetadata();
  }
  function handleMetaChange(key: string, value: ParameterValue) {
    if (value.type !== "VALUE") return;
    setItemMeta(item!, key, value.value);
  }
  function handleMetaChangeGallery(key: string, value: string) {
    if (key !== "keyword" && key !== "subKeyword") return;
    setItemMetaGallery(item!, key, value);
  }

  function handleItemParameterChange(parameter: Parameter, value: ParameterValue) {
    if (value.type !== "VALUE") {
      throw new Error("Only value parameters are supported");
    }
    setItemParameterValue(item!, parameter.name, (value as StaticValue).value);
  }

  function handleChange(parameter: Parameter, value: ParameterValue) {
    setParameterValue(item as Network, node!, parameter.name, value);
  }

  function handleClickNodeReplace() {
    createNodeModalVisible.value = true;
    createNodeModalVisibleMode.value = node!.id;
  }

  function handleToggleExpression(parameter: Parameter) {
    if (!node) return;
    const value = getParameterValue(cx.value!, node, parameter.name);
    if (value && value.type === "VALUE") {
      const expression = JSON.stringify(value.value);
      setParameterValue(item as Network, node, parameter.name, { type: "EXPRESSION", expression });
    } else {
      const literalValue = defaultValueForType(parameter.type);
      setParameterValue(item as Network, node, parameter.name, { type: "VALUE", value: literalValue });
    }
  }

  function handlePublishParameter(parameter: Parameter) {
    if (!node) return;
    const value = getParameterValue(cx.value!, node, parameter.name);
    if (value?.type === "VALUE") {
      createParameterModalDefaultName.value = String(parameter.name).replace(/[\s]/g, "_");
      createParameterModalDefaultValue.value = value.value as unknown as LiteralValue;
    } else {
      createParameterModalDefaultName.value = String(value!.expression);
    }

    createParameterModalDefaultType.value = parameter.type;
    createParameterModalCallback.value = () => {
      const expression = "network." + createParameterModalDefaultName.value.split(".").pop();
      setParameterValue(item as Network, node, parameter.name, { type: "EXPRESSION", expression: expression });
      createParameterModalDefaultName.value = "";
    };
    createParameterModalVisible.value = true;
  }

  function handleAddParameter() {
    createParameterModalVisible.value = true;
  }

  function handleRemoveParameter(parameter: Parameter) {
    removeItemParameter(activeItem.value!, parameter.name);
  }

  function handleShowParameterModal(e: React.MouseEvent, parameterName: string) {
    parameterMetaPanelVisible.value = true;
    parameterMetaPanelParameterName.value = parameterName;
    parameterMetaPanelPosition.value = { x: e.clientX, y: e.clientY };
  }

  if (sticky != null) {
    const stickyColors = [
      { name: "yellow", backgroundColor: "#fde68a", fontColor: "#27272a" },
      { name: "pink", backgroundColor: "#fb923c", fontColor: "#27272a" },
      { name: "orange", backgroundColor: "#4d7c0f", fontColor: "#eeeeee" },
      { name: "d-green", backgroundColor: "#082f49", fontColor: "#eeeeee" },
      { name: "d-blue", backgroundColor: "#fca5a5", fontColor: "#eeeeee" },
    ];

    const segments = [
      { text: "small", fontSize: 12 },
      { text: "medium", fontSize: 18 },
      { text: "large", fontSize: 24 },
    ];

    const selectedSegment = segments.findIndex((s) => s.fontSize === sticky.fontSize);
    const handleSegmentClick = (index: number) => {
      setStickyFontSize(network!, sticky, segments[index].fontSize.toString() + "px");
      stickyCanvasCacheMap.delete(sticky.id);
    };

    return (
      <div className="flex-1 overflow-hidden flex flex-col">
        <div className="text-xs text-zinc-300 font-bold border-b border-b-zinc-700 px-2 py-3">Sticky</div>
        <div className="flex flex-col items-stretch">
          <div className="w-24 px-2 text-xs leading-8 text-zinc-500 text-left">Font size</div>
          <div className="flex-1 flex items-center justify-between h-9 px-8">
            <SegmentedButton segments={segments} selected={selectedSegment} onClick={handleSegmentClick} />
          </div>
          <div className="w-24 px-2 text-xs leading-8 text-zinc-500 text-left">Color</div>
          <div className="flex-1 flex items-center h-9 px-8 gap-6">
            {stickyColors.map((color) => {
              const isSelected = colorToHex(sticky.backgroundColor) === color.backgroundColor;
              return (
                <div
                  key={color.name}
                  className={`w-5 h-5 shadow-sm border-2 cursor-pointer rounded-full ${isSelected ? "border-zinc-100" : "border-zinc-700"}`}
                  style={{ backgroundColor: color.backgroundColor }}
                  onClick={() => {
                    setStickyFontColor(network!, sticky, hexToColor(color.fontColor));
                    setStickyBackgroundColor(network!, sticky, hexToColor(color.backgroundColor + "CC"));
                    stickyCanvasCacheMap.delete(sticky.id);
                  }}
                ></div>
              );
            })}
          </div>
        </div>
      </div>
    );
  }
  if (!node) {
    const showGalleryMetadata = cx.value!.project.__gallery != undefined;
    const showGalleryPlusIcon = network?.__gallery === undefined;
    const galleryMetadata = network?.__gallery || { keyword: "", subKeyword: "" };
    const showNetworkParameters = network != undefined;
    return (
      <div className="flex-1 flex flex-col overflow-hidden">
        <div className="flex justify-between items-center text-xs text-zinc-300 font-bold px-2 py-3">
          <span>{item.type === "NETWORK" ? "Network" : "Function"}</span>
        </div>
        {showNetworkParameters && (
          <div className="flex flex-col items-stretch overflow-y-auto overflow-x-hidden">
            <MenuWidget
              label="Size"
              value={(item as Network).canvasSize ?? "fixed"}
              choices={toChoices("fixed", "auto")}
              onChange={(value) => handleMetaChange("canvasSize", value)}
            />
            <NumberWidget
              label="Width"
              value={item.width ?? 1000}
              min={1}
              max={10000}
              step={1}
              onChange={(value) => handleMetaChange("width", value)}
              disabled={(item as Network).canvasSize === "auto"}
            />
            <NumberWidget
              label="Height"
              value={item.height ?? 1000}
              min={1}
              max={10000}
              step={1}
              onChange={(value) => handleMetaChange("height", value)}
              disabled={(item as Network).canvasSize === "auto"}
            />
            <ColorWidget
              label="Background"
              value={item.background ?? "#F7F8F9"}
              onChange={(value) => handleMetaChange("background", value)}
            />
            <hr className="h-px border-0 bg-zinc-700 my-4" />
            {/* <MenuWidget
            label="Output Type"
            value={item.outputType ?? "geometry"}
            choices={toChoices("float", "int", "string", "boolean", "point", "geometry", "object")}
            onChange={(value) => handleMetaChange("outputType", value)}
          /> */}
            <div>
              {showGalleryMetadata && (
                <div className="relative">
                  <div className="absolute" style={{ top: "-28px" }}>
                    <span className="rounded p-1 hover:bg-gray-700 inline-flex items-center">
                      <Icon
                        name={showGalleryPlusIcon ? "plus" : "minus"}
                        onClick={showGalleryPlusIcon ? handleAddGalleryMetadata : handleRemoveGalleryMetadata}
                      />
                    </span>
                  </div>
                </div>
              )}
              {/* {showGalleryMetadata && <Dropdowns values={galleryMetadata} />} */}
              <MenuWidget
                label="Category"
                value={item.category ?? "graphics"}
                choices={toChoices("graphics", "math", "string", "color", "list", "data", "image", "time", "device")}
                onChange={(value) => handleMetaChange("category", value)}
              />
              <StringWidget
                label="Description"
                value={item.description ?? ""}
                onChange={(value) => handleMetaChange("description", value)}
                isMultiline={true}
              />
              {!showGalleryPlusIcon && (
                <StringWidget
                  label="Keyword"
                  value={galleryMetadata.keyword ?? ""}
                  onChange={(value) => handleMetaChangeGallery("keyword", value as unknown as string)}
                />
              )}
              {!showGalleryPlusIcon && (
                <StringWidget
                  label=""
                  value={galleryMetadata.subKeyword ?? ""}
                  onChange={(value) => handleMetaChangeGallery("subKeyword", value as unknown as string)}
                />
              )}
            </div>

            <hr className="h-px border-0 bg-zinc-700 mt-4" />
            <div className="flex justify-between items-center text-xs text-zinc-300 font-bold px-2 py-3">
              <span>Parameters</span>
              <span className="rounded p-1 hover:bg-gray-700">
                <Icon name="plus" onClick={handleAddParameter} />{" "}
              </span>
              <ParameterModal />
            </div>

            {item.parameters?.map((parameter) => (
              <ItemParameterRow
                key={parameter.name}
                parameter={parameter}
                onChange={(value) => handleItemParameterChange(parameter, value)}
                onRemove={() => handleRemoveParameter(parameter)}
                onMeta={(e) => handleShowParameterModal(e, parameter.name)}
              />
            ))}
          </div>
        )}
      </div>
    );
  }

  const nodeFn = cx.value!.lookupItemByName(node.fn);
  if (!nodeFn) {
    // throw new Error(`Function ${node.fn} not found`);
  }

  // Group parameters by section
  const sections = nodeFn
    ? nodeFn.parameters.reduce(
        (acc, parameter) => {
          const sectionName = parameter.section || "Parameters";
          if (!acc[sectionName]) {
            acc[sectionName] = [];
          }
          acc[sectionName].push(parameter);
          return acc;
        },
        {} as Record<string, Parameter[]>,
      )
    : [];

  return (
    <div className="flex-1 flex flex-col overflow-hidden">
      <ParameterModal />

      <div className="text-xs border-b border-b-zinc-700 px-2 py-3 flex justify-between items-center">
        {isRenaming ? (
          <input
            ref={nodeNameInputRef}
            className="text-zinc-300 bg-transparent border-none text-inherit outline-none font-bold"
            type="text"
            value={nodeNewName}
            onChange={(e) => setNodeNewName(e.target.value)}
            onKeyDown={handleKeyDownNodeRename}
            onBlur={handleBlurNodeRename}
            autoFocus
          />
        ) : (
          <span className="text-zinc-300 font-bold cursor-text" onClick={handleClickNodeRename}>
            {node.name}
          </span>
        )}
        <span className="text-zinc-500 hover:underline cursor-pointer whitespace-nowrap">{node.fn}</span>
      </div>
      {!nodeFn && (
        <div className="p-2">
          <span className="text-xs">
            This node became invalid and does not have a function associated with it. Please replace it with a valid
            node.
          </span>
          <button
            className="border bg-blue-700 hover:bg-zinc-900 border-zinc-700 rounded p-4 text-zinc-100 w-full cursor-pointer"
            onClick={handleClickNodeReplace}
          >
            Replace Node
          </button>
        </div>
      )}
      <div className="flex flex-col items-stretch overflow-y-auto overflow-x-hidden">
        {Object.entries(sections).map(([sectionName, sectionParameters]) => (
          <ParameterSection
            key={sectionName}
            name={sectionName}
            parameters={sectionParameters}
            cx={cx.value!}
            node={node}
            onToggleExpression={handleToggleExpression}
            onPublishParameter={handlePublishParameter}
            onChange={handleChange}
          />
        ))}
      </div>
    </div>
  );
}
