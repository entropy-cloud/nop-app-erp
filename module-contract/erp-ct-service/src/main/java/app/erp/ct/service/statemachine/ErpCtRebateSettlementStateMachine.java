package app.erp.ct.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.ct.service.ErpCtConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 返利结算单（{@code ErpCtRebateSettlement}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/contract/state-machine.md} §适用对象：返利结算（RebateSettlement）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载**已实现**迁移矩阵
 * （DRAFT/POSTED/CANCELLED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。可经 Delta 同名 Bean 覆盖（契约 §6）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor（契约 §7）——
 * {@code ErpCtRebateSettlementPostSettlementProcessor} 捕获后映射为领域码
 * {@code ERR_CT_SETTLEMENT_ILLEGAL_TRANSITION}（仅传 {@code settlementId} + {@code currentStatus}，
 * common 码作 cause 保留，{@code action}/{@code expectedStatus} 不向领域码传播）。
 *
 * <p>迁移矩阵（1 条实现边，dict {@code erp-ct/settlement-status} 3 值）：
 * <ul>
 *   <li>postSettlement(DRAFT→POSTED)。</li>
 * </ul>
 *
 * <p><strong>CANCELLED 死状态裁定（layer-2 四方对照，Decision (A)）</strong>：dict {@code erp-ct/settlement-status}
 * 含 CANCELLED 值（{@code module-contract/model/app-erp-contract.orm.xml:76}），但全仓**零
 * {@code setStatus(SETTLEMENT_STATUS_CANCELLED)} 生产 writer** + 无 reverse/unpost/cancel 命名 mutation。
 * 裁定分类 = <strong>intentional reserved（死状态）</strong>：本 Bean {@link #transitions()} 不含 CANCELLED 边，
 * {@link #terminalStatuses()} 亦不含 CANCELLED（非真正终态，仅预留语义入口——对齐 Contract CANCELLED /
 * RebateAgreement EXPIRED+SETTLED 先例：保留优于删除）。**Successor**：PM 要求 settlement cancel 工作流时，
 * 开独立 plan 实现 cancel mutation + dict 值激活。
 *
 * <p><strong>{@code posted} 布尔列不对称（watch-only residual，Decision (B)）</strong>：ORM {@code posted}
 * 列（{@code app-erp-contract.orm.xml:605}，BOOLEAN default false）存在，但 Processor **从不
 * {@code setPosted(true)}**——仅写 {@code postedAt}/{@code postedBy}；credit-memo 发票自身 {@code posted=false}
 *（经 pur/sal 管道后续翻转）。此为 watch-only residual（owner doc §适用对象四登记，非修正），
 * 本 Bean **不入轴 {@code posted}**（契约 §3：{@code posted} 不作 StateMachine 迁移轴）。
 *
 * <p><strong>初始态写入路径（按契约 §9.2 选项 c）</strong>：DRAFT 经 CRUD {@code save} 写入
 * （{@code ErpCtRebateSettlementBizModel.defaultPrepareSave} 仅 seed businessDate，status 由调用方请求体提供），
 * 不调本 Bean 的 {@code assertCanPostSettlement}（初始态写入，非既有结算单迁移）。
 */
public class ErpCtRebateSettlementStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * postSettlement 前向守卫：仅 DRAFT 可过账（DRAFT→POSTED 唯一实现边）。
     */
    public void assertCanPostSettlement(String status) {
        if (!ErpCtConstants.SETTLEMENT_STATUS_DRAFT.equals(status)) {
            throw illegal("postSettlement", status, ErpCtConstants.SETTLEMENT_STATUS_DRAFT);
        }
    }

    /**
     * postSettlement 目标态：POSTED。
     */
    public String postSettlementTargetStatus() {
        return ErpCtConstants.SETTLEMENT_STATUS_POSTED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类：POSTED（结算单过账后归档，不可回退；CANCELLED 为 intentional reserved 死状态，
     * 非真正终态——见类 javadoc Decision (A)）。
     */
    public boolean isTerminal(String status) {
        return ErpCtConstants.SETTLEMENT_STATUS_POSTED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    /**
     * 迁移元数据：仅 1 条实现边（postSettlement DRAFT→POSTED）。CANCELLED 无入边/出边
     * （intentional reserved，见类 javadoc Decision (A)）。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("postSettlement", ErpCtConstants.SETTLEMENT_STATUS_DRAFT,
                        ErpCtConstants.SETTLEMENT_STATUS_POSTED)));
    }

    /**
     * 终态集合：{POSTED}。CANCELLED 不纳入（intentional reserved 死状态，非真正终态）。
     */
    public List<String> terminalStatuses() {
        return Collections.singletonList(ErpCtConstants.SETTLEMENT_STATUS_POSTED);
    }

    /**
     * 初始态集合：{DRAFT}（CRUD 创建写入，§9.2 选项 c 初始态路径）。
     */
    public List<String> initialStatuses() {
        return Collections.singletonList(ErpCtConstants.SETTLEMENT_STATUS_DRAFT);
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
