package io.nop.app.all.it;

import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.dao.entity.ErpInvCostLayer;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockLedger;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.ErpPurConstants;
import app.erp.pur.service.ErpPurErrors;
import app.erp.sal.dao.entity.ErpSalDelivery;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.service.ErpSalConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * B3 C06：库存成本流转与 COGS 核算（含质检门控，按 {@code docs/design/integration-testing.md §6 C06} 规格）。
 *
 * <p>全链（自包含建数，引用部署 seed：组织 2 / CUST-001 客户 1 / WH-MAIN 仓库 1 / CNY 币种 1 /
 * 2026-07 OPEN 期间）：自包含 FIFO 物料 → QA 来料检验（{@code ErpQaInspection__save}）→
 * **Receive 审批门控**（{@code @NopTestProperty} 显式启用 {@code erp-qua.mandatory-inspection-bill-types}
 * ——**实施期修正：配置值 = {@code ERP_PUR_RECEIVE}（实仓常量 {@code ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE}），
 * 非计划原文 {@code PUR_RECEIPT}（QA 域自引用常量 {@code ErpQaConstants.RELATED_BILL_TYPE_PUR_RECEIPT}
 * 不同值）；门控点在 {@code ErpPurReceiveProcessor.enforceInspectionGate:494-510}）**：REJECTED 负路径
 * （阻断 + {@code ERR_RECEIVE_INSPECTION_BLOCKED} 错误码断言）→ ACCEPTED 正路径（放行过账）→ 采购入库
 * （**Receive approve 内建移动管道**：DONE 入库移动 + FIFO cost_layer 10@8.5=85 + stock_balance 10/85，
 * **无显式 generateMove/confirm**——显式 confirm 对 DONE 移动单非法
 * {@code ERR_ILLEGAL_STATUS_TRANSITION}，见计划 Baseline 入库移动管道）→ 销售订单 submit/approve →
 * 出库单 submit/approve（出库移动 + SALES_OUTPUT 过账，**COGS = FIFO 层成本 6×8.5=51**，对齐
 * {@code TestErpInvFifoCostingEndToEnd}）→ stock_balance/cost_layer/ledger 断言。
 *
 * <p>三层验证：层 1 = JUnit 关键断言；层 2 = 每步 response 快照（拒绝路径不 output——不产生最终
 * 持久化业务结果，仅断言错误码）；层 3 = output/tables 变更行。
 * RECORDING→CHECKING 往返已按 M0.2 试点范式完成（录制后切回默认 CHECKING 复跑全绿）。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
@NopTestProperty(name = "erp-qua.mandatory-inspection-bill-types", value = "ERP_PUR_RECEIVE")
public class TestErpC06InvCostFlowCogs extends ErpIntegrationTestCase {

    static final BigDecimal UNIT_COST = new BigDecimal("8.5");   // 采购入库单价
    static final BigDecimal IN_QTY = new BigDecimal("10");        // 入库数量
    static final BigDecimal IN_TOTAL = new BigDecimal("85");      // 10 × 8.5
    static final BigDecimal OUT_QTY = new BigDecimal("6");
    static final BigDecimal COGS = new BigDecimal("51");         // FIFO 6 × 8.5
    static final BigDecimal BALANCE_QTY_AFTER = new BigDecimal("4");
    static final BigDecimal BALANCE_COST_AFTER = new BigDecimal("34"); // 85 − 51
    static final String NEG_RECEIVE_CODE = "IT-C06-RCV-NEG-001";
    static final String POS_RECEIVE_CODE = "IT-C06-RCV-POS-001";

    @Inject
    IDaoProvider daoProvider;

    @Test
    public void testInvCostFlowCogs() {
        // ---------- 1. 自包含 FIFO 物料 ----------
        ApiResponse<?> materialSave = rpcMutation("ErpMdMaterial__save", request("1_material_save.json5", Map.class));
        output("1_material_save_response.json5", materialSave);
        assertEquals(0, materialSave.getStatus(), "物料保存应成功");
        String materialId = idOf(materialSave);
        addVar("materialId", materialId);

        // ---------- 2. REJECTED 负路径：检验 REJECTED → Receive 审批门控阻断 ----------
        ApiResponse<?> insNeg = rpcMutation("ErpQaInspection__save", request("2_inspection_rejected_save.json5", Map.class));
        output("2_inspection_rejected_save_response.json5", insNeg);
        assertEquals(0, insNeg.getStatus(), "质检单（REJECTED）保存应成功");
        addVar("insNegId", idOf(insNeg));

        ApiResponse<?> rcvNegSave = rpcMutation("ErpPurReceive__save", request("3_receive_neg_save.json5", Map.class));
        output("3_receive_neg_save_response.json5", rcvNegSave);
        assertEquals(0, rcvNegSave.getStatus(), "收货单（负路径）保存应成功");
        String negReceiveId = idOf(rcvNegSave);
        addVar("negReceiveId", negReceiveId);

        ApiResponse<?> rcvNegLine = rpcMutation("ErpPurReceiveLine__save", request("4_receive_neg_line_save.json5", Map.class));
        output("4_receive_neg_line_save_response.json5", rcvNegLine);
        assertEquals(0, rcvNegLine.getStatus(), "收货行（负路径）保存应成功");

        ApiResponse<?> rcvNegSubmit = rpcMutation("ErpPurReceive__submitForApproval", request("5_receive_neg_submit.json5", Map.class));
        output("5_receive_neg_submit_response.json5", rcvNegSubmit);
        assertEquals(0, rcvNegSubmit.getStatus(), "收货单（负路径）提交审批应成功");

        // 层 1 锚点：REJECTED 检验 → ErpPurReceive__approve 阻断（ERR_RECEIVE_INSPECTION_BLOCKED 错误码）
        ApiResponse<?> rcvNegApprove = rpcMutation("ErpPurReceive__approve", request("6_receive_neg_approve.json5", Map.class));
        assertEquals(ErpPurErrors.ERR_RECEIVE_INSPECTION_BLOCKED.getErrorCode(), rcvNegApprove.getCode(),
                "REJECTED 检验应阻断收货审批（erp.err.pur.receive-inspection-blocked）");
        assertEquals(ErpPurConstants.APPROVE_STATUS_SUBMITTED,
                reload(ErpPurReceive.class, negReceiveId).getApproveStatus(), "阻断后收货单保持 SUBMITTED");
        assertNull(findStockMove(ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE, NEG_RECEIVE_CODE),
                "阻断路径不应产生入库移动");

        // ---------- 3. ACCEPTED 正路径：检验 ACCEPTED → Receive 审批门控放行 → 内建入库移动 + cost_layer ----------
        ApiResponse<?> insPos = rpcMutation("ErpQaInspection__save", request("7_inspection_accepted_save.json5", Map.class));
        output("7_inspection_accepted_save_response.json5", insPos);
        assertEquals(0, insPos.getStatus(), "质检单（ACCEPTED）保存应成功");
        addVar("insPosId", idOf(insPos));

        ApiResponse<?> rcvPosSave = rpcMutation("ErpPurReceive__save", request("8_receive_pos_save.json5", Map.class));
        output("8_receive_pos_save_response.json5", rcvPosSave);
        assertEquals(0, rcvPosSave.getStatus(), "收货单（正路径）保存应成功");
        String posReceiveId = idOf(rcvPosSave);
        String posReceiveCode = String.valueOf(((Map<?, ?>) rcvPosSave.getData()).get("code"));
        addVar("posReceiveId", posReceiveId);
        addVar("posReceiveCode", posReceiveCode);

        ApiResponse<?> rcvPosLine = rpcMutation("ErpPurReceiveLine__save", request("9_receive_pos_line_save.json5", Map.class));
        output("9_receive_pos_line_save_response.json5", rcvPosLine);
        assertEquals(0, rcvPosLine.getStatus(), "收货行（正路径）保存应成功");

        ApiResponse<?> rcvPosSubmit = rpcMutation("ErpPurReceive__submitForApproval", request("10_receive_pos_submit.json5", Map.class));
        output("10_receive_pos_submit_response.json5", rcvPosSubmit);
        assertEquals(0, rcvPosSubmit.getStatus(), "收货单（正路径）提交审批应成功");

        ApiResponse<?> rcvPosApprove = rpcMutation("ErpPurReceive__approve", request("11_receive_pos_approve.json5", Map.class));
        output("11_receive_pos_approve_response.json5", rcvPosApprove);
        assertEquals(0, rcvPosApprove.getStatus(), "ACCEPTED 检验应放行收货审批");
        ErpPurReceive approvedReceive = reload(ErpPurReceive.class, posReceiveId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approvedReceive.getApproveStatus(), "收货单 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedReceive.getPosted(), "收货审核后 posted=true");

        // 层 1 锚点：approve 副作用产生的 DONE 入库移动（无显式 generateMove/confirm）
        ErpInvStockMove inMove = findStockMove(ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE, posReceiveCode);
        assertNotNull(inMove, "收货审批应内建触发入库移动");
        assertEquals(ErpPurConstants.MOVE_TYPE_INCOMING, inMove.getMoveType(), "入库移动 MOVE_TYPE=INCOMING");
        assertEquals("DONE", inMove.getDocStatus(), "入库移动 docStatus=DONE");

        // 层 1 锚点：FIFO cost_layer 10@8.5=85 + stock_balance 10/85
        ErpInvCostLayer inLayer = findLayer(materialId);
        assertNotNull(inLayer, "入库应创建 FIFO cost_layer");
        assertEquals(ErpInvConstants.COST_METHOD_FIFO, inLayer.getCostMethod(), "cost_layer costMethod=FIFO");
        assertEquals(0, IN_QTY.compareTo(inLayer.getIncomingQuantity()), "cost_layer incomingQuantity=10");
        assertEquals(0, IN_QTY.compareTo(inLayer.getRemainingQuantity()), "cost_layer remainingQuantity=10");
        assertEquals(0, UNIT_COST.compareTo(inLayer.getUnitCost()), "cost_layer unitCost=8.5");
        assertEquals(0, IN_TOTAL.compareTo(inLayer.getTotalCost()), "cost_layer totalCost=85");
        ErpInvStockBalance inBalance = stockBalance(materialId);
        assertEquals(0, IN_QTY.compareTo(inBalance.getTotalQuantity()), "入库后 stock_balance.totalQty=10");
        assertEquals(0, IN_TOTAL.compareTo(inBalance.getTotalCost()), "入库后 stock_balance.totalCost=85");

        // ---------- 4. 销售订单 → submit → approve ----------
        ApiResponse<?> orderSave = rpcMutation("ErpSalOrder__save", request("12_order_save.json5", Map.class));
        output("12_order_save_response.json5", orderSave);
        assertEquals(0, orderSave.getStatus(), "销售订单保存应成功");
        String orderId = idOf(orderSave);
        addVar("orderId", orderId);

        ApiResponse<?> orderLine = rpcMutation("ErpSalOrderLine__save", request("13_order_line_save.json5", Map.class));
        output("13_order_line_save_response.json5", orderLine);
        assertEquals(0, orderLine.getStatus(), "订单行保存应成功");
        addVar("orderLineId", idOf(orderLine));

        ApiResponse<?> orderSubmit = rpcMutation("ErpSalOrder__submitForApproval", request("14_order_submit.json5", Map.class));
        output("14_order_submit_response.json5", orderSubmit);
        assertEquals(0, orderSubmit.getStatus(), "销售订单提交审批应成功");

        ApiResponse<?> orderApprove = rpcMutation("ErpSalOrder__approve", request("15_order_approve.json5", Map.class));
        output("15_order_approve_response.json5", orderApprove);
        assertEquals(0, orderApprove.getStatus(), "销售订单审批应成功");
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED,
                reload(ErpSalOrder.class, orderId).getApproveStatus(), "订单 approveStatus=APPROVED");

        // ---------- 5. 出库单 → submit → approve（出库移动 + SALES_OUTPUT 凭证 + FIFO COGS） ----------
        ApiResponse<?> deliverySave = rpcMutation("ErpSalDelivery__save", request("16_delivery_save.json5", Map.class));
        output("16_delivery_save_response.json5", deliverySave);
        assertEquals(0, deliverySave.getStatus(), "出库单保存应成功");
        String deliveryId = idOf(deliverySave);
        String deliveryCode = String.valueOf(((Map<?, ?>) deliverySave.getData()).get("code"));
        addVar("deliveryId", deliveryId);
        addVar("deliveryCode", deliveryCode);

        ApiResponse<?> deliveryLine = rpcMutation("ErpSalDeliveryLine__save", request("17_delivery_line_save.json5", Map.class));
        output("17_delivery_line_save_response.json5", deliveryLine);
        assertEquals(0, deliveryLine.getStatus(), "出库行保存应成功");

        ApiResponse<?> deliverySubmit = rpcMutation("ErpSalDelivery__submitForApproval", request("18_delivery_submit.json5", Map.class));
        output("18_delivery_submit_response.json5", deliverySubmit);
        assertEquals(0, deliverySubmit.getStatus(), "出库单提交审批应成功");

        ApiResponse<?> deliveryApprove = rpcMutation("ErpSalDelivery__approve", request("19_delivery_approve.json5", Map.class));
        output("19_delivery_approve_response.json5", deliveryApprove);
        assertEquals(0, deliveryApprove.getStatus(), "出库单审批应成功");
        ErpSalDelivery approvedDelivery = reload(ErpSalDelivery.class, deliveryId);
        assertEquals(ErpSalConstants.APPROVE_STATUS_APPROVED, approvedDelivery.getApproveStatus(), "出库单 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedDelivery.getPosted(), "出库审核后 posted=true (SALES_OUTPUT)");
        assertEquals(ErpSalConstants.DELIVERY_STATUS_PARTIAL,
                reload(ErpSalOrder.class, orderId).getDeliveryStatus(), "订单发货状态回写 PARTIAL（6/10 分批出库）");

        // 层 1 锚点：出库移动 + SALES_OUTPUT 凭证（COGS = FIFO 层成本 6×8.5=51）
        ErpInvStockMove outMove = findStockMove(ErpSalConstants.RELATED_BILL_TYPE_SAL_DELIVERY, deliveryCode);
        assertNotNull(outMove, "出库审批应触发库存出库移动");
        assertEquals(ErpSalConstants.MOVE_TYPE_OUTGOING, outMove.getMoveType(), "出库移动 MOVE_TYPE=OUTGOING");
        assertEquals("DONE", outMove.getDocStatus(), "出库移动 docStatus=DONE");
        ErpFinVoucherBillR cogsLink = findBillLink(outMove.getCode());
        ErpFinVoucher cogsVoucher = requireVoucherBalanced(cogsLink, COGS, "SALES_OUTPUT");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, cogsVoucher.getDocStatus(), "SALES_OUTPUT 凭证已过账");
        List<ErpFinVoucherLine> cogsLines = findVoucherLines(cogsVoucher.getId());
        ErpFinVoucherLine cogsDebit = cogsLines.stream()
                .filter(l -> "6401".equals(l.getSubjectCode())).findFirst().orElse(null);
        ErpFinVoucherLine cogsCredit = cogsLines.stream()
                .filter(l -> "1401".equals(l.getSubjectCode())).findFirst().orElse(null);
        assertNotNull(cogsDebit, "COGS 凭证应含 6401 主营业务成本行");
        assertNotNull(cogsCredit, "COGS 凭证应含 1401 库存商品行");
        assertEquals("DEBIT", cogsDebit.getDcDirection(), "COGS 借方方向");
        assertEquals(0, COGS.compareTo(cogsDebit.getDebitAmount()), "COGS 借方=51（FIFO 6×8.5）");
        assertEquals("CREDIT", cogsCredit.getDcDirection(), "存货贷方方向");
        assertEquals(0, COGS.compareTo(cogsCredit.getCreditAmount()), "存货贷方=51");

        // 层 1 锚点：出库流水 ledger.totalCost 负号（FIFO 跨层口径，对齐 TestErpInvFifoCostingEndToEnd）
        ErpInvStockLedger outLedger = findOutgoingLedger(materialId);
        assertNotNull(outLedger, "出库应生成库存流水");
        assertEquals(0, COGS.negate().compareTo(outLedger.getTotalCost()), "ledger.totalCost=-51");

        // 层 1 锚点：出库后余额与成本层联动（4/34）
        ErpInvStockBalance afterBalance = stockBalance(materialId);
        assertEquals(0, BALANCE_QTY_AFTER.compareTo(afterBalance.getTotalQuantity()), "出库后 stock_balance.totalQty=4");
        assertEquals(0, BALANCE_COST_AFTER.compareTo(afterBalance.getTotalCost()), "出库后 stock_balance.totalCost=34");
        ErpInvCostLayer afterLayer = findLayer(materialId);
        assertEquals(0, BALANCE_QTY_AFTER.compareTo(afterLayer.getRemainingQuantity()), "出库后 cost_layer remainingQuantity=4");
        assertEquals(0, BALANCE_COST_AFTER.compareTo(afterLayer.getTotalCost()), "出库后 cost_layer totalCost=34");
    }

    // ---------- helpers ----------

    private List<ErpFinVoucherLine> findVoucherLines(String voucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
    }

    private ErpInvCostLayer findLayer(String materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        List<ErpInvCostLayer> list = daoProvider.daoFor(ErpInvCostLayer.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvStockBalance stockBalance(String materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        List<ErpInvStockBalance> list = daoProvider.daoFor(ErpInvStockBalance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvStockLedger findOutgoingLedger(String materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        return daoProvider.daoFor(ErpInvStockLedger.class).findAllByQuery(q).stream()
                .filter(l -> l.getQuantity() != null && l.getQuantity().signum() < 0)
                .findFirst()
                .orElse(null);
    }
}