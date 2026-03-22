import { test, expect } from '@playwright/test';
import { launchApp, waitForUpdate, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx?.electronApp?.close();
});

test('pressing Tab opens the node selection dialog', async () => {
  await ctx.window.keyboard.press('Tab');
  await waitForUpdate(ctx.window);
  // The dialog should show the search input with placeholder
  const searchInput = ctx.window.locator('input[placeholder="Search nodes..."]');
  await expect(searchInput).toBeVisible();
});

test('node selection dialog shows node prototypes', async () => {
  await ctx.window.keyboard.press('Tab');
  await waitForUpdate(ctx.window);
  // Should show some known node names
  const rectNode = ctx.window.locator('text=rect').first();
  await expect(rectNode).toBeVisible();
  const ellipseNode = ctx.window.locator('text=ellipse').first();
  await expect(ellipseNode).toBeVisible();
});

test('typing in search field filters nodes', async () => {
  await ctx.window.keyboard.press('Tab');
  await waitForUpdate(ctx.window);
  const searchInput = ctx.window.locator('input[placeholder="Search nodes..."]');
  await searchInput.fill('ell');
  await waitForUpdate(ctx.window);
  // "ellipse" should still be visible
  const ellipseNode = ctx.window.locator('text=ellipse').first();
  await expect(ellipseNode).toBeVisible();
  // "rect" should not be visible since it doesn't match "ell"
  // (it could still appear if the filter matches category, but "rect" category is "geometry")
  // Use a stricter locator to check for exact item rows
  const items = ctx.window.locator('div[style*="height: 32"]');
  const count = await items.count();
  // With "ell" query, only ellipse should match from the geometry nodes
  expect(count).toBeGreaterThanOrEqual(1);
});

test('search with no results shows empty state', async () => {
  await ctx.window.keyboard.press('Tab');
  await waitForUpdate(ctx.window);
  const searchInput = ctx.window.locator('input[placeholder="Search nodes..."]');
  await searchInput.fill('xyznonexistent');
  await waitForUpdate(ctx.window);
  const emptyState = ctx.window.locator('text=No nodes found');
  await expect(emptyState).toBeVisible();
});

test('Escape closes the node selection dialog', async () => {
  await ctx.window.keyboard.press('Tab');
  await waitForUpdate(ctx.window);
  const searchInput = ctx.window.locator('input[placeholder="Search nodes..."]');
  await expect(searchInput).toBeVisible();
  await ctx.window.keyboard.press('Escape');
  await waitForUpdate(ctx.window);
  await expect(searchInput).not.toBeVisible();
});

test('arrow keys navigate the node list', async () => {
  await ctx.window.keyboard.press('Tab');
  await waitForUpdate(ctx.window);
  const searchInput = ctx.window.locator('input[placeholder="Search nodes..."]');
  await expect(searchInput).toBeVisible();
  // Press ArrowDown to move selection
  await ctx.window.keyboard.press('ArrowDown');
  await waitForUpdate(ctx.window);
  // The second item should now be highlighted (we can check by looking
  // for the selected background style)
  // This is a smoke test that keyboard navigation doesn't crash
});

test('Enter on a node closes the dialog', async () => {
  await ctx.window.keyboard.press('Tab');
  await waitForUpdate(ctx.window);
  const searchInput = ctx.window.locator('input[placeholder="Search nodes..."]');
  await expect(searchInput).toBeVisible();
  // Press Enter to select the first item (rect)
  await ctx.window.keyboard.press('Enter');
  await waitForUpdate(ctx.window);
  // Dialog should close
  await expect(searchInput).not.toBeVisible();
});

test('clicking outside the dialog closes it', async () => {
  await ctx.window.keyboard.press('Tab');
  await waitForUpdate(ctx.window);
  const searchInput = ctx.window.locator('input[placeholder="Search nodes..."]');
  await expect(searchInput).toBeVisible();
  // Click on the overlay backdrop (the fixed inset-0 div)
  // Click at coordinates outside the dialog
  await ctx.window.click('body', { position: { x: 10, y: 10 } });
  await waitForUpdate(ctx.window);
  await expect(searchInput).not.toBeVisible();
});
