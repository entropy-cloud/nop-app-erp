import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  eqFilter,
  findFirst,
  deleteById,
} from './_helper';

/**
 * hr ErpHrRecruitment 招聘漏斗浏览器层 E2E（plan 2026-07-14-0215-3 Phase 2）。
 *
 * 验证招聘 6 动作状态机经 GraphQL /graphql 的全栈可达性 + HIRED→员工/合同联动：
 *   OPEN→moveToScreening→SCREENING→scheduleInterview→INTERVIEW→makeOffer→OFFERED→hire→HIRED
 *   hire 联动：自动创建 ErpHrEmployee（employeeId 回写）+ ErpHrEmploymentContract（ACTIVE）。
 *   reject（OPEN→REJECTED）/ close（→CLOSED）异常路径。
 *   非法迁移守卫：HIRED→moveToScreening 抛 ERR_RECRUITMENT_ILLEGAL_STATUS_TRANSITION。
 *
 * 权威设计（use-cases.md UC-HR-05）：扁平 ErpHrRecruitment 上状态机，hire 经 IErpHrEmployeeBiz
 * + IErpHrEmploymentContractBiz 跨实体创建。ORM 无 useWorkflow / 无 useApproval，纯 DIRECT @BizMutation。
 *
 * 种子引用：interviewerId=1（seed employee HR-EMP-001）。自包含 setup：__save ErpHrRecruitment(OPEN 入口)。
 * 清理：hire 产 employee + contract 须连带删除（按 rec.employeeId 反查 contract）。
 */

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

async function seedRecruitment(page: import('@playwright/test').Page, tag: string): Promise<{ id: string; status: string }> {
  return createViaSave(
    page,
    'ErpHrRecruitment',
    {
      code: uniq(`E2E-RC-${tag}`),
      candidateName: `候选人${tag}`,
      candidateEmail: `cand-${tag}@e2e.test`,
      status: 'OPEN',
      orgId: 2,
    },
    'id status',
  );
}

async function cleanupHireLinkage(page: import('@playwright/test').Page, employeeId: string | number): Promise<void> {
  const contract = await findFirst(page, 'ErpHrEmploymentContract', eqFilter('employeeId', Number(employeeId)), 'id');
  if (contract) {
    await deleteById(page, 'ErpHrEmploymentContract', (contract as any).id);
  }
  await deleteById(page, 'ErpHrEmployee', employeeId);
}

test.describe('hr ErpHrRecruitment funnel state machine + hire linkage', () => {
  test('happy path: OPEN→SCREENING→INTERVIEW→OFFERED→HIRED + employee/contract auto-created', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrRecruitment-main');

    const rec = await seedRecruitment(page, 'hp');
    expect(rec.status, 'new recruitment status=OPEN').toBe('OPEN');

    // moveToScreening: OPEN → SCREENING
    await callMutationOk(page, 'ErpHrRecruitment', 'moveToScreening', { id: rec.id }, 'id');
    let st = await verifyState(page, 'ErpHrRecruitment', rec.id, 'status');
    expect(st.status, 'after moveToScreening status=SCREENING').toBe('SCREENING');

    // scheduleInterview: SCREENING → INTERVIEW
    await callMutationOk(page, 'ErpHrRecruitment', 'scheduleInterview', { id: rec.id, interviewerId: 1, interviewDate: '2026-08-01' }, 'id');
    st = await verifyState(page, 'ErpHrRecruitment', rec.id, 'status');
    expect(st.status, 'after scheduleInterview status=INTERVIEW').toBe('INTERVIEW');

    // makeOffer: INTERVIEW → OFFERED
    await callMutationOk(page, 'ErpHrRecruitment', 'makeOffer', { id: rec.id, offerSalary: 15000 }, 'id');
    st = await verifyState(page, 'ErpHrRecruitment', rec.id, 'status offerSalary');
    expect(st.status, 'after makeOffer status=OFFERED').toBe('OFFERED');
    expect(Number(st.offerSalary), 'offerSalary recorded').toBe(15000);

    // hire: OFFERED → HIRED + employee/contract auto-created
    await callMutationOk(page, 'ErpHrRecruitment', 'hire', { id: rec.id, hiredDate: '2026-07-14' }, 'id');
    st = await verifyState(page, 'ErpHrRecruitment', rec.id, 'status employeeId');
    expect(st.status, 'after hire status=HIRED').toBe('HIRED');
    expect(st.employeeId, 'hire writes back employeeId').not.toBeNull();

    // 员工联动：ErpHrEmployee 存在
    const emp = await verifyState(page, 'ErpHrEmployee', st.employeeId, 'id fullName employmentStatus');
    expect(emp, 'auto-created employee exists').toBeTruthy();
    expect(emp.employmentStatus, 'new employee ACTIVE').toBe('ACTIVE');

    // 合同联动：ErpHrEmploymentContract 存在（ACTIVE）
    const contract = await findFirst(page, 'ErpHrEmploymentContract', eqFilter('employeeId', Number(st.employeeId)), 'id status monthlySalary');
    expect(contract, 'auto-created contract exists').not.toBeNull();
    expect((contract as any).status, 'new contract ACTIVE').toBe('ACTIVE');

    // 清理：删合同 + 员工 + 招聘记录
    await cleanupHireLinkage(page, st.employeeId);
    await deleteById(page, 'ErpHrRecruitment', rec.id);
  });

  test('reject path: OPEN → reject (REJECTED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrRecruitment-main');

    const rec = await seedRecruitment(page, 'rj');
    await callMutationOk(page, 'ErpHrRecruitment', 'reject', { id: rec.id }, 'id');
    const st = await verifyState(page, 'ErpHrRecruitment', rec.id, 'status');
    expect(st.status, 'after reject status=REJECTED').toBe('REJECTED');

    await deleteById(page, 'ErpHrRecruitment', rec.id);
  });

  test('close path: OPEN → close (CLOSED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrRecruitment-main');

    const rec = await seedRecruitment(page, 'cl');
    await callMutationOk(page, 'ErpHrRecruitment', 'close', { id: rec.id }, 'id');
    const st = await verifyState(page, 'ErpHrRecruitment', rec.id, 'status');
    expect(st.status, 'after close status=CLOSED').toBe('CLOSED');

    await deleteById(page, 'ErpHrRecruitment', rec.id);
  });

  test('illegal transition guard: HIRED→moveToScreening rejected (ERR_RECRUITMENT_ILLEGAL_STATUS_TRANSITION)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrRecruitment-main');

    const rec = await seedRecruitment(page, 'gd');
    // 推进到 HIRED
    await callMutationOk(page, 'ErpHrRecruitment', 'moveToScreening', { id: rec.id }, 'id');
    await callMutationOk(page, 'ErpHrRecruitment', 'scheduleInterview', { id: rec.id, interviewerId: 1, interviewDate: '2026-08-01' }, 'id');
    await callMutationOk(page, 'ErpHrRecruitment', 'makeOffer', { id: rec.id, offerSalary: 12000 }, 'id');
    const hireResult = await callMutationOk(page, 'ErpHrRecruitment', 'hire', { id: rec.id, hiredDate: '2026-07-14' }, 'id employeeId');

    // HIRED → moveToScreening（须 OPEN）：抛 ERR_RECRUITMENT_ILLEGAL_STATUS_TRANSITION
    const rej = await callMutation(page, 'ErpHrRecruitment', 'moveToScreening', { id: rec.id }, 'id');
    expect(rej.errors, 'moveToScreening from HIRED should be rejected').toBeTruthy();
    expect(JSON.stringify(rej.errors), 'reject should carry illegal-transition token').toContain('不允许执行该操作');

    // 状态不变
    const st = await verifyState(page, 'ErpHrRecruitment', rec.id, 'status');
    expect(st.status, 'status unchanged after guard reject').toBe('HIRED');

    await cleanupHireLinkage(page, hireResult.employeeId);
    await deleteById(page, 'ErpHrRecruitment', rec.id);
  });
});
