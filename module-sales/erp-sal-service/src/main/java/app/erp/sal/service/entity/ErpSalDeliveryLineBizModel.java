
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.sal.biz.IErpSalDeliveryLineBiz;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpSalDeliveryLine")
public class ErpSalDeliveryLineBizModel extends CrudBizModel<ErpSalDeliveryLine> implements IErpSalDeliveryLineBiz{
    public ErpSalDeliveryLineBizModel(){
        setEntityName(ErpSalDeliveryLine.class.getName());
    }

    @BizLoader(forType = ErpSalDeliveryLine.class)
    public List<String> materialName(@ContextSource List<ErpSalDeliveryLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpSalDeliveryLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpSalDeliveryLine.class)
    public List<String> warehouseName(@ContextSource List<ErpSalDeliveryLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("warehouse"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpSalDeliveryLine line : lines) {
            result.add(line.getWarehouse() != null ? line.getWarehouse().getName() : null);
        }
        return result;
    }
}
