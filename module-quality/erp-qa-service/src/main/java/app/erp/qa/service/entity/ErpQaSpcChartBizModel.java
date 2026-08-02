package app.erp.qa.service.entity;

import app.erp.qa.biz.IErpQaSpcChartBiz;
import app.erp.qa.biz.IErpQaSpcSampleBiz;
import app.erp.qa.dao.entity.ErpQaSpcChart;
import app.erp.qa.dao.entity.ErpQaSpcSample;
import app.erp.qa.service.processor.ErpQaSpcChartCollectSamplesProcessor;
import app.erp.qa.service.processor.ErpQaSpcChartEvaluateRulesProcessor;
import app.erp.qa.service.processor.ErpQaSpcChartRecalculateControlLimitProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.biz.BizQuery;
import io.nop.api.core.annotations.core.Name;
import io.nop.api.core.beans.query.QueryBean;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

import java.util.List;

import static io.nop.api.core.beans.FilterBeans.eq;

/**
 * SPC 控制图配置 BizModel（Facade，{@code processor-extension-pattern.md} 两层结构；
 * {@code docs/design/quality/spc.md}，plan 2026-07-07-0305-2 Phase 2/3）。采样/控制限/规则评估多步 mutation
 * 委托 per-mutation Processor，下游可经 Delta beans.xml 同名 bean id 覆盖。失控样本查询经注入的
 * {@link IErpQaSpcSampleBiz}（实体类型隔离）。
 */
@BizModel("ErpQaSpcChart")
public class ErpQaSpcChartBizModel extends CrudBizModel<ErpQaSpcChart> implements IErpQaSpcChartBiz {

    @Inject
    ErpQaSpcChartCollectSamplesProcessor collectSamplesProcessor;
    @Inject
    ErpQaSpcChartEvaluateRulesProcessor evaluateRulesProcessor;
    @Inject
    ErpQaSpcChartRecalculateControlLimitProcessor recalculateControlLimitProcessor;
    @Inject
    IErpQaSpcSampleBiz spcSampleBiz;

    public ErpQaSpcChartBizModel() {
        setEntityName(ErpQaSpcChart.class.getName());
    }

    public void setSpcSampleBiz(IErpQaSpcSampleBiz spcSampleBiz) {
        this.spcSampleBiz = spcSampleBiz;
    }

    @Override
    @BizMutation
    public Integer collectSamples(@Name("chartId") Long chartId, IServiceContext context) {
        return collectSamplesProcessor.collectSamples(chartId, context);
    }

    @Override
    @BizMutation
    public Boolean recalculateControlLimit(@Name("chartId") Long chartId, IServiceContext context) {
        return recalculateControlLimitProcessor.recalculateControlLimit(chartId, context);
    }

    @Override
    @BizMutation
    public Integer evaluateRules(@Name("chartId") Long chartId, IServiceContext context) {
        return evaluateRulesProcessor.evaluateRules(chartId, context);
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
