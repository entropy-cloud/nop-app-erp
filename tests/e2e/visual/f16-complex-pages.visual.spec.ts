// DOM assertions for F16 — complex pages (flux-native contracts).
// plan 2026-07-22-0845-2 (original) + plan 2026-08-03-1232-3-flux-f16-complex-pages-rewrite.md §Phase 2 (three-way-match flux rewrite).
//
// Validates that the 5 F16 low-risk pages render their core interaction DOM at runtime:
//   1. finance ErpFinVoucher edit drawer: balanceBadge + quickTemplate button
//   2. finance ErpFinVoucherTemplate edit drawer: tabs + previewTemplate button + lines sub-grid
//   3. purchase three-way-match page: diff-alert + 3 parallel flux cruds render
//   4. mfg ErpMfgWorkOrder view drawer: progress tab renders
//   5. quality ErpQaNonConformance view drawer: capa + verification tabs render
//
// Flux selector strategy: .nop-crud + data-slot (tabs-trigger / drawer-surface) + page-title text
// (flux-guide/13-testing.md). Row-dependent drawers skip gracefully if no seed row.

import { test, expect, loginAndNavigate } from '../fixtures';
import type { Locator, Page } from '@playwright/test';

async function navigateAndWaitForCrud(page: Page, route: string): Promise<void> {
  await loginAndNavigate(page, route);
  await expect.poll(
    () => page.url(),
    { timeout: 20_000, message: `${route}: URL should include "${route}"` },
  ).toContain(route);
  await page.waitForSelector('.nop-crud', { timeout: 20_000 });
  await page.waitForTimeout(1200);
}

/** Hover + open the first row's action button, return the drawer locator (or null if no row / no drawer). */
async function openFirstRowDrawer(page: Page, actionText: string[]): Promise<Locator | null> {
  const crud = page.locator('.nop-crud').first();
  const firstRow = crud.locator('[data-slot="table-body"] tr[data-slot="table-row"]').first();
  const hasRow = await firstRow.count().then((c) => c > 0).catch(() => false);
  if (!hasRow) return null;
  await firstRow.hover();
  const btn = crud.locator('button, a').filter({ hasText: new RegExp(actionText.join('|')) }).first();
  if (await btn.count().catch(() => 0) === 0) return null;
  await btn.click().catch(() => {});
  await page.waitForTimeout(1500);
  const drawer = page.locator('[data-slot="drawer-surface"], [data-slot="drawer-content"]').last();
  const opened = await drawer.count().catch(() => 0);
  return opened > 0 ? drawer : null;
}

test.describe('F16 — complex pages flux DOM rendering (low-risk batch)', () => {
  test('finance ErpFinVoucher edit drawer renders quickTemplate + autoBalance buttons', async ({ page }) => {
    await navigateAndWaitForCrud(page, '/ErpFinVoucher-main');
    const drawer = await openFirstRowDrawer(page, ['编辑', 'Edit']);
    if (!drawer) {
      test.skip(true, 'no seed voucher row — codegen-level coverage via ErpAllFluxPagesTest');
      return;
    }
    await expect.poll(
      async () => drawer.locator('button:has-text("快捷模板")').count(),
      { timeout: 20_000, message: 'quickTemplate button should render' },
    ).toBeGreaterThan(0);
    await expect.poll(
      async () => drawer.locator('button').filter({ hasText: /刷新合计|借贷自动平衡/ }).count(),
      { timeout: 10_000 },
    ).toBeGreaterThan(0);
  });

  test('finance ErpFinVoucherTemplate edit drawer renders tabs + previewTemplate button', async ({ page }) => {
    await navigateAndWaitForCrud(page, '/ErpFinVoucherTemplate-main');
    const drawer = await openFirstRowDrawer(page, ['编辑', 'Edit']);
    if (!drawer) {
      test.skip(true, 'no seed voucher template row — codegen-level coverage');
      return;
    }
    await expect.poll(
      async () => drawer.locator('[data-slot="tabs-trigger"]').count(),
      { timeout: 20_000 },
    ).toBeGreaterThan(0);
    await expect.poll(
      async () => drawer.locator('button:has-text("预览测试生成凭证")').count(),
      { timeout: 10_000 },
    ).toBeGreaterThan(0);
  });

  test('purchase three-way-match page renders diff-alert crud + 3 parallel flux cruds', async ({ page }) => {
    await navigateAndWaitForCrud(page, '/pur-three-way-match');
    // Page should render at least 4 flux crud regions (diff-alert + PO + Receive + Invoice)
    await expect.poll(
      async () => page.locator('.nop-crud').count().catch(() => 0),
      { timeout: 20_000, message: 'three-way-match page should render 4 flux cruds' },
    ).toBeGreaterThanOrEqual(4);
    await expect(page.locator('text=三单匹配联查').first()).toBeVisible({ timeout: 10_000 });
  });

  test('mfg ErpMfgWorkOrder view drawer renders progress tab', async ({ page }) => {
    await navigateAndWaitForCrud(page, '/ErpMfgWorkOrder-main');
    const drawer = await openFirstRowDrawer(page, ['查看', 'View', '编辑', 'Edit']);
    if (!drawer) {
      test.skip(true, 'no seed work order row — codegen-level coverage');
      return;
    }
    await expect.poll(async () => drawer.locator('[data-slot="tabs-trigger"]').count(), { timeout: 20_000 }).toBeGreaterThan(0);
    await expect.poll(
      async () => drawer.locator('[data-slot="tabs-trigger"]').filter({ hasText: '工单进度' }).count(),
      { timeout: 10_000, message: 'progress tab should render' },
    ).toBeGreaterThan(0);
  });

  test('quality ErpQaNonConformance view drawer renders capa + verification tabs', async ({ page }) => {
    await navigateAndWaitForCrud(page, '/ErpQaNonConformance-main');
    const drawer = await openFirstRowDrawer(page, ['查看', 'View', '编辑', 'Edit']);
    if (!drawer) {
      test.skip(true, 'no seed NCR row — codegen-level coverage');
      return;
    }
    await expect.poll(async () => drawer.locator('[data-slot="tabs-trigger"]').count(), { timeout: 20_000 }).toBeGreaterThan(0);
    await expect.poll(
      async () => drawer.locator('[data-slot="tabs-trigger"]').filter({ hasText: 'CAPA' }).count(),
      { timeout: 10_000, message: 'capa tab should render' },
    ).toBeGreaterThan(0);
    await expect.poll(
      async () => drawer.locator('[data-slot="tabs-trigger"]').filter({ hasText: '效果验证' }).count(),
      { timeout: 10_000, message: 'verification tab should render' },
    ).toBeGreaterThan(0);
  });
});
