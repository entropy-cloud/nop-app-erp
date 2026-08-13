package app.erp.prj.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.prj.service.ErpPrjConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 项目结算单（{@code ErpPrjProjectSettlement}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus}
 * 业务生命周期轴，字典 {@code erp-prj/project-status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/projects/state-machine.md} §适用对象五。
 *
 * <p><b>治理裁定（§11.2 M3(iii)→M4 升级）</b>：{@code approve}（doApprove）双轴同动——既写 docStatus=APPROVED
 * 又触发业财过账（{@code ProjectSettlementPostingDispatcher.tryPost} + CLOSE 类型转固）。依契约 §11.2 M3(iii)
 * 与路线图 M2-M4 纪律，本轴升级为 <b>M4 plan-first</b>。过账/转固编排时序/失败回退/红冲闭环按 §11.2 M4 (ii)/(iv)/(v)
 * 原序保留在 Processor/`I*Biz` 路径；{@code posted} 不入轴（契约 §3）。
 *
 * <p>命名带 {@code Document} 后缀（契约 §1 双轴约定，为审批轴 {@code ErpPrjProjectSettlementApprovalStateMachine}
 * 预留命名空间）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 docStatus 轴迁移矩阵
 * （approve/cancel）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 *
 * <p>迁移矩阵（3 条边）：approve(DRAFT→APPROVED，doApprove 双轴同动触发)、cancel 多源 {DRAFT, APPROVED}→CANCELLED = 2 边。
 * {@code reverseSettlement} 不写 docStatus（纯 {@code posted} 轴冲销）→ 在本 Bean 中<b>无迁移边</b>。
 *
 * <p><b>docStatus dict-value 漂移（Phase 3 Fix 登记，不改 ORM，属 ask-first 保护区）</b>：{@code doApprove} 写入
 * {@code DOC_STATUS_APPROVED="APPROVED"}，但 {@code erp-prj/project-status} 字典仅含 DRAFT/OPEN/ON_HOLD/COMPLETED/
 * CANCELLED——APPROVED 被写入但不在字典内（对齐 {@code ErpPrjCostCollection} §适用对象三先例）。本 Bean 按既有 writer
 * 建模该边（保持行为），dict 补全/rebind 列 successor。
 *
 * <p><b>共享 dict 死状态（Phase 3 Decision，保留不改绑）</b>：{@code erp-prj/project-status} 的 OPEN/ON_HOLD/COMPLETED
 * 对结算单<b>无 writer</b>（死状态），保留为预留语义入口（对齐 §适用对象三 + assets 保留死状态先例）。
 *
 * <p><b>cancel 守卫边界（行为保持）</b>：{@link #assertCanCancel(String)} 仅对终态 CANCELLED 抛错，其余放行——与
 * {@code ErpPrjProjectSettlementProcessor.validateTransitionForCancel} 收敛前行为一致（仅拒绝已作废单据再次 cancel）。
 *
 * <p>动态守卫边界（保留 Processor）：转固条件（CLOSE + transferToAsset）、过账派发、红冲、归集——不属于状态轴判断，
 * 本 Bean 不承载。
 */
public class ErpPrjProjectSettlementDocumentStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * approve（doApprove 触发）目标态守卫：来源态为 {@code DRAFT} 合法。
     *
     * <p>doApprove 的迁移守卫在审批轴（{@code ErpPrjProjectSettlementApprovalStateMachine.assertCanApprove}），
     * docStatus 轴在 facade 既有实现中无独立守卫（docStatus 在正常流恒为 DRAFT）。本方法承载 docStatus 轴的固定矩阵
     * 语义（DRAFT→APPROVED）供完备性分析与未来接线消费；当前 Processor 接线仅用 {@link #approveTargetStatus()}
     * 做目标态写回（保持「docStatus 无独立守卫」既有行为）。
     */
    public void assertCanApprove(String docStatus) {
        if (!ErpPrjConstants.DOC_STATUS_DRAFT.equals(docStatus)) {
            throw illegal("approve", docStatus, ErpPrjConstants.DOC_STATUS_DRAFT);
        }
    }

    /**
     * cancel 守卫：非 CANCELLED 终态合法（行为保持，对齐 facade {@code validateTransitionForCancel}）。
     *
     * <p>对 CANCELLED 报告 common 层非法边（携带 {@code action=cancel}/{@code fromStatus=CANCELLED}）。
     * 接线方 {@code ErpPrjProjectSettlementProcessor.validateTransitionForCancel} 映射为领域码
     * {@code ERR_SETTLEMENT_ILLEGAL_STATUS_TRANSITION}（保持既有 expected="非CANCELLED" 文案）。
     */
    public void assertCanCancel(String docStatus) {
        if (isTerminal(docStatus)) {
            throw illegal("cancel", docStatus, "非CANCELLED");
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    /** doApprove 双轴同动时 docStatus 的目标态（APPROVED，dict-value drift 保留）。 */
    public String approveTargetStatus() {
        return ErpPrjConstants.DOC_STATUS_APPROVED;
    }

    public String cancelTargetStatus() {
        return ErpPrjConstants.DOC_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    /** 业务终态判定。结算单据轴终态为 {@code CANCELLED}（无 writer 将 docStatus 从 CANCELLED 迁出）。 */
    public boolean isTerminal(String docStatus) {
        return ErpPrjConstants.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("approve", ErpPrjConstants.DOC_STATUS_DRAFT, ErpPrjConstants.DOC_STATUS_APPROVED),
                new TransitionDefinition("cancel", ErpPrjConstants.DOC_STATUS_DRAFT, ErpPrjConstants.DOC_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpPrjConstants.DOC_STATUS_APPROVED, ErpPrjConstants.DOC_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpPrjConstants.DOC_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpPrjConstants.DOC_STATUS_DRAFT);
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
