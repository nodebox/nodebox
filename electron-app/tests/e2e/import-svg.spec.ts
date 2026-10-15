import { test, expect } from '@playwright/test';
import { launchApp, getStoreState, sendMenuAction, waitForUpdate, type AppContext } from './helpers';
import { copyFileSync, mkdtempSync, rmSync } from 'fs';
import { tmpdir } from 'os';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const fixturesDir = path.resolve(__dirname, '../fixtures');

let ctx: AppContext;
let projectDir: string;

test.beforeEach(async () => {
  projectDir = mkdtempSync(path.join(tmpdir(), 'nodebox-e2e-'));
  copyFileSync(path.join(fixturesDir, 'popcorn.svg'), path.join(projectDir, 'popcorn.svg'));
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx?.electronApp?.close();
  rmSync(projectDir, { recursive: true, force: true });
});

test('import_svg node renders paths from an SVG file', async () => {
  test.setTimeout(90000);
  // Use the node dialog to add an import_svg node
  await ctx.window.keyboard.press('Tab');
  await waitForUpdate(ctx.window);
  const searchInput = ctx.window.locator('input[placeholder="Search nodes..."]');
  await searchInput.fill('import_svg');
  await waitForUpdate(ctx.window);
  await ctx.window.keyboard.press('Enter');
  await waitForUpdate(ctx.window);

  // The main process reads assets from the folder of the saved project, so save
  // the document next to the SVG first, answering the save dialog here.
  const projectFile = path.join(projectDir, 'project.ndbx');
  await ctx.electronApp.evaluate(({ dialog }, filePath) => {
    dialog.showSaveDialog = async () => ({ canceled: false, filePath });
  }, projectFile);
  await sendMenuAction(ctx.electronApp, 'file:save-as');
  await expect
    .poll(() => ctx.window.evaluate(() => (window as any).__store__.getState().filePath))
    .toBe(projectFile);

  // Point the file port at the SVG and render the node.
  await ctx.window.evaluate(() => {
    const store = (window as any).__store__;
    const state = store.getState();
    const svgNode = state.library.root.children.find(
      (n: any) => n.prototype === 'corevector.import_svg',
    );
    if (!svgNode) throw new Error('import_svg node not found');
    state.setPortValue(svgNode.name, 'file', { String: 'popcorn.svg' });
    state.setRenderedChild('root', svgNode.name);
  });

  // Wait for async evaluation (file reading via IPC + WASM evaluation). On the
  // software GPU in CI the renderer is busy starting the viewer, which delays it.
  await expect
    .poll(async () => (await getStoreState(ctx.window)).renderResult?.pathCount ?? 0, { timeout: 45000 })
    .toBeGreaterThanOrEqual(1);
});
