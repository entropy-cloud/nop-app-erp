package app.erp.cs.service;

import app.erp.common.test.ThreadLocalFrozenClock;
import app.erp.cs.dao.entity.ErpCsSlaPolicy;
import app.erp.cs.dao.entity.ErpCsTicket;
import app.erp.cs.dao.entity.ErpCsTicketAction;
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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * SLA 多级升级链端到端测试（RC-R1.67，P1-RC-056，UC-CS-04 ⑩；
 * plan 2026-08-17-2125-3 Phase 3 测试组 ①-⑪）。
 *
 * <p>冻结时钟（2026-07-17T00:00）+ {@link ThreadLocalFrozenClock#install(LocalDate)} 按天推进窗口，
 * 驱动升级链全语义确定性。断言式测试 + 空 autotest.yaml 标记（镜像 R1.65/R1.66 范式——
 * 时间敏感字段经冻结时钟确定性后以行为断言为主，不录制表快照）。
 *
 * <p>覆盖：①首次超时 L1（count=1/level=1/lastEscalationAt 落库 + 通知目标 = policy.escalationUserId
 * 漂移修正断言）；②窗口内重复扫描零新增（R1.28 幂等保持）；③窗口后重复通知 count=2→3→4（重复 ×3
 * 达 D1「最多 3 次」上限）；④count=4 达上限 → L2；⑤secondEscalationUserId=null 跳级直达 L3；
 * ⑥L2 窗口后 L3 总监 + level=3 封顶零动作；⑦l3-user-id 空 → L3 跳过（WARN + 窗口推进，零审计零通知）；
 * ⑧policy.escalationDelayHours 覆盖 config 默认；⑨resolve 后（isSlaCompleted=true）不命中查询；
 * ⑩_cases/ 空 autotest.yaml 标记；⑪存量工单兼容（既有 ESCALATE 审计行 + 计数器 null → 重新进入
 * L1 count=1 + 新增审计行）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsMultiLevelEscalation extends JunitAutoTestCase {

    @RegisterExtension
    static CsFrozenClockExtension frozenClock = new CsFrozenClockExtension();

    /** 冻结参考时刻：2026-07-17T00:00（CoreMetrics.currentDateTime() 在测试线程恒返回此值）。 */
    static final LocalDateTime NOW = LocalDate.of(2026, 7, 17).atStartOfDay();

    static final Long CUSTOMER_ID = 7701L;
    static final Long TICKET_TYPE_ID = 7801L;
    static final Long POLICY_ID = 7901L;
    static final Long ESCALATION_USER = 9001L;      // policy L1 目标（BIGINT → stringify "9001"）
    static final Long SECOND_USER = 9002L;          // policy L2 目标（→ "9002"）
    static final String L3_DIRECTOR = "9003";       // config 总监（字符串 userId）
    static final String ASSIGNEE = "user-zhang";    // assignedToId 回退目标
    static final Long TICKET_ID = 7201L;
    static final Long TEMPLATE_ID = 7011L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;

    /** 时钟推进偏移（天）：advanceDays 累计推进，@AfterEach 复位。 */
    private long clockOffsetDays;

    @AfterEach
    public void restoreClockAndConfigs() {
        clockOffsetDays = 0;
        ThreadLocalFrozenClock.install(CsFrozenClockExtension.REFERENCE_DATE);
        assign(ErpCsConstants.CONFIG_ESCALATION_L3_USER_ID, "");
    }

    // ---------- ① 首次超时 L1 + 漂移修正断言 + ② 窗口内重复扫描幂等 ----------

    @Test
    public void testFirstOverdueL1PolicyTargetAndInWindowIdempotency() {
        seedTemplate(TEMPLATE_ID);
        seedPolicy(POLICY_ID, ESCALATION_USER, SECOND_USER, null);
        seedTicket(TICKET_ID, "TK-ESC-L1", NOW.minusHours(2), POLICY_ID);

        scanOk();
        ErpCsTicket t = reload(TICKET_ID);
        assertEquals(1, t.getLastEscalationLevel(), "首次升级 level=1");
        assertEquals(1, t.getEscalationCount(), "首次升级 count=1");
        assertEquals(Timestamp.valueOf(NOW), t.getLastEscalationAt(), "lastEscalationAt 落库 = 冻结 now");
        assertEquals(1, countEscalateActions(TICKET_ID), "首次扫描 1 条 ESCALATE 审计");
        ErpCsTicketAction action = lastEscalateAction(TICKET_ID);
        assertEquals("SLA 超时升级 L1（第 1 次）通知 " + ESCALATION_USER, action.getContent(),
                "审计 content 承载级别/次数/目标（plan D5）");
        // 漂移修正断言：通知目标 = policy.escalationUserId（stringify），非 assignedToId
        ErpSysNotification n = lastNotification();
        assertNotNull(n, "L1 通知行落入");
        assertEquals(String.valueOf(ESCALATION_USER), n.getRecipientUserId(),
                "L1 通知目标 = policy.escalationUserId（漂移修正，非 assignedToId）");

        // ② 窗口内（<2h）重复扫描：零新增审计 + 计数器不变（R1.28 幂等保持）
        scanOk();
        assertEquals(1, countEscalateActions(TICKET_ID), "窗口内重复扫描零新增 ESCALATE");
        assertEquals(1, reload(TICKET_ID).getEscalationCount(), "窗口内计数器不变");
    }

    // ---------- ③ 重复通知至上限 + ④ L2 + ⑥ L3 封顶（全链） ----------

    @Test
    public void testRepeatUpToCapThenL2ThenL3Capped() {
        assign(ErpCsConstants.CONFIG_ESCALATION_L3_USER_ID, L3_DIRECTOR);
        seedTemplate(TEMPLATE_ID);
        seedPolicy(POLICY_ID, ESCALATION_USER, SECOND_USER, null);
        seedTicket(TICKET_ID, "TK-ESC-CHAIN", NOW.minusHours(2), POLICY_ID);

        // t0：L1（count=1）
        scanOk();
        assertEquals(1, reload(TICKET_ID).getEscalationCount());
        assertEquals(String.valueOf(ESCALATION_USER), lastNotification().getRecipientUserId(),
                "L1 通知 → policy.escalationUserId");

        // ③ 重复通知 ×3：count=2→3→4（每窗口后一次）
        for (int expectedCount = 2; expectedCount <= 4; expectedCount++) {
            advanceDays(1);
            scanOk();
            ErpCsTicket t = reload(TICKET_ID);
            assertEquals(1, t.getLastEscalationLevel(), "重复窗口内 level 保持 1");
            assertEquals(expectedCount, t.getEscalationCount(),
                    "窗口后重复通知 count=" + expectedCount + "（重复 ×3 达 D1 上限）");
            assertEquals(String.valueOf(ESCALATION_USER), lastNotification().getRecipientUserId(),
                    "重复通知目标仍为 escalationUserId");
        }

        // ④ count=4（1+max-repeat=1+3）达上限 → 下一窗口 L2
        advanceDays(1);
        scanOk();
        ErpCsTicket afterL2 = reload(TICKET_ID);
        assertEquals(2, afterL2.getLastEscalationLevel(), "count 达上限后升级 L2");
        assertEquals(5, afterL2.getEscalationCount(), "L2 计数顺延 count=5");
        assertEquals(String.valueOf(SECOND_USER), lastNotification().getRecipientUserId(),
                "L2 通知 → policy.secondEscalationUserId");

        // ⑥ L2 窗口后 → L3 总监（config 载体）+ level=3
        advanceDays(1);
        scanOk();
        ErpCsTicket afterL3 = reload(TICKET_ID);
        assertEquals(3, afterL3.getLastEscalationLevel(), "L2 窗口后升级 L3");
        assertEquals(6, afterL3.getEscalationCount(), "L3 计数顺延 count=6");
        assertEquals(L3_DIRECTOR, lastNotification().getRecipientUserId(), "L3 通知 → config 总监");

        // ⑥ 封顶：后续窗口扫描零动作（resolve 前不再升级）
        int actionsBefore = countEscalateActions(TICKET_ID);
        advanceDays(1);
        advanceDays(1);
        scanOk();
        assertEquals(actionsBefore, countEscalateActions(TICKET_ID), "level=3 封顶后零新增审计");
        assertEquals(3, reload(TICKET_ID).getLastEscalationLevel(), "level 保持 3");
    }

    // ---------- ⑤ secondEscalationUserId=null → 跳级直达 L3 ----------

    @Test
    public void testSecondNullSkipsToL3() {
        assign(ErpCsConstants.CONFIG_ESCALATION_L3_USER_ID, L3_DIRECTOR);
        seedTemplate(TEMPLATE_ID);
        seedPolicy(POLICY_ID, ESCALATION_USER, null, null);
        seedTicket(TICKET_ID, "TK-ESC-SKIP", NOW.minusHours(2), POLICY_ID);

        // 快进到 count=4（L1 + 3 次重复）
        for (int i = 0; i < 4; i++) {
            scanOk();
            if (i < 3) {
                advanceDays(1);
            }
        }
        assertEquals(1, reload(TICKET_ID).getLastEscalationLevel(), "count=4 时仍在 L1");
        assertEquals(4, reload(TICKET_ID).getEscalationCount());

        // 下一窗口：second=null → 跳级直达 L3（level 1→3，通知总监）
        advanceDays(1);
        scanOk();
        ErpCsTicket t = reload(TICKET_ID);
        assertEquals(3, t.getLastEscalationLevel(), "second=null 跳级直达 L3（无 L2 停留）");
        assertEquals(5, t.getEscalationCount(), "跳级仍推进计数");
        assertEquals(L3_DIRECTOR, lastNotification().getRecipientUserId(), "跳级通知 → config 总监");
        assertNull(notificationTo(String.valueOf(SECOND_USER)), "无 L2 通知行（second 未配置）");
    }

    // ---------- ⑦ l3-user-id 空 → L3 跳过（WARN + 窗口推进；零审计零通知） ----------

    @Test
    public void testL3ConfigEmptyWarnsAndSkips() {
        seedTemplate(TEMPLATE_ID);
        seedPolicy(POLICY_ID, ESCALATION_USER, SECOND_USER, null);
        seedTicket(TICKET_ID, "TK-ESC-L3EMPTY", NOW.minusHours(2), POLICY_ID);

        // 快进到 L2（t0 L1 + 3 重复 + L2）
        for (int i = 0; i < 5; i++) {
            scanOk();
            if (i < 4) {
                advanceDays(1);
            }
        }
        assertEquals(2, reload(TICKET_ID).getLastEscalationLevel(), "已至 L2");
        int actionsAtL2 = countEscalateActions(TICKET_ID);
        Timestamp windowAtL2 = reload(TICKET_ID).getLastEscalationAt();

        // L2 窗口后：l3-user-id 空（默认）→ L3 跳过——零新增审计 + 零通知 + 仅推进窗口时间戳
        advanceDays(1);
        scanOk();
        ErpCsTicket skipped = reload(TICKET_ID);
        assertEquals(actionsAtL2, countEscalateActions(TICKET_ID), "L3 跳过：零新增 ESCALATE 审计");
        assertEquals(2, skipped.getLastEscalationLevel(), "级别不推进（保留后补配置可恢复性）");
        assertTrue(skipped.getLastEscalationAt().compareTo(windowAtL2) > 0,
                "窗口时间戳推进（WARN 噪声约束至每窗口一次）");

        // 窗口内重复扫描：因时间戳推进而天然跳过（不重复 WARN）
        scanOk();
        assertEquals(actionsAtL2, countEscalateActions(TICKET_ID), "窗口内扫描仍零新增");

        // 再过一窗口：config 仍未配置 → 仍零审计（WARN 有界重复），level 保持 2
        advanceDays(1);
        scanOk();
        assertEquals(actionsAtL2, countEscalateActions(TICKET_ID), "跨窗口仍未配置：仍零新增审计");
        assertEquals(2, reload(TICKET_ID).getLastEscalationLevel(), "level 保持 2");
    }

    // ---------- ⑧ policy.escalationDelayHours 覆盖 config 默认 ----------

    @Test
    public void testPolicyDelayHoursOverridesConfigDefault() {
        seedTemplate(TEMPLATE_ID);
        // 票 A：policy delayHours=48（2 天）；票 B：policy null → config 默认 2h
        seedPolicy(POLICY_ID, ESCALATION_USER, SECOND_USER, 48);
        seedPolicy(POLICY_ID + 1, ESCALATION_USER, SECOND_USER, null);
        seedTicket(TICKET_ID, "TK-ESC-DLY48", NOW.minusHours(2), POLICY_ID);
        seedTicket(TICKET_ID + 1, "TK-ESC-DLYDEF", NOW.minusHours(2), POLICY_ID + 1);

        scanOk();
        assertEquals(1, reload(TICKET_ID).getEscalationCount(), "票 A 首扫 L1");
        assertEquals(1, reload(TICKET_ID + 1).getEscalationCount(), "票 B 首扫 L1");

        // +1 天（24h < 48h）：票 A 窗口未到不重复；票 B（默认 2h）窗口已过 → count=2
        advanceDays(1);
        scanOk();
        assertEquals(1, reload(TICKET_ID).getEscalationCount(), "policy 48h：24h 后仍在窗口内");
        assertEquals(2, reload(TICKET_ID + 1).getEscalationCount(), "config 默认 2h：24h 后已重复");

        // +2 天（48h ≥ 48h）：票 A 窗口到期 → count=2
        advanceDays(1);
        scanOk();
        assertEquals(2, reload(TICKET_ID).getEscalationCount(), "policy 48h：48h 后窗口到期重复");
    }

    // ---------- ⑨ resolve 后（isSlaCompleted=true）不再命中查询 ----------

    @Test
    public void testResolvedTicketNotScanned() {
        seedTemplate(TEMPLATE_ID);
        seedPolicy(POLICY_ID, ESCALATION_USER, SECOND_USER, null);
        seedTicket(TICKET_ID, "TK-ESC-RESOLVED", NOW.minusHours(2), POLICY_ID);
        ormTemplate.runInSession(() -> {
            ErpCsTicket t = daoProvider.daoFor(ErpCsTicket.class).getEntityById(TICKET_ID);
            t.setIsSlaCompleted(true);
            t.setStatus(ErpCsConstants.TICKET_STATUS_RESOLVED);
            daoProvider.daoFor(ErpCsTicket.class).updateEntity(t);
        });

        scanOk();
        assertEquals(0, countEscalateActions(TICKET_ID), "已 resolve 工单零 ESCALATE");
        assertNull(reload(TICKET_ID).getLastEscalationLevel(), "计数器不落库");
    }

    // ---------- ⑪ 存量工单兼容：既有 ESCALATE 审计行 + 计数器 null → 重新进入 L1 ----------

    @Test
    public void testLegacyTicketWithExistingEscalateReentersL1() {
        seedTemplate(TEMPLATE_ID);
        seedPolicy(POLICY_ID, ESCALATION_USER, SECOND_USER, null);
        seedTicket(TICKET_ID, "TK-ESC-LEGACY", NOW.minusHours(2), POLICY_ID);
        seedLegacyEscalateAction(7211L, TICKET_ID, "SLA 超时升级通知 escalationUserId");

        scanOk();
        ErpCsTicket t = reload(TICKET_ID);
        assertEquals(1, t.getLastEscalationLevel(), "存量工单（计数器 null）重新进入 L1 计数链");
        assertEquals(1, t.getEscalationCount(), "计数器权威判定：从 count=1 起算");
        assertEquals(2, countEscalateActions(TICKET_ID), "新增 1 条审计（存量 1 + 新 1）");
        // 新审计行 content 级别化（seq id 可能小于存量固定 id，按内容匹配而非按 id 序取尾行）
        boolean hasNewStyleContent = false;
        for (ErpCsTicketAction a : escalateActions(TICKET_ID)) {
            if (("SLA 超时升级 L1（第 1 次）通知 " + ESCALATION_USER).equals(a.getContent())) {
                hasNewStyleContent = true;
                break;
            }
        }
        assertTrue(hasNewStyleContent, "新审计行 content 承载级别/次数/目标");
    }

    // ---------- helpers ----------


    private ErpCsTicket reload(Long ticketId) {
        return daoProvider.daoFor(ErpCsTicket.class).getEntityById(ticketId);
    }

    private void scanOk() {
        ApiResponse<?> resp = rpc(mutation, "ErpCsTicket__scanOverdueTickets", new LinkedHashMap<>());
        assertEquals(0, resp.getStatus(), "scanOverdueTickets 应成功: " + resp);
    }

    /** 累计推进冻结时钟（天）：安装值随调用单调递增，窗口才会到期。 */
    private void advanceDays(long days) {
        clockOffsetDays += days;
        ThreadLocalFrozenClock.install(CsFrozenClockExtension.REFERENCE_DATE.plusDays(clockOffsetDays));
    }

    private static void assign(String key, String value) {
        AppConfig.getConfigProvider().assignConfigValue(key, value);
    }

    private int countEscalateActions(Long ticketId) {
        return escalateActions(ticketId).size();
    }

    private List<ErpCsTicketAction> escalateActions(Long ticketId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("ticketId", ticketId));
        q.addFilter(eq("actionType", ErpCsConstants.ACTION_TYPE_ESCALATE));
        q.addOrderField("id", false);
        return daoProvider.daoFor(ErpCsTicketAction.class).findAllByQuery(q);
    }

    private ErpCsTicketAction lastEscalateAction(Long ticketId) {
        List<ErpCsTicketAction> list = escalateActions(ticketId);
        return list.get(list.size() - 1);
    }

    /** 最近一条 cs.sla-overdue 通知（取最新行再断言接收人）。 */
    private ErpSysNotification lastNotification() {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", ErpCsConstants.NOTIFY_EVENT_SLA_OVERDUE));
        q.addOrderField("id", true);
        q.setLimit(1);
        List<ErpSysNotification> list = daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private ErpSysNotification notificationTo(String recipientUserId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("notificationType", ErpCsConstants.NOTIFY_EVENT_SLA_OVERDUE));
        q.addFilter(eq("recipientUserId", recipientUserId));
        q.setLimit(1);
        List<ErpSysNotification> list = daoProvider.daoFor(ErpSysNotification.class).findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private void seedTemplate(Long id) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpSysNotificationTemplate> dao = daoProvider.daoFor(ErpSysNotificationTemplate.class);
            ErpSysNotificationTemplate t = new ErpSysNotificationTemplate();
            t.orm_propValueByName("id", id);
            t.setNotificationType(ErpCsConstants.NOTIFY_EVENT_SLA_OVERDUE);
            t.setName("SLA超期多级升级");
            t.setChannelSet("IN_APP");
            t.setSubjectTpl("SLA超期预警: ${customerName}");
            t.setBodyTpl("工单 ${ticketCode} 已超 SLA（L${escalationLevel} 第 ${repeatCount} 次）");
            t.setRecipientResolver("USER_LIST");
            t.setRecipientConfig("{\"userIds\":[\"${escalationUserId}\"]}");
            t.setMergeWindowSeconds(0);
            t.setMergeStrategy("NONE");
            t.setStatus("ACTIVE");
            dao.saveEntity(t);
        });
    }

    private void seedPolicy(Long id, Long escalationUserId, Long secondEscalationUserId, Integer delayHours) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsSlaPolicy> dao = daoProvider.daoFor(ErpCsSlaPolicy.class);
            ErpCsSlaPolicy p = new ErpCsSlaPolicy();
            p.orm_propValueByName("id", id);
            p.setCode("SLA-ESC-" + id);
            p.setName("SLA-ESC-" + id);
            p.setTicketTypeId(TICKET_TYPE_ID);
            p.setResolveHours(8);
            p.setIsWorkingDays(false);
            p.setEscalationUserId(escalationUserId);
            p.setSecondEscalationUserId(secondEscalationUserId);
            p.setEscalationDelayHours(delayHours);
            dao.saveEntity(p);
        });
    }

    private void seedTicket(Long id, String code, LocalDateTime deadline, Long policyId) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicket> dao = daoProvider.daoFor(ErpCsTicket.class);
            ErpCsTicket t = new ErpCsTicket();
            t.setBusinessDate(LocalDate.of(2026, 7, 1));
            t.orm_propValueByName("id", id);
            t.setCode(code);
            t.setSubject("工单-" + code);
            t.setCustomerId(CUSTOMER_ID);
            t.setTicketTypeId(TICKET_TYPE_ID);
            t.setPriority(ErpCsConstants.TICKET_PRIORITY_HIGH);
            t.setStatus(ErpCsConstants.TICKET_STATUS_ASSIGNED);
            t.setDocStatus(ErpCsConstants.DOC_STATUS_ACTIVE);
            t.setApproveStatus(ErpCsConstants.APPROVE_STATUS_UNSUBMITTED);
            t.setIsSlaCompleted(false);
            t.setAssignedToId(ASSIGNEE);
            t.setSlaPolicyId(policyId);
            t.setDeadlineDateTime(Timestamp.valueOf(deadline));
            dao.saveEntity(t);
        });
        seedCustomerOnce();
    }

    private void seedLegacyEscalateAction(Long id, Long ticketId, String content) {
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpCsTicketAction> dao = daoProvider.daoFor(ErpCsTicketAction.class);
            ErpCsTicketAction a = new ErpCsTicketAction();
            a.orm_propValueByName("id", id);
            a.setTicketId(ticketId);
            a.setActionType(ErpCsConstants.ACTION_TYPE_ESCALATE);
            a.setFromStatus(ErpCsConstants.TICKET_STATUS_ASSIGNED);
            a.setToStatus(ErpCsConstants.TICKET_STATUS_ASSIGNED);
            a.setContent(content);
            a.setOperatorId("legacy-scan");
            dao.saveEntity(a);
        });
    }

    private void seedCustomerOnce() {
        ormTemplate.runInSession(() -> {
            if (daoProvider.daoFor(ErpMdPartner.class).getEntityById(CUSTOMER_ID) != null) {
                return;
            }
            IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
            ErpMdPartner p = new ErpMdPartner();
            p.orm_propValueByName("id", CUSTOMER_ID);
            p.setCode("CUS-" + CUSTOMER_ID);
            p.setName("ACME Corp");
            p.orm_propValueByName("partnerType", "CUSTOMER");
            p.orm_propValueByName("status", "ACTIVE");
            dao.saveEntity(p);
        });
    }

    private ApiResponse<?> rpc(io.nop.graphql.core.ast.GraphQLOperationType op, String action,
                               Map<String, Object> args) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(op, action, ApiRequest.build(args));
        return graphQLEngine.executeRpc(ctx);
    }
}
