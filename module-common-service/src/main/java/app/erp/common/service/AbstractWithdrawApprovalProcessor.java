package app.erp.common.service;

import io.nop.core.context.IServiceContext;
import io.nop.orm.support.OrmEntity;

import java.util.Objects;

/**
 * 撤回审批（withdrawApproval）编排骨架（plan 2026-07-24-2200-1 Phase 1）。
 *
 * <p>编排骨架：{@code requireEntity → validateNotCancelled → validateTransitionForWithdraw
 * → beforeStateChange → doWithdraw → afterStateChange → save}。
 *
 * <p>目标状态：回到 UNSUBMITTED（草稿态）。
 */
public abstract class AbstractWithdrawApprovalProcessor<T extends OrmEntity> extends AbstractProcessor<T> {

    public T withdrawApproval(String id, IServiceContext context) {
        T entity = requireEntity(id);
        validateNotCancelled(entity, context);
        validateTransitionForWithdraw(entity, context);
        // P2-CK-sal-013：仅提交人可撤回（state-machine.md §2「撤销提交约束：仅提交人可操作」）。
        // 跨域共用骨架一点修（C8.2 全域盘点归后）；SoDGuard 同一 sod-enabled 总开关 + null 容忍。
        SoDGuard.assertWithdrawerIsCreator(getCreatedBy(entity), currentUserId(),
                ErpCommonErrors.ERR_WITHDRAW_NOT_SUBMITTER);
        beforeStateChange(entity, context);
        doWithdraw(entity, context);
        afterStateChange(entity, context);
        dao().updateEntity(entity);
        return entity;
    }

    protected void validateTransitionForWithdraw(T entity, IServiceContext context) {
        String status = getApproveStatus(entity);
        if (!Objects.equals(status, submittedStatus())) {
            throw illegalStatusException(entity, status, submittedStatus());
        }
    }

    protected void doWithdraw(T entity, IServiceContext context) {
        setApproveStatus(entity, unsubmittedStatus());
    }

    protected void validateNotCancelled(T entity, IServiceContext context) {
        if (isCancelled(entity)) {
            throw illegalStatusException(entity, "CANCELLED", "!CANCELLED");
        }
    }

    protected void beforeStateChange(T entity, IServiceContext context) {
    }

    protected void afterStateChange(T entity, IServiceContext context) {
    }

    protected abstract String getApproveStatus(T entity);

    protected abstract void setApproveStatus(T entity, String status);

    protected abstract boolean isCancelled(T entity);

    protected abstract String unsubmittedStatus();

    protected abstract String submittedStatus();
}
