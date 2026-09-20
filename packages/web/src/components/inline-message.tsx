import React, { useEffect, useState } from "react";
import Icon from "./icon";

interface InlineMessageProps {
  children: React.ReactNode;
  className?: string;
  type?: "error" | "warning" | "info" | "success";
  onClose?: () => void;
}

export default function InlineMessage({ children, className = "", type = "error", onClose }: InlineMessageProps) {
  const [progress, setProgress] = useState(100);
  const [visible, setVisible] = useState(true);
  const [fadeOut, setFadeOut] = useState(false); // for smooth fading out

  useEffect(() => {
    if (!onClose) {
      const interval = setInterval(() => {
        setProgress((prev) => prev - 2);
      }, 100);

      const timeout = setTimeout(() => {
        clearInterval(interval);
        setFadeOut(true); // Trigger the fade out
        setTimeout(() => setVisible(false), 500); // Wait for fade-out transition to finish before hiding
      }, 20000);

      return () => {
        clearInterval(interval);
        clearTimeout(timeout);
      };
    }
  }, [onClose]);

  useEffect(() => {
    if (progress <= 0 && !onClose) {
      setFadeOut(true); // Trigger the fade out
      setTimeout(() => setVisible(false), 500); // Wait for fade-out transition to finish before hiding
    }
  }, [progress, onClose]);

  if (!visible) {
    return null; // Completely hide the component after fade-out
  }

  return (
    <div
      className={`mt-4 mb-4 p-4 rounded-md ${className} ${typeStyles(type)} inline-message ${fadeOut ? "inline-message-hidden" : ""}`}
    >
      <div className="flex items-center">
        <div className="flex-shrink-0">
          <Icon name={type} />
        </div>
        <div className="ml-3 flex-1">
          <p className="text-sm">{children}</p>
        </div>
        {onClose && (
          <div className="cursor-pointer hover:opacity-50" onClick={onClose}>
            <Icon name="x" />
          </div>
        )}
      </div>
      {!onClose && (
        <div className={`mt-2 w-full ${typeStyles(type)} rounded-full h-1.5`}>
          <div
            className={`${typeStylesProgress(type)} h-1.5 rounded-full progress-bar`}
            style={{ width: `${progress}%` }}
          ></div>
        </div>
      )}
    </div>
  );
}

function typeStyles(type: "error" | "warning" | "info" | "success") {
  switch (type) {
    case "error":
      return "bg-red-800 border-l-4 border-red-500 text-red-200";
    case "warning":
      return "bg-yellow-800 border-l-4 border-yellow-500 text-yellow-200";
    case "info":
      return "bg-blue-800 border-l-4 border-blue-500 text-blue-200";
    case "success":
      return "bg-green-800 border-l-4 border-green-500 text-green-200";
    default:
      return "";
  }
}

function typeStylesProgress(type: "error" | "warning" | "info" | "success") {
  switch (type) {
    case "error":
      return "bg-red-500";
    case "warning":
      return "bg-yellow-500";
    case "info":
      return "bg-blue-500";
    case "success":
      return "bg-green-500";
    default:
      return "";
  }
}
