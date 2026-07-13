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
import { cleanupVoucherByBillCode } from '../orchestration/_helper';

/**
 * assets ErpAstDepreciationSchedule 折旧引擎业务动作浏览器层 E2E（plan 2026-07-14-0215-1 Phase 1）。
 *
 * 验证折旧引擎 @BizMutation（executeDepreciation / reverseDepreciation）经 GraphQL /graphql 的全栈可达性 +
 * 状态机迁移 + DEPRECIATION 业财过账触发可观测性（posted 标志翻转）。
 *
 * 权威状态机（ErpAstDepreciationScheduleProcessor）：
 *   executeDepreciation(assetId, period) → 计算本期折旧 + 建/更新 schedule(status=EXECUTED) +
 *     资产卡片 accumulatedDepreciation/netBookValue 回写 + DEPRECIATION(70) 过账（成功 posted=true）。
 *   reverseDepreciation(assetId, period) → schedule status EXECUTED→REVERSED + 红冲凭证 + posted=false。
 *
 * 期间控制：period须经 ErpFinAccountingPeriod(code=period, status=OPEN)（requirePeriodOpen）。
 * 种子 period id=1 code=2026-07 status=OPEN ✓。
 *
 * 自包含隔离：自包含建 ErpAstAsset（IN_SERVICE + STRAIGHT_LINE + 原值/残值/年限），唯一 code
 * `E2E-AST-DEP-<ts>`，避开种子资产（id=2 已有 2026-07 schedule）。折旧数学（STRAIGHT_LINE）：
 *   amount = (originalValue - residualValue) / usefulLifeMonths = (12000 - 0) / 36 = 333.3333
 *
 * 过账科目依赖（Phase 1 Decision 裁定）：DepreciationAcctDocProvider 默认科目码 6602（折旧费用 Dr）/
 * 1602（累计折旧 Cr）。种子 erp_md_subject.csv 原 缺 6602/1602 → 补齐 5 行（1601/1602/1603/6301/6602，
 * 0215-1）后过账 happy-path 可达（posted=true），解除优雅降级。
 *
 * 种子引用：org id=2 / category id=1（AST-CAT-IT，STRAIGHT_LINE 36 月）/ currency id=1（CNY）/
 * period 2026-07（OPEN）/ acctSchema ACCT-FIN-01 id=1。
 */
const ORG_ID = 2;
const CATEGORY_ID = 1;
const CURRENCY_ID = 1;
const PERIOD = '2026-07';

test.describe('assets ErpAstDepreciationSchedule depreciation engine lifecycle', () => {
  test('executeDepreciation → schedule EXECUTED + posted=true + asset rollup → reverseDepreciation → REVERSED', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstAsset-main');

    const code = `E2E-AST-DEP-${Date.now()}`;
    const asset = await createViaSave(
      page, 'ErpAstAsset',
      {
        code,
        name: `E2E 折旧资产 ${code}`,
        orgId: ORG_ID,
        categoryId: CATEGORY_ID,
        acquisitionDate: '2026-06-15',
        currencyId: CURRENCY_ID,
        originalValue: 12000,
        residualValue: 0,
        depreciationMethod: 'STRAIGHT_LINE',
        usefulLifeMonths: 36,
        status: 'IN_SERVICE',
        accumulatedDepreciation: 0,
        netBookValue: 12000,
      },
      'id code accumulatedDepreciation netBookValue',
    );
    expect(asset.id, '__save should create an IN_SERVICE asset').toBeTruthy();

    try {
      // executeDepreciation: 建 schedule(status=EXECUTED) + 资产卡片汇总回写 + DEPRECIATION 过账
      const executed = await callMutationOk(
        page, 'ErpAstDepreciationSchedule', 'executeDepreciation',
        { assetId: asset.id, period: PERIOD },
        'id status actualAmount accumulatedDepreciation netBookValue posted voucherId',
      );
      expect(executed.status, 'executeDepreciation should set schedule status=EXECUTED').toBe('EXECUTED');
      expect(Number(executed.actualAmount), 'STRAIGHT_LINE amount=(12000-0)/36=333.3333').toBeCloseTo(333.3333, 3);
      expect(Number(executed.accumulatedDepreciation), 'accumulated=actualAmount').toBeCloseTo(333.3333, 3);
      // posted=true（DEPRECIATION 过账成功，科目 6602/1602 已补齐种子）
      expect(executed.posted, 'DEPRECIATION posting should succeed → posted=true').toBe(true);
      expect(executed.voucherId, 'posted voucherId should be non-null').toBeTruthy();

      // __get 权威确认 schedule 终态
      const verifiedSchedule = await verifyState(
        page, 'ErpAstDepreciationSchedule', executed.id, 'status posted voucherId',
      );
      expect(verifiedSchedule.status, '__get should confirm EXECUTED').toBe('EXECUTED');
      expect(verifiedSchedule.posted, '__get should confirm posted=true').toBe(true);

      // 资产卡片汇总列回写断言（accumulatedDepreciation/netBookValue 经 __get 独立核实）
      const verifiedAsset = await verifyState(
        page, 'ErpAstAsset', asset.id, 'accumulatedDepreciation netBookValue',
      );
      expect(Number(verifiedAsset.accumulatedDepreciation), 'asset rollup accumulatedDepreciation').toBeCloseTo(333.3333, 3);
      expect(Number(verifiedAsset.netBookValue), 'asset rollup netBookValue=12000-333.3333').toBeCloseTo(11666.6667, 3);

      // reverseDepreciation: EXECUTED → REVERSED + 红冲凭证 + posted=false
      const reversed = await callMutationOk(
        page, 'ErpAstDepreciationSchedule', 'reverseDepreciation',
        { assetId: asset.id, period: PERIOD },
        'id status posted voucherId',
      );
      expect(reversed.status, 'reverseDepreciation should set schedule status=REVERSED').toBe('REVERSED');
      expect(reversed.posted, 'reverse should clear posted=false').toBe(false);

      const verifiedReversed = await verifyState(
        page, 'ErpAstDepreciationSchedule', executed.id, 'status posted',
      );
      expect(verifiedReversed.status, '__get should confirm REVERSED').toBe('REVERSED');
      expect(verifiedReversed.posted, '__get should confirm posted=false after reverse').toBe(false);

      // 资产卡片汇总列回退（accumulatedDepreciation 减回 / netBookValue 加回）
      const assetAfterReverse = await verifyState(
        page, 'ErpAstAsset', asset.id, 'accumulatedDepreciation netBookValue',
      );
      expect(Number(assetAfterReverse.accumulatedDepreciation), 'reverse should roll back accumulated').toBeCloseTo(0, 3);
      expect(Number(assetAfterReverse.netBookValue), 'reverse should restore netBookValue=12000').toBeCloseTo(12000, 3);
    } finally {
      // 清理凭证（billHeadCode = assetCode#period）
      await cleanupVoucherByBillCode(page, `${code}#${PERIOD}`);
      // 清理折旧 schedule（按 assetId）
      await deleteByFilter(page, 'ErpAstDepreciationSchedule', eqFilter('assetId', asset.id));
      await deleteById(page, 'ErpAstAsset', asset.id);
    }
  });

  test('illegal-transition guard: reverseDepreciation on non-EXECUTED schedule rejected + non-existent asset guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstAsset-main');

    const code = `E2E-AST-DEP-NEG-${Date.now()}`;
    const asset = await createViaSave(
      page, 'ErpAstAsset',
      {
        code,
        name: `E2E 折旧负路径资产 ${code}`,
        orgId: ORG_ID,
        categoryId: CATEGORY_ID,
        acquisitionDate: '2026-06-15',
        currencyId: CURRENCY_ID,
        originalValue: 12000,
        residualValue: 0,
        depreciationMethod: 'STRAIGHT_LINE',
        usefulLifeMonths: 36,
        status: 'IN_SERVICE',
        accumulatedDepreciation: 0,
        netBookValue: 12000,
      },
      'id',
    );

    try {
      // reverseDepreciation 在无 schedule（未折旧）时拒绝：ERR_SCHEDULE_ILLEGAL_STATUS_TRANSITION
      const rej = await callMutation(
        page, 'ErpAstDepreciationSchedule', 'reverseDepreciation',
        { assetId: asset.id, period: PERIOD }, 'id',
      );
      expect(rej.errors, 'reverseDepreciation without prior execute should be rejected').toBeTruthy();

      // executeDepreciation 不存在资产拒绝：ERR_ASSET_NOT_FOUND
      const rej2 = await callMutation(
        page, 'ErpAstDepreciationSchedule', 'executeDepreciation',
        { assetId: 99999999, period: PERIOD }, 'id',
      );
      expect(rej2.errors, 'executeDepreciation on non-existent asset should be rejected').toBeTruthy();
    } finally {
      await deleteById(page, 'ErpAstAsset', asset.id);
    }
  });
});
