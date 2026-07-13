
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSocialInsuranceConfigBiz;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceConfig;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrSocialInsuranceConfig")
public class ErpHrSocialInsuranceConfigBizModel extends CrudBizModel<ErpHrSocialInsuranceConfig> implements IErpHrSocialInsuranceConfigBiz{
    public ErpHrSocialInsuranceConfigBizModel(){
        setEntityName(ErpHrSocialInsuranceConfig.class.getName());
    }

    @BizLoader(forType = ErpHrSocialInsuranceConfig.class)
    public List<String> orgName(@ContextSource List<ErpHrSocialInsuranceConfig> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSocialInsuranceConfig row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }
}
