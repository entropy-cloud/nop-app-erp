package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinBadDebt;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpFinBadDebt）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpFinBadDebt submitForApproval per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractSubmitForApprovalProcessor to activate the abstract base class; delegates to ErpFinBadDebtProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpFinBadDebtSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpFinBadDebt> {

    @Inject
    ErpFinBadDebtProcessor processor;

    public ErpFinBadDebtSubmitForApprovalProcessor() {
        super("ErpFinBadDebt");
    }

    @Override
    public ErpFinBadDebt submitForApproval(String id, IServiceContext context) {
        ErpFinBadDebt debt = processor.requireBadDebt(id);
        processor.validateTransitionForSubmit(debt);
        processor.doSubmit(debt);
        return debt;
    }

    @Override
    protected IEntityDao<ErpFinBadDebt> dao() {
        return daoProvider.daoFor(ErpFinBadDebt.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3）：非法迁移错误由 common 码改为直抛领域码
     * ERR_BAD_DEBT_ILLEGAL_APPROVAL_TRANSITION（模板参数 badDebtCode/currentStatus/expectedStatus）。
     */
    @Override
    protected NopException illegalStatusException(ErpFinBadDebt entity, String current, String... expected) {
        return new NopException(ErpFinErrors.ERR_BAD_DEBT_ILLEGAL_APPROVAL_TRANSITION)
                .param(ErpFinErrors.ARG_BAD_DEBT_CODE, entity.getCode())
                .param(ErpFinErrors.ARG_CURRENT_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getApproveStatus(ErpFinBadDebt entity) {
        return null;
    }

    @Override
    protected void setApproveStatus(ErpFinBadDebt entity, String status) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected boolean isCancelled(ErpFinBadDebt entity) {
        return false;
    }

    @Override
    protected String unsubmittedStatus() {
        return null;
    }

    @Override
    protected String submittedStatus() {
        return null;
    }

    @Override
    protected String rejectedStatus() {
        return null;
    }
}
