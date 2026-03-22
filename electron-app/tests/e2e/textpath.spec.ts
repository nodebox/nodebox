import { test, expect } from '@playwright/test';
import { launchApp, getStoreState, waitForUpdate, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx?.electronApp?.close();
});

test('textpath node produces render output', async () => {
  // Open node dialog with Tab
  await ctx.window.keyboard.press('Tab');
  await waitForUpdate(ctx.window);

  // Search for "textpath"
  const searchInput = ctx.window.locator('input[placeholder*="Search"]').first();
  await searchInput.fill('textpath');
  await waitForUpdate(ctx.window);

  // Press Enter to create the node
  await ctx.window.keyboard.press('Enter');
  await waitForUpdate(ctx.window);

  // Wait for WASM to initialize and evaluation to complete
  await expect(async () => {
    const state = await getStoreState(ctx.window);
    expect(state.renderResult).not.toBeNull();
    expect(state.renderResult.pathCount).toBeGreaterThanOrEqual(1);
  }).toPass({ timeout: 10000 });
});

test('textpath renders visible text on the viewer canvas', async () => {
  // Open node dialog and create textpath
  await ctx.window.keyboard.press('Tab');
  await waitForUpdate(ctx.window);
  const searchInput = ctx.window.locator('input[placeholder*="Search"]').first();
  await searchInput.fill('textpath');
  await waitForUpdate(ctx.window);
  await ctx.window.keyboard.press('Enter');
  await waitForUpdate(ctx.window);

  // Wait for render result to have paths with contours
  await expect(async () => {
    const state = await getStoreState(ctx.window);
    expect(state.renderResult).not.toBeNull();
    expect(state.renderResult.pathCount).toBeGreaterThanOrEqual(1);
    expect(state.renderResult.totalPoints).toBeGreaterThan(0);
  }).toPass({ timeout: 10000 });
});
