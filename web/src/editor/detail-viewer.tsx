import { ObjectInspector, chromeDark } from "react-inspector";
import Icon from "../components/icon";

interface DetailViewerProps {
  data: unknown;
  viewRaw: boolean;
  style?: React.CSSProperties;
}

const nodeboxTheme = {
  ...chromeDark,
  BASE_BACKGROUND_COLOR: "transparent",
};

export default function DetailViewer({ data, viewRaw = false, style = {} }: DetailViewerProps) {
  function handleCopy() {
    navigator.clipboard.writeText(JSON.stringify(data, null, 2));
  }

  return (
    <div className="flex-1 overflow-auto p-2 relative" style={style}>
      <Icon
        name="copy"
        className="fixed cursor-pointer fill-zinc-500"
        style={{ top: 96, right: 16 }}
        onClick={handleCopy}
      />
      {!viewRaw && <ObjectInspector data={data} expandLevel={1} theme={nodeboxTheme as unknown as string} />}
      {viewRaw && <pre className="font-mono text-xs">{JSON.stringify(data, null, 2)}</pre>}
    </div>
  );
}
