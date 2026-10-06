package app.erp.ast.service;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 4 CRUD 冒烟测试（assets 域）。
 *
 * <p>抽样实体（Phase 1 实体抽样规则）：主实体 ErpAstAsset（覆盖新建/查询筛选/编辑保存/逻辑删除），
 * 头-行对 ErpAstAsset(头) → ErpAstDepreciationSchedule(行)，行实体 assetId 外键引用头实体主键，覆盖关系导航。
 *
 * <p>沿用 Phase 1 样板（-service 模块、JunitAutoTestCase、@NopTestConfig schema bootstrap、
 * _cases/ 快照）。多步 ID 在 Java 内传递；删除用例 DEL_VERSION 列以通配符 * 屏蔽时钟型非确定性。
 * 
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpAstAssetCrudSmoke extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testCreateHead() {
        Map<String, String> pre = createPrereqs();
        Map<String, Object> headData = new LinkedHashMap<>();
        headData.put("code", "SMOKE-AST");
        headData.put("name", "冒烟资产");
        headData.put("acquisitionDate", "2026-07-01");
        headData.put("originalValue", 10000);
        headData.put("status", "DRAFT");
        ApiResponse<?> result = executeRpc(GraphQLOperationType.mutation, "ErpAstAsset__save",
                ApiRequest.build(Map.of("data", headData)));

        assertEquals(0, result.getStatus());
        assertNotNull(((Map<?, ?>) result.getData()).get("id"), "新建应返回非空 ID");
        output("response.json5", result);
    }

    @Test
    public void testQueryHead() {
        Map<String, String> pre = createPrereqs();
        Map<String, Object> headData = new LinkedHashMap<>();
        headData.put("code", "SMOKE-AST");
        headData.put("name", "冒烟资产");
        headData.put("acquisitionDate", "2026-07-01");
        headData.put("originalValue", 10000);
        headData.put("status", "DRAFT");
        ApiResponse<?> created = executeRpc(GraphQLOperationType.mutation, "ErpAstAsset__save",
                ApiRequest.build(Map.of("data", headData)));
        output("1_save_response.json5", created);

        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpAstAsset__findPage",
                ApiRequest.build(Map.of("limit", 10)));
        assertEquals(0, page.getStatus());
        assertTrue(((Number) ((Map<?, ?>) page.getData()).get("total")).intValue() >= 1, "查询应至少返回 1 条");
        output("response.json5", page);
    }

    @Test
    public void testUpdateHead() {
        Map<String, String> pre = createPrereqs();
        Map<String, Object> headData = new LinkedHashMap<>();
        headData.put("code", "SMOKE-AST");
        headData.put("name", "冒烟资产");
        headData.put("acquisitionDate", "2026-07-01");
        headData.put("originalValue", 10000);
        headData.put("status", "DRAFT");
        ApiResponse<?> created = executeRpc(GraphQLOperationType.mutation, "ErpAstAsset__save",
                ApiRequest.build(Map.of("data", headData)));
        output("1_save_response.json5", created);
        String id = String.valueOf(((Map<?, ?>) created.getData()).get("id"));

        Map<String, Object> upd = new LinkedHashMap<>();
        upd.put("id", id);
        upd.put("remark", "冒烟-已修改");
        ApiResponse<?> updated = executeRpc(GraphQLOperationType.mutation, "ErpAstAsset__update",
                ApiRequest.build(Map.of("data", upd)));
        assertEquals(0, updated.getStatus());
        assertEquals("冒烟-已修改", ((Map<?, ?>) updated.getData()).get("remark"), "编辑保存后字段应与输入一致");
        output("2_update_response.json5", updated);
    }

    @Test
    public void testDeleteHead() {
        Map<String, String> pre = createPrereqs();
        Map<String, Object> headData = new LinkedHashMap<>();
        headData.put("code", "SMOKE-AST");
        headData.put("name", "冒烟资产");
        headData.put("acquisitionDate", "2026-07-01");
        headData.put("originalValue", 10000);
        headData.put("status", "DRAFT");
        ApiResponse<?> created = executeRpc(GraphQLOperationType.mutation, "ErpAstAsset__save",
                ApiRequest.build(Map.of("data", headData)));
        output("1_save_response.json5", created);
        String id = String.valueOf(((Map<?, ?>) created.getData()).get("id"));

        ApiResponse<?> deleted = executeRpc(GraphQLOperationType.mutation, "ErpAstAsset__delete",
                ApiRequest.build(Map.of("id", id)));
        assertEquals(0, deleted.getStatus());

        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpAstAsset__findPage",
                ApiRequest.build(Map.of("limit", 10)));
        assertEquals(0, page.getStatus());
        assertEquals(0, ((Number) ((Map<?, ?>) page.getData()).get("total")).intValue(), "逻辑删除后查询应返回 0 条");
        output("response.json5", page);
    }

    @Test
    public void testLineRelation() {
        Map<String, String> pre = createPrereqs();
        Map<String, Object> headData = new LinkedHashMap<>();
        headData.put("code", "SMOKE-AST");
        headData.put("name", "冒烟资产");
        headData.put("acquisitionDate", "2026-07-01");
        headData.put("originalValue", 10000);
        headData.put("status", "DRAFT");
        ApiResponse<?> head = executeRpc(GraphQLOperationType.mutation, "ErpAstAsset__save",
                ApiRequest.build(Map.of("data", headData)));
        output("1_saveHead_response.json5", head);
        String headId = String.valueOf(((Map<?, ?>) head.getData()).get("id"));

        Map<String, Object> lineData = new LinkedHashMap<>();
        lineData.put("period", "2026-07");
        lineData.put("plannedAmount", 1000);
        lineData.put("status", "PENDING");
        lineData.put("exchangeRate", "1");
        lineData.put("assetId", headId);
        ApiResponse<?> line = executeRpc(GraphQLOperationType.mutation, "ErpAstDepreciationSchedule__save",
                ApiRequest.build(Map.of("data", lineData)));
        assertEquals(0, line.getStatus());
        output("2_saveLine_response.json5", line);

        Map<String, Object> q = new LinkedHashMap<>();
        q.put("filter_assetId", headId);
        q.put("limit", 10);
        ApiResponse<?> page = executeRpc(GraphQLOperationType.query, "ErpAstDepreciationSchedule__findPage",
                ApiRequest.build(q));
        assertEquals(0, page.getStatus());
        assertTrue(((Number) ((Map<?, ?>) page.getData()).get("total")).intValue() >= 1, "按外键查询应至少返回 1 行");
        output("response.json5", page);
    }

    // ---------- P2-CK-ast2-014 折旧三要素校验 ----------

    private static final String ERR_TRI_FACTOR = ErpAstErrors.ERR_DEPRECIATION_USEFUL_LIFE_INVALID.getErrorCode();

    /** 建卡 usefulLifeMonths=0（非正年限）→ 拒绝 ERR_DEPRECIATION_USEFUL_LIFE_INVALID（修复前静默 1 个月折完）。 */
    @Test
    public void testAst2_014SaveRejectsZeroUsefulLife() {
        Map<String, Object> headData = draftAssetData("SMOKE-014A");
        headData.put("usefulLifeMonths", 0);
        ApiResponse<?> result = saveAsset(headData);
        assertTrue(result.getStatus() != 0, "年限 0 建卡应被拒绝");
        assertEquals(ERR_TRI_FACTOR, result.getCode(), "消费既有领域码（修复前零消费）");
    }

    /** 建卡残值≥原值 → 同码拒绝（修复前静默零提）。 */
    @Test
    public void testAst2_014SaveRejectsResidualNotBelowOriginal() {
        Map<String, Object> headData = draftAssetData("SMOKE-014B");
        headData.put("usefulLifeMonths", 12);
        headData.put("residualValue", 10000);
        ApiResponse<?> result = saveAsset(headData);
        assertTrue(result.getStatus() != 0, "残值≥原值建卡应被拒绝");
        assertEquals(ERR_TRI_FACTOR, result.getCode(), "残值违规沿用 ERR_DEPRECIATION_USEFUL_LIFE_INVALID");
    }

    /**
     * 建卡折旧方法出字典 → 拒绝。注：字段级字典校验（meta 层 nop.err.biz.invalid-dict-option）
     * 先于 BizModel 三要素校验触发——领域码 ERR_DEPRECIATION_USEFUL_LIFE_INVALID 被字典守卫遮蔽，
     * 双层防线取先到者；本载体锁定「字典外方法必被拒绝」无论落在哪一层。
     */
    @Test
    public void testAst2_014SaveRejectsUnknownDepreciationMethod() {
        Map<String, Object> headData = draftAssetData("SMOKE-014C");
        headData.put("usefulLifeMonths", 12);
        headData.put("depreciationMethod", "WITCHCRAFT");
        ApiResponse<?> result = saveAsset(headData);
        assertTrue(result.getStatus() != 0, "字典外方法建卡应被拒绝");
        assertTrue(ERR_TRI_FACTOR.equals(result.getCode())
                        || "nop.err.biz.invalid-dict-option".equals(result.getCode()),
                "方法出字典拒绝（meta 字典守卫先到 or 领域码），实际=" + result.getCode());
    }

    /** DRAFT 草稿语义（C1 作用域裁决）：三要素缺省（null）放行建卡。 */
    @Test
    public void testAst2_014SaveDraftWithMissingFactorsAllowed() {
        Map<String, Object> headData = draftAssetData("SMOKE-014D");
        ApiResponse<?> result = saveAsset(headData);
        assertEquals(0, result.getStatus(), "缺省三要素（null）DRAFT 建卡放行");
        assertNotNull(((Map<?, ?>) result.getData()).get("id"));
    }

    /** UPDATE 迁移至 IN_SERVICE 时三要素完备性校验：缺年限拒绝；补齐后放行。 */
    @Test
    public void testAst2_014UpdateToInServiceRequiresCompleteFactors() {
        Map<String, Object> headData = draftAssetData("SMOKE-014E");
        ApiResponse<?> created = saveAsset(headData);
        assertEquals(0, created.getStatus());
        String id = String.valueOf(((Map<?, ?>) created.getData()).get("id"));

        // 缺年限迁移 IN_SERVICE → 拒绝
        Map<String, Object> bad = new LinkedHashMap<>();
        bad.put("id", id);
        bad.put("status", ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        ApiResponse<?> rejected = executeRpc(GraphQLOperationType.mutation, "ErpAstAsset__update",
                ApiRequest.build(Map.of("data", bad)));
        assertTrue(rejected.getStatus() != 0, "缺年限迁移 IN_SERVICE 应被拒绝");
        assertEquals(ERR_TRI_FACTOR, rejected.getCode(), "迁移时点完备性校验同码");

        // 补齐年限后迁移成功
        Map<String, Object> good = new LinkedHashMap<>();
        good.put("id", id);
        good.put("status", ErpAstConstants.ASSET_STATUS_IN_SERVICE);
        good.put("usefulLifeMonths", 12);
        ApiResponse<?> accepted = executeRpc(GraphQLOperationType.mutation, "ErpAstAsset__update",
                ApiRequest.build(Map.of("data", good)));
        assertEquals(0, accepted.getStatus(), "补齐年限后迁移放行");
        assertEquals(ErpAstConstants.ASSET_STATUS_IN_SERVICE,
                ((Map<?, ?>) accepted.getData()).get("status"), "资产迁移 IN_SERVICE");
    }

    private Map<String, Object> draftAssetData(String code) {
        Map<String, Object> headData = new LinkedHashMap<>();
        headData.put("code", code);
        headData.put("name", "ast2-014-" + code);
        headData.put("acquisitionDate", "2026-07-01");
        headData.put("originalValue", 10000);
        headData.put("status", "DRAFT");
        return headData;
    }

    private ApiResponse<?> saveAsset(Map<String, Object> headData) {
        return executeRpc(GraphQLOperationType.mutation, "ErpAstAsset__save",
                ApiRequest.build(Map.of("data", headData)));
    }

    private Map<String, String> createPrereqs() {
        Map<String, String> ids = new LinkedHashMap<>();
        // 无前置依赖
        return ids;
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
