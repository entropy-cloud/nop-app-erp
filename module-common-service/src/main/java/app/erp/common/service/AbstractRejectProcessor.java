package app.erp.common.service;

import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.orm.support.OrmEntity;

import java.util.Objects;

/**
 * 审批驳回（reject）编排骨架（plan 2026-07-24-2200-1 Phase 1）。
 *
 * <p>编排骨架：{@code requireEntity → validateNotCancelled → validateTransitionForReject
 * → beforeStateChange → doReject → afterStateChange → save}。
 */
public abstract class AbstractRejectProcessor<T extends OrmEntity> extends AbstractProcessor<T> {

    public T reject(String id, IServiceContext context) {
        T entity = requireEntity(id);
        if (isRejected(entity)) {
            return entity;
        }
        validateNotCancelled(entity, context);
        validateTransitionForReject(entity, context);
        beforeStateChange(entity, context);
        doReject(entity, context);
        afterStateChange(entity, context);
        dao().updateEntity(entity);
        return entity;
    }

    protected void validateTransitionForReject(T entity, IServiceContext context) {
        String status = getApproveStatus(entity);
        if (!Objects.equals(status, submittedStatus())) {
            throw illegalStatusException(entity, status, submittedStatus());
        }
    }

    protected void doReject(T entity, IServiceContext context) {
        setApproveStatus(entity, rejectedStatus());
        setApprovedBy(entity, currentUserId());
        setApprovedAt(entity, CoreMetrics.currentTimestamp());
    }

    protected void validateNotCancelled(T entity, IServiceContext context) {
        if (isCancelled(entity)) {
            throw illegalStatusException(entity, "CANCELLED", "非已作废");
        }
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

    protected abstract boolean isCancelled(T entity);

    protected abstract String submittedStatus();

    protected abstract String rejectedStatus();
}
