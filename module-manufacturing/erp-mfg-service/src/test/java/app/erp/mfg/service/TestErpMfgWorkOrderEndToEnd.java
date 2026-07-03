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

    static final Long ORG_ID = 1401L;
    static final Long WAREHOUSE_ID = 3401L;
    static final Long UOM_ID = 5401L;
    static final Long CURRENCY_ID = 6401L;
    static final Long P = 1101L;     // 产成品
    static final Long M1 = 1102L;    // 子件
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
        seedBom(9101L, P, M1, bd("2"));
        generateIncoming(M1, "PR-E2E-001", bd("10"), bd("5"));   // M1: 10 @ avgCost 5

        Long woId = seedWorkOrder("WO-E2E", 9101L);
        Long wolId = seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID);

        // 工单流转到 IN_PROCESS
        rpcOk(mutation, "ErpMfgWorkOrder__submit", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__checkAvailability", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__start", Map.of("workOrderId", woId));

        // 领料出库：领 M1×2，材料成本 = 2×5 = 10
        Long issueId = seedIssue("MI-E2E", woId);
        seedIssueLine(9301L, issueId, M1, bd("2"), wolId);
        rpcOk(mutation, "ErpMfgMaterialIssue__confirm", Map.of("issueId", issueId));

        // 报工：JobCard 录工时 60 分钟 × 费率 30 → 人工成本 = 60/60×30 = 30
        Long jobCardId = seedJobCard(woId);
        rpcOk(mutation, "ErpMfgJobCard__startJob", Map.of("jobCardId", jobCardId));
        ApiResponse<?> rwResp = recordWorkRequest(jobCardId, bd("60"), bd("30"), bd("1"));
        assertEquals(0, rwResp.getStatus(), "recordWork 应成功: " + rwResp);

        ErpMfgJobCardTimeLog timeLog = findTimeLog(jobCardId);
        assertEquals(0, timeLog.getLaborCost().compareTo(bd("30")), "人工成本 = 60/60×30 = 30");

        // 完工入库：完工 1 件 → COMPLETED
        Map<String, Object> completeReq = new LinkedHashMap<>();
        completeReq.put("workOrderId", woId);
        completeReq.put("completedQty", bd("1"));
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);

        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        assertEquals(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED, wo.getDocStatus(), "完工达量 → COMPLETED");
        assertEquals(0, wo.getCompletedQuantity().compareTo(bd("1")), "完工数量 = 1");
        assertEquals(0, wo.getMaterialCost().compareTo(bd("10")), "材料成本 = 10");
        assertEquals(0, wo.getLaborCost().compareTo(bd("30")), "人工成本 = 30");
        assertEquals(0, wo.getTotalCost().compareTo(bd("40")), "总成本 = 10+30+0+0 = 40");
        assertEquals(0, wo.getUnitCost().compareTo(bd("40")), "单位成本 = 40/1 = 40");

        // 完工入库移动单生成（MANUFACTURING，产成品 P 入库）
        ErpInvStockMove completionMove = findMove(ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER, "WO-E2E");
        assertNotNull(completionMove, "应生成完工入库移动单");
        assertEquals(ErpMfgConstants.MOVE_TYPE_MANUFACTURING, completionMove.getMoveType(), "完工入库用 MANUFACTURING");

        // 产成品 P 入库 → 余额 = 1
        ErpInvStockBalance pBalance = findBalance(P);
        assertNotNull(pBalance, "产成品应入库");
        assertEquals(0, pBalance.getTotalQuantity().compareTo(bd("1")), "产成品 P 入库 1 件");
    }

    @Test
    public void testInspectionGateBlocksCompletionWhenEnabled() {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom(9102L, P, M1, bd("1"));
        seedBomInspectionRequired(9102L, true);
        generateIncoming(M1, "PR-E2E-GATE", bd("10"), bd("5"));

        Long woId = seedWorkOrder("WO-GATE", 9102L);
        seedWorkOrderLine(woId, M1, bd("1"), "INPUT", null);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID);

        rpcOk(mutation, "ErpMfgWorkOrder__submit", Map.of("workOrderId", woId));
        rpcOk(mutation, "ErpMfgWorkOrder__approve", Map.of("workOrderId", woId));
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
        seedBom(9103L, P, M1, bd("1"));
        generateIncoming(M1, "PR-E2E-JC", bd("10"), bd("5"));
        Long woId = seedWorkOrder("WO-JC", 9103L);
        seedWorkOrderLine(woId, M1, bd("1"), "INPUT", null);
        Long jobCardId = seedJobCard(woId);

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

    private String statusOf(Long jobCardId) {
        ErpMfgJobCard jc = daoProvider.daoFor(ErpMfgJobCard.class).getEntityById(jobCardId);
        return jc.getStatus();
    }

    private ApiResponse<?> recordWorkRequest(Long jobCardId, BigDecimal durationMins, BigDecimal hourlyRate,
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

    private void generateIncoming(Long materialId, String billCode, BigDecimal qty, BigDecimal unitCost) {
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

    private void seedMaterial(Long id, String costMethod) {
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

    private void seedBom(Long bomId, Long productId, Long componentId, BigDecimal qty) {
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
            line.orm_propValueByName("id", bomId + 50000);
            line.setBomId(bomId);
            line.setLineNo(10);
            line.setMaterialId(componentId);
            line.setUoMId(UOM_ID);
            line.setQuantity(qty);
            ldao.saveEntity(line);
        });
    }

    private void seedBomInspectionRequired(Long bomId, boolean required) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBom> dao = daoProvider.daoFor(ErpMfgBom.class);
            ErpMfgBom bom = dao.getEntityById(bomId);
            bom.setInspectionRequired(required);
            dao.updateEntity(bom);
        });
    }

    private Long seedWorkOrder(String code, Long bomId) {
        Long id = 8300L + (long) Math.abs(code.hashCode() % 700);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrder> dao = daoProvider.daoFor(ErpMfgWorkOrder.class);
            ErpMfgWorkOrder wo = new ErpMfgWorkOrder();
            wo.orm_propValueByName("id", id);
            wo.setCode(code);
            wo.setProductId(P);
            wo.setBomId(bomId);
            wo.setOrgId(ORG_ID);
            wo.setCurrencyId(CURRENCY_ID);
            wo.setPlannedQuantity(bd("1"));
            wo.setBusinessDate(LocalDate.of(2026, 7, 1));
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_DRAFT);
            dao.saveEntity(wo);
        });
        return id;
    }

    private Long seedWorkOrderLine(Long woId, Long materialId, BigDecimal plannedQty, String lineType,
                                   Long destWarehouseId) {
        long raw = (woId + "" + materialId + lineType).hashCode();
        Long id = 9300L + (long) Math.abs(raw % 700);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrderLine> dao = daoProvider.daoFor(ErpMfgWorkOrderLine.class);
            ErpMfgWorkOrderLine wol = new ErpMfgWorkOrderLine();
            wol.orm_propValueByName("id", id);
            wol.setWorkOrderId(woId);
            wol.setLineNo(materialId.intValue());
            wol.orm_propValueByName("lineType", lineType);
            wol.setMaterialId(materialId);
            wol.setUoMId(UOM_ID);
            wol.setPlannedQuantity(plannedQty);
            wol.setDestWarehouseId(destWarehouseId);
            dao.saveEntity(wol);
        });
        return id;
    }

    private Long seedIssue(String code, Long woId) {
        Long id = 8400L + (long) Math.abs(code.hashCode() % 700);
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

    private void seedIssueLine(Long id, Long issueId, Long materialId, BigDecimal qty, Long wolId) {
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

    private Long seedJobCard(Long woId) {
        Long id = 8500L + (long) Math.abs((woId + "jc").hashCode() % 700);
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

    private ErpMfgJobCardTimeLog findTimeLog(Long jobCardId) {
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

    private ErpInvStockBalance findBalance(Long materialId) {
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
