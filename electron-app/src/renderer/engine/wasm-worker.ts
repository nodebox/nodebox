// Web Worker for WASM-based node evaluation.
// This worker loads the nodebox-electron WASM module and runs evaluations
// off the main thread to keep the UI responsive.

// Message types
interface EvalRequest {
  type: 'eval';
  id: number;
  library: unknown; // Serialized NodeLibrary
  frame: number;
}

interface InitRequest {
  type: 'init';
}

interface CancelRequest {
  type: 'cancel';
  id: number;
}

type WorkerRequest = EvalRequest | InitRequest | CancelRequest;

interface EvalResponse {
  type: 'eval-result';
  id: number;
  result: unknown; // EvalResult
}

interface ErrorResponse {
  type: 'eval-error';
  id: number;
  error: string;
}

interface ReadyResponse {
  type: 'ready';
}

// Future: WorkerResponse = EvalResponse | ErrorResponse | ReadyResponse
// Future: let wasmModule: unknown = null;

self.onmessage = async (event: MessageEvent<WorkerRequest>) => {
  const msg = event.data;

  switch (msg.type) {
    case 'init': {
      try {
        // TODO: Load WASM module when it's built
        // const wasm = await import('../../wasm/nodebox_electron.js');
        // await wasm.default();
        // wasmModule = wasm;
        self.postMessage({ type: 'ready' } satisfies ReadyResponse);
      } catch (err) {
        self.postMessage({
          type: 'eval-error',
          id: 0,
          error: `Failed to init WASM: ${err}`,
        } satisfies ErrorResponse);
      }
      break;
    }

    case 'eval': {
      try {
        // TODO: Call WASM evaluation when module is available
        // const result = wasmModule.evaluate(msg.library, msg.frame);
        const result = {
          paths: [],
          texts: [],
          output: { type: 'geometry', isMultiple: false, values: [] },
          errors: [],
        };
        self.postMessage({
          type: 'eval-result',
          id: msg.id,
          result,
        } satisfies EvalResponse);
      } catch (err) {
        self.postMessage({
          type: 'eval-error',
          id: msg.id,
          error: String(err),
        } satisfies ErrorResponse);
      }
      break;
    }

    case 'cancel': {
      // TODO: Implement cancellation via WASM cancellation token
      break;
    }
  }
};
