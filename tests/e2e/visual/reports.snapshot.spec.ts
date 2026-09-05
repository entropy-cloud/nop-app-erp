import { test, loginAndNavigate } from '../fixtures';
import { assertReportPixelSnapshot, assertSnapshot, pickFluxDate } from './_helper';
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

async function driveReportToState(page: Page, cfg: ReportSnapshot): Promise<void> {
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
}

/** Let the renderHtml table injection settle before capture (shared by both
 * capture paths). */
async function waitForReportRenderSettle(page: Page): Promise<void> {
  await page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => {});
  await page.waitForTimeout(500);
}

async function driveReportAndSnapshot(page: Page, cfg: ReportSnapshot): Promise<void> {
  await driveReportToState(page, cfg);

  // Reports emit static HTML; allow DOM injection + table render to settle.
  await waitForReportRenderSettle(page);

  await assertSnapshot(page, {
    name: `${cfg.reportLabel}-report.png`,
    skipEchartsSettle: true,
  });
}

/**
 * Default-param-state capture (plan 2026-09-03-0400-3 M2.3). Same driving
 * paradigm, but the snapshot goes through the `assertReportPixelSnapshot`
 * subset and the baseline name carries the `-report-default` suffix so the
 * new baselines are orthogonal to the existing per-page ones (zero existing
 * baseline modified).
 */
async function driveReportDefaultSnapshot(page: Page, cfg: ReportSnapshot): Promise<void> {
  await driveReportToState(page, cfg);
  await waitForReportRenderSettle(page);

  await assertReportPixelSnapshot(page, {
    name: `${cfg.reportLabel}-report-default.png`,
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

  // 6. finance — balance sheet (parameterized ID)
  test('fin-balance-sheet snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'fin-balance-sheet',
      route: '/balance-sheet',
      fill: { periodId: '1' },
    });
  });

  // 7. finance — cash flow (parameterized ID)
  test('fin-cash-flow snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'fin-cash-flow',
      route: '/cash-flow',
      fill: { periodId: '1' },
    });
  });

  // 8. finance — period close report (parameterized ID)
  test('fin-period-close-report snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'fin-period-close-report',
      route: '/period-close-report',
      fill: { periodId: '1' },
    });
  });

  // 9. manufacturing — production variance (zero-param)
  test('mfg-production-variance snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'mfg-production-variance',
      route: '/production-variance-report',
    });
  });

  // 10. manufacturing — forecast variance (zero-param)
  test('mfg-forecast-variance snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'mfg-forecast-variance',
      route: '/forecast-variance-report',
    });
  });

  // 11. assets — depreciation detail (zero-param)
  test('ast-asset-depreciation-detail snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'ast-asset-depreciation-detail',
      route: '/asset-depreciation-detail',
    });
  });

  // 12. assets — disposal detail (zero-param)
  test('ast-asset-disposal-detail snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'ast-asset-disposal-detail',
      route: '/asset-disposal-detail',
    });
  });

  // 13. maintenance — downtime summary (number-param)
  test('mnt-downtime-summary snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'mnt-downtime-summary',
      route: '/downtime-summary',
      fill: { equipmentId: '1' },
    });
  });

  // 14. maintenance — maintenance history (zero-param)
  test('mnt-maintenance-history snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'mnt-maintenance-history',
      route: '/maintenance-history',
    });
  });

  // 15. projects — cost summary (zero-param)
  test('prj-project-cost-summary snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'prj-project-cost-summary',
      route: '/project-cost-summary',
    });
  });

  // 16. projects — timesheet detail (number-param)
  test('prj-timesheet-detail snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'prj-timesheet-detail',
      route: '/timesheet-detail',
      fill: { projectId: '1' },
    });
  });

  // 17. quality — inspection summary (zero-param)
  test('qa-inspection-summary snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'qa-inspection-summary',
      route: '/inspection-summary',
    });
  });

  // 18. quality — NCR/CAPA summary (zero-param)
  test('qa-ncr-capa-summary snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'qa-ncr-capa-summary',
      route: '/ncr-capa-summary',
    });
  });

  // 19. master-data — partner list (zero-param)
  test('md-partner-list snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'md-partner-list',
      route: '/partner-list',
    });
  });

  // 20. inventory — trace report (zero-param, data may be empty)
  test('inv-inventory-trace snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'inv-inventory-trace',
      route: '/inventory-trace-report',
    });
  });

  // 21. CRM — forecast accuracy (number-param)
  test('crm-forecast-accuracy snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'crm-forecast-accuracy',
      route: '/forecast-accuracy',
      fill: { forecastId: '1' },
    });
  });

  // 22. HR — employee net balance (zero-param)
  test('hr-employee-net-balance snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'hr-employee-net-balance',
      route: '/employee-net-balance',
    });
  });

  // 23. HR — payroll simulation comparison (number-param, page.yaml default 1)
  test('hr-payroll-simulation-comparison snapshot', async ({ page }) => {
    await driveReportAndSnapshot(page, {
      reportLabel: 'hr-payroll-simulation-comparison',
      route: '/payroll-simulation-comparison',
      fill: { simulationId: '1' },
    });
  });
});

// Default-param-state expansion (plan 2026-09-03-0400-3 M2.3). The 24 tests
// above pin exactly one state per page; for the 6 parameterized pages whose
// page.yaml carries NO default, the missing default-render state (empty
// params → backend full-extent render over frozen seed data) is pinned here
// via assertReportPixelSnapshot. Baselines are named
// `<label>-report-default.png` and are orthogonal to the ones above — zero
// existing baseline is modified. The 5 pages whose page.yaml defaults the
// param to the seeded value 1 (fin ×4 + hr-payroll-simulation-comparison)
// collapse both states into the existing snapshots above (matrix Appendix
// A.2-1 of the plan), so they have no test here.
test.describe('Report pixel-snapshot default-param states (M2.3 expansion)', () => {
  // Batch 1 (m23-default-b1)
  test('fin-ar-ap-aging default-state snapshot', async ({ page }) => {
    await driveReportDefaultSnapshot(page, {
      reportLabel: 'fin-ar-ap-aging',
      route: '/ar-ap-aging',
    });
  });

  test('mfg-crp-load default-state snapshot', async ({ page }) => {
    await driveReportDefaultSnapshot(page, {
      reportLabel: 'mfg-crp-load',
      route: '/crp-load-report',
    });
  });

  test('mnt-downtime-summary default-state snapshot', async ({ page }) => {
    await driveReportDefaultSnapshot(page, {
      reportLabel: 'mnt-downtime-summary',
      route: '/downtime-summary',
    });
  });

  // Batch 2 (m23-default-b2)
  test('prj-timesheet-detail default-state snapshot', async ({ page }) => {
    await driveReportDefaultSnapshot(page, {
      reportLabel: 'prj-timesheet-detail',
      route: '/timesheet-detail',
    });
  });

  test('cs-ticket-sla-csat-summary default-state snapshot', async ({ page }) => {
    await driveReportDefaultSnapshot(page, {
      reportLabel: 'cs-ticket-sla-csat-summary',
      route: '/ticket-sla-csat-summary',
    });
  });

  test('crm-forecast-accuracy default-state snapshot', async ({ page }) => {
    await driveReportDefaultSnapshot(page, {
      reportLabel: 'crm-forecast-accuracy',
      route: '/forecast-accuracy',
    });
  });
});
