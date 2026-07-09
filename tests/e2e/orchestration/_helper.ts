import { test, expect, loginAndNavigate } from '../fixtures';
import type { Page } from '@playwright/test';

/**
 * 跨域编排链浏览器层 E2E helper（plan 2026-07-09-1249-1）。
 *
 * 扩展 0814-2 业务动作三原语（createViaSave/callMutation/verifyState）为 P2P / O2C 链式编排：
 *   - runP2pChain：PO→Receive→Invoice 全链 __save + submitForApproval + approve，每步 verifyState 断言状态翻转。
 *   - runO2cChain：SO→Delivery→Invoice 全链（含出库前置备货 generateMove INCOMING+complete）。
 *   - 过账产物断言原语（findItems/findFirst）：stockMove / voucher_bill_r / ar_ap_item。
 *   - 清理原语：链路创建的不可逆下游产物（库存流水/余额、GL 凭证/凭证行/回链、AR-AP 辅助账）逐域逻辑删除，
 *     保护共享 DB 的下游数值断言基线（inventory/finance dashboard KPI、ar-ap-aging 报表）。
 *
 * 复用 business-actions/_helper.ts 已验证原语（filter 格式、mutation 内联、__get 权威查库）。
 */
export { test, expect, loginAndNavigate };

// 复用 0814-2 已验证原语
import {
  createViaSave,
  callMutationOk,
  verifyState,
  findPageTotal,
  eqFilter,
  andFilter,
  deleteByFilter,
  deleteById,
  input,
} from '../business-actions/_helper';
export { createViaSave, callMutationOk, verifyState, findPageTotal, eqFilter, andFilter, deleteByFilter, deleteById, input };

/** 种子引用（master-data init-data，与 0814-2 inventory spec 一致）。 */
export const SEED = {
  ORG: 2,
  WH_RAW: 2,        // WH-RAW 原料仓（种子中 MAT-1 无余额，备货+清理安全）
  MAT_1: 1,         // MAT-001 ERP 标准型产品甲（FINISHED_PRODUCT, MOVING_AVERAGE, uom=1）
  UOM: 1,           // PCS 个
  CURRENCY: 1,      // CNY
  ACCT_SCHEMA: 1,   // ACCT-FIN-01
  SUPPLIER: 3,      // SUP-001 北方钢铁供应商
  CUSTOMER: 1,      // CUST-001 华东科技（creditLimit=500000，覆盖测试订单金额）
} as const;

const BDATE = '2026-07-09'; // 落在种子 OPEN 期间 2026-07（posting resolveOpenPeriod 需要）
const MOVE_REQ_TYPE = 'i_app_erp_inv_biz_StockMoveRequest';

async function extract(resp: { json: () => Promise<unknown> }, actionKey: string): Promise<{ data: any | null; errors: any[] | null }> {
  const json: any = await resp.json();
  return { data: json?.data?.[actionKey] ?? null, errors: json?.errors ?? null };
}

/**
 * 经 __findPage 返回 items（带 selection）。用于读取下游产物详情（stockMove.code / ar_ap.openAmount 等）。
 */
export async function findItems<T = any>(
  page: Page,
  entityName: string,
  filter: Record<string, unknown>,
  selection: string,
): Promise<T[]> {
  const resp = await page.request.post('/graphql', {
    data: {
      query: `query($f:Map){ ${entityName}__findPage(query:{offset:0,limit:500,filter:$f}){ items{ ${selection} } } }`,
      variables: { f: filter },
    },
  });
  const { data } = await extract(resp, `${entityName}__findPage`);
  return (data?.items || []) as T[];
}

export async function findFirst<T = any>(
  page: Page,
  entityName: string,
  filter: Record<string, unknown>,
  selection: string,
): Promise<T | null> {
  const items = await findItems<T>(page, entityName, filter, selection);
  return items.length > 0 ? items[0] : null;
}

// ---------- 凭证行精确数值断言原语（plan 2026-07-10-0704-1） ----------

/** 凭证行期望：科目码 + 借贷方向 + 借方/贷方金额（金额为确定性种子派生，无浮点误差）。 */
export interface VoucherLineExpect {
  subjectCode: string;
  dcDirection: 'DEBIT' | 'CREDIT';
  /** 期望借方列值：NORMAL 正、REVERSAL 红字取负（方向不变、金额取负）。 */
  debitAmount: number;
  /** 期望贷方列值：NORMAL 正、REVERSAL 红字取负。 */
  creditAmount: number;
}

/**
 * 经 ErpFinVoucherBillR(billCode) 反查凭证 id，可选按 ErpFinVoucher.postingType(NORMAL/REVERSAL) 过滤。
 * 反向冲销红字凭证与原正常凭证共用同一 billHeadCode（reverseProcess 同 code 写 voucher_bill_r），
 * 故 postingType 过滤是区分原/红字凭证的唯一手段。
 */
export async function findVoucherIdByBillCode(
  page: Page,
  billCode: string,
  postingType?: 'NORMAL' | 'REVERSAL',
): Promise<number | null> {
  if (!billCode) return null;
  const links = await findItems<any>(page, 'ErpFinVoucherBillR', eqFilter('billCode', billCode), 'voucherId');
  for (const lnk of links) {
    const v = await findFirst<any>(page, 'ErpFinVoucher', eqFilter('id', Number(lnk.voucherId)), 'id postingType');
    if (!v) continue;
    if (!postingType || v.postingType === postingType) {
      return Number(v.id);
    }
  }
  return null;
}

/**
 * 断言凭证行精确数值：按 voucherId 查 ErpFinVoucherLine，逐行匹配期望 subjectCode + dcDirection + 借贷金额。
 *
 * 匹配语义：同一凭证内每科目码至多一行（Provider 结构派生），按 subjectCode 唯一匹配。
 * 断言维度：①行数相等 ②每期望科目码存在唯一实际行 ③dcDirection + debitAmount + creditAmount 精确匹配。
 * 金额精度：BigDecimal 经 Number() 转换后 toBe 精确匹配（种子派生确定性值，无浮点误差风险）。
 */
export async function assertVoucherLines(
  page: Page,
  voucherId: number | null | undefined,
  expected: VoucherLineExpect[],
): Promise<void> {
  expect(voucherId, 'voucherId must be resolved before asserting voucher lines').toBeTruthy();
  const lines = await findItems<any>(
    page, 'ErpFinVoucherLine', eqFilter('voucherId', Number(voucherId)),
    'subjectCode dcDirection debitAmount creditAmount',
  );
  expect(lines.length, `voucher ${voucherId} should have ${expected.length} lines`).toBe(expected.length);
  for (const exp of expected) {
    const matched = lines.filter((l) => l.subjectCode === exp.subjectCode);
    expect(matched.length, `voucher ${voucherId}: exactly one line for subjectCode=${exp.subjectCode}`).toBe(1);
    const line = matched[0];
    expect(line.dcDirection, `subjectCode=${exp.subjectCode} dcDirection`).toBe(exp.dcDirection);
    expect(Number(line.debitAmount ?? 0), `subjectCode=${exp.subjectCode} debitAmount`).toBe(exp.debitAmount);
    expect(Number(line.creditAmount ?? 0), `subjectCode=${exp.subjectCode} creditAmount`).toBe(exp.creditAmount);
  }
}

/**
 * 保存单据头 + 单条行（行实体 registerShortName=true，独立 __save，FK 显式引用头 id）。
 * 返回 { head, line }（含 id）。比嵌套 lines 更可控（不依赖 GraphQL input 是否暴露 to-many）。
 */
async function saveHeadWithLine(
  page: Page,
  headEntity: string,
  headData: Record<string, unknown>,
  headSelection: string,
  lineEntity: string,
  lineData: Record<string, unknown>,
  fkField: string,
  lineSelection = 'id',
): Promise<{ head: any; line: any }> {
  const head = await createViaSave(page, headEntity, headData, headSelection);
  const line = await createViaSave(page, lineEntity, { ...lineData, [fkField]: head.id }, lineSelection);
  return { head, line };
}

// ---------- 清理原语（逻辑删除，保护共享 DB 数值断言基线） ----------

/**
 * 清理 GL 凭证产物（凭证行 + 凭证 + 回链）经 billCode 关联。
 * posting persistVoucher 仅写 voucher/voucher_line/voucher_bill_r（不写 gl_balance），
 * 故清理此三表即清除过账凭证污染（finance dashboard 读 gl_balance 不受影响）。
 */
export async function cleanupVoucherByBillCode(page: Page, billCode: string): Promise<void> {
  if (!billCode) return;
  const billRs = await findItems<any>(page, 'ErpFinVoucherBillR', eqFilter('billCode', billCode), 'voucherId');
  const voucherIds: any[] = billRs.map((b) => b.voucherId).filter((v) => v != null);
  for (const vid of voucherIds) {
    await deleteByFilter(page, 'ErpFinVoucherLine', eqFilter('voucherId', Number(vid)));
    await deleteById(page, 'ErpFinVoucher', vid);
  }
  await deleteByFilter(page, 'ErpFinVoucherBillR', eqFilter('billCode', billCode));
}

/** 清理 AR-AP 辅助账项（按 sourceBillCode）。 */
export async function cleanupArApByCode(page: Page, sourceBillCode: string): Promise<void> {
  if (!sourceBillCode) return;
  await deleteByFilter(page, 'ErpFinArApItem', eqFilter('sourceBillCode', sourceBillCode));
}

/**
 * 清理库存移动单产物：GL 凭证（billCode=move.code）+ 流水 + 移动单行 + 移动单 + 余额。
 * 镜像 0814-2 inventory spec 清理范式（ledger/balance/moveLine/move），扩展到过账凭证（posting 启用后产生）。
 */
async function cleanupStockMove(
  page: Page,
  move: { id?: any; code?: string } | null,
  materialId?: number,
  warehouseId?: number,
): Promise<void> {
  if (!move) return;
  if (move.code) await cleanupVoucherByBillCode(page, move.code);
  if (move.id != null) {
    await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('moveId', Number(move.id)));
    await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(move.id)));
    await deleteById(page, 'ErpInvStockMove', move.id);
  }
  if (materialId != null && warehouseId != null) {
    await deleteByFilter(page, 'ErpInvStockBalance', andFilter(eqFilter('materialId', materialId), eqFilter('warehouseId', warehouseId)));
  }
}

/** 断言审批状态机单步翻转（经 __get 权威查库）。 */
async function expectApproveStatus(
  page: Page,
  entity: string,
  id: any,
  expected: string,
  label: string,
): Promise<void> {
  const s = await verifyState(page, entity, id, 'approveStatus');
  expect(s?.approveStatus, `${entity} ${label}: approveStatus should be ${expected}`).toBe(expected);
}

// ---------- P2P 链路（PO → Receive → Invoice） ----------

export interface P2pResult {
  po?: any; poLine?: any;
  receive?: any; rcvLine?: any;
  invoice?: any;
  receiveMove?: { id: any; code: string; docStatus: string; posted: boolean } | null;
  codes: { po: string; receive: string; invoice: string };
}

/** 期望值（确定性）：PO 行 10×5=50；发票含税 50+6.5=56.5。 */
export const P2P_EXPECT = { invoiceNet: 50, invoiceTax: 6.5, invoiceWithTax: 56.5, qty: 10, price: 5 };

export async function runP2pChain(page: Page): Promise<P2pResult> {
  const r: P2pResult = {
    codes: {
      po: `E2E-P2P-PO-${Date.now()}`,
      receive: `E2E-P2P-RCV-${Date.now()}`,
      invoice: `E2E-P2P-PINV-${Date.now()}`,
    },
  } as P2pResult;

  // PO：头 + 行（10 × 5 = 50）
  const po = await saveHeadWithLine(
    page, 'ErpPurOrder',
    {
      code: r.codes.po, orgId: SEED.ORG, supplierId: SEED.SUPPLIER, warehouseId: SEED.WH_RAW,
      businessDate: BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
      docStatus: 'ACTIVE', approveStatus: 'UNSUBMITTED', receiveStatus: 'UNRECEIVED',
    }, 'id approveStatus',
    'ErpPurOrderLine',
    { lineNo: 1, materialId: SEED.MAT_1, uoMId: SEED.UOM, quantity: P2P_EXPECT.qty, unitPrice: P2P_EXPECT.price, amount: P2P_EXPECT.invoiceNet },
    'orderId',
  );
  r.po = po.head; r.poLine = po.line;
  await expectApproveStatus(page, 'ErpPurOrder', r.po.id, 'UNSUBMITTED', 'after save');

  await callMutationOk(page, 'ErpPurOrder', 'submitForApproval', { id: r.po.id }, 'id');
  await expectApproveStatus(page, 'ErpPurOrder', r.po.id, 'SUBMITTED', 'after submit');

  await callMutationOk(page, 'ErpPurOrder', 'approve', { id: r.po.id }, 'id');
  await expectApproveStatus(page, 'ErpPurOrder', r.po.id, 'APPROVED', 'after approve');

  // Receive：头 + 行（引用 PO 行，10 × 5）
  const rcv = await saveHeadWithLine(
    page, 'ErpPurReceive',
    {
      code: r.codes.receive, orgId: SEED.ORG, orderId: r.po.id, supplierId: SEED.SUPPLIER, warehouseId: SEED.WH_RAW,
      businessDate: BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
      docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', receiveStatus: 'UNRECEIVED', posted: false,
    }, 'id approveStatus',
    'ErpPurReceiveLine',
    { orderLineId: r.poLine.id, lineNo: 1, materialId: SEED.MAT_1, uoMId: SEED.UOM, quantity: P2P_EXPECT.qty, unitPrice: P2P_EXPECT.price },
    'receiveId',
  );
  r.receive = rcv.head; r.rcvLine = rcv.line;
  await expectApproveStatus(page, 'ErpPurReceive', r.receive.id, 'UNSUBMITTED', 'after save');

  await callMutationOk(page, 'ErpPurReceive', 'submitForApproval', { id: r.receive.id }, 'id');
  await expectApproveStatus(page, 'ErpPurReceive', r.receive.id, 'SUBMITTED', 'after submit');

  await callMutationOk(page, 'ErpPurReceive', 'approve', { id: r.receive.id }, 'id docStatus posted receiveStatus');
  await expectApproveStatus(page, 'ErpPurReceive', r.receive.id, 'APPROVED', 'after approve');

  // Receive approve 触发 INCOMING 移动（relatedBillType=ERP_PUR_RECEIVE, relatedBillCode=receive.code）
  // business-linked → 自动 confirm+complete → DONE + posted（COA 完备后过账成功）
  r.receiveMove = await findFirst(
    page, 'ErpInvStockMove',
    andFilter(eqFilter('relatedBillType', 'ERP_PUR_RECEIVE'), eqFilter('relatedBillCode', r.codes.receive)),
    'id code docStatus posted',
  );

  // Invoice：头 + 行（引用 Receive 行，含税 50+6.5=56.5；三单匹配 qty/price 与 PO 一致）
  const inv = await saveHeadWithLine(
    page, 'ErpPurInvoice',
    {
      code: r.codes.invoice, orgId: SEED.ORG, supplierId: SEED.SUPPLIER,
      businessDate: BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
      totalAmount: P2P_EXPECT.invoiceNet, totalTaxAmount: P2P_EXPECT.invoiceTax, totalAmountWithTax: P2P_EXPECT.invoiceWithTax,
      paidAmount: 0, docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', paidStatus: 'UNPAID', posted: false,
    }, 'id approveStatus',
    'ErpPurInvoiceLine',
    { receiveLineId: r.rcvLine.id, lineNo: 1, materialId: SEED.MAT_1, uoMId: SEED.UOM, quantity: P2P_EXPECT.qty, unitPrice: P2P_EXPECT.price, taxRate: 13 },
    'invoiceId',
  );
  r.invoice = inv.head;
  await expectApproveStatus(page, 'ErpPurInvoice', r.invoice.id, 'UNSUBMITTED', 'after save');

  await callMutationOk(page, 'ErpPurInvoice', 'submitForApproval', { id: r.invoice.id }, 'id');
  await expectApproveStatus(page, 'ErpPurInvoice', r.invoice.id, 'SUBMITTED', 'after submit');

  await callMutationOk(page, 'ErpPurInvoice', 'approve', { id: r.invoice.id }, 'id posted');
  await expectApproveStatus(page, 'ErpPurInvoice', r.invoice.id, 'APPROVED', 'after approve');

  return r;
}

/** 清理 P2P 链路全部产物（下游不可逆产物优先，头最后）。逻辑删除，容忍部分结果。 */
export async function cleanupP2p(page: Page, r: P2pResult): Promise<void> {
  if (!r) return;
  if (r.codes?.invoice) {
    await cleanupArApByCode(page, r.codes.invoice);
    await cleanupVoucherByBillCode(page, r.codes.invoice);
  }
  if (r.invoice) {
    await deleteByFilter(page, 'ErpPurInvoiceLine', eqFilter('invoiceId', Number(r.invoice.id)));
    await deleteById(page, 'ErpPurInvoice', r.invoice.id);
  }
  await cleanupStockMove(page, r.receiveMove, SEED.MAT_1, SEED.WH_RAW);
  if (r.receive) {
    await deleteByFilter(page, 'ErpPurReceiveLine', eqFilter('receiveId', Number(r.receive.id)));
    await deleteById(page, 'ErpPurReceive', r.receive.id);
  }
  if (r.po) {
    await deleteByFilter(page, 'ErpPurOrderLine', eqFilter('orderId', Number(r.po.id)));
    await deleteById(page, 'ErpPurOrder', r.po.id);
  }
}

// ---------- P2P 反向冲销（业财闭环方向二：财务侧红冲 → 采购发票回退） ----------

/**
 * 反向冲销结果。reversalVoucherId 为红字凭证 id（reverseProcess 返回值）。
 * originalVoucherId 为原正常凭证 id（经 voucher_bill_r 回链反查）。
 */
export interface P2pReverseResult extends P2pResult {
  reversalVoucherId?: number;
  originalVoucherId?: number;
}

/**
 * 编排 P2P 正向链（产 posted AP_INVOICE 凭证）→ `ErpFinVoucher__reverse(billHeadCode, businessType)`
 * 财务侧 DIRECT 红冲（plan 2026-07-09-2004-2，M5.2 后端 1452-2）。
 *
 * - 入参形式：`businessType` 经 Nop GraphQL 暴露为 String scalar（Explore 探针裁决：unquoted enum
 *   名被 GraphQL 解析器拒绝 `非法的字符`；quoted 枚举名 `"AP_INVOICE"` 接受）。
 * - 返回值：`reverse` 为标量 Long（非实体），不经 `{ selection }` 选择集——与既有 `callMutation`（总是包
 *   选择集）不兼容，故此处直接构造 `mutation{ X__reverse(...) }` 原始查询。
 * - `reverseProcess` 以**同一 billHeadCode** 写红字凭证的 voucher_bill_r（与原凭证共用 billCode），
 *   故既有 `cleanupVoucherByBillCode` 已覆盖原+红字凭证清理；AR-AP 红冲为既有行 status OPEN→CANCELLED
 *   （非新增行），既有 `cleanupArApByCode` 已覆盖——无需扩展清理原语（Explore 探针裁决）。
 */
export async function runP2pReverse(page: Page): Promise<P2pReverseResult> {
  const base = await runP2pChain(page);
  const billCode = base.codes.invoice;

  // reverse 返回标量 Long，直接构造原始 mutation（无选择集）
  const resp = await page.request.post('/graphql', {
    data: {
      query: `mutation{ ErpFinVoucher__reverse(billHeadCode:${JSON.stringify(billCode)},businessType:${JSON.stringify('AP_INVOICE')}) }`,
    },
  });
  const json: any = await resp.json();
  // GraphQL 成功响应省略 errors 字段（undefined）；失败时为非空数组。用真值检查而非 === null。
  expect(json?.errors, `ErpFinVoucher__reverse(AP_INVOICE) should not return GraphQL errors: ${JSON.stringify(json?.errors)}`).toBeFalsy();
  const reversalVoucherId = json?.data?.ErpFinVoucher__reverse;
  expect(reversalVoucherId, 'ErpFinVoucher__reverse should return reversal voucher id').toBeTruthy();

  const r: P2pReverseResult = { ...base, reversalVoucherId: Number(reversalVoucherId) };

  // 反查原正常凭证 id（经 voucher_bill_r：NORMAL 凭证，非红字）
  const links = await findItems<any>(page, 'ErpFinVoucherBillR', eqFilter('billCode', billCode), 'voucherId');
  for (const lnk of links) {
    const v = await findFirst<any>(page, 'ErpFinVoucher', eqFilter('id', Number(lnk.voucherId)), 'id postingType');
    if (v && v.postingType === 'NORMAL') {
      r.originalVoucherId = Number(v.id);
      break;
    }
  }

  return r;
}

/** 清理 P2P 反向冲销全链产物。复用既有 cleanupP2p（reverseProcess 红字凭证以同 billCode 关联，
 *  既有 cleanupVoucherByBillCode 已覆盖原+红字凭证；AR-AP 既有行取消非新增）。 */
export async function cleanupP2pReverse(page: Page, r: P2pReverseResult): Promise<void> {
  await cleanupP2p(page, r);
}

// ---------- O2C 链路（SO → Delivery → Invoice） ----------

export interface O2cResult {
  setupMove?: { id: any; code: string } | null;
  so?: any; soLine?: any;
  delivery?: any; dlvLine?: any;
  invoice?: any;
  deliveryMove?: { id: any; code: string; docStatus: string; posted: boolean } | null;
  codes: { setup: string; so: string; delivery: string; invoice: string };
}

/** 期望值（确定性）：SO 行 10×10=100；发票含税 100+13=113。备货 20 单覆盖出库 10。 */
export const O2C_EXPECT = { invoiceNet: 100, invoiceTax: 13, invoiceWithTax: 113, qty: 10, price: 10, setupQty: 20 };

export async function runO2cChain(page: Page): Promise<O2cResult> {
  const ts = Date.now();
  const r: O2cResult = {
    codes: {
      setup: `E2E-O2C-SEED-${ts}`,
      so: `E2E-O2C-SO-${ts}`,
      delivery: `E2E-O2C-DLV-${ts}`,
      invoice: `E2E-O2C-SINV-${ts}`,
    },
  } as O2cResult;

  // 前置备货：WH-RAW/MAT-1 种子无余额，出库会因负库存禁止失败。先 INCOMING 备货 20（独立移动 → CONFIRMED → complete → DONE）。
  const setupCreated = await callMutationOk(
    page, 'ErpInvStockMove', 'generateMove',
    {
      request: input(MOVE_REQ_TYPE, {
        moveType: 'INCOMING', orgId: SEED.ORG, businessDate: BDATE,
        destWarehouseId: SEED.WH_RAW, acctSchemaId: SEED.ACCT_SCHEMA, currencyId: SEED.CURRENCY,
        lines: [{ materialId: SEED.MAT_1, uoMId: SEED.UOM, quantity: O2C_EXPECT.setupQty, unitCost: 120, currencyId: SEED.CURRENCY }],
        remark: r.codes.setup,
      }),
    },
    'id code docStatus',
  );
  await callMutationOk(page, 'ErpInvStockMove', 'complete', { moveId: setupCreated.id }, 'id docStatus posted');
  r.setupMove = { id: setupCreated.id, code: setupCreated.code };

  // SO：头 + 行（10 × 10 = 100）
  const so = await saveHeadWithLine(
    page, 'ErpSalOrder',
    {
      code: r.codes.so, orgId: SEED.ORG, customerId: SEED.CUSTOMER, warehouseId: SEED.WH_RAW,
      businessDate: BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
      docStatus: 'ACTIVE', approveStatus: 'UNSUBMITTED', deliveryStatus: 'UNDELIVERED',
    }, 'id approveStatus',
    'ErpSalOrderLine',
    { lineNo: 1, materialId: SEED.MAT_1, uoMId: SEED.UOM, quantity: O2C_EXPECT.qty, unitPrice: O2C_EXPECT.price, amount: O2C_EXPECT.invoiceNet },
    'orderId',
  );
  r.so = so.head; r.soLine = so.line;
  await expectApproveStatus(page, 'ErpSalOrder', r.so.id, 'UNSUBMITTED', 'after save');

  await callMutationOk(page, 'ErpSalOrder', 'submitForApproval', { id: r.so.id }, 'id');
  await expectApproveStatus(page, 'ErpSalOrder', r.so.id, 'SUBMITTED', 'after submit');

  // SO approve：DIRECT 模式 + 信用控制（SOFT_WARNING 默认 + creditLimit=500000 覆盖 113，通过）
  await callMutationOk(page, 'ErpSalOrder', 'approve', { id: r.so.id }, 'id');
  await expectApproveStatus(page, 'ErpSalOrder', r.so.id, 'APPROVED', 'after approve');

  // Delivery：头 + 行（引用 SO 行，10），warehouseId=WH-RAW（备货所在，OUTGOING 源）
  const dlv = await saveHeadWithLine(
    page, 'ErpSalDelivery',
    {
      code: r.codes.delivery, orgId: SEED.ORG, orderId: r.so.id, customerId: SEED.CUSTOMER, warehouseId: SEED.WH_RAW,
      businessDate: BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
      docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', posted: false,
    }, 'id approveStatus',
    'ErpSalDeliveryLine',
    { orderLineId: r.soLine.id, lineNo: 1, materialId: SEED.MAT_1, uoMId: SEED.UOM, quantity: O2C_EXPECT.qty, unitPrice: O2C_EXPECT.price },
    'deliveryId',
  );
  r.delivery = dlv.head; r.dlvLine = dlv.line;
  await expectApproveStatus(page, 'ErpSalDelivery', r.delivery.id, 'UNSUBMITTED', 'after save');

  await callMutationOk(page, 'ErpSalDelivery', 'submitForApproval', { id: r.delivery.id }, 'id');
  await expectApproveStatus(page, 'ErpSalDelivery', r.delivery.id, 'SUBMITTED', 'after submit');

  await callMutationOk(page, 'ErpSalDelivery', 'approve', { id: r.delivery.id }, 'id docStatus posted');
  await expectApproveStatus(page, 'ErpSalDelivery', r.delivery.id, 'APPROVED', 'after approve');

  // Delivery approve 触发 OUTGOING 移动（relatedBillType=ERP_SAL_DELIVERY）
  r.deliveryMove = await findFirst(
    page, 'ErpInvStockMove',
    andFilter(eqFilter('relatedBillType', 'ERP_SAL_DELIVERY'), eqFilter('relatedBillCode', r.codes.delivery)),
    'id code docStatus posted',
  );

  // Invoice：头 + 行（引用 Delivery 行，含税 100+13=113）
  const inv = await saveHeadWithLine(
    page, 'ErpSalInvoice',
    {
      code: r.codes.invoice, orgId: SEED.ORG, customerId: SEED.CUSTOMER,
      businessDate: BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
      totalAmount: O2C_EXPECT.invoiceNet, totalTaxAmount: O2C_EXPECT.invoiceTax, totalAmountWithTax: O2C_EXPECT.invoiceWithTax,
      receivedAmount: 0, docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', receivedStatus: 'UNRECEIVED', posted: false,
    }, 'id approveStatus',
    'ErpSalInvoiceLine',
    { deliveryLineId: r.dlvLine.id, lineNo: 1, materialId: SEED.MAT_1, uoMId: SEED.UOM, quantity: O2C_EXPECT.qty, unitPrice: O2C_EXPECT.price, taxRate: 13 },
    'invoiceId',
  );
  r.invoice = inv.head;
  await expectApproveStatus(page, 'ErpSalInvoice', r.invoice.id, 'UNSUBMITTED', 'after save');

  await callMutationOk(page, 'ErpSalInvoice', 'submitForApproval', { id: r.invoice.id }, 'id');
  await expectApproveStatus(page, 'ErpSalInvoice', r.invoice.id, 'SUBMITTED', 'after submit');

  await callMutationOk(page, 'ErpSalInvoice', 'approve', { id: r.invoice.id }, 'id posted');
  await expectApproveStatus(page, 'ErpSalInvoice', r.invoice.id, 'APPROVED', 'after approve');

  return r;
}

/** 清理 O2C 链路全部产物（含前置备货移动；WH-RAW/MAT-1 余额无种子行，整行删除安全）。 */
export async function cleanupO2c(page: Page, r: O2cResult): Promise<void> {
  if (!r) return;
  if (r.codes?.invoice) {
    await cleanupArApByCode(page, r.codes.invoice);
    await cleanupVoucherByBillCode(page, r.codes.invoice);
  }
  if (r.invoice) {
    await deleteByFilter(page, 'ErpSalInvoiceLine', eqFilter('invoiceId', Number(r.invoice.id)));
    await deleteById(page, 'ErpSalInvoice', r.invoice.id);
  }
  await cleanupStockMove(page, r.deliveryMove, SEED.MAT_1, SEED.WH_RAW);
  if (r.delivery) {
    await deleteByFilter(page, 'ErpSalDeliveryLine', eqFilter('deliveryId', Number(r.delivery.id)));
    await deleteById(page, 'ErpSalDelivery', r.delivery.id);
  }
  if (r.so) {
    await deleteByFilter(page, 'ErpSalOrderLine', eqFilter('orderId', Number(r.so.id)));
    await deleteById(page, 'ErpSalOrder', r.so.id);
  }
  await cleanupStockMove(page, r.setupMove, SEED.MAT_1, SEED.WH_RAW);
}

// ---------- O2C 反向冲销（业财闭环方向二：财务侧红冲 → 销售发票回退） ----------

/**
 * 反向冲销结果。reversalVoucherId 为红字凭证 id；originalVoucherId 为原正常凭证 id。
 */
export interface O2cReverseResult extends O2cResult {
  reversalVoucherId?: number;
  originalVoucherId?: number;
}

/**
 * 编排 O2C 正向链（产 posted AR_INVOICE 凭证，含备货前置）→ `ErpFinVoucher__reverse(billHeadCode, "AR_INVOICE")`
 * 财务侧 DIRECT 红冲（plan 2026-07-09-2004-2 Phase 2）。
 *
 * 入参形式 + 清理范围裁决同 P2P（见 runP2pReverse）：`businessType` 为 String scalar（quoted 枚举名）；
 * reverseProcess 以同一 billHeadCode 写红字凭证 voucher_bill_r，既有 cleanupVoucherByBillCode 已覆盖；
 * AR-AP 红冲为既有行 status→CANCELLED 非新增，既有 cleanupArApByCode 已覆盖——无需扩展清理。
 */
export async function runO2cReverse(page: Page): Promise<O2cReverseResult> {
  const base = await runO2cChain(page);
  const billCode = base.codes.invoice;

  const resp = await page.request.post('/graphql', {
    data: {
      query: `mutation{ ErpFinVoucher__reverse(billHeadCode:${JSON.stringify(billCode)},businessType:${JSON.stringify('AR_INVOICE')}) }`,
    },
  });
  const json: any = await resp.json();
  expect(json?.errors, `ErpFinVoucher__reverse(AR_INVOICE) should not return GraphQL errors: ${JSON.stringify(json?.errors)}`).toBeFalsy();
  const reversalVoucherId = json?.data?.ErpFinVoucher__reverse;
  expect(reversalVoucherId, 'ErpFinVoucher__reverse should return reversal voucher id').toBeTruthy();

  const r: O2cReverseResult = { ...base, reversalVoucherId: Number(reversalVoucherId) };

  const links = await findItems<any>(page, 'ErpFinVoucherBillR', eqFilter('billCode', billCode), 'voucherId');
  for (const lnk of links) {
    const v = await findFirst<any>(page, 'ErpFinVoucher', eqFilter('id', Number(lnk.voucherId)), 'id postingType');
    if (v && v.postingType === 'NORMAL') {
      r.originalVoucherId = Number(v.id);
      break;
    }
  }

  return r;
}

/** 清理 O2C 反向冲销全链产物。复用既有 cleanupO2c（同 P2P 反向裁决）。 */
export async function cleanupO2cReverse(page: Page, r: O2cReverseResult): Promise<void> {
  await cleanupO2c(page, r);
}
