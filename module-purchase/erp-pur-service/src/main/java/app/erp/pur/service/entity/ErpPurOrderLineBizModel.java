
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurOrderLineBiz;
import app.erp.pur.dao.entity.ErpPurOrderLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpPurOrderLine")
public class ErpPurOrderLineBizModel extends CrudBizModel<ErpPurOrderLine> implements IErpPurOrderLineBiz{
    public ErpPurOrderLineBizModel(){
        setEntityName(ErpPurOrderLine.class.getName());
    }

    @BizLoader(forType = ErpPurOrderLine.class)
    public List<String> materialName(@ContextSource List<ErpPurOrderLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPurOrderLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPurOrderLine.class)
    public List<String> warehouseName(@ContextSource List<ErpPurOrderLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("warehouse"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPurOrderLine line : lines) {
            result.add(line.getWarehouse() != null ? line.getWarehouse().getName() : null);
        }
        return result;
    }
}
