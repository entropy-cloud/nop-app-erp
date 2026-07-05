
package app.erp.pur.service.entity;

import app.erp.pur.biz.IErpPurReceiveBiz;
import app.erp.pur.dao.entity.ErpPurReceive;
import app.erp.pur.service.processor.ErpPurReceiveProcessor;
import io.nop.api.core.annotations.biz.BizModel;
import io.nop.api.core.annotations.biz.BizMutation;
import io.nop.api.core.annotations.core.Name;
import io.nop.biz.crud.CrudBizModel;
import io.nop.core.context.IServiceContext;
import jakarta.inject.Inject;

/**
 * 采购入库单 BizModel（Facade）。标准审批动作（submitForApproval/approve/reject/reverseApprove/
 * withdrawApproval）由 xbiz 一行委托注入 Processor；非审批动作（cancel）在本类完成
 * Long→String 转换后委托 Processor。
 */
@BizModel("ErpPurReceive")
public class ErpPurReceiveBizModel extends CrudBizModel<ErpPurReceive> implements IErpPurReceiveBiz {

    @Inject
    ErpPurReceiveProcessor receiveProcessor;

    public ErpPurReceiveBizModel() {
        setEntityName(ErpPurReceive.class.getName());
    }

    @Override
    @BizMutation
    public ErpPurReceive cancel(@Name("receiveId") Long receiveId, IServiceContext context) {
        return receiveProcessor.cancel(String.valueOf(receiveId), context);
    }
}
