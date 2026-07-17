import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  input,
  eqFilter,
  andFilter,
  findFirst,
  findPageTotal,
  deleteByFilter,
  deleteById,
} from './_helper';

/**
 * hr 胜任力评估→差距→发展计划闭环浏览器层 E2E（plan 2026-07-18-0100-2 Phase 1）。
 *
 * 验证人才发展 4 实体（ErpHrEmployeeAssessment/ErpHrAssessmentDetail/ErpHrGapAnalysis/ErpHrDevelopmentPlan(+Item)）
 * 7 DIRECT `@BizMutation` 经 GraphQL /graphql 的全栈可达性 + completeAssessment 自动触发链 + gapSeverity
 * 计算规则 + 发展计划状态机：
 *   - submitAssessment: DRAFT→SUBMITTED（守卫 ≥1 detail）
 *   - completeAssessment: SUBMITTED→COMPLETED + overallScore 写回 + 自动调 refreshGapAnalysisWithLevels
 *     直传聚合 levels（核心副作用 competency-management.md §实现注记 Decision）
 *   - refreshGapAnalysis: 内部聚合 latest COMPLETED → 清旧重建 gap 快照
 *   - refreshGapAnalysisWithLevels: 直传 levels → 清旧重建 gap 快照（守卫 ERR_GAP_NO_ROLE_REQUIREMENT）
 *   - generateDevelopmentPlan: 仅 CRITICAL/MODERATE 差距 → IN_PROGRESS + 每差距一 item；无 actionable 返回 null
 *   - updatePlanItemStatus: NOT_STARTED→IN_PROGRESS→ACHIEVED（守卫非法跳级）
 *   - completePlan: IN_PROGRESS→COMPLETED（守卫非法跳级 ERR_DEV_PLAN_ILLEGAL_STATUS_TRANSITION）
 *
 * 权威设计（docs/design/human-resource/competency-management.md）：gapSeverity 规则 ≤0 NONE / 1 MINOR /
 * 2 MODERATE / ≥3 CRITICAL；completeAssessment 内部直传聚合 levels 避免二次查询跨事务可见性问题。
 *
 * 自包含 setup：建测试专用 competency×3 + position + roleCompetency×3（同 requiredLevel=5）+ employee
 * （positionId 非空）+ assessment(SELF) + detail×3（不同 actualLevel 触发不同 gapSeverity）避免污染种子。
 * 清理：按 employeeId/positionId/code 前缀逐域删 gap/planItem/plan/detail/assessment/roleCompetency/employee/position/dept/competency。
 */

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

interface AssessmentSetup {
  employeeId: string;
  positionId: string;
  departmentId: string;
  competencyIds: string[];
  assessmentId: string;
}

async function setupAssessmentChain(
  page: import('@playwright/test').Page,
  tag: string,
): Promise<AssessmentSetup> {
  const c1 = await createViaSave(
    page,
    'ErpHrCompetency',
    { code: uniq(`E2E-C1-${tag}`), name: `comp1-${tag}`, category: 'SKILL' },
    'id',
  );
  const c2 = await createViaSave(
    page,
    'ErpHrCompetency',
    { code: uniq(`E2E-C2-${tag}`), name: `comp2-${tag}`, category: 'SKILL' },
    'id',
  );
  const c3 = await createViaSave(
    page,
    'ErpHrCompetency',
    { code: uniq(`E2E-C3-${tag}`), name: `comp3-${tag}`, category: 'SKILL' },
    'id',
  );
  const dept = await createViaSave(
    page,
    'ErpHrDepartment',
    { code: uniq(`E2E-DEPT-${tag}`), name: `dept-${tag}` },
    'id',
  );
  const pos = await createViaSave(
    page,
    'ErpHrPosition',
    { code: uniq(`E2E-POS-${tag}`), name: `pos-${tag}`, departmentId: Number(dept.id) },
    'id',
  );
  await createViaSave(
    page,
    'ErpHrRoleCompetency',
    { positionId: Number(pos.id), competencyId: Number(c1.id), requiredLevel: 5 },
    'id',
  );
  await createViaSave(
    page,
    'ErpHrRoleCompetency',
    { positionId: Number(pos.id), competencyId: Number(c2.id), requiredLevel: 5 },
    'id',
  );
  await createViaSave(
    page,
    'ErpHrRoleCompetency',
    { positionId: Number(pos.id), competencyId: Number(c3.id), requiredLevel: 5 },
    'id',
  );
  const emp = await createViaSave(
    page,
    'ErpHrEmployee',
    {
      code: uniq(`E2E-EMP-${tag}`),
      firstName: '评',
      lastName: tag,
      fullName: `评估${tag}`,
      gender: 'FEMALE',
      hireDate: '2023-01-01',
      employmentStatus: 'ACTIVE',
      employeeType: 'FULL_TIME',
      positionId: Number(pos.id),
      orgId: 2,
    },
    'id',
  );
  const assessment = await createViaSave(
    page,
    'ErpHrEmployeeAssessment',
    {
      employeeId: Number(emp.id),
      assessmentType: 'SELF',
      status: 'DRAFT',
      assessmentDate: '2026-07-18',
      businessDate: '2026-07-18',
      orgId: 2,
    },
    'id',
  );
  return {
    employeeId: emp.id,
    positionId: pos.id,
    departmentId: dept.id,
    competencyIds: [c1.id, c2.id, c3.id],
    assessmentId: assessment.id,
  };
}

async function seedDetail(
  page: import('@playwright/test').Page,
  assessmentId: string | number,
  competencyId: string | number,
  actualLevel: number,
): Promise<string> {
  const d = await createViaSave(
    page,
    'ErpHrAssessmentDetail',
    {
      assessmentId: Number(assessmentId),
      competencyId: Number(competencyId),
      actualLevel,
      sourceType: 'SELF',
    },
    'id',
  );
  return d.id;
}

async function cleanupChain(
  page: import('@playwright/test').Page,
  s: AssessmentSetup,
  planId?: string,
): Promise<void> {
  if (planId) {
    await deleteByFilter(page, 'ErpHrDevelopmentPlanItem', eqFilter('planId', Number(planId)));
    await deleteById(page, 'ErpHrDevelopmentPlan', planId);
  }
  await deleteByFilter(page, 'ErpHrGapAnalysis', eqFilter('employeeId', Number(s.employeeId)));
  await deleteByFilter(page, 'ErpHrAssessmentDetail', eqFilter('assessmentId', Number(s.assessmentId)));
  await deleteById(page, 'ErpHrEmployeeAssessment', s.assessmentId);
  await deleteByFilter(page, 'ErpHrRoleCompetency', eqFilter('positionId', Number(s.positionId)));
  await deleteById(page, 'ErpHrEmployee', s.employeeId);
  await deleteById(page, 'ErpHrPosition', s.positionId);
  await deleteById(page, 'ErpHrDepartment', s.departmentId);
  for (const cid of s.competencyIds) {
    await deleteById(page, 'ErpHrCompetency', cid);
  }
}

test.describe('hr competency assessment → gap → development plan closed-loop DIRECT actions', () => {
  test('completeAssessment auto-triggers gap refresh: 3 gaps (MINOR/MODERATE/CRITICAL) + overallScore written back', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrEmployeeAssessment-main');

    const s = await setupAssessmentChain(page, 'hp');
    // c1 actual=4 → gap=1 MINOR；c2 actual=3 → gap=2 MODERATE；c3 actual=2 → gap=3 CRITICAL
    await seedDetail(page, s.assessmentId, s.competencyIds[0], 4);
    await seedDetail(page, s.assessmentId, s.competencyIds[1], 3);
    await seedDetail(page, s.assessmentId, s.competencyIds[2], 2);

    // submitAssessment: DRAFT → SUBMITTED
    await callMutationOk(
      page,
      'ErpHrEmployeeAssessment',
      'submitAssessment',
      { assessmentId: Number(s.assessmentId) },
      'id',
    );
    let st = await verifyState(page, 'ErpHrEmployeeAssessment', s.assessmentId, 'status');
    expect(st.status, 'after submitAssessment status=SUBMITTED').toBe('SUBMITTED');

    // completeAssessment: SUBMITTED → COMPLETED + overallScore 写回（mean(4,3,2)=3.00）
    const completed = await callMutationOk(
      page,
      'ErpHrEmployeeAssessment',
      'completeAssessment',
      { assessmentId: Number(s.assessmentId) },
      'id status overallScore',
    );
    expect(completed.status, 'after completeAssessment status=COMPLETED').toBe('COMPLETED');
    expect(completed.overallScore, 'overallScore written back (mean of 4,3,2 = 3.00)').not.toBeNull();
    expect(Number(completed.overallScore), 'overallScore value=3.00').toBeCloseTo(3.0, 2);

    // 自动 gap refresh 链：经 findFirst 按 employeeId 反查 ErpHrGapAnalysis 3 行存在 + 多档 severity 精确数值断言
    const [g1, g2, g3] = await Promise.all([
      findFirst(
        page,
        'ErpHrGapAnalysis',
        andFilter(
          eqFilter('employeeId', Number(s.employeeId)),
          eqFilter('competencyId', Number(s.competencyIds[0])),
        ),
        'id gapSeverity gapValue requiredLevel actualLevel',
      ),
      findFirst(
        page,
        'ErpHrGapAnalysis',
        andFilter(
          eqFilter('employeeId', Number(s.employeeId)),
          eqFilter('competencyId', Number(s.competencyIds[1])),
        ),
        'id gapSeverity gapValue requiredLevel actualLevel',
      ),
      findFirst(
        page,
        'ErpHrGapAnalysis',
        andFilter(
          eqFilter('employeeId', Number(s.employeeId)),
          eqFilter('competencyId', Number(s.competencyIds[2])),
        ),
        'id gapSeverity gapValue requiredLevel actualLevel',
      ),
    ]);

    expect(g1, 'gap row for c1 exists').not.toBeNull();
    expect((g1 as any).gapSeverity, 'c1 gapValue=1 → MINOR').toBe('MINOR');
    expect(Number((g1 as any).gapValue)).toBe(1);
    expect(Number((g1 as any).requiredLevel)).toBe(5);
    expect(Number((g1 as any).actualLevel)).toBe(4);

    expect(g2, 'gap row for c2 exists').not.toBeNull();
    expect((g2 as any).gapSeverity, 'c2 gapValue=2 → MODERATE').toBe('MODERATE');
    expect(Number((g2 as any).gapValue)).toBe(2);
    expect(Number((g2 as any).actualLevel)).toBe(3);

    expect(g3, 'gap row for c3 exists').not.toBeNull();
    expect((g3 as any).gapSeverity, 'c3 gapValue=3 → CRITICAL').toBe('CRITICAL');
    expect(Number((g3 as any).gapValue)).toBe(3);
    expect(Number((g3 as any).actualLevel)).toBe(2);

    await cleanupChain(page, s);
  });

  test('refreshGapAnalysis dual-entry: internal aggregate (idempotent) + custom levels override + no-role-requirement guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrGapAnalysis-main');

    const s = await setupAssessmentChain(page, 'rf');
    await seedDetail(page, s.assessmentId, s.competencyIds[0], 4);
    await seedDetail(page, s.assessmentId, s.competencyIds[1], 3);
    await seedDetail(page, s.assessmentId, s.competencyIds[2], 2);

    // 先 completeAssessment 触发首次 gap refresh（建立基线 3 行）
    await callMutationOk(page, 'ErpHrEmployeeAssessment', 'submitAssessment',
      { assessmentId: Number(s.assessmentId) }, 'id');
    await callMutationOk(page, 'ErpHrEmployeeAssessment', 'completeAssessment',
      { assessmentId: Number(s.assessmentId) }, 'id');

    const gapCountAfterComplete = await findPageTotal(page, 'ErpHrGapAnalysis',
      eqFilter('employeeId', Number(s.employeeId)));
    expect(gapCountAfterComplete, 'completeAssessment auto-refresh produces 3 gap rows').toBe(3);

    // refreshGapAnalysis 内部聚合入口（重新读 latest COMPLETED → 重算）：幂等覆盖，仍 3 行
    await callMutationOk(page, 'ErpHrGapAnalysis', 'refreshGapAnalysis',
      { employeeId: Number(s.employeeId) }, 'id');
    const gapCountAfterRefresh = await findPageTotal(page, 'ErpHrGapAnalysis',
      eqFilter('employeeId', Number(s.employeeId)));
    expect(gapCountAfterRefresh, 'refreshGapAnalysis idempotent: still 3 rows').toBe(3);

    // refreshGapAnalysisWithLevels 直传 levels 入口：Map<Long,Integer> 经 GraphQL generic Map scalar 传递
    // 重置全部 actualLevel=5 → 全部 gap=0 → NONE（清除 actionable gaps）
    const { errors: err1 } = await callMutation(page, 'ErpHrGapAnalysis', 'refreshGapAnalysisWithLevels', {
      employeeId: Number(s.employeeId),
      aggregatedLevels: input('Map', {
        [Number(s.competencyIds[0])]: 5,
        [Number(s.competencyIds[1])]: 5,
        [Number(s.competencyIds[2])]: 5,
      }),
    }, 'id');
    expect(err1, 'refreshGapAnalysisWithLevels with all-5 levels should succeed').toBeNull();

    const overridden = await findFirst(page, 'ErpHrGapAnalysis',
      andFilter(
        eqFilter('employeeId', Number(s.employeeId)),
        eqFilter('competencyId', Number(s.competencyIds[2])),
      ),
      'gapSeverity gapValue actualLevel');
    expect(overridden, 'overridden gap row exists').not.toBeNull();
    expect((overridden as any).gapSeverity, 'after override all-5: NONE').toBe('NONE');
    expect(Number((overridden as any).gapValue)).toBe(0);
    expect(Number((overridden as any).actualLevel)).toBe(5);

    // 守卫：员工无 position（positionId=null）→ ERR_GAP_NO_ROLE_REQUIREMENT
    // 自包含建无 position 的 employee
    const empNoPos = await createViaSave(page, 'ErpHrEmployee', {
      code: uniq('E2E-EMP-NOPOS-rf'),
      firstName: '无', lastName: '岗', fullName: '无岗员工',
      gender: 'MALE', hireDate: '2023-01-01',
      employmentStatus: 'ACTIVE', employeeType: 'FULL_TIME',
      orgId: 2,
    }, 'id');
    const rej = await callMutation(page, 'ErpHrGapAnalysis', 'refreshGapAnalysis',
      { employeeId: Number(empNoPos.id) }, 'id');
    expect(rej.errors, 'refreshGapAnalysis on employee without position should be rejected').toBeTruthy();
    expect(JSON.stringify(rej.errors), 'reject should carry no-role-requirement token').toContain('岗位');

    await deleteById(page, 'ErpHrEmployee', empNoPos.id);
    await cleanupChain(page, s);
  });

  test('generateDevelopmentPlan + item state machine + completePlan transitions', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrDevelopmentPlan-main');

    const s = await setupAssessmentChain(page, 'dp');
    await seedDetail(page, s.assessmentId, s.competencyIds[0], 4); // MINOR — skipped
    await seedDetail(page, s.assessmentId, s.competencyIds[1], 3); // MODERATE — item
    await seedDetail(page, s.assessmentId, s.competencyIds[2], 2); // CRITICAL — item

    await callMutationOk(page, 'ErpHrEmployeeAssessment', 'submitAssessment',
      { assessmentId: Number(s.assessmentId) }, 'id');
    await callMutationOk(page, 'ErpHrEmployeeAssessment', 'completeAssessment',
      { assessmentId: Number(s.assessmentId) }, 'id');

    // generateDevelopmentPlan：仅 CRITICAL+MODERATE → IN_PROGRESS + 2 items（MINOR 跳过）
    const plan = await callMutationOk(page, 'ErpHrDevelopmentPlan', 'generateDevelopmentPlan',
      { employeeId: Number(s.employeeId) }, 'id status');
    expect(plan, 'generateDevelopmentPlan returns non-null plan').toBeTruthy();
    expect(plan.status, 'new plan status=IN_PROGRESS').toBe('IN_PROGRESS');

    const itemCount = await findPageTotal(page, 'ErpHrDevelopmentPlanItem',
      eqFilter('planId', Number(plan.id)));
    expect(itemCount, '2 plan items (CRITICAL + MODERATE; MINOR skipped)').toBe(2);

    // 经 findFirst 取 CRITICAL gap 对应的 plan item（按 competencyId c3）
    const itemCritical = await findFirst(page, 'ErpHrDevelopmentPlanItem',
      andFilter(
        eqFilter('planId', Number(plan.id)),
        eqFilter('competencyId', Number(s.competencyIds[2])),
      ),
      'id status startDate endDate targetLevel');
    expect(itemCritical, 'CRITICAL-gap plan item exists').not.toBeNull();
    expect((itemCritical as any).status, 'new item status=NOT_STARTED').toBe('NOT_STARTED');
    expect(Number((itemCritical as any).targetLevel), 'targetLevel=requiredLevel=5').toBe(5);

    // updatePlanItemStatus: NOT_STARTED → IN_PROGRESS + startDate 写回
    const started = await callMutationOk(page, 'ErpHrDevelopmentPlan', 'updatePlanItemStatus',
      { planItemId: Number((itemCritical as any).id), status: 'IN_PROGRESS' },
      'id status startDate endDate');
    expect(started.status, 'after updatePlanItemStatus IN_PROGRESS').toBe('IN_PROGRESS');
    expect(started.startDate, 'startDate written back on IN_PROGRESS').not.toBeNull();

    // updatePlanItemStatus: IN_PROGRESS → ACHIEVED + endDate 写回
    const achieved = await callMutationOk(page, 'ErpHrDevelopmentPlan', 'updatePlanItemStatus',
      { planItemId: Number((itemCritical as any).id), status: 'ACHIEVED' },
      'id status startDate endDate');
    expect(achieved.status, 'after updatePlanItemStatus ACHIEVED').toBe('ACHIEVED');
    expect(achieved.endDate, 'endDate written back on ACHIEVED').not.toBeNull();

    // completePlan: IN_PROGRESS → COMPLETED
    await callMutationOk(page, 'ErpHrDevelopmentPlan', 'completePlan',
      { planId: Number(plan.id) }, 'id');
    const planSt = await verifyState(page, 'ErpHrDevelopmentPlan', plan.id, 'status');
    expect(planSt.status, 'after completePlan status=COMPLETED').toBe('COMPLETED');

    await cleanupChain(page, s, plan.id);
  });

  test('guards: submitAssessment no-details + completePlan illegal-transition + generateDevelopmentPlan no-actionable returns null', async ({ page }) => {
    await loginAndNavigate(page, '/ErpHrEmployeeAssessment-main');

    // (a) submitAssessment 无 detail 行 → ERR_ASSESSMENT_NO_DETAILS
    const sGuard = await setupAssessmentChain(page, 'gd');
    const rej = await callMutation(page, 'ErpHrEmployeeAssessment', 'submitAssessment',
      { assessmentId: Number(sGuard.assessmentId) }, 'id');
    expect(rej.errors, 'submitAssessment with no details should be rejected').toBeTruthy();
    expect(JSON.stringify(rej.errors), 'reject should carry no-details token').toContain('明细');

    // 评估状态不变
    const stAfterReject = await verifyState(page, 'ErpHrEmployeeAssessment', sGuard.assessmentId, 'status');
    expect(stAfterReject.status, 'status unchanged after submit guard reject').toBe('DRAFT');

    // (b) generateDevelopmentPlan 无 actionable gap → data null + errors null
    // 用 sGuard chain 但加入 actualLevel=5 的 detail 使全部 gap=0 NONE → 无 actionable
    await seedDetail(page, sGuard.assessmentId, sGuard.competencyIds[0], 5);
    await seedDetail(page, sGuard.assessmentId, sGuard.competencyIds[1], 5);
    await seedDetail(page, sGuard.assessmentId, sGuard.competencyIds[2], 5);
    await callMutationOk(page, 'ErpHrEmployeeAssessment', 'submitAssessment',
      { assessmentId: Number(sGuard.assessmentId) }, 'id');
    await callMutationOk(page, 'ErpHrEmployeeAssessment', 'completeAssessment',
      { assessmentId: Number(sGuard.assessmentId) }, 'id');

    const gapsAllNone = await findPageTotal(page, 'ErpHrGapAnalysis',
      andFilter(
        eqFilter('employeeId', Number(sGuard.employeeId)),
        eqFilter('gapSeverity', 'NONE'),
      ));
    expect(gapsAllNone, 'all 3 gaps are NONE (no actionable)').toBe(3);

    const genRej = await callMutation(page, 'ErpHrDevelopmentPlan', 'generateDevelopmentPlan',
      { employeeId: Number(sGuard.employeeId) }, 'id status');
    expect(genRej.errors, 'no actionable gaps: no errors (returns null)').toBeNull();
    expect(genRej.data, 'no actionable gaps: data null').toBeNull();

    // (c) completePlan illegal transition：构造独立 chain 使 plan 进入 COMPLETED 后再调抛守卫
    const sPlan = await setupAssessmentChain(page, 'cp');
    await seedDetail(page, sPlan.assessmentId, sPlan.competencyIds[2], 2); // CRITICAL — 1 item 足够
    await callMutationOk(page, 'ErpHrEmployeeAssessment', 'submitAssessment',
      { assessmentId: Number(sPlan.assessmentId) }, 'id');
    await callMutationOk(page, 'ErpHrEmployeeAssessment', 'completeAssessment',
      { assessmentId: Number(sPlan.assessmentId) }, 'id');
    const planCp = await callMutationOk(page, 'ErpHrDevelopmentPlan', 'generateDevelopmentPlan',
      { employeeId: Number(sPlan.employeeId) }, 'id status');
    await callMutationOk(page, 'ErpHrDevelopmentPlan', 'completePlan',
      { planId: Number(planCp.id) }, 'id');
    const rej2 = await callMutation(page, 'ErpHrDevelopmentPlan', 'completePlan',
      { planId: Number(planCp.id) }, 'id');
    expect(rej2.errors, 'completePlan on COMPLETED plan should be rejected').toBeTruthy();
    expect(JSON.stringify(rej2.errors), 'reject should carry illegal-transition token').toContain('不允许');

    await cleanupChain(page, sGuard);
    await cleanupChain(page, sPlan, planCp.id);
  });
});
