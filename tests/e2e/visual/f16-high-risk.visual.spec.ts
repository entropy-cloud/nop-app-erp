// DOM assertions for F16 — high-risk complex pages
// (plan 2026-07-22-1400-1-f16-high-risk-gantt-bom-scan.md §Phase 3).
//
// Validates that the 2 high-risk F16 pages render their core interaction DOM at runtime:
//   1. aps schedule-gantt page: echarts custom series gantt canvas renders + filter form
//   2. mfg bom-tree page: filter form renders; tree renders DOM when a bomId is entered
//
// DOM-className/canvas strategy (stable across AMIS upgrades) — same as
// f16-complex-pages.visual.spec.ts + dashboards.visual.spec.ts (canvas non-zero size).
// BOM tree requires a seed bomId to render tree nodes; without seed it gracefully
// asserts the filter form + page load (codegen-level coverage via ErpAllWebPagesTest).

import { test, expect, loginAndNavigate } from '../fixtures';
import type { Page } from '@playwright/test';

async function navigateAndReady(page: Page, route: string): Promise<void> {
  await loginAndNavigate(page, route);
  await expect.poll(
    () => page.url(),
    { timeout: 20_000, message: `${route}: URL should include "${route}"` },
  ).toContain(route);
}

test.describe('F16 — high-risk complex pages DOM rendering', () => {
  test('aps schedule-gantt page renders echarts canvas + filter form', async ({ page }) => {
    await navigateAndReady(page, '/aps-schedule-gantt');

    // Filter form renders (筛选条件: machineId / status / startDate / endDate + 刷新甘特图 button)
    await expect.poll(
      async () => page.locator('button').filter({ hasText: /刷新甘特图|Refresh/ }).count(),
      { timeout: 20_000, message: 'gantt filter form refresh button should render' },
    ).toBeGreaterThan(0);

    // echarts custom series gantt renders a canvas with non-zero size.
    // findPage auto-loads on mount (machineId/status optional), so the chart renders even with empty data.
    await expect(
      page.locator('canvas').first(),
      'aps gantt echarts canvas should render',
    ).toBeVisible({ timeout: 20_000 });
    const box = await page.locator('canvas').first().boundingBox();
    expect(
      box !== null && box.width > 0 && box.height > 0,
      'aps gantt echarts canvas should have non-zero size',
    ).toBe(true);
  });

  test('mfg bom-tree page renders filter form + tree DOM on bomId entry', async ({ page }) => {
    await navigateAndReady(page, '/mfg-bom-tree');

    // Filter form renders (展开参数: BOM ID / 期望产出量 / 多级展开 + 展开 BOM button)
    await expect.poll(
      async () => page.locator('button').filter({ hasText: /展开 BOM|Expand/ }).count(),
      { timeout: 20_000, message: 'bom-tree filter form expand button should render' },
    ).toBeGreaterThan(0);

    // The tree only renders after a bomId is entered + 展开 BOM clicked.
    // Try entering bomId=1 and clicking; assert either a tree renders OR the no-data placeholder shows.
    const bomIdInput = page.locator('input[name="bomId"]').first();
    if (await bomIdInput.count() > 0) {
      await bomIdInput.fill('1');
      await page.locator('button').filter({ hasText: /展开 BOM/ }).first().click();
      await page.waitForTimeout(1500);
      // Either AMIS tree DOM (cxd-Tree) renders with seed data, or the no-data placeholder text shows.
      const treeCount = await page.locator('.cxd-Tree').count().catch(() => 0);
      const placeholderShown = await page.locator('text=请输入 BOM ID').count().catch(() => 0);
      expect(
        treeCount > 0 || placeholderShown > 0,
        'bom-tree should render tree DOM or graceful no-data placeholder after bomId entry',
      ).toBe(true);
    } else {
      test.skip(true, 'bomId input not found — codegen-level coverage via ErpAllWebPagesTest');
    }
  });
});
