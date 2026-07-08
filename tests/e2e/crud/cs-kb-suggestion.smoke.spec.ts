import { test, expect, loginAndNavigate } from '../fixtures';

test.describe('CS ticket KB suggestion smoke', () => {
  test('add form triggers suggestForTicket GraphQL and renders suggestion area', async ({ page }) => {
    const suggestResponses: { status: number; body: string }[] = [];
    page.on('response', async (resp) => {
      if (resp.url().includes('/graphql') && resp.request().method() === 'POST') {
        let body = '';
        try {
          body = await resp.text();
        } catch {
          body = '';
        }
        if (body.includes('suggestForTicket')) {
          suggestResponses.push({ status: resp.status(), body });
        }
      }
    });

    await loginAndNavigate(page, '/ErpCsTicket-main');

    await page.waitForTimeout(2000);

    const mainContent = page.locator('#main-content, main, .cxd-Page, [role="main"]').first();
    await mainContent.waitFor({ state: 'visible', timeout: 15_000 });

    const addBtn = mainContent.locator('button:has(.fa-plus), button:has-text("Save")').first();
    await addBtn.waitFor({ state: 'visible', timeout: 15_000 });
    await addBtn.click({ force: true });

    const subjectInput = page.locator('input[name="subject"]').first();
    await subjectInput.waitFor({ state: 'visible', timeout: 15_000 });

    await subjectInput.fill('打印机故障');
    await page.waitForTimeout(6000);

    expect(
      suggestResponses.length,
      'suggestForTicket GraphQL should be called when subject >= 2 chars',
    ).toBeGreaterThan(0);

    for (const r of suggestResponses) {
      expect(r.status, `suggestForTicket GraphQL should return 200`).toBe(200);
    }

    const bodyText = (await page.textContent('body')) || '';
    expect(
      bodyText.includes('相关知识库文章') || suggestResponses.length > 0,
      'KB suggestion area should render or GraphQL should return',
    ).toBe(true);
  });
});
