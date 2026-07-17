import { test, loginAndNavigate } from '../fixtures';
import { assertSnapshot } from './_helper';
import { getEngine } from '../pages';
import type { Page } from '@playwright/test';

// Pixel-snapshot layer (plan 2026-07-17-2010-2 Phase 2).
//
// Representative subset of the 24 report pages, covering the four parameter
// shapes identified in reports.visual.spec.ts:
//   1. parameterized ID  — fin-income-statement (periodId)
//   2. zero-param        — md-material-price-list, crm-lead-conversion-funnel
//   3. date-param        — fin-ar-ap-aging (账龄基准日 date input)
//   4. string/number-param — cs-ticket-sla-csat-summary (ticketType)
//
// Page-driving (login → navigate → fill ID/date params → click 渲染报表 →
// wait for renderHtml GraphQL response) mirrors reports.visual.spec.ts so
// every snapshot has deterministic seed data behind it. assertSnapshot adds
// font hardening + canonical mask (header) + 1% ratio tolerance. Reports
// emit HTML tables (no echarts canvas), so the canvas canonical mask is a
// no-op here; the header mask covers the AMIS shell's user-name/avatar
// dynamic region.
//
// Baseline update: when a report .xpt.xml template or page.yaml changes
// intentionally, re-record with `--update-snapshots`.

interface ReportSnapshot {
  reportLabel: string;
  route: string;
  fill?: Record<string, string>;
  fillDates?: Record<string, string>;
}

async function driveReportAndSnapshot(page: Page, cfg: ReportSnapshot): Promise<void> {
  const renderResponsePromise = page.waitForResponse(
    (resp) => {
      if (!resp.url().includes('/graphql')) return false;
      const body = resp.request().postData() || '';
      return body.includes('renderHtml');
    },
    { timeout: 30_000 },
  );

  await loginAndNavigate(page, cfg.route);

  if (cfg.fill) {
    for (const [name, value] of Object.entries(cfg.fill)) {
      await page.locator(`input[name="${name}"]`).first().fill(value);
    }
  }

  if (cfg.fillDates) {
    const engine = getEngine();
    for (const [label, value] of Object.entries(cfg.fillDates)) {
      await engine.dateInputByLabel(page, label).fill(value);
    }
  }

  await page.getByRole('button', { name: /渲染报表|Render/ }).first().click();
  await renderResponsePromise;

  // Reports emit static HTML; allow DOM injection + table render to settle.
  await page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => {});
  await page.waitForTimeout(500);

  await assertSnapshot(page, {
    name: `${cfg.reportLabel}-report.png`,
    skipEchartsSettle: true,
  });
}

test.describe('Report pixel-snapshot baseline (representative subset)', () => {
  // 1. parameterized ID
  test('fin-income-statement snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'fin-income-statement',
      route: '/income-statement',
      fill: { periodId: '1' },
    });
  });

  // 2. zero-param
  test('md-material-price-list snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'md-material-price-list',
      route: '/material-price-list',
    });
  });

  test('crm-lead-conversion-funnel snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'crm-lead-conversion-funnel',
      route: '/lead-conversion-funnel',
    });
  });

  // 3. date-param (input-date, no fillable name)
  test('fin-ar-ap-aging snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'fin-ar-ap-aging',
      route: '/ar-ap-aging',
      fillDates: { '账龄基准日': '2026-07-08' },
    });
  });

  // 4. string/number-param
  test('cs-ticket-sla-csat-summary snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'cs-ticket-sla-csat-summary',
      route: '/ticket-sla-csat-summary',
      fill: { ticketType: '1' },
    });
  });

  // 5. number-param + table-heavy (CRP load)
  test('mfg-crp-load snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'mfg-crp-load',
      route: '/crp-load-report',
      fill: { workcenterId: '1' },
    });
  });
});
