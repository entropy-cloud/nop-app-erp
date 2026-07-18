import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  verifyState,
  eqFilter,
  andFilter,
  deleteByFilter,
  deleteById,
  findFirst,
} from './_helper';
import {
  cleanupVoucherByBillCode,
  findVoucherIdByBillCode,
  assertVoucherLines,
} from '../orchestration/_helper';

/**
 * maintenance ErpMntVisit complete + MAINTENANCE_LABOR 工时费用化过账浏览器层 E2E
 * （plan 2026-07-18-0949-1 Phase 4）。
 *
 * 验证 visit complete 经 GraphQL /graphql 的全栈可达性 + MAINTENANCE_LABOR(493) 凭证行数值断言：
 *   complete(visitId) @BizMutation → doComplete（VISIT_STATUS IN_PROGRESS→COMPLETED + totalMinutes 计算）
 *   → MaintenanceLaborPostingDispatcher.postLabor（config-gated erp-mnt.labor-posting-enabled=true）
 *   → MAINTENANCE_LABOR 凭证（Dr 6602 折旧费用 / Cr 2211 应付职工薪酬）。
 *
 * 权威实现（ErpMntVisitBizModel.doComplete:154-178）：
 *   setStatus(COMPLETED) + endTime + totalMinutes=Duration.between(startTime,endTime).toMinutes()
 *   + completedAt + updateEntity
 *   + if laborPostingDispatcher.isPostingEnabled(): postLabor(visit, ctx)（失败 LOG.warn 不阻断）。
 *
 * MAINTENANCE_LABOR 凭证（MaintenanceLaborAcctDocProvider.createFacts:55-89）：
 *   Dr 6602 折旧费用（config erp-mnt.expense-subject-code 默认 6602，种子 subjectName 实测为「折旧费用」）
 *   / Cr 2211 应付职工薪酬（config erp-mnt.labor-payable-subject-code 默认 2211）。
 *   billHeadCode = visit.code + "-ML"（MaintenanceLaborPostingDispatcher.java:84）。
 *
 * config 门控（Infrastructure And Config Prereqs）：
 *   webServer JVM arg 追加 `-Derp-mnt.labor-posting-enabled=true -Derp-mnt.default-labor-hourly-rate=80`
 *   （默认 false 向后兼容；E2E 按需开启对齐 spare-part-posting-enabled/subcontract-posting-enabled 范式）。
 *   config-gated 关闭路径（visit complete 无凭证）经后端单测 TestErpMntLaborPosting 场景 4 覆盖。
 *
 * 确定性值（rate=80 元/小时，totalMinutes=60 → 60×80/60=80）：
 *   测试 1: startTime=now-60min, endTime=now → totalMinutes=60 → Dr 6602=80 / Cr 2211=80
 *   测试 2: startTime=now-30min, endTime=now → totalMinutes=30 → Dr 6602=40 / Cr 2211=40
 *   测试 3: startTime=null → totalMinutes=null → 跳过过账，无凭证
 *
 * Phase 4 Decision（Explore 裁定）：测试专用 Visit 隔离。
 *   设备用种子 id=1（EQ-2026-001 RUNNING）；Visit 完整生命周期（DRAFT→SCHEDULED→IN_PROGRESS→COMPLETED）
 *   经 GraphQL 驱动。complete 触发设备状态恢复（UNDER_MAINTENANCE→RUNNING），其他 spec 不依赖
 *   equipment 状态字段，无污染。
 *
 * 清理：MAINTENANCE_LABOR 凭证（billHeadCode=visit.code+"-ML"）+ Visit。
 *   posting 不写 gl_balance（仅 voucher/voucher_line/voucher_bill_r），不污染 finance dashboard 基线。
 */

const ORG = 2;
const EQUIPMENT_ID = 1; // EQ-2026-001 种子设备 RUNNING
const ASSIGNEE_ID = 1; // 种子用户
const RATE = 80; // erp-mnt.default-labor-hourly-rate webServer JVM arg

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

function isoMinutesAgo(min: number): string {
  const d = new Date(Date.now() - min * 60_000);
  // GraphQL TIMESTAMP 经 ISO string 暴露；Nop 接受 ISO 8601 / 'yyyy-MM-dd HH:mm:ss'
  return d.toISOString();
}

function isoNow(): string {
  return new Date().toISOString();
}

test.describe('maintenance ErpMntVisit complete + MAINTENANCE_LABOR posting voucher line assertion', () => {
  test('complete(visitId) → status=COMPLETED + totalMinutes=60 + MAINTENANCE_LABOR voucher Dr 6602 / Cr 2211', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMntVisit-main');

    const code = uniq('E2E-MNT-LBR-VST');

    // 1. 建 DRAFT Visit（startTime 预置 60min 前，endTime 预置 now）
    //    complete 时 doComplete 会重算 totalMinutes = Duration.between(startTime, endTime).toMinutes() = 60
    const visit = await createViaSave(
      page, 'ErpMntVisit',
      {
        code, orgId: ORG, equipmentId: EQUIPMENT_ID, assignedTo: ASSIGNEE_ID,
        visitDate: '2026-07-18', status: 'DRAFT',
        visitType: 'PLANNED',
        startTime: isoMinutesAgo(60), endTime: isoNow(),
      },
      'id code status',
    );

    try {
      // 2. DRAFT → SCHEDULED → IN_PROGRESS → COMPLETED 完整生命周期
      await callMutationOk(page, 'ErpMntVisit', 'schedule', { visitId: visit.id }, 'id status');
      await callMutationOk(page, 'ErpMntVisit', 'start', { visitId: visit.id }, 'id status');

      // 3. complete 触发 doComplete + postLabor
      const completed = await callMutationOk(
        page, 'ErpMntVisit', 'complete', { visitId: visit.id },
        'id status totalMinutes',
      );
      expect(completed.status, 'complete should set status=COMPLETED').toBe('COMPLETED');

      // __get 权威确认 status + totalMinutes
      const visitFinal = await verifyState(page, 'ErpMntVisit', visit.id, 'status totalMinutes');
      expect(visitFinal.status, '__get should confirm COMPLETED').toBe('COMPLETED');
      expect(Number(visitFinal.totalMinutes), 'totalMinutes should be 60').toBe(60);

      // 4. MAINTENANCE_LABOR 凭证行精确数值断言
      //    billHeadCode = visit.code + "-ML"（MaintenanceLaborPostingDispatcher.java:84）
      const voucherId = await findVoucherIdByBillCode(page, code + '-ML', 'NORMAL');
      expect(voucherId, 'MAINTENANCE_LABOR 凭证应存在').toBeTruthy();
      // laborCost = 60 × 80 / 60 = 80（HALF_UP scale=4）
      const expected = (60 * RATE) / 60;
      await assertVoucherLines(page, voucherId, [
        { subjectCode: '6602', dcDirection: 'DEBIT', debitAmount: expected, creditAmount: 0 },
        { subjectCode: '2211', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: expected },
      ]);
    } finally {
      // 清理：MAINTENANCE_LABOR 凭证（billHeadCode=code+"-ML"）+ Visit
      await cleanupVoucherByBillCode(page, code + '-ML');
      await deleteById(page, 'ErpMntVisit', visit.id);
    }
  });

  test('complete(visitId) partial 30min → MAINTENANCE_LABOR voucher Dr 6602=40 / Cr 2211=40', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMntVisit-main');

    const code = uniq('E2E-MNT-LBR-PART');

    const visit = await createViaSave(
      page, 'ErpMntVisit',
      {
        code, orgId: ORG, equipmentId: EQUIPMENT_ID, assignedTo: ASSIGNEE_ID,
        visitDate: '2026-07-18', status: 'DRAFT',
        visitType: 'PLANNED',
        startTime: isoMinutesAgo(30), endTime: isoNow(),
      },
      'id code',
    );

    try {
      await callMutationOk(page, 'ErpMntVisit', 'schedule', { visitId: visit.id }, 'id');
      await callMutationOk(page, 'ErpMntVisit', 'start', { visitId: visit.id }, 'id');
      await callMutationOk(page, 'ErpMntVisit', 'complete', { visitId: visit.id }, 'id status');

      const visitFinal = await verifyState(page, 'ErpMntVisit', visit.id, 'status totalMinutes');
      expect(visitFinal.status).toBe('COMPLETED');
      expect(Number(visitFinal.totalMinutes), 'totalMinutes should be 30').toBe(30);

      // laborCost = 30 × 80 / 60 = 40
      const voucherId = await findVoucherIdByBillCode(page, code + '-ML', 'NORMAL');
      expect(voucherId, '部分时长也应生成 MAINTENANCE_LABOR 凭证').toBeTruthy();
      await assertVoucherLines(page, voucherId, [
        { subjectCode: '6602', dcDirection: 'DEBIT', debitAmount: 40, creditAmount: 0 },
        { subjectCode: '2211', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: 40 },
      ]);
    } finally {
      await cleanupVoucherByBillCode(page, code + '-ML');
      await deleteById(page, 'ErpMntVisit', visit.id);
    }
  });

  test('complete(visitId) totalMinutes=0 → no MAINTENANCE_LABOR voucher', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMntVisit-main');

    const code = uniq('E2E-MNT-LBR-ZERO');

    // 不预置 startTime/endTime（null） → doComplete 不计算 totalMinutes → postLabor 守卫跳过
    const visit = await createViaSave(
      page, 'ErpMntVisit',
      {
        code, orgId: ORG, equipmentId: EQUIPMENT_ID, assignedTo: ASSIGNEE_ID,
        visitDate: '2026-07-18', status: 'DRAFT',
        visitType: 'PLANNED',
      },
      'id code',
    );

    try {
      await callMutationOk(page, 'ErpMntVisit', 'schedule', { visitId: visit.id }, 'id');
      await callMutationOk(page, 'ErpMntVisit', 'start', { visitId: visit.id }, 'id');
      await callMutationOk(page, 'ErpMntVisit', 'complete', { visitId: visit.id }, 'id status');

      const visitFinal = await verifyState(page, 'ErpMntVisit', visit.id, 'status totalMinutes');
      expect(visitFinal.status).toBe('COMPLETED');

      // totalMinutes=null → postLabor 守卫跳过 → 无 MAINTENANCE_LABOR 凭证
      const voucherId = await findVoucherIdByBillCode(page, code + '-ML', 'NORMAL');
      expect(voucherId, 'totalMinutes 缺失 → 不生成 MAINTENANCE_LABOR 凭证').toBeNull();
    } finally {
      // 无凭证需清理；仅删 Visit
      await deleteById(page, 'ErpMntVisit', visit.id);
    }
  });
});
