import { test, expect, loginAndNavigate } from '../fixtures';
import { GraphQLClient } from '../pages';

/**
 * E3.2 库存审计快照 value-spec（`audit-snapshot-cycle-count.md` §1/§3）：
 * 种子基线（erp_inv_stock_ledger.csv：LDG-001 期初 80@120=9600@06-30 + LDG-002 入库 100@8.5=850@07-03）
 * 驱动的确定性数值断言——快照派生值、asOfDate 边界裁剪、「账面余额 = 快照派生值」一致性。
 */
test.describe('inventory snapshot value-spec', () => {
  test('getInventorySnapshot returns deterministic derived values with asOfDate boundary', async ({ page }) => {
    await loginAndNavigate(page, '/inv-dashboard-main');
    const client = new GraphQLClient(page);

    // 全量快照（asOf 覆盖全部流水）：期初 80 + 入库 100 = 180；9600 + 850 = 10450（与余额总值同源对齐）
    const full: any = await client.raw(
      'query($asOfDate:String){ ErpInvStockLedger__getInventorySnapshot(asOfDate:$asOfDate) }',
      { asOfDate: '2026-07-31' },
    );
    const fullSnap = full?.data?.ErpInvStockLedger__getInventorySnapshot;
    expect(fullSnap, 'snapshot map should be present').toBeTruthy();
    expect(Number(fullSnap.rowCount)).toBe(2);
    expect(Number(fullSnap.totalQuantity)).toBe(180);
    expect(Number(fullSnap.totalCost)).toBe(10450);

    // 边界裁剪：asOf 2026-06-29 早于全部流水 → 空快照
    const before: any = await client.raw(
      'query($asOfDate:String){ ErpInvStockLedger__getInventorySnapshot(asOfDate:$asOfDate) }',
      { asOfDate: '2026-06-29' },
    );
    const beforeSnap = before?.data?.ErpInvStockLedger__getInventorySnapshot;
    expect(Number(beforeSnap.rowCount)).toBe(0);
    expect(Number(beforeSnap.totalQuantity)).toBe(0);

    // 边界裁剪：asOf 2026-07-02 仅计期初（07-03 流水被裁剪）
    const mid: any = await client.raw(
      'query($asOfDate:String){ ErpInvStockLedger__getInventorySnapshot(asOfDate:$asOfDate) }',
      { asOfDate: '2026-07-02' },
    );
    const midSnap = mid?.data?.ErpInvStockLedger__getInventorySnapshot;
    expect(Number(midSnap.rowCount)).toBe(1);
    expect(Number(midSnap.totalQuantity)).toBe(80);
    expect(Number(midSnap.totalCost)).toBe(9600);

    // 仓库过滤：仅仓库 2（07-03 入库行）
    const wh: any = await client.raw(
      'query($warehouseId:String,$asOfDate:String){ ErpInvStockLedger__getInventorySnapshot(warehouseId:$warehouseId,asOfDate:$asOfDate) }',
      { warehouseId: '2', asOfDate: '2026-07-31' },
    );
    const whSnap = wh?.data?.ErpInvStockLedger__getInventorySnapshot;
    expect(Number(whSnap.rowCount)).toBe(1);
    expect(Number(whSnap.totalQuantity)).toBe(100);
    expect(Number(whSnap.totalCost)).toBe(850);

    // 一致性校验：种子基线账面余额 = 快照派生值（零差异）
    const check: any = await client.raw(
      'query($asOfDate:String){ ErpInvStockLedger__checkStockBalanceConsistency(asOfDate:$asOfDate) }',
      { asOfDate: '2026-07-31' },
    );
    const report = check?.data?.ErpInvStockLedger__checkStockBalanceConsistency;
    expect(report, 'consistency report should be present').toBeTruthy();
    expect(Number(report.mismatchCount)).toBe(0);
    expect(report.consistent).toBe(true);
  });
});
