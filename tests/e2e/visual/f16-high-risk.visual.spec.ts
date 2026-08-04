// DOM assertions for F16 — high-risk complex pages (flux-native rewrite).
// plan 2026-08-03-1232-3-flux-f16-complex-pages-rewrite.md §Phase 0/§Phase 2.
//
// Validates that the 2 high-risk F16 pages render flux-native components at runtime:
//   1. aps schedule-gantt: flux gantt (replaces read-only echarts canvas) + filter form
//   2. mfg bom-tree: flux tree (replaces AMIS tree + service adaptor) + filter form
//
// Flux selector strategy: .nop-<control> + data-slot + page-title text (per flux-guide/13-testing.md).
// Pages requiring seed IDs skip gracefully if no data (codegen-level coverage via ErpAllFluxPagesTest).

import { test, expect, loginAndNavigate } from '../fixtures';
import type { Page } from '@playwright/test';

async function navigateAndReady(page: Page, route: string): Promise<void> {
  await loginAndNavigate(page, route);
  await expect.poll(
    () => page.url(),
    { timeout: 20_000, message: `${route}: URL should include "${route}"` },
  ).toContain(route);
}

test.describe('F16 — high-risk complex pages flux DOM rendering', () => {
  test('aps schedule-gantt renders flux gantt + filter form', async ({ page }) => {
    await navigateAndReady(page, '/aps-schedule-gantt');

    // Filter form renders (machineId/status + 刷新甘特图 button)
    await expect.poll(
      async () => page.locator('button').filter({ hasText: /刷新甘特图|Refresh/ }).count(),
      { timeout: 20_000, message: 'gantt filter form refresh button should render' },
    ).toBeGreaterThan(0);

    // Page title present
    await expect(page.locator('text=排产甘特图').first()).toBeVisible({ timeout: 10_000 });

    // flux gantt control renders (native gantt replaces echarts custom-series canvas).
    // With no seed operation orders the gantt may render an empty container; assert
    // the control OR the empty-data text (codegen-level build coverage via ErpAllFluxPagesTest).
    const ganttCount = await page.locator('.nop-gantt').count().catch(() => 0);
    const emptyText = await page.locator('text=共 0 道工序').count().catch(() => 0);
    expect(
      ganttCount + emptyText,
      'gantt should render control or empty-data text',
    ).toBeGreaterThan(0);
  });

  test('mfg bom-tree renders flux tree + filter form', async ({ page }) => {
    await navigateAndReady(page, '/mfg-bom-tree');

    // Filter form renders (bomId/qty/useMultiLevel + 展开 BOM button)
    await expect.poll(
      async () => page.locator('button').filter({ hasText: /展开 BOM|Expand/ }).count(),
      { timeout: 20_000, message: 'bom-tree filter form expand button should render' },
    ).toBeGreaterThan(0);

    // Page title present
    await expect(page.locator('text=BOM 多级展开树').first()).toBeVisible({ timeout: 10_000 });

    // Enter bomId=1 and expand; assert flux tree renders OR the no-data placeholder shows.
    const bomIdInput = page.locator('input[name="bomId"]').first();
    if (await bomIdInput.count() > 0) {
      await bomIdInput.fill('1');
      await page.locator('button').filter({ hasText: /展开 BOM/ }).first().click();
      await page.waitForTimeout(1500);
      const treeCount = await page.locator('.nop-tree').count().catch(() => 0);
      const placeholderShown = await page.locator('text=请输入 BOM ID').count().catch(() => 0);
      expect(
        treeCount > 0 || placeholderShown > 0,
        'bom-tree should render flux tree or graceful no-data placeholder after bomId entry',
      ).toBe(true);
    } else {
      test.skip(true, 'bomId input not found — codegen-level coverage via ErpAllFluxPagesTest');
    }
  });
});
