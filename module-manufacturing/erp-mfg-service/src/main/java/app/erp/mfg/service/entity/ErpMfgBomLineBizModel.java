
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgBomLineBiz;
import app.erp.mfg.dao.entity.ErpMfgBomLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpMfgBomLine")
public class ErpMfgBomLineBizModel extends CrudBizModel<ErpMfgBomLine> implements IErpMfgBomLineBiz{
    public ErpMfgBomLineBizModel(){
        setEntityName(ErpMfgBomLine.class.getName());
    }

    @BizLoader(forType = ErpMfgBomLine.class)
    public List<String> bomCode(@ContextSource List<ErpMfgBomLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("bom"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgBomLine line : lines) {
            result.add(line.getBom() != null ? line.getBom().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgBomLine.class)
    public List<String> materialName(@ContextSource List<ErpMfgBomLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgBomLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgBomLine.class)
    public List<String> uomName(@ContextSource List<ErpMfgBomLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("uoM"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgBomLine line : lines) {
            result.add(line.getUoM() != null ? line.getUoM().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgBomLine.class)
    public List<String> warehouseName(@ContextSource List<ErpMfgBomLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("warehouse"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgBomLine line : lines) {
            result.add(line.getWarehouse() != null ? line.getWarehouse().getName() : null);
        }
        return result;
    }
}
