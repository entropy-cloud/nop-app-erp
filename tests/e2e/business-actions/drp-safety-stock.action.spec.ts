import { test, expect, loginAndNavigate, createViaSave, callMutationOk, verifyState, deleteById } from './_helper';

/**
 * drp ErpInvDrpSafetyStockCalc 安全库存优化业务动作浏览器层 E2E（plan 2026-07-14-0215-2 Phase 2）。
 *
 * 验证 calculate / confirmWriteback 经 GraphQL /graphql 的全栈可达性 + 计算结果/参数回写。
 *
 * 权威引擎（ErpInvDrpSafetyStockCalcBizModel 委派 SafetyStockEngine，对齐
 * docs/design/drp/safety-stock-optimization.md）：
 *   calculate：按 method 算 calculatedSafetyStock/calculatedRop + 回写 lastCalculatedAt。
 *     SIMPLE = meanDaily × leadTime × 0.5；无历史出库时 meanDaily=0 → 计算结果=0（非空写回；
 *     SafetyStockEngine.monthlyDemands 无历史返回 [0]，避免 mean 除零——plan 期修复的引擎 bug）。
 *   confirmWriteback：人工确认后回写 ErpDrpParameter.safetyStock（overrideSafetyStock 优先，否则计算值）；
 *     无匹配 ErpDrpParameter 抛 ERR_DRP_PARAMETER_MISSING。config erp-inv.drp-ss-auto-writeback 默认
 *     false（人工复核门），须显式调本方法才回写。
 *
 * ORM 无 useWorkflow / 无 useApproval，纯 DIRECT @BizMutation 浏览器层可达。
 *
 * 历史前置说明：SafetyStockEngine 以「已过账 OUTGOING 移动」按月聚合需求；种子 stock_move 全
 *   posted=false，且 __save 强制 posted=false（过账须经 complete 派发链，可能优雅降级）。故自包含
 *   无 posted 历史时 SIMPLE 计算结果=0（非空）——本 spec 验证 calculate 浏览器层可达性 + 结果字段写回
 *   + lastCalculatedAt 时间戳；写回正路径由 overrideSafetyStock=50 经 confirmWriteback 确定性断言。
 *
 * 隔离策略：__save 强制 org FK 校验，故用种子 org id=2；warehouseId=2（区别于 drp-plan-engine 的
 *   warehouseId=1）确保 confirmWriteback 三元组 (mat4,wh2,org2) 精确命中本用例自建 Parameter。
 *
 * 自包含 setup：建 ErpDrpParameter（safetyStock=0）+ Calc（override=50）。
 * 清理：删 Calc + Parameter。
 */

const MATERIAL_ID = 4; // MAT-004 包装
const WAREHOUSE_ID = 2; // WH-RAW（区别于 drp-plan-engine 的 WH-MAIN，三元组隔离）
const ORG_ID = 2; // 种子 ERP-CO（__save 强制 org FK 校验）

async function seedParameter(page: import('@playwright/test').Page, tag: string): Promise<{ id: string; safetyStock: number | string }> {
  return createViaSave(
    page, 'ErpDrpParameter',
    {
      warehouseId: WAREHOUSE_ID,
      materialId: MATERIAL_ID,
      safetyStock: 0,
      replenishmentMethod: 'LOT_FOR_LOT',
      orgId: ORG_ID,
    },
    'id safetyStock',
  );
}

async function seedCalc(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpInvDrpSafetyStockCalc',
    {
      code: `E2E-SS-CALC-${tag}-${Date.now()}`,
      orgId: ORG_ID,
      materialId: MATERIAL_ID,
      warehouseId: WAREHOUSE_ID,
      method: 'SIMPLE',
      serviceLevel: 'PCT95',
      leadTimeDays: 7,
      overrideSafetyStock: 50,
    },
    'id',
  );
}

test.describe('drp ErpInvDrpSafetyStockCalc calculate/writeback', () => {
  test('calculate reachable (calculatedSafetyStock non-null + lastCalculatedAt set) → confirmWriteback writes override=50 to ErpDrpParameter', async ({ page }) => {
    await loginAndNavigate(page, '/ErpInvDrpSafetyStockCalc-main');

    const param = await seedParameter(page, 'hp');
    const calc = await seedCalc(page, 'hp');

    // calculate：浏览器层可达 + 计算结果字段写回（无 posted 出库历史 → SIMPLE meanDaily=0 → 计算值 0，非空）
    await callMutationOk(page, 'ErpInvDrpSafetyStockCalc', 'calculate', { calcId: calc.id }, 'id');
    const cs = await verifyState(page, 'ErpInvDrpSafetyStockCalc', calc.id, 'calculatedSafetyStock calculatedRop lastCalculatedAt');
    expect(cs.calculatedSafetyStock, 'calculate should populate calculatedSafetyStock (non-null, 0 due to no posted history)').not.toBeNull();
    expect(cs.lastCalculatedAt, 'calculate should set lastCalculatedAt').not.toBeNull();

    // confirmWriteback：overrideSafetyStock=50（>=0 优先）回写 ErpDrpParameter.safetyStock（0 → 50）
    await callMutationOk(page, 'ErpInvDrpSafetyStockCalc', 'confirmWriteback', { calcId: calc.id }, 'id');
    const ps = await verifyState(page, 'ErpDrpParameter', param.id, 'safetyStock');
    expect(Number(ps.safetyStock), 'confirmWriteback should write overrideSafetyStock=50 to parameter').toBe(50);

    // 清理：删 Calc + Parameter
    await deleteById(page, 'ErpInvDrpSafetyStockCalc', calc.id);
    await deleteById(page, 'ErpDrpParameter', param.id);
  });
});
