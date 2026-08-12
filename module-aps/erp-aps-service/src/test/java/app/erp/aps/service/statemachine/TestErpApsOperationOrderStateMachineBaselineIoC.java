package app.erp.aps.service.statemachine;

import app.erp.aps.service.ErpApsConstants;
import app.erp.common.service.ErpCommonErrors;
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
 * M2.13 Delta 覆盖运行时实证 —— **基线加载**侧（plan Phase 3）。
 *
 * <p>在真实 IoC 容器（aps-service 测试容器，加载生产 {@code app-service.beans.xml}，无 delta-layer 覆盖）下
 * 证明：容器解析的 {@link ErpApsOperationOrderStateMachine} 为基线实例，其矩阵行为 = Phase 1 矩阵。
 *
 * <p>与 {@link TestErpApsOperationOrderStateMachineDeltaOverride} 对照：本测试断言
 * {@code assertCanCancel(IN_PROGRESS)} **放行**（基线允许 IN_PROGRESS 异常终止出口），Delta 测试断言其**抛异常**
 * （Delta 收紧 cancel 仅 DRAFT/PLANNED）。两者构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpApsOperationOrderStateMachineBaselineIoC extends JunitAutoTestCase {

    @Inject
    ErpApsOperationOrderStateMachine stateMachine;

    @Test
    public void testBaselineBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpApsOperationOrderStateMachine,
                "基线加载：容器解析的应为基线 ErpApsOperationOrderStateMachine 实例");
        // 确认不是 Delta 派生类
        assertEquals(ErpApsOperationOrderStateMachine.class, stateMachine.getClass(),
                "基线加载：实例类 = ErpApsOperationOrderStateMachine（非 Delta 派生）");
    }

    @Test
    public void testBaselineCancelAllowsThreeSources() {
        // 基线矩阵：cancel 允许 DRAFT + PLANNED + IN_PROGRESS（Phase 1 矩阵，三源）
        stateMachine.assertCanCancel(ErpApsConstants.OP_STATUS_DRAFT); // 放行
        stateMachine.assertCanCancel(ErpApsConstants.OP_STATUS_PLANNED); // 放行
        stateMachine.assertCanCancel(ErpApsConstants.OP_STATUS_IN_PROGRESS); // 放行（异常终止出口）

        // 终态非法
        NopException ex = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpApsConstants.OP_STATUS_FINISHED));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "基线 cancel(FINISHED) 报告 common 层非法迁移码");
    }

    @Test
    public void testBaselineOtherActionsMatchMatrix() {
        // 基线非覆盖动作保持矩阵语义（Delta 不覆盖这些，应继承相同行为）
        stateMachine.assertCanStart(ErpApsConstants.OP_STATUS_PLANNED); // 放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanStart(ErpApsConstants.OP_STATUS_DRAFT),
                "基线 start(DRAFT) 非法");
        stateMachine.assertCanComplete(ErpApsConstants.OP_STATUS_IN_PROGRESS); // 放行
        stateMachine.assertCanRevertToDraft(ErpApsConstants.OP_STATUS_PLANNED); // 放行
    }
}
