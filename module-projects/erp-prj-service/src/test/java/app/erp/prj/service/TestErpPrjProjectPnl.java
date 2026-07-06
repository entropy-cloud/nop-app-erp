package app.erp.prj.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdPartner;
import app.erp.md.service.ErpMdConstants;
import app.erp.prj.biz.IErpPrjProjectPnlBiz;
import app.erp.prj.dao.entity.ErpPrjBilling;
import app.erp.prj.dao.entity.ErpPrjBudget;
import app.erp.prj.dao.entity.ErpPrjBudgetLine;
import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectPnl;
import app.erp.prj.dao.entity.ErpPrjProjectType;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 项目损益汇总计算引擎端到端单测（plan 2026-07-07-0305-1 Phase 4）。验证：
 * <ul>
 *   <li>含 Billing 收入 + 四类成本项目的 refreshPnl：毛利/毛利率数值与手算一致。</li>
 *   <li>空项目产出零行汇总（不报错）。</li>
 *   <li>非法期间（periodFrom 晚于 periodTo）抛 {@link ErpPrjErrors#ERR_PRJ_PNL_PERIOD_INVALID}。</li>
 *   <li>重算幂等：同期间重算不产生重复行，数值更新。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjProjectPnl extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpPrjProjectPnlBiz pnlBiz;

    @Test
    public void testRefreshPnlWithRevenueAndFourCategoryCost() {
        Long[] holder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long subjectId = seedSubject("5101", "项目成本");
            Long projectTypeId = seedProjectType("PT-PNL", "损益", subjectId);
            Long customerId = seedPartner("CUST-PNL", "测试客户");
            Long projectId = seedProject("PRJ-PNL-001", "损益汇总项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN);
            holder[0] = projectId;

            seedBilling("B-PNL-001", projectId, customerId, "10000");
            Long ccId = seedCostCollection("CC-PNL-001", projectId);
            seedCostLine(ccId, ErpPrjConstants.COST_CATEGORY_LABOR, "2000");
            seedCostLine(ccId, ErpPrjConstants.COST_CATEGORY_MATERIAL, "1500");
            seedCostLine(ccId, ErpPrjConstants.COST_CATEGORY_EXPENSE, "1000");
            seedCostLine(ccId, ErpPrjConstants.COST_CATEGORY_SUBCONTRACT, "1500");

            Long budgetId = seedBudget("BG-PNL-001", projectId, "20000");
            seedBudgetLine(budgetId, ErpPrjConstants.COST_CATEGORY_LABOR, "8000", "3000");
            return null;
        });

        ErpPrjProjectPnl pnl = pnlBiz.refreshPnl(holder[0], null, null, CTX);

        assertEquals(0, pnl.getRevenueAmount().compareTo(new BigDecimal("10000")), "收入合计=10000");
        assertEquals(0, pnl.getCostLabor().compareTo(new BigDecimal("2000")), "人工成本=2000");
        assertEquals(0, pnl.getCostMaterial().compareTo(new BigDecimal("1500")), "物料成本=1500");
        assertEquals(0, pnl.getCostExpense().compareTo(new BigDecimal("1000")), "费用成本=1000");
        assertEquals(0, pnl.getCostSubcontract().compareTo(new BigDecimal("1500")), "分包成本=1500");
        assertEquals(0, pnl.getTotalCost().compareTo(new BigDecimal("6000")), "成本合计=6000");
        assertEquals(0, pnl.getGrossProfit().compareTo(new BigDecimal("4000")), "毛利=4000");
        assertEquals("40.0000", pnl.getGrossMarginPct(), "毛利率=40%");
        assertEquals(0, pnl.getCommittedCost().compareTo(new BigDecimal("3000")), "已承诺成本=3000");
        assertEquals(0, pnl.getBudgetAmount().compareTo(new BigDecimal("20000")), "预算=20000");
        // EAC = totalCost + max(budget - committed, 0) = 6000 + 17000 = 23000
        assertEquals(0, pnl.getForecastCompleteCost().compareTo(new BigDecimal("23000")), "EAC=23000");
        assertEquals(ErpPrjConstants.PNL_CALC_STATUS_CALCULATED, pnl.getCalcStatus());
    }

    @Test
    public void testEmptyProjectProducesZeroPnl() {
        Long[] holder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long subjectId = seedSubject("5101", "项目成本");
            Long projectTypeId = seedProjectType("PT-EMPTY", "空项目", subjectId);
            Long projectId = seedProject("PRJ-EMPTY-001", "空损益项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN);
            holder[0] = projectId;
            return null;
        });

        ErpPrjProjectPnl pnl = pnlBiz.refreshPnl(holder[0], null, null, CTX);

        assertEquals(0, pnl.getRevenueAmount().compareTo(BigDecimal.ZERO), "收入=0");
        assertEquals(0, pnl.getTotalCost().compareTo(BigDecimal.ZERO), "成本=0");
        assertEquals(0, pnl.getGrossProfit().compareTo(BigDecimal.ZERO), "毛利=0");
        assertEquals("0", pnl.getGrossMarginPct(), "毛利率=0（收入为零）");
        assertEquals(ErpPrjConstants.PNL_CALC_STATUS_CALCULATED, pnl.getCalcStatus());
    }

    @Test
    public void testInvalidPeriodThrows() {
        Long[] holder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long subjectId = seedSubject("5101", "项目成本");
            Long projectTypeId = seedProjectType("PT-INV", "非法期间", subjectId);
            Long projectId = seedProject("PRJ-INV-001", "非法期间项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN);
            holder[0] = projectId;
            return null;
        });

        NopException ex = assertThrows(NopException.class, () ->
                pnlBiz.refreshPnl(holder[0], LocalDate.of(2026, 7, 31), LocalDate.of(2026, 7, 1), CTX));
        assertEquals(ErpPrjErrors.ERR_PRJ_PNL_PERIOD_INVALID.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testRecalcIsIdempotent() {
        Long[] holder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long subjectId = seedSubject("5101", "项目成本");
            Long projectTypeId = seedProjectType("PT-IDEM", "幂等", subjectId);
            Long customerId = seedPartner("CUST-IDEM", "幂等客户");
            Long projectId = seedProject("PRJ-IDEM-PNL-001", "幂等损益项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN);
            holder[0] = projectId;
            seedBilling("B-IDEM-001", projectId, customerId, "5000");
            return null;
        });

        ErpPrjProjectPnl first = pnlBiz.refreshPnl(holder[0], null, null, CTX);
        ErpPrjProjectPnl second = pnlBiz.refreshPnl(holder[0], null, null, CTX);

        assertEquals(first.getId(), second.getId(), "重算幂等：同期间不产生重复行");
        assertEquals(1, countPnlForProject(holder[0]), "仅一行汇总");
        assertEquals(0, second.getRevenueAmount().compareTo(new BigDecimal("5000")), "收入数值保持一致");
    }

    // ---------- seed helpers ----------

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
        cc.setExchangeRate("1");
        cc.setAmountSource("0");
        cc.setAmountFunctional("0");
        dao.saveEntity(cc);
        return cc.getId();
    }

    private void seedCostLine(Long costCollectionId, String category, String amount) {
        IEntityDao<ErpPrjCostCollectionLine> dao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        ErpPrjCostCollectionLine line = new ErpPrjCostCollectionLine();
        line.setCostCollectionId(costCollectionId);
        line.setLineNo((int) (Math.random() * 1000) + 1);
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

    private int countPnlForProject(Long projectId) {
        IEntityDao<ErpPrjProjectPnl> dao = daoProvider.daoFor(ErpPrjProjectPnl.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("projectId", projectId));
        return dao.findAllByQuery(q).size();
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
        IEntityDao<app.erp.md.dao.entity.ErpMdSubject> dao = daoProvider.daoFor(app.erp.md.dao.entity.ErpMdSubject.class);
        app.erp.md.dao.entity.ErpMdSubject s = new app.erp.md.dao.entity.ErpMdSubject();
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
