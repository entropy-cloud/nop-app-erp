package app.erp.inv.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.inv.service.ErpInvConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 所有权转移单（{@code ErpInvOwnershipTransfer}）实体级状态机 Bean —— 一 Bean 对应一实体一轴
 * （{@code docStatus} 业务生命周期轴，<b>独立字典 {@code erp-inv/ownership-transfer-status}</b>，
 * 4 值 DRAFT/CONFIRMED/DONE/CANCELLED——值与 {@code erp-inv/move-status} 相同但<b>不复用</b>，
 * 常量取 {@code ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_*}，非 {@code DOC_STATUS_*}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/inventory/state-machine.md} §所有权转移单状态机（独立）。
 *
 * <p><b>治理裁定（§11.2 M4）</b>：done 触发 {@code OwnershipTransferPostingDispatcher.dispatchIfApplicable}
 * （仅 {@code transferType==VMI_CONSUME} 且 config {@code erp-inv.vmi-auto-generate-ap=true} 时过账
 * {@code ErpFinBusinessType.OWNERSHIP_TRANSFER}→{@code IErpFinVoucherBiz.post}，失败保持 DONE +
 * {@code posted=false}）+ 余额重分类（{@code reclassifyBalances}，数量守恒非 stock movement），属存货成本
 * 过账 + 库存强一致保护区。依契约 §11.2 M4 (ii)/(iv)/(v)，过账时序/失败回退/余额重分类编排<b>原序保留</b>在
 * Processor 路径；{@code posted} 不入轴（契约 §3）。本 Bean 为 M4 plan-first 产物。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 3 命名动作迁移矩阵
 * （confirm/done/cancel）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（4 条边）：confirm(DRAFT→CONFIRMED)、done(CONFIRMED→DONE)、
 * cancel 多源 {DRAFT, CONFIRMED}→CANCELLED = 2 边。分类 initial={DRAFT}、terminal={DONE, CANCELLED}。
 *
 * <p>动态守卫边界（保留 Processor）：{@code ERR_OWNERSHIP_TRACKING_DISABLED}（config
 * {@code erp-inv.ownership-tracking-enabled} 门）、余额重分类（{@code reclassifyBalance}，数量守恒）、
 * 过账派发（{@code dispatchIfApplicable}，VMI_CONSUME + config 门，失败保持 DONE + {@code posted=false}）、
 * 不变量校验（loc-mismatch/type-inconsistent）——不属于状态轴判断，本 Bean 不承载。
 */
public class ErpInvOwnershipTransferStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * confirm 守卫：来源态为 {@code DRAFT} 合法。
     *
     * <p>接线方 {@code ErpInvOwnershipTransferProcessor.assertStatus} 映射为领域码
     * {@code ERR_OWNERSHIP_TRANSFER_ILLEGAL_STATUS}（保持既有 expected=DRAFT 文案）。
     */
    public void assertCanConfirm(String docStatus) {
        if (!ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT.equals(docStatus)) {
            throw illegal("confirm", docStatus, ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT);
        }
    }

    /**
     * done 守卫：来源态为 {@code CONFIRMED} 合法。
     *
     * <p>接线方 {@code ErpInvOwnershipTransferProcessor.assertStatus} 映射为领域码
     * {@code ERR_OWNERSHIP_TRANSFER_ILLEGAL_STATUS}（保持既有 expected=CONFIRMED 文案）。
     */
    public void assertCanDone(String docStatus) {
        if (!ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED.equals(docStatus)) {
            throw illegal("done", docStatus, ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED);
        }
    }

    /**
     * cancel 守卫：来源态为 {@code DRAFT} 或 {@code CONFIRMED} 合法。
     *
     * <p>接线方 {@code ErpInvOwnershipTransferProcessor.cancel} 映射为领域码
     * {@code ERR_OWNERSHIP_TRANSFER_ILLEGAL_STATUS}（保持既有 expected="DRAFT或CONFIRMED" 文案）。
     */
    public void assertCanCancel(String docStatus) {
        if (!ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT.equals(docStatus)
                && !ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED.equals(docStatus)) {
            throw illegal("cancel", docStatus, "DRAFT或CONFIRMED");
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    public String confirmTargetStatus() {
        return ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED;
    }

    public String doneTargetStatus() {
        return ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE;
    }

    public String cancelTargetStatus() {
        return ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    /** 业务终态判定。所有权转移单终态为 {@code DONE} 与 {@code CANCELLED}（均无后续出边）。 */
    public boolean isTerminal(String docStatus) {
        return ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE.equals(docStatus)
                || ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CANCELLED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("confirm", ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT, ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED),
                new TransitionDefinition("done", ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED, ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE),
                new TransitionDefinition("cancel", ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT, ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CONFIRMED, ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DONE, ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpInvConstants.OWNERSHIP_TRANSFER_STATUS_DRAFT);
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
