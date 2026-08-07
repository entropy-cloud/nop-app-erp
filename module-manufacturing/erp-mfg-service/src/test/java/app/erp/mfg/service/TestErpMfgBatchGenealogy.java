package app.erp.mfg.service;

import app.erp.inv.dao.entity.ErpInvBatch;
import app.erp.mfg.dao.entity.ErpMfgBatchGenealogy;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgBomLine;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssue;
import app.erp.mfg.dao.entity.ErpMfgMaterialIssueLine;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.mfg.service.genealogy.BatchGenealogyWriter;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.notify.biz.IErpSysNotificationBiz;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
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
import java.util.HashMap;
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
 * 生产批次基因链追溯测试（plan 2026-07-07-0305-3 §Phase 3 + RC-R1.3 P1-RC-010 补强）。
 *
 * <p>覆盖：完工写入基因链（带批次原料→基因行落库 + 数量正确 + 无批次原料跳过 + config 关闭不写）、
 * forwardTrace（成品→原料）、backwardTrace（原料→成品）、traceChain（多级递归 + 环路防护 + maxDepth ErrorCode）、
 * recallReport（受影响成品批次集合——强断言 affectedLots 内容/degraded/sourceLotId + REJECTED 批次排除）、
 * best-effort 写失败路径（派生 Writer 抛错 → 完工不阻断 + catch 分支 notify 告警派发落库 + 无模板静默跳过 +
 * config 关闭跳过）。
 *
 * <p>权威：{@code docs/design/manufacturing/batch-genealogy.md}。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgBatchGenealogy extends JunitAutoTestCase {

    @RegisterExtension
    static MfgFrozenClockExtension frozenClock = new MfgFrozenClockExtension();

    static final Long ORG_ID = 1601L;
    static final Long WAREHOUSE_ID = 3601L;
    static final Long UOM_ID = 5601L;
    static final Long CURRENCY_ID = 6601L;
    static final Long P = 1201L;     // 产成品（FG）
    static final Long M1 = 1202L;    // 原料1
    static final String RECIPIENT = "mfg-genealogy-recipient";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IErpSysNotificationBiz notificationBiz;

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
        // REJECTED 产出批次：应被 collectAffectedIfFinishedGood 排除（不出现在 affectedLots）
        Long lotRejected = seedBatch(2023L, "LOT-RECALL-REJ", P, bd("2"),
                ErpMfgConstants.LOT_STATUS_REJECTED);

        // 问题原料批次 lotA → 影响成品 lotB、lotC（lotC → REJECTED 批次，后者应被排除）
        seedGenealogyRow(9420L, 8010L, lotA, M1, bd("10"), lotB, P, bd("5"));
        seedGenealogyRow(9421L, 8011L, lotB, P, bd("5"), lotC, P, bd("3"));
        seedGenealogyRow(9422L, 8012L, lotC, P, bd("3"), lotRejected, P, bd("2"));

        ApiResponse<?> resp = rpc(query, "ErpMfgBatchGenealogy__recallReport",
                Map.of("lotId", lotA));
        assertEquals(0, resp.getStatus(), "recallReport 应成功: " + resp);
        assertNotNull(resp.getData(), "recallReport 应返回数据");

        // 强断言（P1-RC-010 测试补充义务）：sourceLotId/degraded/affectedLots 内容
        Map<String, Object> data = (Map<String, Object>) resp.getData();
        assertEquals(lotA, ((Number) data.get("sourceLotId")).longValue(),
                "sourceLotId 应为入参 lotA");
        assertEquals(Boolean.TRUE, data.get("degraded"),
                "degraded 应为 true（位置/去向归 inventory successor，结构性恒置）");

        List<?> affectedLots = (List<?>) data.get("affectedLots");
        assertEquals(2, affectedLots.size(),
                "受影响成品批次应恰为 lotB/lotC（REJECTED 批次排除）");
        Map<Long, Map<String, Object>> byLotId = new HashMap<>();
        for (Object item : affectedLots) {
            Map<String, Object> row = (Map<String, Object>) item;
            byLotId.put(((Number) row.get("lotId")).longValue(), row);
        }
        assertAffectedLot(byLotId, lotB, "LOT-RECALL-B");
        assertAffectedLot(byLotId, lotC, "LOT-RECALL-C");
    }

    @Test
    public void testWriteOnCompletionFailureInjectedDispatchesAlert() {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom(9430L, P, M1, bd("1"));
        seedNotifyTemplate(7130L, RECIPIENT);

        Long woId = seedWorkOrder("WO-BG-FAIL", 9430L, bd("1"));
        Long inputWolId = seedWorkOrderLine(woId, M1, bd("1"), "INPUT", null);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID);
        Long issueId = seedIssue("MI-BG-FAIL", woId);
        seedIssueLineWithBatch(9431L, issueId, M1, bd("1"), inputWolId, "BATCH-M1-FAIL");

        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);

        // 失败注入：同包子类派生 Writer 覆盖 doWrite 直接抛 NopException（不改生产代码加测试钩子）
        BatchGenealogyWriter failingWriter = new ThrowingBatchGenealogyWriter();
        failingWriter.setDaoProvider(daoProvider);
        failingWriter.setNotificationBiz(notificationBiz);

        int before = countNotifications(ErpMfgConstants.NOTIFY_EVENT_GENEALOGY_WRITE_FAILURE);
        // best-effort：writeOnCompletion 不 rethrow（完工不被阻断语义）
        failingWriter.writeOnCompletion(wo, bd("1"), null);

        // catch 分支 notify 告警派发：ErpSysNotification 行落库（eventType + recipient + status=SENT）
        assertEquals(before + 1, countNotifications(ErpMfgConstants.NOTIFY_EVENT_GENEALOGY_WRITE_FAILURE),
                "写失败应派发 mfg.production-genealogy-write-failure 通知");
        ErpSysNotification n = findNotification(ErpMfgConstants.NOTIFY_EVENT_GENEALOGY_WRITE_FAILURE);
        assertNotNull(n, "应存在基因链写失败通知行");
        assertEquals(RECIPIENT, n.getRecipientUserId(), "接收人应匹配模板 USER_LIST");
        assertEquals("SENT", n.getStatus(), "通知状态应为 SENT（ErpNotifyConstants.STATUS_SENT）");
        assertNotNull(n.getPayloadJson(), "通知 payload 应含失败上下文");
        assertTrue(n.getPayloadJson().contains("WO-BG-FAIL"), "payload 应含 workOrderCode");
        assertTrue(n.getPayloadJson().contains(
                        ErpMfgErrors.ERR_MFG_GENEALOGY_LOT_NOT_FOUND.getErrorCode()),
                "payload 应含 errorCode");
    }

    @Test
    public void testWriteOnCompletionFailureSilentlySkipsWithoutTemplate() {
        seedMaterial(P, null);
        seedMaterial(M1, "MOVING_AVERAGE");
        seedBom(9432L, P, M1, bd("1"));
        // 不 seed 通知模板 → notify config-gated 静默跳过（对齐 IErpSysNotificationBiz.notify 契约）

        Long woId = seedWorkOrder("WO-BG-NOTPL", 9432L, bd("1"));
        Long inputWolId = seedWorkOrderLine(woId, M1, bd("1"), "INPUT", null);
        seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID);
        Long issueId = seedIssue("MI-BG-NOTPL", woId);
        seedIssueLineWithBatch(9433L, issueId, M1, bd("1"), inputWolId, "BATCH-M1-NOTPL");

        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
        BatchGenealogyWriter failingWriter = new ThrowingBatchGenealogyWriter();
        failingWriter.setDaoProvider(daoProvider);
        failingWriter.setNotificationBiz(notificationBiz);

        // 无 ACTIVE 模板 → 静默跳过不抛（完工不阻断语义保持）
        failingWriter.writeOnCompletion(wo, bd("1"), null);
        assertEquals(0, countNotifications(ErpMfgConstants.NOTIFY_EVENT_GENEALOGY_WRITE_FAILURE),
                "无 ACTIVE 模板应静默跳过不落库");
    }

    @Test
    public void testWriteOnCompletionDisabledConfigSkipsWriteAndAlert() {
        setWriteEnabled(false);
        try {
            seedMaterial(P, null);
            seedMaterial(M1, "MOVING_AVERAGE");
            seedBom(9434L, P, M1, bd("1"));
            seedNotifyTemplate(7134L, RECIPIENT);

            Long woId = seedWorkOrder("WO-BG-DISABLED", 9434L, bd("1"));
            Long inputWolId = seedWorkOrderLine(woId, M1, bd("1"), "INPUT", null);
            seedWorkOrderLine(woId, P, bd("1"), "OUTPUT", WAREHOUSE_ID);
            Long issueId = seedIssue("MI-BG-DISABLED", woId);
            seedIssueLineWithBatch(9435L, issueId, M1, bd("1"), inputWolId, "BATCH-M1-DISABLED");

            ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(woId);
            BatchGenealogyWriter failingWriter = new ThrowingBatchGenealogyWriter();
            failingWriter.setDaoProvider(daoProvider);
            failingWriter.setNotificationBiz(notificationBiz);

            // config 关闭 → writeOnCompletion 提前返回：不写基因链、不派发告警、不抛错
            failingWriter.writeOnCompletion(wo, bd("1"), null);
            assertEquals(0, countNotifications(ErpMfgConstants.NOTIFY_EVENT_GENEALOGY_WRITE_FAILURE),
                    "config 关闭时应跳过写入与告警派发");
        } finally {
            setWriteEnabled(true);
        }
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
        return seedBatch(id, batchNo, materialId, qty, ErpMfgConstants.INV_BATCH_STATUS_OPEN);
    }

    private Long seedBatch(Long id, String batchNo, Long materialId, BigDecimal qty, String status) {
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
            batch.setStatus(status);
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

    private void assertAffectedLot(Map<Long, Map<String, Object>> byLotId, Long lotId, String batchNo) {
        Map<String, Object> row = byLotId.get(lotId);
        assertNotNull(row, "受影响批次应包含 lotId=" + lotId);
        assertEquals(batchNo, row.get("batchNo"), "batchNo 应匹配");
        assertEquals(P, ((Number) row.get("materialId")).longValue(), "materialId 应为产成品 P");
        assertEquals(ErpMfgConstants.LOT_STATUS_RELEASED, row.get("lotStatus"), "lotStatus 应为 RELEASED");
    }

    private void seedNotifyTemplate(Long id, String recipientUserId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", id);
            t.setNotificationType(ErpMfgConstants.NOTIFY_EVENT_GENEALOGY_WRITE_FAILURE);
            t.setName("基因链写失败告警");
            t.setChannelSet("IN_APP");
            t.setSubjectTpl("基因链写失败告警: ${workOrderCode}");
            t.setBodyTpl("工单 ${workOrderCode} 完工写入批次基因链失败（best-effort，不阻断完工入库）：errorCode=${errorCode}，errorMessage=${errorMessage}");
            t.setRecipientResolver("USER_LIST");
            t.setRecipientConfig("{\"userIds\":[\"" + recipientUserId + "\"]}");
            t.setMergeWindowSeconds(60);
            t.setMergeStrategy("MERGE_BY_USER_TYPE");
            t.setStatus("ACTIVE");
            dao.saveEntity(t);
        });
    }

    private int countNotifications(String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", eventType));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q).size();
    }

    private ErpSysNotification findNotification(String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", eventType));
        q.addOrderField("createTime", true);
        q.setLimit(1);
        List<ErpSysNotification> list = daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private void setWriteEnabled(boolean enabled) {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpMfgConstants.CONFIG_GENEALOGY_WRITE_ENABLED, String.valueOf(enabled));
    }

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

    /**
     * 失败注入派生 Writer：覆盖 {@code doWrite} 直接抛 NopException，触发 {@code writeOnCompletion}
     * catch 分支（RC-R1.3 测试：不 rethrow + notify 告警派发）。同包子类可覆盖 protected doWrite。
     */
    static class ThrowingBatchGenealogyWriter extends BatchGenealogyWriter {
        @Override
        protected void doWrite(ErpMfgWorkOrder wo, BigDecimal completedQty, IServiceContext context) {
            throw new NopException(ErpMfgErrors.ERR_MFG_GENEALOGY_LOT_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_LOT_ID, wo.getId());
        }
    }
}
