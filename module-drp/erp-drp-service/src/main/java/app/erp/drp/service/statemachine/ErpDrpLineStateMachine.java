package app.erp.drp.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.drp.service.ErpDrpConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * DRP 明细行（{@code ErpDrpLine}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/drp/state-machine.md} §适用对象二。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载四态迁移矩阵
 * （SUGGESTED/APPROVED/ORDERED/CANCELLED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 BizModel/Processor（契约 §7）。
 *
 * <p>迁移矩阵（4 条边，按 per-(action, fromStatus, toStatus) 三元组）：
 * <ul>
 *   <li>approveLine(SUGGESTED→APPROVED)；</li>
 *   <li>releaseLine(APPROVED→ORDERED)；</li>
 *   <li>cancel 多源 {SUGGESTED→CANCELLED, APPROVED→CANCELLED}（人工取消/驳回均可从建议态或已批准态进入取消）。</li>
 * </ul>
 *
 * <p>ORDERED/CANCELLED 终态无出边；SUGGESTED 初始态。cancelLine/rejectLine 同语义（均置 CANCELLED），按 JavaDoc
 * 自包含副本约定各自内联 Processor；二者均委托本 Bean 的 {@code assertCanCancel}，不合并类。
 *
 * <p>动态守卫边界（保留 Processor）：releaseLine 的 TRANSFER/PURCHASE 类型守卫（sourceWh/supplier 必填）、
 * 释放幂等码 {@code ERR_DRP_LINE_ALREADY_ORDERED}（ORDERED 重复释放，独立语义非矩阵非法边）、
 * {@code ERR_DRP_LINE_NOT_SUGGESTED}（releaseLine 非 APPROVED 误名码，pre-existing 保留）不属于状态轴判断，本 Bean 不承载。
 */
public class ErpDrpLineStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanApproveLine(String status) {
        if (!ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED.equals(status)) {
            throw illegal("approveLine", status, ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED);
        }
    }

    public String approveLineTargetStatus() {
        return ErpDrpConstants.DRP_LINE_STATUS_APPROVED;
    }

    public void assertCanRelease(String status) {
        if (!ErpDrpConstants.DRP_LINE_STATUS_APPROVED.equals(status)) {
            throw illegal("releaseLine", status, ErpDrpConstants.DRP_LINE_STATUS_APPROVED);
        }
    }

    public String releaseTargetStatus() {
        return ErpDrpConstants.DRP_LINE_STATUS_ORDERED;
    }

    /**
     * cancel 守卫：非终态（SUGGESTED/APPROVED）均合法。cancelLine 与 rejectLine 共用本方法（同语义）。
     */
    public void assertCanCancel(String status) {
        if (isTerminal(status)) {
            throw illegal("cancel", status, "非终态(SUGGESTED/APPROVED)");
        }
    }

    public String cancelTargetStatus() {
        return ErpDrpConstants.DRP_LINE_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String status) {
        return ErpDrpConstants.DRP_LINE_STATUS_ORDERED.equals(status)
                || ErpDrpConstants.DRP_LINE_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("approveLine",
                        ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED, ErpDrpConstants.DRP_LINE_STATUS_APPROVED),
                new TransitionDefinition("releaseLine",
                        ErpDrpConstants.DRP_LINE_STATUS_APPROVED, ErpDrpConstants.DRP_LINE_STATUS_ORDERED),
                new TransitionDefinition("cancel",
                        ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED, ErpDrpConstants.DRP_LINE_STATUS_CANCELLED),
                new TransitionDefinition("cancel",
                        ErpDrpConstants.DRP_LINE_STATUS_APPROVED, ErpDrpConstants.DRP_LINE_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpDrpConstants.DRP_LINE_STATUS_ORDERED, ErpDrpConstants.DRP_LINE_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpDrpConstants.DRP_LINE_STATUS_SUGGESTED);
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
