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
} from '../orchestration/_helper';

/**
 * maintenance × assets 跨域维修费用防双重扣减浏览器层 E2E（plan 2026-07-17-2256-2）。
 *
 * 验证当 assets 维修工单关联 mnt 维护工单（maintenanceVisitId 非空，linkedVisit=true）且该 visit
 * 有备件消耗（mnt `MAINTENANCE_ISSUE` 已贷 1403 存货）时，assets `MAINTENANCE_EXPENSE` 应贷
 * **2502 维修中转清算**（非 1403 存货），避免备件成本双重扣减。
 *
 * 后端权威分支（plan 2026-07-10-1100-6）：
 *   - `MaintenanceExpensePostingDispatcher.buildEvent:79`：`linkedVisit = maintenance.getMaintenanceVisitId() != null`。
 *   - `MaintenanceExpenseAcctDocProvider.createFacts:60-64`：
 *       linkedVisit=true  → Dr 6602 / **Cr 2502 维修中转清算**（备件已由 mnt 出库，不再贷存货）。
 *       linkedVisit=false → Dr 6602 / Cr 1002 银行存款（独立维修，无关联备件出库）。
 *
 * 跨域并存防双重扣减语义：
 *   - mnt `MAINTENANCE_ISSUE`：Dr 6602 维修费用 / Cr 1403 存货（备件实物出库，50）。
 *   - assets `MAINTENANCE_EXPENSE`（linkedVisit=true）：Dr 6602 / Cr 2502（**非 Cr 1403**）。
 *   备件 50 元成本仅由 mnt 一次贷记 1403 存货；assets 不再贷 1403，改贷 2502 中转清算，
 *   两域凭证加总不重复扣减存货。
 *
 * 对照（测试 2，linkedVisit=false）：assets 独立维修无关联 visit → Cr 1002 银行存款（与 0215-1
 * `ast-maintenance.action.spec.ts` 路径一致，确认分支差异）。
 *
 * Phase 1 Decision（Explore 裁定）：
 *   - 测试专用备件物料隔离（非种子 MAT-001），对齐 0704-2/0606-2 范式，使 mnt 备件出库 unitCost
 *     确定性（无 WEIGHTED_AVERAGE 混合）+ 清理整行删除不污染 inventory dashboard totalValue 基线。
 *   - 种子设备 id=1（EQ-2026-001 RUNNING）—— mnt confirm 与 assets 维修均不修改设备状态。
 *   - 种子资产 id=3（AST-2026-003 IN_SERVICE，EXPENSE 路径不修改资产卡片，无原值回退污染）。
 *   - mnt Visit 仅作 maintenanceVisitId 软 FK 持有，linkedVisit 判定只检查 != null（Dispatcher:79），
 *     不校验 visit status，故 DRAFT visit 即可触发分支。
 *
 * 确定性值：
 *   测试 1（linkedVisit=true）：备件 qty=10 × unitCost=5 → amount=50；
 *     mnt MAINTENANCE_ISSUE Dr 6602=50 / Cr 1403=50；
 *     assets MaintenanceCost SPARE_PART=50 → totalCost=50 → MAINTENANCE_EXPENSE Dr 6602=50 / Cr 2502=50。
 *   测试 2（linkedVisit=false 对照）：assets MaintenanceCost SPARE_PART=80 → totalCost=80
 *     → MAINTENANCE_EXPENSE Dr 6602=80 / Cr 1002=80。
 *
 * 清理（依赖反向顺序，两测试各自 try/finally 独立隔离）：
 *   测试 1：assets 凭证（billHeadCode=maintenance.code）→ mnt 凭证（billHeadCode=usage.code+"-MI"）
 *     → mnt OUTGOING 移动（relatedBillType=ERP_MNT_SPARE_PART+relatedBillCode=usage.code）
 *     → assets 费用行 + 维修工单 → mnt UsageLine+Usage+Visit
 *     → StockLedger/StockBalance 按测试物料 materialId+warehouseId
 *     → INCOMING 备货移动（lines+move）→ 测试物料。
 *   测试 2：assets 凭证 → assets 费用行 + 维修工单。
 */

const ORG = 2;
const WH = 2;            // WH-RAW
const UOM = 1;           // PCS
const CURRENCY = 1;
const ACCT_SCHEMA = 1;   // ACCT-FIN-01
const EQUIPMENT_ID = 1;  // EQ-2026-001 种子设备 RUNNING
const ASSIGNED_TO = 2;   // 种子员工
const SEED_ASSET_ID = 3; // AST-2026-003 IN_SERVICE（EXPENSE 不修改资产）
const BDATE = '2026-07-10';
const VISIT_DATE = '2026-12-25';
const MOVE_REQ_TYPE = 'i_app_erp_inv_biz_StockMoveRequest';

// 测试 1 确定性派生值
const SETUP_QTY_1 = 20;
const SETUP_UNIT_COST_1 = 5;
const USAGE_QTY_1 = 10;
const USAGE_UNIT_COST_1 = 5;
const USAGE_AMOUNT_1 = USAGE_QTY_1 * USAGE_UNIT_COST_1; // 50
// 测试 2 独立金额（无 mnt 备件出库关联，纯 assets 独立维修）
const ASSET_COST_2 = 80;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

test.describe('maintenance × assets linked-visit anti-double-deduct (cross-domain posting branch)', () => {
  test('linkedVisit=true: mnt MAINTENANCE_ISSUE Dr6602/Cr1403 + assets MAINTENANCE_EXPENSE Dr6602/Cr2502 (clearing, not 1403)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstMaintenance-main');

    const ts = Date.now();

    // 1. 测试专用备件物料（RAW_MATERIAL, MOVING_AVERAGE, 无种子余额 → 确定性 unitCost + 清理安全）
    const material = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code: uniq('E2E-LV-MAT'), name: 'E2E Linked-Visit Spare Part Material',
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
          lines: [{ materialId: material.id, uoMId: UOM, quantity: SETUP_QTY_1, unitCost: SETUP_UNIT_COST_1, currencyId: CURRENCY }],
          remark: `E2E-LV-SEED-${ts}`,
        }),
      },
      'id code',
    );
    await callMutationOk(page, 'ErpInvStockMove', 'complete', { moveId: setupMove.id }, 'id docStatus');

    // 3. mnt DRAFT Visit（仅作 maintenanceVisitId 软 FK 持有，linkedVisit 判定只看 != null）
    const visit = await createViaSave(
      page, 'ErpMntVisit',
      {
        code: uniq('E2E-LV-VIS'), equipmentId: EQUIPMENT_ID, visitDate: VISIT_DATE,
        status: 'DRAFT', assignedTo: ASSIGNED_TO, visitType: 'PLANNED', orgId: ORG,
      },
      'id',
    );

    // 4. mnt SparePartUsage（visitId 显式指向 visit）+ Line（qty=10 × unitCost=5 → amount=50）
    const usage = await createViaSave(
      page, 'ErpMntSparePartUsage',
      {
        code: uniq('E2E-LV-SPU'), orgId: ORG, equipmentId: EQUIPMENT_ID, warehouseId: WH,
        visitId: visit.id, businessDate: BDATE,
        docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', posted: false,
      },
      'id code docStatus posted',
    );
    const usageLine = await createViaSave(
      page, 'ErpMntSparePartUsageLine',
      {
        sparePartUsageId: usage.id, lineNo: 1, materialId: material.id, uoMId: UOM,
        quantity: USAGE_QTY_1, unitCost: USAGE_UNIT_COST_1, amount: USAGE_AMOUNT_1,
      },
      'id',
    );

    // 5. assets Maintenance（**maintenanceVisitId=visit.id 触发 linkedVisit=true**）+ Cost 行（SPARE_PART=50）
    const mntCode = uniq('E2E-LV-AST-MNT');
    const mnt = await callMutationOk(
      page, 'ErpAstMaintenance', 'createMaintenance',
      {
        assetId: SEED_ASSET_ID, code: mntCode, name: `E2E 跨域防双重扣减 ${mntCode}`,
        businessDate: BDATE, maintenanceVisitId: visit.id, reason: 'E2E linkedVisit=true',
      },
      'id code status',
    );
    expect(mnt.status, 'createMaintenance should set status=DRAFT').toBe('DRAFT');

    // __get 独立核实 maintenanceVisitId 已落库（linkedVisit 判定源）
    const mntVerified = await verifyState(page, 'ErpAstMaintenance', mnt.id, 'maintenanceVisitId');
    expect(Number(mntVerified.maintenanceVisitId), 'maintenanceVisitId should be set to visit.id').toBe(Number(visit.id));

    const costRow = await createViaSave(
      page, 'ErpAstMaintenanceCost',
      {
        maintenanceId: mnt.id, orgId: ORG, costType: 'SPARE_PART',
        amount: USAGE_AMOUNT_1, businessDate: BDATE, currencyId: CURRENCY,
      },
      'id',
    );

    try {
      // ---- mnt 备件消耗 confirm → MAINTENANCE_ISSUE 凭证 ----
      const confirmed = await callMutationOk(
        page, 'ErpMntSparePartUsage', 'confirm', { usageId: usage.id },
        'id docStatus posted',
      );
      expect(confirmed.docStatus, 'confirm should set docStatus=ACTIVE').toBe('ACTIVE');
      expect(confirmed.posted, 'confirm should set posted=true (stock issued + GL posted)').toBe(true);

      // mnt MAINTENANCE_ISSUE 凭证行精确数值断言：Dr 6602 维修费用=50 / Cr 1403 存货=50
      // billHeadCode = usage.code + "-MI"（MaintenanceIssuePostingDispatcher）
      const mntVoucherId = await findVoucherIdByBillCode(page, usage.code + '-MI', 'NORMAL');
      await assertVoucherLines(page, mntVoucherId, [
        { subjectCode: '6602', dcDirection: 'DEBIT', debitAmount: USAGE_AMOUNT_1, creditAmount: 0 },
        { subjectCode: '1403', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: USAGE_AMOUNT_1 },
      ]);

      // ---- assets 维修审批 + 过账链 ----
      await callMutationOk(page, 'ErpAstMaintenance', 'submit', { id: mnt.id }, 'id status');
      await callMutationOk(page, 'ErpAstMaintenance', 'startWork', { id: mnt.id }, 'id status');
      await callMutationOk(page, 'ErpAstMaintenance', 'completeWork', { id: mnt.id }, 'id status');

      const decided = await callMutationOk(
        page, 'ErpAstMaintenance', 'decideTreatment',
        { id: mnt.id, treatment: 'EXPENSE' },
        'id treatment totalCostAmount',
      );
      expect(decided.treatment, 'treatment=EXPENSE').toBe('EXPENSE');
      expect(Number(decided.totalCostAmount), 'totalCost=SPARE_PART 50').toBe(USAGE_AMOUNT_1);

      await callMutationOk(page, 'ErpAstMaintenance', 'approve', { id: mnt.id }, 'id status');

      // post → POSTED + MAINTENANCE_EXPENSE 凭证
      const posted = await callMutationOk(
        page, 'ErpAstMaintenance', 'post', { id: mnt.id }, 'id status posted',
      );
      expect(posted.status, 'post should transition COMPLETED → POSTED').toBe('POSTED');
      expect(posted.posted, 'MAINTENANCE_EXPENSE posting → posted=true').toBe(true);

      // assets MAINTENANCE_EXPENSE 凭证行精确数值断言：
      // **防双重扣减核心** — linkedVisit=true → Dr 6602 维修费用=50 / **Cr 2502 维修中转清算=50（非 Cr 1403 存货）**
      // 备件成本已由 mnt MAINTENANCE_ISSUE 贷记 1403，assets 不重复贷存货改贷中转清算。
      const astVoucherId = await findVoucherIdByBillCode(page, mntCode, 'NORMAL');
      await assertVoucherLines(page, astVoucherId, [
        { subjectCode: '6602', dcDirection: 'DEBIT', debitAmount: USAGE_AMOUNT_1, creditAmount: 0 },
        { subjectCode: '2502', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: USAGE_AMOUNT_1 },
      ]);
    } finally {
      // ---- 清理（依赖反向顺序）----
      // 1. assets MAINTENANCE_EXPENSE 凭证（billHeadCode=mntCode）
      await cleanupVoucherByBillCode(page, mntCode);
      // 2. mnt MAINTENANCE_ISSUE 凭证（billHeadCode=usage.code+"-MI"）
      await cleanupVoucherByBillCode(page, usage.code + '-MI');
      // 3. mnt OUTGOING 移动（relatedBillType=ERP_MNT_SPARE_PART + relatedBillCode=usage.code）
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
      // 4. assets 费用行 + 维修工单（EXPENSE 不修改资产卡片，无回退需要）
      await deleteById(page, 'ErpAstMaintenanceCost', costRow.id);
      await deleteById(page, 'ErpAstMaintenance', mnt.id);
      // 5. mnt UsageLine + Usage + Visit
      await deleteById(page, 'ErpMntSparePartUsageLine', usageLine.id);
      await deleteById(page, 'ErpMntSparePartUsage', usage.id);
      await deleteById(page, 'ErpMntVisit', visit.id);
      // 6. StockLedger 按 materialId（含 INCOMING 备货 + OUTGOING 出库行）
      await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('materialId', Number(material.id)));
      // 7. StockBalance 按 materialId+warehouseId（测试专用物料无种子余额行，整行删除安全）
      await deleteByFilter(page, 'ErpInvStockBalance', andFilter(eqFilter('materialId', Number(material.id)), eqFilter('warehouseId', WH)));
      // 8. INCOMING 备货移动（lines + move）
      await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(setupMove.id)));
      await deleteById(page, 'ErpInvStockMove', setupMove.id);
      // 9. 测试物料
      await deleteById(page, 'ErpMdMaterial', material.id);
    }
  });

  test('linkedVisit=false 对照: assets MAINTENANCE_EXPENSE Dr6602/Cr1002 bank (independent maintenance, no visit link)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstMaintenance-main');

    // assets 独立维修：createMaintenance 不传 maintenanceVisitId → linkedVisit=false
    const mntCode = uniq('E2E-LV-IND-MNT');
    const mnt = await callMutationOk(
      page, 'ErpAstMaintenance', 'createMaintenance',
      {
        assetId: SEED_ASSET_ID, code: mntCode, name: `E2E 独立维修对照 ${mntCode}`,
        businessDate: BDATE, reason: 'E2E linkedVisit=false',
      },
      'id code status',
    );
    expect(mnt.status, 'createMaintenance should set status=DRAFT').toBe('DRAFT');

    // __get 独立核实 maintenanceVisitId 未设置（linkedVisit 判定为 false）
    const mntVerified = await verifyState(page, 'ErpAstMaintenance', mnt.id, 'maintenanceVisitId');
    expect(mntVerified.maintenanceVisitId, 'maintenanceVisitId should be null for independent maintenance').toBeNull();

    const costRow = await createViaSave(
      page, 'ErpAstMaintenanceCost',
      {
        maintenanceId: mnt.id, orgId: ORG, costType: 'SPARE_PART',
        amount: ASSET_COST_2, businessDate: BDATE, currencyId: CURRENCY,
      },
      'id',
    );

    try {
      await callMutationOk(page, 'ErpAstMaintenance', 'submit', { id: mnt.id }, 'id status');
      await callMutationOk(page, 'ErpAstMaintenance', 'startWork', { id: mnt.id }, 'id status');
      await callMutationOk(page, 'ErpAstMaintenance', 'completeWork', { id: mnt.id }, 'id status');

      const decided = await callMutationOk(
        page, 'ErpAstMaintenance', 'decideTreatment',
        { id: mnt.id, treatment: 'EXPENSE' },
        'id treatment totalCostAmount',
      );
      expect(decided.treatment, 'treatment=EXPENSE').toBe('EXPENSE');
      expect(Number(decided.totalCostAmount), 'totalCost=SPARE_PART 80').toBe(ASSET_COST_2);

      await callMutationOk(page, 'ErpAstMaintenance', 'approve', { id: mnt.id }, 'id status');

      const posted = await callMutationOk(
        page, 'ErpAstMaintenance', 'post', { id: mnt.id }, 'id status posted',
      );
      expect(posted.status, 'post should transition COMPLETED → POSTED').toBe('POSTED');
      expect(posted.posted, 'MAINTENANCE_EXPENSE posting → posted=true').toBe(true);

      // assets MAINTENANCE_EXPENSE 凭证行精确数值断言：
      // linkedVisit=false → Dr 6602 维修费用=80 / Cr 1002 银行存款=80（独立维修无关联备件出库）
      const astVoucherId = await findVoucherIdByBillCode(page, mntCode, 'NORMAL');
      await assertVoucherLines(page, astVoucherId, [
        { subjectCode: '6602', dcDirection: 'DEBIT', debitAmount: ASSET_COST_2, creditAmount: 0 },
        { subjectCode: '1002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: ASSET_COST_2 },
      ]);
    } finally {
      // 清理：assets 凭证 → 费用行 → 维修工单
      await cleanupVoucherByBillCode(page, mntCode);
      await deleteById(page, 'ErpAstMaintenanceCost', costRow.id);
      await deleteById(page, 'ErpAstMaintenance', mnt.id);
    }
  });
});
