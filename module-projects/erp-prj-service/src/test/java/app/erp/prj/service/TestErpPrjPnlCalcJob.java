package app.erp.prj.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.md.service.ErpMdConstants;
import app.erp.prj.dao.entity.ErpPrjBilling;
import app.erp.prj.dao.entity.ErpPrjBudget;
import app.erp.prj.dao.entity.ErpPrjBudgetLine;
import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectPnl;
import app.erp.prj.dao.entity.ErpPrjProjectType;
import app.erp.prj.service.job.ErpPrjProjectPnlCalcHelper;
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
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 项目损益汇总定时批量计算（plan 2026-08-14-2304-3 Phase 3，P1-RC-053 nop-job 调度接线）测试。
 *
 * <p>batch 任务级执行 {@code /nop/batch-task/prj/pnl-calc.batch.xml}（经
 * {@link IBatchTaskRunner#execute}，nop-batch-dsl 执行入口，参照 R1.23 {@code TestErpCrmLeadScoringRecalcJob}）：
 * <ul>
 *   <li>① 批任务执行后 active 项目（DRAFT/OPEN/ON_HOLD）生成/更新 {@link ErpPrjProjectPnl}
 *       （calcStatus=CALCULATED + Billing 收入 + CostCollection 四类成本数值断言，镜像
 *       {@code TestErpPrjProjectPnl} 断言）；**真实执行路径下 {@code batchChunkCtx.serviceContext} 为 null**，
 *       helper 内 {@code ServiceContextImpl} 兜底（R1.23 潜伏缺陷回归证实不 NPE）</li>
 *   <li>② loader 过滤：COMPLETED/CANCELLED 终态项目排除不汇总</li>
 *   <li>③ cron 空值跳过语义（helper 层，{@code erp-prj.pnl-calc-cron} 显式置空 → 跳过 + 零 Pnl 更新）</li>
 *   <li>④ {@code erp-prj.pnl-auto-calc-enabled} 门控关闭（默认 false）跳过语义</li>
 *   <li>⑤ 失败隔离断言：不存在项目（ERR_PROJECT_NOT_REFERENCEABLE）→ REQUIRES_NEW 回滚 + WARN
 *       不阻断后续项目汇总（镜像 {@code TestErpCrmLeadScoringRecalcJob} ④ 范式）</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjPnlCalcJob extends JunitAutoTestCase {

    @RegisterExtension
    static PrjFrozenClockExtension frozenClock = new PrjFrozenClockExtension();

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IBatchTaskRunner batchTaskRunner;
    @Inject
    ErpPrjProjectPnlCalcHelper calcHelper;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger helperLogger;

    @BeforeEach
    void attachLogAppender() {
        helperLogger = (Logger) LoggerFactory.getLogger(ErpPrjProjectPnlCalcHelper.class);
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

    /** ① batch 任务级执行：active（OPEN + DRAFT）项目经调度路径生成损益汇总（null ctx 兜底回归）。 */
    @Test
    public void testScheduledBatchCalculatesActiveProjects() {
        final Long[] openProject = new Long[1];
        final Long[] draftProject = new Long[1];
        ormTemplate.runInSession(() -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long subjectId = seedSubject("5101", "项目成本");
            Long projectTypeId = seedProjectType("PT-PNL", "损益", subjectId);
            Long customerId = seedPartner("CUST-PNL", "测试客户");
            openProject[0] = seedProject("PRJ-PNL-B001", "批量损益项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN);
            draftProject[0] = seedProject("PRJ-PNL-B002", "批量空项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_DRAFT);

            seedBilling("B-PNL-B001", openProject[0], customerId, "10000");
            Long ccId = seedCostCollection("CC-PNL-B001", openProject[0]);
            seedCostLine(ccId, ErpPrjConstants.COST_CATEGORY_LABOR, "2000");
            seedCostLine(ccId, ErpPrjConstants.COST_CATEGORY_MATERIAL, "1500");
            seedCostLine(ccId, ErpPrjConstants.COST_CATEGORY_EXPENSE, "1000");
            seedCostLine(ccId, ErpPrjConstants.COST_CATEGORY_SUBCONTRACT, "1500");

            Long budgetId = seedBudget("BG-PNL-B001", openProject[0], "20000");
            seedBudgetLine(budgetId, ErpPrjConstants.COST_CATEGORY_LABOR, "8000", "3000");
        });

        assignAutoCalcEnabled("true");
        try {
            // 真实 nop-batch 执行路径：batchChunkCtx.serviceContext 为 null → helper 兜底 ServiceContextImpl（R1.23 缺陷回归）
            batchTaskRunner.execute("/nop/batch-task/prj/pnl-calc.batch.xml");

            ErpPrjProjectPnl pnl = reloadPnl(openProject[0]);
            assertNotNull(pnl, "OPEN 项目经调度路径应生成损益汇总");
            assertEquals(ErpPrjConstants.PNL_CALC_STATUS_CALCULATED, pnl.getCalcStatus());
            assertEquals(0, pnl.getRevenueAmount().compareTo(new BigDecimal("10000")), "收入合计=10000");
            assertEquals(0, pnl.getCostLabor().compareTo(new BigDecimal("2000")), "人工成本=2000");
            assertEquals(0, pnl.getCostMaterial().compareTo(new BigDecimal("1500")), "物料成本=1500");
            assertEquals(0, pnl.getCostExpense().compareTo(new BigDecimal("1000")), "费用成本=1000");
            assertEquals(0, pnl.getCostSubcontract().compareTo(new BigDecimal("1500")), "分包成本=1500");
            assertEquals(0, pnl.getTotalCost().compareTo(new BigDecimal("6000")), "成本合计=6000");
            assertEquals(0, pnl.getGrossProfit().compareTo(new BigDecimal("4000")), "毛利=4000");
            assertEquals(0, pnl.getGrossMarginPct().compareTo(new BigDecimal("40.0000")), "毛利率=40%");
            assertEquals(0, pnl.getCommittedCost().compareTo(new BigDecimal("3000")), "已承诺成本=3000");
            assertEquals(0, pnl.getBudgetAmount().compareTo(new BigDecimal("20000")), "预算=20000");
            assertEquals(0, pnl.getForecastCompleteCost().compareTo(new BigDecimal("23000")), "EAC=23000");

            ErpPrjProjectPnl empty = reloadPnl(draftProject[0]);
            assertNotNull(empty, "DRAFT 项目也在 loader 范围（空数据应产出零值汇总）");
            assertEquals(0, empty.getRevenueAmount().compareTo(BigDecimal.ZERO), "空项目收入=0");
            assertEquals(ErpPrjConstants.PNL_CALC_STATUS_CALCULATED, empty.getCalcStatus());
        } finally {
            assignAutoCalcEnabled("false");
        }
    }

    /** ② loader 过滤：COMPLETED/CANCELLED 终态项目被排除不汇总。 */
    @Test
    public void testTerminalProjectsExcludedByLoader() {
        final Long[] completed = new Long[1];
        final Long[] cancelled = new Long[1];
        ormTemplate.runInSession(() -> {
            Long subjectId = seedSubject("5102", "项目成本2");
            Long projectTypeId = seedProjectType("PT-PNL-T", "终态", subjectId);
            completed[0] = seedProject("PRJ-PNL-T001", "已完成项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_COMPLETED);
            cancelled[0] = seedProject("PRJ-PNL-T002", "已取消项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_CANCELLED);
        });

        assignAutoCalcEnabled("true");
        try {
            batchTaskRunner.execute("/nop/batch-task/prj/pnl-calc.batch.xml");

            assertEquals(0, countPnlForProject(completed[0]), "COMPLETED 终态项目不汇总");
            assertEquals(0, countPnlForProject(cancelled[0]), "CANCELLED 终态项目不汇总");
        } finally {
            assignAutoCalcEnabled("false");
        }
    }

    /** ③ cron 空值跳过语义：{@code erp-prj.pnl-calc-cron} 显式置空（auto-calc-enabled=true）时 helper 跳过（INFO，不汇总）。 */
    @Test
    public void testCronEmptySkipsHelperCalc() {
        final Long[] project = new Long[1];
        ormTemplate.runInSession(() -> {
            Long subjectId = seedSubject("5103", "项目成本3");
            Long projectTypeId = seedProjectType("PT-PNL-C", "cron空", subjectId);
            project[0] = seedProject("PRJ-PNL-C001", "cron空项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN);
        });

        assignAutoCalcEnabled("true");
        AppConfig.getConfigProvider().assignConfigValue(ErpPrjConstants.CONFIG_PNL_CALC_CRON, "");
        try {
            boolean ok = ormTemplate.runInSession(session -> calcHelper.recalculateOne(project[0], CTX));
            assertTrue(ok, "cron 空值跳过应视为成功（不抛错）");
            assertEquals(0, countPnlForProject(project[0]), "pnl-calc-cron 空值 → 跳过汇总");
            ILoggingEvent skipLog = findLog("erp-prj-pnl-calc-skipped-by-config");
            assertNotNull(skipLog, "跳过应记录 INFO 日志（可观测）");
        } finally {
            assignAutoCalcEnabled("false");
            AppConfig.getConfigProvider().assignConfigValue(ErpPrjConstants.CONFIG_PNL_CALC_CRON, "0 0 1 * * ?");
        }
    }

    /** ④ auto-calc-enabled 门控关闭（默认 false）跳过语义：cron 默认非空但总开关关闭 → 跳过。 */
    @Test
    public void testAutoCalcDisabledSkipsHelperCalc() {
        final Long[] project = new Long[1];
        ormTemplate.runInSession(() -> {
            Long subjectId = seedSubject("5104", "项目成本4");
            Long projectTypeId = seedProjectType("PT-PNL-D", "门控关", subjectId);
            project[0] = seedProject("PRJ-PNL-D001", "门控关闭项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN);
        });

        boolean ok = ormTemplate.runInSession(session -> calcHelper.recalculateOne(project[0], CTX));
        assertTrue(ok, "门控关闭跳过应视为成功（不抛错）");
        assertEquals(0, countPnlForProject(project[0]), "pnl-auto-calc-enabled=false → 跳过汇总");
        ILoggingEvent skipLog = findLog("erp-prj-pnl-calc-skipped-by-config");
        assertNotNull(skipLog, "门控关闭跳过应记录 INFO 日志（可观测）");
    }

    /** ⑤ 失败隔离：不存在项目（ERR_PROJECT_NOT_REFERENCEABLE）→ REQUIRES_NEW 回滚 + WARN，不阻断后续项目汇总。 */
    @Test
    public void testPerItemFailureIsolation() {
        final Long[] project = new Long[1];
        ormTemplate.runInSession(() -> {
            Long subjectId = seedSubject("5105", "项目成本5");
            Long projectTypeId = seedProjectType("PT-PNL-F", "隔离", subjectId);
            Long customerId = seedPartner("CUST-PNL-F", "隔离客户");
            project[0] = seedProject("PRJ-PNL-F001", "隔离项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN);
            seedBilling("B-PNL-F001", project[0], customerId, "5000");
        });

        assignAutoCalcEnabled("true");
        try {
            // 失败项目（不存在）→ REQUIRES_NEW 回滚 + WARN，不抛出
            boolean failed = ormTemplate.runInSession(session -> calcHelper.recalculateOne(999999L, CTX));
            assertFalse(failed, "不存在的项目汇总失败应返回 false（隔离）");
            ILoggingEvent warnLog = findLog("erp-prj-pnl-calc-failed");
            assertNotNull(warnLog, "失败应记录 WARN 日志（显式可观测）");
            assertTrue(warnLog.getFormattedMessage().contains("999999"), "WARN 日志含失败项目 projectId");

            // 失败后其余项目继续汇总
            boolean ok = ormTemplate.runInSession(session -> calcHelper.recalculateOne(project[0], CTX));
            assertTrue(ok, "失败项目之后其余项目正常汇总");
            assertNotNull(reloadPnl(project[0]), "隔离语义：失败不阻断后续项目");
        } finally {
            assignAutoCalcEnabled("false");
        }
    }

    // ---------- helpers ----------

    private void assignAutoCalcEnabled(String value) {
        AppConfig.getConfigProvider().assignConfigValue(
                ErpPrjConstants.CONFIG_PNL_AUTO_CALC_ENABLED, value);
    }

    private ErpPrjProjectPnl reloadPnl(Long projectId) {
        IEntityDao<ErpPrjProjectPnl> dao = daoProvider.daoFor(ErpPrjProjectPnl.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("projectId", projectId));
        q.addOrderField("periodTo", true);
        q.setLimit(1);
        return dao.findAllByQuery(q).stream().findFirst().orElse(null);
    }

    private int countPnlForProject(Long projectId) {
        IEntityDao<ErpPrjProjectPnl> dao = daoProvider.daoFor(ErpPrjProjectPnl.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("projectId", projectId));
        return dao.findAllByQuery(q).size();
    }

    private ILoggingEvent findLog(String marker) {
        for (ILoggingEvent event : logAppender.list) {
            if (event.getFormattedMessage().contains(marker)) {
                return event;
            }
        }
        return null;
    }

    private void seedBilling(String code, Long projectId, Long customerId, String amountFunctional) {
        IEntityDao<ErpPrjBilling> dao = daoProvider.daoFor(ErpPrjBilling.class);
        ErpPrjBilling b = new ErpPrjBilling();
        b.setCode(code);
        b.setProjectId(projectId);
        b.setOrgId(1L);
        b.setCustomerId(customerId);
        b.setBusinessDate(LocalDate.of(2026, 6, 15));
        b.setCurrencyId(1L);
        b.setExchangeRate(BigDecimal.ONE);
        b.setTotalAmount(new BigDecimal(amountFunctional));
        b.setAmountFunctional(new BigDecimal(amountFunctional));
        b.setDocStatus(ErpPrjConstants.DOC_STATUS_APPROVED);
        b.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(b);
    }

    private Long seedCostCollection(String code, Long projectId) {
        IEntityDao<ErpPrjCostCollection> dao = daoProvider.daoFor(ErpPrjCostCollection.class);
        ErpPrjCostCollection cc = new ErpPrjCostCollection();
        cc.setCode(code);
        cc.setProjectId(projectId);
        cc.setOrgId(1L);
        cc.setBusinessDate(LocalDate.of(2026, 6, 15));
        cc.setCurrencyId(1L);
        cc.setTotalAmount(BigDecimal.ZERO);
        cc.setDocStatus(ErpPrjConstants.DOC_STATUS_APPROVED);
        cc.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_APPROVED);
        cc.setPosted(false);
        cc.setExchangeRate(BigDecimal.ONE);
        cc.setAmountSource(BigDecimal.ZERO);
        cc.setAmountFunctional(BigDecimal.ZERO);
        dao.saveEntity(cc);
        return cc.getId();
    }

    private void seedCostLine(Long costCollectionId, String category, String amount) {
        IEntityDao<ErpPrjCostCollectionLine> dao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        ErpPrjCostCollectionLine line = new ErpPrjCostCollectionLine();
        line.setCostCollectionId(costCollectionId);
        line.setLineNo(1);
        line.setCostCategory(category);
        line.setAmount(new BigDecimal(amount));
        dao.saveEntity(line);
    }

    private Long seedBudget(String code, Long projectId, String totalAmount) {
        IEntityDao<ErpPrjBudget> dao = daoProvider.daoFor(ErpPrjBudget.class);
        ErpPrjBudget bg = new ErpPrjBudget();
        bg.setCode(code);
        bg.setProjectId(projectId);
        bg.setOrgId(1L);
        bg.setBusinessDate(LocalDate.of(2026, 7, 1));
        bg.setCurrencyId(1L);
        bg.setTotalAmount(new BigDecimal(totalAmount));
        bg.setDocStatus(ErpPrjConstants.DOC_STATUS_APPROVED);
        bg.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_APPROVED);
        dao.saveEntity(bg);
        return bg.getId();
    }

    private void seedBudgetLine(Long budgetId, String category, String planned, String committed) {
        IEntityDao<ErpPrjBudgetLine> dao = daoProvider.daoFor(ErpPrjBudgetLine.class);
        ErpPrjBudgetLine line = new ErpPrjBudgetLine();
        line.setBudgetId(budgetId);
        line.setLineNo(1);
        line.setCostCategory(category);
        line.setPlannedAmount(new BigDecimal(planned));
        line.setCommittedAmount(new BigDecimal(committed));
        line.setActualAmount(BigDecimal.ZERO);
        dao.saveEntity(line);
    }

    private Long seedProject(String code, String name, Long projectTypeId, String status) {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        ErpPrjProject p = new ErpPrjProject();
        p.setCode(code);
        p.setName(name);
        p.setOrgId(1L);
        p.setProjectTypeId(projectTypeId);
        p.setCurrencyId(1L);
        p.setStatus(status);
        p.setBudget(new BigDecimal("100000"));
        p.setActualCost(BigDecimal.ZERO);
        dao.saveEntity(p);
        return p.getId();
    }

    private Long seedProjectType(String code, String name, Long defaultSubjectId) {
        IEntityDao<ErpPrjProjectType> dao = daoProvider.daoFor(ErpPrjProjectType.class);
        ErpPrjProjectType t = new ErpPrjProjectType();
        t.setCode(code);
        t.setName(name);
        t.setDefaultSubjectId(defaultSubjectId);
        dao.saveEntity(t);
        return t.getId();
    }

    private Long seedPartner(String code, String name) {
        IEntityDao<ErpMdPartner> dao = daoProvider.daoFor(ErpMdPartner.class);
        ErpMdPartner p = new ErpMdPartner();
        p.setCode(code);
        p.setName(name);
        p.setPartnerType("CUSTOMER");
        p.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);
        dao.saveEntity(p);
        return p.getId();
    }

    private Long seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass("EXPENSE");
        s.setDirection(ErpFinConstants.DC_DEBIT);
        s.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);
        dao.saveEntity(s);
        return s.getId();
    }

    private void seedAcctSchema(long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature("FINANCIAL");
        schema.setFunctionalCurrencyId(1L);
        schema.setStatus(ErpMdConstants.ACTIVE_STATUS_ACTIVE);
        dao.saveEntity(schema);
    }

    private void seedOpenPeriod(String code) {
        IEntityDao<ErpFinAccountingPeriod> dao = daoProvider.daoFor(ErpFinAccountingPeriod.class);
        ErpFinAccountingPeriod period = new ErpFinAccountingPeriod();
        period.setCode(code);
        period.setName(code);
        period.setOrgId(1L);
        period.setYear(2026);
        period.setMonth(7);
        period.setStartDate(LocalDate.of(2026, 7, 1));
        period.setEndDate(LocalDate.of(2026, 7, 31));
        period.setStatus(ErpFinConstants.PERIOD_STATUS_OPEN);
        dao.saveEntity(period);
    }
}
