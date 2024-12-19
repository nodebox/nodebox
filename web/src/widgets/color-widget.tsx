import React, { useState, useRef, useEffect } from "react";
import { createPortal } from "react-dom";
import Chrome from "@uiw/react-color-chrome";
import {
  WidgetProps,
  WidgetMetaButton,
  WidgetRemoveButton,
  WidgetExpressionButton,
  createParameterValueFromLiteral,
} from "./widget-utils";
import { HsvaColor, rgbaToHsva, hsvaToRgba } from "@uiw/color-convert";
import { colorToCss } from "../lib/color-utils";
import { GithubPlacement } from "@uiw/react-color-github";
import { Paint } from "@ndbx/g";

interface ColorValue {
  r: number;
  g: number;
  b: number;
  a: number;
}

export default function ColorWidget({
  label,
  value,
  onChange,
  onToggleExpression,
  onPublishParameter,
  onRemove,
  onMeta,
}: WidgetProps & { value: ColorValue }) {
  const pickerRef = useRef<HTMLDivElement | null>(null);
  const [pickerVisible, setPickerVisible] = useState(false);
  const [position, setPosition] = useState({ x: 0, y: 0 });
  const [pickerDimensions, setPickerDimensions] = useState({ width: 225, height: 240 });
  if (typeof value === "string") {
    const { r, g, b, a } = Paint.parse(value) as unknown as ColorValue;
    value = { r, g, b, a };
  }
  // Convert Color to HSVA
  const clr = { r: value.r * 255, g: value.g * 255, b: value.b * 255, a: value.a };

  const hsva = rgbaToHsva(clr);

  function handleColorChange(color: { hsva: HsvaColor }) {
    const { r, g, b, a } = hsvaToRgba(color.hsva);
    onChange(createParameterValueFromLiteral({ r: r / 255, g: g / 255, b: b / 255, a }));
  }

  function handleShowPicker(e: React.MouseEvent) {
    const PICKER_WIDTH = pickerDimensions.width;
    const PICKER_HEIGHT = pickerDimensions.height;
    const viewportMargin = 10;

    const colorDiv = (e.currentTarget as HTMLElement).querySelector("div")!.getBoundingClientRect();

    const viewportWidth = window.innerWidth;
    const viewportHeight = window.innerHeight;

    let x = colorDiv.left + colorDiv.width + 2;
    let y = colorDiv.top;

    if (x + PICKER_WIDTH > viewportWidth - viewportMargin) {
      x = Math.max(viewportMargin, viewportWidth - PICKER_WIDTH - viewportMargin);
    }

    if (y + PICKER_HEIGHT > viewportHeight - viewportMargin) {
      y = viewportHeight - PICKER_HEIGHT - viewportMargin;
    }

    setPosition({ x, y });
    setPickerVisible(true);
  }

  // Add click outside handler
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (pickerRef.current && !pickerRef.current.contains(event.target as Node)) {
        setPickerVisible(false);
      }
    }

    if (pickerVisible) {
      document.addEventListener("mousedown", handleClickOutside);
    }

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [pickerVisible]);

  // Add effect to measure picker after mount
  useEffect(() => {
    if (pickerVisible && pickerRef.current) {
      const rect = pickerRef.current.getBoundingClientRect();
      setPickerDimensions({
        width: rect.width,
        height: rect.height,
      });

      const divs = pickerRef.current.querySelectorAll("div[style*='border-style: solid; position: absolute;']");
      divs.forEach((div) => {
        div.remove();
      });
    }
  }, [pickerVisible]);

  return (
    <div className="flex items-stretch px-1 h-8 hover:bg-zinc-700 group relative">
      <WidgetMetaButton onMeta={onMeta} />
      <div className="w-28 px-1 text-xs leading-8 text-zinc-400 text-right">{label}</div>
      <div className="flex-1 flex items-center h-8" onClick={handleShowPicker}>
        <div className="relative ml-2 w-10 h-5 shadow-sm rounded border overflow-hidden">
          <div className="absolute inset-0 bg-[url('data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAMUlEQVQ4T2NkYGAQYcAP3uCTZhw1gGGYhAGBZIA/nYDCgBDAm9BGDWAAJyRCgLaBCAAgXwixzAS0pgAAAABJRU5ErkJggg==')]" />
          <div className="absolute inset-0" style={{ backgroundColor: colorToCss(value) }} />
        </div>
      </div>
      <WidgetRemoveButton onRemove={onRemove} />
      <WidgetExpressionButton onToggleExpression={onToggleExpression} onPublishParameter={onPublishParameter} />
      {pickerVisible &&
        createPortal(
          <div className="fixed inset-0 z-40" onClick={(e) => e.stopPropagation()}>
            <div className="fixed z-50" style={{ left: position.x, top: position.y }}>
              <Chrome
                color={hsva}
                onChange={handleColorChange}
                ref={pickerRef}
                className="!bg-zinc-800 !border-zinc-600"
                placement={GithubPlacement.LeftTop}
              />
            </div>
          </div>,
          document.body,
        )}
    </div>
  );
}
