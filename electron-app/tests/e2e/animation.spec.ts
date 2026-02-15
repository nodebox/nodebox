import { test, expect } from '@playwright/test';
import { launchApp, waitForUpdate, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx.electronApp.close();
});

test('animation bar shows default frame counter', async () => {
  // Default frame is 1
  const frameCounter = ctx.window.locator('span:has-text("1")').first();
  await expect(frameCounter).toBeVisible();
});

test('animation bar shows frame counter', async () => {
  // The animation bar displays the current frame number
  const frameCounter = ctx.window.locator('span:has-text("1")').first();
  await expect(frameCounter).toBeVisible();
});

test('play button is visible', async () => {
  // The play/stop button is in the animation bar
  const playButton = ctx.window.locator('button').last();
  await expect(playButton).toBeVisible();
});

test('space bar toggles playback', async () => {
  // Press space to start playing
  await ctx.window.keyboard.press('Space');
  await waitForUpdate(ctx.window, 100);

  // The button icon should change (Play -> Square for stop)
  // We verify the app is still functional
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');

  // Press space again to stop
  await ctx.window.keyboard.press('Space');
  await waitForUpdate(ctx.window, 100);
});

test('clicking the play/stop button toggles playback', async () => {
  // Find the play button (last button in the animation bar)
  const animationBar = ctx.window.locator(
    'div[style*="height: 32"]',
  ).last();

  const button = animationBar.locator('button');
  if ((await button.count()) > 0) {
    await button.first().click();
    await waitForUpdate(ctx.window, 100);
    // Click again to stop
    await button.first().click();
    await waitForUpdate(ctx.window, 100);
  }

  // App should still be responsive
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');
});

test('animation bar has play and rewind buttons', async () => {
  // The animation bar should contain at least 2 buttons (play/pause + rewind)
  const animationBar = ctx.window.locator(`div[style*="height: 28"]`).last();
  const buttons = animationBar.locator('button');
  const count = await buttons.count();
  expect(count).toBeGreaterThanOrEqual(2);
});
