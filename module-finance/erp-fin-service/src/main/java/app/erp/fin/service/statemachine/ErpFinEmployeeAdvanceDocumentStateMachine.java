package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 员工借款单（{@code ErpFinEmployeeAdvance}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus}
 * 业务生命周期轴，字典 {@code erp-fin/advance-status} 5 值：DRAFT/SUBMITTED/APPROVED/REJECTED/CANCELLED）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/finance/state-machine.md} §对象六 + {@code docs/design/finance/expense-claim.md}。
 *
 * <p><b>治理裁定（§11.2 M4 plan-first，plan 2026-08-13-1146-3）</b>：借款 cancel 在已过账时触发红冲
 * （{@code postingDispatcher.reverse} 红字冲销闭环，自动红冲已过账凭证而非要求先反审核）。
 * 依契约 §11.2 M4 硬约束 (i)–(v)：红冲闭环/失败兜底继续由过账编排 + {@code posted} 标志契约管理（§11.2 M4 (ii)/(v)），
 * Bean 不触碰；{@code posted} 不入轴（§11.2 M4 (iii)）；跨域副作用保留原 Processor 路径（§11.2 M4 (iv)）。
 *
 * <p>命名带 {@code Document} 后缀（契约 §1 双轴约定，与 {@code ErpFinEmployeeAdvanceApprovalStateMachine} approveStatus
 * 轴分离）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。
 *
 * <p><b>唯一迁移边</b>：{@code cancel} {非 CANCELLED}→CANCELLED（{@link #assertCanCancel(String)} 校验
 * {@code !isCancelled(from)}，已 CANCELLED 则拒绝）。生命周期推进由 approveStatus 轴承载。
 *
 * <p><b>docStatus 残余值（Decision，plan Phase 1/Phase 3）</b>：dict 5 值但代码仅写 DRAFT（seed）与 CANCELLED
 * （cancel）——SUBMITTED/APPROVED/REJECTED 为 {@code intentional reserved} 残余值（workflow 轴 = approveStatus，
 * docStatus 仅 DRAFT→CANCELLED）。残余值<b>不纳入</b> {@link #initialStatuses()}/{@link #terminalStatuses()}/
 * {@link #transitions()} 任一集合，dict 项保留不删（dict 治理归 successor）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 */
public class ErpFinEmployeeAdvanceDocumentStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * cancel 入口守卫：来源态为<b>任意非 CANCELLED</b>合法（{@code !isCancelled(from)}，loose 语义，expected「非已作废」）。
     *
     * <p>非法来源态（CANCELLED）报告 common 层非法边（携带 {@code action=cancel}/{@code fromStatus}）。
     * 接线方 {@code ErpFinEmployeeAdvanceProcessor.validateTransitionForCancel} 映射为领域码
     * {@code ERR_EMPLOYEE_ADVANCE_ILLEGAL_DOC_STATUS_TRANSITION}（common 码作 cause 保留）。
     */
    public void assertCanCancel(String docStatus) {
        if (isCancelled(docStatus)) {
            throw illegal("cancel", docStatus, "非已作废");
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    /** cancel 的目标态（CANCELLED，唯一迁移边）。 */
    public String cancelTargetStatus() {
        return ErpFinConstants.DOC_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态 + 分类 helper ----------

    /**
     * 业务终态判定。docStatus 轴终态为 {@code CANCELLED}（无出边）。
     *
     * <p>残余值 SUBMITTED/APPROVED/REJECTED <b>不</b>计入终态（intentional reserved，生命周期推进由 approveStatus 承载）。
     */
    public boolean isTerminal(String docStatus) {
        return ErpFinConstants.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    /** CANCELLED 分类 helper：供 cancel 守卫复用（已作废则拒绝）。 */
    public boolean isCancelled(String docStatus) {
        return ErpFinConstants.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    /**
     * 迁移矩阵只读快照（1 命名边）：cancel DRAFT→CANCELLED（代表边；实际合法来源态 = 任意非 CANCELLED，
     * 以显式 {@code assertCanCancel} 为准）。残余值 SUBMITTED/APPROVED/REJECTED 不在任何边中。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("cancel", ErpFinConstants.DOC_STATUS_DRAFT, ErpFinConstants.DOC_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.singletonList(ErpFinConstants.DOC_STATUS_CANCELLED);
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpFinConstants.DOC_STATUS_DRAFT);
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
