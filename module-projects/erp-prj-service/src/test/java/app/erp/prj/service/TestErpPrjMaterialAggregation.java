package app.erp.prj.service;

import app.erp.fin.dao.entity.ErpFinAccountingPeriod;
import app.erp.fin.service.ErpFinConstants;
import app.erp.md.dao.entity.ErpMdAcctSchema;
import app.erp.md.dao.entity.ErpMdSubject;
import app.erp.md.service.ErpMdConstants;
import app.erp.prj.biz.IErpPrjCostCollectionBiz;
import app.erp.prj.biz.IErpPrjProjectPnlBiz;
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

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 物料（采购入库→项目）成本归集端到端单测（RC-R1.61 / P1-RC-049）。验证
 * {@link IErpPrjCostCollectionBiz#aggregateMaterialCost} 跨域 Facade 契约（projects 侧守卫 + 写入）：
 * <ul>
 *   <li>归集行生成：costCategory=MATERIAL / sourceBillType=PURCHASE_RECEIVE / amount=入库行金额(不含税)
 *       + head totalAmount 累加 + project.actualCost 增量回写。</li>
 *   <li>幂等去重：重复调用零新增。</li>
 *   <li>项目 null 跳过（返回 0 零写入）。</li>
 *   <li>预算检查（P1-RC-051 merge）：STRICT 超预算抛 {@link ErpPrjErrors#ERR_BUDGET_EXCEEDED} /
 *       WARNING 放行。</li>
 *   <li>requireReferenceable 守卫（P2-RC-048 协同）：非 OPEN 项目抛
 *       {@link ErpPrjErrors#ERR_PROJECT_NOT_REFERENCEABLE}。</li>
 *   <li>config-gated：{@code erp-prj.material-aggregation-enabled=false} 时返回 0。</li>
 *   <li>ProjectPnl 四分类聚合：MATERIAL（经生产 Facade）+ LABOR/EXPENSE/SUBCONTRACT seed
 *       → refreshPnl 四类 cost 断言。</li>
 * </ul>
 *
 * <p>SUBCONTRACT 生产 writer 因载体缺失（mfg 委外链零 projectId 维度 + purchase 无分包单据类型）
 * 按 Phase 1 裁决登记 scope 解释（Deferred But Adjudicated），读侧四分类聚合经 seed 断言成立。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjMaterialAggregation extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpPrjCostCollectionBiz collectionBiz;
    @Inject
    IErpPrjProjectPnlBiz pnlBiz;

    @Test
    public void testAggregateMaterialCostWritesCollectionLine() {
        Long[] projectHolder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long subjectId = seedSubject("5101", "项目开发成本");
            Long projectTypeId = seedProjectType("PT-MAT", "研发", subjectId);
            Long projectId = seedProject("PRJ-MAT-001", "物料归集项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN);
            projectHolder[0] = projectId;
            return null;
        });

        BigDecimal added = ormTemplate.runInSession(session -> collectionBiz.aggregateMaterialCost(
                projectHolder[0], new BigDecimal("120.0000"), "PR-MAT-001-1", CTX));
        assertEquals(0, added.compareTo(new BigDecimal("120.0000")), "新增归集=入库行金额 120");

        ErpPrjCostCollectionLine line = findCollectionLine(
                ErpPrjConstants.SOURCE_BILL_TYPE_PURCHASE_RECEIVE, "PR-MAT-001-1");
        assertTrue(line != null, "MATERIAL 归集行已生成");
        assertEquals(ErpPrjConstants.COST_CATEGORY_MATERIAL, line.getCostCategory());
        assertEquals(ErpPrjConstants.SOURCE_BILL_TYPE_PURCHASE_RECEIVE, line.getSourceBillType());
        assertEquals("PR-MAT-001-1", line.getSourceBillCode());
        assertEquals(0, line.getAmount().compareTo(new BigDecimal("120.0000")), "amount=入库行金额不含税");

        // head totalAmount 累加
        ErpPrjCostCollection head = daoProvider.daoFor(ErpPrjCostCollection.class)
                .getEntityById(line.getCostCollectionId());
        assertEquals(0, head.getTotalAmount().compareTo(new BigDecimal("120.0000")), "head totalAmount=120");

        // actualCost 增量回写
        ErpPrjProject project = daoProvider.daoFor(ErpPrjProject.class).getEntityById(projectHolder[0]);
        assertEquals(0, project.getActualCost().compareTo(new BigDecimal("120.0000")),
                "项目 actualCost=120");
    }

    @Test
    public void testAggregateMaterialCostIsIdempotent() {
        Long[] projectHolder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long subjectId = seedSubject("5101", "项目开发成本");
            Long projectTypeId = seedProjectType("PT-MAT", "研发", subjectId);
            Long projectId = seedProject("PRJ-MAT-002", "幂等项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN);
            projectHolder[0] = projectId;
            return null;
        });

        ormTemplate.runInSession(session -> collectionBiz.aggregateMaterialCost(
                projectHolder[0], new BigDecimal("100"), "PR-MAT-002-1", CTX));
        BigDecimal secondCall = ormTemplate.runInSession(session -> collectionBiz.aggregateMaterialCost(
                projectHolder[0], new BigDecimal("100"), "PR-MAT-002-1", CTX));
        assertEquals(0, secondCall.compareTo(BigDecimal.ZERO), "重复调用零新增（幂等去重）");

        List<ErpPrjCostCollectionLine> lines = findAllCollectionLines(
                ErpPrjConstants.SOURCE_BILL_TYPE_PURCHASE_RECEIVE, "PR-MAT-002-1");
        assertEquals(1, lines.size(), "仅一条归集行（幂等）");
    }

    @Test
    public void testNullProjectIdReturnsZero() {
        BigDecimal added = ormTemplate.runInSession(session -> collectionBiz.aggregateMaterialCost(
                null, new BigDecimal("100"), "PR-NULL-1", CTX));
        assertEquals(0, added.compareTo(BigDecimal.ZERO), "projectId null 返回 0");
        assertNull(findCollectionLine(ErpPrjConstants.SOURCE_BILL_TYPE_PURCHASE_RECEIVE, "PR-NULL-1"),
                "projectId null 零写入");
    }

    @Test
    public void testBudgetStrictRejectsOverBudget() {
        System.setProperty(ErpPrjConstants.CONFIG_BUDGET_CONTROL_MODE, "STRICT");
        try {
            Long[] projectHolder = new Long[1];
            ormTemplate.runInSession(session -> {
                seedOpenPeriod("2026-07");
                seedAcctSchema(1L);
                Long subjectId = seedSubject("5101", "项目开发成本");
                Long projectTypeId = seedProjectType("PT-MAT", "研发", subjectId);
                Long projectId = seedProject("PRJ-MAT-S-001", "STRICT 项目", projectTypeId,
                        ErpPrjConstants.PROJECT_STATUS_OPEN);
                daoProvider.daoFor(ErpPrjProject.class).getEntityById(projectId)
                        .setBudget(new BigDecimal("1000"));
                projectHolder[0] = projectId;
                return null;
            });

            // 已使用 0 + 拟新增 1500 > 总预算 1000 → STRICT 抛 ERR_BUDGET_EXCEEDED
            NopException ex = assertThrows(NopException.class, () -> ormTemplate.runInSession(
                    () -> collectionBiz.aggregateMaterialCost(projectHolder[0],
                            new BigDecimal("1500"), "PR-MAT-S-001-1", CTX)));
            assertEquals(ErpPrjErrors.ERR_BUDGET_EXCEEDED.getErrorCode(), ex.getErrorCode());
            assertNull(findCollectionLine(ErpPrjConstants.SOURCE_BILL_TYPE_PURCHASE_RECEIVE,
                    "PR-MAT-S-001-1"), "STRICT 拒绝时不写入归集行");
        } finally {
            System.clearProperty(ErpPrjConstants.CONFIG_BUDGET_CONTROL_MODE);
        }
    }

    @Test
    public void testBudgetWarningAllowsOverBudget() {
        System.setProperty(ErpPrjConstants.CONFIG_BUDGET_CONTROL_MODE, "WARNING");
        try {
            Long[] projectHolder = new Long[1];
            ormTemplate.runInSession(session -> {
                seedOpenPeriod("2026-07");
                seedAcctSchema(1L);
                Long subjectId = seedSubject("5101", "项目开发成本");
                Long projectTypeId = seedProjectType("PT-MAT", "研发", subjectId);
                Long projectId = seedProject("PRJ-MAT-W-001", "WARNING 项目", projectTypeId,
                        ErpPrjConstants.PROJECT_STATUS_OPEN);
                daoProvider.daoFor(ErpPrjProject.class).getEntityById(projectId)
                        .setBudget(new BigDecimal("1000"));
                projectHolder[0] = projectId;
                return null;
            });

            BigDecimal added = ormTemplate.runInSession(session -> collectionBiz.aggregateMaterialCost(
                    projectHolder[0], new BigDecimal("1500"), "PR-MAT-W-001-1", CTX));
            assertEquals(0, added.compareTo(new BigDecimal("1500")), "WARNING 放行并写入");
            assertTrue(findCollectionLine(ErpPrjConstants.SOURCE_BILL_TYPE_PURCHASE_RECEIVE,
                    "PR-MAT-W-001-1") != null, "WARNING 放行时归集行写入");
        } finally {
            System.clearProperty(ErpPrjConstants.CONFIG_BUDGET_CONTROL_MODE);
        }
    }

    @Test
    public void testRequireReferenceableRejectsNonOpen() {
        Long[] projectHolder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long subjectId = seedSubject("5101", "项目开发成本");
            Long projectTypeId = seedProjectType("PT-MAT", "研发", subjectId);
            Long projectId = seedProject("PRJ-MAT-C-001", "已关闭项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_COMPLETED);
            projectHolder[0] = projectId;
            return null;
        });

        NopException ex = assertThrows(NopException.class, () -> ormTemplate.runInSession(
                () -> collectionBiz.aggregateMaterialCost(projectHolder[0],
                        new BigDecimal("100"), "PR-MAT-C-001-1", CTX)));
        assertEquals(ErpPrjErrors.ERR_PROJECT_NOT_REFERENCEABLE.getErrorCode(), ex.getErrorCode(),
                "非 OPEN 项目经 requireReferenceable 单一咽喉拒绝（P2-RC-048 协同）");
        assertNull(findCollectionLine(ErpPrjConstants.SOURCE_BILL_TYPE_PURCHASE_RECEIVE,
                "PR-MAT-C-001-1"), "拒绝时不写入归集行");
    }

    @Test
    public void testMaterialAggregationDisabledByConfig() {
        System.setProperty(ErpPrjConstants.CONFIG_MATERIAL_AGGREGATION_ENABLED, "false");
        try {
            Long[] projectHolder = new Long[1];
            ormTemplate.runInSession(session -> {
                seedOpenPeriod("2026-07");
                seedAcctSchema(1L);
                Long subjectId = seedSubject("5101", "项目开发成本");
                Long projectTypeId = seedProjectType("PT-MAT", "研发", subjectId);
                Long projectId = seedProject("PRJ-MAT-OFF-001", "禁用项目", projectTypeId,
                        ErpPrjConstants.PROJECT_STATUS_OPEN);
                projectHolder[0] = projectId;
                return null;
            });

            BigDecimal added = ormTemplate.runInSession(session -> collectionBiz.aggregateMaterialCost(
                    projectHolder[0], new BigDecimal("100"), "PR-MAT-OFF-001-1", CTX));
            assertEquals(0, added.compareTo(BigDecimal.ZERO), "config-gated 关闭时返回 0");
            assertNull(findCollectionLine(ErpPrjConstants.SOURCE_BILL_TYPE_PURCHASE_RECEIVE,
                    "PR-MAT-OFF-001-1"), "config-gated 关闭时不生成归集行");
        } finally {
            System.clearProperty(ErpPrjConstants.CONFIG_MATERIAL_AGGREGATION_ENABLED);
        }
    }

    @Test
    public void testPnlFourCategoryAggregationWithMaterialViaFacade() {
        Long[] projectHolder = new Long[1];
        ormTemplate.runInSession(session -> {
            seedOpenPeriod("2026-07");
            seedAcctSchema(1L);
            Long subjectId = seedSubject("5101", "项目成本");
            Long projectTypeId = seedProjectType("PT-PNL", "损益", subjectId);
            Long projectId = seedProject("PRJ-PNL-MAT-001", "四分类损益项目", projectTypeId,
                    ErpPrjConstants.PROJECT_STATUS_OPEN);
            projectHolder[0] = projectId;
            seedCostLine(projectId, ErpPrjConstants.COST_CATEGORY_LABOR, "2000");
            seedCostLine(projectId, ErpPrjConstants.COST_CATEGORY_EXPENSE, "1000");
            seedCostLine(projectId, ErpPrjConstants.COST_CATEGORY_SUBCONTRACT, "1500");
            return null;
        });

        // MATERIAL 经生产 Facade 写入（采购入库链）
        ormTemplate.runInSession(session -> collectionBiz.aggregateMaterialCost(
                projectHolder[0], new BigDecimal("1500"), "PR-PNL-MAT-001-1", CTX));

        ErpPrjProjectPnl pnl = ormTemplate.runInSession(
                session -> pnlBiz.refreshPnl(projectHolder[0], null, null, CTX));
        assertEquals(0, pnl.getCostLabor().compareTo(new BigDecimal("2000")), "人工成本=2000");
        assertEquals(0, pnl.getCostMaterial().compareTo(new BigDecimal("1500")), "物料成本=1500");
        assertEquals(0, pnl.getCostExpense().compareTo(new BigDecimal("1000")), "费用成本=1000");
        assertEquals(0, pnl.getCostSubcontract().compareTo(new BigDecimal("1500")), "分包成本=1500");
        assertEquals(0, pnl.getTotalCost().compareTo(new BigDecimal("6000")), "成本合计=6000");
    }

    // ---------- seed helpers ----------

    private void seedCostLine(Long projectId, String category, String amount) {
        IEntityDao<ErpPrjCostCollection> headDao = daoProvider.daoFor(ErpPrjCostCollection.class);
        ErpPrjCostCollection head = new ErpPrjCostCollection();
        head.setCode("CC-SEED-" + projectId + "-" + category + "-" + System.nanoTime());
        head.setProjectId(projectId);
        head.setOrgId(1L);
        head.setBusinessDate(LocalDate.of(2026, 7, 15));
        head.setTotalAmount(new BigDecimal(amount));
        head.setDocStatus(ErpPrjConstants.DOC_STATUS_APPROVED);
        head.setApproveStatus(ErpPrjConstants.APPROVE_STATUS_APPROVED);
        head.setPosted(false);
        head.setExchangeRate(BigDecimal.ONE);
        head.setAmountSource(new BigDecimal(amount));
        head.setAmountFunctional(new BigDecimal(amount));
        headDao.saveEntity(head);

        IEntityDao<ErpPrjCostCollectionLine> lineDao = daoProvider.daoFor(ErpPrjCostCollectionLine.class);
        ErpPrjCostCollectionLine line = new ErpPrjCostCollectionLine();
        line.setCostCollectionId(head.getId());
        line.setLineNo(1);
        line.setCostCategory(category);
        line.setSourceBillType(category);
        line.setSourceBillCode("SEED-" + category + "-" + System.nanoTime());
        line.setAmount(new BigDecimal(amount));
        lineDao.saveEntity(line);
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

    private Long seedSubject(String code, String name) {
        IEntityDao<ErpMdSubject> dao = daoProvider.daoFor(ErpMdSubject.class);
        ErpMdSubject s = new ErpMdSubject();
        s.setCode(code);
        s.setName(name);
        s.setSubjectClass("ASSET");
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
