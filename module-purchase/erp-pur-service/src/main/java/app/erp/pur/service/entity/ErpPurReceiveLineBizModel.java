
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.pur.biz.IErpPurReceiveLineBiz;
import app.erp.pur.dao.entity.ErpPurReceiveLine;

import java.util.List;

@BizModel("ErpPurReceiveLine")
public class ErpPurReceiveLineBizModel extends CrudBizModel<ErpPurReceiveLine> implements IErpPurReceiveLineBiz{
    public ErpPurReceiveLineBizModel(){
        setEntityName(ErpPurReceiveLine.class.getName());
    }

}
