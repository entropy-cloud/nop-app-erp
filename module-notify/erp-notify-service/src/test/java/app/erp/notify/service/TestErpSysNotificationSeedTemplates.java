package app.erp.notify.service;

import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
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

import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 4 端到端测试：三类通知种子模板（业务提醒/异常告警/系统通知）经 {@code notify(eventType, context)} 全链路验证。
 *
 * <p>覆盖 `docs/architecture/notification-strategy.md` 三类通知的频控窗口与合并策略：
 * <ul>
 *   <li>业务提醒（SLA 超期预警 customer-service）：5 分钟窗口 + MERGE_BY_USER_TYPE 合并为一条</li>
 *   <li>异常告警（过账异常 finance ErpFinPostingException 未处置）：1 分钟窗口 + MERGE_BY_USER_TYPE 合并含次数</li>
 *   <li>系统通知（信用超额度 sales）：不合并 NONE，每条独立</li>
 * </ul>
 * 证明三类频控与通道分支可端到端运行（接收人解析、模板渲染、频控合并、站内落库、markRead/findUnread）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSysNotificationSeedTemplates extends JunitAutoTestCase {

    static final String FINANCE_USER = "seed-finance-user";
    static final String CS_USER = "seed-cs-user";
    static final String SALES_USER = "seed-sales-user";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testBusinessRemindSlaOverdueMergesToSingle() {
        // 业务提醒：SLA 超期预警，5 分钟窗口，合并为一条
        seedTemplate(7101L, "cs.sla-overdue",
                "SLA超期预警: ${customerName}",
                "客户 ${customerName} 工单 ${ticketCode} 已超 SLA，请跟进",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + CS_USER + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_BY_USER_TYPE, 300);

        notify("cs.sla-overdue", Map.of("customerName", "ACME", "ticketCode", "T-201"));
        notify("cs.sla-overdue", Map.of("customerName", "Globex", "ticketCode", "T-202"));

        List<ErpSysNotification> list = notificationsOf(CS_USER, "cs.sla-overdue");
        assertEquals(1, list.size(), "业务提醒 5 分钟窗口内两次应合并为 1 条: " + list.size());
        ErpSysNotification n = list.get(0);
        assertTrue(n.getMergeCount() >= 2, "mergeCount 应 >=2");
        assertTrue(n.getBody().contains("[合并"), "合并 body 含次数标记: " + n.getBody());
        assertEquals(ErpNotifyConstants.CHANNEL_IN_APP, n.getChannel());
    }

    @Test
    public void testAlertPostingExceptionMergesWithCount() {
        // 异常告警：过账异常 finance ErpFinPostingException 未处置，1 分钟窗口，合并含次数
        seedTemplate(7102L, "fin.posting-exception",
                "过账异常告警",
                "过账异常: 单据 ${postingNo} 金额 ${amount}，请立即处置",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + FINANCE_USER + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_BY_USER_TYPE, 60);

        notify("fin.posting-exception", Map.of("postingNo", "P-9001", "amount", "12000"));
        notify("fin.posting-exception", Map.of("postingNo", "P-9002", "amount", "8000"));
        notify("fin.posting-exception", Map.of("postingNo", "P-9003", "amount", "5000"));

        List<ErpSysNotification> list = notificationsOf(FINANCE_USER, "fin.posting-exception");
        assertEquals(1, list.size(), "异常告警 1 分钟窗口内三次应合并为 1 条");
        assertEquals(3, list.get(0).getMergeCount(), "mergeCount 应为 3（含发生次数）");
    }

    @Test
    public void testSystemNoticeCreditOverNoMerge() {
        // 系统通知：信用超额度 sales，不合并，每条独立
        seedTemplate(7103L, "sal.credit-over-limit",
                "信用超额度通知: ${customerName}",
                "客户 ${customerName} 信用额度超限 ${overAmount}，订单 ${orderNo} 已挂起",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + SALES_USER + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_NONE, 0);

        notify("sal.credit-over-limit", Map.of("customerName", "Initech", "overAmount", "5000", "orderNo", "SO-7001"));
        notify("sal.credit-over-limit", Map.of("customerName", "Hooli", "overAmount", "9000", "orderNo", "SO-7002"));

        List<ErpSysNotification> list = notificationsOf(SALES_USER, "sal.credit-over-limit");
        assertEquals(2, list.size(), "系统通知不合并，两次应独立 2 条: " + list.size());
        ErpSysNotification first = list.stream().filter(x -> x.getBody().contains("Initech")).findFirst().orElse(null);
        assertNotNull(first, "首条应渲染 Initech");
        assertTrue(first.getSubject().contains("Initech"), "subject 渲染正确");
        assertEquals(1, first.getMergeCount(), "系统通知 mergeCount 应为 1");
    }

    @Test
    public void testEndToEndMarkReadAndFindUnreadAcrossTypes() {
        seedTemplate(7104L, "cs.sla-overdue-2", "SLA", "sla body",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + CS_USER + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_NONE, 0);
        notify("cs.sla-overdue-2", Map.of());
        notify("cs.sla-overdue-2", Map.of());

        List<ErpSysNotification> all = notificationsOf(CS_USER, "cs.sla-overdue-2");
        assertEquals(2, all.size(), "NONE 策略两条独立");
        assertEquals(2, countUnread(CS_USER), "标记前 2 条未读");

        // 全部标记已读
        int marked = markAllRead(CS_USER);
        assertEquals(2, marked, "markAllRead 应处理 2 条");
        assertEquals(0, countUnread(CS_USER), "markAllRead 后 countUnread 归零");
        assertEquals(0, findUnread(CS_USER).size(), "findUnread 为空");
    }

    // ---------- helpers ----------

    private ApiResponse<?> notify(String eventType, Map<String, Object> context) {
        return rpc(mutation, "ErpSysNotification__notify",
                Map.of("eventType", eventType, "context", context));
    }

    private int markAllRead(String userId) {
        ApiResponse<?> resp = rpc(mutation, "ErpSysNotification__markAllRead", Map.of("userId", userId));
        assertEquals(0, resp.getStatus(), "markAllRead 应成功: " + resp);
        return ((Number) resp.getData()).intValue();
    }

    private int countUnread(String userId) {
        ApiResponse<?> resp = rpc(query, "ErpSysNotification__countUnread", Map.of("userId", userId));
        assertEquals(0, resp.getStatus(), "countUnread 应成功: " + resp);
        return resp.getData() == null ? 0 : ((Number) resp.getData()).intValue();
    }

    @SuppressWarnings("unchecked")
    private List<ErpSysNotification> findUnread(String userId) {
        ApiResponse<?> resp = rpc(query, "ErpSysNotification__findUnread", Map.of("userId", userId));
        assertEquals(0, resp.getStatus(), "findUnread 应成功: " + resp);
        return (List<ErpSysNotification>) resp.getData();
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private List<ErpSysNotification> notificationsOf(String userId, String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        q.addFilter(eq("notificationType", eventType));
        q.addOrderField("createTime", true);
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }

    private void seedTemplate(Long id, String notificationType, String subjectTpl, String bodyTpl,
                              String resolver, String recipientConfig, String channelSet,
                              String mergeStrategy, int mergeWindowSeconds) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", id);
            t.setNotificationType(notificationType);
            t.setName("TPL-" + notificationType);
            t.setChannelSet(channelSet);
            t.setSubjectTpl(subjectTpl);
            t.setBodyTpl(bodyTpl);
            t.setRecipientResolver(resolver);
            t.setRecipientConfig(recipientConfig);
            t.setMergeWindowSeconds(mergeWindowSeconds);
            t.setMergeStrategy(mergeStrategy);
            t.setStatus(ErpNotifyConstants.TEMPLATE_ACTIVE);
            dao.saveEntity(t);
        });
    }
}
