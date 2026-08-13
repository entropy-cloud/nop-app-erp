package app.erp.prj.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 项目结算单（{@code ErpPrjProjectSettlement}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code approveStatus} 审批轴，
 * 字典 {@code wf/approve-status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/projects/state-machine.md} §适用对象五。
 *
 * <p><b>治理裁定（§11.2 M3(iii)→M4 升级）</b>：{@code approve}（doApprove）双轴同动——既写 approveStatus=APPROVED
 * 又写 docStatus=APPROVED，且触发业财过账（{@code ProjectSettlementPostingDispatcher.tryPost} + CLOSE 类型转固
 * {@code createAndActivateAsset}）。依契约 §11.2 M3(iii) 与路线图 M2-M4 纪律，本轴升级为 <b>M4 plan-first</b>。
 * 过账/转固编排时序/失败回退/红冲闭环按 §11.2 M4 (ii)/(iv)/(v) 原序保留在 Processor/`I*Biz` 路径；{@code posted}
 * 不入轴（契约 §3）。
 *
 * <p>命名带 {@code Approval} 后缀（契约 §1 双轴约定，与 {@code ErpPrjProjectSettlementDocumentStateMachine}
 * docStatus 轴分离）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载审批轴 3 动作迁移矩阵
 * （submit/approve/reject）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 *
 * <p>迁移矩阵（3 条边）：submit(UNSUBMITTED→SUBMITTED)、approve(SUBMITTED→APPROVED)、reject(SUBMITTED→REJECTED)。
 * {@code reverseSettlement} 不写 approveStatus（已核对 {@code ErpPrjProjectSettlementReverseSettlementProcessor}，
 * 纯 {@code posted} 轴冲销）→ 在本 Bean 中<b>无迁移边</b>。
 *
 * <p><b>approve 的 config-gated 动态守卫（保留 Processor）</b>：{@code erp-prj.settlement-require-approval}
 * （默认 true=STRICT，仅 SUBMITTED 可审批；false=RELAXED，允许 UNSUBMITTED 直审）。本 Bean {@link #assertCanApprove}
 * 承载默认 STRICT 矩阵（SUBMITTED 单源）；RELAXED 分支为 config-gated 动态扩展，保留在
 * {@code ErpPrjProjectSettlementProcessor.validateTransitionForApprove}（行为保持）。
 *
 * <p>APPROVED 为<b>真终态</b>——无 writer 将 approveStatus 从 APPROVED 迁出（cancel 只写 docStatus，reverseSettlement
 * 只写 posted），故适用「终态无出边」强断言。REJECTED 为可达汇（无出边），非声明终态。
 *
 * <p>动态守卫边界（保留 Processor）：转固条件、过账派发、归集、红冲——不属于状态轴判断，本 Bean 不承载。
 */
public class ErpPrjProjectSettlementApprovalStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * submit 守卫：来源态为 {@code UNSUBMITTED} 合法（{@code null} 非法，对齐 facade 既有行为）。
     *
     * <p>接线方 {@code ErpPrjProjectSettlementProcessor.validateTransitionForSubmit} 映射为领域码
     * {@code ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION}。
     */
    public void assertCanSubmit(String approveStatus) {
        if (!ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED.equals(approveStatus)) {
            throw illegal("submit", approveStatus, ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
        }
    }

    /**
     * approve 守卫（默认 STRICT 矩阵）：来源态为 {@code SUBMITTED} 合法。
     *
     * <p>承载 {@code erp-prj.settlement-require-approval=true}（默认）的固定矩阵。RELAXED（=false）分支允许
     * UNSUBMITTED 直审，属 config-gated 动态扩展，保留在 Processor（行为保持）。
     */
    public void assertCanApprove(String approveStatus) {
        if (!ErpPrjConstants.APPROVE_STATUS_SUBMITTED.equals(approveStatus)) {
            throw illegal("approve", approveStatus, ErpPrjConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    /** reject 守卫：来源态为 {@code SUBMITTED} 合法。 */
    public void assertCanReject(String approveStatus) {
        if (!ErpPrjConstants.APPROVE_STATUS_SUBMITTED.equals(approveStatus)) {
            throw illegal("reject", approveStatus, ErpPrjConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    public String submitTargetStatus() {
        return ErpPrjConstants.APPROVE_STATUS_SUBMITTED;
    }

    public String approveTargetStatus() {
        return ErpPrjConstants.APPROVE_STATUS_APPROVED;
    }

    public String rejectTargetStatus() {
        return ErpPrjConstants.APPROVE_STATUS_REJECTED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定。结算审批轴业务终态为 {@code APPROVED}（owner doc §适用对象五）；真终态——无 writer 将
     * approveStatus 从 APPROVED 迁出。
     */
    public boolean isTerminal(String approveStatus) {
        return ErpPrjConstants.APPROVE_STATUS_APPROVED.equals(approveStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submit", ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED, ErpPrjConstants.APPROVE_STATUS_SUBMITTED),
                new TransitionDefinition("approve", ErpPrjConstants.APPROVE_STATUS_SUBMITTED, ErpPrjConstants.APPROVE_STATUS_APPROVED),
                new TransitionDefinition("reject", ErpPrjConstants.APPROVE_STATUS_SUBMITTED, ErpPrjConstants.APPROVE_STATUS_REJECTED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpPrjConstants.APPROVE_STATUS_APPROVED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpPrjConstants.APPROVE_STATUS_UNSUBMITTED);
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
