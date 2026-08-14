package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.qa.biz.InspectionTrigger;
import app.erp.qa.dao.constants.ErpQaInspectionType;
import io.nop.api.core.exceptions.NopException;
import io.nop.api.core.time.CoreMetrics;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

/**
 * ErpMfgWorkOrder reportCompletion per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含完工入库编排（累加完工数量 + 产成品入库移动单 + 成本重算 + 达量 COMPLETED + config-gated 生产差异计算/过账）；
 * 共享 protected helper 单一真相源在 {@link ErpMfgWorkOrderProcessor}。事务边界跟随 Facade {@code @BizMutation} 事务。
 *
 * <p>会计保护区域（{@code docs/design/manufacturing/}）：完工入库业财过账语义不变，仅编排位置迁移。
 */
public class ErpMfgWorkOrderReportCompletionProcessor {

    static final Logger LOG = LoggerFactory.getLogger(ErpMfgWorkOrderReportCompletionProcessor.class);

    @Inject
    ErpMfgWorkOrderProcessor facade;

    public ErpMfgWorkOrder reportCompletion(Long workOrderId, BigDecimal completedQty, IServiceContext context) {
        ErpMfgWorkOrder wo = facade.requireWorkOrder(String.valueOf(workOrderId), context);
        facade.validateTransitionForReportCompletion(wo, context);
        if (completedQty == null || completedQty.signum() < 0) {
            completedQty = BigDecimal.ZERO;
        }
        BigDecimal planned = ErpMfgWorkOrderProcessor.nz(wo.getPlannedQuantity());
        BigDecimal newCompleted = ErpMfgWorkOrderProcessor.nz(wo.getCompletedQuantity()).add(completedQty);
        if (planned.signum() > 0 && newCompleted.compareTo(planned) > 0) {
            throw new NopException(ErpMfgErrors.ERR_OVER_REPORT)
                    .param(ErpMfgErrors.ARG_COMPLETED_QTY, newCompleted)
                    .param(ErpMfgErrors.ARG_PLANNED_QTY, planned);
        }

        boolean willFinish = planned.signum() > 0 && newCompleted.compareTo(planned) >= 0;
        if (willFinish && facade.isInspectionGated(wo)) {
            throw new NopException(ErpMfgErrors.ERR_INSPECTION_REQUIRED)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode());
        }

        if (willFinish && wo.getProductId() != null) {
            int gate = InspectionTrigger.enforceGate(facade.inspectionBiz, ErpMfgConstants.RELATED_BILL_TYPE_MFG_WORK_ORDER,
                    wo.getCode(), wo.getProductId(), ErpQaInspectionType.INSPECTION_TYPE_FINAL,
                    newCompleted, null, null, null, context);
            if (gate == InspectionTrigger.BLOCKED) {
                throw new NopException(ErpMfgErrors.ERR_INSPECTION_REQUIRED)
                        .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode());
            }
        }

        wo.setCompletedQuantity(newCompleted);
        ErpMfgWorkOrderProcessor.recomputeTotals(wo);

        facade.generateCompletionMove(wo, completedQty, context);

        // 完工入库成功后写入生产批次基因链（inputLot→outputLot 消耗行）。
        // best-effort（BatchGenealogyWriter 内部 try/catch，不阻断完工入库）；config-gated erp-mfg.genealogy-write-enabled。
        facade.writeBatchGenealogy(wo, completedQty, context);

        // generateCompletionMove 经 cross-BizModel generateMove 调用，其内部 GL 过账用 REQUIRES_NEW 事务，
        // 成功过账后当前 session 实体可能被 evict。重新加载 wo 并重应用字段，避免 updateEntity 报 save-entity-not-transient。
        wo = facade.workOrderDao().getEntityById(workOrderId);
        wo.setCompletedQuantity(newCompleted);
        ErpMfgWorkOrderProcessor.recomputeTotals(wo);

        if (willFinish) {
            wo.setDocStatus(facade.documentStateMachine.reportCompletionTargetStatus());
            wo.setActualEndDate(CoreMetrics.today());
        }
        facade.workOrderDao().updateEntity(wo);

        // 完工达量（willFinish）：config-gated 自动触发生产差异计算 + 过账。G3 错误传播分级（posting-log.md）：
        // 「无 FIRMED 标准成本」（ERR_VARIANCE_NO_STANDARD_COST）容错跳过（差异未配置，非故障）；
        // 其他失败（配置错误/真实故障）不阻断完工（已 COMPLETED）但派发 IErpSysNotificationBiz 告警，
        // 使 GL 缺 PRODUCTION_VARIANCE 凭证的悬挂可被运营感知（手动重算入口存在）。
        if (willFinish && facade.isVarianceAutoCalcEnabled()) {
            try {
                // 重算幂等闭环（plan 2026-07-18-2251-1）：先红冲既有 PRODUCTION_VARIANCE 凭证 → 删差异旧行 → 重算 → 派发新凭证。
                facade.productionVarianceDispatcher.reverseIfExists(workOrderId);
                facade.productionVarianceCalculator.deleteByWorkOrder(workOrderId);
                facade.productionVarianceCalculator.calculateVariances(workOrderId);
                facade.productionVarianceDispatcher.dispatchIfApplicable(workOrderId);
            } catch (Exception e) {
                if (facade.isNoStandardCostError(e)) {
                    LOG.warn("工单 {} 完工差异计算跳过（无 FIRMED 标准成本，非故障）：{}", wo.getCode(), e.getMessage());
                } else {
                    LOG.error("工单 {} 完工触发生产差异计算/过账失败（不阻断完工，可经手动 calculateVariances 重算，已派发告警）",
                            wo.getCode(), e);
                    facade.dispatchVarianceFailureAlert(wo, e);
                }
            }
        }
        return wo;
    }
}
