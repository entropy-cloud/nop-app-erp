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
  callMutation,
  callMutationOk,
  verifyState,
  findPageTotal,
  eqFilter,
  andFilter,
  deleteByFilter,
  deleteById,
  input,
} from '../business-actions/_helper';
export { createViaSave, callMutation, callMutationOk, verifyState, findPageTotal, eqFilter, andFilter, deleteByFilter, deleteById, input };

/** 种子引用（master-data init-data，与 0814-2 inventory spec 一致）。 */
export const SEED = {
  ORG: 2,
  WH_RAW: 2,        // WH-RAW 原料仓（种子中 MAT-1 无余额，备货+清理安全）
  MAT_1: 1,         // MAT-001 ERP 标准型产品甲（FINISHED_PRODUCT, MOVING_AVERAGE, uom=1）
  UOM: 1,           // PCS 个
  UOM_KG: 2,        // KG 千克（制造链测试专用组件物料计量单位）
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

// ---------- 制造链编排（WorkOrder + MaterialIssue + JobCard 三聚合根协作，plan 2026-07-10-0704-2） ----------
//
// 镜像 P2P/O2C 范式，经 GraphQL /graphql 驱动 WorkOrder 完整制造链：
//   前置备货（组件物料建库存）→ BOM+行 → WorkOrder+OUTPUT/INPUT 行 → 审批轴（submit→approve）
//   → 齐套校验（checkAvailability→STOCK_RESERVED）→ 开工（start→IN_PROCESS）
//   → 领料出库（MaterialIssue+行→confirm，触发 OUTGOING 移动 + WorkOrder.materialCost 回写）
//   → 报工（JobCard→startJob→recordWork，回写 WorkOrder.laborCost→submitJob→completeJob）
//   → 完工入库（reportCompletion，触发 MANUFACTURE 入库移动 + WorkOrder.totalCost/unitCost 重算 + COMPLETED）。
//
// 跨聚合根协作产物：
//   - MaterialIssue.confirm → ErpInvStockMove(relatedBillType=ERP_MFG_ISSUE) OUTGOING + WorkOrder.materialCost
//   - JobCard.recordWork → ErpMfgJobCardTimeLog + WorkOrder.laborCost
//   - reportCompletion → ErpInvStockMove(relatedBillType=ERP_MFG_WORK_ORDER) MANUFACTURE + WorkOrder COMPLETED
// 完工入库 GL 过账（MANUFACTURING_RECEIPT：Dr Inventory / Cr WIP）+ 领料 GL 过账（MANUFACTURING_ISSUE：
// Dr WIP / Cr Inventory）已由 plan 2026-07-10-1100-5 落地，完工/领料移动单 posted=true。

export interface MfgResult {
  componentMat?: any;
  /** 成品物料（默认 MAT-001；差异 spec 使用测试专用成品物料时置此，cleanupMfg 据此清理成品余额）。 */
  productMat?: any;
  setupMove?: { id: any; code: string } | null;
  bom?: any; bomLine?: any;
  wo?: any; woOutputLine?: any; woInputLine?: any;
  issue?: any; issueLine?: any;
  jobCard?: any;
  /** 输入批次（withBatchTracking 时测试创建，cleanupMfg 据此清理）。 */
  inputBatch?: any;
  completionMove?: { id: any; code: string; docStatus: string; posted: boolean } | null;
  codes: { component: string; setup: string; bom: string; wo: string; issue: string; jobCard: string };
}

/**
 * runMfgChain 可选参数。productId 默认 MAT-001（0704-2 基线）；差异 spec 传入测试专用成品物料。
 *（plan 2026-07-10-1800-2 Phase 3：自包含差异链路与 MAT-001 链路隔离。）
 *
 * plan 2026-07-11-0730-1 扩展：
 *   - withBatchTracking：创建输入 ErpInvBatch + 设 MaterialIssueLine.batchNo，触发完工时
 *     BatchGenealogyWriter 写入 inputLot→outputLot 基因链（默认 false，不影响既有 spec）。
 *   - inspectionRequired：BOM 经 __save 时设 inspectionRequired=true，使 Gate 1
 *     （config erp-mfg.inspection-gate-enabled=true）命中满量完工。当 true 时链路停在
 *     reportCompletion 之前（WO=IN_PROCESS），由 spec 驱动 reportCompletion 断言 ERR_INSPECTION_REQUIRED。
 */
export interface MfgChainOptions {
  productId?: number;
  productUoMId?: number;
  withBatchTracking?: boolean;
  inspectionRequired?: boolean;
}

/**
 * 期望值（确定性派生）：
 *   组件用量 bomLineQty=2/单位 × plannedQty=10 → 齐套需求 20；备货 30 覆盖。
 *   materialCost = 出库 20 × moving-average unitCost 50 = 1000（组件为测试专用物料，无种子余额，无混合）。
 *   laborCost = durationMins 60 / 60 × hourlyRate 100 = 100。
 *   totalCost = material(1000) + labor(100) = 1100；unitCost = 1100 / completed 10 = 110。
 */
export const MFG_EXPECT = {
  plannedQty: 10, completedQty: 10,
  setupQty: 30, componentUnitCost: 50,
  bomLineQty: 2, componentRequirement: 20,
  materialCost: 1000, laborCost: 100, totalCost: 1100, unitCost: 110,
  durationMins: 60, hourlyRate: 100,
} as const;

const MFG_MOVE_REQ_TYPE = 'i_app_erp_inv_biz_StockMoveRequest';
const MFG_BDATE = '2026-07-10';

/**
 * 编排 WorkOrder 完整制造链（三聚合根协作）。返回各实体 id + 下游移动单供 spec 断言/清理。
 *
 * 组件物料为**测试专用新建物料**（非种子 MAT-003），无种子余额，使 materialCost 确定性（无 WEIGHTED_AVERAGE
 * 混合）且清理安全（整行删除余额不污染种子库存基线——inventory dashboard totalValue 读 stock_balance）。
 *
 * 步骤链路（每步状态翻转在 helper 内 verifyState 断言，spec 层仅断言跨聚合根协作产物）：
 *   1. 组件物料：createViaSave 测试专用 RAW_MATERIAL（MOVING_AVERAGE，无种子余额）
 *   2. 前置备货：generateMove INCOMING 为组件物料建库存（unitCost=50 确定性 → moving-average 50）
 *   3. BOM + 行：MAT-001 成品 BOM + 组件物料行（用量 2/单位）
 *   4. WorkOrder + 行：MAT-001 + bomId + plannedQty=10；OUTPUT 行（成品 + destWarehouseId，generateCompletionMove 必读）
 *      + INPUT 行（组件 + 需求量）
 *   5. 审批轴：submitForApproval → approve（docStatus DRAFT→SUBMITTED→NOT_STARTED）
 *   6. 齐套校验：checkAvailability → STOCK_RESERVED（BOM 展开组件需求 20 ≤ 库存 30）
 *   7. 开工：start → IN_PROCESS
 *   8. 领料出库：MaterialIssue + 行（引用 WorkOrder INPUT 行）→ confirm → OUTGOING 移动 + materialCost 回写
 *   9. 报工：JobCard → startJob → recordWork → laborCost 回写 → submitJob → completeJob
 *   10. 完工入库：reportCompletion(completedQty=10) → MANUFACTURE 入库移动 + totalCost/unitCost + COMPLETED
 */
export async function runMfgChain(page: Page, options?: MfgChainOptions): Promise<MfgResult> {
  const productId = options?.productId ?? SEED.MAT_1;
  const productUoMId = options?.productUoMId ?? SEED.UOM;
  const ts = Date.now();
  const r: MfgResult = {
    codes: {
      component: `E2E-MFG-MAT-${ts}`,
      setup: `E2E-MFG-SEED-${ts}`,
      bom: `E2E-MFG-BOM-${ts}`,
      wo: `E2E-MFG-WO-${ts}`,
      issue: `E2E-MFG-ISSUE-${ts}`,
      jobCard: `E2E-MFG-JC-${ts}`,
    },
  } as MfgResult;

  // 1. 组件物料：测试专用 RAW_MATERIAL（无种子余额 → 确定性 unitCost + 清理安全）
  const componentMat = await createViaSave(
    page, 'ErpMdMaterial',
    {
      code: r.codes.component, name: 'E2E 测试组件原料',
      materialType: 'RAW_MATERIAL', uoMId: SEED.UOM_KG, status: 'ACTIVE',
      costMethod: 'MOVING_AVERAGE', defaultWarehouseId: SEED.WH_RAW,
    },
    'id',
  );
  r.componentMat = componentMat;

  // withBatchTracking：先创建输入 ErpInvBatch + 备货移动携带 batchNo，使库存余额按批次维度落入。
  //   StockMoveBookkeeper.upsertBalance 以 batchNo 为余额键维度之一，故备货/领料须共用同一 batchNo。
  //   完工时 BatchGenealogyWriter.resolveInputLot 据此 batchNo 反查 ErpInvBatch 得 inputLotId。
  const inputBatchNo = `GEN-IN-${ts}`;
  if (options?.withBatchTracking) {
    r.inputBatch = await createViaSave(
      page, 'ErpInvBatch',
      {
        orgId: SEED.ORG, batchNo: inputBatchNo,
        materialId: componentMat.id, warehouseId: SEED.WH_RAW,
        totalQuantity: MFG_EXPECT.setupQty,
        availableQuantity: MFG_EXPECT.setupQty,
        productionDate: MFG_BDATE, status: 'OPEN',
      },
      'id',
    );
  }

  // 2. 前置备货：组件物料无种子余额，齐套校验/领料出库需库存余额。INCOMING 备货 30（独立移动 → CONFIRMED → complete → DONE）。
  //    withBatchTracking：备货行携带 batchNo，余额按批次维度建立（领料出库按批次扣减时可用量充足）。
  const setupCreated = await callMutationOk(
    page, 'ErpInvStockMove', 'generateMove',
    {
      request: input(MFG_MOVE_REQ_TYPE, {
        moveType: 'INCOMING', orgId: SEED.ORG, businessDate: MFG_BDATE,
        destWarehouseId: SEED.WH_RAW, currencyId: SEED.CURRENCY,
        lines: [{
          materialId: componentMat.id, uoMId: SEED.UOM_KG,
          quantity: MFG_EXPECT.setupQty, unitCost: MFG_EXPECT.componentUnitCost, currencyId: SEED.CURRENCY,
          ...(options?.withBatchTracking ? { batchNo: inputBatchNo } : {}),
        }],
        remark: r.codes.setup,
      }),
    },
    'id code docStatus',
  );
  await callMutationOk(page, 'ErpInvStockMove', 'complete', { moveId: setupCreated.id }, 'id docStatus posted');
  r.setupMove = { id: setupCreated.id, code: setupCreated.code };

  // 3. BOM + 行：成品 + 组件物料（用量 2/单位）。inspectionRequired=true 时置 BOM 字段触发 Gate 1。
  const bom = await createViaSave(
    page, 'ErpMfgBom',
    {
      code: r.codes.bom, productId, bomType: 'NORMAL', qty: 1, isActive: true,
      ...(options?.inspectionRequired ? { inspectionRequired: true } : {}),
    },
    'id',
  );
  r.bom = bom;
  r.bomLine = await createViaSave(
    page, 'ErpMfgBomLine',
    { bomId: bom.id, lineNo: 1, materialId: componentMat.id, uoMId: SEED.UOM_KG, quantity: MFG_EXPECT.bomLineQty, warehouseId: SEED.WH_RAW },
    'id',
  );

  // 4. WorkOrder + 行：成品 + bomId + plannedQty=10
  const wo = await createViaSave(
    page, 'ErpMfgWorkOrder',
    {
      code: r.codes.wo, orgId: SEED.ORG, bomId: bom.id, productId,
      plannedQuantity: MFG_EXPECT.plannedQty, businessDate: MFG_BDATE,
      currencyId: SEED.CURRENCY, exchangeRate: 1,
      docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED',
    },
    'id approveStatus docStatus',
  );
  r.wo = wo;
  // OUTPUT 行：成品产出，destWarehouseId 必填（generateCompletionMove 读此生成入库移动，null 时静默跳过）
  r.woOutputLine = await createViaSave(
    page, 'ErpMfgWorkOrderLine',
    {
      workOrderId: wo.id, lineNo: 1, lineType: 'OUTPUT',
      materialId: productId, uoMId: productUoMId, plannedQuantity: MFG_EXPECT.plannedQty,
      destWarehouseId: SEED.WH_RAW,
    },
    'id',
  );
  // INPUT 行：组件投入，sourceWarehouseId=备货所在仓
  r.woInputLine = await createViaSave(
    page, 'ErpMfgWorkOrderLine',
    {
      workOrderId: wo.id, lineNo: 2, lineType: 'INPUT',
      materialId: componentMat.id, uoMId: SEED.UOM_KG, plannedQuantity: MFG_EXPECT.componentRequirement,
      sourceWarehouseId: SEED.WH_RAW,
    },
    'id',
  );

  // 5. 审批轴：submit → SUBMITTED → approve → APPROVED + docStatus=NOT_STARTED
  await callMutationOk(page, 'ErpMfgWorkOrder', 'submitForApproval', { id: wo.id }, 'id');
  await expectApproveStatus(page, 'ErpMfgWorkOrder', wo.id, 'SUBMITTED', 'after submit');
  await callMutationOk(page, 'ErpMfgWorkOrder', 'approve', { id: wo.id }, 'id');
  await expectApproveStatus(page, 'ErpMfgWorkOrder', wo.id, 'APPROVED', 'after approve');
  let ws = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus');
  expect(ws?.docStatus, 'after approve docStatus=NOT_STARTED').toBe('NOT_STARTED');

  // 6. 齐套校验：BOM 展开组件需求 20 ≤ 库存 30 → STOCK_RESERVED
  await callMutationOk(page, 'ErpMfgWorkOrder', 'checkAvailability', { workOrderId: wo.id }, 'id docStatus');
  ws = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus');
  expect(ws?.docStatus, 'after checkAvailability docStatus=STOCK_RESERVED').toBe('STOCK_RESERVED');

  // 7. 开工 → IN_PROCESS
  await callMutationOk(page, 'ErpMfgWorkOrder', 'start', { workOrderId: wo.id }, 'id docStatus');
  ws = await verifyState(page, 'ErpMfgWorkOrder', wo.id, 'docStatus');
  expect(ws?.docStatus, 'after start docStatus=IN_PROCESS').toBe('IN_PROCESS');

  // 8. 领料出库：MaterialIssue + 行（引用 WorkOrder INPUT 行）→ confirm → OUTGOING 移动 + materialCost 回写
  //    withBatchTracking：MaterialIssueLine.batchNo 引用输入批次，MaterialIssueStockMoveBuilder 透传 batchNo
  //    至出库移动行 → 按批次余额扣减；完工时 BatchGenealogyWriter.findIssueLinesWithBatch 命中 → 写入基因链。
  const issue = await createViaSave(
    page, 'ErpMfgMaterialIssue',
    {
      code: r.codes.issue, orgId: SEED.ORG, workOrderId: wo.id, warehouseId: SEED.WH_RAW,
      businessDate: MFG_BDATE, currencyId: SEED.CURRENCY, exchangeRate: 1,
      docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED',
    },
    'id',
  );
  r.issue = issue;
  r.issueLine = await createViaSave(
    page, 'ErpMfgMaterialIssueLine',
    {
      issueId: issue.id, lineNo: 1, materialId: componentMat.id, uoMId: SEED.UOM_KG,
      workOrderLineId: r.woInputLine.id,
      requiredQuantity: MFG_EXPECT.componentRequirement, issuedQuantity: MFG_EXPECT.componentRequirement,
      ...(options?.withBatchTracking ? { batchNo: inputBatchNo } : {}),
    },
    'id',
  );
  // confirm：DRAFT→CONFIRMED→DONE，经 IErpInvStockMoveBiz.generateMove 生成 OUTGOING 移动（业务联动自动 DONE）+ WorkOrder.materialCost 回写
  await callMutationOk(page, 'ErpMfgMaterialIssue', 'confirm', { issueId: issue.id }, 'id docStatus');

  // 9. 报工：JobCard → startJob → recordWork（回写 WorkOrder.laborCost）→ submitJob → completeJob
  const jobCard = await createViaSave(
    page, 'ErpMfgJobCard',
    {
      workOrderId: wo.id, lineNo: 1, code: r.codes.jobCard,
      plannedQuantity: MFG_EXPECT.plannedQty, status: 'OPEN',
    },
    'id status',
  );
  r.jobCard = jobCard;
  await callMutationOk(page, 'ErpMfgJobCard', 'startJob', { jobCardId: jobCard.id }, 'id status');
  // recordWork 入参 @RequestBean JobCardWorkRecord 经 GraphQL 展平为独立标量参数（非 record 包装，见后端
  // TestErpMfgWorkOrderEndToEnd#recordWorkRequest 范式）。人工成本 = durationMins/60 × hourlyRate → WorkOrder.laborCost。
  await callMutationOk(
    page, 'ErpMfgJobCard', 'recordWork',
    {
      jobCardId: jobCard.id, operatorId: '1', workDate: MFG_BDATE,
      durationMins: MFG_EXPECT.durationMins, setupMins: 0, runMins: MFG_EXPECT.durationMins,
      hourlyRate: MFG_EXPECT.hourlyRate, completedQuantity: 0, scrappedQuantity: 0,
    },
    'id status',
  );
  await callMutationOk(page, 'ErpMfgJobCard', 'submitJob', { jobCardId: jobCard.id }, 'id status');
  await callMutationOk(page, 'ErpMfgJobCard', 'completeJob', { jobCardId: jobCard.id }, 'id status');

  // 10. 完工入库：reportCompletion → MANUFACTURE 入库移动 + totalCost/unitCost 重算 + COMPLETED
  //     inspectionRequired=true 时跳过：Gate 1（config+BOM.inspectionRequired）命中满量完工会抛
  //     ERR_INSPECTION_REQUIRED，由 spec 驱动 reportCompletion 断言负路径。链路停在 completeJob（WO=IN_PROCESS）。
  if (!options?.inspectionRequired) {
    await callMutationOk(
      page, 'ErpMfgWorkOrder', 'reportCompletion',
      { workOrderId: wo.id, completedQty: MFG_EXPECT.completedQty },
      'id docStatus completedQuantity materialCost laborCost totalCost unitCost posted',
    );
    ws = await verifyState(
      page, 'ErpMfgWorkOrder', wo.id,
      'docStatus completedQuantity materialCost laborCost totalCost unitCost posted',
    );
    expect(ws?.docStatus, 'after reportCompletion docStatus=COMPLETED').toBe('COMPLETED');

    // 完工入库移动（MANUFACTURE，relatedBillType=ERP_MFG_WORK_ORDER，业务联动自动 DONE）
    r.completionMove = await findFirst(
      page, 'ErpInvStockMove',
      andFilter(eqFilter('relatedBillType', 'ERP_MFG_WORK_ORDER'), eqFilter('relatedBillCode', r.codes.wo)),
      'id code docStatus posted',
    );
  }

  return r;
}

/**
 * 清理制造链全部产物（下游不可逆产物优先，头最后）。逻辑删除，容忍部分结果。
 *
 * 清理顺序（依赖反向）：完工入库移动（成品 MAT-001 余额）→ MaterialIssue(行) → JobCard+TimeLog
 *   → 领料出库移动（组件物料余额）→ WorkOrderLine → WorkOrder → BOM(行)
 *   → 前置备货移动（组件物料余额）→ 测试专用组件物料。
 * 成品 MAT-001 / 测试组件物料在 WH-RAW 均无种子余额行，整行删除余额安全（不污染 inventory dashboard totalValue 基线）。
 */
export async function cleanupMfg(page: Page, r: MfgResult): Promise<void> {
  if (!r) return;
  const finishedProductId = r.productMat?.id ?? SEED.MAT_1;
  // 批次基因链产物清理（plan 2026-07-11-0730-1，withBatchTracking 完工触发 BatchGenealogyWriter 写入）：
  //   基因链行（filter workOrderId）→ 输出批次（batchNo=FG-{woCode}，ensureOutputLot 创建）→ 输入批次（测试创建）。
  if (r.wo) {
    await deleteByFilter(page, 'ErpMfgBatchGenealogy', eqFilter('workOrderId', Number(r.wo.id)));
  }
  if (r.codes?.wo) {
    await deleteByFilter(page, 'ErpInvBatch', eqFilter('batchNo', `FG-${r.codes.wo}`));
  }
  if (r.inputBatch) {
    await deleteById(page, 'ErpInvBatch', r.inputBatch.id);
  }
  // 生产差异记录清理（config erp-mfg.variance-auto-calc-enabled 时 willFinish 完工触发；
  // MAT-001 链路无 FIRMED rollup → calculateVariances 抛异常被吞 → 无差异记录 → deleteByFilter 空操作安全）
  if (r.wo) {
    await deleteByFilter(page, 'ErpMfgCostVariance', eqFilter('workOrderId', Number(r.wo.id)));
  }
  // PRODUCTION_VARIANCE 凭证清理（billHeadCode = woCode + '-PV'，ProductionVarianceDispatcher 后缀）
  if (r.codes?.wo) {
    await cleanupVoucherByBillCode(page, r.codes.wo + '-PV');
  }
  // 完工入库移动 + 成品余额（MAT-001 或测试专用成品在 WH-RAW 均无种子余额行，整行删除安全）
  await cleanupStockMove(page, r.completionMove, finishedProductId, SEED.WH_RAW);
  // MaterialIssue 行 + 头
  if (r.issue) {
    await deleteByFilter(page, 'ErpMfgMaterialIssueLine', eqFilter('issueId', Number(r.issue.id)));
    await deleteById(page, 'ErpMfgMaterialIssue', r.issue.id);
  }
  // 领料出库移动（OUTGOING，relatedBillType=ERP_MFG_ISSUE，relatedBillCode=issue.code）+ 组件物料余额
  const componentId = r.componentMat?.id;
  if (r.codes?.issue) {
    // 领料 GL 凭证业财回链 billCode = issue.code + '-MI'（ManufacturingIssuePostingDispatcher），
    // 与出库移动单 code 不同，需单独清理（cleanupStockMove 仅按 move.code 清理）
    await cleanupVoucherByBillCode(page, r.codes.issue + '-MI');
    const issueMove = await findFirst<any>(
      page, 'ErpInvStockMove',
      andFilter(eqFilter('relatedBillType', 'ERP_MFG_ISSUE'), eqFilter('relatedBillCode', r.codes.issue)),
      'id code',
    );
    if (componentId != null) await cleanupStockMove(page, issueMove, componentId, SEED.WH_RAW);
    else await cleanupStockMove(page, issueMove);
  }
  // JobCard TimeLog + JobCard
  if (r.jobCard) {
    await deleteByFilter(page, 'ErpMfgJobCardTimeLog', eqFilter('jobCardId', Number(r.jobCard.id)));
    await deleteById(page, 'ErpMfgJobCard', r.jobCard.id);
  }
  // WorkOrderLine + WorkOrder
  if (r.wo) {
    await deleteByFilter(page, 'ErpMfgWorkOrderLine', eqFilter('workOrderId', Number(r.wo.id)));
    await deleteById(page, 'ErpMfgWorkOrder', r.wo.id);
  }
  // BOMLine + BOM
  if (r.bom) {
    await deleteByFilter(page, 'ErpMfgBomLine', eqFilter('bomId', Number(r.bom.id)));
    await deleteById(page, 'ErpMfgBom', r.bom.id);
  }
  // 前置备货移动 + 组件物料余额（测试专用物料无种子余额，整行删除安全）
  if (componentId != null) await cleanupStockMove(page, r.setupMove, componentId, SEED.WH_RAW);
  else await cleanupStockMove(page, r.setupMove);
  // 测试专用组件物料
  if (r.componentMat) {
    await deleteById(page, 'ErpMdMaterial', r.componentMat.id);
  }
}
