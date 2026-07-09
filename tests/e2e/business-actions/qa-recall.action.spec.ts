import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById, input } from './_helper';

/**
 * quality ErpQaRecall 业务动作浏览器层 E2E（plan 2026-07-10-0335-1 Phase 3）。
 *
 * 验证 DIRECT useApproval 审批轴（submitForApproval→approve→双字段翻转 approveStatus=APPROVED + status=APPROVED）+
 * 审批后域状态机迁移（locateTargets→IN_PROGRESS→notifyCustomers→close→CLOSED）+ reject 守卫 + cancel 迁移。
 *
 * 权威状态机（ErpQaRecallBizModel + ErpQaRecallProcessor）：双轴
 *   status 轴：register→OPEN，approve→APPROVED，locateTargets→IN_PROGRESS，close→CLOSED，cancel→CANCELLED
 *   approveStatus 轴：UNSUBMITTED→submit→SUBMITTED→approve→APPROVED / reject→REJECTED(+status→CANCELLED)
 *   submit/approve 前置 status=OPEN；locateTargets 前置 status=APPROVED；
 *   close 前置 status=IN_PROGRESS + notifyCustomer=true + 无 PENDING target（config 默认门控）。
 *
 * register 经 callMutationOk（Map 入参，非 createViaSave）——BizMutation 建单入口。
 * locateTargets 无匹配目标时仍迁移到 IN_PROGRESS（targetLocator.locate 产 0 target，不阻断）；
 * notifyCustomers 空目标循环仍置 notifyCustomer=true；close 门控通过（无 PENDING target）。
 *
 * 种子引用（init-data）：material id=1（MAT-001）。
 * 清理：Recall 审批/域迁移无不可逆下游产物（locateTargets 无匹配目标产 0 ErpQaRecallTarget），
 * 逻辑删除 Recall 自身。
 */

const MATERIAL_ID = 1;
const BDATE = '2026-07-09';

async function seedBatch(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpInvBatch',
    {
      batchNo: `E2E-BATCH-${tag}-${Date.now()}`,
      materialId: MATERIAL_ID,
      warehouseId: 2,
      totalQuantity: 100,
      availableQuantity: 100,
      status: 'OPEN',
    },
    'id',
  );
}

async function seedRecall(page: import('@playwright/test').Page, tag: string, batchId?: string): Promise<{ id: string; status: string; approveStatus: string }> {
  const data: Record<string, unknown> = {
    code: `E2E-QA-REC-${tag}-${Date.now()}`,
    recallName: `E2E Recall ${tag}`,
    triggerType: 'MANUAL',
    severityLevel: 'HIGH',
    businessDate: BDATE,
    materialId: MATERIAL_ID,
    remark: `E2E recall ${tag}`,
  };
  if (batchId) data.batchId = batchId;
  return callMutationOk(
    page, 'ErpQaRecall', 'register',
    { data: input('Map', data) },
    'id status approveStatus',
  );
}

test.describe('quality ErpQaRecall approval axis + domain state machine', () => {
  test('full path: register(OPEN) → submit(SUBMITTED) → approve(APPROVED,status=APPROVED) → locateTargets(IN_PROGRESS) → notifyCustomers → close(CLOSED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaRecall-main');

    const batch = await seedBatch(page, 'hp');
    const recall = await seedRecall(page, 'hp', batch.id);
    expect(recall.status, 'new recall status=OPEN').toBe('OPEN');
    expect(recall.approveStatus, 'new recall approveStatus=UNSUBMITTED').toBe('UNSUBMITTED');

    // 审批轴：submit → SUBMITTED
    await callMutationOk(page, 'ErpQaRecall', 'submitForApproval', { id: recall.id }, 'id');
    let s = await verifyState(page, 'ErpQaRecall', recall.id, 'status approveStatus');
    expect(s.approveStatus, 'after submit approveStatus=SUBMITTED').toBe('SUBMITTED');
    expect(s.status, 'after submit status still OPEN').toBe('OPEN');

    // approve → APPROVED + status=APPROVED（双字段翻转）
    await callMutationOk(page, 'ErpQaRecall', 'approve', { id: recall.id }, 'id');
    s = await verifyState(page, 'ErpQaRecall', recall.id, 'status approveStatus');
    expect(s.approveStatus, 'after approve approveStatus=APPROVED').toBe('APPROVED');
    expect(s.status, 'after approve status=APPROVED').toBe('APPROVED');

    // 域状态机：locateTargets → IN_PROGRESS（无匹配目标，迁移仍发生）
    await callMutationOk(page, 'ErpQaRecall', 'locateTargets', { recallId: recall.id }, 'id status');
    s = await verifyState(page, 'ErpQaRecall', recall.id, 'status');
    expect(s.status, 'after locateTargets status=IN_PROGRESS').toBe('IN_PROGRESS');

    // notifyCustomers → notifyCustomer=true（空目标循环仍置标志）
    await callMutationOk(page, 'ErpQaRecall', 'notifyCustomers', { recallId: recall.id }, 'id');
    s = await verifyState(page, 'ErpQaRecall', recall.id, 'notifyCustomer');
    expect(s.notifyCustomer, 'after notifyCustomers notifyCustomer=true').toBe(true);

    // close → CLOSED（门控通过：notifyCustomer=true + 无 PENDING target）
    await callMutationOk(page, 'ErpQaRecall', 'close', { recallId: recall.id }, 'id status');
    s = await verifyState(page, 'ErpQaRecall', recall.id, 'status');
    expect(s.status, 'after close status=CLOSED').toBe('CLOSED');

    // 清理
    await deleteById(page, 'ErpQaRecall', recall.id);
    await deleteById(page, 'ErpInvBatch', batch.id);
  });

  test('reject path: register(OPEN) → submit(SUBMITTED) → reject(REJECTED, status=CANCELLED); illegal guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaRecall-main');

    const recall = await seedRecall(page, 'rj');

    await callMutationOk(page, 'ErpQaRecall', 'submitForApproval', { id: recall.id }, 'id');
    let s = await verifyState(page, 'ErpQaRecall', recall.id, 'approveStatus');
    expect(s.approveStatus, 'after submit approveStatus=SUBMITTED').toBe('SUBMITTED');

    // reject → REJECTED + status=CANCELLED
    await callMutationOk(page, 'ErpQaRecall', 'reject', { id: recall.id }, 'id');
    s = await verifyState(page, 'ErpQaRecall', recall.id, 'status approveStatus');
    expect(s.approveStatus, 'after reject approveStatus=REJECTED').toBe('REJECTED');
    expect(s.status, 'after reject status=CANCELLED').toBe('CANCELLED');

    // 非法迁移守卫：CANCELLED(status) → locateTargets（须 APPROVED）
    const rej = await callMutation(page, 'ErpQaRecall', 'locateTargets', { recallId: recall.id }, 'id');
    expect(rej.errors, 'locateTargets from CANCELLED should be rejected').toBeTruthy();

    await deleteById(page, 'ErpQaRecall', recall.id);
  });

  test('cancel path: register(OPEN) → cancel(CANCELLED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaRecall-main');

    const recall = await seedRecall(page, 'cn');

    await callMutationOk(page, 'ErpQaRecall', 'cancel', { recallId: recall.id }, 'id status');
    const s = await verifyState(page, 'ErpQaRecall', recall.id, 'status');
    expect(s.status, 'after cancel status=CANCELLED').toBe('CANCELLED');

    await deleteById(page, 'ErpQaRecall', recall.id);
  });
});
