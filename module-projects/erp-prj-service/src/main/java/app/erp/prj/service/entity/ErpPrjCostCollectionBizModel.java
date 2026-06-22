
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.prj.biz.IErpPrjCostCollectionBiz;
import app.erp.prj.dao.entity.ErpPrjCostCollection;

@BizModel("ErpPrjCostCollection")
public class ErpPrjCostCollectionBizModel extends CrudBizModel<ErpPrjCostCollection> implements IErpPrjCostCollectionBiz{
    public ErpPrjCostCollectionBizModel(){
        setEntityName(ErpPrjCostCollection.class.getName());
    }
}
