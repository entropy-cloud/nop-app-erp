package app.erp.cs.service.probe;

import io.nop.api.core.annotations.autotest.NopTestConfig;
import io.nop.api.core.annotations.core.OptionalBoolean;
import io.nop.api.core.exceptions.NopException;
import io.nop.autotest.junit.JunitAutoTestCase;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * M0.1 实体级 StateMachine Bean 契约探针测试（plan 2026-08-12-0617-1 Phase 2）。
 *
 * <p>以**合成的、不绑定任何真实业务实体**的 {@link ErpProbeStateMachine} + {@link ProbeProcessorStub}，
 * 在**真实 IoC 容器**（cs-service 测试容器，即 M1.1 试点目标域容器）下证明
 * {@code docs/architecture/entity-state-machine-bean.md} 契约的机制可行性：
 *
 * <ol>
 *   <li>Bean 可解析（IoC 注册生效）；</li>
 *   <li>Processor 桩**按类型注入** Bean 成功；</li>
 *   <li>非法边经 {@code assertCanComplete} 报告（common 层 {@code illegal-status-transition}），
 *       Processor 桩映射为领域 ErrorCode（{@code erp.err.probe.illegal-status-transition}）+ 实体编号，
 *       common 层错误码作 cause 保留、不外泄抹平领域语义。</li>
 * </ol>
 *
 * <p><strong>明确不断言 Delta 覆盖</strong>——业务级 Delta 同名 Bean 覆盖运行时实证归 M1.2（路线图 Non-Goal + 契约 §6）。
 *
 * <p>测试作用域：零生产代码/生产 beans 变更（仅 {@code src/test/} + 测试 {@code test-probe-statemachine.beans.xml}）。
 */
@NopTestConfig(localDb = true,
        initDatabaseSchema = OptionalBoolean.TRUE,
        enableActionAuth = OptionalBoolean.FALSE,
        testBeansFile = "/erp/cs/beans/test-probe-statemachine.beans.xml")
public class TestErpProbeStateMachineContract extends JunitAutoTestCase {

    @Inject
    ErpProbeStateMachine stateMachine;
    @Inject
    ProbeProcessorStub processor;

    // ---------- (1) Bean 可解析：IoC 注册生效 ----------

    @Test
    public void testBeanResolvableFromRealContainer() {
        assertNotNull(stateMachine, "合成 StateMachine Bean 应由真实 IoC 容器解析注入");
    }

    // ---------- (2) Processor 桩按类型注入 Bean 成功 ----------

    @Test
    public void testProcessorInjectsStateMachineByType() {
        assertNotNull(processor, "Processor 桩应由真实 IoC 容器解析注入");
        assertNotNull(processor.getStateMachine(), "Processor 桩按类型注入 StateMachine Bean 成功");
        assertTrue(processor.getStateMachine() instanceof ErpProbeStateMachine,
                "注入的是 ErpProbeStateMachine 类型实例");
    }

    // ---------- (3a) 合法边：矩阵守卫放行 + 目标态写回 ----------

    @Test
    public void testLegalEdgeAdvancesToTargetStatus() {
        String target = processor.complete("PROBE-OK", ErpProbeStateMachine.STATUS_SUBMITTED);
        assertEquals(ErpProbeStateMachine.STATUS_DONE, target, "SUBMITTED 合法边 → DONE");
    }

    // ---------- (3b) 非法边：Bean 报告 common 层码 + Processor 映射领域 ErrorCode ----------

    @Test
    public void testIllegalEdgeBeanReportsCommonCode() {
        // Bean 自身：非法来源态（DONE 终态无出边）直接抛 common 层 illegal-status-transition
        NopException beanEx = assertThrows(NopException.class,
                () -> stateMachine.assertCanComplete(ErpProbeStateMachine.STATUS_DONE));
        assertEquals(ErpProbeStateMachine.ILLEGAL_STATUS_TRANSITION.getErrorCode(), beanEx.getErrorCode(),
                "Bean 报告 common 层非法迁移码");
        assertEquals("complete", beanEx.getParam("action"), "拒绝元数据携带动作名");
        assertEquals(ErpProbeStateMachine.STATUS_DONE, beanEx.getParam("fromStatus"), "拒绝元数据携带来源态");
    }

    @Test
    public void testIllegalEdgeProcessorMapsToDomainErrorCode() {
        // Processor 桩：把 common 报告映射为领域 ErrorCode + 实体编号，common 码作 cause 保留
        NopException domainEx = assertThrows(NopException.class,
                () -> processor.complete("PROBE-BAD", ErpProbeStateMachine.STATUS_DONE));

        assertEquals(ProbeProcessorStub.ERR_PROBE_INVALID_TRANSITION.getErrorCode(), domainEx.getErrorCode(),
                "Processor 映射为领域 ErrorCode（非 common 层码）");
        assertEquals("PROBE-BAD", domainEx.getParam("probeCode"), "Processor 保留实体编号上下文");
        assertEquals(ErpProbeStateMachine.STATUS_DONE, domainEx.getParam("currentStatus"),
                "Processor 保留当前态上下文");

        // common 层报告作为 cause 保留（不丢失诊断信息），但不外泄为对外错误码
        Throwable cause = domainEx.getCause();
        assertTrue(cause instanceof NopException, "common 层非法迁移报告作为 cause 保留");
        assertEquals(ErpProbeStateMachine.ILLEGAL_STATUS_TRANSITION.getErrorCode(), ((NopException) cause).getErrorCode(),
                "cause 仍是 common 层 illegal-status-transition");
    }

    // ---------- 矩阵元数据完整性（只读元数据接口形状，非 Processor 主路径） ----------

    @Test
    public void testTransitionMetadataShape() {
        List<ErpProbeStateMachine.TransitionDefinition> t = stateMachine.transitions();
        assertEquals(2, t.size(), "两条迁移边：submit、complete");
        assertEquals("submit", t.get(0).getAction());
        assertEquals(ErpProbeStateMachine.STATUS_DRAFT, t.get(0).getFromStatus());
        assertEquals(ErpProbeStateMachine.STATUS_SUBMITTED, t.get(0).getToStatus());
        assertEquals("complete", t.get(1).getAction());

        assertEquals(1, stateMachine.terminalStatuses().size(), "唯一终态 DONE");
        assertEquals(ErpProbeStateMachine.STATUS_DONE, stateMachine.terminalStatuses().get(0));
        assertEquals(ErpProbeStateMachine.STATUS_DRAFT, stateMachine.initialStatuses().get(0), "初始态 DRAFT");
        assertTrue(stateMachine.isTerminal(ErpProbeStateMachine.STATUS_DONE), "DONE 为终态");
    }
}
