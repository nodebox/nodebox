// Main-thread wrapper for the WASM evaluation worker.
// Manages worker lifecycle, request queuing, and result dispatching.

import type { EvalResult } from '../types/eval-result';
import type { NodeLibrary } from '../types/node';

type ResultCallback = (result: EvalResult) => void;
type ErrorCallback = (error: string) => void;

export class RenderWorker {
  private worker: Worker | null = null;
  private nextId = 1;
  private ready = false;
  private pendingCallbacks = new Map<
    number,
    { resolve: ResultCallback; reject: ErrorCallback }
  >();

  async init(): Promise<void> {
    return new Promise((resolve, reject) => {
      this.worker = new Worker(
        new URL('./wasm-worker.ts', import.meta.url),
        { type: 'module' },
      );

      this.worker.onmessage = (event) => {
        const msg = event.data;

        switch (msg.type) {
          case 'ready': {
            this.ready = true;
            resolve();
            break;
          }
          case 'eval-result': {
            const cb = this.pendingCallbacks.get(msg.id);
            if (cb) {
              cb.resolve(msg.result as EvalResult);
              this.pendingCallbacks.delete(msg.id);
            }
            break;
          }
          case 'eval-error': {
            const cb = this.pendingCallbacks.get(msg.id);
            if (cb) {
              cb.reject(msg.error);
              this.pendingCallbacks.delete(msg.id);
            }
            break;
          }
        }
      };

      this.worker.onerror = (err) => {
        reject(new Error(err.message));
      };

      this.worker.postMessage({ type: 'init' });
    });
  }

  evaluate(library: NodeLibrary, frame: number): Promise<EvalResult> {
    return new Promise((resolve, reject) => {
      if (!this.worker || !this.ready) {
        reject('Worker not ready');
        return;
      }

      const id = this.nextId++;
      this.pendingCallbacks.set(id, {
        resolve,
        reject: (err) => reject(new Error(err)),
      });

      this.worker.postMessage({
        type: 'eval',
        id,
        library,
        frame,
      });
    });
  }

  cancel(id: number): void {
    this.worker?.postMessage({ type: 'cancel', id });
    this.pendingCallbacks.delete(id);
  }

  dispose(): void {
    this.worker?.terminate();
    this.worker = null;
    this.ready = false;
    this.pendingCallbacks.clear();
  }
}
