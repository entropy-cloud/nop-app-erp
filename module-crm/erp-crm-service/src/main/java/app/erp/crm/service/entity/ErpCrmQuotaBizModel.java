
package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmQuotaBiz;
import app.erp.crm.dao.entity.ErpCrmQuota;
import app.erp.crm.dao.entity.ErpCrmTerritoryPipeline;
import app.erp.crm.service.ErpCrmErrors;
import app.erp.crm.service.support.QuotaRollupCalculator;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.annotations.core.Optional;
import io.nop.api.core.exceptions.NopException;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

/**
 * 销售配额 BizModel。在标准 CRUD 之上扩展：
 * <ul>
 *   <li>{@link #getQuotaRollup} 层级聚合查询（公司/区域/团队/个人 Σ，显式值优先）</li>
 *   <li>{@link #finalizeQuota} / {@link #unfinalizeQuota} 定稿锁定/解冻</li>
 *   <li>{@link #distributeAnnualQuota} 年度配额按季/月均分</li>
 *   <li>{@link #getTerritoryPipeline} 区域管道对比入口（目标/预测/实际三段同屏）</li>
 * </ul>
 *
 * <p>对齐 {@code docs/design/crm/territory.md §配额层级汇总 / §业务规则 4-5 / §实现注记 4}。
 */
@BizModel("ErpCrmQuota")
public class ErpCrmQuotaBizModel extends CrudBizModel<ErpCrmQuota> implements IErpCrmQuotaBiz {

    @Inject
    QuotaRollupCalculator quotaRollupCalculator;

    public ErpCrmQuotaBizModel() {
        setEntityName(ErpCrmQuota.class.getName());
    }

    @Override
    @BizQuery
    public ErpCrmQuota getQuotaRollup(@Optional @Name("territoryId") Long territoryId,
                                       @Name("periodType") String periodType,
                                       @Name("fiscalYear") int fiscalYear,
                                       @Optional @Name("periodLabel") String periodLabel,
                                       IServiceContext context) {
        return quotaRollupCalculator.rollup(territoryId, periodType, fiscalYear, periodLabel);
    }

    @Override
    @BizMutation
    public ErpCrmQuota finalizeQuota(@Name("quotaId") Long quotaId, IServiceContext context) {
        ErpCrmQuota quota = requireEntity(String.valueOf(quotaId), null, context);
        if (Boolean.TRUE.equals(quota.getIsFinalized())) {
            throw new NopException(ErpCrmErrors.ERR_QUOTA_FINALIZED)
                    .param(ErpCrmErrors.ARG_QUOTA_ID, quotaId);
        }
        quota.setIsFinalized(true);
        updateEntity(quota, null, context);
        return quota;
    }

    @Override
    @BizMutation
    public ErpCrmQuota unfinalizeQuota(@Name("quotaId") Long quotaId, IServiceContext context) {
        ErpCrmQuota quota = requireEntity(String.valueOf(quotaId), null, context);
        quota.setIsFinalized(false);
        updateEntity(quota, null, context);
        return quota;
    }

    @Override
    @BizMutation
    public List<ErpCrmQuota> distributeAnnualQuota(@Name("quotaId") Long quotaId,
                                                     @Optional @Name("periodType") String periodType,
                                                     IServiceContext context) {
        return quotaRollupCalculator.distributeAnnual(quotaId, periodType);
    }

    @Override
    @BizQuery
    public ErpCrmTerritoryPipeline getTerritoryPipeline(@Optional @Name("territoryId") Long territoryId,
                                                          @Name("periodLabel") String periodLabel,
                                                          IServiceContext context) {
        QuotaRollupCalculator.ErpCrmPipelineAccumulator acc =
                quotaRollupCalculator.accumulatePipeline(territoryId, periodLabel);

        ErpCrmTerritoryPipeline pipeline = new ErpCrmTerritoryPipeline();
        pipeline.setTerritoryId(territoryId);
        pipeline.setPeriodLabel(periodLabel);

        ErpCrmTerritoryPipeline.QuotaSummary quotaSummary = new ErpCrmTerritoryPipeline.QuotaSummary();
        quotaSummary.setQuotaAmount(acc.quotaAmount);
        quotaSummary.setFinalized(acc.quotaFinalized);
        quotaSummary.setPeriodType(acc.quotaPeriodType);
        pipeline.setQuota(quotaSummary);

        ErpCrmTerritoryPipeline.ForecastSummary forecastSummary = new ErpCrmTerritoryPipeline.ForecastSummary();
        forecastSummary.setCommitAmount(acc.commitAmount);
        forecastSummary.setUpsideAmount(acc.upsideAmount);
        forecastSummary.setBestCaseAmount(acc.bestCaseAmount);
        forecastSummary.setWeightedAmount(acc.weightedAmount);
        forecastSummary.setOpportunityCount(acc.opportunityCount);
        pipeline.setForecast(forecastSummary);

        ErpCrmTerritoryPipeline.ActualSummary actualSummary = new ErpCrmTerritoryPipeline.ActualSummary();
        actualSummary.setActualRevenue(acc.actualRevenue);
        actualSummary.setConvertedCount(acc.convertedCount);
        pipeline.setActual(actualSummary);
        return pipeline;
    }

    

}
