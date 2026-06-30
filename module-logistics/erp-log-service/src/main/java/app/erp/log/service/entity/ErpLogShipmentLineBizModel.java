
package app.erp.log.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.log.biz.IErpLogShipmentLineBiz;
import app.erp.log.dao.entity.ErpLogShipmentLine;

@BizModel("ErpLogShipmentLine")
public class ErpLogShipmentLineBizModel extends CrudBizModel<ErpLogShipmentLine> implements IErpLogShipmentLineBiz{
    public ErpLogShipmentLineBizModel(){
        setEntityName(ErpLogShipmentLine.class.getName());
    }
}
