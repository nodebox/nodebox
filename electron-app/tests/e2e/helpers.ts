import {
  _electron as electron,
  expect,
  type ElectronApplication,
  type Page,
} from '@playwright/test';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

export interface AppContext {
  electronApp: ElectronApplication;
  window: Page;
}

/**
 * Launch the Electron app and wait for the first window to be ready.
 * Callers must call `electronApp.close()` in afterEach/afterAll.
 */
export async function launchApp(): Promise<AppContext> {
  const electronApp = await electron.launch({
    args: ['.'],
    cwd: path.resolve(__dirname, '../..'),
    env: { ...process.env, NODEBOX_E2E: '1' },
  });
  const window = await electronApp.firstWindow();
  // Wait for the React app to mount
  await window.waitForSelector('#root > *', { timeout: 30000 });
  return { electronApp, window };
}

/**
 * Wait until the viewer's WebGPU device is ready. On a GPU this is immediate.
 * On the software GPU in CI, compiling Vello's shaders takes about 25 seconds
 * and keeps the renderer busy, so callers need a longer test timeout.
 */
export async function waitForViewer(window: Page): Promise<void> {
  await expect(async () => {
    await window.evaluate(() =>
      (window as any).__viewerPixels__({ width: 1, height: 1, offsetX: 0, offsetY: 0, scale: 1, background: [0, 0, 0] }),
    );
  }).toPass({ timeout: 60000 });
}

/**
 * Get serializable store state for test assertions on canvas-based features.
 */
export async function getStoreState(window: Page) {
  return window.evaluate(() => (window as any).__storeState__());
}

/**
 * Wait briefly for state updates and re-renders.
 */
export async function waitForUpdate(window: Page, ms = 300): Promise<void> {
  await window.waitForTimeout(ms);
}

/**
 * Send a menu action to the renderer via IPC, simulating a menu click.
 */
export async function sendMenuAction(
  electronApp: ElectronApplication,
  action: string,
): Promise<void> {
  await electronApp.evaluate(({ BrowserWindow }, act) => {
    const win = BrowserWindow.getAllWindows()[0];
    if (win) {
      win.webContents.send('menu:action', act);
    }
  }, action);
}
