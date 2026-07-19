import { test, expect, loginAndNavigate, createViaSave, callMutationOk, verifyState, deleteById, deleteByFilter, eqFilter, findFirst, findItems } from './_helper';

/**
 * b2b ErpB2bAsn ASN→采购入库行级回填浏览器层 E2E（plan 2026-07-19-0849-1 Phase 3）。
 *
 * 验证 createReceiveFromAsn 行级回填扩展（plan 2026-07-19-0849-1 Phase 2 落地）：
 *   MATCHED 态 ASN → createReceiveFromAsn 不仅是仅建头（0941-2 既有覆盖），而是 iterate AsnLine
 *   → 为每条 AsnLine 创建 ErpPurReceiveLine + 字段映射（materialId/uoMId/quantity/unitPrice/amount/warehouseId/lineNo）
 *   → 批量持久化。ASN RECEIVED_TO_STOCK。
 *
 * 权威字段映射规则（ErpB2bAsnBizModel.fillReceiveLinesFromAsn，plan Phase 1 Decision (a)-(f)）：
 *   - materialId：透传 AsnLine.materialId（null → 抛 ERR_B2B_ASN_LINE_MATERIAL_REQUIRED 守卫，Decision (f)①）。
 *   - uoMId：materialId → ErpMdMaterial.uoMId 反查（mandatory 主数据必然有值，Decision (f)①）。
 *   - quantity：AsnLine.shippedQty 优先（实际发货），fallback AsnLine.quantity。
 *   - lineNo：透传 AsnLine.lineNo（Decision (c)①）。
 *   - unitPrice/taxRate/orderLineId：经 materialId 反查 PO line 透传（Decision (a)①）。
 *   - amount：派生 unitPrice × quantity（HALF_UP scale=4），任一为 null 则 null（Decision (b)①）。
 *   - warehouseId：复用 receive.warehouseId（即 po.warehouseId，Decision (d)②）。
 *
 * Phase 3 决议（plan l.132）：playwright.config.ts webServer args 全局生效无法 per-spec toggle →
 *   - 本 spec 假设 erp-b2b.asn-auto-create-receive=true（playwright.config.ts webServer JVM arg 已追加）
 *   - config 关闭路径仅 JUnit 覆盖（Phase 2 testCreateReceiveFromAsnConfigGateDisabled）
 *
 * 自包含 setup（同 0941-2 范式）：建 ErpPurOrder（APPROVED via __save）+ ErpPurOrderLine（≥2，materialId 各异）
 *   + ASN（MATCHED via __save，绕过 matchPurchaseOrder）+ AsnLine（materialId 匹配 PO line materialId）。
 * 清理：删 ErpPurReceive（cascade-delete 自动清 ReceiveLine）+ AsnLine + ASN + ErpPurOrderLine + ErpPurOrder。
 */

const BDATE = '2026-07-10';
const UOM_ID = 1;
const MAT_A = 1; // MAT-001 种子物料，uoMId=1
const MAT_B = 2; // MAT-002 种子物料，uoMId=1

async function seedPurchaseOrder(page: import('@playwright/test').Page, tag: string): Promise<{ id: string; code: string }> {
  const code = `E2E-B2B-PO-LL-${tag}-${Date.now()}`;
  const po = await createViaSave(
    page, 'ErpPurOrder',
    {
      code, orgId: 2, supplierId: 3, warehouseId: 2,
      businessDate: BDATE, currencyId: 1, exchangeRate: 1,
      docStatus: 'ACTIVE', approveStatus: 'APPROVED', receiveStatus: 'UNRECEIVED',
    },
    'id code',
  );
  return po;
}

async function seedPoLine(
  page: import('@playwright/test').Page,
  orderId: string | number,
  lineNo: number,
  materialId: number,
  unitPrice: number,
  quantity: number,
): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpPurOrderLine',
    { orderId, lineNo, materialId, uoMId: UOM_ID, quantity, unitPrice, amount: unitPrice * quantity },
    'id',
  );
}

async function seedMatchedAsn(page: import('@playwright/test').Page, poCode: string, tag: string): Promise<{ id: string; code: string }> {
  const code = `E2E-B2B-ASN-LL-${tag}-${Date.now()}`;
  return createViaSave(
    page, 'ErpB2bAsn',
    {
      code, orgId: 2, partnerId: 3,
      relatedBillType: 'PO_ORDER', relatedBillCode: poCode,
      status: 'MATCHED', shipmentDate: BDATE, businessDate: BDATE,
    },
    'id code',
  );
}

async function seedAsnLine(
  page: import('@playwright/test').Page,
  asnId: string | number,
  lineNo: number,
  materialId: number,
  shippedQty: number,
): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpB2bAsnLine',
    { asnId, lineNo, materialId, shippedQty, quantity: shippedQty },
    'id',
  );
}

async function cleanupChain(
  page: import('@playwright/test').Page,
  asnCode: string | null,
  asnId?: string | number,
  poId?: string | number,
): Promise<void> {
  if (asnCode) {
    const rcv = await findFirst<any>(page, 'ErpPurReceive', eqFilter('code', `RCV-FROM-ASN-${asnCode}`), 'id');
    if (rcv) await deleteById(page, 'ErpPurReceive', rcv.id); // cascade-delete 自动清 ErpPurReceiveLine
  }
  if (asnId) {
    await deleteByFilter(page, 'ErpB2bAsnLine', eqFilter('asnId', Number(asnId)));
    await deleteById(page, 'ErpB2bAsn', asnId);
  }
  if (poId) {
    await deleteByFilter(page, 'ErpPurOrderLine', eqFilter('orderId', Number(poId)));
    await deleteById(page, 'ErpPurOrder', poId);
  }
}

test.describe('b2b ErpB2bAsn createReceiveFromAsn line-level receive fill', () => {
  test('(1) multi-line mapping: 2 AsnLine → 2 ReceiveLine + field-exact assertions + ASN RECEIVED_TO_STOCK', async ({ page }) => {
    await loginAndNavigate(page, '/ErpB2bAsn-main');

    const po = await seedPurchaseOrder(page, 'multi');
    // 多行 PO：MAT-001 unitPrice=5 / MAT-002 unitPrice=12
    await seedPoLine(page, po.id, 1, MAT_A, 5, 20);
    await seedPoLine(page, po.id, 2, MAT_B, 12, 10);

    const asn = await seedMatchedAsn(page, po.code, 'multi');
    // 2 AsnLine 对应 PO line materialId（materialId 各异）
    await seedAsnLine(page, asn.id, 1, MAT_A, 15); // shippedQty=15
    await seedAsnLine(page, asn.id, 2, MAT_B, 8);  // shippedQty=8

    // createReceiveFromAsn（config-gated=true 已在 playwright.config.ts 启用）
    await callMutationOk(page, 'ErpB2bAsn', 'createReceiveFromAsn', { asnId: asn.id }, 'id status');

    // ASN → RECEIVED_TO_STOCK（0941-2 既有断言）
    const v = await verifyState(page, 'ErpB2bAsn', asn.id, 'status');
    expect(v.status, 'ASN status=RECEIVED_TO_STOCK').toBe('RECEIVED_TO_STOCK');

    // Receive 头断言（对齐 0941-2 范式）
    const rcv = await findFirst<any>(
      page, 'ErpPurReceive',
      eqFilter('code', `RCV-FROM-ASN-${asn.code}`),
      'id code orderId supplierId warehouseId currencyId',
    );
    expect(rcv, 'ErpPurReceive draft should be created').toBeTruthy();
    expect(rcv.code).toBe(`RCV-FROM-ASN-${asn.code}`);
    expect(String(rcv.orderId)).toBe(String(po.id));
    expect(String(rcv.supplierId)).toBe('3');
    expect(String(rcv.warehouseId)).toBe('2');

    // 行级回填断言（本 plan 核心扩展）：行数 == AsnLine 行数 + 字段精确数值断言
    const lines = await findItems<any>(
      page, 'ErpPurReceiveLine',
      eqFilter('receiveId', Number(rcv.id)),
      'id lineNo materialId uoMId quantity unitPrice amount warehouseId orderLineId',
    );
    expect(lines.length, '2 AsnLine → 2 ReceiveLine（行级回填完整）').toBe(2);

    // 逐行断言（按 lineNo 排序后核对）
    const sorted = lines.sort((a, b) => a.lineNo - b.lineNo);
    const l1 = sorted[0];
    expect(Number(l1.lineNo)).toBe(1);
    expect(Number(l1.materialId)).toBe(MAT_A, 'ReceiveLine.materialId 透传 AsnLine.materialId');
    expect(Number(l1.uoMId)).toBe(UOM_ID, 'ReceiveLine.uoMId 反查 ErpMdMaterial.uoMId');
    expect(Number(l1.quantity)).toBe(15, 'ReceiveLine.quantity=AsnLine.shippedQty');
    expect(Number(l1.unitPrice)).toBe(5, 'ReceiveLine.unitPrice 反查 PO line');
    expect(Number(l1.amount)).toBe(75, 'ReceiveLine.amount=unitPrice×qty=5×15=75 HALF_UP scale=4');
    expect(Number(l1.warehouseId)).toBe(2, 'ReceiveLine.warehouseId=receive.warehouseId');
    expect(l1.orderLineId, 'ReceiveLine.orderLineId 反查 PO line').toBeTruthy();

    const l2 = sorted[1];
    expect(Number(l2.lineNo)).toBe(2);
    expect(Number(l2.materialId)).toBe(MAT_B);
    expect(Number(l2.uoMId)).toBe(UOM_ID, 'MAT-002.uoMId 反查');
    expect(Number(l2.quantity)).toBe(8);
    expect(Number(l2.unitPrice)).toBe(12);
    expect(Number(l2.amount)).toBe(96, 'amount=12×8=96');

    await cleanupChain(page, asn.code, asn.id, po.id);
  });

  test('(2) empty AsnLine boundary: 0 lines → Receive header still created + 0 ReceiveLine (Decision (e)①)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpB2bAsn-main');

    const po = await seedPurchaseOrder(page, 'empty');
    // 不建 PO line（与 0941-2 默认场景一致）
    const asn = await seedMatchedAsn(page, po.code, 'empty');
    // 不建 AsnLine（0 行边界）

    await callMutationOk(page, 'ErpB2bAsn', 'createReceiveFromAsn', { asnId: asn.id }, 'id status');

    // ASN → RECEIVED_TO_STOCK：空白 AsnLine 合法（Decision (e)① Receive 头仍创建）
    const v = await verifyState(page, 'ErpB2bAsn', asn.id, 'status');
    expect(v.status, '空白 AsnLine：ASN RECEIVED_TO_STOCK').toBe('RECEIVED_TO_STOCK');

    const rcv = await findFirst<any>(
      page, 'ErpPurReceive',
      eqFilter('code', `RCV-FROM-ASN-${asn.code}`),
      'id',
    );
    expect(rcv, 'Receive 头创建不阻塞').toBeTruthy();

    // 0 ReceiveLine 断言（对照 (1) 多行映射）
    const lines = await findItems<any>(
      page, 'ErpPurReceiveLine',
      eqFilter('receiveId', Number(rcv.id)),
      'id',
    );
    expect(lines.length, '0 AsnLine → 0 ReceiveLine').toBe(0);

    await cleanupChain(page, asn.code, asn.id, po.id);
  });
});
