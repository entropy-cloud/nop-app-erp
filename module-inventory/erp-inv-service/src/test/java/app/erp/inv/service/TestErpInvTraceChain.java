package app.erp.inv.service;

import app.erp.inv.biz.TraceChainResult;
import app.erp.inv.dao.entity.ErpInvStockMove;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 库存追溯链行为测试：generateMove 持久化 origin 上链、reverse 自动挂 originReturnedMoveId、
 * 四类追溯查询（forward/backward/return/batch）递归正确性、环检测截断、max-depth 截断、
 * delVersion 过滤、trace-chain-enabled=false 降级。
 *
 * <p>经 {@link IGraphQLEngine} 调 {@code ErpInvStockMove__generateMove/reverse/forwardTrace/...}，
 * 引擎负责建 session/事务/管道；链路断言解析 {@link TraceChainResult} 序列化结果。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpInvTraceChain extends JunitAutoTestCase {

    static final Long ORG_ID = 1101L;
    static final Long MATERIAL_ID = 2102L;
    static final Long WAREHOUSE_ID = 3102L;
    static final Long LOCATION_ID = 4102L;
    static final Long UOM_ID = 5102L;
    static final Long CURRENCY_ID = 6102L;
    static final Long ACCT_SCHEMA_ID = 7102L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testGenerateMovePersistsOriginLink() {
        Long originId = genChainMove("ORG-LNK-A", null);
        Long childId = genChainMove("ORG-LNK-B", originId);

        ErpInvStockMove child = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(childId);
        assertEquals(originId, child.getOriginMoveId(), "generateMove 应持久化 originMoveId 上链");
    }

    @Test
    public void testReverseSetsOriginReturnedMoveId() {
        Long originalId = genChainMove("REV-A", null);
        Long reversalId = reverse(originalId);

        ErpInvStockMove reversal = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(reversalId);
        assertEquals(originalId, reversal.getOriginReturnedMoveId(),
                "reverse 生成的反向冲销移动单应自动挂 originReturnedMoveId=原单 id");
    }

    @Test
    public void testForwardAndBackwardTraceChain() {
        Long aId = genChainMove("CHN-A", null);
        Long bId = genChainMove("CHN-B", aId);
        Long cId = genChainMove("CHN-C", bId);

        TraceChainResult forward = forwardTrace(aId);
        assertEquals(3, forward.getNodes().size(), "正向 A→B→C 应含 3 个节点");
        assertEquals(aId, forward.getNodes().get(0).getId(), "正向根节点为 A");
        assertEquals(cId, forward.getNodes().get(2).getId(), "正向末端为 C");
        assertFalse(forward.isTruncated(), "正常链不应截断");

        TraceChainResult backward = backwardTrace(cId);
        assertEquals(3, backward.getNodes().size(), "反向 C→B→A 应含 3 个节点");
        assertEquals(cId, backward.getNodes().get(0).getId(), "反向根节点为 C");
        assertEquals(aId, backward.getNodes().get(2).getId(), "反向末端为 A");
    }

    @Test
    public void testReturnTraceBidirectional() {
        Long originalId = genChainMove("RET-ORIG", null);
        Long returnId = genReturnMove("RET-RM", originalId);

        // 给定原移动单 → 其退货移动单
        TraceChainResult fromOriginal = returnTrace(originalId);
        assertTrue(containsNode(fromOriginal, returnId), "原单 returnTrace 应返回退货移动单");
        assertTrue(containsNode(fromOriginal, originalId), "原单 returnTrace 含原单本身");

        // 给定退货移动单 → 其原移动单
        TraceChainResult fromReturn = returnTrace(returnId);
        assertTrue(containsNode(fromReturn, originalId), "退货单 returnTrace 应返回原移动单");
        assertTrue(containsNode(fromReturn, returnId), "退货单 returnTrace 含退货单本身");
    }

    @Test
    public void testRingDetectionTruncated() {
        Long xId = genChainMove("RING-X", null);
        Long yId = genChainMove("RING-Y", null);
        // 人造环：X.originMoveId=Y 且 Y.originMoveId=X
        ormTemplate.runInSession(session -> {
            ErpInvStockMove x = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(xId);
            ErpInvStockMove y = daoProvider.daoFor(ErpInvStockMove.class).getEntityById(yId);
            x.setOriginMoveId(yId);
            y.setOriginMoveId(xId);
            daoProvider.daoFor(ErpInvStockMove.class).saveOrUpdateEntity(x);
            daoProvider.daoFor(ErpInvStockMove.class).saveOrUpdateEntity(y);
            return null;
        });

        TraceChainResult forward = forwardTrace(xId);
        assertTrue(forward.isTruncated(), "成环数据应被环检测截断");
        assertTrue(forward.getNodes().size() <= 2, "环截断后节点不应无限增长");
    }

    @Test
    public void testMaxDepthTruncation() {
        setMaxDepth(2);
        try {
            Long aId = genChainMove("DPT-A", null);
            Long bId = genChainMove("DPT-B", aId);
            Long cId = genChainMove("DPT-C", bId);
            genChainMove("DPT-D", cId);

            TraceChainResult forward = forwardTrace(aId);
            assertTrue(forward.isTruncated(), "超过 max-depth 应截断");
            assertTrue(forward.getNodes().size() <= 3, "max-depth=2 时节点数不应超过 root+2 层");
        } finally {
            setMaxDepth(ErpInvConstants.TRACE_CHAIN_MAX_DEPTH_DEFAULT);
        }
    }

    @Test
    public void testBatchTraceByBatchNo() {
        genBatchMove("BTC-A", "BATCH-001");
        genBatchMove("BTC-B", "BATCH-001");

        TraceChainResult result = batchTrace("BATCH-001");
        assertEquals(2, result.getNodes().size(), "批次追溯应命中 2 张含该 batchNo 的移动单");

        TraceChainResult miss = batchTrace("BATCH-NONE");
        assertEquals(0, miss.getNodes().size(), "不存在的 batchNo 应返回空");
    }

    @Test
    public void testDelVersionFilterExcludesDeleted() {
        Long aId = genChainMove("DEL-A", null);
        Long bId = genChainMove("DEL-B", aId);

        assertEquals(2, forwardTrace(aId).getNodes().size(), "删除前链含 A、B 两个节点");

        ormTemplate.runInSession(session -> {
            IEntityDao<ErpInvStockMove> dao = daoProvider.daoFor(ErpInvStockMove.class);
            ErpInvStockMove b = dao.getEntityById(bId);
            dao.deleteEntity(b);
            return null;
        });

        TraceChainResult after = forwardTrace(aId);
        assertEquals(1, after.getNodes().size(), "逻辑删除后 B 应从链中消失");
        assertEquals(aId, after.getNodes().get(0).getId(), "仅剩根 A");
    }

    @Test
    public void testDisabledReturnsSingleNode() {
        Long aId = genChainMove("DIS-A", null);
        Long bId = genChainMove("DIS-B", aId);

        setTraceEnabled(false);
        try {
            TraceChainResult forward = forwardTrace(aId);
            assertEquals(1, forward.getNodes().size(), "trace-chain-enabled=false 时正向仅返回单节点");
            assertEquals(aId, forward.getNodes().get(0).getId(), "单节点为根 A");
            assertTrue(forward.getLinks().isEmpty(), "关闭时不产出边");
        } finally {
            setTraceEnabled(true);
        }
    }

    // ---------- generation helpers ----------

    private Long genChainMove(String billCode, Long originMoveId) {
        Map<String, Object> req = baseIncomingReq("TRACE_CHAIN", billCode);
        if (originMoveId != null) {
            req.put("originMoveId", originMoveId);
        }
        return idOf(genMove(req));
    }

    private Long genReturnMove(String billCode, Long originReturnedMoveId) {
        Map<String, Object> req = baseIncomingReq("TRACE_RETURN", billCode);
        req.put("originReturnedMoveId", originReturnedMoveId);
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

    // ---------- trace invocation helpers ----------

    private TraceChainResult forwardTrace(Long moveId) {
        return parseTrace(executeRpc(query, "ErpInvStockMove__forwardTrace",
                ApiRequest.build(Map.of("moveId", moveId))));
    }

    private TraceChainResult backwardTrace(Long moveId) {
        return parseTrace(executeRpc(query, "ErpInvStockMove__backwardTrace",
                ApiRequest.build(Map.of("moveId", moveId))));
    }

    private TraceChainResult returnTrace(Long moveId) {
        return parseTrace(executeRpc(query, "ErpInvStockMove__returnTrace",
                ApiRequest.build(Map.of("moveId", moveId))));
    }

    private TraceChainResult batchTrace(String batchNo) {
        return parseTrace(executeRpc(query, "ErpInvStockMove__batchTrace",
                ApiRequest.build(Map.of("batchNo", batchNo))));
    }

    @SuppressWarnings("unchecked")
    private TraceChainResult parseTrace(ApiResponse<?> resp) {
        assertEquals(0, resp.getStatus(), "追溯查询应成功，实际 code=" + resp.getCode());
        Map<String, Object> data = (Map<String, Object>) resp.getData();
        TraceChainResult result = new TraceChainResult();
        List<Map<String, Object>> nodeMaps = (List<Map<String, Object>>) data.get("nodes");
        if (nodeMaps != null) {
            for (Map<String, Object> nm : nodeMaps) {
                ErpInvStockMove m = new ErpInvStockMove();
                m.setId(toLong(nm.get("id")));
                result.getNodes().add(m);
            }
        }
        Object truncated = data.get("truncated");
        result.setTruncated(Boolean.TRUE.equals(truncated));
        return result;
    }

    private boolean containsNode(TraceChainResult result, Long id) {
        for (ErpInvStockMove m : result.getNodes()) {
            if (id.equals(m.getId())) {
                return true;
            }
        }
        return false;
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

    // ---------- config helpers ----------

    private void setMaxDepth(int depth) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_TRACE_CHAIN_MAX_DEPTH, String.valueOf(depth));
    }

    private void setTraceEnabled(boolean enabled) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpInvConstants.CONFIG_TRACE_CHAIN_ENABLED, String.valueOf(enabled));
    }
}
