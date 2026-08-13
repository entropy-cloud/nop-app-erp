package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 会计凭证（{@code ErpFinVoucher}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus}
 * 业务生命周期轴，字典 {@code erp-fin/voucher-status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/finance/state-machine.md} §对象一。
 *
 * <p><b>治理裁定（§11.2 M4 plan-first）</b>：凭证 {@code docStatus} 的 {@code DRAFT→POSTED} 过账核心 +
 * {@code reverseVoucher} 红冲（POSTED 上置 {@code isReversed=true}）属受保护会计过账/红冲行为，且为全域
 * {@code IErpFinAcctDocProvider} 聚合入口。依契约 §11.2 M4 硬约束 (i)–(v)：过账时序/编排/失败回退/红冲闭环
 * 继续由 {@code ErpFinPostingProcessor} 引擎 + {@code isReversed}/{@code docStatus=POSTED} 契约管理（§11.2 M4 (ii)/(v)），
 * Bean 不触碰；跨域副作用（业财回链、7 生成路径）保留原路径（§11.2 M4 (iv)）；{@code posted} 不入轴（§11.2 M4 (iii)，
 * 本实体无独立 {@code posted} boolean 字段，过账状态即 {@code docStatus=POSTED}）。
 *
 * <p>命名带 {@code Document} 后缀（契约 §1 双轴约定，为审批轴预留命名空间）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。
 *
 * <p><b>唯一迁移边</b>：{@code postVoucher} {@code DRAFT→POSTED}。{@code reverseVoucher} 在 POSTED 上置
 * {@code isReversed=true}（保留 POSTED）——<b>非 docStatus 迁移边</b>；其 {@code docStatus==POSTED} 前置守卫
 * 由 {@link #isPosted(String)} 分类 helper 承载（不发明 POSTED→? 边）。
 *
 * <p><b>CANCELLED 死状态</b>（§5.1 已登记）：dict 有 CANCELLED 值但<b>零生产 writer</b>；草稿凭证废弃经
 * {@code useLogicalDelete}（逻辑删除）承载，不经 DRAFT→CANCELLED 迁移（owner doc §对象一 §1/§3/§5）。
 * CANCELLED 不纳入 {@link #initialStatuses()}/{@link #terminalStatuses()}/{@link #transitions()} 任一集合，
 * 为 {@code intentional reserved} 死状态（dict 值保留为未来显式作废工作流的语义入口，successor）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 BizModel（契约 §7）。
 *
 * <p><b>7 生成路径不接线 Bean</b>（契约 §9.2 选项 c 初始态/生成写入，不调 {@code assertCan*}）：
 * {@code ErpFinPostingProcessor}（引擎 persistVoucher 生成即 POSTED）、{@code CloseVoucherWriter}（期末结转）、
 * {@code BudgetVoucherGenerator}（预算凭证）、{@code CommitmentVoucherGenerator}（承付占用/释放）、
 * {@code IntercompanyVoucherGenerator}（内部交易）、{@code ErpFinBudgetScenarioCarryForwardProcessor}（预算结转）、
 * {@code ErpFinConsolidationEliminationPostEliminationProcessor}（合并抵销）。这些生成路径直接写 POSTED/DRAFT，
 * 不经 {@code DRAFT→POSTED} 命名动作，Bean 不覆盖此 §9.2 路径。
 */
public class ErpFinVoucherDocumentStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * postVoucher 目标态守卫：来源态为 {@code DRAFT} 合法（唯一迁移边的来源态）。
     *
     * <p>对非法来源态（POSTED/CANCELLED）报告 common 层非法边（携带 {@code action=postVoucher}/
     * {@code fromStatus}）；接线方 {@code ErpFinVoucherBizModel.postVoucher} 映射为领域码
     * {@code ERR_FIN_VOUCHER_ILLEGAL_TRANSITION}（common 码作 cause 保留）。
     */
    public void assertCanPost(String docStatus) {
        if (!ErpFinConstants.VOUCHER_STATUS_DRAFT.equals(docStatus)) {
            throw illegal("postVoucher", docStatus, ErpFinConstants.VOUCHER_STATUS_DRAFT);
        }
    }

    // ---------- 动作目标态（供 BizModel 写回） ----------

    /** postVoucher 的目标态（POSTED，唯一迁移边）。 */
    public String postVoucherTargetStatus() {
        return ErpFinConstants.VOUCHER_STATUS_POSTED;
    }

    // ---------- 终态/初始态 + 分类 helper ----------

    /**
     * 业务终态判定。凭证据轴终态为 {@code POSTED}（红冲在 POSTED 上置 isReversed=true，保留 POSTED，无出边）。
     *
     * <p>CANCELLED <b>不</b>计入终态（intentional reserved 死状态，零 writer，不活跃）。
     */
    public boolean isTerminal(String docStatus) {
        return ErpFinConstants.VOUCHER_STATUS_POSTED.equals(docStatus);
    }

    /**
     * POSTED 分类 helper：供 {@code reverseVoucher}/{@code previewReverseVoucher} 的前置守卫复用。
     *
     * <p>注意：此 helper 是 isReversed 操作的前置<b>分类</b>（POSTED 才允许置 isReversed=true），
     * <b>非</b> docStatus 迁移边（reverseVoucher 不写 docStatus，POSTED 保留）。
     * CANCELLED 不为 POSTED（死状态，false）。
     */
    public boolean isPosted(String docStatus) {
        return ErpFinConstants.VOUCHER_STATUS_POSTED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 BizModel 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("postVoucher",
                        ErpFinConstants.VOUCHER_STATUS_DRAFT, ErpFinConstants.VOUCHER_STATUS_POSTED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpFinConstants.VOUCHER_STATUS_POSTED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpFinConstants.VOUCHER_STATUS_DRAFT);
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
