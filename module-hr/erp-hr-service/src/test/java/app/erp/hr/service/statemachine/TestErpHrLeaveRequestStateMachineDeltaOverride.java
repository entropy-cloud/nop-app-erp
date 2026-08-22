package app.erp.hr.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.autotest.NopTestProperty;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import io.nop.core.resource.store.VfsConfigLoader;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M2.11 Delta 覆盖运行时实证 —— **Delta 加载**侧（plan Phase 3）。
 *
 * <p>在真实 IoC 容器（hr-service 测试容器，经 VFS Delta 层 {@code test-hr-delta} 以同名 bean id 覆盖基线为
 * {@link ErpHrLeaveRequestStateMachineDelta}）下证明：
 * <ol>
 *   <li>容器解析的 Bean 为 Delta 派生类（经真实 bean 解析注入生效，非静态检查/非编译派生类）；</li>
 *   <li>Delta 覆盖的语义按 Delta 生效：{@code assertCanCancel(SUBMITTED)} 放行（基线抛异常 → Delta 放开）；</li>
 *   <li>非覆盖动作继承基线不变：{@code assertCanCancel(APPROVED)} 仍放行、{@code assertCanSubmit} 语义不变。</li>
 * </ol>
 *
 * <p>与 {@link TestErpHrLeaveRequestStateMachineBaselineIoC} 对照：同一 {@code assertCanCancel(SUBMITTED)}
 * 在基线抛异常、在 Delta 放行 —— 构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 *
 * <p>Delta 层经 {@code @NopTestProperty} 设置 {@code nop.core.vfs.delta-layer-ids=test-hr-delta} 激活，
 * Delta 层文件 {@code _vfs/_delta/test-hr-delta/erp/hr/beans/app-service.beans.xml} 以同名 bean id 覆盖基线。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.core.vfs.delta-layer-ids", value = "default,test-hr-delta")
public class TestErpHrLeaveRequestStateMachineDeltaOverride extends JunitAutoTestCase {

    /**
     * VFS Delta 层隔离兜底（框架静态缓存补丁）。
     *
     * <p>{@code VfsConfigLoader._default} 是静态单例，一旦在某个 surefire fork 中以 {@code test-hr-delta}
     * 激活并被缓存，同一 fork 后续测试类会继承该 delta（{@link io.nop.core.resource.store.VfsConfigLoader}
     * 不在测试类之间清理缓存）。delta 层对 {@code app-service.beans.xml} 做<strong>文件级替换</strong>而非 XDsl 合并，
     * 会丢失基线直接 bean（如 {@code ErpHrReportBizModel}），污染同 fork 后续测试（如
     * {@code TestErpHrReportRendering} 经 {@code @Inject ErpHrReportBizModel} 找不到 bean）。
     *
     * <p>本类在 {@code @BeforeAll}/{@code @AfterAll} 显式置空 {@code _default}，强制下一测试类重建 VFS 配置，
     * 使 Delta 隔离不依赖 surefire fork 调度（多核机器 fork 分布偶然把本类与敏感测试分到同 fork）。
     */
    @BeforeAll
    static void clearVfsConfigCacheBefore() {
        VfsConfigLoader.registerDefault(null);
    }

    @AfterAll
    static void clearVfsConfigCacheAfter() {
        VfsConfigLoader.registerDefault(null);
    }

    @Inject
    ErpHrLeaveRequestStateMachine stateMachine;

    @Test
    public void testDeltaBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpHrLeaveRequestStateMachineDelta,
                "Delta 加载：容器应解析为 ErpHrLeaveRequestStateMachineDelta 派生类（同名 bean id 覆盖生效）");
        assertEquals(ErpHrLeaveRequestStateMachineDelta.class, stateMachine.getClass(),
                "Delta 加载：实例类 = ErpHrLeaveRequestStateMachineDelta（非基线类）");
    }

    @Test
    public void testDeltaOverriddenEdgeDiffersFromBaseline() {
        // Delta 覆盖语义：cancel(SUBMITTED) 放行（基线抛异常 → Delta 放开，可区分差异）
        stateMachine.assertCanCancel(ErpHrConstants.LEAVE_STATUS_SUBMITTED);

        // Delta 仍允许 APPROVED（基线 + Delta 均允许）
        stateMachine.assertCanCancel(ErpHrConstants.LEAVE_STATUS_APPROVED);

        // Delta 收紧后仍拒绝其余态
        NopException ex = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpHrConstants.LEAVE_STATUS_DRAFT));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Delta cancel(DRAFT) 仍报告 common 层非法迁移码");
    }

    @Test
    public void testNonOverriddenActionsInheritBaseline() {
        // Delta 未覆盖的动作继承基线矩阵不变
        stateMachine.assertCanSubmit(ErpHrConstants.LEAVE_STATUS_DRAFT); // 基线 + Delta 均放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanSubmit(ErpHrConstants.LEAVE_STATUS_SUBMITTED),
                "Delta 未覆盖 submit：submit(SUBMITTED) 仍非法（基线继承）");
        stateMachine.assertCanApprove(ErpHrConstants.LEAVE_STATUS_SUBMITTED); // 基线 + Delta 均放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanApprove(ErpHrConstants.LEAVE_STATUS_APPROVED),
                "Delta 未覆盖 approve：approve(APPROVED) 仍非法（基线继承）");
        // 终态分类不变（Delta 未覆盖 isTerminal）
        assertTrue(stateMachine.isTerminal(ErpHrConstants.LEAVE_STATUS_CANCELLED),
                "Delta 未覆盖 isTerminal：CANCELLED 仍为终态");
    }
}
