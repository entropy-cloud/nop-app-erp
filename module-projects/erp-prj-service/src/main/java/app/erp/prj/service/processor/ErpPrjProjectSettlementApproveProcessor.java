package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpPrjProjectSettlement）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpPrjProjectSettlement approve per-mutation Processor (plan 2026-07-30-2046-1 R5.7, Pattern B)。
 * 自包含编排（会计保护区域）：requireSettlement → validateTransitionForApprove
 * → [若 CLOSE+transferToAsset+assetCardId==null: createAndActivateAsset（转固）]
 * → doPost（postingDispatcher 过账） → doApprove(APPROVED+docStatus=APPROVED+approvedBy/At) → save。
 * 转固+过账经 facade protected helper（单一真相源），per-mutation 不复制会计规则。
 * 运行时经 BizModel→facade 旧路径，R5.8 重配线后激活本路径。
 */
public class ErpPrjProjectSettlementApproveProcessor extends AbstractApproveProcessor<ErpPrjProjectSettlement> {

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

    @Override
    public ErpPrjProjectSettlement approve(String id, IServiceContext context) {
        ErpPrjProjectSettlement settlement = processor.requireSettlement(id);
        processor.validateTransitionForApprove(settlement);
        if (ErpPrjConstants.SETTLEMENT_TYPE_CLOSE.equals(settlement.getSettlementType())
                && Boolean.TRUE.equals(settlement.getTransferToAsset()) && settlement.getAssetCardId() == null) {
            processor.createAndActivateAsset(settlement, context);
        }
        processor.doPost(settlement, context);
        processor.doApprove(settlement, context);
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
    protected void setApprovedBy(ErpPrjProjectSettlement entity, String userId) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected void setApprovedAt(ErpPrjProjectSettlement entity, java.sql.Timestamp ts) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected boolean isApproved(ErpPrjProjectSettlement entity) {
        return false;
    }

    @Override
    protected boolean isCancelled(ErpPrjProjectSettlement entity) {
        return false;
    }

    @Override
    protected String submittedStatus() {
        return null;
    }

    @Override
    protected String approvedStatus() {
        return null;
    }
}
