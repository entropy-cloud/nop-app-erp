package app.erp.mfg.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mfg.service.ErpMfgConstants;
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
 * Delta 覆盖运行时实证 —— **Delta 加载**侧（plan Phase 3；契约 §6）。
 *
 * <p>在真实 IoC 容器（mfg-service 测试容器，经 VFS Delta 层 {@code test-mfg-delta} 以同名 bean id 覆盖基线为
 * {@link ErpMfgForecastStateMachineDelta}）下证明：
 * <ol>
 *   <li>容器解析的 Bean 为 Delta 派生类（经真实 bean 解析注入生效，非静态检查/非编译派生类）；</li>
 *   <li>Delta 覆盖的语义按 Delta 生效：{@code assertCanCancel(APPROVED)} 抛异常（基线放行）；</li>
 *   <li>非覆盖动作继承基线不变：{@code assertCanCancel(DRAFT)} 仍放行、{@code assertCanApprove(DRAFT)} 仍放行、
 *       {@code assertCanApprove(APPROVED)} 仍非法。</li>
 * </ol>
 *
 * <p>与 {@link TestErpMfgForecastStateMachineBaselineIoC} 对照：同一 {@code assertCanCancel(APPROVED)}
 * 在基线放行、在 Delta 抛异常 —— 构成可区分的基线/Delta 双加载证据（契约 §6 业务级 Delta 实证义务）。
 *
 * <p>Delta 层经 {@code @NopTestProperty} 设置 {@code nop.core.vfs.delta-layer-ids=test-mfg-delta} 激活，
 * Delta 层文件 {@code _vfs/_delta/test-mfg-delta/erp/mfg/beans/app-service.beans.xml} 以同名 bean id 覆盖基线。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE)
@NopTestProperty(name = "nop.core.vfs.delta-layer-ids", value = "test-mfg-delta")
public class TestErpMfgForecastStateMachineDeltaOverride extends JunitAutoTestCase {

    /**
     * VFS Delta 层隔离兜底（框架静态缓存补丁，对齐 hr 域 plan 2026-08-12-1118-3 范式）。
     *
     * <p>{@code VfsConfigLoader._default} 是静态单例，一旦在某个 surefire fork 中以 {@code test-mfg-delta}
     * 激活并被缓存，同一 fork 后续测试类会继承该 delta（{@link VfsConfigLoader} 不在测试类之间清理缓存）。
     * delta 层对 {@code app-service.beans.xml} 做<strong>文件级替换</strong>而非 XDsl 合并，会丢失基线直接 bean
     * （如 subcontract processor），污染同 fork 后续 mfg 跨模块测试。
     *
     * <p>本类在 {@code @BeforeAll}/{@code @AfterAll} 显式置空 {@code _default}，强制下一测试类重建 VFS 配置，
     * 使 Delta 隔离不依赖 surefire fork 调度（mfg 域 ~20 个 {@code TestErpMfg*} 排在本类之后，单 fork 下必然受污染）。
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
    ErpMfgForecastStateMachine stateMachine;

    @Test
    public void testDeltaBeanResolvedFromRealContainer() {
        assertTrue(stateMachine instanceof ErpMfgForecastStateMachineDelta,
                "Delta 加载：容器应解析为 ErpMfgForecastStateMachineDelta 派生类（同名 bean id 覆盖生效）");
        assertEquals(ErpMfgForecastStateMachineDelta.class, stateMachine.getClass(),
                "Delta 加载：实例类 = ErpMfgForecastStateMachineDelta（非基线类）");
    }

    @Test
    public void testDeltaOverriddenEdgeDiffersFromBaseline() {
        // Delta 覆盖语义：cancel(APPROVED) 抛异常（基线放行 → Delta 收紧，可区分差异）
        NopException ex = assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpMfgConstants.FORECAST_STATUS_APPROVED),
                "Delta cancel(APPROVED) 应非法（Delta 收紧为仅 DRAFT）");
        assertEquals(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION.getErrorCode(), ex.getErrorCode(),
                "Delta cancel(APPROVED) 报告 common 层非法迁移码");

        // Delta 仍允许 DRAFT（基线 + Delta 均允许）
        stateMachine.assertCanCancel(ErpMfgConstants.FORECAST_STATUS_DRAFT);

        // Delta 收紧后 APPROVED 与死状态 CONSUMED 一并非法
        assertThrows(NopException.class,
                () -> stateMachine.assertCanCancel(ErpMfgConstants.FORECAST_STATUS_CONSUMED),
                "Delta cancel(CONSUMED) 仍非法");
    }

    @Test
    public void testNonOverriddenActionsInheritBaseline() {
        // Delta 未覆盖的动作继承基线矩阵不变
        stateMachine.assertCanApprove(ErpMfgConstants.FORECAST_STATUS_DRAFT); // 基线 + Delta 均放行
        assertThrows(NopException.class,
                () -> stateMachine.assertCanApprove(ErpMfgConstants.FORECAST_STATUS_APPROVED),
                "Delta 未覆盖 approve：approve(APPROVED) 仍非法（基线继承）");
        // 终态分类不变（Delta 未覆盖 isTerminal）
        assertTrue(stateMachine.isTerminal(ErpMfgConstants.FORECAST_STATUS_CANCELLED),
                "Delta 未覆盖 isTerminal：CANCELLED 仍为终态");
    }
}
