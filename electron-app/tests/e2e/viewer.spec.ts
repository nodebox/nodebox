import { test, expect } from '@playwright/test';
import { launchApp, sendMenuAction, waitForUpdate, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx.electronApp.close();
});

test('viewer canvas exists and has dimensions', async () => {
  // The viewer is the second canvas element (after the network canvas)
  const canvases = ctx.window.locator('canvas');
  const count = await canvases.count();
  expect(count).toBeGreaterThanOrEqual(2);

  const viewerCanvas = canvases.nth(1);
  const box = await viewerCanvas.boundingBox();
  expect(box).not.toBeNull();
  expect(box!.width).toBeGreaterThan(0);
  expect(box!.height).toBeGreaterThan(0);
});

test('viewer canvas responds to wheel events', async () => {
  const viewerCanvas = ctx.window.locator('canvas').nth(1);
  const box = await viewerCanvas.boundingBox();
  expect(box).not.toBeNull();

  // Scroll on the viewer canvas
  await ctx.window.mouse.move(box!.x + box!.width / 2, box!.y + box!.height / 2);
  await ctx.window.mouse.wheel(0, -100);
  await waitForUpdate(ctx.window);

  // App should still work
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');
});

test('viewer pane header is visible', async () => {
  const header = ctx.window.locator('text=Viewer');
  await expect(header).toBeVisible();
});

test('toggle canvas border via menu action', async () => {
  await sendMenuAction(ctx.electronApp, 'view:toggle-canvas-border');
  await waitForUpdate(ctx.window);
  // Should not crash - the viewer re-renders without the border
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');
});

test('toggle origin crosshair via menu action', async () => {
  await sendMenuAction(ctx.electronApp, 'view:toggle-origin');
  await waitForUpdate(ctx.window);
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');
});

test('toggle handles via menu action', async () => {
  await sendMenuAction(ctx.electronApp, 'view:toggle-handles');
  await waitForUpdate(ctx.window);
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');
});

test('toggle points via menu action', async () => {
  await sendMenuAction(ctx.electronApp, 'view:toggle-points');
  await waitForUpdate(ctx.window);
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');
});

test('View menu has zoom and display options', async () => {
  const menuLabels = await ctx.electronApp.evaluate(({ Menu }) => {
    const menu = Menu.getApplicationMenu();
    if (!menu) return [];
    const viewMenu = menu.items.find((item) => item.label === 'View');
    if (!viewMenu || !viewMenu.submenu) return [];
    return viewMenu.submenu.items.map((item) => item.label);
  });
  expect(menuLabels).toContain('Zoom In');
  expect(menuLabels).toContain('Zoom Out');
  expect(menuLabels).toContain('Fit');
  expect(menuLabels).toContain('Show Handles');
  expect(menuLabels).toContain('Show Points');
  expect(menuLabels).toContain('Show Origin');
  expect(menuLabels).toContain('Show Canvas Border');
});
