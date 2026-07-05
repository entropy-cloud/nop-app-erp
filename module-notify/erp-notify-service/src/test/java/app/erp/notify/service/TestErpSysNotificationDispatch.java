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

import java.sql.Timestamp;
import java.time.LocalDateTime;
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
 * Phase 3 测试：通知派发引擎 notify 入口 + 频控合并 + 站内消息 + 已读状态。
 *
 * <p>覆盖 `docs/architecture/notification-strategy.md` 三类通知频控规则与
 * `docs/plans/2026-07-06-0504-1-notification-dispatch-subsystem.md` Phase 3 Exit Criteria。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSysNotificationDispatch extends JunitAutoTestCase {

    static final String USER_1 = "notify-test-user-1";
    static final String USER_2 = "notify-test-user-2";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testNotifyPersistsInAppAndRendered() {
        seedTemplate(7001L, "sla-overdue", "SLA超期预警: ${customerName} 工单 ${ticketCode}",
                "客户 ${customerName} 的工单 ${ticketCode} 已超期，请处理",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + USER_1 + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_NONE, 0);

        ApiResponse<?> resp = notify("sla-overdue", Map.of("customerName", "ACME", "ticketCode", "T-100"));
        assertEquals(0, resp.getStatus(), "notify 应成功: " + resp);

        ErpSysNotification n = findNotification(USER_1, "sla-overdue");
        assertNotNull(n, "站内消息应落库");
        assertEquals(ErpNotifyConstants.STATUS_SENT, n.getStatus());
        assertEquals(ErpNotifyConstants.CHANNEL_IN_APP, n.getChannel());
        assertEquals(USER_1, n.getRecipientUserId());
        assertEquals("SLA超期预警: ACME 工单 T-100", n.getSubject(), "subject 模板应渲染");
        assertTrue(n.getBody().contains("ACME") && n.getBody().contains("T-100"), "body 模板应渲染: " + n.getBody());
        assertNotNull(n.getPayloadJson(), "payloadJson 应记录 context");

        List<ErpSysNotification> unread = findUnread(USER_1);
        assertEquals(1, unread.size(), "findUnread 应返回该实例");
    }

    @Test
    public void testMergeWithinWindowMerges() {
        seedTemplate(7002L, "posting-alert", "过账异常",
                "过账异常发生",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + USER_2 + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_BY_USER_TYPE, 3600);

        notify("posting-alert", Map.of());
        notify("posting-alert", Map.of());

        List<ErpSysNotification> list = notificationsOf(USER_2, "posting-alert");
        assertEquals(1, list.size(), "频控窗口内二次 notify 应合并为 1 行: " + list.size());
        ErpSysNotification n = list.get(0);
        assertTrue(n.getMergeCount() >= 2, "mergeCount 应递增到 >=2: " + n.getMergeCount());
        assertTrue(n.getBody().contains("[合并"), "合并 body 应含合并标记: " + n.getBody());
    }

    @Test
    public void testMergeOutsideWindowCreatesNew() {
        seedTemplate(7003L, "outside-win", "窗口外测试",
                "窗口外实例",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + USER_1 + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_BY_USER_TYPE, 3600);

        notify("outside-win", Map.of());
        // 将首条 createTime 推到 2 小时前，使第二次 notify 落在窗口外
        ErpSysNotification first = findNotification(USER_1, "outside-win");
        assertNotNull(first);
        ormTemplate.runInSession(() -> {
            ErpSysNotification attached = daoProvider.daoFor(ErpSysNotification.class).getEntityById(first.getId());
            attached.setCreateTime(Timestamp.valueOf(LocalDateTime.now().minusHours(2)));
            daoProvider.daoFor(ErpSysNotification.class).updateEntity(attached);
        });

        notify("outside-win", Map.of());

        List<ErpSysNotification> list = notificationsOf(USER_1, "outside-win");
        assertEquals(2, list.size(), "窗口外应新建第 2 行: " + list.size());
    }

    @Test
    public void testEmailChannelConfigGatedSkipsAndMarkRead() {
        // channelSet 含 EMAIL，但 bootstrap 默认 email-enabled=false → 跳过派发仅 WARN，不抛错、不阻断
        seedTemplate(7004L, "credit-over", "信用超额度",
                "客户信用超额度",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + USER_1 + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP + "," + ErpNotifyConstants.CHANNEL_EMAIL,
                ErpNotifyConstants.MERGE_NONE, 0);

        ApiResponse<?> resp = notify("credit-over", Map.of());
        assertEquals(0, resp.getStatus(), "邮件 config-gated 跳过不应抛错: " + resp);

        ErpSysNotification n = findNotification(USER_1, "credit-over");
        assertNotNull(n, "站内消息仍应落库（IN_APP 永落）");

        assertEquals(1, countUnread(USER_1), "标记前应 1 条未读");
        markRead(n.getId());
        assertEquals(0, countUnread(USER_1), "markRead 后 countUnread 应归零");
        assertNull(findUnread(USER_1).stream().filter(x -> x.getId().equals(n.getId())).findFirst().orElse(null),
                "已标记已读的通知不应出现在 findUnread");
    }

    @Test
    public void testNoActiveTemplateSilentSkip() {
        // 无 ACTIVE 模板 → config-gated 静默跳过，不阻断调用方
        ApiResponse<?> resp = notify("non-existent-event", Map.of());
        assertEquals(0, resp.getStatus(), "无 ACTIVE 模板应静默跳过，不抛错: " + resp);
    }

    // ---------- helpers ----------

    private ApiResponse<?> notify(String eventType, Map<String, Object> context) {
        return rpc(mutation, "ErpSysNotification__notify",
                Map.of("eventType", eventType, "context", context));
    }

    private void markRead(Long notificationId) {
        ApiResponse<?> resp = rpc(mutation, "ErpSysNotification__markRead",
                Map.of("notificationId", notificationId));
        assertEquals(0, resp.getStatus(), "markRead 应成功: " + resp);
    }

    private int countUnread(String userId) {
        ApiResponse<?> resp = rpc(query, "ErpSysNotification__countUnread", Map.of("userId", userId));
        assertEquals(0, resp.getStatus(), "countUnread 应成功: " + resp);
        Object data = resp.getData();
        if (data == null) return 0;
        return ((Number) data).intValue();
    }

    @SuppressWarnings("unchecked")
    private List<ErpSysNotification> findUnread(String userId) {
        ApiResponse<?> resp = rpc(query, "ErpSysNotification__findUnread",
                Map.of("userId", userId));
        assertEquals(0, resp.getStatus(), "findUnread 应成功: " + resp);
        return (List<ErpSysNotification>) resp.getData();
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action, Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpSysNotification findNotification(String userId, String eventType) {
        List<ErpSysNotification> list = notificationsOf(userId, eventType);
        return list.isEmpty() ? null : list.get(0);
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
