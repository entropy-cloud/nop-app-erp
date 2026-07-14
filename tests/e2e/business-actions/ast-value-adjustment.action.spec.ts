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
import {
  findFirst,
  cleanupVoucherByBillCode,
  findVoucherIdByBillCode,
  assertVoucherLines,
} from '../orchestration/_helper';

/**
 * assets ErpAstValueAdjustment 资产减值/重估 DIRECT useApproval 审批轴业务动作浏览器层 E2E
 * (plan 2026-07-14-1218-1 Phase 1 + Phase 2).
 *
 * 验证 useApproval DIRECT 审批轴（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval/cancel）经 GraphQL /graphql 的全栈可达性 + 三轴状态机迁移 +
 * VALUE_ADJUSTMENT 业财过账触发可观测性（posted 标志翻转 + 凭证行精确数值断言）+
 * 净值联动方向 + 红冲同向取负 + 非法迁移守卫。
 *
 * 权威状态机（ErpAstValueAdjustmentProcessor）：
 *   UNSUBMITTED --submitForApproval--> SUBMITTED --approve--> APPROVED
 *     (触发 VALUE_ADJUSTMENT 过账 + posted=true + docStatus=ACTIVE + 资产净值联动)
 *   SUBMITTED --reject--> REJECTED
 *   SUBMITTED --withdrawApproval--> UNSUBMITTED
 *   APPROVED --reverseApprove--> REJECTED (posted=false + 红冲凭证 + 净值回退 + docStatus 保持 ACTIVE)
 *   DRAFT docStatus --cancel--> CANCELLED (approveStatus 不变)
 *
 * 三种调整类型（adjustmentType 字典 erp-ast/adjustment-type）：
 *   IMPAIRMENT（减值）：Dr 6702 资产减值损失 / Cr 1604 固定资产减值准备，净值减少
 *   REVALUATION_UP（重估增值）：Dr 1601 固定资产 / Cr 4002 资本公积，净值增加
 *   REVALUATION_DOWN（重估减值）：Dr 6702 资产减值损失 / Cr 1601 固定资产，净值减少
 *
 * 过账科目依赖（Phase 1 Decision 裁定）：ValueAdjustmentPostingDispatcher 从 ErpAstAssetCategory
 * 字段解析科目码（subjectId→FIXED_ASSET 1601、expenseSubjectId→IMPAIRMENT_LOSS 6702、
 * depreciationSubjectId→IMPAIRMENT_PROVISION 1604），fallback 到硬编码默认值。
 * CAPITAL_RESERVE 恒为 4002（硬编码不经 category 解析）。种子 category id=1（AST-CAT-IT）
 * 三 subject 字段均 null → fallback 默认值生效，凭证行科目码 = 6702/1604/1601/4002 默认值。
 * 种子 erp_md_subject.csv 经 1218-1 补齐 6702/1604/4002 三科目行（1601 已由 0215-1 补齐）
 * 后过账 happy-path 可达（posted=true），解除优雅降级。
 *
 * 净值联动：approve 时 applyAssetValueChange 按 adjustmentType 调整资产 netBookValue/currentValue
 * （IMPAIRMENT/REVALUATION_DOWN 减、REVALUATION_UP 加）；reverseApprove 时 rollbackAssetValue
 * 回退。折旧基数重算 config-gated erp-ast.revaluation-adjust-depreciation-base 默认 true，
 * 本 spec 仅断言 netBookValue 方向（增/减），不验证重算后折旧计划条目精确金额（Deferred）。
 *
 * 自包含隔离：自包含建 ErpAstAsset（IN_SERVICE + 原值/残值/年限/累计折旧/净值）+
 * ErpAstValueAdjustment（DRAFT/UNSUBMITTED 入口），唯一 code `E2E-AST-VADJ-<ts>`。
 * cleanup 逐域删除：凭证（billHeadCode=adjustment.code，NORMAL+REVERSAL）→
 * 折旧计划（assetId，安全兜底无副作用）→ 调整单 → 资产。
 *
 * 种子引用：org id=2 / category id=1（AST-CAT-IT，三 subject 字段 null → 默认科目码生效）/
 * currency id=1（CNY）/ acctSchema ACCT-FIN-01 id=1。
 */
const ORG_ID = 2;
const CATEGORY_ID = 1;
const CURRENCY_ID = 1;
const BDATE = '2026-07-15';

const ORIGINAL_VALUE = 12000;
const INITIAL_NBV = 12000;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

async function createTestAsset(page: import('@playwright/test').Page, tag: string) {
  const code = uniq(`E2E-AST-VADJ-ASSET-${tag}`);
  return await createViaSave(
    page, 'ErpAstAsset',
    {
      code,
      name: `E2E 价值调整测试资产 ${code}`,
      orgId: ORG_ID,
      categoryId: CATEGORY_ID,
      acquisitionDate: '2026-06-15',
      currencyId: CURRENCY_ID,
      originalValue: ORIGINAL_VALUE,
      residualValue: 0,
      depreciationMethod: 'STRAIGHT_LINE',
      usefulLifeMonths: 36,
      status: 'IN_SERVICE',
      accumulatedDepreciation: 0,
      netBookValue: INITIAL_NBV,
    },
    'id code netBookValue',
  );
}

async function createTestAdjustment(
  page: import('@playwright/test').Page,
  tag: string,
  assetId: any,
  adjustmentType: string,
  amount: number,
) {
  const code = uniq(`E2E-AST-VADJ-${tag}`);
  return await createViaSave(
    page, 'ErpAstValueAdjustment',
    {
      code,
      orgId: ORG_ID,
      assetId,
      businessDate: BDATE,
      currencyId: CURRENCY_ID,
      exchangeRate: 1,
      adjustmentType,
      adjustmentAmount: amount,
      reason: `E2E ${adjustmentType} 测试`,
      docStatus: 'DRAFT',
      approveStatus: 'UNSUBMITTED',
      posted: false,
    },
    'id code approveStatus docStatus posted',
  );
}

async function cleanupAdjustmentChain(
  page: import('@playwright/test').Page,
  adjustmentCode: string,
  adjustmentId: any,
  assetId: any,
): Promise<void> {
  if (adjustmentCode) {
    await cleanupVoucherByBillCode(page, adjustmentCode);
  }
  if (assetId) {
    await deleteByFilter(page, 'ErpAstDepreciationSchedule', eqFilter('assetId', Number(assetId)));
  }
  if (adjustmentId) {
    await deleteById(page, 'ErpAstValueAdjustment', adjustmentId);
  }
  if (assetId) {
    await deleteById(page, 'ErpAstAsset', assetId);
  }
}

// ============================================================
// Phase 1 — 三种调整类型生命周期 + 凭证行数值断言
// ============================================================

test.describe('assets ErpAstValueAdjustment Phase 1 — 3 adjustment types lifecycle + voucher line assertion', () => {
  test('IMPAIRMENT: submit→approve→APPROVED+posted=true + Dr 6702/Cr 1604 + netBookValue decrease', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstValueAdjustment-main');

    const asset = await createTestAsset(page, 'IMP');
    const amount = 3000;
    const adj = await createTestAdjustment(page, 'IMP', asset.id, 'IMPAIRMENT', amount);

    try {
      // submitForApproval: UNSUBMITTED → SUBMITTED
      await callMutationOk(page, 'ErpAstValueAdjustment', 'submitForApproval', { id: adj.id }, 'id');
      let s = await verifyState(page, 'ErpAstValueAdjustment', adj.id, 'approveStatus docStatus');
      expect(s.approveStatus, 'after submit approveStatus=SUBMITTED').toBe('SUBMITTED');
      expect(s.docStatus, 'docStatus stays DRAFT after submit').toBe('DRAFT');

      // approve: SUBMITTED → APPROVED + posted=true + docStatus=ACTIVE
      await callMutationOk(page, 'ErpAstValueAdjustment', 'approve', { id: adj.id }, 'id posted');
      s = await verifyState(page, 'ErpAstValueAdjustment', adj.id, 'approveStatus docStatus posted');
      expect(s.approveStatus, 'after approve approveStatus=APPROVED').toBe('APPROVED');
      expect(s.docStatus, 'after approve docStatus=ACTIVE').toBe('ACTIVE');
      expect(s.posted, 'after approve posted=true (VALUE_ADJUSTMENT posting)').toBe(true);

      // VALUE_ADJUSTMENT(IMPAIRMENT) 正向凭证行精确数值断言：
      //   Dr 6702(资产减值损失) / Cr 1604(固定资产减值准备)，金额=adjustmentAmount=3000
      // billHeadCode = adjustment.code；category 三 subject 字段 null → fallback 默认值 6702/1604 生效
      const normalVid = await findVoucherIdByBillCode(page, adj.code, 'NORMAL');
      expect(normalVid, 'VALUE_ADJUSTMENT IMPAIRMENT NORMAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, normalVid, [
        { subjectCode: '6702', dcDirection: 'DEBIT', debitAmount: amount, creditAmount: 0 },
        { subjectCode: '1604', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: amount },
      ]);

      // 净值联动方向断言：IMPAIRMENT 减值 → netBookValue 减少（12000 - 3000 = 9000）
      const assetAfter = await verifyState(page, 'ErpAstAsset', asset.id, 'netBookValue currentValue');
      expect(Number(assetAfter.netBookValue), 'IMPAIRMENT should decrease netBookValue to 9000').toBe(9000);
      expect(Number(assetAfter.currentValue), 'currentValue mirrors netBookValue').toBe(9000);
    } finally {
      await cleanupAdjustmentChain(page, adj.code, adj.id, asset.id);
    }
  });

  test('REVALUATION_UP: submit→approve→posted=true + Dr 1601/Cr 4002 + netBookValue increase', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstValueAdjustment-main');

    const asset = await createTestAsset(page, 'REVUP');
    const amount = 5000;
    const adj = await createTestAdjustment(page, 'REVUP', asset.id, 'REVALUATION_UP', amount);

    try {
      await callMutationOk(page, 'ErpAstValueAdjustment', 'submitForApproval', { id: adj.id }, 'id');
      let s = await verifyState(page, 'ErpAstValueAdjustment', adj.id, 'approveStatus');
      expect(s.approveStatus, 'after submit approveStatus=SUBMITTED').toBe('SUBMITTED');

      await callMutationOk(page, 'ErpAstValueAdjustment', 'approve', { id: adj.id }, 'id posted');
      s = await verifyState(page, 'ErpAstValueAdjustment', adj.id, 'approveStatus docStatus posted');
      expect(s.approveStatus, 'after approve approveStatus=APPROVED').toBe('APPROVED');
      expect(s.docStatus, 'after approve docStatus=ACTIVE').toBe('ACTIVE');
      expect(s.posted, 'after approve posted=true (VALUE_ADJUSTMENT posting)').toBe(true);

      // VALUE_ADJUSTMENT(REVALUATION_UP) 正向凭证行精确数值断言：
      //   Dr 1601(固定资产) / Cr 4002(资本公积)，金额=adjustmentAmount=5000
      const normalVid = await findVoucherIdByBillCode(page, adj.code, 'NORMAL');
      expect(normalVid, 'VALUE_ADJUSTMENT REVALUATION_UP NORMAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, normalVid, [
        { subjectCode: '1601', dcDirection: 'DEBIT', debitAmount: amount, creditAmount: 0 },
        { subjectCode: '4002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: amount },
      ]);

      // 净值联动方向断言：REVALUATION_UP 重估增值 → netBookValue 增加（12000 + 5000 = 17000）
      const assetAfter = await verifyState(page, 'ErpAstAsset', asset.id, 'netBookValue currentValue');
      expect(Number(assetAfter.netBookValue), 'REVALUATION_UP should increase netBookValue to 17000').toBe(17000);
      expect(Number(assetAfter.currentValue), 'currentValue mirrors netBookValue').toBe(17000);
    } finally {
      await cleanupAdjustmentChain(page, adj.code, adj.id, asset.id);
    }
  });

  test('REVALUATION_DOWN: submit→approve→posted=true + Dr 6702/Cr 1601 + netBookValue decrease', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstValueAdjustment-main');

    const asset = await createTestAsset(page, 'REVDN');
    const amount = 2000;
    const adj = await createTestAdjustment(page, 'REVDN', asset.id, 'REVALUATION_DOWN', amount);

    try {
      await callMutationOk(page, 'ErpAstValueAdjustment', 'submitForApproval', { id: adj.id }, 'id');
      let s = await verifyState(page, 'ErpAstValueAdjustment', adj.id, 'approveStatus');
      expect(s.approveStatus, 'after submit approveStatus=SUBMITTED').toBe('SUBMITTED');

      await callMutationOk(page, 'ErpAstValueAdjustment', 'approve', { id: adj.id }, 'id posted');
      s = await verifyState(page, 'ErpAstValueAdjustment', adj.id, 'approveStatus docStatus posted');
      expect(s.approveStatus, 'after approve approveStatus=APPROVED').toBe('APPROVED');
      expect(s.docStatus, 'after approve docStatus=ACTIVE').toBe('ACTIVE');
      expect(s.posted, 'after approve posted=true (VALUE_ADJUSTMENT posting)').toBe(true);

      // VALUE_ADJUSTMENT(REVALUATION_DOWN) 正向凭证行精确数值断言：
      //   Dr 6702(资产减值损失) / Cr 1601(固定资产)，金额=adjustmentAmount=2000
      const normalVid = await findVoucherIdByBillCode(page, adj.code, 'NORMAL');
      expect(normalVid, 'VALUE_ADJUSTMENT REVALUATION_DOWN NORMAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, normalVid, [
        { subjectCode: '6702', dcDirection: 'DEBIT', debitAmount: amount, creditAmount: 0 },
        { subjectCode: '1601', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: amount },
      ]);

      // 净值联动方向断言：REVALUATION_DOWN 重估减值 → netBookValue 减少（12000 - 2000 = 10000）
      const assetAfter = await verifyState(page, 'ErpAstAsset', asset.id, 'netBookValue currentValue');
      expect(Number(assetAfter.netBookValue), 'REVALUATION_DOWN should decrease netBookValue to 10000').toBe(10000);
      expect(Number(assetAfter.currentValue), 'currentValue mirrors netBookValue').toBe(10000);
    } finally {
      await cleanupAdjustmentChain(page, adj.code, adj.id, asset.id);
    }
  });

  test('illegal-transition guards: UNSUBMITTED→approve rejected + APPROVED→submitForApproval rejected', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstValueAdjustment-main');

    const asset = await createTestAsset(page, 'GUARD');
    const adj = await createTestAdjustment(page, 'GUARD', asset.id, 'IMPAIRMENT', 1000);

    try {
      // UNSUBMITTED → approve: rejected (validateTransitionForApprove requires SUBMITTED)
      const rej1 = await callMutation(page, 'ErpAstValueAdjustment', 'approve', { id: adj.id }, 'id');
      expect(rej1.errors, 'approve from UNSUBMITTED should be rejected').toBeTruthy();

      // Forward to APPROVED for the second guard
      await callMutationOk(page, 'ErpAstValueAdjustment', 'submitForApproval', { id: adj.id }, 'id');
      await callMutationOk(page, 'ErpAstValueAdjustment', 'approve', { id: adj.id }, 'id');
      const s = await verifyState(page, 'ErpAstValueAdjustment', adj.id, 'approveStatus');
      expect(s.approveStatus, 'pre: approveStatus=APPROVED').toBe('APPROVED');

      // APPROVED → submitForApproval: rejected (validateTransitionForSubmit requires UNSUBMITTED or REJECTED)
      const rej2 = await callMutation(page, 'ErpAstValueAdjustment', 'submitForApproval', { id: adj.id }, 'id');
      expect(rej2.errors, 'submitForApproval from APPROVED should be rejected').toBeTruthy();
    } finally {
      await cleanupAdjustmentChain(page, adj.code, adj.id, asset.id);
    }
  });
});

// ============================================================
// Phase 2 — 反向红冲 + cancel + 净值回退
// ============================================================

test.describe('assets ErpAstValueAdjustment Phase 2 — reverseApprove + cancel + withdrawApproval', () => {
  test('reverseApprove (IMPAIRMENT): APPROVED→REJECTED + posted=false + red-letter voucher (Dr 6702=-amt/Cr 1604=-amt) + netBookValue rollback + original isReversed', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstValueAdjustment-main');

    const asset = await createTestAsset(page, 'REV');
    const amount = 3000;
    const adj = await createTestAdjustment(page, 'REV', asset.id, 'IMPAIRMENT', amount);

    try {
      // Forward chain to APPROVED + posted
      await callMutationOk(page, 'ErpAstValueAdjustment', 'submitForApproval', { id: adj.id }, 'id');
      await callMutationOk(page, 'ErpAstValueAdjustment', 'approve', { id: adj.id }, 'id posted');
      let s = await verifyState(page, 'ErpAstValueAdjustment', adj.id, 'approveStatus posted');
      expect(s.approveStatus, 'pre: APPROVED').toBe('APPROVED');
      expect(s.posted, 'pre: posted=true').toBe(true);

      // Pre: netBookValue decreased to 9000
      const assetBefore = await verifyState(page, 'ErpAstAsset', asset.id, 'netBookValue');
      expect(Number(assetBefore.netBookValue), 'pre: netBookValue=9000 after IMPAIRMENT approve').toBe(9000);

      // reverseApprove: APPROVED → REJECTED + posted=false + docStatus stays ACTIVE
      await callMutationOk(page, 'ErpAstValueAdjustment', 'reverseApprove', { id: adj.id }, 'id');
      s = await verifyState(page, 'ErpAstValueAdjustment', adj.id, 'approveStatus docStatus posted');
      expect(s.approveStatus, 'after reverseApprove approveStatus=REJECTED').toBe('REJECTED');
      expect(s.docStatus, 'after reverseApprove docStatus stays ACTIVE (not CANCELLED)').toBe('ACTIVE');
      expect(s.posted, 'after reverseApprove posted=false').toBe(false);

      // 红冲凭证行同向取负断言：REVERSAL Dr 6702=-3000 / Cr 1604=-3000
      const reversalVid = await findVoucherIdByBillCode(page, adj.code, 'REVERSAL');
      expect(reversalVid, 'VALUE_ADJUSTMENT REVERSAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, reversalVid, [
        { subjectCode: '6702', dcDirection: 'DEBIT', debitAmount: -amount, creditAmount: 0 },
        { subjectCode: '1604', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -amount },
      ]);

      // 原正向凭证 isReversed=true（凭证级回退标记）
      const originalVid = await findVoucherIdByBillCode(page, adj.code, 'NORMAL');
      expect(originalVid, 'original NORMAL voucher should exist').toBeTruthy();
      const origVoucher = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', Number(originalVid)), 'id postingType isReversed',
      );
      expect(origVoucher?.postingType, 'original voucher postingType=NORMAL').toBe('NORMAL');
      expect(origVoucher?.isReversed, 'original voucher isReversed=true after reverseApprove').toBe(true);

      // 净值回退断言：reverseApprove 后 netBookValue 恢复至调整前值（9000 + 3000 = 12000）
      const assetAfter = await verifyState(page, 'ErpAstAsset', asset.id, 'netBookValue currentValue');
      expect(Number(assetAfter.netBookValue), 'reverseApprove should rollback netBookValue to 12000').toBe(12000);
      expect(Number(assetAfter.currentValue), 'currentValue mirrors netBookValue rollback').toBe(12000);
    } finally {
      await cleanupAdjustmentChain(page, adj.code, adj.id, asset.id);
    }
  });

  test('cancel + withdrawApproval: DRAFT→cancel(CANCELLED, approveStatus unchanged) + SUBMITTED→withdrawApproval(UNSUBMITTED) + CANCELLED→approve guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstValueAdjustment-main');

    // ---- cancel path: DRAFT/UNSUBMITTED → cancel → docStatus=CANCELLED + approveStatus unchanged ----
    const asset1 = await createTestAsset(page, 'CNL');
    const adj1 = await createTestAdjustment(page, 'CNL', asset1.id, 'IMPAIRMENT', 1000);

    try {
      // Pre: docStatus=DRAFT, approveStatus=UNSUBMITTED
      let s1 = await verifyState(page, 'ErpAstValueAdjustment', adj1.id, 'approveStatus docStatus');
      expect(s1.approveStatus, 'pre: UNSUBMITTED').toBe('UNSUBMITTED');
      expect(s1.docStatus, 'pre: DRAFT').toBe('DRAFT');

      // cancel: DRAFT → CANCELLED, approveStatus unchanged (UNSUBMITTED)
      await callMutationOk(page, 'ErpAstValueAdjustment', 'cancel', { id: adj1.id }, 'id');
      s1 = await verifyState(page, 'ErpAstValueAdjustment', adj1.id, 'approveStatus docStatus');
      expect(s1.docStatus, 'after cancel docStatus=CANCELLED').toBe('CANCELLED');
      expect(s1.approveStatus, 'cancel should not change approveStatus (stays UNSUBMITTED)').toBe('UNSUBMITTED');

      // 非法守卫：CANCELLED → approve rejected (validateNotCancelled throws)
      const rej = await callMutation(page, 'ErpAstValueAdjustment', 'approve', { id: adj1.id }, 'id');
      expect(rej.errors, 'approve from CANCELLED should be rejected').toBeTruthy();
    } finally {
      await cleanupAdjustmentChain(page, adj1.code, adj1.id, asset1.id);
    }

    // ---- withdrawApproval path: UNSUBMITTED→submitForApproval(SUBMITTED)→withdrawApproval(UNSUBMITTED) ----
    const asset2 = await createTestAsset(page, 'WD');
    const adj2 = await createTestAdjustment(page, 'WD', asset2.id, 'IMPAIRMENT', 1000);

    try {
      await callMutationOk(page, 'ErpAstValueAdjustment', 'submitForApproval', { id: adj2.id }, 'id');
      let s2 = await verifyState(page, 'ErpAstValueAdjustment', adj2.id, 'approveStatus');
      expect(s2.approveStatus, 'after submit approveStatus=SUBMITTED').toBe('SUBMITTED');

      // withdrawApproval: SUBMITTED → UNSUBMITTED
      await callMutationOk(page, 'ErpAstValueAdjustment', 'withdrawApproval', { id: adj2.id }, 'id');
      s2 = await verifyState(page, 'ErpAstValueAdjustment', adj2.id, 'approveStatus');
      expect(s2.approveStatus, 'after withdrawApproval approveStatus=UNSUBMITTED').toBe('UNSUBMITTED');
    } finally {
      await cleanupAdjustmentChain(page, adj2.code, adj2.id, asset2.id);
    }
  });
});
