
package app.erp.ct.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtContractLineBiz;
import app.erp.contract.dao.entity.ErpCtContractLine;

@BizModel("ErpCtContractLine")
public class ErpCtContractLineBizModel extends CrudBizModel<ErpCtContractLine> implements IErpCtContractLineBiz{
    public ErpCtContractLineBizModel(){
        setEntityName(ErpCtContractLine.class.getName());
    }

}
