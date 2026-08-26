
package app.erp.mfg.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.mfg.biz.IErpMfgSubcontractOrderLineBiz;
import app.erp.mfg.dao.entity.ErpMfgSubcontractOrderLine;

import java.util.List;

@BizModel("ErpMfgSubcontractOrderLine")
public class ErpMfgSubcontractOrderLineBizModel extends AbstractErpCrudBizModel<ErpMfgSubcontractOrderLine> implements IErpMfgSubcontractOrderLineBiz{
    public ErpMfgSubcontractOrderLineBizModel(){
        setEntityName(ErpMfgSubcontractOrderLine.class.getName());
    }

}
