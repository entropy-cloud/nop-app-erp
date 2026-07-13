import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  verifyState,
  eqFilter,
  deleteByFilter,
  deleteById,
} from './_helper';
import { findFirst, cleanupVoucherByBillCode } from '../orchestration/_helper';

/**
 * assets ErpAstCip 在建工程转固链业务动作浏览器层 E2E（plan 2026-07-14-0215-1 Phase 1）。
 *
 * 验证 CIP DIRECT @BizMutation 全链编排经 GraphQL /graphql 的全栈可达性 + 三态状态机迁移 +
 * 成本归集 + 完工转固（建卡 + CAPITALIZATION 业财过账触发可观测性）+ reverseTransfer 红冲回退。
 *
 * 权威状态机（ErpAstCipProcessor，cip-status 字典）：
 *   DRAFT --startConstruction--> IN_CONSTRUCTION --transferToAsset--> TRANSFERRED
 *   TRANSFERRED --reverseTransfer--> IN_CONSTRUCTION（红冲回退）
 * 成本归集：addCostItem（PURCHASE/SERVICE/LABOR/OTHER）累加 accumulatedCost；
 *   addProgressBilling 记录进度付款（不参与转固成本）。
 * 转固：transferToAsset → 构造 ErpAstAssetCapitalization(sourceType=CIP) → submit→approve
 *   （DIRECT 审批链：建 ErpAstAsset + N 折旧计划 PENDING + CAPITALIZATION(80) 过账）→
 *   CostItem.postedTransferFlag=true + CIP.status=TRANSFERRED + completedAssetId 回链。
 *
 * 资产原值 = 所选 CostItem amountFunctional 汇总（buildCapitalizationRequest 累加）。
 *
 * 过账科目依赖（Phase 1 Decision 裁定）：CapitalizationAcctDocProvider sourceType=CIP 默认科目码
 * 1601（固定资产 Dr）/ 1603（在建工程 Cr）。种子 erp_md_subject.csv 补齐（0215-1）后过账 happy-path 可达。
 *
 * 自包含隔离：自包含建 ErpAstCip（DRAFT + categoryId + currencyId + exchangeRate），唯一 code
 * `E2E-AST-CIP-<ts>`。cleanup 逐域删除：凭证（billHeadCode=cap.code，NORMAL+REVERSAL）→
 * 折旧计划（assetId）→ 资产（completedAssetId）→ 资本化单（capitalizationId via cost item）→
 * 成本归集行 + 进度付款（cipId）→ CIP。
 *
 * 种子引用：org id=2 / category id=1（AST-CAT-IT，STRAIGHT_LINE 36 月，满足 validateForApproval）/
 * currency id=1（CNY）/ acctSchema ACCT-FIN-01 id=1。
 */
const ORG_ID = 2;
const CATEGORY_ID = 1;
const CURRENCY_ID = 1;
const TRANSFER_DATE = '2026-07-10';

test.describe('assets ErpAstCip capitalization (transfer-to-asset) lifecycle', () => {
  test('startConstruction → addCostItem → addProgressBilling → transferToAsset (TRANSFERRED + asset) → reverseTransfer (IN_CONSTRUCTION)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstCip-main');

    const cipCode = `E2E-AST-CIP-${Date.now()}`;
    const cip = await createViaSave(
      page, 'ErpAstCip',
      {
        code: cipCode,
        name: `E2E 在建工程 ${cipCode}`,
        orgId: ORG_ID,
        categoryId: CATEGORY_ID,
        currencyId: CURRENCY_ID,
        businessDate: '2026-06-01',
        exchangeRate: 1,
        status: 'DRAFT',
      },
      'id code status accumulatedCost',
    );
    expect(cip.id, '__save should create a DRAFT cip').toBeTruthy();
    expect(cip.status, 'new cip status=DRAFT').toBe('DRAFT');

    try {
      // startConstruction: DRAFT → IN_CONSTRUCTION
      const started = await callMutationOk(
        page, 'ErpAstCip', 'startConstruction', { cipId: cip.id }, 'id status',
      );
      expect(started.status, 'startConstruction should transition DRAFT → IN_CONSTRUCTION').toBe('IN_CONSTRUCTION');

      // addCostItem: 归集成本（PURCHASE 500 + OTHER 300 = 800）
      const cost1 = await callMutationOk(
        page, 'ErpAstCip', 'addCostItem',
        {
          cipId: cip.id, costType: 'PURCHASE', amountFunctional: 500,
          sourceBillType: 'PURCHASE_ORDER', sourceBillCode: 'PO-E2E-CIP', remark: 'E2E 采购成本',
        },
        'id amountFunctional',
      );
      expect(Number(cost1.amountFunctional), 'cost item 1 amount').toBe(500);

      const cost2 = await callMutationOk(
        page, 'ErpAstCip', 'addCostItem',
        {
          cipId: cip.id, costType: 'OTHER', amountFunctional: 300,
          sourceBillType: 'OTHER', sourceBillCode: 'BILL-E2E-CIP', remark: 'E2E 其他成本',
        },
        'id amountFunctional',
      );
      expect(Number(cost2.amountFunctional), 'cost item 2 amount').toBe(300);

      // CIP accumulatedCost 回写断言（500 + 300 = 800）
      const cipAfterCost = await verifyState(page, 'ErpAstCip', cip.id, 'status accumulatedCost');
      expect(cipAfterCost.status, '__get should confirm IN_CONSTRUCTION').toBe('IN_CONSTRUCTION');
      expect(Number(cipAfterCost.accumulatedCost), 'accumulatedCost=500+300').toBe(800);

      // addProgressBilling: 进度付款（不参与转固成本）
      const billing = await callMutationOk(
        page, 'ErpAstCip', 'addProgressBilling',
        {
          cipId: cip.id, billingDate: '2026-06-20', billingMilestone: 'M1',
          amountFunctional: 400, paymentVoucherCode: 'PV-E2E-CIP',
        },
        'id amountFunctional',
      );
      expect(Number(billing.amountFunctional), 'progress billing amount').toBe(400);

      // transferToAsset: IN_CONSTRUCTION → TRANSFERRED + 建卡 + completedAssetId 回链
      const transferred = await callMutationOk(
        page, 'ErpAstCip', 'transferToAsset',
        { cipId: cip.id, costItemIds: [cost1.id, cost2.id], transferDate: TRANSFER_DATE },
        'id status isCompleted completedAssetId',
      );
      expect(transferred.status, 'transferToAsset should transition IN_CONSTRUCTION → TRANSFERRED').toBe('TRANSFERRED');
      expect(transferred.isCompleted, 'transferToAsset should set isCompleted=true').toBe(true);
      expect(transferred.completedAssetId, 'transferToAsset should set completedAssetId').toBeTruthy();

      // __get 权威确认 CIP 终态
      const verifiedCip = await verifyState(
        page, 'ErpAstCip', cip.id, 'status isCompleted completedAssetId',
      );
      expect(verifiedCip.status, '__get should confirm TRANSFERRED').toBe('TRANSFERRED');
      expect(verifiedCip.completedAssetId, '__get should confirm completedAssetId').toBeTruthy();

      // 建卡资产原值 = 成本归集合计（800）经 __get 独立断言
      const assetId = verifiedCip.completedAssetId;
      const createdAsset = await verifyState(
        page, 'ErpAstAsset', assetId, 'code originalValue status',
      );
      expect(Number(createdAsset.originalValue), 'capitalized asset originalValue=800').toBe(800);
      expect(createdAsset.status, 'capitalized asset status=IN_SERVICE').toBe('IN_SERVICE');

      // 资本化单 posted 可观测性（CAPITALIZATION 过账成功，科目 1601/1603 已补齐种子）
      const cap = await findFirst<{ id: string; code: string; posted: boolean }>(
        page, 'ErpAstAssetCapitalization', eqFilter('sourceCode', cipCode), 'id code posted',
      );
      expect(cap, 'capitalization record should exist (sourceCode=cipCode)').toBeTruthy();
      expect(cap!.posted, 'CAPITALIZATION posting should succeed → cap.posted=true').toBe(true);

      // reverseTransfer: TRANSFERRED → IN_CONSTRUCTION + 红冲凭证 + 资产回退 + cap 回退
      const reversed = await callMutationOk(
        page, 'ErpAstCip', 'reverseTransfer',
        { cipId: cip.id, capitalizationId: cap!.id },
        'id status isCompleted completedAssetId',
      );
      expect(reversed.status, 'reverseTransfer should transition TRANSFERRED → IN_CONSTRUCTION').toBe('IN_CONSTRUCTION');
      expect(reversed.isCompleted, 'reverseTransfer should set isCompleted=false').toBe(false);

      const verifiedReversed = await verifyState(
        page, 'ErpAstCip', cip.id, 'status isCompleted completedAssetId',
      );
      expect(verifiedReversed.status, '__get should confirm IN_CONSTRUCTION after reverse').toBe('IN_CONSTRUCTION');
      expect(verifiedReversed.isCompleted, '__get should confirm isCompleted=false').toBe(false);

      // 清理：凭证（billHeadCode=cap.code，NORMAL+REVERSAL）→ 折旧计划（assetId）→
      // 资产（completedAssetId）→ 资本化单（cap.id）→ 成本行 + 进度付款（cipId）→ CIP
      await cleanupVoucherByBillCode(page, cap!.code);
      await deleteByFilter(page, 'ErpAstDepreciationSchedule', eqFilter('assetId', assetId));
      await deleteById(page, 'ErpAstAsset', assetId);
      await deleteById(page, 'ErpAstAssetCapitalization', cap!.id);
    } finally {
      // finally 兜底清理（按 cipId 级联成本行 + 进度付款 + CIP）
      await deleteByFilter(page, 'ErpAstCipCostItem', eqFilter('cipId', cip.id));
      await deleteByFilter(page, 'ErpAstCipProgressBilling', eqFilter('cipId', cip.id));
      await deleteById(page, 'ErpAstCip', cip.id);
    }
  });
});
