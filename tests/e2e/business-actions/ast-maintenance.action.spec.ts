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
import { cleanupVoucherByBillCode, findVoucherIdByBillCode, assertVoucherLines } from '../orchestration/_helper';

/**
 * assets ErpAstMaintenance 资产维修生命周期业务动作浏览器层 E2E（plan 2026-07-14-0215-1 Phase 2）。
 *
 * 验证维修工单 DIRECT @BizMutation 全链编排经 GraphQL /graphql 的全栈可达性 + 状态机迁移 +
 * 费用化/资本化双路径过账触发可观测性（posted 标志翻转）+ reverse 红冲 + cancel 异常路径。
 *
 * 0742-1 叠加双路径凭证行精确数值断言：
 *   - MAINTENANCE_EXPENSE：Dr 6602 / Cr 1002，金额=totalCost=300；红冲同向取负。
 *   - MAINTENANCE_CAPITALIZATION：Dr 1601 / Cr 1002，金额=capitalizedAmount=250；红冲同向取负。
 *
 * 权威状态机（ErpAstMaintenanceProcessor，maintenance-status 字典）：
 *   DRAFT → submit(SUBMITTED) → startWork(IN_PROGRESS) → completeWork(COMPLETED) →
 *   decideTreatment(CAPITALIZE|EXPENSE) → approve → post(POSTED + 过账) → reverse(COMPLETED + 红冲)
 *   cancel: DRAFT/SUBMITTED → CANCELLED
 *
 * 双路径裁决：
 *   - EXPENSE（费用化）：post → MAINTENANCE_EXPENSE 凭证 Dr 6602(维修费用) / Cr 1002(银行存款，
 *     独立维修无 maintenanceVisitId)。科目均已在种子（6602 经 0215-1 补齐，1002 种子 id=2）。
 *   - CAPITALIZE（资本化）：post → 资产原值增量 + 折旧重算 + MAINTENANCE_CAPITALIZATION 凭证
 *     Dr 1601(固定资产) / Cr 1002(银行存款)。1601 经 0215-1 补齐。
 *
 * 费用归集：ErpAstMaintenanceCost 行（LABOR/SPARE_PART/SUBCONTRACT），aggregateCost 汇总。
 * post 守卫：totalCost > 0（须有费用行）+ treatment 非空 + approvedAt 非空（config 默认强制审批）。
 *
 * 自包含隔离：EXPENSE 路径用种子资产 id=3（AST-2026-003 IN_SERVICE，EXPENSE 不修改资产卡片）；
 * CAPITALIZE 路径自包含建测试资产（避免原值增量污染种子资产）。唯一维修 code `E2E-AST-MNT-<ts>`。
 *
 * 过账科目依赖（Phase 1 Decision）：MaintenanceExpenseAcctDocProvider 默认 6602/1002；
 * MaintenanceCapitalizationAcctDocProvider 默认 1601/1002。6602/1601 经 0215-1 补齐种子。
 *
 * 种子引用：org id=2 / category id=1（AST-CAT-IT）/ currency id=1（CNY）/
 * acctSchema ACCT-FIN-01 id=1 / seed asset id=3（AST-2026-003 IN_SERVICE）。
 */
const ORG_ID = 2;
const CATEGORY_ID = 1;
const CURRENCY_ID = 1;
const SEED_ASSET_ID = 3; // AST-2026-003 IN_SERVICE（EXPENSE 路径，不修改资产）
const BDATE = '2026-07-10';

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

test.describe('assets ErpAstMaintenance lifecycle (EXPENSE + CAPITALIZE dual path)', () => {
  test('EXPENSE path: create → submit → startWork → completeWork → decideTreatment(EXPENSE) → approve → post(POSTED) → reverse(COMPLETED)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstMaintenance-main');

    const mntCode = uniq('E2E-AST-MNT-EXP');
    // createMaintenance: 建 DRAFT 维修工单（assetId=种子资产3，maintenanceVisitId=null 独立维修）
    const mnt = await callMutationOk(
      page, 'ErpAstMaintenance', 'createMaintenance',
      {
        assetId: SEED_ASSET_ID, code: mntCode, name: `E2E 维修费用化 ${mntCode}`,
        businessDate: BDATE, reason: 'E2E 维修测试',
      },
      'id code status',
    );
    expect(mnt.status, 'createMaintenance should set status=DRAFT').toBe('DRAFT');

    try {
      // 费用归集行（LABOR 200 + SPARE_PART 100 = totalCost 300）
      await createViaSave(
        page, 'ErpAstMaintenanceCost',
        { maintenanceId: mnt.id, orgId: ORG_ID, costType: 'LABOR', amount: 200, businessDate: BDATE, currencyId: CURRENCY_ID },
        'id',
      );
      await createViaSave(
        page, 'ErpAstMaintenanceCost',
        { maintenanceId: mnt.id, orgId: ORG_ID, costType: 'SPARE_PART', amount: 100, businessDate: BDATE, currencyId: CURRENCY_ID },
        'id',
      );

      // submit → SUBMITTED
      const submitted = await callMutationOk(
        page, 'ErpAstMaintenance', 'submit', { id: mnt.id }, 'id status',
      );
      expect(submitted.status, 'submit should transition DRAFT → SUBMITTED').toBe('SUBMITTED');

      // startWork → IN_PROGRESS
      const started = await callMutationOk(
        page, 'ErpAstMaintenance', 'startWork', { id: mnt.id }, 'id status',
      );
      expect(started.status, 'startWork should transition SUBMITTED → IN_PROGRESS').toBe('IN_PROGRESS');

      // completeWork → COMPLETED
      const completed = await callMutationOk(
        page, 'ErpAstMaintenance', 'completeWork', { id: mnt.id }, 'id status',
      );
      expect(completed.status, 'completeWork should transition IN_PROGRESS → COMPLETED').toBe('COMPLETED');

      // decideTreatment(EXPENSE)
      const decided = await callMutationOk(
        page, 'ErpAstMaintenance', 'decideTreatment',
        { id: mnt.id, treatment: 'EXPENSE' },
        'id status treatment totalCostAmount',
      );
      expect(decided.treatment, 'decideTreatment should set treatment=EXPENSE').toBe('EXPENSE');
      expect(Number(decided.totalCostAmount), 'totalCost=200+100=300').toBe(300);

      // approve（config erp-ast.maintenance-require-approval 默认 true → post 前置）
      await callMutationOk(
        page, 'ErpAstMaintenance', 'approve', { id: mnt.id }, 'id status',
      );

      // post → POSTED + MAINTENANCE_EXPENSE 凭证（Dr 6602 / Cr 1002）
      const posted = await callMutationOk(
        page, 'ErpAstMaintenance', 'post', { id: mnt.id }, 'id status posted',
      );
      expect(posted.status, 'post should transition COMPLETED → POSTED').toBe('POSTED');
      expect(posted.posted, 'MAINTENANCE_EXPENSE posting should succeed → posted=true').toBe(true);

      const verified = await verifyState(page, 'ErpAstMaintenance', mnt.id, 'status posted');
      expect(verified.status, '__get should confirm POSTED').toBe('POSTED');
      expect(verified.posted, '__get should confirm posted=true').toBe(true);

      // MAINTENANCE_EXPENSE 正向凭证行精确数值断言（0742-1）：
      //   Dr 6602(维修费用) / Cr 1002(银行存款，独立维修 linkedVisit=false)，金额=totalCost=300
      // 金额派生：LABOR(200) + SPARE_PART(100) = totalCost 300。billHeadCode = mntCode。
      const expNormalVoucherId = await findVoucherIdByBillCode(page, mntCode, 'NORMAL');
      expect(expNormalVoucherId, 'MAINTENANCE_EXPENSE NORMAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, expNormalVoucherId, [
        { subjectCode: '6602', dcDirection: 'DEBIT', debitAmount: 300, creditAmount: 0 },
        { subjectCode: '1002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: 300 },
      ]);

      // reverse → COMPLETED + 红冲凭证 + posted=false
      const reversed = await callMutationOk(
        page, 'ErpAstMaintenance', 'reverse', { id: mnt.id }, 'id status posted reversed',
      );
      expect(reversed.status, 'reverse should transition POSTED → COMPLETED').toBe('COMPLETED');
      expect(reversed.posted, 'reverse should clear posted=false').toBe(false);
      expect(reversed.reversed, 'reverse should set reversed=true').toBe(true);

      const verifiedRev = await verifyState(page, 'ErpAstMaintenance', mnt.id, 'status posted reversed');
      expect(verifiedRev.status, '__get should confirm COMPLETED after reverse').toBe('COMPLETED');
      expect(verifiedRev.posted, '__get should confirm posted=false').toBe(false);

      // MAINTENANCE_EXPENSE 红冲凭证行断言（0742-1）：REVERSAL 同向取负（Dr 6602=-300 / Cr 1002=-300）
      const expReversalVoucherId = await findVoucherIdByBillCode(page, mntCode, 'REVERSAL');
      expect(expReversalVoucherId, 'MAINTENANCE_EXPENSE REVERSAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, expReversalVoucherId, [
        { subjectCode: '6602', dcDirection: 'DEBIT', debitAmount: -300, creditAmount: 0 },
        { subjectCode: '1002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -300 },
      ]);

      // 清理凭证（billHeadCode=mntCode，NORMAL+REVERSAL）
      await cleanupVoucherByBillCode(page, mntCode);
    } finally {
      // finally 兜底：费用行 → 维修工单
      await deleteByFilter(page, 'ErpAstMaintenanceCost', eqFilter('maintenanceId', mnt.id));
      await deleteById(page, 'ErpAstMaintenance', mnt.id);
    }
  });

  test('CAPITALIZE path: post → asset originalValue increment + MAINTENANCE_CAPITALIZATION voucher → reverse rollback', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstMaintenance-main');

    // 自包含建测试资产（CAPITALIZE 会修改资产原值，避开种子污染）
    const assetCode = uniq('E2E-AST-MNT-CAP-ASSET');
    const asset = await createViaSave(
      page, 'ErpAstAsset',
      {
        code: assetCode,
        name: `E2E 维修资本化测试资产 ${assetCode}`,
        orgId: ORG_ID,
        categoryId: CATEGORY_ID,
        acquisitionDate: '2026-06-15',
        currencyId: CURRENCY_ID,
        originalValue: 1000,
        residualValue: 0,
        depreciationMethod: 'STRAIGHT_LINE',
        usefulLifeMonths: 36,
        status: 'IN_SERVICE',
        accumulatedDepreciation: 0,
        netBookValue: 1000,
      },
      'id originalValue netBookValue',
    );

    const mntCode = uniq('E2E-AST-MNT-CAP');
    const mnt = await callMutationOk(
      page, 'ErpAstMaintenance', 'createMaintenance',
      {
        assetId: asset.id, code: mntCode, name: `E2E 维修资本化 ${mntCode}`,
        businessDate: BDATE, reason: 'E2E 维修资本化测试',
      },
      'id code status',
    );

    try {
      // 费用归集（SUBCONTRACT 250 = totalCost 250，资本化金额默认=totalCost）
      await createViaSave(
        page, 'ErpAstMaintenanceCost',
        { maintenanceId: mnt.id, orgId: ORG_ID, costType: 'SUBCONTRACT', amount: 250, businessDate: BDATE, currencyId: CURRENCY_ID },
        'id',
      );

      await callMutationOk(page, 'ErpAstMaintenance', 'submit', { id: mnt.id }, 'id status');
      await callMutationOk(page, 'ErpAstMaintenance', 'startWork', { id: mnt.id }, 'id status');
      await callMutationOk(page, 'ErpAstMaintenance', 'completeWork', { id: mnt.id }, 'id status');

      // decideTreatment(CAPITALIZE)，capitalizedAmount 默认=totalCost=250
      const decided = await callMutationOk(
        page, 'ErpAstMaintenance', 'decideTreatment',
        { id: mnt.id, treatment: 'CAPITALIZE' },
        'id treatment capitalizedAmount totalCostAmount',
      );
      expect(decided.treatment, 'treatment=CAPITALIZE').toBe('CAPITALIZE');
      expect(Number(decided.capitalizedAmount), 'capitalizedAmount=250').toBe(250);

      await callMutationOk(page, 'ErpAstMaintenance', 'approve', { id: mnt.id }, 'id status');

      // post → POSTED + 资产原值增量(250) + MAINTENANCE_CAPITALIZATION 凭证（Dr 1601 / Cr 1002）
      const posted = await callMutationOk(
        page, 'ErpAstMaintenance', 'post', { id: mnt.id }, 'id status posted',
      );
      expect(posted.status, 'post should transition COMPLETED → POSTED').toBe('POSTED');
      expect(posted.posted, 'MAINTENANCE_CAPITALIZATION posting → posted=true').toBe(true);

      // 资产原值增量断言（1000 + 250 = 1250）经 __get 独立核实
      const assetAfterPost = await verifyState(page, 'ErpAstAsset', asset.id, 'originalValue netBookValue');
      expect(Number(assetAfterPost.originalValue), 'asset originalValue=1000+250').toBe(1250);
      expect(Number(assetAfterPost.netBookValue), 'asset netBookValue=1000+250').toBe(1250);

      // MAINTENANCE_CAPITALIZATION 正向凭证行精确数值断言（0742-1）：
      //   Dr 1601(固定资产) / Cr 1002(银行存款)，金额=capitalizedAmount=250
      // 金额派生：SUBCONTRACT(250) = totalCost 250；capitalizedAmount 默认=totalCost。billHeadCode = mntCode。
      const capNormalVoucherId = await findVoucherIdByBillCode(page, mntCode, 'NORMAL');
      expect(capNormalVoucherId, 'MAINTENANCE_CAPITALIZATION NORMAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, capNormalVoucherId, [
        { subjectCode: '1601', dcDirection: 'DEBIT', debitAmount: 250, creditAmount: 0 },
        { subjectCode: '1002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: 250 },
      ]);

      // reverse → COMPLETED + 资产原值回退 + 红冲凭证
      const reversed = await callMutationOk(
        page, 'ErpAstMaintenance', 'reverse', { id: mnt.id }, 'id status posted',
      );
      expect(reversed.status, 'reverse should transition POSTED → COMPLETED').toBe('COMPLETED');
      expect(reversed.posted, 'reverse should clear posted=false').toBe(false);

      // 资产原值回退断言（1250 - 250 = 1000）
      const assetAfterReverse = await verifyState(page, 'ErpAstAsset', asset.id, 'originalValue netBookValue');
      expect(Number(assetAfterReverse.originalValue), 'reverse should rollback originalValue=1000').toBe(1000);
      expect(Number(assetAfterReverse.netBookValue), 'reverse should rollback netBookValue=1000').toBe(1000);

      // MAINTENANCE_CAPITALIZATION 红冲凭证行断言（0742-1）：REVERSAL 同向取负（Dr 1601=-250 / Cr 1002=-250）
      const capReversalVoucherId = await findVoucherIdByBillCode(page, mntCode, 'REVERSAL');
      expect(capReversalVoucherId, 'MAINTENANCE_CAPITALIZATION REVERSAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, capReversalVoucherId, [
        { subjectCode: '1601', dcDirection: 'DEBIT', debitAmount: -250, creditAmount: 0 },
        { subjectCode: '1002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -250 },
      ]);

      await cleanupVoucherByBillCode(page, mntCode);
    } finally {
      // finally 兜底：折旧计划（recalculate 生成 PENDING 行）→ 费用行 → 维修工单 → 测试资产
      await deleteByFilter(page, 'ErpAstDepreciationSchedule', eqFilter('assetId', asset.id));
      await deleteByFilter(page, 'ErpAstMaintenanceCost', eqFilter('maintenanceId', mnt.id));
      await deleteById(page, 'ErpAstMaintenance', mnt.id);
      await deleteById(page, 'ErpAstAsset', asset.id);
    }
  });

  test('cancel path: submit(SUBMITTED) → cancel(CANCELLED) + illegal IN_PROGRESS→cancel guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstMaintenance-main');

    const mntCode = uniq('E2E-AST-MNT-CNL');
    const mnt = await callMutationOk(
      page, 'ErpAstMaintenance', 'createMaintenance',
      {
        assetId: SEED_ASSET_ID, code: mntCode, name: `E2E 维修取消 ${mntCode}`,
        businessDate: BDATE, reason: 'E2E 取消测试',
      },
      'id status',
    );

    try {
      await callMutationOk(page, 'ErpAstMaintenance', 'submit', { id: mnt.id }, 'id status');

      // cancel: SUBMITTED（DRAFT/SUBMITTED 可达）→ CANCELLED
      const cancelled = await callMutationOk(
        page, 'ErpAstMaintenance', 'cancel', { id: mnt.id }, 'id status',
      );
      expect(cancelled.status, 'cancel should transition SUBMITTED → CANCELLED').toBe('CANCELLED');

      const verified = await verifyState(page, 'ErpAstMaintenance', mnt.id, 'status');
      expect(verified.status, '__get should confirm CANCELLED').toBe('CANCELLED');

      // 非法迁移守卫：IN_PROGRESS 态 cancel 拒绝（仅 DRAFT/SUBMITTED 可 cancel）
      const mntCode2 = uniq('E2E-AST-MNT-CNL-GUARD');
      const mnt2 = await callMutationOk(
        page, 'ErpAstMaintenance', 'createMaintenance',
        { assetId: SEED_ASSET_ID, code: mntCode2, name: `E2E 维修取消守卫 ${mntCode2}`, businessDate: BDATE, reason: 'guard' },
        'id status',
      );
      await callMutationOk(page, 'ErpAstMaintenance', 'submit', { id: mnt2.id }, 'id status');
      await callMutationOk(page, 'ErpAstMaintenance', 'startWork', { id: mnt2.id }, 'id status');

      const rej = await callMutation(page, 'ErpAstMaintenance', 'cancel', { id: mnt2.id }, 'id');
      expect(rej.errors, 'cancel from IN_PROGRESS should be rejected').toBeTruthy();

      await deleteById(page, 'ErpAstMaintenance', mnt2.id);
    } finally {
      await deleteById(page, 'ErpAstMaintenance', mnt.id);
    }
  });
});
