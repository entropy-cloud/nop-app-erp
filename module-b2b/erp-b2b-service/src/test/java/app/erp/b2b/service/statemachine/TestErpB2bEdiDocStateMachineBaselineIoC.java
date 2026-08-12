package app.erp.b2b.service.statemachine;

import app.erp.b2b.service.ErpB2bConstants;
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
 * Delta 覆盖运行时实证 —— **基线加载**侧（plan Phase 3）。
 *
 * <p>在真实 IoC 容器（b2b-service 测试容器，加载生产 {@code app-service.beans.xml}，无 delta-layer 覆盖）下
 * 证明：容器解析的 {@link ErpB2bEdiDocStateMachine} 为基线实例，其矩阵行为 = Phase 1 矩阵。
 *
 * <p>与 {@link TestErpB2bEdiDocStateMachineDeltaOverride} 对照：本测试断言
 * {@code assertCanCancel(SENT)} 和 {@code assertCanCancel(ERROR)} **放行**（基线多来源 cancel），
 * Delta 测试断言其**抛异常**（Delta 收紧为仅 TO_SEND）。两者构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpB2bEdiDocStateMachineBaselineIoC extends JunitAutoTestCase {

    @Inject
    ErpB2bEdiDocStateMachine stateMachine;

    @Test
    public void testBaselineBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpB2bEdiDocStateMachine,
                "基线加载：容器解析的应为基线 ErpB2bEdiDocStateMachine 实例");
        assertEquals(ErpB2bEdiDocStateMachine.class, stateMachine.getClass(),
                "基线加载：实例类 = ErpB2bEdiDocStateMachine（非 Delta 派生）");
    }

    @Test
    public void testBaselineCancelAllowsMultiSource() {
        // 基线矩阵：cancel 允许多来源 TO_SEND/SENT/ERROR（Phase 1 矩阵）
        stateMachine.assertCanCancel(ErpB2bConstants.EDI_DOC_STATE_TO_SEND); // 放行
        stateMachine.assertCanCancel(ErpB2bConstants.EDI_DOC_STATE_SENT); // 放行
        stateMachine.assertCanCancel(ErpB2bConstants.EDI_DOC_STATE_ERROR); // 放行

        // 关键差异点：cancel(CANCELLED) 在基线非法（终态）
        NopException ex = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpB2bConstants.EDI_DOC_STATE_CANCELLED));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "基线 cancel(CANCELLED) 报告 common 层非法迁移码");
    }

    @Test
    public void testBaselineOtherActionsMatchMatrix() {
        // 基线非覆盖动作保持矩阵语义（Delta 不覆盖这些，应继承相同行为）
        stateMachine.assertCanMarkSent(ErpB2bConstants.EDI_DOC_STATE_TO_SEND); // 放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanMarkSent(ErpB2bConstants.EDI_DOC_STATE_SENT),
                "基线 markSent(SENT) 非法");
        stateMachine.assertCanArchive(ErpB2bConstants.EDI_DOC_STATE_RECEIVED); // 放行
    }
}
