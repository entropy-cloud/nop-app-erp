package app.erp.ast.service.statemachine;

import app.erp.ast.service.ErpAstConstants;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 资产移动单（{@code ErpAstMovement}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code docStatus} 业务生命周期轴）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/assets/state-machine.md §适用对象二：资产移动单}（退化轴声明）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。
 *
 * <p><strong>退化分类 Bean（layer-2 四方对照裁定登记）</strong>：dict {@code erp/doc-status} 含 3 值
 * （DRAFT/ACTIVE/CANCELLED），但 Movement 全仓**零命名动作迁移 writer**（无 cancel/activate mutation）。
 * {@code docStatus=CANCELLED} 经 {@code useLogicalDelete} 既有路径承载（owner doc「实现模式与守卫边界」已声明），
 * 无独立 cancel mutation。{@code ACTIVE} 在 dict 内但 Movement 无 writer → ACTIVE 为 Movement 的**死状态**
 * （对齐 assets 域 R1.x 保留死状态先例：保留优于删除，为预留语义入口）。
 *
 * <p>故本 Bean {@link #transitions()} 返回**空列表**（零迁移边，对齐 RebateAgreement 退化 Bean 先例）。
 *
 * <p><strong>唯一 live 用途 = 只读防御守卫集中化</strong>：{@code ErpAstMovement.xbiz} 5 个审批动作均前置
 * {@code isCancelled} 守卫（CANCELLED 单据禁止审批操作）。本 Bean 将该只读分类集中为可测方法 {@link #isCancelled(String)}，
 * 供 xbiz source 委托调用，错误码 {@code nop.err.wf.approve.doc-cancelled} 对外不变。
 *
 * <p><strong>死状态集合声明</strong>：{@code ACTIVE} **不在** {@link #initialStatuses()} /
 * {@link #transitions()} 任一集合（layer-2 裁定登记为 intentional reserved）。{@code CANCELLED} 经
 * {@code useLogicalDelete} 可达，纳入 {@link #terminalStatuses()}。
 *
 * <p><strong>Successor</strong>：资产移动单独立 cancel/activate 工作流需求时，开独立 plan 实现命名动作 mutation
 * + 填充本 Bean 的 {@link #transitions()} 边；届时 ACTIVE 转为可达并据实纳入对应集合。
 */
public class ErpAstMovementDocumentStateMachine {

    // ---------- 集中化只读分类（xbiz source 防御守卫委托） ----------

    /**
     * 只读防御守卫：判断 docStatus 是否为 CANCELLED（作废单据禁止审批操作）。
     *
     * <p>委托点：{@code ErpAstMovement.xbiz} 5 个审批动作（submitForApproval/approve/reject/reverseApprove/
     * withdrawApproval）的前置守卫。违例由 xbiz source 抛领域码 {@code nop.err.wf.approve.doc-cancelled}
     * （错误码对外不变）。
     */
    public boolean isCancelled(String docStatus) {
        return ErpAstConstants.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    // ---------- 终态/初始态分类（退化轴如实反映） ----------

    /**
     * 终态分类：CANCELLED 为经 {@code useLogicalDelete} 可达的终态；ACTIVE 为死状态（非真正终态，仅预留）。
     */
    public boolean isTerminal(String docStatus) {
        return ErpAstConstants.DOC_STATUS_CANCELLED.equals(docStatus);
    }

    // ---------- 只读元数据接口（退化解：零迁移边） ----------

    /**
     * 迁移元数据：**空列表**（退化轴——零命名动作迁移 writer；详见类 javadoc 死状态声明 + Successor）。
     */
    public List<TransitionDefinition> transitions() {
        return Collections.emptyList();
    }

    /**
     * 终态集合：{CANCELLED}（经 {@code useLogicalDelete} 可达；ACTIVE 为死状态不纳入终态集合）。
     */
    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpAstConstants.DOC_STATUS_CANCELLED));
    }

    /**
     * 初始态集合：{DRAFT}（新建 seed 经 CRUD 创建写入）。
     */
    public List<String> initialStatuses() {
        return Collections.unmodifiableList(Arrays.asList(ErpAstConstants.DOC_STATUS_DRAFT));
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
