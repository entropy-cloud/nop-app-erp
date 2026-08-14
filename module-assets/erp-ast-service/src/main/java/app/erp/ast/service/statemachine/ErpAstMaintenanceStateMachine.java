package app.erp.ast.service.statemachine;

import app.erp.ast.service.ErpAstConstants;
import app.erp.common.service.ErpCommonErrors;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 资产维修工单（{@code ErpAstMaintenance}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status} 生命周期轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/assets/maintenance.md §一状态机}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载维修工单生命周期
 * 迁移矩阵 + 终态/初始态分类 + 只读 {@link #transitions()} 元数据。可经 Delta 同名 Bean 覆盖（契约 §6）
 * 替换基线矩阵。
 *
 * <p>迁移矩阵（7 命名动作）：create(DRAFT→DRAFT，创建种子初始态)、submit(DRAFT→SUBMITTED)、
 * startWork(SUBMITTED→IN_PROGRESS)、completeWork(IN_PROGRESS→COMPLETED)、post(COMPLETED→POSTED，终态)、
 * cancel(DRAFT/SUBMITTED→CANCELLED，终态)、reverse(POSTED→COMPLETED，回卷非终态)。
 * decideTreatment/approve <strong>不迁移</strong>——守卫 COMPLETED/POSTED 为动态业务守卫，保留在 Processor 原位。
 *
 * <p><strong>create 动作形态（layer-2 四方对照裁定）</strong>：create 为 {@code §9.2 创建种子} 初始态写入
 * （{@code createMaintenance} 建单写 DRAFT），故 {@link #transitions()} 不含 create 自环边；DRAFT 以
 * {@link #initialStatuses()} 登记。
 *
 * <p><strong>status POSTED 与 {@code posted} boolean（§11.2 M4 (iii)）</strong>：POSTED 为 status 轴的终态值
 * （单据生命周期态）；{@code posted}（boolean）为过账契约标志，两个独立字段，{@code posted} 不入轴——
 * 红冲副作用归 {@code MaintenanceExpensePostingDispatcher}/{@code MaintenanceCapitalizationPostingDispatcher}
 * 原位，本 Bean 只管 status 轴。reverse 为<strong>回卷迁移</strong>（POSTED→COMPLETED，非终态）入矩阵。
 *
 * <p>分类：initial={DRAFT}，terminal={POSTED, CANCELLED}。非法边抛 common 层
 * {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}；调用方 Processor 捕获后 cause-chain 映射为领域码
 * {@code ERR_AST_MAINTENANCE_ILLEGAL_STATUS_TRANSITION}（契约 §7）。
 */
public class ErpAstMaintenanceStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * create 守卫：来源态为 {@code DRAFT}/{@code null} 合法（建单创建种子）。
     */
    public void assertCanCreate(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.MAINTENANCE_STATUS_DRAFT.equals(s)) {
            throw illegal("create", s, ErpAstConstants.MAINTENANCE_STATUS_DRAFT);
        }
    }

    /**
     * submit 守卫：来源态为 {@code DRAFT} 合法。
     */
    public void assertCanSubmit(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.MAINTENANCE_STATUS_DRAFT.equals(s)) {
            throw illegal("submit", s, ErpAstConstants.MAINTENANCE_STATUS_DRAFT);
        }
    }

    /**
     * startWork 守卫：来源态为 {@code SUBMITTED} 合法。
     */
    public void assertCanStartWork(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED.equals(s)) {
            throw illegal("startWork", s, ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED);
        }
    }

    /**
     * completeWork 守卫：来源态为 {@code IN_PROGRESS} 合法。
     */
    public void assertCanCompleteWork(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.MAINTENANCE_STATUS_IN_PROGRESS.equals(s)) {
            throw illegal("completeWork", s, ErpAstConstants.MAINTENANCE_STATUS_IN_PROGRESS);
        }
    }

    /**
     * post 守卫：来源态为 {@code COMPLETED} 合法（config-gated 先 approve，approve 守卫为动态守卫保留在
     * Processor 原位；treatment 裁决/阈值门控为动态业务守卫）。
     */
    public void assertCanPost(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.MAINTENANCE_STATUS_COMPLETED.equals(s)) {
            throw illegal("post", s, ErpAstConstants.MAINTENANCE_STATUS_COMPLETED);
        }
    }

    /**
     * cancel 守卫：来源态为 {@code DRAFT}/{@code SUBMITTED} 合法（仅未开工前可作废）。
     */
    public void assertCanCancel(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.MAINTENANCE_STATUS_DRAFT.equals(s)
                && !ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED.equals(s)) {
            throw illegal("cancel", s,
                    ErpAstConstants.MAINTENANCE_STATUS_DRAFT + " 或 " + ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED);
        }
    }

    /**
     * reverse 守卫：来源态为 {@code POSTED} 合法（红冲纠错回卷）。
     */
    public void assertCanReverse(String status) {
        String s = normalize(status);
        if (!ErpAstConstants.MAINTENANCE_STATUS_POSTED.equals(s)) {
            throw illegal("reverse", s, ErpAstConstants.MAINTENANCE_STATUS_POSTED);
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    /** create 目标态=DRAFT（创建种子初始态）。 */
    public String createTargetStatus() {
        return ErpAstConstants.MAINTENANCE_STATUS_DRAFT;
    }

    public String submitTargetStatus() {
        return ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED;
    }

    public String startWorkTargetStatus() {
        return ErpAstConstants.MAINTENANCE_STATUS_IN_PROGRESS;
    }

    public String completeWorkTargetStatus() {
        return ErpAstConstants.MAINTENANCE_STATUS_COMPLETED;
    }

    public String postTargetStatus() {
        return ErpAstConstants.MAINTENANCE_STATUS_POSTED;
    }

    public String cancelTargetStatus() {
        return ErpAstConstants.MAINTENANCE_STATUS_CANCELLED;
    }

    /** reverse 目标态=COMPLETED（回卷非终态，允许修订后重新 post）。 */
    public String reverseTargetStatus() {
        return ErpAstConstants.MAINTENANCE_STATUS_COMPLETED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定。终态为 {@code POSTED}（可经 reverse 回卷，回卷非终态）/ {@code CANCELLED}。
     */
    public boolean isTerminal(String status) {
        return ErpAstConstants.MAINTENANCE_STATUS_POSTED.equals(status)
                || ErpAstConstants.MAINTENANCE_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    /**
     * 迁移元数据：7 条边（6 命名动作——cancel 双源）。create 创建种子不登记边（§9.2 排除）。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submit", ErpAstConstants.MAINTENANCE_STATUS_DRAFT, ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED),
                new TransitionDefinition("startWork", ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED, ErpAstConstants.MAINTENANCE_STATUS_IN_PROGRESS),
                new TransitionDefinition("completeWork", ErpAstConstants.MAINTENANCE_STATUS_IN_PROGRESS, ErpAstConstants.MAINTENANCE_STATUS_COMPLETED),
                new TransitionDefinition("post", ErpAstConstants.MAINTENANCE_STATUS_COMPLETED, ErpAstConstants.MAINTENANCE_STATUS_POSTED),
                new TransitionDefinition("cancel", ErpAstConstants.MAINTENANCE_STATUS_DRAFT, ErpAstConstants.MAINTENANCE_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpAstConstants.MAINTENANCE_STATUS_SUBMITTED, ErpAstConstants.MAINTENANCE_STATUS_CANCELLED),
                new TransitionDefinition("reverse", ErpAstConstants.MAINTENANCE_STATUS_POSTED, ErpAstConstants.MAINTENANCE_STATUS_COMPLETED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpAstConstants.MAINTENANCE_STATUS_POSTED,
                ErpAstConstants.MAINTENANCE_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpAstConstants.MAINTENANCE_STATUS_DRAFT));
    }

    // ---------- 内部 ----------

    /** null 归一化为 DRAFT（初始态语义：未设置=草稿），与 CRUD 创建写 DRAFT 一致。 */
    private static String normalize(String status) {
        return status == null ? ErpAstConstants.MAINTENANCE_STATUS_DRAFT : status;
    }

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
