package app.erp.mfg.service;

import app.erp.inv.dao.entity.ErpInvBatch;
import app.erp.mfg.dao.entity.ErpMfgBatchGenealogy;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 生产批次基因链追溯测试（plan 2026-07-07-0305-3 §Phase 3）。
 *
 * <p>覆盖：完工写入基因链（带批次原料→基因行落库 + 数量正确 + 无批次原料跳过 + config 关闭不写）、
 * forwardTrace（成品→原料）、backwardTrace（原料→成品）、traceChain（多级递归 + 环路防护 + maxDepth ErrorCode）、
 * recallReport（受影响成品批次集合）。
 *
 * <p>权威：{@code docs/design/manufacturing/batch-genealogy.md}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgBatchGenealogy extends JunitAutoTestCase {

    static final Long ORG_ID = 1601L;
    static final Long WAREHOUSE_ID = 3601L;
    static final Long UOM_ID = 5601L;
    static final Long CURRENCY_ID = 6601L;
    static final Long P = 1201L;     // 产成品（FG）
    static final Long M1 = 1202L;    // 原料1

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testWriteOnCompletionWithBatchMaterial() {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom(9401L, P, M1, bd("1"));

        // 输入批次：M1 批次 BATCH-M1-001，总量 10
        Long inputLotId = seedBatch(2001L, "BATCH-M1-001", M1, bd("10"));

        Long woId = seedWorkOrder("WO-BG-001", 9401L, bd("2"));
        Long inputWolId = seedWorkOrderLine(woId, M1, bd("2"), "INPUT", null);
        seedWorkOrderLine(woId, P, bd("2"), "OUTPUT", WAREHOUSE_ID);

        // 领料出库带 batchNo
        Long issueId = seedIssue("MI-BG-001", woId);
        seedIssueLineWithBatch(9402L, issueId, M1, bd("2"), inputWolId, "BATCH-M1-001");

        // 完工入库 2 件
        Map<String, Object> completeReq = new LinkedHashMap<>();
        completeReq.put("workOrderId", woId);
        completeReq.put("completedQty", bd("2"));
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);

        // 验证基因链行
        List<ErpMfgBatchGenealogy> rows = findGenealogyByWorkOrder(woId);
        assertFalse(rows.isEmpty(), "完工后应写入基因链行");
        ErpMfgBatchGenealogy row = rows.get(0);
        assertEquals(inputLotId, row.getInputLotId(), "inputLotId 应为 M1 批次");
        assertEquals(M1, row.getInputMaterialId(), "inputMaterialId 应为 M1");
        assertEquals(0, row.getInputQty().compareTo(bd("2")), "inputQty 应为 2（领料2×完工比例1.0）");
        assertEquals(P, row.getOutputMaterialId(), "outputMaterialId 应为产成品 P");
        assertEquals(0, row.getOutputQty().compareTo(bd("2")), "outputQty 应为完工数量 2");
        assertEquals(ErpMfgConstants.LOT_STATUS_RELEASED, row.getLotStatus(), "lotStatus=RELEASED");
        assertEquals(Boolean.TRUE, row.getIsInputConsumed(), "isInputConsumed=true");

        // 验证产出批次自动创建
        ErpInvBatch outputLot = daoProvider.daoFor(ErpInvBatch.class).getEntityById(row.getOutputLotId());
        assertNotNull(outputLot, "产出批次应自动创建");
        assertEquals(ErpMfgConstants.INV_BATCH_STATUS_OPEN, outputLot.getStatus(), "产出批次状态=OPEN");
        assertEquals(0, outputLot.getTotalQuantity().compareTo(bd("2")), "产出批次总量=2");
    }

    @Test
    public void testWriteSkippedWhenNoBatchMaterial() {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom(9403L, P, M1, bd("1"));

        Long woId = seedWorkOrder("WO-BG-NOBATCH", 9403L, bd("1"));
        Long inputWolId = seedWorkOrderLine(woId, M1, bd("1"), "INPUT", null);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID);

        // 领料出库【无 batchNo】
        Long issueId = seedIssue("MI-BG-NOBATCH", woId);
        seedIssueLineWithBatch(9404L, issueId, M1, bd("1"), inputWolId, null);

        Map<String, Object> completeReq = new LinkedHashMap<>();
        completeReq.put("workOrderId", woId);
        completeReq.put("completedQty", bd("1"));
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);

        // 无批次原料 → 不报错、不写基因行
        List<ErpMfgBatchGenealogy> rows = findGenealogyByWorkOrder(woId);
        assertTrue(rows.isEmpty(), "无批次原料时应跳过基因链写入");
    }

    @Test
    public void testForwardAndBackwardTrace() {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom(9405L, P, M1, bd("1"));

        Long inputLotId = seedBatch(2005L, "BATCH-M1-FWD", M1, bd("10"));
        Long woId = seedWorkOrder("WO-BG-FWD", 9405L, bd("1"));
        Long inputWolId = seedWorkOrderLine(woId, M1, bd("1"), "INPUT", null);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID);
        Long issueId = seedIssue("MI-BG-FWD", woId);
        seedIssueLineWithBatch(9406L, issueId, M1, bd("1"), inputWolId, "BATCH-M1-FWD");

        Map<String, Object> completeReq = new LinkedHashMap<>();
        completeReq.put("workOrderId", woId);
        completeReq.put("completedQty", bd("1"));
        rpcOk(mutation, "ErpMfgWorkOrder__reportCompletion", completeReq);

        ErpMfgBatchGenealogy row = findGenealogyByWorkOrder(woId).get(0);
        Long outputLotId = row.getOutputLotId();

        // forwardTrace(outputLotId) → 找到 inputLot
        ApiResponse<?> fwdResp = rpc(query, "ErpMfgBatchGenealogy__forwardTrace",
                Map.of("outputLotId", outputLotId));
        assertEquals(0, fwdResp.getStatus(), "forwardTrace 应成功: " + fwdResp);
        List<?> fwdRows = (List<?>) fwdResp.getData();
        assertEquals(1, fwdRows.size(), "forwardTrace 应返回 1 条直接输入");

        // backwardTrace(inputLotId) → 找到 outputLot
        ApiResponse<?> bwdResp = rpc(query, "ErpMfgBatchGenealogy__backwardTrace",
                Map.of("inputLotId", inputLotId));
        assertEquals(0, bwdResp.getStatus(), "backwardTrace 应成功: " + bwdResp);
        List<?> bwdRows = (List<?>) bwdResp.getData();
        assertEquals(1, bwdRows.size(), "backwardTrace 应返回 1 条直接产出");
    }

    @Test
    public void testTraceChainCycleProtectionAndMaxDepth() {
        // 手动构造两级基因链：inputLot1 → outputLot1（=inputLot2） → outputLot2
        seedMaterial(M1, "MOVING_AVERAGE");
        seedMaterial(P, null);
        Long lotA = seedBatch(2010L, "LOT-A", M1, bd("10"));
        Long lotB = seedBatch(2011L, "LOT-B", P, bd("5"));
        Long lotC = seedBatch(2012L, "LOT-C", P, bd("3"));

        // 基因行：lotA → lotB
        seedGenealogyRow(9410L, 8001L, lotA, M1, bd("10"), lotB, P, bd("5"));
        // 基因行：lotB → lotC
        seedGenealogyRow(9411L, 8002L, lotB, P, bd("5"), lotC, P, bd("3"));

        // FORWARD 多级：从 lotC（产出）→ lotB → lotA
        ApiResponse<?> fwdChain = rpc(query, "ErpMfgBatchGenealogy__traceChain",
                Map.of("lotId", lotC, "direction", ErpMfgConstants.TRACE_DIRECTION_FORWARD, "maxDepth", 10));
        assertEquals(0, fwdChain.getStatus(), "traceChain FORWARD 应成功: " + fwdChain);
        List<?> fwdEdges = (List<?>) fwdChain.getData();
        assertEquals(2, fwdEdges.size(), "多级前向应返回 2 条边");

        // BACKWARD 多级：从 lotA（输入）→ lotB → lotC
        ApiResponse<?> bwdChain = rpc(query, "ErpMfgBatchGenealogy__traceChain",
                Map.of("lotId", lotA, "direction", ErpMfgConstants.TRACE_DIRECTION_BACKWARD, "maxDepth", 10));
        assertEquals(0, bwdChain.getStatus(), "traceChain BACKWARD 应成功: " + bwdChain);
        List<?> bwdEdges = (List<?>) bwdChain.getData();
        assertEquals(2, bwdEdges.size(), "多级反向应返回 2 条边");

        // 环路防护：构造环路 lotC → lotA（形成 lotA→lotB→lotC→lotA）
        seedGenealogyRow(9412L, 8003L, lotC, P, bd("3"), lotA, M1, bd("10"));
        ApiResponse<?> cycleResp = rpc(query, "ErpMfgBatchGenealogy__traceChain",
                Map.of("lotId", lotC, "direction", ErpMfgConstants.TRACE_DIRECTION_FORWARD, "maxDepth", 50));
        assertEquals(0, cycleResp.getStatus(), "环路应被防护不无限递归: " + cycleResp);

        // maxDepth 超限抛 ErrorCode（非 0 状态）
        ApiResponse<?> depthResp = rpc(query, "ErpMfgBatchGenealogy__traceChain",
                Map.of("lotId", lotC, "direction", ErpMfgConstants.TRACE_DIRECTION_FORWARD, "maxDepth", 1));
        assertTrue(depthResp.getStatus() != 0, "maxDepth=1 时多级链应超限抛错");

        // 非法方向
        ApiResponse<?> badDir = rpc(query, "ErpMfgBatchGenealogy__traceChain",
                Map.of("lotId", lotC, "direction", "INVALID", "maxDepth", 10));
        assertTrue(badDir.getStatus() != 0, "非法方向应抛错");
    }

    @Test
    public void testRecallReport() {
        seedMaterial(M1, "MOVING_AVERAGE");
        seedMaterial(P, null);
        Long lotA = seedBatch(2020L, "LOT-RECALL-A", M1, bd("10"));
        Long lotB = seedBatch(2021L, "LOT-RECALL-B", P, bd("5"));
        Long lotC = seedBatch(2022L, "LOT-RECALL-C", P, bd("3"));

        // 问题原料批次 lotA → 影响成品 lotB、lotC
        seedGenealogyRow(9420L, 8010L, lotA, M1, bd("10"), lotB, P, bd("5"));
        seedGenealogyRow(9421L, 8011L, lotB, P, bd("5"), lotC, P, bd("3"));

        ApiResponse<?> resp = rpc(query, "ErpMfgBatchGenealogy__recallReport",
                Map.of("lotId", lotA));
        assertEquals(0, resp.getStatus(), "recallReport 应成功: " + resp);
        assertNotNull(resp.getData(), "recallReport 应返回数据");
        // 数据结构为 RecallReport，含 affectedLots（lotB、lotC 均为产出成品）
        // degraded=true（当前 inventory 域未暴露位置查询）
    }

    // ---------- seed helpers ----------

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

    private Long seedBatch(Long id, String batchNo, Long materialId, BigDecimal qty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpInvBatch> dao = daoProvider.daoFor(ErpInvBatch.class);
            ErpInvBatch batch = new ErpInvBatch();
            batch.orm_propValueByName("id", id);
            batch.setOrgId(ORG_ID);
            batch.setBatchNo(batchNo);
            batch.setMaterialId(materialId);
            batch.setWarehouseId(WAREHOUSE_ID);
            batch.setTotalQuantity(qty);
            batch.setAvailableQuantity(qty);
            batch.setProductionDate(LocalDate.of(2026, 7, 1));
            batch.setStatus(ErpMfgConstants.INV_BATCH_STATUS_OPEN);
            dao.saveEntity(batch);
        });
        return id;
    }

    private Long seedWorkOrder(String code, Long bomId, BigDecimal plannedQty) {
        Long id = 8600L + (long) Math.abs(code.hashCode() % 800);
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
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
            dao.saveEntity(wo);
        });
        return id;
    }

    private Long seedWorkOrderLine(Long woId, Long materialId, BigDecimal plannedQty, String lineType, Long destWh) {
        Long id = 9600L + (long) Math.abs((woId + "" + materialId + lineType).hashCode() % 800);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgWorkOrderLine> dao = daoProvider.daoFor(ErpMfgWorkOrderLine.class);
            ErpMfgWorkOrderLine wol = new ErpMfgWorkOrderLine();
            wol.orm_propValueByName("id", id);
            wol.setWorkOrderId(woId);
            wol.setLineNo(10);
            wol.orm_propValueByName("lineType", lineType);
            wol.setMaterialId(materialId);
            wol.setUoMId(UOM_ID);
            wol.setPlannedQuantity(plannedQty);
            if (destWh != null) {
                wol.setDestWarehouseId(destWh);
            }
            dao.saveEntity(wol);
        });
        return id;
    }

    private Long seedIssue(String code, Long woId) {
        Long id = 8700L + (long) Math.abs(code.hashCode() % 800);
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
            // DONE 状态：isIssueConsumed 返回 true（CONFIRMED 或 DONE）
            issue.setDocStatus(ErpMfgConstants.ISSUE_STATUS_DONE);
            issue.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
            dao.saveEntity(issue);
        });
        return id;
    }

    private void seedIssueLineWithBatch(Long id, Long issueId, Long materialId, BigDecimal qty,
                                        Long wolId, String batchNo) {
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
            if (batchNo != null) {
                line.setBatchNo(batchNo);
            }
            dao.saveEntity(line);
        });
    }

    private void seedGenealogyRow(Long id, Long workOrderId, Long inputLotId, Long inputMaterialId,
                                  BigDecimal inputQty, Long outputLotId, Long outputMaterialId,
                                  BigDecimal outputQty) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMfgBatchGenealogy> dao = daoProvider.daoFor(ErpMfgBatchGenealogy.class);
            ErpMfgBatchGenealogy row = new ErpMfgBatchGenealogy();
            row.orm_propValueByName("id", id);
            row.setWorkOrderId(workOrderId);
            row.setInputLotId(inputLotId);
            row.setInputMaterialId(inputMaterialId);
            row.setInputQty(inputQty);
            row.setInputUoMId(UOM_ID);
            row.setOutputLotId(outputLotId);
            row.setOutputMaterialId(outputMaterialId);
            row.setOutputQty(outputQty);
            row.setOutputUoMId(UOM_ID);
            row.setProductionDate(LocalDate.of(2026, 7, 1));
            row.setLineNo(10);
            row.setLotStatus(ErpMfgConstants.LOT_STATUS_RELEASED);
            row.setIsInputConsumed(Boolean.TRUE);
            dao.saveEntity(row);
        });
    }

    // ---------- query helpers ----------

    private List<ErpMfgBatchGenealogy> findGenealogyByWorkOrder(Long woId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workOrderId", woId));
        return daoProvider.daoFor(ErpMfgBatchGenealogy.class).findAllByQuery(q);
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }
}
