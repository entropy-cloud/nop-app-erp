
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.sal.biz.IErpSalQuotationLineBiz;
import app.erp.sal.dao.entity.ErpSalQuotationLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpSalQuotationLine")
public class ErpSalQuotationLineBizModel extends CrudBizModel<ErpSalQuotationLine> implements IErpSalQuotationLineBiz{
    public ErpSalQuotationLineBizModel(){
        setEntityName(ErpSalQuotationLine.class.getName());
    }

    @BizLoader(forType = ErpSalQuotationLine.class)
    public List<String> materialName(@ContextSource List<ErpSalQuotationLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpSalQuotationLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }
}
