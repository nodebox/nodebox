import { Choice } from "@ndbx/runtime";
import { WidgetProps, createParameterValueFromLiteral } from "./widget-utils";

interface ChoiceWidgetProps extends WidgetProps {
  choices?: Choice[];
}

export default function ChoiceWidget({ label, value, onChange, choices = [] }: ChoiceWidgetProps) {
  return (
    <div className="flex items-stretch hover:bg-zinc-700 px-1 h-8 group relative">
      <div className="w-24 px-1 text-xs leading-8 text-zinc-500 text-right select-none">{label}</div>
      <select
        value={value as string}
        onChange={(e) => onChange(createParameterValueFromLiteral(e.target.value))}
        className="menu-widget flex-1 text-xs bg-transparent px-2 overflow-hidden border border-transparent outline-none rounded-sm"
      >
        <option key={-2} value="test1">
          test1
        </option>
        <option key={-1} value="test2">
          test2
        </option>
        {choices.map((option, index) => (
          <option key={index} value={option.name}>
            {option.label}
          </option>
        ))}
      </select>
    </div>
  );
}
