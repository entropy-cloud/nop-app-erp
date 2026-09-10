package app.erp.fin.service.processor;

import app.erp.fin.dao.entity.ErpFinEmployeeAdvance;
import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpFinEmployeeAdvance）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpFinEmployeeAdvance cancel per-mutation Processor (plan 2026-07-25-1057-2).
 * Extends AbstractCancelProcessor to activate the abstract base class; delegates to ErpFinEmployeeAdvanceProcessor
 * for behavior equivalence. Downstream can override via Delta beans.xml with same bean id.
 */
public class ErpFinEmployeeAdvanceCancelProcessor extends AbstractCancelProcessor<ErpFinEmployeeAdvance> {

    @Inject
    ErpFinEmployeeAdvanceProcessor processor;

    @Override
    public ErpFinEmployeeAdvance cancel(String id, IServiceContext context) {
        ErpFinEmployeeAdvance advance = processor.requireAdvance(id, context);
        processor.validateTransitionForCancel(advance, context);
        return processor.doCancel(id, advance, context);
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
     * ERR_EMPLOYEE_ADVANCE_ILLEGAL_DOC_STATUS_TRANSITION（模板参数 advanceCode/currentDocStatus/expectedDocStatus）。
     */
    @Override
    protected NopException illegalStatusException(ErpFinEmployeeAdvance entity, String current, String... expected) {
        return new NopException(ErpFinErrors.ERR_EMPLOYEE_ADVANCE_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_ADVANCE_CODE, entity.getCode())
                .param(ErpFinErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpFinErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getDocStatus(ErpFinEmployeeAdvance entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpFinEmployeeAdvance entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpFinConstants.DOC_STATUS_CANCELLED;
    }
}
