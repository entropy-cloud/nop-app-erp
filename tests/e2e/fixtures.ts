import { test as base, expect, type Page } from '@playwright/test';

async function loginAndNavigate(page: Page, hashRoute: string): Promise<void> {
  await page.goto('/', { waitUntil: 'domcontentloaded', timeout: 30_000 });

  const usernameInput = page.locator('input[name="username"]');
  await usernameInput.waitFor({ state: 'visible', timeout: 20_000 });

  if (page.url().includes('/auth/login')) {
    await usernameInput.fill('nop');
    await page.locator('input[name="password"]').fill('123');
    await page.waitForTimeout(500);

    const loginBtn = page.getByRole('button', { name: /Sign in|登录/ });
    await loginBtn.click();

    await page.waitForURL((url) => !url.hash.includes('login'), { timeout: 30_000 });
    await page.waitForTimeout(2000);
  }

  await page.goto(`/#${hashRoute}`, { waitUntil: 'domcontentloaded', timeout: 30_000 });
  await page.waitForTimeout(4000);
}

export const test = base.extend({
  page: async ({ page }, use) => {
    const consoleErrors: string[] = [];
    page.on('console', (msg) => {
      if (msg.type() === 'error') {
        consoleErrors.push(msg.text());
      }
    });
    page.on('pageerror', (err) => {
      consoleErrors.push(`PAGEERROR: ${err.message}`);
    });

    await use(page);

    if (consoleErrors.length > 0) {
      const filtered = consoleErrors.filter(
        (e) =>
          !e.includes('favicon') &&
          !e.includes('404') &&
          !e.includes('Download the React DevTools') &&
          !e.includes('net::ERR') &&
          !e.includes('Failed to load resource')
      );
      if (filtered.length > 0) {
        throw new Error(`Console errors on ${page.url()}:\n${filtered.join('\n')}`);
      }
    }
  },
});

export { expect, loginAndNavigate };
