import { test } from '@playwright/test';

const ENTITIES = ['ErpCrmLead', 'ErpCsTicket', 'ErpHrEmployee', 'ErpMntVisit', 'ErpLogShipment'];

test.describe('Remaining error diagnosis', () => {
  for (const entity of ENTITIES) {
    test(`${entity} deep`, async ({ page }) => {
      const gqlErrors: string[] = [];
      page.on('response', async (resp) => {
        if (resp.url().includes('/graphql')) {
          try {
            const json = await resp.json();
            if (json?.errors) for (const e of json.errors) gqlErrors.push(e.message);
          } catch {}
        }
      });

      await page.goto('/', { waitUntil: 'domcontentloaded', timeout: 30_000 });
      const u = page.locator('input[name="username"]');
      await u.waitFor({ state: 'visible', timeout: 20_000 });
      if (page.url().includes('/auth/login')) {
        await u.fill('nop'); await page.locator('input[name="password"]').fill('123');
        await page.waitForTimeout(500);
        await page.getByRole('button', { name: /Sign in|登录/ }).click();
        await page.waitForURL((url) => !url.hash.includes('login'), { timeout: 30_000 });
        await page.waitForTimeout(2000);
      }
      await page.goto(`/#/${entity}-main`, { waitUntil: 'domcontentloaded', timeout: 30_000 });
      await page.waitForTimeout(5000);

      const hasCrud = await page.evaluate(() => !!document.querySelector('.cxd-Crud'));
      console.log(`\n${entity}: crud=${hasCrud} errors=${gqlErrors.length ? gqlErrors.join('; ') : 'NONE'}`);
    });
  }
});
