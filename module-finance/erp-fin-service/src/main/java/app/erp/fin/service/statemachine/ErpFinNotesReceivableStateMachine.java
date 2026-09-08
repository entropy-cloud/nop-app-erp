package app.erp.fin.service.statemachine;

import app.erp.fin.service.ErpFinConstants;
import app.erp.fin.service.ErpFinErrors;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 应收票据（{@code ErpFinNotesReceivable}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}
 * 业务生命周期轴，字典 {@code erp-fin/notes-receivable-status} 7 值：
 * RECEIVED/DISCOUNTED/ENDORSED/COLLECTION_PENDING/HONORED/DISHONORED/WRITE_OFF）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/finance/state-machine.md} §对象三 + {@code docs/design/finance/treasury.md} §状态机（7 态）。
 *
 * <p><b>治理裁定（§11.2 M4 plan-first）</b>：receive/discount/endorse/honor 触发业财过账
 * （{@code NotesPostingDispatcher}→{@code FinPostingExecutor} 生成凭证 + {@code ErpFinArApItem} 辅助账），
 * writeOff 若已过账则 {@code reverseReceivable} 红冲；COLLECTION_PENDING（在途）/DISHONORED（终态重分类）<b>不过账</b>。
 * 依契约 §11.2 M4 硬约束 (i)–(v)：过账时序/编排/失败回退（posted 回写）/红冲闭环继续由
 * {@code NotesPostingDispatcher}→{@code FinPostingExecutor} 引擎 + {@code posted}/{@code postedBy}/{@code postedAt}
 * 标志契约管理（§11.2 M4 (ii)/(v)），Bean 不触碰；跨域副作用（ArApItem 生成、dishonor→应收票据冲销联动）保留
 * 原 Processor 路径（§11.2 M4 (iv)）；{@code posted} 不入轴（§11.2 M4 (iii)）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。
 *
 * <p><b>7 命名动作迁移矩阵</b>：
 * <ul>
 *   <li>{@code receive} → RECEIVED：<b>initial 态写入</b>（§9.2 选项 c，票据收到即 RECEIVED，无 DRAFT 前态）。
 *       合法来源态 = {@code null}（初始写入）或 {@code RECEIVED}（幂等短路 {@code isAlreadyReceived}）。
 *       非 null 且非 initial 的来源态非法（receive 守卫<b>有意收窄</b>，见 plan Phase 2——实仓零生产路径/测试
 *       从非 initial 态 receive）。{@link #transitions()} 中该边以 RECEIVED→RECEIVED（幂等）表示。</li>
 *   <li>{@code discount} {RECEIVED}→DISCOUNTED（守卫 RECEIVED，expected「RECEIVED」）。</li>
 *   <li>{@code endorse} {RECEIVED}→ENDORSED。</li>
 *   <li>{@code collect} {RECEIVED, DISCOUNTED}→COLLECTION_PENDING（双源：已收到或已贴现均可送托收）。</li>
 *   <li>{@code honor} {COLLECTION_PENDING}→HONORED。</li>
 *   <li>{@code dishonor} {COLLECTION_PENDING}→DISHONORED（拒付转应收，treasury.md §规则 3）。</li>
 *   <li>{@code writeOff} {非终态}→WRITE_OFF（{@link #assertCanWriteOff(String)} 校验 {@code !isTerminal(from)}，
 *       保留 loose 语义，expected 为「!」前缀 + 终态码列表（plan 2026-09-07-0043-2 CAT-2 传码收敛））。</li>
 * </ul>
 *
 * <p><b>ENDORSED 非终态中间态</b>：背书后票据所有权已转移，<b>仅可 writeOff 出边</b>——不可 collect/
 * discount/endorse（{@code collect} 对 ENDORSED 非法，守卫只允许 RECEIVED/DISCOUNTED）。
 *
 * <p>非法边直抛领域码 {@link ErpFinErrors#ERR_NOTES_RECEIVABLE_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}/{@code action}；plan 2026-09-07-2200-1）；实体元数据（notesCode）经调用点同码补参，
 * expectedStatus 码文案（如「RECEIVED / DISCOUNTED」「! 终态码列表」）由本 Bean 承载（CAT-2 传码，plan 2026-09-07-0043-2）。
 */
public class ErpFinNotesReceivableStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * receive 入口守卫：来源态为 {@code null}（initial 写入）或 {@code RECEIVED}（幂等）合法。
     *
     * <p>接线方 {@code ErpFinNotesReceivableReceiveProcessor}（经 facade {@code validateTransitionForReceive}）
     * 同码补参 notesCode。
     */
    public void assertCanReceive(String status) {
        if (status != null && !ErpFinConstants.NOTES_RECV_RECEIVED.equals(status)) {
            throw illegal("receive", status, ErpFinConstants.NOTES_RECV_RECEIVED);
        }
    }

    /**
     * discount 入口守卫：来源态为 {@code RECEIVED} 合法。
     *
     * <p>接线方 {@code ErpFinNotesReceivableDiscountProcessor}（经 facade {@code validateTransitionForDiscount}）。
     */
    public void assertCanDiscount(String status) {
        if (!ErpFinConstants.NOTES_RECV_RECEIVED.equals(status)) {
            throw illegal("discount", status, ErpFinConstants.NOTES_RECV_RECEIVED);
        }
    }

    /**
     * endorse 入口守卫：来源态为 {@code RECEIVED} 合法。
     *
     * <p>接线方 {@code ErpFinNotesReceivableEndorseProcessor}（经 facade {@code validateTransitionForEndorse}）。
     */
    public void assertCanEndorse(String status) {
        if (!ErpFinConstants.NOTES_RECV_RECEIVED.equals(status)) {
            throw illegal("endorse", status, ErpFinConstants.NOTES_RECV_RECEIVED);
        }
    }

    /**
     * collect 入口守卫：来源态为 {@code RECEIVED} 或 {@code DISCOUNTED} 合法（双源，expected「RECEIVED / DISCOUNTED」）。
     *
     * <p>接线方 {@code ErpFinNotesReceivableCollectProcessor}（直接注入 Bean）。
     */
    public void assertCanCollect(String status) {
        if (!ErpFinConstants.NOTES_RECV_RECEIVED.equals(status)
                && !ErpFinConstants.NOTES_RECV_DISCOUNTED.equals(status)) {
            throw illegal("collect", status, "RECEIVED / DISCOUNTED");
        }
    }

    /**
     * honor 入口守卫：来源态为 {@code COLLECTION_PENDING} 合法。
     *
     * <p>接线方 {@code ErpFinNotesReceivableHonorProcessor}（经 facade {@code validateTransitionForHonorOrDishonor}）。
     */
    public void assertCanHonor(String status) {
        if (!ErpFinConstants.NOTES_RECV_COLLECTION_PENDING.equals(status)) {
            throw illegal("honor", status, ErpFinConstants.NOTES_RECV_COLLECTION_PENDING);
        }
    }

    /**
     * dishonor 入口守卫：来源态为 {@code COLLECTION_PENDING} 合法。
     *
     * <p>接线方 {@code ErpFinNotesReceivableDishonorProcessor}（直接注入 Bean）。
     */
    public void assertCanDishonor(String status) {
        if (!ErpFinConstants.NOTES_RECV_COLLECTION_PENDING.equals(status)) {
            throw illegal("dishonor", status, ErpFinConstants.NOTES_RECV_COLLECTION_PENDING);
        }
    }

    /**
     * writeOff 入口守卫：来源态为<b>任意非终态</b>合法（{@code !isTerminal(from)}，loose 语义，expected 为 {@code "!" + 终态码列表}）。
     *
     * <p>接线方 {@code ErpFinNotesReceivableWriteOffProcessor}（经 facade {@code validateTransitionForWriteOff}）。
     */
    public void assertCanWriteOff(String status) {
        if (isTerminal(status)) {
            throw illegal("writeOff", status, "!" + String.join(" / ", terminalStatuses()));
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    /** receive 的目标态（RECEIVED，initial 写入）。 */
    public String receiveTargetStatus() {
        return ErpFinConstants.NOTES_RECV_RECEIVED;
    }

    /** discount 的目标态（DISCOUNTED）。 */
    public String discountTargetStatus() {
        return ErpFinConstants.NOTES_RECV_DISCOUNTED;
    }

    /** endorse 的目标态（ENDORSED）。 */
    public String endorseTargetStatus() {
        return ErpFinConstants.NOTES_RECV_ENDORSED;
    }

    /** collect 的目标态（COLLECTION_PENDING，在途）。 */
    public String collectTargetStatus() {
        return ErpFinConstants.NOTES_RECV_COLLECTION_PENDING;
    }

    /** honor 的目标态（HONORED，终态）。 */
    public String honorTargetStatus() {
        return ErpFinConstants.NOTES_RECV_HONORED;
    }

    /** dishonor 的目标态（DISHONORED，终态）。 */
    public String dishonorTargetStatus() {
        return ErpFinConstants.NOTES_RECV_DISHONORED;
    }

    /** writeOff 的目标态（WRITE_OFF，终态）。 */
    public String writeOffTargetStatus() {
        return ErpFinConstants.NOTES_RECV_WRITE_OFF;
    }

    // ---------- 终态/初始态分类 ----------

    /** 业务终态判定。应收票据终态为 {@code HONORED}/{@code DISHONORED}/{@code WRITE_OFF}（均无出边）。 */
    public boolean isTerminal(String status) {
        return status != null
                && (ErpFinConstants.NOTES_RECV_HONORED.equals(status)
                || ErpFinConstants.NOTES_RECV_DISHONORED.equals(status)
                || ErpFinConstants.NOTES_RECV_WRITE_OFF.equals(status));
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    /**
     * 迁移矩阵只读快照（7 命名边，每命名动作一条代表边）：
     * receive（RECEIVED→RECEIVED 幂等表示，null initial 写入见 javadoc）/discount/endorse/collect
     * （RECEIVED 代表源，DISCOUNTED 源见 {@link #assertCanCollect(String)}）/honor/dishonor/writeOff
     * （RECEIVED 代表源，全部非终态源见 {@link #assertCanWriteOff(String)}）。多源动作的完整合法来源态
     * 以显式 {@code assertCan*} 方法为准。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("receive",
                        ErpFinConstants.NOTES_RECV_RECEIVED, ErpFinConstants.NOTES_RECV_RECEIVED),
                new TransitionDefinition("discount",
                        ErpFinConstants.NOTES_RECV_RECEIVED, ErpFinConstants.NOTES_RECV_DISCOUNTED),
                new TransitionDefinition("endorse",
                        ErpFinConstants.NOTES_RECV_RECEIVED, ErpFinConstants.NOTES_RECV_ENDORSED),
                new TransitionDefinition("collect",
                        ErpFinConstants.NOTES_RECV_RECEIVED, ErpFinConstants.NOTES_RECV_COLLECTION_PENDING),
                new TransitionDefinition("honor",
                        ErpFinConstants.NOTES_RECV_COLLECTION_PENDING, ErpFinConstants.NOTES_RECV_HONORED),
                new TransitionDefinition("dishonor",
                        ErpFinConstants.NOTES_RECV_COLLECTION_PENDING, ErpFinConstants.NOTES_RECV_DISHONORED),
                new TransitionDefinition("writeOff",
                        ErpFinConstants.NOTES_RECV_RECEIVED, ErpFinConstants.NOTES_RECV_WRITE_OFF)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpFinConstants.NOTES_RECV_HONORED,
                ErpFinConstants.NOTES_RECV_DISHONORED,
                ErpFinConstants.NOTES_RECV_WRITE_OFF));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpFinConstants.NOTES_RECV_RECEIVED);
    }

    // ---------- 内部 ----------

    private static NopException illegal(String action, String currentStatus, String expectedStatus) {
        return new NopException(ErpFinErrors.ERR_NOTES_RECEIVABLE_ILLEGAL_STATUS_TRANSITION)
                .param(ErpFinErrors.ARG_CURRENT_STATUS, currentStatus)
                .param(ErpFinErrors.ARG_EXPECTED_STATUS, expectedStatus)
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
