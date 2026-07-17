import { test, expect, loginAndNavigate } from '../../fixtures';
import type { Page } from '@playwright/test';

// Phase 1 Explore spec (plan 2026-07-17-2010-2).
//
// Drives two representative dashboards — finance (parameterized, echarts) and
// master-data (non-parameterized) — and exercises toHaveScreenshot under four
// incrementally hardened variants to measure cross-run diff stability. The
// variant names embed the strategy so the resulting snapshot filenames
// self-document the matrix:
//
//   v-a-bare            : toHaveScreenshot, no mask / no font hardening / no tolerance
//   v-b-font            : + inject a hard font-family chain via addStyleTag
//   v-c-mask            : + mask known dynamic regions (chart canvas, filter
//                         date display, user name) so per-run pixel noise is
//                         eliminated from those regions
//   v-d-tolerance       : + maxDiffPixelRatio tolerance so residual sub-pixel
//                         anti-aliasing differences below the threshold pass
//
// Each variant has its own snapshot name suffix, so the same baseline run
// captures four distinct reference images and subsequent runs reveal which
// variants are stable across fresh browser contexts.
//
// The exploration is gated so that on the very first run the spec captures
// baselines for every variant (no diffs reported). On subsequent fresh runs,
// Playwright reports per-snapshot diff pixel counts in the failure message
// when a variant exceeds its tolerance, which is the raw evidence this Phase
// collects.
//
// This spec lives under `_exploration/` and is excluded from the regular
// `npx playwright test` suite name pattern by virtue of its directory (it is
// invoked explicitly during Phase 1 evidence collection only). It does not
// register production snapshot baselines.

const FINANCE_ROUTE = '/fin-dashboard-main';
const MASTER_DATA_ROUTE = '/md-dashboard-main';

const FONT_HARDEG_STYLE = `
  *, *::before, *::after {
    font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, "PingFang SC", "Microsoft YaHei", sans-serif !important;
  }
`;

async function maskDynamicRegions(page: Page): Promise<void> {
  // Mask canvas (echarts draws to canvas; final animation frame timing is not
  // pixel-stable across runs) by zeroing it via a deterministic redraw that
  // paints over it.
  await page.evaluate(() => {
    document.querySelectorAll('canvas').forEach((cv) => {
      const el = cv as HTMLCanvasElement;
      const ctx = el.getContext('2d');
      if (ctx) {
        ctx.fillStyle = '#ffffff';
        ctx.fillRect(0, 0, el.width, el.height);
      }
    });
  });
}

async function waitForEchartsSettle(page: Page): Promise<void> {
  // Give echarts animation a chance to settle by polling for network idle
  // + a fixed grace; AMIS charts animate by default and the final canvas
  // bitmap can take a few hundred ms to stabilize after data binding.
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
  // Lock the filter to a deterministic period (periodId=1 = seed period).
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

test.describe('Phase 1 explore — toHaveScreenshot feasibility matrix', () => {
  test('finance dashboard — 4 variants', async ({ page }) => {
    await driveFinance(page);

    // v-a-bare
    await expect(page).toHaveScreenshot('finance-v-a-bare.png');

    // v-b-font
    await page.addStyleTag({ content: FONT_HARDEG_STYLE });
    await page.waitForTimeout(300);
    await expect(page).toHaveScreenshot('finance-v-b-font.png');

    // v-c-mask (re-drive to reset filter state for a clean capture, then mask)
    await maskDynamicRegions(page);
    await expect(page).toHaveScreenshot('finance-v-c-mask.png', {
      mask: [page.locator('canvas')],
    });

    // v-d-tolerance (combine font + mask + tolerance)
    await expect(page).toHaveScreenshot('finance-v-d-tolerance.png', {
      mask: [page.locator('canvas')],
      maxDiffPixelRatio: 0.01,
    });
  });

  test('master-data dashboard — 4 variants', async ({ page }) => {
    await driveMasterData(page);

    // v-a-bare (no chart, fewer dynamic regions)
    await expect(page).toHaveScreenshot('master-data-v-a-bare.png');

    // v-b-font
    await page.addStyleTag({ content: FONT_HARDEG_STYLE });
    await page.waitForTimeout(300);
    await expect(page).toHaveScreenshot('master-data-v-b-font.png');

    // v-c-mask: no canvas on this dashboard; mask the user-facing
    // header area (which contains an avatar and current-user text) to
    // exercise the same primitive.
    await expect(page).toHaveScreenshot('master-data-v-c-mask.png', {
      mask: [page.locator('header').first()],
    });

    // v-d-tolerance
    await expect(page).toHaveScreenshot('master-data-v-d-tolerance.png', {
      mask: [page.locator('header').first()],
      maxDiffPixelRatio: 0.01,
    });
  });
});
