import { test, expect } from '@playwright/test';
import { launchApp, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx?.electronApp?.close();
});

test('Export SVG menu item exists', async () => {
  const menuLabels = await ctx.electronApp.evaluate(({ Menu }) => {
    const menu = Menu.getApplicationMenu();
    if (!menu) return [];
    const fileMenu = menu.items.find((item) => item.label === 'File');
    if (!fileMenu || !fileMenu.submenu) return [];
    return fileMenu.submenu.items.map((item) => item.label);
  });
  expect(menuLabels).toContain('Export SVG...');
});

test('Export PNG menu item exists', async () => {
  const menuLabels = await ctx.electronApp.evaluate(({ Menu }) => {
    const menu = Menu.getApplicationMenu();
    if (!menu) return [];
    const fileMenu = menu.items.find((item) => item.label === 'File');
    if (!fileMenu || !fileMenu.submenu) return [];
    return fileMenu.submenu.items.map((item) => item.label);
  });
  expect(menuLabels).toContain('Export PNG...');
});

test('electronAPI exposes exportSvg in renderer', async () => {
  const hasExportSvg = await ctx.window.evaluate(() => {
    return typeof (window as unknown as { electronAPI?: { exportSvg?: unknown } })
      .electronAPI?.exportSvg === 'function';
  });
  expect(hasExportSvg).toBe(true);
});

test('electronAPI exposes exportPng in renderer', async () => {
  const hasExportPng = await ctx.window.evaluate(() => {
    return typeof (window as unknown as { electronAPI?: { exportPng?: unknown } })
      .electronAPI?.exportPng === 'function';
  });
  expect(hasExportPng).toBe(true);
});
