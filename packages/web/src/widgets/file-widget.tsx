import { WidgetProps, WidgetMetaButton, WidgetRemoveButton } from "./widget-utils";
import { activeItem, activeNetworkItemId, assetParameterPath, assetsModalVisible } from "../editor/signals";

export default function FileWidget({ name, label, value, onRemove, onMeta }: WidgetProps) {
  function handleClick() {
    assetsModalVisible.value = true;
    const network = activeItem.value;
    if (!network) {
      return;
    }
    const networkId = network.id;
    const nodeId = activeNetworkItemId.value;
    assetParameterPath.value = `${networkId}/${nodeId}/${name}`;
  }

  return (
    <div className="flex items-stretch px-1 h-8 hover:bg-zinc-700 group relative">
      <WidgetMetaButton onMeta={onMeta} />
      <div className="w-28 px-1 text-xs leading-8 text-zinc-400 text-right">{label}</div>
      <div
        className="flex-1 flex items-center my-1 rounded px-1 hover:bg-zinc-700 cursor-pointer overflow-hidden whitespace-nowrap"
        onClick={handleClick}
      >
        <span className="overflow-hidden text-ellipsis text-xs">{(value as string) || "Choose File"}</span>
      </div>
      <WidgetRemoveButton onRemove={onRemove} />
    </div>
  );
}
