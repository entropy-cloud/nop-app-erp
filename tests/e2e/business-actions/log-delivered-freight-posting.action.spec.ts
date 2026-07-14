import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById, deleteByFilter, eqFilter, findFirst, findPageTotal } from './_helper';
import { findVoucherIdByBillCode, assertVoucherLines, cleanupVoucherByBillCode } from '../orchestration/_helper';

/**
 * logistics ErpLogShipment DELIVERED 运费过账业务动作浏览器层 E2E（plan 2026-07-14-0941-2 Phase 3）。
 *
 * 验证 DELIVERED 运费过账编排 DIRECT @BizMutation（handleTrackingWebhook → onDelivered → FREIGHT posting）
 * 经 GraphQL /graphql 的全栈可达性 + shipment DELIVERED + freightSettlementStatus SETTLED + FREIGHT 凭证
 * 创建断言 + 幂等守卫。
 *
 * 权威编排（ErpLogShipmentBizModel，对齐 docs/design/logistics/carrier-integration.md §3.18）：
 *   handleTrackingWebhook(carrierCode, signature, payload)：payload 为 JSON 字符串
 *     `{"trackingNo":"...","eventType":"DELIVERED","signedBy":"..."}`，signature 为 HMAC-SHA256
 *     （secret=carrier.code）。config erp-log.webhook-signature-required 默认 true，webServer JVM arg
 *     =false 跳过验签。方法内 parsePayload → findShipmentByTrackingNo → advanceTracking(DISPATCHED→DELIVERED)
 *     → advanced=true 时 onDelivered。
 *   onDelivered（path-1 SALES_DELIVERY）：freightSettlementStatus PENDING → 构建 FREIGHT PostingEvent
 *     （billHeadCode=shipment.code）→ IErpFinVoucherBiz.post → 成功 mark SETTLED。已 SETTLED 时抛
 *     ERR_LOG_SHIPMENT_ALREADY_DELIVERED。
 *   幂等：已 DELIVERED 的 shipment 再次 webhook(DELIVERED) → advanceTracking 返回 false → onDelivered 不
 *     被调用 → 方法静默返回（无重复凭证、无 error 抛出）。
 *
 * Phase 3 Explore Decision（DELIVERED 触发最低成本路径裁定）：
 *   - DELIVERED 前置：shipment 须 DISPATCHED（advanceTracking 从 DISPATCHED 推进到 DELIVERED）。经
 *     advise→completeShipment 链到达 DISPATCHED + trackingNo 写回（mock 网关 trackingNo=MOCK-{shipmentCode}）。
 *   - 签名验证：webServer JVM arg erp-log.webhook-signature-required=false 跳过 HMAC-SHA256 验签，
 *     payload 直传 JSON 字符串（signature 传任意值即可）。
 *   - FREIGHT 凭证行（LogisticsFreightProvider，relatedBillType=SALES_DELIVERY，freightTerms=PREPAID 默认）：
 *     Dr 6601 销售费用=freightAmount / Cr 1002 银行存款=freightAmount。科目 6601/1002 经种子 COA 可达。
 *   - path-2 到岸成本自动创建：config erp-log.path2-landed-cost-auto-create 默认 false（关闭），path-1 运费
 *     过账已作代表验证 onDelivered 触发面。path-2 降级 Deferred But Adjudicated（后端 TestErpLogPath2LandedCost
 *     覆盖，浏览器层 setup 复杂度高——需 PURCHASE_RECEIPT shipment + 到岸成本 Provider 链）。
 *   - ERR_LOG_SHIPMENT_ALREADY_DELIVERED：由 onDelivered 内 freightSettlementStatus 已 SETTLED 时抛出。
 *     webhook 幂等路径下 advanceTracking 返回 false 不触发 onDelivered，故此 error 经 webhook 不可达；
 *     scanForPolling 捕获该异常不向上抛（catch + log），浏览器层不观测为 GraphQL error。故幂等守卫仅断言
 *     凭证数不增加（无重复凭证），ERR_LOG_SHIPMENT_ALREADY_DELIVERED 作内部守卫文档记录。
 *
 * 自包含 setup：建 ErpLogCarrier（gatewayId="mock"）+ ErpLogShipment（SALES_DELIVERY, freightAmount=100,
 *   freightCurrencyId=1, freightSettlementStatus=PENDING）→ advise → completeShipment（DISPATCHED + trackingNo）
 *   → handleTrackingWebhook(DELIVERED)。
 * 清理：删 FREIGHT 凭证（billCode=shipment.code）+ Shipment + Carrier（ShipmentLog 经 shipmentId 残留不阻断）。
 */

const GATEWAY_ID_MOCK = 'mock';
const FREIGHT_AMOUNT = 100;

async function seedCarrier(page: import('@playwright/test').Page, tag: string): Promise<{ id: string; code: string }> {
  const code = `E2E-LOG-DEL-CAR-${tag}-${Date.now()}`;
  const c = await createViaSave(
    page, 'ErpLogCarrier',
    {
      code, carrierName: `E2E Delivered Carrier ${tag}`,
      carrierType: 'EXPRESS', gatewayId: GATEWAY_ID_MOCK, isActive: 1,
    },
    'id code',
  );
  return c;
}

async function seedShipment(page: import('@playwright/test').Page, carrierId: string | number, tag: string): Promise<{ id: string; code: string; status: string }> {
  const code = `E2E-LOG-DEL-SHP-${tag}-${Date.now()}`;
  return createViaSave(
    page, 'ErpLogShipment',
    {
      code, orgId: 2, carrierId,
      relatedBillType: 'SALES_DELIVERY',
      freightAmount: FREIGHT_AMOUNT, freightCurrencyId: 1, freightTerms: 'PREPAID',
      freightSettlementStatus: 'PENDING',
      status: 'DRAFT', businessDate: '2026-07-10',
    },
    'id code status',
  );
}

async function countVouchersByBillCode(page: import('@playwright/test').Page, billCode: string): Promise<number> {
  return findPageTotal(page, 'ErpFinVoucherBillR', eqFilter('billCode', billCode));
}

async function cleanupDeliveredChain(page: import('@playwright/test').Page, shipmentCode: string, shipmentId?: string | number, carrierId?: string | number): Promise<void> {
  if (shipmentCode) {
    await cleanupVoucherByBillCode(page, shipmentCode);
  }
  if (shipmentId) await deleteById(page, 'ErpLogShipment', shipmentId);
  if (carrierId) await deleteById(page, 'ErpLogCarrier', carrierId);
}

test.describe('logistics ErpLogShipment DELIVERED freight posting (handleTrackingWebhook → onDelivered → FREIGHT)', () => {
  test('happy path: advise→completeShipment(DISPATCHED) → handleTrackingWebhook(DELIVERED) → DELIVERED + SETTLED + FREIGHT voucher', async ({ page }) => {
    await loginAndNavigate(page, '/ErpLogShipment-main');

    const carrier = await seedCarrier(page, 'hp');
    const shipment = await seedShipment(page, carrier.id, 'hp');
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
    expect(v.freightSettlementStatus, 'freightSettlementStatus=SETTLED after freight posting').toBe('SETTLED');
    expect(v.signedBy, 'signedBy written from payload').toBe('E2E');
    expect(v.actualDeliveryDate, 'actualDeliveryDate written').not.toBeNull();

    // FREIGHT 凭证创建断言（billHeadCode=shipment.code）
    const voucherId = await findVoucherIdByBillCode(page, shipment.code, 'NORMAL');
    expect(voucherId, 'FREIGHT voucher should be created (billCode=shipment.code)').toBeTruthy();
    // FREIGHT 凭证行数值断言（LogisticsFreightProvider SALES_DELIVERY PREPAID）：
    //   Dr 6601 销售费用=100 / Cr 1002 银行存款=100
    await assertVoucherLines(page, voucherId, [
      { subjectCode: '6601', dcDirection: 'DEBIT', debitAmount: FREIGHT_AMOUNT, creditAmount: 0 },
      { subjectCode: '1002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: FREIGHT_AMOUNT },
    ]);

    await cleanupDeliveredChain(page, shipment.code, shipment.id, carrier.id);
  });

  test('idempotency: second DELIVERED webhook on DELIVERED+SETTLED → no error, no duplicate voucher', async ({ page }) => {
    await loginAndNavigate(page, '/ErpLogShipment-main');

    const carrier = await seedCarrier(page, 'idem');
    const shipment = await seedShipment(page, carrier.id, 'idem');

    await callMutationOk(page, 'ErpLogShipment', 'advise', { shipmentId: shipment.id }, 'id');
    await callMutationOk(page, 'ErpLogShipment', 'completeShipment', { shipmentId: shipment.id }, 'id');
    const dispatched = await verifyState(page, 'ErpLogShipment', shipment.id, 'status trackingNo freightSettlementStatus');
    expect(dispatched.status, 'precondition status=DISPATCHED').toBe('DISPATCHED');

    const payload = JSON.stringify({ trackingNo: dispatched.trackingNo, eventType: 'DELIVERED', signedBy: 'E2E' });

    // 第一次 DELIVERED webhook → DELIVERED + SETTLED + FREIGHT 凭证
    await callMutationOk(
      page, 'ErpLogShipment', 'handleTrackingWebhook',
      { carrierCode: carrier.code, signature: 'skip', payload },
      'id status freightSettlementStatus',
    );
    const afterFirst = await verifyState(page, 'ErpLogShipment', shipment.id, 'status freightSettlementStatus');
    expect(afterFirst.status, 'after first webhook status=DELIVERED').toBe('DELIVERED');
    expect(afterFirst.freightSettlementStatus, 'freightSettlementStatus=SETTLED').toBe('SETTLED');

    const voucherCountAfterFirst = await countVouchersByBillCode(page, shipment.code);
    expect(voucherCountAfterFirst, 'one FREIGHT voucher after first webhook').toBeGreaterThanOrEqual(1);

    // 第二次 DELIVERED webhook → advanceTracking 返回 false（已 DELIVERED）→ onDelivered 不调用 → 静默返回
    await callMutationOk(
      page, 'ErpLogShipment', 'handleTrackingWebhook',
      { carrierCode: carrier.code, signature: 'skip', payload },
      'id status freightSettlementStatus',
    );
    const afterSecond = await verifyState(page, 'ErpLogShipment', shipment.id, 'status freightSettlementStatus');
    expect(afterSecond.status, 'status stays DELIVERED after second webhook').toBe('DELIVERED');
    expect(afterSecond.freightSettlementStatus, 'freightSettlementStatus stays SETTLED').toBe('SETTLED');

    // 凭证数不增加（无重复凭证）
    const voucherCountAfterSecond = await countVouchersByBillCode(page, shipment.code);
    expect(voucherCountAfterSecond, 'no duplicate voucher on second webhook').toBe(voucherCountAfterFirst);

    await cleanupDeliveredChain(page, shipment.code, shipment.id, carrier.id);
  });
});
