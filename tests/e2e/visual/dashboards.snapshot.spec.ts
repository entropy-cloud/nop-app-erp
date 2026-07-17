import { test, loginAndNavigate } from '../fixtures';
import { assertSnapshot } from './_helper';
import type { Page } from '@playwright/test';

// Pixel-snapshot layer (plan 2026-07-17-2010-2 Phase 2).
//
// Builds on the same dashboard AMIS-render pipeline as
// dashboards.visual.spec.ts (DOM-content layer), but asserts pixel-level
// layout stability via assertSnapshot. This layer catches regressions the
// DOM-content layer cannot: CSS misalignment, element overlap, echarts
// canvas dimension collapse, responsive breakpoint breakage — i.e. the
// structural blind spots of DOM-content assertions.
//
// Page-driving (login → navigate → wait for getDashboardKpi GraphQL
// response → fill filters → wait for reload → wait for echarts settle) is
// identical to the DOM-content layer, ensuring deterministic seed data
// backing every snapshot. assertSnapshot adds font hardening, canonical
// mask (header + canvas), and 1% ratio tolerance (Phase 1 selected
// approach).
//
// Baseline update: when a dashboard's page.yaml/view changes intentionally,
// re-record with `--update-snapshots`.

interface DashboardSnapshot {
  domain: string;
  route: string;
  filterValues?: Record<string, string>;
  hasChart: boolean;
}

async function driveAndSnapshot(page: Page, cfg: DashboardSnapshot): Promise<void> {
  const kpiAction = 'getDashboardKpi';
  const initialResponsePromise = page.waitForResponse(
    (resp) => {
      if (!resp.url().includes('/graphql')) return false;
      const body = resp.request().postData() || '';
      return body.includes(kpiAction);
    },
    { timeout: 30_000 },
  );

  await loginAndNavigate(page, cfg.route);
  await initialResponsePromise;

  if (cfg.filterValues && Object.keys(cfg.filterValues).length > 0) {
    for (const [name, value] of Object.entries(cfg.filterValues)) {
      await page.locator(`input[name="${name}"]`).first().fill(value);
    }
    const reloadResponsePromise = page.waitForResponse(
      (resp) => {
        if (!resp.url().includes('/graphql')) return false;
        const body = resp.request().postData() || '';
        return body.includes(kpiAction);
      },
      { timeout: 30_000 },
    );
    await page.getByRole('button', { name: /刷新|Refresh/ }).first().click();
    await reloadResponsePromise;
  }

  await page.locator('.border.rounded.p-3').first().waitFor({ state: 'visible', timeout: 15_000 });

  await assertSnapshot(page, {
    name: `${cfg.domain}-dashboard.png`,
    skipEchartsSettle: !cfg.hasChart,
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
      hasChart: true,
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
});
