import { test, expect } from '@playwright/test';
import { launchApp, sendMenuAction, waitForUpdate, getStoreState, type AppContext } from './helpers';

let ctx: AppContext;

test.beforeEach(async () => {
  ctx = await launchApp();
});

test.afterEach(async () => {
  await ctx?.electronApp?.close();
});

async function setRenderedLibrary(
  kind: 'geometry' | 'float' | 'none',
): Promise<void> {
  await ctx.window.evaluate((libraryKind) => {
    const store = (window as any).__store__;
    const setLibrary = store.getState().setLibrary;

    const createRoot = (children: any[], renderedChild: string | null) => ({
      name: 'root',
      prototype: null,
      function: null,
      category: '',
      description: null,
      comment: null,
      position: { x: 0, y: 0 },
      inputs: [],
      output_type: 'Geometry',
      output_range: 'List',
      is_network: true,
      children,
      rendered_child: renderedChild,
      connections: [],
      handle: null,
      always_rendered: false,
    });

    const rectNode = {
      name: 'rect1',
      prototype: 'corevector.rect',
      function: null,
      category: 'geometry',
      description: null,
      comment: null,
      position: { x: 1, y: 1 },
      inputs: [
        {
          name: 'position',
          port_type: 'Point',
          label: 'position',
          description: null,
          widget: 'Point',
          range: 'Value',
          value: { Point: { x: 0, y: 0 } },
          min: null,
          max: null,
          menu_items: [],
        },
        {
          name: 'width',
          port_type: 'Float',
          label: 'width',
          description: null,
          widget: 'Float',
          range: 'Value',
          value: { Float: 100 },
          min: null,
          max: null,
          menu_items: [],
        },
        {
          name: 'height',
          port_type: 'Float',
          label: 'height',
          description: null,
          widget: 'Float',
          range: 'Value',
          value: { Float: 100 },
          min: null,
          max: null,
          menu_items: [],
        },
        {
          name: 'roundness',
          port_type: 'Point',
          label: 'roundness',
          description: null,
          widget: 'Point',
          range: 'Value',
          value: { Point: { x: 0, y: 0 } },
          min: null,
          max: null,
          menu_items: [],
        },
      ],
      output_type: 'Geometry',
      output_range: 'Value',
      is_network: false,
      children: [],
      rendered_child: null,
      connections: [],
      handle: null,
      always_rendered: false,
    };

    const floatNode = {
      name: 'number1',
      prototype: 'math.number',
      function: null,
      category: 'math',
      description: null,
      comment: null,
      position: { x: 1, y: 1 },
      inputs: [
        {
          name: 'value',
          port_type: 'Float',
          label: 'value',
          description: null,
          widget: 'Float',
          range: 'Value',
          value: { Float: 42 },
          min: null,
          max: null,
          menu_items: [],
        },
      ],
      output_type: 'Float',
      output_range: 'Value',
      is_network: false,
      children: [],
      rendered_child: null,
      connections: [],
      handle: null,
      always_rendered: false,
    };

    const library = {
      name: '',
      root: libraryKind === 'geometry'
        ? createRoot([rectNode], 'rect1')
        : libraryKind === 'float'
          ? createRoot([floatNode], 'number1')
          : createRoot([rectNode], null),
      properties: {
        canvasWidth: '1000',
        canvasHeight: '1000',
      },
      uuid: null,
      format_version: 21,
    };

    setLibrary(library);
  }, kind);
}

test('viewer canvas exists and has dimensions', async () => {
  // The viewer is the second canvas element (after the network canvas)
  const canvases = ctx.window.locator('canvas');
  const count = await canvases.count();
  expect(count).toBeGreaterThanOrEqual(2);

  const viewerCanvas = canvases.nth(1);
  const box = await viewerCanvas.boundingBox();
  expect(box).not.toBeNull();
  expect(box!.width).toBeGreaterThan(0);
  expect(box!.height).toBeGreaterThan(0);
});

test('viewer canvas zooms with wheel events', async () => {
  const viewerCanvas = ctx.window.locator('canvas').nth(1);
  const box = await viewerCanvas.boundingBox();
  expect(box).not.toBeNull();

  const before = await getStoreState(ctx.window);

  // Scroll on the viewer canvas and confirm the viewer zoom changes.
  await ctx.window.mouse.move(box!.x + box!.width / 2, box!.y + box!.height / 2);
  await ctx.window.mouse.wheel(0, -100);
  await waitForUpdate(ctx.window);

  const after = await getStoreState(ctx.window);
  expect(after.viewerZoom).toBeGreaterThan(before.viewerZoom);
});

test('viewer pane header is visible', async () => {
  const header = ctx.window.locator('text=Viewer');
  await expect(header).toBeVisible();
});

test('toggle canvas border via menu action', async () => {
  await sendMenuAction(ctx.electronApp, 'view:toggle-canvas-border');
  await waitForUpdate(ctx.window);
  // Should not crash - the viewer re-renders without the border
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');
});

test('toggle origin crosshair via menu action', async () => {
  await sendMenuAction(ctx.electronApp, 'view:toggle-origin');
  await waitForUpdate(ctx.window);
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');
});

test('toggle handles via menu action', async () => {
  await sendMenuAction(ctx.electronApp, 'view:toggle-handles');
  await waitForUpdate(ctx.window);
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');
});

test('toggle points via menu action', async () => {
  await sendMenuAction(ctx.electronApp, 'view:toggle-points');
  await waitForUpdate(ctx.window);
  const title = await ctx.window.title();
  expect(title).toBe('NodeBox');
});

test('pane headers have different top and bottom border colors', async () => {
  const header = ctx.window.locator('text=Viewer').locator('..');
  const styles = await header.evaluate((el) => {
    const cs = getComputedStyle(el);
    return {
      borderTop: cs.borderTopColor,
      borderBottom: cs.borderBottomColor,
    };
  });
  // Top and bottom borders should be different colors
  expect(styles.borderTop).not.toBe(styles.borderBottom);
});

test('spacebar does not toggle playback', async () => {
  await ctx.window.keyboard.press('Space');
  await waitForUpdate(ctx.window, 100);
  const state = await getStoreState(ctx.window);
  expect(state.isPlaying).toBe(false);
});

test('zoom in changes viewer zoom level', async () => {
  // Use the zoom-in button to zoom (wheel events don't reliably trigger React handlers in E2E)
  const zoomIn = ctx.window.locator('[data-testid="zoom-in"]');
  await zoomIn.click();
  await waitForUpdate(ctx.window);

  const state = await getStoreState(ctx.window);
  expect(state.viewerZoom).toBeGreaterThan(1.0);
});

test('zoom controls are visible in viewer header', async () => {
  const zoomOut = ctx.window.locator('[data-testid="zoom-out"]');
  const zoomLevel = ctx.window.locator('[data-testid="zoom-level"]');
  const zoomIn = ctx.window.locator('[data-testid="zoom-in"]');
  await expect(zoomOut).toBeVisible();
  await expect(zoomLevel).toBeVisible();
  await expect(zoomIn).toBeVisible();
  await expect(zoomLevel).toContainText('100%');
});

test('clicking zoom-in increases zoom', async () => {
  const zoomIn = ctx.window.locator('[data-testid="zoom-in"]');
  await zoomIn.click();
  await waitForUpdate(ctx.window);
  const state = await getStoreState(ctx.window);
  expect(state.viewerZoom).toBeGreaterThan(1.0);
});

test('clicking zoom level resets zoom', async () => {
  // First zoom in
  const zoomIn = ctx.window.locator('[data-testid="zoom-in"]');
  await zoomIn.click();
  await waitForUpdate(ctx.window);

  // Then click the zoom level to reset
  const zoomLevel = ctx.window.locator('[data-testid="zoom-level"]');
  await zoomLevel.click();
  await waitForUpdate(ctx.window);

  const state = await getStoreState(ctx.window);
  expect(state.viewerZoom).toBeCloseTo(1.0, 1);
});

test('splitter is visible and has extended hit area', async () => {
  // The vertical splitter is 2px visually but has an invisible hit area child
  const splitter = ctx.window.locator('[data-testid="vertical-splitter"]');
  await expect(splitter).toBeVisible();
  const box = await splitter.boundingBox();
  expect(box).not.toBeNull();
  expect(box!.width).toBe(2);
  // The hit area child extends beyond the visual splitter
  const hitArea = splitter.locator('div');
  const hitBox = await hitArea.boundingBox();
  expect(hitBox).not.toBeNull();
  expect(hitBox!.width).toBeGreaterThanOrEqual(6);
});

test('View menu has zoom and display options', async () => {
  const menuLabels = await ctx.electronApp.evaluate(({ Menu }) => {
    const menu = Menu.getApplicationMenu();
    if (!menu) return [];
    const viewMenu = menu.items.find((item) => item.label === 'View');
    if (!viewMenu || !viewMenu.submenu) return [];
    return viewMenu.submenu.items.map((item) => item.label);
  });
  expect(menuLabels).toContain('Zoom In');
  expect(menuLabels).toContain('Zoom Out');
  expect(menuLabels).toContain('Fit');
  expect(menuLabels).toContain('Show Handles');
  expect(menuLabels).toContain('Show Points');
  expect(menuLabels).toContain('Show Origin');
  expect(menuLabels).toContain('Show Canvas Border');
});

test('non-graphical output forces data mode and disables visual mode', async () => {
  await setRenderedLibrary('float');

  await expect(async () => {
    const state = await getStoreState(ctx.window);
    expect(state.renderResult?.outputType).toBe('Float');
    expect(state.viewerMode).toBe('data');
    expect(state.visualViewerAvailable).toBe(false);
  }).toPass({ timeout: 5000 });

  await expect(ctx.window.locator('[data-testid="data-viewer"]')).toBeVisible();
  await expect(ctx.window.locator('[data-testid="viewer-mode-visual"]')).toHaveAttribute('aria-disabled', 'true');
  await expect(ctx.window.locator('[data-testid="data-viewer"]')).toContainText('42');

  await ctx.window.locator('[data-testid="viewer-mode-visual"]').click();
  await waitForUpdate(ctx.window);

  const state = await getStoreState(ctx.window);
  expect(state.viewerMode).toBe('data');
});

test('restores the previous geometry-capable viewer mode when geometry returns', async () => {
  await ctx.window.locator('[data-testid="viewer-mode-data"]').click();
  await waitForUpdate(ctx.window);

  await setRenderedLibrary('float');
  await expect(async () => {
    const state = await getStoreState(ctx.window);
    expect(state.viewerMode).toBe('data');
    expect(state.visualViewerAvailable).toBe(false);
  }).toPass({ timeout: 5000 });

  await setRenderedLibrary('geometry');
  await expect(async () => {
    const state = await getStoreState(ctx.window);
    expect(state.renderResult?.outputType).toBe('Geometry');
    expect(state.viewerMode).toBe('data');
    expect(state.visualViewerAvailable).toBe(true);
  }).toPass({ timeout: 5000 });

  await expect(ctx.window.locator('[data-testid="data-viewer"]')).toBeVisible();
});

test('no rendered child keeps visual mode available', async () => {
  await setRenderedLibrary('none');

  await expect(async () => {
    const state = await getStoreState(ctx.window);
    expect(state.rendered_child).toBeNull();
    expect(state.visualViewerAvailable).toBe(true);
  }).toPass({ timeout: 5000 });

  await ctx.window.locator('[data-testid="viewer-mode-visual"]').click();
  await waitForUpdate(ctx.window);

  const state = await getStoreState(ctx.window);
  expect(state.viewerMode).toBe('visual');
});
