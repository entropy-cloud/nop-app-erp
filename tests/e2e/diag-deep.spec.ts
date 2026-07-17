import { test } from '@playwright/test';

test('ErpFinVoucher deep diagnosis', async ({ page }) => {
  const gqlResponses: any[] = [];
  page.on('response', async (resp) => {
    if (resp.url().includes('/graphql') || resp.url().includes('/r/')) {
      const status = resp.status();
      let body = '';
      try { body = (await resp.text()).substring(0, 500); } catch {}
      gqlResponses.push({ url: resp.url().substring(0, 100), status, body: body.substring(0, 300) });
    }
  });

  await page.goto('/', { waitUntil: 'domcontentloaded', timeout: 30_000 });
  const usernameInput = page.locator('input[name="username"]');
  await usernameInput.waitFor({ state: 'visible', timeout: 20_000 });
  if (page.url().includes('/auth/login')) {
    await usernameInput.fill('nop');
    await page.locator('input[name="password"]').fill('123');
    await page.waitForTimeout(500);
    await page.getByRole('button', { name: /Sign in|登录/ }).click();
    await page.waitForURL((url) => !url.hash.includes('login'), { timeout: 30_000 });
    await page.waitForTimeout(2000);
  }

  await page.goto(`/#/ErpFinVoucher-main`, { waitUntil: 'domcontentloaded', timeout: 30_000 });
  await page.waitForTimeout(8000);

  const html = await page.evaluate(() => {
    const main = document.querySelector('#main-content, main, .cxd-Page, [role="main"]');
    return main?.innerHTML?.substring(0, 2000) || 'NO MAIN CONTENT';
  });

  console.log('\n=== MAIN CONTENT HTML ===');
  console.log(html);
  console.log('\n=== GRAPHQL RESPONSES ===');
  for (const r of gqlResponses) {
    console.log(`${r.status} ${r.url}\n  ${r.body}\n`);
  }
});
