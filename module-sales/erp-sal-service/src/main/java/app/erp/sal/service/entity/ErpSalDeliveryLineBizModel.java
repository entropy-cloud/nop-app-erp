
package app.erp.sal.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.sal.biz.IErpSalDeliveryLineBiz;
import app.erp.sal.dao.entity.ErpSalDeliveryLine;

import java.util.List;

@BizModel("ErpSalDeliveryLine")
public class ErpSalDeliveryLineBizModel extends AbstractErpCrudBizModel<ErpSalDeliveryLine> implements IErpSalDeliveryLineBiz{
    public ErpSalDeliveryLineBizModel(){
        setEntityName(ErpSalDeliveryLine.class.getName());
    }

}
