package app.erp.log.service.statemachine;

import app.erp.common.service.ErpCommonErrors;
import app.erp.log.service.ErpLogConstants;
import io.nop.api.core.exceptions.NopException;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 发运单（{@code ErpLogShipment}）实体级状态机 Bean —— 一 Bean 对应一实体一轴（{@code status}
 * 业务生命周期轴，字典 {@code erp-log/shipment-status}，6 值 DRAFT/ADVISED/DISPATCHED/IN_TRANSIT/DELIVERED/CANCELLED）。
 *
 * <p>权威契约：{@code docs/architecture/entity-state-machine-bean.md}；
 * 业务语义：{@code docs/design/logistics/state-machine.md} §适用对象（Shipment 6 态）。
 *
 * <p><b>治理裁定（§11.2 M4）</b>：DELIVERED 触发 path-1 FREIGHT 运费过账（{@code IErpFinVoucherBiz.post}，
 * 直接调用 {@code InvPostingExecutor} 范式）+ path-2 config-gated 到岸成本自动创建（{@code IErpInvLandedCostBiz}）。
 * 依契约 §11.2 M4 (ii)/(iv)/(v)，过账时序/失败回退（freightSettlementStatus 保持 PENDING）/网关重试/死信/告警
 * <b>原序保留</b>在 Processor/`I*Biz` 路径；{@code posted}（boolean）不入轴（契约 §3，本实体无独立 setPosted writer，
 * 运费结算经 {@code freightSettlementStatus} 独立轴）。本 Bean 为 M4 plan-first 产物（人工门控 2026-08-14 已确认解除）。
 *
 * <p>严格无状态（契约 §2）：不注入 DAO/IBiz/IServiceContext/事务，只接收状态值。承载 5 命名动作迁移矩阵
 * （advise/completeShipment/advanceToInTransit/advanceToDelivered/cancelShipment）+ 终态/初始态分类 + 只读
 * {@link #transitions()} 元数据。可经 Delta 同名 Bean 覆盖（契约 §6）替换基线矩阵。
 *
 * <p>非法边抛 common 层 {@link ErpCommonErrors#ERR_ILLEGAL_STATUS_TRANSITION}（参数 {@code currentStatus}/
 * {@code expectedStatus}），并附 {@code action} 补充诊断参数；领域 ErrorCode 映射归接线方
 * {@code GatewayDispatcher}（契约 §7）。
 *
 * <p>迁移矩阵（10 条边）：advise(DRAFT→ADVISED)、completeShipment(ADVISED→DISPATCHED)、
 * advanceToInTransit(DISPATCHED→IN_TRANSIT)、advanceToDelivered {ADVISED,DISPATCHED,IN_TRANSIT}→DELIVERED = 3 边
 * （<b>刻意收紧</b>——Decision (C)：code 原无来源态守卫，Bean 排除 DRAFT/CANCELLED 来源）、
 * cancelShipment 多源 {DRAFT,ADVISED,DISPATCHED,IN_TRANSIT}→CANCELLED = 4 边。
 * 分类 initial={DRAFT}、terminal={DELIVERED, CANCELLED}。
 *
 * <p>幂等 short-circuit（已 ADVISED/DISPATCHED+/CANCELLED|DELIVERED/已 DELIVERED）为动态流程控制，
 * 保留在 {@code GatewayDispatcher}（Decision (B)），本 Bean 不承载。
 */
public class ErpLogShipmentStateMachine {

    /** 补充诊断参数键：被拒绝的动作名（供 M5.2 守卫/诊断消费）。 */
    public static final String ARG_ACTION = "action";

    // ---------- 显式动作方法（主路径） ----------

    /**
     * advise 守卫：来源态为 {@code DRAFT} 合法（DRAFT→ADVISED）。
     *
     * <p>非法来源态报告 common 层非法边（携带 {@code action=advise}/{@code fromStatus}）。
     * 接线方 {@code GatewayDispatcher.advise} 映射为领域码 {@code ERR_LOG_SHIPMENT_ILLEGAL_TRANSITION}
     * （保持既有 expected=DRAFT 文案）。
     */
    public void assertCanAdvise(String status) {
        if (!ErpLogConstants.SHIPMENT_STATUS_DRAFT.equals(status)) {
            throw illegal("advise", status, ErpLogConstants.SHIPMENT_STATUS_DRAFT);
        }
    }

    /**
     * completeShipment 守卫：来源态为 {@code ADVISED} 合法（ADVISED→DISPATCHED）。
     *
     * <p>接线方 {@code GatewayDispatcher.completeShipment} 映射为领域码（保持既有 expected=ADVISED 文案）。
     */
    public void assertCanCompleteShipment(String status) {
        if (!ErpLogConstants.SHIPMENT_STATUS_ADVISED.equals(status)) {
            throw illegal("completeShipment", status, ErpLogConstants.SHIPMENT_STATUS_ADVISED);
        }
    }

    /**
     * advanceToInTransit 守卫：来源态为 {@code DISPATCHED} 合法（DISPATCHED→IN_TRANSIT，
     * TRACKING_EVENT_IN_TRANSIT/PICKED_UP）。
     *
     * <p>接线方 {@code GatewayDispatcher.advanceTracking} 映射为领域码。
     */
    public void assertCanAdvanceToInTransit(String status) {
        if (!ErpLogConstants.SHIPMENT_STATUS_DISPATCHED.equals(status)) {
            throw illegal("advanceToInTransit", status, ErpLogConstants.SHIPMENT_STATUS_DISPATCHED);
        }
    }

    /**
     * advanceToDelivered 守卫：来源态为 {@code ADVISED}/{@code DISPATCHED}/{@code IN_TRANSIT} 合法
     * （→DELIVERED，TRACKING_EVENT_DELIVERED）。
     *
     * <p><b>刻意收紧（Decision (C)）</b>：code 原写入无来源态守卫（任何非 DELIVERED 状态含 DRAFT/CANCELLED
     * 均可被推进到 DELIVERED），本 Bean 排除 DRAFT/CANCELLED 来源——DRAFT 无 trackingNo 不可达（advanceTracking
     * 按 trackingNo 定位运单）、CANCELLED 是终态不应可逆到 DELIVERED。行为收紧（安全改善），四方对照登记
     * {@code intentional narrowing}。接线方映射为领域码。
     */
    public void assertCanAdvanceToDelivered(String status) {
        if (!ErpLogConstants.SHIPMENT_STATUS_ADVISED.equals(status)
                && !ErpLogConstants.SHIPMENT_STATUS_DISPATCHED.equals(status)
                && !ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT.equals(status)) {
            throw illegal("advanceToDelivered", status, "ADVISED/DISPATCHED/IN_TRANSIT");
        }
    }

    /**
     * cancelShipment 守卫：来源态为 {@code DRAFT}/{@code ADVISED}/{@code DISPATCHED}/{@code IN_TRANSIT} 合法
     * （多源→CANCELLED）。
     *
     * <p>接线方 {@code GatewayDispatcher.cancelShipment} 映射为领域码（保持既有 expected=ADVISED 文案）。
     */
    public void assertCanCancelShipment(String status) {
        if (!ErpLogConstants.SHIPMENT_STATUS_DRAFT.equals(status)
                && !ErpLogConstants.SHIPMENT_STATUS_ADVISED.equals(status)
                && !ErpLogConstants.SHIPMENT_STATUS_DISPATCHED.equals(status)
                && !ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT.equals(status)) {
            throw illegal("cancelShipment", status, "DRAFT/ADVISED/DISPATCHED/IN_TRANSIT");
        }
    }

    // ---------- 动作目标态（供接线方写回） ----------

    public String adviseTargetStatus() {
        return ErpLogConstants.SHIPMENT_STATUS_ADVISED;
    }

    public String completeShipmentTargetStatus() {
        return ErpLogConstants.SHIPMENT_STATUS_DISPATCHED;
    }

    public String advanceToInTransitTargetStatus() {
        return ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT;
    }

    public String advanceToDeliveredTargetStatus() {
        return ErpLogConstants.SHIPMENT_STATUS_DELIVERED;
    }

    public String cancelShipmentTargetStatus() {
        return ErpLogConstants.SHIPMENT_STATUS_CANCELLED;
    }

    // ---------- 终态/初始态分类 ----------

    /**
     * 业务终态判定。发运单终态为 {@code DELIVERED} 与 {@code CANCELLED}（均无后续出边——DELIVERED 的退货走
     * sales 域标准退货流程、CANCELLED 后新建发运单，均非状态迁移）。
     */
    public boolean isTerminal(String status) {
        return ErpLogConstants.SHIPMENT_STATUS_DELIVERED.equals(status)
                || ErpLogConstants.SHIPMENT_STATUS_CANCELLED.equals(status);
    }

    // ---------- 只读元数据接口（完备性/可达性分析用，非主调用路径） ----------

    public List<TransitionDefinition> transitions() {
        return Collections.unmodifiableList(Arrays.asList(
                new TransitionDefinition("advise", ErpLogConstants.SHIPMENT_STATUS_DRAFT, ErpLogConstants.SHIPMENT_STATUS_ADVISED),
                new TransitionDefinition("completeShipment", ErpLogConstants.SHIPMENT_STATUS_ADVISED, ErpLogConstants.SHIPMENT_STATUS_DISPATCHED),
                new TransitionDefinition("advanceToInTransit", ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT),
                new TransitionDefinition("advanceToDelivered", ErpLogConstants.SHIPMENT_STATUS_ADVISED, ErpLogConstants.SHIPMENT_STATUS_DELIVERED),
                new TransitionDefinition("advanceToDelivered", ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, ErpLogConstants.SHIPMENT_STATUS_DELIVERED),
                new TransitionDefinition("advanceToDelivered", ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT, ErpLogConstants.SHIPMENT_STATUS_DELIVERED),
                new TransitionDefinition("cancelShipment", ErpLogConstants.SHIPMENT_STATUS_DRAFT, ErpLogConstants.SHIPMENT_STATUS_CANCELLED),
                new TransitionDefinition("cancelShipment", ErpLogConstants.SHIPMENT_STATUS_ADVISED, ErpLogConstants.SHIPMENT_STATUS_CANCELLED),
                new TransitionDefinition("cancelShipment", ErpLogConstants.SHIPMENT_STATUS_DISPATCHED, ErpLogConstants.SHIPMENT_STATUS_CANCELLED),
                new TransitionDefinition("cancelShipment", ErpLogConstants.SHIPMENT_STATUS_IN_TRANSIT, ErpLogConstants.SHIPMENT_STATUS_CANCELLED)));
    }

    public List<String> terminalStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
                ErpLogConstants.SHIPMENT_STATUS_DELIVERED, ErpLogConstants.SHIPMENT_STATUS_CANCELLED));
    }

    public List<String> initialStatuses() {
        return Collections.singletonList(ErpLogConstants.SHIPMENT_STATUS_DRAFT);
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
