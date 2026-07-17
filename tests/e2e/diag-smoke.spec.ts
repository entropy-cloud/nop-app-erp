import { test } from '@playwright/test';

const PASSING = ['ErpMdPartner', 'ErpPurOrder', 'ErpSalOrder', 'ErpAstAsset'];
const FAILING = ['ErpFinVoucher', 'ErpInvStockMove', 'ErpCrmLead', 'ErpMfgWorkOrder'];

test.describe('CRUD smoke diagnosis', () => {
  for (const entity of [...PASSING, ...FAILING]) {
    test(`${entity} page load diagnosis`, async ({ page }) => {
      const errors: string[] = [];
      page.on('console', (msg) => {
        if (msg.type() === 'error') errors.push(msg.text());
      });
      page.on('requestfailed', (req) => {
        errors.push(`REQFAIL: ${req.url()} ${req.failure()?.errorText}`);
      });

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

      await page.goto(`/#/${entity}-main`, { waitUntil: 'domcontentloaded', timeout: 30_000 });
      await page.waitForTimeout(5000);

      const diag = await page.evaluate(() => {
        const crud = document.querySelector('.cxd-Crud');
        const table = document.querySelector('.cxd-Table');
        const plainTable = document.querySelector('table');
        const mainContent = document.querySelector('#main-content, main, .cxd-Page, [role="main"]');
        const bodyLen = document.body.innerHTML.length;
        const errorTexts = Array.from(document.querySelectorAll('.cxd-Alert, .ant-message-error, [class*="error"]'))
          .map(e => e.textContent?.trim().substring(0, 200))
          .filter(t => t && t.length > 5);
        return {
          hasCrud: !!crud,
          hasTable: !!table,
          hasPlainTable: !!plainTable,
          hasMainContent: !!mainContent,
          mainContentLen: mainContent?.innerHTML.length || 0,
          bodyLen,
          errorTexts: errorTexts.slice(0, 3),
          bodyPreview: document.body.innerText.substring(0, 500),
        };
      });

      console.log(`\n=== ${entity} ===`);
      console.log(JSON.stringify(diag, null, 2));
      if (errors.length) console.log(`ERRORS: ${errors.slice(0,5).join('\n')}`);
    });
  }
});
