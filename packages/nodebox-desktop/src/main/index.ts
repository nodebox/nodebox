import { app, BrowserWindow, ipcMain, dialog, Menu } from 'electron';
import { readFile, writeFile } from 'fs/promises';
import { join, dirname, resolve } from 'path';
import { IPC } from '../shared/ipc-channels.js';
import { createMenu } from './menu.js';

let mainWindow: BrowserWindow | null = null;

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1400,
    height: 900,
    webPreferences: {
      preload: join(__dirname, '../preload/index.js'),
      contextIsolation: true,
      nodeIntegration: false,
    },
    title: 'NodeBox',
  });

  if (process.env.NODE_ENV === 'development') {
    mainWindow.loadURL('http://localhost:5173');
  } else {
    mainWindow.loadFile(join(__dirname, '../../index.html'));
  }

  Menu.setApplicationMenu(createMenu(mainWindow));
}

app.whenReady().then(createWindow);

app.on('window-all-closed', () => {
  if (process.platform !== 'darwin') app.quit();
});

app.on('activate', () => {
  if (BrowserWindow.getAllWindows().length === 0) createWindow();
});

// IPC Handlers
ipcMain.handle(IPC.FILE_OPEN, async () => {
  const result = await dialog.showOpenDialog(mainWindow!, {
    filters: [{ name: 'NodeBox Files', extensions: ['ndbx'] }],
    properties: ['openFile'],
  });
  if (result.canceled || result.filePaths.length === 0) return null;
  const filePath = result.filePaths[0];
  const content = await readFile(filePath, 'utf-8');
  return { filePath, content };
});

ipcMain.handle(IPC.FILE_SAVE, async (_event, { filePath, content }: { filePath: string; content: string }) => {
  await writeFile(filePath, content, 'utf-8');
  return true;
});

ipcMain.handle(IPC.FILE_SAVE_AS, async (_event, { content }: { content: string }) => {
  const result = await dialog.showSaveDialog(mainWindow!, {
    filters: [{ name: 'NodeBox Files', extensions: ['ndbx'] }],
  });
  if (result.canceled || !result.filePath) return null;
  await writeFile(result.filePath, content, 'utf-8');
  return result.filePath;
});

ipcMain.handle(IPC.FILE_READ, async (_event, { basePath, relativePath }: { basePath: string; relativePath: string }) => {
  const fullPath = resolve(dirname(basePath), relativePath);
  // Sandbox check: must be within project dir
  if (!fullPath.startsWith(dirname(basePath))) {
    throw new Error('Sandbox violation');
  }
  return readFile(fullPath, 'utf-8');
});

ipcMain.handle(IPC.LIBRARY_LOAD, async (_event, name: string) => {
  // Load library .ndbx from the bundled libraries directory
  const libDir = join(__dirname, '../../..', 'libraries', name);
  const libFile = join(libDir, `${name}.ndbx`);
  return readFile(libFile, 'utf-8');
});
