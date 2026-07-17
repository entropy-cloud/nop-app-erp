import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  eqFilter,
  deleteByFilter,
  deleteById,
} from './_helper';
import { findFirst, cleanupVoucherByBillCode, findVoucherIdByBillCode, assertVoucherLines } from '../orchestration/_helper';

/**
 * projects ErpPrjProjectSettlement FINAL/INTERIM 损益结转浏览器层 E2E（plan 2026-07-17-1005-2 Phase 2）。
 *
 * 验证 FINAL/INTERIM 结算 approve 经 GraphQL /graphql 的全栈可达性 + PROJECT_SETTLEMENT 业财过账凭证行
 * 精确数值断言（Dr 5101 项目成本 / Cr 6001 项目收入 + 条件性 4103 本年利润）+ reverseSettlement 红冲同向取负
 * + INTERIM 凭证结构同 FINAL 对照。
 *
 * 权威凭证结构（ProjectSettlementAcctDocProvider:67-88，FINAL/INTERIM 分支）：
 *   profitLoss = finalRevenue − finalCost
 *   Dr 5101(项目成本) = finalCost
 *   条件性（profitLoss.signum() != 0 才发）4103(本年利润)：signum>0 → DEBIT |profitLoss|；signum<0 → CREDIT |profitLoss|
 *   Cr 6001(项目收入) = finalRevenue
 *
 * **setup 工程化关键**：须使 finalRevenue ≠ finalCost 让 4103 行确定性出现（Phase 1 核实）。
 *   finalRevenue = snapshot.revenueAmount（来自 ErpPrjBilling.amountFunctional 聚合，sumRevenue 过滤 docStatus≠CANCELLED）
 *   finalCost = snapshot.totalCost（来自 ErpPrjCostCollectionLine.amount 按 costCategory 聚合）
 *   本 spec：Billing amountFunctional=10000 + CostCollectionLine MATERIAL amount=6000
 *     → finalRevenue=10000 / finalCost=6000 / profitLoss=4000>0 → **Dr 4103=4000（DEBIT）确定性出现**
 *
 * 凭证行期望（FINAL NORMAL，billHeadCode=settlement.code）：
 *   Dr 5101=6000 / Dr 4103=4000 / Cr 6001=10000
 * 红冲（FINAL REVERSAL，同向取负 dcDirection 不变金额取负）：
 *   Dr 5101=-6000 / Dr 4103=-4000 / Cr 6001=-10000
 * INTERIM 凭证结构与 FINAL 同（ProjectSettlementAcctDocProvider 同分支，仅 settlementType 字面不同）。
 *
 * 三轴状态机（ErpPrjProjectSettlementProcessor，require-approval 默认 true）：
 *   createSettlement(UNSUBMITTED) --submit--> SUBMITTED --approve(doPost→posted)--> APPROVED
 *   FINAL/INTERIM transferToAsset=false（仅 CLOSE=true），故 approve 不建资产卡片（区别 CLOSE 0742-2）。
 *
 * docStatus 字典修正（执行期 Phase 2 捕获）：ErpPrjBilling/CostCollection.docStatus 绑定字典
 *   erp-prj/project-status（DRAFT/OPEN/ON_HOLD/COMPLETED/CANCELLED，无 APPROVED），__save 传 APPROVED 抛
 *   「非法的字典项:APPROVED」。同 0742-2 范式：docStatus=OPEN + approveStatus=APPROVED（sumRevenue/sumCostByCategory
 *   仅过滤 docStatus≠CANCELLED，OPEN 命中聚合）。
 *
 * 自包含隔离：建测试专用 Project(OPEN) + Billing + CostCollection + Line → refreshPnl 产 CALCULATED 快照 →
 *   createSettlement。cleanup 逐域删除：凭证（billHeadCode=settlement.code）→ 结算明细行 → 结算单 →
 *   PnL 快照 → 成本归集行/头 → Billing → 项目。FINAL/INTERIM 无资产卡片/折旧计划产物（transferToAsset=false）。
 *
 * 种子引用：org id=2 / currency id=1（CNY）/ customer id=1（CUST-001，Billing.customerId）。
 * 科目 6001 id=7 + 5101 id=32 + 4103 id=34 经种子齐备（0742-2）。
 */
const ORG_ID = 2;
const CURRENCY_ID = 1;
const CUSTOMER_ID = 1; // CUST-001（Billing.customerId NOT NULL）
const BILL_DATE = '2026-07-10';
const PERIOD_FROM = '2026-07-01';
const PERIOD_TO = '2026-07-31';
const REVENUE = 10000;
const COST = 6000;
const PROFIT_LOSS = REVENUE - COST; // 4000 > 0 → Dr 4103 DEBIT

interface Setup {
  project: any;
  billing: any;
  costCollection: any;
}

async function seedSetup(page: import('@playwright/test').Page, tag: string): Promise<Setup> {
  const ts = Date.now();
  const project = await createViaSave(
    page, 'ErpPrjProject',
    {
      code: `E2E-PRJ-PNL-${tag}-${ts}`, name: `E2E 损益结转项目 ${tag}`, orgId: ORG_ID, currencyId: CURRENCY_ID,
      startDate: '2026-06-01', endDate: '2026-12-31', status: 'OPEN',
    },
    'id',
  );
  const billing = await createViaSave(
    page, 'ErpPrjBilling',
    {
      code: `E2E-PRJ-PNL-BIL-${tag}-${ts}`, projectId: project.id, orgId: ORG_ID, customerId: CUSTOMER_ID,
      businessDate: BILL_DATE, currencyId: CURRENCY_ID, exchangeRate: '1',
      totalAmount: REVENUE, amountSource: REVENUE, amountFunctional: REVENUE,
      docStatus: 'OPEN', approveStatus: 'APPROVED',
    },
    'id',
  );
  const costCollection = await createViaSave(
    page, 'ErpPrjCostCollection',
    {
      code: `E2E-PRJ-PNL-CC-${tag}-${ts}`, projectId: project.id, orgId: ORG_ID, businessDate: BILL_DATE,
      currencyId: CURRENCY_ID, totalAmount: COST, docStatus: 'OPEN', approveStatus: 'APPROVED', posted: false,
      exchangeRate: '1', amountSource: COST, amountFunctional: COST,
    },
    'id',
  );
  await createViaSave(
    page, 'ErpPrjCostCollectionLine',
    {
      costCollectionId: costCollection.id, lineNo: 1, costCategory: 'MATERIAL',
      sourceBillType: 'PURCHASE', amount: COST,
    },
    'id',
  );
  return { project, billing, costCollection };
}

async function cleanupSetup(page: import('@playwright/test').Page, s: Setup, settlement: any): Promise<void> {
  if (settlement?.code) await cleanupVoucherByBillCode(page, settlement.code);
  if (settlement?.id) {
    await deleteByFilter(page, 'ErpPrjProjectSettlementLine', eqFilter('settlementId', settlement.id));
    await deleteById(page, 'ErpPrjProjectSettlement', settlement.id);
  }
  if (s?.project?.id) await deleteByFilter(page, 'ErpPrjProjectPnl', eqFilter('projectId', s.project.id));
  if (s?.costCollection?.id) await deleteByFilter(page, 'ErpPrjCostCollectionLine', eqFilter('costCollectionId', s.costCollection.id));
  if (s?.project?.id) await deleteByFilter(page, 'ErpPrjCostCollection', eqFilter('projectId', s.project.id));
  if (s?.billing?.id) await deleteById(page, 'ErpPrjBilling', s.billing.id);
  if (s?.project?.id) await deleteById(page, 'ErpPrjProject', s.project.id);
}

// FINAL/INTERIM 共用凭证行期望（profitLoss=4000>0 → Dr 4103 DEBIT）
function expectedNormalLines() {
  return [
    { subjectCode: '5101', dcDirection: 'DEBIT', debitAmount: COST, creditAmount: 0 },
    { subjectCode: '4103', dcDirection: 'DEBIT', debitAmount: PROFIT_LOSS, creditAmount: 0 },
    { subjectCode: '6001', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: REVENUE },
  ];
}
function expectedReversalLines() {
  return [
    { subjectCode: '5101', dcDirection: 'DEBIT', debitAmount: -COST, creditAmount: 0 },
    { subjectCode: '4103', dcDirection: 'DEBIT', debitAmount: -PROFIT_LOSS, creditAmount: 0 },
    { subjectCode: '6001', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -REVENUE },
  ];
}

test.describe('projects ErpPrjProjectSettlement FINAL/INTERIM PnL transfer voucher lines', () => {
  test('FINAL: refreshPnl → createSettlement → submit → approve(posted) → Dr 5101/Dr 4103/Cr 6001 → reverseSettlement + illegal guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPrjProjectSettlement-main');

    const s = await seedSetup(page, 'FINAL');
    let settlement: any;
    try {
      // refreshPnl：产生 CALCULATED 快照（createSettlement 硬前置）。revenueAmount=10000 / totalCost=6000。
      const pnl = await callMutationOk(
        page, 'ErpPrjProjectPnl', 'refreshPnl',
        { projectId: s.project.id, periodFrom: PERIOD_FROM, periodTo: PERIOD_TO },
        'id calcStatus revenueAmount totalCost',
      );
      expect(pnl.calcStatus, 'refreshPnl should produce CALCULATED snapshot').toBe('CALCULATED');
      expect(Number(pnl.revenueAmount), 'PnL revenueAmount=10000 (Billing aggregate)').toBe(REVENUE);
      expect(Number(pnl.totalCost), 'PnL totalCost=6000 (CostCollectionLine aggregate)').toBe(COST);

      // createSettlement(FINAL)：finalRevenue ≠ finalCost → profitLoss=4000 → 4103 行确定性出现
      settlement = await callMutationOk(
        page, 'ErpPrjProjectSettlement', 'createSettlement',
        { projectId: s.project.id, settlementType: 'FINAL' },
        'id code approveStatus docStatus posted transferToAsset finalRevenue finalCost finalProfit assetCardId',
      );
      expect(settlement.approveStatus, 'createSettlement should leave UNSUBMITTED').toBe('UNSUBMITTED');
      expect(settlement.transferToAsset, 'FINAL should set transferToAsset=false').toBe(false);
      expect(Number(settlement.finalRevenue), 'finalRevenue=10000').toBe(REVENUE);
      expect(Number(settlement.finalCost), 'finalCost=6000').toBe(COST);
      expect(Number(settlement.finalProfit), 'finalProfit=4000 (revenue-cost)').toBe(PROFIT_LOSS);
      expect(settlement.assetCardId, 'FINAL should not create asset card').toBeNull();

      // 非法迁移守卫：UNSUBMITTED → approve（须 SUBMITTED，settlement-require-approval=true）须被拒
      const rejApprove = await callMutation(
        page, 'ErpPrjProjectSettlement', 'approve', { id: settlement.id }, 'id',
      );
      expect(rejApprove.errors, 'approve from UNSUBMITTED should be rejected').toBeTruthy();

      // submit: UNSUBMITTED → SUBMITTED
      const submitted = await callMutationOk(
        page, 'ErpPrjProjectSettlement', 'submit', { id: settlement.id }, 'id approveStatus',
      );
      expect(submitted.approveStatus, 'submit should transition UNSUBMITTED → SUBMITTED').toBe('SUBMITTED');

      // approve: SUBMITTED → APPROVED + doPost(posted=true)，无资产卡片
      const approved = await callMutationOk(
        page, 'ErpPrjProjectSettlement', 'approve', { id: settlement.id },
        'id approveStatus docStatus posted transferToAsset assetCardId',
      );
      expect(approved.approveStatus, 'approve should transition SUBMITTED → APPROVED').toBe('APPROVED');
      expect(approved.posted, 'approve should trigger PROJECT_SETTLEMENT posting → posted=true').toBe(true);
      expect(approved.assetCardId, 'FINAL approve should NOT create asset card').toBeNull();

      const verified = await verifyState(
        page, 'ErpPrjProjectSettlement', settlement.id, 'approveStatus posted',
      );
      expect(verified.approveStatus, '__get should confirm APPROVED').toBe('APPROVED');
      expect(verified.posted, '__get should confirm posted=true').toBe(true);

      // PROJECT_SETTLEMENT 正向凭证行精确数值断言（FINAL，profitLoss=4000>0 → Dr 4103 DEBIT）
      const normalVoucherId = await findVoucherIdByBillCode(page, settlement.code, 'NORMAL');
      expect(normalVoucherId, 'PROJECT_SETTLEMENT NORMAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, normalVoucherId, expectedNormalLines());

      // reverseSettlement：posted=false + reverse 红冲（无资产卡片回退）
      const reversed = await callMutationOk(
        page, 'ErpPrjProjectSettlement', 'reverseSettlement', { settlementId: settlement.id },
        'id posted',
      );
      expect(reversed.posted, 'reverseSettlement should reverse posting → posted=false').toBe(false);

      // 红冲凭证行断言（REVERSAL，同向取负）
      const reversalVoucherId = await findVoucherIdByBillCode(page, settlement.code, 'REVERSAL');
      expect(reversalVoucherId, 'PROJECT_SETTLEMENT REVERSAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, reversalVoucherId, expectedReversalLines());
    } finally {
      await cleanupSetup(page, s, settlement);
    }
  });

  test('INTERIM: approve → posted + voucher structure same as FINAL (Dr 5101/Dr 4103/Cr 6001)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPrjProjectSettlement-main');

    const s = await seedSetup(page, 'INTERIM');
    let settlement: any;
    try {
      await callMutationOk(
        page, 'ErpPrjProjectPnl', 'refreshPnl',
        { projectId: s.project.id, periodFrom: PERIOD_FROM, periodTo: PERIOD_TO },
        'id calcStatus',
      );

      settlement = await callMutationOk(
        page, 'ErpPrjProjectSettlement', 'createSettlement',
        { projectId: s.project.id, settlementType: 'INTERIM' },
        'id code approveStatus transferToAsset finalRevenue finalCost',
      );
      expect(settlement.transferToAsset, 'INTERIM should set transferToAsset=false').toBe(false);
      expect(Number(settlement.finalRevenue), 'INTERIM finalRevenue=10000').toBe(REVENUE);
      expect(Number(settlement.finalCost), 'INTERIM finalCost=6000').toBe(COST);

      // submit → approve
      await callMutationOk(
        page, 'ErpPrjProjectSettlement', 'submit', { id: settlement.id }, 'id approveStatus',
      );
      const approved = await callMutationOk(
        page, 'ErpPrjProjectSettlement', 'approve', { id: settlement.id },
        'id approveStatus posted',
      );
      expect(approved.approveStatus, 'INTERIM approve → APPROVED').toBe('APPROVED');
      expect(approved.posted, 'INTERIM approve → posted=true').toBe(true);

      // INTERIM 凭证结构同 FINAL（ProjectSettlementAcctDocProvider 同分支，仅 settlementType 字面不同）
      const normalVoucherId = await findVoucherIdByBillCode(page, settlement.code, 'NORMAL');
      expect(normalVoucherId, 'INTERIM NORMAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, normalVoucherId, expectedNormalLines());
    } finally {
      await cleanupSetup(page, s, settlement);
    }
  });
});
