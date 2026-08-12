package app.erp.crm.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.crm.service.ErpCrmConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 活动/事件（{@code ErpCrmEvent}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/crm/state-machine.md §Event}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载**已实现**迁移矩阵
 * （PLANNED/COMPLETED/CANCELLED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。为 crm 域首例 StateMachine Bean（建立域内范式）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 *
 * <p>迁移矩阵（2 条边，仅编码命名动作路径下**已落地**的 writer）：
 * <ul>
 *   <li>complete(PLANNED→COMPLETED)；</li>
 *   <li>cancel(PLANNED→CANCELLED)。</li>
 * </ul>
 *
 * <p>初始态 PLANNED 由创建路径（sequence-progress Processors + BizModel 新建 Event）写入，非迁移（M0.1 §9.2 选项 c）。
 * owner doc §Event 无 §实现约定 段、无 stage/asynchrony/config-gated 例外——§迁移表为唯一无歧义来源
 * （§11.4「§迁移表 vs §实现约定 内部漂移」警示对 Event 不适用）。
 */
public class ErpCrmEventStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanComplete(String status) {
        if (!ErpCrmConstants.EVENT_STATUS_PLANNED.equals(status)) {
            throw illegal("complete", status, ErpCrmConstants.EVENT_STATUS_PLANNED);
        }
    }

    public String completeTargetStatus() {
        return ErpCrmConstants.EVENT_STATUS_COMPLETED;
    }

    public void assertCanCancel(String status) {
        if (!ErpCrmConstants.EVENT_STATUS_PLANNED.equals(status)) {
            throw illegal("cancel", status, ErpCrmConstants.EVENT_STATUS_PLANNED);
        }
    }

    public String cancelTargetStatus() {
        return ErpCrmConstants.EVENT_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类：COMPLETED/CANCELLED。
     *
     * <p>对齐 owner doc {@code crm/state-machine.md §Event}「终态不可恢复。若需重新安排，新建 Event」。
     */
    public boolean isTerminal(String status) {
        return ErpCrmConstants.EVENT_STATUS_COMPLETED.equals(status)
                || ErpCrmConstants.EVENT_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("complete", ErpCrmConstants.EVENT_STATUS_PLANNED, ErpCrmConstants.EVENT_STATUS_COMPLETED),
                new TransitionDefinition("cancel", ErpCrmConstants.EVENT_STATUS_PLANNED, ErpCrmConstants.EVENT_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpCrmConstants.EVENT_STATUS_COMPLETED, ErpCrmConstants.EVENT_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpCrmConstants.EVENT_STATUS_PLANNED);
    }

    // ---------- 内部 ----------

    private static NopException illegal(String action, String currentStatus, String expectedStatus) {
        return new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCommonErrors.ARG_CURRENT_STATUS, currentStatus)
                .param(ErpCommonErrors.ARG_EXPECTED_STATUS, expectedStatus)
                .param(ARG_ACTION, action);
    }

    /** 只读迁移定义记录（供 M5.1/M5.2 可达性/完备性分析与文档一致性校验消费）。 */
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
