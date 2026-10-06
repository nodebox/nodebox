import { useEffect } from 'react';
import { useStore } from './state/store';
import { useKeyboardShortcuts } from './hooks/useKeyboardShortcuts';
import { AppLayout } from './components/AppLayout';
import { evaluate } from './eval/evaluator';
import { exportSvg, isWasmReady, onWasmReady, parseNdbx, renderViewerPixels, serializeNdbx } from './eval/wasm';
import { parseHexColor } from './components/ViewerCanvas';
import { ZINC_200 } from './theme/tokens';
import { createDefaultLibrary } from './types/node';
import type { MenuAction } from '../shared/ipc-channels';

/** Extract directory from a file path (e.g., '/a/b/file.ndbx' -> '/a/b'). */
function dirname(filePath: string): string {
  const sep = filePath.lastIndexOf('/');
  const backSep = filePath.lastIndexOf('\\');
  const lastSep = Math.max(sep, backSep);
  return lastSep >= 0 ? filePath.substring(0, lastSep) : '.';
}

/** The size and background colour of the document canvas. */
function documentFrame(library: { properties: Record<string, string> }) {
  return {
    width: parseFloat(library.properties.canvasWidth ?? '1000'),
    height: parseFloat(library.properties.canvasHeight ?? '1000'),
    background: parseHexColor(library.properties.canvasBackground ?? ZINC_200),
  };
}

/** Encode RGBA pixels as a PNG file. */
async function encodePng(pixels: Uint8Array, width: number, height: number): Promise<Uint8Array> {
  const canvas = new OffscreenCanvas(width, height);
  const image = new ImageData(new Uint8ClampedArray(pixels), width, height);
  canvas.getContext('2d')!.putImageData(image, 0, 0);
  const blob = await canvas.convertToBlob({ type: 'image/png' });
  return new Uint8Array(await blob.arrayBuffer());
}

export function App() {
  useKeyboardShortcuts();

  const library = useStore((s) => s.library);
  const filePath = useStore((s) => s.filePath);
  const frame = useStore((s) => s.frame);
  const isPlaying = useStore((s) => s.isPlaying);
  const renderResult = useStore((s) => s.renderResult);
  const setFrame = useStore((s) => s.setFrame);
  const setRenderResult = useStore((s) => s.setRenderResult);
  const syncViewerModeForRender = useStore((s) => s.syncViewerModeForRender);
  const toggleHandles = useStore((s) => s.toggleHandles);
  const togglePoints = useStore((s) => s.togglePoints);
  const toggleOrigin = useStore((s) => s.toggleOrigin);
  const toggleCanvasBorder = useStore((s) => s.toggleCanvasBorder);
  const setAboutDialogVisible = useStore((s) => s.setAboutDialogVisible);
  const setLibrary = useStore((s) => s.setLibrary);
  const setFilePath = useStore((s) => s.setFilePath);
  const markClean = useStore((s) => s.markClean);
  const clearHistory = useStore((s) => s.clearHistory);
  const clearSelection = useStore((s) => s.clearSelection);

  const projectDir = filePath ? dirname(filePath) : null;

  // Run the evaluator whenever the library, frame, or project dir changes
  useEffect(() => {
    let cancelled = false;
    evaluate(library, frame, projectDir).then((result) => {
      if (!cancelled) setRenderResult(result);
    });
    return () => { cancelled = true; };
  }, [library, frame, projectDir, setRenderResult]);

  // Re-evaluate once WASM is initialized (for textpath nodes)
  useEffect(() => {
    onWasmReady(() => {
      const s = useStore.getState();
      const dir = s.filePath ? dirname(s.filePath) : null;
      evaluate(s.library, s.frame, dir).then((result) => {
        s.setRenderResult(result);
      });
    });
  }, []);

  useEffect(() => {
    const renderedChild = library.root.rendered_child;
    const hasRenderedChild = renderedChild !== null;

    if (!hasRenderedChild) {
      syncViewerModeForRender(false, true);
      return;
    }

    const declaredOutputType = library.root.children.find(
      (child) => child.name === renderedChild,
    )?.output_type;
    const outputType = declaredOutputType ?? renderResult?.output.type;

    if (!outputType) return;

    const isVisualOutput = outputType === 'Geometry' || outputType === 'Point';
    syncViewerModeForRender(true, isVisualOutput);
  }, [library.root.children, library.root.rendered_child, renderResult?.output.type, syncViewerModeForRender]);

  // Animation playback loop
  useEffect(() => {
    if (!isPlaying) return;
    let lastTime = performance.now();
    let rafId: number;
    const fps = 30;
    const frameDuration = 1000 / fps;

    const tick = (now: number) => {
      const elapsed = now - lastTime;
      if (elapsed >= frameDuration) {
        lastTime = now - (elapsed % frameDuration);
        const state = useStore.getState();
        const nextFrame = state.frame >= state.frameEnd ? state.frameStart : state.frame + 1;
        setFrame(nextFrame);
      }
      rafId = requestAnimationFrame(tick);
    };
    rafId = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(rafId);
  }, [isPlaying, setFrame]);

  useEffect(() => {
    if (!window.electronAPI) return;

    window.electronAPI.onMenuAction(async (action: string) => {
      const menuAction = action as MenuAction;
      switch (menuAction) {
        // File operations
        case 'file:new':
          setLibrary(createDefaultLibrary());
          clearHistory();
          setFilePath(null);
          clearSelection();
          break;
        case 'file:open': {
          if (!isWasmReady()) break;
          const openResult = await window.electronAPI.openFile();
          if (openResult) {
            try {
              const parsed = parseNdbx(openResult.content);
              setLibrary(parsed);
              setFilePath(openResult.path);
              clearHistory();
              clearSelection();
            } catch (e) {
              console.error('Failed to parse .ndbx file:', e);
            }
          }
          break;
        }
        case 'file:save': {
          if (!isWasmReady()) break;
          try {
            const state = useStore.getState();
            const ndbx = serializeNdbx(state.library);
            const saveResult = await window.electronAPI.saveFile(ndbx);
            if (saveResult) {
              setFilePath(saveResult.path);
              markClean();
            }
          } catch (e) {
            console.error('Failed to serialize .ndbx file:', e);
          }
          break;
        }
        case 'file:save-as': {
          if (!isWasmReady()) break;
          try {
            const state = useStore.getState();
            const ndbx = serializeNdbx(state.library);
            const saveAsResult = await window.electronAPI.saveFileAs(ndbx);
            if (saveAsResult) {
              setFilePath(saveAsResult.path);
              markClean();
            }
          } catch (e) {
            console.error('Failed to serialize .ndbx file:', e);
          }
          break;
        }
        // Edit operations
        case 'edit:undo': {
          const snapshot = useStore.getState().undo();
          if (snapshot) setLibrary(snapshot);
          break;
        }
        case 'edit:redo': {
          const snapshot = useStore.getState().redo();
          if (snapshot) setLibrary(snapshot);
          break;
        }
        case 'edit:delete': {
          const state = useStore.getState();
          if (state.selectedNodes.size === 0) break;
          state.pushSnapshot(state.library);
          for (const name of state.selectedNodes) {
            state.removeNode('root', name);
          }
          state.clearSelection();
          break;
        }
        // View toggles
        case 'view:toggle-handles':
          toggleHandles();
          break;
        case 'view:toggle-points':
          togglePoints();
          break;
        case 'view:toggle-origin':
          toggleOrigin();
          break;
        case 'view:toggle-canvas-border':
          toggleCanvasBorder();
          break;
        // Export the document at its own size, with the origin in the centre.
        case 'export:svg': {
          const doc = documentFrame(useStore.getState().library);
          await window.electronAPI.exportSvg(exportSvg(doc.width, doc.height, doc.background));
          break;
        }
        case 'export:png': {
          const doc = documentFrame(useStore.getState().library);
          const width = Math.round(doc.width);
          const height = Math.round(doc.height);
          const pixels = await renderViewerPixels({
            width,
            height,
            offsetX: width / 2,
            offsetY: height / 2,
            scale: 1,
            background: doc.background,
          });
          await window.electronAPI.exportPng(await encodePng(pixels, width, height));
          break;
        }
        case 'help:about':
          setAboutDialogVisible(true);
          break;
      }
    });
  }, [
    toggleHandles,
    togglePoints,
    toggleOrigin,
    toggleCanvasBorder,
    setAboutDialogVisible,
    setLibrary,
    setFilePath,
    markClean,
    clearHistory,
    clearSelection,
  ]);

  return <AppLayout />;
}
