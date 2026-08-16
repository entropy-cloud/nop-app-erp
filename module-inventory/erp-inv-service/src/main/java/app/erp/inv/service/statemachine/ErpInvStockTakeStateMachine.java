package app.erp.inv.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.inv.dao.constants.ErpInvDocStatus;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 盘点单（{@code ErpInvStockTake}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus} 业务生命周期轴，
 * 复用字典 {@code erp-inv/move-status}，4 值 DRAFT/CONFIRMED/DONE/CANCELLED）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/inventory/state-machine.md} §盘点单状态机（独立）。
 *
 * <p><b>治理裁定（§11.2 M4）</b>：StockTake DONE 经差异移动单（当前 Deferred 手工 {@code generateMove}）
 * 间接触发存货过账，且与 StockMove 共享同一 {@code erp-inv/move-status} dict + 同一「DONE 触发存货过账」行为契约
 * （计划规则 14 bundling）。依契约 §11.2 M4 (ii)/(iv)/(v)，过账/记账/差异移动单生成编排<b>原序保留</b>在
 * BizModel/`I*Biz` 路径；{@code posted} 不入轴（契约 §3）。本 Bean 为 M4 plan-first 产物。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 3 命名动作迁移矩阵
 * （startTake/completeTake/cancel）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 BizModel（契约 §7）。
 *
 * <p>迁移矩阵（4 条边）：startTake(DRAFT→CONFIRMED)、completeTake(CONFIRMED→DONE)、
 * cancel 多源 {DRAFT, CONFIRMED}→CANCELLED = 2 边。分类 initial={DRAFT}、terminal={DONE, CANCELLED}。
 *
 * <p><b>COUNTING↔CONFIRMED 标签漂移（Phase 3 Decision，保留 CONFIRMED 行为不改 dict/绑）</b>：owner doc
 * §盘点单状态机状态图标注「草稿→盘点中 (COUNTING)→已完成/已取消」，但 {@code erp-inv/move-status} dict
 * <b>无 COUNTING 值</b>——StockTake 实际复用 {@code CONFIRMED}（代码 {@code setDocStatus(CONFIRMED)}）。
 * 即 owner doc 标签「盘点中 (COUNTING)」与实际 dict/code 值 {@code CONFIRMED} 漂移（标签/命名漂移，行为一致）。
 * 本 Bean 按既有 writer 建模（目标态 = CONFIRMED，保持行为），owner doc 补注标签映射。{@link #startTakeTargetStatus()}
 * 返回 CONFIRMED（对应 owner doc 标签「盘点中」）。
 *
 * <p>动态守卫边界（保留 BizModel/Processor）：{@code completeTake} 的差异移动单自动生成（RC-R1.56 已实现——
 * 经 {@code ErpInvStockTakeCompleteTakeProcessor} 行加载 + D1 差异计算回填 + 逐行 {@code IErpInvStockMoveBiz.generateMove}
 * 生成盘盈/盘亏移动单 + 失败逐行隔离告警，owner doc §盘点单状态机）——不属于状态轴判断，本 Bean 不承载。
 */
public class ErpInvStockTakeStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * startTake 守卫：来源态为 {@code DRAFT} 合法。
     *
     * <p>目标态为 {@code CONFIRMED}（owner doc 标签「盘点中 (COUNTING)」，dict 无 COUNTING，实际复用 CONFIRMED）。
     * 接线方 {@code ErpInvStockTakeBizModel.startTake} 映射为领域码 {@code ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION}
     * （保持既有 expected 行为）。
     */
    public void assertCanStartTake(String docStatus) {
        if (!ErpInvDocStatus.DOC_STATUS_DRAFT.equals(docStatus)) {
            throw illegal("startTake", docStatus, ErpInvDocStatus.DOC_STATUS_DRAFT);
        }
    }

    /**
     * completeTake 守卫：来源态为 {@code CONFIRMED}（盘点中）合法。
     *
     * <p>接线方 {@code ErpInvStockTakeBizModel.completeTake} 映射为领域码 {@code ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION}
     * （保持既有 expected=CONFIRMED 行为）。
     */
    public void assertCanCompleteTake(String docStatus) {
        if (!ErpInvDocStatus.DOC_STATUS_CONFIRMED.equals(docStatus)) {
            throw illegal("completeTake", docStatus, ErpInvDocStatus.DOC_STATUS_CONFIRMED);
        }
    }

    /**
     * cancel 守卫：来源态为 {@code DRAFT} 或 {@code CONFIRMED} 合法（守卫非终态 {DONE, CANCELLED}）。
     *
     * <p>接线方 {@code ErpInvStockTakeBizModel.cancelTake} 映射为领域码 {@code ERR_INV_STOCK_TAKE_ILLEGAL_TRANSITION}
     * （保持既有守卫「非 DONE 且 非 CANCELLED」行为）。
     */
    public void assertCanCancel(String docStatus) {
        if (ErpInvDocStatus.DOC_STATUS_DONE.equals(docStatus)
                || ErpInvDocStatus.DOC_STATUS_CANCELLED.equals(docStatus)) {
            throw illegal("cancel", docStatus, "非DONE且非CANCELLED");
        }
    }

    // ---------- 动作目标态（供 BizModel 写回） ----------

    /** startTake 目标态 = CONFIRMED（owner doc 标签「盘点中 (COUNTING)」，dict 无 COUNTING）。 */
    public String startTakeTargetStatus() {
        return ErpInvDocStatus.DOC_STATUS_CONFIRMED;
    }

    public String completeTakeTargetStatus() {
        return ErpInvDocStatus.DOC_STATUS_DONE;
    }

    public String cancelTargetStatus() {
        return ErpInvDocStatus.DOC_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    /** 业务终态判定。盘点单终态为 {@code DONE} 与 {@code CANCELLED}（均无后续出边）。 */
    public boolean isTerminal(String docStatus) {
        return ErpInvDocStatus.DOC_STATUS_DONE.equals(docStatus)
                || ErpInvDocStatus.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 BizModel 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("startTake", ErpInvDocStatus.DOC_STATUS_DRAFT, ErpInvDocStatus.DOC_STATUS_CONFIRMED),
                new TransitionDefinition("completeTake", ErpInvDocStatus.DOC_STATUS_CONFIRMED, ErpInvDocStatus.DOC_STATUS_DONE),
                new TransitionDefinition("cancel", ErpInvDocStatus.DOC_STATUS_DRAFT, ErpInvDocStatus.DOC_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpInvDocStatus.DOC_STATUS_CONFIRMED, ErpInvDocStatus.DOC_STATUS_CANCELLED)));
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
