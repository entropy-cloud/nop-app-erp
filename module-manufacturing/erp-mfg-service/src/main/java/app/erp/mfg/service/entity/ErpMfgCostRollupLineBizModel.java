
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgCostRollupLineBiz;
import app.erp.mfg.dao.entity.ErpMfgCostRollupLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpMfgCostRollupLine")
public class ErpMfgCostRollupLineBizModel extends CrudBizModel<ErpMfgCostRollupLine> implements IErpMfgCostRollupLineBiz{
    public ErpMfgCostRollupLineBizModel(){
        setEntityName(ErpMfgCostRollupLine.class.getName());
    }

    @BizLoader(forType = ErpMfgCostRollupLine.class)
    public List<String> costRollupCode(@ContextSource List<ErpMfgCostRollupLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("costRollup"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgCostRollupLine line : lines) {
            result.add(line.getCostRollup() != null ? line.getCostRollup().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgCostRollupLine.class)
    public List<String> materialName(@ContextSource List<ErpMfgCostRollupLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgCostRollupLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgCostRollupLine.class)
    public List<String> uomName(@ContextSource List<ErpMfgCostRollupLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("uoM"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgCostRollupLine line : lines) {
            result.add(line.getUoM() != null ? line.getUoM().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgCostRollupLine.class)
    public List<String> currencyName(@ContextSource List<ErpMfgCostRollupLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgCostRollupLine line : lines) {
            result.add(line.getCurrency() != null ? line.getCurrency().getName() : null);
        }
        return result;
    }
}
