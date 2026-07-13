
package app.erp.hr.service.entity;

import io.nop.api.core.annotations.biz.BizLoader;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.ContextSource;
import io.nop.biz.crud.CrudBizModel;

import app.erp.hr.biz.IErpHrSocialInsuranceBaseBiz;
import app.erp.hr.dao.entity.ErpHrSocialInsuranceBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@BizModel("ErpHrSocialInsuranceBase")
public class ErpHrSocialInsuranceBaseBizModel extends CrudBizModel<ErpHrSocialInsuranceBase> implements IErpHrSocialInsuranceBaseBiz{
    public ErpHrSocialInsuranceBaseBizModel(){
        setEntityName(ErpHrSocialInsuranceBase.class.getName());
    }

    @BizLoader(forType = ErpHrSocialInsuranceBase.class)
    public List<String> employeeDisplayName(@ContextSource List<ErpHrSocialInsuranceBase> rows) {
        orm().batchLoadProps(rows, Collections.singleton("employee"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSocialInsuranceBase row : rows) {
            result.add(row.orm_attached() && row.getEmployee() != null ? row.getEmployee().getFullName() : null);
        }
        return result;
    }

    @BizLoader(forType = ErpHrSocialInsuranceBase.class)
    public List<String> orgName(@ContextSource List<ErpHrSocialInsuranceBase> rows) {
        orm().batchLoadProps(rows, Collections.singleton("org"));
        List<String> result = new ArrayList<>(rows.size());
        for (ErpHrSocialInsuranceBase row : rows) {
            result.add(row.orm_attached() && row.getOrg() != null ? row.getOrg().getName() : null);
        }
        return result;
    }
}
