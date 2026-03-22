import { test, expect } from '@playwright/test';
import { launchApp, waitForUpdate, getStoreState, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx?.electronApp?.close();
});

async function selectRect1(ctx: AppContext) {
  // rect1 is at grid position (1,1). Click on its center in the network canvas.
  const networkCanvas = ctx.window.locator('canvas').last();
  await networkCanvas.click({ position: { x: 128, y: 80 } });
  await waitForUpdate(ctx.window);
}

test('parameter rows visible after selecting rect1', async () => {
  await selectRect1(ctx);

  // rect1 has 4 inputs: position, width, height, roundness
  const rows = ctx.window.locator('[data-testid^="param-row-"]');
  await expect(rows).toHaveCount(4);

  await expect(ctx.window.locator('[data-testid="param-row-position"]')).toBeVisible();
  await expect(ctx.window.locator('[data-testid="param-row-width"]')).toBeVisible();
  await expect(ctx.window.locator('[data-testid="param-row-height"]')).toBeVisible();
  await expect(ctx.window.locator('[data-testid="param-row-roundness"]')).toBeVisible();
});

test('header shows node info when node selected', async () => {
  await selectRect1(ctx);
  // ParametersHeader (pane header) shows node name and prototype
  const header = ctx.window.locator('text=Parameters').locator('..');
  await expect(header).toContainText('rect1');
  await expect(header).toContainText('corevector.rect');
});

test('DragValue editing: click, type, enter commits value', async () => {
  await selectRect1(ctx);

  // Click on the width value to enter edit mode
  const widthValue = ctx.window.locator('[data-testid="param-value-width"]');
  await expect(widthValue).toBeVisible();
  await widthValue.click();
  await waitForUpdate(ctx.window);

  // An input should appear
  const input = widthValue.locator('input');
  await expect(input).toBeVisible();

  // Clear and type new value
  await input.fill('200');
  await input.press('Enter');
  await waitForUpdate(ctx.window);

  // Verify the store has the new value
  const state = await getStoreState(ctx.window);
  const rect1 = state.children.find((c: any) => c.name === 'rect1');
  expect(rect1).toBeDefined();
  const widthPort = rect1.ports.find((p: any) => p.name === 'width');
  expect(widthPort).toBeDefined();
  expect(widthPort.value.Float).toBe(200);
});

test('DragValue dragging changes value', async () => {
  await selectRect1(ctx);

  const widthValue = ctx.window.locator('[data-testid="param-value-width"]');
  await expect(widthValue).toBeVisible();
  const box = await widthValue.boundingBox();
  expect(box).not.toBeNull();

  // Drag right by 50px to increase value
  const startX = box!.x + box!.width / 2;
  const startY = box!.y + box!.height / 2;

  await ctx.window.mouse.move(startX, startY);
  await ctx.window.mouse.down();
  await ctx.window.mouse.move(startX + 50, startY, { steps: 5 });
  await ctx.window.mouse.up();
  await waitForUpdate(ctx.window);

  // Value should have increased from 100
  const state = await getStoreState(ctx.window);
  const rect1 = state.children.find((c: any) => c.name === 'rect1');
  const widthPort = rect1.ports.find((p: any) => p.name === 'width');
  expect(widthPort.value.Float).toBeGreaterThan(100);
});

test('document properties shown when no node selected', async () => {
  // By default no node is selected; document properties should show
  const widthLabel = ctx.window.locator('text=width').first();
  await expect(widthLabel).toBeVisible();
  const heightLabel = ctx.window.locator('text=height').first();
  await expect(heightLabel).toBeVisible();
});

test('parameter panel has two-tone background columns', async () => {
  await selectRect1(ctx);
  const bgLeft = ctx.window.locator('[data-testid="param-bg-left"]');
  const bgRight = ctx.window.locator('[data-testid="param-bg-right"]');
  await expect(bgLeft).toBeVisible();
  await expect(bgRight).toBeVisible();
});

test('DragValue hover shows inset highlight', async () => {
  await selectRect1(ctx);
  const widthValue = ctx.window.locator('[data-testid="param-value-width"]');
  await widthValue.hover();
  await waitForUpdate(ctx.window);
  // The inner hover element should have margins creating visual inset
  const innerDiv = widthValue.locator('[data-testid="drag-value-inner"]');
  await expect(innerDiv).toBeVisible();
});

test('dragging label changes value', async () => {
  await selectRect1(ctx);
  const label = ctx.window.locator('[data-testid="param-label-width"]');
  await expect(label).toBeVisible();
  const box = await label.boundingBox();
  expect(box).not.toBeNull();

  // Drag right by 50px
  const startX = box!.x + box!.width / 2;
  const startY = box!.y + box!.height / 2;
  await ctx.window.mouse.move(startX, startY);
  await ctx.window.mouse.down();
  await ctx.window.mouse.move(startX + 50, startY, { steps: 5 });
  await ctx.window.mouse.up();
  await waitForUpdate(ctx.window);

  // Width should have increased from 100
  const state = await getStoreState(ctx.window);
  const rect1 = state.children.find((c: any) => c.name === 'rect1');
  const widthPort = rect1.ports.find((p: any) => p.name === 'width');
  expect(widthPort.value.Float).toBeGreaterThan(100);
});
