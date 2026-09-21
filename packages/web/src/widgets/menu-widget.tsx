import React from "react";
import { WidgetProps, WidgetMetaButton, WidgetRemoveButton, createParameterValueFromLiteral } from "./widget-utils";
import { Choice } from "@ndbx/runtime";

interface MenuWidgetProps extends WidgetProps {
  choices: Choice[];
}

export default function MenuWidget({ label, value, choices, onChange, onRemove, onMeta }: MenuWidgetProps) {
  function handleChange(e: React.ChangeEvent<HTMLSelectElement>) {
    onChange(createParameterValueFromLiteral(e.target.value));
  }

  return (
    <div className="flex px-1 h-8 hover:bg-zinc-700 group relative items-baseline">
      <WidgetMetaButton onMeta={onMeta} />
      <WidgetRemoveButton onRemove={onRemove} />

      <div className="flex-none w-28 px-1 text-xs leading-8 text-zinc-400 text-right">{label}</div>
      <select
        value={value as string}
        onChange={handleChange}
        className="menu-widget flex-1 h-7 text-xs px-2 border border-transparent outline-none rounded bg-transparent"
      >
        {choices.map((choice) => (
          <option key={choice.name} value={choice.name}>
            {choice.label}
          </option>
        ))}
      </select>
    </div>
  );
}
