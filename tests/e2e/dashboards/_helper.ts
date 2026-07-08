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
