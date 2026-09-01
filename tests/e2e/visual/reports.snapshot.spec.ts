import { test, loginAndNavigate } from '../fixtures';
import { assertSnapshot, pickFluxDate } from './_helper';
import type { Page } from '@playwright/test';

// Pixel-snapshot layer (plan 2026-07-17-2010-2 Phase 2; /r/ predicate realigned
// by plan 2026-09-01-0527-2).
//
// Representative subset of the 24 report pages, covering the four parameter
// shapes identified in reports.visual.spec.ts:
//   1. parameterized ID  — fin-income-statement (periodId)
//   2. zero-param        — md-material-price-list, crm-lead-conversion-funnel
//   3. date-param        — fin-ar-ap-aging (账龄基准日 date input)
//   4. string/number-param — cs-ticket-sla-csat-summary (ticketType)
//
// Page-driving (login → navigate → auto-fetch /r/<Biz>__renderHtml on load →
// fill ID/date params → wait for the fill-triggered reload) mirrors
// reports.visual.spec.ts so every snapshot has deterministic seed data behind
// it. assertSnapshot adds font hardening + canonical mask (header) + 1% ratio
// tolerance. Reports emit HTML tables (no echarts canvas), so the canvas
// canonical mask is a no-op here; the header mask covers the shell's
// user-name/avatar dynamic region.
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
  // Flux report pages auto-fetch on load (data-source action ajax → REST
  // /r/<Biz>__renderHtml). Register the listener BEFORE navigation; no 渲染报表
  // button click is needed (and clicking is unreliable/deduped under flux).
  const responsePredicate = (resp: import('@playwright/test').Response): boolean => {
    if (!resp.url().includes('/r/')) return false;
    return (
      resp.url().includes('renderHtml') || decodeURIComponent(resp.url()).includes('renderHtml')
    );
  };
  const initialResponsePromise = page.waitForResponse(responsePredicate, { timeout: 30_000 });

  await loginAndNavigate(page, cfg.route);
  await initialResponsePromise;

  const hasValueFills = cfg.fill && Object.keys(cfg.fill).length > 0;
  const hasDateFills = cfg.fillDates && Object.keys(cfg.fillDates).length > 0;
  if (hasValueFills || hasDateFills) {
    let reloadExpected = hasDateFills;
    const pendingFills: Array<() => Promise<void>> = [];
    if (hasValueFills) {
      for (const [name, value] of Object.entries(cfg.fill!)) {
        const input = page.locator(`input[name="${name}"]`).first();
        const current = await input.inputValue();
        if (current !== value) {
          pendingFills.push(() => input.fill(value));
          reloadExpected = true;
        }
      }
    }
    if (reloadExpected) {
      const reloadResponsePromise = page.waitForResponse(responsePredicate, { timeout: 30_000 });
      for (const fillOp of pendingFills) {
        await fillOp();
      }
      if (hasDateFills) {
        for (const [label, value] of Object.entries(cfg.fillDates!)) {
          await pickFluxDate(page, label, value);
        }
      }
      await reloadResponsePromise;
    }
  }

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
