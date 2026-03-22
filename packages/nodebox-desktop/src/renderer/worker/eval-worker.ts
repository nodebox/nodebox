import { evaluate, createDefaultRegistry, type EvalOptions } from 'nodebox-core';
import { TestPlatform } from 'nodebox-core';

// Worker message handling
self.onmessage = async (e: MessageEvent) => {
  const { library, frame, files } = e.data;

  // Create a worker platform with pre-loaded files
  const platform = new TestPlatform();
  if (files) {
    for (const [path, content] of Object.entries(files)) {
      platform.addFile(path, content as string);
    }
  }

  try {
    const result = await evaluate({
      library,
      frame,
      platform,
    });

    self.postMessage({ type: 'result', result });
  } catch (error) {
    self.postMessage({
      type: 'error',
      error: error instanceof Error ? error.message : String(error),
    });
  }
};
