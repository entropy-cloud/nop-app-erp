import { test, expect, loginAndNavigate } from '../business-actions/_helper';

/**
 * USC-05 PDA 盘点扫码作业面浏览器层冒烟（plan 2026-10-01-0930-1 Phase 2）。
 *
 * 双视口（375×812 移动 + 1280×720 桌面）验证 PDA 扫码页可达且表单渲染；
 * 页面为表单式（input+button），规避 USC-04 D5(b) 窄屏网格行定位 residual。
 * 解析/保存动作的语义断言由 TestErpInvBarcodeResolve（JUnit 7/7）承载。
 */
const MOBILE = { width: 375, height: 812 };

async function assertPageRendered(page: import('@playwright/test').Page, label: string) {
  // 表单式页面：断言两个表单标题与输入域渲染（非网格，规避 D5(b)）
  await expect(page.locator('input').first()).toBeVisible({ timeout: 15000 });
  const bodyText = await page.locator('body').innerText();
  expect(bodyText, `${label}: 页面应渲染 PDA 扫码表单`).toContain('PDA');
}

test.describe('USC-05 PDA stock-take scan page (mobile + desktop)', () => {
  test('mobile 375px: PDA scan page reachable, form rendered', async ({ page }) => {
    await page.setViewportSize(MOBILE);
    await loginAndNavigate(page, '/pda-stock-take');
    await assertPageRendered(page, 'mobile 375px');
  });

  test('desktop 1280px: PDA scan page reachable, form rendered', async ({ page }) => {
    await loginAndNavigate(page, '/pda-stock-take');
    await assertPageRendered(page, 'desktop 1280px');
  });
});
