import { useState, useRef, useCallback, useEffect } from 'react';
import {
  VALUE_TEXT,
  VALUE_TEXT_HOVER,
  FIELD_HOVER_BG,
  CORNER_RADIUS_SMALL,
  FONT_SIZE_BASE,
  PORT_VALUE_BACKGROUND,
} from '../theme/tokens';

interface DragValueProps {
  value: number;
  onChange: (value: number) => void;
  min?: number;
  max?: number;
  speed?: number;
  format?: (v: number) => string;
}

const DRAG_THRESHOLD = 3;

export function DragValue({
  value,
  onChange,
  min,
  max,
  speed = 1.0,
  format,
}: DragValueProps) {
  const [mode, setMode] = useState<'display' | 'drag' | 'edit'>('display');
  const [hovered, setHovered] = useState(false);
  const [editText, setEditText] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);
  const dragStartX = useRef(0);
  const dragStartValue = useRef(0);
  const hasDragged = useRef(false);

  const clamp = useCallback(
    (v: number) => {
      let result = v;
      if (min !== undefined) result = Math.max(min, result);
      if (max !== undefined) result = Math.min(max, result);
      return result;
    },
    [min, max],
  );

  const displayText = format ? format(value) : value.toFixed(2);

  const handlePointerDown = useCallback(
    (e: React.PointerEvent) => {
      if (mode === 'edit') return;
      e.preventDefault();
      dragStartX.current = e.clientX;
      dragStartValue.current = value;
      hasDragged.current = false;

      const target = e.currentTarget as HTMLElement;
      target.setPointerCapture(e.pointerId);
    },
    [mode, value],
  );

  const handlePointerMove = useCallback(
    (e: React.PointerEvent) => {
      if (mode === 'edit') return;
      if (!(e.currentTarget as HTMLElement).hasPointerCapture(e.pointerId)) return;

      const deltaX = e.clientX - dragStartX.current;
      if (!hasDragged.current && Math.abs(deltaX) < DRAG_THRESHOLD) return;

      hasDragged.current = true;
      setMode('drag');

      let effectiveSpeed = speed;
      if (e.shiftKey) effectiveSpeed *= 10;
      if (e.altKey) effectiveSpeed *= 0.01;

      const newValue = clamp(dragStartValue.current + deltaX * effectiveSpeed);
      onChange(newValue);
    },
    [mode, speed, clamp, onChange],
  );

  const handlePointerUp = useCallback(
    (e: React.PointerEvent) => {
      if (mode === 'edit') return;
      (e.currentTarget as HTMLElement).releasePointerCapture(e.pointerId);

      if (!hasDragged.current) {
        // Click without drag -> enter edit mode
        setEditText(String(value));
        setMode('edit');
      } else {
        setMode('display');
      }
    },
    [mode, value],
  );

  useEffect(() => {
    if (mode === 'edit' && inputRef.current) {
      inputRef.current.focus();
      inputRef.current.select();
    }
  }, [mode]);

  const commitEdit = useCallback(() => {
    const parsed = parseFloat(editText);
    if (!isNaN(parsed)) {
      onChange(clamp(parsed));
    }
    setMode('display');
  }, [editText, clamp, onChange]);

  const cancelEdit = useCallback(() => {
    setMode('display');
  }, []);

  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Enter') {
        e.preventDefault();
        commitEdit();
      } else if (e.key === 'Escape') {
        e.preventDefault();
        cancelEdit();
      }
    },
    [commitEdit, cancelEdit],
  );

  if (mode === 'edit') {
    return (
      <input
        ref={inputRef}
        type="text"
        value={editText}
        onChange={(e) => setEditText(e.target.value)}
        onBlur={commitEdit}
        onKeyDown={handleKeyDown}
        style={{
          width: '100%',
          height: '100%',
          background: FIELD_HOVER_BG,
          color: VALUE_TEXT,
          border: 'none',
          outline: 'none',
          fontSize: FONT_SIZE_BASE,
          padding: '0 8px',
          borderRadius: CORNER_RADIUS_SMALL,
          fontFamily: 'inherit',
        }}
      />
    );
  }

  return (
    <div
      onPointerDown={handlePointerDown}
      onPointerMove={handlePointerMove}
      onPointerUp={handlePointerUp}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      style={{
        width: '100%',
        height: '100%',
        display: 'flex',
        alignItems: 'center',
        paddingLeft: 8,
        paddingRight: 8,
        cursor: 'ew-resize',
        userSelect: 'none',
        color: hovered ? VALUE_TEXT_HOVER : VALUE_TEXT,
        background: hovered ? FIELD_HOVER_BG : PORT_VALUE_BACKGROUND,
        borderRadius: hovered ? CORNER_RADIUS_SMALL : 0,
        fontSize: FONT_SIZE_BASE,
      }}
    >
      {displayText}
    </div>
  );
}
