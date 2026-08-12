package app.erp.drp.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.drp.service.ErpDrpConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2.14 Delta 覆盖运行时实证 —— **Delta 加载**侧（plan Phase 3；契约 §6 业务级 Delta 实证义务，复用 M1.2 范式）。
 *
 * <p>在真实 IoC 容器（drp-service 测试容器，经 VFS Delta 层 {@code test-drp-delta} 以同名 bean id 覆盖基线为
 * {@link ErpDrpPlanStateMachineDelta}）下证明：
 * <ol>
 *   <li>容器解析的 Bean 为 Delta 派生类（经真实 bean 解析注入生效，非静态检查/非编译派生类）；</li>
 *   <li>Delta 覆盖的语义按 Delta 生效：{@code assertCanResetToDraft(APPROVED)} 抛异常（基线放行）；</li>
 *   <li>非覆盖动作继承基线不变：{@code assertCanResetToDraft(COMPUTED)} 仍放行、runDrp/approvePlan/advanceToExecuted
 *       矩阵不变、isTerminal 不变。</li>
 * </ol>
 *
 * <p>与 {@link TestErpDrpPlanStateMachineBaselineIoC} 对照：同一 {@code assertCanResetToDraft(APPROVED)}
 * 在基线放行、在 Delta 抛异常 —— 构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 *
 * <p>Delta 层经 {@code @NopTestProperty} 设置 {@code nop.core.vfs.delta-layer-ids=test-drp-delta} 激活，
 * Delta 层文件 {@code _vfs/_delta/test-drp-delta/erp/drp/beans/app-service.beans.xml} 以同名 bean id 覆盖基线。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.core.vfs.delta-layer-ids", value = "test-drp-delta")
public class TestErpDrpPlanStateMachineDeltaOverride extends JunitAutoTestCase {

    @Inject
    ErpDrpPlanStateMachine stateMachine;

    @Test
    public void testDeltaBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpDrpPlanStateMachineDelta,
                "Delta 加载：容器应解析为 ErpDrpPlanStateMachineDelta 派生类（同名 bean id 覆盖生效）");
        assertEquals(ErpDrpPlanStateMachineDelta.class, stateMachine.getClass(),
                "Delta 加载：实例类 = ErpDrpPlanStateMachineDelta（非基线类）");
    }

    @Test
    public void testDeltaOverriddenEdgeDiffersFromBaseline() {
        // Delta 覆盖语义：resetToDraft(APPROVED) 抛异常（基线放行 → Delta 收紧，可区分差异）
        NopException ex = assertThrows(NopException.class,
                () -> stateMachine.assertCanResetToDraft(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Delta resetToDraft(APPROVED) 报告 common 层非法迁移码");

        // Delta 仍允许 COMPUTED（基线 + Delta 均允许）
        stateMachine.assertCanResetToDraft(ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED);
    }

    @Test
    public void testNonOverriddenActionsInheritBaseline() {
        // Delta 未覆盖的动作继承基线矩阵不变
        stateMachine.assertCanRunDrp(ErpDrpConstants.DRP_PLAN_STATUS_DRAFT); // 基线 + Delta 均放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanRunDrp(ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED),
                "Delta 未覆盖 runDrp：runDrp(COMPUTED) 仍非法（基线继承）");
        stateMachine.assertCanApprovePlan(ErpDrpConstants.DRP_PLAN_STATUS_COMPUTED); // 基线 + Delta 均放行
        stateMachine.assertCanAdvanceToExecuted(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED); // 基线 + Delta 均放行
        // 终态分类不变（Delta 未覆盖 isTerminal）
        assertTrue(stateMachine.isTerminal(ErpDrpConstants.DRP_PLAN_STATUS_EXECUTED),
                "Delta 未覆盖 isTerminal：EXECUTED 仍为终态");
        assertTrue(!stateMachine.isTerminal(ErpDrpConstants.DRP_PLAN_STATUS_APPROVED),
                "Delta 未覆盖 isTerminal：APPROVED 仍非终态（D-DRP-1）");
    }
}
