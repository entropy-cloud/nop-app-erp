import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  verifyState,
  findFirst,
  eqFilter,
  andFilter,
  deleteById,
} from './_helper';
import {
  findIntercompanyVoucherIdByBillCode,
  assertVoucherLines,
  cleanupVoucherByBillCode,
  SEED,
} from '../orchestration/_helper';

/**
 * Finance 跨公司 Intercompany PO/SO 配对凭证浏览器层 E2E
 * （plan 2026-07-26-0500-1）。
 *
 * 验证 config-gated `erp-fin.intercompany-posting-enabled=true` 启用后，跨法人 PO/SO approve →
 * `IErpFinIntercompanyTransferBiz.onTradeDocumentApproved` → `IntercompanyVoucherGenerator.generatePairedVouchers`
 * 生成 2 配对凭证（AR 侧 INTERCOMPANY_SALE + AP 侧 INTERCOMPANY_PURCHASE，各 2 行 Dr/Cr）+
 * reverseApprove → `onTradeDocumentReversed` → `reverseIntercompany` 红冲的全栈可达性
 * （owner doc `docs/architecture/multi-company.md §跨公司 PO/SO 触发路径` + §Decision B/C）。
 *
 *   (1) **跨法人 PO approve 配对凭证** —— PO.orgId=买方子公司 → approve → 按 orderCode 反查
 *       INTERCOMPANY_SALE + INTERCOMPANY_PURCHASE 两张凭证 + `assertVoucherLines` 逐行断言
 *       （Dr/Cr 科目 + 金额 = PO.totalAmountWithTax，Decision C：amount 直传 generator 不经 resolver）
 *   (2) **跨法人 SO approve 配对凭证** —— SO.orgId=卖方子公司 → approve → 同型反查 + AR/AP 方向
 *       对称性断言（Decision C：SO 执行方=卖方；AR 始终在 seller、AP 始终在 buyer）
 *   (3) **reverseApprove 红冲** —— PO approve → reverseApprove → 原配对凭证 isReversed=true +
 *       红冲凭证行借贷互换（按 Phase 1 期望值表：dcDirection 不变 + debit↔credit 互换）
 *   (4) **同法人控制对照** —— 同法人组织（种子 ERP-CO id=2，无转移定价规则）PO/SO approve →
 *       显式断言无 INTERCOMPANY 凭证（counterparty=null → skip 零凭证）
 *
 * **自包含跨法人 setup**（不复用固定 orgId=2 的 `runP2pChain`/`runO2cChain`——同法人 skip 无配对凭证）：
 *   - buyerCo（COMPANY，法人根）+ sellerCo（COMPANY，法人根）—— 2 不同法人根
 *   - buyerDiv（DEPARTMENT，parentId=buyerCo）+ sellerDiv（DEPARTMENT，parentId=sellerCo）
 *     —— 子公司挂不同法人根，使 `resolveLegalEntityRoot` walk-up 产出不同法人
 *   - `ErpFinIntercompanyTransferPrice`（fromOrgId=sellerCo，toOrgId=buyerCo，isActive=true）
 *     —— 对手方发现键（trade-document 路径仅读 fromOrgId/toOrgId/isActive，Decision C）
 *
 * 权威实现：
 *   - `ErpFinIntercompanyTransferBizModel.onTradeDocumentApproved:106-151`（跨法人判定 + amount 直传 generator）
 *   - `ErpFinIntercompanyTransferBizModel.resolveCounterpartyLegalEntity:174-192`（PO 查 toOrgId / SO 查 fromOrgId）
 *   - `ErpFinIntercompanyTransferBizModel.resolveLegalEntityRoot:207-226`（parentId 链 walk-up + 环检测）
 *   - `IntercompanyVoucherGenerator.generatePairedVouchers:69-112`（AR Dr1131/Cr5001 + AP Dr1401/Cr2202）
 *   - `IntercompanyVoucherGenerator.writeIntercompanyReversalFromLines:180-251`（红冲：dcDirection 不变 + debit↔credit 互换）
 *   - `ErpPurOrderProcessor.runIntercompanyApproveHook:267-281` / `runIntercompanyReverseHook:287-295`（非阻塞 try-catch）
 *   - `ErpSalOrderProcessor.runIntercompanyApproveHook:304-` / `runIntercompanyReverseHook:323-`（同型）
 *
 * config-gated：`erp-fin.intercompany-posting-enabled=true`（playwright.config.ts webServer JVM arg，默认 false）。
 * 科目为 GlMappingResolver fallback 硬编码（fresh-DB 无 INTERCOMPANY_* gl_mapping_rule 种子 → resolver
 * 返回 null → 回落 1131/5001/1401/2202，均存于 `erp_md_subject.csv` 种子）。
 */

const SUPPLIER = SEED.SUPPLIER;
const CUSTOMER = SEED.CUSTOMER;
const WAREHOUSE = SEED.WH_RAW;
const MAT_1 = SEED.MAT_1;
const UOM = SEED.UOM;
const CURRENCY = SEED.CURRENCY;
const BDATE = '2026-07-09';

/** GlMappingResolver fallback 科目码（fresh-DB 无 INTERCOMPANY_* gl_mapping_rule 种子 → 回落硬编码）。 */
const AR_SUBJECT_CODE = '1131'; // 应收账款（ASSET/DEBIT，种子 id=11）
const REVENUE_SUBJECT_CODE = '5001'; // 主营业务收入（INCOME/CREDIT，种子 id=6）
const COST_SUBJECT_CODE = '1401'; // 原材料（ASSET/DEBIT，种子 id=9）
const AP_SUBJECT_CODE = '2202'; // 应付账款（LIABILITY/CREDIT，种子 id=5）

/** PO 期望值：10 × 6 = 60 含税（配对凭证金额 = order.totalAmountWithTax，Decision C）。 */
const PO_WITH_TAX = 60;
/** SO 期望值：10 × 12 = 120 含税。 */
const SO_WITH_TAX = 120;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

// ---------- 自包含跨法人 setup ----------

interface CrossCompanySetup {
  buyerCo: any;
  sellerCo: any;
  buyerDiv: any;
  sellerDiv: any;
  rule: any;
}

/**
 * 建跨法人组织对（2 COMPANY 法人根 + 2 DEPARTMENT 子公司）+ 转移定价规则。
 *
 * buyerDiv.parentId=buyerCo / sellerDiv.parentId=sellerCo 使 `resolveLegalEntityRoot` walk-up
 * 产出 buyerCo / sellerCo 两个不同法人根。规则 fromOrgId=sellerCo / toOrgId=buyerCo 编码
 * intercompany 交易关系（PO 查 toOrgId=买方法人 → 对手=fromOrgId=卖方；SO 查 fromOrgId=卖方 → 对手=toOrgId=买方）。
 */
async function setupCrossCompanyOrgs(page: import('@playwright/test').Page): Promise<CrossCompanySetup> {
  const ts = Date.now();
  const buyerCo = await createViaSave(
    page, 'ErpMdOrganization',
    { code: `IC-BUYER-${ts}`, name: 'E2E 跨公司买方法人', orgType: 'COMPANY', status: 'ACTIVE', functionalCurrencyId: CURRENCY },
    'id',
  );
  const sellerCo = await createViaSave(
    page, 'ErpMdOrganization',
    { code: `IC-SELLER-${ts}`, name: 'E2E 跨公司卖方法人', orgType: 'COMPANY', status: 'ACTIVE', functionalCurrencyId: CURRENCY },
    'id',
  );
  const buyerDiv = await createViaSave(
    page, 'ErpMdOrganization',
    { code: `IC-BUYER-DIV-${ts}`, name: 'E2E 买方子公司', orgType: 'DEPARTMENT', parentId: buyerCo.id, status: 'ACTIVE', functionalCurrencyId: CURRENCY },
    'id',
  );
  const sellerDiv = await createViaSave(
    page, 'ErpMdOrganization',
    { code: `IC-SELLER-DIV-${ts}`, name: 'E2E 卖方子公司', orgType: 'DEPARTMENT', parentId: sellerCo.id, status: 'ACTIVE', functionalCurrencyId: CURRENCY },
    'id',
  );
  // 转移定价规则：fromOrgId=sellerCo(卖方) / toOrgId=buyerCo(买方) / isActive=true（对手方发现键）。
  // pricingMethod/markupRate/validFrom/validTo 满足实体 NOT NULL + C3 MUTEX（trade-document 路径不读）。
  const rule = await createViaSave(
    page, 'ErpFinIntercompanyTransferPrice',
    {
      code: `IC-RULE-${ts}`, name: 'E2E 跨公司转移定价规则', orgId: buyerCo.id,
      fromOrgId: sellerCo.id, toOrgId: buyerCo.id,
      pricingMethod: 'COST_PLUS', markupRate: 0.1, isActive: true,
      validFrom: BDATE, validTo: '2026-12-31',
    },
    'id',
  );
  return { buyerCo, sellerCo, buyerDiv, sellerDiv, rule };
}

async function cleanupCrossCompanyOrgs(page: import('@playwright/test').Page, s: CrossCompanySetup): Promise<void> {
  if (!s) return;
  if (s.rule) await deleteById(page, 'ErpFinIntercompanyTransferPrice', s.rule.id);
  if (s.buyerDiv) await deleteById(page, 'ErpMdOrganization', s.buyerDiv.id);
  if (s.sellerDiv) await deleteById(page, 'ErpMdOrganization', s.sellerDiv.id);
  if (s.sellerCo) await deleteById(page, 'ErpMdOrganization', s.sellerCo.id);
  if (s.buyerCo) await deleteById(page, 'ErpMdOrganization', s.buyerCo.id);
}

// ---------- 订单 setup（自包含，orgId 指向跨法人子公司） ----------

interface OrderCtx {
  entity: string;
  lineEntity: string;
  orderId?: any;
  orderCode?: string;
  lineIds: any[];
}

async function setupOrder(
  page: import('@playwright/test').Page,
  entity: string,
  lineEntity: string,
  orgId: any,
  partnerField: string,
  partnerId: number,
  totalWithTax: number,
  statusField: 'receiveStatus' | 'deliveryStatus',
): Promise<OrderCtx> {
  const ctx: OrderCtx = { entity, lineEntity, lineIds: [] };
  const code = uniq(`E2E-IC-${entity}`);
  const order = await createViaSave(
    page, entity,
    {
      code, orgId, [partnerField]: partnerId, warehouseId: WAREHOUSE,
      businessDate: BDATE, currencyId: CURRENCY, exchangeRate: 1,
      totalAmount: totalWithTax, totalAmountWithTax: totalWithTax,
      docStatus: 'ACTIVE', approveStatus: 'UNSUBMITTED', [statusField]: statusField === 'receiveStatus' ? 'UNRECEIVED' : 'UNDELIVERED',
    },
    'id approveStatus',
  );
  ctx.orderId = order.id;
  ctx.orderCode = code;
  const line = await createViaSave(
    page, lineEntity,
    { orderId: order.id, lineNo: 1, materialId: MAT_1, uoMId: UOM, quantity: 10, unitPrice: totalWithTax / 10, amount: totalWithTax },
    'id',
  );
  ctx.lineIds.push(line.id);
  return ctx;
}

async function cleanupOrder(page: import('@playwright/test').Page, ctx: OrderCtx): Promise<void> {
  if (!ctx) return;
  // intercompany 配对凭证 + 红冲凭证 + COMMITMENT 凭证（config 启用时同 billCode 共存，cleanupVoucherByBillCode 已 postingType/billType-agnostic 全覆盖）
  if (ctx.orderCode) await cleanupVoucherByBillCode(page, ctx.orderCode);
  for (const id of ctx.lineIds ?? []) await deleteById(page, ctx.lineEntity, id);
  if (ctx.orderId != null) await deleteById(page, ctx.entity, ctx.orderId);
}

// ---------- spec ----------

test.describe('Finance intercompany cross-company PO/SO paired voucher browser-layer E2E', () => {
  test('(1) cross-company PO approve → paired INTERCOMPANY_SALE + INTERCOMPANY_PURCHASE vouchers', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurOrder-main');
    const orgs = await setupCrossCompanyOrgs(page);
    const po = await setupOrder(page, 'ErpPurOrder', 'ErpPurOrderLine', orgs.buyerDiv.id, 'supplierId', SUPPLIER, PO_WITH_TAX, 'receiveStatus');
    try {
      await callMutationOk(page, 'ErpPurOrder', 'submitForApproval', { id: po.orderId }, 'id');
      await callMutationOk(page, 'ErpPurOrder', 'approve', { id: po.orderId }, 'id');

      // PO.orgId=buyerDiv → executingLegal=buyerCo（买方）；rule toOrgId=buyerCo 命中 → counterparty=fromOrgId=sellerCo（卖方）
      // → sellerLegal=sellerCo, buyerLegal=buyerCo → AR 在 seller / AP 在 buyer

      // AR 侧：INTERCOMPANY_SALE 凭证（Dr 1131 / Cr 5001，金额=PO.totalAmountWithTax=60）
      const arVoucherId = await findIntercompanyVoucherIdByBillCode(page, po.orderCode!, 'INTERCOMPANY_SALE', false);
      expect(arVoucherId, 'PO approve should produce INTERCOMPANY_SALE (AR) voucher').toBeTruthy();
      await assertVoucherLines(page, arVoucherId, [
        { subjectCode: AR_SUBJECT_CODE, dcDirection: 'DEBIT', debitAmount: PO_WITH_TAX, creditAmount: 0 },
        { subjectCode: REVENUE_SUBJECT_CODE, dcDirection: 'CREDIT', debitAmount: 0, creditAmount: PO_WITH_TAX },
      ]);

      // AP 侧：INTERCOMPANY_PURCHASE 凭证（Dr 1401 / Cr 2202，金额=60）
      const apVoucherId = await findIntercompanyVoucherIdByBillCode(page, po.orderCode!, 'INTERCOMPANY_PURCHASE', false);
      expect(apVoucherId, 'PO approve should produce INTERCOMPANY_PURCHASE (AP) voucher').toBeTruthy();
      await assertVoucherLines(page, apVoucherId, [
        { subjectCode: COST_SUBJECT_CODE, dcDirection: 'DEBIT', debitAmount: PO_WITH_TAX, creditAmount: 0 },
        { subjectCode: AP_SUBJECT_CODE, dcDirection: 'CREDIT', debitAmount: 0, creditAmount: PO_WITH_TAX },
      ]);
    } finally {
      await cleanupOrder(page, po);
      await cleanupCrossCompanyOrgs(page, orgs);
    }
  });

  test('(2) cross-company SO approve → paired vouchers (AR/AP direction symmetry, Decision C)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpSalOrder-main');
    const orgs = await setupCrossCompanyOrgs(page);
    const so = await setupOrder(page, 'ErpSalOrder', 'ErpSalOrderLine', orgs.sellerDiv.id, 'customerId', CUSTOMER, SO_WITH_TAX, 'deliveryStatus');
    try {
      await callMutationOk(page, 'ErpSalOrder', 'submitForApproval', { id: so.orderId }, 'id');
      await callMutationOk(page, 'ErpSalOrder', 'approve', { id: so.orderId }, 'id');

      // SO.orgId=sellerDiv → executingLegal=sellerCo（卖方）；rule fromOrgId=sellerCo 命中 → counterparty=toOrgId=buyerCo（买方）
      // → sellerLegal=sellerCo, buyerLegal=buyerCo（与 PO 对称：AR 始终在 seller / AP 始终在 buyer，Decision C）

      // AR 侧：INTERCOMPANY_SALE 凭证（Dr 1131 / Cr 5001，金额=SO.totalAmountWithTax=120）
      const arVoucherId = await findIntercompanyVoucherIdByBillCode(page, so.orderCode!, 'INTERCOMPANY_SALE', false);
      expect(arVoucherId, 'SO approve should produce INTERCOMPANY_SALE (AR) voucher').toBeTruthy();
      await assertVoucherLines(page, arVoucherId, [
        { subjectCode: AR_SUBJECT_CODE, dcDirection: 'DEBIT', debitAmount: SO_WITH_TAX, creditAmount: 0 },
        { subjectCode: REVENUE_SUBJECT_CODE, dcDirection: 'CREDIT', debitAmount: 0, creditAmount: SO_WITH_TAX },
      ]);

      // AP 侧：INTERCOMPANY_PURCHASE 凭证（Dr 1401 / Cr 2202，金额=120）
      const apVoucherId = await findIntercompanyVoucherIdByBillCode(page, so.orderCode!, 'INTERCOMPANY_PURCHASE', false);
      expect(apVoucherId, 'SO approve should produce INTERCOMPANY_PURCHASE (AP) voucher').toBeTruthy();
      await assertVoucherLines(page, apVoucherId, [
        { subjectCode: COST_SUBJECT_CODE, dcDirection: 'DEBIT', debitAmount: SO_WITH_TAX, creditAmount: 0 },
        { subjectCode: AP_SUBJECT_CODE, dcDirection: 'CREDIT', debitAmount: 0, creditAmount: SO_WITH_TAX },
      ]);
    } finally {
      await cleanupOrder(page, so);
      await cleanupCrossCompanyOrgs(page, orgs);
    }
  });

  test('(3) cross-company PO reverseApprove → paired vouchers reversed (dcDirection unchanged + debit/credit swapped)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurOrder-main');
    const orgs = await setupCrossCompanyOrgs(page);
    const po = await setupOrder(page, 'ErpPurOrder', 'ErpPurOrderLine', orgs.buyerDiv.id, 'supplierId', SUPPLIER, PO_WITH_TAX, 'receiveStatus');
    try {
      await callMutationOk(page, 'ErpPurOrder', 'submitForApproval', { id: po.orderId }, 'id');
      await callMutationOk(page, 'ErpPurOrder', 'approve', { id: po.orderId }, 'id');

      const arOriginalId = await findIntercompanyVoucherIdByBillCode(page, po.orderCode!, 'INTERCOMPANY_SALE', false);
      const apOriginalId = await findIntercompanyVoucherIdByBillCode(page, po.orderCode!, 'INTERCOMPANY_PURCHASE', false);
      expect(arOriginalId, 'pre: forward INTERCOMPANY_SALE voucher should exist').toBeTruthy();
      expect(apOriginalId, 'pre: forward INTERCOMPANY_PURCHASE voucher should exist').toBeTruthy();

      // reverseApprove → onTradeDocumentReversed → reverseIntercompany 红冲两张配对凭证
      await callMutationOk(page, 'ErpPurOrder', 'reverseApprove', { id: po.orderId }, 'id');

      // 原凭证 isReversed=true（经 __get 权威查库）
      const arOrig = await findFirst<any>(page, 'ErpFinVoucher', eqFilter('id', arOriginalId), 'id isReversed reversalOfVoucherId');
      expect(arOrig?.isReversed, 'original INTERCOMPANY_SALE isReversed=true after reverseApprove').toBe(true);
      const apOrig = await findFirst<any>(page, 'ErpFinVoucher', eqFilter('id', apOriginalId), 'id isReversed reversalOfVoucherId');
      expect(apOrig?.isReversed, 'original INTERCOMPANY_PURCHASE isReversed=true after reverseApprove').toBe(true);

      // 红冲凭证存在（reversalOfVoucherId 非空区分）
      const arReversalId = await findIntercompanyVoucherIdByBillCode(page, po.orderCode!, 'INTERCOMPANY_SALE', true);
      expect(arReversalId, 'reverseApprove should produce reversal INTERCOMPANY_SALE voucher').toBeTruthy();
      const apReversalId = await findIntercompanyVoucherIdByBillCode(page, po.orderCode!, 'INTERCOMPANY_PURCHASE', true);
      expect(apReversalId, 'reverseApprove should produce reversal INTERCOMPANY_PURCHASE voucher').toBeTruthy();

      // 红冲行：dcDirection 不变 + debit↔credit 互换（按 Phase 1 期望值表）
      // 红冲 AR：1131 DEBIT debit=0/credit=60；5001 CREDIT debit=60/credit=0
      await assertVoucherLines(page, arReversalId, [
        { subjectCode: AR_SUBJECT_CODE, dcDirection: 'DEBIT', debitAmount: 0, creditAmount: PO_WITH_TAX },
        { subjectCode: REVENUE_SUBJECT_CODE, dcDirection: 'CREDIT', debitAmount: PO_WITH_TAX, creditAmount: 0 },
      ]);
      // 红冲 AP：1401 DEBIT debit=0/credit=60；2202 CREDIT debit=60/credit=0
      await assertVoucherLines(page, apReversalId, [
        { subjectCode: COST_SUBJECT_CODE, dcDirection: 'DEBIT', debitAmount: 0, creditAmount: PO_WITH_TAX },
        { subjectCode: AP_SUBJECT_CODE, dcDirection: 'CREDIT', debitAmount: PO_WITH_TAX, creditAmount: 0 },
      ]);
    } finally {
      await cleanupOrder(page, po);
      await cleanupCrossCompanyOrgs(page, orgs);
    }
  });

  test('(4) same-legal-entity control: PO/SO approve on seed org (no transfer pricing rule) → no INTERCOMPANY vouchers', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPurOrder-main');

    // 种子 ERP-CO id=2 是 COMPANY 法人根，但无转移定价规则 → resolveCounterpartyLegalEntity 返回 null → skip 零凭证
    const po = await setupOrder(page, 'ErpPurOrder', 'ErpPurOrderLine', SEED.ORG, 'supplierId', SUPPLIER, PO_WITH_TAX, 'receiveStatus');
    const so = await setupOrder(page, 'ErpSalOrder', 'ErpSalOrderLine', SEED.ORG, 'customerId', CUSTOMER, SO_WITH_TAX, 'deliveryStatus');
    try {
      await callMutationOk(page, 'ErpPurOrder', 'submitForApproval', { id: po.orderId }, 'id');
      await callMutationOk(page, 'ErpPurOrder', 'approve', { id: po.orderId }, 'id');
      await callMutationOk(page, 'ErpSalOrder', 'submitForApproval', { id: so.orderId }, 'id');
      await callMutationOk(page, 'ErpSalOrder', 'approve', { id: so.orderId }, 'id');

      // 显式断言无 INTERCOMPANY 配对凭证（经 voucher_bill_r 按 billCode + billType 反查返回空）
      const poAr = await findIntercompanyVoucherIdByBillCode(page, po.orderCode!, 'INTERCOMPANY_SALE', false);
      const poAp = await findIntercompanyVoucherIdByBillCode(page, po.orderCode!, 'INTERCOMPANY_PURCHASE', false);
      expect(poAr, 'same-org PO approve should NOT produce INTERCOMPANY_SALE voucher').toBeNull();
      expect(poAp, 'same-org PO approve should NOT produce INTERCOMPANY_PURCHASE voucher').toBeNull();

      const soAr = await findIntercompanyVoucherIdByBillCode(page, so.orderCode!, 'INTERCOMPANY_SALE', false);
      const soAp = await findIntercompanyVoucherIdByBillCode(page, so.orderCode!, 'INTERCOMPANY_PURCHASE', false);
      expect(soAr, 'same-org SO approve should NOT produce INTERCOMPANY_SALE voucher').toBeNull();
      expect(soAp, 'same-org SO approve should NOT produce INTERCOMPANY_PURCHASE voucher').toBeNull();
    } finally {
      await cleanupOrder(page, po);
      await cleanupOrder(page, so);
    }
  });
});

void SEED;
