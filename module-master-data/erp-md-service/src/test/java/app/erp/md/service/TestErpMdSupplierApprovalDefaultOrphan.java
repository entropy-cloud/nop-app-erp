package app.erp.md.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSupplierApproval;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * P2-CK-md-015-r3 供应商准入 status 孤儿默认值回归。
 *
 * <p>缺陷面：{@code ErpMdSupplierApproval.status} ORM defaultValue="10" 为字典外孤儿值——裸 save 建行
 * 落 status="10" 后 {@code assertCanApply}（仅容 null/REJECTED）永不能 apply，且
 * {@code findEffectiveByPartner} 将其当有效资格返回污染采购资格链。
 *
 * <p>修复裁决 = 移除 defaultValue（保留 mandatory + ext:dict）：裸 save 缺 status 被 mandatory 校验
 * 拒绝（fail-fast），合法创建路径（apply/seed/测试）显式赋值；种子零 "10" 行故零迁移。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdSupplierApprovalDefaultOrphan extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testBareSaveWithoutStatusRejected() {
        String partnerId = seedPartner();
        String categoryId = seedMaterialCategory();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("partnerId", partnerId);
        data.put("approvalType", "NEW");
        data.put("materialCategoryId", categoryId);
        data.put("validFrom", "2026-01-01");
        data.put("validTo", "2026-12-31");
        // 无 status：孤儿默认值移除后应由 mandatory 校验拒绝（fail-fast），不得静默落 "10"

        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpMdSupplierApproval__save",
                Map.of("data", data));
        assertTrue(resp.getStatus() != 0,
                "裸 save 缺 status 应被 mandatory 校验拒绝（孤儿默认值 defaultValue=10 已移除）: " + resp);
    }

    @Test
    public void testExplicitStatusSaveAndApplyStillWorks() {
        String partnerId = seedPartner();
        String categoryId = seedMaterialCategory();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("partnerId", partnerId);
        data.put("approvalType", "NEW");
        data.put("materialCategoryId", categoryId);
        data.put("validFrom", "2026-01-01");
        data.put("validTo", "2026-12-31");
        data.put("status", "REJECTED");

        ApiResponse<?> resp = rpc(GraphQLOperationType.mutation, "ErpMdSupplierApproval__save",
                Map.of("data", data));
        assertEquals(0, resp.getStatus(), "显式 status 建行应成功: " + resp);
        String id = String.valueOf(((Map<?, ?>) resp.getData()).get("id"));

        ApiResponse<?> apply = rpc(GraphQLOperationType.mutation, "ErpMdSupplierApproval__apply",
                Map.of("approvalId", id));
        assertEquals(0, apply.getStatus(), "REJECTED 行应可 apply（重新申请）: " + apply);
        assertEquals("APPLIED", reloadStatus(id), "apply 后 status=APPLIED");
    }

    private String reloadStatus(String id) {
        ErpMdSupplierApproval approval = ormTemplate.runInSession(session ->
                daoProvider.daoFor(ErpMdSupplierApproval.class).getEntityById(id));
        return approval.getStatus();
    }

    private String seedPartner() {
        return ormTemplate.runInSession(session -> {
            ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
            p.setCode("MD-DFT-PARTNER-" + System.nanoTime());
            p.setName("孤儿默认值测试伙伴");
            p.setPartnerType("SUPPLIER");
            p.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
            return p.getId();
        });
    }

    private String seedMaterialCategory() {
        return ormTemplate.runInSession(session -> {
            app.erp.md.dao.entity.ErpMdMaterialCategory c = daoProvider.daoFor(app.erp.md.dao.entity.ErpMdMaterialCategory.class).newEntity();
            c.setCode("MD-DFT-CAT-" + System.nanoTime());
            c.setName("孤儿默认值测试分类");
            daoProvider.daoFor(app.erp.md.dao.entity.ErpMdMaterialCategory.class).saveEntity(c);
            return c.getId();
        });
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }
}
