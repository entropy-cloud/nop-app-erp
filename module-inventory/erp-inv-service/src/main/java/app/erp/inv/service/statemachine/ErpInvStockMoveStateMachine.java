package app.erp.inv.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.inv.dao.constants.ErpInvDocStatus;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 库存移动单（{@code ErpInvStockMove}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus}
 * 业务生命周期轴，字典 {@code erp-inv/move-status}，4 值 DRAFT/CONFIRMED/DONE/CANCELLED）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/inventory/state-machine.md} §适用对象。
 *
 * <p><b>治理裁定（§11.2 M4）</b>：DONE 触发存货过账事件（{@code InvPostingExecutor}→{@code IErpFinVoucherBiz.post}
 * 存货估值凭证）+ 库存强一致（余额/预留量经 {@code StockMoveBookkeeper}）。依契约 §11.2 M4 (ii)/(iv)/(v)，
 * 过账时序/失败回退（posted 回写）/红冲闭环、记账、批次效期拦截、可用量校验、预留量占用/释放<b>原序保留</b>在
 * Processor/`I*Biz` 路径；{@code posted}（boolean）不入轴（契约 §3）。本 Bean 为 M4 plan-first 产物。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 3 命名动作迁移矩阵
 * （confirm/complete/cancel）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（4 条边）：confirm(DRAFT→CONFIRMED)、complete(CONFIRMED→DONE)、
 * cancel 多源 {DRAFT, CONFIRMED}→CANCELLED = 2 边。分类 initial={DRAFT}、terminal={DONE, CANCELLED}。
 *
 * <p><b>生成路径无迁移边（契约 §8/§9.2 选项 c）</b>：
 * <ul>
 *   <li>{@code reverse}（{@code ErpInvStockMoveReverseProcessor}）= 生成反向移动单（新 DRAFT，数量取负），
 *       <b>不改原单 docStatus</b>（owner doc §3 明确：DONE 的「冲销」是生成新单非状态回退）→ 在本 Bean 中无迁移边。</li>
 *   <li>{@code generateMove}（{@code ErpInvStockMoveGenerateMoveProcessor}）= 生成路径（创建新移动单 seed DRAFT，
 *       初始态写入），不调 {@code assertCan*}。</li>
 * </ul>
 *
 * <p>动态守卫边界（保留 Processor）：可用量校验（{@code validateAvailable}）、批次效期拦截（config-gated
 * {@code validateBatchExpiry}）、预留量占用/释放（{@code applyReservation}/{@code releaseReservation}）、
 * 记账（{@code StockMoveBookkeeper.bookCompletion}）、过账派发（{@code InvPostingDispatcher}，失败保持 DONE +
 * {@code posted=false}）、乐观锁 retry-on-conflict——不属于状态轴判断，本 Bean 不承载。
 */
public class ErpInvStockMoveStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * confirm 守卫：来源态为 {@code DRAFT} 合法。
     *
     * <p>非法来源态报告 common 层非法边（携带 {@code action=confirm}/{@code fromStatus}）。
     * 接线方 {@code ErpInvStockMoveProcessor.doConfirm} 映射为领域码 {@code ERR_ILLEGAL_STATUS_TRANSITION}
     * （保持既有 expected=DRAFT 文案）。
     */
    public void assertCanConfirm(String docStatus) {
        if (!ErpInvDocStatus.DOC_STATUS_DRAFT.equals(docStatus)) {
            throw illegal("confirm", docStatus, ErpInvDocStatus.DOC_STATUS_DRAFT);
        }
    }

    /**
     * complete 守卫：来源态为 {@code CONFIRMED} 合法。
     *
     * <p>接线方 {@code ErpInvStockMoveProcessor.doComplete} 映射为领域码 {@code ERR_ILLEGAL_STATUS_TRANSITION}
     * （保持既有 expected="CONFIRMED" 文案）。
     */
    public void assertCanComplete(String docStatus) {
        if (!ErpInvDocStatus.DOC_STATUS_CONFIRMED.equals(docStatus)) {
            throw illegal("complete", docStatus, ErpInvDocStatus.DOC_STATUS_CONFIRMED);
        }
    }

    /**
     * cancel 守卫：来源态为 {@code DRAFT} 或 {@code CONFIRMED} 合法。
     *
     * <p>接线方 {@code ErpInvStockMoveCancelProcessor.cancel} 映射为领域码 {@code ERR_ILLEGAL_STATUS_TRANSITION}
     * （保持既有 expected="DRAFT或CONFIRMED" 文案）。
     */
    public void assertCanCancel(String docStatus) {
        if (!ErpInvDocStatus.DOC_STATUS_DRAFT.equals(docStatus)
                && !ErpInvDocStatus.DOC_STATUS_CONFIRMED.equals(docStatus)) {
            throw illegal("cancel", docStatus, "DRAFT或CONFIRMED");
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    public String confirmTargetStatus() {
        return ErpInvDocStatus.DOC_STATUS_CONFIRMED;
    }

    public String completeTargetStatus() {
        return ErpInvDocStatus.DOC_STATUS_DONE;
    }

    public String cancelTargetStatus() {
        return ErpInvDocStatus.DOC_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定。移动单终态为 {@code DONE} 与 {@code CANCELLED}（均无后续出边——DONE 的「冲销」是生成反向新单
     * 非状态迁移）。
     */
    public boolean isTerminal(String docStatus) {
        return ErpInvDocStatus.DOC_STATUS_DONE.equals(docStatus)
                || ErpInvDocStatus.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("confirm", ErpInvDocStatus.DOC_STATUS_DRAFT, ErpInvDocStatus.DOC_STATUS_CONFIRMED),
                new TransitionDefinition("complete", ErpInvDocStatus.DOC_STATUS_CONFIRMED, ErpInvDocStatus.DOC_STATUS_DONE),
                new TransitionDefinition("cancel", ErpInvDocStatus.DOC_STATUS_DRAFT, ErpInvDocStatus.DOC_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpInvDocStatus.DOC_STATUS_CONFIRMED, ErpInvDocStatus.DOC_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpInvDocStatus.DOC_STATUS_DONE, ErpInvDocStatus.DOC_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpInvDocStatus.DOC_STATUS_DRAFT);
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
