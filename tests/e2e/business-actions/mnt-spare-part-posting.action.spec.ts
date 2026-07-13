import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutationOk,
  verifyState,
  eqFilter,
  andFilter,
  deleteByFilter,
  deleteById,
  findFirst,
  input,
} from './_helper';
import {
  cleanupVoucherByBillCode,
  findVoucherIdByBillCode,
  assertVoucherLines,
  findItems,
} from '../orchestration/_helper';

/**
 * maintenance ErpMntSparePartUsage 备件消耗 confirm + MAINTENANCE_ISSUE 过账浏览器层 E2E
 * （plan 2026-07-14-0606-2 Phase 2）。
 *
 * 验证备件消耗确认经 GraphQL /graphql 的全栈可达性 + MAINTENANCE_ISSUE(492) 凭证行数值断言：
 *   confirm(usageId) @BizMutation → SparePartIssueService.issue（OUTGOING 移动，relatedBillType=ERP_MNT_SPARE_PART）
 *   → posted=true（库存已出库）→ MaintenanceIssuePostingDispatcher.dispatchIfApplicable（config-gated）
 *   → MAINTENANCE_ISSUE 凭证（Dr 6602 维修费用 / Cr 1403 存货）。
 *
 * 权威实现（ErpMntSparePartUsageBizModel.confirm:97-119）：
 *   requireUsage → validateNotConfirmed（幂等） → loadLines（空集守卫） → sparePartIssueService.issue
 *   → applyIssueResult（docStatus=ACTIVE + approveStatus=APPROVED + posted=isStockIssued(move)）
 *   → if posted: issuePostingDispatcher.dispatchIfApplicable（config erp-mnt.spare-part-posting-enabled）。
 *
 * MAINTENANCE_ISSUE 凭证（MaintenanceIssueAcctDocProvider.createFacts:56-85）：
 *   Dr 6602 维修费用（config erp-mnt.expense-subject-code 默认 6602，汇总金额）
 *   / Cr 1403 存货（config erp-mnt.inventory-subject-code 默认 1403，按物料分行）。
 *   billHeadCode = usage.code + "-MI"（MaintenanceIssuePostingDispatcher.java:93,129）。
 *
 * config 门控（Infrastructure And Config Prereqs）：
 *   webServer JVM arg 追加 `-Derp-mnt.spare-part-posting-enabled=true`（默认 false 向后兼容，
 *   E2E 按需开启对齐 subcontract-posting-enabled/inspection-gate-enabled 范式）。
 *   config-gated 关闭路径（posted=true 但无凭证）经后端单测 TestErpMntSparePartPosting 场景 3 覆盖（Deferred）。
 *
 * Phase 2 Decision（Explore 裁定）：测试专用物料隔离。
 *   备件物料用**测试专用新建物料**（非种子 MAT-001），对齐 0704-2 mfg-chain 测试专用物料隔离范式。
 *   避免清理抹除种子余额污染 inventory dashboard totalValue 基线（dashboard 读 stock_balance）。
 *   设备用种子 id=1（EQ-2026-001 RUNNING）——confirm 不改变设备状态（设备状态联动仅 Visit start/complete，
 *   非 SparePartUsage confirm），无污染风险。
 *
 * 确定性值（单行 Usage + 单备件物料）：
 *   备货 INCOMING: qty=20, unitCost=5 → balance qty=20, avgCost=5, totalCost=100
 *   UsageLine: qty=10, unitCost=5, amount=50 → OUTGOING qty=10, moving-average cost=5 → totalCost=50
 *   → Dr 6602 = 50 / Cr 1403 = 50
 *
 * 清理：MAINTENANCE_ISSUE 凭证（billHeadCode=usage.code+"-MI"）
 *   + OUTGOING 移动（relatedBillType=ERP_MNT_SPARE_PART + relatedBillCode=usage.code）
 *   + StockLedger 按 materialId（含 INCOMING 备货 + OUTGOING 出库行）
 *   + StockBalance 按 materialId+warehouseId + INCOMING 备货移动 + UsageLine + Usage + 测试物料。
 *   posting 不写 gl_balance，不污染 finance dashboard 基线。
 */

const ORG = 2;
const WH = 2; // WH-RAW
const UOM = 1; // PCS
const CURRENCY = 1;
const ACCT_SCHEMA = 1; // ACCT-FIN-01
const EQUIPMENT_ID = 1; // EQ-2026-001 种子设备 RUNNING（confirm 不改变设备状态）
const BDATE = '2026-07-10';
const MOVE_REQ_TYPE = 'i_app_erp_inv_biz_StockMoveRequest';

const SETUP_QTY = 20;
const SETUP_UNIT_COST = 5;
const USAGE_QTY = 10;
const USAGE_UNIT_COST = 5;
const USAGE_AMOUNT = USAGE_QTY * USAGE_UNIT_COST; // 50

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

test.describe('maintenance ErpMntSparePartUsage confirm + MAINTENANCE_ISSUE posting voucher line assertion', () => {
  test('confirm(usageId) → posted=true + OUTGOING move + MAINTENANCE_ISSUE voucher Dr 6602 / Cr 1403', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMntSparePartUsage-main');

    const ts = Date.now();

    // 1. 测试专用备件物料（RAW_MATERIAL, MOVING_AVERAGE, 无种子余额 → 确定性 unitCost + 清理安全）
    const material = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code: uniq('E2E-MNT-MAT'), name: 'E2E Maintenance Spare Part Material',
        materialType: 'RAW_MATERIAL', uoMId: UOM, status: 'ACTIVE',
        costMethod: 'MOVING_AVERAGE', defaultWarehouseId: WH,
      },
      'id',
    );

    // 2. 前置备货：INCOMING 建库存余量（OUTGOING 扣减需源余额），qty=20 × unitCost=5 → balance totalCost=100
    const setupMove = await callMutationOk(
      page, 'ErpInvStockMove', 'generateMove',
      {
        request: input(MOVE_REQ_TYPE, {
          moveType: 'INCOMING', orgId: ORG, businessDate: BDATE,
          destWarehouseId: WH, acctSchemaId: ACCT_SCHEMA, currencyId: CURRENCY,
          lines: [{ materialId: material.id, uoMId: UOM, quantity: SETUP_QTY, unitCost: SETUP_UNIT_COST, currencyId: CURRENCY }],
          remark: `E2E-MNT-SEED-${ts}`,
        }),
      },
      'id code',
    );
    await callMutationOk(page, 'ErpInvStockMove', 'complete', { moveId: setupMove.id }, 'id docStatus');

    // 3. 建 DRAFT SparePartUsage + Line
    const usage = await createViaSave(
      page, 'ErpMntSparePartUsage',
      {
        code: uniq('E2E-MNT-SPU'), orgId: ORG, equipmentId: EQUIPMENT_ID, warehouseId: WH,
        businessDate: BDATE, docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', posted: false,
      },
      'id code docStatus posted',
    );
    expect(usage.docStatus, 'new usage docStatus=DRAFT').toBe('DRAFT');

    const usageLine = await createViaSave(
      page, 'ErpMntSparePartUsageLine',
      {
        sparePartUsageId: usage.id, lineNo: 1, materialId: material.id, uoMId: UOM,
        quantity: USAGE_QTY, unitCost: USAGE_UNIT_COST, amount: USAGE_AMOUNT,
      },
      'id',
    );

    try {
      // ---- confirm(usageId) @BizMutation ----
      const confirmed = await callMutationOk(
        page, 'ErpMntSparePartUsage', 'confirm', { usageId: usage.id },
        'id docStatus posted',
      );
      // applyIssueResult: docStatus=ACTIVE (erp-mnt/doc-status) + posted=isStockIssued(move)=true（OUTGOING business-linked auto-DONE）
      expect(confirmed.docStatus, 'confirm should set docStatus=ACTIVE').toBe('ACTIVE');
      expect(confirmed.posted, 'confirm should set posted=true (stock issued + GL posted)').toBe(true);

      // __get 权威确认 posted=true
      const usageFinal = await verifyState(page, 'ErpMntSparePartUsage', usage.id, 'docStatus posted');
      expect(usageFinal.docStatus, '__get should confirm ACTIVE').toBe('ACTIVE');
      expect(usageFinal.posted, '__get should confirm posted=true').toBe(true);

      // ---- OUTGOING 移动单存在性断言（relatedBillType=ERP_MNT_SPARE_PART）----
      const issueMove = await findFirst<any>(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_MNT_SPARE_PART'), eqFilter('relatedBillCode', usage.code)),
        'id code docStatus moveType',
      );
      expect(issueMove, 'confirm should produce OUTGOING move with relatedBillType=ERP_MNT_SPARE_PART').toBeTruthy();
      expect(issueMove.docStatus, 'OUTGOING move should be DONE (business-linked auto-complete)').toBe('DONE');

      // ---- MAINTENANCE_ISSUE 凭证行精确数值断言 ----
      // billHeadCode = usage.code + "-MI"（MaintenanceIssuePostingDispatcher.java:93,129）
      const voucherId = await findVoucherIdByBillCode(page, usage.code + '-MI', 'NORMAL');
      await assertVoucherLines(page, voucherId, [
        { subjectCode: '6602', dcDirection: 'DEBIT', debitAmount: USAGE_AMOUNT, creditAmount: 0 },
        { subjectCode: '1403', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: USAGE_AMOUNT },
      ]);
    } finally {
      // ---- 清理（依赖反向顺序）----
      // 1. MAINTENANCE_ISSUE 凭证（billHeadCode=usage.code+"-MI"）
      await cleanupVoucherByBillCode(page, usage.code + '-MI');
      // 2. OUTGOING 移动（relatedBillType=ERP_MNT_SPARE_PART + relatedBillCode=usage.code）
      const issueMove = await findFirst<any>(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_MNT_SPARE_PART'), eqFilter('relatedBillCode', usage.code)),
        'id code',
      );
      if (issueMove) {
        await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('moveId', Number(issueMove.id)));
        await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(issueMove.id)));
        await deleteById(page, 'ErpInvStockMove', issueMove.id);
      }
      // 3. UsageLine + Usage
      await deleteById(page, 'ErpMntSparePartUsageLine', usageLine.id);
      await deleteById(page, 'ErpMntSparePartUsage', usage.id);
      // 4. StockLedger 按 materialId（含 INCOMING 备货 + OUTGOING 出库行）
      await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('materialId', Number(material.id)));
      // 5. StockBalance 按 materialId+warehouseId
      await deleteByFilter(page, 'ErpInvStockBalance', andFilter(eqFilter('materialId', Number(material.id)), eqFilter('warehouseId', WH)));
      // 6. INCOMING 备货移动（lines + move）
      await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(setupMove.id)));
      await deleteById(page, 'ErpInvStockMove', setupMove.id);
      // 7. 测试物料
      await deleteById(page, 'ErpMdMaterial', material.id);
    }
  });
});
