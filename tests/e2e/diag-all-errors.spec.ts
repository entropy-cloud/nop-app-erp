import { test } from '@playwright/test';

const ENTITIES = [
  'ErpFinVoucher', 'ErpInvStockMove', 'ErpCrmLead', 'ErpMfgWorkOrder',
  'ErpCsTicket', 'ErpHrEmployee', 'ErpQaInspection', 'ErpMntVisit',
  'ErpLogShipment', 'ErpCtContract', 'ErpDrpPlan', 'ErpApsConstraint',
  'ErpB2bAsn',
];

test.describe('All failing CRUD page errors', () => {
  for (const entity of ENTITIES) {
    test(`${entity} error capture`, async ({ page }) => {
      // Login
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

      // Capture graphql errors
      const gqlErrors: string[] = [];
      page.on('response', async (resp) => {
        if (resp.url().includes('/graphql')) {
          try {
            const json = await resp.json();
            if (json?.errors) {
              for (const e of json.errors) {
                gqlErrors.push(e.message);
              }
            }
          } catch {}
        }
      });

      await page.goto(`/#/${entity}-main`, { waitUntil: 'domcontentloaded', timeout: 30_000 });
      await page.waitForTimeout(5000);

      if (gqlErrors.length) {
        console.log(`\n${entity}: ${gqlErrors.join('; ')}`);
      } else {
        console.log(`\n${entity}: NO GraphQL ERRORS (might be rendering issue)`);
      }
    });
  }
});
