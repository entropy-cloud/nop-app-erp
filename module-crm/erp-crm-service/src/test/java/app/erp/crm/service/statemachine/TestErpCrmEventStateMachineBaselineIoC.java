package app.erp.crm.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.crm.service.ErpCrmConstants;
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
 * M2.2 Delta 覆盖运行时实证 —— **基线加载**侧（plan Phase 3）。
 *
 * <p>在真实 IoC 容器（crm-service 测试容器，加载生产 {@code app-service.beans.xml}，无 delta-layer 激活）下
 * 证明：容器解析的 {@link ErpCrmEventStateMachine} 为基线实例，其矩阵行为 = Phase 1 矩阵。
 *
 * <p>与 {@link TestErpCrmEventStateMachineDeltaOverride} 对照：本测试断言
 * {@code assertCanCancel(COMPLETED)} **抛异常**（基线仅允许 PLANNED），Delta 测试断言其**放行**
 * （Delta 放宽 cancel 允许 COMPLETED「soft void」）。两者构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCrmEventStateMachineBaselineIoC extends JunitAutoTestCase {

    @Inject
    ErpCrmEventStateMachine stateMachine;

    @Test
    public void testBaselineBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpCrmEventStateMachine,
                "基线加载：容器解析的应为基线 ErpCrmEventStateMachine 实例");
        // 确认不是 Delta 派生类
        assertEquals(ErpCrmEventStateMachine.class, stateMachine.getClass(),
                "基线加载：实例类 = ErpCrmEventStateMachine（非 Delta 派生）");
    }

    @Test
    public void testBaselineCancelAllowsOnlyPlanned() {
        // 基线矩阵：cancel 仅允许 PLANNED（Phase 1 矩阵）
        stateMachine.assertCanCancel(ErpCrmConstants.EVENT_STATUS_PLANNED); // 放行

        // 终态 COMPLETED/CANCELLED 对 cancel 非法
        NopException exCompleted = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpCrmConstants.EVENT_STATUS_COMPLETED));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), exCompleted.getErrorCode(),
                "基线 cancel(COMPLETED) 报告 common 层非法迁移码");

        NopException exCancelled = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpCrmConstants.EVENT_STATUS_CANCELLED));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), exCancelled.getErrorCode(),
                "基线 cancel(CANCELLED) 报告 common 层非法迁移码");
    }

    @Test
    public void testBaselineOtherActionsMatchMatrix() {
        // 基线非覆盖动作保持矩阵语义（Delta 不覆盖这些，应继承相同行为）
        stateMachine.assertCanComplete(ErpCrmConstants.EVENT_STATUS_PLANNED); // 放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanComplete(ErpCrmConstants.EVENT_STATUS_COMPLETED),
                "基线 complete(COMPLETED) 非法");
        assertTrue(stateMachine.isTerminal(ErpCrmConstants.EVENT_STATUS_COMPLETED),
                "基线 isTerminal(COMPLETED) = true");
    }
}
