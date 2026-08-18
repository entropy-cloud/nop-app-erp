package app.erp.cs.service.job;

import app.erp.cs.dao.entity.ErpCsSurvey;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.service.ErpCsConstants;
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
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 满意度调查延迟发送链 Job 测试（RC-R1.70，P1-RC-059，UC-CS-08 ①② + 后置 + 异常；
 * plan 2026-08-18-1849-2 Phase 2 Proof ①-⑨）。
 *
 * <p>覆盖 {@link ErpCsSurveySendJob}：
 * ① cron 空值跳过 ② PENDING 到期 → SENT + surveySentAt + notify 落库（ErpSysNotification 行）
 * ③ 未到期（delay 窗口内）跳过 ④ 遗留 status=null 行派生兼容派发 ⑤ 派发异常 → FAILED + failureCount=1
 * ⑥ 重试成功 FAILED→SENT ⑦ 超限终态不再重试 ⑧ submitSurvey 写 COMPLETED ⑨ reopen 删
 * PENDING/FAILED 未响应行（COMPLETED 已响应保留对比 + FAILED 行断言对称）。
 *
 * <p>断言式测试 + 空 autotest.yaml 标记（镜像 R1.65/R1.67/R1.68 范式——通知行/审计列含真实时钟，
 * 录制表快照会随日期漂移翻红）。真实时钟下到期判定自然成立：delay=0 时 createTime（seed 即时）
 * 早于 job 扫描阈值（execute 时计算）；delay=24 时阈值=now-24h 早于 createTime → 未到期。
 * 失败路径经子类覆写 {@code dispatchSurvey} 注入异常（FAILED 标记真实代码路径）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsSurveySendJob extends JunitAutoTestCase {

    static final Long CUSTOMER_ID = 9101L;
    static final Long TICKET_TYPE_ID = 9201L;
    static final String RECIPIENT = "cs-survey-recipient";
    static final String NOTIFY_EVENT = ErpCsConstants.NOTIFY_EVENT_SURVEY_INVITATION;
    static final String SURVEY_TOKEN_PREFIX = "tok-send-";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    ErpCsSurveySendJob sendJob;

    // ---------- ① cron 空值跳过 ----------

    @Test
    public void testCronEmptySkipsExecution() {
        Long ticketId = seedTicket("TK-SND-CRON", ErpCsConstants.TICKET_STATUS_RESOLVED);
        seedSurvey(9301L, ticketId, SURVEY_TOKEN_PREFIX + "cron", ErpCsConstants.SURVEY_STATUS_PENDING, null, null);

        sendJob.execute(); // cron 未配置（默认空）= 「不调度」语义

        ErpCsSurvey survey = reloadSurvey(9301L);
        assertEquals(ErpCsConstants.SURVEY_STATUS_PENDING, survey.getStatus(), "cron 空值应跳过扫描（状态不变）");
        assertNull(survey.getSurveySentAt(), "cron 空值不应发送");
        assertEquals(0, countNotifications(NOTIFY_EVENT), "零通知行");
    }

    // ---------- ② PENDING 到期 → SENT + surveySentAt + notify 落库 ----------

    @Test
    public void testDuePendingSurveySentWithNotificationRow() {
        seedCustomer(CUSTOMER_ID, "ACME Corp");
        seedSurveyInviteTemplate(7901L, RECIPIENT);
        Long ticketId = seedTicket("TK-SND-OK", ErpCsConstants.TICKET_STATUS_RESOLVED);
        seedSurvey(9302L, ticketId, SURVEY_TOKEN_PREFIX + "ok", ErpCsConstants.SURVEY_STATUS_PENDING, null, null);

        withSurveySendCron(sendJob::execute);

        ErpCsSurvey survey = reloadSurvey(9302L);
        assertEquals(ErpCsConstants.SURVEY_STATUS_SENT, survey.getStatus(), "到期 PENDING 应转 SENT");
        assertNotNull(survey.getSurveySentAt(), "发送时间应落库");
        ErpSysNotification n = findNotification(NOTIFY_EVENT);
        assertNotNull(n, "应派发 cs.survey-invitation ErpSysNotification 行");
        assertEquals(RECIPIENT, n.getRecipientUserId(), "接收人应匹配模板 USER_LIST");
    }

    // ---------- ③ 未到期（delay 窗口内）跳过 ----------

    @Test
    public void testNotDueWithinDelayWindowSkipped() {
        seedCustomer(CUSTOMER_ID, "ACME Corp");
        seedSurveyInviteTemplate(7902L, RECIPIENT);
        Long ticketId = seedTicket("TK-SND-WAIT", ErpCsConstants.TICKET_STATUS_RESOLVED);
        // createTime=冻结 now（seed 即时），delay=24 → now < createTime+24h 未到期
        seedSurvey(9303L, ticketId, SURVEY_TOKEN_PREFIX + "wait", ErpCsConstants.SURVEY_STATUS_PENDING, null, null);

        AppConfig.getConfigProvider().assignConfigValue(ErpCsConstants.CONFIG_SURVEY_SEND_DELAY, "24");
        try {
            withSurveySendCron(sendJob::execute);
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(ErpCsConstants.CONFIG_SURVEY_SEND_DELAY, "0");
        }

        ErpCsSurvey survey = reloadSurvey(9303L);
        assertEquals(ErpCsConstants.SURVEY_STATUS_PENDING, survey.getStatus(), "delay 窗口内不应发送");
        assertNull(survey.getSurveySentAt(), "未到期 surveySentAt 保持空");
        assertEquals(0, countNotifications(NOTIFY_EVENT), "未到期零通知行");
    }

    // ---------- ④ 遗留 null 行派生兼容派发 ----------

    @Test
    public void testLegacyNullStatusRowDerivedPendingAndSent() {
        seedCustomer(CUSTOMER_ID, "ACME Corp");
        seedSurveyInviteTemplate(7903L, RECIPIENT);
        Long ticketId = seedTicket("TK-SND-LEGACY", ErpCsConstants.TICKET_STATUS_RESOLVED);
        // 遗留行：status=null + surveySentAt=null（派生 PENDING）——job 应派发并显式写 SENT
        seedSurvey(9304L, ticketId, SURVEY_TOKEN_PREFIX + "legacy", null, null, null);

        withSurveySendCron(sendJob::execute);

        ErpCsSurvey survey = reloadSurvey(9304L);
        assertEquals(ErpCsConstants.SURVEY_STATUS_SENT, survey.getStatus(), "遗留派生 PENDING 应派发并显式转 SENT");
        assertNotNull(survey.getSurveySentAt(), "发送时间应落库");
        assertTrue(countNotifications(NOTIFY_EVENT) > 0, "遗留行派发应产生通知行");
    }

    // ---------- ⑤ 派发异常 → FAILED + failureCount=1 ----------

    @Test
    public void testDispatchFailureMarksFailedWithCount() {
        Long ticketId = seedTicket("TK-SND-FAIL", ErpCsConstants.TICKET_STATUS_RESOLVED);
        seedSurvey(9305L, ticketId, SURVEY_TOKEN_PREFIX + "fail", ErpCsConstants.SURVEY_STATUS_PENDING, null, null);

        ErpCsSurveySendJob failingJob = newFailingDispatchJob();
        withSurveySendCron(failingJob::execute);

        ErpCsSurvey survey = reloadSurvey(9305L);
        assertEquals(ErpCsConstants.SURVEY_STATUS_FAILED, survey.getStatus(), "派发异常应标记 FAILED");
        assertEquals(Integer.valueOf(1), survey.getFailureCount(), "failureCount 应起算 1");
        assertNull(survey.getSurveySentAt(), "失败行 surveySentAt 保持空");
    }

    // ---------- ⑥ 重试成功 FAILED→SENT ----------

    @Test
    public void testRetryFailedSurveySucceeds() {
        seedCustomer(CUSTOMER_ID, "ACME Corp");
        seedSurveyInviteTemplate(7904L, RECIPIENT);
        Long ticketId = seedTicket("TK-SND-RETRY", ErpCsConstants.TICKET_STATUS_RESOLVED);
        seedSurvey(9306L, ticketId, SURVEY_TOKEN_PREFIX + "retry", ErpCsConstants.SURVEY_STATUS_FAILED, null, 1);

        withSurveySendCron(sendJob::execute);

        ErpCsSurvey survey = reloadSurvey(9306L);
        assertEquals(ErpCsConstants.SURVEY_STATUS_SENT, survey.getStatus(), "FAILED 未超限应重试成功转 SENT");
        assertNotNull(survey.getSurveySentAt(), "重试发送时间应落库");
        assertTrue(countNotifications(NOTIFY_EVENT) > 0, "重试派发应产生通知行");
    }

    // ---------- ⑦ 超限终态不再重试 ----------

    @Test
    public void testRetryExceededStaysTerminalFailed() {
        seedCustomer(CUSTOMER_ID, "ACME Corp");
        seedSurveyInviteTemplate(7905L, RECIPIENT);
        Long ticketId = seedTicket("TK-SND-MAX", ErpCsConstants.TICKET_STATUS_RESOLVED);
        // failureCount=3 = 默认 retry-max → 终态 FAILED 保留
        seedSurvey(9307L, ticketId, SURVEY_TOKEN_PREFIX + "max", ErpCsConstants.SURVEY_STATUS_FAILED, null, 3);

        withSurveySendCron(sendJob::execute);

        ErpCsSurvey survey = reloadSurvey(9307L);
        assertEquals(ErpCsConstants.SURVEY_STATUS_FAILED, survey.getStatus(), "超限终态 FAILED 保留");
        assertEquals(Integer.valueOf(3), survey.getFailureCount(), "超限不再重试（计数不变）");
        assertNull(survey.getSurveySentAt(), "超限不再发送");
        assertEquals(0, countNotifications(NOTIFY_EVENT), "超限零通知行");
    }

    // ---------- ⑧ submitSurvey 写 COMPLETED ----------

    @Test
    public void testSubmitSurveyWritesCompleted() {
        Long ticketId = seedTicket("TK-SND-SUBMIT", ErpCsConstants.TICKET_STATUS_RESOLVED);
        seedSurvey(9308L, ticketId, SURVEY_TOKEN_PREFIX + "submit", ErpCsConstants.SURVEY_STATUS_SENT,
                Timestamp.valueOf("2026-07-17 08:00:00"), null);

        Map<String, Object> args = new HashMap<>();
        args.put("surveyToken", SURVEY_TOKEN_PREFIX + "submit");
        args.put("csatScore", 5);
        ApiResponse<?> resp = rpc(mutation, "ErpCsSurvey__submitSurvey", args);
        assertEquals(0, resp.getStatus(), "submitSurvey 应成功: " + resp);

        ErpCsSurvey survey = reloadSurvey(9308L);
        assertEquals(ErpCsConstants.SURVEY_STATUS_COMPLETED, survey.getStatus(), "提交后终态 COMPLETED 落库");
        assertNotNull(survey.getRespondedAt(), "respondedAt 应设置");
        assertEquals(Integer.valueOf(5), survey.getCsatScore());
    }

    // ---------- ⑨ reopen 删 PENDING/FAILED 未响应行（COMPLETED 保留） ----------

    @Test
    public void testReopenCancelsPendingAndFailedUnrespondedSurveys() {
        // PENDING 未响应：reopen 删除
        Long pendingTicket = seedTicket("TK-SND-RP", ErpCsConstants.TICKET_STATUS_RESOLVED);
        seedSurvey(9309L, pendingTicket, SURVEY_TOKEN_PREFIX + "rp", ErpCsConstants.SURVEY_STATUS_PENDING, null, null);
        // FAILED 未响应（对称断言）：reopen 删除
        Long failedTicket = seedTicket("TK-SND-RF", ErpCsConstants.TICKET_STATUS_RESOLVED);
        seedSurvey(9310L, failedTicket, SURVEY_TOKEN_PREFIX + "rf", ErpCsConstants.SURVEY_STATUS_FAILED, null, 2);
        // 已响应 COMPLETED：reopen 保留
        Long doneTicket = seedTicket("TK-SND-RC", ErpCsConstants.TICKET_STATUS_RESOLVED);
        seedSurvey(9311L, doneTicket, SURVEY_TOKEN_PREFIX + "rc", ErpCsConstants.SURVEY_STATUS_COMPLETED,
                Timestamp.valueOf("2026-07-17 08:00:00"), null);
        markResponded(9311L);

        rpcOk(mutation, "ErpCsTicket__reopen", Map.of("ticketId", pendingTicket));
        rpcOk(mutation, "ErpCsTicket__reopen", Map.of("ticketId", failedTicket));
        rpcOk(mutation, "ErpCsTicket__reopen", Map.of("ticketId", doneTicket));

        assertNull(reloadSurvey(9309L), "reopen 应删除 PENDING 未响应调查（避免误发）");
        assertNull(reloadSurvey(9310L), "reopen 应删除 FAILED 未响应调查（对称取消）");
        assertEquals(ErpCsConstants.TICKET_STATUS_IN_PROGRESS, reloadTicket(pendingTicket).getStatus());
        assertNotNull(reloadSurvey(9311L), "已响应 COMPLETED 调查应保留");
    }

    // ---------- ⑩ execute() 为 public 无参方法（BeanMethodJobInvoker 兼容） ----------

    @Test
    public void testExecuteIsNoArgPublicMethod() throws NoSuchMethodException {
        Method m = ErpCsSurveySendJob.class.getMethod("execute");
        assertEquals(0, m.getParameterCount(), "execute() 必须无参以适配 BeanMethodJobInvoker");
        assertTrue(Modifier.isPublic(m.getModifiers()), "execute() 必须为 public");
    }

    // ---------- helpers ----------

    private void withSurveySendCron(Runnable action) {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpCsConstants.CONFIG_SURVEY_SEND_CRON, "0 0/10 * * * ?");
        try {
            action.run();
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpCsConstants.CONFIG_SURVEY_SEND_CRON, "");
        }
    }

    /** 派发异常注入：覆写 dispatchSurvey 抛异常，其余（扫描/FAILED 标记/session 提交）走真实代码。 */
    private ErpCsSurveySendJob newFailingDispatchJob() {
        ErpCsSurveySendJob job = new ErpCsSurveySendJob() {
            @Override
            protected void dispatchSurvey(ErpCsSurvey survey, IServiceContext ctx) {
                throw new RuntimeException("simulated-notify-failure");
            }
        };
        job.setDaoProvider(daoProvider);
        job.setOrmTemplate(ormTemplate);
        return job;
    }

    private ErpCsSurvey reloadSurvey(Long id) {
        // findAllByQuery 过滤逻辑删除行（getEntityById 不过滤——reopen 逻辑删除断言需要感知删除）
        QueryBean q = new QueryBean();
        q.addFilter(eq("id", id));
        q.setLimit(1);
        List<ErpCsSurvey> list = daoProvider.daoFor(ErpCsSurvey.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpCsTicket reloadTicket(Long id) {
        return daoProvider.daoFor(ErpCsTicket.class).getEntityById(id);
    }

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

    private void markResponded(Long surveyId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsSurvey> dao = daoProvider.daoFor(ErpCsSurvey.class);
            ErpCsSurvey s = dao.getEntityById(surveyId);
            s.setRespondedAt(Timestamp.valueOf("2026-07-17 09:00:00"));
            dao.updateEntity(s);
        });
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

    private void seedSurveyInviteTemplate(Long id, String recipientUserId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", id);
            t.setNotificationType(NOTIFY_EVENT);
            t.setName("满意度调查邀请");
            t.setChannelSet("IN_APP");
            t.setSubjectTpl("满意度调查邀请: ${ticketCode}");
            t.setBodyTpl("工单 ${ticketCode} 的满意度调查已就绪（渠道 ${channel}，token ${surveyToken}），请转达客户 ${customerName} 填写");
            t.setRecipientResolver("USER_LIST");
            t.setRecipientConfig("{\"userIds\":[\"" + recipientUserId + "\"]}");
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy("NONE");
            t.setStatus("ACTIVE");
            dao.saveEntity(t);
        });
    }

    private Long seedTicket(String code, String status) {
        Long id = 9000L + (long) (Math.abs(code.hashCode()) % 500);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
            ErpCsTicket t = new ErpCsTicket();
            t.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
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
            dao.saveEntity(t);
        });
        return id;
    }

    private void seedSurvey(Long id, Long ticketId, String token, String status,
                            java.sql.Timestamp surveySentAt, Integer failureCount) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsSurvey> dao = daoProvider.daoFor(ErpCsSurvey.class);
            ErpCsSurvey s = new ErpCsSurvey();
            s.orm_propValueByName("id", id);
            s.setTicketId(ticketId);
            s.setSurveyToken(token);
            s.setSurveyChannel(ErpCsConstants.SURVEY_CHANNEL_PORTAL);
            s.setStatus(status);
            s.setSurveySentAt(surveySentAt);
            s.setFailureCount(failureCount);
            dao.saveEntity(s);
        });
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                               Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                       Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功，但返回: " + resp);
    }
}
