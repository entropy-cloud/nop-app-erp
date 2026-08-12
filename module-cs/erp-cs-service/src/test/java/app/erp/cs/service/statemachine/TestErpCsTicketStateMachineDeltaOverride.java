package app.erp.cs.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.cs.service.ErpCsConstants;
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
 * M1.2 Delta 覆盖运行时实证 —— **Delta 加载**侧（plan Phase 3）。
 *
 * <p>在真实 IoC 容器（cs-service 测试容器，经 VFS Delta 层 {@code test-cs-delta} 以同名 bean id 覆盖基线为
 * {@link ErpCsTicketStateMachineDelta}）下证明：
 * <ol>
 *   <li>容器解析的 Bean 为 Delta 派生类（经真实 bean 解析注入生效，非静态检查/非编译派生类）；</li>
 *   <li>Delta 覆盖的语义按 Delta 生效：{@code assertCanAssign(RESOLVED)} 放行（基线非法）；</li>
 *   <li>非覆盖动作继承基线不变：{@code assertCanResolve(RESOLVED)} 仍非法、{@code assertCanAssign(NEW)} 仍放行。</li>
 * </ol>
 *
 * <p>与 {@link TestErpCsTicketStateMachineBaselineIoC} 对照：同一 {@code assertCanAssign(RESOLVED)}
 * 在基线抛异常、在 Delta 放行 —— 构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 *
 * <p>Delta 层经 {@code @NopTestProperty} 设置 {@code nop.core.vfs.delta-layer-ids=test-cs-delta} 激活，
 * Delta 层文件 {@code _vfs/_delta/test-cs-delta/erp/cs/beans/app-service.beans.xml} 以同名 bean id 覆盖基线。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.core.vfs.delta-layer-ids", value = "test-cs-delta")
public class TestErpCsTicketStateMachineDeltaOverride extends JunitAutoTestCase {

    @Inject
    ErpCsTicketStateMachine stateMachine;

    @Test
    public void testDeltaBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpCsTicketStateMachineDelta,
                "Delta 加载：容器应解析为 ErpCsTicketStateMachineDelta 派生类（同名 bean id 覆盖生效）");
        assertEquals(ErpCsTicketStateMachineDelta.class, stateMachine.getClass(),
                "Delta 加载：实例类 = ErpCsTicketStateMachineDelta（非基线类）");
    }

    @Test
    public void testDeltaOverriddenEdgeDiffersFromBaseline() {
        // Delta 覆盖语义：assign(RESOLVED) 放行（基线非法 → Delta 合法，可区分差异）
        stateMachine.assertCanAssign(ErpCsConstants.TICKET_STATUS_RESOLVED); // Delta 放行，不抛

        // Delta 仍允许 NEW（基线 + Delta 均允许）
        stateMachine.assertCanAssign(ErpCsConstants.TICKET_STATUS_NEW);

        // Delta 收紧的来源态仍非法（如 IN_PROGRESS 不在 NEW/RESOLVED 中）
        NopException ex = assertThrows(NopException.class,
                () -> stateMachine.assertCanAssign(ErpCsConstants.TICKET_STATUS_IN_PROGRESS));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Delta assign(IN_PROGRESS) 仍报告 common 层非法迁移码");
    }

    @Test
    public void testNonOverriddenActionsInheritBaseline() {
        // Delta 未覆盖的动作继承基线矩阵不变
        stateMachine.assertCanResolve(ErpCsConstants.TICKET_STATUS_IN_PROGRESS); // 基线 + Delta 均放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanResolve(ErpCsConstants.TICKET_STATUS_RESOLVED),
                "Delta 未覆盖 resolve：resolve(RESOLVED) 仍非法（基线继承）");
        stateMachine.assertCanStart(ErpCsConstants.TICKET_STATUS_ASSIGNED); // 基线 + Delta 均放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanStart(ErpCsConstants.TICKET_STATUS_NEW),
                "Delta 未覆盖 start：start(NEW) 仍非法（基线继承）");
        // 终态分类不变（Delta 未覆盖 isTerminal）
        assertTrue(stateMachine.isTerminal(ErpCsConstants.TICKET_STATUS_CLOSED),
                "Delta 未覆盖 isTerminal：CLOSED 仍为终态");
    }
}
