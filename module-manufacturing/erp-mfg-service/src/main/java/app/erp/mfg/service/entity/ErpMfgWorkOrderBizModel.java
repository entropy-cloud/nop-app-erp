
package app.erp.mfg.service.entity;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.inv.dao.entity.ErpInvStockMove;
import app.erp.mfg.biz.IErpMfgWorkOrderBiz;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.workorder.KitAvailabilityChecker;
import app.erp.mfg.service.workorder.KitAvailabilityResult;
import app.erp.qa.biz.IErpQaInspectionBiz;
import app.erp.qa.biz.InspectionTrigger;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * 工单 BizModel。在 {@link CrudBizModel} 标准 CRUD 之上实现工单 10 态状态机
 * （{@code docs/design/manufacturing/state-machine.md §适用对象一`}）+ 三轴审批（提交→审核→NOT_STARTED）
 * + 齐套校验（STOCK_RESERVED / STOCK_PARTIAL）。
 *
 * <p>齐套校验经 {@link KitAvailabilityChecker}（BOM 展开 × plannedQuantity 对照 inventory 余额可用量）。
 *
 * <p>状态机迁移校验前置 {@code docStatus}，违反抛 {@link NopException}（{@link ErpMfgErrors#ERR_INVALID_STATUS_TRANSITION}）。
 * 部分齐套强制开工受 {@code erp-mfg.allow-partial-kit-start} 控制（默认 false）。
 */
@BizModel("ErpMfgWorkOrder")
public class ErpMfgWorkOrderBizModel extends CrudBizModel<ErpMfgWorkOrder> implements IErpMfgWorkOrderBiz {

    @Inject
    KitAvailabilityChecker kitAvailabilityChecker;
    @Inject
    IErpInvStockMoveBiz stockMoveBiz;
    @Inject
    IErpQaInspectionBiz inspectionBiz;

    public ErpMfgWorkOrderBizModel() {
        setEntityName(ErpMfgWorkOrder.class.getName());
    }

    public void setKitAvailabilityChecker(KitAvailabilityChecker kitAvailabilityChecker) {
        this.kitAvailabilityChecker = kitAvailabilityChecker;
    }

    public void setStockMoveBiz(IErpInvStockMoveBiz stockMoveBiz) {
        this.stockMoveBiz = stockMoveBiz;
    }

    public void setInspectionBiz(IErpQaInspectionBiz inspectionBiz) {
        this.inspectionBiz = inspectionBiz;
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder submit(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(workOrderId, context);
        requireStatus(wo, ErpMfgConstants.WORK_ORDER_STATUS_DRAFT, "DRAFT");
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED);
        wo.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_SUBMITTED);
        dao().updateEntity(wo);
        return wo;
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder approve(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(workOrderId, context);
        requireStatus(wo, ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED, "SUBMITTED");
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);
        wo.setApproveStatus(ErpMfgConstants.APPROVE_STATUS_APPROVED);
        dao().updateEntity(wo);
        return wo;
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder checkAvailability(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(workOrderId, context);
        requireStatus(wo, ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED, "NOT_STARTED");
        KitAvailabilityResult result = kitAvailabilityChecker.check(workOrderId);
        wo.setDocStatus(result.getResultingStatus());
        dao().updateEntity(wo);
        return wo;
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder start(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(workOrderId, context);
        Integer status = wo.getDocStatus();
        if (status != null && status == ErpMfgConstants.WORK_ORDER_STATUS_STOCK_RESERVED) {
            // 全齐套：直接开工
        } else if (status != null && status == ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL) {
            // 部分齐套：须配置允许强制开工
            if (!isAllowPartialKitStart()) {
                throw new NopException(ErpMfgErrors.ERR_PARTIAL_KIT_START_FORBIDDEN)
                        .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode());
            }
        } else {
            throw illegalTransition(wo, status, "STOCK_RESERVED 或 STOCK_PARTIAL");
        }
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
        if (wo.getActualStartDate() == null) {
            wo.setActualStartDate(LocalDate.now());
        }
        dao().updateEntity(wo);
        return wo;
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder stop(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(workOrderId, context);
        requireStatus(wo, ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS, "IN_PROCESS");
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_STOPPED);
        dao().updateEntity(wo);
        return wo;
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder resume(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(workOrderId, context);
        requireStatus(wo, ErpMfgConstants.WORK_ORDER_STATUS_STOPPED, "STOPPED");
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS);
        dao().updateEntity(wo);
        return wo;
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder close(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(workOrderId, context);
        Integer status = wo.getDocStatus();
        if (status == null || (status != ErpMfgConstants.WORK_ORDER_STATUS_STOPPED
                && status != ErpMfgConstants.WORK_ORDER_STATUS_IN_PROCESS)) {
            throw illegalTransition(wo, status, "STOPPED 或 IN_PROCESS");
        }
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_CLOSED);
        if (wo.getActualEndDate() == null) {
            wo.setActualEndDate(LocalDate.now());
        }
        dao().updateEntity(wo);
        return wo;
    }

    @Override
    @BizMutation
    public ErpMfgWorkOrder cancel(@Name("workOrderId") Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(workOrderId, context);
        Integer status = wo.getDocStatus();
        // 仅未开工前可取消（DRAFT/SUBMITTED/NOT_STARTED）。STOCK_RESERVED/STOCK_PARTIAL 属 NOT_STARTED 后续态，
        // 依 state-machine.md §迁移完整性「NOT_STARTED/SUBMITTED→CANCELLED」从严只允许前三态。
        if (status == null || (status != ErpMfgConstants.WORK_ORDER_STATUS_DRAFT
                && status != ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED
                && status != ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED)) {
            throw illegalTransition(wo, status, "DRAFT、SUBMITTED 或 NOT_STARTED");
        }
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_CANCELLED);
        dao().updateEntity(wo);
        return wo;
    }

    /**
     * 完工入库：累加完工数量、生成产成品入库移动单（MANUFACTURING，库存域视为入库 → 加产成品库存）、
     * 重算成本（totalCost = material+labor+overhead+subcontract；unitCost = total/completed），完工达量→COMPLETED。
     *
     * <p>完工质检 config-gated 钩子：若 BOM.inspectionRequired=true 且 {@code erp-mfg.inspection-gate-enabled=true}，
     * 且本次完工将达计划量（newCompleted ≥ planned），则拒绝（抛 {@link ErpMfgErrors#ERR_INSPECTION_REQUIRED}），
     * 工单保持 IN_PROCESS 待质检结果（2.4 quality 落地后接线）。gate 默认 false（质检软依赖，本期跳过）。
     *
     * <p>未启用超产配置时拒绝完工超过计划数量（state-machine §异常路径）。
     */
    @Override
    @BizMutation
    public ErpMfgWorkOrder reportCompletion(@Name("workOrderId") Long workOrderId,
                                            @Name("completedQty") BigDecimal completedQty,
                                            IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(workOrderId, context);
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

        // 完工质检 config-gated 钩子：达量且需质检 → 拒绝 COMPLETED 待质检（不生成入库移动单，工单保持 IN_PROCESS）
        boolean willFinish = planned.signum() > 0 && newCompleted.compareTo(planned) >= 0;
        if (willFinish && isInspectionGated(wo)) {
            throw new NopException(ErpMfgErrors.ERR_INSPECTION_REQUIRED)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode());
        }

        // 强制完工质检门控（plan 2026-07-02-2237-3 Phase 2）：达量时若 ERP_MFG_WORK_ORDER 属强制质检类型，
        // 经 InspectionTrigger 生成 FINAL 质检单并阻塞（首次 PENDING；质检合格/让步后再次报工放行）。默认空=不强制。
        if (willFinish && wo.getProductId() != null) {
            int gate = InspectionTrigger.enforceGate(inspectionBiz, ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER,
                    wo.getCode(), wo.getProductId(), 30 /* erp-qa/inspection-type FINAL */,
                    newCompleted, null, null, null, context);
            if (gate == InspectionTrigger.BLOCKED) {
                throw new NopException(ErpMfgErrors.ERR_INSPECTION_REQUIRED)
                        .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode());
            }
        }

        // 累加完工数量 + 重算成本（unitCost 用累加后成本 / 累加后完工量）
        wo.setCompletedQuantity(newCompleted);
        recomputeTotals(wo);

        // 生成产成品入库移动单（MANUFACTURING 入库方向；unitCost 取重算后的工单单位成本）
        generateCompletionMove(wo, completedQty, context);

        if (willFinish) {
            wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED);
            wo.setActualEndDate(LocalDate.now());
        }
        dao().updateEntity(wo);
        return wo;
    }

    private void generateCompletionMove(ErpMfgWorkOrder wo, BigDecimal completedQty, IServiceContext context) {
        if (completedQty == null || completedQty.signum() <= 0) {
            return;
        }
        ErpMfgWorkOrderLine outputLine = findOutputLine(wo.getId());
        Long destWarehouseId = outputLine != null ? outputLine.getDestWarehouseId() : null;
        if (destWarehouseId == null) {
            // 无目的仓库（未配 OUTPUT 行的入库仓库）则跳过入库移动单生成，仅完成状态迁移；
            // 库存记账需仓库维度，缺仓库无法写流水（避免 mandatory 违规）
            return;
        }
        Long productId = wo.getProductId();
        Long uomId = outputLine != null ? outputLine.getUoMId() : null;
        if (uomId == null && productId != null) {
            // 无 OUTPUT 行时回落到产品物料的计量单位
            app.erp.md.dao.entity.ErpMdMaterial product = daoFor(app.erp.md.dao.entity.ErpMdMaterial.class)
                    .getEntityById(productId);
            uomId = product != null ? product.getUoMId() : null;
        }
        if (uomId == null) {
            return;
        }
        StockMoveRequest request = new StockMoveRequest();
        request.setMoveType(ErpMfgConstants.MOVE_TYPE_MANUFACTURING);
        request.setOrgId(wo.getOrgId());
        request.setBusinessDate(wo.getBusinessDate() != null ? wo.getBusinessDate() : LocalDate.now());
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

    private ErpMfgWorkOrderLine findOutputLine(Long workOrderId) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("workOrderId", workOrderId));
        q.addFilter(eq("lineType", ErpMfgConstants.WORK_ORDER_LINE_TYPE_OUTPUT));
        q.setLimit(1);
        IEntityDao<ErpMfgWorkOrderLine> dao = daoFor(ErpMfgWorkOrderLine.class);
        List<ErpMfgWorkOrderLine> list = dao.findAllByQuery(q);
        return list.isEmpty() ? null : list.get(0);
    }

    private boolean isInspectionGated(ErpMfgWorkOrder wo) {
        if (!isInspectionGateEnabled()) {
            return false;
        }
        if (wo.getBomId() == null) {
            return false;
        }
        ErpMfgBom bom = daoFor(ErpMfgBom.class).getEntityById(wo.getBomId());
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

    // ---------- helpers ----------

    private ErpMfgWorkOrder requireWorkOrder(Long workOrderId, IServiceContext context) {
        return requireEntity(String.valueOf(workOrderId), null, context);
    }

    private void requireStatus(ErpMfgWorkOrder wo, int expected, String expectedLabel) {
        Integer current = wo.getDocStatus();
        if (current == null || current != expected) {
            throw illegalTransition(wo, current, expectedLabel);
        }
    }

    private boolean isAllowPartialKitStart() {
        return readBoolConfig(ErpMfgConstants.CONFIG_ALLOW_PARTIAL_KIT_START, false);
    }

    private boolean isInspectionGateEnabled() {
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

    private String currentUserId() {
        try {
            IUserContext ctx = IUserContext.get();
            return ctx == null ? null : ctx.getUserId();
        } catch (Exception e) {
            return null;
        }
    }

    static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private NopException illegalTransition(ErpMfgWorkOrder wo, Integer current, String expected) {
        return new NopException(ErpMfgErrors.ERR_INVALID_STATUS_TRANSITION)
                .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode())
                .param(ErpMfgErrors.ARG_CURRENT_STATUS, current)
                .param(ErpMfgErrors.ARG_EXPECTED_STATUS, expected);
    }
}
