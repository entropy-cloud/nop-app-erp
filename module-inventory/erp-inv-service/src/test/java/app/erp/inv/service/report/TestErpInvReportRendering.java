package app.erp.inv.service.report;

import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.inv.service.ErpInvConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.WebContentBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 库存追溯可视化报表渲染端到端测试（plan 2026-07-06-1247-1 Phase 1 Proof）。
 *
 * <p>覆盖库存追溯报表的 {@code renderHtml}/{@code download(xlsx|pdf)} 渲染管线、追溯数据集口径断言
 * （移动链路 / 批次号 / 退货行）、空数据集不报错、以及路径注入防护（非法 reportName 抛
 * {@code ERR_REPORT_NAME_INVALID}）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvReportRendering extends JunitAutoTestCase {

    private static final io.nop.core.context.IServiceContext CTX = new io.nop.core.context.ServiceContextImpl();

    static final Long ORG_ID = 1101L;
    static final Long MATERIAL_ID = 2102L;
    static final Long WAREHOUSE_ID = 3102L;
    static final Long LOCATION_ID = 4102L;
    static final Long UOM_ID = 5102L;
    static final Long CURRENCY_ID = 6102L;
    static final Long ACCT_SCHEMA_ID = 7102L;

    @Inject
    ErpInvReportBizModel reportBiz;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ===================== Phase 1: 库存追溯可视化报表 =====================

    @Test
    public void testInventoryTraceReportRenderHtmlByMoveId() {
        Long aId = genChainMove("RPT-CHN-A", null);
        genChainMove("RPT-CHN-B", aId);
        genChainMove("RPT-CHN-C", aId);

        Map<String, Object> data = new HashMap<>();
        data.put("moveId", aId);
        String html = reportBiz.renderHtml("inventory-trace-report", data, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
    }

    @Test
    public void testInventoryTraceReportRenderHtmlByBatchNo() {
        genBatchMove("RPT-BTC-A", "BATCH-RPT-001");
        genBatchMove("RPT-BTC-B", "BATCH-RPT-001");

        Map<String, Object> data = new HashMap<>();
        data.put("batchNo", "BATCH-RPT-001");
        String html = reportBiz.renderHtml("inventory-trace-report", data, CTX);
        assertNotNull(html, "renderHtml 非空");
        assertFalse(html.trim().isEmpty(), "renderHtml 文本非空");
        assertTrue(html.contains("BATCH-RPT-001"), "renderHtml 含批次号值");
    }

    @Test
    public void testInventoryTraceReportDownloadXlsxAndPdf() {
        Long aId = genChainMove("RPT-DL-A", null);
        genChainMove("RPT-DL-B", aId);

        Map<String, Object> data = new HashMap<>();
        data.put("moveId", aId);
        for (String renderType : new String[]{"xlsx", "pdf"}) {
            WebContentBean bean = reportBiz.download("inventory-trace-report", renderType, data, CTX);
            assertNotNull(bean, "download 非空: " + renderType);
            Object content = bean.getContent();
            assertNotNull(content, "download 内容非空: " + renderType);
            assertTrue(content instanceof java.io.File, "download 返回 File: " + renderType);
            java.io.File f = (java.io.File) content;
            assertTrue(f.exists() && f.length() > 0, "download 文件有效: " + renderType);
            f.delete();
        }
    }

    @Test
    public void testInventoryTraceDatasetByBatchNo() {
        genBatchMove("RPT-DS-A", "BATCH-DS-001");
        genBatchMove("RPT-DS-B", "BATCH-DS-001");

        List<Map<String, Object>> ds = reportBiz.buildInventoryTraceDataset(
                "BATCH-DS-001", null, null, null, CTX);
        assertFalse(ds.isEmpty(), "批次追溯数据集非空");
        assertEquals("BATCH", ds.get(0).get("traceType"), "批次追溯标记 traceType=BATCH");
        boolean hasBatchNo = false;
        for (Map<String, Object> row : ds) {
            if (((String) row.get("batchNos")).contains("BATCH-DS-001")) {
                hasBatchNo = true;
                break;
            }
        }
        assertTrue(hasBatchNo, "数据集行包含批次号");
    }

    @Test
    public void testInventoryTraceDatasetByMoveIdForwardChain() {
        Long aId = genChainMove("RPT-FWD-A", null);
        Long bId = genChainMove("RPT-FWD-B", aId);

        List<Map<String, Object>> ds = reportBiz.buildInventoryTraceDataset(
                null, null, null, aId, CTX);
        assertFalse(ds.isEmpty(), "正向追溯数据集非空");
        assertEquals("FORWARD", ds.get(0).get("traceType"), "移动单追溯标记 traceType=FORWARD");
        // 根 A + 下游 B
        assertTrue(ds.size() >= 2, "正向链至少含根 A 与下游 B");
        assertEquals(0, bd("10").compareTo(toBd(ds.get(0).get("quantity"))), "行数量=10");
    }

    @Test
    public void testInventoryTraceDatasetReturnMarked() {
        Long originalId = genChainMove("RPT-RET-ORIG", null);
        Long returnId = reverse(originalId);

        List<Map<String, Object>> ds = reportBiz.buildInventoryTraceDataset(
                null, null, null, returnId, CTX);
        assertFalse(ds.isEmpty(), "退货追溯数据集非空");
        // 退货移动单的 originReturnedMoveId 非空 → isReturn=true
        boolean hasReturn = false;
        for (Map<String, Object> row : ds) {
            if (Boolean.TRUE.equals(row.get("isReturn"))) {
                hasReturn = true;
                break;
            }
        }
        assertTrue(hasReturn, "退货移动单标记 isReturn=true");
    }

    @Test
    public void testInventoryTraceEmptyDatasetNoError() {
        // 无数据：不报错，返回空列表
        List<Map<String, Object>> ds = reportBiz.buildInventoryTraceDataset(
                "BATCH-NONE-EMPTY", null, null, null, CTX);
        assertNotNull(ds, "空数据集不报错");
        assertTrue(ds.isEmpty(), "不存在的批次 → 空列表");
    }

    // ===================== 路径注入防护 =====================

    @Test
    public void testPathInjectionRejected() {
        assertThrows(NopException.class,
                () -> reportBiz.renderHtml("../etc/passwd", null, CTX),
                "非法 reportName（含 ../）抛 NopException");
        assertThrows(NopException.class,
                () -> reportBiz.renderHtml(null, null, CTX),
                "空 reportName 抛 NopException");
        Long aId = genChainMove("RPT-INJ-A", null);
        Map<String, Object> data = new HashMap<>();
        data.put("moveId", aId);
        assertThrows(NopException.class,
                () -> reportBiz.download("inventory-trace-report", "exe", data, CTX),
                "非法 renderType 抛 NopException");
    }

    // ===================== 数据准备（复用 TestErpInvTraceChain 范式） =====================

    private Long genChainMove(String billCode, Long originMoveId) {
        Map<String, Object> req = baseIncomingReq("TRACE_CHAIN", billCode);
        if (originMoveId != null) {
            req.put("originMoveId", originMoveId);
        }
        return idOf(genMove(req));
    }

    private Long genBatchMove(String billCode, String batchNo) {
        Map<String, Object> req = baseIncomingReq("TRACE_BATCH", billCode);
        req.put("lines", Collections.singletonList(line(BigDecimal.TEN, null, batchNo)));
        return idOf(genMove(req));
    }

    private Long reverse(Long moveId) {
        return idOf(executeRpc(mutation, "ErpInvStockMove__reverse",
                ApiRequest.build(Map.of("moveId", moveId))));
    }

    private Map<String, Object> baseIncomingReq(String billType, String billCode) {
        Map<String, Object> req = new LinkedHashMap<>();
        req.put("moveType", ErpInvConstants.MOVE_TYPE_INCOMING);
        req.put("orgId", ORG_ID);
        req.put("businessDate", "2026-07-01");
        req.put("destWarehouseId", WAREHOUSE_ID);
        req.put("destLocationId", LOCATION_ID);
        req.put("acctSchemaId", ACCT_SCHEMA_ID);
        req.put("currencyId", CURRENCY_ID);
        req.put("relatedBillType", billType);
        req.put("relatedBillCode", billCode);
        req.put("lines", Collections.singletonList(line(BigDecimal.TEN, new BigDecimal("5"), null)));
        return req;
    }

    private Map<String, Object> line(BigDecimal qty, BigDecimal unitCost, String batchNo) {
        Map<String, Object> line = new LinkedHashMap<>();
        line.put("materialId", MATERIAL_ID);
        line.put("uoMId", UOM_ID);
        line.put("quantity", qty);
        if (unitCost != null) {
            line.put("unitCost", unitCost);
        }
        if (batchNo != null) {
            line.put("batchNo", batchNo);
        }
        line.put("currencyId", CURRENCY_ID);
        return line;
    }

    // ---------- rpc primitives ----------

    private ApiResponse<?> genMove(Map<String, Object> req) {
        return executeRpc(mutation, "ErpInvStockMove__generateMove", ApiRequest.build(Map.of("request", req)));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Long idOf(ApiResponse<?> resp) {
        assertEquals(0, resp.getStatus(), "generateMove/reverse 应成功，实际 code=" + resp.getCode());
        Object id = ((Map<?, ?>) resp.getData()).get("id");
        return toLong(id);
    }

    private Long toLong(Object v) {
        if (v instanceof Number) {
            return ((Number) v).longValue();
        }
        return Long.parseLong(String.valueOf(v));
    }

    private static BigDecimal bd(String v) {
        return new BigDecimal(v);
    }

    private static BigDecimal toBd(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal) return (BigDecimal) v;
        if (v instanceof Number) return new BigDecimal(v.toString());
        return new BigDecimal(String.valueOf(v));
    }
}
