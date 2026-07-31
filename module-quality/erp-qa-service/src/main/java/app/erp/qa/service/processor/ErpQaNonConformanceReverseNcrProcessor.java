package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.service.ErpQaErrors;
import app.erp.qa.service.posting.NcrPostingDispatcher;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * ErpQaNonConformance reverseNcr per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 NCR 报废过账冲销编排（已过账 → 红冲 scrap 凭证）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 * 共享 helper 单一真相源在 {@link AbstractErpQaNonConformanceProcessor}。
 */
public class ErpQaNonConformanceReverseNcrProcessor extends AbstractErpQaNonConformanceProcessor {

    @Inject
    NcrPostingDispatcher ncrPostingDispatcher;

    public ErpQaNonConformance reverseNcr(Long ncrId, IServiceContext context) {
        ErpQaNonConformance ncr = requireNcr(ncrId, context);
        if (!Boolean.TRUE.equals(ncr.getPosted())) {
            throw new NopException(ErpQaErrors.ERR_NCR_NOT_POSTED).param(ErpQaErrors.ARG_NCR_CODE, ncr.getCode());
        }
        String disposition = ncr.getDispositionType();
        if (isScrap(disposition)) {
            ncrPostingDispatcher.reverseScrap(ncr);
        }
        ncrDao().updateEntity(ncr);
        return ncr;
    }
}
