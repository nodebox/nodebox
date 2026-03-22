import { test, expect } from '@playwright/test';
import { launchApp, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx?.electronApp?.close();
});

test('app starts with a visible window', async () => {
  const isVisible = await ctx.window.evaluate(() => {
    return document.visibilityState === 'visible';
  });
  expect(isVisible).toBe(true);
});

test('window title is NodeBox', async () => {
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');
});

test('layout contains Network pane header', async () => {
  const networkHeader = ctx.window.locator('text=Network');
  await expect(networkHeader).toBeVisible();
});

test('layout contains Viewer pane header', async () => {
  const viewerHeader = ctx.window.locator('text=Viewer');
  await expect(viewerHeader).toBeVisible();
});

test('layout contains Parameters pane header', async () => {
  const paramsHeader = ctx.window.locator('text=Parameters');
  await expect(paramsHeader).toBeVisible();
});

test('network canvas element exists', async () => {
  // The NetworkCanvas renders a <canvas> inside the Network pane
  const canvases = ctx.window.locator('canvas');
  const count = await canvases.count();
  // At least 2 canvases: network and viewer
  expect(count).toBeGreaterThanOrEqual(2);
});

test('animation bar is visible', async () => {
  // The AnimationBar shows a frame counter and playback buttons
  const frameCounter = ctx.window.locator('span:has-text("1")').first();
  await expect(frameCounter).toBeVisible();
});

test('address bar shows root', async () => {
  const rootText = ctx.window.locator('text=root');
  await expect(rootText).toBeVisible();
});
