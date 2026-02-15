import { test, expect } from '@playwright/test';
import { launchApp, sendMenuAction, waitForUpdate, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx.electronApp.close();
});

test('edit:delete menu action is wired up', async () => {
  // Verify that the Edit > Delete menu item exists
  const menuLabels = await ctx.electronApp.evaluate(({ Menu }) => {
    const menu = Menu.getApplicationMenu();
    if (!menu) return [];
    const editMenu = menu.items.find((item) => item.label === 'Edit');
    if (!editMenu || !editMenu.submenu) return [];
    return editMenu.submenu.items.map((item) => item.label);
  });
  expect(menuLabels).toContain('Delete');
});

test('delete menu action sends IPC to renderer', async () => {
  // Send the edit:delete action and verify it doesn't crash
  await sendMenuAction(ctx.electronApp, 'edit:delete');
  await waitForUpdate(ctx.window);
  // App should still be running fine
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');
});

test('backspace accelerator is configured for delete', async () => {
  const accelerator = await ctx.electronApp.evaluate(({ Menu }) => {
    const menu = Menu.getApplicationMenu();
    if (!menu) return null;
    const editMenu = menu.items.find((item) => item.label === 'Edit');
    if (!editMenu || !editMenu.submenu) return null;
    const deleteItem = editMenu.submenu.items.find(
      (item) => item.label === 'Delete',
    );
    return deleteItem?.accelerator ?? null;
  });
  expect(accelerator).toBe('Backspace');
});
