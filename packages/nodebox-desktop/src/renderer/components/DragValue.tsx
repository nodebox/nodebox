import React, { useCallback, useRef, useState } from 'react';

interface DragValueProps {
  value: number;
  onChange: (value: number) => void;
  onCommit?: () => void;
  step?: number;
  min?: number | null;
  max?: number | null;
  precision?: number;
}

export function DragValue({ value, onChange, onCommit, step = 1, min = null, max = null, precision = 2 }: DragValueProps) {
  const [editing, setEditing] = useState(false);
  const [editText, setEditText] = useState('');
  const dragging = useRef(false);
  const startX = useRef(0);
  const startValue = useRef(0);

  const clamp = (v: number) => {
    let result = v;
    if (min !== null) result = Math.max(min, result);
    if (max !== null) result = Math.min(max, result);
    return result;
  };

  const handlePointerDown = useCallback((e: React.PointerEvent) => {
    if (editing) return;
    dragging.current = true;
    startX.current = e.clientX;
    startValue.current = value;
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
    e.preventDefault();
  }, [value, editing]);

  const handlePointerMove = useCallback((e: React.PointerEvent) => {
    if (!dragging.current) return;
    const dx = e.clientX - startX.current;
    const newValue = clamp(startValue.current + dx * step);
    onChange(step >= 1 ? Math.round(newValue) : parseFloat(newValue.toFixed(precision)));
  }, [onChange, step, min, max, precision]);

  const handlePointerUp = useCallback(() => {
    if (dragging.current) { dragging.current = false; onCommit?.(); }
  }, [onCommit]);

  const handleDoubleClick = useCallback(() => {
    setEditing(true);
    setEditText(String(step >= 1 ? Math.round(value) : parseFloat(value.toFixed(precision))));
  }, [value, step, precision]);

  const commitEdit = useCallback(() => {
    const v = parseFloat(editText);
    if (!isNaN(v)) onChange(clamp(v));
    setEditing(false);
    onCommit?.();
  }, [editText, onChange, onCommit, min, max]);

  if (editing) {
    return (
      <input
        type="text"
        value={editText}
        onChange={(e) => setEditText(e.target.value)}
        onBlur={commitEdit}
        onKeyDown={(e) => { if (e.key === 'Enter') commitEdit(); if (e.key === 'Escape') setEditing(false); }}
        autoFocus
        className="w-full bg-zinc-600 text-zinc-50 border-none text-[13px] px-1.5 py-0.5 outline-none font-[inherit]"
      />
    );
  }

  const displayValue = step >= 1 ? String(Math.round(value)) : value.toFixed(precision);

  return (
    <div
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onDoubleClick={handleDoubleClick}
      className="bg-transparent hover:bg-field-hover text-zinc-100 text-[13px] px-2 py-1 cursor-ew-resize select-none flex-1 text-left"
    >
      {displayValue}
    </div>
  );
}
