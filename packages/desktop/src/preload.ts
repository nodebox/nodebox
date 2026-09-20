// The bridge between the editor page and the desktop shell. It signs the page in as the local
// user before the app starts and exposes the few desktop-only calls the editor can use.

import { contextBridge, ipcRenderer } from "electron";

const session = JSON.parse(process.argv.find((a) => a.startsWith("--nodebox-session="))?.slice("--nodebox-session=".length) ?? "{}") as {
  token?: string;
  userId?: string;
};

if (session.token && session.userId) {
  try {
    if (localStorage.getItem("token") !== session.token) {
      localStorage.setItem("token", session.token);
      localStorage.setItem("userId", session.userId);
    }
  } catch {
    // localStorage can be unavailable during the first navigation; main retries via IPC.
  }
}

contextBridge.exposeInMainWorld("nodebox", {
  platform: process.platform,
  isDesktop: true,
  openDocument: () => ipcRenderer.invoke("nodebox:open-document"),
  saveDocumentAs: (projectId: string) => ipcRenderer.invoke("nodebox:save-document-as", projectId),
  onOpenProject: (callback: (projectId: string) => void) => {
    ipcRenderer.on("nodebox:open-project", (_event, projectId: string) => callback(projectId));
  },
});
