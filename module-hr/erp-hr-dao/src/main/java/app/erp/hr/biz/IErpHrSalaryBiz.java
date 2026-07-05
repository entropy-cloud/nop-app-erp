
package app.erp.hr.biz;

import app.erp.hr.dao.entity.ErpHrPayrollBankFile;
import app.erp.hr.dao.entity.ErpHrSalary;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;
import io.nop.wf.core.biz.IApprovableBiz;

import java.util.List;

/**
 * 薪酬记录聚合根 Biz（payroll.md §五/§六/§七）。标准审批动作（submitForApproval/approve/reject/
 * reverseApprove/withdrawApproval）由 {@link IApprovableBiz} 声明、平台 {@code approval-support.xbiz}
 * 标准 source 提供。本接口扩展薪酬核算引擎与支付轴动作：
 * <ul>
 *   <li>{@link #calculateSalary} 单员工月度核算（出勤比例→基本工资→津贴→加班→绩效→社保/公积金→个税累计预扣→实发）。</li>
 *   <li>{@link #runPayroll} 批量核算（幂等：同员工同期非 VOID 薪酬抛 ERR_SALARY_ALREADY_EXISTS）。</li>
 *   <li>{@link #markPaid}/{@link #voidSalary} 独立支付轴动作（approveStatus=APPROVED 前提下推进 paymentStatus）。</li>
 *   <li>{@link #generateBankFile} 银行代发文件生成。</li>
 * </ul>
 *
 * <p>Facade 只负责入口/事务；核算编排委托 {@code PayrollCalculator}，过账委托 {@code SalaryPostingDispatcher}。
 */
public interface IErpHrSalaryBiz extends ICrudBiz<ErpHrSalary>, IApprovableBiz<ErpHrSalary> {

    /**
     * 单员工月度薪酬核算。生成 ErpHrSalary（approveStatus=UNSUBMITTED, paymentStatus=PENDING）。
     */
    @BizMutation
    ErpHrSalary calculateSalary(@Name("employeeId") Long employeeId,
                                @Name("year") int year,
                                @Name("month") int month,
                                IServiceContext context);

    /**
     * 批量核算（遍历 ACTIVE/PROBATION 员工调 calculateSalary）。
     * 幂等：同员工同期已有非 VOID 薪酬跳过。
     */
    @BizMutation
    List<ErpHrSalary> runPayroll(@Name("year") int year,
                                 @Name("month") int month,
                                 IServiceContext context);

    /** approveStatus=APPROVED + paymentStatus=PENDING → paymentStatus=PAID（锁定，触发发放凭证）。 */
    @BizMutation
    ErpHrSalary markPaid(@Name("salaryId") Long salaryId, IServiceContext context);

    /** 非 PAID → paymentStatus=VOID（作废终态）。 */
    @BizMutation
    ErpHrSalary voidSalary(@Name("salaryId") Long salaryId, IServiceContext context);

    /**
     * 银行代发文件生成。检索 APPROVED + paymentStatus=PENDING 薪酬生成 ErpHrPayrollBankFile（GENERATED），
     * 标记 ErpHrSalary.paymentBatchNo + 写 paymentStatus=PAID。
     */
    @BizMutation
    ErpHrPayrollBankFile generateBankFile(@Name("year") int year,
                                          @Name("month") int month,
                                          @Name("bankId") Long bankId,
                                          IServiceContext context);

    /**
     * 查询某员工年度累计个税数据（用于核算预扣，可选辅助查询）。
     */
    @BizQuery
    String queryCumulativeTaxData(@Name("employeeId") Long employeeId,
                                  @Name("year") int year,
                                  @Name("upToMonth") int upToMonth,
                                  IServiceContext context);
}
