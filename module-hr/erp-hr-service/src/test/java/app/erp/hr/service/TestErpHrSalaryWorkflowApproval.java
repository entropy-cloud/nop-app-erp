package app.erp.hr.service;

import app.erp.hr.dao.entity.ErpHrSalary;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.beans.ApiRequest;
import io.nop.api.core.beans.ApiResponse;
import io.nop.api.core.context.ContextProvider;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.graphql.core.IGraphQLExecutionContext;
import io.nop.graphql.core.engine.IGraphQLEngine;
import io.nop.orm.IOrmTemplate;
import io.nop.wf.core.IWorkflow;
import io.nop.wf.core.IWorkflowManager;
import io.nop.wf.core.IWorkflowStep;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static io.nop.graphql.core.ast.GraphQLOperationType.mutation;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * HR 薪酬 WORKFLOW 三级审批端到端测试（payroll.md §6）。
 * 验证 submitForApproval 启动 wf → 多级 agree → wf 结束回调 approve action → APPROVED，
 * 以及某级 reject → wf 结束回调 reject action → REJECTED，REJECTED 后可重提（5 态）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrSalaryWorkflowApproval extends JunitAutoTestCase {

    static final Long EMPLOYEE_ID = 8001L;

    @Inject
    IDaoProvider daoProvider;
    @Inject
    IOrmTemplate ormTemplate;
    @Inject
    IGraphQLEngine graphQLEngine;
    @Inject
    IWorkflowManager workflowManager;

    @Test
    public void testSubmitAgreeAllThenApproved() {
        setUser();
        ErpHrSalary salary = salaryOf();
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpHrSalary.class).saveEntity(salary));

        assertEquals(0, submit(salary.getId()).getStatus(), "submitForApproval 应成功");
        assertEquals(ErpHrConstants.APPROVE_STATUS_SUBMITTED, reload(salary).getApproveStatus(),
                "提交后 approveStatus=SUBMITTED");
        assertNotNull(reload(salary).getNopFlowId(), "WORKFLOW 模式应启动 wf 实例并回写 nopFlowId");

        ormTemplate.runInSession(() -> {
            IServiceContext ctx = newContext();
            IWorkflow wf = workflowManager.getWorkflow(reload(salary).getNopFlowId());
            invokeStep(wf, "hr-review", "agree", ctx);
            invokeStep(wf, "finance-review", "agree", ctx);
            invokeStep(wf, "manager-approval", "agree", ctx);
        });

        ErpHrSalary reloaded = reload(salary);
        assertEquals(ErpHrConstants.APPROVE_STATUS_APPROVED, reloaded.getApproveStatus(),
                "三级 agree 后 wf 结束回调 approve → APPROVED");
        assertNotNull(reloaded.getApprovedBy(), "approvedBy 应由 approve action 写入");
        assertNotNull(reloaded.getApprovedAt(), "approvedAt 应由 approve action 写入");
    }

    @Test
    public void testRejectAtFinanceReviewThenRejected() {
        setUser();
        ErpHrSalary salary = salaryOf();
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpHrSalary.class).saveEntity(salary));
        assertEquals(0, submit(salary.getId()).getStatus());

        ormTemplate.runInSession(() -> {
            IServiceContext ctx = newContext();
            IWorkflow wf = workflowManager.getWorkflow(reload(salary).getNopFlowId());
            invokeStep(wf, "hr-review", "agree", ctx);
            invokeStep(wf, "finance-review", "disagree", ctx);
        });

        assertEquals(ErpHrConstants.APPROVE_STATUS_REJECTED, reload(salary).getApproveStatus(),
                "finance-review reject → wf 结束回调 reject → REJECTED");
    }

    @Test
    public void testResubmitAfterRejected() {
        setUser();
        ErpHrSalary salary = salaryOf();
        ormTemplate.runInSession(() -> daoProvider.daoFor(ErpHrSalary.class).saveEntity(salary));
        submit(salary.getId());

        ormTemplate.runInSession(() -> {
            IServiceContext ctx = newContext();
            IWorkflow wf = workflowManager.getWorkflow(reload(salary).getNopFlowId());
            invokeStep(wf, "hr-review", "agree", ctx);
            invokeStep(wf, "finance-review", "disagree", ctx);
        });
        assertEquals(ErpHrConstants.APPROVE_STATUS_REJECTED, reload(salary).getApproveStatus());

        // REJECTED 后可重提（5 态支持：UNSUBMITTED/null/REJECTED 均可 submit）
        assertEquals(0, submit(salary.getId()).getStatus(), "REJECTED 后 submitForApproval 应可重提");
        ErpHrSalary reloaded = reload(salary);
        assertEquals(ErpHrConstants.APPROVE_STATUS_SUBMITTED, reloaded.getApproveStatus(),
                "重提后 approveStatus=SUBMITTED");
        assertNotNull(reloaded.getNopFlowId(), "重提应启动新的 wf 实例");
    }

    // ---------- helpers ----------

    private IServiceContext newContext() {
        IServiceContext ctx = new ServiceContextImpl();
        ctx.getContext().setUserId("0");
        ctx.getContext().setUserName("SYS");
        return ctx;
    }

    private void setUser() {
        // 用 SYS 用户（id=0）：submit 步骤 owner 解析为 SYS，caller=0 匹配 owner 跳过委托校验，
        // 避免对 NopAuthUser 的 requireEntityById 查询（bootstrap 测试库无种子用户）。
        ContextProvider.getOrCreateContext().setUserId("0");
        ContextProvider.getOrCreateContext().setUserName("SYS");
    }

    private void invokeStep(IWorkflow wf, String stepName, String action, IServiceContext ctx) {
        List<? extends IWorkflowStep> steps = wf.getActivatedSteps();
        for (IWorkflowStep step : steps) {
            if (step.getStepName().equals(stepName)) {
                step.invokeAction(action, null, ctx);
                wf.runAutoTransitions(ctx);
                return;
            }
        }
        throw new IllegalStateException("步骤未激活，无法执行 " + action + ": " + stepName
                + "，当前激活步骤=" + steps);
    }

    private ApiResponse<?> submit(Long id) {
        return executeRpc(mutation, "ErpHrSalary__submitForApproval",
                ApiRequest.build(Map.of("id", String.valueOf(id))));
    }

    private ApiResponse<?> executeRpc(io.nop.graphql.core.ast.GraphQLOperationType opType,
                                      String action, ApiRequest<?> request) {
        IGraphQLExecutionContext ctx = graphQLEngine.newRpcContext(opType, action, request);
        return graphQLEngine.executeRpc(ctx);
    }

    private ErpHrSalary reload(ErpHrSalary salary) {
        return daoProvider.daoFor(ErpHrSalary.class).getEntityById(salary.getId());
    }

    private ErpHrSalary salaryOf() {
        ErpHrSalary salary = new ErpHrSalary();
        salary.setEmployeeId(EMPLOYEE_ID);
        salary.setYear(2026);
        salary.setMonth(7);
        salary.setBasicSalary(new BigDecimal("10000"));
        salary.setGrossSalary(new BigDecimal("10000"));
        salary.setNetSalary(new BigDecimal("9000"));
        salary.setPaymentStatus(ErpHrConstants.PAYMENT_PENDING);
        salary.setApproveStatus(ErpHrConstants.APPROVE_STATUS_UNSUBMITTED);
        return salary;
    }
}
