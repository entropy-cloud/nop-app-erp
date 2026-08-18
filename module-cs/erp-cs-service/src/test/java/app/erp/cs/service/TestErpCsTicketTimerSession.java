package app.erp.cs.service;

import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketTimerSession;
import app.erp.cs.dao.entity.ErpCsTimeEntry;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.context.ContextProvider;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static io.nop.graphql.core.ast.GraphQLOperationType.query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 工单计时器 session 端到端测试（RC-R1.66，P1-RC-055，UC-CS-11 ①-⑨；
 * plan 2026-08-17-2125-2 Phase 3 测试组 ①-⑨）。
 *
 * <p>冻结时钟（2026-07-17T00:00）驱动全部时间语义确定性：暂停累计/12h 封顶时刻/停止 duration 数学
 * 均可精确到分钟断言。断言式测试 + 空 autotest.yaml 标记（镜像 R1.65 TestErpCsTicketCreateEnrichment
 * 范式——时间敏感字段经冻结时钟确定性后以行为断言为主，不录制表快照）。
 *
 * <p>覆盖：①start→RUNNING + 单计时器双路径拒绝（应用守卫 + DB UK dao 直插）+ ⑥跨工单仍唯一；
 * ②pause/resume（cumulativePauseMinutes 累计 + pauseReason 可选）；③stop→ErpCsTimeEntry
 * （duration = 墙钟 − Σ暂停 + source=TIMER_IMPORT + D2 agentId 映射）；④12h 惰性结算
 * （超时会话任意入口触碰即封顶 STOPPED + 720 分钟条目 + 封顶时刻反推 + 边界 720==720 不结算 +
 * PAUSED 态超时结算）；⑤config off 门控 + MANUAL 不受影响 + require-description 双路径；
 * ⑦审批链（isBillable→PENDING→approve/reject→重提；threshold 触发；auto-approve 直通；
 * 非法状态拒绝）；⑧三聚合口径断言。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsTicketTimerSession extends JunitAutoTestCase {

    @RegisterExtension
    static CsFrozenClockExtension frozenClock = new CsFrozenClockExtension();

    /** 冻结参考时刻：2026-07-17T00:00（CoreMetrics.currentDateTime() 在测试线程恒返回此值）。 */
    static final LocalDateTime NOW = LocalDate.of(2026, 7, 17).atStartOfDay();

    static final Long CUSTOMER_ID = 7501L;
    static final Long TICKET_TYPE_ID = 7601L;
    static final Long TICKET_A = 7101L;
    static final Long TICKET_B = 7102L;
    static final String AGENT_A = "1001";
    static final String AGENT_B = "1002";
    static final Long SESSION_ID = 7301L;
    static final Long ENTRY_ID_BASE = 7400L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    @BeforeEach
    public void setUpAgent() {
        setUser(AGENT_A);
    }

    @AfterEach
    public void restoreConfigs() {
        assign(ErpCsConstants.CONFIG_TIME_TRACKING_ENABLED, "true");
        assign(ErpCsConstants.CONFIG_TIME_ENTRY_REQUIRE_DESCRIPTION, "true");
        assign(ErpCsConstants.CONFIG_TIME_ENTRY_AUTO_APPROVE, "false");
    }

    // ---------- ① start→RUNNING + 单计时器守卫（应用路径 + DB UK 路径） + ⑥ 跨工单仍唯一 ----------

    @Test
    public void testStartCreatesRunningAndSingleTimerGuardAcrossTickets() {
        seedTicket(TICKET_A, null);
        seedTicket(TICKET_B, null);

        ApiResponse<?> resp = rpc(mutation, "ErpCsTicketTimerSession__startTimer",
                args("ticketId", TICKET_A));
        assertEquals(0, resp.getStatus(), "首次 startTimer 应成功: " + resp);
        ErpCsTicketTimerSession session = firstSession(AGENT_A);
        assertEquals(ErpCsConstants.TIMER_SESSION_STATUS_RUNNING, session.getStatus(), "会话 RUNNING");
        assertEquals(ErpCsConstants.TIMER_SESSION_ACTIVE_FLAG, session.getActiveFlag(), "进行中占位 activeFlag='Y'");
        assertEquals(AGENT_A, session.getAgentId(), "客服 = 操作者上下文 userId（D2）");
        assertEquals(TICKET_A, session.getTicketId(), "会话关联工单 A");
        assertEquals(Timestamp.valueOf(NOW), session.getStartTime(), "开始时间 = 冻结 now");
        assertTrue(session.getStopTime() == null, "未停止前 stopTime 为空");

        // ⑥ 同客服不同工单二次启动：按 D1-② 裁决服务端拒绝 + 专属错误码
        ApiResponse<?> second = rpc(mutation, "ErpCsTicketTimerSession__startTimer",
                args("ticketId", TICKET_B));
        assertEquals(ErpCsErrors.ERR_CS_TIMER_ALREADY_ACTIVE.getErrorCode(), second.getCode(),
                "跨工单二次启动应返回 ERR_CS_TIMER_ALREADY_ACTIVE（⑥）");

        // UK/应用守卫双路径之 DB 状态复查路径：dao 直插绕过管道的进行中行（agent B）→
        // startTimer 守卫按 DB 状态（非仅缓存/仅管道输入）拒绝（本项目 DDL 链仅物化 PK，应用守卫为运行时强制机制）
        ormTemplate.runInSession(this::insertDuplicateOpenSession);
        setUser(AGENT_B);
        ApiResponse<?> bypassGuard = rpc(mutation, "ErpCsTicketTimerSession__startTimer",
                args("ticketId", TICKET_A));
        assertEquals(ErpCsErrors.ERR_CS_TIMER_ALREADY_ACTIVE.getErrorCode(), bypassGuard.getCode(),
                "守卫复查 DB 状态：直插进行中行后 startTimer 仍拒绝（单活跃约束兜底）");
        setUser(AGENT_A);

        // 停止后槽位释放 + 历史行放行：stop → activeFlag NULL → 同客服可再启动（UK NULL 可重复语义）
        rpcOk(mutation, "ErpCsTicketTimerSession__stopTimer", args("sessionId", session.getId()));
        ApiResponse<?> restart = rpc(mutation, "ErpCsTicketTimerSession__startTimer",
                args("ticketId", TICKET_B));
        assertEquals(0, restart.getStatus(), "停止后重新 startTimer 应成功（槽位释放）: " + restart);
        assertEquals(TICKET_B, firstSession(AGENT_A).getTicketId(), "新会话关联工单 B");

        // 不同客服各自独立槽位（1002 已持有直插行，用第三客服 1003 验证）
        setUser("1003");
        ApiResponse<?> other = rpc(mutation, "ErpCsTicketTimerSession__startTimer",
                args("ticketId", TICKET_A));
        assertEquals(0, other.getStatus(), "另一客服可同时启动自己的计时器: " + other);
    }

    // ---------- ② pause/resume：cumulativePauseMinutes 累计 + pauseReason 可选 ----------

    @Test
    public void testPauseResumeAccumulatesPauseAndOptionalReason() {
        seedTicket(TICKET_A, null);
        // 路径 a) 应用入口暂停：RUNNING → PAUSED + pauseReason 可选（填写）
        Long running = seedRunningSession(7311L, AGENT_A, TICKET_A, NOW.minusHours(5), null);
        ApiResponse<?> paused = rpc(mutation, "ErpCsTicketTimerSession__pauseTimer",
                args("sessionId", running, "pauseReason", "等待客户回复"));
        assertEquals(0, paused.getStatus(), "pauseTimer 应成功: " + paused);
        ErpCsTicketTimerSession afterPause = reloadSession(running);
        assertEquals(ErpCsConstants.TIMER_SESSION_STATUS_PAUSED, afterPause.getStatus(), "会话 PAUSED");
        assertEquals(ErpCsConstants.TIMER_SESSION_ACTIVE_FLAG, afterPause.getActiveFlag(),
                "暂停仍占据单计时器槽位（D1：RUNNING 与 PAUSED 均进行中）");
        assertEquals("等待客户回复", afterPause.getPauseReason(), "暂停原因落库（可选字段填写路径）");
        assertNotNull(afterPause.getPauseStartDateTime(), "暂停开始时间记录");

        // 路径 b) 既有未闭合暂停 40 分钟 → resume 结算入累计（种子直插 PAUSED 会话，时间确定性）
        Long pausedSeed = seedPausedSession(7312L, AGENT_B, TICKET_A, NOW.minusHours(8), NOW.minusMinutes(40), 0);
        ApiResponse<?> resumed = rpc(mutation, "ErpCsTicketTimerSession__resumeTimer",
                args("sessionId", pausedSeed));
        assertEquals(0, resumed.getStatus(), "resumeTimer 应成功: " + resumed);
        ErpCsTicketTimerSession afterResume = reloadSession(pausedSeed);
        assertEquals(ErpCsConstants.TIMER_SESSION_STATUS_RUNNING, afterResume.getStatus(), "恢复后 RUNNING");
        assertEquals(40, afterResume.getCumulativePauseMinutes(), "未闭合暂停 40 分钟结算入累计（②数学断言）");
        assertNull(afterResume.getPauseStartDateTime(), "未闭合暂停清空");

        // pauseReason 不填路径（可选）+ 状态守卫：RUNNING 上 resume 非法（先恢复回 RUNNING 再试）
        Long running2 = seedRunningSession(7313L, AGENT_B, TICKET_A, NOW.minusMinutes(30), null);
        ApiResponse<?> paused2 = rpc(mutation, "ErpCsTicketTimerSession__pauseTimer",
                args("sessionId", running2));
        assertEquals(0, paused2.getStatus(), "不填暂停原因的 pauseTimer 应成功（可选）: " + paused2);
        assertEquals(0, rpc(mutation, "ErpCsTicketTimerSession__resumeTimer",
                args("sessionId", running2)).getStatus(), "PAUSED 会话 resume 应成功");
        ApiResponse<?> badResume = rpc(mutation, "ErpCsTicketTimerSession__resumeTimer",
                args("sessionId", running2));
        assertEquals(ErpCsErrors.ERR_CS_TIMER_ILLEGAL_STATE.getErrorCode(), badResume.getCode(),
                "RUNNING 会话再次 resume 应拒绝（非法状态）");
    }

    // ---------- ③ stop → ErpCsTimeEntry 生成（duration = 墙钟 − Σ暂停 数学断言 + source=TIMER_IMPORT + D2 映射） ----------

    @Test
    public void testStopGeneratesEntryWithDurationMath() {
        seedTicket(TICKET_A, null);
        // 墙钟 300 分钟 − 累计暂停 45 分钟 = 有效 255 分钟
        Long sessionId = seedRunningSession(SESSION_ID, AGENT_A, TICKET_A, NOW.minusMinutes(300), 45);

        ApiResponse<?> resp = rpc(mutation, "ErpCsTicketTimerSession__stopTimer",
                args("sessionId", sessionId));
        assertEquals(0, resp.getStatus(), "stopTimer 应成功: " + resp);
        ErpCsTicketTimerSession stopped = reloadSession(sessionId);
        assertEquals(ErpCsConstants.TIMER_SESSION_STATUS_STOPPED, stopped.getStatus(), "会话 STOPPED");
        assertNull(stopped.getActiveFlag(), "停止释放单计时器槽位（activeFlag=NULL）");
        assertEquals(Timestamp.valueOf(NOW), stopped.getStopTime(), "停止时间 = now");

        List<ErpCsTimeEntry> entries = entriesOf(TICKET_A);
        assertEquals(1, entries.size(), "停止生成恰好 1 条 ErpCsTimeEntry（④）");
        ErpCsTimeEntry entry = entries.get(0);
        assertEquals(TICKET_A, entry.getTicketId(), "条目关联工单");
        assertEquals(Long.parseLong(AGENT_A), entry.getAgentId(), "D2 映射：数字 userId 直写 BIGINT");
        assertEquals(Timestamp.valueOf(NOW.minusMinutes(300)), entry.getStartTime(), "条目 startTime = 会话开始时间");
        assertEquals(Timestamp.valueOf(NOW), entry.getEndTime(), "条目 endTime = 停止时间");
        assertEquals(255, entry.getDuration(), "duration = 墙钟 300 − Σ暂停 45 = 255（④数学断言）");
        assertEquals(ErpCsConstants.TIME_ENTRY_SOURCE_TIMER_IMPORT, entry.getSource(), "source=TIMER_IMPORT");
        assertNull(entry.getApprovalStatus(), "approvalStatus=NULL 即 DRAFT（待客服补充后 submit，plan D4）");
        assertEquals(Boolean.TRUE, entry.getIsBillable(), "默认可计费（owner doc §1.1 默认 true）");

        // 已停止会话再 stop → 拒绝
        ApiResponse<?> again = rpc(mutation, "ErpCsTicketTimerSession__stopTimer",
                args("sessionId", sessionId));
        assertEquals(ErpCsErrors.ERR_CS_TIMER_SESSION_NOT_OPEN.getErrorCode(), again.getCode(),
                "STOPPED 会话再 stop 应拒绝");
    }

    // ---------- ④ 12h 惰性结算：任意入口触碰即封顶 + 封顶时刻反推 + 边界 + PAUSED 态超时 ----------

    @Test
    public void testTwelveHourLazySettlementOnAnyTouch() {
        seedTicket(TICKET_A, null);
        // a) 超 12h（有效 840 分钟）→ pause 入口先封顶结算再拒绝暂停
        Long overdue = seedRunningSession(7321L, AGENT_A, TICKET_A, NOW.minusHours(14), 0);
        ApiResponse<?> pauseResp = rpc(mutation, "ErpCsTicketTimerSession__pauseTimer",
                args("sessionId", overdue));
        assertEquals(ErpCsErrors.ERR_CS_TIMER_SESSION_NOT_OPEN.getErrorCode(), pauseResp.getCode(),
                "超时会话 pause 触发结算后按已停止拒绝");
        ErpCsTicketTimerSession settled = reloadSession(7321L);
        assertEquals(ErpCsConstants.TIMER_SESSION_STATUS_STOPPED, settled.getStatus(), "惰性结算置 STOPPED");
        assertNull(settled.getActiveFlag(), "结算释放槽位");
        assertEquals(Timestamp.valueOf(NOW.minusHours(14).plusMinutes(720)), settled.getStopTime(),
                "封顶停止时刻 = startTime + 720 分钟（反推，plan D3）");
        ErpCsTimeEntry capped = entriesOf(TICKET_A).get(0);
        assertEquals(720, capped.getDuration(), "封顶条目 duration=720（12h 上限）");
        assertEquals(Timestamp.valueOf(NOW.minusHours(14).plusMinutes(720)), capped.getEndTime(),
                "封顶条目 endTime = 封顶停止时刻");
        assertEquals(Timestamp.valueOf(NOW.minusHours(14)), capped.getStartTime(), "封顶条目 startTime = 会话开始");

        // b) findActiveTimer 读取入口：超时会话封顶后返回 null
        Long overdue2 = seedRunningSession(7322L, AGENT_A, TICKET_A, NOW.minusHours(13).minusMinutes(30), 0);
        ApiResponse<?> active = rpc(query, "ErpCsTicketTimerSession__findActiveTimer",
                args("agentId", AGENT_A));
        assertEquals(0, active.getStatus(), "findActiveTimer 应成功: " + active);
        assertNull(active.getData(), "超时会话读取时惰性结算后无活跃计时器（返回 null）");
        assertEquals(ErpCsConstants.TIMER_SESSION_STATUS_STOPPED, reloadSession(7322L).getStatus(),
                "读取入口完成封顶结算");
        assertEquals(720, entriesOf(TICKET_A).get(1).getDuration(), "第二笔封顶条目 720 分钟");

        // c) 边界：有效恰好 720 分钟（13h 墙钟 − 60 暂停）不触发结算，正常停止 duration=720
        Long boundary = seedPausedSession(7323L, AGENT_A, TICKET_A, NOW.minusHours(13), NOW.minusMinutes(1), 59);
        ApiResponse<?> stopBoundary = rpc(mutation, "ErpCsTicketTimerSession__stopTimer",
                args("sessionId", boundary));
        assertEquals(0, stopBoundary.getStatus(), "边界会话（有效=720）stop 应成功: " + stopBoundary);
        assertEquals(720, entriesOf(TICKET_A).get(2).getDuration(),
                "边界（未超 720）正常停止 duration = 780 − 60 = 720");

        // d) PAUSED 态超时（墙钟 14h，暂停中 1h → 有效 780 > 720）：stop 入口结算幂等返回
        Long pausedOverdue = seedPausedSession(7324L, AGENT_A, TICKET_A, NOW.minusHours(14), NOW.minusHours(1), 0);
        ApiResponse<?> stopOverdue = rpc(mutation, "ErpCsTicketTimerSession__stopTimer",
                args("sessionId", pausedOverdue));
        assertEquals(0, stopOverdue.getStatus(), "PAUSED 超时 stop 经结算幂等成功: " + stopOverdue);
        ErpCsTicketTimerSession settledPaused = reloadSession(7324L);
        assertEquals(ErpCsConstants.TIMER_SESSION_STATUS_STOPPED, settledPaused.getStatus(), "PAUSED 超时结算 STOPPED");
        assertEquals(60, settledPaused.getCumulativePauseMinutes(), "未闭合暂停 60 分钟结算入累计");
        assertEquals(Timestamp.valueOf(NOW.minusHours(14).plusMinutes(720).plusMinutes(60)),
                settledPaused.getStopTime(), "PAUSED 态封顶时刻 = startTime + 720 + 总暂停");
        assertEquals(720, entriesOf(TICKET_A).get(3).getDuration(), "PAUSED 态封顶条目仍 720 分钟");
    }

    // ---------- ⑤ config 门控：off 拒绝计时器 + MANUAL 不受影响 + require-description 双路径 ----------

    @Test
    public void testConfigGatingAndRequireDescription() {
        seedTicket(TICKET_A, null);
        assign(ErpCsConstants.CONFIG_TIME_TRACKING_ENABLED, "false");

        ApiResponse<?> start = rpc(mutation, "ErpCsTicketTimerSession__startTimer",
                args("ticketId", TICKET_A));
        assertEquals(ErpCsErrors.ERR_CS_TIME_TRACKING_DISABLED.getErrorCode(), start.getCode(),
                "config off 时 startTimer 拒绝（①前置门控）");

        Long session = seedRunningSession(7331L, AGENT_A, TICKET_A, NOW.minusMinutes(30), 0);
        assertEquals(ErpCsErrors.ERR_CS_TIME_TRACKING_DISABLED.getErrorCode(),
                rpc(mutation, "ErpCsTicketTimerSession__pauseTimer", args("sessionId", session)).getCode(),
                "config off 时 pauseTimer 拒绝");
        assertEquals(ErpCsErrors.ERR_CS_TIME_TRACKING_DISABLED.getErrorCode(),
                rpc(mutation, "ErpCsTicketTimerSession__stopTimer", args("sessionId", session)).getCode(),
                "config off 时 stopTimer 拒绝");

        // MANUAL 条目 CRUD + submit 不受计时开关影响（plan D6：开关仅门控计时器）
        ApiResponse<?> manualSave = rpc(mutation, "ErpCsTimeEntry__save", Map.of("data", manualEntryData(null, "手工补录", 30)));
        assertEquals(0, manualSave.getStatus(), "MANUAL 条目保存不受计时开关影响: " + manualSave);
        Long manualId = idOf(manualSave);
        ApiResponse<?> manualSubmit = rpc(mutation, "ErpCsTimeEntry__submit", args("timeEntryId", manualId));
        assertEquals(0, manualSubmit.getStatus(), "MANUAL 条目 submit 不受计时开关影响: " + manualSubmit);
        assertEquals(ErpCsConstants.TIME_ENTRY_APPROVE_APPROVED,
                reloadEntry(manualId).getApprovalStatus(), "不可计费 30 分钟不触发审批直通 APPROVED");

        assign(ErpCsConstants.CONFIG_TIME_TRACKING_ENABLED, "true");

        // require-description 双路径：true + 空 description → 拒绝；false → 放行
        Long noDesc = seedManualEntry(7341L, TICKET_A, "", false, 30);
        assertEquals(ErpCsErrors.ERR_CS_TIME_ENTRY_DESCRIPTION_REQUIRED.getErrorCode(),
                rpc(mutation, "ErpCsTimeEntry__submit", args("timeEntryId", noDesc)).getCode(),
                "require-description=true 且空描述 → submit 拒绝");
        assign(ErpCsConstants.CONFIG_TIME_ENTRY_REQUIRE_DESCRIPTION, "false");
        assertEquals(0, rpc(mutation, "ErpCsTimeEntry__submit", args("timeEntryId", noDesc)).getStatus(),
                "require-description=false → 空描述 submit 放行");
        assertEquals(ErpCsConstants.TIME_ENTRY_APPROVE_APPROVED, reloadEntry(noDesc).getApprovalStatus(),
                "不可计费未超阈值直通 APPROVED");
    }

    // ---------- ⑦ 审批链：isBillable→PENDING→approve；reject→修改重提；threshold；auto-approve；非法状态 ----------

    @Test
    public void testApprovalChainSubmitApproveRejectResubmit() {
        seedTicket(TICKET_A, AGENT_B);

        // isBillable → PENDING → approve → APPROVED
        Long billable = seedManualEntry(7351L, TICKET_A, "远程排查故障", true, 60);
        assertEquals(0, rpc(mutation, "ErpCsTimeEntry__submit", args("timeEntryId", billable)).getStatus(),
                "可计费条目 submit 成功");
        assertEquals(ErpCsConstants.TIME_ENTRY_APPROVE_PENDING, reloadEntry(billable).getApprovalStatus(),
                "可计费条目自动进入审批（⑥）");
        assertEquals(0, rpc(mutation, "ErpCsTimeEntry__approve", args("timeEntryId", billable)).getStatus(),
                "approve 成功");
        ErpCsTimeEntry approved = reloadEntry(billable);
        assertEquals(ErpCsConstants.TIME_ENTRY_APPROVE_APPROVED, approved.getApprovalStatus(), "审批通过（⑦前置）");
        assertEquals(AGENT_A, approved.getApprovedById(), "审批人 = 操作者");
        assertNotNull(approved.getApprovedAt(), "审批时间落库");

        // PENDING → reject（原因前缀）→ REJECTED → 修改后重新 submit
        Long rejected = seedManualEntry(7352L, TICKET_A, "现场支持", true, 90);
        rpcOk(mutation, "ErpCsTimeEntry__submit", args("timeEntryId", rejected));
        assertEquals(0, rpc(mutation, "ErpCsTimeEntry__reject",
                args("timeEntryId", rejected, "rejectReason", "时长与工单记录不符")).getStatus(), "reject 成功");
        ErpCsTimeEntry rejectedEntry = reloadEntry(rejected);
        assertEquals(ErpCsConstants.TIME_ENTRY_APPROVE_REJECTED, rejectedEntry.getApprovalStatus(), "驳回");
        assertTrue(rejectedEntry.getDescription().startsWith("[驳回] 时长与工单记录不符;"),
                "驳回原因追加 description 前缀（plan D4）");
        ormTemplate.runInSession(() -> {
            ErpCsTimeEntry managed = daoProvider.daoFor(ErpCsTimeEntry.class).getEntityById(rejected);
            managed.setDescription("现场支持（修订：60 分钟）");
            daoProvider.daoFor(ErpCsTimeEntry.class).updateEntity(managed);
        });
        assertEquals(0, rpc(mutation, "ErpCsTimeEntry__submit", args("timeEntryId", rejected)).getStatus(),
                "REJECTED 修改后重新 submit 成功（§3.2 状态机）");
        assertEquals(ErpCsConstants.TIME_ENTRY_APPROVE_PENDING, reloadEntry(rejected).getApprovalStatus(),
                "重提后回到 PENDING");

        // threshold 触发：不可计费但 500 分钟 > 480 → PENDING
        Long overThreshold = seedManualEntry(7353L, TICKET_A, "大工单处理", false, 500);
        rpcOk(mutation, "ErpCsTimeEntry__submit", args("timeEntryId", overThreshold));
        assertEquals(ErpCsConstants.TIME_ENTRY_APPROVE_PENDING, reloadEntry(overThreshold).getApprovalStatus(),
                "超阈值（500>480）不可计费仍触发审批（⑥）");

        // 非法状态：APPROVED 再 approve / PENDING 再 submit → 拒绝
        assertEquals(ErpCsErrors.ERR_CS_TIME_ENTRY_ILLEGAL_APPROVAL_STATUS.getErrorCode(),
                rpc(mutation, "ErpCsTimeEntry__approve", args("timeEntryId", billable)).getCode(),
                "APPROVED 再 approve 拒绝");
        assertEquals(ErpCsErrors.ERR_CS_TIME_ENTRY_ILLEGAL_APPROVAL_STATUS.getErrorCode(),
                rpc(mutation, "ErpCsTimeEntry__submit", args("timeEntryId", overThreshold)).getCode(),
                "PENDING 再 submit 拒绝");

        // auto-approve 直通：可计费也跳过 PENDING
        assign(ErpCsConstants.CONFIG_TIME_ENTRY_AUTO_APPROVE, "true");
        Long autoEntry = seedManualEntry(7354L, TICKET_A, "自动审批路径", true, 60);
        rpcOk(mutation, "ErpCsTimeEntry__submit", args("timeEntryId", autoEntry));
        assertEquals(ErpCsConstants.TIME_ENTRY_APPROVE_APPROVED, reloadEntry(autoEntry).getApprovalStatus(),
                "auto-approve=true 可计费直通 APPROVED");
    }

    // ---------- ⑧ 三聚合口径（totalTimeSpent 含 PENDING/APPROVED；billable 仅 APPROVED+isBillable） ----------

    @Test
    public void testTicketTimeAggregations() {
        seedTicket(TICKET_A, null);
        seedTicket(TICKET_B, null);
        seedManualEntrySpec(7361L, TICKET_A, 100, true, ErpCsConstants.TIME_ENTRY_APPROVE_APPROVED, "50.00");
        seedManualEntrySpec(7362L, TICKET_A, 60, true, ErpCsConstants.TIME_ENTRY_APPROVE_PENDING, "30.00");
        seedManualEntrySpec(7363L, TICKET_A, 40, false, ErpCsConstants.TIME_ENTRY_APPROVE_APPROVED, "999.00");
        seedManualEntrySpec(7364L, TICKET_A, 25, true, ErpCsConstants.TIME_ENTRY_APPROVE_REJECTED, "12.50");
        seedManualEntrySpec(7365L, TICKET_A, 10, true, null, "5.00");
        seedManualEntrySpec(7366L, TICKET_B, 999, true, ErpCsConstants.TIME_ENTRY_APPROVE_APPROVED, "1.00");

        assertEquals(200L, agg("ErpCsTicket__totalTimeSpent", TICKET_A),
                "totalTimeSpent = APPROVED(100) + PENDING(60) + APPROVED不可计费(40)，REJECTED/DRAFT/他单排除");
        assertEquals(100L, agg("ErpCsTicket__totalBillableTime", TICKET_A),
                "totalBillableTime 仅 APPROVED+isBillable（100）");
        assertEquals(0, new BigDecimal("50.00").compareTo(aggAmount("ErpCsTicket__totalBilledAmount", TICKET_A)),
                "totalBilledAmount = SUM(billableAmount) isBillable+APPROVED（50.00）");
        assertEquals(999L, agg("ErpCsTicket__totalTimeSpent", TICKET_B), "他单聚合独立");
    }

    // ---------- helpers ----------

    private void setUser(String userId) {
        ContextProvider.getOrCreateContext().setUserId(userId);
        ContextProvider.getOrCreateContext().setUserName(userId);
    }

    private void assign(String key, String value) {
        AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private Map<String, Object> args(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            m.put((String) kv[i], kv[i + 1]);
        }
        return m;
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                               Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }

    private void rpcOk(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                       Map<String, Object> args) {
        ApiResponse<?> resp = rpc(op, action, args);
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
    }

    private Long idOf(ApiResponse<?> resp) {
        Object data = resp.getData();
        assertNotNull(data, "响应应含实体: " + resp);
        @SuppressWarnings("unchecked")
        Map<String, Object> map = (Map<String, Object>) data;
        Object id = map.get("id");
        return id instanceof Number ? ((Number) id).longValue() : Long.valueOf(String.valueOf(id));
    }

    private long agg(String action, Long ticketId) {
        ApiResponse<?> resp = rpc(query, action, args("ticketId", ticketId));
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
        Object data = resp.getData();
        return data instanceof Number ? ((Number) data).longValue() : Long.parseLong(String.valueOf(data));
    }

    private BigDecimal aggAmount(String action, Long ticketId) {
        ApiResponse<?> resp = rpc(query, action, args("ticketId", ticketId));
        assertEquals(0, resp.getStatus(), action + " 应成功: " + resp);
        return new BigDecimal(String.valueOf(resp.getData()));
    }

    private ErpCsTicketTimerSession reloadSession(Long id) {
        return daoProvider.daoFor(ErpCsTicketTimerSession.class).getEntityById(id);
    }

    private ErpCsTimeEntry reloadEntry(Long id) {
        return daoProvider.daoFor(ErpCsTimeEntry.class).getEntityById(id);
    }

    private ErpCsTicketTimerSession firstSession(String agentId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("agentId", agentId));
        q.addFilter(eq("activeFlag", ErpCsConstants.TIMER_SESSION_ACTIVE_FLAG));
        q.setLimit(1);
        List<ErpCsTicketTimerSession> list = daoProvider.daoFor(ErpCsTicketTimerSession.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private List<ErpCsTimeEntry> entriesOf(Long ticketId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.addOrderField("id", false);
        return daoProvider.daoFor(ErpCsTimeEntry.class).findAllByQuery(q);
    }

    /** 直插绕过管道的进行中行（AGENT_B，flush 落库）——守卫 DB 状态复查路径的种子。 */
    private void insertDuplicateOpenSession() {
        IEntityDao<ErpCsTicketTimerSession> dao = daoProvider.daoFor(ErpCsTicketTimerSession.class);
        ErpCsTicketTimerSession dup = dao.newEntity();
        dup.orm_propValueByName("id", 7302L);
        dup.setAgentId(AGENT_B);
        dup.setTicketId(TICKET_B);
        dup.setStartTime(Timestamp.valueOf(NOW.minusMinutes(10)));
        dup.setStatus(ErpCsConstants.TIMER_SESSION_STATUS_RUNNING);
        dup.setActiveFlag(ErpCsConstants.TIMER_SESSION_ACTIVE_FLAG);
        dao.saveEntity(dup);
        dao.flushSession();
    }

    private void seedTicket(Long id, String assignedToId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
            ErpCsTicket t = dao.newEntity();
            t.orm_propValueByName("id", id);
            t.setCode("TK-TIMER-" + id);
            t.setSubject("计时器测试工单-" + id);
            t.setCustomerId(CUSTOMER_ID);
            t.setTicketTypeId(TICKET_TYPE_ID);
            t.setPriority(ErpCsConstants.TICKET_PRIORITY_NORMAL);
            t.setStatus(ErpCsConstants.TICKET_STATUS_IN_PROGRESS);
            t.setDocStatus(ErpCsConstants.DOC_STATUS_ACTIVE);
            t.setApproveStatus(ErpCsConstants.APPROVE_STATUS_UNSUBMITTED);
            t.setIsSlaCompleted(false);
            t.setAssignedToId(assignedToId);
            t.setBusinessDate(LocalDate.of(2026, 7, 16));
            dao.saveEntity(t);
        });
        seedCustomerOnce();
    }

    private void seedCustomerOnce() {
        ormTemplate.runInSession(() -> {
            IEntityDao<app.erp.md.dao.entity.ErpMdPartner> dao =
                    daoProvider.daoFor(app.erp.md.dao.entity.ErpMdPartner.class);
            if (dao.getEntityById(CUSTOMER_ID) != null) {
                return;
            }
            app.erp.md.dao.entity.ErpMdPartner p = dao.newEntity();
            p.orm_propValueByName("id", CUSTOMER_ID);
            p.setCode("CUS-TIMER-" + CUSTOMER_ID);
            p.setName("计时器测试客户");
            p.orm_propValueByName("partnerType", "CUSTOMER");
            p.orm_propValueByName("status", "ACTIVE");
            dao.saveEntity(p);
        });
    }

    /** dao 直插 RUNNING 会话（时间受控种子；cumulative 可预置暂停历史）。 */
    private Long seedRunningSession(Long id, String agentId, Long ticketId, LocalDateTime startTime,
                                    Integer cumulativePauseMinutes) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicketTimerSession> dao = daoProvider.daoFor(ErpCsTicketTimerSession.class);
            ErpCsTicketTimerSession s = dao.newEntity();
            s.orm_propValueByName("id", id);
            s.setAgentId(agentId);
            s.setTicketId(ticketId);
            s.setStartTime(Timestamp.valueOf(startTime));
            s.setCumulativePauseMinutes(cumulativePauseMinutes == null ? 0 : cumulativePauseMinutes);
            s.setStatus(ErpCsConstants.TIMER_SESSION_STATUS_RUNNING);
            s.setActiveFlag(ErpCsConstants.TIMER_SESSION_ACTIVE_FLAG);
            dao.saveEntity(s);
        });
        return id;
    }

    /** dao 直插 PAUSED 会话（含未闭合暂停）。 */
    private Long seedPausedSession(Long id, String agentId, Long ticketId, LocalDateTime startTime,
                                   LocalDateTime pauseStart, Integer cumulativePauseMinutes) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicketTimerSession> dao = daoProvider.daoFor(ErpCsTicketTimerSession.class);
            ErpCsTicketTimerSession s = dao.newEntity();
            s.orm_propValueByName("id", id);
            s.setAgentId(agentId);
            s.setTicketId(ticketId);
            s.setStartTime(Timestamp.valueOf(startTime));
            s.setPauseStartDateTime(Timestamp.valueOf(pauseStart));
            s.setCumulativePauseMinutes(cumulativePauseMinutes == null ? 0 : cumulativePauseMinutes);
            s.setStatus(ErpCsConstants.TIMER_SESSION_STATUS_PAUSED);
            s.setActiveFlag(ErpCsConstants.TIMER_SESSION_ACTIVE_FLAG);
            dao.saveEntity(s);
        });
        return id;
    }

    private Map<String, Object> manualEntryData(Long id, String description, Integer duration) {
        Map<String, Object> data = new LinkedHashMap<>();
        if (id != null) {
            data.put("id", id);
        }
        data.put("ticketId", TICKET_A);
        data.put("agentId", Long.parseLong(AGENT_A));
        data.put("startTime", Timestamp.valueOf(NOW.minusMinutes(duration == null ? 0 : duration)));
        data.put("endTime", Timestamp.valueOf(NOW));
        data.put("duration", duration);
        data.put("isBillable", false);
        data.put("description", description);
        data.put("source", ErpCsConstants.TIME_ENTRY_SOURCE_MANUAL);
        return data;
    }

    private Long seedManualEntry(Long id, Long ticketId, String description, boolean billable, int duration) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTimeEntry> dao = daoProvider.daoFor(ErpCsTimeEntry.class);
            ErpCsTimeEntry e = dao.newEntity();
            e.orm_propValueByName("id", id);
            e.setTicketId(ticketId);
            e.setAgentId(Long.parseLong(AGENT_A));
            e.setStartTime(Timestamp.valueOf(NOW.minusMinutes(duration)));
            e.setEndTime(Timestamp.valueOf(NOW));
            e.setDuration(duration);
            e.setIsBillable(billable);
            e.setDescription(description);
            e.setSource(ErpCsConstants.TIME_ENTRY_SOURCE_MANUAL);
            dao.saveEntity(e);
        });
        return id;
    }

    private void seedManualEntrySpec(Long id, Long ticketId, int duration, boolean billable,
                                     String approvalStatus, String billableAmount) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTimeEntry> dao = daoProvider.daoFor(ErpCsTimeEntry.class);
            ErpCsTimeEntry e = dao.newEntity();
            e.orm_propValueByName("id", id);
            e.setTicketId(ticketId);
            e.setAgentId(Long.parseLong(AGENT_A));
            e.setStartTime(Timestamp.valueOf(NOW.minusMinutes(duration)));
            e.setEndTime(Timestamp.valueOf(NOW));
            e.setDuration(duration);
            e.setIsBillable(billable);
            e.setDescription("聚合口径条目-" + id);
            e.setApprovalStatus(approvalStatus);
            e.setBillableAmount(new BigDecimal(billableAmount));
            e.setSource(ErpCsConstants.TIME_ENTRY_SOURCE_MANUAL);
            dao.saveEntity(e);
        });
    }
}
