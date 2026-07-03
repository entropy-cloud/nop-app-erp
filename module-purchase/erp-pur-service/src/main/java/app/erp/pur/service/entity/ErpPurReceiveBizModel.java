
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
 * 采购入库单 BizModel（Facade）。审批状态机 + 入库审核触发库存移动编排委托
 * {@link ErpPurReceiveProcessor}（protected step 方法，下游可逐 step 覆盖）。
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
    public ErpPurReceive submit(@Name("receiveId") Long receiveId, IServiceContext context) {
        return receiveProcessor.submit(receiveId, context);
    }

    @Override
    @BizMutation
    public ErpPurReceive withdrawSubmit(@Name("receiveId") Long receiveId, IServiceContext context) {
        return receiveProcessor.withdrawSubmit(receiveId, context);
    }

    @Override
    @BizMutation
    public ErpPurReceive approve(@Name("receiveId") Long receiveId, IServiceContext context) {
        return receiveProcessor.approve(receiveId, context);
    }

    @Override
    @BizMutation
    public ErpPurReceive reject(@Name("receiveId") Long receiveId, IServiceContext context) {
        return receiveProcessor.reject(receiveId, context);
    }

    @Override
    @BizMutation
    public ErpPurReceive reverseApprove(@Name("receiveId") Long receiveId, IServiceContext context) {
        return receiveProcessor.reverseApprove(receiveId, context);
    }

    @Override
    @BizMutation
    public ErpPurReceive cancel(@Name("receiveId") Long receiveId, IServiceContext context) {
        return receiveProcessor.cancel(receiveId, context);
    }
}
