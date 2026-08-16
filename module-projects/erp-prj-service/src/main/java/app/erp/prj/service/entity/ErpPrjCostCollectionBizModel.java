package app.erp.prj.service.entity;

import app.erp.prj.biz.IErpPrjCostCollectionBiz;
import app.erp.prj.dao.entity.ErpPrjCostCollection;
import app.erp.prj.service.processor.ErpPrjCostCollectionAggregateMaterialCostProcessor;
import app.erp.prj.service.processor.ErpPrjCostCollectionRefreshExpenseCostProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.math.BigDecimal;

/**
 * 项目成本归集 BizModel。CRUD 之上承载费用报销归集接入（projects 驱动只读聚合）+ 物料归集接入
 * （purchase 侧入库审核触发，RC-R1.61）。
 *
 * <p>{@link #refreshExpenseCost(Long, IServiceContext)} 受 {@code erp-prj.expense-aggregation-enabled}
 * （默认 true）config-gated。关闭时直接返回 0（{@code closeProject} 也据此跳过费用刷新）。
 * {@link #aggregateMaterialCost(Long, BigDecimal, String, IServiceContext)} 受
 * {@code erp-prj.material-aggregation-enabled}（默认 true）config-gated，关闭时返回 0。
 *
 * <p>R6.6：{@code refreshExpenseCost} / {@code aggregateMaterialCost} 均拆为独立 per-mutation Processor
 * （{@code processor-extension-pattern.md}），本类仅作 facade 单行委托。
 */
@BizModel("ErpPrjCostCollection")
public class ErpPrjCostCollectionBizModel extends CrudBizModel<ErpPrjCostCollection>
        implements IErpPrjCostCollectionBiz {

    @Inject
    ErpPrjCostCollectionRefreshExpenseCostProcessor refreshExpenseCostProcessor;
    @Inject
    ErpPrjCostCollectionAggregateMaterialCostProcessor aggregateMaterialCostProcessor;

    public ErpPrjCostCollectionBizModel() {
        setEntityName(ErpPrjCostCollection.class.getName());
    }

    @Override
    @BizMutation
    public BigDecimal refreshExpenseCost(@Name("projectId") Long projectId, IServiceContext context) {
        return refreshExpenseCostProcessor.refreshExpenseCost(projectId, context);
    }

    @Override
    @BizMutation
    public BigDecimal aggregateMaterialCost(@Name("projectId") Long projectId,
                                            @Name("amount") BigDecimal amount,
                                            @Name("sourceBillCode") String sourceBillCode,
                                            IServiceContext context) {
        return aggregateMaterialCostProcessor.aggregateMaterialCost(projectId, amount, sourceBillCode, context);
    }

}
