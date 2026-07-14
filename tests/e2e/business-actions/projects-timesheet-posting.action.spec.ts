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
} from './_helper';
import { findFirst, cleanupVoucherByBillCode, findVoucherIdByBillCode, assertVoucherLines } from '../orchestration/_helper';

/**
 * projects ErpPrjTimesheet 工时过账生命周期浏览器层 E2E（plan 2026-07-14-0742-2 Phase 1）。
 *
 * 验证工时 DIRECT @BizMutation 状态机 submit→approve→posted 全栈可达性 + PROJECT_COST_COLLECTION
 * 业财过账凭证行精确数值断言（Dr 5101 项目成本 / Cr 2211 应付职工薪酬）+ cancel reverse 红冲凭证行同向取负断言。
 *
 * 权威状态机（ErpPrjTimesheetBizModel + wf/approve-status）：
 *   UNSUBMITTED --submit--> SUBMITTED --approve(tryPost→posted=true)--> APPROVED
 *   APPROVED --cancel(reverse 若 posted)--> UNSUBMITTED
 *
 * costAmount 确定性派生（CostRateResolver 优先级 1 = 工时单填 costRate）：
 *   submit 经 CostRateResolver.resolve 取 timesheet.costRate（最高优先级，>=0 即采纳）→
 *   costAmount = hours(8) × costRate(100) = 800（setScale 4 HALF_UP）。
 *   故无需 employee rate 或 config erp-prj.default-labor-cost-rate 回退。
 *
 * 过账科目依赖（TimesheetPostingDispatcher.buildEvent）：
 *   - 借方科目 = 项目类型 defaultSubjectId（**数字行 ID**，经 resolveSubjectCode 解析为 code）。
 *     本 spec 自包含建 ProjectType(defaultSubjectId=32) → 解析为 code 5101（种子 id=32，本计划新增）。
 *     defaultSubjectId 缺失抛 ERR_PROJECT_DEBIT_SUBJECT_NOT_RESOLVED。
 *   - 贷方科目 = config erp-prj.default-payroll-subject-id（值为科目**编码** `2211`，非种子行 ID——config 键名
 *     含 `-id` 后缀但消费方 ErpPrjConfigs.defaultPayrollSubjectCode() 按 code 直填 BILL_DATA_CREDIT_SUBJECT_CODE，
 *     与借方 defaultSubjectId=数字ID 的非对称性经 webServer JVM arg `-Derp-prj.default-payroll-subject-id=2211` 满足）。
 *   - billHeadCode = timesheet.getCode()。
 *   - 过账失败隔离（tryPost 吞异常返回 false 保持 posted=false）；冲销是硬前置（cancel 内 reverse 失败向上抛）。
 *
 * 自包含隔离：建测试专用 ProjectType + Project(OPEN) + Task(IN_PROGRESS) + Timesheet(UNSUBMITTED, costRate=100,
 * hours=8)。cleanup 逐域删除：凭证（billHeadCode=timesheet.code，NORMAL+REVERSAL）→ 成本归集头（projectId，
 * approve 同事务经 ProjectCostAggregator 写入）→ timesheet → task → project → projectType。
 *
 * 种子引用：org id=2 / currency id=1（CNY）/ employee id=1（HR-EMP-001，timesheet.userId 必填 FK）。
 */
const ORG_ID = 2;
const CURRENCY_ID = 1;
const EMPLOYEE_ID = 1;
const SUBJECT_5101_ID = 32; // 项目成本（本计划新增种子行）
const WORK_DATE = '2026-07-10';
const HOURS = 8;
const COST_RATE = 100;
const COST_AMOUNT = HOURS * COST_RATE; // 800

test.describe('projects ErpPrjTimesheet posting lifecycle', () => {
  test('submit → approve(posted) → PROJECT_COST_COLLECTION voucher Dr 5101/Cr 2211 → cancel(reverse) + illegal guards', async ({ page }) => {
    await loginAndNavigate(page, '/ErpPrjTimesheet-main');

    const ts = Date.now();
    const typeCode = `E2E-PRJ-TS-TYPE-${ts}`;
    const projectCode = `E2E-PRJ-TS-PRJ-${ts}`;
    const taskTitle = `E2E-PRJ-TS-TASK-${ts}`;
    const timesheetCode = `E2E-PRJ-TS-${ts}`;

    // setup 变量声明于 try 外，setup 本体置于 try 内——任何 setup 失败亦经 finally 兜底清理（防孤儿 OPEN 项目污染 dashboard 基线）
    let projectType: any, project: any, task: any, timesheet: any;
    try {
      // 自包含 setup：ProjectType(defaultSubjectId → 解析 5101) + Project(OPEN) + Task(IN_PROGRESS)
      projectType = await createViaSave(
        page, 'ErpPrjProjectType',
        { code: typeCode, name: 'E2E 工时过账项目类型', defaultSubjectId: SUBJECT_5101_ID },
        'id',
      );
      project = await createViaSave(
        page, 'ErpPrjProject',
        {
          code: projectCode, name: `E2E 工时项目 ${projectCode}`, orgId: ORG_ID,
          projectTypeId: projectType.id, currencyId: CURRENCY_ID,
          startDate: '2026-06-01', endDate: '2026-12-31', status: 'OPEN',
        },
        'id',
      );
      task = await createViaSave(
        page, 'ErpPrjTask',
        { projectId: project.id, title: taskTitle, status: 'IN_PROGRESS', priority: 'NORMAL' },
        'id',
      );
      // 工时单：costRate 单填（CostRateResolver 最高优先级）→ costAmount 确定性派生
      timesheet = await createViaSave(
        page, 'ErpPrjTimesheet',
        {
          code: timesheetCode, projectId: project.id, taskId: task.id, userId: EMPLOYEE_ID,
          orgId: ORG_ID, workDate: WORK_DATE, hours: HOURS, costRate: COST_RATE, currencyId: CURRENCY_ID,
          status: 'UNSUBMITTED',
        },
        'id code status costRate costAmount posted',
      );
      expect(timesheet.id, '__save should create an UNSUBMITTED timesheet').toBeTruthy();
      expect(timesheet.status, 'new timesheet status=UNSUBMITTED').toBe('UNSUBMITTED');
      // 非法迁移守卫 1：UNSUBMITTED → approve（须 SUBMITTED）须被拒
      const rejApprove = await callMutation(page, 'ErpPrjTimesheet', 'approve', { timesheetId: timesheet.id }, 'id');
      expect(rejApprove.errors, 'approve from UNSUBMITTED should be rejected').toBeTruthy();

      // submit: UNSUBMITTED → SUBMITTED
      const submitted = await callMutationOk(
        page, 'ErpPrjTimesheet', 'submit', { timesheetId: timesheet.id }, 'id status costRate costAmount',
      );
      expect(submitted.status, 'submit should transition UNSUBMITTED → SUBMITTED').toBe('SUBMITTED');
      expect(Number(submitted.costRate), 'submit should adopt timesheet.costRate=100').toBe(COST_RATE);
      expect(Number(submitted.costAmount), 'submit should derive costAmount=hours×costRate=800').toBe(COST_AMOUNT);

      // approve: SUBMITTED → APPROVED + posted=true（PROJECT_COST_COLLECTION 过账）
      const approved = await callMutationOk(
        page, 'ErpPrjTimesheet', 'approve', { timesheetId: timesheet.id }, 'id status posted',
      );
      expect(approved.status, 'approve should transition SUBMITTED → APPROVED').toBe('APPROVED');
      expect(approved.posted, 'approve should trigger PROJECT_COST_COLLECTION posting → posted=true').toBe(true);

      const verified = await verifyState(page, 'ErpPrjTimesheet', timesheet.id, 'status posted');
      expect(verified.status, '__get should confirm APPROVED').toBe('APPROVED');
      expect(verified.posted, '__get should confirm posted=true').toBe(true);

      // 非法迁移守卫 2：APPROVED → submit 须被拒
      const rejSubmit = await callMutation(page, 'ErpPrjTimesheet', 'submit', { timesheetId: timesheet.id }, 'id');
      expect(rejSubmit.errors, 'submit from APPROVED should be rejected').toBeTruthy();

      // PROJECT_COST_COLLECTION 正向凭证行精确数值断言：Dr 5101(项目成本) / Cr 2211(应付职工薪酬)，金额=800
      const normalVoucherId = await findVoucherIdByBillCode(page, timesheetCode, 'NORMAL');
      expect(normalVoucherId, 'PROJECT_COST_COLLECTION NORMAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, normalVoucherId, [
        { subjectCode: '5101', dcDirection: 'DEBIT', debitAmount: COST_AMOUNT, creditAmount: 0 },
        { subjectCode: '2211', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: COST_AMOUNT },
      ]);

      // cancel: APPROVED → UNSUBMITTED + reverse 红冲（posted=true 故先红字冲销）
      const cancelled = await callMutationOk(
        page, 'ErpPrjTimesheet', 'cancel', { timesheetId: timesheet.id }, 'id status posted',
      );
      expect(cancelled.status, 'cancel should transition APPROVED → UNSUBMITTED').toBe('UNSUBMITTED');
      expect(cancelled.posted, 'cancel should reverse posting → posted=false').toBe(false);

      // 红冲凭证行断言：REVERSAL 凭证同向取负（Dr 5101=-800 / Cr 2211=-800）
      const reversalVoucherId = await findVoucherIdByBillCode(page, timesheetCode, 'REVERSAL');
      expect(reversalVoucherId, 'PROJECT_COST_COLLECTION REVERSAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, reversalVoucherId, [
        { subjectCode: '5101', dcDirection: 'DEBIT', debitAmount: -COST_AMOUNT, creditAmount: 0 },
        { subjectCode: '2211', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -COST_AMOUNT },
      ]);
    } finally {
      // cleanup：凭证（NORMAL+REVERSAL）→ 成本归集头（approve 同事务写入）→ timesheet → task → project → projectType
      // setup 在 try 内，任一 setup 失败时变量可能 undefined → 逐项 null 守卫
      if (timesheet?.code) await cleanupVoucherByBillCode(page, timesheet.code);
      if (project?.id) await deleteByFilter(page, 'ErpPrjCostCollection', eqFilter('projectId', project.id));
      if (timesheet?.id) await deleteById(page, 'ErpPrjTimesheet', timesheet.id);
      if (task?.id) await deleteById(page, 'ErpPrjTask', task.id);
      if (project?.id) await deleteById(page, 'ErpPrjProject', project.id);
      if (projectType?.id) await deleteById(page, 'ErpPrjProjectType', projectType.id);
    }
  });
});
