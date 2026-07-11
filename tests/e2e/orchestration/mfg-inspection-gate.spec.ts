import {
  test,
  expect,
  loginAndNavigate,
  runMfgChain,
  cleanupMfg,
  callMutation,
  verifyState,
  MFG_EXPECT,
} from './_helper';

/**
 * 制造域质检门控阻断完工浏览器层 E2E（plan 2026-07-11-0730-1 Phase 2，Gate 1 路径）。
 *
 * 验证 config `erp-mfg.inspection-gate-enabled=true` + BOM.inspectionRequired=true 时，
 * WorkOrder 满量完工（willFinish=true）被 ErpMfgWorkOrderProcessor.isInspectionGated 拦截
 * 抛 ERR_INSPECTION_REQUIRED 的浏览器层全栈可达性，以及门控精确性（inspectionRequired=false 时不拦截）。
 *
 * Gate 1 三条件（ErpMfgWorkOrderProcessor.isInspectionGated）：
 *   1. config erp-mfg.inspection-gate-enabled=true（webServer JVM args，playwright.config.ts:18）
 *   2. WorkOrder.bomId 非空（runMfgChain 总是设 bomId）
 *   3. BOM.inspectionRequired=true（runMfgChain 扩展 inspectionRequired=true 时设）
 *
 * 断言：
 *   - 负路径：inspectionRequired=true → reportCompletion 返回 GraphQL errors（含「质检」语义 token），
 *     WorkOrder docStatus 保持 IN_PROCESS（未完工）。
 *   - 对照路径：inspectionRequired=false（默认）→ reportCompletion 成功 → COMPLETED（Gate 1 未命中）。
 *
 * runMfgChain inspectionRequired=true 时链路停在 completeJob 之后（WO=IN_PROCESS），
 * 由本 spec 驱动 reportCompletion 断言负路径。
 *
 * 清理：cleanupMfg 覆盖（completionMove=null 安全跳过；WO 不论状态均可逻辑删除）。
 */

test.describe('manufacturing inspection gate browser-layer E2E (Gate 1: config + BOM.inspectionRequired)', () => {
  test('inspectionRequired BOM blocks full completion with ERR_INSPECTION_REQUIRED; WO stays IN_PROCESS', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgWorkOrder-main');

    // inspectionRequired=true：BOM 设 inspectionRequired=true，链路停在 completeJob（WO=IN_PROCESS）
    const r = await runMfgChain(page, { inspectionRequired: true });
    try {
      const before = await verifyState(page, 'ErpMfgWorkOrder', r.wo.id, 'docStatus');
      expect(before?.docStatus, 'before reportCompletion: chain stops at IN_PROCESS').toBe('IN_PROCESS');

      // 负路径：满量完工（willFinish=true）被 Gate 1 拦截
      const rej = await callMutation(
        page, 'ErpMfgWorkOrder', 'reportCompletion',
        { workOrderId: r.wo.id, completedQty: MFG_EXPECT.completedQty },
        'id docStatus',
      );
      expect(rej.errors, 'reportCompletion should be rejected by Gate 1 (inspection-gate-enabled + BOM.inspectionRequired)').toBeTruthy();
      // Nop GraphQL 在此配置下仅回传 i18n message（不序列化 extensions.errorCode），断言标志性语义 token。
      // ERR_INSPECTION_REQUIRED message 含「质检」+「完工入库暂挂」，唯一区分于状态迁移/可用量类错误。
      const rejBody = JSON.stringify(rej.errors);
      expect(rejBody, 'reject should carry inspection semantics').toContain('质检');

      // WO 保持 IN_PROCESS（完工被拦截，状态不变）
      const after = await verifyState(page, 'ErpMfgWorkOrder', r.wo.id, 'docStatus completedQuantity');
      expect(after?.docStatus, 'after rejected reportCompletion: docStatus stays IN_PROCESS').toBe('IN_PROCESS');
    } finally {
      await cleanupMfg(page, r);
    }
  });

  test('control path: inspectionRequired=false (default) → reportCompletion succeeds → COMPLETED', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgWorkOrder-main');

    // 默认 inspectionRequired=false：Gate 1 未命中（bom.inspectionRequired=false），正常完工
    const r = await runMfgChain(page);
    try {
      const woState = await verifyState(page, 'ErpMfgWorkOrder', r.wo.id, 'docStatus completedQuantity');
      expect(woState?.docStatus, 'control path: full completion → COMPLETED (gate not hit)').toBe('COMPLETED');
      expect(Number(woState?.completedQuantity ?? 0), 'completedQuantity=10').toBe(MFG_EXPECT.completedQty);
    } finally {
      await cleanupMfg(page, r);
    }
  });
});
