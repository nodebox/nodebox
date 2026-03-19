import { test, expect } from '@playwright/test';
import { launchApp, getStoreState, waitForUpdate, type AppContext } from './helpers';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const fixturesDir = path.resolve(__dirname, '../fixtures');

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx.electronApp.close();
});

test('import_svg node renders paths from an SVG file', async () => {
  // Use the node dialog to add an import_svg node
  await ctx.window.keyboard.press('Tab');
  await waitForUpdate(ctx.window);
  const searchInput = ctx.window.locator('input[placeholder="Search nodes..."]');
  await searchInput.fill('import_svg');
  await waitForUpdate(ctx.window);
  await ctx.window.keyboard.press('Enter');
  await waitForUpdate(ctx.window);

  // Use the store to set up the import_svg node:
  // - Set the file port to "popcorn.svg"
  // - Set filePath so the evaluator can derive the project directory
  // - Set as rendered child
  await ctx.window.evaluate((dir: string) => {
    const store = (window as any).__store__;
    const state = store.getState();
    const svgNode = state.library.root.children.find(
      (n: any) => n.prototype === 'corevector.import_svg',
    );
    if (!svgNode) throw new Error('import_svg node not found');

    // Point to the fixtures directory as our "project"
    state.setFilePath(dir + '/project.ndbx');
    state.setPortValue(svgNode.name, 'file', { String: 'popcorn.svg' });
    state.setRenderedChild('root', svgNode.name);
  }, fixturesDir);

  // Wait for async evaluation (file reading via IPC + WASM evaluation)
  await waitForUpdate(ctx.window, 2000);

  const state = await getStoreState(ctx.window);
  expect(state.renderResult).not.toBeNull();
  expect(state.renderResult.pathCount).toBeGreaterThanOrEqual(1);
});
