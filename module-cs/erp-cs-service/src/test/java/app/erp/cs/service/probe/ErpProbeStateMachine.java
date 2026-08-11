package app.erp.cs.service.probe;

import io.nop.api.core.exceptions.ErrorCode;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * M0.1 契约探针：合成的 {@code ErpXxxStateMachine} 形状 Bean（**不绑定任何真实业务实体**）。
 *
 * <p>证明 {@code docs/architecture/entity-state-machine-bean.md} 契约的机制可行性：
 * <ul>
 *   <li>无状态（不注入 DAO/IBiz/IServiceContext），只接收状态值；</li>
 *   <li>显式动作方法（主路径）+ 只读 {@code transitions()} 元数据（完备性分析用）；</li>
 *   <li>非法边抛通用 {@code illegal-status-transition}（common 层），领域 ErrorCode 映射归 {@link ProbeProcessorStub}。</li>
 * </ul>
 *
 * <p>状态轴（合成）：{@code DRAFT -> SUBMITTED -> DONE(终态)}。动作：{@code submit}、{@code complete}。
 *
 * <p><strong>测试作用域</strong>：仅用于 M0.1 机制探针，不引入生产代码或生产 beans 条目。
 * 真实 {@code ErpCsTicketStateMachine} 由 M1.1 落地。
 */
public class ErpProbeStateMachine {

    public static final String STATUS_DRAFT = "DRAFT";
    public static final String STATUS_SUBMITTED = "SUBMITTED";
    public static final String STATUS_DONE = "DONE";

    /** common 层通用非法迁移错误码（合成，测试作用域）。领域 ErrorCode 映射在 Processor 桩。 */
    static final ErrorCode ILLEGAL_STATUS_TRANSITION = ErrorCode.define(
            "illegal-status-transition",
            "非法状态迁移: action={action}, fromStatus={fromStatus}",
            "action", "fromStatus");

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanSubmit(String status) {
        if (!STATUS_DRAFT.equals(status)) {
            throw illegal("submit", status);
        }
    }

    public String submitTargetStatus() {
        return STATUS_SUBMITTED;
    }

    public void assertCanComplete(String status) {
        if (!STATUS_SUBMITTED.equals(status)) {
            throw illegal("complete", status);
        }
    }

    public String completeTargetStatus() {
        return STATUS_DONE;
    }

    public boolean isTerminal(String status) {
        return STATUS_DONE.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submit", STATUS_DRAFT, STATUS_SUBMITTED),
                new TransitionDefinition("complete", STATUS_SUBMITTED, STATUS_DONE)));
    }

    public List<String> terminalStatuses() {
        return Collections.singletonList(STATUS_DONE);
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(STATUS_DRAFT);
    }

    // ---------- 内部 ----------

    private static NopException illegal(String action, String fromStatus) {
        return new NopException(ILLEGAL_STATUS_TRANSITION)
                .param("action", action)
                .param("fromStatus", fromStatus);
    }

    /** 合成的迁移定义记录（只读元数据形状，供 M5.1/M5.2 守卫消费）。 */
    public static final class TransitionDefinition {
        private final String action;
        private final String fromStatus;
        private final String toStatus;

        TransitionDefinition(String action, String fromStatus, String toStatus) {
            this.action = action;
            this.fromStatus = fromStatus;
            this.toStatus = toStatus;
        }

        public String getAction() {
            return action;
        }

        public String getFromStatus() {
            return fromStatus;
        }

        public String getToStatus() {
            return toStatus;
        }
    }
}
