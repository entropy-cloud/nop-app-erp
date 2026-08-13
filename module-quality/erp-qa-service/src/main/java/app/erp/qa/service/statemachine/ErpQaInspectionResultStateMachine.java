package app.erp.qa.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.qa.service.ErpQaConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 质检单（{@code ErpQaInspection}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code result} 检验结果轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/quality/state-machine.md}（§适用对象一：4 态 PENDING/ACCEPTED/CONDITIONAL/REJECTED）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 result 轴 3 动作迁移矩阵
 * （recordResult/passInspection/failInspection）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p><b>recordResult 数据驱动目标态</b>：recordResult 的目标态由行级评测（{@code InspectionResultEvaluator.aggregate}）
 * 决定（ACCEPTED/CONDITIONAL/REJECTED 三分支），非状态机固定值。故本 Bean 只提供 {@link #assertCanRecordResult(String)}
 * 来源态守卫，不提供 {@code recordResultTargetStatus()}（目标态由 Processor 经评测器计算）。passInspection/failInspection
 * 目标态固定（ACCEPTED/REJECTED），提供 {@link #passInspectionTargetStatus()}/{@link #failInspectionTargetStatus()}。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 *
 * <p><b>M4.58 docStatus 裁定</b>（plan Phase 1 Decision）：质检单 {@code docStatus}（DRAFT/ACTIVE/CANCELLED）
 * 实仓零状态机 writer（仅测试 seed ACTIVE），裁定为 dict-only 泛型占位轴，排除迁移（对齐 M0.2 §5.1 死状态登记范式），
 * 登记为 deferred-but-adjudicated（successor = PM 要求 Inspection docStatus 参与生命周期迁移时）。
 */
public class ErpQaInspectionResultStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * recordResult 守卫：来源态为 {@code PENDING}/{@code null} 合法（终态不可恢复，复检请新建质检单）。
     *
     * <p>非法来源态报告 common 层非法边（携带 {@code action=recordResult}/{@code fromStatus}）。
     * 接线方 {@code ErpQaInspectionRecordResultProcessor} 映射为领域码 {@code ERR_INVALID_INSPECTION_STATUS_TRANSITION}。
     * 目标态由行级评测器决定，不在此守卫范围内。
     */
    public void assertCanRecordResult(String result) {
        String status = normalize(result);
        if (!ErpQaConstants.INSPECTION_RESULT_PENDING.equals(status)) {
            throw illegal("recordResult", status, "PENDING（终态不可恢复，复检请新建质检单）");
        }
    }

    /** passInspection 守卫：来源态为 {@code PENDING}/{@code null} 合法。 */
    public void assertCanPassInspection(String result) {
        String status = normalize(result);
        if (!ErpQaConstants.INSPECTION_RESULT_PENDING.equals(status)) {
            throw illegal("passInspection", status, "PENDING（终态不可恢复，复检请新建质检单）");
        }
    }

    /** failInspection 守卫：来源态为 {@code PENDING}/{@code null} 合法。 */
    public void assertCanFailInspection(String result) {
        String status = normalize(result);
        if (!ErpQaConstants.INSPECTION_RESULT_PENDING.equals(status)) {
            throw illegal("failInspection", status, "PENDING（终态不可恢复，复检请新建质检单）");
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    /** passInspection 目标态=ACCEPTED（固定）。 */
    public String passInspectionTargetStatus() {
        return ErpQaConstants.INSPECTION_RESULT_ACCEPTED;
    }

    /** failInspection 目标态=REJECTED（固定）。 */
    public String failInspectionTargetStatus() {
        return ErpQaConstants.INSPECTION_RESULT_REJECTED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定。result 轴终态为 ACCEPTED/CONDITIONAL/REJECTED（owner doc §3），均为不可恢复终态（无出边）。
     */
    public boolean isTerminal(String result) {
        return ErpQaConstants.INSPECTION_RESULT_ACCEPTED.equals(result)
                || ErpQaConstants.INSPECTION_RESULT_CONDITIONAL.equals(result)
                || ErpQaConstants.INSPECTION_RESULT_REJECTED.equals(result);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    /**
     * 迁移矩阵元数据。recordResult 为数据驱动三分支动作（PENDING→ACCEPTED/CONDITIONAL/REJECTED），
     * 目标态由评测器决定；passInspection/failInspection 为固定单分支。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("recordResult", ErpQaConstants.INSPECTION_RESULT_PENDING, ErpQaConstants.INSPECTION_RESULT_ACCEPTED),
                new TransitionDefinition("recordResult", ErpQaConstants.INSPECTION_RESULT_PENDING, ErpQaConstants.INSPECTION_RESULT_CONDITIONAL),
                new TransitionDefinition("recordResult", ErpQaConstants.INSPECTION_RESULT_PENDING, ErpQaConstants.INSPECTION_RESULT_REJECTED),
                new TransitionDefinition("passInspection", ErpQaConstants.INSPECTION_RESULT_PENDING, ErpQaConstants.INSPECTION_RESULT_ACCEPTED),
                new TransitionDefinition("failInspection", ErpQaConstants.INSPECTION_RESULT_PENDING, ErpQaConstants.INSPECTION_RESULT_REJECTED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpQaConstants.INSPECTION_RESULT_ACCEPTED,
                ErpQaConstants.INSPECTION_RESULT_CONDITIONAL,
                ErpQaConstants.INSPECTION_RESULT_REJECTED));
    }

    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpQaConstants.INSPECTION_RESULT_PENDING));
    }

    // ---------- 内部 ----------

    /** null 归一化为 PENDING（初始态语义：未设结果=待检），与各 Processor getResult 归一一致。 */
    private static String normalize(String result) {
        return result == null ? ErpQaConstants.INSPECTION_RESULT_PENDING : result;
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
