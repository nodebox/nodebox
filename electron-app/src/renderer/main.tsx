import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { App } from './App';
import { useStore } from './state/store';
import './App.css';

// Expose store state for E2E test assertions on canvas-based features
(window as any).__storeState__ = () => {
  const s = useStore.getState();
  return {
    selectedNodes: [...s.selectedNodes],
    activeNode: s.activeNode,
    renderedChild: s.library.root.renderedChild,
    children: s.library.root.children.map((n) => ({
      name: n.name,
      position: n.position,
      inputs: n.inputs.length,
      prototype: n.prototype,
      outputType: n.outputType,
      ports: n.inputs.map((p) => ({
        name: p.name,
        portType: p.portType,
        value: p.value,
      })),
    })),
    connections: s.library.root.connections,
    renderResult: s.renderResult
      ? { pathCount: s.renderResult.paths.length }
      : null,
    viewerMode: (s as any).viewerMode ?? 'visual',
  };
};

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
