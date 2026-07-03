package app.erp.prj.service;

import app.erp.fin.biz.IErpFinExpenseClaimBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.dao.entity.ErpFinExpenseClaimLine;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdEmployee;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.prj.biz.IErpPrjCostCollectionBiz;
import app.erp.prj.biz.IErpPrjProjectBiz;
import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.dao.entity.ErpPrjProjectType;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.query.QueryBean;
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

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 费用报销归集端到端单测（Phase 3）。验证 projects 驱动只读聚合（对齐 data-dependency-matrix §3.2/§4.2）：
 * <ul>
 *   <li>报销单 approve（行带 projectId）→ projects {@code refreshExpenseCost} → 归集行生成
 *       （costCategory=EXPENSE/sourceBillType=EXPENSE）+ actualCost 回写。</li>
 *   <li>幂等去重：重复 refreshExpenseCost 不重复归集。</li>
 *   <li>config-gated：{@code erp-prj.expense-aggregation-enabled=false} 时 refreshExpenseCost 返回 0。</li>
 *   <li>{@code closeProject} 关闭前强制刷新费用归集（actualCost 含费用）。</li>
 * </ul>
 *
 * <p>报销单经 finance-service {@link IErpFinExpenseClaimBiz} 直接构造（已审核 APPROVED+未作废），
 * projects 侧 {@link IErpPrjCostCollectionBiz#refreshExpenseCost} 只读查 + 自写归集表。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjExpenseAggregation extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    /** finance 域 APPROVED=30 / DRAFT=10 / CANCELLED=50。 */
    private static final int FIN_APPROVE_STATUS_APPROVED = 30;
    private static final int FIN_DOC_STATUS_DRAFT = 10;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpPrjProjectBiz projectBiz;
    @Inject
    IErpPrjCostCollectionBiz collectionBiz;

    @Test
    public void testRefreshExpenseCostAggregatesFromApprovedClaim() {
        Long[] projectHolder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long expenseSubjectId = seedSubject("6602", "管理费用");
            Long projectTypeId = seedProjectType("PT-RD", "研发", expenseSubjectId);
            Long projectId = seedProject("PRJ-EXP-001", "费用归集项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN);
            projectHolder[0] = projectId;
            Long claimantId = seedEmployee();
            // 报销单 100（不含税），行标 projectId
            seedApprovedClaimWithProjectLine("EC-EXP-001", claimantId, projectId, expenseSubjectId,
                    new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
            return null;
        });

        BigDecimal added = collectionBiz.refreshExpenseCost(projectHolder[0], CTX);
        assertEquals(0, added.compareTo(new BigDecimal("100")), "新增归集=不含税金额 100");

        // 归集行已生成
        ErpPrjCostCollectionLine line = findCollectionLine(
                ErpPrjConstants.SOURCE_BILL_TYPE_EXPENSE, "EC-EXP-001");
        assertTrue(line != null, "EXPENSE 归集行已生成");
        assertEquals(ErpPrjConstants.COST_CATEGORY_EXPENSE, line.getCostCategory());
        assertEquals("EC-EXP-001", line.getSourceBillCode());
        assertEquals(0, line.getAmount().compareTo(new BigDecimal("100.00")));

        // actualCost 已回写
        ErpPrjProject project = daoProvider.daoFor(ErpPrjProject.class).getEntityById(projectHolder[0]);
        assertEquals(0, project.getActualCost().compareTo(new BigDecimal("100.00")),
                "项目 actualCost=100");
    }

    @Test
    public void testRefreshExpenseCostIsIdempotent() {
        Long[] projectHolder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long expenseSubjectId = seedSubject("6602", "管理费用");
            Long projectTypeId = seedProjectType("PT-RD", "研发", expenseSubjectId);
            Long projectId = seedProject("PRJ-EXP-002", "幂等项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN);
            projectHolder[0] = projectId;
            Long claimantId = seedEmployee();
            seedApprovedClaimWithProjectLine("EC-EXP-002", claimantId, projectId, expenseSubjectId,
                    new BigDecimal("200"), new BigDecimal("26"), new BigDecimal("226"));
            return null;
        });

        collectionBiz.refreshExpenseCost(projectHolder[0], CTX);
        BigDecimal secondCall = collectionBiz.refreshExpenseCost(projectHolder[0], CTX);
        assertEquals(0, secondCall.compareTo(BigDecimal.ZERO), "第二次调用无新增（幂等去重）");

        List<ErpPrjCostCollectionLine> lines = findAllCollectionLines(
                ErpPrjConstants.SOURCE_BILL_TYPE_EXPENSE, "EC-EXP-002");
        assertEquals(1, lines.size(), "仅一条归集行（幂等）");
    }

    @Test
    public void testExpenseAggregationDisabledByConfig() {
        System.setProperty(ErpPrjConstants.CONFIG_EXPENSE_AGGREGATION_ENABLED, "false");
        try {
            Long[] projectHolder = new Long[1];
            ormTemplate.runInSession(session -> {
                seedOpenPeriod("2026-07");
                seedAcctSchema(1L);
                Long expenseSubjectId = seedSubject("6602", "管理费用");
                Long projectTypeId = seedProjectType("PT-RD", "研发", expenseSubjectId);
                Long projectId = seedProject("PRJ-EXP-003", "禁用项目", projectTypeId,
                        ErpPrjConstants.PROJECT_STATUS_OPEN);
                projectHolder[0] = projectId;
                Long claimantId = seedEmployee();
                seedApprovedClaimWithProjectLine("EC-EXP-003", claimantId, projectId, expenseSubjectId,
                        new BigDecimal("100"), new BigDecimal("13"), new BigDecimal("113"));
                return null;
            });

            BigDecimal added = collectionBiz.refreshExpenseCost(projectHolder[0], CTX);
            assertEquals(0, added.compareTo(BigDecimal.ZERO), "config-gated 关闭时返回 0");
            assertNull(findCollectionLine(ErpPrjConstants.SOURCE_BILL_TYPE_EXPENSE, "EC-EXP-003"),
                    "config-gated 关闭时不生成归集行");
        } finally {
            System.clearProperty(ErpPrjConstants.CONFIG_EXPENSE_AGGREGATION_ENABLED);
        }
    }

    @Test
    public void testCloseProjectRefreshesExpenseBeforeClose() {
        Long[] projectHolder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long expenseSubjectId = seedSubject("6602", "管理费用");
            Long projectTypeId = seedProjectType("PT-RD", "研发", expenseSubjectId);
            Long projectId = seedProject("PRJ-CLOSE-EXP", "关闭前刷新项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN);
            projectHolder[0] = projectId;
            Long claimantId = seedEmployee();
            seedApprovedClaimWithProjectLine("EC-CLOSE-EXP", claimantId, projectId, expenseSubjectId,
                    new BigDecimal("500"), new BigDecimal("65"), new BigDecimal("565"));
            return null;
        });

        ErpPrjProject closed = projectBiz.closeProject(projectHolder[0], CTX);
        assertEquals(ErpPrjConstants.PROJECT_STATUS_COMPLETED, closed.getStatus());

        // closeProject 前已刷新费用归集 → actualCost 含费用
        ErpPrjProject project = daoProvider.daoFor(ErpPrjProject.class).getEntityById(projectHolder[0]);
        assertEquals(0, project.getActualCost().compareTo(new BigDecimal("500.00")),
                "关闭后 actualCost=费用 500");
        assertFalse(findAllCollectionLines(
                ErpPrjConstants.SOURCE_BILL_TYPE_EXPENSE, "EC-CLOSE-EXP").isEmpty(),
                "关闭前已生成 EXPENSE 归集行");
    }

    // ---------- seed helpers ----------

    private void seedApprovedClaimWithProjectLine(String code, Long claimantId, Long projectId,
                                                  Long subjectId, BigDecimal amountWithoutTax,
                                                  BigDecimal tax, BigDecimal withTax) {
        IEntityDao<ErpFinExpenseClaim> dao = daoProvider.daoFor(ErpFinExpenseClaim.class);
        ErpFinExpenseClaim claim = new ErpFinExpenseClaim();
        claim.setCode(code);
        claim.setOrgId(1L);
        claim.setClaimantId(claimantId);
        claim.setBusinessDate(LocalDate.of(2026, 7, 15));
        claim.setPaymentMode(10);
        claim.setCurrencyId(1L);
        claim.setExchangeRate(BigDecimal.ONE);
        claim.setAmountWithoutTax(amountWithoutTax);
        claim.setTaxAmount(tax);
        claim.setAmountWithTax(withTax);
        claim.setReason("项目费用");
        claim.setDocStatus(FIN_DOC_STATUS_DRAFT);
        claim.setApproveStatus(FIN_APPROVE_STATUS_APPROVED);
        dao.saveEntity(claim);

        IEntityDao<ErpFinExpenseClaimLine> lineDao = daoProvider.daoFor(ErpFinExpenseClaimLine.class);
        ErpFinExpenseClaimLine line = new ErpFinExpenseClaimLine();
        line.setClaimId(claim.getId());
        line.setLineNo(1);
        line.setExpenseType(10);
        line.setProjectId(projectId);
        line.setSubjectId(subjectId);
        line.setAmountWithoutTax(amountWithoutTax);
        line.setTaxAmount(tax);
        line.setAmountWithTax(withTax);
        lineDao.saveEntity(line);
    }

    private Long seedEmployee() {
        IEntityDao<ErpMdEmployee> dao = daoProvider.daoFor(ErpMdEmployee.class);
        ErpMdEmployee emp = new ErpMdEmployee();
        emp.setCode("EMP-" + System.nanoTime());
        emp.setName("测试员工");
        emp.setOrgId(1L);
        emp.setStatus(10);
        dao.saveEntity(emp);
        return emp.getId();
    }

    private Long seedProject(String code, String name, Long projectTypeId, int status) {
        IEntityDao<ErpPrjProject> dao = daoProvider.daoFor(ErpPrjProject.class);
        ErpPrjProject p = new ErpPrjProject();
        p.setCode(code);
        p.setName(name);
        p.setOrgId(1L);
        p.setProjectTypeId(projectTypeId);
        p.setCurrencyId(1L);
        p.setStatus(status);
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

    private Long seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass(10);
        s.setDirection(10);
        s.setStatus(10);
        dao.saveEntity(s);
        return s.getId();
    }

    private void seedAcctSchema(long orgId) {
        IEntityDao<ErpMdAcctSchema> dao = daoProvider.daoFor(ErpMdAcctSchema.class);
        ErpMdAcctSchema schema = new ErpMdAcctSchema();
        schema.setCode("AS-" + orgId);
        schema.setName("账套-" + orgId);
        schema.setOrgId(orgId);
        schema.setNature(10);
        schema.setFunctionalCurrencyId(1L);
        schema.setStatus(10);
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
        period.setStatus(10);
        dao.saveEntity(period);
    }

    private ErpPrjCostCollectionLine findCollectionLine(String sourceBillType, String sourceBillCode) {
        List<ErpPrjCostCollectionLine> lines = findAllCollectionLines(sourceBillType, sourceBillCode);
        return lines.isEmpty() ? null : lines.get(0);
    }

    private List<ErpPrjCostCollectionLine> findAllCollectionLines(String sourceBillType, String sourceBillCode) {
        IEntityDao<ErpPrjCostCollectionLine> dao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(eq("sourceBillType", sourceBillType), eq("sourceBillCode", sourceBillCode)));
        return dao.findAllByQuery(q);
    }
}
