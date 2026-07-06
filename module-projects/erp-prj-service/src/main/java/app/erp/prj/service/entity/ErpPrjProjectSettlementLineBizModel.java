
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjProjectSettlementLineBiz;
import app.erp.prj.dao.entity.ErpPrjProjectSettlementLine;

@BizModel("ErpPrjProjectSettlementLine")
public class ErpPrjProjectSettlementLineBizModel extends CrudBizModel<ErpPrjProjectSettlementLine> implements IErpPrjProjectSettlementLineBiz{
    public ErpPrjProjectSettlementLineBizModel(){
        setEntityName(ErpPrjProjectSettlementLine.class.getName());
    }
}
