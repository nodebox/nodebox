import { test, expect } from '@playwright/test';
import { launchApp, waitForUpdate, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx.electronApp.close();
});

test('parameter panel shows document properties when no node selected', async () => {
  // When no node is selected, the panel shows document properties (width, height)
  const widthLabel = ctx.window.locator('text=width').first();
  await expect(widthLabel).toBeVisible();
});

test('parameter panel header shows "Parameters" when no node selected', async () => {
  // The ParameterPanel header shows "Parameters" when no node is active
  const header = ctx.window.locator('text=Parameters');
  await expect(header).toBeVisible();
});

test('parameter panel exists in the layout', async () => {
  // The parameter panel is on the right side of the vertical splitter
  // It always renders, even when no node is selected
  // It shows document properties (width, height, background) by default
  const header = ctx.window.locator('text=Document');
  await expect(header).toBeVisible();
});

test('parameter panel has correct default width', async () => {
  // The default width is 450px from the UI slice
  // We can verify by checking the container width
  const panelWidth = await ctx.window.evaluate(() => {
    // Find the parameter panel - it's the last child of the flex container
    const flexContainer = document.querySelector(
      '.flex.flex-1[style*="min-height"]',
    );
    if (!flexContainer) return -1;
    const lastChild = flexContainer.lastElementChild as HTMLElement;
    return lastChild?.offsetWidth ?? -1;
  });
  // Should be approximately 450px (the default from ui-slice)
  expect(panelWidth).toBeGreaterThan(350);
  expect(panelWidth).toBeLessThan(550);
});
