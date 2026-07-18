import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  findItems,
  findVoucherIdByBillCode,
  assertVoucherLines,
  eqFilter,
  deleteById,
  runMfgChain,
  cleanupMfg,
  SEED,
} from '../orchestration/_helper';

/**
 * manufacturing 生产差异重算孤儿凭证修复浏览器层 E2E（plan 2026-07-18-2251-1 Phase 3）。
 *
 * 验证 config `erp-mfg.variance-auto-calc-enabled=true` 启用后，手动 `ErpMfgCostVariance__calculateVariances`
 * 二次调用经 `ProductionVarianceDispatcher.reverseIfExists` 红冲既有 PRODUCTION_VARIANCE 凭证 + deleteByWorkOrder
 * 删旧行 + calculateVariances 重算 + dispatchIfApplicable 派发新凭证 的全栈可达性：
 *   (1) 首次 calculateVariances 产 NORMAL PRODUCTION_VARIANCE 凭证 Dr/Cr 6 差异科目 + 数据行 posted=true
 *   (2) 再次 calculateVariances → reverseIfExists 红冲原凭证（原 isReversed=true）+ 生成 REVERSAL 红字凭证
 *       + 新 NORMAL 凭证派发 + 数据行全 posted=true
 *
 * 权威实现（ErpMfgCostVarianceBizModel.calculateVariances）：
 *   requireWorkOrder → guard COMPLETED → productionVarianceDispatcher.reverseIfExists（红冲）
 *   → deleteByWorkOrder（删旧行）→ calculateVariances（重算）→ dispatchIfApplicable（派发新凭证）
 *
 * 红冲凭证（IErpFinVoucherBiz.reverse → ErpFinPostingProcessor.reverseProcess）：
 *   原凭证 isReversed=true + 新建红字凭证 postingType=REVERSAL + 行同向取负（Dr 1410=-X / Cr 1411=-X）
 *   + 红字凭证与原凭证共用 billHeadCode（voucher_bill_r 回链反查区分 postingType）。
 *
 * 自包含隔离：复用 1800-2 既有 `runMfgChain` + variance setup（测试专用成品物料 + 运行时 FIRMED rollup）。
 *
 * 确定性成本裁决（标准 vs 实际成本偏差派生期望值，复用 mfg-variance.spec.ts 同型裁决）：
 *   标准成本 FIRMED rollup：materialCost=90/unit, laborCost=5/unit, overheadCost=5/unit, unitCost=100
 *   实际成本（runMfgChain）：materialCost=1000（20×50）, laborCost=100（60min/60×100）, overheadCost=0
 *   completedQty=10, plannedQty=10
 *
 *   要素净差异汇总（ProductionVarianceDispatcher）：
 *     MATERIAL = +100 → DEBIT（unfavorable）→ amount=100
 *     LABOR = +50 → DEBIT（unfavorable）→ amount=50
 *     OVERHEAD = -50 → CREDIT（favorable）→ amount=50
 *
 *   PRODUCTION_VARIANCE 凭证行（ProductionVarianceAcctDocProvider 科目码）：
 *     NORMAL：Dr 1410 100 / Cr 1411 100 / Dr 1412 50 / Cr 1413 50 / Dr 1415 50 / Cr 1414 50
 *     REVERSAL 红字（同向取负）：Dr 1410=-100 / Cr 1411=-100 / Dr 1412=-50 / Cr 1413=-50 / Dr 1415=-50 / Cr 1414=-50
 *
 * 清理：cleanupMfg 扩展已覆盖 ErpMfgCostVariance + PRODUCTION_VARIANCE 凭证（原+REVERSAL+新 NORMAL 三张共用
 *   billHeadCode=woCode+'-PV'，cleanupVoucherByBillCode 全量清理）；FIRMED rollup/Line + 测试专用物料由本 spec 清理。
 */
test.describe('manufacturing production variance recompute reversal browser-layer E2E', () => {
  test('calculateVariances twice reverses original voucher + posts new voucher + data lines posted=true', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgWorkOrder-main');

    const ts = Date.now();

    // (1) 测试专用成品物料（FINISHED_PRODUCT, STANDARD costing，非 MAT-001）
    const product = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code: `E2E-MFG-PVRC-FP-${ts}`, name: 'E2E 测试重算红冲成品',
        materialType: 'FINISHED_PRODUCT', uoMId: SEED.UOM, status: 'ACTIVE',
        costMethod: 'STANDARD', defaultWarehouseId: SEED.WH_RAW,
      },
      'id',
    );

    // (2) FIRMED ErpMfgCostRollup + Line（标准成本与实际成本有偏差，复用 1800-2 setup）
    const rollup = await createViaSave(
      page, 'ErpMfgCostRollup',
      {
        code: `E2E-MFG-PVRC-RU-${ts}`, orgId: SEED.ORG,
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

    // (3) runMfgChain 变体：使用测试专用成品物料（与 MAT-001 链路隔离），config 已开启 auto-calc
    const r = await runMfgChain(page, { productId: Number(product.id), productUoMId: SEED.UOM });
    r.productMat = product;

    try {
      // (4) 前置：runMfgChain 满量完工已触发首次 calculateVariances（config 开）
      //     → 5 类差异行 + NORMAL PRODUCTION_VARIANCE 凭证 + 全行 posted=true
      const woCode = r.codes.wo;
      const billHeadCode = woCode + '-PV';

      const firstNormalVid = await findVoucherIdByBillCode(page, billHeadCode, 'NORMAL');
      expect(firstNormalVid, '前置：首次计算应派发 NORMAL PRODUCTION_VARIANCE 凭证').toBeTruthy();

      const firstVarianceLines = await findItems(
        page, 'ErpMfgCostVariance',
        eqFilter('workOrderId', Number(r.wo.id)),
        'varianceType costElement varianceAmount posted',
      );
      expect(firstVarianceLines.length, '前置：5 类差异行').toBe(5);
      const firstPostedCount = firstVarianceLines.filter((l: any) => l.posted === true).length;
      expect(firstPostedCount, '前置：全行 posted=true').toBe(5);

      // (5) 关键驱动：再次 calculateVariances（手动入口） → reverseIfExists 红冲原凭证 + 重算 + 派发新凭证
      await callMutationOk(
        page, 'ErpMfgCostVariance', 'calculateVariances',
        { workOrderId: Number(r.wo.id) },
        'id',
      );

      // (6) 原 NORMAL 凭证已被标记 isReversed=true
      const firstNormalAfter = await findItems(
        page, 'ErpFinVoucher',
        eqFilter('id', Number(firstNormalVid)),
        'id postingType isReversed',
      );
      expect(firstNormalAfter.length, '原 NORMAL 凭证应可反查').toBe(1);
      expect(firstNormalAfter[0].isReversed, '原 NORMAL 凭证应被标记 isReversed=true').toBe(true);

      // (7) REVERSAL 红字凭证存在 + 行同向取负（与原 NORMAL 凭证金额互为相反数）
      const reversalVid = await findVoucherIdByBillCode(page, billHeadCode, 'REVERSAL');
      expect(reversalVid, '重算应生成 REVERSAL 红字凭证').toBeTruthy();
      await assertVoucherLines(page, reversalVid, [
        // 红字凭证行：dcDirection 不变，金额取负
        { subjectCode: '1410', dcDirection: 'DEBIT', debitAmount: -100, creditAmount: 0 },
        { subjectCode: '1411', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -100 },
        { subjectCode: '1412', dcDirection: 'DEBIT', debitAmount: -50, creditAmount: 0 },
        { subjectCode: '1413', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -50 },
        { subjectCode: '1415', dcDirection: 'DEBIT', debitAmount: -50, creditAmount: 0 },
        { subjectCode: '1414', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -50 },
      ]);

      // (8) 新 NORMAL 凭证存在（重算后 dispatchIfApplicable 重新派发）+ isReversed=false
      //     反查 ErpFinVoucherBillR(billCode) → 过滤 postingType=NORMAL + isReversed=false 应仅 1 条
      const allBillRs = await findItems(
        page, 'ErpFinVoucherBillR',
        eqFilter('billCode', billHeadCode),
        'voucherId',
      );
      const activeNormalVouchers: any[] = [];
      for (const lnk of allBillRs) {
        const v = await findItems(
          page, 'ErpFinVoucher',
          eqFilter('id', Number(lnk.voucherId)),
          'id postingType isReversed',
        );
        if (v.length === 1 && v[0].postingType === 'NORMAL' && v[0].isReversed === false) {
          activeNormalVouchers.push(v[0]);
        }
      }
      expect(activeNormalVouchers.length, '重算后应仅 1 条 isReversed=false NORMAL 凭证（无孤儿）').toBe(1);

      // (9) 数据行全 posted=true（重算后 dispatchIfApplicable 成功 markPosted）
      const afterVarianceLines = await findItems(
        page, 'ErpMfgCostVariance',
        eqFilter('workOrderId', Number(r.wo.id)),
        'varianceType posted',
      );
      expect(afterVarianceLines.length, '重算后仍 5 类差异行（deleteByWorkOrder 删旧+重算）').toBe(5);
      const afterPostedCount = afterVarianceLines.filter((l: any) => l.posted === true).length;
      expect(afterPostedCount, '重算后全行 posted=true（dispatchIfApplicable 重新派发成功）').toBe(5);
    } finally {
      // cleanupMfg 已扩展覆盖 ErpMfgCostVariance + PRODUCTION_VARIANCE 凭证（原+REVERSAL+新 NORMAL 共用 billHeadCode）
      await cleanupMfg(page, r);
      // 本 spec 专属清理：FIRMED rollup/Line + 测试专用成品物料
      await deleteById(page, 'ErpMfgCostRollupLine', rollupLine.id);
      await deleteById(page, 'ErpMfgCostRollup', rollup.id);
      await deleteById(page, 'ErpMdMaterial', product.id);
    }
  });
});
