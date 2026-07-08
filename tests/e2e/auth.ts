import { type Page } from '@playwright/test';

export const AUTH_FILE = '_tmp/e2e-auth-state.json';
export const TEST_USERNAME = 'nop';
export const TEST_PASSWORD = '123';

export async function performLogin(page: Page): Promise<void> {
  await page.goto('/', { waitUntil: 'networkidle', timeout: 30_000 });
  await page.waitForTimeout(1000);

  if (!page.url().includes('/auth/login')) {
    return;
  }

  await page.fill('input[name="username"]', TEST_USERNAME);
  await page.fill('input[name="password"]', TEST_PASSWORD);
  await page.click('button:has-text("登录")');
  await page.waitForURL((url) => !url.hash.includes('login'), { timeout: 30_000 });
  await page.waitForTimeout(2000);
}
