package app.erp.qa.service.entity;

import app.erp.qa.biz.IErpQaSpcChartBiz;
import app.erp.qa.biz.IErpQaSpcSampleBiz;
import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.dao.entity.ErpQaSpcSample;
import app.erp.qa.service.spc.SpcControlLimitCalculator;
import app.erp.qa.service.spc.SpcRuleEngine;
import app.erp.qa.service.spc.SpcSamplingService;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * SPC 控制图配置 BizModel（{@code docs/design/quality/spc.md}，plan 2026-07-07-0305-2 Phase 2/3）。
 *
 * <p>采样/控制限/规则评估委托给 {@link SpcSamplingService}/{@link SpcControlLimitCalculator}/
 * {@link SpcRuleEngine}。失控样本查询经注入的 {@link IErpQaSpcSampleBiz}（实体类型隔离）。
 */
@BizModel("ErpQaSpcChart")
public class ErpQaSpcChartBizModel extends CrudBizModel<ErpQaSpcChart> implements IErpQaSpcChartBiz {

    @Inject
    SpcSamplingService spcSamplingService;
    @Inject
    SpcControlLimitCalculator spcControlLimitCalculator;
    @Inject
    SpcRuleEngine spcRuleEngine;
    @Inject
    IErpQaSpcSampleBiz spcSampleBiz;

    public ErpQaSpcChartBizModel() {
        setEntityName(ErpQaSpcChart.class.getName());
    }

    @BizLoader(forType = ErpQaSpcChart.class)
    public List<String> orgName(@ContextSource List<ErpQaSpcChart> list) {
        orm().batchLoadProps(list, Collections.singleton("org"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpQaSpcChart entity : list) {
            result.add(entity.getOrg() != null ? entity.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpQaSpcChart.class)
    public List<String> materialName(@ContextSource List<ErpQaSpcChart> list) {
        orm().batchLoadProps(list, Collections.singleton("material"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpQaSpcChart entity : list) {
            result.add(entity.getMaterial() != null ? entity.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpQaSpcChart.class)
    public List<String> inspectionTemplateCode(@ContextSource List<ErpQaSpcChart> list) {
        orm().batchLoadProps(list, Collections.singleton("inspectionType"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpQaSpcChart entity : list) {
            result.add(entity.getInspectionType() != null ? entity.getInspectionType().getCode() : null);
        }
        return result;
    }

    public void setSpcSamplingService(SpcSamplingService spcSamplingService) {
        this.spcSamplingService = spcSamplingService;
    }

    public void setSpcControlLimitCalculator(SpcControlLimitCalculator spcControlLimitCalculator) {
        this.spcControlLimitCalculator = spcControlLimitCalculator;
    }

    public void setSpcRuleEngine(SpcRuleEngine spcRuleEngine) {
        this.spcRuleEngine = spcRuleEngine;
    }

    public void setSpcSampleBiz(IErpQaSpcSampleBiz spcSampleBiz) {
        this.spcSampleBiz = spcSampleBiz;
    }

    @Override
    @BizMutation
    public Integer collectSamples(@Name("chartId") Long chartId, IServiceContext context) {
        return spcSamplingService.collectSamples(chartId, context);
    }

    @Override
    @BizMutation
    public Boolean recalculateControlLimit(@Name("chartId") Long chartId, IServiceContext context) {
        return spcControlLimitCalculator.recalculate(chartId);
    }

    @Override
    @BizMutation
    public Integer evaluateRules(@Name("chartId") Long chartId, IServiceContext context) {
        return spcRuleEngine.evaluate(chartId, context);
    }

    @Override
    @BizQuery
    public List<ErpQaSpcSample> findOutOfControlSamples(@Name("chartId") Long chartId, IServiceContext context) {
        QueryBean q = new QueryBean();
        q.addFilter(eq("chartId", chartId));
        q.addFilter(eq("isOutOfControl", Boolean.TRUE));
        return spcSampleBiz.findList(q, null, context);
    }
}
