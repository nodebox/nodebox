import { app, BrowserWindow, ipcMain, dialog, Menu } from 'electron';
import { readFile, writeFile } from 'fs/promises';
import { join, relative, isAbsolute } from 'path';
import { IPC } from '../shared/ipc-channels';
import { createMenu } from './menu';
import { listFonts, getFontBytes } from './fonts';

// Use app.getAppPath() for reliable path resolution in both dev and production
const appPath = app.getAppPath();

let mainWindow: BrowserWindow | null = null;
let currentFilePath: string | null = null;

function createWindow() {
  const isTest = !!process.env.NODEBOX_E2E;
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 800,
    minWidth: 800,
    minHeight: 500,
    show: !isTest,
    webPreferences: {
      preload: join(appPath, 'dist-electron/preload/index.mjs'),
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: false,
    },
    backgroundColor: '#27272a', // ZINC_800
    titleBarStyle: 'hiddenInset',
    trafficLightPosition: { x: 12, y: 10 },
  });

  const menu = createMenu(mainWindow);
  Menu.setApplicationMenu(menu);

  if (process.env.VITE_DEV_SERVER_URL) {
    mainWindow.loadURL(process.env.VITE_DEV_SERVER_URL);
  } else {
    mainWindow.loadFile(join(appPath, 'dist/index.html'));
  }
}

// IPC handlers
ipcMain.handle(IPC.FILE_NEW, () => {
  currentFilePath = null;
  return null;
});

ipcMain.handle(IPC.FILE_OPEN, async () => {
  if (!mainWindow) return null;
  const { canceled, filePaths } = await dialog.showOpenDialog(mainWindow, {
    filters: [{ name: 'NodeBox Files', extensions: ['ndbx'] }],
    properties: ['openFile'],
  });
  if (canceled || filePaths.length === 0) return null;
  currentFilePath = filePaths[0];
  const content = await readFile(currentFilePath, 'utf-8');
  return { path: currentFilePath, content };
});

ipcMain.handle(IPC.FILE_SAVE, async (_event, data: string) => {
  if (!currentFilePath) {
    if (!mainWindow) return null;
    const { canceled, filePath } = await dialog.showSaveDialog(mainWindow, {
      filters: [{ name: 'NodeBox Files', extensions: ['ndbx'] }],
    });
    if (canceled || !filePath) return null;
    currentFilePath = filePath;
  }
  await writeFile(currentFilePath, data, 'utf-8');
  return { path: currentFilePath };
});

ipcMain.handle(IPC.FILE_SAVE_AS, async (_event, data: string) => {
  if (!mainWindow) return null;
  const { canceled, filePath } = await dialog.showSaveDialog(mainWindow, {
    filters: [{ name: 'NodeBox Files', extensions: ['ndbx'] }],
  });
  if (canceled || !filePath) return null;
  currentFilePath = filePath;
  await writeFile(currentFilePath, data, 'utf-8');
  return { path: currentFilePath };
});

ipcMain.handle(IPC.EXPORT_SVG, async (_event, data: string) => {
  if (!mainWindow) return null;
  const { canceled, filePath } = await dialog.showSaveDialog(mainWindow, {
    filters: [{ name: 'SVG Files', extensions: ['svg'] }],
  });
  if (canceled || !filePath) return null;
  await writeFile(filePath, data, 'utf-8');
  return { path: filePath };
});

ipcMain.handle(IPC.EXPORT_PNG, async (_event, data: Uint8Array) => {
  if (!mainWindow) return null;
  const { canceled, filePath } = await dialog.showSaveDialog(mainWindow, {
    filters: [{ name: 'PNG Files', extensions: ['png'] }],
  });
  if (canceled || !filePath) return null;
  await writeFile(filePath, Buffer.from(data));
  return { path: filePath };
});

ipcMain.handle(IPC.FONT_LIST, async () => {
  return listFonts();
});

ipcMain.handle(IPC.FONT_BYTES, async (_event, name: string) => {
  return getFontBytes(name);
});

ipcMain.handle(IPC.ASSET_OPEN, async (_event, { filters, projectDir }: { filters: { name: string; extensions: string[] }[]; projectDir: string }) => {
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
  if (relativePath.startsWith('..') || isAbsolute(relativePath)) {
    return { error: 'sandbox' };
  }
  return { path: relativePath };
});

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') {
    app.quit();
  }
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) {
    createWindow();
  }
});
