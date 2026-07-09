import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById } from './_helper';

/**
 * manufacturing ErpMfgWorkOrder 业务动作浏览器层 E2E（plan 2026-07-10-0335-1 Phase 1）。
 *
 * 验证 DIRECT useApproval 审批轴（submitForApproval→approve→approveStatus 翻转）+ reject 守卫 +
 * 审批后域状态机迁移（NOT_STARTED→checkAvailability→STOCK_RESERVED→start→IN_PROCESS→close→CLOSED）。
 *
 * 权威状态机（ErpMfgWorkOrderProcessor）：审批轴 + 工单 10 态双轴
 *   审批轴：UNSUBMITTED --submit--> SUBMITTED --approve--> APPROVED（+ docStatus DRAFT→SUBMITTED→NOT_STARTED）
 *           SUBMITTED --reject--> REJECTED
 *   工单轴：NOT_STARTED --checkAvailability--> STOCK_RESERVED --start--> IN_PROCESS --close--> CLOSED
 *           NOT_STARTED --cancel--> CANCELLED
 *   submit 前置 docStatus=DRAFT；approve 前置 docStatus=SUBMITTED；
 *   start 前置 STOCK_RESERVED/STOCK_PARTIAL（需 checkAvailability 先执行）。
 *
 * start 需齐套校验前置（checkAvailability），而 checkAvailability 需要 BOM。本 spec 创建无子件 BOM
 * （requiredByMaterial 为空 → KitAvailabilityResult.reserved → STOCK_RESERVED），仅作为 start 门控 enabler，
 * 不验证齐套校验本身（完整制造链归 orchestration successor，见 plan Non-Goals）。
 *
 * 种子引用（init-data）：product/material id=1（MAT-001 产品甲）。
 * 清理：WorkOrder approve 不触发 posted（无凭证产物），逻辑删除 WO + BOM 自身。
 */

const PRODUCT_ID = 1;
const BDATE = '2026-07-09';

async function seedBom(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpMfgBom',
    {
      code: `E2E-MFG-BOM-${tag}-${Date.now()}`,
      productId: PRODUCT_ID,
      bomType: 'NORMAL',
    },
    'id',
  );
}

async function seedWorkOrder(page: import('@playwright/test').Page, tag: string, bomId?: string): Promise<{ id: string; status: string; approveStatus: string }> {
  return createViaSave(
    page, 'ErpMfgWorkOrder',
    {
      code: `E2E-MFG-WO-${tag}-${Date.now()}`,
      productId: PRODUCT_ID,
      plannedQuantity: 10,
      businessDate: BDATE,
      docStatus: 'DRAFT',
      approveStatus: 'UNSUBMITTED',
      bomId: bomId ?? null,
    },
    'id docStatus approveStatus',
  );
}

test.describe('manufacturing ErpMfgWorkOrder approval axis + domain state machine', () => {
  test('approval happy path: save(DRAFT) → submit(SUBMITTED) → approve(APPROVED,NOT_STARTED) → checkAvailability(STOCK_RESERVED) → start(IN_PROCESS) → close(CLOSED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgWorkOrder-main');

    const bom = await seedBom(page, 'hp');
    const wo = await seedWorkOrder(page, 'hp', bom.id);
    expect(wo.docStatus, 'new wo docStatus=DRAFT').toBe('DRAFT');
    expect(wo.approveStatus, 'new wo approveStatus=UNSUBMITTED').toBe('UNSUBMITTED');

    // 审批轴正路径：submit → SUBMITTED
    await callMutationOk(page, 'ErpMfgWorkOrder', 'submitForApproval', { id: wo.id }, 'id');
    let s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'approveStatus docStatus');
    expect(s.approveStatus, 'after submit approveStatus=SUBMITTED').toBe('SUBMITTED');
    expect(s.docStatus, 'after submit docStatus=SUBMITTED').toBe('SUBMITTED');

    // approve → APPROVED + docStatus NOT_STARTED
    await callMutationOk(page, 'ErpMfgWorkOrder', 'approve', { id: wo.id }, 'id');
    s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'approveStatus docStatus');
    expect(s.approveStatus, 'after approve approveStatus=APPROVED').toBe('APPROVED');
    expect(s.docStatus, 'after approve docStatus=NOT_STARTED').toBe('NOT_STARTED');

    // 审批后域迁移：checkAvailability（无子件 BOM → STOCK_RESERVED）→ start → IN_PROCESS → close → CLOSED
    await callMutationOk(page, 'ErpMfgWorkOrder', 'checkAvailability', { workOrderId: wo.id }, 'id docStatus');
    s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus');
    expect(s.docStatus, 'after checkAvailability docStatus=STOCK_RESERVED').toBe('STOCK_RESERVED');

    await callMutationOk(page, 'ErpMfgWorkOrder', 'start', { workOrderId: wo.id }, 'id docStatus');
    s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus');
    expect(s.docStatus, 'after start docStatus=IN_PROCESS').toBe('IN_PROCESS');

    await callMutationOk(page, 'ErpMfgWorkOrder', 'close', { workOrderId: wo.id }, 'id docStatus');
    s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus');
    expect(s.docStatus, 'after close docStatus=CLOSED').toBe('CLOSED');

    // 清理
    await deleteById(page, 'ErpMfgWorkOrder', wo.id);
    await deleteById(page, 'ErpMfgBom', bom.id);
  });

  test('reject guard: save(DRAFT) → submit(SUBMITTED) → reject(REJECTED); illegal approve guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgWorkOrder-main');

    const wo = await seedWorkOrder(page, 'rj');

    await callMutationOk(page, 'ErpMfgWorkOrder', 'submitForApproval', { id: wo.id }, 'id');
    let s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'approveStatus');
    expect(s.approveStatus, 'after submit approveStatus=SUBMITTED').toBe('SUBMITTED');

    // reject → REJECTED
    await callMutationOk(page, 'ErpMfgWorkOrder', 'reject', { id: wo.id }, 'id');
    s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'approveStatus');
    expect(s.approveStatus, 'after reject approveStatus=REJECTED').toBe('REJECTED');

    // 非法迁移守卫：REJECTED → approve（须 SUBMITTED），经 GraphQL 返回 errors
    const rej = await callMutation(page, 'ErpMfgWorkOrder', 'approve', { id: wo.id }, 'id');
    expect(rej.errors, 'approve from REJECTED should be rejected as illegal transition').toBeTruthy();

    // 清理
    await deleteById(page, 'ErpMfgWorkOrder', wo.id);
  });
});
