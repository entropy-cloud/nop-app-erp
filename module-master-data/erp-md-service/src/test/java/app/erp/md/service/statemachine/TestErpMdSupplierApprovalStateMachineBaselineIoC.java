package app.erp.md.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.md.service.ErpMdConstants;
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
 * M2.1 Delta 覆盖运行时实证 —— **基线加载**侧（plan Phase 3）。
 *
 * <p>在真实 IoC 容器（md-service 测试容器，加载生产 {@code app-service.beans.xml}，无 testBeansFile 覆盖）下
 * 证明：容器解析的 {@link ErpMdSupplierApprovalStateMachine} 为基线实例，其矩阵行为 = Phase 1 矩阵。
 *
 * <p>与 {@link TestErpMdSupplierApprovalStateMachineDeltaOverride} 对照：本测试断言
 * {@code assertCanApprove(PROBATION)} **放行**（基线允许 PROBATION 试用通过出口），Delta 测试断言其**抛异常**
 * （Delta 收紧 approve 仅 APPLIED）。两者构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
public class TestErpMdSupplierApprovalStateMachineBaselineIoC extends JunitAutoTestCase {

    @Inject
    ErpMdSupplierApprovalStateMachine stateMachine;

    @Test
    public void testBaselineBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpMdSupplierApprovalStateMachine,
                "基线加载：容器解析的应为基线 ErpMdSupplierApprovalStateMachine 实例");
        // 确认不是 Delta 派生类
        assertEquals(ErpMdSupplierApprovalStateMachine.class, stateMachine.getClass(),
                "基线加载：实例类 = ErpMdSupplierApprovalStateMachine（非 Delta 派生）");
    }

    @Test
    public void testBaselineApproveAllowsAppliedAndProbation() {
        // 基线矩阵：approve 允许 APPLIED + PROBATION（Phase 1 矩阵）
        stateMachine.assertCanApprove(ErpMdConstants.APPROVAL_STATUS_APPLIED); // 放行
        stateMachine.assertCanApprove(ErpMdConstants.APPROVAL_STATUS_PROBATION); // 放行（试用通过出口）

        // 其余态非法
        NopException ex = assertThrows(NopException.class,
                () -> stateMachine.assertCanApprove(ErpMdConstants.APPROVAL_STATUS_APPROVED));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "基线 approve(APPROVED) 报告 common 层非法迁移码");
    }

    @Test
    public void testBaselineOtherActionsMatchMatrix() {
        // 基线非覆盖动作保持矩阵语义（Delta 不覆盖这些，应继承相同行为）
        stateMachine.assertCanApply(null); // 放行（新建）
        stateMachine.assertCanApply(ErpMdConstants.APPROVAL_STATUS_REJECTED); // 放行（重新申请）
        assertThrows(NopException.class,
                () -> stateMachine.assertCanApply(ErpMdConstants.APPROVAL_STATUS_APPROVED),
                "基线 apply(APPROVED) 非法");
        stateMachine.assertCanSuspend(ErpMdConstants.APPROVAL_STATUS_APPROVED); // 放行
    }
}
