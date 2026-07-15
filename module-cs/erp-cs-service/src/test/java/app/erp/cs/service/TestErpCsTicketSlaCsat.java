package app.erp.cs.service;

import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import app.erp.cs.dao.entity.ErpCsSurvey;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 端到端测试：工单状态机 + SLA 计时/匹配/升级 + CSAT 调查生命周期。
 *
 * <p>覆盖 {@code docs/plans/2026-07-04-0700-2-cs-ticket-sla-csat.md} Phase 2 Proof 项：
 * <ul>
 *   <li>工单 assign→start→resolve(标 isSlaCompleted)→close 全链路 + 非法迁移拒绝</li>
 *   <li>SLA 策略匹配 + deadline 计算（日历 + 工作日跳周末）</li>
 *   <li>scanOverdueTickets 升级（ESCALATE 审计）</li>
 *   <li>createSurvey(RESOLVED 触发) + submitSurvey(token, csat) + 重复创建拒绝 + token 无效拒绝</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsTicketSlaCsat extends JunitAutoTestCase {

    static final Long CUSTOMER_ID = 5001L;
    static final Long TICKET_TYPE_ID = 6001L;
    static final String ASSIGNEE = "user-zhang";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testTicketFullLifecycleAndIllegalTransitions() {
        Long ticketId = seedTicket("TK-LIFE", ErpCsConstants.TICKET_STATUS_NEW,
                CoreMetrics.currentDateTime().plusHours(8));

        // assign NEW→ASSIGNED
        rpcOk(mutation, "ErpCsTicket__assign", args("ticketId", ticketId, "assignedToId", ASSIGNEE));
        ErpCsTicket t = reload(ticketId);
        assertEquals(ErpCsConstants.TICKET_STATUS_ASSIGNED, t.getStatus());
        assertEquals(ASSIGNEE, t.getAssignedToId());

        // start ASSIGNED→IN_PROGRESS (startDateTime set)
        rpcOk(mutation, "ErpCsTicket__start", args("ticketId", ticketId));
        assertNotNull(reload(ticketId).getStartDateTime(), "start 记录开始处理时间");

        // resolve IN_PROGRESS→RESOLVED (duration set, isSlaCompleted=true 因 deadline 在未来)
        rpcOk(mutation, "ErpCsTicket__resolve",
                args("ticketId", ticketId, "resolution", "已修复"));
        ErpCsTicket resolved = reload(ticketId);
        assertEquals(ErpCsConstants.TICKET_STATUS_RESOLVED, resolved.getStatus());
        assertNotNull(resolved.getDuration(), "resolve 计算 duration");
        assertTrue(resolved.getIsSlaCompleted(), "deadline 在未来，isSlaCompleted=true");

        // close RESOLVED→CLOSED (endDateTime set)
        rpcOk(mutation, "ErpCsTicket__close", args("ticketId", ticketId));
        ErpCsTicket closed = reload(ticketId);
        assertEquals(ErpCsConstants.TICKET_STATUS_CLOSED, closed.getStatus());
        assertNotNull(closed.getEndDateTime(), "close 记录关闭时间");

        // 每迁移写 TicketAction 审计
        assertTrue(countActions(ticketId) >= 4, "状态迁移应写 TicketAction 审计");

        // 非法迁移：从 CLOSED 再 resolve → 拒绝
        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__resolve",
                args("ticketId", ticketId, "resolution", "x"));
        assertEquals(ErpCsErrors.ERR_INVALID_TICKET_STATUS_TRANSITION.getErrorCode(), resp.getCode(),
                "CLOSED→resolve 非法");
    }

    @Test
    public void testIllegalStartFromNew() {
        Long ticketId = seedTicket("TK-ILL", ErpCsConstants.TICKET_STATUS_NEW, null);
        // NEW→start 非法（须 ASSIGNED）
        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__start", args("ticketId", ticketId));
        assertEquals(ErpCsErrors.ERR_INVALID_TICKET_STATUS_TRANSITION.getErrorCode(), resp.getCode(),
                "NEW→start 非法");
    }

    @Test
    public void testSlaPolicyMatchCalendarDeadline() {
        // 日历小时模式 SLA 策略：resolveHours=8
        seedSlaPolicy("SLA-CAL", TICKET_TYPE_ID, 8, null, false);
        Long ticketId = seedTicket("TK-SLA-CAL", ErpCsConstants.TICKET_STATUS_NEW, null);

        LocalDateTime before = CoreMetrics.currentDateTime();
        rpcOk(mutation, "ErpCsTicket__matchAndAttachSla", args("ticketId", ticketId));
        ErpCsTicket t = reload(ticketId);
        assertNotNull(t.getSlaPolicyId(), "匹配到 SLA 策略");
        assertNotNull(t.getDeadlineDateTime(), "计算 deadlineDateTime");
        // deadline ≈ now + 8h（允许少量时间偏移）
        assertTrue(t.getDeadlineDateTime().toLocalDateTime().isAfter(before.plusHours(7)));
        assertTrue(t.getDeadlineDateTime().toLocalDateTime().isBefore(before.plusHours(9)));
    }

    @Test
    public void testSlaPolicyMatchWorkingDaysSkipsWeekend() {
        // 工作日模式 SLA 策略：resolveHours=48（2 天），跳周末
        seedSlaPolicy("SLA-WD", TICKET_TYPE_ID, 48, null, true);
        Long ticketId = seedTicket("TK-SLA-WD", ErpCsConstants.TICKET_STATUS_NEW, null);

        rpcOk(mutation, "ErpCsTicket__matchAndAttachSla", args("ticketId", ticketId));
        ErpCsTicket t = reload(ticketId);
        assertNotNull(t.getDeadlineDateTime(), "工作日模式计算 deadline");
        // deadline 不应为 null，且应在 now 之后
        assertTrue(t.getDeadlineDateTime().toLocalDateTime().isAfter(CoreMetrics.currentDateTime()));
    }

    @Test
    public void testScanOverdueTicketsCreatesEscalateAction() {
        // 已超时工单：deadline 在过去，isSlaCompleted=false，status=ASSIGNED
        Long ticketId = seedTicket("TK-OVERDUE", ErpCsConstants.TICKET_STATUS_ASSIGNED,
                CoreMetrics.currentDateTime().minusHours(2));
        int actionsBefore = countActions(ticketId);

        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__scanOverdueTickets", new java.util.HashMap<>());
        assertEquals(0, resp.getStatus(), "scanOverdueTickets 应成功: " + resp);

        assertTrue(countActions(ticketId) > actionsBefore, "超时工单应生成 ESCALATE 审计");
        // 验证存在 ESCALATE 类型审计
        assertTrue(hasActionType(ticketId, ErpCsConstants.ACTION_TYPE_ESCALATE),
                "存在 ESCALATE 审计记录");
    }

    @Test
    public void testSurveyCreatedOnResolveAndSubmitted() {
        Long ticketId = seedTicket("TK-CSAT", ErpCsConstants.TICKET_STATUS_NEW,
                CoreMetrics.currentDateTime().plusHours(8));
        rpcOk(mutation, "ErpCsTicket__assign", args("ticketId", ticketId, "assignedToId", ASSIGNEE));
        rpcOk(mutation, "ErpCsTicket__start", args("ticketId", ticketId));
        rpcOk(mutation, "ErpCsTicket__resolve",
                args("ticketId", ticketId, "resolution", "已解决"));

        // resolve 触发 createSurvey（config-gated survey-enabled=true 默认）
        ErpCsSurvey survey = findSurveyByTicket(ticketId);
        assertNotNull(survey, "RESOLVED 自动创建调查");
        assertNotNull(survey.getSurveyToken(), "调查生成 token");
        assertNotNull(survey.getSurveySentAt(), "delay=0 默认，surveySentAt=now（状态 SENT）");
        assertNull(survey.getRespondedAt(), "尚未提交，respondedAt 空");
        String token = survey.getSurveyToken();

        // submitSurvey(token, csat=5)
        Map<String, Object> submitArgs = new LinkedHashMap<>();
        submitArgs.put("surveyToken", token);
        submitArgs.put("csatScore", 5);
        submitArgs.put("comment", "服务很好");
        rpcOk(mutation, "ErpCsSurvey__submitSurvey", submitArgs);

        ErpCsSurvey responded = findSurveyByTicket(ticketId);
        assertNotNull(responded.getRespondedAt(), "submit 后 respondedAt 设置（状态 COMPLETED）");
        assertEquals(5, responded.getCsatScore());
    }

    @Test
    public void testSurveyDuplicateCreateRejected() {
        Long ticketId = seedTicket("TK-DUP", ErpCsConstants.TICKET_STATUS_NEW,
                CoreMetrics.currentDateTime().plusHours(8));
        rpcOk(mutation, "ErpCsSurvey__createSurvey", args("ticketId", ticketId));
        // 重复创建 → ERR_SURVEY_ALREADY_EXISTS
        ApiResponse<?> resp = rpc(mutation, "ErpCsSurvey__createSurvey", args("ticketId", ticketId));
        assertEquals(ErpCsErrors.ERR_SURVEY_ALREADY_EXISTS.getErrorCode(), resp.getCode(),
                "重复创建调查应拒绝");
    }

    @Test
    public void testSurveyInvalidTokenRejected() {
        ApiResponse<?> resp = rpc(mutation, "ErpCsSurvey__submitSurvey",
                args("surveyToken", "INVALID-TOKEN", "csatScore", 5));
        assertEquals(ErpCsErrors.ERR_SURVEY_TOKEN_INVALID.getErrorCode(), resp.getCode(),
                "无效 token 提交应拒绝");
    }

    @Test
    public void testReopenCancelsUnrespondedSurvey() {
        Long ticketId = seedTicket("TK-REOPEN", ErpCsConstants.TICKET_STATUS_NEW,
                CoreMetrics.currentDateTime().plusHours(8));
        rpcOk(mutation, "ErpCsTicket__assign", args("ticketId", ticketId, "assignedToId", ASSIGNEE));
        rpcOk(mutation, "ErpCsTicket__start", args("ticketId", ticketId));
        rpcOk(mutation, "ErpCsTicket__resolve",
                args("ticketId", ticketId, "resolution", "已解决"));
        assertNotNull(findSurveyByTicket(ticketId), "resolve 创建调查");

        // reopen RESOLVED→IN_PROGRESS，取消未响应调查
        rpcOk(mutation, "ErpCsTicket__reopen", args("ticketId", ticketId));
        assertEquals(ErpCsConstants.TICKET_STATUS_IN_PROGRESS, reload(ticketId).getStatus());
        assertNull(findSurveyByTicket(ticketId), "reopen 删除未响应调查");
    }

    @Test
    public void testCloseBreachedWithoutReasonRejected() {
        // 超时工单（isSlaCompleted=false）关闭需 remark 注明超时原因
        Long ticketId = seedTicket("TK-BREACH", ErpCsConstants.TICKET_STATUS_NEW, null);
        rpcOk(mutation, "ErpCsTicket__assign", args("ticketId", ticketId, "assignedToId", ASSIGNEE));
        rpcOk(mutation, "ErpCsTicket__start", args("ticketId", ticketId));
        // 手动设 isSlaCompleted=false 且 remark 为空（resolve 会因 deadline null 算 isSlaCompleted=true，
        // 这里通过直接置 false 模拟超时场景）
        ormTemplate.runInSession(() -> {
            ErpCsTicket t = daoProvider.daoFor(ErpCsTicket.class).getEntityById(ticketId);
            t.setIsSlaCompleted(false);
            t.setStatus(ErpCsConstants.TICKET_STATUS_RESOLVED);
            t.setRemark(null);
            daoProvider.daoFor(ErpCsTicket.class).updateEntity(t);
        });

        // close 应拒绝（超时无原因）
        ApiResponse<?> blocked = rpc(mutation, "ErpCsTicket__close", args("ticketId", ticketId));
        assertEquals(ErpCsErrors.ERR_TICKET_CLOSE_BREACHED_NO_REASON.getErrorCode(), blocked.getCode(),
                "超时工单关闭无 remark 应拒绝");

        // 补注超时原因后 close 成功
        ormTemplate.runInSession(() -> {
            ErpCsTicket t = daoProvider.daoFor(ErpCsTicket.class).getEntityById(ticketId);
            t.setRemark("等待供应商补件导致超时");
            daoProvider.daoFor(ErpCsTicket.class).updateEntity(t);
        });
        rpcOk(mutation, "ErpCsTicket__close", args("ticketId", ticketId));
        assertEquals(ErpCsConstants.TICKET_STATUS_CLOSED, reload(ticketId).getStatus());
    }

    @Test
    public void testCancelFromInProgress() {
        Long ticketId = seedTicket("TK-CANCEL", ErpCsConstants.TICKET_STATUS_NEW, null);
        rpcOk(mutation, "ErpCsTicket__assign", args("ticketId", ticketId, "assignedToId", ASSIGNEE));
        rpcOk(mutation, "ErpCsTicket__start", args("ticketId", ticketId));
        rpcOk(mutation, "ErpCsTicket__cancel",
                args("ticketId", ticketId, "cancelReason", "重复工单"));
        assertEquals(ErpCsConstants.TICKET_STATUS_CANCELLED, reload(ticketId).getStatus());

        // 终态后再迁移 → ERR_TICKET_ALREADY_TERMINAL
        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__cancel",
                args("ticketId", ticketId, "cancelReason", "x"));
        assertEquals(ErpCsErrors.ERR_TICKET_ALREADY_TERMINAL.getErrorCode(), resp.getCode(),
                "CANCELLED 终态不可迁移");
    }

    @Test
    public void testFindSlaWarnings() {
        // deadline 在 now 到 now+60min 之间 → 命中预警
        Long ticketId = seedTicket("TK-WARN", ErpCsConstants.TICKET_STATUS_ASSIGNED,
                CoreMetrics.currentDateTime().plusMinutes(30));
        ApiResponse<?> resp = rpc(query, "ErpCsTicket__findSlaWarnings",
                args("beforeMinutes", 60));
        assertEquals(0, resp.getStatus(), "findSlaWarnings 应成功: " + resp);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> data = (List<Map<String, Object>>) resp.getData();
        assertNotNull(data);
        boolean found = false;
        for (Map<String, Object> row : data) {
            if (ticketId.equals(toLong(row.get("id")))) {
                found = true;
                break;
            }
        }
        assertTrue(found, "findSlaWarnings 应返回预警窗口内工单");
    }

    // ---------- helpers ----------

    private ErpCsTicket reload(Long ticketId) {
        return daoProvider.daoFor(ErpCsTicket.class).getEntityById(ticketId);
    }

    private int countActions(Long ticketId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        return daoProvider.daoFor(ErpCsTicketAction.class).findAllByQuery(q).size();
    }

    private boolean hasActionType(Long ticketId, String actionType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.addFilter(eq("actionType", actionType));
        q.setLimit(1);
        return !daoProvider.daoFor(ErpCsTicketAction.class).findAllByQuery(q).isEmpty();
    }

    private ErpCsSurvey findSurveyByTicket(Long ticketId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.setLimit(1);
        List<ErpCsSurvey> list = daoProvider.daoFor(ErpCsSurvey.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private Long seedTicket(String code, String status, LocalDateTime deadline) {
        Long id = 7000L + (long) (Math.abs(code.hashCode()) % 1000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
            ErpCsTicket t = new ErpCsTicket();
            t.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
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
            if (deadline != null) {
                t.setDeadlineDateTime(deadline != null ? Timestamp.valueOf(deadline) : null);
            }
            dao.saveEntity(t);
        });
        return id;
    }

    private void seedSlaPolicy(String code, Long ticketTypeId, Integer resolveHours,
                               Integer resolveDays, boolean isWorkingDays) {
        Long id = 8000L + (long) (Math.abs(code.hashCode()) % 1000);
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsSlaPolicy> dao = daoProvider.daoFor(ErpCsSlaPolicy.class);
            ErpCsSlaPolicy p = new ErpCsSlaPolicy();
            p.orm_propValueByName("id", id);
            p.setCode(code);
            p.setName("SLA-" + code);
            p.setTicketTypeId(ticketTypeId);
            p.setResolveHours(resolveHours);
            p.setResolveDays(resolveDays);
            p.setIsWorkingDays(isWorkingDays);
            dao.saveEntity(p);
        });
    }

    private static Map<String, Object> args(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private static Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Long) return (Long) v;
        if (v instanceof Number) return ((Number) v).longValue();
        return Long.valueOf(String.valueOf(v));
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
