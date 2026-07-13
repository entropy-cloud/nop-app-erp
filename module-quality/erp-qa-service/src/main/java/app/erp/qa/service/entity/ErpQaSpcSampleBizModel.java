
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.qa.biz.IErpQaSpcSampleBiz;
import app.erp.qa.dao.entity.ErpQaSpcSample;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpQaSpcSample")
public class ErpQaSpcSampleBizModel extends CrudBizModel<ErpQaSpcSample> implements IErpQaSpcSampleBiz{
    public ErpQaSpcSampleBizModel(){
        setEntityName(ErpQaSpcSample.class.getName());
    }

    @BizLoader(forType = ErpQaSpcSample.class)
    public List<String> orgName(@ContextSource List<ErpQaSpcSample> list) {
        orm().batchLoadProps(list, Collections.singleton("org"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpQaSpcSample entity : list) {
            result.add(entity.getOrg() != null ? entity.getOrg().getName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpQaSpcSample.class)
    public List<String> chartCode(@ContextSource List<ErpQaSpcSample> list) {
        orm().batchLoadProps(list, Collections.singleton("chart"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpQaSpcSample entity : list) {
            result.add(entity.getChart() != null ? entity.getChart().getCode() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpQaSpcSample.class)
    public List<String> inspectorName(@ContextSource List<ErpQaSpcSample> list) {
        orm().batchLoadProps(list, Collections.singleton("inspector"));
        List<String> result = new ArrayList<>(list.size());
        for (ErpQaSpcSample entity : list) {
            result.add(entity.getInspector() != null ? entity.getInspector().getName() : null);
        }
        return result;
    }
}
