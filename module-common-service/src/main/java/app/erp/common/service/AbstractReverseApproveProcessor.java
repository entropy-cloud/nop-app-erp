package app.erp.common.service;

import io.nop.core.context.IServiceContext;
import io.nop.orm.support.OrmEntity;

import java.util.Objects;

/**
 * 反审核（reverseApprove）编排骨架（plan 2026-07-24-2200-1 Phase 1）。
 *
 * <p>编排骨架：{@code requireEntity → validateTransitionForReverseApprove
 * → beforeStateChange → doReverseApprove → afterStateChange → save}。
 *
 * <p>目标状态：回到 SUBMITTED，清空 approvedBy/approvedAt 审计字段。
 */
public abstract class AbstractReverseApproveProcessor<T extends OrmEntity> extends AbstractProcessor<T> {

    public T reverseApprove(String id, IServiceContext context) {
        T entity = requireEntity(id);
        if (isRejected(entity)) {
            return entity;
        }
        validateTransitionForReverseApprove(entity, context);
        beforeStateChange(entity, context);
        doReverseApprove(entity, context);
        afterStateChange(entity, context);
        dao().updateEntity(entity);
        return entity;
    }

    protected void validateTransitionForReverseApprove(T entity, IServiceContext context) {
        String status = getApproveStatus(entity);
        if (!Objects.equals(status, approvedStatus())) {
            throw illegalStatusException(entity, status, approvedStatus());
        }
    }

    protected void doReverseApprove(T entity, IServiceContext context) {
        setApproveStatus(entity, submittedStatus());
        setApprovedBy(entity, null);
        setApprovedAt(entity, null);
    }

    protected void beforeStateChange(T entity, IServiceContext context) {
    }

    protected void afterStateChange(T entity, IServiceContext context) {
    }

    protected abstract String getApproveStatus(T entity);

    protected abstract void setApproveStatus(T entity, String status);

    protected abstract void setApprovedBy(T entity, String userId);

    protected abstract void setApprovedAt(T entity, java.sql.Timestamp ts);

    protected abstract boolean isRejected(T entity);

    protected abstract String approvedStatus();

    protected abstract String submittedStatus();
}
