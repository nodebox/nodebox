import { useCallback } from 'react';
import { useStore } from '../state/store';
import type { Port } from '../types/node';
import type { Value } from '../types/value';
import { DragValue } from './DragValue';
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

function FloatPortWidget({
  port,
  nodeName,
}: {
  port: Port;
  nodeName: string;
}) {
  const setPortValue = useStore((s) => s.setPortValue);
  const numValue =
    port.value.type === 'float' || port.value.type === 'int'
      ? port.value.value
      : 0;

  const handleChange = useCallback(
    (v: number) => {
      const valueType = port.portType === 'int' ? 'int' : 'float';
      const newValue = valueType === 'int' ? Math.round(v) : v;
      setPortValue(nodeName, port.name, {
        type: valueType,
        value: newValue,
      } as Value);
    },
    [setPortValue, nodeName, port.name, port.portType],
  );

  return (
    <div
      className="flex"
      style={{ height: PARAMETER_ROW_HEIGHT }}
      data-testid={`param-row-${port.name}`}
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
        {port.label ?? port.name}
      </div>
      <div
        className="flex-1"
        style={{ background: PORT_VALUE_BACKGROUND }}
        data-testid={`param-value-${port.name}`}
      >
        <DragValue
          value={numValue}
          onChange={handleChange}
          min={port.min ?? undefined}
          max={port.max ?? undefined}
        />
      </div>
    </div>
  );
}

function PointPortWidget({
  port,
  nodeName,
}: {
  port: Port;
  nodeName: string;
}) {
  const setPortValue = useStore((s) => s.setPortValue);
  const pointValue =
    port.value.type === 'point'
      ? port.value.value
      : { x: 0, y: 0 };

  const handleChangeX = useCallback(
    (v: number) => {
      setPortValue(nodeName, port.name, {
        type: 'point',
        value: { x: v, y: pointValue.y },
      });
    },
    [setPortValue, nodeName, port.name, pointValue.y],
  );

  const handleChangeY = useCallback(
    (v: number) => {
      setPortValue(nodeName, port.name, {
        type: 'point',
        value: { x: pointValue.x, y: v },
      });
    },
    [setPortValue, nodeName, port.name, pointValue.x],
  );

  return (
    <div
      className="flex"
      style={{ height: PARAMETER_ROW_HEIGHT }}
      data-testid={`param-row-${port.name}`}
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
        {port.label ?? port.name}
      </div>
      <div
        className="flex flex-1"
        style={{ background: PORT_VALUE_BACKGROUND }}
      >
        <div className="flex-1" data-testid={`param-value-${port.name}-x`}>
          <DragValue value={pointValue.x} onChange={handleChangeX} />
        </div>
        <div className="flex-1" data-testid={`param-value-${port.name}-y`}>
          <DragValue value={pointValue.y} onChange={handleChangeY} />
        </div>
      </div>
    </div>
  );
}

function GenericPortWidget({ port }: { port: Port }) {
  const displayValue = formatValue(port.value);

  return (
    <div
      className="flex"
      style={{ height: PARAMETER_ROW_HEIGHT }}
      data-testid={`param-row-${port.name}`}
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
        {port.label ?? port.name}
      </div>
      <div
        className="flex items-center px-2 flex-1"
        style={{
          background: PORT_VALUE_BACKGROUND,
          color: VALUE_TEXT,
          fontSize: FONT_SIZE_BASE,
        }}
        data-testid={`param-value-${port.name}`}
      >
        {displayValue}
      </div>
    </div>
  );
}

function PortWidget({ port, nodeName }: { port: Port; nodeName: string }) {
  if (port.portType === 'float' || port.portType === 'int') {
    return <FloatPortWidget port={port} nodeName={nodeName} />;
  }
  if (port.portType === 'point') {
    return <PointPortWidget port={port} nodeName={nodeName} />;
  }
  return <GenericPortWidget port={port} />;
}

function NodeHeader({
  nodeName,
  prototype,
}: {
  nodeName: string;
  prototype: string | null;
}) {
  return (
    <div
      className="flex items-center justify-between px-2 shrink-0"
      style={{
        height: PARAMETER_ROW_HEIGHT,
        background: PORT_LABEL_BACKGROUND,
        fontSize: FONT_SIZE_SMALL,
      }}
      data-testid="param-header"
    >
      <span style={{ color: TEXT_SUBDUED }}>{nodeName}</span>
      {prototype && (
        <span style={{ color: TEXT_SUBDUED, opacity: 0.6 }}>{prototype}</span>
      )}
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
          <>
            <NodeHeader
              nodeName={node.name}
              prototype={node.prototype}
            />
            {node.inputs.map((port) => (
              <PortWidget key={port.name} port={port} nodeName={node.name} />
            ))}
          </>
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
