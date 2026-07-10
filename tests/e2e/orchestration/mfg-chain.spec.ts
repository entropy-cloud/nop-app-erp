import {
  test,
  expect,
  loginAndNavigate,
  runMfgChain,
  cleanupMfg,
  findPageTotal,
  findFirst,
  verifyState,
  eqFilter,
  andFilter,
  MFG_EXPECT,
  SEED,
} from './_helper';

/**
 * 制造工单完整链路编排浏览器层 E2E（plan 2026-07-10-0704-2 Phase 1）。
 *
 * 经 GraphQL /graphql 驱动 WorkOrder + MaterialIssue + JobCard 三聚合根协作全链：
 *   前置备货（组件物料建库存）→ BOM+行 → WorkOrder+OUTPUT/INPUT 行 → 审批轴（submit→approve）
 *   → 齐套校验（checkAvailability→STOCK_RESERVED）→ 开工（start→IN_PROCESS）
 *   → 领料出库（MaterialIssue→confirm，触发 OUTGOING 移动 + WorkOrder.materialCost 回写）
 *   → 报工（JobCard→startJob→recordWork→回写 WorkOrder.laborCost→submitJob→completeJob）
 *   → 完工入库（reportCompletion，触发 MANUFACTURE 入库移动 + WorkOrder.totalCost/unitCost 重算 + COMPLETED）。
 *
 * 断言三层（跨聚合根协作产物）：
 *   1. 状态流转——每步 docStatus 翻转经 helper 内 verifyState `__get` 独立断言（DRAFT→SUBMITTED→NOT_STARTED
 *      →STOCK_RESERVED→IN_PROCESS→COMPLETED）。
 *   2. 领料出库产物——MaterialIssue.confirm 触发 ErpInvStockMove(relatedBillType=ERP_MFG_ISSUE) 存在
 *      + WorkOrder.materialCost > 0（出库流水 totalCost 绝对值回写）。
 *   3. 报工 + 完工入库产物——JobCard.recordWork 回写 WorkOrder.laborCost > 0；reportCompletion 触发
 *      ErpInvStockMove(relatedBillType=ERP_MFG_WORK_ORDER) 入库移动存在 + docStatus=COMPLETED
 *      + completedQuantity=10 + totalCost > 0 + unitCost > 0 + posted=false（完工入库 GL 过账为 Non-Goal，
 *      待 finance 域制造过账 Provider）。
 *
 * 确定性期望值（见 helper MFG_EXPECT）：组件用量 2/单位 × plannedQty 10 = 齐套需求 20；备货 30 覆盖；
 *   materialCost = 出库 20 × moving-average unitCost 50 = 1000；laborCost = 60min/60 × 100 = 100；
 *   totalCost = 1000+100 = 1100；unitCost = 1100/10 = 110。
 *
 * 清理：链路创建不可逆下游产物（库存流水/余额、工序卡工时记录），全栈共享同一 H2 实例，不清理会污染
 *   下游数值断言基线（inventory dashboard totalValue）。finally 调 cleanupMfg 逐域逻辑删除。
 */

test.describe('manufacturing WorkOrder full chain orchestration (WorkOrder + MaterialIssue + JobCard) browser-layer E2E', () => {
  test('full chain: kit check → issue → report work → completion (three aggregate roots collaboration)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgWorkOrder-main');

    const r = await runMfgChain(page);
    try {
      // ---- 领料出库产物：MaterialIssue.confirm 触发 OUTGOING 移动（relatedBillType=ERP_MFG_ISSUE）----
      const issueMoveTotal = await findPageTotal(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_MFG_ISSUE'), eqFilter('relatedBillCode', r.codes.issue)),
      );
      expect(issueMoveTotal, 'MaterialIssue.confirm should produce an ErpInvStockMove').toBeGreaterThan(0);

      // ---- 跨聚合根协作：MaterialIssue.confirm → WorkOrder.materialCost 回写 ----
      // materialCost = 出库流水 totalCost 绝对值回写。组件为测试专用物料（无种子余额，无 WEIGHTED_AVERAGE 混合），
      // 故确定性：出库 20 × moving-average unitCost 50 = 1000。
      let woState = await verifyState(
        page, 'ErpMfgWorkOrder', r.wo.id, 'materialCost laborCost totalCost unitCost completedQuantity docStatus posted',
      );
      expect(Number(woState?.materialCost ?? 0), 'MaterialIssue.confirm should writeback WorkOrder.materialCost > 0').toBeGreaterThan(0);
      expect(Number(woState?.materialCost ?? 0), 'WorkOrder.materialCost = issued qty 20 × unitCost 50').toBe(MFG_EXPECT.materialCost);

      // ---- 跨聚合根协作：JobCard.recordWork → WorkOrder.laborCost 回写 ----
      // laborCost = durationMins 60 / 60 × hourlyRate 100 = 100（确定性，不经库存）
      expect(Number(woState?.laborCost ?? 0), 'JobCard.recordWork should writeback WorkOrder.laborCost > 0').toBeGreaterThan(0);
      expect(Number(woState?.laborCost ?? 0), 'WorkOrder.laborCost = 60min/60 × rate 100').toBe(MFG_EXPECT.laborCost);

      // ---- 完工入库产物：reportCompletion 触发 MANUFACTURE 入库移动（relatedBillType=ERP_MFG_WORK_ORDER）----
      const completionMoveTotal = await findPageTotal(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_MFG_WORK_ORDER'), eqFilter('relatedBillCode', r.codes.wo)),
      );
      expect(completionMoveTotal, 'reportCompletion should produce an ErpInvStockMove').toBeGreaterThan(0);
      expect(r.completionMove, 'completion move should be found').toBeTruthy();
      expect(r.completionMove!.docStatus, 'business-linked completion move auto-completes to DONE').toBe('DONE');
      expect(r.completionMove!.posted, 'completion move posted=false (GL posting is Non-Goal)').toBe(false);

      // ---- 完工入库终态断言：COMPLETED + 成本归集重算 ----
      woState = await verifyState(
        page, 'ErpMfgWorkOrder', r.wo.id, 'materialCost laborCost totalCost unitCost completedQuantity docStatus posted',
      );
      expect(woState?.docStatus, 'after reportCompletion docStatus=COMPLETED').toBe('COMPLETED');
      expect(Number(woState?.completedQuantity ?? 0), 'completedQuantity=10').toBe(MFG_EXPECT.completedQty);
      expect(Number(woState?.totalCost ?? 0), 'totalCost = material+labor > 0').toBeGreaterThan(0);
      expect(Number(woState?.totalCost ?? 0), 'totalCost = material(1000)+labor(100) = 1100').toBe(MFG_EXPECT.totalCost);
      expect(Number(woState?.unitCost ?? 0), 'unitCost = total/completed > 0').toBeGreaterThan(0);
      expect(Number(woState?.unitCost ?? 0), 'unitCost = 1100/10 = 110').toBe(MFG_EXPECT.unitCost);
      expect(woState?.posted, 'WorkOrder.posted=false (MANUFACTURING_RECEIPT GL voucher is Non-Goal)').toBe(false);
    } finally {
      await cleanupMfg(page, r);
    }
  });
});

// 显式标注种子引用（供 lint/可读性；SEED 复用避免硬编码漂移）
void SEED;
void findFirst;
