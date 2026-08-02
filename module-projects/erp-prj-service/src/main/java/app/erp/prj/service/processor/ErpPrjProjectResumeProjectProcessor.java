package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.util.Objects;

/**
 * ErpPrjProject resumeProject per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 ON_HOLD→OPEN 恢复编排（共享 transition helper，按 Option A 小幅复制）。下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPrjProjectResumeProjectProcessor {

    @Inject
    IDaoProvider daoProvider;

    public ErpPrjProject resumeProject(Long projectId, IServiceContext context) {
        return transition(projectId, ErpPrjConstants.PROJECT_STATUS_ON_HOLD,
                ErpPrjConstants.PROJECT_STATUS_OPEN, context);
    }

    private ErpPrjProject transition(Long projectId, String expected, String target, IServiceContext context) {
        ErpPrjProject project = requireProject(projectId);
        String status = project.getStatus();
        if (!Objects.equals(status, expected)) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_NOT_CLOSABLE)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId)
                    .param(ErpPrjErrors.ARG_CURRENT_STATUS, status);
        }
        project.setStatus(target);
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
