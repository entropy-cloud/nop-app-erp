import { chromium, type FullConfig } from '@playwright/test';
import * as path from 'path';
import * as fs from 'fs';
import { performLogin, AUTH_FILE } from './auth';

export default async function globalSetup(config: FullConfig) {
  const baseURL = config.projects[0]?.use?.baseURL || 'http://127.0.0.1:8080';
  const useChromeChannel = !fs.existsSync(
    path.join(process.env.HOME || '', 'Library/Caches/ms-playwright/chromium_headless_shell-1228')
  );

  const browser = await chromium.launch({ headless: true, ...(useChromeChannel ? { channel: 'chrome' } : {}) });
  const context = await browser.newContext({ baseURL });
  const page = await context.newPage();

  try {
    await performLogin(page);

    const cookies = await context.cookies();
    const tokenCookie = cookies.find((c) => c.name === 'nop-token');
    if (tokenCookie) {
      await page.evaluate((token) => {
        localStorage.setItem('nop-token', token);
      }, tokenCookie.value);
    }

    await context.storageState({ path: AUTH_FILE });
  } finally {
    await browser.close();
  }
}
