
package app.erp.b2b.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bMftConfigBiz;
import app.erp.b2b.dao.entity.ErpB2bMftConfig;

@BizModel("ErpB2bMftConfig")
public class ErpB2bMftConfigBizModel extends CrudBizModel<ErpB2bMftConfig> implements IErpB2bMftConfigBiz{
    public ErpB2bMftConfigBizModel(){
        setEntityName(ErpB2bMftConfig.class.getName());
    }

}
