package app.erp.mnt.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mnt.dao.ErpMntDaoConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 备件消耗单（{@code ErpMntSparePartUsage}）单据生命周期轴状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}（§1 双轴约定——同一实体的多个状态轴各落独立 Bean，
 * 后缀区分轴名）；业务语义：{@code docs/design/maintenance/state-machine.md §实现约定}（备件消耗确认出库/红冲）。
 *
 * <p>严格无状态（契约 §2）。承载三态迁移矩阵（DRAFT/ACTIVE/CANCELLED）+ 终态/初始态分类 + 只读 {@link #transitions()}。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（2 条边）：confirm(DRAFT→ACTIVE)、reverseConfirm(ACTIVE→CANCELLED)。
 *
 * <p>注：备件消耗单无独立 submit/reject 审批 Processor——confirm 动作一步推进 docStatus DRAFT→ACTIVE（Decision (A)，
 * plan 2026-08-14-0930-3 Phase 2）。confirm 的来源态运行时守卫因 {@code validateNotConfirmed} silent-guard gap 暂不强制
 * （Deferred），Bean 本身承载完整矩阵供矩阵测试与未来强制化消费。
 */
public class ErpMntSparePartUsageDocumentStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanConfirm(String docStatus) {
        if (!ErpMntDaoConstants.DOC_STATUS_DRAFT.equals(docStatus)) {
            throw illegal("confirm", docStatus, ErpMntDaoConstants.DOC_STATUS_DRAFT);
        }
    }

    public String confirmTargetStatus() {
        return ErpMntDaoConstants.DOC_STATUS_ACTIVE;
    }

    public void assertCanReverseConfirm(String docStatus) {
        if (!ErpMntDaoConstants.DOC_STATUS_ACTIVE.equals(docStatus)) {
            throw illegal("reverseConfirm", docStatus, ErpMntDaoConstants.DOC_STATUS_ACTIVE);
        }
    }

    public String reverseConfirmTargetStatus() {
        return ErpMntDaoConstants.DOC_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String docStatus) {
        return ErpMntDaoConstants.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("confirm", ErpMntDaoConstants.DOC_STATUS_DRAFT, ErpMntDaoConstants.DOC_STATUS_ACTIVE),
                new TransitionDefinition("reverseConfirm", ErpMntDaoConstants.DOC_STATUS_ACTIVE, ErpMntDaoConstants.DOC_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.singletonList(ErpMntDaoConstants.DOC_STATUS_CANCELLED);
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpMntDaoConstants.DOC_STATUS_DRAFT);
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
