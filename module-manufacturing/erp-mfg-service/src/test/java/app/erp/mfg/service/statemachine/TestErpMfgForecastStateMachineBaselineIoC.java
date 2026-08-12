package app.erp.mfg.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Delta 覆盖运行时实证 —— **基线加载**侧（plan Phase 3；契约 §6）。
 *
 * <p>在真实 IoC 容器（mfg-service 测试容器，加载生产 {@code app-service.beans.xml}，无 delta-layer 覆盖）下
 * 证明：容器解析的 {@link ErpMfgForecastStateMachine} 为基线实例，其矩阵行为 = Phase 1 矩阵。
 *
 * <p>与 {@link TestErpMfgForecastStateMachineDeltaOverride} 对照：本测试断言
 * {@code assertCanCancel(APPROVED)} **放行**（基线 allow-list 含 APPROVED），Delta 测试断言其**抛异常**。
 * 两者构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMfgForecastStateMachineBaselineIoC extends JunitAutoTestCase {

    @Inject
    ErpMfgForecastStateMachine stateMachine;

    @Test
    public void testBaselineBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpMfgForecastStateMachine,
                "基线加载：容器解析的应为基线 ErpMfgForecastStateMachine 实例");
        assertEquals(ErpMfgForecastStateMachine.class, stateMachine.getClass(),
                "基线加载：实例类 = ErpMfgForecastStateMachine（非 Delta 派生）");
    }

    @Test
    public void testBaselineCancelAllowsDraftAndApproved() {
        // 基线矩阵：cancel 允许 {DRAFT, APPROVED}
        stateMachine.assertCanCancel(ErpMfgConstants.FORECAST_STATUS_DRAFT); // 放行
        stateMachine.assertCanCancel(ErpMfgConstants.FORECAST_STATUS_APPROVED); // 放行

        // 终态 + 死状态仍非法
        assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpMfgConstants.FORECAST_STATUS_CANCELLED),
                "基线 cancel(CANCELLED) 非法（refuse-terminal）");
        NopException fromDead = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpMfgConstants.FORECAST_STATUS_CONSUMED),
                "基线 cancel(CONSUMED) 非法（refuse-dead-state）");
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), fromDead.getErrorCode(),
                "基线 cancel(CONSUMED) 报告 common 层非法迁移码");
    }

    @Test
    public void testBaselineApproveAndTerminalInheritMatrix() {
        // 基线非覆盖动作保持矩阵语义
        stateMachine.assertCanApprove(ErpMfgConstants.FORECAST_STATUS_DRAFT); // 放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanApprove(ErpMfgConstants.FORECAST_STATUS_APPROVED),
                "基线 approve(APPROVED) 非法");
        // 终态分类不变
        assertTrue(stateMachine.isTerminal(ErpMfgConstants.FORECAST_STATUS_CANCELLED));
        assertFalse(stateMachine.isTerminal(ErpMfgConstants.FORECAST_STATUS_CONSUMED),
                "CONSUMED 不入终态集（预留死状态）");
    }
}
