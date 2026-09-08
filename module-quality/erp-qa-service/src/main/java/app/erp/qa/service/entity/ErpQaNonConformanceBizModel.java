
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
import app.erp.qa.service.statemachine.ErpQaNonConformanceStateMachine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * NCR BizModel（Facade，{@code processor-extension-pattern.md} 两层结构）。在 {@link CrudBizModel} 标准 CRUD 之上
 * 实现 NCR 5 态状态机（{@code docs/design/quality/state-machine.md §适用对象二`}）。单步状态翻转（submitReview/
 * escalateToRecall/cancel）留在 Facade；多步 mutation（resolve 财务过账分派 / postNcr / reverseNcr /
 * upgradeToRecall）委托 per-mutation Processor，下游可经 Delta beans.xml 同名 bean id 覆盖。
 *
 * <p>status 轴固定来源态/目标态判断委托 {@link ErpQaNonConformanceStateMachine}（实体级状态机 Bean，契约 §4/§7）。
 * 非法迁移抛 {@link ErpQaErrors#ERR_INVALID_NCR_STATUS_TRANSITION}。
 */
@BizModel("ErpQaNonConformance")
public class ErpQaNonConformanceBizModel extends AbstractErpCrudBizModel<ErpQaNonConformance> implements IErpQaNonConformanceBiz {

    @Inject
    ErpQaNonConformanceResolveProcessor resolveProcessor;
    @Inject
    ErpQaNonConformancePostNcrProcessor postNcrProcessor;
    @Inject
    ErpQaNonConformanceReverseNcrProcessor reverseNcrProcessor;
    @Inject
    ErpQaNonConformanceUpgradeToRecallProcessor upgradeToRecallProcessor;
    @Inject
    ErpQaNonConformanceStateMachine ncrStateMachine;

    public ErpQaNonConformanceBizModel() {
        setEntityName(ErpQaNonConformance.class.getName());
    }

    @Override
    @BizMutation
    public ErpQaNonConformance submitReview(@Name("ncrId") String ncrId, IServiceContext context) {
        ErpQaNonConformance ncr = requireNcr(ncrId, context);
        String current = ncr.getStatus();
        try {
            ncrStateMachine.assertCanSubmitReview(current);
        } catch (NopException e) {
            throw e.param(ErpQaErrors.ARG_NCR_CODE, ncr.getCode());
        }
        ncr.setStatus(ncrStateMachine.submitReviewTargetStatus());
        updateEntity(ncr, null, context);
        return ncr;
    }

    @Override
    @BizMutation
    public ErpQaNonConformance resolve(@Name("ncrId") String ncrId,
                                       @Name("resolution") String resolution,
                                       @Optional @Name("noCapaReason") String noCapaReason,
                                       IServiceContext context) {
        return resolveProcessor.resolve(ncrId, resolution, noCapaReason, context);
    }

    @Override
    @BizMutation
    public ErpQaNonConformance postNcr(@Name("ncrId") String ncrId, IServiceContext context) {
        return postNcrProcessor.postNcr(ncrId, context);
    }

    @Override
    @BizMutation
    public ErpQaNonConformance reverseNcr(@Name("ncrId") String ncrId, IServiceContext context) {
        return reverseNcrProcessor.reverseNcr(ncrId, context);
    }

    @Override
    @BizMutation
    public ErpQaNonConformance escalateToRecall(@Name("ncrId") String ncrId, IServiceContext context) {
        ErpQaNonConformance ncr = requireNcr(ncrId, context);
        String current = ncr.getStatus();
        try {
            ncrStateMachine.assertCanUpgradeToRecall(current);
        } catch (NopException e) {
            throw e.param(ErpQaErrors.ARG_NCR_CODE, ncr.getCode());
        }
        // 升级为召回（终态，仅状态迁移占位；不建召回实体。真正建召回用 upgradeToRecall）
        ncr.setStatus(ncrStateMachine.upgradeToRecallTargetStatus());
        updateEntity(ncr, null, context);
        return ncr;
    }

    @Override
    @BizMutation
    public ErpQaRecall upgradeToRecall(@Name("ncrId") String ncrId, IServiceContext context) {
        return upgradeToRecallProcessor.upgradeToRecall(ncrId, context);
    }

    @Override
    @BizMutation
    public ErpQaNonConformance cancel(@Name("ncrId") String ncrId, IServiceContext context) {
        ErpQaNonConformance ncr = requireNcr(ncrId, context);
        String current = ncr.getStatus();
        try {
            ncrStateMachine.assertCanCancel(current);
        } catch (NopException e) {
            throw e.param(ErpQaErrors.ARG_NCR_CODE, ncr.getCode());
        }
        ncr.setStatus(ncrStateMachine.cancelTargetStatus());
        updateEntity(ncr, null, context);
        return ncr;
    }

    // ---------- helpers ----------

    private ErpQaNonConformance requireNcr(String ncrId, IServiceContext context) {
        if (ncrId == null) {
            throw new NopException(ErpQaErrors.ERR_NCR_NOT_FOUND).param(ErpQaErrors.ARG_NCR_ID, ncrId);
        }
        return requireEntity(ncrId, null, context);
    }
}
