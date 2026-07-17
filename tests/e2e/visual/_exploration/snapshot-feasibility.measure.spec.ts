import { test, expect, loginAndNavigate } from '../../fixtures';
import type { Page } from '@playwright/test';
import * as fs from 'fs';
import * as path from 'path';

// Phase 1 explore — strict-diff measurement.
//
// Forces toHaveScreenshot with maxDiffPixels: 0 so that on any cross-run
// pixel-level divergence, Playwright reports the exact diff pixel count +
// ratio in the failure message. We collect those numbers across 3 fresh
// runs into a JSON artifact so the Decision record has concrete diff-rate
// evidence per variant (rather than only pass/fail).
//
// This spec is run AFTER baseline capture. On the first measurement run,
// baselines already exist; the spec fails (intentionally) with diff stats
// that the runner scrapes from the output and serializes into
// _exploration-measurements.json next to the snapshots.

const FINANCE_ROUTE = '/fin-dashboard-main';
const MASTER_DATA_ROUTE = '/md-dashboard-main';

const FONT_HARDENED_STYLE = `
  *, *::before, *::after {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "PingFang SC", "Microsoft YaHei", sans-serif !important;
  }
`;

async function waitForEchartsSettle(page: Page): Promise<void> {
  await page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => {});
  await page.waitForTimeout(1500);
}

async function driveFinance(page: Page): Promise<void> {
  const kpiPromise = page.waitForResponse(
    (resp) =>
      resp.url().includes('/graphql') &&
      (resp.request().postData() || '').includes('getDashboardKpi'),
    { timeout: 30_000 },
  );
  await loginAndNavigate(page, FINANCE_ROUTE);
  await kpiPromise;
  await page.locator('input[name="periodId"]').first().fill('1');
  const reloadPromise = page.waitForResponse(
    (resp) =>
      resp.url().includes('/graphql') &&
      (resp.request().postData() || '').includes('getDashboardKpi'),
    { timeout: 30_000 },
  );
  await page.getByRole('button', { name: /刷新|Refresh/ }).first().click();
  await reloadPromise;
  await expect(page.locator('.border.rounded.p-3').first()).toBeVisible({ timeout: 15_000 });
  await waitForEchartsSettle(page);
}

async function driveMasterData(page: Page): Promise<void> {
  const kpiPromise = page.waitForResponse(
    (resp) =>
      resp.url().includes('/graphql') &&
      (resp.request().postData() || '').includes('getDashboardKpi'),
    { timeout: 30_000 },
  );
  await loginAndNavigate(page, MASTER_DATA_ROUTE);
  await kpiPromise;
  await expect(page.locator('.border.rounded.p-3').first()).toBeVisible({ timeout: 15_000 });
  await waitForEchartsSettle(page);
}

const measures: Record<string, { passed: boolean; msg: string }> = {};

test.describe('Phase 1 explore — strict diff measurement (force-report)', () => {
  test('finance v-a-bare strict', async ({ page }) => {
    await driveFinance(page);
    try {
      await expect(page).toHaveScreenshot('finance-v-a-bare.png', { maxDiffPixels: 0 });
      measures['finance-v-a-bare'] = { passed: true, msg: '0 diff pixels (exact match)' };
    } catch (e) {
      measures['finance-v-a-bare'] = { passed: false, msg: String(e).slice(0, 1500) };
      throw e;
    }
  });

  test('finance v-b-font strict', async ({ page }) => {
    await driveFinance(page);
    await page.addStyleTag({ content: FONT_HARDENED_STYLE });
    await page.waitForTimeout(300);
    try {
      await expect(page).toHaveScreenshot('finance-v-b-font.png', { maxDiffPixels: 0 });
      measures['finance-v-b-font'] = { passed: true, msg: '0 diff pixels (exact match)' };
    } catch (e) {
      measures['finance-v-b-font'] = { passed: false, msg: String(e).slice(0, 1500) };
      throw e;
    }
  });

  test('finance v-c-mask strict', async ({ page }) => {
    await driveFinance(page);
    try {
      await expect(page).toHaveScreenshot('finance-v-c-mask.png', {
        mask: [page.locator('canvas')],
        maxDiffPixels: 0,
      });
      measures['finance-v-c-mask'] = { passed: true, msg: '0 diff pixels (exact match)' };
    } catch (e) {
      measures['finance-v-c-mask'] = { passed: false, msg: String(e).slice(0, 1500) };
      throw e;
    }
  });

  test('master-data v-a-bare strict', async ({ page }) => {
    await driveMasterData(page);
    try {
      await expect(page).toHaveScreenshot('master-data-v-a-bare.png', { maxDiffPixels: 0 });
      measures['master-data-v-a-bare'] = { passed: true, msg: '0 diff pixels (exact match)' };
    } catch (e) {
      measures['master-data-v-a-bare'] = { passed: false, msg: String(e).slice(0, 1500) };
      throw e;
    }
  });

  test('master-data v-b-font strict', async ({ page }) => {
    await driveMasterData(page);
    await page.addStyleTag({ content: FONT_HARDENED_STYLE });
    await page.waitForTimeout(300);
    try {
      await expect(page).toHaveScreenshot('master-data-v-b-font.png', { maxDiffPixels: 0 });
      measures['master-data-v-b-font'] = { passed: true, msg: '0 diff pixels (exact match)' };
    } catch (e) {
      measures['master-data-v-b-font'] = { passed: false, msg: String(e).slice(0, 1500) };
      throw e;
    }
  });

  test.afterAll(() => {
    const outDir = __dirname;
    const outPath = path.join(outDir, '_exploration-measurements.json');
    // Merge with existing measurements so successive runs accumulate.
    let existing: Record<string, any> = {};
    if (fs.existsSync(outPath)) {
      try { existing = JSON.parse(fs.readFileSync(outPath, 'utf8')); } catch { existing = {}; }
    }
    const runId = process.env.EXPLORE_RUN_ID || `run-${Date.now()}`;
    existing[runId] = measures;
    fs.writeFileSync(outPath, JSON.stringify(existing, null, 2));
  });
});
