package app.erp.hr.service.statemachine;

import app.erp.hr.service.ErpHrConstants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 员工雇佣状态（{@code ErpHrEmployee.employmentStatus}）实体级状态机 Bean —— 一 Bean 对应一实体一轴
 * （{@code employmentStatus}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/human-resource/state-machine.md §适用对象二}（Deferred）。
 *
 * <p><strong>退化解（degenerate axis）</strong>：当前生产代码<strong>零命名动作迁移 writer</strong>
 * （无 {@code setEmploymentStatus(RESIGNED|TERMINATED|RETIRED|PROBATION)} 生产 writer、无
 * resignEmployee/retireEmployee/terminateEmployee/probationToRegular mutation）。故 {@link #transitions()}
 * 返回<strong>空列表</strong>，无 {@code assertCan<Action>} 迁移方法。Bean 此处如实反映「无活态迁移」。
 *
 * <p>Bean 价值为<strong>纯分类 + 死状态登记</strong>载体（契约 §8 退化轴）：
 * <ul>
 *   <li>{@link #initialStatuses()} = {ACTIVE, PROBATION}（owner doc §1 业务语义：在职/试用期入口）；</li>
 *   <li>{@link #terminalStatuses()} = {RESIGNED, TERMINATED, RETIRED}（owner doc §3 业务语义：业务生命周期终点，
 *       <strong>对齐 §3 显式声明</strong>）；</li>
 *   <li>{@link #isTerminal(String)} 对三终态返回 true；</li>
 *   <li>{@link #isTransferable(String)} / {@link #nonTransferableStatuses()} 集中化只读调动守卫判断
 *       （替代 BizModel + Processor 双副本内联 {@code Objects.equals} 矩阵）。</li>
 * </ul>
 *
 * <p><strong>死状态 vs 终态（§11.4 显式裁定）</strong>：RESIGNED/TERMINATED/RETIRED 当前不可达
 * （零 writer = 无入边），但「死」（无入边）与「终态」（业务生命周期终点、无出边）不矛盾。退化解下全部
 * 状态无出边，故「终态」按 owner doc §3 业务语义裁定（否则 ACTIVE/PROBATION 也无出边会被误判终态）。
 * 三者「不在 transitions/initial 集合」+ 本 javadoc 单独表达「死」语义。
 *
 * <p><strong>PROBATION 零 writer 不对称</strong>：{@link #initialStatuses()} 含 PROBATION（owner doc §1
 * 业务语义为试用期初始态），但 grep 证实 PROBATION 与三死状态同样<strong>零生产 writer</strong>
 * （2 处入职均写 ACTIVE：{@code ErpHrRecruitmentHireProcessor:63} + {@code ErpHrRecruitmentBizModel:149}）。
 * PROBATION 归 initial（业务语义，试用期入口），与三死状态归 terminal 对称；二者当前均不可达，successor
 * （转正/入职试用期流）填充 writer 后二者均可达。区分基于 owner doc §1/§3 业务语义而非当前可达性。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务。可经 Delta 同名 Bean 覆盖（契约 §6）。
 *
 * <p><strong>初始态 ACTIVE 写入路径不经 Bean</strong>（契约 §9.2 选项 c）：2 处
 * {@code setEmploymentStatus(ACTIVE)} 初始态写入（{@code ErpHrRecruitmentHireProcessor:63} 入职新建 +
 * {@code ErpHrRecruitmentBizModel:149} 入职新建 legacy dup）是创建路径的初始态写入，<strong>不</strong>调
 * {@code assertCan*}（本 Bean 无迁移方法，亦无对应接线）。
 *
 * <p><strong>transferEmployee 不改 employmentStatus</strong>（owner doc §适用对象二）：调动委托
 * {@code ErpHrEmployeeTransferEmployeeProcessor}，仅改 departmentId/positionId/superiorId + 处理合同，
 * <strong>不</strong>写 employmentStatus（仅读守卫 {@code isTransferable}）。
 */
public class ErpHrEmployeeStateMachine {

    // ---------- 终态/初始态分类 ----------

    /**
     * 终态分类（owner doc §3 业务语义）：RESIGNED/TERMINATED/RETIRED 为业务生命周期终点。
     * 三者当前零 writer = 不可达（死状态），但 isTerminal=true 对齐 §3。
     */
    public boolean isTerminal(String status) {
        return ErpHrConstants.EMPLOYMENT_RESIGNED.equals(status)
                || ErpHrConstants.EMPLOYMENT_TERMINATED.equals(status)
                || ErpHrConstants.EMPLOYMENT_RETIRED.equals(status);
    }

    // ---------- 只读调动守卫（集中化，替代 BizModel + Processor 双副本） ----------

    /**
     * 只读调动守卫：仅 ACTIVE/PROBATION 可调动（对齐 {@code ErpHrEmployeeBizModel.isTransferable} +
     * {@code ErpHrEmployeeTransferEmployeeProcessor.isTransferable} 双副本）。
     */
    public boolean isTransferable(String employmentStatus) {
        return ErpHrConstants.EMPLOYMENT_ACTIVE.equals(employmentStatus)
                || ErpHrConstants.EMPLOYMENT_PROBATION.equals(employmentStatus);
    }

    /**
     * 不可调动雇佣状态集合（对齐 {@code ErpHrEmployeeBizModel.nonTransferableStatuses}）。
     * = 三终态（RESIGNED/TERMINATED/RETIRED）。BizModel/Processor 在 {@link #isTransferable(String)}
     * 返回 false 时抛领域码 {@code ERR_EMPLOYEE_NOT_TRANSFERABLE}（对外不变），本 Bean 不抛 common 非法迁移码
     * ——调动守卫是只读判断而非状态迁移，领域有专属错误码（契约 §7 + §11.4 cancel 多来源态范式）。
     */
    public List<String> nonTransferableStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpHrConstants.EMPLOYMENT_RESIGNED,
                ErpHrConstants.EMPLOYMENT_TERMINATED,
                ErpHrConstants.EMPLOYMENT_RETIRED));
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非主调用路径） ----------

    /**
     * 迁移定义列表：<strong>空</strong>（退化解，如实反映零命名动作迁移 writer）。
     * successor（离职/退休/转正 mutation 落地）填充入边后此列表扩展。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.emptyList();
    }

    /**
     * 终态集合（owner doc §3）：{RESIGNED, TERMINATED, RETIRED}。
     * 三者当前死状态（零入边），但按 §3 业务语义归终态。
     */
    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpHrConstants.EMPLOYMENT_RESIGNED,
                ErpHrConstants.EMPLOYMENT_TERMINATED,
                ErpHrConstants.EMPLOYMENT_RETIRED));
    }

    /**
     * 初始态集合（owner doc §1）：{ACTIVE, PROBATION}。
     * 二者为业务生命周期入口（在职/试用期）。注意 PROBATION 当前零 writer（与 ACTIVE 不对称），
     * 归 initial 基于 §1 业务语义而非当前可达性。
     */
    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpHrConstants.EMPLOYMENT_ACTIVE,
                ErpHrConstants.EMPLOYMENT_PROBATION));
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
