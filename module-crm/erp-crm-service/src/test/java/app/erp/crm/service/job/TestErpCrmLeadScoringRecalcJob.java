package app.erp.crm.service.job;

import app.erp.crm.dao.entity.ErpCrmLead;
import app.erp.crm.dao.entity.ErpCrmLeadScore;
import app.erp.crm.dao.entity.ErpCrmLeadScoreConfig;
import app.erp.crm.dao.entity.ErpCrmLeadScoreConfigLine;
import app.erp.crm.dao.entity.ErpCrmLeadScoreLine;
import app.erp.crm.dao.entity.ErpCrmStage;
import app.erp.crm.service.ErpCrmConstants;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.batch.dsl.runner.IBatchTaskRunner;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 线索评分定时批量重算（plan 2026-08-14-1815-2 Phase 3，P1-RC-035 SCHEDULED 触发器接线）测试。
 *
 * <p>batch 任务级执行 {@code /nop/batch-task/crm/lead-scoring-recalc.batch.xml}（经
 * {@link IBatchTaskRunner#execute}，nop-batch-dsl 执行入口，参照 {@code TestBatchTaskRunner}）：
 * <ul>
 *   <li>① SCHEDULED 触发后 active 线索生成 {@link ErpCrmLeadScore} 记录（triggerEvent=SCHEDULED，
 *       镜像 {@code TestErpCrmForecastAndScoring} 断言）</li>
 *   <li>② 终态线索（CONVERTED/LOST/CANCELLED）被 loader 排除不评分</li>
 *   <li>③ cron 空值跳过语义（helper 层，schedule-cron 空 → 跳过）</li>
 *   <li>④ 失败隔离断言：单条失败线索（ERR_LEAD_NOT_FOUND）WARN 记录不阻断其余线索评分
 *       （镜像 {@code TestErpFinBankReconAutoReverseJob} ⑤ CLOSED 候选隔离断言范式）</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmLeadScoringRecalcJob extends JunitAutoTestCase {
    static final Long ORG_ID = 1601L;
    static final Long SCORE_CONFIG_ID = 6201L;

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IBatchTaskRunner batchTaskRunner;
    @Inject
    ErpCrmLeadScoringRecalcHelper recalcHelper;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger helperLogger;

    @BeforeEach
    void attachLogAppender() {
        helperLogger = (Logger) LoggerFactory.getLogger(ErpCrmLeadScoringRecalcHelper.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        helperLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        if (helperLogger != null && logAppender != null) {
            helperLogger.detachAppender(logAppender);
            logAppender.stop();
        }
    }

    /** ① batch 任务级执行：active（NEW）线索经 SCHEDULED 触发生成评分记录（append-only + 行级快照）。 */
    @Test
    public void testScheduledBatchScoresActiveLeads() {
        final Long[] leadA = new Long[1];
        final Long[] leadB = new Long[1];
        ormTemplate.runInSession(() -> {
            seedStage(6101L, "STG-B", "已接触", 10, 50);
            seedScoreConfig(SCORE_CONFIG_ID, "批量评分", 70, 30);
            seedConfigLine(6211L, SCORE_CONFIG_ID, "JOB_TITLE", "职位层级", 50,
                    ErpCrmConstants.SCORING_METHOD_LOOKUP, "jobTitle",
                    "[{\"value\":\"C-level\",\"label\":\"C-level\",\"score\":15},{\"value\":\"Manager\",\"label\":\"经理\",\"score\":5}]",
                    15, 10);
            seedConfigLine(6212L, SCORE_CONFIG_ID, "COMPANY_NAME", "公司名称", 50,
                    ErpCrmConstants.SCORING_METHOD_BOOLEAN, "companyName",
                    "[{\"value\":\"Acme Corp\"}]", 10, 20);

            leadA[0] = seedLead(6301L, "LEAD-BATCH-A", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW, "C-level", "Acme Corp");
            leadB[0] = seedLead(6302L, "LEAD-BATCH-B", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW, "Manager", "Acme Corp");
        });

        batchTaskRunner.execute("/nop/batch-task/crm/lead-scoring-recalc.batch.xml");

        ErpCrmLeadScore scoreA = reloadScore(leadA[0]);
        assertNotNull(scoreA, "SCHEDULED 触发后 active 线索应生成评分记录");
        assertEquals(ErpCrmConstants.TRIGGER_EVENT_SCHEDULED, scoreA.getTriggerEvent(), "triggerEvent=SCHEDULED");
        assertEquals(100, scoreA.getTotalScore(), "LOOKUP(15×50)+BOOLEAN(10×50) 满分 → totalScore=100");
        assertEquals(1, loadScores(leadA[0]).size(), "append-only：单次 batch 执行各线索 1 条记录");

        ErpCrmLeadScore scoreB = reloadScore(leadB[0]);
        assertNotNull(scoreB, "第二条 active 线索也应被评分");
        assertEquals(ErpCrmConstants.TRIGGER_EVENT_SCHEDULED, scoreB.getTriggerEvent(), "triggerEvent=SCHEDULED");
        assertEquals(2, loadScoreLines(scoreA.getId()).size(), "2 条评分准则 = 2 行快照");
    }

    /** ② loader 过滤：终态线索（CONVERTED/LOST/CANCELLED）被排除不评分。 */
    @Test
    public void testTerminalLeadsExcludedByLoader() {
        final Long[] converted = new Long[1];
        final Long[] lost = new Long[1];
        final Long[] cancelled = new Long[1];
        ormTemplate.runInSession(() -> {
            seedScoreConfig(SCORE_CONFIG_ID, "批量评分-终态", 70, 30);
            seedConfigLine(6213L, SCORE_CONFIG_ID, "JOB_TITLE", "职位层级", 50,
                    ErpCrmConstants.SCORING_METHOD_LOOKUP, "jobTitle",
                    "[{\"value\":\"C-level\",\"label\":\"C-level\",\"score\":15}]", 15, 10);
            converted[0] = seedLead(6401L, "LEAD-TERM-CONV", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_CONVERTED, "C-level", "Acme Corp");
            lost[0] = seedLead(6402L, "LEAD-TERM-LOST", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_LOST, "C-level", "Acme Corp");
            cancelled[0] = seedLead(6403L, "LEAD-TERM-CANCEL", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_CANCELLED, "C-level", "Acme Corp");
        });

        batchTaskRunner.execute("/nop/batch-task/crm/lead-scoring-recalc.batch.xml");

        assertEquals(0, loadScores(converted[0]).size(), "CONVERTED 终态线索不评分");
        assertEquals(0, loadScores(lost[0]).size(), "LOST 终态线索不评分");
        assertEquals(0, loadScores(cancelled[0]).size(), "CANCELLED 终态线索不评分");
    }

    /** ③ cron 空值跳过语义：schedule-cron 配置为空时 helper 跳过（INFO 日志，不评分）。 */
    @Test
    public void testCronEmptySkipsHelperRecalc() {
        final Long[] lead = new Long[1];
        ormTemplate.runInSession(() -> {
            seedScoreConfig(SCORE_CONFIG_ID, "批量评分-cron空", 70, 30);
            seedConfigLine(6214L, SCORE_CONFIG_ID, "JOB_TITLE", "职位层级", 50,
                    ErpCrmConstants.SCORING_METHOD_LOOKUP, "jobTitle",
                    "[{\"value\":\"C-level\",\"label\":\"C-level\",\"score\":15}]", 15, 10);
            lead[0] = seedLead(6501L, "LEAD-CRON-EMPTY", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW, "C-level", "Acme Corp");
        });

        AppConfig.getConfigProvider().assignConfigValue(
                ErpCrmConstants.CONFIG_LEAD_SCORING_SCHEDULE_CRON, "");
        try {
            boolean ok = ormTemplate.runInSession(session -> recalcHelper.recalculateOne(lead[0], CTX));
            assertTrue(ok, "cron 空值跳过应视为成功（不抛错）");
            assertEquals(0, loadScores(lead[0]).size(), "schedule-cron 空值 → 跳过评分");
            ILoggingEvent skipLog = findLog("erp-crm-lead-scoring-recalc-skipped-by-config");
            assertNotNull(skipLog, "跳过应记录 INFO 日志（可观测）");
        } finally {
            AppConfig.getConfigProvider().assignConfigValue(
                    ErpCrmConstants.CONFIG_LEAD_SCORING_SCHEDULE_CRON, "0 2 * * *");
        }
    }

    /** ④ 失败隔离：单条失败线索（ERR_LEAD_NOT_FOUND）WARN 记录不阻断其余线索评分。 */
    @Test
    public void testPerItemFailureIsolation() {
        final Long[] lead = new Long[1];
        ormTemplate.runInSession(() -> {
            seedScoreConfig(SCORE_CONFIG_ID, "批量评分-隔离", 70, 30);
            seedConfigLine(6215L, SCORE_CONFIG_ID, "JOB_TITLE", "职位层级", 50,
                    ErpCrmConstants.SCORING_METHOD_LOOKUP, "jobTitle",
                    "[{\"value\":\"C-level\",\"label\":\"C-level\",\"score\":15}]", 15, 10);
            lead[0] = seedLead(6601L, "LEAD-ISOLATION", ErpCrmConstants.LEAD_TYPE_LEAD,
                    ErpCrmConstants.DOC_STATUS_NEW, "C-level", "Acme Corp");
        });

        // 失败线索（不存在）→ REQUIRES_NEW 回滚 + WARN，不抛出
        boolean failed = ormTemplate.runInSession(session -> recalcHelper.recalculateOne(999999L, CTX));
        assertEquals(false, failed, "不存在的线索评分失败应返回 false（隔离）");
        ILoggingEvent warnLog = findLog("erp-crm-lead-scoring-recalc-failed");
        assertNotNull(warnLog, "失败应记录 WARN 日志（显式可观测）");
        assertTrue(warnLog.getFormattedMessage().contains("999999"), "WARN 日志含失败线索 leadId");

        // 失败后其余线索继续评分
        boolean ok = ormTemplate.runInSession(session -> recalcHelper.recalculateOne(lead[0], CTX));
        assertTrue(ok, "失败线索之后其余线索正常评分");
        assertNotNull(reloadScore(lead[0]), "隔离语义：失败不阻断后续线索");
    }

    // ---------- helpers ----------

    private Long seedLead(Long id, String code, String leadType, String docStatus,
                          String jobTitle, String companyName) {
        ErpCrmLead lead = new ErpCrmLead();
        lead.setId(id);
        lead.setCode(code);
        lead.setOrgId(ORG_ID);
        lead.setLeadType(leadType);
        lead.setDocStatus(docStatus);
        lead.setContactName("联系人" + id);
        lead.setJobTitle(jobTitle);
        lead.setCompanyName(companyName);
        daoProvider.daoFor(ErpCrmLead.class).saveEntity(lead);
        return id;
    }

    private void seedStage(Long id, String code, String name, int sequence, int defaultProbability) {
        ErpCrmStage stage = new ErpCrmStage();
        stage.setId(id);
        stage.setCode(code);
        stage.setStageName(name);
        stage.setSequence(sequence);
        stage.setDefaultProbability(defaultProbability);
        daoProvider.daoFor(ErpCrmStage.class).saveEntity(stage);
    }

    private void seedScoreConfig(Long id, String name, int autoThreshold, int minFollowUp) {
        ErpCrmLeadScoreConfig config = new ErpCrmLeadScoreConfig();
        config.setId(id);
        config.setCode("SCORE-BATCH-" + id);
        config.setOrgId(ORG_ID);
        config.setConfigName(name);
        config.setIsActive(Boolean.TRUE);
        config.setAutoQualifyThreshold(autoThreshold);
        config.setMinScoreForFollowUp(minFollowUp);
        daoProvider.daoFor(ErpCrmLeadScoreConfig.class).saveEntity(config);
    }

    private void seedConfigLine(Long id, Long configId, String code, String name, int weight,
                                String method, String formula, String lookupTable,
                                Integer maxScore, int sequence) {
        ErpCrmLeadScoreConfigLine line = new ErpCrmLeadScoreConfigLine();
        line.setId(id);
        line.setConfigId(configId);
        line.setOrgId(ORG_ID);
        line.setCriterionCode(code);
        line.setCriterionName(name);
        line.setWeight(weight);
        line.setScoringMethod(method);
        line.setFormula(formula);
        line.setLookupTable(lookupTable);
        line.setMaxScore(maxScore);
        line.setSequence(sequence);
        daoProvider.daoFor(ErpCrmLeadScoreConfigLine.class).saveEntity(line);
    }

    private ErpCrmLeadScore reloadScore(Long leadId) {
        IEntityDao<ErpCrmLeadScore> dao = daoProvider.daoFor(ErpCrmLeadScore.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("leadId", leadId));
        q.addOrderField("calculatedAt", true);
        q.setLimit(1);
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private List<ErpCrmLeadScore> loadScores(Long leadId) {
        IEntityDao<ErpCrmLeadScore> dao = daoProvider.daoFor(ErpCrmLeadScore.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("leadId", leadId));
        return dao.findAllByQuery(q);
    }

    private List<ErpCrmLeadScoreLine> loadScoreLines(Long scoreId) {
        IEntityDao<ErpCrmLeadScoreLine> dao = daoProvider.daoFor(ErpCrmLeadScoreLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("scoreId", scoreId));
        return dao.findAllByQuery(q);
    }

    private ILoggingEvent findLog(String marker) {
        for (ILoggingEvent event : logAppender.list) {
            if (event.getFormattedMessage().contains(marker)) {
                return event;
            }
        }
        return null;
    }
}
