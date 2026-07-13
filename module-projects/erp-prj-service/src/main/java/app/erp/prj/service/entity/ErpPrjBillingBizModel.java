
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjBillingBiz;
import app.erp.prj.dao.entity.ErpPrjBilling;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpPrjBilling")
public class ErpPrjBillingBizModel extends CrudBizModel<ErpPrjBilling> implements IErpPrjBillingBiz{
    public ErpPrjBillingBizModel(){
        setEntityName(ErpPrjBilling.class.getName());
    }

    @BizLoader(forType = ErpPrjBilling.class)
    public List<String> projectName(@ContextSource List<ErpPrjBilling> billings) {
        orm().batchLoadProps(billings, Collections.singleton("project"));
        List<String> result = new ArrayList<>(billings.size());
        for (ErpPrjBilling billing : billings) {
            result.add(billing.getProject() != null ? billing.getProject().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjBilling.class)
    public List<String> orgName(@ContextSource List<ErpPrjBilling> billings) {
        orm().batchLoadProps(billings, Collections.singleton("org"));
        List<String> result = new ArrayList<>(billings.size());
        for (ErpPrjBilling billing : billings) {
            result.add(billing.getOrg() != null ? billing.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjBilling.class)
    public List<String> customerName(@ContextSource List<ErpPrjBilling> billings) {
        orm().batchLoadProps(billings, Collections.singleton("customer"));
        List<String> result = new ArrayList<>(billings.size());
        for (ErpPrjBilling billing : billings) {
            result.add(billing.getCustomer() != null ? billing.getCustomer().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjBilling.class)
    public List<String> milestoneName(@ContextSource List<ErpPrjBilling> billings) {
        orm().batchLoadProps(billings, Collections.singleton("milestone"));
        List<String> result = new ArrayList<>(billings.size());
        for (ErpPrjBilling billing : billings) {
            result.add(billing.getMilestone() != null ? billing.getMilestone().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpPrjBilling.class)
    public List<String> currencyName(@ContextSource List<ErpPrjBilling> billings) {
        orm().batchLoadProps(billings, Collections.singleton("currency"));
        List<String> result = new ArrayList<>(billings.size());
        for (ErpPrjBilling billing : billings) {
            result.add(billing.getCurrency() != null ? billing.getCurrency().getName() : null);
        }
        return result;
    }
}
