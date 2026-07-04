
package app.erp.hr.biz;

import app.erp.hr.dao.entity.ErpHrPayrollBankFile;
import app.erp.hr.dao.entity.ErpHrSalary;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

import java.util.List;

/**
 * 薪酬记录聚合根 Biz（payroll.md §五/§六/§七）。除标准 CRUD 外，承载薪酬核算引擎与审批工作流入口：
 * <ul>
 *   <li>{@link #calculateSalary} 单员工月度核算（出勤比例→基本工资→津贴→加班→绩效→社保/公积金→个税累计预扣→实发）。</li>
 *   <li>{@link #runPayroll} 批量核算（幂等：同员工同期非 VOID 薪酬抛 ERR_SALARY_ALREADY_EXISTS）。</li>
 *   <li>审批状态机：{@link #review}/{@link #approveFinance}/{@link #approveManager}/
 *       {@link #rejectSalary}/{@link #voidSalary}/{@link #markPaid}（payroll.md §6）。</li>
 *   <li>{@link #generateBankFile} 银行代发文件生成。</li>
 * </ul>
 *
 * <p>Facade 只负责入口/事务；核算编排委托 {@code PayrollCalculator}，过账委托 {@code SalaryPostingDispatcher}。
 */
public interface IErpHrSalaryBiz extends ICrudBiz<ErpHrSalary> {

    /**
     * 单员工月度薪酬核算。生成 ErpHrSalary（approvalStatus=PENDING）。
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

    /** PENDING → REVIEWED（HR 复核）。 */
    @BizMutation
    ErpHrSalary review(@Name("salaryId") Long salaryId, IServiceContext context);

    /** REVIEWED → APPROVED_FINANCE（财务审批）。 */
    @BizMutation
    ErpHrSalary approveFinance(@Name("salaryId") Long salaryId, IServiceContext context);

    /** APPROVED_FINANCE → APPROVED_MANAGER（经理审批，触发计提凭证）。 */
    @BizMutation
    ErpHrSalary approveManager(@Name("salaryId") Long salaryId, IServiceContext context);

    /** REVIEWED|APPROVED_FINANCE → PENDING（退回 HR 修正）。 */
    @BizMutation
    ErpHrSalary rejectSalary(@Name("salaryId") Long salaryId, IServiceContext context);

    /** 非 PAID → VOID（作废终态）。 */
    @BizMutation
    ErpHrSalary voidSalary(@Name("salaryId") Long salaryId, IServiceContext context);

    /** APPROVED_MANAGER → PAID（终态，锁定，触发发放凭证）。 */
    @BizMutation
    ErpHrSalary markPaid(@Name("salaryId") Long salaryId, IServiceContext context);

    /**
     * 银行代发文件生成。检索 APPROVED_MANAGER 薪酬生成 ErpHrPayrollBankFile（GENERATED），
     * 标记 ErpHrSalary.paymentBatchNo + 写 approvalStatus=PAID（同步 paymentStatus=PAID）。
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
