package app.erp.aps.service.processor;

import app.erp.aps.dao.entity.ErpApsOperationOrder;
import app.erp.aps.dao.entity.ErpApsOpRouting;
import app.erp.aps.service.ErpApsConstants;
import app.erp.aps.service.ErpApsErrors;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * UC-APS-06 manualOverrideRouting per-mutation Processor（RC-R1.87，{@code alternative-routing.md §4.3 人工强制指定}）。
 *
 * <p>计划员强制指定路由：剥离上次选中路由的时间差 → 叠加新路由时间差 → 回写
 * {@code machineId/selectedRoutingId/manualOverride=true} → 回退 DRAFT 并释放产能预留（下次重排在新工作中心
 * 重新排程；{@code manualOverride=true} 的工序重排时跳过自动路由选择，保持人工指定）。
 *
 * <p>审计：remark 追加强制记录（计划员/路由/时间差），选中路由与强制标记本身即可追溯审计载体。
 *
 * <p>事务边界：跟随 Facade {@code @BizMutation} 事务。
 */
public class ErpApsRoutingManualOverrideProcessor {

    public ErpApsOperationOrder manualOverrideRouting(ErpApsSchedulingProcessor facade,
                                                      String operationOrderId, String routingId,
                                                      IServiceContext context) {
        ErpApsOperationOrder op = facade.requireOperationOrder(operationOrderId, context);
        ErpApsOpRouting row = requireRouting(facade, routingId);

        validateOverridableStatus(op);

        // 剥离上次选中路由时间差（若路由已删除则按当前值，等效差值 0）
        BigDecimal setup = op.getSetupTime() == null ? BigDecimal.ZERO : op.getSetupTime();
        BigDecimal perUnit = op.getRuntimePerUnit() == null ? BigDecimal.ZERO : op.getRuntimePerUnit();
        ErpApsOpRouting previous = findRouting(facade, op.getSelectedRoutingId());
        if (previous != null) {
            setup = setup.subtract(deltaOrZero(previous.getSetupTimeDelta()));
            perUnit = perUnit.subtract(deltaOrZero(previous.getRuntimePerUnitDelta()));
        }

        // 叠加新路由时间差并回写
        setup = setup.add(deltaOrZero(row.getSetupTimeDelta()));
        perUnit = perUnit.add(deltaOrZero(row.getRuntimePerUnitDelta()));
        op.setSetupTime(setup);
        op.setRuntimePerUnit(perUnit);
        op.setMachineId(row.getMachineId());
        op.setSelectedRoutingId(row.getId());
        op.setManualOverride(Boolean.TRUE);
        op.setRoutingSelectionReason(null); // 人工指定语义由 manualOverride 标记承载，非四类自动原因
        BigDecimal qty = op.getQty() == null ? BigDecimal.ZERO : op.getQty();
        op.setTotalDuration(BigDecimal.valueOf(Math.max(1L,
                setup.add(perUnit.multiply(qty)).setScale(0, RoundingMode.CEILING).longValueExact())));

        // 回退排程状态：释放产能预留 + 清空计划时间（PLANNED→DRAFT 重排回退路径，state-machine.md §2）
        if (ErpApsConstants.OP_STATUS_PLANNED.equals(op.getStatus())) {
            facade.releaseReservationsByOrder(op.getId());
        }
        op.setStatus(ErpApsConstants.OP_STATUS_DRAFT);
        op.setPlannedStartDateT(null);
        op.setPlannedEndDateT(null);

        String audit = "manualOverrideRouting→routingId=" + row.getId() + ",machineId=" + row.getMachineId()
                + ",setupDelta=" + deltaOrZero(row.getSetupTimeDelta())
                + ",perUnitDelta=" + deltaOrZero(row.getRuntimePerUnitDelta())
                + ",by=" + (context != null && context.getUserId() != null ? context.getUserId() : "unknown");
        op.setRemark(op.getRemark() == null || op.getRemark().isEmpty() ? audit : op.getRemark() + " | " + audit);

        facade.opOrderDao().updateEntity(op);
        return op;
    }

    protected void validateOverridableStatus(ErpApsOperationOrder op) {
        String s = op.getStatus();
        if (ErpApsConstants.OP_STATUS_DRAFT.equals(s) || ErpApsConstants.OP_STATUS_PLANNED.equals(s)
                || ErpApsConstants.OP_STATUS_UNSCHEDULABLE.equals(s)) {
            return;
        }
        throw new NopException(ErpApsErrors.ERR_APS_OP_ILLEGAL_TRANSITION)
                .param(ErpApsErrors.ARG_OP_CODE, op.getCode())
                .param(ErpApsErrors.ARG_CURRENT_STATUS, s)
                .param(ErpApsErrors.ARG_EXPECTED_STATUS,
                        ErpApsConstants.OP_STATUS_DRAFT + "/" + ErpApsConstants.OP_STATUS_PLANNED
                                + "/" + ErpApsConstants.OP_STATUS_UNSCHEDULABLE);
    }

    protected ErpApsOpRouting requireRouting(ErpApsSchedulingProcessor facade, String routingId) {
        ErpApsOpRouting row = findRouting(facade, routingId);
        if (row == null || !Boolean.TRUE.equals(row.getIsEnabled())) {
            throw new NopException(ErpApsErrors.ERR_APS_ROUTING_NOT_AVAILABLE)
                    .param(ErpApsErrors.ARG_ROUTING_ID, routingId);
        }
        return row;
    }

    protected ErpApsOpRouting findRouting(ErpApsSchedulingProcessor facade, String routingId) {
        if (routingId == null) {
            return null;
        }
        return facade.opRoutingDao().getEntityById(routingId);
    }

    private static BigDecimal deltaOrZero(BigDecimal delta) {
        return delta == null ? BigDecimal.ZERO : delta;
    }
}
