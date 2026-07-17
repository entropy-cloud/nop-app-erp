import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
  callMutationOk,
  verifyState,
  eqFilter,
  findFirst,
  deleteByFilter,
  deleteById,
  GraphQLClient,
} from './_helper';
import { cleanupVoucherByBillCode, findVoucherIdByBillCode, assertVoucherLines } from '../orchestration/_helper';

/**
 * assets ErpAstInventory 资产盘点生命周期业务动作浏览器层 E2E（plan 2026-07-14-0215-1 Phase 2）。
 *
 * 验证盘点 8 动作 DIRECT @BizMutation 状态机经 GraphQL /graphql 的全栈可达性 + 状态翻转 +
 * ASSET_INVENTORY_ADJUSTMENT 业财过账触发可观测性（posted 标志翻转）+ reverse 红冲 + cancel 异常路径。
 *
 * 0742-1 叠加 ASSET_INVENTORY_ADJUSTMENT(SHORTAGE) 凭证行精确数值断言（Dr 6711 / Cr 1601，正向 + 红冲同向取负）。
 *
 * 权威状态机（ErpAstInventoryProcessor，inventory-status 字典）：
 *   createInventory(DRAFT，范围展开为行) → submitForCount(COUNTING) → reconcile(RECONCILING，差异计算) →
 *   processVariance(差异处置：盘亏 SCRAPPED / 盘盈建卡) → approve(复核) → post(POSTED + 过账) →
 *   reverse(RECONCILING + 红冲) / cancel(CANCELLED，非终态可达)
 *
 * 差异路径裁决（Phase 2）：采用 SHORTAGE（盘亏）路径——actualQuantity=0 < bookQuantity=1 →
 * varianceAmount=bookValue（资产 netBookValue，确定性非零）→ shortageAmount>0 → 过账 Dr 6711 / Cr 1601
 * （科目均经 0215-1 补齐种子）。盘亏处置将测试资产置 SCRAPPED（避免与 DISPOSAL 凭证双重过账，
 * owner doc inventory.md §三）。MATCHED-only（零差异）路径 Provider 返回空 facts → voucherId=null →
 * posted=false → status 停 RECONCILING，故须有实际差异以达 POSTED 终态。
 *
 * 范围隔离：自包含建测试资产类别 + 测试资产（IN_SERVICE + netBookValue=500），inventory.rangeCategoryId
 * 指向测试类别，使 expandAssetsToLines 仅展开该测试资产（避开种子 3 资产污染）。
 *
 * 行 actualQuantity 更新：盘点行由 createInventory 自动生成（actualQuantity=null），物理盘点结果经
 * ErpAstInventoryLine__save(id, actualQuantity) 部分更新写入（Nop __save 支持 id+字段部分更新）。
 *
 * 过账科目依赖（Phase 1 Decision）：AssetInventoryAcctDocProvider shortage 路径默认 1601（固定资产 Cr）/
 * 6711（营业外支出 Dr）。种子已含 6711(id=15)，0215-1 补齐 1601(id=27)。
 *
 * 种子引用：org id=2 / currency id=1（CNY）/ acctSchema ACCT-FIN-01 id=1。
 */
const ORG_ID = 2;
const CURRENCY_ID = 1;
const BDATE = '2026-07-10';

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

/**
 * 自包含建测试资产类别 + 测试资产（用于盘点范围隔离）。
 * 返回 { categoryId, assetId, assetCode }。
 */
async function setupIsolatedAsset(page: import('@playwright/test').Page): Promise<{
  categoryId: string; assetId: string; assetCode: string;
}> {
  const category = await createViaSave(
    page, 'ErpAstAssetCategory',
    {
      code: uniq('E2E-AST-CAT-INV'),
      name: 'E2E 盘点测试类别',
      depreciationMethod: 'STRAIGHT_LINE',
      usefulLifeMonths: 36,
    },
    'id',
  );
  const assetCode = uniq('E2E-AST-INV-ASSET');
  const asset = await createViaSave(
    page, 'ErpAstAsset',
    {
      code: assetCode,
      name: `E2E 盘点测试资产 ${assetCode}`,
      orgId: ORG_ID,
      categoryId: category.id,
      acquisitionDate: '2026-06-15',
      currencyId: CURRENCY_ID,
      originalValue: 500,
      residualValue: 0,
      depreciationMethod: 'STRAIGHT_LINE',
      usefulLifeMonths: 36,
      status: 'IN_SERVICE',
      accumulatedDepreciation: 0,
      netBookValue: 500,
    },
    'id code',
  );
  return { categoryId: category.id, assetId: asset.id, assetCode };
}

test.describe('assets ErpAstInventory count lifecycle', () => {
  test('8-action lifecycle: createInventory → submitForCount → reconcile → processVariance(SHORTAGE) → approve → post(POSTED) → reverse(RECONCILING)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstInventory-main');

    const setup = await setupIsolatedAsset(page);
    const invCode = uniq('E2E-AST-INV');
    const inv = await createViaSave(
      page, 'ErpAstInventory',
      {
        code: invCode,
        name: `E2E 资产盘点 ${invCode}`,
        orgId: ORG_ID,
        rangeCategoryId: setup.categoryId,
        businessDate: BDATE,
        currencyId: CURRENCY_ID,
        status: 'DRAFT',
      },
      'id code status',
    );
    expect(inv.id, '__save should create a DRAFT inventory').toBeTruthy();

    try {
      // createInventory: 范围展开为行（仅测试资产），status=DRAFT
      const created = await callMutationOk(
        page, 'ErpAstInventory', 'createInventory', { id: inv.id }, 'id status',
      );
      expect(created.status, 'createInventory should set status=DRAFT').toBe('DRAFT');

      // 物理盘点：更新行 actualQuantity=0（SHORTAGE：0 < bookQuantity=1）
      // __save 拒绝已存在 id，须经 __update 部分更新（Nop CrudBizModel 标准 mutation）
      const line = await findFirst<{ id: string }>(
        page, 'ErpAstInventoryLine', eqFilter('inventoryId', inv.id), 'id',
      );
      expect(line, 'createInventory should expand 1 line for the test asset').toBeTruthy();
      const lineUpdateJson: any = await new GraphQLClient(page).update(
        'ErpAstInventoryLine',
        { id: line!.id, actualQuantity: 0 },
        'id actualQuantity',
      );
      expect(lineUpdateJson, 'ErpAstInventoryLine__update should return updated line').not.toBeNull();

      // submitForCount: DRAFT → COUNTING
      const counting = await callMutationOk(
        page, 'ErpAstInventory', 'submitForCount', { id: inv.id }, 'id status',
      );
      expect(counting.status, 'submitForCount should transition DRAFT → COUNTING').toBe('COUNTING');

      // reconcile: COUNTING → RECONCILING（差异计算：SHORTAGE，shortageAmount=bookValue=500）
      const reconciling = await callMutationOk(
        page, 'ErpAstInventory', 'reconcile', { id: inv.id },
        'id status shortageCount shortageAmount',
      );
      expect(reconciling.status, 'reconcile should transition COUNTING → RECONCILING').toBe('RECONCILING');
      expect(reconciling.shortageCount, '1 shortage line').toBe(1);
      expect(Number(reconciling.shortageAmount), 'shortageAmount=bookValue=500').toBe(500);

      // processVariance: 盘亏处置 → asset SCRAPPED + disposition=DISPOSAL
      await callMutationOk(
        page, 'ErpAstInventory', 'processVariance', { id: inv.id }, 'id status',
      );
      const assetScrapped = await verifyState(page, 'ErpAstAsset', setup.assetId, 'status');
      expect(assetScrapped.status, 'processVariance shortage should set asset SCRAPPED').toBe('SCRAPPED');

      // approve: 复核（置 approvedAt/approvedBy，RECONCILING 态可达）
      await callMutationOk(
        page, 'ErpAstInventory', 'approve', { id: inv.id }, 'id status',
      );

      // post: RECONCILING → POSTED + ASSET_INVENTORY_ADJUSTMENT 过账（shortage>0 → Dr 6711 / Cr 1601）
      const posted = await callMutationOk(
        page, 'ErpAstInventory', 'post', { id: inv.id }, 'id status posted',
      );
      expect(posted.status, 'post should transition RECONCILING → POSTED').toBe('POSTED');
      expect(posted.posted, 'ASSET_INVENTORY_ADJUSTMENT posting should succeed → posted=true').toBe(true);

      const verified = await verifyState(page, 'ErpAstInventory', inv.id, 'status posted');
      expect(verified.status, '__get should confirm POSTED').toBe('POSTED');
      expect(verified.posted, '__get should confirm posted=true').toBe(true);

      // ASSET_INVENTORY_ADJUSTMENT(SHORTAGE) 正向凭证行精确数值断言（0742-1）：
      //   Dr 6711(营业外支出) / Cr 1601(固定资产)，金额=shortageAmount=bookValue=500
      // 金额派生：SHORTAGE 路径 actualQuantity(0) < bookQuantity(1) → varianceAmount=asset netBookValue(500)。
      // billHeadCode = invCode（AssetInventoryPostingDispatcher）。
      const invNormalVoucherId = await findVoucherIdByBillCode(page, invCode, 'NORMAL');
      expect(invNormalVoucherId, 'ASSET_INVENTORY_ADJUSTMENT NORMAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, invNormalVoucherId, [
        { subjectCode: '6711', dcDirection: 'DEBIT', debitAmount: 500, creditAmount: 0 },
        { subjectCode: '1601', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: 500 },
      ]);

      // reverse: POSTED → RECONCILING + 红冲凭证 + posted=false
      const reversed = await callMutationOk(
        page, 'ErpAstInventory', 'reverse', { id: inv.id }, 'id status posted',
      );
      expect(reversed.status, 'reverse should transition POSTED → RECONCILING').toBe('RECONCILING');
      expect(reversed.posted, 'reverse should clear posted=false').toBe(false);

      const verifiedRev = await verifyState(page, 'ErpAstInventory', inv.id, 'status posted');
      expect(verifiedRev.status, '__get should confirm RECONCILING after reverse').toBe('RECONCILING');
      expect(verifiedRev.posted, '__get should confirm posted=false after reverse').toBe(false);

      // ASSET_INVENTORY_ADJUSTMENT(SHORTAGE) 红冲凭证行断言（0742-1）：
      //   REVERSAL 凭证同向取负（Dr 6711=-500 / Cr 1601=-500）
      const invReversalVoucherId = await findVoucherIdByBillCode(page, invCode, 'REVERSAL');
      expect(invReversalVoucherId, 'ASSET_INVENTORY_ADJUSTMENT REVERSAL voucher should exist').toBeTruthy();
      await assertVoucherLines(page, invReversalVoucherId, [
        { subjectCode: '6711', dcDirection: 'DEBIT', debitAmount: -500, creditAmount: 0 },
        { subjectCode: '1601', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -500 },
      ]);

      // 清理凭证（billHeadCode=invCode，NORMAL+REVERSAL）
      await cleanupVoucherByBillCode(page, invCode);
    } finally {
      // finally 兜底：行 → 盘点 → 测试资产 → 测试类别
      await deleteByFilter(page, 'ErpAstInventoryLine', eqFilter('inventoryId', inv.id));
      await deleteById(page, 'ErpAstInventory', inv.id);
      await deleteById(page, 'ErpAstAsset', setup.assetId);
      await deleteById(page, 'ErpAstAssetCategory', setup.categoryId);
    }
  });

  test('cancel path: submitForCount(COUNTING) → cancel(CANCELLED) + illegal POSTED→submitForCount guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstInventory-main');

    const setup = await setupIsolatedAsset(page);
    const invCode = uniq('E2E-AST-INV-CNL');
    const inv = await createViaSave(
      page, 'ErpAstInventory',
      {
        code: invCode,
        name: `E2E 资产盘点取消 ${invCode}`,
        orgId: ORG_ID,
        rangeCategoryId: setup.categoryId,
        businessDate: BDATE,
        currencyId: CURRENCY_ID,
        status: 'DRAFT',
      },
      'id',
    );

    try {
      await callMutationOk(page, 'ErpAstInventory', 'createInventory', { id: inv.id }, 'id status');
      await callMutationOk(page, 'ErpAstInventory', 'submitForCount', { id: inv.id }, 'id status');

      // cancel: COUNTING（非终态）→ CANCELLED
      const cancelled = await callMutationOk(
        page, 'ErpAstInventory', 'cancel', { id: inv.id }, 'id status',
      );
      expect(cancelled.status, 'cancel should transition COUNTING → CANCELLED').toBe('CANCELLED');

      const verified = await verifyState(page, 'ErpAstInventory', inv.id, 'status');
      expect(verified.status, '__get should confirm CANCELLED').toBe('CANCELLED');

      // 非法迁移守卫：CANCELLED → submitForCount 拒绝（submitForCount 须 DRAFT）
      const rej = await callMutation(
        page, 'ErpAstInventory', 'submitForCount', { id: inv.id }, 'id',
      );
      expect(rej.errors, 'submitForCount from CANCELLED should be rejected').toBeTruthy();
    } finally {
      await deleteByFilter(page, 'ErpAstInventoryLine', eqFilter('inventoryId', inv.id));
      await deleteById(page, 'ErpAstInventory', inv.id);
      await deleteById(page, 'ErpAstAsset', setup.assetId);
      await deleteById(page, 'ErpAstAssetCategory', setup.categoryId);
    }
  });
});
