import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById, deleteByFilter, eqFilter, andFilter } from './_helper';
import {
  runO2cChain,
  cleanupO2c,
  cleanupVoucherByBillCode,
  cleanupArApByCode,
  findItems,
  findVoucherIdByBillCode,
  assertVoucherLines,
  SEED,
} from '../orchestration/_helper';
import type { Page } from '@playwright/test';

/**
 * sales ErpSalReturn 业务动作浏览器层 E2E（plan 2026-07-10-0335-1 Phase 2）。
 *
 * 验证 DIRECT useApproval 审批轴（submitForApproval→approve→approveStatus 翻转 + posted=true 过账触发）+
 * reject 守卫 + cancel 迁移。
 *
 * 权威状态机（ErpSalReturnProcessor）：
 *   UNSUBMITTED --submit--> SUBMITTED --approve--> APPROVED（触发反向入库 INCOMING + SALES_RETURN 凭证 + 负 AR 辅助账 posted=true）
 *   SUBMITTED --reject--> REJECTED
 *   非终态 docStatus --cancel--> CANCELLED
 *   submit 前置：lines 非空 + customer ACTIVE；approve 前置：delivery APPROVED + lines + qty 校验 + reason（config 默认必填）。
 *
 * approve 需上游已审批 Delivery（requireSourceDeliveryApproved）+ 行 + 库存（反向入库）。复用 orchestration
 * runO2cChain 产 approved Delivery + 库存（含备货前置），return 行引用 deliveryLine、qty≤10。
 *
 * 清理：approve 触发 INCOMING 移动 + SALES_RETURN 凭证 + AR-AP 辅助账（不可逆下游产物），
 * 逐域逻辑删除 + cleanupO2c 清理链路，保护共享 DB 数值断言基线。
 */

const BDATE = '2026-07-09';

async function cleanupReturnDownstream(page: Page, returnCode: string, materialId: number, warehouseId: number): Promise<void> {
  if (!returnCode) return;
  await cleanupVoucherByBillCode(page, returnCode);
  await cleanupArApByCode(page, returnCode);
  const moves = await findItems<{ id: number; code: string }>(
    page, 'ErpInvStockMove',
    andFilter(eqFilter('relatedBillType', 'ERP_SAL_RETURN'), eqFilter('relatedBillCode', returnCode)),
    'id code',
  );
  for (const m of moves) {
    await cleanupVoucherByBillCode(page, m.code);
    await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('moveId', Number(m.id)));
    await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(m.id)));
    await deleteById(page, 'ErpInvStockMove', m.id);
  }
  await deleteByFilter(page, 'ErpInvStockBalance', andFilter(eqFilter('materialId', materialId), eqFilter('warehouseId', warehouseId)));
}

test.describe('sales ErpSalReturn approval axis + posted side-effect', () => {
  test('approve path: O2C chain → save Return(deliveryId) → submit → approve(APPROVED, posted=true)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpSalReturn-main');

    const o2c = await runO2cChain(page);
    try {
      const retCode = `E2E-SAL-RET-${Date.now()}`;
      const ret = await createViaSave(
        page, 'ErpSalReturn',
        {
          code: retCode,
          orgId: SEED.ORG,
          deliveryId: o2c.delivery.id,
          customerId: SEED.CUSTOMER,
          warehouseId: SEED.WH_RAW,
          businessDate: BDATE,
          currencyId: SEED.CURRENCY,
          docStatus: 'DRAFT',
          approveStatus: 'UNSUBMITTED',
          posted: false,
        },
        'id approveStatus',
      );
      expect(ret.approveStatus, 'new return approveStatus=UNSUBMITTED').toBe('UNSUBMITTED');

      await createViaSave(
        page, 'ErpSalReturnLine',
        {
          returnId: ret.id,
          deliveryLineId: o2c.dlvLine.id,
          lineNo: 1,
          materialId: SEED.MAT_1,
          uoMId: SEED.UOM,
          quantity: 5,
          unitPrice: 10,
          reason: 'E2E customer return',
        },
        'id',
      );

      await callMutationOk(page, 'ErpSalReturn', 'submitForApproval', { id: ret.id }, 'id');
      let s = await verifyState(page, 'ErpSalReturn', ret.id, 'approveStatus');
      expect(s.approveStatus, 'after submit approveStatus=SUBMITTED').toBe('SUBMITTED');

      await callMutationOk(page, 'ErpSalReturn', 'approve', { id: ret.id }, 'id posted');
      s = await verifyState(page, 'ErpSalReturn', ret.id, 'approveStatus posted');
      expect(s.approveStatus, 'after approve approveStatus=APPROVED').toBe('APPROVED');
      expect(s.posted, 'after approve posted=true (posting triggered)').toBe(true);

      // SALES_RETURN 凭证行精确数值断言（plan 2026-07-10-0704-1）：
      // 派生自 SalAcctDocProvider.SALES_RETURN：Dr 1401 库存=TOTAL_COST / Cr 6401 主营业务成本=TOTAL_COST。
      // TOTAL_COST=Σ 行 quantity×unitPrice=5×10=50（SalReturnPostingDispatcher.computeTotalCost，行级聚合）。
      const salReturnCost = 5 * 10;
      const salReturnVid = await findVoucherIdByBillCode(page, retCode, 'NORMAL');
      await assertVoucherLines(page, salReturnVid, [
        { subjectCode: '1401', dcDirection: 'DEBIT', debitAmount: salReturnCost, creditAmount: 0 },
        { subjectCode: '6401', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: salReturnCost },
      ]);

      await cleanupReturnDownstream(page, retCode, SEED.MAT_1, SEED.WH_RAW);
      await deleteByFilter(page, 'ErpSalReturnLine', eqFilter('returnId', Number(ret.id)));
      await deleteById(page, 'ErpSalReturn', ret.id);
    } finally {
      await cleanupO2c(page, o2c);
    }
  });

  test('reject + cancel path: save Return → submit(SUBMITTED) → reject(REJECTED); save → cancel(CANCELLED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpSalReturn-main');

    const rjCode = `E2E-SAL-RET-RJ-${Date.now()}`;
    const rj = await createViaSave(
      page, 'ErpSalReturn',
      {
        code: rjCode,
        orgId: SEED.ORG,
        customerId: SEED.CUSTOMER,
        warehouseId: SEED.WH_RAW,
        businessDate: BDATE,
        currencyId: SEED.CURRENCY,
        docStatus: 'DRAFT',
        approveStatus: 'UNSUBMITTED',
        posted: false,
      },
      'id approveStatus',
    );
    await createViaSave(
      page, 'ErpSalReturnLine',
      { returnId: rj.id, lineNo: 1, materialId: SEED.MAT_1, uoMId: SEED.UOM, quantity: 1, reason: 'E2E reject test' },
      'id',
    );

    await callMutationOk(page, 'ErpSalReturn', 'submitForApproval', { id: rj.id }, 'id');
    let s = await verifyState(page, 'ErpSalReturn', rj.id, 'approveStatus');
    expect(s.approveStatus, 'after submit approveStatus=SUBMITTED').toBe('SUBMITTED');

    await callMutationOk(page, 'ErpSalReturn', 'reject', { id: rj.id }, 'id');
    s = await verifyState(page, 'ErpSalReturn', rj.id, 'approveStatus');
    expect(s.approveStatus, 'after reject approveStatus=REJECTED').toBe('REJECTED');

    const rej = await callMutation(page, 'ErpSalReturn', 'approve', { id: rj.id }, 'id');
    expect(rej.errors, 'approve from REJECTED should be rejected').toBeTruthy();

    await deleteByFilter(page, 'ErpSalReturnLine', eqFilter('returnId', Number(rj.id)));
    await deleteById(page, 'ErpSalReturn', rj.id);

    // cancel path
    const cnCode = `E2E-SAL-RET-CN-${Date.now()}`;
    const cn = await createViaSave(
      page, 'ErpSalReturn',
      {
        code: cnCode,
        orgId: SEED.ORG,
        customerId: SEED.CUSTOMER,
        warehouseId: SEED.WH_RAW,
        businessDate: BDATE,
        currencyId: SEED.CURRENCY,
        docStatus: 'DRAFT',
        approveStatus: 'UNSUBMITTED',
        posted: false,
      },
      'id docStatus',
    );

    await callMutationOk(page, 'ErpSalReturn', 'cancel', { returnId: cn.id }, 'id docStatus');
    s = await verifyState(page, 'ErpSalReturn', cn.id, 'docStatus');
    expect(s.docStatus, 'after cancel docStatus=CANCELLED').toBe('CANCELLED');

    await deleteById(page, 'ErpSalReturn', cn.id);
  });
});
