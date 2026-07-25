package app.erp.common.service;

import io.nop.core.context.IServiceContext;
import io.nop.orm.support.OrmEntity;

import java.util.Objects;

/**
 * 单据作废（cancel）编排骨架（plan 2026-07-24-2200-1 Phase 1）。
 *
 * <p>编排骨架：{@code requireEntity → validateCanCancel → validateTransitionForCancel
 * → beforeCancel → doCancel → afterCancel → save}。
 *
 * <p>cancel 与审批状态机正交：cancel 操作 docStatus（已作废标志），不改 approveStatus。
 * 子类按需在 {@code beforeCancel} 实现副作用清理（如承付释放、跨公司红冲）。
 */
public abstract class AbstractCancelProcessor<T extends OrmEntity> extends AbstractProcessor<T> {

    public T cancel(String id, IServiceContext context) {
        T entity = requireEntity(id);
        validateCanCancel(entity, context);
        validateTransitionForCancel(entity, context);
        beforeCancel(entity, context);
        doCancel(entity, context);
        afterCancel(entity, context);
        dao().updateEntity(entity);
        return entity;
    }

    protected void validateTransitionForCancel(T entity, IServiceContext context) {
        String docStatus = getDocStatus(entity);
        if (Objects.equals(docStatus, cancelledDocStatus())) {
            throw illegalStatusException(entity, docStatus, "非已作废");
        }
    }

    protected void validateCanCancel(T entity, IServiceContext context) {
    }

    protected void doCancel(T entity, IServiceContext context) {
        setDocStatus(entity, cancelledDocStatus());
    }

    protected void beforeCancel(T entity, IServiceContext context) {
    }

    protected void afterCancel(T entity, IServiceContext context) {
    }

    protected abstract String getDocStatus(T entity);

    protected abstract void setDocStatus(T entity, String status);

    protected abstract String cancelledDocStatus();
}
