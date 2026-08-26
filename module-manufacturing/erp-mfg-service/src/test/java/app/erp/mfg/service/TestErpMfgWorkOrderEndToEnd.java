package app.erp.mfg.service;

import app.erp.inv.dao.entity.ErpInvStockBalance;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.biz.JobCardWorkRecord;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgJobCard;
import app.erp.mfg.dao.entity.ErpMfgJobCardTimeLog;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 4 端到端测试：工单创建→审批→齐套→开工→领料出库→报工成本归集→完工入库→COMPLETED
 * + WorkOrder.materialCost/laborCost/totalCost/unitCost 正确 + 完工质检门控。
 *
 * <p>覆盖 {@code docs/design/manufacturing/state-machine.md §场景 A：正常生产 happy path}。
 * 验证报工人工成本（durationMins/60 × hourlyRate）与领料材料成本（出库 ledger.totalCost）正确归集到工单。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgWorkOrderEndToEnd extends JunitAutoTestCase {

    @RegisterExtension
    static MfgFrozenClockExtension frozenClock = new MfgFrozenClockExtension();

    static final String ORG_ID = "1401";
    static final String WAREHOUSE_ID = "3401";
    static final String UOM_ID = "5401";
    static final String CURRENCY_ID = "6401";
    static final String P = "1101";     // 产成品
    static final String M1 = "1102";    // 子件
    static final String MOVE_TYPE_INCOMING = "INCOMING";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testEndToEndIssueReportCompletion() {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom("9101", P, M1, bd("2"));
        generateIncoming(M1, "PR-E2E-001", bd("10"), bd("5"));   // M1: 10 @ avgCost 5

        String woId = seedWorkOrder("WO-E2E", "9101", bd("1"));
        String wolId = seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID);

        // 工单流转到 IN_PROCESS
        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));

        // 领料出库：领 M1×2，材料成本 = 2×5 = 10
        String issueId = seedIssue("MI-E2E", woId);
        seedIssueLine("9301", issueId, M1, bd("2"), wolId);
        ApiResponse<?> issueConfirmResp = rpc(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));
        assertEquals(0, issueConfirmResp.getStatus(), "ErpMfgMaterialIssue__confirm 应成功: " + issueConfirmResp);
        output("1_issue_confirm_response.json5", issueConfirmResp);

        // 报工：JobCard 录工时 60 分钟 × 费率 30 → 人工成本 = 60/60×30 = 30
        String jobCardId = seedJobCard(woId);
        rpcOk(mutation, "ErpMfgJobCard__startJob", Map.of("jobCardId", jobCardId));
        ApiResponse<?> rwResp = recordWorkRequest(jobCardId, bd("60"), bd("30"), bd("1"));
        assertEquals(0, rwResp.getStatus(), "recordWork 应成功: " + rwResp);
        output("2_record_work_response.json5", rwResp);

        ErpMfgJobCardTimeLog timeLog = findTimeLog(jobCardId);
        assertEquals(0, timeLog.getLaborCost().compareTo(bd("30")), "人工成本 = 60/60×30 = 30");

        // 完工入库：完工 1 件 → COMPLETED
        Map<String, Object> completeReq = new LinkedHashMap<>();
        completeReq.put("workOrderId", woId);
        completeReq.put("completedQty", bd("1"));
        ApiResponse<?> completeResp = rpc(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);
        assertEquals(0, completeResp.getStatus(), "reportCompletion 应成功: " + completeResp);
        output("3_report_completion_response.json5", completeResp);

        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, wo.getDocStatus(), "完工达量 → COMPLETED");
        assertEquals(0, wo.getCompletedQuantity().compareTo(bd("1")), "完工数量 = 1");
        assertEquals(0, wo.getMaterialCost().compareTo(bd("10")), "材料成本 = 10");
        assertEquals(0, wo.getLaborCost().compareTo(bd("30")), "人工成本 = 30");
        assertEquals(0, wo.getTotalCost().compareTo(bd("40")), "总成本 = 10+30+0+0 = 40");
        assertEquals(0, wo.getUnitCost().compareTo(bd("40")), "单位成本 = 40/1 = 40");
        java.util.Map<String, Object> woState = new java.util.LinkedHashMap<>();
        woState.put("id", wo.getId());
        woState.put("code", wo.getCode());
        woState.put("docStatus", wo.getDocStatus());
        woState.put("completedQuantity", wo.getCompletedQuantity());
        woState.put("materialCost", wo.getMaterialCost());
        woState.put("laborCost", wo.getLaborCost());
        woState.put("totalCost", wo.getTotalCost());
        woState.put("unitCost", wo.getUnitCost());
        output("4_workorder_final_state.json5", woState);

        // 完工入库移动单生成（MANUFACTURING，产成品 P 入库）
        ErpInvStockMove completionMove = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER, "WO-E2E");
        assertNotNull(completionMove, "应生成完工入库移动单");
        assertEquals(ErpMfgConstants.MOVE_TYPE_MANUFACTURING, completionMove.getMoveType(), "完工入库用 MANUFACTURING");

        // 产成品 P 入库 → 余额 = 1
        ErpInvStockBalance pBalance = findBalance(P);
        assertNotNull(pBalance, "产成品应入库");
        assertEquals(0, pBalance.getTotalQuantity().compareTo(bd("1")), "产成品 P 入库 1 件");
    }

    /**
     * P0-CK-mfg-001（ai-check F1.1 Phase 2）：增量报工不被固定幂等键吞掉——
     * 首张完工移动单保持 wo.code 精确键（既有消费者兼容），后续报工追加 "-C{累计完工量}" 后缀，
     * 每次报工生成独立移动单且数量守恒（Σ移动行数量 = completedQuantity）。
     */
    @Test
    public void testIncrementalReportCompletionGeneratesSeparateMoves() {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom("9103", P, M1, bd("2"));
        generateIncoming(M1, "PR-E2E-INCR", bd("10"), bd("5"));

        String woId = seedWorkOrder("WO-INCR", "9103", bd("5"));
        String wolId = seedWorkOrderLine(woId, M1, bd("4"), "INPUT", null);
        seedWorkOrderLine(woId, P, bd("5"), "OUTPUT", WAREHOUSE_ID);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));

        String issueId = seedIssue("MI-INCR", woId);
        seedIssueLine("9302", issueId, M1, bd("4"), wolId);
        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));

        // 第一次报工 3 件：移动单 relatedBillCode == wo.code（精确，首单）
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion",
                Map.of("workOrderId", woId, "completedQty", bd("3")));
        ErpInvStockMove firstMove = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER, "WO-INCR");
        assertNotNull(firstMove, "首次报工应生成完工移动单");
        assertEquals("WO-INCR", firstMove.getRelatedBillCode(), "首张完工移动单保持 wo.code 精确键（F1.1 既有消费者兼容）");
        BigDecimal firstQty = ormTemplate.runInSession(sess -> daoProvider
                .daoFor(ErpInvStockMove.class).getEntityById(firstMove.getId()).getLines().stream()
                .findFirst().orElseThrow().getQuantity());
        assertEquals(0, firstQty.compareTo(bd("3")), "首张移动单数量 = 本次增量 3");

        // 第二次报工 2 件（累计 5，未达计划 5+1=6 前不触发完成态——本例 OUTPUT 计划 5，累计即达计划转 COMPLETED）
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion",
                Map.of("workOrderId", woId, "completedQty", bd("2")));

        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(0, wo.getCompletedQuantity().compareTo(bd("5")), "累计完工 = 3+2 = 5");

        // 第二张移动单：后缀键（"-C5" = 累计完工量 5）
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER));
        q.addFilter(eq("relatedBillCode", "WO-INCR-C5"));
        List<ErpInvStockMove> second = daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q);
        assertEquals(1, second.size(), "第二次报工应生成第二张完工移动单（P0-CK-mfg-001 修复前被固定幂等键吞掉）");
        BigDecimal secondQty = ormTemplate.runInSession(sess -> daoProvider
                .daoFor(ErpInvStockMove.class).getEntityById(second.get(0).getId()).getLines().stream()
                .findFirst().orElseThrow().getQuantity());
        assertEquals(0, secondQty.compareTo(bd("2")), "第二张移动单数量 = 本次增量 2");

        // 数量守恒：库存累计入库 = 累计完工量
        ErpInvStockBalance pBalance = findBalance(P);
        assertNotNull(pBalance, "产成品应入库");
        assertEquals(0, pBalance.getTotalQuantity().compareTo(bd("5")), "产成品入库累计 = 3+2 = 5 = completedQuantity");
    }

    @Test
    public void testInspectionGateBlocksCompletionWhenEnabled() {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom("9102", P, M1, bd("1"));
        seedBomInspectionRequired("9102", true);
        generateIncoming(M1, "PR-E2E-GATE", bd("10"), bd("5"));

        String woId = seedWorkOrder("WO-GATE", "9102", bd("1"));
        seedWorkOrderLine(woId, M1, bd("1"), "INPUT", null);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID);

        rpcOk(mutation, "ErpMfgWorkOrder__submitForApproval", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("id", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));

        setConfig(ErpMfgConstants.CONFIG_INSPECTION_GATE_ENABLED, "true");
        try {
            Map<String, Object> req = new LinkedHashMap<>();
            req.put("workOrderId", woId);
            req.put("completedQty", bd("1"));
            ApiResponse<?> resp = rpc(mutation, "ErpMfgWorkOrder__reportCompletion", req);
            assertEquals(ErpMfgErrors.ERR_INSPECTION_REQUIRED.getErrorCode(), resp.getCode(),
                    "BOM 要求质检 + gate 开启 + 达量 → 拒绝完工待质检");
            ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
            assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, wo.getDocStatus(),
                    "工单保持 IN_PROCESS 待质检");
            assertNotEquals(0, resp.getStatus());
        } finally {
            setConfig(ErpMfgConstants.CONFIG_INSPECTION_GATE_ENABLED, "false");
        }

        // gate 关闭后 → 正常完工
        Map<String, Object> req2 = new LinkedHashMap<>();
        req2.put("workOrderId", woId);
        req2.put("completedQty", bd("1"));
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", req2);
        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, wo.getDocStatus(),
                "gate=false 跳过质检 → 正常完工");
    }

    @Test
    public void testJobCardStateMachine() {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom("9103", P, M1, bd("1"));
        generateIncoming(M1, "PR-E2E-JC", bd("10"), bd("5"));
        String woId = seedWorkOrder("WO-JC", "9103", bd("1"));
        seedWorkOrderLine(woId, M1, bd("1"), "INPUT", null);
        String jobCardId = seedJobCard(woId);

        rpcOk(mutation, "ErpMfgJobCard__startJob", Map.of("jobCardId", jobCardId));
        assertEquals(ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS, statusOf(jobCardId));
        rpcOk(mutation, "ErpMfgJobCard__holdJob", Map.of("jobCardId", jobCardId));
        assertEquals(ErpMfgConstants.JOB_CARD_STATUS_ON_HOLD, statusOf(jobCardId));
        rpcOk(mutation, "ErpMfgJobCard__resumeJob", Map.of("jobCardId", jobCardId));
        assertEquals(ErpMfgConstants.JOB_CARD_STATUS_WORK_IN_PROGRESS, statusOf(jobCardId));
        rpcOk(mutation, "ErpMfgJobCard__submitJob", Map.of("jobCardId", jobCardId));
        assertEquals(ErpMfgConstants.JOB_CARD_STATUS_SUBMITTED, statusOf(jobCardId));
        rpcOk(mutation, "ErpMfgJobCard__completeJob", Map.of("jobCardId", jobCardId));
        assertEquals(ErpMfgConstants.JOB_CARD_STATUS_COMPLETED, statusOf(jobCardId));
    }

    // ---------- helpers ----------

    private String statusOf(String jobCardId) {
        ErpMfgJobCard jc = daoProvider.daoFor(ErpMfgJobCard.class).getEntityById(jobCardId);
        return jc.getStatus();
    }

    private ApiResponse<?> recordWorkRequest(String jobCardId, BigDecimal durationMins, BigDecimal hourlyRate,
                                             BigDecimal completedQty) {
        // @RequestBean 被 GraphQL 展平为独立参数（非 record 包装）
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("jobCardId", jobCardId);
        args.put("operatorId", "OP-001");
        args.put("workDate", "2026-07-01");
        args.put("durationMins", durationMins);
        args.put("setupMins", BigDecimal.ZERO);
        args.put("runMins", durationMins);
        args.put("hourlyRate", hourlyRate);
        args.put("completedQuantity", completedQty);
        args.put("scrappedQuantity", BigDecimal.ZERO);
        return rpc(mutation, "ErpMfgJobCard__recordWork", args);
    }

    private void generateIncoming(String materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("currencyId", CURRENCY_ID);
        req.put("destWarehouseId", WAREHOUSE_ID);
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

    private void seedBomInspectionRequired(String bomId, boolean required) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = dao.getEntityById(bomId);
            bom.setInspectionRequired(required);
            dao.updateEntity(bom);
        });
    }

    private String seedWorkOrder(String code, String bomId, java.math.BigDecimal plannedQty) {
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
            wo.setPlannedQuantity(plannedQty);
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
            dao.saveEntity(wo);
        });
        return id;
    }

    private String seedWorkOrderLine(String woId, String materialId, BigDecimal plannedQty, String lineType,
                                     String destWarehouseId) {
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

    private String seedJobCard(String woId) {
        String id = String.valueOf(8500L + (long) Math.abs((woId + "jc").hashCode() % 700));
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgJobCard> dao = daoProvider.daoFor(ErpMfgJobCard.class);
            ErpMfgJobCard jc = new ErpMfgJobCard();
            jc.orm_propValueByName("id", id);
            jc.setWorkOrderId(woId);
            jc.setLineNo(10);
            jc.setPlannedQuantity(bd("1"));
            jc.setStatus(ErpMfgConstants.JOB_CARD_STATUS_OPEN);
            jc.setCode("JC-" + id);
            dao.saveEntity(jc);
        });
        return id;
    }

    // ---------- query helpers ----------

    private ErpMfgJobCardTimeLog findTimeLog(String jobCardId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("jobCardId", jobCardId));
        q.addOrderField("id", true);
        List<ErpMfgJobCardTimeLog> list = daoProvider.daoFor(ErpMfgJobCardTimeLog.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvStockMove findMove(String billType, String billCode) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedBillType", billType));
        q.addFilter(eq("relatedBillCode", billCode));
        List<ErpInvStockMove> list = daoProvider.daoFor(ErpInvStockMove.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpInvStockBalance findBalance(String materialId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("materialId", materialId));
        q.addFilter(eq("warehouseId", WAREHOUSE_ID));
        List<ErpInvStockBalance> list = daoProvider.daoFor(ErpInvStockBalance.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
    }

    private void setConfig(String key, String value) {
        io.nop.api.core.config.AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
