
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurInvoiceLineBiz;
import app.erp.pur.dao.entity.ErpPurInvoiceLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpPurInvoiceLine")
public class ErpPurInvoiceLineBizModel extends CrudBizModel<ErpPurInvoiceLine> implements IErpPurInvoiceLineBiz{
    public ErpPurInvoiceLineBizModel(){
        setEntityName(ErpPurInvoiceLine.class.getName());
    }

    @BizLoader(forType = ErpPurInvoiceLine.class)
    public List<String> materialName(@ContextSource List<ErpPurInvoiceLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPurInvoiceLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }
}
