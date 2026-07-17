import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  deleteById,
} from './_helper';
import type { Page } from '@playwright/test';

/**
 * finance ErpFinCreditFacility 银行授信额度占用契约 DIRECT 浏览器层 E2E
 * （plan 2026-07-17-2256-1 Phase 3）。
 *
 * 验证 `ErpFinCreditFacilityBizModel.reserveCredit`/`releaseCredit` 经 GraphQL
 * `/graphql` 的全栈可达性 + 强一致占用契约（treasury.md §关键业务规则 1）。
 *
 * 权威占用逻辑（ErpFinCreditFacilityBizModel:32-66）：
 *   - `reserveCredit(creditFacilityId, amount)`：校验 availableAmount >= amount
 *     （不足抛 `ERR_CREDIT_FACILITY_INSUFFICIENT`），usedAmount += amount，
 *     availableAmount = totalAmount − usedAmount 同步重算。
 *   - `releaseCredit(creditFacilityId, amount)`：usedAmount -= amount（下限 0），
 *     availableAmount = totalAmount − usedAmount 同步重算。
 *
 * 本计划不断言 `version` 乐观锁自增/stale-version 拒绝（并发面归后端单测），
 * 仅断言单线程顺序下的占用契约字段（used/available 同步重算 + 不足守卫事务回滚）。
 *
 * 自包含 setup：每个测试自建独立 facility（隔离互不影响）。`ErpFinCreditFacility`
 * ORM `tagSet="gid,erp.finance"` 无 use-approval/use-workflow，DIRECT `__save` 可达。
 * Processor 无 auto-compute available，须 __save 显式三值（total/used/available）。
 *
 * 种子引用：org=2 / facilityType=REVOLVING（erp-fin/credit-facility-type 字典）。
 * cleanup 逐测试删 facility，保护共享 DB 数值断言基线。
 */
const ORG_ID = 2;
const FACILITY_TYPE = 'BANK_ACCEPTANCE_LINE'; // erp-fin/credit-facility-type 字典仅 BANK_ACCEPTANCE_LINE / LOAN_LINE
const TOTAL_AMOUNT = 1000;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now().toString(36)}-${_seq}`;
}

async function createFacility(
  page: Page,
  overrides: Record<string, unknown> = {},
): Promise<{ id: string; code: string }> {
  const code = uniq('E2E-CF-FAC');
  return createViaSave(
    page,
    'ErpFinCreditFacility',
    {
      code,
      orgId: ORG_ID,
      facilityType: FACILITY_TYPE,
      totalAmount: TOTAL_AMOUNT,
      usedAmount: 0,
      availableAmount: TOTAL_AMOUNT,
      validFrom: '2026-01-01',
      validTo: '2026-12-31',
      status: 'ACTIVE',
      ...overrides,
    },
    'id code',
  );
}

test.describe('finance ErpFinCreditFacility reserve/release occupation contract', () => {
  test('reserveCredit: used += amount, available = total − used (synced recompute)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinCreditFacility-main');
    const facility = await createFacility(page);
    try {
      const r = await callMutationOk(
        page, 'ErpFinCreditFacility', 'reserveCredit',
        { creditFacilityId: Number(facility.id), amount: 300 },
        'id usedAmount availableAmount',
      );
      expect(Number(r.usedAmount), 'reserve(300) → usedAmount=300').toBe(300);
      expect(Number(r.availableAmount), 'reserve(300) → availableAmount=700').toBe(700);

      const v = await verifyState(page, 'ErpFinCreditFacility', facility.id, 'usedAmount availableAmount totalAmount');
      expect(Number(v.usedAmount), '__get confirms usedAmount=300').toBe(300);
      expect(Number(v.availableAmount), '__get confirms availableAmount=700').toBe(700);
      expect(Number(v.totalAmount), 'totalAmount unchanged=1000').toBe(1000);
    } finally {
      await deleteById(page, 'ErpFinCreditFacility', facility.id);
    }
  });

  test('insufficient guard: reserveCredit(>available) → ERR_CREDIT_FACILITY_INSUFFICIENT + transaction rollback (no change)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinCreditFacility-main');
    const facility = await createFacility(page);
    try {
      // 先 reserve(300) 使 available=700（构建可观测前置状态）
      await callMutationOk(
        page, 'ErpFinCreditFacility', 'reserveCredit',
        { creditFacilityId: Number(facility.id), amount: 300 },
        'id usedAmount availableAmount',
      );
      const before = await verifyState(page, 'ErpFinCreditFacility', facility.id, 'usedAmount availableAmount');
      expect(Number(before.usedAmount), 'precondition: usedAmount=300').toBe(300);
      expect(Number(before.availableAmount), 'precondition: availableAmount=700').toBe(700);

      // reserve(2000) > available(700) → 守卫拒绝
      const rej = await callMutation(
        page, 'ErpFinCreditFacility', 'reserveCredit',
        { creditFacilityId: Number(facility.id), amount: 2000 },
        'id',
      );
      expect(rej.errors, 'reserve(2000>700) should return GraphQL errors').toBeTruthy();
      // GraphQL 错误响应携带 NopException 中文描述（非 ErrorCode 字符串），「不足」为
      // ERR_CREDIT_FACILITY_INSUFFICIENT 的语义 token（对齐其他 spec 守卫断言范式：
      // ct-contract-version 含「不允许执行该操作」、drp-plan-engine 含「不允许此操作」）
      const errMsg = JSON.stringify(rej.errors);
      expect(errMsg, 'error should carry insufficient-credit semantic token「不足」').toContain('不足');

      // 事务回滚：used/available 不变
      const after = await verifyState(page, 'ErpFinCreditFacility', facility.id, 'usedAmount availableAmount');
      expect(Number(after.usedAmount), 'rollback: usedAmount unchanged=300').toBe(300);
      expect(Number(after.availableAmount), 'rollback: availableAmount unchanged=700').toBe(700);
    } finally {
      await deleteById(page, 'ErpFinCreditFacility', facility.id);
    }
  });

  test('releaseCredit: used -= amount (floor 0), available = total − used restored', async ({ page }) => {
    await loginAndNavigate(page, '/ErpFinCreditFacility-main');
    const facility = await createFacility(page);
    try {
      // 先 reserve(300) 构建占用状态
      await callMutationOk(
        page, 'ErpFinCreditFacility', 'reserveCredit',
        { creditFacilityId: Number(facility.id), amount: 300 },
        'id usedAmount availableAmount',
      );
      const reserved = await verifyState(page, 'ErpFinCreditFacility', facility.id, 'usedAmount availableAmount');
      expect(Number(reserved.usedAmount), 'precondition: usedAmount=300').toBe(300);
      expect(Number(reserved.availableAmount), 'precondition: availableAmount=700').toBe(700);

      // release(300) → used=0/available=1000 恢复
      const r = await callMutationOk(
        page, 'ErpFinCreditFacility', 'releaseCredit',
        { creditFacilityId: Number(facility.id), amount: 300 },
        'id usedAmount availableAmount',
      );
      expect(Number(r.usedAmount), 'release(300) → usedAmount=0').toBe(0);
      expect(Number(r.availableAmount), 'release(300) → availableAmount=1000 restored').toBe(1000);

      const v = await verifyState(page, 'ErpFinCreditFacility', facility.id, 'usedAmount availableAmount totalAmount');
      expect(Number(v.usedAmount), '__get confirms usedAmount=0').toBe(0);
      expect(Number(v.availableAmount), '__get confirms availableAmount=1000').toBe(1000);
      expect(Number(v.totalAmount), 'totalAmount unchanged=1000').toBe(1000);
    } finally {
      await deleteById(page, 'ErpFinCreditFacility', facility.id);
    }
  });
});
