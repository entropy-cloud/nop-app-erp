import { test, expect, loginAndNavigate } from '../fixtures';

export function runReportSmoke(domain: string, route: string): void {
  test.describe(`${domain} report smoke`, () => {
    test('renders page with render button and GraphQL 200', async ({ page }) => {
      const graphqlResponses: { status: number; body: string }[] = [];
      page.on('response', async (resp) => {
        if (resp.url().includes('/graphql') && resp.request().method() === 'POST') {
          let body = '';
          try {
            body = await resp.text();
          } catch {
            body = '';
          }
          graphqlResponses.push({ status: resp.status(), body });
        }
      });

      await loginAndNavigate(page, route);

      const bodyText = (await page.textContent('body')) || '';
      expect(bodyText.length, 'Report page should render').toBeGreaterThan(50);

      const renderBtn = page.locator('button:has-text("渲染"), button:has-text("Render"), button[type="submit"]').first();
      const btnExists = await renderBtn.count();
      if (btnExists > 0) {
        await renderBtn.click({ timeout: 10_000 }).catch(() => {});
        await page.waitForTimeout(3000);
      }

      expect(graphqlResponses.length, 'Should have GraphQL calls').toBeGreaterThan(0);

      const renderCalls = graphqlResponses.filter(
        (r) => r.body.includes('renderHtml') || r.body.includes('Report__')
      );

      for (const r of graphqlResponses) {
        expect(r.status, `GraphQL should return 200`).toBe(200);
      }

      if (renderCalls.length > 0) {
        const htmlResponse = renderCalls.find((r) => r.body.includes('<') || r.body.length > 100);
        expect(htmlResponse, 'renderHtml response should be non-empty').toBeTruthy();
      }
    });
  });
}
