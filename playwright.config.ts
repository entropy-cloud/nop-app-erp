import { defineConfig, devices } from '@playwright/test';
import * as path from 'path';
import * as fs from 'fs';

const port = parseInt(process.env.PLAYWRIGHT_PORT || '8080', 10);
const baseUrl = `http://127.0.0.1:${port}`;
const repoRoot = path.resolve(__dirname);
const runnerJar = path.join(repoRoot, 'app-erp-all/target/quarkus-app/quarkus-run.jar');

const skipWebServer = !!process.env.SKIP_WEBSERVER || !!process.env.BASE_URL;
const effectiveBaseUrl = process.env.BASE_URL || baseUrl;

const useChromeChannel = !fs.existsSync(
  path.join(process.env.HOME || '', 'Library/Caches/ms-playwright/chromium_headless_shell-1228')
);

const webServerConfig = skipWebServer ? undefined : {
  command: `rm -f db/erp.mv.db db/erp.trace.db && java -Dfile.encoding=UTF8 -Dnop.auth.service-public=true -Dnop.auth.login.allow-create-default-user=true -Dnop.orm.init-database-data=true -Derp-qua.ncr-default-acct-schema=1 -jar "${runnerJar}"`,
  url: effectiveBaseUrl,
  reuseExistingServer: true,
  timeout: 120_000,
  cwd: repoRoot,
};

const authFile = path.join(repoRoot, '_tmp/e2e-auth-state.json');

export default defineConfig({
  testDir: './tests/e2e',
  timeout: 45_000,
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  workers: 1,
  retries: 0,
  reporter: 'list',
  use: {
    baseURL: effectiveBaseUrl,
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
    ...(useChromeChannel ? { channel: 'chrome' } : {}),
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        ...(useChromeChannel ? { channel: 'chrome' } : {}),
      },
    },
  ],
  ...(webServerConfig ? { webServer: webServerConfig } : {}),
});
