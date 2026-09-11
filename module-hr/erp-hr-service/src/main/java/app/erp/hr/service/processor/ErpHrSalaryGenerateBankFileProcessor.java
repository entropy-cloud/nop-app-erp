package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrEmployee;
import app.erp.hr.dao.entity.ErpHrPayrollBankFile;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import app.erp.hr.service.posting.SalaryPostingDispatcher;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static io.nop.api.core.beans.FilterBeans.in;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpHrPayrollBankFile、ErpHrEmployee）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpHrSalary generateBankFile per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含银行代发文件生成（可发放薪酬查询 + CSV 内容拼装 + 逐条 PAID 标记 + BankFile 落库 + 回填 bankFileId）。
 * 固定状态判断委托实体级 StateMachine Bean（契约 §4/§7）：可发放守卫经 {@link AbstractErpHrSalaryProcessor#findPayableSalaries}
 * 查询级实现（APPROVED+PENDING），PAID 目标态改调 {@code ErpHrSalaryPaymentStateMachine.markPaidTargetStatus()}。
 *
 * <p>P1-CK-hr2-002：批量发放路径与单笔 markPaid 同构——逐条 {@code postingDispatcher.tryPostPayment}
 * 生成 SALARY_PAYMENT(280) 凭证（payroll.md §5.2 步骤 4；内含 F1.2 去重守卫，重试幂等）；
 * 文件格式对齐 §7.2 五列「序号,账号,户名,金额,用途」。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrSalaryProcessor}。
 */
public class ErpHrSalaryGenerateBankFileProcessor extends AbstractErpHrSalaryProcessor {

    @Inject
    SalaryPostingDispatcher postingDispatcher;

    public ErpHrPayrollBankFile generateBankFile(int year, int month, String bankId, IServiceContext context) {
        List<ErpHrSalary> pending = findPayableSalaries(year, month, context);
        if (pending.isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_NO_APPROVED_SALARY_FOR_BANK_FILE)
                    .param(ErpHrErrors.ARG_BANK_ID, bankId);
        }
        Map<String, ErpHrEmployee> employeesById = loadEmployeesById(pending);
        String batchNo = "PAY-" + year + String.format("%02d", month) + "-" + CoreMetrics.nanoTime();
        StringBuilder content = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (ErpHrSalary s : pending) {
            count++;
            // P1-CK-hr2-002：镜像 markPaid 序列（过账 → reload → PAID 翻转）。失败吞异常返回 false
            // 不阻塞 PAID 终态（G3 错误传播分级，与 markPaid 同语义），告警由 dispatcher 内部派发。
            postingDispatcher.tryPostPayment(s);
            s = salaryDao().getEntityById(s.getId());
            BigDecimal net = nz(s.getNetSalary());
            total = total.add(net);
            ErpHrEmployee emp = employeesById.get(s.getEmployeeId());
            content.append(String.format("%03d", count)).append(",")
                    .append(emp != null && emp.getBankAccountId() != null ? emp.getBankAccountId() : "").append(",")
                    .append(emp != null && emp.getFullName() != null ? emp.getFullName() : "").append(",")
                    .append(net.toPlainString()).append(",SALARY\n");
            s.setPaymentBatchNo(batchNo);
            s.setPaymentStatus(paymentStateMachine.markPaidTargetStatus());
            s.setPaymentDate(CoreMetrics.today());
            salaryDao().updateEntity(s);
        }

        IEntityDao<ErpHrPayrollBankFile> bankFileDao = daoProvider.daoFor(ErpHrPayrollBankFile.class);
        ErpHrPayrollBankFile bankFile = bankFileDao.newEntity();
        bankFile.setBatchNo(batchNo);
        bankFile.setPaymentDate(CoreMetrics.today());
        bankFile.setTotalAmount(total);
        bankFile.setRecordCount(count);
        bankFile.setFileFormat(ErpHrConstants.BANK_FILE_FORMAT_CSV);
        bankFile.setFileContent(content.toString());
        bankFile.setStatus(ErpHrConstants.BANK_FILE_STATUS_GENERATED);
        bankFile.setBankId(bankId);
        bankFileDao.saveEntity(bankFile);

        for (ErpHrSalary s : pending) {
            s.setBankFileId(bankFile.getId());
            salaryDao().updateEntity(s);
        }
        return bankFile;
    }

    /** 单查询批量加载员工（防 N+1），供文件内容读取账号/户名。 */
    private Map<String, ErpHrEmployee> loadEmployeesById(List<ErpHrSalary> salaries) {
        List<String> ids = new ArrayList<>();
        for (ErpHrSalary s : salaries) {
            if (s.getEmployeeId() != null) {
                ids.add(s.getEmployeeId());
            }
        }
        Map<String, ErpHrEmployee> result = new LinkedHashMap<>();
        if (ids.isEmpty()) {
            return result;
        }
        QueryBean q = new QueryBean();
        q.addFilter(in("id", ids));
        List<ErpHrEmployee> employees = daoProvider.daoFor(ErpHrEmployee.class).findAllByQuery(q);
        for (ErpHrEmployee emp : employees) {
            result.put(emp.getId(), emp);
        }
        return result;
    }
}
