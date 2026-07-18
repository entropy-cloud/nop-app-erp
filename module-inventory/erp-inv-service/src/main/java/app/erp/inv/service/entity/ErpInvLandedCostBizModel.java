
package app.erp.inv.service.entity;

import app.erp.inv.biz.IErpInvLandedCostBiz;
import app.erp.inv.dao.entity.ErpInvLandedCost;
import app.erp.inv.service.processor.ErpInvLandedCostProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * 到岸成本单 BizModel（Facade）。域动作 {@code approve}（审核编排：分摊 → 成本层更新 → 过账）
 * 和 {@code allocate}（分摊预览只读 query）委托 {@link ErpInvLandedCostProcessor}。
 * {@code generateFreightLandedCost}（path-2 运费自动创建 DRAFT）同样委托 Processor。
 *
 * <p>权威：{@code docs/design/finance/costing-methods.md §到岸成本}、
 * {@code docs/plans/2026-07-10-1100-3-landed-cost-allocation.md}、
 * {@code docs/plans/2026-07-11-2329-1-logistics-path2-landed-cost-orchestration.md}。
 */
@BizModel("ErpInvLandedCost")
public class ErpInvLandedCostBizModel extends CrudBizModel<ErpInvLandedCost> implements IErpInvLandedCostBiz {

    @Inject
    ErpInvLandedCostProcessor landedCostProcessor;

    public ErpInvLandedCostBizModel() {
        setEntityName(ErpInvLandedCost.class.getName());
    }

    @Override
    @BizMutation
    public ErpInvLandedCost approve(@Name("id") Long id, IServiceContext context) {
        return landedCostProcessor.approve(id, context);
    }

    @Override
    @BizQuery
    public List<Map<String, Object>> allocate(@Name("id") Long id, IServiceContext context) {
        return landedCostProcessor.allocatePreview(id, context);
    }

    @Override
    @BizMutation
    public ErpInvLandedCost generateFreightLandedCost(@Name("receiveCode") String receiveCode,
                                                       @Name("freightAmount") BigDecimal freightAmount,
                                                       @Name("freightCurrencyId") Long freightCurrencyId,
                                                       @Name("freightExchangeRate") BigDecimal freightExchangeRate,
                                                       IServiceContext context) {
        return landedCostProcessor.generateFreightLandedCost(receiveCode, freightAmount,
                freightCurrencyId, freightExchangeRate, context);
    }

    @Override
    @BizMutation
    public ErpInvLandedCost reverseApprove(@Name("id") Long id, IServiceContext context) {
        return landedCostProcessor.reverseApprove(id, context);
    }
}
