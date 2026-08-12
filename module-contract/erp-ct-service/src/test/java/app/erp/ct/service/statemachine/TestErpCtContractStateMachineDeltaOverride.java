package app.erp.ct.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.ct.service.ErpCtConstants;
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
 * M2.18 Delta 覆盖运行时实证 —— **Delta 加载**侧（plan Phase 3）。
 *
 * <p>在真实 IoC 容器（ct-service 测试容器，经 VFS Delta 层 {@code test-ct-delta} 以同名 bean id 覆盖基线为
 * {@link ErpCtContractStateMachineDelta}）下证明：
 * <ol>
 *   <li>容器解析的 Bean 为 Delta 派生类（经真实 bean 解析注入生效，非静态检查/非编译派生类）；</li>
 *   <li>Delta 覆盖的语义按 Delta 生效：{@code assertCanTerminate(NEGOTIATION)} 抛异常（基线放行 → Delta 收紧）；</li>
 *   <li>非覆盖动作继承基线不变：{@code assertCanTerminate(ACTIVE)} 仍放行、{@code assertCanActivate} 语义不变。</li>
 * </ol>
 *
 * <p>与 {@link TestErpCtContractStateMachineBaselineIoC} 对照：同一 {@code assertCanTerminate(NEGOTIATION)}
 * 在基线放行、在 Delta 抛异常 —— 构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 *
 * <p>Delta 层经 {@code @NopTestProperty} 设置 {@code nop.core.vfs.delta-layer-ids=test-ct-delta} 激活，
 * Delta 层文件 {@code _vfs/_delta/test-ct-delta/erp/ct/beans/app-service.beans.xml} 以同名 bean id 覆盖基线。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.core.vfs.delta-layer-ids", value = "test-ct-delta")
public class TestErpCtContractStateMachineDeltaOverride extends JunitAutoTestCase {

    @Inject
    ErpCtContractStateMachine stateMachine;

    @Test
    public void testDeltaBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpCtContractStateMachineDelta,
                "Delta 加载：容器应解析为 ErpCtContractStateMachineDelta 派生类（同名 bean id 覆盖生效）");
        assertEquals(ErpCtContractStateMachineDelta.class, stateMachine.getClass(),
                "Delta 加载：实例类 = ErpCtContractStateMachineDelta（非基线类）");
    }

    @Test
    public void testDeltaOverriddenEdgeDiffersFromBaseline() {
        // Delta 覆盖语义：terminate(NEGOTIATION) 抛异常（基线放行 → Delta 收紧，可区分差异）
        NopException ex = assertThrows(NopException.class,
                () -> stateMachine.assertCanTerminate(ErpCtConstants.CONTRACT_STATUS_NEGOTIATION));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Delta terminate(NEGOTIATION) 报告 common 层非法迁移码（Delta 收紧为仅 ACTIVE）");

        // Delta 仍允许 ACTIVE（基线 + Delta 均允许）
        stateMachine.assertCanTerminate(ErpCtConstants.CONTRACT_STATUS_ACTIVE);
    }

    @Test
    public void testNonOverriddenActionsInheritBaseline() {
        // Delta 未覆盖的动作继承基线矩阵不变
        stateMachine.assertCanActivate(ErpCtConstants.CONTRACT_STATUS_NEGOTIATION); // 基线 + Delta 均放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanActivate(ErpCtConstants.CONTRACT_STATUS_ACTIVE),
                "Delta 未覆盖 activate：activate(ACTIVE) 仍非法（基线继承）");
        stateMachine.assertCanSuspend(ErpCtConstants.CONTRACT_STATUS_ACTIVE); // 基线 + Delta 均放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanSuspend(ErpCtConstants.CONTRACT_STATUS_SUSPENDED),
                "Delta 未覆盖 suspend：suspend(SUSPENDED) 仍非法（基线继承）");
        // 终态分类不变（Delta 未覆盖 isTerminal）
        assertTrue(stateMachine.isTerminal(ErpCtConstants.CONTRACT_STATUS_EXPIRED),
                "Delta 未覆盖 isTerminal：EXPIRED 仍为终态");
    }
}
