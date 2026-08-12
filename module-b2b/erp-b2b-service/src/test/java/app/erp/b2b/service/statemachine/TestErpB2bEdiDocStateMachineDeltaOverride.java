package app.erp.b2b.service.statemachine;

import app.erp.b2b.service.ErpB2bConstants;
import app.erp.common.service.ErpCommonErrors;
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
 * Delta 覆盖运行时实证 —— **Delta 加载**侧（plan Phase 3）。
 *
 * <p>在真实 IoC 容器（b2b-service 测试容器，经 VFS Delta 层 {@code test-b2b-delta} 以同名 bean id 覆盖基线为
 * {@link ErpB2bEdiDocStateMachineDelta}）下证明：
 * <ol>
 *   <li>容器解析的 Bean 为 Delta 派生类（经真实 bean 解析注入生效，非静态检查/非编译派生类）；</li>
 *   <li>Delta 覆盖的语义按 Delta 生效：{@code assertCanCancel(SENT)} / {@code assertCanCancel(ERROR)} 抛异常（基线放行）；</li>
 *   <li>非覆盖动作继承基线不变：{@code assertCanCancel(TO_SEND)} 仍放行、{@code assertCanMarkSent(TO_SEND)} 仍放行。</li>
 * </ol>
 *
 * <p>与 {@link TestErpB2bEdiDocStateMachineBaselineIoC} 对照：同一 {@code assertCanCancel(SENT)}
 * 在基线放行、在 Delta 抛异常 —— 构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 *
 * <p>Delta 层经 {@code @NopTestProperty} 设置 {@code nop.core.vfs.delta-layer-ids=test-b2b-delta} 激活，
 * Delta 层文件 {@code _vfs/_delta/test-b2b-delta/erp/b2b/beans/app-service.beans.xml} 以同名 bean id 覆盖基线。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.core.vfs.delta-layer-ids", value = "test-b2b-delta")
public class TestErpB2bEdiDocStateMachineDeltaOverride extends JunitAutoTestCase {

    @Inject
    ErpB2bEdiDocStateMachine stateMachine;

    @Test
    public void testDeltaBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpB2bEdiDocStateMachineDelta,
                "Delta 加载：容器应解析为 ErpB2bEdiDocStateMachineDelta 派生类（同名 bean id 覆盖生效）");
        assertEquals(ErpB2bEdiDocStateMachineDelta.class, stateMachine.getClass(),
                "Delta 加载：实例类 = ErpB2bEdiDocStateMachineDelta（非基线类）");
    }

    @Test
    public void testDeltaOverriddenEdgeDiffersFromBaseline() {
        // Delta 覆盖语义：cancel 仅允许 TO_SEND（基线允许 TO_SEND/SENT/ERROR，Delta 收紧）
        stateMachine.assertCanCancel(ErpB2bConstants.EDI_DOC_STATE_TO_SEND); // Delta 放行，不抛

        // 关键差异点：cancel(SENT) 和 cancel(ERROR) 在 Delta 非法（基线放行）
        NopException exSent = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpB2bConstants.EDI_DOC_STATE_SENT));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), exSent.getErrorCode(),
                "Delta cancel(SENT) 报告 common 层非法迁移码");

        NopException exError = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpB2bConstants.EDI_DOC_STATE_ERROR));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), exError.getErrorCode(),
                "Delta cancel(ERROR) 报告 common 层非法迁移码");
    }

    @Test
    public void testNonOverriddenActionsInheritBaseline() {
        // Delta 未覆盖的动作继承基线矩阵不变
        stateMachine.assertCanMarkSent(ErpB2bConstants.EDI_DOC_STATE_TO_SEND); // 基线 + Delta 均放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanMarkSent(ErpB2bConstants.EDI_DOC_STATE_SENT),
                "Delta 未覆盖 markSent：markSent(SENT) 仍非法（基线继承）");
        stateMachine.assertCanArchive(ErpB2bConstants.EDI_DOC_STATE_RECEIVED); // 基线 + Delta 均放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanArchive(ErpB2bConstants.EDI_DOC_STATE_ACKNOWLEDGED),
                "Delta 未覆盖 archive：archive(ACKNOWLEDGED) 仍非法（基线继承）");
        // 终态分类不变（Delta 未覆盖 isTerminal）
        assertTrue(stateMachine.isTerminal(ErpB2bConstants.EDI_DOC_STATE_CANCELLED),
                "Delta 未覆盖 isTerminal：CANCELLED 仍为终态");
    }
}
