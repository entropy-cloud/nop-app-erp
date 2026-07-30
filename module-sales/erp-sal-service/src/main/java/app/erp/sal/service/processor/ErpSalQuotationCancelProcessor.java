package app.erp.sal.service.processor;

import app.erp.sal.dao.entity.ErpSalQuotation;
import app.erp.sal.service.ErpSalConstants;
import app.erp.sal.service.ErpSalErrors;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpSalQuotation cancel per-mutation Processor (plan 2026-07-30-1433-2 R5.2, no xbiz source).
 * Runs the AbstractCancelProcessor skeleton; 报价单 cancel 无域特有 hook（facade cancel 仅 setDocStatus）。
 * 经 BizModel Java 调用，R5.8 重配线前不在 xbiz 委托链（运行时验证移交 R5.8）。
 */
public class ErpSalQuotationCancelProcessor extends AbstractCancelProcessor<ErpSalQuotation> {

    @Inject
    ErpSalQuotationProcessor processor;

    @Override
    protected IEntityDao<ErpSalQuotation> dao() {
        return daoProvider.daoFor(ErpSalQuotation.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return new NopException(ErpSalErrors.ERR_QUOTATION_NOT_FOUND)
                .param(ErpSalErrors.ARG_QUOTATION_ID, id);
    }

    @Override
    protected NopException illegalStatusException(ErpSalQuotation entity, String current, String... expected) {
        return new NopException(ErpSalErrors.ERR_QUOTATION_ILLEGAL_DOC_STATUS_TRANSITION)
                .param(ErpSalErrors.ARG_QUOTATION_CODE, entity.getCode())
                .param(ErpSalErrors.ARG_CURRENT_DOC_STATUS, current)
                .param(ErpSalErrors.ARG_EXPECTED_DOC_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getDocStatus(ErpSalQuotation entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpSalQuotation entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpSalConstants.DOC_STATUS_CANCELLED;
    }
}
