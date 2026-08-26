
package app.erp.ct.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;
import app.erp.common.service.AbstractErpCrudBizModel;

import app.erp.ct.biz.IErpCtRebateTierBiz;
import app.erp.contract.dao.entity.ErpCtRebateTier;

@BizModel("ErpCtRebateTier")
public class ErpCtRebateTierBizModel extends AbstractErpCrudBizModel<ErpCtRebateTier> implements IErpCtRebateTierBiz{
    public ErpCtRebateTierBizModel(){
        setEntityName(ErpCtRebateTier.class.getName());
    }

}
