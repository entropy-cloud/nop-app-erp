package app.erp.mfg.service.processor;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.workorder.KitAvailabilityChecker;
import app.erp.mfg.service.workorder.KitAvailabilityResult;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.qa.biz.IErpQaInspectionBiz;
import app.erp.qa.biz.InspectionTrigger;
import app.erp.qa.dao._ErpQaDaoConstants;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import java.util.Objects;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 工单状态机编排 Processor（{@code processor-extension-pattern.md} 两层结构：Facade + Processor）。
 * 标准审批动作（submitForApproval/approve/reject/reverseApprove/withdrawApproval）由本类全权处理：
 * 加载实体 → 状态守卫 → 业务校验 → setDocStatus/setApproveStatus → 保存返回。
 * xbiz 仅写一行委托：{@code return inject('processor').submitForApproval(id, svcCtx)}。
 *
 * <p>各步骤为 {@code protected} 方法、单一职责、以 {@link IServiceContext} 为末参。
 * 客户/行业覆盖单步实现时，写派生 Processor 重载目标 {@code protected} 方法，在 Delta beans.xml
 * 以同名 bean id 注册覆盖基线。
 *
 * <p>事务边界：跟随 xbiz mutation（由 approval-support.xbiz 标准 source 的 @BizMutation 保护），本类不带 @Transactional。
 */
public class ErpMfgWorkOrderProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    KitAvailabilityChecker kitAvailabilityChecker;
    @Inject
    IErpInvStockMoveBiz stockMoveBiz;
    @Inject
    IErpQaInspectionBiz inspectionBiz;

    public ErpMfgWorkOrder submitForApproval(String id, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(id, context);
        validateTransitionForSubmit(wo, context);
        validateBusinessRulesForSubmit(wo, context);
        doSubmit(wo, context);
        return wo;
    }

    public ErpMfgWorkOrder withdrawApproval(String id, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(id, context);
        validateTransitionForWithdraw(wo, context);
        doWithdrawSubmit(wo, context);
        return wo;
    }

    public ErpMfgWorkOrder approve(String id, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(id, context);
        validateTransitionForApprove(wo, context);
        validateBusinessRulesForApprove(wo, context);
        doApprove(wo, context);
        return wo;
    }

    public ErpMfgWorkOrder reject(String id, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(id, context);
        validateTransitionForReject(wo, context);
        doReject(wo, context);
        return wo;
    }

    public ErpMfgWorkOrder reverseApprove(String id, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(id, context);
        validateTransitionForReverseApprove(wo, context);
        doReverseApprove(wo, context);
        return wo;
    }

    public ErpMfgWorkOrder checkAvailability(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(String.valueOf(workOrderId), context);
        requireStatus(wo, ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, "NOT_STARTED");
        KitAvailabilityResult result = kitAvailabilityChecker.check(workOrderId);
        wo.setDocStatus(result.getResultingStatus());
        workOrderDao().updateEntity(wo);
        return wo;
    }

    public ErpMfgWorkOrder start(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(String.valueOf(workOrderId), context);
        validateTransitionForStart(wo, context);
        doStart(wo, context);
        return wo;
    }

    public ErpMfgWorkOrder stop(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(String.valueOf(workOrderId), context);
        requireStatus(wo, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, "IN_PROCESS");
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_STOPPED);
        workOrderDao().updateEntity(wo);
        return wo;
    }

    public ErpMfgWorkOrder resume(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(String.valueOf(workOrderId), context);
        requireStatus(wo, ErpMfgConstants.WORK_ORDER_STATUS_STOPPED, "STOPPED");
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
        workOrderDao().updateEntity(wo);
        return wo;
    }

    public ErpMfgWorkOrder close(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(String.valueOf(workOrderId), context);
        String status = wo.getDocStatus();
        if (status == null || (!Objects.equals(status, ErpMfgConstants.WORK_ORDER_STATUS_STOPPED)
                && !Objects.equals(status, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS))) {
            throw illegalTransition(wo, status, "STOPPED 或 IN_PROCESS");
        }
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_CLOSED);
        if (wo.getActualEndDate() == null) {
            wo.setActualEndDate(CoreMetrics.today());
        }
        workOrderDao().updateEntity(wo);
        return wo;
    }

    public ErpMfgWorkOrder cancel(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(String.valueOf(workOrderId), context);
        String status = wo.getDocStatus();
        if (status == null || (!Objects.equals(status, ErpMfgConstants.WORK_ORDER_STATUS_DRAFT)
                && !Objects.equals(status, ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED)
                && !Objects.equals(status, ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED))) {
            throw illegalTransition(wo, status, "DRAFT、SUBMITTED 或 NOT_STARTED");
        }
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED);
        workOrderDao().updateEntity(wo);
        return wo;
    }

    /**
     * 完工入库：累加完工数量、生成产成品入库移动单（MANUFACTURING，库存域视为入库 → 加产成品库存）、
     * 重算成本（totalCost = material+labor+overhead+subcontract；unitCost = total/completed），完工达量→COMPLETED。
     */
    public ErpMfgWorkOrder reportCompletion(Long workOrderId, BigDecimal completedQty, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(String.valueOf(workOrderId), context);
        requireStatus(wo, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, "IN_PROCESS");
        if (completedQty == null || completedQty.signum() < 0) {
            completedQty = BigDecimal.ZERO;
        }
        BigDecimal planned = nz(wo.getPlannedQuantity());
        BigDecimal newCompleted = nz(wo.getCompletedQuantity()).add(completedQty);
        if (planned.signum() > 0 && newCompleted.compareTo(planned) > 0) {
            throw new NopException(ErpMfgErrors.ERR_OVER_REPORT)
                    .param(ErpMfgErrors.ARG_COMPLETED_QTY, newCompleted)
                    .param(ErpMfgErrors.ARG_PLANNED_QTY, planned);
        }

        boolean willFinish = planned.signum() > 0 && newCompleted.compareTo(planned) >= 0;
        if (willFinish && isInspectionGated(wo)) {
            throw new NopException(ErpMfgErrors.ERR_INSPECTION_REQUIRED)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode());
        }

        if (willFinish && wo.getProductId() != null) {
            int gate = InspectionTrigger.enforceGate(inspectionBiz, ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER,
                    wo.getCode(), wo.getProductId(), _ErpQaDaoConstants.INSPECTION_TYPE_FINAL,
                    newCompleted, null, null, null, context);
            if (gate == InspectionTrigger.BLOCKED) {
                throw new NopException(ErpMfgErrors.ERR_INSPECTION_REQUIRED)
                        .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode());
            }
        }

        wo.setCompletedQuantity(newCompleted);
        recomputeTotals(wo);

        generateCompletionMove(wo, completedQty, context);

        if (willFinish) {
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED);
            wo.setActualEndDate(CoreMetrics.today());
        }
        workOrderDao().updateEntity(wo);
        return wo;
    }

    // ---------- step：审批迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getApproveStatus();
        if (status == null) {
            status = ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED;
        }
        if (!Objects.equals(status, ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED)
                && !Objects.equals(status, ErpMfgConstants.APPROVE_STATUS_REJECTED)) {
            throw illegalTransition(wo, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpMfgConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(wo, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForApprove(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpMfgConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(wo, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReject(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpMfgConstants.APPROVE_STATUS_SUBMITTED)) {
            throw illegalTransition(wo, status, "SUBMITTED");
        }
    }

    protected void validateTransitionForReverseApprove(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getApproveStatus();
        if (status == null || !Objects.equals(status, ErpMfgConstants.APPROVE_STATUS_APPROVED)) {
            throw illegalTransition(wo, status, "APPROVED");
        }
    }

    // ---------- step：审批业务规则校验 ----------

    protected void validateBusinessRulesForSubmit(ErpMfgWorkOrder wo, IServiceContext context) {
        requireStatus(wo, ErpMfgConstants.WORK_ORDER_STATUS_DRAFT, "DRAFT");
    }

    protected void validateBusinessRulesForApprove(ErpMfgWorkOrder wo, IServiceContext context) {
        requireStatus(wo, ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED, "SUBMITTED");
    }

    // ---------- step：审批执行 ----------

    protected void doSubmit(ErpMfgWorkOrder wo, IServiceContext context) {
        wo.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_SUBMITTED);
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED);
        workOrderDao().updateEntity(wo);
    }

    protected void doWithdrawSubmit(ErpMfgWorkOrder wo, IServiceContext context) {
        wo.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_UNSUBMITTED);
        workOrderDao().updateEntity(wo);
    }

    protected void doApprove(ErpMfgWorkOrder wo, IServiceContext context) {
        wo.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_APPROVED);
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);
        wo.setApprovedBy(currentUserId());
        wo.setApprovedAt(CoreMetrics.currentDateTime());
        workOrderDao().updateEntity(wo);
    }

    protected void doReject(ErpMfgWorkOrder wo, IServiceContext context) {
        wo.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_REJECTED);
        workOrderDao().updateEntity(wo);
    }

    protected void doReverseApprove(ErpMfgWorkOrder wo, IServiceContext context) {
        wo.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_REJECTED);
        wo.setApprovedBy(null);
        wo.setApprovedAt(null);
        workOrderDao().updateEntity(wo);
    }

    // ---------- step：工单操作迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForStart(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getDocStatus();
        if (status != null && Objects.equals(status, ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED)) {
        } else if (status != null && Objects.equals(status, ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL)) {
            if (!isAllowPartialKitStart()) {
                throw new NopException(ErpMfgErrors.ERR_PARTIAL_KIT_START_FORBIDDEN)
                        .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode());
            }
        } else {
            throw illegalTransition(wo, status, "STOCK_RESERVED 或 STOCK_PARTIAL");
        }
    }

    // ---------- step：工单操作执行 ----------

    protected void doStart(ErpMfgWorkOrder wo, IServiceContext context) {
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
        if (wo.getActualStartDate() == null) {
            wo.setActualStartDate(CoreMetrics.today());
        }
        workOrderDao().updateEntity(wo);
    }

    protected void generateCompletionMove(ErpMfgWorkOrder wo, BigDecimal completedQty, IServiceContext context) {
        if (completedQty == null || completedQty.signum() <= 0) {
            return;
        }
        ErpMfgWorkOrderLine outputLine = findOutputLine(wo.getId());
        Long destWarehouseId = outputLine != null ? outputLine.getDestWarehouseId() : null;
        if (destWarehouseId == null) {
            return;
        }
        Long productId = wo.getProductId();
        Long uomId = outputLine != null ? outputLine.getUoMId() : null;
        if (uomId == null && productId != null) {
            ErpMdMaterial product = daoProvider.daoFor(ErpMdMaterial.class).getEntityById(productId);
            uomId = product != null ? product.getUoMId() : null;
        }
        if (uomId == null) {
            return;
        }
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpMfgConstants.MOVE_TYPE_MANUFACTURING);
        request.setOrgId(wo.getOrgId());
        request.setBusinessDate(wo.getBusinessDate() != null ? wo.getBusinessDate() : CoreMetrics.today());
        request.setDestWarehouseId(destWarehouseId);
        request.setCurrencyId(wo.getCurrencyId());
        request.setRelatedBillType(ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER);
        request.setRelatedBillCode(wo.getCode());
        StockMoveLineRequest line = new StockMoveLineRequest();
        line.setMaterialId(productId);
        line.setUoMId(uomId);
        line.setQuantity(completedQty);
        line.setUnitCost(nz(wo.getUnitCost()));
        line.setCurrencyId(wo.getCurrencyId());
        List<StockMoveLineRequest> lines = new ArrayList<>();
        lines.add(line);
        request.setLines(lines);
        stockMoveBiz.generateMove(request, context);
    }

    protected boolean isInspectionGated(ErpMfgWorkOrder wo) {
        if (!isInspectionGateEnabled()) {
            return false;
        }
        if (wo.getBomId() == null) {
            return false;
        }
        ErpMfgBom bom = daoProvider.daoFor(ErpMfgBom.class).getEntityById(wo.getBomId());
        return bom != null && Boolean.TRUE.equals(bom.getInspectionRequired());
    }

    static void recomputeTotals(ErpMfgWorkOrder wo) {
        BigDecimal total = nz(wo.getMaterialCost()).add(nz(wo.getLaborCost()))
                .add(nz(wo.getOverheadCost())).add(nz(wo.getSubcontractCost()));
        wo.setTotalCost(total);
        BigDecimal completed = nz(wo.getCompletedQuantity());
        wo.setUnitCost(completed.signum() != 0 ? total.divide(completed, 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
    }

    // ---------- 校验/查询辅助（protected，供派生复用与覆盖） ----------

    protected ErpMfgWorkOrder requireWorkOrder(String id, IServiceContext context) {
        ErpMfgWorkOrder wo = workOrderDao().getEntityById(id);
        if (wo == null) {
            throw new NopException(ErpMfgErrors.ERR_WORK_ORDER_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, id);
        }
        return wo;
    }

    protected void requireStatus(ErpMfgWorkOrder wo, String expected, String expectedLabel) {
        String current = wo.getDocStatus();
        if (current == null || !Objects.equals(current, expected)) {
            throw illegalTransition(wo, current, expectedLabel);
        }
    }

    protected ErpMfgWorkOrderLine findOutputLine(Long workOrderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workOrderId", workOrderId));
        q.addFilter(eq("lineType", ErpMfgConstants.WORK_ORDER_LINE_TYPE_OUTPUT));
        q.setLimit(1);
        IEntityDao<ErpMfgWorkOrderLine> dao = daoProvider.daoFor(ErpMfgWorkOrderLine.class);
        List<ErpMfgWorkOrderLine> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    protected boolean isAllowPartialKitStart() {
        return readBoolConfig(ErpMfgConstants.CONFIG_ALLOW_PARTIAL_KIT_START, false);
    }

    protected boolean isInspectionGateEnabled() {
        return readBoolConfig(ErpMfgConstants.CONFIG_INSPECTION_GATE_ENABLED, false);
    }

    protected boolean readBoolConfig(String key, boolean defaultValue) {
        try {
            String value = AppConfig.var(key, String.valueOf(defaultValue));
            if (value == null || value.trim().isEmpty()) {
                return defaultValue;
            }
            return Boolean.parseBoolean(value.trim());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    // ---------- misc helpers ----------

    protected IEntityDao<ErpMfgWorkOrder> workOrderDao() {
        return daoProvider.daoFor(ErpMfgWorkOrder.class);
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    protected String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            if (ctx == null) {
                return null;
            }
            return ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    protected NopException illegalTransition(ErpMfgWorkOrder wo, String current, String expected) {
        return new NopException(ErpMfgErrors.ERR_INVALID_STATUS_TRANSITION)
                .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode())
                .param(ErpMfgErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMfgErrors.ARG_EXPECTED_STATUS, expected);
    }
}
