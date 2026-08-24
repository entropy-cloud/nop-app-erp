package io.nop.app.all.it;

import app.erp.drp.dao.entity.ErpDrpLine;
import app.erp.drp.dao.entity.ErpDrpPlan;
import app.erp.drp.service.ErpDrpConstants;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvTransferOrder;
import app.erp.inv.dao.entity.ErpInvTransferOrderLine;
import app.erp.pur.dao.entity.ErpPurOrder;
import app.erp.pur.dao.entity.ErpPurOrderLine;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * B10 C20a：DRP 净需求与补货释放（按 {@code docs/design/integration-testing.md §6 C20a} 规格）。
 *
 * <p>全链（引用部署 seed：组织 2 / MAT-001 物料 1 / MAT-002 物料 2 / WH-MAIN 仓库 1 / WH-RAW 仓库 2 /
 * SUP-001 供应商 3；drp 域无部署 seed，全自包含建数）：自包含 DRP 计划 + 两条补货参数
 * （TRANSFER：MAT-001@WH-RAW 安全库存 100/倍数 10/调出仓 WH-MAIN；PURCHASE：MAT-002@WH-MAIN
 * 安全库存 50/倍数 1/首选供应商 SUP-001）+ 自包含库存余额（MAT-001@WH-RAW total 50/available 30/
 * reserved 20，DAO fixture——allocatedQty 加项证明）→ {@code ErpDrpPlan__runDrp}（净需求 =
 * max(0, safetyStock + forecastDemand − currentStock + allocatedQty − onOrderQty) 实仓 DrpEngine
 * 口径：TRANSFER 行 = 100+0−30+20−0 = **90**——allocatedQty 为加项，设计文档「毛需求 − 在途 − 在手
 * − 已分配」符号相反为勘误；suggested = ceil(90/10)×10 = 90；PURCHASE 行 = 50−0+0 = 50）→
 * {@code ErpDrpPlan__approvePlan}（行 SUGGESTED→APPROVED + approvedQty 回填 = suggestedQty）→
 * {@code ErpDrpLine__releaseLine}×2（TRANSFER 行 → 调拨单 DRP-TO-{lineId} DRAFT；PURCHASE 行 →
 * 采购单 DRP-PO-{lineId} DRAFT；两行全 ORDERED → 计划 APPROVED→EXECUTED 自动推进）→ 下游单据
 * 审批（采购单 {@code submitForApproval}+{@code approve} [DIRECT] → APPROVED；调拨单
 * {@code confirm} → CONFIRMED——调拨无审批轴，confirm 为其生命周期动作）。
 *
 * <p>Phase 1 Decision（三项裁决，落盘设计文档 §6 C20a 勘误）：
 * <ol>
 *   <li><b>净需求口径</b>：allocatedQty 为<b>加项</b>（预留库存增加净需求）——设计「毛需求 − 在途 −
 *       在手 − 已分配」符号相反；forecastDemand 预测行消费仅限仓级（seed 预测行 warehouseId 皆空 → 0）。</li>
 *   <li><b>释放动作面</b>：行级 {@code releaseLine} 为主体（TRANSFER/PURCHASE 各生成下游单据 +
 *       orderBillType/orderBillCode 回写）；计划级 {@code releaseApproved} 为批量编排（全行释放后
 *       计划 EXECUTED 为隐式推进，{@code DrpReleaseService.advancePlanToExecutedIfComplete}）。</li>
 *   <li><b>下游单据形态</b>：生成的采购单/调拨单为 DRAFT/UNSUBMITTED 草稿（采购行 unitPrice/amount=0
 *       待补录）；采购单 approve 后 approveStatus=APPROVED 但 posted=false（过账悬挂至收货环节）；
 *       调拨单生命周期动作 = {@code confirm}（DRAFT→CONFIRMED，无审批/过账语义）——设计「下游审批
 *       后过账（posted）」以实仓行为为准勘误。</li>
 * </ol>
 *
 * <p>冻结时钟 {@code B10FrozenClockExtension}（2026-07-17，B9 先例）：释放链下游单据 businessDate =
 * CoreMetrics.today() 落库、计划 runAt = currentTimestamp()——跨日漂移取确定值。
 *
 * <p>三层验证：层 1 = JUnit 关键断言（净需求/建议量数值、approvedQty 回填、下游单据存在与字段、
 * 计划 EXECUTED、采购单 APPROVED、调拨单 CONFIRMED）；层 2 = 每步 response 快照；层 3 =
 * output/tables 变更行（drp_plan/line + inv_transfer_order 族 + pur_order 族 + inv_stock_balance）。
 * RECORDING→CHECKING 往返按 M0.2 试点范式完成。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
public class TestErpC20aDrpNetRequirementRelease extends ErpIntegrationTestCase {

    static final String PLAN_CODE = "IT-C20A-PLAN-001";

    // TRANSFER 行：MAT-001@WH-RAW —— net = 100 + 0 − 30 + 20 − 0 = 90（allocatedQty 加项）
    static final BigDecimal T_SAFETY = new BigDecimal("100");
    static final BigDecimal T_CURRENT_STOCK = new BigDecimal("30");
    static final BigDecimal T_ALLOCATED = new BigDecimal("20");
    static final BigDecimal T_NET = new BigDecimal("90");
    // PURCHASE 行：MAT-002@WH-MAIN —— net = 50 + 0 − 0 + 0 − 0 = 50
    static final BigDecimal P_SAFETY = new BigDecimal("50");
    static final BigDecimal P_NET = new BigDecimal("50");

    @RegisterExtension
    static B10FrozenClockExtension frozenClock = new B10FrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testDrpNetRequirementRelease() {
        // ---------- 1. 自包含 DRP 计划（DRAFT） ----------
        ApiResponse<?> planSave = rpcMutation("ErpDrpPlan__save", request("1_plan_save.json5", Map.class));
        output("1_plan_save_response.json5", planSave);
        assertEquals(0, planSave.getStatus(), "DRP 计划保存应成功");
        String planId = idOf(planSave);
        addVar("planId", planId);

        // ---------- 2. 两条补货参数（TRANSFER + PURCHASE） ----------
        ApiResponse<?> paramT = rpcMutation("ErpDrpParameter__save", request("2_param_transfer_save.json5", Map.class));
        output("2_param_transfer_save_response.json5", paramT);
        assertEquals(0, paramT.getStatus(), "TRANSFER 补货参数保存应成功");

        ApiResponse<?> paramP = rpcMutation("ErpDrpParameter__save", request("3_param_purchase_save.json5", Map.class));
        output("3_param_purchase_save_response.json5", paramP);
        assertEquals(0, paramP.getStatus(), "PURCHASE 补货参数保存应成功");

        // ---------- 3. 自包含库存余额（MAT-001@WH-RAW total 50 / available 30 / reserved 20，DAO fixture） ----------
        ormTemplate.runInSession(() -> {
            ErpInvStockBalance b = daoProvider.daoFor(ErpInvStockBalance.class).newEntity();
            b.orm_propValueByName("id", "990101");
            b.setOrgId("2");
            b.setMaterialId("1");
            b.setWarehouseId("2");
            b.setTotalQuantity(new BigDecimal("50"));
            b.setAvailableQuantity(T_CURRENT_STOCK);
            b.setReservedQuantity(T_ALLOCATED);
            daoProvider.daoFor(ErpInvStockBalance.class).saveEntity(b);
        });

        // ---------- 4. runDrp：净需求计算（COMPUTED + SUGGESTED 行） ----------
        ApiResponse<?> runDrp = rpcMutation("ErpDrpPlan__runDrp", request("4_run_drp.json5", Map.class));
        output("4_run_drp_response.json5", runDrp);
        assertEquals(0, runDrp.getStatus(), "runDrp 应成功");
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED,
                reload(ErpDrpPlan.class, planId).getStatus(), "runDrp 后计划 COMPUTED");

        List<ErpDrpLine> lines = linesOf(planId);
        assertEquals(2, lines.size(), "应生成 2 条补货行");
        ErpDrpLine tLine = findLine(lines, "1");
        ErpDrpLine pLine = findLine(lines, "2");
        assertNotNull(tLine, "TRANSFER 行（MAT-001）应存在");
        assertNotNull(pLine, "PURCHASE 行（MAT-002）应存在");

        // 层 1 锚点：净需求 = max(0, safetyStock + forecastDemand − currentStock + allocatedQty − onOrderQty)
        // TRANSFER 行：allocatedQty=20 为加项 → 100 + 0 − 30 + 20 − 0 = 90（设计文档减项口径为勘误）
        assertEquals(0, T_CURRENT_STOCK.compareTo(tLine.getCurrentStock()), "TRANSFER 行 currentStock=30");
        assertEquals(0, T_ALLOCATED.compareTo(tLine.getAllocatedQty()), "TRANSFER 行 allocatedQty=20");
        assertEquals(0, BigDecimal.ZERO.compareTo(tLine.getOnOrderQty()), "TRANSFER 行 onOrderQty=0");
        assertEquals(0, BigDecimal.ZERO.compareTo(tLine.getForecastDemand()), "TRANSFER 行 forecastDemand=0");
        assertEquals(0, T_NET.compareTo(tLine.getNetRequirement()), "TRANSFER 行净需求=90（allocatedQty 加项口径）");
        assertEquals(0, T_NET.compareTo(tLine.getSuggestedQty()), "TRANSFER 行建议量=ceil(90/10)×10=90");
        assertEquals(ErpDrpConstants.REPLENISHMENT_TYPE_TRANSFER, tLine.getReplenishmentType());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED, tLine.getStatus());
        assertEquals(0, P_NET.compareTo(pLine.getNetRequirement()), "PURCHASE 行净需求=50");
        assertEquals(0, P_NET.compareTo(pLine.getSuggestedQty()), "PURCHASE 行建议量=50");
        assertEquals(ErpDrpConstants.REPLENISHMENT_TYPE_PURCHASE, pLine.getReplenishmentType());
        assertEquals(0, new BigDecimal("140").compareTo(reload(ErpDrpPlan.class, planId).getTotalReplenishmentQty()),
                "计划总补货量=90+50=140");
        addVar("transferLineId", tLine.getId());
        addVar("purchaseLineId", pLine.getId());

        // ---------- 5. approvePlan：行 SUGGESTED→APPROVED + approvedQty 回填 ----------
        ApiResponse<?> approve = rpcMutation("ErpDrpPlan__approvePlan", request("5_approve_plan.json5", Map.class));
        output("5_approve_plan_response.json5", approve);
        assertEquals(0, approve.getStatus(), "approvePlan 应成功");
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED,
                reload(ErpDrpPlan.class, planId).getStatus(), "approvePlan 后计划 APPROVED");
        ErpDrpLine tApproved = reload(ErpDrpLine.class, tLine.getId());
        ErpDrpLine pApproved = reload(ErpDrpLine.class, pLine.getId());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_APPROVED, tApproved.getStatus());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_APPROVED, pApproved.getStatus());
        // 层 1 锚点：approvedQty 回填 = suggestedQty
        assertEquals(0, T_NET.compareTo(tApproved.getApprovedQty()), "TRANSFER 行 approvedQty 回填=90");
        assertEquals(0, P_NET.compareTo(pApproved.getApprovedQty()), "PURCHASE 行 approvedQty 回填=50");

        // ---------- 6. releaseLine（TRANSFER 行 → 调拨单） ----------
        ApiResponse<?> relT = rpcMutation("ErpDrpLine__releaseLine", request("6_release_transfer_line.json5", Map.class));
        output("6_release_transfer_line_response.json5", relT);
        assertEquals(0, relT.getStatus(), "TRANSFER 行释放应成功");
        ErpDrpLine tOrdered = reload(ErpDrpLine.class, tLine.getId());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_ORDERED, tOrdered.getStatus(), "TRANSFER 行释放后 ORDERED");
        assertEquals(ErpDrpConstants.ORDER_BILL_TYPE_TRANSFER_ORDER, tOrdered.getOrderBillType());
        ErpInvTransferOrder to = findTransferOrderByCode(tOrdered.getOrderBillCode());
        assertNotNull(to, "释放应生成调拨单 " + tOrdered.getOrderBillCode());
        assertEquals("1", String.valueOf(to.getFromWarehouseId()), "调拨单调出仓=参数首选调出仓 WH-MAIN");
        assertEquals("2", String.valueOf(to.getToWarehouseId()), "调拨单调入仓=行目标仓 WH-RAW");
        assertEquals(ErpDrpConstants.DOWNSTREAM_DOC_STATUS_DRAFT, to.getDocStatus(), "生成调拨单 DRAFT");
        ErpInvTransferOrderLine toLine = findTransferOrderLine(to.getId());
        assertNotNull(toLine, "调拨单行应存在");
        assertEquals(0, T_NET.compareTo(toLine.getQuantity()), "调拨行数量=approvedQty 90");
        addVar("toId", to.getId());

        // ---------- 7. releaseLine（PURCHASE 行 → 采购单）+ 计划 EXECUTED 自动推进 ----------
        ApiResponse<?> relP = rpcMutation("ErpDrpLine__releaseLine", request("7_release_purchase_line.json5", Map.class));
        output("7_release_purchase_line_response.json5", relP);
        assertEquals(0, relP.getStatus(), "PURCHASE 行释放应成功");
        ErpDrpLine pOrdered = reload(ErpDrpLine.class, pLine.getId());
        assertEquals(ErpDrpConstants.DRP_LINE_STATUS_ORDERED, pOrdered.getStatus(), "PURCHASE 行释放后 ORDERED");
        assertEquals(ErpDrpConstants.ORDER_BILL_TYPE_PURCHASE_ORDER, pOrdered.getOrderBillType());
        ErpPurOrder po = findPurchaseOrderByCode(pOrdered.getOrderBillCode());
        assertNotNull(po, "释放应生成采购单 " + pOrdered.getOrderBillCode());
        assertEquals("3", String.valueOf(po.getSupplierId()), "采购单供应商=参数首选供应商 SUP-001");
        assertEquals(ErpDrpConstants.DOWNSTREAM_DOC_STATUS_DRAFT, po.getDocStatus(), "生成采购单 DRAFT");
        ErpPurOrderLine poLine = findPurchaseOrderLine(po.getId());
        assertNotNull(poLine, "采购单行应存在");
        assertEquals(0, P_NET.compareTo(poLine.getQuantity()), "采购行数量=approvedQty 50");
        addVar("poId", po.getId());
        // 层 1 锚点：全部行 ORDERED → 计划 APPROVED→EXECUTED（隐式推进）
        assertEquals(ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED,
                reload(ErpDrpPlan.class, planId).getStatus(), "全行释放后计划 EXECUTED");

        // ---------- 8. 下游采购单 DIRECT 审批（submitForApproval → approve） ----------
        ApiResponse<?> poSubmit = rpcMutation("ErpPurOrder__submitForApproval", request("8_po_submit.json5", Map.class));
        output("8_po_submit_response.json5", poSubmit);
        assertEquals(0, poSubmit.getStatus(), "采购单提交审批应成功");
        ApiResponse<?> poApprove = rpcMutation("ErpPurOrder__approve", request("9_po_approve.json5", Map.class));
        output("9_po_approve_response.json5", poApprove);
        assertEquals(0, poApprove.getStatus(), "采购单审批应成功");
        ErpPurOrder poApproved = reload(ErpPurOrder.class, po.getId());
        assertEquals("APPROVED", poApproved.getApproveStatus(), "采购单 approveStatus=APPROVED（DIRECT 轴）");

        // ---------- 9. 下游调拨单 confirm（DRAFT→CONFIRMED，生命周期动作） ----------
        ApiResponse<?> toConfirm = rpcMutation("ErpInvTransferOrder__confirm", request("10_to_confirm.json5", Map.class));
        output("10_to_confirm_response.json5", toConfirm);
        assertEquals(0, toConfirm.getStatus(), "调拨单确认应成功");
        assertEquals("CONFIRMED", reload(ErpInvTransferOrder.class, to.getId()).getDocStatus(),
                "调拨单 confirm 后 CONFIRMED");
    }

    // ---------- helpers ----------

    private List<ErpDrpLine> linesOf(String planId) {
        QueryBean q = eqFilterQuery("planId", planId);
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpDrpLine.class).findAllByQuery(q);
    }

    private ErpDrpLine findLine(List<ErpDrpLine> lines, String materialId) {
        for (ErpDrpLine l : lines) {
            if (materialId.equals(String.valueOf(l.getMaterialId()))) {
                return l;
            }
        }
        return null;
    }

    private ErpInvTransferOrder findTransferOrderByCode(String code) {
        List<ErpInvTransferOrder> list = daoProvider.daoFor(ErpInvTransferOrder.class)
                .findAllByQuery(eqFilterQuery("code", code));
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvTransferOrderLine findTransferOrderLine(String transferId) {
        List<ErpInvTransferOrderLine> list = daoProvider.daoFor(ErpInvTransferOrderLine.class)
                .findAllByQuery(eqFilterQuery("transferId", transferId));
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpPurOrder findPurchaseOrderByCode(String code) {
        List<ErpPurOrder> list = daoProvider.daoFor(ErpPurOrder.class)
                .findAllByQuery(eqFilterQuery("code", code));
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpPurOrderLine findPurchaseOrderLine(String orderId) {
        List<ErpPurOrderLine> list = daoProvider.daoFor(ErpPurOrderLine.class)
                .findAllByQuery(eqFilterQuery("orderId", orderId));
        return list.isEmpty() ? null : list.get(0);
    }
}
