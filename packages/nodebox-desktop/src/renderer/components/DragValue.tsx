import React, { useCallback, useRef, useState } from 'react';
import { TEXT_DEFAULT, TEXT_STRONG, ZINC_600, ZINC_700, FIELD_HOVER_BG, FONT_SIZE_BASE } from '../theme/tokens';

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
  const [hovered, setHovered] = useState(false);
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
    if (dragging.current) {
      dragging.current = false;
      onCommit?.();
    }
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
        style={{
          width: '100%',
          background: ZINC_600,
          color: TEXT_STRONG,
          border: 'none',
          fontSize: FONT_SIZE_BASE,
          padding: '2px 6px',
          outline: 'none',
          fontFamily: 'inherit',
        }}
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
      onPointerEnter={() => setHovered(true)}
      onPointerLeave={() => setHovered(false)}
      style={{
        background: hovered ? FIELD_HOVER_BG : ZINC_700,
        color: TEXT_DEFAULT,
        fontSize: FONT_SIZE_BASE,
        padding: '2px 6px',
        cursor: 'ew-resize',
        userSelect: 'none',
        minWidth: 60,
        textAlign: 'right',
      }}
    >
      {displayValue}
    </div>
  );
}
