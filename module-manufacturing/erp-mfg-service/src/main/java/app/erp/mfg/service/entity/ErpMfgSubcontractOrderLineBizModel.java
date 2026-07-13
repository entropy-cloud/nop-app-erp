
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgSubcontractOrderLineBiz;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrderLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpMfgSubcontractOrderLine")
public class ErpMfgSubcontractOrderLineBizModel extends CrudBizModel<ErpMfgSubcontractOrderLine> implements IErpMfgSubcontractOrderLineBiz{
    public ErpMfgSubcontractOrderLineBizModel(){
        setEntityName(ErpMfgSubcontractOrderLine.class.getName());
    }

    @BizLoader(forType = ErpMfgSubcontractOrderLine.class)
    public List<String> subcontractOrderCode(@ContextSource List<ErpMfgSubcontractOrderLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("subcontractOrder"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgSubcontractOrderLine line : lines) {
            result.add(line.getSubcontractOrder() != null ? line.getSubcontractOrder().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgSubcontractOrderLine.class)
    public List<String> materialName(@ContextSource List<ErpMfgSubcontractOrderLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgSubcontractOrderLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpMfgSubcontractOrderLine.class)
    public List<String> uomName(@ContextSource List<ErpMfgSubcontractOrderLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("uoM"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgSubcontractOrderLine line : lines) {
            result.add(line.getUoM() != null ? line.getUoM().getName() : null);
        }
        return result;
    }
}
