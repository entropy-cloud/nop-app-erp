package app.erp.cs.service;

import app.erp.cs.dao.entity.ErpCsKnowledgeBase;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
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
 * 端到端测试：知识库搜索/建议（UC-CS-05）+ 采纳登记。
 *
 * <p>覆盖 {@code docs/plans/2026-07-08-0056-2-cs-knowledge-base-search-suggestion.md} Phase 3 Proof 项：
 * <ul>
 *   <li>searchKnowledge：命中已发布/排除未发布/categoryId 过滤/limit 钳制/空关键词守门</li>
 *   <li>suggestForTicket：subject 解析/Top 5/过短 subject 守门</li>
 *   <li>adoptKnowledge：采纳登记 TicketAction actionType=NOTE + knowledgeBaseId 引用</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsKnowledgeBaseSearch extends JunitAutoTestCase {

    static final Long TICKET_TYPE_ID = 6001L;
    static final Long CUSTOMER_ID = 5001L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testSearchKnowledgeHitsPublishedExcludesUnpublished() {
        seedKbArticle("KB-PUB-1", "登录密码重置指南", "用户忘记密码时可通过邮箱重置", true, null);
        seedKbArticle("KB-PUB-2", "密码安全策略", "密码需包含大小写字母和数字", true, null);
        seedKbArticle("KB-DRAFT-1", "密码草案未发布", "此文章尚未发布", false, null);

        ApiResponse<?> resp = rpc(query, "ErpCsKnowledgeBase__searchKnowledge",
                args("keyword", "密码"));
        assertEquals(0, resp.getStatus(), "searchKnowledge 应成功: " + resp);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.getData();
        assertNotNull(data);
        assertEquals(2, data.size(), "应命中 2 篇已发布文章（排除未发布）");
        for (Map<String, Object> article : data) {
            assertNotNull(article.get("id"));
            assertNotNull(article.get("code"));
            assertNotNull(article.get("title"));
            assertNotNull(article.get("contentSummary"));
            assertFalse(article.get("title").toString().contains("草案"),
                    "未发布文章不应出现在结果中");
        }
    }

    @Test
    public void testSearchKnowledgeCategoryFilter() {
        seedKbArticle("KB-CAT-1", "退货流程指南", "客户退货操作步骤", true, 1001L);
        seedKbArticle("KB-CAT-2", "退货物流说明", "退货物流费用说明", true, 1002L);
        seedKbArticle("KB-CAT-3", "退货政策摘要", "7天无理由退货政策", true, 1001L);

        ApiResponse<?> resp = rpc(query, "ErpCsKnowledgeBase__searchKnowledge",
                args("keyword", "退货", "categoryId", 1001L));
        assertEquals(0, resp.getStatus(), "categoryId 过滤应成功: " + resp);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.getData();
        assertEquals(2, data.size(), "categoryId=1001 应命中 2 篇");
        for (Map<String, Object> article : data) {
            assertEquals(1001L, toLong(article.get("categoryId")));
        }
    }

    @Test
    public void testSearchKnowledgeLimitClamping() {
        for (int i = 1; i <= 10; i++) {
            seedKbArticle("KB-LIMIT-" + i, "测试文章" + i, "测试内容" + i, true, null);
        }
        // limit=3 应只返回 3 条
        ApiResponse<?> resp = rpc(query, "ErpCsKnowledgeBase__searchKnowledge",
                args("keyword", "测试", "limit", 3));
        assertEquals(0, resp.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.getData();
        assertEquals(3, data.size(), "limit=3 应返回 3 条");

        // limit=0 应使用默认值（5）
        resp = rpc(query, "ErpCsKnowledgeBase__searchKnowledge",
                args("keyword", "测试", "limit", 0));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data2 = (List<Map<String, Object>>) resp.getData();
        assertEquals(5, data2.size(), "limit=0 应使用默认值 5");

        // limit=999 应钳制到 max（20），但只有 10 条数据
        resp = rpc(query, "ErpCsKnowledgeBase__searchKnowledge",
                args("keyword", "测试", "limit", 999));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data3 = (List<Map<String, Object>>) resp.getData();
        assertEquals(10, data3.size(), "limit=999 钳制到 max=20，但只有 10 条数据");
    }

    @Test
    public void testSearchKnowledgeEmptyKeywordReturnsEmpty() {
        seedKbArticle("KB-EMPTY-1", "空关键词测试", "内容", true, null);

        ApiResponse<?> resp = rpc(query, "ErpCsKnowledgeBase__searchKnowledge",
                args("keyword", ""));
        assertEquals(0, resp.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.getData();
        assertTrue(data == null || data.isEmpty(), "空关键词应返回空集");

        resp = rpc(query, "ErpCsKnowledgeBase__searchKnowledge",
                args("keyword", "   "));
        assertEquals(0, resp.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data2 = (List<Map<String, Object>>) resp.getData();
        assertTrue(data2 == null || data2.isEmpty(), "空白关键词应返回空集");
    }

    @Test
    public void testSuggestForTicketParsesSubject() {
        seedKbArticle("KB-SUGG-1", "发票申请流程", "客户可在线申请发票", true, null);
        seedKbArticle("KB-SUGG-2", "发票邮寄说明", "发票通过EMS邮寄", true, null);
        seedKbArticle("KB-SUGG-3", "发票内容规范", "发票内容需与订单一致", true, null);
        seedKbArticle("KB-SUGG-4", "发票税率说明", "不同商品税率不同", true, null);
        seedKbArticle("KB-SUGG-5", "发票作废流程", "发票作废需联系客服", true, null);
        seedKbArticle("KB-SUGG-6", "发票归档", "发票归档流程", true, null);

        ApiResponse<?> resp = rpc(query, "ErpCsKnowledgeBase__suggestForTicket",
                args("subject", "发票 帮助"));
        assertEquals(0, resp.getStatus(), "suggestForTicket 应成功: " + resp);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.getData();
        assertNotNull(data);
        assertTrue(data.size() <= 5, "suggestForTicket 应返回最多 Top 5");
        assertFalse(data.isEmpty(), "应至少命中 1 篇");
    }

    @Test
    public void testSuggestForTicketShortSubjectReturnsEmpty() {
        seedKbArticle("KB-SHORT-1", "测试", "内容", true, null);

        ApiResponse<?> resp = rpc(query, "ErpCsKnowledgeBase__suggestForTicket",
                args("subject", "a"));
        assertEquals(0, resp.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.getData();
        assertTrue(data == null || data.isEmpty(), "过短 subject 应返回空集");

        resp = rpc(query, "ErpCsKnowledgeBase__suggestForTicket",
                args("subject", ""));
        assertEquals(0, resp.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data2 = (List<Map<String, Object>>) resp.getData();
        assertTrue(data2 == null || data2.isEmpty(), "空 subject 应返回空集");
    }

    @Test
    public void testAdoptKnowledgeRecordsTicketAction() {
        Long ticketId = seedTicket("TK-ADOPT", ErpCsConstants.TICKET_STATUS_NEW);
        Long kbId = seedKbArticle("KB-ADOPT-1", "采纳测试文章", "采纳测试内容", true, null);

        int actionsBefore = countActions(ticketId);

        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__adoptKnowledge",
                args("ticketId", ticketId, "knowledgeBaseId", kbId));
        assertEquals(0, resp.getStatus(), "adoptKnowledge 应成功: " + resp);

        int actionsAfter = countActions(ticketId);
        assertTrue(actionsAfter > actionsBefore, "采纳应生成 TicketAction 审计");

        // 验证 actionType=NOTE 且 content 含 knowledgeBaseId 引用
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.addFilter(eq("actionType", ErpCsConstants.ACTION_TYPE_NOTE));
        List<ErpCsTicketAction> noteActions = daoProvider.daoFor(ErpCsTicketAction.class).findAllByQuery(q);
        boolean foundAdopt = false;
        for (ErpCsTicketAction action : noteActions) {
            if (action.getContent() != null && action.getContent().contains(String.valueOf(kbId))) {
                foundAdopt = true;
                break;
            }
        }
        assertTrue(foundAdopt, "应存在 actionType=NOTE 且 content 含 knowledgeBaseId 的审计记录");
    }

    // ---------- helpers ----------

    private Long seedKbArticle(String code, String title, String content, boolean published, Long categoryId) {
        Long id = 9000L + (long) (Math.abs(code.hashCode()) % 1000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsKnowledgeBase> dao = daoProvider.daoFor(ErpCsKnowledgeBase.class);
            ErpCsKnowledgeBase kb = new ErpCsKnowledgeBase();
            kb.orm_propValueByName("id", id);
            kb.setCode(code);
            kb.setTitle(title);
            kb.setContent(content);
            kb.setIsPublished(published);
            if (categoryId != null) {
                kb.setCategoryId(categoryId);
            }
            dao.saveEntity(kb);
        });
        return id;
    }

    private Long seedTicket(String code, String status) {
        Long id = 7000L + (long) (Math.abs(code.hashCode()) % 1000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
            ErpCsTicket t = new ErpCsTicket();
            t.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
            t.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
            t.orm_propValueByName("id", id);
            t.setCode(code);
            t.setSubject("工单-" + code);
            t.setCustomerId(CUSTOMER_ID);
            t.setTicketTypeId(TICKET_TYPE_ID);
            t.setPriority(ErpCsConstants.TICKET_PRIORITY_HIGH);
            t.setStatus(status);
            t.setDocStatus(ErpCsConstants.DOC_STATUS_ACTIVE);
            t.setApproveStatus(ErpCsConstants.APPROVE_STATUS_UNSUBMITTED);
            t.setIsSlaCompleted(false);
            dao.saveEntity(t);
        });
        return id;
    }

    private int countActions(Long ticketId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        return daoProvider.daoFor(ErpCsTicketAction.class).findAllByQuery(q).size();
    }

    private static Map<String, Object> args(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
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
