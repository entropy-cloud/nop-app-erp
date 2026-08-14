package app.erp.mfg.service.processor;

import app.erp.inv.biz.IErpInvStockMoveBiz;
import app.erp.inv.biz.StockMoveLineRequest;
import app.erp.inv.biz.StockMoveRequest;
import app.erp.mfg.dao.entity.ErpMfgBom;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.statemachine.ErpMfgWorkOrderApprovalStateMachine;
import app.erp.mfg.service.statemachine.ErpMfgWorkOrderDocumentStateMachine;
import app.erp.mfg.service.costing.ProductionVarianceCalculator;
import app.erp.mfg.service.genealogy.BatchGenealogyWriter;
import app.erp.mfg.service.posting.ProductionVarianceDispatcher;
import app.erp.mfg.service.workorder.KitAvailabilityChecker;
import app.erp.mfg.service.workorder.KitAvailabilityResult;
import app.erp.common.service.ErpCommonErrors;
import app.erp.common.service.SoDGuard;
import app.erp.md.dao.AcctSchemaResolver;
import app.erp.md.dao.entity.ErpMdMaterial;
import app.erp.notify.biz.IErpSysNotificationBiz;
import app.erp.qa.biz.IErpQaInspectionBiz;
import app.erp.qa.biz.InspectionTrigger;
import app.erp.qa.dao.constants.ErpQaInspectionType;
import io.nop.api.core.auth.IUserContext;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.api.core.config.AppConfig;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import io.nop.core.context.ServiceContextImpl;
import io.nop.dao.api.IDaoProvider;
import io.nop.dao.api.IEntityDao;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    private static final Logger LOG = LoggerFactory.getLogger(ErpMfgWorkOrderProcessor.class);

    @Inject
    IDaoProvider daoProvider;
    @Inject
    KitAvailabilityChecker kitAvailabilityChecker;
    @Inject
    IErpInvStockMoveBiz stockMoveBiz;
    @Inject
    IErpQaInspectionBiz inspectionBiz;
    @Inject
    ProductionVarianceCalculator productionVarianceCalculator;
    @Inject
    ProductionVarianceDispatcher productionVarianceDispatcher;
    @Inject
    BatchGenealogyWriter batchGenealogyWriter;
    @Inject
    IErpSysNotificationBiz notificationBiz;
    @Inject
    ErpMfgWorkOrderSubmitForApprovalProcessor submitForApprovalProcessor;
    @Inject
    ErpMfgWorkOrderApproveProcessor approveProcessor;
    @Inject
    ErpMfgWorkOrderRejectProcessor rejectProcessor;
    @Inject
    ErpMfgWorkOrderReverseApproveProcessor reverseApproveProcessor;
    @Inject
    ErpMfgWorkOrderWithdrawApprovalProcessor withdrawApprovalProcessor;
    @Inject
    ErpMfgWorkOrderApprovalStateMachine approvalStateMachine;
    @Inject
    ErpMfgWorkOrderDocumentStateMachine documentStateMachine;

    static final String NOTIFY_EVENT_VARIANCE_FAILURE = "mfg.production-variance-posting-failure";

    public ErpMfgWorkOrder submitForApproval(String id, IServiceContext context) {
        return submitForApprovalProcessor.submitForApproval(id, context);
    }

    public ErpMfgWorkOrder withdrawApproval(String id, IServiceContext context) {
        return withdrawApprovalProcessor.withdrawApproval(id, context);
    }

    public ErpMfgWorkOrder approve(String id, IServiceContext context) {
        return approveProcessor.approve(id, context);
    }

    public ErpMfgWorkOrder reject(String id, IServiceContext context) {
        return rejectProcessor.reject(id, context);
    }

    public ErpMfgWorkOrder reverseApprove(String id, IServiceContext context) {
        return reverseApproveProcessor.reverseApprove(id, context);
    }

    public ErpMfgWorkOrder checkAvailability(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(String.valueOf(workOrderId), context);
        validateTransitionForCheckAvailability(wo, context);
        KitAvailabilityResult result = kitAvailabilityChecker.check(workOrderId);
        wo.setDocStatus(result.getResultingStatus());
        workOrderDao().updateEntity(wo);
        return wo;
    }

    // ---------- 工单操作（R6.2 per-mutation 拆分：start/stop/resume/close/reportCompletion 已迁入 ----------
    // ----------   ErpMfgWorkOrder<Method>Processor，本类保留 :45 checkAvailability + :46 cancel + protected helper） ----------

    /**
     * 取消工单（:46 单步状态翻转豁免：require + 状态守卫 + setStatus + updateEntity，零副作用）。
     * R6.2 登记豁免保留 facade，BizModel 继续委托本方法。
     */
    public ErpMfgWorkOrder cancel(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = requireWorkOrder(String.valueOf(workOrderId), context);
        validateTransitionForCancel(wo, context);
        wo.setDocStatus(documentStateMachine.cancelTargetStatus());
        workOrderDao().updateEntity(wo);
        return wo;
    }

    /** 判定是否为「无 FIRMED 标准成本」容错跳过场景（差异未配置，非故障）。 */
    protected boolean isNoStandardCostError(Throwable e) {
        if (e instanceof NopException) {
            String code = ((NopException) e).getErrorCode();
            return code != null && code.contains("VARIANCE_NO_STANDARD_COST");
        }
        return false;
    }

    /** 生产差异计算/过账失败告警派发（G3；通知失败降级不阻断主流程）。 */
    protected void dispatchVarianceFailureAlert(ErpMfgWorkOrder wo, Exception cause) {
        if (notificationBiz == null) {
            return;
        }
        Map<String, Object> ctx = new LinkedHashMap<>();
        ctx.put("workOrderId", wo.getId());
        ctx.put("workOrderCode", wo.getCode());
        ctx.put("errorCode", cause instanceof NopException ? ((NopException) cause).getErrorCode() : cause.getClass().getName());
        ctx.put("errorMessage", cause.getMessage());
        ctx.put("postingNo", wo.getCode());
        IServiceContext serviceCtx = new ServiceContextImpl();
        try {
            notificationBiz.notify(NOTIFY_EVENT_VARIANCE_FAILURE, ctx, serviceCtx);
        } catch (Exception notifyErr) {
            LOG.warn("生产差异过账失败告警派发失败（降级）：workOrderCode={}, reason={}",
                    wo.getCode(), notifyErr.getMessage());
        }
    }

    // ---------- step：审批迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForSubmit(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getApproveStatus();
        try {
            approvalStateMachine.assertCanSubmit(status);
        } catch (NopException e) {
            throw illegalTransition(wo, status, "UNSUBMITTED 或 REJECTED");
        }
    }

    protected void validateTransitionForWithdraw(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getApproveStatus();
        try {
            approvalStateMachine.assertCanWithdraw(status);
        } catch (NopException e) {
            throw illegalTransition(wo, status, ErpMfgConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForApprove(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getApproveStatus();
        try {
            approvalStateMachine.assertCanApprove(status);
        } catch (NopException e) {
            throw illegalTransition(wo, status, ErpMfgConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReject(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getApproveStatus();
        try {
            approvalStateMachine.assertCanReject(status);
        } catch (NopException e) {
            throw illegalTransition(wo, status, ErpMfgConstants.APPROVE_STATUS_SUBMITTED);
        }
    }

    protected void validateTransitionForReverseApprove(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getApproveStatus();
        try {
            approvalStateMachine.assertCanReverseApprove(status);
        } catch (NopException e) {
            throw illegalTransition(wo, status, ErpMfgConstants.APPROVE_STATUS_APPROVED);
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
        wo.setApproveStatus(approvalStateMachine.submitTargetStatus());
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED);
        workOrderDao().updateEntity(wo);
    }

    protected void doWithdrawSubmit(ErpMfgWorkOrder wo, IServiceContext context) {
        wo.setApproveStatus(approvalStateMachine.withdrawTargetStatus());
        workOrderDao().updateEntity(wo);
    }

    protected void doApprove(ErpMfgWorkOrder wo, IServiceContext context) {
        SoDGuard.assertApproverNotCreator(wo.getCreatedBy(), currentUserId(), ErpMfgErrors.ERR_MFG_APPROVER_IS_CREATOR);
        wo.setApproveStatus(approvalStateMachine.approveTargetStatus());
        wo.setDocStatus(ErpMfgConstants.WORK_ORDER_STATUS_NOT_STARTED);
        wo.setApprovedBy(currentUserId());
        wo.setApprovedAt(CoreMetrics.currentTimestamp());
        workOrderDao().updateEntity(wo);
    }

    protected void doReject(ErpMfgWorkOrder wo, IServiceContext context) {
        wo.setApproveStatus(approvalStateMachine.rejectTargetStatus());
        workOrderDao().updateEntity(wo);
    }

    protected void doReverseApprove(ErpMfgWorkOrder wo, IServiceContext context) {
        wo.setApproveStatus(approvalStateMachine.reverseApproveTargetStatus());
        wo.setApprovedBy(null);
        wo.setApprovedAt(null);
        workOrderDao().updateEntity(wo);
    }

    // ---------- step：工单操作迁移校验（protected，下游可逐个覆盖） ----------

    protected void validateTransitionForStart(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getDocStatus();
        try {
            documentStateMachine.assertCanStart(status);
        } catch (NopException e) {
            throw illegalTransition(wo, status, "STOCK_RESERVED 或 STOCK_PARTIAL");
        }
        // 动态业务守卫（config-gated 部分齐套开工许可）保留原位
        if (ErpMfgConstants.WORK_ORDER_STATUS_STOCK_PARTIAL.equals(status) && !isAllowPartialKitStart()) {
            throw new NopException(ErpMfgErrors.ERR_PARTIAL_KIT_START_FORBIDDEN)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode());
        }
    }

    /** stop 守卫：仅 IN_PROCESS 合法（固定来源态委托 Document Bean）。 */
    protected void validateTransitionForStop(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getDocStatus();
        try {
            documentStateMachine.assertCanStop(status);
        } catch (NopException e) {
            throw illegalTransition(wo, status, "IN_PROCESS");
        }
    }

    /** resume 守卫：仅 STOPPED 合法（固定来源态委托 Document Bean）。 */
    protected void validateTransitionForResume(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getDocStatus();
        try {
            documentStateMachine.assertCanResume(status);
        } catch (NopException e) {
            throw illegalTransition(wo, status, "STOPPED");
        }
    }

    /** reportCompletion 守卫：仅 IN_PROCESS 合法（固定来源态委托 Document Bean）。 */
    protected void validateTransitionForReportCompletion(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getDocStatus();
        try {
            documentStateMachine.assertCanReportCompletion(status);
        } catch (NopException e) {
            throw illegalTransition(wo, status, "IN_PROCESS");
        }
    }

    /** checkAvailability 守卫：仅 NOT_STARTED 合法（固定来源态委托 Document Bean）。 */
    protected void validateTransitionForCheckAvailability(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getDocStatus();
        try {
            documentStateMachine.assertCanCheckAvailability(status);
        } catch (NopException e) {
            throw illegalTransition(wo, status, "NOT_STARTED");
        }
    }

    /** cancel 守卫：来源态 {DRAFT, SUBMITTED, NOT_STARTED}（固定来源态委托 Document Bean）。 */
    protected void validateTransitionForCancel(ErpMfgWorkOrder wo, IServiceContext context) {
        String status = wo.getDocStatus();
        try {
            documentStateMachine.assertCanCancel(status);
        } catch (NopException e) {
            throw illegalTransition(wo, status, "DRAFT、SUBMITTED 或 NOT_STARTED");
        }
    }

    // ---------- step：工单操作执行 ----------

    protected void doStart(ErpMfgWorkOrder wo, IServiceContext context) {
        wo.setDocStatus(documentStateMachine.startTargetStatus());
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
        request.setAcctSchemaId(resolveAcctSchemaId(wo.getOrgId()));
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

    /**
     * 完工入库后写入生产批次基因链（inputLot→outputLot 消耗行）。
     *
     * <p>plan 2026-07-07-0305-3 §Phase 1。best-effort（{@link BatchGenealogyWriter#writeOnCompletion}
     * 内部 try/catch，不阻断完工入库）；config-gated {@code erp-mfg.genealogy-write-enabled}。
     * 作为 protected step，下游派生 Processor 可覆盖以跳过或增强。
     */
    protected void writeBatchGenealogy(ErpMfgWorkOrder wo, BigDecimal completedQty, IServiceContext context) {
        batchGenealogyWriter.writeOnCompletion(wo, completedQty, context);
    }

    protected boolean isInspectionGated(ErpMfgWorkOrder wo) {
        if (!isInspectionGateEnabled()) {
            return false;
        }
        if (wo.getBomId() == null) {
            return false;
        }
        ErpMfgBom bom = wo.getBom();
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
        try {
            if (ErpMfgConstants.WORK_ORDER_STATUS_DRAFT.equals(expected)) {
                // submit（docStatus 侧）固定守卫：仅 DRAFT
                documentStateMachine.assertCanSubmit(current);
            } else if (ErpMfgConstants.WORK_ORDER_STATUS_SUBMITTED.equals(expected)) {
                // approve（docStatus 侧）固定守卫：仅 SUBMITTED
                documentStateMachine.assertCanApprove(current);
            } else {
                throw new NopException(ErpCommonErrors.ERR_ILLEGAL_STATUS_TRANSITION)
                        .param(ErpCommonErrors.ARG_CURRENT_STATUS, current)
                        .param(ErpCommonErrors.ARG_EXPECTED_STATUS, expectedLabel);
            }
        } catch (NopException e) {
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

    /**
     * 解析工单所属组织的会计账套 ID（同 {@code ProductionVarianceDispatcher.resolveAcctSchemaId} 范式）。
     * 完工入库 GL 过账要求凭证行 acctSchemaId 非空，故 generateCompletionMove 需传入。
     */
    protected Long resolveAcctSchemaId(Long orgId) {
        return AcctSchemaResolver.resolvePrimarySchemaId(daoProvider, orgId);
    }

    protected boolean isAllowPartialKitStart() {
        return readBoolConfig(ErpMfgConstants.CONFIG_ALLOW_PARTIAL_KIT_START, false);
    }

    protected boolean isInspectionGateEnabled() {
        return readBoolConfig(ErpMfgConstants.CONFIG_INSPECTION_GATE_ENABLED, false);
    }

    /**
     * 生产差异完工自动触发开关（plan 2026-07-05-1838-2）。默认关：完工不自动算差异，需手动入口重算。
     */
    protected boolean isVarianceAutoCalcEnabled() {
        return readBoolConfig(ErpMfgConstants.CONFIG_VARIANCE_AUTO_CALC_ENABLED, false);
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
