import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, findFirst, deleteByFilter, deleteById, eqFilter } from './_helper';

/**
 * drp ErpDrpLine 行释放跨域编排业务动作浏览器层 E2E（plan 2026-07-14-0941-1 Phase 3）。
 *
 * 验证 releaseLine 经 GraphQL /graphql 的全栈可达性 + TRANSFER/PURCHASE 双路径下游单据创建 + 行回写。
 *
 * 权威实现（ErpDrpLineBizModel.releaseLine:38 → DrpReleaseService.releaseLine:61，
 * 对齐 docs/design/drp/state-machine.md §场景 C / use-cases.md UC-DRP-03）：
 *   requireReleasable(lineId) 守卫：line.status == APPROVED，否则 ERR_DRP_LINE_NOT_SUGGESTED
 *     （已 ORDERED → ERR_DRP_LINE_ALREADY_ORDERED；其他状态 → ERR_DRP_LINE_NOT_SUGGESTED）。
 *   requireParameter(line)：按 (materialId, warehouseId=line.warehouseId, orgId?) 查 ErpDrpParameter，
 *     无匹配 → ERR_DRP_PARAMETER_MISSING。
 *   TRANSFER 路径：parameter.preferredSourceWarehouseId（或 line.sourceWarehouseId）非空，否则
 *     ERR_DRP_NO_SOURCE_WAREHOUSE。建 ErpInvTransferOrder（code=`DRP-TO-{lineId}`，
 *     fromWarehouseId=source, toWarehouseId=line.warehouseId, docStatus=DRAFT）+ Line。
 *   PURCHASE 路径：parameter.preferredSupplierId 非空，否则 ERR_DRP_NO_PREFERRED_SUPPLIER。
 *     建 ErpPurOrder（code=`DRP-PO-{lineId}`，supplierId=preferredSupplierId, docStatus=DRAFT）+ Line。
 *   回写：line.orderBillType (ERP_INV_TRANSFER_ORDER / ERP_PUR_ORDER) + orderBillCode + status=ORDERED。
 *   advancePlanToExecutedIfComplete：全部行 ORDERED/CANCELLED 后 plan → EXECUTED。
 *
 * ORM 无 useWorkflow / 无 useApproval，纯 DIRECT @BizMutation 浏览器层可达。
 *
 * Phase 3 Explore 裁决：DrpLine APPROVED 态前置 = `__save` 直置 APPROVED（非 runDrp→approvePlan 引擎链）。
 *   runDrp 产出的 SUGGESTED 行 replenishmentType 由 DrpEngine 内部逻辑决定，本 spec 须精确控制
 *   replenishmentType=TRANSFER/PURCHASE 分别验证两路径；__save 直置 APPROVED + replenishmentType 绕过
 *   引擎，确定性可控（对齐 drp-plan-engine / drp-safety-stock __save 直置 DRAFT 范式）。
 *
 * 隔离策略：建测试专用物料（避免与 drp-plan-engine (mat4,wh1,org2) / drp-safety-stock (mat4,wh2,org2)
 *   参数三元组冲突——requireParameter 用 setLimit(1) 返回首条匹配，若复用 seed mat4 + 已有参数行会命中
 *   他用例参数致非确定性）。新建物料 + 自建 Parameter 三元组唯一隔离。
 *
 * 种子引用：warehouse id=1（WH-MAIN，TRANSFER source）/ id=2（WH-RAW，TRANSFER dest）；
 *   partner id=3（SUP-001，PURCHASE preferredSupplierId）；org id=2；uom id=1（PCS）；currency id=1。
 *
 * 清理：下游单据（TransferOrder/Order + Line，code=`DRP-TO-{lineId}` / `DRP-PO-{lineId}` 反查，非 cascade）
 *   + DrpLine + DrpParameter + DrpPlan + 测试物料。
 *   下游单据为 DRAFT 草稿（docStatus=DRAFT, approveStatus=UNSUBMITTED），无凭证/库存产物，删除安全。
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
      code: uniq(`E2E-DRP-REL-MAT-${tag}`),
      name: `E2E DRP Release Material ${tag}`,
      materialType: 'GOODS',
      uoMId: UOM_ID,
      status: 'ACTIVE',
      costMethod: 'MOVING_AVERAGE',
      defaultWarehouseId: WH_DEST,
    },
    'id',
  );
}

async function seedPlan(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpDrpPlan',
    {
      code: uniq(`E2E-DRP-REL-PLAN-${tag}`),
      planName: `E2E DRP Release Plan ${tag}`,
      periodFrom: '2026-07-01',
      periodTo: '2026-07-31',
      status: 'APPROVED',
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
  sourceWarehouseId?: number;
}

async function seedLine(page: import('@playwright/test').Page, o: LineOpts): Promise<{ id: string; status: string }> {
  return createViaSave(
    page, 'ErpDrpLine',
    {
      planId: Number(o.planId),
      lineNo: 10,
      materialId: Number(o.materialId),
      warehouseId: WH_DEST,
      ...(o.sourceWarehouseId != null ? { sourceWarehouseId: o.sourceWarehouseId } : {}),
      replenishmentType: o.replenishmentType,
      approvedQty: APPROVED_QTY,
      status: o.status ?? 'APPROVED',
      orgId: ORG_ID,
    },
    'id status',
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

test.describe('drp ErpDrpLine releaseLine orchestration (TRANSFER / PURCHASE)', () => {
  test('TRANSFER path: APPROVED line + preferredSourceWarehouseId → ErpInvTransferOrder + line ORDERED', async ({ page }) => {
    await loginAndNavigate(page, '/ErpDrpLine-main');

    const material = await seedMaterial(page, 'TO');
    const plan = await seedPlan(page, 'TO');
    const param = await seedParameter(page, {
      materialId: material.id,
      preferredSourceWarehouseId: WH_SOURCE,
    });
    const line = await seedLine(page, {
      planId: plan.id,
      materialId: material.id,
      replenishmentType: 'TRANSFER',
    });

    try {
      // releaseLine：TRANSFER → 建 ErpInvTransferOrder
      await callMutationOk(page, 'ErpDrpLine', 'releaseLine', { lineId: line.id }, 'id');

      // 行回写断言
      const ls = await verifyState(page, 'ErpDrpLine', line.id, 'status orderBillType orderBillCode');
      expect(ls.status, 'line.status=ORDERED after release').toBe('ORDERED');
      expect(ls.orderBillType, 'line.orderBillType=ERP_INV_TRANSFER_ORDER').toBe('ERP_INV_TRANSFER_ORDER');
      expect(ls.orderBillCode, 'line.orderBillCode=DRP-TO-{lineId}').toBe(`DRP-TO-${line.id}`);

      // ErpInvTransferOrder 创建断言
      const expectedCode = `DRP-TO-${line.id}`;
      const to = await findFirst<any>(page, 'ErpInvTransferOrder', eqFilter('code', expectedCode),
        'id code fromWarehouseId toWarehouseId docStatus approveStatus');
      expect(to, 'ErpInvTransferOrder should be created').not.toBeNull();
      expect(Number(to!.fromWarehouseId), 'transferOrder.fromWarehouseId=preferredSourceWarehouseId').toBe(WH_SOURCE);
      expect(Number(to!.toWarehouseId), 'transferOrder.toWarehouseId=line.warehouseId').toBe(WH_DEST);
      expect(to!.docStatus, 'transferOrder.docStatus=DRAFT').toBe('DRAFT');
    } finally {
      // 清理：TransferOrder + Line + DrpLine + Parameter + Plan + Material
      await deleteTransferOrder(page, `DRP-TO-${line.id}`);
      await deleteById(page, 'ErpDrpLine', line.id);
      await deleteById(page, 'ErpDrpParameter', param.id);
      await deleteById(page, 'ErpDrpPlan', plan.id);
      await deleteById(page, 'ErpMdMaterial', material.id);
    }
  });

  test('PURCHASE path: APPROVED line + preferredSupplierId → ErpPurOrder + line ORDERED', async ({ page }) => {
    await loginAndNavigate(page, '/ErpDrpLine-main');

    const material = await seedMaterial(page, 'PO');
    const plan = await seedPlan(page, 'PO');
    const param = await seedParameter(page, {
      materialId: material.id,
      preferredSupplierId: SUPPLIER_ID,
    });
    const line = await seedLine(page, {
      planId: plan.id,
      materialId: material.id,
      replenishmentType: 'PURCHASE',
    });

    try {
      // releaseLine：PURCHASE → 建 ErpPurOrder
      await callMutationOk(page, 'ErpDrpLine', 'releaseLine', { lineId: line.id }, 'id');

      // 行回写断言
      const ls = await verifyState(page, 'ErpDrpLine', line.id, 'status orderBillType orderBillCode');
      expect(ls.status, 'line.status=ORDERED after release').toBe('ORDERED');
      expect(ls.orderBillType, 'line.orderBillType=ERP_PUR_ORDER').toBe('ERP_PUR_ORDER');
      expect(ls.orderBillCode, 'line.orderBillCode=DRP-PO-{lineId}').toBe(`DRP-PO-${line.id}`);

      // ErpPurOrder 创建断言
      const expectedCode = `DRP-PO-${line.id}`;
      const po = await findFirst<any>(page, 'ErpPurOrder', eqFilter('code', expectedCode),
        'id code supplierId docStatus approveStatus');
      expect(po, 'ErpPurOrder should be created').not.toBeNull();
      expect(Number(po!.supplierId), 'purOrder.supplierId=preferredSupplierId').toBe(SUPPLIER_ID);
      expect(po!.docStatus, 'purOrder.docStatus=DRAFT').toBe('DRAFT');
    } finally {
      await deletePurchaseOrder(page, `DRP-PO-${line.id}`);
      await deleteById(page, 'ErpDrpLine', line.id);
      await deleteById(page, 'ErpDrpParameter', param.id);
      await deleteById(page, 'ErpDrpPlan', plan.id);
      await deleteById(page, 'ErpMdMaterial', material.id);
    }
  });

  test('illegal guards: non-APPROVED line rejected; TRANSFER missing source warehouse; PURCHASE missing supplier', async ({ page }) => {
    await loginAndNavigate(page, '/ErpDrpLine-main');

    // (a) 非 APPROVED 行（SUGGESTED）→ ERR_DRP_LINE_NOT_SUGGESTED
    const material1 = await seedMaterial(page, 'G1');
    const plan1 = await seedPlan(page, 'G1');
    const param1 = await seedParameter(page, { materialId: material1.id, preferredSourceWarehouseId: WH_SOURCE });
    const line1 = await seedLine(page, {
      planId: plan1.id, materialId: material1.id, replenishmentType: 'TRANSFER', status: 'SUGGESTED',
    });

    try {
      const rej1 = await callMutation(page, 'ErpDrpLine', 'releaseLine', { lineId: line1.id }, 'id');
      expect(rej1.errors, 'releaseLine on SUGGESTED line should be rejected').toBeTruthy();
      expect(JSON.stringify(rej1.errors), 'reject should carry not-suggested token').toContain('仅 APPROVED 行可释放');
    } finally {
      await deleteById(page, 'ErpDrpLine', line1.id);
      await deleteById(page, 'ErpDrpParameter', param1.id);
      await deleteById(page, 'ErpDrpPlan', plan1.id);
      await deleteById(page, 'ErpMdMaterial', material1.id);
    }

    // (b) TRANSFER 缺 sourceWarehouseId（parameter.preferredSourceWarehouseId=null + line.sourceWarehouseId=null）
    //    → ERR_DRP_NO_SOURCE_WAREHOUSE
    const material2 = await seedMaterial(page, 'G2');
    const plan2 = await seedPlan(page, 'G2');
    const param2 = await seedParameter(page, { materialId: material2.id }); // 无 preferredSourceWarehouseId
    const line2 = await seedLine(page, {
      planId: plan2.id, materialId: material2.id, replenishmentType: 'TRANSFER',
      // 无 sourceWarehouseId
    });

    try {
      const rej2 = await callMutation(page, 'ErpDrpLine', 'releaseLine', { lineId: line2.id }, 'id');
      expect(rej2.errors, 'releaseLine TRANSFER without source warehouse should be rejected').toBeTruthy();
      expect(JSON.stringify(rej2.errors), 'reject should carry no-source-warehouse token').toContain('首选调出仓库');
    } finally {
      await deleteById(page, 'ErpDrpLine', line2.id);
      await deleteById(page, 'ErpDrpParameter', param2.id);
      await deleteById(page, 'ErpDrpPlan', plan2.id);
      await deleteById(page, 'ErpMdMaterial', material2.id);
    }

    // (c) PURCHASE 缺 preferredSupplierId → ERR_DRP_NO_PREFERRED_SUPPLIER
    const material3 = await seedMaterial(page, 'G3');
    const plan3 = await seedPlan(page, 'G3');
    const param3 = await seedParameter(page, { materialId: material3.id }); // 无 preferredSupplierId
    const line3 = await seedLine(page, {
      planId: plan3.id, materialId: material3.id, replenishmentType: 'PURCHASE',
    });

    try {
      const rej3 = await callMutation(page, 'ErpDrpLine', 'releaseLine', { lineId: line3.id }, 'id');
      expect(rej3.errors, 'releaseLine PURCHASE without preferred supplier should be rejected').toBeTruthy();
      expect(JSON.stringify(rej3.errors), 'reject should carry no-preferred-supplier token').toContain('首选供应商');
    } finally {
      await deleteById(page, 'ErpDrpLine', line3.id);
      await deleteById(page, 'ErpDrpParameter', param3.id);
      await deleteById(page, 'ErpDrpPlan', plan3.id);
      await deleteById(page, 'ErpMdMaterial', material3.id);
    }
  });
});
