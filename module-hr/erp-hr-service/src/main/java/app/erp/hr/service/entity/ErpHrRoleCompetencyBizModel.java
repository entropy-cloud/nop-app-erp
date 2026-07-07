package app.erp.hr.service.entity;

import app.erp.hr.biz.IErpHrRoleCompetencyBiz;
import app.erp.hr.dao.entity.ErpHrRoleCompetency;
import app.erp.hr.service.ErpHrConstants;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;

/**
 * 岗位胜任力要求 BizModel（competency-management.md §RoleCompetency）。CRUD 之上扩展
 * requiredLevel 范围校验（1-5）：经 {@link #defaultPrepareSave}/{@link #defaultPrepareUpdate} 钩子
 * 检测，超范围抛 {@link ErpHrErrors#ERR_ROLE_COMPETENCY_INVALID_LEVEL}。
 */
@BizModel("ErpHrRoleCompetency")
public class ErpHrRoleCompetencyBizModel extends CrudBizModel<ErpHrRoleCompetency>
        implements IErpHrRoleCompetencyBiz {

    public ErpHrRoleCompetencyBizModel() {
        setEntityName(ErpHrRoleCompetency.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrRoleCompetency> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        validateRequiredLevel(entityData.getEntity());
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpHrRoleCompetency> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        validateRequiredLevel(entityData.getEntity());
    }

    void validateRequiredLevel(ErpHrRoleCompetency entity) {
        if (entity == null || entity.getRequiredLevel() == null) return;
        int level = entity.getRequiredLevel();
        if (level < ErpHrConstants.COMPETENCY_LEVEL_MIN
                || level > ErpHrConstants.COMPETENCY_LEVEL_MAX) {
            throw new NopException(ErpHrErrors.ERR_ROLE_COMPETENCY_INVALID_LEVEL)
                    .param(ErpHrErrors.ARG_REQUIRED_LEVEL, level);
        }
    }
}
