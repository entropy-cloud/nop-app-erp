/**
 * Visual verification script - takes screenshots of pages with the new fixes
 * to verify visual improvements.
 *
 * Run: npx tsx tests/e2e/visual/_exploration/verify-fixes.ts
 */
import { chromium } from '@playwright/test';
import * as path from 'path';
import * as fs from 'fs';

const BASE_URL = process.env.E2E_BASE_URL || 'http://localhost:8011';
const OUT_DIR = path.join(__dirname, 'verify-screenshots');
if (!fs.existsSync(OUT_DIR)) fs.mkdirSync(OUT_DIR, { recursive: true });

async function login(page: any): Promise<void> {
  await page.goto(BASE_URL + '/', { waitUntil: 'domcontentloaded' });
  const usernameInput = page.locator('input[name="username"]');
  await usernameInput.waitFor({ state: 'visible', timeout: 20_000 });
  await usernameInput.fill('nop');
  await page.locator('input[name="password"]').fill('123');
  await page.waitForTimeout(500);
  const loginBtn = page.getByRole('button', { name: /Sign in|登录/ });
  await loginBtn.click();
  await page.waitForTimeout(2000);
}

async function navigateAndSnap(page: any, route: string, name: string, ms = 3000): Promise<void> {
  await page.goto(BASE_URL + '/#' + route, { waitUntil: 'domcontentloaded' });
  await page.waitForTimeout(ms);
  await page.screenshot({ path: path.join(OUT_DIR, `${name}.png`), fullPage: false });
  console.log(`  -> ${name}.png`);
}

async function main(): Promise<void> {
  console.log('Starting verification...');
  const browser = await chromium.launch({
    headless: true,
    executablePath: '/Applications/Google Chrome.app/Contents/MacOS/Google Chrome',
  });
  const ctx = await browser.newContext({ viewport: { width: 1280, height: 720 } });
  const page = await ctx.newPage();

  console.log('Login...');
  await login(page);

  console.log('1. CRUD list (voucher) - testing queryForm mode=horizontal + labelWidth + button styling');
  await navigateAndSnap(page, '/ErpFinVoucher-main', '01-crud-voucher-after-fix', 5000);

  console.log('2. CRUD list (purchase order) - testing operations column width');
  await navigateAndSnap(page, '/ErpPurOrder-main', '02-crud-purchase-after-fix', 5000);

  console.log('3. Multi-tab page (voucher template) - testing queryForm inside tabs');
  await navigateAndSnap(page, '/ErpFinVoucherTemplate-main', '03-tab-template-after-fix', 5000);

  console.log('4. Wizard (maintenance visit) - testing new layout');
  await navigateAndSnap(page, '/mnt-visit-wizard', '04-wizard-after-fix', 5000);

  console.log('5. Dashboard (finance) - testing refresh button + KPI cards');
  await navigateAndSnap(page, '/fin-dashboard-main', '05-dashboard-finance-after-fix', 5000);

  console.log('6. Tree (HR org chart) - testing fallback for undefined name');
  await navigateAndSnap(page, '/hr-org-chart', '06-tree-org-after-fix', 5000);

  console.log('7. CRUD list (material) - testing master-data page');
  await navigateAndSnap(page, '/ErpMdMaterial-main', '07-crud-material-after-fix', 5000);

  await browser.close();
  console.log('All screenshots done in', OUT_DIR);
}

main().catch((err) => {
  console.error('Error:', err);
  process.exit(1);
});