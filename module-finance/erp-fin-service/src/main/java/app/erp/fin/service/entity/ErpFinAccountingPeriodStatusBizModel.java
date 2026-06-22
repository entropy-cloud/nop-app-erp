
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinAccountingPeriodStatusBiz;
import app.erp.fin.dao.entity.ErpFinAccountingPeriodStatus;

@BizModel("ErpFinAccountingPeriodStatus")
public class ErpFinAccountingPeriodStatusBizModel extends CrudBizModel<ErpFinAccountingPeriodStatus> implements IErpFinAccountingPeriodStatusBiz{
    public ErpFinAccountingPeriodStatusBizModel(){
        setEntityName(ErpFinAccountingPeriodStatus.class.getName());
    }
}
