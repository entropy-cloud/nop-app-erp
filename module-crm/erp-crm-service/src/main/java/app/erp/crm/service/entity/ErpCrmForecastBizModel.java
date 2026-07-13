package app.erp.crm.service.entity;

import app.erp.crm.biz.IErpCrmForecastBiz;
import app.erp.crm.dao.entity.ErpCrmForecast;
import app.erp.crm.service.support.ForecastAggregator;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 销售预测 BizModel。{@link #refreshForecast} 委托 {@link ForecastAggregator} 聚合引擎
 * （commit/upside/best-case/weighted 分类 + 商机级 ForecastLine 快照 + 团队→公司层级 rollup）。
 *
 * <p>对齐 {@code docs/design/crm/sales-forecast.md}。
 */
@BizModel("ErpCrmForecast")
public class ErpCrmForecastBizModel extends CrudBizModel<ErpCrmForecast> implements IErpCrmForecastBiz {

    @Inject
    ForecastAggregator forecastAggregator;

    public ErpCrmForecastBizModel() {
        setEntityName(ErpCrmForecast.class.getName());
    }

    @Override
    @BizMutation
    public void refreshForecast(@Name("periodId") Long periodId, IServiceContext context) {
        forecastAggregator.refreshForecast(periodId, context);
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCrmForecast.class)
    public List<String> orgName(@ContextSource List<ErpCrmForecast> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmForecast row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmForecast.class)
    public List<String> periodCode(@ContextSource List<ErpCrmForecast> rows) {
        orm().batchLoadProps(rows, Collections.singleton("period"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmForecast row : rows) {
            result.add(row.orm_attached() && row.getPeriod() != null ? row.getPeriod().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmForecast.class)
    public List<String> territoryName(@ContextSource List<ErpCrmForecast> rows) {
        orm().batchLoadProps(rows, Collections.singleton("territory"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmForecast row : rows) {
            result.add(row.orm_attached() && row.getTerritory() != null ? row.getTerritory().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmForecast.class)
    public List<String> teamName(@ContextSource List<ErpCrmForecast> rows) {
        orm().batchLoadProps(rows, Collections.singleton("team"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmForecast row : rows) {
            result.add(row.orm_attached() && row.getTeam() != null ? row.getTeam().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmForecast.class)
    public List<String> currencyName(@ContextSource List<ErpCrmForecast> rows) {
        orm().batchLoadProps(rows, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmForecast row : rows) {
            result.add(row.orm_attached() && row.getCurrency() != null ? row.getCurrency().getName() : null);
        }
        return result;
    }

}
