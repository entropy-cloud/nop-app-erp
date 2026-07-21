import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  eqFilter,
  deleteByFilter,
  deleteById,
  GraphQLClient,
} from './_helper';

/**
 * F11 批量操作浏览器层 E2E（plan 2026-07-22-0444-2 Phase 1/2）。
 *
 * 验证 4 类批量操作经 GraphQL /graphql 的全栈可达性：
 *   (a) ErpPurOrder__batchApprove：3 SUBMITTED + 1 UNSUBMITTED → successCount=3 + failedCount=1
 *       （混合状态守卫；模式 b：行级失败不阻塞其他行；SUBMITTED → APPROVED 翻转断言）
 *   (b) ErpSalOrder__batchApprove：1 SUBMITTED → successCount=1（最小用例）
 *   (c) ErpQaInspection__batchPassInspection：1 PENDING → successCount=1（quality 域代表）
 *   (d) ErpMdPartner__batchUpdate（平台 builtin）：1 ACTIVE → INACTIVE（master-data 批量启用/停用代表）
 *
 * 后端 @BizMutation 签名 + 部分失败语义见 Phase 0 决策记录（plan 内）。
 *
 * 自包含隔离：新建带 lineNo + uniq code 的实体（PurOrder/SalOrder 需带 line，避免 submitForApproval
 * 抛 ERR_ORDER_LINES_EMPTY；QA Inspection 仅需头；Partner 仅需头）。cleanup 逐条 __delete。
 *
 * 种子引用（复用 SEED 语义）：org id=2 / currency CNY id=1 / supplier SUP-001 id=3 / customer CUST-001 id=1 /
 *   MAT-001 id=1 / UOM PCS id=1 / warehouse WH-RAW id=2。
 */

const ORG = 2;
const CURRENCY = 1;
const SUPPLIER = 3;
const CUSTOMER = 1;
const MAT_1 = 1;
const UOM = 1;
const WH = 2;
const BDATE = '2026-07-09';

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

async function createPurOrderSubmitted(page: import('@playwright/test').Page, tag: string) {
  const head: any = await createViaSave(
    page, 'ErpPurOrder',
    {
      code: uniq(`E2E-F11-PO-${tag}`), orgId: ORG, supplierId: SUPPLIER, warehouseId: WH,
      businessDate: BDATE, currencyId: CURRENCY, exchangeRate: 1,
      docStatus: 'ACTIVE', approveStatus: 'UNSUBMITTED', receiveStatus: 'UNRECEIVED',
    },
    'id',
  );
  await createViaSave(
    page, 'ErpPurOrderLine',
    { orderId: head.id, lineNo: 1, materialId: MAT_1, uoMId: UOM, quantity: 10, unitPrice: 5, amount: 50 },
    'id',
  );
  await callMutationOk(page, 'ErpPurOrder', 'submitForApproval', { id: head.id }, 'id');
  return head;
}

async function createPurOrderUnsubmitted(page: import('@playwright/test').Page, tag: string) {
  return createViaSave(
    page, 'ErpPurOrder',
    {
      code: uniq(`E2E-F11-POU-${tag}`), orgId: ORG, supplierId: SUPPLIER, warehouseId: WH,
      businessDate: BDATE, currencyId: CURRENCY, exchangeRate: 1,
      docStatus: 'ACTIVE', approveStatus: 'UNSUBMITTED', receiveStatus: 'UNRECEIVED',
    },
    'id',
  );
}

async function createSalOrderSubmitted(page: import('@playwright/test').Page, tag: string) {
  const head: any = await createViaSave(
    page, 'ErpSalOrder',
    {
      code: uniq(`E2E-F11-SO-${tag}`), orgId: ORG, customerId: CUSTOMER, warehouseId: WH,
      businessDate: BDATE, currencyId: CURRENCY, exchangeRate: 1,
      docStatus: 'ACTIVE', approveStatus: 'UNSUBMITTED', deliveryStatus: 'UNDELIVERED',
    },
    'id',
  );
  await createViaSave(
    page, 'ErpSalOrderLine',
    { orderId: head.id, lineNo: 1, materialId: MAT_1, uoMId: UOM, quantity: 10, unitPrice: 10, amount: 100 },
    'id',
  );
  await callMutationOk(page, 'ErpSalOrder', 'submitForApproval', { id: head.id }, 'id');
  return head;
}

async function createQaInspectionPending(page: import('@playwright/test').Page, tag: string) {
  return createViaSave(
    page, 'ErpQaInspection',
    {
      code: uniq(`E2E-F11-QA-${tag}`), materialId: MAT_1, inspectionType: 'INCOMING',
      lotQuantity: 10, supplierId: SUPPLIER, warehouseId: WH,
      inspectionDate: BDATE, businessDate: BDATE,
      result: 'PENDING', docStatus: 'ACTIVE', approveStatus: 'UNSUBMITTED', posted: false,
    },
    'id',
  );
}

async function createPartnerActive(page: import('@playwright/test').Page, tag: string) {
  return createViaSave(
    page, 'ErpMdPartner',
    {
      code: uniq(`E2E-F11-PN-${tag}`), name: `E2E F11 Partner ${tag}`,
      partnerType: 'CUSTOMER', status: 'ACTIVE',
    },
    'id',
  );
}

async function cleanupPurOrders(page: import('@playwright/test').Page, ids: Array<string | number>) {
  for (const id of ids) {
    await deleteByFilter(page, 'ErpPurOrderLine', eqFilter('orderId', Number(id)));
    await deleteById(page, 'ErpPurOrder', id);
  }
}

async function cleanupSalOrders(page: import('@playwright/test').Page, ids: Array<string | number>) {
  for (const id of ids) {
    await deleteByFilter(page, 'ErpSalOrderLine', eqFilter('orderId', Number(id)));
    await deleteById(page, 'ErpSalOrder', id);
  }
}

test.describe('F11 batch operations browser-layer E2E', () => {
  test('(a) ErpPurOrder__batchApprove: 3 SUBMITTED + 1 UNSUBMITTED → successCount=3 + failedCount=1', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurOrder-main');

    const submitted1 = await createPurOrderSubmitted(page, 'a1');
    const submitted2 = await createPurOrderSubmitted(page, 'a2');
    const submitted3 = await createPurOrderSubmitted(page, 'a3');
    const unsubmitted = await createPurOrderUnsubmitted(page, 'a4');
    const allIds = [submitted1.id, submitted2.id, submitted3.id, unsubmitted.id];

    try {
      const result = await callMutationOk(
        page, 'ErpPurOrder', 'batchApprove',
        { ids: allIds },
        'totalCount successCount failedCount failures{id code message}',
      );

      expect(result.totalCount, 'totalCount should equal input ids length').toBe(4);
      expect(result.successCount, '3 SUBMITTED orders should be approved').toBe(3);
      expect(result.failedCount, '1 UNSUBMITTED order should fail').toBe(1);
      expect(result.failures?.length, 'failures list should have 1 entry').toBe(1);
      expect(String(result.failures[0].id), 'failure id should be the unsubmitted order').toBe(String(unsubmitted.id));

      const s1 = await verifyState(page, 'ErpPurOrder', submitted1.id, 'approveStatus');
      const s2 = await verifyState(page, 'ErpPurOrder', submitted2.id, 'approveStatus');
      const s3 = await verifyState(page, 'ErpPurOrder', submitted3.id, 'approveStatus');
      const su = await verifyState(page, 'ErpPurOrder', unsubmitted.id, 'approveStatus');
      expect(s1.approveStatus, 'submitted1 should flip to APPROVED').toBe('APPROVED');
      expect(s2.approveStatus, 'submitted2 should flip to APPROVED').toBe('APPROVED');
      expect(s3.approveStatus, 'submitted3 should flip to APPROVED').toBe('APPROVED');
      expect(su.approveStatus, 'unsubmitted should remain UNSUBMITTED (row-level isolation)').toBe('UNSUBMITTED');
    } finally {
      await cleanupPurOrders(page, allIds);
    }
  });

  test('(a-empty) ErpPurOrder__batchApprove with empty ids returns zero result (no error)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurOrder-main');

    const result = await callMutationOk(
      page, 'ErpPurOrder', 'batchApprove',
      { ids: [] },
      'totalCount successCount failedCount',
    );

    expect(result.totalCount, 'empty input → totalCount=0').toBe(0);
    expect(result.successCount, 'empty input → successCount=0').toBe(0);
    expect(result.failedCount, 'empty input → failedCount=0').toBe(0);
  });

  test('(b) ErpSalOrder__batchApprove: 1 SUBMITTED → successCount=1', async ({ page }) => {
    await loginAndNavigate(page, '/ErpSalOrder-main');

    const submitted = await createSalOrderSubmitted(page, 'b1');
    const ids = [submitted.id];

    try {
      const result = await callMutationOk(
        page, 'ErpSalOrder', 'batchApprove',
        { ids },
        'totalCount successCount failedCount',
      );

      expect(result.totalCount, 'totalCount=1').toBe(1);
      expect(result.successCount, '1 SUBMITTED SO should be approved').toBe(1);
      expect(result.failedCount, 'failedCount=0').toBe(0);

      const so = await verifyState(page, 'ErpSalOrder', submitted.id, 'approveStatus');
      expect(so.approveStatus, 'SO should flip to APPROVED').toBe('APPROVED');
    } finally {
      await cleanupSalOrders(page, ids);
    }
  });

  test('(c) ErpQaInspection__batchPassInspection: 1 PENDING → successCount=1', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaInspection-main');

    const inspection = await createQaInspectionPending(page, 'c1');
    const ids = [inspection.id];

    try {
      const result = await callMutationOk(
        page, 'ErpQaInspection', 'batchPassInspection',
        { ids },
        'totalCount successCount failedCount',
      );

      expect(result.totalCount, 'totalCount=1').toBe(1);
      expect(result.successCount, '1 PENDING inspection should be passed').toBe(1);
      expect(result.failedCount, 'failedCount=0').toBe(0);

      const ins = await verifyState(page, 'ErpQaInspection', inspection.id, 'result');
      expect(ins.result, 'inspection result should flip to ACCEPTED').toBe('ACCEPTED');
    } finally {
      await deleteById(page, 'ErpQaInspection', inspection.id);
    }
  });

  test('(d) ErpMdPartner__batchUpdate (platform builtin): 1 ACTIVE → INACTIVE', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMdPartner-main');

    const partner = await createPartnerActive(page, 'd1');
    const ids = [partner.id];

    try {
      const before = await verifyState(page, 'ErpMdPartner', partner.id, 'status');
      expect(before.status, 'precondition: partner should be ACTIVE').toBe('ACTIVE');

      // batchUpdate returns void;经 raw() 直接发 GraphQL（callMutation 默认附 {fields} 选择集对 void 类型非法）。
      // 平台 builtin mutation 签名：batchUpdate(ids:Set<String>, data:Map, ignoreUnknown:Boolean)。
      const gql = new GraphQLClient(page);
      const json: any = await gql.raw(
        `mutation($ids:[String],$d:Map){ ErpMdPartner__batchUpdate(ids:$ids,data:$d) }`,
        { ids: ids.map(String), d: { status: 'INACTIVE' } },
      );
      const errors = json?.errors ?? null;
      expect(errors, 'batchUpdate should not return GraphQL errors').toBeNull();

      const after = await verifyState(page, 'ErpMdPartner', partner.id, 'status');
      expect(after.status, 'partner status should flip to INACTIVE after batchUpdate').toBe('INACTIVE');
    } finally {
      await deleteById(page, 'ErpMdPartner', partner.id);
    }
  });
});
