import type { NodeLibrary, EvalResult } from 'nodebox-core';

type EvalCallback = (result: EvalResult) => void;
type ErrorCallback = (error: string) => void;

let worker: Worker | null = null;
let pendingCallback: EvalCallback | null = null;
let pendingErrorCallback: ErrorCallback | null = null;
let debounceTimer: ReturnType<typeof setTimeout> | null = null;

function getWorker(): Worker {
  if (!worker) {
    worker = new Worker(new URL('./eval-worker.ts', import.meta.url), { type: 'module' });
    worker.onmessage = (e: MessageEvent) => {
      if (e.data.type === 'result' && pendingCallback) {
        pendingCallback(e.data.result);
      } else if (e.data.type === 'error' && pendingErrorCallback) {
        pendingErrorCallback(e.data.error);
      }
    };
  }
  return worker;
}

export function requestEvaluation(
  library: NodeLibrary,
  frame: number,
  onResult: EvalCallback,
  onError: ErrorCallback,
  debounceMs = 16,
): void {
  pendingCallback = onResult;
  pendingErrorCallback = onError;

  if (debounceTimer) clearTimeout(debounceTimer);
  debounceTimer = setTimeout(() => {
    const w = getWorker();
    w.postMessage({ library, frame, files: {} });
  }, debounceMs);
}

export function terminateWorker(): void {
  if (worker) {
    worker.terminate();
    worker = null;
  }
}
