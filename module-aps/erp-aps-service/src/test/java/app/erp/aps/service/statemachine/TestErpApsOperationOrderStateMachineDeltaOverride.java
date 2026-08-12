package app.erp.aps.service.statemachine;

import app.erp.aps.service.ErpApsConstants;
import app.erp.common.service.ErpCommonErrors;
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
 * M2.13 Delta 覆盖运行时实证 —— **Delta 加载**侧（plan Phase 3）。
 *
 * <p>在真实 IoC 容器（aps-service 测试容器，经 VFS Delta 层 {@code test-aps-delta} 以同名 bean id 覆盖基线为
 * {@link ErpApsOperationOrderStateMachineDelta}）下证明：
 * <ol>
 *   <li>容器解析的 Bean 为 Delta 派生类（经真实 bean 解析注入生效，非静态检查/非编译派生类）；</li>
 *   <li>Delta 覆盖的语义按 Delta 生效：{@code assertCanCancel(IN_PROGRESS)} 抛异常（基线放行 → Delta 收紧）；</li>
 *   <li>非覆盖动作继承基线不变：{@code assertCanCancel(DRAFT/PLANNED)} 仍放行、{@code assertCanStart} 语义不变。</li>
 * </ol>
 *
 * <p>与 {@link TestErpApsOperationOrderStateMachineBaselineIoC} 对照：同一 {@code assertCanCancel(IN_PROGRESS)}
 * 在基线放行、在 Delta 抛异常 —— 构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 *
 * <p>Delta 层经 {@code @NopTestProperty} 设置 {@code nop.core.vfs.delta-layer-ids=test-aps-delta} 激活，
 * Delta 层文件 {@code _vfs/_delta/test-aps-delta/erp/aps/beans/app-service.beans.xml} 以同名 bean id 覆盖基线。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.core.vfs.delta-layer-ids", value = "test-aps-delta")
public class TestErpApsOperationOrderStateMachineDeltaOverride extends JunitAutoTestCase {

    /**
     * VFS Delta 层隔离兜底（框架静态缓存补丁，对齐 hr/mfg 域 plan 2026-08-12-1118-3/1841-3 范式）。
     *
     * <p>{@code VfsConfigLoader._default} 是静态单例，一旦在某个 surefire fork 中以 {@code test-aps-delta}
     * 激活并被缓存，同一 fork 后续测试类会继承该 delta（{@link VfsConfigLoader} 不在测试类之间清理缓存）。
     * delta 层对 {@code app-service.beans.xml} 做<strong>文件级替换</strong>而非 XDsl 合并，会丢失基线直接 bean
     * （如 Scheduling Processors / AtpCtpService / LoadSourceProvider），污染同 fork 后续 aps 跨模块测试
     * （如 {@code TestErpApsOperationOrderStateGuards} 期望 cancel(IN_PROGRESS) 放行，delta 下会抛异常）。
     *
     * <p>本类在 {@code @BeforeAll}/{@code @AfterAll} 显式置空 {@code _default}，强制下一测试类重建 VFS 配置，
     * 使 Delta 隔离不依赖 surefire fork 调度。
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
    ErpApsOperationOrderStateMachine stateMachine;

    @Test
    public void testDeltaBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpApsOperationOrderStateMachineDelta,
                "Delta 加载：容器应解析为 ErpApsOperationOrderStateMachineDelta 派生类（同名 bean id 覆盖生效）");
        assertEquals(ErpApsOperationOrderStateMachineDelta.class, stateMachine.getClass(),
                "Delta 加载：实例类 = ErpApsOperationOrderStateMachineDelta（非基线类）");
    }

    @Test
    public void testDeltaOverriddenEdgeDiffersFromBaseline() {
        // Delta 覆盖语义：cancel(IN_PROGRESS) 抛异常（基线放行 → Delta 收紧，可区分差异）
        NopException ex = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpApsConstants.OP_STATUS_IN_PROGRESS));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Delta cancel(IN_PROGRESS) 报告 common 层非法迁移码（Delta 收紧为仅 DRAFT/PLANNED）");

        // Delta 仍允许 DRAFT + PLANNED（基线 + Delta 均允许）
        stateMachine.assertCanCancel(ErpApsConstants.OP_STATUS_DRAFT);
        stateMachine.assertCanCancel(ErpApsConstants.OP_STATUS_PLANNED);
    }

    @Test
    public void testNonOverriddenActionsInheritBaseline() {
        // Delta 未覆盖的动作继承基线矩阵不变
        stateMachine.assertCanStart(ErpApsConstants.OP_STATUS_PLANNED); // 基线 + Delta 均放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanStart(ErpApsConstants.OP_STATUS_DRAFT),
                "Delta 未覆盖 start：start(DRAFT) 仍非法（基线继承）");
        stateMachine.assertCanComplete(ErpApsConstants.OP_STATUS_IN_PROGRESS); // 基线 + Delta 均放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanComplete(ErpApsConstants.OP_STATUS_PLANNED),
                "Delta 未覆盖 complete：complete(PLANNED) 仍非法（基线继承）");
        stateMachine.assertCanRevertToDraft(ErpApsConstants.OP_STATUS_PLANNED); // 基线 + Delta 均放行
        // 终态分类不变（Delta 未覆盖 isTerminal）
        assertTrue(stateMachine.isTerminal(ErpApsConstants.OP_STATUS_FINISHED),
                "Delta 未覆盖 isTerminal：FINISHED 仍为终态");
    }
}
