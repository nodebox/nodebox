import React, { useState, useEffect, useRef } from "react";
import { evaluateExpression } from "@ndbx/runtime";
import {
  WidgetProps,
  WidgetMetaButton,
  WidgetRemoveButton,
  WidgetExpressionButton,
  createParameterValueFromLiteral,
} from "./widget-utils";

const isChrome = navigator.userAgent.indexOf("Chrome") > -1;
const isSafari = navigator.userAgent.indexOf("Safari") > -1 && !isChrome;

export default function NumberWidget({
  label,
  value,
  min,
  max,
  step,
  onChange,
  onToggleExpression,
  onPublishParameter,
  onRemove,
  onMeta,
  disabled,
}: WidgetProps) {
  const [inputValue, setInputValue] = useState(typeof value === "number" ? value.toString() : "");
  const [prevValue, setPrevValue] = useState(value);
  const [isEditing, setIsEditing] = useState(false);
  const inputRef = useRef<HTMLInputElement | null>(null);
  const draggingValueRef = useRef<number>(0);
  const dragOffset = useRef<number>(0);
  const isDraggingRef = useRef(false);

  useEffect(() => {
    setInputValue(typeof value === "number" ? value.toString() : "");
    setPrevValue(value);
  }, [value]);

  useEffect(() => {
    if (isEditing) {
      inputRef.current!.select();
    }
  }, [isEditing]);

  const clamp = (value: number) => Math.min(Math.max(value, min!), max!);

  const evaluateAndChange = () => {
    try {
      const evaluated = evaluateExpression(inputValue.toString());
      if (typeof evaluated === "number") {
        onChange(createParameterValueFromLiteral(evaluated));
        setPrevValue(evaluated);
      } else {
        setInputValue(prevValue.toString());
      }
    } catch (e) {
      setInputValue(prevValue.toString());
    }
  };

  const handleBlur = () => {
    evaluateAndChange();
    setIsEditing(false);
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === "Enter") {
      // Blur will trigger evaluateAndChange.
      (e.currentTarget as HTMLInputElement).blur();
    } else if (e.key === "Escape") {
      setInputValue(prevValue.toString());
      setIsEditing(false);
    } else if (e.key === "ArrowUp" && !disabled) {
      if (typeof prevValue !== "number") return;
      const stepMultiplier = e.shiftKey ? 10 : e.altKey ? 0.1 : 1;
      const newValue = clamp(prevValue + stepMultiplier * step!);
      onChange(createParameterValueFromLiteral(newValue));
    } else if (e.key === "ArrowDown" && !disabled) {
      if (typeof prevValue !== "number") return;
      const stepMultiplier = e.shiftKey ? 10 : e.altKey ? 0.1 : 1;
      const newValue = clamp(prevValue - stepMultiplier * step!);
      onChange(createParameterValueFromLiteral(newValue));
    }
  };

  // Drag logic
  const handleMouseDown = (e: React.MouseEvent) => {
    if (disabled) return; // Prevent dragging if disabled
    e.preventDefault();
    dragOffset.current = 0;
    draggingValueRef.current = value as number;
    // It's not because the mouse is down that we are dragging.
    isDraggingRef.current = false;
    if (!isSafari) document.body.requestPointerLock();
    window.addEventListener("mousemove", handleMouseDrag);
    window.addEventListener("mouseup", handleMouseUp);
  };

  const handleMouseDrag = (e: MouseEvent) => {
    if (disabled) return; // Prevent dragging if disabled
    // Ignore large movements, they are probably not intentional.
    if (e.movementX < -100 || e.movementX > 100) return;
    // Only start dragging if we crossed a certain threshold.
    dragOffset.current += e.movementX;
    if (isDraggingRef.current || Math.abs(dragOffset.current) > 5) {
      isDraggingRef.current = true;
      const stepMultiplier = e.shiftKey ? 10 : e.altKey ? 0.1 : 1;
      draggingValueRef.current = clamp(draggingValueRef.current + e.movementX * step! * stepMultiplier);
      onChange(createParameterValueFromLiteral(draggingValueRef.current));
    }
  };

  const handleMouseUp = () => {
    if (!isSafari) document.exitPointerLock();
    window.removeEventListener("mousemove", handleMouseDrag);
    window.removeEventListener("mouseup", handleMouseUp);
    if (!isDraggingRef.current) {
      setIsEditing(true);
    }
  };

  function formatNumber(value: number, maxDecimals = 4) {
    let formatted = value.toFixed(maxDecimals);
    formatted = parseFloat(formatted).toString();
    return formatted;
  }

  return (
    <div
      className={`flex items-stretch hover:bg-zinc-700 px-1 h-8 group relative ${disabled ? "opacity-50 cursor-not-allowed" : ""}`}
    >
      <WidgetMetaButton onMeta={onMeta} />
      <div className="w-28 px-1 text-xs leading-8 text-zinc-400 text-right">{label}</div>
      {!isEditing && (
        <div
          className={`flex-1 text-xs px-2 text-zinc-200 leading-8 ${disabled ? "" : "cursor-ew-resize"}`}
          onMouseDown={!disabled ? handleMouseDown : undefined}
        >
          {formatNumber(value as number)}
        </div>
      )}
      {isEditing && (
        <input
          type="text"
          value={inputValue}
          onChange={(e) => setInputValue(e.target.value)}
          onBlur={handleBlur}
          onKeyDown={handleKeyDown}
          ref={inputRef}
          className="flex-1 text-xs px-2 py-1 bg-transparent border-0 outline-none w-full text-zinc-200"
          disabled={disabled}
        />
      )}
      <WidgetRemoveButton onRemove={onRemove} />
      <WidgetExpressionButton onToggleExpression={onToggleExpression} onPublishParameter={onPublishParameter} />
    </div>
  );
}
