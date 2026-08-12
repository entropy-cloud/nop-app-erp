package app.erp.hr.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 劳动合同（{@code ErpHrEmploymentContract}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/human-resource/state-machine.md §适用对象五 合同（ErpHrEmploymentContract）}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载<strong>已实现活态</strong>
 * 迁移矩阵（ACTIVE/EXPIRED/TERMINATED）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（4 条边，编码<strong>已实现命名动作迁移</strong>）：
 * <ul>
 *   <li>renew(ACTIVE→ACTIVE) 自环 + renew(EXPIRED→ACTIVE)（对齐 {@code ErpHrEmploymentContractBizModel.renew:94-100}
 *       守卫 {@code status∈{ACTIVE,EXPIRED}}→setStatus ACTIVE）；</li>
 *   <li>expire(ACTIVE→EXPIRED)（对齐 {@code ErpHrEmploymentContractExpireOverdueContractsProcessor:42}）；</li>
 *   <li>terminate(ACTIVE→TERMINATED)（对齐 {@code ErpHrEmployeeTransferEmployeeProcessor:190} 调动联动 +
 *       {@code ErpHrEmployeeBizModel:233} 离职/调动联动）。</li>
 * </ul>
 *
 * <p><strong>SUSPENDED 不在矩阵</strong>：dict {@code erp-hr/contract-status} 含 SUSPENDED，但生产零
 * {@code setStatus(SUSPENDED)} writer、无 suspendContract mutation = <strong>死状态</strong>（owner doc
 * §适用对象五已记载 Deferred）。Bean 如实反映 = SUSPENDED 不出现在 transitions/initial/terminal 任一集合。
 * Phase 3 layer-2 裁定登记为 {@code intentional reserved}，dict 值保留为预留语义入口。
 *
 * <p><strong>初始态 ACTIVE 写入路径不经 Bean</strong>（契约 §9.2 选项 c）：4 处
 * {@code setStatus(ACTIVE)} 初始态写入（{@code ErpHrRecruitmentHireProcessor:94} 入职新建合同、
 * {@code ErpHrRecruitmentBizModel:180} 入职新建、{@code ErpHrEmployeeTransferEmployeeProcessor:240}
 * 调动后继合同 newContractFrom、{@code ErpHrEmployeeBizModel:283} 离职/调动后继合同 newContractFrom）
 * 是创建路径的初始态写入，<strong>不</strong>调 {@code assertCan*}。
 */
public class ErpHrEmploymentContractStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * renew 守卫：仅 ACTIVE/EXPIRED 合法（对齐 {@code ErpHrEmploymentContractBizModel.renew:94-95}）。
     * ACTIVE→ACTIVE 自环（续签生效中合同）+ EXPIRED→ACTIVE（续签已过期合同）。
     */
    public void assertCanRenew(String status) {
        if (!ErpHrConstants.CONTRACT_STATUS_ACTIVE.equals(status)
                && !ErpHrConstants.CONTRACT_STATUS_EXPIRED.equals(status)) {
            throw illegal("renew", status, "ACTIVE/EXPIRED");
        }
    }

    public String renewTargetStatus() {
        return ErpHrConstants.CONTRACT_STATUS_ACTIVE;
    }

    public void assertCanExpire(String status) {
        if (!ErpHrConstants.CONTRACT_STATUS_ACTIVE.equals(status)) {
            throw illegal("expire", status, ErpHrConstants.CONTRACT_STATUS_ACTIVE);
        }
    }

    public String expireTargetStatus() {
        return ErpHrConstants.CONTRACT_STATUS_EXPIRED;
    }

    public void assertCanTerminate(String status) {
        if (!ErpHrConstants.CONTRACT_STATUS_ACTIVE.equals(status)) {
            throw illegal("terminate", status, ErpHrConstants.CONTRACT_STATUS_ACTIVE);
        }
    }

    public String terminateTargetStatus() {
        return ErpHrConstants.CONTRACT_STATUS_TERMINATED;
    }

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String status) {
        return ErpHrConstants.CONTRACT_STATUS_EXPIRED.equals(status)
                || ErpHrConstants.CONTRACT_STATUS_TERMINATED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("renew", ErpHrConstants.CONTRACT_STATUS_ACTIVE, ErpHrConstants.CONTRACT_STATUS_ACTIVE),
                new TransitionDefinition("renew", ErpHrConstants.CONTRACT_STATUS_EXPIRED, ErpHrConstants.CONTRACT_STATUS_ACTIVE),
                new TransitionDefinition("expire", ErpHrConstants.CONTRACT_STATUS_ACTIVE, ErpHrConstants.CONTRACT_STATUS_EXPIRED),
                new TransitionDefinition("terminate", ErpHrConstants.CONTRACT_STATUS_ACTIVE, ErpHrConstants.CONTRACT_STATUS_TERMINATED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpHrConstants.CONTRACT_STATUS_EXPIRED,
                ErpHrConstants.CONTRACT_STATUS_TERMINATED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpHrConstants.CONTRACT_STATUS_ACTIVE);
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
