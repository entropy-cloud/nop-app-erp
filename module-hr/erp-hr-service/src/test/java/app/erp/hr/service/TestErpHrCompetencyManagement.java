package app.erp.hr.service;

import app.erp.hr.biz.IErpHrCompetencyBiz;
import app.erp.hr.biz.IErpHrDevelopmentPlanBiz;
import app.erp.hr.biz.IErpHrDevelopmentPlanItemBiz;
import app.erp.hr.biz.IErpHrEmployeeAssessmentBiz;
import app.erp.hr.biz.IErpHrGapAnalysisBiz;
import app.erp.hr.biz.IErpHrRoleCompetencyBiz;
import app.erp.hr.dao.entity.ErpHrAssessmentDetail;
import app.erp.hr.dao.entity.ErpHrCompetency;
import app.erp.hr.dao.entity.ErpHrDevelopmentPlan;
import app.erp.hr.dao.entity.ErpHrDevelopmentPlanItem;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrEmployeeAssessment;
import app.erp.hr.dao.entity.ErpHrGapAnalysis;
import app.erp.hr.dao.entity.ErpHrPosition;
import app.erp.hr.dao.entity.ErpHrRoleCompetency;
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

import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 胜任力管理端到端集成测试（competency-management.md §评估流程/§差距分析/§发展计划）。覆盖：
 * <ul>
 *   <li>评估状态机：submit 无 detail 拒绝（ERR_ASSESSMENT_NO_DETAILS）；complete 触发差距刷新。</li>
 *   <li>360 聚合端到端：COMPLETED 后 detail.actualLevel 已被聚合写回。</li>
 *   <li>差距快照清旧重建：refreshGapAnalysis 二次调用不残留旧记录。</li>
 *   <li>发展计划生成：仅 CRITICAL/MODERATE 差距被纳入，按 severity 排序。</li>
 *   <li>计划项状态机：NOT_STARTED→IN_PROGRESS→ACHIEVED；非法迁移拒绝。</li>
 *   <li>字典成环：competency.parentId 自引用/祖先链回环拒绝。</li>
 *   <li>矩阵 requiredLevel 范围：超 1-5 拒绝。</li>
 * </ul>
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrCompetencyManagement extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrCompetencyBiz competencyBiz;
    @Inject
    IErpHrRoleCompetencyBiz roleCompetencyBiz;
    @Inject
    IErpHrEmployeeAssessmentBiz employeeAssessmentBiz;
    @Inject
    IErpHrGapAnalysisBiz gapAnalysisBiz;
    @Inject
    IErpHrDevelopmentPlanBiz developmentPlanBiz;
    @Inject
    IErpHrDevelopmentPlanItemBiz planItemBiz;

    // ============ 评估状态机 + 360 聚合 + 差距刷新 ============

    @Test
    public void testSubmitWithoutDetailsRejects() {
        Long empId = ormTemplate.runInSession(session -> {
            Long posId = seedPosition("POS-NODETAIL");
            return seedEmployeeWithPosition("EMP-NODETAIL", posId);
        });
        Long competencyId = seedCompetency("COMP-NODETAIL", null);
        Long assessmentId = seedAssessment(empId, ErpHrConstants.ASSESSMENT_TYPE_SELF,
                ErpHrConstants.ASSESSMENT_STATUS_DRAFT);

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> employeeAssessmentBiz.submitAssessment(assessmentId, CTX)));
        assertEquals(ErpHrErrors.ERR_ASSESSMENT_NO_DETAILS.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testCompleteAssessmentTriggersGapRefresh() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long posId = seedPosition("POS-GAP");
            Long empId = seedEmployeeWithPosition("EMP-GAP", posId);
            Long competencyId = seedCompetency("COMP-GAP", null);
            // 岗位要求等级 4
            seedRoleCompetency(posId, competencyId, 4);
            return new Object[]{empId, competencyId};
        });
        Long empId = (Long) seeded[0];
        Long competencyId = (Long) seeded[1];

        // 创建评估 + 1 条 SELF detail（actual=2）→ 提交 → 完成
        Long assessmentId = seedAssessment(empId, ErpHrConstants.ASSESSMENT_TYPE_SELF,
                ErpHrConstants.ASSESSMENT_STATUS_DRAFT);
        seedDetail(assessmentId, competencyId, 2, ErpHrConstants.ASSESSMENT_TYPE_SELF);

        ormTemplate.runInSession(() -> employeeAssessmentBiz.submitAssessment(assessmentId, CTX));
        ErpHrEmployeeAssessment completed = ormTemplate.runInSession(session -> employeeAssessmentBiz.completeAssessment(assessmentId, CTX));
        assertEquals(ErpHrConstants.ASSESSMENT_STATUS_COMPLETED, completed.getStatus());

        // 差距应已刷新：required=4, actual=2, gap=2 (MODERATE)
        List<ErpHrGapAnalysis> gaps = findGaps(empId);
        assertEquals(1, gaps.size());
        ErpHrGapAnalysis gap = gaps.get(0);
        assertEquals(4, gap.getRequiredLevel().intValue());
        assertEquals(2, gap.getActualLevel().intValue());
        assertEquals(2, gap.getGapValue().intValue());
        assertEquals(ErpHrConstants.GAP_SEVERITY_MODERATE, gap.getGapSeverity());

        // overallScore 应被写回（detail 聚合后 = 2）
        assertNotNull(completed.getOverallScore());
        assertEquals(2, completed.getOverallScore().intValue());
    }

    @Test
    public void testGapRefreshClearsOldSnapshot() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long posId = seedPosition("POS-REFRESH");
            Long empId = seedEmployeeWithPosition("EMP-REFRESH", posId);
            Long competencyId = seedCompetency("COMP-REFRESH", null);
            seedRoleCompetency(posId, competencyId, 5);
            return new Object[]{empId, competencyId};
        });
        Long empId = (Long) seeded[0];
        Long competencyId = (Long) seeded[1];

        // 第一次刷新：required=5, actual=0 (无 detail) → gap=5 CRITICAL
        Long assessmentId1 = seedAssessmentOn(empId, ErpHrConstants.ASSESSMENT_TYPE_SELF,
                ErpHrConstants.ASSESSMENT_STATUS_COMPLETED, LocalDate.of(2026, 6, 1));
        seedDetail(assessmentId1, competencyId, 0, ErpHrConstants.ASSESSMENT_TYPE_SELF);
        ormTemplate.runInSession(() -> gapAnalysisBiz.refreshGapAnalysis(empId, CTX));
        assertEquals(1, findGaps(empId).size());

        // 第二次刷新：删除第一次 detail，新建更晚日期的评估（actual=3）→ gap=2 MODERATE
        Long assessmentId2 = seedAssessmentOn(empId, ErpHrConstants.ASSESSMENT_TYPE_SELF,
                ErpHrConstants.ASSESSMENT_STATUS_COMPLETED, LocalDate.of(2026, 7, 1));
        ormTemplate.runInSession(() -> {
            IEntityDao<ErpHrAssessmentDetail> detailDao = daoProvider.daoFor(ErpHrAssessmentDetail.class);
            QueryBean q = new QueryBean();
            q.addFilter(eq("assessmentId", assessmentId1));
            for (ErpHrAssessmentDetail d : detailDao.findAllByQuery(q)) {
                detailDao.deleteEntity(d);
            }
        });
        seedDetail(assessmentId2, competencyId, 3, ErpHrConstants.ASSESSMENT_TYPE_SELF);

        ormTemplate.runInSession(() -> gapAnalysisBiz.refreshGapAnalysis(empId, CTX));

        List<ErpHrGapAnalysis> gaps = findGaps(empId);
        assertEquals(1, gaps.size(), "清旧重建：不应残留旧快照");
        assertEquals(2, gaps.get(0).getGapValue().intValue());
        assertEquals(ErpHrConstants.GAP_SEVERITY_MODERATE, gaps.get(0).getGapSeverity());
    }

    @Test
    public void testGapRefreshNoRoleRequirementRejects() {
        // 员工无 positionId → ERR_GAP_NO_ROLE_REQUIREMENT
        Long empId = ormTemplate.runInSession(session ->
                seedEmployeeWithPosition("EMP-NOROLE", null));
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> gapAnalysisBiz.refreshGapAnalysis(empId, CTX)));
        assertEquals(ErpHrErrors.ERR_GAP_NO_ROLE_REQUIREMENT.getErrorCode(), ex.getErrorCode());
    }

    // ============ 360 多源聚合端到端 ============

    @Test
    public void test360AggregationEndToEnd() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long posId = seedPosition("POS-360");
            Long empId = seedEmployeeWithPosition("EMP-360", posId);
            Long competencyId = seedCompetency("COMP-360", null);
            seedRoleCompetency(posId, competencyId, 5);
            return new Object[]{empId, competencyId};
        });
        Long empId = (Long) seeded[0];
        Long competencyId = (Long) seeded[1];

        // 360 评估：4 sourceType 各一条（self=3, mgr=4, peer=5, sub=4）
        // 聚合：0.15*3 + 0.50*4 + 0.25*5 + 0.10*4 = 0.45+2.0+1.25+0.4 = 4.1 → 4
        Long assessmentId = seedAssessment(empId, ErpHrConstants.ASSESSMENT_TYPE_360,
                ErpHrConstants.ASSESSMENT_STATUS_DRAFT);
        seedDetail(assessmentId, competencyId, 3, ErpHrConstants.ASSESSMENT_TYPE_SELF);
        seedDetail(assessmentId, competencyId, 4, ErpHrConstants.ASSESSMENT_TYPE_MANAGER);
        seedDetail(assessmentId, competencyId, 5, ErpHrConstants.ASSESSMENT_TYPE_PEER);
        seedDetail(assessmentId, competencyId, 4, ErpHrConstants.ASSESSMENT_TYPE_SUBORDINATE);

        ormTemplate.runInSession(() -> employeeAssessmentBiz.submitAssessment(assessmentId, CTX));
        ErpHrEmployeeAssessment completed = ormTemplate.runInSession(session -> employeeAssessmentBiz.completeAssessment(assessmentId, CTX));

        // 各 detail actualLevel 应被聚合写回为 4
        IEntityDao<ErpHrAssessmentDetail> detailDao = daoProvider.daoFor(ErpHrAssessmentDetail.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("assessmentId", assessmentId));
        List<ErpHrAssessmentDetail> details = detailDao.findAllByQuery(q);
        assertFalse(details.isEmpty());
        for (ErpHrAssessmentDetail d : details) {
            assertEquals(4, d.getActualLevel().intValue(),
                    "360 detail 应被聚合引擎写回为聚合后 level");
        }
        assertEquals(4, completed.getOverallScore().intValue());
    }

    // ============ 发展计划生成 + 排序 ============

    @Test
    public void testGenerateDevelopmentPlanForActionableGaps() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long posId = seedPosition("POS-PLAN");
            Long empId = seedEmployeeWithPosition("EMP-PLAN", posId);
            Long compCritical = seedCompetency("COMP-CRIT", null);
            Long compModerate = seedCompetency("COMP-MOD", null);
            Long compMinor = seedCompetency("COMP-MINOR", null);
            // CRITICAL: required=5, actual=2 → gap=3
            seedRoleCompetency(posId, compCritical, 5);
            // MODERATE: required=4, actual=2 → gap=2
            seedRoleCompetency(posId, compModerate, 4);
            // MINOR: required=3, actual=2 → gap=1 （不应被纳入）
            seedRoleCompetency(posId, compMinor, 3);

            // 评估（COMPLETED）+ detail（actualLevel）
            Long aId = seedAssessment(empId, ErpHrConstants.ASSESSMENT_TYPE_SELF,
                    ErpHrConstants.ASSESSMENT_STATUS_COMPLETED);
            seedDetail(aId, compCritical, 2, ErpHrConstants.ASSESSMENT_TYPE_SELF);
            seedDetail(aId, compModerate, 2, ErpHrConstants.ASSESSMENT_TYPE_SELF);
            seedDetail(aId, compMinor, 2, ErpHrConstants.ASSESSMENT_TYPE_SELF);
            return new Object[]{empId, compCritical, compModerate, compMinor};
        });
        Long empId = (Long) seeded[0];
        Long compCritical = (Long) seeded[1];
        Long compModerate = (Long) seeded[2];

        // 先刷新差距
        ormTemplate.runInSession(() -> gapAnalysisBiz.refreshGapAnalysis(empId, CTX));

        ErpHrDevelopmentPlan plan = ormTemplate.runInSession(session -> developmentPlanBiz.generateDevelopmentPlan(empId, CTX));
        assertNotNull(plan);
        assertEquals(ErpHrConstants.DEV_PLAN_STATUS_IN_PROGRESS, plan.getStatus());

        List<ErpHrDevelopmentPlanItem> items = findPlanItems(plan.getId());
        assertEquals(2, items.size(), "仅 CRITICAL + MODERATE 差距应生成计划项（MINOR 跳过）");

        // 第一项应为 CRITICAL（severity 排序优先）
        ErpHrDevelopmentPlanItem first = items.get(0);
        assertEquals(compCritical, first.getCompetencyId());
        assertEquals(ErpHrConstants.GAP_SEVERITY_CRITICAL, lookupGapSeverity(first.getGapId()));
        assertEquals(5, first.getTargetLevel().intValue());
        assertEquals(ErpHrConstants.PLAN_ITEM_STATUS_NOT_STARTED, first.getStatus());

        // 第二项为 MODERATE
        ErpHrDevelopmentPlanItem second = items.get(1);
        assertEquals(compModerate, second.getCompetencyId());
        assertEquals(ErpHrConstants.GAP_SEVERITY_MODERATE, lookupGapSeverity(second.getGapId()));
    }

    @Test
    public void testGeneratePlanNoActionableGapReturnsNull() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long posId = seedPosition("POS-NOPLAN");
            Long empId = seedEmployeeWithPosition("EMP-NOPLAN", posId);
            Long competencyId = seedCompetency("COMP-NOPLAN", null);
            // required=2, actual=3 → gap=-1 NONE（无 actionable gap）
            seedRoleCompetency(posId, competencyId, 2);
            Long aId = seedAssessment(empId, ErpHrConstants.ASSESSMENT_TYPE_SELF,
                    ErpHrConstants.ASSESSMENT_STATUS_COMPLETED);
            seedDetail(aId, competencyId, 3, ErpHrConstants.ASSESSMENT_TYPE_SELF);
            return new Object[]{empId};
        });
        Long empId = (Long) seeded[0];
        ormTemplate.runInSession(() -> gapAnalysisBiz.refreshGapAnalysis(empId, CTX));
        ErpHrDevelopmentPlan plan = ormTemplate.runInSession(session -> developmentPlanBiz.generateDevelopmentPlan(empId, CTX));
        assertNull(plan, "无 CRITICAL/MODERATE 差距时返回 null");
    }

    // ============ 计划项状态机 ============

    @Test
    public void testPlanItemStatusMachineValidTransitions() {
        Long[] ids = preparePlanItemForStatusTest();
        Long planItemId = ids[0];

        ErpHrDevelopmentPlanItem started = ormTemplate.runInSession(session -> developmentPlanBiz.updatePlanItemStatus(
                planItemId, ErpHrConstants.PLAN_ITEM_STATUS_IN_PROGRESS, CTX));
        assertEquals(ErpHrConstants.PLAN_ITEM_STATUS_IN_PROGRESS, started.getStatus());
        assertNotNull(started.getStartDate());

        ErpHrDevelopmentPlanItem achieved = ormTemplate.runInSession(session -> developmentPlanBiz.updatePlanItemStatus(
                planItemId, ErpHrConstants.PLAN_ITEM_STATUS_ACHIEVED, CTX));
        assertEquals(ErpHrConstants.PLAN_ITEM_STATUS_ACHIEVED, achieved.getStatus());
        assertNotNull(achieved.getEndDate());
    }

    @Test
    public void testPlanItemStatusMachineIllegalTransition() {
        Long[] ids = preparePlanItemForStatusTest();
        Long planItemId = ids[0];

        // NOT_STARTED → ACHIEVED 跳过 IN_PROGRESS：非法
        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> developmentPlanBiz.updatePlanItemStatus(
                        planItemId, ErpHrConstants.PLAN_ITEM_STATUS_ACHIEVED, CTX)));
        assertEquals(ErpHrErrors.ERR_DEV_PLAN_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testCompletePlanTransitions() {
        Long[] ids = preparePlanItemForStatusTest();
        Long planId = ids[1];

        ErpHrDevelopmentPlan completed = ormTemplate.runInSession(session -> developmentPlanBiz.completePlan(planId, CTX));
        assertEquals(ErpHrConstants.DEV_PLAN_STATUS_COMPLETED, completed.getStatus());

        // 已 COMPLETED 再 complete：非法
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> developmentPlanBiz.completePlan(planId, CTX)));
        assertEquals(ErpHrErrors.ERR_DEV_PLAN_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode());
    }

    private Long[] preparePlanItemForStatusTest() {
        Object[] seeded = ormTemplate.runInSession(session -> {
            Long posId = seedPosition("POS-STATUS");
            Long empId = seedEmployeeWithPosition("EMP-STATUS", posId);
            Long competencyId = seedCompetency("COMP-STATUS", null);
            seedRoleCompetency(posId, competencyId, 5);
            Long aId = seedAssessment(empId, ErpHrConstants.ASSESSMENT_TYPE_SELF,
                    ErpHrConstants.ASSESSMENT_STATUS_COMPLETED);
            seedDetail(aId, competencyId, 1, ErpHrConstants.ASSESSMENT_TYPE_SELF);
            return new Object[]{empId};
        });
        Long empId = (Long) seeded[0];
        ormTemplate.runInSession(() -> gapAnalysisBiz.refreshGapAnalysis(empId, CTX));
        ErpHrDevelopmentPlan plan = ormTemplate.runInSession(session -> developmentPlanBiz.generateDevelopmentPlan(empId, CTX));
        List<ErpHrDevelopmentPlanItem> items = findPlanItems(plan.getId());
        assertFalse(items.isEmpty());
        return new Long[]{items.get(0).getId(), plan.getId()};
    }

    // ============ 字典成环 + 矩阵 requiredLevel 校验 ============
    // 经 biz.save/update(Map) 触发 defaultPrepareSave/Update 钩子（saveEntity 不走钩子管道）

    @Test
    public void testCompetencySelfCycleRejected() {
        Long compId = seedCompetency("COMP-SELF", null);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", compId);
        data.put("parentId", compId);
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> competencyBiz.update(data, CTX)));
        assertEquals(ErpHrErrors.ERR_COMPETENCY_PARENT_CYCLE.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testCompetencyAncestorChainCycleRejected() {
        // A → parent B → parent C；试图让 A 的 parent 指向 C（A→C→B→A 环不存在，但 A→C→B→A 形成：
        //   实际上 A→C, C→B, B→A ：从 A 出发 A→C→B→(A的旧parent应该到A自己) → 检测到 A 在祖先链）
        Long aId = seedCompetency("COMP-A", null);
        Long bId = seedCompetency("COMP-B", aId);
        Long cId = seedCompetency("COMP-C", bId);
        // A 的 parent 改为 C：祖先链 A→C→B→A（A 已在 visited）→ 环
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("id", aId);
        data.put("parentId", cId);
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> competencyBiz.update(data, CTX)));
        assertEquals(ErpHrErrors.ERR_COMPETENCY_PARENT_CYCLE.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testCompetencyValidHierarchyAccepted() {
        Long parentId = seedCompetency("COMP-PARENT", null);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("code", "COMP-CHILD");
        data.put("name", "COMP-CHILD");
        data.put("category", "SKILL");
        data.put("parentId", parentId);
        ErpHrCompetency child = ormTemplate.runInSession(session -> competencyBiz.save(data, CTX));
        assertEquals(parentId, child.getParentId());
    }

    @Test
    public void testRoleCompetencyInvalidLevelRejected() {
        Long posId = seedPosition("POS-BADLEVEL");
        Long compId = seedCompetency("COMP-BADLEVEL", null);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("positionId", posId);
        data.put("competencyId", compId);
        data.put("requiredLevel", 6); // 超出 1-5
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> roleCompetencyBiz.save(data, CTX)));
        assertEquals(ErpHrErrors.ERR_ROLE_COMPETENCY_INVALID_LEVEL.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testRoleCompetencyZeroLevelRejected() {
        Long posId = seedPosition("POS-ZERO");
        Long compId = seedCompetency("COMP-ZERO", null);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("positionId", posId);
        data.put("competencyId", compId);
        data.put("requiredLevel", 0); // 低于 1
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> roleCompetencyBiz.save(data, CTX)));
        assertEquals(ErpHrErrors.ERR_ROLE_COMPETENCY_INVALID_LEVEL.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testRoleCompetencyValidLevelAccepted() {
        Long posId = seedPosition("POS-OKLEVEL");
        Long compId = seedCompetency("COMP-OKLEVEL", null);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("positionId", posId);
        data.put("competencyId", compId);
        data.put("requiredLevel", 3);
        ErpHrRoleCompetency rc = ormTemplate.runInSession(session -> roleCompetencyBiz.save(data, CTX));
        assertNotNull(rc.getId());
        assertEquals(3, rc.getRequiredLevel().intValue());
    }

    // ---------- helpers ----------

    List<ErpHrGapAnalysis> findGaps(Long employeeId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("employeeId", employeeId));
        return ormTemplate.runInSession(session -> gapAnalysisBiz.findList(q, null, CTX));
    }

    List<ErpHrDevelopmentPlanItem> findPlanItems(Long planId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("planId", planId));
        q.addOrderField("id", false);
        return ormTemplate.runInSession(session -> planItemBiz.findList(q, null, CTX));
    }

    String lookupGapSeverity(Long gapId) {
        if (gapId == null) return null;
        ErpHrGapAnalysis g = daoProvider.daoFor(ErpHrGapAnalysis.class).getEntityById(gapId);
        return g != null ? g.getGapSeverity() : null;
    }

    Long seedPosition(String code) {
        IEntityDao<ErpHrPosition> dao = daoProvider.daoFor(ErpHrPosition.class);
        ErpHrPosition p = new ErpHrPosition();
        p.setCode(code);
        p.setName(code);
        dao.saveEntity(p);
        return p.getId();
    }

    Long seedEmployeeWithPosition(String code, Long positionId) {
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        ErpHrEmployee emp = new ErpHrEmployee();
        emp.setCode(code);
        emp.setFirstName("测");
        emp.setLastName("试");
        emp.setFullName("胜任力测试");
        emp.setGender("MALE");
        emp.setHireDate(LocalDate.of(2025, 1, 1));
        emp.setEmploymentStatus(ErpHrConstants.EMPLOYMENT_ACTIVE);
        emp.setEmployeeType("FULL_TIME");
        emp.setPositionId(positionId);
        dao.saveEntity(emp);
        return emp.getId();
    }

    Long seedCompetency(String code, Long parentId) {
        IEntityDao<ErpHrCompetency> dao = daoProvider.daoFor(ErpHrCompetency.class);
        ErpHrCompetency c = new ErpHrCompetency();
        c.setCode(code);
        c.setName(code);
        c.setCategory(ErpHrConstants.ITEM_GROUP_BASIC); // 复用任意合法字符串字典值占位
        c.setParentId(parentId);
        dao.saveEntity(c);
        return c.getId();
    }

    Long seedRoleCompetency(Long positionId, Long competencyId, int requiredLevel) {
        IEntityDao<ErpHrRoleCompetency> dao = daoProvider.daoFor(ErpHrRoleCompetency.class);
        ErpHrRoleCompetency rc = new ErpHrRoleCompetency();
        rc.setPositionId(positionId);
        rc.setCompetencyId(competencyId);
        rc.setRequiredLevel(requiredLevel);
        dao.saveEntity(rc);
        return rc.getId();
    }

    Long seedAssessment(Long employeeId, String assessmentType, String status) {
        return seedAssessmentOn(employeeId, assessmentType, status, LocalDate.of(2026, 7, 1));
    }

    Long seedAssessmentOn(Long employeeId, String assessmentType, String status, LocalDate date) {
        IEntityDao<ErpHrEmployeeAssessment> dao = daoProvider.daoFor(ErpHrEmployeeAssessment.class);
        ErpHrEmployeeAssessment a = new ErpHrEmployeeAssessment();
        a.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        a.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        a.setEmployeeId(employeeId);
        a.setAssessmentType(assessmentType);
        a.setStatus(status);
        a.setAssessmentDate(date);
        dao.saveEntity(a);
        return a.getId();
    }

    void seedDetail(Long assessmentId, Long competencyId, int actualLevel, String sourceType) {
        IEntityDao<ErpHrAssessmentDetail> dao = daoProvider.daoFor(ErpHrAssessmentDetail.class);
        ErpHrAssessmentDetail d = new ErpHrAssessmentDetail();
        d.setAssessmentId(assessmentId);
        d.setCompetencyId(competencyId);
        d.setActualLevel(actualLevel);
        d.setSourceType(sourceType);
        dao.saveEntity(d);
    }

    @SuppressWarnings("unused")
    private static boolean _unused() { return true; }
}
