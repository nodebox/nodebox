import { WidgetProps, WidgetExpressionButton } from "./widget-utils";
import InlineEdit from "../components/inline-edit";

export default function ExpressionWidget({
  label,
  value,
  onChange,
  onToggleExpression,
  onPublishParameter,
}: WidgetProps) {
  return (
    <div className="flex items-stretch px-1 h-8 hover:bg-zinc-700 group relative">
      <div className="w-28 px-1 text-xs leading-8 text-zinc-400 text-right">{label}</div>
      <InlineEdit
        value={value as string}
        onChange={(s) => onChange({ type: "EXPRESSION", expression: s })}
        className="flex-1 text-green-400 font-semibold"
      />
      <WidgetExpressionButton onToggleExpression={onToggleExpression} onPublishParameter={onPublishParameter} />
    </div>
  );
}
