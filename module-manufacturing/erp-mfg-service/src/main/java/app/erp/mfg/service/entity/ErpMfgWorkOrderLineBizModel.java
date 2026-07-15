
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.mfg.biz.IErpMfgWorkOrderLineBiz;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;

import java.util.List;

@BizModel("ErpMfgWorkOrderLine")
public class ErpMfgWorkOrderLineBizModel extends CrudBizModel<ErpMfgWorkOrderLine> implements IErpMfgWorkOrderLineBiz{
    public ErpMfgWorkOrderLineBizModel(){
        setEntityName(ErpMfgWorkOrderLine.class.getName());
    }

}
