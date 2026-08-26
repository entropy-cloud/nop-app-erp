package app.erp.common.service;

import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import io.nop.orm.IOrmEntity;

/**
 * F1.3（ai-check P1-CK-pur-003 族）：实体 BizModel 统一基类——通用 {@code __update}/{@code __delete}
 * 前置状态锁守卫（posted=true 或 approveStatus=APPROVED 拒绝）。
 *
 * <p>覆盖平台钩子（CrudBizModel defaultPrepareUpdate L981 / defaultPrepareDelete L1179），
 * 均先 super（delete 钩子基类含树形实体子记录检查，不可跳过）后守卫。内部状态机动作经
 * {@code updateEntity}/{@code deleteEntity} helper 直写不经 prepare 钩子，不受影响。
 * 无 posted/approveStatus 列的实体守卫惰性（零行为变化）。
 * config 总开关 {@code erp-common.crud-status-lock-enabled}（默认开，kill-switch）。
 *
 * <p>不可变台账实体（库存流水/余额）用 {@link AbstractErpImmutableCrudBizModel}。
 */
public abstract class AbstractErpCrudBizModel<T extends IOrmEntity> extends CrudBizModel<T> {

    public static final String CONFIG_STATUS_LOCK_ENABLED = "erp-common.crud-status-lock-enabled";

    @Override
    protected void defaultPrepareUpdate(EntityData<T> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        T entity = entityData.getEntity();
        if (isStatusLockEnabled() && ErpCrudStatusLock.shouldBlock(entity)) {
            throw locked(entity, "update");
        }
    }

    @Override
    protected void defaultPrepareDelete(T entity, IServiceContext context) {
        super.defaultPrepareDelete(entity, context);
        if (isStatusLockEnabled() && ErpCrudStatusLock.shouldBlock(entity)) {
            throw locked(entity, "delete");
        }
    }

    protected boolean isStatusLockEnabled() {
        return AppConfig.var(CONFIG_STATUS_LOCK_ENABLED, Boolean.TRUE);
    }

    protected NopException locked(T entity, String action) {
        Object id = entity.orm_propValueByName("id");
        return new NopException(ErpCommonErrors.ERR_CRUD_STATUS_LOCKED)
                .param(ErpCommonErrors.ARG_ACTION, action)
                .param(ErpCommonErrors.ARG_ENTITY_NAME, entity.orm_entityName())
                .param(ErpCommonErrors.ARG_ENTITY_KEY, id == null ? null : String.valueOf(id));
    }
}
