package io.nop.app.all.it;

import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.dao.entity.ErpMfgCostVariance;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * B4 C07：制造工单全生命周期与完工过账（按 {@code docs/design/integration-testing.md §6 C07} 规格）。
 *
 * <p>全链（自包含建数，引用部署 seed：组织 2 / WH-RAW 仓库 2 / WH-MAIN 仓库 1 / CNY 币种 1 /
 * 2026-07 OPEN 期间 + 科目 1401/1411/1410）：自包含材料 P（FIFO 产成品）+ M1（MOVING_AVERAGE 原料）
 * → 自包含 BOM（1 P = 2 M1）→ 自包含 M1 入库（10 @ 5，`ErpInvStockMove__generateMove`）→ 自包含工单
 * （产品 P，plannedQuantity=1）save → submitForApproval → approve（DIRECT）→ checkAvailability →
 * start → 自包含领料单 + 行（M1×2）→ {@code ErpMfgMaterialIssue__confirm}（领料出库独立步骤，
 * MANUFACTURING_ISSUE 凭证 Dr 1411/Cr 1401=10）→ 自包含 FIRMED 标准成本（materialCost=8/unit）→
 * {@code ErpMfgWorkOrder__reportCompletion}（completedQty=1 驱动完工入库 + MANUFACTURING_RECEIPT 过账
 * Dr 1401/Cr 1411=10 + config 门控差异自动计算 + PRODUCTION_VARIANCE 过账）→
 * {@code ErpMfgCostVariance__findPage} 差异断言（varianceAmount = actual − standard = 10 − 8 = +2）。
 *
 * <p>Phase 1 Decision（完工驱动动作裁决，落盘设计文档 §6 C07 勘误）：设计文档 §6 C07 步骤 2 原述
 * {@code ErpMfgWorkOrder__close（完工：完工入库 + 完工过账凭证）} 为漂移——实仓
 * {@code ErpMfgWorkOrderBizModel} 三动作 start/close/reportCompletion 中，完工入库 + 完工过账由
 * {@code reportCompletion}（completedQty 驱动，经 {@code ErpMfgWorkOrderReportCompletionProcessor} →
 * {@code generateCompletionMove} → MANUFACTURING_RECEIPT 过账）驱动；{@code close} 为 STOPPED/IN_PROCESS→CLOSED
 * 结案语义（{@code ErpMfgWorkOrderCloseProcessor}），不驱动完工过账。另设计文档 §6 C07 步骤 2「reportCompletion
 * （报工，触发领料出库）」亦为漂移——领料出库由独立 {@code ErpMfgMaterialIssue__confirm} 步骤驱动
 * （B3 closure MINOR-3 同型修正）。动作名以当前实现为准，用例按实仓落地。
 *
 * <p>数据来源裁决：种子材料 MAT-003（WEIGHTED_AVERAGE）余额行无 location，领料出库路径
 * （location 默认 sourceWarehouseId）无法匹配 → 空白余额，成本为零。按 C05/C06 先例 + 「自包含建数」纪律，
 * 材料/BOM/库存全部自包含（引用 seed 组织/仓库/币种/期间/科目），未触发 seed 修正授权。
 *
 * <p>三层验证：层 1 = JUnit 关键断言（状态机 APPROVED→IN_PROCESS→COMPLETED、完工入库 stock_move、
 * 完工过账凭证借贷平衡、cost_variance.varianceAmount = actual − standard）；层 2 = 每步 response 快照；
 * 层 3 = output/tables 变更行。RECORDING→CHECKING 往返已按 M0.2 试点范式完成（录制后切回默认 CHECKING
 * 复跑全绿）。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
@NopTestProperty(name = "erp-mfg.variance-auto-calc-enabled", value = "true")
public class TestErpC07MfgWorkOrderLifecycle extends ErpIntegrationTestCase {

    static final String WO_CODE = "IT-C07-WO-001";
    static final String ISSUE_CODE = "IT-C07-MI-001";
    static final BigDecimal ISSUE_TOTAL = new BigDecimal("10");     // 2 × M1 avg 5
    static final BigDecimal STD_MATERIAL_PER_UNIT = new BigDecimal("8");
    static final BigDecimal MATERIAL_VARIANCE = new BigDecimal("2"); // 10 − 8

    @RegisterExtension
    static C07FrozenClockExtension frozenClock = new C07FrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;

    @Test
    public void testMfgWorkOrderLifecycle() {
        // ---------- 1. 自包含材料 P（FIFO 产成品）+ M1（MOVING_AVERAGE 原料） ----------
        ApiResponse<?> matPSave = rpcMutation("ErpMdMaterial__save", request("1_material_p_save.json5", Map.class));
        output("1_material_p_save_response.json5", matPSave);
        assertEquals(0, matPSave.getStatus(), "产成品物料保存应成功");
        String pId = idOf(matPSave);
        addVar("pId", pId);

        ApiResponse<?> matM1Save = rpcMutation("ErpMdMaterial__save", request("2_material_m1_save.json5", Map.class));
        output("2_material_m1_save_response.json5", matM1Save);
        assertEquals(0, matM1Save.getStatus(), "原料物料保存应成功");
        String m1Id = idOf(matM1Save);
        addVar("m1Id", m1Id);

        // ---------- 2. 自包含 BOM（1 P = 2 M1） ----------
        ApiResponse<?> bomSave = rpcMutation("ErpMfgBom__save", request("3_bom_save.json5", Map.class));
        output("3_bom_save_response.json5", bomSave);
        assertEquals(0, bomSave.getStatus(), "BOM 保存应成功");
        String bomId = idOf(bomSave);
        addVar("bomId", bomId);

        ApiResponse<?> bomLine = rpcMutation("ErpMfgBomLine__save", request("4_bom_line_save.json5", Map.class));
        output("4_bom_line_save_response.json5", bomLine);
        assertEquals(0, bomLine.getStatus(), "BOM 行保存应成功");

        // ---------- 3. 自包含 M1 入库（10 @ 5） ----------
        ApiResponse<?> incomingMove = rpcMutation("ErpInvStockMove__generateMove", request("5_m1_incoming_move.json5", Map.class));
        output("5_m1_incoming_move_response.json5", incomingMove);
        assertEquals(0, incomingMove.getStatus(), "M1 入库移动应成功");

        // ---------- 4. 自包含工单 save → submitForApproval → approve（DIRECT） ----------
        ApiResponse<?> woSave = rpcMutation("ErpMfgWorkOrder__save", request("6_wo_save.json5", Map.class));
        output("6_wo_save_response.json5", woSave);
        assertEquals(0, woSave.getStatus(), "工单保存应成功");
        String woId = idOf(woSave);
        addVar("woId", woId);

        ApiResponse<?> inputLine = rpcMutation("ErpMfgWorkOrderLine__save", request("7_wo_line_input_save.json5", Map.class));
        output("7_wo_line_input_save_response.json5", inputLine);
        assertEquals(0, inputLine.getStatus(), "工单投入行保存应成功");
        addVar("woLineId", idOf(inputLine));

        ApiResponse<?> outputLine = rpcMutation("ErpMfgWorkOrderLine__save", request("8_wo_line_output_save.json5", Map.class));
        output("8_wo_line_output_save_response.json5", outputLine);
        assertEquals(0, outputLine.getStatus(), "工单产出行保存应成功");

        ApiResponse<?> woSubmit = rpcMutation("ErpMfgWorkOrder__submitForApproval", request("9_wo_submit.json5", Map.class));
        output("9_wo_submit_response.json5", woSubmit);
        assertEquals(0, woSubmit.getStatus(), "工单提交审批应成功");

        ApiResponse<?> woApprove = rpcMutation("ErpMfgWorkOrder__approve", request("10_wo_approve.json5", Map.class));
        output("10_wo_approve_response.json5", woApprove);
        assertEquals(0, woApprove.getStatus(), "工单审批应成功");
        ErpMfgWorkOrder approvedWo = reload(ErpMfgWorkOrder.class, woId);
        assertEquals(ErpMfgConstants.APPROVE_STATUS_APPROVED, approvedWo.getApproveStatus(), "工单 approveStatus=APPROVED");

        // ---------- 5. checkAvailability → start（开工） ----------
        ApiResponse<?> woCheck = rpcMutation("ErpMfgWorkOrder__checkAvailability", request("11_wo_check_availability.json5", Map.class));
        output("11_wo_check_availability_response.json5", woCheck);
        assertEquals(0, woCheck.getStatus(), "齐套校验应成功");
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED,
                reload(ErpMfgWorkOrder.class, woId).getDocStatus(), "齐套校验后 docStatus=STOCK_RESERVED");

        ApiResponse<?> woStart = rpcMutation("ErpMfgWorkOrder__start", request("12_wo_start.json5", Map.class));
        output("12_wo_start_response.json5", woStart);
        assertEquals(0, woStart.getStatus(), "开工应成功");
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS,
                reload(ErpMfgWorkOrder.class, woId).getDocStatus(), "开工后 docStatus=IN_PROCESS");

        // ---------- 6. 领料单 save + 行 → confirm（领料出库独立步骤） ----------
        ApiResponse<?> issueSave = rpcMutation("ErpMfgMaterialIssue__save", request("13_issue_save.json5", Map.class));
        output("13_issue_save_response.json5", issueSave);
        assertEquals(0, issueSave.getStatus(), "领料单保存应成功");
        String issueId = idOf(issueSave);
        addVar("issueId", issueId);

        ApiResponse<?> issueLine = rpcMutation("ErpMfgMaterialIssueLine__save", request("14_issue_line_save.json5", Map.class));
        output("14_issue_line_save_response.json5", issueLine);
        assertEquals(0, issueLine.getStatus(), "领料行保存应成功");

        ApiResponse<?> issueConfirm = rpcMutation("ErpMfgMaterialIssue__confirm", request("15_issue_confirm.json5", Map.class));
        output("15_issue_confirm_response.json5", issueConfirm);
        assertEquals(0, issueConfirm.getStatus(), "领料确认应成功");

        // 层 1 锚点：领料出库移动 + MANUFACTURING_ISSUE 凭证（Dr 1411 WIP / Cr 1401 存货 = 10）
        ErpMfgMaterialIssue confirmedIssue = reload(ErpMfgMaterialIssue.class, issueId);
        assertEquals(ErpMfgConstants.ISSUE_STATUS_DONE, confirmedIssue.getDocStatus(), "领料单 docStatus=DONE");
        assertEquals(Boolean.TRUE, confirmedIssue.getPosted(), "领料确认后 posted=true (MANUFACTURING_ISSUE)");
        ErpInvStockMove issueMove = findStockMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_ISSUE, ISSUE_CODE);
        assertNotNull(issueMove, "领料确认应生成出库移动");
        assertEquals("OUTGOING", issueMove.getMoveType(), "领料出库移动 MOVE_TYPE=OUTGOING");
        ErpFinVoucherBillR issueLink = findBillLink(ISSUE_CODE + "-MI");
        ErpFinVoucher issueVoucher = requireVoucherBalanced(issueLink, ISSUE_TOTAL, "MANUFACTURING_ISSUE");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, issueVoucher.getDocStatus(), "MANUFACTURING_ISSUE 凭证已过账");

        // 领料后工单材料成本 = 10（2 × 5）
        assertEquals(0, ISSUE_TOTAL.compareTo(reload(ErpMfgWorkOrder.class, woId).getMaterialCost()),
                "领料后工单 materialCost=10");

        // ---------- 7. 自包含 FIRMED 标准成本（差异计算前置） ----------
        ApiResponse<?> rollupSave = rpcMutation("ErpMfgCostRollup__save", request("16_rollup_save.json5", Map.class));
        output("16_rollup_save_response.json5", rollupSave);
        assertEquals(0, rollupSave.getStatus(), "标准成本滚算单保存应成功");
        addVar("rollupId", idOf(rollupSave));

        ApiResponse<?> rollupLine = rpcMutation("ErpMfgCostRollupLine__save", request("17_rollup_line_save.json5", Map.class));
        output("17_rollup_line_save_response.json5", rollupLine);
        assertEquals(0, rollupLine.getStatus(), "标准成本滚算行保存应成功");

        // ---------- 8. reportCompletion（completedQty=1 驱动完工入库 + 完工过账 + 差异） ----------
        ApiResponse<?> woComplete = rpcMutation("ErpMfgWorkOrder__reportCompletion", request("18_report_completion.json5", Map.class));
        output("18_report_completion_response.json5", woComplete);
        assertEquals(0, woComplete.getStatus(), "报工完工应成功");

        // 层 1 锚点：状态机 APPROVED→IN_PROCESS→COMPLETED
        ErpMfgWorkOrder completedWo = reload(ErpMfgWorkOrder.class, woId);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, completedWo.getDocStatus(), "完工达量 → docStatus=COMPLETED");
        assertEquals(ErpMfgConstants.APPROVE_STATUS_APPROVED, completedWo.getApproveStatus(), "工单 approveStatus=APPROVED");
        assertEquals(0, new BigDecimal("1").compareTo(completedWo.getCompletedQuantity()), "完工数量=1");
        assertEquals(0, ISSUE_TOTAL.compareTo(completedWo.getTotalCost()), "总成本=10（材料成本）");
        assertEquals(0, ISSUE_TOTAL.compareTo(completedWo.getUnitCost()), "单位成本=10/1=10");

        // 层 1 锚点：完工入库移动（MANUFACTURING）生成 + posted
        ErpInvStockMove completionMove = findStockMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER, WO_CODE);
        assertNotNull(completionMove, "报工应生成完工入库移动");
        assertEquals(ErpMfgConstants.MOVE_TYPE_MANUFACTURING, completionMove.getMoveType(), "完工入库 MOVE_TYPE=MANUFACTURE");
        assertEquals(Boolean.TRUE, completionMove.getPosted(), "完工入库 posted=true");

        // 层 1 锚点：完工过账凭证借贷平衡（MANUFACTURING_RECEIPT：Dr 1401 存货 / Cr 1411 WIP = 10）
        ErpFinVoucherBillR receiptLink = findBillLink(completionMove.getCode());
        ErpFinVoucher receiptVoucher = requireVoucherBalanced(receiptLink, ISSUE_TOTAL, "MANUFACTURING_RECEIPT");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, receiptVoucher.getDocStatus(), "MANUFACTURING_RECEIPT 凭证已过账");
        List<ErpFinVoucherLine> receiptLines = findVoucherLines(receiptVoucher.getId());
        ErpFinVoucherLine invDebit = receiptLines.stream()
                .filter(l -> "1401".equals(l.getSubjectCode())).findFirst().orElse(null);
        ErpFinVoucherLine wipCredit = receiptLines.stream()
                .filter(l -> "1411".equals(l.getSubjectCode())).findFirst().orElse(null);
        assertNotNull(invDebit, "MANUFACTURING_RECEIPT 凭证应含 1401 存货行");
        assertNotNull(wipCredit, "MANUFACTURING_RECEIPT 凭证应含 1411 WIP 行");
        assertEquals("DEBIT", invDebit.getDcDirection(), "存货借方方向");
        assertEquals(0, ISSUE_TOTAL.compareTo(invDebit.getDebitAmount()), "存货借方=10");
        assertEquals("CREDIT", wipCredit.getDcDirection(), "WIP 贷方方向");
        assertEquals(0, ISSUE_TOTAL.compareTo(wipCredit.getCreditAmount()), "WIP 贷方=10");

        // ---------- 9. ErpMfgCostVariance__findPage 差异断言 ----------
        ApiResponse<?> variancePage = executeRpc(GraphQLOperationType.query, "ErpMfgCostVariance__findPage",
                request("19_variance_find_page.json5", Map.class));
        output("19_variance_find_page_response.json5", variancePage);
        assertEquals(0, variancePage.getStatus(), "成本差异查询应成功");

        // 层 1 锚点：cost_variance.varianceAmount = actual − standard（MATERIAL_USAGE：10 − 8 = 2）
        List<ErpMfgCostVariance> variances = findVariances(woId);
        ErpMfgCostVariance materialUsage = variances.stream()
                .filter(v -> ErpMfgConstants.VARIANCE_TYPE_MATERIAL_USAGE.equals(v.getVarianceType()))
                .findFirst().orElse(null);
        assertNotNull(materialUsage, "应生成 MATERIAL_USAGE 差异行");
        assertEquals(0, new BigDecimal("10").compareTo(materialUsage.getActualAmount()), "差异行 actualAmount=10");
        assertEquals(0, STD_MATERIAL_PER_UNIT.compareTo(materialUsage.getStandardAmount()), "差异行 standardAmount=8");
        assertEquals(0, materialUsage.getVarianceAmount()
                .compareTo(materialUsage.getActualAmount().subtract(materialUsage.getStandardAmount())),
                "varianceAmount = actual − standard");
        assertEquals(0, MATERIAL_VARIANCE.compareTo(materialUsage.getVarianceAmount()), "MATERIAL_USAGE 差异=+2");

        // 层 1 锚点：PRODUCTION_VARIANCE 过账凭证借贷平衡（Dr 1410 制造差异-材料 / Cr 1411 = 2）
        ErpFinVoucherBillR pvLink = findBillLink(WO_CODE + "-PV");
        ErpFinVoucher pvVoucher = requireVoucherBalanced(pvLink, MATERIAL_VARIANCE, "PRODUCTION_VARIANCE");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, pvVoucher.getDocStatus(), "PRODUCTION_VARIANCE 凭证已过账");

        // 层 1 锚点：完工入库后产成品余额（WH-MAIN，1 @ 10）
        ErpInvStockBalance pBalance = stockBalance(pId, "1");
        assertNotNull(pBalance, "产成品库存余额应存在");
        assertEquals(0, new BigDecimal("1").compareTo(pBalance.getTotalQuantity()), "产成品余额=1");
        assertEquals(0, new BigDecimal("10").compareTo(pBalance.getTotalCost()), "产成品余额成本=10");
    }

    // ---------- helpers ----------

    private List<ErpFinVoucherLine> findVoucherLines(String voucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
    }

    private List<ErpMfgCostVariance> findVariances(String workOrderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workOrderId", workOrderId));
        return daoProvider.daoFor(ErpMfgCostVariance.class).findAllByQuery(q);
    }

    private ErpInvStockBalance stockBalance(String materialId, String warehouseId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", warehouseId));
        List<ErpInvStockBalance> list = daoProvider.daoFor(ErpInvStockBalance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}