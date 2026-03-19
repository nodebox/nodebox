# File Widget File Picker Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add a native file picker to the File widget in the Electron app's parameter panel, matching the Rust desktop app's behavior.

**Architecture:** New IPC channel `asset:open` bridges renderer to main process file dialogs. A `FilePortWidget` component renders filename + "..." button. File filters are determined by node prototype. Paths are stored project-relative with a save-first guard.

**Tech Stack:** Electron IPC, React, Zustand, Node.js `path` module (main process only)

---

### Task 1: Add IPC channel and main process handler

**Files:**
- Modify: `electron-app/src/shared/ipc-channels.ts:3-13`
- Modify: `electron-app/src/main/index.ts:1-3,103-111`

**Step 1: Add `ASSET_OPEN` to IPC channels**

In `electron-app/src/shared/ipc-channels.ts`, add to the `IPC` object:

```typescript
ASSET_OPEN: 'asset:open',
```

**Step 2: Add main process handler**

In `electron-app/src/main/index.ts`, add `relative, dirname` to the `path` import:

```typescript
import { join, relative, dirname } from 'path';
```

Then add the handler after the `FONT_BYTES` handler (after line 111):

```typescript
interface FileFilter {
  name: string;
  extensions: string[];
}

ipcMain.handle(IPC.ASSET_OPEN, async (_event, { filters, projectDir }: { filters: FileFilter[]; projectDir: string }) => {
  if (!mainWindow) return null;
  const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
    defaultPath: projectDir,
    filters: filters.length > 0 ? filters : undefined,
    properties: ['openFile'],
  });
  if (canceled || filePaths.length === 0) return null;
  const absolutePath = filePaths[0];
  const relativePath = relative(projectDir, absolutePath);
  // Reject files outside the project directory (sandbox)
  if (relativePath.startsWith('..') || require('path').isAbsolute(relativePath)) {
    return { error: 'sandbox' };
  }
  return { path: relativePath };
});
```

**Step 3: Build and verify no type errors**

Run: `cd electron-app && npx tsc --noEmit`
Expected: No errors

**Step 4: Commit**

```
git add electron-app/src/shared/ipc-channels.ts electron-app/src/main/index.ts
git commit -m "Add asset:open IPC channel for file widget picker."
```

---

### Task 2: Expose `openAssetFile` in preload and type declarations

**Files:**
- Modify: `electron-app/src/preload/index.ts:4-21`
- Modify: `electron-app/src/renderer/types/electron.d.ts:18-28`

**Step 1: Add to preload bridge**

In `electron-app/src/preload/index.ts`, add after the `exportPng` line (line 11):

```typescript
  openAssetFile: (filters: { name: string; extensions: string[] }[], projectDir: string) =>
    ipcRenderer.invoke(IPC.ASSET_OPEN, { filters, projectDir }),
```

**Step 2: Add type declaration**

In `electron-app/src/renderer/types/electron.d.ts`, add to `ElectronAPI` interface:

```typescript
  openAssetFile(
    filters: { name: string; extensions: string[] }[],
    projectDir: string,
  ): Promise<{ path: string } | { error: string } | null>;
```

**Step 3: Build and verify no type errors**

Run: `cd electron-app && npx tsc --noEmit`
Expected: No errors

**Step 4: Commit**

```
git add electron-app/src/preload/index.ts electron-app/src/renderer/types/electron.d.ts
git commit -m "Expose openAssetFile in preload bridge and type declarations."
```

---

### Task 3: Add FilePortWidget component and wire into PortWidget dispatch

**Files:**
- Modify: `electron-app/src/renderer/components/ParameterPanel.tsx`

**Step 1: Add the file filter helper function**

Add after the existing imports (around line 11):

```typescript
function getFileFilters(prototype: string | null): { name: string; extensions: string[] }[] {
  switch (prototype) {
    case 'data.import_csv':
      return [{ name: 'CSV Files', extensions: ['csv', 'tsv'] }];
    case 'data.import_text':
      return [{ name: 'Text Files', extensions: ['txt', 'text', 'csv', 'tsv', 'log'] }];
    case 'corevector.import_svg':
      return [{ name: 'SVG Files', extensions: ['svg'] }];
    default:
      return [];
  }
}
```

**Step 2: Add the FilePortWidget component**

Add after the `StringPortWidget` component (after line 293):

```typescript
function FilePortWidget({
  port,
  nodeName,
  nodePrototype,
}: {
  port: Port;
  nodeName: string;
  nodePrototype: string | null;
}) {
  const setPortValue = useStore((s) => s.setPortValue);
  const filePath = useStore((s) => s.filePath);
  const strValue = getString(port.value);

  // Extract just the filename for display
  const displayText = strValue
    ? strValue.split('/').pop() || strValue
    : '(none)';

  const handleClick = useCallback(async () => {
    if (!window.electronAPI) return;
    if (!filePath) {
      alert('Please save your project before importing files.');
      return;
    }
    const projectDir = filePath.replace(/[/\\][^/\\]*$/, '');
    const filters = getFileFilters(nodePrototype);
    const result = await window.electronAPI.openAssetFile(filters, projectDir);
    if (!result) return; // User cancelled
    if ('error' in result) {
      alert('Please copy the file to your project folder first.');
      return;
    }
    setPortValue(nodeName, port.name, { String: result.path });
  }, [filePath, nodePrototype, setPortValue, nodeName, port.name]);

  return (
    <div
      className="flex"
      style={{ height: PARAMETER_ROW_HEIGHT }}
      data-testid={`param-row-${port.name}`}
    >
      <div
        className="flex items-center justify-end px-2 shrink-0 text-zinc-300 text-[11px] cursor-default select-none"
        style={{ width: LABEL_WIDTH }}
      >
        {port.label ?? port.name}
      </div>
      <div className="flex-1 flex items-center px-2 py-1">
        <button
          type="button"
          onClick={handleClick}
          className="w-full h-7 flex items-center hover:bg-field-hover rounded-sm cursor-pointer text-left"
          data-testid={`param-value-${port.name}`}
        >
          <span
            className={`flex-1 text-[13px] px-2 truncate ${strValue ? 'text-zinc-100' : 'text-zinc-500'}`}
          >
            {displayText}
          </span>
          <span className="text-zinc-500 text-[13px] px-1 shrink-0">...</span>
        </button>
      </div>
    </div>
  );
}
```

**Step 3: Wire into PortWidget dispatch**

In the `PortWidget` function (around line 481), the `switch` currently handles `Menu` and `Toggle`. Add `File` case and pass `nodePrototype`:

First, add `nodePrototype` prop to `PortWidget`:

```typescript
function PortWidget({ port, nodeName, nodePrototype, isConnected }: { port: Port; nodeName: string; nodePrototype: string | null; isConnected: boolean }) {
```

Then add in the switch block (after `case 'Toggle':`):

```typescript
    case 'File':
      return <FilePortWidget port={port} nodeName={nodeName} nodePrototype={nodePrototype} />;
```

**Step 4: Pass `nodePrototype` from ParameterPanel**

In the `ParameterPanel` component where `PortWidget` is rendered (around line 606), add the prop:

```tsx
<PortWidget
  key={port.name}
  port={port}
  nodeName={node.name}
  nodePrototype={node.prototype}
  isConnected={connections.some(
    (c) => c.input_node === node.name && c.input_port === port.name,
  )}
/>
```

**Step 5: Build and verify no type errors**

Run: `cd electron-app && npx tsc --noEmit`
Expected: No errors

**Step 6: Commit**

```
git add electron-app/src/renderer/components/ParameterPanel.tsx
git commit -m "Add FilePortWidget with native file picker and prototype-based filters."
```

---

### Task 4: Build and run E2E tests

**Step 1: Full build**

Run: `cd electron-app && npm run build`
Expected: Build succeeds with no errors

**Step 2: Run all E2E tests**

Run: `cd electron-app && npx playwright test`
Expected: All existing tests pass (no regressions)

**Step 3: Run unit tests**

Run: `cd electron-app && npm run test`
Expected: All tests pass
