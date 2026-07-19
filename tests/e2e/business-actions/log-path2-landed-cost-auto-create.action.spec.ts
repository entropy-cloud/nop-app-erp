import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  verifyState,
  eqFilter,
  deleteById,
  findFirst,
  findItems,
} from './_helper';

/**
 * logistics ErpLogShipment DELIVERED path-2 采购运费→到岸成本自动创建浏览器层 E2E
 * （plan 2026-07-19-0849-2，承接 0941-2 + 1005-1 Deferred RELEASED）。
 *
 * 验证 path-2 完整链路（PURCHASE_RECEIPT DELIVERED → generateFreightLandedCost → DRAFT
 * ErpInvLandedCost）经 GraphQL /graphql 的全栈可达性，承接 2026-07-11-2329-1 后端落地 +
 * TestErpLogPath2LandedCost 单测覆盖。
 *
 * 权威实现（ErpLogShipmentBizModel.handlePurchaseReceiptDelivered:213-247）：
 *   config-gated erp-log.path2-landed-cost-auto-create（默认 false，webServer JVM arg
 *   =true 启用）：
 *     - 开启 + freightAmount > 0 → 调 IErpInvLandedCostBiz.generateFreightLandedCost(
 *       shipment.relatedBillCode, freightAmount, freightCurrencyId, null, ctx)
 *       → 创建 DRAFT ErpInvLandedCost（FREIGHT 费用行，apPartnerId=receive.supplierId）
 *       → publishDeliveredEvent + mark SETTLED。
 *     - 开启 + freightAmount ≤ 0/null → mark SETTLED（无可分摊运费）。
 *     - 关闭 → publishDeliveredEvent + mark SETTLED（向后兼容，本 spec 不覆盖，由后端单测验证）。
 *   委派链：ErpInvLandedCostBizModel.generateFreightLandedCost:52 →
 *     ErpInvLandedCostProcessor.generateFreightLandedCost:140 → loadReceiveByCode +
 *     validateNoDraftExists + createLandedCostHead(DRAFT+UNSUBMITTED, totalCostAmount=
 *     freightAmount, allocationMethod=BY_AMOUNT) + createFreightLine(FREIGHT, amount=
 *     freightAmount, apPartnerId=receive.supplierId)。
 *
 * Phase 1 Decisions（本计划落地）：
 *   - (a) ErpPurReceive 前置：直 __save DRAFT Receive（docStatus=DRAFT [erp-pur/doc-status 仅
 *     DRAFT/ACTIVE/CANCELLED] + approveStatus=UNSUBMITTED [wf/approve-status] + receiveStatus=
 *     UNRECEIVED [erp-pur/receive-status 仅 UNRECEIVED/PARTIAL/RECEIVED]），无 PO/入库触发污染
 *     共享 DB。backend loadReceiveByCode 仅 resolve code → id（不依赖 approve/入库状态）。
 *   - (b) Shipment setup：relatedBillType=PURCHASE_RECEIPT + relatedBillCode=Receive.code
 *     + freightAmount > 0 + freightCurrencyId（本位币 CNY=1）+ carrierCode（mock 网关）
 *     + status=DRAFT → advise → completeShipment（DISPATCHED + trackingNo=MOCK-{code}）。
 *   - (c) webhook payload：复用 0941-2 范式 {"trackingNo":"...","eventType":"DELIVERED",
 *     "signedBy":"E2E"} + signature 'skip'。
 *   - (d) ErpInvLandedCost 反查：GraphQL findFirst ErpInvLandedCost(filter: eqFilter(
 *     'receiveId', receive.id))，receiveId 是 Long FK 列（GraphQL schema 层暴露），
 *     对齐 backend TestErpLogPath2LandedCost.java:224 范式。ErpInvLandedCostLine 反查经
 *     eqFilter('landedCostId', landedCost.id)。
 *   - (e) cleanup：删 ErpInvLandedCostLine + ErpInvLandedCost（DRAFT path-2 产物无 GL 凭证）
 *     + Shipment + Carrier + ErpPurReceive（UNSUBMITTED 前置）。不触 SalInvoice/SalDelivery
 *     （path-2 不动 sales 域，与 path-1 cleanup 范式不同）。无凭证清理（DRAFT 未 approve）。
 *
 * 字段集（module-inventory/model/app-erp-inventory.orm.xml:1310-1376）：
 *   - ErpInvLandedCost 头：code / receiveId（Long FK，非 receiveCode）/ docStatus +
 *     approveStatus（拆分字段，非单 status）/ totalCostAmount（非 totalAmount）/
 *     currencyId / allocationMethod / supplierId。
 *   - ErpInvLandedCostLine：landedCostId / lineNo / costElement（非 costType，FREIGHT）/
 *     amount / apPartnerId。
 */

const GATEWAY_ID_MOCK = 'mock';
const FREIGHT_AMOUNT = 100;
const ORG = 2;
const SUPPLIER = 3; // SUP-001
const WH = 2;       // WH-RAW
const CURRENCY = 1; // CNY 本位币
const BDATE = '2026-07-19';

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

async function seedCarrier(page: import('@playwright/test').Page, tag: string): Promise<{ id: string; code: string }> {
  const code = uniq(`E2E-P2-CAR-${tag}`);
  return createViaSave(
    page, 'ErpLogCarrier',
    {
      code, carrierName: `E2E Path2 Carrier ${tag}`,
      carrierType: 'EXPRESS', gatewayId: GATEWAY_ID_MOCK, isActive: 1,
    },
    'id code',
  );
}

async function seedReceive(page: import('@playwright/test').Page, tag: string): Promise<{ id: string; code: string }> {
  const code = uniq(`E2E-P2-RCV-${tag}`);
  // docStatus=DRAFT (erp-pur/doc-status 仅 DRAFT/ACTIVE/CANCELLED)；receiveStatus=UNRECEIVED
  // (erp-pur/receive-status 仅 UNRECEIVED/PARTIAL/RECEIVED)；approveStatus=UNSUBMITTED (wf/approve-status)
  // 轻量前置：仅用作 shipment.relatedBillCode + backend loadReceiveByCode 解析（不依赖 approve/入库状态）
  return createViaSave(
    page, 'ErpPurReceive',
    {
      code, orgId: ORG, supplierId: SUPPLIER, warehouseId: WH,
      businessDate: BDATE, currencyId: CURRENCY, exchangeRate: 1,
      docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', receiveStatus: 'UNRECEIVED', posted: false,
    },
    'id code',
  );
}

async function seedShipment(
  page: import('@playwright/test').Page,
  carrierId: string | number,
  receiveCode: string,
  tag: string,
  freightAmount: number | null,
): Promise<{ id: string; code: string; status: string }> {
  const code = uniq(`E2E-P2-SHP-${tag}`);
  return createViaSave(
    page, 'ErpLogShipment',
    {
      code, orgId: ORG, carrierId,
      relatedBillType: 'PURCHASE_RECEIPT', relatedBillCode: receiveCode,
      ...(freightAmount === null ? {} : { freightAmount }),
      freightCurrencyId: CURRENCY, freightTerms: 'PREPAID',
      freightSettlementStatus: 'PENDING',
      status: 'DRAFT', businessDate: BDATE,
    },
    'id code status',
  );
}

async function findLandedCostByReceiveId(
  page: import('@playwright/test').Page,
  receiveId: string | number,
): Promise<{ id: string; code: string; docStatus: string; approveStatus: string; totalCostAmount: string | number; currencyId: string | number; supplierId: string | number; allocationMethod: string } | null> {
  return findFirst(
    page, 'ErpInvLandedCost',
    eqFilter('receiveId', Number(receiveId)),
    'id code docStatus approveStatus totalCostAmount currencyId supplierId allocationMethod',
  );
}

async function findFreightLine(
  page: import('@playwright/test').Page,
  landedCostId: string | number,
): Promise<{ id: string; costElement: string; amount: string | number; apPartnerId: string | number } | null> {
  return findFirst(
    page, 'ErpInvLandedCostLine',
    eqFilter('landedCostId', Number(landedCostId)),
    'id costElement amount apPartnerId',
  );
}

async function cleanupPath2(
  page: import('@playwright/test').Page,
  ctx: { landedCostId?: string | number; shipmentId?: string | number; carrierId?: string | number; receiveId?: string | number },
): Promise<void> {
  if (ctx.landedCostId != null) {
    await findItems(page, 'ErpInvLandedCostLine', eqFilter('landedCostId', Number(ctx.landedCostId)), 'id')
      .then((lines) => Promise.all(lines.map((l) => deleteById(page, 'ErpInvLandedCostLine', l.id))));
    await deleteById(page, 'ErpInvLandedCost', ctx.landedCostId);
  }
  if (ctx.shipmentId) await deleteById(page, 'ErpLogShipment', ctx.shipmentId);
  if (ctx.carrierId) await deleteById(page, 'ErpLogCarrier', ctx.carrierId);
  if (ctx.receiveId) await deleteById(page, 'ErpPurReceive', ctx.receiveId);
}

test.describe('logistics ErpLogShipment DELIVERED path-2 采购运费→到岸成本自动创建 (handlePurchaseReceiptDelivered → generateFreightLandedCost → DRAFT ErpInvLandedCost)', () => {
  test('happy path: advise→completeShipment(DISPATCHED) → handleTrackingWebhook(DELIVERED) → DELIVERED + SETTLED + DRAFT ErpInvLandedCost (FREIGHT amount=100)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpLogShipment-main');

    const carrier = await seedCarrier(page, 'hp');
    const receive = await seedReceive(page, 'hp');
    const shipment = await seedShipment(page, carrier.id, receive.code, 'hp', FREIGHT_AMOUNT);
    expect(shipment.status, 'precondition status=DRAFT').toBe('DRAFT');

    // advise → ADVISED
    await callMutationOk(page, 'ErpLogShipment', 'advise', { shipmentId: shipment.id }, 'id');
    // completeShipment → DISPATCHED + trackingNo 写回（mock: MOCK-{shipmentCode}）
    await callMutationOk(page, 'ErpLogShipment', 'completeShipment', { shipmentId: shipment.id }, 'id');
    const dispatched = await verifyState(page, 'ErpLogShipment', shipment.id, 'status trackingNo freightSettlementStatus');
    expect(dispatched.status, 'after completeShipment status=DISPATCHED').toBe('DISPATCHED');
    expect(dispatched.trackingNo, 'trackingNo written back by mock gateway').toBeTruthy();
    expect(dispatched.freightSettlementStatus, 'precondition freightSettlementStatus=PENDING').toBe('PENDING');

    // handleTrackingWebhook(DELIVERED)：payload 为 JSON 字符串，signature 跳过（config=false）
    const payload = JSON.stringify({
      trackingNo: dispatched.trackingNo,
      eventType: 'DELIVERED',
      signedBy: 'E2E',
    });
    await callMutationOk(
      page, 'ErpLogShipment', 'handleTrackingWebhook',
      { carrierCode: carrier.code, signature: 'skip', payload },
      'id status freightSettlementStatus',
    );

    const v = await verifyState(page, 'ErpLogShipment', shipment.id, 'status freightSettlementStatus signedBy actualDeliveryDate');
    expect(v.status, 'after webhook status=DELIVERED').toBe('DELIVERED');
    expect(v.freightSettlementStatus, 'freightSettlementStatus=SETTLED after path-2 auto-create').toBe('SETTLED');
    expect(v.signedBy, 'signedBy written from payload').toBe('E2E');
    expect(v.actualDeliveryDate, 'actualDeliveryDate written').not.toBeNull();

    // path-2 自动创建 DRAFT ErpInvLandedCost 断言（按 receiveId 直查）
    const landedCost = await findLandedCostByReceiveId(page, receive.id);
    expect(landedCost, 'DRAFT ErpInvLandedCost should be auto-created (path-2)').toBeTruthy();
    expect(landedCost!.docStatus, 'landedCost docStatus=DRAFT').toBe('DRAFT');
    expect(landedCost!.approveStatus, 'landedCost approveStatus=UNSUBMITTED').toBe('UNSUBMITTED');
    expect(Number(landedCost!.totalCostAmount), 'landedCost totalCostAmount=freightAmount').toBe(FREIGHT_AMOUNT);
    expect(Number(landedCost!.currencyId), 'landedCost currencyId=shipment.freightCurrencyId').toBe(CURRENCY);
    expect(Number(landedCost!.supplierId), 'landedCost supplierId=receive.supplierId (SUP-001=3)').toBe(SUPPLIER);
    expect(landedCost!.allocationMethod, 'landedCost allocationMethod=BY_AMOUNT (default)').toBe('BY_AMOUNT');

    // FREIGHT 费用行断言
    const line = await findFreightLine(page, landedCost!.id);
    expect(line, 'FREIGHT line should be created').toBeTruthy();
    expect(line!.costElement, 'line costElement=FREIGHT').toBe('FREIGHT');
    expect(Number(line!.amount), 'line amount=freightAmount').toBe(FREIGHT_AMOUNT);
    expect(Number(line!.apPartnerId), 'line apPartnerId=receive.supplierId').toBe(SUPPLIER);

    await cleanupPath2(page, { landedCostId: landedCost!.id, shipmentId: shipment.id, carrierId: carrier.id, receiveId: receive.id });
  });

  test('freightAmount=0 boundary: handleTrackingWebhook(DELIVERED) → DELIVERED + SETTLED + no ErpInvLandedCost created', async ({ page }) => {
    await loginAndNavigate(page, '/ErpLogShipment-main');

    const carrier = await seedCarrier(page, 'zero');
    const receive = await seedReceive(page, 'zero');
    // freightAmount=0 边界（对齐 backend TestErpLogPath2LandedCost.testPath2SkipWhenFreightAmountNull
    //   freightAmount=null 路径；浏览器层取 freightAmount=0 ≤ 0 分支）
    const shipment = await seedShipment(page, carrier.id, receive.code, 'zero', 0);
    expect(shipment.status, 'precondition status=DRAFT').toBe('DRAFT');

    await callMutationOk(page, 'ErpLogShipment', 'advise', { shipmentId: shipment.id }, 'id');
    await callMutationOk(page, 'ErpLogShipment', 'completeShipment', { shipmentId: shipment.id }, 'id');
    const dispatched = await verifyState(page, 'ErpLogShipment', shipment.id, 'status trackingNo freightSettlementStatus');
    expect(dispatched.status, 'precondition status=DISPATCHED').toBe('DISPATCHED');
    expect(dispatched.freightSettlementStatus, 'precondition freightSettlementStatus=PENDING').toBe('PENDING');

    const payload = JSON.stringify({
      trackingNo: dispatched.trackingNo,
      eventType: 'DELIVERED',
      signedBy: 'E2E',
    });
    await callMutationOk(
      page, 'ErpLogShipment', 'handleTrackingWebhook',
      { carrierCode: carrier.code, signature: 'skip', payload },
      'id status freightSettlementStatus',
    );

    const v = await verifyState(page, 'ErpLogShipment', shipment.id, 'status freightSettlementStatus');
    expect(v.status, 'after webhook status=DELIVERED').toBe('DELIVERED');
    expect(v.freightSettlementStatus, 'freightSettlementStatus=SETTLED (freightAmount=0 ≤ 0)').toBe('SETTLED');

    // 显式断言无 ErpInvLandedCost 创建（freightAmount ≤ 0 路径不调 generateFreightLandedCost）
    const landedCost = await findLandedCostByReceiveId(page, receive.id);
    expect(landedCost, 'no ErpInvLandedCost should be created for freightAmount=0').toBeNull();

    await cleanupPath2(page, { shipmentId: shipment.id, carrierId: carrier.id, receiveId: receive.id });
  });
});
