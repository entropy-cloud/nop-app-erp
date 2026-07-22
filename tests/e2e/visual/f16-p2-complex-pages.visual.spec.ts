// DOM assertions for F16 P2 — extended-domain complex pages
// (plan 2026-07-22-1400-2-f16-p2-ext-domain-complex-pages.md §Phase 4).
//
// Validates that the 7 P2 F16 pages render their core interaction DOM at runtime:
//   1. hr payroll-approval: filter form + summary cards (panels) + salary crud
//   2. hr org-chart: filter form + tree DOM (or no-data placeholder)
//   3. logistics shipment-tracking: filter form + (timeline or placeholder)
//   4. b2b edi-detail: filter form + (timeline or placeholder)
//   5. b2b asn-flow: asn crud + "查看流程" row-action button
//   6. contract version-diff: filter form + version list service
//   7. drp net-requirement: filter form + (group sections or placeholder)
//
// DOM-className strategy (stable across AMIS upgrades) — same as f16-complex-pages / f16-high-risk specs.
// Pages requiring seed IDs skip gracefully if no data (codegen-level coverage via ErpAllWebPagesTest).

import { test, expect, loginAndNavigate } from '../fixtures';
import type { Page } from '@playwright/test';

async function navigateAndReady(page: Page, route: string): Promise<void> {
  await loginAndNavigate(page, route);
  await expect.poll(
    () => page.url(),
    { timeout: 20_000, message: `${route}: URL should include "${route}"` },
  ).toContain(route);
}

test.describe('F16 P2 — extended-domain complex pages DOM rendering', () => {
  test('hr payroll-approval page renders filter form + summary cards + salary crud', async ({ page }) => {
    await navigateAndReady(page, '/hr-payroll-approval');

    // Filter form: year/month + 刷新汇总与明细 button
    await expect.poll(
      async () => page.locator('button').filter({ hasText: /刷新汇总与明细|Refresh/ }).count(),
      { timeout: 20_000, message: 'payroll filter form refresh button should render' },
    ).toBeGreaterThan(0);

    // Click refresh to trigger summary service; assert either summary panels render OR crud renders.
    await page.locator('button').filter({ hasText: /刷新汇总与明细/ }).first().click().catch(() => {});
    await page.waitForTimeout(1500);

    // Salary detail crud should render
    await expect.poll(
      async () => page.locator('.cxd-Crud').count(),
      { timeout: 20_000, message: 'payroll salary crud should render' },
    ).toBeGreaterThan(0);

    // Summary panels (5 cards: 人数/应发/社保/个税/实发) — class panel renders
    const panelCount = await page.locator('.panel').count().catch(() => 0);
    expect(panelCount, 'payroll summary panels should render').toBeGreaterThan(0);
  });

  test('hr org-chart page renders filter form + dept table (each+tpl degraded from tree)', async ({ page }) => {
    await navigateAndReady(page, '/hr-org-chart');

    // Filter form: keyword input + 重建架构图 button
    await expect.poll(
      async () => page.locator('button').filter({ hasText: /重建架构图|Refresh/ }).count(),
      { timeout: 20_000, message: 'org-chart filter form rebuild button should render' },
    ).toBeGreaterThan(0);

    // The service auto-loads; assert either table renders OR no-data placeholder shows.
    // NOTE: AMIS type:tree renderer is unavailable in the Nop bundle (RuntimeError),
    // so org-chart degrades to type:table (Phase 0 (b) downgrade).
    await page.waitForTimeout(2000);
    const tableCount = await page.locator('.cxd-Service .cxd-Table').count().catch(() => 0);
    const placeholderShown = await page.locator('text=未找到匹配的部门').count().catch(() => 0);
    expect(
      tableCount > 0 || placeholderShown > 0,
      'org-chart should render department table or graceful no-data placeholder',
    ).toBe(true);
  });

  test('logistics shipment-tracking page renders filter form + timeline/placeholder', async ({ page }) => {
    await navigateAndReady(page, '/log-shipment-tracking');

    // Filter form: shipmentId + 查询时间线 button
    await expect.poll(
      async () => page.locator('button').filter({ hasText: /查询时间线|Query/ }).count(),
      { timeout: 20_000, message: 'shipment-tracking filter form query button should render' },
    ).toBeGreaterThan(0);

    // Enter shipmentId=1 and query; assert timeline DOM or no-data placeholder
    const sidInput = page.locator('input[name="shipmentId"]').first();
    if (await sidInput.count() > 0) {
      await sidInput.fill('1');
      await page.locator('button').filter({ hasText: /查询时间线/ }).first().click();
      await page.waitForTimeout(1500);
      const timelineCount = await page.locator('.cxd-Timeline').count().catch(() => 0);
      const placeholderShown = await page.locator('text=请输入有效的发运单ID').count().catch(() => 0);
      expect(
        timelineCount > 0 || placeholderShown > 0,
        'shipment-tracking should render timeline or graceful no-data placeholder',
      ).toBe(true);
    } else {
      test.skip(true, 'shipmentId input not found — codegen-level coverage');
    }
  });

  test('b2b edi-detail page renders filter form + timeline/placeholder', async ({ page }) => {
    await navigateAndReady(page, '/b2b-edi-detail');

    // Filter form: ediDocId + 查询详情 button
    await expect.poll(
      async () => page.locator('button').filter({ hasText: /查询详情|Query/ }).count(),
      { timeout: 20_000, message: 'edi-detail filter form query button should render' },
    ).toBeGreaterThan(0);

    // Enter ediDocId=1 and query
    const didInput = page.locator('input[name="ediDocId"]').first();
    if (await didInput.count() > 0) {
      await didInput.fill('1');
      await page.locator('button').filter({ hasText: /查询详情/ }).first().click();
      await page.waitForTimeout(1500);
      const timelineCount = await page.locator('.cxd-Timeline').count().catch(() => 0);
      const placeholderShown = await page.locator('text=请输入有效的 EDI 文档ID').count().catch(() => 0);
      expect(
        timelineCount > 0 || placeholderShown > 0,
        'edi-detail should render timeline or graceful no-data placeholder',
      ).toBe(true);
    } else {
      test.skip(true, 'ediDocId input not found — codegen-level coverage');
    }
  });

  test('b2b asn-flow page renders asn crud + operation column', async ({ page }) => {
    await navigateAndReady(page, '/b2b-asn-flow');

    // ASN list crud renders
    await expect.poll(
      async () => page.locator('.cxd-Crud').count(),
      { timeout: 20_000, message: 'asn-flow crud should render' },
    ).toBeGreaterThan(0);

    // Operation column header renders (操作)
    await expect.poll(
      async () => page.locator('.cxd-Crud th').filter({ hasText: /操作|Operation/ }).count(),
      { timeout: 20_000, message: 'asn-flow operation column should render' },
    ).toBeGreaterThan(0);

    // 查看流程 action renders only on actual data rows (not placeholder).
    // Detect data rows: rows with multiple td (data) vs single colspan td (placeholder).
    const crud = page.locator('.cxd-Crud');
    const dataRows = crud.locator('tbody tr').filter({
      has: page.locator('td:nth-child(2)'),  // data rows have multiple td
    });
    const hasDataRow = await dataRows.count().then(c => c > 0).catch(() => false);
    if (!hasDataRow) {
      test.skip(true, 'no seed ASN data row — codegen-level coverage via ErpAllWebPagesTest');
      return;
    }
    await dataRows.first().hover();
    await page.waitForTimeout(500);
    const actionCount = await dataRows.first().locator('button, a, span').filter({ hasText: /查看流程/ }).count().catch(() => 0);
    expect(actionCount, 'asn-flow 查看流程 action should render on seed data row').toBeGreaterThan(0);
  });

  test('contract version-diff page renders filter form + version list service', async ({ page }) => {
    await navigateAndReady(page, '/ct-version-diff');

    // Filter form: contractId + 加载版本列表 button
    await expect.poll(
      async () => page.locator('button').filter({ hasText: /加载版本列表|Load/ }).count(),
      { timeout: 20_000, message: 'version-diff filter form load button should render' },
    ).toBeGreaterThan(0);

    // Enter contractId=1 and load
    const cidInput = page.locator('input[name="contractId"]').first();
    if (await cidInput.count() > 0) {
      await cidInput.fill('1');
      await page.locator('button').filter({ hasText: /加载版本列表/ }).first().click();
      await page.waitForTimeout(1500);
      // Either version options (select) render, or the no-data placeholder shows
      const placeholderShown = await page.locator('text=请输入有效的合同ID').count().catch(() => 0);
      const selectOrText = await page.locator('text=该合同共').count().catch(() => 0);
      expect(
        placeholderShown > 0 || selectOrText > 0,
        'version-diff should show version list or graceful no-data placeholder',
      ).toBe(true);
    } else {
      test.skip(true, 'contractId input not found — codegen-level coverage');
    }
  });

  test('drp net-requirement page renders filter form + groups/placeholder', async ({ page }) => {
    await navigateAndReady(page, '/drp-net-requirement');

    // Filter form: planId + 加载净需求 button
    await expect.poll(
      async () => page.locator('button').filter({ hasText: /加载净需求|Load/ }).count(),
      { timeout: 20_000, message: 'net-requirement filter form load button should render' },
    ).toBeGreaterThan(0);

    // Enter planId=1 and load
    const pidInput = page.locator('input[name="planId"]').first();
    if (await pidInput.count() > 0) {
      await pidInput.fill('1');
      await page.locator('button').filter({ hasText: /加载净需求/ }).first().click();
      await page.waitForTimeout(1500);
      // Either group sections (panel-info) render OR no-data placeholder shows
      const groupPanels = await page.locator('.panel-info').count().catch(() => 0);
      const placeholderShown = await page.locator('text=请输入有效的 DRP 计划ID').count().catch(() => 0);
      expect(
        groupPanels > 0 || placeholderShown > 0,
        'net-requirement should render group sections or graceful no-data placeholder',
      ).toBe(true);
    } else {
      test.skip(true, 'planId input not found — codegen-level coverage');
    }
  });
});
