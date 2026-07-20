import type { Page } from '@playwright/test';
import { DEFAULT_NAV_TIMEOUT } from './types';

/**
 * Local adaptation of `@nop-chaos/e2e-shared` Navigation.
 *
 * Divergence from shared Navigation.ts (packages/e2e-shared/src/Navigation.ts):
 * - Signature: `login(page, username?, password?)` vs shared `login(page, baseUrl?)`
 *   → nop-app-erp fills explicit credentials from params; shared navigates to baseUrl
 * - Login detection: URL-path check (page.url().includes('/auth/login'))
 *   vs shared input-visibility check (usernameInput.isVisible())
 * - Wait strategy: `domcontentloaded` + explicit timeouts vs shared `networkidle`
 * - Hash route format: `/#${hashRoute}` (hash-first) vs shared `#/${hashRoute}`
 * - `navigateTo` adds 4s explicit wait; `login` adds 2s post-login wait
 *
 * Additionally, the shared package's exported `login` (from MockAuthAdapter.ts)
 * is a mock-mode login (route interception); nop-app-erp uses a real-backend
 * login flow. The local implementation preserves existing behavior for the
 * ~180 dependent specs.
 */
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
