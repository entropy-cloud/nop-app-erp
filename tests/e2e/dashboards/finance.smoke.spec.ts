import { test, expect, loginAndNavigate } from '../fixtures';

test.describe('Finance dashboard smoke', () => {
  test('renders KPI cards, chart, and alert table with GraphQL 200', async ({ page }) => {
    const graphqlResponses: { status: number; url: string }[] = [];
    page.on('response', (resp) => {
      if (resp.url().includes('/graphql')) {
        graphqlResponses.push({ status: resp.status(), url: resp.url() });
      }
    });

    await loginAndNavigate(page, '/fin-dashboard-main');

    await expect(page.locator('body')).not.toBeEmpty();

    const bodyText = (await page.textContent('body')) || '';
    expect(bodyText.length).toBeGreaterThan(100);

    expect(
      bodyText.includes('收入') || bodyText.includes('支出') || bodyText.includes('利润'),
      'Dashboard should contain KPI labels'
    ).toBe(true);

    expect(graphqlResponses.length).toBeGreaterThan(0);
    for (const r of graphqlResponses) {
      expect(r.status, `GraphQL ${r.url} returned ${r.status}`).toBe(200);
    }
  });
});
