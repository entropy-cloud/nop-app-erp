// DOM 结构断言 for F12 Tier C + F16 — 维护访问执行向导（plan 2026-07-23-1145-1 Phase 4）。
//
// 断言 visit-wizard page.yaml 经 AMIS 渲染的 DOM 结构（非像素快照——稳定跨 AMIS 升级/字体漂移）：
//   1. page metadata 可达（路由 + 维护访问选择 select 渲染）
//   2. steps 指示器结构（预渲染 HTML 单 tpl 4 步色块，§5 先例）
//   3. Step 1 维护信息确认区（"开始执行" button 在 SCHEDULED 态可见）
//   4. start mutation 后 step indicator 前进 + Step 2-4 区域在 IN_PROGRESS 可见
//
// 自包含：建测试专用维护访问（DRAFT → schedule → SCHEDULED → 断言 wizard Step 1 + start；
// start → IN_PROGRESS → 断言 Step 2-4 区域可见），cleanup 逻辑删除 visit，不污染 maintenance 看板基线。
// 与 maintenance-visit-wizard.action.spec.ts 互补：action spec 断言状态机翻转数值，本 spec 断言 DOM 结构可达。

import { test, expect, loginAndNavigate } from '../fixtures';
import {
  createViaSave,
  callMutationOk,
  deleteById,
} from '../business-actions/_helper';

const EQ_ID = 1;
const ASSIGNED_TO = 2;
const VISIT_DATE = '2026-12-26';
const WIZARD_ROUTE = '/mnt-visit-wizard';

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

test.describe('F12/F16 — maintenance visit-wizard DOM structure', () => {
  test('renders page metadata + step indicator + Step 1 start area + IN_PROGRESS Step 2-4 areas', async ({ page }) => {
    await loginAndNavigate(page, WIZARD_ROUTE);
    // page metadata 可达：维护访问选择 select 渲染
    await expect(page.locator('label').filter({ hasText: '维护访问' })).toBeVisible({ timeout: 20_000 });

    // 建测试维护访问（DRAFT → schedule → SCHEDULED，使 wizard Step 1 "开始执行" 可见）
    const code = uniq('E2E-MNT-WIZVIS');
    const visit = await createViaSave(
      page, 'ErpMntVisit',
      {
        code,
        equipmentId: EQ_ID,
        visitDate: VISIT_DATE,
        status: 'DRAFT',
        assignedTo: ASSIGNED_TO,
        visitType: 'PLANNED',
        orgId: 2,
      },
      'id status',
    );

    try {
      // schedule: DRAFT → SCHEDULED（wizard Step 1 "开始执行" 仅 SCHEDULED 可见）
      await callMutationOk(
        page, 'ErpMntVisit', 'schedule', { visitId: visit.id }, 'id status',
      );

      // ── 1. page metadata 可达（URL）──
      await expect.poll(
        () => page.url(),
        { timeout: 20_000, message: 'wizard route should be reachable' },
      ).toContain(WIZARD_ROUTE);

      // ── 2. 选维护访问 + 进入向导 → steps 指示器结构 ──
      await page.reload();
      await expect(page.locator('label').filter({ hasText: '维护访问' })).toBeVisible({ timeout: 20_000 });
      const select = page.locator('.cxd-Select').first();
      await select.click();
      await page.waitForTimeout(500);
      const selectInput = page.locator('.cxd-Select input').first();
      if (await selectInput.count() > 0) {
        await selectInput.fill(code);
        await page.waitForTimeout(800);
      }
      const option = page.locator('.cxd-Select-menu').locator('div, label, span').filter({ hasText: code }).first();
      await expect.poll(
        async () => option.count(),
        { timeout: 15_000, message: 'visit option should appear in select menu' },
      ).toBeGreaterThan(0);
      await option.click();
      await page.waitForTimeout(300);
      // 进入向导
      await page.getByRole('button', { name: /进入向导/ }).click();
      // wizard service 渲染：当前维护访问 alert
      await expect.poll(
        async () => (await page.textContent('body')) || '',
        { timeout: 20_000, message: 'wizard body should render current-visit alert' },
      ).toContain('维护访问');

      // steps 指示器色块：4 步标签渲染（预渲染 HTML 单 tpl）
      const bodyText = (await page.textContent('body')) || '';
      expect(bodyText, 'step indicator should render info step').toContain('维护信息确认');
      expect(bodyText, 'step indicator should render spare step').toContain('备件消耗');
      expect(bodyText, 'step indicator should render complete step').toContain('确认完成');

      // ── 3. Step 1 "开始执行" button 在 SCHEDULED 态可见 ──
      await expect(page.getByRole('button', { name: /开始执行/ })).toBeVisible({ timeout: 15_000 });

      // ── 4. start mutation 后 step indicator 前进 + Step 2-4 区域在 IN_PROGRESS 可见 ──
      await page.getByRole('button', { name: /开始执行/ }).click();
      // 确认 dialog（confirmText）
      await page.waitForTimeout(800);
      const confirmBtn = page.locator('.cxd-Modal').locator('button', { hasText: /确认/ }).first();
      if (await confirmBtn.count() > 0) {
        await confirmBtn.click();
      }
      // reload wizardService 后 IN_PROGRESS 区域渲染
      await expect.poll(
        async () => (await page.textContent('body')) || '',
        { timeout: 20_000, message: 'Step 2 spare part area should render on IN_PROGRESS' },
      ).toContain('备件消耗');
      await expect.poll(
        async () => (await page.textContent('body')) || '',
        { timeout: 20_000, message: 'Step 3 result area should render on IN_PROGRESS' },
      ).toContain('执行结果');
      // "确认完成" button 在 IN_PROGRESS 可见
      await expect(page.getByRole('button', { name: /确认完成/ })).toBeVisible({ timeout: 15_000 });
    } finally {
      await deleteById(page, 'ErpMntVisit', visit.id);
    }
  });
});
