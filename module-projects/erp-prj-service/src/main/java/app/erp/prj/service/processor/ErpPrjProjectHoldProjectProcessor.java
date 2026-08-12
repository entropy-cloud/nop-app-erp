package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.prj.service.statemachine.ErpPrjProjectStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

/**
 * ErpPrjProject holdProject per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 OPEN→ON_HOLD 暂停编排。固定来源态/目标态判断改调 {@link ErpPrjProjectStateMachine}（契约 §11.1 步骤 3）；
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPrjProjectHoldProjectProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ErpPrjProjectStateMachine stateMachine;

    public ErpPrjProject holdProject(Long projectId, IServiceContext context) {
        ErpPrjProject project = requireProject(projectId);
        String status = project.getStatus();
        try {
            stateMachine.assertCanHold(status);
        } catch (NopException e) {
            // 非法边（Bean 报告 common 层码）映射为领域 ERR_PROJECT_NOT_CLOSABLE + 项目上下文，common 码作 cause（契约 §7）
            throw new NopException(ErpPrjErrors.ERR_PROJECT_NOT_CLOSABLE, e)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId)
                    .param(ErpPrjErrors.ARG_CURRENT_STATUS, status);
        }
        project.setStatus(stateMachine.holdTargetStatus());
        projectDao().updateEntity(project);
        return project;
    }

    private ErpPrjProject requireProject(Long projectId) {
        ErpPrjProject project = projectDao().getEntityById(projectId);
        if (project == null) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_NOT_FOUND)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId);
        }
        return project;
    }

    private IEntityDao<ErpPrjProject> projectDao() {
        return daoProvider.daoFor(ErpPrjProject.class);
    }
}
