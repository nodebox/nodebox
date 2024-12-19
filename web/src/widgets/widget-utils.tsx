import { useEffect, useRef, useState } from "react";
import { ParameterValue, LiteralValue, Choice } from "@ndbx/runtime";
import Icon from "../components/icon";
import { createPortal } from "react-dom";

export interface WidgetProps {
  name?: string;
  label: string;
  value: LiteralValue;
  min?: number;
  max?: number;
  step?: number;
  choices?: Choice[];
  onChange: (value: ParameterValue) => void;
  onToggleExpression?: () => void;
  onPublishParameter?: () => void;
  onRemove?: () => void;
  onMeta?: (e: React.MouseEvent) => void;
  disabled?: boolean;
}
interface PopupMenuProps {
  isOpen: boolean;
  onClose: () => void;
  position: { x: number; y: number };
  children: React.ReactNode;
  onMouseLeave?: () => void;
  onMouseEnter?: () => void;
}

export function PopupMenu({ isOpen, onClose, position, children, onMouseLeave, onMouseEnter }: PopupMenuProps) {
  const menuRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        onClose();
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [onClose]);

  if (!isOpen) return null;

  return createPortal(
    <div
      ref={menuRef}
      className="absolute bg-zinc-900 shadow-lg rounded-md py-1 min-w-[120px] z-50 border border-zinc-500 text-zinc-400 text-xs"
      style={{ top: position.y, left: position.x }}
      onMouseLeave={onMouseLeave}
      onMouseEnter={onMouseEnter}
    >
      {children}
    </div>,
    document.body,
  );
}

export function MenuItem({ onClick, children }: { onClick: () => void; children: React.ReactNode }) {
  return (
    <div className="px-2 py-1 hover:bg-zinc-700 cursor-pointer text-xs" onClick={onClick}>
      {children}
    </div>
  );
}
export function createParameterValueFromLiteral(value: LiteralValue): ParameterValue {
  return {
    type: "VALUE",
    value,
  };
}

export function WidgetMetaButton({ onMeta }: { onMeta?: (e: React.MouseEvent) => void }) {
  if (!onMeta) return null;
  return (
    <span
      className="absolute top-2 opacity-0 group-hover:opacity-60 transition-opacity cursor-pointer"
      onClick={onMeta}
    >
      <Icon name="cog" />
    </span>
  );
}

export function WidgetRemoveButton({ onRemove }: { onRemove?: () => void }) {
  if (!onRemove) return null;
  return (
    <span className="mt-2 mr-2 opacity-0 group-hover:opacity-60 transition-opacity cursor-pointer" onClick={onRemove}>
      <Icon name="minus" />
    </span>
  );
}

export function WidgetExpressionButton({
  onToggleExpression,
  onPublishParameter,
}: {
  onToggleExpression?: () => void;
  onPublishParameter?: () => void;
}) {
  if (!onToggleExpression) return null;
  const [menuOpen, setMenuOpen] = useState(false);
  const [menuPosition, setMenuPosition] = useState({ x: 0, y: 0 });
  const closeTimeoutRef = useRef<NodeJS.Timeout>();

  useEffect(() => {
    return () => {
      if (closeTimeoutRef.current) {
        clearTimeout(closeTimeoutRef.current);
      }
    };
  }, []);

  const handleMouseLeave = () => {
    closeTimeoutRef.current = setTimeout(() => {
      setMenuOpen(false);
    }, 300);
  };

  const handleMouseEnter = () => {
    if (closeTimeoutRef.current) {
      clearTimeout(closeTimeoutRef.current);
    }
  };

  const handleClick = (e: React.MouseEvent) => {
    e.preventDefault();
    const rect = e.currentTarget.getBoundingClientRect();
    setMenuPosition({
      x: rect.right - 16,
      y: rect.top - 6,
    });
    setMenuOpen(true);
  };

  const handleToggleExpression = () => {
    setMenuOpen(false);
    if (onToggleExpression) {
      onToggleExpression();
    }
  };

  const handlePublishParameter = () => {
    setMenuOpen(false);
    if (onPublishParameter) {
      onPublishParameter();
    }
  };

  return (
    <>
      <span
        className="mt-2 mr-2 opacity-0 group-hover:opacity-60 transition-opacity cursor-pointer"
        onClick={handleClick}
        onMouseLeave={handleMouseLeave}
        onMouseEnter={handleMouseEnter}
      >
        <Icon name="dots-vertical" />
      </span>

      <PopupMenu
        isOpen={menuOpen}
        onClose={() => setMenuOpen(false)}
        position={menuPosition}
        onMouseLeave={handleMouseLeave}
        onMouseEnter={handleMouseEnter}
      >
        <MenuItem onClick={handleToggleExpression}>Toggle Expression</MenuItem>
        <MenuItem onClick={handlePublishParameter}>Publish Parameter</MenuItem>
      </PopupMenu>
    </>
  );
}
