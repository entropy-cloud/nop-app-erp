
package app.erp.fin.biz;

import app.erp.fin.dao.entity.ErpFinBankReconciliation;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.core.context.IServiceContext;
import io.nop.orm.biz.ICrudBiz;

public interface IErpFinBankReconciliationBiz extends ICrudBiz<ErpFinBankReconciliation> {

    /**
     * 按对账单生成余额调节表（DRAFT）：聚合已勾对 + 未达账项，校验平衡恒等式
     * {@code bankBalance + 在途 = bookBalance + 未达}。
     *
     * <p>期间门控：对账单所属期间 glStatus=CLOSED 时拒绝生成。
     * 不平衡（{@code unreconciledDiff ≠ 0}）抛 {@code NopException} 阻止完成。
     */
    @BizMutation
    ErpFinBankReconciliation generate(@Name("statementId") Long statementId, IServiceContext context);

    /**
     * 过账调节表：置 docStatus=POSTED；若存在「银行已记企业未记」项，
     * 生成 {@code BANK_RECON_ADJ} 调整凭证（经 {@code IErpFinVoucherBiz.post}）。
     */
    @BizMutation
    ErpFinBankReconciliation post(@Name("reconciliationId") Long reconciliationId, IServiceContext context);

    /**
     * 红冲未达账项调整凭证（经 {@code IErpFinVoucherBiz.reverse}，
     * 按 {@code ErpFinVoucherBillR.businessType}=BANK_RECON_ADJ 反查）+ 调节表 docStatus=POSTED→CANCELLED。
     */
    @BizMutation
    ErpFinBankReconciliation reverse(@Name("reconciliationId") Long reconciliationId, IServiceContext context);
}
