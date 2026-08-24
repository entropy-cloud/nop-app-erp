// Visual exploration: 截屏主要复杂操作模式用于人工/AI 分析。
// 输出 PNG 到 tests/e2e/visual/_exploration/screenshots/ 下，文件名带序号便于报告引用。
// 不做断言、不更新基线；纯截图采集。
//
// 设计：单个 test 内连续执行所有交互，使用同一个 page/auth 上下文，确保认证态一致。
// 失败/跳过状态下也截图（*-fallback.png）便于诊断。

import { test, loginAndNavigate, navigateTo } from '../../fixtures';
import type { Page, Locator } from '@playwright/test';
import * as path from 'path';
import * as fs from 'fs';

const OUT_DIR = path.join(__dirname, 'screenshots');
if (!fs.existsSync(OUT_DIR)) fs.mkdirSync(OUT_DIR, { recursive: true });

async function settle(page: Page, ms = 1500): Promise<void> {
  await page.waitForLoadState('networkidle', { timeout: 10_000 }).catch(() => {});
  await page.waitForTimeout(ms);
}

async function snap(page: Page, name: string): Promise<string> {
  const file = path.join(OUT_DIR, `${name}.png`);
  await page.screenshot({ path: file, fullPage: false });
  return file;
}

async function waitForFirstRow(crud: Locator, timeoutMs = 10_000): Promise<boolean> {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    const c = await crud.locator('[data-slot="table-body"] tr[data-slot="table-row"]').count().catch(() => 0);
    if (c > 0) return true;
    await new Promise((r) => setTimeout(r, 200));
  }
  return false;
}

test.describe.configure({ mode: 'serial' });
test.describe('复杂页面操作模式截屏（单上下文连续交互）', () => {
  test('capture-all', async ({ page }) => {
    // ───────────── 01: CRUD 列表 + 新水平 queryForm（首次 login） ─────────────
    await loginAndNavigate(page, '/ErpFinVoucher-main');
    await page.waitForSelector('.nop-crud', { timeout: 20_000 }).catch(() => {});
    await settle(page, 2000);
    await snap(page, '01-crud-list-with-horizontal-query-form');

    // ───────────── 02: 编辑抽屉（finance 凭证，复杂表单含子表） ─────────────
    const crud01 = page.locator('.nop-crud').first();
    if (await waitForFirstRow(crud01)) {
      // 编辑按钮：第一行的操作列
      const editBtn = crud01.locator('button, a').filter({ hasText: /编辑|Edit/ }).first();
      await editBtn.click({ timeout: 5000 }).catch(async () => {
        // 备选：hover 后再点
        await crud01.locator('[data-slot="table-body"] tr[data-slot="table-row"]').first().hover();
        await editBtn.click({ timeout: 5000 }).catch(() => {});
      });
      try {
        await page.waitForSelector('[data-slot="drawer-surface"]', { state: 'visible', timeout: 8000 });
        await settle(page, 2500);
        await snap(page, '02-crud-edit-drawer-with-subgrid');
      } catch {
        await snap(page, '02-crud-edit-drawer-fallback');
      }
      await page.keyboard.press('Escape').catch(() => {});
      await settle(page, 800);
    } else {
      await snap(page, '02-crud-edit-drawer-no-row');
    }

    // ───────────── 03: Picker 弹窗（purchase 订单编辑 → 选供应商） ─────────────
    await navigateTo(page, '/ErpPurOrder-main');
    await page.waitForSelector('.nop-crud', { timeout: 20_000 }).catch(() => {});
    await settle(page, 2000);
    const crud03 = page.locator('.nop-crud').first();
    if (await waitForFirstRow(crud03)) {
      const editBtn = crud03.locator('button, a').filter({ hasText: /编辑|Edit/ }).first();
      await editBtn.click({ timeout: 5000 }).catch(() => {});
      try {
        await page.waitForSelector('[data-slot="drawer-surface"]', { state: 'visible', timeout: 8000 });
        await settle(page, 2000);
        // 在抽屉内寻找 picker 触发器（放大镜/搜索图标按钮，或 aria-label 含 选择/Pick）
        const pickerTrigger = page.locator(
          '[data-slot="picker-trigger"], button[aria-label*="选择"], button[aria-label*="Pick"], button:has(i[class*="search"]), button:has(svg)'
        ).filter({ hasNotText: /编辑|Edit|删除|Delete|保存|Save|取消|Cancel/ }).first();
        const triggerCount = await pickerTrigger.count().catch(() => 0);
        if (triggerCount > 0) {
          await pickerTrigger.click({ timeout: 3000 }).catch(() => {});
          await page.waitForSelector('[data-slot="dialog-surface"], [role="dialog"]', { state: 'visible', timeout: 6000 }).catch(() => {});
          await settle(page, 2000);
          await snap(page, '03-picker-dialog-overlay');
          await page.keyboard.press('Escape').catch(() => {});
          await settle(page, 600);
        } else {
          await snap(page, '03-picker-dialog-no-trigger');
        }
      } catch {
        await snap(page, '03-picker-dialog-no-drawer');
      }
      await page.keyboard.press('Escape').catch(() => {});
      await settle(page, 600);
    } else {
      await snap(page, '03-picker-dialog-no-row');
    }

    // ───────────── 04: Master-Detail（purchase 订单编辑内嵌行项目子表） ─────────────
    if (await waitForFirstRow(crud03)) {
      const editBtn = crud03.locator('button, a').filter({ hasText: /编辑|Edit/ }).first();
      await editBtn.click({ timeout: 5000 }).catch(() => {});
      try {
        await page.waitForSelector('[data-slot="drawer-surface"]', { state: 'visible', timeout: 8000 });
        await settle(page, 2500);
        await snap(page, '04-master-detail-edit-drawer');
      } catch {
        await snap(page, '04-master-detail-no-drawer');
      }
      await page.keyboard.press('Escape').catch(() => {});
      await settle(page, 600);
    } else {
      await snap(page, '04-master-detail-no-row');
    }

    // ───────────── 05: 多 Tab 档案（finance 凭证模板） ─────────────
    await navigateTo(page, '/ErpFinVoucherTemplate-main');
    await page.waitForSelector('.nop-crud', { timeout: 20_000 }).catch(() => {});
    await settle(page, 2000);
    const crud05 = page.locator('.nop-crud').first();
    if (await waitForFirstRow(crud05)) {
      const editBtn = crud05.locator('button, a').filter({ hasText: /编辑|Edit/ }).first();
      await editBtn.click({ timeout: 5000 }).catch(() => {});
      try {
        await page.waitForSelector('[data-slot="drawer-surface"]', { state: 'visible', timeout: 8000 });
        await settle(page, 2000);
        await snap(page, '05-multitab-template-edit-drawer');
        const secondTab = page.locator('[data-slot="tabs-trigger"]').nth(1);
        if (await secondTab.count().catch(() => 0) > 0) {
          await secondTab.click({ timeout: 3000 }).catch(() => {});
          await settle(page, 1500);
          await snap(page, '05-multitab-template-second-tab');
        }
      } catch {
        await snap(page, '05-multitab-template-no-drawer');
      }
      await page.keyboard.press('Escape').catch(() => {});
      await settle(page, 600);
    } else {
      await snap(page, '05-multitab-template-no-row');
    }

    // ───────────── 06: 多步向导（maintenance 维护访问 4 步） ─────────────
    await navigateTo(page, '/mnt-visit-wizard');
    await settle(page, 2500);
    await snap(page, '06-wizard-visit-initial');
    const visitSelect = page.locator('select[name="visitId"], input[name="visitId"]').first();
    if (await visitSelect.count().catch(() => 0) > 0) {
      await visitSelect.click({ timeout: 3000 }).catch(() => {});
      await settle(page, 1000);
      await snap(page, '06-wizard-visit-dropdown-open');
      await page.keyboard.press('Escape').catch(() => {});
      await settle(page, 500);
    }

    // ───────────── 07: Dashboard 图表（finance 仪表板） ─────────────
    await navigateTo(page, '/fin-dashboard-main');
    await page.waitForSelector('canvas', { timeout: 10_000 }).catch(() => {});
    await settle(page, 3500);
    await snap(page, '07-dashboard-finance');

    // ───────────── 08: 树形视图（hr 组织架构） ─────────────
    await navigateTo(page, '/hr-org-chart');
    await settle(page, 3500);
    await snap(page, '08-tree-org-chart');
    const expand = page.locator('[data-slot="tree-node-expand"], button[aria-label*="展开"]').first();
    if (await expand.count().catch(() => 0) > 0) {
      await expand.click({ timeout: 3000 }).catch(() => {});
      await settle(page, 1500);
      await snap(page, '08-tree-org-chart-expanded');
    }
  });
});