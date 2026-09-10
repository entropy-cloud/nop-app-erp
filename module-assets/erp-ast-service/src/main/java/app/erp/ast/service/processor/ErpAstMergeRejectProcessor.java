package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstMerge;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.common.service.AbstractRejectProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpAstMerge）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpAstMerge reject per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * Self-contained orchestration: require → validateNotCancelled → validateTransition → set REJECTED → save.
 * Domain logic via facade protected helpers (single source of truth).
 */
public class ErpAstMergeRejectProcessor extends AbstractRejectProcessor<ErpAstMerge> {

    @Inject
    ErpAstMergeProcessor processor;

    @Override
    public ErpAstMerge reject(String id, IServiceContext context) {
        ErpAstMerge merge = processor.requireMerge(id, context);
        processor.validateNotCancelled(merge, context);
        processor.validateTransitionForReject(merge, context);
        merge.setApproveStatus(ErpAstConstants.APPROVE_STATUS_REJECTED);
        processor.mergeDao().updateEntity(merge);
        return merge;
    }

    @Override
    protected IEntityDao<ErpAstMerge> dao() {
        return daoProvider.daoFor(ErpAstMerge.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    /**
     * 骨架守卫裸奔通道修正（plan 2026-09-07-2200-1 form-3）：非法迁移错误由 common 码改为直抛领域码
     * ERR_AST_MERGE_ILLEGAL_STATUS_TRANSITION（参数形态与 facade 组装一致）。
     */
    @Override
    protected NopException illegalStatusException(ErpAstMerge entity, String current, String... expected) {
        return new NopException(ErpAstErrors.ERR_AST_MERGE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpAstErrors.ARG_MERGE_CODE, entity.getCode())
                .param(ErpAstErrors.ARG_CURRENT_STATUS, current)
                .param(ErpAstErrors.ARG_EXPECTED_STATUS, String.join(" / ", expected));
    }

    @Override
    protected String getApproveStatus(ErpAstMerge entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpAstMerge entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpAstMerge entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpAstMerge entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpAstMerge entity) {
        return false;
    }

    @Override
    protected boolean isCancelled(ErpAstMerge entity) {
        return entity.isCancelled();
    }

    @Override
    protected String submittedStatus() {
        return ErpAstConstants.APPROVE_STATUS_SUBMITTED;
    }

    @Override
    protected String rejectedStatus() {
        return ErpAstConstants.APPROVE_STATUS_REJECTED;
    }
}
