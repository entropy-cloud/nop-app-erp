package app.erp.qa.service.processor;

import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * NCR per-mutation Processor 共享基类（R6.6）。承载 postNcr/resolve/reverseNcr/upgradeToRecall 四个
 * per-mutation Processor 共用的加载、状态守卫与处置类型判定辅助（单一真相源，对齐
 * {@code processor-extension-pattern.md} facade protected helper 范式）。子类只编排单 mutation 步骤顺序。
 */
public abstract class AbstractErpQaNonConformanceProcessor {

    @Inject
    IDaoProvider daoProvider;

    protected IEntityDao<ErpQaNonConformance> ncrDao() {
        return daoProvider.daoFor(ErpQaNonConformance.class);
    }

    protected ErpQaNonConformance requireNcr(Long ncrId, IServiceContext context) {
        if (ncrId == null) {
            throw new NopException(ErpQaErrors.ERR_NCR_NOT_FOUND).param(ErpQaErrors.ARG_NCR_ID, ncrId);
        }
        ErpQaNonConformance ncr = ncrDao().getEntityById(ncrId);
        if (ncr == null) {
            throw new NopException(ErpQaErrors.ERR_NCR_NOT_FOUND).param(ErpQaErrors.ARG_NCR_ID, ncrId);
        }
        return ncr;
    }

    protected void requireNcrStatus(ErpQaNonConformance ncr, String expected, String expectedLabel) {
        String current = ncr.getStatus();
        if (current == null || !Objects.equals(current, expected)) {
            throw illegalNcrTransition(ncr, current, expectedLabel);
        }
    }

    protected NopException illegalNcrTransition(ErpQaNonConformance ncr, String current, String expected) {
        return new NopException(ErpQaErrors.ERR_INVALID_NCR_STATUS_TRANSITION)
                .param(ErpQaErrors.ARG_NCR_CODE, ncr.getCode())
                .param(ErpQaErrors.ARG_CURRENT_STATUS, current)
                .param(ErpQaErrors.ARG_EXPECTED_STATUS, expected);
    }

    protected static boolean isScrap(String disposition) {
        return ErpQaConstants.DISPOSITION_TYPE_SCRAP.equals(disposition);
    }

    protected static boolean isReturn(String disposition) {
        return ErpQaConstants.DISPOSITION_TYPE_RETURN.equals(disposition);
    }
}
