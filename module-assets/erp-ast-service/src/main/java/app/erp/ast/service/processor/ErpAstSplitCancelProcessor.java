package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstSplit;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.common.service.AbstractCancelProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpAstSplit）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpAstSplit cancel per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → validateTransitionForCancel → set CANCELLED → save.
 * Domain logic via facade protected helpers (single source of truth).
 * Dormant until R5.8 rewire（BizModel Java 直调 facade.cancel，不经 xbiz 委托链）。
 */
public class ErpAstSplitCancelProcessor extends AbstractCancelProcessor<ErpAstSplit> {

    @Inject
    ErpAstSplitProcessor processor;

    @Override
    public ErpAstSplit cancel(String id, IServiceContext context) {
        ErpAstSplit split = processor.requireSplit(id, context);
        processor.validateTransitionForCancel(split, context);
        split.setDocStatus(ErpAstConstants.DOC_STATUS_CANCELLED);
        processor.splitDao().updateEntity(split);
        return split;
    }

    @Override
    protected IEntityDao<ErpAstSplit> dao() {
        return daoProvider.daoFor(ErpAstSplit.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3）：非法迁移错误由 common 码改为直抛领域码
     * ERR_AST_SPLIT_ILLEGAL_STATUS_TRANSITION（参数形态与 facade 组装一致）。
     */
    @Override
    protected NopException illegalStatusException(ErpAstSplit entity, String current, String... expected) {
        return new NopException(ErpAstErrors.ERR_AST_SPLIT_ILLEGAL_STATUS_TRANSITION)
                .param(ErpAstErrors.ARG_SPLIT_CODE, entity.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getDocStatus(ErpAstSplit entity) {
        return entity.getDocStatus();
    }

    @Override
    protected void setDocStatus(ErpAstSplit entity, String status) {
        entity.setDocStatus(status);
    }

    @Override
    protected String cancelledDocStatus() {
        return ErpAstConstants.DOC_STATUS_CANCELLED;
    }
}
