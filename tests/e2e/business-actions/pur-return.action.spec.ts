import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById, deleteByFilter, findPageTotal, eqFilter, andFilter } from './_helper';
import {
  runP2pChain,
  cleanupP2p,
  cleanupVoucherByBillCode,
  cleanupArApByCode,
  findItems,
  SEED,
} from '../orchestration/_helper';
import type { Page } from '@playwright/test';

/**
 * purchase ErpPurReturn 业务动作浏览器层 E2E（plan 2026-07-10-0335-1 Phase 2）。
 *
 * 验证 DIRECT useApproval 审批轴（submitForApproval→approve→approveStatus 翻转 + posted=true 过账触发）+
 * reject 守卫 + cancel 迁移。
 *
 * 权威状态机（ErpPurReturnProcessor）：
 *   UNSUBMITTED --submit--> SUBMITTED --approve--> APPROVED（触发反向出库 OUTGOING + PURCHASE_RETURN 红字过账 posted=true）
 *   SUBMITTED --reject--> REJECTED
 *   非终态 docStatus --cancel--> CANCELLED
 *   submit 前置：lines 非空 + supplier ACTIVE；approve 前置：receive APPROVED + lines + qty 校验 + reason（config 默认必填）。
 *
 * approve 需上游已审批 Receive（requireSourceReceiveApproved）+ 行 + 库存（反向出库）。复用 orchestration
 * runP2pChain 产 approved Receive + 库存（INCOMING 10 单），return 行引用 receiveLine、qty≤10。
 *
 * 清理：approve 触发 OUTGOING 移动 + PURCHASE_RETURN 凭证 + AR-AP 辅助账（不可逆下游产物），
 * 逐域逻辑删除 + cleanupP2p 清理链路，保护共享 DB 数值断言基线。
 */

const BDATE = '2026-07-09';

async function cleanupReturnDownstream(page: Page, returnCode: string, materialId: number, warehouseId: number): Promise<void> {
  if (!returnCode) return;
  // GL 凭证 + AR-AP 辅助账（经 billCode/sourceBillCode 关联）
  await cleanupVoucherByBillCode(page, returnCode);
  await cleanupArApByCode(page, returnCode);
  // 反向出库移动单（relatedBillType=ERP_PUR_RETURN）+ 流水 + 余额
  const moves = await findItems<{ id: number; code: string }>(
    page, 'ErpInvStockMove',
    andFilter(eqFilter('relatedBillType', 'ERP_PUR_RETURN'), eqFilter('relatedBillCode', returnCode)),
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

test.describe('purchase ErpPurReturn approval axis + posted side-effect', () => {
  test('approve path: P2P chain → save Return(receiveId) → submit → approve(APPROVED, posted=true)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurReturn-main');

    const p2p = await runP2pChain(page);
    try {
      const retCode = `E2E-PUR-RET-${Date.now()}`;
      const ret = await createViaSave(
        page, 'ErpPurReturn',
        {
          code: retCode,
          orgId: SEED.ORG,
          receiveId: p2p.receive.id,
          supplierId: SEED.SUPPLIER,
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

      // 退货行：引用 receiveLine，qty=5（≤ receive 10），reason 必填（config 默认 true）
      await createViaSave(
        page, 'ErpPurReturnLine',
        {
          returnId: ret.id,
          receiveLineId: p2p.rcvLine.id,
          lineNo: 1,
          materialId: SEED.MAT_1,
          uoMId: SEED.UOM,
          quantity: 5,
          unitPrice: 5,
          reason: 'E2E defective batch',
        },
        'id',
      );

      // 审批轴：submit → SUBMITTED
      await callMutationOk(page, 'ErpPurReturn', 'submitForApproval', { id: ret.id }, 'id');
      let s = await verifyState(page, 'ErpPurReturn', ret.id, 'approveStatus');
      expect(s.approveStatus, 'after submit approveStatus=SUBMITTED').toBe('SUBMITTED');

      // approve → APPROVED + posted=true（触发反向出库 + PURCHASE_RETURN 红字过账）
      await callMutationOk(page, 'ErpPurReturn', 'approve', { id: ret.id }, 'id posted');
      s = await verifyState(page, 'ErpPurReturn', ret.id, 'approveStatus posted');
      expect(s.approveStatus, 'after approve approveStatus=APPROVED').toBe('APPROVED');
      expect(s.posted, 'after approve posted=true (posting triggered)').toBe(true);

      // 清理退货下游 + 退货自身
      await cleanupReturnDownstream(page, retCode, SEED.MAT_1, SEED.WH_RAW);
      await deleteByFilter(page, 'ErpPurReturnLine', eqFilter('returnId', Number(ret.id)));
      await deleteById(page, 'ErpPurReturn', ret.id);
    } finally {
      await cleanupP2p(page, p2p);
    }
  });

  test('reject + cancel path: save Return → submit(SUBMITTED) → reject(REJECTED); save → cancel(CANCELLED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurReturn-main');

    // reject 路径：save（supplierId active + line）→ submit → reject
    const rjCode = `E2E-PUR-RET-RJ-${Date.now()}`;
    const rj = await createViaSave(
      page, 'ErpPurReturn',
      {
        code: rjCode,
        orgId: SEED.ORG,
        supplierId: SEED.SUPPLIER,
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
      page, 'ErpPurReturnLine',
      { returnId: rj.id, lineNo: 1, materialId: SEED.MAT_1, uoMId: SEED.UOM, quantity: 1, reason: 'E2E reject test' },
      'id',
    );

    await callMutationOk(page, 'ErpPurReturn', 'submitForApproval', { id: rj.id }, 'id');
    let s = await verifyState(page, 'ErpPurReturn', rj.id, 'approveStatus');
    expect(s.approveStatus, 'after submit approveStatus=SUBMITTED').toBe('SUBMITTED');

    await callMutationOk(page, 'ErpPurReturn', 'reject', { id: rj.id }, 'id');
    s = await verifyState(page, 'ErpPurReturn', rj.id, 'approveStatus');
    expect(s.approveStatus, 'after reject approveStatus=REJECTED').toBe('REJECTED');

    // 非法迁移守卫：REJECTED → approve（须 SUBMITTED）
    const rej = await callMutation(page, 'ErpPurReturn', 'approve', { id: rj.id }, 'id');
    expect(rej.errors, 'approve from REJECTED should be rejected').toBeTruthy();

    await deleteByFilter(page, 'ErpPurReturnLine', eqFilter('returnId', Number(rj.id)));
    await deleteById(page, 'ErpPurReturn', rj.id);

    // cancel 路径：save → cancel(CANCELLED)
    const cnCode = `E2E-PUR-RET-CN-${Date.now()}`;
    const cn = await createViaSave(
      page, 'ErpPurReturn',
      {
        code: cnCode,
        orgId: SEED.ORG,
        supplierId: SEED.SUPPLIER,
        warehouseId: SEED.WH_RAW,
        businessDate: BDATE,
        currencyId: SEED.CURRENCY,
        docStatus: 'DRAFT',
        approveStatus: 'UNSUBMITTED',
        posted: false,
      },
      'id docStatus',
    );

    await callMutationOk(page, 'ErpPurReturn', 'cancel', { returnId: cn.id }, 'id docStatus');
    s = await verifyState(page, 'ErpPurReturn', cn.id, 'docStatus');
    expect(s.docStatus, 'after cancel docStatus=CANCELLED').toBe('CANCELLED');

    await deleteById(page, 'ErpPurReturn', cn.id);
  });
});
