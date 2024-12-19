import { useEffect, useState } from "react";
import { toast } from "react-toastify";

export function debounce<F extends (...args: any[]) => any>(
  func: F,
  timeout: number,
): (...args: Parameters<F>) => void {
  let timerId: ReturnType<typeof setTimeout> | null = null;

  return (...args: Parameters<F>): void => {
    if (timerId !== null) {
      clearTimeout(timerId);
    }

    timerId = setTimeout(() => {
      func(...args);
      timerId = null;
    }, timeout);
  };
}

export async function sleep(timeout: number) {
  return new Promise((resolve) => {
    setTimeout(resolve, timeout);
  });
}

export function formatRelativeTime(date: string | Date) {
  const now = new Date();
  const then = new Date(date);

  const nowDay = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const thenDay = new Date(then.getFullYear(), then.getMonth(), then.getDate());

  const seconds = Math.floor((now.getTime() - then.getTime()) / 1000);

  if (seconds < 10) return "just now";
  if (seconds < 30) return `${seconds} seconds ago`;
  if (seconds < 60) return "less than a minute ago";

  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes} minute${minutes === 1 ? "" : "s"} ago`;

  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} hour${hours === 1 ? "" : "s"} ago`;

  if (thenDay.getTime() === new Date(nowDay.getTime() - 86400000).getTime()) {
    return "yesterday";
  }

  const weekDiff = Math.floor((nowDay.getTime() - thenDay.getTime()) / (86400000 * 7));
  if (weekDiff === 1) {
    return "last week";
  }

  if (now.getFullYear() === then.getFullYear() && now.getMonth() - then.getMonth() === 1) {
    return "last month";
  }

  if (now.getFullYear() - then.getFullYear() === 1 && now.getMonth() === then.getMonth()) {
    return "last year";
  }

  return then.toLocaleDateString();
}

export function useRelativeTime(date: string | Date) {
  const [relativeTime, setRelativeTime] = useState("");

  useEffect(() => {
    if (!date) return;

    let timeoutId: number;

    function updateTime() {
      setRelativeTime(formatRelativeTime(date));

      const interval = getUpdateInterval();
      timeoutId = window.setTimeout(updateTime, interval);
    }

    function getUpdateInterval() {
      const seconds = (Date.now() - new Date(date).getTime()) / 1000;
      if (seconds < 60) return 1000; // Update every second if less than a minute ago
      if (seconds < 3600) return 60000; // Update every minute if less than an hour ago
      if (seconds < 86400) return 300000;
      return 3600000;
    }

    updateTime();

    return () => {
      if (timeoutId) {
        clearTimeout(timeoutId);
      }
    };
  }, [date]);

  return relativeTime;
}

type ToastType = "info" | "success" | "warning" | "error";

export function showToast(message: string, type: ToastType = "info") {
  if (message) {
    toast[type](message);
  }
}
export function showToastOnce(message: string) {
  const key = "shown_messages";
  const shownMessages = JSON.parse(localStorage.getItem(key) || "[]");

  if (message && !shownMessages.includes(message)) {
    showToast(message);
    localStorage.setItem(key, JSON.stringify([...shownMessages, message]));
  }
}
