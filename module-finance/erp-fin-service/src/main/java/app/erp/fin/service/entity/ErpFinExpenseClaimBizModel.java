
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinExpenseClaimBiz;
import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.service.processor.ErpFinExpenseClaimCancelProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 费用报销单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）经 xbiz 单行委托 per-mutation Processor 全权处理；非审批动作（cancel）经
 * per-mutation {@link ErpFinExpenseClaimCancelProcessor}。
 */
@BizModel("ErpFinExpenseClaim")
public class ErpFinExpenseClaimBizModel extends CrudBizModel<ErpFinExpenseClaim> implements IErpFinExpenseClaimBiz {

    @Inject
    ErpFinExpenseClaimCancelProcessor cancelProcessor;

    public ErpFinExpenseClaimBizModel() {
        setEntityName(ErpFinExpenseClaim.class.getName());
    }

    @Override
    @BizMutation
    public ErpFinExpenseClaim cancel(@Name("claimId") Long claimId, IServiceContext context) {
        return cancelProcessor.cancel(String.valueOf(claimId), context);
    }

}
