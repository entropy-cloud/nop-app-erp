package app.erp.cs.service;

import app.erp.cs.dao.entity.ErpCsEntitlement;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.md.dao.entity.ErpMdPartner;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 权益 BizModel 集成测试（plan 2026-07-07-1430-1 §Phase 3）。覆盖：
 * <ul>
 *   <li>PAY_PER_TICKET 扣减 usedTickets+1；超限抛 ERR_ENTITLEMENT_EXHAUSTED。</li>
 *   <li>WARRANTY/SUPPORT_CONTRACT 扣减不增计（不限余量）。</li>
 *   <li>releaseEntitlement 退款回退 usedTickets-1，不低于 0。</li>
 *   <li>scanExpiringEntitlements 窗口查询。</li>
 *   <li>deactivateExpiredEntitlements 自动停用到期权益。</li>
 *   <li>getEntitlementUsage 按客户聚合使用率。</li>
 *   <li>工单 matchAndAttachSla 触发权益匹配 + 扣减（config-gated）；无权益放行 / 拒绝。</li>
 * </ul>
 *
 * <p>经 H2 + IGraphQLEngine 直接调 BizModel 方法（GraphQL 引擎自动开启 OrmSession + 事务）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsEntitlement extends JunitAutoTestCase {

    static final Long PARTNER_ID = 9101L;
    static final Long TICKET_TYPE_ID = 6201L;
    static final Long SLA_POLICY_ID = 7201L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testConsumePayPerTicketIncrementsUsed() {
        seedCustomer(PARTNER_ID, "ACME");
        Long id = seedEntitlement(8001L, PARTNER_ID, ErpCsConstants.SERVICE_TYPE_PAY_PER_TICKET,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30), 5, 2);

        ApiResponse<?> resp = rpc(mutation, "ErpCsEntitlement__consumeEntitlement",
                Map.of("entitlementId", id));
        assertEquals(0, resp.getStatus(), "consumeEntitlement 应成功: " + resp);
        assertEquals(3, loadEntitlement(id).getUsedTickets(), "PAY_PER_TICKET 扣减应 usedTickets+1");
    }

    @Test
    public void testConsumeExhaustedRejected() {
        seedCustomer(PARTNER_ID, "ACME");
        Long id = seedEntitlement(8002L, PARTNER_ID, ErpCsConstants.SERVICE_TYPE_PAY_PER_TICKET,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30), 1, 1);

        ApiResponse<?> resp = rpc(mutation, "ErpCsEntitlement__consumeEntitlement",
                Map.of("entitlementId", id));
        assertEquals(ErpCsErrors.ERR_ENTITLEMENT_EXHAUSTED.getErrorCode(), resp.getCode(),
                "余量耗尽应返回 ERR_ENTITLEMENT_EXHAUSTED");
    }

    @Test
    public void testConsumeWarrantyDoesNotIncrement() {
        seedCustomer(PARTNER_ID, "ACME");
        Long id = seedEntitlement(8003L, PARTNER_ID, ErpCsConstants.SERVICE_TYPE_WARRANTY,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30), null, null);

        ApiResponse<?> resp = rpc(mutation, "ErpCsEntitlement__consumeEntitlement",
                Map.of("entitlementId", id));
        assertEquals(0, resp.getStatus(), "consumeEntitlement WARRANTY 应成功: " + resp);
        ErpCsEntitlement after = loadEntitlement(id);
        assertTrue(after.getUsedTickets() == null || after.getUsedTickets() == 0,
                "WARRANTY 扣减不应增计 usedTickets");
    }

    @Test
    public void testConsumeSupportContractDoesNotIncrement() {
        seedCustomer(PARTNER_ID, "ACME");
        Long id = seedEntitlement(8004L, PARTNER_ID, ErpCsConstants.SERVICE_TYPE_SUPPORT_CONTRACT,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30), 10, 0);

        ApiResponse<?> resp = rpc(mutation, "ErpCsEntitlement__consumeEntitlement",
                Map.of("entitlementId", id));
        assertEquals(0, resp.getStatus(), "consumeEntitlement SUPPORT_CONTRACT 应成功: " + resp);
        assertEquals(0, loadEntitlement(id).getUsedTickets(), "SUPPORT_CONTRACT 扣减不应增计 usedTickets");
    }

    @Test
    public void testConsumeExpiredRejected() {
        seedCustomer(PARTNER_ID, "ACME");
        Long id = seedEntitlement(8005L, PARTNER_ID, ErpCsConstants.SERVICE_TYPE_PAY_PER_TICKET,
                LocalDate.now().minusDays(30), LocalDate.now().minusDays(1), 5, 0);

        ApiResponse<?> resp = rpc(mutation, "ErpCsEntitlement__consumeEntitlement",
                Map.of("entitlementId", id));
        assertEquals(ErpCsErrors.ERR_ENTITLEMENT_EXPIRED.getErrorCode(), resp.getCode(),
                "已过期权益扣减应返回 ERR_ENTITLEMENT_EXPIRED");
    }

    @Test
    public void testReleaseDecrementsNotBelowZero() {
        seedCustomer(PARTNER_ID, "ACME");
        Long id = seedEntitlement(8006L, PARTNER_ID, ErpCsConstants.SERVICE_TYPE_PAY_PER_TICKET,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30), 5, 3);

        rpc(mutation, "ErpCsEntitlement__releaseEntitlement", Map.of("entitlementId", id));
        assertEquals(2, loadEntitlement(id).getUsedTickets(), "退款应 usedTickets-1");

        rpc(mutation, "ErpCsEntitlement__releaseEntitlement", Map.of("entitlementId", id));
        rpc(mutation, "ErpCsEntitlement__releaseEntitlement", Map.of("entitlementId", id));
        rpc(mutation, "ErpCsEntitlement__releaseEntitlement", Map.of("entitlementId", id));
        assertEquals(0, loadEntitlement(id).getUsedTickets(), "usedTickets 不应降至负，最小为 0");
    }

    @Test
    public void testScanExpiringEntitlements() {
        seedCustomer(PARTNER_ID, "ACME");
        seedEntitlement(8010L, PARTNER_ID, ErpCsConstants.SERVICE_TYPE_WARRANTY,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(10), null, null);
        seedEntitlement(8011L, PARTNER_ID, ErpCsConstants.SERVICE_TYPE_WARRANTY,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(100), null, null);

        ApiResponse<?> resp = rpc(query, "ErpCsEntitlement__scanExpiringEntitlements",
                Map.of("warningDays", 30));
        assertEquals(0, resp.getStatus(), "scanExpiringEntitlements 应成功: " + resp);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) resp.getData();
        boolean foundInWindow = false;
        boolean foundOutside = false;
        for (Map<String, Object> row : items) {
            Long id = toLong(row.get("id"));
            if (Long.valueOf(8010L).equals(id)) foundInWindow = true;
            if (Long.valueOf(8011L).equals(id)) foundOutside = true;
        }
        assertTrue(foundInWindow, "窗口内（10 天后到期）应在结果中");
        assertFalse(foundOutside, "窗口外（100 天后到期）不应在结果中");
    }

    @Test
    public void testDeactivateExpiredEntitlements() {
        seedCustomer(PARTNER_ID, "ACME");
        seedEntitlement(8020L, PARTNER_ID, ErpCsConstants.SERVICE_TYPE_WARRANTY,
                LocalDate.now().minusDays(30), LocalDate.now().minusDays(1), null, null);
        seedEntitlement(8021L, PARTNER_ID, ErpCsConstants.SERVICE_TYPE_WARRANTY,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30), null, null);

        ApiResponse<?> resp = rpc(mutation, "ErpCsEntitlement__deactivateExpiredEntitlements",
                new java.util.HashMap<>());
        assertEquals(0, resp.getStatus(), "deactivateExpiredEntitlements 应成功: " + resp);

        assertFalse(loadEntitlement(8020L).getIsActive(), "已到期权益应 isActive=false");
        assertTrue(loadEntitlement(8021L).getIsActive(), "未到期权益应保持 isActive=true");
    }

    @Test
    public void testGetEntitlementUsage() {
        seedCustomer(PARTNER_ID, "ACME");
        seedEntitlement(8030L, PARTNER_ID, ErpCsConstants.SERVICE_TYPE_PAY_PER_TICKET,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30), 10, 4);
        seedEntitlement(8031L, PARTNER_ID, ErpCsConstants.SERVICE_TYPE_PAY_PER_TICKET,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30), 10, 6);

        ApiResponse<?> resp = rpc(query, "ErpCsEntitlement__getEntitlementUsage",
                Map.of("partnerId", PARTNER_ID));
        assertEquals(0, resp.getStatus(), "getEntitlementUsage 应成功: " + resp);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) resp.getData();
        assertEquals(1, items.size(), "单客户聚合为一条");
        Map<String, Object> row = items.get(0);
        assertEquals(2, toInt(row.get("totalEntitlements")));
        assertEquals(10, toInt(row.get("totalUsed")));
        assertEquals(20, toInt(row.get("totalMax")));
        assertEquals(50.0, ((Number) row.get("usageRate")).doubleValue(), "使用率应为 50%");
    }

    @Test
    public void testTicketMatchAndAttachSlaConsumesEntitlement() {
        seedCustomer(PARTNER_ID, "ACME");
        seedSlaPolicy(SLA_POLICY_ID, TICKET_TYPE_ID);
        Long entitlementId = seedEntitlement(8040L, PARTNER_ID, ErpCsConstants.SERVICE_TYPE_PAY_PER_TICKET,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30), 5, 1);
        Long ticketId = seedTicket("TK-ENT-CONSUME", PARTNER_ID);

        // matchAndAttachSla 应触发权益匹配 + 扣减（config-gated 默认开启）
        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__matchAndAttachSla",
                Map.of("ticketId", ticketId));
        assertEquals(0, resp.getStatus(), "matchAndAttachSla 应成功: " + resp);

        assertEquals(2, loadEntitlement(entitlementId).getUsedTickets(),
                "工单建单权益集成应触发 usedTickets+1");
    }

    @Test
    public void testNoEntitlementAllowedByDefault() {
        seedCustomer(PARTNER_ID, "ACME");
        Long ticketId = seedTicket("TK-NO-ENT-ALLOW", PARTNER_ID);

        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__matchAndAttachSla",
                Map.of("ticketId", ticketId));
        assertEquals(0, resp.getStatus(),
                "默认 allow-no-entitlement=true 时无权益应放行建单: " + resp);
    }

    @Test
    public void testNoEntitlementRejectedWhenDisallow() {
        seedCustomer(PARTNER_ID, "ACME");
        Long ticketId = seedTicket("TK-NO-ENT-REJECT", PARTNER_ID);

        AppConfig.getConfigProvider().assignConfigValue(
                ErpCsConstants.CONFIG_ENTITLEMENT_ALLOW_NO_ENTITLEMENT, "false");
        try {
            ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__matchAndAttachSla",
                    Map.of("ticketId", ticketId));
            assertEquals(ErpCsErrors.ERR_ENTITLEMENT_NONE_ACTIVE.getErrorCode(), resp.getCode(),
                    "无权益且 allow=false 时应返回 ERR_ENTITLEMENT_NONE_ACTIVE");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpCsConstants.CONFIG_ENTITLEMENT_ALLOW_NO_ENTITLEMENT, "true");
        }
    }

    @Test
    public void testEntitlementCheckDisabledSkipsConsume() {
        seedCustomer(PARTNER_ID, "ACME");
        Long entitlementId = seedEntitlement(8050L, PARTNER_ID, ErpCsConstants.SERVICE_TYPE_PAY_PER_TICKET,
                LocalDate.now().minusDays(5), LocalDate.now().plusDays(30), 5, 1);
        Long ticketId = seedTicket("TK-ENT-DISABLED", PARTNER_ID);

        AppConfig.getConfigProvider().assignConfigValue(
                ErpCsConstants.CONFIG_ENTITLEMENT_CHECK_ENABLED, "false");
        try {
            ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__matchAndAttachSla",
                    Map.of("ticketId", ticketId));
            assertEquals(0, resp.getStatus(), "config 关闭时应跳过权益扣减: " + resp);

            assertEquals(1, loadEntitlement(entitlementId).getUsedTickets(),
                    "config 关闭时 usedTickets 不应变化");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpCsConstants.CONFIG_ENTITLEMENT_CHECK_ENABLED, "true");
        }
    }

    // ---------- helpers ----------

    private ErpCsEntitlement loadEntitlement(Long id) {
        return daoProvider.daoFor(ErpCsEntitlement.class).getEntityById(id);
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

    private Long seedEntitlement(Long id, Long partnerId, String serviceType,
                                  LocalDate start, LocalDate end,
                                  Integer maxTickets, Integer usedTickets) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsEntitlement> dao = daoProvider.daoFor(ErpCsEntitlement.class);
            ErpCsEntitlement e = new ErpCsEntitlement();
            e.orm_propValueByName("id", id);
            e.setCode("ENT-" + id);
            e.setPartnerId(partnerId);
            e.setServiceType(serviceType);
            e.setStartDate(start);
            e.setEndDate(end);
            e.setMaxTickets(maxTickets);
            e.setUsedTickets(usedTickets);
            e.setIsActive(Boolean.TRUE);
            e.setSlaPolicyId(SLA_POLICY_ID);
            dao.saveEntity(e);
        });
        return id;
    }

    private void seedSlaPolicy(Long id, Long ticketTypeId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<app.erp.cs.dao.entity.ErpCsSlaPolicy> dao =
                    daoProvider.daoFor(app.erp.cs.dao.entity.ErpCsSlaPolicy.class);
            app.erp.cs.dao.entity.ErpCsSlaPolicy p = new app.erp.cs.dao.entity.ErpCsSlaPolicy();
            p.orm_propValueByName("id", id);
            p.setCode("SLA-" + id);
            p.setName("测试 SLA");
            p.setTicketTypeId(ticketTypeId);
            p.setResolveHours(48);
            p.setIsWorkingDays(false);
            dao.saveEntity(p);
        });
    }

    private Long seedTicket(String code, Long customerId) {
        Long id = 8000L + (long) (Math.abs(code.hashCode()) % 1000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
            ErpCsTicket t = new ErpCsTicket();
            t.orm_propValueByName("id", id);
            t.setCode(code);
            t.setSubject("工单-" + code);
            t.setCustomerId(customerId);
            t.setTicketTypeId(TICKET_TYPE_ID);
            t.setPriority(ErpCsConstants.TICKET_PRIORITY_NORMAL);
            t.setStatus(ErpCsConstants.TICKET_STATUS_NEW);
            t.setDocStatus(ErpCsConstants.DOC_STATUS_ACTIVE);
            t.setApproveStatus(ErpCsConstants.APPROVE_STATUS_UNSUBMITTED);
            t.setIsSlaCompleted(false);
            dao.saveEntity(t);
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

    private static int toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number) return ((Number) v).intValue();
        return Integer.valueOf(String.valueOf(v));
    }
}
