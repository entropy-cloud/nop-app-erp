package app.erp.inv.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.inv.dao.constants.ErpInvDocStatus;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 到岸成本单（{@code ErpInvLandedCost}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus}
 * 业务生命周期轴，复用字典 {@code erp-inv/move-status}，4 值 DRAFT/CONFIRMED/DONE/CANCELLED）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/inventory/state-machine.md} §到岸成本单状态机（独立）。
 *
 * <p><b>治理裁定（§11.2 M4）</b>：approve 触发 {@code LandedCostPostingDispatcher.tryPost}
 * （LANDED_COST Dr Inventory/Cr AP）+ 分配引擎 + 子 CostAdjust 成本层更新（{@code createAndApplyCostAdjust}）；
 * reverseApprove 触发红字凭证（{@code postingDispatcher.reverse}，失败吞掉 + 告警
 * {@code IErpSysNotificationBiz.notify}，G4 分级）+ 子 CostAdjust 逆转。属存货成本过账 + 库存强一致保护区。
 * 依契约 §11.2 M4 (ii)/(iv)/(v)，过账时序/失败回退/红冲/告警编排<b>原序保留</b>在 Processor 路径；
 * {@code posted} 不入轴（契约 §3）。本 Bean 为 M4 plan-first 产物。
 *
 * <p><b>approve/reverseApprove 双轴联动</b>：两动作同时写 {@code docStatus} 与 {@code approveStatus}
 * （{@code doPostApprove:348} approveStatus→APPROVED / {@code doReverseApprove:185} approveStatus→REJECTED，
 * 原子写）。Bean 仅集中 docStatus 边 + assertCan*（docStatus 源态），approveStatus 写 + approveStatus/posted
 * gating 保留 Processor（option a，同 StockMove Non-Goal 先例，见计划 Phase 1 Decision）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 2 命名动作迁移矩阵
 * （approve/reverseApprove）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（2 条边）：approve(DRAFT→DONE)、reverseApprove(DONE→CANCELLED)。分类 initial={DRAFT}、
 * terminal={DONE, CANCELLED}。<b>无 CONFIRMED 写</b>（DRAFT→DONE 直达，Bean 无 CONFIRMED 边）。
 *
 * <p><b>生成路径无迁移边（契约 §8/§9.2 选项 c）</b>：{@code generateFreightLandedCost}
 * （{@code ErpInvLandedCostProcessor.createLandedCostHead}）= 生成路径（创建新单 seed DRAFT，初始态写入），
 * 不调 {@code assertCan*}。
 *
 * <p>动态守卫边界（保留 Processor）：幂等守卫（{@code ERR_LANDED_COST_ALREADY_APPROVED}）、悲观锁
 * （{@code lockReceiveForAllocation}）、{@code validateNotAlreadyAllocated}、reverse posted+APPROVED 守卫
 * （{@code ERR_LANDED_COST_NOT_POSTED}）、过账派发（{@code tryPost}/{@code reverse}）、reverse 失败吞掉 +
 * 告警（G4 分级）、子 CostAdjust 联动（{@code createAndApplyCostAdjust} 直接调
 * {@code CostAdjustmentService.applyCostAdjust}，避免双过账）——不属于状态轴判断，本 Bean 不承载。
 */
public class ErpInvLandedCostStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * approve 守卫：来源态为 {@code DRAFT} 合法（无 CONFIRMED 写，DRAFT→DONE 直达）。
     *
     * <p>接线方 {@code ErpInvLandedCostApproveProcessor.approve} 映射为领域码
     * {@code ERR_LANDED_COST_ALREADY_APPROVED}/{@code ERR_LANDED_COST_NOT_POSTED}（幂等/posted 门守卫，
     * docStatus 无专属 illegal-transition 码，见计划 Phase 3 Decision）。
     */
    public void assertCanApprove(String docStatus) {
        if (!ErpInvDocStatus.DOC_STATUS_DRAFT.equals(docStatus)) {
            throw illegal("approve", docStatus, ErpInvDocStatus.DOC_STATUS_DRAFT);
        }
    }

    /**
     * reverseApprove 守卫：来源态为 {@code DONE} 合法。
     *
     * <p>接线方 {@code ErpInvLandedCostReverseApproveProcessor.reverseApprove} 映射为领域码
     * {@code ERR_LANDED_COST_ALREADY_APPROVED}/{@code ERR_LANDED_COST_NOT_POSTED}（幂等/posted 门守卫，
     * docStatus 无专属 illegal-transition 码，见计划 Phase 3 Decision）。
     */
    public void assertCanReverseApprove(String docStatus) {
        if (!ErpInvDocStatus.DOC_STATUS_DONE.equals(docStatus)) {
            throw illegal("reverseApprove", docStatus, ErpInvDocStatus.DOC_STATUS_DONE);
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    public String approveTargetStatus() {
        return ErpInvDocStatus.DOC_STATUS_DONE;
    }

    public String reverseApproveTargetStatus() {
        return ErpInvDocStatus.DOC_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    /** 业务终态判定。到岸成本单终态为 {@code DONE} 与 {@code CANCELLED}（均无后续出边）。 */
    public boolean isTerminal(String docStatus) {
        return ErpInvDocStatus.DOC_STATUS_DONE.equals(docStatus)
                || ErpInvDocStatus.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("approve", ErpInvDocStatus.DOC_STATUS_DRAFT, ErpInvDocStatus.DOC_STATUS_DONE),
                new TransitionDefinition("reverseApprove", ErpInvDocStatus.DOC_STATUS_DONE, ErpInvDocStatus.DOC_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpInvDocStatus.DOC_STATUS_DONE, ErpInvDocStatus.DOC_STATUS_CANCELLED));
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
