package app.erp.md.service;

import app.erp.md.dao.entity.ErpMdPartner;
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
 * F7 §3 {@code ErpMdPartnerBizModel} 后端 @BizQuery 单元测试（plan 2026-07-20-1020-2 Phase 2）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>{@code testIsCodeUnique}：新编码 true / 重复 false / excludeId 自身排除 true（3 用例）。</li>
 *   <li>{@code testCountReferences}：0 引用返回空 Map / 多引用返回非零计数（2 用例）。</li>
 * </ul>
 *
 * <p>跨域引用检查经 {@link TestStubPartnerReferenceChecker} 桩模拟（master-data 不反向依赖下游域）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/md/beans/test-partner-reference-checker.beans.xml")
public class TestErpMdPartnerBiz extends JunitAutoTestCase {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    TestStubPartnerReferenceChecker refChecker;

    @Test
    public void testIsCodeUnique() {
        Long partnerId = seedPartner("E2E-PART-UNIQ-1");
        // 1. 新编码（无冲突）→ true
        Boolean fresh = (Boolean) rpcData(GraphQLOperationType.query, "ErpMdPartner__isCodeUnique",
                Map.of("code", "E2E-PART-UNIQ-NEW"));
        assertEquals(Boolean.TRUE, fresh, "无冲突编码应返回 true");

        // 2. 重复编码（已被占用）→ false
        Boolean dup = (Boolean) rpcData(GraphQLOperationType.query, "ErpMdPartner__isCodeUnique",
                Map.of("code", "E2E-PART-UNIQ-1"));
        assertEquals(Boolean.FALSE, dup, "已存在编码应返回 false");

        // 3. excludeId 自身排除 → true（edit 模式保留原 code 不应误判）
        Boolean self = (Boolean) rpcData(GraphQLOperationType.query, "ErpMdPartner__isCodeUnique",
                Map.of("code", "E2E-PART-UNIQ-1", "excludeId", partnerId));
        assertEquals(Boolean.TRUE, self, "excludeId 排除自身后应返回 true");
    }

    @Test
    public void testCountReferences() {
        // 1. 0 引用（未在桩中标记）→ 空 Map
        Long unreferencedId = seedPartner("E2E-PART-REF-0");
        @SuppressWarnings("unchecked")
        Map<String, Long> empty = (Map<String, Long>) rpcData(GraphQLOperationType.query,
                "ErpMdPartner__countReferences", Map.of("id", unreferencedId));
        assertNotNull(empty, "0 引用应返回非 null 空 Map");
        assertTrue(empty.isEmpty(), "0 引用应返回空 Map，实际：" + empty);

        // 2. 多引用（经 SPI 桩注入）→ 非零计数
        Long referencedId = seedPartner("E2E-PART-REF-MULTI");
        refChecker.markReferenced(referencedId, "purchaseOrder", 7L);
        refChecker.markReferenced(referencedId, "salesOrder", 4L);

        @SuppressWarnings("unchecked")
        Map<String, Long> multi = (Map<String, Long>) rpcData(GraphQLOperationType.query,
                "ErpMdPartner__countReferences", Map.of("id", referencedId));
        assertNotNull(multi, "多引用应返回非 null Map");
        assertEquals(7L, multi.get("purchaseOrder"), "purchaseOrder 计数应为 7");
        assertEquals(4L, multi.get("salesOrder"), "salesOrder 计数应为 4");
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

    private Long seedPartner(String code) {
        ErpMdPartner partner = new ErpMdPartner();
        partner.setCode(code);
        partner.setName("E2E-" + code);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus("ACTIVE");
        ormTemplate.runInSession(() -> partnerDao().saveEntity(partner));
        return partner.getId();
    }

    private IEntityDao<ErpMdPartner> partnerDao() {
        return daoProvider.daoFor(ErpMdPartner.class);
    }
}
