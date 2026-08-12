package app.erp.hr.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 工时表（{@code ErpHrTimesheet}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/human-resource/state-machine.md §适用对象三}（RC-R1.8 实现注记）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载四态迁移矩阵
 * （DRAFT/SUBMITTED/APPROVED/REJECTED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 BizModel（契约 §7）。
 *
 * <p>迁移矩阵（4 条边，编码<strong>已实现</strong>行为）：submit(DRAFT→SUBMITTED) + submit(REJECTED→SUBMITTED)
 * （重提）+ approve(SUBMITTED→APPROVED) + reject(SUBMITTED→REJECTED)。
 *
 * <p><strong>cancel = 不存在（doc drift，本计划 Phase 3 layer-2 裁定）</strong>：owner doc §2 图表
 * （{@code state-machine.md:199}）画 {@code DRAFT→CANCELLED}，但 dict {@code erp-hr/timesheet-status}
 * （4 值 DRAFT/SUBMITTED/APPROVED/REJECTED）<strong>无 CANCELLED 值</strong>，RC-R1.8 权威注记仅列
 * submit/approve/reject，BizModel 无 cancel writer。Bean 不编码 cancel 边；owner doc §2 图表按 RC-R1.8
 * 注记就地补正（cancel 为目标行为未落地 + dict 无值）。镜像 LeaveRequest cancel 单源裁定范式。
 *
 * <p>动态业务校验与副作用不归 Bean（契约 §2 + §8）：submit 时的 totalHours 重算、24h 跨表校验
 * （{@code ERR_TIMESHEET_DAILY_HOURS_EXCEEDED}）、reject 时的 reason 必填守卫与 remark 写入，均保留 BizModel。
 */
public class ErpHrTimesheetStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * submit 守卫：DRAFT 或 REJECTED 合法（对齐 {@code ErpHrTimesheetBizModel.submit} 守卫）。
     * DRAFT→SUBMITTED（首次提交）+ REJECTED→SUBMITTED（被驳回后修改重新提交）。
     */
    public void assertCanSubmit(String status) {
        if (!ErpHrConstants.TIMESHEET_STATUS_DRAFT.equals(status)
                && !ErpHrConstants.TIMESHEET_STATUS_REJECTED.equals(status)) {
            throw illegal("submit", status, "DRAFT/REJECTED");
        }
    }

    public String submitTargetStatus() {
        return ErpHrConstants.TIMESHEET_STATUS_SUBMITTED;
    }

    public void assertCanApprove(String status) {
        if (!ErpHrConstants.TIMESHEET_STATUS_SUBMITTED.equals(status)) {
            throw illegal("approve", status, ErpHrConstants.TIMESHEET_STATUS_SUBMITTED);
        }
    }

    public String approveTargetStatus() {
        return ErpHrConstants.TIMESHEET_STATUS_APPROVED;
    }

    public void assertCanReject(String status) {
        if (!ErpHrConstants.TIMESHEET_STATUS_SUBMITTED.equals(status)) {
            throw illegal("reject", status, ErpHrConstants.TIMESHEET_STATUS_SUBMITTED);
        }
    }

    public String rejectTargetStatus() {
        return ErpHrConstants.TIMESHEET_STATUS_REJECTED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类：APPROVED 严格终态（无出边）；REJECTED 为可恢复终态（经 submit 重提可达 SUBMITTED）。
     * 二者均为业务生命周期终点（owner doc §1），故归 isTerminal=true。
     */
    public boolean isTerminal(String status) {
        return ErpHrConstants.TIMESHEET_STATUS_APPROVED.equals(status)
                || ErpHrConstants.TIMESHEET_STATUS_REJECTED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 BizModel 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("submit", ErpHrConstants.TIMESHEET_STATUS_DRAFT, ErpHrConstants.TIMESHEET_STATUS_SUBMITTED),
                new TransitionDefinition("submit", ErpHrConstants.TIMESHEET_STATUS_REJECTED, ErpHrConstants.TIMESHEET_STATUS_SUBMITTED),
                new TransitionDefinition("approve", ErpHrConstants.TIMESHEET_STATUS_SUBMITTED, ErpHrConstants.TIMESHEET_STATUS_APPROVED),
                new TransitionDefinition("reject", ErpHrConstants.TIMESHEET_STATUS_SUBMITTED, ErpHrConstants.TIMESHEET_STATUS_REJECTED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpHrConstants.TIMESHEET_STATUS_APPROVED,
                ErpHrConstants.TIMESHEET_STATUS_REJECTED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpHrConstants.TIMESHEET_STATUS_DRAFT);
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
