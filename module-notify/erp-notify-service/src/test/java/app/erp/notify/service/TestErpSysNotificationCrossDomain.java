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
public class TestErpSysNotificationCrossDomain extends JunitAutoTestCase {

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testPostingAlertNotification() {
        String user = "cross-test-user-1";
        seedTemplate(7201L, "posting-alert", "过账异常: 凭证 ${voucherCode}",
                "凭证 ${voucherCode} 过账失败: ${errorMsg}",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + user + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_NONE, 0);

        ApiResponse<?> resp = notify("posting-alert",
                Map.of("voucherCode", "V-001", "errorMsg", "余额不平"));
        assertEquals(0, resp.getStatus());

        ErpSysNotification n = findNotification(user, "posting-alert");
        assertNotNull(n);
        assertEquals("过账异常: 凭证 V-001", n.getSubject());
        assertTrue(n.getBody().contains("V-001"));
        assertTrue(n.getBody().contains("余额不平"));
        assertNotNull(n.getPayloadJson());
    }

    @Test
    public void testSlaOverdueNotification() {
        String user = "cross-test-user-2";
        seedTemplate(7202L, "sla-overdue", "SLA超期预警: ${customerName} 工单 ${ticketCode}",
                "客户 ${customerName} 的工单 ${ticketCode} 已超期，请处理",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + user + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_NONE, 0);

        ApiResponse<?> resp = notify("sla-overdue",
                Map.of("customerName", "ACME Corp", "ticketCode", "T-200"));
        assertEquals(0, resp.getStatus());

        ErpSysNotification n = findNotification(user, "sla-overdue");
        assertNotNull(n);
        assertEquals("SLA超期预警: ACME Corp 工单 T-200", n.getSubject());
        assertTrue(n.getBody().contains("ACME Corp"));
        assertTrue(n.getBody().contains("T-200"));
    }

    @Test
    public void testMultipleEventTypesIndependent() {
        String user = "cross-test-user-3";
        seedTemplate(7203L, "cross-event-a", "事件A通知", "事件A正文",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + user + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_NONE, 0);
        seedTemplate(7204L, "cross-event-b", "事件B通知", "事件B正文",
                ErpNotifyConstants.RESOLVER_USER_LIST,
                "{\"userIds\":[\"" + user + "\"]}",
                ErpNotifyConstants.CHANNEL_IN_APP, ErpNotifyConstants.MERGE_NONE, 0);

        notify("cross-event-a", Map.of());
        notify("cross-event-b", Map.of());

        List<ErpSysNotification> aList = notificationsOf(user, "cross-event-a");
        List<ErpSysNotification> bList = notificationsOf(user, "cross-event-b");

        assertEquals(1, aList.size());
        assertEquals(1, bList.size());
        assertEquals("事件A通知", aList.get(0).getSubject());
        assertEquals("事件B通知", bList.get(0).getSubject());
        assertNotEquals(aList.get(0).getId(), bList.get(0).getId());
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
