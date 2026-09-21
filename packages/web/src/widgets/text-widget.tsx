import { WidgetProps, createParameterValueFromLiteral } from "./widget-utils";

export default function TextWidget({ label, value, onChange }: WidgetProps) {
  return (
    <div className="flex flex-col items-stretch  py-2 group relative">
      <div className="flex flex-col mb-2">
        <div className="w-28 px-1 text-xs leading-8 text-zinc-400 text-right">{label}</div>
        <textarea
          value={value as string}
          onChange={(e) => onChange(createParameterValueFromLiteral(e.target.value))}
          className="p-2 mt-1 bg-zinc-950 text-zinc-200 outline-none h-48 font-mono text-xs resize-none"
        />
      </div>
    </div>
  );
}
