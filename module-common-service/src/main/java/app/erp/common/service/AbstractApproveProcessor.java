package app.erp.common.service;

import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.orm.support.OrmEntity;

import java.util.Objects;

/**
 * 审批通过（approve）编排骨架（plan 2026-07-24-2200-1 Phase 1）。
 *
 * <p>编排骨架：{@code requireEntity → validateNotCancelled → validateTransitionForApprove
 * → validateBusinessRules → beforeStateChange → doApprove → afterStateChange → save}。
 *
 * <p>子类仅需实现实体特有步骤（状态读取/写入、域特有校验）。各步骤均为 {@code protected}，
 * 下游可通过 Delta bean 同名 id 覆盖单步。
 */
public abstract class AbstractApproveProcessor<T extends OrmEntity> extends AbstractProcessor<T> {

    public T approve(String id, IServiceContext context) {
        T entity = requireEntity(id);
        if (isApproved(entity)) {
            return entity;
        }
        validateNotCancelled(entity, context);
        validateTransitionForApprove(entity, context);
        validateBusinessRules(entity, context);
        beforeStateChange(entity, context);
        doApprove(entity, context);
        afterStateChange(entity, context);
        dao().updateEntity(entity);
        return entity;
    }

    protected void validateTransitionForApprove(T entity, IServiceContext context) {
        String status = getApproveStatus(entity);
        if (!Objects.equals(status, submittedStatus())) {
            throw illegalStatusException(entity, status, submittedStatus());
        }
    }

    protected void doApprove(T entity, IServiceContext context) {
        setApproveStatus(entity, approvedStatus());
        setApprovedBy(entity, currentUserId());
        setApprovedAt(entity, CoreMetrics.currentTimestamp());
    }

    protected void validateNotCancelled(T entity, IServiceContext context) {
        if (isCancelled(entity)) {
            throw illegalStatusException(entity, "CANCELLED", "非已作废");
        }
    }

    protected void validateBusinessRules(T entity, IServiceContext context) {
    }

    protected void beforeStateChange(T entity, IServiceContext context) {
    }

    protected void afterStateChange(T entity, IServiceContext context) {
    }

    protected abstract String getApproveStatus(T entity);

    protected abstract void setApproveStatus(T entity, String status);

    protected abstract void setApprovedBy(T entity, String userId);

    protected abstract void setApprovedAt(T entity, java.sql.Timestamp ts);

    protected abstract boolean isApproved(T entity);

    protected abstract boolean isCancelled(T entity);

    protected abstract String submittedStatus();

    protected abstract String approvedStatus();
}
