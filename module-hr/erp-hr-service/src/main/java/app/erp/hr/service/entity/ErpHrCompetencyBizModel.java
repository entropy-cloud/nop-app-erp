package app.erp.hr.service.entity;

import app.erp.hr.biz.IErpHrCompetencyBiz;
import app.erp.hr.dao.entity.ErpHrCompetency;
import app.erp.hr.service.ErpHrErrors;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.biz.crud.EntityData;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;

import java.util.HashSet;
import java.util.Set;

/**
 * 胜任力字典 BizModel（competency-management.md §Competency）。CRUD 之上扩展 parentId 自环/成环校验
 * （对齐 projects 0930-3 范式）：经 {@link #defaultPrepareSave}/{@link #defaultPrepareUpdate} 钩子
 * 检测自引用与祖先链回环，违例抛 {@link ErpHrErrors#ERR_COMPETENCY_PARENT_CYCLE}。
 */
@BizModel("ErpHrCompetency")
public class ErpHrCompetencyBizModel extends CrudBizModel<ErpHrCompetency>
        implements IErpHrCompetencyBiz {

    public ErpHrCompetencyBizModel() {
        setEntityName(ErpHrCompetency.class.getName());
    }

    @Override
    protected void defaultPrepareSave(EntityData<ErpHrCompetency> entityData, IServiceContext context) {
        super.defaultPrepareSave(entityData, context);
        validateParentCycle(entityData.getEntity(), context);
    }

    @Override
    protected void defaultPrepareUpdate(EntityData<ErpHrCompetency> entityData, IServiceContext context) {
        super.defaultPrepareUpdate(entityData, context);
        validateParentCycle(entityData.getEntity(), context);
    }

    /**
     * 校验 competency.parentId 不形成环路：自引用直接拒绝；沿祖先链上行检测回环。
     */
    void validateParentCycle(ErpHrCompetency entity, IServiceContext context) {
        if (entity == null || entity.getParentId() == null) return;
        Long selfId = entity.getId();
        Long parentId = entity.getParentId();
        if (selfId != null && selfId.equals(parentId)) {
            throw cycleError(selfId, parentId);
        }
        Set<Long> visited = new HashSet<>();
        visited.add(selfId != null ? selfId : Long.MIN_VALUE);
        Long cursor = parentId;
        int guard = 0;
        IEntityDao<ErpHrCompetency> dao = dao();
        while (cursor != null && guard < 1000) {
            if (!visited.add(cursor)) {
                throw cycleError(selfId, parentId);
            }
            ErpHrCompetency parent = dao.getEntityById(cursor);
            if (parent == null) break;
            if (selfId != null && selfId.equals(parent.getId())) {
                throw cycleError(selfId, parentId);
            }
            cursor = parent.getParentId();
            guard++;
        }
    }

    private NopException cycleError(Long competencyId, Long parentId) {
        return new NopException(ErpHrErrors.ERR_COMPETENCY_PARENT_CYCLE)
                .param(ErpHrErrors.ARG_COMPETENCY_ID, competencyId)
                .param("parentId", parentId);
    }
}
