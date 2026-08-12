package app.erp.md.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.md.service.ErpMdConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 供应商准入资格（{@code ErpMdSupplierApproval}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义（跨域 owner doc）：{@code docs/design/purchase/supplier-evaluation.md §状态机}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载**已实现**迁移矩阵
 * （APPLIED/APPROVED/PROBATION/SUSPENDED/REJECTED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 BizModel/Processor（契约 §7）。
 *
 * <p>迁移矩阵（10 条边，仅编码命名动作路径下**已落地**的 writer）：
 * <ul>
 *   <li>apply(null→APPLIED)（新建申请）、apply(REJECTED→APPLIED)（驳回后重新申请）；</li>
 *   <li>approve(APPLIED→APPROVED)、approve(PROBATION→APPROVED)（多源：正式准入 + 试用通过）；</li>
 *   <li>probate(APPROVED→PROBATION)（新供应商试用期）；</li>
 *   <li>suspend(APPLIED→SUSPENDED)、suspend(APPROVED→SUSPENDED)、suspend(PROBATION→SUSPENDED)
 *       （多源：评分 standing=RED 触发；幂等「已 SUSPENDED」短路留 BizModel/Processor，不进 Bean）；</li>
 *   <li>reinstate(SUSPENDED→APPROVED)（恢复，需审批）；</li>
 *   <li>reject(APPLIED→REJECTED)（驳回）。</li>
 * </ul>
 *
 * <p><strong>REJECTED 可恢复性</strong>：owner doc {@code supplier-evaluation.md §状态机} 声明 {@code APPLIED→REJECTED}，
 * 但 apply 接受 REJECTED 为源（重新申请），即 {@code REJECTED→APPLIED} 是已落地命名动作边。故 REJECTED 是**可恢复准终态**
 * 而非严格不可恢复终态 —— {@link #isTerminal(String)} 对全部 dict 值返回 false、{@link #terminalStatuses()} 返回空集。
 * 此事实在 plan 2026-08-12-2142-1 层 2 Decision 登记。
 */
public class ErpMdSupplierApprovalStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * apply 守卫：接受 {@code null}（新建）与 {@code REJECTED}（驳回后重新申请）两类源态。
     *
     * <p>对齐 {@code supplier-evaluation.md §状态机}：apply 是 AVL 准入状态机的入口动作。{@code null} 视为
     * 「新建前的虚拟初始态」（实体尚未写入 status），REJECTED 视为「驳回后允许重新申请」。其余状态
     * （APPLIED/APPROVED/PROBATION/SUSPENDED）非法。
     */
    public void assertCanApply(String status) {
        if (status != null && !ErpMdConstants.APPROVAL_STATUS_REJECTED.equals(status)) {
            throw illegal("apply", status, "null 或 REJECTED");
        }
    }

    public String applyTargetStatus() {
        return ErpMdConstants.APPROVAL_STATUS_APPLIED;
    }

    /**
     * approve 守卫：接受 APPLIED（正式准入）+ PROBATION（试用通过）两类源态。
     *
     * <p>对齐 {@code supplier-evaluation.md §状态机}：{@code APPLIED→APPROVED} 与 {@code PROBATION→APPROVED}。
     * 其余状态（含 REJECTED/SUSPENDED/APPROVED）非法。
     */
    public void assertCanApprove(String status) {
        if (!ErpMdConstants.APPROVAL_STATUS_APPLIED.equals(status)
                && !ErpMdConstants.APPROVAL_STATUS_PROBATION.equals(status)) {
            throw illegal("approve", status,
                    ErpMdConstants.APPROVAL_STATUS_APPLIED + "/" + ErpMdConstants.APPROVAL_STATUS_PROBATION);
        }
    }

    public String approveTargetStatus() {
        return ErpMdConstants.APPROVAL_STATUS_APPROVED;
    }

    public void assertCanProbate(String status) {
        if (!ErpMdConstants.APPROVAL_STATUS_APPROVED.equals(status)) {
            throw illegal("probate", status, ErpMdConstants.APPROVAL_STATUS_APPROVED);
        }
    }

    public String probateTargetStatus() {
        return ErpMdConstants.APPROVAL_STATUS_PROBATION;
    }

    /**
     * suspend 守卫：接受 APPLIED + APPROVED + PROBATION 三类源态（多源）。
     *
     * <p>对齐 {@code supplier-evaluation.md §状态机}：{@code APPROVED/PROBATION→SUSPENDED}（评分 standing=RED 触发）；
     * {@code APPLIED→SUSPENDED} 为既有实现补充边（申请中也可暂停）。其余状态非法。
     *
     * <p>幂等「已 SUSPENDED」短路不进 Bean：BizModel/Processor 在调本方法前已对 SUSPENDED 态直接 return（不抛），
     * 故本方法无需也**不应**放行 SUSPENDED（若到达此处说明调用方未做幂等短路，按非法边报告）。
     */
    public void assertCanSuspend(String status) {
        if (!ErpMdConstants.APPROVAL_STATUS_APPLIED.equals(status)
                && !ErpMdConstants.APPROVAL_STATUS_APPROVED.equals(status)
                && !ErpMdConstants.APPROVAL_STATUS_PROBATION.equals(status)) {
            throw illegal("suspend", status,
                    ErpMdConstants.APPROVAL_STATUS_APPLIED + "/"
                            + ErpMdConstants.APPROVAL_STATUS_APPROVED + "/"
                            + ErpMdConstants.APPROVAL_STATUS_PROBATION);
        }
    }

    public String suspendTargetStatus() {
        return ErpMdConstants.APPROVAL_STATUS_SUSPENDED;
    }

    public void assertCanReinstate(String status) {
        if (!ErpMdConstants.APPROVAL_STATUS_SUSPENDED.equals(status)) {
            throw illegal("reinstate", status, ErpMdConstants.APPROVAL_STATUS_SUSPENDED);
        }
    }

    public String reinstateTargetStatus() {
        return ErpMdConstants.APPROVAL_STATUS_APPROVED;
    }

    public void assertCanReject(String status) {
        if (!ErpMdConstants.APPROVAL_STATUS_APPLIED.equals(status)) {
            throw illegal("reject", status, ErpMdConstants.APPROVAL_STATUS_APPLIED);
        }
    }

    public String rejectTargetStatus() {
        return ErpMdConstants.APPROVAL_STATUS_REJECTED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类。
     *
     * <p>REJECTED 经 apply 可恢复（{@code REJECTED→APPLIED} 是已落地命名动作边），故 REJECTED 是**可恢复准终态**
     * 而非严格不可恢复终态 —— 本方法对全部 dict 值返回 false。层 2 Decision 登记此事实。
     */
    public boolean isTerminal(String status) {
        return false;
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("apply", null, ErpMdConstants.APPROVAL_STATUS_APPLIED),
                new TransitionDefinition("apply", ErpMdConstants.APPROVAL_STATUS_REJECTED, ErpMdConstants.APPROVAL_STATUS_APPLIED),
                new TransitionDefinition("approve", ErpMdConstants.APPROVAL_STATUS_APPLIED, ErpMdConstants.APPROVAL_STATUS_APPROVED),
                new TransitionDefinition("approve", ErpMdConstants.APPROVAL_STATUS_PROBATION, ErpMdConstants.APPROVAL_STATUS_APPROVED),
                new TransitionDefinition("probate", ErpMdConstants.APPROVAL_STATUS_APPROVED, ErpMdConstants.APPROVAL_STATUS_PROBATION),
                new TransitionDefinition("suspend", ErpMdConstants.APPROVAL_STATUS_APPLIED, ErpMdConstants.APPROVAL_STATUS_SUSPENDED),
                new TransitionDefinition("suspend", ErpMdConstants.APPROVAL_STATUS_APPROVED, ErpMdConstants.APPROVAL_STATUS_SUSPENDED),
                new TransitionDefinition("suspend", ErpMdConstants.APPROVAL_STATUS_PROBATION, ErpMdConstants.APPROVAL_STATUS_SUSPENDED),
                new TransitionDefinition("reinstate", ErpMdConstants.APPROVAL_STATUS_SUSPENDED, ErpMdConstants.APPROVAL_STATUS_APPROVED),
                new TransitionDefinition("reject", ErpMdConstants.APPROVAL_STATUS_APPLIED, ErpMdConstants.APPROVAL_STATUS_REJECTED)));
    }

    /**
     * 终态集合。
     *
     * <p>返回空集：REJECTED 经 apply 可恢复（非严格终态），其余状态均有出边。层 2 Decision 登记此事实。
     */
    public List<String> terminalStatuses() {
        return Collections.emptyList();
    }

    /**
     * 初始态集合：APPLIED。
     *
     * <p>{@code null} 视为「新建前的虚拟初始态」（实体尚未写入 status），apply 接受 null 为源。
     */
    public List<String> initialStatuses() {
        return Collections.singletonList(ErpMdConstants.APPROVAL_STATUS_APPLIED);
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
