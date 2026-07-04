package app.erp.hr.service.payroll;

import app.erp.hr.dao.entity.ErpHrAttendance;
import app.erp.hr.dao.entity.ErpHrEmploymentContract;
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

        ErpHrSalary salary = new ErpHrSalary();
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
        salary.setApprovalStatus(ErpHrConstants.APPROVAL_PENDING);
        salary.setActualWorkDays(actualDays);
        salary.setRequiredWorkDays(requiredDays);
        salary.setTotalOvertimeHours(overtimeHours);
        salary.setUnpaidLeaveDays(BigDecimal.ZERO);
        salary.setCumulativeData(cumulativeData);
        // 公司承担部分暂存 remark 用于过账派发器读取（避免扩展实体字段）；
        // 正式存档于 PostingEvent.billData 而非持久化——见 SalaryPostingDispatcher。
        return salary;
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
        return summary;
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    static class AttendanceSummary {
        BigDecimal actualDays;
        BigDecimal requiredDays;
        BigDecimal overtimeHours;
    }
}
