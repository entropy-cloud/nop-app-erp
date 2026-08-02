package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjProjectSettlement;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPrjProjectSettlement cancel per-mutation Processor (plan 2026-07-30-2046-1 R5.7, Pattern B)。
 * 自包含编排（会计保护区域）：requireSettlement → validateTransitionForCancel
 * → [若 posted: postingDispatcher.reverse（冲销过账）+ rollbackAssetIfNeeded（回滚资产）+ 重载 + 清 posted/postedAt/postedBy]
 * → doCancel(docStatus=CANCELLED) → save。
 * 冲销+回滚经 facade protected helper + 包级 postingDispatcher 字段（单一真相源），per-mutation 不复制会计规则。
 * Long 签名边界：custom override 内 Long.valueOf(id) 转换。
 * 运行时经 BizModel→facade 旧路径，R5.8 重配线后激活本路径。
 */
public class ErpPrjProjectSettlementCancelProcessor extends AbstractCancelProcessor<ErpPrjProjectSettlement> {

    @Inject
    ErpPrjProjectSettlementProcessor processor;

    @Override
    public ErpPrjProjectSettlement cancel(String id, IServiceContext context) {
        Long longId = Long.valueOf(id);
        ErpPrjProjectSettlement settlement = processor.requireSettlement(longId);
        processor.validateTransitionForCancel(settlement);
        if (Boolean.TRUE.equals(settlement.getPosted())) {
            processor.postingDispatcher.reverse(settlement);
            processor.rollbackAssetIfNeeded(settlement);
            settlement = processor.requireSettlement(longId);
            settlement.setPosted(false);
            settlement.setPostedAt(null);
            settlement.setPostedBy(null);
        }
        processor.doCancel(settlement, context);
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
    protected String getDocStatus(ErpPrjProjectSettlement entity) {
        return null;
    }

    @Override
    protected void setDocStatus(ErpPrjProjectSettlement entity, String status) {
        // not reached: main method delegates to monolithic Processor
    }

    @Override
    protected String cancelledDocStatus() {
        return null;
    }
}
