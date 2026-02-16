import { test, expect } from '@playwright/test';
import { launchApp, getStoreState, waitForUpdate, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx.electronApp.close();
});

test('evaluator produces a render result with paths', async () => {
  // Wait for the evaluator to run after mount
  await waitForUpdate(ctx.window, 500);
  const state = await getStoreState(ctx.window);
  expect(state.renderResult).not.toBeNull();
  expect(state.renderResult.pathCount).toBeGreaterThanOrEqual(1);
});

test('viewer canvas draws the rect (has non-white center pixel)', async () => {
  // The default rect is at (0,0) with width=100, height=100, black fill.
  // The origin crosshair paints over the exact center, so sample slightly off-center.
  // Poll until the evaluator finishes and the canvas paints.
  const viewerCanvas = ctx.window.locator('canvas').first();

  await expect(async () => {
    const pixel = await viewerCanvas.evaluate((el: HTMLCanvasElement) => {
      const c = el.getContext('2d');
      if (!c) return null;
      // Offset by 30px to avoid the origin crosshair (arm=20px from center)
      const data = c.getImageData(Math.floor(el.width / 2) - 30, Math.floor(el.height / 2) - 30, 1, 1).data;
      return { r: data[0], g: data[1], b: data[2] };
    });
    expect(pixel).not.toBeNull();
    expect(pixel!.r).toBeLessThan(50);
    expect(pixel!.g).toBeLessThan(50);
    expect(pixel!.b).toBeLessThan(50);
  }).toPass({ timeout: 5000 });
});

test('data tab shows table when clicked', async () => {
  await waitForUpdate(ctx.window, 500);

  // Click on the "Data" segment button in the viewer header
  const dataButton = ctx.window.locator('span:has-text("Data")').first();
  await dataButton.click();
  await waitForUpdate(ctx.window);

  // Verify the data viewer table appears
  const table = ctx.window.locator('[data-testid="data-viewer"]');
  await expect(table).toBeVisible();

  // Should have at least one data row (the rect path)
  const rows = ctx.window.locator('[data-testid="data-row"]');
  const rowCount = await rows.count();
  expect(rowCount).toBeGreaterThanOrEqual(1);
});

test('switching back to visual tab shows the canvas', async () => {
  await waitForUpdate(ctx.window, 500);

  // Switch to Data tab
  const dataButton = ctx.window.locator('span:has-text("Data")').first();
  await dataButton.click();
  await waitForUpdate(ctx.window);

  // Data viewer should be visible
  const table = ctx.window.locator('[data-testid="data-viewer"]');
  await expect(table).toBeVisible();

  // Switch back to Visual tab
  const visualButton = ctx.window.locator('span:has-text("Visual")').first();
  await visualButton.click();
  await waitForUpdate(ctx.window);

  // Canvas should be visible again, data viewer should not
  await expect(table).not.toBeVisible();
  const canvases = ctx.window.locator('canvas');
  const count = await canvases.count();
  expect(count).toBeGreaterThanOrEqual(1);
});
