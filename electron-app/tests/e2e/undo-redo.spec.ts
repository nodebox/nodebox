import { test, expect } from '@playwright/test';
import { launchApp, sendMenuAction, waitForUpdate, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx.electronApp.close();
});

test('Edit menu has Undo and Redo items', async () => {
  const menuLabels = await ctx.electronApp.evaluate(({ Menu }) => {
    const menu = Menu.getApplicationMenu();
    if (!menu) return [];
    const editMenu = menu.items.find((item) => item.label === 'Edit');
    if (!editMenu || !editMenu.submenu) return [];
    return editMenu.submenu.items.map((item) => item.label);
  });
  expect(menuLabels).toContain('Undo');
  expect(menuLabels).toContain('Redo');
});

test('Undo accelerator is CmdOrCtrl+Z', async () => {
  const accelerator = await ctx.electronApp.evaluate(({ Menu }) => {
    const menu = Menu.getApplicationMenu();
    if (!menu) return null;
    const editMenu = menu.items.find((item) => item.label === 'Edit');
    if (!editMenu || !editMenu.submenu) return null;
    const undoItem = editMenu.submenu.items.find(
      (item) => item.label === 'Undo',
    );
    return undoItem?.accelerator ?? null;
  });
  expect(accelerator).toBe('CmdOrCtrl+Z');
});

test('Redo accelerator is CmdOrCtrl+Shift+Z', async () => {
  const accelerator = await ctx.electronApp.evaluate(({ Menu }) => {
    const menu = Menu.getApplicationMenu();
    if (!menu) return null;
    const editMenu = menu.items.find((item) => item.label === 'Edit');
    if (!editMenu || !editMenu.submenu) return null;
    const redoItem = editMenu.submenu.items.find(
      (item) => item.label === 'Redo',
    );
    return redoItem?.accelerator ?? null;
  });
  expect(accelerator).toBe('CmdOrCtrl+Shift+Z');
});

test('undo action via IPC does not crash', async () => {
  await sendMenuAction(ctx.electronApp, 'edit:undo');
  await waitForUpdate(ctx.window);
  // App should remain functional
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');
});

test('redo action via IPC does not crash', async () => {
  await sendMenuAction(ctx.electronApp, 'edit:redo');
  await waitForUpdate(ctx.window);
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');
});
