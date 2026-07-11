import { test, expect, loginAndNavigate, input, callMutationOk, callMutation, verifyState, eqFilter, andFilter, deleteByFilter, deleteById } from './_helper';
import { cleanupVoucherByBillCode } from '../orchestration/_helper';

/**
 * inventory StockMove reverse（物理冲销）浏览器层 E2E（plan 2026-07-11-1234-1 Phase 1）。
 *
 * 验证 `ErpInvStockMoveBiz__reverse(moveId)` 物理冲销动作经 GraphQL /graphql 的全栈可达性：
 *   - 成功模式：DONE 移动单 → reverse 构造反向移动单（inverseMoveType + swap source/dest +
 *     relatedBillType=REVERSAL + relatedBillCode=原移动单 code + originReturnedMoveId=原 moveId）
 *     → business-linked auto-DONE
 *   - 失败模式：非 DONE 移动单调 reverse 抛 ERR_REVERSE_NOT_DONE
 *
 * 权威实现（ErpInvStockMoveProcessor.reverse:114）：校验 docStatus=DONE → 构造 StockMoveRequest
 * （inverseMoveType swap INCOMING↔OUTGOING + source/dest warehouse/location 互换 +
 * originReturnedMoveId 挂链 + relatedBillType="REVERSAL" + relatedBillCode=原 code）→
 * generateMove（business-linked → 内部 doConfirm + doComplete → auto DONE）。
 *
 * 种子引用（master-data init-data）：material MAT-001 id=1 / uom id=1 / warehouse WH-RAW id=2 /
 * org id=2 / acctSchema ACCT-FIN-01 id=1 / currency CNY id=1。
 */

const MOVE_REQ_TYPE = 'i_app_erp_inv_biz_StockMoveRequest';
const MAT = 1;
const WH = 2;
const ORG = 2;
const UOM = 1;
const ACCT_SCHEMA = 1;
const CURRENCY = 1;

function incomingRequest(remark: string) {
  return input(MOVE_REQ_TYPE, {
    moveType: 'INCOMING',
    orgId: ORG,
    businessDate: '2026-07-11',
    destWarehouseId: WH,
    acctSchemaId: ACCT_SCHEMA,
    currencyId: CURRENCY,
    lines: [{ materialId: MAT, uoMId: UOM, quantity: 10, unitCost: 5, currencyId: CURRENCY }],
    remark,
  });
}

async function cleanupMove(page: import('@playwright/test').Page, move: { id?: any; code?: string } | null): Promise<void> {
  if (!move) return;
  if (move.code) await cleanupVoucherByBillCode(page, move.code);
  if (move.id != null) {
    await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('moveId', Number(move.id)));
    await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(move.id)));
    await deleteById(page, 'ErpInvStockMove', move.id);
  }
}

test.describe('inventory StockMove reverse (physical reversal)', () => {
  test('reverse: DONE INCOMING move → reversal OUTGOING move with REVERSAL linkage + direction swap', async ({ page }) => {
    await loginAndNavigate(page, '/ErpInvStockMove-main');

    // generateMove(independent INCOMING) → CONFIRMED
    const created = await callMutationOk(
      page, 'ErpInvStockMove', 'generateMove',
      { request: incomingRequest(`E2E-reverse-happy-${Date.now()}`) },
      'id code docStatus moveType sourceWarehouseId destWarehouseId',
    );
    expect(created.docStatus, 'independent generateMove stops at CONFIRMED').toBe('CONFIRMED');
    expect(created.moveType, 'original moveType=INCOMING').toBe('INCOMING');

    // complete → DONE
    await callMutationOk(page, 'ErpInvStockMove', 'complete', { moveId: created.id }, 'id docStatus');
    const doneState = await verifyState(page, 'ErpInvStockMove', created.id, 'docStatus');
    expect(doneState.docStatus, 'complete should transition CONFIRMED → DONE').toBe('DONE');

    // reverse → 构造反向移动单（business-linked: REVERSAL + code → auto DONE）
    const reversed = await callMutationOk(
      page, 'ErpInvStockMove', 'reverse', { moveId: created.id },
      'id code docStatus moveType relatedBillType relatedBillCode originReturnedMoveId sourceWarehouseId destWarehouseId',
    );

    // ---- 断言：冲销移动单创建 + relatedBill 挂链 + originReturnedMoveId ----
    expect(reversed.id, 'reversal move should have id').not.toBe(created.id);
    expect(reversed.docStatus, 'business-linked reversal auto-DONE').toBe('DONE');
    expect(reversed.relatedBillType, 'reversal relatedBillType=REVERSAL').toBe('REVERSAL');
    expect(reversed.relatedBillCode, 'reversal relatedBillCode=original code').toBe(created.code);
    expect(Number(reversed.originReturnedMoveId), 'reversal originReturnedMoveId=original id')
      .toBe(Number(created.id));

    // ---- 断言：冲销移动单方向取反 ----
    // inverseMoveType: INCOMING → OUTGOING
    expect(reversed.moveType, 'reversal moveType=OUTGOING (inverse of INCOMING)').toBe('OUTGOING');
    // source/dest swap: original destWarehouseId(2) → reversal sourceWarehouseId(2)
    expect(Number(reversed.sourceWarehouseId), 'reversal sourceWarehouseId=original destWarehouseId')
      .toBe(Number(created.destWarehouseId));
    // original sourceWarehouseId(null) → reversal destWarehouseId(null)
    expect(reversed.destWarehouseId ?? null, 'reversal destWarehouseId=original sourceWarehouseId(null)')
      .toBe(created.sourceWarehouseId ?? null);

    // ---- 清理：冲销移动单 + 原移动单 + 余额 ----
    // 净库存效应：INCOMING +10, OUTGOING -10 = 0；余额行可能以 quantity=0 残留，整行删除安全
    // （MAT-1/WH-2 在种子中无余额行）
    await cleanupMove(page, reversed);
    await cleanupMove(page, created);
    await deleteByFilter(page, 'ErpInvStockBalance', andFilter(eqFilter('materialId', MAT), eqFilter('warehouseId', WH)));
  });

  test('reverse guard: CONFIRMED (not DONE) move → ERR_REVERSE_NOT_DONE', async ({ page }) => {
    await loginAndNavigate(page, '/ErpInvStockMove-main');

    // generateMove(independent INCOMING) → CONFIRMED (NOT DONE)
    const created = await callMutationOk(
      page, 'ErpInvStockMove', 'generateMove',
      { request: incomingRequest(`E2E-reverse-guard-${Date.now()}`) },
      'id docStatus',
    );
    expect(created.docStatus, 'independent generateMove stops at CONFIRMED').toBe('CONFIRMED');

    // reverse on CONFIRMED → should fail with ERR_REVERSE_NOT_DONE
    const rej = await callMutation(page, 'ErpInvStockMove', 'reverse', { moveId: created.id }, 'id');
    expect(rej.errors, 'reverse on CONFIRMED should be rejected (ERR_REVERSE_NOT_DONE)').toBeTruthy();

    // 清理：CONFIRMED 不写流水/余额，仅清理移动单 + 行
    await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(created.id)));
    await deleteById(page, 'ErpInvStockMove', created.id);
  });
});
