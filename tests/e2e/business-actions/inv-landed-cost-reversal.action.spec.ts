import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  callQuery,
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
  findItems,
} from '../orchestration/_helper';

/**
 * inventory ErpInvLandedCost reverseApprove + LANDED_COST 凭证红冲浏览器层 E2E
 * （plan 2026-07-18-1745-2 Phase 4）。
 *
 * 验证 reverseApprove 经 GraphQL /graphql 的全栈可达 + LANDED_COST(490) 红冲凭证行数值断言：
 *   reverseApprove(id) @BizMutation → validateCanReverse（posted=true + APPROVED 守卫）
 *   → postingDispatcher.reverse（红冲 LANDED_COST 凭证）
 *   → CostAdjustmentService.reverseCostAdjust（反向应用 LANDED_COST_SUPPLEMENT 成本层）
 *   → doReverseApprove（posted=false + approveStatus=REJECTED + docStatus=CANCELLED）。
 *
 * 权威实现（ErpInvLandedCostProcessor.reverseApprove）：
 *   requireLandedCost → validateCanReverse（守卫 ERR_LANDED_COST_NOT_POSTED）
 *   → try { postingDispatcher.reverse } catch LOG.warn（GL 红冲失败吞异常保持幂等）
 *   → findCostAdjustForLandedCost → costAdjustmentService.reverseCostAdjust（反向应用成本层）
 *   → doReverseApprove（posted=false + approveStatus=REJECTED + docStatus=CANCELLED）
 *
 * LANDED_COST 红字凭证（IErpFinVoucherBiz.reverse → ErpFinPostingProcessor.reverseProcess）：
 *   原凭证 isReversed=true + 新建红字凭证 postingType=REVERSAL + 行同向取负（Dr 1401=-X / Cr 2202=-X）
 *   + 红字凭证与原凭证共用 billHeadCode（voucher_bill_r 回链反查区分 postingType）。
 *
 * 成本层反向应用（CostAdjustmentService.reverseCostAdjust → reverseLine）：
 *   MOVING_AVERAGE：balance.avgCost = line.oldUnitCost（回退至原始 unitCost）+ totalCost -= adjustAmount。
 *
 * 确定性值（与 inv-landed-cost.action.spec.ts 一致）：
 *   ReceiveLine: qty=10, unitPrice=10, amount=100
 *   LandedCost totalCostAmount=50, allocationMethod=BY_AMOUNT → 全部分摊到唯一 ReceiveLine
 *   LandedCostLine: FREIGHT amount=50, apPartnerId=SUP-001(3)
 *   → approve：Dr 1401 = 50 / Cr 2202 = 50；avgCost: 10 → 15（+50/10）
 *   → reverseApprove：Dr 1401 = -50 / Cr 2202 = -50；avgCost: 15 → 10
 *
 * 清理：LANDED_COST 凭证（原+红字 + cleanupVoucherByBillCode）+ CostAdjust/CostAdjustLine
 *   + StockLedger + StockBalance + INCOMING 备货 + LandedCost(行) + Receive(行) + 测试物料。
 */

const ORG = 2;
const WH = 2; // WH-RAW
const UOM = 1; // PCS
const CURRENCY = 1;
const ACCT_SCHEMA = 1; // ACCT-FIN-01
const SUPPLIER = 3; // SUP-001
const BDATE = '2026-07-10';
const MOVE_REQ_TYPE = 'i_app_erp_inv_biz_StockMoveRequest';

const SETUP_QTY = 10;
const SETUP_UNIT_COST = 10;
const LANDED_COST_AMOUNT = 50;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

test.describe('inventory ErpInvLandedCost reverseApprove + LANDED_COST voucher reversal assertion', () => {
  test('approve → reverseApprove triggers LANDED_COST voucher reversal + cost layer rollback + posted=false', async ({ page }) => {
    await loginAndNavigate(page, '/ErpInvLandedCost-main');

    const ts = Date.now();

    // 1. 测试专用物料（隔离避免污染 inventory dashboard totalValue 基线）
    const material = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code: uniq('E2E-LC-RV-MAT'), name: 'E2E Landed Cost Reversal Material',
        materialType: 'RAW_MATERIAL', uoMId: UOM, status: 'ACTIVE',
        costMethod: 'MOVING_AVERAGE', defaultWarehouseId: WH,
      },
      'id',
    );

    // 2. 前置备货：INCOMING 建库存余量，qty=10 × unitCost=10 → balance totalCost=100, avgCost=10
    const setupMove = await callMutationOk(
      page, 'ErpInvStockMove', 'generateMove',
      {
        request: input(MOVE_REQ_TYPE, {
          moveType: 'INCOMING', orgId: ORG, businessDate: BDATE,
          destWarehouseId: WH, acctSchemaId: ACCT_SCHEMA, currencyId: CURRENCY,
          lines: [{ materialId: material.id, uoMId: UOM, quantity: SETUP_QTY, unitCost: SETUP_UNIT_COST, currencyId: CURRENCY }],
          remark: `E2E-LC-RV-SEED-${ts}`,
        }),
      },
      'id code',
    );
    await callMutationOk(page, 'ErpInvStockMove', 'complete', { moveId: setupMove.id }, 'id docStatus');

    // 3. __save 预置 APPROVED Receive（docStatus=ACTIVE, approveStatus=APPROVED, 无业务副作用）
    const receive = await createViaSave(
      page, 'ErpPurReceive',
      {
        code: uniq('E2E-LC-RV-RCV'), orgId: ORG, supplierId: SUPPLIER, warehouseId: WH,
        businessDate: BDATE, currencyId: CURRENCY, exchangeRate: 1,
        docStatus: 'ACTIVE', approveStatus: 'APPROVED', receiveStatus: 'RECEIVED', posted: false,
      },
      'id',
    );
    const rcvLine = await createViaSave(
      page, 'ErpPurReceiveLine',
      {
        receiveId: receive.id, lineNo: 1, materialId: material.id, uoMId: UOM,
        quantity: SETUP_QTY, unitPrice: SETUP_UNIT_COST, amount: SETUP_QTY * SETUP_UNIT_COST, warehouseId: WH,
      },
      'id',
    );

    // 4. 建 DRAFT LandedCost + Line
    const landedCost = await createViaSave(
      page, 'ErpInvLandedCost',
      {
        code: uniq('E2E-LC-RV'), orgId: ORG, receiveId: receive.id, supplierId: SUPPLIER,
        currencyId: CURRENCY, exchangeRate: 1, totalCostAmount: LANDED_COST_AMOUNT,
        allocationMethod: 'BY_AMOUNT', docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED',
        posted: false, businessDate: BDATE,
      },
      'id code approveStatus docStatus posted',
    );

    const lcLine = await createViaSave(
      page, 'ErpInvLandedCostLine',
      {
        landedCostId: landedCost.id, lineNo: 1, costElement: 'FREIGHT',
        amount: LANDED_COST_AMOUNT, apPartnerId: SUPPLIER,
      },
      'id',
    );

    try {
      // ---- 前置：approve 产 LANDED_COST 凭证 + 成本层更新 ----
      const approved = await callMutationOk(
        page, 'ErpInvLandedCost', 'approve', { id: landedCost.id },
        'id approveStatus docStatus posted',
      );
      expect(approved.approveStatus, '前置 approve: approveStatus=APPROVED').toBe('APPROVED');
      expect(approved.docStatus, '前置 approve: docStatus=DONE').toBe('DONE');
      expect(approved.posted, '前置 approve: posted=true').toBe(true);

      const originalVoucherId = await findVoucherIdByBillCode(page, landedCost.code, 'NORMAL');
      expect(originalVoucherId, '前置：approve 应生成 LANDED_COST NORMAL 凭证').toBeTruthy();

      // 前置：成本层 avgCost 更新 10 → 15（+50/10）
      const balBefore = await findFirst<any>(
        page, 'ErpInvStockBalance',
        andFilter(eqFilter('materialId', Number(material.id)), eqFilter('warehouseId', WH)),
        'avgCost totalCost',
      );
      expect(Number(balBefore.avgCost), '前置 approve 后 avgCost=15').toBe(SETUP_UNIT_COST + LANDED_COST_AMOUNT / SETUP_QTY);

      // ---- 执行 reverseApprove ----
      const reversed = await callMutationOk(
        page, 'ErpInvLandedCost', 'reverseApprove', { id: landedCost.id },
        'id approveStatus docStatus posted',
      );
      expect(reversed.approveStatus, 'reverseApprove: approveStatus=REJECTED').toBe('REJECTED');
      expect(reversed.docStatus, 'reverseApprove: docStatus=CANCELLED').toBe('CANCELLED');
      expect(reversed.posted, 'reverseApprove: posted=false').toBe(false);

      // __get 权威确认状态翻转
      const finalLc = await verifyState(page, 'ErpInvLandedCost', landedCost.id, 'approveStatus docStatus posted');
      expect(finalLc.approveStatus, '__get: approveStatus=REJECTED').toBe('REJECTED');
      expect(finalLc.docStatus, '__get: docStatus=CANCELLED').toBe('CANCELLED');
      expect(finalLc.posted, '__get: posted=false').toBe(false);

      // ---- 原 LANDED_COST 凭证 isReversed=true ----
      const originalAfter = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', Number(originalVoucherId)), 'id isReversed postingType',
      );
      expect(originalAfter?.isReversed, '原 LANDED_COST 凭证应被标记 isReversed=true').toBe(true);

      // ---- 红字凭证存在 + 行同向取负断言 ----
      const reversalVoucherId = await findVoucherIdByBillCode(page, landedCost.code, 'REVERSAL');
      expect(reversalVoucherId, '应存在 LANDED_COST 红字冲销凭证').toBeTruthy();

      await assertVoucherLines(page, reversalVoucherId, [
        // 红字凭证行：dcDirection 不变，金额取负
        { subjectCode: '1401', dcDirection: 'DEBIT', debitAmount: -LANDED_COST_AMOUNT, creditAmount: 0 },
        { subjectCode: '2202', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -LANDED_COST_AMOUNT },
      ]);

      // ---- 成本层反向应用：avgCost 回退至原始 10 ----
      const balAfter = await findFirst<any>(
        page, 'ErpInvStockBalance',
        andFilter(eqFilter('materialId', Number(material.id)), eqFilter('warehouseId', WH)),
        'avgCost totalCost',
      );
      expect(Number(balAfter.avgCost), 'reverseApprove 后 avgCost 回退至原始 10').toBe(SETUP_UNIT_COST);
    } finally {
      // ---- 清理（依赖反向顺序）----
      // 1. LANDED_COST 凭证（原+红字共用 billHeadCode=landedCost.code 无后缀，cleanupVoucherByBillCode 全量清理）
      await cleanupVoucherByBillCode(page, landedCost.code);
      // 2. CostAdjustLine + CostAdjust（按 materialId 反查 adjustId）
      const caLines = await findItems<any>(page, 'ErpInvCostAdjustLine', eqFilter('materialId', Number(material.id)), 'id adjustId');
      const adjustIds = new Set(caLines.map((l) => l.adjustId));
      for (const l of caLines) await deleteById(page, 'ErpInvCostAdjustLine', l.id);
      for (const aid of adjustIds) await deleteById(page, 'ErpInvCostAdjust', aid);
      // 3. StockLedger 按 materialId（含 INCOMING 备货 + CostAdjust 写入的 quantity=0 行）
      await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('materialId', Number(material.id)));
      // 4. StockBalance 按 materialId+warehouseId
      await deleteByFilter(page, 'ErpInvStockBalance', andFilter(eqFilter('materialId', Number(material.id)), eqFilter('warehouseId', WH)));
      // 5. INCOMING 备货移动（lines + move）
      await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(setupMove.id)));
      await deleteById(page, 'ErpInvStockMove', setupMove.id);
      // 6. LandedCostLine + LandedCost
      await deleteById(page, 'ErpInvLandedCostLine', lcLine.id);
      await deleteById(page, 'ErpInvLandedCost', landedCost.id);
      // 7. ReceiveLine + Receive
      await deleteById(page, 'ErpPurReceiveLine', rcvLine.id);
      await deleteById(page, 'ErpPurReceive', receive.id);
      // 8. 测试物料
      await deleteById(page, 'ErpMdMaterial', material.id);
    }
  });

  test('reverseApprove on unposted LandedCost → ERR_LANDED_COST_NOT_POSTED guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpInvLandedCost-main');

    // 建 DRAFT LandedCost（未 approve → posted=false）
    const receive = await createViaSave(
      page, 'ErpPurReceive',
      {
        code: uniq('E2E-LC-REJ-RCV'), orgId: ORG, supplierId: SUPPLIER, warehouseId: WH,
        businessDate: BDATE, currencyId: CURRENCY, exchangeRate: 1,
        docStatus: 'ACTIVE', approveStatus: 'APPROVED', receiveStatus: 'RECEIVED', posted: false,
      },
      'id',
    );
    const landedCost = await createViaSave(
      page, 'ErpInvLandedCost',
      {
        code: uniq('E2E-LC-REJ'), orgId: ORG, receiveId: receive.id, supplierId: SUPPLIER,
        currencyId: CURRENCY, exchangeRate: 1, totalCostAmount: LANDED_COST_AMOUNT,
        allocationMethod: 'BY_AMOUNT', docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED',
        posted: false, businessDate: BDATE,
      },
      'id code',
    );

    try {
      // 未 approve 直接 reverseApprove → 守卫拒绝（data=null + errors 非 null）
      const resp = await callMutation(page, 'ErpInvLandedCost', 'reverseApprove', { id: landedCost.id }, 'id');
      expect(resp.errors, '未过账到岸成本单 reverseApprove 应被守卫拒绝（errors 非空）').toBeTruthy();
      expect(resp.data, '未过账到岸成本单 reverseApprove 应被守卫拒绝（data=null）').toBeNull();
      const errMsg = JSON.stringify(resp.errors ?? '');
      expect(errMsg.includes('未过账') || errMsg.includes('不可红冲') || errMsg.includes('not-posted'),
        `errors 应含语义 token，实际: ${errMsg}`).toBe(true);

      // 状态不变（守卫前置）
      const unchanged = await verifyState(page, 'ErpInvLandedCost', landedCost.id, 'approveStatus docStatus posted');
      expect(unchanged.approveStatus, '守卫拒绝后 approveStatus 保持 UNSUBMITTED').toBe('UNSUBMITTED');
      expect(unchanged.docStatus, '守卫拒绝后 docStatus 保持 DRAFT').toBe('DRAFT');
      expect(unchanged.posted, '守卫拒绝后 posted 保持 false').toBe(false);
    } finally {
      await deleteById(page, 'ErpInvLandedCost', landedCost.id);
      await deleteById(page, 'ErpPurReceive', receive.id);
    }
  });
});
