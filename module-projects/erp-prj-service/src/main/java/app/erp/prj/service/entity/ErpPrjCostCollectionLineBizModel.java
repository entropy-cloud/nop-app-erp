
package app.erp.prj.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.prj.biz.IErpPrjCostCollectionLineBiz;
import app.erp.prj.dao.entity.ErpPrjCostCollectionLine;

import java.util.List;

@BizModel("ErpPrjCostCollectionLine")
public class ErpPrjCostCollectionLineBizModel extends AbstractErpCrudBizModel<ErpPrjCostCollectionLine> implements IErpPrjCostCollectionLineBiz{
    public ErpPrjCostCollectionLineBizModel(){
        setEntityName(ErpPrjCostCollectionLine.class.getName());
    }

}
