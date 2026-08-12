package app.erp.md.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.md.service.ErpMdConstants;
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
 * M2.1 Delta 覆盖运行时实证 —— **Delta 加载**侧（plan Phase 3）。
 *
 * <p>在真实 IoC 容器（md-service 测试容器，经 VFS Delta 层 {@code test-md-delta} 以同名 bean id 覆盖基线为
 * {@link ErpMdSupplierApprovalStateMachineDelta}）下证明：
 * <ol>
 *   <li>容器解析的 Bean 为 Delta 派生类（经真实 bean 解析注入生效，非静态检查/非编译派生类）；</li>
 *   <li>Delta 覆盖的语义按 Delta 生效：{@code assertCanApprove(PROBATION)} 抛异常（基线放行 → Delta 收紧）；</li>
 *   <li>非覆盖动作继承基线不变：{@code assertCanApprove(APPLIED)} 仍放行、{@code assertCanApply} 语义不变。</li>
 * </ol>
 *
 * <p>与 {@link TestErpMdSupplierApprovalStateMachineBaselineIoC} 对照：同一 {@code assertCanApprove(PROBATION)}
 * 在基线放行、在 Delta 抛异常 —— 构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 *
 * <p>Delta 层经 {@code @NopTestProperty} 设置 {@code nop.core.vfs.delta-layer-ids=test-md-delta} 激活，
 * Delta 层文件 {@code _vfs/_delta/test-md-delta/erp/md/beans/app-service.beans.xml} 以同名 bean id 覆盖基线。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.core.vfs.delta-layer-ids", value = "test-md-delta")
public class TestErpMdSupplierApprovalStateMachineDeltaOverride extends JunitAutoTestCase {

    @Inject
    ErpMdSupplierApprovalStateMachine stateMachine;

    @Test
    public void testDeltaBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpMdSupplierApprovalStateMachineDelta,
                "Delta 加载：容器应解析为 ErpMdSupplierApprovalStateMachineDelta 派生类（同名 bean id 覆盖生效）");
        assertEquals(ErpMdSupplierApprovalStateMachineDelta.class, stateMachine.getClass(),
                "Delta 加载：实例类 = ErpMdSupplierApprovalStateMachineDelta（非基线类）");
    }

    @Test
    public void testDeltaOverriddenEdgeDiffersFromBaseline() {
        // Delta 覆盖语义：approve(PROBATION) 抛异常（基线放行 → Delta 收紧，可区分差异）
        NopException ex = assertThrows(NopException.class,
                () -> stateMachine.assertCanApprove(ErpMdConstants.APPROVAL_STATUS_PROBATION));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Delta approve(PROBATION) 报告 common 层非法迁移码（Delta 收紧为仅 APPLIED）");

        // Delta 仍允许 APPLIED（基线 + Delta 均允许）
        stateMachine.assertCanApprove(ErpMdConstants.APPROVAL_STATUS_APPLIED);
    }

    @Test
    public void testNonOverriddenActionsInheritBaseline() {
        // Delta 未覆盖的动作继承基线矩阵不变
        stateMachine.assertCanApply(null); // 基线 + Delta 均放行（新建）
        stateMachine.assertCanApply(ErpMdConstants.APPROVAL_STATUS_REJECTED); // 基线 + Delta 均放行（重新申请）
        assertThrows(NopException.class,
                () -> stateMachine.assertCanApply(ErpMdConstants.APPROVAL_STATUS_APPROVED),
                "Delta 未覆盖 apply：apply(APPROVED) 仍非法（基线继承）");
        stateMachine.assertCanSuspend(ErpMdConstants.APPROVAL_STATUS_APPROVED); // 基线 + Delta 均放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanSuspend(ErpMdConstants.APPROVAL_STATUS_SUSPENDED),
                "Delta 未覆盖 suspend：suspend(SUSPENDED) 仍非法（基线继承）");
        // 终态分类不变（Delta 未覆盖 isTerminal）
        assertTrue(!stateMachine.isTerminal(ErpMdConstants.APPROVAL_STATUS_REJECTED),
                "Delta 未覆盖 isTerminal：REJECTED 仍非严格终态（基线继承）");
    }
}
