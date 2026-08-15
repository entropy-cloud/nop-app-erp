package app.erp.ct.service;

import app.erp.contract.dao.entity.ErpCtApprovalMatrix;
import app.erp.contract.dao.entity.ErpCtApprovalRecord;
import app.erp.contract.dao.entity.ErpCtContract;
import app.erp.md.dao.entity.ErpMdCurrency;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.notify.service.ErpNotifyConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 72h 审批超时升级 Job 测试（RC-R1.34，P1-RC-077 ④，UC-CT-07 异常）。
 *
 * <p>覆盖：超时 PENDING 记录 → 升级通知上一节点审批人（recipient 断言）；未超时零动作；
 * cron 空值跳过；单条异常记录（合同缺失）跳过不阻断正常记录（失败隔离）。
 *
 * <p>手工装配 Job bean（镜像 TestErpHrLeaveApproverTimeoutJob.newWiredJob 范式——
 * biz_* 代理 bean 的 lazy props 在测试容器按需创建时不赋值）。updateTime 以
 * {@code orm_disableAutoStamp(true)} + 显式赋值 seed 旧时点（ORM 自动盖章会覆盖为 now）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtApprovalTimeoutJob extends JunitAutoTestCase {

    static final String JOB_ENABLED_KEY = "nop.job.erp-ct-approval-timeout.enabled";
    static final String PREV_APPROVER = "ct-prev-approver";
    static final String OWNER_USER = "ct-owner-user";

    @RegisterExtension
    static CtFrozenClockExtension frozenClock = new CtFrozenClockExtension();

    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    app.erp.ct.biz.IErpCtApprovalRecordBiz approvalRecordBiz;
    @Inject
    app.erp.ct.biz.IErpCtContractBiz contractBiz;
    @Inject
    app.erp.ct.service.approval.ErpCtApprovalWorkflowEngine approvalEngine;
    @Inject
    app.erp.notify.biz.IErpSysNotificationBiz notificationBiz;

    @AfterEach
    public void resetConfig() {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_APPROVAL_TIMEOUT_CRON, "");
        AppConfig.getConfigProvider().assignConfigValue(JOB_ENABLED_KEY, "true");
    }

    private app.erp.ct.service.job.ErpCtApprovalTimeoutEscalationJob newWiredJob() {
        app.erp.ct.service.job.ErpCtApprovalTimeoutEscalationJob job =
                new app.erp.ct.service.job.ErpCtApprovalTimeoutEscalationJob();
        job.setApprovalRecordBiz(approvalRecordBiz);
        job.setContractBiz(contractBiz);
        job.setApprovalEngine(approvalEngine);
        job.setNotificationBiz(notificationBiz);
        job.setOrmTemplate(ormTemplate);
        return job;
    }

    // ---------- ① 超时记录 → 升级通知上一节点审批人 ----------

    @Test
    public void testTimeoutRecordEscalatesToPrevApproverAndNotifies() {
        seedTemplate(8821L, "{\"userIds\":[\"${escalationUserId}\"]}");
        long contractId = createContractWithOwner();
        seedMatrix(1, "CT-ROLE-P");
        seedMatrix(2, "CT-ROLE-P");
        // node1 APPROVED（上一节点，approver=PREV_APPROVER）
        seedRecord(contractId, 1, true, ErpCtConstants.APPROVAL_STATUS_APPROVED, PREV_APPROVER, oldTs());
        // node2 PENDING（超时）
        seedRecord(contractId, 2, true, ErpCtConstants.APPROVAL_STATUS_PENDING, "ct-pending-user", oldTs());
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        List<ErpSysNotification> notifications = notificationsOf(PREV_APPROVER);
        assertEquals(1, notifications.size(), "应派发 1 条升级通知给上一节点审批人");
        ErpSysNotification n = notifications.get(0);
        assertEquals(ErpCtConstants.NOTIFY_EVENT_APPROVAL_TIMEOUT_ESCALATION, n.getNotificationType());
        assertEquals(PREV_APPROVER, n.getRecipientUserId());
        assertEquals(ErpNotifyConstants.STATUS_SENT, n.getStatus());
    }

    // ---------- ② 未超时 → 零动作 ----------

    @Test
    public void testNotTimeoutRecordUntouched() {
        seedTemplate(8822L, "{\"userIds\":[\"${escalationUserId}\"]}");
        long contractId = createContractWithOwner();
        seedMatrix(1, "CT-ROLE-Q");
        seedRecord(contractId, 1, true, ErpCtConstants.APPROVAL_STATUS_PENDING, "ct-pending-user", recentTs());
        setCron("0 0 1 * * ?");

        newWiredJob().execute();

        assertTrue(notificationsOf(PREV_APPROVER).isEmpty(), "未超时不应派发通知");
        ErpCtApprovalRecord record = daoProvider.daoFor(ErpCtApprovalRecord.class)
                .findFirstByQuery(eqQuery("contractId", contractId));
        assertEquals(ErpCtConstants.APPROVAL_STATUS_PENDING, record.getApprovalStatus(), "未超时记录状态不变");
    }

    // ---------- ③ 单条异常（合同缺失）跳过不阻断 ----------

    @Test
    public void testMissingContractSkippedIsolation() {
        seedTemplate(8823L, "{\"userIds\":[\"${escalationUserId}\"]}");
        long contractId = createContractWithOwner();
        seedMatrix(1, "CT-ROLE-R");
        // 超时记录 + 合同随后删除 → resolveEscalationUserId 无接收人 → 跳过（LOG.warn）
        seedRecord(contractId, 1, true, ErpCtConstants.APPROVAL_STATUS_PENDING, "ct-pending-user", oldTs());
        ormTemplate.runInSession(session -> {
            daoProvider.daoFor(ErpCtContract.class).deleteEntity(
                    daoProvider.daoFor(ErpCtContract.class).getEntityById(contractId));
            return null;
        });
        setCron("0 0 1 * * ?");

        // 不应抛出（execute 顶层 try/catch + 单条 try/catch 双保险）
        newWiredJob().execute();
        assertTrue(notificationsOf(PREV_APPROVER).isEmpty(), "合同缺失记录应跳过不派发");
    }

    // ---------- ④ cron 空值跳过 ----------

    @Test
    public void testCronEmptySkipsScan() {
        seedTemplate(8824L, "{\"userIds\":[\"${escalationUserId}\"]}");
        long contractId = createContractWithOwner();
        seedMatrix(1, "CT-ROLE-S");
        seedRecord(contractId, 1, true, ErpCtConstants.APPROVAL_STATUS_PENDING, "ct-pending-user", oldTs());
        setCron("");

        newWiredJob().execute();

        assertTrue(notificationsOf(PREV_APPROVER).isEmpty(), "cron 空时不应扫描派发");
    }

    // ---------- ⑤ job 门控 config 绑定断言 ----------

    @Test
    public void testJobEnabledConfigBinding() {
        AppConfig.getConfigProvider().assignConfigValue(JOB_ENABLED_KEY, "false");
        assertEquals("false", AppConfig.var(JOB_ENABLED_KEY, "true"),
                "job.yaml @cfg 引用的 enabled 键应可经 AppConfig 绑定读写");
        AppConfig.getConfigProvider().assignConfigValue(JOB_ENABLED_KEY, "true");
        assertEquals("true", AppConfig.var(JOB_ENABLED_KEY, "true"));
    }

    // ---------- helpers ----------

    private void setCron(String cron) {
        AppConfig.getConfigProvider().assignConfigValue(ErpCtConfigs.CFG_APPROVAL_TIMEOUT_CRON, cron);
    }

    private Timestamp oldTs() {
        return new Timestamp(CoreMetrics.currentTimeMillis() - 100L * 3600_000L);
    }

    private Timestamp recentTs() {
        return new Timestamp(CoreMetrics.currentTimeMillis() - 1L * 3600_000L);
    }

    private long createContractWithOwner() {
        long[] ids = new long[2];
        ormTemplate.runInSession(session -> {
            ErpMdPartner p = daoProvider.daoFor(ErpMdPartner.class).newEntity();
            p.setCode("CT-JOB-PARTNER-" + System.nanoTime());
            p.setName("超时 job 测试伙伴");
            p.setPartnerType("CUSTOMER");
            p.setStatus("ACTIVE");
            daoProvider.daoFor(ErpMdPartner.class).saveEntity(p);
            ErpMdCurrency c = daoProvider.daoFor(ErpMdCurrency.class).newEntity();
            c.setCode("CNY-JOB");
            c.setName("人民币");
            daoProvider.daoFor(ErpMdCurrency.class).saveEntity(c);
            ids[0] = p.getId();
            ids[1] = c.getId();
            return null;
        });
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("code", "CT-JOB-" + System.nanoTime());
        data.put("contractName", "超时 job 测试合同");
        data.put("contractType", "PURCHASE");
        data.put("contractDirection", "INBOUND");
        data.put("partnerId", ids[0]);
        data.put("currencyId", ids[1]);
        data.put("startDate", "2026-01-01");
        data.put("endDate", "2027-12-31");
        data.put("totalAmount", new BigDecimal("1000"));
        data.put("status", "NEGOTIATION");
        ApiResponse<?> resp = executeRpc(mutation, "ErpCtContract__save",
                ApiRequest.build(Map.of("data", data)));
        assertEquals(0, resp.getStatus(), "ErpCtContract__save 应成功: " + resp);
        return toLong(((Map<?, ?>) resp.getData()).get("id"));
    }

    private void seedMatrix(int order, String roleName) {
        ormTemplate.runInSession(() -> {
            ErpCtApprovalMatrix m = daoProvider.daoFor(ErpCtApprovalMatrix.class).newEntity();
            m.setCode("CT-JOB-MTX-" + order + "-" + System.nanoTime());
            m.setMaxAmount(new BigDecimal("2000"));
            m.setApproverRole(roleName);
            m.setApprovalOrder(order);
            m.setIsActive(true);
            daoProvider.daoFor(ErpCtApprovalMatrix.class).saveEntity(m);
        });
    }

    private void seedRecord(long contractId, int order, boolean matrixIdSet, String status,
                            String approverId, Timestamp updateTime) {
        ormTemplate.runInSession(() -> {
            ErpCtApprovalRecord r = daoProvider.daoFor(ErpCtApprovalRecord.class).newEntity();
            r.orm_disableAutoStamp(true);
            r.setCreatedBy("test");
            r.setUpdatedBy("test");
            r.setCreateTime(updateTime);
            r.setUpdateTime(updateTime);
            r.setContractId(contractId);
            r.setApprovalOrder(order);
            r.setApproverId(approverId);
            r.setApprovalStatus(status);
            if (matrixIdSet) {
                r.setApprovalMatrixId(1L);
            }
            daoProvider.daoFor(ErpCtApprovalRecord.class).saveEntity(r);
        });
    }

    private void seedTemplate(Long id, String recipientConfig) {
        ormTemplate.runInSession(() -> {
            ErpSysNotificationTemplate t = daoProvider.daoFor(ErpSysNotificationTemplate.class).newEntity();
            t.orm_propValueByName("id", id);
            t.setNotificationType(ErpCtConstants.NOTIFY_EVENT_APPROVAL_TIMEOUT_ESCALATION);
            t.setName("TPL-TIMEOUT-ESCALATION");
            t.setChannelSet(ErpNotifyConstants.CHANNEL_IN_APP);
            t.setSubjectTpl("审批超时升级: ${contractCode}");
            t.setBodyTpl("合同 ${contractCode} 审批节点 ${approvalOrder} 超时未处理，请跟进");
            t.setRecipientResolver(ErpNotifyConstants.RESOLVER_USER_LIST);
            t.setRecipientConfig(recipientConfig);
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy(ErpNotifyConstants.MERGE_NONE);
            t.setStatus(ErpNotifyConstants.TEMPLATE_ACTIVE);
            daoProvider.daoFor(ErpSysNotificationTemplate.class).saveEntity(t);
        });
    }

    private List<ErpSysNotification> notificationsOf(String userId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        q.addFilter(eq("notificationType", ErpCtConstants.NOTIFY_EVENT_APPROVAL_TIMEOUT_ESCALATION));
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }

    private QueryBean eqQuery(String field, Object value) {
        QueryBean q = new QueryBean();
        q.addFilter(eq(field, value));
        return q;
    }

    private long toLong(Object o) {
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        return Long.parseLong(String.valueOf(o));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }
}
