import React from "react";
import { Parameter } from "@ndbx/runtime";
import {
  parameterMetaPanelVisible,
  parameterMetaPanelPosition,
  parameterMetaPanelParameterName,
  activeItem,
  setItemParameterProperty,
} from "./signals";
import Icon from "../components/icon";

interface WidgetProps {
  label: string;
  value: string;
  onChange: (value: string) => void;
}

function TextWidget({ label, value, onChange }: WidgetProps) {
  return (
    <div className="flex items-center gap-2">
      <span className="text-xs text-zinc-300 w-20">{label}</span>
      <input
        type="text"
        className="input w-24 bg-transparent text-zinc-300 text-xs px-2 py-1 outline-none border border-transparent focus:border-blue-400"
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
    </div>
  );
}

export default function ParameterMetaPanel() {
  const panelRef = React.useRef<HTMLDivElement | null>(null);
  const panelPosition = parameterMetaPanelPosition.value;
  const item = activeItem.value!;
  const parameterName = parameterMetaPanelParameterName.value!;
  const parameter = item.parameters.find((p) => p.name === parameterName) as Parameter;

  function handleClose() {
    parameterMetaPanelVisible.value = false;
  }

  function handleChange(key: string, value: string) {
    setItemParameterProperty(item, parameterName, key, value);
  }

  React.useEffect(() => {
    if (panelRef.current) {
      const el = panelRef.current!;
      const r = el.getBoundingClientRect();
      const viewHeight = window.innerHeight;
      const offset = 10;
      el.style.position = "fixed";
      el.style.left = "200px";
      if (panelPosition.y + r.height > viewHeight - offset) {
        el.style.top = viewHeight - r.height - offset + "px";
      } else {
        el.style.top = panelPosition.y + "px";
      }
    }
  }, [panelPosition]);

  return (
    <div className="fixed z-50 top-0 left-0 w-screen h-screen" onClick={handleClose}>
      <div ref={panelRef} onClick={(e) => e.stopPropagation()} className="drop-shadow-lg">
        <div className="panel-header flex justify-between px-3 h-8 items-center bg-zinc-800 border-zinc-700 border-b">
          <span className="text-xs text-zinc-400">Parameter Settings</span>
          <Icon name="x" onClick={handleClose} />
        </div>
        <div className="panel-content bg-zinc-800 px-3 py-2 flex flex-col gap-2">
          <div className="flex items-center gap-2">
            <span className="text-xs text-zinc-300 w-20">Name</span>
            <span className="input w-24 bg-transparent text-zinc-600 text-xs px-2 py-1">{parameter.name}</span>
          </div>
          <TextWidget label="Label" value={parameter.label} onChange={(value) => handleChange("label", value)} />
          <div className="flex items-center gap-2">
            <span className="text-xs text-zinc-300 w-20">Type</span>
            <select
              className="bg-transparent text-zinc-300 text-xs px-2 py-1"
              value={parameter.type}
              onChange={(e) => handleChange("type", e.target.value)}
            >
              <option value="NUMBER">Number</option>
              <option value="STRING">String</option>
              <option value="BOOLEAN">Boolean</option>
              <option value="COLOR">Color</option>
              <option value="FILE">File</option>
            </select>
          </div>
        </div>
      </div>
    </div>
  );
}
