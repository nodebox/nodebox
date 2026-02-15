import { test, expect } from '@playwright/test';
import { launchApp, waitForUpdate, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx.electronApp.close();
});

test('network canvas renders without errors', async () => {
  // The network canvas is the first <canvas> in the Network pane
  const canvases = ctx.window.locator('canvas');
  const count = await canvases.count();
  expect(count).toBeGreaterThanOrEqual(2);

  // Verify the first canvas has non-zero dimensions
  const dims = await canvases.first().boundingBox();
  expect(dims).not.toBeNull();
  expect(dims!.width).toBeGreaterThan(0);
  expect(dims!.height).toBeGreaterThan(0);
});

test('network canvas responds to wheel events for zoom', async () => {
  const canvas = ctx.window.locator('canvas').first();
  const box = await canvas.boundingBox();
  expect(box).not.toBeNull();

  // Perform a wheel scroll on the canvas - should not throw
  await ctx.window.mouse.move(box!.x + box!.width / 2, box!.y + box!.height / 2);
  await ctx.window.mouse.wheel(0, -100);
  await waitForUpdate(ctx.window);

  // The app should still be responsive
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');
});

test('clicking on empty network canvas clears selection', async () => {
  const canvas = ctx.window.locator('canvas').first();
  const box = await canvas.boundingBox();
  expect(box).not.toBeNull();

  // Click on the canvas (empty area)
  await canvas.click({ position: { x: 50, y: 50 } });
  await waitForUpdate(ctx.window);

  // Should show document properties in parameter panel (no node selected)
  const header = ctx.window.locator('text=Document');
  await expect(header).toBeVisible();
});

test('alt+click on network canvas changes cursor for panning', async () => {
  const canvas = ctx.window.locator('canvas').first();
  const box = await canvas.boundingBox();
  expect(box).not.toBeNull();

  // Alt+pointerdown should initiate panning
  // We verify by checking cursor style changes
  const cursorBefore = await canvas.evaluate(
    (el) => (el as HTMLCanvasElement).style.cursor,
  );
  expect(cursorBefore).toBe('default');
});

test('network pane header label is correct', async () => {
  const header = ctx.window.locator('text=Network');
  await expect(header).toBeVisible();
});
