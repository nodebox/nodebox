import { test, expect } from '@playwright/test';
import { launchApp, getStoreState, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx.electronApp.close();
});

test('rect1 has geometry category', async () => {
  const state = await getStoreState(ctx.window);
  const rect1 = state.children.find((c: any) => c.name === 'rect1');
  expect(rect1).toBeDefined();
  expect(rect1.category).toBe('geometry');
});

test('node dialog shows category headers', async () => {
  // Open the node selection dialog
  await ctx.window.keyboard.press('Tab');
  await ctx.window.waitForSelector('input[placeholder="Search nodes..."]');

  // Should have category headers
  const headers = ctx.window.locator('[data-testid^="category-header-"]');
  const count = await headers.count();
  expect(count).toBeGreaterThan(0);
});

test('node dialog shows node icons', async () => {
  await ctx.window.keyboard.press('Tab');
  await ctx.window.waitForSelector('input[placeholder="Search nodes..."]');

  // Should have at least one icon (either SVG img or fallback square)
  const icons = ctx.window.locator('[data-testid^="node-icon-"]');
  const count = await icons.count();
  expect(count).toBeGreaterThan(0);
});
