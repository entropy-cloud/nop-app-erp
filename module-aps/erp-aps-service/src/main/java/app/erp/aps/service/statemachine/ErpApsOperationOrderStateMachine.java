package app.erp.aps.service.statemachine;

import app.erp.aps.service.ErpApsConstants;
import app.erp.common.service.ErpCommonErrors;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 工序工单（{@code ErpApsOperationOrder}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/aps/state-machine.md}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载**已实现**迁移矩阵
 * （DRAFT/PLANNED/IN_PROGRESS/FINISHED/CANCELLED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 BizModel/Processor（契约 §7）。
 *
 * <p>迁移矩阵（7 条边，按 per-(action, fromStatus, toStatus) 三元组）：
 * <ul>
 *   <li>start(PLANNED→IN_PROGRESS)；</li>
 *   <li>complete(IN_PROGRESS→FINISHED)；</li>
 *   <li>cancel 多源 {DRAFT→CANCELLED, PLANNED→CANCELLED, IN_PROGRESS→CANCELLED}
 *       （草稿废弃 / 已排程取消 / 异常终止三源，正向枚举合法来源）；</li>
 *   <li>revertToDraft(PLANNED→DRAFT)（插单区间重排回退路径矩阵权威，{@code ErpApsSchedulingInsertRushOrderProcessor}
 *       所选低优先级 PLANNED 工序回退时调用）；</li>
 *   <li>DRAFT→PLANNED（APS 排产引擎驱动，**无 assertCan 守卫**——引擎按可行性写状态，无可集中守卫；
 *       仅作 {@link #transitions()} 声明边供可达性分析，非命名动作路径）。</li>
 * </ul>
 *
 * <p><strong>引擎边界裁定（plan 2026-08-12-2142-3 Phase 3 Decision）</strong>：{@code ErpApsSchedulingEngine}
 * 的 DRAFT↔PLANNED 写是纯算法 POJO 按可行性写状态（无 status 守卫可集中），其调用方 {@code ErpApsSchedulingProcessor.persist}
 * 的容量预留获取/释放是强一致约束（保留不动）。本 Bean {@link #transitions()} 声明 DRAFT→PLANNED 边仅供可达性分析，
 * **不路由引擎写经 Bean**（无可集中守卫）——登记为 intentional architecture boundary（引擎=算法状态，Bean=命名动作矩阵）。
 *
 * <p><strong>动态业务守卫边界（保留 Processor，本 Bean 不承载）</strong>：IN_PROGRESS 工序不可重排硬守卫
 * （{@code ERR_APS_OP_IN_PROGRESS_NOT_RESCHEDULABLE}）、插单优先级选择规则、容量预留获取/释放
 * （{@code ErpApsSchedulingProcessor}）、{@code requireOrder} 实体加载、乐观锁。
 *
 * <p><strong>Deferred 项（本 Bean 不实现，保持行为）</strong>：
 * <ul>
 *   <li>start/complete/cancel 不释放容量预留（owner doc §4 {@code :58} P1-MA2-077 MR1）；仅 PLANNED→DRAFT
 *       抢单回退路径释放（{@code InsertRushOrderProcessor.releaseReservationsByOrder}）。</li>
 *   <li>IN_PROGRESS cancel 审批工作流（owner doc §6 {@code :77-79} P1-MA2-078）未落地，今日 cancel 仅经
 *       entry-permission overlay 门控。</li>
 * </ul>
 */
public class ErpApsOperationOrderStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanStart(String status) {
        if (!ErpApsConstants.OP_STATUS_PLANNED.equals(status)) {
            throw illegal("start", status, ErpApsConstants.OP_STATUS_PLANNED);
        }
    }

    public String startTargetStatus() {
        return ErpApsConstants.OP_STATUS_IN_PROGRESS;
    }

    public void assertCanComplete(String status) {
        if (!ErpApsConstants.OP_STATUS_IN_PROGRESS.equals(status)) {
            throw illegal("complete", status, ErpApsConstants.OP_STATUS_IN_PROGRESS);
        }
    }

    public String completeTargetStatus() {
        return ErpApsConstants.OP_STATUS_FINISHED;
    }

    /**
     * cancel 守卫：接受 DRAFT（草稿废弃）/ PLANNED（已排程取消）/ IN_PROGRESS（异常终止）三类源态。
     *
     * <p>对齐 {@code state-machine.md §2/§3}：三源→CANCELLED 终态。FINISHED/CANCELLED 终态非法。
     */
    public void assertCanCancel(String status) {
        if (!ErpApsConstants.OP_STATUS_DRAFT.equals(status)
                && !ErpApsConstants.OP_STATUS_PLANNED.equals(status)
                && !ErpApsConstants.OP_STATUS_IN_PROGRESS.equals(status)) {
            throw illegal("cancel", status,
                    ErpApsConstants.OP_STATUS_DRAFT + "/" + ErpApsConstants.OP_STATUS_PLANNED
                            + "/" + ErpApsConstants.OP_STATUS_IN_PROGRESS);
        }
    }

    public String cancelTargetStatus() {
        return ErpApsConstants.OP_STATUS_CANCELLED;
    }

    /**
     * revertToDraft 守卫：仅 PLANNED 合法（插单区间重排回退路径矩阵权威）。
     *
     * <p>对齐 {@code state-machine.md §2 :28/§5 :64}：PLANNED→DRAFT 回退路径合法且必要（重排场景）。
     * 调用方 {@code ErpApsSchedulingInsertRushOrderProcessor} 在所选低优先级 PLANNED 工序回退前调用本方法
     * 确认矩阵合法性（所选 op 本为 PLANNED，调用为矩阵权威确认）。IN_PROGRESS 不可重排硬守卫
     * （{@code ERR_APS_OP_IN_PROGRESS_NOT_RESCHEDULABLE}）保留在 Processor 选择之前，非本 Bean 范围。
     */
    public void assertCanRevertToDraft(String status) {
        if (!ErpApsConstants.OP_STATUS_PLANNED.equals(status)) {
            throw illegal("revertToDraft", status, ErpApsConstants.OP_STATUS_PLANNED);
        }
    }

    public String revertToDraftTargetStatus() {
        return ErpApsConstants.OP_STATUS_DRAFT;
    }

    /**
     * hold 守卫：仅 PLANNED 合法（RC-R1.88，auto-dispatch.md §3.3 派工保持——计划员暂不派工）。
     */
    public void assertCanHold(String status) {
        if (!ErpApsConstants.OP_STATUS_PLANNED.equals(status)) {
            throw illegal("hold", status, ErpApsConstants.OP_STATUS_PLANNED);
        }
    }

    public String holdTargetStatus() {
        return ErpApsConstants.OP_STATUS_HOLD;
    }

    /**
     * unhold 守卫：HOLD（人工保持）/ ON_HOLD（缺料系统暂停）合法（auto-dispatch.md §3.3 解除保持
     * → 重新进入自动派工检查循环）。
     */
    public void assertCanUnhold(String status) {
        if (!ErpApsConstants.OP_STATUS_HOLD.equals(status)
                && !ErpApsConstants.OP_STATUS_ON_HOLD.equals(status)) {
            throw illegal("unhold", status,
                    ErpApsConstants.OP_STATUS_HOLD + "/" + ErpApsConstants.OP_STATUS_ON_HOLD);
        }
    }

    public String unholdTargetStatus() {
        return ErpApsConstants.OP_STATUS_PLANNED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类：FINISHED/CANCELLED。
     *
     * <p>对齐 {@code state-machine.md §3 :46-49}：终态不可直接恢复，须重建 OperationOrder。
     */
    public boolean isTerminal(String status) {
        return ErpApsConstants.OP_STATUS_FINISHED.equals(status)
                || ErpApsConstants.OP_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    /**
     * 返回不可变快照，声明全部 13 条已实现边（按 per-(action, fromStatus, toStatus) 三元组）。
     *
     * <p>含引擎驱动边（DRAFT→PLANNED {@code schedule}、UNSCHEDULABLE→PLANNED 自愈重排 {@code schedule}、
     * DRAFT→UNSCHEDULABLE {@code markUnschedulable}、PLANNED→ON_HOLD 缺料暂停 {@code shortageHold}——
     * 引擎按可行性写状态无可集中守卫，仅作可达性分析声明边）；RC-R1.88 新增 hold/unhold 命名动作边
     * （PLANNED↔HOLD 保持语义 + HOLD/ON_HOLD→PLANNED 解除保持）。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("schedule",
                        ErpApsConstants.OP_STATUS_DRAFT, ErpApsConstants.OP_STATUS_PLANNED),
                new TransitionDefinition("schedule",
                        ErpApsConstants.OP_STATUS_UNSCHEDULABLE, ErpApsConstants.OP_STATUS_PLANNED),
                new TransitionDefinition("markUnschedulable",
                        ErpApsConstants.OP_STATUS_DRAFT, ErpApsConstants.OP_STATUS_UNSCHEDULABLE),
                new TransitionDefinition("start",
                        ErpApsConstants.OP_STATUS_PLANNED, ErpApsConstants.OP_STATUS_IN_PROGRESS),
                new TransitionDefinition("complete",
                        ErpApsConstants.OP_STATUS_IN_PROGRESS, ErpApsConstants.OP_STATUS_FINISHED),
                new TransitionDefinition("cancel",
                        ErpApsConstants.OP_STATUS_DRAFT, ErpApsConstants.OP_STATUS_CANCELLED),
                new TransitionDefinition("cancel",
                        ErpApsConstants.OP_STATUS_PLANNED, ErpApsConstants.OP_STATUS_CANCELLED),
                new TransitionDefinition("cancel",
                        ErpApsConstants.OP_STATUS_IN_PROGRESS, ErpApsConstants.OP_STATUS_CANCELLED),
                new TransitionDefinition("revertToDraft",
                        ErpApsConstants.OP_STATUS_PLANNED, ErpApsConstants.OP_STATUS_DRAFT),
                new TransitionDefinition("hold",
                        ErpApsConstants.OP_STATUS_PLANNED, ErpApsConstants.OP_STATUS_HOLD),
                new TransitionDefinition("unhold",
                        ErpApsConstants.OP_STATUS_HOLD, ErpApsConstants.OP_STATUS_PLANNED),
                new TransitionDefinition("unhold",
                        ErpApsConstants.OP_STATUS_ON_HOLD, ErpApsConstants.OP_STATUS_PLANNED),
                new TransitionDefinition("shortageHold",
                        ErpApsConstants.OP_STATUS_PLANNED, ErpApsConstants.OP_STATUS_ON_HOLD)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpApsConstants.OP_STATUS_FINISHED, ErpApsConstants.OP_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpApsConstants.OP_STATUS_DRAFT);
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
