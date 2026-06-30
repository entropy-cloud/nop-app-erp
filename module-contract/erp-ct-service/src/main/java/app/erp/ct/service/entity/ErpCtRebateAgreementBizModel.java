
package app.erp.ct.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.biz.crud.CrudBizModel;

import app.erp.ct.biz.IErpCtRebateAgreementBiz;
import app.erp.contract.dao.entity.ErpCtRebateAgreement;

@BizModel("ErpCtRebateAgreement")
public class ErpCtRebateAgreementBizModel extends CrudBizModel<ErpCtRebateAgreement> implements IErpCtRebateAgreementBiz{
    public ErpCtRebateAgreementBizModel(){
        setEntityName(ErpCtRebateAgreement.class.getName());
    }
}
