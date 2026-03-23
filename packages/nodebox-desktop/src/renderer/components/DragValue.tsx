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
  const inputRef = useRef<HTMLInputElement>(null);
  const dragging = useRef(false);
  const didDrag = useRef(false);
  const lastX = useRef(0);
  const accumulator = useRef(0);

  const clamp = (v: number) => {
    let result = v;
    if (min !== null) result = Math.max(min, result);
    if (max !== null) result = Math.min(max, result);
    return result;
  };

  const openEditor = useCallback(() => {
    setEditing(true);
    setEditText(String(step >= 1 ? Math.round(value) : parseFloat(value.toFixed(precision))));
    // Select all text after React renders the input
    requestAnimationFrame(() => inputRef.current?.select());
  }, [value, step, precision]);

  const handlePointerDown = useCallback((e: React.PointerEvent) => {
    if (editing) return;
    dragging.current = true;
    didDrag.current = false;
    lastX.current = e.clientX;
    accumulator.current = 0;
    (e.target as HTMLElement).setPointerCapture(e.pointerId);
    e.preventDefault();
  }, [editing]);

  // Incremental accumulator: modifier only affects the delta since last move,
  // so pressing/releasing Shift or Alt mid-drag doesn't cause jumps.
  const handlePointerMove = useCallback((e: React.PointerEvent) => {
    if (!dragging.current) return;
    const dx = e.clientX - lastX.current;
    lastX.current = e.clientX;
    if (Math.abs(dx) > 0) didDrag.current = true;

    const modifier = e.shiftKey ? 10 : e.altKey ? 0.1 : 1;
    accumulator.current += dx * step * modifier;

    const newValue = clamp(value + accumulator.current);
    const rounded = step >= 1 ? Math.round(newValue) : parseFloat(newValue.toFixed(precision));
    onChange(rounded);
  }, [value, onChange, step, min, max, precision]);

  const handlePointerUp = useCallback(() => {
    if (!dragging.current) return;
    dragging.current = false;
    if (didDrag.current) {
      // Was a real drag → commit
      onCommit?.();
    } else {
      // Was a click (no movement) → open editor
      openEditor();
    }
  }, [onCommit, openEditor]);

  const commitEdit = useCallback(() => {
    const v = parseFloat(editText);
    if (!isNaN(v)) onChange(clamp(v));
    setEditing(false);
    onCommit?.();
  }, [editText, onChange, onCommit, min, max]);

  if (editing) {
    return (
      <input
        ref={inputRef}
        type="text"
        value={editText}
        onChange={(e) => setEditText(e.target.value)}
        onBlur={commitEdit}
        onKeyDown={(e) => { if (e.key === 'Enter') commitEdit(); if (e.key === 'Escape') setEditing(false); }}
        autoFocus
        style={{ width: '100%', background: '#52525c', color: '#fafafa', border: 'none', fontSize: 13, padding: '2px 8px', outline: 'none', fontFamily: 'inherit' }}
      />
    );
  }

  const displayValue = step >= 1 ? String(Math.round(value)) : value.toFixed(precision);

  return (
    <div
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      style={{ background: 'transparent', color: '#f4f4f5', fontSize: 13, padding: '4px 8px', cursor: 'ew-resize', userSelect: 'none', flex: 1, textAlign: 'left' }}
    >
      {displayValue}
    </div>
  );
}
