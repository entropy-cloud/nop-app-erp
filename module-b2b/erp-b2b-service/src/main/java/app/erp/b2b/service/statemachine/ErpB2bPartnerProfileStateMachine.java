package app.erp.b2b.service.statemachine;

import app.erp.b2b.service.ErpB2bConstants;
import app.erp.common.service.ErpCommonErrors;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * B2B 伙伴档案（{@code ErpB2bPartnerProfile}）上线状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/b2b/partner-onboarding.md §伙伴状态机}（四阶段
 * REGISTERED→TESTING→CERTIFIED→PRODUCTION + 暂停/终止边）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载六态迁移矩阵
 * （REGISTERED/TESTING/CERTIFIED/PRODUCTION/SUSPENDED/TERMINATED）+ 终态/初始态分类
 * + 只读 {@link #transitions()} 元数据。可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归 BizModel（契约 §7）。
 *
 * <h2>已实现迁移矩阵（5 声明动作，12 边）</h2>
 * <ul>
 *   <li>promoteToTesting(REGISTERED→TESTING)</li>
 *   <li>promoteToCertified(TESTING→CERTIFIED)</li>
 *   <li>activate(CERTIFIED→PRODUCTION)</li>
 *   <li>suspend(REGISTERED|TESTING|CERTIFIED|PRODUCTION→SUSPENDED)</li>
 *   <li>deactivate(REGISTERED|TESTING|CERTIFIED|PRODUCTION|SUSPENDED→TERMINATED)</li>
 * </ul>
 *
 * <h2>未落地边 / 不对称裁定（RC-R1.36）</h2>
 * <ul>
 *   <li><b>PRODUCTION→TESTING 回退 + SUSPENDED→原阶段 resume</b>：partner-onboarding.md 状态机图有
 *       但 UC-B2B-007 L1 基本流程未列，本 Bean <b>不编码</b>——Deferred But Adjudicated
 *       （successor = 生产回退/暂停恢复需求立项）；SUSPENDED 无 resume 会滞留伙伴的不对称已登记。</li>
 *   <li><b>TERMINATED</b>：终态，无出边（deactivate 对 TERMINATED 再操作非法）。</li>
 *   <li>activate 仅 CERTIFIED→PRODUCTION（业务规则 1「不可跳过阶段」+ 接口 javadoc
 *       「上线：CERTIFIED→PRODUCTION」），SUSPENDED/CERTIFIED 暂停后无 resume 故不可直接 activate。</li>
 * </ul>
 */
public class ErpB2bPartnerProfileStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    public void assertCanPromoteToTesting(String status) {
        if (!ErpB2bConstants.PARTNER_STATUS_REGISTERED.equals(status)) {
            throw illegal("promoteToTesting", status, ErpB2bConstants.PARTNER_STATUS_REGISTERED);
        }
    }

    public String promoteToTestingTargetStatus() {
        return ErpB2bConstants.PARTNER_STATUS_TESTING;
    }

    public void assertCanPromoteToCertified(String status) {
        if (!ErpB2bConstants.PARTNER_STATUS_TESTING.equals(status)) {
            throw illegal("promoteToCertified", status, ErpB2bConstants.PARTNER_STATUS_TESTING);
        }
    }

    public String promoteToCertifiedTargetStatus() {
        return ErpB2bConstants.PARTNER_STATUS_CERTIFIED;
    }

    public void assertCanActivate(String status) {
        if (!ErpB2bConstants.PARTNER_STATUS_CERTIFIED.equals(status)) {
            throw illegal("activate", status, ErpB2bConstants.PARTNER_STATUS_CERTIFIED);
        }
    }

    public String activateTargetStatus() {
        return ErpB2bConstants.PARTNER_STATUS_PRODUCTION;
    }

    /**
     * suspend 守卫：仅 {@code REGISTERED}/{@code TESTING}/{@code CERTIFIED}/{@code PRODUCTION} 四源合法
     * （partner-onboarding.md 状态机图「REGISTERED / TESTING / CERTIFIED / PRODUCTION → [临时暂停] → SUSPENDED」）。
     * 已暂停（SUSPENDED）/ 终态（TERMINATED）非法。
     */
    public void assertCanSuspend(String status) {
        if (!ErpB2bConstants.PARTNER_STATUS_REGISTERED.equals(status)
                && !ErpB2bConstants.PARTNER_STATUS_TESTING.equals(status)
                && !ErpB2bConstants.PARTNER_STATUS_CERTIFIED.equals(status)
                && !ErpB2bConstants.PARTNER_STATUS_PRODUCTION.equals(status)) {
            throw illegal("suspend", status, "REGISTERED/TESTING/CERTIFIED/PRODUCTION");
        }
    }

    public String suspendTargetStatus() {
        return ErpB2bConstants.PARTNER_STATUS_SUSPENDED;
    }

    /**
     * deactivate 守卫：任意非终态合法（{@code *→TERMINATED}，partner-onboarding.md 状态机图）；
     * 终态 TERMINATED 再操作非法。
     */
    public void assertCanDeactivate(String status) {
        if (ErpB2bConstants.PARTNER_STATUS_TERMINATED.equals(status)) {
            throw illegal("deactivate", status, "非终态（REGISTERED/TESTING/CERTIFIED/PRODUCTION/SUSPENDED）");
        }
    }

    public String deactivateTargetStatus() {
        return ErpB2bConstants.PARTNER_STATUS_TERMINATED;
    }

    // ---------- 终态/初始态分类 ----------

    public boolean isTerminal(String status) {
        return ErpB2bConstants.PARTNER_STATUS_TERMINATED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非 BizModel 主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("promoteToTesting", ErpB2bConstants.PARTNER_STATUS_REGISTERED, ErpB2bConstants.PARTNER_STATUS_TESTING),
                new TransitionDefinition("promoteToCertified", ErpB2bConstants.PARTNER_STATUS_TESTING, ErpB2bConstants.PARTNER_STATUS_CERTIFIED),
                new TransitionDefinition("activate", ErpB2bConstants.PARTNER_STATUS_CERTIFIED, ErpB2bConstants.PARTNER_STATUS_PRODUCTION),
                new TransitionDefinition("suspend", ErpB2bConstants.PARTNER_STATUS_REGISTERED, ErpB2bConstants.PARTNER_STATUS_SUSPENDED),
                new TransitionDefinition("suspend", ErpB2bConstants.PARTNER_STATUS_TESTING, ErpB2bConstants.PARTNER_STATUS_SUSPENDED),
                new TransitionDefinition("suspend", ErpB2bConstants.PARTNER_STATUS_CERTIFIED, ErpB2bConstants.PARTNER_STATUS_SUSPENDED),
                new TransitionDefinition("suspend", ErpB2bConstants.PARTNER_STATUS_PRODUCTION, ErpB2bConstants.PARTNER_STATUS_SUSPENDED),
                new TransitionDefinition("deactivate", ErpB2bConstants.PARTNER_STATUS_REGISTERED, ErpB2bConstants.PARTNER_STATUS_TERMINATED),
                new TransitionDefinition("deactivate", ErpB2bConstants.PARTNER_STATUS_TESTING, ErpB2bConstants.PARTNER_STATUS_TERMINATED),
                new TransitionDefinition("deactivate", ErpB2bConstants.PARTNER_STATUS_CERTIFIED, ErpB2bConstants.PARTNER_STATUS_TERMINATED),
                new TransitionDefinition("deactivate", ErpB2bConstants.PARTNER_STATUS_PRODUCTION, ErpB2bConstants.PARTNER_STATUS_TERMINATED),
                new TransitionDefinition("deactivate", ErpB2bConstants.PARTNER_STATUS_SUSPENDED, ErpB2bConstants.PARTNER_STATUS_TERMINATED)));
    }

    public List<String> terminalStatuses() {
        return Collections.singletonList(ErpB2bConstants.PARTNER_STATUS_TERMINATED);
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpB2bConstants.PARTNER_STATUS_REGISTERED);
    }

    // ---------- 内部 ----------

    private static NopException illegal(String action, String currentStatus, String expectedStatus) {
        return new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                .param(ErpCommonErrors.ARG_CURRENT_STATUS, currentStatus)
                .param(ErpCommonErrors.ARG_EXPECTED_STATUS, expectedStatus)
                .param(ARG_ACTION, action);
    }

    /**
     * 只读迁移定义记录（供 M5.1/M5.2 可达性/完备性分析与文档一致性校验消费）。
     *
     * <p>字段名 {@code fromStatus/toStatus} 为通用契约命名；本轴 {@code status} 字段语义等价。
     */
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
