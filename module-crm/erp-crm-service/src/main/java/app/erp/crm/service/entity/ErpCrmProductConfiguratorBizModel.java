
package app.erp.crm.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.crm.biz.IErpCrmProductConfiguratorBiz;
import app.erp.crm.dao.entity.ErpCrmProductConfigurator;

@BizModel("ErpCrmProductConfigurator")
public class ErpCrmProductConfiguratorBizModel extends CrudBizModel<ErpCrmProductConfigurator> implements IErpCrmProductConfiguratorBiz{
    public ErpCrmProductConfiguratorBizModel(){
        setEntityName(ErpCrmProductConfigurator.class.getName());
    }
}
