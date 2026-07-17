import { test as base, expect } from '@playwright/test';
import { getEngine } from './pages';
import type { EngineAdapter } from './pages';
import { loginAndNavigate, login, navigateTo } from './pages/Navigation';

export { loginAndNavigate, login, navigateTo };

export const test = base.extend<{ engine: EngineAdapter }>({
  engine: async ({}, use) => {
    await use(getEngine());
  },
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

export { expect };
export type { EngineAdapter };
