import { test, expect, loginAndNavigate } from '../fixtures';
import type { Page } from '@playwright/test';

export interface DashboardVisualAssertion {
  domain: string;
  route: string;
  expectedKpiTokens?: string[];
  hasChart: boolean;
  alertTable: boolean;
  /** Form field values to fill before reloading, locking deterministic seed
   * values (aligned with the value-spec layer). Without these, date/period
   * KPIs would drift with the server clock. */
  filterValues?: Record<string, string>;
  /** GraphQL action name whose response marks the post-reload KPI ready.
   * Defaults to `getDashboardKpi`. */
  kpiAction?: string;
}

async function kpiSpanTexts(page: Page): Promise<string[]> {
  return page.locator('span.h3').allTextContents();
}

export function assertDashboardRendered(cfg: DashboardVisualAssertion): void {
  test.describe(`${cfg.domain} dashboard AMIS render`, () => {
    test('renders KPI cards + echarts canvas + alert table via AMIS GraphQL pipeline', async ({ page }) => {
      const kpiAction = cfg.kpiAction ?? 'getDashboardKpi';

      // Initial load (default/empty filters) — captures the AMIS GraphQL
      // pipeline integrity. Defect A (mangled `$var`) still returns HTTP 200
      // here, so this alone does not prove values; the token assertions below do.
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

      // Deterministic filtered reload: fill the filter form, then click the
      // "刷新" reload button, then wait for the post-reload KPI response. This
      // locks date/period ranges to seed values (same caliber as value specs).
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

      const kpiCards = page.locator('.border.rounded.p-3');
      await expect(
        kpiCards.first(),
        `${cfg.domain} KPI cards should render`,
      ).toBeVisible({ timeout: 15_000 });
      expect(
        await kpiCards.count(),
        `${cfg.domain} should render multiple KPI cards`,
      ).toBeGreaterThanOrEqual(1);

      if (cfg.expectedKpiTokens && cfg.expectedKpiTokens.length > 0) {
        for (const tok of cfg.expectedKpiTokens) {
          await expect.poll(
            async () => (await kpiSpanTexts(page)).join('||'),
            { timeout: 20_000, message: `${cfg.domain} KPI span.h3 should render token "${tok}"` },
          ).toContain(tok);
        }
      }

      if (cfg.hasChart) {
        await expect(
          page.locator('canvas').first(),
          `${cfg.domain} echarts canvas should render`,
        ).toBeVisible({ timeout: 20_000 });
        const box = await page.locator('canvas').first().boundingBox();
        expect(
          box !== null && box.width > 0 && box.height > 0,
          `${cfg.domain} echarts canvas should have non-zero size`,
        ).toBe(true);
      }

      if (cfg.alertTable) {
        await expect(
          page.locator('table').first(),
          `${cfg.domain} alert crud table should render`,
        ).toBeVisible({ timeout: 15_000 });
      }
    });
  });
}
