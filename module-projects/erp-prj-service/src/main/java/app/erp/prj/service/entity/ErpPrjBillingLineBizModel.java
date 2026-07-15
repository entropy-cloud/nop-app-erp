
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjBillingLineBiz;
import app.erp.prj.dao.entity.ErpPrjBillingLine;

import java.util.List;

@BizModel("ErpPrjBillingLine")
public class ErpPrjBillingLineBizModel extends CrudBizModel<ErpPrjBillingLine> implements IErpPrjBillingLineBiz{
    public ErpPrjBillingLineBizModel(){
        setEntityName(ErpPrjBillingLine.class.getName());
    }

}
