package io.nop.app.all.it;

import app.erp.cs.dao.entity.ErpCsCannedResponse;
import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import app.erp.cs.dao.entity.ErpCsSurvey;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.notify.dao.entity.ErpSysNotification;
import app.erp.notify.dao.entity.ErpSysNotificationTemplate;
import app.erp.notify.service.ErpNotifyConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * B8 C16：CS 工单 SLA 与通知派发（按 {@code docs/design/integration-testing.md §6 C16} 规格）。
 *
 * <p>全链（引用部署 seed：组织 2 / CUST-001 客户 1 / 工单类型 TT-COMPLAINT 投诉（id 1，默认 HIGH）/
 * seed 工单 2 行 + seed 调研（ticket 1，CSAT 5/NPS 9））：自包含 SLA 策略（id 9001，投诉类型精确匹配，
 * resolveHours=8，日历小时模式）+ 自包含宏响应（id 9101，{customer_name}/{ticket_id} 系统变量）→
 * 自包含工单 {@code ErpCsTicket__save}（save 后置自动挂载 SLA：slaPolicyId + deadline 断言；自动分配
 * 候选池空留 NEW；创建确认通知静默降级）→ 显式 {@code matchAndAttachSla}（幂等重挂）→ 六态状态机推进
 * {@code assign}（NEW→ASSIGNED）→ {@code start}（→IN_PROGRESS，startDateTime 记时点）→
 * {@code applyCannedResponse}（usageCount 0→1 递增 + 渲染正文断言）→ {@code resolve}（→RESOLVED，
 * isSlaCompleted=true——冻结时钟 now=deadline 前达标记）+ 自动创建满意度调研（config-gated trigger
 * RESOLVED）→ {@code close}（→CLOSED 终态）→ {@code submitSurvey}（CSAT 5/NPS 9 回填，token 经 DB 反查
 * {@code addVar} 传递）→ {@code ErpCsReport__renderHtml(reportName="ticket-sla-csat-summary")}（投诉桶
 * token：totalTickets=2/SLA 命中=2/CSAT 5.00/NPS 9.00 = seed 调研 + 自包含回填均值）→ 通知派发
 * {@code ErpSysNotification__notify}（自包含模板，C09 先例——实仓工单链 notify 均静默降级无落库）→
 * {@code markRead} → {@code countUnread} 归零。
 *
 * <p>Phase 2 Decision（四项裁决，落盘设计文档 §6 C16 勘误）：
 * <ol>
 *   <li><b>六态推进动作序列</b>：assign/start/resolve/close（无 {@code respond} 动作——B2 C04 已裁决
 *       「以当前实现为准」，设计文档步骤 1 提及 respond 为动作名漂移勘误）；close 前置仅超时工单
 *       （isSlaCompleted=false）须 remark，本用例达标路径直close。</li>
 *   <li><b>SLA 达标口径</b>：{@code resolve} 置 isSlaCompleted = resolvedAt ≤ deadlineDateTime
 *       （{@code ErpCsTicketResolveProcessor} 实仓语义）；deadline = matchAndAttachSla 时
 *       {@code SlaDeadlineCalculator.calculate(now, policy)}（日历小时模式 now+8h）；冻结时钟下
 *       now=2026-07-17T00:00 → deadline=08:00 → resolve 达标 true。save 后置自动挂载（D1 守卫：
 *       slaPolicyId/deadline 均空才触发）+ 显式 matchAndAttachSla 幂等重挂双证。</li>
 *   <li><b>ticket-sla-csat-summary 期望 token</b>：数据集按 ticketTypeId 聚合全表工单——投诉桶
 *       totalTickets=2（seed TKT-2026-001 + 自包含）/ slaCompleted=2 / surveyCount=2（seed 调研 +
 *       自包含回填）/ avgCsat=(5+5)/2=5.00 / avgNps=(9+9)/2=9.00（单元格 numberFormat {@code #,##0.00}）；
 *       设计期望「投诉桶/5.00/9.00」成立（自包含回填取值与 seed 同值保持均值稳定）。</li>
 *   <li><b>通知模板/落库路径与冻结时钟</b>：实仓工单链 notify（创建确认/SLA 预警/知识库建议/wf 任务）
 *       均模板缺失静默降级不落库——通知断言路径改 C09 自包含模板先例（{@code ErpSysNotification__notify}
 *       → markRead → countUnread 归零）；冻结时钟 {@code C15C16C17FrozenClockExtension}（2026-07-17）
 *       保证 SLA deadline/工单 businessDate/调研 respondedAt 确定性。</li>
 * </ol>
 *
 * <p>三层验证：层 1 = JUnit 关键断言（SLA 装配 + 六态翻转 + isSlaCompleted + usageCount 递增 + 调研回填 +
 * 报表 token + 通知已读归零）；层 2 = 每步 response 快照；层 3 = output/tables 变更行（sla_policy +
 * canned_response + ticket + ticket_action + survey + notification 族）。RECORDING→CHECKING 往返按
 * M0.2 试点范式完成。
 */
@NopTestConfig(localDb = false,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.orm.init-database-data", value = "true")
public class TestErpC16CsSlaNotification extends ErpIntegrationTestCase {

    static final String TICKET_CODE = "IT-C16-TKT-001";
    static final String SLA_POLICY_ID = "9001";
    static final String CANNED_RESPONSE_ID = "9101";
    static final String NOTIFY_USER = "it-c16-user";
    static final String NOTIFY_EVENT = "cs.ticket-closed";

    @RegisterExtension
    static C15C16C17FrozenClockExtension frozenClock = new C15C16C17FrozenClockExtension();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;

    @Test
    public void testCsSlaNotificationClosedLoop() {
        // ---------- 1. 自包含 SLA 策略 + 宏响应（seed 无对应行——自包含建数） ----------
        seedSlaPolicyAndCannedResponse();

        // ---------- 2. 自包含工单 save（NEW；save 后置自动挂载 SLA：slaPolicyId + deadline） ----------
        ApiResponse<?> ticketSave = rpcMutation("ErpCsTicket__save", request("1_ticket_save.json5", Map.class));
        output("1_ticket_save_response.json5", ticketSave);
        assertEquals(0, ticketSave.getStatus(), "工单保存应成功");
        String ticketId = idOf(ticketSave);
        addVar("ticketId", ticketId);

        ErpCsTicket saved = reloadTicket(ticketId);
        assertEquals("NEW", saved.getStatus(), "初始 status=NEW（自动分配候选池空留 NEW）");
        assertEquals(SLA_POLICY_ID, saved.getSlaPolicyId(), "save 后置自动挂载 SLA 策略（D1 守卫）");
        assertNotNull(saved.getDeadlineDateTime(), "自动挂载计算 deadline（now+8h 日历小时模式）");

        // ---------- 3. 显式 matchAndAttachSla（幂等重挂：策略/deadline 保持） ----------
        ApiResponse<?> attachSla = rpcMutation("ErpCsTicket__matchAndAttachSla",
                request("2_ticket_match_attach_sla.json5", Map.class));
        output("2_ticket_match_attach_sla_response.json5", attachSla);
        assertEquals(0, attachSla.getStatus(), "matchAndAttachSla 应成功");
        ErpCsTicket attached = reloadTicket(ticketId);
        assertEquals(SLA_POLICY_ID, attached.getSlaPolicyId(), "重挂后 SLA 策略保持");
        assertNotNull(attached.getDeadlineDateTime(), "重挂后 deadline 保持");

        // ---------- 4. 六态推进：assign（NEW→ASSIGNED）→ start（→IN_PROGRESS） ----------
        ApiResponse<?> assign = rpcMutation("ErpCsTicket__assign", request("3_ticket_assign.json5", Map.class));
        output("3_ticket_assign_response.json5", assign);
        assertEquals(0, assign.getStatus(), "assign 应成功");
        assertEquals("ASSIGNED", reloadTicket(ticketId).getStatus(), "assign 后 ASSIGNED");

        ApiResponse<?> start = rpcMutation("ErpCsTicket__start", request("4_ticket_start.json5", Map.class));
        output("4_ticket_start_response.json5", start);
        assertEquals(0, start.getStatus(), "start 应成功");
        ErpCsTicket started = reloadTicket(ticketId);
        assertEquals("IN_PROGRESS", started.getStatus(), "start 后 IN_PROGRESS");
        assertNotNull(started.getStartDateTime(), "start 记录计时起点 startDateTime");

        // ---------- 5. applyCannedResponse（usageCount 0→1 + 渲染正文） ----------
        ApiResponse<?> applyCanned = rpcMutation("ErpCsCannedResponse__applyCannedResponse",
                request("5_apply_canned_response.json5", Map.class));
        output("5_apply_canned_response_response.json5", applyCanned);
        assertEquals(0, applyCanned.getStatus(), "applyCannedResponse 应成功");
        String rendered = String.valueOf(applyCanned.getData());
        assertTrue(rendered.contains(TICKET_CODE), "渲染正文含工单号 {ticket_id}: " + rendered);
        assertEquals(1, reload(ErpCsCannedResponse.class, CANNED_RESPONSE_ID).getUsageCount(),
                "宏响应 usageCount 0→1 递增");

        // ---------- 6. resolve（→RESOLVED + isSlaCompleted=true + 自动创建调研） ----------
        ApiResponse<?> resolve = rpcMutation("ErpCsTicket__resolve", request("6_ticket_resolve.json5", Map.class));
        output("6_ticket_resolve_response.json5", resolve);
        assertEquals(0, resolve.getStatus(), "resolve 应成功");
        ErpCsTicket resolved = reloadTicket(ticketId);
        assertEquals("RESOLVED", resolved.getStatus(), "resolve 后 RESOLVED");
        assertEquals(Boolean.TRUE, resolved.getIsSlaCompleted(),
                "冻结时钟 now(00:00) ≤ deadline(08:00) → isSlaCompleted=true（SLA 达标口径）");

        // 自动创建满意度调研（config-gated trigger=RESOLVED）：token 经 DB 反查 addVar
        ErpCsSurvey autoSurvey = findSurveyByTicket(ticketId);
        assertNotNull(autoSurvey, "resolve 应自动创建满意度调研");
        assertEquals("SENT", autoSurvey.getStatus(), "delay=0 立即发送 SENT");
        addVar("surveyToken", autoSurvey.getSurveyToken());

        // ---------- 7. close（→CLOSED 终态） ----------
        ApiResponse<?> close = rpcMutation("ErpCsTicket__close", request("7_ticket_close.json5", Map.class));
        output("7_ticket_close_response.json5", close);
        assertEquals(0, close.getStatus(), "close 应成功（SLA 达标无 remark 前置）");
        ErpCsTicket closed = reloadTicket(ticketId);
        assertEquals("CLOSED", closed.getStatus(), "close 后 CLOSED（六态终态）");
        assertNotNull(closed.getEndDateTime(), "close 记录 endDateTime");

        // ---------- 8. submitSurvey（CSAT 5/NPS 9 回填——与 seed 同值保持报表均值稳定） ----------
        ApiResponse<?> submitSurvey = rpcMutation("ErpCsSurvey__submitSurvey",
                request("8_survey_submit.json5", Map.class));
        output("8_survey_submit_response.json5", submitSurvey);
        assertEquals(0, submitSurvey.getStatus(), "submitSurvey 应成功");
        ErpCsSurvey responded = reload(ErpCsSurvey.class, autoSurvey.getId());
        assertEquals(5, responded.getCsatScore(), "CSAT=5 回填");
        assertEquals(9, responded.getNpsScore(), "NPS=9 回填");
        assertEquals("COMPLETED", responded.getStatus(), "调研 COMPLETED");

        // ---------- 9. ticket-sla-csat-summary 报表（投诉桶 token 断言） ----------
        ApiResponse<?> report = executeRpc(GraphQLOperationType.query, "ErpCsReport__renderHtml",
                request("9_report_sla_csat.json5", Map.class));
        output("9_report_sla_csat_response.json5", report);
        assertEquals(0, report.getStatus(), "ticket-sla-csat-summary 渲染应成功");
        String html = String.valueOf(report.getData());
        assertTrue(html.contains("投诉"), "报表含投诉桶（工单类型聚合）");
        assertTrue(html.contains("5.00"), "报表含 avgCsat=5.00（seed 5 + 自包含 5 均值）");
        assertTrue(html.contains("9.00"), "报表含 avgNps=9.00（seed 9 + 自包含 9 均值）");

        // ---------- 10. 通知派发（C09 自包含模板先例）→ markRead → countUnread 归零 ----------
        seedNotificationTemplate();
        ApiResponse<?> notify = rpcMutation("ErpSysNotification__notify", request("10_notify.json5", Map.class));
        output("10_notify_response.json5", notify);
        assertEquals(0, notify.getStatus(), "通知派发应成功");

        List<ErpSysNotification> sent = notificationsOf(NOTIFY_USER);
        assertEquals(1, sent.size(), "应派发 1 条工单关闭通知");
        ErpSysNotification notification = sent.get(0);
        assertTrue(notification.getBody().contains(TICKET_CODE), "通知正文含工单号: " + notification.getBody());
        addVar("notifId", notification.getId());

        ApiResponse<?> markRead = rpcMutation("ErpSysNotification__markRead",
                request("11_notify_mark_read.json5", Map.class));
        output("11_notify_mark_read_response.json5", markRead);
        assertEquals(0, markRead.getStatus(), "markRead 应成功");

        ApiResponse<?> unread = executeRpc(GraphQLOperationType.query, "ErpSysNotification__countUnread",
                request("12_notify_count_unread.json5", Map.class));
        output("12_notify_count_unread_response.json5", unread);
        assertEquals(0, unread.getStatus(), "countUnread 查询应成功");
        assertEquals(0, ((Number) unread.getData()).intValue(), "markRead 后 countUnread 归零");
    }

    // ---------- helpers ----------

    private ErpCsTicket reloadTicket(String ticketId) {
        return daoProvider.daoFor(ErpCsTicket.class).getEntityById(ticketId);
    }

    private ErpCsSurvey findSurveyByTicket(String ticketId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.setLimit(1);
        List<ErpCsSurvey> list = daoProvider.daoFor(ErpCsSurvey.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpSysNotification> notificationsOf(String userId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("recipientUserId", userId));
        q.addOrderField("createTime", true);
        return daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
    }

    /** 自包含 SLA 策略（投诉类型精确匹配，日历小时模式 8h）+ 宏响应（{customer_name}/{ticket_id} 变量）。 */
    private void seedSlaPolicyAndCannedResponse() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsSlaPolicy> policyDao = daoProvider.daoFor(ErpCsSlaPolicy.class);
            ErpCsSlaPolicy policy = new ErpCsSlaPolicy();
            policy.orm_propValueByName("id", SLA_POLICY_ID);
            policy.setCode("SLA-C16-COMPLAINT");
            policy.setName("C16 投诉工单 8 小时策略");
            policy.setTicketTypeId("1");
            policy.orm_propValueByName("resolveHours", 8);
            policy.orm_propValueByName("isWorkingDays", Boolean.FALSE);
            policyDao.saveEntity(policy);

            IEntityDao<ErpCsCannedResponse> cannedDao = daoProvider.daoFor(ErpCsCannedResponse.class);
            ErpCsCannedResponse canned = new ErpCsCannedResponse();
            canned.orm_propValueByName("id", CANNED_RESPONSE_ID);
            canned.setCode("CR-C16-001");
            canned.setOrgId("2");
            canned.setTitle("C16 关闭致谢模板");
            canned.setContent("您好 {customer_name}，工单 {ticket_id} 已处理完毕，感谢您的支持。");
            canned.setIsActive(Boolean.TRUE);
            canned.orm_propValueByName("usageCount", 0);
            cannedDao.saveEntity(canned);
        });
    }

    private void seedNotificationTemplate() {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", "7109");
            t.setNotificationType(NOTIFY_EVENT);
            t.setName("TPL-" + NOTIFY_EVENT);
            t.setChannelSet(ErpNotifyConstants.CHANNEL_IN_APP);
            t.setSubjectTpl("工单关闭通知: ${ticketCode}");
            t.setBodyTpl("工单 ${ticketCode} 已关闭，请知悉");
            t.setRecipientResolver(ErpNotifyConstants.RESOLVER_USER_LIST);
            t.setRecipientConfig("{\"userIds\":[\"" + NOTIFY_USER + "\"]}");
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy(ErpNotifyConstants.MERGE_NONE);
            t.setStatus(ErpNotifyConstants.TEMPLATE_ACTIVE);
            dao.saveEntity(t);
        });
    }
}
