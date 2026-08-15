package app.erp.ct.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.ct.service.ErpCtConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 合同头（{@code ErpCtContract}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/contract/state-machine.md}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载**已实现**迁移矩阵
 * （DRAFT/NEGOTIATION/ACTIVE/SUSPENDED/EXPIRED/TERMINATED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（9 条边，仅编码命名动作路径下**已落地**的 writer）：
 * <ul>
 *   <li>submit(DRAFT→NEGOTIATION)（提交谈判，RC-R1.32 落地 §2 漂移 successor）；</li>
 *   <li>activate(NEGOTIATION→ACTIVE)；</li>
 *   <li>suspend(ACTIVE→SUSPENDED)、resume(SUSPENDED→ACTIVE)；</li>
 *   <li>terminate(ACTIVE→TERMINATED)、terminate(NEGOTIATION→TERMINATED)（多源：生效合同提前终止 + 谈判破裂）；</li>
 *   <li>expire(ACTIVE→EXPIRED)；</li>
 *   <li>amend(ACTIVE→DRAFT)（修订回草稿）；</li>
 *   <li>rejectAmend(DRAFT→ACTIVE)（变更单驳回恢复，RC-R1.32）。</li>
 * </ul>
 *
 * <p><strong>NEGOTIATION 可达性漂移（RC-R1.32 已修复）</strong>：命名动作路径下从 DRAFT 的
 * {@code setStatus(NEGOTIATION)} writer（{@code submitForNegotiation}）已由本行落地
 * （{@link #assertCanSubmitForNegotiation} + {@link ErpCtContractBizModel#submit}），DRAFT 出边
 * = submit(DRAFT→NEGOTIATION) + rejectAmend(DRAFT→ACTIVE)。NEGOTIATION 不再仅作为 activate/terminate
 * 的源态被消费（该漂移在 plan 2026-08-12-1118-1 层 2 四方对照登记为 implementation drift + successor，
 * 本 Bean 已按 successor 落地编码该边）。
 *
 * <p><strong>CANCELLED 漂移</strong>：owner doc 声明 {@code DRAFT→CANCELLED} 草稿废弃终态，但 dict
 * {@code erp-ct/contract-status} 缺 CANCELLED 值且零 writer → 本 Bean 不纳入终态集（dict 与 writer 双侧均无）。
 * 此漂移在 plan 2026-08-12-1118-1 层 2 登记为 doc drift + successor（触及 ORM 保护区 ask-first）。
 */
public class ErpCtContractStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * submit 守卫：仅 DRAFT 合法（RC-R1.32 落地 §2 漂移 successor，UC-CT-01 提交审批）。
     *
     * <p><strong>不因已有版本拒绝</strong>——amend 生命周期下变更 DRAFT 已有版本
     * （v1 false + v2 DRAFT true），submit 是其唯一前向出口（MAJOR-1 修正：仅守卫 status==DRAFT，
     * 零版本建 v1 / 已有版本保留既有 DRAFT 当前版本不动，由 BizModel 实现承载）。
     */
    public void assertCanSubmitForNegotiation(String status) {
        if (!ErpCtConstants.CONTRACT_STATUS_DRAFT.equals(status)) {
            throw illegal("submit", status, ErpCtConstants.CONTRACT_STATUS_DRAFT);
        }
    }

    public String submitTargetStatus() {
        return ErpCtConstants.CONTRACT_STATUS_NEGOTIATION;
    }

    public void assertCanActivate(String status) {
        if (!ErpCtConstants.CONTRACT_STATUS_NEGOTIATION.equals(status)) {
            throw illegal("activate", status, ErpCtConstants.CONTRACT_STATUS_NEGOTIATION);
        }
    }

    public String activateTargetStatus() {
        return ErpCtConstants.CONTRACT_STATUS_ACTIVE;
    }

    public void assertCanSuspend(String status) {
        if (!ErpCtConstants.CONTRACT_STATUS_ACTIVE.equals(status)) {
            throw illegal("suspend", status, ErpCtConstants.CONTRACT_STATUS_ACTIVE);
        }
    }

    public String suspendTargetStatus() {
        return ErpCtConstants.CONTRACT_STATUS_SUSPENDED;
    }

    public void assertCanResume(String status) {
        if (!ErpCtConstants.CONTRACT_STATUS_SUSPENDED.equals(status)) {
            throw illegal("resume", status, ErpCtConstants.CONTRACT_STATUS_SUSPENDED);
        }
    }

    public String resumeTargetStatus() {
        return ErpCtConstants.CONTRACT_STATUS_ACTIVE;
    }

    /**
     * terminate 守卫：接受 ACTIVE（生效合同提前终止）+ NEGOTIATION（谈判破裂放弃）两类源态。
     *
     * <p>对齐 {@code state-machine.md §2/§3}：NEGOTIATION→TERMINATED 谈判破裂终态。其余状态
     * （DRAFT/SUSPENDED/EXPIRED/TERMINATED）非法。
     */
    public void assertCanTerminate(String status) {
        if (!ErpCtConstants.CONTRACT_STATUS_ACTIVE.equals(status)
                && !ErpCtConstants.CONTRACT_STATUS_NEGOTIATION.equals(status)) {
            throw illegal("terminate", status,
                    ErpCtConstants.CONTRACT_STATUS_ACTIVE + "/" + ErpCtConstants.CONTRACT_STATUS_NEGOTIATION);
        }
    }

    public String terminateTargetStatus() {
        return ErpCtConstants.CONTRACT_STATUS_TERMINATED;
    }

    public void assertCanExpire(String status) {
        if (!ErpCtConstants.CONTRACT_STATUS_ACTIVE.equals(status)) {
            throw illegal("expire", status, ErpCtConstants.CONTRACT_STATUS_ACTIVE);
        }
    }

    public String expireTargetStatus() {
        return ErpCtConstants.CONTRACT_STATUS_EXPIRED;
    }

    public void assertCanAmend(String status) {
        if (!ErpCtConstants.CONTRACT_STATUS_ACTIVE.equals(status)) {
            throw illegal("amend", status, ErpCtConstants.CONTRACT_STATUS_ACTIVE);
        }
    }

    public String amendTargetStatus() {
        return ErpCtConstants.CONTRACT_STATUS_DRAFT;
    }

    /**
     * rejectAmend 守卫：仅 DRAFT 合法（RC-R1.32 落地，UC-CT-02 异常「变更单被驳回 → 原合同保持 ACTIVE 不变」）。
     * 恢复目标（前任 current 版本）规格化见 {@link ErpCtContractBizModel#rejectAmend}（D5 选项 B：
     * 优先 SIGNED 最大 versionNo，无 SIGNED 回落 FINALIZED 最大者——恢复时推导，跨请求可行 + 对遗留
     * DRAFT 行免疫 + 防 finalize-then-reject 恢复未签署 FINALIZED 为 current）。
     */
    public void assertCanRejectAmend(String status) {
        if (!ErpCtConstants.CONTRACT_STATUS_DRAFT.equals(status)) {
            throw illegal("rejectAmend", status, ErpCtConstants.CONTRACT_STATUS_DRAFT);
        }
    }

    public String rejectAmendTargetStatus() {
        return ErpCtConstants.CONTRACT_STATUS_ACTIVE;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类：EXPIRED/TERMINATED。
     *
     * <p>注：CANCELLED 因 dict {@code erp-ct/contract-status} 缺值 + 零 writer，不纳入终态集
     * （owner doc 声明的 DRAFT→CANCELLED 草稿废弃终态未落地，层 2 登记为 doc drift + successor）。
     */
    public boolean isTerminal(String status) {
        return ErpCtConstants.CONTRACT_STATUS_EXPIRED.equals(status)
                || ErpCtConstants.CONTRACT_STATUS_TERMINATED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submit", ErpCtConstants.CONTRACT_STATUS_DRAFT, ErpCtConstants.CONTRACT_STATUS_NEGOTIATION),
                new TransitionDefinition("activate", ErpCtConstants.CONTRACT_STATUS_NEGOTIATION, ErpCtConstants.CONTRACT_STATUS_ACTIVE),
                new TransitionDefinition("suspend", ErpCtConstants.CONTRACT_STATUS_ACTIVE, ErpCtConstants.CONTRACT_STATUS_SUSPENDED),
                new TransitionDefinition("resume", ErpCtConstants.CONTRACT_STATUS_SUSPENDED, ErpCtConstants.CONTRACT_STATUS_ACTIVE),
                new TransitionDefinition("terminate", ErpCtConstants.CONTRACT_STATUS_ACTIVE, ErpCtConstants.CONTRACT_STATUS_TERMINATED),
                new TransitionDefinition("terminate", ErpCtConstants.CONTRACT_STATUS_NEGOTIATION, ErpCtConstants.CONTRACT_STATUS_TERMINATED),
                new TransitionDefinition("expire", ErpCtConstants.CONTRACT_STATUS_ACTIVE, ErpCtConstants.CONTRACT_STATUS_EXPIRED),
                new TransitionDefinition("amend", ErpCtConstants.CONTRACT_STATUS_ACTIVE, ErpCtConstants.CONTRACT_STATUS_DRAFT),
                new TransitionDefinition("rejectAmend", ErpCtConstants.CONTRACT_STATUS_DRAFT, ErpCtConstants.CONTRACT_STATUS_ACTIVE)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpCtConstants.CONTRACT_STATUS_EXPIRED, ErpCtConstants.CONTRACT_STATUS_TERMINATED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpCtConstants.CONTRACT_STATUS_DRAFT);
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
