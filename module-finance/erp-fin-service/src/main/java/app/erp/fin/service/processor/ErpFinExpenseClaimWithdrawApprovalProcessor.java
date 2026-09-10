package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinExpenseClaim;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.common.service.AbstractWithdrawApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpFinExpenseClaim）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpFinExpenseClaim withdrawApproval per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractWithdrawApprovalProcessor to activate the abstract base class; delegates to ErpFinExpenseClaimProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpFinExpenseClaimWithdrawApprovalProcessor extends AbstractWithdrawApprovalProcessor<ErpFinExpenseClaim> {

    @Inject
    ErpFinExpenseClaimProcessor processor;

    @Override
    public ErpFinExpenseClaim withdrawApproval(String id, IServiceContext context) {
        ErpFinExpenseClaim claim = processor.requireClaim(id, context);
        processor.validateNotCancelled(claim, context);
        processor.validateTransitionForWithdraw(claim, context);
        processor.doWithdrawSubmit(claim, context);
        return claim;
    }

    @Override
    protected IEntityDao<ErpFinExpenseClaim> dao() {
        return daoProvider.daoFor(ErpFinExpenseClaim.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3）：非法迁移错误由 common 码改为直抛领域码
     * ERR_EXPENSE_CLAIM_ILLEGAL_STATUS_TRANSITION（模板参数 claimCode/currentStatus/expectedStatus）。
     */
    @Override
    protected NopException illegalStatusException(ErpFinExpenseClaim entity, String current, String... expected) {
        return new NopException(ErpFinErrors.ERR_EXPENSE_CLAIM_ILLEGAL_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_CLAIM_CODE, entity.getCode())
                .param(ErpFinErrors.ARG_CURRENT_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getApproveStatus(ErpFinExpenseClaim entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpFinExpenseClaim entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected boolean isCancelled(ErpFinExpenseClaim entity) {
        return entity.isCancelled();
    }

    @Override
    protected String unsubmittedStatus() {
        return ErpFinConstants.APPROVE_STATUS_UNSUBMITTED;
    }

    @Override
    protected String submittedStatus() {
        return ErpFinConstants.APPROVE_STATUS_SUBMITTED;
    }
}
