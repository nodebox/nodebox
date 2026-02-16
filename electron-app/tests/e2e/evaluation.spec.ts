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
  // Wait for evaluation and rendering
  await waitForUpdate(ctx.window, 500);

  // The viewer canvas is the first canvas in the left pane
  const canvases = ctx.window.locator('canvas');
  const count = await canvases.count();
  expect(count).toBeGreaterThanOrEqual(1);

  // Viewer canvas renders content — sample a pixel near center
  // The default rect is at (0,0) with width=100, height=100, black fill
  // Viewer centers at canvas center, so center pixel should be black (the rect fill)
  const viewerCanvas = canvases.first();
  const pixel = await viewerCanvas.evaluate((el: HTMLCanvasElement) => {
    const c = el.getContext('2d');
    if (!c) return null;
    const w = el.width / 2;
    const h = el.height / 2;
    const data = c.getImageData(Math.floor(w), Math.floor(h), 1, 1).data;
    return { r: data[0], g: data[1], b: data[2], a: data[3] };
  });
  expect(pixel).not.toBeNull();
  // The rect has black fill (r=0, g=0, b=0). The background is #e4e4e7 (ZINC_200).
  // Center pixel should be the black rect, not the background.
  expect(pixel!.r).toBeLessThan(50);
  expect(pixel!.g).toBeLessThan(50);
  expect(pixel!.b).toBeLessThan(50);
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
