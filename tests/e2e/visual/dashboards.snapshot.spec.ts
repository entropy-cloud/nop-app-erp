import { test, loginAndNavigate } from '../fixtures';
import { assertSnapshot, pickFluxDate } from './_helper';
import type { Page, Locator } from '@playwright/test';

// Pixel-snapshot layer (plan 2026-07-17-2010-2 Phase 2; inventory determinism
// by plan 2026-09-01-0527-2).
//
// Builds on the same dashboard flux-render pipeline as
// dashboards.visual.spec.ts (DOM-content layer), but asserts pixel-level
// layout stability via assertSnapshot. This layer catches regressions the
// DOM-content layer cannot: CSS misalignment, element overlap, chart
// dimension collapse, responsive breakpoint breakage — i.e. the structural
// blind spots of DOM-content assertions.
//
// Page-driving (login → navigate → wait for getDashboardKpi response → fill
// filters → wait for reload → wait for chart settle) is identical to the
// DOM-content layer, ensuring deterministic seed data backing every snapshot.
// assertSnapshot adds font hardening, canonical mask (header + canvas), and
// 1% ratio tolerance (Phase 1 selected approach).
//
// Baseline update: when a dashboard's page.yaml/view changes intentionally,
// re-record with `--update-snapshots`.

interface DashboardSnapshot {
  domain: string;
  route: string;
  filterValues?: Record<string, string>;
  /** Flux date-picker filters, keyed by the trigger's aria-label (field
   * label), driven via pickFluxDate (mirrors the DOM-content layer cfgs). */
  filterDates?: Record<string, string>;
  hasChart: boolean;
  /** Extra page-level masks for today-relative regions that page params
   * cannot pin (plan 2026-09-01-0527-2 inventory adjudication: trend chart
   * rolling window). Evaluated against the live page. */
  maskLocators?: (page: Page) => Locator[];
  /** For non-KPI dashboards that don't have getDashboardKpi endpoint. */
  skipKpiWait?: boolean;
}

async function driveAndSnapshot(page: Page, cfg: DashboardSnapshot): Promise<void> {
  const kpiAction = 'getDashboardKpi';
  let initialResponsePromise: Promise<import('@playwright/test').Response | null> | null = null;

  if (!cfg.skipKpiWait) {
    initialResponsePromise = page.waitForResponse(
      (resp) => {
        if (!resp.url().includes('/r/')) return false;
        return resp.url().includes(kpiAction) || decodeURIComponent(resp.url()).includes(kpiAction);
      },
      { timeout: 30_000 },
    );
  }

  await loginAndNavigate(page, cfg.route);

  if (initialResponsePromise) {
    await initialResponsePromise;
  }

  const hasValues = cfg.filterValues && Object.keys(cfg.filterValues).length > 0;
  const hasDates = cfg.filterDates && Object.keys(cfg.filterDates).length > 0;
  if (hasValues || hasDates) {
    for (const [name, value] of Object.entries(cfg.filterValues ?? {})) {
      await page.locator(`input[name="${name}"]`).first().fill(value);
    }
    for (const [label, value] of Object.entries(cfg.filterDates ?? {})) {
      await pickFluxDate(page, label, value);
    }
    if (!cfg.skipKpiWait) {
      const reloadResponsePromise = page.waitForResponse(
        (resp) => {
          if (!resp.url().includes('/r/')) return false;
          return resp.url().includes(kpiAction) || decodeURIComponent(resp.url()).includes(kpiAction);
        },
        { timeout: 30_000 },
      );
      await page.getByRole('button', { name: /刷新|Refresh/ }).first().click();
      await reloadResponsePromise;
    } else {
      await page.getByRole('button', { name: /刷新|Refresh/ }).first().click().catch(() => {});
      await page.waitForTimeout(1000);
    }
  }

  if (!cfg.skipKpiWait) {
    await page.locator('.border.rounded.p-3').first().waitFor({ state: 'visible', timeout: 15_000 });
  } else {
    // For non-KPI dashboards, wait for any content to render
    await page.waitForLoadState('networkidle', { timeout: 15_000 }).catch(() => {});
    await page.waitForTimeout(1000);
  }

  const extraMasks = cfg.maskLocators ? cfg.maskLocators(page) : [];
  await assertSnapshot(page, {
    name: `${cfg.domain}-dashboard.png`,
    skipEchartsSettle: !cfg.hasChart,
    ...(extraMasks.length > 0 ? { mask: extraMasks } : {}),
  });
}

test.describe('Dashboard pixel-snapshot baseline (10 domains)', () => {
  test('finance dashboard snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'finance',
      route: '/fin-dashboard-main',
      filterValues: { periodId: '1' },
      hasChart: true,
    });
  });

  test('sales dashboard snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'sales',
      route: '/sal-dashboard-main',
      hasChart: true,
    });
  });

  test('purchase dashboard snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'purchase',
      route: '/pur-dashboard-main',
      hasChart: true,
    });
  });

  test('inventory dashboard snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'inventory',
      route: '/inv-dashboard-main',
      // Deterministic KPI window (mirrors dashboards.visual.spec.ts cfg): the
      // page default 本月1日..今天 flips across month boundaries and moved the
      // KPI values between the 08-31 recording and the 09-01 run.
      filterDates: { 开始日期: '2026-07-01', 结束日期: '2026-07-31' },
      hasChart: true,
      // Trend chart is a 12-month rolling window ending today (getDashboardTrend
      // takes no date params): axis month keys + trailing partial bar change at
      // each month boundary and it renders as recharts SVG (not covered by the
      // canvas canonical mask). Mask it; the warehouse pie + alert tables
      // (config-gated off, static 暂无数据) stay unmasked.
      maskLocators: (page) => [page.locator('svg.recharts-surface').first()],
    });
  });

  test('assets dashboard snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'assets',
      route: '/ast-dashboard-main',
      filterValues: { periodId: '2026-07' },
      hasChart: true,
    });
  });

  test('projects dashboard snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'projects',
      route: '/prj-dashboard-main',
      hasChart: true,
    });
  });

  test('manufacturing dashboard snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'manufacturing',
      route: '/mfg-dashboard-main',
      hasChart: true,
    });
  });

  test('maintenance dashboard snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'maintenance',
      route: '/mnt-dashboard-main',
      hasChart: true,
    });
  });

  test('quality dashboard snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'quality',
      route: '/qa-dashboard-main',
      hasChart: true,
    });
  });

  test('master-data dashboard snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'master-data',
      route: '/md-dashboard-main',
      hasChart: false,
    });
  });

  // Extended-domain dashboards (F16 P2 complex pages)
  test('purchase three-way-match snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'purchase-three-way-match',
      route: '/pur-three-way-match',
      hasChart: false,
      skipKpiWait: true,
    });
  });

  test('drp net-requirement snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'drp-net-requirement',
      route: '/drp-net-requirement',
      hasChart: false,
      skipKpiWait: true,
    });
  });

  test('manufacturing bom-tree snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'manufacturing-bom-tree',
      route: '/mfg-bom-tree',
      hasChart: false,
      skipKpiWait: true,
    });
  });

  test('b2b asn-flow snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'b2b-asn-flow',
      route: '/b2b-asn-flow',
      hasChart: false,
      skipKpiWait: true,
    });
  });

  test('b2b edi-detail snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'b2b-edi-detail',
      route: '/b2b-edi-detail',
      hasChart: false,
      skipKpiWait: true,
    });
  });

  test('hr payroll-approval snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'hr-payroll-approval',
      route: '/hr-payroll-approval',
      hasChart: false,
      skipKpiWait: true,
    });
  });

  test('hr org-chart snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'hr-org-chart',
      route: '/hr-org-chart',
      hasChart: false,
      skipKpiWait: true,
    });
  });

  test('logistics shipment-tracking snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'logistics-shipment-tracking',
      route: '/log-shipment-tracking',
      hasChart: false,
      skipKpiWait: true,
    });
  });

  test('contract version-diff snapshot', async ({ page }) => {
    await driveAndSnapshot(page, {
      domain: 'contract-version-diff',
      route: '/ct-version-diff',
      hasChart: false,
      skipKpiWait: true,
    });
  });
});
