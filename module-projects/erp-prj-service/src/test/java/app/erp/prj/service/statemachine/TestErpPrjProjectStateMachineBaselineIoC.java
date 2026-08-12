package app.erp.prj.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.prj.service.ErpPrjConstants;
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
 * M2.4 Delta 覆盖运行时实证 —— **基线加载**侧（plan Phase 3；契约 §6 业务级 Delta 实证义务，复用 M1.2 范式）。
 *
 * <p>在真实 IoC 容器（prj-service 测试容器，加载生产 {@code app-service.beans.xml}，无 testBeansFile 覆盖）下
 * 证明：容器解析的 {@link ErpPrjProjectStateMachine} 为基线实例，其矩阵行为 = Phase 1 矩阵。
 *
 * <p>与 {@link TestErpPrjProjectStateMachineDeltaOverride} 对照：本测试断言
 * {@code assertCanCancel(DRAFT)} **放行**（基线允许 DRAFT/OPEN/ON_HOLD），Delta 测试断言其**抛异常**（收紧为仅 OPEN）。
 * 两者构成可区分的基线/Delta 双加载证据。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpPrjProjectStateMachineBaselineIoC extends JunitAutoTestCase {

    @Inject
    ErpPrjProjectStateMachine stateMachine;

    @Test
    public void testBaselineBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpPrjProjectStateMachine,
                "基线加载：容器解析的应为基线 ErpPrjProjectStateMachine 实例");
        // 确认不是 Delta 派生类
        assertEquals(ErpPrjProjectStateMachine.class, stateMachine.getClass(),
                "基线加载：实例类 = ErpPrjProjectStateMachine（非 Delta 派生）");
    }

    @Test
    public void testBaselineCancelAllowsAllNonTerminal() {
        // 基线矩阵：cancel 允许 DRAFT/OPEN/ON_HOLD（非终态均合法）
        stateMachine.assertCanCancel(ErpPrjConstants.PROJECT_STATUS_DRAFT); // 放行
        stateMachine.assertCanCancel(ErpPrjConstants.PROJECT_STATUS_OPEN); // 放行
        stateMachine.assertCanCancel(ErpPrjConstants.PROJECT_STATUS_ON_HOLD); // 放行

        // 关键差异点：cancel(DRAFT) 在基线放行（Delta 将收紧为非法）
        stateMachine.assertCanCancel(ErpPrjConstants.PROJECT_STATUS_DRAFT);
    }

    @Test
    public void testBaselineCancelRejectsTerminal() {
        NopException ex1 = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpPrjConstants.PROJECT_STATUS_COMPLETED));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex1.getErrorCode(),
                "基线 cancel(COMPLETED) 报告 common 层非法迁移码");

        NopException ex2 = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpPrjConstants.PROJECT_STATUS_CANCELLED));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex2.getErrorCode(),
                "基线 cancel(CANCELLED) 报告 common 层非法迁移码");
    }

    @Test
    public void testBaselineOtherActionsMatchMatrix() {
        // 基线非覆盖动作保持矩阵语义（Delta 不覆盖这些，应继承相同行为）
        stateMachine.assertCanStart(ErpPrjConstants.PROJECT_STATUS_DRAFT); // 放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanStart(ErpPrjConstants.PROJECT_STATUS_OPEN),
                "基线 start(OPEN) 非法");
        stateMachine.assertCanHold(ErpPrjConstants.PROJECT_STATUS_OPEN); // 放行
        stateMachine.assertCanResume(ErpPrjConstants.PROJECT_STATUS_ON_HOLD); // 放行
        stateMachine.assertCanClose(ErpPrjConstants.PROJECT_STATUS_OPEN); // 放行
    }
}
