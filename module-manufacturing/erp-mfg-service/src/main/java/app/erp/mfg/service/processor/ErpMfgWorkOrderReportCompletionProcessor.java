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

    public ErpMfgWorkOrder reportCompletion(String workOrderId, BigDecimal completedQty, IServiceContext context) {
        ErpMfgWorkOrder wo = facade.requireWorkOrder(workOrderId, context);
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
        // P1-CK-mfg3-005：完工时归集实际委外费（config-gated；置于 recomputeTotals 前使 totalCost/unitCost 含委外费）
        facade.applySubcontractCostToWorkOrder(wo, context);
        ErpMfgWorkOrderProcessor.recomputeTotals(wo);

        // F1.2（P2-CK-mfg-011 报工链）：工单持久化与预留释放前移到 generateCompletionMove（内含
        // REQUIRES_NEW 完工入库凭证）之前——updateEntity/reload 与 releaseRemainingReservations 均可抛，
        // 原顺序下失败会回滚主事务但凭证已独立提交（孤儿凭证）。同主事务前移原子性不变。
        if (willFinish) {
            wo.setDocStatus(facade.documentStateMachine.reportCompletionTargetStatus());
            wo.setActualEndDate(CoreMetrics.today());
        }
        facade.workOrderDao().updateEntity(wo);

        // 完工释放未领料预留（UC-MFG-08 ⑤⑥⑦）：config-gated（auto-release-on-complete + reservation-enabled 联动）。
        // 查无预留 no-op 零写入；不阻断完工主链。
        if (willFinish) {
            facade.releaseRemainingReservations(wo, context);
        }

        facade.generateCompletionMove(wo, completedQty, context);

        // 完工入库成功后写入生产批次基因链（inputLot→outputLot 消耗行）。
        // best-effort（BatchGenealogyWriter 内部 try/catch，不阻断完工入库）；config-gated erp-mfg.genealogy-write-enabled。
        facade.writeBatchGenealogy(wo, completedQty, context);

        // generateCompletionMove 经 cross-BizModel generateMove 调用，其内部 GL 过账用 REQUIRES_NEW 事务，
        // 成功过账后当前 session 实体可能被 evict。差异段/alert 只读消费 wo，重载防 evict 读失败。
        wo = facade.workOrderDao().getEntityById(workOrderId);

        // 完工达量（willFinish）：config-gated 自动触发生产差异计算 + 过账。G3 错误传播分级（posting-log.md）：
        // 「无 FIRMED 标准成本」（ERR_VARIANCE_NO_STANDARD_COST）容错跳过（差异未配置，非故障）；
        // 其他失败（配置错误/真实故障）不阻断完工（已 COMPLETED）但派发 IErpSysNotificationBiz 告警，
        // 使 GL 缺 PRODUCTION_VARIANCE 凭证的悬挂可被运营感知（手动重算入口存在）。
        if (willFinish && facade.isVarianceAutoCalcEnabled()) {
            try {
                // 重算幂等闭环（plan 2026-07-18-2251-1）：先红冲既有 PRODUCTION_VARIANCE 凭证 → 删差异旧行 → 重算 → 派发新凭证。
                // P2-CK-mfg3-009：红冲真实失败时中止派发段（新差异行保持 posted=false，防幂等命中旧凭证误标 posted）。
                boolean reversalOk = facade.productionVarianceDispatcher.reverseIfExists(workOrderId);
                facade.productionVarianceCalculator.deleteByWorkOrder(workOrderId);
                facade.productionVarianceCalculator.calculateVariances(workOrderId);
                if (reversalOk) {
                    facade.productionVarianceDispatcher.dispatchIfApplicable(workOrderId);
                }
            } catch (Exception e) {
                if (facade.isNoStandardCostError(e)) {
                    LOG.warn("Work order {} completion variance calculation skipped (no FIRMED standard cost, not a fault): {}", wo.getCode(), e.getMessage());
                } else {
                    LOG.error("Work order {} completion-triggered production variance calculation/posting failed (non-blocking for completion, recalculate via manual calculateVariances, alert dispatched)",
                            wo.getCode(), e);
                    facade.dispatchVarianceFailureAlert(wo, e);
                }
            }
        }
        return wo;
    }
}
