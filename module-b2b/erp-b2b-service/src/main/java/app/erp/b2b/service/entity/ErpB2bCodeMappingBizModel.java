
package app.erp.b2b.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.b2b.biz.IErpB2bCodeMappingBiz;
import app.erp.b2b.dao.entity.ErpB2bCodeMapping;

@BizModel("ErpB2bCodeMapping")
public class ErpB2bCodeMappingBizModel extends CrudBizModel<ErpB2bCodeMapping> implements IErpB2bCodeMappingBiz{
    public ErpB2bCodeMappingBizModel(){
        setEntityName(ErpB2bCodeMapping.class.getName());
    }

}
