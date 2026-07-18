import {
  test,
  expect,
  loginAndNavigate,
  createViaSave,
  callMutation,
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
 * maintenance ErpMntSparePartUsage reverseConfirm + MAINTENANCE_ISSUE 凭证红冲浏览器层 E2E
 * （plan 2026-07-18-1745-1 Phase 3）。
 *
 * 验证 reverseConfirm 经 GraphQL /graphql 的全栈可达 + MAINTENANCE_ISSUE(492) 红冲凭证行数值断言：
 *   reverseConfirm(usageId) @BizMutation → validateCanReverse（posted=true + ACTIVE 守卫）
 *   → issuePostingDispatcher.reverseIssue（红冲 MAINTENANCE_ISSUE 凭证）
 *   → stockMoveBiz.reverse（反向 OUTGOING 移动单，REVERSAL relatedBillType）
 *   → doReverseConfirm（posted=false + docStatus=CANCELLED）。
 *
 * 权威实现（ErpMntSparePartUsageBizModel.reverseConfirm）：
 *   requireUsage → validateCanReverse（守卫 ERR_SPARE_PART_USAGE_NOT_POSTED）
 *   → try { reverseIssue(usage) } catch LOG.warn（GL 红冲失败吞异常保持幂等）
 *   → findIssueMove → try { stockMoveBiz.reverse(moveId) } catch LOG.warn（库存反向失败吞异常保持幂等）
 *   → reload usage → doReverseConfirm（posted=false + docStatus=CANCELLED）
 *
 * MAINTENANCE_ISSUE 红字凭证（IErpFinVoucherBiz.reverse → ErpFinPostingProcessor.reverseProcess）：
 *   原凭证 isReversed=true + 新建红字凭证 postingType=REVERSAL + 行同向取负（Dr 6602=-50 / Cr 1403=-50）
 *   + 红字凭证与原凭证共用 billHeadCode（voucher_bill_r 回链反查区分 postingType）。
 *
 * 库存反向移动单（IErpInvStockMoveBiz.reverse → ErpInvStockMoveProcessor.reverse）：
 *   DONE 移动单 → 生成 REVERSAL 反向冲销移动单（relatedBillType=REVERSAL + relatedBillCode=原移动单 code）。
 *
 * 确定性值（与 mnt-spare-part-posting.action.spec.ts 一致）：
 *   备货 INCOMING: qty=20, unitCost=5
 *   UsageLine: qty=10, unitCost=5, amount=50 → Dr 6602=50 / Cr 1403=50
 *   红冲：Dr 6602=-50 / Cr 1403=-50
 *
 * 清理：MAINTENANCE_ISSUE 凭证（原+红字）+ OUTGOING 移动 + REVERSAL 反向移动 + StockLedger + StockBalance
 *   + INCOMING 备货移动 + UsageLine + Usage + 测试物料。
 */

const ORG = 2;
const WH = 2; // WH-RAW
const UOM = 1; // PCS
const CURRENCY = 1;
const ACCT_SCHEMA = 1;
const EQUIPMENT_ID = 1;
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

test.describe('maintenance ErpMntSparePartUsage reverseConfirm + MAINTENANCE_ISSUE voucher reversal assertion', () => {
  test('confirm→reverseConfirm triggers MI voucher reversal + reversal stock move + posted=false', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMntSparePartUsage-main');

    const ts = Date.now();

    // 1. 测试专用备件物料（隔离避免污染 inventory dashboard totalValue 基线）
    const material = await createViaSave(
      page, 'ErpMdMaterial',
      {
        code: uniq('E2E-MNT-MAT'), name: 'E2E Maintenance Spare Part Material',
        materialType: 'RAW_MATERIAL', uoMId: UOM, status: 'ACTIVE',
        costMethod: 'MOVING_AVERAGE', defaultWarehouseId: WH,
      },
      'id',
    );

    // 2. 前置备货：INCOMING 建库存余量
    const setupMove = await callMutationOk(
      page, 'ErpInvStockMove', 'generateMove',
      {
        request: input(MOVE_REQ_TYPE, {
          moveType: 'INCOMING', orgId: ORG, businessDate: BDATE,
          destWarehouseId: WH, acctSchemaId: ACCT_SCHEMA, currencyId: CURRENCY,
          lines: [{ materialId: material.id, uoMId: UOM, quantity: SETUP_QTY, unitCost: SETUP_UNIT_COST, currencyId: CURRENCY }],
          remark: `E2E-MNT-RV-SEED-${ts}`,
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
      'id code',
    );
    const usageLine = await createViaSave(
      page, 'ErpMntSparePartUsageLine',
      {
        sparePartUsageId: usage.id, lineNo: 1, materialId: material.id, uoMId: UOM,
        quantity: USAGE_QTY, unitCost: USAGE_UNIT_COST, amount: USAGE_AMOUNT,
      },
      'id',
    );

    try {
      // 4. 前置：confirm 产 MAINTENANCE_ISSUE 凭证 + OUTGOING 移动 + posted=true
      const confirmed = await callMutationOk(
        page, 'ErpMntSparePartUsage', 'confirm', { usageId: usage.id },
        'id docStatus posted',
      );
      expect(confirmed.docStatus, '前置 confirm: docStatus=ACTIVE').toBe('ACTIVE');
      expect(confirmed.posted, '前置 confirm: posted=true').toBe(true);

      const originalVoucherId = await findVoucherIdByBillCode(page, usage.code + '-MI', 'NORMAL');
      expect(originalVoucherId, '前置：confirm 应生成 MAINTENANCE_ISSUE NORMAL 凭证').toBeTruthy();

      const originalMove = await findFirst<any>(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_MNT_SPARE_PART'), eqFilter('relatedBillCode', usage.code)),
        'id code docStatus',
      );
      expect(originalMove, '前置：confirm 应生成 OUTGOING 移动单').toBeTruthy();
      expect(originalMove.docStatus, '前置 OUTGOING: docStatus=DONE').toBe('DONE');

      // 5. 执行 reverseConfirm
      const reversed = await callMutationOk(
        page, 'ErpMntSparePartUsage', 'reverseConfirm', { usageId: usage.id },
        'id docStatus posted',
      );
      expect(reversed.docStatus, 'reverseConfirm: docStatus=CANCELLED').toBe('CANCELLED');
      expect(reversed.posted, 'reverseConfirm: posted=false').toBe(false);

      // __get 权威确认状态翻转
      const finalUsage = await verifyState(page, 'ErpMntSparePartUsage', usage.id, 'docStatus posted');
      expect(finalUsage.docStatus, '__get: docStatus=CANCELLED').toBe('CANCELLED');
      expect(finalUsage.posted, '__get: posted=false').toBe(false);

      // 6. 原 MAINTENANCE_ISSUE 凭证 isReversed=true
      const originalAfter = await findFirst<any>(
        page, 'ErpFinVoucher', eqFilter('id', Number(originalVoucherId)), 'id isReversed postingType',
      );
      expect(originalAfter?.isReversed, '原 MAINTENANCE_ISSUE 凭证应被标记 isReversed=true').toBe(true);

      // 7. 红字凭证存在 + 行同向取负断言
      const reversalVoucherId = await findVoucherIdByBillCode(page, usage.code + '-MI', 'REVERSAL');
      expect(reversalVoucherId, '应存在 MAINTENANCE_ISSUE 红字冲销凭证').toBeTruthy();

      await assertVoucherLines(page, reversalVoucherId, [
        // 红字凭证行：dcDirection 不变，金额取负
        { subjectCode: '6602', dcDirection: 'DEBIT', debitAmount: -USAGE_AMOUNT, creditAmount: 0 },
        { subjectCode: '1403', dcDirection: 'CREDIT', debitAmount: 0, creditAmount: -USAGE_AMOUNT },
      ]);

      // 8. 反向 OUTGOING 移动单（REVERSAL 移动单，relatedBillType=REVERSAL + relatedBillCode=原移动单 code）
      const reversalMove = await findFirst<any>(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'REVERSAL'), eqFilter('relatedBillCode', originalMove.code)),
        'id code docStatus relatedBillType relatedBillCode',
      );
      expect(reversalMove, '应存在 REVERSAL 反向冲销移动单').toBeTruthy();
      expect(reversalMove.relatedBillType, 'REVERSAL 移动单 relatedBillType=REVERSAL').toBe('REVERSAL');
      expect(reversalMove.relatedBillCode, 'REVERSAL 移动单 relatedBillCode 指向原移动单 code')
        .toBe(originalMove.code);
    } finally {
      // ---- 清理（依赖反向顺序）----
      // 1. MAINTENANCE_ISSUE 凭证（原+红字共用 billHeadCode，cleanupVoucherByBillCode 全量清理）
      await cleanupVoucherByBillCode(page, usage.code + '-MI');
      // 2. OUTGOING 移动 + REVERSAL 反向移动（清理 REVERSAL 移动先，因 relatedBillCode 引用原 code）
      const issueMove = await findFirst<any>(
        page, 'ErpInvStockMove',
        andFilter(eqFilter('relatedBillType', 'ERP_MNT_SPARE_PART'), eqFilter('relatedBillCode', usage.code)),
        'id code',
      );
      if (issueMove) {
        // REVERSAL 反向移动单
        const reversalMove = await findFirst<any>(
          page, 'ErpInvStockMove',
          andFilter(eqFilter('relatedBillType', 'REVERSAL'), eqFilter('relatedBillCode', issueMove.code)),
          'id',
        );
        if (reversalMove) {
          await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('moveId', Number(reversalMove.id)));
          await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(reversalMove.id)));
          await deleteById(page, 'ErpInvStockMove', reversalMove.id);
        }
        // 原 OUTGOING 移动
        await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('moveId', Number(issueMove.id)));
        await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(issueMove.id)));
        await deleteById(page, 'ErpInvStockMove', issueMove.id);
      }
      // 3. UsageLine + Usage
      await deleteById(page, 'ErpMntSparePartUsageLine', usageLine.id);
      await deleteById(page, 'ErpMntSparePartUsage', usage.id);
      // 4. StockLedger + StockBalance 按物料
      await deleteByFilter(page, 'ErpInvStockLedger', eqFilter('materialId', Number(material.id)));
      await deleteByFilter(page, 'ErpInvStockBalance', andFilter(eqFilter('materialId', Number(material.id)), eqFilter('warehouseId', WH)));
      // 5. INCOMING 备货移动
      await deleteByFilter(page, 'ErpInvStockMoveLine', eqFilter('moveId', Number(setupMove.id)));
      await deleteById(page, 'ErpInvStockMove', setupMove.id);
      // 6. 测试物料
      await deleteById(page, 'ErpMdMaterial', material.id);
    }
  });

  test('reverseConfirm on unposted usage → ERR_SPARE_PART_USAGE_NOT_POSTED guard', async ({ page }) => {
    await loginAndNavigate(page, '/ErpMntSparePartUsage-main');

    // 建 DRAFT SparePartUsage（未 confirm → posted=false）
    const usage = await createViaSave(
      page, 'ErpMntSparePartUsage',
      {
        code: uniq('E2E-MNT-SPU-REJ'), orgId: ORG, equipmentId: EQUIPMENT_ID, warehouseId: WH,
        businessDate: BDATE, docStatus: 'DRAFT', approveStatus: 'UNSUBMITTED', posted: false,
      },
      'id code',
    );

    try {
      // 未 confirm 直接 reverseConfirm → 守卫拒绝（data=null + errors 非 null）
      const resp = await callMutation(page, 'ErpMntSparePartUsage', 'reverseConfirm', { usageId: usage.id }, 'id');
      expect(resp.errors, '未过账消耗单 reverseConfirm 应被守卫拒绝（errors 非空）').toBeTruthy();
      expect(resp.data, '未过账消耗单 reverseConfirm 应被守卫拒绝（data=null）').toBeNull();
      // 错误码经 errors 字段携带（Nop 此配置仅回 i18n message 不序列化 extensions.errorCode，断言语义 token 即可）
      const errMsg = JSON.stringify(resp.errors ?? '');
      expect(errMsg.includes('未过账') || errMsg.includes('不可红冲') || errMsg.includes('not-posted'),
        `errors 应含语义 token，实际: ${errMsg}`).toBe(true);

      // 状态不变（守卫前置）
      const unchanged = await verifyState(page, 'ErpMntSparePartUsage', usage.id, 'docStatus posted');
      expect(unchanged.docStatus, '守卫拒绝后 docStatus 保持 DRAFT').toBe('DRAFT');
      expect(unchanged.posted, '守卫拒绝后 posted 保持 false').toBe(false);
    } finally {
      await deleteById(page, 'ErpMntSparePartUsage', usage.id);
    }
  });
});
