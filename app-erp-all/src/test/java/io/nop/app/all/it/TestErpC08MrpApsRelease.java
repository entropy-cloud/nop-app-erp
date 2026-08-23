package io.nop.app.all.it;

import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.dao.entity.ErpApsSchedule;
import app.erp.aps.service.ErpApsConstants;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.mfg.dao.entity.ErpMfgMrpPlanLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B4 C08：MRP 计划 → APS 排程 → 工单释放（按 {@code docs/design/integration-testing.md §6 C08} 规格）。
 *
 * <p>全链（自包含建数，引用部署 seed：组织 2 / WC-001 工作中心 1 / CNY 币种 1 / SUP-001 供应商 3）：
 * 自包含物料 P（制造件）+ M1（采购件）+ BOM（1 P = 1 M1，isDefault）→ M1 自包含库存（4 @ 5，
 * onHand=4）→ 基线 MRP 计划 save + 手工需求（P qty 10）→ 仿真场景
 * （{@code ErpMfgMrpScenario__runSimulation}，@NopTestProperty 开启 {@code erp-mfg.simulation-enabled} 门控）→
 * {@code promoteToFormalPlan}（-PROMOTED-1 DRAFT 计划）→ 行级释放
 * （{@code ErpMfgMrpPlanLine__releaseWorkRequest} 生成 WO-MRP-xxx / {@code releasePurchaseRequest}
 * 生成 PO-MRP-xxx）→ APS 排程 save → publish（PUBLISHED）→ 自包含工序订单（引用释放工单，machineId=WC-001）
 * → scheduleForward（DRAFT→PLANNED）→ start（IN_PROGRESS）→ complete（FINISHED）。
 *
 * <p>Phase 2 Decision（释放动作名裁决，落盘设计文档 §6 C08 勘误）：设计文档 §6 C08 步骤 2 原述
 * {@code ErpMfgMrpPlan__release} 为漂移——实仓为<b>行级</b>释放（{@code ErpMfgMrpPlanLineBizModel} 三个
 * per-mutation Processor：{@code releaseWorkRequest}/{@code releasePurchaseRequest}/{@code releaseSubcontractRequest}，
 * {@code TestErpMfgMrpEndToEnd} 实证），用例按实仓落地。另裁决 APS 衔接路径（设计 §6 C08 步骤 4）：
 * MRP 释放的工单为 DRAFT 且无 routing，{@code scanReleasedWorkOrders} 仅扫描 NOT_STARTED..IN_PROCESS、
 * {@code createOperationOrdersFromWorkOrder} 要求 WO 挂 routing——两条实仓路径均无法直接消费释放工单，
 * 故按「自包含建排程」落地：工序订单直接 save（workOrderId=释放工单，保留 MRP 输出→APS 排程输入数据关联）→
 * scheduleForward → publish → start → complete。
 *
 * <p>三层验证：层 1 = JUnit 关键断言（MRP 净需求 = 毛需求 − 在途 − 在手、下游单据存在、排程发布态、
 * 工序订单状态机翻转）；层 2 = 每步 response 快照；层 3 = output/tables 变更行。
 * RECORDING→CHECKING 往返已按 M0.2 试点范式完成。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
@NopTestProperty(name = "erp-mfg.simulation-enabled", value = "true")
public class TestErpC08MrpApsRelease extends ErpIntegrationTestCase {

    static final BigDecimal P_DEMAND = new BigDecimal("10");
    static final BigDecimal M1_ON_HAND = new BigDecimal("4");
    static final BigDecimal P_NET = new BigDecimal("10");  // 10 − 0 − 0
    static final BigDecimal M1_NET = new BigDecimal("6");  // 10 − 4 − 0

    @Inject
    IDaoProvider daoProvider;

    @Test
    public void testMrpApsRelease() {
        // ---------- 1. 自包含物料 P（制造件）+ M1（采购件） ----------
        ApiResponse<?> matPSave = rpcMutation("ErpMdMaterial__save", request("1_material_p_save.json5", Map.class));
        output("1_material_p_save_response.json5", matPSave);
        assertEquals(0, matPSave.getStatus(), "制造件物料保存应成功");
        String pId = idOf(matPSave);
        addVar("pId", pId);

        ApiResponse<?> matM1Save = rpcMutation("ErpMdMaterial__save", request("2_material_m1_save.json5", Map.class));
        output("2_material_m1_save_response.json5", matM1Save);
        assertEquals(0, matM1Save.getStatus(), "采购件物料保存应成功");
        String m1Id = idOf(matM1Save);
        addVar("m1Id", m1Id);

        // ---------- 2. 自包含 BOM（1 P = 1 M1，isDefault） ----------
        ApiResponse<?> bomSave = rpcMutation("ErpMfgBom__save", request("3_bom_save.json5", Map.class));
        output("3_bom_save_response.json5", bomSave);
        assertEquals(0, bomSave.getStatus(), "BOM 保存应成功");
        String bomId = idOf(bomSave);
        addVar("bomId", bomId);

        ApiResponse<?> bomLine = rpcMutation("ErpMfgBomLine__save", request("4_bom_line_save.json5", Map.class));
        output("4_bom_line_save_response.json5", bomLine);
        assertEquals(0, bomLine.getStatus(), "BOM 行保存应成功");

        // ---------- 3. M1 自包含库存（4 @ 5 → onHand=4） ----------
        ApiResponse<?> incomingMove = rpcMutation("ErpInvStockMove__generateMove", request("5_m1_incoming_move.json5", Map.class));
        output("5_m1_incoming_move_response.json5", incomingMove);
        assertEquals(0, incomingMove.getStatus(), "M1 入库移动应成功");

        // ---------- 4. 基线 MRP 计划 save + 手工需求 ----------
        ApiResponse<?> planSave = rpcMutation("ErpMfgMrpPlan__save", request("6_plan_save.json5", Map.class));
        output("6_plan_save_response.json5", planSave);
        assertEquals(0, planSave.getStatus(), "MRP 计划保存应成功");
        String planId = idOf(planSave);
        addVar("planId", planId);

        ApiResponse<?> demandSave = rpcMutation("ErpMfgMrpDemand__save", request("7_demand_save.json5", Map.class));
        output("7_demand_save_response.json5", demandSave);
        assertEquals(0, demandSave.getStatus(), "MRP 需求保存应成功");

        // ---------- 5. 仿真场景 → runSimulation → promoteToFormalPlan ----------
        ApiResponse<?> scenarioSave = rpcMutation("ErpMfgMrpScenario__save", request("8_scenario_save.json5", Map.class));
        output("8_scenario_save_response.json5", scenarioSave);
        assertEquals(0, scenarioSave.getStatus(), "仿真场景保存应成功");
        String scenarioId = idOf(scenarioSave);
        addVar("scenarioId", scenarioId);

        ApiResponse<?> runSim = rpcMutation("ErpMfgMrpScenario__runSimulation", request("9_run_simulation.json5", Map.class));
        output("9_run_simulation_response.json5", runSim);
        assertEquals(0, runSim.getStatus(), "仿真运行应成功（simulation-enabled 门控开启）");
        String versionId = idOf(runSim);
        addVar("scenarioVersionId", versionId);

        ApiResponse<?> promote = rpcMutation("ErpMfgMrpScenario__promoteToFormalPlan", request("10_promote_to_formal_plan.json5", Map.class));
        output("10_promote_to_formal_plan_response.json5", promote);
        assertEquals(0, promote.getStatus(), "转正式计划应成功");
        String promotedPlanId = idOf(promote);
        addVar("promotedPlanId", promotedPlanId);

        // 层 1 锚点：MRP 净需求 = 毛需求 − 在途 − 在手（P: 10−0−0=10；M1: 10−4−0=6）
        List<ErpMfgMrpPlanLine> lines = planLinesOf(promotedPlanId);
        ErpMfgMrpPlanLine pLine = lines.stream()
                .filter(l -> pId.equals(l.getMaterialId())).findFirst().orElse(null);
        ErpMfgMrpPlanLine m1Line = lines.stream()
                .filter(l -> m1Id.equals(l.getMaterialId())).findFirst().orElse(null);
        assertNotNull(pLine, "制造件 P 计划行应存在");
        assertNotNull(m1Line, "采购件 M1 计划行应存在");
        assertEquals(ErpMfgConstants.MRP_ORDER_TYPE_WORK_ORDER_REQUEST, pLine.getOrderType(), "P 行建议类型=工单");
        assertEquals(ErpMfgConstants.MRP_ORDER_TYPE_PURCHASE_REQUEST, m1Line.getOrderType(), "M1 行建议类型=采购");
        assertNetFormula(pLine, P_DEMAND, BigDecimal.ZERO, BigDecimal.ZERO, P_NET);
        assertNetFormula(m1Line, P_DEMAND, M1_ON_HAND, BigDecimal.ZERO, M1_NET);
        addVar("workPlanLineId", pLine.getId());
        addVar("purPlanLineId", m1Line.getId());

        // ---------- 6. 行级释放：工单建议 + 采购建议 ----------
        ApiResponse<?> releaseWork = rpcMutation("ErpMfgMrpPlanLine__releaseWorkRequest", request("11_release_work.json5", Map.class));
        output("11_release_work_response.json5", releaseWork);
        assertEquals(0, releaseWork.getStatus(), "工单建议释放应成功");

        ApiResponse<?> releasePur = rpcMutation("ErpMfgMrpPlanLine__releasePurchaseRequest", request("12_release_purchase.json5", Map.class));
        output("12_release_purchase_response.json5", releasePur);
        assertEquals(0, releasePur.getStatus(), "采购建议释放应成功");

        // 层 1 锚点：下游单据存在（WO-MRP-xxx + PO-MRP-xxx）+ 释放行 isFirmed + convertedBillCode
        String woCode = ErpMfgConstants.RELEASE_WO_CODE_PREFIX + pLine.getId();
        String poCode = ErpMfgConstants.RELEASE_PO_CODE_PREFIX + m1Line.getId();
        ErpMfgWorkOrder releasedWo = findWorkOrder(woCode);
        assertNotNull(releasedWo, "释放应生成工单 " + woCode);
        assertEquals(pId, releasedWo.getProductId(), "工单产品=P");
        assertEquals(0, P_NET.compareTo(releasedWo.getPlannedQuantity()), "工单计划数量=10");
        ErpPurOrder releasedPo = findPurchaseOrder(poCode);
        assertNotNull(releasedPo, "释放应生成采购单 " + poCode);
        assertEquals("3", releasedPo.getSupplierId(), "采购单供应商=SUP-001");
        ErpPurOrderLine poLine = findPurchaseOrderLine(releasedPo.getId());
        assertNotNull(poLine, "采购单行应存在");
        assertEquals(0, M1_NET.compareTo(poLine.getQuantity()), "采购单行数量=6");
        assertEquals(Boolean.TRUE, reload(ErpMfgMrpPlanLine.class, pLine.getId()).getIsFirmed(), "P 行 isFirmed=true");
        assertEquals(Boolean.TRUE, reload(ErpMfgMrpPlanLine.class, m1Line.getId()).getIsFirmed(), "M1 行 isFirmed=true");
        assertNotNull(reload(ErpMfgMrpPlanLine.class, pLine.getId()).getConvertedBillCode(), "P 行 convertedBillCode 回写");
        assertNotNull(reload(ErpMfgMrpPlanLine.class, m1Line.getId()).getConvertedBillCode(), "M1 行 convertedBillCode 回写");
        // 注：仿真引擎按 seed 主数据 safetyStock 补充 SAFETY_STOCK 需求行（seed 物料 MAT-001..004 均设 safetyStock），
        // 故转正计划含 seed 物料需求行，仅释放本用例 P/M1 行后计划保持非 FIRMED（设计文档 §6 C08 layer-1 不要求计划 FIRMED）。
        addVar("releasedWoId", releasedWo.getId());

        // ---------- 7. APS：自包含工序订单（引用释放工单）→ 排程 save → scheduleForward → publish ----------
        ApiResponse<?> opSave = rpcMutation("ErpApsOperationOrder__save", request("13_op_order_save.json5", Map.class));
        output("13_op_order_save_response.json5", opSave);
        assertEquals(0, opSave.getStatus(), "工序订单保存应成功");
        String opOrderId = idOf(opSave);
        addVar("opOrderId", opOrderId);

        ApiResponse<?> scheduleSave = rpcMutation("ErpApsSchedule__save", request("14_schedule_save.json5", Map.class));
        output("14_schedule_save_response.json5", scheduleSave);
        assertEquals(0, scheduleSave.getStatus(), "排程方案保存应成功");
        String scheduleId = idOf(scheduleSave);
        addVar("scheduleId", scheduleId);

        ApiResponse<?> scheduleForward = rpcMutation("ErpApsOperationOrder__scheduleForward", request("15_schedule_forward.json5", Map.class));
        output("15_schedule_forward_response.json5", scheduleForward);
        assertEquals(0, scheduleForward.getStatus(), "前向排程应成功");
        assertEquals(ErpApsConstants.OP_STATUS_PLANNED,
                reload(ErpApsOperationOrder.class, opOrderId).getStatus(), "排程后工序订单 PLANNED");

        ApiResponse<?> publish = rpcMutation("ErpApsSchedule__publish", request("16_schedule_publish.json5", Map.class));
        output("16_schedule_publish_response.json5", publish);
        assertEquals(0, publish.getStatus(), "排程发布应成功");
        assertEquals(ErpApsConstants.SCHEDULE_STATUS_PUBLISHED,
                reload(ErpApsSchedule.class, scheduleId).getStatus(), "排程发布后 PUBLISHED");

        // ---------- 8. 工序订单状态机推进：start → complete ----------
        ApiResponse<?> opStart = rpcMutation("ErpApsOperationOrder__start", request("17_op_start.json5", Map.class));
        output("17_op_start_response.json5", opStart);
        assertEquals(0, opStart.getStatus(), "工序开工应成功");
        assertEquals(ErpApsConstants.OP_STATUS_IN_PROGRESS,
                reload(ErpApsOperationOrder.class, opOrderId).getStatus(), "开工后 IN_PROGRESS");

        ApiResponse<?> opComplete = rpcMutation("ErpApsOperationOrder__complete", request("18_op_complete.json5", Map.class));
        output("18_op_complete_response.json5", opComplete);
        assertEquals(0, opComplete.getStatus(), "工序完工应成功");
        assertEquals(ErpApsConstants.OP_STATUS_FINISHED,
                reload(ErpApsOperationOrder.class, opOrderId).getStatus(), "完工后 FINISHED");

        // 层 1 锚点：工序订单状态机翻转 DRAFT→PLANNED→IN_PROGRESS→FINISHED 已逐级断言
        assertTrue(true, "工序订单状态机翻转链已断言");
    }

    // ---------- helpers ----------

    private void assertNetFormula(ErpMfgMrpPlanLine line, BigDecimal gross, BigDecimal onHand,
                                  BigDecimal scheduled, BigDecimal expectedNet) {
        assertEquals(0, gross.compareTo(line.getGrossRequirement()), "毛需求");
        assertEquals(0, onHand.compareTo(line.getOnHand()), "在手");
        assertEquals(0, scheduled.compareTo(line.getScheduledReceipt()), "在途");
        assertEquals(0, expectedNet.compareTo(line.getNetRequirement()),
                "净需求 = 毛需求 − 在途 − 在手");
    }

    private List<ErpMfgMrpPlanLine> planLinesOf(String planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("mrpPlanId", planId));
        return daoProvider.daoFor(ErpMfgMrpPlanLine.class).findAllByQuery(q);
    }

    private ErpMfgWorkOrder findWorkOrder(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpMfgWorkOrder> list = daoProvider.daoFor(ErpMfgWorkOrder.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpPurOrder findPurchaseOrder(String code) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("code", code));
        List<ErpPurOrder> list = daoProvider.daoFor(ErpPurOrder.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpPurOrderLine findPurchaseOrderLine(String orderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("orderId", orderId));
        List<ErpPurOrderLine> list = daoProvider.daoFor(ErpPurOrderLine.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }
}