
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmForecastAccuracyBiz;
import app.erp.crm.dao.entity.ErpCrmForecastAccuracy;
import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.ContextSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpCrmForecastAccuracy")
public class ErpCrmForecastAccuracyBizModel extends CrudBizModel<ErpCrmForecastAccuracy> implements IErpCrmForecastAccuracyBiz{
    public ErpCrmForecastAccuracyBizModel(){
        setEntityName(ErpCrmForecastAccuracy.class.getName());
    }

    
    // ---------- 高价值外键名称解析（机制 D：xmeta 派生 *Name/*Code 字段 + @BizLoader 批量加载防 N+1）----------
    @BizLoader(forType = ErpCrmForecastAccuracy.class)
    public List<String> orgName(@ContextSource List<ErpCrmForecastAccuracy> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmForecastAccuracy row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmForecastAccuracy.class)
    public List<String> periodCode(@ContextSource List<ErpCrmForecastAccuracy> rows) {
        orm().batchLoadProps(rows, Collections.singleton("period"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmForecastAccuracy row : rows) {
            result.add(row.orm_attached() && row.getPeriod() != null ? row.getPeriod().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmForecastAccuracy.class)
    public List<String> teamName(@ContextSource List<ErpCrmForecastAccuracy> rows) {
        orm().batchLoadProps(rows, Collections.singleton("team"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmForecastAccuracy row : rows) {
            result.add(row.orm_attached() && row.getTeam() != null ? row.getTeam().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpCrmForecastAccuracy.class)
    public List<String> territoryName(@ContextSource List<ErpCrmForecastAccuracy> rows) {
        orm().batchLoadProps(rows, Collections.singleton("territory"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpCrmForecastAccuracy row : rows) {
            result.add(row.orm_attached() && row.getTerritory() != null ? row.getTerritory().getName() : null);
        }
        return result;
    }

}
