import type { Page } from '@playwright/test';
import { DEFAULT_NAV_TIMEOUT } from './types';

export const DEFAULT_USER = process.env.E2E_USER || 'nop';
export const DEFAULT_PASS = process.env.E2E_PASSWORD || '123';

export async function login(page: Page, username = DEFAULT_USER, password = DEFAULT_PASS): Promise<void> {
  await page.goto('/', { waitUntil: 'domcontentloaded', timeout: DEFAULT_NAV_TIMEOUT });

  const usernameInput = page.locator('input[name="username"]');
  await usernameInput.waitFor({ state: 'visible', timeout: 20_000 });

  if (page.url().includes('/auth/login')) {
    await usernameInput.fill(username);
    await page.locator('input[name="password"]').fill(password);
    await page.waitForTimeout(500);

    const loginBtn = page.getByRole('button', { name: /Sign in|登录/ });
    await loginBtn.click();

    await page.waitForURL((url) => !url.hash.includes('login'), { timeout: DEFAULT_NAV_TIMEOUT });
    await page.waitForTimeout(2000);
  }
}

export async function navigateTo(page: Page, hashRoute: string): Promise<void> {
  await page.goto(`/#${hashRoute}`, { waitUntil: 'domcontentloaded', timeout: DEFAULT_NAV_TIMEOUT });
  await page.waitForTimeout(4000);
}

export async function loginAndNavigate(page: Page, hashRoute: string): Promise<void> {
  await login(page);
  await navigateTo(page, hashRoute);
}
