package app.erp.qa.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 不符合项报告（{@code ErpQaNonConformance}，NCR）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status} 单轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/quality/state-machine.md}（§适用对象二：NCR 5 态 OPEN/IN_REVIEW/RESOLVED/
 * ESCALATED_TO_RECALL/CANCELLED）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 status 轴 6 动作迁移矩阵
 * （submitReview/resolve/upgradeToRecall/cancel/postNcr/reverseNcr）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p><b>postNcr/reverseNcr 自环</b>：postNcr/reverseNcr 仅在 status=RESOLVED 时操作 {@code posted} 标志（§3 posted 不入轴），
 * 不改变 status（自环 RESOLVED→RESOLVED）。Bean 只集中固定来源态守卫（须 RESOLVED），posted 判定 + 过账编排
 * （NcrPostingDispatcher/NcrReturnOrchestrator）保留在 Processor 原位（动态副作用，契约 §8）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 */
public class ErpQaNonConformanceStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /** submitReview 守卫：来源态为 {@code OPEN} 合法。 */
    public void assertCanSubmitReview(String status) {
        if (!ErpQaConstants.NCR_STATUS_OPEN.equals(status)) {
            throw illegal("submitReview", status, ErpQaConstants.NCR_STATUS_OPEN);
        }
    }

    /** resolve 守卫：来源态为 {@code IN_REVIEW} 合法（CAPA 闭环门控为动态业务守卫，保留在 Processor）。 */
    public void assertCanResolve(String status) {
        if (!ErpQaConstants.NCR_STATUS_IN_REVIEW.equals(status)) {
            throw illegal("resolve", status, ErpQaConstants.NCR_STATUS_IN_REVIEW);
        }
    }

    /** upgradeToRecall 守卫：来源态为 {@code IN_REVIEW} 合法（跨实体创建 Recall 为动态副作用，保留在 Processor）。 */
    public void assertCanUpgradeToRecall(String status) {
        if (!ErpQaConstants.NCR_STATUS_IN_REVIEW.equals(status)) {
            throw illegal("upgradeToRecall", status, ErpQaConstants.NCR_STATUS_IN_REVIEW);
        }
    }

    /** cancel 守卫：来源态为 {@code OPEN} 或 {@code IN_REVIEW} 合法（多来源态动作）。 */
    public void assertCanCancel(String status) {
        if (!ErpQaConstants.NCR_STATUS_OPEN.equals(status)
                && !ErpQaConstants.NCR_STATUS_IN_REVIEW.equals(status)) {
            throw illegal("cancel", status, ErpQaConstants.NCR_STATUS_OPEN + " 或 " + ErpQaConstants.NCR_STATUS_IN_REVIEW);
        }
    }

    /** postNcr 守卫：来源态为 {@code RESOLVED} 合法（posted 判定为动态守卫，保留在 Processor）。 */
    public void assertCanPostNcr(String status) {
        if (!ErpQaConstants.NCR_STATUS_RESOLVED.equals(status)) {
            throw illegal("postNcr", status, ErpQaConstants.NCR_STATUS_RESOLVED);
        }
    }

    /** reverseNcr 守卫：来源态为 {@code RESOLVED} 合法（posted 判定为动态守卫，保留在 Processor）。 */
    public void assertCanReverseNcr(String status) {
        if (!ErpQaConstants.NCR_STATUS_RESOLVED.equals(status)) {
            throw illegal("reverseNcr", status, ErpQaConstants.NCR_STATUS_RESOLVED);
        }
    }

    // ---------- 动作目标态（供 Processor/BizModel 写回） ----------

    public String submitReviewTargetStatus() {
        return ErpQaConstants.NCR_STATUS_IN_REVIEW;
    }

    public String resolveTargetStatus() {
        return ErpQaConstants.NCR_STATUS_RESOLVED;
    }

    public String upgradeToRecallTargetStatus() {
        return ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL;
    }

    public String cancelTargetStatus() {
        return ErpQaConstants.NCR_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定。NCR 终态为 RESOLVED/ESCALATED_TO_RECALL/CANCELLED（owner doc §适用对象二）。
     * RESOLVED 为「带过账操作的终态」——经 postNcr/reverseNcr 自环（操作 posted 标志），故 {@link #transitions()}
     * 中 RESOLVED 存在自环出边，不适用「终态无出边」的强可达性断言（见矩阵测试）。
     */
    public boolean isTerminal(String status) {
        return ErpQaConstants.NCR_STATUS_RESOLVED.equals(status)
                || ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL.equals(status)
                || ErpQaConstants.NCR_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submitReview", ErpQaConstants.NCR_STATUS_OPEN, ErpQaConstants.NCR_STATUS_IN_REVIEW),
                new TransitionDefinition("resolve", ErpQaConstants.NCR_STATUS_IN_REVIEW, ErpQaConstants.NCR_STATUS_RESOLVED),
                new TransitionDefinition("upgradeToRecall", ErpQaConstants.NCR_STATUS_IN_REVIEW, ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL),
                new TransitionDefinition("cancel", ErpQaConstants.NCR_STATUS_OPEN, ErpQaConstants.NCR_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpQaConstants.NCR_STATUS_IN_REVIEW, ErpQaConstants.NCR_STATUS_CANCELLED),
                new TransitionDefinition("postNcr", ErpQaConstants.NCR_STATUS_RESOLVED, ErpQaConstants.NCR_STATUS_RESOLVED),
                new TransitionDefinition("reverseNcr", ErpQaConstants.NCR_STATUS_RESOLVED, ErpQaConstants.NCR_STATUS_RESOLVED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpQaConstants.NCR_STATUS_RESOLVED,
                ErpQaConstants.NCR_STATUS_ESCALATED_TO_RECALL,
                ErpQaConstants.NCR_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpQaConstants.NCR_STATUS_OPEN));
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
