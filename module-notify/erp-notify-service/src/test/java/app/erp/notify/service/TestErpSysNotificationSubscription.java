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
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.*;

@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSysNotificationSubscription extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testMarkAllReadClearsUnread() {
        String user = "sub-test-user-1";
        seedTemplate(7101L, "sub-event-1", "订阅测试事件1", "正文1",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + user + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_NONE, 0);

        notify("sub-event-1", Map.of());
        notify("sub-event-1", Map.of());
        notify("sub-event-1", Map.of());

        assertEquals(3, countUnread(user));

        ApiResponse<?> resp = executeRpc(mutation, "ErpSysNotification__markAllRead",
                ApiRequest.build(Map.of("userId", user)));
        assertEquals(0, resp.getStatus());

        assertEquals(0, countUnread(user));
    }

    @Test
    public void testFindUnreadReturnsOnlyUnread() {
        String user = "sub-test-user-2";
        seedTemplate(7102L, "sub-event-2", "订阅测试事件2", "正文2",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + user + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_NONE, 0);

        notify("sub-event-2", Map.of());
        notify("sub-event-2", Map.of());

        List<ErpSysNotification> all = notificationsOf(user, "sub-event-2");
        assertEquals(2, all.size());

        Long firstId = all.get(0).getId();
        ApiResponse<?> resp = executeRpc(mutation, "ErpSysNotification__markRead",
                ApiRequest.build(Map.of("notificationId", firstId)));
        assertEquals(0, resp.getStatus());

        assertEquals(1, countUnread(user));
        List<ErpSysNotification> remaining = notificationsOf(user, "sub-event-2");
        assertEquals(2, remaining.size(), "DB 仍有 2 条记录");
        assertEquals(all.get(1).getId(), remaining.get(1).getId(), "第二条未被标记已读");
    }

    @Test
    public void testCountUnreadReturnsCorrectCount() {
        String user = "sub-test-user-1";
        seedTemplate(7103L, "sub-event-3", "订阅测试事件3", "正文3",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + user + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_NONE, 0);

        notify("sub-event-3", Map.of());
        notify("sub-event-3", Map.of());
        notify("sub-event-3", Map.of());

        assertEquals(3, countUnread(user));
    }

    private ApiResponse<?> notify(String eventType, Map<String, Object> context) {
        return executeRpc(mutation, "ErpSysNotification__notify",
                ApiRequest.build(Map.of("eventType", eventType, "context", context)));
    }

    private int countUnread(String userId) {
        ApiResponse<?> resp = executeRpc(query, "ErpSysNotification__countUnread",
                ApiRequest.build(Map.of("userId", userId)));
        assertEquals(0, resp.getStatus());
        Object data = resp.getData();
        if (data == null) return 0;
        return ((Number) data).intValue();
    }

    @SuppressWarnings("unchecked")
    private List<ErpSysNotification> findUnread(String userId) {
        ApiResponse<?> resp = executeRpc(query, "ErpSysNotification__findUnread",
                ApiRequest.build(Map.of("userId", userId)));
        assertEquals(0, resp.getStatus());
        return (List<ErpSysNotification>) resp.getData();
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

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
