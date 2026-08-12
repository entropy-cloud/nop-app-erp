package app.erp.crm.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.crm.service.ErpCrmConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 线索/商机（{@code ErpCrmLead}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/crm/state-machine.md §Lead}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载迁移矩阵
 * （NEW/QUALIFIED/CONVERTED/LOST/CANCELLED 5 态）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）。
 *
 * <p>迁移矩阵（7 条意图边）：
 * <ul>
 *   <li>qualify(NEW→QUALIFIED)；</li>
 *   <li>lose(NEW/QUALIFIED→LOST)；</li>
 *   <li>cancel(NEW/QUALIFIED→CANCELLED)；</li>
 *   <li>convert({NEW,QUALIFIED}→CONVERTED)。</li>
 * </ul>
 *
 * <p><b>convert 运行时 vs 意图矩阵有意区分</b>（plan 2026-08-13-0945-3 Decision 分支 a + Phase 2 已知漂移 2）：
 * {@link #assertCanConvert(String)} 运行时<b>仅拒绝 CONVERTED</b>（匹配现行 {@code validateNotConverted}
 * 代码行为，保持既有外部行为不变，含 NEW→CONVERTED 合法），对一切非 CONVERTED 态（NEW/QUALIFIED/LOST/CANCELLED）
 * 运行时通过；而 {@link #transitions()} 编码<b>意图矩阵</b> {NEW,QUALIFIED}→CONVERTED（声明态）。此范围为
 * <b>既有 latent gap</b>（迁移前已存在：LOST/CANCELLED→CONVERTED 在代码中技术上允许），非本计划引入；
 * Phase 2 裁定保持运行时宽放 + owner doc 补注为 watch-only residual。
 *
 * <p>初始态 NEW 由创建路径写入（M0.1 §9.2 选项 c），非迁移。
 * stageId 维度的 sequence 方向守卫（{@code validateStageDirection}）是独立维度动态守卫，不纳入本 Bean。
 */
public class ErpCrmLeadStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanQualify(String docStatus) {
        if (!ErpCrmConstants.DOC_STATUS_NEW.equals(docStatus)) {
            throw illegal("qualify", docStatus, ErpCrmConstants.DOC_STATUS_NEW);
        }
    }

    public String qualifyTargetStatus() {
        return ErpCrmConstants.DOC_STATUS_QUALIFIED;
    }

    public void assertCanLose(String docStatus) {
        if (!ErpCrmConstants.DOC_STATUS_NEW.equals(docStatus)
                && !ErpCrmConstants.DOC_STATUS_QUALIFIED.equals(docStatus)) {
            throw illegal("lose", docStatus, ErpCrmConstants.DOC_STATUS_NEW + "/" + ErpCrmConstants.DOC_STATUS_QUALIFIED);
        }
    }

    public String loseTargetStatus() {
        return ErpCrmConstants.DOC_STATUS_LOST;
    }

    public void assertCanCancel(String docStatus) {
        if (!ErpCrmConstants.DOC_STATUS_NEW.equals(docStatus)
                && !ErpCrmConstants.DOC_STATUS_QUALIFIED.equals(docStatus)) {
            throw illegal("cancel", docStatus, ErpCrmConstants.DOC_STATUS_NEW + "/" + ErpCrmConstants.DOC_STATUS_QUALIFIED);
        }
    }

    public String cancelTargetStatus() {
        return ErpCrmConstants.DOC_STATUS_CANCELLED;
    }

    /**
     * convert 合法性断言。运行时<b>仅拒绝 CONVERTED</b>（匹配现行 {@code validateNotConverted} 行为，
     * 保持既有外部行为不变）。leadType/partner 门控作为动态守卫保留 Conversion Processor 原位，不经本方法。
     */
    public void assertCanConvert(String docStatus) {
        if (ErpCrmConstants.DOC_STATUS_CONVERTED.equals(docStatus)) {
            throw illegal("convert", docStatus, "非 " + ErpCrmConstants.DOC_STATUS_CONVERTED);
        }
    }

    public String convertTargetStatus() {
        return ErpCrmConstants.DOC_STATUS_CONVERTED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类：CONVERTED/LOST/CANCELLED。
     *
     * <p>对齐 owner doc {@code crm/state-machine.md §Lead}「终态不可恢复」。
     */
    public boolean isTerminal(String docStatus) {
        return ErpCrmConstants.DOC_STATUS_CONVERTED.equals(docStatus)
                || ErpCrmConstants.DOC_STATUS_LOST.equals(docStatus)
                || ErpCrmConstants.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    /**
     * 意图矩阵（声明态）：编码 {NEW,QUALIFIED}→CONVERTED。与运行时 {@link #assertCanConvert} 仅拒 CONVERTED
     * 的范围有意不同（见类注释 convert latent gap）。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("qualify", ErpCrmConstants.DOC_STATUS_NEW, ErpCrmConstants.DOC_STATUS_QUALIFIED),
                new TransitionDefinition("lose", ErpCrmConstants.DOC_STATUS_NEW, ErpCrmConstants.DOC_STATUS_LOST),
                new TransitionDefinition("lose", ErpCrmConstants.DOC_STATUS_QUALIFIED, ErpCrmConstants.DOC_STATUS_LOST),
                new TransitionDefinition("cancel", ErpCrmConstants.DOC_STATUS_NEW, ErpCrmConstants.DOC_STATUS_CANCELLED),
                new TransitionDefinition("cancel", ErpCrmConstants.DOC_STATUS_QUALIFIED, ErpCrmConstants.DOC_STATUS_CANCELLED),
                new TransitionDefinition("convert", ErpCrmConstants.DOC_STATUS_NEW, ErpCrmConstants.DOC_STATUS_CONVERTED),
                new TransitionDefinition("convert", ErpCrmConstants.DOC_STATUS_QUALIFIED, ErpCrmConstants.DOC_STATUS_CONVERTED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpCrmConstants.DOC_STATUS_CONVERTED, ErpCrmConstants.DOC_STATUS_LOST, ErpCrmConstants.DOC_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpCrmConstants.DOC_STATUS_NEW);
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
