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
 * maintenance × assets 跨域维修资本化防双重扣减浏览器层 E2E（plan 2026-07-19-0849-3）。
 *
 * 2256-2 EXPENSE 段已落地（mnt-ast-linked-visit-anti-double-deduct.action.spec.ts）：
 *   - EXPENSE 路径 linkedVisit=true → assets MAINTENANCE_EXPENSE Dr 6602 / **Cr 2502 中转清算**
 *     （备件已由 mnt MAINTENANCE_ISSUE 贷记 1403，assets 不再贷存货改贷中转清算防双重扣减）。
 *
 * 本 spec 承接 2256-2 CAPITALIZE successor：CAPITALIZE 路径 linkedVisit 分支（0849-3 Phase 2 落地）：
 *   - `MaintenanceCapitalizationPostingDispatcher.buildEvent:79,84,87`：linkedVisit 标志计算 + 透传 +
 *     `BILL_DATA_MAINTENANCE_CLEARING_SUBJECT_CODE = "2502"` 透传（CAPITALIZE 之前不透传，本 plan 补加）。
 *   - `MaintenanceCapitalizationAcctDocProvider.createFacts:46-69`（本 plan 扩展）：
 *       linkedVisit=true  → Dr 1601 / **Cr 2502 维修中转清算**（备件已由 mnt 出库，不再贷银行存款）。
 *       linkedVisit=false → Dr 1601 / Cr 1002 银行存款（既有独立 CAPITALIZE 路径无回归，对齐 0215-1/0742-1）。
 *
 * 跨域并存防双重扣减语义（CAPITALIZE 视角）：
 *   - mnt `MAINTENANCE_ISSUE`：Dr 6602 / Cr 1403 存货（备件实物出库，50）。
 *   - assets `MAINTENANCE_CAPITALIZATION`（linkedVisit=true）：Dr 1601 固定资产原值增量 / **Cr 2502 中转清算**
 *     （**非 Cr 1002 银行存款**——资本化备件成本已由 mnt 出库，无银行实际付出，会计实质一致性）。
 *   备件 50 元成本仅由 mnt 一次贷记 1403 存货；assets 改贷 2502 中转清算避免虚增银行付出。
 *
 * 对照（测试 2，linkedVisit=false）：assets 独立资本化维修无关联 visit → Dr 1601 / Cr 1002 银行存款
 * （既有路径，与 0215-1 `ast-maintenance.action.spec.ts` CAPITALIZE 路径一致，验证 0 回归）。
 *
 * Phase 1 Decision（Explore 裁定）：
 *   - **Decision 1 (a) 引入分支**：会计实质一致性（避免虚增银行付出）+ EXPENSE 范式对齐 + dispatcher 已透传
 *     linkedVisit 字段（激活 dead code）。
 *   - **Decision 2 (a) 无 config-gate**：对齐 EXPENSE Provider 实际代码范式（`MaintenanceExpenseAcctDocProvider:60-64`
 *     无条件分支不读 config；doc-vs-code drift 是 pre-existing bug 超出本计划范围）。
 *
 * 确定性值：
 *   测试 1（linkedVisit=true CAPITALIZE 正路径）：备件 qty=10 × unitCost=5 → amount=50；
 *     mnt MAINTENANCE_ISSUE Dr 6602=50 / Cr 1403=50；
 *     assets MaintenanceCost SPARE_PART=50 → totalCost=50 → CAPITALIZE capitalizedAmount=50
 *     → MAINTENANCE_CAPITALIZATION Dr 1601=50 / Cr 2502=50；
 *     资产原值增量（1000+50=1050）；红冲回退（1050-50=1000）+ 红字凭证 Dr 1601=-50/Cr 2502=-50。
 *   测试 2（linkedVisit=false CAPITALIZE 对照）：assets MaintenanceCost SPARE_PART=80 → totalCost=80
 *     → CAPITALIZE capitalizedAmount=80 → MAINTENANCE_CAPITALIZATION Dr 1601=80 / Cr 1002=80
 *     （既有路径无回归，与 0215-1 CAPITALIZE 路径一致）。
 *
 * 自包含隔离：两测试各自建测试资产（CAPITALIZE 修改资产原值 + 折旧重算，避开种子污染）+ 唯一维修 code。
 *
 * 清理（依赖反向顺序，两测试各自 try/finally 独立隔离）：
 *   测试 1：assets 凭证（NORMAL+REVERSAL 共用 billHeadCode=maintenance.code）
 *     → 折旧计划（recalculate 生成 PENDING 行）→ 费用行 → 维修工单 → 测试资产
 *     → mnt 凭证（billHeadCode=usage.code+"-MI"）→ mnt OUTGOING 移动（relatedBillType=ERP_MNT_SPARE_PART+relatedBillCode=usage.code）
 *     → mnt UsageLine+Usage+Visit → StockLedger/StockBalance 按测试物料 materialId+warehouseId
 *     → INCOMING 备货移动（lines+move）→ 测试物料。
 *   测试 2：assets 凭证 → 折旧计划 → 费用行 → 维修工单 → 测试资产。
 */

const ORG = 2;
const WH = 2;            // WH-RAW
const UOM = 1;           // PCS
const CURRENCY = 1;
const ACCT_SCHEMA = 1;   // ACCT-FIN-01
const EQUIPMENT_ID = 1;  // EQ-2026-001 种子设备 RUNNING
const ASSIGNED_TO = 2;   // 种子员工
const CATEGORY_ID = 1;   // AST-CAT-IT 种子类别（无 subjectId → dispatcher 默认 1601）
const BDATE = '2026-07-10';
const VISIT_DATE = '2026-12-25';
const MOVE_REQ_TYPE = 'i_app_erp_inv_biz_StockMoveRequest';

// 测试 1 确定性派生值
const SETUP_QTY_1 = 20;
const SETUP_UNIT_COST_1 = 5;
const USAGE_QTY_1 = 10;
const USAGE_UNIT_COST_1 = 5;
const USAGE_AMOUNT_1 = USAGE_QTY_1 * USAGE_UNIT_COST_1; // 50
// 测试 1 资产初始原值（与 0215-1 CAPITALIZE 测试对齐，便于红冲回退断言）
const ASSET_ORIGINAL_VALUE_1 = 1000;
// 测试 2 独立金额（无 mnt 备件出库关联，纯 assets 独立资本化维修）
const ASSET_COST_2 = 80;
const ASSET_ORIGINAL_VALUE_2 = 1000;

let _seq = 0;
function uniq(tag: string): string {
  _seq += 1;
  return `${tag}-${Date.now()}-${_seq}`;
}

test.describe('maintenance × assets linked-visit CAPITALIZE anti-double-deduct (cross-domain posting branch)', () => {
  test('linkedVisit=true: mnt MAINTENANCE_ISSUE Dr6602/Cr1403 + assets MAINTENANCE_CAPITALIZATION Dr1601/Cr2502 (clearing, not 1002 bank)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstMaintenance-main');

    const ts = Date.now();

    // 1. 测试专用备件物料（RAW_MATERIAL, MOVING_AVERAGE, 无种子余额 → 确定性 unitCost + 清理安全）
    const material = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code: uniq('E2E-LVC-MAT'), name: 'E2E Linked-Visit Capitalize Spare Part Material',
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
          remark: `E2E-LVC-SEED-${ts}`,
        }),
      },
      'id code',
    );
    await callMutationOk(page, 'ErpInvStockMove', 'complete', { moveId: setupMove.id }, 'id docStatus');

    // 3. mnt DRAFT Visit（仅作 maintenanceVisitId 软 FK 持有，linkedVisit 判定只看 != null）
    const visit = await createViaSave(
      page, 'ErpMntVisit',
      {
        code: uniq('E2E-LVC-VIS'), equipmentId: EQUIPMENT_ID, visitDate: VISIT_DATE,
        status: 'DRAFT', assignedTo: ASSIGNED_TO, visitType: 'PLANNED', orgId: ORG,
      },
      'id',
    );

    // 4. mnt SparePartUsage（visitId 显式指向 visit）+ Line（qty=10 × unitCost=5 → amount=50）
    const usage = await createViaSave(
      page, 'ErpMntSparePartUsage',
      {
        code: uniq('E2E-LVC-SPU'), orgId: ORG, equipmentId: EQUIPMENT_ID, warehouseId: WH,
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

    // 5. 自包含建测试资产（CAPITALIZE 修改原值，避开种子污染，对齐 0215-1 CAPITALIZE 范式）
    const assetCode = uniq('E2E-LVC-AST');
    const asset = await createViaSave(
      page, 'ErpAstAsset',
      {
        code: assetCode,
        name: `E2E 资本化关联维护测试资产 ${assetCode}`,
        orgId: ORG,
        categoryId: CATEGORY_ID,
        acquisitionDate: '2026-06-15',
        currencyId: CURRENCY,
        originalValue: ASSET_ORIGINAL_VALUE_1,
        residualValue: 0,
        depreciationMethod: 'STRAIGHT_LINE',
        usefulLifeMonths: 36,
        status: 'IN_SERVICE',
        accumulatedDepreciation: 0,
        netBookValue: ASSET_ORIGINAL_VALUE_1,
      },
      'id originalValue netBookValue',
    );

    // 6. assets Maintenance（**maintenanceVisitId=visit.id 触发 linkedVisit=true**）+ Cost 行（SPARE_PART=50）
    const mntCode = uniq('E2E-LVC-AST-MNT');
    const mnt = await callMutationOk(
      page, 'ErpAstMaintenance', 'createMaintenance',
      {
        assetId: asset.id, code: mntCode, name: `E2E 跨域资本化防双重扣减 ${mntCode}`,
        businessDate: BDATE, maintenanceVisitId: visit.id, reason: 'E2E CAPITALIZE linkedVisit=true',
      },
      'id code status',
    );
    expect(mnt.status, 'createMaintenance should set status=DRAFT').toBe('DRAFT');

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

      // decideTreatment(CAPITALIZE)，capitalizedAmount 默认=totalCost=50
      const decided = await callMutationOk(
        page, 'ErpAstMaintenance', 'decideTreatment',
        { id: mnt.id, treatment: 'CAPITALIZE' },
        'id treatment capitalizedAmount totalCostAmount',
      );
      expect(decided.treatment, 'treatment=CAPITALIZE').toBe('CAPITALIZE');
      expect(Number(decided.capitalizedAmount), 'capitalizedAmount=50').toBe(USAGE_AMOUNT_1);
      expect(Number(decided.totalCostAmount), 'totalCost=SPARE_PART 50').toBe(USAGE_AMOUNT_1);

      await callMutationOk(page, 'ErpAstMaintenance', 'approve', { id: mnt.id }, 'id status');

      // post → POSTED + 资产原值增量 + MAINTENANCE_CAPITALIZATION 凭证
      const posted = await callMutationOk(
        page, 'ErpAstMaintenance', 'post', { id: mnt.id }, 'id status posted',
      );
      expect(posted.status, 'post should transition COMPLETED → POSTED').toBe('POSTED');
      expect(posted.posted, 'MAINTENANCE_CAPITALIZATION posting → posted=true').toBe(true);

      // 资产原值增量断言（1000 + 50 = 1050）经 __get 独立核实（对齐 0215-1/0742-1 范式）
      const assetAfterPost = await verifyState(page, 'ErpAstAsset', asset.id, 'originalValue netBookValue');
      expect(Number(assetAfterPost.originalValue), 'asset originalValue=1000+50').toBe(ASSET_ORIGINAL_VALUE_1 + USAGE_AMOUNT_1);
      expect(Number(assetAfterPost.netBookValue), 'asset netBookValue=1000+50').toBe(ASSET_ORIGINAL_VALUE_1 + USAGE_AMOUNT_1);

      // assets MAINTENANCE_CAPITALIZATION 正向凭证行精确数值断言（0849-3 落地核心）：
      // **防双重扣减 CAPITALIZE 分支** — linkedVisit=true → Dr 1601 固定资产=50 / **Cr 2502 维修中转清算=50（非 Cr 1002 银行存款）**
      // 备件成本已由 mnt MAINTENANCE_ISSUE 贷记 1403，assets 资本化改贷 2502 中转清算避免虚增银行付出。
      const astVoucherId = await findVoucherIdByBillCode(page, mntCode, 'NORMAL');
      await assertVoucherLines(page, astVoucherId, [
        { subjectCode: '1601', dcDirection: 'DEBIT', debitAmount: USAGE_AMOUNT_1, creditAmount: 0 },
        { subjectCode: '2502', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: USAGE_AMOUNT_1 },
      ]);

      // reverse → COMPLETED + 资产原值回退 + 红冲凭证
      const reversed = await callMutationOk(
        page, 'ErpAstMaintenance', 'reverse', { id: mnt.id }, 'id status posted reversed',
      );
      expect(reversed.status, 'reverse should transition POSTED → COMPLETED').toBe('COMPLETED');
      expect(reversed.posted, 'reverse should clear posted=false').toBe(false);
      expect(reversed.reversed, 'reverse should set reversed=true').toBe(true);

      // 资产原值回退断言（1050 - 50 = 1000）
      const assetAfterReverse = await verifyState(page, 'ErpAstAsset', asset.id, 'originalValue netBookValue');
      expect(Number(assetAfterReverse.originalValue), 'reverse should rollback originalValue=1000').toBe(ASSET_ORIGINAL_VALUE_1);
      expect(Number(assetAfterReverse.netBookValue), 'reverse should rollback netBookValue=1000').toBe(ASSET_ORIGINAL_VALUE_1);

      // MAINTENANCE_CAPITALIZATION 红冲凭证行断言（对齐 0742-1 范式）：REVERSAL 同向取负
      // Dr 1601=-50 / Cr 2502=-50（红字凭证同向取负：dcDirection 不变，金额取负）
      const astReversalVoucherId = await findVoucherIdByBillCode(page, mntCode, 'REVERSAL');
      await assertVoucherLines(page, astReversalVoucherId, [
        { subjectCode: '1601', dcDirection: 'DEBIT', debitAmount: -USAGE_AMOUNT_1, creditAmount: 0 },
        { subjectCode: '2502', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -USAGE_AMOUNT_1 },
      ]);
    } finally {
      // ---- 清理（依赖反向顺序）----
      // 1. assets MAINTENANCE_CAPITALIZATION 凭证（NORMAL+REVERSAL 共用 billHeadCode=mntCode）
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
      // 4. assets 折旧计划（recalculate 生成 PENDING 行）→ 费用行 → 维修工单 → 测试资产
      await deleteByFilter(page, 'ErpAstDepreciationSchedule', eqFilter('assetId', asset.id));
      await deleteById(page, 'ErpAstMaintenanceCost', costRow.id);
      await deleteById(page, 'ErpAstMaintenance', mnt.id);
      await deleteById(page, 'ErpAstAsset', asset.id);
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

  test('linkedVisit=false 对照: assets MAINTENANCE_CAPITALIZATION Dr1601/Cr1002 bank (independent capitalize, no visit link)', async ({ page }) => {
    await loginAndNavigate(page, '/ErpAstMaintenance-main');

    // 自包含建测试资产（CAPITALIZE 修改原值，避开种子污染，对齐 0215-1 CAPITALIZE 范式）
    const assetCode = uniq('E2E-IND-CAP-AST');
    const asset = await createViaSave(
      page, 'ErpAstAsset',
      {
        code: assetCode,
        name: `E2E 资本化独立维修对照资产 ${assetCode}`,
        orgId: ORG,
        categoryId: CATEGORY_ID,
        acquisitionDate: '2026-06-15',
        currencyId: CURRENCY,
        originalValue: ASSET_ORIGINAL_VALUE_2,
        residualValue: 0,
        depreciationMethod: 'STRAIGHT_LINE',
        usefulLifeMonths: 36,
        status: 'IN_SERVICE',
        accumulatedDepreciation: 0,
        netBookValue: ASSET_ORIGINAL_VALUE_2,
      },
      'id originalValue netBookValue',
    );

    // assets 独立资本化维修：createMaintenance 不传 maintenanceVisitId → linkedVisit=false
    const mntCode = uniq('E2E-IND-CAP-MNT');
    const mnt = await callMutationOk(
      page, 'ErpAstMaintenance', 'createMaintenance',
      {
        assetId: asset.id, code: mntCode, name: `E2E 独立资本化维修对照 ${mntCode}`,
        businessDate: BDATE, reason: 'E2E CAPITALIZE linkedVisit=false',
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
        { id: mnt.id, treatment: 'CAPITALIZE' },
        'id treatment capitalizedAmount totalCostAmount',
      );
      expect(decided.treatment, 'treatment=CAPITALIZE').toBe('CAPITALIZE');
      expect(Number(decided.capitalizedAmount), 'capitalizedAmount=80').toBe(ASSET_COST_2);
      expect(Number(decided.totalCostAmount), 'totalCost=SPARE_PART 80').toBe(ASSET_COST_2);

      await callMutationOk(page, 'ErpAstMaintenance', 'approve', { id: mnt.id }, 'id status');

      const posted = await callMutationOk(
        page, 'ErpAstMaintenance', 'post', { id: mnt.id }, 'id status posted',
      );
      expect(posted.status, 'post should transition COMPLETED → POSTED').toBe('POSTED');
      expect(posted.posted, 'MAINTENANCE_CAPITALIZATION posting → posted=true').toBe(true);

      // 资产原值增量断言（1000 + 80 = 1080）
      const assetAfterPost = await verifyState(page, 'ErpAstAsset', asset.id, 'originalValue netBookValue');
      expect(Number(assetAfterPost.originalValue), 'asset originalValue=1000+80').toBe(ASSET_ORIGINAL_VALUE_2 + ASSET_COST_2);
      expect(Number(assetAfterPost.netBookValue), 'asset netBookValue=1000+80').toBe(ASSET_ORIGINAL_VALUE_2 + ASSET_COST_2);

      // assets MAINTENANCE_CAPITALIZATION 凭证行精确数值断言（既有独立路径回归）：
      // linkedVisit=false → Dr 1601 固定资产=80 / Cr 1002 银行存款=80（独立资本化维修既有路径无回归，
      // 与 0215-1 `ast-maintenance.action.spec.ts` CAPITALIZE 路径一致）
      const astVoucherId = await findVoucherIdByBillCode(page, mntCode, 'NORMAL');
      await assertVoucherLines(page, astVoucherId, [
        { subjectCode: '1601', dcDirection: 'DEBIT', debitAmount: ASSET_COST_2, creditAmount: 0 },
        { subjectCode: '1002', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: ASSET_COST_2 },
      ]);
    } finally {
      // 清理：assets 凭证 → 折旧计划 → 费用行 → 维修工单 → 测试资产
      await cleanupVoucherByBillCode(page, mntCode);
      await deleteByFilter(page, 'ErpAstDepreciationSchedule', eqFilter('assetId', asset.id));
      await deleteById(page, 'ErpAstMaintenanceCost', costRow.id);
      await deleteById(page, 'ErpAstMaintenance', mnt.id);
      await deleteById(page, 'ErpAstAsset', asset.id);
    }
  });
});
