package app.erp.hr.service.processor;

import app.erp.hr.dao.entity.ErpHrPayrollBankFile;
import app.erp.hr.dao.entity.ErpHrSalary;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

import java.math.BigDecimal;
import java.util.List;

/**
 * ErpHrSalary generateBankFile per-mutation Processor（R6.7，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含银行代发文件生成（可发放薪酬查询 + CSV 内容拼装 + 逐条 PAID 标记 + BankFile 落库 + 回填 bankFileId），薪酬语义不变（payroll.md §七）。
 * 固定状态判断委托实体级 StateMachine Bean（契约 §4/§7）：可发放守卫经 {@link AbstractErpHrSalaryProcessor#findPayableSalaries}
 * 查询级实现（APPROVED+PENDING），PAID 目标态改调 {@code ErpHrSalaryPaymentStateMachine.markPaidTargetStatus()}。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpHrSalaryProcessor}。
 */
public class ErpHrSalaryGenerateBankFileProcessor extends AbstractErpHrSalaryProcessor {

    public ErpHrPayrollBankFile generateBankFile(int year, int month, String bankId, IServiceContext context) {
        List<ErpHrSalary> pending = findPayableSalaries(year, month, context);
        if (pending.isEmpty()) {
            throw new NopException(ErpHrErrors.ERR_NO_APPROVED_SALARY_FOR_BANK_FILE)
                    .param(ErpHrErrors.ARG_BANK_ID, bankId);
        }
        String batchNo = "PAY-" + year + String.format("%02d", month) + "-" + CoreMetrics.nanoTime();
        StringBuilder content = new StringBuilder();
        BigDecimal total = BigDecimal.ZERO;
        int count = 0;
        for (ErpHrSalary s : pending) {
            count++;
            BigDecimal net = nz(s.getNetSalary());
            total = total.add(net);
            content.append(String.format("%03d", count)).append(",")
                    .append(s.getEmployeeId()).append(",")
                    .append(net.toPlainString()).append(",工资\n");
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
}
