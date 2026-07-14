import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById, deleteByFilter, eqFilter, findFirst } from './_helper';

/**
 * b2b ErpB2bAsn ASN→PO 匹配 + 跨域建收业务动作浏览器层 E2E（plan 2026-07-14-0941-2 Phase 1）。
 *
 * 验证 ASN 跨域编排 DIRECT @BizMutation（matchPurchaseOrder / retryMatch / createReceiveFromAsn）经 GraphQL
 * /graphql 的全栈可达性 + ASN 状态翻转（RECEIVED→MATCHED→RECEIVED_TO_STOCK）+ 跨域读 PO 匹配 + 跨域建
 * ErpPurReceive 草稿 + 非法守卫。
 *
 * 权威编排（ErpB2bAsnBizModel，对齐 docs/design/b2b/asn-processing.md）：
 *   matchPurchaseOrder(asnId)：RECEIVED 态 → 按 asn.relatedBillCode（PO code）经 findPurchaseOrder 跨域
 *     读 ErpPurOrder（非 partnerId 匹配）→ 逐行按 materialId 匹配 + shippedQty vs PO line remaining 数量
 *     校验 → MATCHED。PO 不存在 → 保留 RECEIVED（日志 info）。PO 已关闭/取消 → 标记 remark 保留 RECEIVED。
 *     非 RECEIVED 态 → 抛 ERR_B2B_ASN_ILLEGAL_TRANSITION。
 *   createReceiveFromAsn(asnId)：config-gated erp-b2b.asn-auto-create-receive（默认 false，webServer JVM arg
 *     追加 =true 启用）。MATCHED 态 → 跨域建 ErpPurReceive 草稿（code=RCV-FROM-ASN-{asnCode}，orderId=PO.id，
 *     supplierId/warehouseId/currencyId 据 PO 回填，docStatus=UNSUBMITTED，approveStatus=UNSUBMITTED，
 *     receiveStatus=NOT_RECEIVED，**仅头无行**）→ ASN RECEIVED_TO_STOCK。
 *   retryMatch(asnId)：幂等——已 MATCHED/RECEIVED_TO_STOCK 直接返回；否则回到 RECEIVED 后重新匹配。
 *
 * Phase 1 Explore Decision（前置路径裁定）：
 *   - ASN RECEIVED 态前置经 `__save` 直置 status=RECEIVED（绕过 handleInboundWebhook EDI 文档驱动编排，
 *     同 0508-1 b2b-edi-doc 经 __save 置入口范式）。relatedBillType=PO_ORDER + relatedBillCode=PO.code
 *     使 matchPurchaseOrder 经 findPurchaseOrder(code) 命中。
 *   - matchPurchaseOrder 匹配键为 asn.relatedBillCode（PO code），非 partnerId；逐行按 materialId 匹配
 *     （findMatchingPoLine）。故 AsnLine.materialId 须与 PO line materialId 一致。
 *   - createReceiveFromAsn config-gate erp-b2b.asn-auto-create-receive 默认 false，须 webServer JVM arg
 *     =true 启用（playwright.config.ts 已追加），否则方法返回 null 跳过。
 *   - PO 无审批状态守卫（matchPurchaseOrder 仅检查 isPoClosedOrCancelled），故经 __save 直置
 *     approveStatus=APPROVED + docStatus=ACTIVE 作匹配前置（非 runP2pChain 避跨域产物污染）。
 *
 * 自包含 setup：建 ErpPurOrder（APPROVED via __save）+ ErpPurOrderLine（materialId=种子 MAT_1）+ ASN
 *   （RECEIVED, relatedBillCode=PO.code）+ AsnLine（materialId 匹配 PO line materialId）。
 * 清理：删 ErpPurReceive（createReceiveFromAsn 产物）+ AsnLine + ASN + ErpPurOrderLine + ErpPurOrder。
 */

const BDATE = '2026-07-10';
const ASN_QTY = 10;
const PO_QTY = 10;

async function seedPurchaseOrder(page: import('@playwright/test').Page, tag: string): Promise<{ id: string; code: string }> {
  const code = `E2E-B2B-PO-${tag}-${Date.now()}`;
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

async function seedPoLine(page: import('@playwright/test').Page, orderId: string | number, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpPurOrderLine',
    { orderId, lineNo: 1, materialId: 1, uoMId: 1, quantity: PO_QTY, unitPrice: 5, amount: 50 },
    'id',
  );
}

async function seedAsn(page: import('@playwright/test').Page, poCode: string, tag: string, status = 'RECEIVED'): Promise<{ id: string; code: string; status: string }> {
  const code = `E2E-B2B-ASN-${tag}-${Date.now()}`;
  return createViaSave(
    page, 'ErpB2bAsn',
    {
      code, orgId: 2, partnerId: 3,
      relatedBillType: 'PO_ORDER', relatedBillCode: poCode,
      status, shipmentDate: BDATE, businessDate: BDATE,
    },
    'id code status',
  );
}

async function seedAsnLine(page: import('@playwright/test').Page, asnId: string | number, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpB2bAsnLine',
    { asnId, lineNo: 1, materialId: 1, shippedQty: ASN_QTY, quantity: ASN_QTY },
    'id',
  );
}

async function cleanupAsnChain(page: import('@playwright/test').Page, asnCode: string, asnId?: string | number, poId?: string | number): Promise<void> {
  if (asnCode) {
    const rcv = await findFirst<any>(page, 'ErpPurReceive', eqFilter('code', `RCV-FROM-ASN-${asnCode}`), 'id');
    if (rcv) await deleteById(page, 'ErpPurReceive', rcv.id);
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

test.describe('b2b ErpB2bAsn ASN→PO match + createReceiveFromAsn orchestration', () => {
  test('matchPurchaseOrder: RECEIVED asn + matching PO → MATCHED (cross-domain read PO by relatedBillCode)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpB2bAsn-main');

    const po = await seedPurchaseOrder(page, 'match');
    await seedPoLine(page, po.id, 'match');
    const asn = await seedAsn(page, po.code, 'match');
    await seedAsnLine(page, asn.id, 'match');
    expect(asn.status, 'precondition status=RECEIVED').toBe('RECEIVED');

    await callMutationOk(page, 'ErpB2bAsn', 'matchPurchaseOrder', { asnId: asn.id }, 'id status');
    const v = await verifyState(page, 'ErpB2bAsn', asn.id, 'status');
    expect(v.status, 'after matchPurchaseOrder status=MATCHED').toBe('MATCHED');

    await cleanupAsnChain(page, asn.code, asn.id, po.id);
  });

  test('createReceiveFromAsn: MATCHED asn → ErpPurReceive draft created + ASN RECEIVED_TO_STOCK', async ({ page }) => {
    await loginAndNavigate(page, '/ErpB2bAsn-main');

    const po = await seedPurchaseOrder(page, 'recv');
    await seedPoLine(page, po.id, 'recv');
    const asn = await seedAsn(page, po.code, 'recv');
    await seedAsnLine(page, asn.id, 'recv');

    await callMutationOk(page, 'ErpB2bAsn', 'matchPurchaseOrder', { asnId: asn.id }, 'id status');

    // createReceiveFromAsn：config-gated erp-b2b.asn-auto-create-receive=true（webServer JVM arg）
    await callMutationOk(page, 'ErpB2bAsn', 'createReceiveFromAsn', { asnId: asn.id }, 'id status');

    const v = await verifyState(page, 'ErpB2bAsn', asn.id, 'status');
    expect(v.status, 'after createReceiveFromAsn status=RECEIVED_TO_STOCK').toBe('RECEIVED_TO_STOCK');

    // 跨域建 ErpPurReceive 草稿断言（仅头无行）
    const rcv = await findFirst<any>(
      page, 'ErpPurReceive',
      eqFilter('code', `RCV-FROM-ASN-${asn.code}`),
      'id code orderId supplierId warehouseId currencyId docStatus approveStatus receiveStatus',
    );
    expect(rcv, 'ErpPurReceive draft should be created').toBeTruthy();
    expect(rcv.code, 'receive code = RCV-FROM-ASN-{asnCode}').toBe(`RCV-FROM-ASN-${asn.code}`);
    expect(String(rcv.orderId), 'receive.orderId = PO.id').toBe(String(po.id));
    expect(String(rcv.supplierId), 'receive.supplierId from PO').toBe('3');
    expect(String(rcv.warehouseId), 'receive.warehouseId from PO').toBe('2');
    expect(String(rcv.currencyId), 'receive.currencyId from PO').toBe('1');
    expect(rcv.docStatus, 'receive.docStatus=UNSUBMITTED').toBe('UNSUBMITTED');
    expect(rcv.approveStatus, 'receive.approveStatus=UNSUBMITTED').toBe('UNSUBMITTED');
    expect(rcv.receiveStatus, 'receive.receiveStatus=NOT_RECEIVED').toBe('NOT_RECEIVED');

    await cleanupAsnChain(page, asn.code, asn.id, po.id);
  });

  test('illegal guards: non-matching PO keeps RECEIVED; non-RECEIVED asn rejects (ERR_B2B_ASN_ILLEGAL_TRANSITION)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpB2bAsn-main');

    // 无匹配 PO（relatedBillCode 指向不存在 PO）→ matchPurchaseOrder 保持 RECEIVED 不迁移
    const asnNoPo = await seedAsn(page, `NONEXISTENT-${Date.now()}`, 'nopo');
    await seedAsnLine(page, asnNoPo.id, 'nopo');
    expect(asnNoPo.status, 'precondition status=RECEIVED').toBe('RECEIVED');

    await callMutationOk(page, 'ErpB2bAsn', 'matchPurchaseOrder', { asnId: asnNoPo.id }, 'id status');
    const v1 = await verifyState(page, 'ErpB2bAsn', asnNoPo.id, 'status');
    expect(v1.status, 'no matching PO → stays RECEIVED').toBe('RECEIVED');

    // 非 RECEIVED 态 ASN matchPurchaseOrder 抛 ERR_B2B_ASN_ILLEGAL_TRANSITION
    const asnMatched = await seedAsn(page, `NONEXISTENT-MATCHED-${Date.now()}`, 'gd', 'MATCHED');
    const rej = await callMutation(page, 'ErpB2bAsn', 'matchPurchaseOrder', { asnId: asnMatched.id }, 'id');
    expect(rej.errors, 'matchPurchaseOrder from MATCHED should be rejected').toBeTruthy();
    expect(JSON.stringify(rej.errors), 'reject should carry illegal-transition token').toContain('不允许执行该操作');
    const v2 = await verifyState(page, 'ErpB2bAsn', asnMatched.id, 'status');
    expect(v2.status, 'failed matchPurchaseOrder should not change status').toBe('MATCHED');

    // retryMatch 幂等：已 MATCHED 直接返回（不抛错、不迁移）
    await callMutationOk(page, 'ErpB2bAsn', 'retryMatch', { asnId: asnMatched.id }, 'id status');
    const v3 = await verifyState(page, 'ErpB2bAsn', asnMatched.id, 'status');
    expect(v3.status, 'retryMatch on MATCHED is idempotent').toBe('MATCHED');

    await cleanupAsnChain(page, asnNoPo.code, asnNoPo.id);
    await cleanupAsnChain(page, asnMatched.code, asnMatched.id);
  });
});
