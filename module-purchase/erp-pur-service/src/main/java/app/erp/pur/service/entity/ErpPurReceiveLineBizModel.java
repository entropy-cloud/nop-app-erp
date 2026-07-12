
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurReceiveLineBiz;
import app.erp.pur.dao.entity.ErpPurReceiveLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpPurReceiveLine")
public class ErpPurReceiveLineBizModel extends CrudBizModel<ErpPurReceiveLine> implements IErpPurReceiveLineBiz{
    public ErpPurReceiveLineBizModel(){
        setEntityName(ErpPurReceiveLine.class.getName());
    }

    @BizLoader(forType = ErpPurReceiveLine.class)
    public List<String> materialName(@ContextSource List<ErpPurReceiveLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPurReceiveLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPurReceiveLine.class)
    public List<String> warehouseName(@ContextSource List<ErpPurReceiveLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("warehouse"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPurReceiveLine line : lines) {
            result.add(line.getWarehouse() != null ? line.getWarehouse().getName() : null);
        }
        return result;
    }
}
