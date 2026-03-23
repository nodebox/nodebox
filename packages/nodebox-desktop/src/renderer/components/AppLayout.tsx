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
import {
  PANEL_BG,
  PANE_HEADER_FOREGROUND_COLOR,
  PANE_HEADER_BACKGROUND_COLOR,
  PANE_HEADER_HEIGHT,
  FONT_SIZE_SMALL,
  SPLITTER_THICKNESS,
  SPLITTER_AFFORDANCE,
  LABEL_WIDTH,
  TEXT_STRONG,
  TEXT_DISABLED,
  ZINC_500,
  ZINC_600,
  ZINC_900,
} from '../theme/tokens';

const headerStyle: React.CSSProperties = {
  height: PANE_HEADER_HEIGHT,
  background: PANE_HEADER_BACKGROUND_COLOR,
  color: PANE_HEADER_FOREGROUND_COLOR,
  fontSize: FONT_SIZE_SMALL,
  borderTop: `1px solid ${ZINC_600}`,
  borderBottom: `1px solid ${ZINC_900}`,
  display: 'flex',
  alignItems: 'center',
  paddingLeft: 8,
  paddingRight: 8,
  gap: 4,
  flexShrink: 0,
};

const separatorStyle: React.CSSProperties = {
  width: 1,
  alignSelf: 'stretch',
  background: ZINC_500,
  marginTop: 4,
  marginBottom: 4,
};

function SegmentButton({ label, active, onClick }: { label: string; active: boolean; onClick?: () => void }) {
  return (
    <span
      onClick={onClick}
      style={{
        fontSize: FONT_SIZE_SMALL,
        padding: '1px 6px',
        background: active ? ZINC_600 : 'transparent',
        color: active ? TEXT_STRONG : PANE_HEADER_FOREGROUND_COLOR,
        cursor: 'pointer',
        userSelect: 'none',
      }}
    >
      {label}
    </span>
  );
}

function ToggleButton({ label, active, onClick }: { label: string; active: boolean; onClick: () => void }) {
  return (
    <span
      onClick={onClick}
      style={{
        fontSize: FONT_SIZE_SMALL,
        padding: '1px 4px',
        color: active ? TEXT_STRONG : TEXT_DISABLED,
        cursor: 'pointer',
        userSelect: 'none',
      }}
    >
      {label}
    </span>
  );
}

const zoomBtnStyle: React.CSSProperties = {
  width: 22,
  height: 22,
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'center',
  background: 'transparent',
  border: 'none',
  color: TEXT_DISABLED,
  cursor: 'pointer',
  fontSize: 14,
  fontFamily: 'inherit',
  flexShrink: 0,
};

function ZoomControl({ zoom, onZoom }: { zoom: number; onZoom: (action: 'in' | 'out' | 'reset') => void }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', background: ZINC_600, height: 20, flexShrink: 0 }}>
      <button onClick={() => onZoom('out')} style={zoomBtnStyle}
        onMouseEnter={(e) => { e.currentTarget.style.color = TEXT_STRONG; }}
        onMouseLeave={(e) => { e.currentTarget.style.color = TEXT_DISABLED; }}
      >−</button>
      <span
        onClick={() => onZoom('reset')}
        style={{ width: 48, textAlign: 'center', fontSize: FONT_SIZE_SMALL, color: TEXT_DISABLED, cursor: 'pointer', userSelect: 'none' }}
      >
        {Math.round(zoom * 100)}%
      </span>
      <button onClick={() => onZoom('in')} style={zoomBtnStyle}
        onMouseEnter={(e) => { e.currentTarget.style.color = TEXT_STRONG; }}
        onMouseLeave={(e) => { e.currentTarget.style.color = TEXT_DISABLED; }}
      >+</button>
    </div>
  );
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
    <div style={headerStyle}>
      <span style={{ width: LABEL_WIDTH - 16, fontSize: 10, letterSpacing: '0.05em', textTransform: 'uppercase' }}>
        Viewer
      </span>
      <div style={separatorStyle} />
      <SegmentButton label="Visual" active={viewerMode === 'visual'} onClick={() => setViewerMode('visual')} />
      <SegmentButton label="Data" active={viewerMode === 'data'} onClick={() => setViewerMode('data')} />
      <div style={{ width: 8 }} />
      <ToggleButton label="Handles" active={showHandles} onClick={toggleHandles} />
      <ToggleButton label="Points" active={showPoints} onClick={togglePoints} />
      <ToggleButton label="Pt#" active={showPointNumbers} onClick={togglePointNumbers} />
      <ToggleButton label="Origin" active={showOrigin} onClick={toggleOrigin} />
      <ToggleButton label="Canvas" active={showCanvasBorder} onClick={toggleCanvasBorder} />
      <div style={{ flex: 1 }} />
      <ZoomControl zoom={viewerZoom} onZoom={requestViewerZoom} />
    </div>
  );
}

function ParametersHeader() {
  const activeNode = useStore((s) => s.activeNode);
  const library = useStore((s) => s.library);
  const node = activeNode && library ? library.root.children.find((n: any) => n.name === activeNode) : null;

  return (
    <div style={headerStyle}>
      <span style={{ width: LABEL_WIDTH - 16, fontSize: 10, letterSpacing: '0.05em', textTransform: 'uppercase' }}>
        Parameters
      </span>
      <div style={separatorStyle} />
      <span style={{ marginLeft: 4, fontSize: FONT_SIZE_SMALL }}>
        {node ? node.name : 'Document'}
      </span>
      <div style={{ flex: 1 }} />
      {node?.prototype && (
        <span style={{ fontSize: FONT_SIZE_SMALL, color: TEXT_DISABLED }}>{node.prototype}</span>
      )}
    </div>
  );
}

function NetworkHeader() {
  const setNodeSelectionDialogOpen = useStore((s) => s.setNodeSelectionDialogOpen);

  return (
    <div style={headerStyle}>
      <span style={{ width: LABEL_WIDTH - 16, fontSize: 10, letterSpacing: '0.05em', textTransform: 'uppercase' }}>
        Network
      </span>
      <div style={separatorStyle} />
      <div style={{ flex: 1 }} />
      <button
        onClick={() => setNodeSelectionDialogOpen(true)}
        style={{ background: 'transparent', border: 'none', color: PANE_HEADER_FOREGROUND_COLOR, fontSize: FONT_SIZE_SMALL, cursor: 'pointer', padding: '0 4px' }}
      >
        + New Node
      </button>
    </div>
  );
}

function HorizontalSplitter({ onDrag }: { onDrag: (deltaY: number) => void }) {
  const dragging = useRef(false);
  const lastY = useRef(0);
  return (
    <div
      style={{ height: SPLITTER_THICKNESS, background: PANEL_BG, position: 'relative', cursor: 'row-resize', flexShrink: 0 }}
      onPointerDown={(e) => { dragging.current = true; lastY.current = e.clientY; (e.target as HTMLElement).setPointerCapture(e.pointerId); }}
      onPointerMove={(e) => { if (!dragging.current) return; onDrag(e.clientY - lastY.current); lastY.current = e.clientY; }}
      onPointerUp={() => { dragging.current = false; }}
    >
      <div style={{ position: 'absolute', left: 0, right: 0, top: -(SPLITTER_AFFORDANCE - SPLITTER_THICKNESS) / 2, bottom: -(SPLITTER_AFFORDANCE - SPLITTER_THICKNESS) / 2, cursor: 'row-resize' }} />
    </div>
  );
}

function VerticalSplitter({ onDrag }: { onDrag: (deltaX: number) => void }) {
  const dragging = useRef(false);
  const lastX = useRef(0);
  return (
    <div
      style={{ width: SPLITTER_THICKNESS, background: PANEL_BG, position: 'relative', cursor: 'col-resize', flexShrink: 0 }}
      onPointerDown={(e) => { dragging.current = true; lastX.current = e.clientX; (e.target as HTMLElement).setPointerCapture(e.pointerId); }}
      onPointerMove={(e) => { if (!dragging.current) return; onDrag(e.clientX - lastX.current); lastX.current = e.clientX; }}
      onPointerUp={() => { dragging.current = false; }}
    >
      <div style={{ position: 'absolute', top: 0, bottom: 0, left: -(SPLITTER_AFFORDANCE - SPLITTER_THICKNESS) / 2, right: -(SPLITTER_AFFORDANCE - SPLITTER_THICKNESS) / 2, cursor: 'col-resize' }} />
    </div>
  );
}

function ViewerContent() {
  const viewerMode = useStore((s) => s.viewerMode);
  return (
    <div style={{ width: '100%', height: '100%', position: 'relative' }}>
      <div style={{
        position: 'absolute', inset: 0,
        visibility: viewerMode === 'visual' ? 'visible' : 'hidden',
        pointerEvents: viewerMode === 'visual' ? 'auto' : 'none',
      }}>
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
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', background: PANEL_BG, fontFamily: "-apple-system, BlinkMacSystemFont, 'Segoe UI', system-ui, sans-serif", fontSize: 13, color: '#f4f4f5', WebkitFontSmoothing: 'antialiased' }}>
      <AddressBar />
      <NotificationBanner />
      <div style={{ display: 'flex', flex: 1, minHeight: 0 }}>
        {/* LEFT: Viewer */}
        <div style={{ display: 'flex', flexDirection: 'column', flex: 1, minWidth: 0 }}>
          <ViewerHeader />
          <div style={{ flex: 1, minHeight: 0 }}>
            <ViewerContent />
          </div>
        </div>

        <VerticalSplitter onDrag={onVerticalDrag} />

        {/* RIGHT: Parameters (top) + Network (bottom) */}
        <div ref={rightPanelRef} style={{ display: 'flex', flexDirection: 'column', width: rightPanelWidth, minWidth: 0 }}>
          <div style={{ flex: rightPanelSplit, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
            <ParametersHeader />
            <div style={{ flex: 1, minHeight: 0, overflow: 'auto' }}>
              <ParameterPanel />
            </div>
          </div>

          <HorizontalSplitter onDrag={onHorizontalDrag} />

          <div style={{ flex: 1 - rightPanelSplit, minHeight: 0, display: 'flex', flexDirection: 'column' }}>
            <NetworkHeader />
            <div style={{ flex: 1, minHeight: 0 }}>
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
