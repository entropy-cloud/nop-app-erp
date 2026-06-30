
package app.erp.cs.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.cs.biz.IErpCsServiceCatalogItemBiz;
import app.erp.cs.dao.entity.ErpCsServiceCatalogItem;

@BizModel("ErpCsServiceCatalogItem")
public class ErpCsServiceCatalogItemBizModel extends CrudBizModel<ErpCsServiceCatalogItem> implements IErpCsServiceCatalogItemBiz{
    public ErpCsServiceCatalogItemBizModel(){
        setEntityName(ErpCsServiceCatalogItem.class.getName());
    }
}
