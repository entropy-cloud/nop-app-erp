import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, findFirst, deleteByFilter, deleteById, eqFilter, andFilter } from './_helper';

/**
 * contract ErpCtRebateSettlement 返利结算单过账业务动作浏览器层 E2E（plan 2026-07-17-1005-2 Phase 2）。
 *
 * 验证 postSettlement 经 GraphQL /graphql 的全栈可达性 + 跨域负额 credit memo 发票创建 + 计提 isSettled 翻转。
 *
 * 权威实现（ErpCtRebateSettlementBizModel.postSettlement:64-117，
 * 对齐 docs/design/contract/volume-discount.md §返利信用单 / §结算流程）：
 *   守卫：settlement.status == DRAFT，否则 ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION。
 *   聚合：findUnsettledAccruals(agreementId)（eq(isSettled,false)）→ total=Σ accruedRebate。
 *   贷项凭证（负额发票）：creditAmount=total.negate()。
 *     PURCHASE→createNegativeApInvoice（ErpPurInvoice code=`CT-REBATE-{id}` supplierId=partnerId posted=false + Line）
 *     SALES →createNegativeArInvoice（ErpSalInvoice）。
 *   回链：settlement.creditMemoBillCode=`CT-REBATE-{id}` + creditMemoBillType=AP_INVOICE/AR_INVOICE。
 *   计提翻转：accrual.isSettled=true + settledDate=today。
 *   终态：settlement.status=POSTED + postedAt/postedBy。
 *
 * **按设计不过 GL 凭证**（Non-Goal，见计划）：postSettlement 跨域建负额 credit memo 发票（AP/AR 侧），
 * 故本 spec 不断言凭证行——断言 credit memo 发票 + 计提翻转。
 *
 * ORM 无 useWorkflow / 无 useApproval，纯 DIRECT @BizMutation 浏览器层可达。
 *
 * Phase 1 Explore 核实（见计划）：setup 须建 ErpCtContract(currencyId) + ErpCtContractLine(materialId) +
 *   ErpCtRebateAgreement(ACTIVE, contractId 非空驱动 resolveCurrencyId/MaterialId，否则发票 NOT NULL 违约)
 *   + Tier + runAccrual 产 accrual(isSettled=false) + ErpCtRebateSettlement(DRAFT)。
 *   复用种子 posted AP 发票 PINV-2026-001（supplierId=3）作 runAccrual 只读前置（0941-1 范式，
 *   __save 直置 posted=true 不可行 use-approval tagSet 强制 false）。
 *
 * 确定性值（PURCHASE 返利 + 单档 10% + 种子 posted AP 发票 960.5）：
 *   RebateAgreement partnerId=3 rebateType=PURCHASE contractId=自建合同 accrualMethod=PROGRESSIVE status=ACTIVE
 *   RebateTier fromAmount=0 rebatePercent=10（开放档）
 *   runAccrual → ErpCtRebateAccrual accruedRebate=96.05（960.5×10%，0941-1 已验证）
 *   postSettlement → credit memo AP 发票 totalAmount=-96.05（负额）+ accrual.isSettled=true + settlement POSTED
 *
 * 清理：credit memo 发票（code=`CT-REBATE-{settlementId}` 反查）+ 行 + accruals + settlement + tier + agreement
 *   + contractLine + contract。**不删种子发票**（PINV-2026-001 共享只读 fixture）。
 *   credit memo posted=false 故无 GL 凭证/AR-AP 辅助账产物。
 */

const PARTNER_SUPPLIER_ID = 3; // SUP-001（PINV-2026-001 supplierId）
const CURRENCY_ID = 1; // CNY（种子）
const MATERIAL_ID = 1; // MAT-001（种子，供 contractLine.materialId + 发票行 NOT NULL）
const AS_OF_DATE = '2026-07-17';
const EXPECTED_ACCRUED_REBATE = 96.05; // 960.5 × 10%（单档开放 tier，0941-1 验证）

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

test.describe('contract ErpCtRebateSettlement postSettlement orchestration', () => {
  test('happy path: DRAFT settlement + accrual → postSettlement POSTED + negative AP credit memo + accruals isSettled', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCtRebateSettlement-main');

    // setup：Contract(currencyId) + ContractLine(materialId) 驱动 resolveCurrencyId/MaterialId（否则发票 NOT NULL 违约）
    const contract = await createViaSave(
      page, 'ErpCtContract',
      {
        code: uniq('E2E-REB-STL-CT'),
        contractName: 'E2E 返利结算合同',
        contractType: 'PURCHASE',
        contractDirection: 'INBOUND',
        partnerId: PARTNER_SUPPLIER_ID,
        currencyId: CURRENCY_ID,
        startDate: '2026-01-01',
        endDate: '2027-12-31',
        status: 'NEGOTIATION',
      },
      'id',
    );
    await createViaSave(
      page, 'ErpCtContractLine',
      {
        contractId: contract.id, lineNo: 1, materialId: MATERIAL_ID,
        quantity: 100, unitPrice: 10, amount: 1000,
      },
      'id',
    );

    const agreement = await createViaSave(
      page, 'ErpCtRebateAgreement',
      {
        code: uniq('E2E-REB-STL-AG'),
        contractId: contract.id,
        partnerId: PARTNER_SUPPLIER_ID,
        rebateType: 'PURCHASE',
        startDate: '2026-01-01',
        endDate: '2027-12-31',
        accrualMethod: 'PROGRESSIVE',
        status: 'ACTIVE',
        businessDate: '2026-07-05',
      },
      'id',
    );
    const tier = await createViaSave(
      page, 'ErpCtRebateTier',
      { rebateAgreementId: Number(agreement.id), fromAmount: 0, rebatePercent: 10 },
      'id',
    );

    // runAccrual：PROGRESSIVE 逐张聚合种子 posted AP 发票 → ErpCtRebateAccrual(isSettled=false, accruedRebate=96.05)
    await callMutationOk(page, 'ErpCtRebateAgreement', 'runAccrual',
      { agreementId: agreement.id, asOfDate: AS_OF_DATE }, 'id');

    // 建结算单（DRAFT）
    const settlement = await createViaSave(
      page, 'ErpCtRebateSettlement',
      {
        rebateAgreementId: Number(agreement.id),
        settlementDate: AS_OF_DATE,
        status: 'DRAFT',
      },
      'id',
    );

    try {
      // postSettlement：DRAFT → POSTED + 负额 credit memo AP 发票 + accruals isSettled
      const posted = await callMutationOk(page, 'ErpCtRebateSettlement', 'postSettlement',
        { settlementId: settlement.id }, 'id status creditMemoBillCode creditMemoBillType');
      expect(posted.status, 'postSettlement should transition DRAFT → POSTED').toBe('POSTED');
      expect(posted.creditMemoBillCode, 'creditMemoBillCode=CT-REBATE-{id}').toBe(`CT-REBATE-${settlement.id}`);
      expect(posted.creditMemoBillType, 'PURCHASE rebate → AP_INVOICE credit memo').toBe('AP_INVOICE');

      // __get 独立断言状态翻转
      const verified = await verifyState(page, 'ErpCtRebateSettlement', settlement.id,
        'status creditMemoBillCode creditMemoBillType');
      expect(verified.status, '__get should confirm POSTED').toBe('POSTED');
      expect(verified.creditMemoBillCode, '__get should confirm creditMemoBillCode').toBe(`CT-REBATE-${settlement.id}`);

      // 跨域反查负额 credit memo AP 发票（posted=false + totalAmount<0）
      const creditMemo = await findFirst<any>(
        page, 'ErpPurInvoice',
        eqFilter('code', `CT-REBATE-${settlement.id}`),
        'id code supplierId totalAmount docStatus approveStatus posted',
      );
      expect(creditMemo, 'negative AP credit memo should be created').not.toBeNull();
      expect(Number(creditMemo!.supplierId), 'creditMemo.supplierId=agreement.partnerId').toBe(PARTNER_SUPPLIER_ID);
      expect(Number(creditMemo!.totalAmount), 'creditMemo.totalAmount should be negative (= -96.05)').toBe(-EXPECTED_ACCRUED_REBATE);
      expect(creditMemo!.posted, 'creditMemo.posted=false (not yet posted through approval chain)').toBe(false);

      // 计提 isSettled 翻转断言（findFirst by agreementId）
      const accrual = await findFirst<any>(
        page, 'ErpCtRebateAccrual',
        eqFilter('rebateAgreementId', Number(agreement.id)),
        'id isSettled',
      );
      expect(accrual, 'accrual should exist').not.toBeNull();
      expect(accrual!.isSettled, 'accrual.isSettled=true after postSettlement').toBe(true);
    } finally {
      // 清理：credit memo 发票 + 行 + accruals + settlement + tier + agreement + contractLine + contract
      const cm = await findFirst<any>(page, 'ErpPurInvoice', eqFilter('code', `CT-REBATE-${settlement.id}`), 'id');
      if (cm) {
        await deleteByFilter(page, 'ErpPurInvoiceLine', eqFilter('invoiceId', Number(cm.id)));
        await deleteById(page, 'ErpPurInvoice', cm.id);
      }
      await deleteByFilter(page, 'ErpCtRebateAccrual', eqFilter('rebateAgreementId', Number(agreement.id)));
      await deleteById(page, 'ErpCtRebateSettlement', settlement.id);
      await deleteById(page, 'ErpCtRebateTier', tier.id);
      await deleteById(page, 'ErpCtRebateAgreement', agreement.id);
      await deleteByFilter(page, 'ErpCtContractLine', eqFilter('contractId', Number(contract.id)));
      await deleteById(page, 'ErpCtContract', contract.id);
    }
  });

  test('illegal guard: postSettlement on already-POSTED settlement rejected (ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCtRebateSettlement-main');

    const contract = await createViaSave(
      page, 'ErpCtContract',
      {
        code: uniq('E2E-REB-STL-G-CT'),
        contractName: 'E2E 返利结算守卫合同',
        contractType: 'PURCHASE', contractDirection: 'INBOUND',
        partnerId: PARTNER_SUPPLIER_ID, currencyId: CURRENCY_ID,
        startDate: '2026-01-01', endDate: '2027-12-31', status: 'NEGOTIATION',
      },
      'id',
    );
    await createViaSave(
      page, 'ErpCtContractLine',
      { contractId: contract.id, lineNo: 1, materialId: MATERIAL_ID, quantity: 100, unitPrice: 10, amount: 1000 },
      'id',
    );
    const agreement = await createViaSave(
      page, 'ErpCtRebateAgreement',
      {
        code: uniq('E2E-REB-STL-G-AG'), contractId: contract.id, partnerId: PARTNER_SUPPLIER_ID,
        rebateType: 'PURCHASE', startDate: '2026-01-01', endDate: '2027-12-31',
        accrualMethod: 'PROGRESSIVE', status: 'ACTIVE', businessDate: '2026-07-05',
      },
      'id',
    );
    const tier = await createViaSave(
      page, 'ErpCtRebateTier',
      { rebateAgreementId: Number(agreement.id), fromAmount: 0, rebatePercent: 10 },
      'id',
    );
    await callMutationOk(page, 'ErpCtRebateAgreement', 'runAccrual',
      { agreementId: agreement.id, asOfDate: AS_OF_DATE }, 'id');
    const settlement = await createViaSave(
      page, 'ErpCtRebateSettlement',
      { rebateAgreementId: Number(agreement.id), settlementDate: AS_OF_DATE, status: 'DRAFT' },
      'id',
    );

    try {
      // 首次 postSettlement 成功 → POSTED
      await callMutationOk(page, 'ErpCtRebateSettlement', 'postSettlement',
        { settlementId: settlement.id }, 'id status');

      // 再次 postSettlement（POSTED → POSTED）应拒绝
      const rej = await callMutation(page, 'ErpCtRebateSettlement', 'postSettlement',
        { settlementId: settlement.id }, 'id');
      expect(rej.errors, 'postSettlement on POSTED settlement should be rejected').toBeTruthy();
      expect(JSON.stringify(rej.errors), 'reject should carry illegal-transition token').toContain('不允许');

      // 状态保持 POSTED（事务回滚）
      const verified = await verifyState(page, 'ErpCtRebateSettlement', settlement.id, 'status');
      expect(verified.status, 'status should remain POSTED after rejected repeat').toBe('POSTED');
    } finally {
      const cm = await findFirst<any>(page, 'ErpPurInvoice', eqFilter('code', `CT-REBATE-${settlement.id}`), 'id');
      if (cm) {
        await deleteByFilter(page, 'ErpPurInvoiceLine', eqFilter('invoiceId', Number(cm.id)));
        await deleteById(page, 'ErpPurInvoice', cm.id);
      }
      await deleteByFilter(page, 'ErpCtRebateAccrual', eqFilter('rebateAgreementId', Number(agreement.id)));
      await deleteById(page, 'ErpCtRebateSettlement', settlement.id);
      await deleteById(page, 'ErpCtRebateTier', tier.id);
      await deleteById(page, 'ErpCtRebateAgreement', agreement.id);
      await deleteByFilter(page, 'ErpCtContractLine', eqFilter('contractId', Number(contract.id)));
      await deleteById(page, 'ErpCtContract', contract.id);
    }
  });
});
