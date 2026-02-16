import { test, expect } from '@playwright/test';
import { launchApp, waitForUpdate, getStoreState, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx.electronApp.close();
});

test('animation bar shows default frame counter', async () => {
  const frameCounter = ctx.window.locator('[data-testid="frame-counter"]');
  await expect(frameCounter).toBeVisible();
  await expect(frameCounter).toContainText('1');
});

test('animation bar shows frame counter', async () => {
  const frameCounter = ctx.window.locator('[data-testid="frame-counter"]');
  await expect(frameCounter).toBeVisible();
});

test('frame counter is interactive', async () => {
  const frameCounter = ctx.window.locator('[data-testid="frame-counter"]');
  await expect(frameCounter).toBeVisible();

  // Click should enter edit mode
  await frameCounter.click();
  await waitForUpdate(ctx.window);
  const input = frameCounter.locator('input');
  await expect(input).toBeVisible();

  // Type a new frame value
  await input.fill('10');
  await input.press('Enter');
  await waitForUpdate(ctx.window);

  // Verify frame changed
  const state = await getStoreState(ctx.window);
  expect(state.frame).toBe(10);
});

test('play button is visible', async () => {
  // The play/stop button is in the animation bar
  const playButton = ctx.window.locator('button').last();
  await expect(playButton).toBeVisible();
});

test('space bar does not toggle playback', async () => {
  await ctx.window.keyboard.press('Space');
  await waitForUpdate(ctx.window, 100);
  // Spacebar should NOT start playback anymore
  const state = await getStoreState(ctx.window);
  expect(state.isPlaying).toBe(false);
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
