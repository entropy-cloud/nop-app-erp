
package app.erp.fin.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.fin.biz.IErpFinGlBalanceBiz;
import app.erp.fin.dao.entity.ErpFinGlBalance;

@BizModel("ErpFinGlBalance")
public class ErpFinGlBalanceBizModel extends CrudBizModel<ErpFinGlBalance> implements IErpFinGlBalanceBiz{
    public ErpFinGlBalanceBizModel(){
        setEntityName(ErpFinGlBalance.class.getName());
    }
}
