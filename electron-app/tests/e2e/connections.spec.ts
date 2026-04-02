import { test, expect } from '@playwright/test';
import { launchApp, waitForUpdate, getStoreState, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx?.electronApp?.close();
});

test('create connection by dragging from output port to input port', async () => {
  // First, add a colorize node via the node dialog
  const canvas = ctx.window.locator('canvas').last();
  await canvas.dblclick({ position: { x: 50, y: 50 } });
  await waitForUpdate(ctx.window);

  // Search for colorize and select it
  const searchInput = ctx.window.locator('input[placeholder="Search nodes..."]');
  await expect(searchInput).toBeVisible({ timeout: 2000 });
  await searchInput.fill('colorize');
  await waitForUpdate(ctx.window);

  // Press Enter to create the colorize node (first result is selected by default)
  await searchInput.press('Enter');
  await waitForUpdate(ctx.window);

  // Verify colorize1 was added
  const stateAfterAdd = await getStoreState(ctx.window);
  const colorize1 = stateAfterAdd.children.find((c: any) => c.name === 'colorize1');
  expect(colorize1).toBeDefined();

  // Now drag from rect1's output port to colorize1's input port
  const box = await canvas.boundingBox();
  expect(box).not.toBeNull();

  // rect1 output port is at bottom-left of rect1
  // rect1 is at grid position {x:1, y:1}
  // rect1 screen rect within canvas: x = 8 + 1*48 + 8 = 64, y = 8 + 1*48 + 8 = 64
  // Output port: x=64, y=64+32=96, w=12, h=4
  // Port center: (64+6, 96+2) = (70, 98)
  const outPortX = 70;
  const outPortY = 98;

  // colorize1 is placed at grid position {x:1, y:3} (children.length was 1 when added, so y = 1 + 1*2 = 3)
  // colorize1 screen rect: x = 8 + 1*48 + 8 = 64, y = 8 + 3*48 + 8 = 160
  // Input port (first port "shape"): x=64+6=70, y=160-2=158
  const colorize1Pos = colorize1!.position;
  const inputPortX = 8 + colorize1Pos.x * 48 + 8 + 6;
  const inputPortY = 8 + colorize1Pos.y * 48 + 8 - 2;

  // Drag from output port to input port (using canvas-relative positions)
  await canvas.click({ position: { x: outPortX, y: outPortY } }); // ensure canvas is focused
  await waitForUpdate(ctx.window);

  // Use mouse.move with absolute page coordinates
  await ctx.window.mouse.move(box!.x + outPortX, box!.y + outPortY);
  await ctx.window.mouse.down();
  await ctx.window.mouse.move(box!.x + inputPortX, box!.y + inputPortY, { steps: 10 });
  await ctx.window.mouse.up();
  await waitForUpdate(ctx.window);

  const stateAfterConnect = await getStoreState(ctx.window);
  const conn = stateAfterConnect.connections.find(
    (c: any) => c.output_node === 'rect1' && c.input_node === 'colorize1',
  );
  expect(conn).toBeDefined();
});
