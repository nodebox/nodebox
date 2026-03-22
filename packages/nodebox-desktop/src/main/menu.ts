import { Menu, type BrowserWindow, type MenuItemConstructorOptions } from 'electron';

export function createMenu(win: BrowserWindow): Menu {
  const template: MenuItemConstructorOptions[] = [
    {
      label: 'File',
      submenu: [
        { label: 'New', accelerator: 'CmdOrCtrl+N', click: () => win.webContents.send('menu:new') },
        { label: 'Open...', accelerator: 'CmdOrCtrl+O', click: () => win.webContents.send('menu:open') },
        { type: 'separator' },
        { label: 'Save', accelerator: 'CmdOrCtrl+S', click: () => win.webContents.send('menu:save') },
        { label: 'Save As...', accelerator: 'CmdOrCtrl+Shift+S', click: () => win.webContents.send('menu:save-as') },
        { type: 'separator' },
        { label: 'Export SVG...', click: () => win.webContents.send('menu:export-svg') },
        { label: 'Export PNG...', click: () => win.webContents.send('menu:export-png') },
        { type: 'separator' },
        { role: 'quit' },
      ],
    },
    {
      label: 'Edit',
      submenu: [
        { label: 'Undo', accelerator: 'CmdOrCtrl+Z', click: () => win.webContents.send('menu:undo') },
        { label: 'Redo', accelerator: 'CmdOrCtrl+Shift+Z', click: () => win.webContents.send('menu:redo') },
        { type: 'separator' },
        { role: 'cut' },
        { role: 'copy' },
        { role: 'paste' },
        { label: 'Delete', accelerator: 'Backspace', click: () => win.webContents.send('menu:delete') },
      ],
    },
    {
      label: 'View',
      submenu: [
        { label: 'Show Points', type: 'checkbox', click: () => win.webContents.send('menu:toggle-points') },
        { label: 'Show Origin', type: 'checkbox', checked: true, click: () => win.webContents.send('menu:toggle-origin') },
        { label: 'Show Bounds', type: 'checkbox', click: () => win.webContents.send('menu:toggle-bounds') },
        { type: 'separator' },
        { role: 'toggleDevTools' },
        { role: 'reload' },
      ],
    },
  ];

  // macOS app menu
  if (process.platform === 'darwin') {
    template.unshift({
      label: 'NodeBox',
      submenu: [
        { role: 'about' },
        { type: 'separator' },
        { role: 'hide' },
        { role: 'hideOthers' },
        { type: 'separator' },
        { role: 'quit' },
      ],
    });
  }

  return Menu.buildFromTemplate(template);
}
