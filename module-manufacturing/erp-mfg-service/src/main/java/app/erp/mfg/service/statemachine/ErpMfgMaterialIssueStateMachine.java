package app.erp.mfg.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.mfg.service.ErpMfgConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 领料单（{@code ErpMfgMaterialIssue}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus} 业务生命周期轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/manufacturing/state-machine.md} §实现约定（领料红冲实现注记）+ 4 态
 * dict {@code erp-mfg/issue-status}（DRAFT/CONFIRMED/DONE/CANCELLED）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载领料单 confirm/reverseConfirm
 * 动作迁移矩阵 + 终态/初始态分类 + 只读 {@link #transitions()} 元数据。可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>实体无 approveStatus 轴，单轴 Bean 不带 {@code Document}/{@code Approval} 后缀（契约 §1 单轴省略约定）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 * MaterialIssue 领域码复用泛型 {@code ERR_INVALID_STATUS_TRANSITION}（码 {@code erp.err.mfg.work-order.illegal-status-transition}，
 * misnamed——路线图 Non-Goal「不借迁移改变既有错误码」，保持不变，归 watch-only successor）。
 *
 * <p><b>confirm 两步迁移建模（plan Phase 3 Decision）</b>：confirm 为单命名动作，入口守卫仅 DRAFT，
 * 动作内部两步写 DRAFT→CONFIRMED→DONE（{@code ErpMfgMaterialIssueConfirmProcessor} 先置 CONFIRMED 生成出库移动单，
 * 出库成功后再置 DONE）。Bean 按命名动作建模：<b>confirm(DRAFT→DONE)</b> 单条边 + {@code confirmTargetStatus()}=DONE；
 * <b>CONFIRMED 为瞬态中间态</b>（confirm 动作内部写入的中间态，同事务内立即推进至 DONE，非命名动作边界）——
 * 不暴露为独立动作边、不入初始/终态集；<b>不得判其为死状态</b>（CONFIRMED 有 writer：confirm 动作在
 * {@code ConfirmProcessor} 置位，dict 持久态）。DONE 幂等短路（重复确认空操作）为动态幂等守卫，保留在 Processor 原位。
 *
 * <p><b>reverseConfirm 为「动态不对称守卫 + 固定状态边」</b>：posted=true 判定是动态业务守卫（含 posted，
 * 非固定状态迁移边），保留在 {@code ErpMfgMaterialIssueReverseConfirmProcessor.validateCanReverse} 原位；
 * 本 Bean 仅编码固定状态边 DONE→CANCELLED。
 */
public class ErpMfgMaterialIssueStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * confirm 守卫：仅 DRAFT 合法（领料确认入口）。
     *
     * <p>已 DONE 的幂等短路（重复确认空操作）由 Processor 原位处理（动态幂等守卫），不经本守卫；
     * 其余来源态（含 CONFIRMED 瞬态中间态）经正向 allow-list 拒绝。
     */
    public void assertCanConfirm(String status) {
        if (!ErpMfgConstants.ISSUE_STATUS_DRAFT.equals(status)) {
            throw illegal("confirm", status, ErpMfgConstants.ISSUE_STATUS_DRAFT);
        }
    }

    /** confirm 目标态=DONE（confirm 动作的最终目标态；中间态 CONFIRMED 由动作内部两步写入）。 */
    public String confirmTargetStatus() {
        return ErpMfgConstants.ISSUE_STATUS_DONE;
    }

    /**
     * reverseConfirm 状态边守卫：仅 DONE 合法。
     *
     * <p>posted=true 前置为动态业务守卫（含 posted，非固定状态迁移边），保留在
     * {@code ErpMfgMaterialIssueReverseConfirmProcessor.validateCanReverse} 原位；本 Bean 仅编码固定状态边 DONE→CANCELLED。
     */
    public void assertCanReverseConfirm(String status) {
        if (!ErpMfgConstants.ISSUE_STATUS_DONE.equals(status)) {
            throw illegal("reverseConfirm", status, ErpMfgConstants.ISSUE_STATUS_DONE);
        }
    }

    public String reverseConfirmTargetStatus() {
        return ErpMfgConstants.ISSUE_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类：DONE（已出库）与 CANCELLED（已取消/已红冲）均为终态。
     *
     * <p>CONFIRMED 为瞬态中间态（confirm 动作内部写入），不入终态集（非命名动作边界）。
     */
    public boolean isTerminal(String status) {
        return ErpMfgConstants.ISSUE_STATUS_DONE.equals(status)
                || ErpMfgConstants.ISSUE_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("confirm",
                        ErpMfgConstants.ISSUE_STATUS_DRAFT, ErpMfgConstants.ISSUE_STATUS_DONE),
                new TransitionDefinition("reverseConfirm",
                        ErpMfgConstants.ISSUE_STATUS_DONE, ErpMfgConstants.ISSUE_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpMfgConstants.ISSUE_STATUS_DONE,
                ErpMfgConstants.ISSUE_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Collections.singletonList(ErpMfgConstants.ISSUE_STATUS_DRAFT));
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
