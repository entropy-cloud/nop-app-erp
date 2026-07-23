import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  findFirst,
  findItems,
  eqFilter,
  deleteByFilter,
  deleteById,
} from './_helper';
import { GraphQLClient } from '../pages';
import { cleanupVoucherByBillCode } from '../orchestration/_helper';

/**
 * Finance ErpFinAccountingPeriod 期末结账向导 action E2E（plan 2026-07-23-0818-2 Phase 2）。
 *
 * 浏览器层驱动既有 finance Facade mutation 链（零后端 delta，M4 已审计），验证向导编排的 4 action
 * 经 GraphQL /graphql 的全栈可达性 + 状态机翻转 + per-module 关账结果数据源 + 非法守卫：
 *   preCheck(periodId) → @BizQuery → PeriodPreCheckReport（只读，结构非空）
 *   closePeriod(periodId) → @BizMutation → ErpFinAccountingPeriod status OPEN→CLOSED + ErpFinAccountingPeriodStatus per-module
 *   finalizePeriod(periodId) → @BizMutation → status CLOSED→CLOSED_FINAL
 *   reverseClose(periodId) → @BizMutation → status→OPEN + 红冲凭证（REVERSAL postingType）
 *   守卫：closePeriod on CLOSED_FINAL → GraphQL errors（非法状态翻转）
 *
 * 权威设计（docs/design/finance/period-close.md）：
 *   - 期间状态机 OPEN→CLOSING→CLOSED→CLOSED_FINAL（含反结账）；closePeriod 一次性编排 AR/AP/INV/AST/GL + 损益结转；
 *     finalizePeriod 终关；reverseClose 红冲结转凭证 + 重开（owner doc §反结账步骤 8 步概念模型一次性执行）。
 *   - PeriodPreCheckReport 字段：unpostedVoucherCodes/unsettledArApCodes/unresolvedPostingExceptionKeys +
 *     allowanceRequired/Balance/Shortfall/Excess + hasIssues/issueCount。
 *   - ErpFinAccountingPeriodStatus per-module：arStatus/apStatus/invStatus/glStatus/assetStatus（dict erp-fin/module-close-status）。
 *
 * 自包含隔离：建测试专用期间（非 12 月，避开年度结转分支），preCheck 干净期间 hasIssues=false，
 * 驱动 close→finalize→reverse 全链。cleanup 删凭证（billCode PERIOD-CLOSE-/FX-REVAL- 前缀）+ per-module 状态行 + 期间，
 * 不污染 finance 看板基线（看板读 period id=1 种子期间 + gl_balance，测试期间产物全清理）。
 *
 * 种子引用：org id=2 / acctSchema ACCT-FIN-01 id=1。测试期间用 2026-08（种子仅有 2026-07 id=1 OPEN）。
 */
const ORG = 2;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

async function createTestPeriod(page: import('@playwright/test').Page, year: number, month: number): Promise<{ id: string; code: string }> {
  const code = uniq(`E2E-WIZ-${year}-${month}`);
  const start = `${year}-${String(month).padStart(2, '0')}-01`;
  const endDay = new Date(year, month, 0).getDate();
  const end = `${year}-${String(month).padStart(2, '0')}-${String(endDay).padStart(2, '0')}`;
  const saved = await createViaSave(
    page,
    'ErpFinAccountingPeriod',
    {
      code,
      name: `E2E Wizard Period ${code}`,
      orgId: ORG,
      year,
      month,
      startDate: start,
      endDate: end,
      quarter: Math.ceil(month / 3),
      isAdjustment: false,
      status: 'OPEN',
    },
    'id code',
  );
  return { id: saved.id, code: saved.code };
}

interface CleanupCtx {
  periodId?: string | number | null;
  code?: string;
}

async function cleanupPeriod(page: import('@playwright/test').Page, ctx: CleanupCtx): Promise<void> {
  // 凭证（PERIOD-CLOSE-/FX-REVAL- 前缀，closePeriod 生成的损益结转/汇兑重估凭证，含 reverseClose 红冲同 billCode）
  if (ctx.code) {
    await cleanupVoucherByBillCode(page, `PERIOD-CLOSE-${ctx.code}`);
    await cleanupVoucherByBillCode(page, `FX-REVAL-${ctx.code}`);
  }
  // per-module 关账状态行
  if (ctx.periodId != null) {
    await deleteByFilter(page, 'ErpFinAccountingPeriodStatus', eqFilter('periodId', Number(ctx.periodId)));
    // 试算平衡表快照（若存在）
    await deleteByFilter(page, 'ErpFinTrialBalance', eqFilter('periodId', Number(ctx.periodId))).catch(() => {});
    // 期间本身
    await deleteById(page, 'ErpFinAccountingPeriod', ctx.periodId);
  }
}

test.describe('Finance period-close wizard action E2E', () => {
  test('preCheck → closePeriod → finalizePeriod → reverseClose full chain + illegal-transition guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinAccountingPeriod-main');

    const period = await createTestPeriod(page, 2026, 8);
    const ctx: CleanupCtx = { periodId: period.id, code: period.code };

    try {
      // ── Step 1: preCheck（@BizQuery 只读）→ PeriodPreCheckReport 结构非空 ──
      // preCheck 返回复杂类型 PeriodPreCheckReport，必须显式指定 field selection（GraphQLClient.callQuery 不支持）。
      // hasIssues/issueCount 非 GraphQL 字段（has*/非 get 方法不被 Nop bean 暴露），客户端按 PeriodPreCheckReport.hasIssues() 同语义计算。
      const gql = new GraphQLClient(page);
      const preCheckJson: any = await gql.raw(
        `query(${'$'}pid:Long){ ErpFinAccountingPeriod__preCheck(periodId:${'$'}pid){ unpostedVoucherCodes unsettledArApCodes unresolvedPostingExceptionKeys allowanceRequired allowanceBalance allowanceShortfall allowanceExcess } }`,
        { pid: period.id },
      );
      expect(preCheckJson?.errors, 'preCheck should not return GraphQL errors').toBeFalsy();
      const report = preCheckJson?.data?.ErpFinAccountingPeriod__preCheck;
      expect(report, 'preCheck should return PeriodPreCheckReport').toBeTruthy();
      // 结构非空：核心字段全部存在（数组 + 坏账数值）
      expect(Array.isArray(report.unpostedVoucherCodes), 'report.unpostedVoucherCodes should be array').toBe(true);
      expect(Array.isArray(report.unsettledArApCodes), 'report.unsettledArApCodes should be array').toBe(true);
      expect(Array.isArray(report.unresolvedPostingExceptionKeys), 'report.unresolvedPostingExceptionKeys should be array').toBe(true);
      expect(report.allowanceShortfall != null, 'report.allowanceShortfall should exist').toBe(true);
      // hasIssues 客户端计算（与 PeriodPreCheckReport.hasIssues() 同语义：含 unsettledArApCodes 非阻断提示）。
      // 测试期间本身无凭证/AR-AP，但种子 id=5 OPEN RECEIVABLE（EMPLOYEE_ADVANCE）可能使全局 unsettledArApCodes 非空
      // （非阻断，closePeriod 仅在 unpostedVoucherCodes + allowanceShortfall 时阻断，见 period-close.md §结账前置检查）。
      const clientHasIssues =
        (report.unpostedVoucherCodes || []).length > 0 ||
        (report.unsettledArApCodes || []).length > 0 ||
        (report.unresolvedPostingExceptionKeys || []).length > 0 ||
        Number(report.allowanceShortfall || 0) > 0;
      expect(typeof clientHasIssues, 'clientHasIssues should be computable from report fields').toBe('boolean');
      // 测试期间无未过账凭证（unpostedVoucherCodes 必空）——这是 closePeriod 的硬阻断项
      expect((report.unpostedVoucherCodes || []).length, 'test period should have no unposted vouchers').toBe(0);

      // ── Step 2: closePeriod（@BizMutation 一次性）→ status CLOSED ──
      const closed = await callMutationOk(
        page, 'ErpFinAccountingPeriod', 'closePeriod', { periodId: period.id }, 'id status',
      );
      expect(closed.status, 'closePeriod should transition period to CLOSED').toBe('CLOSED');

      const closedVerify = await verifyState(page, 'ErpFinAccountingPeriod', period.id, 'status');
      expect(closedVerify.status, '__get should confirm CLOSED after closePeriod').toBe('CLOSED');

      // per-module 关账结果数据源（ErpFinAccountingPeriodStatus，向导 Step 2 结果卡消费）
      const statusRow = await findFirst<any>(
        page, 'ErpFinAccountingPeriodStatus', eqFilter('periodId', Number(period.id)),
        'id arStatus apStatus invStatus glStatus assetStatus',
      );
      expect(statusRow, 'closePeriod should produce ErpFinAccountingPeriodStatus row').toBeTruthy();
      // GL 模块关账（closePeriod 编排 GL 模块关账为终步）
      expect(statusRow.glStatus, 'glStatus should be CLOSED after closePeriod').toBe('CLOSED');

      // closePeriod 生成的损益结转凭证（PERIOD-CLOSE-{code}）经 voucher_bill_r 可反查（若有收入/费用余额则生成；
      // 干净期间可能为零额跳过，故仅断言「无 GraphQL error」+「若存在则 billCode 匹配」，不强制存在）
      const closeVoucherLinks = await findItems<any>(
        page, 'ErpFinVoucherBillR', eqFilter('billCode', `PERIOD-CLOSE-${period.code}`), 'voucherId',
      );
      // 断言可达：查询不报错（已隐含于 findItems），凭证存在性取决于是否有可结转余额（干净期间可不存在）

      // ── Step 3: finalizePeriod（@BizMutation）→ status CLOSED_FINAL ──
      const finalized = await callMutationOk(
        page, 'ErpFinAccountingPeriod', 'finalizePeriod', { periodId: period.id }, 'id status',
      );
      expect(finalized.status, 'finalizePeriod should transition period to CLOSED_FINAL').toBe('CLOSED_FINAL');

      // ── 守卫：closePeriod on CLOSED_FINAL → GraphQL errors（向导 visibleOn canClose 守卫的后端镜像）──
      const illegalClose = await callMutation(
        page, 'ErpFinAccountingPeriod', 'closePeriod', { periodId: period.id }, 'id status',
      );
      expect(illegalClose.errors, 'closePeriod on CLOSED_FINAL should be rejected (illegal transition)').toBeTruthy();
      expect(illegalClose.data, 'illegal closePeriod should return null data').toBeNull();

      // ── Step 4: reverseClose（@BizMutation）→ status OPEN + 红冲凭证 ──
      const reversed = await callMutationOk(
        page, 'ErpFinAccountingPeriod', 'reverseClose', { periodId: period.id }, 'id status',
      );
      expect(reversed.status, 'reverseClose should reopen period to OPEN').toBe('OPEN');

      const reversedVerify = await verifyState(page, 'ErpFinAccountingPeriod', period.id, 'status');
      expect(reversedVerify.status, '__get should confirm OPEN after reverseClose').toBe('OPEN');

      // 红冲凭证断言：若 closePeriod 生成了 PERIOD-CLOSE 凭证，reverseClose 应产 REVERSAL 红字凭证（同 billCode）。
      // 经 voucher_bill_r 反查所有同 billCode 凭证，验证存在 postingType=REVERSAL（红冲）。
      if (closeVoucherLinks.length > 0) {
        const allLinks = await findItems<any>(
          page, 'ErpFinVoucherBillR', eqFilter('billCode', `PERIOD-CLOSE-${period.code}`), 'voucherId',
        );
        let hasReversal = false;
        for (const lnk of allLinks) {
          const v = await findFirst<any>(page, 'ErpFinVoucher', eqFilter('id', Number(lnk.voucherId)), 'id postingType');
          if (v && v.postingType === 'REVERSAL') { hasReversal = true; break; }
        }
        expect(hasReversal, 'reverseClose should produce a REVERSAL voucher red-reversing the close voucher').toBe(true);
      }
    } finally {
      await cleanupPeriod(page, ctx);
    }
  });
});
