import { useCallback, useRef } from 'react';
import { useStore } from '../state/store';
import type { Port } from '../types/node';
import type { Value } from '../types/value';
import { isFloat, isInt, getFloat, getPoint, getString, getBoolean, getColor } from '../types/value';
import { DragValue, type DragValueHandle } from './DragValue';
import {
  LABEL_WIDTH,
  PARAMETER_ROW_HEIGHT,
} from '../theme/tokens';

const DRAG_THRESHOLD = 3;

function DraggableLabel({
  label,
  portName,
  onDrag,
  onClick,
}: {
  label: string;
  portName: string;
  onDrag: (delta: number) => void;
  onClick?: () => void;
}) {
  const dragOriginX = useRef(0);
  const lastX = useRef(0);
  const hasDragged = useRef(false);

  const handlePointerDown = useCallback((e: React.PointerEvent) => {
    e.preventDefault();
    dragOriginX.current = e.clientX;
    lastX.current = e.clientX;
    hasDragged.current = false;
    const target = e.currentTarget as HTMLElement;
    target.setPointerCapture(e.pointerId);
  }, []);

  const handlePointerMove = useCallback(
    (e: React.PointerEvent) => {
      if (!(e.currentTarget as HTMLElement).hasPointerCapture(e.pointerId)) return;
      if (!hasDragged.current && Math.abs(e.clientX - dragOriginX.current) < DRAG_THRESHOLD) return;
      hasDragged.current = true;

      const moveDelta = e.clientX - lastX.current;
      lastX.current = e.clientX;

      let speed = 1.0;
      if (e.shiftKey) speed *= 10;
      if (e.altKey) speed *= 0.01;

      onDrag(moveDelta * speed);
    },
    [onDrag],
  );

  const handlePointerUp = useCallback(
    (e: React.PointerEvent) => {
      (e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId);
      if (!hasDragged.current && onClick) {
        onClick();
      }
    },
    [onClick],
  );

  return (
    <div
      className="flex items-center justify-end px-2 shrink-0 text-zinc-300 text-[11px] cursor-ew-resize select-none"
      style={{ width: LABEL_WIDTH }}
      data-testid={`param-label-${portName}`}
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
    >
      {label}
    </div>
  );
}

function FloatPortWidget({
  port,
  nodeName,
}: {
  port: Port;
  nodeName: string;
}) {
  const setPortValue = useStore((s) => s.setPortValue);
  const numValue =
    isFloat(port.value) || isInt(port.value)
      ? getFloat(port.value)
      : 0;

  const accumulatedLabelValue = useRef(numValue);
  const dragRef = useRef<DragValueHandle>(null);

  const handleChange = useCallback(
    (v: number) => {
      const isIntType = port.port_type === 'Int';
      const newValue = isIntType ? Math.round(v) : v;
      setPortValue(nodeName, port.name,
        isIntType ? { Int: newValue } : { Float: newValue },
      );
    },
    [setPortValue, nodeName, port.name, port.port_type],
  );

  const handleLabelDrag = useCallback(
    (delta: number) => {
      accumulatedLabelValue.current += delta;
      handleChange(accumulatedLabelValue.current);
    },
    [handleChange],
  );

  const handleLabelClick = useCallback(() => {
    dragRef.current?.startEdit();
  }, []);

  const handleLabelPointerDownCapture = useCallback(() => {
    accumulatedLabelValue.current = numValue;
  }, [numValue]);

  return (
    <div
      className="flex"
      style={{ height: PARAMETER_ROW_HEIGHT }}
      data-testid={`param-row-${port.name}`}
      onPointerDownCapture={handleLabelPointerDownCapture}
    >
      <DraggableLabel
        label={port.label ?? port.name}
        portName={port.name}
        onDrag={handleLabelDrag}
        onClick={handleLabelClick}
      />
      <div
        className="flex-1 pr-2"
        data-testid={`param-value-${port.name}`}
      >
        <DragValue
          ref={dragRef}
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
  const pointValue = getPoint(port.value);

  const accumulatedLabelPoint = useRef(pointValue);
  const xRef = useRef<DragValueHandle>(null);

  const handleChangeX = useCallback(
    (v: number) => {
      setPortValue(nodeName, port.name, {
        Point: { x: v, y: pointValue.y },
      });
    },
    [setPortValue, nodeName, port.name, pointValue.y],
  );

  const handleChangeY = useCallback(
    (v: number) => {
      setPortValue(nodeName, port.name, {
        Point: { x: pointValue.x, y: v },
      });
    },
    [setPortValue, nodeName, port.name, pointValue.x],
  );

  const handleLabelCommitBoth = useCallback(
    (v: number) => {
      setPortValue(nodeName, port.name, {
        Point: { x: v, y: v },
      });
    },
    [setPortValue, nodeName, port.name],
  );

  const handleLabelClick = useCallback(() => {
    xRef.current?.startEdit();
  }, []);

  const handleLabelDrag = useCallback(
    (delta: number) => {
      accumulatedLabelPoint.current = {
        x: accumulatedLabelPoint.current.x + delta,
        y: accumulatedLabelPoint.current.y + delta,
      };
      setPortValue(nodeName, port.name, {
        Point: accumulatedLabelPoint.current,
      });
    },
    [setPortValue, nodeName, port.name],
  );

  const handleLabelPointerDownCapture = useCallback(() => {
    accumulatedLabelPoint.current = pointValue;
  }, [pointValue]);

  return (
    <div
      className="flex"
      style={{ height: PARAMETER_ROW_HEIGHT }}
      data-testid={`param-row-${port.name}`}
      onPointerDownCapture={handleLabelPointerDownCapture}
    >
      <DraggableLabel
        label={port.label ?? port.name}
        portName={port.name}
        onDrag={handleLabelDrag}
        onClick={handleLabelClick}
      />
      <div className="flex flex-1 gap-4 pr-2">
        <div className="flex-1" data-testid={`param-value-${port.name}-x`}>
          <DragValue ref={xRef} value={pointValue.x} onChange={handleChangeX} onLabelCommit={handleLabelCommitBoth} />
        </div>
        <div className="flex-1" data-testid={`param-value-${port.name}-y`}>
          <DragValue value={pointValue.y} onChange={handleChangeY} />
        </div>
      </div>
    </div>
  );
}

function StringPortWidget({
  port,
  nodeName,
}: {
  port: Port;
  nodeName: string;
}) {
  const setPortValue = useStore((s) => s.setPortValue);
  const strValue = getString(port.value);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      setPortValue(nodeName, port.name, { String: e.target.value });
    },
    [setPortValue, nodeName, port.name],
  );

  const handleLabelClick = useCallback(() => {
    inputRef.current?.focus();
  }, []);

  return (
    <div
      className="flex"
      style={{ height: PARAMETER_ROW_HEIGHT }}
      data-testid={`param-row-${port.name}`}
    >
      <div
        className="flex items-center justify-end px-2 shrink-0 text-zinc-300 text-[11px] cursor-default select-none"
        style={{ width: LABEL_WIDTH }}
        onClick={handleLabelClick}
      >
        {port.label ?? port.name}
      </div>
      <div className="flex-1 flex items-center pr-2">
        <input
          ref={inputRef}
          type="text"
          value={strValue}
          onChange={handleChange}
          data-testid={`param-value-${port.name}`}
          className="w-full h-7 bg-transparent hover:bg-field-hover text-zinc-100 border-none outline-none text-[13px] px-2 rounded-sm font-[inherit] focus:bg-field-hover"
        />
      </div>
    </div>
  );
}

function ColorPortWidget({
  port,
  nodeName,
}: {
  port: Port;
  nodeName: string;
}) {
  const setPortValue = useStore((s) => s.setPortValue);
  const colorValue = getColor(port.value);

  const toHex = (c: { r: number; g: number; b: number }) => {
    const r = Math.round(c.r * 255).toString(16).padStart(2, '0');
    const g = Math.round(c.g * 255).toString(16).padStart(2, '0');
    const b = Math.round(c.b * 255).toString(16).padStart(2, '0');
    return `#${r}${g}${b}`;
  };

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLInputElement>) => {
      const hex = e.target.value;
      const r = parseInt(hex.slice(1, 3), 16) / 255;
      const g = parseInt(hex.slice(3, 5), 16) / 255;
      const b = parseInt(hex.slice(5, 7), 16) / 255;
      setPortValue(nodeName, port.name, {
        Color: { r, g, b, a: colorValue.a },
      });
    },
    [setPortValue, nodeName, port.name, colorValue.a],
  );

  return (
    <div
      className="flex"
      style={{ height: PARAMETER_ROW_HEIGHT }}
      data-testid={`param-row-${port.name}`}
    >
      <div
        className="flex items-center justify-end px-2 shrink-0 text-zinc-300 text-[11px]"
        style={{ width: LABEL_WIDTH }}
      >
        {port.label ?? port.name}
      </div>
      <div className="flex-1 flex items-center gap-2 px-2">
        <input
          type="color"
          value={toHex(colorValue)}
          onChange={handleChange}
          className="w-7 h-7 p-0 border-none cursor-pointer bg-transparent"
          data-testid={`param-color-${port.name}`}
        />
        <span className="text-zinc-100 text-[13px]">{toHex(colorValue)}</span>
      </div>
    </div>
  );
}

function TogglePortWidget({
  port,
  nodeName,
}: {
  port: Port;
  nodeName: string;
}) {
  const setPortValue = useStore((s) => s.setPortValue);
  const boolValue = getBoolean(port.value);

  const handleClick = useCallback(() => {
    setPortValue(nodeName, port.name, { Boolean: !boolValue });
  }, [setPortValue, nodeName, port.name, boolValue]);

  return (
    <div
      className="flex"
      style={{ height: PARAMETER_ROW_HEIGHT }}
      data-testid={`param-row-${port.name}`}
    >
      <div
        className="flex items-center justify-end px-2 shrink-0 text-zinc-300 text-[11px]"
        style={{ width: LABEL_WIDTH }}
      >
        {port.label ?? port.name}
      </div>
      <div
        className="flex-1 flex items-center px-2 text-zinc-100 text-[13px] cursor-pointer select-none"
        data-testid={`param-value-${port.name}`}
        onClick={handleClick}
      >
        {boolValue ? 'true' : 'false'}
      </div>
    </div>
  );
}

function MenuPortWidget({
  port,
  nodeName,
}: {
  port: Port;
  nodeName: string;
}) {
  const setPortValue = useStore((s) => s.setPortValue);
  const strValue = getString(port.value);

  const handleChange = useCallback(
    (e: React.ChangeEvent<HTMLSelectElement>) => {
      setPortValue(nodeName, port.name, { String: e.target.value });
    },
    [setPortValue, nodeName, port.name],
  );

  return (
    <div
      className="flex"
      style={{ height: PARAMETER_ROW_HEIGHT }}
      data-testid={`param-row-${port.name}`}
    >
      <div
        className="flex items-center justify-end px-2 shrink-0 text-zinc-300 text-[11px]"
        style={{ width: LABEL_WIDTH }}
      >
        {port.label ?? port.name}
      </div>
      <div className="flex-1 flex items-center pr-2">
        <select
          value={strValue}
          onChange={handleChange}
          data-testid={`param-value-${port.name}`}
          className="w-full h-7 bg-transparent hover:bg-field-hover text-zinc-100 border-none outline-none text-[13px] px-1 rounded-sm font-[inherit] focus:bg-field-hover cursor-pointer"
        >
          {port.menu_items.map((item) => (
            <option key={item.key} value={item.key}>{item.label}</option>
          ))}
        </select>
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
        className="flex items-center justify-end px-2 shrink-0 text-zinc-300 text-[11px]"
        style={{ width: LABEL_WIDTH }}
      >
        {port.label ?? port.name}
      </div>
      <div
        className="flex items-center px-2 flex-1 text-zinc-100 text-[13px]"
        data-testid={`param-value-${port.name}`}
      >
        {displayValue}
      </div>
    </div>
  );
}

function ConnectedPortWidget({ port }: { port: Port }) {
  return (
    <div
      className="flex"
      style={{ height: PARAMETER_ROW_HEIGHT }}
      data-testid={`param-row-${port.name}`}
    >
      <div
        className="flex items-center justify-end px-2 shrink-0 text-zinc-300 text-[11px]"
        style={{ width: LABEL_WIDTH }}
      >
        {port.label ?? port.name}
      </div>
      <div
        className="flex items-center px-2 flex-1 text-zinc-500 italic text-[13px]"
        data-testid={`param-value-${port.name}`}
      >
        connected
      </div>
    </div>
  );
}

function PortWidget({ port, nodeName, isConnected }: { port: Port; nodeName: string; isConnected: boolean }) {
  if (isConnected) {
    return <ConnectedPortWidget port={port} />;
  }

  // Dispatch by widget type first (handles Menu, Toggle, etc.)
  switch (port.widget) {
    case 'Menu':
      return <MenuPortWidget port={port} nodeName={nodeName} />;
    case 'Toggle':
      return <TogglePortWidget port={port} nodeName={nodeName} />;
  }

  // Fall back to port_type for standard widgets
  if (port.port_type === 'Float' || port.port_type === 'Int') {
    return <FloatPortWidget port={port} nodeName={nodeName} />;
  }
  if (port.port_type === 'Point') {
    return <PointPortWidget port={port} nodeName={nodeName} />;
  }
  if (port.port_type === 'String') {
    return <StringPortWidget port={port} nodeName={nodeName} />;
  }
  if (port.port_type === 'Color') {
    return <ColorPortWidget port={port} nodeName={nodeName} />;
  }
  return <GenericPortWidget port={port} />;
}

function DocumentPropertyRow({ label, value }: { label: string; value: string }) {
  return (
    <div
      className="flex"
      style={{ height: PARAMETER_ROW_HEIGHT }}
    >
      <div
        className="flex items-center justify-end px-2 shrink-0 text-zinc-300 text-[11px]"
        style={{ width: LABEL_WIDTH }}
      >
        {label}
      </div>
      <div
        className="flex items-center px-2 flex-1 text-zinc-100 text-[13px]"
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
        className="flex items-center justify-end px-2 shrink-0 text-zinc-300 text-[11px]"
        style={{ width: LABEL_WIDTH }}
      >
        {label}
      </div>
      <div
        className="flex items-center px-2 flex-1"
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

function formatValue(v: Value): string {
  if (v === 'Null') return '';
  if (typeof v === 'object') {
    if ('Int' in v) return String(v.Int);
    if ('Float' in v) return String(v.Float);
    if ('String' in v) return v.String;
    if ('Boolean' in v) return String(v.Boolean);
    if ('Point' in v) return `${v.Point.x}, ${v.Point.y}`;
    if ('Color' in v) return `rgba(${Math.round(v.Color.r * 255)}, ${Math.round(v.Color.g * 255)}, ${Math.round(v.Color.b * 255)}, ${v.Color.a.toFixed(2)})`;
    if ('Path' in v) return '[Path]';
    if ('List' in v) return `[${v.List.length} items]`;
    if ('Map' in v) return `[${Object.keys(v.Map).length} keys]`;
  }
  return '';
}

export function ParameterPanel() {
  const activeNode = useStore((s) => s.activeNode);
  const children = useStore((s) => s.library.root.children);
  const connections = useStore((s) => s.library.root.connections);
  const library = useStore((s) => s.library);
  const node = activeNode ? children.find((n) => n.name === activeNode) : null;

  const docWidth = library.properties.canvasWidth ?? '1000';
  const docHeight = library.properties.canvasHeight ?? '1000';
  const bgColor = library.properties.canvasBackground ?? '#e4e4e7';

  return (
    <div className="flex flex-col h-full bg-zinc-800">
      {/* Port list / Document properties */}
      <div className="flex-1 overflow-y-auto relative">
        {/* Two-tone background columns */}
        <div
          data-testid="param-bg-left"
          className="absolute top-0 left-0 bottom-0 bg-port-label"
          style={{ width: LABEL_WIDTH }}
        />
        <div
          data-testid="param-bg-right"
          className="absolute top-0 right-0 bottom-0 bg-port-value"
          style={{ left: LABEL_WIDTH }}
        />
        {/* Content on top */}
        <div className="relative z-[1]">
          {node ? (
            <>
              {node.inputs.map((port) => (
                <PortWidget
                  key={port.name}
                  port={port}
                  nodeName={node.name}
                  isConnected={connections.some(
                    (c) => c.input_node === node.name && c.input_port === port.name,
                  )}
                />
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
    </div>
  );
}
