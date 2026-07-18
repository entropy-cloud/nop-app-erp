import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  verifyState,
  eqFilter,
  deleteById,
} from './_helper';
import { GraphQLClient } from '../pages';
import {
  cleanupVoucherByBillCode,
  findVoucherIdByBillCode,
  assertVoucherLines,
  findFirst,
} from '../orchestration/_helper';

/**
 * maintenance ErpMntVisit cancel + MAINTENANCE_LABOR 凭证红冲浏览器层 E2E
 * （plan 2026-07-18-1745-1 Phase 3）。
 *
 * 验证 cancel 在已过账 visit 上触发 MAINTENANCE_LABOR 凭证红冲经 GraphQL /graphql 全栈可达：
 *   cancel(visitId) @BizMutation → doCancel（status=CANCELLED）→
 *   if (laborPostingDispatcher.isPostingEnabled()) laborPostingDispatcher.reverseLabor(visit)
 *   → MntPostingExecutor.reverse → IErpFinVoucherBiz.reverse（红字凭证 + 原凭证 isReversed=true）。
 *
 * 状态机前置（Non-Goal：不新增 cancel 入口语义变化）：
 *   validateNotTerminal 仅允许 DRAFT/SCHEDULED/IN_PROGRESS → CANCELLED；COMPLETED 终态不可 cancel。
 *   实际场景：操作员发现「错误完工」（已 complete 触发 postLabor 生成 ML 凭证）需回退时，先经 __save
 *   将 status 改回 IN_PROGRESS（un-common 但合法的纠错编辑），再 cancel 触发红冲。本 spec 复现此路径。
 *
 * 权威实现（ErpMntVisitBizModel.doCancel:183-202）：
 *   setStatus(CANCELLED) + updateEntity
 *   + if (isPostingEnabled) try { reverseLabor(visit) } catch LOG.warn（吞异常保持 cancel 终态）
 *
 * MAINTENANCE_LABOR 红字凭证（IErpFinVoucherBiz.reverse → ErpFinPostingProcessor.reverseProcess）：
 *   原凭证 isReversed=true（O-8 公共流程 markOriginalVoucherReversed）
 *   + 新建红字凭证 postingType=REVERSAL + 行同向取负（dcDirection 不变，金额取负：Dr 6602=-X / Cr 2211=-X）
 *   + 红字凭证与原凭证共用同一 billHeadCode（voucher_bill_r 回链反查区分 postingType）。
 *
 * 确定性值（与 mnt-labor-posting.action.spec.ts 一致）：
 *   rate=80 元/小时（webServer JVM arg），totalMinutes=60 → laborCost=60×80/60=80
 *   红冲：Dr 6602=-80 / Cr 2211=-80
 *
 * 清理：MAINTENANCE_LABOR 凭证（billHeadCode=visit.code+"-ML"，含原+红字）+ Visit。
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
  return d.toISOString();
}

function isoNow(): string {
  return new Date().toISOString();
}

test.describe('maintenance ErpMntVisit cancel + MAINTENANCE_LABOR voucher reversal assertion', () => {
  test('complete→saveback IN_PROGRESS→cancel triggers ML voucher reversal (Dr 6602=-80 / Cr 2211=-80)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMntVisit-main');

    const code = uniq('E2E-MNT-CNL-RV');

    // 1. 建 DRAFT Visit（startTime 预置 60min 前，endTime 预置 now → complete 时 totalMinutes=60）
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
      await callMutationOk(page, 'ErpMntVisit', 'complete', { visitId: visit.id }, 'id status totalMinutes');

      // 3. 前置断言：complete 已生成 MAINTENANCE_LABOR 凭证
      const originalVoucherId = await findVoucherIdByBillCode(page, code + '-ML', 'NORMAL');
      expect(originalVoucherId, '前置：complete 应生成 MAINTENANCE_LABOR NORMAL 凭证').toBeTruthy();

      // 4. 状态机纠错：操作员发现「错误完工」，经 __update 将 status 改回 IN_PROGRESS（un-common 但合法编辑）
      //    模拟「已过账但未终态」场景——validateNotTerminal 仅 DRAFT/SCHEDULED/IN_PROGRESS 可 cancel
      //    __save 拒绝已存在 id，须经 __update 部分更新（Nop CrudBizModel 标准 mutation）
      const updateJson: any = await new GraphQLClient(page).update(
        'ErpMntVisit',
        { id: visit.id, status: 'IN_PROGRESS' },
        'id status',
      );
      expect(updateJson, 'ErpMntVisit__update should return updated visit').not.toBeNull();
      const beforeCancel = await verifyState(page, 'ErpMntVisit', visit.id, 'status');
      expect(beforeCancel.status, '前置：__update 改回 IN_PROGRESS').toBe('IN_PROGRESS');

      // 5. 执行 cancel → doCancel 触发 reverseLabor
      const cancelled = await callMutationOk(
        page, 'ErpMntVisit', 'cancel', { visitId: visit.id },
        'id status',
      );
      expect(cancelled.status, 'cancel should set status=CANCELLED').toBe('CANCELLED');

      // 6. 原凭证 isReversed=true（ErpFinPostingProcessor.markOriginalVoucherReversed 公共流程）
      const originalAfter = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', Number(originalVoucherId)), 'id isReversed postingType',
      );
      expect(originalAfter?.isReversed, '原 MAINTENANCE_LABOR 凭证应被标记 isReversed=true').toBe(true);

      // 7. 红字凭证存在 + 行同向取负断言
      const reversalVoucherId = await findVoucherIdByBillCode(page, code + '-ML', 'REVERSAL');
      expect(reversalVoucherId, '应存在 MAINTENANCE_LABOR 红字冲销凭证').toBeTruthy();

      const expected = (60 * RATE) / 60; // 80
      await assertVoucherLines(page, reversalVoucherId, [
        // 红字凭证行：dcDirection 不变，金额取负
        { subjectCode: '6602', dcDirection: 'DEBIT', debitAmount: -expected, creditAmount: 0 },
        { subjectCode: '2211', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -expected },
      ]);
    } finally {
      // 清理：MAINTENANCE_LABOR 凭证（原+红字共用 billHeadCode，cleanupVoucherByBillCode 全量清理）+ Visit
      await cleanupVoucherByBillCode(page, code + '-ML');
      await deleteById(page, 'ErpMntVisit', visit.id);
    }
  });
});
