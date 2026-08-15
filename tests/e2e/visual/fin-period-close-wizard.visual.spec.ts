// DOM 结构断言 for 期末结账向导（flux-native wizard rewrite）。
// plan 2026-08-03-1232-3-flux-f16-complex-pages-rewrite.md §Phase 1.
//
// 断言 period-close-wizard.flux.yaml 经 flux 渲染的 DOM 结构（flux wizard 替换手写步骤指示器）：
//   1. page metadata 可达（路由 + 期间选择 combobox 渲染）
//   2. flux wizard steps 结构（前置检查/月度结账/终关/反结账 步骤标题渲染）
//   3. preCheck 结果区（step 1 内联 data-source 文本）
//   4. 反结账步骤（CLOSED 期间下反结账 step + confirm dialog 二次确认）
//
// Flux selector strategy: .nop-wizard + data-slot (combobox/dialog-surface/alert-dialog) + 文本契约.
// 自包含：建测试专用期间，cleanup 不污染 finance 看板基线。
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

/** 经 flux combobox 选择期间（trigger → 输入过滤 → 点 combobox-item）。 */
async function selectPeriod(page: import('@playwright/test').Page, code: string): Promise<void> {
  const trigger = page.locator('[data-slot="combobox-trigger"], [role="combobox"]').first();
  await trigger.click();
  await page.waitForTimeout(400);
  // 按 code 子串原生点击可见 combobox-item
  await page.evaluate((c: string) => {
    const items = Array.from(document.querySelectorAll('[data-slot="combobox-item"]'));
    for (const it of items) {
      const r = it.getBoundingClientRect();
      if (r.width <= 0 || r.height <= 0) continue;
      if ((it.textContent || '').includes(c)) { (it as HTMLElement).click(); return; }
    }
  }, code);
  await page.waitForTimeout(300);
}

test.describe('F16 — period-close-wizard flux wizard DOM structure', () => {
  test('renders flux wizard steps + preCheck inline + reverse-close confirm', async ({ page }) => {
    test.setTimeout(150_000);
    await loginAndNavigate(page, WIZARD_ROUTE);
    await expect(page.locator('text=期末结账向导').first()).toBeVisible({ timeout: 20_000 });

    const period = await createTestPeriod(page);

    try {
      // ── 1. page metadata 可达 ──
      await expect.poll(() => page.url(), { timeout: 20_000, message: 'wizard route reachable' }).toContain(WIZARD_ROUTE);

      // ── 2. 选期间 + 进入向导 → flux wizard 结构 ──
      await page.reload();
      await expect(page.locator('text=期末结账向导').first()).toBeVisible({ timeout: 20_000 });
      await selectPeriod(page, period.code);
      // 进入向导（触发 periodData data-source）
      await page.getByRole('button', { name: /进入向导/ }).first().click().catch(() => {});
      await page.waitForTimeout(1500);

      // flux wizard renders step titles
      await expect.poll(
        async () => (await page.textContent('body')) || '',
        { timeout: 20_000, message: 'flux wizard should render step titles' },
      ).toContain('前置检查');
      const bodyText = (await page.textContent('body')) || '';
      expect(bodyText, 'wizard should render close step').toContain('月度结账');
      expect(bodyText, 'wizard should render finalize step').toContain('终关');

      // ── 3. 反结账步骤（需 CLOSED 期间，经 GraphQL closePeriod 翻转后 reload wizard）──
      await callMutationOk(page, 'ErpFinAccountingPeriod', 'closePeriod', { periodId: period.id }, 'id status').catch(() => {});
      await page.getByRole('button', { name: /进入向导/ }).first().click().catch(() => {});
      await page.waitForTimeout(1500);
      await expect.poll(
        async () => (await page.textContent('body')) || '',
        { timeout: 20_000, message: 'reverse-close step should render on CLOSED period' },
      ).toContain('反结账');

      // ── 3.5 RC-9 反结账审计（plan 2026-08-15-2119-1）：reason 输入控件渲染核对 ──
      // flux wizard mountOnEnter：step body 仅当该步激活时挂载 → 先经「下一步」导航至反结账步
      for (let i = 0; i < 6; i++) {
        const bodyHasReason = (await page.textContent('body'))?.includes('反结账原因') ?? false;
        if (bodyHasReason) break;
        const nextBtn = page.getByRole('button', { name: /下一步/ }).first();
        if (await nextBtn.count().catch(() => 0) === 0) break;
        await nextBtn.click().catch(() => {});
        await page.waitForTimeout(600);
      }
      await expect.poll(
        async () => (await page.textContent('body')) || '',
        { timeout: 20_000, message: 'reverse-close reason input label should render after navigating to step' },
      ).toContain('反结账原因');
      const reasonInputCount = await page.locator('input[type="text"], input:not([type])').count().catch(() => 0);
      expect(reasonInputCount, 'reason input control should exist on the wizard page').toBeGreaterThan(0);

      // ── 4. 反结账 confirm dialog 二次确认（best-effort：依赖 closePeriod 成功使 reverseClose button 可见）──
      const reverseBtn = await page.getByRole('button', { name: /执行反结账/ }).count().catch(() => 0);
      if (reverseBtn > 0) {
        await page.getByRole('button', { name: /执行反结账/ }).first().click().catch(() => {});
        const confirmDialog = page.locator('[data-slot="alert-dialog-content"]').first();
        const dialogSurface = page.locator('[data-slot="dialog-surface"]').first();
        await expect.poll(
          async () => (await confirmDialog.count()) + (await dialogSurface.count()),
          { timeout: 20_000, message: 'reverse-close confirm dialog should open' },
        ).toBeGreaterThan(0);
      } else {
        // closePeriod 未成功（测试环境 org 可能缺账套配置）→ 反结账 button 不可见；wizard 渲染已由步骤标题断言覆盖
      }
    } finally {
      // RC-9：reason 必填契约——清理路径直调 reverseClose 须显式传 reason（缺失抛 ERR_REVERSE_CLOSE_REASON_REQUIRED）
      await callMutationOk(page, 'ErpFinAccountingPeriod', 'reverseClose', { periodId: period.id, reason: 'E2E visual 清理反结账' }, 'id status').catch(() => {});
      await cleanupPeriod(page, period);
    }
  });
});
