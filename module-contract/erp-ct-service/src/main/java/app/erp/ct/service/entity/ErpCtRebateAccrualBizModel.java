
package app.erp.ct.service.entity;

import java.util.List;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtRebateAccrualBiz;
import app.erp.contract.dao.entity.ErpCtRebateAccrual;

@BizModel("ErpCtRebateAccrual")
public class ErpCtRebateAccrualBizModel extends CrudBizModel<ErpCtRebateAccrual> implements IErpCtRebateAccrualBiz{
    public ErpCtRebateAccrualBizModel(){
        setEntityName(ErpCtRebateAccrual.class.getName());
    }

}
