import { app, BrowserWindow, Menu, MenuItemConstructorOptions } from 'electron';
import { IPC } from '../shared/ipc-channels';

function sendAction(win: BrowserWindow | null, action: string) {
  if (win) {
    win.webContents.send(IPC.MENU_ACTION, action);
  }
}

export function createMenu(win: BrowserWindow): Menu {
  const isMac = process.platform === 'darwin';

  const template: MenuItemConstructorOptions[] = [
    ...(isMac
      ? [
          {
            label: app.name,
            submenu: [
              { role: 'about' as const },
              { type: 'separator' as const },
              { role: 'services' as const },
              { type: 'separator' as const },
              { role: 'hide' as const },
              { role: 'hideOthers' as const },
              { role: 'unhide' as const },
              { type: 'separator' as const },
              { role: 'quit' as const },
            ],
          },
        ]
      : []),
    {
      label: 'File',
      submenu: [
        {
          label: 'New',
          accelerator: 'CmdOrCtrl+N',
          click: () => sendAction(win, 'file:new'),
        },
        {
          label: 'Open...',
          accelerator: 'CmdOrCtrl+O',
          click: () => sendAction(win, 'file:open'),
        },
        {
          label: 'Open Recent',
          role: 'recentDocuments' as MenuItemConstructorOptions['role'],
          submenu: [
            {
              label: 'Clear Recent',
              role: 'clearRecentDocuments' as MenuItemConstructorOptions['role'],
            },
          ],
        },
        { type: 'separator' },
        {
          label: 'Save',
          accelerator: 'CmdOrCtrl+S',
          click: () => sendAction(win, 'file:save'),
        },
        {
          label: 'Save As...',
          accelerator: 'CmdOrCtrl+Shift+S',
          click: () => sendAction(win, 'file:save-as'),
        },
        { type: 'separator' },
        {
          label: 'Export SVG...',
          click: () => sendAction(win, 'export:svg'),
        },
        {
          label: 'Export PNG...',
          click: () => sendAction(win, 'export:png'),
        },
        { type: 'separator' },
        isMac ? { role: 'close' } : { role: 'quit' },
      ],
    },
    {
      label: 'Edit',
      submenu: [
        {
          label: 'Undo',
          accelerator: 'CmdOrCtrl+Z',
          click: () => sendAction(win, 'edit:undo'),
        },
        {
          label: 'Redo',
          accelerator: 'CmdOrCtrl+Shift+Z',
          click: () => sendAction(win, 'edit:redo'),
        },
        { type: 'separator' },
        {
          label: 'Delete',
          accelerator: 'Backspace',
          click: () => sendAction(win, 'edit:delete'),
        },
      ],
    },
    {
      label: 'View',
      submenu: [
        {
          label: 'Zoom In',
          accelerator: 'CmdOrCtrl+=',
          click: () => sendAction(win, 'view:zoom-in'),
        },
        {
          label: 'Zoom Out',
          accelerator: 'CmdOrCtrl+-',
          click: () => sendAction(win, 'view:zoom-out'),
        },
        {
          label: 'Fit',
          accelerator: 'CmdOrCtrl+0',
          click: () => sendAction(win, 'view:fit'),
        },
        { type: 'separator' },
        {
          label: 'Show Handles',
          type: 'checkbox',
          checked: true,
          click: () => sendAction(win, 'view:toggle-handles'),
        },
        {
          label: 'Show Points',
          type: 'checkbox',
          checked: false,
          click: () => sendAction(win, 'view:toggle-points'),
        },
        {
          label: 'Show Origin',
          type: 'checkbox',
          checked: true,
          click: () => sendAction(win, 'view:toggle-origin'),
        },
        {
          label: 'Show Canvas Border',
          type: 'checkbox',
          checked: true,
          click: () => sendAction(win, 'view:toggle-canvas-border'),
        },
        { type: 'separator' },
        { role: 'toggleDevTools' },
      ],
    },
    {
      label: 'Help',
      submenu: [
        {
          label: 'About NodeBox',
          click: () => sendAction(win, 'help:about'),
        },
      ],
    },
  ];

  return Menu.buildFromTemplate(template);
}
