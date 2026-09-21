// The Electron main process: starts the local server, opens the editor window and wires the
// File menu to NodeBox documents on disk.

import { BrowserWindow, Menu, MenuItemConstructorOptions, app, dialog, ipcMain, shell } from "electron";
import path from "node:path";
import { existsSync } from "node:fs";
import { LOCAL_USER_ID, LocalServer, startLocalServer } from "./local-server";
import { openDocument, saveDocument } from "./documents";
import type { LiveProject } from "@ndbx/core";

let server: LocalServer | null = null;
let mainWindow: BrowserWindow | null = null;
const pendingFiles: string[] = [];

function resourcePath(...segments: string[]): string {
  // Packaged: next to the app in resources/; in development: the workspace checkout.
  const packaged = path.join(process.resourcesPath ?? "", ...segments);
  if (app.isPackaged && existsSync(packaged)) return packaged;
  return path.join(__dirname, "..", "..", "..", ...segments);
}

async function ensureServer(): Promise<LocalServer> {
  if (server) return server;
  const webDist = app.isPackaged ? path.join(process.resourcesPath, "web") : resourcePath("packages", "web", "dist");
  const guide = resourcePath("doc", "guide");
  server = await startLocalServer({
    dataDir: path.join(app.getPath("userData"), "nodebox"),
    staticDirs: [webDist, path.join(app.getPath("userData"), "nodebox", "static")],
    seedDir: app.isPackaged ? path.join(process.resourcesPath, "data") : resourcePath("packages", "server", "data"),
  });
  // The guide is rendered by the worker from /_guide/<slug>.md; expose it through a static directory.
  void guide;
  return server;
}

async function createWindow(): Promise<BrowserWindow> {
  const local = await ensureServer();
  const window = new BrowserWindow({
    width: 1400,
    height: 900,
    title: "NodeBox",
    webPreferences: {
      preload: path.join(__dirname, "preload.cjs"),
      additionalArguments: [`--nodebox-session=${JSON.stringify({ token: local.token, userId: LOCAL_USER_ID })}`],
      contextIsolation: true,
      sandbox: false,
    },
  });
  window.on("closed", () => {
    if (mainWindow === window) mainWindow = null;
  });
  window.webContents.setWindowOpenHandler(({ url }) => {
    if (!url.startsWith(local.url)) void shell.openExternal(url);
    return { action: "deny" };
  });
  window.webContents.on("did-fail-load", (_event, code, description, url) => {
    console.error(`NodeBox could not load ${url}: ${description} (${code})`);
  });
  console.log(`NodeBox local server at ${local.url}`);
  await window.loadURL(`${local.url}/${LOCAL_USER_ID}`);
  mainWindow = window;
  return window;
}

async function importFile(file: string): Promise<string | null> {
  const local = await ensureServer();
  try {
    const { project, warnings } = await openDocument(file);
    const projectId = project.id!;
    const exists = await local.store.projectExists(LOCAL_USER_ID, projectId);
    if (!exists) await local.store.createProject(LOCAL_USER_ID, projectId, project.title, "private");
    project.scope = "private";
    await local.store.saveProject(LOCAL_USER_ID, projectId, project);
    if (!exists) await local.store.updateProjectTitleInProfile(LOCAL_USER_ID, projectId, project.title);
    if (warnings.length > 0) {
      dialog.showMessageBox({ type: "warning", message: `${path.basename(file)} opened with warnings`, detail: warnings.slice(0, 20).join("\n") });
    }
    return projectId;
  } catch (e) {
    dialog.showErrorBox("Could not open document", e instanceof Error ? e.message : String(e));
    return null;
  }
}

async function openFromDialog(): Promise<void> {
  const result = await dialog.showOpenDialog({
    title: "Open NodeBox Document",
    filters: [
      { name: "NodeBox Documents", extensions: ["ndbx", "json"] },
      { name: "All Files", extensions: ["*"] },
    ],
    properties: ["openFile"],
  });
  if (result.canceled || result.filePaths.length === 0) return;
  const projectId = await importFile(result.filePaths[0]);
  if (projectId) await showProject(projectId);
}

async function showProject(projectId: string): Promise<void> {
  const local = await ensureServer();
  const window = mainWindow ?? (await createWindow());
  await window.loadURL(`${local.url}/${LOCAL_USER_ID}/${projectId}`);
}

async function saveAs(projectId: string): Promise<void> {
  const local = await ensureServer();
  const project = (await local.store.getUserProject(LOCAL_USER_ID, projectId)) as LiveProject;
  const result = await dialog.showSaveDialog({
    title: "Save NodeBox Document",
    defaultPath: `${project.title || projectId}.ndbx`,
    filters: [
      { name: "NodeBox 3 Document", extensions: ["ndbx"] },
      { name: "NodeBox Live Project", extensions: ["json"] },
    ],
  });
  if (result.canceled || !result.filePath) return;
  try {
    await saveDocument(project, result.filePath);
  } catch (e) {
    dialog.showErrorBox("Could not save document", e instanceof Error ? e.message : String(e));
  }
}

function currentProjectId(): string | null {
  const url = mainWindow?.webContents.getURL();
  if (!url) return null;
  const parts = new URL(url).pathname.split("/").filter((p) => p.length > 0);
  return parts.length >= 2 && parts[0] === LOCAL_USER_ID ? parts[1] : null;
}

function buildMenu(): void {
  const isMac = process.platform === "darwin";
  const template: MenuItemConstructorOptions[] = [
    ...(isMac ? [{ role: "appMenu" as const }] : []),
    {
      label: "File",
      submenu: [
        { label: "New Project", accelerator: "CmdOrCtrl+N", click: () => void showProjectList("/create") },
        { label: "Open…", accelerator: "CmdOrCtrl+O", click: () => void openFromDialog() },
        { type: "separator" },
        {
          label: "Save As…",
          accelerator: "CmdOrCtrl+Shift+S",
          click: () => {
            const id = currentProjectId();
            if (id) void saveAs(id);
          },
        },
        { type: "separator" },
        { label: "Projects", accelerator: "CmdOrCtrl+Shift+O", click: () => void showProjectList() },
        { type: "separator" },
        isMac ? { role: "close" } : { role: "quit" },
      ],
    },
    { role: "editMenu" },
    { role: "viewMenu" },
    { role: "windowMenu" },
    {
      role: "help",
      submenu: [
        { label: "NodeBox Guide", click: () => void showProjectList("/guide/welcome") },
        { label: "nodebox.net", click: () => void shell.openExternal("https://nodebox.net/") },
      ],
    },
  ];
  Menu.setApplicationMenu(Menu.buildFromTemplate(template));
}

async function showProjectList(route = `/${LOCAL_USER_ID}`): Promise<void> {
  const local = await ensureServer();
  const window = mainWindow ?? (await createWindow());
  await window.loadURL(`${local.url}${route}`);
}

ipcMain.handle("nodebox:open-document", () => openFromDialog());
ipcMain.handle("nodebox:save-document-as", (_event, projectId: string) => saveAs(projectId));

app.on("open-file", (event, file) => {
  event.preventDefault();
  if (app.isReady()) {
    void importFile(file).then((id) => {
      if (id) void showProject(id);
    });
  }
  else pendingFiles.push(file);
});

app.whenReady().then(async () => {
  buildMenu();
  await createWindow();
  const files = [...pendingFiles, ...process.argv.slice(1).filter((a) => /\.(ndbx|json)$/i.test(a) && existsSync(a))];
  for (const file of files) {
    const id = await importFile(file);
    if (id) await showProject(id);
  }
  app.on("activate", () => {
    if (BrowserWindow.getAllWindows().length === 0) void createWindow();
  });
});

app.on("window-all-closed", () => {
  if (process.platform !== "darwin") app.quit();
});

app.on("will-quit", () => {
  void server?.close();
});
