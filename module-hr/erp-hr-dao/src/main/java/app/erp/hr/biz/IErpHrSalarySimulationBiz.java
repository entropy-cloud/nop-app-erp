package app.erp.hr.biz;

import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.dao.entity.ErpHrSalarySimulation;
import app.erp.hr.dao.entity.ErpHrSalarySimulationItemAdjustment;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;
import java.util.Map;

/**
 * 薪酬模拟聚合根 Biz（payroll-simulation.md）。承载模拟生命周期：创建模拟、调整即时应变、
 * 对比视图、批量调薪、异常告警、审批状态机、转正式。详见各方法文档。
 *
 * <p>模拟数据隔离正式数据：CONVERTED 前模拟不影响任何正式薪酬。转正式仅创建 PENDING 正式薪酬
 * （approvalStatus=PENDING）进入 0831-2 审批流；追溯单向 {@code Simulation.convertedSalaryId → ErpHrSalary}
 * （核心零污染，不在 ErpHrSalary 加列）。
 */
public interface IErpHrSalarySimulationBiz extends ICrudBiz<ErpHrSalarySimulation> {

    /**
     * 创建模拟。加载源期间 ErpHrSalary 行（按 employeeScope 筛选），建 ErpHrSalarySimulation(status=DRAFT)。
     * 源值经 ItemAdjustment.originalAmount 锚定冻结——后续源期间修改不影响模拟。
     *
     * @param sourceYear            源薪酬期间年
     * @param sourceMonth           源薪酬期间月
     * @param simulationPeriodYear  模拟目标期间年
     * @param simulationPeriodMonth 模拟目标期间月
     * @param simulationName        模拟名称（如"2026-07 调薪试算 v1"）
     * @param employeeScope         员工范围筛选；为空表示全部源员工。支持键：departmentId/positionId/employeeIds
     */
    @BizMutation
    ErpHrSalarySimulation createSimulation(@Name("sourceYear") int sourceYear,
                                           @Name("sourceMonth") int sourceMonth,
                                           @Name("simulationPeriodYear") int simulationPeriodYear,
                                           @Name("simulationPeriodMonth") int simulationPeriodMonth,
                                           @Name("simulationName") String simulationName,
                                           @Name("employeeScope") Map<String, Object> employeeScope,
                                           IServiceContext context);

    /**
     * 调整单项。仅 DRAFT 可调；记 ItemAdjustment（originalAmount=源快照值）；经 PayrollCalculator
     * 覆盖重算返回模拟 ErpHrSalary（不持久化为正式）。重复调整同员工同项目则覆盖既有 ItemAdjustment。
     *
     * @param salaryItemCode 薪酬项目字段名（basicSalary/positionAllowance/performanceBonus/overtimePay/
     *                       mealAllowance/transportAllowance/otherAllowance/otherDeductions）
     */
    @BizMutation
    ErpHrSalary adjustItem(@Name("simulationId") Long simulationId,
                           @Name("employeeId") Long employeeId,
                           @Name("salaryItemCode") String salaryItemCode,
                           @Name("adjustedAmount") java.math.BigDecimal adjustedAmount,
                           @Name("reason") String reason,
                           IServiceContext context);

    /**
     * 只读返回某员工模拟薪酬（据源 base + 全部已记 ItemAdjustment 经覆盖重算）。
     */
    @BizQuery
    ErpHrSalary getSimulatedSalary(@Name("simulationId") Long simulationId,
                                   @Name("employeeId") Long employeeId,
                                   IServiceContext context);

    /**
     * 列出模拟下某员工的全部 ItemAdjustment（用于对比/审计）。
     */
    @BizQuery
    List<ErpHrSalarySimulationItemAdjustment> listAdjustments(@Name("simulationId") Long simulationId,
                                                              @Name("employeeId") Long employeeId,
                                                              IServiceContext context);

    /**
     * 员工级三列对比（上期/当前期/模拟值 + 差额），按薪酬项目行展开（payroll-simulation.md §3.2）。
     */
    @BizQuery
    Map<String, Object> getComparison(@Name("simulationId") Long simulationId,
                                      @Name("employeeId") Long employeeId,
                                      IServiceContext context);

    /**
     * 部门汇总（应发/实发合计差异聚合，按部门分组）。
     */
    @BizQuery
    List<Map<String, Object>> getDepartmentSummary(@Name("simulationId") Long simulationId,
                                                    IServiceContext context);

    /**
     * 薪酬项目汇总（各薪酬项目合计变化，按 salaryItemCode 分组）。
     */
    @BizQuery
    List<Map<String, Object>> getProjectSummary(@Name("simulationId") Long simulationId,
                                                 IServiceContext context);

    /**
     * 公司级汇总（总人工成本变化）。
     */
    @BizQuery
    Map<String, Object> getCompanySummary(@Name("simulationId") Long simulationId,
                                          IServiceContext context);

    /**
     * 批量调薪模拟（payroll-simulation.md §5.1）。对 scope 内员工逐个生成 ItemAdjustment + 重算。
     *
     * @param scope       员工范围（同 createSimulation 的 employeeScope）；为空表示模拟内全部员工
     * @param adjustType  FIXED/RATIO/ALLOWANCE/LEVEL_MAP
     * @param value       FIXED/RATIO/ALLOWANCE 为 BigDecimal；LEVEL_MAP 为 Map&lt;jobGrade, BigDecimal&gt;
     * @return 影响汇总（affectedCount/totalGrossIncrease/avgIncrease）
     */
    @BizMutation
    Map<String, Object> applyBatchAdjustment(@Name("simulationId") Long simulationId,
                                             @Name("scope") Map<String, Object> scope,
                                             @Name("adjustType") String adjustType,
                                             @Name("value") Object value,
                                             IServiceContext context);

    /**
     * 异常告警扫描（payroll-simulation.md §3.3）。按 config 阈值标异常。
     */
    @BizQuery
    List<Map<String, Object>> findAnomalies(@Name("simulationId") Long simulationId,
                                             IServiceContext context);

    /**
     * DRAFT → IN_REVIEW（提交审核）。前置：至少一项 ItemAdjustment（否则 ERR_HR_SIMULATION_NO_ADJUSTMENT）。
     */
    @BizMutation
    ErpHrSalarySimulation submitForReview(@Name("simulationId") Long simulationId,
                                          IServiceContext context);

    /**
     * IN_REVIEW → APPROVED（审批通过，写 reviewerId/reviewedAt，锁定不可再调）。
     */
    @BizMutation
    ErpHrSalarySimulation approve(@Name("simulationId") Long simulationId,
                                  @Name("reviewerId") Long reviewerId,
                                  IServiceContext context);

    /**
     * IN_REVIEW → REJECTED（终态，notes=reason）。
     */
    @BizMutation
    ErpHrSalarySimulation reject(@Name("simulationId") Long simulationId,
                                 @Name("reason") String reason,
                                 IServiceContext context);

    /**
     * APPROVED → CONVERTED（转正式，payroll-simulation.md §四）。
     *
     * <p>校验目标期间（simulationPeriodYear/Month）无 PAID 正式薪酬（否则 ERR_HR_SIMULATION_TARGET_PERIOD_CONFLICT）；
     * 校验同员工无重复正式薪酬（含 DRAFT/PENDING 等，否则 ERR_HR_SIMULATION_EMPLOYEE_DUPLICATE）；
     * 为每位无冲突员工创建正式 ErpHrSalary（取模拟重算值，approvalStatus=PENDING 进入 0831-2 审批流），
     * 回填 convertedSalaryId（首条）+ convertedAt；部分冲突仅转无冲突员工。
     */
    @BizMutation
    ErpHrSalarySimulation convertToFormal(@Name("simulationId") Long simulationId,
                                          IServiceContext context);

    /**
     * 反向追溯查询：根据正式薪酬 ID 查关联的模拟（单向追溯补全，payroll-simulation.md §四）。
     */
    @BizQuery
    List<ErpHrSalarySimulation> findSimulationsByConvertedSalary(@Name("salaryId") Long salaryId,
                                                                 IServiceContext context);
}
