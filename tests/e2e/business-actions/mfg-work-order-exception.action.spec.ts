import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  callMutation,
  verifyState,
  deleteById,
} from './_helper';
import {
  cleanupMfg,
  findFirst,
  eqFilter,
  andFilter,
  SEED,
  type MfgResult,
} from '../orchestration/_helper';

/**
 * manufacturing ErpMfgWorkOrder 异常分支 + 部分完工浏览器层 E2E
 * （plan 2026-07-10-1800-2 Phase 1 + Phase 2）。
 *
 * Phase 1——WorkOrder 异常分支（ErpMfgWorkOrderProcessor:124-166）：
 *   正路径 A：stop（IN_PROCESS→STOPPED）→ resume（STOPPED→IN_PROCESS）→ close（IN_PROCESS→CLOSED，actualEndDate 回填）
 *   正路径 B：stop（IN_PROCESS→STOPPED）→ close（STOPPED→CLOSED）
 *   正路径 C：cancel（NOT_STARTED→CANCELLED）
 *   非法迁移守卫：IN_PROCESS→cancel 拒绝（cancel 仅允许 DRAFT/SUBMITTED/NOT_STARTED）；CLOSED→stop/resume 拒绝
 *
 * Phase 2——部分完工（ErpMfgWorkOrderProcessor.reportCompletion willFinish 门控）：
 *   reportCompletion(completedQty=10 < plannedQty=20) → willFinish=false → docStatus 保持 IN_PROCESS
 *   + completedQuantity=10 → close → CLOSED（非 COMPLETED）。
 *
 * 复用 0335-1 范式（DIRECT 审批轴 + 简单 BOM 无子件 → checkAvailability → STOCK_RESERVED）。
 * 清理：异常分支无过账/库存产物（仅逻辑删除 WO+BOM）；部分完工有 MANUFACTURE 入库移动 → cleanupMfg。
 */

const PRODUCT_ID = 1; // MAT-001
const BDATE = '2026-07-10';

async function seedBom(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpMfgBom',
    { code: `E2E-MFG-BOM-EX-${tag}-${Date.now()}`, productId: PRODUCT_ID, bomType: 'NORMAL' },
    'id',
  );
}

async function seedWorkOrder(
  page: import('@playwright/test').Page,
  tag: string,
  bomId: string,
  plannedQty = 10,
): Promise<{ id: string; docStatus: string; approveStatus: string }> {
  return createViaSave(
    page, 'ErpMfgWorkOrder',
    {
      code: `E2E-MFG-WO-EX-${tag}-${Date.now()}`,
      productId: PRODUCT_ID,
      plannedQuantity: plannedQty,
      businessDate: BDATE,
      docStatus: 'DRAFT',
      approveStatus: 'UNSUBMITTED',
      bomId,
    },
    'id docStatus approveStatus',
  );
}

/** 审批 + 齐套 + 开工 → IN_PROCESS（复用 0335-1 范式）。 */
async function seedToInProcess(
  page: import('@playwright/test').Page,
  tag: string,
): Promise<{ wo: { id: string }; bom: { id: string } }> {
  const bom = await seedBom(page, tag);
  const wo = await seedWorkOrder(page, tag, bom.id);
  await callMutationOk(page, 'ErpMfgWorkOrder', 'submitForApproval', { id: wo.id }, 'id');
  await callMutationOk(page, 'ErpMfgWorkOrder', 'approve', { id: wo.id }, 'id');
  await callMutationOk(page, 'ErpMfgWorkOrder', 'checkAvailability', { workOrderId: wo.id }, 'id docStatus');
  await callMutationOk(page, 'ErpMfgWorkOrder', 'start', { workOrderId: wo.id }, 'id docStatus');
  return { wo, bom };
}

async function cleanupWoBom(
  page: import('@playwright/test').Page,
  wo: { id: string } | null,
  bom: { id: string } | null,
): Promise<void> {
  if (wo) await deleteById(page, 'ErpMfgWorkOrder', wo.id);
  if (bom) await deleteById(page, 'ErpMfgBom', bom.id);
}

// ---------- Phase 1：异常分支 ----------

test.describe('manufacturing ErpMfgWorkOrder exception branches (stop/resume/close/cancel)', () => {
  test('path A: start→IN_PROCESS → stop→STOPPED → resume→IN_PROCESS → close→CLOSED (actualEndDate backfilled)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgWorkOrder-main');
    const { wo, bom } = await seedToInProcess(page, 'pA');
    try {
      let s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus actualEndDate');
      expect(s.docStatus, 'precondition IN_PROCESS').toBe('IN_PROCESS');

      await callMutationOk(page, 'ErpMfgWorkOrder', 'stop', { workOrderId: wo.id }, 'id docStatus');
      s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus');
      expect(s.docStatus, 'after stop docStatus=STOPPED').toBe('STOPPED');

      await callMutationOk(page, 'ErpMfgWorkOrder', 'resume', { workOrderId: wo.id }, 'id docStatus');
      s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus');
      expect(s.docStatus, 'after resume docStatus=IN_PROCESS').toBe('IN_PROCESS');

      await callMutationOk(page, 'ErpMfgWorkOrder', 'close', { workOrderId: wo.id }, 'id docStatus');
      s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus actualEndDate');
      expect(s.docStatus, 'after close docStatus=CLOSED').toBe('CLOSED');
      expect(s.actualEndDate, 'close should backfill actualEndDate').toBeTruthy();
    } finally {
      await cleanupWoBom(page, wo, bom);
    }
  });

  test('path B: start→IN_PROCESS → stop→STOPPED → close→CLOSED (close reachable from STOPPED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgWorkOrder-main');
    const { wo, bom } = await seedToInProcess(page, 'pB');
    try {
      await callMutationOk(page, 'ErpMfgWorkOrder', 'stop', { workOrderId: wo.id }, 'id docStatus');
      let s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus');
      expect(s.docStatus, 'after stop docStatus=STOPPED').toBe('STOPPED');

      await callMutationOk(page, 'ErpMfgWorkOrder', 'close', { workOrderId: wo.id }, 'id docStatus');
      s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus');
      expect(s.docStatus, 'close from STOPPED → CLOSED').toBe('CLOSED');
    } finally {
      await cleanupWoBom(page, wo, bom);
    }
  });

  test('path C: approve→NOT_STARTED → cancel→CANCELLED', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgWorkOrder-main');
    const bom = await seedBom(page, 'pC');
    const wo = await seedWorkOrder(page, 'pC', bom.id);
    try {
      await callMutationOk(page, 'ErpMfgWorkOrder', 'submitForApproval', { id: wo.id }, 'id');
      await callMutationOk(page, 'ErpMfgWorkOrder', 'approve', { id: wo.id }, 'id');
      let s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus');
      expect(s.docStatus, 'after approve docStatus=NOT_STARTED').toBe('NOT_STARTED');

      await callMutationOk(page, 'ErpMfgWorkOrder', 'cancel', { workOrderId: wo.id }, 'id docStatus');
      s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus');
      expect(s.docStatus, 'after cancel docStatus=CANCELLED').toBe('CANCELLED');
    } finally {
      await cleanupWoBom(page, wo, bom);
    }
  });

  test('guard: IN_PROCESS→cancel rejected (cancel only allows DRAFT/SUBMITTED/NOT_STARTED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgWorkOrder-main');
    const { wo, bom } = await seedToInProcess(page, 'g1');
    try {
      const rej = await callMutation(page, 'ErpMfgWorkOrder', 'cancel', { workOrderId: wo.id }, 'id');
      expect(rej.errors, 'cancel from IN_PROCESS should be rejected').toBeTruthy();

      const s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus');
      expect(s.docStatus, 'docStatus unchanged after rejected cancel').toBe('IN_PROCESS');
    } finally {
      await cleanupWoBom(page, wo, bom);
    }
  });

  test('guard: CLOSED→stop/resume rejected (stop requires IN_PROCESS, resume requires STOPPED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgWorkOrder-main');
    const { wo, bom } = await seedToInProcess(page, 'g2');
    try {
      await callMutationOk(page, 'ErpMfgWorkOrder', 'close', { workOrderId: wo.id }, 'id docStatus');
      let s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus');
      expect(s.docStatus, 'precondition CLOSED').toBe('CLOSED');

      const rejStop = await callMutation(page, 'ErpMfgWorkOrder', 'stop', { workOrderId: wo.id }, 'id');
      expect(rejStop.errors, 'stop from CLOSED should be rejected').toBeTruthy();

      const rejResume = await callMutation(page, 'ErpMfgWorkOrder', 'resume', { workOrderId: wo.id }, 'id');
      expect(rejResume.errors, 'resume from CLOSED should be rejected').toBeTruthy();

      s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus');
      expect(s.docStatus, 'docStatus unchanged after rejected stop/resume').toBe('CLOSED');
    } finally {
      await cleanupWoBom(page, wo, bom);
    }
  });
});

// ---------- Phase 2：部分完工 ----------

test.describe('manufacturing ErpMfgWorkOrder partial completion (willFinish gating)', () => {
  test('partial completion: reportCompletion(qty<planned) → IN_PROCESS retained → close → CLOSED', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgWorkOrder-main');

    const ts = Date.now();
    const woCode = `E2E-MFG-WO-PC-${ts}`;
    const bomCode = `E2E-MFG-BOM-PC-${ts}`;

    const bom = await createViaSave(
      page, 'ErpMfgBom',
      { code: bomCode, productId: PRODUCT_ID, bomType: 'NORMAL' },
      'id',
    );
    const wo = await createViaSave(
      page, 'ErpMfgWorkOrder',
      {
        code: woCode, productId: PRODUCT_ID, plannedQuantity: 20, businessDate: BDATE,
        docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', bomId: bom.id,
      },
      'id',
    );
    // OUTPUT 行：generateCompletionMove 读此生成入库移动（部分完工也会生成移动）
    await createViaSave(
      page, 'ErpMfgWorkOrderLine',
      {
        workOrderId: wo.id, lineNo: 1, lineType: 'OUTPUT',
        materialId: PRODUCT_ID, uoMId: SEED.UOM, plannedQuantity: 20,
        destWarehouseId: SEED.WH_RAW,
      },
      'id',
    );

    const r: MfgResult = {
      bom, wo,
      codes: { component: '', setup: '', bom: bomCode, wo: woCode, issue: '', jobCard: '' },
    } as MfgResult;

    try {
      await callMutationOk(page, 'ErpMfgWorkOrder', 'submitForApproval', { id: wo.id }, 'id');
      await callMutationOk(page, 'ErpMfgWorkOrder', 'approve', { id: wo.id }, 'id');
      await callMutationOk(page, 'ErpMfgWorkOrder', 'checkAvailability', { workOrderId: wo.id }, 'id docStatus');
      await callMutationOk(page, 'ErpMfgWorkOrder', 'start', { workOrderId: wo.id }, 'id docStatus');

      // 部分完工：completedQty=10 < plannedQty=20 → willFinish=false
      await callMutationOk(
        page, 'ErpMfgWorkOrder', 'reportCompletion',
        { workOrderId: wo.id, completedQty: 10 },
        'id docStatus completedQuantity',
      );
      let s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus completedQuantity');
      expect(s.docStatus, 'partial completion: willFinish=false → IN_PROCESS retained').toBe('IN_PROCESS');
      expect(Number(s.completedQuantity ?? 0), 'completedQuantity accumulated to 10').toBe(10);

      // close → CLOSED（非 COMPLETED，因 willFinish 未触发）
      await callMutationOk(page, 'ErpMfgWorkOrder', 'close', { workOrderId: wo.id }, 'id docStatus');
      s = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus completedQuantity');
      expect(s.docStatus, 'after close docStatus=CLOSED').toBe('CLOSED');
      expect(Number(s.completedQuantity ?? 0), 'completedQuantity still 10 after close').toBe(10);

      // 完工入库移动（部分完工也生成 MANUFACTURE 移动，10 件）
      r.completionMove = await findFirst(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_MFG_WORK_ORDER'), eqFilter('relatedBillCode', woCode)),
        'id code',
      );
    } finally {
      await cleanupMfg(page, r);
    }
  });
});
