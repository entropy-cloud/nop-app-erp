package app.erp.fin.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.fin.service.ErpFinConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 核销单（{@code ErpFinReconciliation}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus}
 * 核销单生命周期轴，字典 {@code erp-fin/reconciliation-status} 3 值：DRAFT/POSTED/REVERSED）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/finance/state-machine.md} §对象七 + {@code docs/design/finance/ar-ap-reconciliation.md}。
 *
 * <p><b>治理裁定（§11.2 M4 plan-first，plan 2026-08-14-0456-1）</b>：核销单 {@code post} 触发 ArApItem 核销联动
 * （settled/openAmount 回写 + 往来余额刷新），{@code reverse} 为 POSTED→REVERSED 红冲侧（ArApItem 对称回滚 + 可选
 * 汇兑损益凭证红冲）。依契约 §11.2 M4 硬约束 (i)–(v)：过账/红冲时序、编排、失败回退继续由
 * {@code ErpFinReconciliationPostProcessor}/{@code ErpFinReconciliationReverseProcessor} +
 * {@code ReconciliationSettler} + {@code postedBy}/{@code postedAt} 契约管理（§11.2 M4 (ii)/(v)），Bean 不触碰；
 * 跨域副作用（ArApItem 核销联动、往来余额刷新、FX 凭证）保留原 Processor 路径（§11.2 M4 (iv)）；{@code posted}
 * 不入轴（§11.2 M4 (iii)，本实体无独立 {@code posted} boolean 字段，核销状态即 {@code docStatus} 表达）。
 *
 * <p>命名带 {@code Document} 后缀（契约 §1 双轴约定，为审批轴预留命名空间）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。
 *
 * <p><b>唯一 2 迁移边</b>：{@code post} {@code DRAFT→POSTED}、{@code reverse} {@code POSTED→REVERSED}。
 * 3 个 dict 值全部活跃（无死状态）：DRAFT=initial、POSTED=中间态（有出边）、REVERSED=终态。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7，
 * {@code ERR_RECONCILIATION_STATUS_INVALID}，common 码作 cause 保留）。
 *
 * <p><b>生成路径不接线 Bean</b>（契约 §9.2 选项 c 初始态/生成写入，不调 {@code assertCan*}）：
 * {@code ErpFinReconciliationCreateProcessor} 创建时写 DRAFT（初始态直接落库）；auto-reconciliation
 * （{@code runAutoReconciliation}）走 BizModel 入口但核销单本身仍经命名动作 post——无独立旁路写入路径。
 */
public class ErpFinReconciliationDocumentStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * post 入口守卫：来源态为 {@code DRAFT} 合法（唯一迁移边的来源态）。
     *
     * <p>对非法来源态（POSTED/REVERSED）报告 common 层非法边（携带 {@code action=post}/{@code fromStatus}）；
     * 接线方 {@code ErpFinReconciliationPostProcessor} 映射为领域码 {@code ERR_RECONCILIATION_STATUS_INVALID}
     * （common 码作 cause 保留）。
     */
    public void assertCanPost(String docStatus) {
        if (!ErpFinConstants.RECON_STATUS_DRAFT.equals(docStatus)) {
            throw illegal("post", docStatus, ErpFinConstants.RECON_STATUS_DRAFT);
        }
    }

    /**
     * reverse 入口守卫：来源态为 {@code POSTED} 合法（红冲侧唯一迁移边的来源态）。
     *
     * <p>对非法来源态（DRAFT/REVERSED）报告 common 层非法边；接线方
     * {@code ErpFinReconciliationReverseProcessor}（+ BizModel {@code previewReverse} 前置守卫一致性）
     * 映射为领域码 {@code ERR_RECONCILIATION_STATUS_INVALID}（common 码作 cause 保留）。
     */
    public void assertCanReverse(String docStatus) {
        if (!ErpFinConstants.RECON_STATUS_POSTED.equals(docStatus)) {
            throw illegal("reverse", docStatus, ErpFinConstants.RECON_STATUS_POSTED);
        }
    }

    // ---------- 动作目标态（供 Processor 写回） ----------

    /** post 的目标态（POSTED）。 */
    public String postTargetStatus() {
        return ErpFinConstants.RECON_STATUS_POSTED;
    }

    /** reverse 的目标态（REVERSED）。 */
    public String reverseTargetStatus() {
        return ErpFinConstants.RECON_STATUS_REVERSED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定：{@code REVERSED}（红冲后无出边，不可恢复）。{@code POSTED} 为中间态
     * （经 reverse 有出边，非终态）。{@code DRAFT} 为初始态。
     */
    public boolean isTerminal(String docStatus) {
        return ErpFinConstants.RECON_STATUS_REVERSED.equals(docStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("post", ErpFinConstants.RECON_STATUS_DRAFT, ErpFinConstants.RECON_STATUS_POSTED),
                new TransitionDefinition("reverse", ErpFinConstants.RECON_STATUS_POSTED, ErpFinConstants.RECON_STATUS_REVERSED)));
    }

    public List<String> terminalStatuses() {
        return Collections.singletonList(ErpFinConstants.RECON_STATUS_REVERSED);
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpFinConstants.RECON_STATUS_DRAFT);
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
