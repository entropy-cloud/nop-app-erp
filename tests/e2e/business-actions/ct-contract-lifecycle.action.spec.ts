import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, eqFilter, findPageTotal, deleteByFilter, deleteById } from './_helper';

/**
 * contract ErpCtContract 合同生命周期业务动作浏览器层 E2E（plan 2026-07-14-0215-2 Phase 1）。
 *
 * 验证合同 6 动作状态机经 GraphQL /graphql 的全栈可达性 + status 翻转。
 *
 * 权威状态机（ErpCtContractBizModel，对齐 docs/design/contract/state-machine.md）：
 *   DRAFT→NEGOTIATION（无 @BizMutation，经 __save 直接置入口）→
 *   activate(须 NEGOTIATION)→ACTIVE↔suspend/resume→
 *   terminate/expire（终态，须 ACTIVE）/ amend（须 ACTIVE，回 DRAFT + 新建修订版本）。
 *   activate 附加 validateTypeDirectionCombo：SALES→OUTBOUND / PURCHASE→INBOUND。
 *   非法迁移抛 ERR_CT_ILLEGAL_STATUS_TRANSITION（message token「不允许执行该操作」）。
 *
 * ORM 无 useWorkflow / 无 useApproval，纯 DIRECT @BizMutation 浏览器层可达。
 *
 * 种子引用（init-data）：partner id=1（CUST-001 客户）/ org id=2 / currency id=1。
 * 自包含 setup：经 __save 直接置 status=NEGOTIATION 入口（DRAFT→NEGOTIATION 无 @BizMutation）。
 * 清理：合同头 versions 关系 cascade-delete；amend 产版本经 deleteByFilter 显式清理后删合同头。
 */

const PARTNER_CUSTOMER_ID = 1;
const ORG_ID = 2;
const CURRENCY_ID = 1;

interface ContractOpts {
  status: string;
  tag: string;
}

async function seedContract(page: import('@playwright/test').Page, o: ContractOpts): Promise<{ id: string; status: string }> {
  return createViaSave(
    page, 'ErpCtContract',
    {
      code: `E2E-CT-${o.tag}-${Date.now()}`,
      contractName: `E2E Contract ${o.tag}`,
      contractType: 'SALES',
      contractDirection: 'OUTBOUND',
      partnerId: PARTNER_CUSTOMER_ID,
      orgId: ORG_ID,
      currencyId: CURRENCY_ID,
      totalAmount: 10000,
      startDate: '2026-01-01',
      endDate: '2026-12-31',
      businessDate: '2026-07-14',
      status: o.status,
    },
    'id status',
  );
}

test.describe('contract ErpCtContract lifecycle state machine', () => {
  test('happy path: save(NEGOTIATION) → activate(ACTIVE) → suspend(SUSPENDED) → resume(ACTIVE) → terminate(TERMINATED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCtContract-main');

    const c = await seedContract(page, { status: 'NEGOTIATION', tag: 'hp' });
    expect(c.status, 'new contract status=NEGOTIATION').toBe('NEGOTIATION');

    // activate: NEGOTIATION → ACTIVE（SALES+OUTBOUND 经 validateTypeDirectionCombo 放行）
    await callMutationOk(page, 'ErpCtContract', 'activate', { contractId: c.id }, 'id');
    let s = await verifyState(page, 'ErpCtContract', c.id, 'status');
    expect(s.status, 'after activate status=ACTIVE').toBe('ACTIVE');

    // suspend: ACTIVE → SUSPENDED
    await callMutationOk(page, 'ErpCtContract', 'suspend', { contractId: c.id }, 'id');
    s = await verifyState(page, 'ErpCtContract', c.id, 'status');
    expect(s.status, 'after suspend status=SUSPENDED').toBe('SUSPENDED');

    // resume: SUSPENDED → ACTIVE
    await callMutationOk(page, 'ErpCtContract', 'resume', { contractId: c.id }, 'id');
    s = await verifyState(page, 'ErpCtContract', c.id, 'status');
    expect(s.status, 'after resume status=ACTIVE').toBe('ACTIVE');

    // terminate: ACTIVE → TERMINATED
    await callMutationOk(page, 'ErpCtContract', 'terminate', { contractId: c.id }, 'id');
    s = await verifyState(page, 'ErpCtContract', c.id, 'status');
    expect(s.status, 'after terminate status=TERMINATED').toBe('TERMINATED');

    // 清理
    await deleteById(page, 'ErpCtContract', c.id);
  });

  test('expire path: activate(ACTIVE) → expire(EXPIRED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCtContract-main');

    const c = await seedContract(page, { status: 'NEGOTIATION', tag: 'exp' });
    await callMutationOk(page, 'ErpCtContract', 'activate', { contractId: c.id }, 'id');
    let s = await verifyState(page, 'ErpCtContract', c.id, 'status');
    expect(s.status, 'after activate status=ACTIVE').toBe('ACTIVE');

    // expire: ACTIVE → EXPIRED
    await callMutationOk(page, 'ErpCtContract', 'expire', { contractId: c.id }, 'id');
    s = await verifyState(page, 'ErpCtContract', c.id, 'status');
    expect(s.status, 'after expire status=EXPIRED').toBe('EXPIRED');

    await deleteById(page, 'ErpCtContract', c.id);
  });

  test('amend path: ACTIVE contract amend → contract DRAFT + new revision version created', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCtContract-main');

    const c = await seedContract(page, { status: 'NEGOTIATION', tag: 'amd' });
    await callMutationOk(page, 'ErpCtContract', 'activate', { contractId: c.id }, 'id');

    // amend: ACTIVE → DRAFT + 新建修订版本（versionNo=max+1=1，isCurrent=true，status=DRAFT）
    await callMutationOk(page, 'ErpCtContract', 'amend', { contractId: c.id }, 'id');
    const s = await verifyState(page, 'ErpCtContract', c.id, 'status');
    expect(s.status, 'after amend contract status=DRAFT (back to amendment)').toBe('DRAFT');

    // 修订版本回链：ErpCtContractVersion 非空（amend 新建修订版）
    const versionCount = await findPageTotal(page, 'ErpCtContractVersion', eqFilter('contractId', Number(c.id)));
    expect(versionCount, 'amend should create a revision version').toBeGreaterThan(0);

    // 清理：先删 amend 产版本，再删合同头（versions 关系 cascade-delete 兜底）
    await deleteByFilter(page, 'ErpCtContractVersion', eqFilter('contractId', Number(c.id)));
    await deleteById(page, 'ErpCtContract', c.id);
  });

  test('illegal transition guards: DRAFT→activate rejected; TERMINATED→activate rejected (ERR_CT_ILLEGAL_STATUS_TRANSITION)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCtContract-main');

    // DRAFT → activate（activate 须 NEGOTIATION）：经 GraphQL 返回 errors
    const draft = await seedContract(page, { status: 'DRAFT', tag: 'gd' });
    const rej1 = await callMutation(page, 'ErpCtContract', 'activate', { contractId: draft.id }, 'id');
    expect(rej1.errors, 'activate from DRAFT should be rejected (requires NEGOTIATION)').toBeTruthy();
    expect(JSON.stringify(rej1.errors), 'reject should carry illegal-transition token').toContain('不允许执行该操作');

    // TERMINATED → activate：终态不可再迁移
    const term = await seedContract(page, { status: 'NEGOTIATION', tag: 'gt' });
    await callMutationOk(page, 'ErpCtContract', 'activate', { contractId: term.id }, 'id');
    await callMutationOk(page, 'ErpCtContract', 'terminate', { contractId: term.id }, 'id');
    const rej2 = await callMutation(page, 'ErpCtContract', 'activate', { contractId: term.id }, 'id');
    expect(rej2.errors, 'activate from TERMINATED should be rejected').toBeTruthy();
    expect(JSON.stringify(rej2.errors), 'reject should carry illegal-transition token').toContain('不允许执行该操作');

    await deleteById(page, 'ErpCtContract', draft.id);
    await deleteById(page, 'ErpCtContract', term.id);
  });
});
