package app.erp.prj.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.prj.service.ErpPrjConstants;
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
 * M2.4 Delta 覆盖运行时实证 —— **Delta 加载**侧（plan Phase 3；契约 §6 业务级 Delta 实证义务，复用 M1.2 范式）。
 *
 * <p>在真实 IoC 容器（prj-service 测试容器，经 VFS Delta 层 {@code test-prj-delta} 以同名 bean id 覆盖基线为
 * {@link ErpPrjProjectStateMachineDelta}）下证明：
 * <ol>
 *   <li>容器解析的 Bean 为 Delta 派生类（经真实 bean 解析注入生效，非静态检查/非编译派生类）；</li>
 *   <li>Delta 覆盖的语义按 Delta 生效：{@code assertCanCancel(DRAFT)} 抛异常（基线放行）；</li>
 *   <li>非覆盖动作继承基线不变：{@code assertCanCancel(OPEN)} 仍放行、start/hold/resume/close 矩阵不变。</li>
 * </ol>
 *
 * <p>与 {@link TestErpPrjProjectStateMachineBaselineIoC} 对照：同一 {@code assertCanCancel(DRAFT)}
 * 在基线放行、在 Delta 抛异常 —— 构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 *
 * <p>Delta 层经 {@code @NopTestProperty} 设置 {@code nop.core.vfs.delta-layer-ids=test-prj-delta} 激活，
 * Delta 层文件 {@code _vfs/_delta/test-prj-delta/erp/prj/beans/app-service.beans.xml} 以同名 bean id 覆盖基线。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.core.vfs.delta-layer-ids", value = "test-prj-delta")
public class TestErpPrjProjectStateMachineDeltaOverride extends JunitAutoTestCase {

    @Inject
    ErpPrjProjectStateMachine stateMachine;

    @Test
    public void testDeltaBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpPrjProjectStateMachineDelta,
                "Delta 加载：容器应解析为 ErpPrjProjectStateMachineDelta 派生类（同名 bean id 覆盖生效）");
        assertEquals(ErpPrjProjectStateMachineDelta.class, stateMachine.getClass(),
                "Delta 加载：实例类 = ErpPrjProjectStateMachineDelta（非基线类）");
    }

    @Test
    public void testDeltaOverriddenEdgeDiffersFromBaseline() {
        // Delta 覆盖语义：cancel(DRAFT) 抛异常（基线放行 → Delta 收紧，可区分差异）
        NopException ex = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpPrjConstants.PROJECT_STATUS_DRAFT));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Delta cancel(DRAFT) 报告 common 层非法迁移码");

        // Delta 收紧的来源态 ON_HOLD 同样非法（Delta 仅允许 OPEN）
        NopException ex2 = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpPrjConstants.PROJECT_STATUS_ON_HOLD));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex2.getErrorCode(),
                "Delta cancel(ON_HOLD) 报告 common 层非法迁移码");

        // Delta 仍允许 OPEN（基线 + Delta 均允许）
        stateMachine.assertCanCancel(ErpPrjConstants.PROJECT_STATUS_OPEN);
    }

    @Test
    public void testNonOverriddenActionsInheritBaseline() {
        // Delta 未覆盖的动作继承基线矩阵不变
        stateMachine.assertCanStart(ErpPrjConstants.PROJECT_STATUS_DRAFT); // 基线 + Delta 均放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanStart(ErpPrjConstants.PROJECT_STATUS_OPEN),
                "Delta 未覆盖 start：start(OPEN) 仍非法（基线继承）");
        stateMachine.assertCanHold(ErpPrjConstants.PROJECT_STATUS_OPEN); // 基线 + Delta 均放行
        stateMachine.assertCanResume(ErpPrjConstants.PROJECT_STATUS_ON_HOLD); // 基线 + Delta 均放行
        stateMachine.assertCanClose(ErpPrjConstants.PROJECT_STATUS_OPEN); // 基线 + Delta 均放行
        // 终态分类不变（Delta 未覆盖 isTerminal）
        assertTrue(stateMachine.isTerminal(ErpPrjConstants.PROJECT_STATUS_COMPLETED),
                "Delta 未覆盖 isTerminal：COMPLETED 仍为终态");
        assertTrue(stateMachine.isTerminal(ErpPrjConstants.PROJECT_STATUS_CANCELLED),
                "Delta 未覆盖 isTerminal：CANCELLED 仍为终态");
    }
}
