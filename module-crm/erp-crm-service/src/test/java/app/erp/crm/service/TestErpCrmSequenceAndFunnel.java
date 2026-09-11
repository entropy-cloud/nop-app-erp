package app.erp.crm.service;

import app.erp.crm.biz.IErpCrmEventBiz;
import app.erp.crm.biz.IErpCrmLeadSequenceProgressBiz;
import app.erp.crm.dao.entity.ErpCrmEvent;
import app.erp.crm.dao.entity.ErpCrmFunnelStageMetrics;
import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadConvLog;
import app.erp.crm.dao.entity.ErpCrmLeadFunnel;
import app.erp.crm.dao.entity.ErpCrmLeadSequenceProgress;
import app.erp.crm.dao.entity.ErpCrmLostReason;
import app.erp.crm.dao.entity.ErpCrmSequence;
import app.erp.crm.dao.entity.ErpCrmSequenceAssignment;
import app.erp.crm.dao.entity.ErpCrmSequenceStep;
import app.erp.crm.dao.entity.ErpCrmStage;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.time.CoreMetrics;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
 * CRM 销售序列 + 漏斗端到端集成测试（plan 2026-07-07-1430-3 §Phase 3）。
 *
 * <p>经 {@link IGraphQLEngine} 调序列分配/推进/切换/逾期扫描 + 漏斗聚合引擎，覆盖：
 * 序列分配→推进→完成端到端、序列切换旧序列 SKIPPED、逾期扫描、漏斗 refreshFunnel 清旧重建 + getFunnelView 可视化结构。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmSequenceAndFunnel extends JunitAutoTestCase {

    static final String ORG_ID = "1301";
    static final String STAGE_NEW = "6301";
    static final String STAGE_QUALIFIED = "6302";
    static final String STAGE_WON = "6303";
    static final String REASON_PRICE = "6401";
    static final String SEQ_ID = "6501";
    static final String SEQ2_ID = "6502";
    static final String RULE_ID = "6511";

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IErpCrmLeadSequenceProgressBiz progressBiz;
    @Inject
    IErpCrmEventBiz eventBiz;
    @Inject
    app.erp.crm.biz.IErpCrmLeadBiz leadBiz;

    @Test
    public void testSequenceAssignAdvanceComplete() {
        String leadId = "6001";
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            seedSequence(SEQ_ID, "SEQ-001", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
            // 两步序列：CALL（首步 autoCreateEvent）→ EMAIL
            seedStep("6601", SEQ_ID, 1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, true);
            seedStep("6602", SEQ_ID, 2, "EMAIL", ErpCrmConstants.STEP_COMPLETION_EMAIL_OPENED, false);
            seedAssignmentRule(RULE_ID, SEQ_ID, "LEAD_SOURCE", "{\"sourceId\":[101]}", 10);

            // Lead：sourceId=101 命中规则
            ErpCrmLead lead = newLead(leadId, "LEAD-SEQ-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setSourceId("101");
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
        });

        // 1. assignSequence → 建 Progress(IN_PROGRESS, stepIndex=0) + 首步 autoCreateEvent 建 CALL Event
        ApiResponse<?> assignResp = assignSequence(leadId);
        assertEquals(0, assignResp.getStatus(), "assignSequence 应成功");

        ErpCrmLeadSequenceProgress progress = reloadActiveProgress(leadId);
        assertNotNull(progress, "活跃进度已创建");
        assertEquals(SEQ_ID, progress.getSequenceId());
        assertEquals(0, progress.getCurrentStepIndex(), "首步 stepIndex=0");
        assertEquals(ErpCrmConstants.SEQUENCE_PROGRESS_IN_PROGRESS, progress.getStatus());

        // 首步 autoCreateEvent=true → 已建 PLANNED CALL Event
        ErpCrmEvent plannedCall = findPlannedEvent(leadId, "CALL");
        assertNotNull(plannedCall, "首步 autoCreateEvent 已建 CALL Event");

        // 2. 模拟 CALL Event 完成 → advanceStep → stepIndex=1
        ormTemplate.runInSession(() -> {
            ErpCrmEvent evt = ormTemplate.runInSession(session -> eventBiz.requireEntity(String.valueOf(plannedCall.getId()), null,
                    new io.nop.core.context.ServiceContextImpl()));
            evt.setStatus(ErpCrmConstants.EVENT_STATUS_COMPLETED);
            daoProvider.daoFor(ErpCrmEvent.class).updateEntity(evt);
        });

        ApiResponse<?> advance1 = advanceStep(progress.getId(), plannedCall.getId());
        assertEquals(0, advance1.getStatus(), "advanceStep(CALL) 应成功");
        progress = reloadProgress(progress.getId());
        assertEquals(1, progress.getCurrentStepIndex(), "推进后 stepIndex=1");
        assertEquals(ErpCrmConstants.SEQUENCE_PROGRESS_IN_PROGRESS, progress.getStatus(), "仍有下一步 → 未完成");

        // 3. 第二步 EMAIL：建一个 COMPLETED EMAIL Event → advanceStep → 序列完成
        ErpCrmEvent emailEvent = newCompletedEvent(leadId, "EMAIL", "EVT-EMAIL-001");
        daoProvider.daoFor(ErpCrmEvent.class).saveEntity(emailEvent);

        ApiResponse<?> advance2 = advanceStep(progress.getId(), emailEvent.getId());
        assertEquals(0, advance2.getStatus(), "advanceStep(EMAIL) 应成功");
        progress = reloadProgress(progress.getId());
        assertEquals(ErpCrmConstants.SEQUENCE_PROGRESS_COMPLETED, progress.getStatus(), "末步完成 → status=COMPLETED");
        assertNotNull(progress.getCompletedAt(), "completedAt 已写");
    }

    @Test
    public void testSwitchSequenceOldSkipped() {
        String leadId = "6002";
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            seedSequence(SEQ_ID, "SEQ-SW-1", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
            seedSequence(SEQ2_ID, "SEQ-SW-2", ErpCrmConstants.SEQUENCE_TEMPLATE_QUALIFICATION);
            seedStep("6611", SEQ_ID, 1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
            seedStep("6612", SEQ2_ID, 1, "EMAIL", ErpCrmConstants.STEP_COMPLETION_EMAIL_OPENED, false);
            seedAssignmentRule(RULE_ID, SEQ_ID, "LEAD_SOURCE", "{\"sourceId\":[101]}", 10);

            ErpCrmLead lead = newLead(leadId, "LEAD-SW-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setSourceId("101");
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
        });

        // 先分配旧序列
        assertEquals(0, assignSequence(leadId).getStatus());
        ErpCrmLeadSequenceProgress oldProgress = reloadActiveProgress(leadId);
        assertNotNull(oldProgress);
        assertEquals(SEQ_ID, oldProgress.getSequenceId());
        String oldProgressId = oldProgress.getId();

        // switchSequence → 旧序列 SKIPPED + 新序列 IN_PROGRESS
        ApiResponse<?> switchResp = switchSequence(leadId, SEQ2_ID);
        assertEquals(0, switchResp.getStatus(), "switchSequence 应成功");

        // 旧进度变 SKIPPED
        ErpCrmLeadSequenceProgress oldAfter = reloadProgress(oldProgressId);
        assertEquals(ErpCrmConstants.SEQUENCE_PROGRESS_SKIPPED, oldAfter.getStatus(),
                "旧序列 SKIPPED");
        assertNotNull(oldAfter.getCompletedAt(), "旧序列 completedAt 已写（SKIPPED 时间戳）");

        // 新活跃进度
        ErpCrmLeadSequenceProgress newProgress = reloadActiveProgress(leadId);
        assertNotNull(newProgress, "新序列活跃进度已创建");
        assertEquals(SEQ2_ID, newProgress.getSequenceId(), "新序列 = SEQ2");
        assertEquals(0, newProgress.getCurrentStepIndex(), "新序列 stepIndex=0");
        assertEquals(ErpCrmConstants.SEQUENCE_PROGRESS_IN_PROGRESS, newProgress.getStatus());
    }

    @Test
    public void testAssignTwiceRejects() {
        String leadId = "6003";
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            seedSequence(SEQ_ID, "SEQ-DUP", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
            seedStep("6621", SEQ_ID, 1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
            seedAssignmentRule(RULE_ID, SEQ_ID, "LEAD_SOURCE", "{\"sourceId\":[101]}", 10);

            ErpCrmLead lead = newLead(leadId, "LEAD-DUP-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setSourceId("101");
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
        });

        assertEquals(0, assignSequence(leadId).getStatus(), "首次 assignSequence 应成功");
        // 再次 assign → ERR_SEQUENCE_ALREADY_ASSIGNED
        ApiResponse<?> dup = assignSequence(leadId);
        assertEquals(ErpCrmErrors.ERR_SEQUENCE_ALREADY_ASSIGNED.getErrorCode(), dup.getCode(),
                "已有活跃进度时拒绝重复分配 → ERR_SEQUENCE_ALREADY_ASSIGNED");
    }

    @Test
    public void testNoMatchRejects() {
        String leadId = "6004";
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            seedSequence(SEQ_ID, "SEQ-NOMATCH", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
            seedStep("6631", SEQ_ID, 1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
            // 规则要求 sourceId=101，但 lead sourceId=999
            seedAssignmentRule(RULE_ID, SEQ_ID, "LEAD_SOURCE", "{\"sourceId\":[101]}", 10);

            ErpCrmLead lead = newLead(leadId, "LEAD-NOMATCH-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setSourceId("999");
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
        });

        ApiResponse<?> resp = assignSequence(leadId);
        assertEquals(ErpCrmErrors.ERR_SEQUENCE_NO_MATCH.getErrorCode(), resp.getCode(),
                "无匹配规则且无 default → ERR_SEQUENCE_NO_MATCH");
    }

    @Test
    public void testDefaultFallbackAssigns() {
        String leadId = "6005";
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            seedSequence(SEQ_ID, "SEQ-DEF", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
            seedStep("6641", SEQ_ID, 1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
            // 仅 default 规则（sourceId=101 不匹配 lead sourceId=999）
            seedDefaultAssignmentRule(String.valueOf(Long.parseLong(RULE_ID) + 1), SEQ_ID);

            ErpCrmLead lead = newLead(leadId, "LEAD-DEF-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setSourceId("999");
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
        });

        ApiResponse<?> resp = assignSequence(leadId);
        assertEquals(0, resp.getStatus(), "无具体命中 → 走 default 规则");
        ErpCrmLeadSequenceProgress progress = reloadActiveProgress(leadId);
        assertNotNull(progress);
        assertEquals(SEQ_ID, progress.getSequenceId(), "default 规则分配 SEQ_ID");
    }

    @Test
    public void testScanOverdueSteps() {
        String leadId = "6006";
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            seedSequence(SEQ_ID, "SEQ-OVERDUE", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
            // 三步：每步 dueDays=1，startedAt=10天前 → 全部逾期（累计 due=3 + grace=2 = 5 < 10）
            seedStep("6651", SEQ_ID, 1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
            seedStep("6652", SEQ_ID, 2, "EMAIL", ErpCrmConstants.STEP_COMPLETION_EMAIL_OPENED, false);
            seedStep("6653", SEQ_ID, 3, "MEETING", ErpCrmConstants.STEP_COMPLETION_MEETING_HELD, false);
            seedAssignmentRule(RULE_ID, SEQ_ID, "LEAD_SOURCE", "{\"sourceId\":[101]}", 10);

            ErpCrmLead lead = newLead(leadId, "LEAD-OVERDUE-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setSourceId("101");
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
        });
        assertEquals(0, assignSequence(leadId).getStatus());

        // 把 startedAt 改为 10 天前 + currentStepIndex=2（在三步序列的第3步）
        // → 累计 dueDays = 1+1+1 = 3，due = startedAt + 3 + grace(2) = startedAt + 5 = now - 5 < now → 逾期
        // → 连续逾期 = 3（step 0,1,2 全部逾期）≥ max-overdue-steps(3)
        ormTemplate.runInSession(() -> {
            ErpCrmLeadSequenceProgress p = reloadActiveProgress(leadId);
            p.setStartedAt(Timestamp.valueOf(CoreMetrics.currentDateTime().minusDays(10)));
            p.setCurrentStepIndex(2);
            daoProvider.daoFor(ErpCrmLeadSequenceProgress.class).updateEntity(p);
        });

        List<Map<String, Object>> overdue = ormTemplate.runInSession(session -> progressBiz.scanOverdueSteps(new io.nop.core.context.ServiceContextImpl()));
        assertFalse(overdue.isEmpty(), "应扫描到逾期进度（连续逾期 3 步 ≥ max-overdue-steps=3）");
        Map<String, Object> first = overdue.get(0);
        assertEquals(leadId, String.valueOf(first.get("leadId")));
        int overdueCount = ((Number) first.get("overdueStepCount")).intValue();
        assertTrue(overdueCount >= 3, "连续逾期步数 ≥ 3");
    }

    @Test
    public void testOnTimeAdvanceDoesNotMisreportOverdue() {
        // P1-CK-crm2-001 反向用例（plan 2026-09-11-2350-1 Phase 2）：按期推进到第 5 步不应误报逾期。
        // dueDays=[1,1,1,10,10]、grace=2、startedAt=10 天前、currentIndex=4：
        //   旧正向扫描：due[0..2]=3/4/5 天 < 10 → 计 3 ≥ max-overdue-steps → 误报；
        //   修复后反向扫描：当前步 due[4]=1+1+1+10+10+2=25 天 > 10 → 当步未逾期 → 0。
        String leadId = "6010";
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            seedSequence(SEQ_ID, "SEQ-ONTIME", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
            seedStep("6671", SEQ_ID, 1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
            ((ErpCrmSequenceStep) daoProvider.daoFor(ErpCrmSequenceStep.class).getEntityById("6671")).setDueDays(1);
            seedStep("6672", SEQ_ID, 2, "EMAIL", ErpCrmConstants.STEP_COMPLETION_EMAIL_OPENED, false);
            ((ErpCrmSequenceStep) daoProvider.daoFor(ErpCrmSequenceStep.class).getEntityById("6672")).setDueDays(1);
            seedStep("6673", SEQ_ID, 3, "MEETING", ErpCrmConstants.STEP_COMPLETION_MEETING_HELD, false);
            ((ErpCrmSequenceStep) daoProvider.daoFor(ErpCrmSequenceStep.class).getEntityById("6673")).setDueDays(1);
            seedStep("6674", SEQ_ID, 4, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
            ((ErpCrmSequenceStep) daoProvider.daoFor(ErpCrmSequenceStep.class).getEntityById("6674")).setDueDays(10);
            seedStep("6675", SEQ_ID, 5, "EMAIL", ErpCrmConstants.STEP_COMPLETION_EMAIL_OPENED, false);
            ((ErpCrmSequenceStep) daoProvider.daoFor(ErpCrmSequenceStep.class).getEntityById("6675")).setDueDays(10);
            seedAssignmentRule(RULE_ID, SEQ_ID, "LEAD_SOURCE", "{\"sourceId\":[101]}", 10);

            ErpCrmLead lead = newLead(leadId, "LEAD-ONTIME-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setSourceId("101");
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
        });
        assertEquals(0, assignSequence(leadId).getStatus());

        ormTemplate.runInSession(() -> {
            ErpCrmLeadSequenceProgress p = reloadActiveProgress(leadId);
            p.setStartedAt(Timestamp.valueOf(CoreMetrics.currentDateTime().minusDays(10)));
            p.setCurrentStepIndex(4);
            daoProvider.daoFor(ErpCrmLeadSequenceProgress.class).updateEntity(p);
        });

        List<Map<String, Object>> overdue = ormTemplate.runInSession(session -> progressBiz.scanOverdueSteps(new io.nop.core.context.ServiceContextImpl()));
        boolean misreported = overdue.stream()
                .anyMatch(row -> String.valueOf(row.get("leadId")).equals(leadId));
        assertFalse(misreported, "按期推进到第 5 步（当前步未到期）不应误报逾期");
    }

    @Test
    public void testGetSequencePerformance() {
        String leadId = "6007";
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            seedSequence(SEQ_ID, "SEQ-PERF", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
            seedStep("6661", SEQ_ID, 1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
            seedAssignmentRule(RULE_ID, SEQ_ID, "LEAD_SOURCE", "{\"sourceId\":[101]}", 10);

            ErpCrmLead lead = newLead(leadId, "LEAD-PERF-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setSourceId("101");
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
        });
        assertEquals(0, assignSequence(leadId).getStatus());
        // 手动标记为 COMPLETED 以便性能统计
        ormTemplate.runInSession(() -> {
            ErpCrmLeadSequenceProgress p = reloadActiveProgress(leadId);
            p.setStatus(ErpCrmConstants.SEQUENCE_PROGRESS_COMPLETED);
            p.setStartedAt(Timestamp.valueOf(CoreMetrics.currentDateTime().minusDays(5)));
            p.setCompletedAt(CoreMetrics.currentTimestamp());
            daoProvider.daoFor(ErpCrmLeadSequenceProgress.class).updateEntity(p);
        });

        Map<String, Object> perf = (Map<String, Object>) graphQLEngine.executeRpc(
                graphQLEngine.newRpcContext(query, "ErpCrmLeadSequenceProgress__getSequencePerformance",
                        ApiRequest.build(Map.of("templateType", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD))))
                .getData();
        assertNotNull(perf);
        assertEquals(ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD, perf.get("templateType"));
        assertTrue(((Number) perf.get("totalAssigned")).intValue() >= 1, "totalAssigned ≥ 1");
        assertTrue(((Number) perf.get("totalCompleted")).intValue() >= 1, "totalCompleted ≥ 1");
    }

    @Test
    public void testRefreshFunnelClearRebuildAndView() {
        String leadWon = "6011";
        String leadLost = "6012";
        String leadActive = "6013";
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            // 3 leads：won / lost / active
            ErpCrmLead won = newLead(leadWon, "OPP-FN-WON", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                    ErpCrmConstants.DOC_STATUS_CONVERTED, new BigDecimal("1000"), 90, null);
            ErpCrmLead lost = newLead(leadLost, "OPP-FN-LOST", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                    ErpCrmConstants.DOC_STATUS_LOST, new BigDecimal("500"), 0, REASON_PRICE);
            ErpCrmLead active = newLead(leadActive, "OPP-FN-ACT", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                    ErpCrmConstants.DOC_STATUS_QUALIFIED, new BigDecimal("2000"), 50, null);
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(won);
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lost);
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(active);

            // ConvLogs：NEW → QUALIFIED → WON（仅 won）
            LocalDateTime t0 = LocalDateTime.of(2026, 7, 5, 9, 0);
            saveConvLog("7001", leadWon, null, STAGE_NEW, t0);
            saveConvLog("7002", leadWon, STAGE_NEW, STAGE_QUALIFIED, t0.plusDays(2));
            saveConvLog("7003", leadWon, STAGE_QUALIFIED, STAGE_WON, t0.plusDays(7));
            // lost：NEW → QUALIFIED（在 QUALIFIED 丢失）
            saveConvLog("7004", leadLost, null, STAGE_NEW, t0);
            saveConvLog("7005", leadLost, STAGE_NEW, STAGE_QUALIFIED, t0.plusDays(1));
            // active：NEW → QUALIFIED
            saveConvLog("7006", leadActive, null, STAGE_NEW, t0);
            saveConvLog("7007", leadActive, STAGE_NEW, STAGE_QUALIFIED, t0.plusDays(3));
        });

        LocalDate start = LocalDate.of(2026, 7, 1);
        LocalDate end = LocalDate.of(2026, 7, 31);

        // 首次 refresh
        ApiResponse<?> r1 = refreshFunnel(start, end, null, null, null);
        assertEquals(0, r1.getStatus(), "refreshFunnel 应成功");
        ErpCrmLeadFunnel funnel1 = reloadFunnel(start, end, null, null, null);
        assertNotNull(funnel1, "LeadFunnel 已创建");
        assertEquals(1, funnel1.getTotalWon(), "1 won");
        assertEquals(1, funnel1.getTotalLost(), "1 lost");
        assertEquals(0, funnel1.getTotalRevenue().compareTo(new BigDecimal("1000")), "won revenue=1000");
        assertEquals(0, funnel1.getLostRevenue().compareTo(new BigDecimal("500")), "lost revenue=500");
        // 阶段明细
        List<ErpCrmFunnelStageMetrics> metrics = loadStageMetrics(funnel1.getId());
        assertEquals(3, metrics.size(), "3 阶段 = 3 明细");

        // 再次 refresh → 清旧重建（不应产生重复）
        assertEquals(0, refreshFunnel(start, end, null, null, null).getStatus());
        List<ErpCrmLeadFunnel> all = loadAllFunnels(start, end, null, null, null);
        assertEquals(1, all.size(), "清旧重建：仅 1 条 LeadFunnel");
        // 重新加载获取新 ID（旧 ID 已被清旧重建删除）
        ErpCrmLeadFunnel funnelReloaded = all.get(0);

        // getFunnelView 可视化结构
        ApiResponse<?> viewResp = graphQLEngine.executeRpc(
                graphQLEngine.newRpcContext(query, "ErpCrmLeadFunnel__getFunnelView",
                        ApiRequest.build(Map.of("funnelId", funnelReloaded.getId()))));
        assertEquals(0, viewResp.getStatus(), "getFunnelView 应成功: code=" + viewResp.getCode() + " msg=" + viewResp.getMsg());
        Map<String, Object> view = (Map<String, Object>) viewResp.getData();
        assertNotNull(view);
        assertEquals(funnelReloaded.getId(), view.get("funnelId"));
        assertNotNull(view.get("stages"), "stages 数组已生成");
        assertEquals(3, ((List<?>) view.get("stages")).size(), "3 阶段可视化");
    }

    @Test
    public void testFunnelCapturesTerminalEventsAndWonSingleCount() {
        // P1-CK-crm-002（plan 2026-09-11-2350-1 Phase 3）：
        // 场景 1（期间圈定闭合）：lead 六月 moveStage、七月 lose（丢失事件无六月日志）→ 七月漏斗应计 totalLost=1
        //   （修复前 refreshFunnel 以「期间内有 ConvLog」圈定 → 七月无该 lead 日志 → totalLost 漏计为 0）。
        // 场景 2（won 消双计）：convertToCustomer 链的 LEAD 型 CONVERTED + OPPORTUNITY 型 CONVERTED 同期并存
        //   → totalWon 应仅计 OPPORTUNITY（=1）、totalRevenue 单计（修复前双计 2/2000）。
        String leadLost = "6014";
        String leadOrig = "6015";
        String leadOpp = "6016";
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            // 场景 1：OPPORTUNITY 六月推进、七月丢失（种子 QUALIFIED，由真实 lose mutation 完成迁移）
            ErpCrmLead lost = newLead(leadLost, "OPP-FN-LS2", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                    ErpCrmConstants.DOC_STATUS_QUALIFIED, new BigDecimal("500"), 50, REASON_PRICE);
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lost);
            saveConvLog("7101", leadLost, null, STAGE_NEW, java.time.LocalDateTime.of(2026, 6, 5, 9, 0));
            saveConvLog("7102", leadLost, STAGE_NEW, STAGE_QUALIFIED, java.time.LocalDateTime.of(2026, 6, 20, 9, 0));
            // 场景 2：转化链双记录（原 LEAD CONVERTED + 新建 OPPORTUNITY CONVERTED，同额 revenue）
            ErpCrmLead orig = newLead(leadOrig, "LEAD-FN-ORIG", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_CONVERTED, new BigDecimal("1000"), 100, null);
            ErpCrmLead opp = newLead(leadOpp, "OPP-FN-ORIG", ErpCrmConstants.LEAD_TYPE_OPPORTUNITY,
                    ErpCrmConstants.DOC_STATUS_CONVERTED, new BigDecimal("1000"), 100, null);
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(orig);
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(opp);
            saveConvLog("7103", leadOrig, null, STAGE_NEW, java.time.LocalDateTime.now().minusDays(3));
            saveConvLog("7104", leadOpp, null, STAGE_NEW, java.time.LocalDateTime.now().minusDays(3));
        });

        // 场景 1：当前月漏斗（丢失事件期间 = lose mutation 的 ConvLog.changedAt 所在月）——
        // 经真实 lose mutation 触发（修复后 doLose 写当月 ConvLog）；六月 moveStage 日志落在上月不圈定
        String lostReasonId = REASON_PRICE;
        ormTemplate.runInSession(session -> leadBiz.lose(leadLost, lostReasonId, "当月丢失", new io.nop.core.context.ServiceContextImpl()));
        LocalDate now = io.nop.api.core.time.CoreMetrics.today();
        LocalDate periodStart = now.withDayOfMonth(1);
        LocalDate periodEnd = now.withDayOfMonth(now.lengthOfMonth());
        assertEquals(0, refreshFunnel(periodStart, periodEnd, null, null, null).getStatus());
        ErpCrmLeadFunnel periodFunnel = reloadFunnel(periodStart, periodEnd, null, null, null);
        assertNotNull(periodFunnel);
        assertEquals(1, periodFunnel.getTotalLost(), "当月丢失事件应入当月漏斗（期间圈定闭合）");

        // 场景 2：won 仅计 OPPORTUNITY
        assertEquals(1, periodFunnel.getTotalWon(), "转化链双记录仅 OPPORTUNITY 计 won");
        assertEquals(0, periodFunnel.getTotalRevenue().compareTo(new BigDecimal("1000")), "won revenue 单计 1000");
    }

    @Test
    public void testInvalidPeriodRejects() {
        ApiResponse<?> resp = refreshFunnel(
                LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 1), null, null, null);
        assertEquals(ErpCrmErrors.ERR_FUNNEL_PERIOD_INVALID.getErrorCode(), resp.getCode(),
                "periodStart 晚于 periodEnd → ERR_FUNNEL_PERIOD_INVALID");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> assignSequence(String leadId) {
        return rpc(mutation, "ErpCrmLeadSequenceProgress__assignSequence", Map.of("leadId", leadId));
    }

    private ApiResponse<?> advanceStep(String progressId, String eventId) {
        return rpc(mutation, "ErpCrmLeadSequenceProgress__advanceStep",
                Map.of("progressId", progressId, "eventId", eventId));
    }

    private ApiResponse<?> switchSequence(String leadId, String newSequenceId) {
        return rpc(mutation, "ErpCrmLeadSequenceProgress__switchSequence",
                Map.of("leadId", leadId, "newSequenceId", newSequenceId));
    }

    private ApiResponse<?> refreshFunnel(LocalDate start, LocalDate end, String t, String team, String src) {
        // 使用 HashMap 允许 null 值（Map.of 不允许 null）
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("periodStart", start);
        data.put("periodEnd", end);
        data.put("territoryId", t);
        data.put("teamId", team);
        data.put("sourceId", src);
        return rpc(mutation, "ErpCrmLeadFunnel__refreshFunnel", data);
    }

    private ApiResponse<?> rpc(GraphQLOperationType opType, String action, Map<String, Object> data) {
        return graphQLEngine.executeRpc(
                graphQLEngine.newRpcContext(opType, action, ApiRequest.build(data)));
    }

    // ---------- seed helpers ----------

    private void seedStages() {
        IEntityDao<ErpCrmStage> dao = daoProvider.daoFor(ErpCrmStage.class);
        saveStage(dao, STAGE_NEW, "STG-N", "新线索", 10);
        saveStage(dao, STAGE_QUALIFIED, "STG-Q", "已验证", 20);
        saveStage(dao, STAGE_WON, "STG-W", "赢单", 30);
    }

    private void saveStage(IEntityDao<ErpCrmStage> dao, String id, String code, String name, int seq) {
        ErpCrmStage s = new ErpCrmStage();
        s.setId(id);
        s.setCode(code);
        s.setStageName(name);
        s.setSequence(seq);
        dao.saveEntity(s);
    }

    private void seedLostReasons() {
        IEntityDao<ErpCrmLostReason> dao = daoProvider.daoFor(ErpCrmLostReason.class);
        ErpCrmLostReason r = new ErpCrmLostReason();
        r.setId(REASON_PRICE);
        r.setCode("LR-PRICE");
        r.setName("价格太高");
        r.setSequence(10);
        dao.saveEntity(r);
    }

    private void seedSequence(String id, String code, String templateType) {
        ErpCrmSequence seq = new ErpCrmSequence();
        seq.setId(id);
        seq.setCode(code);
        seq.setOrgId(ORG_ID);
        seq.setName(code);
        seq.setTemplateType(templateType);
        seq.setIsActive(Boolean.TRUE);
        seq.setIsDefault(Boolean.FALSE);
        daoProvider.daoFor(ErpCrmSequence.class).saveEntity(seq);
    }

    private void seedStep(String id, String sequenceId, int order, String activityType,
                           String condition, boolean autoCreateEvent) {
        ErpCrmSequenceStep step = new ErpCrmSequenceStep();
        step.setId(id);
        step.setSequenceId(sequenceId);
        step.setOrgId(ORG_ID);
        step.setStepName("Step-" + order);
        step.setStepOrder(order);
        step.setDueDays(1);
        step.setActivityType(activityType);
        step.setCompletionCondition(condition);
        step.setAutoCreateEvent(autoCreateEvent);
        daoProvider.daoFor(ErpCrmSequenceStep.class).saveEntity(step);
    }

    private void seedAssignmentRule(String id, String sequenceId, String conditionType,
                                     String conditionValue, int priority) {
        ErpCrmSequenceAssignment rule = new ErpCrmSequenceAssignment();
        rule.setId(id);
        rule.setOrgId(ORG_ID);
        rule.setSequenceId(sequenceId);
        rule.setConditionType(conditionType);
        rule.setConditionValue(conditionValue);
        rule.setPriority(priority);
        rule.setIsActive(Boolean.TRUE);
        rule.setIsDefault(Boolean.FALSE);
        daoProvider.daoFor(ErpCrmSequenceAssignment.class).saveEntity(rule);
    }

    private void seedDefaultAssignmentRule(String id, String sequenceId) {
        ErpCrmSequenceAssignment rule = new ErpCrmSequenceAssignment();
        rule.setId(id);
        rule.setOrgId(ORG_ID);
        rule.setSequenceId(sequenceId);
        rule.setConditionType(ErpCrmConstants.SEQ_ASSIGNMENT_CONDITION_CUSTOM_FIELD);
        rule.setConditionValue("{}");
        rule.setIsActive(Boolean.TRUE);
        rule.setIsDefault(Boolean.TRUE);
        rule.setPriority(Integer.MAX_VALUE);
        daoProvider.daoFor(ErpCrmSequenceAssignment.class).saveEntity(rule);
    }

    private ErpCrmLead newLead(String id, String code, String leadType, String docStatus) {
        ErpCrmLead lead = new ErpCrmLead();
        lead.setId(id);
        lead.setCode(code);
        lead.setOrgId(ORG_ID);
        lead.setLeadType(leadType);
        lead.setDocStatus(docStatus);
        lead.setContactName("联系人" + id);
        return lead;
    }

    private ErpCrmLead newLead(String id, String code, String leadType, String docStatus,
                                BigDecimal revenue, int probability, String lostReasonId) {
        ErpCrmLead lead = newLead(id, code, leadType, docStatus);
        lead.setExpectedRevenue(revenue);
        lead.setProbability(probability);
        lead.setLostReasonId(lostReasonId);
        return lead;
    }

    private ErpCrmEvent newCompletedEvent(String leadId, String eventType, String code) {
        ErpCrmEvent event = new ErpCrmEvent();
        event.setId("6021");
        event.setOrgId(ORG_ID);
        event.setCode(code);
        event.setEventType(eventType);
        event.setSubject("Test Event " + code);
        event.setRelatedLeadId(leadId);
        event.setStatus(ErpCrmConstants.EVENT_STATUS_COMPLETED);
        event.setPriority("NORMAL");
        event.setStartDateTime(CoreMetrics.currentTimestamp());
        event.setEndDateTime(Timestamp.valueOf(CoreMetrics.currentDateTime().plusHours(1)));
        return event;
    }

    private void saveConvLog(String id, String leadId, String fromStage, String toStage, LocalDateTime changedAt) {
        ErpCrmLeadConvLog log = new ErpCrmLeadConvLog();
        log.setId(id);
        log.setLeadId(leadId);
        log.setFromStageId(fromStage);
        log.setToStageId(toStage);
        log.setChangedAt(Timestamp.valueOf(changedAt));
        daoProvider.daoFor(ErpCrmLeadConvLog.class).saveEntity(log);
    }

    // ---------- reload helpers ----------

    private ErpCrmLeadSequenceProgress reloadActiveProgress(String leadId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("leadId", leadId));
        q.addFilter(eq("status", ErpCrmConstants.SEQUENCE_PROGRESS_IN_PROGRESS));
        q.setLimit(1);
        return progressDao().findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpCrmLeadSequenceProgress reloadProgress(String progressId) {
        return progressDao().getEntityById(progressId);
    }

    private ErpCrmEvent findPlannedEvent(String leadId, String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedLeadId", leadId));
        q.addFilter(eq("eventType", eventType));
        q.setLimit(1);
        return eventDao().findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpCrmLeadFunnel reloadFunnel(LocalDate start, LocalDate end, String t, String team, String src) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodStart", start));
        q.addFilter(eq("periodEnd", end));
        q.setLimit(1);
        return funnelDao().findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private List<ErpCrmLeadFunnel> loadAllFunnels(LocalDate start, LocalDate end, String t, String team, String src) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodStart", start));
        q.addFilter(eq("periodEnd", end));
        return funnelDao().findAllByQuery(q);
    }

    private List<ErpCrmFunnelStageMetrics> loadStageMetrics(String funnelId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("funnelId", funnelId));
        return stageMetricsDao().findAllByQuery(q);
    }

    private IEntityDao<ErpCrmLeadSequenceProgress> progressDao() {
        return daoProvider.daoFor(ErpCrmLeadSequenceProgress.class);
    }

    private IEntityDao<ErpCrmEvent> eventDao() {
        return daoProvider.daoFor(ErpCrmEvent.class);
    }

    private IEntityDao<ErpCrmLeadFunnel> funnelDao() {
        return daoProvider.daoFor(ErpCrmLeadFunnel.class);
    }

    private IEntityDao<ErpCrmFunnelStageMetrics> stageMetricsDao() {
        return daoProvider.daoFor(ErpCrmFunnelStageMetrics.class);
    }
}
