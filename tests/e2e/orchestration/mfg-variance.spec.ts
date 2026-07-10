import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  verifyState,
  findItems,
  findVoucherIdByBillCode,
  assertVoucherLines,
  eqFilter,
  deleteById,
  runMfgChain,
  cleanupMfg,
  SEED,
} from './_helper';

/**
 * 生产差异计算浏览器层 E2E（plan 2026-07-10-1800-2 Phase 3）。
 *
 * 验证 config `erp-mfg.variance-auto-calc-enabled=true` 启用后，WorkOrder 满量完工（willFinish=true）
 * 触发 ProductionVarianceCalculator.calculateVariances + ProductionVarianceDispatcher.dispatchIfApplicable
 * 的浏览器层全栈可达性：
 *   (1) ErpMfgCostVariance 记录非空（5 类差异行：材料用量/人工效率/人工费率/制造费用/产量）
 *   (2) PRODUCTION_VARIANCE 凭证行可断言（科目码 + 借贷方向 + 金额由标准 vs 实际成本差额派生）
 *
 * 自包含隔离（plan Phase 3 Decision）：使用**测试专用成品物料**（非 MAT-001）+ 运行时 FIRMED
 * ErpMfgCostRollup/Line。MAT-001 链路（mfg-chain.spec.ts）无 FIRMED rollup → calculateVariances
 * 抛 ERR_VARIANCE_NO_STANDARD_COST → 被 Processor try/catch 吞 → 仅记 ERROR 日志不阻断完工。
 *
 * 确定性成本裁决（标准 vs 实际成本偏差派生期望值）：
 *   标准成本（FIRMED rollup）：materialCost=90/unit, laborCost=5/unit, overheadCost=5/unit, unitCost=100
 *   实际成本（runMfgChain）：materialCost=1000（20×50）, laborCost=100（60min/60×100）, overheadCost=0
 *   completedQty=10, plannedQty=10
 *
 *   差异计算（ProductionVarianceCalculator）：
 *     MATERIAL_USAGE: std=90×10=900, act=1000, variance=+100（unfavorable, MATERIAL 要素）
 *     LABOR_EFFICIENCY: std=5×10=50, act=0（stdHourlyRate=0 无 BOM 工艺）, variance=-50（LABOR 要素）
 *     LABOR_RATE: std=0, act=100, variance=+100（LABOR 要素）
 *     OVERHEAD: std=5×10=50, act=0, variance=-50（OVERHEAD 要素）
 *     VOLUME: std=10×100=1000, act=10×100=1000, variance=0（MATERIAL 要素，完工=计划）
 *
 *   要素净差异汇总（ProductionVarianceDispatcher）：
 *     MATERIAL = MATERIAL_USAGE(+100) + VOLUME(0) = +100 → DEBIT（unfavorable）→ amount=100
 *     LABOR = LABOR_EFFICIENCY(-50) + LABOR_RATE(+100) = +50 → DEBIT（unfavorable）→ amount=50
 *     OVERHEAD = OVERHEAD(-50) = -50 → CREDIT（favorable）→ amount=50
 *
 *   PRODUCTION_VARIANCE 凭证行（ProductionVarianceAcctDocProvider 科目码）：
 *     Material DEBIT(unfavorable): Dr 1410 100 / Cr 1411 100
 *     Labor DEBIT(unfavorable):    Dr 1412 50  / Cr 1413 50
 *     Overhead CREDIT(favorable):  Dr 1415 50  / Cr 1414 50
 *
 * 清理：差异 spec 自清理全部产物（ErpMfgCostVariance + PRODUCTION_VARIANCE 凭证由 cleanupMfg 扩展覆盖；
 *   FIRMED rollup/Line + 测试专用物料由本 spec 清理）。
 */
test.describe('manufacturing production variance browser-layer E2E (config-gated auto-calc on full completion)', () => {
  test('full completion triggers variance calculation + PRODUCTION_VARIANCE voucher posting', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgWorkOrder-main');

    const ts = Date.now();

    // (1) 测试专用成品物料（FINISHED_PRODUCT, STANDARD costing，非 MAT-001）
    const product = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code: `E2E-MFG-PV-FP-${ts}`, name: 'E2E 测试差异成品',
        materialType: 'FINISHED_PRODUCT', uoMId: SEED.UOM, status: 'ACTIVE',
        costMethod: 'STANDARD', defaultWarehouseId: SEED.WH_RAW,
      },
      'id',
    );

    // (2) FIRMED ErpMfgCostRollup + Line（标准成本与实际成本有偏差）
    const rollup = await createViaSave(
      page, 'ErpMfgCostRollup',
      {
        code: `E2E-MFG-PV-RU-${ts}`, orgId: SEED.ORG,
        businessDate: '2026-07-10', status: 'FIRMED',
      },
      'id',
    );
    const rollupLine = await createViaSave(
      page, 'ErpMfgCostRollupLine',
      {
        costRollupId: rollup.id, lineNo: 1,
        materialId: product.id, uoMId: SEED.UOM,
        materialCost: 90, laborCost: 5, overheadCost: 5, subcontractCost: 0,
        totalCost: 100, unitCost: 100,
      },
      'id',
    );

    // (3) runMfgChain 变体：使用测试专用成品物料（与 MAT-001 链路隔离）
    const r = await runMfgChain(page, { productId: Number(product.id), productUoMId: SEED.UOM });
    r.productMat = product;

    try {
      // (4) verifyState 断言 docStatus=COMPLETED（满量完工）
      const woState = await verifyState(page, 'ErpMfgWorkOrder', r.wo.id, 'docStatus completedQuantity');
      expect(woState?.docStatus, 'full completion → COMPLETED').toBe('COMPLETED');
      expect(Number(woState?.completedQuantity ?? 0), 'completedQuantity=10').toBe(10);

      // (5) ErpMfgCostVariance 记录非空 + varianceType 存在（5 类差异行）
      const varianceLines = await findItems(
        page, 'ErpMfgCostVariance',
        eqFilter('workOrderId', Number(r.wo.id)),
        'varianceType costElement varianceAmount posted',
      );
      expect(varianceLines.length, 'ErpMfgCostVariance should have 5 lines (material/efficiency/rate/overhead/volume)').toBe(5);
      const varianceTypes = varianceLines.map((l: any) => l.varianceType);
      expect(varianceTypes, 'should contain MATERIAL_USAGE').toContain('MATERIAL_USAGE');
      expect(varianceTypes, 'should contain VOLUME').toContain('VOLUME');

      // (6) PRODUCTION_VARIANCE 凭证行断言（billHeadCode = woCode + '-PV'）
      const woCode = r.codes.wo;
      const pvVoucherId = await findVoucherIdByBillCode(page, woCode + '-PV');
      expect(pvVoucherId, 'PRODUCTION_VARIANCE voucher should exist').toBeTruthy();
      await assertVoucherLines(page, pvVoucherId, [
        // Material unfavorable (DEBIT): Dr 1410 / Cr 1411
        { subjectCode: '1410', dcDirection: 'DEBIT', debitAmount: 100, creditAmount: 0 },
        { subjectCode: '1411', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: 100 },
        // Labor unfavorable (DEBIT): Dr 1412 / Cr 1413
        { subjectCode: '1412', dcDirection: 'DEBIT', debitAmount: 50, creditAmount: 0 },
        { subjectCode: '1413', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: 50 },
        // Overhead favorable (CREDIT): Dr 1415 / Cr 1414
        { subjectCode: '1415', dcDirection: 'DEBIT', debitAmount: 50, creditAmount: 0 },
        { subjectCode: '1414', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: 50 },
      ]);

      // 差异行 posted=true（dispatchIfApplicable 成功过账后回写）
      const postedLines = varianceLines.filter((l: any) => l.posted === true);
      expect(postedLines.length, 'all variance lines should be posted=true after successful dispatch').toBe(5);
    } finally {
      // cleanupMfg 扩展覆盖：ErpMfgCostVariance + PRODUCTION_VARIANCE 凭证 + 完工移动 + 链路
      await cleanupMfg(page, r);
      // 本 spec 专属清理：FIRMED rollup/Line + 测试专用成品物料
      await deleteById(page, 'ErpMfgCostRollupLine', rollupLine.id);
      await deleteById(page, 'ErpMfgCostRollup', rollup.id);
      await deleteById(page, 'ErpMdMaterial', product.id);
    }
  });
});

