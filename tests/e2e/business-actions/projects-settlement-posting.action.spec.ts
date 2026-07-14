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
 * projects ErpPrjProjectSettlement 结算过账生命周期浏览器层 E2E（plan 2026-07-14-0742-2 Phase 2）。
 *
 * 验证结算 DIRECT @BizMutation createSettlement(CLOSE)→submit→approve→posted 全栈可达性 +
 * PROJECT_SETTLEMENT 业财过账凭证行精确数值断言（Dr 1601 固定资产 / Cr 1603 在建工程，CLOSE 资本化路径）+
 * reverseSettlement 红冲凭证行同向取负断言 + 跨域资产卡片 status 回退断言。
 *
 * 权威状态机（ErpPrjProjectSettlementProcessor，三轴 docStatus/approveStatus/posted）：
 *   createSettlement(UNSUBMITTED) --submit--> SUBMITTED --approve(doPost→posted + createAndActivateAsset)--> APPROVED
 *   APPROVED --reverseSettlement(reverse + rollbackAsset)--> posted=false
 *
 * CLOSE 硬前置（ErpPrjProjectSettlementProcessor.createSettlement:65-69）：
 *   createSettlement 调 pnlBiz.getProjectPnl(projectId)，缺 CALCULATED 快照抛 ERR_SETTLEMENT_PNL_SNAPSHOT_MISSING；
 *   getProjectPnl→findLatestCalculated **不惰性计算**（无快照返 null）。故测试流须先调
 *   ErpPrjProjectPnl__refreshPnl(projectId, periodFrom, periodTo) 产生 CALCULATED 快照，再 createSettlement。
 *
 * CLOSE approve 跨域副作用（createAndActivateAsset）：
 *   approve→IErpAstAssetBiz.save 跨域**持久化真实资产卡片**（status=IN_SERVICE, originalValue=finalCost）；
 *   reverseSettlement 仅 rollbackAssetIfNeeded 回退 status=DRAFT，**不删除卡片**——spec cleanup 显式清理资产卡片。
 *
 * finalCost 确定性派生：createSettlement 读 snapshot.totalCost（PnL 聚合 CostCollectionLine.amount）。
 *   本 spec 自包含建 CostCollection(APPROVED) + Line(MATERIAL, amount=1000) → refreshPnl → totalCost=1000。
 *   CLOSE 凭证 Dr 1601(固定资产)=finalCost / Cr 1603(在建工程)=finalCost，借贷平衡。
 *
 * 过账科目（ProjectSettlementAcctDocProvider，CLOSE + transferToAsset=true 分支）：
 *   Dr 1601 / Cr 1603（种子 id=27/id=29，0215-1 补齐）。billHeadCode = settlement.getCode()
 *   （ProjectSettlementPostingDispatcher:74 内联 setBillHeadCode，code 由 createSettlement 生成
 *   `STL-{projectId}-{millis}`，无独立 setter）。过账失败隔离（tryPost 吞异常）；冲销硬前置（reverse 失败向上抛）。
 *
 * 自包含隔离：建测试专用 Project(OPEN) + CostCollection + Line → refreshPnl → createSettlement CLOSE。
 * cleanup 逐域删除：凭证（billHeadCode=settlement.code，NORMAL+REVERSAL）→ 资产卡片（assetCardId，跨域持久化）
 *   → 折旧计划（assetId，防御性）→ 结算明细行（settlementId）→ 结算单 → PnL 快照（projectId）
 *   → 成本归集行（costCollectionId）→ 成本归集头（projectId）→ 项目。
 *
 * 种子引用：org id=2 / currency id=1（CNY）。config erp-prj.settlement-require-approval 默认 true（无须显式 arg）。
 */
const ORG_ID = 2;
const CURRENCY_ID = 1;
const COST_DATE = '2026-07-10';
const PERIOD_FROM = '2026-07-01';
const PERIOD_TO = '2026-07-31';
const FINAL_COST = 1000;

test.describe('projects ErpPrjProjectSettlement CLOSE posting lifecycle', () => {
  test('refreshPnl → createSettlement(CLOSE) → submit → approve(posted + asset) → Dr 1601/Cr 1603 → reverseSettlement + illegal guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPrjProjectSettlement-main');

    const ts = Date.now();
    const projectCode = `E2E-PRJ-STL-PRJ-${ts}`;
    const ccCode = `E2E-PRJ-STL-CC-${ts}`;

    // setup 变量声明于 try 外，setup 本体置于 try 内——任何 setup 失败亦经 finally 兜底清理（防孤儿 OPEN 项目污染 dashboard 基线）
    let project: any, costCollection: any, settlement: any, assetId: any;
    try {
      // 自包含 setup：Project(OPEN) + CostCollection(OPEN) + Line(MATERIAL, amount=1000)
      project = await createViaSave(
        page, 'ErpPrjProject',
        {
          code: projectCode, name: `E2E 结算项目 ${projectCode}`, orgId: ORG_ID, currencyId: CURRENCY_ID,
          startDate: '2026-06-01', endDate: '2026-12-31', status: 'OPEN',
        },
        'id',
      );
      costCollection = await createViaSave(
        page, 'ErpPrjCostCollection',
        {
          code: ccCode, projectId: project.id, orgId: ORG_ID, businessDate: COST_DATE, currencyId: CURRENCY_ID,
          totalAmount: FINAL_COST, docStatus: 'OPEN', approveStatus: 'APPROVED', posted: false,
          exchangeRate: '1', amountSource: FINAL_COST, amountFunctional: FINAL_COST,
        },
        'id',
      );
      await createViaSave(
        page, 'ErpPrjCostCollectionLine',
        {
          costCollectionId: costCollection.id, lineNo: 1, costCategory: 'MATERIAL',
          sourceBillType: 'PURCHASE', amount: FINAL_COST,
        },
        'id',
      );

      // refreshPnl：产生 CALCULATED 快照（createSettlement 硬前置）。totalCost = Σ line.amount = 1000。
      const pnl = await callMutationOk(
        page, 'ErpPrjProjectPnl', 'refreshPnl',
        { projectId: project.id, periodFrom: PERIOD_FROM, periodTo: PERIOD_TO },
        'id calcStatus totalCost',
      );
      expect(pnl.calcStatus, 'refreshPnl should produce CALCULATED snapshot').toBe('CALCULATED');
      expect(Number(pnl.totalCost), 'PnL totalCost should aggregate cost line=1000').toBe(FINAL_COST);

      // createSettlement(CLOSE)：读 snapshot → finalCost=1000 + transferToAsset=true
      settlement = await callMutationOk(
        page, 'ErpPrjProjectSettlement', 'createSettlement',
        { projectId: project.id, settlementType: 'CLOSE' },
        'id code approveStatus docStatus posted transferToAsset finalCost assetCardId',
      );
      expect(settlement.approveStatus, 'createSettlement should leave UNSUBMITTED').toBe('UNSUBMITTED');
      expect(settlement.transferToAsset, 'CLOSE should set transferToAsset=true').toBe(true);
      expect(Number(settlement.finalCost), 'finalCost should equal PnL totalCost=1000').toBe(FINAL_COST);
      expect(settlement.assetCardId, 'assetCardId should be null before approve').toBeNull();

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

      // approve: SUBMITTED → APPROVED + createAndActivateAsset（跨域资产卡片 IN_SERVICE）+ doPost(posted=true)
      const approved = await callMutationOk(
        page, 'ErpPrjProjectSettlement', 'approve', { id: settlement.id },
        'id approveStatus docStatus posted transferToAsset assetCardId',
      );
      expect(approved.approveStatus, 'approve should transition SUBMITTED → APPROVED').toBe('APPROVED');
      expect(approved.posted, 'approve should trigger PROJECT_SETTLEMENT posting → posted=true').toBe(true);
      expect(approved.assetCardId, 'CLOSE approve should create asset card → assetCardId non-null').toBeTruthy();

      const verified = await verifyState(
        page, 'ErpPrjProjectSettlement', settlement.id, 'approveStatus posted assetCardId',
      );
      expect(verified.approveStatus, '__get should confirm APPROVED').toBe('APPROVED');
      expect(verified.posted, '__get should confirm posted=true').toBe(true);

      assetId = verified.assetCardId;
      const assetState = await verifyState(page, 'ErpAstAsset', assetId, 'status originalValue');
      expect(assetState.status, 'capitalized asset should be IN_SERVICE').toBe('IN_SERVICE');
      expect(Number(assetState.originalValue), 'capitalized asset originalValue=finalCost=1000').toBe(FINAL_COST);

      // PROJECT_SETTLEMENT 正向凭证行精确数值断言：Dr 1601(固定资产) / Cr 1603(在建工程)，金额=finalCost=1000
      const normalVoucherId = await findVoucherIdByBillCode(page, settlement.code, 'NORMAL');
      expect(normalVoucherId, 'PROJECT_SETTLEMENT NORMAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, normalVoucherId, [
        { subjectCode: '1601', dcDirection: 'DEBIT', debitAmount: FINAL_COST, creditAmount: 0 },
        { subjectCode: '1603', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FINAL_COST },
      ]);

      // reverseSettlement：posted=false + reverse 红冲 + rollbackAsset（卡片 status→DRAFT，不删除）
      const reversed = await callMutationOk(
        page, 'ErpPrjProjectSettlement', 'reverseSettlement', { settlementId: settlement.id },
        'id posted assetCardId',
      );
      expect(reversed.posted, 'reverseSettlement should reverse posting → posted=false').toBe(false);

      const assetAfterReverse = await verifyState(page, 'ErpAstAsset', assetId, 'status');
      expect(assetAfterReverse.status, 'reverseSettlement should rollback asset status → DRAFT').toBe('DRAFT');

      // 红冲凭证行断言：REVERSAL 凭证同向取负（Dr 1601=-1000 / Cr 1603=-1000）
      const reversalVoucherId = await findVoucherIdByBillCode(page, settlement.code, 'REVERSAL');
      expect(reversalVoucherId, 'PROJECT_SETTLEMENT REVERSAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, reversalVoucherId, [
        { subjectCode: '1601', dcDirection: 'DEBIT', debitAmount: -FINAL_COST, creditAmount: 0 },
        { subjectCode: '1603', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -FINAL_COST },
      ]);
    } finally {
      // cleanup（全量置 finally + null 守卫，任一阶段失败亦清除已建产物，防跨域资产卡片/结算单/凭证污染基线）：
      //   凭证（billHeadCode=settlement.code）→ 资产卡片（assetId，跨域持久化，reverseSettlement 仅回退 status 不删除）
      //   → 折旧计划（防御性）→ 结算明细行 + 结算单 → PnL 快照 → 成本归集行/头 → 项目
      if (settlement?.code) await cleanupVoucherByBillCode(page, settlement.code);
      if (assetId) {
        await deleteByFilter(page, 'ErpAstDepreciationSchedule', eqFilter('assetId', assetId));
        await deleteById(page, 'ErpAstAsset', assetId);
      }
      if (settlement?.id) {
        await deleteByFilter(page, 'ErpPrjProjectSettlementLine', eqFilter('settlementId', settlement.id));
        await deleteById(page, 'ErpPrjProjectSettlement', settlement.id);
      }
      if (project?.id) await deleteByFilter(page, 'ErpPrjProjectPnl', eqFilter('projectId', project.id));
      if (costCollection?.id) await deleteByFilter(page, 'ErpPrjCostCollectionLine', eqFilter('costCollectionId', costCollection.id));
      if (project?.id) await deleteByFilter(page, 'ErpPrjCostCollection', eqFilter('projectId', project.id));
      if (project?.id) await deleteById(page, 'ErpPrjProject', project.id);
    }
  });
});
