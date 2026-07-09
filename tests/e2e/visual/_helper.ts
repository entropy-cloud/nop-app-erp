import { test, expect, loginAndNavigate, type Page } from '../fixtures';

export interface DashboardVisualAssertion {
  domain: string;
  route: string;
  expectedKpiTokens?: string[];
  hasChart: boolean;
  alertTable: boolean;
}

async function kpiSpanTexts(page: Page): Promise<string[]> {
  return page.locator('span.h3').allTextContents();
}

export function assertDashboardRendered(cfg: DashboardVisualAssertion): void {
  test.describe(`${cfg.domain} dashboard AMIS render`, () => {
    test('renders KPI cards + echarts canvas + alert table via AMIS GraphQL pipeline', async ({ page }) => {
      const kpiResponsePromise = page.waitForResponse(
        (resp) => {
          if (!resp.url().includes('/graphql')) return false;
          const body = resp.request().postData() || '';
          return body.includes('getDashboardKpi');
        },
        { timeout: 30_000 },
      );

      await loginAndNavigate(page, cfg.route);

      const kpiResponse = await kpiResponsePromise;
      expect(kpiResponse.status(), `${cfg.domain} KPI GraphQL should return 200`).toBe(200);

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
            { timeout: 15_000, message: `${cfg.domain} KPI span.h3 should render token "${tok}"` },
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
