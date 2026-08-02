package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.common.service.AbstractSubmitForApprovalProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPrjProjectSettlement submitForApproval per-mutation Processor (plan 2026-07-30-2046-1 R5.7, Pattern B)。
 * 自包含编排：requireSettlement → validateTransitionForSubmit → doSubmit(SUBMITTED) → save。
 * Long 签名边界：custom override 内 Long.valueOf(id) 转换；域逻辑经 facade
 * {@link ErpPrjProjectSettlementProcessor} protected helper（单一真相源）。
 * 运行时经 BizModel→facade 旧路径，R5.8 重配线（BizModel 改经 per-mutation）后激活本路径。
 */
public class ErpPrjProjectSettlementSubmitForApprovalProcessor extends AbstractSubmitForApprovalProcessor<ErpPrjProjectSettlement> {

    @Inject
    ErpPrjProjectSettlementProcessor processor;

    public ErpPrjProjectSettlementSubmitForApprovalProcessor() {
        super("ErpPrjProjectSettlement");
    }

    @Override
    public ErpPrjProjectSettlement submitForApproval(String id, IServiceContext context) {
        Long longId = Long.valueOf(id);
        ErpPrjProjectSettlement settlement = processor.requireSettlement(longId);
        processor.validateTransitionForSubmit(settlement);
        processor.doSubmit(settlement, context);
        processor.save(settlement);
        return settlement;
    }

    @Override
    protected IEntityDao<ErpPrjProjectSettlement> dao() {
        return daoProvider.daoFor(ErpPrjProjectSettlement.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpPrjProjectSettlement entity) {
        return null;
    }

    @Override
    protected void setApproveStatus(ErpPrjProjectSettlement entity, String status) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected boolean isCancelled(ErpPrjProjectSettlement entity) {
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
