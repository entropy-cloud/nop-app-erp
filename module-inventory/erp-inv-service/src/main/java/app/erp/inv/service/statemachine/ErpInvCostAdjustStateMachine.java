package app.erp.inv.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.inv.dao.constants.ErpInvDocStatus;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 成本调整单（{@code ErpInvCostAdjust}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus}
 * 业务生命周期轴，复用字典 {@code erp-inv/move-status}，4 值 DRAFT/CONFIRMED/DONE/CANCELLED）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/inventory/state-machine.md} §成本调整单状态机（独立）。
 *
 * <p><b>治理裁定（§11.2 M4）</b>：applyCostAdjust 触发 {@code CostAdjustmentPostingDispatcher.tryPost}
 * （COST_ADJUSTMENT，方向由 totalAdjustAmount 符号定，净 0 跳过）+ 成本层更新（{@code CostAdjustmentService}）；
 * reverseCostAdjust 触发红字凭证 + 成本层逆转。属存货成本过账 + 库存强一致保护区。依契约 §11.2 M4
 * (ii)/(iv)/(v)，过账时序/失败回退（tryPost 失败返回 null 保持 posted=false）/红冲编排<b>原序保留</b>在
 * Processor 路径；{@code posted} 不入轴（契约 §3）。本 Bean 为 M4 plan-first 产物。
 *
 * <p><b>approveStatus 轴不在本 Bean（option a，同 StockMove Non-Goal 先例）</b>：CostAdjust 的审批轴
 * （{@code approveStatus}，5 INLINE 动作 submitForApproval/approve/reject/reverseApprove/withdrawApproval）
 * 是独立审批轴，本 Bean 仅集中 docStatus 边；approveStatus 写 + approveStatus/posted gating 保留 Processor
 * （{@code ErpInvCostAdjustProcessor} 审批门控 config {@code erp-fin.cost-adjust-approval}）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 2 命名动作迁移矩阵
 * （applyCostAdjust/reverseCostAdjust）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（3 条边）：applyCostAdjust 多源 {DRAFT, CONFIRMED}→DONE = 2 边、
 * reverseCostAdjust(DONE→CONFIRMED) = 1 边。分类 initial={DRAFT}、terminal={DONE}
 * （CONFIRMED 可逆——仅由 reverse 到达、可 re-apply，非终态）。CANCELLED 仅由跨实体内部编排
 * （{@code ErpInvLandedCostProcessor} facade 写子 CostAdjust docStatus DRAFT seed / DONE / CANCELLED）
 * 写入，刻意绕过 {@code ErpInvCostAdjustProcessor.applyCostAdjust}（避免双过账）——非 Bean 迁移边
 * （契约 §9.2 内部编排），本 Bean 容忍这些写（不发明边、不拒绝）。
 *
 * <p>动态守卫边界（保留 Processor）：approveStatus/posted gating（{@code validateNotCancelled} +
 * 已-applied 检查 {@code ERR_COST_ADJUST_ALREADY_APPLIED} + 审批门 {@code ERR_COST_ADJUST_NOT_APPROVED} +
 * {@code requirePosted} {@code ERR_COST_ADJUST_NOT_APPLIED}）、成本层更新、过账派发（{@code tryPost}/
 * {@code reverse}）、净 0 跳过——不属于状态轴判断，本 Bean 不承载。
 */
public class ErpInvCostAdjustStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * applyCostAdjust 守卫：来源态为 {@code DRAFT} 或 {@code CONFIRMED} 合法（CONFIRMED 为 reverse 后可逆
     * 重应用态）。
     *
     * <p>较既有代码（{@code applyCostAdjust} 的 {@code requireAndValidate} 无 docStatus 源态守卫，仅守
     * validateNotCancelled/已-applied/审批门）略严：net-0 调整可达 DONE+posted=false，理论可从 DONE 重 apply；
     * 本 Bean 对 DONE 源态拒绝属合理收紧（从 DONE 重 apply 语义错误，正常流程重 apply 必经
     * reverse→CONFIRMED），Phase 3 四方对照 (c) 已核实无既有测试覆盖此边缘且裁定收紧不违反 Non-Goal。
     *
     * <p>接线方 {@code ErpInvCostAdjustApplyCostAdjustProcessor.requireAndValidate} 映射为领域码
     * {@code ERR_ILLEGAL_STATUS_TRANSITION}（docStatus 无专属 illegal-transition 码，见计划 Phase 3 Decision）。
     */
    public void assertCanApplyCostAdjust(String docStatus) {
        if (!ErpInvDocStatus.DOC_STATUS_DRAFT.equals(docStatus)
                && !ErpInvDocStatus.DOC_STATUS_CONFIRMED.equals(docStatus)) {
            throw illegal("applyCostAdjust", docStatus, "DRAFT或CONFIRMED");
        }
    }

    /**
     * reverseCostAdjust 守卫：来源态为 {@code DONE} 合法。
     *
     * <p>接线方 {@code ErpInvCostAdjustReverseCostAdjustProcessor.requirePosted} 映射为领域码
     * {@code ERR_ILLEGAL_STATUS_TRANSITION}（docStatus 无专属 illegal-transition 码，见计划 Phase 3 Decision）。
     */
    public void assertCanReverseCostAdjust(String docStatus) {
        if (!ErpInvDocStatus.DOC_STATUS_DONE.equals(docStatus)) {
            throw illegal("reverseCostAdjust", docStatus, ErpInvDocStatus.DOC_STATUS_DONE);
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    public String applyCostAdjustTargetStatus() {
        return ErpInvDocStatus.DOC_STATUS_DONE;
    }

    public String reverseCostAdjustTargetStatus() {
        return ErpInvDocStatus.DOC_STATUS_CONFIRMED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定。成本调整单终态为 {@code DONE}（无后续出边）。CONFIRMED 非终态（可 re-apply）；
     * CANCELLED 仅由跨实体内部编排写入（非 Bean 边），按业务语义同为不可逆终态。
     */
    public boolean isTerminal(String docStatus) {
        return ErpInvDocStatus.DOC_STATUS_DONE.equals(docStatus)
                || ErpInvDocStatus.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("applyCostAdjust", ErpInvDocStatus.DOC_STATUS_DRAFT, ErpInvDocStatus.DOC_STATUS_DONE),
                new TransitionDefinition("applyCostAdjust", ErpInvDocStatus.DOC_STATUS_CONFIRMED, ErpInvDocStatus.DOC_STATUS_DONE),
                new TransitionDefinition("reverseCostAdjust", ErpInvDocStatus.DOC_STATUS_DONE, ErpInvDocStatus.DOC_STATUS_CONFIRMED)));
    }

    public List<String> terminalStatuses() {
        return Collections.singletonList(ErpInvDocStatus.DOC_STATUS_DONE);
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpInvDocStatus.DOC_STATUS_DRAFT);
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
