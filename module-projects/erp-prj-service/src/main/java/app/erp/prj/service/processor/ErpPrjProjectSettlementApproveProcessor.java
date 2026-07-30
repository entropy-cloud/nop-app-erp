package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.common.service.AbstractApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPrjProjectSettlement approve per-mutation Processor (plan 2026-07-30-2046-1 R5.7, Pattern B)。
 * 自包含编排（会计保护区域）：requireSettlement → validateTransitionForApprove
 * → [若 CLOSE+transferToAsset+assetCardId==null: createAndActivateAsset（转固）]
 * → doPost（postingDispatcher 过账） → doApprove(APPROVED+docStatus=APPROVED+approvedBy/At) → save。
 * 转固+过账经 facade protected helper（单一真相源），per-mutation 不复制会计规则。
 * Long 签名边界：custom override 内 Long.valueOf(id) 转换。
 * 运行时经 BizModel→facade 旧路径，R5.8 重配线后激活本路径。
 */
public class ErpPrjProjectSettlementApproveProcessor extends AbstractApproveProcessor<ErpPrjProjectSettlement> {

    @Inject
    ErpPrjProjectSettlementProcessor processor;

    @Override
    public ErpPrjProjectSettlement approve(String id, IServiceContext context) {
        Long longId = Long.valueOf(id);
        ErpPrjProjectSettlement settlement = processor.requireSettlement(longId);
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
