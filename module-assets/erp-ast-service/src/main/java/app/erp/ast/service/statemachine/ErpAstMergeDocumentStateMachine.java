package app.erp.ast.service.statemachine;

import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.ErpCommonErrors;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 资产合并单（{@code ErpAstMerge}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus} 业务生命周期轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/assets/split-merge.md} + {@code docs/design/assets/state-machine.md §适用对象四}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。
 *
 * <p><strong>非退化轴（layer-2 四方对照裁定，plan Phase 2，镜像 Split Phase 1 Decision (B)）</strong>：
 * dict {@code erp/doc-status} 含 3 值（DRAFT/ACTIVE/CANCELLED）。Merge 的 docStatus 有 1 个生产命名动作 writer：
 * {@code ErpAstMergeProcessor.executeApprove:120} 写 {@code ACTIVE}（approve 是唯一命名 writer）。
 * 故 {@link #transitions()} 含 1 条边：approve(DRAFT→ACTIVE)。{@code CANCELLED} 经独立 cancel mutation
 * （{@code ErpAstMergeCancelProcessor:26} 写 CANCELLED）+ {@code useLogicalDelete} 可达，无独立命名边
 * （cancel 守卫的 ACTIVE/posted 动态条件保留原位）。
 *
 * <p><strong>守卫接线</strong>：{@code ErpAstMergeProcessor.validateTransitionForCancel}（doc-cancelled 守卫，
 * 供 approve 前置 {@code validateNotCancelled} 与 5 个审批动作共用）的 CANCELLED 判定改调
 * {@link #isCancelled(String)} 只读守卫（ACTIVE「非已生效」+ posted「非已过账」动态条件保留原位）；
 * {@code executeApprove} 的 docStatus 写回改调 {@link #approveTargetStatus()}。
 *
 * <p><strong>终态声明</strong>：{@code ACTIVE}（approve 后业务终态，无出边）+ {@code CANCELLED}（经
 * cancel mutation / {@code useLogicalDelete} 可达）纳入 {@link #terminalStatuses()}；初始态 {@code DRAFT}
 * （CRUD 创建写入）。
 */
public class ErpAstMergeDocumentStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * approve 守卫（docStatus 轴）：来源态非 {@code CANCELLED} 合法（已作废单据禁止 approve）。
     *
     * <p>非法来源态（CANCELLED）报告 common 层非法边（携带 {@code action=approve}/{@code fromStatus}）。
     * 接线方 {@code ErpAstMergeProcessor.validateTransitionForCancel}（doc-cancelled 守卫）映射为领域码
     * {@code ERR_AST_MERGE_ILLEGAL_DOC_TRANSITION}（common 码作 cause 保留）。
     */
    public void assertCanApprove(String docStatus) {
        if (isCancelled(docStatus)) {
            throw illegal("approve", docStatus, "非已作废");
        }
    }

    /** approve 的 docStatus 目标态（ACTIVE，唯一命名 writer 边）。 */
    public String approveTargetStatus() {
        return ErpAstConstants.DOC_STATUS_ACTIVE;
    }

    // ---------- 终态/初始态 + 分类 helper ----------

    /**
     * 业务终态判定：{@code ACTIVE}（approve 后无出边）或 {@code CANCELLED}（cancel mutation /
     * useLogicalDelete 逻辑删除终态）为终态。{@code DRAFT} 为初始态非终态。
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
     * 迁移矩阵只读快照（1 条命名边）：approve(DRAFT→ACTIVE)。CANCELLED 经 cancel mutation /
     * {@code useLogicalDelete} 可达（cancel 守卫动态条件保留原位，非纯命名边，不编码入 transitions()）；
     * ACTIVE 无出边（reverseApprove 不写 docStatus）。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("approve", ErpAstConstants.DOC_STATUS_DRAFT, ErpAstConstants.DOC_STATUS_ACTIVE)));
    }

    /**
     * 终态集合：{ACTIVE, CANCELLED}（approve 后业务终态 + cancel mutation / 逻辑删除终态）。
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
