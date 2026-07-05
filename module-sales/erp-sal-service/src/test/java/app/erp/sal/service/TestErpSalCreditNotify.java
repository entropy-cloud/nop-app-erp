package app.erp.sal.service;

import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.dao.entity.ErpSalOrderLine;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Phase 1 行为测试（plan 2026-07-06-0642-1）：销售信用额度超限通知消费者。
 *
 * <p>覆盖：SOFT_WARNING 级别下客户信用超限放行后调
 * {@code IErpSysNotificationBiz.notify("sal.credit-over-limit", ctx)}；断言：
 * <ul>
 *   <li>notify 被调 + ErpSysNotification 行落入（recipient 匹配 USER_LIST 模板的接收人）</li>
 *   <li>config 关闭（erp-sal.credit-notify-enabled=false）时静默跳过，无新通知行</li>
 *   <li>HARD_BLOCK 路径（拒绝抛错）不派发通知</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpSalCreditNotify extends JunitAutoTestCase {

    static final Long ORG_ID = 1301L;
    static final Long CUSTOMER_ID = 2311L;
    static final Long WAREHOUSE_ID = 3311L;
    static final Long MATERIAL_ID = 4311L;
    static final Long UOM_ID = 5311L;
    static final Long CURRENCY_ID = 6311L;
    static final String NOTIFY_EVENT = ErpSalConstants.NOTIFY_EVENT_CREDIT_OVER_LIMIT;
    static final String RECIPIENT = "sal-credit-recipient";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testSoftWarningOverLimitTriggersNotify() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_SOFT_WARNING);
        seedNotifyTemplate(7103L, RECIPIENT);
        ErpSalOrder order = newOrder("SO-CREDIT-NOTIFY-001", "150");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveOrderWithLine(order, "10");
        });
        int before = countNotifications(NOTIFY_EVENT);

        assertEquals(0, submit(order.getId()).getStatus());
        assertEquals(0, approve(order.getId()).getStatus(), "SOFT_WARNING 超额度应放行");

        int after = countNotifications(NOTIFY_EVENT);
        assertEquals(before + 1, after, "SOFT_WARNING 超额度应派发 sal.credit-over-limit 通知");
        ErpSysNotification n = findNotification(NOTIFY_EVENT);
        assertNotNull(n, "应存在 sal.credit-over-limit ErpSysNotification 行");
        assertEquals(RECIPIENT, n.getRecipientUserId(), "接收人应匹配模板 USER_LIST");
    }

    @Test
    public void testNotifyDisabledSkipsDispatch() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_SOFT_WARNING);
        AppConfig.getConfigProvider().assignConfigValue(
                ErpSalConstants.CONFIG_CREDIT_NOTIFY_ENABLED, "false");
        try {
            seedNotifyTemplate(7113L, RECIPIENT);
            ErpSalOrder order = newOrder("SO-CREDIT-NOTIFY-OFF-001", "150");
            ormTemplate.runInSession(() -> {
                seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
                saveOrderWithLine(order, "10");
            });
            int before = countNotifications(NOTIFY_EVENT);

            assertEquals(0, submit(order.getId()).getStatus());
            assertEquals(0, approve(order.getId()).getStatus(), "SOFT_WARNING 超额度应放行");

            int after = countNotifications(NOTIFY_EVENT);
            assertEquals(before, after, "config 关闭时应静默跳过 notify 派发");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpSalConstants.CONFIG_CREDIT_NOTIFY_ENABLED, "true");
        }
    }

    @Test
    public void testHardBlockRejectsNoNotify() {
        setCreditCheckLevel(ErpSalConstants.CREDIT_CHECK_LEVEL_HARD_BLOCK);
        seedNotifyTemplate(7123L, RECIPIENT);
        ErpSalOrder order = newOrder("SO-CREDIT-NOTIFY-HARD-001", "150");
        ormTemplate.runInSession(() -> {
            seedActiveCustomer(CUSTOMER_ID, new BigDecimal("100"));
            saveOrderWithLine(order, "10");
        });
        int before = countNotifications(NOTIFY_EVENT);

        assertEquals(0, submit(order.getId()).getStatus());
        ApiResponse<?> bad = approve(order.getId());
        assertEquals(ErpSalErrors.ERR_CREDIT_LIMIT_EXCEEDED.getErrorCode(), bad.getCode(),
                "HARD_BLOCK 超额度应拒绝");

        int after = countNotifications(NOTIFY_EVENT);
        assertEquals(before, after, "HARD_BLOCK 拒绝路径不派发通知");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> submit(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__submitForApproval",
                ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> approve(Long orderId) {
        return executeRpc(mutation, "ErpSalOrder__approve",
                ApiRequest.build(Map.of("id", String.valueOf(orderId))));
    }

    private ApiResponse<?> executeRpc(io.nop.graphql.core.ast.GraphQLOperationType opType, String action,
                                      ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
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

    private void seedNotifyTemplate(Long id, String recipientUserId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", id);
            t.setNotificationType(NOTIFY_EVENT);
            t.setName("信用超额度通知");
            t.setChannelSet("IN_APP");
            t.setSubjectTpl("信用超额度通知: ${customerName}");
            t.setBodyTpl("客户 ${customerName} 信用额度超限 ${overAmount}，订单 ${orderNo} 已挂起");
            t.setRecipientResolver("USER_LIST");
            t.setRecipientConfig("{\"userIds\":[\"" + recipientUserId + "\"]}");
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy("NONE");
            t.setStatus("ACTIVE");
            dao.saveEntity(t);
        });
    }

    private ErpSalOrder newOrder(String code, String totalAmountWithTax) {
        ErpSalOrder order = new ErpSalOrder();
        order.setCode(code);
        order.setOrgId(ORG_ID);
        order.setCustomerId(CUSTOMER_ID);
        order.setWarehouseId(WAREHOUSE_ID);
        order.setBusinessDate(LocalDate.of(2026, 7, 1));
        order.setCurrencyId(CURRENCY_ID);
        order.setExchangeRate(new BigDecimal("1"));
        order.setTotalAmountWithTax(new BigDecimal(totalAmountWithTax));
        order.setTotalAmount(new BigDecimal(totalAmountWithTax));
        order.setDocStatus(ErpSalConstants.DOC_STATUS_DRAFT);
        order.setApproveStatus(ErpSalConstants.APPROVE_STATUS_UNSUBMITTED);
        order.setDeliveryStatus(ErpSalConstants.DELIVERY_STATUS_UNDELIVERED);
        order.setPosted(false);
        return order;
    }

    private void saveOrderWithLine(ErpSalOrder order, String quantity) {
        daoProvider.daoFor(ErpSalOrder.class).saveEntity(order);
        IEntityDao<ErpSalOrderLine> lineDao = daoProvider.daoFor(ErpSalOrderLine.class);
        ErpSalOrderLine line = new ErpSalOrderLine();
        line.setOrderId(order.getId());
        line.setLineNo(1);
        line.setMaterialId(MATERIAL_ID);
        line.setUoMId(UOM_ID);
        line.setQuantity(new BigDecimal(quantity));
        line.setUnitPrice(new BigDecimal("10"));
        line.setAmount(new BigDecimal("100"));
        lineDao.saveEntity(line);
    }

    private void seedActiveCustomer(Long id, BigDecimal creditLimit) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner partner = new ErpMdPartner();
        partner.setId(id);
        partner.setCode("CUS-" + id);
        partner.setName("客户" + id);
        partner.setPartnerType("CUSTOMER");
        partner.setStatus(ErpSalConstants.PARTNER_STATUS_ACTIVE);
        partner.setCreditLimit(creditLimit);
        dao.saveEntity(partner);
    }

    private void setCreditCheckLevel(String level) {
        AppConfig.getConfigProvider()
                .assignConfigValue(ErpSalConstants.CONFIG_CREDIT_CHECK_LEVEL, level);
    }
}
