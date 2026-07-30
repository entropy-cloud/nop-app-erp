package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalOrder;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalOrder cancel per-mutation Processor (plan 2026-07-30-1433-2 R5.2, no xbiz source).
 * Runs the AbstractCancelProcessor skeleton; beforeCancel 承载 commitment-release + intercompany-reverse hooks。
 * 经 BizModel Java 调用，R5.8 重配线前不在 xbiz 委托链（运行时验证移交 R5.8）。
 */
public class ErpSalOrderCancelProcessor extends AbstractCancelProcessor<ErpSalOrder> {

    @Inject
    ErpSalOrderProcessor processor;

    @Override
    protected IEntityDao<ErpSalOrder> dao() {
        return daoProvider.daoFor(ErpSalOrder.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_ORDER_NOT_FOUND)
                .param(ErpSalErrors.ARG_ORDER_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalOrder entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_ORDER_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_ORDER_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected void beforeCancel(ErpSalOrder entity, IServiceContext context) {
        processor.runCommitmentReleaseHook(entity, context);
        processor.runIntercompanyReverseHook(entity, context);
    }

    @Override
    protected String getDocStatus(ErpSalOrder entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpSalOrder entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpSalConstants.DOC_STATUS_CANCELLED;
    }
}
