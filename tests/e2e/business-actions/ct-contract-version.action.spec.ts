import { test, expect, loginAndNavigate, createViaSave, callMutationOk, callMutation, verifyState, deleteById } from './_helper';

/**
 * contract ErpCtContractVersion 版本管理业务动作浏览器层 E2E（plan 2026-07-14-0215-2 Phase 1）。
 *
 * 验证版本 finalizeVersion/signVersion 状态机经 GraphQL /graphql 的全栈可达性 + status/isCurrent 翻转。
 *
 * 权威状态机（ErpCtContractVersionBizModel，对齐 docs/design/contract/state-machine.md §版本管理）：
 *   DRAFT --finalizeVersion(须 DRAFT)--> FINALIZED --signVersion(须 FINALIZED + isCurrent)--> SIGNED
 *   signVersion 原子翻转：目标版本置 SIGNED + isCurrent=true，同合同其他版本 isCurrent=false。
 *   非法迁移抛 ERR_CT_ILLEGAL_STATUS_TRANSITION（message token「不允许执行该操作」）。
 *
 * ORM 无 useWorkflow / 无 useApproval，纯 DIRECT @BizMutation 浏览器层可达。
 *
 * 种子引用（init-data）：partner id=1（CUST-001）/ org id=2 / currency id=1。
 * 自包含 setup：建合同头（ErpCtContract NEGOTIATION 入口）+ 建版本（DRAFT + isCurrent=true）。
 * 清理：合同头 versions 关系 cascade-delete；删合同头兜底删版本。
 */

const PARTNER_CUSTOMER_ID = 1;
const ORG_ID = 2;
const CURRENCY_ID = 1;

async function seedContract(page: import('@playwright/test').Page, tag: string): Promise<{ id: string }> {
  return createViaSave(
    page, 'ErpCtContract',
    {
      code: `E2E-CTV-${tag}-${Date.now()}`,
      contractName: `E2E Version Contract ${tag}`,
      contractType: 'SALES',
      contractDirection: 'OUTBOUND',
      partnerId: PARTNER_CUSTOMER_ID,
      orgId: ORG_ID,
      currencyId: CURRENCY_ID,
      totalAmount: 5000,
      startDate: '2026-01-01',
      endDate: '2026-12-31',
      businessDate: '2026-07-14',
      status: 'NEGOTIATION',
    },
    'id',
  );
}

async function seedVersion(page: import('@playwright/test').Page, contractId: string | number, tag: string): Promise<{ id: string; status: string; isCurrent: boolean }> {
  return createViaSave(
    page, 'ErpCtContractVersion',
    {
      contractId,
      versionNo: 1,
      versionDate: '2026-07-14',
      content: `E2E version content ${tag}`,
      isCurrent: true,
      status: 'DRAFT',
    },
    'id status isCurrent',
  );
}

test.describe('contract ErpCtContractVersion finalize/sign state machine', () => {
  test('version lifecycle: save(DRAFT) → finalizeVersion(FINALIZED) → signVersion(SIGNED + isCurrent)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCtContractVersion-main');

    const contract = await seedContract(page, 'hp');
    const v = await seedVersion(page, contract.id, 'hp');
    expect(v.status, 'new version status=DRAFT').toBe('DRAFT');
    expect(v.isCurrent, 'new version isCurrent=true').toBe(true);

    // finalizeVersion: DRAFT → FINALIZED
    await callMutationOk(page, 'ErpCtContractVersion', 'finalizeVersion', { versionId: v.id }, 'id');
    let s = await verifyState(page, 'ErpCtContractVersion', v.id, 'status isCurrent');
    expect(s.status, 'after finalizeVersion status=FINALIZED').toBe('FINALIZED');

    // signVersion: FINALIZED → SIGNED + isCurrent 翻转保持 true
    await callMutationOk(page, 'ErpCtContractVersion', 'signVersion', { versionId: v.id }, 'id');
    s = await verifyState(page, 'ErpCtContractVersion', v.id, 'status isCurrent');
    expect(s.status, 'after signVersion status=SIGNED').toBe('SIGNED');
    expect(s.isCurrent, 'after signVersion isCurrent=true (current version)').toBe(true);

    // 清理
    await deleteById(page, 'ErpCtContractVersion', v.id);
    await deleteById(page, 'ErpCtContract', contract.id);
  });

  test('illegal transition guard: SIGNED→finalizeVersion rejected (ERR_CT_ILLEGAL_STATUS_TRANSITION, requires DRAFT)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpCtContractVersion-main');

    const contract = await seedContract(page, 'gd');
    const v = await seedVersion(page, contract.id, 'gd');

    // 走完正向链到 SIGNED
    await callMutationOk(page, 'ErpCtContractVersion', 'finalizeVersion', { versionId: v.id }, 'id');
    await callMutationOk(page, 'ErpCtContractVersion', 'signVersion', { versionId: v.id }, 'id');
    const s = await verifyState(page, 'ErpCtContractVersion', v.id, 'status');
    expect(s.status, 'version now SIGNED').toBe('SIGNED');

    // SIGNED → finalizeVersion（须 DRAFT）：经 GraphQL 返回 errors
    const rej = await callMutation(page, 'ErpCtContractVersion', 'finalizeVersion', { versionId: v.id }, 'id');
    expect(rej.errors, 'finalizeVersion from SIGNED should be rejected (requires DRAFT)').toBeTruthy();
    expect(JSON.stringify(rej.errors), 'reject should carry illegal-transition token').toContain('不允许执行该操作');

    // 清理
    await deleteById(page, 'ErpCtContractVersion', v.id);
    await deleteById(page, 'ErpCtContract', contract.id);
  });
});
