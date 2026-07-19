
package app.erp.pur.service.entity;

import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;

import app.erp.pur.biz.IErpPurRfqBiz;
import app.erp.pur.dao.constants.ErpPurDocStatus;
import app.erp.pur.dao.entity.ErpPurRfq;

@BizModel("ErpPurRfq")
public class ErpPurRfqBizModel extends CrudBizModel<ErpPurRfq> implements IErpPurRfqBiz {
    public ErpPurRfqBizModel(){
        setEntityName(ErpPurRfq.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurRfq cancel(@Name("rfqId") Long rfqId, IServiceContext context) {
        ErpPurRfq rfq = requireEntity(String.valueOf(rfqId), null, context);
        rfq.setDocStatus(ErpPurDocStatus.DOC_STATUS_CANCELLED);
        updateEntity(rfq, null, context);
        return rfq;
    }
}
