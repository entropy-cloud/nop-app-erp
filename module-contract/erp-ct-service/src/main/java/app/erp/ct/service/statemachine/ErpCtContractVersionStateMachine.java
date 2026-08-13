package app.erp.ct.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.ct.service.ErpCtConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 合同版本（{@code ErpCtContractVersion}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/contract/state-machine.md} §适用对象：合同版本（ContractVersion）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载**已实现**迁移矩阵
 * （DRAFT/FINALIZED/SIGNED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。可经 Delta 同名 Bean 覆盖（契约 §6）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（2 条边，线性无分支，dict {@code erp-ct/version-status} 3 值全可达——无死状态）：
 * <ul>
 *   <li>finalize(DRAFT→FINALIZED)；</li>
 *   <li>sign(FINALIZED→SIGNED)。</li>
 * </ul>
 *
 * <p><strong>跨聚合联动声明</strong>：版本签署（FINALIZED→SIGNED）除经版本自身命名动作 {@code signVersion}
 * 触发外，亦由合同激活（{@code ErpCtContractActivateProcessor}）在 Contract 激活时对当前 FINALIZED 版本级联调用
 * {@code contractVersionBiz.signVersion}（父驱子）。两条路径均经 signVersion Processor 注入本 Bean 统一守卫，
 * 本 Bean 自身不直接被 Contract Processor 注入（版本级联经 IBiz 调用）。
 *
 * <p><strong>初始态写入（amend 新建版本，按 §9.2 选项 c）</strong>：合同修订时 {@code ErpCtContractAmendProcessor}
 * 对新建版本 seed {@code setStatus(VERSION_STATUS_DRAFT)} 属初始态写入（非既有版本迁移），不调本 Bean 的
 * {@code assertCanFinalize}/{@code assertCanSign}（契约 §9.2 初始态路径）。
 */
public class ErpCtContractVersionStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanFinalize(String status) {
        if (!ErpCtConstants.VERSION_STATUS_DRAFT.equals(status)) {
            throw illegal("finalize", status, ErpCtConstants.VERSION_STATUS_DRAFT);
        }
    }

    public String finalizeTargetStatus() {
        return ErpCtConstants.VERSION_STATUS_FINALIZED;
    }

    public void assertCanSign(String status) {
        if (!ErpCtConstants.VERSION_STATUS_FINALIZED.equals(status)) {
            throw illegal("sign", status, ErpCtConstants.VERSION_STATUS_FINALIZED);
        }
    }

    public String signTargetStatus() {
        return ErpCtConstants.VERSION_STATUS_SIGNED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类：SIGNED（版本签署后归档，不可回退）。
     */
    public boolean isTerminal(String status) {
        return ErpCtConstants.VERSION_STATUS_SIGNED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("finalize", ErpCtConstants.VERSION_STATUS_DRAFT, ErpCtConstants.VERSION_STATUS_FINALIZED),
                new TransitionDefinition("sign", ErpCtConstants.VERSION_STATUS_FINALIZED, ErpCtConstants.VERSION_STATUS_SIGNED)));
    }

    public List<String> terminalStatuses() {
        return Collections.singletonList(ErpCtConstants.VERSION_STATUS_SIGNED);
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpCtConstants.VERSION_STATUS_DRAFT);
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
