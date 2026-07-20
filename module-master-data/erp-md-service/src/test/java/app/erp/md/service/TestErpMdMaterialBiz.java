package app.erp.md.service;

import app.erp.md.dao.entity.ErpMdMaterial;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * F7 §3 {@code ErpMdMaterialBizModel} 后端 @BizQuery 单元测试（plan 2026-07-20-1020-2 Phase 2）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>{@code testIsCodeUnique}：新编码 true / 重复 false / excludeId 自身排除 true（3 用例）。</li>
 *   <li>{@code testCountReferences}：0 引用返回空 Map / 多引用返回非零计数（2 用例）。</li>
 * </ul>
 *
 * <p>跨域引用检查经 {@link TestStubMaterialReferenceChecker} 桩模拟（master-data 不反向依赖下游域）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/md/beans/test-material-reference-checker.beans.xml")
public class TestErpMdMaterialBiz extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    TestStubMaterialReferenceChecker refChecker;

    @Test
    public void testIsCodeUnique() {
        Long matId = seedMaterial("E2E-MAT-UNIQ-1");
        // 1. 新编码（无冲突）→ true
        Boolean fresh = (Boolean) rpcData(GraphQLOperationType.query, "ErpMdMaterial__isCodeUnique",
                Map.of("code", "E2E-MAT-UNIQ-NEW"));
        assertEquals(Boolean.TRUE, fresh, "无冲突编码应返回 true");

        // 2. 重复编码（已被占用）→ false
        Boolean dup = (Boolean) rpcData(GraphQLOperationType.query, "ErpMdMaterial__isCodeUnique",
                Map.of("code", "E2E-MAT-UNIQ-1"));
        assertEquals(Boolean.FALSE, dup, "已存在编码应返回 false");

        // 3. excludeId 自身排除 → true（edit 模式保留原 code 不应误判）
        Boolean self = (Boolean) rpcData(GraphQLOperationType.query, "ErpMdMaterial__isCodeUnique",
                Map.of("code", "E2E-MAT-UNIQ-1", "excludeId", matId));
        assertEquals(Boolean.TRUE, self, "excludeId 排除自身后应返回 true");
    }

    @Test
    public void testCountReferences() {
        // 1. 0 引用（未在桩中标记）→ 空 Map
        Long unreferencedId = seedMaterial("E2E-MAT-REF-0");
        @SuppressWarnings("unchecked")
        Map<String, Long> empty = (Map<String, Long>) rpcData(GraphQLOperationType.query,
                "ErpMdMaterial__countReferences", Map.of("id", unreferencedId));
        assertNotNull(empty, "0 引用应返回非 null 空 Map");
        assertTrue(empty.isEmpty(), "0 引用应返回空 Map，实际：" + empty);

        // 2. 多引用（经 SPI 桩注入）→ 非零计数
        Long referencedId = seedMaterial("E2E-MAT-REF-MULTI");
        refChecker.markReferenced(referencedId, "purchaseOrder", 5L);
        refChecker.markReferenced(referencedId, "salesOrder", 3L);
        refChecker.markReferenced(referencedId, "stockMove", 2L);

        @SuppressWarnings("unchecked")
        Map<String, Long> multi = (Map<String, Long>) rpcData(GraphQLOperationType.query,
                "ErpMdMaterial__countReferences", Map.of("id", referencedId));
        assertNotNull(multi, "多引用应返回非 null Map");
        assertEquals(5L, multi.get("purchaseOrder"), "purchaseOrder 计数应为 5");
        assertEquals(3L, multi.get("salesOrder"), "salesOrder 计数应为 3");
        assertEquals(2L, multi.get("stockMove"), "stockMove 计数应为 2");
    }

    // ---------- helpers ----------

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private Object rpcData(GraphQLOperationType opType, String action, Map<String, Object> args) {
        ApiResponse<?> resp = rpc(opType, action, ApiRequest.build(args));
        assertEquals(0, resp.getStatus(), action + " 应成功，实际 code=" + resp.getCode());
        return resp.getData();
    }

    private Long seedMaterial(String code) {
        ErpMdMaterial material = new ErpMdMaterial();
        material.setCode(code);
        material.setName("E2E-" + code);
        material.setMaterialType("GOODS");
        material.setUoMId(1L);
        material.setStatus("ACTIVE");
        ormTemplate.runInSession(() -> materialDao().saveEntity(material));
        return material.getId();
    }

    private IEntityDao<ErpMdMaterial> materialDao() {
        return daoProvider.daoFor(ErpMdMaterial.class);
    }
}
