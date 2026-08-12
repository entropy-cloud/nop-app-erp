package app.erp.drp.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.drp.service.ErpDrpConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2.14 Delta 覆盖运行时实证 —— **基线加载**侧（plan Phase 3；契约 §6 业务级 Delta 实证义务，复用 M1.2 范式）。
 *
 * <p>在真实 IoC 容器（drp-service 测试容器，加载生产 {@code app-service.beans.xml}，无 deltaLayer 激活）下证明：
 * 容器解析的 {@link ErpDrpPlanStateMachine} 为基线实例，其矩阵行为 = Phase 1 矩阵。
 *
 * <p>与 {@link TestErpDrpPlanStateMachineDeltaOverride} 对照：本测试断言
 * {@code assertCanResetToDraft(APPROVED)} **放行**（基线允许 COMPUTED/APPROVED），Delta 测试断言其**抛异常**（收紧为仅 COMPUTED）。
 * 两者构成可区分的基线/Delta 双加载证据。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpDrpPlanStateMachineBaselineIoC extends JunitAutoTestCase {

    @Inject
    ErpDrpPlanStateMachine stateMachine;

    @Test
    public void testBaselineBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpDrpPlanStateMachine,
                "基线加载：容器解析的应为基线 ErpDrpPlanStateMachine 实例");
        // 确认不是 Delta 派生类
        assertEquals(ErpDrpPlanStateMachine.class, stateMachine.getClass(),
                "基线加载：实例类 = ErpDrpPlanStateMachine（非 Delta 派生）");
    }

    @Test
    public void testBaselineResetToDraftAllowsMultiSource() {
        // 基线矩阵：resetToDraft 允许 COMPUTED/APPROVED（多源）
        stateMachine.assertCanResetToDraft(ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED); // 放行
        // 关键差异点：resetToDraft(APPROVED) 在基线放行（Delta 将收紧为非法）
        stateMachine.assertCanResetToDraft(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED); // 放行
    }

    @Test
    public void testBaselineResetToDraftRejectsTerminalAndInitial() {
        // EXECUTED 终态 + DRAFT 初始态 均非法
        NopException ex1 = assertThrows(NopException.class,
                () -> stateMachine.assertCanResetToDraft(ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex1.getErrorCode(),
                "基线 resetToDraft(EXECUTED) 报告 common 层非法迁移码");

        NopException ex2 = assertThrows(NopException.class,
                () -> stateMachine.assertCanResetToDraft(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex2.getErrorCode(),
                "基线 resetToDraft(DRAFT) 报告 common 层非法迁移码");
    }

    @Test
    public void testBaselineOtherActionsMatchMatrix() {
        // 基线非覆盖动作保持矩阵语义（Delta 不覆盖这些，应继承相同行为）
        stateMachine.assertCanRunDrp(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT); // 放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanRunDrp(ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED),
                "基线 runDrp(COMPUTED) 非法");
        stateMachine.assertCanApprovePlan(ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED); // 放行
        stateMachine.assertCanAdvanceToExecuted(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED); // 放行
        // D-DRP-1：APPROVED 非终态
        assertTrue(!stateMachine.isTerminal(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED),
                "基线 isTerminal(APPROVED)=false（D-DRP-1：APPROVED 非终态）");
        assertTrue(stateMachine.isTerminal(ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED),
                "基线 isTerminal(EXECUTED)=true");
    }
}
