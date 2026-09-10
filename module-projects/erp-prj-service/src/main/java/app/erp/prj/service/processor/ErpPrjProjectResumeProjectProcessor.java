package app.erp.prj.service.processor;

import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.prj.service.statemachine.ErpPrjProjectStateMachine;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

// 族 A/U20 豁免登记：本类为非 BizModel 服务组件（processor-extension-pattern 惯例）；daoFor 目标（ErpPrjProject）=同域实体批量聚合，只读批量聚合，逐条 I*Biz 管道不适用批量场景。
/**
 * ErpPrjProject resumeProject per-mutation Processor（R6.6，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含 ON_HOLD→OPEN 恢复编排。固定来源态/目标态判断改调 {@link ErpPrjProjectStateMachine}（契约 §11.1 步骤 3）；
 * 下游可经 Delta beans.xml 同名 bean id 覆盖本类。
 */
public class ErpPrjProjectResumeProjectProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ErpPrjProjectStateMachine stateMachine;

    public ErpPrjProject resumeProject(String projectId, IServiceContext context) {
        ErpPrjProject project = requireProject(projectId);
        String status = project.getStatus();
        try {
            stateMachine.assertCanResume(status);
        } catch (NopException e) {
            // 非法边 Bean 直抛领域码 ERR_PROJECT_NOT_CLOSABLE（plan 2026-09-07-2200-1 转码退役），本处同码补参 projectId
            throw e.param(ErpPrjErrors.ARG_PROJECT_ID, projectId);
        }
        project.setStatus(stateMachine.resumeTargetStatus());
        projectDao().updateEntity(project);
        return project;
    }

    private ErpPrjProject requireProject(String projectId) {
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
