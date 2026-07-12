
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurRequisitionLineBiz;
import app.erp.pur.dao.entity.ErpPurRequisitionLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpPurRequisitionLine")
public class ErpPurRequisitionLineBizModel extends CrudBizModel<ErpPurRequisitionLine> implements IErpPurRequisitionLineBiz{
    public ErpPurRequisitionLineBizModel(){
        setEntityName(ErpPurRequisitionLine.class.getName());
    }

    @BizLoader(forType = ErpPurRequisitionLine.class)
    public List<String> materialName(@ContextSource List<ErpPurRequisitionLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPurRequisitionLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }
}
