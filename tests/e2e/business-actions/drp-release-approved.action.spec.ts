import { test, expect, loginAndNavigate, createViaSave, callMutation, callMutationOk, verifyState, findFirst, deleteByFilter, deleteById, eqFilter, andFilter } from './_helper';

/**
 * drp ErpDrpLine 批量释放 releaseApproved 业务动作浏览器层 E2E（plan 2026-07-17-1005-2 Phase 2）。
 *
 * 验证 releaseApproved(planId) 经 GraphQL /graphql 的全栈可达性 + 全行 ORDERED + 计划 EXECUTED 翻转 +
 * 下游单据（TRANSFER→ErpInvTransferOrder / PURCHASE→ErpPurOrder）批量创建。
 *
 * 权威实现（ErpDrpLineBizModel.releaseApproved:41 → DrpReleaseService.releaseApproved:99-113）：
 *   - 按 (planId, status=APPROVED) 过滤行（:101-103），逐行 releaseLine（:106）建下游单据。
 *   - 全部行 ORDERED/CANCELLED 后 advancePlanToExecutedIfComplete（:110,115-134）置 plan=EXECUTED。
 *   - **无 plan 级状态守卫**：非 APPROVED 行被过滤跳过，released=0 时空返回（不抛错）。
 *   - 每行 releaseLine 含 per-line 守卫（DrpReleaseService.requireReleasable:144-149）。
 *   - 下游 code 前缀 `DRP-`（ErpDrpConstants.RELEASE_TO_CODE_PREFIX）→ `DRP-TO-{lineId}` / `DRP-PO-{lineId}`。
 *
 * **releaseApproved 返回 null**（ErpDrpLineBizModel:43 `return null`）→ GraphQL `data` 为 null 但 `errors` 亦 null。
 * 故断言须经 `verifyState(plan)` + 逐行 `verifyState(line)` + `findFirst` 下游单据（mutation 返回值不可断言）。
 *
 * ORM 无 useWorkflow / 无 useApproval，纯 DIRECT @BizMutation 浏览器层可达。
 *
 * Phase 1 Explore 核实（见计划）：`__save` 直置 APPROVED plan + 多 APPROVED 行（TRANSFER/PURCHASE 混合），
 * 绕过 runDrp→approvePlan 引擎链精确控制 replenishmentType（0941-1 releaseLine 范式）。
 * 每行独立 materialId + Parameter 三元组隔离（避 drp-plan-engine/safety-stock 参数冲突）。
 *
 * 清理：下游单据（TransferOrder/PurOrder + 行，code=`DRP-TO-{lineId}` / `DRP-PO-{lineId}` 反查，非 cascade）
 *   + DrpLines + DrpParameters + DrpPlan + 测试物料。
 *   下游单据为 DRAFT 草稿，无凭证/库存产物，删除安全。
 */

const ORG_ID = 2;
const UOM_ID = 1; // PCS
const WH_DEST = 2; // WH-RAW（TRANSFER 目标仓库 = line.warehouseId）
const WH_SOURCE = 1; // WH-MAIN（TRANSFER 源仓库 = parameter.preferredSourceWarehouseId）
const SUPPLIER_ID = 3; // SUP-001（PURCHASE preferredSupplierId）
const APPROVED_QTY = 10;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

async function seedMaterial(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpMdMaterial',
    {
      code: uniq(`E2E-DRP-BAT-MAT-${tag}`),
      name: `E2E DRP Batch Material ${tag}`,
      materialType: 'GOODS',
      uoMId: UOM_ID,
      status: 'ACTIVE',
      costMethod: 'MOVING_AVERAGE',
      defaultWarehouseId: WH_DEST,
    },
    'id',
  );
}

async function seedPlan(page: import('@playwright/test').Page, status: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpDrpPlan',
    {
      code: uniq('E2E-DRP-BAT-PLAN'),
      planName: 'E2E DRP Batch Release Plan',
      periodFrom: '2026-07-01',
      periodTo: '2026-07-31',
      status,
      orgId: ORG_ID,
    },
    'id',
  );
}

interface LineOpts {
  planId: string | number;
  materialId: string | number;
  replenishmentType: string;
  status?: string;
  lineNo?: number;
}

async function seedLine(page: import('@playwright/test').Page, o: LineOpts): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpDrpLine',
    {
      planId: Number(o.planId),
      lineNo: o.lineNo ?? 10,
      materialId: Number(o.materialId),
      warehouseId: WH_DEST,
      replenishmentType: o.replenishmentType,
      approvedQty: APPROVED_QTY,
      status: o.status ?? 'APPROVED',
      orgId: ORG_ID,
    },
    'id',
  );
}

interface ParamOpts {
  materialId: string | number;
  preferredSourceWarehouseId?: number;
  preferredSupplierId?: number;
}

async function seedParameter(page: import('@playwright/test').Page, o: ParamOpts): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpDrpParameter',
    {
      warehouseId: WH_DEST,
      materialId: Number(o.materialId),
      safetyStock: 100,
      replenishmentMethod: 'LOT_FOR_LOT',
      orgId: ORG_ID,
      ...(o.preferredSourceWarehouseId != null ? { preferredSourceWarehouseId: o.preferredSourceWarehouseId } : {}),
      ...(o.preferredSupplierId != null ? { preferredSupplierId: o.preferredSupplierId } : {}),
    },
    'id',
  );
}

async function deleteTransferOrder(page: import('@playwright/test').Page, code: string): Promise<void> {
  const order = await findFirst<any>(page, 'ErpInvTransferOrder', eqFilter('code', code), 'id');
  if (order) {
    await deleteByFilter(page, 'ErpInvTransferOrderLine', eqFilter('transferId', Number(order.id)));
    await deleteById(page, 'ErpInvTransferOrder', order.id);
  }
}

async function deletePurchaseOrder(page: import('@playwright/test').Page, code: string): Promise<void> {
  const order = await findFirst<any>(page, 'ErpPurOrder', eqFilter('code', code), 'id');
  if (order) {
    await deleteByFilter(page, 'ErpPurOrderLine', eqFilter('orderId', Number(order.id)));
    await deleteById(page, 'ErpPurOrder', order.id);
  }
}

test.describe('drp ErpDrpLine releaseApproved batch release orchestration', () => {
  test('happy path: APPROVED plan + TRANSFER/PURCHASE mixed lines → all ORDERED + plan EXECUTED + downstream orders', async ({ page }) => {
    await loginAndNavigate(page, '/ErpDrpLine-main');

    const matTo = await seedMaterial(page, 'TO');
    const matPo = await seedMaterial(page, 'PO');
    const plan = await seedPlan(page, 'APPROVED');
    const paramTo = await seedParameter(page, { materialId: matTo.id, preferredSourceWarehouseId: WH_SOURCE });
    const paramPo = await seedParameter(page, { materialId: matPo.id, preferredSupplierId: SUPPLIER_ID });
    const lineTo = await seedLine(page, { planId: plan.id, materialId: matTo.id, replenishmentType: 'TRANSFER', lineNo: 10 });
    const linePo = await seedLine(page, { planId: plan.id, materialId: matPo.id, replenishmentType: 'PURCHASE', lineNo: 20 });

    try {
      // releaseApproved(planId)：批量释放全部 APPROVED 行（mutation 返回 null，断言 errors null 即成功）
      const { data, errors } = await callMutation(page, 'ErpDrpLine', 'releaseApproved', { planId: plan.id }, 'id');
      expect(errors, 'releaseApproved should not return GraphQL errors').toBeNull();
      expect(data, 'releaseApproved returns null (ErpDrpLineBizModel:43), state verified via __get').toBeNull();

      // 全行 ORDERED 断言（逐行 verifyState 独立查库）
      const lsTo = await verifyState(page, 'ErpDrpLine', lineTo.id, 'status orderBillType orderBillCode');
      expect(lsTo.status, 'TRANSFER line.status=ORDERED after batch release').toBe('ORDERED');
      expect(lsTo.orderBillType, 'TRANSFER line.orderBillType=ERP_INV_TRANSFER_ORDER').toBe('ERP_INV_TRANSFER_ORDER');
      expect(lsTo.orderBillCode, 'TRANSFER line.orderBillCode=DRP-TO-{lineId}').toBe(`DRP-TO-${lineTo.id}`);

      const lsPo = await verifyState(page, 'ErpDrpLine', linePo.id, 'status orderBillType orderBillCode');
      expect(lsPo.status, 'PURCHASE line.status=ORDERED after batch release').toBe('ORDERED');
      expect(lsPo.orderBillType, 'PURCHASE line.orderBillType=ERP_PUR_ORDER').toBe('ERP_PUR_ORDER');
      expect(lsPo.orderBillCode, 'PURCHASE line.orderBillCode=DRP-PO-{lineId}').toBe(`DRP-PO-${linePo.id}`);

      // 计划 EXECUTED 翻转断言（全部行 ORDERED 后 advancePlanToExecutedIfComplete）
      const planState = await verifyState(page, 'ErpDrpPlan', plan.id, 'status');
      expect(planState.status, 'plan.status=EXECUTED after all lines ORDERED').toBe('EXECUTED');

      // 下游单据创建断言（findFirst by code）
      const to = await findFirst<any>(page, 'ErpInvTransferOrder', eqFilter('code', `DRP-TO-${lineTo.id}`),
        'id code fromWarehouseId toWarehouseId docStatus');
      expect(to, 'ErpInvTransferOrder should be created for TRANSFER line').not.toBeNull();
      expect(Number(to!.fromWarehouseId), 'transferOrder.fromWarehouseId=preferredSourceWarehouseId').toBe(WH_SOURCE);
      expect(Number(to!.toWarehouseId), 'transferOrder.toWarehouseId=line.warehouseId').toBe(WH_DEST);

      const po = await findFirst<any>(page, 'ErpPurOrder', eqFilter('code', `DRP-PO-${linePo.id}`),
        'id code supplierId docStatus');
      expect(po, 'ErpPurOrder should be created for PURCHASE line').not.toBeNull();
      expect(Number(po!.supplierId), 'purOrder.supplierId=preferredSupplierId').toBe(SUPPLIER_ID);
    } finally {
      await deleteTransferOrder(page, `DRP-TO-${lineTo.id}`);
      await deletePurchaseOrder(page, `DRP-PO-${linePo.id}`);
      await deleteById(page, 'ErpDrpLine', lineTo.id);
      await deleteById(page, 'ErpDrpLine', linePo.id);
      await deleteById(page, 'ErpDrpParameter', paramTo.id);
      await deleteById(page, 'ErpDrpParameter', paramPo.id);
      await deleteById(page, 'ErpDrpPlan', plan.id);
      await deleteById(page, 'ErpMdMaterial', matTo.id);
      await deleteById(page, 'ErpMdMaterial', matPo.id);
    }
  });

  test('guard: plan with only SUGGESTED (non-APPROVED) lines → releaseApproved no-op (plan unchanged, no downstream)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpDrpLine-main');

    const matTo = await seedMaterial(page, 'G-TO');
    const plan = await seedPlan(page, 'APPROVED');
    const paramTo = await seedParameter(page, { materialId: matTo.id, preferredSourceWarehouseId: WH_SOURCE });
    // SUGGESTED 行（非 APPROVED）→ releaseApproved 过滤 status=APPROVED 时被跳过
    const lineTo = await seedLine(page, {
      planId: plan.id, materialId: matTo.id, replenishmentType: 'TRANSFER', status: 'SUGGESTED', lineNo: 10,
    });

    try {
      // releaseApproved：无 APPROVED 行 → released=0 空返回（不抛错），plan/line 不变，无下游
      const { data, errors } = await callMutation(page, 'ErpDrpLine', 'releaseApproved', { planId: plan.id }, 'id');
      expect(errors, 'releaseApproved with no APPROVED lines should not error (no-op)').toBeNull();

      // plan 保持 APPROVED（advancePlanToExecutedIfComplete 仅在 released>0 时调用）
      const planState = await verifyState(page, 'ErpDrpPlan', plan.id, 'status');
      expect(planState.status, 'plan.status should remain APPROVED (no APPROVED lines released)').toBe('APPROVED');

      // 行保持 SUGGESTED（未被释放）
      const ls = await verifyState(page, 'ErpDrpLine', lineTo.id, 'status orderBillType');
      expect(ls.status, 'line.status should remain SUGGESTED').toBe('SUGGESTED');
      expect(ls.orderBillType, 'line.orderBillType should remain null').toBeNull();

      // 无下游单据创建
      const to = await findFirst<any>(page, 'ErpInvTransferOrder', eqFilter('code', `DRP-TO-${lineTo.id}`), 'id');
      expect(to, 'no TransferOrder should be created for SUGGESTED-only plan').toBeNull();
    } finally {
      await deleteById(page, 'ErpDrpLine', lineTo.id);
      await deleteById(page, 'ErpDrpParameter', paramTo.id);
      await deleteById(page, 'ErpDrpPlan', plan.id);
      await deleteById(page, 'ErpMdMaterial', matTo.id);
    }
  });
});
