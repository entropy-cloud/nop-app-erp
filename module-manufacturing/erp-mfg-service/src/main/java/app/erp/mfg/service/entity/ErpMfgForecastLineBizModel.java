
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgForecastLineBiz;
import app.erp.mfg.dao.entity.ErpMfgForecastLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpMfgForecastLine")
public class ErpMfgForecastLineBizModel extends CrudBizModel<ErpMfgForecastLine> implements IErpMfgForecastLineBiz{
    public ErpMfgForecastLineBizModel(){
        setEntityName(ErpMfgForecastLine.class.getName());
    }

    @BizLoader(forType = ErpMfgForecastLine.class)
    public List<String> forecastCode(@ContextSource List<ErpMfgForecastLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("forecast"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgForecastLine line : lines) {
            result.add(line.getForecast() != null ? line.getForecast().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgForecastLine.class)
    public List<String> materialName(@ContextSource List<ErpMfgForecastLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgForecastLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgForecastLine.class)
    public List<String> warehouseName(@ContextSource List<ErpMfgForecastLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("warehouse"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgForecastLine line : lines) {
            result.add(line.getWarehouse() != null ? line.getWarehouse().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgForecastLine.class)
    public List<String> uomName(@ContextSource List<ErpMfgForecastLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("uoM"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgForecastLine line : lines) {
            result.add(line.getUoM() != null ? line.getUoM().getName() : null);
        }
        return result;
    }
}
