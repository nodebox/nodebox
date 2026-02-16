import { useState, useRef, useCallback, useEffect } from 'react';
import {
  FIELD_HOVER_BG,
  CORNER_RADIUS_SMALL,
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
  const dragOriginX = useRef(0);
  const lastDragX = useRef(0);
  const accumulatedValue = useRef(0);
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
      dragOriginX.current = e.clientX;
      lastDragX.current = e.clientX;
      accumulatedValue.current = value;
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

      if (!hasDragged.current && Math.abs(e.clientX - dragOriginX.current) < DRAG_THRESHOLD) return;

      const moveDelta = e.clientX - lastDragX.current;
      lastDragX.current = e.clientX;
      hasDragged.current = true;
      setMode('drag');

      let effectiveSpeed = speed;
      if (e.shiftKey) effectiveSpeed *= 10;
      if (e.altKey) effectiveSpeed *= 0.01;

      accumulatedValue.current += moveDelta * effectiveSpeed;
      onChange(clamp(accumulatedValue.current));
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
        className="w-full h-full bg-field-hover text-zinc-100 border-none outline-none text-[13px] px-2 rounded-sm font-[inherit]"
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
      className="w-full h-full flex items-center cursor-ew-resize select-none text-[13px]"
    >
      <div
        data-testid="drag-value-inner"
        style={{
          flex: 1,
          display: 'flex',
          alignItems: 'center',
          padding: '0 8px',
          margin: '2px 0',
          height: 'calc(100% - 4px)',
          borderRadius: hovered ? CORNER_RADIUS_SMALL : 0,
          background: hovered ? FIELD_HOVER_BG : 'transparent',
          color: hovered ? '#fafafa' : '#f4f4f5',
        }}
      >
        {displayText}
      </div>
    </div>
  );
}
