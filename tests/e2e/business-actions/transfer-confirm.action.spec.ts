import { test, expect, loginAndNavigate, callMutation, callMutationOk, verifyState } from './_helper';

/**
 * USC-07 调拨确认行为断言（plan 2026-10-01-1230-1）。
 *
 * 核心主干故事 US-IV-02「在途可见」缺口补齐：运行时行为边界实测（plan 起草前核验）
 * confirm 仅状态翻转 DRAFT→CONFIRMED + 条件分派内部交易凭证钩子（种子女仓同法人
 * skip 分支，不抛错即达）；inTransitWarehouseId 实体字段服务层零消费（设计层概念
 * 未实现——「在途数量运行时追踪」successor 登记于计划 Deferred 节）。
 *
 * 断言：①confirm 状态翻转 DRAFT→CONFIRMED；②非法守卫 CONFIRMED 再 confirm 抛
 * ERR_ILLEGAL_STATUS_TRANSITION；③inTransitWarehouseId 字段持久化。
 */
test.describe('USC-07 transfer confirm behavior (US-IV-02 缺口补齐)', () => {
  test('confirm: DRAFT→CONFIRMED 翻转 + 非法守卫 + inTransitWarehouseId 持久化', async ({ page }) => {
    await loginAndNavigate(page, '/ErpInvTransferOrder-main');

    // 1. 建单（最小集沿 inventory.write.spec.ts :148 先例 + inTransitWarehouseId）
    const saved = await createTransfer(page, 'BAR-TRF-001');
    const id = String(saved.id);
    expect(saved.code).toBe('BAR-TRF-001');

    // 2. confirm → docStatus CONFIRMED（调拨状态机：DRAFT→CONFIRMED，design state-machine.md :178）
    await callMutationOk(page, 'ErpInvTransferOrder', 'confirm', { transferOrderId: id }, 'id docStatus');
    const st = await verifyState(page, 'ErpInvTransferOrder', id, 'docStatus inTransitWarehouseId');
    expect(st.docStatus).toBe('CONFIRMED');
    // inTransitWarehouseId 赋值持久化（实体字段持久化断言，非在途数量行为）
    if (saved.inTransitWarehouseId != null) {
      expect(String(st.inTransitWarehouseId)).toBe(String(saved.inTransitWarehouseId));
    }

    // 3. 非法守卫：CONFIRMED 再 confirm → ERR_ILLEGAL_STATUS_TRANSITION
    //    （守卫文案：状态=CONFIRMED，不允许执行该操作（期望状态=DRAFT）——状态机直抛领域码）
    const rej = await callMutation(page, 'ErpInvTransferOrder', 'confirm', { transferOrderId: id }, 'id');
    expect(rej.errors?.[0]?.message).toContain('不允许执行该操作');

    // 4. cleanup（CONFIRMED 同法人确认无下游产物，删行+删头）
    await cleanupTransfer(page, id);
  });

  test('inTransitWarehouseId 赋值场景: confirm 后字段持久化 + 守卫', async ({ page }) => {
    await loginAndNavigate(page, '/ErpInvTransferOrder-main');

    const saved = await createTransfer(page, 'BAR-TRF-002', '2');
    const id = String(saved.id);
    await callMutationOk(page, 'ErpInvTransferOrder', 'confirm', { transferOrderId: id }, 'id');

    const st = await verifyState(page, 'ErpInvTransferOrder', id, 'docStatus inTransitWarehouseId');
    expect(st.docStatus).toBe('CONFIRMED');
    expect(String(st.inTransitWarehouseId)).toBe('2');

    await cleanupTransfer(page, id);
  });
});

/** 建调拨单（最小集沿 inventory.write.spec.ts :148 先例 + inTransitWarehouseId）。 */
const SEED = { ORG: '2', WH_FROM: '1', WH_TO: '2', MAT: '1', UOM: '1' };

function transferData(code: string, inTransitWarehouseId?: string) {
  return {
    code,
    orgId: SEED.ORG,
    businessDate: '2026-10-01',
    fromWarehouseId: SEED.WH_FROM,
    toWarehouseId: SEED.WH_TO,
    docStatus: 'DRAFT',
    approveStatus: 'UNSUBMITTED',
    inTransitWarehouseId: inTransitWarehouseId ?? SEED.WH_FROM,
    lines: [{ lineNo: 1, materialId: SEED.MAT, uoMId: SEED.UOM, quantity: 10, batchNo: 'BATCH-001' }],
  };
}

async function createTransfer(page: import('@playwright/test').Page, code: string, inTransitWarehouseId?: string) {
  const { createViaSave } = await import('./_helper');
  return createViaSave(page, 'ErpInvTransferOrder', transferData(code, inTransitWarehouseId), 'id code inTransitWarehouseId');
}

async function cleanupTransfer(page: import('@playwright/test').Page, id: string) {
  const { callMutation } = await import('./_helper');
  await callMutation(page, 'ErpInvTransferOrder', 'delete', { id }, 'id');
}
