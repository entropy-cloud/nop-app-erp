import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById, deleteByFilter, eqFilter, andFilter, input } from './_helper';
import {
  runO2cChain,
  cleanupO2c,
  cleanupVoucherByBillCode,
  cleanupArApByCode,
  findItems,
  SEED,
} from '../orchestration/_helper';
import type { Page } from '@playwright/test';

/**
 * sales ErpSalReturn 换货出库单生成业务动作浏览器层 E2E（RC-R1.51 P1-RC-025，UC-SAL-06 断言②④）。
 *
 * 流程：O2C 链 → 建 Return(returnType=EXCHANGE) → submit → approve（库存恢复 INCOMING + SALES_RETURN 凭证）→
 * generateExchangeDelivery（换货出库单 DRAFT + 双向关联 exchangeDeliveryId ↔ exchangeReturnId）→
 * 换货出库 submit → approve（OUTGOING 移动单扣库存）。
 *
 * 价差：换货行与退货行同价（5×10=50=50，Δ=0）避免补差价发票产物，聚焦断言②④。
 *
 * 清理：approve 触发 INCOMING/OUTGOING 移动 + 凭证 + AR-AP 辅助账（不可逆下游产物），
 * 逐域逻辑删除 + cleanupO2c 清理链路，保护共享 DB 数值断言基线。
 */

const BDATE = '2026-07-09';

const EX_LINE_INPUT_TYPE = '[i_app_erp_sal_biz_ErpSalExchangeDeliveryLine]';

/** 清理移动单产物：GL 凭证（billCode=move.code）+ 流水 + 移动单行 + 移动单（镜像 orchestration cleanupStockMove，未导出故内联）。 */
async function cleanupMove(page: Page, move: { id?: number; code?: string }): Promise<void> {
  if (!move) return;
  if (move.code) await cleanupVoucherByBillCode(page, move.code);
  if (move.id != null) {
    await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('moveId', Number(move.id)));
    await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(move.id)));
    await deleteById(page, 'ErpInvStockMove', move.id);
  }
}

test.describe('sales ErpSalReturn exchange flow (returnType=EXCHANGE + generateExchangeDelivery)', () => {
  test('exchange path: O2C → Return(EXCHANGE) approve → generateExchangeDelivery → delivery approve deducts stock', async ({ page }) => {
    await loginAndNavigate(page, '/ErpSalReturn-main');

    const o2c = await runO2cChain(page);
    try {
      const retCode = `E2E-SAL-RET-XC-${Date.now()}`;
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
          returnType: 'EXCHANGE',
        },
        'id approveStatus returnType',
      );
      expect(ret.returnType, 'new return returnType=EXCHANGE').toBe('EXCHANGE');

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
          reason: 'E2E exchange return',
        },
        'id',
      );

      await callMutationOk(page, 'ErpSalReturn', 'submitForApproval', { id: ret.id }, 'id');
      let s = await verifyState(page, 'ErpSalReturn', ret.id, 'approveStatus');
      expect(s.approveStatus, 'after submit approveStatus=SUBMITTED').toBe('SUBMITTED');

      await callMutationOk(page, 'ErpSalReturn', 'approve', { id: ret.id }, 'id approveStatus');
      s = await verifyState(page, 'ErpSalReturn', ret.id, 'approveStatus');
      expect(s.approveStatus, 'after approve approveStatus=APPROVED').toBe('APPROVED');

      // 断言① 库存恢复：退货审核生成 INCOMING 移动单（ERP_SAL_RETURN）
      const retMoves = await findItems<{ id: number; code: string }>(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_SAL_RETURN'), eqFilter('relatedBillCode', retCode)),
        'id code moveType docStatus',
      );
      expect(retMoves.length, 'return approve should create INCOMING move').toBe(1);
      expect(retMoves[0].moveType, 'return move type INCOMING').toBe('INCOMING');
      expect(retMoves[0].docStatus, 'return move auto DONE').toBe('DONE');

      // 断言②④ generateExchangeDelivery：换货出库单生成 + 双向关联
      const ex = await callMutationOk(
        page, 'ErpSalReturn', 'generateExchangeDelivery',
        {
          returnId: ret.id,
          lines: input(EX_LINE_INPUT_TYPE, [
            { materialId: SEED.MAT_1, uoMId: SEED.UOM, quantity: 5, unitPrice: 10, taxRate: 0 },
          ]),
        },
        'id exchangeDeliveryId',
      );
      expect(ex.exchangeDeliveryId, 'return should carry exchangeDeliveryId').toBeTruthy();

      const exCode = `EX-${retCode}`;
      const exDlv = await findItems<{ id: number; code: string; exchangeReturnId: number; docStatus: string; approveStatus: string }>(
        page, 'ErpSalDelivery',
        andFilter(eqFilter('code', exCode), eqFilter('exchangeReturnId', Number(ret.id))),
        'id code exchangeReturnId docStatus approveStatus',
      );
      expect(exDlv.length, 'exchange delivery should exist with code EX-<returnCode>').toBe(1);
      expect(exDlv[0].exchangeReturnId, 'delivery.exchangeReturnId = return.id（断言④ 反向）').toBe(Number(ret.id));
      expect(exDlv[0].docStatus, 'exchange delivery DRAFT 待标准出库审核').toBe('DRAFT');
      expect(exDlv[0].approveStatus, 'exchange delivery UNSUBMITTED').toBe('UNSUBMITTED');

      // 换货出库走既有出库状态机 → 扣库存（断言② 运行时闭合）
      await callMutationOk(page, 'ErpSalDelivery', 'submitForApproval', { id: exDlv[0].id }, 'id');
      await callMutationOk(page, 'ErpSalDelivery', 'approve', { id: exDlv[0].id }, 'id approveStatus');
      s = await verifyState(page, 'ErpSalDelivery', exDlv[0].id, 'approveStatus');
      expect(s.approveStatus, 'after approve exchange delivery APPROVED').toBe('APPROVED');

      const exMoves = await findItems<{ id: number; code: string; moveType: string; docStatus: string }>(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_SAL_DELIVERY'), eqFilter('relatedBillCode', exCode)),
        'id code moveType docStatus',
      );
      expect(exMoves.length, 'exchange delivery approve should create OUTGOING move').toBe(1);
      expect(exMoves[0].moveType, 'exchange move type OUTGOING').toBe('OUTGOING');
      expect(exMoves[0].docStatus, 'exchange move auto DONE').toBe('DONE');

      // 幂等拒绝：重复生成 → GraphQL 错误
      const dup = await callMutation(
        page, 'ErpSalReturn', 'generateExchangeDelivery',
        { returnId: ret.id, lines: input(EX_LINE_INPUT_TYPE, []) },
        'id',
      );
      expect(dup.errors, 'duplicate generateExchangeDelivery should be rejected').toBeTruthy();
      expect(JSON.stringify(dup.errors), 'duplicate rejection message should mention already-generated').toContain('已生成换货出库单');

      // 清理：换货出库下游（OUTGOING 移动 + 凭证 + 流水）→ 退货下游（INCOMING 移动 + 凭证 + AR-AP）→ 单据 → O2C
      for (const m of exMoves) {
        await cleanupMove(page, { id: m.id, code: m.code });
      }
      await cleanupVoucherByBillCode(page, exCode);
      await deleteByFilter(page, 'ErpSalDeliveryLine', eqFilter('deliveryId', Number(exDlv[0].id)));
      await deleteById(page, 'ErpSalDelivery', exDlv[0].id);

      await cleanupVoucherByBillCode(page, retCode);
      await cleanupArApByCode(page, retCode);
      for (const m of retMoves) {
        await cleanupMove(page, { id: m.id, code: m.code });
      }
      await deleteByFilter(page, 'ErpInvStockBalance', andFilter(eqFilter('materialId', SEED.MAT_1), eqFilter('warehouseId', SEED.WH_RAW)));
      await deleteByFilter(page, 'ErpSalReturnLine', eqFilter('returnId', Number(ret.id)));
      await deleteById(page, 'ErpSalReturn', ret.id);
    } finally {
      await cleanupO2c(page, o2c);
    }
  });
});
