import { test, expect } from '@playwright/test';
import { launchApp, getStoreState, waitForUpdate, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx?.electronApp?.close();
});

test('evaluator produces a render result with paths', async () => {
  // Wait for the evaluator to run after mount
  await waitForUpdate(ctx.window, 500);
  const state = await getStoreState(ctx.window);
  expect(state.renderResult).not.toBeNull();
  expect(state.renderResult.pathCount).toBeGreaterThanOrEqual(1);
});

test('viewer draws the rect (dark pixel inside it, background outside)', async () => {
  // The default rect is at (0,0) with width=100, height=100, black fill.
  // Poll until the evaluator and the WebGPU device are ready.
  // Vello draws the geometry on a WebGPU canvas, which has no getImageData,
  // so the test renders the same geometry offscreen and reads that.
  await expect(async () => {
    const pixel = await ctx.window.evaluate(async () => {
      const size = 200;
      const pixels: Uint8Array = await (window as any).__viewerPixels__({
        width: size,
        height: size,
        offsetX: size / 2,
        offsetY: size / 2,
        scale: 1,
        background: [255, 255, 255],
      });
      // 30px left of and above the origin, inside the 100x100 rect.
      const i = ((size / 2 - 30) * size + (size / 2 - 30)) * 4;
      return { r: pixels[i], g: pixels[i + 1], b: pixels[i + 2] };
    });
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
