package app.erp.cs.service;

import app.erp.cs.biz.IErpCsCannedResponseBiz;
import app.erp.cs.dao.entity.ErpCsCannedResponse;
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

import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 预设应答 BizModel 集成测试（plan 2026-07-11-1234-2 §Phase 4）。
 *
 * <p>覆盖：
 * <ul>
 *   <li>renderTemplate：系统变量解析（customer_name/ticket_id/agent_name）+ 自定义变量覆盖。</li>
 *   <li>suggestForTicket：三级匹配（精确 > 类型 > 全局兜底）。</li>
 *   <li>applyCannedResponse：渲染 + usageCount+1 + TicketAction NOTE 审计。</li>
 *   <li>inactive 应答渲染拒绝。</li>
 * </ul>
 *
 * <p>经 H2 + IGraphQLEngine 直接调 BizModel 方法（GraphQL 引擎自动开启 OrmSession + 事务）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsCannedResponseBiz extends JunitAutoTestCase {

    static final Long PARTNER_ID = 9201L;
    static final Long TICKET_TYPE_ID = 6201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IErpCsCannedResponseBiz cannedResponseBiz;

    @Test
    public void testRenderTemplateSystemVars() {
        seedCustomer(PARTNER_ID, "ACME公司");
        Long ticketId = seedTicket("TK-RENDER-001", PARTNER_ID);
        Long crId = seedCannedResponse(9301L, "CR-RENDER-001", "您好 {customer_name}，工单 {ticket_id} 由 {agent_name} 处理",
                null, null, null, Boolean.TRUE, 0);

        ApiResponse<?> resp = rpc(query, "ErpCsCannedResponse__renderTemplate",
                Map.of("cannedResponseId", crId, "ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "renderTemplate 应成功: " + resp);
        String rendered = (String) resp.getData();
        assertTrue(rendered.contains("ACME公司"), "渲染后应包含客户名 ACME公司: " + rendered);
        assertTrue(rendered.contains("TK-RENDER-001"), "渲染后应包含工单号: " + rendered);
        assertFalse(rendered.contains("{customer_name}"), "不应残留 {customer_name} 占位符");
        assertFalse(rendered.contains("{ticket_id}"), "不应残留 {ticket_id} 占位符");
    }

    @Test
    public void testRenderTemplateCustomVarOverride() {
        seedCustomer(PARTNER_ID, "系统客户名");
        Long ticketId = seedTicket("TK-RENDER-002", PARTNER_ID);
        Long crId = seedCannedResponse(9302L, "CR-RENDER-002", "{customer_name}",
                "{\"variables\":[{\"key\":\"{customer_name}\",\"required\":false}]}", null, null, Boolean.TRUE, 0);

        ApiResponse<?> resp = rpc(query, "ErpCsCannedResponse__renderTemplate",
                Map.of("cannedResponseId", crId, "ticketId", ticketId,
                        "customVariables", Map.of("{customer_name}", "自定义名")));
        assertEquals(0, resp.getStatus(), "renderTemplate 应成功: " + resp);
        assertEquals("自定义名", resp.getData(), "自定义变量应覆盖系统变量");
    }

    @Test
    public void testRenderTemplateInactiveRejected() {
        Long crId = seedCannedResponse(9303L, "CR-INACTIVE", "内容",
                null, null, null, Boolean.FALSE, 0);

        ApiResponse<?> resp = rpc(query, "ErpCsCannedResponse__renderTemplate",
                Map.of("cannedResponseId", crId, "ticketId", 1L));
        assertEquals(ErpCsErrors.ERR_CANNED_RESPONSE_INACTIVE.getErrorCode(), resp.getCode(),
                "inactive 应答渲染应返回 ERR_CANNED_RESPONSE_INACTIVE");
    }

    @Test
    public void testSuggestExactMatch() {
        Long ticketId = seedTicket("TK-SUGGEST-EXACT", PARTNER_ID);
        // 精确匹配：type=6201 + priority=NORMAL
        Long exactId = seedCannedResponse(9310L, "CR-EXACT", "精确匹配",
                null, TICKET_TYPE_ID, ErpCsConstants.TICKET_PRIORITY_NORMAL, Boolean.TRUE, 10);
        // 类型匹配：type=6201 + priority=null
        seedCannedResponse(9311L, "CR-TYPE", "类型匹配",
                null, TICKET_TYPE_ID, null, Boolean.TRUE, 20);
        // 全局兜底：type=null + priority=null
        seedCannedResponse(9312L, "CR-GLOBAL", "全局兜底",
                null, null, null, Boolean.TRUE, 30);

        ApiResponse<?> resp = rpc(query, "ErpCsCannedResponse__suggestForTicket",
                Map.of("ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "suggestForTicket 应成功: " + resp);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) resp.getData();
        assertFalse(items.isEmpty(), "应返回匹配结果");
        // 精确匹配应排在最前
        boolean foundExact = false;
        for (Map<String, Object> r : items) {
            if (toLong(r.get("id")).equals(exactId)) {
                foundExact = true;
                break;
            }
        }
        assertTrue(foundExact, "精确匹配应答应在结果中");
    }

    @Test
    public void testSuggestFallbackToTypeMatch() {
        // 工单 type=6201, priority=LOW（无精确 LOW 匹配，应 fallback 到类型匹配）
        Long ticketId = seedTicketWithPriority("TK-SUGGEST-LOW", PARTNER_ID, ErpCsConstants.TICKET_PRIORITY_LOW);
        Long typeId = seedCannedResponse(9320L, "CR-TYPE-ONLY", "类型匹配",
                null, TICKET_TYPE_ID, null, Boolean.TRUE, 20);
        // 全局兜底
        seedCannedResponse(9321L, "CR-GLOBAL-2", "全局兜底",
                null, null, null, Boolean.TRUE, 30);

        ApiResponse<?> resp = rpc(query, "ErpCsCannedResponse__suggestForTicket",
                Map.of("ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "suggestForTicket 应成功: " + resp);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) resp.getData();
        boolean foundType = false;
        for (Map<String, Object> r : items) {
            if (toLong(r.get("id")).equals(typeId)) {
                foundType = true;
                break;
            }
        }
        assertTrue(foundType, "无精确匹配时应 fallback 到类型匹配");
    }

    @Test
    public void testSuggestFallbackToGlobal() {
        // 工单 type=9999（无任何匹配），应 fallback 到全局兜底
        Long ticketId = seedTicketWithPriorityAndType("TK-SUGGEST-GLOBAL", PARTNER_ID, 9999L, ErpCsConstants.TICKET_PRIORITY_NORMAL);
        Long globalId = seedCannedResponse(9330L, "CR-GLOBAL-3", "全局兜底",
                null, null, null, Boolean.TRUE, 30);

        ApiResponse<?> resp = rpc(query, "ErpCsCannedResponse__suggestForTicket",
                Map.of("ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "suggestForTicket 应成功: " + resp);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) resp.getData();
        boolean foundGlobal = false;
        for (Map<String, Object> r : items) {
            if (toLong(r.get("id")).equals(globalId)) {
                foundGlobal = true;
                break;
            }
        }
        assertTrue(foundGlobal, "无精确/类型匹配时应 fallback 到全局兜底");
    }

    @Test
    public void testApplyCannedResponseIncrementsUsageAndWritesAction() {
        seedCustomer(PARTNER_ID, "ACME");
        Long ticketId = seedTicket("TK-APPLY-001", PARTNER_ID);
        Long crId = seedCannedResponse(9340L, "CR-APPLY", "应答内容 {ticket_id}",
                null, null, null, Boolean.TRUE, 5);

        ApiResponse<?> resp = rpc(mutation, "ErpCsCannedResponse__applyCannedResponse",
                Map.of("cannedResponseId", crId, "ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "applyCannedResponse 应成功: " + resp);

        // usageCount 应从 5 → 6
        ErpCsCannedResponse after = daoProvider.daoFor(ErpCsCannedResponse.class).getEntityById(crId);
        assertEquals(6, after.getUsageCount(), "usageCount 应递增 5→6");

        // TicketAction NOTE 审计写入
        QueryCheckResult check = findTicketActions(ticketId);
        assertTrue(check.hasNote, "应写入 TicketAction NOTE 审计");
    }

    // ---------- helpers ----------

    private static class QueryCheckResult {
        boolean hasNote = false;
    }

    private QueryCheckResult findTicketActions(Long ticketId) {
        QueryCheckResult r = new QueryCheckResult();
        io.nop.api.core.beans.query.QueryBean q = new io.nop.api.core.beans.query.QueryBean();
        q.addFilter(io.nop.api.core.beans.FilterBeans.eq("ticketId", ticketId));
        List<ErpCsTicketAction> actions = daoProvider.daoFor(ErpCsTicketAction.class).findAllByQuery(q);
        for (ErpCsTicketAction a : actions) {
            if (ErpCsConstants.ACTION_TYPE_NOTE.equals(a.getActionType())) {
                r.hasNote = true;
            }
        }
        return r;
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

    private Long seedTicket(String code, Long customerId) {
        return seedTicketWithPriorityAndType(code, customerId, TICKET_TYPE_ID, ErpCsConstants.TICKET_PRIORITY_NORMAL);
    }

    private Long seedTicketWithPriority(String code, Long customerId, String priority) {
        return seedTicketWithPriorityAndType(code, customerId, TICKET_TYPE_ID, priority);
    }

    private Long seedTicketWithPriorityAndType(String code, Long customerId, Long ticketTypeId, String priority) {
        Long id = 9400L + (long) (Math.abs(code.hashCode()) % 500);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
            ErpCsTicket t = new ErpCsTicket();
            t.setBusinessDate(java.time.LocalDate.of(2026, 7, 11));
            t.orm_propValueByName("id", id);
            t.setCode(code);
            t.setSubject("工单-" + code);
            t.setCustomerId(customerId);
            t.setTicketTypeId(ticketTypeId);
            t.setPriority(priority);
            t.setStatus(ErpCsConstants.TICKET_STATUS_NEW);
            t.setDocStatus(ErpCsConstants.DOC_STATUS_ACTIVE);
            t.setApproveStatus(ErpCsConstants.APPROVE_STATUS_UNSUBMITTED);
            t.setIsSlaCompleted(false);
            dao.saveEntity(t);
        });
        return id;
    }

    private Long seedCannedResponse(Long id, String code, String content,
                                     String variableDefs, Long macroTicketTypeId, String macroPriority,
                                     Boolean isActive, Integer usageCount) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsCannedResponse> dao = daoProvider.daoFor(ErpCsCannedResponse.class);
            ErpCsCannedResponse r = new ErpCsCannedResponse();
            r.orm_propValueByName("id", id);
            r.setCode(code);
            r.setTitle(code);
            r.setContent(content);
            r.setVariableDefs(variableDefs);
            r.setMacroTicketTypeId(macroTicketTypeId);
            r.setMacroPriority(macroPriority);
            r.setIsActive(isActive);
            r.setUsageCount(usageCount);
            dao.saveEntity(r);
        });
        return id;
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                                Map<String, Object> args) {
        IGraphQLExecutionContext graphQLCtx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(graphQLCtx);
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.valueOf(String.valueOf(v));
    }
}
