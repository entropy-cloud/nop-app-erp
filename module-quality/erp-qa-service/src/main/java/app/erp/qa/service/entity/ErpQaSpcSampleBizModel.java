
package app.erp.qa.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.qa.biz.IErpQaSpcSampleBiz;
import app.erp.qa.dao.entity.ErpQaSpcSample;

@BizModel("ErpQaSpcSample")
public class ErpQaSpcSampleBizModel extends CrudBizModel<ErpQaSpcSample> implements IErpQaSpcSampleBiz{
    public ErpQaSpcSampleBizModel(){
        setEntityName(ErpQaSpcSample.class.getName());
    }
}
