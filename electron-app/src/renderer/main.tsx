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
    rendered_child: s.library.root.rendered_child,
    children: s.library.root.children.map((n) => ({
      name: n.name,
      position: n.position,
      inputs: n.inputs.length,
      prototype: n.prototype,
      output_type: n.output_type,
      category: n.category,
      ports: n.inputs.map((p) => ({
        name: p.name,
        port_type: p.port_type,
        value: p.value,
      })),
    })),
    connections: s.library.root.connections,
    renderResult: s.renderResult
      ? {
          pathCount: s.renderResult.paths.length,
          totalPoints: s.renderResult.paths.reduce(
            (sum, p) => sum + p.contours.reduce((cs, c) => cs + c.points.length, 0),
            0,
          ),
        }
      : null,
    viewerMode: (s as any).viewerMode ?? 'visual',
    isPlaying: s.isPlaying,
    frame: s.frame,
    viewerZoom: s.viewerZoom,
  };
};

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
);
