package app.erp.ct.service.statemachine;

import app.erp.ct.service.ErpCtConstants;

import java.util.Collections;
import java.util.List;

/**
 * 返利协议（{@code ErpCtRebateAgreement}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/contract/state-machine.md} §适用对象：返利协议（RebateAgreement）
 * + {@code docs/design/contract/volume-discount.md} §返利计提明细。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。
 *
 * <p><strong>退化分类 Bean（layer-2 四方对照裁定登记）</strong>：dict {@code erp-ct/rebate-agreement-status}
 * 含 4 值（DRAFT/ACTIVE/EXPIRED/SETTLED），但全仓**零命名动作迁移 writer**（无 activate/suspend/expire/
 * terminate/cancel mutation），仅 DRAFT 经 CRUD 创建可达（新建 seed）。ACTIVE/EXPIRED/SETTLED 在命名动作路径下
 * **零 writer 可达**，登记为**预留死状态**（intentional reserved——对齐 Contract CANCELLED/NEGOTIATION +
 * hr SUSPENDED 先例：保留优于删除）。故本 Bean {@link #transitions()} 返回**空列表**（零迁移边），
 * {@link #terminalStatuses()} 亦为空（无终态——三死状态非真正终态，仅预留语义入口）。
 *
 * <p><strong>唯一 live 用途 = 只读 accrual 守卫集中化</strong>：{@code ErpCtRebateAgreementRunAccrualProcessor}
 * 与 {@code RebateEngine} 的 runAccrual/accrual 路径在计提前断言 {@code status==ACTIVE}（否则抛
 * {@code ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE}）。本 Bean 将该只读分类集中为可测元数据 {@link #isActive(String)}，
 * 供两处委托调用，错误码对外不变。
 *
 * <p><strong>返利结算过账边界声明</strong>：返利结算过账操作**独立** {@code ErpCtRebateSettlement} 实体的
 * {@code settlement-status}（DRAFT→POSTED，M4.65 plan-first），生成 credit-memo 发票保存 {@code posted=false}，
 * **与本 RebateAgreement.status 轴无关**。本 Bean 不触及过账（§8 + §3 posted 不入轴）。
 *
 * <p><strong>死状态集合声明</strong>：ACTIVE/EXPIRED/SETTLED **不在** {@link #initialStatuses()} /
 * {@link #terminalStatuses()} / {@link #transitions()} 任一集合（layer-2 裁定登记为 intentional reserved）。
 *
 * <p><strong>Successor</strong>：返利协议 activate/expire/settle 业务流落地时，开独立 plan 实现命名动作 mutation
 * + 填充本 Bean 的 {@link #transitions()} 边；届时 ACTIVE/EXPIRED/SETTLED 转为可达并据实纳入对应集合。
 */
public class ErpCtRebateAgreementStateMachine {

    // ---------- 集中化只读分类（accrual 守卫委托） ----------

    /**
     * 返利计提（accrual）只读守卫：仅 ACTIVE 协议可计提。
     *
     * <p>委托点：{@code ErpCtRebateAgreementRunAccrualProcessor} + {@code RebateEngine}。违例仍由调用方
     * 抛领域码 {@code ERR_CT_REBATE_AGREEMENT_NOT_ACTIVE}（错误码对外不变）。
     */
    public boolean isActive(String status) {
        return ErpCtConstants.REBATE_AGREEMENT_STATUS_ACTIVE.equals(status);
    }

    // ---------- 终态/初始态分类（退化轴如实反映） ----------

    /**
     * 终态分类：**退化轴无终态**（ACTIVE/EXPIRED/SETTLED 为预留死状态，非真正终态；零命名动作迁移 writer）。
     *
     * <p>对所有状态返回 false（含三死状态），如实反映退化解。
     */
    public boolean isTerminal(String status) {
        return false;
    }

    // ---------- 只读元数据接口（退化解：零迁移边） ----------

    /**
     * 迁移元数据：**空列表**（退化轴——零命名动作迁移 writer；详见类 javadoc 死状态声明 + Successor）。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.emptyList();
    }

    /**
     * 终态集合：**空列表**（退化轴无终态；ACTIVE/EXPIRED/SETTLED 为预留死状态，不纳入）。
     */
    public List<String> terminalStatuses() {
        return Collections.emptyList();
    }

    /**
     * 初始态集合：{DRAFT}（新建 seed 经 CRUD 创建写入，§9.2 选项 c 初始态路径）。
     */
    public List<String> initialStatuses() {
        return Collections.singletonList(ErpCtConstants.REBATE_AGREEMENT_STATUS_DRAFT);
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
