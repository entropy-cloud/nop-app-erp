package app.erp.hr.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.hr.service.ErpHrConstants;
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
 * M2.11 Delta 覆盖运行时实证 —— **基线加载**侧（plan Phase 3）。
 *
 * <p>在真实 IoC 容器（hr-service 测试容器，加载生产 {@code app-service.beans.xml}，无 delta-layer 覆盖）下
 * 证明：容器解析的 {@link ErpHrLeaveRequestStateMachine} 为基线实例，其矩阵行为 = Phase 1 矩阵。
 *
 * <p>与 {@link TestErpHrLeaveRequestStateMachineDeltaOverride} 对照：本测试断言
 * {@code assertCanCancel(SUBMITTED)} **抛异常**（基线 cancel 单源 APPROVED），Delta 测试断言其**放行**
 * （Delta 放开至 SUBMITTED）。两者构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpHrLeaveRequestStateMachineBaselineIoC extends JunitAutoTestCase {

    @Inject
    ErpHrLeaveRequestStateMachine stateMachine;

    @Test
    public void testBaselineBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpHrLeaveRequestStateMachine,
                "基线加载：容器解析的应为基线 ErpHrLeaveRequestStateMachine 实例");
        assertEquals(ErpHrLeaveRequestStateMachine.class, stateMachine.getClass(),
                "基线加载：实例类 = ErpHrLeaveRequestStateMachine（非 Delta 派生）");
    }

    @Test
    public void testBaselineCancelIsSingleSourceApproved() {
        // 基线矩阵：cancel 仅 APPROVED（Phase 1 矩阵，单源）
        stateMachine.assertCanCancel(ErpHrConstants.LEAVE_STATUS_APPROVED); // 放行

        // SUBMITTED/DRAFT/REJECTED/CANCELLED 全非法（含 owner doc 声明但代码未实现的 DRAFT/SUBMITTED）
        NopException ex = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpHrConstants.LEAVE_STATUS_SUBMITTED));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "基线 cancel(SUBMITTED) 报告 common 层非法迁移码（单源 APPROVED）");
    }

    @Test
    public void testBaselineOtherActionsMatchMatrix() {
        // 基线非覆盖动作保持矩阵语义（Delta 不覆盖这些，应继承相同行为）
        stateMachine.assertCanSubmit(ErpHrConstants.LEAVE_STATUS_DRAFT); // 放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanSubmit(ErpHrConstants.LEAVE_STATUS_APPROVED),
                "基线 submit(APPROVED) 非法");
        stateMachine.assertCanApprove(ErpHrConstants.LEAVE_STATUS_SUBMITTED); // 放行
    }
}
