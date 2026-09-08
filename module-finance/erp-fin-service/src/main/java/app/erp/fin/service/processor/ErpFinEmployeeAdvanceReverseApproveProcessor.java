package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpFinEmployeeAdvance reverseApprove per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractReverseApproveProcessor to activate the abstract base class; delegates to ErpFinEmployeeAdvanceProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpFinEmployeeAdvanceReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpFinEmployeeAdvance> {

    @Inject
    ErpFinEmployeeAdvanceProcessor processor;

    @Override
    public ErpFinEmployeeAdvance reverseApprove(String id, IServiceContext context) {
        ErpFinEmployeeAdvance advance = processor.requireAdvance(id, context);
        if (advance.isRejected()) {
            return advance;
        }
        processor.validateTransitionForReverseApprove(advance, context);
        return processor.doReverseApprove(id, advance, context);
    }

    @Override
    protected IEntityDao<ErpFinEmployeeAdvance> dao() {
        return daoProvider.daoFor(ErpFinEmployeeAdvance.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3）：非法迁移错误由 common 码改为直抛领域码
     * ERR_EMPLOYEE_ADVANCE_ILLEGAL_STATUS_TRANSITION（模板参数 advanceCode/currentStatus/expectedStatus）。
     */
    @Override
    protected NopException illegalStatusException(ErpFinEmployeeAdvance entity, String current, String... expected) {
        return new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_ADVANCE_CODE, entity.getCode())
                .param(ErpFinErrors.ARG_CURRENT_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getApproveStatus(ErpFinEmployeeAdvance entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpFinEmployeeAdvance entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpFinEmployeeAdvance entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpFinEmployeeAdvance entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpFinEmployeeAdvance entity) {
        return entity.isRejected();
    }

    @Override
    protected String approvedStatus() {
        return ErpFinConstants.APPROVE_STATUS_APPROVED;
    }

    @Override
    protected String submittedStatus() {
        return ErpFinConstants.APPROVE_STATUS_SUBMITTED;
    }
}
