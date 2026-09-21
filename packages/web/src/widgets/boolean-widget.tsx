import { WidgetProps, WidgetMetaButton, WidgetRemoveButton, createParameterValueFromLiteral } from "./widget-utils";

export default function BooleanWidget({ label, value, onChange, onRemove, onMeta }: WidgetProps) {
  return (
    <div className="flex items-stretch px-1 h-8 hover:bg-zinc-700 group relative">
      <WidgetMetaButton onMeta={onMeta} />
      <div className="w-28 px-1 text-xs leading-8 text-zinc-400 text-right">{label}</div>
      <div className="flex-1 flex items-center ml-2">
        <input
          className="boolean-widget"
          type="checkbox"
          checked={value as boolean}
          onChange={(e) => onChange(createParameterValueFromLiteral(e.target.checked))}
        />
      </div>
      <WidgetRemoveButton onRemove={onRemove} />
    </div>
  );
}
