
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinAccountingPeriodBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriod;

@BizModel("ErpFinAccountingPeriod")
public class ErpFinAccountingPeriodBizModel extends CrudBizModel<ErpFinAccountingPeriod> implements IErpFinAccountingPeriodBiz{
    public ErpFinAccountingPeriodBizModel(){
        setEntityName(ErpFinAccountingPeriod.class.getName());
    }
}
