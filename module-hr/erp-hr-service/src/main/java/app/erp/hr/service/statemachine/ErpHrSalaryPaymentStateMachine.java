package app.erp.hr.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.hr.service.ErpHrConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 薪酬记录（{@code ErpHrSalary}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code paymentStatus} 发放执行独立轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/human-resource/state-machine.md §适用对象四 §4 发放执行独立轴}。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载支付轴 2 动作迁移矩阵
 * （markPaid PENDING→PAID、voidSalary PENDING→VOID）+ 终态/初始态分类 + 只读 {@link #transitions()} 元数据。
 * 可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>命名带 {@code Payment} 后缀（契约 §1 双轴约定，与 {@code ErpHrSalaryApprovalStateMachine} approveStatus 轴分离）。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 Processor/BizModel（契约 §7）。
 *
 * <p>迁移矩阵（2 条边，编码<strong>已实现</strong>行为 + owner doc §4）：markPaid(PENDING→PAID)、
 * voidSalary(PENDING→VOID)。PAID/VOID 均为纯终态（无出边）。PAID 终态由领域
 * {@code ERR_SALARY_LOCKED_AFTER_PAID} 在 BizModel 接线层专属处理（§11.4 终态领域异常重叠模式），
 * Bean 仍按统一 common 码报告 PAID 源态非法。
 */
public class ErpHrSalaryPaymentStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * markPaid 守卫：来源态为 {@code PENDING} 合法（发放执行仅从未发放发起）。
     *
     * <p>接线方 {@code ErpHrSalaryMarkPaidProcessor} / {@code ErpHrSalaryGenerateBankFileProcessor}
     * 映射为领域 {@code ERR_SALARY_ILLEGAL_STATUS_TRANSITION}。
     */
    public void assertCanMarkPaid(String paymentStatus) {
        if (!ErpHrConstants.PAYMENT_PENDING.equals(paymentStatus)) {
            throw illegal("markPaid", paymentStatus, ErpHrConstants.PAYMENT_PENDING);
        }
    }

    public String markPaidTargetStatus() {
        return ErpHrConstants.PAYMENT_PAID;
    }

    /**
     * voidSalary 守卫：来源态为 {@code PENDING} 合法（作废仅从未发放发起）。
     *
     * <p>{@code PAID} 源态经领域 {@code ERR_SALARY_LOCKED_AFTER_PAID} 在 BizModel 接线层专属处理（§11.4
     * 终态领域异常重叠），其余非法源态（VOID/null）由 Bean 报告 common 层非法边 → BizModel 映射领域码。
     */
    public void assertCanVoid(String paymentStatus) {
        if (!ErpHrConstants.PAYMENT_PENDING.equals(paymentStatus)) {
            throw illegal("voidSalary", paymentStatus, ErpHrConstants.PAYMENT_PENDING);
        }
    }

    public String voidTargetStatus() {
        return ErpHrConstants.PAYMENT_VOID;
    }

    // ---------- 终态/初始态分类 ----------

    /** 支付轴终态 = {PAID, VOID}（PAID 锁定不可改；VOID 作废终态，owner doc §4）。 */
    public boolean isTerminal(String paymentStatus) {
        return ErpHrConstants.PAYMENT_PAID.equals(paymentStatus)
                || ErpHrConstants.PAYMENT_VOID.equals(paymentStatus);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 Processor 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("markPaid", ErpHrConstants.PAYMENT_PENDING, ErpHrConstants.PAYMENT_PAID),
                new TransitionDefinition("voidSalary", ErpHrConstants.PAYMENT_PENDING, ErpHrConstants.PAYMENT_VOID)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpHrConstants.PAYMENT_PAID,
                ErpHrConstants.PAYMENT_VOID));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpHrConstants.PAYMENT_PENDING);
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
