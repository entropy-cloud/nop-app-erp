package io.nop.app.all.it;

import app.erp.fin.dao.entity.ErpFinVoucher;
import app.erp.fin.dao.entity.ErpFinVoucherBillR;
import app.erp.fin.dao.entity.ErpFinVoucherLine;
import app.erp.fin.service.ErpFinConstants;
import app.erp.inv.dao.entity.ErpInvCostAdjust;
import app.erp.inv.dao.entity.ErpInvCostLayer;
import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.ErpPurConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * B3 C05：库存到岸成本分摊过账（按 {@code docs/design/integration-testing.md §6 C05} 规格）。
 *
 * <p>全链（自包含建数，引用部署 seed：组织 2 / SUP-001 供应商 3 / WH-RAW 仓库 2 / CNY 币种 1 /
 * 2026-07 OPEN 期间）：自包含 FIFO 物料 → 自包含已收货采购链（PO → Receive submit/approve，
 * approve 内建触发入库移动 DONE + cost_layer 10@5=50 + stock_balance 10/50）→ 自包含物流运费
 * （{@code ErpLogShipment__save} → {@code ErpLogShipment__get} 来源核对 freightAmount=15）→
 * 到岸成本单 save + FREIGHT 行 → **approve（approve-only——实仓 {@code ErpInvLandedCostBizModel}
 * 无 submitForApproval，仅 approve/reverseApprove/allocate/generateFreightLandedCost，Phase 1
 * Decision 落地）**（LANDED_COST 凭证 15 + FIFO 分摊 delta 成本层 + stock_balance 联动）。
 *
 * <p>Phase 1 Decision（审批轴 + 数据来源双裁决）：(1) 审批轴 = approve-only（设计文档 §6 C05
 * 「save → submitForApproval → approve」动作名漂移，以当前实现为准落地 approve-only 并登记澄清）；
 * (2) 数据来源 = 收货链/物流运费自包含建数（seed 收货链移动 posted=false、无 erp_log_* seed，
 * 未触发 seed 修正授权——roadmap 横切关注点 3 未启用）；(3) 实施期发现：MOVING_AVERAGE 成本调整
 * 只更新 balance 不动 cost_layer（{@code CostAdjustmentService.applyAverageLike}），故本用例物料
 * 取 FIFO——分摊 delta 层（unitCost=Δ）追加后 Σ cost_layer totalCost = 原成本 + 分摊运费，满足
 * 设计文档「cost_layer 总成本联动」断言语义（balance.totalCost 同源联动）。
 *
 * <p>三层验证：层 1 = JUnit 关键断言；层 2 = 每步 response 快照；层 3 = output/tables 变更行。
 * RECORDING→CHECKING 往返已按 M0.2 试点范式完成（录制后切回默认 CHECKING 复跑全绿）。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
public class TestErpC05InvLandedCost extends ErpIntegrationTestCase {

    static final BigDecimal RECEIVE_TOTAL = new BigDecimal("50");  // 10 × 5
    static final BigDecimal FREIGHT = new BigDecimal("15");        // 物流运费（BY_AMOUNT 全额分摊）
    static final BigDecimal LAYER_TOTAL_AFTER = new BigDecimal("65"); // 50 + 15
    static final String SHIPMENT_CODE = "IT-C05-SHP-001";
    static final String LANDED_COST_CODE = "IT-C05-LC-001";

    @Inject
    IDaoProvider daoProvider;

    @Test
    public void testInvLandedCost() {
        // ---------- 1. 自包含 FIFO 物料 ----------
        ApiResponse<?> materialSave = rpcMutation("ErpMdMaterial__save", request("1_material_save.json5", Map.class));
        output("1_material_save_response.json5", materialSave);
        assertEquals(0, materialSave.getStatus(), "物料保存应成功");
        String materialId = idOf(materialSave);
        addVar("materialId", materialId);

        // ---------- 2. 自包含已收货采购链：PO → submit → approve ----------
        ApiResponse<?> poSave = rpcMutation("ErpPurOrder__save", request("2_order_save.json5", Map.class));
        output("2_order_save_response.json5", poSave);
        assertEquals(0, poSave.getStatus(), "PO 保存应成功");
        String poId = idOf(poSave);
        addVar("poId", poId);

        ApiResponse<?> poLine = rpcMutation("ErpPurOrderLine__save", request("3_order_line_save.json5", Map.class));
        output("3_order_line_save_response.json5", poLine);
        assertEquals(0, poLine.getStatus(), "PO 行保存应成功");
        addVar("poLineId", idOf(poLine));

        ApiResponse<?> poSubmit = rpcMutation("ErpPurOrder__submitForApproval", request("4_order_submit.json5", Map.class));
        output("4_order_submit_response.json5", poSubmit);
        assertEquals(0, poSubmit.getStatus(), "PO 提交审批应成功");

        ApiResponse<?> poApprove = rpcMutation("ErpPurOrder__approve", request("5_order_approve.json5", Map.class));
        output("5_order_approve_response.json5", poApprove);
        assertEquals(0, poApprove.getStatus(), "PO 审批应成功");

        // ---------- 3. 收货单 → submit → approve（内建入库移动 + cost_layer + stock_balance） ----------
        ApiResponse<?> rcvSave = rpcMutation("ErpPurReceive__save", request("6_receive_save.json5", Map.class));
        output("6_receive_save_response.json5", rcvSave);
        assertEquals(0, rcvSave.getStatus(), "收货单保存应成功");
        String receiveId = idOf(rcvSave);
        String receiveCode = String.valueOf(((Map<?, ?>) rcvSave.getData()).get("code"));
        addVar("receiveId", receiveId);
        addVar("receiveCode", receiveCode);

        ApiResponse<?> rcvLine = rpcMutation("ErpPurReceiveLine__save", request("7_receive_line_save.json5", Map.class));
        output("7_receive_line_save_response.json5", rcvLine);
        assertEquals(0, rcvLine.getStatus(), "收货行保存应成功");

        ApiResponse<?> rcvSubmit = rpcMutation("ErpPurReceive__submitForApproval", request("8_receive_submit.json5", Map.class));
        output("8_receive_submit_response.json5", rcvSubmit);
        assertEquals(0, rcvSubmit.getStatus(), "收货单提交审批应成功");

        ApiResponse<?> rcvApprove = rpcMutation("ErpPurReceive__approve", request("9_receive_approve.json5", Map.class));
        output("9_receive_approve_response.json5", rcvApprove);
        assertEquals(0, rcvApprove.getStatus(), "收货单审批应成功");
        ErpPurReceive approvedReceive = reload(ErpPurReceive.class, receiveId);
        assertEquals(ErpPurConstants.APPROVE_STATUS_APPROVED, approvedReceive.getApproveStatus(), "收货单 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedReceive.getPosted(), "收货审核后 posted=true");

        // 层 1 锚点：入库移动 DONE + 原成本层 10@5=50（收货链前置态）
        ErpInvStockMove inMove = findStockMove(ErpPurConstants.RELATED_BILL_TYPE_PUR_RECEIVE, receiveCode);
        assertNotNull(inMove, "收货审批应触发入库移动");
        assertEquals(ErpPurConstants.MOVE_TYPE_INCOMING, inMove.getMoveType(), "入库移动 MOVE_TYPE=INCOMING");
        assertEquals("DONE", inMove.getDocStatus(), "入库移动 docStatus=DONE");
        List<ErpInvCostLayer> layersBefore = findCostLayers(materialId);
        assertEquals(1, layersBefore.size(), "收货后应有 1 个原成本层");
        assertEquals(0, RECEIVE_TOTAL.compareTo(layersBefore.get(0).getTotalCost()), "原成本层 totalCost=50");
        assertEquals(0, RECEIVE_TOTAL.compareTo(stockBalance(materialId).getTotalCost()), "收货后 stock_balance.totalCost=50");

        // ---------- 4. 自包含物流运费（无 erp_log_* seed，实仓核验 94 CSV 无 logistics 文件）→ get 来源核对 ----------
        ApiResponse<?> carrierSave = rpcMutation("ErpLogCarrier__save", request("10_carrier_save.json5", Map.class));
        output("10_carrier_save_response.json5", carrierSave);
        assertEquals(0, carrierSave.getStatus(), "承运商保存应成功");
        addVar("carrierId", idOf(carrierSave));

        ApiResponse<?> shipSave = rpcMutation("ErpLogShipment__save", request("11_shipment_save.json5", Map.class));
        output("11_shipment_save_response.json5", shipSave);
        assertEquals(0, shipSave.getStatus(), "发运单保存应成功");
        String shipmentId = idOf(shipSave);
        addVar("shipmentId", shipmentId);

        ApiResponse<?> shipGet = executeRpc(GraphQLOperationType.query, "ErpLogShipment__get",
                request("12_shipment_get.json5", Map.class));
        output("12_shipment_get_response.json5", shipGet);
        assertEquals(0, shipGet.getStatus(), "发运单读取应成功");
        assertEquals(SHIPMENT_CODE, String.valueOf(((Map<?, ?>) shipGet.getData()).get("code")), "运费来源核对：单号一致");
        assertEquals(0, FREIGHT.compareTo(new BigDecimal(String.valueOf(((Map<?, ?>) shipGet.getData()).get("freightAmount")))),
                "运费来源核对：freightAmount=15");

        // ---------- 5. 到岸成本单 save + FREIGHT 行 → approve（approve-only，Phase 1 Decision） ----------
        ApiResponse<?> lcSave = rpcMutation("ErpInvLandedCost__save", request("13_landed_cost_save.json5", Map.class));
        output("13_landed_cost_save_response.json5", lcSave);
        assertEquals(0, lcSave.getStatus(), "到岸成本单保存应成功");
        String landedCostId = idOf(lcSave);
        addVar("landedCostId", landedCostId);

        ApiResponse<?> lcLine = rpcMutation("ErpInvLandedCostLine__save", request("14_landed_cost_line_save.json5", Map.class));
        output("14_landed_cost_line_save_response.json5", lcLine);
        assertEquals(0, lcLine.getStatus(), "到岸成本行保存应成功");

        ApiResponse<?> lcGet = executeRpc(GraphQLOperationType.query, "ErpInvLandedCost__get",
                request("15_landed_cost_get.json5", Map.class));
        output("15_landed_cost_get_response.json5", lcGet);
        assertEquals(0, lcGet.getStatus(), "到岸成本单读取应成功");

        ApiResponse<?> lcApprove = rpcMutation("ErpInvLandedCost__approve", request("16_landed_cost_approve.json5", Map.class));
        output("16_landed_cost_approve_response.json5", lcApprove);
        assertEquals(0, lcApprove.getStatus(), "到岸成本审核（approve-only）应成功");

        // 层 1 锚点：LandedCost approveStatus=APPROVED + posted=true
        ErpInvLandedCost approvedLandedCost = reload(ErpInvLandedCost.class, landedCostId);
        assertEquals(ErpInvConstants.APPROVE_STATUS_APPROVED, approvedLandedCost.getApproveStatus(), "到岸成本 approveStatus=APPROVED");
        assertEquals(Boolean.TRUE, approvedLandedCost.getPosted(), "到岸成本审核后 posted=true (LANDED_COST)");

        // 层 1 锚点：LANDED_COST 凭证借贷平衡（1401 借 / 2202 贷 = 分摊运费 15）
        ErpFinVoucherBillR lcLink = findBillLink(LANDED_COST_CODE);
        ErpFinVoucher lcVoucher = requireVoucherBalanced(lcLink, FREIGHT, "LANDED_COST");
        assertEquals(ErpFinConstants.VOUCHER_STATUS_POSTED, lcVoucher.getDocStatus(), "LANDED_COST 凭证已过账");
        List<ErpFinVoucherLine> lcLines = findVoucherLines(lcVoucher.getId());
        ErpFinVoucherLine invDebit = lcLines.stream()
                .filter(l -> "1401".equals(l.getSubjectCode())).findFirst().orElse(null);
        ErpFinVoucherLine apCredit = lcLines.stream()
                .filter(l -> "2202".equals(l.getSubjectCode())).findFirst().orElse(null);
        assertNotNull(invDebit, "LANDED_COST 凭证应含 1401 存货行");
        assertNotNull(apCredit, "LANDED_COST 凭证应含 2202 应付行");
        assertEquals("DEBIT", invDebit.getDcDirection(), "存货借方方向");
        assertEquals(0, FREIGHT.compareTo(invDebit.getDebitAmount()), "存货借方=15");
        assertEquals("CREDIT", apCredit.getDcDirection(), "应付贷方方向");
        assertEquals(0, FREIGHT.compareTo(apCredit.getCreditAmount()), "应付贷方=15");

        // 层 1 锚点：成本调整单（LC-<code>）落库 + posted=true
        ErpInvCostAdjust adjust = findCostAdjust("LC-" + LANDED_COST_CODE);
        assertNotNull(adjust, "到岸成本应生成成本调整单 LC-IT-C05-LC-001");
        assertEquals(Boolean.TRUE, adjust.getPosted(), "成本调整单 posted=true");

        // 层 1 锚点：cost_layer 总成本联动（FIFO 分摊 delta 层追加，Σ = 50 + 15 = 65）
        List<ErpInvCostLayer> layersAfter = findCostLayers(materialId);
        assertEquals(2, layersAfter.size(), "分摊后应追加 1 个 delta 调整层（原层 + 调整层）");
        ErpInvCostLayer deltaLayer = layersAfter.stream()
                .filter(l -> l.getIncomingMoveId() != null && l.getIncomingMoveId().startsWith("-"))
                .findFirst().orElse(null);
        assertNotNull(deltaLayer, "FIFO 分摊 delta 层应存在（incomingMoveId 负行 id 哨兵）");
        assertEquals(0, FREIGHT.compareTo(deltaLayer.getTotalCost()), "delta 层 totalCost=15");
        assertEquals(0, new BigDecimal("1.5").compareTo(deltaLayer.getUnitCost()), "delta 层 unitCost=Δ=1.5");
        assertEquals(ErpInvConstants.COST_METHOD_FIFO, deltaLayer.getCostMethod(), "delta 层 costMethod=FIFO");
        assertEquals(0, LAYER_TOTAL_AFTER.compareTo(
                layersAfter.stream().map(l -> nz(l.getTotalCost())).reduce(BigDecimal.ZERO, BigDecimal::add)),
                "Σ cost_layer totalCost = 原成本 50 + 分摊运费 15 = 65");

        // 层 1 锚点：stock_balance.totalCost 联动（50 + 15 = 65；FIFO 语义 avgCost 置空）
        ErpInvStockBalance balance = stockBalance(materialId);
        assertEquals(0, new BigDecimal("10").compareTo(balance.getTotalQuantity()), "stock_balance.totalQty=10");
        assertEquals(0, LAYER_TOTAL_AFTER.compareTo(balance.getTotalCost()), "stock_balance.totalCost=65");
    }

    // ---------- helpers ----------

    private List<ErpFinVoucherLine> findVoucherLines(String voucherId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("voucherId", voucherId));
        return daoProvider.daoFor(ErpFinVoucherLine.class).findAllByQuery(q);
    }

    private List<ErpInvCostLayer> findCostLayers(String materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        return daoProvider.daoFor(ErpInvCostLayer.class).findAllByQuery(q);
    }

    private ErpInvStockBalance stockBalance(String materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        List<ErpInvStockBalance> list = daoProvider.daoFor(ErpInvStockBalance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvCostAdjust findCostAdjust(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpInvCostAdjust> list = daoProvider.daoFor(ErpInvCostAdjust.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}