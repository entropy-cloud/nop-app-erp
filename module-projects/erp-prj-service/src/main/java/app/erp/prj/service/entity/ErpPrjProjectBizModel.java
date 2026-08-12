package app.erp.prj.service.entity;

import app.erp.prj.biz.IErpPrjProjectBiz;
import app.erp.prj.dao.entity.ErpPrjProject;
import app.erp.prj.service.ErpPrjConfigs;
import app.erp.prj.service.ErpPrjConstants;
import app.erp.prj.service.ErpPrjErrors;
import app.erp.prj.service.processor.ErpPrjProjectCloseProjectProcessor;
import app.erp.prj.service.processor.ErpPrjProjectHoldProjectProcessor;
import app.erp.prj.service.processor.ErpPrjProjectRefreshActualCostProcessor;
import app.erp.prj.service.processor.ErpPrjProjectResumeProjectProcessor;
import app.erp.prj.service.statemachine.ErpPrjProjectStateMachine;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 项目 BizModel。CRUD 之上承载项目状态引用校验（{@code cost-collection.md §七}）与
 * 成本归集回写（{@code §4.2}）。
 *
 * <p>{@code closeProject} 实现 OPEN→COMPLETED 冻结（对齐 §4.3「项目关闭」）；关闭前经
 * {@code validateTasksFinished} 校验任务已结束（config-gated STRICT/WARN，对齐
 * {@code state-machine.md §迁移完整性 OPEN→COMPLETED}）；关闭后
 * {@link #requireReferenceable} 拒绝新单据引用，从而拒绝新归集。
 *
 * <p>{@code startProject} 实现 DRAFT→OPEN 迁移；迁移前经 {@code validateStartPreconditions}
 * 校验必填字段（项目名/起止日期/预算，config-gated STRICT/WARN，对齐
 * {@code state-machine.md §迁移完整性 DRAFT→OPEN}）。
 *
 * <p>R6.6：{@code closeProject}/{@code holdProject}/{@code refreshActualCost}/{@code resumeProject}
 * 已拆为独立 per-mutation Processor（{@code processor-extension-pattern.md}），本类仅作 facade 单行委托。
 */
@BizModel("ErpPrjProject")
public class ErpPrjProjectBizModel extends CrudBizModel<ErpPrjProject> implements IErpPrjProjectBiz {

    private static final Logger LOG = LoggerFactory.getLogger(ErpPrjProjectBizModel.class);

    @Inject
    ErpPrjProjectCloseProjectProcessor closeProjectProcessor;
    @Inject
    ErpPrjProjectHoldProjectProcessor holdProjectProcessor;
    @Inject
    ErpPrjProjectResumeProjectProcessor resumeProjectProcessor;
    @Inject
    ErpPrjProjectRefreshActualCostProcessor refreshActualCostProcessor;
    @Inject
    ErpPrjProjectStateMachine stateMachine;

    public ErpPrjProjectBizModel() {
        setEntityName(ErpPrjProject.class.getName());
    }

    @Override
    @BizMutation
    public ErpPrjProject requireReferenceable(@Name("projectId") Long projectId, IServiceContext context) {
        ErpPrjProject project = requireEntity(String.valueOf(projectId), null, context);
        String status = project.getStatus();
        if (status == null || !Objects.equals(status, ErpPrjConstants.PROJECT_STATUS_OPEN)) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_NOT_REFERENCEABLE)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId)
                    .param(ErpPrjErrors.ARG_CURRENT_STATUS, status);
        }
        return project;
    }

    @Override
    @BizMutation
    public BigDecimal refreshActualCost(@Name("projectId") Long projectId, IServiceContext context) {
        return refreshActualCostProcessor.refreshActualCost(projectId, context);
    }

    @Override
    @BizMutation
    public ErpPrjProject closeProject(@Name("projectId") Long projectId, IServiceContext context) {
        return closeProjectProcessor.closeProject(projectId, context);
    }

    @Override
    @BizMutation
    public ErpPrjProject startProject(@Name("projectId") Long projectId, IServiceContext context) {
        ErpPrjProject project = requireEntity(String.valueOf(projectId), null, context);
        // 动态守卫保留原位：立项前校验必填字段（config-gated STRICT/WARN，对齐 state-machine.md §迁移完整性 DRAFT→OPEN）
        validateStartPreconditions(project);
        String status = project.getStatus();
        assertCan("start", projectId, status);
        project.setStatus(stateMachine.startTargetStatus());
        updateEntity(project, null, context);
        return project;
    }

    @Override
    @BizMutation
    public ErpPrjProject holdProject(@Name("projectId") Long projectId, IServiceContext context) {
        return holdProjectProcessor.holdProject(projectId, context);
    }

    @Override
    @BizMutation
    public ErpPrjProject resumeProject(@Name("projectId") Long projectId, IServiceContext context) {
        return resumeProjectProcessor.resumeProject(projectId, context);
    }

    @Override
    @BizMutation
    public ErpPrjProject cancelProject(@Name("projectId") Long projectId, IServiceContext context) {
        ErpPrjProject project = requireEntity(String.valueOf(projectId), null, context);
        String status = project.getStatus();
        // 终态走领域码 ERR_PROJECT_NOT_CLOSABLE（保持既有外部错误码）；非终态经 Bean 矩阵守卫
        // （cancel 非终态 DRAFT/OPEN/ON_HOLD 均合法）。参照 M1.1 ErpCsTicketBizModel.cancel 防冲突范式（契约 §11.4）。
        if (stateMachine.isTerminal(status)) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_NOT_CLOSABLE)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId)
                    .param(ErpPrjErrors.ARG_CURRENT_STATUS, status);
        }
        stateMachine.assertCanCancel(status);
        project.setStatus(stateMachine.cancelTargetStatus());
        updateEntity(project, null, context);
        return project;
    }

    /**
     * 经 StateMachine Bean 断言来源态合法（start）；非法边（Bean 报告 common 层码）映射为领域
     * {@code ERR_PROJECT_NOT_CLOSABLE}（项目域 start/cancel/Hold/Resume/Close 共享此码）+ 项目编号/上下文，
     * common 码作 cause 保留（契约 §7）。cancel 的终态拒绝不经此 helper（终态优先走领域码路径，见 cancelProject）。
     */
    private void assertCan(String action, Long projectId, String from) {
        try {
            switch (action) {
                case "start":
                    stateMachine.assertCanStart(from);
                    break;
                default:
                    throw new IllegalArgumentException("unexpected action: " + action);
            }
        } catch (NopException e) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_NOT_CLOSABLE, e)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, projectId)
                    .param(ErpPrjErrors.ARG_CURRENT_STATUS, from);
        }
    }

    /**
     * 校验立项必填字段（项目名/起止日期/预算，且 startDate<=endDate）。STRICT 模式（默认）抛
     * {@code ERR_PROJECT_START_PRECONDITION_FAILED}；WARN 模式 {@code LOG.warn} 放行。
     */
    private void validateStartPreconditions(ErpPrjProject project) {
        List<String> missingFields = new ArrayList<>();
        if (project.getName() == null || project.getName().trim().isEmpty()) {
            missingFields.add("name");
        }
        LocalDate startDate = project.getStartDate();
        LocalDate endDate = project.getEndDate();
        if (startDate == null) {
            missingFields.add("startDate");
        }
        if (endDate == null) {
            missingFields.add("endDate");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            missingFields.add("startDate>endDate");
        }
        if (project.getBudget() == null) {
            missingFields.add("budget");
        }
        if (missingFields.isEmpty()) {
            return;
        }
        if (ErpPrjConfigs.strictProjectStartPrecheck()) {
            throw new NopException(ErpPrjErrors.ERR_PROJECT_START_PRECONDITION_FAILED)
                    .param(ErpPrjErrors.ARG_PROJECT_ID, project.getId())
                    .param(ErpPrjErrors.ARG_MISSING_FIELDS, missingFields);
        }
        LOG.warn("项目 {} 立项前置校验失败：缺少必填字段 {}（WARN 模式放行），state-machine.md §迁移完整性 DRAFT→OPEN",
                project.getId(), missingFields);
    }

}
