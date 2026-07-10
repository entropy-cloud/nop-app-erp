package app.erp.hr.service.payroll;

import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
import app.erp.hr.dao.entity.ErpHrLeaveRequest;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.ErpHrConfigs;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.and;
import static io.nop.api.core.beans.FilterBeans.eq;
import static io.nop.api.core.beans.FilterBeans.ge;
import static io.nop.api.core.beans.FilterBeans.in;
import static io.nop.api.core.beans.FilterBeans.le;

/**
 * 薪酬核算编排器（payroll.md §5.2）。按设计 §十一 关键规则 1 的顺序：
 * <pre>
 * 基本工资（出勤比例）→ 津贴/补贴 → 加班费（加班小时×费率）→ 绩效奖金
 *     → 社保/公积金（个人+公司，基数 min/max 钳制）→ 个税（累计预扣法）→ 实发
 * </pre>
 * 不持久化——返回内存 ErpHrSalary 对象，由 BizModel 层统一保存。
 *
 * <p>本类为纯编排组件（无事务），委托 {@link SocialInsuranceCalculator} 和 {@link IncomeTaxCalculator}。
 */
public class PayrollCalculator {

    /** 加班费默认小时费率（合同未配置时回退，payroll.md §5.2 注：加班费由合同/政策决定）。 */
    private static final BigDecimal DEFAULT_OVERTIME_HOURLY_RATE = new BigDecimal("50");
    /** 月标准工作日（用于出勤比例计算）。 */
    private static final BigDecimal DEFAULT_REQUIRED_WORK_DAYS = new BigDecimal("22");

    @Inject
    IDaoProvider daoProvider;
    @Inject
    SocialInsuranceCalculator socialInsuranceCalculator;
    @Inject
    IncomeTaxCalculator incomeTaxCalculator;

    /**
     * 计算单员工月度薪酬。返回填充完毕的内存 ErpHrSalary（approvalStatus=PENDING）。
     */
    public ErpHrSalary calculate(Long employeeId, int year, int month) {
        ErpHrEmploymentContract contract = findActiveContract(employeeId);
        if (contract == null) {
            throw new NopException(ErpHrErrors.ERR_EMPLOYMENT_CONTRACT_NOT_FOUND)
                    .param(ErpHrErrors.ARG_EMPLOYEE_ID, employeeId);
        }

        LocalDate periodStart = LocalDate.of(year, month, 1);
        LocalDate periodEnd = periodStart.plusMonths(1).minusDays(1);

        int scale = ErpHrConfigs.salaryRoundingScale();

        // 1. 出勤数据
        AttendanceSummary attendance = summarizeAttendance(employeeId, periodStart, periodEnd);
        BigDecimal requiredDays = attendance.requiredDays != null ? attendance.requiredDays : DEFAULT_REQUIRED_WORK_DAYS;
        // 无考勤记录视为全勤（合同月薪全额发放），避免新员工/无打卡数据时工资为 0
        BigDecimal actualDays = (attendance.actualDays == null || attendance.actualDays.signum() <= 0)
                ? requiredDays : attendance.actualDays;
        BigDecimal attendanceRatio = requiredDays.signum() == 0 ? BigDecimal.ONE
                : actualDays.divide(requiredDays, 6, RoundingMode.HALF_UP);

        // 1b. 无薪假扣减（config-gated，默认 false 向后兼容）
        BigDecimal unpaidLeaveDays = attendance.unpaidLeaveDays != null ? attendance.unpaidLeaveDays : BigDecimal.ZERO;
        if (ErpHrConfigs.deductUnpaidLeave() && unpaidLeaveDays.signum() > 0) {
            // 按比例扣减基本工资：basicSalary × (1 − unpaidLeaveDays / requiredDays)
            BigDecimal deductionRatio = unpaidLeaveDays.divide(requiredDays.signum() == 0 ? BigDecimal.ONE : requiredDays,
                    6, RoundingMode.HALF_UP);
            attendanceRatio = attendanceRatio.subtract(deductionRatio).max(BigDecimal.ZERO);
        }

        // 2. 基本工资（合同月薪 × 出勤比例）
        BigDecimal monthlySalary = nz(contract.getMonthlySalary());
        BigDecimal basicSalary = monthlySalary.multiply(attendanceRatio).setScale(scale, RoundingMode.HALF_UP);

        // 3. 津贴/补贴（固定项，全额发放；预留合同扩展字段，暂取 0）
        BigDecimal positionAllowance = BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        BigDecimal mealAllowance = BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        BigDecimal transportAllowance = BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);
        BigDecimal otherAllowance = BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);

        // 4. 加班费（加班小时 × 默认费率）
        BigDecimal overtimeHours = attendance.overtimeHours != null ? attendance.overtimeHours : BigDecimal.ZERO;
        BigDecimal overtimePay = overtimeHours.multiply(DEFAULT_OVERTIME_HOURLY_RATE)
                .setScale(scale, RoundingMode.HALF_UP);

        // 5. 绩效奖金（手工录入：本期新建为 0，后续 HR 录入或外部输入）
        BigDecimal performanceBonus = BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);

        // 6. 应发合计
        BigDecimal grossSalary = basicSalary.add(positionAllowance).add(performanceBonus).add(overtimePay)
                .add(mealAllowance).add(transportAllowance).add(otherAllowance)
                .setScale(scale, RoundingMode.HALF_UP);

        // 7. 社保（个人 + 公司）
        BigDecimal[] social = socialInsuranceCalculator.calculate(employeeId, year, month);
        BigDecimal socialInsuranceEE = social[0].setScale(scale, RoundingMode.HALF_UP);
        BigDecimal socialInsuranceER = social[1].setScale(scale, RoundingMode.HALF_UP);

        // 8. 公积金（个人 + 公司）
        BigDecimal[] fund = socialInsuranceCalculator.calculateHousingFund(employeeId, year, month);
        BigDecimal housingFundEE = fund[0].setScale(scale, RoundingMode.HALF_UP);
        BigDecimal housingFundER = fund[1].setScale(scale, RoundingMode.HALF_UP);

        // 9. 个税（累计预扣法，专项扣除 = 社保个人 + 公积金个人）
        BigDecimal specialDeduction = socialInsuranceEE.add(housingFundEE);
        Object[] taxResult = incomeTaxCalculator.calculate(employeeId, year, month, grossSalary, specialDeduction);
        BigDecimal taxAmount = (BigDecimal) taxResult[0];
        String cumulativeData = (String) taxResult[1];

        // 10. 其他扣款（手工录入，本期 0）
        BigDecimal otherDeductions = BigDecimal.ZERO.setScale(scale, RoundingMode.HALF_UP);

        // 11. 实发 = 应发 − 社保个人 − 公积金个人 − 个税 − 其他扣款
        BigDecimal netSalary = grossSalary.subtract(socialInsuranceEE).subtract(housingFundEE)
                .subtract(taxAmount).subtract(otherDeductions).setScale(scale, RoundingMode.HALF_UP);

        ErpHrSalary salary = daoProvider.daoFor(ErpHrSalary.class).newEntity();

        salary.setBusinessDate(io.nop.api.core.time.CoreMetrics.today());
        salary.setEmployeeId(employeeId);
        salary.setYear(year);
        salary.setMonth(month);
        salary.setBasicSalary(basicSalary);
        salary.setPositionAllowance(positionAllowance);
        salary.setPerformanceBonus(performanceBonus);
        salary.setOvertimePay(overtimePay);
        salary.setMealAllowance(mealAllowance);
        salary.setTransportAllowance(transportAllowance);
        salary.setOtherAllowance(otherAllowance);
        salary.setGrossSalary(grossSalary);
        salary.setSocialInsurance(socialInsuranceEE);
        salary.setHousingFund(housingFundEE);
        salary.setTaxAmount(taxAmount);
        salary.setOtherDeductions(otherDeductions);
        salary.setNetSalary(netSalary);
        salary.setPaymentStatus(ErpHrConstants.PAYMENT_PENDING);
        salary.setApproveStatus(ErpHrConstants.APPROVE_STATUS_UNSUBMITTED);
        salary.setActualWorkDays(actualDays);
        salary.setRequiredWorkDays(requiredDays);
        salary.setTotalOvertimeHours(overtimeHours);
        salary.setUnpaidLeaveDays(unpaidLeaveDays);
        salary.setCumulativeData(cumulativeData);
        // 公司承担部分暂存 remark 用于过账派发器读取（避免扩展实体字段）；
        // 正式存档于 PostingEvent.billData 而非持久化——见 SalaryPostingDispatcher。
        return salary;
    }

    /**
     * 以覆盖输入重算模拟薪酬（payroll-simulation.md §2.2/§三 即时应变）。
     *
     * <p>Explore 结论：{@link SocialInsuranceCalculator} 仅接受 employeeId 内部读 master（员工社保基数），
     * 不接受入参覆盖；{@link IncomeTaxCalculator} 接受 gross/specialDeduction 入参。故采用 Decision 降级方案——
     * 克隆源 ErpHrSalary 为 base，按 overrides 覆盖薪酬项目字段，重算 gross/tax/net。
     * 社保/公积金沿用源期间值（master 驱动，非月工资派生；社保基数钳制已在源期间核算时由
     * {@link SocialInsuranceCalculator#clamp} 应用）。0831-2 计算规则零修改。
     *
     * <p>overrides 的 key 为 ErpHrSalary 薪酬项目字段名（basicSalary/positionAllowance/performanceBonus/
     * overtimePay/mealAllowance/transportAllowance/otherAllowance/otherDeductions）。返回内存 ErpHrSalary，
     * 不持久化——由 BizModel 层决定是否落库。
     *
     * @param base           源期间 ErpHrSalary 快照（不为 null）
     * @param overrides      覆盖项（key=字段名，value=调整后值）；null 或空表示无调整，仅基于 base 重算
     * @param targetYear     模拟目标期间年（个税累计预扣窗口对齐模拟期；与 base.year/Month 可能不同）
     * @param targetMonth    模拟目标期间月
     * @return 模拟 ErpHrSalary（employee/year/month=模拟期，金额经覆盖重算）
     */
    public ErpHrSalary recalculateWithOverrides(ErpHrSalary base,
                                                java.util.Map<String, BigDecimal> overrides,
                                                int targetYear, int targetMonth) {
        if (base == null) {
            throw new NopException(ErpHrErrors.ERR_HR_SIMULATION_SOURCE_NOT_FOUND);
        }
        int scale = ErpHrConfigs.salaryRoundingScale();

        ErpHrSalary sim = base.cloneInstance();
        // 切到模拟期间——个税累计预扣窗口按模拟期查询历史
        sim.setYear(targetYear);
        sim.setMonth(targetMonth);
        // 清除主键——内存对象不持久化，避免误用为已存实体
        sim.orm_propValue(1, null);

        if (overrides != null) {
            for (java.util.Map.Entry<String, BigDecimal> e : overrides.entrySet()) {
                applyOverride(sim, e.getKey(), e.getValue());
            }
        }
        recalculateDerived(sim, scale);
        return sim;
    }

    void applyOverride(ErpHrSalary sim, String fieldName, BigDecimal value) {
        if (fieldName == null) {
            return;
        }
        BigDecimal v = value != null ? value : BigDecimal.ZERO;
        switch (fieldName) {
            case "basicSalary":
                sim.setBasicSalary(v);
                break;
            case "positionAllowance":
                sim.setPositionAllowance(v);
                break;
            case "performanceBonus":
                sim.setPerformanceBonus(v);
                break;
            case "overtimePay":
                sim.setOvertimePay(v);
                break;
            case "mealAllowance":
                sim.setMealAllowance(v);
                break;
            case "transportAllowance":
                sim.setTransportAllowance(v);
                break;
            case "otherAllowance":
                sim.setOtherAllowance(v);
                break;
            case "otherDeductions":
                sim.setOtherDeductions(v);
                break;
            default:
                // 未知字段忽略——未来扩展点
                break;
        }
    }

    /**
     * 据薪酬项目字段重算派生字段（gross/tax/net）。社保/公积金沿用 sim 当前值（master 驱动）。
     */
    void recalculateDerived(ErpHrSalary sim, int scale) {
        BigDecimal grossSalary = nz(sim.getBasicSalary())
                .add(nz(sim.getPositionAllowance()))
                .add(nz(sim.getPerformanceBonus()))
                .add(nz(sim.getOvertimePay()))
                .add(nz(sim.getMealAllowance()))
                .add(nz(sim.getTransportAllowance()))
                .add(nz(sim.getOtherAllowance()))
                .setScale(scale, RoundingMode.HALF_UP);
        sim.setGrossSalary(grossSalary);

        BigDecimal socialInsuranceEE = nz(sim.getSocialInsurance()).setScale(scale, RoundingMode.HALF_UP);
        BigDecimal housingFundEE = nz(sim.getHousingFund()).setScale(scale, RoundingMode.HALF_UP);
        BigDecimal specialDeduction = socialInsuranceEE.add(housingFundEE);

        Long employeeId = sim.getEmployeeId();
        int year = sim.getYear() != null ? sim.getYear() : 0;
        int month = sim.getMonth() != null ? sim.getMonth() : 0;
        Object[] taxResult = incomeTaxCalculator.calculate(employeeId, year, month, grossSalary, specialDeduction);
        BigDecimal taxAmount = (BigDecimal) taxResult[0];
        String cumulativeData = (String) taxResult[1];
        sim.setTaxAmount(taxAmount);
        sim.setCumulativeData(cumulativeData);

        BigDecimal netSalary = grossSalary.subtract(socialInsuranceEE).subtract(housingFundEE)
                .subtract(taxAmount).subtract(nz(sim.getOtherDeductions()))
                .setScale(scale, RoundingMode.HALF_UP);
        sim.setNetSalary(netSalary);
    }

    ErpHrEmploymentContract findActiveContract(Long employeeId) {
        IEntityDao<ErpHrEmploymentContract> dao = daoProvider.daoFor(ErpHrEmploymentContract.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("employeeId", employeeId));
        q.setLimit(1);
        List<ErpHrEmploymentContract> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    AttendanceSummary summarizeAttendance(Long employeeId, LocalDate periodStart, LocalDate periodEnd) {
        IEntityDao<ErpHrAttendance> dao = daoProvider.daoFor(ErpHrAttendance.class);
        QueryBean q = new QueryBean();
        q.addFilter(and(
                eq("employeeId", employeeId),
                ge("date", periodStart),
                le("date", periodEnd)));
        List<ErpHrAttendance> records = dao.findAllByQuery(q);

        AttendanceSummary summary = new AttendanceSummary();
        BigDecimal presentDays = BigDecimal.ZERO;
        BigDecimal overtimeHours = BigDecimal.ZERO;
        for (ErpHrAttendance a : records) {
            if (!Boolean.TRUE.equals(a.getIsAbsent())) {
                presentDays = presentDays.add(BigDecimal.ONE);
            }
            if (a.getWorkHours() != null) {
                BigDecimal standardHours = new BigDecimal("8");
                if (a.getWorkHours().compareTo(standardHours) > 0) {
                    overtimeHours = overtimeHours.add(a.getWorkHours().subtract(standardHours));
                }
            }
        }
        summary.actualDays = presentDays;
        summary.requiredDays = DEFAULT_REQUIRED_WORK_DAYS;
        summary.overtimeHours = overtimeHours;
        summary.unpaidLeaveDays = sumUnpaidLeaveDays(employeeId, periodStart, periodEnd);
        return summary;
    }

    /**
     * 汇总无薪假天数（UC-HR-06）：APPROVED 状态的 SICK/PERSONAL 类型休假落入核算期间的天数。
     * 视 SICK/PERSONAL 为无薪假（config-gated by erp-hr.deduct-unpaid-leave）。
     */
    BigDecimal sumUnpaidLeaveDays(Long employeeId, LocalDate periodStart, LocalDate periodEnd) {
        IEntityDao<ErpHrLeaveRequest> dao = daoProvider.daoFor(ErpHrLeaveRequest.class);
        QueryBean q = new QueryBean();
        q.addFilter(eq("employeeId", employeeId));
        q.addFilter(eq("status", ErpHrConstants.LEAVE_STATUS_APPROVED));
        q.addFilter(in("leaveType", List.of("SICK", "PERSONAL")));
        q.addFilter(ge("startDate", periodStart));
        q.addFilter(le("endDate", periodEnd));
        List<ErpHrLeaveRequest> unpaidLeaves = dao.findAllByQuery(q);
        BigDecimal sum = BigDecimal.ZERO;
        for (ErpHrLeaveRequest lr : unpaidLeaves) {
            if (lr.getDurationDays() != null) {
                sum = sum.add(lr.getDurationDays());
            }
        }
        return sum;
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    static class AttendanceSummary {
        BigDecimal actualDays;
        BigDecimal requiredDays;
        BigDecimal overtimeHours;
        BigDecimal unpaidLeaveDays;
    }
}
