package app.erp.cs.service;

import app.erp.cs.dao.entity.ErpCsKnowledgeBase;
import app.erp.cs.dao.entity.ErpCsSurvey;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
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
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 知识库采纳族端到端测试（RC-R1.69，P1-RC-058，UC-CS-05 ⑦⑧⑨；
 * plan 2026-08-18-1849-1 Phase 2 Proof ①-⑥）。
 *
 * <p>覆盖 ⑦ adoptKnowledge autoResolve（true → 委托 resolveProcessor 转 RESOLVED + survey 触发链；
 * false → 仅审计回归）+ ⑧ ADOPT_KNOWLEDGE 派生统计（knowledgeUsageStats 单条 eq 精确/全量 group，
 * 前缀碰撞防护——id=1 不误配 id=12）+ ⑨ resolve 后置无采纳 → 建议创建条目 notify 落库
 * （recipient=handler；config 关闭跳过）。
 *
 * <p>断言式测试 + 空 autotest.yaml 标记（镜像 R1.65/R1.67/R1.68 范式）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsKnowledgeAdoption extends JunitAutoTestCase {

    @RegisterExtension
    static CsFrozenClockExtension frozenClock = new CsFrozenClockExtension();

    static final String CUSTOMER_ID = "8601";
    static final String TICKET_TYPE_ID = "8701";
    static final String TEMPLATE_ID = "8811";
    static final String HANDLER = "cs-kb-handler";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    // ---------- ① ⑦ autoResolve=true → RESOLVED + resolve 审计 + survey 触发链 ----------

    @Test
    public void testAdoptWithAutoResolveResolvesTicket() {
        String ticketId = seedTicket("TK-KB-AR", ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
        String kbId = seedKbArticle("KB-AR-1");

        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__adoptKnowledge", args(
                "ticketId", ticketId, "knowledgeBaseId", kbId, "autoResolve", true));
        assertEquals(0, resp.getStatus(), "adoptKnowledge(autoResolve=true) 应成功: " + resp);

        ErpCsTicket ticket = reloadTicket(ticketId);
        assertEquals(ErpCsConstants.TICKET_STATUS_RESOLVED, ticket.getStatus(),
                "⑦ L1 ⑤ 采纳文章解决问题 → 直接标记 RESOLVED");
        assertNotNull(ticket.getRemark(), "resolution 应经 resolve 路径写入");
        assertTrue(ticket.getRemark().contains("knowledgeBaseId=" + kbId), "resolution 携带采纳语义");

        // adopt 审计（先写入）+ resolve 审计（NOTE 标记解决）双行齐备
        assertEquals("knowledgeBaseId=" + kbId, findAction(ticketId, ErpCsConstants.ACTION_TYPE_ADOPT_KNOWLEDGE).getContent());
        assertNotNull(findActionContentContains(ticketId, ErpCsConstants.ACTION_TYPE_NOTE, "标记解决"),
                "resolve 审计行应存在（委托既有 resolve 路径）");

        // survey 触发链（config on 默认）：resolve → ErpCsSurvey 落库
        assertEquals(1, countSurveys(ticketId), "resolve 触发链应创建 survey");
    }

    // ---------- ② autoResolve=false（缺省）仅审计（既有行为回归） ----------

    @Test
    public void testAdoptWithoutAutoResolveOnlyAudits() {
        String ticketId = seedTicket("TK-KB-NA", ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
        String kbId = seedKbArticle("KB-NA-1");

        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__adoptKnowledge", args(
                "ticketId", ticketId, "knowledgeBaseId", kbId));
        assertEquals(0, resp.getStatus(), "adoptKnowledge 应成功: " + resp);

        assertEquals(ErpCsConstants.TICKET_STATUS_IN_PROGRESS, reloadTicket(ticketId).getStatus(),
                "缺省 autoResolve=false 不改状态（既有行为回归）");
        assertEquals("knowledgeBaseId=" + kbId,
                findAction(ticketId, ErpCsConstants.ACTION_TYPE_ADOPT_KNOWLEDGE).getContent());
        assertEquals(0, countSurveys(ticketId), "不触发 survey");
    }

    // ---------- ③ ⑧ ADOPT_KNOWLEDGE 审计格式 + knowledgeUsageStats 单条/全量 ----------

    @Test
    public void testKnowledgeUsageStatsDerivedCounting() {
        String ticketA = seedTicket("TK-KB-SA", ErpCsConstants.TICKET_STATUS_NEW);
        String ticketB = seedTicket("TK-KB-SB", ErpCsConstants.TICKET_STATUS_NEW);
        String ticketC = seedTicket("TK-KB-SC", ErpCsConstants.TICKET_STATUS_NEW);
        String kb1 = seedKbArticle("KB-ST-1");
        String kb12 = seedKbArticle("KB-ST-12");

        // kb1 采纳 2 次（不同工单）+ kb12 采纳 1 次
        rpc(mutation, "ErpCsTicket__adoptKnowledge", args("ticketId", ticketA, "knowledgeBaseId", kb1));
        rpc(mutation, "ErpCsTicket__adoptKnowledge", args("ticketId", ticketB, "knowledgeBaseId", kb1));
        rpc(mutation, "ErpCsTicket__adoptKnowledge", args("ticketId", ticketC, "knowledgeBaseId", kb12));

        // 单条 eq 精确匹配：kb1 = 2（kb12=1 不误入）
        ApiResponse<?> single = rpc(query, "ErpCsKnowledgeBase__knowledgeUsageStats",
                args("knowledgeBaseId", kb1));
        assertEquals(0, single.getStatus(), "knowledgeUsageStats 应成功: " + single);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> singleData = (List<Map<String, Object>>) single.getData();
        assertEquals(1, singleData.size(), "单条查询应只返回该 KB 统计");
        assertEquals(kb1, String.valueOf(singleData.get(0).get("knowledgeBaseId")));
        assertEquals(2, toInt(singleData.get(0).get("adoptCount")), "kb1 采纳计数 = 2");

        // 前缀碰撞防护：content eq 精确匹配语义断言（like id=1% 会误配 id=12）
        QueryBean q = new QueryBean();
        q.addFilter(eq("actionType", ErpCsConstants.ACTION_TYPE_ADOPT_KNOWLEDGE));
        q.addFilter(eq("content", "knowledgeBaseId=" + kb1));
        assertEquals(2, daoProvider.daoFor(ErpCsTicketAction.class).findAllByQuery(q).size(),
                "content eq 精确匹配杜绝前缀碰撞");

        // kb12 单条查询 = 1（不误入 kb1 计数）
        ApiResponse<?> other = rpc(query, "ErpCsKnowledgeBase__knowledgeUsageStats",
                args("knowledgeBaseId", kb12));
        assertEquals(0, other.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> otherData = (List<Map<String, Object>>) other.getData();
        assertEquals(1, toInt(otherData.get(0).get("adoptCount")), "kb12 计数独立 = 1");

        // 全量 group：每 KB 一条
        ApiResponse<?> all = rpc(query, "ErpCsKnowledgeBase__knowledgeUsageStats", args());
        assertEquals(0, all.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> allData = (List<Map<String, Object>>) all.getData();
        assertEquals(2, allData.size(), "全量应按 KB 分组 2 条");
        int total = 0;
        for (Map<String, Object> row : allData) {
            total += toInt(row.get("adoptCount"));
        }
        assertEquals(3, total, "全量采纳总数 = 3");

        // 遗留 NOTE 旧格式行不计入（D7 边界）
        seedLegacyNoteAction(ticketA, "采纳知识库文章参考: knowledgeBaseId=" + kb1);
        ApiResponse<?> after = rpc(query, "ErpCsKnowledgeBase__knowledgeUsageStats",
                args("knowledgeBaseId", kb1));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> afterData = (List<Map<String, Object>>) after.getData();
        assertEquals(2, toInt(afterData.get(0).get("adoptCount")), "遗留 NOTE 旧格式行不计入派生统计");
    }

    // ---------- ④ ⑨ resolve 无采纳 → 建议创建条目 notify 落库（recipient=handler） ----------

    @Test
    public void testResolveWithoutAdoptNotifiesSuggestCreate() {
        seedTemplate(TEMPLATE_ID);
        String ticketId = seedTicket("TK-KB-SG", ErpCsConstants.TICKET_STATUS_IN_PROGRESS);

        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__resolve", args("ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "resolve 应成功: " + resp);

        ErpSysNotification n = findNotification();
        assertNotNull(n, "⑨ resolve 无采纳 → 建议创建条目 notify 应落库");
        assertEquals(ErpCsConstants.NOTIFY_EVENT_KNOWLEDGE_SUGGEST_CREATE, n.getNotificationType());
        assertEquals(HANDLER, n.getRecipientUserId(), "接收人 = handler（assignedToId）");
    }

    // ---------- ⑤ ⑨ resolve 有采纳 → 不推送（避免无效建议） ----------

    @Test
    public void testResolveWithAdoptSkipsSuggestNotify() {
        seedTemplate(TEMPLATE_ID);
        String ticketId = seedTicket("TK-KB-SD", ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
        String kbId = seedKbArticle("KB-SD-1");

        rpc(mutation, "ErpCsTicket__adoptKnowledge", args("ticketId", ticketId, "knowledgeBaseId", kbId));
        // 回到 IN_PROCESS 经 reopen（RESOLVED→IN_PROCESS）后再 resolve，工单已有 ADOPT_KNOWLEDGE 行
        rpc(mutation, "ErpCsTicket__reopen", args("ticketId", ticketId));
        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__resolve", args("ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "resolve 应成功: " + resp);

        assertNull(findNotification(), "已有采纳行 → 不推送建议创建（推送语义=无匹配）");
    }

    // ---------- ⑥ config 关闭跳过 ----------

    @Test
    public void testSuggestConfigOffSkipsNotify() {
        seedTemplate(TEMPLATE_ID);
        String ticketId = seedTicket("TK-KB-CF", ErpCsConstants.TICKET_STATUS_IN_PROGRESS);

        AppConfig.getConfigProvider().assignConfigValue(
                ErpCsConstants.CONFIG_KNOWLEDGE_SUGGEST_ON_RESOLVE, "false");
        try {
            ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__resolve", args("ticketId", ticketId));
            assertEquals(0, resp.getStatus(), "resolve 应成功: " + resp);
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpCsConstants.CONFIG_KNOWLEDGE_SUGGEST_ON_RESOLVE, "true");
        }
        assertNull(findNotification(), "config 关闭 → 跳过建议推送");
    }

    // ---------- helpers ----------

    private static Map<String, Object> args(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                               Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private String seedTicket(String code, String status) {
        String id = String.valueOf(8900 + Math.abs(code.hashCode()) % 90);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
            ErpCsTicket t = new ErpCsTicket();
            t.setBusinessDate(LocalDate.of(2026, 7, 1));
            t.orm_propValueByName("id", id);
            t.setCode(code);
            t.setSubject("工单-" + code);
            t.setCustomerId(CUSTOMER_ID);
            t.setTicketTypeId(TICKET_TYPE_ID);
            t.setPriority(ErpCsConstants.TICKET_PRIORITY_NORMAL);
            t.setStatus(status);
            t.setAssignedToId(HANDLER);
            t.setDocStatus(ErpCsConstants.DOC_STATUS_ACTIVE);
            t.setApproveStatus(ErpCsConstants.APPROVE_STATUS_UNSUBMITTED);
            t.setIsSlaCompleted(false);
            dao.saveEntity(t);
        });
        return id;
    }

    private String seedKbArticle(String code) {
        String id = String.valueOf(9200 + Math.abs(code.hashCode()) % 200);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsKnowledgeBase> dao = daoProvider.daoFor(ErpCsKnowledgeBase.class);
            ErpCsKnowledgeBase kb = new ErpCsKnowledgeBase();
            kb.orm_propValueByName("id", id);
            kb.setCode(code);
            kb.setTitle("知识-" + code);
            kb.setContent("内容-" + code);
            kb.setIsPublished(true);
            dao.saveEntity(kb);
        });
        return id;
    }

    private void seedTemplate(String id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", id);
            t.setNotificationType(ErpCsConstants.NOTIFY_EVENT_KNOWLEDGE_SUGGEST_CREATE);
            t.setName("建议创建知识库条目");
            t.setChannelSet("IN_APP");
            t.setSubjectTpl("工单无知识库匹配: ${ticketCode}");
            t.setBodyTpl("工单 ${ticketCode} 解决时未采纳知识库文章，建议创建新条目");
            t.setRecipientResolver("USER_LIST");
            t.setRecipientConfig("{\"userIds\":[\"${handlerUserId}\"]}");
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy("NONE");
            t.setStatus("ACTIVE");
            dao.saveEntity(t);
        });
    }

    private void seedLegacyNoteAction(String ticketId, String content) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicketAction> dao = daoProvider.daoFor(ErpCsTicketAction.class);
            ErpCsTicketAction a = new ErpCsTicketAction();
            a.orm_propValueByName("id", String.valueOf(9900 + Math.abs(content.hashCode()) % 90));
            a.setTicketId(ticketId);
            a.setActionType(ErpCsConstants.ACTION_TYPE_NOTE);
            a.setContent(content);
            dao.saveEntity(a);
        });
    }

    private ErpCsTicket reloadTicket(String ticketId) {
        return daoProvider.daoFor(ErpCsTicket.class).getEntityById(ticketId);
    }

    private ErpCsTicketAction findAction(String ticketId, String actionType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.addFilter(eq("actionType", actionType));
        q.setLimit(1);
        List<ErpCsTicketAction> list = daoProvider.daoFor(ErpCsTicketAction.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpCsTicketAction findActionContentContains(String ticketId, String actionType, String keyword) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.addFilter(eq("actionType", actionType));
        for (ErpCsTicketAction a : daoProvider.daoFor(ErpCsTicketAction.class).findAllByQuery(q)) {
            if (a.getContent() != null && a.getContent().contains(keyword)) {
                return a;
            }
        }
        return null;
    }

    private int countSurveys(String ticketId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        return daoProvider.daoFor(ErpCsSurvey.class).findAllByQuery(q).size();
    }

    private ErpSysNotification findNotification() {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", ErpCsConstants.NOTIFY_EVENT_KNOWLEDGE_SUGGEST_CREATE));
        q.setLimit(1);
        List<ErpSysNotification> list = daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private static int toInt(Object v) {
        return v == null ? 0 : Integer.parseInt(String.valueOf(v));
    }
}
