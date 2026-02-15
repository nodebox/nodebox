import { useStore } from '../state/store';
import type { Port } from '../types/node';
import type { Value } from '../types/value';
import {
  PANEL_BG,
  FONT_SIZE_SMALL,
  FONT_SIZE_BASE,
  PORT_LABEL_BACKGROUND,
  PORT_VALUE_BACKGROUND,
  LABEL_WIDTH,
  PARAMETER_ROW_HEIGHT,
  TEXT_SUBDUED,
  VALUE_TEXT,
} from '../theme/tokens';

function PortWidget({ port }: { port: Port }) {
  const displayValue = formatValue(port.value);

  return (
    <div
      className="flex"
      style={{ height: PARAMETER_ROW_HEIGHT }}
    >
      {/* Label */}
      <div
        className="flex items-center px-2 shrink-0"
        style={{
          width: LABEL_WIDTH,
          background: PORT_LABEL_BACKGROUND,
          color: TEXT_SUBDUED,
          fontSize: FONT_SIZE_SMALL,
        }}
      >
        {port.label ?? port.name}
      </div>
      {/* Value */}
      <div
        className="flex items-center px-2 flex-1"
        style={{
          background: PORT_VALUE_BACKGROUND,
          color: VALUE_TEXT,
          fontSize: FONT_SIZE_BASE,
        }}
      >
        {displayValue}
      </div>
    </div>
  );
}

function DocumentPropertyRow({ label, value }: { label: string; value: string }) {
  return (
    <div
      className="flex"
      style={{ height: PARAMETER_ROW_HEIGHT }}
    >
      <div
        className="flex items-center px-2 shrink-0"
        style={{
          width: LABEL_WIDTH,
          background: PORT_LABEL_BACKGROUND,
          color: TEXT_SUBDUED,
          fontSize: FONT_SIZE_SMALL,
        }}
      >
        {label}
      </div>
      <div
        className="flex items-center px-2 flex-1"
        style={{
          background: PORT_VALUE_BACKGROUND,
          color: VALUE_TEXT,
          fontSize: FONT_SIZE_BASE,
        }}
      >
        {value}
      </div>
    </div>
  );
}

function ColorSwatchRow({ label, color }: { label: string; color: string }) {
  return (
    <div
      className="flex"
      style={{ height: PARAMETER_ROW_HEIGHT }}
    >
      <div
        className="flex items-center px-2 shrink-0"
        style={{
          width: LABEL_WIDTH,
          background: PORT_LABEL_BACKGROUND,
          color: TEXT_SUBDUED,
          fontSize: FONT_SIZE_SMALL,
        }}
      >
        {label}
      </div>
      <div
        className="flex items-center px-2 flex-1"
        style={{
          background: PORT_VALUE_BACKGROUND,
        }}
      >
        <div
          style={{
            width: 16,
            height: 16,
            background: color,
            border: '1px solid rgba(255,255,255,0.2)',
          }}
        />
      </div>
    </div>
  );
}

function formatValue(value: Value): string {
  switch (value.type) {
    case 'null':
      return '';
    case 'int':
    case 'float':
      return String(value.value);
    case 'string':
      return value.value;
    case 'boolean':
      return value.value ? 'true' : 'false';
    case 'point':
      return `${value.value.x}, ${value.value.y}`;
    case 'color':
      return `rgba(${Math.round(value.value.r * 255)}, ${Math.round(value.value.g * 255)}, ${Math.round(value.value.b * 255)}, ${value.value.a.toFixed(2)})`;
    case 'path':
      return '[Path]';
    case 'list':
      return `[${value.value.length} items]`;
    case 'map':
      return `[${Object.keys(value.value).length} keys]`;
  }
}

export function ParameterPanel() {
  const activeNode = useStore((s) => s.activeNode);
  const children = useStore((s) => s.library.root.children);
  const library = useStore((s) => s.library);
  const node = activeNode ? children.find((n) => n.name === activeNode) : null;

  const docWidth = library.properties.canvasWidth ?? '1000';
  const docHeight = library.properties.canvasHeight ?? '1000';
  const bgColor = library.properties.canvasBackground ?? '#e4e4e7';

  return (
    <div className="flex flex-col h-full" style={{ background: PANEL_BG }}>
      {/* Port list / Document properties */}
      <div className="flex-1 overflow-y-auto">
        {node ? (
          node.inputs.map((port) => (
            <PortWidget key={port.name} port={port} />
          ))
        ) : (
          <>
            <DocumentPropertyRow label="width" value={parseFloat(docWidth).toFixed(2)} />
            <DocumentPropertyRow label="height" value={parseFloat(docHeight).toFixed(2)} />
            <ColorSwatchRow label="background" color={bgColor} />
          </>
        )}
      </div>
    </div>
  );
}
