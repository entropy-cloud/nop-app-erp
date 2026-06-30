
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmTerritoryBiz;
import app.erp.crm.dao.entity.ErpCrmTerritory;

@BizModel("ErpCrmTerritory")
public class ErpCrmTerritoryBizModel extends CrudBizModel<ErpCrmTerritory> implements IErpCrmTerritoryBiz{
    public ErpCrmTerritoryBizModel(){
        setEntityName(ErpCrmTerritory.class.getName());
    }
}
