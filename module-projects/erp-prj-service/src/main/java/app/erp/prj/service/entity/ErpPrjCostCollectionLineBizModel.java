
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjCostCollectionLineBiz;
import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;

@BizModel("ErpPrjCostCollectionLine")
public class ErpPrjCostCollectionLineBizModel extends CrudBizModel<ErpPrjCostCollectionLine> implements IErpPrjCostCollectionLineBiz{
    public ErpPrjCostCollectionLineBizModel(){
        setEntityName(ErpPrjCostCollectionLine.class.getName());
    }
}
