import {
  test,
  expect,
  loginAndNavigate,
  runMfgChain,
  cleanupMfg,
  findItems,
  verifyState,
  eqFilter,
  MFG_EXPECT,
  SEED,
} from './_helper';
import type { Page } from '@playwright/test';

/**
 * 制造域批次基因追溯浏览器层 E2E（plan 2026-07-11-0730-1 Phase 1）。
 *
 * 验证 config `erp-mfg.genealogy-write-enabled` 默认 true 启用后，WorkOrder 满量完工触发
 * BatchGenealogyWriter.writeOnCompletion 写入 inputLot→outputLot 基因链（ErpMfgBatchGenealogy）
 * 的浏览器层全栈可达性，以及前向/反向追溯查询经 GraphQL 返回非空消耗关系。
 *
 * 前置条件（runMfgChain 扩展 withBatchTracking=true）：
 *   - 运行时创建输入 ErpInvBatch（batchNo=GEN-IN-{ts}，组件物料 + WH-RAW）
 *   - MaterialIssueLine.batchNo 引用该输入批次
 *   - 完工时 ensureOutputLot 自动创建产出批次（batchNo=FG-{woCode}）
 *
 * 断言三层：
 *   1. 状态流转——reportCompletion 后 docStatus=COMPLETED（满量完工）。
 *   2. 基因链写入——ErpMfgBatchGenealogy__findPage(filter{workOrderId}) 非空，
 *      inputLotId/outputLotId 非空 + inputMaterialId(组件)/outputMaterialId(成品) 正确。
 *   3. 追溯查询可达——forwardTrace(outputLotId) 返回非空（含 inputLot→outputLot 消耗行）；
 *      backwardTrace(inputLotId) 返回含产出批次。
 *
 * 清理：cleanupMfg 扩展覆盖 ErpMfgBatchGenealogy + 输入/输出 ErpInvBatch。
 */

/** 经 GraphQL 调用 ErpMfgBatchGenealogy @BizQuery 追溯查询（forwardTrace/backwardTrace），返回基因行列表。 */
async function traceQuery(
  page: Page,
  action: 'forwardTrace' | 'backwardTrace',
  argName: string,
  lotId: number,
): Promise<any[]> {
  const resp = await page.request.post('/graphql', {
    data: {
      query: `{ ErpMfgBatchGenealogy__${action}(${argName}:${lotId}){ id inputLotId outputLotId inputMaterialId outputMaterialId } }`,
    },
  });
  const json: any = await resp.json();
  expect(json?.errors, `ErpMfgBatchGenealogy__${action} should not return GraphQL errors`).toBeFalsy();
  return json?.data?.[`ErpMfgBatchGenealogy__${action}`] ?? [];
}

test.describe('manufacturing batch genealogy browser-layer E2E (config-gated write-on-completion + trace queries)', () => {
  test('full completion writes input→output genealogy chain + forward/backward trace non-empty', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMfgWorkOrder-main');

    const r = await runMfgChain(page, { withBatchTracking: true });
    try {
      // ---- 完工终态：满量完工 COMPLETED ----
      const woState = await verifyState(page, 'ErpMfgWorkOrder', r.wo.id, 'docStatus completedQuantity');
      expect(woState?.docStatus, 'full completion → COMPLETED').toBe('COMPLETED');
      expect(Number(woState?.completedQuantity ?? 0), 'completedQuantity=10').toBe(MFG_EXPECT.completedQty);

      // ---- 基因链写入：findPage 非空 + inputLot/outputLot 非空 + 物料正确 ----
      const genealogyRows = await findItems(
        page, 'ErpMfgBatchGenealogy',
        eqFilter('workOrderId', Number(r.wo.id)),
        'id inputLotId outputLotId inputMaterialId outputMaterialId inputQty outputQty',
      );
      expect(genealogyRows.length, 'ErpMfgBatchGenealogy rows should be non-empty (withBatchTracking triggers writeOnCompletion)')
        .toBeGreaterThan(0);
      const row = genealogyRows[0];
      expect(Number(row.inputLotId), 'inputLotId non-empty (input batch)').toBeTruthy();
      expect(Number(row.outputLotId), 'outputLotId non-empty (output batch)').toBeTruthy();
      expect(Number(row.inputMaterialId), 'inputMaterialId = component material').toBe(Number(r.componentMat.id));
      expect(Number(row.outputMaterialId), 'outputMaterialId = finished product').toBe(SEED.MAT_1);

      // ---- 追溯查询可达：forwardTrace(outputLotId) 非空 ----
      const outputLotId = Number(row.outputLotId);
      const inputLotId = Number(row.inputLotId);
      const forward = await traceQuery(page, 'forwardTrace', 'outputLotId', outputLotId);
      expect(forward.length, 'forwardTrace(outputLotId) should return non-empty genealogy edges').toBeGreaterThan(0);
      const forwardHasInput = forward.some((e: any) => Number(e.inputLotId) === inputLotId);
      expect(forwardHasInput, 'forwardTrace should contain the input→output consumption edge').toBe(true);

      // ---- 反向追溯：backwardTrace(inputLotId) 非空 + 含产出批次 ----
      const backward = await traceQuery(page, 'backwardTrace', 'inputLotId', inputLotId);
      expect(backward.length, 'backwardTrace(inputLotId) should return non-empty genealogy edges').toBeGreaterThan(0);
      const backwardHasOutput = backward.some((e: any) => Number(e.outputLotId) === outputLotId);
      expect(backwardHasOutput, 'backwardTrace should include the output (finished good) lot').toBe(true);
    } finally {
      await cleanupMfg(page, r);
    }
  });
});
