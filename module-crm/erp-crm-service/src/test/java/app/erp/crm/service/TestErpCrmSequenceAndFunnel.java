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

    static final Long ORG_ID = 1301L;
    static final Long STAGE_NEW = 6301L;
    static final Long STAGE_QUALIFIED = 6302L;
    static final Long STAGE_WON = 6303L;
    static final Long REASON_PRICE = 6401L;
    static final Long SEQ_ID = 6501L;
    static final Long SEQ2_ID = 6502L;
    static final Long RULE_ID = 6511L;

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

    @Test
    public void testSequenceAssignAdvanceComplete() {
        Long leadId = 6001L;
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            seedSequence(SEQ_ID, "SEQ-001", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
            // 两步序列：CALL（首步 autoCreateEvent）→ EMAIL
            seedStep(6601L, SEQ_ID, 1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, true);
            seedStep(6602L, SEQ_ID, 2, "EMAIL", ErpCrmConstants.STEP_COMPLETION_EMAIL_OPENED, false);
            seedAssignmentRule(RULE_ID, SEQ_ID, "LEAD_SOURCE", "{\"sourceId\":[101]}", 10);

            // Lead：sourceId=101 命中规则
            ErpCrmLead lead = newLead(leadId, "LEAD-SEQ-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setSourceId(101L);
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
        Long leadId = 6002L;
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            seedSequence(SEQ_ID, "SEQ-SW-1", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
            seedSequence(SEQ2_ID, "SEQ-SW-2", ErpCrmConstants.SEQUENCE_TEMPLATE_QUALIFICATION);
            seedStep(6611L, SEQ_ID, 1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
            seedStep(6612L, SEQ2_ID, 1, "EMAIL", ErpCrmConstants.STEP_COMPLETION_EMAIL_OPENED, false);
            seedAssignmentRule(RULE_ID, SEQ_ID, "LEAD_SOURCE", "{\"sourceId\":[101]}", 10);

            ErpCrmLead lead = newLead(leadId, "LEAD-SW-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setSourceId(101L);
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
        });

        // 先分配旧序列
        assertEquals(0, assignSequence(leadId).getStatus());
        ErpCrmLeadSequenceProgress oldProgress = reloadActiveProgress(leadId);
        assertNotNull(oldProgress);
        assertEquals(SEQ_ID, oldProgress.getSequenceId());
        Long oldProgressId = oldProgress.getId();

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
        Long leadId = 6003L;
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            seedSequence(SEQ_ID, "SEQ-DUP", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
            seedStep(6621L, SEQ_ID, 1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
            seedAssignmentRule(RULE_ID, SEQ_ID, "LEAD_SOURCE", "{\"sourceId\":[101]}", 10);

            ErpCrmLead lead = newLead(leadId, "LEAD-DUP-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setSourceId(101L);
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
        Long leadId = 6004L;
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            seedSequence(SEQ_ID, "SEQ-NOMATCH", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
            seedStep(6631L, SEQ_ID, 1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
            // 规则要求 sourceId=101，但 lead sourceId=999
            seedAssignmentRule(RULE_ID, SEQ_ID, "LEAD_SOURCE", "{\"sourceId\":[101]}", 10);

            ErpCrmLead lead = newLead(leadId, "LEAD-NOMATCH-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setSourceId(999L);
            daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
        });

        ApiResponse<?> resp = assignSequence(leadId);
        assertEquals(ErpCrmErrors.ERR_SEQUENCE_NO_MATCH.getErrorCode(), resp.getCode(),
                "无匹配规则且无 default → ERR_SEQUENCE_NO_MATCH");
    }

    @Test
    public void testDefaultFallbackAssigns() {
        Long leadId = 6005L;
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            seedSequence(SEQ_ID, "SEQ-DEF", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
            seedStep(6641L, SEQ_ID, 1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
            // 仅 default 规则（sourceId=101 不匹配 lead sourceId=999）
            seedDefaultAssignmentRule(RULE_ID + 1, SEQ_ID);

            ErpCrmLead lead = newLead(leadId, "LEAD-DEF-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setSourceId(999L);
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
        Long leadId = 6006L;
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            seedSequence(SEQ_ID, "SEQ-OVERDUE", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
            // 三步：每步 dueDays=1，startedAt=10天前 → 全部逾期（累计 due=3 + grace=2 = 5 < 10）
            seedStep(6651L, SEQ_ID, 1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
            seedStep(6652L, SEQ_ID, 2, "EMAIL", ErpCrmConstants.STEP_COMPLETION_EMAIL_OPENED, false);
            seedStep(6653L, SEQ_ID, 3, "MEETING", ErpCrmConstants.STEP_COMPLETION_MEETING_HELD, false);
            seedAssignmentRule(RULE_ID, SEQ_ID, "LEAD_SOURCE", "{\"sourceId\":[101]}", 10);

            ErpCrmLead lead = newLead(leadId, "LEAD-OVERDUE-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setSourceId(101L);
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
        assertEquals(leadId, toLong(first.get("leadId")));
        int overdueCount = ((Number) first.get("overdueStepCount")).intValue();
        assertTrue(overdueCount >= 3, "连续逾期步数 ≥ 3");
    }

    @Test
    public void testGetSequencePerformance() {
        Long leadId = 6007L;
        ormTemplate.runInSession(() -> {
            seedStages();
            seedLostReasons();
            seedSequence(SEQ_ID, "SEQ-PERF", ErpCrmConstants.SEQUENCE_TEMPLATE_NEW_LEAD);
            seedStep(6661L, SEQ_ID, 1, "CALL", ErpCrmConstants.STEP_COMPLETION_CALL_COMPLETED, false);
            seedAssignmentRule(RULE_ID, SEQ_ID, "LEAD_SOURCE", "{\"sourceId\":[101]}", 10);

            ErpCrmLead lead = newLead(leadId, "LEAD-PERF-001", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW);
            lead.setSourceId(101L);
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
        Long leadWon = 6011L;
        Long leadLost = 6012L;
        Long leadActive = 6013L;
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
            saveConvLog(7001L, leadWon, null, STAGE_NEW, t0);
            saveConvLog(7002L, leadWon, STAGE_NEW, STAGE_QUALIFIED, t0.plusDays(2));
            saveConvLog(7003L, leadWon, STAGE_QUALIFIED, STAGE_WON, t0.plusDays(7));
            // lost：NEW → QUALIFIED（在 QUALIFIED 丢失）
            saveConvLog(7004L, leadLost, null, STAGE_NEW, t0);
            saveConvLog(7005L, leadLost, STAGE_NEW, STAGE_QUALIFIED, t0.plusDays(1));
            // active：NEW → QUALIFIED
            saveConvLog(7006L, leadActive, null, STAGE_NEW, t0);
            saveConvLog(7007L, leadActive, STAGE_NEW, STAGE_QUALIFIED, t0.plusDays(3));
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
        assertEquals(funnelReloaded.getId(), toLong(view.get("funnelId")));
        assertNotNull(view.get("stages"), "stages 数组已生成");
        assertEquals(3, ((List<?>) view.get("stages")).size(), "3 阶段可视化");
    }

    @Test
    public void testInvalidPeriodRejects() {
        ApiResponse<?> resp = refreshFunnel(
                LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 1), null, null, null);
        assertEquals(ErpCrmErrors.ERR_FUNNEL_PERIOD_INVALID.getErrorCode(), resp.getCode(),
                "periodStart 晚于 periodEnd → ERR_FUNNEL_PERIOD_INVALID");
    }

    // ---------- rpc helpers ----------

    private ApiResponse<?> assignSequence(Long leadId) {
        return rpc(mutation, "ErpCrmLeadSequenceProgress__assignSequence", Map.of("leadId", leadId));
    }

    private ApiResponse<?> advanceStep(Long progressId, Long eventId) {
        return rpc(mutation, "ErpCrmLeadSequenceProgress__advanceStep",
                Map.of("progressId", progressId, "eventId", eventId));
    }

    private ApiResponse<?> switchSequence(Long leadId, Long newSequenceId) {
        return rpc(mutation, "ErpCrmLeadSequenceProgress__switchSequence",
                Map.of("leadId", leadId, "newSequenceId", newSequenceId));
    }

    private ApiResponse<?> refreshFunnel(LocalDate start, LocalDate end, Long t, Long team, Long src) {
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

    private void saveStage(IEntityDao<ErpCrmStage> dao, Long id, String code, String name, int seq) {
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

    private void seedSequence(Long id, String code, String templateType) {
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

    private void seedStep(Long id, Long sequenceId, int order, String activityType,
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

    private void seedAssignmentRule(Long id, Long sequenceId, String conditionType,
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

    private void seedDefaultAssignmentRule(Long id, Long sequenceId) {
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

    private ErpCrmLead newLead(Long id, String code, String leadType, String docStatus) {
        ErpCrmLead lead = new ErpCrmLead();
        lead.setId(id);
        lead.setCode(code);
        lead.setOrgId(ORG_ID);
        lead.setLeadType(leadType);
        lead.setDocStatus(docStatus);
        lead.setContactName("联系人" + id);
        return lead;
    }

    private ErpCrmLead newLead(Long id, String code, String leadType, String docStatus,
                                BigDecimal revenue, int probability, Long lostReasonId) {
        ErpCrmLead lead = newLead(id, code, leadType, docStatus);
        lead.setExpectedRevenue(revenue);
        lead.setProbability(probability);
        lead.setLostReasonId(lostReasonId);
        return lead;
    }

    private ErpCrmEvent newCompletedEvent(Long leadId, String eventType, String code) {
        ErpCrmEvent event = new ErpCrmEvent();
        event.setId(System.nanoTime());
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

    private void saveConvLog(Long id, Long leadId, Long fromStage, Long toStage, LocalDateTime changedAt) {
        ErpCrmLeadConvLog log = new ErpCrmLeadConvLog();
        log.setId(id);
        log.setLeadId(leadId);
        log.setFromStageId(fromStage);
        log.setToStageId(toStage);
        log.setChangedAt(Timestamp.valueOf(changedAt));
        daoProvider.daoFor(ErpCrmLeadConvLog.class).saveEntity(log);
    }

    // ---------- reload helpers ----------

    private ErpCrmLeadSequenceProgress reloadActiveProgress(Long leadId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("leadId", leadId));
        q.addFilter(eq("status", ErpCrmConstants.SEQUENCE_PROGRESS_IN_PROGRESS));
        q.setLimit(1);
        return progressDao().findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpCrmLeadSequenceProgress reloadProgress(Long progressId) {
        return progressDao().getEntityById(progressId);
    }

    private ErpCrmEvent findPlannedEvent(Long leadId, String eventType) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("relatedLeadId", leadId));
        q.addFilter(eq("eventType", eventType));
        q.setLimit(1);
        return eventDao().findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private ErpCrmLeadFunnel reloadFunnel(LocalDate start, LocalDate end, Long t, Long team, Long src) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodStart", start));
        q.addFilter(eq("periodEnd", end));
        q.setLimit(1);
        return funnelDao().findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private List<ErpCrmLeadFunnel> loadAllFunnels(LocalDate start, LocalDate end, Long t, Long team, Long src) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("periodStart", start));
        q.addFilter(eq("periodEnd", end));
        return funnelDao().findAllByQuery(q);
    }

    private List<ErpCrmFunnelStageMetrics> loadStageMetrics(Long funnelId) {
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

    private Long toLong(Object value) {
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).longValue();
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
