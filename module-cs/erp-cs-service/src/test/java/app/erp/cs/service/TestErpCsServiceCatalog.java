package app.erp.cs.service;

import app.erp.cs.dao.entity.ErpCsCatalogCategory;
import app.erp.cs.dao.entity.ErpCsCatalogFulfillment;
import app.erp.cs.dao.entity.ErpCsServiceCatalogItem;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 服务目录 BizModel 集成测试（plan 2026-07-07-1430-1 §Phase 3）。覆盖：
 * <ul>
 *   <li>目录分类树校验：parentId 自环/成环/深度超限拒绝，有子节点禁删。</li>
 *   <li>目录项驱动建单 createFromCatalog：ticketType/slaPolicy 自动填充 + catalogItemId 回写 +
 *       表单字段映射（subject/description/urgency→priority）。</li>
 *   <li>履行首步 CREATE_TICKET 落地：建单后 TicketAction 审计含 CREATE_TICKET（DONE） +
 *       INVOKE_WORKFLOW/CREATE_CHILD_TICKET（SKIPPED）。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsServiceCatalog extends JunitAutoTestCase {

    static final Long PARTNER_ID = 9301L;
    static final Long TICKET_TYPE_ID = 6301L;
    static final Long SLA_POLICY_ID = 7301L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- 目录分类树校验 ----------

    @Test
    public void testCategorySelfCycleRejected() {
        Long rootId = seedCategory(5001L, "技术支持", null);
        // 自环：parentId = self
        ApiResponse<?> resp = rpc(mutation, "ErpCsCatalogCategory__update",
                Map.of("data", Map.of("id", rootId, "parentId", rootId)));
        assertEquals(ErpCsErrors.ERR_CATALOG_CATEGORY_CYCLE.getErrorCode(), resp.getCode(),
                "parentId 自环应返回 ERR_CATALOG_CATEGORY_CYCLE");
    }

    @Test
    public void testCategoryChainCycleRejected() {
        Long aId = seedCategory(5010L, "A", null);
        Long bId = seedCategory(5011L, "B", aId);
        // B → A 已有；现把 A 的 parentId 设为 B → 形成环 A→B→A
        ApiResponse<?> resp = rpc(mutation, "ErpCsCatalogCategory__update",
                Map.of("data", Map.of("id", aId, "parentId", bId)));
        assertEquals(ErpCsErrors.ERR_CATALOG_CATEGORY_CYCLE.getErrorCode(), resp.getCode(),
                "parentId 链成环应返回 ERR_CATALOG_CATEGORY_CYCLE");
    }

    @Test
    public void testCategoryMaxDepthExceededRejected() {
        // 默认 maxDepth=3：建 4 层链应失败
        Long l1 = seedCategory(5020L, "L1", null);
        Long l2 = seedCategory(5021L, "L2", l1);
        Long l3 = seedCategory(5022L, "L3", l2);
        // 第 4 层（深度超 3）应拒绝
        ApiResponse<?> resp = rpc(mutation, "ErpCsCatalogCategory__save",
                Map.of("data", Map.of("code", "CAT-L4", "name", "L4", "parentId", l3)));
        assertEquals(ErpCsErrors.ERR_CATALOG_CATEGORY_MAX_DEPTH_EXCEEDED.getErrorCode(), resp.getCode(),
                "深度超 max-depth（默认 3）应返回 ERR_CATALOG_CATEGORY_MAX_DEPTH_EXCEEDED");
    }

    @Test
    public void testCategoryDepthWithinLimitAllowed() {
        // 默认 maxDepth=3：建 3 层链应成功
        Long l1 = seedCategory(5030L, "L1", null);
        Long l2 = seedCategory(5031L, "L2", l1);
        // 第 3 层（深度=3）应允许
        ApiResponse<?> resp = rpc(mutation, "ErpCsCatalogCategory__save",
                Map.of("data", Map.of("code", "CAT-L3", "name", "L3", "parentId", l2)));
        assertEquals(0, resp.getStatus(), "深度=3 应允许建子分类: " + resp);
    }

    @Test
    public void testCategoryDeleteWithChildrenRejected() {
        Long parent = seedCategory(5040L, "父分类", null);
        seedCategory(5041L, "子分类", parent);
        ApiResponse<?> resp = rpc(mutation, "ErpCsCatalogCategory__delete",
                Map.of("id", parent));
        assertEquals(ErpCsErrors.ERR_CATALOG_CATEGORY_HAS_CHILDREN.getErrorCode(), resp.getCode(),
                "有子节点禁删应返回 ERR_CATALOG_CATEGORY_HAS_CHILDREN");
    }

    // ---------- 目录项驱动建单 ----------

    @Test
    public void testCreateFromCatalogFillsTicketFields() {
        seedCustomer(PARTNER_ID, "ACME");
        seedSlaPolicy(SLA_POLICY_ID, TICKET_TYPE_ID);
        Long catalogItemId = seedCatalogItem(6001L, "设备维修", TICKET_TYPE_ID, SLA_POLICY_ID, true);

        Map<String, Object> formData = new HashMap<>();
        formData.put("subject", "打印机故障");
        formData.put("description", "无法开机");
        formData.put("customerId", PARTNER_ID);
        formData.put("urgency", ErpCsConstants.TICKET_PRIORITY_HIGH);

        ApiResponse<?> resp = rpc(mutation, "ErpCsServiceCatalogItem__createFromCatalog",
                Map.of("catalogItemId", catalogItemId, "formData", formData));
        assertEquals(0, resp.getStatus(), "createFromCatalog 应成功: " + resp);

        Map<?, ?> ticketData = (Map<?, ?>) resp.getData();
        Long ticketId = toLong(ticketData.get("id"));
        ErpCsTicket ticket = daoProvider.daoFor(ErpCsTicket.class).getEntityById(ticketId);
        assertEquals(TICKET_TYPE_ID, ticket.getTicketTypeId(), "ticketType 应从目录项自动填充");
        assertEquals(SLA_POLICY_ID, ticket.getSlaPolicyId(), "slaPolicy 应从目录项自动填充");
        assertEquals(catalogItemId, ticket.getCatalogItemId(), "catalogItemId 应回写");
        assertEquals("打印机故障", ticket.getSubject(), "subject 应从 formData 映射");
        assertEquals("无法开机", ticket.getDescription(), "description 应从 formData 映射");
        assertEquals(ErpCsConstants.TICKET_PRIORITY_HIGH, ticket.getPriority(),
                "urgency 应映射到 priority");
        assertEquals(ErpCsConstants.TICKET_STATUS_NEW, ticket.getStatus(), "新建工单状态应为 NEW");
    }

    @Test
    public void testCreateFromCatalogInactiveRejected() {
        seedCustomer(PARTNER_ID, "ACME");
        seedSlaPolicy(SLA_POLICY_ID, TICKET_TYPE_ID);
        Long catalogItemId = seedCatalogItem(6002L, "已下架项", TICKET_TYPE_ID, SLA_POLICY_ID, false);

        ApiResponse<?> resp = rpc(mutation, "ErpCsServiceCatalogItem__createFromCatalog",
                Map.of("catalogItemId", catalogItemId, "formData", new HashMap<>()));
        assertEquals(ErpCsErrors.ERR_CATALOG_ITEM_INACTIVE.getErrorCode(), resp.getCode(),
                "未上架目录项应返回 ERR_CATALOG_ITEM_INACTIVE");
    }

    @Test
    public void testCreateFromCatalogSubjectFallbackToItemName() {
        seedCustomer(PARTNER_ID, "ACME");
        seedSlaPolicy(SLA_POLICY_ID, TICKET_TYPE_ID);
        Long catalogItemId = seedCatalogItem(6003L, "网络咨询", TICKET_TYPE_ID, SLA_POLICY_ID, true);

        // formData 无 subject → 应回退为目录项 name
        Map<String, Object> formData = new HashMap<>();
        formData.put("customerId", PARTNER_ID);

        ApiResponse<?> resp = rpc(mutation, "ErpCsServiceCatalogItem__createFromCatalog",
                Map.of("catalogItemId", catalogItemId, "formData", formData));
        assertEquals(0, resp.getStatus(), "createFromCatalog 应成功: " + resp);

        Long ticketId = toLong(((Map<?, ?>) resp.getData()).get("id"));
        ErpCsTicket ticket = daoProvider.daoFor(ErpCsTicket.class).getEntityById(ticketId);
        assertEquals("网络咨询", ticket.getSubject(), "subject 缺省应回退为目录项 name");
    }

    // ---------- 履行首步 CREATE_TICKET 落地 ----------

    @Test
    public void testFulfillmentCreateTicketStepRegistered() {
        seedCustomer(PARTNER_ID, "ACME");
        seedSlaPolicy(SLA_POLICY_ID, TICKET_TYPE_ID);
        Long catalogItemId = seedCatalogItem(6101L, "履行测试项", TICKET_TYPE_ID, SLA_POLICY_ID, true);
        // 履行步骤：CREATE_TICKET + INVOKE_WORKFLOW（应 SKIPPED）
        seedFulfillmentStep(6201L, catalogItemId, 1, ErpCsConstants.FULFILLMENT_ACTION_CREATE_TICKET);
        seedFulfillmentStep(6202L, catalogItemId, 2, ErpCsConstants.FULFILLMENT_ACTION_INVOKE_WORKFLOW);
        seedFulfillmentStep(6203L, catalogItemId, 3, ErpCsConstants.FULFILLMENT_ACTION_ASSIGN_TEAM);
        seedFulfillmentStep(6204L, catalogItemId, 4, ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER);

        Map<String, Object> formData = new HashMap<>();
        formData.put("customerId", PARTNER_ID);
        formData.put("subject", "履行流程触发");

        ApiResponse<?> resp = rpc(mutation, "ErpCsServiceCatalogItem__createFromCatalog",
                Map.of("catalogItemId", catalogItemId, "formData", formData));
        assertEquals(0, resp.getStatus(), "createFromCatalog 应成功: " + resp);

        Long ticketId = toLong(((Map<?, ?>) resp.getData()).get("id"));
        // 验证 TicketAction 审计已登记各步骤
        List<ErpCsTicketAction> actions = listTicketActions(ticketId);
        assertFalse(actions.isEmpty(), "履行步骤应写入 TicketAction 审计");

        boolean hasCreateTicket = actions.stream().anyMatch(a ->
                ErpCsConstants.FULFILLMENT_ACTION_CREATE_TICKET.equals(a.getActionType())
                        && a.getContent() != null && a.getContent().contains("DONE"));
        boolean hasInvokeWorkflowSkipped = actions.stream().anyMatch(a ->
                ErpCsConstants.FULFILLMENT_ACTION_INVOKE_WORKFLOW.equals(a.getActionType())
                        && a.getContent() != null && a.getContent().contains("SKIPPED"));
        boolean hasAssignTeam = actions.stream().anyMatch(a ->
                ErpCsConstants.FULFILLMENT_ACTION_ASSIGN_TEAM.equals(a.getActionType()));
        boolean hasNotifyCustomer = actions.stream().anyMatch(a ->
                ErpCsConstants.FULFILLMENT_ACTION_NOTIFY_CUSTOMER.equals(a.getActionType()));

        assertTrue(hasCreateTicket, "CREATE_TICKET 步骤应登记为 DONE");
        assertTrue(hasInvokeWorkflowSkipped, "INVOKE_WORKFLOW 应标记 SKIPPED（Non-Goal successor）");
        assertTrue(hasAssignTeam, "ASSIGN_TEAM 应登记执行结果");
        assertTrue(hasNotifyCustomer, "NOTIFY_CUSTOMER 应登记执行结果");
    }

    // ---------- helpers ----------

    private List<ErpCsTicketAction> listTicketActions(Long ticketId) {
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        return daoProvider.daoFor(ErpCsTicketAction.class).findAllByQuery(q);
    }

    private void seedCustomer(Long id, String name) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
            ErpMdPartner p = new ErpMdPartner();
            p.orm_propValueByName("id", id);
            p.setCode("CUS-" + id);
            p.setName(name);
            p.orm_propValueByName("partnerType", "CUSTOMER");
            p.orm_propValueByName("status", "ACTIVE");
            dao.saveEntity(p);
        });
    }

    private void seedSlaPolicy(Long id, Long ticketTypeId) {
        seedTicketType(ticketTypeId);
        ormTemplate.runInSession(() -> {
            IEntityDao<app.erp.cs.dao.entity.ErpCsSlaPolicy> dao =
                    daoProvider.daoFor(app.erp.cs.dao.entity.ErpCsSlaPolicy.class);
            app.erp.cs.dao.entity.ErpCsSlaPolicy p = new app.erp.cs.dao.entity.ErpCsSlaPolicy();
            p.orm_propValueByName("id", id);
            p.setCode("SLA-" + id);
            p.setName("测试 SLA");
            p.setTicketTypeId(ticketTypeId);
            p.setResolveHours(48);
            p.setIsWorkingDays(false);
            dao.saveEntity(p);
        });
    }

    private void seedTicketType(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<app.erp.cs.dao.entity.ErpCsTicketType> dao =
                    daoProvider.daoFor(app.erp.cs.dao.entity.ErpCsTicketType.class);
            app.erp.cs.dao.entity.ErpCsTicketType t = new app.erp.cs.dao.entity.ErpCsTicketType();
            t.orm_propValueByName("id", id);
            t.setCode("TT-" + id);
            t.setName("工单类型-" + id);
            dao.saveEntity(t);
        });
    }

    private Long seedCategory(Long id, String name, Long parentId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsCatalogCategory> dao = daoProvider.daoFor(ErpCsCatalogCategory.class);
            ErpCsCatalogCategory c = new ErpCsCatalogCategory();
            c.orm_propValueByName("id", id);
            c.setCode("CAT-" + id);
            c.setName(name);
            c.setParentId(parentId);
            c.setIsActive(Boolean.TRUE);
            dao.saveEntity(c);
        });
        return id;
    }

    private Long seedCatalogItem(Long id, String name, Long ticketTypeId, Long slaPolicyId, boolean active) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsServiceCatalogItem> dao = daoProvider.daoFor(ErpCsServiceCatalogItem.class);
            ErpCsServiceCatalogItem item = new ErpCsServiceCatalogItem();
            item.orm_propValueByName("id", id);
            item.setCode("ITEM-" + id);
            item.setName(name);
            item.setTicketTypeId(ticketTypeId);
            item.setSlaPolicyId(slaPolicyId);
            item.setIsActive(active);
            item.setIsPublic(Boolean.TRUE);
            dao.saveEntity(item);
        });
        return id;
    }

    private void seedFulfillmentStep(Long id, Long catalogItemId, int sequence, String actionType) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsCatalogFulfillment> dao = daoProvider.daoFor(ErpCsCatalogFulfillment.class);
            ErpCsCatalogFulfillment f = new ErpCsCatalogFulfillment();
            f.orm_propValueByName("id", id);
            f.setCode("FUL-" + id);
            f.setCatalogItemId(catalogItemId);
            f.setSequence(sequence);
            f.setActionType(actionType);
            f.setIsMandatory(false);
            dao.saveEntity(f);
        });
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.valueOf(String.valueOf(v));
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                                Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }
}
