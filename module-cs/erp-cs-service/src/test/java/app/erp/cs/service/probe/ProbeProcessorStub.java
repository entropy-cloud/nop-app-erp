package app.erp.cs.service.probe;

import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;
import jakarta.inject.Inject;

/**
 * M0.1 契约探针：合成的 per-mutation Processor 桩（**不绑定任何真实业务实体**）。
 *
 * <p>证明 {@code docs/architecture/entity-state-machine-bean.md} 契约的 Processor 侧机制：
 * <ul>
 *   <li>**按类型注入** StateMachine Bean（{@code @Inject ErpProbeStateMachine}，非 private）；</li>
 *   <li>非法边由 Bean 报告（common 层 {@code illegal-status-transition}），Processor **保留并映射**领域 ErrorCode +
 *       实体编号/上下文参数（common 层错误码不抹平领域语义，见契约 §7）。</li>
 * </ul>
 *
 * <p><strong>测试作用域</strong>：仅用于 M0.1 机制探针。真实 Processor 由各迁移项落地。
 */
public class ProbeProcessorStub {

    /** 合成的领域 ErrorCode（测试作用域）。Processor 把 common 层非法迁移映射为此领域码 + 实体编号。 */
    static final ErrorCode ERR_PROBE_INVALID_TRANSITION = ErrorCode.define(
            "erp.err.probe.illegal-status-transition",
            "探针单据[{probeCode}]当前状态[{currentStatus}]不允许此操作",
            "probeCode", "currentStatus");

    /** 按类型注入 StateMachine Bean（非 private，Nop IoC 规则）。 */
    @Inject
    ErpProbeStateMachine stateMachine;

    /**
     * 合成的 {@code complete} 动作入口：调用矩阵守卫，非法边映射为领域 ErrorCode。
     *
     * @return 目标态（DONE）；非法边抛映射后的领域 {@link NopException}
     */
    public String complete(String probeCode, String currentStatus) {
        try {
            stateMachine.assertCanComplete(currentStatus);
        } catch (NopException e) {
            // Processor 保留领域 ErrorCode + 实体编号/上下文，common 层错误码不外泄
            throw new NopException(ERR_PROBE_INVALID_TRANSITION, e)
                    .param("probeCode", probeCode)
                    .param("currentStatus", currentStatus);
        }
        return stateMachine.completeTargetStatus();
    }

    ErpProbeStateMachine getStateMachine() {
        return stateMachine;
    }
}
