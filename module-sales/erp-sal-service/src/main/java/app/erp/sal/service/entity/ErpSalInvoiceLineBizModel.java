
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.sal.biz.IErpSalInvoiceLineBiz;
import app.erp.sal.dao.entity.ErpSalInvoiceLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpSalInvoiceLine")
public class ErpSalInvoiceLineBizModel extends CrudBizModel<ErpSalInvoiceLine> implements IErpSalInvoiceLineBiz{
    public ErpSalInvoiceLineBizModel(){
        setEntityName(ErpSalInvoiceLine.class.getName());
    }

    @BizLoader(forType = ErpSalInvoiceLine.class)
    public List<String> materialName(@ContextSource List<ErpSalInvoiceLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("material"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpSalInvoiceLine line : lines) {
            result.add(line.getMaterial() != null ? line.getMaterial().getName() : null);
        }
        return result;
    }
}
