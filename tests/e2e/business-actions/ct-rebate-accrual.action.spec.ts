import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, findFirst, deleteByFilter, deleteById, eqFilter, andFilter } from './_helper';

/**
 * contract ErpCtRebateAgreement 返利计提引擎业务动作浏览器层 E2E（plan 2026-07-14-0941-1 Phase 1）。
 *
 * 验证 runAccrual 经 GraphQL /graphql 的全栈可达性 + ErpCtRebateAccrual 计提明细行写入。
 *
 * 权威引擎（ErpCtRebateAgreementBizModel.runAccrual:72 → RebateEngine.accrue:53，
 * 对齐 docs/design/contract/volume-discount.md §年度返利协议 / §追溯调整）：
 *   守卫：agreement.status == ACTIVE，否则抛 ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE
 *     （message token「非生效中...不可计提」）。
 *   PROGRESSIVE：逐张聚合期间内 posted=true 发票（PURCHASE 返利读 ErpPurInvoice 按 supplierId，
 *     SALES 返利读 ErpSalInvoice 按 customerId），按 invoice.totalAmountWithTax 喂 RebateEngine.accrue。
 *   RebateEngine.accrue：loadTiers → matchTier(cumulative) → computeRebate（命中档 rebatePercent ×
 *     cumulative / 100，或 rebateAmount 固定额）→ delta = expectedRebate − alreadyAccrued → 写
 *     ErpCtRebateAccrual（rebateAgreementId/sourceBillType/sourceBillCode/billAmountSource/accruedRebate）。
 *
 * ORM 无 useWorkflow / 无 useApproval，纯 DIRECT @BizMutation 浏览器层可达。
 *
 * Phase 1 Explore 裁决：posted 发票前置最低成本路径 = 复用种子 posted 发票（非 __save 直置，
 *   非 runP2pChain 审批链）。经实测核实：ErpPurInvoice（use-approval tagSet）经 GraphQL __save 时
 *   平台强制 posted=false（即使 body 显式传 posted=true，INSERT 仍写 false——posted 仅可经完整
 *   审批-过账管道置 true，非 __save 直达）。故自包含 __save 直置 posted 发票路径不可行；runP2pChain
 *   路径会硬编码 SEED.MAT_1 并为 MAT_1 在 WH-MAIN 新增余额行 + 写 GL voucher，污染 inventory dashboard
 *   totalValue / finance 看板基线（镜像 inv-landed-cost spec 裁决）。
 *   裁定：复用种子 posted AP 发票 PINV-2026-001（orgId=2, supplierId=3=SUP-001, businessDate=2026-07-05,
 *   totalAmountWithTax=960.5, posted=true）作只读前置——runAccrual 只读发票聚合计提，不修改发票；
 *   本 spec 创建独立 agreement（partnerId=3）隔离 accrual 行（每用例自建新 agreement，
 *   loadAccruedBillCodes 按 agreementId 过滤，无跨用例串扰）。
 *
 * 确定性值（PURCHASE 返利 + 单档 10% + 种子 posted AP 发票 960.5）：
 *   RebateAgreement partnerId=3 rebateType=PURCHASE accrualMethod=PROGRESSIVE status=ACTIVE
 *     startDate=2026-01-01 endDate=2027-12-31（覆盖 invoice.businessDate=2026-07-05）
 *   RebateTier fromAmount=0 toAmount=null rebatePercent=10（开放档，命中 cumulative=960.5）
 *   → newCumulative=960.5 / matchTier=[0,null) / expectedRebate=960.5×10%=96.05
 *   → delta=96.05−0=96.05 > 0 / ErpCtRebateAccrual.accruedRebate=96.05
 *
 * 清理：计提明细（rebateAgreementId 隔离）+ Tier + Agreement。**不删种子发票**（PINV-2026-001 为
 *   共享只读 fixture，p2p-chain/p2p-reverse/o2c-chain 等多 spec 依赖）。
 *   无凭证/辅助账/库存产物（runAccrual 只读发票聚合，写 accrual 行 + 更新协议累计字段）。
 */

const PARTNER_SUPPLIER_ID = 3; // SUP-001（PINV-2026-001 supplierId）
const SEED_POSTED_AP_INVOICE_CODE = 'PINV-2026-001';
const SEED_POSTED_AP_INVOICE_AMOUNT = 960.5;
const EXPECTED_ACCRUED_REBATE = 96.05; // 960.5 × 10%（单档开放 tier）
const AS_OF_DATE = '2026-07-14';

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

async function seedAgreement(page: import('@playwright/test').Page, status: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpCtRebateAgreement',
    {
      code: uniq('E2E-REB-AG'),
      partnerId: PARTNER_SUPPLIER_ID,
      rebateType: 'PURCHASE',
      startDate: '2026-01-01',
      endDate: '2027-12-31',
      accrualMethod: 'PROGRESSIVE',
      status,
      businessDate: '2026-07-05',
    },
    'id',
  );
}

async function seedTier(page: import('@playwright/test').Page, agreementId: string | number, percent: number): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpCtRebateTier',
    {
      rebateAgreementId: Number(agreementId),
      fromAmount: 0,
      rebatePercent: percent,
    },
    'id',
  );
}

test.describe('contract ErpCtRebateAgreement runAccrual engine', () => {
  test('happy path: ACTIVE agreement + seed posted AP invoice + 10% tier → ErpCtRebateAccrual row accruedRebate=96.05', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCtRebateAgreement-main');

    const agreement = await seedAgreement(page, 'ACTIVE');
    const tier = await seedTier(page, agreement.id, 10);

    try {
      // runAccrual(agreementId, asOfDate)：PROGRESSIVE 逐张计提 posted 发票
      await callMutationOk(page, 'ErpCtRebateAgreement', 'runAccrual',
        { agreementId: agreement.id, asOfDate: AS_OF_DATE }, 'id');

      // 断言 ErpCtRebateAccrual 行写入：sourceBillCode 匹配种子发票 code + accruedRebate=96.05（10% × 960.5）
      const accrual = await findFirst<any>(
        page, 'ErpCtRebateAccrual',
        andFilter(eqFilter('rebateAgreementId', Number(agreement.id)),
                  eqFilter('sourceBillCode', SEED_POSTED_AP_INVOICE_CODE)),
        'id sourceBillType sourceBillCode billAmountSource accruedRebate accrualDate',
      );
      expect(accrual, 'runAccrual should produce ErpCtRebateAccrual row').not.toBeNull();
      expect(accrual!.sourceBillCode, 'accrual.sourceBillCode matches seed posted AP invoice code').toBe(SEED_POSTED_AP_INVOICE_CODE);
      expect(accrual!.sourceBillType, 'accrual.sourceBillType=AP_INVOICE (PURCHASE rebate)').toBe('AP_INVOICE');
      expect(Number(accrual!.accruedRebate), 'accrual.accruedRebate=96.05 (960.5 × 10% delta, no prior accrual)').toBe(EXPECTED_ACCRUED_REBATE);
      expect(Number(accrual!.billAmountSource), 'accrual.billAmountSource=invoice totalAmountWithTax').toBe(SEED_POSTED_AP_INVOICE_AMOUNT);

      // 协议累计/预估字段回写（RebateEngine.accrue:77-79）
      const agState = await verifyState(page, 'ErpCtRebateAgreement', agreement.id,
        'totalAccumulatedAmount estimatedRebateAmount');
      expect(Number(agState.totalAccumulatedAmount), 'agreement.totalAccumulatedAmount=960.5 after accrual').toBe(SEED_POSTED_AP_INVOICE_AMOUNT);
      expect(Number(agState.estimatedRebateAmount), 'agreement.estimatedRebateAmount=96.05 after accrual').toBe(EXPECTED_ACCRUED_REBATE);
    } finally {
      // 清理：accrual 行 + tier + agreement（不删种子发票）
      await deleteByFilter(page, 'ErpCtRebateAccrual', eqFilter('rebateAgreementId', Number(agreement.id)));
      await deleteById(page, 'ErpCtRebateTier', tier.id);
      await deleteById(page, 'ErpCtRebateAgreement', agreement.id);
    }
  });

  test('illegal guard: runAccrual on DRAFT agreement rejected (ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCtRebateAgreement-main');

    // DRAFT agreement（非 ACTIVE）→ runAccrual 应拒绝
    const agreement = await seedAgreement(page, 'DRAFT');

    try {
      const rej = await callMutation(page, 'ErpCtRebateAgreement', 'runAccrual',
        { agreementId: agreement.id, asOfDate: AS_OF_DATE }, 'id');
      expect(rej.errors, 'runAccrual on DRAFT agreement should be rejected').toBeTruthy();
      expect(JSON.stringify(rej.errors), 'reject should carry not-active token').toContain('不可计提');

      // 无 accrual 行写入（事务回滚）
      const accrual = await findFirst<any>(
        page, 'ErpCtRebateAccrual',
        eqFilter('rebateAgreementId', Number(agreement.id)),
        'id',
      );
      expect(accrual, 'no ErpCtRebateAccrual row should be created for DRAFT agreement').toBeNull();
    } finally {
      await deleteById(page, 'ErpCtRebateAgreement', agreement.id);
    }
  });
});
