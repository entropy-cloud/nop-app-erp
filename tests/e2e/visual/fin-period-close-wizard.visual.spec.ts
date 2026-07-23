// DOM 结构断言 for F12 Tier C — 期末结账向导（plan 2026-07-23-0818-2 Phase 2）。
//
// 断言 period-close-wizard page.yaml 经 AMIS 渲染的 DOM 结构（非像素快照——稳定跨 AMIS 升级/字体漂移）：
//   1. page metadata 可达（路由 + 期间选择 form 渲染）
//   2. steps 指示器结构（each+tpl 4 步色块渲染，§8.12 先例）
//   3. preCheck 结果区（dialog 内 alert + table 结构）
//   4. 反结账 dialog 存在（CLOSED 期间下 反结账 section + dialog 结构）
//
// 自包含：建测试专用期间（OPEN → 断言 step indicator + preCheck；closePeriod → CLOSED → 断言 反结账 section），
// cleanup 删凭证 + per-module 状态行 + 期间，不污染 finance 看板基线。
// 与 fin-period-close-wizard.action.spec.ts 互补：action spec 断言状态机翻转数值，本 spec 断言 DOM 结构可达。

import { test, expect, loginAndNavigate } from '../fixtures';
import {
  createViaSave,
  callMutationOk,
  eqFilter,
  deleteByFilter,
  deleteById,
} from '../business-actions/_helper';
import { cleanupVoucherByBillCode } from '../orchestration/_helper';

const ORG = 2;
const WIZARD_ROUTE = '/fin-period-close-wizard';

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

async function createTestPeriod(page: import('@playwright/test').Page): Promise<{ id: string; code: string }> {
  const code = uniq('E2E-WIZVIS');
  const saved = await createViaSave(
    page,
    'ErpFinAccountingPeriod',
    {
      code, name: `E2E Wizard Visual ${code}`,
      orgId: ORG, year: 2026, month: 8,
      startDate: '2026-08-01', endDate: '2026-08-31',
      quarter: 3, isAdjustment: false, status: 'OPEN',
    },
    'id code',
  );
  return { id: saved.id, code: saved.code };
}

async function cleanupPeriod(page: import('@playwright/test').Page, period: { id: string; code: string }): Promise<void> {
  if (period?.code) {
    await cleanupVoucherByBillCode(page, `PERIOD-CLOSE-${period.code}`);
    await cleanupVoucherByBillCode(page, `FX-REVAL-${period.code}`);
  }
  if (period?.id) {
    await deleteByFilter(page, 'ErpFinAccountingPeriodStatus', eqFilter('periodId', Number(period.id)));
    await deleteByFilter(page, 'ErpFinTrialBalance', eqFilter('periodId', Number(period.id))).catch(() => {});
    await deleteById(page, 'ErpFinAccountingPeriod', period.id);
  }
}

test.describe('F12 Tier C — period-close-wizard DOM structure', () => {
  test('renders page metadata + step indicator + preCheck area + reverse-close section', async ({ page }) => {
    // 单次 login：导航到向导路由建立会话；后续 createViaSave/callMutationOk 经 GraphQL 复用会话
    await loginAndNavigate(page, WIZARD_ROUTE);
    // page metadata 可达：期间选择 form 渲染
    await expect(page.locator('label').filter({ hasText: '会计期间' })).toBeVisible({ timeout: 20_000 });

    // 建测试期间（经 GraphQL，避开种子 period id=1 看板基线）
    const period = await createTestPeriod(page);

    try {
      // ── 1. page metadata 可达（URL + 标题）──
      await expect.poll(
        () => page.url(),
        { timeout: 20_000, message: 'wizard route should be reachable' },
      ).toContain(WIZARD_ROUTE);

      // ── 2. 选期间 + 进入向导 → steps 指示器结构 ──
      // 重新加载页面使 select source 刷新（包含新建期间）
      await page.reload();
      await expect(page.locator('label').filter({ hasText: '会计期间' })).toBeVisible({ timeout: 20_000 });
      // 打开下拉 + 输入 code 过滤 + 选中
      const select = page.locator('.cxd-Select').first();
      await select.click();
      await page.waitForTimeout(500);
      // AMIS select 支持输入过滤；输入 code 片段缩小候选
      const selectInput = page.locator('.cxd-Select input').first();
      if (await selectInput.count() > 0) {
        await selectInput.fill(period.code);
        await page.waitForTimeout(800);
      }
      // 点击匹配 option（按 code 子串）
      const option = page.locator('.cxd-Select-menu').locator('div, label, span').filter({ hasText: period.code }).first();
      await expect.poll(
        async () => option.count(),
        { timeout: 15_000, message: 'period option should appear in select menu' },
      ).toBeGreaterThan(0);
      await option.click();
      await page.waitForTimeout(300);
      // 进入向导
      await page.getByRole('button', { name: /进入向导/ }).click();
      // wizard service 渲染：当前期间 alert
      await expect.poll(
        async () => (await page.textContent('body')) || '',
        { timeout: 20_000, message: 'wizard body should render current-period alert' },
      ).toContain('当前期间');
      // steps 指示器色块：4 步标签渲染（each+tpl 色块）
      const bodyText = (await page.textContent('body')) || '';
      expect(bodyText, 'step indicator should render preCheck step').toContain('前置检查');
      expect(bodyText, 'step indicator should render close step').toContain('月度结账');
      expect(bodyText, 'step indicator should render finalize step').toContain('终关');

      // ── 3. preCheck 结果区（dialog 结构）──
      await page.getByRole('button', { name: /执行前置检查/ }).click();
      const preCheckDialog = page.locator('.cxd-Modal').last();
      await expect.poll(
        async () => preCheckDialog.locator('table').count(),
        { timeout: 20_000, message: 'preCheck dialog should render issue-summary table' },
      ).toBeGreaterThan(0);
      await expect.poll(
        async () => preCheckDialog.textContent() || '',
        { timeout: 20_000, message: 'preCheck dialog should render allowance summary section' },
      ).toContain('坏账准备充足性');
      // 关闭 dialog
      await preCheckDialog.locator('.cxd-Modal-close').first().click().catch(() => {});
      await page.waitForTimeout(500);

      // ── 4. 反结账 dialog 存在（需 CLOSED 期间，经 GraphQL closePeriod 翻转后 reload wizard）──
      await callMutationOk(page, 'ErpFinAccountingPeriod', 'closePeriod', { periodId: period.id }, 'id status');
      // reload wizard service（重新进入向导，使 canReverse=true 生效）
      await page.getByRole('button', { name: /进入向导/ }).click();
      // 反结账 section 应渲染（visibleOn canReverse）
      await expect.poll(
        async () => (await page.textContent('body')) || '',
        { timeout: 20_000, message: 'reverse-close section should render on CLOSED period' },
      ).toContain('反结账');
      // 点击 反结账 → dialog（红冲影响预览）
      await page.getByRole('button', { name: /反结账/ }).first().click();
      const reverseDialog = page.locator('.cxd-Modal').last();
      await expect.poll(
        async () => reverseDialog.textContent() || '',
        { timeout: 20_000, message: 'reverse-close dialog should render red-impact preview' },
      ).toContain('红冲影响预览');
    } finally {
      // cleanup：若已 closePeriod 则先 reverseClose 回 OPEN（避免删除受状态约束的期间）
      await callMutationOk(page, 'ErpFinAccountingPeriod', 'reverseClose', { periodId: period.id }, 'id status').catch(() => {});
      await cleanupPeriod(page, period);
    }
  });
});
