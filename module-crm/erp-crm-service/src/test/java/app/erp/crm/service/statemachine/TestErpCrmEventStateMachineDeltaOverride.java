package app.erp.crm.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.crm.service.ErpCrmConstants;
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
 * M2.2 Delta 覆盖运行时实证 —— **Delta 加载**侧（plan Phase 3）。
 *
 * <p>在真实 IoC 容器（crm-service 测试容器，经 VFS Delta 层 {@code test-crm-delta} 以同名 bean id 覆盖基线为
 * {@link ErpCrmEventStateMachineDelta}）下证明：
 * <ol>
 *   <li>容器解析的 Bean 为 Delta 派生类（经真实 bean 解析注入生效，非静态检查/非编译派生类）；</li>
 *   <li>Delta 覆盖的语义按 Delta 生效：{@code assertCanCancel(COMPLETED)} 放行（基线抛异常 → Delta 放宽「soft void」）；</li>
 *   <li>非覆盖动作继承基线不变：{@code assertCanCancel(PLANNED)} 仍放行、{@code assertCanComplete} 语义不变、
 *       {@code assertCanCancel(CANCELLED)} 仍非法（Delta 未放开 CANCELLED 源）。</li>
 * </ol>
 *
 * <p>与 {@link TestErpCrmEventStateMachineBaselineIoC} 对照：同一 {@code assertCanCancel(COMPLETED)}
 * 在基线抛异常、在 Delta 放行 —— 构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 *
 * <p>Delta 层经 {@code @NopTestProperty} 设置 {@code nop.core.vfs.delta-layer-ids=test-crm-delta} 激活，
 * Delta 层文件 {@code _vfs/_delta/test-crm-delta/erp/crm/beans/app-service.beans.xml} 以同名 bean id 覆盖基线。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.core.vfs.delta-layer-ids", value = "test-crm-delta")
public class TestErpCrmEventStateMachineDeltaOverride extends JunitAutoTestCase {

    /**
     * VFS Delta 层隔离兜底（框架静态缓存补丁，镜像 hr 域 M2.11 已落地的范式）。
     *
     * <p>{@code VfsConfigLoader._default} 是静态单例，一旦在某个 surefire fork 中以 {@code test-crm-delta}
     * 激活并被缓存，同一 fork 后续测试类会继承该 delta（{@link VfsConfigLoader} 不在测试类之间清理缓存）。
     * delta 层对 {@code app-service.beans.xml} 做<strong>文件级替换</strong>而非 XDsl 合并，会丢失基线直接 bean
     * （如 {@code ErpCrmReportBizModel} 经 {@code ioc:type="@bean:id"} 注册），污染同 fork 后续测试。
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
    ErpCrmEventStateMachine stateMachine;

    @Test
    public void testDeltaBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpCrmEventStateMachineDelta,
                "Delta 加载：容器应解析为 ErpCrmEventStateMachineDelta 派生类（同名 bean id 覆盖生效）");
        assertEquals(ErpCrmEventStateMachineDelta.class, stateMachine.getClass(),
                "Delta 加载：实例类 = ErpCrmEventStateMachineDelta（非基线类）");
    }

    @Test
    public void testDeltaOverriddenEdgeDiffersFromBaseline() {
        // Delta 覆盖语义：cancel(COMPLETED) 放行（基线抛异常 → Delta 放宽「soft void」，可区分差异）
        stateMachine.assertCanCancel(ErpCrmConstants.EVENT_STATUS_COMPLETED); // 放行

        // Delta 仍允许 PLANNED（基线 + Delta 均允许）
        stateMachine.assertCanCancel(ErpCrmConstants.EVENT_STATUS_PLANNED);

        // Delta 未放开 CANCELLED 源（终态取消仍非法）
        NopException ex = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpCrmConstants.EVENT_STATUS_CANCELLED));
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Delta cancel(CANCELLED) 仍报告 common 层非法迁移码（Delta 未放开 CANCELLED 源）");
    }

    @Test
    public void testNonOverriddenActionsInheritBaseline() {
        // Delta 未覆盖的动作继承基线矩阵不变
        stateMachine.assertCanComplete(ErpCrmConstants.EVENT_STATUS_PLANNED); // 基线 + Delta 均放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanComplete(ErpCrmConstants.EVENT_STATUS_COMPLETED),
                "Delta 未覆盖 complete：complete(COMPLETED) 仍非法（基线继承）");
        assertThrows(NopException.class,
                () -> stateMachine.assertCanComplete(ErpCrmConstants.EVENT_STATUS_CANCELLED),
                "Delta 未覆盖 complete：complete(CANCELLED) 仍非法（基线继承）");
        // 终态分类不变（Delta 未覆盖 isTerminal）
        assertTrue(stateMachine.isTerminal(ErpCrmConstants.EVENT_STATUS_COMPLETED),
                "Delta 未覆盖 isTerminal：COMPLETED 仍为终态");
    }
}
