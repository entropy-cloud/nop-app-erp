package app.erp.ast.service.processor;

import app.erp.ast.dao.entity.ErpAstSplit;
import app.erp.ast.service.ErpAstConstants;
import app.erp.ast.service.ErpAstErrors;
import app.erp.common.service.AbstractReverseApproveProcessor;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpAstSplit reverseApprove per-mutation Processor (plan 2026-07-25-1057-2, R5.4 Pattern B).
 * 拆分执行后不可撤销（owner doc split-merge.md §关键业务规则 5 不可逆契约）。require 后直接抛错。
 */
public class ErpAstSplitReverseApproveProcessor extends AbstractReverseApproveProcessor<ErpAstSplit> {

    @Inject
    ErpAstSplitProcessor processor;

    @Override
    public ErpAstSplit reverseApprove(String id, IServiceContext context) {
        ErpAstSplit split = processor.requireSplit(id, context);
        throw new NopException(ErpAstErrors.ERR_AST_SPLIT_REVERSE_NOT_SUPPORTED)
                .param(ErpAstErrors.ARG_SPLIT_CODE, split.getCode());
    }

    @Override
    protected IEntityDao<ErpAstSplit> dao() {
        return daoProvider.daoFor(ErpAstSplit.class);
    }

    @Override
    protected NopException notFoundException(String id) {
        return defaultNotFoundException(id);
    }

    @Override
    protected String getApproveStatus(ErpAstSplit entity) {
        return entity.getApproveStatus();
    }

    @Override
    protected void setApproveStatus(ErpAstSplit entity, String status) {
        entity.setApproveStatus(status);
    }

    @Override
    protected void setApprovedBy(ErpAstSplit entity, String userId) {
        entity.setApprovedBy(userId);
    }

    @Override
    protected void setApprovedAt(ErpAstSplit entity, java.sql.Timestamp ts) {
        entity.setApprovedAt(ts);
    }

    @Override
    protected boolean isRejected(ErpAstSplit entity) {
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
