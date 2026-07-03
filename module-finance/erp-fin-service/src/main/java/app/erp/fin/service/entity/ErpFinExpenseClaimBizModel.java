
package app.erp.fin.service.entity;

import app.erp.fin.biz.IErpFinExpenseClaimBiz;
import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.service.processor.ErpFinExpenseClaimProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.orm.SingleSession;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 费用报销单 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。
 * 三轴审批状态机 + EXPENSE_CLAIM 业财过账 + 借款抵扣编排委托
 * {@link ErpFinExpenseClaimProcessor}（protected step 方法，下游可逐 step 覆盖）。
 *
 * <p>语义与配置门控见 {@code expense-claim.md}；{@code @BizMutation}+{@code @SingleSession} 钉事务/会话边界。
 */
@BizModel("ErpFinExpenseClaim")
public class ErpFinExpenseClaimBizModel extends CrudBizModel<ErpFinExpenseClaim> implements IErpFinExpenseClaimBiz {

    @Inject
    ErpFinExpenseClaimProcessor claimProcessor;

    public ErpFinExpenseClaimBizModel() {
        setEntityName(ErpFinExpenseClaim.class.getName());
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinExpenseClaim submit(@Name("claimId") Long claimId, IServiceContext context) {
        return claimProcessor.submit(claimId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinExpenseClaim withdrawSubmit(@Name("claimId") Long claimId, IServiceContext context) {
        return claimProcessor.withdrawSubmit(claimId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinExpenseClaim approve(@Name("claimId") Long claimId, IServiceContext context) {
        return claimProcessor.approve(claimId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinExpenseClaim reject(@Name("claimId") Long claimId, IServiceContext context) {
        return claimProcessor.reject(claimId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinExpenseClaim reverseApprove(@Name("claimId") Long claimId, IServiceContext context) {
        return claimProcessor.reverseApprove(claimId, context);
    }

    @Override
    @BizMutation
    @SingleSession
    public ErpFinExpenseClaim cancel(@Name("claimId") Long claimId, IServiceContext context) {
        return claimProcessor.cancel(claimId, context);
    }
}
