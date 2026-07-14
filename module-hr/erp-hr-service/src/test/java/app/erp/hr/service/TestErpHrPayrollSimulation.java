package app.erp.hr.service;

import app.erp.hr.biz.IErpHrSalaryBiz;
import app.erp.hr.biz.IErpHrSalarySimulationBiz;
import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.dao.entity.ErpHrSalarySimulation;
import app.erp.hr.dao.entity.ErpHrSalarySimulationItemAdjustment;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceBase;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceConfig;
import app.erp.hr.dao.entity.ErpHrTaxConfig;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.context.ContextProvider;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.ast.GraphQLOperationType;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 薪酬模拟（What-If）+ 审批转正式端到端测试（payroll-simulation.md §一/§二/§三/§四/§五）。验证：
 * <ul>
 *   <li>创建模拟冻结源快照 + adjustItem 即时应变（基本工资调→gross/tax/net 联动）。</li>
 *   <li>对比三列差额 + 批量调薪 + 异常告警阈值命中。</li>
 *   <li>审批状态机（无调整提交拒/非法迁移抛错）。</li>
 *   <li>转正式（PAID 冲突拒/重复拒/部分冲突仅转无冲突 + 回填 convertedSalaryId + 正式 PENDING）。</li>
 * </ul>
 *
 * <p>0831-2 payroll 计算零回归由 {@link TestErpHrPayrollEngine} 覆盖。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrPayrollSimulation extends JunitAutoTestCase {

    private static final IServiceContext CTX = new ServiceContextImpl();

    // WORKFLOW 模式下薪酬 submit 会启动 wf 实例，wf 引擎校验 caller 需 resolved 用户。
    // 用 SYS（id=0）：submit 步骤 owner 解析为 SYS，caller=0 匹配跳过委托校验，避免 NopAuthUser 查询。
    @BeforeEach
    public void setUpWfUser() {
        ContextProvider.getOrCreateContext().setUserId("0");
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IErpHrSalaryBiz salaryBiz;
    @Inject
    IErpHrSalarySimulationBiz simulationBiz;
    @Inject
    IGraphQLEngine graphQLEngine;

    @Test
    public void testCreateSimulationFreezesSourceAndAdjustItemRecalculates() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMP-SIM-ADJ");
            seedContract(empId, "10000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "10000", "10000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        // 源期间 2026-06 正式薪酬
        ErpHrSalary source = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 6, CTX));
        BigDecimal sourceGross = source.getGrossSalary();
        BigDecimal sourceNet = source.getNetSalary();
        assertTrue(sourceGross.signum() > 0, "源应发>0");

        // 创建模拟（源=2026-06，目标=2026-08）
        ErpHrSalarySimulation simulation = ormTemplate.runInSession(session -> simulationBiz.createSimulation(
                2026, 6, 2026, 8, "2026-08 调薪试算", null, CTX));
        assertEquals(ErpHrConstants.SIMULATION_STATUS_DRAFT, simulation.getStatus());
        assertNotNull(simulation.getSourceSalaryId());

        // adjustItem：基本工资 +5000（覆盖重算：gross↑→tax↑→net↑）
        ErpHrSalary sim1 = ormTemplate.runInSession(session -> simulationBiz.adjustItem(simulation.getId(), employeeId,
                "basicSalary", new BigDecimal("15000"),
                ErpHrConstants.ADJUSTMENT_REASON_SALARY_CHANGE, CTX));
        assertTrue(sim1.getGrossSalary().compareTo(sourceGross) > 0,
                "调整后 gross 应高于源值");
        assertTrue(sim1.getBasicSalary().compareTo(new BigDecimal("15000")) == 0,
                "basicSalary=覆盖值");
        // 实发应变化（社保沿用源值，gross↑→tax↑，net 通常也↑）
        assertTrue(sim1.getNetSalary().compareTo(sourceNet) != 0,
                "调整后 net 应不同于源值（联动重算生效）");

        // 验证 ItemAdjustment 落库
        List<ErpHrSalarySimulationItemAdjustment> adjList =
 ormTemplate.runInSession(session -> simulationBiz.listAdjustments(simulation.getId(), employeeId, CTX));
        assertEquals(1, adjList.size(), "1 条调整记录");
        assertEquals("basicSalary", adjList.get(0).getSalaryItemCode());
        assertEquals(0, adjList.get(0).getOriginalAmount().compareTo(nz(source.getBasicSalary())),
                "originalAmount=源快照值");
        assertEquals(0, adjList.get(0).getAdjustedAmount().compareTo(new BigDecimal("15000")),
                "adjustedAmount=覆盖值");

        // 源修改不污染模拟（源期间 salary.basicSalary 改了 → 模拟 originalAmount 仍冻结）
        BigDecimal frozenOriginal = adjList.get(0).getOriginalAmount();
        ormTemplate.runInSession(session -> {
            ErpHrSalary src = daoProvider.daoFor(ErpHrSalary.class).getEntityById(source.getId());
            src.setBasicSalary(new BigDecimal("99999"));
            daoProvider.daoFor(ErpHrSalary.class).updateEntity(src);
            return null;
        });
        List<ErpHrSalarySimulationItemAdjustment> adjList2 =
 ormTemplate.runInSession(session -> simulationBiz.listAdjustments(simulation.getId(), employeeId, CTX));
        assertEquals(0, frozenOriginal.compareTo(adjList2.get(0).getOriginalAmount()),
                "ItemAdjustment.originalAmount 冻结在源快照值（不受源后续修改影响）");

        // 非 DRAFT 调整抛错
        ormTemplate.runInSession(session -> {
            ErpHrSalarySimulation sim = daoProvider.daoFor(ErpHrSalarySimulation.class)
                    .getEntityById(simulation.getId());
            sim.setStatus(ErpHrConstants.SIMULATION_STATUS_IN_REVIEW);
            daoProvider.daoFor(ErpHrSalarySimulation.class).updateEntity(sim);
            return null;
        });
        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> simulationBiz.adjustItem(simulation.getId(), employeeId, "basicSalary",
                        new BigDecimal("20000"), ErpHrConstants.ADJUSTMENT_REASON_SALARY_CHANGE, CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SIMULATION_ILLEGAL_TRANSITION.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testComparisonThreeColumns() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMP-SIM-CMP");
            seedContract(empId, "12000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "12000", "12000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ormTemplate.runInSession(() -> salaryBiz.calculateSalary(employeeId, 2026, 5, CTX));
        ormTemplate.runInSession(() -> salaryBiz.calculateSalary(employeeId, 2026, 6, CTX));

        ErpHrSalarySimulation simulation = ormTemplate.runInSession(session -> simulationBiz.createSimulation(
                2026, 6, 2026, 8, "对比测试", null, CTX));
        ormTemplate.runInSession(() -> simulationBiz.adjustItem(simulation.getId(), employeeId,
                "basicSalary", new BigDecimal("15000"),
                ErpHrConstants.ADJUSTMENT_REASON_SALARY_CHANGE, CTX));

        Map<String, Object> comparison = ormTemplate.runInSession(session -> simulationBiz.getComparison(simulation.getId(), employeeId, CTX));
        assertEquals(employeeId, comparison.get("employeeId"));
        assertNotNull(comparison.get("sourcePeriod"));
        assertNotNull(comparison.get("simulationPeriod"));
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> rows = (List<Map<String, Object>>) comparison.get("rows");
        assertFalse(rows.isEmpty(), "对比行不为空");
        Map<String, Object> basicRow = rows.stream()
                .filter(r -> "basicSalary".equals(r.get("itemCode")))
                .findFirst().orElseThrow(() -> new AssertionError("basicSalary 行缺失"));
        assertEquals(0, new BigDecimal("15000").compareTo((BigDecimal) basicRow.get("simulatedAmount")));
        BigDecimal diff = (BigDecimal) basicRow.get("diff");
        assertTrue(diff.signum() > 0, "basicSalary 差额>0（模拟值>源值）");
    }

    @Test
    public void testBatchAdjustmentFixed() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMP-SIM-BATCH");
            seedContract(empId, "10000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "10000", "10000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ormTemplate.runInSession(() -> salaryBiz.calculateSalary(employeeId, 2026, 6, CTX));
        ErpHrSalarySimulation simulation = ormTemplate.runInSession(session -> simulationBiz.createSimulation(
                2026, 6, 2026, 8, "批量调薪", null, CTX));

        Map<String, Object> result = ormTemplate.runInSession(session -> simulationBiz.applyBatchAdjustment(
                simulation.getId(), null, ErpHrConstants.BATCH_ADJUST_TYPE_FIXED,
                new BigDecimal("3000"), CTX));
        int affected = ((Number) result.get("affectedCount")).intValue();
        assertTrue(affected >= 1, "至少 1 人受影响");
        BigDecimal totalIncrease = (BigDecimal) result.get("totalGrossIncrease");
        assertTrue(totalIncrease.signum() > 0, "总应发增加>0");

        List<ErpHrSalarySimulationItemAdjustment> adjList =
 ormTemplate.runInSession(session -> simulationBiz.listAdjustments(simulation.getId(), employeeId, CTX));
        assertFalse(adjList.isEmpty(), "批量调薪生成 ItemAdjustment");
        assertEquals("basicSalary", adjList.get(0).getSalaryItemCode());
    }

    @Test
    public void testFindAnomaliesThresholdHit() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMP-SIM-ANOM");
            seedContract(empId, "10000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "10000", "10000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ormTemplate.runInSession(() -> salaryBiz.calculateSalary(employeeId, 2026, 6, CTX));
        ErpHrSalarySimulation simulation = ormTemplate.runInSession(session -> simulationBiz.createSimulation(
                2026, 6, 2026, 8, "异常告警", null, CTX));
        // 大幅调薪 +100%（远超 net-pay-change-threshold=0.2 / total-change-threshold=0.1）
        ormTemplate.runInSession(() -> simulationBiz.adjustItem(simulation.getId(), employeeId,
                "basicSalary", new BigDecimal("20000"),
                ErpHrConstants.ADJUSTMENT_REASON_SALARY_CHANGE, CTX));

        List<Map<String, Object>> anomalies = ormTemplate.runInSession(session -> simulationBiz.findAnomalies(simulation.getId(), CTX));
        assertFalse(anomalies.isEmpty(), "大幅调薪应触发异常告警");
        boolean hasTotalChange = anomalies.stream()
                .anyMatch(a -> ErpHrConstants.ANOMALY_TOTAL_CHANGE.equals(a.get("anomalyType")));
        assertTrue(hasTotalChange, "应命中 TOTAL_CHANGE 告警（gross 变化>10%）");
    }

    @Test
    public void testApprovalStateMachineNoAdjustmentRejected() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMP-SIM-NOADJ");
            seedContract(empId, "10000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "10000", "10000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ormTemplate.runInSession(() -> salaryBiz.calculateSalary(employeeId, 2026, 6, CTX));
        ErpHrSalarySimulation simulation = ormTemplate.runInSession(session -> simulationBiz.createSimulation(
                2026, 6, 2026, 8, "无调整测试", null, CTX));

        // 无任何 ItemAdjustment → 提交审核被拒
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> simulationBiz.submitForReview(simulation.getId(), CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SIMULATION_NO_ADJUSTMENT.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testApprovalStateMachineHappyPathAndReject() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMP-SIM-FLOW");
            seedContract(empId, "10000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "10000", "10000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ormTemplate.runInSession(() -> salaryBiz.calculateSalary(employeeId, 2026, 6, CTX));
        ErpHrSalarySimulation simulation = ormTemplate.runInSession(session -> simulationBiz.createSimulation(
                2026, 6, 2026, 8, "审批流", null, CTX));
        final Long simId1 = simulation.getId();
        ormTemplate.runInSession(() -> simulationBiz.adjustItem(simId1, employeeId,
                "basicSalary", new BigDecimal("12000"),
                ErpHrConstants.ADJUSTMENT_REASON_SALARY_CHANGE, CTX));

        // DRAFT → IN_REVIEW
        final Long simId2 = simulation.getId();
        simulation = ormTemplate.runInSession(session -> simulationBiz.submitForReview(simId2, CTX));
        assertEquals(ErpHrConstants.SIMULATION_STATUS_IN_REVIEW, simulation.getStatus());

        // IN_REVIEW → APPROVED
        final Long simId3 = simulation.getId();
        simulation = ormTemplate.runInSession(session -> simulationBiz.approve(simId3, 1L, CTX));
        assertEquals(ErpHrConstants.SIMULATION_STATUS_APPROVED, simulation.getStatus());
        assertEquals(1L, simulation.getReviewerId());
        assertNotNull(simulation.getReviewedAt());

        // 非法迁移：APPROVED 再 submitForReview（期望 DRAFT）→ 抛错
        Long approvedSimId = simulation.getId();
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> simulationBiz.submitForReview(approvedSimId, CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SIMULATION_ILLEGAL_TRANSITION.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testRejectFromInReview() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMP-SIM-REJ");
            seedContract(empId, "10000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "10000", "10000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ormTemplate.runInSession(() -> salaryBiz.calculateSalary(employeeId, 2026, 6, CTX));
        ErpHrSalarySimulation simulation = ormTemplate.runInSession(session -> simulationBiz.createSimulation(
                2026, 6, 2026, 8, "驳回测试", null, CTX));
        final Long simId1 = simulation.getId();
        ormTemplate.runInSession(() -> simulationBiz.adjustItem(simId1, employeeId,
                "basicSalary", new BigDecimal("11000"),
                ErpHrConstants.ADJUSTMENT_REASON_SALARY_CHANGE, CTX));
        final Long simId2 = simulation.getId();
        simulation = ormTemplate.runInSession(session -> simulationBiz.submitForReview(simId2, CTX));

        final Long simId3 = simulation.getId();
        simulation = ormTemplate.runInSession(session -> simulationBiz.reject(simId3, "调薪幅度不合理", CTX));
        assertEquals(ErpHrConstants.SIMULATION_STATUS_REJECTED, simulation.getStatus());
        assertEquals("调薪幅度不合理", simulation.getNotes());
    }

    @Test
    public void testConvertToFormalSuccess() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMP-SIM-CONV");
            seedContract(empId, "10000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "10000", "10000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ormTemplate.runInSession(() -> salaryBiz.calculateSalary(employeeId, 2026, 6, CTX));
        ErpHrSalarySimulation simulation = ormTemplate.runInSession(session -> simulationBiz.createSimulation(
                2026, 6, 2026, 12, "转正式成功", null, CTX));
        final Long simId1 = simulation.getId();
        ormTemplate.runInSession(() -> simulationBiz.adjustItem(simId1, employeeId,
                "basicSalary", new BigDecimal("12000"),
                ErpHrConstants.ADJUSTMENT_REASON_SALARY_CHANGE, CTX));
        final Long simId2 = simulation.getId();
        ormTemplate.runInSession(() -> simulationBiz.submitForReview(simId2, CTX));
        final Long simId3 = simulation.getId();
        ormTemplate.runInSession(() -> simulationBiz.approve(simId3, 1L, CTX));

        final Long simId4 = simulation.getId();
        simulation = ormTemplate.runInSession(session -> simulationBiz.convertToFormal(simId4, CTX));
        assertEquals(ErpHrConstants.SIMULATION_STATUS_CONVERTED, simulation.getStatus());
        assertNotNull(simulation.getConvertedSalaryId(), "回填 convertedSalaryId");
        assertNotNull(simulation.getConvertedAt(), "回填 convertedAt");

        // 验证正式 PENDING 薪酬已创建（2026-12）
        ErpHrSalary formal = daoProvider.daoFor(ErpHrSalary.class).getEntityById(
                simulation.getConvertedSalaryId());
        assertEquals(ErpHrConstants.APPROVE_STATUS_UNSUBMITTED, formal.getApproveStatus());
        assertEquals(Integer.valueOf(2026), formal.getYear());
        assertEquals(Integer.valueOf(12), formal.getMonth());
        assertEquals(0, formal.getBasicSalary().compareTo(new BigDecimal("12000")),
                "正式薪酬取模拟重算值");

        // 反向追溯
        final Long convertedSalaryId1 = simulation.getConvertedSalaryId();
        List<ErpHrSalarySimulation> traced = ormTemplate.runInSession(session -> simulationBiz.findSimulationsByConvertedSalary(
                convertedSalaryId1, CTX));
        assertFalse(traced.isEmpty(), "反向追溯链完整");
        assertEquals(simulation.getId(), traced.get(0).getId());
    }

    @Test
    public void testConvertToFormalDuplicateConflict() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMP-SIM-DUP");
            seedContract(empId, "10000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "10000", "10000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        // 目标期间 2026-07 已有正式薪酬（重复）
        ormTemplate.runInSession(() -> salaryBiz.calculateSalary(employeeId, 2026, 6, CTX));
        ormTemplate.runInSession(() -> salaryBiz.calculateSalary(employeeId, 2026, 7, CTX));

        ErpHrSalarySimulation simulation = ormTemplate.runInSession(session -> simulationBiz.createSimulation(
                2026, 6, 2026, 7, "重复冲突", null, CTX));
        ormTemplate.runInSession(() -> simulationBiz.adjustItem(simulation.getId(), employeeId,
                "basicSalary", new BigDecimal("11000"),
                ErpHrConstants.ADJUSTMENT_REASON_SALARY_CHANGE, CTX));
        ormTemplate.runInSession(() -> simulationBiz.submitForReview(simulation.getId(), CTX));
        ormTemplate.runInSession(() -> simulationBiz.approve(simulation.getId(), 1L, CTX));

        // 全员冲突 → 抛 EMPLOYEE_DUPLICATE
        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> simulationBiz.convertToFormal(simulation.getId(), CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SIMULATION_EMPLOYEE_DUPLICATE.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testConvertToFormalPaidConflict() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMP-SIM-PAID");
            seedContract(empId, "10000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "10000", "10000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        // 目标期间已有 PAID 薪酬
        ErpHrSalary target = ormTemplate.runInSession(session -> salaryBiz.calculateSalary(employeeId, 2026, 6, CTX));
        submitSalary(target.getId());
        approveSalary(target.getId());
        ormTemplate.runInSession(() -> salaryBiz.markPaid(target.getId(), CTX));

        ErpHrSalarySimulation simulation = ormTemplate.runInSession(session -> simulationBiz.createSimulation(
                2026, 6, 2026, 6, "PAID 冲突", null, CTX));
        ormTemplate.runInSession(() -> simulationBiz.adjustItem(simulation.getId(), employeeId,
                "basicSalary", new BigDecimal("11000"),
                ErpHrConstants.ADJUSTMENT_REASON_SALARY_CHANGE, CTX));
        ormTemplate.runInSession(() -> simulationBiz.submitForReview(simulation.getId(), CTX));
        ormTemplate.runInSession(() -> simulationBiz.approve(simulation.getId(), 1L, CTX));

        NopException ex = assertThrows(NopException.class,
                () -> ormTemplate.runInSession(session -> simulationBiz.convertToFormal(simulation.getId(), CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SIMULATION_TARGET_PERIOD_CONFLICT.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testSourceNotFoundThrows() {
        // 源期间无任何薪酬 → ERR_HR_SIMULATION_SOURCE_NOT_FOUND
        NopException ex = assertThrows(NopException.class, () ->
                ormTemplate.runInSession(session -> simulationBiz.createSimulation(2099, 12, 2099, 12, "空源", null, CTX)));
        assertEquals(ErpHrErrors.ERR_HR_SIMULATION_SOURCE_NOT_FOUND.getErrorCode(), ex.getErrorCode());
    }

    @Test
    public void testGetSimulatedSalaryReadOnly() {
        Long employeeId = ormTemplate.runInSession(session -> {
            seedTaxConfig(2026);
            Long empId = seedEmployee("EMP-SIM-GET");
            seedContract(empId, "10000");
            seedSocialInsuranceBase(empId, "SHENZHEN", "10000", "10000");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_PENSION,
                    "0.15", "0.08", "6123", "32694");
            seedSocialInsuranceConfig("SHENZHEN", ErpHrConstants.INSURANCE_HOUSING_FUND,
                    "0.12", "0.12", "2360", "32694");
            System.setProperty(ErpHrConstants.CONFIG_DEFAULT_PAYROLL_SUBJECT_ID, "2211");
            return empId;
        });

        ormTemplate.runInSession(() -> salaryBiz.calculateSalary(employeeId, 2026, 6, CTX));
        ErpHrSalarySimulation simulation = ormTemplate.runInSession(session -> simulationBiz.createSimulation(
                2026, 6, 2026, 8, "只读查询", null, CTX));

        // 无调整时，模拟薪酬≈源值（社保/公积金沿用源；tax 按 2026-08 累计窗口重算可能略异）
        ErpHrSalary sim = ormTemplate.runInSession(session -> simulationBiz.getSimulatedSalary(simulation.getId(), employeeId, CTX));
        assertNotNull(sim);
        assertEquals(employeeId, sim.getEmployeeId());
        assertEquals(Integer.valueOf(2026), sim.getYear());
        assertEquals(Integer.valueOf(8), sim.getMonth());
    }

    // ---------- seed helpers ----------

    private ApiResponse<?> submitSalary(Long salaryId) {
        return executeRpc(mutation, "ErpHrSalary__submitForApproval", ApiRequest.build(Map.of("id", String.valueOf(salaryId))));
    }

    private ApiResponse<?> approveSalary(Long salaryId) {
        return executeRpc(mutation, "ErpHrSalary__approve", ApiRequest.build(Map.of("id", String.valueOf(salaryId))));
    }

    private ApiResponse<?> executeRpc(GraphQLOperationType opType, String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private void seedTaxConfig(int year) {
        IEntityDao<ErpHrTaxConfig> dao = daoProvider.daoFor(ErpHrTaxConfig.class);
        ErpHrTaxConfig cfg = new ErpHrTaxConfig();
        cfg.setYear(year);
        cfg.setTaxThreshold(new BigDecimal("5000"));
        cfg.setTaxBrackets("["
                + "{\"rangeUpperLimit\":36000,\"rate\":0.03,\"quickDeduction\":0},"
                + "{\"rangeUpperLimit\":144000,\"rate\":0.10,\"quickDeduction\":2520},"
                + "{\"rangeUpperLimit\":300000,\"rate\":0.20,\"quickDeduction\":16920},"
                + "{\"rangeUpperLimit\":420000,\"rate\":0.25,\"quickDeduction\":31920},"
                + "{\"rangeUpperLimit\":660000,\"rate\":0.30,\"quickDeduction\":52920},"
                + "{\"rangeUpperLimit\":960000,\"rate\":0.35,\"quickDeduction\":85920},"
                + "{\"rangeUpperLimit\":null,\"rate\":0.45,\"quickDeduction\":181920}"
                + "]");
        dao.saveEntity(cfg);
    }

    private Long seedEmployee(String code) {
        IEntityDao<ErpHrEmployee> dao = daoProvider.daoFor(ErpHrEmployee.class);
        ErpHrEmployee emp = new ErpHrEmployee();
        emp.setCode(code);
        emp.setFirstName("测");
        emp.setLastName("试");
        emp.setFullName("模拟测试员工");
        emp.setGender("MALE");
        emp.setHireDate(LocalDate.of(2025, 1, 1));
        emp.setEmploymentStatus(ErpHrConstants.EMPLOYMENT_ACTIVE);
        emp.setEmployeeType("FULL_TIME");
        dao.saveEntity(emp);
        return emp.getId();
    }

    private void seedContract(Long employeeId, String monthlySalary) {
        IEntityDao<ErpHrEmploymentContract> dao = daoProvider.daoFor(ErpHrEmploymentContract.class);
        ErpHrEmploymentContract c = new ErpHrEmploymentContract();
        c.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        c.setBusinessDate(java.time.LocalDate.of(2026, 7, 1));
        c.setCode("C-" + employeeId);
        c.setEmployeeId(employeeId);
        c.setContractType("OPEN_ENDED");
        c.setSignDate(LocalDate.of(2025, 1, 1));
        c.setStartDate(LocalDate.of(2025, 1, 1));
        c.setMonthlySalary(new BigDecimal(monthlySalary));
        c.setStatus("ACTIVE");
        dao.saveEntity(c);
    }

    private void seedSocialInsuranceBase(Long employeeId, String cityCode,
                                          String socialInsuranceBase, String housingFundBase) {
        IEntityDao<ErpHrSocialInsuranceBase> dao = daoProvider.daoFor(ErpHrSocialInsuranceBase.class);
        ErpHrSocialInsuranceBase base = new ErpHrSocialInsuranceBase();
        base.setEmployeeId(employeeId);
        base.setCityCode(cityCode);
        base.setSocialInsuranceBase(new BigDecimal(socialInsuranceBase));
        base.setHousingFundBase(new BigDecimal(housingFundBase));
        base.setEffectiveFrom(LocalDate.of(2026, 1, 1));
        dao.saveEntity(base);
    }

    private void seedSocialInsuranceConfig(String cityCode, String insuranceType,
                                            String companyRate, String employeeRate,
                                            String lower, String upper) {
        IEntityDao<ErpHrSocialInsuranceConfig> dao = daoProvider.daoFor(ErpHrSocialInsuranceConfig.class);
        ErpHrSocialInsuranceConfig cfg = new ErpHrSocialInsuranceConfig();
        cfg.setCityCode(cityCode);
        cfg.setInsuranceType(insuranceType);
        cfg.setCompanyRate(new BigDecimal(companyRate));
        cfg.setEmployeeRate(new BigDecimal(employeeRate));
        cfg.setBaseLowerLimit(new BigDecimal(lower));
        cfg.setBaseUpperLimit(new BigDecimal(upper));
        dao.saveEntity(cfg);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}
