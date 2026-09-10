package app.erp.common.service;

import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import io.nop.orm.IOrmEntity;

/**
 * F1.3（ai-check P1-CK-inv-003）+ M2.8 common-011-r3：不可变台账实体基类——拒绝全部通用
 * {@code __save}/{@code __update}/{@code __delete}
 * （库存流水/余额等「台账不可变、余额由流水驱动」实体；合法写路径只应经域内专用编排 Processor）。
 * save 通道（create/upsert）全拒封堵 373 生成页面暴露的最后盲区。
 */
public abstract class AbstractErpImmutableCrudBizModel<T extends IOrmEntity> extends AbstractErpCrudBizModel<T> {

    @Override
    protected void defaultPrepareSave(EntityData<T> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        throw immutable(entityData.getEntity(), "save");
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<T> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        throw immutable(entityData.getEntity(), "update");
    }

    @Override
    protected void defaultPrepareDelete(T entity, IServiceContext context) {
        super.defaultPrepareDelete(entity, context);
        throw immutable(entity, "delete");
    }

    protected NopException immutable(T entity, String action) {
        Object id = entity == null ? null : entity.orm_propValueByName("id");
        return new NopException(ErpCommonErrors.ERR_CRUD_IMMUTABLE_ENTITY)
                .param(ErpCommonErrors.ARG_ACTION, action)
                .param(ErpCommonErrors.ARG_ENTITY_NAME, entity == null ? null : entity.orm_entityName())
                .param(ErpCommonErrors.ARG_ENTITY_KEY, id == null ? null : String.valueOf(id));
    }
}
