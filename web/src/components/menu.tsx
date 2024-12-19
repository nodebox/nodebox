import React, { useRef, useEffect } from "react";

interface MenuItemProps {
  onClick: (event: React.MouseEvent) => void;
  children: React.ReactNode;
  disabled?: boolean;
  shortcutKey?: string;
}

function deviceShortcutKey(shortcutKey: string) {
  const isMac = navigator.platform.toUpperCase().indexOf("MAC") >= 0;
  return isMac ? shortcutKey.replace("ctrl+", "⌘ ") : shortcutKey;
}

export function MenuItem({ onClick, children, disabled = false, shortcutKey = "" }: MenuItemProps) {
  return (
    <div
      className={`py-2 px-3 text-xs min-w-24 flex justify-between ${
        disabled ? "text-zinc-600 cursor-not-allowed" : "text-zinc-200 hover:bg-blue-500 cursor-pointer"
      }`}
      onClick={disabled ? undefined : onClick}
    >
      {children}
      {shortcutKey !== "" ? (
        <div className="text-[10px] ml-2 text-right text-zinc-600">{deviceShortcutKey(shortcutKey)}</div>
      ) : (
        ""
      )}
    </div>
  );
}

export function MenuSeparator() {
  return <div className="border-t border-zinc-700 my-1"></div>;
}

interface MenuProps {
  open: boolean;
  onClose: () => void;
  onContextMenu?: (e: React.MouseEvent) => void;
  anchorPosition: { x: number; y: number };
  children: React.ReactNode;
}

export function Menu({ open, onClose, onContextMenu, anchorPosition, children }: MenuProps) {
  const menuRef = useRef<HTMLDivElement>(null);
  const isFirefox = useRef(false); // To track if the browser is Firefox
  const hasClearedClipboard = useRef(false); // To track one-time clipboard clearing

  useEffect(() => {
    isFirefox.current = typeof navigator !== "undefined" && /firefox/i.test(navigator.userAgent);
    if (!open) return;

    if (isFirefox.current && !hasClearedClipboard.current) {
      navigator.clipboard.writeText(""); // Clear clipboard
      hasClearedClipboard.current = true; // Mark that clipboard has been cleared once
    }

    const menuBounds = menuRef.current!.getBoundingClientRect();
    menuRef.current!.style.left = Math.min(anchorPosition.x, window.innerWidth - menuBounds.width) + "px";
    menuRef.current!.style.top = Math.min(anchorPosition.y, window.innerHeight - menuBounds.height) + "px";

    const handleClickOutside = (event: MouseEvent) => {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        onClose();
      }
    };

    // Close the menu when clicking outside
    document.addEventListener("mousedown", handleClickOutside);

    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
    };
  }, [open, anchorPosition, onClose]);

  if (!open) return null;

  return (
    <div
      className="fixed z-20 inset-0 overflow-y-auto"
      onContextMenu={(e) => {
        e.preventDefault(); // Prevent the default context menu from showing
        onContextMenu && onContextMenu(e); // Call the provided onContextMenu if any
      }}
      onClick={onClose}
    >
      <div className="fixed inset-0" aria-hidden="true">
        <div className="absolute inset-0"></div>
      </div>
      <div
        ref={menuRef}
        className="fixed bg-zinc-950 rounded shadow-lg overflow-hidden py-2"
        style={{ left: anchorPosition.x, top: anchorPosition.y }}
      >
        {children}
      </div>
    </div>
  );
}
