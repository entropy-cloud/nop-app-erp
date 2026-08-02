package app.erp.mfg.service.processor;

import app.erp.mfg.dao.entity.ErpMfgCostVariance;
import app.erp.mfg.dao.entity.ErpMfgWorkOrder;
import app.erp.mfg.service.ErpMfgConstants;
import app.erp.mfg.service.ErpMfgErrors;
import app.erp.mfg.service.costing.ProductionVarianceCalculator;
import app.erp.mfg.service.posting.ProductionVarianceDispatcher;
import io.nop.api.core.exceptions.NopException;
import io.nop.core.context.IServiceContext;
import io.nop.dao.api.IDaoProvider;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Objects;

/**
 * ErpMfgCostVariance calculateVariances per-mutation Processor（R6.2，{@code processor-extension-pattern.md} 每 mutation 一 Processor）。
 * 自包含生产差异重算编排（工单完工校验 → 红冲既有凭证 → 删差异旧行 → 重算 → 派发新凭证）；从 ErpMfgCostVarianceBizModel
 * 内联 @BizMutation 提取。会计保护区域（差异过账）语义不变。
 *
 * <p>权威：{@code docs/design/manufacturing/variance-analysis.md}。
 */
public class ErpMfgCostVarianceCalculateVariancesProcessor {

    @Inject
    IDaoProvider daoProvider;
    @Inject
    ProductionVarianceCalculator productionVarianceCalculator;
    @Inject
    ProductionVarianceDispatcher productionVarianceDispatcher;

    public List<ErpMfgCostVariance> calculateVariances(Long workOrderId, IServiceContext context) {
        ErpMfgWorkOrder wo = daoProvider.daoFor(ErpMfgWorkOrder.class).getEntityById(workOrderId);
        if (wo == null) {
            throw new NopException(ErpMfgErrors.ERR_WORK_ORDER_NOT_FOUND)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_ID, workOrderId);
        }
        if (!Objects.equals(wo.getDocStatus(), ErpMfgConstants.WORK_ORDER_STATUS_COMPLETED)) {
            throw new NopException(ErpMfgErrors.ERR_VARIANCE_WORKORDER_NOT_COMPLETED)
                    .param(ErpMfgErrors.ARG_WORK_ORDER_CODE, wo.getCode())
                    .param(ErpMfgErrors.ARG_CURRENT_STATUS, wo.getDocStatus());
        }
        // 重算幂等闭环（plan 2026-07-18-2251-1）：先红冲既有 PRODUCTION_VARIANCE 凭证 → 再删差异旧行 → 再重算 → 再派发新凭证。
        productionVarianceDispatcher.reverseIfExists(workOrderId);
        // 幂等：先删该工单全部差异旧行，再重算
        productionVarianceCalculator.deleteByWorkOrder(workOrderId);
        List<ErpMfgCostVariance> lines = productionVarianceCalculator.calculateVariances(workOrderId);
        // 差异过账（承接 PPV 范式，失败隔离吞异常保持 posted=false）
        productionVarianceDispatcher.dispatchIfApplicable(workOrderId);
        return lines;
    }
}
