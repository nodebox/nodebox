import React from 'react';
import { ViewerCanvas } from './ViewerCanvas.js';
import { NetworkCanvas } from './NetworkCanvas.js';
import { ParameterPanel } from './ParameterPanel.js';
import { AddressBar } from './AddressBar.js';
import { AnimationBar } from './AnimationBar.js';

export function AppLayout() {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', overflow: 'hidden' }}>
      <AddressBar />
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden' }}>
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
          <div style={{ flex: 1, overflow: 'hidden' }}>
            <ViewerCanvas />
          </div>
          <div style={{ height: 200, borderTop: '1px solid #333', overflow: 'hidden' }}>
            <NetworkCanvas />
          </div>
        </div>
        <div style={{ width: 300, borderLeft: '1px solid #333', overflow: 'auto' }}>
          <ParameterPanel />
        </div>
      </div>
      <AnimationBar />
    </div>
  );
}
