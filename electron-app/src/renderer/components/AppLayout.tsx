import { useCallback, useRef, useState } from 'react';
import { useStore } from '../state/store';
import { AddressBar } from './AddressBar';
import { AnimationBar } from './AnimationBar';
import { NetworkCanvas } from './NetworkCanvas';
import { ViewerCanvas } from './ViewerCanvas';
import { DataViewer } from './DataViewer';
import { ParameterPanel } from './ParameterPanel';
import { NodeSelectionDialog } from './NodeSelectionDialog';
import { AboutDialog } from './AboutDialog';
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

/* ------------------------------------------------------------------ */
/*  Pane headers                                                       */
/* ------------------------------------------------------------------ */

const headerStyle: React.CSSProperties = {
  height: PANE_HEADER_HEIGHT,
  background: PANE_HEADER_BACKGROUND_COLOR,
  color: PANE_HEADER_FOREGROUND_COLOR,
  fontSize: FONT_SIZE_SMALL,
  borderTop: `1px solid ${ZINC_600}`,
  borderBottom: `1px solid ${ZINC_900}`,
};

const separatorStyle: React.CSSProperties = {
  width: 1,
  alignSelf: 'stretch',
  background: ZINC_500,
  marginTop: 4,
  marginBottom: 4,
  marginLeft: 0,
  marginRight: 0,
};

function ViewerHeader() {
  const viewerMode = useStore((s) => s.viewerMode);
  const setViewerMode = useStore((s) => s.setViewerMode);
  const showHandles = useStore((s) => s.showHandles);
  const showPoints = useStore((s) => s.showPoints);
  const showOrigin = useStore((s) => s.showOrigin);
  const showCanvasBorder = useStore((s) => s.showCanvasBorder);
  const toggleHandles = useStore((s) => s.toggleHandles);
  const togglePoints = useStore((s) => s.togglePoints);
  const toggleOrigin = useStore((s) => s.toggleOrigin);
  const toggleCanvasBorder = useStore((s) => s.toggleCanvasBorder);
  const viewerZoom = useStore((s) => s.viewerZoom);
  const requestViewerZoom = useStore((s) => s.requestViewerZoom);

  return (
    <div className="flex items-center px-2 shrink-0 gap-1" style={headerStyle}>
      <span
        className="uppercase"
        style={{ width: LABEL_WIDTH - 16, fontSize: 10, letterSpacing: '0.05em' }}
      >
        Viewer
      </span>
      <div style={separatorStyle} />

      {/* View mode tabs */}
      <SegmentButton label="Visual" active={viewerMode === 'visual'} onClick={() => setViewerMode('visual')} />
      <SegmentButton label="Data" active={viewerMode === 'data'} onClick={() => setViewerMode('data')} />

      <div style={{ width: 8 }} />

      {/* Toggle buttons */}
      <ToggleButton label="Handles" active={showHandles} onClick={toggleHandles} />
      <ToggleButton label="Points" active={showPoints} onClick={togglePoints} />
      <ToggleButton label="Origin" active={showOrigin} onClick={toggleOrigin} />
      <ToggleButton label="Canvas" active={showCanvasBorder} onClick={toggleCanvasBorder} />
      <div className="flex-1" />
      <button
        data-testid="zoom-out"
        onClick={() => requestViewerZoom('out')}
        style={{ background: 'transparent', border: 'none', color: TEXT_DISABLED, cursor: 'pointer', fontSize: FONT_SIZE_SMALL, padding: '0 2px' }}
      >
        -
      </button>
      <span
        data-testid="zoom-level"
        onClick={() => requestViewerZoom('reset')}
        style={{ fontSize: FONT_SIZE_SMALL, color: TEXT_DISABLED, cursor: 'pointer', padding: '0 2px' }}
      >
        {Math.round(viewerZoom * 100)}%
      </span>
      <button
        data-testid="zoom-in"
        onClick={() => requestViewerZoom('in')}
        style={{ background: 'transparent', border: 'none', color: TEXT_DISABLED, cursor: 'pointer', fontSize: FONT_SIZE_SMALL, padding: '0 2px' }}
      >
        +
      </button>
    </div>
  );
}

function ParametersHeader() {
  const activeNode = useStore((s) => s.activeNode);
  const children = useStore((s) => s.library.root.children);
  const node = activeNode ? children.find((n) => n.name === activeNode) : null;

  return (
    <div className="flex items-center px-2 shrink-0 gap-1" style={headerStyle}>
      <span
        className="uppercase"
        style={{ width: LABEL_WIDTH - 16, fontSize: 10, letterSpacing: '0.05em' }}
      >
        Parameters
      </span>
      <div style={separatorStyle} />
      <span style={{ marginLeft: 4, fontSize: FONT_SIZE_SMALL }}>
        {node ? node.name : 'Document'}
      </span>
      <div className="flex-1" />
      {node?.prototype && (
        <span style={{ fontSize: FONT_SIZE_SMALL, color: TEXT_DISABLED }}>
          {node.prototype}
        </span>
      )}
    </div>
  );
}

function NetworkHeader() {
  const setNodeDialogVisible = useStore((s) => s.setNodeDialogVisible);

  return (
    <div className="flex items-center px-2 shrink-0 gap-1" style={headerStyle}>
      <span
        className="uppercase"
        style={{ width: LABEL_WIDTH - 16, fontSize: 10, letterSpacing: '0.05em' }}
      >
        Network
      </span>
      <div style={separatorStyle} />
      <div className="flex-1" />
      <button
        onClick={() => setNodeDialogVisible(true)}
        style={{
          background: 'transparent',
          border: 'none',
          color: PANE_HEADER_FOREGROUND_COLOR,
          fontSize: FONT_SIZE_SMALL,
          cursor: 'pointer',
          padding: '0 4px',
        }}
      >
        + New Node
      </button>
    </div>
  );
}

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

function ToggleButton({
  label,
  active,
  onClick,
}: {
  label: string;
  active: boolean;
  onClick: () => void;
}) {
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

/* ------------------------------------------------------------------ */
/*  Splitters                                                          */
/* ------------------------------------------------------------------ */

function HorizontalSplitter({
  onDrag,
}: {
  onDrag: (deltaY: number) => void;
}) {
  const dragging = useRef(false);
  const lastY = useRef(0);

  const onPointerDown = useCallback(
    (e: React.PointerEvent) => {
      dragging.current = true;
      lastY.current = e.clientY;
      (e.target as HTMLElement).setPointerCapture(e.pointerId);
    },
    [],
  );

  const onPointerMove = useCallback(
    (e: React.PointerEvent) => {
      if (!dragging.current) return;
      const delta = e.clientY - lastY.current;
      lastY.current = e.clientY;
      onDrag(delta);
    },
    [onDrag],
  );

  const onPointerUp = useCallback(() => {
    dragging.current = false;
  }, []);

  return (
    <div
      data-testid="horizontal-splitter"
      className="cursor-row-resize shrink-0"
      style={{
        height: SPLITTER_THICKNESS,
        background: PANEL_BG,
        position: 'relative',
      }}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
    >
      {/* Invisible hit area extending above and below the 2px line */}
      <div style={{
        position: 'absolute',
        left: 0,
        right: 0,
        top: -(SPLITTER_AFFORDANCE - SPLITTER_THICKNESS) / 2,
        bottom: -(SPLITTER_AFFORDANCE - SPLITTER_THICKNESS) / 2,
        cursor: 'row-resize',
      }} />
    </div>
  );
}

function VerticalSplitter({
  onDrag,
}: {
  onDrag: (deltaX: number) => void;
}) {
  const dragging = useRef(false);
  const lastX = useRef(0);

  const onPointerDown = useCallback(
    (e: React.PointerEvent) => {
      dragging.current = true;
      lastX.current = e.clientX;
      (e.target as HTMLElement).setPointerCapture(e.pointerId);
    },
    [],
  );

  const onPointerMove = useCallback(
    (e: React.PointerEvent) => {
      if (!dragging.current) return;
      const delta = e.clientX - lastX.current;
      lastX.current = e.clientX;
      onDrag(delta);
    },
    [onDrag],
  );

  const onPointerUp = useCallback(() => {
    dragging.current = false;
  }, []);

  return (
    <div
      data-testid="vertical-splitter"
      className="cursor-col-resize shrink-0"
      style={{
        width: SPLITTER_THICKNESS,
        background: PANEL_BG,
        position: 'relative',
      }}
      onPointerDown={onPointerDown}
      onPointerMove={onPointerMove}
      onPointerUp={onPointerUp}
    >
      {/* Invisible hit area extending left and right of the 2px line */}
      <div style={{
        position: 'absolute',
        top: 0,
        bottom: 0,
        left: -(SPLITTER_AFFORDANCE - SPLITTER_THICKNESS) / 2,
        right: -(SPLITTER_AFFORDANCE - SPLITTER_THICKNESS) / 2,
        cursor: 'col-resize',
      }} />
    </div>
  );
}

/* ------------------------------------------------------------------ */
/*  Main layout                                                        */
/* ------------------------------------------------------------------ */

function ViewerContent() {
  const viewerMode = useStore((s) => s.viewerMode);
  if (viewerMode === 'data') {
    return <DataViewer />;
  }
  return <ViewerCanvas />;
}

export function AppLayout() {
  const [rightPanelSplit, setRightPanelSplit] = useState(0.35);
  const rightPanelWidth = useStore((s) => s.parameterPanelWidth);
  const setRightPanelWidth = useStore((s) => s.setParameterPanelWidth);
  const rightPanelRef = useRef<HTMLDivElement>(null);

  const onHorizontalDrag = useCallback((deltaY: number) => {
    if (!rightPanelRef.current) return;
    const totalHeight = rightPanelRef.current.clientHeight;
    setRightPanelSplit((prev) =>
      Math.max(0.15, Math.min(0.85, prev + deltaY / totalHeight)),
    );
  }, []);

  const onVerticalDrag = useCallback(
    (deltaX: number) => {
      setRightPanelWidth(
        Math.max(300, Math.min(600, rightPanelWidth - deltaX)),
      );
    },
    [rightPanelWidth, setRightPanelWidth],
  );

  return (
    <div className="flex flex-col h-screen" style={{ background: PANEL_BG }}>
      <AddressBar />
      <div className="flex flex-1" style={{ minHeight: 0 }}>
        {/* LEFT: Viewer (fills remaining space) */}
        <div className="flex flex-col flex-1" style={{ minWidth: 0 }}>
          <ViewerHeader />
          <div className="flex-1" style={{ minHeight: 0 }}>
            <ViewerContent />
          </div>
        </div>

        <VerticalSplitter onDrag={onVerticalDrag} />

        {/* RIGHT: Parameters (top) + Network (bottom) */}
        <div
          ref={rightPanelRef}
          className="flex flex-col"
          style={{ width: rightPanelWidth, minWidth: 0 }}
        >
          {/* Parameters pane */}
          <div
            style={{ flex: rightPanelSplit, minHeight: 0 }}
            className="flex flex-col"
          >
            <ParametersHeader />
            <div className="flex-1" style={{ minHeight: 0 }}>
              <ParameterPanel />
            </div>
          </div>

          <HorizontalSplitter onDrag={onHorizontalDrag} />

          {/* Network pane */}
          <div
            style={{ flex: 1 - rightPanelSplit, minHeight: 0 }}
            className="flex flex-col"
          >
            <NetworkHeader />
            <div className="flex-1" style={{ minHeight: 0 }}>
              <NetworkCanvas />
            </div>
          </div>
        </div>
      </div>
      <AnimationBar />

      {/* Dialogs */}
      <NodeSelectionDialog />
      <AboutDialog />
    </div>
  );
}
