// IPC channel name constants shared between main and renderer processes.

export const IPC = {
  FILE_NEW: 'file:new',
  FILE_OPEN: 'file:open',
  FILE_SAVE: 'file:save',
  FILE_SAVE_AS: 'file:save-as',
  EXPORT_SVG: 'export:svg',
  EXPORT_PNG: 'export:png',
  FONT_LIST: 'font:list',
  FONT_BYTES: 'font:bytes',
  MENU_ACTION: 'menu:action',
} as const;

export type MenuAction =
  | 'file:new'
  | 'file:open'
  | 'file:save'
  | 'file:save-as'
  | 'export:svg'
  | 'export:png'
  | 'edit:undo'
  | 'edit:redo'
  | 'edit:delete'
  | 'view:zoom-in'
  | 'view:zoom-out'
  | 'view:fit'
  | 'view:toggle-handles'
  | 'view:toggle-points'
  | 'view:toggle-origin'
  | 'view:toggle-canvas-border'
  | 'help:about';
