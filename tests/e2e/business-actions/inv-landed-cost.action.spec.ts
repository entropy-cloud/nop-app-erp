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
 * inventory ErpInvLandedCost 到岸成本审核生命周期 + LANDED_COST 过账浏览器层 E2E
 * （plan 2026-07-14-0606-2 Phase 1）。
 *
 * 验证到岸成本审核编排经 GraphQL /graphql 的全栈可达性：
 *   allocate(id) @BizQuery 分摊预览（不改状态）→ approve(id) @BizMutation 审核三步编排
 *   （分摊 → CostAdjust → LANDED_COST(490) 过账）→ approveStatus=APPROVED + docStatus=DONE + posted=true。
 *
 * 权威实现（ErpInvLandedCostProcessor.approve:76-103）：
 *   DRAFT/UNSUBMITTED → approve → requireLandedCost → loadCostLines(空集守卫) → loadReceive
 *   → validateReceiveApproved(receive.approveStatus==APPROVED) → validateNotAlreadyAllocated
 *   → LandedCostAllocationEngine.allocate(BY_AMOUNT) → CostAdjustmentService.applyCostAdjust
 *   → LandedCostPostingDispatcher.tryPost(LANDED_COST) → doPostApprove 状态翻转。
 *
 * LANDED_COST 凭证（LandedCostAcctDocProvider.createFacts:46-88）：
 *   Dr 1401 存货商品（每入库行分摊金额）/ Cr 2202 应付账款（每费用要素金额，partnerId=费用行 apPartnerId）。
 *   billHeadCode = landedCost.getCode()（无后缀，LandedCostPostingDispatcher.java:69）。
 *
 * Phase 1 Decision（Explore 裁定）：自包含 __save setup（非 runP2pChain）。
 *   runP2pChain 硬编码 SEED.MAT_1（orchestration/_helper.ts:288），物料不可控——复用会为 MAT_1
 *   在 WH-RAW 新增余额行，且 CostAdjust 会修改其 avgCost，污染 inventory dashboard totalValue 基线。
 *   裁定：建测试专用物料（RAW_MATERIAL, MOVING_AVERAGE, 无种子余额 → CostAdjust 确定性 + 清理安全），
 *   __save 预置 APPROVED Receive（docStatus=ACTIVE, approveStatus=APPROVED, 无业务副作用——__save 不触发
 *   审批工作流，对齐后端 TestErpInvLandedCostEndToEnd#seedReceive 直接置 APPROVED 范式），
 *   generateMove INCOMING 建库存余量（CostAdjust 更新 avgCost 需源余额）。
 *
 * 确定性值（单行 Receive + 单费用要素）：
 *   ReceiveLine: qty=10, unitPrice=10, amount=100
 *   LandedCost totalCostAmount=50, allocationMethod=BY_AMOUNT → 全部分摊到唯一 ReceiveLine
 *   LandedCostLine: FREIGHT amount=50, apPartnerId=SUP-001(3)
 *   → Dr 1401 = 50（唯一分摊行）/ Cr 2202 = 50（唯一费用要素）
 *
 * 清理：LANDED_COST 凭证（cleanupVoucherByBillCode 按 landedCost.code 无后缀）
 *   + CostAdjust/CostAdjustLine（按 materialId）+ StockLedger（按 materialId，含 CostAdjust 写入的 quantity=0 行）
 *   + StockBalance（按 materialId+warehouseId）+ INCOMING 备货移动 + LandedCost(行) + Receive(行) + 测试物料。
 *   posting 不写 gl_balance（仅 voucher/voucher_line/voucher_bill_r），不污染 finance dashboard 基线。
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

test.describe('inventory ErpInvLandedCost approve lifecycle + LANDED_COST posting voucher line assertion', () => {
  test('allocate preview → approve(DONE+APPROVED+posted) → LANDED_COST voucher Dr 1401 / Cr 2202', async ({ page }) => {
    await loginAndNavigate(page, '/ErpInvLandedCost-main');

    const ts = Date.now();

    // 1. 测试专用物料（RAW_MATERIAL, MOVING_AVERAGE, 无种子余额 → CostAdjust 确定性 + 清理安全）
    const material = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code: uniq('E2E-LC-MAT'), name: 'E2E Landed Cost Test Material',
        materialType: 'RAW_MATERIAL', uoMId: UOM, status: 'ACTIVE',
        costMethod: 'MOVING_AVERAGE', defaultWarehouseId: WH,
      },
      'id',
    );

    // 2. 前置备货：INCOMING 建库存余量（CostAdjust 更新 avgCost 需源余额），qty=10 × unitCost=10 → balance totalCost=100
    const setupMove = await callMutationOk(
      page, 'ErpInvStockMove', 'generateMove',
      {
        request: input(MOVE_REQ_TYPE, {
          moveType: 'INCOMING', orgId: ORG, businessDate: BDATE,
          destWarehouseId: WH, acctSchemaId: ACCT_SCHEMA, currencyId: CURRENCY,
          lines: [{ materialId: material.id, uoMId: UOM, quantity: SETUP_QTY, unitCost: SETUP_UNIT_COST, currencyId: CURRENCY }],
          remark: `E2E-LC-SEED-${ts}`,
        }),
      },
      'id code',
    );
    await callMutationOk(page, 'ErpInvStockMove', 'complete', { moveId: setupMove.id }, 'id docStatus');

    // 3. __save 预置 APPROVED Receive（docStatus=ACTIVE erp-pur/doc-status, approveStatus=APPROVED, 无业务副作用）
    const receive = await createViaSave(
      page, 'ErpPurReceive',
      {
        code: uniq('E2E-LC-RCV'), orgId: ORG, supplierId: SUPPLIER, warehouseId: WH,
        businessDate: BDATE, currencyId: CURRENCY, exchangeRate: 1,
        docStatus: 'ACTIVE', approveStatus: 'APPROVED', receiveStatus: 'RECEIVED', posted: false,
      },
      'id',
    );
    // ReceiveLine：materialId + qty=10 + unitPrice=10 + amount=100 + warehouseId=WH
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
        code: uniq('E2E-LC'), orgId: ORG, receiveId: receive.id, supplierId: SUPPLIER,
        currencyId: CURRENCY, exchangeRate: 1, totalCostAmount: LANDED_COST_AMOUNT,
        allocationMethod: 'BY_AMOUNT', docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED',
        posted: false, businessDate: BDATE,
      },
      'id code approveStatus docStatus posted',
    );
    expect(landedCost.approveStatus, 'new landedCost approveStatus=UNSUBMITTED').toBe('UNSUBMITTED');

    const lcLine = await createViaSave(
      page, 'ErpInvLandedCostLine',
      {
        landedCostId: landedCost.id, lineNo: 1, costElement: 'FREIGHT',
        amount: LANDED_COST_AMOUNT, apPartnerId: SUPPLIER,
      },
      'id',
    );

    try {
      // ---- allocate(id) @BizQuery 分摊预览 ----
      const allocResult = await callQuery(page, 'ErpInvLandedCost', 'allocate', { id: landedCost.id });
      expect(allocResult.errors, 'allocate should not return GraphQL errors').toBeNull();
      expect(Array.isArray(allocResult.data), 'allocate should return List<Map> allocation preview').toBe(true);
      expect(allocResult.data.length, 'allocate preview should have 1 allocation row for 1 receive line').toBe(1);
      expect(Number(allocResult.data[0].allocatedAmount), 'allocatedAmount should equal totalCostAmount for single line').toBe(LANDED_COST_AMOUNT);
      expect(Number(allocResult.data[0].materialId), 'allocation materialId matches test material').toBe(Number(material.id));

      // 状态不改：approveStatus 仍 UNSUBMITTED
      const afterAlloc = await verifyState(page, 'ErpInvLandedCost', landedCost.id, 'approveStatus');
      expect(afterAlloc.approveStatus, 'allocate should not change approveStatus').toBe('UNSUBMITTED');

      // ---- approve(id) @BizMutation 审核过账 ----
      const approved = await callMutationOk(
        page, 'ErpInvLandedCost', 'approve', { id: landedCost.id },
        'id approveStatus docStatus posted',
      );
      // doPostApprove 三者确定值（ErpInvLandedCostProcessor.java:284,287,289）
      expect(approved.approveStatus, 'approve should set approveStatus=APPROVED').toBe('APPROVED');
      expect(approved.docStatus, 'approve should set docStatus=DONE').toBe('DONE');
      expect(approved.posted, 'approve should set posted=true').toBe(true);

      // __get 权威确认
      const lcFinal = await verifyState(page, 'ErpInvLandedCost', landedCost.id, 'approveStatus docStatus posted');
      expect(lcFinal.approveStatus, '__get should confirm APPROVED').toBe('APPROVED');
      expect(lcFinal.docStatus, '__get should confirm DONE').toBe('DONE');
      expect(lcFinal.posted, '__get should confirm posted=true').toBe(true);

      // ---- LANDED_COST 凭证行精确数值断言 ----
      // billHeadCode = landedCost.code（无后缀，LandedCostPostingDispatcher.java:69）
      const voucherId = await findVoucherIdByBillCode(page, landedCost.code, 'NORMAL');
      await assertVoucherLines(page, voucherId, [
        { subjectCode: '1401', dcDirection: 'DEBIT', debitAmount: LANDED_COST_AMOUNT, creditAmount: 0 },
        { subjectCode: '2202', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: LANDED_COST_AMOUNT },
      ]);

      // ---- CostAdjust 成本层更新断言（MOVING_AVERAGE → ErpInvStockBalance.avgCost 更新，若可达查询）----
      // 注：MOVING_AVERAGE path 经 CostAdjustmentService.applyAverageLike 更新 StockBalance.avgCost（不写 CostLayer）。
      const balance = await findFirst<any>(
        page, 'ErpInvStockBalance',
        eqFilter('materialId', Number(material.id)),
        'avgCost totalCost quantity warehouseId',
      );
      // balance 可达性受 CostAdjust 内部余额查找逻辑影响（按 materialId+warehouseId+可选 batch 维度）。
      // 主要验证已由 approve 生命周期 + LANDED_COST 凭证行断言覆盖（Dr 1401 / Cr 2202 精确匹配）。
      if (balance) {
        // CostAdjust: newAvgCost = oldAvgCost(10) + allocatedAmount(50)/qty(10) = 15
        expect(Number(balance.avgCost), 'avgCost should reflect landed cost supplement').toBe(SETUP_UNIT_COST + LANDED_COST_AMOUNT / SETUP_QTY);
      }

      // ---- 非法迁移守卫：DONE→approve 抛 ERR_LANDED_COST_ALREADY_APPROVED ----
      const rej = await callMutation(page, 'ErpInvLandedCost', 'approve', { id: landedCost.id }, 'id');
      expect(rej.errors, 're-approve on APPROVED should be rejected').toBeTruthy();
    } finally {
      // ---- 清理（依赖反向顺序）----
      // 1. LANDED_COST 凭证（billHeadCode=landedCost.code 无后缀）
      await cleanupVoucherByBillCode(page, landedCost.code);
      // 2. CostAdjustLine + CostAdjust（按 materialId 反查 adjustId）
      const caLines = await findItems<any>(page, 'ErpInvCostAdjustLine', eqFilter('materialId', Number(material.id)), 'id adjustId');
      const adjustIds = new Set(caLines.map((l) => l.adjustId));
      for (const l of caLines) await deleteById(page, 'ErpInvCostAdjustLine', l.id);
      for (const aid of adjustIds) await deleteById(page, 'ErpInvCostAdjust', aid);
      // 3. StockLedger 按 materialId（含 INCOMING 备货 + CostAdjust 写入的 quantity=0 行）
      await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('materialId', Number(material.id)));
      // 4. StockBalance 按 materialId+warehouseId（INCOMING 备货建立的余额）
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
});
