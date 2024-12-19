import React from "react";

interface FullscreenModalProps {
  children: React.ReactNode;
  className?: string;
  style: React.CSSProperties;
  onClose: () => void;
}

export default function FullscreenModal({ children, className = "", style = {}, onClose }: FullscreenModalProps) {
  return (
    <div className="fixed z-20 inset-0 overflow-y-auto">
      <div className="flex items-end justify-center min-h-screen pt-4 px-4 pb-20 text-center sm:block sm:p-0">
        <div className="fixed inset-0 transition-opacity" aria-hidden="true" onClick={onClose}>
          <div className="absolute inset-0 bg-zinc-800 opacity-75"></div>
        </div>
        <span className="hidden sm:inline-block sm:align-middle sm:h-screen" aria-hidden="true">
          &#8203;
        </span>
        <div
          className={`inline-block align-bottom bg-white rounded-lg text-left overflow-hidden shadow-xl transform transition-all sm:my-8 sm:align-middle ${className}`}
          style={style}
        >
          {children}
        </div>
      </div>
    </div>
  );
}
