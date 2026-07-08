import { test, expect, loginAndNavigate } from '../fixtures';

export function runDashboardSmoke(domain: string, route: string, kpiKeywords: string[] = []): void {
  test.describe(`${domain} dashboard smoke`, () => {
    test('renders page with GraphQL 200 and no console errors', async ({ page }) => {
      const graphqlResponses: number[] = [];
      page.on('response', (resp) => {
        if (resp.url().includes('/graphql')) {
          graphqlResponses.push(resp.status());
        }
      });

      await loginAndNavigate(page, route);

      const bodyText = (await page.textContent('body')) || '';
      expect(bodyText.length, 'Page should render substantial content').toBeGreaterThan(100);

      if (kpiKeywords.length > 0) {
        const hasKpi = kpiKeywords.some((kw) => bodyText.includes(kw));
        expect(hasKpi, `Dashboard should contain one of KPI keywords: ${kpiKeywords.join(', ')}`).toBe(true);
      }

      expect(graphqlResponses.length, 'Should have GraphQL calls').toBeGreaterThan(0);
      for (const status of graphqlResponses) {
        expect(status, `GraphQL should return 200`).toBe(200);
      }
    });
  });
}

export interface DashboardKpiAssertion {
  domain: string;
  route: string;
  query: string;
  variables: Record<string, unknown>;
  responseKey: string;
  expected: Record<string, number>;
}

export function assertDashboardKpiValues(cfg: DashboardKpiAssertion): void {
  test.describe(`${cfg.domain} dashboard KPI values`, () => {
    test(`getDashboardKpi returns deterministic seed-driven values`, async ({ page }) => {
      await loginAndNavigate(page, cfg.route);

      const resp = await page.request.post('/graphql', {
        data: { query: cfg.query, variables: cfg.variables },
      });
      expect(resp.status(), 'GraphQL getDashboardKpi should return 200').toBe(200);

      const json = await resp.json();
      const kpi = json?.data?.[cfg.responseKey];
      expect(kpi, `KPI map should be present at data.${cfg.responseKey}`).toBeTruthy();

      for (const [field, expected] of Object.entries(cfg.expected)) {
        const actual = Number(kpi[field]);
        expect(
          !Number.isNaN(actual) && actual === expected,
          `${cfg.domain}.${field} expected ${expected} but got ${kpi[field]} (raw)`,
        ).toBe(true);
      }
    });
  });
}
