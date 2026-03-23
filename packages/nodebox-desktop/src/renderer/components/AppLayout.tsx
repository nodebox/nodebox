import React, { useCallback, useRef, useState } from 'react';
import { useStore } from '../state/store';
import { AddressBar } from './AddressBar';
import { AnimationBar } from './AnimationBar';
import { NetworkCanvas } from './NetworkCanvas';
import { ViewerCanvas } from './ViewerCanvas';
import { DataViewer } from './DataViewer';
import { ParameterPanel } from './ParameterPanel';
import { NodeSelectionDialog } from './NodeSelectionDialog';
import { NotificationBanner } from './NotificationBanner';

function SegmentButton({ label, active, onClick }: { label: string; active: boolean; onClick?: () => void }) {
  return (
    <span
      onClick={onClick}
      className={`text-[11px] px-1.5 py-px cursor-pointer select-none ${active ? 'bg-zinc-600 text-zinc-50' : 'text-zinc-200'}`}
    >
      {label}
    </span>
  );
}

function ToggleButton({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <span onClick={onClick} className={`text-[11px] px-1 py-px cursor-pointer select-none ${active ? 'text-zinc-50' : 'text-zinc-400'}`}>
      {label}
    </span>
  );
}

function ZoomControl({ zoom, onZoom }: { zoom: number; onZoom: (action: 'in' | 'out' | 'reset') => void }) {
  return (
    <div className="flex items-center h-5 shrink-0">
      <button onClick={() => onZoom('out')} className="w-[22px] h-[22px] flex items-center justify-center bg-transparent border-none text-zinc-400 hover:text-zinc-50 cursor-pointer text-sm shrink-0">−</button>
      <span onClick={() => onZoom('reset')} className="w-12 text-center text-[11px] text-zinc-400 cursor-pointer select-none">
        {Math.round(zoom * 100)}%
      </span>
      <button onClick={() => onZoom('in')} className="w-[22px] h-[22px] flex items-center justify-center bg-transparent border-none text-zinc-400 hover:text-zinc-50 cursor-pointer text-sm shrink-0">+</button>
    </div>
  );
}

function PaneHeader({ children }: { children: React.ReactNode }) {
  return (
    <div style={{ height: 24, background: '#3f3f46', color: '#e4e4e7', fontSize: 11, borderTop: '1px solid #52525c', borderBottom: '1px solid #18181b', display: 'flex', alignItems: 'center', padding: '0 8px', gap: 4, flexShrink: 0 }}>
      {children}
    </div>
  );
}

function PaneSeparator() {
  return <div className="w-px self-stretch bg-zinc-500 my-1" />;
}

function PaneLabel({ children }: { children: React.ReactNode }) {
  return <span className="w-24 text-[10px] tracking-wider uppercase">{children}</span>;
}

function ViewerHeader() {
  const viewerMode = useStore((s) => s.viewerMode);
  const setViewerMode = useStore((s) => s.setViewerMode);
  const showHandles = useStore((s) => s.showHandles);
  const showPoints = useStore((s) => s.showPoints);
  const showPointNumbers = useStore((s) => s.showPointNumbers);
  const showOrigin = useStore((s) => s.showOrigin);
  const showCanvasBorder = useStore((s) => s.showCanvasBorder);
  const toggleHandles = useStore((s) => s.toggleHandles);
  const togglePoints = useStore((s) => s.togglePoints);
  const togglePointNumbers = useStore((s) => s.togglePointNumbers);
  const toggleOrigin = useStore((s) => s.toggleOrigin);
  const toggleCanvasBorder = useStore((s) => s.toggleCanvasBorder);
  const viewerZoom = useStore((s) => s.viewerZoom);
  const requestViewerZoom = useStore((s) => s.requestViewerZoom);

  return (
    <PaneHeader>
      <PaneLabel>Viewer</PaneLabel>
      <PaneSeparator />
      <SegmentButton label="Visual" active={viewerMode === 'visual'} onClick={() => setViewerMode('visual')} />
      <SegmentButton label="Data" active={viewerMode === 'data'} onClick={() => setViewerMode('data')} />
      <div className="w-2" />
      <ToggleButton label="Handles" active={showHandles} onClick={toggleHandles} />
      <ToggleButton label="Points" active={showPoints} onClick={togglePoints} />
      <ToggleButton label="Pt#" active={showPointNumbers} onClick={togglePointNumbers} />
      <ToggleButton label="Origin" active={showOrigin} onClick={toggleOrigin} />
      <ToggleButton label="Canvas" active={showCanvasBorder} onClick={toggleCanvasBorder} />
      <div className="flex-1" />
      <ZoomControl zoom={viewerZoom} onZoom={requestViewerZoom} />
    </PaneHeader>
  );
}

function ParametersHeader() {
  const activeNode = useStore((s) => s.activeNode);
  const library = useStore((s) => s.library);
  const node = activeNode && library ? library.root.children.find((n: any) => n.name === activeNode) : null;

  return (
    <PaneHeader>
      <PaneLabel>Parameters</PaneLabel>
      <PaneSeparator />
      <span className="ml-1 text-[11px]">{node ? node.name : 'Document'}</span>
      <div className="flex-1" />
      {node?.prototype && <span className="text-[11px] text-zinc-400">{node.prototype}</span>}
    </PaneHeader>
  );
}

function NetworkHeader() {
  const setNodeSelectionDialogOpen = useStore((s) => s.setNodeSelectionDialogOpen);
  return (
    <PaneHeader>
      <PaneLabel>Network</PaneLabel>
      <PaneSeparator />
      <div className="flex-1" />
      <button onClick={() => setNodeSelectionDialogOpen(true)} className="bg-transparent border-none text-zinc-200 text-[11px] cursor-pointer px-2 py-0.5">
        + New Node
      </button>
    </PaneHeader>
  );
}

function HorizontalSplitter({ onDrag }: { onDrag: (deltaY: number) => void }) {
  const dragging = useRef(false);
  const lastY = useRef(0);
  return (
    <div
      className="h-0.5 bg-panel relative cursor-row-resize shrink-0"
      onPointerDown={(e) => { dragging.current = true; lastY.current = e.clientY; (e.target as HTMLElement).setPointerCapture(e.pointerId); }}
      onPointerMove={(e) => { if (!dragging.current) return; onDrag(e.clientY - lastY.current); lastY.current = e.clientY; }}
      onPointerUp={() => { dragging.current = false; }}
    >
      <div className="absolute left-0 right-0 -top-[3px] -bottom-[3px] cursor-row-resize" />
    </div>
  );
}

function VerticalSplitter({ onDrag }: { onDrag: (deltaX: number) => void }) {
  const dragging = useRef(false);
  const lastX = useRef(0);
  return (
    <div
      className="w-0.5 bg-panel relative cursor-col-resize shrink-0"
      onPointerDown={(e) => { dragging.current = true; lastX.current = e.clientX; (e.target as HTMLElement).setPointerCapture(e.pointerId); }}
      onPointerMove={(e) => { if (!dragging.current) return; onDrag(e.clientX - lastX.current); lastX.current = e.clientX; }}
      onPointerUp={() => { dragging.current = false; }}
    >
      <div className="absolute top-0 bottom-0 -left-[3px] -right-[3px] cursor-col-resize" />
    </div>
  );
}

function ViewerContent() {
  const viewerMode = useStore((s) => s.viewerMode);
  return (
    <div className="w-full h-full relative">
      <div className={`absolute inset-0 ${viewerMode === 'visual' ? 'visible pointer-events-auto' : 'invisible pointer-events-none'}`}>
        <ViewerCanvas />
      </div>
      {viewerMode === 'data' && <DataViewer />}
    </div>
  );
}

export function AppLayout() {
  const [rightPanelSplit, setRightPanelSplit] = useState(0.35);
  const rightPanelWidth = useStore((s) => s.parameterPanelWidth);
  const setRightPanelWidth = useStore((s) => s.setParameterPanelWidth);
  const rightPanelRef = useRef<HTMLDivElement>(null);

  const onHorizontalDrag = useCallback((deltaY: number) => {
    if (!rightPanelRef.current) return;
    const totalHeight = rightPanelRef.current.clientHeight;
    setRightPanelSplit((prev) => Math.max(0.15, Math.min(0.85, prev + deltaY / totalHeight)));
  }, []);

  const onVerticalDrag = useCallback((deltaX: number) => {
    setRightPanelWidth(Math.max(300, Math.min(600, rightPanelWidth - deltaX)));
  }, [rightPanelWidth, setRightPanelWidth]);

  return (
    <div className="flex flex-col h-screen bg-panel text-zinc-100">
      <AddressBar />
      <NotificationBanner />
      <div className="flex flex-1 min-h-0">
        <div className="flex flex-col flex-1 min-w-0">
          <ViewerHeader />
          <div className="flex-1 min-h-0">
            <ViewerContent />
          </div>
        </div>

        <VerticalSplitter onDrag={onVerticalDrag} />

        <div ref={rightPanelRef} className="flex flex-col min-w-0" style={{ width: rightPanelWidth }}>
          <div className="flex flex-col min-h-0" style={{ flex: rightPanelSplit }}>
            <ParametersHeader />
            <div className="flex-1 min-h-0 overflow-auto">
              <ParameterPanel />
            </div>
          </div>
          <HorizontalSplitter onDrag={onHorizontalDrag} />
          <div className="flex flex-col min-h-0" style={{ flex: 1 - rightPanelSplit }}>
            <NetworkHeader />
            <div className="flex-1 min-h-0">
              <NetworkCanvas />
            </div>
          </div>
        </div>
      </div>
      <AnimationBar />
      <NodeSelectionDialog />
    </div>
  );
}
