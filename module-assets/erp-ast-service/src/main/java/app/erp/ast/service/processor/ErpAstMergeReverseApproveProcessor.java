package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstMerge;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstMerge reverseApprove per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * 合并执行后不可撤销（owner doc split-merge.md §关键业务规则 5 不可逆契约）。require 后直接抛错。
 */
public class ErpAstMergeReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpAstMerge> {

    @Inject
    ErpAstMergeProcessor processor;

    @Override
    public ErpAstMerge reverseApprove(String id, IServiceContext context) {
        ErpAstMerge merge = processor.requireMerge(id, context);
        throw new NopException(ErpAstErrors.ERR_AST_MERGE_REVERSE_NOT_SUPPORTED)
                .param(ErpAstErrors.ARG_MERGE_CODE, merge.getCode());
    }

    @Override
    protected IEntityDao<ErpAstMerge> dao() {
        return daoProvider.daoFor(ErpAstMerge.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
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
    protected String approvedStatus() {
        return ErpAstConstants.APPROVE_STATUS_APPROVED;
    }

    @Override
    protected String submittedStatus() {
        return ErpAstConstants.APPROVE_STATUS_SUBMITTED;
    }
}
