package app.erp.mfg.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.inv.dao.entity.ErpInvReservation;
import app.erp.inv.dao.entity.ErpInvReservationLine;
import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.md.dao.entity.ErpMdSubject;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.context.ContextProvider;
import io.nop.autotest.junit.JunitBaseTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * RC-R1.48 mfg 侧物料预留写路径集成测试（Phase 4）：工单审核创建预留 → 领料消耗 → 取消/完工释放全链
 * + config 门控 + no-op 语义 + 无 BOM 不阻断。
 *
 * <p>覆盖 UC-MFG-05（①②③④）/ UC-MFG-08（⑤⑥⑦）/ UC-MFG-06（⑧⑨⑩⑪）的预留写路径运行时行为
 * （对齐 TestErpMfgWorkOrderEndToEnd 范式：IGraphQLEngine + 直断言）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgReservationLifecycle extends JunitBaseTestCase {

    static final String ORG_ID = "1401";
    static final String WAREHOUSE_ID = "3401";
    static final String UOM_ID = "5401";
    static final String CURRENCY_ID = "6401";
    static final String ACCT_SCHEMA_ID = "7401";
    static final String P = "1101";     // 产成品
    static final String M1 = "1102";    // 子件
    static final String MOVE_TYPE_INCOMING = "INCOMING";
    static final String SUBJECT_INVENTORY = "1401";
    static final String SUBJECT_WIP = "1411";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- P1-CK-mfg-002：驳回/反审核后重新提交（双轴守卫互锁回归） ----------

    /**
     * P1-CK-mfg-002 回归：reject 后工单可重新提交（驳回不再成为准终态）。
     * 修复前 doReject 只翻 approveStatus=REJECTED、docStatus 停留 SUBMITTED，
     * 重提被 documentStateMachine.assertCanSubmit(仅 DRAFT) 拦截——只能作废重建。
     * 修复后 doReject 回写 docStatus=DRAFT，重提可达。
     */
    @Test
    public void testRejectThenResubmit() {
        seedBase("9121", "WO-REJECT-RESUBMIT", "2");
        String woId = seedWorkOrder("WO-REJECT-RESUBMIT", "9121");
        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__reject", Map.of("id", woId));

        // 驳回后 approveStatus=REJECTED + docStatus=DRAFT（修复后），重提应成功。
        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId), "驳回后重新提交应成功（P1-CK-mfg-002）");
        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(ErpMfgConstants.APPROVE_STATUS_SUBMITTED, wo.getApproveStatus(), "重提后 approveStatus=SUBMITTED");
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED, wo.getDocStatus(), "重提后 docStatus=SUBMITTED");
    }

    /**
     * P1-CK-mfg-002 回归：reverseApprove（未开工前提）后工单可重新提交。
     * 修复前 doReverseApprove 只翻 approveStatus=REJECTED、docStatus 停留 NOT_STARTED，重提被拦。
     * 修复后回写 docStatus=DRAFT，重提可达。
     */
    @Test
    public void testReverseApproveThenResubmit() {
        seedBase("9122", "WO-REVAPPR-RESUBMIT", "2");
        String woId = seedWorkOrder("WO-REVAPPR-RESUBMIT", "9122");
        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__reverseApprove", Map.of("id", woId));

        // 反审核后 docStatus=DRAFT（修复后），重提应成功。
        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId), "反审核后重新提交应成功（P1-CK-mfg-002）");
        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(ErpMfgConstants.APPROVE_STATUS_SUBMITTED, wo.getApproveStatus(), "重提后 approveStatus=SUBMITTED");
    }

    // ---------- P1-CK-mfg-022-r3：reverseApprove docStatus 守卫（在制/终态拒绝） ----------

    /**
     * P1-CK-mfg-022-r3 负路径：工单 approve 后 approveStatus 恒 APPROVED（IN_PROCESS/STOCK_RESERVED/
     * STOCK_PARTIAL/STOPPED/COMPLETED/CLOSED/CANCELLED 全程不翻审批轴），对七种在制/终态 docStatus
     * 组合调 reverseApprove 须被业务异常拒绝（错误码 + docStatus/approveStatus 双轴不变）。
     * 修复前 reverseApprove 全链无 docStatus 守卫 × P1-CK-mfg-002 修复（doReverseApprove 无条件回写
     * docStatus=DRAFT）叠加 → 在制/终态工单被复活为 DRAFT 可编辑可重提态。
     */
    @Test
    public void testReverseApproveRejectedForInProcessAndTerminalDocStatus() {
        seedBase("9123", "WO-RA-GUARD", "2");
        String[][] combos = {
                {ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, "WO-RA-GUARD-IP"},
                {ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED, "WO-RA-GUARD-SR"},
                {ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL, "WO-RA-GUARD-SP"},
                {ErpMfgConstants.WORK_ORDER_STATUS_STOPPED, "WO-RA-GUARD-ST"},
                {ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, "WO-RA-GUARD-CP"},
                {ErpMfgConstants.WORK_ORDER_STATUS_CLOSED, "WO-RA-GUARD-CL"},
                {ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED, "WO-RA-GUARD-CN"},
        };
        for (String[] combo : combos) {
            String docStatus = combo[0];
            String code = combo[1];
            String woId = seedWorkOrderInStatus(code, "9123", docStatus, ErpMfgConstants.APPROVE_STATUS_APPROVED);
            ApiResponse<?> resp = rpc(mutation, "ErpMfgWorkOrder__reverseApprove", Map.of("id", woId));
            assertEquals(ErpMfgErrors.ERR_REVERSE_APPROVE_DOC_STATUS_FORBIDDEN.getErrorCode(), resp.getCode(),
                    docStatus + " 工单 reverseApprove 应被 docStatus 守卫拒绝: " + resp);
            ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
            assertEquals(docStatus, wo.getDocStatus(), docStatus + " 工单 docStatus 被守卫拒绝后保持不变");
            assertEquals(ErpMfgConstants.APPROVE_STATUS_APPROVED, wo.getApproveStatus(),
                    docStatus + " 工单 approveStatus 被守卫拒绝后保持不变");
        }
    }

    /**
     * P1-CK-mfg-022-r3 控制组：NOT_STARTED 合法路径放行且双轴回写行为与现状一致
     * （docStatus=DRAFT + approveStatus=REJECTED + 审批审计字段清空 + 可重提）——守卫不得收窄合法路径。
     */
    @Test
    public void testReverseApproveNotStartedControlGroupUnchanged() {
        seedBase("9124", "WO-RA-CTRL", "2");
        String woId = seedWorkOrder("WO-RA-CTRL", "9124");
        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", woId));
        ErpMfgWorkOrder approved = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, approved.getDocStatus(),
                "approve 后 docStatus=NOT_STARTED");
        assertEquals(ErpMfgConstants.APPROVE_STATUS_APPROVED, approved.getApproveStatus(),
                "approve 后 approveStatus=APPROVED");

        rpcOk(mutation, "ErpMfgWorkOrder__reverseApprove", Map.of("id", woId), "NOT_STARTED 工单反审核应放行");
        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT, wo.getDocStatus(), "反审核回写 docStatus=DRAFT");
        assertEquals(ErpMfgConstants.APPROVE_STATUS_REJECTED, wo.getApproveStatus(), "审批轴翻 REJECTED");
        assertNull(wo.getApprovedBy(), "审批审计字段 approvedBy 清空");
        assertNull(wo.getApprovedAt(), "审批审计字段 approvedAt 清空");

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId), "反审核后可重新提交");
    }

    // ---------- ① 审核创建预留（UC-MFG-05） ----------

    @Test
    public void testApproveCreatesReservation() {
        seedBase("9101", "WO-RSV-APPROVE", "2");
        generateIncoming(M1, "PR-RSV-AP", bd("10"), bd("5"));

        String woId = seedWorkOrder("WO-RSV-APPROVE", "9101");
        seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", woId));

        ErpInvReservation reservation = findReservation("WO-RSV-APPROVE");
        assertNotNull(reservation, "审核后应创建预留头");
        assertEquals(app.erp.inv.dao.ErpInvDaoConstants.RESERVATION_STATUS_OPEN, reservation.getStatus(),
                "头状态=OPEN（生效中）");
        assertEquals("WORK_ORDER", reservation.getSourceBillType());
        List<ErpInvReservationLine> lines = findReservationLines(reservation.getId());
        assertEquals(1, lines.size(), "每个子件一条预留行");
        ErpInvReservationLine line = lines.get(0);
        assertEquals(M1, line.getMaterialId());
        // BOM qty=2 × planned=2 → 需求 4，可用 10 → 预留 4（min 语义）
        assertEquals(0, line.getReservedQuantity().compareTo(bd("4")), "预留量 = min(需求4, 可用10) = 4");
        assertEquals(0, line.getConsumedQuantity().compareTo(bd("0")));
        assertEquals(WAREHOUSE_ID, line.getWarehouseId(), "行仓库 = WO 行 sourceWarehouseId");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("4")), "库存余额.预留量 += 4");
        assertEquals(0, findBalance(M1).getAvailableQuantity().compareTo(bd("6")), "可用量 = 10 − 4 = 6");
    }

    @Test
    public void testApproveReservesMinOfAvailable() {
        seedBase("9102", "WO-RSV-MIN", "2");
        generateIncoming(M1, "PR-RSV-MIN", bd("3"), bd("5"));

        String woId = seedWorkOrder("WO-RSV-MIN", "9102");
        seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", woId));

        ErpInvReservation reservation = findReservation("WO-RSV-MIN");
        List<ErpInvReservationLine> lines = findReservationLines(reservation.getId());
        assertEquals(0, lines.get(0).getReservedQuantity().compareTo(bd("3")),
                "预留量 = min(需求4, 可用3) = 3");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("3")));
    }

    // ---------- ② 取消释放（UC-MFG-08） ----------

    @Test
    public void testCancelReleasesReservation() {
        seedBase("9103", "WO-RSV-CANCEL", "2");
        generateIncoming(M1, "PR-RSV-CA", bd("10"), bd("5"));
        String woId = seedWorkOrder("WO-RSV-CANCEL", "9103");
        seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", woId));
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("4")), "审核后占用 4");

        rpcOk(mutation, "ErpMfgWorkOrder__cancel", Map.of("workOrderId", woId));

        ErpInvReservation reservation = findReservation("WO-RSV-CANCEL");
        assertEquals(app.erp.inv.dao.ErpInvDaoConstants.RESERVATION_STATUS_CANCELLED, reservation.getStatus(),
                "取消释放 → 头状态=CANCELLED（⑦ D2 映射）");
        List<ErpInvReservationLine> lines = findReservationLines(reservation.getId());
        assertEquals(0, lines.get(0).getReservedQuantity().compareTo(bd("0")), "未领料全释放 → 行预留量归 0");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("0")), "库存余额.预留量 -= 4");
        assertEquals(0, findBalance(M1).getAvailableQuantity().compareTo(bd("10")), "可用量恢复 10");
    }

    // ---------- ③ 完工释放未领料部分（UC-MFG-08）+ ④ 领料消耗（UC-MFG-06） ----------

    @Test
    public void testIssueConfirmAndCompleteRelease() {
        seedBase("9104", "WO-RSV-FLOW", "2");
        generateIncoming(M1, "PR-RSV-FL", bd("10"), bd("5"));
        String woId = seedWorkOrder("WO-RSV-FLOW", "9104");
        String wolId = seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));

        // ④ 领料消耗：领 M1×3（预留 4 → 消耗 3，剩 1）
        String issueId = seedIssue("MI-RSV-FLOW", woId);
        seedIssueLine("9301", issueId, M1, bd("3"), wolId);
        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));

        ErpInvReservation reservation = findReservation("WO-RSV-FLOW");
        List<ErpInvReservationLine> lines = findReservationLines(reservation.getId());
        assertEquals(0, lines.get(0).getConsumedQuantity().compareTo(bd("3")), "consumedQuantity += 3");
        assertEquals(0, lines.get(0).getReservedQuantity().compareTo(bd("4")), "行预留量保持初始 4");
        assertEquals(app.erp.inv.dao.ErpInvDaoConstants.RESERVATION_STATUS_PARTIALLY_CONSUMED,
                reservation.getStatus(), "部分领料 → PARTIALLY_CONSUMED（⑦）");
        // 余额：10 − 4(领料出库) = 6；预留 4 − 3(消耗) = 1
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("1")), "库存余额.预留量 = 4 − 3 = 1");

        // ③ 完工达量（planned=2, completed=2）→ 释放未领料部分（1）
        Map<String, Object> completeReq = new LinkedHashMap<>();
        completeReq.put("workOrderId", woId);
        completeReq.put("completedQty", bd("2"));
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);

        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, wo.getDocStatus(), "完工达量 → COMPLETED");
        ErpInvReservation after = findReservation("WO-RSV-FLOW");
        assertEquals(app.erp.inv.dao.ErpInvDaoConstants.RESERVATION_STATUS_PARTIALLY_CONSUMED, after.getStatus(),
                "完工释放剩余 → PARTIALLY_CONSUMED（⑦）");
        List<ErpInvReservationLine> afterLines = findReservationLines(after.getId());
        assertEquals(0, afterLines.get(0).getReservedQuantity().compareTo(bd("3")),
                "释放后行 reservedQuantity = consumedQuantity = 3（D2）");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("0")), "完工释放后余额预留量清零");
    }

    @Test
    public void testIssueFullConsumptionStatusConsumed() {
        seedBase("9105", "WO-RSV-FULL", "2");
        generateIncoming(M1, "PR-RSV-FU", bd("10"), bd("5"));
        String woId = seedWorkOrder("WO-RSV-FULL", "9105");
        String wolId = seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));

        String issueId = seedIssue("MI-RSV-FULL", woId);
        seedIssueLine("9302", issueId, M1, bd("4"), wolId);
        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));

        ErpInvReservation reservation = findReservation("WO-RSV-FULL");
        assertEquals(app.erp.inv.dao.ErpInvDaoConstants.RESERVATION_STATUS_CONSUMED, reservation.getStatus(),
                "领料领完 → CONSUMED（⑦）");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("0")), "余额预留量清零");
    }

    // ---------- P1-CK-mfg-003：领料红冲回退预留消耗（闭环回归） ----------

    /**
     * P1-CK-mfg-003 回归：reverseConfirm 红冲后预留消耗闭环回退。
     * 修复前 confirm 消耗预留（consumedQuantity+= / 余额预留量-=）后红冲只回滚 GL 与库存移动单，
     * 预留 consumedQuantity 单向残留——重领料超预留、完工释放量失真。
     * 修复后红冲镜像回退：consumedQuantity 3→0、余额预留量 1→4、工单 materialCost/totalCost 归零、
     * 行 actualQuantity 回退。
     */
    @Test
    public void testReverseConfirmRestoresReservationConsumed() {
        seedPeriodAndSubjects();
        seedBase("9111", "WO-RSV-REV", "2");
        generateIncoming(M1, "PR-RSV-RV", bd("10"), bd("5"), ACCT_SCHEMA_ID);
        String woId = seedWorkOrder("WO-RSV-REV", "9111");
        String wolId = seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));

        String issueId = seedIssue("MI-RSV-REV", woId);
        seedIssueLine("9309", issueId, M1, bd("3"), wolId);
        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));

        // 前置：confirm 消耗预留（consumed=3、余额预留 4−3=1）+ 工单材料成本 3×5=15
        ErpInvReservation before = findReservation("WO-RSV-REV");
        assertNotNull(before, "confirm 前置：预留头应存在");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("1")),
                "confirm 后余额预留量 = 4 − 3 = 1");
        ErpMfgWorkOrder woBefore = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertTrue(nz(woBefore.getMaterialCost()).signum() > 0, "confirm 后工单材料成本 > 0");

        // 红冲：预留消耗回退 + 工单成本/行量回退
        rpcOk(mutation, "ErpMfgMaterialIssue__reverseConfirm", Map.of("issueId", issueId));

        ErpInvReservation after = findReservation("WO-RSV-REV");
        List<ErpInvReservationLine> afterLines = findReservationLines(after.getId());
        assertEquals(0, afterLines.get(0).getConsumedQuantity().compareTo(bd("0")),
                "红冲后 consumedQuantity 回退 0");
        assertEquals(0, afterLines.get(0).getReservedQuantity().compareTo(bd("4")),
                "行预留量保持初始 4");
        assertEquals(app.erp.inv.dao.ErpInvDaoConstants.RESERVATION_STATUS_OPEN, after.getStatus(),
                "全回退 → OPEN（⑦）");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("4")),
                "余额预留量恢复 4");
        ErpMfgWorkOrder woAfter = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(0, nz(woAfter.getMaterialCost()).compareTo(BigDecimal.ZERO),
                "红冲后 materialCost 归零");
        assertEquals(0, nz(woAfter.getTotalCost()).compareTo(BigDecimal.ZERO),
                "红冲后 totalCost 归零");
    }

    // ---------- ⑤ 超预留警告放行（D1） ----------

    @Test
    public void testOverPickWarnsAndPasses() {
        seedBase("9106", "WO-RSV-OVER", "2");
        generateIncoming(M1, "PR-RSV-OV", bd("10"), bd("5"));
        String woId = seedWorkOrder("WO-RSV-OVER", "9106");
        String wolId = seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));

        // 领 6 > 预留 4 → over-pick-warning=true LOG.warn 放行（不阻断领料主链）
        String issueId = seedIssue("MI-RSV-OVER", woId);
        seedIssueLine("9303", issueId, M1, bd("6"), wolId);
        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId),
                "超预留 confirm 应放行（D1 warn 不阻断）");

        ErpInvReservation reservation = findReservation("WO-RSV-OVER");
        List<ErpInvReservationLine> lines = findReservationLines(reservation.getId());
        assertEquals(0, lines.get(0).getConsumedQuantity().compareTo(bd("4")),
                "超预留按 min 封顶：消耗 = 预留 4（非 6）");
        assertEquals(app.erp.inv.dao.ErpInvDaoConstants.RESERVATION_STATUS_CONSUMED, reservation.getStatus());
        // 库存侧：现有量 10 − 6（出库） = 4；预留清零
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("0")));
        assertEquals(0, findBalance(M1).getTotalQuantity().compareTo(bd("4")), "领料主链扣减不受预留影响");
    }

    // ---------- ⑥ config 关闭全链跳过 ----------

    @Test
    public void testConfigOffSkipsReservationChain() {
        seedBase("9107", "WO-RSV-OFF", "2");
        generateIncoming(M1, "PR-RSV-OF", bd("10"), bd("5"));
        String woId = seedWorkOrder("WO-RSV-OFF", "9107");
        String wolId = seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        setConfig(ErpMfgConstants.CONFIG_RESERVATION_ENABLED, "false");
        try {
            rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
            rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", woId));
            assertNull(findReservation("WO-RSV-OFF"), "config 关闭 → 不创建预留");

            rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));
            String issueId = seedIssue("MI-RSV-OFF", woId);
            seedIssueLine("9304", issueId, M1, bd("4"), wolId);
            rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId),
                    "config 关闭 → 领料消耗跳过");
            Map<String, Object> completeReq = new LinkedHashMap<>();
            completeReq.put("workOrderId", woId);
            completeReq.put("completedQty", bd("2"));
            rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);
            ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
            assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, wo.getDocStatus(), "完工主链不受影响");
            assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("0")), "余额预留量恒 0");
        } finally {
            setConfig(ErpMfgConstants.CONFIG_RESERVATION_ENABLED, "true");
        }
    }

    // ---------- ⑧ 无预留工单（旧数据）no-op 语义 + ⑨ 无 BOM approve 不阻断 ----------

    @Test
    public void testLegacyWorkOrderNoReservationNoOp() {
        // 工单行无 sourceWarehouseId → 审核跳过预留创建（MINOR-8），后续 cancel/confirm/complete 全部 no-op
        seedBase("9108", "WO-RSV-LEGACY", "2");
        generateIncoming(M1, "PR-RSV-LE", bd("10"), bd("5"));
        String woId = seedWorkOrder("WO-RSV-LEGACY", "9108");
        String wolId = seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null, null);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", woId));
        assertNull(findReservation("WO-RSV-LEGACY"), "无领料仓库行 → 不创建预留");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("0")), "余额零占用");

        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));
        String issueId = seedIssue("MI-RSV-LEGACY", woId);
        seedIssueLine("9305", issueId, M1, bd("4"), wolId);
        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId),
                "无预留工单 confirm 不抛异常零写入");
        Map<String, Object> completeReq = new LinkedHashMap<>();
        completeReq.put("workOrderId", woId);
        completeReq.put("completedQty", bd("2"));
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);
        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, wo.getDocStatus(), "完工主链正常");
        assertEquals(0, findBalance(M1).getReservedQuantity().compareTo(bd("0")), "全程零预留写入");

        // cancel 路径 no-op（新工单走 cancel；物料已 seed，仅补 BOM + WO + 余额）
        seedBom("9109", P, M1, bd("2"));
        generateIncoming(M1, "PR-RSV-LE2", bd("10"), bd("5"));
        String wo2 = seedWorkOrder("WO-RSV-LEGACY2", "9109");
        seedWorkOrderLine(wo2, M1, bd("2"), "INPUT", null, null);
        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", wo2));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", wo2));
        rpcOk(mutation, "ErpMfgWorkOrder__cancel", Map.of("workOrderId", wo2),
                "无预留工单 cancel 不抛异常");
    }

    @Test
    public void testNoBomApproveNotBlocked() {
        // 无 bomId 且无默认 BOM → approve 跳过预留创建不阻断（MINOR-5）
        seedMaterial(P, null);
        String woId = String.valueOf(8300L + (long) Math.abs("WO-RSV-NOBOM".hashCode() % 700));
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", woId);
            wo.setCode("WO-RSV-NOBOM");
            wo.setProductId(P);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(bd("1"));
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
            dao.saveEntity(wo);
        });

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", woId),
                "无 BOM 工单 approve 不阻断（跳过预留 LOG.warn）");
        assertNull(findReservation("WO-RSV-NOBOM"), "无 BOM → 不创建预留");
    }

    // ---------- ⑩ 跨工单并发预留探针（A4.2.3 MA4 回队义务，无条件新增） ----------

    /**
     * 跨工单并发预留 lost-update 防护运行时核验（A4.2.3）：两工单同物料经 mfg approve 集成层并发建预留
     * （镜像 {@code TestErpInvReservationWriteApi#testConcurrentCreateReservationNoLostUpdate}
     * ExecutorService + CountDownLatch 模式，但经 {@code ErpMfgWorkOrder__approve} 集成层
     * → {@code createReservations} → {@code IErpInvReservationBiz.createReservation}
     * → {@code StockMoveBookkeeper.updateBalanceWithRetry} 乐观锁重试）。
     *
     * <p>断言：两工单预留均落库 + reservedQuantity 累加无丢失（4 + 4 = 8）+ available = total − reserved
     * 恒等式保持（10 − 8 = 2）+ 无异常/无重试耗尽。
     */
    @Test
    public void testConcurrentCrossWorkOrderApproveNoLostUpdate() throws Exception {
        seedBase("9111", "WO-RSV-CONC-A", "2");
        generateIncoming(M1, "PR-RSV-CONC", bd("10"), bd("5"));

        String woA = seedWorkOrder("WO-RSV-CONC-A", "9111");
        seedWorkOrderLine(woA, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woA, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);
        String woB = seedWorkOrder("WO-RSV-CONC-B", "9111");
        seedWorkOrderLine(woB, M1, bd("2"), "INPUT", null, WAREHOUSE_ID);
        seedWorkOrderLine(woB, P, bd("1"), "OUTPUT", WAREHOUSE_ID, null);

        int threadCount = 2;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicReference<Throwable> firstError = new AtomicReference<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                final String woId = i == 0 ? woA : woB;
                pool.submit(() -> {
                    ContextProvider.newContext();
                    try {
                        startGate.await();
                        ApiResponse<?> submitResp = rpc(mutation, "ErpMfgWorkOrder__submitForApproval",
                                Map.of("id", woId));
                        ApiResponse<?> approveResp = rpc(mutation, "ErpMfgWorkOrder__approve",
                                Map.of("id", woId));
                        if (submitResp.getStatus() != 0) {
                            throw new AssertionError("工单 " + woId + " submitForApproval 失败: " + submitResp);
                        }
                        if (approveResp.getStatus() != 0) {
                            throw new AssertionError("工单 " + woId + " approve 失败: " + approveResp);
                        }
                    } catch (Throwable t) {
                        firstError.compareAndSet(null, t);
                    } finally {
                        ContextProvider.instance().detachContext();
                        doneLatch.countDown();
                    }
                });
            }
            startGate.countDown();
            assertTrue(doneLatch.await(60, TimeUnit.SECONDS), "全部 worker 应在 60s 内完成");
            if (firstError.get() != null) {
                throw new AssertionError("worker 线程抛错: " + firstError.get().getMessage(), firstError.get());
            }
        } finally {
            pool.shutdownNow();
        }

        // 两工单预留均落库（无丢失）
        ErpInvReservation resA = findReservation("WO-RSV-CONC-A");
        assertNotNull(resA, "工单 A 审核后应创建预留头");
        List<ErpInvReservationLine> linesA = findReservationLines(resA.getId());
        assertEquals(1, linesA.size(), "工单 A 每个子件一条预留行");
        assertEquals(0, linesA.get(0).getReservedQuantity().compareTo(bd("4")),
                "工单 A 预留量 = min(需求 2×2=4, 可用 10) = 4");

        ErpInvReservation resB = findReservation("WO-RSV-CONC-B");
        assertNotNull(resB, "工单 B 审核后应创建预留头");
        List<ErpInvReservationLine> linesB = findReservationLines(resB.getId());
        assertEquals(1, linesB.size(), "工单 B 每个子件一条预留行");
        assertEquals(0, linesB.get(0).getReservedQuantity().compareTo(bd("4")),
                "工单 B 预留量 = min(需求 2×2=4, 可用) = 4");

        // reservedQuantity 累加无丢失：4 + 4 = 8；available = total − reserved 恒等式保持
        ErpInvStockBalance balance = findBalance(M1);
        assertEquals(0, balance.getTotalQuantity().compareTo(bd("10")), "total 守恒 = 10");
        assertEquals(0, balance.getReservedQuantity().compareTo(bd("8")),
                "跨工单并发预留：4 + 4 = 8（无丢失更新，乐观锁重试串行化）");
        assertEquals(0, balance.getAvailableQuantity().compareTo(bd("2")),
                "available = total − reserved = 10 − 8 = 2");
    }

    // ---------- helpers ----------

    private void seedBase(String bomId, String woCode, String bomQty) {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom(bomId, P, M1, bd(bomQty));
    }

    private void generateIncoming(String materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        generateIncoming(materialId, billCode, qty, unitCost, null);
    }

    private void generateIncoming(String materialId, String billCode, BigDecimal qty, BigDecimal unitCost,
                                  String acctSchemaId) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("currencyId", CURRENCY_ID);
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("acctSchemaId", acctSchemaId);
        req.put("relatedBillType", "PUR_RECEIPT");
        req.put("relatedBillCode", billCode);
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", materialId);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        line.put("unitCost", unitCost);
        line.put("currencyId", CURRENCY_ID);
        req.put("lines", Collections.singletonList(line));
        rpcOk(mutation, "ErpInvStockMove__generateMove", Map.of("request", req));
    }

    private void seedMaterial(String id, String costMethod) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdMaterial> dao = daoProvider.daoFor(ErpMdMaterial.class);
            ErpMdMaterial m = new ErpMdMaterial();
            m.orm_propValueByName("id", id);
            m.setCode("MAT-" + id);
            m.setName("Material " + id);
            m.orm_propValueByName("materialType", "GOODS");
            m.setUoMId(UOM_ID);
            m.setStatus("ACTIVE");
            m.setCostMethod(costMethod);
            dao.saveEntity(m);
        });
    }

    private void seedBom(String bomId, String productId, String componentId, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = new ErpMfgBom();
            bom.orm_propValueByName("id", bomId);
            bom.setCode("BOM-" + bomId);
            bom.setProductId(productId);
            bom.setBomType(ErpMfgConstants.BOM_TYPE_MANUFACTURED);
            bom.setIsDefault(Boolean.TRUE);
            bom.setIsActive(Boolean.TRUE);
            bom.setQty(bd("1"));
            dao.saveEntity(bom);
            IEntityDao<ErpMfgBomLine> ldao = daoProvider.daoFor(ErpMfgBomLine.class);
            ErpMfgBomLine line = new ErpMfgBomLine();
            line.orm_propValueByName("id", String.valueOf(Long.parseLong(bomId) + 50000));
            line.setBomId(bomId);
            line.setLineNo(10);
            line.setMaterialId(componentId);
            line.setUoMId(UOM_ID);
            line.setQuantity(qty);
            ldao.saveEntity(line);
        });
    }

    private String seedWorkOrder(String code, String bomId) {
        String id = String.valueOf(8300L + (long) Math.abs(code.hashCode() % 700));
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(P);
            wo.setBomId(bomId);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(bd("2"));
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
            dao.saveEntity(wo);
        });
        return id;
    }

    private String seedWorkOrderInStatus(String code, String bomId, String docStatus, String approveStatus) {
        String id = String.valueOf(8300L + (long) Math.abs(code.hashCode() % 700));
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(P);
            wo.setBomId(bomId);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(bd("2"));
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(docStatus);
            wo.setApproveStatus(approveStatus);
            dao.saveEntity(wo);
        });
        return id;
    }

    private String seedWorkOrderLine(String woId, String materialId, BigDecimal plannedQty, String lineType,
                                     String destWarehouseId, String sourceWarehouseId) {
        long raw = (woId + "" + materialId + lineType).hashCode();
        String id = String.valueOf(9300L + (long) Math.abs(raw % 700));
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrderLine> dao = daoProvider.daoFor(ErpMfgWorkOrderLine.class);
            ErpMfgWorkOrderLine wol = new ErpMfgWorkOrderLine();
            wol.orm_propValueByName("id", id);
            wol.setWorkOrderId(woId);
            wol.setLineNo(Integer.parseInt(materialId));
            wol.orm_propValueByName("lineType", lineType);
            wol.setMaterialId(materialId);
            wol.setUoMId(UOM_ID);
            wol.setPlannedQuantity(plannedQty);
            wol.setDestWarehouseId(destWarehouseId);
            wol.setSourceWarehouseId(sourceWarehouseId);
            dao.saveEntity(wol);
        });
        return id;
    }

    private String seedIssue(String code, String woId) {
        String id = String.valueOf(8400L + (long) Math.abs(code.hashCode() % 700));
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMaterialIssue> dao = daoProvider.daoFor(ErpMfgMaterialIssue.class);
            ErpMfgMaterialIssue issue = new ErpMfgMaterialIssue();
            issue.orm_propValueByName("id", id);
            issue.setCode(code);
            issue.setWorkOrderId(woId);
            issue.setOrgId(ORG_ID);
            issue.setWarehouseId(WAREHOUSE_ID);
            issue.setBusinessDate(LocalDate.of(2026, 7, 1));
            issue.setCurrencyId(CURRENCY_ID);
            issue.setDocStatus(ErpMfgConstants.ISSUE_STATUS_DRAFT);
            issue.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
            dao.saveEntity(issue);
        });
        return id;
    }

    private void seedIssueLine(String id, String issueId, String materialId, BigDecimal qty, String wolId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgMaterialIssueLine> dao = daoProvider.daoFor(ErpMfgMaterialIssueLine.class);
            ErpMfgMaterialIssueLine line = new ErpMfgMaterialIssueLine();
            line.orm_propValueByName("id", id);
            line.setIssueId(issueId);
            line.setLineNo(10);
            line.setMaterialId(materialId);
            line.setUoMId(UOM_ID);
            line.setRequiredQuantity(qty);
            line.setIssuedQuantity(qty);
            line.setWorkOrderLineId(wolId);
            dao.saveEntity(line);
        });
    }

    // ---------- query helpers ----------

    private ErpInvReservation findReservation(String workOrderCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("sourceBillType", ErpMfgConstants.SOURCE_BILL_TYPE_WORK_ORDER));
        q.addFilter(eq("sourceBillCode", workOrderCode));
        List<ErpInvReservation> list = daoProvider.daoFor(ErpInvReservation.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpInvReservationLine> findReservationLines(String reservationId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("reservationId", reservationId));
        q.addOrderField("lineNo", false);
        return daoProvider.daoFor(ErpInvReservationLine.class).findAllByQuery(q);
    }

    private ErpInvStockBalance findBalance(String materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = daoProvider.daoFor(ErpInvStockBalance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ApiResponse<?> rpc(GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
    }

    private void rpcOk(GraphQLOperationType op, String action, Map<String, Object> args, String msg) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), msg + ": " + resp);
    }

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    // ---------- finance seed（领料红冲需过账 → 期间/账套/科目） ----------

    private void seedPeriodAndSubjects() {
        ormTemplate.runInSession(() -> {
            seedAcctSchema();
            seedOpenPeriod();
            seedSubject(SUBJECT_INVENTORY, "原材料存货", "ASSET", "DEBIT");
            seedSubject(SUBJECT_WIP, "在制品", "ASSET", "DEBIT");
        });
    }

    private void seedAcctSchema() {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.orm_propValueByName("id", ACCT_SCHEMA_ID);
        schema.setCode("ACCT-" + ORG_ID);
        schema.setName("账套 " + ORG_ID);
        schema.setOrgId(ORG_ID);
        schema.orm_propValueByName("nature", "FINANCIAL");
        schema.setFunctionalCurrencyId(CURRENCY_ID);
        schema.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod() {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode("2026-07-RSV-REV");
        period.setName("2026-07-RSV-REV");
        period.setOrgId(ORG_ID);
        period.orm_propValueByName("year", 2026);
        period.orm_propValueByName("month", 7);
        period.setStartDate(LocalDate.of(2026, 7, 1));
        period.setEndDate(LocalDate.of(2026, 7, 31));
        period.orm_propValueByName("status", "OPEN");
        dao.saveEntity(period);
    }

    private void seedSubject(String code, String name, String subjectClass, String direction) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject subject = new ErpMdSubject();
        subject.setCode(code);
        subject.setName(name);
        subject.orm_propValueByName("subjectClass", subjectClass);
        subject.orm_propValueByName("direction", direction);
        subject.orm_propValueByName("status", "ACTIVE");
        dao.saveEntity(subject);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
