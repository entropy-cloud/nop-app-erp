package app.erp.inv.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.inv.dao.constants.ErpInvDocStatus;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 调拨单（{@code ErpInvTransferOrder}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus}
 * 业务生命周期轴，复用字典 {@code erp-inv/move-status}，4 值 DRAFT/CONFIRMED/DONE/CANCELLED）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/inventory/state-machine.md} §调拨单状态机（独立）。
 *
 * <p><b>治理裁定（§11.2 M4）</b>：TransferOrder 的 confirm 仅触发可选跨法人内部往来 GL hook
 * （{@code dispatchIntercompanyPosting}，config-gated + 失败吞掉），<b>不触发存货成本过账、不生成 stock
 * movement</b>，属较轻保护区。依契约 §11.2 M4 (ii)/(iv)/(v)，intercompany hook 编排<b>原序保留</b>在
 * Processor 路径；{@code posted} 不入轴（契约 §3）。本 Bean 为 M4 plan-first 产物。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 1 命名动作迁移矩阵
 * （confirm）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（1 条边）：confirm(DRAFT→CONFIRMED)。分类 initial={DRAFT}、terminal={CONFIRMED}。
 * <b>仅 confirm 边</b>——TransferOrder 无 DONE/CANCELLED writer：后续物理移动是独立
 * {@code ErpInvStockMove} 流（非本单 docStatus 生命周期，owner doc 语义）；cancel/complete/reverse 生命周期
 * 属 out-of-scope（见路线图 Deferred）。
 *
 * <p>动态守卫边界（保留 Processor）：intercompany hook（{@code dispatchIntercompanyPosting}）为 config-gated
 * 跨域 GL 副作用、失败吞掉 log warn——不属于状态轴判断，本 Bean 不承载。
 */
public class ErpInvTransferOrderStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * confirm 守卫：来源态为 {@code DRAFT} 合法。
     *
     * <p>接线方 {@code ErpInvTransferOrderConfirmProcessor.validateDraft} 映射为领域码
     * {@code ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION}（行为保持——既有错误码缺陷按路线图 Non-Goal 不修正，
     * 见计划 Phase 3 Decision；保持既有 expected=DRAFT 文案）。
     */
    public void assertCanConfirm(String docStatus) {
        if (!ErpInvDocStatus.DOC_STATUS_DRAFT.equals(docStatus)) {
            throw illegal("confirm", docStatus, ErpInvDocStatus.DOC_STATUS_DRAFT);
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    public String confirmTargetStatus() {
        return ErpInvDocStatus.DOC_STATUS_CONFIRMED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定。调拨单终态为 {@code CONFIRMED}（无后续出边——后续物理移动是独立 StockMove 流，非本单
     * docStatus 迁移）。
     */
    public boolean isTerminal(String docStatus) {
        return ErpInvDocStatus.DOC_STATUS_CONFIRMED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Collections.singletonList(
                new TransitionDefinition("confirm", ErpInvDocStatus.DOC_STATUS_DRAFT, ErpInvDocStatus.DOC_STATUS_CONFIRMED)));
    }

    public List<String> terminalStatuses() {
        return Collections.singletonList(ErpInvDocStatus.DOC_STATUS_CONFIRMED);
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
