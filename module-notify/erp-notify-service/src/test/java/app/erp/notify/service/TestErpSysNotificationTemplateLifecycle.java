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
import static org.junit.jupiter.api.Assertions.*;

@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSysNotificationTemplateLifecycle extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testActiveTemplateDispatches() {
        String user = "lc-test-user-1";
        seedTemplate(7301L, "lc-event-1", "活跃模板测试", "活跃模板正文",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + user + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_NONE, 0,
                ErpNotifyConstants.TEMPLATE_ACTIVE);

        ApiResponse<?> resp = notify("lc-event-1", Map.of());
        assertEquals(0, resp.getStatus());

        List<ErpSysNotification> list = notificationsOf(user, "lc-event-1");
        assertEquals(1, list.size());
        assertEquals(ErpNotifyConstants.STATUS_SENT, list.get(0).getStatus());
        assertEquals("活跃模板测试", list.get(0).getSubject());
    }

    @Test
    public void testDraftTemplateSkipsDispatch() {
        String user = "lc-test-user-2";
        seedTemplate(7302L, "lc-event-2", "草稿模板测试", "草稿模板正文",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + user + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_NONE, 0,
                ErpNotifyConstants.TEMPLATE_DRAFT);

        ApiResponse<?> resp = notify("lc-event-2", Map.of());
        assertEquals(0, resp.getStatus());

        List<ErpSysNotification> list = notificationsOf(user, "lc-event-2");
        assertTrue(list.isEmpty());
    }

    @Test
    public void testTemplateSubjectRendering() {
        String user = "lc-test-user-3";
        seedTemplate(7303L, "lc-event-3", "订单 ${orderNo} 已确认",
                "客户 ${customerName} 的订单 ${orderNo} 已确认，金额 ${amount}",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + user + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_NONE, 0,
                ErpNotifyConstants.TEMPLATE_ACTIVE);

        ApiResponse<?> resp = notify("lc-event-3",
                Map.of("orderNo", "ORD-001", "customerName", "ACME", "amount", "1000"));
        assertEquals(0, resp.getStatus());

        ErpSysNotification n = findNotification(user, "lc-event-3");
        assertNotNull(n);
        assertEquals("订单 ORD-001 已确认", n.getSubject());
        assertEquals("客户 ACME 的订单 ORD-001 已确认，金额 1000", n.getBody());
    }

    private ApiResponse<?> notify(String eventType, Map<String, Object> context) {
        return executeRpc(mutation, "ErpSysNotification__notify",
                ApiRequest.build(Map.of("eventType", eventType, "context", context)));
    }

    private ErpSysNotification findNotification(String userId, String eventType) {
        List<ErpSysNotification> list = notificationsOf(userId, eventType);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpSysNotification> notificationsOf(String userId, String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        q.addFilter(eq("notificationType", eventType));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }

    private void seedTemplate(Long id, String notificationType, String subjectTpl, String bodyTpl,
                              String resolver, String recipientConfig, String channelSet,
                              String mergeStrategy, int mergeWindowSeconds, String status) {
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
            t.setStatus(status);
            dao.saveEntity(t);
        });
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
