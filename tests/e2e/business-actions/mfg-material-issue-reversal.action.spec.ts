import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  eqFilter,
  andFilter,
  deleteByFilter,
  deleteById,
  findFirst,
  input,
} from './_helper';
import {
  cleanupVoucherByBillCode,
  findVoucherIdByBillCode,
  assertVoucherLines,
} from '../orchestration/_helper';

/**
 * manufacturing ErpMfgMaterialIssue reverseConfirm + MANUFACTURING_ISSUE 凭证红冲浏览器层 E2E
 * （plan 2026-07-18-1745-2 Phase 4）。
 *
 * 验证 reverseConfirm 经 GraphQL /graphql 的全栈可达 + MANUFACTURING_ISSUE 凭证红冲凭证行数值断言：
 *   reverseConfirm(issueId) @BizMutation → validateCanReverse（posted=true + DONE 守卫）
 *   → issuePostingDispatcher.reverse（红冲 MANUFACTURING_ISSUE 凭证）
 *   → stockMoveBiz.reverse（反向 OUTGOING 移动单，REVERSAL relatedBillType）
 *   → doReverseConfirm（posted=false + docStatus=CANCELLED）。
 *
 * 权威实现（ErpMfgMaterialIssueBizModel.reverseConfirm）：
 *   requireEntity → validateCanReverse（守卫 ERR_MATERIAL_ISSUE_NOT_POSTED）
 *   → try { issuePostingDispatcher.reverse } catch LOG.warn（GL 红冲失败吞异常保持幂等）
 *   → findIssueMove → try { stockMoveBiz.reverse(moveId) } catch LOG.warn（库存反向失败吞异常保持幂等）
 *   → reload issue → doReverseConfirm（posted=false + docStatus=CANCELLED）
 *
 * MANUFACTURING_ISSUE 红字凭证（IErpFinVoucherBiz.reverse → ErpFinPostingProcessor.reverseProcess）：
 *   原凭证 isReversed=true + 新建红字凭证 postingType=REVERSAL + 行同向取负（Dr 1411=-X / Cr 1401=-X）
 *   + 红字凭证与原凭证共用 billHeadCode（voucher_bill_r 回链反查区分 postingType）。
 *
 * 库存反向移动单（IErpInvStockMoveBiz.reverse → ErpInvStockMoveProcessor.reverse）：
 *   DONE 移动单 → 生成 REVERSAL 反向冲销移动单（relatedBillType=REVERSAL + relatedBillCode=原移动单 code）。
 *
 * 确定性值：
 *   备货 INCOMING: qty=20, unitCost=5
 *   MaterialIssueLine: requiredQuantity=issuedQuantity=10
 *   → confirm 出库 10 × avgCost 5 = 50 → Dr 1411 WIP = 50 / Cr 1401 存货 = 50
 *   → reverseConfirm：Dr 1411 = -50 / Cr 1401 = -50
 *
 * 清理：MANUFACTURING_ISSUE 凭证（原+红字 + cleanupVoucherByBillCode 按 issue.code + "-MI"）
 *   + OUTGOING 移动 + REVERSAL 反向移动 + StockLedger + StockBalance
 *   + INCOMING 备货移动 + MaterialIssueLine + MaterialIssue + WorkOrderLine + WorkOrder
 *   + BOMLine + BOM + 测试物料。
 */

const ORG = 2;
const WH = 2; // WH-RAW
const UOM = 1; // PCS
const CURRENCY = 1;
const ACCT_SCHEMA = 1;
const BDATE = '2026-07-10';
const MOVE_REQ_TYPE = 'i_app_erp_inv_biz_StockMoveRequest';

const SETUP_QTY = 20;
const SETUP_UNIT_COST = 5;
const ISSUE_QTY = 10;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

test.describe('manufacturing ErpMfgMaterialIssue reverseConfirm + MANUFACTURING_ISSUE voucher reversal assertion', () => {
  test('confirm → reverseConfirm triggers MI voucher reversal + reversal stock move + posted=false', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgMaterialIssue-main');

    const ts = Date.now();

    // 1. 测试专用组件物料 + 成品物料（隔离避免污染 inventory dashboard totalValue 基线）
    const componentMat = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code: uniq('E2E-MFG-MI-RV-MAT'), name: 'E2E Mfg Issue Reversal Component',
        materialType: 'RAW_MATERIAL', uoMId: UOM, status: 'ACTIVE',
        costMethod: 'MOVING_AVERAGE', defaultWarehouseId: WH,
      },
      'id',
    );
    const productMat = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code: uniq('E2E-MFG-MI-RV-FG'), name: 'E2E Mfg Issue Reversal Finished Good',
        materialType: 'FINISHED_PRODUCT', uoMId: UOM, status: 'ACTIVE',
        costMethod: 'MOVING_AVERAGE', defaultWarehouseId: WH,
      },
      'id',
    );

    // 2. 前置备货：INCOMING 建库存余量
    const setupMove = await callMutationOk(
      page, 'ErpInvStockMove', 'generateMove',
      {
        request: input(MOVE_REQ_TYPE, {
          moveType: 'INCOMING', orgId: ORG, businessDate: BDATE,
          destWarehouseId: WH, acctSchemaId: ACCT_SCHEMA, currencyId: CURRENCY,
          lines: [{ materialId: componentMat.id, uoMId: UOM, quantity: SETUP_QTY, unitCost: SETUP_UNIT_COST, currencyId: CURRENCY }],
          remark: `E2E-MFG-MI-RV-SEED-${ts}`,
        }),
      },
      'id code',
    );
    await callMutationOk(page, 'ErpInvStockMove', 'complete', { moveId: setupMove.id }, 'id docStatus');

    // 3. BOM + 行：成品 + 组件物料（用量 1/单位）
    const bom = await createViaSave(
      page, 'ErpMfgBom',
      { code: uniq('E2E-MFG-MI-RV-BOM'), productId: Number(productMat.id), bomType: 'NORMAL', qty: 1, isActive: true },
      'id',
    );
    const bomLine = await createViaSave(
      page, 'ErpMfgBomLine',
      { bomId: bom.id, lineNo: 1, materialId: Number(componentMat.id), uoMId: UOM, quantity: 1, warehouseId: WH },
      'id',
    );

    // 4. WorkOrder + INPUT 行：成品 + bomId + plannedQty=10
    const wo = await createViaSave(
      page, 'ErpMfgWorkOrder',
      {
        code: uniq('E2E-MFG-MI-RV-WO'), orgId: ORG, bomId: bom.id, productId: Number(productMat.id),
        plannedQuantity: 10, businessDate: BDATE,
        currencyId: CURRENCY, exchangeRate: 1,
        docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED',
      },
      'id',
    );
    const woInputLine = await createViaSave(
      page, 'ErpMfgWorkOrderLine',
      {
        workOrderId: wo.id, lineNo: 1, lineType: 'INPUT',
        materialId: Number(componentMat.id), uoMId: UOM, plannedQuantity: ISSUE_QTY,
        sourceWarehouseId: WH,
      },
      'id',
    );

    // 5. 建 DRAFT MaterialIssue + Line
    const issue = await createViaSave(
      page, 'ErpMfgMaterialIssue',
      {
        code: uniq('E2E-MFG-MI-RV'), orgId: ORG, workOrderId: wo.id, warehouseId: WH,
        businessDate: BDATE, currencyId: CURRENCY, exchangeRate: 1,
        docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED',
      },
      'id code',
    );
    const issueLine = await createViaSave(
      page, 'ErpMfgMaterialIssueLine',
      {
        issueId: issue.id, lineNo: 1, materialId: Number(componentMat.id), uoMId: UOM,
        workOrderLineId: woInputLine.id,
        requiredQuantity: ISSUE_QTY, issuedQuantity: ISSUE_QTY,
      },
      'id',
    );

    try {
      // ---- 前置：confirm 产 MANUFACTURING_ISSUE 凭证 + OUTGOING 移动 + posted=true ----
      const confirmed = await callMutationOk(
        page, 'ErpMfgMaterialIssue', 'confirm', { issueId: issue.id },
        'id docStatus posted',
      );
      expect(confirmed.docStatus, '前置 confirm: docStatus=DONE').toBe('DONE');
      expect(confirmed.posted, '前置 confirm: posted=true').toBe(true);

      const originalVoucherId = await findVoucherIdByBillCode(page, issue.code + '-MI', 'NORMAL');
      expect(originalVoucherId, '前置：confirm 应生成 MANUFACTURING_ISSUE NORMAL 凭证').toBeTruthy();

      const originalMove = await findFirst<any>(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_MFG_ISSUE'), eqFilter('relatedBillCode', issue.code)),
        'id code docStatus',
      );
      expect(originalMove, '前置：confirm 应生成 OUTGOING 移动单').toBeTruthy();
      expect(originalMove.docStatus, '前置 OUTGOING: docStatus=DONE').toBe('DONE');

      // ---- 执行 reverseConfirm ----
      const reversed = await callMutationOk(
        page, 'ErpMfgMaterialIssue', 'reverseConfirm', { issueId: issue.id },
        'id docStatus posted',
      );
      expect(reversed.docStatus, 'reverseConfirm: docStatus=CANCELLED').toBe('CANCELLED');
      expect(reversed.posted, 'reverseConfirm: posted=false').toBe(false);

      // __get 权威确认状态翻转
      const finalIssue = await verifyState(page, 'ErpMfgMaterialIssue', issue.id, 'docStatus posted');
      expect(finalIssue.docStatus, '__get: docStatus=CANCELLED').toBe('CANCELLED');
      expect(finalIssue.posted, '__get: posted=false').toBe(false);

      // ---- 原 MANUFACTURING_ISSUE 凭证 isReversed=true ----
      const originalAfter = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', Number(originalVoucherId)), 'id isReversed postingType',
      );
      expect(originalAfter?.isReversed, '原 MANUFACTURING_ISSUE 凭证应被标记 isReversed=true').toBe(true);

      // ---- 红字凭证存在 + 行同向取负断言 ----
      const reversalVoucherId = await findVoucherIdByBillCode(page, issue.code + '-MI', 'REVERSAL');
      expect(reversalVoucherId, '应存在 MANUFACTURING_ISSUE 红字冲销凭证').toBeTruthy();

      const voucherAmount = ISSUE_QTY * SETUP_UNIT_COST; // 50
      await assertVoucherLines(page, reversalVoucherId, [
        // 红字凭证行：dcDirection 不变，金额取负
        { subjectCode: '1411', dcDirection: 'DEBIT', debitAmount: -voucherAmount, creditAmount: 0 },
        { subjectCode: '1401', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -voucherAmount },
      ]);

      // ---- 反向 OUTGOING 移动单（REVERSAL 移动单）----
      const reversalMove = await findFirst<any>(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'REVERSAL'), eqFilter('relatedBillCode', originalMove.code)),
        'id code docStatus relatedBillType relatedBillCode',
      );
      expect(reversalMove, '应存在 REVERSAL 反向冲销移动单').toBeTruthy();
      expect(reversalMove.relatedBillType, 'REVERSAL 移动单 relatedBillType=REVERSAL').toBe('REVERSAL');
      expect(reversalMove.relatedBillCode, 'REVERSAL 移动单 relatedBillCode 指向原移动单 code')
        .toBe(originalMove.code);
    } finally {
      // ---- 清理（依赖反向顺序）----
      // 1. MANUFACTURING_ISSUE 凭证（原+红字共用 billHeadCode=issue.code + "-MI"）
      await cleanupVoucherByBillCode(page, issue.code + '-MI');
      // 2. OUTGOING 移动 + REVERSAL 反向移动（清理 REVERSAL 移动先）
      const issueMove = await findFirst<any>(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_MFG_ISSUE'), eqFilter('relatedBillCode', issue.code)),
        'id code',
      );
      if (issueMove) {
        const reversalMove = await findFirst<any>(
          page, 'ErpInvStockMove',
          andFilter(eqFilter('relatedBillType', 'REVERSAL'), eqFilter('relatedBillCode', issueMove.code)),
          'id',
        );
        if (reversalMove) {
          await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('moveId', Number(reversalMove.id)));
          await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(reversalMove.id)));
          await deleteById(page, 'ErpInvStockMove', reversalMove.id);
        }
        await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('moveId', Number(issueMove.id)));
        await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(issueMove.id)));
        await deleteById(page, 'ErpInvStockMove', issueMove.id);
      }
      // 3. MaterialIssueLine + MaterialIssue
      await deleteById(page, 'ErpMfgMaterialIssueLine', issueLine.id);
      await deleteById(page, 'ErpMfgMaterialIssue', issue.id);
      // 4. WorkOrderLine + WorkOrder
      await deleteById(page, 'ErpMfgWorkOrderLine', woInputLine.id);
      await deleteById(page, 'ErpMfgWorkOrder', wo.id);
      // 5. BOMLine + BOM
      await deleteById(page, 'ErpMfgBomLine', bomLine.id);
      await deleteById(page, 'ErpMfgBom', bom.id);
      // 6. StockLedger + StockBalance 按物料
      await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('materialId', Number(componentMat.id)));
      await deleteByFilter(page, 'ErpInvStockBalance', andFilter(eqFilter('materialId', Number(componentMat.id)), eqFilter('warehouseId', WH)));
      // 7. INCOMING 备货移动
      await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(setupMove.id)));
      await deleteById(page, 'ErpInvStockMove', setupMove.id);
      // 8. 测试物料
      await deleteById(page, 'ErpMdMaterial', componentMat.id);
      await deleteById(page, 'ErpMdMaterial', productMat.id);
    }
  });

  test('reverseConfirm on unposted issue → ERR_MATERIAL_ISSUE_NOT_POSTED guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgMaterialIssue-main');

    // 建 DRAFT MaterialIssue（未 confirm → posted=false），需引用一个 WorkOrder
    const productMat = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code: uniq('E2E-MFG-MI-REJ-FG'), name: 'E2E Mfg Issue Reversal Reject FG',
        materialType: 'FINISHED_PRODUCT', uoMId: UOM, status: 'ACTIVE',
        costMethod: 'MOVING_AVERAGE', defaultWarehouseId: WH,
      },
      'id',
    );
    const wo = await createViaSave(
      page, 'ErpMfgWorkOrder',
      {
        code: uniq('E2E-MFG-MI-REJ-WO'), orgId: ORG, productId: Number(productMat.id),
        plannedQuantity: 1, businessDate: BDATE,
        currencyId: CURRENCY, exchangeRate: 1,
        docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED',
      },
      'id',
    );
    const issue = await createViaSave(
      page, 'ErpMfgMaterialIssue',
      {
        code: uniq('E2E-MFG-MI-REJ'), orgId: ORG, workOrderId: wo.id, warehouseId: WH,
        businessDate: BDATE, currencyId: CURRENCY, exchangeRate: 1,
        docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED',
      },
      'id code',
    );

    try {
      // 未 confirm 直接 reverseConfirm → 守卫拒绝
      const resp = await callMutation(page, 'ErpMfgMaterialIssue', 'reverseConfirm', { issueId: issue.id }, 'id');
      expect(resp.errors, '未过账领料单 reverseConfirm 应被守卫拒绝（errors 非空）').toBeTruthy();
      expect(resp.data, '未过账领料单 reverseConfirm 应被守卫拒绝（data=null）').toBeNull();
      const errMsg = JSON.stringify(resp.errors ?? '');
      expect(errMsg.includes('未过账') || errMsg.includes('不可红冲') || errMsg.includes('not-posted'),
        `errors 应含语义 token，实际: ${errMsg}`).toBe(true);

      // 状态不变（守卫前置）
      const unchanged = await verifyState(page, 'ErpMfgMaterialIssue', issue.id, 'docStatus posted');
      expect(unchanged.docStatus, '守卫拒绝后 docStatus 保持 DRAFT').toBe('DRAFT');
      expect(unchanged.posted, '守卫拒绝后 posted 保持 false').toBe(false);
    } finally {
      await deleteById(page, 'ErpMfgMaterialIssue', issue.id);
      await deleteById(page, 'ErpMfgWorkOrder', wo.id);
      await deleteById(page, 'ErpMdMaterial', productMat.id);
    }
  });
});
