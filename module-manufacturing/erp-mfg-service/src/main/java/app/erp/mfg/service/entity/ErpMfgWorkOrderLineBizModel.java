
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.mfg.biz.IErpMfgWorkOrderLineBiz;
import app.erp.mfg.dao.entity.ErpMfgWorkOrderLine;

import java.util.List;

@BizModel("ErpMfgWorkOrderLine")
public class ErpMfgWorkOrderLineBizModel extends AbstractErpCrudBizModel<ErpMfgWorkOrderLine> implements IErpMfgWorkOrderLineBiz{
    public ErpMfgWorkOrderLineBizModel(){
        setEntityName(ErpMfgWorkOrderLine.class.getName());
    }

}
