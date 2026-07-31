
package app.erp.qa.service.entity;

import app.erp.qa.biz.IErpQaNonConformanceBiz;
import app.erp.qa.dao.entity.ErpQaNonConformance;
import app.erp.qa.dao.entity.ErpQaRecall;
import app.erp.qa.service.ErpQaConstants;
import app.erp.qa.service.ErpQaErrors;
import app.erp.qa.service.processor.ErpQaNonConformancePostNcrProcessor;
import app.erp.qa.service.processor.ErpQaNonConformanceResolveProcessor;
import app.erp.qa.service.processor.ErpQaNonConformanceReverseNcrProcessor;
import app.erp.qa.service.processor.ErpQaNonConformanceUpgradeToRecallProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * NCR BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。在 {@link CrudBizModel} 标准 CRUD 之上
 * 实现 NCR 5 态状态机（{@code docs/design/quality/state-machine.md §适用对象二`}）。单步状态翻转（submitReview/
 * escalateToRecall/cancel）留在 Facade；多步 mutation（resolve 财务过账分派 / postNcr / reverseNcr /
 * upgradeToRecall）委托 per-mutation Processor，下游可经 Delta beans.xml 同名 bean id 覆盖。
 *
 * <p>非法迁移抛 {@link ErpQaErrors#ERR_INVALID_NCR_STATUS_TRANSITION}。
 */
@BizModel("ErpQaNonConformance")
public class ErpQaNonConformanceBizModel extends CrudBizModel<ErpQaNonConformance> implements IErpQaNonConformanceBiz {

    @Inject
    ErpQaNonConformanceResolveProcessor resolveProcessor;
    @Inject
    ErpQaNonConformancePostNcrProcessor postNcrProcessor;
    @Inject
    ErpQaNonConformanceReverseNcrProcessor reverseNcrProcessor;
    @Inject
    ErpQaNonConformanceUpgradeToRecallProcessor upgradeToRecallProcessor;

    public ErpQaNonConformanceBizModel() {
        setEntityName(ErpQaNonConformance.class.getName());
    }

    @Override
    @BizMutation
    public ErpQaNonConformance submitReview(@Name("ncrId") Long ncrId, IServiceContext context) {
        ErpQaNonConformance ncr = requireNcr(ncrId, context);
        requireNcrStatus(ncr, ErpQaConstants.NCR_STATUS_OPEN, "OPEN");
        ncr.setStatus(ErpQaConstants.NCR_STATUS_IN_REVIEW);
        updateEntity(ncr, null, context);
        return ncr;
    }

    @Override
    @BizMutation
    public ErpQaNonConformance resolve(@Name("ncrId") Long ncrId,
                                       @Name("resolution") String resolution,
                                       @Optional @Name("noCapaReason") String noCapaReason,
                                       IServiceContext context) {
        return resolveProcessor.resolve(ncrId, resolution, noCapaReason, context);
    }

    @Override
    @BizMutation
    public ErpQaNonConformance postNcr(@Name("ncrId") Long ncrId, IServiceContext context) {
        return postNcrProcessor.postNcr(ncrId, context);
    }

    @Override
    @BizMutation
    public ErpQaNonConformance reverseNcr(@Name("ncrId") Long ncrId, IServiceContext context) {
        return reverseNcrProcessor.reverseNcr(ncrId, context);
    }

    @Override
    @BizMutation
    public ErpQaNonConformance escalateToRecall(@Name("ncrId") Long ncrId, IServiceContext context) {
        ErpQaNonConformance ncr = requireNcr(ncrId, context);
        requireNcrStatus(ncr, ErpQaConstants.NCR_STATUS_IN_REVIEW, "IN_REVIEW");
        // 升级为召回（终态，仅状态迁移占位；不建召回实体。真正建召回用 upgradeToRecall）
        ncr.setStatus(ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL);
        updateEntity(ncr, null, context);
        return ncr;
    }

    @Override
    @BizMutation
    public ErpQaRecall upgradeToRecall(@Name("ncrId") Long ncrId, IServiceContext context) {
        return upgradeToRecallProcessor.upgradeToRecall(ncrId, context);
    }

    @Override
    @BizMutation
    public ErpQaNonConformance cancel(@Name("ncrId") Long ncrId, IServiceContext context) {
        ErpQaNonConformance ncr = requireNcr(ncrId, context);
        String current = ncr.getStatus();
        if (current == null || (!Objects.equals(current, ErpQaConstants.NCR_STATUS_OPEN)
                && !Objects.equals(current, ErpQaConstants.NCR_STATUS_IN_REVIEW))) {
            throw illegalNcrTransition(ncr, current, "OPEN 或 IN_REVIEW");
        }
        ncr.setStatus(ErpQaConstants.NCR_STATUS_CANCELLED);
        updateEntity(ncr, null, context);
        return ncr;
    }

    // ---------- helpers ----------

    private ErpQaNonConformance requireNcr(Long ncrId, IServiceContext context) {
        if (ncrId == null) {
            throw new NopException(ErpQaErrors.ERR_NCR_NOT_FOUND).param(ErpQaErrors.ARG_NCR_ID, ncrId);
        }
        return requireEntity(String.valueOf(ncrId), null, context);
    }

    private void requireNcrStatus(ErpQaNonConformance ncr, String expected, String expectedLabel) {
        String current = ncr.getStatus();
        if (current == null || !Objects.equals(current, expected)) {
            throw illegalNcrTransition(ncr, current, expectedLabel);
        }
    }

    private NopException illegalNcrTransition(ErpQaNonConformance ncr, String current, String expected) {
        return new NopException(ErpQaErrors.ERR_INVALID_NCR_STATUS_TRANSITION)
                .param(ErpQaErrors.ARG_NCR_CODE, ncr.getCode())
                .param(ErpQaErrors.ARG_CURRENT_STATUS, current)
                .param(ErpQaErrors.ARG_EXPECTED_STATUS, expected);
    }
}
