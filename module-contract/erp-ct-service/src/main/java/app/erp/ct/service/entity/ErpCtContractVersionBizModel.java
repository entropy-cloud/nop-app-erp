
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtContractVersionBiz;
import app.erp.contract.dao.entity.ErpCtContractVersion;

@BizModel("ErpCtContractVersion")
public class ErpCtContractVersionBizModel extends CrudBizModel<ErpCtContractVersion> implements IErpCtContractVersionBiz{
    public ErpCtContractVersionBizModel(){
        setEntityName(ErpCtContractVersion.class.getName());
    }
}
