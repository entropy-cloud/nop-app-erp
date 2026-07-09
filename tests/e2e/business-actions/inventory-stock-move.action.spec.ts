import { test, expect, loginAndNavigate, input, createViaSave, callMutationOk, callMutation, verifyState, findPageTotal, eqFilter, andFilter, deleteByFilter, deleteById } from './_helper';

/**
 * inventory StockMove 业务动作浏览器层 E2E（plan 2026-07-09-0814-2 Phase 2）。
 *
 * 验证自定义 @BizMutation（generateMove/complete/cancel）经 GraphQL /graphql 的全栈可达性 + 状态机迁移 +
 * 过账型下游产物（不可变流水 ErpInvStockLedger）。
 *
 * 实现说明（经核实 ErpInvStockMoveProcessor）：generateMove 内部经 doConfirm 自动推进 DRAFT→CONFIRMED，
 * 独立创建（无 relatedBillType）停在 CONFIRMED（库管员二次确认执行 DONE）。故可观测状态链为：
 *   generateMove(独立) → CONFIRMED → complete → DONE + 流水写入
 * 「confirm」为 generateMove 内部过渡步骤（BizModel 注释确认无独立 DRAFT 创建入口），不再单列直测。
 *
 * 种子引用（master-data init-data）：material MAT-001 id=1 / uom id=1 / warehouse WH-RAW id=2 /
 * org id=2 / acctSchema ACCT-FIN-01 id=1 / currency CNY id=1。
 */

const MOVE_REQ_TYPE = 'i_app_erp_inv_biz_StockMoveRequest';

function incomingRequest(unique: string) {
  return input(MOVE_REQ_TYPE, {
    moveType: 'INCOMING',
    orgId: 2,
    businessDate: '2026-07-09',
    destWarehouseId: 2,
    acctSchemaId: 1,
    currencyId: 1,
    lines: [{ materialId: 1, uoMId: 1, quantity: 10, unitCost: 5, currencyId: 1 }],
    remark: `E2E-business-action-${unique}`,
  });
}

test.describe('inventory StockMove business action lifecycle', () => {
  test('generateMove(independent) → CONFIRMED → complete → DONE + ledger written', async ({ page }) => {
    await loginAndNavigate(page, '/ErpInvStockMove-main');

    // generateMove（独立，无 relatedBillType）→ 内部 doConfirm → CONFIRMED
    const created = await callMutationOk(
      page, 'ErpInvStockMove', 'generateMove',
      { request: incomingRequest('happy') },
      'id docStatus posted',
    );
    expect(created.id, 'generateMove should return move id').toBeTruthy();
    expect(created.docStatus, 'independent generateMove stops at CONFIRMED').toBe('CONFIRMED');

    // confirm 为 generateMove 内部过渡步骤，独立 confirm 需 DRAFT 态——这里已 CONFIRMED，
    // 再调 confirm 应拒绝（验证状态机守卫：非法迁移经 GraphQL 返回 errors 而非抛异常）。
    const rej = await callMutation(page, 'ErpInvStockMove', 'confirm', { moveId: created.id }, 'id');
    expect(rej.errors, 'confirm on CONFIRMED (not DRAFT) should be rejected as illegal transition').toBeTruthy();

    // complete → DONE + 过账派发（posted 反映跨域财务过账，可能优雅降级为 false；流水为同事务可靠产物）
    const done = await callMutationOk(
      page, 'ErpInvStockMove', 'complete', { moveId: created.id }, 'id docStatus posted',
    );
    expect(done.docStatus, 'complete should transition CONFIRMED → DONE').toBe('DONE');

    // verifyState 经 __get 断言终态翻转（独立于 mutation 返回值，权威查库）
    const verified = await verifyState(page, 'ErpInvStockMove', created.id, 'id docStatus posted');
    expect(verified.docStatus, '__get should confirm DONE').toBe('DONE');
    expect(typeof verified.posted, 'posted should be a boolean (cross-domain posting flag)').toBe('boolean');

    // 过账产物存在性：不可变流水 ErpInvStockLedger 按 moveId 非空（complete 同事务写入，可靠）
    const ledgerTotal = await findPageTotal(page, 'ErpInvStockLedger', eqFilter('moveId', Number(created.id)));
    expect(ledgerTotal, 'StockMove DONE should write at least one immutable ledger line').toBeGreaterThan(0);

    // 清理：complete 写入的不可逆下游产物（流水/余额）会污染共享 DB 的下游数值断言
    // （inventory dashboard KPI 聚合 stock_balance/ledger 的 totalValue/incomingQty）。
    // MAT-1/WH-2 组合在种子中无余额（种子为 MAT-3/WH-2 + MAT-1/WH-1），故按此组合删除安全。
    await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('moveId', Number(created.id)));
    await deleteByFilter(page, 'ErpInvStockBalance', andFilter(eqFilter('materialId', 1), eqFilter('warehouseId', 2)));
    await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(created.id)));
    await deleteById(page, 'ErpInvStockMove', created.id);
  });

  test('cancel path: generateMove(independent) → CONFIRMED → cancel → CANCELLED', async ({ page }) => {
    await loginAndNavigate(page, '/ErpInvStockMove-main');

    const created = await callMutationOk(
      page, 'ErpInvStockMove', 'generateMove',
      { request: incomingRequest('cancel') },
      'id docStatus',
    );
    expect(created.docStatus, 'independent generateMove stops at CONFIRMED').toBe('CONFIRMED');

    const cancelled = await callMutationOk(
      page, 'ErpInvStockMove', 'cancel', { moveId: created.id }, 'id docStatus',
    );
    expect(cancelled.docStatus, 'cancel should transition CONFIRMED → CANCELLED').toBe('CANCELLED');

    const verified = await verifyState(page, 'ErpInvStockMove', created.id, 'docStatus');
    expect(verified.docStatus, '__get should confirm CANCELLED').toBe('CANCELLED');

    // 清理：cancel 路径不写流水/余额（INCOMING confirm 不预留），仅清理移动单 + 行
    await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(created.id)));
    await deleteById(page, 'ErpInvStockMove', created.id);
  });
});
