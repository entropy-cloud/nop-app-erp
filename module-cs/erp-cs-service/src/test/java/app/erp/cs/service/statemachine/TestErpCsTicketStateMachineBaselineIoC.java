package app.erp.cs.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.cs.service.ErpCsConstants;
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
 * M1.2 Delta 覆盖运行时实证 —— **基线加载**侧（plan Phase 3）。
 *
 * <p>在真实 IoC 容器（cs-service 测试容器，加载生产 {@code app-service.beans.xml}，无 testBeansFile 覆盖）下
 * 证明：容器解析的 {@link ErpCsTicketStateMachine} 为基线实例，其矩阵行为 = Phase 1 矩阵。
 *
 * <p>与 {@link TestErpCsTicketStateMachineDeltaOverride} 对照：本测试断言
 * {@code assertCanAssign(RESOLVED)} **抛异常**（基线仅允许 NEW），Delta 测试断言其**放行**。
 * 两者构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCsTicketStateMachineBaselineIoC extends JunitAutoTestCase {

    @Inject
    ErpCsTicketStateMachine stateMachine;

    @Test
    public void testBaselineBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpCsTicketStateMachine,
                "基线加载：容器解析的应为基线 ErpCsTicketStateMachine 实例");
        // 确认不是 Delta 派生类
        assertEquals(ErpCsTicketStateMachine.class, stateMachine.getClass(),
                "基线加载：实例类 = ErpCsTicketStateMachine（非 Delta 派生）");
    }

    @Test
    public void testBaselineAssignAllowsOnlyNew() {
        // 基线矩阵：assign 仅允许 NEW（Phase 1 矩阵）
        stateMachine.assertCanAssign(ErpCsConstants.TICKET_STATUS_NEW); // 放行

        // 关键差异点：assign(RESOLVED) 在基线非法（Delta 将放开）
        NopException ex = assertThrows(NopException.class,
                () -> stateMachine.assertCanAssign(ErpCsConstants.TICKET_STATUS_RESOLVED));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "基线 assign(RESOLVED) 报告 common 层非法迁移码");
    }

    @Test
    public void testBaselineOtherActionsMatchMatrix() {
        // 基线非覆盖动作保持矩阵语义（Delta 不覆盖这些，应继承相同行为）
        stateMachine.assertCanResolve(ErpCsConstants.TICKET_STATUS_IN_PROGRESS); // 放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanResolve(ErpCsConstants.TICKET_STATUS_RESOLVED),
                "基线 resolve(RESOLVED) 非法");
        stateMachine.assertCanStart(ErpCsConstants.TICKET_STATUS_ASSIGNED); // 放行
    }
}
