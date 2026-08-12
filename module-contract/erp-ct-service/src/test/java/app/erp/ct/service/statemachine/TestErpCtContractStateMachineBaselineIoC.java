package app.erp.ct.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.ct.service.ErpCtConstants;
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
 * M2.18 Delta 覆盖运行时实证 —— **基线加载**侧（plan Phase 3）。
 *
 * <p>在真实 IoC 容器（ct-service 测试容器，加载生产 {@code app-service.beans.xml}，无 testBeansFile 覆盖）下
 * 证明：容器解析的 {@link ErpCtContractStateMachine} 为基线实例，其矩阵行为 = Phase 1 矩阵。
 *
 * <p>与 {@link TestErpCtContractStateMachineDeltaOverride} 对照：本测试断言
 * {@code assertCanTerminate(NEGOTIATION)} **放行**（基线允许 NEGOTIATION 谈判破裂出口），Delta 测试断言其**抛异常**
 * （Delta 收紧 terminate 仅 ACTIVE）。两者构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpCtContractStateMachineBaselineIoC extends JunitAutoTestCase {

    @Inject
    ErpCtContractStateMachine stateMachine;

    @Test
    public void testBaselineBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpCtContractStateMachine,
                "基线加载：容器解析的应为基线 ErpCtContractStateMachine 实例");
        // 确认不是 Delta 派生类
        assertEquals(ErpCtContractStateMachine.class, stateMachine.getClass(),
                "基线加载：实例类 = ErpCtContractStateMachine（非 Delta 派生）");
    }

    @Test
    public void testBaselineTerminateAllowsActiveAndNegotiation() {
        // 基线矩阵：terminate 允许 ACTIVE + NEGOTIATION（Phase 1 矩阵）
        stateMachine.assertCanTerminate(ErpCtConstants.CONTRACT_STATUS_ACTIVE); // 放行
        stateMachine.assertCanTerminate(ErpCtConstants.CONTRACT_STATUS_NEGOTIATION); // 放行（谈判破裂出口）

        // 其余态非法
        NopException ex = assertThrows(NopException.class,
                () -> stateMachine.assertCanTerminate(ErpCtConstants.CONTRACT_STATUS_DRAFT));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "基线 terminate(DRAFT) 报告 common 层非法迁移码");
    }

    @Test
    public void testBaselineOtherActionsMatchMatrix() {
        // 基线非覆盖动作保持矩阵语义（Delta 不覆盖这些，应继承相同行为）
        stateMachine.assertCanActivate(ErpCtConstants.CONTRACT_STATUS_NEGOTIATION); // 放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanActivate(ErpCtConstants.CONTRACT_STATUS_ACTIVE),
                "基线 activate(ACTIVE) 非法");
        stateMachine.assertCanSuspend(ErpCtConstants.CONTRACT_STATUS_ACTIVE); // 放行
    }
}
