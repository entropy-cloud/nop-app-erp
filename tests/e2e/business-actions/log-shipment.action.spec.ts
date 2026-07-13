import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById } from './_helper';

/**
 * logistics ErpLogShipment 发运单状态机业务动作浏览器层 E2E（plan 2026-07-14-0508-1 Phase 3）。
 *
 * 验证发运单 DIRECT @BizMutation 状态机（advise→completeShipment→cancelShipment）经 GraphQL /graphql
 * 的全栈可达性 + status 翻转 + 承运商网关 SPI 集成。
 *
 * 权威状态机（ErpLogShipmentBizModel 委派 GatewayDispatcher，对齐 docs/design/logistics/state-machine.md §1/§2）：
 *   DRAFT --advise--> ADVISED --completeShipment--> DISPATCHED（经承运商网关 completeDeliveryOrder 回写
 *     trackingNo/labelUrl） --cancelShipment--> CANCELLED
 *   cancelShipment 允许 ADVISED/DISPATCHED/IN_TRANSIT/DRAFT → CANCELLED
 *   advise 允许 DRAFT（或幂等 ADVISED）→ ADVISED；其他态抛 ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION
 *   completeShipment 须 ADVISED（其他态抛 ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION）
 *
 * Phase 3 Explore Decision（承运商网关可达性裁定）：
 *   - MockCarrierGatewayClientFactory（gatewayId="mock"，无外部 HTTP）已注册为 bean
 *     （module-logistics/erp-log-service/_vfs/erp/log/beans/app-service.beans.xml）。
 *   - gatewayRegistry.getClient(carrierId) 按 ErpLogCarrier.gatewayId 派发：自包含建 ErpLogCarrier
 *     （gatewayId="mock"）+ ErpLogShipment（carrierId→carrier.id）即可让 MockClient 承运 completeDeliveryOrder
 *     返回 success（默认 failureMode=SUCCESS）+ 写回 trackingNo/labelUrl。
 *   - 故 advise→completeShipment→cancelShipment 三链均完整可达，**不降级**。4 测试全绿（happy forward +
 *     cancel from ADVISED + cancel from DISPATCHED + illegal guards 双守卫）证实裁定准确。
 *
 * 自包含 setup：建 ErpLogCarrier（gatewayId="mock"，唯一 code）+ ErpLogShipment（carrierId→carrier.id，
 *   status=DRAFT，唯一 code）。
 * 清理：删 Shipment + Carrier（ErpLogShipmentLog 经 shipmentId 残留，不阻断——逻辑删除可选）。
 *
 * 种子引用：无（logistics 域交易单据未 seed，business-action spec 自包含 setup，同 0215-2 contract/drp 范式）。
 */

const GATEWAY_ID_MOCK = 'mock';

async function seedCarrier(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpLogCarrier',
    {
      code: `E2E-LOG-CAR-${tag}-${Date.now()}`,
      carrierName: `E2E Mock Carrier ${tag}`,
      carrierType: 'EXPRESS',
      gatewayId: GATEWAY_ID_MOCK,
      isActive: 1,
    },
    'id',
  );
}

async function seedShipment(page: import('@playwright/test').Page, carrierId: string | number, tag: string, status = 'DRAFT'): Promise<{ id: string; status: string }> {
  return createViaSave(
    page, 'ErpLogShipment',
    {
      code: `E2E-LOG-SHP-${tag}-${Date.now()}`,
      carrierId,
      status,
      relatedBillType: 'SALES_DELIVERY',
      freightSettlementStatus: 'PENDING',
    },
    'id status',
  );
}

test.describe('logistics ErpLogShipment state machine (advise/completeShipment/cancelShipment)', () => {
  test('happy path forward: save(DRAFT) → advise(ADVISED) → completeShipment(DISPATCHED + trackingNo written back)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpLogShipment-main');

    const carrier = await seedCarrier(page, 'hp');
    const shipment = await seedShipment(page, carrier.id, 'hp');
    expect(shipment.status, 'precondition status=DRAFT').toBe('DRAFT');

    // advise: DRAFT → ADVISED
    await callMutationOk(page, 'ErpLogShipment', 'advise', { shipmentId: shipment.id }, 'id');
    let v = await verifyState(page, 'ErpLogShipment', shipment.id, 'status');
    expect(v.status, 'after advise status=ADVISED').toBe('ADVISED');

    // completeShipment: ADVISED → DISPATCHED（经 mock 网关 completeDeliveryOrder 回写 trackingNo/labelUrl）
    await callMutationOk(page, 'ErpLogShipment', 'completeShipment', { shipmentId: shipment.id }, 'id');
    v = await verifyState(page, 'ErpLogShipment', shipment.id, 'status trackingNo labelUrl');
    expect(v.status, 'after completeShipment status=DISPATCHED').toBe('DISPATCHED');
    expect(v.trackingNo, 'trackingNo should be written back by mock gateway').toBeTruthy();
    expect(v.labelUrl, 'labelUrl should be written back by mock gateway').toBeTruthy();

    await deleteById(page, 'ErpLogShipment', shipment.id);
    await deleteById(page, 'ErpLogCarrier', carrier.id);
  });

  test('cancel from ADVISED: advise(ADVISED) → cancelShipment(CANCELLED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpLogShipment-main');

    const carrier = await seedCarrier(page, 'ca');
    const shipment = await seedShipment(page, carrier.id, 'ca');
    await callMutationOk(page, 'ErpLogShipment', 'advise', { shipmentId: shipment.id }, 'id');
    let v = await verifyState(page, 'ErpLogShipment', shipment.id, 'status');
    expect(v.status, 'precondition status=ADVISED').toBe('ADVISED');

    // cancelShipment: ADVISED → CANCELLED（不调 client.cancelShipment，仅状态迁移）
    await callMutationOk(page, 'ErpLogShipment', 'cancelShipment', { shipmentId: shipment.id }, 'id');
    v = await verifyState(page, 'ErpLogShipment', shipment.id, 'status');
    expect(v.status, 'after cancelShipment status=CANCELLED').toBe('CANCELLED');

    await deleteById(page, 'ErpLogShipment', shipment.id);
    await deleteById(page, 'ErpLogCarrier', carrier.id);
  });

  test('cancel from DISPATCHED: advise→completeShipment(DISPATCHED) → cancelShipment(CANCELLED, mock supports cancel)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpLogShipment-main');

    const carrier = await seedCarrier(page, 'cd');
    const shipment = await seedShipment(page, carrier.id, 'cd');
    await callMutationOk(page, 'ErpLogShipment', 'advise', { shipmentId: shipment.id }, 'id');
    await callMutationOk(page, 'ErpLogShipment', 'completeShipment', { shipmentId: shipment.id }, 'id');
    let v = await verifyState(page, 'ErpLogShipment', shipment.id, 'status');
    expect(v.status, 'precondition status=DISPATCHED').toBe('DISPATCHED');

    // DISPATCHED → cancelShipment 经 client.cancelShipment（mock 无副作用）→ CANCELLED
    await callMutationOk(page, 'ErpLogShipment', 'cancelShipment', { shipmentId: shipment.id }, 'id');
    v = await verifyState(page, 'ErpLogShipment', shipment.id, 'status');
    expect(v.status, 'after cancelShipment status=CANCELLED').toBe('CANCELLED');

    await deleteById(page, 'ErpLogShipment', shipment.id);
    await deleteById(page, 'ErpLogCarrier', carrier.id);
  });

  test('illegal transition guards: CANCELLED→advise rejected; DRAFT→completeShipment rejected (ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpLogShipment-main');

    // CANCELLED → advise（advise 须 DRAFT 或 ADVISED）
    const carrier = await seedCarrier(page, 'gd');
    const cancelled = await seedShipment(page, carrier.id, 'gd-c');
    await callMutationOk(page, 'ErpLogShipment', 'advise', { shipmentId: cancelled.id }, 'id');
    await callMutationOk(page, 'ErpLogShipment', 'cancelShipment', { shipmentId: cancelled.id }, 'id');
    let v = await verifyState(page, 'ErpLogShipment', cancelled.id, 'status');
    expect(v.status, 'precondition status=CANCELLED').toBe('CANCELLED');

    const rej1 = await callMutation(page, 'ErpLogShipment', 'advise', { shipmentId: cancelled.id }, 'id');
    expect(rej1.errors, 'advise from CANCELLED should be rejected').toBeTruthy();
    expect(JSON.stringify(rej1.errors), 'reject should carry illegal-transition token').toContain('不允许执行该操作');
    v = await verifyState(page, 'ErpLogShipment', cancelled.id, 'status');
    expect(v.status, 'failed advise should not change status').toBe('CANCELLED');

    // DRAFT → completeShipment（completeShipment 须 ADVISED）
    const draft = await seedShipment(page, carrier.id, 'gd-d');
    const rej2 = await callMutation(page, 'ErpLogShipment', 'completeShipment', { shipmentId: draft.id }, 'id');
    expect(rej2.errors, 'completeShipment from DRAFT should be rejected').toBeTruthy();
    expect(JSON.stringify(rej2.errors), 'reject should carry illegal-transition token').toContain('不允许执行该操作');
    v = await verifyState(page, 'ErpLogShipment', draft.id, 'status');
    expect(v.status, 'failed completeShipment should not change status').toBe('DRAFT');

    await deleteById(page, 'ErpLogShipment', cancelled.id);
    await deleteById(page, 'ErpLogShipment', draft.id);
    await deleteById(page, 'ErpLogCarrier', carrier.id);
  });
});
