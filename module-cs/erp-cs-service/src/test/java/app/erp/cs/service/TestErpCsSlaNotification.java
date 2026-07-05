package app.erp.cs.service;

import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.md.dao.entity.ErpMdPartner;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 1 行为测试（plan 2026-07-06-0642-1）：CS SLA 超期/预警通知消费者。
 *
 * <p>覆盖：scanOverdueTickets 升级时 + findSlaWarnings 预警时调
 * {@code IErpSysNotificationBiz.notify("cs.sla-overdue", ctx)}；断言：
 * <ul>
 *   <li>notify 被调 + ErpSysNotification 行落入（recipient 匹配 USER_LIST 模板的接收人）</li>
 *   <li>config 关闭（erp-cs.sla-notify-enabled=false）时静默跳过，无新通知行</li>
 * </ul>
 *
 * <p>模板使用 USER_LIST resolver（直接指定接收人 userId），避免依赖角色基础设施
 * （与 {@code TestErpSysNotificationDispatch} 同范式）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsSlaNotification extends JunitAutoTestCase {

    static final Long CUSTOMER_ID = 5101L;
    static final Long TICKET_TYPE_ID = 6101L;
    static final String RECIPIENT = "cs-sla-recipient";
    static final String NOTIFY_EVENT = ErpCsConstants.NOTIFY_EVENT_SLA_OVERDUE;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testScanOverdueTriggersNotify() {
        seedCustomer(CUSTOMER_ID, "ACME Corp");
        seedSlaNotifyTemplate(7001L, RECIPIENT);
        Long ticketId = seedTicket("TK-NOTIFY-OVERDUE", ErpCsConstants.TICKET_STATUS_ASSIGNED,
                LocalDateTime.now().minusHours(2));
        int before = countNotifications(NOTIFY_EVENT);

        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__scanOverdueTickets", new java.util.HashMap<>());
        assertEquals(0, resp.getStatus(), "scanOverdueTickets 应成功: " + resp);

        int after = countNotifications(NOTIFY_EVENT);
        assertTrue(after > before, "SLA 超时升级应派发 cs.sla-overdue 通知");
        ErpSysNotification n = findNotification(NOTIFY_EVENT);
        assertNotNull(n, "应存在 cs.sla-overdue ErpSysNotification 行");
        assertEquals(RECIPIENT, n.getRecipientUserId(), "接收人应匹配模板 USER_LIST");
    }

    @Test
    public void testFindSlaWarningsTriggersNotify() {
        seedCustomer(CUSTOMER_ID, "ACME Corp");
        seedSlaNotifyTemplate(7002L, RECIPIENT);
        // deadline 在 now 到 now+60min 之间 → 命中预警
        seedTicket("TK-NOTIFY-WARN", ErpCsConstants.TICKET_STATUS_ASSIGNED,
                LocalDateTime.now().plusMinutes(30));
        int before = countNotifications(NOTIFY_EVENT);

        ApiResponse<?> resp = rpc(query, "ErpCsTicket__findSlaWarnings",
                Map.of("beforeMinutes", 60));
        assertEquals(0, resp.getStatus(), "findSlaWarnings 应成功: " + resp);

        int after = countNotifications(NOTIFY_EVENT);
        assertTrue(after > before, "SLA 预警窗口应派发 cs.sla-overdue 通知");
    }

    @Test
    public void testNotifyDisabledSkipsDispatch() {
        // 关闭通知派发开关
        AppConfig.getConfigProvider().assignConfigValue(
                ErpCsConstants.CONFIG_SLA_NOTIFY_ENABLED, "false");
        try {
            seedCustomer(CUSTOMER_ID, "ACME Corp");
            seedSlaNotifyTemplate(7003L, RECIPIENT);
            seedTicket("TK-NOTIFY-DISABLED", ErpCsConstants.TICKET_STATUS_ASSIGNED,
                    LocalDateTime.now().minusHours(2));
            int before = countNotifications(NOTIFY_EVENT);

            ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__scanOverdueTickets", new java.util.HashMap<>());
            assertEquals(0, resp.getStatus(), "scanOverdueTickets 应成功: " + resp);

            int after = countNotifications(NOTIFY_EVENT);
            assertEquals(before, after, "config 关闭时应静默跳过 notify 派发");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpCsConstants.CONFIG_SLA_NOTIFY_ENABLED, "true");
        }
    }

    // ---------- helpers ----------

    private int countNotifications(String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", eventType));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q).size();
    }

    private ErpSysNotification findNotification(String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", eventType));
        q.addOrderField("createTime", true);
        q.setLimit(1);
        List<ErpSysNotification> list = daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
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

    private void seedSlaNotifyTemplate(Long id, String recipientUserId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", id);
            t.setNotificationType(NOTIFY_EVENT);
            t.setName("SLA超期预警");
            t.setChannelSet("IN_APP");
            t.setSubjectTpl("SLA超期预警: ${customerName}");
            t.setBodyTpl("客户 ${customerName} 工单 ${ticketCode} 已超 SLA，请跟进");
            t.setRecipientResolver("USER_LIST");
            t.setRecipientConfig("{\"userIds\":[\"" + recipientUserId + "\"]}");
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy("NONE");
            t.setStatus("ACTIVE");
            dao.saveEntity(t);
        });
    }

    private Long seedTicket(String code, String status, LocalDateTime deadline) {
        Long id = 7000L + (long) (Math.abs(code.hashCode()) % 1000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
            ErpCsTicket t = new ErpCsTicket();
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
            if (deadline != null) {
                t.setDeadlineDateTime(deadline);
            }
            dao.saveEntity(t);
        });
        return id;
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                               Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }
}
