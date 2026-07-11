import { test, expect, loginAndNavigate, createViaSave, callMutationOk, verifyState, deleteById } from './_helper';
import {
  findFirst,
  eqFilter,
  SEED,
  deleteByFilter,
} from '../orchestration/_helper';

/**
 * quality ErpQaNonConformance（NCR）RETURN 处置跨域退货编排浏览器层 E2E
 * （plan 2026-07-11-0730-2 Phase 2）。
 *
 * 验证 NCR resolve（RETURN 处置 + supplierId 非空）经 NcrReturnOrchestrator.orchestrateReturn
 * 创建采购退货单（ErpPurReturn）并登记 NCR.returnCode：
 *   - 成功模式：resolve RETURN → NCR.returnCode = `PR-FROM-NCR-{ncrId}` + ErpPurReturn 存在且
 *     supplierId 匹配 + docStatus=DRAFT + approveStatus=UNSUBMITTED
 *   - 失败模式（对照）：resolve CONCESSION → 无退货副作用，returnCode 保持 null
 *
 * 编排路径（ErpQaNonConformanceBizModel.resolve:84 → dispatchFinancialImpact:205 →
 * NcrReturnOrchestrator.orchestrateReturn:70 → createPurchaseReturn:82 → IErpPurReturnBiz.save）：
 *   resolve(RETURN) → orchestrateReturn 按 supplierId 非空判定采购退货域
 *   → resolveWarehouseId/resolveCurrencyId（按 materialId 查 ErpInvStockBalance limit 1）
 *   → purReturnBiz.save(code=PR-FROM-NCR-{ncrId}, supplierId, warehouseId, currencyId, DRAFT, UNSUBMITTED)
 *   → ncr.setReturnCode(returnCode)。
 *
 * CAPA 门控（NcrLifecycleService.requireResolveGate）：NCR 无关联 CAPA 时 resolve 直接放行
 * （0335-2 已验证无 CAPA 路径）。本 spec 不创建 CAPA 以简化 setup（对齐 0335-2 无 CAPA 范式）。
 *
 * 种子引用（init-data）：materialId=1（MAT-001，种子余额 warehouseId=1/currencyId=1）；
 * supplierId=3（SUP-001 北方钢铁供应商，P2P 链路 1234-1 落地）。
 *
 * 清理：ErpPurReturn 退货单（NCR→退货单创建编排的产物，退货单后续生命周期归退货域自身 E2E）
 * + NCR 本体。退货单无行（createPurchaseReturn 仅建头），逻辑删除头即可。
 */

const MATERIAL_ID = SEED.MAT_1; // 1 — MAT-001，种子余额 warehouseId=1/currencyId=1
const SUPPLIER_ID = SEED.SUPPLIER; // 3 — SUP-001 北方钢铁供应商

test.describe('quality ErpQaNonConformance RETURN disposition cross-domain return orchestration', () => {
  test('resolve RETURN(supplier) → create ErpPurReturn + register returnCode', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaNonConformance-main');

    const ncr = await createViaSave(
      page, 'ErpQaNonConformance',
      {
        code: `E2E-NCR-RET-${Date.now()}`,
        ncrDate: '2026-07-11',
        materialId: MATERIAL_ID,
        supplierId: SUPPLIER_ID,
        severity: 'NORMAL',
        dispositionType: 'RETURN',
        status: 'OPEN',
        quantity: 1,
        description: 'E2E NCR return disposition',
      },
      'id status code returnCode',
    );
    expect(ncr.status, 'new ncr status=OPEN').toBe('OPEN');
    expect(ncr.returnCode, 'new ncr returnCode=null').toBeNull();

    try {
      await callMutationOk(
        page, 'ErpQaNonConformance', 'submitReview', { ncrId: ncr.id }, 'id status',
      );

      const resolved = await callMutationOk(
        page, 'ErpQaNonConformance', 'resolve',
        { ncrId: ncr.id, resolution: 'RETURN to supplier' }, 'id status returnCode',
      );
      expect(resolved.status, 'resolve should transition IN_REVIEW → RESOLVED').toBe('RESOLVED');
      expect(resolved.returnCode, 'resolve RETURN should set returnCode').toBeTruthy();
      expect(resolved.returnCode, 'returnCode should follow PR-FROM-NCR-{ncrId} format')
        .toBe(`PR-FROM-NCR-${ncr.id}`);

      const ncrFinal = await verifyState(page, 'ErpQaNonConformance', ncr.id, 'status returnCode');
      expect(ncrFinal.status, '__get should confirm RESOLVED').toBe('RESOLVED');
      expect(ncrFinal.returnCode, '__get should confirm returnCode registered').toBe(resolved.returnCode);

      const purReturn = await findFirst<any>(
        page, 'ErpPurReturn', eqFilter('code', resolved.returnCode),
        'id code supplierId docStatus approveStatus',
      );
      expect(purReturn, 'ErpPurReturn should exist for returnCode').toBeTruthy();
      expect(Number(purReturn.supplierId), 'ErpPurReturn supplierId should match NCR').toBe(SUPPLIER_ID);
      expect(purReturn.docStatus, 'ErpPurReturn docStatus=DRAFT').toBe('DRAFT');
      expect(purReturn.approveStatus, 'ErpPurReturn approveStatus=UNSUBMITTED').toBe('UNSUBMITTED');

      if (purReturn) await deleteById(page, 'ErpPurReturn', purReturn.id);
    } finally {
      await deleteById(page, 'ErpQaNonConformance', ncr.id);
    }
  });

  test('resolve CONCESSION → no return side-effect (returnCode stays null)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpQaNonConformance-main');

    const ncr = await createViaSave(
      page, 'ErpQaNonConformance',
      {
        code: `E2E-NCR-CONC-${Date.now()}`,
        ncrDate: '2026-07-11',
        materialId: MATERIAL_ID,
        severity: 'NORMAL',
        dispositionType: 'CONCESSION',
        status: 'OPEN',
        quantity: 1,
        description: 'E2E NCR concession (no return side-effect)',
      },
      'id status code returnCode',
    );
    expect(ncr.status, 'new ncr status=OPEN').toBe('OPEN');

    try {
      await callMutationOk(
        page, 'ErpQaNonConformance', 'submitReview', { ncrId: ncr.id }, 'id status',
      );

      const resolved = await callMutationOk(
        page, 'ErpQaNonConformance', 'resolve',
        { ncrId: ncr.id, resolution: 'concession accepted' }, 'id status returnCode',
      );
      expect(resolved.status, 'resolve should transition IN_REVIEW → RESOLVED').toBe('RESOLVED');
      expect(resolved.returnCode, 'CONCESSION should have no return side-effect (returnCode=null)').toBeNull();

      const total = await findFirst<any>(
        page, 'ErpPurReturn', eqFilter('code', `PR-FROM-NCR-${ncr.id}`),
        'id code',
      );
      expect(total, 'no ErpPurReturn should be created for CONCESSION').toBeNull();
    } finally {
      await deleteById(page, 'ErpQaNonConformance', ncr.id);
    }
  });
});
