
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjBillingLineBiz;
import app.erp.prj.dao.entity.ErpPrjBillingLine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpPrjBillingLine")
public class ErpPrjBillingLineBizModel extends CrudBizModel<ErpPrjBillingLine> implements IErpPrjBillingLineBiz{
    public ErpPrjBillingLineBizModel(){
        setEntityName(ErpPrjBillingLine.class.getName());
    }

    @BizLoader(forType = ErpPrjBillingLine.class)
    public List<String> billingCode(@ContextSource List<ErpPrjBillingLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("billing"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPrjBillingLine line : lines) {
            result.add(line.getBilling() != null ? line.getBilling().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjBillingLine.class)
    public List<String> taskName(@ContextSource List<ErpPrjBillingLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("task"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPrjBillingLine line : lines) {
            result.add(line.getTask() != null ? line.getTask().getTitle() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjBillingLine.class)
    public List<String> subjectName(@ContextSource List<ErpPrjBillingLine> lines) {
        orm().batchLoadProps(lines, Collections.singleton("subject"));
        List<String> result = new ArrayList<>(lines.size());
        for (ErpPrjBillingLine line : lines) {
            result.add(line.getSubject() != null ? line.getSubject().getName() : null);
        }
        return result;
    }
}
