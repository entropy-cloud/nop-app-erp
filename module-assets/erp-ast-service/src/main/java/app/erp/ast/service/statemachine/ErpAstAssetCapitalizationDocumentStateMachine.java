package app.erp.ast.service.statemachine;

import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.ErpCommonErrors;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 资产资本化单（{@code ErpAstAssetCapitalization}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus} 业务生命周期轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/assets/state-machine.md §适用对象三：资产价值调整文档双轴}（Capitalization 双轴节）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。
 *
 * <p><strong>非退化轴 + 特例边（layer-2 四方对照裁定，plan Phase 2 Proof）</strong>：dict {@code erp/doc-status} 含 3 值
 * （DRAFT/ACTIVE/CANCELLED）。Capitalization 的 docStatus 有 2 个生产命名动作 writer：
 * {@code executeApprove:86} 写 {@code ACTIVE} + {@code executeReverseApprove:122} 写 {@code CANCELLED}。
 * 故 {@link #transitions()} 含 2 条边：approve(DRAFT→ACTIVE) + <b>reverseApprove(ACTIVE→CANCELLED)</b>——
 * 后者为 <b>Capitalization Document 轴特例边</b>（Disposal/ValueAdjustment 的 reverseApprove 只写 approveStatus=REJECTED、
 * 不写 docStatus；draft review M2 显式登记本 Bean 不与其他 Document Bean 静默同构）。
 *
 * <p><strong>终态声明</strong>：{@code CANCELLED} 为 docStatus 轴唯一终态（reverseApprove 后可达）；
 * {@code ACTIVE} 为「可逆中间态」——经 reverseApprove 有出边（ACTIVE→CANCELLED），不适用「终态无出边」强断言，
 * 不纳入 {@link #terminalStatuses()}。初始态 {@code DRAFT}（CRUD 创建写入）。{@code CANCELLED} 亦经
 * {@code useLogicalDelete} 既有路径可达（实体 {@code useLogicalDelete="true"} 实仓核实），无独立 cancel mutation。
 *
 * <p><strong>守卫接线</strong>：{@code ErpAstAssetCapitalizationProcessor.validateTransitionForCancel}（doc-cancelled
 * 守卫，供 approve 前置 {@code validateNotCancelled} 共用）改调 {@link #isCancelled(String)} 只读守卫；
 * {@code executeApprove} docStatus 写回改调 {@link #approveTargetStatus()}；{@code executeReverseApprove}
 * docStatus 写回改调 {@link #reverseApproveTargetStatus()}（特例边）。
 */
public class ErpAstAssetCapitalizationDocumentStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * approve 守卫（docStatus 轴）：来源态非 {@code CANCELLED} 合法（已作废单据禁止 approve）。
     *
     * <p>非法来源态（CANCELLED）报告 common 层非法边（携带 {@code action=approve}/{@code fromStatus}）。
     * 接线方 {@code ErpAstAssetCapitalizationProcessor.validateNotCancelled}→{@code validateTransitionForCancel}
     * 映射为领域码 {@code ERR_CAPITALIZATION_ILLEGAL_DOC_TRANSITION}（common 码作 cause 保留）。
     */
    public void assertCanApprove(String docStatus) {
        if (isCancelled(docStatus)) {
            throw illegal("approve", docStatus, "非已作废");
        }
    }

    /**
     * reverseApprove 守卫（docStatus 轴，特例边）：来源态非 {@code CANCELLED} 合法（已作废单据禁止红冲审批）。
     *
     * <p>非法来源态（CANCELLED）报告 common 层非法边（携带 {@code action=reverseApprove}/{@code fromStatus}）。
     */
    public void assertCanReverseApprove(String docStatus) {
        if (isCancelled(docStatus)) {
            throw illegal("reverseApprove", docStatus, "非已作废");
        }
    }

    /** approve 的 docStatus 目标态（ACTIVE）。 */
    public String approveTargetStatus() {
        return ErpAstConstants.DOC_STATUS_ACTIVE;
    }

    /** reverseApprove 的 docStatus 目标态（CANCELLED——Capitalization 特例边，Disposal/ValueAdjustment 无此写）。 */
    public String reverseApproveTargetStatus() {
        return ErpAstConstants.DOC_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态 + 分类 helper ----------

    /**
     * 业务终态判定：仅 {@code CANCELLED} 为终态（reverseApprove→CANCELLED 后可及终态 + useLogicalDelete 逻辑删除终态）。
     * {@code ACTIVE} 为「可逆中间态」——经 reverseApprove 有出边，非终态；{@code DRAFT} 为初始态非终态。
     */
    public boolean isTerminal(String docStatus) {
        return ErpAstConstants.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    /** CANCELLED 分类 helper：只读防御守卫（作废单据禁止审批操作），供 facade cancel 守卫委托。 */
    public boolean isCancelled(String docStatus) {
        return ErpAstConstants.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    /**
     * 迁移矩阵只读快照（2 条命名边）：approve(DRAFT→ACTIVE) + reverseApprove(ACTIVE→CANCELLED)。
     * CANCELLED 亦经 {@code useLogicalDelete} 可达（非命名动作边，不重复编码）。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("approve", ErpAstConstants.DOC_STATUS_DRAFT, ErpAstConstants.DOC_STATUS_ACTIVE),
                new TransitionDefinition("reverseApprove", ErpAstConstants.DOC_STATUS_ACTIVE, ErpAstConstants.DOC_STATUS_CANCELLED)));
    }

    /**
     * 终态集合：{CANCELLED}（reverseApprove 特例边 + useLogicalDelete 可达；ACTIVE 为可逆中间态不纳入）。
     */
    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpAstConstants.DOC_STATUS_CANCELLED));
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
