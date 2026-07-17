import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, findFirst, deleteByFilter, deleteById, eqFilter, GraphQLClient } from './_helper';

/**
 * contract ErpCtInvoicePlan 发票计划触发跨域编排业务动作浏览器层 E2E（plan 2026-07-14-0941-1 Phase 2）。
 *
 * 验证 triggerInvoice / triggerDuePlans 经 GraphQL /graphql 的全栈可达性 + AP/AR 发票草稿创建 + plan 回写。
 *
 * 权威实现（ErpCtInvoicePlanBizModel.triggerInvoice:60 / triggerDuePlans:99，
 * 对齐 docs/design/contract/state-machine.md §InvoicePlan 触发）：
 *   triggerInvoice(planId) 守卫顺序：
 *     1. plan.isInvoiced == true → ERR_CT_INVOICE_PLAN_ALREADY_INVOICED（token「已生成发票，不可重复触发」）
 *     2. contract.status == SUSPENDED → ERR_CT_CONTRACT_SUSPENDED（token「已中止...不可触发生成新发票」）
 *     3. contract.status != ACTIVE → ERR_CT_CONTRACT_NOT_ACTIVE（token「非执行中」）
 *   通过后：contractDirection=INBOUND → createApInvoiceDraft（ErpPurInvoice code=`CT-INV-{planId}`，
 *     supplierId=contract.partnerId, totalAmount=plan.amount, posted=false）
 *     OUTBOUND → createArInvoiceDraft（ErpSalInvoice code=`CT-INV-{planId}`，
 *     customerId=contract.partnerId, totalAmount=plan.amount, posted=false）
 *   回写：plan.isInvoiced=true / invoiceBillCode=`CT-INV-{planId}` / invoiceDate=today。
 *
 *   triggerDuePlans(contractId, asOfDate) config-gated（erp-ct.invoiceplan-auto-trigger 默认 true）：
 *     扫描 planDate <= asOfDate + isInvoiced=false 的 plan，逐个调 triggerInvoice；返回触发行数。
 *
 * ORM 无 useWorkflow / 无 useApproval，纯 DIRECT @BizMutation 浏览器层可达。
 *
 * 种子引用：partner id=1（CUST-001 客户，OUTBOUND→AR）/ id=3（SUP-001 供应商，INBOUND→AP）；
 *   org id=2 / currency id=1 / material id=4。
 *
 * 自包含 setup：经 __save 直置合同 NEGOTIATION 入口（DRAFT→NEGOTIATION 无 @BizMutation）→ activate(ACTIVE)
 *   → __save ContractLine（materialId+quantity+unitPrice+amount）→ __save InvoicePlan（planDate+amount+invoiceTerm）。
 *
 * 清理：triggerInvoice 创建的 AP/AR 发票 + 发票行（code=`CT-INV-{planId}` 反查，非 cascade）+ Contract
 *   （ContractLine + InvoicePlan cascade-delete）。
 *   发票为 DRAFT 草稿（posted=false, approveStatus=UNSUBMITTED），无凭证/辅助账产物，删除安全。
 */

const PARTNER_CUSTOMER_ID = 1; // CUST-001（OUTBOUND→AR 发票 customerId）
const PARTNER_SUPPLIER_ID = 3; // SUP-001（INBOUND→AP 发票 supplierId）
const ORG_ID = 2;
const CURRENCY_ID = 1;
const MATERIAL_ID = 4; // MAT-004
const AS_OF_DATE = '2026-07-14';
const PLAN_AMOUNT = 1000;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

interface ContractOpts {
  contractType: string;
  contractDirection: string;
  partnerId: number;
  tag: string;
}

async function seedActiveContract(page: import('@playwright/test').Page, o: ContractOpts): Promise<{ id: string; status: string }> {
  const c = await createViaSave(
    page, 'ErpCtContract',
    {
      code: uniq(`E2E-CT-IP-${o.tag}`),
      contractName: `E2E InvoicePlan Contract ${o.tag}`,
      contractType: o.contractType,
      contractDirection: o.contractDirection,
      partnerId: o.partnerId,
      orgId: ORG_ID,
      currencyId: CURRENCY_ID,
      totalAmount: 10000,
      startDate: '2026-01-01',
      endDate: '2026-12-31',
      businessDate: '2026-07-14',
      status: 'NEGOTIATION',
    },
    'id status',
  );
  await callMutationOk(page, 'ErpCtContract', 'activate', { contractId: c.id }, 'id');
  return c;
}

async function seedContractLine(page: import('@playwright/test').Page, contractId: string | number): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpCtContractLine',
    {
      contractId: Number(contractId),
      lineNo: 1,
      materialId: MATERIAL_ID,
      quantity: 100,
      unitPrice: 10,
      amount: 1000,
    },
    'id',
  );
}

async function seedInvoicePlan(page: import('@playwright/test').Page, contractLineId: string | number, amount: number): Promise<{ id: string; isInvoiced: boolean }> {
  return createViaSave(
    page, 'ErpCtInvoicePlan',
    {
      contractLineId: Number(contractLineId),
      planDate: '2026-06-01',
      amount,
      invoiceTerm: 'MILESTONE',
      isInvoiced: false,
    },
    'id isInvoiced',
  );
}

async function deleteApInvoiceDraft(page: import('@playwright/test').Page, billCode: string): Promise<void> {
  const inv = await findFirst<any>(page, 'ErpPurInvoice', eqFilter('code', billCode), 'id');
  if (inv) {
    await deleteByFilter(page, 'ErpPurInvoiceLine', eqFilter('invoiceId', Number(inv.id)));
    await deleteById(page, 'ErpPurInvoice', inv.id);
  }
}

async function deleteArInvoiceDraft(page: import('@playwright/test').Page, billCode: string): Promise<void> {
  const inv = await findFirst<any>(page, 'ErpSalInvoice', eqFilter('code', billCode), 'id');
  if (inv) {
    await deleteByFilter(page, 'ErpSalInvoiceLine', eqFilter('invoiceId', Number(inv.id)));
    await deleteById(page, 'ErpSalInvoice', inv.id);
  }
}

test.describe('contract ErpCtInvoicePlan triggerInvoice / triggerDuePlans orchestration', () => {
  test('INBOUND contract triggerInvoice → AP invoice draft (ErpPurInvoice) + plan writeback', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCtInvoicePlan-main');

    const contract = await seedActiveContract(page, {
      contractType: 'PURCHASE', contractDirection: 'INBOUND',
      partnerId: PARTNER_SUPPLIER_ID, tag: 'IB',
    });
    const line = await seedContractLine(page, contract.id);
    const plan = await seedInvoicePlan(page, line.id, PLAN_AMOUNT);

    try {
      // triggerInvoice：INBOUND 合同 → 建 AP 发票草稿
      await callMutationOk(page, 'ErpCtInvoicePlan', 'triggerInvoice', { planId: plan.id }, 'id');

      // plan 回写断言
      const ps = await verifyState(page, 'ErpCtInvoicePlan', plan.id, 'isInvoiced invoiceBillCode invoiceDate');
      expect(ps.isInvoiced, 'plan.isInvoiced=true after trigger').toBe(true);
      expect(ps.invoiceBillCode, 'plan.invoiceBillCode=CT-INV-{planId}').toBe(`CT-INV-${plan.id}`);
      expect(ps.invoiceDate, 'plan.invoiceDate non-null (today)').not.toBeNull();

      // AP 发票草稿创建断言（ErpPurInvoice code=CT-INV-{planId}）
      const expectedBillCode = `CT-INV-${plan.id}`;
      const ap = await findFirst<any>(page, 'ErpPurInvoice', eqFilter('code', expectedBillCode),
        'id code supplierId totalAmount docStatus approveStatus posted');
      expect(ap, 'AP invoice draft (ErpPurInvoice) should be created').not.toBeNull();
      expect(ap!.supplierId, 'AP invoice supplierId=contract.partnerId').toBe(PARTNER_SUPPLIER_ID);
      expect(Number(ap!.totalAmount), 'AP invoice totalAmount=plan.amount').toBe(PLAN_AMOUNT);
      expect(ap!.posted, 'AP invoice draft posted=false').toBe(false);
    } finally {
      // 清理：AP 发票草稿（非 cascade）+ Contract（cascade ContractLine + InvoicePlan）
      await deleteApInvoiceDraft(page, `CT-INV-${plan.id}`);
      await deleteById(page, 'ErpCtContract', contract.id);
    }
  });

  test('OUTBOUND contract triggerInvoice → AR invoice draft (ErpSalInvoice) + plan writeback', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCtInvoicePlan-main');

    const contract = await seedActiveContract(page, {
      contractType: 'SALES', contractDirection: 'OUTBOUND',
      partnerId: PARTNER_CUSTOMER_ID, tag: 'OB',
    });
    const line = await seedContractLine(page, contract.id);
    const plan = await seedInvoicePlan(page, line.id, PLAN_AMOUNT);

    try {
      await callMutationOk(page, 'ErpCtInvoicePlan', 'triggerInvoice', { planId: plan.id }, 'id');

      const ps = await verifyState(page, 'ErpCtInvoicePlan', plan.id, 'isInvoiced invoiceBillCode invoiceDate');
      expect(ps.isInvoiced, 'plan.isInvoiced=true after trigger').toBe(true);
      expect(ps.invoiceBillCode, 'plan.invoiceBillCode=CT-INV-{planId}').toBe(`CT-INV-${plan.id}`);

      // AR 发票草稿创建断言（ErpSalInvoice code=CT-INV-{planId}）
      const expectedBillCode = `CT-INV-${plan.id}`;
      const ar = await findFirst<any>(page, 'ErpSalInvoice', eqFilter('code', expectedBillCode),
        'id code customerId totalAmount docStatus approveStatus posted');
      expect(ar, 'AR invoice draft (ErpSalInvoice) should be created').not.toBeNull();
      expect(ar!.customerId, 'AR invoice customerId=contract.partnerId').toBe(PARTNER_CUSTOMER_ID);
      expect(Number(ar!.totalAmount), 'AR invoice totalAmount=plan.amount').toBe(PLAN_AMOUNT);
      expect(ar!.posted, 'AR invoice draft posted=false').toBe(false);
    } finally {
      await deleteArInvoiceDraft(page, `CT-INV-${plan.id}`);
      await deleteById(page, 'ErpCtContract', contract.id);
    }
  });

  test('illegal guards: already-invoiced plan rejected (ERR_CT_INVOICE_PLAN_ALREADY_INVOICED); SUSPENDED contract rejected (ERR_CT_CONTRACT_SUSPENDED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCtInvoicePlan-main');

    // (a) 已开票 plan 重复触发 → ERR_CT_INVOICE_PLAN_ALREADY_INVOICED
    const contract1 = await seedActiveContract(page, {
      contractType: 'PURCHASE', contractDirection: 'INBOUND',
      partnerId: PARTNER_SUPPLIER_ID, tag: 'G1',
    });
    const line1 = await seedContractLine(page, contract1.id);
    const plan1 = await seedInvoicePlan(page, line1.id, PLAN_AMOUNT);

    try {
      // 首次触发成功
      await callMutationOk(page, 'ErpCtInvoicePlan', 'triggerInvoice', { planId: plan1.id }, 'id');
      // 二次触发应拒绝
      const rej1 = await callMutation(page, 'ErpCtInvoicePlan', 'triggerInvoice', { planId: plan1.id }, 'id');
      expect(rej1.errors, 're-trigger on invoiced plan should be rejected').toBeTruthy();
      expect(JSON.stringify(rej1.errors), 'reject should carry already-invoiced token').toContain('已生成发票');

      // (b) SUSPENDED 合同的 plan 触发 → ERR_CT_CONTRACT_SUSPENDED
      const contract2 = await seedActiveContract(page, {
        contractType: 'PURCHASE', contractDirection: 'INBOUND',
        partnerId: PARTNER_SUPPLIER_ID, tag: 'G2',
      });
      const line2 = await seedContractLine(page, contract2.id);
      const plan2 = await seedInvoicePlan(page, line2.id, PLAN_AMOUNT);
      // ACTIVE → SUSPENDED
      await callMutationOk(page, 'ErpCtContract', 'suspend', { contractId: contract2.id }, 'id');

      try {
        const rej2 = await callMutation(page, 'ErpCtInvoicePlan', 'triggerInvoice', { planId: plan2.id }, 'id');
        expect(rej2.errors, 'trigger on SUSPENDED contract plan should be rejected').toBeTruthy();
        expect(JSON.stringify(rej2.errors), 'reject should carry suspended token').toContain('已中止');

        // plan 未回写（事务回滚）
        const ps2 = await verifyState(page, 'ErpCtInvoicePlan', plan2.id, 'isInvoiced');
        expect(ps2.isInvoiced, 'plan2.isInvoiced remains false (rejected)').toBe(false);
      } finally {
        await deleteById(page, 'ErpCtContract', contract2.id);
      }
    } finally {
      await deleteApInvoiceDraft(page, `CT-INV-${plan1.id}`);
      await deleteById(page, 'ErpCtContract', contract1.id);
    }
  });

  test('triggerDuePlans batch entry: 2 due plans → returns 2 + both isInvoiced=true', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCtInvoicePlan-main');

    const contract = await seedActiveContract(page, {
      contractType: 'PURCHASE', contractDirection: 'INBOUND',
      partnerId: PARTNER_SUPPLIER_ID, tag: 'TD',
    });
    const line = await seedContractLine(page, contract.id);
    // 建 2 个到期未开票 plan（planDate <= AS_OF_DATE）
    const plan1 = await seedInvoicePlan(page, line.id, 500);
    const plan2 = await seedInvoicePlan(page, line.id, 300);

    try {
      // triggerDuePlans 返回触发行数（int 标量，原始 mutation 无选择集）
      const json: any = await new GraphQLClient(page).raw(
        `mutation{ ErpCtInvoicePlan__triggerDuePlans(contractId:${contract.id},asOfDate:"${AS_OF_DATE}") }`,
      );
      expect(json.errors, 'triggerDuePlans should not return GraphQL errors').toBeFalsy();
      const triggered = Number(json?.data?.ErpCtInvoicePlan__triggerDuePlans);
      expect(triggered, 'triggerDuePlans should return >=2 triggered count').toBeGreaterThanOrEqual(2);

      // 两 plan 均已开票
      const ps1 = await verifyState(page, 'ErpCtInvoicePlan', plan1.id, 'isInvoiced invoiceBillCode');
      expect(ps1.isInvoiced, 'plan1.isInvoiced=true after triggerDuePlans').toBe(true);
      expect(ps1.invoiceBillCode, 'plan1.invoiceBillCode=CT-INV-{plan1Id}').toBe(`CT-INV-${plan1.id}`);

      const ps2 = await verifyState(page, 'ErpCtInvoicePlan', plan2.id, 'isInvoiced invoiceBillCode');
      expect(ps2.isInvoiced, 'plan2.isInvoiced=true after triggerDuePlans').toBe(true);
      expect(ps2.invoiceBillCode, 'plan2.invoiceBillCode=CT-INV-{plan2Id}').toBe(`CT-INV-${plan2.id}`);
    } finally {
      // 清理 2 张 AP 发票草稿 + Contract（cascade）
      await deleteApInvoiceDraft(page, `CT-INV-${plan1.id}`);
      await deleteApInvoiceDraft(page, `CT-INV-${plan2.id}`);
      await deleteById(page, 'ErpCtContract', contract.id);
    }
  });
});
