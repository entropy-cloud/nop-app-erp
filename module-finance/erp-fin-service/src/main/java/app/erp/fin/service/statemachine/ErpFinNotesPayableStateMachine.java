package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 应付票据（{@code ErpFinNotesPayable}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}
 * 业务生命周期轴，字典 {@code erp-fin/notes-payable-status} 4 值：ISSUED/HONORED/DISHONORED/WRITE_OFF）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/finance/state-machine.md} §对象四 + {@code docs/design/finance/treasury.md} §状态机。
 *
 * <p><b>治理裁定（§11.2 M4 plan-first）</b>：issue/honor 触发业财过账（{@code NotesPostingDispatcher}→
 * {@code FinPostingExecutor} 生成凭证），writeOff 若已过账则 {@code reversePayable} 红冲；<b>授信占用/释放</b>
 * （{@code IErpFinCreditFacilityBiz}）为 Payable 独有受保护维度——issue 时 {@code reserveCreditIfNeeded}（银承占用授信，
 * config-gated {@code erp-fin.credit-check-on-issue}），honor/dishonor/writeOff 时 {@code releaseOccupiedCredit}。
 * 依契约 §11.2 M4 硬约束 (i)–(v)：过账时序/编排/失败回退（posted 回写）/红冲闭环继续由 {@code NotesPostingDispatcher}→
 * {@code FinPostingExecutor} 引擎 + {@code posted}/{@code postedBy}/{@code postedAt} 标志契约管理（§11.2 M4 (ii)/(v)），
 * Bean 不触碰；授信占用/释放时序保留原位（§11.2 M4 (iv)）；{@code posted} 不入轴（§11.2 M4 (iii)）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。
 *
 * <p><b>4 命名动作迁移矩阵</b>：
 * <ul>
 *   <li>{@code issue} → ISSUED：<b>initial 态写入</b>（§9.2 选项 c，票据开出即 ISSUED，无 DRAFT 前态）。
 *       合法来源态 = {@code null}（初始写入）或 {@code ISSUED}（幂等短路 {@code isAlreadyIssued}）。
 *       非 null 且非 initial 的来源态非法（issue 守卫<b>有意收窄</b>，见 plan Phase 3——实仓零生产路径/测试
 *       从非 initial 态 issue）。{@link #transitions()} 中该边以 ISSUED→ISSUED（幂等）表示。</li>
 *   <li>{@code honor} {ISSUED}→HONORED（守卫 ISSUED，expected「ISSUED」）。</li>
 *   <li>{@code dishonor} {ISSUED}→DISHONORED。</li>
 *   <li>{@code writeOff} {非终态}→WRITE_OFF（{@link #assertCanWriteOff(String)} 校验 {@code !isTerminal(from)}，
 *       保留 loose 语义，expected「非终态」）。</li>
 * </ul>
 *
 * <p><b>writer 放置</b>：Payable 4 动作 writer 全部在 facade {@code do*}（无 per-mutation 不对称，
 * 区别于 Receivable 的 collect/dishonor per-mutation writer）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7），
 * expectedStatus 文案（如「ISSUED」「非终态」）由本 Bean 承载、对外不变。
 */
public class ErpFinNotesPayableStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * issue 入口守卫：来源态为 {@code null}（initial 写入）或 {@code ISSUED}（幂等）合法。
     *
     * <p>接线方 {@code ErpFinNotesPayableIssueProcessor}（经 facade {@code validateTransitionForIssue}）
     * 映射为领域码 {@code ERR_NOTES_PAYABLE_ILLEGAL_STATUS_TRANSITION}（common 码作 cause 保留）。
     */
    public void assertCanIssue(String status) {
        if (status != null && !ErpFinConstants.NOTES_PAY_ISSUED.equals(status)) {
            throw illegal("issue", status, ErpFinConstants.NOTES_PAY_ISSUED);
        }
    }

    /**
     * honor 入口守卫：来源态为 {@code ISSUED} 合法。
     *
     * <p>接线方 {@code ErpFinNotesPayableHonorProcessor}（经 facade {@code validateTransitionForHonor}）。
     */
    public void assertCanHonor(String status) {
        if (!ErpFinConstants.NOTES_PAY_ISSUED.equals(status)) {
            throw illegal("honor", status, ErpFinConstants.NOTES_PAY_ISSUED);
        }
    }

    /**
     * dishonor 入口守卫：来源态为 {@code ISSUED} 合法。
     *
     * <p>接线方 {@code ErpFinNotesPayableDishonorProcessor}（经 facade {@code validateTransitionForDishonor}）。
     */
    public void assertCanDishonor(String status) {
        if (!ErpFinConstants.NOTES_PAY_ISSUED.equals(status)) {
            throw illegal("dishonor", status, ErpFinConstants.NOTES_PAY_ISSUED);
        }
    }

    /**
     * writeOff 入口守卫：来源态为<b>任意非终态</b>合法（{@code !isTerminal(from)}，loose 语义，expected「非终态」）。
     *
     * <p>接线方 {@code ErpFinNotesPayableWriteOffProcessor}（经 facade {@code validateTransitionForWriteOff}）。
     */
    public void assertCanWriteOff(String status) {
        if (isTerminal(status)) {
            throw illegal("writeOff", status, "非终态");
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    /** issue 的目标态（ISSUED，initial 写入）。 */
    public String issueTargetStatus() {
        return ErpFinConstants.NOTES_PAY_ISSUED;
    }

    /** honor 的目标态（HONORED，终态）。 */
    public String honorTargetStatus() {
        return ErpFinConstants.NOTES_PAY_HONORED;
    }

    /** dishonor 的目标态（DISHONORED，终态）。 */
    public String dishonorTargetStatus() {
        return ErpFinConstants.NOTES_PAY_DISHONORED;
    }

    /** writeOff 的目标态（WRITE_OFF，终态）。 */
    public String writeOffTargetStatus() {
        return ErpFinConstants.NOTES_PAY_WRITE_OFF;
    }

    // ---------- 终态/初始态分类 ----------

    /** 业务终态判定。应付票据终态为 {@code HONORED}/{@code DISHONORED}/{@code WRITE_OFF}（均无出边）。 */
    public boolean isTerminal(String status) {
        return status != null
                && (ErpFinConstants.NOTES_PAY_HONORED.equals(status)
                || ErpFinConstants.NOTES_PAY_DISHONORED.equals(status)
                || ErpFinConstants.NOTES_PAY_WRITE_OFF.equals(status));
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    /**
     * 迁移矩阵只读快照（4 命名边，每命名动作一条代表边）：
     * issue（ISSUED→ISSUED 幂等表示，null initial 写入见 javadoc）/honor/dishonor/writeOff（ISSUED 代表源，
     * 全部非终态源见 {@link #assertCanWriteOff(String)}）。多源动作的完整合法来源态以显式 {@code assertCan*}
     * 方法为准。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("issue",
                        ErpFinConstants.NOTES_PAY_ISSUED, ErpFinConstants.NOTES_PAY_ISSUED),
                new TransitionDefinition("honor",
                        ErpFinConstants.NOTES_PAY_ISSUED, ErpFinConstants.NOTES_PAY_HONORED),
                new TransitionDefinition("dishonor",
                        ErpFinConstants.NOTES_PAY_ISSUED, ErpFinConstants.NOTES_PAY_DISHONORED),
                new TransitionDefinition("writeOff",
                        ErpFinConstants.NOTES_PAY_ISSUED, ErpFinConstants.NOTES_PAY_WRITE_OFF)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpFinConstants.NOTES_PAY_HONORED,
                ErpFinConstants.NOTES_PAY_DISHONORED,
                ErpFinConstants.NOTES_PAY_WRITE_OFF));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpFinConstants.NOTES_PAY_ISSUED);
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
