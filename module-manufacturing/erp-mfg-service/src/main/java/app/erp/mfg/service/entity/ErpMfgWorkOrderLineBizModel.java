
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgWorkOrderLineBiz;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpMfgWorkOrderLine")
public class ErpMfgWorkOrderLineBizModel extends CrudBizModel<ErpMfgWorkOrderLine> implements IErpMfgWorkOrderLineBiz{
    public ErpMfgWorkOrderLineBizModel(){
        setEntityName(ErpMfgWorkOrderLine.class.getName());
    }

    @BizLoader(forType = ErpMfgWorkOrderLine.class)
    public List<String> materialName(@ContextSource List<ErpMfgWorkOrderLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpMfgWorkOrderLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }
}
