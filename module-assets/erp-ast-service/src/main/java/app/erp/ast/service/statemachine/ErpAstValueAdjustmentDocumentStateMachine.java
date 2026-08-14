package app.erp.ast.service.statemachine;

import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.ErpCommonErrors;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 资产价值调整单（{@code ErpAstValueAdjustment}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus} 业务生命周期轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/assets/state-machine.md §适用对象三：资产价值调整文档双轴}（ValueAdjustment 双轴节）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。
 *
 * <p><strong>非退化轴（layer-2 四方对照裁定，plan Phase 3 Proof）</strong>：dict {@code erp/doc-status} 含 3 值
 * （DRAFT/ACTIVE/CANCELLED）。ValueAdjustment 的 docStatus 有 3 个生产 writer（2 个命名动作）：
 * {@code executeApprove:68} + {@code doAutoApprove:270} 写 {@code ACTIVE}（approve/auto-approve 双 writer）、
 * {@code ErpAstValueAdjustmentCancelProcessor:26} 写 {@code CANCELLED}（<b>唯一有独立 cancel mutation</b> 的
 * 资产价值调整文档实体——Disposal/Capitalization 无 cancel mutation，CANCELLED 经 useLogicalDelete 承载）。
 * 故 {@link #transitions()} 含 2 条边：approve(DRAFT→ACTIVE) + cancel(DRAFT→CANCELLED)。
 *
 * <p><strong>终态声明</strong>：{@code ACTIVE}（approve 后业务终态，无出边——reverseApprove 不写 docStatus）+ 
 * {@code CANCELLED}（cancel mutation + useLogicalDelete 可达）纳入 {@link #terminalStatuses()}；初始态
 * {@code DRAFT}（CRUD 创建写入）。
 *
 * <p><strong>守卫接线</strong>：{@code ErpAstValueAdjustmentProcessor.validateTransitionForCancel} 的固定状态守卫
 * （ACTIVE 禁 cancel「非已生效」/CANCELLED 禁 cancel「非已作废」）改调 {@link #assertCanCancel(String)}（common 码作
 * cause → 领域码 {@code ERR_ADJUSTMENT_ILLEGAL_DOC_TRANSITION}，expected 按当前态区分）；<b>posted 动态守卫保留原位</b>
 * （posted=true 拒绝 cancel「非已过账」——posted 不入轴，契约 §3）；{@code validateNotCancelled} 改调
 * {@link #isCancelled(String)} 只读守卫；{@code executeApprove}/{@code doAutoApprove} docStatus 写回改调
 * {@link #approveTargetStatus()}。
 */
public class ErpAstValueAdjustmentDocumentStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * approve 守卫（docStatus 轴）：来源态非 {@code CANCELLED} 合法（已作废单据禁止 approve）。
     *
     * <p>非法来源态（CANCELLED）报告 common 层非法边（携带 {@code action=approve}/{@code fromStatus}）。
     * 接线方 {@code ErpAstValueAdjustmentProcessor.validateNotCancelled}→{@code validateTransitionForCancel}
     * 映射为领域码 {@code ERR_ADJUSTMENT_ILLEGAL_DOC_TRANSITION}（common 码作 cause 保留）。
     */
    public void assertCanApprove(String docStatus) {
        if (isCancelled(docStatus)) {
            throw illegal("approve", docStatus, "非已作废");
        }
    }

    /**
     * cancel 守卫（docStatus 轴）：仅 {@code DRAFT}/{@code null} 来源态合法（唯一有独立 cancel mutation 的实体）。
     *
     * <p>{@code ACTIVE}（「非已生效」）与 {@code CANCELLED}（「非已作废」）均为非法来源态，报告 common 层非法边
     * （携带 {@code action=cancel}/{@code fromStatus}）。接线方 {@code ErpAstValueAdjustmentProcessor.
     * validateTransitionForCancel} 捕获后按当前态映射领域码 {@code ERR_ADJUSTMENT_ILLEGAL_DOC_TRANSITION}，
     * expected 参数区分「非已生效」/「非已作废」（错误码值/参数对外不变）。
     *
     * <p><b>posted 动态守卫不在本 Bean</b>：posted=true 拒绝 cancel（「非已过账」）由接线方原位保留
     * （posted 不入轴，契约 §3）。
     */
    public void assertCanCancel(String docStatus) {
        if (ErpAstConstants.DOC_STATUS_ACTIVE.equals(docStatus)) {
            throw illegal("cancel", docStatus, "非已生效");
        }
        if (isCancelled(docStatus)) {
            throw illegal("cancel", docStatus, "非已作废");
        }
    }

    /** approve/auto-approve 的 docStatus 目标态（ACTIVE）。 */
    public String approveTargetStatus() {
        return ErpAstConstants.DOC_STATUS_ACTIVE;
    }

    /** cancel 的 docStatus 目标态（CANCELLED）。 */
    public String cancelTargetStatus() {
        return ErpAstConstants.DOC_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态 + 分类 helper ----------

    /**
     * 业务终态判定：{@code ACTIVE}（approve 后无出边——reverseApprove 不写 docStatus）或 {@code CANCELLED}
     * （cancel mutation + useLogicalDelete 逻辑删除终态）为终态。{@code DRAFT} 为初始态非终态。
     */
    public boolean isTerminal(String docStatus) {
        return ErpAstConstants.DOC_STATUS_ACTIVE.equals(docStatus)
                || ErpAstConstants.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    /** CANCELLED 分类 helper：只读防御守卫（作废单据禁止审批操作），供 facade cancel 守卫委托。 */
    public boolean isCancelled(String docStatus) {
        return ErpAstConstants.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    /**
     * 迁移矩阵只读快照（2 条命名边）：approve(DRAFT→ACTIVE) + cancel(DRAFT→CANCELLED)。CANCELLED 亦经
     * {@code useLogicalDelete} 可达（非命名动作边，不重复编码）。ACTIVE 无出边（reverseApprove 不写 docStatus）。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("approve", ErpAstConstants.DOC_STATUS_DRAFT, ErpAstConstants.DOC_STATUS_ACTIVE),
                new TransitionDefinition("cancel", ErpAstConstants.DOC_STATUS_DRAFT, ErpAstConstants.DOC_STATUS_CANCELLED)));
    }

    /**
     * 终态集合：{ACTIVE, CANCELLED}（approve 后业务终态 + cancel mutation/useLogicalDelete 终态）。
     */
    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpAstConstants.DOC_STATUS_ACTIVE,
                ErpAstConstants.DOC_STATUS_CANCELLED));
    }

    /**
     * 初始态集合：{DRAFT}（新建经 CRUD 创建写入）。
     */
    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpAstConstants.DOC_STATUS_DRAFT));
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

    // ---------- 内部 ----------

    private static NopException illegal(String action, String currentStatus, String expectedStatus) {
        return new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCommonErrors.ARG_CURRENT_STATUS, currentStatus)
                .param(ErpCommonErrors.ARG_EXPECTED_STATUS, expectedStatus)
                .param(ARG_ACTION, action);
    }
}
