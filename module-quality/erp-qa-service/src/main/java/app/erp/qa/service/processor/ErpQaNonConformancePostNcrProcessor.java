package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import app.erp.qa.service.posting.NcrPostingDispatcher;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpQaNonConformance postNcr per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 RESOLVED NCR 报废处置人工过账编排（{@code docs/design/quality/ncr.md §财务过账`}）。
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。共享 helper 单一真相源在 {@link AbstractErpQaNonConformanceProcessor}。
 */
public class ErpQaNonConformancePostNcrProcessor extends AbstractErpQaNonConformanceProcessor {

    @Inject
    NcrPostingDispatcher ncrPostingDispatcher;

    public ErpQaNonConformance postNcr(Long ncrId, IServiceContext context) {
        ErpQaNonConformance ncr = requireNcr(ncrId, context);
        requireNcrStatus(ncr, ErpQaConstants.NCR_STATUS_RESOLVED, "RESOLVED");
        if (Boolean.TRUE.equals(ncr.getPosted())) {
            throw new NopException(ErpQaErrors.ERR_NCR_ALREADY_POSTED).param(ErpQaErrors.ARG_NCR_CODE, ncr.getCode());
        }
        String disposition = ncr.getDispositionType();
        if (!isScrap(disposition)) {
            throw new NopException(ErpQaErrors.ERR_NCR_DISPOSITION_NOT_POSTABLE)
                    .param(ErpQaErrors.ARG_NCR_CODE, ncr.getCode())
                    .param(ErpQaErrors.ARG_DISPOSITION_TYPE, disposition);
        }
        ncrPostingDispatcher.dispatchScrap(ncr, context);
        ncrDao().updateEntity(ncr);
        return ncr;
    }
}
