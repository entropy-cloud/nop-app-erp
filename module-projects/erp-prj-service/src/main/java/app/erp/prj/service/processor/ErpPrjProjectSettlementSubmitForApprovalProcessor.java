package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
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

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3）：非法迁移错误由 common 码改为直抛领域码
     * ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION（参数形态与 facade illegalTransition helper 一致）。
     */
    @Override
    protected NopException illegalStatusException(ErpPrjProjectSettlement entity, String current, String... expected) {
        return new NopException(ErpPrjErrors.ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpPrjErrors.ARG_SETTLEMENT_CODE, entity.getCode())
                .param(ErpPrjErrors.ARG_CURRENT_STATUS, current)
                .param(ErpPrjErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    public ErpPrjProjectSettlementSubmitForApprovalProcessor() {
        super("ErpPrjProjectSettlement");
    }

    @Override
    public ErpPrjProjectSettlement submitForApproval(String id, IServiceContext context) {
        ErpPrjProjectSettlement settlement = processor.requireSettlement(id);
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
